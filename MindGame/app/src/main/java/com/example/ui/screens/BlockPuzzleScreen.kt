package com.example.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundManager
import com.example.data.model.AppLanguage
import com.example.data.model.Localization
import com.example.game.blockpuzzle.BlockPuzzleEngine
import com.example.game.blockpuzzle.BlockShape
import com.example.game.blockpuzzle.CentipedeEscapeAnim
import com.example.game.blockpuzzle.CrossBoomParticle
import com.example.ui.components.AppBackground
import com.example.ui.theme.AppThemeStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.sin

// 陶藝 6 大柔和自然色盤
val CLAY_COLORS = listOf(
    Color(0xFFE07A5F), // 磚陶紅 (Terracotta Red)
    Color(0xFF81B29A), // 抹茶陶綠 (Sage Ceramic)
    Color(0xFFF2CC8F), // 釉面薑黃 (Mustard Glaze)
    Color(0xFF3D405B), // 青藍灰陶 (Slate Blue Pottery)
    Color(0xFFE76F51), // 珊瑚暖橙 (Warm Coral)
    Color(0xFF9D8189)  // 香芋紫陶 (Earthy Lilac)
)

/**
 * 《塊陶啊！》(Block Puzzle) 次世代 2024-2026 頂級手遊視覺重構
 * 導入古老機關神殿沙岩遺跡棋盤、青銅導軌護框、3D 晶亮立體釉面陶塊、
 * 百足小蜈蚣滑稽眨眼竄逃、POP! 煙塵與金幣星芒爆破、頂部石刻羊皮紙 HUD 儀表板與黃金聖甲蟲能量羅盤。
 */
