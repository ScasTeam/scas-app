package com.bammm.scas_app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary      = Primary,
    secondary    = Secondary,
    background   = Background,
    surface      = Surface,
    onPrimary    = OnPrimary,
    onBackground = TextPrimary,
    onSurface    = TextPrimary,
    error        = Danger
)

private val DarkColors = darkColorScheme(
    primary      = PrimaryLight,
    secondary    = Secondary,
    background   = BackgroundDark,
    surface      = SurfaceDark,
    onBackground = Background,
    onSurface    = Background,
    error        = Danger
)

@Composable
fun ScasTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        typography  = Typography,
        content     = content
    )
}