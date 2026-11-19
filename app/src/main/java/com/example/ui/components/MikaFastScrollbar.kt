package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
fun MikaVerticalFastScroller(
    listState: LazyListState,
    totalItems: Int,
    modifier: Modifier = Modifier,
    sectionLabelProvider: ((Int) -> String?)? = null,
    content: @Composable () -> Unit
) {
    val canScroll by remember(listState) {
        derivedStateOf { listState.canScrollForward || listState.canScrollBackward }
    }
    val isScrollInProgress by remember(listState) {
        derivedStateOf { listState.isScrollInProgress }
    }
    val maxFirstIndex by remember(listState, totalItems) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val count = layoutInfo.totalItemsCount.coerceAtLeast(totalItems)
            val visibleCount = layoutInfo.visibleItemsInfo.size
            (count - visibleCount).coerceAtLeast(1)
        }
    }
    val fraction by remember(listState, totalItems) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val count = layoutInfo.totalItemsCount.coerceAtLeast(totalItems)
            val visibleCount = layoutInfo.visibleItemsInfo.size
            val maxFirst = (count - visibleCount).coerceAtLeast(1)
            if (count <= visibleCount || count <= 1) 0f
            else {
                val subItemOffset = layoutInfo.visibleItemsInfo.firstOrNull()?.let { first ->
                    val h = first.size.coerceAtLeast(1)
                    (-first.offset).toFloat() / h.toFloat()
                } ?: 0f
                ((listState.firstVisibleItemIndex + subItemOffset) / maxFirst.toFloat()).coerceIn(0f, 1f)
            }
        }
    }
    val scope = rememberCoroutineScope()

    FastScrollerContainer(
        canScroll = canScroll && totalItems > 2,
        isScrollInProgress = isScrollInProgress,
        scrollFractionProvider = { fraction },
        totalItems = totalItems,
        sectionLabelProvider = sectionLabelProvider,
        onScrubFraction = { scrubFraction ->
            val targetIdx = (scrubFraction * maxFirstIndex).roundToInt()
                .coerceIn(0, maxFirstIndex)
            scope.launch { listState.scrollToItem(targetIdx) }
        },
        modifier = modifier,
        content = content
    )
}

@Composable
fun MikaVerticalGridFastScroller(
    gridState: LazyGridState,
    totalItems: Int,
    columns: Int,
    modifier: Modifier = Modifier,
    sectionLabelProvider: ((Int) -> String?)? = null,
    content: @Composable () -> Unit
) {
    val safeColumns = columns.coerceAtLeast(1)
    val canScroll by remember(gridState) {
        derivedStateOf { gridState.canScrollForward || gridState.canScrollBackward }
    }
    val isScrollInProgress by remember(gridState) {
        derivedStateOf { gridState.isScrollInProgress }
    }
    val maxFirstIndex by remember(gridState, totalItems, safeColumns) {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val count = layoutInfo.totalItemsCount.coerceAtLeast(totalItems)
            val visibleCount = layoutInfo.visibleItemsInfo.size
            val totalRows = ceil(count.toFloat() / safeColumns).toInt().coerceAtLeast(1)
            val visibleRows = ceil(visibleCount.toFloat() / safeColumns).toInt().coerceAtLeast(1)
            val maxFirstRow = (totalRows - visibleRows).coerceAtLeast(1)
            (maxFirstRow * safeColumns).coerceIn(0, (count - 1).coerceAtLeast(0))
        }
    }
    val fraction by remember(gridState, totalItems, safeColumns) {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val count = layoutInfo.totalItemsCount.coerceAtLeast(totalItems)
            val visibleCount = layoutInfo.visibleItemsInfo.size
            val totalRows = ceil(count.toFloat() / safeColumns).toInt().coerceAtLeast(1)
            val visibleRows = ceil(visibleCount.toFloat() / safeColumns).toInt().coerceAtLeast(1)
            val maxFirstRow = (totalRows - visibleRows).coerceAtLeast(1)
            if (count <= visibleCount || count <= safeColumns) 0f
            else {
                val firstRow = gridState.firstVisibleItemIndex / safeColumns
                val subRowOffset = layoutInfo.visibleItemsInfo.firstOrNull()?.let { first ->
                    val h = first.size.height.coerceAtLeast(1)
                    (-first.offset.y).toFloat() / h.toFloat()
                } ?: 0f
                ((firstRow + subRowOffset) / maxFirstRow.toFloat()).coerceIn(0f, 1f)
            }
        }
    }
    val scope = rememberCoroutineScope()

    FastScrollerContainer(
        canScroll = canScroll && totalItems > safeColumns,
        isScrollInProgress = isScrollInProgress,
        scrollFractionProvider = { fraction },
        totalItems = totalItems,
        sectionLabelProvider = sectionLabelProvider,
        onScrubFraction = { scrubFraction ->
            val targetIdx = (scrubFraction * maxFirstIndex).roundToInt()
                .coerceIn(0, (totalItems - 1).coerceAtLeast(0))
            scope.launch { gridState.scrollToItem(targetIdx) }
        },
        modifier = modifier,
        content = content
    )
}

