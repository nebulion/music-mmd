package com.calmapps.calmmusic.ui

import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material.icons.outlined.LibraryAddCheck
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import androidx.compose.material.icons.outlined.Downloading
import com.mudita.mmd.components.slider.SliderMMD

enum class RepeatMode {
    OFF,
    QUEUE,
    ONE,
}

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
fun NowPlayingScreen(
    title: String,
    artist: String,
    album: String? = null,
    isPlaying: Boolean,
    isLoading: Boolean,
    currentPosition: Long,
    duration: Long,
    repeatMode: RepeatMode,
    isShuffleOn: Boolean,
    onPlayPauseClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekBackwardClick: () -> Unit,
    onSeekForwardClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onBackClick: () -> Unit = {},
    isVideo: Boolean = false,
    player: Player? = null,
    canDownload: Boolean = false,
    isDownloadInProgress: Boolean = false,
    onDownloadClick: () -> Unit = {},
    onCancelDownloadClick: () -> Unit = {},
    canAddToLibrary: Boolean = false,
    onAddToLibraryClick: () -> Unit = {},
    isInLibrary: Boolean = false,
    sourceType: String? = null,
    streamResolverLabel: String? = null,
    /** Opens the artist's page; null when the artist isn't known (no link shown). */
    onArtistClick: (() -> Unit)? = null,
    onQueueClick: () -> Unit = {},
    /** The playing song's cover; null hides it (Settings → Album covers off, or none). */
    cover: androidx.compose.ui.graphics.ImageBitmap? = null,
    showCover: Boolean = false,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {},
    ) {
        // The same MMD top bar and 3 dp rule as every other page (it had only a small back row)
        com.mudita.mmd.components.top_app_bar.TopAppBarMMD(
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            title = {
                com.mudita.mmd.components.text.TextMMD(
                    text = "Now Playing",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            },
            showDivider = false,
        )
        com.mudita.mmd.components.divider.HorizontalDividerMMD(thickness = 3.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // The cover sits beside the title, artist and album, all resting on the slider (owner,
        // 2026-10-08: the cover stacked on top pushed the text off the screen).
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalAlignment = Alignment.Bottom,
        ) {
        if (showCover && !isVideo) {
            com.calmapps.calmmusic.ui.kit.CoverBox(cover, 112.dp)
            Spacer(modifier = Modifier.width(16.dp))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.Bottom,
        ) {
            Text(
                text = title,
                style = if (isVideo) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                maxLines = if (isVideo) 1 else 3,
                overflow = TextOverflow.Ellipsis
            )

            if (!isVideo) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = artist,
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    // underlined when it opens the artist's page (no colour on E Ink)
                    textDecoration = if (onArtistClick != null) androidx.compose.ui.text.style.TextDecoration.Underline else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = if (onArtistClick != null) Modifier.clickable(onClick = onArtistClick) else Modifier,
                )

                val hasAlbum = !album.isNullOrBlank()
                if (hasAlbum) {
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = album!!,
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (isVideo && player != null) {
                Spacer(modifier = Modifier.height(16.dp))

                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                    factory = { context ->
                        PlayerView(context).apply {
                            useController = false
                            this.player = player
                        }
                    },
                    update = { view ->
                        view.player = player
                    },
                )
            }
        }

        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            SliderMMD(
                modifier = Modifier.fillMaxWidth(),
                value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                onValueChange = { value ->
                    if (duration > 0) {
                        val newPosition = (value * duration).toLong().coerceIn(0L, duration)
                        onSeek(newPosition)
                    }
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDurationMillisNonNull(currentPosition),
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = formatDurationMillisNonNull(duration),
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onSeekBackwardClick,
                modifier = Modifier.size(72.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.SkipPrevious,
                    modifier = Modifier.size(46.dp),
                    contentDescription = "Previous Song",
                )
            }

            // No spinner (E Ink): the button stays put; "Loading" shows under the controls instead.
            IconButton(
                onClick = onPlayPauseClick,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(46.dp),
                )
            }

            IconButton(
                onClick = onSeekForwardClick,
                modifier = Modifier.size(72.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.SkipNext,
                    modifier = Modifier.size(46.dp),
                    contentDescription = "Next Song",
                )
            }
        }

        DelayedLoadingLine(visible = isLoading)

        // Bottom row for secondary actions (e.g. shuffle, repeat, add to playlist / library)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (canAddToLibrary) {
                IconButton(onClick = onAddToLibraryClick) {
                    Icon(
                        imageVector = Icons.Outlined.LibraryAdd,
                        contentDescription = "Add to library",
                    )
                }
            }

            if (canDownload) {
                if (isDownloadInProgress) {
                    IconButton(onClick = onCancelDownloadClick) {
                        Icon(
                            imageVector = Icons.Outlined.Downloading,
                            contentDescription = "Downloading. Tap to cancel",
                        )
                    }
                } else {
                    IconButton(onClick = onDownloadClick) {
                        Icon(
                            imageVector = Icons.Outlined.Download,
                            contentDescription = "Download",
                        )
                    }
                }
            }

            IconButton(onClick = onQueueClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.QueueMusic,
                    contentDescription = "Queue",
                )
            }

            IconButton(onClick = onAddToPlaylistClick) {
                Icon(
                    imageVector = Icons.Outlined.PlaylistAdd,
                    contentDescription = "Add to playlist",
                )
            }

            IconButton(onClick = onRepeatClick) {
                val (icon, description, isActive) = when (repeatMode) {
                    RepeatMode.OFF -> Triple(Icons.Outlined.Repeat, "Repeat off", false)
                    RepeatMode.QUEUE -> Triple(Icons.Outlined.Repeat, "Repeat queue", true)
                    RepeatMode.ONE -> Triple(Icons.Outlined.RepeatOne, "Repeat current song", true)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = icon,
                        contentDescription = description,
                    )
                    if (isActive) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape,
                                ),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(onClick = onShuffleClick) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Shuffle,
                        contentDescription = "Shuffle queue",
                    )
                    if (isShuffleOn) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape,
                                ),
                        )
                    }
                }
            }

            val isLocal = sourceType == "LOCAL_FILE" || sourceType == "YOUTUBE_DOWNLOAD"
            if (!isLocal) {
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.onSurface,
                            shape = CircleShape
                        )
                        .padding(12.dp, 4.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Cloud,
                            contentDescription = "Streaming source",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.surface
                        )

