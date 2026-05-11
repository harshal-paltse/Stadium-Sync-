package com.stadiumsync.app.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// ─── ELECTRIC PALETTE ────────────────────────────────────
// Primary family
val Obsidian = Color(0xFF080B12)
val Gunmetal = Color(0xFF111827)
val Slate = Color(0xFF1F2937)
val Zinc = Color(0xFF374151)
val Ash = Color(0xFF6B7280)

// Signal colors — BOLD, not pastel
val ElectricCyan = Color(0xFF06D6A0)       // Success / Live
val HotCoral = Color(0xFFEF476F)            // Danger / Critical
val SolarAmber = Color(0xFFFFD166)          // Warning
val VividBlue = Color(0xFF118AB2)           // Primary / Info
val DeepViolet = Color(0xFF7B2FF7)         // Special / AI
val IceWhite = Color(0xFFF9FAFB)
val PaperWhite = Color(0xFFFFFFFF)
val MutedText = Color(0xFF9CA3AF)

// Surface
val SurfLight = Color(0xFFF3F4F6)
val SurfDark = Color(0xFF111827)
val CardLight = Color(0xFFFFFFFF)
val CardDark = Color(0xFF1F2937)

// ─── GRADIENTS ───────────────────────────────────────────
val CyanGlow = Brush.linearGradient(listOf(Color(0xFF06D6A0), Color(0xFF118AB2)))
val CoralGlow = Brush.linearGradient(listOf(Color(0xFFEF476F), Color(0xFFFF6B6B)))
val VioletGlow = Brush.linearGradient(listOf(Color(0xFF7B2FF7), Color(0xFFA855F7)))
val AmberGlow = Brush.linearGradient(listOf(Color(0xFFFFD166), Color(0xFFFBBF24)))
val OceanGrad = Brush.linearGradient(listOf(Color(0xFF118AB2), Color(0xFF06D6A0)))
val NightGrad = Brush.verticalGradient(listOf(Color(0xFF080B12), Color(0xFF111827)))
val HeroLight = Brush.verticalGradient(listOf(PaperWhite, Color(0xFFF0F4FF), PaperWhite))

// ─── THEME COLORS ────────────────────────────────────────
private val LightScheme = lightColorScheme(
    primary = VividBlue, onPrimary = PaperWhite,
    primaryContainer = Color(0xFFDBEAFE), onPrimaryContainer = Color(0xFF0C1D36),
    secondary = ElectricCyan, onSecondary = Obsidian,
    background = PaperWhite, onBackground = Obsidian,
    surface = SurfLight, onSurface = Gunmetal,
    surfaceVariant = Color(0xFFE5E7EB), onSurfaceVariant = Ash,
    error = HotCoral, onError = PaperWhite,
    outline = Color(0xFFD1D5DB), outlineVariant = Color(0xFFE5E7EB)
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF60A5FA), onPrimary = Obsidian,
    primaryContainer = Color(0xFF1E3A5F), onPrimaryContainer = Color(0xFF93C5FD),
    secondary = ElectricCyan, onSecondary = Obsidian,
    background = Obsidian, onBackground = IceWhite,
    surface = Gunmetal, onSurface = IceWhite,
    surfaceVariant = Slate, onSurfaceVariant = MutedText,
    error = Color(0xFFFCA5A5), onError = Color(0xFF7F1D1D),
    outline = Zinc, outlineVariant = Slate
)

// ─── TYPOGRAPHY — BOLD & DISTINCTIVE ─────────────────────
val AppType = Typography(
    displayLarge = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.Black, letterSpacing = (-2).sp, lineHeight = 48.sp),
    displayMedium = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Black, letterSpacing = (-1.5).sp),
    displaySmall = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp),
    headlineLarge = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.1.sp),
    titleSmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal, lineHeight = 18.sp),
    bodySmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Normal, lineHeight = 15.sp),
    labelLarge = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp),
    labelMedium = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp), small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(22.dp), extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun StadiumSyncTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val scheme = if (darkTheme) DarkScheme else LightScheme
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        val w = (view.context as Activity).window
        w.statusBarColor = if (darkTheme) Obsidian.toArgb() else PaperWhite.toArgb()
        WindowCompat.getInsetsController(w, view).isAppearanceLightStatusBars = !darkTheme
    }
    MaterialTheme(colorScheme = scheme, typography = AppType, shapes = AppShapes, content = content)
}
