package com.calmapps.calmmusic.ui

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.outlined.Cloud
import com.calmapps.calmmusic.ui.kit.ListRow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.calmapps.calmmusic.ui.kit.PagedList
import com.mudita.mmd.components.text.TextMMD

/** UI model for displaying albums in the library. */
data class AlbumUiModel(
    val id: String,
    val title: String,
    val artist: String?,
    val sourceType: String,
    /** Optional release year for display when available. */
    val releaseYear: Int? = null,
    val addedAt: Long? = null,
)

@Composable
fun AlbumsScreen(
    albums: List<AlbumUiModel>,
    isLoading: Boolean,
    errorMessage: String?,
    isSyncInProgress: Boolean,
    hasAnySongs: Boolean,
    onOpenStreamingSettingsClick: () -> Unit,
    onOpenLocalSettingsClick: () -> Unit,
    onAlbumClick: (AlbumUiModel) -> Unit = {},
) {
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        TextMMD(text = "Error loading albums")
                        TextMMD(text = errorMessage)
                    }
                }
            }

            albums.isEmpty() && isSyncInProgress -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    TextMMD(text = "Music sync is in progress…")
                }
            }

            albums.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!hasAnySongs) {
                        LibraryOnboardingEmptyState(
                            title = "No albums yet",
                            body = "Download songs from YouTube Music or choose local folders in Settings to start building your library.",
                            onOpenStreamingSettingsClick = onOpenStreamingSettingsClick,
                            onOpenLocalSettingsClick = onOpenLocalSettingsClick,
                        )
                    } else {
                        TextMMD(text = "No albums to show yet. Once your songs have album info, they'll appear here.")
                    }
                }
            }

            else -> {
                val lastAlbumId = albums.lastOrNull()?.id
                PagedList(contentPadding = PaddingValues(horizontal = 16.dp)) {
                    items(
                        items = albums,
                        key = { it.id },
                    ) { album ->
                        val isLast = album.id == lastAlbumId
                        AlbumItem(
                            album = album,
                            onClick = { onAlbumClick(album) },
                            showDivider = !isLast,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AlbumItem(
    album: AlbumUiModel,
    onClick: () -> Unit,
    showDivider: Boolean = true,
) {
    val subtitle = listOfNotNull(album.artist?.takeIf { it.isNotBlank() }, album.releaseYear?.toString())
        .joinToString(" • ")
    ListRow(
        title = album.title,
        subtitle = subtitle.ifEmpty { album.sourceType.takeIf { it == "YOUTUBE" }?.let { "YouTube Music" } },
        bold = true,
        showDivider = showDivider,
        modifier = Modifier.clickable(onClick = onClick),
        // as on song rows: a cloud marks what isn't on the phone
        subtitleLeading = if (album.sourceType == "YOUTUBE") {
            {
                androidx.compose.material3.Icon(
                    androidx.compose.material.icons.Icons.Outlined.Cloud,
                    contentDescription = "Not downloaded",
                    modifier = Modifier.size(16.dp),
                )
                androidx.compose.foundation.layout.Spacer(Modifier.width(6.dp))
            }
        } else {
            null
        },
    )
}
