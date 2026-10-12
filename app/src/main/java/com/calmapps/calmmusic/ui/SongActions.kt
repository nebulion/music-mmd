package com.calmapps.calmmusic.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.calmapps.calmmusic.ui.kit.ListRow
import com.calmapps.calmmusic.ui.kit.MmdSheet
import com.calmapps.calmmusic.ui.kit.SheetTitle

/**
 * What can be done to any song, wherever its row is (owner's pick 1A, 2026-10-12: a ⋮ on every
 * row instead of hold → menu, which MMD rules out, P6). Provided once by the activity.
 */
class SongActions(
    val playNext: (SongUiModel) -> Unit = {},
    val addToQueue: (SongUiModel) -> Unit = {},
    /** Null when the song's album can't be found. */
    val goToAlbum: (SongUiModel) -> (() -> Unit)? = { null },
    val goToArtist: (SongUiModel) -> (() -> Unit)? = { null },
    val download: (SongUiModel) -> Unit = {},
)

val LocalSongActions = staticCompositionLocalOf { SongActions() }

/** One action of the song sheet. */
data class SheetAction(val label: String, val run: () -> Unit)

/** The song's actions as a sheet: title, ✕, one row each; a tap closes it and acts. */
@Composable
fun SongActionSheet(title: String, actions: List<SheetAction>, onDismiss: () -> Unit) {
    MmdSheet(onDismiss) {
        SheetTitle(title, onDismiss)
        actions.forEachIndexed { i, action ->
            ListRow(
                title = action.label,
                showDivider = i != actions.lastIndex,
                modifier = Modifier
                    .clickable {
                        onDismiss()
                        action.run()
                    }
                    .padding(horizontal = 16.dp),
            )
        }
    }
}
