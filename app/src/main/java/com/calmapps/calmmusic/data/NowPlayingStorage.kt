package com.calmapps.calmmusic.data

import android.content.Context
import android.content.SharedPreferences
import com.calmapps.calmmusic.ui.SongUiModel
import org.json.JSONArray

/**
 * Lightweight persistence layer for capturing the now playing snapshot so it
 * can survive process death and background kills.
 */
data class NowPlayingSnapshot(
    val queueSongIds: List<String>,
    val currentIndex: Int?,
    val isPlaying: Boolean,
    val positionMs: Long,
    val repeatModeKey: String,
    val isShuffleOn: Boolean,
    /** The queue's songs, so YouTube songs that aren't in the library come back too. */
    val queueSongs: List<SongUiModel> = emptyList(),
)

class NowPlayingStorage(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(snapshot: NowPlayingSnapshot) {
        prefs.edit()
            .putString(KEY_QUEUE_IDS, snapshot.queueSongIds.joinToString(DELIMITER))
            .putInt(KEY_INDEX, snapshot.currentIndex ?: INDEX_NONE)
            .putBoolean(KEY_IS_PLAYING, snapshot.isPlaying)
            .putLong(KEY_POSITION_MS, snapshot.positionMs)
            .putString(KEY_REPEAT_MODE, snapshot.repeatModeKey)
            .putBoolean(KEY_IS_SHUFFLE_ON, snapshot.isShuffleOn)
            .putString(KEY_QUEUE_SONGS, snapshot.queueSongs.songsToJson().toString())
            .apply()
    }

    fun load(): NowPlayingSnapshot? {
        val queueStr = prefs.getString(KEY_QUEUE_IDS, null) ?: return null
        val ids = queueStr
            .takeIf { it.isNotEmpty() }
            ?.split(DELIMITER)
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
        if (ids.isEmpty()) return null

        val indexRaw = prefs.getInt(KEY_INDEX, INDEX_NONE)
        val index = if (indexRaw >= 0) indexRaw else null
        val isPlaying = prefs.getBoolean(KEY_IS_PLAYING, false)
        val positionMs = prefs.getLong(KEY_POSITION_MS, 0L)
        val repeatModeKey = prefs.getString(KEY_REPEAT_MODE, NowPlayingRepeatModeKeys.OFF) ?: NowPlayingRepeatModeKeys.OFF
        val isShuffleOn = prefs.getBoolean(KEY_IS_SHUFFLE_ON, false)
        val queueSongs = try {
            prefs.getString(KEY_QUEUE_SONGS, null)?.let { JSONArray(it) }.toSongs()
        } catch (_: Exception) {
            emptyList()
        }

        return NowPlayingSnapshot(
            queueSongIds = ids,
            currentIndex = index,
            isPlaying = isPlaying,
            positionMs = positionMs,
            repeatModeKey = repeatModeKey,
            isShuffleOn = isShuffleOn,
            queueSongs = queueSongs,
        )
    }

    companion object {
        private const val PREFS_NAME = "now_playing"
        private const val KEY_QUEUE_IDS = "queue_ids"
        private const val KEY_INDEX = "current_index"
        private const val KEY_IS_PLAYING = "is_playing"
        private const val KEY_POSITION_MS = "position_ms"
        private const val KEY_REPEAT_MODE = "repeat_mode"
        private const val KEY_IS_SHUFFLE_ON = "is_shuffle_on"
        private const val KEY_QUEUE_SONGS = "queue_songs"
        private const val DELIMITER = ","
        private const val INDEX_NONE = -1
    }
}

object NowPlayingRepeatModeKeys {
    const val OFF = "off"
    const val QUEUE = "queue"
    const val ONE = "one"
}
