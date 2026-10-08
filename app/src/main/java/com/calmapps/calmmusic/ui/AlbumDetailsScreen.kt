package com.calmapps.calmmusic.ui

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
fun AlbumDetailsScreen(
    album: AlbumUiModel?,
    viewModel: MonoMusicViewModel,
    onPlaySongClick: (SongUiModel, List<SongUiModel>) -> Unit,
    onShuffleClick: (List<SongUiModel>) -> Unit,
    onEditSongClick: (SongUiModel) -> Unit,
    onAddToPlaylistClick: (SongUiModel) -> Unit,
    onRemoveFromLibraryClick: (SongUiModel) -> Unit,
    onDeleteClick: (SongUiModel) -> Unit,
    librarySongIds: Set<String> = emptySet(),
) {
    // Opened only once its songs are cached (see PageCache): drawn complete, in one paint.
    val cached = remember(album?.id) { album?.let { viewModel.cachedAlbumSongs(it) } }
    var songs by remember(album?.id) { mutableStateOf(cached ?: emptyList()) }
    var isLoading by remember(album?.id) { mutableStateOf(album != null && cached == null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val playbackState by viewModel.playbackState.collectAsState()
    val currentSongId = playbackState.currentSongId

    val refreshTrigger by viewModel.libraryRefreshTrigger.collectAsState()

    LaunchedEffect(album?.id, album?.sourceType, refreshTrigger) {
        if (album == null) {
            isLoading = false
            return@LaunchedEffect
        }
        try {
            // a quiet refresh: the page only changes if the songs did
            val fresh = viewModel.loadAlbumSongs(album)
            if (fresh != songs) songs = fresh
            errorMessage = null
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e // the page was left or reloaded: not an error
        } catch (e: Exception) {
            if (songs.isEmpty()) errorMessage = e.message ?: "Couldn't load this album"
        } finally {
            isLoading = false
        }
    }

    val discNumbers = remember(songs) {
        songs.map { it.discNumber ?: 1 }.distinct().sorted()
    }

    var selectedDiscIndex by remember(discNumbers) { mutableIntStateOf(0) }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    com.calmapps.calmmusic.ui.kit.DelayedText("Loading")
                }
            }

            errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    TextMMD(text = errorMessage!!)
                }
            }

            songs.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    TextMMD(text = "No songs in this album")
                }
            }

            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (album != null) AlbumHeader(album, songs)
                    if (discNumbers.size > 1) {
                        PrimaryTabRowMMD(selectedTabIndex = selectedDiscIndex) {
                            discNumbers.forEachIndexed { index, disc ->
                                TabMMD(
                                    selected = selectedDiscIndex == index,
                                    onClick = { selectedDiscIndex = index },
                                    text = {
                                        TextMMD(
                                            text = "Disc $disc",
                                            style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                                            fontWeight = if (selectedDiscIndex == index) FontWeight.Bold else FontWeight.Normal,
                                        )
                                    },
                                )
                            }
                        }
                    }

                    val currentDiscNumber = discNumbers.getOrElse(selectedDiscIndex) { 1 }
                    val displaySongs = if (discNumbers.size > 1) {
                        songs.filter { (it.discNumber ?: 1) == currentDiscNumber }
                    } else {
                        songs
                    }

                    PagedList(contentPadding = PaddingValues(horizontal = 16.dp)) {
                        items(displaySongs.size) { index ->
                            val song = displaySongs[index]
                            SongItem(
                                song = song,
                                isCurrentlyPlaying = song.id == currentSongId,
                                onClick = {
                                    onPlaySongClick(song, songs)
                                },
                                onEdit = { onEditSongClick(song) },
                                onAddToPlaylist = { onAddToPlaylistClick(song) },
                                onRemoveFromLibrary = { onRemoveFromLibraryClick(song) },
                                onDelete = { onDeleteClick(song) },
                                showDivider = song != displaySongs.lastOrNull(),
                                isInLibrary = librarySongIds.contains(song.id),
                            )
                        }
                    }
                }
            }
        }

        if (!isLoading && errorMessage == null && songs.isNotEmpty()) {
            FloatingActionButtonMMD(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                onClick = { onShuffleClick(songs) },
            ) {
                Icon(
                    imageVector = Icons.Outlined.Shuffle,
                    contentDescription = "Shuffle album",
                )
            }
        }
    }
}

/**
 * Which album this is, at a glance: its cover (Settings → Album covers), artist, year and songs.
 * The cover was loaded before the page opened, so the header is drawn complete.
 */
@Composable
private fun AlbumHeader(album: AlbumUiModel, songs: List<SongUiModel>) {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as com.calmapps.calmmusic.MonoMusic
    val showCover = com.calmapps.calmmusic.ui.kit.coversShown()
    val cover = remember(album.sourceType, album.id) { app.covers.peek(album) }
    val onPhone = songs.count { it.sourceType != "YOUTUBE" }
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showCover) {
            com.calmapps.calmmusic.ui.kit.CoverBox(cover, 96.dp)
            androidx.compose.foundation.layout.Spacer(Modifier.width(16.dp))
        }
        Column(Modifier.weight(1f)) {
            album.artist?.takeIf { it.isNotBlank() }?.let {
                TextMMD(it, style = androidx.compose.material3.MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            TextMMD(
                listOfNotNull(album.releaseYear?.toString(), if (songs.size == 1) "1 song" else "${songs.size} songs").joinToString(" · "),
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            )
            TextMMD(
                when (onPhone) {
                    0 -> "On YouTube Music"
                    songs.size -> "On this phone"
                    else -> "$onPhone of ${songs.size} on this phone"
                },
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            )
        }
    }
    com.mudita.mmd.components.divider.HorizontalDividerMMD(thickness = 1.dp)
}
