package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Refresh
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.style.TextAlign
import com.example.ui.components.formatTimeMillis
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundManager
import com.example.data.model.AppLanguage
import com.example.ui.theme.AppThemeStyle
import com.example.data.model.GameDifficulty
import com.example.data.model.Localization
import com.example.game.brainintrouble.BrainColor
import com.example.game.brainintrouble.BrainInTroubleConfig
import com.example.game.brainintrouble.BrainInTroubleGenerator
import com.example.game.brainintrouble.BrainShape
import com.example.game.brainintrouble.DropItem
import com.example.game.brainintrouble.Particle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 左右為難 (Brain in Trouble) 主畫面
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
    fun spawnParticles(x: Float, y: Float, color: Color, count: Int = 16) {
        for (i in 0 until count) {
            val angle = Math.random().toFloat() * 2f * PI.toFloat()
            val speed = (80f + Math.random().toFloat() * 220f)
            val vx = cos(angle) * speed
            val vy = sin(angle) * speed
            val size = (6f + Math.random().toFloat() * 8f)
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
            spawnParticles(targetX, targetY, Color(0xFF38BDF8), 16)
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
            spawnParticles(targetX, targetY, color.color, 16)
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
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LaserGlow"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0F172A),
                        Color(0xFF090D16),
                        Color(0xFF020617)
                    )
                )
            )
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        // --- 頂部導航與狀態列 ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    SoundManager.playClick()
                    if (!isGameOver && lives > 0) {
                        onGameInterrupted()
                    }
                    onBackClick()
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = Localization.getString("brain_in_trouble_title", language),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    )
                    Text(
                        text = Localization.getString(difficulty.key.lowercase(), language),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            // 得分與連擊徽章
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (combo >= 3) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFF5722).copy(alpha = 0.25f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5722))
                    ) {
                        Text(
                            text = "🔥 ${combo}X",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFF9800)
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Text(
                        text = "$score 分",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFACC15)
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(onClick = {
                    SoundManager.playClick()
                    onLeaderboardClick()
                }) {
                    Icon(
                        imageVector = Icons.Default.Leaderboard,
                        contentDescription = "Leaderboard",
                        tint = Color(0xFFF59E0B)
                    )
                }
            }
        }

        // 生命值指示條 (3 顆立體紅心)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                for (i in 1..3) {
                    val isAlive = i <= lives
                    Icon(
                        imageVector = if (isAlive) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Heart",
                        tint = if (isAlive) Color(0xFFEF4444) else Color(0xFF64748B),
                        modifier = Modifier
                            .size(24.dp)
                            .padding(horizontal = 2.dp)
                            .scale(if (isAlive) heartScale.value else 0.9f)
                    )
                }
            }

            // 難度特殊規則提示
            if (config.isStroopConflict) {
                Text(
                    text = Localization.getString("brain_in_trouble_stroop_rule", language),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFFFF6B6B),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                )
            } else {
                Text(
                    text = Localization.getString("brain_in_trouble_hint", language),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // --- 中間雙軌戰場 (Track Battlefield) ---
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(18.dp))
        ) {
            val totalWidth = constraints.maxWidth.toFloat()
            val trackHeight = constraints.maxHeight.toFloat()
            val halfWidth = totalWidth / 2f
            val leftTrackCenterX = halfWidth * 0.5f
            val rightTrackCenterX = halfWidth * 1.5f

            // 是否有即將越線的緊急物體
            val isLeftUrgent = dropItems.any { it.isLeftSide && it.progress > 0.75f && !it.isEliminated }
            val isRightUrgent = dropItems.any { !it.isLeftSide && it.progress > 0.75f && !it.isEliminated }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val _tick = gameFrameTick // 訂閱高精度物理幀，保證 60/120Hz 零掉幀平滑重繪

                // 1. 繪製左右雙軌背景漸層
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF0F172A).copy(alpha = 0.85f), Color(0xFF1E293B).copy(alpha = 0.45f)),
                        startX = 0f,
                        endX = halfWidth
                    ),
                    size = Size(halfWidth, trackHeight)
                )
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF1E1B4B).copy(alpha = 0.45f), Color(0xFF2E1065).copy(alpha = 0.85f)),
                        startX = halfWidth,
                        endX = totalWidth
                    ),
                    topLeft = Offset(halfWidth, 0f),
                    size = Size(halfWidth, trackHeight)
                )

                // 2. 中央高科技能量光柱
                drawLine(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF38BDF8).copy(alpha = 0.2f), Color(0xFFA855F7), Color(0xFFEC4899).copy(alpha = 0.2f))
                    ),
                    start = Offset(halfWidth, 0f),
                    end = Offset(halfWidth, trackHeight),
                    strokeWidth = 2.5f
                )

                // 3. 底部雷射判定警戒線 (Impact Line)
                val lineY = trackHeight - 4f
                val leftLineColor = if (isLeftUrgent) Color(0xFFFF3366).copy(alpha = laserGlow) else Color(0xFF38BDF8).copy(alpha = 0.6f)
                val rightLineColor = if (isRightUrgent) Color(0xFFFF3366).copy(alpha = laserGlow) else Color(0xFFA855F7).copy(alpha = 0.6f)

                // 左底線
                drawLine(
                    color = leftLineColor,
                    start = Offset(12f, lineY),
                    end = Offset(halfWidth - 8f, lineY),
                    strokeWidth = if (isLeftUrgent) 4f else 2.5f
                )
                // 右底線
                drawLine(
                    color = rightLineColor,
                    start = Offset(halfWidth + 8f, lineY),
                    end = Offset(totalWidth - 12f, lineY),
                    strokeWidth = if (isRightUrgent) 4f else 2.5f
                )

                // 4. 繪製掉落物 (3大獨立跑道分流，多物同屏流暢共存)
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
                                drawBrainShape(
                                    shape = shape,
                                    centerX = posX,
                                    centerY = posY,
                                    radius = 42f
                                )
                            }
                        } else {
                            val posX = halfWidth + (laneIdx + 0.5f) * laneWidth
                            item.visualColor?.let { visualColor ->
                                drawBrainColorPill(
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

                // 5. 繪製爆炸消散粒子
                particles.forEach { p ->
                    drawCircle(
                        color = p.color.copy(alpha = p.alpha),
                        radius = p.size,
                        center = Offset(p.x, p.y)
                    )
                }
            }

            // 雙軌文字浮水印
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Text(
                    text = "◀ 左腦形狀 (Left Brain)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF38BDF8).copy(alpha = 0.45f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                )
                Text(
                    text = "右腦色彩 (Right Brain) ▶",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFFA855F7).copy(alpha = 0.45f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- 底部雙手控制區 (Dual Controllers) ---
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val totalW = constraints.maxWidth.toFloat()
            val trackH = 300f // 基準估算高度
            val halfW = totalW / 2f

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
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

        Spacer(modifier = Modifier.height(8.dp))
    }

    // --- GameOver 結算彈窗 ---
    if (isGameOver) {
        GameOverDialog(
            score = score,
            maxCombo = maxCombo,
            timeMs = totalPlayedTimeMs,
            difficulty = difficulty,
            language = language,
            onRestart = { restartGame() },
            onLeaderboard = { onLeaderboardClick() },
            onBack = { onBackClick() }
        )
    }
}

/**
 * 繪製左側發光立體幾何形狀 (放大 2.25 倍)
 */
private fun DrawScope.drawBrainShape(
    shape: BrainShape,
    centerX: Float,
    centerY: Float,
    radius: Float = 54f
) {
    val cyanGlow = Color(0xFF38BDF8)
    val darkBlueBg = Color(0xFF0369A1)

    // 外發光圓形底襯
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(darkBlueBg.copy(alpha = 0.85f), Color(0xFF0F172A).copy(alpha = 0.95f)),
            center = Offset(centerX, centerY),
            radius = radius * 1.4f
        ),
        radius = radius * 1.35f,
        center = Offset(centerX, centerY)
    )

    // 外圈光暈線
    drawCircle(
        color = cyanGlow.copy(alpha = 0.6f),
        radius = radius * 1.35f,
        center = Offset(centerX, centerY),
        style = Stroke(width = 2.5f)
    )

    when (shape) {
        BrainShape.CIRCLE -> {
            drawCircle(color = cyanGlow, radius = radius * 0.75f, center = Offset(centerX, centerY))
            drawCircle(color = Color.White.copy(alpha = 0.75f), radius = radius * 0.32f, center = Offset(centerX - 8f, centerY - 8f))
        }
        BrainShape.SQUARE -> {
            val half = radius * 0.75f
            drawRoundRect(
                color = cyanGlow,
                topLeft = Offset(centerX - half, centerY - half),
                size = Size(half * 2f, half * 2f),
                cornerRadius = CornerRadius(14f, 14f)
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.65f),
                topLeft = Offset(centerX - half + 6f, centerY - half + 6f),
                size = Size(half * 0.6f, half * 0.6f),
                cornerRadius = CornerRadius(6f, 6f)
            )
        }
        BrainShape.TRIANGLE -> {
            val path = Path().apply {
                moveTo(centerX, centerY - radius * 0.9f)
                lineTo(centerX + radius * 0.85f, centerY + radius * 0.75f)
                lineTo(centerX - radius * 0.85f, centerY + radius * 0.75f)
                close()
            }
            drawPath(path = path, color = cyanGlow)
        }
        BrainShape.DIAMOND -> {
            val path = Path().apply {
                moveTo(centerX, centerY - radius * 0.95f)
                lineTo(centerX + radius * 0.85f, centerY)
                lineTo(centerX, centerY + radius * 0.95f)
                lineTo(centerX - radius * 0.85f, centerY)
                close()
            }
            drawPath(path = path, color = cyanGlow)
        }
        BrainShape.STAR -> {
            val path = Path()
            val points = 5
            val outerR = radius * 0.95f
            val innerR = radius * 0.44f
            for (i in 0 until points * 2) {
                val r = if (i % 2 == 0) outerR else innerR
                val angle = (i * PI / points - PI / 2).toFloat()
                val x = centerX + r * cos(angle)
                val y = centerY + r * sin(angle)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path = path, color = cyanGlow)
        }
        BrainShape.HEXAGON -> {
            val path = Path()
            val sides = 6
            for (i in 0 until sides) {
                val angle = (i * 2 * PI / sides).toFloat()
                val x = centerX + radius * 0.85f * cos(angle)
                val y = centerY + radius * 0.85f * sin(angle)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path = path, color = cyanGlow)
        }
    }
}

