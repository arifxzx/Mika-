package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.media.VideoScanner
import com.example.model.FolderItem
import com.example.model.Song
import com.example.model.SortDirection
import com.example.model.SortField
import com.example.model.TrackSwipeAction
import com.example.model.VideoItem
import com.example.ui.components.MikaGlassSurface
import com.example.ui.components.MikaVerticalFastScroller
import com.example.ui.components.SongRow
import com.example.ui.components.SortBottomSheet
import com.example.ui.components.TrackArtwork
import com.example.ui.icons.MikaIcons
import com.example.ui.theme.BrandDisplayFontFamily
import com.example.ui.theme.LocalMikaColors
import com.example.ui.theme.mikaScaledIcon
import com.example.ui.viewmodel.FolderBrowserState
import com.example.util.Formatters

private enum class FolderMediaFilter(val label: String) {
    ALL("All"),
    AUDIO("Audio"),
    VIDEOS("Videos")
}

@Composable
fun FoldersScreen(
    browserState: FolderBrowserState,
    allFolders: List<FolderItem>,
    currentSongId: Long?,
    isPlaying: Boolean,
    showLyricsBadges: Boolean,
    sortField: SortField,
    sortDirection: SortDirection,
    onNavigateFolder: (String?) -> Unit,
    onNavigateUp: () -> Boolean,
    onUpdateFolderSort: (SortField, SortDirection) -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onPlayFolder: (List<Song>) -> Unit,
    onPlayFolderByPath: (String) -> Unit,
    onSongMoreClick: (Song) -> Unit,
    onLyricsBadgeLongClick: ((Song) -> Unit)? = null,
    onShuffleFolder: ((List<Song>) -> Unit)? = null,
    enableFoldersHierarchy: Boolean = true,
    skipSingleSubfolder: Boolean = true,
    onUpdateHierarchyOptions: (Boolean, Boolean) -> Unit = { _, _ -> },
    videos: List<VideoItem> = emptyList(),
    onPlayVideo: (VideoItem) -> Unit = {},
    onAddAudioFolder: (Uri) -> Unit = {},
    onAddVideoFolder: (Uri) -> Unit = {},
    onExcludeFolder: (String) -> Unit = {},
    onRefreshLibrary: () -> Unit = {},
    bottomNavVisible: Boolean = true,
    onOpenDrawer: () -> Unit = {},
    onOpenSort: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    showLikeButton: Boolean = false,
    swipeLeftAction: TrackSwipeAction = TrackSwipeAction.NONE,
    swipeRightAction: TrackSwipeAction = TrackSwipeAction.NONE,
    onToggleFavorite: (Song) -> Unit = {},
    onExecuteSwipeAction: (Song, TrackSwipeAction) -> Unit = { _, _ -> },
    onRateAndMood: (Song) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showInnerSortSheet by remember { mutableStateOf(false) }
    var showHierarchyMenu by remember { mutableStateOf(false) }
    var flatViewMode by remember { mutableStateOf(false) }
    var mediaFilter by remember { mutableStateOf(FolderMediaFilter.ALL) }
    var selectedVideoFolder by remember { mutableStateOf<String?>(null) }
    var searchExpanded by remember { mutableStateOf(false) }
    var folderQuery by remember { mutableStateOf("") }
    var pendingVideoPick by remember { mutableStateOf(false) }
    val mikaColors = LocalMikaColors.current
    val accent = MaterialTheme.colorScheme.primary
    val secondary = mikaColors.accentSecondary

    val videoFolders = remember(videos, folderQuery) {
        val q = folderQuery.trim()
        videos.groupBy { it.folderName.ifBlank { "Videos" } }
            .entries
            .filter { (name, items) ->
                q.isEmpty() || name.contains(q, ignoreCase = true) || items.any { it.title.contains(q, ignoreCase = true) }
            }
            .sortedBy { it.key.lowercase() }
    }

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            if (pendingVideoPick || mediaFilter == FolderMediaFilter.VIDEOS) {
                onAddVideoFolder(uri)
            } else {
                onAddAudioFolder(uri)
            }
        }
        pendingVideoPick = false
    }

    BackHandler(
        enabled = searchExpanded || folderQuery.isNotEmpty() || selectedVideoFolder != null || (!flatViewMode && browserState.currentPath != null)
    ) {
        when {
            searchExpanded || folderQuery.isNotEmpty() -> {
                searchExpanded = false
                folderQuery = ""
            }
            selectedVideoFolder != null -> selectedVideoFolder = null
            else -> onNavigateUp()
        }
    }

    if (showInnerSortSheet) {
        SortBottomSheet(
            currentField = sortField,
            currentDirection = sortDirection,
            onSelect = onUpdateFolderSort,
            onDismiss = { showInnerSortSheet = false }
        )
    }

    val rawFolders = if (flatViewMode && browserState.currentPath == null) {
        allFolders
    } else {
        browserState.subfolders
    }
    val displayedFolders = remember(rawFolders, folderQuery) {
        val q = folderQuery.trim()
        if (q.isEmpty()) rawFolders
        else rawFolders.filter { it.name.contains(q, ignoreCase = true) || it.path.contains(q, ignoreCase = true) }
    }
    val displayedSongs = remember(browserState.songs, folderQuery) {
        val q = folderQuery.trim()
        if (q.isEmpty()) browserState.songs
        else browserState.songs.filter {
            it.title.contains(q, ignoreCase = true) || it.artist.contains(q, ignoreCase = true)
        }
    }

    val totalAudioSize = remember(allFolders) { allFolders.sumOf { it.totalSizeBytes } }
    val totalVideoSize = remember(videos) { videos.sumOf { it.sizeBytes } }
    val listState = rememberLazyListState()

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
            .testTag("folders_page_root")
    ) {
        // Standalone Top Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onOpenDrawer,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("folders_drawer_btn")
            ) {
                Icon(
                    imageVector = MikaIcons.Menu,
                    contentDescription = "Open navigation menu",
                    modifier = Modifier.size(mikaScaledIcon(24.dp))
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp)
            ) {
                Text(
                    text = "Folders",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = BrandDisplayFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = {
                    if (searchExpanded) {
                        searchExpanded = false
                        folderQuery = ""
                    } else {
                        searchExpanded = true
                    }
                },
                modifier = Modifier
                    .size(44.dp)
                    .testTag("folders_search_toggle")
            ) {
                Icon(
                    imageVector = if (searchExpanded) MikaIcons.Close else MikaIcons.Search,
                    contentDescription = "Search folders",
                    tint = if (searchExpanded) accent else MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(mikaScaledIcon(21.dp))
                )
            }
            IconButton(
                onClick = onOpenSort,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("folders_sort_btn")
            ) {
                Icon(
                    imageVector = MikaIcons.Sort,
                    contentDescription = "Sort folders",
                    modifier = Modifier.size(mikaScaledIcon(21.dp))
                )
            }
            IconButton(
                onClick = onRefreshLibrary,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("folders_refresh_btn")
            ) {
                Icon(
                    imageVector = MikaIcons.Refresh,
                    contentDescription = "Refresh folders",
                    modifier = Modifier.size(mikaScaledIcon(21.dp))
                )
            }
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("folders_settings_btn")
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
                    Icon(
                        imageVector = MikaIcons.Search,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (folderQuery.isEmpty()) {
                            Text(
                                text = "Filter folders, songs, or videos...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        BasicTextField(
                            value = folderQuery,
                            onValueChange = { folderQuery = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(accent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("folders_search_input")
                        )
                    }
                    if (folderQuery.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .clickable { folderQuery = "" },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = MikaIcons.Close,
                                contentDescription = "Clear filter",
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }

        MikaVerticalFastScroller(
            listState = listState,
            totalItems = displayedFolders.size + displayedSongs.size + videoFolders.size + 3,
            sectionLabelProvider = { idx ->
                displayedFolders.getOrNull((idx - 2).coerceAtLeast(0))?.name?.trim()?.firstOrNull()?.uppercase()
            },
            modifier = Modifier.weight(1f)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("folders_screen"),
                contentPadding = PaddingValues(
                    start = 14.dp,
                    end = 14.dp,
                    top = 6.dp,
                    bottom = if (bottomNavVisible) 184.dp else 124.dp
                ),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                // Storage & Quick Sync Hero Strip
                item(key = "folders_sync_card") {
                    MikaGlassSurface(
                        shape = RoundedCornerShape(22.dp),
                        tintColor = accent,
                        tintStrength = 0.08f
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    onClick = {
                                        pendingVideoPick = false
                                        folderPicker.launch(null)
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    color = accent.copy(alpha = 0.14f),
                                    border = BorderStroke(0.75.dp, accent.copy(alpha = 0.28f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(MikaIcons.FolderAdd, null, tint = accent, modifier = Modifier.size(mikaScaledIcon(17.dp)))
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "Audio Folder",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = accent,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Surface(
                                    onClick = {
                                        pendingVideoPick = true
                                        folderPicker.launch(VideoScanner.suggestedVideosInitialTreeUri())
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    color = secondary.copy(alpha = 0.14f),
                                    border = BorderStroke(0.75.dp, secondary.copy(alpha = 0.28f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(MikaIcons.VideoBold, null, tint = secondary, modifier = Modifier.size(mikaScaledIcon(17.dp)))
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "Video Folder",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = secondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FolderMediaFilter.entries.forEach { filter ->
                                    FilterChip(
                                        selected = mediaFilter == filter,
                                        onClick = {
                                            mediaFilter = filter
                                            selectedVideoFolder = null
                                        },
                                        label = {
                                            Text(
                                                text = filter.label,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                        }
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChip(
                                    selected = !flatViewMode,
                                    onClick = { flatViewMode = false },
                                    label = { Text("Tree View", fontWeight = FontWeight.Bold, maxLines = 1) },
                                    modifier = Modifier.testTag("folders_mode_tree")
                                )
                                FilterChip(
                                    selected = flatViewMode,
                                    onClick = {
                                        flatViewMode = true
                                        onNavigateFolder(null)
                                    },
                                    label = { Text("All (${allFolders.size})", fontWeight = FontWeight.Bold, maxLines = 1) },
                                    modifier = Modifier.testTag("folders_mode_flat")
                                )

                                Spacer(Modifier.weight(1f))

                                Box {
                                    Surface(
                                        onClick = { showHierarchyMenu = true },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (enableFoldersHierarchy || skipSingleSubfolder) {
                                            accent.copy(alpha = 0.14f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                        },
                                        modifier = Modifier.testTag("folders_hierarchy_options_btn")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = MikaIcons.Folder,
                                                contentDescription = "Folder hierarchy options",
                                                tint = accent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Icon(
                                                imageVector = MikaIcons.ArrowDown,
                                                contentDescription = null,
                                                tint = accent,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = showHierarchyMenu,
                                        onDismissRequest = { showHierarchyMenu = false },
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier
                                            .width(276.dp)
                                            .testTag("folders_hierarchy_menu")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            FolderHierarchyOptionCard(
                                                icon = MikaIcons.Folder,
                                                title = "Enable Folders Hierarchy",
                                                checked = enableFoldersHierarchy,
                                                onClick = {
                                                    onUpdateHierarchyOptions(!enableFoldersHierarchy, skipSingleSubfolder)
                                                },
                                                testTag = "toggle_folders_hierarchy"
                                            )
                                            FolderHierarchyOptionCard(
                                                icon = MikaIcons.NextBold,
                                                title = "Skip folders with a single subfolder",
                                                checked = skipSingleSubfolder,
                                                onClick = {
                                                    onUpdateHierarchyOptions(enableFoldersHierarchy, !skipSingleSubfolder)
                                                },
                                                testTag = "toggle_skip_single_subfolder"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Breadcrumbs bar (in Tree View or inside folder)
                if (mediaFilter != FolderMediaFilter.VIDEOS && (!flatViewMode || browserState.currentPath != null)) {
                    item(key = "folders_breadcrumbs") {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(browserState.breadcrumbs.size) { index ->
                                val (label, path) = browserState.breadcrumbs[index]
                                val isLast = index == browserState.breadcrumbs.lastIndex
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        onClick = {
                                            flatViewMode = false
                                            onNavigateFolder(path)
                                        },
                                        shape = RoundedCornerShape(50),
                                        color = if (isLast) {
                                            accent.copy(alpha = 0.16f)
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        }
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isLast) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                    if (!isLast) {
                                        Icon(
                                            imageVector = MikaIcons.ArrowRight,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Active Audio Folder Summary Hero Card
                if (mediaFilter != FolderMediaFilter.VIDEOS && displayedSongs.isNotEmpty()) {
                    item(key = "folders_summary_card") {
                        val activeFolderName = browserState.currentPath?.substringAfterLast('/')?.ifBlank { "Root" } ?: "Folder"
                        val totalDuration = remember(displayedSongs) { displayedSongs.sumOf { it.durationMs } }
                        val totalSize = remember(displayedSongs) { displayedSongs.sumOf { it.sizeBytes } }
                        MikaGlassSurface(
                            shape = RoundedCornerShape(20.dp),
                            tintColor = accent,
                            tintStrength = 0.12f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("folders_summary_card")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(accent.copy(alpha = 0.18f), RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = MikaIcons.FolderBold,
                                            contentDescription = activeFolderName,
                                            tint = accent,
                                            modifier = Modifier.size(23.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = activeFolderName,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${displayedSongs.size} tracks · ${Formatters.formatDuration(totalDuration)} · ${Formatters.formatFileSize(totalSize)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    TextButton(onClick = { showInnerSortSheet = true }) {
                                        Icon(MikaIcons.Sort, null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(sortField.label, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        onClick = { onPlayFolder(displayedSongs) },
                                        shape = RoundedCornerShape(50),
                                        color = accent,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                MikaIcons.PlayBold,
                                                null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(17.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = "Play All",
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }
                                    Surface(
                                        onClick = {
                                            if (onShuffleFolder != null) {
                                                onShuffleFolder(displayedSongs)
                                            } else {
                                                onPlayFolder(displayedSongs.shuffled())
                                            }
                                        },
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(MikaIcons.Shuffle, null, tint = accent, modifier = Modifier.size(17.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = "Shuffle",
                                                color = accent,
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Audio Folder Cards & Tracks
                if (mediaFilter != FolderMediaFilter.VIDEOS && selectedVideoFolder == null) {
                    if (displayedFolders.isNotEmpty() && mediaFilter == FolderMediaFilter.ALL) {
                        item(key = "audio_folders_section_header") {
                            Text(
                                text = "Audio Folders (${displayedFolders.size})",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = accent,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    items(displayedFolders, key = { it.path }, contentType = { "audio_folder" }) { folder ->
                        MikaGlassSurface(
                            shape = RoundedCornerShape(18.dp),
                            tintColor = accent,
                            tintStrength = 0.04f,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        flatViewMode = false
                                        onNavigateFolder(folder.path)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(contentAlignment = Alignment.BottomEnd) {
                                    if (folder.representativeSong != null) {
                                        TrackArtwork(
                                            song = folder.representativeSong,
                                            cornerRadius = 12.dp,
                                            bitmapSizePx = 150,
                                            modifier = Modifier.size(52.dp)
                                        )
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.surface,
                                            modifier = Modifier.size(22.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = MikaIcons.Folder,
                                                    contentDescription = null,
                                                    tint = accent,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .background(accent.copy(alpha = 0.14f), RoundedCornerShape(12.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = MikaIcons.FolderBold,
                                                contentDescription = folder.name,
                                                tint = accent,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = folder.name,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (folder.subfolderCount > 0) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = accent.copy(alpha = 0.14f)
                                            ) {
                                                Text(
                                                    text = "${folder.subfolderCount} sub",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = accent,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    val metaParts = buildList {
                                        add("${folder.songCount} tracks")
                                        if (folder.totalDurationMs > 0L) add(Formatters.formatDuration(folder.totalDurationMs))
                                        if (folder.totalSizeBytes > 0L) add(Formatters.formatFileSize(folder.totalSizeBytes))
                                    }
                                    Text(
                                        text = metaParts.joinToString(" · "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { onPlayFolderByPath(folder.path) },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = MikaIcons.PlayBold,
                                        contentDescription = "Play ${folder.name}",
                                        tint = accent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                var showFolderMenu by remember(folder.path) { mutableStateOf(false) }
                                Box {
                                    IconButton(
                                        onClick = { showFolderMenu = true },
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(
                                            imageVector = MikaIcons.MoreVert,
                                            contentDescription = "Folder actions",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showFolderMenu,
                                        onDismissRequest = { showFolderMenu = false }
                                    ) {
                                        androidx.compose.material3.DropdownMenuItem(
                                            leadingIcon = { Icon(MikaIcons.PlayBold, null, modifier = Modifier.size(18.dp)) },
                                            text = { Text("Play folder", fontWeight = FontWeight.Bold) },
                                            onClick = {
                                                showFolderMenu = false
                                                onPlayFolderByPath(folder.path)
                                            }
                                        )
                                        androidx.compose.material3.DropdownMenuItem(
                                            leadingIcon = { Icon(MikaIcons.EyeSlash, null, modifier = Modifier.size(18.dp)) },
                                            text = { Text("Hide / Exclude folder", fontWeight = FontWeight.Bold) },
                                            onClick = {
                                                showFolderMenu = false
                                                onExcludeFolder(folder.path)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    items(displayedSongs, key = { it.id }, contentType = { "folder_song" }) { song ->
                        SongRow(
                            song = song,
                            isCurrentTrack = song.id == currentSongId,
                            isPlaying = isPlaying,
                            isSelectionMode = false,
                            isSelected = false,
                            showLyricsBadge = showLyricsBadges,
                            onClick = { onPlaySong(song, displayedSongs) },
                            onLongClick = { onSongMoreClick(song) },
                            onMoreClick = { onSongMoreClick(song) },
                            onLyricsBadgeLongClick = onLyricsBadgeLongClick?.let { cb -> { cb(song) } },
                            showLikeButton = showLikeButton,
                            swipeLeftAction = swipeLeftAction,
                            swipeRightAction = swipeRightAction,
                            onToggleFavorite = onToggleFavorite,
                            onExecuteSwipeAction = onExecuteSwipeAction,
                            onRateAndMood = onRateAndMood
                        )
                    }
                }

                // Video folders & videos
                if (mediaFilter != FolderMediaFilter.AUDIO && videoFolders.isNotEmpty()) {
                    val activeVidFolder = selectedVideoFolder
                    if (activeVidFolder == null) {
                        item(key = "video_folders_header") {
                            Text(
                                text = "Video Folders (${videoFolders.size})",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = secondary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                            )
                        }
                        items(videoFolders, key = { "vfolder_${it.key}" }, contentType = { "video_folder" }) { (folderName, list) ->
                            val totalSize = list.sumOf { it.sizeBytes }
                            val totalDur = list.sumOf { it.durationMs }
                            val previewVideo = list.firstOrNull()
                            MikaGlassSurface(
                                shape = RoundedCornerShape(18.dp),
                                tintColor = secondary,
                                tintStrength = 0.05f,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedVideoFolder = folderName }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FolderVideoThumbnailBox(
                                        video = previewVideo,
                                        accent = secondary,
                                        modifier = Modifier.size(56.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = folderName,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val meta = buildList {
                                            add("${list.size} videos")
                                            if (totalDur > 0L) add(Formatters.formatDuration(totalDur))
                                            if (totalSize > 0L) add(Formatters.formatFileSize(totalSize))
                                        }.joinToString(" · ")
                                        Text(
                                            text = meta,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(
                                        onClick = { previewVideo?.let(onPlayVideo) },
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(
                                            imageVector = MikaIcons.PlayBold,
                                            contentDescription = "Play $folderName",
                                            tint = secondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Icon(
                                        imageVector = MikaIcons.ArrowRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        val activeVideos = videoFolders.firstOrNull { it.key == activeVidFolder }?.value.orEmpty()
                        item(key = "active_video_folder_bar") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    onClick = { selectedVideoFolder = null },
                                    shape = RoundedCornerShape(50),
                                    color = secondary.copy(alpha = 0.14f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(MikaIcons.ArrowLeft, null, tint = secondary, modifier = Modifier.size(15.dp))
                                        Text("Folders", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = secondary)
                                    }
                                }
                                Text(
                                    text = "$activeVidFolder (${activeVideos.size})",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        items(activeVideos, key = { "v_${it.id}" }, contentType = { "video_item" }) { video ->
                            MikaGlassSurface(
                                shape = RoundedCornerShape(16.dp),
                                tintColor = secondary,
                                tintStrength = 0.04f,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onPlayVideo(video) }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    FolderVideoThumbnailBox(
                                        video = video,
                                        accent = secondary,
                                        modifier = Modifier
                                            .width(74.dp)
                                            .height(48.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = video.title,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = buildList {
                                                if (video.durationMs > 0L) add(Formatters.formatDuration(video.durationMs))
                                                if (video.sizeBytes > 0L) add(Formatters.formatFileSize(video.sizeBytes))
                                            }.joinToString(" · "),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                    Icon(MikaIcons.PlayBold, contentDescription = "Play video", tint = secondary, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderVideoThumbnailBox(
    video: VideoItem?,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var thumbBitmap by remember(video?.contentUri) {
        mutableStateOf(video?.let { VideoScanner.getCachedMemoryThumbnail(it.contentUri) })
    }
    LaunchedEffect(video?.contentUri) {
        if (video != null && thumbBitmap == null) {
            thumbBitmap = VideoScanner.getOrLoadThumbnail(context, video)
        }
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        val bmp = thumbBitmap
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = video?.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = MikaIcons.VideoBold,
                contentDescription = video?.title,
                tint = accent,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun FolderHierarchyOptionCard(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(
                        color = if (checked) accent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (checked) {
                    Icon(
                        imageVector = MikaIcons.Check,
                        contentDescription = "Enabled",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}
