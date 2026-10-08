// SPDX-License-Identifier: GPL-3.0-or-later

package com.calmapps.calmmusic.ui.kit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mudita.mmd.components.text.TextMMD
import kotlinx.coroutines.delay

/**
 * A waiting message that only appears if the wait is long: nothing for the first half second (most
 * loads are quicker, and a flash of "Loading" is an extra E Ink repaint), then static text.
 */
@Composable
fun DelayedText(text: String, modifier: Modifier = Modifier.fillMaxSize(), delayMs: Long = 500) {
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMs)
        show = true
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        if (show) TextMMD(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}
