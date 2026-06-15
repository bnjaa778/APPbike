package com.example.appbike.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = BikeMint,
    onPrimary = BikeGreenDark,
    primaryContainer = BikeGreen,
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFAFCDBE),
    background = BikeDarkSurface,
    surface = BikeDarkSurface,
    surfaceVariant = BikeDarkContainer,
    onBackground = Color(0xFFF0F5F1),
    onSurface = Color(0xFFF0F5F1)
)

private val LightColorScheme = lightColorScheme(
    primary = BikeGreen,
    onPrimary = Color.White,
    primaryContainer = BikeMint,
    onPrimaryContainer = BikeGreenDark,
    secondary = BikeSlate,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE9E1),
    onSecondaryContainer = BikeInk,
    tertiary = Color(0xFF9A5B25),
    background = BikeSand,
    surface = BikeSurface,
    surfaceVariant = Color(0xFFE8ECE8),
    surfaceContainer = Color(0xFFF0F1EC),
    surfaceContainerHigh = Color(0xFFE8EAE4),
    onBackground = BikeInk,
    onSurface = BikeInk,
    outline = Color(0xFF7A8982)
)

@Composable
fun APPbikeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
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

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as android.app.Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()

            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
