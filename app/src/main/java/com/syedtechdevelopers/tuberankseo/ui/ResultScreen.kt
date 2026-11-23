package com.syedtechdevelopers.tuberankseo.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.syedtechdevelopers.tuberankseo.AppViewModel
import com.syedtechdevelopers.tuberankseo.ads.Ads
import com.syedtechdevelopers.tuberankseo.ads.NativeAdCard
import com.syedtechdevelopers.tuberankseo.data.Platform
import com.syedtechdevelopers.tuberankseo.data.SeoResult
import kotlinx.coroutines.delay

fun Context.copy(label: String, text: String) {
    val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(this, "$label copied", Toast.LENGTH_SHORT).show()
}

fun Context.shareText(text: String) {
    Ads.suppressNextReturn()
    val i = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
    runCatching { startActivity(Intent.createChooser(i, "Share")) }
}


@Composable
fun ResultScreen(vm: AppViewModel) {
    BackHandler { vm.showResult = false }
    val ctx = LocalContext.current
    val r = vm.result
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(C.bg)
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { vm.showResult = false }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            androidx.compose.foundation.Image(
                androidx.compose.ui.res.painterResource((r?.platform ?: vm.platform).icon()), null,
                Modifier.padding(end = 10.dp).size(26.dp),
            )
            Text(
                vm.keyword.ifBlank { r?.keyword.orEmpty() },
                fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1, modifier = Modifier.weight(1f),
            )
            if (r != null) {
                IconButton(onClick = { ctx.shareText(allText(r)) }) { Icon(Icons.Filled.Share, "Share all") }
            }
        }
        if (r == null || vm.loading) {
            Loading()
        } else {
            Results(vm, r)
        }
    }
}

private fun tips(p: Platform) = when (p) {
    Platform.YouTube -> listOf(
        "Say your main keyword in the first 15 seconds of the video.",
        "Use the keyword in your thumbnail text and file name.",
        "Add chapters (timestamps) – they appear in search results.",
        "Reply to comments in the first hour to boost engagement.",
        "Put the video in a playlist with the keyword in its name.",
    )
    Platform.TikTok -> listOf(
        "Hook viewers in the first 2 seconds – use one of the hooks as on-screen text.",
        "Say your keyword out loud: TikTok reads speech for search.",
        "Use a trending sound that fits your niche.",
        "Keep it short and loopable to raise completion rate.",
        "Reply to comments with video replies.",
    )
    Platform.Instagram -> listOf(
        "Put the keyword in the first line of the caption and in your on-screen text.",
        "Use only your 5 most relevant hashtags.",
        "Add alt text with the keyword when posting a photo or carousel.",
        "Ask people to save and share – saves and shares boost reach.",
        "Post reels with trending audio and a strong cover image.",
    )
    Platform.Facebook -> listOf(
        "Keep the key message in the first 80 characters, before \"See more\".",
        "Upload videos natively instead of posting links.",
        "Ask a question to get comments.",
        "Share to relevant groups (follow their rules).",
        "Post Reels – they get extra reach on Facebook.",
    )
}

private fun allText(r: SeoResult) = buildString {
    append("${r.platform.headlineName.dropLast(1).uppercase()}:\n").append(r.titles.first())
    append("\n\n${r.platform.textName.uppercase()}:\n").append(r.description)
    append("\n\n${r.platform.keywordsName.uppercase()}:\n").append(r.tagsText)
    append("\n\nHASHTAGS:\n").append(r.hashtagsText)
}

@Composable
private fun Loading() {
    val steps = listOf(
        "Reading live search suggestions…",
        "Finding high-ranking keywords…",
        "Writing titles & description…",
        "Building tags & hashtags…",
    )
    var i by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1200); i = (i + 1) % steps.size
        }
    }
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = C.red, strokeWidth = 5.dp, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(20.dp))
        Text(steps[i], color = C.muted)
    }
}

