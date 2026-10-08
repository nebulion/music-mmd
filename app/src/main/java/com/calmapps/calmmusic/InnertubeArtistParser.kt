package com.calmapps.calmmusic

import org.json.JSONArray
import org.json.JSONObject

/**
 * Artists on YouTube Music: search results, the artist page and the full
 * discography behind its "More" link. Pure functions over the InnerTube JSON,
 * tested against saved responses (src/test/resources/innertube).
 *
 * Shapes, as returned by WEB_REMIX in 2026-10:
 * - search (artists filter): `musicResponsiveListItemRenderer` rows whose
 *   `navigationEndpoint.browseEndpoint.browseId` is the artist's `UC…` id.
 * - artist page (`browse UC…`): `musicShelfRenderer` "Top songs", then
 *   `musicCarouselShelfRenderer`s ("Albums", "Singles & EPs", …) of
 *   `musicTwoRowItemRenderer` (`MPREb_…` ids). Each carousel header's
 *   `moreContentButton` browses the full list (`MPAD…` + params).
 * - discography (`browse MPAD…`): a grid of `musicTwoRowItemRenderer`.
 */

data class InnertubeArtistResult(
    val artistId: String,
    val name: String,
    /** e.g. "207M monthly audience" */
    val subtitle: String?,
)

/** A browse call that lists more of a carousel. */
data class InnertubeBrowseRef(val browseId: String, val params: String?)

data class InnertubeArtistPage(
    val artistId: String,
    val name: String,
    val topSongs: List<InnertubeSongResult>,
    val albums: List<InnertubeAlbumResult>,
    val singles: List<InnertubeAlbumResult>,
    val allAlbums: InnertubeBrowseRef?,
    val allSingles: InnertubeBrowseRef?,
)

internal object InnertubeArtistParser {

    fun parseArtistSearch(root: JSONObject, limit: Int): List<InnertubeArtistResult> {
        val rows = mutableListOf<JSONObject>()
        collect(root, "musicResponsiveListItemRenderer", rows)
        return rows.mapNotNull { row ->
            val browse = row.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")
            val id = browse?.optString("browseId").orEmpty()
            if (!id.startsWith("UC")) return@mapNotNull null
            val columns = row.optJSONArray("flexColumns")
            val name = columnText(columns, 0)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val subtitle = columnRuns(columns, 1)
                ?.let { runs -> (0 until runs.length()).map { runs.optJSONObject(it)?.optString("text").orEmpty().trim() } }
                ?.filter { it.isNotEmpty() && it != "•" && !it.equals("Artist", ignoreCase = true) && !isSeparator(it) }
                ?.joinToString(" · ")
                ?.takeIf { it.isNotBlank() }
            InnertubeArtistResult(id, name, subtitle)
        }.distinctBy { it.artistId }.take(limit)
    }

    fun parseArtistPage(artistId: String, root: JSONObject): InnertubeArtistPage {
        val header = root.optJSONObject("header")?.let { h -> h.keys().asSequence().firstOrNull()?.let { h.optJSONObject(it) } }
        val name = header?.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text").orEmpty()

        val shelves = mutableListOf<JSONObject>()
        collect(root.optJSONObject("contents"), "musicShelfRenderer", shelves)
        val topSongs = shelves.firstOrNull()?.optJSONArray("contents")?.let { rows ->
            (0 until rows.length()).mapNotNull { rows.optJSONObject(it)?.optJSONObject("musicResponsiveListItemRenderer") }
                .mapNotNull { parseSongRow(it) }
        }.orEmpty()

        var albums = emptyList<InnertubeAlbumResult>()
        var singles = emptyList<InnertubeAlbumResult>()
        var allAlbums: InnertubeBrowseRef? = null
        var allSingles: InnertubeBrowseRef? = null

        val carousels = mutableListOf<JSONObject>()
        collect(root.optJSONObject("contents"), "musicCarouselShelfRenderer", carousels)
        for (carousel in carousels) {
            val headerRenderer = carousel.optJSONObject("header")?.optJSONObject("musicCarouselShelfBasicHeaderRenderer")
            val title = headerRenderer?.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text").orEmpty()
            val more = headerRenderer?.optJSONObject("moreContentButton")?.optJSONObject("buttonRenderer")
                ?.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")
                ?.let { b -> b.optString("browseId").takeIf { it.isNotBlank() }?.let { InnertubeBrowseRef(it, b.optString("params").takeIf { p -> p.isNotBlank() }) } }
            val items = carousel.optJSONArray("contents").twoRowItems().mapNotNull { parseTwoRowAlbum(it, name) }
            when {
                title.equals("Albums", ignoreCase = true) -> { albums = items; allAlbums = more }
                title.startsWith("Singles", ignoreCase = true) -> { singles = items; allSingles = more }
            }
        }

        return InnertubeArtistPage(artistId, name, topSongs, albums, singles, allAlbums, allSingles)
    }

    /** Every album (or single) in a discography grid. */
    fun parseDiscography(root: JSONObject, artistName: String?): List<InnertubeAlbumResult> {
        val items = mutableListOf<JSONObject>()
        collect(root, "musicTwoRowItemRenderer", items)
        return items.mapNotNull { parseTwoRowAlbum(it, artistName) }.distinctBy { it.albumId }
    }

