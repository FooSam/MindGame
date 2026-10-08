package com.example.ui.screens

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.data.model.GameDifficulty
import com.example.data.model.Localization
import com.example.ui.viewmodel.GameStatus
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun SpeedMatchScreen(
    difficulty: GameDifficulty,
    gameStatus: GameStatus,
    gridNumbers: List<Int>,
    selectedCellIndex: Int?,
    correctRounds: Int,
    wrongTaps: Int,
    remainingTimeMs: Long,
    isWrongFlash: Boolean,
    language: AppLanguage,
    onBackClick: () -> Unit,
    onStartClick: () -> Unit,
    onResetClick: () -> Unit,
    onCellClick: (Int) -> Unit,
    onLeaderboardClick: () -> Unit
) {
    val context = LocalContext.current

    // Trigger audio beep & vibration on wrong tap
    LaunchedEffect(isWrongFlash) {
        if (isWrongFlash) {
            playBeepAndVibrate(context)
        }
    }

    // Flash background color animation
    val flashColor by animateColorAsState(
        targetValue = if (isWrongFlash) {
            Color(0x55E53935)
        } else {
            Color.Transparent
        },
        animationSpec = tween(durationMillis = 200),
        label = "flashAnim"
    )

    // 次世代曜石深色背景底襯
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF161920),
                        Color(0xFF0F1116),
                        Color(0xFF08090C)
                    )
                )
            )
            .background(flashColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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
                            contentDescription = "Back",
                            tint = Color(0xFFECEFF1)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
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
                            text = Localization.getString("game_speed_match", language),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFF5F7FA)
                            )
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFB300))
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = Localization.getString(diffTitleKey, language) + " (${difficulty.speedMatchCells}格)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFFFFB300),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onLeaderboardClick,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0x22FFFFFF))
                ) {
                    Icon(
                        imageVector = Icons.Default.Leaderboard,
                        contentDescription = "Leaderboard",
                        tint = Color(0xFFFFD54F)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 1-Minute Progress Bar & Timer Header (電競街機 HUD 儀表板)
            SpeedMatchHudHeader(
                remainingTimeMs = remainingTimeMs,
                language = language
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Live Game Stats Dashboard: Cleared & Wrong Taps
            if (gameStatus == GameStatus.PLAYING) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 通關輪數徽章
                    SpeedMatchStatBadge(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CheckCircle,
                        iconTint = Color(0xFF00E676),
                        title = Localization.getString("cleared_rounds", language),
                        valueText = "$correctRounds",
                        borderColor = Color(0x5500E676)
                    )

                    // 點錯次數徽章
                    SpeedMatchStatBadge(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Error,
                        iconTint = Color(0xFFFF5252),
                        title = Localization.getString("wrong_taps", language),
                        valueText = "$wrongTaps",
                        borderColor = Color(0x55FF5252)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Center Game Area (曜石棋盤護框)
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                val totalCells = difficulty.speedMatchCells
                val cols = difficulty.speedMatchCols

                // 精雕胡桃木與黑金雙層護框
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF1E232E),
                                    Color(0xFF141720),
                                    Color(0xFF0D0F14)
                                )
                            )
                        )
                        .drawBehind {
                            // 外圍立體深金金屬倒角線
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    listOf(Color(0xFFC5A059), Color(0xFF423828))
                                ),
                                topLeft = Offset.Zero,
                                size = size,
                                cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx()),
                                style = Stroke(width = 2.dp.toPx())
                            )
                            // 內下沉陰影
                            drawRoundRect(
                                color = Color(0x44000000),
                                topLeft = Offset(3.dp.toPx(), 3.dp.toPx()),
                                size = Size(size.width - 6.dp.toPx(), size.height - 6.dp.toPx()),
                                cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx()),
                                style = Stroke(width = 3.dp.toPx())
                            )
                        }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (gameStatus == GameStatus.IDLE) {
                        // Start Overlay
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            // 預覽光環寶石圖騰
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .drawBehind {
                                        drawBeveledObsidianCard(isSelected = true)
                                        drawStylized3DGemRune(number = 1)
                                    }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = Localization.getString("game_speed_match", language),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFFFD54F)
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = Localization.getString("game_speed_match_desc", language),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    textAlign = TextAlign.Center,
                                    color = Color(0xFFB0BEC5)
                                )
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = onStartClick,
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFB300)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth(0.75f)
                                    .height(54.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color(0xFF1E1400),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = Localization.getString("speed_match_start", language),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF1E1400)
                                    )
                                )
                            }
                        }
                    } else if (gameStatus == GameStatus.PLAYING) {
                        // Match Grid
                        val spacing = when {
                            totalCells <= 9 -> 12.dp
                            totalCells <= 16 -> 8.dp
                            else -> 6.dp
                        }

                        val fontSize = when {
                            totalCells <= 6 -> 24.sp
                            totalCells <= 9 -> 20.sp
                            totalCells <= 16 -> 17.sp
                            else -> 14.sp
                        }

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(cols),
                            horizontalArrangement = Arrangement.spacedBy(spacing),
                            verticalArrangement = Arrangement.spacedBy(spacing),
                            userScrollEnabled = false,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(gridNumbers) { index, number ->
                                val isSelected = selectedCellIndex == index

                                MatchCellView(
                                    number = number,
                                    isSelected = isSelected,
                                    fontSize = fontSize,
                                    onClick = { onCellClick(index) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onLeaderboardClick,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFFFD54F)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Leaderboard,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = Localization.getString("leaderboard_button", language),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                if (gameStatus != GameStatus.IDLE) {
                    Button(
                        onClick = onResetClick,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF37474F)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = Localization.getString("restart_game", language),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
            }
        }
    }

    // Time Up Settlement Dialog
    if (gameStatus == GameStatus.COMPLETED) {
        val totalAttempts = correctRounds + wrongTaps
        val accuracyPct = if (totalAttempts > 0) {
            (correctRounds.toFloat() / totalAttempts * 100f)
        } else 0f

        SpeedMatchCompletedDialog(
            correctRounds = correctRounds,
            wrongTaps = wrongTaps,
            accuracyPct = accuracyPct,
            language = language,
            onPlayAgain = onResetClick,
            onLeaderboard = onLeaderboardClick,
            onBackToMenu = onBackClick
        )
    }
}

