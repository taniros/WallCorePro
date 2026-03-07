package com.offline.wallcorepro.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ─── Good Morning / Good Night Color Tokens ───────────────────────────────────
// Morning palette: warm sunrise hues
val SunriseAmber     = Color(0xFFFFB300)   // Golden amber
val SunriseOrange    = Color(0xFFFF6F00)   // Deep sunrise orange
val SunrisePeach     = Color(0xFFFFCCBC)   // Soft peach sky
val MorningGold      = Color(0xFFFFD54F)   // Soft golden yellow

// Night palette: deep moonlight hues
val MoonPurple       = Color(0xFF7C4DFF)   // Rich purple
val MidnightBlue     = Color(0xFF0D1B3E)   // Deep midnight blue
val StarGlow         = Color(0xFFB0BEC5)   // Soft star/silver
val NightNavy        = Color(0xFF090E1A)   // Near-black deep navy

// Shared
val BackgroundWarm   = Color(0xFF0D0A0E)   // Warm near-black
val SurfaceWarm      = Color(0xFF1A1015)   // Deep warm surface
val CardWarm         = Color(0xFF251820)   // Warm card surface
val OnSurfaceWarm    = Color(0xFFF5E6D3)   // Warm white / cream

// ─── Dark Scheme (default – moonlit night mood) ────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary              = SunriseAmber,
    onPrimary            = Color(0xFF1A0A00),
    primaryContainer     = SunriseOrange.copy(alpha = 0.3f),
    onPrimaryContainer   = SunrisePeach,
    secondary            = MoonPurple,
    onSecondary          = Color.White,
    secondaryContainer   = Color(0xFF2D1B69),
    onSecondaryContainer = Color(0xFFDDD0FF),
    tertiary             = StarGlow,
    background           = BackgroundWarm,
    onBackground         = OnSurfaceWarm,
    surface              = SurfaceWarm,
    onSurface            = OnSurfaceWarm,
    surfaceVariant       = CardWarm,
    onSurfaceVariant     = Color(0xFFCFA882),
    outline              = SunriseAmber.copy(alpha = 0.5f),
    error                = Color(0xFFFF6B6B),
    onError              = Color(0xFF3B0000)
)

// ─── Light Scheme (morning mode – warm sunrise) ────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary              = SunriseOrange,
    onPrimary            = Color.White,
    primaryContainer     = Color(0xFFFFE0B2),
    onPrimaryContainer   = Color(0xFF3D1600),
    secondary            = MoonPurple,
    onSecondary          = Color.White,
    secondaryContainer   = Color(0xFFEDE7F6),
    onSecondaryContainer = Color(0xFF21005E),
    tertiary             = Color(0xFF795548),
    background           = Color(0xFFFFF8F0),
    onBackground         = Color(0xFF1A0E00),
    surface              = Color(0xFFFFF3E0),
    onSurface            = Color(0xFF1A0E00),
    surfaceVariant       = Color(0xFFFFE0B2),
    onSurfaceVariant     = Color(0xFF5C3D11)
)

// ─── Theme composable ─────────────────────────────────────────────────────────
@Composable
fun WallCoreTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars     = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = WallCoreTypography,
        shapes      = WallCoreShapes,
        content     = content
    )
}
