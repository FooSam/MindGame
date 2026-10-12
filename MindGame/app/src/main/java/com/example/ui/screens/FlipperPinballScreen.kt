package com.example.ui.screens

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
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
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundManager
import com.example.data.model.AppLanguage
import com.example.data.model.GameDifficulty
import com.example.data.model.Localization
import com.example.game.pinball.Bumper
import com.example.game.pinball.Flipper
import com.example.game.pinball.FlipperPhysicsEngine
import com.example.game.pinball.FloatingText
import com.example.game.pinball.Particle
import com.example.game.pinball.Pinball
import com.example.game.pinball.Slingshot
import com.example.ui.components.AppBackground
import com.example.ui.theme.AppThemeStyle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 依難度與分數科學評定手遊成就 1~3 顆璀璨金星
 */
fun calculatePinballStars(difficulty: GameDifficulty, score: Int): Int {
    return when (difficulty) {
        GameDifficulty.BEGINNER -> when {
            score >= 3000 -> 3
            score >= 1500 -> 2
            else -> 1
        }
        GameDifficulty.INTERMEDIATE -> when {
            score >= 4500 -> 3
            score >= 2500 -> 2
            else -> 1
        }
        else -> when {
            score >= 6000 -> 3
            score >= 3500 -> 2
            else -> 1
        }
    }
}