/**
 * 頂部電競街機 HUD 能量倒數儀表板
 */
@Composable
private fun SpeedMatchHudHeader(
    remainingTimeMs: Long,
    language: AppLanguage
) {
    val seconds = (remainingTimeMs / 1000).coerceAtLeast(0)
    val millis = ((remainingTimeMs % 1000) / 100).coerceAtLeast(0)
    val timeStr = String.format(Locale.getDefault(), "%02d.%d s", seconds, millis)
    val progress = (remainingTimeMs.toFloat() / 60_000f).coerceIn(0f, 1f)
    val isUrgent = remainingTimeMs < 10_000L

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF1E222B)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    // 外邊框微倒角裝飾
                    drawRoundRect(
                        color = Color(0x33FFFFFF),
                        topLeft = Offset.Zero,
                        size = size,
                        cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx()),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isUrgent) Color(0xFFFF1744) else Color(0xFF00E5FF))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Localization.getString("timer_label", language),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFCFD8DC)
                        )
                    )
                }

                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isUrgent) Color(0xFFFF3D00) else Color(0xFF00E5FF)
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 雙層霓虹能量進度條
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFF0D0F14))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = progress)
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                if (isUrgent) {
                                    listOf(Color(0xFFFF9100), Color(0xFFFF1744))
                                } else {
                                    listOf(Color(0xFF00B0FF), Color(0xFF00E5FF), Color(0xFF76FF03))
                                }
                            )
                        )
                )
            }
        }
    }
}

/**
 * 即時統計膠囊徽章
 */
@Composable
private fun SpeedMatchStatBadge(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    valueText: String,
    borderColor: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1B202A)
    ) {
        Row(
            modifier = Modifier
                .drawBehind {
                    drawRoundRect(
                        color = borderColor,
                        topLeft = Offset.Zero,
                        size = size,
                        cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                }
                .padding(vertical = 8.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$title: $valueText",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFECEFF1)
                )
            )
        }
    }
}

/**
 * 曜石微倒角卡牌與 3D 浮雕寶石圖騰單元格 (`MatchCellView`)
 */
