package com.example.game.fruit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

/**
 * 模式三：切片工坊 ASMR 核心物理、切片節奏與特調果汁引擎
 * 輸送帶推進、均勻下刀連切、金屬砧板避讓、切片掉落、果汁機旋轉攪拌與特調成果展
 */
class SlicingWorkshopEngine(
    var screenWidth: Float = 1080f,
    var screenHeight: Float = 1920f,
    private val random: Random = Random.Default
) {
    // 切刀水平固定位置 (螢幕寬度 35% 處)
    val bladeX: Float
        get() = screenWidth * 0.35f

    // 遊戲狀態機
    var state by mutableStateOf(WorkshopState.SLICING)

    // 即時 Compose 狀態，驅動 UI 每一刀即時重繪
    var score by mutableIntStateOf(0)
    var totalCuts by mutableIntStateOf(0)
    var perfectCuts by mutableIntStateOf(0)
    var comboCuts by mutableIntStateOf(0)
    var maxComboCuts by mutableIntStateOf(0)

    // 關卡進度 (目標切滿 40 片)
    val targetCutsForJuice: Int = 40
    var isJuiceReady by mutableStateOf(false)
    var isStageCompleted by mutableStateOf(false)

    // 榨汁動畫進度 (0f -> 1f，持續 1.8 秒)
    var blenderTimer by mutableFloatStateOf(0f)
    var blendingProgress by mutableFloatStateOf(0f)

    // 輸送帶速度 (像素/秒)
    var conveyorSpeed: Float = 240f

    // 輸送帶上的蔬果長條與障礙物
    val items = mutableListOf<WorkshopItem>()
    // 已切下掉落中的薄片
    val fallingPieces = mutableListOf<SlicedPiece>()
    // 浮動評價標籤
    val floatingTexts = mutableListOf<FloatingText>()

    // 下刀冷卻計時 (秒)
    private var chopCooldownTimer: Float = 0f
    private val minChopInterval: Float = 0.10f // 100ms 下刀最小保護間隔

    // 上一次下刀時間 (用於節奏均勻度評級)
    private var lastChopTimestamp: Long = 0L

    // 關卡切下的物體統計 (用於生成特調果汁)
    val cutCountsByType = mutableMapOf<WorkshopItemType, Int>()

    private var nextItemId: Long = 1L
    private var nextPieceId: Long = 1L
    private var nextTextId: Long = 1L

    /**
     * 重設/開局
     */
    fun reset() {
        state = WorkshopState.SLICING
        score = 0
        totalCuts = 0
        perfectCuts = 0
        comboCuts = 0
        maxComboCuts = 0
        isJuiceReady = false
        isStageCompleted = false
        blenderTimer = 0f
        blendingProgress = 0f
        conveyorSpeed = 240f
        items.clear()
        fallingPieces.clear()
        floatingTexts.clear()
        cutCountsByType.clear()
        chopCooldownTimer = 0f
        lastChopTimestamp = 0L

        // 生成初始輸送帶物體鏈
        spawnInitialItems()
    }

    private fun spawnInitialItems() {
        var startX = screenWidth * 0.35f
        for (i in 0 until 5) {
            val isObstacle = (i == 2 || i == 4) && random.nextFloat() < 0.35f
            val type = if (isObstacle) {
                WorkshopItemType.METAL_OBSTACLE
            } else {
                listOf(
                    WorkshopItemType.CARROT,
                    WorkshopItemType.BANANA,
                    WorkshopItemType.CUCUMBER,
                    WorkshopItemType.STRAWBERRY_ROLL,
                    WorkshopItemType.RAINBOW_JELLY
                ).random(random)
            }
            val length = if (type.isObstacle) 120f else random.nextFloat() * 180f + 240f
            items.add(
                WorkshopItem(
                    id = nextItemId++,
                    type = type,
                    x = startX,
                    totalLength = length
                )
            )
            startX += length + random.nextFloat() * 80f + 60f
        }
    }

    /**
     * 幀更新 (dt 為秒)
     */
    fun update(dt: Float) {
        if (state == WorkshopState.BLENDING) {
            blenderTimer += dt
            blendingProgress = (blenderTimer / 1.8f).coerceIn(0f, 1f)
            if (blenderTimer >= 1.8f) {
                state = WorkshopState.RESULT
                isJuiceReady = true
                isStageCompleted = true
            }

            // 更新掉落與漩渦微粒
            val pieceIterator = fallingPieces.iterator()
            while (pieceIterator.hasNext()) {
                val piece = pieceIterator.next()
                piece.vy += 600f * dt
                piece.y += piece.vy * dt
                piece.rotation += 360f * dt
                if (piece.y > screenHeight * 0.85f) {
                    pieceIterator.remove()
                }
            }
            return
        }

        if (state == WorkshopState.RESULT) {
            return
        }

        if (chopCooldownTimer > 0f) {
            chopCooldownTimer -= dt
        }

        // 1. 輸送帶向左推進
        val itemIterator = items.iterator()
        while (itemIterator.hasNext()) {
            val item = itemIterator.next()
            item.x -= conveyorSpeed * dt

            // 移出螢幕左側
            if (item.x + (item.totalLength - item.slicedLength) < -100f) {
                itemIterator.remove()
            }
        }

        // 2. 自動在右側補充新長條物體
        val rightMostX = items.maxOfOrNull { it.x + (it.totalLength - it.slicedLength) } ?: (screenWidth * 0.5f)
        if (rightMostX < screenWidth + 300f) {
            val isObstacle = random.nextFloat() < 0.28f
            val type = if (isObstacle) {
                WorkshopItemType.METAL_OBSTACLE
            } else {
                listOf(
                    WorkshopItemType.CARROT,
                    WorkshopItemType.BANANA,
                    WorkshopItemType.CUCUMBER,
                    WorkshopItemType.STRAWBERRY_ROLL,
                    WorkshopItemType.RAINBOW_JELLY
                ).random(random)
            }
            val length = if (type.isObstacle) 130f else random.nextFloat() * 200f + 220f
            val gap = random.nextFloat() * 90f + 70f
            items.add(
                WorkshopItem(
                    id = nextItemId++,
                    type = type,
                    x = rightMostX + gap,
                    totalLength = length
                )
            )
        }

        // 3. 更新掉落薄片物理 (落入果汁機槽)
        val pieceIterator = fallingPieces.iterator()
        while (pieceIterator.hasNext()) {
            val piece = pieceIterator.next()
            piece.vy += 850f * dt
            piece.y += piece.vy * dt
            piece.x -= 40f * dt
            piece.rotation += 180f * dt

            if (piece.y > screenHeight * 0.75f) {
                pieceIterator.remove()
            }
        }

        // 4. 更新浮動標籤
        val textIterator = floatingTexts.iterator()
        while (textIterator.hasNext()) {
            val text = textIterator.next()
            text.y -= 80f * dt
            text.alpha -= dt * 1.1f
            if (text.alpha <= 0f) {
                textIterator.remove()
            }
        }
    }

    /**
     * 切片下刀判定結果
     */
    data class ChopResult(
        val hitItem: Boolean,
        val isMetalClang: Boolean,
        val isPerfect: Boolean,
        val pieceColor: Color = Color.Transparent,
        val pieceSecondaryColor: Color = Color.Transparent,
        val combo: Int = 0,
        val points: Int = 0
    )

    /**
     * 玩家長按或點擊觸發下斬
     */
    fun performChop(): ChopResult {
        if (state != WorkshopState.SLICING) {
            return ChopResult(hitItem = false, isMetalClang = false, isPerfect = false)
        }
        if (chopCooldownTimer > 0f) {
            return ChopResult(hitItem = false, isMetalClang = false, isPerfect = false)
        }
        chopCooldownTimer = minChopInterval

        val now = System.currentTimeMillis()
        val intervalMs = if (lastChopTimestamp > 0L) now - lastChopTimestamp else 130L
        lastChopTimestamp = now

        // 檢查刀口 bladeX 下方是否有長條蔬果或障礙物
        val itemUnderBlade = items.find { item ->
            val start = item.x
            val end = item.x + (item.totalLength - item.slicedLength)
            bladeX in start..end
        }

        if (itemUnderBlade == null) {
            // 空切
            comboCuts = 0
            return ChopResult(hitItem = false, isMetalClang = false, isPerfect = false)
        }

        // 切中金屬障礙
        if (itemUnderBlade.type.isObstacle) {
            comboCuts = 0
            score = (score - 10).coerceAtLeast(0)
            floatingTexts.add(
                FloatingText(
                    id = nextTextId++,
                    text = "CLANG! -10",
                    x = bladeX,
                    y = screenHeight * 0.42f,
                    color = Color(0xFFEF5350)
                )
            )
            return ChopResult(hitItem = true, isMetalClang = true, isPerfect = false)
        }

        // 切中正常蔬果
        totalCuts++
        itemUnderBlade.cutCount++
        // 扣除切片厚度
        val sliceThickness = 14f
        itemUnderBlade.slicedLength += sliceThickness

        // 節奏判定：100ms ~ 220ms 視為均勻完美切片
        val isPerfect = intervalMs in 100L..230L
        if (isPerfect) {
            perfectCuts++
            comboCuts++
            maxComboCuts = maxOf(maxComboCuts, comboCuts)
        } else {
            comboCuts = 1
        }

        // 計分
        val baseScore = 15
        val bonus = if (isPerfect) 10 + (comboCuts / 5) * 5 else 0
        val earned = baseScore + bonus
        score += earned

        // 記錄統計
        cutCountsByType[itemUnderBlade.type] = (cutCountsByType[itemUnderBlade.type] ?: 0) + 1

        // 產生掉落薄片
        fallingPieces.add(
            SlicedPiece(
                id = nextPieceId++,
                color = itemUnderBlade.type.baseColor,
                secondaryColor = itemUnderBlade.type.innerColor,
                x = bladeX,
                y = screenHeight * 0.48f,
                rotation = random.nextFloat() * 45f,
                vy = random.nextFloat() * 60f + 40f
            )
        )

        // 浮動文字
        val textStr = if (isPerfect && comboCuts >= 3) {
            "PERFECT x$comboCuts! +$earned"
        } else if (isPerfect) {
            "PERFECT! +$earned"
        } else {
            "+$earned"
        }
        floatingTexts.add(
            FloatingText(
                id = nextTextId++,
                text = textStr,
                x = bladeX,
                y = screenHeight * 0.42f,
                color = if (isPerfect) Color(0xFFFFD700) else Color.White
            )
        )

        // 判斷是否達標完成關卡進度 -> 進入 1.8 秒全螢幕攪拌動畫
        if (totalCuts >= targetCutsForJuice && state == WorkshopState.SLICING) {
            state = WorkshopState.BLENDING
            blenderTimer = 0f
            blendingProgress = 0f
        }

        return ChopResult(
            hitItem = true,
            isMetalClang = false,
            isPerfect = isPerfect,
            pieceColor = itemUnderBlade.type.baseColor,
            pieceSecondaryColor = itemUnderBlade.type.innerColor,
            combo = comboCuts,
            points = earned
        )
    }

    /**
     * 生成特調客製果汁成果展資料 (含專屬名稱、成份比例、色彩漸層、風味口感與杯飾)
     */
    fun evaluateJuiceResult(): JuiceResult {
        val total = cutCountsByType.values.sum().coerceAtLeast(1)
        val sortedIngredients = cutCountsByType.entries
            .filter { !it.key.isObstacle && it.value > 0 }
            .sortedByDescending { it.value }

        val ingredientRatios = sortedIngredients.map { entry ->
            val pct = (entry.value * 100f / total).toInt()
            entry.key to pct
        }

        val primaryType = sortedIngredients.firstOrNull()?.key ?: WorkshopItemType.STRAWBERRY_ROLL
        val secondaryType = sortedIngredients.getOrNull(1)?.key

        // 根據主要與次要成份動態匹配
        data class RecipeMeta(
            val nameKey: String,
            val customTitleZh: String,
            val colors: List<Color>,
            val flavorDesc: String,
            val garnish: WorkshopItemType
        )

        val meta = when {
            primaryType == WorkshopItemType.STRAWBERRY_ROLL && secondaryType == WorkshopItemType.BANANA -> {
                RecipeMeta(
                    "juice_berry_banana",
                    "草莓芭娜娜極光奶昔",
                    listOf(Color(0xFFFF4081), Color(0xFFFF80AB), Color(0xFFFFF59D)),
                    "前調酸甜嬌豔，尾韻絲滑濃郁，香甜與莓香交織的完美之作！",
                    WorkshopItemType.STRAWBERRY_ROLL
                )
            }
            primaryType == WorkshopItemType.BANANA && secondaryType == WorkshopItemType.STRAWBERRY_ROLL -> {
                RecipeMeta(
                    "juice_berry_banana",
                    "香蕉草莓絲滑歐蕾",
                    listOf(Color(0xFFFFD54F), Color(0xFFFFF59D), Color(0xFFFF4081)),
                    "香蕉的綿密與草莓的微酸相得益彰，入口即化的夢幻口感！",
                    WorkshopItemType.BANANA
                )
            }
            primaryType == WorkshopItemType.CUCUMBER -> {
                RecipeMeta(
                    "juice_emerald_breeze",
                    "盛夏翡翠清涼特調",
                    listOf(Color(0xFF00E676), Color(0xFF69F0AE), Color(0xFFE8F5E9)),
                    "清冽爽口，解暑生津，帶有甘甜回韻的自然田園純粹之美！",
                    WorkshopItemType.CUCUMBER
                )
            }
            primaryType == WorkshopItemType.CARROT -> {
                RecipeMeta(
                    "juice_sunset_glow",
                    "晨曦暮光朝氣雙蔬汁",
                    listOf(Color(0xFFFF5722), Color(0xFFFF9800), Color(0xFFFFE082)),
                    "富含豐富維他命與天然胡蘿蔔素，清脆香甜，元氣活力滿點！",
                    WorkshopItemType.CARROT
                )
            }
            primaryType == WorkshopItemType.RAINBOW_JELLY -> {
                RecipeMeta(
                    "juice_galaxy_dream",
                    "銀河夢境彩虹冰沙",
                    listOf(Color(0xFF7C4DFF), Color(0xFFE040FB), Color(0xFF80D8FF)),
                    "炫彩魔幻風味，入口微甜氣泡感，視覺與味覺的雙重饗宴！",
                    WorkshopItemType.RAINBOW_JELLY
                )
            }
            primaryType == WorkshopItemType.BANANA -> {
                RecipeMeta(
                    "juice_golden_banana",
                    "黃金甘露純蕉果昔",
                    listOf(Color(0xFFFFD600), Color(0xFFFFEE58), Color(0xFFFFF9C4)),
                    "濃郁甜郁，溫和順喉，滿滿熱帶香氣撫平一整天的疲憊！",
                    WorkshopItemType.BANANA
                )
            }
            else -> {
                RecipeMeta(
                    "juice_tropical_fusion",
                    "主廚特調百果交響曲",
                    listOf(Color(0xFFFF1744), Color(0xFFFFD600), Color(0xFF00E676)),
                    "多種果香完美融合，甜酸平衡，極致舒壓解渴！",
                    WorkshopItemType.STRAWBERRY_ROLL
                )
            }
        }

        // 評級判定
        val perfectRatio = perfectCuts.toFloat() / totalCuts.coerceAtLeast(1)
        val grade = when {
            perfectRatio >= 0.7f -> "S"
            perfectRatio >= 0.45f -> "A"
            else -> "B"
        }

        val bonus = when (grade) {
            "S" -> 300
            "A" -> 150
            else -> 80
        }
        score += bonus

        val feedbackKey = when (grade) {
            "S" -> "juice_feedback_s"
            "A" -> "juice_feedback_a"
            else -> "juice_feedback_b"
        }

        return JuiceResult(
            juiceNameKey = meta.nameKey,
            customTitleZh = meta.customTitleZh,
            gradientColors = meta.colors,
            grade = grade,
            bonusScore = bonus,
            feedbackKey = feedbackKey,
            flavorDescriptionZh = meta.flavorDesc,
            ingredientRatios = ingredientRatios,
            garnishType = meta.garnish
        )
    }
}
