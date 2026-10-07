package com.calmapps.calmmusic

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Saved YouTube Music responses for "radiohead" (2026-10-07), trimmed of thumbnails. */
class InnertubeArtistParserTest {

    private fun fixture(name: String) =
        JSONObject(javaClass.getResource("/innertube/$name")!!.readText())

    @Test
    fun `artist search returns artists with their channel ids`() {
        val artists = InnertubeArtistParser.parseArtistSearch(fixture("search_artists.json"), 25)
        val first = artists.first()
        assertEquals("Radiohead", first.name)
        assertEquals("UCr_iyUANcn9OX_yy9piYoLw", first.artistId)
        assertTrue(artists.any { it.name == "Thom Yorke" })
        assertTrue(artists.all { it.artistId.startsWith("UC") })
    }

    @Test
    fun `artist page has top songs, albums, singles and the full-list links`() {
        val page = InnertubeArtistParser.parseArtistPage("UCr_iyUANcn9OX_yy9piYoLw", fixture("artist_page.json"))
        assertEquals("Radiohead", page.name)
        assertTrue(page.topSongs.isNotEmpty())
        assertTrue(page.topSongs.all { it.videoId.isNotBlank() })
        assertTrue(page.albums.any { it.albumId.startsWith("MPREb_") })
        assertTrue(page.singles.isNotEmpty())
        assertNotNull(page.allAlbums)
        assertEquals("MPADUCr_iyUANcn9OX_yy9piYoLw", page.allAlbums!!.browseId)
        assertNotNull(page.allSingles)
    }

    @Test
    fun `discography lists every album with its year`() {
        val albums = InnertubeArtistParser.parseDiscography(fixture("discography_albums.json"), "Radiohead")
        assertEquals(15, albums.size)
        val okComputer = albums.first { it.title == "OK Computer" }
        assertEquals(1997, okComputer.year)
        assertEquals("MPREb_yXhSI4FCUo6", okComputer.albumId)
        assertEquals("Radiohead", okComputer.artist)
    }

    @Test
    fun `durations parse`() {
        assertEquals(290_000L, InnertubeArtistParser.parseDuration("4:50"))
        assertEquals(3_723_000L, InnertubeArtistParser.parseDuration("1:02:03"))
    }
}
