package com.calmapps.calmmusic

import com.mudita.mmd.components.divider.HorizontalDividerMMD
import androidx.compose.ui.Alignment
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.navigation.NavDestination.Companion.hierarchy
import com.calmapps.calmmusic.ui.AlbumUiModel
import com.calmapps.calmmusic.ui.PlaylistUiModel
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.nav_bar.NavigationBarItemMMD
import com.mudita.mmd.components.nav_bar.NavigationBarMMD
import com.mudita.mmd.components.search_bar.SearchBarDefaultsMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD
import com.mudita.mmd.components.menus.DropdownMenuItemMMD
import com.mudita.mmd.components.menus.DropdownMenuMMD
import com.calmapps.calmmusic.ui.DashedDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.graphics.vector.rememberVectorPainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonoMusicTopAppBar(
    currentDestination: NavDestination?,
    canNavigateBack: Boolean,
    focusRequester: FocusRequester,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onPerformSearchClick: () -> Unit,
    selectedAlbum: AlbumUiModel?,
    selectedArtistName: String?,
    selectedPlaylist: PlaylistUiModel?,
    isPlaylistsEditMode: Boolean,
    isPlaylistDetailsEditMode: Boolean,
    playlistEditSelectionCount: Int,
    playlistDetailsSelectionCount: Int,
    isPlaylistDetailsMenuExpanded: Boolean,
    canDownloadSelectedAlbum: Boolean,
    canRenameSelectedAlbum: Boolean,
    onArtistShuffleClick: (() -> Unit)?,
    onBackClick: () -> Unit,
    onCancelPlaylistsEditClick: () -> Unit,
    onCancelPlaylistDetailsEditClick: () -> Unit,
    onEnterPlaylistsEditClick: () -> Unit,
    onNavigateToSearchClick: () -> Unit,
    onPlaylistDetailsMenuToggle: () -> Unit,
    onPlaylistDetailsEditClick: () -> Unit,
    onPlaylistDetailsAddSongsClick: () -> Unit,
    onPlaylistDetailsRenameClick: () -> Unit,
    onPlaylistDetailsDeleteClick: () -> Unit,
    onAlbumDownloadClick: () -> Unit,
    onAlbumRenameClick: () -> Unit,
    onShowDeletePlaylistSongsConfirmationClick: () -> Unit,
    onShowDeletePlaylistsConfirmationClick: () -> Unit,
    onPlaylistAddSongsDoneClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navRoutes = remember { navItems.map { it.route } }
    val keyboardController = LocalSoftwareKeyboardController.current

    TopAppBarMMD(
        navigationIcon = {
            when {
                currentDestination?.route == Screen.PlaylistDetails.route && isPlaylistDetailsEditMode -> {
                    IconButton(onClick = onCancelPlaylistDetailsEditClick) {
                        Icon(
                            imageVector = Icons.Outlined.Clear,
                            contentDescription = "Cancel playlist edits",
                        )
                    }
                }

                currentDestination?.route == Screen.Playlists.route && isPlaylistsEditMode -> {
                    IconButton(onClick = onCancelPlaylistsEditClick) {
                        Icon(
                            imageVector = Icons.Outlined.Clear,
                            contentDescription = "Cancel playlist edit",
                        )
                    }
                }

                canNavigateBack && currentDestination?.route !in navRoutes -> {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                }
            }
        },
        title = {
            when {
                currentDestination?.route == Screen.Search.route -> {
                    SearchBarDefaultsMMD.InputField(
                        query = searchQuery,
                        onQueryChange = onSearchQueryChange,
                        onSearch = {
                            keyboardController?.hide()
                            onPerformSearchClick()
                        },
                        expanded = true,
                        onExpandedChange = { },
                        placeholder = { TextMMD("Search") },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    keyboardController?.hide()
                                    onPerformSearchClick()
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = "Search",
                                )
                            }
                        },
                        modifier = Modifier
                            .focusRequester(focusRequester),
                    )
                }

                currentDestination?.route == Screen.AlbumDetails.route && selectedAlbum != null -> {
                    androidx.compose.foundation.layout.Column {
                        Text(
                            text = selectedAlbum.title,
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val artist = selectedAlbum.artist
                        if (!artist.isNullOrBlank()) {
                            Text(
                                text = artist,
                                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                currentDestination?.route == Screen.ArtistDetails.route && selectedArtistName != null -> {
                    Text(
                        text = selectedArtistName,
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                currentDestination?.route == Screen.PlaylistDetails.route && selectedPlaylist != null -> {
                    Text(
                        text = selectedPlaylist.name,
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                else -> {
                    Text(
                        text = getAppBarTitle(currentDestination),
                        style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            }
        },
        actions = {
            MonoMusicTopAppBarActions(
                currentDestination = currentDestination,
                isPlaylistsEditMode = isPlaylistsEditMode,
                playlistEditSelectionCount = playlistEditSelectionCount,
                playlistDetailsSelectionCount = playlistDetailsSelectionCount,
                isPlaylistDetailsEditMode = isPlaylistDetailsEditMode,
                isPlaylistDetailsMenuExpanded = isPlaylistDetailsMenuExpanded,
                canDownloadSelectedAlbum = canDownloadSelectedAlbum,
                canRenameSelectedAlbum = canRenameSelectedAlbum,
                hasLibraryPlaylists = selectedPlaylist != null,
                onArtistShuffleClick = onArtistShuffleClick,
                onEnterPlaylistsEditClick = onEnterPlaylistsEditClick,
                onNavigateToSearchClick = onNavigateToSearchClick,
                onPlaylistDetailsMenuToggle = onPlaylistDetailsMenuToggle,
                onPlaylistDetailsEditClick = onPlaylistDetailsEditClick,
                onPlaylistDetailsAddSongsClick = onPlaylistDetailsAddSongsClick,
                onPlaylistDetailsRenameClick = onPlaylistDetailsRenameClick,
                onPlaylistDetailsDeleteClick = onPlaylistDetailsDeleteClick,
                onAlbumDownloadClick = onAlbumDownloadClick,
                onAlbumRenameClick = onAlbumRenameClick,
                onShowDeletePlaylistSongsConfirmationClick = onShowDeletePlaylistSongsConfirmationClick,
                onShowDeletePlaylistsConfirmationClick = onShowDeletePlaylistsConfirmationClick,
                onPlaylistAddSongsDoneClick = onPlaylistAddSongsDoneClick,
            )
        },
        showDivider = false,
        modifier = modifier,
    )
}

@Composable
private fun MonoMusicTopAppBarActions(
    currentDestination: NavDestination?,
    isPlaylistsEditMode: Boolean,
    playlistEditSelectionCount: Int,
    playlistDetailsSelectionCount: Int,
    isPlaylistDetailsEditMode: Boolean,
    isPlaylistDetailsMenuExpanded: Boolean,
    canDownloadSelectedAlbum: Boolean,
    canRenameSelectedAlbum: Boolean,
    hasLibraryPlaylists: Boolean,
    onArtistShuffleClick: (() -> Unit)?,
    onEnterPlaylistsEditClick: () -> Unit,
    onNavigateToSearchClick: () -> Unit,
    onPlaylistDetailsMenuToggle: () -> Unit,
    onPlaylistDetailsEditClick: () -> Unit,
    onPlaylistDetailsAddSongsClick: () -> Unit,
    onPlaylistDetailsRenameClick: () -> Unit,
    onPlaylistDetailsDeleteClick: () -> Unit,
    onAlbumDownloadClick: () -> Unit,
    onAlbumRenameClick: () -> Unit,
    onShowDeletePlaylistSongsConfirmationClick: () -> Unit,
    onShowDeletePlaylistsConfirmationClick: () -> Unit,
    onPlaylistAddSongsDoneClick: () -> Unit,
) {
    val navRoutes = remember { navItems.map { it.route } }

    if (currentDestination?.route != Screen.Search.route && currentDestination?.route in navRoutes) {
        if (currentDestination?.route == Screen.Playlists.route && hasLibraryPlaylists && !isPlaylistsEditMode) {
            IconButton(onClick = onEnterPlaylistsEditClick) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "Edit playlists",
                )
            }
        }

        IconButton(onClick = onNavigateToSearchClick) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Search",
            )
        }
    }

    if (currentDestination?.route == Screen.PlaylistDetails.route && !isPlaylistDetailsEditMode) {
        androidx.compose.foundation.layout.Box {
            IconButton(onClick = onPlaylistDetailsMenuToggle) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = "Playlist options",
                )
            }

            DropdownMenuMMD(
                expanded = isPlaylistDetailsMenuExpanded,
                onDismissRequest = onPlaylistDetailsMenuToggle,
            ) {
                DropdownMenuItemMMD(
                    text = { TextMMD("Edit") },
                    onClick = onPlaylistDetailsEditClick,
                )

                DashedDivider(thickness = 1.dp)

                DropdownMenuItemMMD(
                    text = { TextMMD("Add songs") },
                    onClick = onPlaylistDetailsAddSongsClick,
                )

                DashedDivider(thickness = 1.dp)

                DropdownMenuItemMMD(
                    text = { TextMMD("Rename") },
                    onClick = onPlaylistDetailsRenameClick,
                )

                DashedDivider(thickness = 1.dp)

                DropdownMenuItemMMD(
                    text = { TextMMD("Delete") },
                    onClick = onPlaylistDetailsDeleteClick,
                )
            }
        }
    }

    if (currentDestination?.route == Screen.AlbumDetails.route) {
        if (canDownloadSelectedAlbum) {
            IconButton(onClick = onAlbumDownloadClick) {
                Icon(
                    imageVector = Icons.Outlined.Download,
                    contentDescription = "Download album",
                )
            }
        } else if (canRenameSelectedAlbum) {
            IconButton(onClick = onAlbumRenameClick) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "Rename album",
                )
            }
        }
    }

    if (
        currentDestination?.route == Screen.Playlists.route &&
        isPlaylistsEditMode &&
        playlistEditSelectionCount > 0
    ) {
        OutlinedButtonMMD(
            contentPadding = PaddingValues(8.dp),
            modifier = Modifier.padding(horizontal = 8.dp),
            onClick = onShowDeletePlaylistsConfirmationClick,
        ) {
            TextMMD(
                text = "Delete $playlistEditSelectionCount",
                textAlign = TextAlign.Center,
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }

    if (currentDestination?.route == Screen.PlaylistAddSongs.route) {
        OutlinedButtonMMD(
            contentPadding = PaddingValues(8.dp),
            modifier = Modifier.padding(horizontal = 8.dp),
            onClick = onPlaylistAddSongsDoneClick,
        ) {
            TextMMD(
                text = "Done",
                textAlign = TextAlign.Center,
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }

    if (currentDestination?.route == Screen.ArtistDetails.route && onArtistShuffleClick != null) {
        IconButton(onClick = onArtistShuffleClick) {
            Icon(
                imageVector = Icons.Outlined.Shuffle,
                contentDescription = "Shuffle artist songs",
            )
        }
    }
}

/**
 * What's playing, one line above the tabs (owner's pick 1A): title, artist and play/pause; tap the
 * strip for Now Playing. It changes only when the song or play state does, never with the position.
 */
@Composable
fun PlayingStrip(
    title: String,
    artist: String,
    isPlaying: Boolean,
    onOpen: () -> Unit,
    onPlayPause: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        HorizontalDividerMMD(thickness = 3.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clickable(onClick = onOpen)
                .padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                TextMMD(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (artist.isNotBlank()) {
                    TextMMD(
                        text = artist,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = onPlayPause, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}

@Composable
fun MonoMusicBottomBar(
    currentDestination: NavDestination?,
    onNavigate: (String) -> Unit,
    playingTitle: String?,
    playingArtist: String,
    isPlaying: Boolean,
    onOpenNowPlaying: () -> Unit,
    onPlayPause: () -> Unit,
) {
    val navRoutes = remember { navItems.map { it.route } }
    val route = currentDestination?.route

    Column {
    if (playingTitle != null && route != Screen.Radio.route && route != Screen.PlaylistAddSongs.route) {
        PlayingStrip(
            title = playingTitle,
            artist = playingArtist,
            isPlaying = isPlaying,
            onOpen = onOpenNowPlaying,
            onPlayPause = onPlayPause,
        )
    }

    if (currentDestination?.route in navRoutes) {
        NavigationBarMMD(
            modifier = Modifier.padding(bottom = 2.dp),
        ) {
            navItems.forEach { screen ->
                val isSelected =
                    currentDestination?.hierarchy?.any { it.route == screen.route } == true
                NavigationBarItemMMD(
                    icon = {
                        Icon(
                            painter = rememberVectorPainter(image = screen.icon),
                            contentDescription = screen.label,
                        )
                    },
                    label = {
                        TextMMD(
                            text = screen.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                            maxLines = 1,
                        )
                    },
                    selected = isSelected,
                    onClick = { onNavigate(screen.route) },
                )
            }
        }
    }
    }
}
