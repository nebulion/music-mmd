package com.calmapps.calmmusic

import com.calmapps.calmmusic.ui.SongUiModel

/**
 * What album, artist and playlist pages show, kept for the life of the process.
 *
 * One paint per screen change (E Ink): a page is opened only once its content is here, so the top
 * bar and the rows arrive in the same frame. Coming back to a page (Back, or the activity being
 * recreated under "Don't keep activities") draws it straight from here, then refreshes quietly.
 */
class PageCache(private val capacity: Int = 40) {

    private val map = object : LinkedHashMap<String, Any>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Any>?) = size > capacity
    }

    @Synchronized
    private fun get(key: String): Any? = map[key]

    @Synchronized
    private fun put(key: String, value: Any) {
        map[key] = value
    }

    fun album(sourceType: String, id: String): List<SongUiModel>? =
        @Suppress("UNCHECKED_CAST") (get("album:$sourceType:$id") as? List<SongUiModel>)

    fun putAlbum(sourceType: String, id: String, songs: List<SongUiModel>) = put("album:$sourceType:$id", songs)

    fun artist(id: String): MonoMusicViewModel.ArtistContent? = get("artist:$id") as? MonoMusicViewModel.ArtistContent

    fun putArtist(id: String, content: MonoMusicViewModel.ArtistContent) = put("artist:$id", content)

    fun playlist(id: String): List<SongUiModel>? =
        @Suppress("UNCHECKED_CAST") (get("playlist:$id") as? List<SongUiModel>)

    fun putPlaylist(id: String, songs: List<SongUiModel>) = put("playlist:$id", songs)
}
