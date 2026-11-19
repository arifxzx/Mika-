package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.media.VideoScanner
import com.example.model.SortDirection
import com.example.model.VideoItem
import com.example.model.VideoSortField
import com.example.ui.components.MikaGlassSurface
import com.example.ui.components.MikaVerticalFastScroller
import com.example.ui.icons.MikaIcons
import com.example.ui.theme.BrandDisplayFontFamily
import com.example.ui.theme.LocalMikaColors
import com.example.ui.theme.mikaScaledIcon
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideosScreen(
    videos: List<VideoItem>,
    currentVideoId: String?,
    isPlaying: Boolean,
    isLoading: Boolean,
    sortField: VideoSortField,
    sortDirection: SortDirection,
    bottomNavVisible: Boolean,
    onVideosPicked: (List<Uri>) -> Unit,
    onVideoFolderPicked: (Uri) -> Unit,
    onPlayVideo: (VideoItem) -> Unit,
    onRemoveVideo: (VideoItem) -> Unit,
    onSetSort: (VideoSortField, SortDirection) -> Unit,
    onRefresh: () -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    isGridView: Boolean = false,
    onToggleGridView: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val mikaColors = LocalMikaColors.current
    val secondary = mikaColors.accentSecondary
    val listState = rememberLazyListState()
    var showSortMenu by remember { mutableStateOf(false) }
    var searchExpanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var selectedFolder by remember { mutableStateOf<String?>(null) }

    val fallbackDocPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) onVideosPicked(uris)
    }

    val videoPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) onVideosPicked(uris)
    }

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let(onVideoFolderPicked)
    }

    val launchVideoPicker: () -> Unit = {
        runCatching {
            videoPhotoPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
            )
        }.onFailure {
            fallbackDocPicker.launch(arrayOf("video/*"))
        }
    }

    val folderGroups = remember(videos) {
        videos.groupBy { it.folderName.ifBlank { "Videos" } }
            .entries
            .sortedByDescending { it.value.size }
    }

    LaunchedEffect(folderGroups) {
        if (selectedFolder != null && folderGroups.none { it.key == selectedFolder }) {
            selectedFolder = null
        }
    }

    val filteredVideos = remember(videos, query, selectedFolder) {
        val q = query.trim()
        videos.filter { video ->
            val folderOk = selectedFolder == null || video.folderName.ifBlank { "Videos" } == selectedFolder
            val queryOk = q.isEmpty() ||
                video.title.contains(q, ignoreCase = true) ||
                video.folderName.contains(q, ignoreCase = true)
            folderOk && queryOk
        }
    }

    val heroVideo = remember(videos, currentVideoId) {
        videos.firstOrNull { it.id == currentVideoId } ?: videos.firstOrNull()
    }
    val totalSizeBytes = remember(filteredVideos) { filteredVideos.sumOf { it.sizeBytes } }
    val totalDurationMs = remember(filteredVideos) { filteredVideos.sumOf { it.durationMs } }
    val hdCount = remember(filteredVideos) { filteredVideos.count { it.height >= 720 } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        mikaColors.gradientStart.copy(alpha = if (mikaColors.isDark) 0.45f else 0.35f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .statusBarsPadding()
            .testTag("videos_screen")
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onOpenDrawer,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = MikaIcons.Menu,
                    contentDescription = "Open menu",
                    modifier = Modifier.size(mikaScaledIcon(24.dp))
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp)
            ) {
                Text(
                    text = "Videos",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = BrandDisplayFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = {
                    if (searchExpanded) {
                        searchExpanded = false
                        query = ""
                    } else {
                        searchExpanded = true
                    }
                },
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = if (searchExpanded) MikaIcons.Close else MikaIcons.Search,
                    contentDescription = "Search videos",
                    tint = if (searchExpanded) accent else MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(mikaScaledIcon(21.dp))
                )
            }
            IconButton(
                onClick = { onToggleGridView(!isGridView) },
                modifier = Modifier
                    .size(44.dp)
                    .testTag("videos_view_mode_toggle")
            ) {
                Icon(
                    imageVector = if (isGridView) MikaIcons.Sort else MikaIcons.Category,
                    contentDescription = if (isGridView) "Switch to list view" else "Switch to grid view",
                    tint = accent,
                    modifier = Modifier.size(mikaScaledIcon(21.dp))
                )
            }
            IconButton(
                onClick = onRefresh,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = MikaIcons.Refresh,
                    contentDescription = "Rescan videos",
                    modifier = Modifier.size(mikaScaledIcon(21.dp))
                )
            }
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = MikaIcons.Settings,
                    contentDescription = "Settings",
                    modifier = Modifier.size(mikaScaledIcon(21.dp))
                )
            }
        }

        AnimatedVisibility(visible = searchExpanded) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(0.75.dp, accent.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
                    .height(42.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(MikaIcons.Search, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(
                                text = "Search video title or folder...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(accent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("videos_search_input")
                        )
                    }
                    if (query.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .clickable { query = "" },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(MikaIcons.Close, contentDescription = "Clear search", modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }
        }

        // Auto-Index Folder + Sort + Pick Fallback Action Bar
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                FilterChip(
                    selected = true,
                    onClick = { folderPicker.launch(VideoScanner.suggestedVideosInitialTreeUri()) },
                    leadingIcon = { Icon(MikaIcons.FolderAdd, null, modifier = Modifier.size(16.dp)) },
                    label = { Text("Auto-Index Folder", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("pick_video_folder_button")
                )
            }
            item {
                Box {
                    FilterChip(
                        selected = false,
                        onClick = { showSortMenu = true },
                        leadingIcon = { Icon(MikaIcons.Sort, null, modifier = Modifier.size(16.dp)) },
                        label = {
                            Text(
                                text = "${sortField.label} ${if (sortDirection == SortDirection.ASCENDING) "↑" else "↓"}",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        VideoSortField.entries.forEach { field ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = field.label,
                                        fontWeight = if (sortField == field) FontWeight.Bold else FontWeight.Medium,
                                        color = if (sortField == field) accent else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    val nextDir = if (sortField == field) {
                                        if (sortDirection == SortDirection.ASCENDING) SortDirection.DESCENDING else SortDirection.ASCENDING
                                    } else {
                                        if (field == VideoSortField.TITLE) SortDirection.ASCENDING else SortDirection.DESCENDING
                                    }
                                    onSetSort(field, nextDir)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }
            item {
                FilterChip(
                    selected = false,
                    onClick = launchVideoPicker,
                    leadingIcon = { Icon(MikaIcons.Add, null, modifier = Modifier.size(16.dp)) },
                    label = { Text("Import", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("pick_videos_button")
                )
            }
        }

        // Auto-grouped Video Folder Filter Pills
        if (folderGroups.size > 1) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    FilterChip(
                        selected = selectedFolder == null,
                        onClick = { selectedFolder = null },
                        label = { Text("All (${videos.size})", fontWeight = FontWeight.Bold) }
                    )
                }
                items(folderGroups, key = { it.key }) { (folderName, list) ->
                    FilterChip(
                        selected = selectedFolder == folderName,
                        onClick = {
                            selectedFolder = if (selectedFolder == folderName) null else folderName
                        },
                        label = { Text("$folderName (${list.size})", fontWeight = FontWeight.Bold) }
                    )
                }
            }
        }

        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
            )
        }

        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = onRefresh,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (videos.isEmpty() && !isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(accent.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = MikaIcons.VideoBold,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "No Videos Found Yet",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Grant access once to your Movies or Video folder and Mika will index all videos automatically — no need to pick files one by one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { folderPicker.launch(VideoScanner.suggestedVideosInitialTreeUri()) }
                        ) {
                            Icon(MikaIcons.FolderAdd, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Sync Storage Folder", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(onClick = launchVideoPicker) {
                            Icon(MikaIcons.VideoBold, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Pick Clips", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (filteredVideos.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No matching videos",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Clear your filter or rescan folders.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 14.dp,
                        end = 14.dp,
                        top = 8.dp,
                        bottom = if (bottomNavVisible) 184.dp else 124.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    gridItems(
                        items = filteredVideos,
                        key = { it.id },
                        contentType = { "video_grid_card" }
                    ) { video ->
                        val isActive = video.id == currentVideoId
                        VideoGridCard(
                            video = video,
                            isActive = isActive,
                            isPlaying = isActive && isPlaying,
                            onClick = { onPlayVideo(video) },
                            onRemove = { onRemoveVideo(video) }
                        )
                    }
                }
            } else {
                MikaVerticalFastScroller(
                    listState = listState,
                    totalItems = filteredVideos.size + 1,
                    sectionLabelProvider = { idx ->
                        filteredVideos.getOrNull((idx - 1).coerceAtLeast(0))?.title?.trim()?.firstOrNull()?.uppercase()
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 14.dp,
                            end = 14.dp,
                            top = 8.dp,
                            bottom = if (bottomNavVisible) 184.dp else 124.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (heroVideo != null && query.isEmpty() && selectedFolder == null) {
                            item(key = "video_hero_banner", contentType = "video_hero") {
                                VideoContinueHeroCard(
                                    video = heroVideo,
                                    isCurrent = heroVideo.id == currentVideoId,
                                    isPlaying = heroVideo.id == currentVideoId && isPlaying,
                                    totalVideos = videos.size,
                                    totalDurationMs = totalDurationMs,
                                    accent = accent,
                                    secondary = secondary,
                                    onPlay = { onPlayVideo(heroVideo) }
                                )
                            }
                        }

                        items(
                            items = filteredVideos,
                            key = { it.id },
                            contentType = { "video_list_card" }
                        ) { video ->
                            val isActive = video.id == currentVideoId
                            VideoListCard(
                                video = video,
                                isActive = isActive,
                                isPlaying = isActive && isPlaying,
                                onClick = { onPlayVideo(video) },
                                onRemove = { onRemoveVideo(video) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoContinueHeroCard(
    video: VideoItem,
    isCurrent: Boolean,
    isPlaying: Boolean,
    totalVideos: Int,
    totalDurationMs: Long,
    accent: Color,
    secondary: Color,
    onPlay: () -> Unit
) {
    val context = LocalContext.current
    var thumb by remember(video.contentUri) {
        mutableStateOf(VideoScanner.getCachedMemoryThumbnail(video.contentUri))
    }
    LaunchedEffect(video.contentUri) {
        if (thumb == null) {
            thumb = VideoScanner.getOrLoadThumbnail(context, video)
        }
    }

    MikaGlassSurface(
        shape = RoundedCornerShape(22.dp),
        tintColor = accent,
        tintStrength = 0.11f,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onPlay)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(114.dp)
                    .height(68.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                val bmp = thumb
                if (bmp != null) {
                    Image(
                        bitmap = bmp,
                        contentDescription = video.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(MikaIcons.VideoBold, contentDescription = null, tint = accent, modifier = Modifier.size(28.dp))
                }
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.56f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) MikaIcons.PauseBold else MikaIcons.PlayBold,
                            contentDescription = "Play highlight video",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = secondary.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = if (isCurrent) "NOW WATCHING" else "UP NEXT · $totalVideos VIDEOS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                        color = secondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildList {
                        if (video.folderName.isNotBlank()) add(video.folderName)
                        if (video.durationMs > 0L) add(Formatters.formatDuration(video.durationMs))
                        if (totalDurationMs > 0L) add("Total ${Formatters.formatDuration(totalDurationMs)}")
                    }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun VideoGridCard(
    video: VideoItem,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val accent = MaterialTheme.colorScheme.primary
    var thumbBitmap by remember(video.contentUri) {
        mutableStateOf(VideoScanner.getCachedMemoryThumbnail(video.contentUri))
    }
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(video.contentUri) {
        if (thumbBitmap == null) {
            thumbBitmap = VideoScanner.getOrLoadThumbnail(context, video)
        }
    }

    MikaGlassSurface(
        shape = RoundedCornerShape(18.dp),
        tintColor = if (isActive) accent else Color.Transparent,
        tintStrength = if (isActive) 0.13f else 0.04f,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(106.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                val bmp = thumbBitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp,
                        contentDescription = video.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(MikaIcons.VideoBold, null, tint = accent, modifier = Modifier.size(30.dp))
                }
                if (video.durationMs > 0L) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.72f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(5.dp)
                    ) {
                        Text(
                            text = Formatters.formatDuration(video.durationMs),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
                if (video.height > 0) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = accent.copy(alpha = 0.88f),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(5.dp)
                    ) {
                        Text(
                            text = if (video.height >= 2160) "4K" else "${video.height}p",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isActive) accent else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildList {
                            if (isPlaying) add("Active")
                            if (video.folderName.isNotBlank()) add(video.folderName)
                            if (video.sizeBytes > 0L) add(Formatters.formatFileSize(video.sizeBytes))
                        }.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(34.dp)) {
                        Icon(MikaIcons.MoreVert, contentDescription = "Options", modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            leadingIcon = { Icon(MikaIcons.PlayBold, null, modifier = Modifier.size(18.dp)) },
                            text = { Text("Play", fontWeight = FontWeight.Bold) },
                            onClick = { showMenu = false; onClick() }
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(MikaIcons.Close, null, modifier = Modifier.size(18.dp)) },
                            text = { Text("Remove", fontWeight = FontWeight.Bold) },
                            onClick = { showMenu = false; onRemove() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoListCard(
    video: VideoItem,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val accent = MaterialTheme.colorScheme.primary
    var thumbBitmap by remember(video.contentUri) {
        mutableStateOf(VideoScanner.getCachedMemoryThumbnail(video.contentUri))
    }
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(video.contentUri) {
        if (thumbBitmap == null) {
            thumbBitmap = VideoScanner.getOrLoadThumbnail(context, video)
        }
    }

    MikaGlassSurface(
        shape = RoundedCornerShape(18.dp),
        tintColor = if (isActive) accent else Color.Transparent,
        tintStrength = if (isActive) 0.12f else 0.04f,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(126.dp)
                    .height(74.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                val bmp = thumbBitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp,
                        contentDescription = video.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = MikaIcons.VideoBold,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(28.dp)
                    )
                }

                if (video.durationMs > 0L) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.72f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(5.dp)
                    ) {
                        Text(
                            text = Formatters.formatDuration(video.durationMs),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isActive) accent else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val resLabel = when {
                        video.height >= 2160 -> "4K"
                        video.height > 0 -> "${video.height}p"
                        else -> null
                    }
                    if (resLabel != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = accent.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = resLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = accent,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    val meta = buildList {
                        if (video.folderName.isNotBlank()) add(video.folderName)
                        if (video.sizeBytes > 0L) add(Formatters.formatFileSize(video.sizeBytes))
                    }.joinToString(" · ")
                    Text(
                        text = if (isPlaying) "Active · $meta" else meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = MikaIcons.MoreVert,
                        contentDescription = "Video options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        leadingIcon = { Icon(MikaIcons.PlayBold, null, modifier = Modifier.size(18.dp)) },
                        text = { Text("Play", fontWeight = FontWeight.Bold) },
                        onClick = {
                            showMenu = false
                            onClick()
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(MikaIcons.Close, null, modifier = Modifier.size(18.dp)) },
                        text = { Text("Remove from list", fontWeight = FontWeight.Bold) },
                        onClick = {
                            showMenu = false
                            onRemove()
                        }
                    )
                }
            }
        }
    }
}
