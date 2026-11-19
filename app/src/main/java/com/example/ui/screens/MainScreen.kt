package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import com.example.R
import com.example.model.Album
import com.example.model.AppSettings
import com.example.model.Artist
import com.example.model.CollectionType
import com.example.model.FolderItem
import com.example.model.GenreItem
import com.example.model.GenreSortField
import com.example.model.MainTab
import com.example.model.PlaybackUiState
import com.example.model.Playlist
import com.example.model.SleepTimerState
import com.example.model.Song
import com.example.model.SortDirection
import com.example.model.SortField
import com.example.model.TabLongPressAction
import com.example.model.TabTapAction
import com.example.model.ThemeMode
import com.example.model.TrackSwipeAction
import com.example.model.VideoItem
import com.example.ui.components.LibraryTabsCustomizationSheet
import com.example.ui.components.MikaGlassSurface
import com.example.ui.components.MikaVerticalFastScroller
import com.example.ui.components.SongRow
import com.example.ui.components.TrackArtwork
import com.example.ui.icons.MikaIcons
import com.example.ui.theme.BrandDisplayFontFamily
import com.example.ui.theme.LocalMikaColors
import com.example.ui.theme.mikaScaledIcon
import com.example.ui.viewmodel.FolderBrowserState
import com.example.util.Formatters
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
fun MikaNavigationDrawerContent(
    tabs: List<MainTab>,
    selectedTab: MainTab,
    settings: AppSettings,
    allSongsCount: Int,
    playlistsCount: Int,
    foldersCount: Int,
    albumsCount: Int,
    artistsCount: Int,
    genresCount: Int,
    favoritesCount: Int,
    lyricsCount: Int,
    mostPlayedCount: Int,
    recentlyPlayedCount: Int,
    recentlyAddedCount: Int,
    trashedCount: Int,
    sleepTimerState: SleepTimerState,
    onSelectTab: (MainTab) -> Unit,
    onOpenHome: () -> Unit = {},
    onOpenFolders: () -> Unit = {},
    onCloseDrawer: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenVideos: () -> Unit = {},
    onOpenGenres: () -> Unit,
    onOpenCollection: (CollectionType) -> Unit,
    onOpenStatistics: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    hiddenCount: Int = 0,
    onOpenHidden: () -> Unit = {},
    onOpenPlayerStyleStudio: () -> Unit = {}
) {
    val accent = MaterialTheme.colorScheme.primary
    val mikaColors = LocalMikaColors.current
    val drawerScrollState = rememberScrollState()
    LaunchedEffect(Unit) {
        drawerScrollState.scrollTo(0)
    }

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .width(248.dp)
            .testTag("main_navigation_drawer")
    ) {
        Column(modifier = Modifier.fillMaxHeight()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(drawerScrollState)
                    .padding(horizontal = 8.dp, vertical = 10.dp)
            ) {
                Surface(
                    onClick = {
                        onCloseDrawer()
                        onOpenAbout()
                    },
                    shape = RoundedCornerShape(18.dp),
                    color = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("drawer_about_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        accent.copy(alpha = 0.22f),
                                        mikaColors.accentSecondary.copy(alpha = 0.14f)
                                    )
                                )
                            )
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF0B1431),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.mika_icon_fg),
                                contentDescription = "Mika Player Icon",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(5.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Mika Player",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontFamily = BrandDisplayFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = MikaIcons.ArrowRight,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                DrawerMenuRow(
                    label = "Home",
                    icon = MikaIcons.HomeLinear,
                    selected = false,
                    badgeText = null,
                    onClick = {
                        onCloseDrawer()
                        onOpenHome()
                    },
                    testTag = "drawer_global_home_item"
                )
                DrawerMenuRow(
                    label = "Folders",
                    icon = MikaIcons.Folder,
                    selected = false,
                    badgeText = foldersCount.takeIf { it > 0 }?.toString(),
                    onClick = {
                        onCloseDrawer()
                        onOpenFolders()
                    },
                    testTag = "drawer_folders_item"
                )
                DrawerMenuRow(
                    label = "Videos",
                    icon = MikaIcons.VideoLinear,
                    selected = false,
                    badgeText = null,
                    onClick = {
                        onCloseDrawer()
                        onOpenVideos()
                    },
                    testTag = "drawer_videos_item"
                )
                Spacer(Modifier.height(6.dp))
                DrawerSectionLabel("LIBRARY")

                DrawerMenuRow(
                    label = "Songs",
                    icon = MikaIcons.MusicNote,
                    selected = selectedTab == MainTab.SONGS,
                    badgeText = allSongsCount.takeIf { it > 0 }?.toString(),
                    onClick = {
                        onSelectTab(MainTab.SONGS)
                        onCloseDrawer()
                    },
                    testTag = "drawer_tab_songs"
                )
                DrawerMenuRow(
                    label = "Playlists",
                    icon = MikaIcons.Playlist,
                    selected = selectedTab == MainTab.PLAYLISTS,
                    badgeText = playlistsCount.takeIf { it > 0 }?.toString(),
                    onClick = {
                        onSelectTab(MainTab.PLAYLISTS)
                        onCloseDrawer()
                    },
                    testTag = "drawer_tab_playlists"
                )
                DrawerMenuRow(
                    label = "Albums",
                    icon = MikaIcons.Disc,
                    selected = selectedTab == MainTab.ALBUMS,
                    badgeText = albumsCount.takeIf { it > 0 }?.toString(),
                    onClick = {
                        onSelectTab(MainTab.ALBUMS)
                        onCloseDrawer()
                    },
                    testTag = "drawer_tab_albums"
                )
                DrawerMenuRow(
                    label = "Artists",
                    icon = MikaIcons.User,
                    selected = selectedTab == MainTab.ARTISTS,
                    badgeText = artistsCount.takeIf { it > 0 }?.toString(),
                    onClick = {
                        onSelectTab(MainTab.ARTISTS)
                        onCloseDrawer()
                    },
                    testTag = "drawer_tab_artists"
                )
                DrawerMenuRow(
                    label = "Genres",
                    icon = MikaIcons.Category,
                    selected = selectedTab == MainTab.GENRES && MainTab.GENRES in tabs,
                    badgeText = genresCount.takeIf { it > 0 }?.toString(),
                    onClick = {
                        onCloseDrawer()
                        if (MainTab.GENRES in tabs) onSelectTab(MainTab.GENRES) else onOpenGenres()
                    },
                    testTag = "drawer_genres_item"
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                DrawerSectionLabel("COLLECTIONS")

                DrawerMenuRow(
                    label = "Favorites",
                    icon = MikaIcons.HeartBold,
                    selected = selectedTab == MainTab.FAVORITES && MainTab.FAVORITES in tabs,
                    badgeText = favoritesCount.takeIf { it > 0 }?.toString(),
                    onClick = {
                        onCloseDrawer()
                        onOpenCollection(CollectionType.FAVORITES)
                    },
                    testTag = "drawer_favorites_item"
                )
                DrawerMenuRow(
                    label = "Lyrics Tracks",
                    icon = MikaIcons.Lyrics,
                    selected = false,
                    badgeText = lyricsCount.takeIf { it > 0 }?.toString(),
                    onClick = {
                        onCloseDrawer()
                        onOpenCollection(CollectionType.LYRICS)
                    },
                    testTag = "drawer_lyrics_item"
                )
                DrawerMenuRow(
                    label = "Recently Played",
                    icon = MikaIcons.Clock,
                    selected = false,
                    badgeText = recentlyPlayedCount.takeIf { it > 0 }?.toString(),
                    onClick = {
                        onCloseDrawer()
                        onOpenCollection(CollectionType.RECENTLY_PLAYED)
                    },
                    testTag = "drawer_recent_played_item"
                )
                DrawerMenuRow(
                    label = "Most Played",
                    icon = MikaIcons.Star,
                    selected = false,
                    badgeText = mostPlayedCount.takeIf { it > 0 }?.toString(),
                    onClick = {
                        onCloseDrawer()
                        onOpenCollection(CollectionType.MOST_PLAYED)
                    },
                    testTag = "drawer_most_played_item"
                )
                DrawerMenuRow(
                    label = "Recently Added",
                    icon = MikaIcons.AudioFile,
                    selected = false,
                    badgeText = recentlyAddedCount.takeIf { it > 0 }?.toString(),
                    onClick = {
                        onCloseDrawer()
                        onOpenCollection(CollectionType.RECENTLY_ADDED)
                    },
                    testTag = "drawer_recently_added_item"
                )

                DrawerMenuRow(
                    label = "Hidden Tracks",
                    icon = MikaIcons.EyeSlash,
                    selected = false,
                    badgeText = hiddenCount.takeIf { it > 0 }?.toString(),
                    onClick = {
                        onCloseDrawer()
                        onOpenHidden()
                    },
                    testTag = "drawer_hidden_item"
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                DrawerSectionLabel("TOOLS & PREFERENCES")

                DrawerMenuRow(
                    label = "Statistics",
                    icon = MikaIcons.Chart,
                    selected = false,
                    badgeText = null,
                    onClick = {
                        onCloseDrawer()
                        onOpenStatistics()
                    },
                    testTag = "drawer_stats_item"
                )
                DrawerMenuRow(
                    label = "Equalizer",
                    icon = MikaIcons.Equalizer,
                    selected = false,
                    badgeText = null,
                    onClick = {
                        onCloseDrawer()
                        onOpenEqualizer()
                    },
                    testTag = "drawer_eq_item"
                )
                DrawerMenuRow(
                    label = "Player Styles",
                    icon = MikaIcons.StyleTune,
                    selected = false,
                    badgeText = null,
                    onClick = {
                        onCloseDrawer()
                        onOpenPlayerStyleStudio()
                    },
                    testTag = "drawer_player_style_item"
                )
                DrawerMenuRow(
                    label = "Trash Bin",
                    icon = MikaIcons.Trash,
                    selected = false,
                    badgeText = trashedCount.takeIf { it > 0 }?.toString(),
                    onClick = {
                        onCloseDrawer()
                        onOpenTrash()
                    },
                    testTag = "drawer_trash_item"
                )
                DrawerMenuRow(
                    label = "Backup & Restore",
                    icon = MikaIcons.BackupUpload,
                    selected = false,
                    badgeText = null,
                    onClick = {
                        onCloseDrawer()
                        onOpenBackup()
                    },
                    testTag = "drawer_backup_item"
                )
                DrawerMenuRow(
                    label = "Settings",
                    icon = MikaIcons.Settings,
                    selected = false,
                    badgeText = null,
                    onClick = {
                        onCloseDrawer()
                        onOpenSettings()
                    },
                    testTag = "drawer_settings_item"
                )
                DrawerMenuRow(
                    label = "About Mika Player",
                    icon = MikaIcons.InfoCircle,
                    selected = false,
                    badgeText = null,
                    onClick = {
                        onCloseDrawer()
                        onOpenAbout()
                    },
                    testTag = "drawer_about_item"
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val sleepLabel = when {
                    sleepTimerState.stopAtEndOfTrack -> "End of track"
                    sleepTimerState.isActive && sleepTimerState.remainingMs > 0L ->
                        Formatters.formatDuration(sleepTimerState.remainingMs)
                    else -> "Off"
                }
                Surface(
                    onClick = {
                        onCloseDrawer()
                        onOpenSleepTimer()
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = if (sleepTimerState.isActive) {
                        accent.copy(alpha = 0.14f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                    },
                    border = if (sleepTimerState.isActive) {
                        BorderStroke(1.dp, accent.copy(alpha = 0.45f))
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("drawer_sleep_timer_footer")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = MikaIcons.SleepTimer,
                            contentDescription = "Sleep Timer",
                            tint = if (sleepTimerState.isActive) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Sleep Timer",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = sleepLabel,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (sleepTimerState.isActive) accent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val modeOptions = listOf(
                            Triple(ThemeMode.DARK, "Dark", MikaIcons.Moon),
                            Triple(ThemeMode.LIGHT, "Light", MikaIcons.Sun),
                            Triple(ThemeMode.SYSTEM, "Auto", MikaIcons.SystemTheme)
                        )
                        modeOptions.forEach { (mode, label, icon) ->
                            val selectedMode = settings.themeMode == mode
                            Surface(
                                onClick = { onSetThemeMode(mode) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selectedMode) accent else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("drawer_theme_${mode.name.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = mode.label,
                                        tint = if (selectedMode) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (selectedMode) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainLibraryScreen(
    selectedTab: MainTab,
    visibleTabs: List<MainTab> = MainTab.entries,
    isDrawerOpen: Boolean,
    settings: AppSettings,
    hasAudioPermission: Boolean,
    isScanning: Boolean,
    allSongs: List<Song>,
    recentlyPlayed: List<Song>,
    recentlyAdded: List<Song>,
    mostPlayed: List<Song>,
    favorites: List<Song>,
    lyricsSongs: List<Song>,
    playlists: List<Playlist>,
    recentPlaylists: List<Playlist>,
    trashedCount: Int,
    albums: List<Album>,
    artists: List<Artist>,
    genres: List<GenreItem>,
    genreSortField: GenreSortField,
    genreSortDirection: SortDirection,
    allFolders: List<FolderItem>,
    folderBrowserState: FolderBrowserState,
    folderSortField: SortField,
    folderSortDirection: SortDirection,
    playbackState: PlaybackUiState,
    selectedSongIds: Set<Long>,
    onOpenDrawer: () -> Unit,
    onSelectTab: (MainTab) -> Unit,
    onRequestPermission: () -> Unit,
    onImportAudio: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSort: () -> Unit,
    onOpenPlaylistSort: () -> Unit,
    onOpenFolderListSort: () -> Unit,
    onOpenAlbumSort: () -> Unit,
    onOpenArtistSort: () -> Unit,
    onOpenGenreSort: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenStatistics: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenGenres: () -> Unit,
    onOpenGenre: (String) -> Unit,
    onOpenCollection: (CollectionType) -> Unit,
    onPlayCollection: (CollectionType) -> Unit,
    onOpenAbout: () -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onPlayFolderByPath: (String) -> Unit,
    onPlayAll: (List<Song>, Boolean) -> Unit,
    onOpenFullLyricsForSong: (Song) -> Unit,
    onToggleSongSelection: (Long) -> Unit,
    onSelectAllVisible: () -> Unit,
    onClearSelection: () -> Unit,
    onBulkAddToQueue: () -> Unit,
    onBulkAddToPlaylist: () -> Unit,
    onBulkFavorite: () -> Unit,
    onBulkEditTags: () -> Unit,
    onBulkMoveToTrash: () -> Unit,
    onSongMoreClick: (Song) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onRenamePlaylist: (Long, String) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    onOpenAlbum: (Long) -> Unit,
    onOpenArtist: (Long) -> Unit,
    onNavigateFolder: (String?) -> Unit,
    onNavigateFolderUp: () -> Boolean,
    onUpdateFolderSort: (SortField, SortDirection) -> Unit,
    onRescanLibrary: () -> Unit,
    onUpdateFolderHierarchyOptions: (Boolean, Boolean) -> Unit = { _, _ -> },
    videos: List<VideoItem> = emptyList(),
    onPlayVideo: (VideoItem) -> Unit = {},
    onOpenVideos: () -> Unit = {},
    onOpenPlayerStyleStudio: () -> Unit = {},
    onBulkHide: () -> Unit = {},
    onAddAudioFolder: (Uri) -> Unit = {},
    onAddVideoFolder: (Uri) -> Unit = {},
    onToggleTabEnabled: (MainTab) -> Unit = {},
    onMoveTab: (MainTab, Int) -> Unit = { _, _ -> },
    onSetDefaultTab: (MainTab) -> Unit = {},
    playHistorySongs: List<Song> = recentlyPlayed,
    onReorderEnabledTabs: (List<MainTab>) -> Unit = {},
    onSetTabActions: (TabTapAction?, TabLongPressAction?) -> Unit = { _, _ -> },
    onToggleFavorite: (Song) -> Unit = {},
    onExecuteSwipeAction: (Song, TrackSwipeAction) -> Unit = { _, _ -> },
    onRateAndMood: (Song) -> Unit = {}
) {
    val tabs = if (visibleTabs.isEmpty()) MainTab.entries else visibleTabs
    val resolvedDefaultTab = if (settings.defaultTab in tabs) settings.defaultTab else tabs.first()
    val coroutineScope = rememberCoroutineScope()
    var showLibraryTabCustomizer by remember { mutableStateOf(false) }

    BackHandler(enabled = !isDrawerOpen && selectedSongIds.isNotEmpty()) {
        onClearSelection()
    }
    BackHandler(enabled = !isDrawerOpen && selectedSongIds.isEmpty() && selectedTab != resolvedDefaultTab) {
        onSelectTab(resolvedDefaultTab)
    }

    val selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)
    val pagerState = rememberPagerState(
        initialPage = selectedIndex,
        pageCount = { tabs.size }
    )
    val tabRowState = rememberLazyListState()
    var programmaticTargetIndex by remember { mutableStateOf<Int?>(null) }

    var headerHeightPx by remember { mutableFloatStateOf(0f) }
    var headerOffsetPx by remember { mutableFloatStateOf(0f) }
    var headerSnapJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(selectedSongIds.isNotEmpty()) {
        if (selectedSongIds.isNotEmpty()) {
            headerSnapJob?.cancel()
            headerOffsetPx = 0f
        }
    }

    LaunchedEffect(headerHeightPx) {
        if (headerHeightPx > 0f && headerOffsetPx < -headerHeightPx) {
            headerOffsetPx = -headerHeightPx
        }
    }

    val headerNestedScrollConnection = remember(isDrawerOpen) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (selectedSongIds.isNotEmpty() || isDrawerOpen || headerHeightPx <= 0f) {
                    return Offset.Zero
                }
                val delta = available.y
                if (delta != 0f) {
                    headerSnapJob?.cancel()
                    headerOffsetPx = (headerOffsetPx + delta).coerceIn(-headerHeightPx, 0f)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (selectedSongIds.isNotEmpty() || isDrawerOpen || headerHeightPx <= 0f) {
                    return Velocity.Zero
                }
                if (headerOffsetPx < 0f && headerOffsetPx > -headerHeightPx) {
                    val target = when {
                        consumed.y < -160f -> -headerHeightPx
                        consumed.y > 160f -> 0f
                        headerOffsetPx < -headerHeightPx * 0.5f -> -headerHeightPx
                        else -> 0f
                    }
                    headerSnapJob?.cancel()
                    headerSnapJob = coroutineScope.launch {
                        animate(
                            initialValue = headerOffsetPx,
                            targetValue = target,
                            animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing)
                        ) { value, _ ->
                            headerOffsetPx = value
                        }
                    }
                }
                return Velocity.Zero
            }
        }
    }

    LaunchedEffect(selectedTab, tabs) {
        val targetIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)
        if (pagerState.currentPage != targetIndex) {
            programmaticTargetIndex = targetIndex
            try {
                if (abs(pagerState.currentPage - targetIndex) <= 1) {
                    pagerState.animateScrollToPage(
                        page = targetIndex,
                        animationSpec = tween(durationMillis = 190, easing = FastOutSlowInEasing)
                    )
                } else {
                    pagerState.scrollToPage(targetIndex)
                }
            } finally {
                programmaticTargetIndex = null
            }
        }
        tabRowState.animateScrollToItem(targetIndex)
    }

    LaunchedEffect(pagerState, tabs) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                if (programmaticTargetIndex == null) {
                    val tab = tabs.getOrNull(page)
                    if (tab != null && tab != selectedTab) {
                        onSelectTab(tab)
                    }
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .nestedScroll(headerNestedScrollConnection)
    ) {
            // Solid opaque status bar shield + collapsible header + pinned tab bar on high zIndex
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .zIndex(2f)
            ) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsTopHeight(WindowInsets.statusBars)
                        .background(MaterialTheme.colorScheme.background)
                )

                // Collapsible Top Header (collapses cleanly into solid status bar shield; expands on downward scroll)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clipToBounds()
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)
                            val measuredH = placeable.height.toFloat()
                            if (headerHeightPx != measuredH) {
                                headerHeightPx = measuredH
                            }
                            val clampedOffset = headerOffsetPx.coerceIn(-placeable.height.toFloat(), 0f)
                            val visibleHeight = (placeable.height + clampedOffset)
                                .roundToInt()
                                .coerceIn(0, placeable.height)
                            layout(placeable.width, visibleHeight) {
                                placeable.placeRelative(0, clampedOffset.roundToInt())
                            }
                        }
                ) {
                    if (selectedSongIds.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = onClearSelection, modifier = Modifier.size(48.dp)) {
                                    Icon(MikaIcons.Close, contentDescription = "Cancel selection", modifier = Modifier.size(22.dp))
                                }
                                Text(
                                    text = "${selectedSongIds.size} selected",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = onSelectAllVisible, modifier = Modifier.size(48.dp)) {
                                        Icon(MikaIcons.SelectAll, contentDescription = "Select all", modifier = Modifier.size(22.dp))
                                    }
                                    IconButton(onClick = onBulkFavorite, modifier = Modifier.size(48.dp)) {
                                        Icon(MikaIcons.HeartBold, contentDescription = "Favorite selected", modifier = Modifier.size(22.dp))
                                    }
                                    IconButton(
                                        onClick = onBulkEditTags,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("bulk_edit_tags_button")
                                    ) {
                                        Icon(MikaIcons.TagEdit, contentDescription = "Edit tags for selected", modifier = Modifier.size(22.dp))
                                    }
                                    IconButton(onClick = onBulkAddToQueue, modifier = Modifier.size(48.dp)) {
                                        Icon(MikaIcons.Queue, contentDescription = "Add selected to queue", modifier = Modifier.size(22.dp))
                                    }
                                    IconButton(onClick = onBulkAddToPlaylist, modifier = Modifier.size(48.dp)) {
                                        Icon(MikaIcons.PlaylistAdd, contentDescription = "Add selected to playlist", modifier = Modifier.size(22.dp))
                                    }
                                    IconButton(
                                        onClick = onBulkHide,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("bulk_hide_button")
                                    ) {
                                        Icon(MikaIcons.EyeSlash, contentDescription = "Hide selected", modifier = Modifier.size(22.dp))
                                    }
                                    IconButton(onClick = onBulkMoveToTrash, modifier = Modifier.size(48.dp)) {
                                        Icon(
                                            MikaIcons.Trash,
                                            contentDescription = "Move selected to trash",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
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
                                    .testTag("drawer_menu_button")
                            ) {
                                Icon(
                                    imageVector = MikaIcons.Menu,
                                    contentDescription = "Open navigation menu",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(mikaScaledIcon(24.dp))
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(onClick = onOpenAbout)
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                                    .testTag("top_about_title")
                            ) {
                                Text(
                                    text = "Mika Player",
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
                                onClick = onOpenSearch,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("top_search_button")
                            ) {
                                Icon(
                                    imageVector = MikaIcons.Search,
                                    contentDescription = "Search music",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(mikaScaledIcon(24.dp))
                                )
                            }
                            IconButton(
                                onClick = onOpenStatistics,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("top_stats_button")
                            ) {
                                Icon(
                                    imageVector = MikaIcons.Chart,
                                    contentDescription = "Library statistics",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(mikaScaledIcon(24.dp))
                                )
                            }
                            IconButton(
                                onClick = onOpenSettings,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("top_settings_button")
                            ) {
                                Icon(
                                    imageVector = MikaIcons.Settings,
                                    contentDescription = "Settings",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(mikaScaledIcon(24.dp))
                                )
                            }
                        }
                    }
                }

                // Horizontally Scrollable Rounded Navigation Tabs (always pinned visibly when header collapses)
                LazyRow(
                    state = tabRowState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("main_nav_tabs")
                ) {
                    itemsIndexed(tabs, key = { _, tab -> tab.name }) { index, tab ->
                        val isSelected = selectedTab == tab
                        val pillColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
                            label = "tab_pill_color"
                        )
                        val textColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
                            label = "tab_text_color"
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = pillColor,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .combinedClickable(
                                    onClick = {
                                        if (isSelected && settings.tabTapAction == TabTapAction.SHUFFLE_PLAY) {
                                            val listForTab = when (tab) {
                                                MainTab.FAVORITES -> favorites
                                                MainTab.MOST_PLAYED -> mostPlayed
                                                MainTab.RECENTLY_PLAYED -> recentlyPlayed
                                                MainTab.RECENTLY_ADDED -> recentlyAdded
                                                MainTab.HISTORY -> playHistorySongs
                                                MainTab.PLAYLISTS -> playlists.flatMap { it.songs }.distinctBy { it.id }.ifEmpty { allSongs }
                                                else -> allSongs
                                            }
                                            if (listForTab.isNotEmpty()) onPlayAll(listForTab, true)
                                        }
                                        onSelectTab(tab)
                                        coroutineScope.launch {
                                            programmaticTargetIndex = index
                                            try {
                                                if (abs(pagerState.currentPage - index) <= 1) {
                                                    pagerState.animateScrollToPage(
                                                        page = index,
                                                        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                                                    )
                                                } else {
                                                    pagerState.scrollToPage(index)
                                                }
                                            } finally {
                                                programmaticTargetIndex = null
                                            }
                                        }
                                    },
                                    onLongClick = {
                                        when (settings.tabLongPressAction) {
                                            TabLongPressAction.CUSTOMIZE_TABS -> showLibraryTabCustomizer = true
                                            TabLongPressAction.SORT_MENU -> {
                                                onSelectTab(tab)
                                                when (tab) {
                                                    MainTab.SONGS -> onOpenSort()
                                                    MainTab.PLAYLISTS -> onOpenPlaylistSort()
                                                    MainTab.ALBUMS -> onOpenAlbumSort()
                                                    MainTab.ARTISTS -> onOpenArtistSort()
                                                    MainTab.GENRES -> onOpenGenreSort()
                                                    else -> showLibraryTabCustomizer = true
                                                }
                                            }
                                            TabLongPressAction.SHUFFLE_PLAY -> {
                                                val listForTab = when (tab) {
                                                    MainTab.FAVORITES -> favorites
                                                    MainTab.MOST_PLAYED -> mostPlayed
                                                    MainTab.RECENTLY_PLAYED -> recentlyPlayed
                                                    MainTab.RECENTLY_ADDED -> recentlyAdded
                                                    MainTab.HISTORY -> playHistorySongs
                                                    MainTab.PLAYLISTS -> playlists.flatMap { it.songs }.distinctBy { it.id }.ifEmpty { allSongs }
                                                    else -> allSongs
                                                }
                                                if (listForTab.isNotEmpty()) onPlayAll(listForTab, true)
                                            }
                                            TabLongPressAction.FILTER_INFO -> showLibraryTabCustomizer = true
                                        }
                                    }
                                )
                                .testTag("nav_tab_${tab.name.lowercase()}")
                        ) {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = textColor,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp)
                            )
                        }
                    }
                }

                // Unified Sub-Tab Action Strip matching visual reference: [Shuffle] [Play]  ( N Items )  ( SortField ^/v )
                if (hasAudioPermission || allSongs.isNotEmpty()) {
                    val activeTracksForBar = when (selectedTab) {
                        MainTab.FAVORITES -> favorites
                        MainTab.MOST_PLAYED -> mostPlayed
                        MainTab.RECENTLY_PLAYED -> recentlyPlayed
                        MainTab.RECENTLY_ADDED -> recentlyAdded
                        MainTab.HISTORY -> playHistorySongs
                        MainTab.PLAYLISTS -> playlists.flatMap { it.songs }.distinctBy { it.id }.ifEmpty { allSongs }
                        else -> allSongs
                    }
                    UnifiedSubTabActionBar(
                        selectedTab = selectedTab,
                        settings = settings,
                        genreSortField = genreSortField,
                        genreSortDirection = genreSortDirection,
                        songsCount = allSongs.size,
                        playlistsCount = playlists.size,
                        albumsCount = albums.size,
                        artistsCount = artists.size,
                        genresCount = genres.size,
                        favoritesCount = favorites.size,
                        mostPlayedCount = mostPlayed.size,
                        recentlyPlayedCount = recentlyPlayed.size,
                        recentlyAddedCount = recentlyAdded.size,
                        historyCount = playHistorySongs.size,
                        onShuffle = {
                            if (activeTracksForBar.isNotEmpty()) onPlayAll(activeTracksForBar, true)
                        },
                        onPlay = {
                            if (activeTracksForBar.isNotEmpty()) onPlayAll(activeTracksForBar, false)
                        },
                        onOpenSort = {
                            when (selectedTab) {
                                MainTab.SONGS -> onOpenSort()
                                MainTab.PLAYLISTS -> onOpenPlaylistSort()
                                MainTab.ALBUMS -> onOpenAlbumSort()
                                MainTab.ARTISTS -> onOpenArtistSort()
                                MainTab.GENRES -> onOpenGenreSort()
                                else -> Unit
                            }
                        }
                    )
                }

                if (isScanning) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp)
                    )
                }
            }

            if (!hasAudioPermission && allSongs.isEmpty()) {
                PermissionBanner(
                    onRequestPermission = onRequestPermission,
                    onImportAudio = onImportAudio,
                    modifier = Modifier.weight(1f)
                )
            } else {
                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = 1,
                    key = { page -> tabs.getOrElse(page) { MainTab.SONGS }.name },
                    modifier = Modifier.weight(1f)
                ) { page ->
                    val tabForPage = tabs.getOrNull(page) ?: MainTab.SONGS
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                                    .absoluteValue
                                    .coerceIn(0f, 1f)
                                alpha = lerp(0.84f, 1f, 1f - pageOffset)
                            }
                    ) {
                        when (tabForPage) {
                            MainTab.SONGS -> SongsScreen(
                                songs = allSongs,
                                currentSongId = playbackState.currentSong?.id,
                                isPlaying = playbackState.isPlaying,
                                isRefreshing = isScanning,
                                selectedSongIds = selectedSongIds,
                                showLyricsBadges = settings.showLyricsBadges,
                                onSongClick = { song ->
                                    if (selectedSongIds.isNotEmpty()) {
                                        onToggleSongSelection(song.id)
                                    } else {
                                        onPlaySong(song, allSongs)
                                    }
                                },
                                onSongLongClick = { song -> onToggleSongSelection(song.id) },
                                onSongMoreClick = onSongMoreClick,
                                onImportAudio = onImportAudio,
                                onRefresh = onRescanLibrary,
                                onLyricsBadgeLongClick = onOpenFullLyricsForSong,
                                showLikeButton = settings.showLikeButtonInList,
                                swipeLeftAction = settings.swipeLeftAction,
                                swipeRightAction = settings.swipeRightAction,
                                onToggleFavorite = onToggleFavorite,
                                onExecuteSwipeAction = onExecuteSwipeAction,
                                onRateAndMood = onRateAndMood
                            )

                            MainTab.PLAYLISTS -> PlaylistsScreen(
                                playlists = playlists,
                                favoritesCount = favorites.size,
                                lyricsCount = lyricsSongs.size,
                                mostPlayedCount = mostPlayed.size,
                                recentlyPlayedCount = recentlyPlayed.size,
                                onCreatePlaylist = onCreatePlaylist,
                                onRenamePlaylist = onRenamePlaylist,
                                onDeletePlaylist = onDeletePlaylist,
                                onOpenPlaylist = onOpenPlaylist,
                                onOpenCollection = onOpenCollection,
                                onPlayCollection = onPlayCollection
                            )

                            MainTab.ALBUMS -> AlbumsScreen(
                                albums = albums,
                                onAlbumClick = onOpenAlbum
                            )

                            MainTab.ARTISTS -> ArtistsScreen(
                                artists = artists,
                                onArtistClick = onOpenArtist
                            )

                            MainTab.GENRES -> EmbeddedGenresTab(
                                genres = genres,
                                onOpenGenre = onOpenGenre
                            )

                            MainTab.FAVORITES -> EmbeddedCollectionTab(
                                title = "Favorites",
                                emptyHint = "Tap the heart button on any track to collect your favorites.",
                                emptyIcon = MikaIcons.HeartLinear,
                                songs = favorites,
                                currentSongId = playbackState.currentSong?.id,
                                isPlaying = playbackState.isPlaying,
                                selectedSongIds = selectedSongIds,
                                showLyricsBadges = settings.showLyricsBadges,
                                showLikeButton = settings.showLikeButtonInList,
                                swipeLeftAction = settings.swipeLeftAction,
                                swipeRightAction = settings.swipeRightAction,
                                onSongClick = { song ->
                                    if (selectedSongIds.isNotEmpty()) onToggleSongSelection(song.id)
                                    else onPlaySong(song, favorites)
                                },
                                onSongLongClick = { onToggleSongSelection(it.id) },
                                onSongMoreClick = onSongMoreClick,
                                onLyricsBadgeLongClick = onOpenFullLyricsForSong,
                                onToggleFavorite = onToggleFavorite,
                                onExecuteSwipeAction = onExecuteSwipeAction,
                                onRateAndMood = onRateAndMood
                            )

                            MainTab.MOST_PLAYED -> EmbeddedCollectionTab(
                                title = "Most Played",
                                emptyHint = "Play tracks to build your Most Played chart.",
                                emptyIcon = MikaIcons.Star,
                                songs = mostPlayed,
                                currentSongId = playbackState.currentSong?.id,
                                isPlaying = playbackState.isPlaying,
                                selectedSongIds = selectedSongIds,
                                showLyricsBadges = settings.showLyricsBadges,
                                showLikeButton = settings.showLikeButtonInList,
                                swipeLeftAction = settings.swipeLeftAction,
                                swipeRightAction = settings.swipeRightAction,
                                onSongClick = { song ->
                                    if (selectedSongIds.isNotEmpty()) onToggleSongSelection(song.id)
                                    else onPlaySong(song, mostPlayed)
                                },
                                onSongLongClick = { onToggleSongSelection(it.id) },
                                onSongMoreClick = onSongMoreClick,
                                onLyricsBadgeLongClick = onOpenFullLyricsForSong,
                                onToggleFavorite = onToggleFavorite,
                                onExecuteSwipeAction = onExecuteSwipeAction,
                                onRateAndMood = onRateAndMood
                            )

                            MainTab.RECENTLY_PLAYED -> EmbeddedCollectionTab(
                                title = "Recently Played",
                                emptyHint = "Tracks you play appear here in chronological order.",
                                emptyIcon = MikaIcons.Clock,
                                songs = recentlyPlayed,
                                currentSongId = playbackState.currentSong?.id,
                                isPlaying = playbackState.isPlaying,
                                selectedSongIds = selectedSongIds,
                                showLyricsBadges = settings.showLyricsBadges,
                                showLikeButton = settings.showLikeButtonInList,
                                swipeLeftAction = settings.swipeLeftAction,
                                swipeRightAction = settings.swipeRightAction,
                                onSongClick = { song ->
                                    if (selectedSongIds.isNotEmpty()) onToggleSongSelection(song.id)
                                    else onPlaySong(song, recentlyPlayed)
                                },
                                onSongLongClick = { onToggleSongSelection(it.id) },
                                onSongMoreClick = onSongMoreClick,
                                onLyricsBadgeLongClick = onOpenFullLyricsForSong,
                                onToggleFavorite = onToggleFavorite,
                                onExecuteSwipeAction = onExecuteSwipeAction,
                                onRateAndMood = onRateAndMood
                            )

                            MainTab.RECENTLY_ADDED -> EmbeddedCollectionTab(
                                title = "Recently Added",
                                emptyHint = "Newly scanned or imported audio tracks appear here.",
                                emptyIcon = MikaIcons.AudioFile,
                                songs = recentlyAdded,
                                currentSongId = playbackState.currentSong?.id,
                                isPlaying = playbackState.isPlaying,
                                selectedSongIds = selectedSongIds,
                                showLyricsBadges = settings.showLyricsBadges,
                                showLikeButton = settings.showLikeButtonInList,
                                swipeLeftAction = settings.swipeLeftAction,
                                swipeRightAction = settings.swipeRightAction,
                                onSongClick = { song ->
                                    if (selectedSongIds.isNotEmpty()) onToggleSongSelection(song.id)
                                    else onPlaySong(song, recentlyAdded)
                                },
                                onSongLongClick = { onToggleSongSelection(it.id) },
                                onSongMoreClick = onSongMoreClick,
                                onLyricsBadgeLongClick = onOpenFullLyricsForSong,
                                onToggleFavorite = onToggleFavorite,
                                onExecuteSwipeAction = onExecuteSwipeAction,
                                onRateAndMood = onRateAndMood
                            )

                            MainTab.HISTORY -> EmbeddedCollectionTab(
                                title = "History",
                                emptyHint = "Your listening log builds automatically as you play tracks.",
                                emptyIcon = MikaIcons.Changelog,
                                songs = playHistorySongs,
                                currentSongId = playbackState.currentSong?.id,
                                isPlaying = playbackState.isPlaying,
                                selectedSongIds = selectedSongIds,
                                showLyricsBadges = settings.showLyricsBadges,
                                showLikeButton = settings.showLikeButtonInList,
                                swipeLeftAction = settings.swipeLeftAction,
                                swipeRightAction = settings.swipeRightAction,
                                onSongClick = { song ->
                                    if (selectedSongIds.isNotEmpty()) onToggleSongSelection(song.id)
                                    else onPlaySong(song, playHistorySongs)
                                },
                                onSongLongClick = { onToggleSongSelection(it.id) },
                                onSongMoreClick = onSongMoreClick,
                                onLyricsBadgeLongClick = onOpenFullLyricsForSong,
                                onToggleFavorite = onToggleFavorite,
                                onExecuteSwipeAction = onExecuteSwipeAction,
                                onRateAndMood = onRateAndMood
                            )
                        }
                    }
                }
            }
        }

        if (showLibraryTabCustomizer) {
            LibraryTabsCustomizationSheet(
                settings = settings,
                onToggleTabEnabled = onToggleTabEnabled,
                onMoveTab = onMoveTab,
                onSetDefaultTab = onSetDefaultTab,
                onDismiss = { showLibraryTabCustomizer = false },
                onReorderEnabledTabs = onReorderEnabledTabs,
                onSetTabActions = onSetTabActions
            )
        }
}

