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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

// 星際寶石扇區調色盤 (青金石、皇家紫水晶、祖母綠、琥珀赤金、冰魄天青、熾炎紅寶石)
private val celestialSectorBrushes = listOf(
    // 1. 青金石藍 (Lapis Lazuli)
    Brush.radialGradient(listOf(Color(0xFF283593), Color(0xFF1A237E), Color(0xFF0D1242))),
    // 2. 皇家紫水晶 (Royal Amethyst)
    Brush.radialGradient(listOf(Color(0xFF6A1B9A), Color(0xFF4A148C), Color(0xFF24074D))),
    // 3. 祖母綠晶石 (Emerald Crystal)
    Brush.radialGradient(listOf(Color(0xFF00695C), Color(0xFF004D40), Color(0xFF00251A))),
    // 4. 琥珀耀金 (Amber Sun)
    Brush.radialGradient(listOf(Color(0xFFFF8F00), Color(0xFFE65100), Color(0xFF5D1D00))),
    // 5. 冰魄天青 (Glacial Cyan)
    Brush.radialGradient(listOf(Color(0xFF0277BD), Color(0xFF01579B), Color(0xFF002F6C))),
    // 6. 熾炎紅寶石 (Ruby Flame)
    Brush.radialGradient(listOf(Color(0xFFC2185B), Color(0xFF880E4F), Color(0xFF4A0020))),
    // 7. 幻彩鈷藍 (Cobalt Prismatic)
    Brush.radialGradient(listOf(Color(0xFF1565C0), Color(0xFF0D47A1), Color(0xFF002171))),
    // 8. 幽影紫晶 (Shadow Plum)
    Brush.radialGradient(listOf(Color(0xFF7B1FA2), Color(0xFF38006B), Color(0xFF180033)))
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
    // 次世代曜石星穹背景
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF141722),
                        Color(0xFF0E1118),
                        Color(0xFF07090D)
                    )
                )
            )
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
                            text = Localization.getString("game_focus_train", language),
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
                                text = "${Localization.getString(diffTitleKey, language)} (1~${config.totalNumbers})",
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

            // Status Card: Target, Timer, Mistakes (星盤 HUD 儀表板)
            FocusTrainHudHeader(
                currentTarget = currentTarget,
                wrongCount = wrongCount,
                elapsedTimeMillis = elapsedTimeMillis,
                gameStatus = gameStatus,
                language = language
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Center Game Board Area (星穹天體儀旋轉輪盤)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (gameStatus == GameStatus.IDLE) {
                    // Ready Overlay: Central Start Button & Instructions
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .padding(16.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFF1B202C)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .drawBehind {
                                    drawRoundRect(
                                        brush = Brush.verticalGradient(
                                            listOf(Color(0xFFC5A059), Color(0xFF423828))
                                        ),
                                        topLeft = Offset.Zero,
                                        size = size,
                                        cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
                                        style = Stroke(width = 1.8.dp.toPx())
                                    )
                                }
                                .padding(24.dp)
                        ) {
                            // 星盤圖騰日輪預覽
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(Color(0xFFFFD54F), Color(0xFFFF8F00), Color(0xFF3E2723))
                                        )
                                    )
                                    .drawBehind {
                                        drawCircle(Color(0xFFFFF9C4), radius = size.minDimension / 2f, style = Stroke(width = 2.dp.toPx()))
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "1",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF1E1400)
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = Localization.getString("game_focus_train", language),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFFFD54F)
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = Localization.getString("focus_train_ready_hint", language),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    textAlign = TextAlign.Center,
                                    color = Color(0xFFB0BEC5),
                                    lineHeight = 22.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(22.dp))
                            Button(
                                onClick = onStartClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFB300)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color(0xFF1E1400),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = Localization.getString("game_start_button", language),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF1E1400)
                                    )
                                )
                            }
                        }
                    }
                } else {
                    // Active Dynamic Annular Sector Wheel (星穹天體儀雙向旋轉星盤)
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
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFFFD54F)
                    ),
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
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // Right: Restart Button
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
                            text = Localization.getString("game_reset_button", language),
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

    // Completed Settlement Dialog
    if (gameStatus == GameStatus.COMPLETED && lastCompletedTimeMillis != null) {
        FocusTrainCompletedDialog(
            timeMillis = lastCompletedTimeMillis,
            wrongCount = wrongCount,
            language = language,
            onPlayAgain = onStartClick,
            onBackToMenu = onBackClick
        )
    }
}

/**
 * 頂部星盤 HUD 儀表板
 */
