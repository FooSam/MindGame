package com.example.game.pinball

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.data.model.GameDifficulty
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 落入獎勵槽並永久堆疊留存之鋼珠資料模型
 */
data class SettledBall(
    val slotIndex: Int,
    val stackOrder: Int,
    val x: Float,
    val y: Float
)

/**
 * 夜市珠霸王專屬物理演算引擎
 * 封裝鋼珠生命週期狀態機、通道發射、頂弧導引、釘陣碰撞、斜坡滾動重力與入槽判定。
 * 完全解耦 UI，支援無 Compose 環境之純單元測試與全軌跡步進模擬。
 */
class NightMarketPhysicsEngine(
    val virtualWidth: Float = 340f,
    val virtualHeight: Float = 560f,
    val difficulty: GameDifficulty = GameDifficulty.BEGINNER,
    customAlleyWidth: Float? = null,
    val onPinHit: ((pin: NightMarketPin, bounceImpulse: Float) -> Unit)? = null,
    val onSlotLanded: ((slot: RewardSlot) -> Unit)? = null,
    val onPaddleHit: ((center: Offset) -> Unit)? = null,
    val onGateHit: (() -> Unit)? = null
) {
    // 依據難度決定發射道寬度、鋼珠半徑與槽數 (幾個槽 = 總共有幾顆彈珠)
    val alleyWidth: Float = customAlleyWidth ?: when (difficulty) {
        GameDifficulty.BEGINNER -> 28f
        GameDifficulty.INTERMEDIATE -> 25f
        GameDifficulty.ADVANCED -> 22f
        GameDifficulty.HARD, GameDifficulty.HELL, GameDifficulty.EPIC -> 18f
    }

    val ballRadius: Float = when (difficulty) {
        GameDifficulty.BEGINNER -> 11.0f
        GameDifficulty.INTERMEDIATE -> 9.5f
        GameDifficulty.ADVANCED -> 8.2f
        GameDifficulty.HARD, GameDifficulty.HELL, GameDifficulty.EPIC -> 6.8f
    }

    val slotCount: Int = when (difficulty) {
        GameDifficulty.BEGINNER -> 6
        GameDifficulty.INTERMEDIATE -> 8
        GameDifficulty.ADVANCED -> 10
        GameDifficulty.HARD, GameDifficulty.HELL, GameDifficulty.EPIC -> 13
    }

    val mainBoardWidth: Float = virtualWidth - alleyWidth

    val ball: NightMarketBall = NightMarketBall(
        x = virtualWidth - alleyWidth / 2f,
        y = virtualHeight - 60f,
        vx = 0f,
        vy = 0f,
        radius = ballRadius,
        state = NightMarketBallState.WAITING_LAUNCH
    )

    val rewardSlots: MutableList<RewardSlot> = mutableListOf()
    val pins: MutableList<NightMarketPin> = mutableListOf()
    val settledBalls: MutableList<SettledBall> = mutableListOf()

    var spinningPaddle: SpinningPaddle? = null
    var pegSplitter: PegSplitter? = null
    var swingingGate: SwingingGate? = null

    // 防卡釘無限迴圈監測
    private var consecutivePinHits = 0
    private var lastPinY = 0f

    init {
        initializeSlots()
        initializePins()
        initializeHazards()
    }

    private fun initializeSlots() {
        rewardSlots.clear()
        val slotW = mainBoardWidth / slotCount

        // 依難度設定各槽點數分佈 (中央為特獎，兩側常態分佈)
        val points = when (slotCount) {
            6 -> listOf(10, 20, 30, 50, 30, 20)
            8 -> listOf(10, 20, 30, 50, 50, 30, 20, 10)
            10 -> listOf(10, 20, 30, 40, 60, 60, 40, 30, 20, 10)
            else -> listOf(10, 15, 20, 30, 40, 60, 100, 60, 40, 30, 20, 15, 10)
        }

        val palette = listOf(
            Color(0xFF5C6BC0),
            Color(0xFF42A5F5),
            Color(0xFF26A69A),
            Color(0xFFFFB300),
            Color(0xFFFF7043),
            Color(0xFFAB47BC),
            Color(0xFFEF5350)
        )

        for (i in 0 until slotCount) {
            val pts = points.getOrElse(i) { 20 }
            val isMaxPrize = pts == points.maxOrNull()
            rewardSlots.add(
                RewardSlot(
                    id = i,
                    minX = i * slotW,
                    maxX = (i + 1) * slotW,
                    label = "${pts}分",
                    points = pts,
                    sausages = if (isMaxPrize) 1 else 0,
                    isFreeBall = false,
                    color = if (isMaxPrize) Color(0xFFFFB300) else palette[i % palette.size]
                )
            )
        }
    }

    private fun initializePins() {
        pins.clear()
        val (rows, pinRadius, baseCols) = when (difficulty) {
            GameDifficulty.BEGINNER -> Triple(7, 6.0f, 6)
            GameDifficulty.INTERMEDIATE -> Triple(9, 5.0f, 8)
            GameDifficulty.ADVANCED -> Triple(11, 4.2f, 10)
            GameDifficulty.HARD, GameDifficulty.HELL, GameDifficulty.EPIC -> Triple(14, 3.5f, 13)
        }

        val startY = 125f
        // endY 由 110f 調整至 140f：確保最後一排釘子 (釘半徑+球半徑最大=6+11=17f) 遠離槽頂
        // 槽口頂部在 virtualHeight-95f，修正後最後一排物理底端 = 420+17 = 437f，遠低於 465f 槽頂，徹底消除擋槽 Bug
        val endY = virtualHeight - 140f
        val rowSpacing = (endY - startY) / (rows - 1)

        for (r in 0 until rows) {
            val isEven = (r % 2 == 0)
            val cols = if (isEven) baseCols else baseCols + 1
            val colSpacing = (mainBoardWidth - 32f) / (cols - 1)
            val offsetX = if (isEven) 16f else 16f + colSpacing * 0.5f

            for (c in 0 until cols) {
                val px = offsetX + c * colSpacing
                val py = startY + r * rowSpacing
                if (px in (12f + pinRadius)..(mainBoardWidth - 12f - pinRadius)) {
                    pins.add(NightMarketPin(x = px, y = py, radius = pinRadius))
                }
            }
        }
    }

    private fun initializeHazards() {
        spinningPaddle = if (difficulty == GameDifficulty.BEGINNER) {
            SpinningPaddle(center = Offset(mainBoardWidth * 0.50f, 240f), radius = 22f)
        } else null

        pegSplitter = if (difficulty == GameDifficulty.INTERMEDIATE) {
            PegSplitter(
                topVertex = Offset(mainBoardWidth * 0.50f, 215f),
                leftVertex = Offset(mainBoardWidth * 0.50f - 24f, 245f),
                rightVertex = Offset(mainBoardWidth * 0.50f + 24f, 245f)
            )
        } else null

        swingingGate = if (difficulty in listOf(GameDifficulty.ADVANCED, GameDifficulty.HARD, GameDifficulty.HELL, GameDifficulty.EPIC)) {
            SwingingGate(
                pivot = Offset(mainBoardWidth * 0.50f, virtualHeight - 115f),
                length = 42f,
                speedDegPerSec = 75f
            )
        } else null
    }

    /**
     * 重設鋼珠至底座待發位置 (不清除已落槽留存的鋼珠)
     */
    fun resetBallToAlley() {
        ball.x = virtualWidth - alleyWidth / 2f
        ball.y = virtualHeight - 60f
        ball.vx = 0f
        ball.vy = 0f
        ball.radius = ballRadius
        ball.state = NightMarketBallState.WAITING_LAUNCH
        consecutivePinHits = 0
    }

    /**
     * 清除所有已落槽鋼珠 (重新整局時調用)
     */
    fun clearSettledBalls() {
        settledBalls.clear()
        consecutivePinHits = 0
    }

    /**
     * 發射鋼珠
     * @param powerFraction 蓄力比例 (0.15f ~ 1.0f)
     */
    fun launch(powerFraction: Float): Boolean {
        if (ball.state != NightMarketBallState.WAITING_LAUNCH) return false
        val power = powerFraction.coerceIn(0.2f, 1.0f)
        // 彈射初速直接與玩家拉桿蓄力深度連動 (-600f ~ -1250f)
        val launchVy = -600f - power * 650f
        ball.vy = launchVy
        ball.vx = 0f
        ball.state = NightMarketBallState.LAUNCHING_ALLEY
        consecutivePinHits = 0
        return true
    }

    /**
     * 單幀物理積分與碰撞檢測
     * @param dt 時間步長 (秒)
     */
    fun update(dt: Float) {
        val clampedDt = dt.coerceIn(0.001f, 0.033f)

        // 機關物理更新
        updateHazards(clampedDt)

        if (ball.state == NightMarketBallState.WAITING_LAUNCH || ball.state == NightMarketBallState.SETTLED) {
            return
        }

        // Sub-stepping 細分防止穿牆與精確碰撞
        val subSteps = 3
        val subDt = clampedDt / subSteps

        for (step in 0 until subSteps) {
            stepPhysics(subDt)
            if (ball.state == NightMarketBallState.SETTLED) break
        }
    }

    private fun updateHazards(dt: Float) {
        spinningPaddle?.let { pad ->
            pad.angleDeg = (pad.angleDeg + pad.angularVelocity * dt) % 360f
            pad.angularVelocity *= (1f - 0.8f * dt)
        }

        swingingGate?.let { gate ->
            gate.currentAngleDeg += gate.direction * gate.speedDegPerSec * dt
            if (gate.currentAngleDeg > gate.maxAngleDeg) {
                gate.currentAngleDeg = gate.maxAngleDeg
                gate.direction = -1f
            } else if (gate.currentAngleDeg < gate.minAngleDeg) {
                gate.currentAngleDeg = gate.minAngleDeg
                gate.direction = 1f
            }
        }
    }

    private fun stepPhysics(subDt: Float) {
        // 重力與空氣阻力 (真實厚重鋼珠手感)
        if (ball.state == NightMarketBallState.IN_PINS) {
            ball.vy += 480f * subDt
            ball.vx *= (1f - 0.06f * subDt)
            ball.vy *= (1f - 0.03f * subDt)
        } else if (ball.state == NightMarketBallState.TOP_ARCH) {
            ball.vy += 320f * subDt
        }

        ball.x += ball.vx * subDt
        ball.y += ball.vy * subDt

        when (ball.state) {
            NightMarketBallState.LAUNCHING_ALLEY -> {
                // 發射道內約束直上，受重力自然減速
                ball.x = virtualWidth - alleyWidth / 2f
                ball.vx = 0f
                ball.vy += 450f * subDt

                if (ball.y <= 55f) {
                    ball.state = NightMarketBallState.TOP_ARCH
                    val spd = Math.abs(ball.vy)
                    ball.vx = -spd * 0.90f
                    ball.vy = -spd * 0.22f
                } else if (ball.vy > 0f && ball.y >= virtualHeight - 60f) {
                    // 若蓄力極弱在通道內掉回，安全停靠底座，不扣球數
                    resetBallToAlley()
                }
            }

            NightMarketBallState.TOP_ARCH -> {
                // 頂部圓弧引導轉向左側
                if (ball.y - ball.radius < 16f) {
                    ball.y = 16f + ball.radius
                    if (ball.vy < 0f) ball.vy = 0f
                }
                if (ball.x + ball.radius > virtualWidth - 6f) {
                    ball.x = virtualWidth - 6f - ball.radius
                    if (ball.vx > 0f) ball.vx = 0f
                }
                ball.vy += 320f * subDt
                ball.vx *= (1f - 0.03f * subDt)

                // 蓄力太弱未能過頂，回落通道
                if (ball.vx > -40f && ball.x > virtualWidth - alleyWidth - 4f && ball.vy > 0f) {
                    ball.state = NightMarketBallState.LAUNCHING_ALLEY
                }

                // 跨過右側隔板頂端進入釘陣主場地 (保留向左動能，絕不強加向下墜速，滿蓄力滑向左側，中蓄力中路，輕蓄力右路)
                if (ball.x < mainBoardWidth - 4f) {
                    ball.state = NightMarketBallState.IN_PINS
                    if (ball.vy < 0f) ball.vy = 0f
                }
            }

            NightMarketBallState.IN_PINS -> {
                // 1. 外框與天花板導流約束 (天花板平滑導向最左側，絕無向下倒勾)
                if (ball.y - ball.radius < 16f) {
                    ball.y = 16f + ball.radius
                    if (ball.vy < 0f) ball.vy = 0f
                    ball.vx *= (1f - 0.04f * subDt)
                }
                if (ball.x - ball.radius < 10f) {
                    ball.x = 10f + ball.radius
                    ball.vx = Math.abs(ball.vx) * 0.55f
                }
                if (ball.x + ball.radius > mainBoardWidth && ball.y > 55f) {
                    ball.x = mainBoardWidth - ball.radius
                    ball.vx = -Math.abs(ball.vx) * 0.55f
                }

                // 2. 黃銅釘陣碰撞檢測 (鋼珠沉著彈跳，防卡釘無限死循環)
                for (pin in pins) {
                    val dx = ball.x - pin.x
                    val dy = ball.y - pin.y
                    val dist = sqrt(dx * dx + dy * dy)
                    val minDist = pin.radius + ball.radius
                    if (dist < minDist && dist > 0.001f) {
                        val nx = dx / dist
                        val ny = dy / dist
                        ball.x = pin.x + nx * (minDist + 0.6f)
                        ball.y = pin.y + ny * (minDist + 0.6f)

                        val vDotN = ball.vx * nx + ball.vy * ny
                        if (vDotN < 0f) {
                            val rest = 0.38f // 鋼珠重量感碰撞，彈跳不高且動能迅速被吸收
                            val jitter = (Math.random().toFloat() - 0.5f) * 16f
                            ball.vx = (ball.vx - (1f + rest) * vDotN * nx) + jitter
                            ball.vy = (ball.vy - (1f + rest) * vDotN * ny)

                            val curSpd = sqrt(ball.vx * ball.vx + ball.vy * ball.vy)
                            if (curSpd > 350f) {
                                ball.vx = (ball.vx / curSpd) * 350f
                                ball.vy = (ball.vy / curSpd) * 350f
                            }
                        }

                        // 防卡釘無限迴圈監測
                        if (Math.abs(ball.y - lastPinY) < 18f) {
                            consecutivePinHits++
                        } else {
                            consecutivePinHits = 0
                        }
                        lastPinY = ball.y

                        if (consecutivePinHits >= 3) {
                            // 連續 3 次撞釘且卡在相同高度，強制向下推進打破無限共振
                            ball.vy = (ball.vy.coerceAtLeast(0f) + 120f).coerceAtMost(320f)
                            ball.vx += if (ball.x > mainBoardWidth / 2f) -30f else 30f
                            consecutivePinHits = 0
                        }

                        pin.hitAnim = 1f
                        onPinHit?.invoke(pin, 1f)
                    }
                }

                // 3. 機關碰撞
                spinningPaddle?.let { pad ->
                    val dx = ball.x - pad.center.x
                    val dy = ball.y - pad.center.y
                    val dist = sqrt(dx * dx + dy * dy)
                    if (dist < pad.radius + ball.radius && dist > 0.001f) {
                        val nx = dx / dist
                        val ny = dy / dist
                        ball.x = pad.center.x + nx * (pad.radius + ball.radius)
                        ball.vx = nx * 180f
                        ball.vy = ny * 180f
                        pad.angularVelocity = 600f
                        onPaddleHit?.invoke(pad.center)
                    }
                }

                swingingGate?.let { gate ->
                    val rad = gate.currentAngleDeg * (PI / 180f)
                    val tipX = gate.pivot.x + cos(rad).toFloat() * gate.length
                    val tipY = gate.pivot.y + sin(rad).toFloat() * gate.length
                    val segX = tipX - gate.pivot.x
                    val segY = tipY - gate.pivot.y
                    val segLenSq = segX * segX + segY * segY
                    if (segLenSq > 0f) {
                        val u = ((ball.x - gate.pivot.x) * segX + (ball.y - gate.pivot.y) * segY) / segLenSq
                        val clampedU = u.coerceIn(0f, 1f)
                        val closestX = gate.pivot.x + clampedU * segX
                        val closestY = gate.pivot.y + clampedU * segY
                        val dx = ball.x - closestX
                        val dy = ball.y - closestY
                        val dist = sqrt(dx * dx + dy * dy)
                        if (dist < ball.radius + 6f && dist > 0.001f) {
                            val nx = dx / dist
                            val ny = dy / dist
                            ball.x = closestX + nx * (ball.radius + 6f)
                            ball.y = closestY + ny * (ball.radius + 6f)
                            ball.vx = nx * 220f
                            ball.vy = ny * 220f
                            onGateHit?.invoke()
                        }
                    }
                }

                // 4. 入槽判定（嚴格限定在 IN_PINS 狀態且到達槽頂深度）
                if (ball.y >= virtualHeight - 95f) {
                    ball.state = NightMarketBallState.SETTLED
                    val landedSlot = rewardSlots.find { ball.x >= it.minX && ball.x < it.maxX }
                        ?: rewardSlots.first()
                    landedSlot.hitAnim = 1f

                    // 計算該槽堆疊層次並永久留存
                    val stackOrder = settledBalls.count { it.slotIndex == landedSlot.id }
                    val slotCenterX = (landedSlot.minX + landedSlot.maxX) / 2f
                    // 鋼珠直徑 22f，留存堆疊坐標
                    val settledY = (virtualHeight - 16f) - stackOrder * (ball.radius * 2f + 2f)

                    settledBalls.add(
                        SettledBall(
                            slotIndex = landedSlot.id,
                            stackOrder = stackOrder,
                            x = slotCenterX,
                            y = settledY
                        )
                    )

                    onSlotLanded?.invoke(landedSlot)
                }
            }

            NightMarketBallState.SETTLED,
            NightMarketBallState.WAITING_LAUNCH -> {
                // 無動作
            }
        }
    }
}
