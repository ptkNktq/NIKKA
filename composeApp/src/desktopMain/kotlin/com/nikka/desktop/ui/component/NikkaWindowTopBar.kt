package com.nikka.desktop.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowScope
import com.nikka.core.ui.component.NikkaTopBarContent
import com.nikka.core.ui.theme.WindowClose
import com.nikka.core.ui.theme.WindowControlIcon
import com.nikka.core.ui.theme.WindowMinimize

@Composable
fun WindowScope.NikkaWindowTopBar(
    onMinimize: () -> Unit,
    onClose: () -> Unit,
    actions: @Composable () -> Unit = {},
) {
    WindowDraggableArea {
        NikkaTopBarContent(
            actions = actions,
            windowControls = {
                WindowControlButtons(
                    onMinimize = onMinimize,
                    onClose = onClose,
                )
            },
        )
    }
}

@Composable
private fun WindowControlButtons(
    onMinimize: () -> Unit,
    onClose: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WindowButton(
            color = WindowMinimize,
            icon = Icons.Rounded.Remove,
            contentDescription = "最小化",
            onClick = onMinimize,
        )
        WindowButton(
            color = WindowClose,
            icon = Icons.Rounded.Close,
            contentDescription = "閉じる",
            onClick = onClose,
        )
    }
}

@Composable
private fun WindowButton(
    color: Color,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(20.dp),
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(10.dp),
                tint = WindowControlIcon,
            )
        }
    }
}
