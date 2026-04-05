@file:Suppress("MatchingDeclarationName")

package com.nikka.core.ui.component

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
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
    private var currentItemCount by mutableIntStateOf(0)
    private var currentOnMove: ((Int, Int) -> Unit)? = null

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

    internal fun adjustForScroll(scrolled: Float) {
        dragOffset += scrolled
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

    private fun getSlotDistance(fromIndex: Int, toIndex: Int): Float? {
        val items = lazyListState?.layoutInfo?.visibleItemsInfo
        val fromItem = items?.find { it.index == fromIndex }
        val toItem = items?.find { it.index == toIndex }
        return if (fromItem != null && toItem != null) {
            kotlin.math.abs(toItem.offset - fromItem.offset).toFloat()
        } else {
            null
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
        internal const val SCROLL_ZONE_FRACTION = 0.25f
        internal const val SCROLL_SPEED_PX = 8f
        internal const val SCROLL_INTERVAL_MS = 16L
        internal const val VIEWPORT_MARGIN = 40f
    }
}

@Composable
fun rememberReorderState(lazyListState: LazyListState? = null): ReorderState {
    val state = remember { ReorderState() }
    state.lazyListState = lazyListState

    if (lazyListState != null) {
        LaunchedEffect(Unit) {
            snapshotFlow { state.isDragging }.collect { dragging ->
                if (!dragging) return@collect
                while (state.isDragging) {
                    val layoutInfo = lazyListState.layoutInfo
                    val viewportHeight = layoutInfo.viewportSize.height
                    val scrollZone = viewportHeight * ReorderState.SCROLL_ZONE_FRACTION
                    val draggedItem = layoutInfo.visibleItemsInfo
                        .find { it.index == state.draggedIndex }
                    if (draggedItem != null) {
                        val itemCenter = draggedItem.offset + draggedItem.size / 2 + state.dragOffset
                        val itemTop = draggedItem.offset + state.dragOffset
                        val itemBottom = itemTop + draggedItem.size
                        val rawScroll = when {
                            itemCenter < scrollZone && lazyListState.canScrollBackward ->
                                -ReorderState.SCROLL_SPEED_PX
                            itemCenter > viewportHeight - scrollZone && lazyListState.canScrollForward ->
                                ReorderState.SCROLL_SPEED_PX
                            else -> 0f
                        }
                        // ドラッグ中のアイテムがビューポート外に出ないよう制限
                        val safeScroll = when {
                            rawScroll > 0 -> rawScroll.coerceAtMost(
                                (itemTop - ReorderState.VIEWPORT_MARGIN).coerceAtLeast(0f),
                            )
                            rawScroll < 0 -> rawScroll.coerceAtLeast(
                                -(viewportHeight - itemBottom - ReorderState.VIEWPORT_MARGIN).coerceAtLeast(0f),
                            )
                            else -> 0f
                        }
                        if (safeScroll != 0f) {
                            val scrolled = lazyListState.scrollBy(safeScroll)
                            state.adjustForScroll(scrolled)
                        }
                    }
                    delay(ReorderState.SCROLL_INTERVAL_MS)
                }
            }
        }
    }

    return state
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
