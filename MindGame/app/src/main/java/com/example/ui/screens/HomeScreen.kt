package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.audio.SoundManager
import com.example.data.model.AppLanguage
import com.example.data.model.GameCategory
import com.example.data.model.GameType
import com.example.data.model.Localization
import com.example.ui.components.AppBackground
import com.example.ui.theme.AppThemeStyle

import com.example.ui.components.FeaturedSpotlightBanner
import com.example.ui.components.NewBadge

data class CategoryItem(
    val category: GameCategory,
    val nameKey: String,
    val descKey: String,
    val icon: ImageVector,
    val iconBgColor: Color,
    val isAvailable: Boolean
)

@Composable
fun HomeScreen(
    playerName: String,
    language: AppLanguage,
    appTheme: AppThemeStyle,
    isCategoryHasNew: (GameCategory) -> Boolean = { false },
    userFavorites: List<GameType> = emptyList(),
    hasVotedThisMonth: Boolean = true,
    currentMonth: String = "",
    onSettingsClick: () -> Unit,
    onLeaderboardClick: () -> Unit = {},
    onVoteClick: () -> Unit = {},
    onChangeNameClick: () -> Unit,
    onCategoryClick: (GameCategory) -> Unit,
    onFeaturedBannerClick: () -> Unit = {}
) {
    val categories = listOf(
        CategoryItem(
            category = GameCategory.TEST,
            nameKey = "category_test",
            descKey = "category_test_desc",
            icon = Icons.Default.Timer,
            iconBgColor = Color(0xFF4F46E5), // Indigo
            isAvailable = true
        ),
        CategoryItem(
            category = GameCategory.BRAIN,
            nameKey = "category_brain",
            descKey = "category_brain_desc",
            icon = Icons.Default.Psychology,
            iconBgColor = Color(0xFF0EA5E9), // Sky Blue
            isAvailable = true
        ),
        CategoryItem(
            category = GameCategory.DEDUCTION,
            nameKey = "category_deduction",
            descKey = "category_deduction_desc",
            icon = Icons.Default.Lightbulb,
            iconBgColor = Color(0xFF10B981), // Emerald
            isAvailable = true
        ),
        CategoryItem(
            category = GameCategory.CASUAL,
            nameKey = "category_casual",
            descKey = "category_casual_desc",
            icon = Icons.Default.SportsEsports,
            iconBgColor = Color(0xFFE07A5F), // Terracotta Warm Amber
            isAvailable = true
        )
    )

    AppBackground(themeStyle = appTheme) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Top Bar: Rectangular Brain Banner Logo + Dual Action Buttons (🏆 + ⚙)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Transparent,
                    modifier = Modifier
                        .height(52.dp)
                        .width(180.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_app_logo_banner),
                        contentDescription = "Left/Right Brain Logo Banner",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 右側精巧雙圖示按鈕組 (🏆 全球風雲榜 + ⚙ 系統設定)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 全球風雲榜按鈕 (42dp x 42dp，若未完成當月投票帶紅點提示)
                    Box {
                        Surface(
                            onClick = {
                                SoundManager.playClick()
                                onLeaderboardClick()
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(14.dp))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = Localization.getString("hall_of_fame_button", language),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        if (!hasVotedThisMonth) {
                            Box(modifier = Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 2.dp)) {
                                NewBadge()
                            }
                        }
                    }

                    // 系統設定按鈕 (42dp x 42dp)
                    Surface(
                        onClick = {
                            SoundManager.playClick()
                            onSettingsClick()
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = Localization.getString("settings_button", language),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Player Profile Box with Name Change Button & Favorite Games Status
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Player",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = Localization.getString("player_name_label", language),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                Text(
                                    text = playerName,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                SoundManager.playClick()
                                onChangeNameClick()
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Name",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = Localization.getString("change_name_button", language),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // 本月最愛遊戲應援條 (❤️ 支援紅點強迫症引導)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (!hasVotedThisMonth) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                SoundManager.playClick()
                                onVoteClick()
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = if (!hasVotedThisMonth) MaterialTheme.colorScheme.primary else Color(0xFFE91E63),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                if (!hasVotedThisMonth) {
                                    Text(
                                        text = if (currentMonth.isNotBlank()) "$currentMonth ${Localization.getString("profile_vote_banner_title", language)}" else Localization.getString("profile_vote_banner_title", language),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                } else {
                                    val favNames = userFavorites.map { Localization.getString(it.titleKey, language) }.joinToString("、")
                                    Text(
                                        text = "${Localization.getString("profile_voted_label", language)}：$favNames",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            if (!hasVotedThisMonth) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = Localization.getString("profile_vote_action_btn", language),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        NewBadge()
                                    }
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = Localization.getString("profile_vote_edit", language),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 新登場焦點橫幅跑馬燈 (Featured Spotlight Banner，無限循環滾動，若未投票可輪播應援提示)
            FeaturedSpotlightBanner(
                language = language,
                marqueeExtraText = if (!hasVotedThisMonth && currentMonth.isNotBlank()) Localization.getString("featured_banner_vote_prompt", language, currentMonth) else null,
                onClick = onFeaturedBannerClick
            )


            Spacer(modifier = Modifier.height(20.dp))

            // First Menu Category Selection (遊戲類別)
            Text(
                text = Localization.getString("game_categories_label", language),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(categories) { item ->
                    CategoryCard(
                        item = item,
                        language = language,
                        hasNewGame = isCategoryHasNew(item.category),
                        onClick = {
                            if (item.isAvailable) {
                                SoundManager.playClick()
                                onCategoryClick(item.category)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryCard(
    item: CategoryItem,
    language: AppLanguage,
    hasNewGame: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = item.isAvailable, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isAvailable) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isAvailable) 4.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        if (item.isAvailable) item.iconBgColor else Color.Gray,
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = Localization.getString(item.nameKey, language),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (item.isAvailable) MaterialTheme.colorScheme.onSurface else Color.Gray
                        )
                    )
                    if (item.isAvailable && hasNewGame) {
                        Spacer(modifier = Modifier.width(8.dp))
                        NewBadge()
                    }
                    if (!item.isAvailable) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Gray.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = Localization.getString("game_coming_soon", language),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.Gray,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = Localization.getString(item.descKey, language),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
