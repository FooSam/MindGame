package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundManager
import com.example.data.model.AppLanguage
import com.example.data.model.GameDifficulty
import com.example.data.model.LaserBoardState
import com.example.data.model.LaserDirection
import com.example.data.model.LaserLevelConfig
import com.example.data.model.LaserPiece
import com.example.data.model.Localization
import com.example.data.model.OpticalPieceType
import com.example.game.lasermaze.LaserMazeGenerator
import com.example.game.lasermaze.LaserRaycaster
import com.example.ui.components.AppBackground
import com.example.ui.theme.AppThemeStyle
import com.example.ui.theme.LaserMazePalette
import com.example.ui.theme.LaserMazeThemePalettes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * 粒子實體 (通關時超載爆炸動效)
 */
private data class MazeParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var alpha: Float,
    val color: Color,
    val radius: Float
)

/**
 * 《雷射迷宮》(Laser Maze) 主畫面
 */
@Composable
fun LaserMazeScreen(
    difficulty: GameDifficulty,
    language: AppLanguage,
    appTheme: AppThemeStyle,
    onBackClick: () -> Unit,
    onLeaderboardClick: () -> Unit,
    onGameComplete: (timeMs: Long) -> Unit,
    onGameInterrupted: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val palette = remember(appTheme) { LaserMazeThemePalettes.get(appTheme) }

    // 觸覺微震動輔助函式
    fun triggerHaptic(durationMs: Long = 30L) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    // 關卡進度 (1..10)
    var currentLevel by remember { mutableIntStateOf(1) }

    // 任務簡報與光學指引 Dialog 開關
    var showBriefingDialog by remember { mutableStateOf(true) }

    // 當前盤面狀態
    var boardState by remember {
        mutableStateOf(
            LaserMazeGenerator.generate(difficulty, currentLevel).let { initial ->
                var state = initial
                var retries = 0
                while ((state.isCleared || state.grid.flatten().none { it != null && !it.isFixed && (it.type == OpticalPieceType.MIRROR_SINGLE || it.type == OpticalPieceType.MIRROR_DOUBLE || it.type == OpticalPieceType.BEAM_SPLITTER) }) && retries < 10) {
                    retries++
                    state = LaserMazeGenerator.generate(difficulty, currentLevel)
                }
                state
            }
        )
    }

    // 各格子旋轉角度平滑動畫 (row_col -> Animatable)
    val rotationAnimatables = remember { mutableStateMapOf<String, Animatable<Float, *>>() }

    // 計時器與統計
    var elapsedTimeMs by remember { mutableLongStateOf(0L) }
    var isTimerRunning by remember { mutableStateOf(false) } // 關閉簡報後才正式開始
    var moveCount by remember { mutableIntStateOf(0) }
    var isCleared by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    // 粒子系統 (全盤超載時觸發)
    val particles = remember { mutableStateListOf<MazeParticle>() }

    // 大地圖平移視角偏移 (Pan Offset)
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    // 3D 微傾透視角度 (限制變化不超過 30 度)
    var tiltPitch by remember { mutableFloatStateOf(18f) } // X軸俯仰角: 0f (正俯視) ~ 30f (微傾極限)，預設 18f
    var tiltYaw by remember { mutableFloatStateOf(-5f) }   // Y軸偏航角: -20f ~ 20f，預設 -5f
    var isAngleAdjustMode by remember { mutableStateOf(false) } // 大棋盤時是否處於單指旋轉視角模式

    // 一鍵回正視角
    fun resetViewAngle() {
        tiltPitch = 18f
        tiltYaw = -5f
    }

    // 切換微傾與正俯視
    fun toggleTiltView() {
        if (tiltPitch > 4f) {
            tiltPitch = 0f
            tiltYaw = 0f
        } else {
            tiltPitch = 18f
            tiltYaw = -5f
        }
    }

    // 重設或載入新關卡
    fun loadLevel(level: Int, showBriefing: Boolean = true) {
        currentLevel = level.coerceIn(1, 10)
        var newState = LaserMazeGenerator.generate(difficulty, currentLevel)
        var retries = 0
        while ((newState.isCleared || newState.grid.flatten().none { it != null && !it.isFixed && (it.type == OpticalPieceType.MIRROR_SINGLE || it.type == OpticalPieceType.MIRROR_DOUBLE || it.type == OpticalPieceType.BEAM_SPLITTER) }) && retries < 10) {
            retries++
            newState = LaserMazeGenerator.generate(difficulty, currentLevel)
        }
        boardState = newState
        rotationAnimatables.clear()
        elapsedTimeMs = 0L
        isTimerRunning = !showBriefing
        showBriefingDialog = showBriefing
        moveCount = 0
        isCleared = false
        showClearDialog = false
        particles.clear()
        panOffsetX = 0f
        panOffsetY = 0f
    }

    // 監聽難度變化重新載入
    LaunchedEffect(difficulty) {
        loadLevel(1, showBriefing = true)
    }

    // 計時器循環
    LaunchedEffect(isTimerRunning) {
        while (isTimerRunning) {
            delay(50)
            elapsedTimeMs += 50
        }
    }

    // 粒子物理更新迴圈
    LaunchedEffect(isCleared) {
        if (isCleared) {
            while (particles.isNotEmpty()) {
                delay(16)
                val it = particles.iterator()
                while (it.hasNext()) {
                    val p = it.next()
                    p.x += p.vx
                    p.y += p.vy
                    p.alpha -= 0.02f
                    if (p.alpha <= 0f) {
                        it.remove()
                    }
                }
            }
        }
    }

    // 返回鍵攔截
    BackHandler {
        if (!isCleared) {
            onGameInterrupted()
        }
        onBackClick()
    }

    // 點擊格子旋轉處理
    fun onCellTap(r: Int, c: Int, piece: LaserPiece) {
        if (isCleared || piece.isFixed) return

        SoundManager.playMirrorRotateSnap()
        triggerHaptic(30L)
        moveCount++

        val newRot = (piece.rotation + 1) % 4
        val key = "${r}_$c"
        val anim = rotationAnimatables.getOrPut(key) { Animatable(piece.rotation * 90f) }

        coroutineScope.launch {
            anim.animateTo(
                targetValue = anim.value + 90f,
                animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing)
            )
        }

        val newGrid = boardState.grid.mapIndexed { rowIdx, rowList ->
            rowList.mapIndexed { colIdx, p ->
                if (rowIdx == r && colIdx == c && p != null) {
                    p.copy(rotation = newRot)
                } else {
                    p
                }
            }
        }

        val prevLitCount = boardState.litReceiverIndices.size
        val updatedState = LaserRaycaster.trace(
            gridSize = boardState.gridSize,
            grid = newGrid,
            emitterRow = boardState.emitterRow,
            emitterCol = boardState.emitterCol,
            emitterDir = boardState.emitterDir,
            receivers = boardState.receivers,
            moveCount = moveCount,
            receiverColors = boardState.receiverColors
        )
        boardState = updatedState

        if (updatedState.litReceiverIndices.size > prevLitCount) {
            SoundManager.playCrystalActivate()
        } else if (updatedState.segments.isNotEmpty()) {
            SoundManager.playLaserConnect()
        }

        if (updatedState.isCleared && !isCleared) {
            isCleared = true
            isTimerRunning = false
            SoundManager.playLaserMazeClear()
            triggerHaptic(80L)

            for (i in 0 until 36) {
                val angle = (i * (360f / 36f)) * (PI.toFloat() / 180f)
                val speed = (4f + (i % 6) * 2.2f)
                particles.add(
                    MazeParticle(
                        x = 0f,
                        y = 0f,
                        vx = cos(angle) * speed,
                        vy = sin(angle) * speed,
                        alpha = 1f,
                        color = when (i % 3) {
                            0 -> Color(0xFF00E5FF)
                            1 -> Color(0xFFFFD700)
                            else -> Color(0xFFFF1744)
                        },
                        radius = (4f + (i % 4) * 2f)
                    )
                )
            }

            coroutineScope.launch {
                delay(650)
                onGameComplete(elapsedTimeMs)
                showClearDialog = true
            }
        }
    }

    // 外層套用主題背景
    AppBackground(themeStyle = appTheme) {
        Scaffold(
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .statusBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        SoundManager.playClick()
                        if (!isCleared) onGameInterrupted()
                        onBackClick()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // 關卡進度膠囊晶片
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = Localization.getString("laser_maze_title", language),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = Localization.getString("laser_maze_level", language, currentLevel),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontSize = 11.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 3D 視角切換按鈕 (微傾 18° <-> 正俯視 0°)
                        IconButton(onClick = {
                            SoundManager.playClick()
                            toggleTiltView()
                        }) {
                            Text(
                                text = if (tiltPitch > 4f) "📐" else "平",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // 任務簡報與指引按鈕
                        IconButton(onClick = {
                            SoundManager.playClick()
                            showBriefingDialog = true
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = "Help",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        // 重設按鈕
                        IconButton(onClick = {
                            SoundManager.playClick()
                            loadLevel(currentLevel, showBriefing = false)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        // 排行榜按鈕
                        IconButton(onClick = {
                            SoundManager.playClick()
                            onLeaderboardClick()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Leaderboard,
                                contentDescription = "Leaderboard",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 頂部數據狀態列 (耗時、步數、水晶充能)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 耗時
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "⏱ ", fontSize = 12.sp)
                            val totalSec = elapsedTimeMs / 1000
                            val mins = totalSec / 60
                            val secs = totalSec % 60
                            val tenths = (elapsedTimeMs % 1000) / 100
                            Text(
                                text = String.format("%02d:%02d.%d", mins, secs, tenths),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }

                    // 步數
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                    ) {
                        Text(
                            text = Localization.getString("laser_maze_moves", language, moveCount),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    // 水晶充能指示
                    val litCount = boardState.litReceiverIndices.size
                    val totalCount = boardState.receivers.size
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (litCount == totalCount && totalCount > 0) {
                            palette.accentColor.copy(alpha = 0.25f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                        },
                        border = if (litCount == totalCount && totalCount > 0) {
                            BorderStroke(1.5.dp, palette.accentColor)
                        } else null
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🎯 ",
                                fontSize = 12.sp
                            )
                            Text(
                                text = Localization.getString("laser_maze_crystals", language, litCount, totalCount),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (litCount == totalCount && totalCount > 0) palette.accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 主光學棋盤視圖 (Box + 3D graphicsLayer + Canvas)
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.verticalGradient(palette.boardBackgroundGradient))
                        .border(1.5.dp, palette.boardBorderColor, RoundedCornerShape(20.dp))
                ) {
                    val availableWidth = constraints.maxWidth.toFloat()
                    val availableHeight = constraints.maxHeight.toFloat()
                    val gridSize = boardState.gridSize

                    // 留出 3D 視角景深邊距 (預留 7% 透視擴展空間避免微傾時觸及容器圓角)
                    val contentWidth = availableWidth * 0.93f
                    val contentHeight = availableHeight * 0.93f

                    // 判定是否為大棋盤 (8x8, 9x9, 10x10, 12x12)
                    val baseCellSize = min(contentWidth, contentHeight) / gridSize
                    val isLargeMap = gridSize >= 8
                    val cellSize = if (isLargeMap) max(baseCellSize, 52f * LocalDensity.current.density) else baseCellSize
                    val totalBoardWidth = cellSize * gridSize
                    val totalBoardHeight = cellSize * gridSize

                    val maxPanX = max(0f, (totalBoardWidth - contentWidth) / 2f)
                    val maxPanY = max(0f, (totalBoardHeight - contentHeight) / 2f)

                    val boardOriginX = (availableWidth - totalBoardWidth) / 2f + panOffsetX.coerceIn(-maxPanX, maxPanX)
                    val boardOriginY = (availableHeight - totalBoardHeight) / 2f + panOffsetY.coerceIn(-maxPanY, maxPanY)

                    val currentMaxPanX by rememberUpdatedState(maxPanX)
                    val currentMaxPanY by rememberUpdatedState(maxPanY)
                    val currentBoardOriginX by rememberUpdatedState(boardOriginX)
                    val currentBoardOriginY by rememberUpdatedState(boardOriginY)
                    val currentCellSize by rememberUpdatedState(cellSize)
                    val currentGridSize by rememberUpdatedState(gridSize)
                    val currentBoardState by rememberUpdatedState(boardState)
                    val currentIsCleared by rememberUpdatedState(isCleared)
                    val currentIsAngleAdjustMode by rememberUpdatedState(isAngleAdjustMode)

                    // 3D 微傾透視渲染層 (Box + graphicsLayer)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                rotationX = tiltPitch.coerceIn(0f, 30f)
                                rotationY = tiltYaw.coerceIn(-25f, 25f)
                                cameraDistance = 16f * density
                            }
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(isLargeMap, currentIsAngleAdjustMode) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        if (isLargeMap && !currentIsAngleAdjustMode) {
                                            panOffsetX = (panOffsetX + dragAmount.x).coerceIn(-currentMaxPanX, currentMaxPanX)
                                            panOffsetY = (panOffsetY + dragAmount.y).coerceIn(-currentMaxPanY, currentMaxPanY)
                                        } else {
                                            // 自由微調 3D 視角 (限制不超過 30 度)
                                            tiltYaw = (tiltYaw + dragAmount.x * 0.16f).coerceIn(-25f, 25f)
                                            tiltPitch = (tiltPitch - dragAmount.y * 0.16f).coerceIn(0f, 30f)
                                        }
                                    }
                                }
                                .pointerInput(Unit) {
                                    detectTapGestures { tapOffset ->
                                        val localX = tapOffset.x - currentBoardOriginX
                                        val localY = tapOffset.y - currentBoardOriginY
                                        val col = (localX / currentCellSize).toInt()
                                        val row = (localY / currentCellSize).toInt()
                                        if (row in 0 until currentGridSize && col in 0 until currentGridSize) {
                                            val piece = currentBoardState.grid[row][col]
                                            if (piece != null && !currentIsCleared) {
                                                onCellTap(row, col, piece)
                                            }
                                        }
                                    }
                                }
                        ) {
                            // 1. 繪製基座格子與風格自適應凹槽
                            for (r in 0 until gridSize) {
                                for (c in 0 until gridSize) {
                                    val cx = boardOriginX + c * cellSize
                                    val cy = boardOriginY + r * cellSize
                                    val pad = 2.5f

                                    drawRoundRect(
                                        color = palette.slotBackground,
                                        topLeft = Offset(cx + pad, cy + pad),
                                        size = Size(cellSize - pad * 2, cellSize - pad * 2),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                                    )
                                    drawRoundRect(
                                        color = palette.slotBorder,
                                        topLeft = Offset(cx + pad, cy + pad),
                                        size = Size(cellSize - pad * 2, cellSize - pad * 2),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f),
                                        style = Stroke(width = 1f)
                                    )
                                    // 中心微縮導軌十字微點
                                    drawCircle(
                                        color = palette.slotGuideDot,
                                        radius = 1.5f,
                                        center = Offset(cx + cellSize / 2f, cy + cellSize / 2f)
                                    )
                                }
                            }

                            // 2. 繪製各格子物件 (3A 工藝手繪)
                            for (r in 0 until gridSize) {
                                for (c in 0 until gridSize) {
                                    val piece = boardState.grid[r][c] ?: continue
                                    val cellCenterX = boardOriginX + (c + 0.5f) * cellSize
                                    val cellCenterY = boardOriginY + (r + 0.5f) * cellSize

                                    val key = "${r}_$c"
                                    val anim = rotationAnimatables[key]
                                    val currentAngle = anim?.value ?: (piece.rotation * 90f)

                                    drawOpticalPiece(
                                        piece = piece,
                                        centerX = cellCenterX,
                                        centerY = cellCenterY,
                                        size = cellSize * 0.82f,
                                        rotationDegrees = currentAngle,
                                        isLit = (Pair(r, c) in boardState.receivers && boardState.receivers.indexOf(Pair(r, c)) in boardState.litReceiverIndices),
                                        palette = palette
                                    )
                                }
                            }

                            // 3. 繪製雷射光束 (雙層高能發光 Bloom + 鏡面端點星芒)
                            for (seg in boardState.segments) {
                                val startX = boardOriginX + (seg.startCol + 0.5f) * cellSize
                                val startY = boardOriginY + (seg.startRow + 0.5f) * cellSize
                                val endX = boardOriginX + (seg.endCol + 0.5f) * cellSize
                                val endY = boardOriginY + (seg.endRow + 0.5f) * cellSize

                                val beamColor = Color(seg.colorHex)

                                // 外層廣域霓虹光暈 (Bloom Glow)
                                drawLine(
                                    color = beamColor.copy(alpha = 0.38f),
                                    start = Offset(startX, startY),
                                    end = Offset(endX, endY),
                                    strokeWidth = 14f,
                                    cap = StrokeCap.Round
                                )
                                // 中層高飽和光束
                                drawLine(
                                    color = beamColor.copy(alpha = 0.75f),
                                    start = Offset(startX, startY),
                                    end = Offset(endX, endY),
                                    strokeWidth = 8f,
                                    cap = StrokeCap.Round
                                )
                                // 內層純白高亮核心
                                drawLine(
                                    color = Color.White,
                                    start = Offset(startX, startY),
                                    end = Offset(endX, endY),
                                    strokeWidth = 3f,
                                    cap = StrokeCap.Round
                                )

                                // 端點折射星芒高光 (Focal Sparkle)
                                drawCircle(
                                    color = beamColor.copy(alpha = 0.85f),
                                    radius = 6.5f,
                                    center = Offset(endX, endY)
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = 2.8f,
                                    center = Offset(endX, endY)
                                )
                            }

                            // 4. 繪製通關星塵超載粒子
                            val boardCenterX = boardOriginX + totalBoardWidth / 2f
                            val boardCenterY = boardOriginY + totalBoardHeight / 2f
                            for (p in particles) {
                                drawCircle(
                                    color = p.color.copy(alpha = p.alpha),
                                    radius = p.radius,
                                    center = Offset(boardCenterX + p.x, boardCenterY + p.y)
                                )
                            }
                        }
                    }

                    // 5. 【微型導航雷達小地圖 (Mini-map)】(置於 3D 透視層之外，維持清晰平面 HUD)
                    if (isLargeMap) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val miniW = 96f * density
                            val miniH = 96f * density
                            val miniX = availableWidth - miniW - 14f
                            val miniY = 14f

                            // 小地圖底板 (風格自適應)
                            drawRoundRect(
                                color = palette.miniMapBackground,
                                topLeft = Offset(miniX, miniY),
                                size = Size(miniW, miniH),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
                            )
                            drawRoundRect(
                                color = palette.miniMapBorder,
                                topLeft = Offset(miniX, miniY),
                                size = Size(miniW, miniH),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f),
                                style = Stroke(width = 1.5f)
                            )

                            val miniCellW = miniW / gridSize
                            val miniCellH = miniH / gridSize

                            // 微縮網格線
                            for (i in 1 until gridSize) {
                                drawLine(
                                    color = palette.miniMapGridLine,
                                    start = Offset(miniX + i * miniCellW, miniY),
                                    end = Offset(miniX + i * miniCellW, miniY + miniH),
                                    strokeWidth = 0.8f
                                )
                                drawLine(
                                    color = palette.miniMapGridLine,
                                    start = Offset(miniX, miniY + i * miniCellH),
                                    end = Offset(miniX + miniW, miniY + i * miniCellH),
                                    strokeWidth = 0.8f
                                )
                            }

                            // 繪製發射塔微標記
                            val emX = miniX + boardState.emitterCol * miniCellW + miniCellW / 2f
                            val emY = miniY + boardState.emitterRow * miniCellH + miniCellH / 2f
                            drawCircle(color = Color(0xFFFF9800), radius = 3.5f, center = Offset(emX, emY))

                            // 繪製微縮終點靶心點與傳送門點
                            for (recv in boardState.receivers) {
                                val rx = miniX + recv.second * miniCellW + miniCellW / 2f
                                val ry = miniY + recv.first * miniCellH + miniCellH / 2f
                                val isLit = boardState.receivers.indexOf(recv) in boardState.litReceiverIndices
                                val reqColor = if (boardState.receiverColors[recv] != null && boardState.receiverColors[recv] != 0xFF00E5FFL) {
                                    Color(boardState.receiverColors[recv]!!)
                                } else {
                                    Color(0xFFFFB300) // 亮金終點標誌色
                                }
                                // 外圈醒目目標光環
                                drawCircle(
                                    color = if (isLit) Color.White else reqColor,
                                    radius = 4.2f,
                                    center = Offset(rx, ry)
                                )
                                // 內層高對比受光靶心
                                drawCircle(
                                    color = if (isLit) reqColor else Color(0xFF0F172A),
                                    radius = 2.4f,
                                    center = Offset(rx, ry)
                                )
                                if (!isLit) {
                                    drawCircle(
                                        color = Color.White,
                                        radius = 1.0f,
                                        center = Offset(rx, ry)
                                    )
                                }
                            }

                            // 繪製高科技取景框 (帶相機角標 [ ])
                            val viewW = (availableWidth / totalBoardWidth).coerceAtMost(1f) * miniW
                            val viewH = (availableHeight / totalBoardHeight).coerceAtMost(1f) * miniH
                            val viewX = miniX + (-boardOriginX / totalBoardWidth).coerceIn(0f, 1f - viewW / miniW) * miniW
                            val viewY = miniY + (-boardOriginY / totalBoardHeight).coerceIn(0f, 1f - viewH / miniH) * miniH

                            // 視窗主體半透底色
                            drawRect(
                                color = palette.miniMapViewport.copy(alpha = 0.18f),
                                topLeft = Offset(viewX, viewY),
                                size = Size(viewW, viewH)
                            )
                            drawRect(
                                color = palette.miniMapViewport.copy(alpha = 0.85f),
                                topLeft = Offset(viewX, viewY),
                                size = Size(viewW, viewH),
                                style = Stroke(width = 1.2f)
                            )
                            // 四角科技角標
                            val bracketLen = min(viewW, viewH) * 0.25f
                            // 左上
                            drawLine(color = palette.miniMapViewport, start = Offset(viewX, viewY), end = Offset(viewX + bracketLen, viewY), strokeWidth = 2.2f)
                            drawLine(color = palette.miniMapViewport, start = Offset(viewX, viewY), end = Offset(viewX, viewY + bracketLen), strokeWidth = 2.2f)
                            // 右下
                            drawLine(color = palette.miniMapViewport, start = Offset(viewX + viewW, viewY + viewH), end = Offset(viewX + viewW - bracketLen, viewY + viewH), strokeWidth = 2.2f)
                            drawLine(color = palette.miniMapViewport, start = Offset(viewX + viewW, viewY + viewH), end = Offset(viewX + viewW, viewY + viewH - bracketLen), strokeWidth = 2.2f)
                        }
                    }

                    // 6. 【3D 微傾視角調節懸浮膠囊 (HUD Pill)】(置於 3D 透視層之外，方便玩家隨時調節與回正)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                        border = BorderStroke(1.dp, palette.boardBorderColor.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isTilted = tiltPitch > 2f
                            Text(
                                text = if (isTilted) "📐 " + Localization.getString("laser_maze_tilt_view", language) + " ${tiltPitch.toInt()}°" else "📐 " + Localization.getString("laser_maze_top_view", language),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isTilted) palette.accentColor else MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.clickable {
                                    SoundManager.playClick()
                                    toggleTiltView()
                                }
                            )

                            if (isLargeMap) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .height(12.dp)
                                        .width(1.dp)
                                        .background(MaterialTheme.colorScheme.outlineVariant)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isAngleAdjustMode) "🔄 " + Localization.getString("laser_maze_mode_tilt", language) else "✋ " + Localization.getString("laser_maze_mode_pan", language),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAngleAdjustMode) palette.accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.clickable {
                                        SoundManager.playClick()
                                        isAngleAdjustMode = !isAngleAdjustMode
                                    }
                                )
                            }

                            if (tiltPitch != 18f || tiltYaw != -5f) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "↺ " + Localization.getString("laser_maze_reset_view", language),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.clickable {
                                        SoundManager.playClick()
                                        resetViewAngle()
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 底部操作提示標籤
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = Localization.getString("laser_maze_hint", language),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    // 開局「任務簡報與光學指引」彈窗 (Mission Briefing Dialog)
    if (showBriefingDialog) {
        AlertDialog(
            onDismissRequest = {
                showBriefingDialog = false
                isTimerRunning = true
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "📡 ", fontSize = 22.sp)
                    Text(
                        text = Localization.getString("laser_maze_briefing_title", language),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 關卡規格與目標卡片
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = palette.briefingCardBg,
                        border = BorderStroke(1.dp, palette.boardBorderColor.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = Localization.getString("laser_maze_level", language, currentLevel),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = palette.accentColor
                                    )
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = palette.accentColor.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = Localization.getString("laser_maze_grid_dim", language, boardState.gridSize, boardState.gridSize),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = palette.accentColor,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = Localization.getString("laser_maze_mission_goal", language),
                                style = MaterialTheme.typography.bodySmall.copy(color = palette.onBriefingCardText)
                            )
                        }
                    }

                    Text(
                        text = Localization.getString("laser_maze_mechanics_title", language),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )

                    // 機關清單提示
                    BriefingMechanicItem(
                        icon = "🪞",
                        title = Localization.getString("piece_mirror_single", language),
                        desc = "點擊旋轉 90°，微傾視角下可清晰看見立體鍍銀鏡面與反射朝向",
                        textColor = palette.onBriefingCardText
                    )
                    BriefingMechanicItem(
                        icon = "🌀",
                        title = Localization.getString("piece_portal", language),
                        desc = "成對時空蟲洞，光束射入時跨越空間即刻躍遷穿出",
                        textColor = palette.onBriefingCardText
                    )
                    BriefingMechanicItem(
                        icon = "🎨",
                        title = "三原色濾光鏡與混光稜鏡",
                        desc = "濾除或合成專屬波長（紅/綠/藍），匹配共振水晶",
                        textColor = palette.onBriefingCardText
                    )
                    BriefingMechanicItem(
                        icon = "🎯",
                        title = Localization.getString("piece_receiver", language),
                        desc = "雷射的終點目的地！需引導光束擊中受光靶心激發超載，點亮全場終點即通關",
                        textColor = palette.onBriefingCardText
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        SoundManager.playClick()
                        showBriefingDialog = false
                        isTimerRunning = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accentColor)
                ) {
                    Text(
                        text = Localization.getString("laser_maze_start_mission", language),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }

    // 通關結算彈窗
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "⚡ ", fontSize = 22.sp)
                    Text(
                        text = Localization.getString("laser_maze_win_title", language),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = Localization.getString("laser_maze_win_desc", language),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = Localization.getString("your_time", language),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                            val totalSec = elapsedTimeMs / 1000
                            val mins = totalSec / 60
                            val secs = totalSec % 60
                            val tenths = (elapsedTimeMs % 1000) / 100
                            Text(
                                text = String.format("%02d:%02d.%d", mins, secs, tenths),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Text(
                                    text = "旋轉步數：$moveCount 步",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                                Text(
                                    text = "評級：⭐⭐⭐ 大師",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFB300)
                                    )
                                )
                            }
                        }
                    }

                    Text(
                        text = "✓ " + Localization.getString("score_saved", language),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        SoundManager.playClick()
                        showClearDialog = false
                        if (currentLevel < 10) {
                            loadLevel(currentLevel + 1, showBriefing = true)
                        } else {
                            loadLevel(1, showBriefing = true)
                        }
                    }
                ) {
                    Text(
                        text = if (currentLevel < 10) {
                            Localization.getString("laser_maze_next_level", language)
                        } else {
                            Localization.getString("play_again", language)
                        }
                    )
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            SoundManager.playClick()
                            showClearDialog = false
                            onLeaderboardClick()
                        }
                    ) {
                        Text(Localization.getString("leaderboard_title", language))
                    }
                    TextButton(
                        onClick = {
                            SoundManager.playClick()
                            showClearDialog = false
                            onBackClick()
                        }
                    ) {
                        Text(Localization.getString("back_to_menu", language))
                    }
                }
            }
        )
    }
}

