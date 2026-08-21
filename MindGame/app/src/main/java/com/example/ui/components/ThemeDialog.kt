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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.Localization
import com.example.ui.theme.AppThemeStyle

@Composable
fun ThemeDialog(
    currentTheme: AppThemeStyle,
    language: AppLanguage,
    onSelectTheme: (AppThemeStyle) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = Localization.getString("theme_selector", language),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(AppThemeStyle.values()) { style ->
                    val isSelected = style == currentTheme
                    val themeName = Localization.getString(style.key, language)

                    val (bgPreview, primaryPreview) = getThemePreviewColors(style)

                    Surface(
                        onClick = {
                            onSelectTheme(style)
                        },
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
                            // Color Preview Circles
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
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(Localization.getString("confirm", language))
            }
        }
    )
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
