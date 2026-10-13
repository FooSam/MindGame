package com.example.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Translate
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AppLanguage
import com.example.data.model.GameDifficulty
import com.example.data.model.Localization
import com.example.game.stroop.StroopColor
import com.example.game.stroop.StroopInstruction
import com.example.game.stroop.StroopOption
import com.example.game.stroop.StroopQuestion
import com.example.game.stroop.StroopShape
import com.example.ui.viewmodel.GameStatus

/**
 * 《斯特魯普效應》(Stroop Effect) 次世代 2024-2026 頂級手遊視覺重構
 * 導入賽博全息競技場、3D 立體液態霓虹字符、全息微倒角曜石浮空基座、
 * 3D 水晶微倒角能量按鈕、電競 HUD 與超頻連擊儀表板。
 */
@Composable
fun StroopScreen(
    difficulty: GameDifficulty,
    gameStatus: GameStatus,
    question: StroopQuestion?,
    score: Int,
    combo: Int,
    maxCombo: Int,
    correctCount: Int,
    wrongCount: Int,
    remainingGameTimeMs: Long,
    questionTimeProgress: Float,
    isWrongFlash: Boolean,
    language: AppLanguage,
    onBackClick: () -> Unit,
    onStartClick: () -> Unit,
    onResetClick: () -> Unit,
    onOptionSelected: (StroopOption) -> Unit,
    onLeaderboardClick: () -> Unit
) {
    val context = LocalContext.current
    val totalSeconds = (remainingGameTimeMs / 1000).coerceAtLeast(0)

    // 觸覺震動回饋：答錯時短促震動
    LaunchedEffect(isWrongFlash) {
        if (isWrongFlash) {
            triggerErrorVibration(context)
        }
    }

    // 地獄與史詩級動態微傾震顫
    val infiniteTransition = rememberInfiniteTransition(label = "stroopDynamic")
    val dynamicRotation by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dynamicAngle"
    )

    // 全息呼吸光暈動畫
    val holoPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "holoPulse"
    )

    // 賽博競技場主底色漸層
    val bgBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0D111A), // 深邃曜石頂部
                Color(0xFF090D14), // 中段科技玄鐵
                Color(0xFF05070B)  // 底部黑曜石基盤
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
            .drawBehind {
                drawCyberArenaGridBackground()
            }
            .then(
                if (isWrongFlash) Modifier
                    .background(Color(0x33EF4444))
                    .border(6.dp, Color(0xFFEF4444))
                else Modifier
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // 1. 頂部導航列 (Top Bar)
            StroopTopBar(
                difficulty = difficulty,
                language = language,
                onBackClick = onBackClick,
                onLeaderboardClick = onLeaderboardClick,
                onResetClick = onResetClick
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2. 次世代全息電競 HUD 儀表板 (Cyber HUD Dashboard)
            StroopCyberHudDashboard(
                score = score,
                combo = combo,
                remainingGameTimeMs = remainingGameTimeMs,
                totalSeconds = totalSeconds,
                questionTimeProgress = questionTimeProgress,
                language = language
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. 遊戲核心區域 (Playing vs Idle)
            if (gameStatus == GameStatus.PLAYING && question != null) {
                // 3.1 全息任務指令光帶 (Instruction Holo-Banner)
                StroopInstructionHoloBanner(
                    instruction = question.instruction,
                    language = language,
                    holoPulseAlpha = holoPulseAlpha
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 3.2 賽博全息曜石浮空基座與 3D 立體液態霓虹字符
                StroopCyberStimulusStage(
                    question = question,
                    difficulty = difficulty,
                    language = language,
                    dynamicRotation = dynamicRotation,
                    holoPulseAlpha = holoPulseAlpha,
                    isWrongFlash = isWrongFlash,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 3.3 次世代 3D 水晶微倒角能量按鈕 (Options Grid & Yes/No Buttons)
                StroopOptionsArea(
                    question = question,
                    language = language,
                    onOptionSelected = onOptionSelected
                )

                Spacer(modifier = Modifier.height(14.dp))
            } else if (gameStatus == GameStatus.IDLE) {
                // 開局 Ready 科技簡報卡片
                StroopIdleBriefingCard(
                    difficulty = difficulty,
                    language = language,
                    onStartClick = onStartClick,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // 4. 結算成就成就面板 (Material 3 Dialog)
        if (gameStatus == GameStatus.COMPLETED) {
            StroopVictorySettlementDialog(
                score = score,
                correctCount = correctCount,
                wrongCount = wrongCount,
                maxCombo = maxCombo,
                language = language,
                onBackClick = onBackClick,
                onStartClick = onStartClick
            )
        }
    }
}

// -------------------------------------------------------------------------------------
// 1. 頂部導航列 (Top Bar)
// -------------------------------------------------------------------------------------
@Composable
private fun StroopTopBar(
    difficulty: GameDifficulty,
    language: AppLanguage,
    onBackClick: () -> Unit,
    onLeaderboardClick: () -> Unit,
    onResetClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左側：返回鍵 + 遊戲標題與難度徽章
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF1E293B).copy(alpha = 0.85f),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .size(42.dp)
                    .border(1.dp, Color(0xFF475569), CircleShape)
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFFE2E8F0)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = Localization.getString("game_stroop_effect", language),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFF8FAFC),
                        letterSpacing = 0.5.sp
                    )
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF8B5CF6).copy(alpha = 0.25f),
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                ) {
                    Text(
                        text = Localization.getString("diff_name_${difficulty.key.lowercase()}", language),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFFA78BFA),
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // 右側：排行榜按鈕與重整按鈕
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF1E293B).copy(alpha = 0.85f),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .size(42.dp)
                    .border(1.dp, Color(0xFF475569), CircleShape)
            ) {
                IconButton(onClick = onLeaderboardClick) {
                    Icon(
                        imageVector = Icons.Default.Leaderboard,
                        contentDescription = "Leaderboard",
                        tint = Color(0xFFFFD54F)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = CircleShape,
                color = Color(0xFF1E293B).copy(alpha = 0.85f),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .size(42.dp)
                    .border(1.dp, Color(0xFF475569), CircleShape)
            ) {
                IconButton(onClick = onResetClick) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset",
                        tint = Color(0xFF94A3B8)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// 2. 次世代全息電競 HUD 儀表板 (Cyber HUD Dashboard)
// -------------------------------------------------------------------------------------
@Composable
private fun StroopCyberHudDashboard(
    score: Int,
    combo: Int,
    remainingGameTimeMs: Long,
    totalSeconds: Long,
    questionTimeProgress: Float,
    language: AppLanguage
) {
    val isUrgentTime = remainingGameTimeMs < 10_000L

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF131B2A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.5.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF38BDF8).copy(alpha = 0.6f), Color(0xFF1E293B))
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 2.1 積分 (Score Capsule)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0F172A),
                        modifier = Modifier.border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
                            Text(
                                text = "SCORE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF94A3B8),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )
                            Text(
                                text = "$score",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF38BDF8),
                                    fontSize = 22.sp
                                )
                            )
                        }
                    }
                }

                // 2.2 超頻連擊徽章 (Overdrive Combo Badge)
                if (combo >= 2) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFEA580C),
                        shadowElevation = 6.dp,
                        modifier = Modifier.border(1.5.dp, Color(0xFFFDBA74), RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = "Combo",
                                tint = Color(0xFFFFF7ED),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "OVERDRIVE x$combo",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = Color(0xFFFFF7ED),
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }
                    }
                }

                // 2.3 剩餘總時間 (Countdown Capsule)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isUrgentTime) Color(0xFF7F1D1D) else Color(0xFF0F172A),
                    modifier = Modifier.border(
                        1.dp,
                        if (isUrgentTime) Color(0xFFEF4444) else Color(0xFF64748B),
                        RoundedCornerShape(12.dp)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = if (isUrgentTime) Color(0xFFFCA5A5) else Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${totalSeconds}s",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = if (isUrgentTime) Color(0xFFFEE2E2) else Color(0xFFF8FAFC)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2.4 單題限時流動霓虹能量管 (Neon Plasma Progress Tube)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(4.dp))
            ) {
                // 填充能量進度
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = questionTimeProgress)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF8B5CF6),
                                    Color(0xFF06B6D4),
                                    Color(0xFF10B981)
                                )
                            )
                        )
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// 3.1 全息任務指令指示光帶 (Instruction Holo-Banner)
// -------------------------------------------------------------------------------------
@Composable
private fun StroopInstructionHoloBanner(
    instruction: StroopInstruction,
    language: AppLanguage,
    holoPulseAlpha: Float
) {
    val isFocusingColor = instruction == StroopInstruction.CHOOSE_COLOR ||
            instruction == StroopInstruction.DISTRACTED_CHOOSE ||
            instruction == StroopInstruction.DYNAMIC_FAST
    val isFocusingText = instruction == StroopInstruction.CHOOSE_TEXT ||
            instruction == StroopInstruction.DISTRACTED_TEXT ||
            instruction == StroopInstruction.DYNAMIC_TEXT

    val bannerBorderColor = when {
        isFocusingColor -> Color(0xFF06B6D4)
        isFocusingText -> Color(0xFFF59E0B)
        instruction == StroopInstruction.COMPARE_EQUAL -> Color(0xFFA855F7)
        else -> Color(0xFF10B981)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.95f),
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                color = bannerBorderColor.copy(alpha = holoPulseAlpha),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = when {
                    isFocusingColor -> Icons.Default.ColorLens
                    isFocusingText -> Icons.Default.Translate
                    else -> Icons.Default.Psychology
                },
                contentDescription = null,
                tint = bannerBorderColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = Localization.getString(instruction.key, language),
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    letterSpacing = 0.3.sp
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

// -------------------------------------------------------------------------------------
// 3.2 賽博全息曜石浮空基座與 3D 立體液態霓虹字符
// -------------------------------------------------------------------------------------
@Composable
private fun StroopCyberStimulusStage(
    question: StroopQuestion,
    difficulty: GameDifficulty,
    language: AppLanguage,
    dynamicRotation: Float,
    holoPulseAlpha: Float,
    isWrongFlash: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .graphicsLayer {
                if (difficulty == GameDifficulty.HELL || difficulty == GameDifficulty.EPIC) {
                    rotationZ = dynamicRotation
                }
            }
            .shadow(16.dp, RoundedCornerShape(26.dp)),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 2.dp,
                    brush = Brush.verticalGradient(
                        colors = if (isWrongFlash) listOf(Color(0xFFEF4444), Color(0xFF991B1B))
                        else listOf(Color(0xFF38BDF8).copy(alpha = 0.8f), Color(0xFF1E293B))
                    ),
                    shape = RoundedCornerShape(26.dp)
                )
                .drawBehind {
                    drawHolographicPlatformGrid(holoPulseAlpha)
                },
            contentAlignment = Alignment.Center
        ) {
            if (question.isYesNoMode) {
                // 3.2.1 左右雙字對決模式 (ADVANCED COMPARE_EQUAL)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左字全息島 (Left Meaning)
                    CyberWordIsland(
                        label = "左 (字義)",
                        wordMeaning = question.leftWordMeaning,
                        displayColor = question.leftDisplayColor,
                        language = language
                    )

                    // VS 熾金電漿徽章
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF1E293B),
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .size(46.dp)
                            .border(2.dp, Color(0xFFFFD54F), CircleShape)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "VS",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFFFD54F)
                                )
                            )
                        }
                    }

                    // 右字全息島 (Right Color)
                    CyberWordIsland(
                        label = "右 (顏色)",
                        wordMeaning = question.rightWordMeaning,
                        displayColor = question.rightDisplayColor,
                        language = language
                    )
                }
            } else {
                // 3.2.2 單字 / 形狀 / 複合否定模式 (BEGINNER, INTERMEDIATE, HARD, HELL, EPIC)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // 形狀或排除條件提示標籤
                    if (question.shape != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E293B).copy(alpha = 0.9f),
                            modifier = Modifier
                                .padding(bottom = 8.dp)
                                .border(1.dp, Color(0xFF64748B), RoundedCornerShape(10.dp))
                        ) {
                            val shapePrefix = when (language) {
                                AppLanguage.TRADITIONAL_CHINESE -> "◈ 形狀："
                                AppLanguage.ENGLISH -> "◈ Shape: "
                                AppLanguage.JAPANESE -> "◈ 形状："
                                AppLanguage.KOREAN -> "◈ 도형: "
                            }
                            Text(
                                text = "$shapePrefix${question.shape.getName(language)}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = Color(0xFF38BDF8),
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (question.notConditionColor != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF450A0A).copy(alpha = 0.9f),
                            modifier = Modifier
                                .padding(bottom = 8.dp)
                                .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(10.dp))
                        ) {
                            val notPrefix = when (language) {
                                AppLanguage.TRADITIONAL_CHINESE -> "🚫 排除："
                                AppLanguage.ENGLISH -> "🚫 Exclude: "
                                AppLanguage.JAPANESE -> "🚫 除外："
                                AppLanguage.KOREAN -> "🚫 제외: "
                            }
                            Text(
                                text = "$notPrefix${question.notConditionColor.getName(language)}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFFFCA5A5),
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // 核心 3D 立體液態流動霓虹字符
                    LiquidNeonCharacter(
                        wordMeaning = question.mainWord,
                        displayColor = question.displayColor,
                        language = language
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// 3.2.1 雙字全息浮島 (Cyber Word Island)
// -------------------------------------------------------------------------------------
@Composable
private fun CyberWordIsland(
    label: String,
    wordMeaning: StroopColor?,
    displayColor: StroopColor?,
    language: AppLanguage
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF131C2E).copy(alpha = 0.9f),
        shadowElevation = 6.dp,
        modifier = Modifier
            .width(120.dp)
            .border(1.dp, Color(0xFF475569), RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(6.dp))

            val wordText = wordMeaning?.getName(language) ?: ""
            val wordColor = displayColor?.color ?: Color.White

            // 雙層立體發光字符
            Box(contentAlignment = Alignment.Center) {
                // 外層光暈
                Text(
                    text = wordText,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Black,
                        color = wordColor.copy(alpha = 0.4f),
                        fontSize = 38.sp
                    ),
                    modifier = Modifier.graphicsLayer { scaleX = 1.05f; scaleY = 1.05f }
                )
                // 核心字體
                Text(
                    text = wordText,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Black,
                        color = wordColor,
                        fontSize = 38.sp
                    )
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// 3.2.2 核心 3D 立體液態流動霓虹字符 (Liquid Neon Character)
// -------------------------------------------------------------------------------------
@Composable
private fun LiquidNeonCharacter(
    wordMeaning: StroopColor,
    displayColor: StroopColor,
    language: AppLanguage
) {
    val wordText = wordMeaning.getName(language)
    val neonColor = displayColor.color

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        // 第一層：廣域氛圍光暈 (Bloom 1)
        Text(
            text = wordText,
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Black,
                color = neonColor.copy(alpha = 0.25f),
                fontSize = 68.sp
            ),
            modifier = Modifier.graphicsLayer {
                scaleX = 1.15f
                scaleY = 1.15f
            }
        )

        // 第二層：飽和霓虹光環 (Bloom 2)
        Text(
            text = wordText,
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Black,
                color = neonColor.copy(alpha = 0.55f),
                fontSize = 68.sp
            ),
            modifier = Modifier.graphicsLayer {
                scaleX = 1.06f
                scaleY = 1.06f
            }
        )

        // 第三層：核心熾熱字芯 (Core)
        Text(
            text = wordText,
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Black,
                color = neonColor,
                fontSize = 68.sp,
                letterSpacing = 1.sp
            )
        )
    }
}

