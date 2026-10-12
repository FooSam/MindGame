package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.audio.SoundManager
import com.example.data.model.AppLanguage
import com.example.data.model.GameDifficulty
import com.example.data.model.Localization
import com.example.game.brainintrouble.BrainColor
import com.example.game.brainintrouble.BrainInTroubleConfig
import com.example.game.brainintrouble.BrainInTroubleGenerator
import com.example.game.brainintrouble.BrainShape
import com.example.game.brainintrouble.DropItem
import com.example.game.brainintrouble.Particle
import com.example.ui.components.AppBackground
import com.example.ui.components.formatTimeMillis
import com.example.ui.theme.AppThemeStyle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 依關卡難度與得分科學評定 1~3 顆 3D 金星勳章
 */
fun calculateBrainInTroubleStars(difficulty: GameDifficulty, score: Int): Int {
    return when (difficulty) {
        GameDifficulty.BEGINNER -> when {
            score >= 3500 -> 3
            score >= 2000 -> 2
            else -> 1
        }
        GameDifficulty.INTERMEDIATE -> when {
            score >= 5000 -> 3
            score >= 3000 -> 2
            else -> 1
        }
        GameDifficulty.ADVANCED -> when {
            score >= 7000 -> 3
            score >= 4500 -> 2
            else -> 1
        }
        GameDifficulty.HARD, GameDifficulty.HELL, GameDifficulty.EPIC -> when {
            score >= 9000 -> 3
            score >= 6000 -> 2
            else -> 1
        }
    }
}

/**
 * 計算左右腦多工神經平衡指數 (Neural Multi-Tasking Index)
 */
fun calculateNeuralBalanceIndex(leftHits: Int, rightHits: Int): Int {
    val total = leftHits + rightHits
    if (total <= 0) return 100
    val diff = kotlin.math.abs(leftHits - rightHits)
    val ratio = (diff.toFloat() / total.toFloat()) * 100f
    return (100 - ratio.toInt()).coerceIn(50, 100)
}

/**
 * 左右為難 (Brain in Trouble / Dual Track Reflex) 次世代 2024-2026 頂級視覺重構主畫面
 */