@Composable
fun BlockPuzzleScreen(
    language: AppLanguage,
    appTheme: AppThemeStyle,
    bestScore: Int,
    onBackClick: () -> Unit,
    onSaveScore: (score: Int) -> Unit,
    onGameOver: () -> Unit = {},
    onGameInterrupted: () -> Unit = {}
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    // 棋盤狀態 8x8 (0 為空, 1..6 為色號)
    val board = remember { mutableStateListOf<IntArray>().apply {
        repeat(8) { add(IntArray(8) { 0 }) }
    } }

    // 備選方塊 3 個
    val pieces = remember { mutableStateListOf<BlockShape?>().apply {
        addAll(BlockPuzzleEngine.generateThreePieces())
    } }

    var score by remember { mutableIntStateOf(0) }
    var combo by remember { mutableIntStateOf(0) }
    var inspirationEnergy by remember { mutableFloatStateOf(0f) } // 0f ~ 1f
    var isRotateMode by remember { mutableStateOf(false) }

    // 竄逃蜈蚣動畫清單與十字爆破粒子清單
    val activeCentipedes = remember { mutableStateListOf<CentipedeEscapeAnim>() }
    val activeCrossBooms = remember { mutableStateListOf<CrossBoomParticle>() }

    // 拖曳狀態
    var draggedPieceIndex by remember { mutableStateOf<Int?>(null) }
    var dragPositionInRoot by remember { mutableStateOf(Offset.Zero) }
    var boardBoundsInRoot by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    val pieceBoundsInRoot = remember { mutableStateListOf<androidx.compose.ui.geometry.Rect?>().apply { repeat(3) { add(null) } } }

    // 對話框狀態
    var showQuitDialog by remember { mutableStateOf(false) }
    var showGameOverDialog by remember { mutableStateOf(false) }

    // 攔截實體返回鍵
    BackHandler {
        showQuitDialog = true
    }

    // 震動輔助
    fun triggerVibration() {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(35)
            }
        } catch (_: Exception) {}
    }

    // 檢查全盤消除與動畫流程
    fun processEliminations() {
        val boardSnapshot = Array(8) { r -> board[r].clone() }
        val result = BlockPuzzleEngine.checkEliminations(boardSnapshot, combo)

        if (result.fullRows.isNotEmpty() || result.fullCols.isNotEmpty()) {
            triggerVibration()
            combo += 1
            score += result.scoreEarned
            inspirationEnergy = (inspirationEnergy + 0.35f).coerceAtMost(1f)

            SoundManager.playCentipedeEscape()
            SoundManager.playComboChime(combo)
            if (result.crossPoints.isNotEmpty()) {
                SoundManager.playCrossBoom()
            }

            // 建立蜈蚣竄逃資料
            val newCentipedes = mutableListOf<CentipedeEscapeAnim>()
            for (r in result.fullRows) {
                val rowColors = (0 until 8).map { c -> (board[r][c] - 1).coerceAtLeast(0) }
                newCentipedes.add(
                    CentipedeEscapeAnim(
                        isRow = true,
                        lineIndex = r,
                        movingPositive = r % 2 == 0,
                        cellColors = rowColors
                    )
                )
            }
            for (c in result.fullCols) {
                val colColors = (0 until 8).map { r -> (board[r][c] - 1).coerceAtLeast(0) }
                newCentipedes.add(
                    CentipedeEscapeAnim(
                        isRow = false,
                        lineIndex = c,
                        movingPositive = c % 2 == 0,
                        cellColors = colColors
                    )
                )
            }

            // 十字交叉爆破粒子
            val newCrossBooms = result.crossPoints.map { (r, c) ->
                CrossBoomParticle(row = r, col = c)
            }

            activeCentipedes.addAll(newCentipedes)
            activeCrossBooms.addAll(newCrossBooms)

            coroutineScope.launch {
                delay(400)
                BlockPuzzleEngine.clearLines(boardSnapshot, result.fullRows, result.fullCols)
                for (r in 0 until 8) {
                    board[r] = boardSnapshot[r].clone()
                }
                activeCentipedes.removeAll(newCentipedes)
                activeCrossBooms.removeAll(newCrossBooms)

                if (pieces.all { it == null }) {
                    pieces.clear()
                    pieces.addAll(BlockPuzzleEngine.generateThreePieces())
                }

                if (BlockPuzzleEngine.isGameOver(pieces.toList(), Array(8) { r -> board[r] })) {
                    delay(300)
                    showGameOverDialog = true
                    onSaveScore(score)
                    onGameOver()
                }
            }
        } else {
            combo = 0
            if (pieces.all { it == null }) {
                pieces.clear()
                pieces.addAll(BlockPuzzleEngine.generateThreePieces())
            }
            if (BlockPuzzleEngine.isGameOver(pieces.toList(), Array(8) { r -> board[r] })) {
                coroutineScope.launch {
                    delay(350)
                    showGameOverDialog = true
                    onSaveScore(score)
                    onGameOver()
                }
            }
        }
    }

    // 落子執行
    fun handlePiecePlaced(pieceIndex: Int, startRow: Int, startCol: Int) {
        val piece = pieces[pieceIndex] ?: return
        val currentBoardArray = Array(8) { r -> board[r].clone() }

        if (BlockPuzzleEngine.canPlace(piece, startRow, startCol, currentBoardArray)) {
            BlockPuzzleEngine.placeShape(piece, startRow, startCol, currentBoardArray)
            for (r in 0 until 8) {
                board[r] = currentBoardArray[r].clone()
            }
            score += piece.cellCount * 10
            pieces[pieceIndex] = null
            triggerVibration()
            SoundManager.playClayDrop()

            processEliminations()
        }
    }

    // 聖甲蟲能量滿時脈衝光芒動畫
    val infiniteTransition = rememberInfiniteTransition(label = "relicAnim")
    val scarabPulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scarabGlow"
    )

    AppBackground(themeStyle = appTheme) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // 1. 古老冒險者石刻羊皮紙 HUD 儀表板 (Temple Adventurer HUD)
            TempleAdventurerHud(
                score = score,
                bestScore = bestScore,
                inspirationEnergy = inspirationEnergy,
                isRotateMode = isRotateMode,
                scarabPulseScale = scarabPulseScale,
                language = language,
                onBackClick = {
                    SoundManager.playClick()
                    showQuitDialog = true
                },
                onScarabClick = {
                    if (inspirationEnergy >= 1f) {
                        SoundManager.playClick()
                        isRotateMode = !isRotateMode
                    }
                }
            )

            // 連擊或靈光旋轉提示膠囊
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isRotateMode) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFE65100),
                        shadowElevation = 4.dp
                    ) {
                        Text(
                            text = "💡 " + Localization.getString("block_puzzle_rotate_hint", language),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                        )
                    }
                } else if (combo > 1) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF2E7D32),
                        shadowElevation = 4.dp
                    ) {
                        Text(
                            text = "🔥 ${Localization.getString("block_puzzle_combo", language)} x$combo !",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFFFD54F),
                                fontWeight = FontWeight.ExtraBold
                            ),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.4f))

            // 2. 中部 8x8 古老陶藝神殿遺跡棋盤 (Ancient Temple Board)
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(22.dp))
                    .shadow(12.dp, RoundedCornerShape(22.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF2D231C), Color(0xFF1F1712))
                        )
                    )
                    .border(
                        width = 2.5.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF8D6E63), Color(0xFF3E2723))
                        ),
                        shape = RoundedCornerShape(22.dp)
                    )
                    .onGloballyPositioned { coordinates ->
                        boardBoundsInRoot = coordinates.boundsInRoot()
                    }
            ) {
                val boardWidthPx = constraints.maxWidth.toFloat()
                val cellSizePx = boardWidthPx / 8f

                val draggedPiece = draggedPieceIndex?.let { pieces[it] }
                var previewTargetRow = -1
                var previewTargetCol = -1
                var canPlacePreview = false

                if (draggedPiece != null && boardBoundsInRoot != null) {
                    val bounds = boardBoundsInRoot!!
                    val draggingCellSizeDp = 28
                    val pieceHeightPx = with(density) { (draggedPiece.height * draggingCellSizeDp).dp.toPx() }
                    val hoverAboveFingerPx = with(density) { 24.dp.toPx() }

                    val (targetR, targetC) = calculateTargetCell(
                        touchPosInRoot = dragPositionInRoot,
                        shape = draggedPiece,
                        boardBounds = bounds,
                        hoverAboveFingerPx = hoverAboveFingerPx,
                        pieceHeightPx = pieceHeightPx
                    )

                    if (targetR in 0 until 8 && targetC in 0 until 8) {
                        val currentBoardArray = Array(8) { r -> board[r] }
                        if (BlockPuzzleEngine.canPlace(draggedPiece, targetR, targetC, currentBoardArray)) {
                            previewTargetRow = targetR
                            previewTargetCol = targetC
                            canPlacePreview = true
                        }
                    }
                }

                // 蜈蚣身體正弦波相位
                val animPhase by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 6.28318f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 280, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "wiggle_phase"
                )

                Canvas(modifier = Modifier.fillMaxSize()) {
                    // 2.1 繪製 8x8 神殿下沉陶槽底座 (Sunken Ceramic Socket)
                    for (r in 0 until 8) {
                        for (c in 0 until 8) {
                            val x = c * cellSizePx
                            val y = r * cellSizePx
                            val margin = 2.5f

                            // 下沉陰影外框
                            drawRoundRect(
                                color = Color.Black.copy(alpha = 0.45f),
                                topLeft = Offset(x + margin, y + margin),
                                size = Size(cellSizePx - margin * 2, cellSizePx - margin * 2),
                                cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx())
                            )
                            // 槽底暗岩色
                            drawRoundRect(
                                color = Color(0xFF140F0B),
                                topLeft = Offset(x + margin + 1f, y + margin + 1f),
                                size = Size(cellSizePx - margin * 2 - 2f, cellSizePx - margin * 2 - 2f),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )
                        }
                    }

                    // 2.2 繪製已放置的 3D 晶亮雙層釉面陶塊
                    for (r in 0 until 8) {
                        for (c in 0 until 8) {
                            val colorIdx = board[r][c]
                            val isEscaping = activeCentipedes.any {
                                if (it.isRow) it.lineIndex == r else it.lineIndex == c
                            }
                            if (colorIdx > 0 && !isEscaping) {
                                val clayColor = CLAY_COLORS[(colorIdx - 1) % CLAY_COLORS.size]
                                drawClaySolidCube(
                                    topLeft = Offset(c * cellSizePx, r * cellSizePx),
                                    cellSize = cellSizePx,
                                    baseColor = clayColor
                                )
                            }
                        }
                    }

                    // 2.3 繪製拖曳落點半透明透光投影 (Ghost Preview)
                    if (canPlacePreview && draggedPiece != null) {
                        val clayColor = CLAY_COLORS[draggedPiece.colorIndex % CLAY_COLORS.size]
                        for ((pr, pc) in draggedPiece.coords) {
                            val r = previewTargetRow + pr
                            val c = previewTargetCol + pc
                            drawRoundRect(
                                color = clayColor.copy(alpha = 0.55f),
                                topLeft = Offset(c * cellSizePx + 2f, r * cellSizePx + 2f),
                                size = Size(cellSizePx - 4f, cellSizePx - 4f),
                                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                            )
                            drawRoundRect(
                                color = Color(0xFFFFD54F).copy(alpha = 0.8f),
                                topLeft = Offset(c * cellSizePx + 2f, r * cellSizePx + 2f),
                                size = Size(cellSizePx - 4f, cellSizePx - 4f),
                                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }

                    // 2.4 繪製「3D 黏土小蜈蚣」逗趣波浪竄逃與 POP! 煙塵動畫
                    val now = System.currentTimeMillis()
                    for (centipede in activeCentipedes) {
                        val elapsed = now - centipede.startTimestamp
                        val progress = (elapsed / centipede.durationMs.toFloat()).coerceIn(0f, 1f)
                        val speedMultiplier = progress * progress * progress
                        val totalTravel = boardWidthPx * 1.5f
                        val directionSign = if (centipede.movingPositive) 1f else -1f
                        val travelOffset = speedMultiplier * totalTravel * directionSign

                        for (segIdx in 0 until 8) {
                            val segColor = CLAY_COLORS[centipede.cellColors[segIdx] % CLAY_COLORS.size]
                            val wiggleOffset = sin(animPhase + segIdx * 1.2f) * 6.dp.toPx()

                            val (segX, segY) = if (centipede.isRow) {
                                val baseX = segIdx * cellSizePx + travelOffset
                                val baseY = centipede.lineIndex * cellSizePx + wiggleOffset
                                Pair(baseX, baseY)
                            } else {
                                val baseX = centipede.lineIndex * cellSizePx + wiggleOffset
                                val baseY = segIdx * cellSizePx + travelOffset
                                Pair(baseX, baseY)
                            }

                            // 繪製陶偶單元
                            drawClaySolidCube(
                                topLeft = Offset(segX, segY),
                                cellSize = cellSizePx,
                                baseColor = segColor
                            )

                            // 踏步小短腿
                            val legStep = sin(animPhase * 2 + segIdx) * 3.dp.toPx()
                            if (centipede.isRow) {
                                drawCircle(
                                    color = Color(0xFF5D4037),
                                    radius = 3.dp.toPx(),
                                    center = Offset(segX + cellSizePx * 0.35f + legStep, segY - 2.dp.toPx())
                                )
                                drawCircle(
                                    color = Color(0xFF5D4037),
                                    radius = 3.dp.toPx(),
                                    center = Offset(segX + cellSizePx * 0.65f - legStep, segY + cellSizePx + 2.dp.toPx())
                                )
                            } else {
                                drawCircle(
                                    color = Color(0xFF5D4037),
                                    radius = 3.dp.toPx(),
                                    center = Offset(segX - 2.dp.toPx(), segY + cellSizePx * 0.35f + legStep)
                                )
                                drawCircle(
                                    color = Color(0xFF5D4037),
                                    radius = 3.dp.toPx(),
                                    center = Offset(segX + cellSizePx + 2.dp.toPx(), segY + cellSizePx * 0.65f - legStep)
                                )
                            }

                            // 前端頭部大眼睛與觸角
                            val isHead = if (centipede.movingPositive) segIdx == 7 else segIdx == 0
                            if (isHead) {
                                val eyeRadius = 4.2.dp.toPx()
                                val pupilRadius = 2.4.dp.toPx()
                                val eyeX1 = segX + cellSizePx * 0.36f
                                val eyeY1 = segY + cellSizePx * 0.42f
                                val eyeX2 = segX + cellSizePx * 0.68f
                                val eyeY2 = segY + cellSizePx * 0.42f

                                drawCircle(color = Color.White, radius = eyeRadius, center = Offset(eyeX1, eyeY1))
                                drawCircle(color = Color.White, radius = eyeRadius, center = Offset(eyeX2, eyeY2))
                                drawCircle(color = Color(0xFF1E1E1E), radius = pupilRadius, center = Offset(eyeX1 + 0.5f, eyeY1))
                                drawCircle(color = Color(0xFF1E1E1E), radius = pupilRadius, center = Offset(eyeX2 + 0.5f, eyeY2))
                                drawCircle(color = Color.White, radius = 1.2.dp.toPx(), center = Offset(eyeX1 - 1f, eyeY1 - 1f))
                                drawCircle(color = Color.White, radius = 1.2.dp.toPx(), center = Offset(eyeX2 - 1f, eyeY2 - 1f))

                                // 小觸角
                                drawLine(
                                    color = Color(0xFFFFD54F),
                                    start = Offset(segX + cellSizePx * 0.35f, segY + 2.dp.toPx()),
                                    end = Offset(segX + cellSizePx * 0.22f, segY - 6.dp.toPx()),
                                    strokeWidth = 2.2.dp.toPx()
                                )
                                drawLine(
                                    color = Color(0xFFFFD54F),
                                    start = Offset(segX + cellSizePx * 0.65f, segY + 2.dp.toPx()),
                                    end = Offset(segX + cellSizePx * 0.78f, segY - 6.dp.toPx()),
                                    strokeWidth = 2.2.dp.toPx()
                                )
                            }
                        }
                    }

                    // 2.5 繪製雙向十字爆破光斑 (Cross Boom)
                    for (boom in activeCrossBooms) {
                        val elapsed = now - boom.startTimestamp
                        val p = (elapsed / boom.durationMs.toFloat()).coerceIn(0f, 1f)
                        val centerX = boom.col * cellSizePx + cellSizePx / 2f
                        val centerY = boom.row * cellSizePx + cellSizePx / 2f
                        val blastRadius = (cellSizePx * 1.8f) * p

                        drawCircle(
                            color = Color(0xFFFFD54F).copy(alpha = (1f - p) * 0.85f),
                            radius = blastRadius,
                            center = Offset(centerX, centerY),
                            style = Stroke(width = 4.dp.toPx())
                        )
                        for (angle in 0 until 8) {
                            val rad = Math.toRadians((angle * 45.0 + p * 60.0))
                            val dist = blastRadius * 0.85f
                            val px = centerX + (Math.cos(rad) * dist).toFloat()
                            val py = centerY + (Math.sin(rad) * dist).toFloat()
                            drawCircle(
                                color = Color(0xFFFFD54F).copy(alpha = 1f - p),
                                radius = (3.5.dp.toPx() * (1f - p)).coerceAtLeast(1f),
                                center = Offset(px, py)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.5f))

            // 3. 底部 3 大備選方塊神殿托盤展台 (Relic Pedestal Tray)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(138.dp)
                    .shadow(10.dp, RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF221A14))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(
                            width = 1.5.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF5D4037), Color(0xFF2E1C14))
                            ),
                            shape = RoundedCornerShape(22.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        pieces.forEachIndexed { index, piece ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(122.dp)
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isRotateMode && piece != null) Color(0xFFE65100).copy(alpha = 0.22f)
                                        else Color(0xFF17110C)
                                    )
                                    .border(
                                        width = if (isRotateMode && piece != null) 2.dp else 1.dp,
                                        color = if (isRotateMode && piece != null) Color(0xFFFF9800) else Color(0xFF3E2723),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .onGloballyPositioned { coordinates ->
                                        if (coordinates.isAttached) {
                                            pieceBoundsInRoot[index] = coordinates.boundsInRoot()
                                        }
                                    }
                                    .clickable(enabled = isRotateMode && piece != null) {
                                        val rotated = piece!!.rotateClockwise()
                                        pieces[index] = rotated
                                        inspirationEnergy = 0f
                                        isRotateMode = false
                                        SoundManager.playRotate()
                                        triggerVibration()
                                    }
                                    .pointerInput(piece) {
                                        if (piece != null && !isRotateMode) {
                                            detectDragGestures(
                                                onDragStart = { localOffset ->
                                                    draggedPieceIndex = index
                                                    val bounds = pieceBoundsInRoot.getOrNull(index)
                                                    val startTouchInRoot = if (bounds != null) {
                                                        bounds.topLeft + localOffset
                                                    } else {
                                                        localOffset
                                                    }
                                                    dragPositionInRoot = startTouchInRoot
                                                    triggerVibration()
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragPositionInRoot += dragAmount
                                                },
                                                onDragEnd = {
                                                    val bounds = boardBoundsInRoot
                                                    if (bounds != null) {
                                                        val draggingCellSizeDp = 28
                                                        val pieceHeightPx = with(density) { (piece.height * draggingCellSizeDp).dp.toPx() }
                                                        val hoverAboveFingerPx = with(density) { 24.dp.toPx() }

                                                        val (targetR, targetC) = calculateTargetCell(
                                                            touchPosInRoot = dragPositionInRoot,
                                                            shape = piece,
                                                            boardBounds = bounds,
                                                            hoverAboveFingerPx = hoverAboveFingerPx,
                                                            pieceHeightPx = pieceHeightPx
                                                        )

                                                        if (targetR in 0 until 8 && targetC in 0 until 8) {
                                                            handlePiecePlaced(index, targetR, targetC)
                                                        }
                                                    }
                                                    draggedPieceIndex = null
                                                },
                                                onDragCancel = {
                                                    draggedPieceIndex = null
                                                }
                                            )
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (piece != null && draggedPieceIndex != index) {
                                    MiniBlockPreview(shape = piece)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // 浮動在手指上方被拖曳的方塊
        if (draggedPieceIndex != null) {
            val draggingShape = pieces[draggedPieceIndex!!]
            if (draggingShape != null) {
                val draggingCellSizeDp = 28
                val pieceWidthPx = with(density) { (draggingShape.width * draggingCellSizeDp).dp.toPx() }
                val pieceHeightPx = with(density) { (draggingShape.height * draggingCellSizeDp).dp.toPx() }
                val hoverAboveFingerPx = with(density) { 24.dp.toPx() }

                val floatLeftPx = dragPositionInRoot.x - pieceWidthPx / 2f
                val floatTopPx = dragPositionInRoot.y - pieceHeightPx - hoverAboveFingerPx

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = floatLeftPx.roundToInt(),
                                y = floatTopPx.roundToInt()
                            )
                        }
                ) {
                    MiniBlockPreview(shape = draggingShape, cellSizeDp = draggingCellSizeDp)
                }
            }
        }

        // 結束遊戲結算對話框 (Game Over Dialog)
        if (showGameOverDialog) {
            TempleGameOverDialog(
                score = score,
                bestScore = bestScore,
                language = language,
                onPlayAgainClick = {
                    SoundManager.playClick()
                    showGameOverDialog = false
                    board.clear()
                    repeat(8) { board.add(IntArray(8) { 0 }) }
                    pieces.clear()
                    pieces.addAll(BlockPuzzleEngine.generateThreePieces())
                    score = 0
                    combo = 0
                    inspirationEnergy = 0f
                },
                onBackClick = {
                    SoundManager.playClick()
                    showGameOverDialog = false
                    onBackClick()
                }
            )
        }

        // 中途退出確認彈窗
        if (showQuitDialog) {
            AlertDialog(
                onDismissRequest = { showQuitDialog = false },
                title = {
                    Text(
                        text = Localization.getString("block_puzzle_quit_confirm_title", language),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                text = {
                    Text(text = Localization.getString("block_puzzle_quit_confirm_desc", language))
                },
                confirmButton = {
                    Button(
                        onClick = {
                            SoundManager.playClick()
                            showQuitDialog = false
                            val hasStarted = score > 0 || board.any { row -> row.any { it != 0 } }
                            if (hasStarted && !showGameOverDialog) {
                                onGameInterrupted()
                            }
                            onBackClick()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = Localization.getString("confirm", language))
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            SoundManager.playClick()
                            showQuitDialog = false
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = Localization.getString("cancel", language))
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

// -------------------------------------------------------------------------------------
// 1. 古老冒險者石刻羊皮紙 HUD 儀表板 (Temple Adventurer HUD)
// -------------------------------------------------------------------------------------
@Composable
private fun TempleAdventurerHud(
    score: Int,
    bestScore: Int,
    inspirationEnergy: Float,
    isRotateMode: Boolean,
    scarabPulseScale: Float,
    language: AppLanguage,
    onBackClick: () -> Unit,
    onScarabClick: () -> Unit
) {
    val isEnergyFull = inspirationEnergy >= 1f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF2A1E17)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 2.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF8D6E63), Color(0xFF3E2723))
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 返回按鈕
            Surface(
                shape = CircleShape,
                color = Color(0xFF1D140F),
                modifier = Modifier
                    .size(40.dp)
                    .border(1.dp, Color(0xFF5D4037), CircleShape)
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFFFFD54F)
                    )
                }
            }

            // 得分大字
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = Localization.getString("block_puzzle_score", language),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD7CCC8),
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    text = "$score",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFFFD54F),
                        fontSize = 24.sp
                    )
                )
            }

            // 歷史最高紀錄
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color(0xFFFFB74D),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = Localization.getString("block_puzzle_best", language),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFBCAAA4),
                            fontSize = 10.sp
                        )
                    )
                }
                Text(
                    text = "${maxOf(bestScore, score)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFFFE082)
                    )
                )
            }

            // 神聖黃金聖甲蟲羅盤按鈕 (Scarab Relic Button)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isEnergyFull) Color(0xFFFFD54F).copy(alpha = 0.25f) else Color(0xFF1D140F))
                    .border(
                        1.5.dp,
                        if (isEnergyFull) Color(0xFFFFD54F) else Color(0xFF5D4037),
                        CircleShape
                    )
                    .clickable(enabled = isEnergyFull, onClick = onScarabClick),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { inspirationEnergy },
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFFFD54F),
                    trackColor = Color.Transparent,
                    strokeWidth = 3.dp
                )
                Icon(
                    imageVector = if (isRotateMode) Icons.Default.Refresh else Icons.Default.Star,
                    contentDescription = "Rotate Skill",
                    tint = if (isEnergyFull) Color(0xFFFFD54F) else Color(0xFF8D6E63),
                    modifier = Modifier
                        .size(22.dp)
                        .scale(if (isEnergyFull) scarabPulseScale else 1f)
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// 結束遊戲結算對話框 (Game Over Dialog)
// -------------------------------------------------------------------------------------
@Composable
private fun TempleGameOverDialog(
    score: Int,
    bestScore: Int,
    language: AppLanguage,
    onPlayAgainClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val isNewBest = score > bestScore
    val stars = when {
        score >= 2500 -> 3
        score >= 1200 -> 2
        else -> 1
    }

    AlertDialog(
        onDismissRequest = {},
        confirmButton = {
            Button(
                onClick = onPlayAgainClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = Localization.getString("play_again", language), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onBackClick,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(text = Localization.getString("back_to_menu", language), color = Color(0xFFD7CCC8))
            }
        },
        title = {
            Text(
                text = Localization.getString("block_puzzle_game_over", language),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFFF7ED)
                )
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                // 星級成就列
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..3) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (i <= stars) Color(0xFFFFD54F) else Color(0xFF5D4037),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (isNewBest) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFFD54F),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = "🏆 突破個人最佳紀錄！",
                            color = Color(0xFF3E2723),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }

                Text(
                    text = Localization.getString("block_puzzle_game_over_desc", language),
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFBCAAA4))
                )
                Spacer(modifier = Modifier.height(14.dp))

                // 得分成績卡片
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1D140F),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF5D4037), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = Localization.getString("block_puzzle_score", language),
                            style = MaterialTheme.typography.labelMedium.copy(color = Color(0xFFD7CCC8))
                        )
                        Text(
                            text = "$score",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFFFD54F)
                            )
                        )
                    }
                }
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color(0xFF261912)
    )
}

