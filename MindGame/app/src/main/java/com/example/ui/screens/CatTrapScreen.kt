package com.example.ui.screens

import android.app.Activity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.audio.SoundManager
import com.example.data.model.AppLanguage
import com.example.ui.theme.AppThemeStyle
import com.example.data.model.Localization
import com.example.game.cattrap.CatBreed
import com.example.game.cattrap.CatEmotion
import com.example.game.cattrap.CatTrapGameStatus
import com.example.game.cattrap.CatTrapLevels
import com.example.game.cattrap.CatTrapMode
import com.example.game.cattrap.CatTrapTool
import com.example.game.cattrap.HexCellType
import com.example.game.cattrap.HexCoord
import com.example.ui.components.AppBackground
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 《喵影圍捕：別想逃！》次世代 2024-2026 頂級手遊視覺主畫面
 */
@Composable
fun CatTrapScreen(
    language: AppLanguage,
    appTheme: AppThemeStyle,
    status: CatTrapGameStatus,
    catPos: HexCoord,
    catEmotion: CatEmotion,
    grid: Map<HexCoord, HexCellType>,
    steps: Int,
    mode: CatTrapMode,
    selectedBreed: CatBreed,
    freezeCount: Int,
    doubleBlockCount: Int,
    radarCount: Int,
    isFreezeActive: Boolean,
    doubleBlockRemaining: Int,
    isRadarHighlit: Boolean,
    escapePath: List<HexCoord>,
    stars: Int,
    score: Int,
    showBriefingDialog: Boolean,
    showRewardAdDialog: Boolean,
    pendingRewardTool: CatTrapTool?,
    onBackClick: () -> Unit,
    onLeaderboardClick: () -> Unit,
    onCellClick: (HexCoord) -> Unit,
    onUseTool: (CatTrapTool) -> Unit,
    onUndoClick: () -> Unit,
    onSelectBreed: (CatBreed) -> Unit,
    onRestartClick: () -> Unit,
    onNextLevelClick: () -> Unit,
    onOpenBriefingClick: () -> Unit,
    onCloseBriefingClick: () -> Unit,
    onDismissRewardDialog: () -> Unit,
    onWatchAdForTool: (Activity) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()

    // 貓咪呼吸與搖尾動畫
    val infiniteTransition = rememberInfiniteTransition(label = "CatIdle")
    val tailWagAngle by infiniteTransition.animateFloat(
        initialValue = -18f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "TailWag"
    )
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Breathe"
    )

    AppBackground(themeStyle = appTheme) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 頂部全息 HUD 膠囊
            CatTrapTopHud(
                language = language,
                mode = mode,
                steps = steps,
                onBackClick = onBackClick,
                onHelpClick = onOpenBriefingClick,
                onLeaderboardClick = onLeaderboardClick
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 狀態與道具效果提示列
            CatTrapStatusPill(
                language = language,
                isFreezeActive = isFreezeActive,
                doubleBlockRemaining = doubleBlockRemaining,
                isRadarHighlit = isRadarHighlit
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 核心六角蜂巢戰場
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CatTrapHexBoard(
                    grid = grid,
                    catPos = catPos,
                    catEmotion = catEmotion,
                    catBreed = selectedBreed,
                    escapePath = escapePath,
                    isRadarHighlit = isRadarHighlit,
                    tailWagAngle = tailWagAngle,
                    breatheScale = breatheScale,
                    onCellClick = onCellClick
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 底部操作與道具工具箱
            CatTrapBottomControls(
                language = language,
                freezeCount = freezeCount,
                doubleBlockCount = doubleBlockCount,
                radarCount = radarCount,
                onUndoClick = onUndoClick,
                onUseTool = onUseTool
            )
        }
    }

    // 任務玩法簡報彈窗
    if (showBriefingDialog) {
        CatTrapBriefingDialog(
            language = language,
            mode = mode,
            selectedBreed = selectedBreed,
            onSelectBreed = onSelectBreed,
            onDismiss = onCloseBriefingClick
        )
    }

    // 獲勝 / 失敗結算雙彈窗
    if (status == CatTrapGameStatus.VICTORY) {
        CatTrapVictoryDialog(
            language = language,
            mode = mode,
            steps = steps,
            stars = stars,
            score = score,
            onPlayAgain = onRestartClick,
            onNextLevel = onNextLevelClick,
            onLeaderboard = onLeaderboardClick
        )
    } else if (status == CatTrapGameStatus.DEFEAT) {
        CatTrapDefeatDialog(
            language = language,
            mode = mode,
            steps = steps,
            onRetry = onRestartClick,
            onBackToMenu = onBackClick
        )
    }

    // AdMob 道具獎勵加值彈窗
    if (showRewardAdDialog && pendingRewardTool != null && activity != null) {
        CatTrapRewardDialog(
            language = language,
            tool = pendingRewardTool,
            onConfirmWatchAd = { onWatchAdForTool(activity) },
            onDismiss = onDismissRewardDialog
        )
    }
}

