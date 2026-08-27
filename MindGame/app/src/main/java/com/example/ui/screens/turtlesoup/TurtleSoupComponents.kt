package com.example.ui.screens.turtlesoup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.data.model.InvestigationDimension
import com.example.data.model.InvestigationQueryLog
import com.example.data.model.AppLanguage
import com.example.data.model.DeductionSlot
import com.example.data.model.Localization
import com.example.data.model.PuzzleQuestion
import com.example.data.model.QuestionAnswer

// 懸疑復古主題專用色系
object MysteryThemeColors {
    val BackgroundDark = Color(0xFF14151F)
    val CardDark = Color(0xFF1E202E)
    val CardBorder = Color(255, 255, 255, 20) // rgba(255,255,255,0.08)
    val YesGreen = Color(0xFF10B981)          // 翡翠綠
    val NoRed = Color(0xFFEF4444)             // 朱砂紅
    val IrrelevantGray = Color(0xFF6B7280)    // 冷灰
    val CoreClueGold = Color(0xFFF59E0B)      // 金色微光
    val CoreClueGlowBg = Color(0x33F59E0B)
    val TextPrimary = Color(0xFFF3F4F6)
    val TextSecondary = Color(0xFF9CA3AF)
    val GoldAccent = Color(0xFFFFD700)
}

/**
 * 頂部狀態列：難度、關卡名稱、剩餘次數、核心線索燈號、計時器
 */
