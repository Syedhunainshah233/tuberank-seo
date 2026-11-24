package com.syedtechdevelopers.tuberankseo.data

import java.util.Calendar

data class Check(val ok: Boolean, val text: String)

/**
 * Per-platform rules (2026):
 *  YouTube   – title ≤ 100 (≤ 70 shows fully), tags box 500 chars, max 15 hashtags
 *              (with more than 15 YouTube ignores ALL hashtags on the video)
 *  TikTok    – caption 2,200 chars, only the first ~100 show in the feed; up to 20 hashtags
 *  Instagram – caption 2,200 chars, ~125 show before "more"; hard limit of 5 hashtags per post/reel
 *  Facebook  – long posts allowed, ~80 chars show before "See more"; up to 20 hashtags
 */
enum class Platform(
    val label: String,
    val emoji: String,
    val headlineName: String,
    val headlineMax: Int,
    val textName: String,
    val textMax: Int,
    val keywordsName: String,
    val maxHashtags: Int,
) {
    YouTube("YouTube", "▶️", "Titles", 70, "Description", 5000, "Tags", 15),
    TikTok("TikTok", "🎵", "Hooks", 100, "Caption", 2200, "Keywords", 20),
    Instagram("Instagram", "📸", "Hooks", 125, "Caption", 2200, "Keywords", 5),
    Facebook("Facebook", "📘", "Headlines", 80, "Post", 5000, "Keywords", 20),
    ;

    companion object {
        fun of(name: String) = entries.firstOrNull { it.name == name } ?: YouTube
    }
}

data class SeoResult(
    val keyword: String,
    val lang: String,
    val country: String,
    val titles: List<String>,
    val description: String,
    val tags: List<String>,
    val hashtags: List<String>,
    val ideas: List<String>,
    val score: Int,
    val volume: String,
    val competition: String,
    val online: Boolean,
    val platform: Platform = Platform.YouTube,
    val pro: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
) {
    /** Share of the SEO checklist this result completes (Basic ≈ 50%, PRO = 100% when online). */
    val optimization: Int get() = checklist().let { c -> c.count { it.ok } * 100 / c.size }

    val tagsText: String get() = tags.joinToString(", ")
    val hashtagsText: String get() = hashtags.joinToString(" ")
    val tagsChars: Int get() = tagsText.length

    fun checklist(): List<Check> {
        val t = titles.firstOrNull().orEmpty()
        val kw = keyword.lowercase()
        val kwFirst = t.lowercase().indexOf(kw).let { it in 0..25 }
        val p = platform
        val list = mutableListOf(
            Check(t.length <= p.headlineMax, "${p.headlineName.dropLast(1)} length ${t.length}/${p.headlineMax} characters"),
            Check(kwFirst, "Main keyword near the start"),
        )
        when (p) {
            Platform.YouTube -> {
                list += Check(description.length >= 800, "Description is ${description.length} characters (800+ recommended)")
                list += Check(description.lowercase().take(160).contains(kw), "Keyword in the first 2 lines of the description")
                list += Check(tagsChars in 200..500, "Tags use $tagsChars/500 characters")
                list += Check(hashtags.size in 5..15, "${hashtags.size} hashtags (5–15, first 3 show above the title)")
                list += Check(description.contains("00:00"), "Chapters / timestamps in the description")
                list += Check(description.contains("Related searches"), "Related search phrases in the description")
            }
            else -> {
                list += Check(description.length <= p.textMax, "${p.textName} is ${description.length}/${p.textMax} characters")
                list += Check(description.lowercase().take(p.headlineMax).contains(kw), "Keyword in the first line people see")
                list += Check(hashtags.size in minOf(5, p.maxHashtags)..p.maxHashtags, "${hashtags.size} hashtags (max ${p.maxHashtags} on ${p.label})")
                list += Check(tags.size >= 10, "${tags.size} search keywords to say in your video / on-screen text")
                list += Check(description.contains("✨"), "Keyword-rich points in the ${p.textName.lowercase()}")
                list += Check(description.contains("➕") || description.contains("👍"), "Call to action (follow, save, share)")
            }
        }
        list += Check(ideas.size >= 10, "${ideas.size} long-tail keyword ideas for more videos")
        list += Check(online, if (online) "Based on live search suggestions" else "Offline mode: connect to the internet for live data")
        return list
    }
}

