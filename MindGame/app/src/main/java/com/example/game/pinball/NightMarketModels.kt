package com.example.game.pinball

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

enum class NightMarketBallState {
    WAITING_LAUNCH,    // 於發射道底座待發
    LAUNCHING_ALLEY,   // 於右側走道向上發射衝刺
    TOP_ARCH,          // 頂部圓弧導軌偏折轉向
    IN_PINS,           // 於釘陣木台中連續彈跳滾動
    SETTLED            // 已落入底部獎勵槽結算完畢
}

data class NightMarketPin(
    val x: Float,
    val y: Float,
    val radius: Float = 7f,
    var hitAnim: Float = 0f
)

data class RewardSlot(
    val id: Int,
    val minX: Float,
    val maxX: Float,
    val label: String,
    val points: Int,
    val sausages: Int,
    val isFreeBall: Boolean,
    val color: Color,
    var hitAnim: Float = 0f
)

data class NightMarketBall(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var radius: Float = 11f,
    var state: NightMarketBallState = NightMarketBallState.WAITING_LAUNCH
) {
    // 相容性便利屬性
    val isLaunched: Boolean
        get() = state != NightMarketBallState.WAITING_LAUNCH

    val isSettled: Boolean
        get() = state == NightMarketBallState.SETTLED
}

// 難度專屬特別花樣與阻礙
data class SpinningPaddle(
    val center: Offset,
    val radius: Float = 22f,
    var angleDeg: Float = 0f,
    var angularVelocity: Float = 0f
)

data class PegSplitter(
    val topVertex: Offset,
    val leftVertex: Offset,
    val rightVertex: Offset,
    var hitAnim: Float = 0f
)

data class SwingingGate(
    val pivot: Offset,
    val length: Float = 48f,
    val minAngleDeg: Float = -35f,
    val maxAngleDeg: Float = 35f,
    var currentAngleDeg: Float = 0f,
    var direction: Float = 1f,
    val speedDegPerSec: Float = 60f
)
