package com.example.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

@Composable
fun BlockPuzzleScreen(
    language: AppLanguage,
    appTheme: AppThemeStyle,
    bestScore: Int,
    onBackClick: () -> Unit,
    onSaveScore: (score: Int) -> Unit
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
            // 觸發消除！
            triggerVibration()
            combo += 1
            score += result.scoreEarned
            inspirationEnergy = (inspirationEnergy + 0.35f).coerceAtMost(1f)

            // 播放音效
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
                        movingPositive = r % 2 == 0, // 偶數列朝右、奇數列朝左竄逃
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
                        movingPositive = c % 2 == 0, // 偶數行朝下、奇數行朝上竄逃
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

            // 動畫期間先在棋盤底層清除格位
            coroutineScope.launch {
                delay(400)
                BlockPuzzleEngine.clearLines(boardSnapshot, result.fullRows, result.fullCols)
                for (r in 0 until 8) {
                    board[r] = boardSnapshot[r].clone()
                }
                activeCentipedes.removeAll(newCentipedes)
                activeCrossBooms.removeAll(newCrossBooms)

                // 檢查是否所有備選方塊放完 -> 補新方塊
                if (pieces.all { it == null }) {
                    pieces.clear()
                    pieces.addAll(BlockPuzzleEngine.generateThreePieces())
                }

                // 檢查 Game Over
                if (BlockPuzzleEngine.isGameOver(pieces.toList(), Array(8) { r -> board[r] })) {
                    delay(300)
                    showGameOverDialog = true
                    onSaveScore(score)
                }
            }
        } else {
            // 沒有消除，連擊中斷
            combo = 0
            // 檢查是否 3 個備選方塊都放完
            if (pieces.all { it == null }) {
                pieces.clear()
                pieces.addAll(BlockPuzzleEngine.generateThreePieces())
            }
            // 檢查 Game Over
            if (BlockPuzzleEngine.isGameOver(pieces.toList(), Array(8) { r -> board[r] })) {
                coroutineScope.launch {
                    delay(350)
                    showGameOverDialog = true
                    onSaveScore(score)
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

            // 執行滿行檢測
            processEliminations()
        }
    }

    AppBackground(themeStyle = appTheme) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // 頂部資訊列 (返回、得分、歷史最高、靈光一閃能量鈕)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 返回鍵
                IconButton(onClick = {
                    SoundManager.playClick()
                    showQuitDialog = true
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                // 得分看板
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = Localization.getString("block_puzzle_score", language),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = "$score",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFE07A5F)
                        )
                    )
                }

                // 歷史最佳紀錄
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFFF2CC8F),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = Localization.getString("block_puzzle_best", language),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                    Text(
                        text = "${maxOf(bestScore, score)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // 「靈光一閃」轉向按鈕
                val isEnergyFull = inspirationEnergy >= 1f
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            if (isEnergyFull) Color(0xFFE07A5F).copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable(enabled = isEnergyFull) {
                            if (isEnergyFull) {
                                SoundManager.playClick()
                                isRotateMode = !isRotateMode
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { inspirationEnergy },
                        modifier = Modifier.fillMaxSize(),
                        color = Color(0xFFE07A5F),
                        trackColor = Color.Transparent,
                        strokeWidth = 3.dp
                    )
                    Icon(
                        imageVector = if (isRotateMode) Icons.Default.Refresh else Icons.Default.Lightbulb,
                        contentDescription = "Rotate Skill",
                        tint = if (isEnergyFull) Color(0xFFE07A5F) else Color.Gray,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

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
                        color = Color(0xFFE07A5F)
                    ) {
                        Text(
                            text = "💡 " + Localization.getString("block_puzzle_rotate_hint", language),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                } else if (combo > 1) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF81B29A)
                    ) {
                        Text(
                            text = "🔥 ${Localization.getString("block_puzzle_combo", language)} x$combo !",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // 彈性間距：將 8x8 棋盤推向畫面中央
            Spacer(modifier = Modifier.weight(0.45f))

            // 中部 8x8 陶藝棋盤
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .onGloballyPositioned { coordinates ->
                        boardBoundsInRoot = coordinates.boundsInRoot()
                    }
            ) {
                val boardWidthPx = constraints.maxWidth.toFloat()
                val cellSizePx = boardWidthPx / 8f

                // 計算拖曳中的方塊對應的預覽目標座標
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

                // 無限旋轉動畫相位 (用於蜈蚣扭動與小短腿踏步)
                val infiniteTransition = rememberInfiniteTransition(label = "centipede_anim")
                val animPhase by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 6.28318f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 280, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "wiggle_phase"
                )

                // 繪製棋盤、落子、預覽、與蜈蚣竄逃
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // 1. 繪製 8x8 格子底槽 (Grid Well 凹槽陰影)
                    for (r in 0 until 8) {
                        for (c in 0 until 8) {
                            val x = c * cellSizePx
                            val y = r * cellSizePx
                            val margin = 2.5f

                            // 格子微內陰影底槽
                            drawRoundRect(
                                color = Color.Black.copy(alpha = 0.06f),
                                topLeft = Offset(x + margin, y + margin),
                                size = Size(cellSizePx - margin * 2, cellSizePx - margin * 2),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )
                            drawRoundRect(
                                color = Color.White.copy(alpha = 0.08f),
                                topLeft = Offset(x + margin + 1, y + margin + 1),
                                size = Size(cellSizePx - margin * 2 - 2, cellSizePx - margin * 2 - 2),
                                cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                            )
                        }
                    }

                    // 2. 繪製已放置在盤面上的 3D 實心陶塊
                    for (r in 0 until 8) {
                        for (c in 0 until 8) {
                            val colorIdx = board[r][c]
                            // 若該格正處於蜈蚣竄逃中，不在盤面上靜態繪製，改在上方動畫繪製
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

                    // 3. 繪製拖曳落點半透明預覽
                    if (canPlacePreview && draggedPiece != null) {
                        val clayColor = CLAY_COLORS[draggedPiece.colorIndex % CLAY_COLORS.size]
                        for ((pr, pc) in draggedPiece.coords) {
                            val r = previewTargetRow + pr
                            val c = previewTargetCol + pc
                            drawRoundRect(
                                color = clayColor.copy(alpha = 0.45f),
                                topLeft = Offset(c * cellSizePx + 2f, r * cellSizePx + 2f),
                                size = Size(cellSizePx - 4f, cellSizePx - 4f),
                                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                            )
                            // 柔白光暈框
                            drawRoundRect(
                                color = Color.White.copy(alpha = 0.7f),
                                topLeft = Offset(c * cellSizePx + 2f, r * cellSizePx + 2f),
                                size = Size(cellSizePx - 4f, cellSizePx - 4f),
                                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }

                    // 4. 繪製「呆萌蜈蚣長出小臉與小短腿」波浪竄逃動畫
                    val now = System.currentTimeMillis()
                    for (centipede in activeCentipedes) {
                        val elapsed = now - centipede.startTimestamp
                        val progress = (elapsed / centipede.durationMs.toFloat()).coerceIn(0f, 1f)

                        // 加速向邊界外衝出
                        val speedMultiplier = progress * progress * progress
                        val totalTravel = boardWidthPx * 1.5f
                        val directionSign = if (centipede.movingPositive) 1f else -1f
                        val travelOffset = speedMultiplier * totalTravel * directionSign

                        for (segIdx in 0 until 8) {
                            val segColor = CLAY_COLORS[centipede.cellColors[segIdx] % CLAY_COLORS.size]

                            // 身體正弦波波浪扭動 (Body Wiggle)
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

                            // 繪製陶偶身體單元
                            drawClaySolidCube(
                                topLeft = Offset(segX, segY),
                                cellSize = cellSizePx,
                                baseColor = segColor
                            )

                            // 每一節長出小短腿 (圓弧可愛小腳，隨腳步踏動)
                            val legStep = sin(animPhase * 2 + segIdx) * 3.dp.toPx()
                            if (centipede.isRow) {
                                // 上方與下方小腳
                                drawCircle(
                                    color = Color(0xFF6B4423),
                                    radius = 3.dp.toPx(),
                                    center = Offset(segX + cellSizePx * 0.35f + legStep, segY - 2.dp.toPx())
                                )
                                drawCircle(
                                    color = Color(0xFF6B4423),
                                    radius = 3.dp.toPx(),
                                    center = Offset(segX + cellSizePx * 0.65f - legStep, segY + cellSizePx + 2.dp.toPx())
                                )
                            } else {
                                // 左側與右側小腳
                                drawCircle(
                                    color = Color(0xFF6B4423),
                                    radius = 3.dp.toPx(),
                                    center = Offset(segX - 2.dp.toPx(), segY + cellSizePx * 0.35f + legStep)
                                )
                                drawCircle(
                                    color = Color(0xFF6B4423),
                                    radius = 3.dp.toPx(),
                                    center = Offset(segX + cellSizePx + 2.dp.toPx(), segY + cellSizePx * 0.65f - legStep)
                                )
                            }

                            // 最前頭那一節探出「大眼睛小臉與可愛小觸角」
                            val isHead = if (centipede.movingPositive) segIdx == 7 else segIdx == 0
                            if (isHead) {
                                val eyeRadius = 4.dp.toPx()
                                val pupilRadius = 2.5.dp.toPx()
                                val eyeCenterX1 = segX + cellSizePx * 0.38f
                                val eyeCenterY1 = segY + cellSizePx * 0.42f
                                val eyeCenterX2 = segX + cellSizePx * 0.68f
                                val eyeCenterY2 = segY + cellSizePx * 0.42f

                                // 眼白
                                drawCircle(color = Color.White, radius = eyeRadius, center = Offset(eyeCenterX1, eyeCenterY1))
                                drawCircle(color = Color.White, radius = eyeRadius, center = Offset(eyeCenterX2, eyeCenterY2))
                                // 黑眼珠
                                drawCircle(color = Color(0xFF1E1E1E), radius = pupilRadius, center = Offset(eyeCenterX1 + 0.5f, eyeCenterY1))
                                drawCircle(color = Color(0xFF1E1E1E), radius = pupilRadius, center = Offset(eyeCenterX2 + 0.5f, eyeCenterY2))
                                // 白色閃爍高光
                                drawCircle(color = Color.White, radius = 1.dp.toPx(), center = Offset(eyeCenterX1 - 1f, eyeCenterY1 - 1f))
                                drawCircle(color = Color.White, radius = 1.dp.toPx(), center = Offset(eyeCenterX2 - 1f, eyeCenterY2 - 1f))

                                // 可愛小觸角 (兩根微翹天線)
                                drawLine(
                                    color = Color(0xFFE07A5F),
                                    start = Offset(segX + cellSizePx * 0.35f, segY + 2.dp.toPx()),
                                    end = Offset(segX + cellSizePx * 0.25f, segY - 6.dp.toPx()),
                                    strokeWidth = 2.dp.toPx()
                                )
                                drawLine(
                                    color = Color(0xFFE07A5F),
                                    start = Offset(segX + cellSizePx * 0.65f, segY + 2.dp.toPx()),
                                    end = Offset(segX + cellSizePx * 0.75f, segY - 6.dp.toPx()),
                                    strokeWidth = 2.dp.toPx()
                                )
                            }
                        }
                    }

                    // 5. 繪製雙向十字爆破光斑 (Cross Boom)
                    for (boom in activeCrossBooms) {
                        val elapsed = now - boom.startTimestamp
                        val p = (elapsed / boom.durationMs.toFloat()).coerceIn(0f, 1f)
                        val centerX = boom.col * cellSizePx + cellSizePx / 2f
                        val centerY = boom.row * cellSizePx + cellSizePx / 2f
                        val blastRadius = (cellSizePx * 1.6f) * p

                        // 擴散光環
                        drawCircle(
                            color = Color(0xFFF2CC8F).copy(alpha = (1f - p) * 0.8f),
                            radius = blastRadius,
                            center = Offset(centerX, centerY),
                            style = Stroke(width = 4.dp.toPx())
                        )
                        // 金色陶屑飛散
                        for (angle in 0 until 8) {
                            val rad = Math.toRadians((angle * 45.0 + p * 60.0))
                            val dist = blastRadius * 0.85f
                            val px = centerX + (Math.cos(rad) * dist).toFloat()
                            val py = centerY + (Math.sin(rad) * dist).toFloat()
                            drawCircle(
                                color = Color(0xFFE07A5F).copy(alpha = 1f - p),
                                radius = (3.dp.toPx() * (1f - p)).coerceAtLeast(1f),
                                center = Offset(px, py)
                            )
                        }
                    }
                }
            }

            // 彈性間距：棋盤與備選托盤之間的舒適間距
            Spacer(modifier = Modifier.weight(0.55f))

            // 下方 3 個備選方塊展示與拖曳托盤區
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(138.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
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
                                    if (isRotateMode && piece != null) Color(0xFFE07A5F).copy(alpha = 0.18f)
                                    else Color.Transparent
                                )
                                .border(
                                    width = if (isRotateMode && piece != null) 2.dp else 0.dp,
                                    color = if (isRotateMode && piece != null) Color(0xFFE07A5F) else Color.Transparent,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .onGloballyPositioned { coordinates ->
                                    if (coordinates.isAttached) {
                                        pieceBoundsInRoot[index] = coordinates.boundsInRoot()
                                    }
                                }
                                .clickable(enabled = isRotateMode && piece != null) {
                                    // 靈光一閃：旋轉此方塊 90 度！
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
                                                // 放開手指，計算是否在合法格子落子
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
                                // 托盤上的小尺寸方塊預覽
                                MiniBlockPreview(shape = piece)
                            }
                        }
                    }
                }
            }

            // 底部安全留白
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 浮動在最上層的正在被手指拖曳的方塊 (跟隨手指 + 懸浮於手指正上方 24dp)
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
            val isNewBest = score > bestScore
            AlertDialog(
                onDismissRequest = {},
                confirmButton = {
                    Button(
                        onClick = {
                            SoundManager.playClick()
                            showGameOverDialog = false
                            // 重新開局
                            board.clear()
                            repeat(8) { board.add(IntArray(8) { 0 }) }
                            pieces.clear()
                            pieces.addAll(BlockPuzzleEngine.generateThreePieces())
                            score = 0
                            combo = 0
                            inspirationEnergy = 0f
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE07A5F)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = Localization.getString("play_again", language), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            SoundManager.playClick()
                            showGameOverDialog = false
                            onBackClick()
                        },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(text = Localization.getString("back_to_menu", language))
                    }
                },
                title = {
                    Text(
                        text = Localization.getString("block_puzzle_game_over", language),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                    )
                },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        if (isNewBest) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF2CC8F),
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                Text(
                                    text = "🏆 突破個人最佳紀錄！",
                                    color = Color(0xFF6B4423),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        Text(
                            text = Localization.getString("block_puzzle_game_over_desc", language),
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // 得分成績卡片
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = Localization.getString("block_puzzle_score", language),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "$score",
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFE07A5F)
                                    )
                                )
                            }
                        }
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )
        }

        // 中途退出確認彈窗 (Quit Confirm Dialog)
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
                            onBackClick() // 退出不計分
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
 * 繪製 3D 實心陶土方塊 (Solid Glazed Ceramic Bevel)
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

    // 1. 陶土底層立體側厚面 (3D Bevel Shadow & Slab)
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.32f),
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

    // 2. 實心主體正面 (Top Surface)
    drawRoundRect(
        color = baseColor,
        topLeft = Offset(x, y),
        size = Size(w, h - 2.5.dp.toPx()),
        cornerRadius = CornerRadius(5.5.dp.toPx(), 5.5.dp.toPx())
    )

    // 3. 左上斜向高光 (陶瓷釉面柔光 Ceramic Shine)
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
        color = Color.White.copy(alpha = 0.48f),
        style = Fill
    )

    // 4. 中心溫潤釉點
    drawCircle(
        color = Color.White.copy(alpha = 0.28f),
        radius = 2.5.dp.toPx(),
        center = Offset(x + w * 0.35f, y + h * 0.35f)
    )

    // 5. 外圍柔和細邊框
    drawRoundRect(
        color = Color.White.copy(alpha = 0.22f),
        topLeft = Offset(x, y),
        size = Size(w, h - 2.5.dp.toPx()),
        cornerRadius = CornerRadius(5.5.dp.toPx(), 5.5.dp.toPx()),
        style = Stroke(width = 1.dp.toPx())
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