/**
 * 頂部全息電競 HUD 膠囊
 */
@Composable
private fun CatTrapTopHud(
    language: AppLanguage,
    mode: CatTrapMode,
    steps: Int,
    onBackClick: () -> Unit,
    onHelpClick: () -> Unit,
    onLeaderboardClick: () -> Unit
) {
    val titleText = when (mode) {
        is CatTrapMode.LevelMode -> "關卡 - ${mode.levelId}"
        is CatTrapMode.RandomMode -> Localization.getString("cattrap_mode_random", language)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(6.dp, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        color = Color(0xCC1E293B)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            // 關卡標題與步數儀表
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = titleText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFBBF24)
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0x660F172A),
                    border = BorderStroke(1.dp, Color(0x44FBBF24))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "🐾", fontSize = 14.sp)
                        Text(
                            text = "$steps",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF38BDF8)
                        )
                    }
                }
            }

            // 輔助按鈕
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(onClick = onHelpClick) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "Help",
                        tint = Color(0xFF94A3B8)
                    )
                }
                IconButton(onClick = onLeaderboardClick) {
                    Icon(
                        imageVector = Icons.Default.Leaderboard,
                        contentDescription = "Leaderboard",
                        tint = Color(0xFFF59E0B)
                    )
                }
            }
        }
    }
}

/**
 * 道具生效狀態提示標籤
 */