/**
 * 計算拖曳懸浮方塊在盤面上的對應目標列與欄
 */
private fun calculateTargetCell(
    touchPosInRoot: Offset,
    shape: BlockShape,
    boardBounds: androidx.compose.ui.geometry.Rect,
    hoverAboveFingerPx: Float,
    pieceHeightPx: Float
): Pair<Int, Int> {
    val hoverCenterX = touchPosInRoot.x
    val hoverCenterY = touchPosInRoot.y - pieceHeightPx / 2f - hoverAboveFingerPx

    val cellSizePx = boardBounds.width / 8f
    val pieceCenterInBoardX = hoverCenterX - boardBounds.left
    val pieceCenterInBoardY = hoverCenterY - boardBounds.top

    val shapeCenterColOffsetPx = (shape.width * cellSizePx) / 2f
    val shapeCenterRowOffsetPx = (shape.height * cellSizePx) / 2f

    val shapeTopLeftInBoardX = pieceCenterInBoardX - shapeCenterColOffsetPx
    val shapeTopLeftInBoardY = pieceCenterInBoardY - shapeCenterRowOffsetPx

    val targetC = ((shapeTopLeftInBoardX + cellSizePx / 2f) / cellSizePx).toInt()
    val targetR = ((shapeTopLeftInBoardY + cellSizePx / 2f) / cellSizePx).toInt()

    return Pair(targetR, targetC)
}

