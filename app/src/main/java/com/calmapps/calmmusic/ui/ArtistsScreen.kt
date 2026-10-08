package com.calmapps.calmmusic.ui

import com.calmapps.calmmusic.ui.kit.ListRow
import androidx.compose.foundation.clickable
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

/** Simple UI model for distinct artists in the library. */
data class ArtistUiModel(
    val id: String,
    val name: String,
    val songCount: Int,
    val albumCount: Int,
    val addedAt: Long? = null,
)

@Composable
fun ArtistsScreen(
    artists: List<ArtistUiModel>,
    isLoading: Boolean,
    errorMessage: String?,
    isSyncInProgress: Boolean,
    hasAnySongs: Boolean,
    onOpenStreamingSettingsClick: () -> Unit,
    onOpenLocalSettingsClick: () -> Unit,
    onArtistClick: (ArtistUiModel) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
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
                        TextMMD(text = "Error loading artists")
                        TextMMD(text = errorMessage)
                    }
                }
            }

            artists.isEmpty() && isSyncInProgress -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    TextMMD(text = "Music sync is in progress…")
                }
            }

            artists.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!hasAnySongs) {
                        LibraryOnboardingEmptyState(
                            title = "No artists yet",
                            body = "Download songs from YouTube Music or choose local folders in Settings to start building your library.",
                            onOpenStreamingSettingsClick = onOpenStreamingSettingsClick,
                            onOpenLocalSettingsClick = onOpenLocalSettingsClick,
                        )
                    } else {
                        TextMMD(text = "No artists to show yet. Once your songs have artist info, they'll appear here.")
                    }
                }
            }

            else -> {
                val lastArtistId = artists.lastOrNull()?.id
                PagedList(contentPadding = PaddingValues(horizontal = 16.dp)) {
                    items(
                        items = artists,
                        key = { it.id },
                    ) { artist ->
                        val isLast = artist.id == lastArtistId
                        ArtistItem(
                            artist = artist,
                            onClick = { onArtistClick(artist) },
                            showDivider = !isLast,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ArtistItem(
    artist: ArtistUiModel,
    onClick: () -> Unit,
    showDivider: Boolean = true,
) {
    val songLabel = if (artist.songCount == 1) "1 song" else "${artist.songCount} songs"
    val albumLabel = if (artist.albumCount == 1) "1 album" else "${artist.albumCount} albums"
    ListRow(
        title = artist.name,
        subtitle = "$songLabel • $albumLabel",
        showDivider = showDivider,
        modifier = Modifier.clickable(onClick = onClick),
    )
}