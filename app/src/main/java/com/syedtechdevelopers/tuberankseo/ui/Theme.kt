package com.syedtechdevelopers.tuberankseo.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object C {
    val bg = Color(0xFF0A0A0A)
    val card = Color(0xFF171717)
    val card2 = Color(0xFF242424)
    val line = Color(0xFF333333)
    val text = Color(0xFFFFFFFF)
    val muted = Color(0xFFA8A8B3)
    val red = Color(0xFFE50914)
    val darkRed = Color(0xFF8B0000)
    val green = Color(0xFF2ED47A)
    val yellow = Color(0xFFFFC542)
    val grad = Brush.linearGradient(listOf(red, darkRed))
}

@Composable
fun TubeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = C.red,
            onPrimary = Color.White,
            secondary = C.darkRed,
            background = C.bg,
            onBackground = C.text,
            surface = C.card,
            onSurface = C.text,
            surfaceVariant = C.card2,
            onSurfaceVariant = C.muted,
            outline = C.line,
        ),
    ) {
        CompositionLocalProvider(LocalContentColor provides C.text, content = content)
    }
}

@Composable
fun Card(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(C.card)
            .padding(16.dp),
        content = content,
    )
}

@Composable
fun Pill(text: String, color: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) { Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
}

fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

val LANGUAGES = listOf("en" to "English", "hi" to "Hindi", "ur" to "Urdu", "ar" to "Arabic", "es" to "Spanish", "pt" to "Portuguese", "id" to "Indonesian", "bn" to "Bengali")
val REGIONS = listOf("GLOBAL" to "Global", "PK" to "Pakistan", "IN" to "India", "US" to "USA", "GB" to "UK", "AE" to "UAE", "SA" to "Saudi Arabia", "BD" to "Bangladesh", "CA" to "Canada")
