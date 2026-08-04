package com.example.appbike.ui.theme

import android.os.Build
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
    primary = AppPrimary,
    onPrimary = AppTextPrimary,
    primaryContainer = AppPrimarySoft,
    onPrimaryContainer = AppPrimaryBright,
    secondary = AppPrimaryBright,
    onSecondary = AppBackground,
    secondaryContainer = AppPrimarySoft,
    onSecondaryContainer = AppTextPrimary,
    tertiary = AppSuccess,
    onTertiary = AppBackground,
    tertiaryContainer = Color(0x1A55D98B),
    onTertiaryContainer = AppSuccess,
    background = AppBackground,
    onBackground = AppTextPrimary,
    surface = AppSurface,
    onSurface = AppTextPrimary,
    surfaceVariant = AppSurfaceElevated,
    onSurfaceVariant = AppTextSecondary,
    surfaceContainer = AppBackgroundElevated,
    surfaceContainerHigh = AppSurfaceElevated,
    surfaceContainerHighest = AppSurfacePressed,
    outline = AppBorderSubtle,
    outlineVariant = AppBorderSubtle,
    error = AppError,
    onError = AppBackground,
    errorContainer = AppErrorSoft,
    onErrorContainer = AppError
)

private val LightColorScheme = lightColorScheme(
    primary = BikePurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7DFFF),
    onPrimaryContainer = BikePurpleDeep,
    secondary = BikePurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD8ECFF),
    onSecondaryContainer = BikeInk,
    tertiary = Color(0xFF0EA5E9),
    background = Color(0xFFF4F6FF),
    surface = BikeLightSurface,
    surfaceVariant = Color(0xFFE3E8F7),
    surfaceContainer = Color(0xFFEEF2FF),
    surfaceContainerHigh = Color(0xFFE3E9FA),
    surfaceContainerHighest = Color(0xFFD9E2F5),
    onBackground = BikeInk,
    onSurface = BikeInk,
    onSurfaceVariant = Color(0xFF4B5568),
    outline = Color(0xFF6B748A),
    outlineVariant = Color(0xFFC7D0E6)
)

@Composable
fun APPbikeTheme(
    darkTheme: Boolean = true,
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
