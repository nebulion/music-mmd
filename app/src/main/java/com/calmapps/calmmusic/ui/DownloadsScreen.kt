package com.calmapps.calmmusic.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.calmapps.calmmusic.YouTubeDownloadStatus
import com.calmapps.calmmusic.YouTubeDownloadStatus.State
import com.calmapps.calmmusic.ui.kit.ActionRow
import com.calmapps.calmmusic.ui.kit.GroupDivider
import com.calmapps.calmmusic.ui.kit.ListRow
import com.calmapps.calmmusic.ui.kit.PagedList
import com.calmapps.calmmusic.ui.kit.SectionTitle
import com.mudita.mmd.components.text.TextMMD

/** The downloads of one album (or of loose songs), as the Downloads page shows them. */
private data class AlbumDownloads(
    val title: String,
    val artist: String?,
    val items: List<YouTubeDownloadStatus>,
) {
    val active = items.filter { it.state == State.PENDING || it.state == State.IN_PROGRESS }
    val failed = items.filter { it.state == State.FAILED }
    val done = items.count { it.state == State.COMPLETED || it.state == State.SKIPPED }
    val newest = items.maxOf { it.queuedAt }
}

/**
 * Downloads by album (owner's pick 2A, 2026-10-08): "Downloading" first, newest first, ✕ cancels the
 * album's remaining songs; then "Done", newest first, with Clear. Failed songs show on their album
 * with a retry. Counts move per song, never per second.
 */
@Composable
fun DownloadsScreen(
    downloads: List<YouTubeDownloadStatus>,
    onCancelDownload: (String) -> Unit,
    onClearFinished: () -> Unit,
    onRetry: (List<String>) -> Unit,
) {
    val groups = remember(downloads) {
        downloads.filter { it.state != State.CANCELED }
            .groupBy { (it.album ?: "") to (it.albumArtist ?: it.artist) }
            .map { (key, items) -> AlbumDownloads(key.first.ifBlank { "Songs" }, key.second, items) }
            .sortedByDescending { it.newest }
    }
    val downloading = groups.filter { it.active.isNotEmpty() }
    val finished = groups.filter { it.active.isEmpty() }

    if (groups.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            TextMMD(text = "Nothing downloading", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    PagedList(modifier = Modifier.fillMaxSize()) {
        if (downloading.isNotEmpty()) {
            item { SectionTitle("Downloading") }
            downloading.forEachIndexed { i, g ->
                item(key = "a:" + g.title + g.artist) {
                    val inProgress = g.active.firstOrNull { it.state == State.IN_PROGRESS }
                    val status = buildString {
                        append("${g.done} of ${g.items.size}")
                        when {
                            inProgress != null -> append(" · ${inProgress.title}")
                            g.active.any { it.errorMessage == com.calmapps.calmmusic.WAITING_FOR_WIFI } -> append(" · waiting for Wi-Fi")
                            else -> append(" · waiting")
                        }
                    }
                    ListRow(
                        title = g.title,
                        subtitle = listOfNotNull(g.artist, status).joinToString(" · "),
                        showDivider = i != downloading.lastIndex,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        trailing = {
                            IconButton(onClick = { g.active.forEach { onCancelDownload(it.id) } }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Cancel ${g.title}")
                            }
                        },
                    )
                }
            }
        }
        if (finished.isNotEmpty()) {
            if (downloading.isNotEmpty()) item { GroupDivider() }
            item { SectionTitle("Done") }
            finished.forEachIndexed { i, g ->
                item(key = "d:" + g.title + g.artist) {
                    val failed = g.failed.size
                    val summary = when {
                        failed > 0 -> "$failed failed · tap to retry"
                        g.done == 1 -> "1 song"
                        else -> "${g.done} songs"
                    }
                    ListRow(
                        title = g.title,
                        subtitle = listOfNotNull(g.artist, summary).joinToString(" · "),
                        showDivider = i != finished.lastIndex,
                        modifier = Modifier
                            .then(if (failed > 0) Modifier.clickable { onRetry(g.failed.map { it.id }) } else Modifier)
                            .padding(horizontal = 16.dp),
                    )
                }
            }
            item { GroupDivider() }
            item { ActionRow(title = "Clear", subtitle = "Remove finished downloads from this list", onClick = onClearFinished) }
        }
    }
}
