package com.example.game.fruit

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * 水果切切樂 - 經典爽快切水果核心物理與遊戲引擎
 * 同時支援：
 * 1. 模式一：心跳果刃戰（Heartbeat Slicer）- 60秒計時、無炸彈無扣血、ECG心跳儀、最後15秒心臟跳動
 * 2. 模式二：避雷狂刀客（Blade & Bomb）- 3顆心、不可漏切、搞怪炸彈考驗反應
 */
class FruitSlicerEngine(
    val mode: FruitGameMode,
    var screenWidth: Float = 1080f,
    var screenHeight: Float = 1920f,
    private val random: Random = Random.Default
) {
    // 遊戲狀態
    var score: Int = 0
        private set
    var hearts: Int = if (mode == FruitGameMode.BLADE_AND_BOMB) 3 else 0
        private set
    val maxHearts: Int = 3
    var remainingSeconds: Float = if (mode == FruitGameMode.HEARTBEAT_SLICER) 60f else 0f
        private set
    var isGameOver: Boolean = false
        private set
    var totalSliced: Int = 0
        private set
    var maxCombo: Int = 0
        private set

    // 狂熱條 (0.0f ~ 1.0f)
    var feverGauge: Float = 0f
        private set
    var isFeverActive: Boolean = false
        private set
    var feverTimer: Float = 0f
        private set

    // 冰凍減速效果
    var isFrozen: Boolean = false
        private set
    var freezeTimer: Float = 0f
        private set

    // 墨水遮擋效果 (0.0f ~ 1.0f)
    var inkAlpha: Float = 0f
        private set

    // 水果清單與粒子清單
    val fruits = mutableListOf<FruitItem>()
    val particles = mutableListOf<JuiceParticle>()
    val floatingTexts = mutableListOf<FloatingText>()

    // 拋物線重力加速度 (像素/秒^2)
    private val gravity: Float = 950f

    // 水果拋射計時
    private var spawnTimer: Float = 0f
    private var nextSpawnInterval: Float = 1.2f

    // 手勢切割快照與 Combo
    private var currentStrokeSlicedCount: Int = 0
    private var strokeActive: Boolean = false
    private var currentStrokeTimestamp: Long = 0L

    private var nextItemId: Long = 1L
    private var nextTextId: Long = 1L

    // 最後 15 秒心跳危機狀態 (僅限心跳果刃戰)
    val isCriticalHeartbeat: Boolean
        get() = mode == FruitGameMode.HEARTBEAT_SLICER && remainingSeconds in 0.01f..15.0f

    // 動態心跳頻率倍率 (剩餘 15 秒時為 1.0x，剩餘 1 秒時攀升至 2.5x)
    val heartbeatSpeedMultiplier: Float
        get() {
            if (!isCriticalHeartbeat) return 1.0f
            val progress = (15f - remainingSeconds).coerceIn(0f, 15f) / 15f
            return 1.0f + progress * 1.5f // 1.0x -> 2.5x
        }

    /**
     * 重設/開局
     */
    fun reset() {
        score = 0
        hearts = if (mode == FruitGameMode.BLADE_AND_BOMB) 3 else 0
        remainingSeconds = if (mode == FruitGameMode.HEARTBEAT_SLICER) 60f else 0f
        isGameOver = false
        totalSliced = 0
        maxCombo = 0
        feverGauge = 0f
        isFeverActive = false
        feverTimer = 0f
        isFrozen = false
        freezeTimer = 0f
        inkAlpha = 0f
        fruits.clear()
        particles.clear()
        floatingTexts.clear()
        spawnTimer = 0.5f // 開局 0.5 秒後拋出第一批水果
        nextSpawnInterval = 1.0f
        currentStrokeSlicedCount = 0
        strokeActive = false
    }

    /**
     * 主物理與狀態更新 (每幀呼叫，dt 為秒)
     */
    fun update(dt: Float) {
        if (isGameOver) return

        // 冰凍減速係數
        val timeScale = if (isFrozen) 0.35f else 1.0f
        val effectiveDt = dt * timeScale

        // 1. 模式計時器更新
        if (mode == FruitGameMode.HEARTBEAT_SLICER) {
            remainingSeconds -= dt
            if (remainingSeconds <= 0f) {
                remainingSeconds = 0f
                isGameOver = true
            }
        }

        // 2. 冰凍香蕉計時
        if (isFrozen) {
            freezeTimer -= dt
            if (freezeTimer <= 0f) {
                isFrozen = false
                freezeTimer = 0f
            }
        }

        // 3. 狂熱模式計時
        if (isFeverActive) {
            feverTimer -= dt
            if (feverTimer <= 0f) {
                isFeverActive = false
                feverTimer = 0f
                feverGauge = 0f
            }
        }

        // 4. 墨水遮擋淡出
        if (inkAlpha > 0f) {
            inkAlpha = (inkAlpha - dt * 0.5f).coerceAtLeast(0f)
        }

        // 5. 水果拋射邏輯
        spawnTimer += dt
        val currentInterval = if (isFeverActive) 0.4f else nextSpawnInterval
        if (spawnTimer >= currentInterval) {
            spawnTimer = 0f
            spawnFruitWave()
            nextSpawnInterval = if (mode == FruitGameMode.HEARTBEAT_SLICER) {
                // 心跳果刃隨時間推進越來越快
                val timeFactor = (60f - remainingSeconds) / 60f
                (1.4f - timeFactor * 0.6f).coerceIn(0.6f, 1.4f)
            } else {
                random.nextFloat() * 0.6f + 0.9f
            }
        }

        // 6. 更新所有水果物理狀態
        val fruitIterator = fruits.iterator()
        while (fruitIterator.hasNext()) {
            val fruit = fruitIterator.next()

            if (!fruit.isSliced) {
                // 完整水果運動
                fruit.vy += gravity * effectiveDt
                fruit.x += fruit.vx * effectiveDt
                fruit.y += fruit.vy * effectiveDt
                fruit.rotation += fruit.angularSpeed * effectiveDt

                // 水果落下螢幕底端判定 (水果整體已超出底端)
                if (fruit.y - fruit.radius > screenHeight && fruit.vy > 0) {
                    // 避雷狂刀客模式：非炸彈水果未切掉落扣除 1 顆心！
                    if (mode == FruitGameMode.BLADE_AND_BOMB && fruit.specialType != SpecialFruitType.FUNNY_BOMB) {
                        hearts--
                        if (hearts <= 0) {
                            hearts = 0
                            isGameOver = true
                        }
                    }
                    fruitIterator.remove()
                }
            } else {
                // 已切開的水果半片旋轉散開下墜
                fruit.piece1Vel = Offset(fruit.piece1Vel.x, fruit.piece1Vel.y + gravity * 1.2f * effectiveDt)
                fruit.piece2Vel = Offset(fruit.piece2Vel.x, fruit.piece2Vel.y + gravity * 1.2f * effectiveDt)
                fruit.piece1Offset += fruit.piece1Vel * effectiveDt
                fruit.piece2Offset += fruit.piece2Vel * effectiveDt
                fruit.piece1Rotation += 180f * effectiveDt
                fruit.piece2Rotation -= 180f * effectiveDt
                fruit.alpha -= dt * 0.8f

                if (fruit.alpha <= 0f || (fruit.y + fruit.piece1Offset.y > screenHeight + 200f && fruit.y + fruit.piece2Offset.y > screenHeight + 200f)) {
                    fruitIterator.remove()
                }
            }
        }

        // 7. 更新爆汁粒子
        val particleIterator = particles.iterator()
        while (particleIterator.hasNext()) {
            val p = particleIterator.next()
            p.vy += gravity * 0.7f * dt
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.life -= dt * p.decay
            p.currentRadius = p.maxRadius * p.life.coerceIn(0f, 1f)
            if (p.life <= 0f) {
                particleIterator.remove()
            }
        }

        // 8. 更新浮動文字
        val textIterator = floatingTexts.iterator()
        while (textIterator.hasNext()) {
            val t = textIterator.next()
            t.y -= 70f * dt
            t.alpha -= dt * 0.9f
            t.scale = (t.scale + dt * 0.5f).coerceAtMost(1.3f)
            if (t.alpha <= 0f) {
                textIterator.remove()
            }
        }
    }

    /**
     * 拋射一批水果 (1 ~ 3 顆，狂熱時 3 ~ 5 顆)
     */
    private fun spawnFruitWave() {
        val count = if (isFeverActive) {
            random.nextInt(3, 6)
        } else if (mode == FruitGameMode.HEARTBEAT_SLICER && isCriticalHeartbeat) {
            random.nextInt(2, 5) // 最後15秒加速出果
        } else {
            random.nextInt(1, 4)
        }

        for (i in 0 until count) {
            spawnSingleFruit()
        }
    }

    private fun spawnSingleFruit() {
        val id = nextItemId++
        val margin = screenWidth * 0.15f
        val startX = random.nextFloat() * (screenWidth - margin * 2) + margin
        val startY = screenHeight + 40f

        // 拋物線初速度：根據螢幕高度動態計算，躍升至螢幕 16% ~ 32% (佔螢幕 68% ~ 84% 高度)
        val targetApexY = screenHeight * (random.nextFloat() * 0.16f + 0.16f)
        val jumpHeight = (startY - targetApexY).coerceAtLeast(screenHeight * 0.65f)
        val vyMag = kotlin.math.sqrt(2f * gravity * jumpHeight)
        val vy = -vyMag

        val targetCenterX = screenWidth * 0.5f + (random.nextFloat() - 0.5f) * (screenWidth * 0.5f)
        val timeToApex = vyMag / gravity
        val vx = (targetCenterX - startX) / timeToApex * (random.nextFloat() * 0.3f + 0.85f)

        // 決定水果類型
        val fruitType = if (isFeverActive) {
            // 狂熱模式大量金蘋果與高分果
            if (random.nextFloat() < 0.5f) FruitType.GOLDEN_APPLE else FruitType.entries.random(random)
        } else {
            val roll = random.nextFloat()
            when {
                roll < 0.08f -> FruitType.GOLDEN_APPLE
                roll < 0.25f -> FruitType.WATERMELON
                roll < 0.42f -> FruitType.BANANA
                roll < 0.58f -> FruitType.STRAWBERRY
                roll < 0.74f -> FruitType.PINEAPPLE
                roll < 0.88f -> FruitType.KIWI
                else -> FruitType.DRAGON_FRUIT
            }
        }

        // 決定特殊水果 (僅在非狂熱時出現)
        var specialType = SpecialFruitType.NONE
        if (!isFeverActive) {
            val specialRoll = random.nextFloat()
            if (mode == FruitGameMode.BLADE_AND_BOMB && specialRoll < 0.18f) {
                // 避雷狂刀客專屬搞怪炸彈 💣
                specialType = SpecialFruitType.FUNNY_BOMB
            } else if (specialRoll < 0.04f) {
                // 冰凍香蕉
                specialType = SpecialFruitType.FREEZE_BANANA
            } else if (specialRoll < 0.07f) {
                // 彩虹西瓜
                specialType = SpecialFruitType.RAINBOW_WATERMELON
            }
        }

        val fruit = FruitItem(
            id = id,
            x = startX,
            y = startY,
            vx = vx,
            vy = vy,
            rotation = random.nextFloat() * 360f,
            angularSpeed = (random.nextFloat() - 0.5f) * 200f,
            type = fruitType,
            specialType = specialType,
            radius = fruitType.radiusDp * 1.5f
        )
        fruits.add(fruit)
    }

    /**
     * 手勢開始 (手指按下)
     */
    fun onPointerDown() {
        strokeActive = true
        currentStrokeSlicedCount = 0
        currentStrokeTimestamp = System.currentTimeMillis()
    }

    /**
     * 手勢結束 (手指提起)
     */
    fun onPointerUp(): Int {
        val combo = currentStrokeSlicedCount
        strokeActive = false
        currentStrokeSlicedCount = 0
        return combo
    }

    /**
     * 處理手指劃過線段 (p1 -> p2)
     * 回傳是否有切中水果、是否有切中炸彈、是否觸發冰凍、是否觸發彩虹、Combo 數
     */
    data class SliceResult(
        val hitFruits: List<FruitItem> = emptyList(),
        val hitBomb: Boolean = false,
        val triggeredFreeze: Boolean = false,
        val triggeredRainbow: Boolean = false,
        val comboCount: Int = 0,
        val pointsEarned: Int = 0
    )

    fun processSlice(p1: Offset, p2: Offset): SliceResult {
        if (isGameOver) return SliceResult()

        val hitList = mutableListOf<FruitItem>()
        var hitBomb = false
        var triggeredFreeze = false
        var triggeredRainbow = false
        var pointsGained = 0

        val sliceAngle = atan2(p2.y - p1.y, p2.x - p1.x)
        // 切割垂直擴散向量
        val normalAngle = sliceAngle + Math.PI.toFloat() / 2f
        val pushDist = 300f

        for (fruit in fruits) {
            if (fruit.isSliced) continue

            // 判斷線段與圓形的交錯碰撞
            if (checkSegmentCircleCollision(p1, p2, Offset(fruit.x, fruit.y), fruit.radius)) {
                fruit.isSliced = true
                fruit.sliceAngle = sliceAngle

                // 計算兩半塊的初始分離速度與方向
                val nx = cos(normalAngle) * pushDist
                val ny = sin(normalAngle) * pushDist
                fruit.piece1Vel = Offset(fruit.vx - nx, fruit.vy - ny - 150f)
                fruit.piece2Vel = Offset(fruit.vx + nx, fruit.vy + ny - 150f)

                hitList.add(fruit)
                currentStrokeSlicedCount++
                totalSliced++

                // 特殊水果判定
                when (fruit.specialType) {
                    SpecialFruitType.FUNNY_BOMB -> {
                        hitBomb = true
                        inkAlpha = 1.0f // 噴濺黑墨水遮擋視野 2 秒
                        if (mode == FruitGameMode.BLADE_AND_BOMB) {
                            hearts--
                            if (hearts <= 0) {
                                hearts = 0
                                isGameOver = true
                            }
                        }
                        spawnBombParticles(fruit.x, fruit.y)
                    }
                    SpecialFruitType.FREEZE_BANANA -> {
                        triggeredFreeze = true
                        isFrozen = true
                        freezeTimer = 4.0f
                        spawnJuiceParticles(fruit, count = 20)
                    }
                    SpecialFruitType.RAINBOW_WATERMELON -> {
                        triggeredRainbow = true
                        spawnJuiceParticles(fruit, count = 25)
                        // 全螢幕引爆所有未切水果
                        explodeAllFruits()
                    }
                    SpecialFruitType.NONE -> {
                        spawnJuiceParticles(fruit, count = 12)
                    }
                }

                if (fruit.specialType != SpecialFruitType.FUNNY_BOMB) {
                    // 計分加成 (狂熱模式雙倍)
                    val mult = if (isFeverActive) 2 else 1
                    val pts = fruit.type.baseScore * mult
                    pointsGained += pts
                    score += pts

                    // 累積狂熱量
                    if (!isFeverActive) {
                        feverGauge = (feverGauge + 0.05f).coerceAtMost(1f)
                        if (feverGauge >= 1f) {
                            isFeverActive = true
                            feverTimer = 5.0f
                        }
                    }
                }
            }
        }

        // 連擊判定 (一刀切 3 顆以上)
        if (currentStrokeSlicedCount >= 3) {
            val comboBonus = (currentStrokeSlicedCount - 2) * 15
            pointsGained += comboBonus
            score += comboBonus
            maxCombo = max(maxCombo, currentStrokeSlicedCount)

            if (hitList.isNotEmpty()) {
                val lastHit = hitList.last()
                floatingTexts.add(
                    FloatingText(
                        id = nextTextId++,
                        text = "COMBO x$currentStrokeSlicedCount! +$comboBonus",
                        x = lastHit.x,
                        y = lastHit.y - 40f,
                        color = Color(0xFFFFD700)
                    )
                )
            }
        } else if (pointsGained > 0 && hitList.isNotEmpty()) {
            val lastHit = hitList.last()
            floatingTexts.add(
                FloatingText(
                    id = nextTextId++,
                    text = "+$pointsGained",
                    x = lastHit.x,
                    y = lastHit.y - 30f,
                    color = Color.White
                )
            )
        }

        return SliceResult(
            hitFruits = hitList,
            hitBomb = hitBomb,
            triggeredFreeze = triggeredFreeze,
            triggeredRainbow = triggeredRainbow,
            comboCount = currentStrokeSlicedCount,
            pointsEarned = pointsGained
        )
    }

    /**
     * 彩虹西瓜觸發：全場未切水果全爆消除
     */
    private fun explodeAllFruits() {
        for (fruit in fruits) {
            if (!fruit.isSliced && fruit.specialType != SpecialFruitType.FUNNY_BOMB) {
                fruit.isSliced = true
                fruit.piece1Vel = Offset(-200f, -300f)
                fruit.piece2Vel = Offset(200f, -300f)
                val pts = fruit.type.baseScore * (if (isFeverActive) 2 else 1)
                score += pts
                totalSliced++
                spawnJuiceParticles(fruit, count = 8)
            }
        }
    }

    /**
     * 線段與圓形交錯碰撞檢測
     */
    private fun checkSegmentCircleCollision(p1: Offset, p2: Offset, center: Offset, radius: Float): Boolean {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        val lengthSq = dx * dx + dy * dy

        if (lengthSq == 0f) {
            return hypot(center.x - p1.x, center.y - p1.y) <= radius
        }

        // 投影比例 t
        val t = ((center.x - p1.x) * dx + (center.y - p1.y) * dy) / lengthSq
        val clampedT = t.coerceIn(0f, 1f)

        val closestX = p1.x + clampedT * dx
        val closestY = p1.y + clampedT * dy

        val distX = center.x - closestX
        val distY = center.y - closestY
        return (distX * distX + distY * distY) <= (radius * radius)
    }

    /**
     * 產生爆汁粒子
     */
    private fun spawnJuiceParticles(fruit: FruitItem, count: Int) {
        val color = if (fruit.specialType == SpecialFruitType.RAINBOW_WATERMELON) {
            listOf(Color(0xFFFF1744), Color(0xFFFFEA00), Color(0xFF00E676), Color(0xFF00B0FF), Color(0xFFD500F9)).random(random)
        } else if (fruit.specialType == SpecialFruitType.FREEZE_BANANA) {
            Color(0xFF80D8FF)
        } else {
            fruit.type.juiceColor
        }

        for (i in 0 until count) {
            val angle = random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = random.nextFloat() * 400f + 150f
            val vx = cos(angle) * speed
            val vy = sin(angle) * speed
            val rad = random.nextFloat() * 6f + 4f
            particles.add(
                JuiceParticle(
                    x = fruit.x,
                    y = fruit.y,
                    vx = vx,
                    vy = vy,
                    color = color,
                    maxRadius = rad,
                    currentRadius = rad,
                    decay = random.nextFloat() * 1.5f + 1.2f
                )
            )
        }
    }

    /**
     * 炸彈爆炸粒子 (黑煙與金橘火花)
     */
    private fun spawnBombParticles(x: Float, y: Float) {
        for (i in 0 until 24) {
            val angle = random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = random.nextFloat() * 500f + 100f
            val color = if (i % 2 == 0) Color(0xFF212121) else Color(0xFFFF5722)
            particles.add(
                JuiceParticle(
                    x = x,
                    y = y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    color = color,
                    maxRadius = 8f,
                    currentRadius = 8f,
                    decay = 1.8f
                )
            )
        }
    }
}