/**
 * 繪製 3D 晶亮雙層釉面陶塊 (Glazed Porcelain Bevel Block)
 */
fun DrawScope.drawClaySolidCube(
    topLeft: Offset,
    cellSize: Float,
    baseColor: Color
) {
    val margin = 2.5f
    val w = cellSize - margin * 2
    val h = cellSize - margin * 2
    val x = topLeft.x + margin
    val y = topLeft.y + margin
    val cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())

    // 1. 底層立體厚度與陰影
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.42f),
        topLeft = Offset(x, y + 3.5.dp.toPx()),
        size = Size(w, h),
        cornerRadius = cornerRadius
    )
    drawRoundRect(
        color = baseColor.copy(alpha = 0.72f),
        topLeft = Offset(x, y + 2.dp.toPx()),
        size = Size(w, h),
        cornerRadius = cornerRadius
    )

    // 2. 實心主體正面
    drawRoundRect(
        color = baseColor,
        topLeft = Offset(x, y),
        size = Size(w, h - 2.5.dp.toPx()),
        cornerRadius = CornerRadius(5.5.dp.toPx(), 5.5.dp.toPx())
    )

    // 3. 上緣晶透微高光線條 (Glaze Specular Sheen)
    val highlightPath = Path().apply {
        moveTo(x + 2.5.dp.toPx(), y + h * 0.7f)
        lineTo(x + 2.5.dp.toPx(), y + 4.dp.toPx())
        lineTo(x + w * 0.7f, y + 4.dp.toPx())
        lineTo(x + w * 0.5f, y + 7.5.dp.toPx())
        lineTo(x + 6.dp.toPx(), y + 7.5.dp.toPx())
        lineTo(x + 6.dp.toPx(), y + h * 0.5f)
        close()
    }
    drawPath(
        path = highlightPath,
        color = Color.White.copy(alpha = 0.55f),
        style = Fill
    )

    // 4. 中心溫潤釉點
    drawCircle(
        color = Color.White.copy(alpha = 0.35f),
        radius = 2.5.dp.toPx(),
        center = Offset(x + w * 0.35f, y + h * 0.35f)
    )

    // 5. 外圍微倒角細邊框
    drawRoundRect(
        color = Color.White.copy(alpha = 0.28f),
        topLeft = Offset(x, y),
        size = Size(w, h - 2.5.dp.toPx()),
        cornerRadius = CornerRadius(5.5.dp.toPx(), 5.5.dp.toPx()),
        style = Stroke(width = 1.2.dp.toPx())
    )
}

/**
 * 備選方塊迷你縮圖展示元件
 */
@Composable
fun MiniBlockPreview(
    shape: BlockShape,
    cellSizeDp: Int = 20
) {
    val clayColor = CLAY_COLORS[shape.colorIndex % CLAY_COLORS.size]
    val density = LocalDensity.current
    val cellSizePx = with(density) { cellSizeDp.dp.toPx() }

    Box(
        modifier = Modifier.size(
            width = (shape.width * cellSizeDp).dp,
            height = (shape.height * cellSizeDp).dp
        )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            for ((r, c) in shape.coords) {
                drawClaySolidCube(
                    topLeft = Offset(c * cellSizePx, r * cellSizePx),
                    cellSize = cellSizePx,
                    baseColor = clayColor
                )
            }
        }
    }
}
