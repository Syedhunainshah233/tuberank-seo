# Changelog

## 1.0.8
- Unity Ads added through AdMob mediation (banner, interstitial, rewarded)
- Google Mobile Ads SDK updated to 25.4.0

## 1.0.7
- Fix: "Watch 5 ads • Unlock FREE" button in locked sections now opens PRO (taps were going to the hidden list behind it)

## 1.0.6
- Removed the "More titles / More hooks" button and its ad (rewarded interstitial no longer used)

## 1.0.5
- Fix "isn't responding" (ANR): launch splash waits at most 3 s for the app open ad (was 7 s)
- AdMob starts on a background thread; Google's OPTIMIZE_INITIALIZATION / OPTIMIZE_AD_LOADING flags on
- Native ads are cached per screen and reused instead of reloading on every tab change

## 1.0.4
- "More titles / hooks" now always adds brand-new titles at the top with a "New" badge and a message
- Reward from the "More titles" ad is applied after the ad closes; a message explains when the ad was closed early

## 1.0.3
- App open ad on launch: logo splash stays up while the ad loads (was dropped after 8 s on slow real-ad loads)

## 1.0.2
- R8 code shrinking and obfuscation for release builds
- Updated androidx.fragment (was 1.1.0 via AdMob)
- Edge-to-edge: removed deprecated status/navigation bar colours from the theme (enableEdgeToEdge handles them)

## 1.0.1
- Targets Android 16 (API 36), required by Google Play

## 1.0.0
- SEO titles/hooks, description/caption, tags/keywords and hashtags for YouTube, TikTok, Instagram and Facebook
- Free: 5 generations a day, Basic SEO (50–60/100)
- PRO free for 5 hours after watching 5 rewarded ads: unlimited generations, 100/100 SEO
- Blurred PRO preview in every result tab
- AdMob: app open, banner, interstitial, rewarded, rewarded interstitial, native

