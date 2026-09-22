package com.cryptora.securechat.core.designsystem

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = CryptoraColors.ElectricCyan,
    onPrimary = CryptoraColors.DeepNavyBackground,
    primaryContainer = CryptoraColors.SurfaceElevated,
    onPrimaryContainer = CryptoraColors.ElectricCyan,
    secondary = CryptoraColors.CobaltBlue,
    onSecondary = CryptoraColors.TextPrimary,
    background = CryptoraColors.DeepNavyBackground,
    onBackground = CryptoraColors.TextPrimary,
    surface = CryptoraColors.SurfaceNavy,
    onSurface = CryptoraColors.TextPrimary,
    surfaceVariant = CryptoraColors.SurfaceElevated,
    onSurfaceVariant = CryptoraColors.TextSecondary,
    outline = CryptoraColors.BorderSubtle,
    error = CryptoraColors.CoralRevoked,
    onError = CryptoraColors.TextPrimary
)

@Composable
fun CryptoraTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CryptoraTypography,
        content = content
    )
}