/**
 * 《珠過來啊！》次世代頂級視覺重構畫面 (FlipperPinballScreen)
 * 導入 80s 美式復古未來主義 Synthwave 電玩廳底盤、超寫實鍍鉻鏡面鋼珠、
 * 彈簧回彈式發光蘑菇柱 Bumpers、重裝高分子防滑雙擋板、LED 點陣 HUD 與 3 星成就結算。
 */
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
                repeat(8) {
                    val angle = Math.random() * 2.0 * PI
                    val spd = 70f + Math.random().toFloat() * 150f
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
                floatingTexts.add(FloatingText("+$pts", bumper.center.x, bumper.center.y - 22f, bumper.color))
            },
            onRolloverLit = { target, pts ->
                SoundManager.playPinDrop()
                score += pts
                floatingTexts.add(FloatingText("LIT! +$pts", target.center.x, target.center.y - 18f, target.color))
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
                engine.bumpers.forEach { if (it.hitAnim > 0f) it.hitAnim = (it.hitAnim - dt * 4.5f).coerceAtLeast(0f) }
                engine.slingshots.forEach { if (it.hitAnim > 0f) it.hitAnim = (it.hitAnim - dt * 4.5f).coerceAtLeast(0f) }
                engine.centerPost?.let { if (it.hitAnim > 0f) it.hitAnim = (it.hitAnim - dt * 4.5f).coerceAtLeast(0f) }

                // 驅動 Compose Canvas 連續重繪
                renderTick = nowNanos
            }
        }
    }

    AppBackground(themeStyle = appTheme) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 6.dp)
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
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color(0xFF38BDF8)
                    )
                    Text(
                        text = "${difficulty.name} • ${elapsedSeconds}s",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
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
                        tint = Color(0xFFFFD54F)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ★★★ 80s 街機點陣 LED 全息記分牌 HUD (ArcadeDotMatrixHud) ★★★
            ArcadeDotMatrixHud(
                score = score,
                ballsLeft = ballsLeft,
                totalBalls = totalBallsCount
            )

            Spacer(modifier = Modifier.height(6.dp))

            // ★★★ 彈珠台畫布主區域 (Synthwave Arcade Table) ★★★
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF090D18))
                    .border(
                        width = 2.dp,
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFF475569), Color(0xFF1E293B), Color(0xFF0F172A))
                        ),
                        shape = RoundedCornerShape(22.dp)
                    )
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

                    val alleyLeftX = engine.mainPlayWidth * scaleX
                    val alleyRightX = (engine.virtualWidth - 8f) * scaleX

                    // 1. 繪製 80s 美式復古未來主義機台底板、外框與發射走道 (Cabinet & Rails)
                    drawSynthwaveArcadeCabinet(
                        engine = engine,
                        scaleX = scaleX,
                        scaleY = scaleY,
                        canvasWidth = canvasWidth,
                        canvasHeight = canvasHeight,
                        alleyLeftX = alleyLeftX,
                        alleyRightX = alleyRightX,
                        plungerDragDistance = plungerDragDistance,
                        maxDragDistance = maxDragDistance,
                        isGameOver = isGameOver
                    )

                    // 2. 繪製三角形彈性保險桿 (Slingshots)
                    for (sling in engine.slingshots) {
                        val path = Path().apply {
                            moveTo(sling.p1.x * scaleX, sling.p1.y * scaleY)
                            lineTo(sling.p2.x * scaleX, sling.p2.y * scaleY)
                            lineTo(sling.p3.x * scaleX, sling.p3.y * scaleY)
                            close()
                        }
                        // 底層陰影
                        drawPath(
                            path = path,
                            brush = Brush.linearGradient(
                                colors = if (sling.hitAnim > 0f) {
                                    listOf(Color(0xFFFF9100), Color(0xFFFF3D00))
                                } else {
                                    listOf(Color(0xFF0284C7).copy(alpha = 0.6f), Color(0xFF0F172A).copy(alpha = 0.8f))
                                },
                                start = Offset(sling.p1.x * scaleX, sling.p1.y * scaleY),
                                end = Offset(sling.p2.x * scaleX, sling.p2.y * scaleY)
                            )
                        )
                        // 外框高飽和發光輪廓
                        drawPath(
                            path = path,
                            color = if (sling.hitAnim > 0f) Color(0xFFFFD54F) else Color(0xFF38BDF8),
                            style = Stroke(width = 2.5f * scaleX, cap = StrokeCap.Round)
                        )
                    }

                    // 3. 頂部 Rollover 全息微晶燈條
                    for (target in engine.rollovers) {
                        val cx = target.center.x * scaleX
                        val cy = target.center.y * scaleY
                        val w = target.width * scaleX
                        val h = target.height * scaleY
                        // 燈槽深色底座
                        drawRoundRect(
                            color = Color(0xFF0B0F19),
                            topLeft = Offset(cx - w / 2f - 1.dp.toPx(), cy - h / 2f - 1.dp.toPx()),
                            size = Size(w + 2.dp.toPx(), h + 2.dp.toPx()),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                        // 發光燈條
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = if (target.isLit) {
                                    listOf(Color.White, target.color, target.color)
                                } else {
                                    listOf(target.color.copy(alpha = 0.35f), target.color.copy(alpha = 0.15f))
                                }
                            ),
                            topLeft = Offset(cx - w / 2f, cy - h / 2f),
                            size = Size(w, h),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx())
                        )
                        if (target.isLit) {
                            // 頂部高光
                            drawLine(
                                color = Color.White,
                                start = Offset(cx - w / 3f, cy - h / 2f + 1f),
                                end = Offset(cx + w / 3f, cy - h / 2f + 1f),
                                strokeWidth = 1.2.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    // 4. 繪製彈簧回彈式發光蘑菇柱 (Synthwave Arcade Bumpers)
                    for (bumper in engine.bumpers) {
                        drawSynthwaveArcadeBumper(bumper, scaleX, scaleY)
                    }

                    // 5. 難度特殊機關精雕
                    engine.centerPost?.let { post ->
                        val px = post.center.x * scaleX
                        val py = post.center.y * scaleY
                        val pr = post.radius * scaleX * (1f + post.hitAnim * 0.25f)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF4ADE80), Color(0xFF16A34A), Color(0xFF052E16)),
                                center = Offset(px - pr * 0.3f, py - pr * 0.3f),
                                radius = pr
                            ),
                            radius = pr,
                            center = Offset(px, py)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = pr * 0.35f,
                            center = Offset(px - pr * 0.28f, py - pr * 0.28f)
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
                            brush = Brush.linearGradient(
                                colors = if (mob.hitAnim > 0f) {
                                    listOf(Color(0xFFFFB74D), Color(0xFFFF5722))
                                } else {
                                    listOf(Color(0xFFF59E0B), Color(0xFFB45309))
                                }
                            ),
                            topLeft = Offset(mx - mw / 2f, my - mh / 2f),
                            size = Size(mw, mh),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.8f),
                            start = Offset(mx - mw / 2f + 4f, my - mh / 2f + 2f),
                            end = Offset(mx + mw / 2f - 4f, my - mh / 2f + 2f),
                            strokeWidth = 1.2.dp.toPx()
                        )
                    }

                    engine.magneticWell?.let { mag ->
                        val mx = mag.center.x * scaleX
                        val my = mag.center.y * scaleY
                        val mr = mag.radius * scaleX * (0.85f + 0.15f * sin(mag.pulseAnim))
                        drawCircle(
                            color = Color(0xFF06B6D4).copy(alpha = 0.28f),
                            radius = mr,
                            center = Offset(mx, my),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }

                    // 6. 繪製重裝防滑雙擋板 (Heavy-Duty Flippers)
                    drawHeavyDutyFlipper(engine.leftFlipper, scaleX, scaleY)
                    drawHeavyDutyFlipper(engine.rightFlipper, scaleX, scaleY)

                    // 7. 繪製次世代超寫實鍍鉻鏡面鋼珠 (HyperChrome Pinball)
                    val pullOffset = if (engine.pinball.inAlley) (plungerDragDistance / maxDragDistance) * 35f else 0f
                    drawHyperChromePinball(engine.pinball, scaleX, scaleY, pullOffset)

                    // 8. 繪製火花粒子
                    for (p in particles) {
                        drawCircle(
                            color = p.color.copy(alpha = p.alpha.coerceIn(0f, 1f)),
                            radius = p.size * scaleX * p.alpha,
                            center = Offset(p.x * scaleX, p.y * scaleY)
                        )
                    }

                    // 9. 繪製得分漂浮字
                    for (txt in floatingTexts) {
                        drawContext.canvas.nativeCanvas.apply {
                            val paint = android.graphics.Paint().apply {
                                color = android.graphics.Color.argb(
                                    (txt.alpha.coerceIn(0f, 1f) * 255).toInt(),
                                    (txt.color.red * 255).toInt(),
                                    (txt.color.green * 255).toInt(),
                                    (txt.color.blue * 255).toInt()
                                )
                                textSize = 17.sp.toPx() * scaleX
                                isFakeBoldText = true
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                            drawText(txt.text, txt.x * scaleX, txt.y * scaleY, paint)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ★★★ 3D 水晶微倒角電競雙手擋板操作大按鍵 (ArcadeFlipperControlButtons) ★★★
            ArcadeFlipperControlButtons(
                isLeftPressed = isLeftPressedState,
                isRightPressed = isRightPressedState,
                onLeftPressStart = {
                    engine.leftFlipper.isPressed = true
                    isLeftPressedState = true
                    SoundManager.playFlipperSnap()
                },
                onLeftPressEnd = {
                    engine.leftFlipper.isPressed = false
                    isLeftPressedState = false
                },
                onRightPressStart = {
                    engine.rightFlipper.isPressed = true
                    isRightPressedState = true
                    SoundManager.playFlipperSnap()
                },
                onRightPressEnd = {
                    engine.rightFlipper.isPressed = false
                    isRightPressedState = false
                }
            )
        }

        // ★★★ 次世代手遊成就 3 星結算彈窗 (PinballVictoryDialog) ★★★
        if (isGameOver) {
            PinballVictoryDialog(
                difficulty = difficulty,
                score = score,
                elapsedSeconds = elapsedSeconds,
                language = language,
                onPlayAgain = {
                    SoundManager.playClick()
                    restartGame()
                },
                onLeaderboard = {
                    SoundManager.playClick()
                    onLeaderboardClick()
                },
                onBackMenu = {
                    SoundManager.playClick()
                    onBackClick()
                }
            )
        }
    }
}

/**
 * 80s 街機點陣 LED 全息記分牌 HUD
 */
@Composable
private fun ArcadeDotMatrixHud(
    score: Int,
    ballsLeft: Int,
    totalBalls: Int
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.85f),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.2.dp,
                brush = Brush.horizontalGradient(
                    listOf(Color(0xFF38BDF8), Color(0xFFA855F7), Color(0xFFFFD54F))
                ),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 得分點陣看板
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E293B)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Score",
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "SCORE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = String.format("%,d", score),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.5.sp
                        ),
                        color = Color(0xFFFFD54F)
                    )
                }
            }

            // 鋼珠槽立體鍍鉻徽章
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "BALLS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(totalBalls) { idx ->
                        val isAvailable = idx < ballsLeft
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isAvailable) {
                                        Brush.radialGradient(
                                            listOf(Color.White, Color(0xFF38BDF8), Color(0xFF0369A1))
                                        )
                                    } else {
                                        Brush.radialGradient(
                                            listOf(Color(0xFF334155), Color(0xFF1E293B))
                                        )
                                    }
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isAvailable) Color(0xFFE2E8F0) else Color(0xFF475569),
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }
        }
    }
}

