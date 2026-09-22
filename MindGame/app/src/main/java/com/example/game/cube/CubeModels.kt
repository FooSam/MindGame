package com.example.game.cube

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin

/**
 * 魔方 6 個面的法向量與定義
 */
enum class CubeFace(
    val id: Int,
    val normalX: Float,
    val normalY: Float,
    val normalZ: Float,
    val labelZh: String,
    val labelEn: String
) {
    FRONT(0, 0f, 0f, 1f, "前", "FRONT"),
    BACK(1, 0f, 0f, -1f, "後", "BACK"),
    LEFT(2, -1f, 0f, 0f, "左", "LEFT"),
    RIGHT(3, 1f, 0f, 0f, "右", "RIGHT"),
    TOP(4, 0f, -1f, 0f, "上", "TOP"),
    BOTTOM(5, 0f, 1f, 0f, "下", "BOTTOM");

    companion object {
        fun fromId(id: Int): CubeFace = entries.firstOrNull { it.id == id } ?: FRONT
    }
}

/**
 * 網格座標 (0..2, 0..2)
 */
data class CellPos(val row: Int, val col: Int)

/**
 * 拼塊（多格骨牌 Polyomino）定義
 */
data class PuzzlePiece(
    val id: Int,
    val cells: List<CellPos>, // 相對於 (0,0) 的正規化座標
    val color: Color,
    val size: Int = cells.size,
    val rotation: Int = 0 // 0, 90, 180, 270
) {
    val width: Int get() = (cells.maxOfOrNull { it.col } ?: 0) + 1
    val height: Int get() = (cells.maxOfOrNull { it.row } ?: 0) + 1

    /**
     * 順時針旋轉 90 度並重設正規化座標至 (0,0)
     */
    fun rotatedClockwise(): PuzzlePiece {
        // (row, col) 順時針旋轉 90 度 -> (newRow = col, newCol = -row)
        val rotated = cells.map { CellPos(row = it.col, col = -it.row) }
        val minR = rotated.minOf { it.row }
        val minC = rotated.minOf { it.col }
        return copy(
            cells = rotated.map { CellPos(it.row - minR, it.col - minC) }
                .sortedWith(compareBy({ it.row }, { it.col })),
            rotation = (rotation + 90) % 360
        )
    }
}

/**
 * 已放置於魔方面上的拼塊資訊
 */
data class PlacedPiece(
    val pieceId: Int,
    val face: CubeFace,
    val originRow: Int,
    val originCol: Int,
    val piece: PuzzlePiece
) {
    /**
     * 取得該拼塊在該面 3x3 網格中所佔用的絕對座標列表
     */
    val occupiedCells: List<CellPos>
        get() = piece.cells.map { CellPos(originRow + it.row, originCol + it.col) }
}

/**
 * 3D 空間旋轉矩陣運算 (X 軸俯仰角 rotX, Y 軸旋轉角 rotY)
 */
fun rotatePoint(x: Float, y: Float, z: Float, rotX: Float, rotY: Float): FloatArray {
    val radX = Math.toRadians(rotX.toDouble())
    val radY = Math.toRadians(rotY.toDouble())

    // 1. 繞 Y 軸旋轉 (偏航角)
    val x1 = (x * cos(radY) + z * sin(radY)).toFloat()
    val z1 = (-x * sin(radY) + z * cos(radY)).toFloat()

    // 2. 繞 X 軸旋轉 (俯仰角)
    val y2 = (y * cos(radX) - z1 * sin(radX)).toFloat()
    val z2 = (y * sin(radX) + z1 * cos(radX)).toFloat()

    return floatArrayOf(x1, y2, z2)
}

/**
 * 等角正交投影（Isometric Orthographic Projection）
 * 旋轉時對邊保持嚴格平行，消除透視產生的梯形變形
 * @return Pair(螢幕 2D 座標, 深度 Z)
 */
fun projectPoint(
    x: Float,
    y: Float,
    z: Float,
    rotX: Float,
    rotY: Float,
    center: Offset,
    radius: Float
): Pair<Offset, Float> {
    val r = rotatePoint(x, y, z, rotX, rotY)
    val screenX = center.x + r[0] * radius
    val screenY = center.y + r[1] * radius
    return Pair(Offset(screenX, screenY), r[2])
}

/**
 * 計算指定面 (face) 的指定網格 (row, col) 在 3D 立方體空間中的 4 個頂點 (左上, 右上, 右下, 左下)
 * 立方體中心為 (0,0,0)，半徑為 1.5（範圍 -1.5 .. +1.5），每格寬高為 1.0
 */