@Composable
private fun Results(vm: AppViewModel, r: SeoResult) {
    val ctx = LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val p = r.platform
    val tabs = listOf(p.headlineName, p.textName, p.keywordsName, "Hashtags", "Ideas", "SEO Check")
    var askMoreTitles by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        ScoreCard(r)
        if (!r.pro) Box(Modifier.padding(horizontal = 16.dp)) { PlanCard(vm) }
        ScrollableTabRow(
            selectedTabIndex = tab,
            edgePadding = 12.dp,
            containerColor = C.bg,
            contentColor = C.text,
            divider = {},
            indicator = { pos ->
                TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(pos[tab]), color = C.red)
            },
        ) {
            tabs.forEachIndexed { i, t ->
                Tab(selected = tab == i, onClick = { tab = i }, text = {
                    Text(t, fontWeight = if (tab == i) FontWeight.Bold else FontWeight.Normal, color = if (tab == i) C.text else C.muted)
                })
            }
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (tab) {
                0 -> {
                    r.titles.forEachIndexed { i, t ->
                        CopyRow(t, "${t.length}/${p.headlineMax}", if (i == 0) "Best" else null) { ctx.copy(p.headlineName.dropLast(1), t) }
                        if (i == 3) NativeAdCard()
                    }
                    if (!r.pro) ProUpsell(vm, "9 more ${p.headlineName.lowercase()}")
                    OutlineAction("↻  More ${p.headlineName.lowercase()}", Icons.Filled.Refresh) { askMoreTitles = true }
                }

                1 -> {
                    Card {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${r.description.length}/${p.textMax} characters", color = C.muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            SmallCopy { ctx.copy(p.textName, r.description) }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(r.description, fontSize = 14.sp, lineHeight = 20.sp)
                    }
                    if (!r.pro) ProUpsell(vm, "Full ${p.textName.lowercase()} with keywords, chapters & call to action")
                    NativeAdCard()
                }

                2 -> {
                    Card {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (p == Platform.YouTube) "${r.tags.size} tags • ${r.tagsChars}/500 characters" else "${r.tags.size} search keywords",
                                color = C.muted, fontSize = 12.sp, modifier = Modifier.weight(1f),
                            )
                            SmallCopy { ctx.copy(p.keywordsName, r.tagsText) }
                        }
                        Spacer(Modifier.height(10.dp))
                        Chips(r.tags) { ctx.copy("Tag", it) }
                    }
                    if (!r.pro) ProUpsell(vm, if (p == Platform.YouTube) "Full tag set (up to 500 characters)" else "20 search keywords")
                    Hint(
                        if (p == Platform.YouTube) "Paste all tags in the Tags box of your video. Remove any other channel or brand name that is not in your video."
                        else "${p.label} search reads your caption, on-screen text and what you say. Use these keywords in all three.",
                    )
                    NativeAdCard()
                }

                3 -> {
                    Card {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${r.hashtags.size} hashtags", color = C.muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            SmallCopy { ctx.copy("Hashtags", r.hashtagsText) }
                        }
                        Spacer(Modifier.height(10.dp))
                        Chips(r.hashtags) { ctx.copy("Hashtag", it) }
                    }
                    if (!r.pro && p.maxHashtags > r.hashtags.size) ProUpsell(vm, "Up to ${p.maxHashtags} best hashtags")
                    Hint(
                        when (p) {
                            Platform.YouTube -> "The first 3 hashtags appear above your video title. Never use more than 15."
                            Platform.Instagram -> "Instagram allows only 5 hashtags per post or reel."
                            else -> "3–5 focused hashtags work better than many generic ones."
                        },
                    )
                    NativeAdCard()
                }

                4 -> Ideas(vm, r)

                5 -> {
                    Card {
                        Text("SEO checklist – ${r.optimization}% done", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        r.checklist().forEach { c ->
                            Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
                                Text(if (c.ok) "✅" else "❌")
                                Spacer(Modifier.width(10.dp))
                                Text(c.text, fontSize = 14.sp, color = if (c.ok) C.green else C.red)
                            }
                        }
                    }
                    Card {
                        Text("Pro tips to rank higher", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        tips(p).forEach { Text("• $it", color = C.muted, fontSize = 14.sp, modifier = Modifier.padding(vertical = 3.dp)) }
                    }
                    NativeAdCard()
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }

    if (askMoreTitles) {
        RewardIntro(
            title = "More ${r.platform.headlineName.lowercase()}",
            text = "Watch a short ad to get a new set of ${r.platform.headlineName.lowercase()}.",
            onSkip = { askMoreTitles = false },
            onWatch = {
                askMoreTitles = false
                Ads.showRewardedInterstitial(ctx.findActivity()) { vm.moreTitles() }
            },
        )
    }
}

