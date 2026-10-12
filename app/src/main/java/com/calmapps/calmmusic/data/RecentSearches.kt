package com.calmapps.calmmusic.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** A search the owner ran, and what it found ("12 songs · 3 albums"). */
data class RecentSearch(val query: String, val found: String)

/**
 * The last [MAX] searches, newest first (owner's pick 3A, 2026-10-12): shown on the search page
 * before typing, from the owner's own history (pattern D1). The same query again moves to the top.
 */
class RecentSearches(context: Context) {
    private val prefs = context.getSharedPreferences("recent_searches", Context.MODE_PRIVATE)

    fun all(): List<RecentSearch> = try {
        val a = JSONArray(prefs.getString(KEY, "[]"))
        (0 until a.length()).map { a.getJSONObject(it).let { o -> RecentSearch(o.getString("q"), o.optString("f")) } }
    } catch (_: Exception) {
        emptyList()
    }

    fun add(query: String, found: String): List<RecentSearch> {
        val list = (listOf(RecentSearch(query, found)) + all().filterNot { it.query.equals(query, ignoreCase = true) }).take(MAX)
        save(list)
        return list
    }

    fun clear(): List<RecentSearch> {
        save(emptyList())
        return emptyList()
    }

    private fun save(list: List<RecentSearch>) {
        val a = JSONArray()
        list.forEach { a.put(JSONObject().put("q", it.query).put("f", it.found)) }
        prefs.edit().putString(KEY, a.toString()).apply()
    }

    companion object {
        private const val KEY = "list"
        const val MAX = 6
    }
}
