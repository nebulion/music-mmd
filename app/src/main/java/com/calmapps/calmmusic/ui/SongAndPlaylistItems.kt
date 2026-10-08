package com.calmapps.calmmusic.ui

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.LibraryAddCheck
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.calmapps.calmmusic.ui.kit.DashedDividerMMD
import com.calmapps.calmmusic.ui.kit.ListRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mudita.mmd.components.checkbox.CheckboxMMD
import com.mudita.mmd.components.menus.DropdownMenuItemMMD
import com.mudita.mmd.components.menus.DropdownMenuMMD
import com.mudita.mmd.components.text.TextMMD

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongItem(
    song: SongUiModel,
    showTrackNumber: Boolean = false,
    isCurrentlyPlaying: Boolean,
    onClick: () -> Unit,
    onAddToPlaylist: () -> Unit = {},
    onDelete: () -> Unit = {},
    onRemoveFromLibrary: () -> Unit = {},
    onEdit: (() -> Unit)? = null,
    isDownloaded: Boolean = false,
    showDivider: Boolean = true,
    isInLibrary: Boolean = false,
    /** Show the ✓ for library songs even when they are local files (search results). */
    markInLibrary: Boolean = false,
) {
    val (isLocal, subtitle) = remember(
        song.id,
        song.audioUri,
        song.artist,
        song.album,
        song.durationText,
        song.sourceType,
    ) {
        val local = song.sourceType == "LOCAL_FILE" || song.sourceType == "YOUTUBE_DOWNLOAD"
        val fileExtension = if (local) {
            val uriString = song.audioUri ?: song.id
            try {
                val lastSegment = Uri.parse(uriString).lastPathSegment ?: ""
                lastSegment.substringAfterLast('.', "").lowercase()
            } catch (_: Exception) {
                ""
            }
        } else {
            ""
        }

        val mp4 = local && fileExtension == "mp4"
        val baseArtist = song.artist.ifBlank { if (local) "Local file" else "" }
        val album = song.album?.takeIf { it.isNotBlank() }
        val durationText = song.durationText?.takeIf { it.isNotBlank() }
        val prefix = if (mp4) "MP4 • " else ""

        val coreSubtitle = buildString {
            if (baseArtist.isNotBlank()) {
                append(baseArtist)
            }
            if (!album.isNullOrBlank()) {
                if (isNotEmpty()) append(" • ")
                append(album)
            }
            if (!durationText.isNullOrBlank()) {
                if (isNotEmpty()) append(" • ")
                append(durationText)
            }
        }

        val sub = when {
            coreSubtitle.isNotBlank() && prefix.isNotBlank() -> prefix + coreSubtitle
            coreSubtitle.isNotBlank() -> coreSubtitle
            prefix.isNotBlank() -> prefix.trimEnd(' ', '•')
            else -> ""
        }

        Pair(local, sub)
    }

    var showMenu by remember { mutableStateOf(false) }

    Box {
        ListRow(
            title = song.title,
            subtitle = subtitle.ifBlank { null },
            showDivider = showDivider,
            modifier = Modifier.combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true },
            ),
            // album pages reserve the number column on every row, so labels line up
            leading = if (showTrackNumber) {
                {
                    if (isCurrentlyPlaying) {
                        Icon(Icons.Outlined.Headphones, contentDescription = "Now playing", modifier = Modifier.size(24.dp))
                    } else {
                        TextMMD(
                            text = song.trackNumber?.toString().orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            } else {
                null
            },
            subtitleLeading = {
                if (!isLocal) {
                    Icon(Icons.Outlined.Cloud, contentDescription = "Streaming", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                }
                if (isInLibrary && (!isLocal || markInLibrary)) {
                    Icon(Icons.Outlined.LibraryAddCheck, contentDescription = "In library", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                }
            },
            trailing = if (isCurrentlyPlaying && !showTrackNumber) {
                { Icon(Icons.Outlined.Headphones, contentDescription = "Now playing", modifier = Modifier.size(24.dp)) }
            } else {
                null
            },
        )

        DropdownMenuMMD(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            DropdownMenuItemMMD(
                text = { TextMMD(text = "Add to playlist") },
                onClick = {
                    showMenu = false
                    onAddToPlaylist()
                },
            )
            if (isLocal && onEdit != null) {
                DashedDividerMMD()
                DropdownMenuItemMMD(
                    text = { TextMMD(text = "Edit") },
                    onClick = {
                        showMenu = false
                        onEdit()
                    },
                )
            }
            if (isInLibrary && !isLocal && !isDownloaded) {
                DashedDividerMMD()
                DropdownMenuItemMMD(
                    text = { TextMMD(text = "Remove from library") },
                    onClick = {
                        showMenu = false
                        onRemoveFromLibrary()
                    },
                )
            }
            // destructive last (MMD Menus)
            if (isDownloaded || isLocal) {
                DashedDividerMMD()
                DropdownMenuItemMMD(
                    text = { TextMMD(text = "Delete") },
                    onClick = {
                        showMenu = false
                        onDelete()
                    },
                )
            }
        }
    }
}

@Composable
fun PlaylistItem(
    playlist: PlaylistUiModel,
    onClick: () -> Unit,
    showDivider: Boolean = true,
) {
    val subtitle = remember(playlist.id, playlist.description, playlist.songCount) {
        val songCountText = playlist.songCount?.let { count ->
            if (count == 1) "1 song" else "$count songs"
        }
        buildString {
            val description = playlist.description?.takeIf { it.isNotBlank() }
            if (!description.isNullOrEmpty()) {
                append(description)
            }
            if (!songCountText.isNullOrEmpty()) {
                if (isNotEmpty()) {
                    append(" • ")
                }
                append(songCountText)
            }
        }
    }

    ListRow(
        title = playlist.name,
        subtitle = subtitle.ifEmpty { null },
        bold = true,
        showDivider = showDivider,
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
fun SelectablePlaylistItem(
    playlist: PlaylistUiModel,
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    showDivider: Boolean,
) {
    val subtitle = remember(playlist.id, playlist.description, playlist.songCount) {
        val songCountText = playlist.songCount?.let { count ->
            if (count == 1) "1 song" else "$count songs"
        }
        buildString {
            val description = playlist.description?.takeIf { it.isNotBlank() }
            if (!description.isNullOrEmpty()) {
                append(description)
            }
            if (!songCountText.isNullOrEmpty()) {
                if (isNotEmpty()) {
                    append(" • ")
                }
                append(songCountText)
            }
        }
    }

    val toggle: () -> Unit = {
        onSelectionChange(!isSelected)
    }

    ListRow(
        title = playlist.name,
        subtitle = subtitle.ifEmpty { null },
        bold = true,
        showDivider = showDivider,
        modifier = Modifier.clickable(onClick = toggle),
        leading = { CheckboxMMD(checked = isSelected, onCheckedChange = null) },
    )
}