@Composable
private fun Ideas(vm: AppViewModel, r: SeoResult) {
    val ctx = LocalContext.current
    if (!r.pro) {
        r.ideas.take(3).forEach { CopyRow(it, null, null) { ctx.copy("Keyword", it) } }
        ProUpsell(vm, "Video ideas & long-tail keywords")
        NativeAdCard()
    } else {
        r.ideas.forEachIndexed { i, s ->
            CopyRow(s, null, null) { ctx.copy("Keyword", s) }
            if (i == 5) NativeAdCard()
        }
    }
}

/** Green with border for the best values, red for the rest. */
@Composable
fun ScorePill(text: String, good: Boolean) {
    val c = if (good) C.green else C.red
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(c.copy(alpha = 0.16f))
            .border(1.dp, c, RoundedCornerShape(50))
            .padding(horizontal = 9.dp, vertical = 3.dp),
    ) { Text(text, color = c, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScoreCard(r: SeoResult) {
    val score = r.optimization
    val anim by animateFloatAsState(score / 100f, tween(900), label = "score")
    val good = score >= 80
    val color = if (good) C.green else C.red
    Card(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(78.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f))
                    .border(2.dp, color.copy(alpha = 0.7f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.fillMaxSize().padding(6.dp)) {
                    val sw = 7.dp.toPx()
                    val s = Size(size.width - sw, size.height - sw)
                    val o = Offset(sw / 2, sw / 2)
                    drawArc(C.card2, -90f, 360f, false, o, s, style = Stroke(sw))
                    drawArc(color, -90f, 360f * anim, false, o, s, style = Stroke(sw, cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$score", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = color, lineHeight = 22.sp)
                    Text("/100", fontSize = 10.sp, color = C.muted, lineHeight = 10.sp)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("SEO Score", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ScorePill(if (r.pro) "👑 PRO" else "Basic", r.pro)
                    ScorePill("Search: ${r.volume}", r.volume == "High")
                    ScorePill("Competition: ${r.competition}", r.competition == "Low")
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    if (!r.online) "Offline – connect to internet for live data"
                    else if (r.pro) "Full SEO pack • based on live search data" else "Free = Basic SEO. Get PRO for 100/100",
                    color = C.muted, fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun CopyRow(text: String, meta: String?, badge: String?, onCopy: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(C.card)
            .clickable(onClick = onCopy)
            .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (meta != null || badge != null) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    badge?.let { ScorePill(it, true) }
                    meta?.let { Text(it, color = C.muted, fontSize = 12.sp) }
                }
            }
        }
        SmallCopy(onCopy)
    }
}

@Composable
private fun SmallCopy(onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(C.red.copy(alpha = 0.16f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) { Text("Copy", color = C.red, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Chips(items: List<String>, onTap: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach {
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(C.card2)
                    .clickable { onTap(it) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) { Text(it, fontSize = 13.sp) }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text("💡 $text", color = C.muted, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 4.dp))
}

@Composable
private fun OutlineAction(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(C.card2)
            .clickable(onClick = onClick)
            .padding(14.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, fontWeight = FontWeight.Bold, color = C.text) }
}

/** Intro screen required by AdMob before a rewarded interstitial, with a way to skip. */
@Composable
fun RewardIntro(title: String, text: String, onSkip: () -> Unit, onWatch: () -> Unit) {
    var left by remember { mutableIntStateOf(5) }
    LaunchedEffect(Unit) {
        while (left > 0) {
            delay(1000); left--
        }
        onWatch()
    }
    AlertDialog(
        onDismissRequest = onSkip,
        containerColor = C.card,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text("$text\n\nAd starts in $left s…", color = C.muted) },
        confirmButton = { TextButton(onClick = onWatch) { Text("Watch now", color = C.red, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onSkip) { Text("No thanks", color = C.muted) } },
    )
}

