package com.calmapps.calmmusic.data

import android.content.Context
import android.util.AtomicFile
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/** How often and when a song was last played. */
data class PlayStat(val count: Int, val lastPlayed: Long)

/**
 * Play counts and last-played times per song id, for the "Most played" / "Recently played" sorts.
 * A play counts once a song has played for [PLAY_COUNTS_AFTER_MS]; recorded by the playback
 * service, so it works with the app closed. Started counting 2026-10-08: older plays are unknown.
 */
class PlayHistory(context: Context) {

    private val file = AtomicFile(File(context.filesDir, "play_history.json"))
    private val stats = ConcurrentHashMap<String, PlayStat>()

    init {
        try {
            val o = JSONObject(String(file.readFully(), Charsets.UTF_8))
            for (id in o.keys()) {
                val s = o.getJSONObject(id)
                stats[id] = PlayStat(s.optInt("c"), s.optLong("t"))
            }
        } catch (_: Exception) {
        }
    }

    fun of(songId: String): PlayStat? = stats[songId]

    fun snapshot(): Map<String, PlayStat> = HashMap(stats)

    @Synchronized
    fun recordPlay(songId: String, now: Long = System.currentTimeMillis()) {
        val old = stats[songId]
        stats[songId] = PlayStat((old?.count ?: 0) + 1, now)
        write()
    }

    private fun write() {
        val o = JSONObject()
        stats.forEach { (id, s) -> o.put(id, JSONObject().put("c", s.count).put("t", s.lastPlayed)) }
        val out = try { file.startWrite() } catch (_: Exception) { return }
        try {
            out.write(o.toString().toByteArray(Charsets.UTF_8))
            file.finishWrite(out)
        } catch (_: Exception) {
            file.failWrite(out)
        }
    }

    companion object {
        const val PLAY_COUNTS_AFTER_MS = 30_000L
    }
}
