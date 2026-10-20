package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = ElectricIndigoLight,
    onPrimary = VoidBlack,
    primaryContainer = Carbon,
    onPrimaryContainer = PureWhite,
    secondary = Silver,
    onSecondary = VoidBlack,
    tertiary = UrgencyNeutralBlue,
    background = VoidBlack,
    onBackground = Pearl,
    surface = Graphite,
    onSurface = Pearl,
    surfaceVariant = Carbon,
    onSurfaceVariant = Silver,
    outline = CardBorderDark,
    outlineVariant = Slate
)

private val LightColorScheme = lightColorScheme(
    primary = VoidBlack,
    onPrimary = PureWhite,
    primaryContainer = Pearl,
    onPrimaryContainer = VoidBlack,
    secondary = Carbon,
    onSecondary = PureWhite,
    tertiary = ElectricIndigo,
    background = PureWhite,
    onBackground = VoidBlack,
    surface = PureWhite,
    onSurface = VoidBlack,
    surfaceVariant = SurfaceSubtle,
    onSurfaceVariant = Steel,
    outline = CardBorderLight,
    outlineVariant = Mist
)

@Composable
fun FounderSyncTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Preserve bespoke brand palette
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
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
