package com.syedtechdevelopers.tuberankseo.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.syedtechdevelopers.tuberankseo.AppViewModel
import com.syedtechdevelopers.tuberankseo.Page
import com.syedtechdevelopers.tuberankseo.ads.Ads
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DIAMOND = Color(0xFF4FC3F7)

/** Settings card: balance, "Watch ad +1 💎", history and shop. */
@Composable
fun DiamondCard(vm: AppViewModel) {
    val ctx = LocalContext.current
    var loading by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(C.card)
            .border(1.dp, DIAMOND.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("💎", fontSize = 34.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("${vm.diamonds}", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = DIAMOND)
                Text("Your diamonds", color = C.muted, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(14.dp))
        // Earn diamonds: one fully watched rewarded ad = 1 diamond.
        Box(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(GOLD)
                .clickable(enabled = !loading) {
                    loading = true
                    Ads.showRewarded(
                        ctx.findActivity(),
                        onReward = {
                            vm.addDiamond()
                            Toast.makeText(ctx, "+1 💎 added! You now have ${vm.diamonds}", Toast.LENGTH_SHORT).show()
                        },
                        onFailed = {
                            loading = false
                            Toast.makeText(ctx, "No ad available right now. Please try again in a minute.", Toast.LENGTH_LONG).show()
                        },
                        onClosed = { loading = false },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            if (loading) {
                CircularProgressIndicator(color = C.bg, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            } else {
                Text("🎬 Earn Diamonds • Watch 1 ad = +1 💎", color = C.bg, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
        }
        Spacer(Modifier.height(6.dp))
        DiamondRow("📜  Diamond history", "${vm.diamondLog.size}") { vm.page = Page.DiamondHistory }
        DiamondRow("🛍️  Shop", "Coming soon") { vm.page = Page.Shop }
    }
}

@Composable
private fun DiamondRow(text: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, modifier = Modifier.weight(1f), fontSize = 15.sp)
        Text(value, color = C.muted, fontSize = 13.sp)
        Icon(Icons.Filled.KeyboardArrowRight, null, tint = C.muted)
    }
}

@Composable
private fun PageHeader(title: String, onBack: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
        Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp)
    }
}

private val timeFmt = SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault())

/** Every diamond the user earned, newest first, with date and time. */
@Composable
fun DiamondHistoryScreen(vm: AppViewModel) {
    BackHandler { vm.page = null }
    Column(Modifier.fillMaxSize().background(C.bg)) {
        PageHeader("Diamond history") { vm.page = null }
        Row(
            Modifier
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(C.card)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("💎", fontSize = 24.sp)
            Spacer(Modifier.width(10.dp))
            Text("Total: ${vm.diamonds} diamonds", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("${vm.diamondLog.size} entries", color = C.muted, fontSize = 12.sp)
        }
        if (vm.diamondLog.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("💎", fontSize = 44.sp)
                Spacer(Modifier.height(10.dp))
                Text("No diamonds yet", fontWeight = FontWeight.Bold)
                Text(
                    "Go to Settings and tap Earn Diamonds. Every ad you watch gives you 1 diamond.",
                    color = C.muted, fontSize = 14.sp, textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(vm.diamondLog, key = { it.time.toString() + it.reason }) { e ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(C.card)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(e.reason, fontWeight = FontWeight.SemiBold)
                            Text(timeFmt.format(Date(e.time)), color = C.muted, fontSize = 12.sp)
                        }
                        Text("+${e.amount} 💎", color = DIAMOND, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

/** Placeholder until the shop opens: diamonds are saved for later. */
@Composable
fun ShopScreen(vm: AppViewModel) {
    BackHandler { vm.page = null }
    Column(Modifier.fillMaxSize().background(C.bg)) {
        PageHeader("Shop") { vm.page = null }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(30.dp))
            Text("🛍️", fontSize = 64.sp)
            Spacer(Modifier.height(14.dp))
            Text("Shop is coming soon", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                "Keep collecting diamonds. Soon you'll be able to use them here.",
                color = C.muted, fontSize = 15.sp, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(DIAMOND.copy(alpha = 0.15f))
                    .border(1.dp, DIAMOND, RoundedCornerShape(50))
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("💎 ${vm.diamonds}", color = DIAMOND, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Text("  saved", color = C.muted, fontSize = 14.sp)
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "Earn more diamonds in Settings → Earn Diamonds.",
                color = C.muted, fontSize = 13.sp, textAlign = TextAlign.Center,
            )
        }
    }
}
