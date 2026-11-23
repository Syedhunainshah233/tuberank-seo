package com.syedtechdevelopers.tuberankseo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import com.syedtechdevelopers.tuberankseo.ui.LimitDialog
import com.syedtechdevelopers.tuberankseo.ui.ProDialog
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.syedtechdevelopers.tuberankseo.ads.Ads
import com.syedtechdevelopers.tuberankseo.ads.BannerAd
import com.syedtechdevelopers.tuberankseo.ui.C
import com.syedtechdevelopers.tuberankseo.ui.HistoryScreen
import com.syedtechdevelopers.tuberankseo.ui.HomeScreen
import com.syedtechdevelopers.tuberankseo.ui.ResultScreen
import com.syedtechdevelopers.tuberankseo.ui.SettingsScreen
import com.syedtechdevelopers.tuberankseo.ui.TubeTheme
import com.syedtechdevelopers.tuberankseo.ui.findActivity

class MainActivity : ComponentActivity() {
    internal val vm: AppViewModel by viewModels()
    private var coldStart = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        coldStart = savedInstanceState == null
        enableEdgeToEdge()
        setContent {
            TubeTheme { App(vm) }
        }
        Ads.start(this)
    }

    override fun onStart() {
        super.onStart()
        Ads.onAppStart(this, coldStart)
        coldStart = false
    }
}

@Composable
fun App(vm: AppViewModel) {
    val ctx = LocalContext.current
    val generate = {
        if (vm.mayGenerate()) {
            // Results start loading straight away; the interstitial plays on top of the loading screen.
            vm.generate()
            Ads.onGenerate(ctx.findActivity()) { }
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            vm.refreshDay(); delay(30_000)
        }
    }
    if (vm.showLimit) LimitDialog(vm)
    if (vm.showPro) ProDialog(vm)
    Column(
        Modifier
            .fillMaxSize()
            .background(C.bg)
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Box(Modifier.weight(1f)) {
            AnimatedContent(
                targetState = if (vm.showResult) null else vm.tab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen",
            ) { t ->
                when (t) {
                    null -> ResultScreen(vm)
                    Tab.Home -> HomeScreen(vm, onGenerate = generate)
                    Tab.History -> HistoryScreen(vm)
                    Tab.Settings -> SettingsScreen(vm)
                }
            }
        }
        BannerAd()
        if (!vm.showResult) {
            BackHandler(enabled = vm.tab != Tab.Home) { vm.tab = Tab.Home }
            NavigationBar(containerColor = C.card, modifier = Modifier.fillMaxWidth(), windowInsets = WindowInsets(0)) {
                listOf(
                    Triple(Tab.Home, "Generate", Icons.Filled.Home),
                    Triple(Tab.History, "History", Icons.Filled.List),
                    Triple(Tab.Settings, "Settings", Icons.Filled.Settings),
                ).forEach { (t, label, icon) ->
                    NavigationBarItem(
                        selected = vm.tab == t,
                        onClick = { vm.tab = t },
                        icon = { Icon(icon, label) },
                        label = { Text(label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = C.text, selectedTextColor = C.text,
                            indicatorColor = C.red, unselectedIconColor = C.muted, unselectedTextColor = C.muted,
                        ),
                    )
                }
            }
        }
    }
}