@Composable
fun TurtleSoupStatusBar(
    difficulty: String,
    title: String,
    remainingChances: Int,
    discoveredCoreClues: Int,
    requiredCoreClues: Int,
    elapsedSeconds: Long,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val diffColor = when (difficulty.uppercase()) {
        "EASY" -> MysteryThemeColors.YesGreen
        "MEDIUM" -> MysteryThemeColors.CoreClueGold
        else -> MysteryThemeColors.NoRed
    }

    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MysteryThemeColors.CardDark),
        border = BorderStroke(1.dp, MysteryThemeColors.CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // 第一列：難度標籤 + 關卡名稱 + 計時器
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = diffColor.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, diffColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = difficulty.uppercase(),
                            color = diffColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        color = MysteryThemeColors.TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                // 計時器
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Timer",
                        tint = MysteryThemeColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = timeFormatted,
                        color = MysteryThemeColors.TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 第二列：剩餘次數徽章 + 核心線索燈號
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 剩餘機會徽章
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (remainingChances <= 2) MysteryThemeColors.NoRed.copy(alpha = 0.25f) else Color(0xFF2A2D3D),
                    border = BorderStroke(
                        1.dp,
                        if (remainingChances <= 2) MysteryThemeColors.NoRed else MysteryThemeColors.CardBorder
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = Localization.getString("turtle_soup_remaining_chances", language, remainingChances),
                            color = if (remainingChances <= 2) MysteryThemeColors.NoRed else MysteryThemeColors.TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 核心線索進度
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = "Core Clues",
                        tint = if (discoveredCoreClues >= requiredCoreClues) MysteryThemeColors.CoreClueGold else MysteryThemeColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = Localization.getString("turtle_soup_core_clues", language, discoveredCoreClues, requiredCoreClues),
                        color = if (discoveredCoreClues >= requiredCoreClues) MysteryThemeColors.CoreClueGold else MysteryThemeColors.TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * 湯面題目卡片 (Surface Card)
 */
@Composable
fun TurtleSoupSurfaceCard(
    categoryName: String,
    surfaceText: String,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF232536)
        ),
        border = BorderStroke(1.dp, MysteryThemeColors.CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Localization.getString("turtle_soup_surface", language),
                    color = MysteryThemeColors.CoreClueGold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0x334B5563)
                ) {
                    Text(
                        text = categoryName,
                        color = MysteryThemeColors.TextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = surfaceText,
                color = MysteryThemeColors.TextPrimary,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

/**
 * 是非問題翻牌卡片 (Question Card)
 */
@Composable
fun TurtleSoupQuestionCard(
    index: Int,
    question: PuzzleQuestion,
    isUnlocked: Boolean,
    language: AppLanguage,
    onUnlockClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCore = question.isCore
    val borderStroke = if (isUnlocked && isCore) {
        BorderStroke(1.5.dp, MysteryThemeColors.CoreClueGold)
    } else {
        BorderStroke(1.dp, MysteryThemeColors.CardBorder)
    }

    val cardBg = if (isUnlocked && isCore) {
        Color(0xFF272533)
    } else if (isUnlocked) {
        Color(0xFF1E202E)
    } else {
        Color(0xFF1B1C28)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = !isUnlocked, onClick = onUnlockClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = borderStroke,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isUnlocked) 2.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // 頂部列：關鍵字 + 狀態標籤 (未翻開顯示鎖定/點擊提示)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${index + 1}. [${question.keyword.get(language)}]",
                        color = MysteryThemeColors.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (isUnlocked && isCore) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MysteryThemeColors.CoreClueGlowBg,
                            border = BorderStroke(0.5.dp, MysteryThemeColors.CoreClueGold)
                        ) {
                            Text(
                                text = "★ ${Localization.getString("turtle_soup_core_clue_badge", language)}",
                                color = MysteryThemeColors.CoreClueGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                if (!isUnlocked) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0x334B5563)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = MysteryThemeColors.TextSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "1 Chance",
                                color = MysteryThemeColors.TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    // 答案標籤 (YES / NO / IRRELEVANT)
                    val (badgeTextKey, badgeColor) = when (question.answerType) {
                        QuestionAnswer.YES -> Pair("turtle_soup_ans_yes", MysteryThemeColors.YesGreen)
                        QuestionAnswer.NO -> Pair("turtle_soup_ans_no", MysteryThemeColors.NoRed)
                        QuestionAnswer.IRRELEVANT -> Pair("turtle_soup_ans_irrelevant", MysteryThemeColors.IrrelevantGray)
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeColor.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = Localization.getString(badgeTextKey, language),
                            color = badgeColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 問題內容
            Text(
                text = question.question.get(language),
                color = if (isUnlocked) MysteryThemeColors.TextPrimary else MysteryThemeColors.TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            // 已解鎖時展開解析 Detail
            AnimatedVisibility(
                visible = isUnlocked,
                enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0x3310111A),
                        border = BorderStroke(0.5.dp, MysteryThemeColors.CardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = question.detail.get(language),
                            color = MysteryThemeColors.TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }

            if (!isUnlocked) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = Localization.getString("turtle_soup_tap_to_reveal", language),
                    color = MysteryThemeColors.TextSecondary.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

/**
 * 案件調查對話紀錄清單元件 (Investigation Chat Log)
 */
@Composable
fun InvestigationChatLogView(
    queryLogs: List<InvestigationQueryLog>,
    language: AppLanguage,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState()
) {
    LaunchedEffect(queryLogs.size) {
        if (queryLogs.isNotEmpty()) {
            listState.animateScrollToItem(queryLogs.size - 1)
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF181926)),
        border = BorderStroke(1.dp, MysteryThemeColors.CardBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = null,
                        tint = MysteryThemeColors.CoreClueGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = Localization.getString("turtle_soup_chat_log_title", language),
                        color = MysteryThemeColors.CoreClueGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "${queryLogs.size} records",
                    color = MysteryThemeColors.TextSecondary,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (queryLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .background(Color(0x22000000), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = Localization.getString("turtle_soup_chat_empty_hint", language),
                        color = MysteryThemeColors.TextSecondary.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(queryLogs) { log ->
                        InvestigationLogItem(log = log, language = language)
                    }
                }
            }
        }
    }
}

@Composable
private fun InvestigationLogItem(
    log: InvestigationQueryLog,
    language: AppLanguage
) {
    val (ansKey, ansColor) = when (log.answer) {
        QuestionAnswer.YES -> Pair("turtle_soup_ans_yes", MysteryThemeColors.YesGreen)
        QuestionAnswer.NO -> Pair("turtle_soup_ans_no", MysteryThemeColors.NoRed)
        QuestionAnswer.IRRELEVANT -> Pair("turtle_soup_ans_irrelevant", MysteryThemeColors.IrrelevantGray)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // 玩家提問氣泡 (靠右)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 4.dp, bottomStart = 12.dp, bottomEnd = 12.dp),
                color = Color(0xFF2C2E43),
                border = BorderStroke(0.5.dp, MysteryThemeColors.CardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💬 ${log.queryText}",
                        color = MysteryThemeColors.TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 系統/主持人回覆氣泡 (靠左)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 12.dp),
                color = if (log.isCore) Color(0xFF2E2718) else Color(0xFF1E202F),
                border = BorderStroke(
                    1.dp,
                    if (log.isCore) MysteryThemeColors.CoreClueGold.copy(alpha = 0.6f) else ansColor.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = ansColor.copy(alpha = 0.2f),
                            border = BorderStroke(0.5.dp, ansColor)
                        ) {
                            Text(
                                text = Localization.getString(ansKey, language),
                                color = ansColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }

                        if (log.isCore) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MysteryThemeColors.CoreClueGlowBg,
                                border = BorderStroke(0.5.dp, MysteryThemeColors.CoreClueGold)
                            ) {
                                Text(
                                    text = "★ ${Localization.getString("turtle_soup_core_clue_badge", language)}",
                                    color = MysteryThemeColors.CoreClueGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = log.detail,
                        color = if (log.isCore) MysteryThemeColors.TextPrimary else MysteryThemeColors.TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

/**
 * 多維度人事物下拉選單/選擇器元件 (Investigation Dimension Selector)
 */
@Composable
fun InvestigationDimensionSelector(
    dimensions: List<InvestigationDimension>,
    selectedDimensions: Map<String, String>,
    language: AppLanguage,
    onOptionSelected: (dimensionId: String, option: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MysteryThemeColors.CardDark),
        border = BorderStroke(1.dp, MysteryThemeColors.CardBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            dimensions.forEachIndexed { index, dimension ->
                val label = dimension.label.get(language)
                val options = dimension.options.get(language)
                val selectedOpt = selectedDimensions[dimension.id] ?: options.firstOrNull() ?: ""

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 維度標籤 (如 人物 / 物件 / 事件)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF2A2D3E),
                        modifier = Modifier.width(72.dp)
                    ) {
                        Text(
                            text = label,
                            color = MysteryThemeColors.CoreClueGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // 下拉選單按鈕
                    var expanded by remember { mutableStateOf(false) }

                    Box(modifier = Modifier.weight(1f)) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { expanded = true },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF141522),
                            border = BorderStroke(1.dp, if (expanded) MysteryThemeColors.CoreClueGold else MysteryThemeColors.CardBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedOpt.ifEmpty { "請選擇..." },
                                    color = MysteryThemeColors.TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Dropdown",
                                    tint = MysteryThemeColors.TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(MysteryThemeColors.CardDark)
                        ) {
                            options.forEach { opt ->
                                val isSelected = opt == selectedOpt
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = opt,
                                            color = if (isSelected) MysteryThemeColors.CoreClueGold else MysteryThemeColors.TextPrimary,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp
                                        )
                                    },
                                    onClick = {
                                        onOptionSelected(dimension.id, opt)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 調查次數耗盡彈窗：看廣告補充次數並立即送出詢問
 */
@Composable
fun OutOfChancesAskDialog(
    language: AppLanguage,
    onWatchAdAndSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MysteryThemeColors.CardDark,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = null,
                    tint = MysteryThemeColors.CoreClueGold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = Localization.getString("turtle_soup_watch_ad_to_ask_title", language),
                    color = MysteryThemeColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Text(
                text = Localization.getString("turtle_soup_watch_ad_to_ask_desc", language),
                color = MysteryThemeColors.TextSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onWatchAdAndSubmit,
                colors = ButtonDefaults.buttonColors(containerColor = MysteryThemeColors.CoreClueGold),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = Localization.getString("turtle_soup_watch_ad_and_submit_btn", language),
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = Localization.getString("cancel", language),
                    color = MysteryThemeColors.TextSecondary
                )
            }
        }
    )
}

/**
 * 複合填空 Slot 選擇膠囊元件
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeductionSlotItem(
    slotIndex: Int,
    slot: DeductionSlot,
    selectedIndex: Int?,
    language: AppLanguage,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = slot.options.get(language)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MysteryThemeColors.CardDark),
        border = BorderStroke(1.dp, MysteryThemeColors.CardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MysteryThemeColors.CoreClueGold.copy(alpha = 0.2f),
                    modifier = Modifier.size(22.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${slotIndex + 1}",
                            color = MysteryThemeColors.CoreClueGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "【${slot.label.get(language)}】",
                    color = MysteryThemeColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 各選項按鈕
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEachIndexed { optIndex, optText ->
                    val isSelected = selectedIndex == optIndex
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onOptionSelected(optIndex) },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) MysteryThemeColors.CoreClueGold else Color(0xFF27293A),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MysteryThemeColors.GoldAccent else MysteryThemeColors.CardBorder
                        )
                    ) {
                        Text(
                            text = optText,
                            color = if (isSelected) Color.Black else MysteryThemeColors.TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 探索次數耗盡彈窗：[觀看廣告 (+3次)] / [放棄本局查看真相]
 */
@Composable
fun OutOfChancesDialog(
    language: AppLanguage,
    onWatchAdClick: () -> Unit,
    onGiveUpClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MysteryThemeColors.CardDark,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = null,
                    tint = MysteryThemeColors.NoRed
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = Localization.getString("turtle_soup_out_of_chances_title", language),
                    color = MysteryThemeColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Text(
                text = Localization.getString("turtle_soup_out_of_chances_desc", language),
                color = MysteryThemeColors.TextSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onWatchAdClick,
                colors = ButtonDefaults.buttonColors(containerColor = MysteryThemeColors.CoreClueGold),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = Localization.getString("turtle_soup_watch_ad_btn", language),
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onGiveUpClick,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MysteryThemeColors.CardBorder)
            ) {
                Text(
                    text = Localization.getString("turtle_soup_give_up_btn", language),
                    color = MysteryThemeColors.TextSecondary
                )
            }
        }
    )
}

/**
 * 湯底真相彈窗 (用於放棄時展示真相)
 */
@Composable
fun BottomTruthDialog(
    title: String,
    bottomText: String,
    language: AppLanguage,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MysteryThemeColors.CardDark,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    tint = MysteryThemeColors.CoreClueGold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = Localization.getString("turtle_soup_bottom_truth_title", language),
                    color = MysteryThemeColors.CoreClueGold,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column {
                Text(
                    text = title,
                    color = MysteryThemeColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = bottomText,
                    color = MysteryThemeColors.TextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = Localization.getString("confirm", language),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

/**
 * 結案結算印章與星級展示元件
 */
@Composable
fun StampAndStarsView(
    stars: Int,
    score: Int,
    usedChances: Int,
    usedAdReward: Boolean,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val stampScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "stampScale"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 星級展示 (0~3 星)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 1..3) {
                val isLit = i <= stars
                Icon(
                    imageVector = if (isLit) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Star $i",
                    tint = if (isLit) MysteryThemeColors.GoldAccent else MysteryThemeColors.TextSecondary.copy(alpha = 0.4f),
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 印章反饋 (結案大吉 / CASE SOLVED)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MysteryThemeColors.YesGreen.copy(alpha = 0.15f),
            border = BorderStroke(2.dp, MysteryThemeColors.YesGreen),
            modifier = Modifier
                .scale(stampScale)
                .rotate(-6f)
        ) {
            Text(
                text = "✔ ${Localization.getString("turtle_soup_completed_title", language)}",
                color = MysteryThemeColors.YesGreen,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 得分與消耗次數
        Text(
            text = Localization.getString("turtle_soup_score_format", language, score),
            color = MysteryThemeColors.CoreClueGold,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (usedAdReward) {
                "(${Localization.getString("turtle_soup_watch_ad_btn", language)} - 50% Base Score)"
            } else {
                Localization.getString("turtle_soup_used_chances_format", language, usedChances)
            },
            color = MysteryThemeColors.TextSecondary,
            fontSize = 13.sp
        )
    }
}
