package com.example.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.audio.SoundManager
import com.example.data.model.AppLanguage
import com.example.data.model.GameDifficulty
import com.example.data.model.Localization
import com.example.game.fruit.FruitGameMode
import com.example.game.fruit.FruitItem
import com.example.game.fruit.FruitSlicerEngine
import com.example.game.fruit.FruitType
import com.example.game.fruit.JuiceParticle
import com.example.game.fruit.JuiceResult
import com.example.game.fruit.SlicingWorkshopEngine
import com.example.game.fruit.SpecialFruitType
import com.example.game.fruit.WorkshopItemType
import com.example.game.fruit.WorkshopState
import com.example.ui.components.AppBackground
import com.example.ui.theme.AppThemeStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 水果切切樂主畫面
 * 1. 模式選單頁：包含三大模式獨立 3D 檜木精雕卡片、玩法介紹、最高分與開玩按鈕
 * 2. 遊戲進行頁：日式檜木案板全螢幕沉浸遊玩，徹底移除行政分頁標籤
 */
@Composable
fun FruitMasterScreen(
    language: AppLanguage,
    appTheme: AppThemeStyle,
    heartbeatBestScore: Int,
    bladeBombBestScore: Int,
    workshopBestScore: Int,
    onBackClick: () -> Unit,
    onSaveScore: (mode: FruitGameMode, score: Int) -> Unit,
    onLeaderboardClick: (GameDifficulty?) -> Unit = {},
    onGameOver: () -> Unit = {},
    onGameInterrupted: () -> Unit = {}
) {
    // 當前正在遊玩的模式，null 表示處於「模式選單頁」
    var activePlayingMode by remember { mutableStateOf<FruitGameMode?>(null) }

    BackHandler {
        if (activePlayingMode != null) {
            onGameInterrupted()
            activePlayingMode = null
        } else {
            onBackClick()
        }
    }

    AppBackground(themeStyle = appTheme) {
        if (activePlayingMode == null) {
            // 水果切切樂獨立模式選單頁面
            FruitMasterModeSelectMenu(
                language = language,
                heartbeatBestScore = heartbeatBestScore,
                bladeBombBestScore = bladeBombBestScore,
                workshopBestScore = workshopBestScore,
                onBackClick = onBackClick,
                onSelectMode = { mode ->
                    SoundManager.playClick()
                    activePlayingMode = mode
                },
                onLeaderboardClick = { onLeaderboardClick(null) }
            )
        } else {
            // 全螢幕沉浸遊戲遊玩區（日式檜木案板道場）
            when (val mode = activePlayingMode) {
                FruitGameMode.HEARTBEAT_SLICER, FruitGameMode.BLADE_AND_BOMB -> {
                    FruitSlicerPlayArea(
                        mode = mode,
                        language = language,
                        appTheme = appTheme,
                        bestScore = if (mode == FruitGameMode.HEARTBEAT_SLICER) heartbeatBestScore else bladeBombBestScore,
                        onBackToMenu = { activePlayingMode = null },
                        onSaveScore = { score -> onSaveScore(mode, score) },
                        onOpenLeaderboard = {
                            onLeaderboardClick(if (mode == FruitGameMode.HEARTBEAT_SLICER) GameDifficulty.BEGINNER else GameDifficulty.INTERMEDIATE)
                        },
                        onGameOver = onGameOver,
                        onGameInterrupted = onGameInterrupted
                    )
                }
                FruitGameMode.WORKSHOP -> {
                    SlicingWorkshopPlayArea(
                        language = language,
                        appTheme = appTheme,
                        bestScore = workshopBestScore,
                        onBackToMenu = { activePlayingMode = null },
                        onSaveScore = { score -> onSaveScore(FruitGameMode.WORKSHOP, score) },
                        onOpenLeaderboard = { onLeaderboardClick(GameDifficulty.ADVANCED) },
                        onGameOver = onGameOver,
                        onGameInterrupted = onGameInterrupted
                    )
                }
                null -> Unit
            }
        }
    }
}

/**
 * 水果切切樂 - 獨立三大模式選擇選單（3D 檜木精雕卡片風格）
 */
@Composable
private fun FruitMasterModeSelectMenu(
    language: AppLanguage,
    heartbeatBestScore: Int,
    bladeBombBestScore: Int,
    workshopBestScore: Int,
    onBackClick: () -> Unit,
    onSelectMode: (FruitGameMode) -> Unit,
    onLeaderboardClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // 頂部導航列（日式檜木飾板風）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    SoundManager.playClick()
                    onBackClick()
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = Localization.getString("game_fruit_master", language),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                )
            }

            // 排行榜按鈕（金屬徽章質感）
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                modifier = Modifier.clickable {
                    SoundManager.playClick()
                    onLeaderboardClick()
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Leaderboard,
                        contentDescription = "Leaderboard",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = Localization.getString("leaderboard_title", language),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 標題提示（檜木金邊飾卷）
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD54F).copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "⚔️ " + Localization.getString("mode_select_title", language) + " - 體驗三大舒壓極致切削",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 三大模式獨立 3D 檜木精雕卡片清單
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 模式 1：心跳果刃戰
            item {
                ModeCard(
                    title = Localization.getString("mode_heartbeat_slicer", language),
                    tag = "⏱️ 限時 60 秒極限衝刺",
                    desc = "緊張刺激的限時衝刺！無限連斬累積狂熱雙倍得分，最後 15 秒進入心臟脈衝危機警示與動態 ECG 心跳儀！",
                    bestScore = heartbeatBestScore,
                    primaryColor = Color(0xFFFF5722),
                    iconEmoji = "💓",
                    language = language,
                    onPlay = { onSelectMode(FruitGameMode.HEARTBEAT_SLICER) }
                )
            }

            // 模式 2：避雷狂刀客
            item {
                ModeCard(
                    title = Localization.getString("mode_blade_and_bomb", language),
                    tag = "❤️ 經典 3 命考驗",
                    desc = "眼疾手快的考驗！任何一顆水果都不可漏切落下，更要精準閃避黑炸彈噴墨遮罩與即時引爆！",
                    bestScore = bladeBombBestScore,
                    primaryColor = Color(0xFFE53935),
                    iconEmoji = "💣",
                    language = language,
                    onPlay = { onSelectMode(FruitGameMode.BLADE_AND_BOMB) }
                )
            }

            // 模式 3：切片工坊 ASMR
            item {
                ModeCard(
                    title = Localization.getString("mode_workshop", language),
                    tag = "🍹 節奏勻速切片舒壓",
                    desc = "極致解壓傳送帶切削體驗！按壓菜刀均勻切片，靈巧避開堅硬金屬障礙，收集新鮮果汁特調彩虹成果！",
                    bestScore = workshopBestScore,
                    primaryColor = Color(0xFF00897B),
                    iconEmoji = "🔪",
                    language = language,
                    onPlay = { onSelectMode(FruitGameMode.WORKSHOP) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * 模式獨立卡片元件（次世代 3D 檜木精雕微倒角質感）
 */
@Composable
private fun ModeCard(
    title: String,
    tag: String,
    desc: String,
    bestScore: Int,
    primaryColor: Color,
    iconEmoji: String,
    language: AppLanguage,
    onPlay: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable { onPlay() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 5.dp,
            pressedElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            primaryColor.copy(alpha = 0.5f),
                            Color(0xFFFFD54F).copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(22.dp)
                )
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 3D 浮雕圓盤圖標座
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = primaryColor.copy(alpha = 0.16f),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, primaryColor.copy(alpha = 0.4f)),
                        modifier = Modifier.size(50.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = iconEmoji, fontSize = 26.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = primaryColor,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                // 歷史最佳得分金屬膠囊
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFFD54F).copy(alpha = 0.14f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD54F).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🏆 $bestScore",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFFFB300)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 19.sp
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3D 水晶開玩大按鈕
            Button(
                onClick = onPlay,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 1.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = Localization.getString("start_game", language),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            }
        }
    }
}

// 飄落花瓣與竹葉微粒數據
private data class DojoFloatingParticle(
    var x: Float,
    var y: Float,
    var speedY: Float,
    var speedX: Float,
    var rotation: Float,
    var rotationSpeed: Float,
    var size: Float,
    var alpha: Float,
    val isLeaf: Boolean
)

// 刀尖撕裂火花星芒微粒數據
private data class BladeSparkParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var color: Color,
    var size: Float,
    var life: Float
)

/**
 * 模式一（心跳果刃戰）與 模式二（避雷狂刀客）全螢幕次世代沉浸道場遊玩區域
 */
