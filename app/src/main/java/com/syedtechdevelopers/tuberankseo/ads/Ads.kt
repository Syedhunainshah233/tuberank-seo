package com.syedtechdevelopers.tuberankseo.ads

import android.app.Activity
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.syedtechdevelopers.tuberankseo.BuildConfig
import java.lang.ref.WeakReference

/**
 * All 6 AdMob formats in one place (debug builds use Google's test units):
 *  1. App open        – app launch and coming back to the app
 *  2. Banner          – bottom of every screen
 *  3. Interstitial    – every tap on "Generate" (results load behind it)
 *  4. Rewarded        – optional: watch 5 to get PRO for 5 hours (unlimited, full SEO)
 *  5. Rewarded interstitial – optional: "More titles", after an intro dialog with a skip option
 *  6. Native          – inside results and history, with a clear "Ad" label
 */
object Ads {
    var ready by mutableStateOf(false)
        private set

    /** Shortest time between two full-screen ads (Google penalises back-to-back ads). */
    private const val MIN_GAP_MS = 15 * 1000L
    private var lastShownAt = 0L

    val isRobolectric: Boolean get() = Build.FINGERPRINT == "robolectric"

    fun start(activity: Activity) {
        if (isRobolectric || ready) return
        runCatching {
            val consent = UserMessagingPlatform.getConsentInformation(activity)
            consent.requestConsentInfoUpdate(
                activity,
                ConsentRequestParameters.Builder().build(),
                {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                        if (consent.canRequestAds()) initSdk(activity) else releaseSplash()
                    }
                },
                { if (consent.canRequestAds()) initSdk(activity) else releaseSplash() },
            )
            if (consent.canRequestAds()) initSdk(activity)
        }
    }

    /** "Privacy options" button in Settings is shown only where the law needs it (e.g. EU). */
    fun privacyOptionsRequired(activity: Activity): Boolean = runCatching {
        UserMessagingPlatform.getConsentInformation(activity).privacyOptionsRequirementStatus ==
            com.google.android.ump.ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }.getOrDefault(false)

    fun showPrivacyOptions(activity: Activity) {
        runCatching { UserMessagingPlatform.showPrivacyOptionsForm(activity) { } }
    }

    private var initStarted = false

    private fun initSdk(activity: Activity) {
        if (initStarted) return
        initStarted = true
        runCatching {
            MobileAds.initialize(activity.applicationContext) {
                ready = true
                val ctx = activity.applicationContext
                loadInterstitial(ctx)
                loadAppOpen(ctx)
                loadRewarded(ctx)
                loadRewardedInterstitial(ctx)
            }
        }
    }

    private fun gapOk() = System.currentTimeMillis() - lastShownAt >= MIN_GAP_MS

    // ---------- 3. Interstitial (Generate button) ----------

    private var interstitial: InterstitialAd? = null
    private var loadingInterstitial = false

    private fun loadInterstitial(context: Context) {
        if (loadingInterstitial || interstitial != null) return
        loadingInterstitial = true
        InterstitialAd.load(
            context, BuildConfig.AD_INTERSTITIAL, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitial = ad; loadingInterstitial = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitial = null; loadingInterstitial = false
                }
            },
        )
    }

    /** Shows an interstitial (if one is ready) when the user taps Generate. [onDone] always runs. */
    fun onGenerate(activity: Activity?, onDone: () -> Unit) {
        val ad = interstitial
        if (activity == null || !ready || ad == null || !gapOk()) {
            activity?.let { if (ready) loadInterstitial(it.applicationContext) }
            onDone(); return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitial = null
                loadInterstitial(activity.applicationContext)
                onDone()
            }

            override fun onAdFailedToShowFullScreenContent(e: AdError) {
                interstitial = null
                loadInterstitial(activity.applicationContext)
                onDone()
            }
        }
        lastShownAt = System.currentTimeMillis()
        suppressNextReturn()
        ad.show(activity)
    }

    // ---------- 4. Rewarded ----------

    private var rewarded: RewardedAd? = null
    private var loadingRewarded = false

    private fun loadRewarded(context: Context) {
        if (loadingRewarded || rewarded != null) return
        loadingRewarded = true
        RewardedAd.load(
            context, BuildConfig.AD_REWARDED, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewarded = ad; loadingRewarded = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewarded = null; loadingRewarded = false
                }
            },
        )
    }

    /**
     * [onReward] runs only if the video was watched; [onClosed] when the ad screen closes;
     * [onFailed] if no ad could be loaded. Loads on demand when none is waiting.
     */
    fun showRewarded(activity: Activity?, onReward: () -> Unit, onFailed: () -> Unit, onClosed: () -> Unit = {}) {
        if (activity == null || !ready) {
            onFailed(); return
        }
        val ad = rewarded
        if (ad == null) {
            RewardedAd.load(
                activity, BuildConfig.AD_REWARDED, AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(loaded: RewardedAd) {
                        rewarded = loaded
                        showRewarded(activity, onReward, onFailed, onClosed)
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) = onFailed()
                },
            )
            return
        }
        rewarded = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                loadRewarded(activity.applicationContext); onClosed()
            }

            override fun onAdFailedToShowFullScreenContent(e: AdError) {
                loadRewarded(activity.applicationContext); onFailed()
            }
        }
        lastShownAt = System.currentTimeMillis()
        suppressNextReturn()
        ad.show(activity) { onReward() }
    }

    // ---------- 5. Rewarded interstitial ----------

    private var rewardedInter: RewardedInterstitialAd? = null
    private var loadingRewardedInter = false

    private fun loadRewardedInterstitial(context: Context) {
        if (loadingRewardedInter || rewardedInter != null) return
        loadingRewardedInter = true
        RewardedInterstitialAd.load(
            context, BuildConfig.AD_REWARDED_INTERSTITIAL, AdRequest.Builder().build(),
            object : RewardedInterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedInterstitialAd) {
                    rewardedInter = ad; loadingRewardedInter = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedInter = null; loadingRewardedInter = false
                }
            },
        )
    }

    /** Must be called only after the user saw an intro dialog and did not skip it. */
    fun showRewardedInterstitial(activity: Activity?, onNotEarned: () -> Unit = {}, onReward: () -> Unit) {
        val ad = rewardedInter
        if (activity == null || !ready || ad == null) {
            activity?.let { if (ready) loadRewardedInterstitial(it.applicationContext) }
            onReward(); return // no ad available: don't block the user
        }
        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedInter = null; loadRewardedInterstitial(activity.applicationContext)
                // Reward is applied once the ad screen is gone, so the user sees the new titles.
                if (earned) onReward() else onNotEarned()
            }

            override fun onAdFailedToShowFullScreenContent(e: AdError) {
                rewardedInter = null; loadRewardedInterstitial(activity.applicationContext); onReward()
            }
        }
        lastShownAt = System.currentTimeMillis()
        suppressNextReturn()
        ad.show(activity) { earned = true }
    }

    // ---------- 1. App open ----------

    private const val APP_OPEN_MAX_AGE_MS = 4 * 60 * 60 * 1000L
    /** Longest the launch splash waits for the app open ad (first launch also starts the SDK). */
    private const val LAUNCH_WAIT_MS = 7_000L
    private var appOpen: AppOpenAd? = null
    private var appOpenLoadedAt = 0L
    private var loadingAppOpen = false
    private var launchActivity: WeakReference<Activity>? = null
    private var launchDeadline = 0L
    private var suppressReturn = false

    /** Called from onCreate on a cold start: the splash is held until the ad shows, fails or times out. */
    fun beginLaunch(activity: Activity) {
        if (isRobolectric) return
        launchActivity = WeakReference(activity)
        launchDeadline = System.currentTimeMillis() + LAUNCH_WAIT_MS
    }

    /** True while the launch splash should stay on screen. */
    fun holdSplash(): Boolean = launchDeadline != 0L && System.currentTimeMillis() < launchDeadline

    private fun releaseSplash() {
        launchDeadline = 0L
    }

    /** Call before leaving to another app of ours (share, browser, mail) so returning shows no ad. */
    fun suppressNextReturn() {
        suppressReturn = true
    }

    private fun appOpenFresh(): Boolean =
        appOpen != null && System.currentTimeMillis() - appOpenLoadedAt < APP_OPEN_MAX_AGE_MS

    private fun loadAppOpen(context: Context) {
        if (loadingAppOpen || appOpenFresh()) return
        loadingAppOpen = true
        AppOpenAd.load(
            context, BuildConfig.AD_APP_OPEN, AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpen = ad
                    appOpenLoadedAt = System.currentTimeMillis()
                    loadingAppOpen = false
                    val act = launchActivity?.get()
                    if (act != null && System.currentTimeMillis() < launchDeadline && !act.isFinishing) {
                        launchDeadline = 0L
                        showAppOpen(act)
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loadingAppOpen = false
                    releaseSplash()
                }
            },
        )
    }

    /** From the activity's onStart: app launch (coldStart) or coming back from the background. */
    fun onAppStart(activity: Activity, coldStart: Boolean) {
        if (isRobolectric) return
        if (suppressReturn) {
            suppressReturn = false
            return
        }
        if (coldStart) {
            launchActivity = WeakReference(activity)
            if (launchDeadline == 0L) launchDeadline = System.currentTimeMillis() + LAUNCH_WAIT_MS
            if (appOpenFresh()) {
                launchDeadline = 0L
                showAppOpen(activity)
            }
            return
        }
        showAppOpen(activity)
    }

    private fun showAppOpen(activity: Activity) {
        if (!ready) return
        val ad = appOpen?.takeIf { appOpenFresh() }
        if (ad == null) {
            loadAppOpen(activity.applicationContext); return
        }
        if (!gapOk()) return
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                appOpen = null; loadAppOpen(activity.applicationContext)
            }

            override fun onAdFailedToShowFullScreenContent(e: AdError) {
                appOpen = null; loadAppOpen(activity.applicationContext)
            }
        }
        appOpen = null
        releaseSplash()
        lastShownAt = System.currentTimeMillis()
        suppressNextReturn()
        ad.show(activity)
    }
}

