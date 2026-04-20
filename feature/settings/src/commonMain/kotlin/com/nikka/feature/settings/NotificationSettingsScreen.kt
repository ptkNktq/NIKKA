package com.nikka.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.nikka.core.model.NotificationSettings
import org.koin.compose.koinInject

@Composable
fun NotificationSettingsScreen(
    viewModel: SettingsViewModel = koinInject(),
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        NotificationToggleRow(
            enabled = state.settings.enabled,
            onToggle = viewModel::setEnabled,
        )
        NotificationFields(
            settings = state.settings,
            onWebhookUrlChange = viewModel::setWebhookUrl,
            onMessageChange = viewModel::setMessage,
            onHourClick = viewModel::showHourDialog,
        )
    }

    if (state.isHourDialogVisible) {
        HourPickerDialog(
            currentHour = state.settings.hour,
            onConfirm = { hour ->
                viewModel.setHour(hour)
                viewModel.dismissHourDialog()
            },
            onDismiss = viewModel::dismissHourDialog,
        )
    }
}

@Composable
private fun NotificationFields(
    settings: NotificationSettings,
    onWebhookUrlChange: (String) -> Unit,
    onMessageChange: (String) -> Unit,
    onHourClick: () -> Unit,
) {
    OutlinedTextField(
        value = settings.webhookUrl,
        onValueChange = onWebhookUrlChange,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        label = { Text("Webhook URL") },
        placeholder = { Text("https://discord.com/api/webhooks/...") },
        singleLine = true,
        enabled = settings.enabled,
    )
    OutlinedTextField(
        value = settings.message,
        onValueChange = onMessageChange,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        label = { Text("メッセージ") },
        minLines = MESSAGE_MIN_LINES,
        enabled = settings.enabled,
    )
    NotificationHourRow(
        hour = settings.hour,
        enabled = settings.enabled,
        onClick = onHourClick,
    )
}

@Composable
private fun NotificationToggleRow(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Notifications,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "未達成通知",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "指定時刻に未達成の日課があれば Discord に通知します",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = enabled, onCheckedChange = onToggle)
    }
}

@Composable
private fun NotificationHourRow(
    hour: Int,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val titleColor = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_ALPHA)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Schedule,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = titleColor,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "通知時刻",
                style = MaterialTheme.typography.bodyLarge,
                color = titleColor,
            )
            Text(
                text = "$hour:00",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HourPickerDialog(
    currentHour: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedHour by remember { mutableStateOf(currentHour) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("通知時刻") },
        text = {
            HourGrid(
                selectedHour = selectedHour,
                onHourSelected = { selectedHour = it },
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedHour) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("設定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        },
        shape = RoundedCornerShape(20.dp),
    )
}

@Composable
private fun HourGrid(
    selectedHour: Int,
    onHourSelected: (Int) -> Unit,
) {
    Column {
        Text(
            text = "$selectedHour:00 に通知",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(HOUR_GRID_COLUMNS),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.height(HOUR_GRID_HEIGHT),
        ) {
            items(HOURS_IN_DAY) { hour ->
                HourCell(
                    hour = hour,
                    isSelected = hour == selectedHour,
                    onClick = { onHourSelected(hour) },
                )
            }
        }
    }
}

@Composable
private fun HourCell(
    hour: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surface
                },
            )
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "$hour:00",
            style = MaterialTheme.typography.bodySmall,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

private const val HOURS_IN_DAY = 24
private const val HOUR_GRID_COLUMNS = 4
private val HOUR_GRID_HEIGHT = 240.dp
private const val MESSAGE_MIN_LINES = 2
private const val DISABLED_ALPHA = 0.38f