private fun tabIcon(tab: MainTab): ImageVector = when (tab) {
    MainTab.SONGS -> MikaIcons.MusicNote
    MainTab.PLAYLISTS -> MikaIcons.Playlist
    MainTab.ALBUMS -> MikaIcons.Disc
    MainTab.ARTISTS -> MikaIcons.User
    MainTab.GENRES -> MikaIcons.Category
    MainTab.FAVORITES -> MikaIcons.HeartBold
    MainTab.MOST_PLAYED -> MikaIcons.Star
    MainTab.RECENTLY_PLAYED -> MikaIcons.Clock
    MainTab.RECENTLY_ADDED -> MikaIcons.AudioFile
    MainTab.HISTORY -> MikaIcons.Changelog
}

private fun tabBadge(
    tab: MainTab,
    songsCount: Int,
    playlistsCount: Int,
    foldersCount: Int,
    albumsCount: Int,
    artistsCount: Int,
    genresCount: Int,
    favoritesCount: Int,
    mostPlayedCount: Int,
    recentCount: Int,
    addedCount: Int,
    historyCount: Int = recentCount
): String? = when (tab) {
    MainTab.SONGS -> songsCount.takeIf { it > 0 }?.toString()
    MainTab.PLAYLISTS -> playlistsCount.takeIf { it > 0 }?.toString()
    MainTab.ALBUMS -> albumsCount.takeIf { it > 0 }?.toString()
    MainTab.ARTISTS -> artistsCount.takeIf { it > 0 }?.toString()
    MainTab.GENRES -> genresCount.takeIf { it > 0 }?.toString()
    MainTab.FAVORITES -> favoritesCount.takeIf { it > 0 }?.toString()
    MainTab.MOST_PLAYED -> mostPlayedCount.takeIf { it > 0 }?.toString()
    MainTab.RECENTLY_PLAYED -> recentCount.takeIf { it > 0 }?.toString()
    MainTab.RECENTLY_ADDED -> addedCount.takeIf { it > 0 }?.toString()
    MainTab.HISTORY -> historyCount.takeIf { it > 0 }?.toString()
}