@Composable
fun BrainInTroubleScreen(
    difficulty: GameDifficulty,
    language: AppLanguage,
    appTheme: AppThemeStyle,
    onBackClick: () -> Unit,
    onLeaderboardClick: () -> Unit,
    onGameComplete: (score: Int, timeMs: Long) -> Unit,
    onGameInterrupted: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // 鎖定直屏，防止橫置時縱向下落距離過短
    DisposableEffect(Unit) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose {
            activity?.requestedOrientation = originalOrientation
        }
    }

    val config = remember(difficulty) { BrainInTroubleConfig.of(difficulty) }
    val textMeasurer = rememberTextMeasurer()

    // 遊戲狀態
    var lives by remember { mutableIntStateOf(3) }
    var score by remember { mutableIntStateOf(0) }
    var combo by remember { mutableIntStateOf(0) }
    var maxCombo by remember { mutableIntStateOf(0) }
    var leftHits by remember { mutableIntStateOf(0) }
    var rightHits by remember { mutableIntStateOf(0) }
    var isGameOver by remember { mutableStateOf(false) }
    var gameStartTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var totalPlayedTimeMs by remember { mutableLongStateOf(0L) }

    // 實體返回鍵雙重攔截，未結束退出時確保計入廣告投播中斷
    BackHandler {
        SoundManager.playClick()
        if (!isGameOver && lives > 0) {
            onGameInterrupted()
        }
        onBackClick()
    }

    // 掉落物與粒子列表
    val dropItems = remember { mutableStateListOf<DropItem>() }
    val particles = remember { mutableStateListOf<Particle>() }

    // 連擊與生命受傷動畫
    val heartScale = remember { Animatable(1f) }
    var gameFrameTick by remember { mutableLongStateOf(0L) }

    // 操作防抖與單幀原子鎖 (防止硬體觸控彈跳或同一幀內連鎖消除多個相同掉落物)
    var lastLeftActionTime by remember { mutableLongStateOf(0L) }
    var lastRightActionTime by remember { mutableLongStateOf(0L) }
    var lastLeftEliminatedFrame by remember { mutableLongStateOf(-1L) }
    var lastRightEliminatedFrame by remember { mutableLongStateOf(-1L) }

    // 震動回饋輔助
    fun triggerHaptic(durationMs: Long = 150L) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(durationMs)
                }
            }
        } catch (_: Exception) {}
    }

    // 爆炸粒子生成
    fun spawnParticles(x: Float, y: Float, color: Color, count: Int = 18) {
        for (i in 0 until count) {
            val angle = Math.random().toFloat() * 2f * PI.toFloat()
            val speed = (90f + Math.random().toFloat() * 260f)
            val vx = cos(angle) * speed
            val vy = sin(angle) * speed
            val size = (6f + Math.random().toFloat() * 9f)
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = vx,
                    vy = vy,
                    color = color,
                    size = size,
                    alpha = 1f,
                    life = 1f
                )
            )
        }
    }

    // 重新開始遊戲
    fun restartGame() {
        lives = 3
        score = 0
        combo = 0
        maxCombo = 0
        leftHits = 0
        rightHits = 0
        isGameOver = false
        lastLeftActionTime = 0L
        lastRightActionTime = 0L
        lastLeftEliminatedFrame = -1L
        lastRightEliminatedFrame = -1L
        dropItems.clear()
        particles.clear()
        BrainInTroubleGenerator.resetId()
        gameStartTime = System.currentTimeMillis()
    }

    // 玩家點擊左側幾何形狀按鈕
    fun onShapeButtonPressed(shape: BrainShape, halfWidth: Float, trackHeight: Float) {
        if (isGameOver) return
        val now = System.currentTimeMillis()
        // 防抖保護：同一側 120ms 內或同一物理幀已消除過時忽略，防止連鎖誤消多個相同形狀
        if (now - lastLeftActionTime < 120L || lastLeftEliminatedFrame == gameFrameTick) {
            return
        }
        lastLeftActionTime = now

        // 找到左側未消除、最靠近底線（progress 最大）的掉落物
        val target = dropItems
            .filter { it.isLeftSide && !it.isEliminated && !it.isMissed }
            .maxByOrNull { it.progress }

        if (target != null && target.shape == shape) {
            target.isEliminated = true
            lastLeftEliminatedFrame = gameFrameTick
            leftHits++
            combo++
            if (combo > maxCombo) maxCombo = combo
            val bonus = 100 + combo * 15
            score += bonus
            SoundManager.playImpactHit(combo)
            val startY = 48f
            val endY = (trackHeight - 56f).coerceAtLeast(startY)
            val targetY = startY + target.progress * (endY - startY)
            val laneWidth = halfWidth / 3f
            val targetX = (target.laneIndex.coerceIn(0, 2) + 0.5f) * laneWidth
            spawnParticles(targetX, targetY, Color(0xFF38BDF8), 20)
        } else {
            // 誤觸
            combo = 0
            SoundManager.playError()
        }
    }

    // 玩家點擊右側色彩按鈕
    fun onColorButtonPressed(color: BrainColor, halfWidth: Float, trackHeight: Float) {
        if (isGameOver) return
        val now = System.currentTimeMillis()
        // 防抖保護：同一側 120ms 內或同一物理幀已消除過時忽略，防止連鎖誤消多個相同顏色
        if (now - lastRightActionTime < 120L || lastRightEliminatedFrame == gameFrameTick) {
            return
        }
        lastRightActionTime = now

        // 找到右側未消除、最靠近底線（progress 最大）的掉落物
        val target = dropItems
            .filter { !it.isLeftSide && !it.isEliminated && !it.isMissed }
            .maxByOrNull { it.progress }

        if (target != null && target.targetColor == color) {
            target.isEliminated = true
            lastRightEliminatedFrame = gameFrameTick
            rightHits++
            combo++
            if (combo > maxCombo) maxCombo = combo
            val bonus = 100 + combo * 15
            score += bonus
            SoundManager.playImpactHit(combo)
            val startY = 48f
            val endY = (trackHeight - 56f).coerceAtLeast(startY)
            val targetY = startY + target.progress * (endY - startY)
            val laneWidth = halfWidth / 3f
            val targetX = halfWidth + (target.laneIndex.coerceIn(0, 2) + 0.5f) * laneWidth
            spawnParticles(targetX, targetY, color.color, 20)
        } else {
            // 誤觸
            combo = 0
            SoundManager.playError()
        }
    }

    // 核心物理與生成循環 (60/120 FPS 平滑連續運動)
    LaunchedEffect(isGameOver) {
        if (isGameOver) return@LaunchedEffect

        var lastFrameTimeNanos = 0L
        var spawnTimerMs = 0L
        var alternateLeft = true

        while (!isGameOver) {
            withFrameNanos { nowNanos ->
                if (lastFrameTimeNanos == 0L) {
                    lastFrameTimeNanos = nowNanos
                    return@withFrameNanos
                }
                val dtSec = ((nowNanos - lastFrameTimeNanos) / 1_000_000_000.0).toFloat().coerceIn(0f, 0.05f)
                val dtMs = (dtSec * 1000f).toLong()
                lastFrameTimeNanos = nowNanos

                // 1. 生成計時
                spawnTimerMs += dtMs
                if (spawnTimerMs >= config.spawnIntervalMs) {
                    spawnTimerMs = 0L
                    val newItem = BrainInTroubleGenerator.createDropItem(config, forceSide = alternateLeft)
                    alternateLeft = !alternateLeft
                    dropItems.add(newItem)
                }

                // 2. 更新掉落物位置
                val speed = 1.0f / (config.fallDurationMs / 1000f)
                val iterator = dropItems.iterator()
                while (iterator.hasNext()) {
                    val item = iterator.next()
                    if (item.isEliminated) {
                        iterator.remove()
                        continue
                    }
                    item.progress += speed * dtSec
                    // 觸底 Miss 判定
                    if (item.progress >= 1.0f && !item.isMissed) {
                        item.isMissed = true
                        iterator.remove()
                        // 扣命與震動
                        lives = (lives - 1).coerceAtLeast(0)
                        combo = 0
                        triggerHaptic(150L)
                        SoundManager.playDamageMiss()

                        if (lives <= 0) {
                            isGameOver = true
                            totalPlayedTimeMs = System.currentTimeMillis() - gameStartTime
                            onGameComplete(score, totalPlayedTimeMs)
                        }
                    }
                }

                // 3. 更新粒子
                val pIter = particles.iterator()
                while (pIter.hasNext()) {
                    val p = pIter.next()
                    p.x += p.vx * dtSec
                    p.y += p.vy * dtSec
                    p.life -= dtSec * 2.2f
                    p.alpha = p.life.coerceIn(0f, 1f)
                    if (p.life <= 0f) {
                        pIter.remove()
                    }
                }

                // 4. 驅動 Canvas Draw 管道在每幀硬體刷新 (60/120Hz) 平滑重繪，徹底消除跳格瞬移
                gameFrameTick = nowNanos
            }
        }
    }

    // 警戒底線脈衝動效
    val infiniteTransition = rememberInfiniteTransition(label = "LaserPulse")
    val laserGlow by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LaserGlow"
    )

    AppBackground(themeStyle = appTheme) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            // --- 頂部電競全息 HUD 儀表板 ---
            CyberDualTrackHud(
                difficulty = difficulty,
                language = language,
                lives = lives,
                heartScale = heartScale.value,
                score = score,
                combo = combo,
                isStroopConflict = config.isStroopConflict,
                onBackClick = {
                    SoundManager.playClick()
                    if (!isGameOver && lives > 0) {
                        onGameInterrupted()
                    }
                    onBackClick()
                },
                onLeaderboardClick = {
                    SoundManager.playClick()
                    onLeaderboardClick()
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // --- 中間雙軌量子能量戰場 (Quantum Battlefield) ---
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(
                        width = 1.8.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color(0xFF38BDF8).copy(alpha = 0.65f),
                                Color(0xFFA855F7).copy(alpha = 0.65f)
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
            ) {
                val totalWidth = constraints.maxWidth.toFloat()
                val trackHeight = constraints.maxHeight.toFloat()
                val halfWidth = totalWidth / 2f

                // 是否有即將越線的緊急物體
                val isLeftUrgent = dropItems.any { it.isLeftSide && it.progress > 0.75f && !it.isEliminated }
                val isRightUrgent = dropItems.any { !it.isLeftSide && it.progress > 0.75f && !it.isEliminated }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val _tick = gameFrameTick // 訂閱高精度物理幀，保證 60/120Hz 零掉幀平滑重繪

                    // 1. 繪製左右雙軌量子能量底盤與音遊導軌光柵
                    drawQuantumTrackBattlefield(
                        totalWidth = totalWidth,
                        trackHeight = trackHeight,
                        halfWidth = halfWidth
                    )

                    // 2. 底部全息量子防禦力場警戒判定線 (Plasma Barrier / Impact Line)
                    drawPlasmaBarrier(
                        totalWidth = totalWidth,
                        trackHeight = trackHeight,
                        halfWidth = halfWidth,
                        isLeftUrgent = isLeftUrgent,
                        isRightUrgent = isRightUrgent,
                        glowPulse = laserGlow
                    )

                    // 3. 繪製掉落物 (3大獨立跑道分流，3D符文晶石 & 3D果凍彩膠囊)
                    val startY = 48f
                    val endY = (trackHeight - 56f).coerceAtLeast(startY)
                    val laneWidth = halfWidth / 3f

                    dropItems.forEach { item ->
                        if (!item.isEliminated && !item.isMissed) {
                            val posY = startY + item.progress * (endY - startY)
                            val laneIdx = item.laneIndex.coerceIn(0, 2)
                            if (item.isLeftSide) {
                                val posX = (laneIdx + 0.5f) * laneWidth
                                item.shape?.let { shape ->
                                    drawStylized3DRuneCrystal(
                                        shape = shape,
                                        centerX = posX,
                                        centerY = posY,
                                        radius = 38f
                                    )
                                }
                            } else {
                                val posX = halfWidth + (laneIdx + 0.5f) * laneWidth
                                item.visualColor?.let { visualColor ->
                                    drawStylized3DColorPill(
                                        color = visualColor,
                                        text = item.textLabel,
                                        centerX = posX,
                                        centerY = posY,
                                        textMeasurer = textMeasurer
                                    )
                                }
                            }
                        }
                    }

                    // 4. 繪製爆炸消散衝擊粒子與火花
                    particles.forEach { p ->
                        // 粒子本體
                        drawCircle(
                            color = p.color.copy(alpha = p.alpha * 0.95f),
                            radius = p.size,
                            center = Offset(p.x, p.y)
                        )
                        // 粒子高光核心
                        if (p.size > 4f) {
                            drawCircle(
                                color = Color.White.copy(alpha = p.alpha * 0.8f),
                                radius = p.size * 0.4f,
                                center = Offset(p.x - 1f, p.y - 1f)
                            )
                        }
                    }
                }

                // 雙軌頂部光學標籤 (微倒角全息膠囊浮水印)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0369A1).copy(alpha = 0.28f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = "◀ 左腦・幾何認知",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF7E22CE).copy(alpha = 0.28f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = "右腦・色彩光學 ▶",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFA855F7),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- 底部 3D 水晶微倒角電競雙手控制器按鈕 ---
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val totalW = constraints.maxWidth.toFloat()
                val trackH = 300f // 基準估算高度
                val halfW = totalW / 2f

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 左手形狀按鈕群 (佔比 50%)
                    Box(modifier = Modifier.weight(1f)) {
                        ShapeButtonGroup(
                            shapes = config.leftShapes,
                            onShapeClick = { shape ->
                                onShapeButtonPressed(shape, halfW, trackH)
                            }
                        )
                    }

                    // 右手顏色按鈕群 (佔比 50%)
                    Box(modifier = Modifier.weight(1f)) {
                        ColorButtonGroup(
                            colors = config.rightColors,
                            onColorClick = { color ->
                                onColorButtonPressed(color, halfW, trackH)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }

    // --- 次世代手遊成就 3 星結算面板 ---
    if (isGameOver) {
        BrainInTroubleVictoryDialog(
            score = score,
            maxCombo = maxCombo,
            timeMs = totalPlayedTimeMs,
            leftHits = leftHits,
            rightHits = rightHits,
            difficulty = difficulty,
            language = language,
            onRestart = { restartGame() },
            onLeaderboard = { onLeaderboardClick() },
            onBack = { onBackClick() }
        )
    }
}

/**
 * 頂部電競全息 HUD 儀表板
 */
@Composable
private fun CyberDualTrackHud(
    difficulty: GameDifficulty,
    language: AppLanguage,
    lives: Int,
    heartScale: Float,
    score: Int,
    combo: Int,
    isStroopConflict: Boolean,
    onBackClick: () -> Unit,
    onLeaderboardClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左側返回與標題
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF0F172A).copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF334155)),
                    modifier = Modifier.size(40.dp)
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = Localization.getString("brain_in_trouble_title", language),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF38BDF8).copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = Localization.getString(difficulty.key.lowercase(), language),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            // 右側得分與排行榜
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 燙金全息記分牌
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF0B132B).copy(alpha = 0.9f),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.5.dp,
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFFFFD54F), Color(0xFFFFA000))
                        )
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "$score",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFFD54F),
                                fontFamily = FontFamily.Monospace
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "PTS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFE082),
                                fontSize = 9.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                    shape = CircleShape,
                    color = Color(0xFF0F172A).copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF334155)),
                    modifier = Modifier.size(40.dp)
                ) {
                    IconButton(onClick = onLeaderboardClick) {
                        Icon(
                            imageVector = Icons.Default.Leaderboard,
                            contentDescription = "Leaderboard",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 下排：3D 水晶生命心核與連擊指示
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 3 顆立體水晶生命指示
            Row(verticalAlignment = Alignment.CenterVertically) {
                for (i in 1..3) {
                    val isAlive = i <= lives
                    Surface(
                        shape = CircleShape,
                        color = if (isAlive) Color(0xFF881337).copy(alpha = 0.35f) else Color(0xFF0F172A).copy(alpha = 0.6f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isAlive) Color(0xFFF43F5E) else Color(0xFF334155)
                        ),
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isAlive) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Heart",
                                tint = if (isAlive) Color(0xFFFF2A6D) else Color(0xFF475569),
                                modifier = Modifier
                                    .size(15.dp)
                                    .scale(if (isAlive) heartScale else 0.85f)
                            )
                        }
                    }
                }
            }

            // 超頻連擊指示
            if (combo >= 3) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFF5722).copy(alpha = 0.25f),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFFF7043))
                ) {
                    Text(
                        text = "🔥 OVERDRIVE x$combo",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFB74D),
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            } else {
                // 提示全息光膠囊
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E293B).copy(alpha = 0.6f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.6f))
                ) {
                    Text(
                        text = if (isStroopConflict) {
                            Localization.getString("brain_in_trouble_stroop_rule", language)
                        } else {
                            Localization.getString("brain_in_trouble_hint", language)
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isStroopConflict) Color(0xFFFF8A80) else Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            fontWeight = if (isStroopConflict) FontWeight.Bold else FontWeight.Normal
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

/**
 * 繪製雙軌量子能量音遊戰場與高科技導軌光柵
 */
private fun DrawScope.drawQuantumTrackBattlefield(
    totalWidth: Float,
    trackHeight: Float,
    halfWidth: Float
) {
    // 1. 左側量子軌道漸層底板 (電光藍至鈦青)
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF07192A).copy(alpha = 0.95f),
                Color(0xFF0F2B48).copy(alpha = 0.70f)
            ),
            startX = 0f,
            endX = halfWidth
        ),
        size = Size(halfWidth, trackHeight)
    )

    // 2. 右側量子軌道漸層底板 (極光紫至深緋紅)
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF1E0B36).copy(alpha = 0.70f),
                Color(0xFF3B0764).copy(alpha = 0.95f)
            ),
            startX = halfWidth,
            endX = totalWidth
        ),
        topLeft = Offset(halfWidth, 0f),
        size = Size(halfWidth, trackHeight)
    )

    // 3. 繪製左右兩側各 3 條獨立跑道導軌光柵與縱向音遊刻度線
    val laneWidth = halfWidth / 3f
    val cyanRailColor = Color(0xFF38BDF8).copy(alpha = 0.16f)
    val purpleRailColor = Color(0xFFA855F7).copy(alpha = 0.16f)

    // 左側 2 條內部跑道分隔線
    for (i in 1..2) {
        val x = i * laneWidth
        drawLine(
            color = cyanRailColor,
            start = Offset(x, 0f),
            end = Offset(x, trackHeight),
            strokeWidth = 1.2f,
            cap = StrokeCap.Round
        )
        // 刻度點 (25%, 50%, 75%)
        for (step in 1..3) {
            val y = trackHeight * (step * 0.25f)
            drawLine(
                color = Color(0xFF38BDF8).copy(alpha = 0.35f),
                start = Offset(x - 4f, y),
                end = Offset(x + 4f, y),
                strokeWidth = 1.5f
            )
        }
    }

    // 右側 2 條內部跑道分隔線
    for (i in 1..2) {
        val x = halfWidth + i * laneWidth
        drawLine(
            color = purpleRailColor,
            start = Offset(x, 0f),
            end = Offset(x, trackHeight),
            strokeWidth = 1.2f,
            cap = StrokeCap.Round
        )
        // 刻度點 (25%, 50%, 75%)
        for (step in 1..3) {
            val y = trackHeight * (step * 0.25f)
            drawLine(
                color = Color(0xFFA855F7).copy(alpha = 0.35f),
                start = Offset(x - 4f, y),
                end = Offset(x + 4f, y),
                strokeWidth = 1.5f
            )
        }
    }

    // 4. 中央雙層量子能量分隔柱 (Central Quantum Energy Pillar)
    // 外層等離子光暈
    drawLine(
        brush = Brush.verticalGradient(
            listOf(
                Color(0xFF38BDF8).copy(alpha = 0.15f),
                Color(0xFFA855F7).copy(alpha = 0.35f),
                Color(0xFFF43F5E).copy(alpha = 0.15f)
            )
        ),
        start = Offset(halfWidth, 0f),
        end = Offset(halfWidth, trackHeight),
        strokeWidth = 8f
    )
    // 中層高亮能量束
    drawLine(
        brush = Brush.verticalGradient(
            listOf(
                Color(0xFF38BDF8),
                Color(0xFFA855F7),
                Color(0xFFEC4899)
            )
        ),
        start = Offset(halfWidth, 0f),
        end = Offset(halfWidth, trackHeight),
        strokeWidth = 2.8f
    )
    // 核心熾白能量芯
    drawLine(
        color = Color.White.copy(alpha = 0.85f),
        start = Offset(halfWidth, 0f),
        end = Offset(halfWidth, trackHeight),
        strokeWidth = 1f
    )
}