    /** An album tile: title, `MPREb_…` id, year from the subtitle ("Album • 2007"). */
    private fun parseTwoRowAlbum(item: JSONObject, artistName: String?): InnertubeAlbumResult? {
        val title = item.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text").orEmpty()
        if (title.isBlank()) return null
        val id = item.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")?.optString("browseId").orEmpty()
        if (!id.startsWith("MPRE")) return null
        val subtitle = item.optJSONObject("subtitle")?.optJSONArray("runs")
        val year = subtitle?.let { runs ->
            (0 until runs.length()).firstNotNullOfOrNull { i ->
                runs.optJSONObject(i)?.optString("text")?.trim()?.toIntOrNull()?.takeIf { it in 1900..2100 }
            }
        }
        return InnertubeAlbumResult(albumId = id, title = title, artist = artistName?.takeIf { it.isNotBlank() }, year = year, coverUrl = thumbnailOf(item))
    }

    /**
     * A song row (search results, an artist's top songs). Keeps the artist's
     * `UC…` id and the album's `MPREb_…` id so both can be opened.
     */
    fun parseSongRow(item: JSONObject): InnertubeSongResult? {
        val columns = item.optJSONArray("flexColumns") ?: return null
        val titleRun = columnRuns(columns, 0)?.optJSONObject(0) ?: return null
        val title = titleRun.optString("text").orEmpty()
        if (title.isBlank()) return null

        val videoId = titleRun.optJSONObject("navigationEndpoint")?.optJSONObject("watchEndpoint")?.optString("videoId")
            .takeUnless { it.isNullOrBlank() }
            ?: item.optJSONObject("playlistItemData")?.optString("videoId").takeUnless { it.isNullOrBlank() }
            ?: return null

        var artist: String? = null
        var artistId: String? = null
        var album: String? = null
        var albumId: String? = null
        var durationText: String? = null

        for (i in 1 until columns.length()) {
            val runs = columnRuns(columns, i) ?: continue
            for (k in 0 until runs.length()) {
                val run = runs.optJSONObject(k) ?: continue
                val text = run.optString("text").orEmpty().trim()
                if (text.isEmpty() || isSeparator(text)) continue
                val browseId = run.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")?.optString("browseId")
                when {
                    artistId == null && browseId != null && browseId.startsWith("UC") -> { artist = text; artistId = browseId }
                    albumId == null && browseId != null && browseId.startsWith("MPRE") -> { album = text; albumId = browseId }
                    artist == null && browseId.isNullOrBlank() && !text.contains(":") && !isCount(text) &&
                        !text.equals("Song", ignoreCase = true) -> artist = text
                }
                if (durationText == null && DURATION.matches(text)) durationText = text
            }
        }
        if (durationText == null) {
            val fixed = item.optJSONArray("fixedColumns")
            if (fixed != null) for (i in 0 until fixed.length()) {
                val text = fixed.optJSONObject(i)?.optJSONObject("musicResponsiveListItemFixedColumnRenderer")
                    ?.optJSONObject("text")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text").orEmpty()
                if (DURATION.matches(text)) { durationText = text; break }
            }
        }

        return InnertubeSongResult(
            videoId = videoId,
            title = title,
            artist = artist?.takeIf { it.isNotBlank() } ?: "Unknown artist",
            album = album,
            durationMillis = durationText?.let { parseDuration(it) },
            artistId = artistId,
            albumId = albumId,
        )
    }

    /** The smallest thumbnail of at least 200 px (or the largest there is) under [item]. */
    fun thumbnailOf(item: JSONObject): String? {
        val lists = mutableListOf<JSONObject>()
        collect(item, "musicThumbnailRenderer", lists)
        val thumbs = lists.firstOrNull()?.optJSONObject("thumbnail")?.optJSONArray("thumbnails") ?: return null
        val all = (0 until thumbs.length()).mapNotNull { thumbs.optJSONObject(it) }
        val pick = all.filter { it.optInt("width") >= 200 }.minByOrNull { it.optInt("width") } ?: all.lastOrNull()
        return pick?.optString("url")?.takeIf { it.startsWith("http") }
    }

    private val DURATION = Regex("""^\d{1,2}(:\d{2}){1,2}$""")

    fun parseDuration(text: String): Long? {
        val parts = text.trim().split(":").map { it.toIntOrNull() ?: return null }
        val seconds = when (parts.size) {
            2 -> parts[0] * 60 + parts[1]
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            else -> return null
        }
        return seconds * 1000L
    }

    /** "1.5B plays", "207M monthly audience" */
    private fun isCount(text: String) = Regex("""^[\d.,]+[KMB]?\s+\w+.*$""").matches(text)

    private fun isSeparator(text: String) = text == "•" || text == "&" || text == "," || text.all { !it.isLetterOrDigit() }

    private fun columnRuns(columns: JSONArray?, index: Int): JSONArray? =
        columns?.optJSONObject(index)?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.optJSONObject("text")?.optJSONArray("runs")

    private fun columnText(columns: JSONArray?, index: Int): String? = columnRuns(columns, index)?.let { runs ->
        (0 until runs.length()).joinToString("") { runs.optJSONObject(it)?.optString("text").orEmpty() }
    }

    private fun JSONArray?.twoRowItems(): List<JSONObject> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it)?.optJSONObject("musicTwoRowItemRenderer") }

    /** Every object stored under [key], depth first. */
    fun collect(node: Any?, key: String, out: MutableList<JSONObject>) {
        when (node) {
            is JSONObject -> for (name in node.keys()) {
                val value = node.opt(name)
                if (name == key && value is JSONObject) out += value else collect(value, key, out)
            }
            is JSONArray -> for (i in 0 until node.length()) collect(node.opt(i), key, out)
        }
    }
}
