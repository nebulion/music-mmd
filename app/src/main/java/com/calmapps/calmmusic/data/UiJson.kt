package com.calmapps.calmmusic.data

import com.calmapps.calmmusic.ui.AlbumUiModel
import com.calmapps.calmmusic.ui.SongUiModel
import org.json.JSONArray
import org.json.JSONObject

/** JSON for the UI models that have to outlive the process (queue, last search, open album). */

fun SongUiModel.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("title", title)
    .put("artist", artist)
    .putOpt("durationText", durationText)
    .putOpt("durationMillis", durationMillis)
    .putOpt("discNumber", discNumber)
    .putOpt("trackNumber", trackNumber)
    .put("sourceType", sourceType)
    .putOpt("audioUri", audioUri)
    .putOpt("album", album)

fun songFromJson(o: JSONObject): SongUiModel = SongUiModel(
    id = o.getString("id"),
    title = o.optString("title"),
    artist = o.optString("artist"),
    durationText = o.optStringOrNull("durationText"),
    durationMillis = if (o.has("durationMillis")) o.getLong("durationMillis") else null,
    discNumber = if (o.has("discNumber")) o.getInt("discNumber") else null,
    trackNumber = if (o.has("trackNumber")) o.getInt("trackNumber") else null,
    sourceType = o.optString("sourceType", "YOUTUBE"),
    audioUri = o.optStringOrNull("audioUri"),
    album = o.optStringOrNull("album"),
)

fun AlbumUiModel.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("title", title)
    .putOpt("artist", artist)
    .put("sourceType", sourceType)
    .putOpt("releaseYear", releaseYear)

fun albumFromJson(o: JSONObject): AlbumUiModel = AlbumUiModel(
    id = o.getString("id"),
    title = o.optString("title"),
    artist = o.optStringOrNull("artist"),
    sourceType = o.optString("sourceType", "YOUTUBE"),
    releaseYear = if (o.has("releaseYear")) o.getInt("releaseYear") else null,
)

fun List<SongUiModel>.songsToJson(): JSONArray = JSONArray().also { a -> forEach { a.put(it.toJson()) } }
fun List<AlbumUiModel>.albumsToJson(): JSONArray = JSONArray().also { a -> forEach { a.put(it.toJson()) } }

fun JSONArray?.toSongs(): List<SongUiModel> =
    if (this == null) emptyList() else (0 until length()).mapNotNull { runCatching { songFromJson(getJSONObject(it)) }.getOrNull() }

fun JSONArray?.toAlbums(): List<AlbumUiModel> =
    if (this == null) emptyList() else (0 until length()).mapNotNull { runCatching { albumFromJson(getJSONObject(it)) }.getOrNull() }

private fun JSONObject.optStringOrNull(key: String): String? =
    if (has(key) && !isNull(key)) getString(key) else null
