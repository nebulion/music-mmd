package com.calmapps.calmmusic.data

import kotlin.math.abs

/**
 * One row per song in the library (owner, 2026-10-08: "things show up twice in an album").
 *
 * A song can be on the phone twice: a download in Music/MonoMusic and the owner's own copy in a
 * folder added in Settings. They are different files (different content), so the scan keeps both
 * rows. Here, songs in the same album (or, without an album, by the same artist) with the same
 * title and about the same length (within [SAME_LENGTH_MS]) count as one; the copy kept is the
 * bigger file (usually the better encoding), then the one with a YouTube identity. Nothing is
 * deleted: this only decides what lists show, so it undoes itself if a file goes.
 * Different versions ("Intro" on two albums, a live take of other length) are left alone.
 */
object Dedupe {
    const val SAME_LENGTH_MS = 3_000L

    private fun norm(s: String) = s.lowercase().replace(Regex("[^\\p{L}\\p{Nd}]"), "")

    fun songs(all: List<Song>): List<Song> {
        val groups = all.groupBy { (it.albumKey ?: ("artist:" + it.artistKey)) to norm(it.title) }
        val drop = HashSet<String>()
        for ((_, group) in groups) {
            if (group.size < 2) continue
            // within a title, cluster by length so different versions stay apart
            val remaining = group.sortedWith(keeperOrder).toMutableList()
            while (remaining.isNotEmpty()) {
                val keeper = remaining.removeAt(0)
                val twins = remaining.filter { same(keeper, it) }
                twins.forEach { drop += it.id }
                remaining.removeAll(twins)
            }
        }
        return if (drop.isEmpty()) all else all.filter { it.id !in drop }
    }

    private fun same(a: Song, b: Song): Boolean {
        val da = a.durationMillis
        val db = b.durationMillis
        return da == null || db == null || abs(da - db) <= SAME_LENGTH_MS
    }

    /** Best copy first: on the phone, bigger file, YouTube identity, then a stable order. */
    private val keeperOrder = compareByDescending<Song> { it.localUri != null }
        .thenByDescending { it.localSizeBytes ?: 0L }
        .thenByDescending { it.isYouTube }
        .thenBy { it.id }
}
