package com.syedtechdevelopers.tuberankseo.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import com.syedtechdevelopers.tuberankseo.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.syedtechdevelopers.tuberankseo.AppViewModel
import com.syedtechdevelopers.tuberankseo.ads.NativeAdCard
import com.syedtechdevelopers.tuberankseo.data.Platform

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(vm: AppViewModel, onGenerate: () -> Unit) {
    val focus = LocalFocusManager.current
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // Header
        Box(
            Modifier
                .fillMaxWidth()
                .background(C.grad)
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 22.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painterResource(R.drawable.logo), "TubeRank SEO logo",
                    modifier = Modifier.size(60.dp).clip(RoundedCornerShape(14.dp)),
                )
                Spacer(Modifier.width(14.dp))
                Column {
                Text("TubeRank SEO", color = C.text, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "YouTube • TikTok • Instagram • Facebook",
                    color = C.text.copy(alpha = 0.9f), fontSize = 14.sp,
                )
                }
            }
        }

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            PlatformTabs(vm.platform) { vm.setPlatformTo(it) }
            PlanCard(vm)
            Card {
                Text("${vm.platform.label} video topic or keyword", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    when (vm.platform) {
                        Platform.YouTube -> "Get titles, description, tags & hashtags"
                        else -> "Get hooks, caption, keywords & hashtags"
                    },
                    color = C.muted, fontSize = 13.sp,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = vm.keyword,
                    onValueChange = { if (it.length <= 80) vm.keyword = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. how to make biryani") },
                    leadingIcon = { Icon(Icons.Filled.Search, null, tint = C.muted) },
                    trailingIcon = {
                        if (vm.keyword.isNotEmpty()) {
                            IconButton(onClick = { vm.keyword = "" }) { Icon(Icons.Filled.Clear, "Clear", tint = C.muted) }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        focus.clearFocus()
                        if (vm.canGenerate()) onGenerate()
                    }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = C.red, unfocusedBorderColor = C.line,
                        focusedContainerColor = C.card2, unfocusedContainerColor = C.card2,
                    ),
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Picker("Language", LANGUAGES, vm.lang, Modifier.weight(1f)) { vm.setLanguage(it) }
                    Picker("Region", REGIONS, vm.country, Modifier.weight(1f)) { vm.setRegion(it) }
                }
                Spacer(Modifier.height(16.dp))
                GradientButton(
                    text = "⚡ Generate ${vm.platform.label} SEO",
                    enabled = vm.canGenerate(),
                    onClick = { focus.clearFocus(); onGenerate() },
                )
            }

            NativeAdCard()

            if (vm.history.isNotEmpty()) {
                Card {
                    Text("Recent", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        vm.history.take(8).forEach { r ->
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(C.card2)
                                    .clickable { vm.open(r) }
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Image(painterResource(r.platform.icon()), r.platform.label, Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(r.keyword, fontSize = 13.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }

            Card {
                Text("How it works", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                listOf(
                    "🔎" to "Reads what people really search for your topic",
                    "🏷️" to "Follows each platform's limits (YouTube 500-char tags, Instagram 5 hashtags…)",
                    "✍️" to "Writes titles and hooks with your keyword first",
                    "📈" to "Gives an SEO score and a checklist to improve your video",
                ).forEach { (e, t) ->
                    Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
                        Text(e, fontSize = 16.sp)
                        Spacer(Modifier.width(10.dp))
                        Text(t, color = C.muted, fontSize = 14.sp)
                    }
                }
            }
            Text(
                "Not affiliated with YouTube or Google.",
                color = C.muted, fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun GradientButton(text: String, enabled: Boolean = true, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(C.grad)
            .alpha(if (enabled) 1f else 0.45f)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = C.text, fontWeight = FontWeight.Bold, fontSize = 17.sp) }
}

@Composable
fun Picker(label: String, items: List<Pair<String, String>>, value: String, modifier: Modifier, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(C.card2)
                .clickable { open = true }
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(label, color = C.muted, fontSize = 11.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    items.firstOrNull { it.first == value }?.second ?: value,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1,
                )
                Icon(Icons.Filled.ArrowDropDown, null, tint = C.muted, modifier = Modifier.size(20.dp))
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            items.forEach { (code, name) ->
                DropdownMenuItem(text = { Text(name) }, onClick = { onPick(code); open = false })
            }
        }
    }
}

@Composable
fun PlatformTabs(selected: Platform, onPick: (Platform) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(C.card)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Platform.entries.forEach { p ->
            val on = p == selected
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .then(if (on) Modifier.background(C.grad) else Modifier)
                    .clickable { onPick(p) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(painterResource(p.icon()), p.label, Modifier.size(30.dp))
                Spacer(Modifier.height(4.dp))
                Text(
                    p.label, fontSize = 11.sp, maxLines = 1,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                    color = if (on) C.text else C.muted,
                )
            }
        }
    }
}
