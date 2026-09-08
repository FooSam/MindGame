package com.example.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.db.ScoreRecord
import com.example.data.model.AppLanguage
import com.example.data.model.GameDifficulty
import com.example.data.model.GameType
import com.example.data.model.GlobalScoreEntry
import com.example.data.model.Localization
import java.util.Date

fun formatTimeMillis(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val millis = timeMs % 1000
    return String.format("%02d:%02d.%03d", minutes, seconds, millis)
}

enum class LeaderboardTab {
    LOCAL,
    GLOBAL
}

@Composable
fun LeaderboardDialog(
    selectedDifficulty: GameDifficulty,
    selectedGameType: GameType = GameType.FOCUS_TEST,
    scores: List<ScoreRecord>,
    globalScores: List<GlobalScoreEntry> = emptyList(),
    myGlobalRankEntry: GlobalScoreEntry? = null,
    isFetchingGlobal: Boolean = false,
    isUploadingGlobal: Boolean = false,
    language: AppLanguage,
    onDifficultySelected: (GameDifficulty) -> Unit,
    onClearScores: () -> Unit,
    onUploadToGlobal: () -> Unit = {},
    onRefreshGlobal: () -> Unit = {},
    onDismiss: () -> Unit
) {
    var activeTab by remember { mutableStateOf(LeaderboardTab.LOCAL) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Title & Scope Tabs (Local vs Global)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (activeTab == LeaderboardTab.LOCAL) {
                            Localization.getString("leaderboard_title", language)
                        } else {
                            Localization.getString("leaderboard_global_title", language)
                        },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )

                    // Local vs Global Toggle Segmented Control
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (activeTab == LeaderboardTab.LOCAL) MaterialTheme.colorScheme.primary else Color.Transparent,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { activeTab = LeaderboardTab.LOCAL }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Storage,
                                        contentDescription = null,
                                        tint = if (activeTab == LeaderboardTab.LOCAL) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = Localization.getString("leaderboard_tab_local", language),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (activeTab == LeaderboardTab.LOCAL) FontWeight.Bold else FontWeight.Normal,
                                            color = if (activeTab == LeaderboardTab.LOCAL) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (activeTab == LeaderboardTab.GLOBAL) MaterialTheme.colorScheme.primary else Color.Transparent,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        activeTab = LeaderboardTab.GLOBAL
                                        onRefreshGlobal()
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Public,
                                        contentDescription = null,
                                        tint = if (activeTab == LeaderboardTab.GLOBAL) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = Localization.getString("leaderboard_tab_global", language),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (activeTab == LeaderboardTab.GLOBAL) FontWeight.Bold else FontWeight.Normal,
                                            color = if (activeTab == LeaderboardTab.GLOBAL) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Difficulty Selector Tabs
                val difficulties = GameDifficulty.entries
                val selectedIndex = difficulties.indexOf(selectedDifficulty)

                ScrollableTabRow(
                    selectedTabIndex = if (selectedIndex >= 0) selectedIndex else 0,
                    edgePadding = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    difficulties.forEach { diff ->
                        val labelKey = when (diff) {
                            GameDifficulty.BEGINNER -> "diff_name_beginner"
                            GameDifficulty.INTERMEDIATE -> "diff_name_intermediate"
                            GameDifficulty.ADVANCED -> "diff_name_advanced"
                            GameDifficulty.HARD -> "diff_name_hard"
                            GameDifficulty.HELL -> "diff_name_hell"
                            GameDifficulty.EPIC -> "diff_name_epic"
                        }
                        Tab(
                            selected = diff == selectedDifficulty,
                            onClick = { onDifficultySelected(diff) },
                            text = {
                                Text(
                                    text = Localization.getString(labelKey, language),
                                    fontWeight = if (diff == selectedDifficulty) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Table Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Localization.getString("rank", language),
                        modifier = Modifier.width(40.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                    Text(
                        text = Localization.getString("player", language),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                    val scoreHeaderLabel = when (selectedGameType) {
                        GameType.SPEED_MATCH, GameType.TURTLE_SOUP, GameType.AVATAR_WHACK, GameType.STROOP_EFFECT -> Localization.getString("score_label", language)
                        GameType.SUDOKU, GameType.CAT_SUDOKU -> "${Localization.getString("time", language)} (${Localization.getString("mistakes_label", language)})"
                        else -> Localization.getString("time", language)
                    }

                    Text(
                        text = scoreHeaderLabel,
                        modifier = Modifier.width(120.dp),
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Main List Content (Local or Global)
                if (activeTab == LeaderboardTab.LOCAL) {
                    // Local List
                    if (scores.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = Localization.getString("no_records", language),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        ) {
                            itemsIndexed(scores) { index, record ->
                                val rank = index + 1
                                val rankColor = when (rank) {
                                    1 -> Color(0xFFFFD700) // Gold
                                    2 -> Color(0xFFC0C0C0) // Silver
                                    3 -> Color(0xFFCD7F32) // Bronze
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }

                                val scoreValueText = when (selectedGameType) {
                                    GameType.SPEED_MATCH -> "${record.score} 次"
                                    GameType.TURTLE_SOUP, GameType.AVATAR_WHACK, GameType.STROOP_EFFECT -> "${record.score} 分"
                                    GameType.SUDOKU, GameType.CAT_SUDOKU -> "${formatTimeMillis(record.timeMillis)} (${record.wrongCount}錯)"
                                    else -> formatTimeMillis(record.timeMillis)
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "#$rank",
                                        modifier = Modifier.width(40.dp),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = rankColor
                                        )
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = record.playerName,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val dateStr = DateFormat.format("MM/dd HH:mm", Date(record.timestamp)).toString()
                                        Text(
                                            text = dateStr,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 10.sp
                                            )
                                        )
                                    }
                                    Text(
                                        text = scoreValueText,
                                        modifier = Modifier.width(120.dp),
                                        textAlign = TextAlign.End,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                                if (index < scores.size - 1) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        thickness = 0.5.dp
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Global List
                    if (isFetchingGlobal) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = Localization.getString("loading", language),
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }
                    } else if (globalScores.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = Localization.getString("no_records", language),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(onClick = onRefreshGlobal) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = Localization.getString("retry", language))
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        ) {
                            itemsIndexed(globalScores) { index, record ->
                                val rank = record.rank
                                val rankColor = when (rank) {
                                    1 -> Color(0xFFFFD700) // Gold
                                    2 -> Color(0xFFC0C0C0) // Silver
                                    3 -> Color(0xFFCD7F32) // Bronze
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }

                                val scoreValueText = when (selectedGameType) {
                                    GameType.SPEED_MATCH -> "${record.score} 次"
                                    GameType.TURTLE_SOUP, GameType.AVATAR_WHACK, GameType.STROOP_EFFECT -> "${record.score} 分"
                                    GameType.SUDOKU, GameType.CAT_SUDOKU -> "${formatTimeMillis(record.timeMillis)} (${record.wrongCount}錯)"
                                    else -> formatTimeMillis(record.timeMillis)
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "#$rank",
                                        modifier = Modifier.width(40.dp),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = rankColor
                                        )
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "${record.country.flagEmoji} ",
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = record.playerName,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.SemiBold
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        val dateStr = DateFormat.format("MM/dd HH:mm", Date(record.timestamp)).toString()
                                        Text(
                                            text = dateStr,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 10.sp
                                            )
                                        )
                                    }
                                    Text(
                                        text = scoreValueText,
                                        modifier = Modifier.width(120.dp),
                                        textAlign = TextAlign.End,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                                if (index < globalScores.size - 1) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        thickness = 0.5.dp
                                    )
                                }
                            }
                        }
                    }

                    // Bottom Dedicated "My Global Best Rank" Card
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = Localization.getString("my_global_rank_label", language),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                )
                            }

                            if (myGlobalRankEntry != null) {
                                val myScoreText = when (selectedGameType) {
                                    GameType.SPEED_MATCH -> "${myGlobalRankEntry.score} 次"
                                    GameType.TURTLE_SOUP, GameType.AVATAR_WHACK, GameType.STROOP_EFFECT -> "${myGlobalRankEntry.score} 分"
                                    GameType.SUDOKU, GameType.CAT_SUDOKU -> "${formatTimeMillis(myGlobalRankEntry.timeMillis)} (${myGlobalRankEntry.wrongCount}錯)"
                                    else -> formatTimeMillis(myGlobalRankEntry.timeMillis)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "#${myGlobalRankEntry.rank}",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "(${myGlobalRankEntry.country.flagEmoji} $myScoreText)",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    )
                                }
                            } else {
                                Text(
                                    text = Localization.getString("not_ranked_yet", language),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Upload to Global Button
                    Button(
                        onClick = onUploadToGlobal,
                        enabled = !isUploadingGlobal && scores.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isUploadingGlobal) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = Localization.getString("uploading", language),
                                fontSize = 12.sp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = Localization.getString("upload_to_global", language),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (activeTab == LeaderboardTab.LOCAL && scores.isNotEmpty()) {
                            TextButton(onClick = onClearScores) {
                                Text(
                                    text = Localization.getString("clear_records", language),
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        TextButton(onClick = onDismiss) {
                            Text(text = Localization.getString("confirm", language))
                        }
                    }
                }
            }
        }
    }
}