@Composable
private fun UnifiedSubTabActionBar(
    selectedTab: MainTab,
    settings: AppSettings,
    genreSortField: GenreSortField,
    genreSortDirection: SortDirection,
    songsCount: Int,
    playlistsCount: Int,
    albumsCount: Int,
    artistsCount: Int,
    genresCount: Int,
    favoritesCount: Int,
    mostPlayedCount: Int,
    recentlyPlayedCount: Int,
    recentlyAddedCount: Int,
    historyCount: Int = recentlyPlayedCount,
    onShuffle: () -> Unit,
    onPlay: () -> Unit,
    onOpenSort: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val countLabel = when (selectedTab) {
        MainTab.SONGS -> "$songsCount Tracks"
        MainTab.PLAYLISTS -> "$playlistsCount Playlists"
        MainTab.ALBUMS -> "$albumsCount Albums"
        MainTab.ARTISTS -> "$artistsCount Artists"
        MainTab.GENRES -> "$genresCount Genres"
        MainTab.FAVORITES -> "$favoritesCount Tracks"
        MainTab.MOST_PLAYED -> "$mostPlayedCount Tracks"
        MainTab.RECENTLY_PLAYED -> "$recentlyPlayedCount Tracks"
        MainTab.RECENTLY_ADDED -> "$recentlyAddedCount Tracks"
        MainTab.HISTORY -> "$historyCount Plays"
    }

    val sortMeta: Pair<String, SortDirection>? = when (selectedTab) {
        MainTab.SONGS -> settings.sortField.label to settings.sortDirection
        MainTab.PLAYLISTS -> settings.playlistSortField.label to settings.playlistSortDirection
        MainTab.ALBUMS -> settings.albumSortField.label to settings.albumSortDirection
        MainTab.ARTISTS -> settings.artistSortField.label to settings.artistSortDirection
        MainTab.GENRES -> genreSortField.label to genreSortDirection
        else -> null
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 3.dp)
                .testTag("unified_sub_tab_bar"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(
                onClick = onShuffle,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("unified_shuffle_button")
            ) {
                Icon(
                    imageVector = MikaIcons.Shuffle,
                    contentDescription = "Shuffle",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(mikaScaledIcon(20.dp))
                )
            }

            IconButton(
                onClick = onPlay,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("unified_play_button")
            ) {
                Icon(
                    imageVector = MikaIcons.PlayLinear,
                    contentDescription = "Play all",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(mikaScaledIcon(20.dp))
                )
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                modifier = Modifier
                    .weight(1f, fill = false)
                    .testTag("unified_count_pill")
            ) {
                Text(
                    text = countLabel,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }

            Spacer(Modifier.weight(1f))

            if (sortMeta != null) {
                val (sortLabel, sortDir) = sortMeta
                Surface(
                    onClick = onOpenSort,
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f)),
                    modifier = Modifier.testTag("unified_sort_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = sortLabel,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = if (sortDir == SortDirection.ASCENDING) {
                                MikaIcons.ArrowUp
                            } else {
                                MikaIcons.ArrowDown
                            },
                            contentDescription = if (sortDir == SortDirection.ASCENDING) "Ascending" else "Descending",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(mikaScaledIcon(15.dp))
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            accent.copy(alpha = 0.22f),
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

@Composable
private fun EmbeddedGenresTab(
    genres: List<GenreItem>,
    onOpenGenre: (String) -> Unit
) {
    if (genres.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = MikaIcons.Category,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(60.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text("No genres indexed yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    } else {
        val listState = rememberLazyListState()
        MikaVerticalFastScroller(
            listState = listState,
            totalItems = genres.size,
            sectionLabelProvider = { idx -> genres.getOrNull(idx)?.name?.trim()?.firstOrNull()?.uppercase() },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("tab_genres_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 172.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(genres, key = { it.name }) { genre ->
                    Surface(
                        onClick = { onOpenGenre(genre.name) },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TrackArtwork(
                                song = genre.representativeSong,
                                cornerRadius = 14.dp,
                                bitmapSizePx = 140,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = genre.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${genre.songCount} tracks · ${Formatters.formatDuration(genre.totalDurationMs)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = MikaIcons.ArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmbeddedCollectionTab(
    title: String,
    emptyHint: String,
    emptyIcon: ImageVector,
    songs: List<Song>,
    currentSongId: Long?,
    isPlaying: Boolean,
    selectedSongIds: Set<Long>,
    showLyricsBadges: Boolean,
    onSongClick: (Song) -> Unit,
    onSongLongClick: (Song) -> Unit,
    onSongMoreClick: (Song) -> Unit,
    onLyricsBadgeLongClick: (Song) -> Unit,
    showLikeButton: Boolean = false,
    swipeLeftAction: TrackSwipeAction = TrackSwipeAction.NONE,
    swipeRightAction: TrackSwipeAction = TrackSwipeAction.NONE,
    onToggleFavorite: (Song) -> Unit = {},
    onExecuteSwipeAction: (Song, TrackSwipeAction) -> Unit = { _, _ -> },
    onRateAndMood: (Song) -> Unit = {}
) {
    val isSelectionMode = selectedSongIds.isNotEmpty()
    if (songs.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = emptyIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(60.dp)
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "No $title Yet",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = emptyHint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        val listState = rememberLazyListState()
        MikaVerticalFastScroller(
            listState = listState,
            totalItems = songs.size,
            sectionLabelProvider = { idx -> songs.getOrNull(idx)?.title?.trim()?.firstOrNull()?.uppercase() },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("tab_${title.lowercase().replace(' ', '_')}_list"),
                contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 172.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(songs, key = { it.id }, contentType = { "song_row" }) { song ->
                    SongRow(
                        song = song,
                        isCurrentTrack = song.id == currentSongId,
                        isPlaying = isPlaying,
                        isSelectionMode = isSelectionMode,
                        isSelected = song.id in selectedSongIds,
                        showLyricsBadge = showLyricsBadges,
                        onClick = { onSongClick(song) },
                        onLongClick = { onSongLongClick(song) },
                        onMoreClick = { onSongMoreClick(song) },
                        onLyricsBadgeLongClick = { onLyricsBadgeLongClick(song) },
                        showLikeButton = showLikeButton,
                        swipeLeftAction = swipeLeftAction,
                        swipeRightAction = swipeRightAction,
                        onToggleFavorite = onToggleFavorite,
                        onExecuteSwipeAction = onExecuteSwipeAction,
                        onRateAndMood = onRateAndMood
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 1.0.sp
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
    )
}

@Composable
private fun DrawerMenuRow(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    badgeText: String?,
    onClick: () -> Unit,
    testTag: String
) {
    NavigationDrawerItem(
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.5.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(19.dp)
            )
        },
        badge = badgeText?.let { count ->
            {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = count,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        },
        selected = selected,
        onClick = onClick,
        shape = RoundedCornerShape(13.dp),
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(vertical = 1.dp)
            .testTag(testTag)
    )
}

@Composable
private fun PermissionBanner(
    onRequestPermission: () -> Unit,
    onImportAudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = MikaIcons.Folder,
                contentDescription = "Audio library permission",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(38.dp)
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = "Access Your Local Music",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Allow access to play your local music offline.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(22.dp))
        Button(
            onClick = onRequestPermission,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("grant_audio_permission_button")
        ) {
            Text("Allow Audio Access")
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onImportAudio,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(MikaIcons.AudioFile, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Or Pick Audio File Directly")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenSongsScreen(
    hiddenSongs: List<Song>,
    currentSongId: Long?,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onUnhideSong: (Long) -> Unit,
    onUnhideAll: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Hidden Tracks",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = BrandDisplayFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(MikaIcons.ArrowLeft, contentDescription = "Back", modifier = Modifier.size(mikaScaledIcon(24.dp)))
                    }
                },
                actions = {
                    if (hiddenSongs.isNotEmpty()) {
                        TextButton(onClick = onUnhideAll) {
                            Icon(MikaIcons.Eye, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Unhide All", fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        if (hiddenSongs.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp)
                    .testTag("hidden_songs_screen"),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = MikaIcons.EyeSlash,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(34.dp)
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "No Hidden Tracks",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Tracks you hide from your main library appear privately here until you unhide them.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val listState = rememberLazyListState()
            MikaVerticalFastScroller(
                listState = listState,
                totalItems = hiddenSongs.size,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .testTag("hidden_songs_screen")
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 140.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(hiddenSongs, key = { it.id }) { song ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    SongRow(
                                        song = song,
                                        isCurrentTrack = song.id == currentSongId,
                                        isPlaying = isPlaying,
                                        isSelectionMode = false,
                                        isSelected = false,
                                        showLyricsBadge = false,
                                        onClick = { onPlaySong(song, hiddenSongs) },
                                        onLongClick = { onUnhideSong(song.id) },
                                        onMoreClick = { onUnhideSong(song.id) }
                                    )
                                }
                                FilledTonalButton(
                                    onClick = { onUnhideSong(song.id) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(MikaIcons.Eye, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Unhide", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
