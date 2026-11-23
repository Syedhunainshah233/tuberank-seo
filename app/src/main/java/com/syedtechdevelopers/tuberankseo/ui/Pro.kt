package com.syedtechdevelopers.tuberankseo.ui

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
                    "Free: ${vm.freeLeft}/${AppViewModel.FREE_PER_DAY} left today",
                    fontWeight = FontWeight.Bold, fontSize = 14.sp,
                )
                Text("Basic SEO 50–60/100", color = C.muted, fontSize = 12.sp)
            }
            Column(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(GOLD)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("👑 FREE PRO", color = C.bg, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                Text("Watch 5 ads", color = C.bg, fontSize = 11.sp)
            }
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
                Text("🎬 Watch 5 ads – FREE PRO", color = GOLD, fontWeight = FontWeight.Bold)
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
        title = { Text("👑 PRO is FREE – no payment", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "You don't pay anything. Just watch ${AppViewModel.ADS_FOR_PRO} short ads and PRO unlocks for ${AppViewModel.PRO_HOURS} hours:",
                    color = C.muted,
                )
                listOf(
                    "Unlimited generations for ${AppViewModel.PRO_HOURS} hours",
                    "SEO score 100/100 (free plan is 50–60)",
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
                    "No payment, no subscription, no card. An ad still plays when you tap Generate. Scores are estimates; no tool can guarantee rankings.",
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
                    Text("▶ Watch ad ${vm.proAds + 1} of ${AppViewModel.ADS_FOR_PRO} (free)", color = GOLD, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = { TextButton(onClick = { vm.showPro = false }, enabled = !loading) { Text("Close", color = C.muted) } },
    )
}

/** Big clear button text used everywhere: PRO is free, it only costs 5 ads. */
const val FREE_PRO_CTA = "🎬 Watch 5 ads • Unlock FREE"

/**
 * Shows the full PRO result blurred, with a lock card on top:
 * "100/100 SEO – watch 5 ads to unlock, no payment".
 */
@Composable
fun LockedPreview(vm: AppViewModel, title: String, content: @Composable ColumnScope.() -> Unit) {
    val blurOk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(max = 360.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, GOLD.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
            .clickable { vm.showPro = true },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .then(if (blurOk) Modifier.blur(9.dp) else Modifier)
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
        // Older phones can't blur: cover more strongly so the text can't be read.
        Box(
            Modifier
                .matchParentSize()
                .background(C.bg.copy(alpha = if (blurOk) 0.35f else 0.88f)),
        )
        Column(
            Modifier
                .align(Alignment.Center)
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(C.card.copy(alpha = 0.96f))
                .border(1.dp, GOLD, RoundedCornerShape(16.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("🔒 $title", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text("100/100 SEO with PRO", color = C.green, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("100% FREE • No payment • ${AppViewModel.PRO_HOURS} hours", color = C.muted, fontSize = 12.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(GOLD)
                    .padding(horizontal = 14.dp, vertical = 9.dp),
            ) { Text(FREE_PRO_CTA, color = C.bg, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp) }
        }
    }
}
