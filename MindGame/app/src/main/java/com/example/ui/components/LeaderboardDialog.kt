package com.example.ui.components

import android.text.format.DateFormat
import com.example.audio.SoundManager
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.data.model.HotGameEntry
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

enum class LeaderboardMainTab {
    RECORDS,
    HOT_GAMES
}

@Composable
fun LeaderboardDialog(
    selectedDifficulty: GameDifficulty,
    selectedGameType: GameType = GameType.FOCUS_TEST,
    scores: List<ScoreRecord>,
    globalScores: List<GlobalScoreEntry> = emptyList(),
    myGlobalRankEntry: GlobalScoreEntry? = null,
    hotGames: List<HotGameEntry> = emptyList(),
    totalVotersCount: Int = 0,
    currentMonth: String = "",
    isFetchingGlobal: Boolean = false,
    isUploadingGlobal: Boolean = false,
    isFetchingHotGames: Boolean = false,
    initialMainTab: LeaderboardMainTab = LeaderboardMainTab.RECORDS,
    language: AppLanguage,
    onDifficultySelected: (GameDifficulty) -> Unit,
    onGameSelected: (GameType) -> Unit = {},
    onClearScores: () -> Unit,
    onUploadToGlobal: () -> Unit = {},
    onRefreshGlobal: () -> Unit = {},
    onRefreshHotGames: () -> Unit = {},
    onPlayGame: (GameType) -> Unit = {},
    onDismiss: () -> Unit
) {
    var mainTab by remember(initialMainTab) { mutableStateOf(initialMainTab) }
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
                // Top Main Tabs: [🎮 單項紀錄] vs [❤️ 熱門風雲榜]
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (mainTab == LeaderboardMainTab.RECORDS) MaterialTheme.colorScheme.primary else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { mainTab = LeaderboardMainTab.RECORDS }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = if (mainTab == LeaderboardMainTab.RECORDS) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = Localization.getString("leaderboard_main_tab_records", language),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (mainTab == LeaderboardMainTab.RECORDS) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (mainTab == LeaderboardMainTab.HOT_GAMES) MaterialTheme.colorScheme.primary else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { mainTab = LeaderboardMainTab.HOT_GAMES }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = if (mainTab == LeaderboardMainTab.HOT_GAMES) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = Localization.getString("leaderboard_main_tab_hot", language),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (mainTab == LeaderboardMainTab.HOT_GAMES) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (mainTab == LeaderboardMainTab.HOT_GAMES) {
                    HotGamesLeaderboardContent(
                        hotGames = hotGames,
                        totalVotersCount = totalVotersCount,
                        currentMonth = currentMonth,
                        isFetching = isFetchingHotGames,
                        language = language,
                        onRefresh = onRefreshHotGames,
                        onPlayGame = onPlayGame,
                        onDismiss = onDismiss
                    )
                } else {
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

                // Game Selector Row & Dropdown
                var showGameDropdown by remember { mutableStateOf(false) }

                Surface(
                    onClick = {
                        SoundManager.playClick()
                        showGameDropdown = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SportsEsports,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = Localization.getString("current_game_label", language) + "：" + Localization.getString(selectedGameType.titleKey, language),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = Localization.getString("switch_game", language),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Switch Game",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showGameDropdown,
                        onDismissRequest = { showGameDropdown = false },
                        modifier = Modifier.heightIn(max = 380.dp)
                    ) {
                        GameType.entries.forEach { game ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = Localization.getString(game.titleKey, language),
                                        fontWeight = if (game == selectedGameType) FontWeight.Bold else FontWeight.Normal,
                                        color = if (game == selectedGameType) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    showGameDropdown = false
                                    SoundManager.playClick()
                                    onGameSelected(game)
                                },
                                leadingIcon = {
                                    if (game == selectedGameType) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.size(18.dp))
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Difficulty Selector Tabs
                val isFruitMaster = selectedGameType == GameType.FRUIT_MASTER
                val difficulties = if (isFruitMaster) {
                    listOf(GameDifficulty.BEGINNER, GameDifficulty.INTERMEDIATE, GameDifficulty.ADVANCED)
                } else {
                    GameDifficulty.entries
                }
                val selectedIndex = difficulties.indexOf(selectedDifficulty)

                ScrollableTabRow(
                    selectedTabIndex = if (selectedIndex >= 0) selectedIndex else 0,
                    edgePadding = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    difficulties.forEach { diff ->
                        val labelKey = if (isFruitMaster) {
                            when (diff) {
                                GameDifficulty.BEGINNER -> "mode_heartbeat_slicer"
                                GameDifficulty.INTERMEDIATE -> "mode_blade_and_bomb"
                                else -> "mode_workshop"
                            }
                        } else {
                            when (diff) {
                                GameDifficulty.BEGINNER -> "diff_name_beginner"
                                GameDifficulty.INTERMEDIATE -> "diff_name_intermediate"
                                GameDifficulty.ADVANCED -> "diff_name_advanced"
                                GameDifficulty.HARD -> "diff_name_hard"
                                GameDifficulty.HELL -> "diff_name_hell"
                                GameDifficulty.EPIC -> "diff_name_epic"
                            }
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
                        GameType.SPEED_MATCH -> Localization.getString("speed_match_score_label", language)
                        GameType.TURTLE_SOUP, GameType.AVATAR_WHACK, GameType.STROOP_EFFECT, GameType.BLOCK_PUZZLE, GameType.FRUIT_MASTER -> Localization.getString("score_label", language)
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
                        val sortedScores = remember(scores, selectedGameType) {
                            when (selectedGameType) {
                                GameType.SPEED_MATCH, GameType.TURTLE_SOUP, GameType.AVATAR_WHACK,
                                GameType.STROOP_EFFECT, GameType.BLOCK_PUZZLE, GameType.FRUIT_MASTER -> {
                                    scores.sortedWith(
                                        compareByDescending<ScoreRecord> { it.score }
                                            .thenBy { it.wrongCount }
                                            .thenByDescending { it.id }
                                    )
                                }
                                GameType.SUDOKU, GameType.CAT_SUDOKU -> {
                                    scores.sortedWith(
                                        compareBy<ScoreRecord> { it.wrongCount }
                                            .thenBy { it.timeMillis }
                                            .thenByDescending { it.id }
                                    )
                                }
                                else -> {
                                    scores.sortedWith(
                                        compareBy<ScoreRecord> { it.timeMillis }
                                            .thenBy { it.wrongCount }
                                            .thenByDescending { it.id }
                                    )
                                }
                            }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        ) {
                            itemsIndexed(sortedScores) { index, record ->
                                val rank = index + 1
                                val rankColor = when (rank) {
                                    1 -> Color(0xFFFFD700) // Gold
                                    2 -> Color(0xFFC0C0C0) // Silver
                                    3 -> Color(0xFFCD7F32) // Bronze
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }

                                val scoreValueText = when (selectedGameType) {
                                    GameType.SPEED_MATCH -> "${record.score} 次"
                                    GameType.TURTLE_SOUP, GameType.AVATAR_WHACK, GameType.STROOP_EFFECT, GameType.BLOCK_PUZZLE, GameType.FRUIT_MASTER -> "${record.score} 分"
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
                        val sortedGlobalScores = remember(globalScores) {
                            globalScores.sortedBy { it.rank }
                        }
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        ) {
                            itemsIndexed(sortedGlobalScores) { index, record ->
                                val rank = record.rank
                                val rankColor = when (rank) {
                                    1 -> Color(0xFFFFD700) // Gold
                                    2 -> Color(0xFFC0C0C0) // Silver
                                    3 -> Color(0xFFCD7F32) // Bronze
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }

                                val scoreValueText = when (selectedGameType) {
                                    GameType.SPEED_MATCH -> "${record.score} 次"
                                    GameType.TURTLE_SOUP, GameType.AVATAR_WHACK, GameType.STROOP_EFFECT, GameType.BLOCK_PUZZLE, GameType.FRUIT_MASTER -> "${record.score} 分"
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
                                    GameType.TURTLE_SOUP, GameType.AVATAR_WHACK, GameType.STROOP_EFFECT, GameType.BLOCK_PUZZLE, GameType.FRUIT_MASTER -> "${myGlobalRankEntry.score} 分"
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
                } // End of if (mainTab == LeaderboardMainTab.RECORDS)
            }
        }
    }
}

@Composable
fun HotGamesLeaderboardContent(
    hotGames: List<HotGameEntry>,
    totalVotersCount: Int,
    currentMonth: String,
    isFetching: Boolean,
    language: AppLanguage,
    onRefresh: () -> Unit,
    onPlayGame: (GameType) -> Unit,
    onDismiss: () -> Unit
) {
    val maxVotes = (hotGames.maxOfOrNull { it.votes } ?: 1).coerceAtLeast(1)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = Localization.getString("hot_games_title", language),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Text(
                    text = Localization.getString("hot_games_voters_count", language, totalVotersCount) +
                            if (currentMonth.isNotBlank()) " ($currentMonth)" else "",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            IconButton(onClick = {
                SoundManager.playClick()
                onRefresh()
            }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (isFetching) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (hotGames.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = Localization.getString("no_records", language),
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(hotGames) { index, entry ->
                    val gameType = entry.gameType
                    val isTop3 = index < 3
                    val rankColor = when (index) {
                        0 -> Color(0xFFFFB800) // Gold
                        1 -> Color(0xFF9E9E9E) // Silver
                        2 -> Color(0xFFCD7F32) // Bronze
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    val progress = (entry.votes.toFloat() / maxVotes).coerceIn(0f, 1f)

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = when (index) {
                            0 -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            1 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            2 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                        },
                        border = if (index == 0) {
                            androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFFB800).copy(alpha = 0.6f))
                        } else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Rank Badge
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isTop3) rankColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (index) {
                                        0 -> "👑1"
                                        1 -> "🥈2"
                                        2 -> "🥉3"
                                        else -> "#${index + 1}"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = rankColor
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Game Icon
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (gameType == GameType.GLASS_PUZZLE_CUBE) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_isometric_cube),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = getGameTypeIcon(gameType),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Title & Progress Bar
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = Localization.getString(gameType.titleKey, language),
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = Localization.getString("hot_games_votes_format", language, entry.votes),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (index == 0) Color(0xFFFFB800) else MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Play Now Button
                            Button(
                                onClick = {
                                    SoundManager.playClick()
                                    onPlayGame(gameType)
                                },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = Localization.getString("play_now_button", language),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    SoundManager.playClick()
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = Localization.getString("confirm", language))
            }
        }
    }
}

