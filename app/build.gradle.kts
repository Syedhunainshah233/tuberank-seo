plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.syedtechdevelopers.tuberankseo"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.syedtechdevelopers.tuberankseo"
        minSdk = 26
        targetSdk = 36
        versionCode = 4
        versionName = "1.0.3"
    }

    // Play Store upload key comes from GitHub Actions secrets; it is never stored in the repo.
    val releaseKeystore = System.getenv("TUBERANK_KEYSTORE")

    signingConfigs {
        // Fixed test key so the SHA-1 registered in Google Cloud stays the same on every build.
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = System.getenv("TUBERANK_KEYSTORE_PASSWORD")
                keyAlias = "tuberank"
                keyPassword = System.getenv("TUBERANK_KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            // Google's official TEST ads: safe to click, never earn money.
            manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
            buildConfigField("String", "AD_APP_OPEN", "\"ca-app-pub-3940256099942544/9257395921\"")
            buildConfigField("String", "AD_BANNER", "\"ca-app-pub-3940256099942544/9214589741\"")
            buildConfigField("String", "AD_INTERSTITIAL", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "AD_REWARDED", "\"ca-app-pub-3940256099942544/5224354917\"")
            buildConfigField("String", "AD_REWARDED_INTERSTITIAL", "\"ca-app-pub-3940256099942544/5354046379\"")
            buildConfigField("String", "AD_NATIVE", "\"ca-app-pub-3940256099942544/2247696110\"")
        }
        release {
            // Real TubeRank SEO ad units (AdMob account pub-8064134362670148).
            manifestPlaceholders["admobAppId"] = "ca-app-pub-8064134362670148~9344795441"
            buildConfigField("String", "AD_APP_OPEN", "\"ca-app-pub-8064134362670148/8794131106\"")
            buildConfigField("String", "AD_BANNER", "\"ca-app-pub-8064134362670148/1466305423\"")
            buildConfigField("String", "AD_INTERSTITIAL", "\"ca-app-pub-8064134362670148/2658686333\"")
            buildConfigField("String", "AD_REWARDED", "\"ca-app-pub-8064134362670148/8139524983\"")
            buildConfigField("String", "AD_REWARDED_INTERSTITIAL", "\"ca-app-pub-8064134362670148/9452606651\"")
            buildConfigField("String", "AD_NATIVE", "\"ca-app-pub-8064134362670148/6826443316\"")
            if (releaseKeystore != null) signingConfig = signingConfigs.getByName("release")
            // R8: shrink + obfuscate (Play Console "DEX code optimization").
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.systemProperty("roborazzi.test.record", "true")
                it.systemProperty("robolectric.pixelCopyRenderMode", "hardware")
                it.maxHeapSize = "3g"
            }
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    // AdMob pulls in androidx.fragment 1.1.0, which Play flags as outdated; pin a current version.
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")

    // AdMob + consent (GDPR) form
    implementation("com.google.android.gms:play-services-ads:23.5.0")
    implementation("com.google.android.ump:user-messaging-platform:3.1.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core-ktx:1.6.1")
    testImplementation("androidx.test.ext:junit-ktx:1.2.1")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.32.2")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.32.2")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
