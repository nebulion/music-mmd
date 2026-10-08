// SPDX-License-Identifier: GPL-3.0-or-later

package com.calmapps.calmmusic.ui.kit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.radio_button.RadioButtonMMD

/**
 * Settings → E Ink: flash to clear ghosting (on by default, every 12 taps), as in Fit and Macros.
 * Five intervals fit on the page, so they are radio rows here rather than a sheet.
 */
@Composable
fun EinkSettings() {
    var enabled by remember { mutableStateOf(EinkRefresh.enabled) }
    var actions by remember { mutableIntStateOf(EinkRefresh.actions) }
    Column(Modifier.fillMaxSize()) {
        SwitchRow(
            title = "Flash to clear ghosting",
            subtitle = "Turns the screen black for a moment",
            checked = enabled,
            onCheckedChange = {
                enabled = it
                EinkRefresh.enabled = it
            },
        )
        RowDivider()
        if (enabled) {
            SectionTitle("Flash every")
            EinkRefresh.ACTION_CHOICES.forEachIndexed { i, n ->
                ListRow(
                    title = "$n taps",
                    showDivider = i != EinkRefresh.ACTION_CHOICES.lastIndex,
                    modifier = Modifier
                        .selectable(selected = actions == n, role = Role.RadioButton) {
                            actions = n
                            EinkRefresh.actions = n
                        }
                        .padding(horizontal = 16.dp),
                    trailing = { RadioButtonMMD(selected = actions == n, onClick = null) },
                )
            }
        }
    }
}
