package com.example.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundManager
import com.example.data.model.AppLanguage
import com.example.data.model.GameDifficulty
import com.example.data.model.Localization
import com.example.game.cube.CellPos
import com.example.game.cube.CubeFace
import com.example.game.cube.CubePuzzleGenerator
import com.example.game.cube.GlassTheme
import com.example.game.cube.PlacedPiece
import com.example.game.cube.PuzzlePiece
import com.example.game.cube.getCell3DVertices
import com.example.game.cube.isPointInQuad
import com.example.game.cube.projectPoint
import com.example.game.cube.rotatePoint
import com.example.ui.components.AppBackground
import com.example.ui.theme.AppThemeStyle
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun GlassPuzzleCubeScreen(
    difficulty: GameDifficulty,
    language: AppLanguage,
    appTheme: AppThemeStyle,
    onBackClick: () -> Unit,
    onSaveScore: (Long) -> Unit,
    onLeaderboardClick: () -> Unit,
    onGameCompleted: () -> Unit = {},
    onGameInterrupted: () -> Unit = {}
) {
    val context = LocalContext.current

    // 震動輔助
    fun triggerVibrate(millis: Long = 25L) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                v?.vibrate(millis)
            }
        } catch (_: Exception) {}
    }

    // 玻璃主題色彩
    var currentGlassTheme by remember { mutableStateOf(GlassTheme.SKY_BLUE) }
    var showThemeMenu by remember { mutableStateOf(false) }

    // 視角旋轉狀態 (等角正交投影基準角度: rotX = 25f, rotY = 35f)
    var rotX by remember { mutableFloatStateOf(25f) }
    var rotY by remember { mutableFloatStateOf(35f) }

    // 遊戲拼塊資料狀態
    val inventoryPieces = remember { mutableStateListOf<PuzzlePiece>() }
    val placedPieces = remember { mutableStateListOf<PlacedPiece>() }

    // 計時與狀態
    var elapsedTimeMillis by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(true) }
    var isCompleted by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    // 遊戲初始化
    fun initGame() {
        inventoryPieces.clear()
        placedPieces.clear()
        val generated = CubePuzzleGenerator.generatePuzzle(difficulty)
        inventoryPieces.addAll(generated)
        elapsedTimeMillis = 0L
        isPlaying = true
        isCompleted = false
    }

    LaunchedEffect(difficulty) {
        initGame()
    }

    // 計時器循環
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(100L)
            elapsedTimeMillis += 100L
        }
    }

    // 拖曳狀態
    var draggingPiece by remember { mutableStateOf<PuzzlePiece?>(null) }
    var dragTouchPosition by remember { mutableStateOf(Offset.Zero) }

    // 魔方在螢幕全域中的 Bounds (用於計算拖曳時的落點判定)
    var cubeCanvasBounds by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }

    // 計算當前已填滿的格子總數 (目標: 54)
    val filledCellCount = placedPieces.sumOf { it.piece.size }

    // 檢查放置合法性輔助函式 (不跨面且不重疊)
    fun canPlacePiece(face: CubeFace, originRow: Int, originCol: Int, piece: PuzzlePiece): Boolean {
        // 1. 檢查是否超出 3x3 網格邊界 (不跨面)
        for (cell in piece.cells) {
            val r = originRow + cell.row
            val c = originCol + cell.col
            if (r !in 0..2 || c !in 0..2) return false
        }
        // 2. 檢查是否與該面既有拼塊重疊
        val facePlaced = placedPieces.filter { it.face == face }
        val occupiedCells = facePlaced.flatMap { it.occupiedCells }.toSet()
        for (cell in piece.cells) {
            val targetPos = CellPos(originRow + cell.row, originCol + cell.col)
            if (occupiedCells.contains(targetPos)) return false
        }
        return true
    }

    // 計算當前拖曳時的吸附候選 (包含合法綠色吸附與非法紅色警告陰影)
    val currentSnapCandidate = if (draggingPiece != null && cubeCanvasBounds.width > 0) {
        val localTouch = dragTouchPosition - cubeCanvasBounds.topLeft
        findHoverSnapCandidate(
            localTouch = localTouch,
            canvasBounds = cubeCanvasBounds,
            rotX = rotX,
            rotY = rotY,
            piece = draggingPiece!!,
            canPlace = ::canPlacePiece
        )
    } else null

    var lastHoverWasValid by remember { mutableStateOf(false) }
    LaunchedEffect(currentSnapCandidate?.isValid) {
        if (currentSnapCandidate?.isValid == true && !lastHoverWasValid) {
            triggerVibrate(15L) // 磁吸微震動 (Haptic snap feedback)
        }
        lastHoverWasValid = currentSnapCandidate?.isValid == true
    }

    // 放置拼塊至魔方
    fun placePieceOnCube(face: CubeFace, originRow: Int, originCol: Int, piece: PuzzlePiece) {
        placedPieces.add(
            PlacedPiece(
                pieceId = piece.id,
                face = face,
                originRow = originRow,
                originCol = originCol,
                piece = piece
            )
        )
        inventoryPieces.removeAll { it.id == piece.id }
        SoundManager.playSwitchSnap()
        triggerVibrate(40L)

        // 檢查 54 格是否全部填滿過關
        if (placedPieces.sumOf { it.piece.size } == 54) {
            isPlaying = false
            isCompleted = true
            SoundManager.playWin()
            triggerVibrate(100L)
            onSaveScore(elapsedTimeMillis)
            onGameCompleted()
        }
    }

    // 釋放拖曳放置處理 (使用放手當下即時全域座標直接判定，零時延且避免重組閉包失效)
    fun handleDropPiece(dropTouchInRoot: Offset) {
        val piece = draggingPiece
        if (piece != null && cubeCanvasBounds.width > 0) {
            val localTouch = dropTouchInRoot - cubeCanvasBounds.topLeft
            val candidate = findHoverSnapCandidate(
                localTouch = localTouch,
                canvasBounds = cubeCanvasBounds,
                rotX = rotX,
                rotY = rotY,
                piece = piece,
                canPlace = ::canPlacePiece
            )
            if (candidate != null && candidate.isValid) {
                placePieceOnCube(
                    face = candidate.face,
                    originRow = candidate.originRow,
                    originCol = candidate.originCol,
                    piece = piece
                )
            } else {
                // 無效放置反饋：輕微雙震動提點
                triggerVibrate(25L)
            }
        }
        draggingPiece = null
    }

    // 旋轉拖曳中之拼塊 (支援空中快速旋轉 90°)
    fun rotateDraggingPiece() {
        val current = draggingPiece ?: return
        val rotated = current.rotatedClockwise()
        draggingPiece = rotated
        val idx = inventoryPieces.indexOfFirst { it.id == current.id }
        if (idx >= 0) {
            inventoryPieces[idx] = rotated
        }
        SoundManager.playClick()
        triggerVibrate(20L)
    }

    // 從魔方取下拼塊 (長按 0.5 秒)
    fun removePieceFromCube(placed: PlacedPiece) {
        placedPieces.remove(placed)
        inventoryPieces.add(0, placed.piece) // 歸還待拼庫首位
        SoundManager.playClick()
        triggerVibrate(40L)
    }

    AppBackground(themeStyle = appTheme) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // 頂部導航與工具列
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            SoundManager.playClick()
                            if (placedPieces.isNotEmpty() && !isCompleted) {
                                onGameInterrupted()
                            }
                            onBackClick()
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = Localization.getString("glass_cube_title", language),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                            val diffKey = when (difficulty) {
                                GameDifficulty.BEGINNER -> "diff_name_beginner"
                                GameDifficulty.INTERMEDIATE -> "diff_name_intermediate"
                                else -> "diff_name_hard"
                            }
                            Text(
                                text = Localization.getString(diffKey, language),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 水晶色澤切換按鈕
                        IconButton(onClick = {
                            val nextIndex = (GlassTheme.entries.indexOf(currentGlassTheme) + 1) % GlassTheme.entries.size
                            currentGlassTheme = GlassTheme.entries[nextIndex]
                            SoundManager.playClick()
                        }) {
                            Icon(
                                imageVector = Icons.Default.ColorLens,
                                contentDescription = Localization.getString("glass_cube_theme", language),
                                tint = currentGlassTheme.baseColor
                            )
                        }

                        // 重置按鈕
                        IconButton(onClick = {
                            SoundManager.playClick()
                            showResetDialog = true
                        }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
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

                Spacer(modifier = Modifier.height(8.dp))

                // 計時與進度狀態卡片
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val seconds = (elapsedTimeMillis / 1000) % 60
                            val minutes = (elapsedTimeMillis / 1000) / 60
                            val timeStr = String.format("%02d:%02d", minutes, seconds)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "⏱ $timeStr",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }

                            Text(
                                text = Localization.getString("glass_cube_progress", language, filledCellCount),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { (filledCellCount.toFloat() / 54f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = currentGlassTheme.baseColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 中央 3D 掌上型玻璃魔方視圖
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.12f))
                        .onGloballyPositioned { coordinates ->
                            cubeCanvasBounds = coordinates.boundsInRoot()
                        }
                        .pointerInput(Unit) {
                            // 空白處單指滑動：360度自由旋轉
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                rotY = (rotY + dragAmount.x * 0.55f) % 360f
                                rotX = (rotX - dragAmount.y * 0.55f).coerceIn(-85f, 85f)
                            }
                        }
                ) {
                    Cube3DCanvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(placedPieces.size, rotX, rotY) {
                                // 長按已放置方塊 0.5 秒卸下
                                detectTapGestures(
                                    onLongPress = { touchOffset ->
                                        // 命中測試：尋找被點擊的面與格子
                                        val hit = findHitCell(
                                            touchOffset = touchOffset,
                                            canvasSize = size,
                                            rotX = rotX,
                                            rotY = rotY
                                        )
                                        if (hit != null) {
                                            val (face, pos) = hit
                                            val placed = placedPieces.find {
                                                it.face == face && it.occupiedCells.contains(pos)
                                            }
                                            if (placed != null) {
                                                removePieceFromCube(placed)
                                            }
                                        }
                                    }
                                )
                            },
                        rotX = rotX,
                        rotY = rotY,
                        glassTheme = currentGlassTheme,
                        placedPieces = placedPieces,
                        hoverCandidate = currentSnapCandidate
                    )

                    // 旋轉與操作小提示標籤
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "🔄 " + Localization.getString("glass_cube_remove_hint", language),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 底欄待拼方塊盤 (Inventory Tray)
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = Localization.getString("glass_cube_tray_title", language) + " (${inventoryPieces.size})",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )

                            Text(
                                text = Localization.getString("glass_cube_rotate_hint", language),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // 水平可滑動的拼塊展示列
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            inventoryPieces.forEach { piece ->
                                IsometricPieceCard(
                                    piece = piece,
                                    onTapRotate = {
                                        val idx = inventoryPieces.indexOfFirst { it.id == piece.id }
                                        if (idx >= 0) {
                                            inventoryPieces[idx] = piece.rotatedClockwise()
                                            SoundManager.playClick()
                                            triggerVibrate(20L)
                                        }
                                    },
                                    onStartDrag = { rootPos ->
                                        draggingPiece = piece
                                        dragTouchPosition = rootPos
                                        triggerVibrate(30L)
                                    },
                                    onDrag = { rootPos ->
                                        dragTouchPosition = rootPos
                                    },
                                    onEndDrag = { rootPos ->
                                        handleDropPiece(rootPos)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // 拖曳中的立體水晶浮層 (Floating Dragging Piece)
            if (draggingPiece != null) {
                // 空中快速旋轉輔助提示膠囊按鈕 (點擊直接旋轉 90°)
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = (-168).dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { rotateDraggingPiece() }
                        .border(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.65f), RoundedCornerShape(16.dp)),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Rotate",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = Localization.getString("glass_cube_rotate_air_hint", language),
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (dragTouchPosition.x - 60.dp.toPx()).roundToInt(),
                                (dragTouchPosition.y - 60.dp.toPx()).roundToInt()
                            )
                        }
                        .size(120.dp)
                        .clickable { rotateDraggingPiece() }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawIsometricPiece(
                            piece = draggingPiece!!,
                            cellSize = 24.dp.toPx(),
                            center = Offset(size.width / 2f, size.height / 2f),
                            alpha = 0.95f
                        )
                    }
                }
            }

            // 通關慶祝彈窗
            if (isCompleted) {
                AlertDialog(
                    onDismissRequest = { /* 不自動關閉 */ },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(48.dp)
                        )
                    },
                    title = {
                        Text(
                            text = Localization.getString("glass_cube_completed_title", language),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = Localization.getString("glass_cube_completed_desc", language),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            val seconds = (elapsedTimeMillis / 1000) % 60
                            val minutes = (elapsedTimeMillis / 1000) / 60
                            val timeStr = String.format("%02d:%02d", minutes, seconds)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = Localization.getString("glass_cube_time", language) + ": ",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = timeStr,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // 操作按鈕一：[再玩一局]
                            Button(
                                onClick = {
                                    SoundManager.playClick()
                                    initGame()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = Localization.getString("glass_cube_play_again", language),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 操作按鈕二：[排行榜]
                            OutlinedButton(
                                onClick = {
                                    SoundManager.playClick()
                                    onLeaderboardClick()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = Localization.getString("leaderboard_title", language),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 操作按鈕三：[返回選單]
                            TextButton(
                                onClick = {
                                    SoundManager.playClick()
                                    onBackClick()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = Localization.getString("back_to_menu", language),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    },
                    confirmButton = { },
                    dismissButton = { }
                )
            }

            // 重置確認對話框
            if (showResetDialog) {
                AlertDialog(
                    onDismissRequest = { showResetDialog = false },
                    title = { Text(Localization.getString("glass_cube_reset_confirm_title", language)) },
                    text = { Text(Localization.getString("glass_cube_reset_confirm_desc", language)) },
                    confirmButton = {
                        Button(
                            onClick = {
                                SoundManager.playClick()
                                if (placedPieces.isNotEmpty() && !isCompleted) {
                                    onGameInterrupted()
                                }
                                showResetDialog = false
                                initGame()
                            }
                        ) {
                            Text(Localization.getString("confirm", language))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showResetDialog = false }) {
                            Text(Localization.getString("cancel", language))
                        }
                    }
                )
            }
        }
    }
}

/**
 * 候選吸附資訊 (包含是否合法可放置 isValid)
 */
data class HoverSnapCandidate(
    val face: CubeFace,
    val originRow: Int,
    val originCol: Int,
    val targetCells: List<CellPos>,
    val isValid: Boolean
)

/**
 * 尋找當前拖曳點在魔方正交投影下的最佳吸附候選
 */
fun findHoverSnapCandidate(
    localTouch: Offset,
    canvasBounds: androidx.compose.ui.geometry.Rect,
    rotX: Float,
    rotY: Float,
    piece: PuzzlePiece,
    canPlace: (CubeFace, Int, Int, PuzzlePiece) -> Boolean
): HoverSnapCandidate? {
    val canvasCenter = Offset(canvasBounds.width / 2f, canvasBounds.height / 2f)
    val cubeRadius = (canvasBounds.width.coerceAtMost(canvasBounds.height) * 0.28f)

    // 依面向正面深度 (normalRot[2]) 由大到小排序，優先判定正對觀察者的面
    val visibleFaces = CubeFace.entries
        .map { face -> Pair(face, rotatePoint(face.normalX, face.normalY, face.normalZ, rotX, rotY)) }
        .filter { it.second[2] > 0.05f }
        .sortedByDescending { it.second[2] }

    // 第一階段：精確網格四邊形命中測試
    for ((face, _) in visibleFaces) {
        for (r in 0..2) {
            for (c in 0..2) {
                val vertices = getCell3DVertices(face, r, c)
                val proj0 = projectPoint(vertices[0][0], vertices[0][1], vertices[0][2], rotX, rotY, canvasCenter, cubeRadius).first
                val proj1 = projectPoint(vertices[1][0], vertices[1][1], vertices[1][2], rotX, rotY, canvasCenter, cubeRadius).first
                val proj2 = projectPoint(vertices[2][0], vertices[2][1], vertices[2][2], rotX, rotY, canvasCenter, cubeRadius).first
                val proj3 = projectPoint(vertices[3][0], vertices[3][1], vertices[3][2], rotX, rotY, canvasCenter, cubeRadius).first

                if (isPointInQuad(localTouch, proj0, proj1, proj2, proj3)) {
                    // 找出該面所有能容納此拼塊的合法原點
                    val validOrigins = mutableListOf<Pair<Int, Int>>()
                    for (orR in 0..(3 - piece.height)) {
                        for (orC in 0..(3 - piece.width)) {
                            if (canPlace(face, orR, orC, piece)) {
                                validOrigins.add(Pair(orR, orC))
                            }
                        }
                    }

                    if (validOrigins.isNotEmpty()) {
                        // 智慧挑選：選擇 2D 幾何投影中心距離觸控點最近的合法原點
                        val bestOrigin = validOrigins.minByOrNull { (orR, orC) ->
                            var sumX = 0f
                            var sumY = 0f
                            for (cell in piece.cells) {
                                val cr = orR + cell.row
                                val cc = orC + cell.col
                                val v = getCell3DVertices(face, cr, cc)
                                val cx = (v[0][0] + v[1][0] + v[2][0] + v[3][0]) / 4f
                                val cy = (v[0][1] + v[1][1] + v[2][1] + v[3][1]) / 4f
                                val cz = (v[0][2] + v[1][2] + v[2][2] + v[3][2]) / 4f
                                val p = projectPoint(cx, cy, cz, rotX, rotY, canvasCenter, cubeRadius).first
                                sumX += p.x
                                sumY += p.y
                            }
                            val avgX = sumX / piece.size
                            val avgY = sumY / piece.size
                            val dx = avgX - localTouch.x
                            val dy = avgY - localTouch.y
                            dx * dx + dy * dy
                        }

                        if (bestOrigin != null) {
                            return HoverSnapCandidate(
                                face = face,
                                originRow = bestOrigin.first,
                                originCol = bestOrigin.second,
                                targetCells = piece.cells.map { CellPos(bestOrigin.first + it.row, bestOrigin.second + it.col) },
                                isValid = true
                            )
                        }
                    }

                    // 該面無任何合法位置 (形狀不符、超出邊界或被佔用) -> 顯示紅色警告陰影
                    val centerR = (piece.cells.map { it.row }.average()).roundToInt()
                    val centerC = (piece.cells.map { it.col }.average()).roundToInt()
                    val fallbackR = r - centerR
                    val fallbackC = c - centerC
                    return HoverSnapCandidate(
                        face = face,
                        originRow = fallbackR,
                        originCol = fallbackC,
                        targetCells = piece.cells.map { CellPos(fallbackR + it.row, fallbackC + it.col) },
                        isValid = false
                    )
                }
            }
        }
    }

    // 第二階段：邊緣微外側寬容磁吸 (容許手指在魔方外圍 40px 內微幅偏差)
    var closestCell: Triple<CubeFace, Int, Int>? = null
    var minDistanceSq = Float.MAX_VALUE
    val snapTolerancePx = 40f

    for ((face, _) in visibleFaces) {
        for (r in 0..2) {
            for (c in 0..2) {
                val vertices = getCell3DVertices(face, r, c)
                val cx = (vertices[0][0] + vertices[1][0] + vertices[2][0] + vertices[3][0]) / 4f
                val cy = (vertices[0][1] + vertices[1][1] + vertices[2][1] + vertices[3][1]) / 4f
                val cz = (vertices[0][2] + vertices[1][2] + vertices[2][2] + vertices[3][2]) / 4f
                val center2D = projectPoint(cx, cy, cz, rotX, rotY, canvasCenter, cubeRadius).first
                val dx = center2D.x - localTouch.x
                val dy = center2D.y - localTouch.y
                val distSq = dx * dx + dy * dy
                if (distSq < minDistanceSq && distSq <= (cubeRadius * 0.45f + snapTolerancePx) * (cubeRadius * 0.45f + snapTolerancePx)) {
                    minDistanceSq = distSq
                    closestCell = Triple(face, r, c)
                }
            }
        }
    }

    if (closestCell != null) {
        val (face, r, c) = closestCell
        val validOrigins = mutableListOf<Pair<Int, Int>>()
        for (orR in 0..(3 - piece.height)) {
            for (orC in 0..(3 - piece.width)) {
                if (canPlace(face, orR, orC, piece)) {
                    validOrigins.add(Pair(orR, orC))
                }
            }
        }
        if (validOrigins.isNotEmpty()) {
            val bestOrigin = validOrigins.minByOrNull { (orR, orC) ->
                var sumX = 0f
                var sumY = 0f
                for (cell in piece.cells) {
                    val cr = orR + cell.row
                    val cc = orC + cell.col
                    val v = getCell3DVertices(face, cr, cc)
                    val cx = (v[0][0] + v[1][0] + v[2][0] + v[3][0]) / 4f
                    val cy = (v[0][1] + v[1][1] + v[2][1] + v[3][1]) / 4f
                    val cz = (v[0][2] + v[1][2] + v[2][2] + v[3][2]) / 4f
                    val p = projectPoint(cx, cy, cz, rotX, rotY, canvasCenter, cubeRadius).first
                    sumX += p.x
                    sumY += p.y
                }
                val avgX = sumX / piece.size
                val avgY = sumY / piece.size
                val dx = avgX - localTouch.x
                val dy = avgY - localTouch.y
                dx * dx + dy * dy
            }
            if (bestOrigin != null) {
                return HoverSnapCandidate(
                    face = face,
                    originRow = bestOrigin.first,
                    originCol = bestOrigin.second,
                    targetCells = piece.cells.map { CellPos(bestOrigin.first + it.row, bestOrigin.second + it.col) },
                    isValid = true
                )
            }
        }
    }

    return null
}

/**
 * 尋找點擊命中的正面網格
 */
fun findHitCell(
    touchOffset: Offset,
    canvasSize: androidx.compose.ui.unit.IntSize,
    rotX: Float,
    rotY: Float
): Pair<CubeFace, CellPos>? {
    val canvasCenter = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
    val cubeRadius = (canvasSize.width.coerceAtMost(canvasSize.height) * 0.28f)

    for (face in CubeFace.entries) {
        val normalRot = rotatePoint(face.normalX, face.normalY, face.normalZ, rotX, rotY)
        if (normalRot[2] <= 0.15f) continue

        for (r in 0..2) {
            for (c in 0..2) {
                val vertices = getCell3DVertices(face, r, c)
                val p0 = projectPoint(vertices[0][0], vertices[0][1], vertices[0][2], rotX, rotY, canvasCenter, cubeRadius).first
                val p1 = projectPoint(vertices[1][0], vertices[1][1], vertices[1][2], rotX, rotY, canvasCenter, cubeRadius).first
                val p2 = projectPoint(vertices[2][0], vertices[2][1], vertices[2][2], rotX, rotY, canvasCenter, cubeRadius).first
                val p3 = projectPoint(vertices[3][0], vertices[3][1], vertices[3][2], rotX, rotY, canvasCenter, cubeRadius).first

                if (isPointInQuad(touchOffset, p0, p1, p2, p3)) {
                    return Pair(face, CellPos(r, c))
                }
            }
        }
    }
    return null
}

/**
 * 3D 掌上型玻璃魔方渲染 Canvas
 */
@Composable
fun Cube3DCanvas(
    modifier: Modifier,
    rotX: Float,
    rotY: Float,
    glassTheme: GlassTheme,
    placedPieces: List<PlacedPiece>,
    hoverCandidate: HoverSnapCandidate?
) {
    Canvas(modifier = modifier) {
        val canvasCenter = Offset(size.width / 2f, size.height / 2f)
        val cubeRadius = (size.width.coerceAtMost(size.height) * 0.28f)

        // 12 條立方體主稜線頂點定義 (3D 空間半徑 1.5)
        val edgePairs = listOf(
            // 平行於 X 軸
            Pair(floatArrayOf(-1.5f, -1.5f, -1.5f), floatArrayOf(1.5f, -1.5f, -1.5f)),
            Pair(floatArrayOf(-1.5f, -1.5f,  1.5f), floatArrayOf(1.5f, -1.5f,  1.5f)),
            Pair(floatArrayOf(-1.5f,  1.5f, -1.5f), floatArrayOf(1.5f,  1.5f, -1.5f)),
            Pair(floatArrayOf(-1.5f,  1.5f,  1.5f), floatArrayOf(1.5f,  1.5f,  1.5f)),
            // 平行於 Y 軸
            Pair(floatArrayOf(-1.5f, -1.5f, -1.5f), floatArrayOf(-1.5f, 1.5f, -1.5f)),
            Pair(floatArrayOf( 1.5f, -1.5f, -1.5f), floatArrayOf( 1.5f, 1.5f, -1.5f)),
            Pair(floatArrayOf(-1.5f, -1.5f,  1.5f), floatArrayOf(-1.5f, 1.5f,  1.5f)),
            Pair(floatArrayOf( 1.5f, -1.5f,  1.5f), floatArrayOf( 1.5f, 1.5f,  1.5f)),
            // 平行於 Z 軸
            Pair(floatArrayOf(-1.5f, -1.5f, -1.5f), floatArrayOf(-1.5f, -1.5f, 1.5f)),
            Pair(floatArrayOf( 1.5f, -1.5f, -1.5f), floatArrayOf( 1.5f, -1.5f, 1.5f)),
            Pair(floatArrayOf(-1.5f,  1.5f, -1.5f), floatArrayOf(-1.5f,  1.5f, 1.5f)),
            Pair(floatArrayOf( 1.5f,  1.5f, -1.5f), floatArrayOf( 1.5f,  1.5f, 1.5f))
        )

        val projectedEdges = edgePairs.map { (pA, pB) ->
            val projA = projectPoint(pA[0], pA[1], pA[2], rotX, rotY, canvasCenter, cubeRadius)
            val projB = projectPoint(pB[0], pB[1], pB[2], rotX, rotY, canvasCenter, cubeRadius)
            val midZ = (projA.second + projB.second) / 2f
            Triple(projA.first, projB.first, midZ)
        }

        // 計算 6 個面的法向量旋轉深度 Z
        val faceDepths = CubeFace.entries.map { face ->
            val normalRot = rotatePoint(face.normalX, face.normalY, face.normalZ, rotX, rotY)
            Triple(face, normalRot[2], normalRot)
        }.sortedBy { it.second } // 由深至淺排序 (Back-to-Front Painter's Algorithm)

        // 第一階段：繪製背面稜線與背面半透明網格
        for (edge in projectedEdges) {
            if (edge.third <= 0f) {
                drawLine(
                    color = Color(0x330F172A),
                    start = edge.first,
                    end = edge.second,
                    strokeWidth = 1.8f
                )
            }
        }

        for ((face, normalZ, _) in faceDepths) {
            if (normalZ <= 0f) {
                drawCubeFace(
                    face = face,
                    rotX = rotX,
                    rotY = rotY,
                    center = canvasCenter,
                    radius = cubeRadius,
                    isFront = false,
                    glassTheme = glassTheme,
                    placedPieces = placedPieces,
                    hoverCandidate = null
                )
            }
        }

        // 第二階段：繪製正面超清半透明玻璃、九宮格深色溝槽、立體水晶拼塊
        for ((face, normalZ, _) in faceDepths) {
            if (normalZ > 0f) {
                drawCubeFace(
                    face = face,
                    rotX = rotX,
                    rotY = rotY,
                    center = canvasCenter,
                    radius = cubeRadius,
                    isFront = true,
                    glassTheme = glassTheme,
                    placedPieces = placedPieces,
                    hoverCandidate = if (hoverCandidate?.face == face) hoverCandidate else null
                )
            }
        }

        // 第三階段：繪製正面立體主稜線骨架 (正面 12 主邊框加粗與雙重高光)
        for (edge in projectedEdges) {
            if (edge.third > 0f) {
                // 深色骨架外框
                drawLine(
                    color = Color(0xCC0F172A),
                    start = edge.first,
                    end = edge.second,
                    strokeWidth = 3.6f
                )
                // 晶體倒角微高光線
                drawLine(
                    color = glassTheme.edgeHighlightColor.copy(alpha = 0.85f),
                    start = edge.first,
                    end = edge.second,
                    strokeWidth = 1.5f
                )
            }
        }
    }
}

/**
 * 繪製魔方單一面
 */
fun DrawScope.drawCubeFace(
    face: CubeFace,
    rotX: Float,
    rotY: Float,
    center: Offset,
    radius: Float,
    isFront: Boolean,
    glassTheme: GlassTheme,
    placedPieces: List<PlacedPiece>,
    hoverCandidate: HoverSnapCandidate?
) {
    val facePieces = placedPieces.filter { it.face == face }
    val cellOccupantMap = mutableMapOf<CellPos, PlacedPiece>()
    for (pp in facePieces) {
        for (c in pp.occupiedCells) {
            cellOccupantMap[c] = pp
        }
    }

    // 遍歷 3x3 網格
    for (r in 0..2) {
        for (c in 0..2) {
            val vertices = getCell3DVertices(face, r, c)
            val p0 = projectPoint(vertices[0][0], vertices[0][1], vertices[0][2], rotX, rotY, center, radius).first
            val p1 = projectPoint(vertices[1][0], vertices[1][1], vertices[1][2], rotX, rotY, center, radius).first
            val p2 = projectPoint(vertices[2][0], vertices[2][1], vertices[2][2], rotX, rotY, center, radius).first
            val p3 = projectPoint(vertices[3][0], vertices[3][1], vertices[3][2], rotX, rotY, center, radius).first

            val quadPath = Path().apply {
                moveTo(p0.x, p0.y)
                lineTo(p1.x, p1.y)
                lineTo(p2.x, p2.y)
                lineTo(p3.x, p3.y)
                close()
            }

            val curPos = CellPos(r, c)
            val occupant = cellOccupantMap[curPos]

            // 1. 底層九宮格深色接縫溝槽 (深色暗槽，解決在亮色底色上對比不足的問題)
            val grooveColor = if (isFront) {
                Color(0x990F172A)
            } else {
                Color(0x2B0F172A)
            }
            drawPath(quadPath, color = grooveColor, style = Stroke(width = if (isFront) 3.2f else 1.5f))

            if (occupant != null) {
                // 2. 繪製該格上的 3D 浮雕立體水晶方塊
                drawCellCrystal(
                    vertices = vertices,
                    face = face,
                    isFront = isFront,
                    color = occupant.piece.color,
                    rotX = rotX,
                    rotY = rotY,
                    center = center,
                    radius = radius
                )
            } else {
                // 3. 空格子：向中心微縮 7%，露出一顆顆獨立魔方水晶方塊 (Cubie Tile) 顆粒感
                val centerPt = Offset((p0.x + p1.x + p2.x + p3.x) / 4f, (p0.y + p1.y + p2.y + p3.y) / 4f)
                val inset0 = centerPt + (p0 - centerPt) * 0.93f
                val inset1 = centerPt + (p1 - centerPt) * 0.93f
                val inset2 = centerPt + (p2 - centerPt) * 0.93f
                val inset3 = centerPt + (p3 - centerPt) * 0.93f

                val insetPath = Path().apply {
                    moveTo(inset0.x, inset0.y)
                    lineTo(inset1.x, inset1.y)
                    lineTo(inset2.x, inset2.y)
                    lineTo(inset3.x, inset3.y)
                    close()
                }

                // 超清通透水晶玻璃底色 (極致透明，消除大藍板油漆感)
                val fillColor = if (isFront) {
                    glassTheme.baseColor.copy(alpha = 0.08f)
                } else {
                    glassTheme.baseColor.copy(alpha = 0.03f)
                }
                drawPath(insetPath, color = fillColor, style = Fill)

                // 晶體微倒角高光邊線
                val highlightColor = if (isFront) {
                    Color.White.copy(alpha = 0.55f)
                } else {
                    Color.White.copy(alpha = 0.18f)
                }
                drawPath(insetPath, color = highlightColor, style = Stroke(width = if (isFront) 1.2f else 0.8f))
            }

            // 4. 拖曳懸浮吸附預覽高光 (合法: 翡翠綠螢光；不合法: 紅色警告陰影)
            if (hoverCandidate != null && hoverCandidate.face == face && hoverCandidate.targetCells.contains(curPos)) {
                val centerPt = Offset((p0.x + p1.x + p2.x + p3.x) / 4f, (p0.y + p1.y + p2.y + p3.y) / 4f)
                val inset0 = centerPt + (p0 - centerPt) * 0.93f
                val inset1 = centerPt + (p1 - centerPt) * 0.93f
                val inset2 = centerPt + (p2 - centerPt) * 0.93f
                val inset3 = centerPt + (p3 - centerPt) * 0.93f
                val snapPath = Path().apply {
                    moveTo(inset0.x, inset0.y)
                    lineTo(inset1.x, inset1.y)
                    lineTo(inset2.x, inset2.y)
                    lineTo(inset3.x, inset3.y)
                    close()
                }
                if (hoverCandidate.isValid) {
                    drawPath(
                        snapPath,
                        color = Color(0xFF10B981).copy(alpha = 0.55f),
                        style = Fill
                    )
                    drawPath(
                        snapPath,
                        color = Color(0xFF6EE7B7),
                        style = Stroke(width = 3.6f)
                    )
                } else {
                    drawPath(
                        snapPath,
                        color = Color(0xFFEF4444).copy(alpha = 0.45f),
                        style = Fill
                    )
                    drawPath(
                        snapPath,
                        color = Color(0xFFF87171),
                        style = Stroke(width = 3.2f)
                    )
                }
            }
        }
    }

    // 5. 若為不合法放置，將超出 3x3 網格邊界的格位以半透明紅色虛影投射繪出
    if (isFront && hoverCandidate != null && hoverCandidate.face == face && !hoverCandidate.isValid) {
        val oobCells = hoverCandidate.targetCells.filter { it.row !in 0..2 || it.col !in 0..2 }
        for (oob in oobCells) {
            val vertices = getCell3DVertices(face, oob.row, oob.col)
            val p0 = projectPoint(vertices[0][0], vertices[0][1], vertices[0][2], rotX, rotY, center, radius).first
            val p1 = projectPoint(vertices[1][0], vertices[1][1], vertices[1][2], rotX, rotY, center, radius).first
            val p2 = projectPoint(vertices[2][0], vertices[2][1], vertices[2][2], rotX, rotY, center, radius).first
            val p3 = projectPoint(vertices[3][0], vertices[3][1], vertices[3][2], rotX, rotY, center, radius).first

            val centerPt = Offset((p0.x + p1.x + p2.x + p3.x) / 4f, (p0.y + p1.y + p2.y + p3.y) / 4f)
            val inset0 = centerPt + (p0 - centerPt) * 0.93f
            val inset1 = centerPt + (p1 - centerPt) * 0.93f
            val inset2 = centerPt + (p2 - centerPt) * 0.93f
            val inset3 = centerPt + (p3 - centerPt) * 0.93f
            val oobPath = Path().apply {
                moveTo(inset0.x, inset0.y)
                lineTo(inset1.x, inset1.y)
                lineTo(inset2.x, inset2.y)
                lineTo(inset3.x, inset3.y)
                close()
            }
            drawPath(oobPath, color = Color(0xFFEF4444).copy(alpha = 0.28f), style = Fill)
            drawPath(oobPath, color = Color(0xFFF87171).copy(alpha = 0.75f), style = Stroke(width = 2.0f))
        }
    }
}

/**
 * 繪製單個網格上的 3D 立體水晶方塊 (凸出玻璃面的真實 3D 浮雕與光影，告別平面油漆感)
 */
fun DrawScope.drawCellCrystal(
    vertices: Array<FloatArray>,
    face: CubeFace,
    isFront: Boolean,
    color: Color,
    rotX: Float,
    rotY: Float,
    center: Offset,
    radius: Float
) {
    // 1. 計算該格 3D 幾何中心
    val cX = (vertices[0][0] + vertices[1][0] + vertices[2][0] + vertices[3][0]) / 4f
    val cY = (vertices[0][1] + vertices[1][1] + vertices[2][1] + vertices[3][1]) / 4f
    val cZ = (vertices[0][2] + vertices[1][2] + vertices[2][2] + vertices[3][2]) / 4f

    // 2. 3D 空間微內縮 6% (相鄰方塊間保留清晰深色凹槽接縫，不互相黏結)
    val insetRatio = 0.94f
    val b0 = floatArrayOf(cX + (vertices[0][0] - cX) * insetRatio, cY + (vertices[0][1] - cY) * insetRatio, cZ + (vertices[0][2] - cZ) * insetRatio)
    val b1 = floatArrayOf(cX + (vertices[1][0] - cX) * insetRatio, cY + (vertices[1][1] - cY) * insetRatio, cZ + (vertices[1][2] - cZ) * insetRatio)
    val b2 = floatArrayOf(cX + (vertices[2][0] - cX) * insetRatio, cY + (vertices[2][1] - cY) * insetRatio, cZ + (vertices[2][2] - cZ) * insetRatio)
    val b3 = floatArrayOf(cX + (vertices[3][0] - cX) * insetRatio, cY + (vertices[3][1] - cY) * insetRatio, cZ + (vertices[3][2] - cZ) * insetRatio)

    // 3. 沿法向量向外凸出厚度 (浮雕立體厚度，等比拉伸)
    val extrudeDist = if (isFront) 0.28f else 0.12f
    val nx = face.normalX * extrudeDist
    val ny = face.normalY * extrudeDist
    val nz = face.normalZ * extrudeDist

    val t0 = floatArrayOf(b0[0] + nx, b0[1] + ny, b0[2] + nz)
    val t1 = floatArrayOf(b1[0] + nx, b1[1] + ny, b1[2] + nz)
    val t2 = floatArrayOf(b2[0] + nx, b2[1] + ny, b2[2] + nz)
    val t3 = floatArrayOf(b3[0] + nx, b3[1] + ny, b3[2] + nz)

    // 4. 正交投影至 2D 螢幕
    val pb0 = projectPoint(b0[0], b0[1], b0[2], rotX, rotY, center, radius).first
    val pb1 = projectPoint(b1[0], b1[1], b1[2], rotX, rotY, center, radius).first
    val pb2 = projectPoint(b2[0], b2[1], b2[2], rotX, rotY, center, radius).first
    val pb3 = projectPoint(b3[0], b3[1], b3[2], rotX, rotY, center, radius).first

    val pt0 = projectPoint(t0[0], t0[1], t0[2], rotX, rotY, center, radius).first
    val pt1 = projectPoint(t1[0], t1[1], t1[2], rotX, rotY, center, radius).first
    val pt2 = projectPoint(t2[0], t2[1], t2[2], rotX, rotY, center, radius).first
    val pt3 = projectPoint(t3[0], t3[1], t3[2], rotX, rotY, center, radius).first

    // 5. 若在正面，動態繪製面向觀察者的 3D 凸起側面 (受光面與陰影面)
    if (isFront) {
        val sides = listOf(
            listOf(pb0, pb1, pt1, pt0),
            listOf(pb1, pb2, pt2, pt1),
            listOf(pb2, pb3, pt3, pt2),
            listOf(pb3, pb0, pt0, pt3)
        )

        for (side in sides) {
            val area = (side[1].x - side[0].x) * (side[2].y - side[0].y) - (side[1].y - side[0].y) * (side[2].x - side[0].x)
            if (area > 0f) {
                val sidePath = Path().apply {
                    moveTo(side[0].x, side[0].y)
                    lineTo(side[1].x, side[1].y)
                    lineTo(side[2].x, side[2].y)
                    lineTo(side[3].x, side[3].y)
                    close()
                }

                val midX = (side[0].x + side[1].x + side[2].x + side[3].x) / 4f
                val midY = (side[0].y + side[1].y + side[2].y + side[3].y) / 4f
                val topMidX = (pt0.x + pt1.x + pt2.x + pt3.x) / 4f
                val topMidY = (pt0.y + pt1.y + pt2.y + pt3.y) / 4f

                val isBacklit = (midX > topMidX || midY > topMidY)
                val shadowAlpha = if (isBacklit) 0.38f else 0.12f

                drawPath(sidePath, color = color.copy(alpha = 0.95f), style = Fill)
                drawPath(sidePath, color = Color.Black.copy(alpha = shadowAlpha), style = Fill)
                drawPath(sidePath, color = Color.Black.copy(alpha = 0.25f), style = Stroke(width = 0.8f))
            }
        }
    }

    // 6. 繪製頂面 (Top Face)
    val topPath = Path().apply {
        moveTo(pt0.x, pt0.y)
        lineTo(pt1.x, pt1.y)
        lineTo(pt2.x, pt2.y)
        lineTo(pt3.x, pt3.y)
        close()
    }

    val baseAlpha = if (isFront) 0.96f else 0.35f
    // 頂面主色填滿
    drawPath(topPath, color = color.copy(alpha = baseAlpha), style = Fill)

    if (isFront) {
        // 頂面覆蓋柔和高光微白層
        drawPath(topPath, color = Color.White.copy(alpha = 0.25f), style = Fill)
        // 晶體倒角微高光框
        drawPath(topPath, color = Color.White.copy(alpha = 0.82f), style = Stroke(width = 1.3f))
    } else {
        drawPath(topPath, color = Color.White.copy(alpha = 0.20f), style = Stroke(width = 0.8f))
    }
}

/**
 * 底欄立體積木拼塊卡片 (依拼塊尺寸自適應呈現飽滿的 1:1:1 立體正方體積木組)
 */
@Composable
fun IsometricPieceCard(
    piece: PuzzlePiece,
    onTapRotate: () -> Unit,
    onStartDrag: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onEndDrag: (Offset) -> Unit
) {
    val currentOnTapRotate = rememberUpdatedState(onTapRotate)
    val currentOnStartDrag = rememberUpdatedState(onStartDrag)
    val currentOnDrag = rememberUpdatedState(onDrag)
    val currentOnEndDrag = rememberUpdatedState(onEndDrag)

    var cardBounds by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }

    val maxDim = maxOf(piece.width, piece.height)
    val cellSize = when {
        maxDim <= 2 -> 24.dp
        maxDim == 3 -> 20.dp
        else -> 17.dp
    }

    Surface(
        modifier = Modifier
            .size(96.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.5.dp,
                color = piece.color.copy(alpha = 0.45f),
                shape = RoundedCornerShape(16.dp)
            )
            .onGloballyPositioned { coordinates ->
                if (coordinates.isAttached) {
                    cardBounds = coordinates.boundsInRoot()
                }
            }
            .pointerInput(piece.id) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var isDragging = false
                    val touchSlop = viewConfiguration.touchSlop
                    var totalDelta = Offset.Zero
                    var currentTouchPos = cardBounds.topLeft + down.position

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null) {
                            if (isDragging) {
                                currentOnEndDrag.value(currentTouchPos)
                            }
                            break
                        }

                        if (change.pressed) {
                            val dragAmount = change.positionChange()
                            totalDelta += dragAmount

                            if (!isDragging) {
                                val dx = kotlin.math.abs(totalDelta.x)
                                val dy = kotlin.math.abs(totalDelta.y)
                                val dist = totalDelta.getDistance()

                                if (dist > touchSlop) {
                                    if (dx > dy) {
                                        // 水平滑動為主：判定為瀏覽底盤方塊庫意圖，放行給外層 horizontalScroll
                                        break
                                    } else {
                                        // 垂直/向上滑動為主：判定為拿起方塊拖曳至魔方意圖，立即啟動拖曳
                                        isDragging = true
                                        change.consume()
                                        currentTouchPos = cardBounds.topLeft + change.position
                                        currentOnStartDrag.value(currentTouchPos)
                                    }
                                }
                            } else {
                                // 拖曳狀態已確立：全方向跟隨指針自由移動
                                change.consume()
                                currentTouchPos += dragAmount
                                currentOnDrag.value(currentTouchPos)
                            }
                        } else {
                            // 手指放開 (UP)
                            change.consume()
                            if (isDragging) {
                                currentOnEndDrag.value(currentTouchPos)
                            } else {
                                // 點擊 (Tap)：未超過 touchSlop，判定為順時針旋轉 90 度
                                currentOnTapRotate.value()
                            }
                            break
                        }
                    }
                }
            },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawIsometricPiece(
                piece = piece,
                cellSize = cellSize.toPx(),
                center = Offset(size.width / 2f, size.height / 2f),
                alpha = 1f
            )
        }
    }
}