@Composable
private fun BriefingMechanicItem(
    icon: String,
    title: String,
    desc: String,
    textColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, fontSize = 20.sp, modifier = Modifier.padding(end = 10.dp))
        Column {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textColor)
            Text(text = desc, fontSize = 11.sp, color = textColor.copy(alpha = 0.8f))
        }
    }
}

/**
 * 繪製單個光學元件實體 (3A / 旗艦手遊大廠級頂級工藝)
 */
private fun DrawScope.drawOpticalPiece(
    piece: LaserPiece,
    centerX: Float,
    centerY: Float,
    size: Float,
    rotationDegrees: Float,
    isLit: Boolean,
    palette: LaserMazePalette
) {
    val half = size / 2f

    when (piece.type) {
        OpticalPieceType.EMITTER -> {
            // 重型高能光學發射砲塔 (散熱鰭片 + 聚焦線圈 + 晶體發射鏡頭)
            rotate(rotationDegrees, pivot = Offset(centerX, centerY)) {
                // 1. 底盤散熱散片 (背部散熱鰭片)
                val finColor = Color(0xFF1E293B)
                drawRect(
                    color = finColor,
                    topLeft = Offset(centerX - half * 0.55f, centerY + half * 0.35f),
                    size = Size(half * 1.1f, half * 0.3f)
                )
                // 鰭片刻痕
                for (f in -2..2) {
                    val fx = centerX + f * (half * 0.22f)
                    drawLine(
                        color = Color(0xFF475569),
                        start = Offset(fx, centerY + half * 0.35f),
                        end = Offset(fx, centerY + half * 0.65f),
                        strokeWidth = 2f
                    )
                }

                // 2. 砲塔主基座圓盤 (雙層金屬)
                drawCircle(
                    color = Color(0xFF0F172A),
                    radius = half * 0.82f,
                    center = Offset(centerX, centerY)
                )
                drawCircle(
                    color = palette.boardBorderColor,
                    radius = half * 0.82f,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 2.5f)
                )

                // 3. 磁能聚焦線圈環
                drawCircle(
                    color = Color(0xFF334155),
                    radius = half * 0.58f,
                    center = Offset(centerX, centerY)
                )
                drawCircle(
                    color = Color(0xFFFF9800),
                    radius = half * 0.58f,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 2f)
                )

                // 4. 晶體發射管與高能聚焦槍口 (朝上)
                val barrelPath = Path().apply {
                    moveTo(centerX - half * 0.22f, centerY + half * 0.1f)
                    lineTo(centerX - half * 0.18f, centerY - half * 0.72f)
                    lineTo(centerX + half * 0.18f, centerY - half * 0.72f)
                    lineTo(centerX + half * 0.22f, centerY + half * 0.1f)
                    close()
                }
                drawPath(barrelPath, color = Color(0xFF1E293B))
                drawPath(barrelPath, color = Color.White.copy(alpha = 0.5f), style = Stroke(width = 1.2f))

                // 發射槍口高能耀斑
                drawCircle(
                    color = Color(0xFF00E5FF),
                    radius = half * 0.22f,
                    center = Offset(centerX, centerY - half * 0.65f)
                )
                drawCircle(
                    color = Color.White,
                    radius = half * 0.1f,
                    center = Offset(centerX, centerY - half * 0.65f)
                )
            }
        }

        OpticalPieceType.RECEIVER -> {
            // 終點：高辨識度量子受光靶心 (四隅鎖定角標 + 雙層同心雷達靶環 + 醒目量子核心 + 十字準星)
            drawTargetReceiver(
                centerX = centerX,
                centerY = centerY,
                half = half,
                piece = piece,
                isLit = isLit,
                palette = palette
            )
        }

        OpticalPieceType.MIRROR_SINGLE -> {
            // 單面 45° 鍍銀反光鏡 (3D 立體斜切鏡面 + 鏡背裝甲側身 + 精密轉盤 + 鍍銀高光 + 合金固定扣)
            rotate(rotationDegrees, pivot = Offset(centerX, centerY)) {
                drawPrecisionTurntable(centerX, centerY, half, piece.isDecoy)

                // 鏡背裝甲 (背部三角斜板，帶有金屬加強筋與微型鉚釘)
                val p1 = Offset(centerX - half * 0.65f, centerY + half * 0.65f)
                val p2 = Offset(centerX + half * 0.65f, centerY - half * 0.65f)
                val backCorner = Offset(centerX - half * 0.45f, centerY - half * 0.45f)

                // 1. 鏡背裝甲側身厚度 (3D Extruded Armor Thickness)
                val armorSide = Path().apply {
                    moveTo(p1.x, p1.y)
                    lineTo(p2.x, p2.y)
                    lineTo(p2.x - half * 0.08f, p2.y - half * 0.08f)
                    lineTo(p1.x - half * 0.08f, p1.y - half * 0.08f)
                    close()
                }
                drawPath(armorSide, color = Color(0xFF1E293B))

                val backArmor = Path().apply {
                    moveTo(p1.x, p1.y)
                    lineTo(p2.x, p2.y)
                    lineTo(backCorner.x, backCorner.y)
                    close()
                }
                drawPath(backArmor, color = Color(0xFF263238))
                drawPath(backArmor, color = Color(0xFF455A64), style = Stroke(width = 1.5f))

                // 背板微型六角鉚釘
                drawCircle(color = Color(0xFFB0BEC5), radius = 2.2f, center = Offset(centerX - half * 0.25f, centerY - half * 0.25f))

                // 2. 45° 玻璃鏡面立體斜切面 (3D Beveled Optical Mirror Prism Facet)
                // 朝向前向反射側擴展立體斜面
                val normX = half * 0.12f
                val normY = half * 0.12f
                val facetPath = Path().apply {
                    moveTo(p1.x, p1.y)
                    lineTo(p2.x, p2.y)
                    lineTo(p2.x + normX, p2.y + normY)
                    lineTo(p1.x + normX, p1.y + normY)
                    close()
                }
                // 鏡面主體真空鍍銀層與抗反射青藍微光
                drawPath(
                    facetPath,
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFE2E8F0), Color.White, Color(0xFFCBD5E1), Color(0xFF94A3B8)),
                        start = p1,
                        end = Offset(p2.x + normX, p2.y + normY)
                    )
                )
                // 鏡面倒角邊緣高光
                drawPath(facetPath, color = Color.White.copy(alpha = 0.85f), style = Stroke(width = 1.2f))

                // 鏡面流光高光帶 (Specular Sheen Sweep)
                drawLine(
                    color = Color.White,
                    start = Offset(centerX - half * 0.18f + normX * 0.5f, centerY + half * 0.18f + normY * 0.5f),
                    end = Offset(centerX + half * 0.35f + normX * 0.5f, centerY - half * 0.35f + normY * 0.5f),
                    strokeWidth = 2.2f,
                    cap = StrokeCap.Round
                )

                // 兩端合金夾扣 (Mounting Clamps)
                drawCircle(color = Color(0xFF37474F), radius = 4.2f, center = p1)
                drawCircle(color = Color(0xFF90A4AE), radius = 3.0f, center = p1)
                drawCircle(color = Color(0xFF37474F), radius = 4.2f, center = p2)
                drawCircle(color = Color(0xFF90A4AE), radius = 3.0f, center = p2)
            }
        }

        OpticalPieceType.MIRROR_DOUBLE -> {
            // 雙面反射鏡 (兩側皆具備立體斜切鏡面，中央高折射水晶夾層)
            rotate(rotationDegrees, pivot = Offset(centerX, centerY)) {
                drawPrecisionTurntable(centerX, centerY, half, piece.isDecoy)

                val p1 = Offset(centerX - half * 0.65f, centerY + half * 0.65f)
                val p2 = Offset(centerX + half * 0.65f, centerY - half * 0.65f)
                val normX = half * 0.10f
                val normY = half * 0.10f

                // 中央高折射光學晶體層 (Core Sapphire Crystal)
                val corePath = Path().apply {
                    moveTo(p1.x - normX * 0.5f, p1.y - normY * 0.5f)
                    lineTo(p2.x - normX * 0.5f, p2.y - normY * 0.5f)
                    lineTo(p2.x + normX * 0.5f, p2.y + normY * 0.5f)
                    lineTo(p1.x + normX * 0.5f, p1.y + normY * 0.5f)
                    close()
                }
                drawPath(corePath, color = Color(0xFF0284C7))

                // 正面鏡面 (Front Mirror Facet)
                val frontFacet = Path().apply {
                    moveTo(p1.x, p1.y)
                    lineTo(p2.x, p2.y)
                    lineTo(p2.x + normX, p2.y + normY)
                    lineTo(p1.x + normX, p1.y + normY)
                    close()
                }
                drawPath(frontFacet, brush = Brush.linearGradient(listOf(Color(0xFFE2E8F0), Color.White, Color(0xFF94A3B8)), start = p1, end = Offset(p2.x + normX, p2.y + normY)))
                drawPath(frontFacet, color = Color.White.copy(alpha = 0.8f), style = Stroke(width = 1f))

                // 反面鏡面 (Back Mirror Facet)
                val backFacet = Path().apply {
                    moveTo(p1.x, p1.y)
                    lineTo(p2.x, p2.y)
                    lineTo(p2.x - normX, p2.y - normY)
                    lineTo(p1.x - normX, p1.y - normY)
                    close()
                }
                drawPath(backFacet, brush = Brush.linearGradient(listOf(Color(0xFF94A3B8), Color.White, Color(0xFFE2E8F0)), start = p1, end = Offset(p2.x - normX, p2.y - normY)))
                drawPath(backFacet, color = Color.White.copy(alpha = 0.8f), style = Stroke(width = 1f))

                // 兩端合金固定扣
                drawCircle(color = Color(0xFF0369A1), radius = 4.5f, center = p1)
                drawCircle(color = Color(0xFF38BDF8), radius = 2.8f, center = p1)
                drawCircle(color = Color(0xFF0369A1), radius = 4.5f, center = p2)
                drawCircle(color = Color(0xFF38BDF8), radius = 2.8f, center = p2)
            }
        }

        OpticalPieceType.BEAM_SPLITTER, OpticalPieceType.PRISM_SYNTHESIZER -> {
            // 十字分光稜鏡 / 稜鏡混光合成器 (3D 立體晶體立方 + 彩虹色散光斑)
            rotate(rotationDegrees, pivot = Offset(centerX, centerY)) {
                drawPrecisionTurntable(centerX, centerY, half, piece.isDecoy)

                val cubeR = half * 0.65f
                // 半透光學晶體立方
                drawRoundRect(
                    color = Color(0xFF0284C7).copy(alpha = 0.25f),
                    topLeft = Offset(centerX - cubeR, centerY - cubeR),
                    size = Size(cubeR * 2, cubeR * 2),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                )
                drawRoundRect(
                    color = Color(0xFF38BDF8),
                    topLeft = Offset(centerX - cubeR, centerY - cubeR),
                    size = Size(cubeR * 2, cubeR * 2),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
                    style = Stroke(width = 1.8f)
                )

                // 45° 介電質鍍膜斜切面 (彩虹色散光紋)
                drawLine(
                    color = Color(0xFFFF5252).copy(alpha = 0.7f),
                    start = Offset(centerX - cubeR * 0.75f - 1.5f, centerY + cubeR * 0.75f),
                    end = Offset(centerX + cubeR * 0.75f - 1.5f, centerY - cubeR * 0.75f),
                    strokeWidth = 2.5f
                )
                drawLine(
                    color = Color.White,
                    start = Offset(centerX - cubeR * 0.75f, centerY + cubeR * 0.75f),
                    end = Offset(centerX + cubeR * 0.75f, centerY - cubeR * 0.75f),
                    strokeWidth = 3.5f
                )
                drawLine(
                    color = Color(0xFF00E5FF).copy(alpha = 0.7f),
                    start = Offset(centerX - cubeR * 0.75f + 1.5f, centerY + cubeR * 0.75f),
                    end = Offset(centerX + cubeR * 0.75f + 1.5f, centerY - cubeR * 0.75f),
                    strokeWidth = 2.5f
                )
            }
        }

        OpticalPieceType.FILTER_RED, OpticalPieceType.FILTER_GREEN, OpticalPieceType.FILTER_BLUE -> {
            // 三原色濾光鏡 (立體合金鏡框 + 高純度光學濾光板 + 波長標籤)
            val filterColor = when (piece.type) {
                OpticalPieceType.FILTER_RED -> Color(0xFFFF1744)
                OpticalPieceType.FILTER_GREEN -> Color(0xFF00E676)
                else -> Color(0xFF2979FF)
            }
            rotate(rotationDegrees, pivot = Offset(centerX, centerY)) {
                // 外合金框架
                drawRoundRect(
                    color = Color(0xFF1E293B),
                    topLeft = Offset(centerX - half * 0.75f, centerY - half * 0.75f),
                    size = Size(half * 1.5f, half * 1.5f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                )
                drawRoundRect(
                    color = filterColor,
                    topLeft = Offset(centerX - half * 0.75f, centerY - half * 0.75f),
                    size = Size(half * 1.5f, half * 1.5f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f),
                    style = Stroke(width = 2.5f)
                )
                // 濾光通光晶板
                drawRoundRect(
                    color = filterColor.copy(alpha = 0.45f),
                    topLeft = Offset(centerX - half * 0.55f, centerY - half * 0.55f),
                    size = Size(half * 1.1f, half * 1.1f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                )
                // 晶面高光
                drawLine(
                    color = Color.White.copy(alpha = 0.7f),
                    start = Offset(centerX - half * 0.45f, centerY - half * 0.35f),
                    end = Offset(centerX + half * 0.35f, centerY - half * 0.45f),
                    strokeWidth = 2f
                )
            }
        }

        OpticalPieceType.PORTAL -> {
            // 時空傳送門 (雙層金屬維度環 + 旋轉星雲蟲洞渦流)
            drawCircle(color = Color(0xFF0F172A), radius = half * 0.82f, center = Offset(centerX, centerY))
            drawCircle(color = Color(0xFF7C3AED), radius = half * 0.82f, center = Offset(centerX, centerY), style = Stroke(width = 3f))
            drawCircle(color = Color(0xFFC084FC), radius = half * 0.65f, center = Offset(centerX, centerY), style = Stroke(width = 1.8f))
            drawCircle(color = Color(0xFF00E5FF).copy(alpha = 0.45f), radius = half * 0.45f, center = Offset(centerX, centerY))
            drawCircle(color = Color.White, radius = half * 0.2f, center = Offset(centerX, centerY))

            // 渦流旋轉刺
            for (ang in 0 until 4) {
                val rad = (ang * 90f + rotationDegrees) * (PI.toFloat() / 180f)
                val sx = centerX + cos(rad) * (half * 0.3f)
                val sy = centerY + sin(rad) * (half * 0.3f)
                val ex = centerX + cos(rad + 0.4f) * (half * 0.65f)
                val ey = centerY + sin(rad + 0.4f) * (half * 0.65f)
                drawLine(color = Color(0xFFE879F9), start = Offset(sx, sy), end = Offset(ex, ey), strokeWidth = 2f)
            }
        }

        OpticalPieceType.OBSTACLE_ABSORBER -> {
            // 碳素吸光石 (非對稱多邊形立體晶岩 Low-Poly 3D Rock + 三面光影 + 天然裂隙 + 環境遮蔽陰影)
            // 1. 接觸陰影 (Ambient Occlusion Drop Shadow)
            drawOval(
                color = Color.Black.copy(alpha = 0.45f),
                topLeft = Offset(centerX - half * 0.82f, centerY + half * 0.45f),
                size = Size(half * 1.64f, half * 0.45f)
            )

            // 2. 多面體巨石多邊形頂點
            val pTop = Offset(centerX - half * 0.15f, centerY - half * 0.75f)
            val pTopRight = Offset(centerX + half * 0.72f, centerY - half * 0.42f)
            val pRight = Offset(centerX + half * 0.78f, centerY + half * 0.35f)
            val pBottom = Offset(centerX + half * 0.1f, centerY + half * 0.75f)
            val pBottomLeft = Offset(centerX - half * 0.72f, centerY + half * 0.45f)
            val pLeft = Offset(centerX - half * 0.78f, centerY - half * 0.32f)
            val pCenter = Offset(centerX - half * 0.05f, centerY - half * 0.05f)

            // 受光頂面 (Top Light Facet)
            val topFacet = Path().apply {
                moveTo(pTop.x, pTop.y)
                lineTo(pTopRight.x, pTopRight.y)
                lineTo(pCenter.x, pCenter.y)
                lineTo(pLeft.x, pLeft.y)
                close()
            }
            drawPath(topFacet, color = Color(0xFF475569))

            // 側邊過渡面 (Midtone Facet)
            val midFacet = Path().apply {
                moveTo(pLeft.x, pLeft.y)
                lineTo(pCenter.x, pCenter.y)
                lineTo(pBottom.x, pBottom.y)
                lineTo(pBottomLeft.x, pBottomLeft.y)
                close()
            }
            drawPath(midFacet, color = Color(0xFF334155))

            // 背光陰影面 (Shadow Facet)
            val shadowFacet = Path().apply {
                moveTo(pCenter.x, pCenter.y)
                lineTo(pTopRight.x, pTopRight.y)
                lineTo(pRight.x, pRight.y)
                lineTo(pBottom.x, pBottom.y)
                close()
            }
            drawPath(shadowFacet, color = Color(0xFF0F172A))

            // 巨石外稜線輪廓
            val rockOutline = Path().apply {
                moveTo(pTop.x, pTop.y)
                lineTo(pTopRight.x, pTopRight.y)
                lineTo(pRight.x, pRight.y)
                lineTo(pBottom.x, pBottom.y)
                lineTo(pBottomLeft.x, pBottomLeft.y)
                lineTo(pLeft.x, pLeft.y)
                close()
            }
            drawPath(rockOutline, color = Color(0xFF1E293B), style = Stroke(width = 2.2f))

            // 表面地質龜裂紋 (帶微弱暗能量幽光)
            val crackPath = Path().apply {
                moveTo(pCenter.x, pCenter.y)
                lineTo(centerX + half * 0.25f, centerY + half * 0.15f)
                lineTo(centerX + half * 0.45f, centerY + half * 0.38f)
            }
            drawLine(color = Color(0xFF00E5FF).copy(alpha = 0.35f), start = Offset(pCenter.x, pCenter.y), end = Offset(centerX + half * 0.25f, centerY + half * 0.15f), strokeWidth = 2.5f)
            drawPath(crackPath, color = Color(0xFF0F172A), style = Stroke(width = 1.5f))
        }

        OpticalPieceType.OBSTACLE_FIXED_MIRROR -> {
            // 固定折射斜壁 (重裝甲三角基座 + 六角螺栓釘死 + 3D 拋光黃金反射稜鏡)
            rotate(rotationDegrees, pivot = Offset(centerX, centerY)) {
                val t1 = Offset(centerX - half * 0.75f, centerY + half * 0.75f)
                val t2 = Offset(centerX + half * 0.75f, centerY - half * 0.75f)
                val t3 = Offset(centerX - half * 0.75f, centerY - half * 0.75f)

                val normX = half * 0.12f
                val normY = half * 0.12f

                val triangle = Path().apply {
                    moveTo(t1.x, t1.y)
                    lineTo(t2.x, t2.y)
                    lineTo(t3.x, t3.y)
                    close()
                }
                // 重合金深底
                drawPath(triangle, color = Color(0xFF78350F))
                drawPath(triangle, color = Color(0xFFB45309), style = Stroke(width = 2f))

                // 六角螺栓錨點
                drawCircle(color = Color(0xFFFDE68A), radius = 2.5f, center = Offset(t3.x + 6f, t3.y + 6f))
                drawCircle(color = Color(0xFFFDE68A), radius = 2.5f, center = Offset(t1.x + 6f, t1.y - 6f))

                // 45° 拋光黃金立體斜切鏡面 (3D Golden Prism Facet)
                val goldFacet = Path().apply {
                    moveTo(t1.x, t1.y)
                    lineTo(t2.x, t2.y)
                    lineTo(t2.x + normX, t2.y + normY)
                    lineTo(t1.x + normX, t1.y + normY)
                    close()
                }
                drawPath(
                    goldFacet,
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFD97706), Color(0xFFFDE68A), Color(0xFFF59E0B)),
                        start = t1,
                        end = Offset(t2.x + normX, t2.y + normY)
                    )
                )
                drawPath(goldFacet, color = Color.White.copy(alpha = 0.8f), style = Stroke(width = 1.2f))

                // 鏡面流光
                drawLine(
                    color = Color.White,
                    start = Offset(centerX - half * 0.18f + normX * 0.5f, centerY + half * 0.18f + normY * 0.5f),
                    end = Offset(centerX + half * 0.38f + normX * 0.5f, centerY - half * 0.38f + normY * 0.5f),
                    strokeWidth = 2.2f,
                    cap = StrokeCap.Round
                )
            }
        }

        OpticalPieceType.OBSTACLE_TINTED_GLASS -> {
            // 半透綠晶石 (祖母綠多面體透光晶石)
            val glassR = half * 0.72f
            drawRoundRect(
                color = Color(0xFF059669).copy(alpha = 0.35f),
                topLeft = Offset(centerX - glassR, centerY - glassR),
                size = Size(glassR * 2, glassR * 2),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
            )
            drawRoundRect(
                color = Color(0xFF34D399),
                topLeft = Offset(centerX - glassR, centerY - glassR),
                size = Size(glassR * 2, glassR * 2),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f),
                style = Stroke(width = 2.2f)
            )
            // 內部焦散亮線
            drawLine(
                color = Color.White.copy(alpha = 0.6f),
                start = Offset(centerX - glassR * 0.6f, centerY - glassR * 0.6f),
                end = Offset(centerX + glassR * 0.6f, centerY + glassR * 0.6f),
                strokeWidth = 1.5f
            )
        }

        OpticalPieceType.TUNNEL -> {
            // 立體交叉隧道
            drawRect(color = Color(0xFF1E293B), topLeft = Offset(centerX - half * 0.75f, centerY - half * 0.75f), size = Size(half * 1.5f, half * 1.5f))
            drawRect(color = Color(0xFF475569), topLeft = Offset(centerX - half * 0.85f, centerY - half * 0.22f), size = Size(half * 1.7f, half * 0.44f))
            drawRect(color = Color(0xFF64748B), topLeft = Offset(centerX - half * 0.22f, centerY - half * 0.85f), size = Size(half * 0.44f, half * 1.7f))
        }

        OpticalPieceType.EMPTY -> {}
    }
}

