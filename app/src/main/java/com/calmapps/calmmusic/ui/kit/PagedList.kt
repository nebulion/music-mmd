// SPDX-License-Identifier: GPL-3.0-or-later

package com.calmapps.calmmusic.ui.kit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.lazy.LazyDefaultsMMD
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlinx.coroutines.coroutineScope
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitEachGesture

/**
 * A list that pages instead of scrolling, with a permanent scrollbar in MMD's own look.
 *
 * Paging (owner, 2026-09-29): a page turn brings **the last row you could see to the top**, so
 * nothing is skipped and you keep your place. `LazyColumnMMD` pages by a fixed number of items;
 * here the swipe step is "rows on screen less one" and the arrows turn by the exact height.
 *
 * The scrollbar: MMD's own sizes its thumb by *item count*, so a page of a few tall rows (the edit
 * screen) showed a sliver of a thumb. This one is drawn from the same parts (MMD's `chevron_*`
 * drawables, `LazyDefaultsMMD` track and border) but sized by height: the thumb is the share of
 * the list's height that is on screen, from each row's measured height (see [Heights]). Tap the arrows to turn a page, hold them to jump to either end, tap
 * the track to jump there.
 *
 * Read `LazyMMD.kt` before changing how this is used:
 * - Emit one item per row; a single tall item cannot be paged through.
 * - [canGrow]: a list whose rows can come and go while it is open keeps an inactive bar when it
 *   fits, so rows never shift sideways when it grows.
 */
@Composable
fun PagedList(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    // Music: shown when the list runs past one page (MMD "always show UI controls", as Anki and
    // Fit do). Macros has none by the owner's choice; one rule for all apps is still open.
    isScrollbarVisible: Boolean = true,
    canGrow: Boolean = false,
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(),
    verticalArrangement: androidx.compose.foundation.layout.Arrangement.Vertical = androidx.compose.foundation.layout.Arrangement.Top,
    content: LazyListScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    Row(modifier) {
        // Foundation's list, not LazyColumnMMD (owner, 2026-10-06: "top bar and content at the same
        // time"): LazyColumnMMD draws itself at alpha 0 until its first layout, so every screen
        // painted twice, bar first and rows a frame later. Paging is ours (pageOnSwipe) either way.
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.weight(1f).pageOnSwipe(state),
            state = state,
            contentPadding = contentPadding,
            verticalArrangement = verticalArrangement,
            userScrollEnabled = false,
            content = content,
        )
        // Always the same column (owner, 2026-10-08: pages painted twice): a bar that appears once
        // the list has measured itself narrows every row a frame later. The thumb and arrows are
        // worked out while drawing, from this frame's layout, so the first frame is already right.
        if (isScrollbarVisible) {
            PageScrollbar(
                state = state,
                onPage = { forward -> scope.launch { if (forward) state.pageForward() else state.pageBack() } },
                onEnd = { forward ->
                    scope.launch {
                        state.scrollToItem(if (forward) (state.layoutInfo.totalItemsCount - 1).coerceAtLeast(0) else 0)
                    }
                },
                onJump = { index -> scope.launch { state.scrollToItem(index) } },
            )
        }
    }
}

/**
 * A short swipe turns a whole page (owner, 2026-10-02: swipes needed most of the screen). Left to
 * itself the list follows the finger, so a page took a swipe as long as the page. This looks at
 * the touch first: once the finger has moved past the touch slop up or down, the page turns and the
 * rest of the gesture is taken, so the list never drags. Taps never move that far and pass through.
 */
private fun Modifier.pageOnSwipe(state: LazyListState): Modifier = pointerInput(state) {
    val slop = viewConfiguration.touchSlop
    coroutineScope {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var moved = 0f
            var paged = false
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) break
                moved += change.positionChange().y
                if (!paged && abs(moved) > slop) {
                    paged = true
                    val forward = moved < 0
                    launch { if (forward) state.pageForward() else state.pageBack() }
                }
                if (paged) change.consume()
            }
        }
    }
}

/**
 * Where each page turned forward from, per list, so that turning back returns to exactly that page
 * (owner, 2026-10-07: down then up showed a different page, because the page before was worked out
 * again from row heights). Entries at or past the current page are stale (the list changed) and
 * dropped.
 */
private val pageStarts = java.util.WeakHashMap<LazyListState, ArrayDeque<Pair<Int, Int>>>()

