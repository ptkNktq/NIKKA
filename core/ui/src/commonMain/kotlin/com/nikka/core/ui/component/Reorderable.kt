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
    private var lastSwapDirection by mutableIntStateOf(0)

    val isDragging: Boolean get() = draggedIndex >= 0

    internal fun startDrag(index: Int) {
        draggedIndex = index
        dragOffset = 0f
        lastSwapDirection = 0
    }

    internal fun endDrag() {
        draggedIndex = -1
        dragOffset = 0f
        lastSwapDirection = 0
    }

    internal fun onDrag(
        deltaY: Float,
        itemCount: Int,
        onMove: (from: Int, to: Int) -> Unit,
    ) {
        dragOffset += deltaY
        val height = itemHeights[draggedIndex] ?: return
        if (height <= 0f) return
        resetDirectionLockIfCrossedZero()
        trySwap(itemCount, height, onMove)
    }

    private fun resetDirectionLockIfCrossedZero() {
        val shouldReset = (lastSwapDirection > 0 && dragOffset <= 0) ||
            (lastSwapDirection < 0 && dragOffset >= 0)
        if (shouldReset) lastSwapDirection = 0
    }

    private fun trySwap(itemCount: Int, fallbackHeight: Float, onMove: (Int, Int) -> Unit) {
        when {
            dragOffset > 0 && draggedIndex < itemCount - 1 && lastSwapDirection >= 0 -> {
                val nextHeight = itemHeights[draggedIndex + 1] ?: fallbackHeight
                if (dragOffset > nextHeight / 2) {
                    swapHeights(draggedIndex, draggedIndex + 1)
                    onMove(draggedIndex, draggedIndex + 1)
                    draggedIndex++
                    dragOffset -= nextHeight
                    lastSwapDirection = 1
                }
            }
            dragOffset < 0 && draggedIndex > 0 && lastSwapDirection <= 0 -> {
                val prevHeight = itemHeights[draggedIndex - 1] ?: fallbackHeight
                if (dragOffset < -prevHeight / 2) {
                    swapHeights(draggedIndex, draggedIndex - 1)
                    onMove(draggedIndex, draggedIndex - 1)
                    draggedIndex--
                    dragOffset += prevHeight
                    lastSwapDirection = -1
                }
            }
        }
    }

    private fun swapHeights(indexA: Int, indexB: Int) {
        val heightA = itemHeights[indexA]
        val heightB = itemHeights[indexB]
        if (heightA != null) itemHeights[indexB] = heightA
        if (heightB != null) itemHeights[indexA] = heightB
    }
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
                    onDragStart = { state.startDrag(currentIndex) },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        state.onDrag(dragAmount.y, currentItemCount, currentOnMove)
                    },
                    onDragEnd = { state.endDrag() },
                    onDragCancel = { state.endDrag() },
                )
            },
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
