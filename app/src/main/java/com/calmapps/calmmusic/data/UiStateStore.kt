package com.calmapps.calmmusic.data

import android.content.Context
import android.util.AtomicFile
import com.calmapps.calmmusic.ui.AlbumUiModel
import com.calmapps.calmmusic.ui.ArtistResultUiModel
import com.calmapps.calmmusic.ui.SongUiModel
import org.json.JSONObject
import java.io.File

/**
 * Where the owner was: last search (query, tab, results), the open album or
 * artist, and whether Now Playing was open. The Kompakt runs with "Don't keep
 * activities", and low memory kills the process, so plain `remember` state is
 * gone after every trip to the home screen. Written when the app goes to the
 * background; read once per process.
 */
data class UiState(
    val searchQuery: String = "",
    val searchTab: Int = 0,
    val searchSongs: List<SongUiModel> = emptyList(),
    val searchAlbums: List<AlbumUiModel> = emptyList(),
    val searchArtists: List<ArtistResultUiModel> = emptyList(),
    val searchLocalSongs: List<SongUiModel> = emptyList(),
    val selectedAlbum: AlbumUiModel? = null,
    val selectedArtist: String? = null,
    val selectedArtistId: String? = null,
    val showNowPlaying: Boolean = false,
)

class UiStateStore(context: Context) {

    private val file = AtomicFile(File(context.filesDir, "ui_state.json"))

    /** Survives activity recreation without touching the disk. */
    @Volatile
    private var cached: UiState? = null

    fun load(): UiState {
        cached?.let { return it }
        val state = try {
            decode(JSONObject(String(file.readFully(), Charsets.UTF_8)))
        } catch (_: Exception) {
            UiState()
        }
        cached = state
        return state
    }

    fun save(state: UiState) {
        if (state == cached) return
        cached = state
        val bytes = encode(state).toString().toByteArray(Charsets.UTF_8)
        val out = try { file.startWrite() } catch (_: Exception) { return }
        try {
            out.write(bytes)
            file.finishWrite(out)
        } catch (_: Exception) {
            file.failWrite(out)
        }
    }

    companion object {
        fun encode(s: UiState): JSONObject = JSONObject()
            .put("searchQuery", s.searchQuery)
            .put("searchTab", s.searchTab)
            .put("searchSongs", s.searchSongs.songsToJson())
            .put("searchAlbums", s.searchAlbums.albumsToJson())
            .put("searchArtists", s.searchArtists.artistsToJson())
            .put("searchLocalSongs", s.searchLocalSongs.songsToJson())
            .putOpt("selectedAlbum", s.selectedAlbum?.toJson())
            .putOpt("selectedArtist", s.selectedArtist)
            .putOpt("selectedArtistId", s.selectedArtistId)
            .put("showNowPlaying", s.showNowPlaying)

        fun decode(o: JSONObject): UiState = UiState(
            searchQuery = o.optString("searchQuery"),
            searchTab = o.optInt("searchTab"),
            searchSongs = o.optJSONArray("searchSongs").toSongs(),
            searchAlbums = o.optJSONArray("searchAlbums").toAlbums(),
            searchArtists = o.optJSONArray("searchArtists").toArtistResults(),
            searchLocalSongs = o.optJSONArray("searchLocalSongs").toSongs(),
            selectedAlbum = o.optJSONObject("selectedAlbum")?.let { runCatching { albumFromJson(it) }.getOrNull() },
            selectedArtist = if (o.has("selectedArtist")) o.getString("selectedArtist") else null,
            selectedArtistId = if (o.has("selectedArtistId")) o.getString("selectedArtistId") else null,
            showNowPlaying = o.optBoolean("showNowPlaying"),
        )
    }
}