@Composable
private fun FruitSlicerPlayArea(
    mode: FruitGameMode,
    language: AppLanguage,
    appTheme: AppThemeStyle,
    bestScore: Int,
    onBackToMenu: () -> Unit,
    onSaveScore: (Int) -> Unit,
    onOpenLeaderboard: () -> Unit,
    onGameOver: () -> Unit = {},
    onGameInterrupted: () -> Unit = {}
) {
    val context = LocalContext.current
    var showResultDialog by remember { mutableStateOf(false) }
    var gameSessionId by remember { mutableIntStateOf(0) }
    val engine = remember(mode) { FruitSlicerEngine(mode = mode) }

    // 每幀更新觸發器
    var frameTicker by remember { mutableLongStateOf(0L) }

    // 連擊徽章顯示計時 (劃出 >= 3 連擊時展示 1.2 秒)
    var displayedCombo by remember { mutableIntStateOf(0) }
    LaunchedEffect(displayedCombo) {
        if (displayedCombo > 0) {
            delay(1200L)
            displayedCombo = 0
        }
    }

    // 手勢軌跡快照
    val trailPoints = remember { mutableStateListOf<Offset>() }

    // 刀光撕裂火花星芒系統
    val bladeSparks = remember { mutableStateListOf<BladeSparkParticle>() }

    // 道場飄落櫻花瓣與竹葉微粒 (18顆)
    val floatingParticles = remember {
        mutableStateListOf<DojoFloatingParticle>().apply {
            for (i in 0 until 18) {
                add(
                    DojoFloatingParticle(
                        x = (0..1080).random().toFloat(),
                        y = (0..1920).random().toFloat(),
                        speedY = (35..85).random().toFloat(),
                        speedX = (-25..25).random().toFloat(),
                        rotation = (0..360).random().toFloat(),
                        rotationSpeed = (-80..80).random().toFloat(),
                        size = (10..22).random().toFloat(),
                        alpha = Random.nextFloat() * (0.65f - 0.28f) + 0.28f,
                        isLeaf = i % 2 == 0
                    )
                )
            }
        }
    }

    // 主題刀刃光芒色彩（概念圖為深邃電光紫藍色系）
    val (bladeCoreColor, bladeGlowColor) = remember(appTheme) {
        when (appTheme) {
            AppThemeStyle.SNOW_WHITE -> Pair(Color.White, Color(0xFF00E5FF))
            AppThemeStyle.DARK -> Pair(Color.White, Color(0xFF7C4DFF)) // 概念圖標誌電光武士紫藍
            AppThemeStyle.MECHANICAL -> Pair(Color.White, Color(0xFF00E5FF))
            AppThemeStyle.CUTE -> Pair(Color(0xFFFFF9C4), Color(0xFFFF4081))
            AppThemeStyle.SUNNY -> Pair(Color.White, Color(0xFFFF9100))
            AppThemeStyle.CORPORATE -> Pair(Color.White, Color(0xFF2979FF))
            AppThemeStyle.CASUAL -> Pair(Color.White, Color(0xFF00E676))
        }
    }

    // 心跳儀 ECG 動態脈衝偏移
    val infiniteTransition = rememberInfiniteTransition(label = "ecg")
    val ecgPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (engine.isCriticalHeartbeat) (1000 / engine.heartbeatSpeedMultiplier).toInt() else 1000,
                easing = LinearEasing
            )
        ),
        label = "ecgPhase"
    )

    // 連擊徽章呼吸浮雕動畫
    val comboPulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "comboPulse"
    )

    // 最後 15 秒背景呼吸紅光
    val criticalRedAlpha by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (engine.isCriticalHeartbeat) (600 / engine.heartbeatSpeedMultiplier).toInt() else 600,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "criticalRed"
    )

    // 心臟搏動脈衝音效觸發 (最後 15 秒每秒跳動)
    LaunchedEffect(engine.isCriticalHeartbeat, (engine.remainingSeconds).toInt()) {
        if (engine.isCriticalHeartbeat && !engine.isGameOver) {
            SoundManager.playHeartbeatPulse(engine.heartbeatSpeedMultiplier)
        }
    }

    // 主遊戲循環（每幀持續更新物理、粒子與驅動 Canvas 重繪）
    LaunchedEffect(engine, gameSessionId) {
        var lastTimeNanos = 0L
        while (isActive && !engine.isGameOver) {
            withFrameNanos { timeNanos ->
                if (lastTimeNanos != 0L) {
                    val dt = ((timeNanos - lastTimeNanos) / 1_000_000_000f).coerceAtMost(0.05f)
                    engine.update(dt)

                    // 1. 更新飄落花瓣/竹葉
                    val screenW = if (engine.screenWidth > 0f) engine.screenWidth else 1080f
                    val screenH = if (engine.screenHeight > 0f) engine.screenHeight else 1920f
                    for (p in floatingParticles) {
                        p.y += p.speedY * dt
                        p.x += p.speedX * dt
                        p.rotation += p.rotationSpeed * dt
                        if (p.y > screenH + 40f) {
                            p.y = -40f
                            p.x = Random.nextFloat() * screenW
                        }
                    }

                    // 2. 更新刀光撕裂火花星芒
                    val it = bladeSparks.iterator()
                    while (it.hasNext()) {
                        val sp = it.next()
                        sp.x += sp.vx * dt
                        sp.y += sp.vy * dt
                        sp.life -= dt * 2.8f
                        if (sp.life <= 0f) {
                            it.remove()
                        }
                    }

                    frameTicker = timeNanos // 驅動每一幀 100% 重繪！
                    if (engine.isGameOver) {
                        onSaveScore(engine.score)
                        showResultDialog = true
                        onGameOver()
                    }
                }
                lastTimeNanos = timeNanos
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        engine.screenWidth = constraints.maxWidth.toFloat()
        engine.screenHeight = constraints.maxHeight.toFloat()

        // 1. 最後 15 秒背景呼吸紅光警示
        if (engine.isCriticalHeartbeat) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Red.copy(alpha = criticalRedAlpha))
            )
        }

        // 2. 主物理、道場案板、水果與刀痕 Canvas 繪製層（每幀平滑重繪）
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(engine) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            trailPoints.clear()
                            trailPoints.add(offset)
                            engine.onPointerDown()
                            SoundManager.playFruitSliceSwish()
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val newPoint = change.position
                            if (trailPoints.isNotEmpty()) {
                                val lastPoint = trailPoints.last()
                                val res = engine.processSlice(lastPoint, newPoint)

                                // 在滑動軌跡上噴濺撕裂火花星芒粒子
                                val sparkCount = if (res.hitFruits.isNotEmpty()) 6 else 2
                                for (k in 0 until sparkCount) {
                                    bladeSparks.add(
                                        BladeSparkParticle(
                                            x = newPoint.x + Random.nextInt(-12, 13).toFloat(),
                                            y = newPoint.y + Random.nextInt(-12, 13).toFloat(),
                                            vx = Random.nextInt(-160, 161).toFloat(),
                                            vy = Random.nextInt(-160, 161).toFloat(),
                                            color = if (res.hitFruits.isNotEmpty()) Color(0xFFFFD54F) else bladeGlowColor,
                                            size = Random.nextInt(3, 8).toFloat(),
                                            life = 1.0f
                                        )
                                    )
                                }

                                if (res.comboCount >= 3) {
                                    displayedCombo = maxOf(displayedCombo, res.comboCount)
                                }
                                if (res.hitFruits.isNotEmpty()) {
                                    SoundManager.playFruitJuiceSplash()
                                    triggerVibration(context, 40)
                                }
                                if (res.hitBomb) {
                                    SoundManager.playBombExplosion()
                                    triggerVibration(context, 150)
                                }
                                if (res.triggeredFreeze) {
                                    SoundManager.playFreezeEffect()
                                }
                                if (res.triggeredRainbow) {
                                    SoundManager.playRainbowExplosion()
                                }
                            }
                            trailPoints.add(newPoint)
                            if (trailPoints.size > 16) {
                                trailPoints.removeAt(0)
                            }
                        },
                        onDragEnd = {
                            val combo = engine.onPointerUp()
                            if (combo >= 3) {
                                SoundManager.playComboChime(combo)
                                displayedCombo = maxOf(displayedCombo, combo)
                            }
                            trailPoints.clear()
                        },
                        onDragCancel = {
                            engine.onPointerUp()
                            trailPoints.clear()
                        }
                    )
                }
        ) {
            // 讀取 frameTicker 確保每一幀 100% 刷新重繪
            val _tick = frameTicker

            // 1. 繪製日式道場檜木案板底盤與沉浸竹林陽光漫射 (Japanese Dojo Hinoki Board)
            drawDojoHinokiBoard(appTheme, floatingParticles)

            // 背景心臟浮點立體跳動 (最後 15 秒，佔螢幕 60% 以上，柔和半透明羽化)
            if (engine.isCriticalHeartbeat) {
                drawHeartbeatPulsingArt(
                    center = Offset(size.width / 2f, size.height * 0.46f),
                    scale = 1.0f + (ecgPhase * 0.12f),
                    alpha = criticalRedAlpha * 0.45f
                )
            }

            // 2. 繪製所有超寫實立體熱帶水果
            for (fruit in engine.fruits) {
                drawFruitItem(fruit)
            }

            // 3. 繪製次世代晶瑩有機水滴爆汁粒子 (Juice Splash Droplets)
            drawHyperJuicyParticles(engine.particles)

            // 4. 繪製電光疾風斬痕刀光與撕裂星芒粒子 (Electric Plasma Blade Trail & Sparks)
            drawElectricPlasmaBladeTrail(
                trailPoints = trailPoints,
                sparks = bladeSparks,
                bladeCoreColor = bladeCoreColor,
                bladeGlowColor = bladeGlowColor,
                isFever = engine.isFeverActive
            )

            // 冰凍全螢幕霜凍濾鏡
            if (engine.isFrozen) {
                drawRect(
                    color = Color(0x3380D8FF),
                    size = size
                )
            }

            // 搞怪炸彈墨水覆蓋遮罩 (墨水噗滋遮蔽視野 2 秒)
            if (engine.inkAlpha > 0f) {
                drawInkSplatterMask(size = size, alpha = engine.inkAlpha)
            }
        }

        // 3. 浮動加分文字
        for (t in engine.floatingTexts) {
            Text(
                text = t.text,
                color = t.color.copy(alpha = t.alpha.coerceIn(0f, 1f)),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                modifier = Modifier
                    .offset(x = (t.x - 50f).dp, y = (t.y - 20f).dp)
                    .scale(t.scale)
            )
        }

        // 4. 概念圖「⚡ COMBO X {combo}! ⚡」神威金紫漸層流光徽章 (連擊 >= 3 觸發)
        val effectiveCombo = maxOf(engine.currentCombo, displayedCombo)
        if (effectiveCombo >= 3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 96.dp),
                contentAlignment = Alignment.Center
            ) {
                ComboBadgeOverlay(
                    combo = effectiveCombo,
                    pulseScale = comboPulseScale
                )
            }
        }

        // 5. 頂部日式檜木浮雕招牌 HUD（心跳儀 / 愛心 / 分數 / 返回模式選單按鈕）
        TopGameStatusOverlay(
            engine = engine,
            mode = mode,
            bestScore = bestScore,
            ecgPhase = ecgPhase,
            language = language,
            onBackToMenu = {
                if (!engine.isGameOver && (engine.score > 0 || engine.totalSliced > 0)) {
                    onGameInterrupted()
                }
                onBackToMenu()
            },
            onOpenLeaderboard = onOpenLeaderboard
        )

        // 6. 底部概念圖電光武士刀鞘能量槽裝飾 (Katana Sheath Energy Gauge)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp)
        ) {
            KatanaSheathEnergyGauge(
                progress = if (engine.isFeverActive) (engine.feverTimer / 5.0f).coerceIn(0f, 1f) else engine.feverGauge,
                isFever = engine.isFeverActive
            )
        }
    }

    // 結算彈窗（升級為手遊 3 星成就榮譽面板）
    if (showResultDialog) {
        FruitResultDialog(
            score = engine.score,
            bestScore = bestScore,
            totalSliced = engine.totalSliced,
            maxCombo = engine.maxCombo,
            language = language,
            onRestart = {
                showResultDialog = false
                engine.reset()
                gameSessionId++
            },
            onLeaderboard = {
                showResultDialog = false
                onOpenLeaderboard()
            },
            onBackToMenu = {
                showResultDialog = false
                onBackToMenu()
            }
        )
    }
}

/**
 * 頂部日式檜木浮雕招牌 HUD 儀表板
 */
@Composable
private fun TopGameStatusOverlay(
    engine: FruitSlicerEngine,
    mode: FruitGameMode,
    bestScore: Int,
    ecgPhase: Float,
    language: AppLanguage,
    onBackToMenu: () -> Unit,
    onOpenLeaderboard: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左側：返回模式選單木牌按鈕 + 心跳/生命槽
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF2E1C11).copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFFD54F).copy(alpha = 0.4f)),
                    modifier = Modifier.clickable {
                        SoundManager.playClick()
                        onBackToMenu()
                    }
                ) {
                    Box(modifier = Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to menu",
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (mode == FruitGameMode.HEARTBEAT_SLICER) {
                    // 電光管 ECG 心跳儀面板
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF1E120B).copy(alpha = 0.9f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (engine.isCriticalHeartbeat) Color(0xFFFF1744) else Color(0xFFFFD54F).copy(alpha = 0.45f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Canvas(modifier = Modifier.size(width = 44.dp, height = 22.dp)) {
                                drawEcgWave(phase = ecgPhase, isCritical = engine.isCriticalHeartbeat)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${engine.remainingSeconds.toInt()}s",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp,
                                color = if (engine.isCriticalHeartbeat) Color(0xFFFF1744) else Color(0xFFFFD54F)
                            )
                        }
                    }
                } else {
                    // 避雷狂刀客：3 顆晶亮立體心心
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF1E120B).copy(alpha = 0.85f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD54F).copy(alpha = 0.35f)),
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (i in 1..engine.maxHearts) {
                                Icon(
                                    imageVector = if (i <= engine.hearts) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (i <= engine.hearts) Color(0xFFFF2A48) else Color.Gray.copy(alpha = 0.45f),
                                    modifier = Modifier.size(24.dp)
                                )
                                if (i < engine.maxHearts) Spacer(modifier = Modifier.width(3.dp))
                            }
                        }
                    }
                }
            }

            // 右側：檜木浮雕金色招牌「得分: 148,750」+ 獎盃排行榜按鈕
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF2E1C11).copy(alpha = 0.9f),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFFD54F).copy(alpha = 0.5f))
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "得分: ${engine.score}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFFFD54F),
                                letterSpacing = 0.5.sp
                            )
                        )
                        Text(
                            text = "最佳: $bestScore",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFFFE082).copy(alpha = 0.8f),
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF2E1C11).copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFFD54F).copy(alpha = 0.4f)),
                    modifier = Modifier.clickable {
                        SoundManager.playClick()
                        onOpenLeaderboard()
                    }
                ) {
                    Box(modifier = Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Leaderboard,
                            contentDescription = "Leaderboard",
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 概念圖中驚艷絕倫的「⚡ COMBO X {combo}! ⚡」神威金紫漸層流光徽章
 */
@Composable
private fun ComboBadgeOverlay(
    combo: Int,
    pulseScale: Float
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF1A0B2E).copy(alpha = 0.88f),
        border = androidx.compose.foundation.BorderStroke(
            width = 2.dp,
            brush = Brush.horizontalGradient(
                listOf(Color(0xFFFFD54F), Color(0xFFD500F9), Color(0xFF00E5FF), Color(0xFFFFD54F))
            )
        ),
        shadowElevation = 8.dp,
        modifier = Modifier.scale(pulseScale)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⚡ COMBO X $combo! ⚡",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    letterSpacing = 1.2.sp
                ),
                color = Color(0xFFFFD700)
            )
        }
    }
}