fun getCell3DVertices(face: CubeFace, row: Int, col: Int): Array<FloatArray> {
    val r = row.toFloat()
    val c = col.toFloat()

    return when (face) {
        CubeFace.FRONT -> {
            val x0 = -1.5f + c
            val x1 = x0 + 1f
            val y0 = -1.5f + r
            val y1 = y0 + 1f
            val z = 1.5f
            arrayOf(
                floatArrayOf(x0, y0, z),
                floatArrayOf(x1, y0, z),
                floatArrayOf(x1, y1, z),
                floatArrayOf(x0, y1, z)
            )
        }
        CubeFace.BACK -> {
            // 從後方正視：x 由 +1.5 往 -1.5
            val x0 = 1.5f - c
            val x1 = x0 - 1f
            val y0 = -1.5f + r
            val y1 = y0 + 1f
            val z = -1.5f
            arrayOf(
                floatArrayOf(x0, y0, z),
                floatArrayOf(x1, y0, z),
                floatArrayOf(x1, y1, z),
                floatArrayOf(x0, y1, z)
            )
        }
        CubeFace.LEFT -> {
            // 從左方正視：z 由 -1.5 往 +1.5
            val z0 = -1.5f + c
            val z1 = z0 + 1f
            val y0 = -1.5f + r
            val y1 = y0 + 1f
            val x = -1.5f
            arrayOf(
                floatArrayOf(x, y0, z0),
                floatArrayOf(x, y0, z1),
                floatArrayOf(x, y1, z1),
                floatArrayOf(x, y1, z0)
            )
        }
        CubeFace.RIGHT -> {
            // 從右方正視：z 由 +1.5 往 -1.5
            val z0 = 1.5f - c
            val z1 = z0 - 1f
            val y0 = -1.5f + r
            val y1 = y0 + 1f
            val x = 1.5f
            arrayOf(
                floatArrayOf(x, y0, z0),
                floatArrayOf(x, y0, z1),
                floatArrayOf(x, y1, z1),
                floatArrayOf(x, y1, z0)
            )
        }
        CubeFace.TOP -> {
            // 從上方俯視：x 由 -1.5 往 +1.5，z 由 -1.5 往 +1.5
            val x0 = -1.5f + c
            val x1 = x0 + 1f
            val z0 = -1.5f + r
            val z1 = z0 + 1f
            val y = -1.5f
            arrayOf(
                floatArrayOf(x0, y, z0),
                floatArrayOf(x1, y, z0),
                floatArrayOf(x1, y, z1),
                floatArrayOf(x0, y, z1)
            )
        }
        CubeFace.BOTTOM -> {
            // 從下方仰視：x 由 -1.5 往 +1.5，z 由 +1.5 往 -1.5
            val x0 = -1.5f + c
            val x1 = x0 + 1f
            val z0 = 1.5f - r
            val z1 = z0 - 1f
            val y = 1.5f
            arrayOf(
                floatArrayOf(x0, y, z0),
                floatArrayOf(x1, y, z0),
                floatArrayOf(x1, y, z1),
                floatArrayOf(x0, y, z1)
            )
        }
    }
}

/**
 * 判定 2D 點是否落在凸四邊形內部 (利用外積 cross-product 符號一致性)
 */
fun isPointInQuad(pt: Offset, p0: Offset, p1: Offset, p2: Offset, p3: Offset): Boolean {
    fun cross(a: Offset, b: Offset, p: Offset): Float {
        return (b.x - a.x) * (p.y - a.y) - (b.y - a.y) * (p.x - a.x)
    }
    val d1 = cross(p0, p1, pt)
    val d2 = cross(p1, p2, pt)
    val d3 = cross(p2, p3, pt)
    val d4 = cross(p3, p0, pt)

    val hasNeg = (d1 < 0) || (d2 < 0) || (d3 < 0) || (d4 < 0)
    val hasPos = (d1 > 0) || (d2 > 0) || (d3 > 0) || (d4 > 0)
    return !(hasNeg && hasPos)
}

/**
 * 玻璃材質半透明色調主題
 */
enum class GlassTheme(
    val id: String,
    val nameZh: String,
    val nameEn: String,
    val baseColor: Color,
    val edgeHighlightColor: Color,
    val glowColor: Color
) {
    SKY_BLUE(
        "SKY_BLUE",
        "淡藍晶瑩",
        "Sky Mist",
        Color(0xFF38BDF8),
        Color(0xFFBAE6FD),
        Color(0x660284C7)
    ),
    ROSE_PINK(
        "ROSE_PINK",
        "淡粉櫻晶",
        "Rose Quartz",
        Color(0xFFF472B6),
        Color(0xFFFCE7F3),
        Color(0x66DB2777)
    ),
    CITRINE_YELLOW(
        "CITRINE_YELLOW",
        "淡黃琥珀",
        "Citrine Amber",
        Color(0xFFFBBF24),
        Color(0xFFFEF3C7),
        Color(0x66D97706)
    ),
    TANGERINE_ORANGE(
        "TANGERINE_ORANGE",
        "淡橘琉璃",
        "Tangerine Glow",
        Color(0xFFFB923C),
        Color(0xFFFFEDD5),
        Color(0x66EA580C)
    ),
    MINT_GREEN(
        "MINT_GREEN",
        "淡綠翡翠",
        "Emerald Mint",
        Color(0xFF34D399),
        Color(0xFFD1FAE5),
        Color(0x66059669)
    ),
    SMOKED_GRAY(
        "SMOKED_GRAY",
        "淡灰黑曜",
        "Obsidian Smoked",
        Color(0xFF94A3B8),
        Color(0xFFF1F5F9),
        Color(0x66475569)
    );

    companion object {
        fun fromId(id: String): GlassTheme = entries.firstOrNull { it.id == id } ?: SKY_BLUE
    }
}
