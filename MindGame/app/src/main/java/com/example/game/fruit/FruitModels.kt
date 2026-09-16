package com.example.game.fruit

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

/**
 * 水果切切樂三大遊戲模式
 */
enum class FruitGameMode(val key: String) {
    HEARTBEAT_SLICER("HEARTBEAT"),     // 模式一：心跳果刃戰（60秒ECG心跳儀、無炸彈無扣命、最後15秒心臟跳動呼吸紅光）
    BLADE_AND_BOMB("BLADE_BOMB"),       // 模式二：避雷狂刀客（經典闖關、3顆心、不可漏切、搞怪炸彈考驗即時反應）
    WORKSHOP("WORKSHOP");              // 模式三：切片工坊 ASMR（輸送帶、節奏連切、金屬障礙、漸層果汁成果展）

    companion object {
        fun fromKey(key: String): FruitGameMode {
            return entries.find { it.key == key } ?: HEARTBEAT_SLICER
        }
    }
}

/**
 * 水果類型
 */
enum class FruitType(
    val baseScore: Int,
    val radiusDp: Float,
    val outerColor: Color,
    val innerColor: Color,
    val seedColor: Color,
    val juiceColor: Color
) {
    WATERMELON(
        baseScore = 10,
        radiusDp = 84f,
        outerColor = Color(0xFF2E7D32), // 深綠條紋外皮
        innerColor = Color(0xFFEF5350), // 鮮紅果肉
        seedColor = Color(0xFF212121),  // 黑西瓜籽
        juiceColor = Color(0xFFFF5252)
    ),
    BANANA(
        baseScore = 10,
        radiusDp = 72f,
        outerColor = Color(0xFFFFD54F), // 亮黃香蕉皮
        innerColor = Color(0xFFFFF9C4), // 奶油白香蕉果肉
        seedColor = Color(0xFF8D6E63),
        juiceColor = Color(0xFFFFEE58)
    ),
    STRAWBERRY(
        baseScore = 15,
        radiusDp = 64f,
        outerColor = Color(0xFFD32F2F), // 嬌豔草莓紅
        innerColor = Color(0xFFFFCDD2), // 粉白果心
        seedColor = Color(0xFFFFEB3B),  // 金黃小籽
        juiceColor = Color(0xFFFF1744)
    ),
    PINEAPPLE(
        baseScore = 20,
        radiusDp = 82f,
        outerColor = Color(0xFFFFA000), // 金黃鳳梨鱗皮
        innerColor = Color(0xFFFFEE58), // 亮黃鳳梨心
        seedColor = Color(0xFF388E3C),  // 頂冠深綠
        juiceColor = Color(0xFFFFD700)
    ),
    KIWI(
        baseScore = 15,
        radiusDp = 66f,
        outerColor = Color(0xFF795548), // 棕色絨毛外皮
        innerColor = Color(0xFF81C784), // 翠綠奇異果肉
        seedColor = Color(0xFF212121),  // 放射黑籽
        juiceColor = Color(0xFF66BB6A)
    ),
    DRAGON_FRUIT(
        baseScore = 25,
        radiusDp = 78f,
        outerColor = Color(0xFFC2185B), // 洋紅龍鱗
        innerColor = Color(0xFFFAFAFA), // 白芝麻火龍果肉
        seedColor = Color(0xFF212121),  // 密布芝麻點
        juiceColor = Color(0xFFE91E63)
    ),
    GOLDEN_APPLE(
        baseScore = 50,
        radiusDp = 74f,
        outerColor = Color(0xFFFFD700), // 耀眼金蘋果皮
        innerColor = Color(0xFFFFF59D), // 金黃果肉
        seedColor = Color(0xFFFFB300),  // 輝光核心
        juiceColor = Color(0xFFFFEA00)
    )
}

/**
 * 特殊水果類型
 */