@Composable
private fun CatTrapStatusPill(
    language: AppLanguage,
    isFreezeActive: Boolean,
    doubleBlockRemaining: Int,
    isRadarHighlit: Boolean
) {
    if (!isFreezeActive && doubleBlockRemaining <= 0 && !isRadarHighlit) return

    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isFreezeActive) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xDD0284C7),
                border = BorderStroke(1.dp, Color(0xFFBAE6FD))
            ) {
                Text(
                    text = "🧊 貓咪冰凍中 (剩餘 1 回合)",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
        if (doubleBlockRemaining > 0) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xDDCA8A04),
                border = BorderStroke(1.dp, Color(0xFFFEF08A))
            ) {
                Text(
                    text = "🧱 連環障礙生效中 (尚可放 $doubleBlockRemaining 格)",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
        if (isRadarHighlit) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xDD059669),
                border = BorderStroke(1.dp, Color(0xFFA7F3D0))
            ) {
                Text(
                    text = "🧭 逃跑路徑透視中",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * 核心六角蜂巢棋盤渲染元件 (Jetpack Compose Canvas)
 */
@Composable
private fun CatTrapHexBoard(
    grid: Map<HexCoord, HexCellType>,
    catPos: HexCoord,
    catEmotion: CatEmotion,
    catBreed: CatBreed,
    escapePath: List<HexCoord>,
    isRadarHighlit: Boolean,
    tailWagAngle: Float,
    breatheScale: Float,
    onCellClick: (HexCoord) -> Unit
) {
    val gridSize = CatTrapLevels.GRID_SIZE

    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    // 棋盤解除 aspectRatio(1f) 限制，縱向充滿可用空間
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        val density = LocalDensity.current
        val minRadiusPx = with(density) { 24.dp.toPx() }
        val maxRadiusPx = with(density) { 34.dp.toPx() }

        // 放大六角外接半徑 R：以畫面高度為基準大幅放大，格子直徑放大近 50%
        val radius = ((height / 17.5f) * 0.95f).coerceIn(minRadiusPx, maxRadiusPx)

        val stepX = sqrt(3.0f) * radius
        val stepY = 1.5f * radius

        val totalBoardW = (gridSize - 1) * stepX + 0.5f * stepX + 2f * radius
        val totalBoardH = (gridSize - 1) * stepY + 2f * radius

        val baseStartX = (width - totalBoardW) / 2f
        val baseStartY = (height - totalBoardH) / 2f

        val maxPanX = maxOf(0f, (totalBoardW - width) / 2f + 50f)
        val maxPanY = maxOf(0f, (totalBoardH - height) / 2f + 50f)

        // 開局或貓咪移動時，自動平滑聚焦貓咪中心
        LaunchedEffect(catPos) {
            val catTargetX = baseStartX + catPos.q * stepX + (if (catPos.r % 2 == 1) stepX / 2f else 0f)
            val catTargetY = baseStartY + catPos.r * stepY
            val targetPanX = ((width / 2f) - catTargetX).coerceIn(-maxPanX, maxPanX)
            val targetPanY = ((height / 2f) - catTargetY).coerceIn(-maxPanY, maxPanY)
            panOffsetX = targetPanX
            panOffsetY = targetPanY
        }

        val startX = baseStartX + panOffsetX
        val startY = baseStartY + panOffsetY

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(maxPanX, maxPanY) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        panOffsetX = (panOffsetX + dragAmount.x).coerceIn(-maxPanX, maxPanX)
                        panOffsetY = (panOffsetY + dragAmount.y).coerceIn(-maxPanY, maxPanY)
                    }
                }
                .pointerInput(startX, startY, radius) {
                    detectTapGestures { tapOffset ->
                        // 尋找距離點擊最近的六角中心
                        var closestCoord: HexCoord? = null
                        var minDistanceSq = Float.MAX_VALUE

                        for (r in 0 until gridSize) {
                            for (q in 0 until gridSize) {
                                val cx = startX + q * stepX + (if (r % 2 == 1) stepX / 2f else 0f)
                                val cy = startY + r * stepY
                                val dx = tapOffset.x - cx
                                val dy = tapOffset.y - cy
                                val distSq = dx * dx + dy * dy
                                if (distSq < radius * radius && distSq < minDistanceSq) {
                                    minDistanceSq = distSq
                                    closestCoord = HexCoord(q, r)
                                }
                            }
                        }

                        closestCoord?.let { onCellClick(it) }
                    }
                }
        ) {
            // 1. 繪製六角網格浮板
            for (r in 0 until gridSize) {
                for (q in 0 until gridSize) {
                    val coord = HexCoord(q, r)
                    val cx = startX + q * stepX + (if (r % 2 == 1) stepX / 2f else 0f)
                    val cy = startY + r * stepY

                    val cellType = grid[coord] ?: HexCellType.EMPTY
                    val isPathCell = isRadarHighlit && coord in escapePath

                    draw3DHexCell(
                        cx = cx,
                        cy = cy,
                        radius = radius,
                        cellType = cellType,
                        isRadarPath = isPathCell
                    )
                }
            }

            // 2. 繪製栩栩如生的貓咪主角
            val catCx = startX + catPos.q * stepX + (if (catPos.r % 2 == 1) stepX / 2f else 0f)
            val catCy = startY + catPos.r * stepY

            drawLivelyCat(
                cx = catCx,
                cy = catCy,
                radius = radius,
                breed = catBreed,
                emotion = catEmotion,
                tailWag = tailWagAngle,
                breathe = breatheScale
            )
        }
    }
}

/**
 * 繪製具備立體厚度與高光微倒角的 3D 六角晶質地磚
 */
