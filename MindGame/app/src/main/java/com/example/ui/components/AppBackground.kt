package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.ui.theme.AppThemeStyle

@Composable
fun AppBackground(
    themeStyle: AppThemeStyle,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(getThemeBaseBackground(themeStyle))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            when (themeStyle) {
                AppThemeStyle.SNOW_WHITE -> drawSnowWhiteBackground()
                AppThemeStyle.DARK -> drawDarkBackground()
                AppThemeStyle.MECHANICAL -> drawMechanicalBackground()
                AppThemeStyle.CUTE -> drawCuteBackground()
                AppThemeStyle.SUNNY -> drawSunnyBackground()
                AppThemeStyle.CORPORATE -> drawCorporateBackground()
                AppThemeStyle.CASUAL -> drawCasualBackground()
            }
        }
        content()
    }
}

private fun getThemeBaseBackground(themeStyle: AppThemeStyle): Brush {
    return when (themeStyle) {
        AppThemeStyle.SNOW_WHITE -> Brush.verticalGradient(
            colors = listOf(Color(0xFFF8FAFC), Color(0xFFEDF4FC), Color(0xFFE2E8F0))
        )
        AppThemeStyle.DARK -> Brush.verticalGradient(
            colors = listOf(Color(0xFF0F0F17), Color(0xFF161626), Color(0xFF1B1B2F))
        )
        AppThemeStyle.MECHANICAL -> Brush.verticalGradient(
            colors = listOf(Color(0xFF18181B), Color(0xFF202025), Color(0xFF27272A))
        )
        AppThemeStyle.CUTE -> Brush.verticalGradient(
            colors = listOf(Color(0xFFFFF0F5), Color(0xFFFFE4EC), Color(0xFFFCE7F3))
        )
        AppThemeStyle.SUNNY -> Brush.verticalGradient(
            colors = listOf(Color(0xFFFFFBEB), Color(0xFFFEF3C7), Color(0xFFFDE68A))
        )
        AppThemeStyle.CORPORATE -> Brush.verticalGradient(
            colors = listOf(Color(0xFFF8FAFC), Color(0xFFEBF3FC), Color(0xFFE2E8F0))
        )
        AppThemeStyle.CASUAL -> Brush.verticalGradient(
            colors = listOf(Color(0xFFF0FDF4), Color(0xFFDCFCE7), Color(0xFFD1FAE5))
        )
    }
}

private fun DrawScope.drawSnowWhiteBackground() {
    val w = size.width
    val h = size.height

    // Subtle ethereal soft cyan & blue ambient orbs
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x3338BDF8), Color.Transparent),
            center = Offset(w * 0.85f, h * 0.15f),
            radius = w * 0.6f
        ),
        center = Offset(w * 0.85f, h * 0.15f),
        radius = w * 0.6f
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x22818CF8), Color.Transparent),
            center = Offset(w * 0.1f, h * 0.7f),
            radius = w * 0.5f
        ),
        center = Offset(w * 0.1f, h * 0.7f),
        radius = w * 0.5f
    )

    // Crisp micro dot grid for clean precision feel
    val step = 32f
    var x = 16f
    while (x < w) {
        var y = 16f
        while (y < h) {
            drawCircle(
                color = Color(0x18334155),
                radius = 1.2f,
                center = Offset(x, y)
            )
            y += step
        }
        x += step
    }
}

private fun DrawScope.drawDarkBackground() {
    val w = size.width
    val h = size.height

    // Deep purple & electric cyan nebula glows
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x357C3AED), Color.Transparent),
            center = Offset(w * 0.2f, h * 0.2f),
            radius = w * 0.7f
        ),
        center = Offset(w * 0.2f, h * 0.2f),
        radius = w * 0.7f
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x300284C7), Color.Transparent),
            center = Offset(w * 0.85f, h * 0.65f),
            radius = w * 0.65f
        ),
        center = Offset(w * 0.85f, h * 0.65f),
        radius = w * 0.65f
    )

    // Faint constellation / circuit glowing points
    val stars = listOf(
        Offset(w * 0.15f, h * 0.1f) to 2f,
        Offset(w * 0.75f, h * 0.08f) to 2.5f,
        Offset(w * 0.88f, h * 0.35f) to 1.8f,
        Offset(w * 0.12f, h * 0.5f) to 2.2f,
        Offset(w * 0.8f, h * 0.82f) to 2f,
        Offset(w * 0.3f, h * 0.9f) to 1.6f
    )
    for ((pos, r) in stars) {
        drawCircle(
            color = Color(0x60D8B4FE),
            radius = r * 2.5f,
            center = pos
        )
        drawCircle(
            color = Color(0xCCFFFFFF),
            radius = r,
            center = pos
        )
    }
}

