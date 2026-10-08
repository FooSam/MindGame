package com.example.ui.screens

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import com.example.game.pinball.Flipper
import com.example.game.pinball.FlipperPhysicsEngine
import com.example.game.pinball.FloatingText
import com.example.game.pinball.Particle
import com.example.game.pinball.Slingshot
import com.example.ui.components.AppBackground
import com.example.ui.theme.AppThemeStyle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FlipperPinballScreen(
    difficulty: GameDifficulty,
    language: AppLanguage,
    appTheme: AppThemeStyle,
    onBackClick: () -> Unit,
    onLeaderboardClick: () -> Unit,
    onGameComplete: (score: Int, timeMs: Long) -> Unit,
    onGameInterrupted: () -> Unit = {}
) {
    // 依難度設定球數
    val totalBallsCount = when (difficulty) {
        GameDifficulty.BEGINNER -> 5
        GameDifficulty.INTERMEDIATE -> 4
        else -> 3
    }

    var score by remember { mutableIntStateOf(0) }
    var ballsLeft by remember { mutableIntStateOf(totalBallsCount) }
    var isGameOver by remember { mutableStateOf(false) }
    var gameStartTime by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var renderTick by remember { mutableLongStateOf(0L) }

    var isLeftPressedState by remember { mutableStateOf(false) }
    var isRightPressedState by remember { mutableStateOf(false) }
    var plungerDragDistance by remember { mutableFloatStateOf(0f) }
    val maxDragDistance = 140f

    val particles = remember { mutableStateListOf<Particle>() }
    val floatingTexts = remember { mutableStateListOf<FloatingText>() }

    // 核心物理演算引擎 (採用常規彈珠台黃金佈局與發球動力學保護)
    val engine = remember(difficulty) {
        FlipperPhysicsEngine(
            difficulty = difficulty,
            onBumperHit = { bumper, pts ->
                SoundManager.playPinballBounce()
                score += pts
                repeat(6) {
                    val angle = Math.random() * 2.0 * PI
                    val spd = 60f + Math.random().toFloat() * 120f
                    particles.add(
                        Particle(
                            x = bumper.center.x,
                            y = bumper.center.y,
                            vx = (cos(angle) * spd).toFloat(),
                            vy = (sin(angle) * spd).toFloat(),
                            color = bumper.color
                        )
                    )
                }
                floatingTexts.add(FloatingText("+$pts", bumper.center.x, bumper.center.y - 20f, bumper.color))
            },
            onRolloverLit = { target, pts ->
                SoundManager.playPinDrop()
                score += pts
                floatingTexts.add(FloatingText("LIT! +$pts", target.center.x, target.center.y - 15f, target.color))
            },
            onSlingshotHit = { sling ->
                SoundManager.playPinballBounce()
                score += 30
            },
            onFlipperHit = { isLeft ->
                SoundManager.playPinballBounce()
            },
            onWallBounce = {
                SoundManager.playPinballBounce()
            },
            onBallDrained = { eng ->
                // 進入場地後漏失掉落底部排水坑，扣一顆球
                ballsLeft--
                if (ballsLeft > 0) {
                    eng.resetBallToAlley()
                } else {
                    isGameOver = true
                    val totalTimeMs = SystemClock.elapsedRealtime() - gameStartTime
                    onGameComplete(score, totalTimeMs)
                }
            },
            onBallRealignedInAlley = {
                // 發球保護：未進場掉回發射道，安全重新就位，不扣球數
            }
        )
    }

    // 發射球函式
    fun launchBall(powerFraction: Float = 0.95f) {
        if (!isGameOver && engine.pinball.inAlley) {
            if (engine.launchBall(powerFraction)) {
                SoundManager.playSpringLaunch()
            }
        }
    }

    // 重新開始全局
    fun restartGame() {
        score = 0
        ballsLeft = totalBallsCount
        isGameOver = false
        gameStartTime = SystemClock.elapsedRealtime()
        elapsedSeconds = 0
        particles.clear()
        floatingTexts.clear()
        engine.rollovers.forEach { it.isLit = false }
        engine.resetBallToAlley()
    }

    // 計時器
    LaunchedEffect(isGameOver) {
        while (!isGameOver) {
            kotlinx.coroutines.delay(1000)
            elapsedSeconds = ((SystemClock.elapsedRealtime() - gameStartTime) / 1000).toInt()
        }
    }

    // 主物理與繪製動畫迴圈 (60fps ~ 120fps 連續平滑刷新)
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

                // 物理演算
                engine.update(dt)

                // 粒子與飄字更新
                val pIter = particles.iterator()
                while (pIter.hasNext()) {
                    val p = pIter.next()
                    p.x += p.vx * dt
                    p.y += p.vy * dt
                    p.alpha -= dt * 1.8f
                    if (p.alpha <= 0f) pIter.remove()
                }

                val tIter = floatingTexts.iterator()
                while (tIter.hasNext()) {
                    val t = tIter.next()
                    t.y -= dt * 45f
                    t.alpha -= dt * 1.2f
                    if (t.alpha <= 0f) tIter.remove()
                }

                // 動畫恢復
                engine.bumpers.forEach { if (it.hitAnim > 0f) it.hitAnim = (it.hitAnim - dt * 4f).coerceAtLeast(0f) }
                engine.slingshots.forEach { if (it.hitAnim > 0f) it.hitAnim = (it.hitAnim - dt * 4f).coerceAtLeast(0f) }
                engine.centerPost?.let { if (it.hitAnim > 0f) it.hitAnim = (it.hitAnim - dt * 4f).coerceAtLeast(0f) }

                // 驅動 Compose Canvas 連續重繪
                renderTick = nowNanos
            }
        }
    }

    AppBackground(themeStyle = appTheme) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // 頂部導航與標題
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    SoundManager.playClick()
                    val hasStarted = !isGameOver && (score > 0 || ballsLeft < totalBallsCount || !engine.pinball.inAlley)
                    if (hasStarted) {
                        onGameInterrupted()
                    }
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
                        text = Localization.getString("game_pinball_flipper", language),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
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
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 得分看板
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SCORE: ",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$score",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.primary
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
                            text = "BALLS: ",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(totalBallsCount) { idx ->
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (idx < ballsLeft) MaterialTheme.colorScheme.primary
                                             else Color.Gray.copy(alpha = 0.3f)
                                        )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 彈珠台畫布主區域
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF141A29))
            ) {
                val canvasWidth = constraints.maxWidth.toFloat()
                val canvasHeight = constraints.maxHeight.toFloat()
                val scaleX = canvasWidth / engine.virtualWidth
                val scaleY = canvasHeight / engine.virtualHeight

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val virtX = down.position.x / scaleX

                                if (virtX > engine.mainPlayWidth && engine.pinball.inAlley) {
                                    // 玩家在右側發射走道向下拉動蓄力發射
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
                                                0.85f // 輕點亦能蓄力擊發
                                            }
                                            launchBall(powerFraction)
                                            plungerDragDistance = 0f
                                            break
                                        }
                                        val deltaY = (pointer.position.y - startY).coerceAtLeast(0f)
                                        currentDrag = deltaY.coerceIn(0f, maxDragDistance)
                                        plungerDragDistance = currentDrag
                                        pointer.consume()
                                    }
                                } else {
                                    if (virtX < engine.virtualWidth / 2f) {
                                        engine.leftFlipper.isPressed = true
                                        isLeftPressedState = true
                                        SoundManager.playFlipperSnap()
                                        waitForUpOrCancellation()
                                        engine.leftFlipper.isPressed = false
                                        isLeftPressedState = false
                                    } else {
                                        engine.rightFlipper.isPressed = true
                                        isRightPressedState = true
                                        SoundManager.playFlipperSnap()
                                        waitForUpOrCancellation()
                                        engine.rightFlipper.isPressed = false
                                        isRightPressedState = false
                                    }
                                }
                            }
                        }
                ) {
                    val _currentTick = renderTick

                    // 1. 彈珠台背景網格與光影
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                        )
                    )

                    // 2. 右側獨立發射通道底色與立體軌道
                    val alleyLeftX = engine.mainPlayWidth * scaleX
                    val alleyRightX = (engine.virtualWidth - 8f) * scaleX
                    drawRect(
                        color = Color(0xFF0B132B).copy(alpha = 0.7f),
                        topLeft = Offset(alleyLeftX, 65f * scaleY),
                        size = Size(alleyRightX - alleyLeftX, canvasHeight - 65f * scaleY)
                    )

                    // 2.1 發射走道實體彈簧拉桿與蓄力反饋
                    val plungerCenterX = (alleyLeftX + alleyRightX) / 2f
                    val pullRatio = (plungerDragDistance / maxDragDistance).coerceIn(0f, 1f)
                    val plungerTopY = (engine.virtualHeight - 65f + pullRatio * 35f) * scaleY
                    val plungerBottomY = canvasHeight - 4.dp.toPx()

                    val coilCount = 7
                    val coilStep = (plungerBottomY - plungerTopY) / coilCount
                    for (i in 0 until coilCount) {
                        val cy = plungerTopY + i * coilStep
                        val cOffset = if (i % 2 == 0) -4.dp.toPx() else 4.dp.toPx()
                        drawLine(
                            color = Color(0xFF94A3B8),
                            start = Offset(plungerCenterX - cOffset, cy),
                            end = Offset(plungerCenterX + cOffset, cy + coilStep),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                    drawRoundRect(
                        color = if (pullRatio > 0.05f) Color(0xFFFF7043) else Color(0xFF38BDF8),
                        topLeft = Offset(plungerCenterX - 7.dp.toPx(), plungerTopY - 4.dp.toPx()),
                        size = Size(14.dp.toPx(), 5.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )

                    // 2.2 發射提示與即時蓄力百分比
                    if (engine.pinball.inAlley && !isGameOver) {
                        drawContext.canvas.nativeCanvas.apply {
                            val paint = android.graphics.Paint().apply {
                                color = if (pullRatio > 0.05f) android.graphics.Color.argb(255, 255, 112, 67) else android.graphics.Color.argb(200, 56, 189, 248)
                                textSize = 11.sp.toPx()
                                isFakeBoldText = true
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                            val label = if (pullRatio > 0.05f) "${(pullRatio * 100).toInt()}%" else "▼下拉"
                            drawText(label, plungerCenterX, plungerTopY - 26.dp.toPx(), paint)
                        }
                    }

                    // 3. 頂部美式彈珠台圓弧導軌 (平順圓弧過渡至天花板水平切線，徹底消除向下倒勾)
                    val archPath = Path().apply {
                        moveTo(alleyRightX, 65f * scaleY)
                        cubicTo(
                            alleyRightX, 14f * scaleY,
                            alleyLeftX + 5f * scaleX, 14f * scaleY,
                            alleyLeftX - 25f * scaleX, 14f * scaleY
                        )
                    }
                    drawPath(
                        path = archPath,
                        color = Color(0xFF38BDF8),
                        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // 4. 外軌與發射通道實木/金屬立體隔板 (消除豆腐渣工程單薄感)
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF334155), Color(0xFF64748B), Color(0xFF1E293B)),
                            startX = alleyLeftX - 6.dp.toPx(),
                            endX = alleyLeftX + 2.dp.toPx()
                        ),
                        topLeft = Offset(alleyLeftX - 6.dp.toPx(), 65f * scaleY),
                        size = Size(8.dp.toPx(), canvasHeight - 65f * scaleY)
                    )
                    // 隔板高光反光線
                    drawLine(
                        color = Color(0xFFCBD5E1),
                        start = Offset(alleyLeftX - 5.dp.toPx(), 65f * scaleY),
                        end = Offset(alleyLeftX - 5.dp.toPx(), canvasHeight),
                        strokeWidth = 1.2.dp.toPx()
                    )

                    // 5. 主場地左外壁與頂壁厚實機台外框 (Cabinet Side Rails)
                    // 左側立體側邊框 (厚度 14px，深金屬烤漆 + 內外層高光倒角)
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF0F172A), Color(0xFF334155), Color(0xFF1E293B)),
                            startX = 0f,
                            endX = 14f * scaleX
                        ),
                        topLeft = Offset(0f, 20f * scaleY),
                        size = Size(14f * scaleX, canvasHeight)
                    )
                    // 左側內金屬導軌亮線
                    drawLine(
                        color = Color(0xFF94A3B8),
                        start = Offset(13f * scaleX, 28f * scaleY),
                        end = Offset(13f * scaleX, canvasHeight),
                        strokeWidth = 2.dp.toPx()
                    )
                    // 頂壁實體水平金屬框 (平順銜接頂弧，絕無倒勾)
                    drawLine(
                        color = Color(0xFF64748B),
                        start = Offset(13f * scaleX, 14f * scaleY),
                        end = Offset(alleyLeftX - 25f * scaleX, 14f * scaleY),
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color(0xFFE2E8F0),
                        start = Offset(14f * scaleX, 15f * scaleY),
                        end = Offset(alleyLeftX - 25f * scaleX, 15f * scaleY),
                        strokeWidth = 1.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // 6. ★★★ 美式彈珠台專業實體導流台座 (Apron Blocks & Wireform Guides) ★★★
                    // 【左側實體導流台座】(告別懸空鐵皮，構築完整實體三角機械導流座)
                    val leftApronPath = Path().apply {
                        moveTo(0f, engine.leftGuideP1.y * scaleY - 10f * scaleY)
                        lineTo(engine.leftGuideP1.x * scaleX, engine.leftGuideP1.y * scaleY)
                        lineTo(engine.leftGuideP2.x * scaleX, engine.leftGuideP2.y * scaleY)
                        lineTo(engine.leftGuideP2.x * scaleX, (engine.leftFlipper.pivot.y + 30f) * scaleY)
                        lineTo(0f, (engine.leftFlipper.pivot.y + 30f) * scaleY)
                        close()
                    }
                    drawPath(
                        path = leftApronPath,
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF1E1B4B)),
                            start = Offset(0f, engine.leftGuideP1.y * scaleY),
                            end = Offset(engine.leftGuideP2.x * scaleX, engine.leftGuideP2.y * scaleY)
                        )
                    )
                    // 左立體雙管鍍鉻金屬導軌 (Wireform Rail)
                    drawLine(
                        color = Color(0xFF1E293B),
                        start = Offset(engine.leftGuideP1.x * scaleX + 2f, engine.leftGuideP1.y * scaleY + 2f),
                        end = Offset(engine.leftGuideP2.x * scaleX + 2f, engine.leftGuideP2.y * scaleY + 2f),
                        strokeWidth = 5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color(0xFF94A3B8),
                        start = Offset(engine.leftGuideP1.x * scaleX, engine.leftGuideP1.y * scaleY),
                        end = Offset(engine.leftGuideP2.x * scaleX, engine.leftGuideP2.y * scaleY),
                        strokeWidth = 3.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color(0xFFF8FAFC),
                        start = Offset(engine.leftGuideP1.x * scaleX - 1f, engine.leftGuideP1.y * scaleY - 1f),
                        end = Offset(engine.leftGuideP2.x * scaleX - 1f, engine.leftGuideP2.y * scaleY - 1f),
                        strokeWidth = 1.2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    // 左側導流柱電鍍圓柱與橡膠環 (Chrome Post with Rubber Ring)
                    listOf(engine.leftGuideP1, engine.leftGuideP2).forEach { pt ->
                        val px = pt.x * scaleX
                        val py = pt.y * scaleY
                        // 黑色橡膠底墊
                        drawCircle(color = Color(0xFF0F172A), radius = 6.dp.toPx(), center = Offset(px, py))
                        // 拋光金屬柱身
                        drawCircle(color = Color(0xFF94A3B8), radius = 4.5.dp.toPx(), center = Offset(px, py))
                        // 柱頂高光
                        drawCircle(color = Color.White, radius = 2.dp.toPx(), center = Offset(px - 1.5.dp.toPx(), py - 1.5.dp.toPx()))
                    }

                    // 【右側實體導流台座】
                    val rightApronPath = Path().apply {
                        moveTo(alleyLeftX, engine.rightGuideP1.y * scaleY - 10f * scaleY)
                        lineTo(engine.rightGuideP1.x * scaleX, engine.rightGuideP1.y * scaleY)
                        lineTo(engine.rightGuideP2.x * scaleX, engine.rightGuideP2.y * scaleY)
                        lineTo(engine.rightGuideP2.x * scaleX, (engine.rightFlipper.pivot.y + 30f) * scaleY)
                        lineTo(alleyLeftX, (engine.rightFlipper.pivot.y + 30f) * scaleY)
                        close()
                    }
                    drawPath(
                        path = rightApronPath,
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF1E1B4B)),
                            start = Offset(alleyLeftX, engine.rightGuideP1.y * scaleY),
                            end = Offset(engine.rightGuideP2.x * scaleX, engine.rightGuideP2.y * scaleY)
                        )
                    )
                    // 右立體雙管鍍鉻金屬導軌 (Wireform Rail)
                    drawLine(
                        color = Color(0xFF1E293B),
                        start = Offset(engine.rightGuideP1.x * scaleX + 2f, engine.rightGuideP1.y * scaleY + 2f),
                        end = Offset(engine.rightGuideP2.x * scaleX + 2f, engine.rightGuideP2.y * scaleY + 2f),
                        strokeWidth = 5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color(0xFF94A3B8),
                        start = Offset(engine.rightGuideP1.x * scaleX, engine.rightGuideP1.y * scaleY),
                        end = Offset(engine.rightGuideP2.x * scaleX, engine.rightGuideP2.y * scaleY),
                        strokeWidth = 3.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color(0xFFF8FAFC),
                        start = Offset(engine.rightGuideP1.x * scaleX - 1f, engine.rightGuideP1.y * scaleY - 1f),
                        end = Offset(engine.rightGuideP2.x * scaleX - 1f, engine.rightGuideP2.y * scaleY - 1f),
                        strokeWidth = 1.2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    // 右側導流柱電鍍圓柱與橡膠環
                    listOf(engine.rightGuideP1, engine.rightGuideP2).forEach { pt ->
                        val px = pt.x * scaleX
                        val py = pt.y * scaleY
                        drawCircle(color = Color(0xFF0F172A), radius = 6.dp.toPx(), center = Offset(px, py))
                        drawCircle(color = Color(0xFF94A3B8), radius = 4.5.dp.toPx(), center = Offset(px, py))
                        drawCircle(color = Color.White, radius = 2.dp.toPx(), center = Offset(px - 1.5.dp.toPx(), py - 1.5.dp.toPx()))
                    }

                    // 7. 三角形彈性保險桿 (Slingshots)
                    for (sling in engine.slingshots) {
                        val path = Path().apply {
                            moveTo(sling.p1.x * scaleX, sling.p1.y * scaleY)
                            lineTo(sling.p2.x * scaleX, sling.p2.y * scaleY)
                            lineTo(sling.p3.x * scaleX, sling.p3.y * scaleY)
                            close()
                        }
                        drawPath(
                            path = path,
                            color = if (sling.hitAnim > 0f) Color(0xFFFF9100) else Color(0xFF0288D1).copy(alpha = 0.5f)
                        )
                        drawPath(
                            path = path,
                            color = Color(0xFF81D4FA),
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // 8. 頂部 Rollover 燈條
                    for (target in engine.rollovers) {
                        val cx = target.center.x * scaleX
                        val cy = target.center.y * scaleY
                        val w = target.width * scaleX
                        val h = target.height * scaleY
                        drawRoundRect(
                            color = if (target.isLit) target.color else target.color.copy(alpha = 0.25f),
                            topLeft = Offset(cx - w / 2f, cy - h / 2f),
                            size = Size(w, h),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }

                    // 9. Bumpers 彈簧柱
                    for (bumper in engine.bumpers) {
                        val bx = bumper.center.x * scaleX
                        val by = bumper.center.y * scaleY
                        val br = bumper.radius * scaleX * (1f + bumper.hitAnim * 0.2f)

                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(bumper.color.copy(alpha = 0.6f), Color.Transparent),
                                center = Offset(bx, by),
                                radius = br * 1.5f
                            ),
                            radius = br * 1.5f,
                            center = Offset(bx, by)
                        )
                        drawCircle(
                            color = bumper.color,
                            radius = br,
                            center = Offset(bx, by)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.85f),
                            radius = br * 0.45f,
                            center = Offset(bx, by)
                        )
                    }

                    // 10. 難度特殊機關
                    engine.centerPost?.let { post ->
                        val px = post.center.x * scaleX
                        val py = post.center.y * scaleY
                        val pr = post.radius * scaleX * (1f + post.hitAnim * 0.3f)
                        drawCircle(
                            color = Color(0xFF00E676),
                            radius = pr,
                            center = Offset(px, py)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = pr * 0.4f,
                            center = Offset(px, py)
                        )
                    }

                    engine.vortex?.let { vox ->
                        val vx = vox.center.x * scaleX
                        val vy = vox.center.y * scaleY
                        val vr = vox.radius * scaleX
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF8B5CF6).copy(alpha = 0.5f), Color.Transparent),
                                center = Offset(vx, vy),
                                radius = vr
                            ),
                            radius = vr,
                            center = Offset(vx, vy)
                        )
                        for (i in 0 until 4) {
                            val ang = (vox.rotationAngle + i * 90f) * (PI / 180f)
                            drawLine(
                                color = Color(0xFFC084FC),
                                start = Offset(vx, vy),
                                end = Offset((vx + cos(ang) * vr).toFloat(), (vy + sin(ang) * vr).toFloat()),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                    }

                    engine.movingObstacle?.let { mob ->
                        val mx = mob.x * scaleX
                        val my = mob.y * scaleY
                        val mw = mob.width * scaleX
                        val mh = mob.height * scaleY
                        drawRoundRect(
                            color = if (mob.hitAnim > 0f) Color(0xFFFF9800) else Color(0xFFF59E0B),
                            topLeft = Offset(mx - mw / 2f, my - mh / 2f),
                            size = Size(mw, mh),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }

                    engine.magneticWell?.let { mag ->
                        val mx = mag.center.x * scaleX
                        val my = mag.center.y * scaleY
                        val mr = mag.radius * scaleX * (0.85f + 0.15f * sin(mag.pulseAnim))
                        drawCircle(
                            color = Color(0xFF06B6D4).copy(alpha = 0.25f),
                            radius = mr,
                            center = Offset(mx, my),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }

                    // 11. 繪製左右擋板 (黃金比例長度與軸心)
                    drawFlipper(engine.leftFlipper, scaleX, scaleY)
                    drawFlipper(engine.rightFlipper, scaleX, scaleY)

                    // 12. 繪製彈珠 (Pinball)
                    if (engine.pinball.isAlive) {
                        val ballX = engine.pinball.x * scaleX
                        val pullOffset = if (engine.pinball.inAlley) (plungerDragDistance / maxDragDistance) * 35f else 0f
                        val ballY = (engine.pinball.y + pullOffset) * scaleY
                        val ballR = engine.pinball.radius * scaleX

                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.White, Color(0xFFE2E8F0), Color(0xFF64748B)),
                                center = Offset(ballX - ballR * 0.3f, ballY - ballR * 0.3f),
                                radius = ballR
                            ),
                            radius = ballR,
                            center = Offset(ballX, ballY)
                        )
                    }

                    // 13. 繪製火花粒子
                    for (p in particles) {
                        drawCircle(
                            color = p.color.copy(alpha = p.alpha.coerceIn(0f, 1f)),
                            radius = p.size * scaleX * p.alpha,
                            center = Offset(p.x * scaleX, p.y * scaleY)
                        )
                    }

                    // 14. 繪製得分漂浮字
                    for (txt in floatingTexts) {
                        drawContext.canvas.nativeCanvas.apply {
                            val paint = android.graphics.Paint().apply {
                                color = android.graphics.Color.argb(
                                    (txt.alpha.coerceIn(0f, 1f) * 255).toInt(),
                                    (txt.color.red * 255).toInt(),
                                    (txt.color.green * 255).toInt(),
                                    (txt.color.blue * 255).toInt()
                                )
                                textSize = 16.sp.toPx() * scaleX
                                isFakeBoldText = true
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                            drawText(txt.text, txt.x * scaleX, txt.y * scaleY, paint)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 底部雙手擋板操作大按鍵 (左半腦 / 右半腦對應)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 左手擋板鍵
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp)
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                engine.leftFlipper.isPressed = true
                                isLeftPressedState = true
                                SoundManager.playFlipperSnap()
                                waitForUpOrCancellation()
                                engine.leftFlipper.isPressed = false
                                isLeftPressedState = false
                            }
                        },
                    shape = RoundedCornerShape(18.dp),
                    color = if (isLeftPressedState) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = if (isLeftPressedState) 1.dp else 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "◀ 左手擋板 (右腦)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isLeftPressedState) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // 右手擋板鍵
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp)
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                engine.rightFlipper.isPressed = true
                                isRightPressedState = true
                                SoundManager.playFlipperSnap()
                                waitForUpOrCancellation()
                                engine.rightFlipper.isPressed = false
                                isRightPressedState = false
                            }
                        },
                    shape = RoundedCornerShape(18.dp),
                    color = if (isRightPressedState) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = if (isRightPressedState) 1.dp else 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "右手擋板 (左腦) ▶",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isRightPressedState) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // 結算彈窗 (Game Over)
        if (isGameOver) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = Localization.getString("game_over", language),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "最終得分",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$score",
                                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "通關時間: ${elapsedSeconds} 秒",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    SoundManager.playClick()
                                    restartGame()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(Localization.getString("play_again", language))
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
                                Text(Localization.getString("back_to_menu", language))
                            }
                        }
                    }
                }
            }
        }
    }
}

// 輔助繪製擋板
private fun DrawScope.drawFlipper(flipper: Flipper, scaleX: Float, scaleY: Float) {
    val px = flipper.pivot.x * scaleX
    val py = flipper.pivot.y * scaleY
    val rad = flipper.currentAngleDeg * (PI / 180f)
    val len = flipper.length * scaleX
    val tipX = (px + cos(rad) * len).toFloat()
    val tipY = (py + sin(rad) * len).toFloat()

    // 繪製擋板橡膠厚度身
    drawLine(
        color = Color(0xFFFF5252),
        start = Offset(px, py),
        end = Offset(tipX, tipY),
        strokeWidth = 14f * scaleX,
        cap = StrokeCap.Round
    )

    // 軸心小金屬圓
    drawCircle(
        color = Color.White,
        radius = 6f * scaleX,
        center = Offset(px, py)
    )
}