private fun DrawScope.draw3DHexCell(
    cx: Float,
    cy: Float,
    radius: Float,
    cellType: HexCellType,
    isRadarPath: Boolean
) {
    val hexPath = createPointyHexPath(cx, cy, radius * 0.94f)
    val shadowPath = createPointyHexPath(cx, cy + radius * 0.18f, radius * 0.94f)

    when (cellType) {
        HexCellType.EMPTY -> {
            // 立體下沉暗部
            drawPath(shadowPath, color = Color(0x660F172A))
            // 頂部浮板漸層
            val topBrush = if (isRadarPath) {
                Brush.radialGradient(
                    colors = listOf(Color(0xFFFDE047), Color(0xFFCA8A04)),
                    center = Offset(cx, cy),
                    radius = radius
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(Color(0xFFE2E8F0), Color(0xFF94A3B8)),
                    start = Offset(cx - radius, cy - radius),
                    end = Offset(cx + radius, cy + radius)
                )
            }
            drawPath(hexPath, brush = topBrush)
            // 高光微倒角邊框
            val strokeColor = if (isRadarPath) Color(0xFFFEF08A) else Color(0xAAFFFFFF)
            drawPath(hexPath, color = strokeColor, style = Stroke(width = 1.8f))
        }

        HexCellType.WATER_POND -> {
            // 天然凹陷水塘 / 融冰
            drawPath(hexPath, color = Color(0xCC0369A1))
            drawPath(
                hexPath,
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x3338BDF8), Color(0xCC0C4A6E)),
                    center = Offset(cx, cy),
                    radius = radius * 0.8f
                )
            )
            // 水面波光高光弧
            drawCircle(
                color = Color(0x44BAE6FD),
                radius = radius * 0.35f,
                center = Offset(cx - radius * 0.2f, cy - radius * 0.2f)
            )
            drawPath(hexPath, color = Color(0x550284C7), style = Stroke(width = 1.5f))
        }

        HexCellType.BLOCK -> {
            // 玩家放置的高立體毛線球 / 阻擋石
            drawPath(shadowPath, color = Color(0xAA020617))
            // 障礙磚本體 (金紅曜石)
            val blockBrush = Brush.radialGradient(
                colors = listOf(Color(0xFFFB7185), Color(0xFFBE123C)),
                center = Offset(cx, cy),
                radius = radius
            )
            drawPath(hexPath, brush = blockBrush)
            drawPath(hexPath, color = Color(0xFFFFE4E6), style = Stroke(width = 2.0f))

            // 毛線球紋理或金屬鉚釘裝飾
            drawCircle(
                color = Color(0x66FFFFFF),
                radius = radius * 0.25f,
                center = Offset(cx - radius * 0.15f, cy - radius * 0.15f)
            )
        }
    }
}

/**
 * 創建尖頂六角路徑 (Pointy-Topped Hexagon)
 */
