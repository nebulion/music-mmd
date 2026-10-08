package com.calmapps.calmmusic.ui

import android.content.Context
import androidx.compose.runtime.Composable
import com.calmapps.calmmusic.ui.kit.MmdSheet
import com.calmapps.calmmusic.ui.kit.SheetRadioRow
import com.calmapps.calmmusic.ui.kit.SheetTitle

/**
 * Sorting (owner's pick 3A, 2026-10-08): ⇅ in the top bar opens a sheet of sort keys. Tapping a key
 * sorts by it in its natural direction; tapping the key already in use flips it (newest ↔ oldest,
 * A–Z ↔ Z–A) — no separate "newest first" / "oldest first" rows. Remembered per page.
 */
enum class SortKey(val label: String, val ascending: String, val descending: String, val startAscending: Boolean) {
    TITLE("Title", "A–Z", "Z–A", true),
    NAME("Name", "A–Z", "Z–A", true),
    ARTIST("Artist", "A–Z", "Z–A", true),
    ALBUM("Album", "A–Z", "Z–A", true),
    YEAR("Year", "oldest first", "newest first", false),
    ADDED("Recently added", "oldest first", "newest first", false),
    SONGS("Songs", "fewest first", "most first", false),
    POPULAR("Most popular", "least first", "most first", false),
    MOST_PLAYED("Most played", "least first", "most first", false),
    RECENTLY_PLAYED("Recently played", "longest ago first", "latest first", false),
}

/** Play counts for sorting, looked up per song, album or artist id (see PlayHistory). */
class PlayStats(
    val song: (String) -> com.calmapps.calmmusic.data.PlayStat? = { null },
    val album: (String) -> com.calmapps.calmmusic.data.PlayStat? = { null },
    val artist: (String) -> com.calmapps.calmmusic.data.PlayStat? = { null },
)

data class SortState(val key: SortKey, val ascending: Boolean) {
    val label: String get() = "${key.label} · ${if (ascending) key.ascending else key.descending}"

    /** What tapping [tapped] in the sheet does. */
    fun tap(tapped: SortKey): SortState =
        if (tapped == key) copy(ascending = !ascending) else SortState(tapped, tapped.startAscending)
}

/** The pages that sort, their keys and where they start. */
enum class SortPage(val title: String, val keys: List<SortKey>, val default: SortState) {
    SONGS("Sort songs", listOf(SortKey.TITLE, SortKey.ARTIST, SortKey.ALBUM, SortKey.ADDED, SortKey.MOST_PLAYED, SortKey.RECENTLY_PLAYED), SortState(SortKey.TITLE, true)),
    ALBUMS("Sort albums", listOf(SortKey.TITLE, SortKey.ARTIST, SortKey.YEAR, SortKey.ADDED, SortKey.MOST_PLAYED, SortKey.RECENTLY_PLAYED), SortState(SortKey.TITLE, true)),
    ARTISTS("Sort artists", listOf(SortKey.NAME, SortKey.SONGS, SortKey.ADDED, SortKey.MOST_PLAYED, SortKey.RECENTLY_PLAYED), SortState(SortKey.NAME, true)),
    ARTIST_PAGE("Sort albums", listOf(SortKey.YEAR, SortKey.TITLE, SortKey.POPULAR), SortState(SortKey.YEAR, false)),
}

class SortStore(context: Context) {
    private val prefs = context.getSharedPreferences("sorting", Context.MODE_PRIVATE)

    fun get(page: SortPage): SortState {
        val key = prefs.getString("${page.name}.key", null)?.let { runCatching { SortKey.valueOf(it) }.getOrNull() }
            ?.takeIf { it in page.keys } ?: return page.default
        return SortState(key, prefs.getBoolean("${page.name}.asc", key.startAscending))
    }

    fun set(page: SortPage, state: SortState) {
        prefs.edit().putString("${page.name}.key", state.key.name).putBoolean("${page.name}.asc", state.ascending).apply()
    }
}

private val textOrder = compareBy<String?, String?>(nullsLast(String.CASE_INSENSITIVE_ORDER)) { it?.trim()?.removePrefix("The ")?.takeIf { s -> s.isNotEmpty() } }

/** Missing values (no year, no date) always go last, whichever way the sort runs. */
private fun <T, K : Comparable<K>> List<T>.by(ascending: Boolean, key: (T) -> K?): List<T> {
    val (has, missing) = partition { key(it) != null }
    val sorted = has.sortedBy { key(it)!! }
    return (if (ascending) sorted else sorted.reversed()) + missing
}

private fun <T> List<T>.byText(ascending: Boolean, key: (T) -> String?): List<T> {
    val sorted = sortedWith(compareBy(textOrder) { key(it) })
    return if (ascending) sorted else sorted.reversed()
}

@JvmName("sortedSongsFor")
fun List<SongUiModel>.sortedFor(state: SortState, stats: PlayStats = PlayStats()): List<SongUiModel> = when (state.key) {
    SortKey.MOST_PLAYED -> by(state.ascending) { stats.song(it.id)?.count }
    SortKey.RECENTLY_PLAYED -> by(state.ascending) { stats.song(it.id)?.lastPlayed }
    SortKey.ARTIST -> byText(state.ascending) { it.artist }
    SortKey.ALBUM -> byText(state.ascending) { it.album }
    SortKey.ADDED -> by(state.ascending) { it.addedAt }
    else -> byText(state.ascending) { it.title }
}

@JvmName("sortedAlbumsFor")
fun List<AlbumUiModel>.sortedFor(state: SortState, stats: PlayStats = PlayStats()): List<AlbumUiModel> = when (state.key) {
    SortKey.MOST_PLAYED -> by(state.ascending) { stats.album(it.id)?.count }
    SortKey.RECENTLY_PLAYED -> by(state.ascending) { stats.album(it.id)?.lastPlayed }
    SortKey.ARTIST -> byText(state.ascending) { it.artist }
    SortKey.YEAR -> by(state.ascending) { it.releaseYear }
    SortKey.ADDED -> by(state.ascending) { it.addedAt }
    // YouTube's own order is its popularity order
    SortKey.POPULAR -> if (state.ascending) reversed() else this
    else -> byText(state.ascending) { it.title }
}

fun List<ArtistUiModel>.sortedArtistsFor(state: SortState, stats: PlayStats = PlayStats()): List<ArtistUiModel> = when (state.key) {
    SortKey.MOST_PLAYED -> by(state.ascending) { stats.artist(it.id)?.count }
    SortKey.RECENTLY_PLAYED -> by(state.ascending) { stats.artist(it.id)?.lastPlayed }
    SortKey.SONGS -> by(state.ascending) { it.songCount }
    SortKey.ADDED -> by(state.ascending) { it.addedAt }
    else -> byText(state.ascending) { it.name }
}

/** The sort sheet: one radio row per key; the one in use says which way it runs. */
@Composable
fun SortSheet(page: SortPage, state: SortState, onChange: (SortState) -> Unit, onDismissRequest: () -> Unit) {
    MmdSheet(onDismissRequest) {
        SheetTitle(page.title, onDismissRequest)
        page.keys.forEachIndexed { i, key ->
            SheetRadioRow(
                label = if (key == state.key) state.label else key.label,
                selected = key == state.key,
                showDivider = i != page.keys.lastIndex,
            ) {
                onChange(state.tap(key))
                onDismissRequest()
            }
        }
    }
}