/**
 * 底部概念圖電光武士刀鞘能量槽 (Katana Sheath Energy Gauge)
 */
@Composable
private fun KatanaSheathEnergyGauge(
    progress: Float,
    isFever: Boolean
) {
    Canvas(modifier = Modifier.size(width = 300.dp, height = 38.dp)) {
        val w = size.width
        val h = size.height

        // 1. 刀鞘金屬外廓與底盤 (黑鐵鍍銀外鞘)
        val sheathPath = Path().apply {
            moveTo(14f, h * 0.28f)
            cubicTo(w * 0.35f, h * 0.2f, w * 0.7f, h * 0.2f, w - 14f, h * 0.38f)
            lineTo(w - 14f, h * 0.75f)
            cubicTo(w * 0.7f, h * 0.65f, w * 0.35f, h * 0.65f, 14f, h * 0.82f)
            close()
        }

        // 刀鞘深色金屬漸層
        drawPath(
            path = sheathPath,
            brush = Brush.verticalGradient(
                listOf(Color(0xFF263238), Color(0xFF101416), Color(0xFF0A0D0E))
            )
        )

        // 刀鞘外緣電光霓虹光暈 (Fever 時為耀金，平日為電光藍紫)
        val glowColor = if (isFever) Color(0xFFFFD54F) else Color(0xFF00E5FF)
        drawPath(
            path = sheathPath,
            color = glowColor.copy(alpha = if (isFever) 0.9f else 0.5f),
            style = Stroke(width = if (isFever) 2.5f else 1.8f)
        )

        // 2. 刀身古代發光符文雕刻 (如 ᚲ ᚱ ᚦ ᚾ)
        val runeX = w * 0.32f
        val runeY = h * 0.48f
        drawLine(
            color = glowColor.copy(alpha = 0.8f),
            start = Offset(runeX - 25f, runeY),
            end = Offset(runeX + 25f, runeY),
            strokeWidth = 2f
        )
        drawLine(
            color = glowColor.copy(alpha = 0.8f),
            start = Offset(runeX - 10f, runeY - 5f),
            end = Offset(runeX - 10f, runeY + 5f),
            strokeWidth = 2f
        )
        drawLine(
            color = glowColor.copy(alpha = 0.8f),
            start = Offset(runeX + 10f, runeY - 5f),
            end = Offset(runeX + 10f, runeY + 5f),
            strokeWidth = 2f
        )

        // 3. 刀身充能進度光槽 (四段式能量光條)
        val barStartX = w * 0.48f
        val barTotalW = w * 0.42f
        val segW = barTotalW / 4f - 4f
        for (i in 0 until 4) {
            val segX = barStartX + i * (segW + 4f)
            val segProgress = (progress * 4f - i).coerceIn(0f, 1f)
            // 底槽
            drawRoundRect(
                color = Color(0xFF1A1A1A),
                topLeft = Offset(segX, h * 0.42f),
                size = Size(segW, 6f),
                cornerRadius = CornerRadius(3f, 3f)
            )
            // 充能光條
            if (segProgress > 0f) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        if (isFever) listOf(Color(0xFFFF1744), Color(0xFFFFD54F)) else listOf(Color(0xFF00E5FF), Color(0xFF7C4DFF))
                    ),
                    topLeft = Offset(segX, h * 0.42f),
                    size = Size(segW * segProgress, 6f),
                    cornerRadius = CornerRadius(3f, 3f)
                )
            }
        }

        // 4. 刀鞘末端「滿 / MAX」標記點
        if (progress >= 0.98f || isFever) {
            drawCircle(
                color = Color(0xFFFFD54F),
                radius = 4f,
                center = Offset(w - 22f, h * 0.52f)
            )
        }
    }
}

/**
 * 模式三：切片工坊 ASMR 遊玩區域（補齊平滑循環與返回按鈕）
 */
/**
 * 模式三：切片工坊 ASMR 遊玩區域（支援長按連切、刀刃高度校正、榨汁攪拌動畫與特調成果展）
 */
@Composable
private fun SlicingWorkshopPlayArea(
    language: AppLanguage,
    appTheme: AppThemeStyle,
    bestScore: Int,
    onBackToMenu: () -> Unit,
    onSaveScore: (Int) -> Unit,
    onOpenLeaderboard: () -> Unit,
    onGameOver: () -> Unit = {},
    onGameInterrupted: () -> Unit = {}
) {
    val context = LocalContext.current
    var gameSessionId by remember { mutableIntStateOf(0) }
    val engine = remember { SlicingWorkshopEngine() }
    var juiceResult by remember { mutableStateOf<JuiceResult?>(null) }
    var showJuiceDialog by remember { mutableStateOf(false) }

    // 每幀更新觸發器
    var frameTicker by remember { mutableLongStateOf(0L) }

    // 廚刀下斬動態動畫 (0f 閒置抬刀 ~ 60f 下斬觸碰切板)
    val knifeOffset = remember { Animatable(0f) }

    // 長按連切狀態
    var isPressing by remember { mutableStateOf(false) }

    // 輸送帶條紋滾動相位
    val infiniteTransition = rememberInfiniteTransition(label = "conveyor")
    val beltOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 350, easing = LinearEasing)
        ),
        label = "beltScroll"
    )

    // 長按連切協程迴圈 (每 115ms 自動連續急速下刀，爽快 ASMR 節奏)
    LaunchedEffect(isPressing, engine.state) {
        if (isPressing && engine.state == WorkshopState.SLICING) {
            while (isPressing && engine.state == WorkshopState.SLICING) {
                knifeOffset.snapTo(60f)
                val chop = engine.performChop()
                if (chop.isMetalClang) {
                    SoundManager.playMetalClang()
                    triggerVibration(context, 100)
                } else if (chop.hitItem) {
                    SoundManager.playWoodblockChop()
                    triggerVibration(context, 25)
                }
                knifeOffset.animateTo(0f, tween(50))
                delay(65)
            }
        }
    }

    // 狀態轉移與音效監聽
    LaunchedEffect(engine.state) {
        if (engine.state == WorkshopState.BLENDING) {
            isPressing = false
            SoundManager.playBlenderWhir()
        } else if (engine.state == WorkshopState.RESULT && !showJuiceDialog) {
            juiceResult = engine.evaluateJuiceResult()
            onSaveScore(engine.score)
            showJuiceDialog = true
            onGameOver()
        }
    }

    // 主物理與渲染循環
    LaunchedEffect(engine, gameSessionId) {
        var lastTimeNanos = 0L
        engine.reset()
        while (isActive) {
            withFrameNanos { timeNanos ->
                if (lastTimeNanos != 0L) {
                    val dt = ((timeNanos - lastTimeNanos) / 1_000_000_000f).coerceAtMost(0.05f)
                    engine.update(dt)
                    frameTicker = timeNanos
                }
                lastTimeNanos = timeNanos
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        engine.screenWidth = constraints.maxWidth.toFloat()
        engine.screenHeight = constraints.maxHeight.toFloat()

        Column(modifier = Modifier.fillMaxSize()) {
            // 頂部進度與得分資訊 (Compose State 即時響應，每一刀即刻跳動！)
            val progressRatio = (engine.totalCuts.toFloat() / engine.targetCutsForJuice).coerceIn(0f, 1f)
            val progressPercent = (progressRatio * 100).toInt()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            SoundManager.playClick()
                            if (engine.state != WorkshopState.RESULT && !showJuiceDialog && engine.totalCuts > 0) {
                                onGameInterrupted()
                            }
                            onBackToMenu()
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to menu"
                            )
                        }
                        Column {
                            Text(
                                text = "🍹 榨汁進度: $progressPercent% (${engine.totalCuts} / ${engine.targetCutsForJuice} 片)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (engine.comboCuts >= 3) "🔥 PERFECT 連切 x${engine.comboCuts}" else "✨ Perfect: ${engine.perfectCuts}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (engine.comboCuts >= 3) Color(0xFFFF5722) else MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${engine.score}",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Text(
                                text = "${Localization.getString("best_score", language)}: $bestScore",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(onClick = {
                            SoundManager.playClick()
                            onOpenLeaderboard()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Leaderboard,
                                contentDescription = "Leaderboard",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 榨汁機容量實時進度條
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressRatio)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF00E676), Color(0xFFFFD600), Color(0xFFFF1744))
                                )
                            )
                    )
                }
            }

            // 輸送帶與下刀互動 Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .pointerInput(engine) {
                        detectTapGestures(
                            onPress = {
                                if (engine.state != WorkshopState.SLICING) return@detectTapGestures
                                isPressing = true
                                try {
                                    tryAwaitRelease()
                                } finally {
                                    isPressing = false
                                }
                            }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val _tick = frameTicker
                    val conveyorY = size.height * 0.48f
                    val conveyorHeight = 50f

                    // 1. 繪製輸送帶軌道與動態滾動紋理
                    drawRect(
                        color = Color(0xFF37474F),
                        topLeft = Offset(0f, conveyorY),
                        size = Size(size.width, conveyorHeight)
                    )
                    for (x in -40..(size.width.toInt() + 40) step 40) {
                        val stripeX = x + beltOffset
                        drawLine(
                            color = Color(0xFF263238),
                            start = Offset(stripeX, conveyorY),
                            end = Offset(stripeX - 15f, conveyorY + conveyorHeight),
                            strokeWidth = 4f
                        )
                    }

                    // 2. 繪製輸送帶上的蔬果長條與障礙物 (高度 34f)
                    for (item in engine.items) {
                        val startX = item.x
                        val remainingWidth = (item.totalLength - item.slicedLength).coerceAtLeast(0f)
                        if (remainingWidth > 0f) {
                            if (item.type.isObstacle) {
                                // 金屬砧板障礙物
                                drawRoundRect(
                                    color = item.type.baseColor,
                                    topLeft = Offset(startX, conveyorY - 32f),
                                    size = Size(remainingWidth, 32f),
                                    cornerRadius = CornerRadius(4f, 4f)
                                )
                                drawRoundRect(
                                    color = Color.White.copy(alpha = 0.5f),
                                    topLeft = Offset(startX + 4f, conveyorY - 30f),
                                    size = Size(remainingWidth - 8f, 6f),
                                    cornerRadius = CornerRadius(2f, 2f)
                                )
                                // 螺栓金屬光斑
                                drawCircle(color = Color(0xFF37474F), radius = 3f, center = Offset(startX + 12f, conveyorY - 16f))
                                drawCircle(color = Color(0xFF37474F), radius = 3f, center = Offset(startX + remainingWidth - 12f, conveyorY - 16f))
                            } else {
                                drawRoundRect(
                                    color = item.type.baseColor,
                                    topLeft = Offset(startX, conveyorY - 34f),
                                    size = Size(remainingWidth, 34f),
                                    cornerRadius = CornerRadius(8f, 8f)
                                )
                                drawRoundRect(
                                    color = item.type.innerColor,
                                    topLeft = Offset(startX + 2f, conveyorY - 26f),
                                    size = Size(remainingWidth - 4f, 18f),
                                    cornerRadius = CornerRadius(4f, 4f)
                                )
                            }
                        }
                    }

                    // 3. 繪製掉落中切片
                    for (piece in engine.fallingPieces) {
                        rotate(piece.rotation, pivot = Offset(piece.x, piece.y)) {
                            drawCircle(
                                color = piece.color,
                                radius = 16f,
                                center = Offset(piece.x, piece.y)
                            )
                        }
                    }

                    // 4. 繪製切片工坊主廚大菜刀 (高度校正：閒置時刀刃位於 conveyorY - 60f，下切剛好觸及切板 conveyorY)
                    val knifeX = engine.bladeX
                    val knifeBaseY = conveyorY - 145f + knifeOffset.value

                    val bladePath = Path().apply {
                        moveTo(knifeX - 16f, knifeBaseY)
                        lineTo(knifeX + 16f, knifeBaseY)
                        lineTo(knifeX + 14f, knifeBaseY + 85f)
                        lineTo(knifeX - 14f, knifeBaseY + 85f)
                        close()
                    }
                    drawPath(
                        path = bladePath,
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFFECEFF1), Color(0xFFCFD8DC), Color(0xFFFFFFFF), Color(0xFF90A4AE)),
                            startX = knifeX - 16f,
                            endX = knifeX + 16f
                        )
                    )
                    // 刀刃反光鋒芒線
                    drawLine(
                        color = Color.White,
                        start = Offset(knifeX - 14f, knifeBaseY + 85f),
                        end = Offset(knifeX + 14f, knifeBaseY + 85f),
                        strokeWidth = 2.5f
                    )
                    // 木質刀柄
                    drawRoundRect(
                        color = Color(0xFF5D4037),
                        topLeft = Offset(knifeX - 10f, knifeBaseY - 36f),
                        size = Size(20f, 36f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )

                    // 5. 全螢幕榨汁機旋轉攪拌流體動畫 (進度滿 100% 時觸發，持續 1.8 秒)
                    if (engine.state == WorkshopState.BLENDING) {
                        // 沉浸半透明暗幕
                        drawRect(color = Color.Black.copy(alpha = 0.68f), size = size)

                        val blenderX = size.width / 2f
                        val blenderY = size.height * 0.45f
                        val progress = engine.blendingProgress

                        // 榨汁機透明玻璃杯身
                        val jarPath = Path().apply {
                            moveTo(blenderX - 85f, blenderY - 130f)
                            lineTo(blenderX + 85f, blenderY - 130f)
                            lineTo(blenderX + 65f, blenderY + 90f)
                            lineTo(blenderX - 65f, blenderY + 90f)
                            close()
                        }

                        // 攪拌底座
                        drawRoundRect(
                            color = Color(0xFF37474F),
                            topLeft = Offset(blenderX - 80f, blenderY + 90f),
                            size = Size(160f, 50f),
                            cornerRadius = CornerRadius(10f, 10f)
                        )
                        drawCircle(color = Color(0xFF00E676), radius = 6f, center = Offset(blenderX, blenderY + 115f))

                        // 液位自底層向上攀升
                        val liquidHeight = 200f * progress
                        val liquidTopY = (blenderY + 85f) - liquidHeight

                        clipPath(jarPath) {
                            // 漸層融合流體
                            drawRect(
                                brush = Brush.verticalGradient(
                                    listOf(Color(0xFFFF4081), Color(0xFFFFD54F), Color(0xFF00E676)),
                                    startY = liquidTopY,
                                    endY = blenderY + 85f
                                ),
                                topLeft = Offset(blenderX - 90f, liquidTopY),
                                size = Size(180f, liquidHeight + 10f)
                            )

                            // 高速旋轉漩渦氣泡與果粒微粒
                            val spinAngle = progress * 1800f
                            rotate(spinAngle, pivot = Offset(blenderX, blenderY + 20f)) {
                                for (k in 0..5) {
                                    val ra = k * (Math.PI / 3.0)
                                    val rx = blenderX + cos(ra).toFloat() * 40f
                                    val ry = (blenderY + 20f) + sin(ra).toFloat() * 18f
                                    drawCircle(
                                        color = Color.White.copy(alpha = 0.7f),
                                        radius = 4.5f,
                                        center = Offset(rx, ry)
                                    )
                                }
                            }
                        }

                        // 玻璃杯邊緣反光
                        drawPath(path = jarPath, color = Color.White.copy(alpha = 0.8f), style = Stroke(width = 3.5f))
                        drawLine(
                            color = Color.White.copy(alpha = 0.5f),
                            start = Offset(blenderX - 70f, blenderY - 110f),
                            end = Offset(blenderX - 52f, blenderY + 70f),
                            strokeWidth = 3f,
                            cap = StrokeCap.Round
                        )
                    }
                }

                // 浮動加分文字
                for (t in engine.floatingTexts) {
                    Text(
                        text = t.text,
                        color = t.color.copy(alpha = t.alpha.coerceIn(0f, 1f)),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        modifier = Modifier
                            .offset(x = (t.x - 40f).dp, y = (t.y - 20f).dp)
                            .scale(t.scale)
                    )
                }

                // 榨汁動畫階段中央提示
                if (engine.state == WorkshopState.BLENDING) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.offset(y = 150.dp)
                        ) {
                            Text(
                                text = "🌀 主廚高速真空榨汁中...",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${(engine.blendingProgress * 100).toInt()}%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFFFFD54F)
                            )
                        }
                    }
                }
            }
        }
    }

    // 果汁特調成果彈窗（補齊返回按鈕）
    if (showJuiceDialog && juiceResult != null) {
        JuiceResultDialog(
            result = juiceResult!!,
            finalScore = engine.score,
            language = language,
            onRestart = {
                showJuiceDialog = false
                engine.reset()
                juiceResult = null
                gameSessionId++
            },
            onLeaderboard = {
                showJuiceDialog = false
                onOpenLeaderboard()
            },
            onBackToMenu = {
                showJuiceDialog = false
                onBackToMenu()
            }
        )
    }
}

