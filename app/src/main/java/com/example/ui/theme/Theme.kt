package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ArcadeDarkColorScheme = darkColorScheme(
    primary = CoralPop,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4A1B2B),
    onPrimaryContainer = Color(0xFFFFD9DE),
    secondary = MintGlow,
    onSecondary = DeepIndigo,
    secondaryContainer = Color(0xFF144239),
    onSecondaryContainer = Color(0xFFC5FFF0),
    tertiary = ElectricGold,
    onTertiary = DeepIndigo,
    background = DeepIndigo,
    onBackground = WarmCream,
    surface = MidnightCard,
    onSurface = WarmCream,
    surfaceVariant = Color(0xFF332D5E),
    onSurfaceVariant = SoftLavender,
    outline = Color(0xFF534B88)
)

@Composable
fun PowerFlipTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ArcadeDarkColorScheme,
        typography = AppTypography,
        content = content
    )
}
