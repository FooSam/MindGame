package com.example.game.pinball

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.data.model.GameDifficulty
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 珠過來啊！(FlipperPinball) 核心物理演算引擎
 * 採用標準剛體物理、連續子步積分、幾何約束、擋板角速度線速度轉移、發球保護與常規彈珠台黃金佈局。
 * 完全解耦 UI，支援無 Compose 環境之純單元測試與物理步進模擬。
 */
class FlipperPhysicsEngine(
    val virtualWidth: Float = 360f,
    val virtualHeight: Float = 600f,
    val alleyWidth: Float = 32f,
    val difficulty: GameDifficulty = GameDifficulty.BEGINNER,
    val onBumperHit: ((bumper: Bumper, pts: Int) -> Unit)? = null,
    val onRolloverLit: ((target: RolloverTarget, pts: Int) -> Unit)? = null,
    val onSlingshotHit: ((slingshot: Slingshot) -> Unit)? = null,
    val onFlipperHit: ((isLeft: Boolean) -> Unit)? = null,
    val onWallBounce: (() -> Unit)? = null,
    val onBallDrained: ((engine: FlipperPhysicsEngine) -> Unit)? = null,
    val onBallRealignedInAlley: (() -> Unit)? = null
) {
    val mainPlayWidth: Float = virtualWidth - alleyWidth // 328f

    // 全難度統一使用真實鋼珠物理重力 (依規範：難度改變的是機關佈局與得分差異，而非重力與材質)
    val gravityScale: Float = 1450f

    // 各難度得分倍率
    val scoreMultiplier: Float = when (difficulty) {
        GameDifficulty.BEGINNER -> 1.0f
        GameDifficulty.INTERMEDIATE -> 1.25f
        GameDifficulty.ADVANCED -> 1.5f
        GameDifficulty.HARD -> 2.0f
        GameDifficulty.HELL -> 2.5f
        GameDifficulty.EPIC -> 3.0f
    }

    // 擋板長度 (黃金比例覆蓋底部，中央保留標準 44f 落球空隙)
    val flipperLength: Float = when (difficulty) {
        GameDifficulty.BEGINNER -> 82f
        GameDifficulty.INTERMEDIATE -> 80f
        GameDifficulty.ADVANCED -> 78f
        GameDifficulty.HARD -> 76f
        GameDifficulty.HELL -> 74f
        GameDifficulty.EPIC -> 72f
    }

    val pinball: Pinball = Pinball(
        x = virtualWidth - alleyWidth / 2f,
        y = virtualHeight - 80f,
        vx = 0f,
        vy = 0f,
        radius = 12f,
        isAlive = true,
        inAlley = true
    )

    // 左右擋板 (軸心移至兩側導板出口 x=70 與 x=258)
    val leftFlipper: Flipper = Flipper(
        pivot = Offset(70f, 520f),
        length = flipperLength,
        restAngleDeg = 26f,
        activeAngleDeg = -24f,
        currentAngleDeg = 26f,
        isLeft = true
    )

    val rightFlipper: Flipper = Flipper(
        pivot = Offset(258f, 520f),
        length = flipperLength,
        restAngleDeg = 154f,
        activeAngleDeg = 204f,
        currentAngleDeg = 154f,
        isLeft = false
    )

    // 左右兩側導流斜壁 (引導球順暢滑入擋板，消除太空盲區)
    // 左斜壁: (14f, 420f) -> (68f, 515f)
    // 右斜壁: (314f, 420f) -> (260f, 515f)
    val leftGuideP1 = Offset(14f, 420f)
    val leftGuideP2 = Offset(68f, 515f)
    val rightGuideP1 = Offset(mainPlayWidth - 14f, 420f)
    val rightGuideP2 = Offset(260f, 515f)

    // 彈力機構與標靶
    val bumpers: MutableList<Bumper> = mutableListOf()
    val rollovers: MutableList<RolloverTarget> = mutableListOf()
    val slingshots: MutableList<Slingshot> = mutableListOf()

    // 難度專屬機關
    var centerPost: CenterPost? = null
    var vortex: Vortex? = null
    var movingObstacle: MovingObstacle? = null
    var magneticWell: MagneticWell? = null

    // 鋼珠生命狀態
    var hasEnteredPlayfield: Boolean = false
    var combo: Int = 0

    // 防無限迴圈反彈監測
    private var bounceCountInRegion = 0
    private var lastBounceY = 0f
    private var consecutiveSlingBounces = 0

    init {
        initializeFieldElements()
    }

    private fun initializeFieldElements() {
        bumpers.clear()
        // 經典頂部 3 個 Bumpers
        bumpers.add(Bumper(1, Offset(mainPlayWidth * 0.35f, 150f), 24f, Color(0xFFFF5252), (100 * scoreMultiplier).toInt()))
        bumpers.add(Bumper(2, Offset(mainPlayWidth * 0.65f, 150f), 24f, Color(0xFF448AFF), (100 * scoreMultiplier).toInt()))
        bumpers.add(Bumper(3, Offset(mainPlayWidth * 0.50f, 215f), 26f, Color(0xFFFFD740), (200 * scoreMultiplier).toInt()))

        // 高難度增加第 4、第 5 個 Bumper 增強障礙挑戰
        if (difficulty in listOf(GameDifficulty.HARD, GameDifficulty.HELL, GameDifficulty.EPIC)) {
            bumpers.add(Bumper(4, Offset(mainPlayWidth * 0.50f, 110f), 19f, Color(0xFFE040FB), (250 * scoreMultiplier).toInt()))
        }
        if (difficulty == GameDifficulty.EPIC) {
            bumpers.add(Bumper(5, Offset(mainPlayWidth * 0.50f, 280f), 20f, Color(0xFFFF6E40), (300 * scoreMultiplier).toInt()))
        }

        rollovers.clear()
        rollovers.add(RolloverTarget(1, Offset(mainPlayWidth * 0.28f, 65f), 24f, 14f, Color(0xFF00E676)))
        rollovers.add(RolloverTarget(2, Offset(mainPlayWidth * 0.50f, 52f), 24f, 14f, Color(0xFF00E676)))
        rollovers.add(RolloverTarget(3, Offset(mainPlayWidth * 0.72f, 65f), 24f, 14f, Color(0xFF00E676)))

        slingshots.clear()
        // 左側三角保險桿：法向量偏向右下，引導鋼珠滾向下方的 Flipper 擋板防守區，絕不向對面互射
        slingshots.add(
            Slingshot(
                p1 = Offset(26f, 380f),
                p2 = Offset(62f, 445f),
                p3 = Offset(26f, 445f),
                normal = Offset(0.89f, 0.45f)
            )
        )
        // 右側三角保險桿：法向量偏向左下，引導鋼珠滾向下方的 Flipper 擋板防守區
        slingshots.add(
            Slingshot(
                p1 = Offset(mainPlayWidth - 26f, 380f),
                p2 = Offset(mainPlayWidth - 62f, 445f),
                p3 = Offset(mainPlayWidth - 26f, 445f),
                normal = Offset(-0.89f, 0.45f)
            )
        )

        // 難度專屬機關佈置 (遵照規範：徹底拔除初級正中央下方的無敵彈回點，初級已有加長型 82f 擋板保護)
        centerPost = null

        vortex = if (difficulty in listOf(GameDifficulty.INTERMEDIATE, GameDifficulty.EPIC)) {
            Vortex(center = Offset(mainPlayWidth * 0.50f, 290f), radius = 32f)
        } else null

        movingObstacle = if (difficulty in listOf(GameDifficulty.ADVANCED, GameDifficulty.HELL, GameDifficulty.EPIC)) {
            MovingObstacle(
                x = mainPlayWidth * 0.50f,
                y = 285f,
                width = 54f,
                height = 12f,
                vx = 80f,
                minX = 65f,
                maxX = mainPlayWidth - 65f
            )
        } else null

        magneticWell = if (difficulty in listOf(GameDifficulty.HARD, GameDifficulty.HELL, GameDifficulty.EPIC)) {
            MagneticWell(center = Offset(mainPlayWidth * 0.50f, 320f), radius = 46f, strength = 1100f)
        } else null
    }

    /**
     * 重設鋼珠至發射走道
     */
    fun resetBallToAlley() {
        pinball.x = virtualWidth - alleyWidth / 2f
        pinball.y = virtualHeight - 80f
        pinball.vx = 0f
        pinball.vy = 0f
        pinball.inAlley = true
        pinball.isAlive = true
        hasEnteredPlayfield = false
    }

    /**
     * 發射鋼珠
     * @param powerFraction 蓄力比例 (0.3f ~ 1.0f)
     */
    fun launchBall(powerFraction: Float): Boolean {
        if (!pinball.inAlley || !pinball.isAlive) return false
        val clampedPower = powerFraction.coerceIn(0.25f, 1.0f)
        // 彈射初速直接與拉桿力道連動 (-1175f ~ -2075f)，提升上限使 100% 力道確實能飛到左上角
        val launchVy = -1175f - (clampedPower * 900f)
        pinball.vy = launchVy
        pinball.vx = 0f
        pinball.inAlley = false
        hasEnteredPlayfield = false
        return true
    }

    /**
     * 更新單幀物理
     * @param dt 時間間隔 (秒)
     */
    fun update(dt: Float) {
        val clampedDt = dt.coerceIn(0.001f, 0.033f)

        // 1. 擋板角速度平滑動畫與物理
        updateFlipperAngles(clampedDt)

        // 2. 機關動態
        updateHazards(clampedDt)

        // 3. 鋼珠物理 Sub-stepping
        val subSteps = 3
        val subDt = clampedDt / subSteps

        for (step in 0 until subSteps) {
            if (!pinball.inAlley && pinball.isAlive) {
                stepPinballPhysics(subDt)
            }
        }
    }

    private fun updateFlipperAngles(dt: Float) {
        val flipperSpeed = 900f // 度/秒
        // 左擋板
        val leftTarget = if (leftFlipper.isPressed) leftFlipper.activeAngleDeg else leftFlipper.restAngleDeg
        val leftDiff = leftTarget - leftFlipper.currentAngleDeg
        if (Math.abs(leftDiff) > 0.5f) {
            val step = Math.signum(leftDiff) * flipperSpeed * dt
            leftFlipper.angularVelocity = step / dt
            leftFlipper.currentAngleDeg = if (Math.abs(step) >= Math.abs(leftDiff)) leftTarget else leftFlipper.currentAngleDeg + step
        } else {
            leftFlipper.currentAngleDeg = leftTarget
            leftFlipper.angularVelocity = 0f
        }

        // 右擋板
        val rightTarget = if (rightFlipper.isPressed) rightFlipper.activeAngleDeg else rightFlipper.restAngleDeg
        val rightDiff = rightTarget - rightFlipper.currentAngleDeg
        if (Math.abs(rightDiff) > 0.5f) {
            val step = Math.signum(rightDiff) * flipperSpeed * dt
            rightFlipper.angularVelocity = step / dt
            rightFlipper.currentAngleDeg = if (Math.abs(step) >= Math.abs(rightDiff)) rightTarget else rightFlipper.currentAngleDeg + step
        } else {
            rightFlipper.currentAngleDeg = rightTarget
            rightFlipper.angularVelocity = 0f
        }
    }

    private fun updateHazards(dt: Float) {
        movingObstacle?.let { mob ->
            mob.x += mob.vx * dt
            if (mob.x < mob.minX) {
                mob.x = mob.minX
                mob.vx = Math.abs(mob.vx)
            } else if (mob.x > mob.maxX) {
                mob.x = mob.maxX
                mob.vx = -Math.abs(mob.vx)
            }
            if (mob.hitAnim > 0f) mob.hitAnim = (mob.hitAnim - dt * 3f).coerceAtLeast(0f)
        }

        vortex?.let { vox ->
            vox.rotationAngle = (vox.rotationAngle + dt * 180f) % 360f
        }

        magneticWell?.let { mag ->
            mag.pulseAnim = (mag.pulseAnim + dt * 2.5f) % (2f * PI.toFloat())
        }
    }

    private fun stepPinballPhysics(subDt: Float) {
        // 重力：所有狀態皆施加
        pinball.vy += gravityScale * subDt

        // 阻尼：頂弧為光滑金屬導軌，阻力極小，只在「已進入場地」後才對水平速度施加阻尼
        // 此設計確保不同蓄力進入場地時保留速度差異（滿力快、輕力慢），符合物理且讓測試可靠
        if (hasEnteredPlayfield) {
            pinball.vx *= (1f - 0.12f * subDt)
            pinball.vy *= (1f - 0.12f * subDt)
        } else {
            // 未進場地前（發射道 + 頂弧）：只施加少量阻尼在 vy（空氣阻力），vx 保持蓄力差異
            pinball.vy *= (1f - 0.04f * subDt)
        }

        // 離開 Slingshot 區域時自動重設橫跳計數器
        if (pinball.y < 340f || pinball.y > 470f) {
            consecutiveSlingBounces = 0
        }


        // 磁力與漩渦
        if (hasEnteredPlayfield) {
            magneticWell?.let { mag ->
                val dx = mag.center.x - pinball.x
                val dy = mag.center.y - pinball.y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist < mag.radius && dist > 1f) {
                    val pull = (1f - dist / mag.radius) * mag.strength * subDt
                    pinball.vx += (dx / dist) * pull
                    pinball.vy += (dy / dist) * pull
                }
            }

            vortex?.let { vox ->
                val dx = pinball.x - vox.center.x
                val dy = pinball.y - vox.center.y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist < vox.radius && dist > 1f) {
                    val tangentX = -dy / dist
                    val tangentY = dx / dist
                    pinball.vx += tangentX * 320f * subDt
                    pinball.vy += tangentY * 320f * subDt
                }
            }
        }

        // 位置積分
        pinball.x += pinball.vx * subDt
        pinball.y += pinball.vy * subDt

        // 發射走道與頂弧通道導流
        if (!hasEnteredPlayfield) {
            if (pinball.y > 65f && pinball.vx >= 0f) {
                pinball.x = virtualWidth - alleyWidth / 2f
                pinball.vx = 0f
                // 發球保護：若動能耗盡下落至底座，視為重發，絕不扣球數
                if (pinball.vy > 0f && pinball.y >= virtualHeight - 75f) {
                    resetBallToAlley()
                    onBallRealignedInAlley?.invoke()
                    return
                }
            } else {
                // 初入頂弧時將向上動能一次性轉化為向左水平動量 (大小直接與發射蓄力連動)
                // 使用 0.46f 轉換係數（而非 0.85f），確保輕蓄力 (~-1490f) → -685f，
                // 滿蓄力 (~-2075f) → -954f，兩者明顯不同且不被截斷
                // 上限放寬至 -1100f，保留各蓄力比例間的差異（連續可控）
                if (pinball.vx >= -100f) {
                    val entryUpSpeed = Math.abs(pinball.vy).coerceAtLeast(300f)
                    pinball.vx = (-entryUpSpeed * 0.46f).coerceIn(-1100f, -220f)
                    pinball.vy = -60f
                } else {
                    pinball.vx *= (1f - 0.08f * subDt)
                    pinball.vy = (pinball.vy + gravityScale * 0.3f * subDt).coerceIn(-60f, 60f)
                }

                // 頂弧圓滑彎曲 (頂部平順天花板約束，絕無朝下折彎的倒勾)
                if (pinball.y - pinball.radius < 14f) {
                    pinball.y = 14f + pinball.radius
                    if (pinball.vy < 0f) pinball.vy = 0f
                }
                if (pinball.x + pinball.radius > virtualWidth - 8f) {
                    pinball.x = virtualWidth - 8f - pinball.radius
                    if (pinball.vx > 0f) pinball.vx = 0f
                }

                // 跨過右隔板頂端進入主場地 (保留真實向左動能，絕對不寫死初速！滿力飛向左側，中力進入中央 Bumpers，輕力順右側下滑)
                if (pinball.x < mainPlayWidth - 4f) {
                    hasEnteredPlayfield = true
                    if (pinball.vy < 0f) pinball.vy = 0f
                    onWallBounce?.invoke()
                }
            }
        } else {
            // 主場地外框碰撞約束 (右隔板阻擋逆流回發射道，真實鋼材反彈 0.52f，迅速耗散動能)
            if (pinball.x - pinball.radius < 12f) {
                pinball.x = 12f + pinball.radius
                pinball.vx = Math.abs(pinball.vx) * 0.52f
                onWallBounce?.invoke()
            }
            if (pinball.x + pinball.radius > mainPlayWidth && pinball.y > 60f) {
                pinball.x = mainPlayWidth - pinball.radius
                pinball.vx = -Math.abs(pinball.vx) * 0.52f
                onWallBounce?.invoke()
            }
            if (pinball.y - pinball.radius < 14f) {
                pinball.y = 14f + pinball.radius
                pinball.vy = Math.abs(pinball.vy) * 0.52f
                onWallBounce?.invoke()
            }

            // 左右外側導流斜壁碰撞檢測 (金屬平滑導流進擋板，動能適度吸收 0.50f)
            checkLineSegmentCollision(pinball, leftGuideP1, leftGuideP2, 0.50f)
            checkLineSegmentCollision(pinball, rightGuideP1, rightGuideP2, 0.50f)

            // Slingshots 三角保險桿碰撞
            for (sling in slingshots) {
                if (checkSlingshotCollision(pinball, sling)) {
                    onSlingshotHit?.invoke(sling)
                }
            }

            // Bumper 碰撞 (具備適度衝擊動能與物理耗散，絕不無上限超速或死循環)
            for (bumper in bumpers) {
                val dx = pinball.x - bumper.center.x
                val dy = pinball.y - bumper.center.y
                val dist = sqrt(dx * dx + dy * dy)
                val minDist = bumper.radius + pinball.radius
                if (dist < minDist && dist > 0.001f) {
                    val nx = dx / dist
                    val ny = dy / dist
                    pinball.x = bumper.center.x + nx * (minDist + 1f)
                    pinball.y = bumper.center.y + ny * (minDist + 1f)

                    val vDotN = pinball.vx * nx + pinball.vy * ny
                    val inSpeed = Math.abs(vDotN)
                    // Bumper 機電彈簧提供彈力衝量，但受終端速度限制，能量自然收斂
                    val bounceSpeed = (280f + inSpeed * 0.45f).coerceIn(240f, 440f)
                    pinball.vx = nx * bounceSpeed
                    pinball.vy = ny * bounceSpeed

                    // 防死循環阻尼：若在同一 y 高度區間連續碰撞，給予向下牽引打破共振
                    if (Math.abs(pinball.y - lastBounceY) < 40f) {
                        bounceCountInRegion++
                    } else {
                        bounceCountInRegion = 0
                    }
                    lastBounceY = pinball.y
                    if (bounceCountInRegion >= 3) {
                        pinball.vy += 140f
                        bounceCountInRegion = 0
                    }

                    bumper.hitAnim = 1f
                    combo++
                    val pts = bumper.points * (1 + combo / 3)
                    onBumperHit?.invoke(bumper, pts)
                }
            }

            // 移動障礙板
            movingObstacle?.let { mob ->
                val halfW = mob.width / 2f
                val halfH = mob.height / 2f
                if (pinball.x + pinball.radius > mob.x - halfW &&
                    pinball.x - pinball.radius < mob.x + halfW &&
                    pinball.y + pinball.radius > mob.y - halfH &&
                    pinball.y - pinball.radius < mob.y + halfH
                ) {
                    mob.hitAnim = 1f
                    pinball.vy = if (pinball.vy > 0) -Math.abs(pinball.vy) * 0.75f - 80f else Math.abs(pinball.vy) * 0.75f + 80f
                    pinball.vx += mob.vx * 0.4f
                    onWallBounce?.invoke()
                }
            }

            // 初級防漏救球柱 (圓形柱體彈性碰撞，能量自然耗散，絕無永動暴衝)
            centerPost?.let { post ->
                if (post.isActive) {
                    val dx = pinball.x - post.center.x
                    val dy = pinball.y - post.center.y
                    val dist = sqrt(dx * dx + dy * dy)
                    val minDist = post.radius + pinball.radius
                    if (dist < minDist && dist > 0.001f) {
                        val nx = dx / dist
                        val ny = dy / dist
                        pinball.x = post.center.x + nx * minDist
                        pinball.y = post.center.y + ny * minDist
                        val vDotN = pinball.vx * nx + pinball.vy * ny
                        if (vDotN < 0f) {
                            val rest = 0.45f
                            pinball.vx -= (1f + rest) * vDotN * nx
                            pinball.vy -= (1f + rest) * vDotN * ny
                            // 限制向上反彈速度上限，防垂直無限共振
                            if (pinball.vy < -280f) pinball.vy = -280f
                            post.hitAnim = 1f
                            onWallBounce?.invoke()
                        }
                    }
                }
            }

            // Rollover 標靶
            for (target in rollovers) {
                if (!target.isLit) {
                    val halfW = target.width / 2f
                    val halfH = target.height / 2f
                    if (pinball.x > target.center.x - halfW && pinball.x < target.center.x + halfW &&
                        pinball.y > target.center.y - halfH && pinball.y < target.center.y + halfH
                    ) {
                        target.isLit = true
                        onRolloverLit?.invoke(target, 250)
                    }
                }
            }

            // 雙手擋板線段物理碰撞
            checkFlipperCollision(pinball, leftFlipper)
            checkFlipperCollision(pinball, rightFlipper)

            // 漏球結算 (Drain: 只有進入場地後掉出底部才算失誤)
            if (pinball.y > virtualHeight + 20f) {
                pinball.isAlive = false
                combo = 0
                onBallDrained?.invoke(this)
            }
        }
    }

    private fun checkLineSegmentCollision(ball: Pinball, p1: Offset, p2: Offset, rest: Float) {
        val segX = p2.x - p1.x
        val segY = p2.y - p1.y
        val segLenSq = segX * segX + segY * segY
        if (segLenSq == 0f) return

        val u = ((ball.x - p1.x) * segX + (ball.y - p1.y) * segY) / segLenSq
        val clampedU = u.coerceIn(0f, 1f)
        val closestX = p1.x + clampedU * segX
        val closestY = p1.y + clampedU * segY

        val dx = ball.x - closestX
        val dy = ball.y - closestY
        val dist = sqrt(dx * dx + dy * dy)
        val minDist = ball.radius + 3f

        if (dist < minDist && dist > 0.0001f) {
            val nx = dx / dist
            val ny = dy / dist
            ball.x = closestX + nx * (minDist + 0.5f)
            ball.y = closestY + ny * (minDist + 0.5f)

            val vDotN = ball.vx * nx + ball.vy * ny
            if (vDotN < 0f) {
                ball.vx = (ball.vx - (1f + rest) * vDotN * nx)
                ball.vy = (ball.vy - (1f + rest) * vDotN * ny)
                onWallBounce?.invoke()
            }
        }
    }

    private fun checkSlingshotCollision(ball: Pinball, sling: Slingshot): Boolean {
        val segX = sling.p2.x - sling.p1.x
        val segY = sling.p2.y - sling.p1.y
        val segLenSq = segX * segX + segY * segY
        if (segLenSq == 0f) return false

        val u = ((ball.x - sling.p1.x) * segX + (ball.y - sling.p1.y) * segY) / segLenSq
        val clampedU = u.coerceIn(0f, 1f)
        val closestX = sling.p1.x + clampedU * segX
        val closestY = sling.p1.y + clampedU * segY

        val dx = ball.x - closestX
        val dy = ball.y - closestY
        val dist = sqrt(dx * dx + dy * dy)
        val minDist = ball.radius + 4f

        if (dist < minDist && dist > 0.0001f) {
            val nx = sling.normal.x
            val ny = sling.normal.y
            ball.x = closestX + nx * (minDist + 1f)
            ball.y = closestY + ny * (minDist + 1f)

            consecutiveSlingBounces++
            val inSpeed = sqrt(ball.vx * ball.vx + ball.vy * ball.vy)
            // 彈跳速度隨入射速度連動，並隨連續彈跳次數迅速衰減阻尼 (1.0x -> 0.65x -> 0.35x)
            val damping = when {
                consecutiveSlingBounces <= 1 -> 1.0f
                consecutiveSlingBounces == 2 -> 0.65f
                else -> 0.35f
            }
            val baseSpeed = (180f + inSpeed * 0.42f).coerceIn(160f, 320f) * damping
            ball.vx = nx * baseSpeed
            // 導向下方擋板防守區，連續橫跳時額外注入向下速度分量，保證順利落向擋板由玩家擊打
            val extraDownVy = (consecutiveSlingBounces - 1) * 95f
            ball.vy = ny * baseSpeed + extraDownVy

            sling.hitAnim = 1f
            return true
        }
        return false
    }

    private fun checkFlipperCollision(ball: Pinball, flipper: Flipper) {
        val rad = flipper.currentAngleDeg * (PI / 180f).toFloat()
        val tX = cos(rad)
        val tY = sin(rad)
        val tipX = flipper.pivot.x + tX * flipper.length
        val tipY = flipper.pivot.y + tY * flipper.length

        val segX = tipX - flipper.pivot.x
        val segY = tipY - flipper.pivot.y
        val segLenSq = segX * segX + segY * segY
        if (segLenSq == 0f) return

        val u = ((ball.x - flipper.pivot.x) * segX + (ball.y - flipper.pivot.y) * segY) / segLenSq
        val clampedU = u.coerceIn(0f, 1f)

        val closestX = flipper.pivot.x + clampedU * segX
        val closestY = flipper.pivot.y + clampedU * segY

        val dx = ball.x - closestX
        val dy = ball.y - closestY
        val dist = sqrt(dx * dx + dy * dy)
        val collisionThresh = ball.radius + 1.5f

        if (dist < collisionThresh && dist > 0.0001f) {
            // 朝向場地上方的打擊面法向量
            val (nUpX, nUpY) = if (flipper.isLeft) {
                // 左擋板角度 [-24, 26]，tX > 0，向上法向量為 (tY, -tX)
                Pair(tY, -tX)
            } else {
                // 右擋板角度 [154, 204]，tX < 0，向上法向量為 (-tY, tX)
                Pair(-tY, tX)
            }

            // 判斷是否為「主動向上揮擊」
            val isActivelySwinging = if (flipper.isLeft) {
                flipper.isPressed && flipper.angularVelocity < -50f
            } else {
                flipper.isPressed && flipper.angularVelocity > 50f
            }

            // 若橫板靜止且球已滾出末端 tip，球自然滑脫下落，絕不黏滯卡在端點
            if (!isActivelySwinging && u >= 1.0f) {
                return
            }

            val pushNx = if (u in 0f..1f) nUpX else dx / dist
            val pushNy = if (u in 0f..1f) nUpY else dy / dist

            // 將鋼珠推出擋板表面，避免穿透
            ball.x = closestX + pushNx * collisionThresh
            ball.y = closestY + pushNy * collisionThresh

            // 擋板擊球成功，重設連續橫跳計數器
            consecutiveSlingBounces = 0

            if (isActivelySwinging) {
                // 【主動揮擊】：擋板向上高速旋轉，將角速度轉化為擊球點線速度
                // 接觸點位置 (clampedU: 0=根部, 1=末端) 決定力臂長度與出射偏角方向
                val armDist = clampedU * flipper.length
                val rotRadPerSec = Math.abs(flipper.angularVelocity) * (PI / 180f).toFloat()
                val paddleLinearSpeed = rotRadPerSec * armDist
                // 移除 360f 最小值限制，讓接觸時機真實影響力道 (輕刷=低速, 正中揮擊=高速)
                val swingSpeed = (paddleLinearSpeed * 1.15f).coerceIn(180f, 1050f)

                // 接觸點越靠末端 (tipInfluence→1)，出射角越偏向橫板切線方向 (增加角度多樣性)
                val tipInfluence = clampedU
                val vT = ball.vx * tX + ball.vy * tY
                // 真實彈珠台物理：末端打擊偏角更大，根部打擊偏角較小，完全連續可控
                val lateralBoost = tipInfluence * swingSpeed * 0.30f
                // 微量隨機擾動模擬橡膠接觸面不確定性，徹底破除固定軌跡幻覺
                val jitter = (Math.random().toFloat() - 0.5f) * 30f
                val exitVx = nUpX * swingSpeed + tX * (vT * 0.35f + lateralBoost) + jitter
                val exitVy = (nUpY * swingSpeed + tY * (vT * 0.35f)).coerceAtMost(-180f)

                ball.vx = exitVx
                ball.vy = exitVy

                onFlipperHit?.invoke(flipper.isLeft)
            } else {
                // 【被動接觸】：橫板靜止、保持按下或回落中
                // 遵循真實物理：無主動反彈衝量，反彈係數極低 (0.15f)，主要由向下重力主導順著斜板自然向下滑落
                val vN = ball.vx * nUpX + ball.vy * nUpY
                val vT = ball.vx * tX + ball.vy * tY

                if (vN < 0f) {
                    val restitution = 0.15f
                    val bounceN = (-vN * restitution).let { if (it < 25f) 0f else it }
                    val bounceT = vT * 0.96f
                    // 被動接觸亦加入微量擾動，避免靜止橫板產生完全鏡面反彈
                    val passiveJitter = (Math.random().toFloat() - 0.5f) * 12f

                    ball.vx = nUpX * bounceN + tX * bounceT + passiveJitter
                    ball.vy = nUpY * bounceN + tY * bounceT

                    if (bounceN > 40f) {
                        onFlipperHit?.invoke(flipper.isLeft)
                    }
                }
            }
        }
    }
}