@Composable
fun MatchCellView(
    number: Int,
    isSelected: Boolean,
    fontSize: TextUnit,
    onClick: () -> Unit
) {
    // 點擊與選中彈簧浮空縮放
    val scaleAnim by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cellScale"
    )

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = scaleAnim
                scaleY = scaleAnim
            }
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .drawBehind {
                // 1. 繪製曜石金屬微倒角卡牌
                drawBeveledObsidianCard(isSelected = isSelected)

                // 2. 繪製 3D 浮雕符文寶石徽章圖騰
                drawStylized3DGemRune(number = number)
            },
        contentAlignment = Alignment.Center
    ) {
        // 微雕立體數字標籤（浮雕金色字體，兼具雙重認知辨識）
        Text(
            text = "$number",
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = fontSize,
                fontWeight = FontWeight.ExtraBold,
                color = if (isSelected) Color(0xFFFFFFFF) else Color(0xFFE0E0E0),
                textAlign = TextAlign.Center
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 3.dp)
                .background(
                    color = if (isSelected) Color(0xCC005B96) else Color(0xAA111317),
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 6.dp, vertical = 1.dp)
        )
    }
}

/**
 * 繪製曜石金屬微倒角卡牌底板 (`drawBeveledObsidianCard`)
 */
fun DrawScope.drawBeveledObsidianCard(
    isSelected: Boolean
) {
    val cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
    val w = size.width
    val h = size.height

    // 接觸陰影 (Drop Shadow)
    drawRoundRect(
        color = Color(0x55000000),
        topLeft = Offset(0f, 3.dp.toPx()),
        size = Size(w, h),
        cornerRadius = cornerRadius
    )

    // 曜石底板主體
    val baseBrush = if (isSelected) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF2C3545),
                Color(0xFF1B2230),
                Color(0xFF10141C)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFF262A34),
                Color(0xFF191C24),
                Color(0xFF111318)
            )
        )
    }

    drawRoundRect(
        brush = baseBrush,
        topLeft = Offset.Zero,
        size = Size(w, h),
        cornerRadius = cornerRadius
    )

    // 內縮金屬飾框線
    val inset = 3.5.dp.toPx()
    val innerCornerRadius = CornerRadius(13.dp.toPx(), 13.dp.toPx())
    drawRoundRect(
        brush = Brush.verticalGradient(
            listOf(
                if (isSelected) Color(0xFF00E5FF) else Color(0xFFC5A059),
                if (isSelected) Color(0xFF0091EA) else Color(0xFF5D4037)
            )
        ),
        topLeft = Offset(inset, inset),
        size = Size(w - inset * 2, h - inset * 2),
        cornerRadius = innerCornerRadius,
        style = Stroke(width = if (isSelected) 2.dp.toPx() else 1.2.dp.toPx())
    )

    // 若選中：星穹青藍霓虹光環
    if (isSelected) {
        // 外圍光環
        drawRoundRect(
            color = Color(0x4400E5FF),
            topLeft = Offset(-2.dp.toPx(), -2.dp.toPx()),
            size = Size(w + 4.dp.toPx(), h + 4.dp.toPx()),
            cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx()),
            style = Stroke(width = 3.dp.toPx())
        )
        // 核心白熾倒角框
        drawRoundRect(
            color = Color(0xEEFFFFFF),
            topLeft = Offset.Zero,
            size = Size(w, h),
            cornerRadius = cornerRadius,
            style = Stroke(width = 2.dp.toPx())
        )
    } else {
        // 未選中外微倒角金屬框
        drawRoundRect(
            color = Color(0x22FFFFFF),
            topLeft = Offset.Zero,
            size = Size(w, h),
            cornerRadius = cornerRadius,
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

/**
 * 繪製 12 大 3D 浮雕符文寶石徽章 (`drawStylized3DGemRune`)
 */
fun DrawScope.drawStylized3DGemRune(
    number: Int
) {
    val gemIndex = ((number - 1) % 12).coerceAtLeast(0)
    val cx = size.width / 2f
    val cy = size.height / 2f - 4.dp.toPx()
    val center = Offset(cx, cy)
    val radius = min(size.width, size.height) * 0.30f

    when (gemIndex) {
        0 -> drawBlazingRubyHeart(center, radius)
        1 -> drawRadiantSolarStar(center, radius)
        2 -> drawFrostIceSpark(center, radius)
        3 -> drawAmethystDiamond(center, radius)
        4 -> drawEmeraldTriangle(center, radius)
        5 -> drawAbyssalSapphireEye(center, radius)
        6 -> drawAmberCrest(center, radius)
        7 -> drawInfinityChrono(center, radius)
        8 -> drawCrimsonFlame(center, radius)
        9 -> drawPrismaticPrism(center, radius)
        10 -> drawThunderBolt(center, radius)
        else -> drawCrescentMoon(center, radius)
    }
}

// 0: 烈焰心晶 (Blazing Ruby Heart)
private fun DrawScope.drawBlazingRubyHeart(c: Offset, r: Float) {
    val path = Path().apply {
        moveTo(c.x, c.y + r * 0.9f)
        cubicTo(c.x - r * 1.2f, c.y + r * 0.3f, c.x - r * 1.1f, c.y - r * 0.8f, c.x, c.y - r * 0.3f)
        cubicTo(c.x + r * 1.1f, c.y - r * 0.8f, c.x + r * 1.2f, c.y + r * 0.3f, c.x, c.y + r * 0.9f)
        close()
    }
    // 陰影
    drawPath(path, Brush.radialGradient(listOf(Color(0xFFD50000), Color(0xFF880E4F), Color(0xFF310000)), c, r * 1.2f))
    // 倒角金邊
    drawPath(path, Color(0xFFFF80AB), style = Stroke(width = 2.dp.toPx()))
    // 高光點
    drawCircle(Color(0xCCFFFFFF), radius = r * 0.22f, center = Offset(c.x - r * 0.35f, c.y - r * 0.25f))
}

// 1: 日曜金星 (Radiant Solar Star)
private fun DrawScope.drawRadiantSolarStar(c: Offset, r: Float) {
    val path = Path()
    val points = 8
    val innerR = r * 0.45f
    for (i in 0 until points * 2) {
        val rad = (i * PI / points).toFloat()
        val curR = if (i % 2 == 0) r else innerR
        val px = c.x + curR * cos(rad)
        val py = c.y + curR * sin(rad)
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    drawPath(path, Brush.radialGradient(listOf(Color(0xFFFFEA00), Color(0xFFFF9100), Color(0xFFE65100)), c, r))
    drawPath(path, Color(0xFFFFF9C4), style = Stroke(width = 1.5.dp.toPx()))
    // 中心日輪
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFFFFF), Color(0xFFFFD54F)), c, r * 0.35f), radius = r * 0.35f, center = c)
}

