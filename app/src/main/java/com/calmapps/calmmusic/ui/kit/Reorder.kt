package com.calmapps.calmmusic.ui.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs

/** The drag in progress: the row picked up and the boundary it would drop on (lazy-list indices). */
class ReorderState {
    var dragging by mutableStateOf<Int?>(null)
        internal set
    var boundary by mutableStateOf<Int?>(null)
        internal set

    /** True when the line under row [index] (between it and the next) is where the drop would land. */
    fun boldBelow(index: Int): Boolean = boundary == index + 1

    /** True when the line above row [index] is where the drop would land (only drawn for the first row). */
    fun boldAbove(index: Int): Boolean = boundary == index
}

/**
 * Hold a row, then either let go to select it or, still holding, drag it to a new place (owner,
 * 2026-10-07; the drag as in the owner's MMD habits app, loop-habit-tracker-fork
 * HabitListScreenMMD `reorderGesture`): the list stays put, the divider the row would drop into
 * turns bold, and holding near the top or bottom edge turns one page a second. It works in the
 * Initial pass from the container around the list, ahead of the list's own page-on-swipe, and
 * consumes nothing until a hold is recognised, so taps and swipes behave as before. Rows in the
 * range must not have their own long-click.
 *
 * Rows [rows] (lazy indices, contiguous) take part; [onHold] gets a held-and-released row and
 * [onDrop] a dragged row's old position and its new one, all counted from [rows].first. Only rows
 * [canDrag] allows can be dragged.
 */
fun Modifier.reorder(
    listState: LazyListState,
    reorder: ReorderState,
    rows: IntRange,
    canDrag: (index: Int) -> Boolean = { true },
    onHold: (index: Int) -> Unit = {},
    onDrop: (from: Int, to: Int) -> Unit,
): Modifier = composed {
    val allowed = rememberUpdatedState(canDrag)
    val hold = rememberUpdatedState(onHold)
    val scope = rememberCoroutineScope()
    val edge = with(LocalDensity.current) { 56.dp.toPx() }
    val range = rememberUpdatedState(rows)
    val drop = rememberUpdatedState(onDrop)
    pointerInput(listState) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val start = listState.layoutInfo.indexAt(down.position.y) ?: return@awaitEachGesture
            val movable = range.value
            if (start !in movable) return@awaitEachGesture
            // a long press is "still down, and still roughly here, when the timeout expires"
            val slop = viewConfiguration.touchSlop
            val moved = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: return@withTimeoutOrNull true
                    if (!change.pressed) return@withTimeoutOrNull true
                    if (abs(change.position.y - down.position.y) > slop) return@withTimeoutOrNull true
                }
                @Suppress("UNREACHABLE_CODE") true
            }
            if (moved != null) return@awaitEachGesture

            // held: letting go here selects; moving past the slop starts the drag
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: return@awaitEachGesture
                change.consume() // the row must not take this as a tap, nor the list page
                if (!change.pressed) {
                    hold.value(start - movable.first)
                    return@awaitEachGesture
                }
                if (abs(change.position.y - down.position.y) > slop) break
            }
            if (!allowed.value(start)) {
                // a row that can't move: the rest of the gesture does nothing
                while (true) {
                    val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } ?: break
                    change.consume()
                    if (!change.pressed) break
                }
                return@awaitEachGesture
            }

            var lastTurn = 0L
            var turn: Job? = null
            reorder.dragging = start
            reorder.boundary = null
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                val pressed = change.pressed
                change.consume() // the list must not page under the drag, nor the row take a tap
                if (!pressed) break
                val info = listState.layoutInfo
                reorder.boundary = info.nearestBoundary(change.position.y, start, movable)
                val now = System.currentTimeMillis()
                if (now - lastTurn >= 1000L) {
                    val forward = change.position.y >= info.viewportEndOffset - edge
                    val back = change.position.y <= info.viewportStartOffset + edge
                    if (forward || back) {
                        lastTurn = now
                        turn?.cancel()
                        turn = scope.launch { if (forward) listState.pageForward() else listState.pageBack() }
                    }
                }
            }
            turn?.cancel()
            val landed = reorder.boundary
            reorder.dragging = null
            reorder.boundary = null
            if (landed != null) {
                // a boundary below the row absorbs the place it leaves
                val to = if (landed > start) landed - 1 else landed
                drop.value(start - movable.first, to - movable.first)
            }
        }
    }
}

private fun LazyListLayoutInfo.indexAt(y: Float): Int? =
    visibleItemsInfo.firstOrNull { y >= it.offset && y < it.offset + it.size }?.index

/** The boundary between movable rows nearest [y], or null when it is one of the row's own two. */
private fun LazyListLayoutInfo.nearestBoundary(y: Float, start: Int, rows: IntRange): Int? {
    var best: Int? = null
    var distance = Float.MAX_VALUE
    visibleItemsInfo.filter { it.index in rows }.forEach { item ->
        listOf(item.index to item.offset, item.index + 1 to item.offset + item.size).forEach { (b, at) ->
            val d = abs(y - at)
            if (d < distance) {
                distance = d
                best = b
            }
        }
    }
    return best?.takeUnless { it == start || it == start + 1 }
}

/**
 * The divider a drop points at: the usual dashed rule, bolded where the row would land. Both sit
 * in the same 3dp slot, so bolding one never moves the rows below it (a whole-screen repaint).
 */
@Composable
fun DropDivider(bold: Boolean) {
    Box(
        Modifier.fillMaxWidth().height(DropSlot).padding(start = RowDefaults.EdgePadding),
        contentAlignment = Alignment.Center,
    ) {
        if (bold) {
            Box(Modifier.fillMaxWidth().height(DropSlot).background(MaterialTheme.colorScheme.onSurface))
        } else {
            DashedDividerMMD()
        }
    }
}

/** The line above the first movable row: an empty slot unless the drop would land there. */
@Composable
fun DropLineAbove(bold: Boolean) {
    Box(Modifier.fillMaxWidth().height(DropSlot).padding(start = RowDefaults.EdgePadding)) {
        if (bold) Box(Modifier.fillMaxWidth().height(DropSlot).background(MaterialTheme.colorScheme.onSurface))
    }
}

private val DropSlot = 3.dp
