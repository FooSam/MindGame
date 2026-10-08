package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AppLanguage
import com.example.data.model.GameDifficulty
import com.example.data.model.Localization
import com.example.game.whack.AvatarCatalog
import com.example.game.whack.AvatarType
import com.example.game.whack.WhackDifficultyConfig
import com.example.game.whack.WhackHoleState
import com.example.ui.viewmodel.GameStatus

/**
 * 《打那個誰》(Avatar Whack) 次世代 2024-2026 頂級手遊視覺重構
 * 導入青翠陽光蘑菇林背景、擬真 3D 泥土地穴、POW! 打擊星芒爆破、
 * 精雕原木告示牌風格 HUD 儀表板與手遊星級結算面板。
 */
@Composable
fun AvatarWhackScreen(
    difficulty: GameDifficulty,
    gameStatus: GameStatus,
    config: WhackDifficultyConfig,
    holes: List<WhackHoleState>,
    score: Int,
    combo: Int,
    maxCombo: Int,
    hits: Int,
    misses: Int,
    bombHits: Int,
    remainingTimeMs: Long,
    lastCompletedScore: Int?,
    language: AppLanguage,
    onBackClick: () -> Unit,
    onStartClick: () -> Unit,
    onResetClick: () -> Unit,
    onHoleClick: (Int) -> Unit,
    onLeaderboardClick: () -> Unit
) {
    val totalSeconds = (remainingTimeMs / 1000).coerceAtLeast(0)
    val timeProgress = (remainingTimeMs.toFloat() / config.gameDurationMs.toFloat()).coerceIn(0f, 1f)

    // 森林陽光微風脈衝
    val infiniteTransition = rememberInfiniteTransition(label = "forestSunlight")
    val sunRayAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sunRay"
    )

    // 青翠森林漸層底色
    val forestBgBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF132A1C), // 頂部林蔭深綠
                Color(0xFF0F2318), // 中段青翠草地
                Color(0xFF0A1810)  // 底部黑土苔蘚
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(forestBgBrush)
            .drawBehind {
                drawForestMeadowDappledSunlight(sunRayAlpha)
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // 1. 頂部原木導航列 (Top Bar)
            AvatarWhackTopBar(
                difficulty = difficulty,
                language = language,
                onBackClick = onBackClick,
                onLeaderboardClick = onLeaderboardClick,
                onResetClick = onResetClick
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2. 精雕橡木告示牌風格 HUD 儀表板 (Rustic Oak Signpost HUD)
            AvatarWhackOakHud(
                score = score,
                combo = combo,
                totalSeconds = totalSeconds,
                remainingTimeMs = remainingTimeMs,
                timeProgress = timeProgress,
                language = language
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. 擬真 3D 泥土地穴矩陣 (Whack Hole Grid)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(config.cols),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(holes) { holeState ->
                        WhackHoleItem(
                            holeState = holeState,
                            gameStatus = gameStatus,
                            onClick = { onHoleClick(holeState.holeIndex) }
                        )
                    }
                }

                // 4. 開局森林木屋告示牌 (Idle Briefing Overlay)
                if (gameStatus == GameStatus.IDLE) {
                    AvatarWhackIdleBriefingCard(
                        difficulty = difficulty,
                        language = language,
                        onStartClick = onStartClick
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // 5. 結算成就成就面板 (Material 3 Dialog)
        if (gameStatus == GameStatus.COMPLETED) {
            AvatarWhackVictoryDialog(
                score = score,
                hits = hits,
                maxCombo = maxCombo,
                bombHits = bombHits,
                language = language,
                onBackClick = onBackClick,
                onStartClick = onStartClick
            )
        }
    }
}

// -------------------------------------------------------------------------------------
// 1. 頂部原木導航列 (Top Bar)
// -------------------------------------------------------------------------------------
@Composable
private fun AvatarWhackTopBar(
    difficulty: GameDifficulty,
    language: AppLanguage,
    onBackClick: () -> Unit,
    onLeaderboardClick: () -> Unit,
    onResetClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左側：木質圓形返回鍵 + 遊戲標題與難度木牌
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF2E1C14),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .size(42.dp)
                    .border(1.5.dp, Color(0xFF5D4037), CircleShape)
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFFFFE0B2)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = Localization.getString("game_avatar_whack", language),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFFF7ED),
                        letterSpacing = 0.5.sp
                    )
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF3E2723),
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .border(1.dp, Color(0xFF8D6E63), RoundedCornerShape(6.dp))
                ) {
                    Text(
                        text = Localization.getString("diff_name_${difficulty.key.lowercase()}", language),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFFFFD54F),
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // 右側：木質排行榜按鈕與重整按鈕
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF2E1C14),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .size(42.dp)
                    .border(1.5.dp, Color(0xFF5D4037), CircleShape)
            ) {
                IconButton(onClick = onLeaderboardClick) {
                    Icon(
                        imageVector = Icons.Default.Leaderboard,
                        contentDescription = "Leaderboard",
                        tint = Color(0xFFFFD54F)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = CircleShape,
                color = Color(0xFF2E1C14),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .size(42.dp)
                    .border(1.5.dp, Color(0xFF5D4037), CircleShape)
            ) {
                IconButton(onClick = onResetClick) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset",
                        tint = Color(0xFFD7CCC8)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// 2. 精雕橡木告示牌風格 HUD 儀表板 (Rustic Oak Signpost HUD)
// -------------------------------------------------------------------------------------
@Composable
private fun AvatarWhackOakHud(
    score: Int,
    combo: Int,
    totalSeconds: Long,
    remainingTimeMs: Long,
    timeProgress: Float,
    language: AppLanguage
) {
    val isUrgentTime = remainingTimeMs < 10_000L

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF2A1B12)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 2.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF8D6E63), Color(0xFF3E2723))
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .drawBehind {
                    drawRusticWoodGrain()
                }
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 2.1 積分 (Score Wood Plate)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1D110B),
                    modifier = Modifier.border(1.dp, Color(0xFF8D6E63).copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text(
                            text = Localization.getString("avatar_whack_score", language),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFD7CCC8),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = "$score",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFFFD54F),
                                fontSize = 22.sp
                            )
                        )
                    }
                }

                // 2.2 超頻連擊木牌 (Overdrive Combo Badge)
                if (combo >= 2) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFE65100),
                        shadowElevation = 6.dp,
                        modifier = Modifier.border(1.5.dp, Color(0xFFFFB74D), RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = "Combo",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "COMBO x$combo",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }
                    }
                }

                // 2.3 剩餘總時間 (Timer Wooden Capsule)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isUrgentTime) Color(0xFFB71C1C) else Color(0xFF1D110B),
                    modifier = Modifier.border(
                        1.dp,
                        if (isUrgentTime) Color(0xFFEF4444) else Color(0xFF8D6E63).copy(alpha = 0.6f),
                        RoundedCornerShape(12.dp)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = if (isUrgentTime) Color(0xFFFFCDD2) else Color(0xFFFFD54F),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${totalSeconds}s",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = if (isUrgentTime) Color(0xFFFFEBEE) else Color(0xFFFFF7ED)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2.4 藤蔓與橡果能量時間條 (Forest Vine Progress Tube)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF1D110B))
                    .border(1.dp, Color(0xFF4E342E), RoundedCornerShape(4.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = timeProgress)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = if (isUrgentTime) {
                                    listOf(Color(0xFFD32F2F), Color(0xFFFF5722))
                                } else {
                                    listOf(Color(0xFF2E7D32), Color(0xFF81C784), Color(0xFFFFD54F))
                                }
                            )
                        )
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// 3. 擬真 3D 泥土地穴單元 (`WhackHoleItem`)
// -------------------------------------------------------------------------------------
@Composable
fun WhackHoleItem(
    holeState: WhackHoleState,
    gameStatus: GameStatus,
    onClick: () -> Unit
) {
    val avatar = holeState.avatar
    val isPopping = holeState.isPopping
    val isHit = holeState.isHit

    // 彈簧物理探頭動態 (Spring Pop-Up)
    val animatedScale by animateFloatAsState(
        targetValue = if (isPopping) 1f else 0.0f,
        animationSpec = if (isHit) spring(dampingRatio = 0.45f, stiffness = 500f)
        else spring(dampingRatio = 0.65f, stiffness = 420f),
        label = "avatarScale"
    )

    val animatedOffsetY by animateFloatAsState(
        targetValue = if (isPopping) 0f else 64f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "avatarOffset"
    )

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(24.dp))
            .clickable(
                enabled = gameStatus == GameStatus.PLAYING,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .drawBehind {
                drawMossyEarthenBurrow()
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        // 下沉式深邃地心洞穴陰影 (Deep Underground Pit)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .height(44.dp)
                .offset(y = (-8).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF070503), Color(0xFF1F140D))
                    )
                )
        )

        // 角色探頭 (Avatar Image)
        if (avatar != null) {
            val drawableRes = AvatarCatalog.getDrawableRes(
                avatarIndex = avatar.avatarIndex,
                expression = avatar.expression,
                type = avatar.type
            )

            Image(
                painter = painterResource(id = drawableRes),
                contentDescription = "Avatar ${avatar.avatarIndex}",
                modifier = Modifier
                    .fillMaxSize(0.86f)
                    .graphicsLayer {
                        scaleX = animatedScale
                        scaleY = animatedScale
                        translationY = animatedOffsetY
                    }
                    .padding(bottom = 12.dp)
            )

            // 打擊爆破特效 (Hit Impact FX)
            if (isHit) {
                HitImpactBurstOverlay(avatarType = avatar.type)
            }
        }

        // 前置泥土阻隔擋土草丘 (Front Dirt Mound Lip)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .height(20.dp)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 22.dp, bottomEnd = 22.dp))
                .drawBehind {
                    drawFrontDirtLip()
                }
        )
    }
}

