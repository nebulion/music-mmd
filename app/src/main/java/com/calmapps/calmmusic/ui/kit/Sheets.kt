// SPDX-License-Identifier: GPL-3.0-or-later
// After the Macros kit's Sheets.kt (pattern P4: a choice from a short list is a sheet of radio rows).

package com.calmapps.calmmusic.ui.kit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.mudita.mmd.components.bottom_sheet.ModalBottomSheetMMD
import com.mudita.mmd.components.bottom_sheet.rememberModalBottomSheetMMDState
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.radio_button.RadioButtonMMD
import com.mudita.mmd.components.text.TextMMD

/**
 * The bottom-anchored surface for choices: opens fully expanded, no drag handle (a handle
 * advertises a drag that only causes partial repaints), no dim, a 3dp rule on its top edge.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MmdSheet(
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheetMMD(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetMMDState(skipPartiallyExpanded = true),
        dragHandle = null,
    ) {
        KeepStatusBar()
        HorizontalDividerMMD(thickness = 3.dp, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(2.dp))
        content()
        Spacer(Modifier.height(8.dp))
    }
}

/** A sheet is a window of its own; keep the status bar shown, dark on white, while it is open. */
@Composable
fun KeepStatusBar() {
    val view = LocalView.current
    SideEffect {
        val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.show(WindowInsetsCompat.Type.statusBars())
        controller.isAppearanceLightStatusBars = true
        controller.isAppearanceLightNavigationBars = true
    }
}

/** A sheet's title, bold on the left, and the ✕ that closes it on the right. */
@Composable
fun SheetTitle(text: String, onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextMMD(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Outlined.Close, contentDescription = "Close", modifier = Modifier.size(28.dp))
        }
    }
}

/** One radio row of a sheet; the whole row is the target, the radio itself is inert. */
@Composable
fun SheetRadioRow(label: String, selected: Boolean, showDivider: Boolean, onClick: () -> Unit) {
    ListRow(
        title = label,
        bold = selected,
        showDivider = showDivider,
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 16.dp),
        trailing = { RadioButtonMMD(selected = selected, onClick = null) },
    )
}

/** A single choice from up to six (pattern P4): picking one commits and closes. */
@Composable
fun <T> ChoiceSheet(
    title: String,
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    onDismissRequest: () -> Unit,
) {
    MmdSheet(onDismissRequest) {
        SheetTitle(title, onDismissRequest)
        options.forEachIndexed { i, option ->
            SheetRadioRow(label(option), option == selected, i != options.lastIndex) {
                onSelect(option)
                onDismissRequest()
            }
        }
    }
}