// ---------- 2. Banner ----------

@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    if (!Ads.ready) return
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val widthDp = maxWidth.value.toInt()
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, widthDp))
                    adUnitId = BuildConfig.AD_BANNER
                    loadAd(AdRequest.Builder().build())
                }
            },
            onRelease = { it.destroy() },
        )
    }
}

// ---------- 6. Native ----------

@Composable
fun NativeAdCard(modifier: Modifier = Modifier) {
    if (!Ads.ready) return
    val ctx = LocalContext.current
    var ad by remember { mutableStateOf<NativeAd?>(null) }
    DisposableEffect(Unit) {
        var loaded: NativeAd? = null
        val loader = AdLoader.Builder(ctx, BuildConfig.AD_NATIVE)
            .forNativeAd { loaded = it; ad = it }
            .withAdListener(object : AdListener() {})
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()
        loader.loadAd(AdRequest.Builder().build())
        onDispose { loaded?.destroy() }
    }
    val current = ad ?: return
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { c -> buildNativeView(c) },
        update = { v -> bindNative(v, current) },
    )
}

private fun Context.dp(v: Int): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()

private fun buildNativeView(c: Context): NativeAdView {
    val card = 0xFF171717.toInt()
    val text = 0xFFFFFFFF.toInt()
    val muted = 0xFFA8A8B3.toInt()
    val button = 0xFFE50914.toInt()
    val root = NativeAdView(c)
    val row = LinearLayout(c).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(c.dp(14), c.dp(12), c.dp(12), c.dp(12))
        background = GradientDrawable().apply { setColor(card); cornerRadius = c.dp(18).toFloat() }
    }
    val icon = ImageView(c).apply {
        layoutParams = LinearLayout.LayoutParams(c.dp(46), c.dp(46)).apply { marginEnd = c.dp(12) }
        scaleType = ImageView.ScaleType.CENTER_CROP
        clipToOutline = true
        background = GradientDrawable().apply { cornerRadius = c.dp(12).toFloat(); setColor(0x22FFFFFF) }
    }
    val texts = LinearLayout(c).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
    }
    val titleRow = LinearLayout(c).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    val badge = TextView(c).apply {
        this.text = "Ad"
        setTextColor(0xFF111111.toInt())
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        typeface = Typeface.DEFAULT_BOLD
        setPadding(c.dp(6), c.dp(1), c.dp(6), c.dp(1))
        background = GradientDrawable().apply { setColor(0xFFFFC542.toInt()); cornerRadius = c.dp(6).toFloat() }
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { marginEnd = c.dp(6) }
    }
    val headline = TextView(c).apply {
        setTextColor(text)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        typeface = Typeface.DEFAULT_BOLD
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }
    val body = TextView(c).apply {
        setTextColor(muted)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        maxLines = 2
        ellipsize = TextUtils.TruncateAt.END
    }
    val cta = TextView(c).apply {
        setTextColor(0xFFFFFFFF.toInt())
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        typeface = Typeface.DEFAULT_BOLD
        maxLines = 1
        setPadding(c.dp(12), c.dp(8), c.dp(12), c.dp(8))
        background = GradientDrawable().apply { setColor(button); cornerRadius = c.dp(12).toFloat() }
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { marginStart = c.dp(8) }
    }
    titleRow.addView(badge)
    titleRow.addView(headline)
    texts.addView(titleRow)
    texts.addView(body)
    row.addView(icon)
    row.addView(texts)
    row.addView(cta)
    root.addView(row)
    root.iconView = icon
    root.headlineView = headline
    root.bodyView = body
    root.callToActionView = cta
    return root
}

private fun bindNative(view: NativeAdView, ad: NativeAd) {
    (view.headlineView as TextView).text = ad.headline
    (view.bodyView as TextView).apply {
        text = ad.body
        visibility = if (ad.body.isNullOrBlank()) View.GONE else View.VISIBLE
    }
    (view.callToActionView as TextView).apply {
        text = ad.callToAction
        visibility = if (ad.callToAction.isNullOrBlank()) View.GONE else View.VISIBLE
    }
    (view.iconView as ImageView).apply {
        val d = ad.icon?.drawable
        setImageDrawable(d)
        visibility = if (d == null) View.GONE else View.VISIBLE
    }
    view.setNativeAd(ad)
}
