package com.nth.thechatptit.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

val Orange = Color(0xFFFC4C02)
val OnOrange = Color(0xFF000000)
val OrangeContainer = Color(0xFF691E00)
val OnOrangeContainer = Color(0xFFFFDBCC)

val NightBackground = Color(0xFF111111)
val NightOnBackground = Color(0xFFF5F5F5)
val NightSurface = Color(0xFF1A1A1A)
val NightOnSurface = Color(0xFFF5F5F5)
val NightSurfaceVariant = Color(0xFF222222)
val NightOnSurfaceVariant = Color(0xFFAAAAAA)
val NightOutline = Color(0xFF5A5A5A)
val NightOutlineVariant = Color(0xFF3A3A3A)

val GreenContainer = Color(0xFF005127)
val OnGreenContainer = Color(0xFFBFF5D8)
val ErrorContainer = Color(0xFF8F1A1D)
val OnErrorContainer = Color(0xFFFFDAD6)
val InverseNight = Color(0xFF111111)
val InverseSurface = Color(0xFFEDEDED)
val BlackScrim = Color(0xFF000000)

val GreenSuccess = Color(0xFF35D07F)
val WarningOrange = Color(0xFFF5A623)
val ErrorRed = Color(0xFFFF5A5F)
val InfoBlue = Color(0xFF57A9FF)

val ColorScheme.success: Color
    get() = GreenSuccess

val ColorScheme.warning: Color
    get() = WarningOrange

val ColorScheme.info: Color
    get() = InfoBlue

@Composable
fun ColorScheme.compositedOnSurface(alpha: Float): Color {
    return onSurface.copy(alpha = alpha).compositeOver(surface)
}