private fun DrawScope.drawMechanicalBackground() {
    val w = size.width
    val h = size.height

    // Amber cyber industrial glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x30D97706), Color.Transparent),
            center = Offset(w * 0.9f, h * 0.2f),
            radius = w * 0.55f
        ),
        center = Offset(w * 0.9f, h * 0.2f),
        radius = w * 0.55f
    )

    // Tech diagonal grid lines & angular accents
    val step = 48f
    var x = -h
    while (x < w + h) {
        drawLine(
            color = Color(0x12F59E0B),
            start = Offset(x, 0f),
            end = Offset(x + h, h),
            strokeWidth = 1.2f
        )
        x += step
    }

    // Corner rivet & tech marks
    val path = Path().apply {
        moveTo(24f, 40f)
        lineTo(48f, 40f)
        lineTo(48f, 64f)
    }
    drawPath(path, color = Color(0x40F59E0B), style = Stroke(width = 2f))
}

private fun DrawScope.drawCuteBackground() {
    val w = size.width
    val h = size.height

    // Soft pastel strawberry & lavender dreamy floating bubbles
    val bubbles = listOf(
        Triple(Offset(w * 0.15f, h * 0.12f), w * 0.22f, Color(0x30F472B6)),
        Triple(Offset(w * 0.85f, h * 0.28f), w * 0.28f, Color(0x28C084FC)),
        Triple(Offset(w * 0.1f, h * 0.65f), w * 0.3f, Color(0x22FBCFE8)),
        Triple(Offset(w * 0.82f, h * 0.85f), w * 0.24f, Color(0x30F43F5E))
    )

    for ((center, radius, color) in bubbles) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color, Color.Transparent),
                center = center,
                radius = radius
            ),
            center = center,
            radius = radius
        )
    }

    // Soft sparkles / decorative dots
    val sparkles = listOf(
        Offset(w * 0.7f, h * 0.12f),
        Offset(w * 0.25f, h * 0.42f),
        Offset(w * 0.85f, h * 0.52f),
        Offset(w * 0.35f, h * 0.82f)
    )
    for (sp in sparkles) {
        drawCircle(
            color = Color(0x40EC4899),
            radius = 4f,
            center = sp
        )
    }
}

private fun DrawScope.drawSunnyBackground() {
    val w = size.width
    val h = size.height

    // Radiant sunburst center in top right
    val sunCenter = Offset(w * 0.85f, h * 0.1f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x55F59E0B), Color(0x20FBBF24), Color.Transparent),
            center = sunCenter,
            radius = w * 0.8f
        ),
        center = sunCenter,
        radius = w * 0.8f
    )

    // Concentric calm ripple rings
    val rings = listOf(w * 0.3f, w * 0.5f, w * 0.7f, w * 0.9f)
    for (r in rings) {
        drawCircle(
            color = Color(0x1CF59E0B),
            radius = r,
            center = sunCenter,
            style = Stroke(width = 1.5f)
        )
    }
}

private fun DrawScope.drawCorporateBackground() {
    val w = size.width
    val h = size.height

    // Executive Navy / Slate clean geometric structure
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x281E3A8A), Color.Transparent),
            center = Offset(w * 0.1f, h * 0.15f),
            radius = w * 0.6f
        ),
        center = Offset(w * 0.1f, h * 0.15f),
        radius = w * 0.6f
    )

    // Subtle modern geometric blueprint polygons
    val path = Path().apply {
        moveTo(w * 0.6f, 0f)
        lineTo(w, 0f)
        lineTo(w, h * 0.35f)
        close()
    }
    drawPath(
        path = path,
        brush = Brush.linearGradient(
            colors = listOf(Color(0x153B82F6), Color(0x051D4ED8)),
            start = Offset(w * 0.6f, 0f),
            end = Offset(w, h * 0.35f)
        )
    )

    // Fine structural grid lines
    for (i in 1..4) {
        val y = h * (i * 0.2f)
        drawLine(
            color = Color(0x100F172A),
            start = Offset(0f, y),
            end = Offset(w, y),
            strokeWidth = 1f
        )
    }
}

private fun DrawScope.drawCasualBackground() {
    val w = size.width
    val h = size.height

    // Calming emerald/mint zen waves and ambient organic glows
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x3510B981), Color.Transparent),
            center = Offset(w * 0.8f, h * 0.25f),
            radius = w * 0.65f
        ),
        center = Offset(w * 0.8f, h * 0.25f),
        radius = w * 0.65f
    )

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x28059669), Color.Transparent),
            center = Offset(w * 0.15f, h * 0.75f),
            radius = w * 0.55f
        ),
        center = Offset(w * 0.15f, h * 0.75f),
        radius = w * 0.55f
    )

    // Organic wavy contours
    val wavePath = Path().apply {
        moveTo(0f, h * 0.4f)
        cubicTo(w * 0.35f, h * 0.35f, w * 0.65f, h * 0.45f, w, h * 0.38f)
        lineTo(w, h * 0.5f)
        cubicTo(w * 0.65f, h * 0.57f, w * 0.35f, h * 0.47f, 0f, h * 0.52f)
        close()
    }
    drawPath(
        path = wavePath,
        color = Color(0x12059669)
    )
}