// =========================================================================
// 次世代日式道場案板環境、電光斬痕與有機爆汁粒子繪製模組
// =========================================================================

/**
 * 1. 繪製日式道場檜木案板底盤與沉浸竹林陽光漫射 (Japanese Dojo Hinoki Board)
 */
private fun DrawScope.drawDojoHinokiBoard(
    themeStyle: AppThemeStyle,
    particles: List<DojoFloatingParticle>
) {
    val w = size.width
    val h = size.height

    // 檜木主色深棕漸層底盤
    drawRect(
        brush = Brush.verticalGradient(
            listOf(
                Color(0xFF2E1C11),
                Color(0xFF22140C),
                Color(0xFF170D08)
            )
        ),
        size = size
    )

    // 精細縱向檜木條紋理 (Japanese Hinoki Woodgrain)
    val grainSpacing = 42f
    var gx = 0f
    while (gx < w) {
        drawLine(
            color = Color(0xFF3E2723).copy(alpha = 0.22f),
            start = Offset(gx, 0f),
            end = Offset(gx, h),
            strokeWidth = 2f
        )
        drawLine(
            color = Color(0xFF1B0F07).copy(alpha = 0.35f),
            start = Offset(gx + 2f, 0f),
            end = Offset(gx + 2f, h),
            strokeWidth = 1.5f
        )
        gx += grainSpacing
    }

    // 頂部神聖陽光漫射 (Volumetric Sunlight God-Rays)
    val rayOrigin = Offset(w * 0.5f, -60f)
    for (i in -4..4) {
        val targetX = w * 0.5f + i * (w * 0.18f)
        val rayPath = Path().apply {
            moveTo(rayOrigin.x - 20f, rayOrigin.y)
            lineTo(rayOrigin.x + 20f, rayOrigin.y)
            lineTo(targetX + 60f, h * 0.85f)
            lineTo(targetX - 60f, h * 0.85f)
            close()
        }
        drawPath(
            path = rayPath,
            brush = Brush.verticalGradient(
                listOf(
                    Color(0xFFFFECB3).copy(alpha = 0.08f),
                    Color(0xFFFFD54F).copy(alpha = 0.03f),
                    Color.Transparent
                ),
                startY = 0f,
                endY = h * 0.85f
            )
        )
    }

    // 四周微倒角厚木道場邊框
    val borderW = 12f
    // 左、右倒角飾框
    drawRect(
        brush = Brush.horizontalGradient(listOf(Color(0xFF4E342E), Color(0xFF2E1C11))),
        topLeft = Offset.Zero,
        size = Size(borderW, h)
    )
    drawRect(
        brush = Brush.horizontalGradient(listOf(Color(0xFF2E1C11), Color(0xFF4E342E))),
        topLeft = Offset(w - borderW, 0f),
        size = Size(borderW, h)
    )
    // 溫潤金光內飾線 (1.5dp)
    drawLine(
        color = Color(0xFFFFD54F).copy(alpha = 0.28f),
        start = Offset(borderW, 0f),
        end = Offset(borderW, h),
        strokeWidth = 2f
    )
    drawLine(
        color = Color(0xFFFFD54F).copy(alpha = 0.28f),
        start = Offset(w - borderW, 0f),
        end = Offset(w - borderW, h),
        strokeWidth = 2f
    )

    // 飄落櫻花瓣與翠綠竹葉微粒 (隨風飄蕩與自轉)
    for (p in particles) {
        rotate(p.rotation, pivot = Offset(p.x, p.y)) {
            if (p.isLeaf) {
                // 翠綠飄逸竹葉
                val leafPath = Path().apply {
                    moveTo(p.x, p.y - p.size)
                    cubicTo(p.x + p.size * 0.45f, p.y - p.size * 0.3f, p.x + p.size * 0.45f, p.y + p.size * 0.3f, p.x, p.y + p.size)
                    cubicTo(p.x - p.size * 0.45f, p.y + p.size * 0.3f, p.x - p.size * 0.45f, p.y - p.size * 0.3f, p.x, p.y - p.size)
                    close()
                }
                drawPath(
                    path = leafPath,
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF81C784).copy(alpha = p.alpha), Color(0xFF388E3C).copy(alpha = p.alpha * 0.8f))
                    )
                )
            } else {
                // 粉嫩櫻花瓣 (心形微凹雙瓣)
                val petalPath = Path().apply {
                    moveTo(p.x, p.y + p.size * 0.7f)
                    cubicTo(p.x - p.size * 0.8f, p.y, p.x - p.size * 0.6f, p.y - p.size * 0.7f, p.x, p.y - p.size * 0.3f)
                    cubicTo(p.x + p.size * 0.6f, p.y - p.size * 0.7f, p.x + p.size * 0.8f, p.y, p.x, p.y + p.size * 0.7f)
                    close()
                }
                drawPath(
                    path = petalPath,
                    brush = Brush.radialGradient(
                        listOf(Color(0xFFFF80AB).copy(alpha = p.alpha), Color(0xFFFF4081).copy(alpha = p.alpha * 0.6f)),
                        center = Offset(p.x, p.y),
                        radius = p.size
                    )
                )
            }
        }
    }
}

/**
 * 2. 繪製次世代晶瑩有機水滴爆汁粒子 (Juice Splash Droplets)
 * 具備拉伸有機橢圓水滴、雙層透光色暈、左上方微高光白點 (Specular Highlight)
 */
private fun DrawScope.drawHyperJuicyParticles(particles: List<JuiceParticle>) {
    for (p in particles) {
        val alpha = p.life.coerceIn(0f, 1f)
        val r = p.currentRadius
        val center = Offset(p.x, p.y)

        // 水滴外層微柔光
        drawCircle(
            color = p.color.copy(alpha = alpha * 0.35f),
            radius = r * 1.55f,
            center = center
        )

        // 水滴有機橢圓主體
        drawOval(
            brush = Brush.radialGradient(
                listOf(
                    p.color.copy(alpha = alpha * 0.95f),
                    p.color.copy(alpha = alpha * 0.7f)
                ),
                center = Offset(center.x - r * 0.25f, center.y - r * 0.25f),
                radius = r
            ),
            topLeft = Offset(center.x - r, center.y - r * 1.25f),
            size = Size(r * 2f, r * 2.5f)
        )

        // 水滴左上角晶亮微高光白點 (Specular Highlight)
        if (r >= 3f) {
            drawCircle(
                color = Color.White.copy(alpha = alpha * 0.85f),
                radius = (r * 0.28f).coerceAtLeast(1.2f),
                center = Offset(center.x - r * 0.35f, center.y - r * 0.45f)
            )
        }
    }
}

