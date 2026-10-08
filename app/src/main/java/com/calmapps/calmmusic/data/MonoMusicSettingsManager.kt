package com.calmapps.calmmusic.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MonoMusicSettingsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _includeLocalMusic = MutableStateFlow(getIncludeLocalMusicSync())
    val includeLocalMusic: StateFlow<Boolean> = _includeLocalMusic.asStateFlow()

    private val _localMusicFolders = MutableStateFlow(getLocalMusicFoldersSync())
    val localMusicFolders: StateFlow<Set<String>> = _localMusicFolders.asStateFlow()

    private val _completeAlbumsWithYouTube = MutableStateFlow(getCompleteAlbumsWithYouTubeSync())
    val completeAlbumsWithYouTube: StateFlow<Boolean> = _completeAlbumsWithYouTube.asStateFlow()

    fun getIdentifyFailedIds(): Set<String> =
        prefs.getStringSet(KEY_IDENTIFY_FAILED_IDS, emptySet()) ?: emptySet()

    fun addIdentifyFailedIds(ids: Set<String>) {
        prefs.edit { putStringSet(KEY_IDENTIFY_FAILED_IDS, getIdentifyFailedIds() + ids) }
    }

    fun getLastLocalLibraryScanMillis(): Long {
        return prefs.getLong(KEY_LAST_LOCAL_LIBRARY_SCAN_MILLIS, 0L)
    }

    fun updateLastLocalLibraryScanMillis(value: Long) {
        prefs.edit { putLong(KEY_LAST_LOCAL_LIBRARY_SCAN_MILLIS, value) }
    }

    fun setIncludeLocalMusic(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_INCLUDE_LOCAL_MUSIC, enabled) }
        _includeLocalMusic.value = enabled
    }

    // Always on (owner, 2026-10-08: settings that should just be on are gone): music on the phone
    // is always part of the library.
    private fun getIncludeLocalMusicSync(): Boolean = true

    fun addLocalMusicFolder(uri: String) {
        val current = getLocalMusicFoldersSync().toMutableSet()
        if (current.add(uri)) {
            prefs.edit { putStringSet(KEY_LOCAL_MUSIC_FOLDERS, current) }
            _localMusicFolders.value = current
        }
    }

    fun removeLocalMusicFolder(uri: String) {
        val current = getLocalMusicFoldersSync().toMutableSet()
        if (current.remove(uri)) {
            prefs.edit { putStringSet(KEY_LOCAL_MUSIC_FOLDERS, current) }
            _localMusicFolders.value = current
        }
    }

    fun getLocalMusicFoldersSync(): Set<String> {
        return prefs.getStringSet(KEY_LOCAL_MUSIC_FOLDERS, emptySet()) ?: emptySet()
    }

    // Always on: an album page shows the album's whole track list, missing songs marked.
    fun getCompleteAlbumsWithYouTubeSync(): Boolean = true

    fun setCompleteAlbumsWithYouTube(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_COMPLETE_ALBUMS_WITH_YOUTUBE, enabled) }
        _completeAlbumsWithYouTube.value = enabled
    }

    private val _showAlbumCovers = MutableStateFlow(prefs.getBoolean(KEY_SHOW_ALBUM_COVERS, true))
    /** Settings → Album covers (owner, 2026-10-08), on by default. */
    val showAlbumCovers: StateFlow<Boolean> = _showAlbumCovers.asStateFlow()

    fun setShowAlbumCovers(shown: Boolean) {
        prefs.edit { putBoolean(KEY_SHOW_ALBUM_COVERS, shown) }
        _showAlbumCovers.value = shown
    }

    /** Highest download bitrate, kbps (YouTube audio comes at about 70, 128 or 160). */
    fun getDownloadQualityKbps(): Int = prefs.getInt(KEY_DOWNLOAD_QUALITY_KBPS, 160)

    fun setDownloadQualityKbps(kbps: Int) {
        prefs.edit { putInt(KEY_DOWNLOAD_QUALITY_KBPS, kbps) }
    }

    /** Downloads wait for Wi-Fi (an unmetered network) instead of using mobile data. */
    fun getDownloadOnWifiOnly(): Boolean = prefs.getBoolean(KEY_DOWNLOAD_WIFI_ONLY, false)

    fun setDownloadOnWifiOnly(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_DOWNLOAD_WIFI_ONLY, enabled) }
    }

    // Permissions onboarding
    fun hasCompletedPermissionsOnboarding(): Boolean {
        return prefs.getBoolean(KEY_HAS_COMPLETED_PERMISSIONS_ONBOARDING, false)
    }

    fun setHasCompletedPermissionsOnboarding(completed: Boolean) {
        prefs.edit { putBoolean(KEY_HAS_COMPLETED_PERMISSIONS_ONBOARDING, completed) }
    }

    companion object {
        private const val PREFS_NAME = "calmmusic_settings"
        private const val KEY_IDENTIFY_FAILED_IDS = "identify_failed_ids"
        private const val KEY_INCLUDE_LOCAL_MUSIC = "include_local_music"
        private const val KEY_LOCAL_MUSIC_FOLDERS = "local_music_folders"
        private const val KEY_LAST_LOCAL_LIBRARY_SCAN_MILLIS = "last_local_library_scan_millis"
        private const val KEY_HAS_COMPLETED_PERMISSIONS_ONBOARDING = "has_completed_permissions_onboarding"
        private const val KEY_COMPLETE_ALBUMS_WITH_YOUTUBE = "complete_albums_with_youtube"
        private const val KEY_DOWNLOAD_QUALITY_KBPS = "download_quality_kbps"
        private const val KEY_DOWNLOAD_WIFI_ONLY = "download_wifi_only"
        private const val KEY_SHOW_ALBUM_COVERS = "show_album_covers"
    }
}