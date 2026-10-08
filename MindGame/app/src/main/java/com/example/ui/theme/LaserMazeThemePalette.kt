package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 《雷射迷宮》全風格自適應色彩與材質調色盤 (Theme Harmony Palette)
 */
data class LaserMazePalette(
    val boardBackgroundGradient: List<Color>,
    val boardBorderColor: Color,
    val boardInnerBorderColor: Color,
    val slotBackground: Color,
    val slotBorder: Color,
    val slotGuideDot: Color,
    val laserDefaultGlow: Color,
    val laserCore: Color = Color.White,
    val miniMapBackground: Color,
    val miniMapBorder: Color,
    val miniMapGridLine: Color,
    val miniMapViewport: Color,
    val briefingCardBg: Color,
    val onBriefingCardText: Color,
    val accentColor: Color,
    val isLightStyle: Boolean
)

object LaserMazeThemePalettes {

    fun get(theme: AppThemeStyle): LaserMazePalette {
        return when (theme) {
            AppThemeStyle.SNOW_WHITE -> LaserMazePalette(
                boardBackgroundGradient = listOf(Color(0xFFF8FAFC), Color(0xFFF1F5F9), Color(0xFFE2E8F0)),
                boardBorderColor = Color(0xFF38BDF8),
                boardInnerBorderColor = Color(0xFF94A3B8),
                slotBackground = Color(0xFFFFFFFF),
                slotBorder = Color(0xFFCBD5E1),
                slotGuideDot = Color(0xFFE2E8F0),
                laserDefaultGlow = Color(0xFF0284C7),
                miniMapBackground = Color(0xFFF1F5F9).copy(alpha = 0.92f),
                miniMapBorder = Color(0xFF0284C7),
                miniMapGridLine = Color(0xFFCBD5E1).copy(alpha = 0.6f),
                miniMapViewport = Color(0xFF0284C7),
                briefingCardBg = Color(0xFFFFFFFF),
                onBriefingCardText = Color(0xFF0F172A),
                accentColor = Color(0xFF0284C7),
                isLightStyle = true
            )

            AppThemeStyle.DARK -> LaserMazePalette(
                boardBackgroundGradient = listOf(Color(0xFF0A0A12), Color(0xFF0F111E), Color(0xFF141726)),
                boardBorderColor = Color(0xFF00E5FF),
                boardInnerBorderColor = Color(0xFF1E293B),
                slotBackground = Color(0xFF141724),
                slotBorder = Color(0xFF23283E),
                slotGuideDot = Color(0xFF333B58),
                laserDefaultGlow = Color(0xFF00E5FF),
                miniMapBackground = Color(0xFF0A0A12).copy(alpha = 0.92f),
                miniMapBorder = Color(0xFF00E5FF),
                miniMapGridLine = Color(0xFF1E293B).copy(alpha = 0.6f),
                miniMapViewport = Color(0xFFFF5252),
                briefingCardBg = Color(0xFF161626),
                onBriefingCardText = Color(0xFFF1F5F9),
                accentColor = Color(0xFF00E5FF),
                isLightStyle = false
            )

            AppThemeStyle.MECHANICAL -> LaserMazePalette(
                boardBackgroundGradient = listOf(Color(0xFF1E1E24), Color(0xFF25252E), Color(0xFF2C2C36)),
                boardBorderColor = Color(0xFFEAB308),
                boardInnerBorderColor = Color(0xFF713F12),
                slotBackground = Color(0xFF18181D),
                slotBorder = Color(0xFF3F3F46),
                slotGuideDot = Color(0xFFCA8A04),
                laserDefaultGlow = Color(0xFFFF5722),
                miniMapBackground = Color(0xFF1E1E24).copy(alpha = 0.92f),
                miniMapBorder = Color(0xFFEAB308),
                miniMapGridLine = Color(0xFF52525B).copy(alpha = 0.6f),
                miniMapViewport = Color(0xFFEAB308),
                briefingCardBg = Color(0xFF27272A),
                onBriefingCardText = Color(0xFFF4F4F5),
                accentColor = Color(0xFFEAB308),
                isLightStyle = false
            )

            AppThemeStyle.CUTE -> LaserMazePalette(
                boardBackgroundGradient = listOf(Color(0xFFFFF0F5), Color(0xFFFFE4EC), Color(0xFFFCE7F3)),
                boardBorderColor = Color(0xFFF472B6),
                boardInnerBorderColor = Color(0xFFFBCFE8),
                slotBackground = Color(0xFFFFFFFF),
                slotBorder = Color(0xFFFBCFE8),
                slotGuideDot = Color(0xFFF9A8D4),
                laserDefaultGlow = Color(0xFFEC4899),
                miniMapBackground = Color(0xFFFFF0F5).copy(alpha = 0.92f),
                miniMapBorder = Color(0xFFF472B6),
                miniMapGridLine = Color(0xFFFBCFE8).copy(alpha = 0.6f),
                miniMapViewport = Color(0xFFDB2777),
                briefingCardBg = Color(0xFFFFF0F5),
                onBriefingCardText = Color(0xFF831843),
                accentColor = Color(0xFFEC4899),
                isLightStyle = true
            )

            AppThemeStyle.SUNNY -> LaserMazePalette(
                boardBackgroundGradient = listOf(Color(0xFFFFFBEB), Color(0xFFFEF3C7), Color(0xFFFDE68A)),
                boardBorderColor = Color(0xFFF59E0B),
                boardInnerBorderColor = Color(0xFFFBBF24),
                slotBackground = Color(0xFFFFFFFF),
                slotBorder = Color(0xFFFDE68A),
                slotGuideDot = Color(0xFFF59E0B),
                laserDefaultGlow = Color(0xFFF59E0B),
                miniMapBackground = Color(0xFFFFFBEB).copy(alpha = 0.92f),
                miniMapBorder = Color(0xFFF59E0B),
                miniMapGridLine = Color(0xFFFDE68A).copy(alpha = 0.6f),
                miniMapViewport = Color(0xFFD97706),
                briefingCardBg = Color(0xFFFFFBEB),
                onBriefingCardText = Color(0xFF78350F),
                accentColor = Color(0xFFF59E0B),
                isLightStyle = true
            )

            AppThemeStyle.CORPORATE -> LaserMazePalette(
                boardBackgroundGradient = listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF334155)),
                boardBorderColor = Color(0xFF3B82F6),
                boardInnerBorderColor = Color(0xFF60A5FA),
                slotBackground = Color(0xFF1E293B),
                slotBorder = Color(0xFF334155),
                slotGuideDot = Color(0xFF64748B),
                laserDefaultGlow = Color(0xFF2563EB),
                miniMapBackground = Color(0xFF0F172A).copy(alpha = 0.92f),
                miniMapBorder = Color(0xFF3B82F6),
                miniMapGridLine = Color(0xFF334155).copy(alpha = 0.6f),
                miniMapViewport = Color(0xFF60A5FA),
                briefingCardBg = Color(0xFF1E293B),
                onBriefingCardText = Color(0xFFF8FAFC),
                accentColor = Color(0xFF3B82F6),
                isLightStyle = false
            )

            AppThemeStyle.CASUAL -> LaserMazePalette(
                boardBackgroundGradient = listOf(Color(0xFFF0FDF4), Color(0xFFDCFCE7), Color(0xFFD1FAE5)),
                boardBorderColor = Color(0xFF10B981),
                boardInnerBorderColor = Color(0xFF34D399),
                slotBackground = Color(0xFFFFFFFF),
                slotBorder = Color(0xFFA7F3D0),
                slotGuideDot = Color(0xFF6EE7B7),
                laserDefaultGlow = Color(0xFF10B981),
                miniMapBackground = Color(0xFFF0FDF4).copy(alpha = 0.92f),
                miniMapBorder = Color(0xFF10B981),
                miniMapGridLine = Color(0xFFA7F3D0).copy(alpha = 0.6f),
                miniMapViewport = Color(0xFF059669),
                briefingCardBg = Color(0xFFF0FDF4),
                onBriefingCardText = Color(0xFF064E3B),
                accentColor = Color(0xFF10B981),
                isLightStyle = true
            )
        }
    }
}