enum class SpecialFruitType {
    NONE,
    FREEZE_BANANA,       // ❄️ 冰凍香蕉（全場減速 4 秒）
    RAINBOW_WATERMELON,  // 🌈 彩虹西瓜（一刀爆裂全場消除）
    FUNNY_BOMB           // 💣 搞怪炸彈（避雷模式考驗即時反應，切中引爆扣心並噴墨汁遮蔽2秒）
}

/**
 * 拋物線實體水果物件
 */
data class FruitItem(
    val id: Long,
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var rotation: Float = 0f,
    var angularSpeed: Float = 0f,
    val type: FruitType,
    val specialType: SpecialFruitType = SpecialFruitType.NONE,
    val radius: Float,
    var isSliced: Boolean = false,
    var sliceAngle: Float = 0f,
    // 切開後兩半的物理狀態
    var piece1Offset: Offset = Offset.Zero,
    var piece1Rotation: Float = 0f,
    var piece1Vel: Offset = Offset.Zero,
    var piece2Offset: Offset = Offset.Zero,
    var piece2Rotation: Float = 0f,
    var piece2Vel: Offset = Offset.Zero,
    var alpha: Float = 1f
)

/**
 * 手勢刀刃軌跡點
 */
data class SlicePoint(
    val offset: Offset,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 爆汁粒子特效
 */
data class JuiceParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    val maxRadius: Float,
    var currentRadius: Float,
    var life: Float = 1f, // 1.0 -> 0.0
    val decay: Float
)

/**
 * 浮動加分/Combo 提示文字
 */
data class FloatingText(
    val id: Long,
    val text: String,
    val x: Float,
    var y: Float,
    val color: Color,
    var alpha: Float = 1f,
    var scale: Float = 1f
)

/**
 * 切片工坊輸送帶物體種類
 */
enum class WorkshopItemType(
    val titleKey: String,
    val baseColor: Color,
    val innerColor: Color,
    val isObstacle: Boolean = false
) {
    CARROT("workshop_carrot", Color(0xFFF4511E), Color(0xFFFFB74D)),
    BANANA("workshop_banana", Color(0xFFFDD835), Color(0xFFFFF9C4)),
    CUCUMBER("workshop_cucumber", Color(0xFF2E7D32), Color(0xFFA5D6A7)),
    STRAWBERRY_ROLL("workshop_strawberry", Color(0xFFE91E63), Color(0xFFF8BBD0)),
    RAINBOW_JELLY("workshop_jelly", Color(0xFF9C27B0), Color(0xFFE1BEE7)),
    METAL_OBSTACLE("workshop_metal", Color(0xFF78909C), Color(0xFFCFD8DC), isObstacle = true) // 金屬砧板障礙
}

/**
 * 切片工坊輸送帶上的連續長條物體
 */
data class WorkshopItem(
    val id: Long,
    val type: WorkshopItemType,
    var x: Float,
    val totalLength: Float,
    var slicedLength: Float = 0f,
    var cutCount: Int = 0
)

/**
 * 掉落至果汁槽的水果片
 */
data class SlicedPiece(
    val id: Long,
    val color: Color,
    val secondaryColor: Color,
    var x: Float,
    var y: Float,
    var rotation: Float,
    var vy: Float = 8f
)

/**
 * 切片工坊遊戲狀態
 */
enum class WorkshopState {
    SLICING,     // 輸送帶連切遊玩階段
    BLENDING,    // 榨汁機旋轉攪拌動畫階段 (1.8秒)
    RESULT       // 成果展展示階段 (具備防誤觸保護)
}

/**
 * 果汁特調成果展評級資料
 */
data class JuiceResult(
    val juiceNameKey: String,
    val customTitleZh: String,
    val gradientColors: List<Color>,
    val grade: String, // S, A, B
    val bonusScore: Int,
    val feedbackKey: String,
    val flavorDescriptionZh: String,
    val ingredientRatios: List<Pair<WorkshopItemType, Int>>,
    val garnishType: WorkshopItemType
)
