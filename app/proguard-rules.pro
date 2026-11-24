# TubeRank SEO - R8 rules for release builds.
# AdMob, UMP, Compose and AndroidX ship their own consumer rules, so only app-specific rules are here.

# Keep file/line info so crash reports in Play Console stay readable (mapping.txt is uploaded with the AAB).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Saved history and settings use enum names and data fields; keep them stable.
-keep enum com.syedtechdevelopers.tuberankseo.** { *; }
-keepclassmembers class com.syedtechdevelopers.tuberankseo.data.** { *; }