// -------------------------------------------------------------------------------------
// 打擊爆破特效 (Hit Impact Burst Overlay)
// -------------------------------------------------------------------------------------
@Composable
private fun HitImpactBurstOverlay(avatarType: AvatarType) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                when (avatarType) {
                    AvatarType.BOMB -> {
                        // 炸彈被擊中：黑煙與赤紅警戒光環
                        drawCircle(
                            color = Color(0x66EF4444),
                            radius = size.minDimension * 0.45f
                        )
                        drawCircle(
                            color = Color(0xFFEF4444),
                            radius = size.minDimension * 0.45f,
                            style = Stroke(width = 4.dp.toPx())
                        )
                    }
                    AvatarType.BONUS -> {
                        // 加分怪獸被擊中：璀璨黃金光環與星芒
                        drawCircle(
                            color = Color(0x66FFD54F),
                            radius = size.minDimension * 0.48f
                        )
                        drawCircle(
                            color = Color(0xFFFFD54F),
                            radius = size.minDimension * 0.48f,
                            style = Stroke(width = 4.dp.toPx())
                        )
                    }
                    AvatarType.NORMAL -> {
                        // 普通怪獸被擊中：POW! 金黃打擊星芒
                        drawCircle(
                            color = Color.White.copy(alpha = 0.4f),
                            radius = size.minDimension * 0.45f
                        )
                        drawCircle(
                            color = Color(0xFFFFD54F),
                            radius = size.minDimension * 0.45f,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                }
            }
    )
}

