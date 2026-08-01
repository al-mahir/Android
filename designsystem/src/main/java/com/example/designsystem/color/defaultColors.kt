package com.example.designsystem.color

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Verdant Design System — brand palette (Al-Mahir).
 * Deep forest-green primary (#014F39) communicating trust, professionalism, and
 * premium clarity. Tokens follow Material Design 3 roles and target WCAG AA.
 */
val lightColors = ColorScheme(
    primary = Color(0xFF014F39),           // --color-primary: Forest Green
    primaryVariant = Color(0xFF013729),
    onPrimary = Color(0xFFFFFFFF),         // --color-primary-fg: White
    primaryContainer = Color(0xFFCCE8D7),
    onPrimaryContainer = Color(0xFF003822),
    secondary = Color(0xFF3A6B57),
    onSecondary = Color(0xFFFFFFFF),
    backGround = Color(0xFFFFFFFF),        // --color-background: Pure White
    primaryFont = Color(0xFF0F0F0F),       // --color-text: Near Black
    secondaryFont = Color(0xFF3D4E41),
    surface = Color(0xFFFFFFFF),           // --color-surface: White
    surfaceVariant = Color(0xFFDDE5DE),
    surfaceContainer = Color(0xFFF5F5F5),  // card / chip bg light
    onSurface = Color(0xFF0F0F0F),
    field = Color(0xFFF7F7F7),             // --color-field: Neutral Gray
    border = Color(0xFFE8E8E8),            // --color-border: Light Gray
    outline = Color(0xFF6D7D71),
    hint = Color(0xFF8A9A8E),
    warning = Color(0xFF7A5500),
    onWarning = Color(0xFFFFFFFF),
    error = Color(0xFFC93B2B),             // --color-error: Deep Red
    onError = Color(0xFFFFFFFF),
    success = Color(0xFF1B6B3A),
    onSuccess = Color(0xFFFFFFFF),
    amber = Color(0xFF7A5800),
    onAmber = Color(0xFFFFFFFF),
    disable = Color(0xFFDDE5DE),
    onDisable = Color(0xFF9BA8A0),
)

val darkColors = ColorScheme(
    primary = Color(0xFF5DC995),           // --color-primary: Mint Green
    primaryVariant = Color(0xFF48BF80),
    onPrimary = Color(0xFF002E1C),         // --color-primary-fg: Deep Forest
    primaryContainer = Color(0xFF127043),
    onPrimaryContainer = Color(0xFFCCE8D7),
    secondary = Color(0xFF9EDAB9),
    onSecondary = Color(0xFF002D1E),
    backGround = Color(0xFF0F1418),        // --color-background: Near Black
    primaryFont = Color(0xFFE8EDEA),       // --color-text: Off White
    secondaryFont = Color(0xFFBAC9BC),
    surface = Color(0xFF171C18),           // --color-surface: Dark Surface
    surfaceVariant = Color(0xFF252C23),
    surfaceContainer = Color(0xFF191A1A),  // card / chip bg dark
    onSurface = Color(0xFFE8EDEA),
    field = Color(0xFF252C23),             // --color-field: Dark Container
    border = Color(0x2EBCC9C7),            // --color-border: rgba(188,201,199,.18)
    outline = Color(0xFF87958A),
    hint = Color(0xFF87958A),
    warning = Color(0xFFFFB955),
    onWarning = Color(0xFF3B2A00),
    error = Color(0xFFFFB4AB),             // --color-error: Soft Coral
    onError = Color(0xFF690005),
    success = Color(0xFF7CDC96),
    onSuccess = Color(0xFF00391B),
    amber = Color(0xFFE8C26B),
    onAmber = Color(0xFF3F2E00),
    disable = Color(0xFF2E3330),
    onDisable = Color(0xFF5B615C),
)

internal val localSPColorScheme = staticCompositionLocalOf { darkColors }