object SeoEngine {

    val year: Int get() = Calendar.getInstance().get(Calendar.YEAR)

    private val platformWords = setOf("youtube", "tiktok", "instagram", "insta", "facebook", "fb", "reels", "reel", "shorts", "short", "ig")

    private val stopWords = setOf(
        "a", "an", "the", "and", "or", "of", "to", "in", "on", "for", "with", "is", "are", "how", "what",
        "my", "your", "me", "i", "it", "at", "by", "from", "vs", "be", "do", "kaise", "ka", "ki", "ke", "se",
        "hai", "kya", "aur", "mein", "this", "that", "best", "new", "video", "videos",
    ) + platformWords

    fun titleCase(s: String): String = s.split(" ").filter { it.isNotBlank() }.joinToString(" ") { w ->
        if (w.length <= 3 && w.lowercase() in setOf("a", "an", "the", "of", "to", "in", "on", "for", "and", "or", "vs")) {
            w.lowercase()
        } else if (w.any { it.isUpperCase() } || w.any { it.isDigit() }) {
            w
        } else {
            w.replaceFirstChar { it.uppercase() }
        }
    }.replaceFirstChar { it.uppercase() }

    private fun camel(s: String): String =
        s.split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotBlank() }
            .joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }

    /** Words that people add to the keyword when searching, best first. */
    fun modifiers(keyword: String, ranked: List<String>): List<String> {
        val kwWords = keyword.lowercase().split(" ").toSet()
        val count = LinkedHashMap<String, Int>()
        ranked.forEachIndexed { i, s ->
            s.split(" ").filter { it.length > 2 && it !in kwWords && it !in stopWords && !it.all(Char::isDigit) }
                .forEach { count[it] = (count[it] ?: 0) + (ranked.size - i) }
        }
        return count.entries.sortedByDescending { it.value }.map { it.key }
    }

    /** Removes platform names from a phrase ("biryani recipe tiktok" -> "biryani recipe"). */
    private fun clean(s: String): String =
        s.split(" ").filter { it !in platformWords }.joinToString(" ").trim()

    /**
     * Drops phrases that would be bad metadata: other creators' names ("... by xyz"),
     * the keyword repeated twice, or a dangling single letter ("biryani k").
     */
    private fun usable(s: String, kw: String): Boolean {
        val words = s.split(" ")
        if (" by " in " $s " || s.startsWith("by ")) return false
        if (words.last().length == 1 && !words.last()[0].isDigit()) return false
        if (kw.length > 3 && s.indexOf(kw) != s.lastIndexOf(kw)) return false
        return true
    }

    fun build(
        keywordRaw: String,
        weights: Map<String, Double>,
        online: Boolean,
        lang: String,
        country: String,
        platform: Platform = Platform.YouTube,
        pro: Boolean = true,
    ): SeoResult {
        val keyword = keywordRaw.trim().replace(Regex("\\s+"), " ")
        val kw = keyword.lowercase()
        val merged = LinkedHashMap<String, Double>()
        weights.entries.sortedByDescending { it.value }.forEach { (k, v) ->
            val c = clean(k)
            if (c.length >= 2 && c != kw && usable(c, kw)) merged[c] = (merged[c] ?: 0.0) + v
        }
        var ranked = merged.entries.sortedByDescending { it.value }.map { it.key }
        if (ranked.isEmpty()) ranked = fallback(kw, lang)
        val kwWords = kw.split(" ").filter { it !in stopWords }
        val relevant = ranked.filter { s -> kwWords.isEmpty() || kwWords.any { s.contains(it) } }
            .ifEmpty { ranked }
        val mods = modifiers(kw, relevant)

        val titles = headlines(platform, keyword, relevant, mods, lang, seed = 0)
        val tags = if (platform == Platform.YouTube) tags(kw, relevant, mods) else keywords(kw, relevant, mods)
        val hashtags = hashtags(platform, kw, relevant, mods)
        val description = when (platform) {
            Platform.YouTube -> description(keyword, titles.first(), relevant, hashtags, lang)
            else -> caption(platform, keyword, titles.first(), relevant, hashtags, lang)
        }
        val ideas = relevant.take(40).map { titleCase(it) }

        // Estimated score: search demand + how specific (long-tail) the keyword is.
        val seedHits = if (weights.containsKey(kw)) 1 else 0
        val demand = (weights.size / 2.5).coerceAtMost(100.0)
        val words = kw.split(" ").size
        val longTail = when {
            words >= 4 -> 90.0
            words == 3 -> 78.0
            words == 2 -> 60.0
            else -> 38.0
        }
        val score = if (!online) 0 else (demand * 0.5 + longTail * 0.4 + seedHits * 10).toInt().coerceIn(15, 98)
        val volume = when {
            !online -> "Unknown"
            weights.size >= 180 -> "High"
            weights.size >= 80 -> "Medium"
            else -> "Low"
        }
        val competition = when {
            words <= 1 -> "High"
            words == 2 -> if (volume == "High") "High" else "Medium"
            words == 3 -> "Medium"
            else -> "Low"
        }
        val full = SeoResult(
            keyword = keyword, lang = lang, country = country, titles = titles, description = description,
            tags = tags, hashtags = hashtags, ideas = ideas, score = score, volume = volume,
            competition = competition, online = online, platform = platform,
        )
        return full.copy(pro = pro)
    }

    /** Free (Basic) view of a full result: fewer titles, short text, fewer tags/hashtags, 3 ideas. */
    fun basic(r: SeoResult): SeoResult {
        val tags = mutableListOf<String>()
        var chars = 0
        for (t in r.tags) {
            val add = t.length + if (tags.isEmpty()) 0 else 2
            if (chars + add > 150 || (r.platform != Platform.YouTube && tags.size >= 5)) break
            tags += t; chars += add
        }
        val hashtags = r.hashtags.take(5)
        val first = r.titles.first()
        val text = if (r.platform == Platform.YouTube) {
            "$first\n\nIn this video you will learn ${r.keyword} step by step. " +
                "Watch till the end for the best tips about ${r.keyword}.\n\n" + hashtags.joinToString(" ")
        } else {
            "$first\n\nEverything about ${r.keyword} in one place.\n\n" + hashtags.joinToString(" ")
        }
        return r.copy(
            titles = r.titles.take(3), description = text, tags = tags, hashtags = hashtags,
            ideas = r.ideas.take(3), pro = false,
        )
    }

    private fun fallback(kw: String, lang: String): List<String> {
        val base = mutableListOf(
            "$kw tutorial", "$kw for beginners", "$kw tips", "$kw $year", "how to $kw", "$kw explained",
            "$kw guide", "best $kw", "$kw step by step", "$kw tricks", "$kw ideas", "$kw complete course",
        )
        if (lang == "ur" || lang == "hi") base += listOf("$kw in urdu", "$kw in hindi", "$kw kaise kare")
        return base
    }

    private fun urduish(lang: String) = lang == "ur" || lang == "hi"

    /**
     * "More titles / hooks": up to 12 headlines that are NOT already in [r].titles.
     * Mixes the searched phrases and modifiers differently each [round] so every tap gives new ones.
     */
    fun moreHeadlines(r: SeoResult, round: Int): List<String> {
        val seen = r.titles.map { it.lowercase() }.toHashSet()
        val ideas = r.ideas.map { it.lowercase() }
        val mods = modifiers(r.keyword.lowercase(), ideas)
        val rnd = java.util.Random(r.createdAt + round * 7919L)
        val fresh = LinkedHashSet<String>()
        for (attempt in 0 until 10) {
            val rel = if (attempt == 0) ideas.drop(3) + ideas.take(3) else ideas.shuffled(rnd)
            val m = if (attempt == 0) mods.drop(1) + mods.take(1) else mods.shuffled(rnd)
            for (t in headlines(r.platform, r.keyword, rel, m, r.lang, round * 3 + attempt)) {
                if (t.lowercase() !in seen) fresh += t
            }
            if (fresh.size >= 12) break
        }
        return fresh.take(12).toList()
    }

    fun headlines(platform: Platform, keyword: String, relevant: List<String>, mods: List<String>, lang: String, seed: Int): List<String> =
        if (platform == Platform.YouTube) titles(keyword, relevant, mods, lang, seed)
        else hooks(platform, keyword, relevant, mods, lang, seed)

    fun titles(keyword: String, relevant: List<String>, mods: List<String>, lang: String, seed: Int): List<String> {
        val k = titleCase(keyword)
        val m1 = mods.getOrNull(0)?.let { titleCase(it) } ?: "Guide"
        val m2 = mods.getOrNull(1)?.let { titleCase(it) } ?: "Tips"
        val y = year
        val top = relevant.take(6).map { titleCase(it) }
        val templates = mutableListOf(
            "$k – Complete $m1 Guide ($y)",
            "$k for Beginners: Step by Step Tutorial",
            "$k – Easy $m2 Anyone Can Follow",
            "10 $k Tips You Must Know in $y",
            "$k Explained in 10 Minutes",
            "Stop Making These $k Mistakes!",
            "The Ultimate $k Guide ($y Update)",
            "I Tried $k for 30 Days – Here's What Happened",
            "$k: Secrets Nobody Tells You",
            "$k $m1 & $m2 – Full Tutorial",
            "Best $k Tricks for Fast Results ($y)",
            "$k Made Easy – Watch This First!",
            "$k in $y: What Really Works",
            "Beginner to Pro: $k Masterclass",
            "Why Your $k Isn't Working (And How to Fix It)",
            "$k Hacks Everyone Should Know",
            "$k: Everything You Need to Know",
            "My Honest $k Review ($y)",
            "$k – Do This Before You Start",
            "5 Easy $k Ideas for Beginners",
            "$k in Under 5 Minutes",
            "The Right Way to Do $k",
            "$k Tips the Pros Use",
            "$k – Simple $m1 That Works",
        )
        if (urduish(lang)) {
            templates.add(2, "$k Kaise Karein? Sab Se Asaan Tareeqa ($y)")
            templates.add(6, "$k Seekhein Sirf 10 Minute Mein | Urdu/Hindi")
        }
        val suffix = listOf("Full Guide", "Step by Step", "$y", "Tips & Tricks", "Explained", "Tutorial")
        // Real searched phrases make the strongest titles; keep the short, generic ones.
        top.filter { it.split(" ").size <= keyword.split(" ").size + 2 }
            .forEachIndexed { i, s -> templates.add(1 + i * 2, "$s | ${suffix[i % 6]}") }
        return rotate(templates, 20, 70, seed, k)
    }

    /** Short scroll-stopping first lines for TikTok, Instagram and Facebook. */
    fun hooks(platform: Platform, keyword: String, relevant: List<String>, mods: List<String>, lang: String, seed: Int): List<String> {
        val k = titleCase(keyword)
        val m1 = mods.getOrNull(0) ?: "tips"
        val y = year
        val templates = mutableListOf(
            "POV: you finally learned $keyword the right way 😮",
            "Nobody tells you this about $keyword 🤫",
            "$k in 30 seconds ⏱️ Save this!",
            "Stop scrolling if you love $keyword 👀",
            "3 $keyword $m1 you need to try in $y 🔥",
            "This $keyword hack changed everything ✨",
            "Wait for the end… $keyword like you've never seen 😍",
            "Rate this $keyword from 1–10 👇",
            "Things I wish I knew before $keyword 💡",
            "Easy $keyword anyone can do ✅",
            "Don't make this $keyword mistake ❌",
            "Save this for your next $keyword 📌",
            "The $keyword trick you'll wish you knew sooner 😱",
            "$k but make it easy ✨",
            "Try this $keyword idea today 👇",
            "Watch this before your next $keyword 🎯",
            "$k level: expert 🔥",
            "Don't skip this $keyword tip ⏭️",
        )
        if (platform == Platform.Facebook) {
            templates.add(0, "$k: the complete guide everyone is sharing")
            templates.add(3, "Tag a friend who needs to see this $keyword 👇")
        }
        if (platform == Platform.Instagram) templates.add(1, "Your sign to try $keyword today ✨")
        if (urduish(lang)) {
            templates.add(1, "$k ka sab se asaan tareeqa 😍")
            templates.add(4, "Yeh $keyword trick koi nahi batata 🤫")
        }
        relevant.take(4).forEachIndexed { i, s -> templates.add(2 + i * 3, "${titleCase(s)} 🔥 (save this)") }
        return rotate(templates, 15, platform.headlineMax, seed, k)
    }

    private fun rotate(templates: List<String>, min: Int, max: Int, seed: Int, fallback: String): List<String> {
        // "Tips Tips" -> "Tips" when the keyword already ends with the template word.
        val pool = templates.map { it.replace(Regex("\\s+"), " ").trim().replace(Regex("(?i)\\b(\\w+) \\1\\b"), "$1") }.filter { it.length in min..max }.distinct()
        if (pool.isEmpty()) return listOf(fallback.take(max))
        val start = (seed * 7) % pool.size
        return (pool.drop(start) + pool.take(start)).take(12)
    }

    /** YouTube tags box: fits in 500 characters. */
    fun tags(kw: String, relevant: List<String>, mods: List<String>): List<String> {
        val cands = mutableListOf(kw)
        cands += relevant.take(25)
        cands += mods.take(6).map { "$kw $it" }
        cands += listOf("$kw $year", "$kw tutorial", "how to $kw")
        cands += kw.split(" ").filter { it.length > 3 && it !in stopWords }
        cands += mods.take(6)
        val out = mutableListOf<String>()
        var chars = 0
        for (c in cands.map { it.trim() }.distinct()) {
            if (c.length !in 2..60 || c.contains(",") || out.any { it.equals(c, true) }) continue
            val add = c.length + if (out.isEmpty()) 0 else 2
            if (chars + add > 490) continue
            out += c
            chars += add
        }
        return out
    }

    /** TikTok/Instagram/Facebook search keywords: say them in the video, on-screen text and caption. */
    fun keywords(kw: String, relevant: List<String>, mods: List<String>): List<String> {
        val cands = mutableListOf(kw) + relevant.take(20) + mods.take(5).map { "$kw $it" }
        return cands.map { it.trim() }.filter { it.length in 2..50 }.distinctBy { it.lowercase() }.take(20)
    }

    fun hashtags(platform: Platform, kw: String, relevant: List<String>, mods: List<String>): List<String> {
        val cands = mutableListOf(camel(kw))
        cands += relevant.take(8).map { camel(it) }
        cands += kw.split(" ").filter { it.length > 3 && it !in stopWords }.map { camel(it) }
        cands += mods.take(10).map { camel(it) }
        cands += relevant.drop(8).take(20).map { camel(it) }
        cands += mods.take(6).map { camel("$kw $it") }
        cands += listOf(camel("$kw $year"), camel("$kw tips"), camel("$kw ideas"), "Trending", "Viral")
        when (platform) {
            Platform.YouTube -> if (kw.contains("short")) cands.add(1, "Shorts")
            Platform.TikTok -> cands.addAll(1, listOf("FYP", "ForYou"))
            Platform.Instagram -> cands.add(1, "Reels")
            Platform.Facebook -> {}
        }
        return cands.filter { it.length in 2..24 }.distinctBy { it.lowercase() }.take(platform.maxHashtags).map { "#$it" }
    }

    fun description(keyword: String, title: String, relevant: List<String>, hashtags: List<String>, lang: String): String {
        val k = titleCase(keyword)
        val s = relevant.take(20)
        val learn = s.take(8).joinToString("\n") { "✅ ${titleCase(it)}" }
        val related = s.take(15).joinToString(", ")
        val also = s.take(3).joinToString(", ").ifBlank { keyword }
        val more = s.drop(3).take(3).joinToString(", ").ifBlank { "$keyword tips" }
        val faq = s.filter { it.startsWith("how ") || it.startsWith("what ") || it.startsWith("why ") || it.startsWith("can ") }
            .take(3).ifEmpty { listOf("how to $keyword", "what is the best way to $keyword", "is $keyword easy for beginners") }
        val urdu = if (urduish(lang)) {
            "\n\nIs video mein hum $keyword ko asaan alfaaz mein step by step samjhenge. Agar aap $also dhoond rahe hain to yeh video aap ke liye hai. Video aakhir tak zaroor dekhein!"
        } else ""
        return buildString {
            append(title).append("\n\n")
            append("In this video you will learn $keyword step by step. Whether you are searching for $also, this video covers everything you need to know about $keyword – from the basics to pro tips that really work in $year.")
            append(" We also cover $more, so beginners and experienced viewers can both follow along easily.")
            append(urdu)
            append("\n\n👉 What you will learn in this video:\n").append(learn)
            append("\n\n💡 Why watch this video?\nMost $keyword videos skip the small details that make the biggest difference. Here you get a clear, simple and complete $keyword guide with real examples, common mistakes to avoid and tips you can use today.")
            append("\n\n⏱️ Timestamps:\n00:00 Intro\n00:30 What is $k?\n01:30 What you need\n02:30 Step by step\n05:00 Pro tips\n07:00 Common mistakes\n08:30 Final result & conclusion\n(Edit the timestamps to match your video)")
            append("\n\n❓ Frequently asked questions:\n")
            faq.forEach { q -> append("• ${titleCase(q).trimEnd('?')}? – Watch the full video, we answer it step by step.\n") }
            append("\n🔔 Subscribe and turn on notifications for more videos about $keyword.\n👍 Like  💬 Comment your questions  🔁 Share with a friend who needs this!")
            append("\n\n📌 Watch next: more $keyword videos on this channel.")
            append("\n\n🔎 Related searches:\n").append(related)
            append("\n\n").append(hashtags.joinToString(" "))
        }
    }

    /** Caption for TikTok / Instagram / Facebook: hook first, keywords in natural sentences, CTA, hashtags. */
    fun caption(platform: Platform, keyword: String, hook: String, relevant: List<String>, hashtags: List<String>, lang: String): String {
        val s = relevant.take(10)
        val also = s.take(3).joinToString(", ").ifBlank { keyword }
        val urdu = if (urduish(lang)) "\n\nPoori video dekhein aur apni raaye comment mein batayein! 👇" else ""
        val first = keyword.split(" ").first().uppercase()
        val cta = when (platform) {
            Platform.TikTok -> "💬 Comment \"$first\" if you want part 2!\n➕ Follow for more $keyword tips every day.\n🔁 Share this with someone who needs it."
            Platform.Instagram -> "📌 Save this post so you don't lose it.\n📤 Share it with a friend who loves $keyword.\n➕ Follow for more $keyword ideas."
            else -> "👍 Like & share if this helped you!\n💬 Tell us in the comments: what should we post next?\n➕ Follow the page for more $keyword content."
        }
        val points = s.take(if (platform == Platform.Facebook) 7 else 5).joinToString("\n") { "✨ ${titleCase(it)}" }
        return buildString {
            append(hook).append("\n\n")
            append("Everything you need to know about $keyword in one place – $also and more. ")
            append("Simple steps, real results and tips that actually work in $year.")
            append(urdu)
            append("\n\nIn this ${if (platform == Platform.Facebook) "post" else "video"}:\n").append(points)
            append("\n\n❓ Which tip will you try first? Let me know below 👇")
            append("\n\n").append(cta)
            append("\n\n").append(hashtags.joinToString(" "))
        }
    }
}
