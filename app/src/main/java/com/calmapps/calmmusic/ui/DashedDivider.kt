package com.calmapps.calmmusic.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.calmapps.calmmusic.ui.kit.DashedDividerMMD

/** The dotted row divider: always the kit's 1px pixel-snapped line (Kompakt Settings). */
@Suppress("UNUSED_PARAMETER")
@Composable
fun DashedDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = Dp.Unspecified,
) {
    DashedDividerMMD(modifier)
}
