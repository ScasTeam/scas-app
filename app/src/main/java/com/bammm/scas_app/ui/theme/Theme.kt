package com.bammm.scas_app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext

private val DarkColors = darkColorScheme(
    primary = Primary,              // White
    onPrimary = PrimaryDark,        // Black
    secondary = Secondary,          // Muted Gray
    background = BackgroundDark,    // #171717
    surface = SurfaceDark,          // #242424
    onBackground = Primary,         // White
    onSurface = Primary,            // White
    error = Danger,                 // Deep Crimson
    onError = Color.White
)

private val LightColors = lightColorScheme(
    primary = PrimaryDark,          // Black
    onPrimary = Primary,            // White
    secondary = Secondary,          // Muted Gray
    background = Background,        // Apple-like off-white
    surface = Surface,              // White card surface
    onBackground = TextPrimary,     // #171717
    onSurface = TextPrimary,        // #171717
    error = Danger,
    onError = Color.White
)

@Composable
fun ScasTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep dynamic color support but fallback to our brand colors
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors

    // Set status bar and navigation bar colors if possible
    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            val window = (view.context as android.app.Activity).window
            window.statusBarColor = colors.background.toArgb()
            window.navigationBarColor = colors.background.toArgb()
            androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography  = Typography,
        content     = content
    )
}