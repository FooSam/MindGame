package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SnowWhiteColorScheme = lightColorScheme(
    primary = SnowWhitePrimary,
    onPrimary = SnowWhiteOnPrimary,
    primaryContainer = SnowWhitePrimaryContainer,
    onPrimaryContainer = SnowWhiteOnPrimaryContainer,
    secondary = SnowWhiteSecondary,
    tertiary = SnowWhiteTertiary,
    background = SnowWhiteBg,
    surface = SnowWhiteSurface,
    surfaceVariant = SnowWhiteCardBg,
    onBackground = SnowWhiteOnBg,
    onSurface = SnowWhiteOnBg
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    tertiary = DarkTertiary,
    background = DarkBg,
    surface = DarkSurface,
    surfaceVariant = DarkCardBg,
    onBackground = DarkOnBg,
    onSurface = DarkOnBg
)

private val MechanicalColorScheme = darkColorScheme(
    primary = MechPrimary,
    onPrimary = MechOnPrimary,
    primaryContainer = MechPrimaryContainer,
    onPrimaryContainer = MechOnPrimaryContainer,
    secondary = MechSecondary,
    tertiary = MechTertiary,
    background = MechBg,
    surface = MechSurface,
    surfaceVariant = MechCardBg,
    onBackground = MechOnBg,
    onSurface = MechOnBg
)

private val CuteColorScheme = lightColorScheme(
    primary = CutePrimary,
    onPrimary = CuteOnPrimary,
    primaryContainer = CutePrimaryContainer,
    onPrimaryContainer = CuteOnPrimaryContainer,
    secondary = CuteSecondary,
    tertiary = CuteTertiary,
    background = CuteBg,
    surface = CuteSurface,
    surfaceVariant = CuteCardBg,
    onBackground = CuteOnBg,
    onSurface = CuteOnBg
)

private val SunnyColorScheme = lightColorScheme(
    primary = SunnyPrimary,
    onPrimary = SunnyOnPrimary,
    primaryContainer = SunnyPrimaryContainer,
    onPrimaryContainer = SunnyOnPrimaryContainer,
    secondary = SunnySecondary,
    tertiary = SunnyTertiary,
    background = SunnyBg,
    surface = SunnySurface,
    surfaceVariant = SunnyCardBg,
    onBackground = SunnyOnBg,
    onSurface = SunnyOnBg
)

private val CorporateColorScheme = lightColorScheme(
    primary = CorpPrimary,
    onPrimary = CorpOnPrimary,
    primaryContainer = CorpPrimaryContainer,
    onPrimaryContainer = CorpOnPrimaryContainer,
    secondary = CorpSecondary,
    tertiary = CorpTertiary,
    background = CorpBg,
    surface = CorpSurface,
    surfaceVariant = CorpCardBg,
    onBackground = CorpOnBg,
    onSurface = CorpOnBg
)

private val CasualColorScheme = lightColorScheme(
    primary = CasualPrimary,
    onPrimary = CasualOnPrimary,
    primaryContainer = CasualPrimaryContainer,
    onPrimaryContainer = CasualOnPrimaryContainer,
    secondary = CasualSecondary,
    tertiary = CasualTertiary,
    background = CasualBg,
    surface = CasualSurface,
    surfaceVariant = CasualCardBg,
    onBackground = CasualOnBg,
    onSurface = CasualOnBg
)

@Composable
fun MyApplicationTheme(
    themeStyle: AppThemeStyle = AppThemeStyle.SNOW_WHITE,
    content: @Composable () -> Unit
) {
    val colorScheme: ColorScheme = when (themeStyle) {
        AppThemeStyle.SNOW_WHITE -> SnowWhiteColorScheme
        AppThemeStyle.DARK -> DarkColorScheme
        AppThemeStyle.MECHANICAL -> MechanicalColorScheme
        AppThemeStyle.CUTE -> CuteColorScheme
        AppThemeStyle.SUNNY -> SunnyColorScheme
        AppThemeStyle.CORPORATE -> CorporateColorScheme
        AppThemeStyle.CASUAL -> CasualColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