/**
 * 繪製底部全息量子防禦力場警戒線 (Holographic Plasma Barrier / Impact Line)
 */
private fun DrawScope.drawPlasmaBarrier(
    totalWidth: Float,
    trackHeight: Float,
    halfWidth: Float,
    isLeftUrgent: Boolean,
    isRightUrgent: Boolean,
    glowPulse: Float
) {
    val lineY = trackHeight - 8f
    val leftColor = if (isLeftUrgent) Color(0xFFFF1744) else Color(0xFF38BDF8)
    val rightColor = if (isRightUrgent) Color(0xFFFF1744) else Color(0xFFA855F7)

    // 左側防禦力場
    val leftAlpha = if (isLeftUrgent) glowPulse else 0.75f
    // 外發光
    drawLine(
        color = leftColor.copy(alpha = leftAlpha * 0.45f),
        start = Offset(10f, lineY),
        end = Offset(halfWidth - 8f, lineY),
        strokeWidth = if (isLeftUrgent) 8f else 5f,
        cap = StrokeCap.Round
    )
    // 核心高光線
    drawLine(
        color = leftColor.copy(alpha = leftAlpha),
        start = Offset(10f, lineY),
        end = Offset(halfWidth - 8f, lineY),
        strokeWidth = if (isLeftUrgent) 3.5f else 2.5f,
        cap = StrokeCap.Round
    )
    // 左端金屬發射基座
    drawCircle(color = Color(0xFF1E293B), radius = 4.5f, center = Offset(10f, lineY))
    drawCircle(color = leftColor, radius = 2.5f, center = Offset(10f, lineY))

    // 右側防禦力場
    val rightAlpha = if (isRightUrgent) glowPulse else 0.75f
    // 外發光
    drawLine(
        color = rightColor.copy(alpha = rightAlpha * 0.45f),
        start = Offset(halfWidth + 8f, lineY),
        end = Offset(totalWidth - 10f, lineY),
        strokeWidth = if (isRightUrgent) 8f else 5f,
        cap = StrokeCap.Round
    )
    // 核心高光線
    drawLine(
        color = rightColor.copy(alpha = rightAlpha),
        start = Offset(halfWidth + 8f, lineY),
        end = Offset(totalWidth - 10f, lineY),
        strokeWidth = if (isRightUrgent) 3.5f else 2.5f,
        cap = StrokeCap.Round
    )
    // 右端金屬發射基座
    drawCircle(color = Color(0xFF1E293B), radius = 4.5f, center = Offset(totalWidth - 10f, lineY))
    drawCircle(color = rightColor, radius = 2.5f, center = Offset(totalWidth - 10f, lineY))
}