// -------------------------------------------------------------------------------------
// 3.3 次世代 3D 水晶微倒角能量按鈕 (Options Grid & Yes/No Buttons)
// -------------------------------------------------------------------------------------
@Composable
private fun StroopOptionsArea(
    question: StroopQuestion,
    language: AppLanguage,
    onOptionSelected: (StroopOption) -> Unit
) {
    if (question.isYesNoMode) {
        // 二選一（是 / 否）電競按鈕
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            question.options.forEach { option ->
                val isYes = option.id == "YES"
                val buttonBaseColor = if (isYes) Color(0xFF10B981) else Color(0xFFEF4444)
                val icon = if (isYes) Icons.Default.Check else Icons.Default.Close

                CrystalYesNoButton(
                    option = option,
                    language = language,
                    baseColor = buttonBaseColor,
                    icon = icon,
                    modifier = Modifier
                        .weight(1f)
                        .height(68.dp),
                    onClick = { onOptionSelected(option) }
                )
            }
        }
    } else {
        // 4 色/多色選項網格 (2x2)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val rows = question.options.chunked(2)
            rows.forEach { rowOptions ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowOptions.forEach { opt ->
                        val btnBgColor = if (opt.bgColor != Color.Unspecified) opt.bgColor else Color(0xFF1E293B)
                        CrystalOptionEnergyButton(
                            option = opt,
                            language = language,
                            baseColor = btnBgColor,
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp),
                            onClick = { onOptionSelected(opt) }
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// 水晶四選一能量按鈕 (Crystal Option Energy Button)
// -------------------------------------------------------------------------------------
@Composable
private fun CrystalOptionEnergyButton(
    option: StroopOption,
    language: AppLanguage,
    baseColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val labelText = option.getLabel(language)

    Box(
        modifier = modifier
            .scale(if (isPressed) 0.96f else 1f)
            .shadow(if (isPressed) 2.dp else 6.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .drawBehind {
                drawCrystalBeveledSurface(baseColor, isPressed)
            },
        contentAlignment = Alignment.Center
    ) {
        // 浮雕深色描邊大字，確保任何背景色皆清晰
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = labelText,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    color = Color.Black.copy(alpha = 0.5f)
                ),
                modifier = Modifier.graphicsLayer { translationY = 2f }
            )
            Text(
                text = labelText,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    color = Color.White
                )
            )
        }
    }
}