/**
 * 3. 繪製電光疾風斬痕刀光與撕裂星芒粒子 (Electric Plasma Blade Trail & Sparks)
 * 具備 4 層光學渲染：外層等離子光暈、中層電漿流光、核心純白鋒刃光芯、刀尖撕裂火花星芒
 */
private fun DrawScope.drawElectricPlasmaBladeTrail(
    trailPoints: List<Offset>,
    sparks: List<BladeSparkParticle>,
    bladeCoreColor: Color,
    bladeGlowColor: Color,
    isFever: Boolean
) {
    // 1. 繪製刀刃多層拖尾斬痕
    if (trailPoints.size >= 2) {
        val total = trailPoints.size
        for (i in 0 until total - 1) {
            val p1 = trailPoints[i]
            val p2 = trailPoints[i + 1]
            val progress = i.toFloat() / total
            val strokeW = 18f * progress + 2f

            // 第一層：外層流動等離子廣域霓虹光暈 (Bloom Aura)
            drawLine(
                color = if (isFever) Color(0xFFFF1744).copy(alpha = progress * 0.6f) else bladeGlowColor.copy(alpha = progress * 0.65f),
                start = p1,
                end = p2,
                strokeWidth = strokeW * 2.8f,
                cap = StrokeCap.Round
            )

            // 第二層：中層飽和電漿流光 (Plasma Sheen)
            drawLine(
                color = if (isFever) Color(0xFFFFD54F).copy(alpha = progress * 0.85f) else Color(0xFF00E5FF).copy(alpha = progress * 0.85f),
                start = p1,
                end = p2,
                strokeWidth = strokeW * 1.5f,
                cap = StrokeCap.Round
            )

            // 第三層：核心極致純白鋒刃光芯 (Razor-Sharp White Core)
            drawLine(
                color = bladeCoreColor.copy(alpha = progress * 0.98f),
                start = p1,
                end = p2,
                strokeWidth = strokeW * 0.7f,
                cap = StrokeCap.Round
            )
        }
    }

    // 2. 繪製刀尖撕裂火花星芒 (Sparks Particle System)
    for (sp in sparks) {
        val alpha = sp.life.coerceIn(0f, 1f)
        val c = Offset(sp.x, sp.y)
        val s = sp.size

        // 火花微光暈
        drawCircle(
            color = sp.color.copy(alpha = alpha * 0.5f),
            radius = s * 1.8f,
            center = c
        )
        // 火花核心純白十字星芒
        drawLine(
            color = Color.White.copy(alpha = alpha * 0.95f),
            start = Offset(c.x - s * 1.4f, c.y),
            end = Offset(c.x + s * 1.4f, c.y),
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White.copy(alpha = alpha * 0.95f),
            start = Offset(c.x, c.y - s * 1.4f),
            end = Offset(c.x, c.y + s * 1.4f),
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )
    }
}

// =========================================================================
// 擬真水果全面重製與真實斷裂剖面繪製模組
// =========================================================================

/**
 * 繪製水果（未切開擬真水果 vs 切開兩半斷裂剖面）
 */
private fun DrawScope.drawFruitItem(fruit: FruitItem) {
    if (!fruit.isSliced) {
        rotate(fruit.rotation, pivot = Offset(fruit.x, fruit.y)) {
            when (fruit.specialType) {
                SpecialFruitType.FUNNY_BOMB -> drawFunnyBomb(fruit)
                SpecialFruitType.FREEZE_BANANA -> drawFreezeBanana(fruit)
                SpecialFruitType.RAINBOW_WATERMELON -> drawRainbowWatermelon(fruit)
                SpecialFruitType.NONE -> drawRealisticFruit(fruit)
            }
        }
    } else {
        // 切開半塊 1（帶有剖面結構）
        val p1Center = Offset(fruit.x + fruit.piece1Offset.x, fruit.y + fruit.piece1Offset.y)
        rotate(fruit.piece1Rotation, pivot = p1Center) {
            drawFruitHalfSlice(fruit, isPiece1 = true)
        }
        // 切開半塊 2（帶有剖面結構）
        val p2Center = Offset(fruit.x + fruit.piece2Offset.x, fruit.y + fruit.piece2Offset.y)
        rotate(fruit.piece2Rotation, pivot = p2Center) {
            drawFruitHalfSlice(fruit, isPiece1 = false)
        }
    }
}

/**
 * 完整擬真水果繪製
 */
private fun DrawScope.drawRealisticFruit(fruit: FruitItem) {
    when (fruit.type) {
        FruitType.WATERMELON -> drawRealisticWatermelon(fruit)
        FruitType.BANANA -> drawRealisticBanana(fruit)
        FruitType.STRAWBERRY -> drawRealisticStrawberry(fruit)
        FruitType.PINEAPPLE -> drawRealisticPineapple(fruit)
        FruitType.KIWI -> drawRealisticKiwi(fruit)
        FruitType.DRAGON_FRUIT -> drawRealisticDragonFruit(fruit)
        FruitType.GOLDEN_APPLE -> drawRealisticGoldenApple(fruit)
    }
}

/**
 * 1. 擬真西瓜：立體球形天光漸層、墨綠交錯波浪瓜皮紋路、瓜蒂與立體月牙高光弧
 */
private fun DrawScope.drawRealisticWatermelon(fruit: FruitItem) {
    val center = Offset(fruit.x, fruit.y)
    val r = fruit.radius

    // 球體 3D 天光漸層底色 (左上受光翠綠，右下深林墨綠陰影)
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFF43A047), Color(0xFF2E7D32), Color(0xFF1B5E20)),
            center = Offset(center.x - r * 0.35f, center.y - r * 0.35f),
            radius = r * 1.15f
        ),
        radius = r,
        center = center
    )

    // 墨綠黑相間波浪西瓜條紋
    for (i in 0 until 6) {
        val angle = i * (Math.PI / 3.0)
        val x1 = center.x + cos(angle).toFloat() * r * 0.92f
        val y1 = center.y + sin(angle).toFloat() * r * 0.92f
        val x2 = center.x + cos(angle + 0.35).toFloat() * r * 0.25f
        val y2 = center.y + sin(angle + 0.35).toFloat() * r * 0.25f
        drawLine(
            color = Color(0xFF0F3813),
            start = Offset(x1, y1),
            end = Offset(x2, y2),
            strokeWidth = r * 0.18f,
            cap = StrokeCap.Round
        )
    }

    // 球體立體天光月牙弧 (次表面反射光)
    drawOval(
        color = Color.White.copy(alpha = 0.28f),
        topLeft = Offset(center.x - r * 0.65f, center.y - r * 0.65f),
        size = Size(r * 0.65f, r * 0.42f)
    )

    // 西瓜深褐果蒂
    drawCircle(color = Color(0xFF3E2723), radius = r * 0.08f, center = Offset(center.x, center.y - r * 0.94f))
}

/**
 * 2. 擬真香蕉：優雅彎月月牙輪廓、熟成金黃與果蒂微青過渡、兩端深褐果蒂、立體稜角光影
 */
private fun DrawScope.drawRealisticBanana(fruit: FruitItem) {
    val center = Offset(fruit.x, fruit.y)
    val r = fruit.radius

    val bananaPath = Path().apply {
        moveTo(center.x - r * 0.88f, center.y + r * 0.35f)
        cubicTo(
            center.x - r * 0.5f, center.y - r * 0.8f,
            center.x + r * 0.5f, center.y - r * 0.8f,
            center.x + r * 0.88f, center.y + r * 0.25f
        )
        cubicTo(
            center.x + r * 0.42f, center.y - r * 0.35f,
            center.x - r * 0.42f, center.y - r * 0.35f,
            center.x - r * 0.88f, center.y + r * 0.35f
        )
        close()
    }

    drawPath(
        path = bananaPath,
        brush = Brush.linearGradient(
            listOf(Color(0xFF81C784), Color(0xFFFFD54F), Color(0xFFFFEE58), Color(0xFFFFC107)),
            start = Offset(center.x - r, center.y),
            end = Offset(center.x + r, center.y)
        )
    )

    // 果蒂深褐尖端
    drawCircle(color = Color(0xFF4E342E), radius = r * 0.1f, center = Offset(center.x - r * 0.88f, center.y + r * 0.35f))
    drawCircle(color = Color(0xFF3E2723), radius = r * 0.08f, center = Offset(center.x + r * 0.88f, center.y + r * 0.25f))

    // 稜角立體高光線
    val ridgePath = Path().apply {
        moveTo(center.x - r * 0.78f, center.y + r * 0.22f)
        cubicTo(
            center.x - r * 0.4f, center.y - r * 0.58f,
            center.x + r * 0.4f, center.y - r * 0.58f,
            center.x + r * 0.78f, center.y + r * 0.12f
        )
    }
    drawPath(path = ridgePath, color = Color.White.copy(alpha = 0.45f), style = Stroke(width = 3.5f))
}

/**
 * 3. 擬真草莓：立體水滴心形輪廓、鮮紅向深紅漸層、5瓣星形立體綠萼、金色微凹小種子
 */
private fun DrawScope.drawRealisticStrawberry(fruit: FruitItem) {
    val center = Offset(fruit.x, fruit.y)
    val r = fruit.radius

    val berryPath = Path().apply {
        moveTo(center.x, center.y + r * 0.98f) // 底部圓尖
        cubicTo(
            center.x - r * 0.98f, center.y + r * 0.4f,
            center.x - r * 0.88f, center.y - r * 0.68f,
            center.x, center.y - r * 0.68f
        )
        cubicTo(
            center.x + r * 0.88f, center.y - r * 0.68f,
            center.x + r * 0.98f, center.y + r * 0.4f,
            center.x, center.y + r * 0.98f
        )
        close()
    }

    drawPath(
        path = berryPath,
        brush = Brush.radialGradient(
            listOf(Color(0xFFFF5252), Color(0xFFD50000), Color(0xFF880E4F)),
            center = Offset(center.x - r * 0.25f, center.y - r * 0.15f),
            radius = r * 1.18f
        )
    )

    // 金黃微凹草莓種子點
    val seedOffsets = listOf(
        Offset(0f, 0f),
        Offset(-0.4f, -0.3f), Offset(0.4f, -0.3f),
        Offset(-0.45f, 0.2f), Offset(0.45f, 0.2f),
        Offset(-0.25f, 0.55f), Offset(0.25f, 0.55f),
        Offset(0f, 0.75f), Offset(0f, -0.45f)
    )
    for (so in seedOffsets) {
        val sx = center.x + so.x * r * 0.75f
        val sy = center.y + so.y * r * 0.75f
        drawCircle(color = Color(0xFF4A0000), radius = 3.8f, center = Offset(sx + 1f, sy + 1f))
        drawCircle(color = Color(0xFFFFD54F), radius = 2.8f, center = Offset(sx, sy))
        drawCircle(color = Color.White.copy(alpha = 0.8f), radius = 1.0f, center = Offset(sx - 0.8f, sy - 0.8f))
    }

    // 頂部 5 瓣星形立體綠萼蒂頭
    for (i in 0 until 5) {
        val leafAngle = i * (Math.PI * 2 / 5.0) - Math.PI / 2.0
        val lx = center.x + cos(leafAngle).toFloat() * r * 0.58f
        val ly = center.y - r * 0.65f + sin(leafAngle).toFloat() * r * 0.38f
        drawLine(
            color = Color(0xFF43A047),
            start = Offset(center.x, center.y - r * 0.65f),
            end = Offset(lx, ly),
            strokeWidth = 6.5f,
            cap = StrokeCap.Round
        )
    }
}

/**
 * 4. 擬真鳳梨：卵形金黃果身、交錯立體蜂巢鱗甲、頂部層疊鋸齒墨綠葉冠
 */