// -------------------------------------------------------------------------------------
// 4. 開局森林木屋告示牌 (Idle Briefing Overlay)
// -------------------------------------------------------------------------------------
@Composable
private fun AvatarWhackIdleBriefingCard(
    difficulty: GameDifficulty,
    language: AppLanguage,
    onStartClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .shadow(16.dp, RoundedCornerShape(26.dp)),
        shape = RoundedCornerShape(26.dp),
        color = Color(0xFF261710)
    ) {
        Column(
            modifier = Modifier
                .border(
                    width = 2.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF8D6E63), Color(0xFF3E2723))
                    ),
                    shape = RoundedCornerShape(26.dp)
                )
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 木槌圖騰
            Surface(
                shape = CircleShape,
                color = Color(0xFF4E342E),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .size(74.dp)
                    .border(2.dp, Color(0xFFFFD54F), CircleShape)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Gavel,
                        contentDescription = null,
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(42.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = Localization.getString("game_avatar_whack", language),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFFF7ED)
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = Localization.getString("avatar_whack_diff_${difficulty.key.lowercase()}_desc", language),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFFD7CCC8),
                    lineHeight = 20.sp
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(22.dp))

            Button(
                onClick = onStartClick,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = Localization.getString("start_game", language),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontSize = 17.sp
                    )
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// 5. 結算成就成就面板 (Material 3 Dialog)
// -------------------------------------------------------------------------------------
@Composable
private fun AvatarWhackVictoryDialog(
    score: Int,
    hits: Int,
    maxCombo: Int,
    bombHits: Int,
    language: AppLanguage,
    onBackClick: () -> Unit,
    onStartClick: () -> Unit
) {
    // 星級評價計算
    val stars = when {
        score >= 2000 && bombHits == 0 -> 3
        score >= 1200 && bombHits <= 1 -> 2
        else -> 1
    }

    Dialog(onDismissRequest = { /* 必須點擊按鈕操作 */ }) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFF261710),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 2.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFFFD54F).copy(alpha = 0.8f), Color(0xFF3E2723))
                    ),
                    shape = RoundedCornerShape(26.dp)
                )
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 獎章圖示
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF4E342E),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .size(68.dp)
                        .border(2.dp, Color(0xFFFFD54F), CircleShape)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 星星成就列
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

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = Localization.getString("avatar_whack_time_up", language),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "$score 分",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFFFD54F)
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 數據卡片
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1D110B).copy(alpha = 0.8f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF5D4037), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = Localization.getString("avatar_whack_hits", language),
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFD7CCC8))
                            )
                            Text(
                                text = "$hits 次",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF81C784)
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = Localization.getString("avatar_whack_combo", language),
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFD7CCC8))
                            )
                            Text(
                                text = "最高 $maxCombo",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFB74D)
                                )
                            )
                        }
                        if (bombHits > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = Localization.getString("avatar_whack_bombs", language),
                                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFE57373))
                                )
                                Text(
                                    text = "$bombHits 次",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFEF5350)
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = Localization.getString("score_saved", language),
                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF8D6E63))
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onBackClick,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = Localization.getString("back_to_menu", language),
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFD7CCC8))
                        )
                    }

                    Button(
                        onClick = onStartClick,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = Localization.getString("play_again", language),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// 繪圖工具函式：森林草地斑駁陽光、泥土洞穴、木紋與前置擋土丘
// -------------------------------------------------------------------------------------

/**
 * 繪製森林草地斑駁陽光微粒 (Dappled Sunlight)
 */
private fun DrawScope.drawForestMeadowDappledSunlight(sunRayAlpha: Float) {
    val sunColor = Color(0xFFFFF9C4).copy(alpha = sunRayAlpha * 0.12f)
    drawCircle(
        color = sunColor,
        radius = size.width * 0.45f,
        center = Offset(size.width * 0.2f, size.height * 0.15f)
    )
    drawCircle(
        color = sunColor,
        radius = size.width * 0.35f,
        center = Offset(size.width * 0.8f, size.height * 0.4f)
    )
}

/**
 * 繪製木牌的木紋質感 (Rustic Wood Grain)
 */
private fun DrawScope.drawRusticWoodGrain() {
    val grainColor = Color(0xFF1D110B).copy(alpha = 0.2f)
    var y = 8f
    while (y < size.height) {
        drawLine(
            color = grainColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1.2f
        )
        y += 20f
    }
}

/**
 * 繪製青苔泥土洞穴護坡座 (Mossy Earthen Burrow)
 */
private fun DrawScope.drawMossyEarthenBurrow() {
    val corner = CornerRadius(24.dp.toPx(), 24.dp.toPx())

    // 1. 底層厚泥土底座漸層
    val earthenGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF4E342E), // 頂層泥土
            Color(0xFF3E2723), // 中層深泥
            Color(0xFF2E1C14)  // 底座厚泥
        )
    )
    drawRoundRect(
        brush = earthenGradient,
        size = size,
        cornerRadius = corner
    )

    // 2. 洞穴外圈頂部青苔綠邊 (Moss Rim)
    val mossPath = Path().apply {
        moveTo(12.dp.toPx(), 4.dp.toPx())
        lineTo(size.width - 12.dp.toPx(), 4.dp.toPx())
    }
    drawPath(
        path = mossPath,
        color = Color(0xFF4CAF50).copy(alpha = 0.5f),
        style = Stroke(width = 3.dp.toPx())
    )

    // 3. 微倒角外框 (Outer Mound Edge)
    drawRoundRect(
        color = Color(0xFF8D6E63).copy(alpha = 0.4f),
        size = size,
        cornerRadius = corner,
        style = Stroke(width = 1.5.dp.toPx())
    )
}

/**
 * 繪製洞穴前方有機擋土丘 (Front Dirt Lip)
 */
private fun DrawScope.drawFrontDirtLip() {
    val moundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF5D4037),
            Color(0xFF3E2723)
        )
    )
    drawRect(brush = moundGradient, size = size)

    // 上緣青苔微草線
    drawLine(
        color = Color(0xFF81C784).copy(alpha = 0.6f),
        start = Offset(0f, 0f),
        end = Offset(size.width, 0f),
        strokeWidth = 2.dp.toPx()
    )
}