/**
 * 繪製左側次世代 3D 發光幾何符文晶石 (3D Beveled Geometric Rune Crystal)
 */
private fun DrawScope.drawStylized3DRuneCrystal(
    shape: BrainShape,
    centerX: Float,
    centerY: Float,
    radius: Float = 38f
) {
    val cyanGlow = Color(0xFF38BDF8)
    val darkBlueBg = Color(0xFF0369A1)
    val deepNavy = Color(0xFF0B192C)

    // 1. 外層全息柔和發光光暈 (Bloom Aura)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                cyanGlow.copy(alpha = 0.45f),
                cyanGlow.copy(alpha = 0.15f),
                Color.Transparent
            ),
            center = Offset(centerX, centerY),
            radius = radius * 1.55f
        ),
        radius = radius * 1.5f,
        center = Offset(centerX, centerY)
    )

    // 2. 下沉式微倒角深鈦金屬底盤 (Sunken Obsidian Mount)
    drawCircle(
        brush = Brush.verticalGradient(
            colors = listOf(deepNavy, Color(0xFF040D1A)),
            startY = centerY - radius * 1.25f,
            endY = centerY + radius * 1.25f
        ),
        radius = radius * 1.22f,
        center = Offset(centerX, centerY)
    )

    // 外圈金屬倒角高光線條
    drawCircle(
        color = cyanGlow.copy(alpha = 0.55f),
        radius = radius * 1.22f,
        center = Offset(centerX, centerY),
        style = Stroke(width = 1.8f)
    )

    // 3. 核心 3D 發光幾何符文雕繪 (立體微斜切面與高光)
    when (shape) {
        BrainShape.CIRCLE -> {
            // 球體 3D 天光漸層
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFE0F2FE),
                        cyanGlow,
                        Color(0xFF0284C7),
                        darkBlueBg
                    ),
                    center = Offset(centerX - radius * 0.28f, centerY - radius * 0.28f),
                    radius = radius * 0.95f
                ),
                radius = radius * 0.78f,
                center = Offset(centerX, centerY)
            )
            // 頂部純白聚光星芒
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = radius * 0.22f,
                center = Offset(centerX - radius * 0.26f, centerY - radius * 0.26f)
            )
        }

        BrainShape.SQUARE -> {
            val half = radius * 0.72f
            // 立體微倒角外框
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF7DD3FC), cyanGlow, Color(0xFF0284C7)),
                    startY = centerY - half,
                    endY = centerY + half
                ),
                topLeft = Offset(centerX - half, centerY - half),
                size = Size(half * 2f, half * 2f),
                cornerRadius = CornerRadius(10f, 10f)
            )
            // 頂面內嵌晶片浮雕
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFFE0F2FE).copy(alpha = 0.75f), cyanGlow.copy(alpha = 0.35f)),
                    startY = centerY - half + 4f,
                    endY = centerY + half - 4f
                ),
                topLeft = Offset(centerX - half + 4f, centerY - half + 4f),
                size = Size((half - 4f) * 2f, (half - 4f) * 2f),
                cornerRadius = CornerRadius(6f, 6f)
            )
            // 左上角高光亮線
            drawLine(
                color = Color.White.copy(alpha = 0.85f),
                start = Offset(centerX - half + 6f, centerY - half + 4f),
                end = Offset(centerX + half - 6f, centerY - half + 4f),
                strokeWidth = 2f
            )
        }

        BrainShape.TRIANGLE -> {
            // 三棱錐立體切面
            val topVertex = Offset(centerX, centerY - radius * 0.88f)
            val leftVertex = Offset(centerX - radius * 0.82f, centerY + radius * 0.72f)
            val rightVertex = Offset(centerX + radius * 0.82f, centerY + radius * 0.72f)

            // 左側受光面 (高亮藍)
            val leftPath = Path().apply {
                moveTo(topVertex.x, topVertex.y)
                lineTo(leftVertex.x, leftVertex.y)
                lineTo(centerX, centerY + radius * 0.25f)
                close()
            }
            drawPath(
                path = leftPath,
                brush = Brush.linearGradient(
                    listOf(Color(0xFFBAE6FD), cyanGlow),
                    start = topVertex,
                    end = leftVertex
                )
            )

            // 右側背光面 (深鈦藍)
            val rightPath = Path().apply {
                moveTo(topVertex.x, topVertex.y)
                lineTo(rightVertex.x, rightVertex.y)
                lineTo(centerX, centerY + radius * 0.25f)
                close()
            }
            drawPath(
                path = rightPath,
                brush = Brush.linearGradient(
                    listOf(cyanGlow, Color(0xFF0369A1)),
                    start = topVertex,
                    end = rightVertex
                )
            )

            // 頂角聚光點
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = 3.5f,
                center = topVertex
            )
        }

        BrainShape.DIAMOND -> {
            val topP = Offset(centerX, centerY - radius * 0.90f)
            val rightP = Offset(centerX + radius * 0.82f, centerY)
            val bottomP = Offset(centerX, centerY + radius * 0.90f)
            val leftP = Offset(centerX - radius * 0.82f, centerY)
            val centerP = Offset(centerX, centerY)

            // 4 面鑽石寶石切面
            // 左上面 (最亮)
            val p1 = Path().apply {
                moveTo(topP.x, topP.y); lineTo(leftP.x, leftP.y); lineTo(centerP.x, centerP.y); close()
            }
            drawPath(path = p1, brush = Brush.linearGradient(listOf(Color(0xFFE0F2FE), cyanGlow), start = topP, end = centerP))

            // 右上面
            val p2 = Path().apply {
                moveTo(topP.x, topP.y); lineTo(rightP.x, rightP.y); lineTo(centerP.x, centerP.y); close()
            }
            drawPath(path = p2, brush = Brush.linearGradient(listOf(cyanGlow, Color(0xFF0284C7)), start = topP, end = centerP))

            // 左下面
            val p3 = Path().apply {
                moveTo(bottomP.x, bottomP.y); lineTo(leftP.x, leftP.y); lineTo(centerP.x, centerP.y); close()
            }
            drawPath(path = p3, brush = Brush.linearGradient(listOf(cyanGlow, Color(0xFF0369A1)), start = centerP, end = bottomP))

            // 右下面 (最暗)
            val p4 = Path().apply {
                moveTo(bottomP.x, bottomP.y); lineTo(rightP.x, rightP.y); lineTo(centerP.x, centerP.y); close()
            }
            drawPath(path = p4, brush = Brush.linearGradient(listOf(Color(0xFF0284C7), Color(0xFF075985)), start = centerP, end = bottomP))

            // 中心晶線高光
            drawCircle(color = Color.White.copy(alpha = 0.95f), radius = 3.5f, center = centerP)
        }

        BrainShape.STAR -> {
            val path = Path()
            val points = 5
            val outerR = radius * 0.90f
            val innerR = radius * 0.42f
            for (i in 0 until points * 2) {
                val r = if (i % 2 == 0) outerR else innerR
                val angle = (i * PI / points - PI / 2).toFloat()
                val x = centerX + r * cos(angle)
                val y = centerY + r * sin(angle)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(
                path = path,
                brush = Brush.radialGradient(
                    listOf(Color(0xFFE0F2FE), cyanGlow, Color(0xFF0284C7)),
                    center = Offset(centerX, centerY),
                    radius = outerR
                )
            )
            // 中心星核爆發高光
            drawCircle(
                color = Color.White.copy(alpha = 0.95f),
                radius = radius * 0.26f,
                center = Offset(centerX, centerY)
            )
        }

        BrainShape.HEXAGON -> {
            val path = Path()
            val sides = 6
            for (i in 0 until sides) {
                val angle = (i * 2 * PI / sides).toFloat()
                val x = centerX + radius * 0.82f * cos(angle)
                val y = centerY + radius * 0.82f * sin(angle)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(
                path = path,
                brush = Brush.radialGradient(
                    listOf(Color(0xFFBAE6FD), cyanGlow, Color(0xFF0284C7)),
                    center = Offset(centerX, centerY),
                    radius = radius * 0.85f
                )
            )
            // 核心科技同心環
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = radius * 0.35f,
                center = Offset(centerX, centerY),
                style = Stroke(width = 2.2f)
            )
            drawCircle(
                color = Color.White,
                radius = radius * 0.16f,
                center = Offset(centerX, centerY)
            )
        }
    }
}

