package com.syedtechdevelopers.tuberankseo.ui

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.syedtechdevelopers.tuberankseo.AppViewModel
import com.syedtechdevelopers.tuberankseo.R
import com.syedtechdevelopers.tuberankseo.ads.Ads
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class GoogleProfile(val name: String, val email: String, val photo: String)

/**
 * Google sign-in with Google Identity Services. Needs an "Android" OAuth client in Google Cloud
 * for package com.syedtechdevelopers.tuberankseo + the app's SHA-1 (no server or web client needed).
 * We only read name, email and photo, and keep them on the phone.
 */
object GoogleLogin {
    private val scopes = listOf(Scope("openid"), Scope("email"), Scope("profile"))

    suspend fun authorize(ctx: Context): AuthorizationResult =
        Identity.getAuthorizationClient(ctx)
            .authorize(AuthorizationRequest.builder().setRequestedScopes(scopes).build())
            .await()

    suspend fun profile(token: String): GoogleProfile? = withContext(Dispatchers.IO) {
        runCatching {
            val c = URL("https://www.googleapis.com/oauth2/v3/userinfo").openConnection() as HttpURLConnection
            c.setRequestProperty("Authorization", "Bearer $token")
            c.connectTimeout = 8000; c.readTimeout = 8000
            val o = JSONObject(c.inputStream.use { it.readBytes().toString(Charsets.UTF_8) })
            c.disconnect()
            GoogleProfile(o.optString("name"), o.optString("email"), o.optString("picture"))
        }.getOrNull()
    }
}

/** Returns a function that starts Google sign-in. [onDone] gets the profile, or null on cancel/error. */
@Composable
fun rememberGoogleLogin(onDone: (GoogleProfile?) -> Unit): () -> Unit {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val callback by rememberUpdatedState(onDone)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { res ->
        scope.launch {
            val token = if (res.resultCode == Activity.RESULT_OK) runCatching {
                Identity.getAuthorizationClient(ctx).getAuthorizationResultFromIntent(res.data).accessToken
            }.getOrNull() else null
            callback(token?.let { GoogleLogin.profile(it) })
        }
    }
    return {
        scope.launch {
            try {
                val result = GoogleLogin.authorize(ctx)
                val pending = result.pendingIntent
                if (result.hasResolution() && pending != null) {
                    Ads.suppressNextReturn()
                    launcher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
                } else {
                    callback(result.accessToken?.let { GoogleLogin.profile(it) })
                }
            } catch (e: Exception) {
                callback(null)
            }
        }
    }
}

/** Round profile photo, or the first letter of the name while it loads / if there is none. */
@Composable
fun Avatar(name: String, photo: String, size: Int = 44) {
    var bmp by remember(photo) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(photo) {
        if (photo.isNotBlank()) bmp = withContext(Dispatchers.IO) {
            runCatching { URL(photo).openStream().use { BitmapFactory.decodeStream(it) } }.getOrNull()
        }
    }
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(C.red),
        contentAlignment = Alignment.Center,
    ) {
        val b = bmp
        if (b != null) {
            Image(b.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Text(name.take(1).uppercase().ifBlank { "?" }, fontWeight = FontWeight.Bold, fontSize = (size / 2.2).sp)
        }
    }
}

/** First screen: sign in with Google (or skip). */
@Composable
fun WelcomeScreen(vm: AppViewModel) {
    val ctx = LocalContext.current
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    val login = rememberGoogleLogin { p ->
        busy = false
        if (p != null) vm.signedIn(p) else error = true
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(C.bg)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))
        Image(
            painterResource(R.drawable.logo), "TubeRank SEO logo",
            modifier = Modifier.size(140.dp).clip(RoundedCornerShape(30.dp)),
        )
        Spacer(Modifier.height(22.dp))
        Text("TubeRank SEO", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Titles, descriptions, tags & hashtags that help your videos get found.",
            color = C.muted, textAlign = TextAlign.Center, fontSize = 15.sp,
        )
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            com.syedtechdevelopers.tuberankseo.data.Platform.entries.forEach {
                Image(painterResource(it.icon()), it.label, Modifier.size(40.dp))
            }
        }
        Spacer(Modifier.height(36.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .clickable(enabled = !busy) { busy = true; error = false; login() }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (busy) {
                CircularProgressIndicator(color = C.red, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            } else {
                Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                Spacer(Modifier.width(12.dp))
                Text("Continue with Google", color = Color(0xFF1F1F1F), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
        if (error) {
            Spacer(Modifier.height(10.dp))
            Text("Sign-in didn't complete. Please try again.", color = C.red, fontSize = 13.sp)
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "Skip for now", color = C.muted, fontSize = 14.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable { vm.skipLogin() }
                .padding(horizontal = 14.dp, vertical = 8.dp),
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "We only use your name, email and photo to show your profile. They stay on this phone. " +
                "By continuing you agree to our Terms and Privacy Policy.",
            color = C.muted, fontSize = 11.sp, textAlign = TextAlign.Center,
        )
    }
}

/** Account row for Settings: profile + sign out, or a sign-in button. */
@Composable
fun AccountCard(vm: AppViewModel) {
    var busy by remember { mutableStateOf(false) }
    val login = rememberGoogleLogin { p -> busy = false; if (p != null) vm.signedIn(p) }
    Card {
        if (vm.userEmail.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(vm.userName, vm.userPhoto)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(vm.userName.ifBlank { "Google account" }, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(vm.userEmail, color = C.muted, fontSize = 12.sp, maxLines = 1)
                }
                Text(
                    "Sign out", color = C.red, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, C.red.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                        .clickable { vm.signOut() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        } else {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .clickable(enabled = !busy) { busy = true; login() }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                Spacer(Modifier.width(10.dp))
                Text(if (busy) "Signing in…" else "Sign in with Google", color = Color(0xFF1F1F1F), fontWeight = FontWeight.Bold)
            }
        }
    }
}

fun com.syedtechdevelopers.tuberankseo.data.Platform.icon(): Int = when (this) {
    com.syedtechdevelopers.tuberankseo.data.Platform.YouTube -> R.drawable.pf_youtube
    com.syedtechdevelopers.tuberankseo.data.Platform.TikTok -> R.drawable.pf_tiktok
    com.syedtechdevelopers.tuberankseo.data.Platform.Instagram -> R.drawable.pf_instagram
    com.syedtechdevelopers.tuberankseo.data.Platform.Facebook -> R.drawable.pf_facebook
}