/**
 * 輔助繪製精密齒輪機械轉盤 (12 齒防滑刻痕 + 軸承高光)
 */
private fun DrawScope.drawPrecisionTurntable(
    centerX: Float,
    centerY: Float,
    half: Float,
    isDecoy: Boolean
) {
    val turntableR = half * 0.88f

    // 1. 底盤下沉 3D 陰影厚度邊界 (3D Base Extrusion Shadow)
    drawOval(
        color = Color(0xFF090D16).copy(alpha = 0.7f),
        topLeft = Offset(centerX - turntableR, centerY - turntableR + 3.5f),
        size = Size(turntableR * 2, turntableR * 2)
    )

    // 2. 轉盤主體金屬座
    drawCircle(color = Color(0xFF111827), radius = turntableR, center = Offset(centerX, centerY))
    drawCircle(
        color = if (isDecoy) Color(0xFFF59E0B) else Color(0xFF4B5563),
        radius = turntableR,
        center = Offset(centerX, centerY),
        style = Stroke(width = 1.5f)
    )

    // 3. 12 齒外周齒輪防滑刻痕 (Knurling Grips)
    for (i in 0 until 12) {
        val rad = (i * (360f / 12f)) * (PI.toFloat() / 180f)
        val outerX = centerX + cos(rad) * turntableR
        val outerY = centerY + sin(rad) * turntableR
        val innerX = centerX + cos(rad) * (turntableR - 4f)
        val innerY = centerY + sin(rad) * (turntableR - 4f)
        drawLine(
            color = if (isDecoy) Color(0xFFFBBF24).copy(alpha = 0.6f) else Color(0xFF9CA3AF).copy(alpha = 0.4f),
            start = Offset(innerX, innerY),
            end = Offset(outerX, outerY),
            strokeWidth = 1.2f
        )
    }

    // 4. 中心軸承 (Concentric Bearing)
    drawCircle(color = Color(0xFF1F2937), radius = half * 0.28f, center = Offset(centerX, centerY))
    drawCircle(color = Color(0xFFE5E7EB), radius = half * 0.12f, center = Offset(centerX, centerY))
}

