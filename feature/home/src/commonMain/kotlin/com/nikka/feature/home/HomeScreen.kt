package com.nikka.feature.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.onClick
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.nikka.core.model.DailyTask
import com.nikka.core.model.TaskGroup
import com.nikka.core.ui.component.ProvideTopBarActions
import com.nikka.core.ui.theme.StatusGreen
import com.nikka.core.ui.theme.StatusRed
import org.koin.compose.viewmodel.koinViewModel
import sh.calvin.reorderable.ReorderableColumn
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    ProvideTopBarActions {
        IconButton(onClick = viewModel::refreshAutoReset) {
            Icon(
                imageVector = Icons.Rounded.Sync,
                contentDescription = "リセット時刻に達した日課をリセット",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            HomeContent(
                uiState = uiState,
                onToggleTask = viewModel::toggleTask,
                onShowAddTask = viewModel::showAddTaskDialog,
                onRemoveTask = viewModel::removeTask,
                onRemoveGroup = viewModel::showDeleteGroupConfirm,
                onToggleGroupCollapse = viewModel::toggleGroupCollapse,
                onResetGroup = viewModel::resetGroupTasks,
                onMoveGroup = viewModel::moveGroup,
                onSettleDrag = viewModel::settleDrag,
                onMoveTask = viewModel::moveTask,
                onSetResetHour = viewModel::showResetHourDialog,
            )
        }
        FloatingActionButton(
            onClick = { viewModel.showAddGroupDialog() },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            containerColor = MaterialTheme.colorScheme.primary,
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
    onMoveGroup: (Int, Int) -> Unit,
    onSettleDrag: () -> Unit,
    onMoveTask: (String, Int, Int) -> Unit,
    onSetResetHour: (String) -> Unit,
) {
    if (uiState.groups.isEmpty()) {
        EmptyState()
    } else {
        val lazyListState = rememberLazyListState()
        val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
            onMoveGroup(from.index, to.index)
        }
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(uiState.groups, key = { it.id }) { group ->
                ReorderableItem(reorderableLazyListState, key = group.id) {
                    GroupCard(
                        modifier = Modifier.animateItem(),
                        group = group,
                        tasks = uiState.tasks.filter { it.groupId == group.id },
                        isCollapsed = group.id in uiState.collapsedGroupIds,
                        onToggleCollapse = { onToggleGroupCollapse(group.id) },
                        onToggleTask = onToggleTask,
                        onAddTask = { onShowAddTask(group.id) },
                        onRemoveTask = onRemoveTask,
                        onRemoveGroup = { onRemoveGroup(group.id) },
                        onResetGroup = { onResetGroup(group.id) },
                        onSetResetHour = { onSetResetHour(group.id) },
                        onMoveTask = onMoveTask,
                        dragModifier = Modifier.draggableHandle(
                            onDragStopped = { onSettleDrag() },
                        ),
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
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
    val addTaskGroupId = uiState.addTaskTargetGroupId
    if (uiState.isAddTaskDialogVisible && addTaskGroupId != null) {
        InputDialog(
            title = "日課を追加",
            placeholder = "例: デイリー任務、樹脂消費...",
            confirmText = "追加",
            onConfirm = { title -> viewModel.addTask(addTaskGroupId, title) },
            onDismiss = viewModel::dismissAddTaskDialog,
        )
    }
    if (uiState.deleteGroupConfirmId != null) {
        val groupName = uiState.groups
            .find { it.id == uiState.deleteGroupConfirmId }?.name ?: ""
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteGroupConfirm,
            title = { Text("グループを削除") },
            text = { Text("「$groupName」とその日課をすべて削除しますか？") },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmDeleteGroup,
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("削除")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteGroupConfirm) {
                    Text("キャンセル")
                }
            },
            shape = RoundedCornerShape(20.dp),
        )
    }
    if (uiState.resetHourTargetGroupId != null) {
        val group = uiState.groups.find { it.id == uiState.resetHourTargetGroupId }
        if (group != null) {
            ResetHourDialog(
                currentHour = group.resetHour,
                onConfirm = { hour -> viewModel.setResetHour(group.id, hour) },
                onDismiss = viewModel::dismissResetHourDialog,
            )
        }
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
    modifier: Modifier = Modifier,
    group: TaskGroup,
    tasks: List<DailyTask>,
    isCollapsed: Boolean,
    onToggleCollapse: () -> Unit,
    onToggleTask: (String) -> Unit,
    onAddTask: () -> Unit,
    onRemoveTask: (String) -> Unit,
    onRemoveGroup: () -> Unit,
    onResetGroup: () -> Unit,
    onSetResetHour: () -> Unit,
    onMoveTask: (String, Int, Int) -> Unit,
    dragModifier: Modifier = Modifier,
) {
    val allCompleted = tasks.isNotEmpty() && tasks.all { it.isCompleted }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .animateContentSize()
            .padding(16.dp),
    ) {
        GroupCardHeader(
            group = group,
            allCompleted = allCompleted,
            isCollapsed = isCollapsed,
            onToggleCollapse = onToggleCollapse,
            onAddTask = onAddTask,
            onResetGroup = onResetGroup,
            onRemoveGroup = onRemoveGroup,
            onSetResetHour = onSetResetHour,
            dragModifier = dragModifier,
        )
        if (!isCollapsed) {
            GroupCardBody(
                group = group,
                tasks = tasks,
                onToggleTask = onToggleTask,
                onRemoveTask = onRemoveTask,
                onMoveTask = onMoveTask,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupCardHeader(
    group: TaskGroup,
    allCompleted: Boolean,
    isCollapsed: Boolean,
    onToggleCollapse: () -> Unit,
    onAddTask: () -> Unit,
    onResetGroup: () -> Unit,
    onRemoveGroup: () -> Unit,
    onSetResetHour: () -> Unit,
    dragModifier: Modifier = Modifier,
) {
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuOffset by remember { mutableStateOf(DpOffset.Zero) }
    var lastPointerPosition by remember { mutableStateOf(Offset.Zero) }
    var anchorHeight by remember { mutableStateOf(0) }
    val density = LocalDensity.current

    Box(modifier = Modifier.onSizeChanged { anchorHeight = it.height }) {
        GroupCardHeaderContent(
            group = group,
            allCompleted = allCompleted,
            isCollapsed = isCollapsed,
            onToggleCollapse = onToggleCollapse,
            onResetGroup = onResetGroup,
            dragModifier = dragModifier,
            onPointerPositionChanged = { lastPointerPosition = it },
            onSecondaryClick = {
                with(density) {
                    contextMenuOffset = DpOffset(
                        lastPointerPosition.x.toDp(),
                        lastPointerPosition.y.toDp() - anchorHeight.toDp(),
                    )
                }
                showContextMenu = true
            },
        )
        GroupContextMenu(
            expanded = showContextMenu,
            offset = contextMenuOffset,
            resetHour = group.resetHour,
            onDismiss = { showContextMenu = false },
            onAddTask = {
                showContextMenu = false
                onAddTask()
            },
            onSetResetHour = {
                showContextMenu = false
                onSetResetHour()
            },
            onRemove = {
                showContextMenu = false
                onRemoveGroup()
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupCardHeaderContent(
    group: TaskGroup,
    allCompleted: Boolean,
    isCollapsed: Boolean,
    onToggleCollapse: () -> Unit,
    onResetGroup: () -> Unit,
    dragModifier: Modifier = Modifier,
    onPointerPositionChanged: (Offset) -> Unit,
    onSecondaryClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        event.changes.firstOrNull()?.let {
                            onPointerPositionChanged(it.position)
                        }
                    }
                }
            }
            .onClick(
                matcher = PointerMatcher.mouse(PointerButton.Secondary),
                onClick = onSecondaryClick,
            )
            .clickable(onClick = onToggleCollapse),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GroupCardTitle(
            groupName = group.name,
            allCompleted = allCompleted,
            isCollapsed = isCollapsed,
            modifier = Modifier.weight(1f),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.DragIndicator,
                contentDescription = "並べ替え",
                modifier = Modifier.size(20.dp).then(dragModifier),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(onClick = onResetGroup, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "リセット",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun GroupCardTitle(
    groupName: String,
    allCompleted: Boolean,
    isCollapsed: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
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
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (allCompleted) StatusGreen else StatusRed),
        )
        Text(
            text = groupName,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun GroupContextMenu(
    expanded: Boolean,
    offset: DpOffset = DpOffset.Zero,
    resetHour: Int?,
    onDismiss: () -> Unit,
    onAddTask: () -> Unit,
    onSetResetHour: () -> Unit,
    onRemove: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        offset = offset,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
    ) {
        DropdownMenuItem(
            text = { Text("日課を追加") },
            onClick = onAddTask,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
        )
        DropdownMenuItem(
            text = {
                val label = if (resetHour != null) {
                    "リセット時刻: $resetHour:00"
                } else {
                    "リセット時刻を設定"
                }
                Text(label)
            },
            onClick = onSetResetHour,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
        DropdownMenuItem(
            text = {
                Text("削除", color = StatusRed)
            },
            onClick = onRemove,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = StatusRed,
                )
            },
        )
    }
}

@Composable
private fun GroupCardBody(
    group: TaskGroup,
    tasks: List<DailyTask>,
    onToggleTask: (String) -> Unit,
    onRemoveTask: (String) -> Unit,
    onMoveTask: (String, Int, Int) -> Unit,
) {
    if (tasks.isEmpty()) {
        Text(
            text = "日課を追加してみましょう",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 22.dp, top = 4.dp),
        )
    } else {
        ReorderableColumn(
            list = tasks,
            onSettle = { fromIndex, toIndex ->
                onMoveTask(group.id, fromIndex, toIndex)
            },
            modifier = Modifier.padding(top = 4.dp),
        ) { _, task, _ ->
            key(task.id) {
                ReorderableItem {
                    TaskRow(
                        task = task,
                        onToggle = { onToggleTask(task.id) },
                        onRemove = { onRemoveTask(task.id) },
                        dragModifier = Modifier.draggableHandle(),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskRow(
    task: DailyTask,
    onToggle: () -> Unit,
    onRemove: () -> Unit,
    dragModifier: Modifier = Modifier,
) {
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuOffset by remember { mutableStateOf(DpOffset.Zero) }
    var lastPointerPosition by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(TASK_ROW_HEIGHT)
                .clip(RoundedCornerShape(8.dp))
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            event.changes.firstOrNull()?.let {
                                lastPointerPosition = it.position
                            }
                        }
                    }
                }
                .onClick(
                    matcher = PointerMatcher.mouse(PointerButton.Secondary),
                    onClick = {
                        with(density) {
                            contextMenuOffset = DpOffset(
                                lastPointerPosition.x.toDp(),
                                lastPointerPosition.y.toDp() - TASK_ROW_HEIGHT,
                            )
                        }
                        showContextMenu = true
                    },
                )
                .clickable(onClick = onToggle),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.DragIndicator,
                contentDescription = "並べ替え",
                modifier = Modifier.size(20.dp).then(dragModifier),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TaskRowContent(task = task, onToggle = onToggle)
        }
        TaskContextMenu(
            expanded = showContextMenu,
            offset = contextMenuOffset,
            onDismiss = { showContextMenu = false },
            onRemove = {
                showContextMenu = false
                onRemove()
            },
        )
    }
}

@Composable
private fun RowScope.TaskRowContent(task: DailyTask, onToggle: () -> Unit) {
    Checkbox(
        checked = task.isCompleted,
        onCheckedChange = { onToggle() },
        colors = CheckboxDefaults.colors(
            checkedColor = MaterialTheme.colorScheme.primary,
            uncheckedColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
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
}

@Composable
private fun TaskContextMenu(
    expanded: Boolean,
    offset: DpOffset = DpOffset.Zero,
    onDismiss: () -> Unit,
    onRemove: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        offset = offset,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
    ) {
        DropdownMenuItem(
            text = {
                Text("削除", color = StatusRed)
            },
            onClick = onRemove,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = StatusRed,
                )
            },
        )
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
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

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
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { if (text.isNotBlank()) onConfirm(text) },
                ),
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
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

@Composable
private fun ResetHourDialog(
    currentHour: Int?,
    onConfirm: (Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedHour by remember { mutableStateOf(currentHour) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自動リセット時刻") },
        text = {
            ResetHourSelector(
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
            if (currentHour != null) {
                TextButton(onClick = { onConfirm(null) }) {
                    Text("解除")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("キャンセル")
                }
            }
        },
        shape = RoundedCornerShape(20.dp),
    )
}

@Composable
private fun ResetHourSelector(
    selectedHour: Int?,
    onHourSelected: (Int) -> Unit,
) {
    Column {
        Text(
            text = if (selectedHour != null) "$selectedHour:00 にリセット" else "時刻を選択",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(RESET_HOUR_GRID_COLUMNS),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.height(RESET_HOUR_GRID_HEIGHT),
        ) {
            items(HOURS_IN_DAY) { hour ->
                val isSelected = hour == selectedHour
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        )
                        .clickable { onHourSelected(hour) }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "$hour:00",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }
        }
    }
}

// タスク行の固定高さ。変更時はコンテキストメニューのオフセット計算にも影響する
private val TASK_ROW_HEIGHT = 40.dp
private const val HOURS_IN_DAY = 24
private const val RESET_HOUR_GRID_COLUMNS = 4
private val RESET_HOUR_GRID_HEIGHT = 240.dp