/**
 * 繪製 3D 等角立體水晶積木 (真實 1:1:1 立體正方體魔方小方塊組件)
 */
fun DrawScope.drawIsometricPiece(
    piece: PuzzlePiece,
    cellSize: Float,
    center: Offset,
    alpha: Float = 1f
) {
    val h = cellSize * 0.50f // 俯仰壓縮比
    val depth = cellSize * 0.72f // 立體正方體垂直厚度 (沉穩飽滿的 1:1:1 正方體！)

    // 計算拼塊在 2D 上的邊界中心偏移量，使其完美居中 (考慮立體 depth)
    val minRow = piece.cells.minOf { it.row }
    val maxRow = piece.cells.maxOf { it.row }
    val minCol = piece.cells.minOf { it.col }
    val maxCol = piece.cells.maxOf { it.col }

    val pieceWidth = (maxCol - minCol + 1) * cellSize
    val pieceHeight = (maxRow - minRow + 1) * h + depth
    val originX = center.x - pieceWidth / 2f
    val originY = center.y - pieceHeight / 2f

    // 依 row 由小到大、col 由小到大排序繪製 (Painter's algorithm: 遠處先繪製，近處立體遮擋)
    val sortedCells = piece.cells.sortedWith(compareBy({ it.row }, { it.col }))

    for (c in sortedCells) {
        val cx = originX + (c.col - minCol) * cellSize + cellSize / 2f
        val cy = originY + (c.row - minRow) * h + h / 2f

        // 方塊頂面 4 個頂點 (菱形)，微縮 1.2px 形成方塊接縫
        val hw = cellSize * 0.46f
        val hh = h * 0.46f

        val topP0 = Offset(cx, cy - hh)
        val topP1 = Offset(cx + hw, cy)
        val topP2 = Offset(cx, cy + hh)
        val topP3 = Offset(cx - hw, cy)

        val topPath = Path().apply {
            moveTo(topP0.x, topP0.y)
            lineTo(topP1.x, topP1.y)
            lineTo(topP2.x, topP2.y)
            lineTo(topP3.x, topP3.y)
            close()
        }

        // 左前側面 (Front-Left Face)
        val flPath = Path().apply {
            moveTo(topP3.x, topP3.y)
            lineTo(topP2.x, topP2.y)
            lineTo(topP2.x, topP2.y + depth)
            lineTo(topP3.x, topP3.y + depth)
            close()
        }

        // 右前側面 (Front-Right Face)
        val frPath = Path().apply {
            moveTo(topP2.x, topP2.y)
            lineTo(topP1.x, topP1.y)
            lineTo(topP1.x, topP1.y + depth)
            lineTo(topP2.x, topP2.y + depth)
            close()
        }

        // 1. 繪製左側面 (受光面)
        drawPath(flPath, color = piece.color.copy(alpha = 0.92f * alpha), style = Fill)
        drawPath(flPath, color = Color.Black.copy(alpha = 0.12f * alpha), style = Fill)
        drawPath(flPath, color = Color.Black.copy(alpha = 0.25f * alpha), style = Stroke(width = 0.8f))

        // 2. 繪製右側面 (背光陰影面)
        drawPath(frPath, color = piece.color.copy(alpha = 0.92f * alpha), style = Fill)
        drawPath(frPath, color = Color.Black.copy(alpha = 0.38f * alpha), style = Fill)
        drawPath(frPath, color = Color.Black.copy(alpha = 0.30f * alpha), style = Stroke(width = 0.8f))

        // 3. 繪製頂面 (亮面)
        drawPath(topPath, color = piece.color.copy(alpha = 0.98f * alpha), style = Fill)
        drawPath(topPath, color = Color.White.copy(alpha = 0.28f * alpha), style = Fill)

        // 4. 頂面倒角晶瑩高光描邊
        drawPath(topPath, color = Color.White.copy(alpha = 0.82f * alpha), style = Stroke(width = 1.3f))
    }
}