private fun DrawScope.drawRealisticPineapple(fruit: FruitItem) {
    val center = Offset(fruit.x, fruit.y)
    val r = fruit.radius

    // 卵形果體
    drawRoundRect(
        brush = Brush.verticalGradient(
            listOf(Color(0xFFFFB300), Color(0xFFFF8F00), Color(0xFFE65100))
        ),
        topLeft = Offset(center.x - r * 0.75f, center.y - r * 0.8f),
        size = Size(r * 1.5f, r * 1.7f),
        cornerRadius = CornerRadius(r * 0.7f, r * 0.8f)
    )

    // 交叉菱形蜂巢網格紋理
    for (row in -3..3) {
        val y = center.y + row * (r * 0.24f)
        drawLine(
            color = Color(0xFFBF360C),
            start = Offset(center.x - r * 0.65f, y - r * 0.2f),
            end = Offset(center.x + r * 0.65f, y + r * 0.2f),
            strokeWidth = 3f
        )
        drawLine(
            color = Color(0xFFBF360C),
            start = Offset(center.x - r * 0.65f, y + r * 0.2f),
            end = Offset(center.x + r * 0.65f, y - r * 0.2f),
            strokeWidth = 3f
        )
        // 蜂巢中心微凸金黃高光點
        drawCircle(color = Color(0xFFFFEE58), radius = 2.5f, center = Offset(center.x, y))
    }

    // 頂部層疊墨綠鋸齒葉冠
    for (i in -2..2) {
        val leafPath = Path().apply {
            val bx = center.x + i * (r * 0.25f)
            moveTo(bx, center.y - r * 0.75f)
            lineTo(bx + i * (r * 0.16f), center.y - r * 1.38f)
            lineTo(bx + 7f, center.y - r * 0.75f)
            close()
        }
        drawPath(path = leafPath, color = Color(0xFF2E7D32))
    }
}

/**
 * 5. 擬真奇異果：棕褐絨毛外皮、立體光澤、細密絨毛斑點、深色果蒂
 */
private fun DrawScope.drawRealisticKiwi(fruit: FruitItem) {
    val center = Offset(fruit.x, fruit.y)
    val r = fruit.radius

    // 橢圓絨毛外皮
    drawRoundRect(
        brush = Brush.radialGradient(
            listOf(Color(0xFF8D6E63), Color(0xFF6D4C41), Color(0xFF4E342E)),
            center = Offset(center.x - r * 0.25f, center.y - r * 0.25f),
            radius = r * 1.1f
        ),
        topLeft = Offset(center.x - r * 0.85f, center.y - r),
        size = Size(r * 1.7f, r * 2f),
        cornerRadius = CornerRadius(r * 0.85f, r)
    )
    // 絨毛細斑光點
    for (i in 0 until 14) {
        val a = i * (Math.PI / 7.0)
        val fx = center.x + cos(a).toFloat() * r * 0.62f
        val fy = center.y + sin(a).toFloat() * r * 0.72f
        drawCircle(color = Color(0xFF3E2723), radius = 2.4f, center = Offset(fx, fy))
    }
    drawCircle(color = Color(0xFF271510), radius = r * 0.12f, center = Offset(center.x, center.y - r * 0.95f))
}

/**
 * 6. 擬真火龍果：概念圖焦點！立體洋紅果體、多層外翹翡翠尖瓣龍鱗（根部洋紅漸層至尖端青翠）
 */
private fun DrawScope.drawRealisticDragonFruit(fruit: FruitItem) {
    val center = Offset(fruit.x, fruit.y)
    val r = fruit.radius

    // 洋紅果體 (天光飽和徑向漸層)
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFFFF4081), Color(0xFFE91E63), Color(0xFF880E4F)),
            center = Offset(center.x - r * 0.22f, center.y - r * 0.22f),
            radius = r * 1.05f
        ),
        radius = r * 0.9f,
        center = center
    )

    // 外翻翡翠龍鱗尖瓣 (6片，外翹立體感)
    for (i in 0 until 6) {
        val angle = i * (Math.PI / 3.0)
        val sx = center.x + cos(angle).toFloat() * r * 0.8f
        val sy = center.y + sin(angle).toFloat() * r * 0.8f
        val tipX = center.x + cos(angle).toFloat() * (r * 1.3f)
        val tipY = center.y + sin(angle).toFloat() * (r * 1.3f)

        val scalePath = Path().apply {
            moveTo(sx - 11f, sy)
            lineTo(tipX, tipY)
            lineTo(sx + 11f, sy)
            close()
        }
        drawPath(
            path = scalePath,
            brush = Brush.linearGradient(
                listOf(Color(0xFFE91E63), Color(0xFF00E676)),
                start = Offset(sx, sy),
                end = Offset(tipX, tipY)
            )
        )
    }
}

/**
 * 7. 擬真金蘋果：奢華純金金屬高光弧、蘋果雙凹造型、神聖星芒環繞
 */
private fun DrawScope.drawRealisticGoldenApple(fruit: FruitItem) {
    val center = Offset(fruit.x, fruit.y)
    val r = fruit.radius

    val applePath = Path().apply {
        moveTo(center.x, center.y - r * 0.7f)
        cubicTo(center.x - r * 0.9f, center.y - r * 0.95f, center.x - r * 1.1f, center.y + r * 0.3f, center.x - r * 0.5f, center.y + r * 0.95f)
        cubicTo(center.x - r * 0.2f, center.y + r * 0.85f, center.x + r * 0.2f, center.y + r * 0.85f, center.x + r * 0.5f, center.y + r * 0.95f)
        cubicTo(center.x + r * 1.1f, center.y + r * 0.3f, center.x + r * 0.9f, center.y - r * 0.95f, center.x, center.y - r * 0.7f)
        close()
    }

    drawPath(
        path = applePath,
        brush = Brush.radialGradient(
            listOf(Color(0xFFFFFFF0), Color(0xFFFFD54F), Color(0xFFFFB300), Color(0xFFE65100)),
            center = Offset(center.x - r * 0.28f, center.y - r * 0.28f),
            radius = r * 1.25f
        )
    )

    // 果梗與小綠葉
    drawLine(
        color = Color(0xFF5D4037),
        start = Offset(center.x, center.y - r * 0.7f),
        end = Offset(center.x + r * 0.15f, center.y - r * 1.05f),
        strokeWidth = 4.5f,
        cap = StrokeCap.Round
    )
    drawOval(
        color = Color(0xFF66BB6A),
        topLeft = Offset(center.x + r * 0.08f, center.y - r * 1.1f),
        size = Size(r * 0.35f, r * 0.2f)
    )

    // 金屬高光星芒
    drawCircle(
        color = Color.White.copy(alpha = 0.85f),
        radius = r * 0.22f,
        center = Offset(center.x - r * 0.35f, center.y - r * 0.3f)
    )
}

/**
 * 搞怪炸彈：厚重霧黑金屬球體、金色引信套筒、麻繩引信跳動火花
 */
private fun DrawScope.drawFunnyBomb(fruit: FruitItem) {
    val center = Offset(fruit.x, fruit.y)
    val r = fruit.radius

    // 厚實黑金屬球體
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFF546E7A), Color(0xFF263238), Color(0xFF000000)),
            center = Offset(center.x - r * 0.28f, center.y - r * 0.28f),
            radius = r
        ),
        radius = r,
        center = center
    )

    // 金屬引信基座
    drawRoundRect(
        color = Color(0xFFFFB300),
        topLeft = Offset(center.x - 10f, center.y - r - 8f),
        size = Size(20f, 10f),
        cornerRadius = CornerRadius(3f, 3f)
    )

    // 麻繩引信
    drawLine(
        color = Color(0xFF8D6E63),
        start = Offset(center.x, center.y - r - 8f),
        end = Offset(center.x + 18f, center.y - r - 26f),
        strokeWidth = 6f,
        cap = StrokeCap.Round
    )

    // 燃燒火花 (Sparks)
    drawCircle(color = Color(0xFFFF5722), radius = 11f, center = Offset(center.x + 18f, center.y - r - 26f))
    drawCircle(color = Color(0xFFFFEB3B), radius = 6f, center = Offset(center.x + 18f, center.y - r - 26f))
}

private fun DrawScope.drawFreezeBanana(fruit: FruitItem) {
    val center = Offset(fruit.x, fruit.y)
    drawCircle(color = Color(0x6680D8FF), radius = fruit.radius * 1.25f, center = center)
    drawRealisticBanana(fruit)
}

private fun DrawScope.drawRainbowWatermelon(fruit: FruitItem) {
    val center = Offset(fruit.x, fruit.y)
    drawCircle(
        brush = Brush.sweepGradient(
            listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Magenta, Color.Red),
            center = center
        ),
        radius = fruit.radius * 1.2f,
        center = center
    )
    drawRealisticWatermelon(fruit)
}

/**
 * 水果切開後真實斷裂剖面半塊 (Fruit Half Slice)
 * 雙層果肉厚度、瓜皮白層、晶亮水滴高光黑籽、果汁溢出反光
 */
