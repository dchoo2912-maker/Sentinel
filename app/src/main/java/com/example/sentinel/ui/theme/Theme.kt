package com.example.sentinel.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.sentinel.SentinelColors

private val DarkColorScheme = darkColorScheme(
    primary = SentinelColors.Blue600,
    secondary = SentinelColors.Slate400,
    tertiary = SentinelColors.Green300,
    background = SentinelColors.Blue950,
    surface = SentinelColors.Slate900,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = SentinelColors.Blue950,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = SentinelColors.Blue600,
    secondary = SentinelColors.Slate400,
    tertiary = SentinelColors.Green600,
    background = Color.White,
    surface = SentinelColors.Slate50,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = SentinelColors.Slate900,
    onSurface = SentinelColors.Slate900
)

@Composable
fun SentinelTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
