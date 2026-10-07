package com.calmapps.calmmusic

import com.calmapps.calmmusic.data.UiState
import com.calmapps.calmmusic.data.UiStateStore
import com.calmapps.calmmusic.ui.AlbumUiModel
import com.calmapps.calmmusic.ui.SongUiModel
import org.junit.Assert.assertEquals
import org.junit.Test

class UiStateStoreTest {

    @Test
    fun `last search and open album survive a round trip`() {
        val state = UiState(
            searchQuery = "radiohead",
            searchTab = 1,
            searchSongs = listOf(SongUiModel(id = "abc", title = "Reckoner", artist = "Radiohead", durationMillis = 290_000L, album = "In Rainbows")),
            searchAlbums = listOf(AlbumUiModel(id = "MPREb_x", title = "In Rainbows", artist = "Radiohead", sourceType = "YOUTUBE", releaseYear = 2007)),
            selectedAlbum = AlbumUiModel(id = "MPREb_x", title = "In Rainbows", artist = "Radiohead", sourceType = "YOUTUBE", releaseYear = 2007),
            selectedArtist = "Radiohead",
            selectedArtistId = "UC123",
            showNowPlaying = true,
        )
        assertEquals(state, UiStateStore.decode(UiStateStore.encode(state)))
    }

    @Test
    fun `empty state round trips`() {
        assertEquals(UiState(), UiStateStore.decode(UiStateStore.encode(UiState())))
    }
}