// 2: 冰晶星芒 (Frost Ice Spark)
private fun DrawScope.drawFrostIceSpark(c: Offset, r: Float) {
    val path = Path()
    val points = 4
    val innerR = r * 0.22f
    for (i in 0 until points * 2) {
        val rad = (i * PI / points).toFloat()
        val curR = if (i % 2 == 0) r else innerR
        val px = c.x + curR * cos(rad)
        val py = c.y + curR * sin(rad)
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    drawPath(path, Brush.radialGradient(listOf(Color(0xFFE1F5FE), Color(0xFF00B0FF), Color(0xFF01579B)), c, r))
    drawPath(path, Color(0xFFFFFFFF), style = Stroke(width = 2.dp.toPx()))
    // 星核
    drawCircle(Color.White, radius = r * 0.20f, center = c)
}

// 3: 紫水晶菱錐 (Amethyst Diamond)
private fun DrawScope.drawAmethystDiamond(c: Offset, r: Float) {
    val top = Offset(c.x, c.y - r)
    val bottom = Offset(c.x, c.y + r)
    val left = Offset(c.x - r * 0.85f, c.y)
    val right = Offset(c.x + r * 0.85f, c.y)

    // 左上半
    val pLeftTop = Path().apply { moveTo(c.x, c.y); lineTo(left.x, left.y); lineTo(top.x, top.y); close() }
    drawPath(pLeftTop, Brush.linearGradient(listOf(Color(0xFFEA80FC), Color(0xFFAB47BC)), top, left))

    // 右上半
    val pRightTop = Path().apply { moveTo(c.x, c.y); lineTo(right.x, right.y); lineTo(top.x, top.y); close() }
    drawPath(pRightTop, Brush.linearGradient(listOf(Color(0xFFE1BEE7), Color(0xFF8E24AA)), top, right))

    // 左下半
    val pLeftBottom = Path().apply { moveTo(c.x, c.y); lineTo(left.x, left.y); lineTo(bottom.x, bottom.y); close() }
    drawPath(pLeftBottom, Brush.linearGradient(listOf(Color(0xFF7B1FA2), Color(0xFF4A148C)), left, bottom))

    // 右下半
    val pRightBottom = Path().apply { moveTo(c.x, c.y); lineTo(right.x, right.y); lineTo(bottom.x, bottom.y); close() }
    drawPath(pRightBottom, Brush.linearGradient(listOf(Color(0xFF8E24AA), Color(0xFF311B92)), right, bottom))

    // 金屬外輪廓線
    val fullPath = Path().apply { moveTo(top.x, top.y); lineTo(right.x, right.y); lineTo(bottom.x, bottom.y); lineTo(left.x, left.y); close() }
    drawPath(fullPath, Color(0xFFF3E5F5), style = Stroke(width = 2.dp.toPx()))
    drawCircle(Color(0xCCFFFFFF), radius = r * 0.16f, center = Offset(c.x - r * 0.2f, c.y - r * 0.3f))
}

// 4: 翠綠翡翠三角 (Emerald Triangle)
private fun DrawScope.drawEmeraldTriangle(c: Offset, r: Float) {
    val top = Offset(c.x, c.y - r * 0.9f)
    val bl = Offset(c.x - r * 0.95f, c.y + r * 0.8f)
    val br = Offset(c.x + r * 0.95f, c.y + r * 0.8f)

    val p1 = Path().apply { moveTo(c.x, c.y); lineTo(top.x, top.y); lineTo(bl.x, bl.y); close() }
    drawPath(p1, Brush.linearGradient(listOf(Color(0xFFB9F6CA), Color(0xFF00E676)), top, bl))

    val p2 = Path().apply { moveTo(c.x, c.y); lineTo(top.x, top.y); lineTo(br.x, br.y); close() }
    drawPath(p2, Brush.linearGradient(listOf(Color(0xFF69F0AE), Color(0xFF00C853)), top, br))

    val p3 = Path().apply { moveTo(c.x, c.y); lineTo(bl.x, bl.y); lineTo(br.x, br.y); close() }
    drawPath(p3, Brush.linearGradient(listOf(Color(0xFF00B248), Color(0xFF1B5E20)), bl, br))

    val fullPath = Path().apply { moveTo(top.x, top.y); lineTo(br.x, br.y); lineTo(bl.x, bl.y); close() }
    drawPath(fullPath, Color(0xFFE8F5E9), style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round))
}