private fun createPointyHexPath(cx: Float, cy: Float, r: Float): Path {
    val path = Path()
    for (i in 0 until 6) {
        val angleRad = (60.0 * i - 30.0) * PI / 180.0
        val x = (cx + r * cos(angleRad)).toFloat()
        val y = (cy + r * sin(angleRad)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

/**
 * 繪製栩栩如生的貓咪主角（多部位、毛色、花紋、靈動大眼、晃動尾巴與情緒）
 */
private fun DrawScope.drawLivelyCat(
    cx: Float,
    cy: Float,
    radius: Float,
    breed: CatBreed,
    emotion: CatEmotion,
    tailWag: Float,
    breathe: Float
) {
    val catScale = radius * 0.72f * breathe
    val bodyColor = Color(breed.primaryColorHex)
    val stripeColor = Color(breed.stripeColorHex)

    // 1. 貓咪投影
    drawOval(
        color = Color(0x66000000),
        topLeft = Offset(cx - catScale * 0.7f, cy + catScale * 0.35f),
        size = Size(catScale * 1.4f, catScale * 0.45f)
    )

    // 2. 靈活尾巴 (曲線搖晃)
    val tailPath = Path().apply {
        moveTo(cx - catScale * 0.45f, cy + catScale * 0.2f)
        quadraticTo(
            cx - catScale * 0.9f + tailWag * 0.5f,
            cy - catScale * 0.1f,
            cx - catScale * 0.65f + tailWag,
            cy - catScale * 0.6f
        )
    }
    drawPath(
        path = tailPath,
        color = bodyColor,
        style = Stroke(width = catScale * 0.22f, cap = StrokeCap.Round)
    )

    // 3. 圓潤貓身
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(bodyColor, stripeColor),
            center = Offset(cx, cy + catScale * 0.1f),
            radius = catScale * 0.75f
        ),
        radius = catScale * 0.6f,
        center = Offset(cx, cy + catScale * 0.1f)
    )

    // 4. 軟萌貓耳 (立體三角形 + 粉嫩內耳)
    val leftEar = Path().apply {
        moveTo(cx - catScale * 0.45f, cy - catScale * 0.35f)
        lineTo(cx - catScale * 0.38f, cy - catScale * 0.9f)
        lineTo(cx - catScale * 0.05f, cy - catScale * 0.45f)
        close()
    }
    val rightEar = Path().apply {
        moveTo(cx + catScale * 0.05f, cy - catScale * 0.45f)
        lineTo(cx + catScale * 0.38f, cy - catScale * 0.9f)
        lineTo(cx + catScale * 0.45f, cy - catScale * 0.35f)
        close()
    }
    drawPath(leftEar, color = bodyColor)
    drawPath(rightEar, color = bodyColor)

    // 粉色內耳
    val innerLeftEar = Path().apply {
        moveTo(cx - catScale * 0.40f, cy - catScale * 0.38f)
        lineTo(cx - catScale * 0.35f, cy - catScale * 0.78f)
        lineTo(cx - catScale * 0.12f, cy - catScale * 0.45f)
        close()
    }
    val innerRightEar = Path().apply {
        moveTo(cx + catScale * 0.12f, cy - catScale * 0.45f)
        lineTo(cx + catScale * 0.35f, cy - catScale * 0.78f)
        lineTo(cx + catScale * 0.40f, cy - catScale * 0.38f)
        close()
    }
    drawPath(innerLeftEar, color = Color(0xFFFFB7B2))
    drawPath(innerRightEar, color = Color(0xFFFFB7B2))

    // 5. 貓咪頭部
    drawCircle(
        color = bodyColor,
        radius = catScale * 0.48f,
        center = Offset(cx, cy - catScale * 0.25f)
    )

    // 6. 花紋 (如三花或虎斑)
    if (breed == CatBreed.CALICO || breed == CatBreed.ORANGE_TABBY) {
        drawOval(
            color = stripeColor,
            topLeft = Offset(cx - catScale * 0.35f, cy - catScale * 0.55f),
            size = Size(catScale * 0.22f, catScale * 0.18f)
        )
        drawOval(
            color = stripeColor,
            topLeft = Offset(cx + catScale * 0.12f, cy - catScale * 0.58f),
            size = Size(catScale * 0.24f, catScale * 0.22f)
        )
    }

    // 7. 生動眼部表情
    when (emotion) {
        CatEmotion.TRAPPED -> {
            // 蚊香圈眼 (認輸)
            drawCircle(Color.White, radius = catScale * 0.14f, center = Offset(cx - catScale * 0.18f, cy - catScale * 0.28f))
            drawCircle(Color.White, radius = catScale * 0.14f, center = Offset(cx + catScale * 0.18f, cy - catScale * 0.28f))
            drawCircle(Color.Black, radius = catScale * 0.08f, center = Offset(cx - catScale * 0.18f, cy - catScale * 0.28f), style = Stroke(2f))
            drawCircle(Color.Black, radius = catScale * 0.08f, center = Offset(cx + catScale * 0.18f, cy - catScale * 0.28f), style = Stroke(2f))
        }
        CatEmotion.ALERT -> {
            // 炸毛大瞳孔
            drawCircle(Color.White, radius = catScale * 0.15f, center = Offset(cx - catScale * 0.18f, cy - catScale * 0.28f))
            drawCircle(Color.White, radius = catScale * 0.15f, center = Offset(cx + catScale * 0.18f, cy - catScale * 0.28f))
            drawCircle(Color.Black, radius = catScale * 0.11f, center = Offset(cx - catScale * 0.18f, cy - catScale * 0.28f))
            drawCircle(Color.Black, radius = catScale * 0.11f, center = Offset(cx + catScale * 0.18f, cy - catScale * 0.28f))
            drawCircle(Color.White, radius = catScale * 0.04f, center = Offset(cx - catScale * 0.21f, cy - catScale * 0.31f))
            drawCircle(Color.White, radius = catScale * 0.04f, center = Offset(cx + catScale * 0.15f, cy - catScale * 0.31f))
        }
        else -> {
            // 靈動大眼睛 + 高光
            drawCircle(Color(0xFF2E7D32), radius = catScale * 0.13f, center = Offset(cx - catScale * 0.18f, cy - catScale * 0.28f))
            drawCircle(Color(0xFF2E7D32), radius = catScale * 0.13f, center = Offset(cx + catScale * 0.18f, cy - catScale * 0.28f))
            drawCircle(Color.Black, radius = catScale * 0.08f, center = Offset(cx - catScale * 0.18f, cy - catScale * 0.28f))
            drawCircle(Color.Black, radius = catScale * 0.08f, center = Offset(cx + catScale * 0.18f, cy - catScale * 0.28f))
            drawCircle(Color.White, radius = catScale * 0.035f, center = Offset(cx - catScale * 0.20f, cy - catScale * 0.30f))
            drawCircle(Color.White, radius = catScale * 0.035f, center = Offset(cx + catScale * 0.16f, cy - catScale * 0.30f))
        }
    }

    // 8. 粉嫩鼻尖與立體貓鬍鬚
    drawCircle(Color(0xFFFF8A80), radius = catScale * 0.045f, center = Offset(cx, cy - catScale * 0.18f))

    // 鬍鬚
    drawLine(Color(0xCC616161), Offset(cx - catScale * 0.12f, cy - catScale * 0.17f), Offset(cx - catScale * 0.48f, cy - catScale * 0.22f), strokeWidth = 1.5f)
    drawLine(Color(0xCC616161), Offset(cx - catScale * 0.12f, cy - catScale * 0.14f), Offset(cx - catScale * 0.46f, cy - catScale * 0.11f), strokeWidth = 1.5f)
    drawLine(Color(0xCC616161), Offset(cx + catScale * 0.12f, cy - catScale * 0.17f), Offset(cx + catScale * 0.48f, cy - catScale * 0.22f), strokeWidth = 1.5f)
    drawLine(Color(0xCC616161), Offset(cx + catScale * 0.12f, cy - catScale * 0.14f), Offset(cx + catScale * 0.46f, cy - catScale * 0.11f), strokeWidth = 1.5f)

    // 9. 軟萌前爪
    drawCircle(Color.White, radius = catScale * 0.11f, center = Offset(cx - catScale * 0.22f, cy + catScale * 0.38f))
    drawCircle(Color.White, radius = catScale * 0.11f, center = Offset(cx + catScale * 0.22f, cy + catScale * 0.38f))
}

/**
 * 底部操作與道具工具箱元件
 */
@Composable
private fun CatTrapBottomControls(
    language: AppLanguage,
    freezeCount: Int,
    doubleBlockCount: Int,
    radarCount: Int,
    onUndoClick: () -> Unit,
    onUseTool: (CatTrapTool) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .shadow(8.dp, RoundedCornerShape(36.dp)),
        shape = RoundedCornerShape(36.dp),
        color = Color(0xCC0F172A)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 悔棋按鈕
            Button(
                onClick = onUndoClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = Localization.getString("cattrap_undo", language), fontSize = 13.sp)
            }

            // 3 大道具按鈕
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CatTrapToolChip(
                    emoji = "🧊",
                    name = "冰凍",
                    count = freezeCount,
                    onClick = { onUseTool(CatTrapTool.FREEZE) }
                )
                CatTrapToolChip(
                    emoji = "🧱",
                    name = "連環",
                    count = doubleBlockCount,
                    onClick = { onUseTool(CatTrapTool.DOUBLE_BLOCK) }
                )
                CatTrapToolChip(
                    emoji = "🧭",
                    name = "雷達",
                    count = radarCount,
                    onClick = { onUseTool(CatTrapTool.RADAR) }
                )
            }
        }
    }
}

