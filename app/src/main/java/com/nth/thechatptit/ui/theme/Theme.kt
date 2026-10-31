package com.nth.thechatptit.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.nth.thechatptit.data.model.ThemeMode

private val KaiWalkerColorScheme = darkColorScheme(
    primary = Orange,
    onPrimary = OnOrange,
    primaryContainer = OrangeContainer,
    onPrimaryContainer = OnOrangeContainer,
    secondary = Orange,
    onSecondary = OnOrange,
    secondaryContainer = OrangeContainer,
    onSecondaryContainer = OnOrangeContainer,
    tertiary = GreenSuccess,
    onTertiary = OnOrange,
    tertiaryContainer = GreenContainer,
    onTertiaryContainer = OnGreenContainer,
    error = ErrorRed,
    onError = OnOrange,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    background = NightBackground,
    onBackground = NightOnBackground,
    surface = NightSurface,
    onSurface = NightOnSurface,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = NightOnSurfaceVariant,
    outline = NightOutline,
    inverseOnSurface = InverseNight,
    inverseSurface = InverseSurface,
    inversePrimary = Orange,
    surfaceTint = Orange,
    outlineVariant = NightOutlineVariant,
    scrim = BlackScrim
)

private val KaiWalkerLightColorScheme = lightColorScheme(
    primary = Orange,
    onPrimary = Color.White,
    primaryContainer = OrangeContainer,
    onPrimaryContainer = OnOrangeContainer,
    secondary = Orange,
    onSecondary = Color.White,
    secondaryContainer = OrangeContainer,
    onSecondaryContainer = OnOrangeContainer,
    tertiary = GreenSuccess,
    onTertiary = Color.Black,
    error = ErrorRed,
    background = Color(0xFFFFFBFF),
    onBackground = Color(0xFF1B1B1B),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF1B1B1B),
    surfaceVariant = Color(0xFFF4DED5),
    onSurfaceVariant = Color(0xFF51443F),
    outline = Color(0xFF84746D),
    outlineVariant = Color(0xFFD7C2BB)
)

@Composable
fun AppTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (darkTheme) KaiWalkerColorScheme else KaiWalkerLightColorScheme,
        typography = Typography,
        content = content
    )
}
