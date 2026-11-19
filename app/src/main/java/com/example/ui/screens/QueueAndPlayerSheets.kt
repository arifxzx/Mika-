package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.model.EqPreset
import com.example.model.EqualizerState
import com.example.model.PlaybackUiState
import com.example.model.SleepTimerState
import com.example.ui.components.MikaGlassSurface
import com.example.ui.components.MikaSleekSlider
import com.example.ui.components.TrackArtwork
import com.example.ui.icons.MikaIcons
import com.example.ui.theme.LocalMikaColors
import com.example.ui.theme.mikaScaledIcon
import com.example.util.Formatters
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.Song

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    playbackState: PlaybackUiState,
    onSelectIndex: (Int) -> Unit,
    onRemoveIndex: (Int) -> Unit,
    onMoveItem: (Int, Int) -> Unit,
    onClearQueue: () -> Unit,
    onDismiss: () -> Unit,
    allSongs: List<Song> = emptyList(),
    onTogglePlayPause: () -> Unit = {},
    onToggleFavorite: (Long) -> Unit = {},
    onToggleShuffle: () -> Unit = {},
    onAddSongsToQueue: (List<Song>) -> Unit = {},
    onOpenEqualizer: () -> Unit = {}
) {
    val mikaColors = LocalMikaColors.current
    val accent = MaterialTheme.colorScheme.primary
    val secondaryAccent = mikaColors.accentSecondary
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var itemHeightPx by remember { mutableFloatStateOf(164f) }
    val latestDraggingIndex by rememberUpdatedState(draggingIndex)
    val latestQueueSize by rememberUpdatedState(playbackState.queue.size)

    var showFilter by remember { mutableStateOf(false) }
    var filterQuery by remember { mutableStateOf("") }
    val isFiltered = filterQuery.isNotBlank()
    var showMoreMenu by remember { mutableStateOf(false) }
    var showAddTracksDialog by remember { mutableStateOf(false) }

    val stableKeys = remember(playbackState.queue) {
        val counts = HashMap<Long, Int>(playbackState.queue.size)
        playbackState.queue.map { song ->
            val count = counts.getOrDefault(song.id, 0)
            counts[song.id] = count + 1
            "${song.id}_$count"
        }
    }

    val totalDurationMs = remember(playbackState.queue) {
        playbackState.queue.sumOf { it.durationMs.coerceAtLeast(0L) }
    }
    val queueCount = playbackState.queue.size
    val queuePos = if (queueCount > 0) (playbackState.currentIndex + 1).coerceIn(1, queueCount) else 0

    if (showAddTracksDialog && allSongs.isNotEmpty()) {
        val currentIds = remember(playbackState.queue) { playbackState.queue.map { it.id }.toSet() }
        val candidates = remember(allSongs, currentIds) {
            allSongs.filter { it.id !in currentIds }.take(60)
        }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showAddTracksDialog = false },
            title = { Text("Add Tracks to Queue", fontWeight = FontWeight.Bold) },
            text = {
                if (candidates.isEmpty()) {
                    Text("All library tracks are already in your queue.")
                } else {
                    LazyColumn(modifier = Modifier.height(340.dp)) {
                        items(candidates, key = { it.id }) { candidate ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        onAddSongsToQueue(listOf(candidate))
                                        showAddTracksDialog = false
                                    }
                                    .padding(vertical = 8.dp, horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TrackArtwork(song = candidate, cornerRadius = 8.dp, modifier = Modifier.size(40.dp))
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(candidate.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        Formatters.formatArtistAndAlbum(candidate.artist, candidate.album),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(MikaIcons.AddCircle, contentDescription = "Add to queue", tint = accent, modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddTracksDialog = false }) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            mikaColors.gradientStart,
                            MaterialTheme.colorScheme.background,
                            mikaColors.gradientEnd
                        )
                    )
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .testTag("queue_sheet")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Floating Now-Playing Card (matches Screenshot_2026_1115_203040)
                playbackState.currentSong?.let { currentSong ->
                    Surface(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(22.dp),
                        color = mikaColors.glassSurface,
                        border = BorderStroke(1.dp, mikaColors.glassBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TrackArtwork(
                                song = currentSong,
                                cornerRadius = 14.dp,
                                bitmapSizePx = 192,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentSong.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = currentSong.artist.ifBlank { "Unknown Artist" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                onClick = onTogglePlayPause,
                                shape = CircleShape,
                                color = accent.copy(alpha = 0.22f),
                                border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (playbackState.isPlaying) MikaIcons.PauseBold else MikaIcons.PlayBold,
                                        contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Queue Body Card Container
                Surface(
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, mikaColors.glassBorder),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Queue Header with icon actions + Collapse chevron
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 6.dp, top = 12.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                                Text(
                                    text = "Queue",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$queuePos/$queueCount • ${Formatters.formatDuration(totalDurationMs)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                IconButton(
                                    onClick = { showFilter = !showFilter; if (!showFilter) filterQuery = "" },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        MikaIcons.Filter,
                                        contentDescription = "Filter queue",
                                        tint = if (showFilter) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(mikaScaledIcon(20.dp))
                                    )
                                }
                                IconButton(onClick = onOpenEqualizer, modifier = Modifier.size(40.dp)) {
                                    Icon(
                                        MikaIcons.Equalizer,
                                        contentDescription = "Equalizer",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(mikaScaledIcon(20.dp))
                                    )
                                }
                                Box {
                                    IconButton(onClick = { showMoreMenu = true }, modifier = Modifier.size(40.dp)) {
                                        Icon(
                                            MikaIcons.MoreVert,
                                            contentDescription = "Queue options",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(mikaScaledIcon(20.dp))
                                        )
                                    }
                                    DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                                        DropdownMenuItem(
                                            leadingIcon = { Icon(MikaIcons.LocateCurrent, null, modifier = Modifier.size(18.dp)) },
                                            text = { Text("Locate current track") },
                                            onClick = {
                                                showMoreMenu = false
                                                if (playbackState.queue.isNotEmpty() && playbackState.currentIndex >= 0) {
                                                    coroutineScope.launch {
                                                        listState.animateScrollToItem(playbackState.currentIndex.coerceIn(0, playbackState.queue.lastIndex))
                                                    }
                                                }
                                            }
                                        )
                                        DropdownMenuItem(
                                            leadingIcon = { Icon(MikaIcons.BroomBold, null, modifier = Modifier.size(18.dp)) },
                                            text = { Text("Clear Queue") },
                                            onClick = {
                                                showMoreMenu = false
                                                onClearQueue()
                                                onDismiss()
                                            }
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .testTag("queue_collapse_button")
                                ) {
                                    Icon(
                                        MikaIcons.ArrowDown,
                                        contentDescription = "Close queue",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(mikaScaledIcon(22.dp))
                                    )
                                }
                            }
                        }

                        if (showFilter) {
                            OutlinedTextField(
                                value = filterQuery,
                                onValueChange = { filterQuery = it },
                                placeholder = { Text("Filter queue tracks...") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }

                        HorizontalDivider(color = mikaColors.glassBorder)

                        val filteredIndices = remember(playbackState.queue, filterQuery) {
                            val q = filterQuery.trim()
                            if (q.isEmpty()) playbackState.queue.indices.toList()
                            else playbackState.queue.indices.filter { idx ->
                                val s = playbackState.queue[idx]
                                s.title.contains(q, ignoreCase = true) ||
                                    s.artist.contains(q, ignoreCase = true) ||
                                    s.album.contains(q, ignoreCase = true)
                            }
                        }

                        if (filteredIndices.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (playbackState.queue.isEmpty()) "Queue is empty" else "No matching tracks in queue",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 112.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(
                                    items = filteredIndices,
                                    key = { idx -> stableKeys.getOrElse(idx) { idx.toString() } },
                                    contentType = { "queue_item" }
                                ) { index ->
                                    val song = playbackState.queue[index]
                                    val isCurrent = index == playbackState.currentIndex
                                    val isDragging = index == draggingIndex
                                    val latestIndex by rememberUpdatedState(index)
                                    val rowShape = RoundedCornerShape(14.dp)

                                    val bgModifier = when {
                                        isDragging -> Modifier
                                            .background(MaterialTheme.colorScheme.surfaceContainerHigh, rowShape)
                                            .border(1.5.dp, accent, rowShape)
                                        isCurrent -> Modifier
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(
                                                        accent.copy(alpha = 0.24f),
                                                        secondaryAccent.copy(alpha = 0.12f)
                                                    )
                                                ),
                                                rowShape
                                            )
                                            .border(1.2.dp, accent.copy(alpha = 0.65f), rowShape)
                                        else -> Modifier
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .zIndex(if (isDragging) 1f else 0f)
                                            .graphicsLayer {
                                                if (isDragging) {
                                                    translationY = dragOffsetY
                                                    scaleX = 1.02f
                                                    scaleY = 1.02f
                                                    shadowElevation = 14f
                                                }
                                            }
                                            .onSizeChanged { size ->
                                                if (size.height > 0) {
                                                    itemHeightPx = size.height.toFloat()
                                                }
                                            }
                                            .clip(rowShape)
                                            .then(bgModifier)
                                            .clickable { onSelectIndex(index) }
                                            .padding(start = 8.dp, end = 4.dp, top = 7.dp, bottom = 7.dp)
                                            .testTag("queue_item_${song.id}"),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TrackArtwork(
                                            song = song,
                                            cornerRadius = 10.dp,
                                            isCurrentTrack = isCurrent,
                                            isPlaying = playbackState.isPlaying,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = song.title,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (isCurrent) accent else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${song.artist.ifBlank { song.album }} ✿ ${song.playCount}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = Formatters.formatDuration(song.durationMs),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            modifier = Modifier.padding(horizontal = 6.dp)
                                        )
                                        if (!isFiltered) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .pointerInput(stableKeys.getOrElse(index) { index }, isFiltered) {
                                                        detectVerticalDragGestures(
                                                            onDragStart = {
                                                                if (!isFiltered) {
                                                                    draggingIndex = latestIndex
                                                                    dragOffsetY = 0f
                                                                }
                                                            },
                                                            onDragEnd = {
                                                                draggingIndex = null
                                                                dragOffsetY = 0f
                                                            },
                                                            onDragCancel = {
                                                                draggingIndex = null
                                                                dragOffsetY = 0f
                                                            },
                                                            onVerticalDrag = { change, dragAmount ->
                                                                if (isFiltered) return@detectVerticalDragGestures
                                                                change.consume()
                                                                val cur = latestDraggingIndex ?: return@detectVerticalDragGestures
                                                                dragOffsetY += dragAmount
                                                                val step = (itemHeightPx * 0.72f).coerceAtLeast(48f)
                                                                if (dragOffsetY > step && cur < latestQueueSize - 1) {
                                                                    val next = cur + 1
                                                                    onMoveItem(cur, next)
                                                                    draggingIndex = next
                                                                    dragOffsetY -= itemHeightPx
                                                                } else if (dragOffsetY < -step && cur > 0) {
                                                                    val prev = cur - 1
                                                                    onMoveItem(cur, prev)
                                                                    draggingIndex = prev
                                                                    dragOffsetY += itemHeightPx
                                                                }
                                                            }
                                                        )
                                                    }
                                                    .testTag("queue_drag_handle_$index")
                                            ) {
                                                Icon(
                                                    imageVector = MikaIcons.DragHandle,
                                                    contentDescription = "Drag to reorder",
                                                    tint = if (isDragging || isCurrent) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(mikaScaledIcon(18.dp))
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = { onRemoveIndex(index) },
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Icon(
                                                MikaIcons.Close,
                                                contentDescription = "Remove from queue",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(mikaScaledIcon(16.dp))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Floating Action Capsule (Broom Clear | + Add | LocateCurrent | Shuffle)
            MikaGlassSurface(
                shape = RoundedCornerShape(50),
                tintColor = accent,
                tintStrength = 0.10f,
                tonalElevation = 10.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .widthIn(max = 520.dp)
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = {
                            onClearQueue()
                            onDismiss()
                        },
                        shape = CircleShape,
                        color = mikaColors.glassSurface,
                        modifier = Modifier
                            .size(42.dp)
                            .testTag("queue_clear_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                MikaIcons.BroomBold,
                                contentDescription = "Clear queue",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(mikaScaledIcon(19.dp))
                            )
                        }
                    }

                    Surface(
                        onClick = { showAddTracksDialog = true },
                        shape = CircleShape,
                        color = mikaColors.glassSurface,
                        modifier = Modifier
                            .size(42.dp)
                            .testTag("queue_add_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                MikaIcons.AddCircle,
                                contentDescription = "Add track to queue",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(mikaScaledIcon(19.dp))
                            )
                        }
                    }

                    Surface(
                        onClick = {
                            if (playbackState.queue.isNotEmpty() && playbackState.currentIndex >= 0) {
                                coroutineScope.launch {
                                    listState.animateScrollToItem(playbackState.currentIndex.coerceIn(0, playbackState.queue.lastIndex))
                                }
                            }
                        },
                        shape = CircleShape,
                        color = mikaColors.glassSurface,
                        modifier = Modifier
                            .size(42.dp)
                            .testTag("queue_scroll_current_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                MikaIcons.LocateCurrent,
                                contentDescription = "Locate current track",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(mikaScaledIcon(19.dp))
                            )
                        }
                    }

                    Surface(
                        onClick = onToggleShuffle,
                        shape = RoundedCornerShape(50),
                        color = if (playbackState.shuffleEnabled) accent.copy(alpha = 0.24f) else mikaColors.glassSurface,
                        border = BorderStroke(1.dp, if (playbackState.shuffleEnabled) accent else mikaColors.glassBorder),
                        modifier = Modifier
                            .height(42.dp)
                            .testTag("queue_shuffle_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                MikaIcons.Shuffle,
                                contentDescription = "Shuffle",
                                tint = if (playbackState.shuffleEnabled) accent else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(mikaScaledIcon(18.dp))
                            )
                            Text(
                                text = "Shuffle",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (playbackState.shuffleEnabled) accent else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerSheet(
    state: EqualizerState,
    onToggleEnabled: (Boolean) -> Unit,
    onSelectPreset: (EqPreset) -> Unit,
    onBandChange: (Int, Int) -> Unit,
    onBassBoostChange: (Int) -> Unit,
    onVirtualizerChange: (Int) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 22.dp)
                .testTag("equalizer_sheet"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Compact Studio Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = MikaIcons.Equalizer,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(mikaScaledIcon(22.dp))
                        )
                    }
                    Column {
                        Text("Equalizer & FX", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = if (state.isSupported) "Studio DSP Active" else "Software DSP",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (state.enabled) accent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onReset, modifier = Modifier.size(42.dp)) {
                        Icon(MikaIcons.Refresh, contentDescription = "Reset equalizer", modifier = Modifier.size(mikaScaledIcon(22.dp)))
                    }
                    Switch(
                        checked = state.enabled,
                        onCheckedChange = onToggleEnabled,
                        modifier = Modifier.testTag("equalizer_enable_switch")
                    )
                }
            }

            // 9 Preset Profiles
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(EqPreset.entries) { preset ->
                    FilterChip(
                        selected = state.preset == preset,
                        onClick = { onSelectPreset(preset) },
                        label = { Text(preset.label, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            // Compact Vertical Band Faders Side-by-Side
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    state.bandLevelsMb.forEachIndexed { idx, levelMb ->
                        val freqHz = state.bandFrequenciesHz.getOrElse(idx) { 1000 }
                        VerticalEqFader(
                            frequencyLabel = Formatters.formatFrequencyHz(freqHz),
                            levelMb = levelMb,
                            minMb = state.minBandLevelMb,
                            maxMb = state.maxBandLevelMb,
                            enabled = state.enabled,
                            accentColor = accent,
                            onLevelChange = { onBandChange(idx, it) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Compact Side-by-Side Bass Boost & Virtualizer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(MikaIcons.Equalizer, null, tint = accent, modifier = Modifier.size(mikaScaledIcon(18.dp)))
                                Text("Bass", style = MaterialTheme.typography.titleSmall)
                            }
                            Text(
                                "${state.bassBoostStrength / 10}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = accent
                            )
                        }
                        CompactSlimSlider(
                            value = state.bassBoostStrength.toFloat(),
                            onValueChange = { onBassBoostChange(it.toInt()) },
                            valueRange = 0f..1000f,
                            accentColor = accent
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(MikaIcons.Disc, null, tint = accent, modifier = Modifier.size(mikaScaledIcon(18.dp)))
                                Text("3D Stage", style = MaterialTheme.typography.titleSmall)
                            }
                            Text(
                                "${state.virtualizerStrength / 10}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = accent
                            )
                        }
                        CompactSlimSlider(
                            value = state.virtualizerStrength.toFloat(),
                            onValueChange = { onVirtualizerChange(it.toInt()) },
                            valueRange = 0f..1000f,
                            accentColor = accent
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VerticalEqFader(
    frequencyLabel: String,
    levelMb: Int,
    minMb: Int,
    maxMb: Int,
    enabled: Boolean,
    accentColor: Color,
    onLevelChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val range = (maxMb - minMb).coerceAtLeast(1).toFloat()
    val fraction = ((levelMb - minMb) / range).coerceIn(0f, 1f)
    val dbText = String.format("%+.0f", levelMb / 100f)
    val trackBg = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
    val activeColor = if (enabled) accentColor else MaterialTheme.colorScheme.onSurfaceVariant

    var trackHeightPx by remember { mutableFloatStateOf(1f) }

    fun valueFromY(y: Float): Int {
        val norm = (1f - (y / trackHeightPx)).coerceIn(0f, 1f)
        return (minMb + norm * range).roundToInt().coerceIn(minMb, maxMb)
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "${dbText}dB",
            style = MaterialTheme.typography.labelSmall,
            color = if (levelMb != 0 && enabled) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
        )

        Box(
            modifier = Modifier
                .width(40.dp)
                .height(142.dp)
                .onSizeChanged { trackHeightPx = it.height.toFloat().coerceAtLeast(1f) }
                .pointerInput(minMb, maxMb) {
                    detectTapGestures { offset ->
                        onLevelChange(valueFromY(offset.y))
                    }
                }
                .pointerInput(minMb, maxMb) {
                    detectVerticalDragGestures { change, _ ->
                        change.consume()
                        onLevelChange(valueFromY(change.position.y))
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val trackW = 5.dp.toPx()
                val centerX = size.width / 2f
                val pad = 8.dp.toPx()
                val usableH = (size.height - pad * 2).coerceAtLeast(1f)

                // Background rail
                drawRoundRect(
                    color = trackBg,
                    topLeft = Offset(centerX - trackW / 2f, pad),
                    size = Size(trackW, usableH),
                    cornerRadius = CornerRadius(trackW / 2f, trackW / 2f)
                )

                // 0 dB center marker
                val zeroY = pad + usableH * 0.5f
                drawLine(
                    color = trackBg.copy(alpha = 0.45f),
                    start = Offset(centerX - 9.dp.toPx(), zeroY),
                    end = Offset(centerX + 9.dp.toPx(), zeroY),
                    strokeWidth = 1.5.dp.toPx()
                )

                // Active fill from 0 dB or bottom
                val thumbY = pad + usableH * (1f - fraction)
                val fillTop = minOf(thumbY, zeroY)
                val fillHeight = abs(zeroY - thumbY).coerceAtLeast(2.dp.toPx())
                drawRoundRect(
                    color = activeColor,
                    topLeft = Offset(centerX - trackW / 2f, fillTop),
                    size = Size(trackW, fillHeight),
                    cornerRadius = CornerRadius(trackW / 2f, trackW / 2f)
                )

                // Thumb pill
                drawCircle(
                    color = Color.White,
                    radius = 7.5.dp.toPx(),
                    center = Offset(centerX, thumbY)
                )
                drawCircle(
                    color = activeColor,
                    radius = 4.5.dp.toPx(),
                    center = Offset(centerX, thumbY)
                )
            }
        }

        Text(
            text = frequencyLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerSheet(
    sleepTimerState: SleepTimerState,
    onStartTimer: (Int, Boolean) -> Unit,
    onExtendTimer: (Int) -> Unit,
    onSetEndOfTrack: () -> Unit,
    onToggleFadeOut: (Boolean) -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit
) {
    var customMinutes by remember { mutableFloatStateOf(25f) }
    val accent = MaterialTheme.colorScheme.primary

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 24.dp)
                .testTag("sleep_timer_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = MikaIcons.SleepTimer,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(mikaScaledIcon(22.dp))
                    )
                }
                Column {
                    Text("Sleep Timer", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = if (sleepTimerState.isActive) "Playback will pause automatically" else "Auto-stop playback with gentle fade",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (sleepTimerState.isActive) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = accent.copy(alpha = 0.14f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "TIMER ACTIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accent
                                )
                                Text(
                                    text = if (sleepTimerState.stopAtEndOfTrack) {
                                        "Stops at end of current track"
                                    } else {
                                        Formatters.formatDuration(sleepTimerState.remainingMs)
                                    },
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (!sleepTimerState.stopAtEndOfTrack) {
                                    FilledTonalButton(onClick = { onExtendTimer(5) }) {
                                        Text("+5m", fontWeight = FontWeight.Bold)
                                    }
                                }
                                OutlinedButton(onClick = {
                                    onCancelTimer()
                                    onDismiss()
                                }) {
                                    Text("Cancel")
                                }
                            }
                        }

                        if (!sleepTimerState.stopAtEndOfTrack && sleepTimerState.totalConfiguredMs > 0L) {
                            val ratio = (sleepTimerState.remainingMs.toFloat() / sleepTimerState.totalConfiguredMs.toFloat())
                                .coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { ratio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(50))
                            )
                        }
                    }
                }
            }

            // Preset Durations
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf(5, 10, 15, 30, 45, 60, 90)) { mins ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            onStartTimer(mins, sleepTimerState.fadeOutEnabled)
                            onDismiss()
                        },
                        label = { Text("${mins}m", fontWeight = FontWeight.Bold) }
                    )
                }
                item {
                    FilterChip(
                        selected = sleepTimerState.stopAtEndOfTrack,
                        onClick = {
                            onSetEndOfTrack()
                            onDismiss()
                        },
                        label = { Text("End of Track", fontWeight = FontWeight.Bold) }
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Custom Duration", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${customMinutes.toInt()} min",
                            style = MaterialTheme.typography.titleSmall,
                            color = accent
                        )
                    }
                    CompactSlimSlider(
                        value = customMinutes,
                        onValueChange = { customMinutes = it },
                        valueRange = 1f..120f,
                        accentColor = accent
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(MikaIcons.Equalizer, null, tint = accent, modifier = Modifier.size(mikaScaledIcon(20.dp)))
                            Column {
                                Text("Gentle Fade-Out", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Smoothly lowers volume in final 10 seconds",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = sleepTimerState.fadeOutEnabled,
                            onCheckedChange = onToggleFadeOut
                        )
                    }
                }
            }

            Button(
                onClick = {
                    onStartTimer(customMinutes.toInt(), sleepTimerState.fadeOutEnabled)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start ${customMinutes.toInt()} min Sleep Timer", fontWeight = FontWeight.Bold)
            }
        }
    }
}

private data class SpeedPitchPreset(
    val id: String,
    val label: String,
    val speed: Float,
    val pitch: Float
)

private val speedPitchPresets = listOf(
    SpeedPitchPreset("normal", "Normal", 1.00f, 1.00f),
    SpeedPitchPreset("slowed_deep", "Slowed + Deep", 0.82f, 0.86f),
    SpeedPitchPreset("chill", "Chill", 0.90f, 0.95f),
    SpeedPitchPreset("podcast", "Podcast", 1.35f, 1.00f),
    SpeedPitchPreset("fast", "Fast", 1.50f, 1.00f),
    SpeedPitchPreset("nightcore", "Nightcore", 1.25f, 1.22f),
    SpeedPitchPreset("turbo", "2x Turbo", 2.00f, 1.00f)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedAndPitchSheet(
    currentSpeed: Float,
    currentPitch: Float,
    onApply: (speed: Float, pitch: Float) -> Unit,
    onDismiss: () -> Unit
) {
    var speed by remember { mutableFloatStateOf(currentSpeed.coerceIn(0.25f, 3.0f)) }
    var pitch by remember { mutableFloatStateOf(currentPitch.coerceIn(0.5f, 2.0f)) }
    var linkPitchToSpeed by remember { mutableStateOf(false) }
    val accent = MaterialTheme.colorScheme.primary

    LaunchedEffect(currentSpeed, currentPitch) {
        speed = currentSpeed.coerceIn(0.25f, 3.0f)
        pitch = currentPitch.coerceIn(0.5f, 2.0f)
    }

    fun round2(value: Float): Float = kotlin.math.round(value * 100f) / 100f

    fun updateValues(newSpeed: Float, newPitch: Float) {
        val safeSpeed = round2(newSpeed.coerceIn(0.25f, 3.0f))
        val safePitch = round2(newPitch.coerceIn(0.5f, 2.0f))
        speed = safeSpeed
        pitch = safePitch
        onApply(safeSpeed, safePitch)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 22.dp)
                .testTag("speed_pitch_sheet"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Compact Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = MikaIcons.Speedometer,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(mikaScaledIcon(22.dp))
                        )
                    }
                    Column {
                        Text("Speed & Pitch", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = String.format("Speed %.2fx · Pitch %.2fx", speed, pitch),
                            style = MaterialTheme.typography.bodySmall,
                            color = accent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                TextButton(
                    onClick = {
                        linkPitchToSpeed = false
                        updateValues(1.0f, 1.0f)
                    },
                    modifier = Modifier.testTag("speed_pitch_reset")
                ) {
                    Icon(MikaIcons.Refresh, contentDescription = null, modifier = Modifier.size(mikaScaledIcon(18.dp)))
                    Spacer(Modifier.width(4.dp))
                    Text("Reset", fontWeight = FontWeight.Bold)
                }
            }

            // Compact Horizontal Presets
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(speedPitchPresets, key = { it.id }) { preset ->
                    val isSelected = abs(speed - preset.speed) < 0.015f &&
                        abs(pitch - preset.pitch) < 0.015f
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            linkPitchToSpeed = false
                            updateValues(preset.speed, preset.pitch)
                        },
                        label = {
                            Text(
                                "${preset.label} · ${String.format("%.2gx", preset.speed)}",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        modifier = Modifier.testTag("speed_preset_${preset.id}")
                    )
                }
            }

            // Compact Speed Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Playback Speed", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = String.format("%.2fx", speed),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = accent
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                val nextSpeed = (speed - 0.05f).coerceIn(0.25f, 3.0f)
                                val nextPitch = if (linkPitchToSpeed) nextSpeed.coerceIn(0.5f, 2.0f) else pitch
                                updateValues(nextSpeed, nextPitch)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(MikaIcons.Remove, contentDescription = "Decrease speed", modifier = Modifier.size(mikaScaledIcon(18.dp)))
                        }

                        CompactSlimSlider(
                            value = speed,
                            onValueChange = { raw ->
                                val nextSpeed = round2(raw.coerceIn(0.25f, 3.0f))
                                val nextPitch = if (linkPitchToSpeed) nextSpeed.coerceIn(0.5f, 2.0f) else pitch
                                updateValues(nextSpeed, nextPitch)
                            },
                            valueRange = 0.25f..3.00f,
                            accentColor = accent,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("custom_speed_slider")
                        )

                        FilledTonalIconButton(
                            onClick = {
                                val nextSpeed = (speed + 0.05f).coerceIn(0.25f, 3.0f)
                                val nextPitch = if (linkPitchToSpeed) nextSpeed.coerceIn(0.5f, 2.0f) else pitch
                                updateValues(nextSpeed, nextPitch)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(MikaIcons.Add, contentDescription = "Increase speed", modifier = Modifier.size(mikaScaledIcon(18.dp)))
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf(0.50f, 0.75f, 1.00f, 1.25f, 1.50f, 2.00f, 2.50f)) { quickSpeed ->
                            FilterChip(
                                selected = abs(speed - quickSpeed) < 0.015f,
                                onClick = {
                                    val nextPitch = if (linkPitchToSpeed) quickSpeed.coerceIn(0.5f, 2.0f) else pitch
                                    updateValues(quickSpeed, nextPitch)
                                },
                                label = { Text("${String.format("%.2gx", quickSpeed)}", fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                }
            }

            // Compact Pitch Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Audio Pitch", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = String.format("%.2fx", pitch),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = accent
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                linkPitchToSpeed = false
                                updateValues(speed, pitch - 0.05f)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(MikaIcons.Remove, contentDescription = "Lower pitch", modifier = Modifier.size(mikaScaledIcon(18.dp)))
                        }

                        CompactSlimSlider(
                            value = pitch,
                            onValueChange = { raw ->
                                linkPitchToSpeed = false
                                updateValues(speed, raw)
                            },
                            valueRange = 0.50f..2.00f,
                            accentColor = accent,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("custom_pitch_slider")
                        )

                        FilledTonalIconButton(
                            onClick = {
                                linkPitchToSpeed = false
                                updateValues(speed, pitch + 0.05f)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(MikaIcons.Add, contentDescription = "Raise pitch", modifier = Modifier.size(mikaScaledIcon(18.dp)))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Link pitch to speed (tape effect)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = linkPitchToSpeed,
                            onCheckedChange = { checked ->
                                linkPitchToSpeed = checked
                                if (checked) {
                                    updateValues(speed, speed.coerceIn(0.5f, 2.0f))
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CompactSlimSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    MikaSleekSlider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        accentColor = accentColor,
        secondaryColor = LocalMikaColors.current.accentSecondary,
        trackHeight = 4.5.dp,
        thumbRadius = 7.dp,
        touchHeight = 34.dp,
        modifier = modifier
    )
}
