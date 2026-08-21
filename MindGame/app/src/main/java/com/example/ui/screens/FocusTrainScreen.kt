package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

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

        Spacer(modifier = Modifier.height(10.dp))

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

        Spacer(modifier = Modifier.height(12.dp))

        // Center Rotating Wheel Board
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            RotatingWheelBoard(
                difficulty = difficulty,
                config = config,
                numbers = numbers,
                clearedIndices = clearedIndices,
                wrongTapIndex = wrongTapIndex,
                isPlaying = gameStatus == GameStatus.PLAYING,
                onCellClick = onCellClick
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bottom Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (gameStatus == GameStatus.IDLE) {
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
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Localization.getString("game_start_button", language),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            } else {
                OutlinedButton(
                    onClick = onResetClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Localization.getString("game_reset_button", language),
                        style = MaterialTheme.typography.titleMedium.copy(
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
private fun RotatingWheelBoard(
    difficulty: GameDifficulty,
    config: WheelDifficultyConfig,
    numbers: List<Int>,
    clearedIndices: Set<Int>,
    wrongTapIndex: Int?,
    isPlaying: Boolean,
    onCellClick: (Int) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "WheelRotationTransition")
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

    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val ringGlowColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        val boardSizePx = with(density) { min(maxWidth.toPx(), maxHeight.toPx()) }
        val center = Offset(boardSizePx / 2f, boardSizePx / 2f)
        val maxRadius = boardSizePx * 0.44f

        // Calculate Ring Radii according to ring count
        val ringCount = config.rings.size
        val ringRadii = when (ringCount) {
            1 -> listOf(maxRadius * 0.70f)
            2 -> listOf(maxRadius * 0.46f, maxRadius * 0.85f)
            else -> listOf(maxRadius * 0.35f, maxRadius * 0.63f, maxRadius * 0.90f)
        }

        // Adaptive Cell Size
        val cellDiameterDp: Dp = when {
            difficulty == GameDifficulty.EPIC -> 34.dp
            difficulty == GameDifficulty.HELL -> 38.dp
            difficulty == GameDifficulty.HARD -> 42.dp
            ringCount == 2 -> 46.dp
            else -> 52.dp
        }
        val fontSizeSp = when {
            difficulty == GameDifficulty.EPIC -> 13.sp
            difficulty == GameDifficulty.HELL -> 14.sp
            difficulty == GameDifficulty.HARD -> 16.sp
            else -> 18.sp
        }

        // Draw Ring Guides (Canvas Background)
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Center ring hint
            drawCircle(
                color = ringGlowColor,
                radius = with(density) { (cellDiameterDp / 2f).toPx() + 4.dp.toPx() },
                center = center,
                style = Stroke(
                    width = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            )

            ringRadii.forEach { r ->
                drawCircle(
                    color = outlineColor,
                    radius = r,
                    center = center,
                    style = Stroke(
                        width = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                    )
                )
            }
        }

        // 1. Center Button (Index 0)
        val centerNumber = numbers.getOrNull(0) ?: 1
        val isCenterCleared = clearedIndices.contains(0)
        val isCenterWrong = wrongTapIndex == 0

        WheelCellButton(
            number = centerNumber,
            isCleared = isCenterCleared,
            isWrong = isCenterWrong,
            diameter = cellDiameterDp,
            fontSize = fontSizeSp,
            modifier = Modifier.align(Alignment.Center),
            onClick = { onCellClick(0) }
        )

        // 2. Ring Buttons
        var globalIndex = 1
        config.rings.forEachIndexed { rIdx, ringInfo ->
            val radius = ringRadii[rIdx]
            val ringAngle = if (isPlaying) {
                if (ringInfo.isClockwise) cwAngle else ccwAngle
            } else {
                0f
            }

            for (k in 0 until ringInfo.itemCount) {
                val itemIndex = globalIndex++
                val number = numbers.getOrNull(itemIndex) ?: itemIndex
                val isCleared = clearedIndices.contains(itemIndex)
                val isWrong = wrongTapIndex == itemIndex

                val baseAngleDeg = (360f / ringInfo.itemCount) * k
                val currentAngleDeg = baseAngleDeg + ringAngle
                val angleRad = currentAngleDeg * (PI / 180f)

                val xOffsetPx = (radius * cos(angleRad)).toFloat()
                val yOffsetPx = (radius * sin(angleRad)).toFloat()

                val xOffsetDp = with(density) { xOffsetPx.toDp() }
                val yOffsetDp = with(density) { yOffsetPx.toDp() }

                WheelCellButton(
                    number = number,
                    isCleared = isCleared,
                    isWrong = isWrong,
                    diameter = cellDiameterDp,
                    fontSize = fontSizeSp,
                    modifier = Modifier.offset(x = xOffsetDp, y = yOffsetDp),
                    onClick = { onCellClick(itemIndex) }
                )
            }
        }
    }
}

@Composable
private fun WheelCellButton(
    number: Int,
    isCleared: Boolean,
    isWrong: Boolean,
    diameter: Dp,
    fontSize: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    val bgColor = when {
        isCleared -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        isWrong -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surface
    }

    val textColor = when {
        isCleared -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        isWrong -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    val borderColor = when {
        isWrong -> MaterialTheme.colorScheme.error
        isCleared -> MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
    }

    Surface(
        modifier = modifier
            .size(diameter)
            .clip(CircleShape)
            .border(
                width = if (isWrong) 2.5.dp else 1.5.dp,
                color = borderColor,
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = !isCleared,
                onClick = onClick
            ),
        shape = CircleShape,
        color = bgColor,
        shadowElevation = if (isCleared) 0.dp else 2.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$number",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = fontSize,
                    fontWeight = if (isCleared) FontWeight.Normal else FontWeight.Bold,
                    color = textColor,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}
