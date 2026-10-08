package com.calmapps.calmmusic

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.outlined.DownloadDone
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
    currentRoute: String?,
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
    albumSongsLeft: Int,
    albumComplete: Boolean,
    canRenameSelectedAlbum: Boolean,
    onArtistShuffleClick: (() -> Unit)?,
    onOpenPage: (String) -> Unit,
    onSortClick: (() -> Unit)?,
    activeDownloads: Int,
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
                currentRoute == Screen.PlaylistDetails.route && isPlaylistDetailsEditMode -> {
                    IconButton(onClick = onCancelPlaylistDetailsEditClick) {
                        Icon(
                            imageVector = Icons.Outlined.Clear,
                            contentDescription = "Cancel playlist edits",
                        )
                    }
                }

                currentRoute == Screen.Playlists.route && isPlaylistsEditMode -> {
                    IconButton(onClick = onCancelPlaylistsEditClick) {
                        Icon(
                            imageVector = Icons.Outlined.Clear,
                            contentDescription = "Cancel playlist edit",
                        )
                    }
                }

                canNavigateBack && currentRoute !in navRoutes -> {
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
                currentRoute == Screen.Search.route -> {
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

                currentRoute == Screen.AlbumDetails.route && selectedAlbum != null -> {
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

                currentRoute == Screen.ArtistDetails.route && selectedArtistName != null -> {
                    Text(
                        text = selectedArtistName,
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                currentRoute == Screen.PlaylistDetails.route && selectedPlaylist != null -> {
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
                        text = getAppBarTitle(currentRoute),
                        style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            }
        },
        actions = {
            MonoMusicTopAppBarActions(
                currentRoute = currentRoute,
                isPlaylistsEditMode = isPlaylistsEditMode,
                playlistEditSelectionCount = playlistEditSelectionCount,
                playlistDetailsSelectionCount = playlistDetailsSelectionCount,
                isPlaylistDetailsEditMode = isPlaylistDetailsEditMode,
                isPlaylistDetailsMenuExpanded = isPlaylistDetailsMenuExpanded,
                canDownloadSelectedAlbum = canDownloadSelectedAlbum,
                albumSongsLeft = albumSongsLeft,
                albumComplete = albumComplete,
                canRenameSelectedAlbum = canRenameSelectedAlbum,
                hasLibraryPlaylists = selectedPlaylist != null,
                onArtistShuffleClick = onArtistShuffleClick,
                onOpenPage = onOpenPage,
                onSortClick = onSortClick,
                activeDownloads = activeDownloads,
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
    currentRoute: String?,
    isPlaylistsEditMode: Boolean,
    playlistEditSelectionCount: Int,
    playlistDetailsSelectionCount: Int,
    isPlaylistDetailsEditMode: Boolean,
    isPlaylistDetailsMenuExpanded: Boolean,
    canDownloadSelectedAlbum: Boolean,
    albumSongsLeft: Int,
    albumComplete: Boolean,
    canRenameSelectedAlbum: Boolean,
    hasLibraryPlaylists: Boolean,
    onArtistShuffleClick: (() -> Unit)?,
    onOpenPage: (String) -> Unit,
    onSortClick: (() -> Unit)?,
    activeDownloads: Int,
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

    if (currentRoute != Screen.Search.route && currentRoute in navRoutes) {
        if (currentRoute == Screen.Playlists.route && hasLibraryPlaylists && !isPlaylistsEditMode) {
            IconButton(onClick = onEnterPlaylistsEditClick) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "Edit playlists",
                )
            }
        }

        if (onSortClick != null) {
            IconButton(onClick = onSortClick) {
                Icon(imageVector = Icons.AutoMirrored.Outlined.Sort, contentDescription = "Sort")
            }
        }
        IconButton(onClick = onNavigateToSearchClick) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Search",
            )
        }

        // Downloads and Settings straight in the bar (owner, 2026-10-08: "move the items out of
        // the three dots"). Four actions on the tab pages: one over MMD's three, by choice.
        IconButton(onClick = { onOpenPage(Screen.Downloads.route) }) {
            if (activeDownloads > 0) {
                com.mudita.mmd.components.badge.BadgedBoxMMD(
                    badge = { com.mudita.mmd.components.badge.BadgeMMD { TextMMD(if (activeDownloads > 99) "99+" else "$activeDownloads") } },
                ) {
                    Icon(imageVector = Icons.Outlined.Download, contentDescription = "Downloads, $activeDownloads left")
                }
            } else {
                Icon(imageVector = Icons.Outlined.Download, contentDescription = "Downloads")
            }
        }
        IconButton(onClick = { onOpenPage(Screen.Settings.route) }) {
            Icon(imageVector = Icons.Outlined.Settings, contentDescription = "Settings")
        }
    }

    if (currentRoute == Screen.PlaylistDetails.route && !isPlaylistDetailsEditMode) {
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

    if (currentRoute == Screen.AlbumDetails.route) {
        // Owner's pick 1A: the button is the feedback, no message. Download → "7 left" (tap: the
        // Downloads page) → ✓ when every song is on the phone.
        if (canDownloadSelectedAlbum) {
            when {
                albumSongsLeft > 0 -> {
                    com.mudita.mmd.components.buttons.OutlinedButtonMMD(
                        onClick = { onOpenPage(Screen.Downloads.route) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.padding(horizontal = 4.dp),
                    ) {
                        TextMMD(text = "$albumSongsLeft left", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
                albumComplete -> {
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Outlined.DownloadDone, contentDescription = "Downloaded")
                    }
                }
                else -> {
                    IconButton(onClick = onAlbumDownloadClick) {
                        Icon(
                            imageVector = Icons.Outlined.Download,
                            contentDescription = "Download album",
                        )
                    }
                }
            }
        }
        if (canRenameSelectedAlbum) {
            IconButton(onClick = onAlbumRenameClick) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "Rename album",
                )
            }
        }
    }

    if (
        currentRoute == Screen.Playlists.route &&
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

    if (currentRoute == Screen.PlaylistAddSongs.route) {
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

    if (currentRoute == Screen.ArtistDetails.route && onSortClick != null) {
        IconButton(onClick = onSortClick) {
            Icon(imageVector = Icons.AutoMirrored.Outlined.Sort, contentDescription = "Sort albums")
        }
    }
    if (currentRoute == Screen.ArtistDetails.route && onArtistShuffleClick != null) {
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
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    playingTitle: String?,
    playingArtist: String,
    isPlaying: Boolean,
    onOpenNowPlaying: () -> Unit,
    onPlayPause: () -> Unit,
) {
    val navRoutes = remember { navItems.map { it.route } }
    val route = currentRoute

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

    if (currentRoute in navRoutes) {
        NavigationBarMMD(
            modifier = Modifier.padding(bottom = 2.dp),
        ) {
            navItems.forEach { screen ->
                val isSelected =
                    currentRoute == screen.route
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
