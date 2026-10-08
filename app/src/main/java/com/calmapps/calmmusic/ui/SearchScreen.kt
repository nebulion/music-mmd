package com.calmapps.calmmusic.ui

import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
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
    query: String = "",
    onEditSearch: () -> Unit = {},
    /** Results from all of YouTube; null until asked for. */
    videos: List<SongUiModel>? = null,
    isSearchingVideos: Boolean = false,
    onSearchVideos: () -> Unit = {},
    onPlayVideoClick: (SongUiModel) -> Unit = {},
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
                            com.calmapps.calmmusic.ui.kit.DelayedText(
                                "Searching",
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                            )
                        }
                    }

                    if (errorMessage != null) {
                        item {
                            TextMMD(text = "Error: $errorMessage")
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    val localIds = localSongs.mapTo(HashSet()) { it.id }
                    val found = listOf(
                        localSongs.size + songs.count { it.id !in localIds },
                        albums.size,
                        artists.size,
                    )
                    val emptyHere = !isSearching && errorMessage == null && query.isNotBlank()

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

                            if (query.isNotBlank() && !isSearching) {
                                allOfYouTube(videos, isSearchingVideos, onSearchVideos, onPlayVideoClick)
                            }

                            if (emptyHere && combined.isEmpty() && videos == null) {
                                item {
                                    EmptyTab(query, found, onSelectedTabChange, onEditSearch, onSearchVideos)
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

                            if (emptyHere && albums.isEmpty()) {
                                item {
                                    EmptyTab(query, found, onSelectedTabChange, onEditSearch, onSearchVideos)
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

                            if (emptyHere && artists.isEmpty()) {
                                item {
                                    EmptyTab(query, found, onSelectedTabChange, onEditSearch, onSearchVideos)
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

/**
 * A tab with nothing in it offers the next step instead of a message (UI-PATTERNS A3: "a spot
 * that changes with the moment"): the tabs that did find something, one tap each; when nothing
 * was found anywhere, one row that puts you back in the search field.
 */
@Composable
private fun EmptyTab(
    query: String,
    found: List<Int>,
    onSelectTab: (Int) -> Unit,
    onEditSearch: () -> Unit,
    onSearchVideos: () -> Unit,
) {
    val elsewhere = found.withIndex().filter { it.value > 0 }
    Column {
        if (elsewhere.isEmpty()) {
            com.calmapps.calmmusic.ui.kit.ListRow(
                title = "Nothing for \u201c$query\u201d",
                subtitle = "Tap to change the search",
                modifier = Modifier.clickable(onClick = onEditSearch),
            )
            com.calmapps.calmmusic.ui.kit.ListRow(
                title = "Search all of YouTube",
                subtitle = "Videos, live versions, uploads",
                showDivider = false,
                modifier = Modifier.clickable(onClick = onSearchVideos),
            )
        } else {
            elsewhere.forEachIndexed { i, (tab, count) ->
                com.calmapps.calmmusic.ui.kit.ListRow(
                    title = SEARCH_TABS[tab],
                    subtitle = "$count found",
                    showDivider = i != elsewhere.lastIndex,
                    modifier = Modifier.clickable { onSelectTab(tab) },
                    trailing = {
                        androidx.compose.material3.Icon(
                            androidx.compose.material.icons.Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                        )
                    },
                )
            }
        }
    }
}

/**
 * The end of the Songs tab: "Search all of YouTube" until asked, then the videos found
 * (owner, 2026-10-08: for what YouTube Music doesn't have).
 */
private fun androidx.compose.foundation.lazy.LazyListScope.allOfYouTube(
    videos: List<SongUiModel>?,
    isSearching: Boolean,
    onSearch: () -> Unit,
    onPlay: (SongUiModel) -> Unit,
) {
    when {
        videos == null -> item(key = "all-youtube") {
            com.calmapps.calmmusic.ui.kit.ListRow(
                title = if (isSearching) "Searching all of YouTube" else "Search all of YouTube",
                subtitle = "Videos, live versions, uploads",
                showDivider = false,
                modifier = Modifier.clickable(enabled = !isSearching, onClick = onSearch),
            )
        }
        else -> {
            item(key = "videos-title") { com.calmapps.calmmusic.ui.kit.SectionTitle("All of YouTube") }
            if (videos.isEmpty()) {
                item(key = "videos-none") { com.calmapps.calmmusic.ui.kit.ListRow(title = "No videos either", showDivider = false) }
            }
            videos.forEachIndexed { i, video ->
                item(key = "v:" + video.id) {
                    SongItem(
                        song = video,
                        isCurrentlyPlaying = false,
                        onClick = { onPlay(video) },
                        showDivider = i != videos.lastIndex,
                    )
                }
            }
        }
    }
}
