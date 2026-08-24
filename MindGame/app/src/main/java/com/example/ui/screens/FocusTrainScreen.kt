package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.data.model.GameDifficulty
import com.example.data.model.Localization
import com.example.data.model.WheelDifficultyConfig
import com.example.ui.components.formatTimeMillis
import com.example.ui.viewmodel.GameStatus
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

// Harmonious Pastel Palette for Wheel Sectors
private val sectorColors = listOf(
    Color(0xFFFFB4BA), // Soft Rose Pink
    Color(0xFFBAE6FD), // Soft Sky Blue
    Color(0xFFBBF7D0), // Soft Mint Green
    Color(0xFFFEF08A), // Soft Lemon Yellow
    Color(0xFFE9D5FF), // Soft Lavender
    Color(0xFFFED7AA), // Soft Peach Orange
    Color(0xFF99F6E4), // Soft Aqua Cyan
    Color(0xFFDDD6FE), // Soft Periwinkle
    Color(0xFFD9F99D), // Soft Lime
    Color(0xFFFECDD3), // Soft Blossom
    Color(0xFFA5F3FC), // Soft Ice Blue
    Color(0xFFFDE68A)  // Soft Butter
)

@Composable
fun FocusTrainScreen(
    difficulty: GameDifficulty,
    gameStatus: GameStatus,
    config: WheelDifficultyConfig,
    numbers: List<Int>,
    currentTarget: Int,
    clearedIndices: Set<Int>,
    elapsedTimeMillis: Long,
    wrongTapIndex: Int?,
    wrongCount: Int,
    lastCompletedTimeMillis: Long?,
    language: AppLanguage,
    onBackClick: () -> Unit,
    onStartClick: () -> Unit,
    onResetClick: () -> Unit,
    onCellClick: (Int) -> Unit,
    onLeaderboardClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Navigation Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                val diffTitleKey = when (difficulty) {
                    GameDifficulty.BEGINNER -> "diff_beginner"
                    GameDifficulty.INTERMEDIATE -> "diff_intermediate"
                    GameDifficulty.ADVANCED -> "diff_advanced"
                    GameDifficulty.HARD -> "diff_hard"
                    GameDifficulty.HELL -> "diff_hell"
                    GameDifficulty.EPIC -> "diff_epic"
                }
                Column {
                    Text(
                        text = Localization.getString("game_focus_train", language),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "${Localization.getString(diffTitleKey, language)} (1~${config.totalNumbers})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            IconButton(onClick = onLeaderboardClick) {
                Icon(
                    imageVector = Icons.Default.Leaderboard,
                    contentDescription = "Leaderboard",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Status Card: Target, Timer, Mistakes
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Current Target
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = Localization.getString("current_target_label", language),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = if (gameStatus == GameStatus.PLAYING) "$currentTarget" else "-",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                // Mistakes Counter
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = Localization.getString("mistakes_label", language),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = "$wrongCount",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (wrongCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                // Timer
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = Localization.getString("time_elapsed_label", language),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = formatTimeMillis(elapsedTimeMillis),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Center Game Board Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (gameStatus == GameStatus.IDLE) {
                // Ready Overlay: Central Start Button & Instructions
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.90f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = Localization.getString("game_focus_train", language),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = Localization.getString("focus_train_ready_hint", language),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onStartClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = Localization.getString("game_start_button", language),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            } else {
                // Active Dynamic Annular Sector Wheel
                RotatingAnnularWheelBoard(
                    difficulty = difficulty,
                    config = config,
                    numbers = numbers,
                    clearedIndices = clearedIndices,
                    wrongTapIndex = wrongTapIndex,
                    isPlaying = gameStatus == GameStatus.PLAYING,
                    onCellClick = onCellClick
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bottom Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Leaderboard Button
            OutlinedButton(
                onClick = onLeaderboardClick,
                shape = RoundedCornerShape(14.dp),
                modifier = if (gameStatus == GameStatus.IDLE) Modifier.fillMaxWidth() else Modifier
            ) {
                Icon(
                    imageVector = Icons.Default.Leaderboard,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = Localization.getString("leaderboard_button", language),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            // Right: Restart Button (Only when playing or completed)
            if (gameStatus != GameStatus.IDLE) {
                Button(
                    onClick = onResetClick,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = Localization.getString("game_reset_button", language),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }

    // Completed Settlement Dialog
    if (gameStatus == GameStatus.COMPLETED && lastCompletedTimeMillis != null) {
        AlertDialog(
            onDismissRequest = onResetClick,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = Localization.getString("game_completed_title", language),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = Localization.getString("record_time_format", language, formatTimeMillis(lastCompletedTimeMillis)),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                    if (wrongCount > 0) {
                        Text(
                            text = "${Localization.getString("mistakes_label", language)}: $wrongCount",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.error)
                        )
                    }
                    Text(
                        text = Localization.getString("record_saved_message", language),
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onStartClick,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(Localization.getString("play_again_button", language))
                }
            },
            dismissButton = {
                TextButton(onClick = onBackClick) {
                    Text(Localization.getString("back_to_menu", language))
                }
            }
        )
    }
}

@Composable
private fun RotatingAnnularWheelBoard(
    difficulty: GameDifficulty,
    config: WheelDifficultyConfig,
    numbers: List<Int>,
    clearedIndices: Set<Int>,
    wrongTapIndex: Int?,
    isPlaying: Boolean,
    onCellClick: (Int) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "AnnularWheelRotation")
    val duration = config.rotationDurationMs

    // Clockwise angle: 0 -> 360
    val cwAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = duration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ClockwiseAngle"
    )

    // Counter-Clockwise angle: 360 -> 0
    val ccwAngle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = duration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "CounterClockwiseAngle"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        val boardSizePx = with(density) { min(maxWidth.toPx(), maxHeight.toPx()) }
        val center = Offset(boardSizePx / 2f, boardSizePx / 2f)
        val maxRadius = boardSizePx * 0.48f

        val ringCount = config.rings.size

        // Radii Partitioning: Inner and Outer radii for each ring
        val (r0, innerRadii, outerRadii) = when (ringCount) {
            1 -> {
                val rCenter = maxRadius * 0.35f
                Triple(rCenter, listOf(rCenter), listOf(maxRadius))
            }
            2 -> {
                val rCenter = maxRadius * 0.24f
                val r1 = maxRadius * 0.60f
                Triple(rCenter, listOf(rCenter, r1), listOf(r1, maxRadius))
            }
            else -> {
                val rCenter = maxRadius * 0.18f
                val r1 = maxRadius * 0.44f
                val r2 = maxRadius * 0.72f
                Triple(rCenter, listOf(rCenter, r1, r2), listOf(r1, r2, maxRadius))
            }
        }

        // Adaptive Font Size
        val fontSizeSp = when {
            difficulty == GameDifficulty.EPIC -> 13.sp
            difficulty == GameDifficulty.HELL -> 15.sp
            difficulty == GameDifficulty.HARD -> 16.sp
            difficulty == GameDifficulty.ADVANCED -> 18.sp
            difficulty == GameDifficulty.INTERMEDIATE -> 20.sp
            else -> 24.sp
        }

        val centerFontSizeSp = when {
            difficulty == GameDifficulty.EPIC -> 18.sp
            difficulty == GameDifficulty.HELL -> 20.sp
            else -> 24.sp
        }

        // Colors
        val defaultBorderColor = Color(0xFF334155).copy(alpha = 0.35f)
        val clearedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        val errorBorderColor = MaterialTheme.colorScheme.error
        val clearedBgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        val errorBgColor = MaterialTheme.colorScheme.errorContainer
        val centerDefaultBgColor = Color(0xFFFFFFFF)

        // 1. Canvas Layer: Draws Annular Sectors & Center Circle
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isPlaying, numbers, clearedIndices) {
                    if (!isPlaying) return@pointerInput
                    detectTapGestures { tapOffset ->
                        val dx = tapOffset.x - center.x
                        val dy = tapOffset.y - center.y
                        val dist = sqrt(dx * dx + dy * dy)

                        // 1. Center Circle check (Index 0)
                        if (dist <= r0) {
                            if (!clearedIndices.contains(0)) {
                                onCellClick(0)
                            }
                            return@detectTapGestures
                        }

                        // 2. Ring check
                        var tapAngleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                        if (tapAngleDeg < 0) tapAngleDeg += 360f

                        var globalIdx = 1
                        for (rIdx in 0 until ringCount) {
                            val rIn = innerRadii[rIdx]
                            val rOut = outerRadii[rIdx]
                            val ringInfo = config.rings[rIdx]
                            val itemCount = ringInfo.itemCount
                            val ringAngle = if (ringInfo.isClockwise) cwAngle else ccwAngle

                            if (dist in rIn..rOut) {
                                var relAngle = (tapAngleDeg - ringAngle) % 360f
                                if (relAngle < 0) relAngle += 360f
                                val sweep = 360f / itemCount
                                val k = (relAngle / sweep).toInt().coerceIn(0, itemCount - 1)
                                val itemIndex = globalIdx + k
                                if (!clearedIndices.contains(itemIndex)) {
                                    onCellClick(itemIndex)
                                }
                                return@detectTapGestures
                            }
                            globalIdx += itemCount
                        }
                    }
                }
        ) {
            // Draw Center Circle
            val isCenterCleared = clearedIndices.contains(0)
            val isCenterWrong = wrongTapIndex == 0
            val centerFill = when {
                isCenterWrong -> errorBgColor
                isCenterCleared -> clearedBgColor
                else -> centerDefaultBgColor
            }
            val centerBorder = when {
                isCenterWrong -> errorBorderColor
                isCenterCleared -> clearedBorderColor
                else -> defaultBorderColor
            }

            drawCircle(
                color = centerFill,
                radius = r0,
                center = center
            )
            drawCircle(
                color = centerBorder,
                radius = r0,
                center = center,
                style = Stroke(width = if (isCenterWrong) 3.dp.toPx() else 2.dp.toPx())
            )

            // Draw Annular Sectors
            var sectorGlobalIdx = 1
            config.rings.forEachIndexed { rIdx, ringInfo ->
                val rIn = innerRadii[rIdx]
                val rOut = outerRadii[rIdx]
                val itemCount = ringInfo.itemCount
                val sweepAngleDeg = 360f / itemCount
                val ringAngle = if (isPlaying) {
                    if (ringInfo.isClockwise) cwAngle else ccwAngle
                } else {
                    0f
                }

                for (k in 0 until itemCount) {
                    val itemIndex = sectorGlobalIdx++
                    val isCleared = clearedIndices.contains(itemIndex)
                    val isWrong = wrongTapIndex == itemIndex

                    val startAngleDeg = (sweepAngleDeg * k) + ringAngle

                    val fillColor = when {
                        isWrong -> errorBgColor
                        isCleared -> clearedBgColor
                        else -> sectorColors[(rIdx * 7 + k) % sectorColors.size]
                    }

                    val borderColor = when {
                        isWrong -> errorBorderColor
                        isCleared -> clearedBorderColor
                        else -> defaultBorderColor
                    }

                    val sectorPath = createAnnularSectorPath(
                        center = center,
                        innerRadius = rIn,
                        outerRadius = rOut,
                        startAngleDeg = startAngleDeg,
                        sweepAngleDeg = sweepAngleDeg
                    )

                    drawPath(
                        path = sectorPath,
                        color = fillColor
                    )
                    drawPath(
                        path = sectorPath,
                        color = borderColor,
                        style = Stroke(width = if (isWrong) 2.5.dp.toPx() else 1.5.dp.toPx())
                    )
                }
            }
        }

        // 2. Text Overlay Layer: Renders numbers upright in their respective sector centers
        Box(modifier = Modifier.fillMaxSize()) {
            // Center Number
            val centerNum = numbers.getOrNull(0) ?: 1
            val isCenterCleared = clearedIndices.contains(0)
            val isCenterWrong = wrongTapIndex == 0
            val centerTextColor = when {
                isCenterWrong -> MaterialTheme.colorScheme.onErrorContainer
                isCenterCleared -> Color(0xFF94A3B8).copy(alpha = 0.4f)
                else -> Color(0xFF1E293B)
            }

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(with(density) { (r0 * 2).toDp() }),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$centerNum",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = centerFontSizeSp,
                        fontWeight = FontWeight.ExtraBold,
                        color = centerTextColor,
                        textAlign = TextAlign.Center
                    )
                )
            }

            // Ring Numbers
            var textGlobalIdx = 1
            config.rings.forEachIndexed { rIdx, ringInfo ->
                val rIn = innerRadii[rIdx]
                val rOut = outerRadii[rIdx]
                val rMid = (rIn + rOut) / 2f
                val itemCount = ringInfo.itemCount
                val sweepAngleDeg = 360f / itemCount
                val ringAngle = if (isPlaying) {
                    if (ringInfo.isClockwise) cwAngle else ccwAngle
                } else {
                    0f
                }

                for (k in 0 until itemCount) {
                    val itemIndex = textGlobalIdx++
                    val number = numbers.getOrNull(itemIndex) ?: itemIndex
                    val isCleared = clearedIndices.contains(itemIndex)
                    val isWrong = wrongTapIndex == itemIndex

                    val midAngleDeg = (sweepAngleDeg * k) + ringAngle + (sweepAngleDeg / 2f)
                    val angleRad = midAngleDeg * (PI / 180f)

                    val xPosPx = center.x + (rMid * cos(angleRad)).toFloat()
                    val yPosPx = center.y + (rMid * sin(angleRad)).toFloat()

                    val xOffsetDp = with(density) { xPosPx.toDp() }
                    val yOffsetDp = with(density) { yPosPx.toDp() }

                    val textColor = when {
                        isWrong -> MaterialTheme.colorScheme.onErrorContainer
                        isCleared -> Color(0xFF94A3B8).copy(alpha = 0.35f)
                        else -> Color(0xFF1E293B)
                    }

                    val boxSizeDp = 40.dp
                    Box(
                        modifier = Modifier
                            .offset(
                                x = xOffsetDp - (boxSizeDp / 2),
                                y = yOffsetDp - (boxSizeDp / 2)
                            )
                            .size(boxSizeDp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$number",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = fontSizeSp,
                                fontWeight = FontWeight.ExtraBold,
                                color = textColor,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Creates an exact Annular Sector (環狀扇形) geometry Path
 */
private fun createAnnularSectorPath(
    center: Offset,
    innerRadius: Float,
    outerRadius: Float,
    startAngleDeg: Float,
    sweepAngleDeg: Float
): Path {
    val path = Path()
    val outerRect = Rect(
        center.x - outerRadius, center.y - outerRadius,
        center.x + outerRadius, center.y + outerRadius
    )
    val innerRect = Rect(
        center.x - innerRadius, center.y - innerRadius,
        center.x + innerRadius, center.y + innerRadius
    )

    // Outer Arc from startAngle to startAngle + sweepAngle
    path.arcTo(outerRect, startAngleDeg, sweepAngleDeg, false)
    // Line to inner arc and sweep back
    path.arcTo(innerRect, startAngleDeg + sweepAngleDeg, -sweepAngleDeg, false)
    path.close()
    return path
}