// -------------------------------------------------------------------------------------
// 水晶是/否電競按鈕 (Crystal Yes/No Button)
// -------------------------------------------------------------------------------------
@Composable
private fun CrystalYesNoButton(
    option: StroopOption,
    language: AppLanguage,
    baseColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val labelText = option.getLabel(language)

    Box(
        modifier = modifier
            .scale(if (isPressed) 0.96f else 1f)
            .shadow(if (isPressed) 2.dp else 8.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .drawBehind {
                drawCrystalBeveledSurface(baseColor, isPressed)
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = labelText,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = Color.White
                )
            )
        }
    }
}

// -------------------------------------------------------------------------------------
// 開局 Ready 科技簡報卡片 (Idle Briefing Card)
// -------------------------------------------------------------------------------------
@Composable
private fun StroopIdleBriefingCard(
    difficulty: GameDifficulty,
    language: AppLanguage,
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .shadow(12.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF0F172A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 1.5.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF8B5CF6).copy(alpha = 0.7f), Color(0xFF1E293B))
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 全息大腦突觸能量圖騰
            Surface(
                shape = CircleShape,
                color = Color(0xFF1E1B4B),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .size(76.dp)
                    .border(2.dp, Color(0xFF8B5CF6), CircleShape)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = Color(0xFFA78BFA),
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = Localization.getString("game_stroop_effect", language),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFF8FAFC)
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = Localization.getString("stroop_diff_${difficulty.key.lowercase()}_desc", language),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF94A3B8),
                    lineHeight = 20.sp
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 開始鍛鍊水晶按鈕
            Button(
                onClick = onStartClick,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = Localization.getString("start_game", language),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontSize = 17.sp
                    )
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// 4. 結算成就成就面板 (Material 3 Dialog)
// -------------------------------------------------------------------------------------
@Composable
private fun StroopVictorySettlementDialog(
    score: Int,
    correctCount: Int,
    wrongCount: Int,
    maxCombo: Int,
    language: AppLanguage,
    onBackClick: () -> Unit,
    onStartClick: () -> Unit
) {
    // 星級評價計算
    val stars = when {
        score >= 2500 && wrongCount <= 2 -> 3
        score >= 1500 && wrongCount <= 5 -> 2
        else -> 1
    }

    Dialog(onDismissRequest = { /* 必須點擊按鈕操作 */ }) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFF0F172A),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 2.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFFFD54F).copy(alpha = 0.8f), Color(0xFF1E293B))
                    ),
                    shape = RoundedCornerShape(26.dp)
                )
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 獎章圖示
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF451A03),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .size(68.dp)
                        .border(2.dp, Color(0xFFFFD54F), CircleShape)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 星星成就列
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..3) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (i <= stars) Color(0xFFFFD54F) else Color(0xFF334155),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = Localization.getString("game_completed", language),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "$score 分",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF38BDF8)
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 數據卡片
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E293B).copy(alpha = 0.8f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = Localization.getString("stroop_correct_count", language),
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF94A3B8))
                            )
                            Text(
                                text = "$correctCount 題",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = Localization.getString("stroop_wrong_count", language),
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF94A3B8))
                            )
                            Text(
                                text = "$wrongCount 題",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF4444)
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = Localization.getString("avatar_whack_combo", language),
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF94A3B8))
                            )
                            Text(
                                text = "最高 $maxCombo",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF97316)
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = Localization.getString("score_saved", language),
                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B))
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onBackClick,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = Localization.getString("back_to_menu", language),
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF94A3B8))
                        )
                    }

                    Button(
                        onClick = onStartClick,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = Localization.getString("play_again", language),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// 繪圖工具函式：全息競技場網格與微倒角能量表面
// -------------------------------------------------------------------------------------

/**
 * 繪製背景賽博競技場光學透視網格線
 */
private fun DrawScope.drawCyberArenaGridBackground() {
    val gridColor = Color(0xFF1E293B).copy(alpha = 0.25f)
    val step = 40.dp.toPx()

    // 垂直線
    var x = 0f
    while (x < size.width) {
        drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1f
        )
        x += step
    }

    // 水平線
    var y = 0f
    while (y < size.height) {
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f
        )
        y += step
    }
}