@Composable
private fun CatTrapToolChip(
    emoji: String,
    name: String,
    count: Int,
    onClick: () -> Unit
) {
    val isOutOfStock = count <= 0
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = if (isOutOfStock) Color(0xFF292524) else Color(0xFF1E293B),
        border = BorderStroke(
            if (isOutOfStock) 1.5.dp else 1.dp,
            if (isOutOfStock) Color(0xFFFBBF24) else Color(0xFF38BDF8)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = emoji, fontSize = 16.sp)
            if (isOutOfStock) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFFBBF24).copy(alpha = 0.25f)
                ) {
                    Text(
                        text = "+ 🎬",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFBBF24),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            } else {
                Text(
                    text = "x$count",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8)
                )
            }
        }
    }
}

/**
 * 玩法任務簡報彈窗 (Briefing Dialog)
 */
@Composable
private fun CatTrapBriefingDialog(
    language: AppLanguage,
    mode: CatTrapMode,
    selectedBreed: CatBreed,
    onSelectBreed: (CatBreed) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = Localization.getString("cattrap_briefing_title", language),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFBBF24)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = Localization.getString("cattrap_briefing_desc", language),
                    fontSize = 14.sp,
                    color = Color(0xFFE2E8F0),
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Spacer(modifier = Modifier.height(14.dp))

                // 戰術輔助道具指南
                Text(
                    text = Localization.getString("cattrap_tool_guide_title", language),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFBBF24)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🧊", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${Localization.getString("cattrap_tool_freeze", language)}: ${Localization.getString("cattrap_tool_freeze_desc", language)}",
                            fontSize = 12.sp,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🧱", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${Localization.getString("cattrap_tool_double", language)}: ${Localization.getString("cattrap_tool_double_desc", language)}",
                            fontSize = 12.sp,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🧭", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${Localization.getString("cattrap_tool_radar", language)}: ${Localization.getString("cattrap_tool_radar_desc", language)}",
                            fontSize = 12.sp,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "🎬 ${Localization.getString("cattrap_tool_ad_hint", language)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFDE047)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 貓咪品系切換
                Text(
                    text = Localization.getString("cattrap_breed_select", language),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF38BDF8)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CatBreed.entries.forEach { breed ->
                        val isSelected = (breed == selectedBreed)
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(breed.primaryColorHex))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) Color(0xFFFBBF24) else Color.White,
                                    shape = CircleShape
                                )
                                .clickable { onSelectBreed(breed) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "🐾", fontSize = 18.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "開始圍捕！", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

/**
 * 3 星勝利結算彈窗 (Victory Dialog)
 */
@Composable
private fun CatTrapVictoryDialog(
    language: AppLanguage,
    mode: CatTrapMode,
    steps: Int,
    stars: Int,
    score: Int,
    onPlayAgain: () -> Unit,
    onNextLevel: () -> Unit,
    onLeaderboard: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = Localization.getString("cattrap_win_title", language),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFBBF24),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3 星動態解鎖
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..3) {
                        val isUnlocked = i <= stars
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (isUnlocked) Color(0xFFFBBF24) else Color(0xFF334155),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "消耗步數：$steps 步  |  得分：$score 分",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = Localization.getString("cattrap_win_desc", language),
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onPlayAgain,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(text = "重來一局", fontSize = 14.sp)
                    }

                    Button(
                        onClick = onNextLevel,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(text = "下一關", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

/**
 * 失敗結算彈窗 (Defeat Dialog)
 */
@Composable
private fun CatTrapDefeatDialog(
    language: AppLanguage,
    mode: CatTrapMode,
    steps: Int,
    onRetry: () -> Unit,
    onBackToMenu: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = Localization.getString("cattrap_lose_title", language),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF87171),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = Localization.getString("cattrap_lose_desc", language),
                    fontSize = 13.sp,
                    color = Color(0xFFCBD5E1),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onBackToMenu,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(text = "返回選單", fontSize = 14.sp)
                    }

                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(text = "再試一次", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

/**
 * AdMob 道具獎勵加值彈窗
 */
@Composable
private fun CatTrapRewardDialog(
    language: AppLanguage,
    tool: CatTrapTool,
    onConfirmWatchAd: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${tool.iconEmoji} ${Localization.getString("cattrap_reward_ad_title", language)}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFBBF24)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = Localization.getString("cattrap_reward_ad_desc", language),
                    fontSize = 14.sp,
                    color = Color(0xFFCBD5E1),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(text = "取消", fontSize = 14.sp)
                    }

                    Button(
                        onClick = onConfirmWatchAd,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(text = "觀看短片", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
