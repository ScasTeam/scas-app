package com.bammm.scas_app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ScasPrimary,
    onPrimary = ScasOnPrimary,
    secondary = ScasSecondary,
    onSecondary = ScasOnSecondary,
    background = ScasSurfaceDark,
    onBackground = ScasOnSurfaceDark,
    surface = ScasSurfaceDark,
    onSurface = ScasOnSurfaceDark,
    surfaceVariant = ScasSurfaceVariantDark,
    onSurfaceVariant = ScasOnSurfaceVariantDark,
    error = ScasError,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = ScasPrimary,
    onPrimary = ScasOnPrimary,
    secondary = ScasSecondary,
    onSecondary = ScasOnSecondary,
    background = ScasSurfaceLight,
    onBackground = ScasOnSurfaceLight,
    surface = ScasSurfaceLight,
    onSurface = ScasOnSurfaceLight,
    surfaceVariant = ScasSurfaceVariantLight,
    onSurfaceVariant = ScasOnSurfaceVariantLight,
    error = ScasError,
    onError = Color.White
)

@Composable
fun ScasappTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep dynamic color support but fallback to our brand colors
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Set status bar and navigation bar colors if possible
    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}