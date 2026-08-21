package com.example.ui.components

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.data.model.Localization
import com.example.ui.theme.AppThemeStyle

private enum class SettingsTab {
    THEME,
    LANGUAGE,
    AUDIO,
    ABOUT
}

@Composable
fun SettingsDialog(
    currentTheme: AppThemeStyle,
    currentLanguage: AppLanguage,
    isSfxEnabled: Boolean,
    isBgmEnabled: Boolean,
    onSelectTheme: (AppThemeStyle) -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onToggleSfx: (Boolean) -> Unit,
    onToggleBgm: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var activeTab by remember { mutableStateOf(SettingsTab.THEME) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = Localization.getString("settings_title", currentLanguage),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
            ) {
                // Settings Top Filter / Category Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TabChip(
                        icon = Icons.Default.Palette,
                        title = Localization.getString("theme_selector", currentLanguage),
                        isSelected = activeTab == SettingsTab.THEME,
                        onClick = { activeTab = SettingsTab.THEME },
                        modifier = Modifier.weight(1f)
                    )
                    TabChip(
                        icon = Icons.Default.Language,
                        title = Localization.getString("language_selector", currentLanguage),
                        isSelected = activeTab == SettingsTab.LANGUAGE,
                        onClick = { activeTab = SettingsTab.LANGUAGE },
                        modifier = Modifier.weight(1f)
                    )
                    TabChip(
                        icon = Icons.Default.VolumeUp,
                        title = Localization.getString("audio_settings_title", currentLanguage).take(2),
                        isSelected = activeTab == SettingsTab.AUDIO,
                        onClick = { activeTab = SettingsTab.AUDIO },
                        modifier = Modifier.weight(1f)
                    )
                    TabChip(
                        icon = Icons.Default.Info,
                        title = Localization.getString("about_section_title", currentLanguage).take(2),
                        isSelected = activeTab == SettingsTab.ABOUT,
                        onClick = { activeTab = SettingsTab.ABOUT },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Box(modifier = Modifier.weight(1f)) {
                    when (activeTab) {
                        SettingsTab.THEME -> {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(AppThemeStyle.values()) { style ->
                                    val isSelected = style == currentTheme
                                    val themeName = Localization.getString(style.key, currentLanguage)
                                    val (bgPreview, primaryPreview) = getThemePreviewColors(style)

                                    Surface(
                                        onClick = { onSelectTheme(style) },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        border = if (isSelected) {
                                            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                        } else null,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                                .fillMaxWidth()
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(bgPreview)
                                                    .border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(12.dp)
                                                        .align(Alignment.Center)
                                                        .clip(CircleShape)
                                                        .background(primaryPreview)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Text(
                                                text = themeName,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                                ),
                                                modifier = Modifier.weight(1f)
                                            )

                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        SettingsTab.LANGUAGE -> {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                AppLanguage.values().forEach { lang ->
                                    val isSelected = lang == currentLanguage
                                    Surface(
                                        onClick = { onSelectLanguage(lang) },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        border = if (isSelected) {
                                            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                        } else null,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .padding(horizontal = 16.dp, vertical = 14.dp)
                                                .fillMaxWidth()
                                        ) {
                                            Text(
                                                text = lang.displayName,
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                                ),
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        SettingsTab.AUDIO -> {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // SFX Switch Card
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.VolumeUp,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = Localization.getString("sfx_switch_label", currentLanguage),
                                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                                                )
                                                Text(
                                                    text = if (isSfxEnabled) Localization.getString("state_enabled", currentLanguage) else Localization.getString("state_disabled", currentLanguage),
                                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                )
                                            }
                                        }
                                        Switch(
                                            checked = isSfxEnabled,
                                            onCheckedChange = onToggleSfx
                                        )
                                    }
                                }

                                // BGM Switch Card
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.MusicNote,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = Localization.getString("bgm_switch_label", currentLanguage),
                                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                                                )
                                                Text(
                                                    text = if (isBgmEnabled) Localization.getString("state_enabled", currentLanguage) else Localization.getString("state_disabled", currentLanguage),
                                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                )
                                            }
                                        }
                                        Switch(
                                            checked = isBgmEnabled,
                                            onCheckedChange = onToggleBgm
                                        )
                                    }
                                }
                            }
                        }

                        SettingsTab.ABOUT -> {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                item {
                                    Card(
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            // Version and Name
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = Localization.getString("about_app_name_value", currentLanguage),
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = MaterialTheme.colorScheme.primaryContainer
                                                ) {
                                                    Text(
                                                        text = Localization.getString("about_version_value", currentLanguage),
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                                        )
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))
                                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                                            Spacer(modifier = Modifier.height(8.dp))

                                            // Dev Team
                                            Text(
                                                text = Localization.getString("about_dev_label", currentLanguage),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                            Text(
                                                text = Localization.getString("about_dev_value", currentLanguage),
                                                style = MaterialTheme.typography.bodyMedium
                                            )

                                            Spacer(modifier = Modifier.height(10.dp))

                                            // Description
                                            Text(
                                                text = Localization.getString("about_desc_label", currentLanguage),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                            Text(
                                                text = Localization.getString("about_desc_value", currentLanguage),
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                ),
                                                lineHeight = 18.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(Localization.getString("confirm", currentLanguage))
            }
        }
    )
}

@Composable
private fun TabChip(
    icon: ImageVector,
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                maxLines = 1
            )
        }
    }
}

private fun getThemePreviewColors(style: AppThemeStyle): Pair<Color, Color> {
    return when (style) {
        AppThemeStyle.SNOW_WHITE -> Pair(Color(0xFFFFFFFF), Color(0xFF2563EB))
        AppThemeStyle.DARK -> Pair(Color(0xFF121218), Color(0xFFD0BCFF))
        AppThemeStyle.MECHANICAL -> Pair(Color(0xFF18181B), Color(0xFFF59E0B))
        AppThemeStyle.CUTE -> Pair(Color(0xFFFFF0F5), Color(0xFFEC4899))
        AppThemeStyle.SUNNY -> Pair(Color(0xFFFFFBEB), Color(0xFFD97706))
        AppThemeStyle.CORPORATE -> Pair(Color(0xFFF1F5F9), Color(0xFF1E3A8A))
        AppThemeStyle.CASUAL -> Pair(Color(0xFFF0FDF4), Color(0xFF059669))
    }
}
