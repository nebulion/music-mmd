package com.calmapps.calmmusic

import com.calmapps.calmmusic.data.Dedupe
import com.calmapps.calmmusic.data.Song
import org.junit.Assert.assertEquals
import org.junit.Test

class DedupeTest {

    private fun song(id: String, title: String, album: String? = "geogaddi", ms: Long? = 200_000, size: Long? = 5_000_000, uri: String? = "content://$id") =
        Song(id, title, "Boards of Canada", null, album, 1, 1, ms, 2002, "boc", album, uri, 0L, size)

    @Test
    fun `download and own copy of the same song are listed once, bigger file kept`() {
        val download = song("dQw4w9WgXcQ", "Music Is Math", size = 5_000_000)
        val own = song("local:abc", "Music Is Math", ms = 201_000, size = 12_000_000)
        assertEquals(listOf("local:abc"), Dedupe.songs(listOf(download, own)).map { it.id })
    }

    @Test
    fun `different lengths are different versions`() {
        val studio = song("a", "Roygbiv", ms = 151_000)
        val live = song("b", "Roygbiv", ms = 240_000)
        assertEquals(2, Dedupe.songs(listOf(studio, live)).size)
    }

    @Test
    fun `same title on different albums stays`() {
        val a = song("a", "Intro", album = "one")
        val b = song("b", "Intro", album = "two")
        assertEquals(2, Dedupe.songs(listOf(a, b)).size)
    }

    @Test
    fun `title punctuation and case do not matter`() {
        val a = song("a", "Music Is Math")
        val b = song("b", "music is math!", size = 1)
        assertEquals(listOf("a"), Dedupe.songs(listOf(a, b)).map { it.id })
    }
}
