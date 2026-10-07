package com.calmapps.calmmusic.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.tabs.PrimaryTabRowMMD
import com.mudita.mmd.components.tabs.TabMMD
import com.mudita.mmd.components.text.TextMMD

/** An artist found on YouTube Music. */
data class ArtistResultUiModel(
    val id: String,
    val name: String,
    val subtitle: String?,
)

private val SEARCH_TABS = listOf("Songs", "Albums", "Artists", "Local")
const val SEARCH_TAB_ARTISTS = 2
const val SEARCH_TAB_LOCAL = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    isSearching: Boolean,
    errorMessage: String?,
    songs: List<SongUiModel>,
    albums: List<AlbumUiModel>,
    artists: List<ArtistResultUiModel>,
    localSongs: List<SongUiModel>,
    selectedTab: Int,
    onSelectedTabChange: (Int) -> Unit,
    onPlaySongClick: (SongUiModel) -> Unit,
    onAlbumClick: (AlbumUiModel) -> Unit,
    onArtistClick: (ArtistResultUiModel) -> Unit,
    librarySongIds: Set<String> = emptySet(),
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
                PrimaryTabRowMMD(selectedTabIndex = selectedTab) {
                    SEARCH_TABS.forEachIndexed { index, title ->
                        TabMMD(
                            selected = selectedTab == index,
                            onClick = { onSelectedTabChange(index) },
                            text = {
                                TextMMD(
                                    text = title,
                                    fontSize = 16.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                )
                            },
                        )
                    }
                }

                LazyColumnMMD(contentPadding = PaddingValues(16.dp)) {
                    if (isSearching) {
                        item {
                            TextMMD(text = "Searching...")
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    if (errorMessage != null) {
                        item {
                            TextMMD(text = "Error: $errorMessage")
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    when (selectedTab) {
                        0 -> {
                            if (songs.isNotEmpty()) {
                                items(songs.size) { index ->
                                    val song = songs[index]
                                    SongItem(
                                        song = song,
                                        isCurrentlyPlaying = false,
                                        onClick = { onPlaySongClick(song) },
                                        showDivider = song != songs.lastOrNull(),
                                        isInLibrary = librarySongIds.contains(song.id),
                                    )
                                }
                            }

                            if (
                                !isSearching &&
                                errorMessage == null &&
                                songs.isEmpty()
                            ) {
                                item {
                                    TextMMD(text = "No songs. Try a different search.")
                                }
                            }
                        }

                        1 -> {
                            if (albums.isNotEmpty()) {
                                items(albums.size) { index ->
                                    val album = albums[index]
                                    AlbumItem(
                                        album = album,
                                        onClick = { onAlbumClick(album) },
                                        showDivider = album != albums.lastOrNull(),
                                    )
                                }
                            }

                            if (
                                !isSearching &&
                                errorMessage == null &&
                                albums.isEmpty()
                            ) {
                                item {
                                    TextMMD(text = "No albums. Try a different search.")
                                }
                            }
                        }

                        SEARCH_TAB_ARTISTS -> {
                            if (artists.isNotEmpty()) {
                                items(artists.size) { index ->
                                    val artist = artists[index]
                                    ArtistResultItem(
                                        artist = artist,
                                        onClick = { onArtistClick(artist) },
                                        showDivider = index != artists.lastIndex,
                                    )
                                }
                            }

                            if (!isSearching && errorMessage == null && artists.isEmpty()) {
                                item {
                                    TextMMD(text = "No artists. Try a different search.")
                                }
                            }
                        }

                        SEARCH_TAB_LOCAL -> {
                            if (localSongs.isNotEmpty()) {
                                items(localSongs.size) { index ->
                                    val song = localSongs[index]
                                    SongItem(
                                        song = song,
                                        isCurrentlyPlaying = false,
                                        onClick = { onPlaySongClick(song) },
                                        showDivider = song != localSongs.lastOrNull(),
                                        isInLibrary = true,
                                    )
                                }
                            }

                            if (!isSearching && localSongs.isEmpty()) {
                                item {
                                    TextMMD(text = "No local songs found.")
                                }
                            }
                        }
                    }
                }
            }
    }
}

@Composable
private fun ArtistResultItem(
    artist: ArtistResultUiModel,
    onClick: () -> Unit,
    showDivider: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(bottom = 8.dp),
    ) {
        TextMMD(
            text = artist.name,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!artist.subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            TextMMD(
                text = artist.subtitle,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (showDivider) {
            Spacer(modifier = Modifier.height(8.dp))
            DashedDivider()
        }
    }
}