/**
 * 繪製右側次世代 3D 水晶果凍彩晶能量膠囊 (3D Crystal Jelly Energy Pill)
 */
private fun DrawScope.drawStylized3DColorPill(
    color: BrainColor,
    text: String?,
    centerX: Float,
    centerY: Float,
    textMeasurer: TextMeasurer
) {
    val pillWidth = if (text != null) 124f else 96f
    val pillHeight = 68f
    val left = centerX - pillWidth / 2f
    val top = centerY - pillHeight / 2f

    // 1. 外層全息柔和發光 (Bloom Glow)
    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(
                color.color.copy(alpha = 0.55f),
                color.color.copy(alpha = 0.15f),
                Color.Transparent
            ),
            center = Offset(centerX, centerY),
            radius = pillWidth * 0.75f
        ),
        topLeft = Offset(left - 8f, top - 8f),
        size = Size(pillWidth + 16f, pillHeight + 16f),
        cornerRadius = CornerRadius(22f, 22f)
    )

    // 2. 實體 3D 果凍琉璃晶體漸層底板
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                color.color.copy(alpha = 0.95f),
                color.color.copy(alpha = 0.80f),
                Color.Black.copy(alpha = 0.30f)
            ),
            startY = top,
            endY = top + pillHeight
        ),
        topLeft = Offset(left, top),
        size = Size(pillWidth, pillHeight),
        cornerRadius = CornerRadius(18f, 18f)
    )

    // 3. 頂部弧形鏡面反光帶 (Glass Specular Sheen Sweep)
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.55f),
                Color.White.copy(alpha = 0.05f)
            ),
            startY = top + 2f,
            endY = top + pillHeight * 0.48f
        ),
        topLeft = Offset(left + 3f, top + 2f),
        size = Size(pillWidth - 6f, pillHeight * 0.44f),
        cornerRadius = CornerRadius(14f, 14f)
    )

    // 4. 微倒角高光輪廓線
    drawRoundRect(
        color = Color.White.copy(alpha = 0.70f),
        topLeft = Offset(left, top),
        size = Size(pillWidth, pillHeight),
        cornerRadius = CornerRadius(18f, 18f),
        style = Stroke(width = 2.2f)
    )

    // 5. 3D 浮雕文字繪製 (Stroop 文字或同色字，具備深色微立體陰影)
    if (!text.isNullOrEmpty()) {
        // 陰影層
        val shadowResult = textMeasurer.measure(
            text = text,
            style = TextStyle(
                color = Color.Black.copy(alpha = 0.85f),
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )
        )
        val textX = centerX - shadowResult.size.width / 2f
        val textY = centerY - shadowResult.size.height / 2f
        drawText(
            textLayoutResult = shadowResult,
            topLeft = Offset(textX + 1.8f, textY + 1.8f)
        )

        // 頂層白字
        val mainTextResult = textMeasurer.measure(
            text = text,
            style = TextStyle(
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )
        )
        drawText(
            textLayoutResult = mainTextResult,
            topLeft = Offset(textX, textY)
        )
    }
}