// 5: 深海之眼 (Abyssal Sapphire Eye)
private fun DrawScope.drawAbyssalSapphireEye(c: Offset, r: Float) {
    val path = Path().apply {
        moveTo(c.x - r * 1.1f, c.y)
        cubicTo(c.x - r * 0.5f, c.y - r * 0.85f, c.x + r * 0.5f, c.y - r * 0.85f, c.x + r * 1.1f, c.y)
        cubicTo(c.x + r * 0.5f, c.y + r * 0.85f, c.x - r * 0.5f, c.y + r * 0.85f, c.x - r * 1.1f, c.y)
        close()
    }
    drawPath(path, Brush.radialGradient(listOf(Color(0xFF40C4FF), Color(0xFF0091EA), Color(0xFF0D47A1)), c, r * 1.1f))
    drawPath(path, Color(0xFFB3E5FC), style = Stroke(width = 2.dp.toPx()))
    // 瞳孔與雙高光
    drawCircle(Color(0xFF061838), radius = r * 0.42f, center = c)
    drawCircle(Color(0xFF2979FF), radius = r * 0.32f, center = c)
    drawCircle(Color.White, radius = r * 0.16f, center = Offset(c.x - r * 0.12f, c.y - r * 0.12f))
    drawCircle(Color(0xCCFFFFFF), radius = r * 0.08f, center = Offset(c.x + r * 0.14f, c.y + r * 0.10f))
}