/** The last row on screen, even if cut off, becomes the first. */
suspend fun LazyListState.pageForward() {
    val items = layoutInfo.visibleItemsInfo
    val first = items.firstOrNull() ?: return
    val last = items.last()
    val to = if (last.index > first.index) last.index else first.index + 1
    if (to >= layoutInfo.totalItemsCount || !canScrollForward) return
    pageStarts.getOrPut(this) { ArrayDeque() }.addLast(firstVisibleItemIndex to firstVisibleItemScrollOffset)
    scrollToItem(to)
}

/** Back to the page this one was turned to from; failing that, the first row on screen becomes the last whole row of the page before. */
suspend fun LazyListState.pageBack() {
    val here = firstVisibleItemIndex to firstVisibleItemScrollOffset
    val starts = pageStarts[this]
    while (starts != null && starts.isNotEmpty()) {
        val (index, offset) = starts.removeLast()
        if (index < here.first || (index == here.first && offset < here.second)) {
            scrollToItem(index, offset)
            if (index == 0 && offset == 0) starts.clear()
            return
        }
    }
    val info = layoutInfo
    val first = info.visibleItemsInfo.firstOrNull() ?: return
    val viewport = info.viewportEndOffset - info.viewportStartOffset
    val back = viewport - (first.offset + first.size)
    if (back <= 0) {
        scrollToItem((first.index - 1).coerceAtLeast(0))
        return
    }
    scrollBy(-back.toFloat())
    // Start the page on a whole row; the old first row still fits below it. When the row above is
    // taller than the page (a dashboard card), snapping forward would land back where we started
    // and the page would never turn: show that row from its top instead.
    if (firstVisibleItemScrollOffset > 0) {
        val next = firstVisibleItemIndex + 1
        scrollToItem(if (next >= first.index) (first.index - 1).coerceAtLeast(0) else next)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PageScrollbar(
    state: LazyListState,
    onPage: (forward: Boolean) -> Unit,
    onEnd: (forward: Boolean) -> Unit,
    onJump: (index: Int) -> Unit,
) {
    // read while drawing, not composing: no recomposition, no second frame
    val atStart = { !state.canScrollBackward }
    val atEnd = { !state.canScrollForward }
    // MMD's own slider colours are internal; they are the theme's surface and its ink
    val heights = remember(state) { Heights() }
    val ink = MaterialTheme.colorScheme.onSurface
    val paper = MaterialTheme.colorScheme.surface
    Column(
        // `LazyMMD.kt`'s own scrollbar column: 8dp either side of a 24dp arrow, so 40dp in all
        Modifier.fillMaxHeight().padding(horizontal = PagedListDefaults.ScrollbarSidePadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ScrollArrow(
            up = true,
            active = { !atStart() },
            onClick = { onPage(false) },
            onLongClick = { onEnd(false) },
        )
        Canvas(
            Modifier
                .width(PagedListDefaults.ScrollbarTrackWidth)
                .weight(1f)
                .border(LazyDefaultsMMD.sliderBackgroundBorderWidth, ink, LazyDefaultsMMD.sliderBackgroundCorners)
                .pointerInput(state) {
                    detectTapGestures { tap ->
                        if (state.layoutInfo.totalItemsCount > 0) {
                            heights.update(state.layoutInfo)
                            onJump(heights.indexAt(tap.y / size.height * heights.total()))
                        }
                    }
                },
        ) {
            val radius = CornerRadius(size.width / 2, size.width / 2)
            drawRoundRect(color = paper, size = size, cornerRadius = radius)
            val thumb = thumbOf(state, heights, atStart(), atEnd()) ?: return@Canvas
            // the keyboard can squeeze the track below the thumb's minimum (search): fill it then
            val minHeight = minOf(maxOf(size.height * 0.05f, LazyDefaultsMMD.VERTICAL_SLIDER_MIN_HEIGHT), size.height)
            val height = (size.height * thumb.second).coerceIn(minHeight, size.height)
            val top = ((size.height - height) * thumb.first).coerceIn(0f, size.height - height)
            drawRoundRect(color = ink, topLeft = Offset(0f, top), size = Size(size.width, height), cornerRadius = radius)
        }
        ScrollArrow(
            up = false,
            active = { !atEnd() },
            onClick = { onPage(true) },
            onLongClick = { onEnd(true) },
        )
    }
}

/**
 * Row heights seen so far, by index. A list's rows differ a lot (a section title, a two-line row,
 * a body map), so the thumb is worked out from each row's own height once it has been on screen,
 * and from the average of the measured ones until then. Forgotten when the row count changes.
 */
private class Heights {
    private val sizes = HashMap<Int, Int>()
    private var count = -1

    fun update(info: LazyListLayoutInfo) {
        if (info.totalItemsCount != count) {
            sizes.clear()
            count = info.totalItemsCount
        }
        info.visibleItemsInfo.forEach { sizes[it.index] = it.size + info.mainAxisItemSpacing }
    }

    private fun average() = if (sizes.isEmpty()) 1f else sizes.values.sum().toFloat() / sizes.size

    fun of(index: Int): Float = sizes[index]?.toFloat() ?: average()

    /** Height of rows 0 until [index]. */
    fun before(index: Int): Float {
        var known = 0f
        var unknown = 0
        for (i in 0 until index) {
            val h = sizes[i]
            if (h != null) known += h else unknown++
        }
        return known + unknown * average()
    }

    fun total(): Float = before(count)

    /** The row at [y] pixels down the whole list. */
    fun indexAt(y: Float): Int {
        var top = 0f
        for (i in 0 until count) {
            top += of(i)
            if (top > y) return i
        }
        return (count - 1).coerceAtLeast(0)
    }
}

/**
 * Where the thumb sits (0 = top, 1 = bottom) and how much of the track it fills: the share of the
 * list's height that is on screen. Null when the whole list is on screen (inactive bar).
 */
private fun thumbOf(state: LazyListState, heights: Heights, atStart: Boolean, atEnd: Boolean): Pair<Float, Float>? {
    val info = state.layoutInfo
    heights.update(info)
    if (atStart && atEnd) return null
    val items = info.visibleItemsInfo
    if (items.isEmpty() || info.totalItemsCount == 0) return null
    val viewport = (info.viewportEndOffset - info.viewportStartOffset).toFloat()
    val content = heights.total().coerceAtLeast(viewport)
    val scrolled = heights.before(items.first().index) - items.first().offset
    val position = when {
        atStart -> 0f
        atEnd -> 1f
        else -> (scrolled / (content - viewport)).coerceIn(0f, 1f)
    }
    return position to (viewport / content).coerceIn(0f, 1f)
}

/**
 * One of the scrollbar's page arrows: MMD's filled chevron when it can be used, its dotted one for
 * "nothing more this way", as `LazyMMD.kt` draws them.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScrollArrow(
    up: Boolean,
    active: () -> Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    // MMD's filled chevron when it can be used, its dotted one for "nothing more this way", as
    // `LazyMMD.kt` draws them; chosen while drawing so the first frame shows the right one
    val filled = painterResource(if (up) com.mudita.mmd.R.drawable.chevron_filled_up else com.mudita.mmd.R.drawable.chevron_filled_down)
    val dotted = painterResource(if (up) com.mudita.mmd.R.drawable.chevron_dotted_up else com.mudita.mmd.R.drawable.chevron_dotted_down)
    Canvas(
        Modifier
            .padding(vertical = PagedListDefaults.ScrollbarArrowPadding)
            .size(PagedListDefaults.ScrollbarArrowSize)
            .combinedClickable(onClick = { if (active()) onClick() }, onLongClick = { if (active()) onLongClick() }),
    ) {
        with(if (active()) filled else dotted) { draw(size) }
    }
}

object PagedListDefaults {
    /**
     * Width of MMD's scrollbar column: its 24dp page arrows (`LazyDefaultsMMD.navigateIconSize`)
     * with 8dp padding either side (`LazyMMD.kt`). Matches the Kompakt's Settings, whose rows end
     * 40dp from the edge (`docs/mmd/eink-design.md` → Calibration).
     */
    val ScrollbarGutter = 40.dp

    /** The gap either side of an arrow, and above and below it, in `LazyMMD.kt`'s own scrollbar. */
    val ScrollbarSidePadding = 8.dp
    val ScrollbarArrowPadding = 16.dp

    /** The track's width, from `LazyDefaultsMMD.sliderBackgroundWidth`, which is internal to MMD. */
    val ScrollbarTrackWidth = 8.dp

    /** The page arrows, from `LazyDefaultsMMD.navigateIconSize`. */
    val ScrollbarArrowSize = 24.dp
}
