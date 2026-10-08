package com.example.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.data.model.GameDifficulty
import com.example.data.model.Localization
import com.example.ui.components.formatTimeMillis
import com.example.ui.viewmodel.GameStatus
import kotlin.math.min

@Composable
fun FocusGameScreen(
    difficulty: GameDifficulty,
    gameStatus: GameStatus,
    gridNumbers: List<Int>,
    currentTarget: Int,
    clearedIndices: Set<Int>,
    elapsedTimeMillis: Long,
    wrongTapIndex: Int?,
    lastCompletedTimeMillis: Long?,
    language: AppLanguage,
    onBackClick: () -> Unit,
    onStartClick: () -> Unit,
    onResetClick: () -> Unit,
    onCellClick: (Int) -> Unit,
    onLeaderboardClick: () -> Unit
) {
    // 次世代曜石神殿背景
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF161922),
                        Color(0xFF0F1218),
                        Color(0xFF080A0E)
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
                            text = Localization.getString("game_focus_test", language),
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
                                text = Localization.getString(diffTitleKey, language) + " (${difficulty.totalCells}格)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFFFFB300),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                // Top-Right Leaderboard Button
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

            // Timer & Next Target Dashboard Box (全息電漿計時與目標膠囊)
            FocusTestHudHeader(
                elapsedTimeMillis = elapsedTimeMillis,
                currentTarget = currentTarget,
                totalCells = difficulty.totalCells,
                gameStatus = gameStatus,
                language = language
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Grid Area (曜石神殿護盤，尺寸自動適應無滾動)
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                val maxW = maxWidth
                val maxH = maxHeight
                val sideLength = if (maxW < maxH) maxW else maxH

                val gridDim = difficulty.gridDim
                val spacing = when (gridDim) {
                    3 -> 10.dp
                    4 -> 8.dp
                    5 -> 6.dp
                    6 -> 5.dp
                    7 -> 4.dp
                    8 -> 3.dp
                    else -> 4.dp
                }

                val fontSize = when (gridDim) {
                    3 -> 28.sp
                    4 -> 24.sp
                    5 -> 20.sp
                    6 -> 16.sp
                    7 -> 14.sp
                    8 -> 12.sp
                    else -> 14.sp
                }

                // 神殿黑金護框底座
                Box(
                    modifier = Modifier
                        .size(sideLength)
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
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (gameStatus == GameStatus.IDLE) {
                        // Ready overlay
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            // 神殿開局符石預覽
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .drawBehind {
                                        drawTempleRuneCell(isCleared = false, isWrong = false)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "1",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFFFD54F)
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = Localization.getString("game_ready_hint", language),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    textAlign = TextAlign.Center,
                                    color = Color(0xFFB0BEC5)
                                )
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = onStartClick,
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFB300)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth(0.75f)
                                    .height(52.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color(0xFF1E1400),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = Localization.getString("start_game", language),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF1E1400)
                                    )
                                )
                            }
                        }
                    } else {
                        // Active Grid
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(gridDim),
                            horizontalArrangement = Arrangement.spacedBy(spacing),
                            verticalArrangement = Arrangement.spacedBy(spacing),
                            userScrollEnabled = false,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(gridNumbers) { index, number ->
                                val isCleared = clearedIndices.contains(index)
                                val isWrong = wrongTapIndex == index

                                CellView(
                                    number = number,
                                    isCleared = isCleared,
                                    isWrong = isWrong,
                                    fontSize = fontSize,
                                    onClick = { onCellClick(index) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Action Bar: Reset / Restart Button & Leaderboard
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

    // Completion Dialog
    if (gameStatus == GameStatus.COMPLETED && lastCompletedTimeMillis != null) {
        GameCompletedDialog(
            timeMillis = lastCompletedTimeMillis,
            difficulty = difficulty,
            language = language,
            onPlayAgain = onResetClick,
            onLeaderboard = onLeaderboardClick,
            onBackToMenu = onBackClick
        )
    }
}

/**
 * 頂部全息電漿計時儀表板與目標指引膠囊
 */
@Composable
private fun FocusTestHudHeader(
    elapsedTimeMillis: Long,
    currentTarget: Int,
    totalCells: Int,
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
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // High Precision Timer Box (全息電漿青藍計時器)
            Column(horizontalAlignment = Alignment.Start) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E5FF))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = Localization.getString("timer_label", language),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB0BEC5)
                        )
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatTimeMillis(elapsedTimeMillis),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF00E5FF)
                    )
                )
            }

            // Target Number Indicator (璀璨聚光神聖符石膠囊)
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Localization.getString("target_label", language),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB0BEC5)
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF262C38)
                ) {
                    Row(
                        modifier = Modifier
                            .drawBehind {
                                drawRoundRect(
                                    brush = Brush.verticalGradient(
                                        listOf(Color(0xFFFFD54F), Color(0xFFFF8F00))
                                    ),
                                    topLeft = Offset.Zero,
                                    size = size,
                                    cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
                                    style = Stroke(width = 1.5.dp.toPx())
                                )
                            }
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (gameStatus == GameStatus.PLAYING) "$currentTarget" else "-",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFFFD54F)
                            )
                        )
                        Text(
                            text = " / $totalCells",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB0BEC5)
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * 曜石神殿微倒角方格單元 (`CellView`)
 */
