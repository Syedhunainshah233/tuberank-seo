package com.syedtechdevelopers.tuberankseo.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Real video-search suggestions (the same list people see while typing in the search box).
 * We ask for the keyword itself plus "keyword a".."keyword z" and common question
 * prefixes, then rank every phrase by how often and how high it appears.
 */
object Suggest {

    /** One query -> ordered suggestions. Replaceable in tests. */
    var fetcher: suspend (query: String, lang: String, country: String, video: Boolean) -> List<String> = ::fetchOne

    private fun fetchOne(query: String, lang: String, country: String, video: Boolean): List<String> {
        val q = URLEncoder.encode(query, "UTF-8")
        val gl = if (country == "GLOBAL") "" else "&gl=$country"
        // Video search suggestions for YouTube; general web search suggestions for the others.
        val ds = if (video) "&ds=yt" else ""
        val url = URL(
            "https://suggestqueries.google.com/complete/search?client=firefox$ds&ie=utf-8&oe=utf-8&hl=$lang$gl&q=$q",
        )
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 6000
            readTimeout = 6000
            setRequestProperty("User-Agent", "Mozilla/5.0 (Android) TubeRankSEO")
        }
        return try {
            if (conn.responseCode != 200) return emptyList()
            val body = conn.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
            val arr = JSONArray(body).getJSONArray(1)
            (0 until arr.length()).map { arr.getString(it).trim() }.filter { it.isNotEmpty() }
        } finally {
            conn.disconnect()
        }
    }

    fun queriesFor(keyword: String, lang: String, platform: Platform): List<String> {
        val k = keyword.trim()
        val list = mutableListOf(k, "$k ")
        ('a'..'z').forEach { list += "$k $it" }
        list += listOf("how to $k", "best $k", "$k for", "$k vs", "what is $k", "$k tips")
        if (lang == "ur" || lang == "hi") list += listOf("$k kaise", "$k in urdu", "$k in hindi")
        list += when (platform) {
            Platform.TikTok -> listOf("$k tiktok", "$k trend", "$k viral", "$k challenge")
            Platform.Instagram -> listOf("$k reels", "$k instagram", "$k aesthetic", "$k captions")
            Platform.Facebook -> listOf("$k facebook", "$k reels", "$k ideas")
            Platform.YouTube -> listOf("$k shorts", "$k tutorial")
        }
        return list.distinct()
    }

    /** Returns phrase -> weight (higher = more searched), and whether we reached the internet. */
    suspend fun collect(keyword: String, lang: String, country: String, platform: Platform = Platform.YouTube): Pair<Map<String, Double>, Boolean> =
        withContext(Dispatchers.IO) {
            val gate = Semaphore(8)
            val queries = queriesFor(keyword, lang, platform)
            val video = platform == Platform.YouTube
            val results = coroutineScope {
                queries.mapIndexed { qi, q ->
                    async {
                        gate.withPermit {
                            val r = runCatching { fetcher(q, lang, country, video) }.getOrNull()
                            Triple(qi, q, r)
                        }
                    }
                }.awaitAll()
            }
            val online = results.any { it.third != null }
            val weights = LinkedHashMap<String, Double>()
            for ((qi, _, list) in results) {
                if (list == null) continue
                // The plain keyword's own suggestions are the strongest signal.
                val boost = if (qi <= 1) 3.0 else 1.0
                list.forEachIndexed { rank, s ->
                    val key = s.lowercase().replace(Regex("\\s+"), " ").trim()
                    if (key.length < 2) return@forEachIndexed
                    weights[key] = (weights[key] ?: 0.0) + boost * (10 - rank.coerceAtMost(9))
                }
            }
            weights to online
        }
}