/**
 * 3D 水晶微倒角電競雙手擋板操作大按鍵
 */
@Composable
private fun ArcadeFlipperControlButtons(
    isLeftPressed: Boolean,
    isRightPressed: Boolean,
    onLeftPressStart: () -> Unit,
    onLeftPressEnd: () -> Unit,
    onRightPressStart: () -> Unit,
    onRightPressEnd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 左手擋板鍵 (電光藍晶)
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(60.dp)
                .shadow(
                    elevation = if (isLeftPressed) 2.dp else 6.dp,
                    shape = RoundedCornerShape(20.dp),
                    spotColor = Color(0xFF38BDF8)
                )
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        onLeftPressStart()
                        waitForUpOrCancellation()
                        onLeftPressEnd()
                    }
                },
            shape = RoundedCornerShape(20.dp),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            if (isLeftPressed) {
                                listOf(Color(0xFF0284C7), Color(0xFF0369A1))
                            } else {
                                listOf(Color(0xFF38BDF8), Color(0xFF0284C7))
                            }
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .border(
                        width = 1.5.dp,
                        color = Color.White.copy(alpha = if (isLeftPressed) 0.4f else 0.8f),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "◀",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "左擋板",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                            color = Color.White
                        )
                        Text(
                            text = "右半腦直覺",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        // 右手擋板鍵 (幻紫水晶)
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(60.dp)
                .shadow(
                    elevation = if (isRightPressed) 2.dp else 6.dp,
                    shape = RoundedCornerShape(20.dp),
                    spotColor = Color(0xFFA855F7)
                )
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        onRightPressStart()
                        waitForUpOrCancellation()
                        onRightPressEnd()
                    }
                },
            shape = RoundedCornerShape(20.dp),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            if (isRightPressed) {
                                listOf(Color(0xFF7C3AED), Color(0xFF6D28D9))
                            } else {
                                listOf(Color(0xFFA855F7), Color(0xFF7C3AED))
                            }
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .border(
                        width = 1.5.dp,
                        color = Color.White.copy(alpha = if (isRightPressed) 0.4f else 0.8f),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "右擋板",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                            color = Color.White
                        )
                        Text(
                            text = "左半腦邏輯",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "▶",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

/**
 * 手遊成就 3 星結算彈窗 (PinballVictoryDialog)
 */
@Composable
private fun PinballVictoryDialog(
    difficulty: GameDifficulty,
    score: Int,
    elapsedSeconds: Int,
    language: AppLanguage,
    onPlayAgain: () -> Unit,
    onLeaderboard: () -> Unit,
    onBackMenu: () -> Unit
) {
    val stars = calculatePinballStars(difficulty, score)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .padding(16.dp),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.5.dp,
                brush = Brush.verticalGradient(
                    listOf(Color(0xFFFFD54F), Color(0xFF38BDF8), Color(0xFF1E293B))
                )
            )
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 3 星耀眼金色徽章
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..3) {
                        val isLit = i <= stars
                        Box(
                            modifier = Modifier
                                .size(if (i == 2) 48.dp else 40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isLit) {
                                        Brush.radialGradient(
                                            listOf(Color(0xFFFFF9C4), Color(0xFFFFD54F), Color(0xFFF57F17))
                                        )
                                    } else {
                                        Brush.radialGradient(
                                            listOf(Color(0xFF334155), Color(0xFF1E293B))
                                        )
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Star $i",
                                tint = if (isLit) Color.White else Color(0xFF64748B),
                                modifier = Modifier.size(if (i == 2) 28.dp else 22.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = Localization.getString("game_over", language),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                    color = Color(0xFFFFD54F),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "${difficulty.name} MODE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    ),
                    color = Color(0xFF38BDF8)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 得分與時間資訊卡片
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF1E293B).copy(alpha = 0.8f),
                    modifier = Modifier.fillMaxWidth(),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "最終得分",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = String.format("%,d", score),
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = Color(0xFFFFD54F)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Time",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "存活時間: ${elapsedSeconds} 秒",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFE2E8F0)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 水晶操作按鈕組
                Button(
                    onClick = onPlayAgain,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        Localization.getString("play_again", language),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onLeaderboard,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                    ) {
                        Icon(Icons.Default.Leaderboard, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("排行榜", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Button(
                        onClick = onBackMenu,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                    ) {
                        Text(
                            Localization.getString("back_to_menu", language),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 繪製 80s 美式復古未來主義機台底板、外框與發射走道 (Cabinet & Rails)
 */
private fun DrawScope.drawSynthwaveArcadeCabinet(
    engine: FlipperPhysicsEngine,
    scaleX: Float,
    scaleY: Float,
    canvasWidth: Float,
    canvasHeight: Float,
    alleyLeftX: Float,
    alleyRightX: Float,
    plungerDragDistance: Float,
    maxDragDistance: Float,
    isGameOver: Boolean
) {
    // 1. Synthwave 霓虹光柵漸層底板
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF070B14), Color(0xFF131127), Color(0xFF1E1035))
        )
    )

    // 1.1 柔和等距橫向霓虹網格線條
    val gridCount = 14
    for (i in 1..gridCount) {
        val y = canvasHeight * (i / (gridCount + 1f))
        drawLine(
            color = Color(0xFF818CF8).copy(alpha = 0.07f),
            start = Offset(0f, y),
            end = Offset(canvasWidth, y),
            strokeWidth = 1.dp.toPx()
        )
    }

    // 2. 右側獨立發射走道底色與深色槽
    drawRect(
        color = Color(0xFF070C1B).copy(alpha = 0.85f),
        topLeft = Offset(alleyLeftX, 65f * scaleY),
        size = Size(alleyRightX - alleyLeftX, canvasHeight - 65f * scaleY)
    )

    // 2.1 發射走道實體彈簧拉桿與蓄力反饋
    val plungerCenterX = (alleyLeftX + alleyRightX) / 2f
    val pullRatio = (plungerDragDistance / maxDragDistance).coerceIn(0f, 1f)
    val plungerTopY = (engine.virtualHeight - 65f + pullRatio * 35f) * scaleY
    val plungerBottomY = canvasHeight - 4.dp.toPx()

    val coilCount = 8
    val coilStep = (plungerBottomY - plungerTopY) / coilCount
    for (i in 0 until coilCount) {
        val cy = plungerTopY + i * coilStep
        val cOffset = if (i % 2 == 0) -4.5.dp.toPx() else 4.5.dp.toPx()
        drawLine(
            color = if (pullRatio > 0.05f) Color(0xFFFFAB91) else Color(0xFF94A3B8),
            start = Offset(plungerCenterX - cOffset, cy),
            end = Offset(plungerCenterX + cOffset, cy + coilStep),
            strokeWidth = 2.2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
    drawRoundRect(
        color = if (pullRatio > 0.05f) Color(0xFFFF5722) else Color(0xFF38BDF8),
        topLeft = Offset(plungerCenterX - 8.dp.toPx(), plungerTopY - 5.dp.toPx()),
        size = Size(16.dp.toPx(), 6.dp.toPx()),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
    )

    // 2.2 發射提示與即時蓄力百分比
    if (engine.pinball.inAlley && !isGameOver) {
        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                color = if (pullRatio > 0.05f) android.graphics.Color.argb(255, 255, 112, 67) else android.graphics.Color.argb(220, 56, 189, 248)
                textSize = 11.sp.toPx()
                isFakeBoldText = true
                textAlign = android.graphics.Paint.Align.CENTER
            }
            val label = if (pullRatio > 0.05f) "${(pullRatio * 100).toInt()}%" else "▼ 下拉"
            drawText(label, plungerCenterX, plungerTopY - 26.dp.toPx(), paint)
        }
    }

    // 3. 頂部雙層拋光鍍鉻圓弧頂部導軌
    val archPath = Path().apply {
        moveTo(alleyRightX, 65f * scaleY)
        cubicTo(
            alleyRightX, 14f * scaleY,
            alleyLeftX + 5f * scaleX, 14f * scaleY,
            alleyLeftX - 25f * scaleX, 14f * scaleY
        )
    }
    // 底層陰影導軌
    drawPath(
        path = archPath,
        color = Color(0xFF0F172A),
        style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
    )
    // 中層鍍鉻導軌
    drawPath(
        path = archPath,
        color = Color(0xFF38BDF8),
        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
    )
    // 頂層白熾鏡面高光線
    drawPath(
        path = archPath,
        color = Color.White,
        style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
    )

    // 4. 外軌與發射通道實木/合金隔板
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(Color(0xFF1E293B), Color(0xFF475569), Color(0xFF0F172A)),
            startX = alleyLeftX - 6.dp.toPx(),
            endX = alleyLeftX + 2.dp.toPx()
        ),
        topLeft = Offset(alleyLeftX - 6.dp.toPx(), 65f * scaleY),
        size = Size(8.dp.toPx(), canvasHeight - 65f * scaleY)
    )
    drawLine(
        color = Color(0xFFCBD5E1),
        start = Offset(alleyLeftX - 5.dp.toPx(), 65f * scaleY),
        end = Offset(alleyLeftX - 5.dp.toPx(), canvasHeight),
        strokeWidth = 1.2.dp.toPx()
    )

    // 5. 左側重裝機台邊框 (Cabinet Side Rails)
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(Color(0xFF0A0F1D), Color(0xFF334155), Color(0xFF1E293B)),
            startX = 0f,
            endX = 14f * scaleX
        ),
        topLeft = Offset(0f, 20f * scaleY),
        size = Size(14f * scaleX, canvasHeight)
    )
    drawLine(
        color = Color(0xFF94A3B8),
        start = Offset(13f * scaleX, 28f * scaleY),
        end = Offset(13f * scaleX, canvasHeight),
        strokeWidth = 2.dp.toPx()
    )

    // 頂壁實體雙層金屬導軌
    drawLine(
        color = Color(0xFF475569),
        start = Offset(13f * scaleX, 14f * scaleY),
        end = Offset(alleyLeftX - 25f * scaleX, 14f * scaleY),
        strokeWidth = 4.dp.toPx(),
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFFF1F5F9),
        start = Offset(14f * scaleX, 14f * scaleY),
        end = Offset(alleyLeftX - 25f * scaleX, 14f * scaleY),
        strokeWidth = 1.2.dp.toPx(),
        cap = StrokeCap.Round
    )

    // 6. 左側實體導流台座 (Apron Block)
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
        color = Color.White,
        start = Offset(engine.leftGuideP1.x * scaleX - 1f, engine.leftGuideP1.y * scaleY - 1f),
        end = Offset(engine.leftGuideP2.x * scaleX - 1f, engine.leftGuideP2.y * scaleY - 1f),
        strokeWidth = 1.2.dp.toPx(),
        cap = StrokeCap.Round
    )
    listOf(engine.leftGuideP1, engine.leftGuideP2).forEach { pt ->
        val px = pt.x * scaleX
        val py = pt.y * scaleY
        drawCircle(color = Color(0xFF0F172A), radius = 6.dp.toPx(), center = Offset(px, py))
        drawCircle(color = Color(0xFF94A3B8), radius = 4.5.dp.toPx(), center = Offset(px, py))
        drawCircle(color = Color.White, radius = 2.dp.toPx(), center = Offset(px - 1.5.dp.toPx(), py - 1.5.dp.toPx()))
    }

    // 7. 右側實體導流台座 (Apron Block)
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
        color = Color.White,
        start = Offset(engine.rightGuideP1.x * scaleX - 1f, engine.rightGuideP1.y * scaleY - 1f),
        end = Offset(engine.rightGuideP2.x * scaleX - 1f, engine.rightGuideP2.y * scaleY - 1f),
        strokeWidth = 1.2.dp.toPx(),
        cap = StrokeCap.Round
    )
    listOf(engine.rightGuideP1, engine.rightGuideP2).forEach { pt ->
        val px = pt.x * scaleX
        val py = pt.y * scaleY
        drawCircle(color = Color(0xFF0F172A), radius = 6.dp.toPx(), center = Offset(px, py))
        drawCircle(color = Color(0xFF94A3B8), radius = 4.5.dp.toPx(), center = Offset(px, py))
        drawCircle(color = Color.White, radius = 2.dp.toPx(), center = Offset(px - 1.5.dp.toPx(), py - 1.5.dp.toPx()))
    }
}

