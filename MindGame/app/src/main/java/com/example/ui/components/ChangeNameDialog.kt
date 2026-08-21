package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.Localization

@Composable
fun ChangeNameDialog(
    currentName: String,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var nameText by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = Localization.getString("input_name_title", language))
        },
        text = {
            Column {
                Text(text = Localization.getString("input_name_hint", language))
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    singleLine = true,
                    label = { Text(Localization.getString("player_name_label", language)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (nameText.isNotBlank()) {
                        onConfirm(nameText.trim())
                    }
                }
            ) {
                Text(text = Localization.getString("confirm", language))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = Localization.getString("cancel", language))
            }
        }
    )
}
