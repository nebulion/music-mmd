package com.calmapps.calmmusic

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.calmapps.calmmusic.ui.AlbumUiModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Album covers for E Ink (owner, 2026-10-08: "make it clearer which albums are which").
 *
 * Sources, in order: the YouTube Music album thumbnail; the picture inside the album's own files;
 * a YouTube song's thumbnail (square, from the middle). Each is cut square, turned grey and given
 * more contrast — for photos that reads better on the 1-bit panel than dithering by hand
 * (eink-conversion-learnings §7) — and kept as a small PNG in the cache folder.
 *
 * One paint per screen: a drawn row never changes when a cover arrives. [peek] is what rows draw
 * (memory only, at once); [load] fills memory and disk in the background, so the next time the row
 * is drawn the cover is there. Pages that show one big cover load it before they open.
 */
class CoverStore(private val app: MonoMusic) {

    private val dir = File(app.cacheDir, "covers").apply { mkdirs() }
    private val memory = object : LruCache<String, ImageBitmap>(12 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 2
    }
    /** Albums known to have no cover anywhere (not asked again this session). */
    private val none = ConcurrentHashMap.newKeySet<String>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gate = Semaphore(3)
    private val http by lazy { OkHttpClient() }

    private fun key(album: AlbumUiModel) = ("v2_" + album.sourceType + "_" + album.id).replace(Regex("[^A-Za-z0-9_-]"), "_").take(120)

    /** The cover if it is already in memory; never waits. */
    fun peek(album: AlbumUiModel): ImageBitmap? = memory.get(key(album))

    /** Loads covers in the background (memory and disk), a few at a time. */
    fun prefetch(albums: List<AlbumUiModel>) {
        albums.forEach { album ->
            if (peek(album) == null && key(album) !in none) scope.launch { gate.withPermit { load(album) } }
        }
    }

    /** The cover, from memory, disk or its source; null when there is none. */
    suspend fun load(album: AlbumUiModel): ImageBitmap? = loadKeyed(key(album)) { fromUrl(album.coverUrl) ?: fromFiles(album) }

    private fun songKey(song: com.calmapps.calmmusic.ui.SongUiModel) =
        ("s2_" + (song.albumId ?: (song.album.orEmpty() + "_" + song.artist).ifBlank { song.id })).replace(Regex("[^A-Za-z0-9_-]"), "_").take(120)

    fun peekSong(song: com.calmapps.calmmusic.ui.SongUiModel): ImageBitmap? = memory.get(songKey(song))

    /** The playing song's cover (Now Playing): its file's picture, else its YouTube still. */
    suspend fun loadForSong(song: com.calmapps.calmmusic.ui.SongUiModel): ImageBitmap? = loadKeyed(songKey(song)) {
        val local = song.audioUri?.takeIf { song.sourceType != "YOUTUBE" && it.contains(":") }
        val embedded = local?.let { uri ->
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(app, Uri.parse(uri))
                retriever.embeddedPicture?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
            } catch (_: Exception) {
                null
            } finally {
                runCatching { retriever.release() }
            }
        }
        embedded ?: if (!song.id.startsWith("local:")) fromVideo(song.id) else null
    }

    /**
     * The cover of a queued song as PNG bytes, for the media session (launchers such as InkOS show
     * only a picture attached to the session, never one behind a link).
     */
    suspend fun artworkFor(item: androidx.media3.common.MediaItem): ByteArray? {
        val uri = item.localConfiguration?.uri
        val isYouTube = uri?.host == "www.youtube.com"
        val song = com.calmapps.calmmusic.ui.SongUiModel(
            id = item.mediaId,
            title = item.mediaMetadata.title?.toString().orEmpty(),
            artist = item.mediaMetadata.artist?.toString().orEmpty(),
            sourceType = if (isYouTube) "YOUTUBE" else "LOCAL_FILE",
            audioUri = if (isYouTube) item.mediaId else uri?.toString(),
            album = item.mediaMetadata.albumTitle?.toString(),
        )
        loadForSong(song) ?: return null
        return withContext(Dispatchers.IO) { File(dir, songKey(song) + ".png").takeIf { it.exists() }?.readBytes() }
    }

    private suspend fun loadKeyed(k: String, source: suspend () -> Bitmap?): ImageBitmap? = withContext(Dispatchers.IO) {
        memory.get(k)?.let { return@withContext it }
        if (k in none) return@withContext null
        val file = File(dir, "$k.png")
        val bitmap = if (file.exists()) {
            BitmapFactory.decodeFile(file.path)
        } else {
            val raw = try { source() } catch (_: Exception) { null }
            raw?.let { prepare(it) }?.also { out ->
                runCatching { file.outputStream().use { out.compress(Bitmap.CompressFormat.PNG, 100, it) } }
            }
        }
        if (bitmap == null) {
            none += k
            return@withContext null
        }
        bitmap.asImageBitmap().also { memory.put(k, it) }
    }

    private fun fromUrl(url: String?): Bitmap? {
        if (url.isNullOrBlank()) return null
        val request = Request.Builder().url(url).build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val bytes = response.body?.bytes() ?: return null
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
    }

    /** The picture embedded in the first of the album's files that has one. */
    private suspend fun fromFiles(album: AlbumUiModel): Bitmap? {
        // a library album (YouTube albums have their own thumbnail URL and no rows here)
        val songs = MonoMusicDatabase_songs(album.id)
        for (song in songs) {
            val uri = song.localUri ?: continue
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(app, Uri.parse(uri))
                val bytes = retriever.embeddedPicture ?: continue
                return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (_: Exception) {
            } finally {
                runCatching { retriever.release() }
            }
        }
        // a downloaded YouTube song: its thumbnail
        return songs.firstOrNull { it.isYouTube }?.let { fromVideo(it.id) }
    }

    /**
     * YouTube's still for a song. The 16:9 one (mqdefault) has the album art as its middle square;
     * the 4:3 one (hqdefault) adds black bars above and below as well.
     */
    private fun fromVideo(videoId: String): Bitmap? = fromUrl("https://i.ytimg.com/vi/$videoId/mqdefault.jpg")

    private suspend fun MonoMusicDatabase_songs(albumKey: String) =
        com.calmapps.calmmusic.data.MonoMusicDatabase.getDatabase(app).songDao().getByAlbumKey(albumKey)

    /** Square from the middle, [SIZE] px, grey, contrast stretched. */
    private fun prepare(source: Bitmap): Bitmap {
        val side = minOf(source.width, source.height)
        val src = Rect((source.width - side) / 2, (source.height - side) / 2, (source.width + side) / 2, (source.height + side) / 2)
        val out = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.RGB_565)
        val canvas = Canvas(out)
        val grey = ColorMatrix().apply { setSaturation(0f) }
        // a little more contrast: E Ink flattens mid-greys
        val contrast = 1.25f
        val shift = (1 - contrast) * 128f
        grey.postConcat(ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, shift,
            0f, contrast, 0f, 0f, shift,
            0f, 0f, contrast, 0f, shift,
            0f, 0f, 0f, 1f, 0f,
        )))
        val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply { colorFilter = ColorMatrixColorFilter(grey) }
        canvas.drawBitmap(source, src, Rect(0, 0, SIZE, SIZE), paint)
        return out
    }

    companion object {
        /** Big enough for Now Playing's cover; rows draw it smaller. */
        const val SIZE = 240
    }
}
