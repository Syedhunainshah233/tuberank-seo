package com.syedtechdevelopers.tuberankseo

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
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
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotTests {
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
        rule.onAllNodesWithText(text, substring = substring)[0].performClick()
        rule.waitForIdle()
    }

    private fun waitFor(text: String) {
        rule.waitUntil(15_000) { rule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun walkThrough() {
        shot("00_welcome_login")
        skipLogin()
        shot("01_home_youtube")
        click("👑 Get PRO", substring = true); shot("01b_pro_dialog")
        click("Close")
        rule.onAllNodes(hasSetTextAction())[0].performTextInput("chicken biryani")
        click("⚡ Generate", substring = true)
        waitFor("SEO Score")
        shot("02_result_titles")
        click("Description"); shot("03_result_description")
        click("Tags"); shot("04_result_tags")
        click("Hashtags"); shot("05_result_hashtags")
        click("Ideas"); shot("06_result_ideas_locked")
        click("SEO Check"); shot("07_result_check")
        rule.runOnUiThread { rule.activity.vm.showResult = false }
        click("TikTok"); shot("08_home_tiktok")
        click("⚡ Generate", substring = true)
        waitFor("SEO Score")
        shot("09_tiktok_hooks")
        click("Caption"); shot("10_tiktok_caption")
        rule.runOnUiThread { rule.activity.vm.showResult = false }
        click("Instagram")
        click("⚡ Generate", substring = true); waitFor("SEO Score")
        click("Hashtags"); shot("11_instagram_hashtags")
        rule.runOnUiThread { rule.activity.vm.showResult = false }
        // Use up the free generations to show the limit dialog.
        rule.runOnUiThread { repeat(5) { rule.activity.vm.store.freeUsed = 5 } }
        click("⚡ Generate", substring = true); shot("11b_limit_dialog")
        click("Later")
        click("History"); shot("12_history")
        click("Settings"); shot("13_settings")
    }
}