/**
 * 繪製全息基座內部的光學網格與四角瞄準角標
 */
private fun DrawScope.drawHolographicPlatformGrid(holoPulseAlpha: Float) {
    val reticleColor = Color(0xFF38BDF8).copy(alpha = holoPulseAlpha * 0.8f)
    val cornerLen = 16.dp.toPx()
    val margin = 8.dp.toPx()
    val strokeWidth = 2.dp.toPx()

    // 四角瞄準角標 [ ]
    // 左上
    drawLine(reticleColor, Offset(margin, margin), Offset(margin + cornerLen, margin), strokeWidth)
    drawLine(reticleColor, Offset(margin, margin), Offset(margin, margin + cornerLen), strokeWidth)
    // 右上
    drawLine(reticleColor, Offset(size.width - margin, margin), Offset(size.width - margin - cornerLen, margin), strokeWidth)
    drawLine(reticleColor, Offset(size.width - margin, margin), Offset(size.width - margin, margin + cornerLen), strokeWidth)
    // 左下
    drawLine(reticleColor, Offset(margin, size.height - margin), Offset(margin + cornerLen, size.height - margin), strokeWidth)
    drawLine(reticleColor, Offset(margin, size.height - margin), Offset(margin, size.height - margin - cornerLen), strokeWidth)
    // 右下
    drawLine(reticleColor, Offset(size.width - margin, size.height - margin), Offset(size.width - margin - cornerLen, size.height - margin), strokeWidth)
    drawLine(reticleColor, Offset(size.width - margin, size.height - margin), Offset(size.width - margin, size.height - margin - cornerLen), strokeWidth)
}