@Composable
fun CellView(
    number: Int,
    isCleared: Boolean,
    isWrong: Boolean,
    fontSize: TextUnit,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isWrong) 1.15f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cellScale"
    )

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !isCleared, onClick = onClick)
            .drawBehind {
                drawTempleRuneCell(
                    isCleared = isCleared,
                    isWrong = isWrong
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (!isCleared) {
            Text(
                text = "$number",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = fontSize,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isWrong) Color(0xFFFF5252) else Color(0xFFFFD54F),
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

/**
 * 繪製曜石神殿微倒角方格底板 (`drawTempleRuneCell`)
 */
fun DrawScope.drawTempleRuneCell(
    isCleared: Boolean,
    isWrong: Boolean
) {
    val cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
    val w = size.width
    val h = size.height

    if (isCleared) {
        // 下沉式金屬基座凹槽 (Sunken Inset Pedestal)
        // 內陰影
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF090B0E), Color(0xFF13161D))
            ),
            topLeft = Offset.Zero,
            size = Size(w, h),
            cornerRadius = cornerRadius
        )
        // 微弱凹槽暗飾線
        drawRoundRect(
            color = Color(0x33C5A059),
            topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
            size = Size(w - 4.dp.toPx(), h - 4.dp.toPx()),
            cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
            style = Stroke(width = 1.dp.toPx())
        )
        // 凹槽中央神殿星塵微印
        val cx = w / 2f
        val cy = h / 2f
        val r = min(w, h) * 0.15f
        drawCircle(
            color = Color(0x22C5A059),
            radius = r,
            center = Offset(cx, cy)
        )
    } else {
        // 未消除：曜石玄鐵神殿方格
        // 接觸陰影
        drawRoundRect(
            color = Color(0x55000000),
            topLeft = Offset(0f, 2.5.dp.toPx()),
            size = Size(w, h),
            cornerRadius = cornerRadius
        )

        // 曜石底盤漸層
        val baseBrush = if (isWrong) {
            Brush.verticalGradient(
                listOf(Color(0xFF4A1212), Color(0xFF2E0B0B))
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color(0xFF2C3240),
                    Color(0xFF1B202A),
                    Color(0xFF12151B)
                )
            )
        }

        drawRoundRect(
            brush = baseBrush,
            topLeft = Offset.Zero,
            size = Size(w, h),
            cornerRadius = cornerRadius
        )

        // 內縮金屬飾框
        val inset = 3.dp.toPx()
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(
                    if (isWrong) Color(0xFFFF5252) else Color(0xFFE5C07B),
                    if (isWrong) Color(0xFFB71C1C) else Color(0xFF5D4037)
                )
            ),
            topLeft = Offset(inset, inset),
            size = Size(w - inset * 2, h - inset * 2),
            cornerRadius = CornerRadius(9.dp.toPx(), 9.dp.toPx()),
            style = Stroke(width = 1.2.dp.toPx())
        )

        // 外微倒角高光線
        drawRoundRect(
            color = if (isWrong) Color(0xFFFF5252) else Color(0x33FFFFFF),
            topLeft = Offset.Zero,
            size = Size(w, h),
            cornerRadius = cornerRadius,
            style = Stroke(width = 1.dp.toPx())
        )

        // 神殿四角微雕幾何印記 (Corner Filigree)
        val markSize = 4.dp.toPx()
        // 左上
        drawLine(Color(0x88C5A059), Offset(inset, inset + markSize), Offset(inset, inset), 1.5.dp.toPx())
        drawLine(Color(0x88C5A059), Offset(inset, inset), Offset(inset + markSize, inset), 1.5.dp.toPx())
        // 右上
        drawLine(Color(0x88C5A059), Offset(w - inset - markSize, inset), Offset(w - inset, inset), 1.5.dp.toPx())
        drawLine(Color(0x88C5A059), Offset(w - inset, inset), Offset(w - inset, inset + markSize), 1.5.dp.toPx())
    }
}

/**
 * 現代手遊高級通關結算對話框
 */
@Composable
fun GameCompletedDialog(
    timeMillis: Long,
    difficulty: GameDifficulty,
    language: AppLanguage,
    onPlayAgain: () -> Unit,
    onLeaderboard: () -> Unit,
    onBackToMenu: () -> Unit
) {
    val ratingKey = when {
        timeMillis < difficulty.totalCells * 600L -> "focus_rating_fast"
        timeMillis < difficulty.totalCells * 1200L -> "focus_rating_great"
        else -> "focus_rating_good"
    }

    val starCount = when {
        timeMillis < difficulty.totalCells * 600L -> 3
        timeMillis < difficulty.totalCells * 1200L -> 2
        else -> 1
    }

    AlertDialog(
        onDismissRequest = {},
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 星星成就
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
                    text = Localization.getString("game_completed", language),
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
                Text(
                    text = Localization.getString(ratingKey, language),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E676),
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

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
                            text = Localization.getString("your_time", language),
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
