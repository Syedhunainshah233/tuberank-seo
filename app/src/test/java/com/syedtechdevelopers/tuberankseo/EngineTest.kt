package com.syedtechdevelopers.tuberankseo

import com.syedtechdevelopers.tuberankseo.data.Platform
import com.syedtechdevelopers.tuberankseo.data.SeoEngine
import com.syedtechdevelopers.tuberankseo.data.Suggest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/** Checks every platform's limits, and writes a live sample to build/screens/live-sample.txt. */
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34])
class EngineTest {

    private fun fake(kw: String): Map<String, Double> {
        val list = listOf(
            "$kw", "$kw recipe", "$kw easy", "$kw at home", "$kw in urdu", "$kw tiktok", "$kw for beginners",
            "$kw step by step", "best $kw", "$kw masala", "$kw by famous chef", "$kw $kw", "$kw k", "$kw restaurant style", "how to make $kw",
        )
        return list.mapIndexed { i, s -> s to (100.0 - i * 5) }.toMap()
    }

    @Test
    fun limitsPerPlatform() {
        for (p in Platform.entries) {
            val r = SeoEngine.build("chicken biryani", fake("chicken biryani"), true, "ur", "PK", p)
            assertTrue("$p headlines", r.titles.isNotEmpty() && r.titles.all { it.length <= p.headlineMax })
            assertTrue("$p hashtags ${r.hashtags}", r.hashtags.size in minOf(5, p.maxHashtags)..p.maxHashtags)
            assertTrue("$p text", r.description.length <= p.textMax)
            assertTrue("$p no platform words in hashtags", r.hashtags.none { it.contains("tiktok", true) })
            if (p == Platform.YouTube) assertTrue("tags ${r.tagsChars}", r.tagsChars <= 500)
            val all = (r.titles + r.tags + r.ideas).joinToString("|").lowercase()
            assertTrue("$p creator names / junk filtered: $all", "famous chef" !in all && "biryani k|" !in all)
        }
    }

    @Test
    fun basicIsAboutHalfProIsFull() {
        for (p in Platform.entries) {
            val w = fake("chicken biryani") + (1..30).associate { "chicken biryani idea $it" to 1.0 }
            val pro = SeoEngine.build("chicken biryani", w, true, "en", "PK", p, pro = true)
            val basic = SeoEngine.basic(SeoEngine.build("chicken biryani", w, true, "en", "PK", p, pro = false))
            assertTrue("$p pro ${pro.optimization} ${pro.checklist()}", pro.optimization == 100)
            assertTrue("$p basic ${basic.optimization} ${basic.checklist()}", basic.optimization in 50..60)
            assertTrue(basic.titles.size == 3 && basic.hashtags.size == 5)
            if (p == Platform.YouTube) assertTrue("long description ${pro.description.length}", pro.description.length >= 1800)
            if (p != Platform.Instagram) assertTrue("$p pro hashtags ${pro.hashtags.size}", pro.hashtags.size >= 10)
        }
    }

    @Test
    fun moreTitlesAreAlwaysNew() {
        for (p in Platform.entries) {
            val w = fake("chicken biryani") + (1..30).associate { "chicken biryani idea $it" to 1.0 }
            var r = SeoEngine.build("chicken biryani", w, true, "en", "PK", p, pro = false)
            repeat(3) { round ->
                val before = r.titles.map { it.lowercase() }.toSet()
                val fresh = SeoEngine.moreHeadlines(r, round + 1)
                assertTrue("$p round $round gave ${fresh.size}", fresh.size >= 3)
                assertTrue("$p repeated titles $fresh", fresh.none { it.lowercase() in before })
                assertTrue("$p length", fresh.all { it.length <= p.headlineMax })
                r = r.copy(titles = (fresh + r.titles).distinct())
                // Free users see the first 3, which must now be the new ones.
                assertTrue(SeoEngine.basic(r).titles == fresh.take(3))
            }
        }
    }

    @Test
    fun offlineStillWorks() {
        val r = SeoEngine.build("photo editing", emptyMap(), false, "en", "GLOBAL", Platform.YouTube)
        assertTrue(r.titles.isNotEmpty() && r.tags.isNotEmpty() && r.hashtags.isNotEmpty())
    }

    @Test
    fun liveSample() = runBlocking {
        val out = StringBuilder()
        for (kw in listOf("chicken biryani", "mobile photography tips")) for (p in Platform.entries) {
            val (w, online) = runCatching { Suggest.collect(kw, "en", "PK", p) }.getOrDefault(emptyMap<String, Double>() to false)
            val r = SeoEngine.build(kw, w, online, "en", "PK", p)
            out.append("===== $kw | ${p.label} | online=$online suggestions=${w.size} score=${r.score} ${r.volume}/${r.competition}\n")
            out.append("HEADLINES:\n").append(r.titles.joinToString("\n") { "  - $it" }).append("\n")
            out.append("${p.keywordsName.uppercase()} (${r.tagsChars}): ${r.tagsText}\n")
            out.append("HASHTAGS: ${r.hashtagsText}\n")
            out.append("${p.textName.uppercase()}:\n${r.description}\n")
            r.checklist().forEach { out.append("  [${if (it.ok) "x" else " "}] ${it.text}\n") }
            out.append("\n")
        }
        File("build/screens").mkdirs()
        File("build/screens/live-sample.txt").writeText(out.toString())
    }
}
