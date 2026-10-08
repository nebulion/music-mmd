package com.calmapps.calmmusic.ui

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import com.calmapps.calmmusic.ui.kit.ActionRow
import com.calmapps.calmmusic.ui.kit.ChoiceSheet
import com.calmapps.calmmusic.ui.kit.EinkRefresh
import com.calmapps.calmmusic.ui.kit.GroupDivider
import com.calmapps.calmmusic.ui.kit.ListRow
import com.calmapps.calmmusic.ui.kit.NavRow
import com.calmapps.calmmusic.ui.kit.PagedList
import com.calmapps.calmmusic.ui.kit.RowDivider
import com.calmapps.calmmusic.ui.kit.SwitchRow
import com.calmapps.calmmusic.ui.kit.ValueRow

/**
 * Settings as the Kompakt's own Settings app lays them out (UI-PATTERNS A4): one page of rows, each
 * with its current value under a bold title; a switch sits on its row, a value opens its editor (a
 * sheet of radio rows), a list opens its own page. Groups are separated by a rule, no headings.
 */
@Composable
fun SettingsScreen(
    completeAlbumsWithYouTube: Boolean,
    onCompleteAlbumsWithYouTubeChange: (Boolean) -> Unit,
    includeLocalMusic: Boolean,
    localFolders: List<String>,
    onIncludeLocalMusicChange: (Boolean) -> Unit,
    onOpenFoldersClick: () -> Unit,
    onRescanLocalMusicClick: () -> Unit,
    isRescanningLocal: Boolean,
    localScanProgress: Float,
    isIngestingLocal: Boolean,
    localIngestProgress: Float,
    localScanTotalDiscovered: Int?,
    localScanIndexedNewOrUpdated: Int?,
    localScanDeletedMissing: Int?,
) {
    var flashEnabled by remember { mutableStateOf(EinkRefresh.enabled) }
    var flashActions by remember { mutableIntStateOf(EinkRefresh.actions) }
    var choosingInterval by remember { mutableStateOf(false) }

    // what the last or running scan did, in one line (updated per step, not per file)
    val scanStatus = when {
        isIngestingLocal -> "Adding to library · ${(localIngestProgress * 100).toInt().coerceIn(0, 100) / 10 * 10}%"
        isRescanningLocal -> "Scanning folders · ${(localScanProgress * 100).toInt().coerceIn(0, 100) / 10 * 10}%"
        localScanTotalDiscovered != null -> buildString {
            append("$localScanTotalDiscovered files")
            if ((localScanIndexedNewOrUpdated ?: 0) > 0) append(" · $localScanIndexedNewOrUpdated new")
            if ((localScanDeletedMissing ?: 0) > 0) append(" · $localScanDeletedMissing removed")
        }
        else -> "Look for new music in your folders"
    }

    PagedList(modifier = Modifier.fillMaxSize()) {
        run {
            item {
                NavRow(
                    title = "Music folders",
                    subtitle = when (localFolders.size) {
                        0 -> "None yet"
                        1 -> formatDirectoryPath(localFolders.first())
                        else -> "${localFolders.size} folders"
                    },
                    onClick = onOpenFoldersClick,
                )
            }
            if (localFolders.isNotEmpty()) {
                item { RowDivider() }
                item {
                    ActionRow(
                        title = "Rescan",
                        subtitle = scanStatus,
                        onClick = { if (!isRescanningLocal && !isIngestingLocal) onRescanLocalMusicClick() },
                    )
                }
            }
        }
        item { GroupDivider() }
        item {
            SwitchRow(
                title = "Flash to clear ghosting",
                subtitle = "Turns the screen black for a moment",
                checked = flashEnabled,
                onCheckedChange = {
                    flashEnabled = it
                    EinkRefresh.enabled = it
                },
            )
        }
        if (flashEnabled) {
            item { RowDivider() }
            item {
                ValueRow(
                    title = "Flash every",
                    value = "$flashActions taps",
                    onClick = { choosingInterval = true },
                )
            }
        }
    }

    if (choosingInterval) {
        ChoiceSheet(
            title = "Flash every",
            options = EinkRefresh.ACTION_CHOICES,
            selected = flashActions,
            label = { "$it taps" },
            onSelect = {
                flashActions = it
                EinkRefresh.actions = it
            },
            onDismissRequest = { choosingInterval = false },
        )
    }
}

/** Settings → Music folders: one row per folder (delete at the right), "Add folder" first. */
@Composable
fun MusicFoldersScreen(
    folders: List<String>,
    onAddFolderClick: () -> Unit,
    onRemoveFolderClick: (String) -> Unit,
) {
    PagedList(modifier = Modifier.fillMaxSize()) {
        item { ActionRow(title = "Add folder", onClick = onAddFolderClick) }
        item { GroupDivider() }
        folders.forEachIndexed { i, folder ->
            item(key = folder) {
                ListRow(
                    title = formatDirectoryPath(folder),
                    showDivider = i != folders.lastIndex,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    trailing = {
                        IconButton(onClick = { onRemoveFolderClick(folder) }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Remove folder")
                        }
                    },
                )
            }
        }
    }
}

private fun formatDirectoryPath(uriString: String): String {
    try {
        val decoded = Uri.decode(uriString)
        val marker = "primary:"
        val index = decoded.indexOf(marker)
        if (index != -1) {
            val afterMarker = decoded.substring(index + marker.length)
            val path = afterMarker.replace("/", " › ")
            return if (path.isEmpty()) "Phone" else path
        }
    } catch (_: Exception) {
    }
    return uriString
}
