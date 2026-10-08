// SPDX-License-Identifier: GPL-3.0-or-later

package com.calmapps.calmmusic.ui.kit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mudita.mmd.components.text.TextMMD

/**
 * One MMD list row (zeroheight List): label Black 21/23 over supporting text Medium 18/18, one line
 * each with an ellipsis, so every row in a list is the same height and the lines land in the same
 * places after a page turn (Follow the Lines). Regular weight when the label is alone.
 *
 * The list supplies the 16dp side padding; the dotted divider runs under the label.
 */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    bold: Boolean = subtitle != null,
    showDivider: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    subtitleLeading: (@Composable RowScope.() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    leadingWidth: Dp = ListRowDefaults.LeadingWidth,
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().height(if (subtitle != null) ListRowDefaults.TwoLineHeight else ListRowDefaults.OneLineHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                Box(Modifier.size(leadingWidth), contentAlignment = Alignment.CenterStart) { leading() }
            }
            Column(Modifier.weight(1f)) {
                TextMMD(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 21.sp, lineHeight = 23.sp),
                    fontWeight = if (bold) FontWeight.Black else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        subtitleLeading?.invoke(this)
                        TextMMD(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 18.sp, lineHeight = 18.sp),
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            if (trailing != null) {
                Box(Modifier.padding(start = 16.dp).size(48.dp), contentAlignment = Alignment.Center) { trailing() }
            }
        }
        if (showDivider) {
            DashedDividerMMD(Modifier.padding(start = if (leading != null) leadingWidth else 0.dp))
        }
    }
}

object ListRowDefaults {
    /** 15.5 + 23 + 4 + 18 + 15.5 (zeroheight List). */
    val TwoLineHeight: Dp = 76.dp
    val OneLineHeight: Dp = 64.dp
    /** A 28dp glyph or a track number, then the label. */
    val LeadingWidth: Dp = 40.dp
    /** A 48dp cover tile and the 16dp gap after it (zeroheight List: tile icon). */
    val TileWidth: Dp = 64.dp
}