@Composable
private fun FastScrollerContainer(
    canScroll: Boolean,
    isScrollInProgress: Boolean,
    scrollFractionProvider: () -> Float,
    totalItems: Int,
    sectionLabelProvider: ((Int) -> String?)?,
    onScrubFraction: (Float) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var hoveredIndex by remember { mutableIntStateOf(0) }
    var recentlyScrolled by remember { mutableStateOf(false) }

    LaunchedEffect(isScrollInProgress, isDragging) {
        if (isScrollInProgress || isDragging) {
            recentlyScrolled = true
        } else {
            delay(1100)
            recentlyScrolled = false
        }
    }

    val barAlpha by animateFloatAsState(
        targetValue = if (canScroll && (recentlyScrolled || isDragging)) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "fast_scrollbar_alpha"
    )

    val activePopupLabel = if (isDragging) {
        sectionLabelProvider?.invoke(hoveredIndex)?.takeIf { it.isNotBlank() }
    } else null

    Box(modifier = modifier.fillMaxSize()) {
        content()

        if (canScroll && totalItems > 0) {
            BoxWithConstraints(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(top = 8.dp, bottom = 8.dp, end = 2.dp)
            ) {
                val density = LocalDensity.current
                val thumbHeightDp = if (isDragging) 48.dp else 40.dp
                val thumbWidthDp = if (isDragging) 7.dp else 5.dp
                val maxHeightPx = with(density) { maxHeight.toPx() }
                val thumbHeightPx = with(density) { thumbHeightDp.toPx() }
                val trackRangePx = (maxHeightPx - thumbHeightPx).coerceAtLeast(1f)
                val badgeXOffsetPx = -with(density) { 44.dp.roundToPx() }
                val computeThumbY: () -> Int = {
                    val activeFraction = if (isDragging) dragFraction else scrollFractionProvider()
                    (activeFraction.coerceIn(0f, 1f) * trackRangePx).roundToInt()
                }

                AnimatedVisibility(
                    visible = isDragging && activePopupLabel != null,
                    enter = fadeIn(tween(120)),
                    exit = fadeOut(tween(150)),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset { IntOffset(badgeXOffsetPx, computeThumbY()) }
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 6.dp
                    ) {
                        Text(
                            text = activePopupLabel.orEmpty(),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(36.dp)
                        .graphicsLayer { alpha = if (isDragging) 1f else barAlpha }
                        .pointerInput(totalItems, trackRangePx) {
                            detectVerticalDragGestures(
                                onDragStart = { startOffset ->
                                    isDragging = true
                                    val newFraction = ((startOffset.y - thumbHeightPx / 2f) / trackRangePx)
                                        .coerceIn(0f, 1f)
                                    dragFraction = newFraction
                                    hoveredIndex = (newFraction * (totalItems - 1)).roundToInt()
                                        .coerceIn(0, (totalItems - 1).coerceAtLeast(0))
                                    onScrubFraction(newFraction)
                                },
                                onDragEnd = { isDragging = false },
                                onDragCancel = { isDragging = false },
                                onVerticalDrag = { change, _ ->
                                    change.consume()
                                    val newFraction = ((change.position.y - thumbHeightPx / 2f) / trackRangePx)
                                        .coerceIn(0f, 1f)
                                    dragFraction = newFraction
                                    hoveredIndex = (newFraction * (totalItems - 1)).roundToInt()
                                        .coerceIn(0, (totalItems - 1).coerceAtLeast(0))
                                    onScrubFraction(newFraction)
                                }
                            )
                        }
                        .testTag("fast_scrollbar_track")
                ) {
                    // Subtle rail backing while dragging
                    if (isDragging) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 3.dp)
                                .fillMaxHeight()
                                .width(3.dp)
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f))
                        )
                    }

                    // Draggable thumb pill
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 2.dp)
                            .offset { IntOffset(0, computeThumbY()) }
                            .width(thumbWidthDp)
                            .height(thumbHeightDp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (isDragging) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.78f)
                            )
                            .testTag("fast_scrollbar_thumb")
                    )
                }
            }
        }
    }
}
