package com.example

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.media.MediaScanner
import com.example.model.AlbumSortField
import com.example.model.ArtistSortField
import com.example.model.BottomNavDest
import com.example.model.CollectionType
import com.example.model.FolderSortField
import com.example.model.GenreSortField
import com.example.model.MainTab
import com.example.model.PlayerBackgroundMode
import com.example.model.PlaylistSortField
import com.example.model.Song
import com.example.model.TrackSwipeAction
import com.example.ui.components.AddToPlaylistSheet
import com.example.ui.components.BottomNavCustomizationSheet
import com.example.ui.components.FloatingVideoPipOverlay
import com.example.ui.components.MiniPlayer
import com.example.ui.components.PlayerBackgroundDialog
import com.example.ui.components.RateAndMoodDialog
import com.example.ui.components.SongActionsSheet
import com.example.ui.components.SongInfoDialog
import com.example.ui.components.SortBottomSheet
import com.example.ui.components.SoundControlSheet
import com.example.ui.components.TagEditorSheet
import com.example.ui.components.TypedSortBottomSheet
import com.example.ui.navigation.MikaRoute
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.AlbumDetailScreen
import com.example.ui.screens.ArtistDetailScreen
import com.example.ui.screens.BackupScreen
import com.example.ui.screens.CollectionScreen
import com.example.ui.screens.EqualizerSheet
import com.example.ui.screens.FoldersScreen
import com.example.ui.screens.FullPlayerScreen
import com.example.ui.screens.FullVideoPlayerScreen
import com.example.ui.screens.GenreDetailScreen
import com.example.ui.screens.GenresScreen
import com.example.ui.screens.HiddenSongsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LyricsScreen
import com.example.ui.screens.MainLibraryScreen
import com.example.ui.screens.MikaNavigationDrawerContent
import com.example.ui.screens.PlayerStyleSettingsScreen
import com.example.ui.screens.PlaylistDetailScreen
import com.example.ui.screens.QueueSheet
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SleepTimerSheet
import com.example.ui.screens.SpeedAndPitchSheet
import com.example.ui.screens.StatisticsScreen
import com.example.ui.screens.TrashScreen
import com.example.ui.screens.VideosScreen
import com.example.ui.screens.WidgetSettingsScreen
import com.example.ui.icons.MikaIcons
import com.example.ui.theme.BrandDisplayFontFamily
import com.example.ui.theme.MikaTheme
import com.example.ui.theme.mikaScaledIcon
import com.example.ui.viewmodel.MikaViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class ActiveSortSheet {
    SONGS,
    PLAYLISTS,
    FOLDERS,
    ALBUMS,
    ARTISTS,
    GENRES
}

class MainActivity : ComponentActivity() {

    private val viewModel: MikaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingMediaIntent(intent)

        setContent {
            MikaAppRoot(viewModel = viewModel)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingMediaIntent(intent)
    }

