package com.calmapps.calmmusic.ui

import com.calmapps.calmmusic.ui.kit.ListRow
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
import com.calmapps.calmmusic.ui.kit.PagedList
import com.mudita.mmd.components.tabs.PrimaryTabRowMMD
import com.mudita.mmd.components.tabs.TabMMD
import com.mudita.mmd.components.text.TextMMD

/** An artist found on YouTube Music. */
data class ArtistResultUiModel(
    val id: String,
    val name: String,
    val subtitle: String?,
)

// Owner's pick 2B: three tabs; library matches lead the Songs tab instead of a Local tab.
private val SEARCH_TABS = listOf("Songs", "Albums", "Artists")
const val SEARCH_TAB_ARTISTS = 2

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
                PrimaryTabRowMMD(selectedTabIndex = selectedTab.coerceIn(0, SEARCH_TABS.lastIndex)) {
                    SEARCH_TABS.forEachIndexed { index, title ->
                        TabMMD(
                            selected = selectedTab == index,
                            onClick = { onSelectedTabChange(index) },
                            text = {
                                TextMMD(
                                    text = title,
                                    style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                )
                            },
                        )
                    }
                }

                PagedList(contentPadding = PaddingValues(horizontal = 16.dp)) {
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
                            // your own songs first, marked ✓; then YouTube's, without repeats
                            val localIds = localSongs.mapTo(HashSet()) { it.id }
                            val combined = localSongs + songs.filter { it.id !in localIds }
                            if (combined.isNotEmpty()) {
                                items(combined.size) { index ->
                                    val song = combined[index]
                                    SongItem(
                                        song = song,
                                        isCurrentlyPlaying = false,
                                        onClick = { onPlaySongClick(song) },
                                        showDivider = index != combined.lastIndex,
                                        isInLibrary = song.id in localIds || librarySongIds.contains(song.id),
                                        markInLibrary = true,
                                    )
                                }
                            }

                            if (
                                !isSearching &&
                                errorMessage == null &&
                                combined.isEmpty()
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
    ListRow(
        title = artist.name,
        subtitle = artist.subtitle,
        bold = true,
        showDivider = showDivider,
        modifier = Modifier.clickable(onClick = onClick),
    )
}