/**
 * 次世代拋光鍍鉻超寫實鏡面鋼珠繪製 (HyperChrome Pinball)
 */
private fun DrawScope.drawHyperChromePinball(
    pinball: Pinball,
    scaleX: Float,
    scaleY: Float,
    plungerOffset: Float
) {
    if (!pinball.isAlive) return
    val ballX = pinball.x * scaleX
    val ballY = (pinball.y + plungerOffset) * scaleY
    val ballR = pinball.radius * scaleX

    // 1. 移動微光尾跡 (當有速度時繪製動態流光光暈)
    val speedSq = pinball.vx * pinball.vx + pinball.vy * pinball.vy
    if (speedSq > 2500f) {
        val speed = sqrt(speedSq)
        val dirX = -pinball.vx / speed
        val dirY = -pinball.vy / speed
        val trailLen = (speed * 0.04f).coerceIn(4f, 22f) * scaleX
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF38BDF8).copy(alpha = 0.5f), Color.Transparent),
                start = Offset(ballX, ballY),
                end = Offset(ballX + dirX * trailLen, ballY + dirY * trailLen)
            ),
            start = Offset(ballX, ballY),
            end = Offset(ballX + dirX * trailLen, ballY + dirY * trailLen),
            strokeWidth = ballR * 1.4f,
            cap = StrokeCap.Round
        )
    }

    // 2. 鋼珠底部金屬投射陰影
    drawCircle(
        color = Color(0x77000000),
        radius = ballR * 0.95f,
        center = Offset(ballX + 2f * scaleX, ballY + 3f * scaleY)
    )

    // 3. 鋼珠外圈微發光冷光藍折射光暈
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x5538BDF8), Color.Transparent),
            center = Offset(ballX, ballY),
            radius = ballR * 1.35f
        ),
        radius = ballR * 1.35f,
        center = Offset(ballX, ballY)
    )

    // 4. 拋光鍍鉻超寫實鏡面金屬漸層球體
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFFFFF), // 頂部高光焦點
                Color(0xFFF1F5F9), // 亮銀灰
                Color(0xFFCBD5E1), // 鏡面過渡
                Color(0xFF64748B), // 陰影過渡
                Color(0xFF334155), // 深鈦金屬
                Color(0xFF0F172A)  // 最深金屬輪廓
            ),
            center = Offset(ballX - ballR * 0.35f, ballY - ballR * 0.35f),
            radius = ballR * 1.05f
        ),
        radius = ballR,
        center = Offset(ballX, ballY)
    )

    // 5. 鋼珠底部次表面環境冷光回折 (Cyan/Blue Ambient bounce)
    drawArc(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x8838BDF8), Color.Transparent),
            center = Offset(ballX, ballY + ballR * 0.5f),
            radius = ballR * 0.7f
        ),
        startAngle = 30f,
        sweepAngle = 120f,
        useCenter = false,
        topLeft = Offset(ballX - ballR * 0.85f, ballY - ballR * 0.85f),
        size = Size(ballR * 1.7f, ballR * 1.7f),
        style = Stroke(width = ballR * 0.28f, cap = StrokeCap.Round)
    )

    // 6. 頂部純白高光鏡面聚光亮點
    drawCircle(
        color = Color.White,
        radius = ballR * 0.28f,
        center = Offset(ballX - ballR * 0.32f, ballY - ballR * 0.32f)
    )
    drawCircle(
        color = Color(0xCCFFFFFF),
        radius = ballR * 0.12f,
        center = Offset(ballX - ballR * 0.12f, ballY - ballR * 0.45f)
    )
}

