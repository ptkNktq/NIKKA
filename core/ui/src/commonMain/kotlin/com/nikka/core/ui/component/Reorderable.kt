@file:Suppress("MatchingDeclarationName")

package com.nikka.core.ui.component

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

class ReorderState {
    var draggedIndex by mutableIntStateOf(-1)
        internal set
    var dragOffset by mutableFloatStateOf(0f)
        internal set
    internal val itemHeights = mutableStateMapOf<Int, Float>()

    val isDragging: Boolean get() = draggedIndex >= 0
}

@Composable
fun rememberReorderState(): ReorderState = remember { ReorderState() }

fun Modifier.reorderableItem(
    state: ReorderState,
    index: Int,
): Modifier = composed {
    DisposableEffect(index) {
        onDispose { state.itemHeights.remove(index) }
    }
    this
        .onSizeChanged { state.itemHeights[index] = it.height.toFloat() }
        .zIndex(if (state.draggedIndex == index) 1f else 0f)
        .graphicsLayer {
            translationY = if (state.draggedIndex == index) state.dragOffset else 0f
        }
}

@Composable
fun DragHandle(
    state: ReorderState,
    index: Int,
    itemCount: Int,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentIndex by rememberUpdatedState(index)
    val currentItemCount by rememberUpdatedState(itemCount)
    val currentOnMove by rememberUpdatedState(onMove)

    Icon(
        imageVector = Icons.Rounded.DragIndicator,
        contentDescription = "並べ替え",
        modifier = modifier
            .size(20.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        state.draggedIndex = currentIndex
                        state.dragOffset = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        state.dragOffset += dragAmount.y
                        val height = state.itemHeights[state.draggedIndex] ?: return@detectDragGestures
                        if (height <= 0f) return@detectDragGestures
                        when {
                            state.dragOffset > 0 && state.draggedIndex < currentItemCount - 1 -> {
                                val nextHeight = state.itemHeights[state.draggedIndex + 1] ?: height
                                if (state.dragOffset > nextHeight / 2) {
                                    currentOnMove(state.draggedIndex, state.draggedIndex + 1)
                                    state.draggedIndex++
                                    state.dragOffset = (state.dragOffset - nextHeight)
                                        .coerceAtLeast(0f)
                                }
                            }
                            state.dragOffset < 0 && state.draggedIndex > 0 -> {
                                val prevHeight = state.itemHeights[state.draggedIndex - 1] ?: height
                                if (state.dragOffset < -prevHeight / 2) {
                                    currentOnMove(state.draggedIndex, state.draggedIndex - 1)
                                    state.draggedIndex--
                                    state.dragOffset = (state.dragOffset + prevHeight)
                                        .coerceAtMost(0f)
                                }
                            }
                        }
                    },
                    onDragEnd = {
                        state.draggedIndex = -1
                        state.dragOffset = 0f
                    },
                    onDragCancel = {
                        state.draggedIndex = -1
                        state.dragOffset = 0f
                    },
                )
            },
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
