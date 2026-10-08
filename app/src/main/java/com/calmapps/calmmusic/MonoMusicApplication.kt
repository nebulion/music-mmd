package com.calmapps.calmmusic

import android.app.Application
import android.content.Intent
import android.provider.Settings
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.calmapps.calmmusic.data.MonoMusicSettingsManager
import com.calmapps.calmmusic.data.NowPlayingStorage
import okhttp3.OkHttpClient
import java.io.File

@UnstableApi
class MonoMusic : Application() {

    val mediaCache: SimpleCache by lazy {
        val cacheDirectory = File(this.cacheDir, "media_cache")
        val evictor = LeastRecentlyUsedCacheEvictor(256L * 1024L * 1024L) // 256 MB
        SimpleCache(cacheDirectory, evictor)
    }

    val cacheDataSourceFactory: CacheDataSource.Factory by lazy {
        val upstream = DefaultDataSource.Factory(this)
        CacheDataSource.Factory()
            .setCache(mediaCache)
            .setUpstreamDataSourceFactory(upstream)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    val youTubeSearchClient: YouTubeMusicSearchClient by lazy {
        YouTubeMusicSearchClientImpl.create()
    }

    val youTubeInnertubeClient: YouTubeMusicInnertubeClient by lazy {
        val client = OkHttpClient.Builder().build()
        YouTubeMusicInnertubeClientImpl(client)
    }

    val youTubeStreamResolver: YouTubeStreamResolver by lazy {
        YouTubeStreamResolver()
    }

    val youTubePrecacheManager: YouTubePrecacheManager by lazy {
        YouTubePrecacheManager(this)
    }

    /**
     * Stream URL for a YouTube video and the resolver that produced it. NewPipe
     * first (it talks to YouTube directly); public Piped instances are often down,
     * so Piped is only the fallback. Unplayable content is not retried on Piped.
     */
    suspend fun resolveAudioUrl(videoId: String): Pair<String, String> =
        try {
            youTubeStreamResolver.getBestAudioUrl(videoId) to "NewPipe"
        } catch (e: Exception) {
            if (isContentUnavailable(e)) throw e
            try {
                youTubeInnertubeClient.getBestAudioUrl(videoId) to "Piped"
            } catch (_: Exception) {
                throw e
            }
        }

    /** Which resolver produced the current stream URL; shown on Now Playing. */
    val streamResolverLabel = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    /** Album, artist and playlist page content; see [PageCache]. */
    val pageCache = PageCache()

    val uiStateStore: com.calmapps.calmmusic.data.UiStateStore by lazy {
        com.calmapps.calmmusic.data.UiStateStore(this)
    }

    val nowPlayingStorage: NowPlayingStorage by lazy {
        NowPlayingStorage(this)
    }

    lateinit var settingsManager: MonoMusicSettingsManager
        private set

    lateinit var youTubeDownloadManager: YouTubeDownloadManager
        private set

    override fun onCreate() {
        super.onCreate()

        // Flash the screen black every N taps to clear E Ink ghosting (Settings → E Ink).
        com.calmapps.calmmusic.ui.kit.EinkRefresh.install(this)
        settingsManager = MonoMusicSettingsManager(this)
        youTubeDownloadManager = YouTubeDownloadManager(
            app = this,
            appScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO),
        )

    }

}
