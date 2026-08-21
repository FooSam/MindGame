package com.example.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.db.ScoreRecord
import com.example.data.model.AppLanguage
import com.example.data.model.GameDifficulty
import com.example.data.model.GameType
import com.example.data.model.Localization
import java.util.Date

fun formatTimeMillis(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val millis = timeMs % 1000
    return String.format("%02d:%02d.%03d", minutes, seconds, millis)
}

@Composable
fun LeaderboardDialog(
    selectedDifficulty: GameDifficulty,
    selectedGameType: GameType = GameType.FOCUS_TEST,
    scores: List<ScoreRecord>,
    language: AppLanguage,
    onDifficultySelected: (GameDifficulty) -> Unit,
    onClearScores: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = Localization.getString("leaderboard_title", language),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

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
                                    fontWeight = if (diff == selectedDifficulty) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Localization.getString("rank", language),
                        modifier = Modifier.width(44.dp),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                    Text(
                        text = Localization.getString("player", language),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                    val scoreHeaderLabel = when (selectedGameType) {
                        GameType.SPEED_MATCH -> Localization.getString("score_label", language)
                        GameType.SUDOKU, GameType.CAT_SUDOKU -> "${Localization.getString("time", language)} (${Localization.getString("mistakes_label", language)})"
                        else -> Localization.getString("time", language)
                    }

                    Text(
                        text = scoreHeaderLabel,
                        modifier = Modifier.width(130.dp),
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Scores List
                if (scores.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
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
                            .height(240.dp)
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
                                GameType.SUDOKU, GameType.CAT_SUDOKU -> "${formatTimeMillis(record.timeMillis)} (${record.wrongCount}錯)"
                                else -> formatTimeMillis(record.timeMillis)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "#$rank",
                                    modifier = Modifier.width(44.dp),
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
                                        )
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
                                    modifier = Modifier.width(130.dp),
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

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (scores.isNotEmpty()) {
                        TextButton(onClick = onClearScores) {
                            Text(
                                text = Localization.getString("clear_records", language),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    TextButton(onClick = onDismiss) {
                        Text(text = Localization.getString("confirm", language))
                    }
                }
            }
        }
    }
}