/**
 * 繪製終點量子受光靶心 (超高辨識度目標 Target / 醒目同心靶環 / 瞄準準星)
 * 專為讓玩家「一眼就知道那就是終點、要讓雷射到達那裡」所打造之旗艦視覺
 */
private fun DrawScope.drawTargetReceiver(
    centerX: Float,
    centerY: Float,
    half: Float,
    piece: LaserPiece,
    isLit: Boolean,
    palette: LaserMazePalette
) {
    // 取得指定或預設色彩：未指定特殊波長時，以超醒目的「量子耀金 (#FFB300)」為標誌色，在各風格棋盤均有極高對比
    val targetColor = if (piece.requiredColorHex != null && piece.requiredColorHex != 0xFF00E5FFL) {
        Color(piece.requiredColorHex)
    } else {
        Color(0xFFFFB300) // 超亮量子金黃
    }
    val highlightColor = if (isLit) Color.White else Color(0xFFFFE082)

    // 1. 四隅高科技目標鎖定角標 (Target Reticle Brackets: [ ])
    val bracketSize = half * 0.94f
    val bracketLen = half * 0.28f
    val bracketColor = if (isLit) Color.White else targetColor.copy(alpha = 0.95f)
    val bracketStroke = 2.8f

    // 左上角 ┌
    drawLine(color = bracketColor, start = Offset(centerX - bracketSize, centerY - bracketSize), end = Offset(centerX - bracketSize + bracketLen, centerY - bracketSize), strokeWidth = bracketStroke, cap = StrokeCap.Round)
    drawLine(color = bracketColor, start = Offset(centerX - bracketSize, centerY - bracketSize), end = Offset(centerX - bracketSize, centerY - bracketSize + bracketLen), strokeWidth = bracketStroke, cap = StrokeCap.Round)
    // 右上角 ┐
    drawLine(color = bracketColor, start = Offset(centerX + bracketSize, centerY - bracketSize), end = Offset(centerX + bracketSize - bracketLen, centerY - bracketSize), strokeWidth = bracketStroke, cap = StrokeCap.Round)
    drawLine(color = bracketColor, start = Offset(centerX + bracketSize, centerY - bracketSize), end = Offset(centerX + bracketSize, centerY - bracketSize + bracketLen), strokeWidth = bracketStroke, cap = StrokeCap.Round)
    // 右下角 ┘
    drawLine(color = bracketColor, start = Offset(centerX + bracketSize, centerY + bracketSize), end = Offset(centerX + bracketSize - bracketLen, centerY + bracketSize), strokeWidth = bracketStroke, cap = StrokeCap.Round)
    drawLine(color = bracketColor, start = Offset(centerX + bracketSize, centerY + bracketSize), end = Offset(centerX + bracketSize, centerY + bracketSize - bracketLen), strokeWidth = bracketStroke, cap = StrokeCap.Round)
    // 左下角 └
    drawLine(color = bracketColor, start = Offset(centerX - bracketSize, centerY + bracketSize), end = Offset(centerX - bracketSize + bracketLen, centerY + bracketSize), strokeWidth = bracketStroke, cap = StrokeCap.Round)
    drawLine(color = bracketColor, start = Offset(centerX - bracketSize, centerY + bracketSize), end = Offset(centerX - bracketSize, centerY + bracketSize - bracketLen), strokeWidth = bracketStroke, cap = StrokeCap.Round)

    // 2. 鈦黑金屬沉頭基盤 (Deep Titanium Hub)
    val hubRadius = half * 0.76f
    drawCircle(
        color = Color(0xFF0F172A),
        radius = hubRadius,
        center = Offset(centerX, centerY)
    )

    if (isLit) {
        // === 點亮狀態：超載爆發、高頻脈衝外環與十字星芒 ===
        // 外層廣域超載耀斑光暈 (Overload Bloom)
        drawCircle(
            color = targetColor.copy(alpha = 0.35f),
            radius = half * 1.4f,
            center = Offset(centerX, centerY)
        )
        drawCircle(
            color = targetColor.copy(alpha = 0.65f),
            radius = half * 1.08f,
            center = Offset(centerX, centerY)
        )
        // 外靶環超能金屬圈
        drawCircle(
            color = Color.White,
            radius = hubRadius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 3.5f)
        )
        // 內核超載白熾光
        drawCircle(
            color = targetColor,
            radius = half * 0.52f,
            center = Offset(centerX, centerY)
        )
        drawCircle(
            color = Color.White,
            radius = half * 0.36f,
            center = Offset(centerX, centerY)
        )

        // 四向超能長星芒
        val flareLen = half * 1.35f
        drawLine(color = Color.White, start = Offset(centerX - flareLen, centerY), end = Offset(centerX + flareLen, centerY), strokeWidth = 3.2f)
        drawLine(color = Color.White, start = Offset(centerX, centerY - flareLen), end = Offset(centerX, centerY + flareLen), strokeWidth = 3.2f)
        // 45 度副星芒
        val subLen = half * 0.9f
        val offsetSub = subLen * 0.707f
        drawLine(color = targetColor, start = Offset(centerX - offsetSub, centerY - offsetSub), end = Offset(centerX + offsetSub, centerY + offsetSub), strokeWidth = 2.2f)
        drawLine(color = targetColor, start = Offset(centerX + offsetSub, centerY - offsetSub), end = Offset(centerX - offsetSub, centerY + offsetSub), strokeWidth = 2.2f)
    } else {
        // === 未點亮狀態：極高對比終點雷達靶心 (High Contrast Target Bullseye) ===
        // 外靶環：鮮明金屬瞄準圈
        drawCircle(
            color = targetColor.copy(alpha = 0.25f),
            radius = hubRadius,
            center = Offset(centerX, centerY)
        )
        drawCircle(
            color = targetColor,
            radius = hubRadius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 3f)
        )

        // 4 向瞄準十字刻痕 (Crosshair Ticks)
        val tickInner = hubRadius * 0.65f
        val tickOuter = hubRadius * 1.15f
        drawLine(color = targetColor, start = Offset(centerX, centerY - tickOuter), end = Offset(centerX, centerY - tickInner), strokeWidth = 2.5f)
        drawLine(color = targetColor, start = Offset(centerX, centerY + tickInner), end = Offset(centerX, centerY + tickOuter), strokeWidth = 2.5f)
        drawLine(color = targetColor, start = Offset(centerX - tickOuter, centerY), end = Offset(centerX - tickInner, centerY), strokeWidth = 2.5f)
        drawLine(color = targetColor, start = Offset(centerX + tickInner, centerY), end = Offset(centerX + tickOuter, centerY), strokeWidth = 2.5f)

        // 中層：目標同心光環 (Concentric Target Ring)
        val midRadius = half * 0.52f
        drawCircle(
            color = targetColor.copy(alpha = 0.35f),
            radius = midRadius,
            center = Offset(centerX, centerY)
        )
        drawCircle(
            color = highlightColor,
            radius = midRadius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 2f)
        )

        // 核心：高能透光受光晶核 (Luminous Bullseye Core)
        val coreRadius = half * 0.35f
        drawCircle(
            color = targetColor,
            radius = coreRadius,
            center = Offset(centerX, centerY)
        )
        // 核心純白高光靶心焦點 (Bullseye Center Dot)
        drawCircle(
            color = Color.White,
            radius = coreRadius * 0.55f,
            center = Offset(centerX, centerY)
        )
        // 核心十字瞄準線 (Center Crosshair)
        val crossLen = coreRadius * 0.85f
        drawLine(color = Color(0xFF0F172A), start = Offset(centerX - crossLen, centerY), end = Offset(centerX + crossLen, centerY), strokeWidth = 1.5f)
        drawLine(color = Color(0xFF0F172A), start = Offset(centerX, centerY - crossLen), end = Offset(centerX, centerY + crossLen), strokeWidth = 1.5f)
    }
}
