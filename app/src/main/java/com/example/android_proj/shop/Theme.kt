package com.example.android_proj.shop

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.android_proj.R

/** cennge-ui tokens (dark theme). */
object Palette {
    val Background = Color(0xFF111317)
    val Surface2 = Color(0xFF181B20)
    val Surface3 = Color(0xFF21252B)
    val Surface4 = Color(0xFF2C3038)
    val Foreground = Color(0xFFE8EAEE)
    val Muted = Color(0xFF9AA2AE)
    val Border = Color(0xFF2F343C)
    val Primary = Color(0xFFFB6C1A)
    val PrimaryLight = Color(0xFFFF8A3D)
    val PrimaryText = Color(0xFFFFB06D)
    val OnPrimary = Color(0xFF140C06)
    val Violet = Color(0xFF7C3AED)
    val Pink = Color(0xFFFF4D6D)
    val Gold = Color(0xFFFFC53D)
    val Green = Color(0xFF12B85F)
}

val Inter = FontFamily(
    Font(R.font.inter_400, FontWeight.Normal),
    Font(R.font.inter_500, FontWeight.Medium),
    Font(R.font.inter_600, FontWeight.SemiBold),
    Font(R.font.inter_700, FontWeight.Bold),
    Font(R.font.inter_800, FontWeight.ExtraBold),
)

@Composable
fun ShopTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Palette.Primary,
            onPrimary = Palette.OnPrimary,
            background = Palette.Background,
            onBackground = Palette.Foreground,
            surface = Palette.Surface2,
            onSurface = Palette.Foreground,
            surfaceContainerLow = Palette.Surface2,
        ),
    ) {
        CompositionLocalProvider(
            LocalContentColor provides Palette.Foreground,
            LocalTextStyle provides TextStyle(fontFamily = Inter, color = Palette.Foreground),
            content = content,
        )
    }
}
