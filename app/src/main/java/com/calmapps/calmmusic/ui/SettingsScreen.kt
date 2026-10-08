package com.calmapps.calmmusic.ui

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.calmapps.calmmusic.ui.kit.PagedList
import com.mudita.mmd.components.slider.SliderMMD
import com.mudita.mmd.components.switcher.SwitchMMD
import com.mudita.mmd.components.tabs.PrimaryTabRowMMD
import com.mudita.mmd.components.tabs.TabMMD
import com.mudita.mmd.components.text.TextMMD

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    selectedTab: Int,
    onSelectedTabChange: (Int) -> Unit,
    completeAlbumsWithYouTube: Boolean,
    onCompleteAlbumsWithYouTubeChange: (Boolean) -> Unit,
    includeLocalMusic: Boolean,
    localFolders: List<String>,
    onIncludeLocalMusicChange: (Boolean) -> Unit,
    onAddFolderClick: () -> Unit,
    onRemoveFolderClick: (String) -> Unit,
    onRescanLocalMusicClick: () -> Unit,
    isRescanningLocal: Boolean,
    localScanProgress: Float,
    isIngestingLocal: Boolean,
    localIngestProgress: Float,
    localScanTotalDiscovered: Int?,
    localScanSkippedUnchanged: Int?,
    localScanIndexedNewOrUpdated: Int?,
    localScanDeletedMissing: Int?,
) {
    // 0 = Streaming, 1 = Local, 2 = E Ink
    val tabOptions = listOf("Streaming", "Local", "E Ink")

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        PrimaryTabRowMMD(selectedTabIndex = selectedTab) {
            tabOptions.forEachIndexed { index, title ->
                TabMMD(
                    selected = selectedTab == index,
                    onClick = { onSelectedTabChange(index) },
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

        if (selectedTab == 2) {
            com.calmapps.calmmusic.ui.kit.EinkSettings()
        } else if (selectedTab == 0) {
            PagedList(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.Top,
            ) {
                item {

                    TextMMD(
                        text = "Library features",
                        style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )

                    HorizontalDividerMMD(
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 16.dp)
                    ) {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCompleteAlbumsWithYouTubeChange(!completeAlbumsWithYouTube) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 16.dp)
                            ) {
                                TextMMD(
                                    text = "Complete albums with YouTube",
                                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                TextMMD(
                                    text = "When viewing a local album, search YouTube for missing songs and display them in the list.",
                                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            SwitchMMD(
                                checked = completeAlbumsWithYouTube,
                                onCheckedChange = onCompleteAlbumsWithYouTubeChange,
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        } else if (selectedTab == 1) {
            PagedList(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Top,
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextMMD(
                            text = "Include local music",
                            style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f),
                        )
                        SwitchMMD(
                            checked = includeLocalMusic,
                            onCheckedChange = onIncludeLocalMusicChange,
                        )
                    }
                }

                if (includeLocalMusic) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                        ) {
                            TextMMD(
                                text = "Local music folders",
                                style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            TextMMD(
                                text = "Choose one or more folders to scan for audio files.",
                                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                            )

                            HorizontalDividerMMD(
                                thickness = 1.dp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButtonMMD(
                                onClick = onAddFolderClick,
                                modifier = Modifier.weight(1f),
                            ) {
                                TextMMD(
                                    text = "Add folder",
                                    style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                                )
                            }

                            if (localFolders.isNotEmpty()) {
                                OutlinedButtonMMD(
                                    onClick = onRescanLocalMusicClick,
                                    modifier = Modifier.weight(1f),
                                    enabled = localFolders.isNotEmpty() && !isRescanningLocal,
                                ) {
                                    TextMMD(
                                        text = "Rescan",
                                        style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                                    )
                                }
                            }
                        }
                    }

                    if (isRescanningLocal && !isIngestingLocal) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 4.dp, end = 4.dp, top = 4.dp),
                                ) {
                                    SliderMMD(
                                        modifier = Modifier.fillMaxWidth(),
                                        value = localScanProgress.coerceIn(0f, 1f),
                                        onValueChange = { },
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                ) {
                                    val percent =
                                        (localScanProgress * 100f).toInt().coerceIn(0, 100)
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        TextMMD(
                                            text = "Step 1 of 2 – Scanning folders for audio files… $percent%",
                                            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                        )
                                        if (localScanTotalDiscovered != null && localScanSkippedUnchanged != null && localScanIndexedNewOrUpdated != null) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            TextMMD(
                                                text = "Found $localScanTotalDiscovered files · Skipped $localScanSkippedUnchanged unchanged · Indexed $localScanIndexedNewOrUpdated new/updated",
                                                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (isIngestingLocal) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 4.dp, end = 4.dp, top = 4.dp),
                                ) {
                                    SliderMMD(
                                        modifier = Modifier.fillMaxWidth(),
                                        value = localIngestProgress.coerceIn(0f, 1f),
                                        onValueChange = { },
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                ) {
                                    val ingestPercent =
                                        (localIngestProgress * 100f).toInt().coerceIn(0, 100)
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        TextMMD(
                                            text = "Step 2 of 2 – Adding music to library… $ingestPercent%",
                                            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                        )
                                        if (localScanDeletedMissing != null && localScanDeletedMissing > 0) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            TextMMD(
                                                text = "Removed ${localScanDeletedMissing} files that are no longer present",
                                                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Show the last scan summary after work is complete.
                    if (!isRescanningLocal && !isIngestingLocal &&
                        localScanTotalDiscovered != null &&
                        localScanSkippedUnchanged != null &&
                        localScanIndexedNewOrUpdated != null
                    ) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                            ) {
                                TextMMD(
                                    text = "Last scan",
                                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                TextMMD(
                                    text = "Found $localScanTotalDiscovered files · Skipped $localScanSkippedUnchanged unchanged · Indexed $localScanIndexedNewOrUpdated new/updated",
                                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                )

                                if (localScanDeletedMissing != null && localScanDeletedMissing > 0) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    TextMMD(
                                        text = "Removed ${localScanDeletedMissing} files that are no longer present",
                                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }
                        }
                    }

                    if (localFolders.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
                            ) {
                                TextMMD(
                                    text = "No folders selected yet.",
                                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    } else {
                        items(localFolders) { folder ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                TextMMD(
                                    text = formatDirectoryPath(folder),
                                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(onClick = { onRemoveFolderClick(folder) }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = "Remove folder",
                                    )
                                }
                            }
                        }
                    }
                }
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
            val path = afterMarker.replace("/", " > ")
            return if (path.isEmpty()) "Primary" else "Primary > $path"
        }
    } catch (e: Exception) {
        // Fallback
    }
    return uriString
}