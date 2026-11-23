package com.syedtechdevelopers.tuberankseo.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Settings and history, kept only on this phone. */
class Store(context: Context) {
    private val sp = context.getSharedPreferences("tuberank", Context.MODE_PRIVATE)

    var lang: String
        get() = sp.getString("lang", "en") ?: "en"
        set(v) = sp.edit().putString("lang", v).apply()

    var country: String
        get() = sp.getString("country", "GLOBAL") ?: "GLOBAL"
        set(v) = sp.edit().putString("country", v).apply()

    var platform: Platform
        get() = Platform.of(sp.getString("platform", "YouTube") ?: "YouTube")
        set(v) = sp.edit().putString("platform", v.name).apply()

    // ---- Free limit (resets every day) and PRO time ----
    var freeDay: String
        get() = sp.getString("freeDay", "") ?: ""
        set(v) = sp.edit().putString("freeDay", v).apply()
    var freeUsed: Int
        get() = sp.getInt("freeUsed", 0)
        set(v) = sp.edit().putInt("freeUsed", v).apply()
    var proAds: Int
        get() = sp.getInt("proAds", 0)
        set(v) = sp.edit().putInt("proAds", v).apply()
    var proUntil: Long
        get() = sp.getLong("proUntil", 0L)
        set(v) = sp.edit().putLong("proUntil", v).apply()

    // ---- Google account (kept only on this phone) ----
    var userName: String
        get() = sp.getString("userName", "") ?: ""
        set(v) = sp.edit().putString("userName", v).apply()
    var userEmail: String
        get() = sp.getString("userEmail", "") ?: ""
        set(v) = sp.edit().putString("userEmail", v).apply()
    var userPhoto: String
        get() = sp.getString("userPhoto", "") ?: ""
        set(v) = sp.edit().putString("userPhoto", v).apply()
    var loginSkipped: Boolean
        get() = sp.getBoolean("loginSkipped", false)
        set(v) = sp.edit().putBoolean("loginSkipped", v).apply()

    var generations: Int
        get() = sp.getInt("gens", 0)
        set(v) = sp.edit().putInt("gens", v).apply()

    fun loadHistory(): List<SeoResult> = runCatching {
        val arr = JSONArray(sp.getString("history", "[]"))
        (0 until arr.length()).mapNotNull { runCatching { fromJson(arr.getJSONObject(it)) }.getOrNull() }
    }.getOrDefault(emptyList())

    fun saveHistory(list: List<SeoResult>) {
        val arr = JSONArray()
        list.take(50).forEach { arr.put(toJson(it)) }
        sp.edit().putString("history", arr.toString()).apply()
    }

    private fun JSONArray.strings() = (0 until length()).map { getString(it) }
    private fun List<String>.json() = JSONArray().also { a -> forEach { a.put(it) } }

    private fun toJson(r: SeoResult) = JSONObject().apply {
        put("k", r.keyword); put("l", r.lang); put("c", r.country)
        put("t", r.titles.json()); put("d", r.description); put("g", r.tags.json())
        put("h", r.hashtags.json()); put("i", r.ideas.json()); put("s", r.score)
        put("v", r.volume); put("comp", r.competition); put("o", r.online); put("at", r.createdAt); put("p", r.platform.name); put("pro", r.pro)
    }

    private fun fromJson(o: JSONObject) = SeoResult(
        keyword = o.getString("k"), lang = o.optString("l", "en"), country = o.optString("c", "GLOBAL"),
        titles = o.getJSONArray("t").strings(), description = o.getString("d"),
        tags = o.getJSONArray("g").strings(), hashtags = o.getJSONArray("h").strings(),
        ideas = o.optJSONArray("i")?.strings() ?: emptyList(), score = o.getInt("s"),
        volume = o.getString("v"), competition = o.getString("comp"), online = o.getBoolean("o"),
        createdAt = o.getLong("at"), platform = Platform.of(o.optString("p", "YouTube")), pro = o.optBoolean("pro", true),
    )
}
