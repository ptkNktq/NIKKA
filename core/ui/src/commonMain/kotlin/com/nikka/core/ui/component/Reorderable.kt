@file:Suppress("MatchingDeclarationName")

package com.nikka.core.ui.component

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ReorderState {
    var draggedIndex by mutableIntStateOf(-1)
        internal set
    var dragOffset by mutableFloatStateOf(0f)
        internal set
    internal val itemHeights = mutableStateMapOf<Int, Float>()
    internal var lazyListState: LazyListState? = null
    private var lastSwapDirection by mutableIntStateOf(0)
    internal var currentItemCount by mutableIntStateOf(0)
    internal var currentOnMove: ((Int, Int) -> Unit)? = null

    internal var isAnimating by mutableStateOf(false)
        private set

    val isDragging: Boolean get() = draggedIndex >= 0

    internal fun startDrag(index: Int) {
        if (isAnimating) return
        draggedIndex = index
        dragOffset = 0f
        lastSwapDirection = 0
    }

    internal suspend fun endDrag() {
        if (draggedIndex < 0) return
        isAnimating = true
        try {
            animate(dragOffset, 0f, animationSpec = tween(SETTLE_DURATION_MS)) { value, _ ->
                dragOffset = value
            }
        } finally {
            draggedIndex = -1
            dragOffset = 0f
            lastSwapDirection = 0
            currentOnMove = null
            isAnimating = false
        }
    }

    internal fun onDrag(
        deltaY: Float,
        itemCount: Int,
        onMove: (from: Int, to: Int) -> Unit,
    ) {
        currentItemCount = itemCount
        currentOnMove = onMove
        val oldOffset = dragOffset
        dragOffset += deltaY
        resetDirectionLockOnDrag(oldOffset)
        checkSwap()
    }

    private fun resetDirectionLockOnDrag(oldOffset: Float) {
        val crossedZero = (oldOffset > 0 && dragOffset <= 0) || (oldOffset < 0 && dragOffset >= 0)
        if (crossedZero) lastSwapDirection = 0
    }

    private fun checkSwap() {
        val height = itemHeights[draggedIndex] ?: return
        val onMove = currentOnMove ?: return
        if (height > 0f) {
            trySwap(currentItemCount, height, onMove)
        }
    }

    private fun trySwap(itemCount: Int, fallbackHeight: Float, onMove: (Int, Int) -> Unit) {
        when {
            dragOffset > 0 && draggedIndex < itemCount - 1 && lastSwapDirection >= 0 -> {
                val nextHeight = itemHeights[draggedIndex + 1] ?: fallbackHeight
                val slotDistance = getSlotDistance(draggedIndex, draggedIndex + 1) ?: nextHeight
                if (dragOffset > nextHeight / 2) {
                    swapHeights(draggedIndex, draggedIndex + 1)
                    onMove(draggedIndex, draggedIndex + 1)
                    draggedIndex++
                    dragOffset -= slotDistance
                    lastSwapDirection = 1
                }
            }
            dragOffset < 0 && draggedIndex > 0 && lastSwapDirection <= 0 -> {
                val prevHeight = itemHeights[draggedIndex - 1] ?: fallbackHeight
                val slotDistance = getSlotDistance(draggedIndex, draggedIndex - 1) ?: prevHeight
                if (dragOffset < -prevHeight / 2) {
                    swapHeights(draggedIndex, draggedIndex - 1)
                    onMove(draggedIndex, draggedIndex - 1)
                    draggedIndex--
                    dragOffset += slotDistance
                    lastSwapDirection = -1
                }
            }
        }
    }

    internal fun getSlotDistance(fromIndex: Int, toIndex: Int): Float? {
        val items = lazyListState?.layoutInfo?.visibleItemsInfo
        val fromItem = items?.find { it.index == fromIndex }
        val toItem = items?.find { it.index == toIndex }
        return if (fromItem != null && toItem != null) {
            kotlin.math.abs(toItem.offset - fromItem.offset).toFloat()
        } else {
            null
        }
    }

    internal fun tryEdgeSwap(listState: LazyListState) {
        val onMove = currentOnMove
        val draggedItem = listState.layoutInfo.visibleItemsInfo
            .find { it.index == draggedIndex }
        if (onMove == null || draggedItem == null || currentItemCount <= 0) return
        val viewportHeight = listState.layoutInfo.viewportSize.height
        val edgeZone = viewportHeight * EDGE_ZONE_FRACTION
        val visualCenter = draggedItem.offset + draggedItem.size / 2 + dragOffset
        val target = findEdgeSwapTarget(visualCenter, edgeZone, viewportHeight)
        val fallbackDist = itemHeights[draggedIndex] ?: 0f
        val dist = if (target != null) {
            getSlotDistance(draggedIndex, target) ?: itemHeights[target] ?: fallbackDist
        } else {
            0f
        }
        if (target != null && dist > 0f) {
            executeSwap(target, dist, onMove)
        }
    }

    private fun findEdgeSwapTarget(visualCenter: Float, edgeZone: Float, viewportHeight: Int): Int? {
        return when {
            visualCenter < edgeZone && draggedIndex > 0 -> draggedIndex - 1
            visualCenter > viewportHeight - edgeZone && draggedIndex < currentItemCount - 1 ->
                draggedIndex + 1
            else -> null
        }
    }

    private fun executeSwap(target: Int, dist: Float, onMove: (Int, Int) -> Unit) {
        swapHeights(draggedIndex, target)
        onMove(draggedIndex, target)
        if (target < draggedIndex) {
            draggedIndex--
            dragOffset += dist
        } else {
            draggedIndex++
            dragOffset -= dist
        }
    }

    private fun swapHeights(indexA: Int, indexB: Int) {
        val heightA = itemHeights[indexA]
        val heightB = itemHeights[indexB]
        if (heightA != null && heightB != null) {
            itemHeights[indexA] = heightB
            itemHeights[indexB] = heightA
        }
    }

    companion object {
        private const val SETTLE_DURATION_MS = 200
        internal const val EDGE_ZONE_FRACTION = 0.2f
        internal const val AUTO_SWAP_INTERVAL_MS = 300L
    }
}

@Composable
fun rememberReorderState(lazyListState: LazyListState? = null): ReorderState {
    val state = remember { ReorderState() }
    state.lazyListState = lazyListState

    if (lazyListState != null) {
        AutoSwapEffect(state, lazyListState)
    }

    return state
}

@Composable
private fun AutoSwapEffect(state: ReorderState, lazyListState: LazyListState) {
    LaunchedEffect(Unit) {
        snapshotFlow { state.isDragging }.collect { dragging ->
            if (!dragging) return@collect
            while (state.isDragging && !state.isAnimating) {
                state.tryEdgeSwap(lazyListState)
                delay(ReorderState.AUTO_SWAP_INTERVAL_MS)
            }
        }
    }
}

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
    val scope = rememberCoroutineScope()

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
                    onDragEnd = { scope.launch { state.endDrag() } },
                    onDragCancel = { scope.launch { state.endDrag() } },
                )
            },
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