/**
 * 左手形狀按鈕群組 (自適應單排或雙排)
 */
@Composable
private fun ShapeButtonGroup(
    shapes: List<BrainShape>,
    onShapeClick: (BrainShape) -> Unit
) {
    val rows = if (shapes.size <= 3) {
        listOf(shapes)
    } else {
        val firstRowCount = if (shapes.size == 4) 2 else 3
        listOf(shapes.take(firstRowCount), shapes.drop(firstRowCount))
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        rows.forEach { rowShapes ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowShapes.forEach { shape ->
                    Box(modifier = Modifier.weight(1f)) {
                        ShapeButton(shape = shape, onClick = { onShapeClick(shape) })
                    }
                }
            }
        }
    }
}

/**
 * 3D 水晶微倒角電競形狀按鈕
 */
@Composable
private fun ShapeButton(
    shape: BrainShape,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .offset(y = if (isPressed) 2.dp else 0.dp)
            .scale(if (isPressed) 0.94f else 1.0f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(14.dp),
        color = if (isPressed) Color(0xFF0369A1) else Color(0xFF0F172A),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isPressed) 2.2.dp else 1.4.dp,
            color = if (isPressed) Color(0xFF7DD3FC) else Color(0xFF1E293B)
        ),
        shadowElevation = if (isPressed) 1.dp else 4.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(28.dp)) {
                val radius = size.minDimension / 2f
                val centerX = size.width / 2f
                val centerY = size.height / 2f
                val color = if (isPressed) Color.White else Color(0xFF38BDF8)

                when (shape) {
                    BrainShape.CIRCLE -> {
                        drawCircle(color = color, radius = radius * 0.8f, center = Offset(centerX, centerY))
                        drawCircle(color = Color.White.copy(alpha = 0.7f), radius = radius * 0.28f, center = Offset(centerX - 3f, centerY - 3f))
                    }
                    BrainShape.SQUARE -> {
                        val h = radius * 0.75f
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(centerX - h, centerY - h),
                            size = Size(h * 2f, h * 2f),
                            cornerRadius = CornerRadius(5f, 5f)
                        )
                    }
                    BrainShape.TRIANGLE -> {
                        val path = Path().apply {
                            moveTo(centerX, centerY - radius * 0.85f)
                            lineTo(centerX + radius * 0.8f, centerY + radius * 0.75f)
                            lineTo(centerX - radius * 0.8f, centerY + radius * 0.75f)
                            close()
                        }
                        drawPath(path = path, color = color)
                    }
                    BrainShape.DIAMOND -> {
                        val path = Path().apply {
                            moveTo(centerX, centerY - radius * 0.85f)
                            lineTo(centerX + radius * 0.75f, centerY)
                            lineTo(centerX, centerY + radius * 0.85f)
                            lineTo(centerX - radius * 0.75f, centerY)
                            close()
                        }
                        drawPath(path = path, color = color)
                    }
                    BrainShape.STAR -> {
                        val path = Path()
                        val points = 5
                        val outerR = radius * 0.85f
                        val innerR = radius * 0.38f
                        for (i in 0 until points * 2) {
                            val r = if (i % 2 == 0) outerR else innerR
                            val angle = (i * PI / points - PI / 2).toFloat()
                            val x = centerX + r * cos(angle)
                            val y = centerY + r * sin(angle)
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        path.close()
                        drawPath(path = path, color = color)
                    }
                    BrainShape.HEXAGON -> {
                        val path = Path()
                        val sides = 6
                        for (i in 0 until sides) {
                            val angle = (i * 2 * PI / sides).toFloat()
                            val x = centerX + radius * 0.75f * cos(angle)
                            val y = centerY + radius * 0.75f * sin(angle)
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        path.close()
                        drawPath(path = path, color = color)
                    }
                }
            }
        }
    }
}

