package com.syedtechdevelopers.tuberankseo

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.syedtechdevelopers.tuberankseo.data.Suggest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Walks through the screens and saves PNGs to app/build/screens for review. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w320dp-h640dp-xhdpi")
class SmallScreenTests {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Before
    fun fakeNetwork() {
        Suggest.fetcher = { q, _, _, _ ->
            val base = q.trim()
            listOf(base, "$base recipe", "$base easy", "$base at home", "$base in urdu", "$base step by step", "best $base", "$base masala", "how to make $base", "$base spicy")
        }
    }

    @Before
    fun freshState() {
        rule.runOnUiThread {
            val vm = rule.activity.vm
            vm.store.freeUsed = 0; vm.store.freeDay = ""; vm.store.proUntil = 0L
            vm.refreshDay()
        }
    }

    @org.junit.After
    fun clearPrefs() {
        rule.activity.getSharedPreferences("tuberank", android.content.Context.MODE_PRIVATE).edit().clear().commit()
    }

    private fun skipLogin() {
        rule.waitForIdle()
        if (rule.onAllNodesWithText("Skip for now").fetchSemanticsNodes().isNotEmpty()) click("Skip for now")
    }

    private fun shot(name: String) {
        rule.waitForIdle()
        captureScreenRoboImage("build/screens/$name.png")
    }

    private fun click(text: String, substring: Boolean = false) {
        rule.waitForIdle()
        val node = rule.onAllNodesWithText(text, substring = substring)[0]
        runCatching { node.performScrollTo() }
        node.performClick()
        rule.waitForIdle()
    }

    private fun waitFor(text: String) {
        rule.waitUntil(15_000) { rule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun walkThrough320() {
        shot("s320_01_home")
        rule.onAllNodes(hasSetTextAction())[0].performTextInput("chicken biryani")
        click("⚡ Generate", substring = true)
        waitFor("SEO Check")
        shot("s320_02_titles")
        click("Description"); shot("s320_03_description")
        click("Hashtags"); shot("s320_04_hashtags")
        click("SEO Check"); shot("s320_05_check")
        rule.runOnUiThread { rule.activity.vm.showResult = false }
        click("FREE PRO", substring = true); shot("s320_06_pro")
        click("Close")
        click("Facebook"); shot("s320_07_home_facebook")
        click("Settings"); shot("s320_08_settings")
        // Diamonds: earn 3 (as if 3 ads were watched), then open history and shop.
        rule.runOnUiThread { repeat(3) { rule.activity.vm.addDiamond() } }
        rule.waitForIdle()
        shot("s320_09b_settings_diamonds")
        click("Diamond history", substring = true); waitFor("Total: 3 diamonds"); shot("s320_09c_diamond_history")
        rule.runOnUiThread { rule.activity.vm.page = null }
        click("Shop", substring = true); waitFor("Shop is coming soon"); shot("s320_09d_shop")
        rule.runOnUiThread { rule.activity.vm.page = null }
    }
}