/**
 * 彈簧回彈式發光蘑菇柱 Bumpers 精雕
 */
private fun DrawScope.drawSynthwaveArcadeBumper(
    bumper: Bumper,
    scaleX: Float,
    scaleY: Float
) {
    val bx = bumper.center.x * scaleX
    val by = bumper.center.y * scaleY
    val br = bumper.radius * scaleX * (1f + bumper.hitAnim * 0.18f)

    // 1. 命中時向外擴散的雙層霓虹震盪衝擊波環 (Bloom Shockwave)
    if (bumper.hitAnim > 0.02f) {
        val waveR1 = br * (1.3f + (1f - bumper.hitAnim) * 1.2f)
        val waveR2 = br * (1.1f + (1f - bumper.hitAnim) * 0.6f)
        drawCircle(
            color = bumper.color.copy(alpha = (bumper.hitAnim * 0.75f).coerceIn(0f, 1f)),
            radius = waveR1,
            center = Offset(bx, by),
            style = Stroke(width = 2.5f * scaleX)
        )
        drawCircle(
            color = Color.White.copy(alpha = (bumper.hitAnim * 0.9f).coerceIn(0f, 1f)),
            radius = waveR2,
            center = Offset(bx, by),
            style = Stroke(width = 1.8f * scaleX)
        )
    }

    // 2. 底層柔和等離子發光光暈
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                bumper.color.copy(alpha = if (bumper.hitAnim > 0f) 0.85f else 0.45f),
                Color.Transparent
            ),
            center = Offset(bx, by),
            radius = br * 1.7f
        ),
        radius = br * 1.7f,
        center = Offset(bx, by)
    )

    // 3. 外圈立體金屬鍍鉻雙層護座 (Chrome Bezel Ring)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF94A3B8), Color(0xFF334155), Color(0xFF0F172A)),
            center = Offset(bx - br * 0.2f, by - br * 0.2f),
            radius = br * 1.15f
        ),
        radius = br * 1.12f,
        center = Offset(bx, by)
    )
    drawCircle(
        color = Color(0xFFE2E8F0),
        radius = br * 1.12f,
        center = Offset(bx, by),
        style = Stroke(width = 1.2f * scaleX)
    )

    // 4. 中圈等離子霓虹光環 (Neon Plasma Ring)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, bumper.color, bumper.color.copy(alpha = 0.8f)),
            center = Offset(bx, by),
            radius = br * 0.95f
        ),
        radius = br * 0.92f,
        center = Offset(bx, by)
    )

    // 5. 核心水晶蘑菇頭 (Jelly Crystal Cap)
    val capRadius = br * 0.68f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                if (bumper.hitAnim > 0f) Color.White else bumper.color.copy(alpha = 0.9f),
                bumper.color,
                Color(0xFF0F172A).copy(alpha = 0.85f)
            ),
            center = Offset(bx - capRadius * 0.35f, by - capRadius * 0.35f),
            radius = capRadius * 1.1f
        ),
        radius = capRadius,
        center = Offset(bx, by)
    )

    // 6. 頂部偏心水晶聚光白點
    drawCircle(
        color = Color.White.copy(alpha = 0.95f),
        radius = capRadius * 0.35f,
        center = Offset(bx - capRadius * 0.3f, by - capRadius * 0.3f)
    )
}