/**
 * 右手顏色按鈕群組 (自適應單排或雙排)
 */
@Composable
private fun ColorButtonGroup(
    colors: List<BrainColor>,
    onColorClick: (BrainColor) -> Unit
) {
    val rows = if (colors.size <= 3) {
        listOf(colors)
    } else {
        val firstRowCount = if (colors.size == 4) 2 else 3
        listOf(colors.take(firstRowCount), colors.drop(firstRowCount))
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        rows.forEach { rowColors ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowColors.forEach { color ->
                    Box(modifier = Modifier.weight(1f)) {
                        ColorButton(color = color, onClick = { onColorClick(color) })
                    }
                }
            }
        }
    }
}

/**
 * 3D 水晶微倒角電競色彩按鈕
 */
@Composable
private fun ColorButton(
    color: BrainColor,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .offset(y = if (isPressed) 2.dp else 0.dp)
            .scale(if (isPressed) 0.94f else 1.0f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(14.dp),
        color = color.color.copy(alpha = if (isPressed) 0.95f else 0.82f),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isPressed) 2.2.dp else 1.5.dp,
            color = Color.White.copy(alpha = if (isPressed) 0.95f else 0.55f)
        ),
        shadowElevation = if (isPressed) 1.dp else 4.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = color.displayNameZh,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            )
        }
    }
}