/**
 * 繪製右側發光立體色彩膠囊 (放大 2.2 倍，含文字與 Stroop 衝突)
 */
private fun DrawScope.drawBrainColorPill(
    color: BrainColor,
    text: String?,
    centerX: Float,
    centerY: Float,
    textMeasurer: TextMeasurer
) {
    val pillWidth = if (text != null) 124f else 96f
    val pillHeight = 72f
    val left = centerX - pillWidth / 2f
    val top = centerY - pillHeight / 2f

    // 外發光
    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(color.color.copy(alpha = 0.55f), Color.Transparent),
            center = Offset(centerX, centerY),
            radius = pillWidth * 0.75f
        ),
        topLeft = Offset(left - 10f, top - 10f),
        size = Size(pillWidth + 20f, pillHeight + 20f),
        cornerRadius = CornerRadius(22f, 22f)
    )

    // 實體膠囊底板
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                color.color.copy(alpha = 0.95f),
                color.color.copy(alpha = 0.75f)
            )
        ),
        topLeft = Offset(left, top),
        size = Size(pillWidth, pillHeight),
        cornerRadius = CornerRadius(18f, 18f)
    )

    // 高光倒角邊框
    drawRoundRect(
        color = Color.White.copy(alpha = 0.55f),
        topLeft = Offset(left, top),
        size = Size(pillWidth, pillHeight),
        cornerRadius = CornerRadius(18f, 18f),
        style = Stroke(width = 2.5f)
    )

    // 文字繪製 (Stroop 文字或同色字)
    if (!text.isNullOrEmpty()) {
        val textLayoutResult = textMeasurer.measure(
            text = text,
            style = TextStyle(
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )
        )
        val textX = centerX - textLayoutResult.size.width / 2f
        val textY = centerY - textLayoutResult.size.height / 2f
        drawText(
            textLayoutResult = textLayoutResult,
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
        // 4 個 (2x2), 5 個 (3+2), 6 個 (3x2)
        val firstRowCount = if (shapes.size == 4) 2 else 3
        listOf(shapes.take(firstRowCount), shapes.drop(firstRowCount))
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
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
 * 水晶立體形狀按鈕
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
            .scale(if (isPressed) 0.92f else 1.0f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(14.dp),
        color = if (isPressed) Color(0xFF0369A1) else Color(0xFF0F172A),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isPressed) 2.dp else 1.2.dp,
            color = if (isPressed) Color(0xFF38BDF8) else Color(0xFF1E293B)
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
                    }
                    BrainShape.SQUARE -> {
                        val h = radius * 0.75f
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(centerX - h, centerY - h),
                            size = Size(h * 2f, h * 2f),
                            cornerRadius = CornerRadius(4f, 4f)
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
        verticalArrangement = Arrangement.spacedBy(8.dp)
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
 * 水晶立體顏色按鈕
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
            .scale(if (isPressed) 0.92f else 1.0f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(14.dp),
        color = color.color.copy(alpha = if (isPressed) 0.95f else 0.75f),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isPressed) 2.5f.dp else 1.5.dp,
            color = Color.White.copy(alpha = if (isPressed) 0.9f else 0.45f)
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
 * GameOver 結算彈窗 (標準 Material 3 AlertDialog 風格)
 */
@Composable
private fun GameOverDialog(
    score: Int,
    maxCombo: Int,
    timeMs: Long,
    difficulty: GameDifficulty,
    language: AppLanguage,
    onRestart: () -> Unit,
    onLeaderboard: () -> Unit,
    onBack: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(
                text = Localization.getString("game_completed", language),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = Localization.getString("brain_in_trouble_game_over_desc", language),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 得分主看板
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = Localization.getString("brain_in_trouble_score", language),
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                        Text(
                            text = "$score 分",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
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
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = Localization.getString("brain_in_trouble_max_combo", language),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Text(
                                text = "🔥 $maxCombo",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF9800)
                                )
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = Localization.getString("time", language),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Text(
                                text = formatTimeMillis(timeMs),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = Localization.getString("score_saved", language),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                SoundManager.playClick()
                onRestart()
            }) {
                Text(text = Localization.getString("play_again", language))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    SoundManager.playClick()
                    onLeaderboard()
                }) {
                    Text(text = Localization.getString("leaderboard_button", language))
                }
                TextButton(onClick = {
                    SoundManager.playClick()
                    onBack()
                }) {
                    Text(text = Localization.getString("back_to_menu", language))
                }
            }
        }
    )
}
