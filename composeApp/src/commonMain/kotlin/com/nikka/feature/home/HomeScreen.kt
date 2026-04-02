package com.nikka.feature.home

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nikka.core.model.DailyTask
import com.nikka.core.model.TaskGroup
import com.nikka.core.ui.theme.DarkBackground
import com.nikka.core.ui.theme.GroupColors
import com.nikka.core.ui.theme.LavenderPrimary
import com.nikka.core.ui.theme.StatusGreen
import com.nikka.core.ui.theme.StatusRed
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel(),
    topBar: @Composable (actions: @Composable () -> Unit) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {
            topBar {}
            HomeContent(
                uiState = uiState,
                onToggleTask = viewModel::toggleTask,
                onShowAddTask = viewModel::showAddTaskDialog,
                onRemoveTask = viewModel::removeTask,
                onRemoveGroup = viewModel::removeGroup,
                onToggleGroupCollapse = viewModel::toggleGroupCollapse,
                onResetGroup = viewModel::resetGroupTasks,
            )
        }
        FloatingActionButton(
            onClick = { viewModel.showAddGroupDialog() },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            containerColor = LavenderPrimary,
            shape = CircleShape,
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "グループ追加",
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }

    HomeDialogs(uiState = uiState, viewModel = viewModel)
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onToggleTask: (String) -> Unit,
    onShowAddTask: (String) -> Unit,
    onRemoveTask: (String) -> Unit,
    onRemoveGroup: (String) -> Unit,
    onToggleGroupCollapse: (String) -> Unit,
    onResetGroup: (String) -> Unit,
) {
    if (uiState.groups.isEmpty()) {
        EmptyState()
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(uiState.groups, key = { it.id }) { group ->
                GroupCard(
                    group = group,
                    tasks = uiState.tasks.filter { it.groupId == group.id },
                    isCollapsed = group.id in uiState.collapsedGroupIds,
                    onToggleCollapse = { onToggleGroupCollapse(group.id) },
                    onToggleTask = onToggleTask,
                    onAddTask = { onShowAddTask(group.id) },
                    onRemoveTask = onRemoveTask,
                    onRemoveGroup = { onRemoveGroup(group.id) },
                    onResetGroup = { onResetGroup(group.id) },
                )
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun HomeDialogs(uiState: HomeUiState, viewModel: HomeViewModel) {
    if (uiState.isAddGroupDialogVisible) {
        InputDialog(
            title = "グループを追加",
            placeholder = "例: 原神、スターレイル...",
            confirmText = "追加",
            onConfirm = viewModel::addGroup,
            onDismiss = viewModel::dismissAddGroupDialog,
        )
    }
    if (uiState.isAddTaskDialogVisible && uiState.addTaskTargetGroupId != null) {
        InputDialog(
            title = "日課を追加",
            placeholder = "例: デイリー任務、樹脂消費...",
            confirmText = "追加",
            onConfirm = { title -> viewModel.addTask(uiState.addTaskTargetGroupId!!, title) },
            onDismiss = viewModel::dismissAddTaskDialog,
        )
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.SportsEsports,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "まだグループがありません",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "右下の＋ボタンからグループを追加しましょう！",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GroupCard(
    group: TaskGroup,
    tasks: List<DailyTask>,
    isCollapsed: Boolean,
    onToggleCollapse: () -> Unit,
    onToggleTask: (String) -> Unit,
    onAddTask: () -> Unit,
    onRemoveTask: (String) -> Unit,
    onRemoveGroup: () -> Unit,
    onResetGroup: () -> Unit,
) {
    val accentColor = GroupColors[group.colorIndex % GroupColors.size]
    val allCompleted = tasks.isNotEmpty() && tasks.all { it.isCompleted }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .animateContentSize()
            .padding(16.dp),
    ) {
        GroupCardHeader(
            group = group,
            accentColor = accentColor,
            allCompleted = allCompleted,
            isCollapsed = isCollapsed,
            onToggleCollapse = onToggleCollapse,
            onAddTask = onAddTask,
            onResetGroup = onResetGroup,
            onRemoveGroup = onRemoveGroup,
        )
        if (!isCollapsed) {
            GroupCardBody(
                tasks = tasks,
                accentColor = accentColor,
                onToggleTask = onToggleTask,
                onRemoveTask = onRemoveTask,
            )
        }
    }
}

@Composable
private fun GroupCardHeader(
    group: TaskGroup,
    accentColor: androidx.compose.ui.graphics.Color,
    allCompleted: Boolean,
    isCollapsed: Boolean,
    onToggleCollapse: () -> Unit,
    onAddTask: () -> Unit,
    onResetGroup: () -> Unit,
    onRemoveGroup: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggleCollapse),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = if (isCollapsed) {
                    Icons.Rounded.KeyboardArrowRight
                } else {
                    Icons.Rounded.KeyboardArrowDown
                },
                contentDescription = if (isCollapsed) "展開" else "折りたたみ",
                modifier = Modifier.size(20.dp),
                tint = accentColor,
            )
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (allCompleted) StatusGreen else StatusRed),
            )
            Text(
                text = group.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onResetGroup, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "リセット",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onAddTask) {
                Text("+ 追加", color = accentColor)
            }
            IconButton(onClick = onRemoveGroup, modifier = Modifier.size(28.dp)) {
                Text(
                    "×",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun GroupCardBody(
    tasks: List<DailyTask>,
    accentColor: androidx.compose.ui.graphics.Color,
    onToggleTask: (String) -> Unit,
    onRemoveTask: (String) -> Unit,
) {
    if (tasks.isEmpty()) {
        Text(
            text = "日課を追加してみましょう",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 22.dp, top = 4.dp),
        )
    } else {
        Column(modifier = Modifier.padding(top = 4.dp)) {
            tasks.forEach { task ->
                TaskRow(
                    task = task,
                    accentColor = accentColor,
                    onToggle = { onToggleTask(task.id) },
                    onRemove = { onRemoveTask(task.id) },
                )
            }
        }
    }
}

@Composable
private fun TaskRow(
    task: DailyTask,
    accentColor: androidx.compose.ui.graphics.Color,
    onToggle: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onToggle)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = task.isCompleted,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = accentColor,
                uncheckedColor = accentColor.copy(alpha = 0.5f),
            ),
        )
        Text(
            text = task.title,
            style = MaterialTheme.typography.bodyLarge.copy(
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
            ),
            color = if (task.isCompleted) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(28.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "削除",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InputDialog(
    title: String,
    placeholder: String,
    confirmText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title)
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(placeholder) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LavenderPrimary,
                    cursorColor = LavenderPrimary,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onConfirm(text) }),
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text) },
                colors = ButtonDefaults.buttonColors(containerColor = LavenderPrimary),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(confirmText)
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