//                        if (!streamResolverLabel.isNullOrBlank()) {
//                            Spacer(modifier = Modifier.width(6.dp))
//                            Text(
//                                text = streamResolverLabel,
//                                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
//                                color = MaterialTheme.colorScheme.surface,
//                                maxLines = 1,
//                                overflow = TextOverflow.Ellipsis,
//                            )
//                        }

                        if (isInLibrary) {
                            Spacer(modifier = Modifier.width(12.dp))

                            Icon(
                                imageVector = Icons.Outlined.LibraryAddCheck,
                                contentDescription = "In Library",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.surface
                            )
                        }
                    }
                }
            }
        }
    }
    }
}

private fun formatDurationMillisNonNull(millis: Long): String {
    return com.calmapps.calmmusic.formatDurationMillis(millis) ?: "0:00"
}

@Preview(showBackground = true)
@Composable
private fun NowPlayingScreenPreview() {
    NowPlayingScreen(
        title = "Song Title",
        artist = "Artist Name",
        album = "Album Name",
        isPlaying = false,
        isLoading = false,
        currentPosition = 0L,
        duration = 1000L,
        repeatMode = RepeatMode.OFF,
        isShuffleOn = false,
        onPlayPauseClick = {},
        onSeek = {},
        onSeekBackwardClick = {},
        onSeekForwardClick = {},
        onShuffleClick = {},
        onRepeatClick = {},
        onAddToPlaylistClick = {},
        isVideo = false,
        player = null,
        canDownload = false,
        isDownloadInProgress = false,
        onDownloadClick = {},
        onCancelDownloadClick = {},
        canAddToLibrary = false,
        onAddToLibraryClick = {},
        isInLibrary = true,
        sourceType = "YOUTUBE",
        streamResolverLabel = "Innertube/Piped",
    )
}

/**
 * "Loading" under the controls while a song buffers: nothing for the first half second (most
 * starts are quicker), then static text. The line's height is always reserved, so nothing moves.
 */
@Composable
private fun DelayedLoadingLine(visible: Boolean) {
    var show by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(visible) {
        show = false
        if (visible) {
            kotlinx.coroutines.delay(500)
            show = true
        }
    }
    Box(Modifier.fillMaxWidth().height(32.dp), contentAlignment = Alignment.Center) {
        if (show) {
            com.mudita.mmd.components.text.TextMMD(
                text = "Loading",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
