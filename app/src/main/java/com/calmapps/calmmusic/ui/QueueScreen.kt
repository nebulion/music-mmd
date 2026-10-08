package com.calmapps.calmmusic.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.calmapps.calmmusic.ui.kit.DropDivider
import com.calmapps.calmmusic.ui.kit.DropLineAbove
import com.calmapps.calmmusic.ui.kit.ListRow
import com.calmapps.calmmusic.ui.kit.PagedList
import com.calmapps.calmmusic.ui.kit.ReorderState
import com.calmapps.calmmusic.ui.kit.reorder
import com.mudita.mmd.components.text.TextMMD

/**
 * What plays next. Tap a song to play it; hold and drag (without letting go) to move it — the line
 * where it would land turns bold (pattern P7, as in Macros and Habits); ✕ takes it out of the queue.
 * Opens on the page with the playing song.
 */
@Composable
fun QueueScreen(
    queue: List<SongUiModel>,
    currentIndex: Int?,
    onPlay: (index: Int) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    onRemove: (index: Int) -> Unit,
) {
    if (queue.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            TextMMD("Nothing queued", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }
    // start where the music is: the playing song is the first row on screen
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (currentIndex ?: 0).coerceIn(0, queue.lastIndex))
    val reorder = remember { ReorderState() }
    PagedList(
        modifier = Modifier.fillMaxSize().reorder(listState, reorder, queue.indices) { from, to ->
            onMove(from, to.coerceIn(0, queue.lastIndex))
        },
        state = listState,
        canGrow = true,
    ) {
        queue.forEachIndexed { i, song ->
            item(key = "$i:${song.id}") {
                Column {
                    if (i == 0) DropLineAbove(reorder.boldAbove(i))
                    val playing = i == currentIndex
                    ListRow(
                        title = song.title,
                        // length first: a long artist name is cut, never the time
                        subtitle = listOfNotNull(song.durationText, song.artist.takeIf { it.isNotBlank() }).joinToString(" · "),
                        showDivider = false,
                        modifier = Modifier.clickable { onPlay(i) }.padding(horizontal = 16.dp),
                        leading = {
                            if (playing) Icon(Icons.Outlined.Headphones, contentDescription = "Playing", modifier = Modifier.size(24.dp))
                        },
                        trailing = if (playing) null else {
                            {
                                IconButton(onClick = { onRemove(i) }) {
                                    Icon(Icons.Outlined.Close, contentDescription = "Remove from queue")
                                }
                            }
                        },
                    )
                    DropDivider(reorder.boldBelow(i))
                }
            }
        }
    }
}