/**
 * 次世代手遊成就 3 星結算面板 (BrainInTroubleVictoryDialog)
 */
@Composable
private fun BrainInTroubleVictoryDialog(
    score: Int,
    maxCombo: Int,
    timeMs: Long,
    leftHits: Int,
    rightHits: Int,
    difficulty: GameDifficulty,
    language: AppLanguage,
    onRestart: () -> Unit,
    onLeaderboard: () -> Unit,
    onBack: () -> Unit
) {
    val stars = calculateBrainInTroubleStars(difficulty, score)
    val balanceIndex = calculateNeuralBalanceIndex(leftHits, rightHits)

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.78f))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = androidx.compose.foundation.BorderStroke(
                    width = 2.dp,
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFFFFD54F), Color(0xFF38BDF8), Color(0xFFA855F7))
                    )
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 頂部大徽章
                    Text(
                        text = "💥 雙腦極限測驗結算 💥",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF38BDF8)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 1~3 顆 3D 炫金成就星章
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 1..3) {
                            val earned = i <= stars
                            Canvas(modifier = Modifier.size(if (i == 2) 48.dp else 40.dp)) {
                                val r = size.minDimension / 2f
                                val cx = size.width / 2f
                                val cy = size.height / 2f

                                if (earned) {
                                    // 外層金色星芒光暈
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(Color(0xFFFFD54F).copy(alpha = 0.5f), Color.Transparent),
                                            center = Offset(cx, cy),
                                            radius = r * 1.3f
                                        ),
                                        radius = r * 1.25f,
                                        center = Offset(cx, cy)
                                    )
                                    // 3D 星星切面
                                    val path = Path()
                                    val points = 5
                                    val outerR = r * 0.95f
                                    val innerR = r * 0.42f
                                    for (p in 0 until points * 2) {
                                        val rad = if (p % 2 == 0) outerR else innerR
                                        val angle = (p * PI / points - PI / 2).toFloat()
                                        val x = cx + rad * cos(angle)
                                        val y = cy + rad * sin(angle)
                                        if (p == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                    }
                                    path.close()
                                    drawPath(
                                        path = path,
                                        brush = Brush.verticalGradient(
                                            listOf(Color(0xFFFFF9C4), Color(0xFFFFD54F), Color(0xFFB58107)),
                                            startY = cy - r,
                                            endY = cy + r
                                        )
                                    )
                                    // 頂部高光星芒點
                                    drawCircle(color = Color.White, radius = 3.5f, center = Offset(cx, cy - r * 0.45f))
                                } else {
                                    // 未解鎖暗星
                                    val path = Path()
                                    val points = 5
                                    val outerR = r * 0.90f
                                    val innerR = r * 0.40f
                                    for (p in 0 until points * 2) {
                                        val rad = if (p % 2 == 0) outerR else innerR
                                        val angle = (p * PI / points - PI / 2).toFloat()
                                        val x = cx + rad * cos(angle)
                                        val y = cy + rad * sin(angle)
                                        if (p == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                    }
                                    path.close()
                                    drawPath(path = path, color = Color(0xFF334155))
                                }
                            }
                        }
                    }

                    // 星級評價名稱
                    val rankTitleKey = when (stars) {
                        3 -> "brain_in_trouble_rank_3star"
                        2 -> "brain_in_trouble_rank_2star"
                        else -> "brain_in_trouble_rank_1star"
                    }
                    Text(
                        text = Localization.getString(rankTitleKey, language),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFFD54F)
                        ),
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 得分大銘牌
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF1E293B).copy(alpha = 0.9f),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFFFD54F).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = Localization.getString("brain_in_trouble_score", language),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = Color(0xFF94A3B8)
                                )
                            )
                            Text(
                                text = "$score",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFFFD54F),
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 4 大戰鬥數據卡片 (2x2 Grid)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 最高連擊
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1E293B).copy(alpha = 0.7f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = Localization.getString("brain_in_trouble_max_combo", language),
                                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8), fontSize = 10.sp)
                                    )
                                    Text(
                                        text = "🔥 $maxCombo",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFF9800)
                                        )
                                    )
                                }
                            }

                            // 存活時間
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1E293B).copy(alpha = 0.7f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = Localization.getString("time", language),
                                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8), fontSize = 10.sp)
                                    )
                                    Text(
                                        text = formatTimeMillis(timeMs),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                }
                            }
                        }

                        // 左右腦多工神經平衡指數
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1E293B).copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = Localization.getString("brain_in_trouble_balance_index", language),
                                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8), fontSize = 11.sp)
                                    )
                                    Text(
                                        text = "左幾何 $leftHits / 右色彩 $rightHits",
                                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B), fontSize = 9.sp)
                                    )
                                }
                                Text(
                                    text = "🧠 $balanceIndex%",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        color = if (balanceIndex >= 85) Color(0xFF4ADE80) else Color(0xFF38BDF8)
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // 3D 立體操作按鈕組
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                SoundManager.playClick()
                                onRestart()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0284C7)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = Localization.getString("play_again", language),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Button(
                            onClick = {
                                SoundManager.playClick()
                                onLeaderboard()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6B21A8)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Icon(Icons.Default.Leaderboard, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = Localization.getString("leaderboard_button", language),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = {
                            SoundManager.playClick()
                            onBack()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF334155)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                    ) {
                        Text(
                            text = Localization.getString("back_to_menu", language),
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }
            }
        }
    }
}
