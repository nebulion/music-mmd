package com.calmapps.calmmusic.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calmapps.calmmusic.MonoMusicViewModel
import com.mudita.mmd.components.buttons.FloatingActionButtonMMD
import com.calmapps.calmmusic.ui.kit.PagedList
import com.mudita.mmd.components.tabs.PrimaryTabRowMMD
import com.mudita.mmd.components.tabs.TabMMD
import com.mudita.mmd.components.text.TextMMD

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailsScreen(
    artistId: String?,
    viewModel: MonoMusicViewModel,
    onPlaySongClick: (SongUiModel, List<SongUiModel>) -> Unit,
    onAlbumClick: (AlbumUiModel) -> Unit,
    /** The songs shuffle would play, or null when there are none (leaving the page clears it). */
    onShuffleAvailable: (List<SongUiModel>?) -> Unit,
    onAddToPlaylistClick: (SongUiModel) -> Unit,
    onRemoveFromLibraryClick: (SongUiModel) -> Unit,
    onDeleteClick: (SongUiModel) -> Unit,
) {
    var songs by remember { mutableStateOf<List<SongUiModel>>(emptyList()) }
    var albums by remember { mutableStateOf<List<AlbumUiModel>>(emptyList()) }
    var singles by remember { mutableStateOf<List<AlbumUiModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    // Singles only when the artist has any (YouTube artists); at most three tabs.
    val tabOptions = if (singles.isEmpty()) listOf("Albums", "Songs") else listOf("Albums", "Singles", "Songs")
    val songsTab = tabOptions.lastIndex

    val playbackState by viewModel.playbackState.collectAsState()
    val currentSongId = playbackState.currentSongId

    val refreshTrigger by viewModel.libraryRefreshTrigger.collectAsState()

    val shownSongs = if (!isLoading && errorMessage == null && songs.isNotEmpty()) songs else null
    LaunchedEffect(shownSongs) { onShuffleAvailable(shownSongs) }
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { onShuffleAvailable(null) } }

    LaunchedEffect(artistId, refreshTrigger) {
        if (artistId == null) {
            isLoading = false
            return@LaunchedEffect
        }
        isLoading = true
        errorMessage = null
        try {
            val content = viewModel.getArtistContent(artistId)
            songs = content.songs
            albums = content.albums
            singles = content.singles
            if (albums.isEmpty() && singles.isEmpty() && songs.isNotEmpty()) selectedTab = 1
        } catch (e: Exception) {
            errorMessage = e.message ?: "Failed to load artist"
        } finally {
            isLoading = false
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    TextMMD(text = "Loading artist...")
                }
            }

            errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        TextMMD(text = "Error loading artist")
                        TextMMD(text = errorMessage!!)
                    }
                }
            }

            songs.isEmpty() && albums.isEmpty() && singles.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    TextMMD(text = "No content for this artist yet")
                }
            }

            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    PrimaryTabRowMMD(selectedTabIndex = selectedTab) {
                        tabOptions.forEachIndexed { index, title ->
                            TabMMD(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = {
                                    TextMMD(
                                        text = title,
                                        style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    )
                                },
                            )
                        }
                    }

                    if (selectedTab < songsTab) {
                        // Albums or Singles tab
                        val shown = if (selectedTab == 0) albums else singles
                        PagedList(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            verticalArrangement = Arrangement.Top,
                        ) {
                            if (shown.isNotEmpty()) {
                                items(shown) { album ->
                                    AlbumItem(
                                        album = album,
                                        onClick = { onAlbumClick(album) },
                                        showDivider = album != shown.lastOrNull(),
                                    )
                                }
                            } else {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .height(200.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        TextMMD(text = "No albums for this artist")
                                    }
                                }
                            }
                        }
                    } else {
                        // Songs tab
                        PagedList(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            verticalArrangement = Arrangement.Top,
                        ) {
                            if (songs.isNotEmpty()) {
                                items(songs) { song ->
                                    SongItem(
                                        song = song,
                                        isCurrentlyPlaying = song.id == currentSongId,
                                        onClick = { onPlaySongClick(song, songs) },
                                        onAddToPlaylist = { onAddToPlaylistClick(song) },
                                        onRemoveFromLibrary = { onRemoveFromLibraryClick(song) },
                                        onDelete = { onDeleteClick(song) },
                                        showDivider = song != songs.lastOrNull(),
                                    )
                                }
                            } else {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .height(200.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        TextMMD(text = "No songs for this artist")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }


    }
}