    private fun handleIncomingMediaIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            intent.data?.let { uri ->
                val mime = intent.type ?: contentResolver.getType(uri)
                if (mime?.startsWith("video/") == true) {
                    viewModel.playExternalVideoUri(uri)
                } else {
                    viewModel.importAudioUri(uri)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MikaAppRoot(viewModel: MikaViewModel) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val artworkColors by viewModel.currentArtworkColors.collectAsStateWithLifecycle()
    val brightArtworkColors by viewModel.currentBrightArtworkColors.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val equalizerState by viewModel.equalizerState.collectAsStateWithLifecycle()
    val sleepTimerState by viewModel.sleepTimerState.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val hasAudioPermission by viewModel.hasAudioPermission.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val allSongs by viewModel.sortedSongs.collectAsStateWithLifecycle()
    val rawSongs by viewModel.rawSongs.collectAsStateWithLifecycle()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val recentlyAdded by viewModel.recentlyAdded.collectAsStateWithLifecycle()
    val mostPlayed by viewModel.mostPlayed.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val lyricsSongs by viewModel.lyricsSongs.collectAsStateWithLifecycle()
    val favoriteSongIds by viewModel.favoriteSongIds.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val sortedPlaylists by viewModel.sortedPlaylists.collectAsStateWithLifecycle()
    val recentPlaylists by viewModel.recentlyPlayedPlaylists.collectAsStateWithLifecycle()
    val trashedSongs by viewModel.trashedSongs.collectAsStateWithLifecycle()
    val hiddenSongs by viewModel.hiddenSongs.collectAsStateWithLifecycle()
    val libraryStats by viewModel.libraryStats.collectAsStateWithLifecycle()
    val playHistory by viewModel.playHistory.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val genres by viewModel.genres.collectAsStateWithLifecycle()
    val genreSortField by viewModel.genreSortField.collectAsStateWithLifecycle()
    val genreSortDirection by viewModel.genreSortDirection.collectAsStateWithLifecycle()
    val allFolders by viewModel.allFolders.collectAsStateWithLifecycle()
    val folderBrowserState by viewModel.folderBrowserState.collectAsStateWithLifecycle()
    val folderSortField by viewModel.folderSortField.collectAsStateWithLifecycle()
    val folderSortDirection by viewModel.folderSortDirection.collectAsStateWithLifecycle()
    val selectedSongIds by viewModel.selectedSongIds.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()
    val currentLyrics by viewModel.currentLyrics.collectAsStateWithLifecycle()
    val cacheSizeBytes by viewModel.cacheSizeBytes.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val availableFolders by viewModel.availableDeviceFolders.collectAsStateWithLifecycle()
    val localBackups by viewModel.localBackups.collectAsStateWithLifecycle()
    val pendingBackupPreview by viewModel.pendingBackupPreview.collectAsStateWithLifecycle()
    val isBackupBusy by viewModel.isBackupBusy.collectAsStateWithLifecycle()
    val videoState by viewModel.videoState.collectAsStateWithLifecycle()
    val videoAspectRatio by viewModel.videoAspectRatio.collectAsStateWithLifecycle()
    val videos by viewModel.videos.collectAsStateWithLifecycle()
    val isLoadingVideos by viewModel.isLoadingVideos.collectAsStateWithLifecycle()
    val videoSortField by viewModel.videoSortField.collectAsStateWithLifecycle()
    val videoSortDirection by viewModel.videoSortDirection.collectAsStateWithLifecycle()
    val trackWaveform by viewModel.trackWaveform.collectAsStateWithLifecycle()
    val liveWaveformHistory by viewModel.liveWaveformHistory.collectAsStateWithLifecycle()
    val duplicateGroups by viewModel.duplicateGroups.collectAsStateWithLifecycle()
    val isScanningDuplicates by viewModel.isScanningDuplicates.collectAsStateWithLifecycle()

    val playHistorySongs = remember(playHistory, rawSongs, recentlyPlayed) {
        val songById = rawSongs.associateBy { it.id }
        playHistory.mapNotNull { songById[it.songId] }
            .distinctBy { it.id }
            .ifEmpty { recentlyPlayed }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }
    LaunchedEffect(playbackState.errorMessage) {
        playbackState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.playbackController.clearError()
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[MediaScanner.requiredAudioPermission()] == true ||
            MediaScanner.hasAudioPermission(context)
        viewModel.onPermissionChanged(granted)
    }

    val requestAudioPermissions = {
        val perms = buildList {
            add(MediaScanner.requiredAudioPermission())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
        audioPermissionLauncher.launch(perms)
    }

    LaunchedEffect(Unit) {
        if (!hasAudioPermission) {
            requestAudioPermissions()
        }
    }

    val importAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importAudioUri(it) }
    }

    val importLyricsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importLyricsUri(it) }
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let { viewModel.exportBackupToUri(it) }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.inspectBackupUri(it) }
    }

    val addFolderTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        treeUri?.let { viewModel.addIncludedFolderTreeUri(it) }
    }

    val backupLocationTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        treeUri?.let { viewModel.setDefaultBackupLocation(it) }
    }

    val pickCustomBgImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val savedUri = withContext(Dispatchers.IO) {
                    runCatching {
                        context.filesDir.listFiles()
                            ?.filter { it.name.startsWith("player_bg_custom_") }
                            ?.forEach { it.delete() }
                        val destFile = File(context.filesDir, "player_bg_custom_${System.currentTimeMillis()}.jpg")
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            destFile.outputStream().use { output -> input.copyTo(output) }
                        }
                        Uri.fromFile(destFile).toString()
                    }.getOrNull()
                }
                if (savedUri != null) {
                    viewModel.setPlayerBackground(mode = PlayerBackgroundMode.CUSTOM, customUri = savedUri)
                }
            }
        }
    }

    val pickCustomAppIconLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.importCustomAppIcon(it) }
    }

    var pendingOsDeleteSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    val permanentDeleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && pendingOsDeleteSongs.isNotEmpty()) {
            viewModel.finalizePermanentDelete(pendingOsDeleteSongs, deletedBySystemPrompt = true)
        }
        pendingOsDeleteSongs = emptyList()
    }

    val triggerPermanentDelete: (List<Song>) -> Unit = { targets ->
        if (targets.isNotEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val mediaUris = targets.filter { it.id > 0L }.map { Uri.parse(it.contentUri) }
                if (mediaUris.isNotEmpty()) {
                    pendingOsDeleteSongs = targets
                    runCatching {
                        val pi = MediaStore.createDeleteRequest(context.contentResolver, mediaUris)
                        permanentDeleteLauncher.launch(IntentSenderRequest.Builder(pi.intentSender).build())
                    }.onFailure {
                        pendingOsDeleteSongs = emptyList()
                        viewModel.finalizePermanentDelete(targets, deletedBySystemPrompt = false)
                    }
                } else {
                    viewModel.finalizePermanentDelete(targets, deletedBySystemPrompt = false)
                }
            } else {
                viewModel.finalizePermanentDelete(targets, deletedBySystemPrompt = false)
            }
        }
    }

    // 1:1 Interactive Finger-Follow Player Expansion State (0f = MiniPlayer, 1f = FullPlayer)
    val playerExpansion = remember { Animatable(0f) }
    val playerSpringSpec = remember {
        spring<Float>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        )
    }
    fun openFullPlayer() {
        coroutineScope.launch { playerExpansion.animateTo(1f, playerSpringSpec) }
    }
    fun minimizeFullPlayer() {
        coroutineScope.launch { playerExpansion.animateTo(0f, playerSpringSpec) }
    }

    var showQueueSheet by remember { mutableStateOf(false) }
    var showEqualizerSheet by remember { mutableStateOf(false) }
    var showSleepTimerSheet by remember { mutableStateOf(false) }
    var showSpeedPitchSheet by remember { mutableStateOf(false) }
    var showSoundControlSheet by remember { mutableStateOf(false) }
    var showPlayerBackgroundDialog by remember { mutableStateOf(false) }
    var showBottomNavCustomizer by remember { mutableStateOf(false) }
    var showSplashOverlay by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(650L)
        showSplashOverlay = false
    }
    var activeSortSheet by remember { mutableStateOf<ActiveSortSheet?>(null) }
    var actionSong by remember { mutableStateOf<Song?>(null) }
    var infoSong by remember { mutableStateOf<Song?>(null) }
    var rateMoodSong by remember { mutableStateOf<Song?>(null) }
    var tagEditorSongs by remember { mutableStateOf<List<Song>?>(null) }
    var playlistSongIdsToAdd by remember { mutableStateOf<List<Long>?>(null) }

    val executeSwipeAction: (Song, TrackSwipeAction) -> Unit = { song, action ->
        when (action) {
            TrackSwipeAction.NONE -> Unit
            TrackSwipeAction.PLAY_NEXT -> viewModel.playNext(song)
            TrackSwipeAction.ADD_TO_QUEUE -> viewModel.addToQueue(listOf(song))
            TrackSwipeAction.FAVORITE -> viewModel.toggleFavorite(song.id)
            TrackSwipeAction.ADD_TO_PLAYLIST -> playlistSongIdsToAdd = listOf(song.id)
            TrackSwipeAction.RATE_MOOD -> rateMoodSong = song
            TrackSwipeAction.HIDE -> viewModel.hideSongs(listOf(song.id))
            TrackSwipeAction.TRASH -> viewModel.moveSongToTrash(song)
        }
    }

    val navController = rememberNavController()

    val openFullLyricsForSong: (Song) -> Unit = { song ->
        if (playbackState.currentSong?.id != song.id) {
            viewModel.playSong(song, allSongs)
        }
        coroutineScope.launch { playerExpansion.snapTo(0f) }
        navController.navigate(MikaRoute.Lyrics)
    }

    val playCollection: (CollectionType) -> Unit = { type ->
        val tracks = when (type) {
            CollectionType.FAVORITES -> favorites
            CollectionType.LYRICS -> lyricsSongs
            CollectionType.RECENTLY_PLAYED -> recentlyPlayed
            CollectionType.MOST_PLAYED -> mostPlayed
            CollectionType.RECENTLY_ADDED -> recentlyAdded
        }
        if (tracks.isNotEmpty()) {
            viewModel.playAll(tracks, false)
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("No tracks in ${type.title} yet")
            }
        }
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val isOnHomeRoute = navBackStackEntry?.destination?.hasRoute<MikaRoute.Home>() == true ||
        navBackStackEntry?.destination == null
    val isOnFoldersRoute = navBackStackEntry?.destination?.hasRoute<MikaRoute.Folders>() == true
    val isOnVideosRoute = navBackStackEntry?.destination?.hasRoute<MikaRoute.Videos>() == true
    val isOnLyricsRoute = navBackStackEntry?.destination?.hasRoute<MikaRoute.Lyrics>() == true
    val isOnLibraryRoute = navBackStackEntry?.destination?.hasRoute<MikaRoute.Library>() == true
    val showBottomNav = settings.showBottomNavBar &&
        (isOnHomeRoute || isOnFoldersRoute || isOnLibraryRoute || isOnVideosRoute) &&
        selectedSongIds.isEmpty()

    val isPlayerMostlyExpanded by remember { derivedStateOf { playerExpansion.value > 0.5f } }
    val isPlayerCollapsed by remember { derivedStateOf { playerExpansion.value < 0.05f } }
    val showMiniPlayer by remember { derivedStateOf { playerExpansion.value < 0.99f } }
    val showFullPlayer by remember { derivedStateOf { playerExpansion.value > 0.004f } }

    LaunchedEffect(videoState.isFullScreenOpen) {
        if (videoState.isFullScreenOpen && playerExpansion.value > 0f) {
            playerExpansion.snapTo(0f)
        }
    }

    BackHandler(enabled = drawerState.isOpen) {
        coroutineScope.launch { drawerState.close() }
    }
    BackHandler(enabled = !drawerState.isOpen && isPlayerMostlyExpanded && playbackState.currentSong != null && !videoState.isFullScreenOpen) {
        minimizeFullPlayer()
    }

    MikaTheme(
        settings = settings,
        artworkAccentPair = artworkColors,
        autoAccentPair = brightArtworkColors
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val maxHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)

            val onDragExpansionDelta: (Float) -> Unit = { dyPx ->
                val next = (playerExpansion.value - (dyPx / maxHeightPx)).coerceIn(0f, 1f)
                coroutineScope.launch { playerExpansion.snapTo(next) }
            }
            val onDragExpansionEnd: (Float) -> Unit = { velocityY ->
                val target = when {
                    velocityY < -70f -> 1f
                    velocityY > 70f -> 0f
                    playerExpansion.value < 0.5f -> if (playerExpansion.value >= 0.045f) 1f else 0f
                    else -> if (playerExpansion.value <= 0.945f) 0f else 1f
                }
                coroutineScope.launch {
                    playerExpansion.animateTo(
                        targetValue = target,
                        animationSpec = playerSpringSpec
                    )
                }
            }

            ModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = settings.swipeToOpenDrawer &&
                    (isOnHomeRoute || isOnFoldersRoute || isOnLibraryRoute || isOnVideosRoute) &&
                    selectedSongIds.isEmpty() &&
                    isPlayerCollapsed &&
                    !videoState.isFullScreenOpen &&
                    !showQueueSheet &&
                    !showEqualizerSheet &&
                    !showSleepTimerSheet &&
                    !showSpeedPitchSheet &&
                    !showSoundControlSheet,
                drawerContent = {
                    val tabs = if (settings.enabledTabs.isEmpty()) MainTab.entries else settings.enabledTabs
                    MikaNavigationDrawerContent(
                        tabs = tabs,
                        selectedTab = selectedTab,
                        settings = settings,
                        allSongsCount = allSongs.size,
                        playlistsCount = sortedPlaylists.size,
                        foldersCount = allFolders.size,
                        albumsCount = albums.size,
                        artistsCount = artists.size,
                        genresCount = genres.size,
                        favoritesCount = favorites.size,
                        lyricsCount = lyricsSongs.size,
                        mostPlayedCount = mostPlayed.size,
                        recentlyPlayedCount = recentlyPlayed.size,
                        recentlyAddedCount = recentlyAdded.size,
                        trashedCount = trashedSongs.size,
                        sleepTimerState = sleepTimerState,
                        onSelectTab = { tab ->
                            if (!isOnLibraryRoute) {
                                navController.navigate(MikaRoute.Library) {
                                    popUpTo<MikaRoute.Home> { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                            viewModel.selectTab(tab)
                        },
                        onOpenHome = {
                            navController.navigate(MikaRoute.Home) {
                                popUpTo<MikaRoute.Home> { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        onOpenFolders = {
                            navController.navigate(MikaRoute.Folders) {
                                popUpTo<MikaRoute.Home> { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        onCloseDrawer = { coroutineScope.launch { drawerState.close() } },
                        onOpenAbout = { navController.navigate(MikaRoute.About) },
                        onOpenVideos = {
                            navController.navigate(MikaRoute.Videos) {
                                launchSingleTop = true
                            }
                        },
                        onOpenGenres = { navController.navigate(MikaRoute.Genres) },
                        onOpenCollection = { type -> navController.navigate(MikaRoute.Collection(type.name)) },
                        onOpenStatistics = { navController.navigate(MikaRoute.Statistics) },
                        onOpenEqualizer = { showEqualizerSheet = true },
                        onOpenTrash = { navController.navigate(MikaRoute.Trash) },
                        onOpenBackup = { navController.navigate(MikaRoute.Backup) },
                        onOpenSettings = { navController.navigate(MikaRoute.Settings) },
                        onOpenSleepTimer = { showSleepTimerSheet = true },
                        onSetThemeMode = viewModel::setThemeMode,
                        hiddenCount = hiddenSongs.size,
                        onOpenHidden = { navController.navigate(MikaRoute.Hidden) },
                        onOpenPlayerStyleStudio = { navController.navigate(MikaRoute.PlayerStyleSettings) }
                    )
                }
            ) {
                Scaffold { _ ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        NavHost(
                            navController = navController,
                            startDestination = MikaRoute.Home,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            composable<MikaRoute.Home> {
                                HomeScreen(
                                    allSongs = allSongs,
                                    recentlyPlayed = recentlyPlayed,
                                    recentlyAdded = recentlyAdded,
                                    mostPlayed = mostPlayed,
                                    favorites = favorites,
                                    recentPlaylists = recentPlaylists,
                                    playlistsCount = sortedPlaylists.size,
                                    trashedCount = trashedSongs.size,
                                    currentSong = playbackState.currentSong,
                                    isPlayingAudio = playbackState.isPlaying,
                                    currentVideo = videoState.currentVideo,
                                    isPlayingVideo = videoState.isPlaying,
                                    hasAudioPermission = hasAudioPermission,
                                    bottomNavVisible = showBottomNav,
                                    onRequestPermission = requestAudioPermissions,
                                    onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                                    onOpenSearch = { navController.navigate(MikaRoute.Search) },
                                    onOpenSettings = { navController.navigate(MikaRoute.Settings) },
                                    onPlaySong = { song, list -> viewModel.playSong(song, list) },
                                    onToggleAudioPlayPause = viewModel.playbackController::togglePlayPause,
                                    onOpenFullAudioPlayer = { openFullPlayer() },
                                    onShuffleAll = { viewModel.playAll(allSongs, true) },
                                    onPlayFavorites = { viewModel.playAll(favorites, false) },
                                    onNavigateToLibrarySongs = {
                                        viewModel.selectTab(MainTab.SONGS)
                                        navController.navigate(MikaRoute.Library) { launchSingleTop = true }
                                    },
                                    onNavigateToLibraryPlaylists = {
                                        viewModel.selectTab(MainTab.PLAYLISTS)
                                        navController.navigate(MikaRoute.Library) { launchSingleTop = true }
                                    },
                                    onNavigateToLibraryFolders = {
                                        navController.navigate(MikaRoute.Folders) {
                                            popUpTo<MikaRoute.Home> { inclusive = false }
                                            launchSingleTop = true
                                        }
                                    },
                                    onOpenPlaylist = { id -> navController.navigate(MikaRoute.PlaylistDetail(id)) },
                                    onImportAudio = { importAudioLauncher.launch(arrayOf("audio/*")) },
                                    onOpenStatistics = { navController.navigate(MikaRoute.Statistics) },
                                    onOpenEqualizer = { showEqualizerSheet = true },
                                    onOpenCollection = { type -> navController.navigate(MikaRoute.Collection(type.name)) },
                                    videos = videos,
                                    onPlayVideo = viewModel::playVideo,
                                    onToggleVideoPlayPause = viewModel.videoPlaybackController::togglePlayPause,
                                    onExpandVideoPlayer = { viewModel.videoPlaybackController.setFullScreen(true) },
                                    onOpenVideos = {
                                        navController.navigate(MikaRoute.Videos) { launchSingleTop = true }
                                    },
                                    onOpenPlayerStyleStudio = {
                                        navController.navigate(MikaRoute.PlayerStyleSettings)
                                    },
                                    playHistory = playHistory,
                                    onRateAndMood = { rateMoodSong = it }
                                )
                            }

                            composable<MikaRoute.Folders> {
                                FoldersScreen(
                                    browserState = folderBrowserState,
                                    allFolders = allFolders,
                                    currentSongId = playbackState.currentSong?.id,
                                    isPlaying = playbackState.isPlaying,
                                    showLyricsBadges = settings.showLyricsBadges,
                                    sortField = folderSortField,
                                    sortDirection = folderSortDirection,
                                    onNavigateFolder = viewModel::navigateToFolder,
                                    onNavigateUp = viewModel::navigateFolderUp,
                                    onUpdateFolderSort = viewModel::updateFolderSort,
                                    onPlaySong = { song, list -> viewModel.playSong(song, list) },
                                    onPlayFolder = { songs -> viewModel.playAll(songs, false) },
                                    onPlayFolderByPath = { path ->
                                        val folderSongs = allSongs.filter {
                                            it.folderPath == path || it.folderPath.startsWith("$path/")
                                        }
                                        if (folderSongs.isNotEmpty()) {
                                            viewModel.playAll(folderSongs, false)
                                        }
                                    },
                                    onSongMoreClick = { actionSong = it },
                                    onLyricsBadgeLongClick = openFullLyricsForSong,
                                    onShuffleFolder = { songs -> viewModel.playAll(songs, true) },
                                    enableFoldersHierarchy = settings.enableFoldersHierarchy,
                                    skipSingleSubfolder = settings.skipSingleSubfolder,
                                    onUpdateHierarchyOptions = viewModel::setFoldersHierarchy,
                                    videos = videos,
                                    onPlayVideo = viewModel::playVideo,
                                    onAddAudioFolder = viewModel::addIncludedFolderTreeUri,
                                    onAddVideoFolder = viewModel::onVideoFolderPicked,
                                    onRefreshLibrary = viewModel::rescanLibrary,
                                    bottomNavVisible = showBottomNav,
                                    onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                                    onOpenSort = { activeSortSheet = ActiveSortSheet.FOLDERS },
                                    onOpenSettings = { navController.navigate(MikaRoute.Settings) },
                                    showLikeButton = settings.showLikeButtonInList,
                                    swipeLeftAction = settings.swipeLeftAction,
                                    swipeRightAction = settings.swipeRightAction,
                                    onToggleFavorite = { viewModel.toggleFavorite(it.id) },
                                    onExecuteSwipeAction = executeSwipeAction,
                                    onRateAndMood = { rateMoodSong = it }
                                )
                            }

                            composable<MikaRoute.Library> {
                                MainLibraryScreen(
                                    selectedTab = selectedTab,
                                    visibleTabs = settings.enabledTabs,
                                    isDrawerOpen = drawerState.isOpen,
                                    settings = settings,
                                    hasAudioPermission = hasAudioPermission,
                                    isScanning = isScanning,
                                    allSongs = allSongs,
                                    recentlyPlayed = recentlyPlayed,
                                    recentlyAdded = recentlyAdded,
                                    mostPlayed = mostPlayed,
                                    favorites = favorites,
                                    lyricsSongs = lyricsSongs,
                                    playlists = sortedPlaylists,
                                    recentPlaylists = recentPlaylists,
                                    trashedCount = trashedSongs.size,
                                    albums = albums,
                                    artists = artists,
                                    genres = genres,
                                    genreSortField = genreSortField,
                                    genreSortDirection = genreSortDirection,
                                    allFolders = allFolders,
                                    folderBrowserState = folderBrowserState,
                                    folderSortField = folderSortField,
                                    folderSortDirection = folderSortDirection,
                                    playbackState = playbackState,
                                    selectedSongIds = selectedSongIds,
                                    onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                                    onSelectTab = viewModel::selectTab,
                                    onRequestPermission = requestAudioPermissions,
                                    onImportAudio = { importAudioLauncher.launch(arrayOf("audio/*")) },
                                    onOpenSearch = { navController.navigate(MikaRoute.Search) },
                                    onOpenSort = { activeSortSheet = ActiveSortSheet.SONGS },
                                    onOpenPlaylistSort = { activeSortSheet = ActiveSortSheet.PLAYLISTS },
                                    onOpenFolderListSort = { activeSortSheet = ActiveSortSheet.FOLDERS },
                                    onOpenAlbumSort = { activeSortSheet = ActiveSortSheet.ALBUMS },
                                    onOpenArtistSort = { activeSortSheet = ActiveSortSheet.ARTISTS },
                                    onOpenGenreSort = { activeSortSheet = ActiveSortSheet.GENRES },
                                    onOpenSettings = { navController.navigate(MikaRoute.Settings) },
                                    onOpenStatistics = { navController.navigate(MikaRoute.Statistics) },
                                    onOpenTrash = { navController.navigate(MikaRoute.Trash) },
                                    onOpenGenres = { navController.navigate(MikaRoute.Genres) },
                                    onOpenGenre = { genreName ->
                                        navController.navigate(MikaRoute.GenreDetail(genreName))
                                    },
                                    onOpenCollection = { type ->
                                        navController.navigate(MikaRoute.Collection(type.name))
                                    },
                                    onOpenAbout = { navController.navigate(MikaRoute.About) },
                                    onPlaySong = { song, list -> viewModel.playSong(song, list) },
                                    onPlayFolderByPath = { path ->
                                        val folderSongs = allSongs.filter {
                                            it.folderPath == path || it.folderPath.startsWith("$path/")
                                        }
                                        if (folderSongs.isNotEmpty()) {
                                            viewModel.playAll(folderSongs, false)
                                        }
                                    },
                                    onPlayAll = { list, shuffle -> viewModel.playAll(list, shuffle) },
                                    onPlayCollection = playCollection,
                                    onOpenFullLyricsForSong = openFullLyricsForSong,
                                    onToggleSongSelection = viewModel::toggleSongSelection,
                                    onSelectAllVisible = { viewModel.selectAllVisible(allSongs) },
                                    onClearSelection = viewModel::clearSelection,
                                    onBulkAddToQueue = viewModel::bulkAddToQueue,
                                    onBulkAddToPlaylist = {
                                        playlistSongIdsToAdd = selectedSongIds.toList()
                                        viewModel.clearSelection()
                                    },
                                    onBulkFavorite = { viewModel.bulkFavorite(true) },
                                    onBulkEditTags = {
                                        val selected = allSongs.filter { it.id in selectedSongIds }
                                        if (selected.isNotEmpty()) {
                                            tagEditorSongs = selected
                                        }
                                    },
                                    onBulkMoveToTrash = viewModel::bulkMoveToTrash,
                                    onSongMoreClick = { actionSong = it },
                                    onCreatePlaylist = { viewModel.createPlaylist(it) },
                                    onRenamePlaylist = viewModel::renamePlaylist,
                                    onDeletePlaylist = viewModel::deletePlaylist,
                                    onOpenPlaylist = { id ->
                                        navController.navigate(MikaRoute.PlaylistDetail(id))
                                    },
                                    onOpenAlbum = { id ->
                                        navController.navigate(MikaRoute.AlbumDetail(id))
                                    },
                                    onOpenArtist = { id ->
                                        navController.navigate(MikaRoute.ArtistDetail(id))
                                    },
                                    onNavigateFolder = viewModel::navigateToFolder,
                                    onNavigateFolderUp = viewModel::navigateFolderUp,
                                    onUpdateFolderSort = viewModel::updateFolderSort,
                                    onRescanLibrary = viewModel::rescanLibrary,
                                    onUpdateFolderHierarchyOptions = viewModel::setFoldersHierarchy,
                                    videos = videos,
                                    onPlayVideo = viewModel::playVideo,
                                    onOpenVideos = {
                                        navController.navigate(MikaRoute.Videos) {
                                            launchSingleTop = true
                                        }
                                    },
                                    onOpenPlayerStyleStudio = {
                                        navController.navigate(MikaRoute.PlayerStyleSettings)
                                    },
                                    onBulkHide = {
                                        viewModel.hideSongs(selectedSongIds.toList())
                                    },
                                    onAddAudioFolder = viewModel::addIncludedFolderTreeUri,
                                    onAddVideoFolder = viewModel::onVideoFolderPicked,
                                    onToggleTabEnabled = viewModel::toggleTabEnabled,
                                    onMoveTab = viewModel::moveEnabledTab,
                                    onSetDefaultTab = viewModel::setDefaultTab,
                                    playHistorySongs = playHistorySongs,
                                    onReorderEnabledTabs = viewModel::reorderEnabledTabs,
                                    onSetTabActions = viewModel::setTabActions,
                                    onToggleFavorite = { viewModel.toggleFavorite(it.id) },
                                    onExecuteSwipeAction = executeSwipeAction,
                                    onRateAndMood = { rateMoodSong = it }
                                )
                            }

                        composable<MikaRoute.Search> {
                            SearchScreen(
                                query = searchQuery,
                                results = searchResults,
                                history = searchHistory,
                                currentSongId = playbackState.currentSong?.id,
                                isPlaying = playbackState.isPlaying,
                                showLyricsBadges = settings.showLyricsBadges,
                                onQueryChange = viewModel::updateSearchQuery,
                                onCommitQuery = viewModel::commitSearchQuery,
                                onRemoveHistoryItem = viewModel::removeSearchHistoryItem,
                                onClearHistory = viewModel::clearSearchHistory,
                                onBack = { navController.popBackStack() },
                                onPlaySong = { song, list -> viewModel.playSong(song, list) },
                                onSongMoreClick = { actionSong = it },
                                onOpenAlbum = { id -> navController.navigate(MikaRoute.AlbumDetail(id)) },
                                onOpenArtist = { id -> navController.navigate(MikaRoute.ArtistDetail(id)) },
                                onOpenPlaylist = { id -> navController.navigate(MikaRoute.PlaylistDetail(id)) },
                                onOpenFolder = { path ->
                                    viewModel.navigateToFolder(path)
                                    navController.navigate(MikaRoute.Folders) {
                                        popUpTo<MikaRoute.Home> { inclusive = false }
                                        launchSingleTop = true
                                    }
                                },
                                onOpenGenre = { genreName ->
                                    navController.navigate(MikaRoute.GenreDetail(genreName))
                                },
                                onPlayVideo = viewModel::playVideo,
                                showLikeButton = settings.showLikeButtonInList,
                                swipeLeftAction = settings.swipeLeftAction,
                                swipeRightAction = settings.swipeRightAction,
                                onToggleFavorite = { viewModel.toggleFavorite(it.id) },
                                onExecuteSwipeAction = executeSwipeAction,
                                onRateAndMood = { rateMoodSong = it }
                            )
                        }

                        composable<MikaRoute.Settings> {
                            SettingsScreen(
                                settings = settings,
                                cacheSizeBytes = cacheSizeBytes,
                                isScanning = isScanning,
                                songCount = rawSongs.size,
                                trashedCount = trashedSongs.size,
                                availableFolders = availableFolders,
                                duplicateGroups = duplicateGroups,
                                isScanningDuplicates = isScanningDuplicates,
                                onBack = { navController.popBackStack() },
                                onSetThemeMode = viewModel::setThemeMode,
                                onSetAccentColor = viewModel::setAccentColor,
                                onSetCustomAccentColor = viewModel::setCustomAccentColor,
                                onSetAutoColoring = viewModel::setAutoColoring,
                                onSetApplyColorOnApp = { enabled ->
                                    viewModel.setColorTargets(onApp = enabled, onPlayer = null)
                                },
                                onSetApplyColorOnPlayer = { enabled ->
                                    viewModel.setColorTargets(onApp = null, onPlayer = enabled)
                                },
                                onSetFontScale = viewModel::setFontScale,
                                onSetCornerRadiusDp = viewModel::setCornerRadiusDp,
                                onSetGradientStyle = viewModel::setGradientStyle,
                                onSetGradientTargets = viewModel::setGradientTargets,
                                onToggleTabEnabled = viewModel::toggleTabEnabled,
                                onMoveTab = viewModel::moveEnabledTab,
                                onSetDefaultTab = viewModel::setDefaultTab,
                                onSetTabActions = viewModel::setTabActions,
                                onRefreshFolders = viewModel::refreshDeviceFolders,
                                onToggleExcludedFolder = viewModel::toggleExcludedFolder,
                                onClearExcludedFolders = viewModel::clearExcludedFolders,
                                onSetMinDurationSec = viewModel::setMinDurationSec,
                                onSetMinFileSizeKb = viewModel::setMinFileSizeKb,
                                onRescanLibrary = viewModel::rescanLibrary,
                                onSetShowLyricsBadges = viewModel::setShowLyricsBadges,
                                onSetObeyNoMedia = viewModel::setObeyNoMedia,
                                onSetAutoScanOnStartup = viewModel::setAutoScanOnStartup,
                                onUpdateExtensionBlacklist = viewModel::updateExtensionBlacklist,
                                onSetGroupArtworkByAlbum = viewModel::setGroupArtworkByAlbum,
                                onScanDuplicates = viewModel::scanDuplicates,
                                onDeleteDuplicateSong = viewModel::deleteDuplicateSong,
                                onSetGapless = viewModel::setGaplessPlayback,
                                onSetCrossfade = viewModel::setCrossfade,
                                onSetSkipInterval = viewModel::setSkipInterval,
                                onSetRememberLastPosition = viewModel::setRememberLastPosition,
                                onSetAllowSimultaneousPlayback = viewModel::setAllowSimultaneousPlayback,
                                onSetShowBottomNavBar = viewModel::setShowBottomNavBar,
                                onClearArtworkCache = viewModel::clearArtworkCache,
                                onClearAllAppCaches = viewModel::clearAllAppCaches,
                                onClearPlaybackHistory = viewModel::clearPlaybackHistory,
                                onClearSearchHistory = viewModel::clearSearchHistory,
                                onOpenStatistics = { navController.navigate(MikaRoute.Statistics) },
                                onOpenTrash = { navController.navigate(MikaRoute.Trash) },
                                onOpenBackup = { navController.navigate(MikaRoute.Backup) },
                                onOpenAbout = { navController.navigate(MikaRoute.About) },
                                onAddFolder = { addFolderTreeLauncher.launch(null) },
                                onOpenWidgetSettings = { navController.navigate(MikaRoute.WidgetSettings) },
                                onOpenPlayerBackground = { showPlayerBackgroundDialog = true },
                                onOpenPlayerStyleStudio = { navController.navigate(MikaRoute.PlayerStyleSettings) },
                                onSetPickColorsFromWallpaper = viewModel::setPickColorsFromWallpaper,
                                onSetForceMiniPlayerTrackColors = viewModel::setForceMiniPlayerTrackColors,
                                onSetUsePitchBlack = viewModel::setUsePitchBlack,
                                onSetCustomDarkAccentColor = viewModel::setCustomDarkAccentColor,
                                onSetSkipSilence = viewModel::setSkipSilence,
                                onSetCrossfadeEnabled = viewModel::setCrossfadeEnabled,
                                onSetCrossfadeDurationMs = viewModel::setCrossfadeDurationMs,
                                onSetCrossfadeTriggerLastSec = viewModel::setCrossfadeTriggerLastSec,
                                onSetCrossfadeStyle = viewModel::setCrossfadeStyle,
                                onSetPlayPauseFadeEnabled = viewModel::setPlayPauseFadeEnabled,
                                onSetPlayFadeDurationMs = viewModel::setPlayFadeDurationMs,
                                onSetPauseFadeDurationMs = viewModel::setPauseFadeDurationMs,
                                onSetJumpToFirstAfterQueue = viewModel::setJumpToFirstAfterQueue,
                                onSetPreviousButtonReplays = viewModel::setPreviousButtonReplays,
                                onSetRestorePositionMinDurationMin = viewModel::setRestorePositionMinDurationMin,
                                onSetVisualEffects = viewModel::setVisualEffects,
                                onSetArtworkGestures = viewModel::setArtworkGestures,
                                onResetArtworkGestures = viewModel::resetArtworkGestures,
                                onMoveBottomNavDest = viewModel::moveBottomNavDest,
                                onToggleBottomNavDestHidden = viewModel::toggleBottomNavDestHidden,
                                onSetAutoScan = viewModel::setAutoScan,
                                onSetAppIconScale = viewModel::setAppIconScale,
                                onSetAppIconPreset = viewModel::setAppIconPreset,
                                onPickCustomAppIcon = {
                                    pickCustomAppIconLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onSetSeekBarStyle = viewModel::setSeekBarStyle,
                                onSetPlaybackIconStyle = viewModel::setPlaybackIconStyle,
                                onSetShowActualArtworkRatio = viewModel::setShowActualArtworkRatio,
                                onSetShowLikeButtonInList = viewModel::setShowLikeButtonInList,
                                onSetTrackSwipeActions = viewModel::setTrackSwipeActions,
                                onSetSwipeToOpenDrawer = viewModel::setSwipeToOpenDrawer,
                                onResetAllSettings = viewModel::resetAllSettings,
                                onClearAllAppData = viewModel::clearAllAppData
                            )
                        }

                        composable<MikaRoute.Statistics> {
                            StatisticsScreen(
                                stats = libraryStats,
                                rawSongs = rawSongs,
                                albums = albums,
                                playHistory = playHistory,
                                videos = videos,
                                onBack = { navController.popBackStack() },
                                onPlaySong = { song, list -> viewModel.playSong(song, list) },
                                onPlayTopMix = {
                                    val mix = libraryStats.topPlayedSongs.ifEmpty { allSongs.take(15) }
                                    if (mix.isNotEmpty()) {
                                        viewModel.playAll(mix, false)
                                    }
                                },
                                onOpenGenres = { navController.navigate(MikaRoute.Genres) },
                                onOpenGenre = { genreName ->
                                    navController.navigate(MikaRoute.GenreDetail(genreName))
                                },
                                onOpenBackup = { navController.navigate(MikaRoute.Backup) },
                                onOpenTrash = { navController.navigate(MikaRoute.Trash) }
                            )
                        }

                        composable<MikaRoute.Trash> {
                            TrashScreen(
                                trashedSongs = trashedSongs,
                                onBack = { navController.popBackStack() },
                                onRestoreSong = { song -> viewModel.restoreSongsFromTrash(listOf(song.id)) },
                                onRestoreAll = viewModel::restoreAllFromTrash,
                                onPermanentDeleteSongs = triggerPermanentDelete
                            )
                        }

                        composable<MikaRoute.Hidden> {
                            HiddenSongsScreen(
                                hiddenSongs = hiddenSongs,
                                currentSongId = playbackState.currentSong?.id,
                                isPlaying = playbackState.isPlaying,
                                onBack = { navController.popBackStack() },
                                onPlaySong = { song, list -> viewModel.playSong(song, list) },
                                onUnhideSong = { songId -> viewModel.unhideSongs(listOf(songId)) },
                                onUnhideAll = { viewModel.unhideSongs(hiddenSongs.map { it.id }) }
                            )
                        }

                        composable<MikaRoute.About> {
                            AboutScreen(
                                stats = libraryStats,
                                onBack = { navController.popBackStack() },
                                onOpenBackup = { navController.navigate(MikaRoute.Backup) },
                                onOpenSettings = { navController.navigate(MikaRoute.Settings) }
                            )
                        }

                        composable<MikaRoute.Backup> {
                            BackupScreen(
                                settings = settings,
                                stats = libraryStats,
                                localBackups = localBackups,
                                pendingBackupPreview = pendingBackupPreview,
                                isBusy = isBackupBusy,
                                onBack = { navController.popBackStack() },
                                onCreateLocalBackup = viewModel::createLocalBackup,
                                onExportBackup = {
                                    val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                                    exportBackupLauncher.launch("mika_backup_$stamp.zip")
                                },
                                onImportBackup = {
                                    importBackupLauncher.launch(arrayOf("application/zip", "application/json", "*/*"))
                                },
                                onInspectLocalBackup = viewModel::inspectLocalBackup,
                                onDeleteLocalBackup = viewModel::deleteLocalBackup,
                                onConfirmRestore = viewModel::confirmPendingRestore,
                                onDismissRestore = viewModel::dismissPendingRestore,
                                onSetAutoBackupIntervalDays = viewModel::setAutoBackupIntervalDays,
                                onPickDefaultBackupLocation = { backupLocationTreeLauncher.launch(null) },
                                onResetDefaultBackupLocation = { viewModel.setDefaultBackupLocation(null) }
                            )
                        }

                        composable<MikaRoute.WidgetSettings> {
                            WidgetSettingsScreen(
                                initialConfig = settings.widgetConfig,
                                currentSong = playbackState.currentSong,
                                isPlaying = playbackState.isPlaying,
                                artworkAccentPair = artworkColors,
                                onSave = { config ->
                                    viewModel.saveWidgetConfig(config)
                                    navController.popBackStack()
                                },
                                onReset = viewModel::resetWidgetConfig,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable<MikaRoute.PlayerStyleSettings> {
                            PlayerStyleSettingsScreen(
                                settings = settings,
                                currentSong = playbackState.currentSong,
                                isPlaying = playbackState.isPlaying,
                                artworkAccentPair = artworkColors,
                                onBack = { navController.popBackStack() },
                                onUpdateMiniPlayerConfig = viewModel::saveMiniPlayerConfig,
                                onResetMiniPlayerConfig = viewModel::resetMiniPlayerConfig,
                                onUpdateFullPlayerConfig = viewModel::saveFullPlayerConfig,
                                onResetFullPlayerConfig = viewModel::resetFullPlayerConfig
                            )
                        }

                        composable<MikaRoute.Genres> {
                            GenresScreen(
                                genres = genres,
                                sortField = genreSortField,
                                sortDirection = genreSortDirection,
                                onUpdateSort = viewModel::setGenreSort,
                                onBack = { navController.popBackStack() },
                                onGenreClick = { genreName ->
                                    navController.navigate(MikaRoute.GenreDetail(genreName))
                                }
                            )
                        }

                        composable<MikaRoute.GenreDetail> { backStackEntry ->
                            val route = backStackEntry.toRoute<MikaRoute.GenreDetail>()
                            val genreSongs = allSongs.filter { s ->
                                val resolvedGenre = s.genre.trim().ifBlank { "Other / Untagged" }
                                resolvedGenre.equals(route.genreName, ignoreCase = true)
                            }
                            GenreDetailScreen(
                                genreName = route.genreName,
                                songs = genreSongs,
                                currentSongId = playbackState.currentSong?.id,
                                isPlaying = playbackState.isPlaying,
                                showLyricsBadges = settings.showLyricsBadges,
                                onBack = { navController.popBackStack() },
                                onPlayAll = { list, shuffle -> viewModel.playAll(list, shuffle) },
                                onPlaySong = { song, list -> viewModel.playSong(song, list) },
                                onSongMoreClick = { actionSong = it },
                                onLyricsBadgeLongClick = openFullLyricsForSong,
                                showLikeButton = settings.showLikeButtonInList,
                                swipeLeftAction = settings.swipeLeftAction,
                                swipeRightAction = settings.swipeRightAction,
                                onToggleFavorite = { viewModel.toggleFavorite(it.id) },
                                onExecuteSwipeAction = executeSwipeAction,
                                onRateAndMood = { rateMoodSong = it }
                            )
                        }

                        composable<MikaRoute.Collection> { backStackEntry ->
                            val route = backStackEntry.toRoute<MikaRoute.Collection>()
                            val collectionType = CollectionType.entries.firstOrNull {
                                it.name == route.collectionType
                            } ?: CollectionType.FAVORITES
                            val collectionSongs = when (collectionType) {
                                CollectionType.FAVORITES -> favorites
                                CollectionType.LYRICS -> lyricsSongs
                                CollectionType.RECENTLY_PLAYED -> recentlyPlayed
                                CollectionType.MOST_PLAYED -> mostPlayed
                                CollectionType.RECENTLY_ADDED -> recentlyAdded
                            }
                            CollectionScreen(
                                collectionType = collectionType,
                                songs = collectionSongs,
                                currentSongId = playbackState.currentSong?.id,
                                isPlaying = playbackState.isPlaying,
                                showLyricsBadges = settings.showLyricsBadges,
                                onBack = { navController.popBackStack() },
                                onPlayAll = { list, shuffle -> viewModel.playAll(list, shuffle) },
                                onPlaySong = { song, list -> viewModel.playSong(song, list) },
                                onSongMoreClick = { actionSong = it },
                                onLyricsBadgeLongClick = openFullLyricsForSong,
                                showLikeButton = settings.showLikeButtonInList,
                                swipeLeftAction = settings.swipeLeftAction,
                                swipeRightAction = settings.swipeRightAction,
                                onToggleFavorite = { viewModel.toggleFavorite(it.id) },
                                onExecuteSwipeAction = executeSwipeAction,
                                onRateAndMood = { rateMoodSong = it }
                            )
                        }

                        composable<MikaRoute.Lyrics> {
                            LyricsScreen(
                                playbackState = playbackState,
                                lyrics = currentLyrics,
                                progressFlow = viewModel.playbackProgress,
                                artworkAccentPair = artworkColors,
                                onBack = { navController.popBackStack() },
                                onTogglePlayPause = viewModel.playbackController::togglePlayPause,
                                onSkipNext = viewModel.playbackController::skipNext,
                                onSkipPrevious = viewModel.playbackController::skipPrevious,
                                onSeekTo = viewModel.playbackController::seekTo,
                                onImportLyrics = { importLyricsLauncher.launch(arrayOf("*/*")) },
                                onSaveCustomLyrics = viewModel::saveCustomLyricsForCurrentSong,
                                onClearCustomLyrics = viewModel::clearCustomLyricsForCurrentSong,
                                onOpenPlayer = {
                                    navController.popBackStack()
                                    openFullPlayer()
                                }
                            )
                        }

                        composable<MikaRoute.PlaylistDetail> { backStackEntry ->
                            val route = backStackEntry.toRoute<MikaRoute.PlaylistDetail>()
                            val playlist = playlists.firstOrNull { it.id == route.playlistId }
                            PlaylistDetailScreen(
                                playlist = playlist,
                                allSongs = allSongs,
                                currentSongId = playbackState.currentSong?.id,
                                isPlaying = playbackState.isPlaying,
                                onBack = { navController.popBackStack() },
                                onPlayAll = { songs, shuffle ->
                                    viewModel.playAll(songs, shuffle, playlistId = route.playlistId)
                                },
                                onPlaySong = { song, list ->
                                    viewModel.playSong(song, list, playlistId = route.playlistId)
                                },
                                onAddSongs = { ids -> viewModel.addSongsToPlaylist(route.playlistId, ids) },
                                onRemoveSong = { songId ->
                                    viewModel.removeSongFromPlaylist(route.playlistId, songId)
                                },
                                onReorderSongs = { ordered ->
                                    viewModel.reorderPlaylistSongs(route.playlistId, ordered)
                                },
                                onRenamePlaylist = { newName ->
                                    viewModel.renamePlaylist(route.playlistId, newName)
                                }
                            )
                        }

                        composable<MikaRoute.AlbumDetail> { backStackEntry ->
                            val route = backStackEntry.toRoute<MikaRoute.AlbumDetail>()
                            val album = albums.firstOrNull { it.id == route.albumId }
                            val albumSongs = rawSongs
                                .filter { it.albumId == route.albumId }
                                .sortedBy { it.trackNumber }
                            AlbumDetailScreen(
                                album = album,
                                albumSongs = albumSongs,
                                currentSongId = playbackState.currentSong?.id,
                                isPlaying = playbackState.isPlaying,
                                showLyricsBadges = settings.showLyricsBadges,
                                onBack = { navController.popBackStack() },
                                onPlayAll = { list, shuffle -> viewModel.playAll(list, shuffle) },
                                onPlaySong = { song, list -> viewModel.playSong(song, list) },
                                onSongMoreClick = { actionSong = it },
                                onLyricsBadgeLongClick = openFullLyricsForSong,
                                showLikeButton = settings.showLikeButtonInList,
                                swipeLeftAction = settings.swipeLeftAction,
                                swipeRightAction = settings.swipeRightAction,
                                onToggleFavorite = { viewModel.toggleFavorite(it.id) },
                                onExecuteSwipeAction = executeSwipeAction,
                                onRateAndMood = { rateMoodSong = it }
                            )
                        }

                        composable<MikaRoute.ArtistDetail> { backStackEntry ->
                            val route = backStackEntry.toRoute<MikaRoute.ArtistDetail>()
                            val artist = artists.firstOrNull { it.id == route.artistId }
                            val artistSongs = rawSongs.filter { it.artistId == route.artistId }
                            val artistAlbums = albums.filter { al ->
                                artistSongs.any { it.albumId == al.id }
                            }
                            ArtistDetailScreen(
                                artist = artist,
                                artistAlbums = artistAlbums,
                                artistSongs = artistSongs,
                                currentSongId = playbackState.currentSong?.id,
                                isPlaying = playbackState.isPlaying,
                                showLyricsBadges = settings.showLyricsBadges,
                                onBack = { navController.popBackStack() },
                                onAlbumClick = { id -> navController.navigate(MikaRoute.AlbumDetail(id)) },
                                onPlayAll = { list, shuffle -> viewModel.playAll(list, shuffle) },
                                onPlaySong = { song, list -> viewModel.playSong(song, list) },
                                onSongMoreClick = { actionSong = it },
                                onLyricsBadgeLongClick = openFullLyricsForSong,
                                showLikeButton = settings.showLikeButtonInList,
                                swipeLeftAction = settings.swipeLeftAction,
                                swipeRightAction = settings.swipeRightAction,
                                onToggleFavorite = { viewModel.toggleFavorite(it.id) },
                                onExecuteSwipeAction = executeSwipeAction,
                                onRateAndMood = { rateMoodSong = it }
                            )
                        }
                        composable<MikaRoute.Videos> {
                            VideosScreen(
                                videos = videos,
                                currentVideoId = videoState.currentVideo?.id,
                                isPlaying = videoState.isPlaying,
                                isLoading = isLoadingVideos,
                                sortField = videoSortField,
                                sortDirection = videoSortDirection,
                                bottomNavVisible = showBottomNav,
                                onVideosPicked = viewModel::onVideosPicked,
                                onVideoFolderPicked = viewModel::onVideoFolderPicked,
                                onPlayVideo = viewModel::playVideo,
                                onRemoveVideo = viewModel::removeVideoFromList,
                                onSetSort = viewModel::setVideoSort,
                                onRefresh = viewModel::refreshVideos,
                                onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                                onOpenSettings = { navController.navigate(MikaRoute.Settings) },
                                isGridView = settings.videoGridView,
                                onToggleGridView = viewModel::setVideoGridView
                            )
                        }
                    }

                    // Bottom dock: Audio Mini-Player + Solid (Non-Blurred) Bottom Navigation Bar
                    val showAudioMini = !isOnLyricsRoute && playbackState.currentSong != null && showMiniPlayer

                    if (showAudioMini || showBottomNav) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .graphicsLayer {
                                    val t = (playerExpansion.value * 2.5f).coerceIn(0f, 1f)
                                    translationY = t * size.height
                                    alpha = 1f - t
                                }
                        ) {
                            if (showAudioMini) {
                                MiniPlayer(
                                    playbackState = playbackState,
                                    progressFlow = viewModel.playbackProgress,
                                    artworkAccentPair = artworkColors,
                                    applyNavigationBarsPadding = !showBottomNav,
                                    forceTrackColors = settings.forceMiniPlayerTrackColors,
                                    onOpenFullPlayer = { openFullPlayer() },
                                    onDragExpansionDelta = onDragExpansionDelta,
                                    onDragExpansionEnd = onDragExpansionEnd,
                                    onTogglePlayPause = viewModel.playbackController::togglePlayPause,
                                    onSkipNext = viewModel.playbackController::skipNext,
                                    onSkipPrevious = viewModel.playbackController::skipPrevious,
                                    onToggleFavorite = viewModel::toggleFavorite,
                                    miniPlayerConfig = settings.miniPlayerConfig,
                                    playbackIconStyle = settings.playbackIconStyle
                                )
                            }

                            if (showBottomNav) {
                                val visibleBottomDests = remember(settings.bottomNavOrder, settings.hiddenBottomNavItems) {
                                    (settings.bottomNavOrder + BottomNavDest.entries)
                                        .distinct()
                                        .filter { it !in settings.hiddenBottomNavItems }
                                        .ifEmpty { listOf(BottomNavDest.HOME) }
                                }
                                val dockShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                                Surface(
                                    shape = dockShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    tonalElevation = 2.dp,
                                    shadowElevation = 10.dp,
                                    border = BorderStroke(
                                        width = 0.85.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("optional_bottom_nav_bar")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .navigationBarsPadding()
                                            .height(60.dp)
                                            .padding(horizontal = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        visibleBottomDests.forEach { dest ->
                                            val isSelected = when (dest) {
                                                BottomNavDest.HOME -> isOnHomeRoute
                                                BottomNavDest.FOLDERS -> isOnFoldersRoute
                                                BottomNavDest.LIBRARY -> isOnLibraryRoute
                                                BottomNavDest.VIDEOS -> isOnVideosRoute
                                            }
                                            val iconVector = when (dest) {
                                                BottomNavDest.HOME -> if (isSelected) MikaIcons.HomeBold else MikaIcons.HomeLinear
                                                BottomNavDest.FOLDERS -> if (isSelected) MikaIcons.FolderBold else MikaIcons.Folder
                                                BottomNavDest.LIBRARY -> if (isSelected) MikaIcons.MusicNoteBold else MikaIcons.MusicNote
                                                BottomNavDest.VIDEOS -> if (isSelected) MikaIcons.VideoBold else MikaIcons.VideoLinear
                                            }
                                            val accentColor = MaterialTheme.colorScheme.primary
                                            val itemTint = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(50.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(
                                                        if (isSelected) accentColor.copy(alpha = 0.16f) else Color.Transparent
                                                    )
                                                    .combinedClickable(
                                                        onClick = {
                                                            when (dest) {
                                                                BottomNavDest.HOME -> {
                                                                    navController.navigate(MikaRoute.Home) {
                                                                        popUpTo<MikaRoute.Home> { inclusive = false }
                                                                        launchSingleTop = true
                                                                    }
                                                                }
                                                                BottomNavDest.FOLDERS -> {
                                                                    if (!isOnFoldersRoute) {
                                                                        navController.navigate(MikaRoute.Folders) {
                                                                            popUpTo<MikaRoute.Home> { inclusive = false }
                                                                            launchSingleTop = true
                                                                        }
                                                                    }
                                                                }
                                                                BottomNavDest.LIBRARY -> {
                                                                    if (!isOnLibraryRoute) {
                                                                        navController.navigate(MikaRoute.Library) {
                                                                            popUpTo<MikaRoute.Home> { inclusive = false }
                                                                            launchSingleTop = true
                                                                        }
                                                                    }
                                                                }
                                                                BottomNavDest.VIDEOS -> {
                                                                    if (!isOnVideosRoute) {
                                                                        navController.navigate(MikaRoute.Videos) {
                                                                            popUpTo<MikaRoute.Home> { inclusive = false }
                                                                            launchSingleTop = true
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        },
                                                        onLongClick = { showBottomNavCustomizer = true }
                                                    )
                                                    .testTag("bottom_nav_${dest.name.lowercase()}")
                                            ) {
                                                Icon(
                                                    imageVector = iconVector,
                                                    contentDescription = dest.label,
                                                    tint = itemTint,
                                                    modifier = Modifier.size(mikaScaledIcon(21.dp))
                                                )
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    text = dest.label,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                                    ),
                                                    color = itemTint,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Floating PiP Video Window overlay
                    if (!isOnLyricsRoute &&
                        videoState.currentVideo != null &&
                        !videoState.isFullScreenOpen &&
                        !isPlayerMostlyExpanded
                    ) {
                        FloatingVideoPipOverlay(
                            videoState = videoState,
                            videoAspectRatio = videoAspectRatio,
                            bottomNavVisible = showBottomNav,
                            onAttachSurface = viewModel.videoPlaybackController::attachSurfaceView,
                            onDetachSurface = viewModel.videoPlaybackController::detachSurfaceView,
                            onExpand = { viewModel.videoPlaybackController.setFullScreen(true) },
                            onTogglePlayPause = viewModel.videoPlaybackController::togglePlayPause,
                            onDismiss = viewModel.videoPlaybackController::stopAndDismiss,
                            modifier = Modifier.zIndex(15f)
                        )
                    }
                }
            }
            }

            // Full-Screen Player Overlay with continuous 1:1 finger-drag expansion/minimize morph
            if (playbackState.currentSong != null && showFullPlayer) {
                val currentFavorite = playbackState.currentSong?.let { song ->
                    song.id in favoriteSongIds || song.isFavorite
                } == true
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationY = (1f - playerExpansion.value) * maxHeightPx
                            alpha = (playerExpansion.value * 5f).coerceIn(0f, 1f)
                        }
                ) {
                    FullPlayerScreen(
                        playbackState = playbackState,
                        isFavorite = currentFavorite,
                        isEqualizerEnabled = equalizerState.enabled,
                        progressFlow = viewModel.playbackProgress,
                        lyrics = currentLyrics,
                        artworkAccentPair = artworkColors,
                        sleepTimerState = sleepTimerState,
                        skipIntervalSec = settings.skipIntervalSec,
                        trackWaveform = trackWaveform,
                        liveWaveformHistory = liveWaveformHistory,
                        onMinimize = { minimizeFullPlayer() },
                        onDragExpansionDelta = onDragExpansionDelta,
                        onDragExpansionEnd = onDragExpansionEnd,
                        onTogglePlayPause = viewModel.playbackController::togglePlayPause,
                        onSkipNext = viewModel.playbackController::skipNext,
                        onSkipPrevious = viewModel.playbackController::skipPrevious,
                        onSeekTo = viewModel.playbackController::seekTo,
                        onSeekRelative = viewModel.playbackController::seekRelative,
                        onToggleShuffle = viewModel.playbackController::toggleShuffle,
                        onCycleRepeat = viewModel.playbackController::cycleRepeatMode,
                        onToggleFavorite = viewModel::toggleFavorite,
                        onRateAndMood = { rateMoodSong = it },
                        onSaveArtwork = viewModel::saveArtworkToMikaFolder,
                        onInspectArtworkDetails = viewModel::inspectArtworkDetails,
                        onOpenSpeedPitch = { showSpeedPitchSheet = true },
                        onOpenEqualizer = { showEqualizerSheet = true },
                        onOpenQueue = { showQueueSheet = true },
                        onOpenSleepTimer = { showSleepTimerSheet = true },
                        onOpenFullLyrics = {
                            minimizeFullPlayer()
                            navController.navigate(MikaRoute.Lyrics)
                        },
                        onImportLyrics = { importLyricsLauncher.launch(arrayOf("*/*")) },
                        onEditTags = { tagEditorSongs = listOf(it) },
                        onShowSongInfo = { infoSong = it },
                        onMoreClick = { actionSong = it },
                        settings = settings,
                        onOpenPlayerBackground = { showPlayerBackgroundDialog = true },
                        onShareSong = { viewModel.shareSong(context, it) },
                        onOpenSoundControl = { showSoundControlSheet = true },
                        onUpdateFullPlayerConfig = viewModel::saveFullPlayerConfig
                    )
                }
            }

            // Full-Screen Video Player Overlay
            if (videoState.currentVideo != null && videoState.isFullScreenOpen) {
                FullVideoPlayerScreen(
                    videoState = videoState,
                    videoAspectRatio = videoAspectRatio,
                    onMinimize = { viewModel.videoPlaybackController.setFullScreen(false) },
                    onAttachSurface = viewModel.videoPlaybackController::attachSurfaceView,
                    onDetachSurface = viewModel.videoPlaybackController::detachSurfaceView,
                    onTogglePlayPause = viewModel.videoPlaybackController::togglePlayPause,
                    onSeekTo = viewModel.videoPlaybackController::seekTo,
                    onSeekRelative = viewModel.videoPlaybackController::seekRelative,
                    onSkipNext = viewModel.videoPlaybackController::skipNext,
                    onSkipPrevious = viewModel.videoPlaybackController::skipPrevious,
                    onSetSpeed = viewModel.videoPlaybackController::setPlaybackSpeed,
                    onCycleResizeMode = viewModel.videoPlaybackController::cycleResizeMode,
                    onToggleLock = viewModel.videoPlaybackController::toggleControlsLocked,
                    onToggleMute = viewModel.videoPlaybackController::toggleMute,
                    onToggleLoop = viewModel.videoPlaybackController::toggleLoop,
                    modifier = Modifier.zIndex(20f)
                )
            }

            // Shared Bottom Sheets & Per-Tab Sort Pickers
            when (activeSortSheet) {
                ActiveSortSheet.SONGS -> {
                    SortBottomSheet(
                        currentField = settings.sortField,
                        currentDirection = settings.sortDirection,
                        onSelect = viewModel::setSort,
                        onDismiss = { activeSortSheet = null }
                    )
                }

                ActiveSortSheet.PLAYLISTS -> {
                    TypedSortBottomSheet(
                        title = "Sort Playlists",
                        fields = PlaylistSortField.entries,
                        currentField = settings.playlistSortField,
                        currentDirection = settings.playlistSortDirection,
                        labelProvider = { it.label },
                        onSelect = viewModel::setPlaylistSort,
                        onDismiss = { activeSortSheet = null }
                    )
                }

                ActiveSortSheet.FOLDERS -> {
                    TypedSortBottomSheet(
                        title = "Sort Folders",
                        fields = FolderSortField.entries,
                        currentField = settings.folderListSortField,
                        currentDirection = settings.folderListSortDirection,
                        labelProvider = { it.label },
                        onSelect = viewModel::setFolderListSort,
                        onDismiss = { activeSortSheet = null }
                    )
                }

                ActiveSortSheet.ALBUMS -> {
                    TypedSortBottomSheet(
                        title = "Sort Albums",
                        fields = AlbumSortField.entries,
                        currentField = settings.albumSortField,
                        currentDirection = settings.albumSortDirection,
                        labelProvider = { it.label },
                        onSelect = viewModel::setAlbumSort,
                        onDismiss = { activeSortSheet = null }
                    )
                }

                ActiveSortSheet.ARTISTS -> {
                    TypedSortBottomSheet(
                        title = "Sort Artists",
                        fields = ArtistSortField.entries,
                        currentField = settings.artistSortField,
                        currentDirection = settings.artistSortDirection,
                        labelProvider = { it.label },
                        onSelect = viewModel::setArtistSort,
                        onDismiss = { activeSortSheet = null }
                    )
                }

                ActiveSortSheet.GENRES -> {
                    TypedSortBottomSheet(
                        title = "Sort Genres",
                        fields = GenreSortField.entries,
                        currentField = genreSortField,
                        currentDirection = genreSortDirection,
                        labelProvider = { it.label },
                        onSelect = viewModel::setGenreSort,
                        onDismiss = { activeSortSheet = null }
                    )
                }

                null -> Unit
            }

            actionSong?.let { song ->
                val liveFavorite = song.id in favoriteSongIds ||
                    rawSongs.firstOrNull { it.id == song.id }?.isFavorite == true
                val resolvedSong = song.copy(isFavorite = liveFavorite)
                SongActionsSheet(
                    song = resolvedSong,
                    onDismiss = { actionSong = null },
                    onPlayNow = { viewModel.playSong(resolvedSong, allSongs) },
                    onPlayNext = { viewModel.playNext(resolvedSong) },
                    onAddToQueue = { viewModel.addToQueue(listOf(resolvedSong)) },
                    onToggleFavorite = { viewModel.toggleFavorite(resolvedSong.id) },
                    onAddToPlaylist = { playlistSongIdsToAdd = listOf(resolvedSong.id) },
                    onEditTags = { tagEditorSongs = listOf(resolvedSong) },
                    onShowInfo = { infoSong = resolvedSong },
                    onShare = { viewModel.shareSong(context, resolvedSong) },
                    onMoveToTrash = { viewModel.moveSongToTrash(resolvedSong) },
                    onDeletePermanently = { triggerPermanentDelete(listOf(resolvedSong)) },
                    onHideSong = { viewModel.hideSongs(listOf(resolvedSong.id)) },
                    onRateAndMood = { rateMoodSong = resolvedSong }
                )
            }

            infoSong?.let { song ->
                SongInfoDialog(
                    song = song,
                    onDismiss = { infoSong = null }
                )
            }

            rateMoodSong?.let { target ->
                val liveSong = rawSongs.firstOrNull { it.id == target.id } ?: target
                val isFav = liveSong.id in favoriteSongIds || liveSong.isFavorite
                RateAndMoodDialog(
                    titleLabel = "${liveSong.title} · ${liveSong.artist}",
                    initialRating = liveSong.rating,
                    initialMood = liveSong.mood,
                    isFavorite = isFav,
                    onToggleFavorite = { viewModel.toggleFavorite(liveSong.id) },
                    onSave = { rating, mood ->
                        viewModel.setSongRating(liveSong.id, rating)
                        viewModel.setSongMood(liveSong.id, mood)
                    },
                    onDismiss = { rateMoodSong = null }
                )
            }

            tagEditorSongs?.let { targets ->
                TagEditorSheet(
                    songs = targets,
                    onSave = { title, artist, album, genre, year, trackNumber, artworkUri, clearArtwork ->
                        viewModel.updateSongTagsAndArtwork(
                            songIds = targets.map { it.id },
                            title = title,
                            artist = artist,
                            album = album,
                            genre = genre,
                            year = year,
                            trackNumber = trackNumber,
                            artworkUri = artworkUri,
                            clearArtwork = clearArtwork
                        )
                    },
                    onDismiss = { tagEditorSongs = null }
                )
            }

            playlistSongIdsToAdd?.let { ids ->
                AddToPlaylistSheet(
                    playlists = playlists,
                    songIdsToAdd = ids,
                    onSelectPlaylist = { plId -> viewModel.addSongsToPlaylist(plId, ids) },
                    onCreateNewPlaylist = { name -> viewModel.createPlaylist(name, ids) },
                    onDismiss = { playlistSongIdsToAdd = null }
                )
            }

            if (showQueueSheet) {
                QueueSheet(
                    playbackState = playbackState,
                    onSelectIndex = viewModel.playbackController::skipToQueueIndex,
                    onRemoveIndex = viewModel.playbackController::removeFromQueue,
                    onMoveItem = viewModel.playbackController::moveQueueItem,
                    onClearQueue = viewModel.playbackController::clearQueue,
                    onDismiss = { showQueueSheet = false },
                    allSongs = allSongs,
                    onTogglePlayPause = viewModel.playbackController::togglePlayPause,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onToggleShuffle = viewModel.playbackController::toggleShuffle,
                    onAddSongsToQueue = { songs -> viewModel.addToQueue(songs) },
                    onOpenEqualizer = { showEqualizerSheet = true }
                )
            }

            if (showEqualizerSheet) {
                EqualizerSheet(
                    state = equalizerState,
                    onToggleEnabled = viewModel::setEqEnabled,
                    onSelectPreset = viewModel::applyEqPreset,
                    onBandChange = viewModel::updateEqBand,
                    onBassBoostChange = viewModel::updateBassBoost,
                    onVirtualizerChange = viewModel::updateVirtualizer,
                    onReset = viewModel::resetEqualizer,
                    onDismiss = { showEqualizerSheet = false }
                )
            }

            if (showSleepTimerSheet) {
                SleepTimerSheet(
                    sleepTimerState = sleepTimerState,
                    onStartTimer = viewModel.playbackController::startSleepTimer,
                    onExtendTimer = viewModel.playbackController::extendSleepTimer,
                    onSetEndOfTrack = viewModel.playbackController::setSleepAtEndOfTrack,
                    onToggleFadeOut = viewModel.playbackController::setSleepTimerFadeOut,
                    onCancelTimer = viewModel.playbackController::cancelSleepTimer,
                    onDismiss = { showSleepTimerSheet = false }
                )
            }

            if (showSpeedPitchSheet) {
                SpeedAndPitchSheet(
                    currentSpeed = playbackState.playbackSpeed,
                    currentPitch = playbackState.playbackPitch,
                    onApply = viewModel::setPlaybackSpeedAndPitch,
                    onDismiss = { showSpeedPitchSheet = false }
                )
            }

            if (showSoundControlSheet) {
                SoundControlSheet(
                    currentSpeed = playbackState.playbackSpeed,
                    skipSilence = settings.skipSilence,
                    soundControl = settings.soundControl,
                    equalizerState = equalizerState,
                    onUpdateSoundControl = viewModel::setSoundControl,
                    onToggleSkipSilence = viewModel::setSkipSilence,
                    onEqToggle = viewModel::setEqEnabled,
                    onEqSelectPreset = viewModel::applyEqPreset,
                    onEqBandChange = viewModel::updateEqBand,
                    onEqBassBoostChange = viewModel::updateBassBoost,
                    onEqVirtualizerChange = viewModel::updateVirtualizer,
                    onEqReset = viewModel::resetEqualizer,
                    onDismiss = { showSoundControlSheet = false }
                )
            }

            if (showPlayerBackgroundDialog) {
                PlayerBackgroundDialog(
                    settings = settings,
                    onSelectMode = { mode -> viewModel.setPlayerBackground(mode = mode) },
                    onPickCustomImage = {
                        pickCustomBgImageLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onToggleVignette = { viewModel.setPlayerBackground(vignette = it) },
                    onToggleAnimated = { viewModel.setPlayerBackground(animated = it) },
                    onToggleColorWhenExpanded = { viewModel.setPlayerBackground(colorWhenExpanded = it) },
                    onDismiss = { showPlayerBackgroundDialog = false }
                )
            }

            if (showBottomNavCustomizer) {
                BottomNavCustomizationSheet(
                    settings = settings,
                    onToggleShowBar = viewModel::setShowBottomNavBar,
                    onMoveDest = viewModel::moveBottomNavDest,
                    onReorderDests = viewModel::reorderBottomNavDest,
                    onToggleDestHidden = viewModel::toggleBottomNavDestHidden,
                    onDismiss = { showBottomNavCustomizer = false }
                )
            }

            // Top-aligned, swipe-dismissible notification banner (never covers MiniPlayer)
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 8.dp, start = 14.dp, end = 14.dp)
                    .zIndex(30f)
                    .testTag("top_notification_host")
            ) { data ->
                MikaTopNotificationBanner(data = data)
            }

            // Smooth startup splash overlay (~650ms)
            AnimatedVisibility(
                visible = showSplashOverlay,
                enter = fadeIn(tween(120)),
                exit = fadeOut(tween(320)),
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(40f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shadowElevation = 14.dp,
                            modifier = Modifier
                                .size(92.dp)
                                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.mika_icon_fg),
                                contentDescription = "Mika Player",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(14.dp)
                            )
                        }
                        Text(
                            text = "Mika Player",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = BrandDisplayFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "v${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MikaTopNotificationBanner(data: SnackbarData) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val dismissThresholdPx = with(density) { 42.dp.toPx() }

    key(data) {
        val verticalOffset = remember { Animatable(0f) }
        val horizontalDismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                if (value != SwipeToDismissBoxValue.Settled) {
                    data.dismiss()
                    true
                } else {
                    false
                }
            }
        )

        SwipeToDismissBox(
            state = horizontalDismissState,
            enableDismissFromStartToEnd = true,
            enableDismissFromEndToStart = true,
            backgroundContent = {},
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationY = verticalOffset.value
                    val fadeProgress = ((-verticalOffset.value) / (dismissThresholdPx * 2f)).coerceIn(0f, 0.65f)
                    alpha = 1f - fadeProgress
                }
                .pointerInput(data) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { _, dragAmount ->
                            val next = (verticalOffset.value + dragAmount).coerceAtMost(0f)
                            scope.launch { verticalOffset.snapTo(next) }
                        },
                        onDragEnd = {
                            if (verticalOffset.value < -dismissThresholdPx) {
                                data.dismiss()
                            } else {
                                scope.launch {
                                    verticalOffset.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch { verticalOffset.animateTo(0f) }
                        }
                    )
                }
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 12.dp,
                tonalElevation = 6.dp,
                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("top_notification_banner")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = MikaIcons.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = data.visuals.message,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        data.visuals.actionLabel?.let { label ->
                            TextButton(onClick = { data.performAction() }) {
                                Text(label, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(38.dp)
                            .height(3.5.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                    )
                }
            }
        }
    }
}

