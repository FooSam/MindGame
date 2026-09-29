package com.example.ui.screens

import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundManager
import com.example.data.model.AppLanguage
import com.example.data.model.GameDifficulty
import com.example.data.model.Localization
import com.example.game.pinball.FloatingText
import com.example.game.pinball.NightMarketBallState
import com.example.game.pinball.NightMarketPhysicsEngine
import com.example.game.pinball.Particle
import com.example.ui.components.AppBackground
import com.example.ui.theme.AppThemeStyle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class NightMarketPrize(
    val rankName: String,
    val sausages: Int,
    val giftName: String,
    val bonusDesc: String?
)

@Composable
fun NightMarketPinballScreen(
    difficulty: GameDifficulty,
    language: AppLanguage,
    appTheme: AppThemeStyle,
    onBackClick: () -> Unit,
    onLeaderboardClick: () -> Unit,
    onGameComplete: (score: Int, timeMs: Long) -> Unit
) {
    val initialBallsCount = when (difficulty) {
        GameDifficulty.BEGINNER -> 6
        GameDifficulty.INTERMEDIATE -> 8
        GameDifficulty.ADVANCED -> 10
        GameDifficulty.HARD, GameDifficulty.HELL, GameDifficulty.EPIC -> 13
    }

    // 遊戲狀態
    var score by remember { mutableIntStateOf(0) }
    var sausagesWon by remember { mutableIntStateOf(0) }
    var ballsRemaining by remember(difficulty) { mutableIntStateOf(initialBallsCount) }
    var isGameOver by remember { mutableStateOf(false) }
    var gameStartTime by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var renderTick by remember { mutableLongStateOf(0L) }

    val particles = remember { mutableStateListOf<Particle>() }
    val floatingTexts = remember { mutableStateListOf<FloatingText>() }
    var nextBallTimer by remember { mutableFloatStateOf(-1f) }

    // 物理演算引擎 (完全接管生命週期與連續運動)
    val engine = remember(difficulty) {
        NightMarketPhysicsEngine(
            difficulty = difficulty,
            onPinHit = { pin, impulse ->
                SoundManager.playPinDrop()
                score += 5
                if (Math.random() < 0.35) {
                    particles.add(
                        Particle(
                            x = pin.x,
                            y = pin.y,
                            vx = (Math.random().toFloat() - 0.5f) * 60f,
                            vy = (Math.random().toFloat() - 0.5f) * 60f,
                            color = Color(0xFFFFD54F)
                        )
                    )
                }
            },
            onSlotLanded = { slot ->
                // 台灣夜市真實玩法：鋼珠落槽永久留存堆疊，即時累積槽位分數
                score += slot.points
                ballsRemaining--
                SoundManager.playCoinOrSausage()
                floatingTexts.add(
                    FloatingText("+${slot.points}分", (slot.minX + slot.maxX) / 2f, 560f - 110f, Color(0xFFFFCA28))
                )

                // 啟動下一球裝填倒數；若已打完全部鋼珠，等待看清全盤後結算
                nextBallTimer = if (ballsRemaining > 0) 0.55f else 0.9f
            },
            onPaddleHit = { center ->
                score += 20
                SoundManager.playPinDrop()
                floatingTexts.add(FloatingText("SPIN! +20", center.x, center.y - 15f, Color(0xFF4FC3F7)))
            },
            onGateHit = {
                score += 30
                SoundManager.playPinballBounce()
            }
        )
    }

    // 台灣傳統夜市木造彈珠台鐵律：幾個槽就等於總共有幾顆彈珠
    val totalBallsCount = engine.rewardSlots.size

    // 換球邏輯
    fun prepareNextBall() {
        if (ballsRemaining > 0) {
            engine.resetBallToAlley()
        } else {
            isGameOver = true
            val totalTimeMs = SystemClock.elapsedRealtime() - gameStartTime
            onGameComplete(score, totalTimeMs)
        }
    }

    // 重新開始全盤
    fun restartGame() {
        score = 0
        sausagesWon = 0
        ballsRemaining = engine.rewardSlots.size
        isGameOver = false
        gameStartTime = SystemClock.elapsedRealtime()
        elapsedSeconds = 0
        particles.clear()
        floatingTexts.clear()
        nextBallTimer = -1f
        engine.clearSettledBalls()
        engine.resetBallToAlley()
    }

    // 計時器
    LaunchedEffect(isGameOver) {
        while (!isGameOver) {
            kotlinx.coroutines.delay(1000)
            elapsedSeconds = ((SystemClock.elapsedRealtime() - gameStartTime) / 1000).toInt()
        }
    }

    // 物理與高幀率連續繪製迴圈 (60fps ~ 120fps)
    LaunchedEffect(isGameOver) {
        var lastFrameTimeNanos = 0L

        while (!isGameOver) {
            withFrameNanos { nowNanos ->
                if (lastFrameTimeNanos == 0L) {
                    lastFrameTimeNanos = nowNanos
                    return@withFrameNanos
                }

                val dt = ((nowNanos - lastFrameTimeNanos) / 1_000_000_000f).coerceIn(0.001f, 0.033f)
                lastFrameTimeNanos = nowNanos

                // 物理演算步進
                engine.update(dt)

                // 換球倒數計時
                if (nextBallTimer > 0f) {
                    nextBallTimer -= dt
                    if (nextBallTimer <= 0f) {
                        nextBallTimer = -1f
                        prepareNextBall()
                    }
                }

                // 粒子與飄字更新
                val pIter = particles.iterator()
                while (pIter.hasNext()) {
                    val p = pIter.next()
                    p.x += p.vx * dt
                    p.y += p.vy * dt
                    p.alpha -= dt * 2.2f
                    if (p.alpha <= 0f) pIter.remove()
                }

                val tIter = floatingTexts.iterator()
                while (tIter.hasNext()) {
                    val t = tIter.next()
                    t.y -= dt * 40f
                    t.alpha -= dt * 1.1f
                    if (t.alpha <= 0f) tIter.remove()
                }

                // 動畫消退
                engine.pins.forEach { if (it.hitAnim > 0f) it.hitAnim = (it.hitAnim - dt * 5f).coerceAtLeast(0f) }
                engine.rewardSlots.forEach { if (it.hitAnim > 0f) it.hitAnim = (it.hitAnim - dt * 3f).coerceAtLeast(0f) }

                // 驅動 Compose Canvas 每幀連續刷新
                renderTick = nowNanos
            }
        }
    }

    // 拉桿蓄力拖曳狀態
    var plungerDragDistance by remember { mutableFloatStateOf(0f) }
    val maxDragDistance = 180f

    fun firePlunger(powerFraction: Float) {
        if (!isGameOver && engine.ball.state == NightMarketBallState.WAITING_LAUNCH) {
            if (engine.launch(powerFraction)) {
                SoundManager.playSpringLaunch()
            }
        }
    }

    AppBackground(themeStyle = appTheme) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            // 頂部導航
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    SoundManager.playClick()
                    onBackClick()
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "🍢 " + Localization.getString("game_night_market_pinball", language),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFFF7043)
                    )
                    Text(
                        text = "${difficulty.name} • ${elapsedSeconds}s",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = {
                    SoundManager.playClick()
                    onLeaderboardClick()
                }) {
                    Icon(
                        imageVector = Icons.Default.Leaderboard,
                        contentDescription = "Leaderboard",
                        tint = Color(0xFFFF7043)
                    )
                }
            }

            // 夜市香腸與得分看板
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFE0B2)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🏮 總得分: ",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFD84315)
                        )
                        Text(
                            text = "$score",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = Color(0xFFBF360C)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "已落槽: ",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${engine.settledBalls.size} / $totalBallsCount 珠",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "待發: $ballsRemaining 珠",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 復古木紋夜市台面畫布
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF4E342E))
            ) {
                val canvasWidth = constraints.maxWidth.toFloat()
                val canvasHeight = constraints.maxHeight.toFloat()
                val scaleX = canvasWidth / engine.virtualWidth
                val scaleY = canvasHeight / engine.virtualHeight

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(engine.ball.state, isGameOver) {
                            if (engine.ball.state == NightMarketBallState.WAITING_LAUNCH && !isGameOver) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val startY = down.position.y
                                    var currentDrag = 0f
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val pointer = event.changes.firstOrNull { it.id == down.id } ?: break
                                        if (!pointer.pressed) {
                                            // 鬆手彈射
                                            val powerFraction = if (currentDrag > 12f) {
                                                (currentDrag / maxDragDistance).coerceIn(0.25f, 1.0f)
                                            } else {
                                                0.85f // 點按彈射
                                            }
                                            firePlunger(powerFraction)
                                            plungerDragDistance = 0f
                                            break
                                        }
                                        val deltaY = (pointer.position.y - startY).coerceAtLeast(0f)
                                        currentDrag = deltaY.coerceIn(0f, maxDragDistance)
                                        plungerDragDistance = currentDrag
                                        pointer.consume()
                                    }
                                }
                            }
                        }
                ) {
                    // 依賴 renderTick 保證高幀率即時連續繪製
                    val _currentTick = renderTick

                    // 1. 復古木紋背景與邊框
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF5D4037), Color(0xFF3E2723))
                        )
                    )

                    // 2. 右側發射走道木質隔板與獨立通道
                    val divX = engine.mainBoardWidth * scaleX
                    val alleyRightX = (engine.virtualWidth - 6f) * scaleX
                    drawRect(
                        color = Color(0xFF2D1E18).copy(alpha = 0.65f),
                        topLeft = Offset(divX, 55f * scaleY),
                        size = Size(alleyRightX - divX, canvasHeight - 55f * scaleY)
                    )
                    drawLine(
                        color = Color(0xFF8D6E63),
                        start = Offset(divX, 55f * scaleY),
                        end = Offset(divX, canvasHeight),
                        strokeWidth = 4.dp.toPx()
                    )
                    drawLine(
                        color = Color(0xFFBCAAA4),
                        start = Offset(divX - 1.dp.toPx(), 55f * scaleY),
                        end = Offset(divX - 1.dp.toPx(), canvasHeight),
                        strokeWidth = 1.dp.toPx()
                    )

                    // 2.1 頂部弧形木外導軌 (平順延展至天花板頂壁，徹底消除向下倒勾)
                    val woodArchPath = Path().apply {
                        moveTo(alleyRightX, 55f * scaleY)
                        cubicTo(
                            alleyRightX, 16f * scaleY,
                            divX + 5f * scaleX, 16f * scaleY,
                            divX - 25f * scaleX, 16f * scaleY
                        )
                        lineTo(12f * scaleX, 16f * scaleY)
                    }
                    drawPath(
                        path = woodArchPath,
                        color = Color(0xFFFFB74D),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // 2.2 走道下方實體長彈簧與拉桿
                    val plungerCenterX = (engine.virtualWidth - engine.alleyWidth / 2f) * scaleX
                    val pullRatio = (plungerDragDistance / maxDragDistance).coerceIn(0f, 1f)
                    val plungerTopY = (engine.virtualHeight - 50f + pullRatio * 35f) * scaleY
                    val plungerBottomY = canvasHeight - 4.dp.toPx()

                    val coilCount = 8
                    val coilStep = (plungerBottomY - plungerTopY) / coilCount
                    for (i in 0 until coilCount) {
                        val cy = plungerTopY + i * coilStep
                        val cOffset = if (i % 2 == 0) -5.dp.toPx() else 5.dp.toPx()
                        drawLine(
                            color = Color(0xFFCFD8DC),
                            start = Offset(plungerCenterX - cOffset, cy),
                            end = Offset(plungerCenterX + cOffset, cy + coilStep),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                    drawRoundRect(
                        color = Color(0xFFEF6C00),
                        topLeft = Offset(plungerCenterX - 8.dp.toPx(), plungerTopY - 5.dp.toPx()),
                        size = Size(16.dp.toPx(), 6.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )

                    // 2.3 發射走道蓄力百分比反饋
                    if (engine.ball.state == NightMarketBallState.WAITING_LAUNCH && !isGameOver) {
                        drawContext.canvas.nativeCanvas.apply {
                            val paint = android.graphics.Paint().apply {
                                color = if (pullRatio > 0.05f) android.graphics.Color.argb(255, 255, 112, 67) else android.graphics.Color.argb(200, 255, 183, 77)
                                textSize = 11.sp.toPx()
                                isFakeBoldText = true
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                            val label = if (pullRatio > 0.05f) "${(pullRatio * 100).toInt()}%" else "▼下拉"
                            drawText(label, plungerCenterX, plungerTopY - 26.dp.toPx(), paint)
                        }
                    }

                    // 3. 底部加長深木槽 (Deep Wooden Reward Slots: 105f 長度，可堆疊 4~5 顆鋼珠)
                    val slotDepth = 105f
                    val sy = (engine.virtualHeight - slotDepth) * scaleY
                    val sh = slotDepth * scaleY

                    for (slot in engine.rewardSlots) {
                        val sx = slot.minX * scaleX
                        val sw = (slot.maxX - slot.minX) * scaleX

                        // 深木槽底板漸層 (立體深邃木格質感)
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = if (slot.hitAnim > 0f) {
                                    listOf(Color.White, Color(0xFFFFD54F))
                                } else {
                                    listOf(Color(0xFF3E2723), Color(0xFF261815), Color(0xFF1B0F0B))
                                }
                            ),
                            topLeft = Offset(sx + 1.5f, sy),
                            size = Size(sw - 3f, sh),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )

                        // 槽底吸震軟墊
                        drawRoundRect(
                            color = Color(0xFF5D4037).copy(alpha = 0.5f),
                            topLeft = Offset(sx + 3f, (engine.virtualHeight - 14f) * scaleY),
                            size = Size(sw - 6f, 12f * scaleY),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx())
                        )

                        // 槽位黃銅拋光立體隔板 (Polished Brass Divider)
                        drawLine(
                            color = Color(0xFFFFD54F),
                            start = Offset(sx + 1f, sy - 8f * scaleY),
                            end = Offset(sx + 1f, engine.virtualHeight * scaleY),
                            strokeWidth = 3.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = Color(0xFFFFF9C4),
                            start = Offset(sx + 0.5f, sy - 7f * scaleY),
                            end = Offset(sx + 0.5f, engine.virtualHeight * scaleY),
                            strokeWidth = 1.dp.toPx()
                        )
                        // 隔板頂部黃銅圓帽
                        drawCircle(
                            color = Color(0xFFFFD54F),
                            radius = 3.dp.toPx(),
                            center = Offset(sx + 1f, sy - 8f * scaleY)
                        )

                        // 槽頂標籤 (自適應槽寬動態排版：大字分數 + 小字說明)
                        val maxPoints = engine.rewardSlots.maxOfOrNull { it.points } ?: 50
                        val isSpecial = slot.points == maxPoints
                        val topLabel = "${slot.points}"
                        val bottomLabel = if (isSpecial) "特獎" else "分"

                        drawContext.canvas.nativeCanvas.apply {
                            val topTextSize = (sw * 0.36f).coerceIn(9f, 15f) * scaleX
                            val topPaint = android.graphics.Paint().apply {
                                color = if (isSpecial) android.graphics.Color.YELLOW else android.graphics.Color.WHITE
                                textSize = topTextSize
                                isFakeBoldText = true
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                            drawText(topLabel, sx + sw / 2f, sy + 22f * scaleY, topPaint)

                            val bottomTextSize = (sw * 0.25f).coerceIn(6.5f, 10.5f) * scaleX
                            val bottomPaint = android.graphics.Paint().apply {
                                color = if (isSpecial) android.graphics.Color.argb(255, 255, 179, 0) else android.graphics.Color.argb(220, 255, 255, 255)
                                textSize = bottomTextSize
                                isFakeBoldText = true
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                            drawText(bottomLabel, sx + sw / 2f, sy + 38f * scaleY, bottomPaint)
                        }
                    }

                    // 4. 黃銅釘陣 (Brass Pins)
                    for (pin in engine.pins) {
                        val px = pin.x * scaleX
                        val py = pin.y * scaleY
                        val pr = pin.radius * scaleX * (1f + pin.hitAnim * 0.25f)

                        drawCircle(
                            color = Color(0xFFFFD54F),
                            radius = pr,
                            center = Offset(px, py)
                        )
                        drawCircle(
                            color = Color(0xFFFFF9C4),
                            radius = pr * 0.5f,
                            center = Offset(px - pr * 0.25f, py - pr * 0.25f)
                        )
                    }

                    // 5. 難度特殊機關
                    engine.spinningPaddle?.let { pad ->
                        val cx = pad.center.x * scaleX
                        val cy = pad.center.y * scaleY
                        val cr = pad.radius * scaleX
                        drawCircle(
                            color = Color(0xFF29B6F6),
                            radius = cr * 0.4f,
                            center = Offset(cx, cy)
                        )
                        for (i in 0 until 4) {
                            val ang = (pad.angleDeg + i * 90f) * (PI / 180f)
                            drawLine(
                                color = Color(0xFFE1F5FE),
                                start = Offset(cx, cy),
                                end = Offset((cx + cos(ang) * cr).toFloat(), (cy + sin(ang) * cr).toFloat()),
                                strokeWidth = 3.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    engine.swingingGate?.let { gate ->
                        val gx = gate.pivot.x * scaleX
                        val gy = gate.pivot.y * scaleY
                        val rad = gate.currentAngleDeg * (PI / 180f)
                        val tx = (gx + cos(rad) * gate.length * scaleX).toFloat()
                        val ty = (gy + sin(rad) * gate.length * scaleY).toFloat()

                        drawLine(
                            color = Color(0xFFFF7043),
                            start = Offset(gx, gy),
                            end = Offset(tx, ty),
                            strokeWidth = 6.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 4.dp.toPx(),
                            center = Offset(gx, gy)
                        )
                    }

                    // 6. ★★★ 留存鋼珠與當前運動鋼珠繪製 (落槽永久保留堆疊，絕不消失) ★★★
                    // 6.1 繪製所有已落槽堆疊鋼珠 (Settled Balls in Slots)
                    for (sBall in engine.settledBalls) {
                        val sbx = sBall.x * scaleX
                        val sby = sBall.y * scaleY
                        val sbr = engine.ball.radius * scaleX

                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.White, Color(0xFFCFD8DC), Color(0xFF455A64)),
                                center = Offset(sbx - sbr * 0.35f, sby - sbr * 0.35f),
                                radius = sbr
                            ),
                            radius = sbr,
                            center = Offset(sbx, sby)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.5f),
                            radius = sbr * 0.25f,
                            center = Offset(sbx - sbr * 0.35f, sby - sbr * 0.35f)
                        )
                    }

                    // 6.2 繪製當前發射/運動中之鋼珠 (Active Flying Ball)
                    val ball = engine.ball
                    if (ball.state != NightMarketBallState.SETTLED) {
                        val bx = ball.x * scaleX
                        val pullOffset = if (ball.state == NightMarketBallState.WAITING_LAUNCH) (plungerDragDistance / maxDragDistance) * 35f else 0f
                        val by = (ball.y + pullOffset) * scaleY
                        val br = ball.radius * scaleX

                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.White, Color(0xFFCFD8DC), Color(0xFF546E7A)),
                                center = Offset(bx - br * 0.3f, by - br * 0.3f),
                                radius = br
                            ),
                            radius = br,
                            center = Offset(bx, by)
                        )
                    }

                    // 7. 粒子與浮動文字
                    for (p in particles) {
                        drawCircle(
                            color = p.color.copy(alpha = p.alpha.coerceIn(0f, 1f)),
                            radius = p.size * scaleX * p.alpha,
                            center = Offset(p.x * scaleX, p.y * scaleY)
                        )
                    }

                    for (txt in floatingTexts) {
                        drawContext.canvas.nativeCanvas.apply {
                            val paint = android.graphics.Paint().apply {
                                color = android.graphics.Color.argb(
                                    (txt.alpha.coerceIn(0f, 1f) * 255).toInt(),
                                    (txt.color.red * 255).toInt(),
                                    (txt.color.green * 255).toInt(),
                                    (txt.color.blue * 255).toInt()
                                )
                                textSize = 15.sp.toPx() * scaleX
                                isFakeBoldText = true
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                            drawText(txt.text, txt.x * scaleX, txt.y * scaleY, paint)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 底部極簡蓄力指示與玩法說明 (釋出垂直空間，彈珠台視野大台化)
            val powerFraction = (plungerDragDistance / maxDragDistance).coerceIn(0f, 1f)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                if (powerFraction > 0.05f) {
                    val aimHint = when {
                        powerFraction > 0.82f -> "🔥 滿力衝刺左路 (大獎/特獎區)"
                        powerFraction > 0.52f -> "⚡ 中蓄力直擊中路"
                        powerFraction > 0.22f -> "🎯 輕拉切入右路"
                        else -> "⚠️ 力道過弱將滾回重發"
                    }
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "🍢 蓄力: ${(powerFraction * 100).toInt()}% • $aimHint",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFFF5722)
                        )
                        Text(
                            text = "鬆手彈射!",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                            color = Color(0xFFD84315)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "💡 螢幕中間或右側向下拉動蓄力，鬆手彈射",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // ★★★ 台灣夜市真實彈珠台：打完全部鋼珠後統一兌獎結算彈窗 ★★★
        if (isGameOver) {
            val totalSlots = engine.rewardSlots.size
            val slotCounts = (0 until totalSlots).map { slotId -> engine.settledBalls.count { it.slotIndex == slotId } }
            val maxInSlot = slotCounts.maxOrNull() ?: 0
            val isGrandSlam = slotCounts.all { it == 1 }

            val bonusSausage = if (maxInSlot >= 3) 1 else 0
            val bonusDesc = when {
                isGrandSlam -> "👑 傳奇大滿貫！全盤 $totalSlots 槽均勻各落 1 顆珠子！"
                maxInSlot >= 3 -> "🎯 一統天下彩蛋！單槽聚集 $maxInSlot 顆 (+1 香腸)"
                else -> null
            }

            val avgBallPoints = if (totalSlots > 0) score.toFloat() / totalSlots else 0f
            val prize = when {
                isGrandSlam -> NightMarketPrize("🏆 傳奇大滿貫特等獎！", 5, "烤香腸 5 支 + 豪華大娃娃 🧸", bonusDesc)
                avgBallPoints >= 38f -> NightMarketPrize("頭獎 (香腸霸主)", 3 + bonusSausage, "烤香腸 ${3 + bonusSausage} 支 + 懷舊彈珠汽水 🍾", bonusDesc)
                avgBallPoints >= 26f -> NightMarketPrize("二獎 (夜市神射)", 2 + bonusSausage, "烤香腸 ${2 + bonusSausage} 支 🌭", bonusDesc)
                avgBallPoints >= 18f -> NightMarketPrize("三獎 (夜市高手)", 1 + bonusSausage, "烤香腸 ${1 + bonusSausage} 支 🌭", bonusDesc)
                else -> NightMarketPrize("安慰獎 (好滋味)", bonusSausage, if (bonusSausage > 0) "烤香腸 1 支 🌭" else "懷舊沙士糖 1 顆 🍬", bonusDesc)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.90f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🏮 夜市彈珠香腸攤 兌獎處 🏮",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = Color(0xFFD84315),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0xFFFFE0B2).copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = prize.rankName,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                    color = Color(0xFFBF360C)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "獎品：${prize.giftName}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFE65100),
                                    textAlign = TextAlign.Center
                                )

                                prize.bonusDesc?.let { bDesc ->
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFFFCC80)
                                    ) {
                                        Text(
                                            text = bDesc,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFFB71C1C),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "🎯 本局總分: $score 分 (打完 10 顆鋼珠)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 各槽落球狀況列表
                        Text(
                            text = "📊 各槽落球成果 (常態分佈):",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val slotLabels = listOf("10分", "20分", "30分", "50分", "30分", "20分")
                            slotCounts.forEachIndexed { idx, count ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 2.dp)
                                ) {
                                    Text(
                                        text = "${count}珠",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                        color = if (count > 0) Color(0xFFD84315) else Color.Gray
                                    )
                                    Text(
                                        text = slotLabels[idx],
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    SoundManager.playClick()
                                    restartGame()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7043))
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("再來一盤", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    SoundManager.playClick()
                                    onBackClick()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Text("收下獎勵", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