private fun DrawScope.drawFruitHalfSlice(fruit: FruitItem, isPiece1: Boolean) {
    val offset = if (isPiece1) fruit.piece1Offset else fruit.piece2Offset
    val center = Offset(fruit.x + offset.x, fruit.y + offset.y)
    val r = fruit.radius
    val alpha = fruit.alpha.coerceIn(0f, 1f)
    val sign = if (isPiece1) 1f else -1f

    when (fruit.type) {
        FruitType.BANANA -> {
            // 🍌 香蕉專屬斷截：弧形長條香蕉果身 + 斜切橢圓截面
            val bananaHalfPath = Path().apply {
                moveTo(center.x - r * 0.85f * sign, center.y + r * 0.32f)
                cubicTo(
                    center.x - r * 0.45f * sign, center.y - r * 0.65f,
                    center.x, center.y - r * 0.65f,
                    center.x + r * 0.15f * sign, center.y - r * 0.15f
                )
                lineTo(center.x - r * 0.05f * sign, center.y + r * 0.38f)
                cubicTo(
                    center.x - r * 0.35f * sign, center.y - r * 0.22f,
                    center.x - r * 0.68f * sign, center.y + r * 0.12f,
                    center.x - r * 0.85f * sign, center.y + r * 0.32f
                )
                close()
            }
            drawPath(
                path = bananaHalfPath,
                brush = Brush.linearGradient(
                    listOf(Color(0xFF81C784), Color(0xFFFFD54F), Color(0xFFFFEE58), Color(0xFFFFD54F)),
                    start = Offset(center.x - r * sign, center.y),
                    end = Offset(center.x + r * sign, center.y)
                ),
                alpha = alpha
            )

            // 斜切果肉斷面 (奶油象牙白)
            drawOval(
                brush = Brush.radialGradient(
                    listOf(Color(0xFFFFFFF0), Color(0xFFFFF9C4), Color(0xFFFFF176)),
                    center = Offset(center.x + r * 0.05f * sign, center.y + r * 0.1f),
                    radius = r * 0.42f
                ),
                topLeft = Offset(center.x + r * 0.05f * sign - r * 0.35f, center.y + r * 0.1f - r * 0.28f),
                size = Size(r * 0.7f, r * 0.56f),
                alpha = alpha
            )

            // 中心三瓣 Y 字形果核與黑芝麻微粒
            val coreCenter = Offset(center.x + r * 0.05f * sign, center.y + r * 0.1f)
            for (ang in listOf(-Math.PI / 2.0, Math.PI / 6.0, Math.PI * 5.0 / 6.0)) {
                val cx = coreCenter.x + cos(ang).toFloat() * r * 0.16f
                val cy = coreCenter.y + sin(ang).toFloat() * r * 0.12f
                drawLine(
                    color = Color(0xFFD7CCC8).copy(alpha = alpha * 0.9f),
                    start = coreCenter,
                    end = Offset(cx, cy),
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = Color(0xFF5D4037).copy(alpha = alpha * 0.85f),
                    radius = 2.8f,
                    center = Offset(cx, cy)
                )
            }
        }

        FruitType.WATERMELON -> {
            // 🍉 西瓜雙瓣：深綠瓜皮外緣 + 白綠皮層 + 鮮紅沙瓤 + 水滴高光黑瓜籽 + 溢出果汁光點
            val halfPath = Path().apply {
                arcTo(
                    rect = Rect(center.x - r, center.y - r, center.x + r, center.y + r),
                    startAngleDegrees = if (isPiece1) 0f else 180f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = true
                )
                close()
            }
            drawPath(path = halfPath, color = Color(0xFF1B5E20).copy(alpha = alpha))

            // 瓜皮墨綠條紋
            for (i in -3..3) {
                val stripeX = center.x + i * (r * 0.28f)
                drawLine(
                    color = Color(0xFF0F3813).copy(alpha = alpha * 0.85f),
                    start = Offset(stripeX, center.y),
                    end = Offset(stripeX, center.y + sign * r * 0.92f),
                    strokeWidth = 5f
                )
            }

            // 白綠瓜皮厚層 (厚度立體感)
            val innerPath = Path().apply {
                arcTo(
                    rect = Rect(center.x - r * 0.88f, center.y - r * 0.88f, center.x + r * 0.88f, center.y + r * 0.88f),
                    startAngleDegrees = if (isPiece1) 0f else 180f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = true
                )
                close()
            }
            drawPath(path = innerPath, color = Color(0xFFE8F5E9).copy(alpha = alpha))

            // 鮮紅多汁沙瓤果肉
            val pulpPath = Path().apply {
                arcTo(
                    rect = Rect(center.x - r * 0.78f, center.y - r * 0.78f, center.x + r * 0.78f, center.y + r * 0.78f),
                    startAngleDegrees = if (isPiece1) 0f else 180f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = true
                )
                close()
            }
            drawPath(
                path = pulpPath,
                brush = Brush.radialGradient(
                    listOf(Color(0xFFFF2A48), Color(0xFFEF5350), Color(0xFFD32F2F)),
                    center = center,
                    radius = r * 0.85f
                ),
                alpha = alpha
            )

            // 淚滴狀黑西瓜籽 + 高光點 (概念圖細節)
            for (i in 1..4) {
                val seedX = center.x + (i - 2.5f) * (r * 0.32f)
                val seedY = center.y + sign * r * 0.38f
                drawOval(
                    color = Color(0xFF1A1A1A).copy(alpha = alpha),
                    topLeft = Offset(seedX - 3.5f, seedY - 5f),
                    size = Size(7f, 10f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = alpha * 0.85f),
                    radius = 1.4f,
                    center = Offset(seedX - 1.2f, seedY - 2.2f)
                )
            }

            // 溢出晶亮果汁反光小亮點
            drawCircle(
                color = Color.White.copy(alpha = alpha * 0.65f),
                radius = 3.2f,
                center = Offset(center.x + r * 0.2f, center.y + sign * r * 0.15f)
            )
        }

        FruitType.STRAWBERRY -> {
            // 🍓 草莓斷面：倒圓錐心形斷面 + 放射狀果髓紋理
            val berryPath = Path().apply {
                if (isPiece1) {
                    moveTo(center.x - r * 0.8f, center.y)
                    cubicTo(
                        center.x - r * 0.85f, center.y - r * 0.7f,
                        center.x, center.y - r * 0.7f,
                        center.x, center.y - r * 0.4f
                    )
                    cubicTo(
                        center.x, center.y - r * 0.7f,
                        center.x + r * 0.85f, center.y - r * 0.7f,
                        center.x + r * 0.8f, center.y
                    )
                    close()
                } else {
                    moveTo(center.x - r * 0.8f, center.y)
                    cubicTo(
                        center.x - r * 0.7f, center.y + r * 0.5f,
                        center.x - r * 0.3f, center.y + r * 0.95f,
                        center.x, center.y + r * 0.95f
                    )
                    cubicTo(
                        center.x + r * 0.3f, center.y + r * 0.95f,
                        center.x + r * 0.7f, center.y + r * 0.5f,
                        center.x + r * 0.8f, center.y
                    )
                    close()
                }
            }
            drawPath(path = berryPath, color = Color(0xFFD32F2F).copy(alpha = alpha))

            // 淡粉紅果肉層
            val innerBerry = Path().apply {
                if (isPiece1) {
                    moveTo(center.x - r * 0.65f, center.y)
                    cubicTo(center.x - r * 0.68f, center.y - r * 0.55f, center.x, center.y - r * 0.55f, center.x, center.y - r * 0.32f)
                    cubicTo(center.x, center.y - r * 0.55f, center.x + r * 0.68f, center.y - r * 0.55f, center.x + r * 0.65f, center.y)
                    close()
                } else {
                    moveTo(center.x - r * 0.65f, center.y)
                    cubicTo(center.x - r * 0.55f, center.y + r * 0.4f, center.x - r * 0.2f, center.y + r * 0.75f, center.x, center.y + r * 0.75f)
                    cubicTo(center.x + r * 0.2f, center.y + r * 0.75f, center.x + r * 0.55f, center.y + r * 0.4f, center.x + r * 0.65f, center.y)
                    close()
                }
            }
            drawPath(path = innerBerry, color = Color(0xFFFFCDD2).copy(alpha = alpha))

            // 白色放射狀果髓紋
            for (i in -3..3) {
                val spokeX = center.x + i * (r * 0.18f)
                val targetY = if (isPiece1) center.y - r * 0.45f else center.y + r * 0.55f
                drawLine(
                    color = Color.White.copy(alpha = alpha * 0.75f),
                    start = Offset(center.x, center.y),
                    end = Offset(spokeX, targetY),
                    strokeWidth = 2.2f,
                    cap = StrokeCap.Round
                )
            }
        }

        FruitType.KIWI -> {
            // 🥝 奇異果剖面：棕褐毛茸外皮 + 翠綠透光果肉 + 乳白果心 + 放射黑籽圈
            val halfPath = Path().apply {
                arcTo(
                    rect = Rect(center.x - r * 0.88f, center.y - r, center.x + r * 0.88f, center.y + r),
                    startAngleDegrees = if (isPiece1) 0f else 180f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = true
                )
                close()
            }
            drawPath(path = halfPath, color = Color(0xFF6D4C41).copy(alpha = alpha))

            // 翡翠翠綠果肉
            val pulpPath = Path().apply {
                arcTo(
                    rect = Rect(center.x - r * 0.78f, center.y - r * 0.9f, center.x + r * 0.78f, center.y + r * 0.9f),
                    startAngleDegrees = if (isPiece1) 0f else 180f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = true
                )
                close()
            }
            drawPath(path = pulpPath, color = Color(0xFF81C784).copy(alpha = alpha))

            // 乳白果芯
            drawOval(
                color = Color(0xFFF1F8E9).copy(alpha = alpha),
                topLeft = Offset(center.x - r * 0.22f, center.y + (if (isPiece1) 0f else -r * 0.35f)),
                size = Size(r * 0.44f, r * 0.35f)
            )

            // 放射狀黑籽環 (10顆)
            for (i in 0 until 10) {
                val a = (i * (Math.PI / 9.0)) + (if (isPiece1) 0.0 else Math.PI)
                val sx = center.x + cos(a).toFloat() * (r * 0.42f)
                val sy = center.y + sin(a).toFloat() * (r * 0.38f)
                drawLine(
                    color = Color.White.copy(alpha = alpha * 0.5f),
                    start = Offset(center.x, center.y),
                    end = Offset(sx, sy),
                    strokeWidth = 1.5f
                )
                drawCircle(color = Color(0xFF212121).copy(alpha = alpha), radius = 2.8f, center = Offset(sx, sy))
            }
        }

        FruitType.DRAGON_FRUIT -> {
            // 🐉 火龍果半塊：洋紅龍鱗外皮 + 雪白晶瑩果肉 + 密布黑芝麻籽
            val halfPath = Path().apply {
                arcTo(
                    rect = Rect(center.x - r * 0.9f, center.y - r * 0.9f, center.x + r * 0.9f, center.y + r * 0.9f),
                    startAngleDegrees = if (isPiece1) 0f else 180f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = true
                )
                close()
            }
            drawPath(path = halfPath, color = Color(0xFFC2185B).copy(alpha = alpha))

            // 外翻翠綠鱗尖
            for (i in 0..3) {
                val angle = (i * (Math.PI / 3.0)) + (if (isPiece1) 0.0 else Math.PI)
                val lx = center.x + cos(angle).toFloat() * r * 0.9f
                val ly = center.y + sin(angle).toFloat() * r * 0.9f
                drawCircle(color = Color(0xFF43A047).copy(alpha = alpha), radius = 6f, center = Offset(lx, ly))
            }

            // 雪白果肉
            val pulpPath = Path().apply {
                arcTo(
                    rect = Rect(center.x - r * 0.78f, center.y - r * 0.78f, center.x + r * 0.78f, center.y + r * 0.78f),
                    startAngleDegrees = if (isPiece1) 0f else 180f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = true
                )
                close()
            }
            drawPath(path = pulpPath, color = Color(0xFFFAFAFA).copy(alpha = alpha))

            // 散佈芝麻黑點
            val seedOffsets = listOf(
                Offset(-0.4f, 0.2f), Offset(-0.2f, 0.4f), Offset(0f, 0.3f), Offset(0.3f, 0.2f),
                Offset(-0.35f, 0.55f), Offset(0.15f, 0.5f), Offset(0.35f, 0.45f),
                Offset(-0.1f, 0.65f), Offset(0.25f, 0.65f)
            )
            for (so in seedOffsets) {
                val kx = center.x + so.x * r * 0.85f
                val ky = center.y + sign * so.y * r * 0.85f
                drawCircle(color = Color(0xFF212121).copy(alpha = alpha), radius = 2.4f, center = Offset(kx, ky))
            }
        }

        FruitType.PINEAPPLE -> {
            // 🍍 鳳梨切片：金黃鱗格皮層 + 亮黃果肉旋轉纖維紋 + 緊密圓形果心
            val halfPath = Path().apply {
                arcTo(
                    rect = Rect(center.x - r * 0.85f, center.y - r * 0.85f, center.x + r * 0.85f, center.y + r * 0.85f),
                    startAngleDegrees = if (isPiece1) 0f else 180f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = true
                )
                close()
            }
            drawPath(path = halfPath, color = Color(0xFFFFA000).copy(alpha = alpha))

            val pulpPath = Path().apply {
                arcTo(
                    rect = Rect(center.x - r * 0.75f, center.y - r * 0.75f, center.x + r * 0.75f, center.y + r * 0.75f),
                    startAngleDegrees = if (isPiece1) 0f else 180f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = true
                )
                close()
            }
            drawPath(path = pulpPath, color = Color(0xFFFFEE58).copy(alpha = alpha))

            // 旋轉螺旋果肉放射纖維
            for (i in 0 until 8) {
                val a = (i * (Math.PI / 7.0)) + (if (isPiece1) 0.0 else Math.PI)
                val fx = center.x + cos(a).toFloat() * (r * 0.65f)
                val fy = center.y + sin(a).toFloat() * (r * 0.65f)
                drawLine(
                    color = Color(0xFFFFD54F).copy(alpha = alpha * 0.7f),
                    start = Offset(center.x, center.y),
                    end = Offset(fx, fy),
                    strokeWidth = 2.5f
                )
            }
            drawCircle(color = Color(0xFFFFF59D).copy(alpha = alpha), radius = r * 0.22f, center = Offset(center.x, center.y + sign * r * 0.15f))
        }

        FruitType.GOLDEN_APPLE -> {
            // 🍏 金蘋果剖面：耀眼奢華金皮 + 象牙白果肉 + 星狀果核室與深褐種子
            val appleHalf = Path().apply {
                arcTo(
                    rect = Rect(center.x - r * 0.85f, center.y - r * 0.85f, center.x + r * 0.85f, center.y + r * 0.85f),
                    startAngleDegrees = if (isPiece1) 0f else 180f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = true
                )
                close()
            }
            drawPath(
                path = appleHalf,
                brush = Brush.radialGradient(
                    listOf(Color(0xFFFFFDE7), Color(0xFFFFD700), Color(0xFFFFA000)),
                    center = center,
                    radius = r
                ),
                alpha = alpha
            )

            // 象牙白果肉
            val pulpPath = Path().apply {
                arcTo(
                    rect = Rect(center.x - r * 0.76f, center.y - r * 0.76f, center.x + r * 0.76f, center.y + r * 0.76f),
                    startAngleDegrees = if (isPiece1) 0f else 180f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = true
                )
                close()
            }
            drawPath(path = pulpPath, color = Color(0xFFFFFDE7).copy(alpha = alpha))

            // 星形果核室
            drawCircle(color = Color(0xFFFFE082).copy(alpha = alpha * 0.85f), radius = r * 0.2f, center = Offset(center.x, center.y + sign * r * 0.2f))
            drawOval(
                color = Color(0xFF3E2723).copy(alpha = alpha),
                topLeft = Offset(center.x - 7f, center.y + sign * r * 0.2f - 4f),
                size = Size(6f, 9f)
            )
            drawOval(
                color = Color(0xFF3E2723).copy(alpha = alpha),
                topLeft = Offset(center.x + 2f, center.y + sign * r * 0.2f - 4f),
                size = Size(6f, 9f)
            )
        }
    }
}