// 6: 琥珀金幣 (Amber Crest)
private fun DrawScope.drawAmberCrest(c: Offset, r: Float) {
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFD54F), Color(0xFFFFB300), Color(0xFFFF6F00)), c, r), radius = r, center = c)
    drawCircle(Color(0xFFFFF9C4), radius = r, center = c, style = Stroke(width = 2.dp.toPx()))
    drawCircle(Color(0x66FF6F00), radius = r * 0.65f, center = c, style = Stroke(width = 1.5.dp.toPx()))
    drawCircle(Color(0xCCFFFFFF), radius = r * 0.25f, center = Offset(c.x - r * 0.35f, c.y - r * 0.35f))
}

// 7: 時空環印 (Infinity Chrono)
private fun DrawScope.drawInfinityChrono(c: Offset, r: Float) {
    val leftC = Offset(c.x - r * 0.45f, c.y)
    val rightC = Offset(c.x + r * 0.45f, c.y)
    drawCircle(Brush.radialGradient(listOf(Color(0xFF00E5FF), Color(0xFF0091EA)), leftC, r * 0.55f), radius = r * 0.5f, center = leftC, style = Stroke(width = 3.5.dp.toPx()))
    drawCircle(Brush.radialGradient(listOf(Color(0xFF76FF03), Color(0xFF00E676)), rightC, r * 0.55f), radius = r * 0.5f, center = rightC, style = Stroke(width = 3.5.dp.toPx()))
    drawCircle(Color.White, radius = r * 0.18f, center = c)
}

// 8: 赤紅烈火 (Crimson Flame)
private fun DrawScope.drawCrimsonFlame(c: Offset, r: Float) {
    val path = Path().apply {
        moveTo(c.x, c.y - r)
        cubicTo(c.x + r * 0.9f, c.y - r * 0.2f, c.x + r * 0.7f, c.y + r * 0.9f, c.x, c.y + r * 0.95f)
        cubicTo(c.x - r * 0.7f, c.y + r * 0.9f, c.x - r * 0.9f, c.y - r * 0.2f, c.x, c.y - r)
        close()
    }
    drawPath(path, Brush.verticalGradient(listOf(Color(0xFFFFEA00), Color(0xFFFF3D00), Color(0xFFBF360C)), startY = c.y - r, endY = c.y + r))
    drawPath(path, Color(0xFFFFF3E0), style = Stroke(width = 2.dp.toPx()))
    // 內核白熾焰心
    val inner = Path().apply {
        moveTo(c.x, c.y - r * 0.4f)
        cubicTo(c.x + r * 0.4f, c.y, c.x + r * 0.3f, c.y + r * 0.55f, c.x, c.y + r * 0.6f)
        cubicTo(c.x - r * 0.3f, c.y + r * 0.55f, c.x - r * 0.4f, c.y, c.x, c.y - r * 0.4f)
        close()
    }
    drawPath(inner, Brush.verticalGradient(listOf(Color.White, Color(0xFFFFD54F))))
}

