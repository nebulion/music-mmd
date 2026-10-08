// SPDX-License-Identifier: GPL-3.0-or-later

package com.calmapps.calmmusic.ui.kit

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.calmapps.calmmusic.MonoMusic
import com.calmapps.calmmusic.ui.AlbumUiModel

/** Settings → Album covers (on by default). */
@Composable
fun coversShown(): Boolean {
    val app = LocalContext.current.applicationContext as MonoMusic
    val shown by app.settingsManager.showAlbumCovers.collectAsState()
    return shown
}

/**
 * An album's cover in an outlined tile (zeroheight List "tile icon"). Drawn from memory only: a
 * cover that isn't loaded yet shows the album glyph and starts loading for next time; the row never
 * changes after it is drawn (one paint per screen).
 */
@Composable
fun CoverTile(album: AlbumUiModel, size: Dp = 48.dp) {
    val app = LocalContext.current.applicationContext as MonoMusic
    val cover = remember(album.sourceType, album.id) { app.covers.peek(album) }
    if (cover == null) LaunchedEffect(album.sourceType, album.id) { app.covers.prefetch(listOf(album)) }
    CoverBox(cover, size)
}

/** The tile itself: the picture, or the album glyph while there is none. */
@Composable
fun CoverBox(cover: ImageBitmap?, size: Dp) {
    val shape = RoundedCornerShape(if (size > 64.dp) 8.dp else 4.dp)
    Box(
        Modifier
            .size(size)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.onSurface, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (cover != null) {
            Image(cover, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Icon(Icons.Outlined.Album, contentDescription = null, modifier = Modifier.size(size / 2))
        }
    }
}