@Composable
private fun FocusTrainHudHeader(
    currentTarget: Int,
    wrongCount: Int,
    elapsedTimeMillis: Long,
    gameStatus: GameStatus,
    language: AppLanguage
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF1B202A)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRoundRect(
                        color = Color(0x33FFFFFF),
                        topLeft = Offset.Zero,
                        size = size,
                        cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx()),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Current Target (星核聚光膠囊)
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = Localization.getString("current_target_label", language),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB0BEC5)
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF262C38)
                ) {
                    Text(
                        text = if (gameStatus == GameStatus.PLAYING) "$currentTarget" else "-",
                        modifier = Modifier
                            .drawBehind {
                                drawRoundRect(
                                    brush = Brush.verticalGradient(
                                        listOf(Color(0xFFFFD54F), Color(0xFFFF8F00))
                                    ),
                                    topLeft = Offset.Zero,
                                    size = size,
                                    cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                                    style = Stroke(width = 1.5.dp.toPx())
                                )
                            }
                            .padding(horizontal = 14.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFFD54F)
                        )
                    )
                }
            }

            // Mistakes Counter
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = Localization.getString("mistakes_label", language),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB0BEC5)
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$wrongCount",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = if (wrongCount > 0) Color(0xFFFF5252) else Color(0xFFECEFF1)
                    )
                )
            }

            // Timer (全息電漿碼錶)
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Localization.getString("time_elapsed_label", language),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB0BEC5)
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatTimeMillis(elapsedTimeMillis),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF00E5FF)
                    )
                )
            }
        }
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

        // 金屬星軌線條色彩
        val goldTrackColor = Color(0xFFC5A059)
        val stardustClearedBg = Brush.verticalGradient(listOf(Color(0xFF0F131C), Color(0xFF181C26)))
        val errorBg = Brush.radialGradient(listOf(Color(0xFFD50000), Color(0xFF6A0000)))

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
            // 最外圍星盤金屬底座護盤
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFF1E232E), Color(0xFF0E1118)),
                    center,
                    maxRadius + 8.dp.toPx()
                ),
                radius = maxRadius + 6.dp.toPx(),
                center = center
            )
            drawCircle(
                color = Color(0x66C5A059),
                radius = maxRadius + 6.dp.toPx(),
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Draw Annular Sectors (星際寶石同心圓扇區)
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

                    val sectorPath = createAnnularSectorPath(
                        center = center,
                        innerRadius = rIn,
                        outerRadius = rOut,
                        startAngleDeg = startAngleDeg,
                        sweepAngleDeg = sweepAngleDeg
                    )

                    // 扇區底色 (寶石晶質漸層 / 消除下沉金屬槽 / 錯誤電弧紅)
                    when {
                        isWrong -> drawPath(path = sectorPath, brush = errorBg)
                        isCleared -> drawPath(path = sectorPath, brush = stardustClearedBg)
                        else -> {
                            val brushIdx = (rIdx * 7 + k) % celestialSectorBrushes.size
                            drawPath(path = sectorPath, brush = celestialSectorBrushes[brushIdx])
                        }
                    }

                    // 扇區拋光黃金輻條與軌道線
                    drawPath(
                        path = sectorPath,
                        color = if (isWrong) Color(0xFFFF1744) else if (isCleared) Color(0x33C5A059) else goldTrackColor,
                        style = Stroke(width = if (isWrong) 2.5.dp.toPx() else 1.2.dp.toPx())
                    )
                }
            }

            // Draw Center Circle (中央曜石日輪金核)
            val isCenterCleared = clearedIndices.contains(0)
            val isCenterWrong = wrongTapIndex == 0

            when {
                isCenterWrong -> drawCircle(brush = errorBg, radius = r0, center = center)
                isCenterCleared -> drawCircle(brush = stardustClearedBg, radius = r0, center = center)
                else -> {
                    // 黑曜金太陽日輪
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Color(0xFFFFD54F), Color(0xFFFF8F00), Color(0xFF3E2723), Color(0xFF1A1311)),
                            center,
                            r0
                        ),
                        radius = r0,
                        center = center
                    )
                }
            }

            // 中央日輪立體黃金外飾圈
            drawCircle(
                color = if (isCenterWrong) Color(0xFFFF1744) else Color(0xFFFFF9C4),
                radius = r0,
                center = center,
                style = Stroke(width = if (isCenterWrong) 3.dp.toPx() else 2.dp.toPx())
            )
            // 內同心齒輪細環
            drawCircle(
                color = Color(0x66FFD54F),
                radius = r0 * 0.72f,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // 2. Text Overlay Layer: Renders numbers upright in their respective sector centers
        Box(modifier = Modifier.fillMaxSize()) {
            // Center Number
            val centerNum = numbers.getOrNull(0) ?: 1
            val isCenterCleared = clearedIndices.contains(0)
            val isCenterWrong = wrongTapIndex == 0

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
                        color = when {
                            isCenterWrong -> Color(0xFFFF8A80)
                            isCenterCleared -> Color(0x66B0BEC5)
                            else -> Color(0xFFFFFFFF)
                        },
                        textAlign = TextAlign.Center
                    )
                )
            }

            // Ring Numbers (3D 浮雕燙金星紋數字)
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
                        isWrong -> Color(0xFFFF5252)
                        isCleared -> Color(0x44B0BEC5)
                        else -> Color(0xFFFFD54F) // 耀眼燙金色
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

/**
 * 現代手遊高級星盤通關結算對話框
 */
@Composable
private fun FocusTrainCompletedDialog(
    timeMillis: Long,
    wrongCount: Int,
    language: AppLanguage,
    onPlayAgain: () -> Unit,
    onBackToMenu: () -> Unit
) {
    val starCount = if (wrongCount == 0) 3 else if (wrongCount <= 3) 2 else 1

    AlertDialog(
        onDismissRequest = onPlayAgain,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 星星成就列
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
                    text = Localization.getString("game_completed_title", language),
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
                // 用時大卡片
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
                            text = Localization.getString("time_elapsed_label", language),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFB0BEC5),
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatTimeMillis(timeMillis),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF00E5FF)
                            )
                        )
                    }
                }

                if (wrongCount > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "${Localization.getString("mistakes_label", language)}: $wrongCount 次",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFFFF5252),
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = Localization.getString("record_saved_message", language),
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
                    text = Localization.getString("play_again_button", language),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E1400)
                    )
                )
            }
        },
        dismissButton = {
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
    )
}