// 9: 幻彩極光鑽 (Prismatic Prism)
private fun DrawScope.drawPrismaticPrism(c: Offset, r: Float) {
    val points = 6
    val path = Path()
    for (i in 0 until points) {
        val rad = (i * 2 * PI / points).toFloat()
        val px = c.x + r * cos(rad)
        val py = c.y + r * sin(rad)
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    drawPath(path, Brush.radialGradient(listOf(Color(0xFFFF4081), Color(0xFFD500F9), Color(0xFF4A148C)), c, r))
    drawPath(path, Color(0xFFF8BBD0), style = Stroke(width = 2.dp.toPx()))
    // 切面星芒
    drawLine(Color(0x99FFFFFF), Offset(c.x - r, c.y), Offset(c.x + r, c.y), strokeWidth = 1.5.dp.toPx())
    drawLine(Color(0x99FFFFFF), Offset(c.x, c.y - r), Offset(c.x, c.y + r), strokeWidth = 1.5.dp.toPx())
    drawCircle(Color.White, radius = r * 0.18f, center = Offset(c.x - r * 0.3f, c.y - r * 0.3f))
}

// 10: 雷霆神戟 (Thunder Bolt)
private fun DrawScope.drawThunderBolt(c: Offset, r: Float) {
    val path = Path().apply {
        moveTo(c.x + r * 0.1f, c.y - r)
        lineTo(c.x - r * 0.7f, c.y)
        lineTo(c.x, c.y)
        lineTo(c.x - r * 0.2f, c.y + r)
        lineTo(c.x + r * 0.7f, c.y - r * 0.1f)
        lineTo(c.x + r * 0.05f, c.y - r * 0.1f)
        close()
    }
    drawPath(path, Brush.verticalGradient(listOf(Color(0xFFFFFF00), Color(0xFFFFD600), Color(0xFFFF6D00)), startY = c.y - r, endY = c.y + r))
    drawPath(path, Color(0xFFFFFFFF), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawCircle(Color.White, radius = r * 0.15f, center = Offset(c.x, c.y))
}

// 11: 星月徽印 (Crescent Moon)
private fun DrawScope.drawCrescentMoon(c: Offset, r: Float) {
    val path = Path().apply {
        moveTo(c.x, c.y - r)
        cubicTo(c.x + r * 0.95f, c.y - r * 0.4f, c.x + r * 0.95f, c.y + r * 0.4f, c.x, c.y + r)
        cubicTo(c.x + r * 0.45f, c.y + r * 0.4f, c.x + r * 0.45f, c.y - r * 0.4f, c.x, c.y - r)
        close()
    }
    drawPath(path, Brush.radialGradient(listOf(Color(0xFFEDE7F6), Color(0xFFB388FF), Color(0xFF512DA8)), c, r))
    drawPath(path, Color(0xFFFFFFFF), style = Stroke(width = 2.dp.toPx()))
    drawCircle(Color.White, radius = r * 0.15f, center = Offset(c.x + r * 0.4f, c.y))
}

/**
 * 現代手遊高級結算對話框
 */
@Composable
fun SpeedMatchCompletedDialog(
    correctRounds: Int,
    wrongTaps: Int,
    accuracyPct: Float,
    language: AppLanguage,
    onPlayAgain: () -> Unit,
    onLeaderboard: () -> Unit,
    onBackToMenu: () -> Unit
) {
    // 依通關次數評等 (1~3 星)
    val starCount = when {
        correctRounds >= 25 -> 3
        correctRounds >= 15 -> 2
        else -> 1
    }

    AlertDialog(
        onDismissRequest = {},
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 星星獎章列
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { i ->
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (i < starCount) Color(0xFFFFD54F) else Color(0xFF455A64),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = Localization.getString("speed_match_time_up", language),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFFD54F),
                        textAlign = TextAlign.Center
                    )
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 主得分卡（立體燙金微倒角面板）
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF1E2430),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .drawBehind {
                                drawRoundRect(
                                    color = Color(0x66C5A059),
                                    topLeft = Offset.Zero,
                                    size = size,
                                    cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx()),
                                    style = Stroke(width = 1.5.dp.toPx())
                                )
                            }
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = Localization.getString("cleared_rounds", language),
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color(0xFFB0BEC5),
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$correctRounds 次",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFFFD54F)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF1E2430),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = Localization.getString("wrong_taps", language),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF90A4AE)
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$wrongTaps 次",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFFF5252)
                                )
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF1E2430),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = Localization.getString("accuracy_rate", language),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF90A4AE)
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f%%", accuracyPct),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF00E676)
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = Localization.getString("score_saved", language),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF78909C)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onPlayAgain,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = Localization.getString("play_again", language),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E1400)
                    )
                )
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onLeaderboard) {
                    Text(
                        text = Localization.getString("leaderboard_button", language),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD54F)
                        )
                    )
                }
                TextButton(onClick = onBackToMenu) {
                    Text(
                        text = Localization.getString("back_to_menu", language),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB0BEC5)
                        )
                    )
                }
            }
        }
    )
}

private fun playBeepAndVibrate(context: Context) {
    try {
        val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
        toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            val vibrator = vibratorManager.defaultVibrator
            vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vibrator.vibrate(150)
            }
        }
    } catch (_: Exception) {
    }
}
