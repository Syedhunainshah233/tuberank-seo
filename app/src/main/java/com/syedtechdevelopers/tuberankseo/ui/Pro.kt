package com.syedtechdevelopers.tuberankseo.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import com.syedtechdevelopers.tuberankseo.ads.Ads

val GOLD = androidx.compose.ui.graphics.Color(0xFFFFC542)

fun timeLeft(ms: Long): String {
    val m = (ms / 60_000).coerceAtLeast(0)
    return if (m >= 60) "${m / 60}h ${m % 60}m" else "${m}m"
}

/** Home screen status: free generations left, or PRO time left. */
@Composable
fun PlanCard(vm: AppViewModel) {
    if (vm.isPro) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(C.card)
                .border(1.dp, GOLD.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("👑", fontSize = 24.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("PRO active", fontWeight = FontWeight.Bold, color = GOLD)
                Text("Unlimited generations • Full SEO • ${timeLeft(vm.proUntil - vm.now)} left", color = C.muted, fontSize = 12.sp)
            }
        }
    } else {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(C.card)
                .clickable { vm.showPro = true }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Free: ${vm.freeLeft}/${AppViewModel.FREE_PER_DAY} generations left today",
                    fontWeight = FontWeight.Bold, fontSize = 14.sp,
                )
                Text("Basic SEO (about 50% optimized)", color = C.muted, fontSize = 12.sp)
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(GOLD)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) { Text("👑 Get PRO", color = C.bg, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp) }
        }
    }
}

/** Shown when the 5 free generations of the day are used up. */
@Composable
fun LimitDialog(vm: AppViewModel) {
    AlertDialog(
        onDismissRequest = { vm.showLimit = false },
        containerColor = C.card,
        title = { Text("Daily free limit reached", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "You have used your ${AppViewModel.FREE_PER_DAY} free generations for today.\n\n" +
                    "Watch ${AppViewModel.ADS_FOR_PRO} short ads to get PRO for ${AppViewModel.PRO_HOURS} hours, or come back tomorrow.",
                color = C.muted,
            )
        },
        confirmButton = {
            TextButton(onClick = { vm.showLimit = false; vm.showPro = true }) {
                Text("👑 Get PRO", color = GOLD, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = { vm.showLimit = false }) { Text("Later", color = C.muted) } },
    )
}

/** Explains PRO and lets the user watch rewarded ads one by one (progress is saved). */
@Composable
fun ProDialog(vm: AppViewModel) {
    val ctx = LocalContext.current
    var loading by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { if (!loading) vm.showPro = false },
        containerColor = C.card,
        title = { Text("👑 TubeRank PRO – ${AppViewModel.PRO_HOURS} hours free", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Watch ${AppViewModel.ADS_FOR_PRO} short ads to unlock PRO:", color = C.muted)
                listOf(
                    "Unlimited generations for ${AppViewModel.PRO_HOURS} hours",
                    "Full SEO pack – 100% of the SEO checklist",
                    "12 titles / hooks, full description or caption",
                    "Full tags (up to 500 characters) & all hashtags",
                    "Video ideas & long-tail keywords",
                ).forEach { Text("✅ $it", fontSize = 14.sp) }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    repeat(AppViewModel.ADS_FOR_PRO) { i ->
                        Box(
                            Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(if (i < vm.proAds) GOLD else C.card2),
                            contentAlignment = Alignment.Center,
                        ) { Text(if (i < vm.proAds) "✓" else "${i + 1}", fontSize = 12.sp, color = if (i < vm.proAds) C.bg else C.muted) }
                    }
                }
                Text(
                    "An ad still plays when you tap Generate. Scores are estimates; no tool can guarantee rankings.",
                    color = C.muted, fontSize = 11.sp,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !loading,
                onClick = {
                    loading = true
                    Ads.showRewarded(
                        ctx.findActivity(),
                        onReward = {
                            if (vm.onProAdWatched()) {
                                Toast.makeText(ctx, "👑 PRO unlocked for ${AppViewModel.PRO_HOURS} hours!", Toast.LENGTH_LONG).show()
                                vm.showPro = false
                            }
                        },
                        onFailed = {
                            loading = false
                            Toast.makeText(ctx, "No ad available right now. Please try again in a minute.", Toast.LENGTH_LONG).show()
                        },
                        onClosed = { loading = false },
                    )
                },
            ) {
                if (loading) {
                    CircularProgressIndicator(color = GOLD, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                } else {
                    Text("▶ Watch ad (${vm.proAds}/${AppViewModel.ADS_FOR_PRO})", color = GOLD, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = { TextButton(onClick = { vm.showPro = false }, enabled = !loading) { Text("Close", color = C.muted) } },
    )
}

/** Shown inside results for things only PRO unlocks. */
@Composable
fun ProUpsell(vm: AppViewModel, what: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(C.card)
            .border(1.dp, GOLD.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            .clickable { vm.showPro = true }
            .padding(16.dp),
    ) {
        Text("🔒 $what", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            "Free results are Basic (about 50% optimized). Watch ${AppViewModel.ADS_FOR_PRO} ads for ${AppViewModel.PRO_HOURS} hours of PRO.",
            color = C.muted, fontSize = 13.sp,
        )
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(GOLD)
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) { Text("👑 Unlock PRO", color = C.bg, fontWeight = FontWeight.ExtraBold) }
    }
}
