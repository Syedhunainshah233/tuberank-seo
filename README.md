# TubeRank SEO

Android app (Kotlin + Jetpack Compose) by **SyedTechdevelopers** that generates SEO metadata for
**YouTube, TikTok, Instagram and Facebook** from live search suggestions:

- YouTube: titles (≤ 70 chars), description with timestamps, tags (≤ 500 chars), hashtags (≤ 15)
- TikTok / Instagram / Facebook: hooks, caption, search keywords, hashtags (Instagram max 5)
- SEO score, search volume / competition estimate, SEO checklist, video ideas, history

## Ads (AdMob, 6 formats)
| Format | Where |
|---|---|
| App open | App launch and return to app |
| Banner | Bottom of every screen |
| Interstitial | Every tap on **Generate** (15 s minimum gap) |
| Rewarded | Unlock "Ideas" (40 long-tail keywords) |
| Rewarded interstitial | "More titles / hooks", after a 5 s intro with "No thanks" |
| Native | Inside results, home and history |

Ad unit IDs live in `app/build.gradle.kts`: **debug** builds use Google test ads, **release**
builds use the real TubeRank SEO units (App ID `ca-app-pub-8064134362670148~9344795441`).

## Release (Play Store)
Add two repository secrets (Settings → Secrets and variables → Actions):
`TUBERANK_KEYSTORE_BASE64` and `TUBERANK_KEYSTORE_PASSWORD`. Every push then builds a signed
`TubeRankSEO.apk` (real ads) and `TubeRankSEO-playstore.aab` for Play Console.

## Build
Every push to `main` builds on GitHub Actions. Download the APK from the
[latest release](../../releases/latest). Screenshots of every screen are saved to the
`screenshots` branch.

Privacy policy: https://syedhunainshah233.github.io/tuberank-seo/privacy.html