/**
 * 繪製重裝高分子防滑雙擋板 (Heavy-Duty Flippers)
 */
private fun DrawScope.drawHeavyDutyFlipper(
    flipper: Flipper,
    scaleX: Float,
    scaleY: Float
) {
    val px = flipper.pivot.x * scaleX
    val py = flipper.pivot.y * scaleY
    val rad = flipper.currentAngleDeg * (PI / 180f)
    val len = flipper.length * scaleX

    // 旋轉方向向量
    val cosA = cos(rad).toFloat()
    val sinA = sin(rad).toFloat()
    val tipX = px + cosA * len
    val tipY = py + sinA * len

    // 法向量 (垂直於擋板方向)
    val normX = -sinA
    val normY = cosA

    val rootRadius = 9.5f * scaleX
    val tipRadius = 5.2f * scaleX

    // 水滴錐度造型 Path
    val p1 = Offset(px + normX * rootRadius, py + normY * rootRadius)
    val p2 = Offset(tipX + normX * tipRadius, tipY + normY * tipRadius)
    val p3 = Offset(tipX - normX * tipRadius, tipY - normY * tipRadius)
    val p4 = Offset(px - normX * rootRadius, py - normY * rootRadius)

    val flipperPath = Path().apply {
        moveTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        // 圓尖端
        arcTo(
            rect = androidx.compose.ui.geometry.Rect(
                center = Offset(tipX, tipY),
                radius = tipRadius
            ),
            startAngleDegrees = (rad * 180f / PI).toFloat() - 90f,
            sweepAngleDegrees = 180f,
            forceMoveTo = false
        )
        lineTo(p4.x, p4.y)
        // 圓根部
        arcTo(
            rect = androidx.compose.ui.geometry.Rect(
                center = Offset(px, py),
                radius = rootRadius
            ),
            startAngleDegrees = (rad * 180f / PI).toFloat() + 90f,
            sweepAngleDegrees = 180f,
            forceMoveTo = false
        )
        close()
    }

    // 1. 若處於按下狀態，繪製高飽和等離子電弧發光外框 (Electric Glow Aura)
    if (flipper.isPressed) {
        drawPath(
            path = flipperPath,
            color = if (flipper.isLeft) Color(0xFF38BDF8).copy(alpha = 0.55f) else Color(0xFFA855F7).copy(alpha = 0.55f),
            style = Stroke(width = 6f * scaleX)
        )
    }

    // 2. 底部立體厚重深色陰影
    drawPath(
        path = flipperPath,
        color = Color(0x99000000)
    )

    // 3. 高分子防滑橡膠錐度本體漸層 (左藍右紫電競美學)
    val mainColors = if (flipper.isLeft) {
        listOf(Color(0xFF38BDF8), Color(0xFF0284C7), Color(0xFF0369A1))
    } else {
        listOf(Color(0xFFA855F7), Color(0xFF7C3AED), Color(0xFF581C87))
    }
    drawPath(
        path = flipperPath,
        brush = Brush.linearGradient(
            colors = mainColors,
            start = Offset(p1.x, p1.y),
            end = Offset(p4.x, p4.y)
        )
    )

    // 4. 上緣拋光微高光線條 (Chrome highlight)
    drawLine(
        color = Color.White.copy(alpha = 0.85f),
        start = Offset(p1.x - normX * 1.5f, p1.y - normY * 1.5f),
        end = Offset(p2.x - normX * 1.5f, p2.y - normY * 1.5f),
        strokeWidth = 1.8f * scaleX,
        cap = StrokeCap.Round
    )

    // 5. 橡膠表面防滑微凹凸刻紋 (3道微槽)
    for (i in 1..3) {
        val fraction = i * 0.22f
        val grooveCenter = Offset(px + cosA * (len * fraction), py + sinA * (len * fraction))
        val gHalf = rootRadius * (1f - fraction * 0.4f) * 0.7f
        drawLine(
            color = Color.Black.copy(alpha = 0.35f),
            start = Offset(grooveCenter.x + normX * gHalf, grooveCenter.y + normY * gHalf),
            end = Offset(grooveCenter.x - normX * gHalf, grooveCenter.y - normY * gHalf),
            strokeWidth = 1.2f * scaleX,
            cap = StrokeCap.Round
        )
    }

    // 6. 軸心處精密機械雙層軸承與鍍鉻螺栓 (Chrome Pivot Nut)
    // 6.1 黑色橡膠襯墊
    drawCircle(
        color = Color(0xFF0F172A),
        radius = rootRadius * 1.05f,
        center = Offset(px, py)
    )
    // 6.2 雙層鍍鉻金屬轉軸
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFF8FAFC), Color(0xFF94A3B8), Color(0xFF334155)),
            center = Offset(px - rootRadius * 0.25f, py - rootRadius * 0.25f),
            radius = rootRadius * 0.75f
        ),
        radius = rootRadius * 0.75f,
        center = Offset(px, py)
    )
    // 6.3 軸頂高光微亮點
    drawCircle(
        color = Color.White,
        radius = rootRadius * 0.22f,
        center = Offset(px - rootRadius * 0.25f, py - rootRadius * 0.25f)
    )
}