/**
 * 繪製 3D 水晶微倒角能量按鈕表面
 */
private fun DrawScope.drawCrystalBeveledSurface(baseColor: Color, isPressed: Boolean) {
    val corner = CornerRadius(18.dp.toPx(), 18.dp.toPx())

    // 1. 底層漸層
    val surfaceGradient = Brush.verticalGradient(
        colors = if (isPressed) {
            listOf(baseColor.copy(alpha = 0.85f), baseColor.copy(alpha = 0.65f))
        } else {
            listOf(baseColor, baseColor.copy(alpha = 0.82f))
        }
    )
    drawRoundRect(
        brush = surfaceGradient,
        size = size,
        cornerRadius = corner
    )

    // 2. 頂部拋光微高光晶透線 (Top Bevel Highlight)
    if (!isPressed) {
        val highlightPath = Path().apply {
            moveTo(14.dp.toPx(), 2.dp.toPx())
            lineTo(size.width - 14.dp.toPx(), 2.dp.toPx())
        }
        drawPath(
            path = highlightPath,
            color = Color.White.copy(alpha = 0.45f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }

    // 3. 雙層金屬外框 (Bevel Outer Rim)
    drawRoundRect(
        color = Color.White.copy(alpha = if (isPressed) 0.15f else 0.3f),
        size = size,
        cornerRadius = corner,
        style = Stroke(width = 1.2.dp.toPx())
    )
}

/**
 * 錯誤震動回饋輔助
 */
private fun triggerErrorVibration(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator?.vibrate(
                VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(120)
            }
        }
    } catch (_: Exception) {
    }
}
