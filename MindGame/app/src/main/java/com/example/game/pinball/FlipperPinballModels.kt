package com.example.game.pinball

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

data class Pinball(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val radius: Float = 14f,
    var isAlive: Boolean = true,
    var inAlley: Boolean = true
)

data class Bumper(
    val id: Int,
    val center: Offset,
    val radius: Float,
    val color: Color,
    val points: Int,
    var hitAnim: Float = 0f
)

data class RolloverTarget(
    val id: Int,
    val center: Offset,
    val width: Float,
    val height: Float,
    val color: Color,
    var isLit: Boolean = false
)

data class Slingshot(
    val p1: Offset,
    val p2: Offset,
    val p3: Offset,
    val normal: Offset,
    var hitAnim: Float = 0f
)

data class Flipper(
    val pivot: Offset,
    val length: Float,
    val restAngleDeg: Float,
    val activeAngleDeg: Float,
    var currentAngleDeg: Float,
    val isLeft: Boolean,
    var isPressed: Boolean = false,
    var angularVelocity: Float = 0f
)

data class Particle(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    var alpha: Float = 1f,
    val size: Float = 6f
)

data class FloatingText(
    val text: String,
    val x: Float,
    var y: Float,
    val color: Color,
    var alpha: Float = 1f,
    val scale: Float = 1f
)

// 難度專屬特別花樣與阻礙
data class CenterPost(
    val center: Offset,
    val radius: Float = 12f,
    var isActive: Boolean = true,
    var hitAnim: Float = 0f
)

data class Vortex(
    val center: Offset,
    val radius: Float = 36f,
    val strength: Float = 1.2f,
    var rotationAngle: Float = 0f
)

data class MovingObstacle(
    var x: Float,
    val y: Float,
    val width: Float = 60f,
    val height: Float = 14f,
    var vx: Float = 90f,
    val minX: Float = 60f,
    val maxX: Float = 300f,
    var hitAnim: Float = 0f
)

data class MagneticWell(
    val center: Offset,
    val radius: Float = 55f,
    val strength: Float = 1200f,
    var pulseAnim: Float = 0f
)
