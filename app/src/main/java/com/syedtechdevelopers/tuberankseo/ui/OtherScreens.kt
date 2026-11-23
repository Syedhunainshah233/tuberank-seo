package com.syedtechdevelopers.tuberankseo.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.syedtechdevelopers.tuberankseo.AppViewModel
import com.syedtechdevelopers.tuberankseo.BuildConfig
import com.syedtechdevelopers.tuberankseo.ads.Ads
import com.syedtechdevelopers.tuberankseo.ads.NativeAdCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
private fun Title(text: String) {
    Text(
        text, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold,
        modifier = Modifier
            .statusBarsPadding()
            .padding(start = 20.dp, top = 18.dp, bottom = 8.dp),
    )
}

@Composable
fun HistoryScreen(vm: AppViewModel) {
    val fmt = remember { SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()) }
    Column(Modifier.fillMaxSize()) {
        Title("History")
        if (vm.history.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🕘", fontSize = 44.sp)
                Spacer(Modifier.height(10.dp))
                Text("No history yet", fontWeight = FontWeight.Bold)
                Text("Your generated results will be saved here.", color = C.muted, fontSize = 14.sp)
            }
        } else LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(vm.history, key = { _, r -> "${r.platform}${r.createdAt}" }) { i, r ->
                Column {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(C.card)
                            .clickable { vm.open(r) }
                            .padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.foundation.Image(
                            androidx.compose.ui.res.painterResource(r.platform.icon()), r.platform.label,
                            Modifier.padding(end = 12.dp).size(30.dp),
                        )
                        Column(Modifier.weight(1f)) {
                            Text(r.keyword, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text(
                                "Score ${if (r.online) r.score else "–"} • ${r.tags.size} tags • ${fmt.format(Date(r.createdAt))}",
                                color = C.muted, fontSize = 12.sp,
                            )
                        }
                        IconButton(onClick = { vm.deleteHistory(r) }) { Icon(Icons.Filled.Delete, "Delete", tint = C.muted) }
                    }
                    if (i % 5 == 2) {
                        Spacer(Modifier.height(10.dp))
                        NativeAdCard()
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val ctx = LocalContext.current
    val activity = ctx.findActivity()
    var confirmClear by remember { mutableStateOf(false) }
    fun open(url: String) {
        Ads.suppressNextReturn()
        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }
    val site = "https://syedhunainshah233.github.io/tuberank-seo"
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Title("Settings")
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AccountCard(vm)
            Card {
                Text("Defaults", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Picker("Language", LANGUAGES, vm.lang, Modifier.weight(1f)) { vm.setLanguage(it) }
                    Picker("Region", REGIONS, vm.country, Modifier.weight(1f)) { vm.setRegion(it) }
                }
            }
            Card {
                Item("⭐  Rate this app") { open("https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}") }
                Item("📤  Share with friends") {
                    ctx.shareText("Get more views with TubeRank SEO – free titles, tags & hashtags generator: https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}")
                }
                Item("✉️  Contact & support") { open("mailto:syedhunainshah120@gmail.com?subject=TubeRank%20SEO") }
            }
            Card {
                Item("🔒  Privacy policy") { open("$site/privacy.html") }
                Item("📄  Terms of service") { open("$site/terms.html") }
                if (activity != null && Ads.privacyOptionsRequired(activity)) {
                    Item("🛡️  Ad privacy choices") { Ads.showPrivacyOptions(activity) }
                }
                Item("🗑️  Clear history") { confirmClear = true }
            }
            Text(
                "TubeRank SEO ${BuildConfig.VERSION_NAME} • SyedTechdevelopers\nNot affiliated with YouTube or Google.",
                color = C.muted, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            containerColor = C.card,
            title = { Text("Clear history?") },
            text = { Text("All saved results on this phone will be deleted.", color = C.muted) },
            confirmButton = {
                TextButton(onClick = { vm.clearHistory(); confirmClear = false }) { Text("Delete", color = C.red) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel", color = C.muted) } },
        )
    }
}

@Composable
private fun Item(text: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, modifier = Modifier.weight(1f), fontSize = 15.sp)
        Icon(Icons.Filled.KeyboardArrowRight, null, tint = C.muted)
    }
}
