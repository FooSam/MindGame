package com.example.game.blockpuzzle

/**
 * 蜈蚣竄逃動畫狀態
 * 當橫列或直行湊滿 8 格時觸發
 */
data class CentipedeEscapeAnim(
    val id: Long = System.currentTimeMillis(),
    val isRow: Boolean, // true: 橫列消除, false: 直行消除
    val lineIndex: Int, // 0..7
    val movingPositive: Boolean, // true: 朝右/朝下竄逃, false: 朝左/朝上竄逃
    val cellColors: List<Int>, // 8 個格子的顏色索引 (0..5)
    val startTimestamp: Long = System.currentTimeMillis(),
    val durationMs: Long = 450L
)

/**
 * 十字雙向爆破特效狀態
 */
data class CrossBoomParticle(
    val row: Int,
    val col: Int,
    val startTimestamp: Long = System.currentTimeMillis(),
    val durationMs: Long = 500L
)
