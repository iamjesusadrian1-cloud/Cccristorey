package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = RoyalBlue,
    onPrimary = PureWhite,
    primaryContainer = SkyTint,
    onPrimaryContainer = DeepNavy,
    secondary = SacredGold,
    onSecondary = DeepNavy,
    secondaryContainer = WarmGoldContainer,
    onSecondaryContainer = DeepBronze,
    tertiary = SanctuaryBlue,
    onTertiary = PureWhite,
    background = SoftIvory,
    onBackground = TextPrimaryDark,
    surface = PureWhite,
    onSurface = TextPrimaryDark,
    surfaceVariant = SubtleMist,
    onSurfaceVariant = TextSecondarySlate,
    outline = BorderSlate,
    error = LiveCrimson,
    onError = PureWhite,
    errorContainer = LiveCrimsonContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = BrightGold,
    onPrimary = DeepNavy,
    primaryContainer = DarkSanctuaryCard,
    onPrimaryContainer = PureWhite,
    secondary = SacredGold,
    onSecondary = DeepNavy,
    secondaryContainer = DeepBronze,
    onSecondaryContainer = WarmGoldContainer,
    tertiary = SanctuaryBlue,
    onTertiary = PureWhite,
    background = DarkSanctuaryBg,
    onBackground = PureWhite,
    surface = DarkSanctuarySurface,
    onSurface = PureWhite,
    surfaceVariant = DarkSanctuaryCard,
    onSurfaceVariant = BorderSlate,
    outline = DarkBorderSlate,
    error = LiveCrimson,
    onError = PureWhite
)

private val CristoReyShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun CristoReyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CristoReyTypography,
        shapes = CristoReyShapes,
        content = content
    )
}
