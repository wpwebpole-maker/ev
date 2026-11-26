package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val RaagLightColorScheme = lightColorScheme(
    primary = RaagCrimson,
    onPrimary = RaagWhite,
    primaryContainer = RaagSurfaceVariant,
    onPrimaryContainer = RaagDarkRed,
    secondary = RaagGold,
    onSecondary = RaagDarkRed,
    secondaryContainer = RaagGoldSoft,
    onSecondaryContainer = RaagDarkRed,
    tertiary = RaagDarkRed,
    onTertiary = RaagWhite,
    background = RaagBackground,
    onBackground = RaagInk,
    surface = RaagSurface,
    onSurface = RaagInk,
    surfaceVariant = RaagSurfaceVariant,
    onSurfaceVariant = RaagMuted,
    outline = RaagBorder,
    error = StatusDangerInk,
    onError = RaagWhite,
    errorContainer = StatusDangerBg,
    onErrorContainer = StatusDangerInk
)

@Composable
fun RaagInsuranceTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RaagLightColorScheme,
        typography = Typography,
        content = content
    )
}