/**
 * 繪製 ECG 心電圖波形
 */
private fun DrawScope.drawEcgWave(phase: Float, isCritical: Boolean) {
    val path = Path()
    val width = size.width
    val height = size.height
    val midY = height / 2f

    val color = if (isCritical) Color(0xFFEF5350) else Color(0xFF00E676)

    path.moveTo(0f, midY)
    val pulseX = width * phase
    for (x in 0..width.toInt()) {
        val xf = x.toFloat()
        val dist = xf - pulseX
        val y = if (dist in -10f..10f) {
            when {
                dist < -4f -> midY + 6f
                dist < 2f -> midY - 12f
                else -> midY + 8f
            }
        } else {
            midY
        }
        path.lineTo(xf, y)
    }

    drawPath(path = path, color = color, style = Stroke(width = 2.5f, cap = StrokeCap.Round))
}

/**
 * 繪製心跳立體浮點跳動心臟 (最後 15 秒)
 * - 寬度動態自適應佔螢幕寬度 65% 以上 (大於 60% 規格需求)
 * - 提高透明度 (降低 alpha 至 0.04f ~ 0.12f)，採用由中心向外柔和消融羽化的徑向漸層
 * - 營造深沉迫切的心跳臨場氛圍，柔美朦朧且絕不遮蔽前景水果與手勢視線
 */
private fun DrawScope.drawHeartbeatPulsingArt(center: Offset, scale: Float, alpha: Float) {
    val heartWidth = size.width * 0.65f
    val r = (heartWidth / 2.2f) * scale
    val path = Path().apply {
        moveTo(center.x, center.y - r * 0.25f)
        cubicTo(center.x - r * 0.6f, center.y - r * 0.8f, center.x - r * 1.1f, center.y - r * 0.2f, center.x - r * 1.1f, center.y + r * 0.25f)
        cubicTo(center.x - r * 1.1f, center.y + r * 0.7f, center.x - r * 0.5f, center.y + r * 1.1f, center.x, center.y + r * 1.4f)
        cubicTo(center.x + r * 0.5f, center.y + r * 1.1f, center.x + r * 1.1f, center.y + r * 0.7f, center.x + r * 1.1f, center.y + r * 0.25f)
        cubicTo(center.x + r * 1.1f, center.y - r * 0.2f, center.x + r * 0.6f, center.y - r * 0.8f, center.x, center.y - r * 0.25f)
        close()
    }
    val heartAlpha = alpha.coerceIn(0.04f, 0.12f)
    // 柔和羽化徑向漸層：核心柔光、邊緣漸層消融至全透明，營造半透明朦朧心跳氛圍
    drawPath(
        path = path,
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFE53935).copy(alpha = heartAlpha),
                Color(0xFFD32F2F).copy(alpha = heartAlpha * 0.5f),
                Color(0xFFC62828).copy(alpha = 0f)
            ),
            center = center,
            radius = r * 1.35f
        )
    )
    // 外層極淡輪廓微光
    drawPath(
        path = path,
        color = Color(0xFFFF5252).copy(alpha = heartAlpha * 0.6f),
        style = Stroke(width = 2f)
    )
}

/**
 * 墨水噴濺視野遮罩 (炸彈被切中後覆蓋 2 秒)
 */
private fun DrawScope.drawInkSplatterMask(size: Size, alpha: Float) {
    val inkColor = Color(0xFF1A1A1A).copy(alpha = (alpha * 0.85f).coerceIn(0f, 0.85f))
    drawCircle(color = inkColor, radius = size.width * 0.38f, center = Offset(size.width * 0.35f, size.height * 0.4f))
    drawCircle(color = inkColor, radius = size.width * 0.3f, center = Offset(size.width * 0.68f, size.height * 0.55f))
    drawCircle(color = inkColor, radius = size.width * 0.25f, center = Offset(size.width * 0.5f, size.height * 0.3f))
}

/**
 * 水果結算對話框（手遊 3 星榮譽成就面板）
 */
@Composable
private fun FruitResultDialog(
    score: Int,
    bestScore: Int,
    totalSliced: Int,
    maxCombo: Int,
    language: AppLanguage,
    onRestart: () -> Unit,
    onLeaderboard: () -> Unit,
    onBackToMenu: () -> Unit
) {
    val isNewRecord = score > bestScore && score > 0
    val starCount = when {
        score >= 1200 -> 3
        score >= 500 -> 2
        else -> 1
    }

    AlertDialog(
        onDismissRequest = onRestart,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 3 星榮譽評級展示
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..3) {
                        val isLit = i <= starCount
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = if (isLit) Color(0xFFFFD54F) else Color.Gray.copy(alpha = 0.35f),
                            modifier = Modifier.size(if (i == 2) 38.dp else 30.dp)
                        )
                        if (i < 3) Spacer(modifier = Modifier.width(6.dp))
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isNewRecord) "🎉 " + Localization.getString("new_record", language) else Localization.getString("stage_completed", language),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 燙金 3D 浮雕得分
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF2E1C11).copy(alpha = 0.9f),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFFD54F).copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = Localization.getString("final_score", language),
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color(0xFFFFE082).copy(alpha = 0.8f),
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$score",
                            fontWeight = FontWeight.Black,
                            fontSize = 32.sp,
                            color = Color(0xFFFFD54F)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 數據統計晶片卡片
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "🍉 切削數量", style = MaterialTheme.typography.labelSmall)
                            Text(text = "$totalSliced", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "🔥 最高連擊", style = MaterialTheme.typography.labelSmall)
                            Text(text = "$maxCombo", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }

                if (isNewRecord) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⭐ " + Localization.getString("record_broken", language),
                        color = Color(0xFFD32F2F),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 返回模式選單按鈕
                OutlinedButton(
                    onClick = onBackToMenu,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = Localization.getString("back_to_mode_menu", language), fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onRestart,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = Localization.getString("play_again", language), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onLeaderboard,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = Localization.getString("leaderboard_title", language), fontWeight = FontWeight.Bold)
            }
        }
    )
}

/**
 * 切片工坊特調漸層果汁成果展彈窗（包含 3 星評級與返回按鈕）
 */
@Composable
private fun JuiceResultDialog(
    result: JuiceResult,
    finalScore: Int,
    language: AppLanguage,
    onRestart: () -> Unit,
    onLeaderboard: () -> Unit,
    onBackToMenu: () -> Unit
) {
    var canInteract by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(1000L)
        canInteract = true
    }

    AlertDialog(
        onDismissRequest = {
            if (canInteract) onRestart()
        },
        properties = DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = false
        ),
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🍹 " + result.customTitleZh,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "主廚評級: ${result.grade} 級特調",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFFFF9800)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Canvas 精美果汁玻璃杯
                Canvas(modifier = Modifier.size(width = 120.dp, height = 150.dp)) {
                    val glassW = 80f
                    val glassH = 125f
                    val startX = (size.width - glassW) / 2f
                    val startY = 16f

                    val glassPath = Path().apply {
                        moveTo(startX, startY)
                        lineTo(startX + glassW, startY)
                        lineTo(startX + glassW - 8f, startY + glassH)
                        lineTo(startX + 8f, startY + glassH)
                        close()
                    }

                    clipPath(glassPath) {
                        drawRect(
                            brush = Brush.verticalGradient(result.gradientColors),
                            topLeft = Offset(startX, startY + 12f),
                            size = Size(glassW, glassH - 12f)
                        )

                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.45f),
                            topLeft = Offset(startX + 14f, startY + 28f),
                            size = Size(22f, 20f),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.45f),
                            topLeft = Offset(startX + 44f, startY + 52f),
                            size = Size(20f, 18f),
                            cornerRadius = CornerRadius(4f, 4f)
                        )

                        val bubbleOffsets = listOf(
                            Offset(startX + 26f, startY + 75f),
                            Offset(startX + 50f, startY + 90f),
                            Offset(startX + 36f, startY + 42f),
                            Offset(startX + 58f, startY + 32f)
                        )
                        for (bo in bubbleOffsets) {
                            drawCircle(color = Color.White.copy(alpha = 0.75f), radius = 2.8f, center = bo)
                        }
                    }

                    drawLine(
                        color = Color(0xFFFF4081),
                        start = Offset(startX + 28f, startY + 85f),
                        end = Offset(startX + glassW + 14f, startY - 12f),
                        strokeWidth = 6.5f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(startX + 28f, startY + 85f),
                        end = Offset(startX + glassW + 14f, startY - 12f),
                        strokeWidth = 2.5f,
                        cap = StrokeCap.Round
                    )

                    val garnishColor = result.garnishType.baseColor
                    val garnishInner = result.garnishType.innerColor
                    drawCircle(color = garnishColor, radius = 13f, center = Offset(startX + 6f, startY + 4f))
                    drawCircle(color = garnishInner, radius = 8f, center = Offset(startX + 6f, startY + 4f))

                    drawPath(
                        path = glassPath,
                        color = Color.White.copy(alpha = 0.85f),
                        style = Stroke(width = 3f)
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.55f),
                        start = Offset(startX + 6f, startY + 8f),
                        end = Offset(startX + 12f, startY + glassH - 8f),
                        strokeWidth = 2.5f,
                        cap = StrokeCap.Round
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    result.ingredientRatios.take(4).forEach { (type, pct) ->
                        val (emoji, name) = when (type) {
                            WorkshopItemType.CARROT -> "🥕" to "胡蘿蔔"
                            WorkshopItemType.BANANA -> "🍌" to "香蕉"
                            WorkshopItemType.CUCUMBER -> "🥒" to "黃瓜"
                            WorkshopItemType.STRAWBERRY_ROLL -> "🍓" to "草莓"
                            WorkshopItemType.RAINBOW_JELLY -> "🔮" to "果凍"
                            else -> "🍎" to "蔬果"
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 2.dp)
                        ) {
                            Text(
                                text = "$emoji $name $pct%",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = result.flavorDescriptionZh,
                    style = MaterialTheme.typography.bodySmall.copy(
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "${Localization.getString("final_score", language)}: $finalScore (+${result.bonusScore})",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onBackToMenu,
                    enabled = canInteract,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = Localization.getString("back_to_mode_menu", language), fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onRestart,
                enabled = canInteract,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = Localization.getString("play_again", language), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onLeaderboard,
                enabled = canInteract,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = Localization.getString("leaderboard_title", language), fontWeight = FontWeight.Bold)
            }
        }
    )
}

private fun triggerVibration(context: Context, durationMs: Long) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.vibrate(durationMs)
        }
    } catch (_: Exception) {}
}
