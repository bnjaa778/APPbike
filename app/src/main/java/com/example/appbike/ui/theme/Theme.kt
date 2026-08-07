package com.example.appbike.ui.theme

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AppPrimary,
    onPrimary = Color(0xFF002113),
    primaryContainer = AppPrimarySoft,
    onPrimaryContainer = AppPrimaryBright,
    secondary = AppAccentBlue,
    onSecondary = AppBackground,
    secondaryContainer = Color(0x242FA8FF),
    onSecondaryContainer = Color(0xFFBDE5FF),
    tertiary = AppAccentAmber,
    onTertiary = Color(0xFF261A00),
    tertiaryContainer = Color(0x24FFC857),
    onTertiaryContainer = Color(0xFFFFE3A3),
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
    primary = BikeGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6F7E4),
    onPrimaryContainer = BikeGreenDeep,
    secondary = Color(0xFF176B8A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD5F0FF),
    onSecondaryContainer = BikeInk,
    tertiary = Color(0xFF8A6400),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE7AE),
    onTertiaryContainer = Color(0xFF2B1D00),
    background = Color(0xFFF5F3E9),
    onBackground = BikeInk,
    surface = BikeLightSurface,
    onSurface = BikeInk,
    surfaceVariant = Color(0xFFE4ECE6),
    onSurfaceVariant = Color(0xFF46564D),
    surfaceContainer = Color(0xFFF0F4EC),
    surfaceContainerHigh = Color(0xFFE7EEE7),
    surfaceContainerHighest = Color(0xFFDDE8E0),
    outline = Color(0xFF617369),
    outlineVariant = Color(0xFFBFCFC4)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun APPbikeTheme(
    darkTheme: Boolean = true,
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
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
