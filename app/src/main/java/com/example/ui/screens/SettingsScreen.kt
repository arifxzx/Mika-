package com.example.ui.screens

import android.graphics.Color as AndroidColor
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.R
import com.example.data.repository.MusicRepository
import com.example.media.MediaScanner
import com.example.model.AccentPalette
import com.example.model.AppIconPreset
import com.example.model.AppSettings
import com.example.model.ArtworkGestureAction
import com.example.model.BackgroundEffect
import com.example.model.BottomNavDest
import com.example.model.CrossfadeStyle
import com.example.model.GradientStyle
import com.example.model.MainTab
import com.example.model.PlaybackIconStyle
import com.example.model.SeekBarStyle
import com.example.model.Song
import com.example.model.TabLongPressAction
import com.example.model.TabTapAction
import com.example.model.ThemeMode
import com.example.model.TrackSwipeAction
import com.example.ui.components.MikaGlassSurface
import com.example.ui.components.MikaSeekBar
import com.example.ui.components.MikaVerticalFastScroller
import com.example.ui.icons.MikaIcons
import com.example.ui.theme.BrandDisplayFontFamily
import com.example.ui.theme.LocalMikaColors
import com.example.ui.theme.mikaScaledIcon
import com.example.util.Formatters
import java.util.Locale
import kotlin.math.abs

private enum class SettingsCategory(
    val title: String,
    val subtitle: String
) {
    APPEARANCE("Appearance", "Theme, colors, text size & gradients"),
    CUSTOMIZATION("Customization", "App icons, seek bars, controls & swipe gestures"),
    NAVIGATION("Navigation & Tabs", "Bottom bar, visible tabs & tap actions"),
    PLAYBACK("Playback & Audio", "Gapless, crossfade, mixing & skip step"),
    LIBRARY("Library & Filters", "Rescan, folders, blacklist & duplicates"),
    STORAGE("Storage & History", "Cache management & history")
}

private data class SettingsSearchEntry(
    val title: String,
    val subtitle: String,
    val keywords: String,
    val icon: ImageVector,
    val category: SettingsCategory? = null,
    val actionKey: String = ""
)

private data class FontScalePreset(
    val label: String,
    val scale: Float
)

private val fontScalePresets = listOf(
    FontScalePreset("Compact", 0.90f),
    FontScalePreset("Normal", 1.00f),
    FontScalePreset("Large", 1.12f),
    FontScalePreset("Extra Large", 1.22f)
)

private enum class ConfirmClearAction {
    ARTWORK_CACHE,
    ALL_APP_CACHE,
    PLAYBACK_HISTORY,
    SEARCH_HISTORY,
    RESET_SETTINGS,
    CLEAR_ALL_DATA
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    cacheSizeBytes: Long,
    isScanning: Boolean,
    songCount: Int,
    trashedCount: Int,
    availableFolders: List<MediaScanner.DeviceFolderSummary>,
    duplicateGroups: List<MusicRepository.DuplicateTrackGroup> = emptyList(),
    isScanningDuplicates: Boolean = false,
    onBack: () -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetAccentColor: (String) -> Unit,
    onSetCustomAccentColor: (Long) -> Unit,
    onSetAutoColoring: (Boolean) -> Unit,
    onSetApplyColorOnApp: (Boolean) -> Unit = {},
    onSetApplyColorOnPlayer: (Boolean) -> Unit = {},
    onSetFontScale: (Float) -> Unit,
    onSetCornerRadiusDp: (Int) -> Unit = {},
    onSetGradientStyle: (GradientStyle) -> Unit,
    onSetGradientTargets: (Boolean, Boolean, Boolean) -> Unit,
    onToggleTabEnabled: (MainTab) -> Unit,
    onMoveTab: (MainTab, Int) -> Unit,
    onSetDefaultTab: (MainTab) -> Unit,
    onSetTabActions: (TabTapAction?, TabLongPressAction?) -> Unit = { _, _ -> },
    onRefreshFolders: () -> Unit,
    onToggleExcludedFolder: (String) -> Unit,
    onClearExcludedFolders: () -> Unit,
    onSetMinDurationSec: (Int) -> Unit,
    onSetMinFileSizeKb: (Int) -> Unit,
    onRescanLibrary: () -> Unit,
    onSetShowLyricsBadges: (Boolean) -> Unit,
    onSetObeyNoMedia: (Boolean) -> Unit = {},
    onSetAutoScanOnStartup: (Boolean) -> Unit = {},
    onUpdateExtensionBlacklist: (Set<String>) -> Unit = {},
    onSetGroupArtworkByAlbum: (Boolean) -> Unit = {},
    onScanDuplicates: () -> Unit = {},
    onDeleteDuplicateSong: (Song, Boolean) -> Unit = { _, _ -> },
    onSetGapless: (Boolean) -> Unit,
    onSetCrossfade: (Int) -> Unit,
    onSetSkipInterval: (Int) -> Unit,
    onSetRememberLastPosition: (Boolean) -> Unit,
    onSetAllowSimultaneousPlayback: (Boolean) -> Unit,
    onSetShowBottomNavBar: (Boolean) -> Unit = {},
    onClearArtworkCache: () -> Unit,
    onClearAllAppCaches: () -> Unit = {},
    onClearPlaybackHistory: () -> Unit,
    onClearSearchHistory: () -> Unit,
    onOpenStatistics: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenAbout: () -> Unit,
    onAddFolder: () -> Unit = {},
    onOpenWidgetSettings: () -> Unit = {},
    onOpenPlayerBackground: () -> Unit = {},
    onOpenPlayerStyleStudio: () -> Unit = {},
    onSetPickColorsFromWallpaper: (Boolean) -> Unit = {},
    onSetForceMiniPlayerTrackColors: (Boolean) -> Unit = {},
    onSetUsePitchBlack: (Boolean) -> Unit = {},
    onSetCustomDarkAccentColor: (Long) -> Unit = {},
    onSetSkipSilence: (Boolean) -> Unit = {},
    onSetCrossfadeEnabled: (Boolean) -> Unit = {},
    onSetCrossfadeDurationMs: (Int) -> Unit = {},
    onSetCrossfadeTriggerLastSec: (Int) -> Unit = {},
    onSetCrossfadeStyle: (CrossfadeStyle) -> Unit = {},
    onSetPlayPauseFadeEnabled: (Boolean) -> Unit = {},
    onSetPlayFadeDurationMs: (Int) -> Unit = {},
    onSetPauseFadeDurationMs: (Int) -> Unit = {},
    onSetJumpToFirstAfterQueue: (Boolean) -> Unit = {},
    onSetPreviousButtonReplays: (Boolean) -> Unit = {},
    onSetRestorePositionMinDurationMin: (Int) -> Unit = {},
    onSetVisualEffects: (Boolean?, Boolean?, Boolean?, BackgroundEffect?) -> Unit = { _, _, _, _ -> },
    onSetArtworkGestures: (Boolean?, ArtworkGestureAction?, ArtworkGestureAction?) -> Unit = { _, _, _ -> },
    onResetArtworkGestures: () -> Unit = {},
    onMoveBottomNavDest: (BottomNavDest, Int) -> Unit = { _, _ -> },
    onToggleBottomNavDestHidden: (BottomNavDest) -> Unit = {},
    onSetAutoScan: (Boolean?, Boolean?) -> Unit = { _, _ -> },
    onSetAppIconScale: (Float) -> Unit = {},
    onSetAppIconPreset: (AppIconPreset) -> Unit = {},
    onPickCustomAppIcon: () -> Unit = {},
    onSetSeekBarStyle: (SeekBarStyle) -> Unit = {},
    onSetPlaybackIconStyle: (PlaybackIconStyle) -> Unit = {},
    onSetShowActualArtworkRatio: (Boolean) -> Unit = {},
    onSetShowLikeButtonInList: (Boolean) -> Unit = {},
    onSetTrackSwipeActions: (TrackSwipeAction?, TrackSwipeAction?) -> Unit = { _, _ -> },
    onSetSwipeToOpenDrawer: (Boolean) -> Unit = {},
    onResetAllSettings: () -> Unit = {},
    onClearAllAppData: () -> Unit = {}
) {
    var selectedCategory by remember { mutableStateOf<SettingsCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showExcludedFoldersSheet by remember { mutableStateOf(false) }
    var showCustomColorPicker by remember(settings.accentColorId) {
        mutableStateOf(settings.accentColorId == "custom")
    }
    var pendingClearAction by remember { mutableStateOf<ConfirmClearAction?>(null) }
    var activeDefaultColorTarget by remember { mutableStateOf<Int?>(null) } // 1 = Light, 2 = Dark
    val accent = MaterialTheme.colorScheme.primary

    if (activeDefaultColorTarget != null) {
        val isDarkTarget = activeDefaultColorTarget == 2
        SimpleHueColorPickerDialog(
            title = if (isDarkTarget) "Default Color (Dark)" else "Default Color",
            initialArgb = if (isDarkTarget) settings.customDarkAccentArgb else settings.customAccentArgb,
            onSelectArgb = { argb ->
                if (isDarkTarget) onSetCustomDarkAccentColor(argb) else onSetCustomAccentColor(argb)
            },
            onDismiss = { activeDefaultColorTarget = null }
        )
    }

    BackHandler(enabled = selectedCategory != null || searchQuery.isNotEmpty()) {
        if (selectedCategory != null) {
            selectedCategory = null
        } else {
            searchQuery = ""
        }
    }

    pendingClearAction?.let { action ->
        val (dialogTitle, dialogBody, confirmLabel) = when (action) {
            ConfirmClearAction.ARTWORK_CACHE -> Triple(
                "Clear Artwork Cache?",
                "Cached individual track covers will be removed and re-extracted automatically as needed.",
                "Clear"
            )
            ConfirmClearAction.ALL_APP_CACHE -> Triple(
                "Clear All App Cache?",
                "Removes cached artwork, waveforms, video thumbnails, and image cache (${Formatters.formatFileSize(cacheSizeBytes)}).",
                "Clear Cache"
            )
            ConfirmClearAction.PLAYBACK_HISTORY -> Triple(
                "Clear Playback History?",
                "All recently played timestamps and song play counts will be reset to zero. Your tracks and playlists remain untouched.",
                "Clear"
            )
            ConfirmClearAction.SEARCH_HISTORY -> Triple(
                "Clear Search History?",
                "All saved recent search queries will be permanently deleted.",
                "Clear"
            )
            ConfirmClearAction.RESET_SETTINGS -> Triple(
                "Reset All Settings?",
                "Appearance, player styles, playback options, and tab layouts will be restored to defaults. Your library and playlists stay safe.",
                "Reset"
            )
            ConfirmClearAction.CLEAR_ALL_DATA -> Triple(
                "Clear All App Data?",
                "Playlists, favorites, playback history, custom tags, lyrics overrides, and settings will be wiped. Audio files on storage are NOT deleted.",
                "Clear All"
            )
        }
        AlertDialog(
            onDismissRequest = { pendingClearAction = null },
            title = { Text(dialogTitle, fontWeight = FontWeight.Bold) },
            text = { Text(dialogBody) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingClearAction = null
                        when (action) {
                            ConfirmClearAction.ARTWORK_CACHE -> onClearArtworkCache()
                            ConfirmClearAction.ALL_APP_CACHE -> onClearAllAppCaches()
                            ConfirmClearAction.PLAYBACK_HISTORY -> onClearPlaybackHistory()
                            ConfirmClearAction.SEARCH_HISTORY -> onClearSearchHistory()
                            ConfirmClearAction.RESET_SETTINGS -> onResetAllSettings()
                            ConfirmClearAction.CLEAR_ALL_DATA -> onClearAllAppData()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_clear_action_button")
                ) {
                    Text(confirmLabel, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingClearAction = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showExcludedFoldersSheet) {
        ExcludedFoldersSheet(
            availableFolders = availableFolders,
            excludedFolders = settings.excludedFolders,
            onToggleFolder = onToggleExcludedFolder,
            onClearAll = onClearExcludedFolders,
            onDismiss = { showExcludedFoldersSheet = false }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    val activeCat = selectedCategory
                    if (activeCat != null) {
                        Text(
                            text = activeCat.title,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = BrandDisplayFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Settings",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = BrandDisplayFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                ),
                                maxLines = 1,
                                softWrap = false
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = BorderStroke(
                                    0.75.dp,
                                    if (searchQuery.isNotEmpty()) accent.copy(alpha = 0.65f)
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = MikaIcons.Search,
                                        contentDescription = "Search settings",
                                        tint = if (searchQuery.isNotEmpty()) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(mikaScaledIcon(16.dp))
                                    )
                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                text = "Search...",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        BasicTextField(
                                            value = searchQuery,
                                            onValueChange = { searchQuery = it },
                                            singleLine = true,
                                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            ),
                                            cursorBrush = SolidColor(accent),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("settings_search_input")
                                        )
                                    }
                                    if (searchQuery.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .clickable { searchQuery = "" },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = MikaIcons.Close,
                                                contentDescription = "Clear search",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            when {
                                selectedCategory != null -> selectedCategory = null
                                searchQuery.isNotEmpty() -> searchQuery = ""
                                else -> onBack()
                            }
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = MikaIcons.ArrowLeft,
                            contentDescription = "Back",
                            modifier = Modifier.size(mikaScaledIcon(24.dp))
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        AnimatedContent(
            targetState = selectedCategory,
            transitionSpec = {
                if (targetState != null) {
                    (slideInHorizontally(tween(200)) { it / 4 } + fadeIn(tween(180)))
                        .togetherWith(slideOutHorizontally(tween(180)) { -it / 6 } + fadeOut(tween(150)))
                } else {
                    (slideInHorizontally(tween(200)) { -it / 4 } + fadeIn(tween(180)))
                        .togetherWith(slideOutHorizontally(tween(180)) { it / 6 } + fadeOut(tween(150)))
                }
            },
            label = "settings_navigation",
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("settings_screen")
        ) { activeCategory ->
            if (activeCategory == null) {
                val hubListState = rememberLazyListState()
                val searchableEntries = remember {
                    listOf(
                        SettingsSearchEntry("Theme Mode", "Dark, Light or System", "theme dark light system mode pitch black", MikaIcons.Palette, SettingsCategory.APPEARANCE),
                        SettingsSearchEntry("Auto Coloring & Color Targets", "Match artwork and apply to app or player", "auto coloring palette artwork tint apply app player", MikaIcons.Palette, SettingsCategory.APPEARANCE),
                        SettingsSearchEntry("Accent Color", "Preset or custom color picker", "accent color custom picker", MikaIcons.Palette, SettingsCategory.APPEARANCE),
                        SettingsSearchEntry("Font Size", "Scale UI text", "font size text scale typography", MikaIcons.Lyrics, SettingsCategory.APPEARANCE),
                        SettingsSearchEntry("Gradients", "Linear, radial & aurora styles", "gradient surface player style", MikaIcons.Palette, SettingsCategory.APPEARANCE),
                        SettingsSearchEntry("Launcher App Icon", "Switch presets or pick icon from storage", "app icon preset custom aurora violet mono neon", MikaIcons.MagicPen, SettingsCategory.CUSTOMIZATION),
                        SettingsSearchEntry("App Icon Size", "Scale icons across bars & controls", "icon size scale compact large customization", MikaIcons.MagicPen, SettingsCategory.CUSTOMIZATION),
                        SettingsSearchEntry("Corner Border Radius", "Adjust card & artwork corner rounding", "corner border radius shape rounded sharp pill", MikaIcons.Brush, SettingsCategory.CUSTOMIZATION),
                        SettingsSearchEntry("Playback Control Icons", "Normal, broken, outline, capsule & minimal", "playback icon style normal broken outline capsule play pause", MikaIcons.PlayLinear, SettingsCategory.CUSTOMIZATION),
                        SettingsSearchEntry("Seek Bar Style", "Capsule, waveform, live flow, wavy or ruler", "seek bar slider style waveform live flow wavy pulse tick ruler", MikaIcons.SliderHorizontal, SettingsCategory.CUSTOMIZATION),
                        SettingsSearchEntry("Lists, Like Button & Swipe Actions", "Swipe left/right actions, like button, actual art, drawer swipe", "swipe left right action like button actual artwork drawer", MikaIcons.SwapHorizontal, SettingsCategory.CUSTOMIZATION),
                        SettingsSearchEntry("Bottom Navigation Bar", "Reorder & visibility for Home, Folders, Library & Videos", "bottom navigation bar nav video home folders", MikaIcons.Category, SettingsCategory.NAVIGATION),
                        SettingsSearchEntry("Visible Tabs & Actions", "Reorder tabs, startup tab, tap & long-press actions", "tabs order visible hide startup tap long press", MikaIcons.Category, SettingsCategory.NAVIGATION),
                        SettingsSearchEntry("Gapless & Crossfade", "Fade curve, play/pause fade & seek step", "gapless crossfade fade play pause skip interval silence remember", MikaIcons.Equalizer, SettingsCategory.PLAYBACK),
                        SettingsSearchEntry("Folders, Auto-Scan & Blacklist", "Excluded folders, .nomedia, album artwork & extension blacklist", "exclude folder scan rescan startup nomedia blacklist album group", MikaIcons.Folder, SettingsCategory.LIBRARY),
                        SettingsSearchEntry("Duplicate File Detector", "Find and remove duplicate tracks", "duplicate file detector clean storage", MikaIcons.Copy, SettingsCategory.LIBRARY),
                        SettingsSearchEntry("Cache & History", "Clear all app cache, artwork cache or history", "cache clear all artwork history search playback storage reset", MikaIcons.Cleaning, SettingsCategory.STORAGE),
                        SettingsSearchEntry("Home Screen Widget", "Customize home screen widget & preview", "widget home screen style controls preview", MikaIcons.HomeLinear, actionKey = "widget"),
                        SettingsSearchEntry("Player Style Studio", "Customize Mini Player & Full Player styles", "mini player full player style visualizer vinyl classic", MikaIcons.StyleTune, actionKey = "player_style"),
                        SettingsSearchEntry("Player Background", "Mesh gradient, ambient lights, bokeh or photo", "player background mesh bokeh vignette custom photo", MikaIcons.Palette, actionKey = "player_bg"),
                        SettingsSearchEntry("Visual Effects & Gestures", "Blur, glow, parallax, seasonal effects & artwork gestures", "blur glow parallax snow sakura artwork tap gesture customization", MikaIcons.Star, SettingsCategory.CUSTOMIZATION),
                        SettingsSearchEntry("Backup & Restore", "ZIP backups in /mika/backup", "backup restore export import location zip", MikaIcons.BackupUpload, actionKey = "backup"),
                        SettingsSearchEntry("Statistics", "Listening insights & breakdown", "statistics stats top tracks", MikaIcons.Chart, actionKey = "stats"),
                        SettingsSearchEntry("Trash Bin", "Recover or delete tracks", "trash bin delete restore", MikaIcons.Trash, actionKey = "trash"),
                        SettingsSearchEntry("About", "Developer, socials, donate & version", "about developer sagor islam facebook telegram donate", MikaIcons.InfoCircle, actionKey = "about")
                    )
                }
                val filteredSearch = remember(searchQuery, searchableEntries) {
                    val q = searchQuery.trim().lowercase()
                    if (q.isEmpty()) emptyList()
                    else searchableEntries.filter {
                        it.title.lowercase().contains(q) ||
                            it.subtitle.lowercase().contains(q) ||
                            it.keywords.lowercase().contains(q)
                    }
                }
                MikaVerticalFastScroller(
                    listState = hubListState,
                    totalItems = if (searchQuery.isNotBlank()) filteredSearch.size + 1 else 10,
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        state = hubListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("settings_hub_list"),
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {

                        if (searchQuery.isNotBlank()) {
                            if (filteredSearch.isEmpty()) {
                                item {
                                    Text(
                                        text = "No matching settings",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)
                                    )
                                }
                            } else {
                                items(filteredSearch, key = { it.title }) { entry ->
                                    SettingsHubRow(
                                        icon = entry.icon,
                                        title = entry.title,
                                        subtitle = entry.subtitle,
                                        onClick = {
                                            searchQuery = ""
                                            when {
                                                entry.category != null -> selectedCategory = entry.category
                                                entry.actionKey == "widget" -> onOpenWidgetSettings()
                                                entry.actionKey == "player_style" -> onOpenPlayerStyleStudio()
                                                entry.actionKey == "player_bg" -> onOpenPlayerBackground()
                                                entry.actionKey == "backup" -> onOpenBackup()
                                                entry.actionKey == "stats" -> onOpenStatistics()
                                                entry.actionKey == "trash" -> onOpenTrash()
                                                entry.actionKey == "about" -> onOpenAbout()
                                            }
                                        },
                                        testTag = "settings_search_result_${entry.title.lowercase().replace(" ", "_")}"
                                    )
                                }
                            }
                        } else {
                            item {
                                SettingsHubRow(
                                    icon = MikaIcons.Palette,
                                    title = SettingsCategory.APPEARANCE.title,
                                    onClick = { selectedCategory = SettingsCategory.APPEARANCE },
                                    testTag = "settings_cat_appearance"
                                )
                            }
                            item {
                                SettingsHubRow(
                                    icon = MikaIcons.Brush,
                                    title = SettingsCategory.CUSTOMIZATION.title,
                                    onClick = { selectedCategory = SettingsCategory.CUSTOMIZATION },
                                    testTag = "settings_cat_customization"
                                )
                            }
                            item {
                                SettingsHubRow(
                                    icon = MikaIcons.Category,
                                    title = SettingsCategory.NAVIGATION.title,
                                    onClick = { selectedCategory = SettingsCategory.NAVIGATION },
                                    testTag = "settings_cat_navigation"
                                )
                            }
                            item {
                                SettingsHubRow(
                                    icon = MikaIcons.Equalizer,
                                    title = SettingsCategory.PLAYBACK.title,
                                    onClick = { selectedCategory = SettingsCategory.PLAYBACK },
                                    testTag = "settings_cat_playback"
                                )
                            }
                            item {
                                SettingsHubRow(
                                    icon = MikaIcons.Folder,
                                    title = SettingsCategory.LIBRARY.title,
                                    onClick = { selectedCategory = SettingsCategory.LIBRARY },
                                    testTag = "settings_cat_library"
                                )
                            }
                            item {
                                SettingsHubRow(
                                    icon = MikaIcons.Cleaning,
                                    title = SettingsCategory.STORAGE.title,
                                    onClick = { selectedCategory = SettingsCategory.STORAGE },
                                    testTag = "settings_cat_storage"
                                )
                            }

                            item {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                                )
                            }

                            item {
                                SettingsHubRow(
                                    icon = MikaIcons.HomeLinear,
                                    title = "Home Screen Widget",
                                    onClick = onOpenWidgetSettings,
                                    testTag = "settings_hub_widget"
                                )
                            }
                            item {
                                SettingsHubRow(
                                    icon = MikaIcons.BackupUpload,
                                    title = "Backup & Restore",
                                    onClick = onOpenBackup,
                                    testTag = "settings_hub_backup"
                                )
                            }
                            item {
                                SettingsHubRow(
                                    icon = MikaIcons.Chart,
                                    title = "Statistics",
                                    onClick = onOpenStatistics,
                                    testTag = "settings_hub_stats"
                                )
                            }
                            item {
                                SettingsHubRow(
                                    icon = MikaIcons.Trash,
                                    title = if (trashedCount > 0) "Trash Bin ($trashedCount)" else "Trash Bin",
                                    onClick = onOpenTrash,
                                    testTag = "settings_hub_trash"
                                )
                            }
                            item {
                                SettingsHubRow(
                                    icon = MikaIcons.InfoCircle,
                                    title = "About",
                                    onClick = onOpenAbout,
                                    testTag = "settings_hub_about"
                                )
                            }
                        }
                    }
                }
            } else {
                val detailListState = rememberLazyListState()
                MikaVerticalFastScroller(
                    listState = detailListState,
                    totalItems = 4,
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        state = detailListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("settings_category_detail"),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (activeCategory) {
                            SettingsCategory.APPEARANCE -> {
                                item {
                                    SettingsSectionCard(title = "Theme & Colors") {
                                        Text("Theme Mode", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            ThemeMode.entries.forEach { mode ->
                                                val selected = settings.themeMode == mode
                                                val modeIcon = when (mode) {
                                                    ThemeMode.DARK -> MikaIcons.Moon
                                                    ThemeMode.LIGHT -> MikaIcons.Sun
                                                    ThemeMode.SYSTEM -> MikaIcons.SystemTheme
                                                }
                                                val shortLabel = mode.label
                                                    .replace(" Mode", "")
                                                    .replace(" Default", "")
                                                Surface(
                                                    onClick = { onSetThemeMode(mode) },
                                                    shape = RoundedCornerShape(16.dp),
                                                    color = if (selected) {
                                                        accent.copy(alpha = 0.16f)
                                                    } else {
                                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)
                                                    },
                                                    border = BorderStroke(
                                                        width = if (selected) 1.5.dp else 1.dp,
                                                        color = if (selected) accent else MaterialTheme.colorScheme.outlineVariant
                                                    ),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .testTag("theme_mode_${mode.name}")
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(34.dp)
                                                                .clip(CircleShape)
                                                                .background(
                                                                    if (selected) accent else MaterialTheme.colorScheme.surface
                                                                ),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = modeIcon,
                                                                contentDescription = mode.label,
                                                                tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                                modifier = Modifier.size(mikaScaledIcon(18.dp))
                                                            )
                                                        }
                                                        Text(
                                                            text = shortLabel,
                                                            style = MaterialTheme.typography.labelLarge,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (selected) accent else MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("Accent Color", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                                if (settings.autoColoring) {
                                                    Text(
                                                        text = "Manual fallback when paused or untagged",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            TextButton(
                                                onClick = { showCustomColorPicker = !showCustomColorPicker },
                                                modifier = Modifier.testTag("toggle_custom_color_picker")
                                            ) {
                                                Icon(MikaIcons.Palette, null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    text = if (showCustomColorPicker) "Hide Picker" else "Color Picker",
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("accent_color_row")
                                        ) {
                                            items(AccentPalette.options, key = { it.id }) { option ->
                                                val selected = settings.accentColorId == option.id
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier
                                                        .width(66.dp)
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .clickable { onSetAccentColor(option.id) }
                                                        .padding(vertical = 4.dp)
                                                        .testTag("accent_${option.id}")
                                                ) {
                                                    Box(
                                                        contentAlignment = Alignment.Center,
                                                        modifier = Modifier
                                                            .size(48.dp)
                                                            .clip(CircleShape)
                                                            .background(option.color)
                                                            .then(
                                                                if (selected) {
                                                                    Modifier.border(3.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                                                                } else Modifier
                                                            )
                                                    ) {
                                                        if (selected) {
                                                            Icon(
                                                                imageVector = MikaIcons.Check,
                                                                contentDescription = option.name,
                                                                tint = if (option.color.luminance() > 0.56f) Color(0xFF0A0F1A) else Color.White,
                                                                modifier = Modifier.size(mikaScaledIcon(22.dp))
                                                            )
                                                        }
                                                    }
                                                    Spacer(Modifier.height(5.dp))
                                                    Text(
                                                        text = option.name.substringBefore(" "),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                                                        color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }

                                            item(key = "custom_color_chip") {
                                                val isCustomSelected = settings.accentColorId == "custom"
                                                val customPreview = if (settings.customAccentArgb != 0L) {
                                                    Color(settings.customAccentArgb.toInt())
                                                } else {
                                                    accent
                                                }
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier
                                                        .width(66.dp)
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .clickable {
                                                            showCustomColorPicker = true
                                                            if (settings.customAccentArgb != 0L) {
                                                                onSetCustomAccentColor(settings.customAccentArgb)
                                                            }
                                                        }
                                                        .padding(vertical = 4.dp)
                                                        .testTag("accent_custom")
                                                ) {
                                                    Box(
                                                        contentAlignment = Alignment.Center,
                                                        modifier = Modifier
                                                            .size(48.dp)
                                                            .clip(CircleShape)
                                                            .background(
                                                                if (settings.customAccentArgb != 0L) {
                                                                    Brush.linearGradient(listOf(customPreview, customPreview))
                                                                } else {
                                                                    Brush.sweepGradient(
                                                                        listOf(
                                                                            Color(0xFFEF4444),
                                                                            Color(0xFFF59E0B),
                                                                            Color(0xFF10B981),
                                                                            Color(0xFF1E88FF),
                                                                            Color(0xFF8B5CF6),
                                                                            Color(0xFFEF4444)
                                                                        )
                                                                    )
                                                                }
                                                            )
                                                            .then(
                                                                if (isCustomSelected) {
                                                                    Modifier.border(3.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                                                                } else Modifier
                                                            )
                                                    ) {
                                                        Icon(
                                                            imageVector = if (isCustomSelected) MikaIcons.Check else MikaIcons.Palette,
                                                            contentDescription = "Custom Color Picker",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(21.dp)
                                                        )
                                                    }
                                                    Spacer(Modifier.height(5.dp))
                                                    Text(
                                                        text = "Custom",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = if (isCustomSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                        color = if (isCustomSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }

                                        if (showCustomColorPicker) {
                                            val initialColorInt = if (settings.customAccentArgb != 0L) {
                                                settings.customAccentArgb.toInt()
                                            } else {
                                                accent.toArgb()
                                            }
                                            AccentColorPickerCard(
                                                initialColorInt = initialColorInt,
                                                onApplyColor = { argbLong ->
                                                    onSetCustomAccentColor(argbLong)
                                                }
                                            )
                                        }
                                    }
                                }

                                item {
                                    SettingsSectionCard(title = "Typography & Sizing") {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(MikaIcons.Lyrics, null, tint = accent, modifier = Modifier.size(22.dp))
                                                Text("Font Size", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            }
                                            Text(
                                                text = "${(settings.fontScale * 100).toInt()}%",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = accent
                                            )
                                        }
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            items(fontScalePresets, key = { it.label }) { preset ->
                                                val selected = abs(settings.fontScale - preset.scale) < 0.03f
                                                FilterChip(
                                                    selected = selected,
                                                    onClick = { onSetFontScale(preset.scale) },
                                                    label = { Text(preset.label, fontWeight = FontWeight.Bold) },
                                                    modifier = Modifier.testTag("font_scale_${preset.label.lowercase().replace(" ", "_")}")
                                                )
                                            }
                                        }
                                        CompactSlimSlider(
                                            value = settings.fontScale,
                                            onValueChange = { raw ->
                                                val snapped = kotlin.math.round(raw * 20f) / 20f
                                                onSetFontScale(snapped.coerceIn(0.85f, 1.30f))
                                            },
                                            valueRange = 0.85f..1.30f,
                                            accentColor = accent,
                                            modifier = Modifier.testTag("font_scale_slider")
                                        )
                                    }
                                }

                                item {
                                    SettingsSectionCard(title = "Player Style Studio") {
                                        SettingsActionRow(
                                            icon = MikaIcons.StyleTune,
                                            title = "Mini Player & Full Player Styles",
                                            subtitle = "Mini: ${settings.miniPlayerConfig.style.label} · Full: ${settings.fullPlayerConfig.style.label}",
                                            trailing = {
                                                Icon(
                                                    imageVector = MikaIcons.ArrowRight,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            },
                                            onClick = onOpenPlayerStyleStudio
                                        )
                                    }
                                }

                                item {
                                    SettingsSectionCard(title = "Gradients") {
                                        Text("Gradient Style", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            items(GradientStyle.entries) { style ->
                                                FilterChip(
                                                    selected = settings.gradientStyle == style,
                                                    onClick = { onSetGradientStyle(style) },
                                                    label = { Text(style.label, fontWeight = FontWeight.Bold) }
                                                )
                                            }
                                        }
                                        Text(
                                            text = when (settings.gradientStyle) {
                                                GradientStyle.NONE -> "Flat crisp M3 surfaces without gradient overlays."
                                                GradientStyle.SUBTLE_LINEAR -> "Smooth diagonal sheen tuned from your accent hue."
                                                GradientStyle.RADIAL_GLOW -> "Soft radial halo blooming behind player controls and art."
                                                GradientStyle.AURORA_DUAL -> "Rich multi-point ambient blend reacting to artwork & theme."
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        SettingsSwitchRow(
                                            title = "Gradient on Mini-Player",
                                            checked = settings.applyGradientToMiniPlayer,
                                            onCheckedChange = {
                                                onSetGradientTargets(
                                                    it,
                                                    settings.applyGradientToFullPlayer,
                                                    settings.applyGradientToSurfaces
                                                )
                                            }
                                        )
                                        SettingsSwitchRow(
                                            title = "Gradient on Full-Screen Player",
                                            checked = settings.applyGradientToFullPlayer,
                                            onCheckedChange = {
                                                onSetGradientTargets(
                                                    settings.applyGradientToMiniPlayer,
                                                    it,
                                                    settings.applyGradientToSurfaces
                                                )
                                            }
                                        )
                                        SettingsSwitchRow(
                                            title = "Gradient on Library Cards",
                                            checked = settings.applyGradientToSurfaces,
                                            onCheckedChange = {
                                                onSetGradientTargets(
                                                    settings.applyGradientToMiniPlayer,
                                                    settings.applyGradientToFullPlayer,
                                                    it
                                                )
                                            }
                                        )
                                    }
                                }

                                item {
                                    ColorAndThemeExtrasCard(
                                        settings = settings,
                                        onToggleAutoColoring = onSetAutoColoring,
                                        onTogglePickColorsFromWallpaper = onSetPickColorsFromWallpaper,
                                        onToggleForceMiniPlayerTrackColors = onSetForceMiniPlayerTrackColors,
                                        onToggleUsePitchBlack = onSetUsePitchBlack,
                                        onOpenLightColorPicker = { activeDefaultColorTarget = 1 },
                                        onOpenDarkColorPicker = { activeDefaultColorTarget = 2 },
                                        onToggleApplyColorOnApp = onSetApplyColorOnApp,
                                        onToggleApplyColorOnPlayer = onSetApplyColorOnPlayer
                                    )
                                }

                                item {
                                    VisualAndBackgroundEffectsCard(
                                        settings = settings,
                                        onToggleBlur = { onSetVisualEffects(it, null, null, null) },
                                        onToggleGlow = { onSetVisualEffects(null, it, null, null) },
                                        onToggleParallax = { onSetVisualEffects(null, null, it, null) },
                                        onSelectBackgroundEffect = { onSetVisualEffects(null, null, null, it) },
                                        onOpenPlayerBackground = onOpenPlayerBackground,
                                        onOpenWidgetSettings = onOpenWidgetSettings
                                    )
                                }

                                item {
                                    ArtworkGesturesCollapsibleCard(
                                        settings = settings,
                                        onToggleDoubleTapLyrics = { onSetArtworkGestures(it, null, null) },
                                        onSelectTapAction = { onSetArtworkGestures(null, it, null) },
                                        onSelectLongPressAction = { onSetArtworkGestures(null, null, it) },
                                        onReset = onResetArtworkGestures
                                    )
                                }
                            }

                            SettingsCategory.CUSTOMIZATION -> {
                                item {
                                    SettingsSectionCard(title = "Launcher App Icon") {
                                        Text(
                                            text = "Choose an app icon preset or import a custom image from storage.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            items(AppIconPreset.entries, key = { it.name }) { preset ->
                                                val selected = settings.appIconPreset == preset
                                                val previewRes = when (preset) {
                                                    AppIconPreset.AURORA_GLASS -> R.drawable.ic_app_preset_aurora
                                                    AppIconPreset.MONO_OUTLINE -> R.drawable.ic_app_preset_mono
                                                    AppIconPreset.VIOLET_LINE -> R.drawable.ic_app_preset_violet
                                                    AppIconPreset.NEON_WAVE -> R.drawable.ic_app_preset_neon
                                                    AppIconPreset.CLASSIC -> R.drawable.mika_icon_fg
                                                    AppIconPreset.CUSTOM -> R.drawable.ic_app_preset_aurora
                                                }
                                                Surface(
                                                    onClick = {
                                                        if (preset == AppIconPreset.CUSTOM) {
                                                            onPickCustomAppIcon()
                                                        } else {
                                                            onSetAppIconPreset(preset)
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(16.dp),
                                                    color = if (selected) accent.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                                                    border = BorderStroke(
                                                        width = if (selected) 1.5.dp else 1.dp,
                                                        color = if (selected) accent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                                                    ),
                                                    modifier = Modifier
                                                        .width(102.dp)
                                                        .testTag("app_icon_preset_${preset.name.lowercase()}")
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(10.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(52.dp)
                                                                .clip(RoundedCornerShape(14.dp))
                                                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            if (preset == AppIconPreset.CUSTOM && settings.customAppIconPath.isBlank()) {
                                                                Icon(
                                                                    imageVector = MikaIcons.AddCircle,
                                                                    contentDescription = "Pick custom app icon",
                                                                    tint = accent,
                                                                    modifier = Modifier.size(24.dp)
                                                                )
                                                            } else {
                                                                Image(
                                                                    painter = painterResource(id = previewRes),
                                                                    contentDescription = preset.label,
                                                                    contentScale = ContentScale.Crop,
                                                                    modifier = Modifier.fillMaxSize()
                                                                )
                                                            }
                                                        }
                                                        Text(
                                                            text = preset.label,
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                            color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        if (settings.customAppIconPath.isNotBlank()) {
                                            Text(
                                                text = "Custom icon ready · Tap 'From Storage' to change",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = accent
                                            )
                                        }
                                    }
                                }

                                item {
                                    SettingsSectionCard(title = "Corner Border Radius") {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Global Card & Art Rounding", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            Text(
                                                text = "${settings.cornerRadiusDp}dp",
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                color = accent
                                            )
                                        }
                                        val radiusPresets = listOf("Sharp" to 6, "Soft" to 12, "Rounded" to 16, "Pill" to 24)
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            items(radiusPresets, key = { it.first }) { (label, dpVal) ->
                                                FilterChip(
                                                    selected = abs(settings.cornerRadiusDp - dpVal) <= 1,
                                                    onClick = { onSetCornerRadiusDp(dpVal) },
                                                    label = { Text("$label (${dpVal}dp)", fontWeight = FontWeight.Bold) },
                                                    modifier = Modifier.testTag("corner_radius_${label.lowercase()}")
                                                )
                                            }
                                        }
                                        CompactSlimSlider(
                                            value = settings.cornerRadiusDp.toFloat(),
                                            onValueChange = { onSetCornerRadiusDp(it.toInt().coerceIn(4, 28)) },
                                            valueRange = 4f..28f,
                                            accentColor = accent,
                                            modifier = Modifier.testTag("corner_radius_slider")
                                        )
                                    }
                                }

                                item {
                                    SettingsSectionCard(title = "Playback Control Icons") {
                                        Text(
                                            text = "Pick the play, pause, next & previous button style used across the player.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            PlaybackIconStyle.entries.forEach { style ->
                                                val selected = settings.playbackIconStyle == style
                                                val preview = MikaIcons.transportIcons(style)
                                                Surface(
                                                    onClick = { onSetPlaybackIconStyle(style) },
                                                    shape = RoundedCornerShape(16.dp),
                                                    color = if (selected) accent.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                                                    border = BorderStroke(
                                                        width = if (selected) 1.5.dp else 1.dp,
                                                        color = if (selected) accent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                                                    ),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .testTag("playback_icon_style_${style.name.lowercase()}")
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                    ) {
                                                        Text(
                                                            text = style.label,
                                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                            color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        Icon(preview.previous, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
                                                        Icon(preview.play, null, tint = if (selected) accent else MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                                                        Icon(preview.pause, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                                        Icon(preview.next, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                item {
                                    SettingsSectionCard(title = "Track List & Swipe Gestures") {
                                        SettingsSwitchRowWithSubtitle(
                                            title = "Show Like Button in Music Lists",
                                            subtitle = "Quickly favorite tracks directly from song rows",
                                            checked = settings.showLikeButtonInList,
                                            onCheckedChange = onSetShowLikeButtonInList,
                                            modifier = Modifier.testTag("show_like_button_switch")
                                        )
                                        SettingsSwitchRowWithSubtitle(
                                            title = "Show Actual Artwork Proportions",
                                            subtitle = "Display natural cover aspect ratio instead of forcing square",
                                            checked = settings.showActualArtworkRatio,
                                            onCheckedChange = onSetShowActualArtworkRatio,
                                            modifier = Modifier.testTag("actual_artwork_ratio_switch")
                                        )
                                        SettingsSwitchRowWithSubtitle(
                                            title = "Swipe Screen Edge to Open Drawer",
                                            subtitle = "Disable if edge swipes conflict with song row swipe gestures",
                                            checked = settings.swipeToOpenDrawer,
                                            onCheckedChange = onSetSwipeToOpenDrawer,
                                            modifier = Modifier.testTag("swipe_open_drawer_switch")
                                        )

                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

                                        Text("Swipe Track Right Action", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            items(TrackSwipeAction.entries, key = { "right_${it.name}" }) { action ->
                                                FilterChip(
                                                    selected = settings.swipeRightAction == action,
                                                    onClick = { onSetTrackSwipeActions(null, action) },
                                                    label = { Text(action.label, fontWeight = FontWeight.Bold) }
                                                )
                                            }
                                        }

                                        Text("Swipe Track Left Action", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            items(TrackSwipeAction.entries, key = { "left_${it.name}" }) { action ->
                                                FilterChip(
                                                    selected = settings.swipeLeftAction == action,
                                                    onClick = { onSetTrackSwipeActions(action, null) },
                                                    label = { Text(action.label, fontWeight = FontWeight.Bold) }
                                                )
                                            }
                                        }
                                    }
                                }

                                item {
                                    SettingsSectionCard(title = "App Icon Size") {
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
                                                Icon(
                                                    MikaIcons.MagicPen,
                                                    null,
                                                    tint = accent,
                                                    modifier = Modifier.size(mikaScaledIcon(22.dp))
                                                )
                                                Text(
                                                    text = "Global Icon Scale",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Text(
                                                text = "${(settings.appIconScale * 100).toInt()}%",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = accent,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        val iconPresets = listOf(
                                            "Compact" to 0.85f,
                                            "Normal" to 1.00f,
                                            "Large" to 1.15f,
                                            "XL" to 1.25f
                                        )
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            items(iconPresets, key = { it.first }) { (label, scaleVal) ->
                                                val selected = abs(settings.appIconScale - scaleVal) < 0.03f
                                                FilterChip(
                                                    selected = selected,
                                                    onClick = { onSetAppIconScale(scaleVal) },
                                                    label = { Text(label, fontWeight = FontWeight.Bold) },
                                                    modifier = Modifier.testTag("icon_scale_${label.lowercase()}")
                                                )
                                            }
                                        }
                                        CompactSlimSlider(
                                            value = settings.appIconScale,
                                            onValueChange = { raw ->
                                                val snapped = kotlin.math.round(raw * 20f) / 20f
                                                onSetAppIconScale(snapped.coerceIn(0.80f, 1.30f))
                                            },
                                            valueRange = 0.80f..1.30f,
                                            accentColor = accent,
                                            modifier = Modifier.testTag("app_icon_scale_slider")
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                                horizontalArrangement = Arrangement.SpaceEvenly,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(MikaIcons.HomeBold, "Preview home icon", tint = accent, modifier = Modifier.size(mikaScaledIcon(22.dp)))
                                                Icon(MikaIcons.MusicNote, "Preview music icon", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(mikaScaledIcon(22.dp)))
                                                Icon(MikaIcons.VideoBold, "Preview video icon", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(mikaScaledIcon(22.dp)))
                                                Icon(MikaIcons.HeartBold, "Preview heart icon", tint = accent, modifier = Modifier.size(mikaScaledIcon(22.dp)))
                                                Icon(MikaIcons.Settings, "Preview settings icon", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(mikaScaledIcon(22.dp)))
                                            }
                                        }
                                    }
                                }

                                item {
                                    SettingsSectionCard(title = "Seek Bar Styles") {
                                        Text(
                                            text = "Choose how the playback progress seek bar looks and animates in Full Player.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        val mikaColors = LocalMikaColors.current
                                        var previewProgress by remember { mutableFloatStateOf(0.58f) }
                                        val previewWaveform = remember {
                                            floatArrayOf(
                                                0.28f, 0.55f, 0.82f, 0.44f, 0.91f, 0.65f, 0.36f, 0.76f,
                                                0.95f, 0.52f, 0.40f, 0.84f, 0.68f, 0.31f, 0.74f, 0.88f,
                                                0.59f, 0.43f, 0.79f, 0.61f, 0.33f, 0.70f, 0.90f, 0.48f,
                                                0.37f, 0.66f, 0.85f, 0.50f, 0.38f, 0.72f, 0.54f, 0.29f
                                            )
                                        }
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            SeekBarStyle.entries.forEach { style ->
                                                val selected = settings.seekBarStyle == style
                                                Surface(
                                                    onClick = { onSetSeekBarStyle(style) },
                                                    shape = RoundedCornerShape(16.dp),
                                                    color = if (selected) {
                                                        accent.copy(alpha = 0.14f)
                                                    } else {
                                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
                                                    },
                                                    border = BorderStroke(
                                                        width = if (selected) 1.5.dp else 1.dp,
                                                        color = if (selected) accent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                                                    ),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .testTag("seek_bar_style_${style.name.lowercase()}")
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = style.label,
                                                                style = MaterialTheme.typography.titleSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                            if (selected) {
                                                                Icon(
                                                                    imageVector = MikaIcons.Check,
                                                                    contentDescription = "Selected",
                                                                    tint = accent,
                                                                    modifier = Modifier.size(mikaScaledIcon(18.dp))
                                                                )
                                                            }
                                                        }
                                                        MikaSeekBar(
                                                            value = previewProgress,
                                                            onValueChange = {
                                                                previewProgress = it
                                                                onSetSeekBarStyle(style)
                                                            },
                                                            seekBarStyle = style,
                                                            isPlaying = true,
                                                            accentColor = accent,
                                                            secondaryColor = mikaColors.accentSecondary,
                                                            inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f),
                                                            thumbCenterColor = MaterialTheme.colorScheme.surface,
                                                            trackHeight = 6.dp,
                                                            thumbRadius = 7.dp,
                                                            touchHeight = 32.dp,
                                                            trackWaveform = previewWaveform,
                                                            liveWaveformHistory = previewWaveform
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                item {
                                    SettingsSectionCard(title = "Studios & Visual Polish") {
                                        SettingsActionRow(
                                            icon = MikaIcons.StyleTune,
                                            title = "Player Style Studio",
                                            subtitle = "Mini: ${settings.miniPlayerConfig.style.label} · Full: ${settings.fullPlayerConfig.style.label}",
                                            trailing = {
                                                Icon(
                                                    imageVector = MikaIcons.ArrowRight,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(mikaScaledIcon(18.dp))
                                                )
                                            },
                                            onClick = onOpenPlayerStyleStudio
                                        )
                                        SettingsActionRow(
                                            icon = MikaIcons.Palette,
                                            title = "Player Background",
                                            subtitle = "${settings.playerBackgroundMode.label} · ${if (settings.playerBackgroundAnimated) "Animated" else "Static"}",
                                            trailing = {
                                                Icon(
                                                    imageVector = MikaIcons.ArrowRight,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(mikaScaledIcon(18.dp))
                                                )
                                            },
                                            onClick = onOpenPlayerBackground
                                        )
                                        SettingsActionRow(
                                            icon = MikaIcons.HomeLinear,
                                            title = "Home Screen Widget",
                                            subtitle = "Customize widget layout, theme & tap actions",
                                            trailing = {
                                                Icon(
                                                    imageVector = MikaIcons.ArrowRight,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(mikaScaledIcon(18.dp))
                                                )
                                            },
                                            onClick = onOpenWidgetSettings
                                        )
                                    }
                                }

                                item {
                                    VisualAndBackgroundEffectsCard(
                                        settings = settings,
                                        onToggleBlur = { onSetVisualEffects(it, null, null, null) },
                                        onToggleGlow = { onSetVisualEffects(null, it, null, null) },
                                        onToggleParallax = { onSetVisualEffects(null, null, it, null) },
                                        onSelectBackgroundEffect = { onSetVisualEffects(null, null, null, it) },
                                        onOpenPlayerBackground = onOpenPlayerBackground,
                                        onOpenWidgetSettings = onOpenWidgetSettings
                                    )
                                }
                            }

                            SettingsCategory.NAVIGATION -> {
                                item {
                                    SettingsSectionCard(title = "Bottom Navigation") {
                                        SettingsSwitchRowWithSubtitle(
                                            title = "Bottom Navigation Bar",
                                            subtitle = "Home, Folders, Library & Videos bar",
                                            checked = settings.showBottomNavBar,
                                            onCheckedChange = onSetShowBottomNavBar,
                                            modifier = Modifier.testTag("bottom_nav_bar_switch")
                                        )

                                        val orderedBottomDests = remember(settings.bottomNavOrder) {
                                            (settings.bottomNavOrder + BottomNavDest.entries).distinct()
                                        }
                                        val visibleBottomCount = orderedBottomDests.count { it !in settings.hiddenBottomNavItems }
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                                        Text(
                                            text = "Bottom Destinations",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Show, hide, or reorder items in the bottom navigation bar.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            orderedBottomDests.forEachIndexed { idx, dest ->
                                                val isVisible = dest !in settings.hiddenBottomNavItems
                                                val isLastVisible = isVisible && visibleBottomCount <= 1
                                                Surface(
                                                    shape = RoundedCornerShape(14.dp),
                                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(
                                                        alpha = if (isVisible) 0.55f else 0.25f
                                                    ),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .testTag("setting_bottom_dest_${dest.name.lowercase()}")
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable(enabled = !isLastVisible) { onToggleBottomNavDestHidden(dest) }
                                                            .padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Checkbox(
                                                            checked = isVisible,
                                                            enabled = !isLastVisible,
                                                            onCheckedChange = { if (!isLastVisible) onToggleBottomNavDestHidden(dest) }
                                                        )
                                                        Text(
                                                            text = dest.label,
                                                            style = MaterialTheme.typography.titleSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isVisible) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        IconButton(
                                                            onClick = { onMoveBottomNavDest(dest, -1) },
                                                            enabled = idx > 0,
                                                            modifier = Modifier.size(36.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = MikaIcons.ArrowUp,
                                                                contentDescription = "Move ${dest.label} up",
                                                                tint = if (idx > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                        IconButton(
                                                            onClick = { onMoveBottomNavDest(dest, 1) },
                                                            enabled = idx < orderedBottomDests.lastIndex,
                                                            modifier = Modifier.size(36.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = MikaIcons.ArrowDown,
                                                                contentDescription = "Move ${dest.label} down",
                                                                tint = if (idx < orderedBottomDests.lastIndex) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                item {
                                    val orderedTabs = remember(settings.enabledTabs) {
                                        settings.enabledTabs + MainTab.entries.filter { it !in settings.enabledTabs }
                                    }
                                    SettingsSectionCard(title = "Visible Tabs & Order") {
                                        Text(
                                            text = "Show, hide, or reorder top tabs.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            orderedTabs.forEach { tab ->
                                                val isEnabled = tab in settings.enabledTabs
                                                val isDefault = settings.defaultTab == tab
                                                val isLastRemaining = isEnabled && settings.enabledTabs.size <= 1
                                                val enabledIdx = settings.enabledTabs.indexOf(tab)

                                                Surface(
                                                    shape = RoundedCornerShape(14.dp),
                                                    color = if (isEnabled) {
                                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                                    } else {
                                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                                    },
                                                    border = BorderStroke(
                                                        width = 1.dp,
                                                        color = if (isDefault && isEnabled) accent.copy(alpha = 0.55f) else Color.Transparent
                                                    ),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .testTag("setting_visible_tab_${tab.name.lowercase()}")
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable(enabled = !isLastRemaining) { onToggleTabEnabled(tab) }
                                                            .padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Checkbox(
                                                            checked = isEnabled,
                                                            enabled = !isLastRemaining,
                                                            onCheckedChange = { if (!isLastRemaining) onToggleTabEnabled(tab) }
                                                        )
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = tab.label,
                                                                style = MaterialTheme.typography.titleSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                            if (isDefault && isEnabled) {
                                                                Text(
                                                                    text = "Default startup tab",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = accent,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                        }

                                                        if (isEnabled) {
                                                            IconButton(
                                                                onClick = { onMoveTab(tab, -1) },
                                                                enabled = enabledIdx > 0,
                                                                modifier = Modifier
                                                                    .size(38.dp)
                                                                    .testTag("move_tab_up_${tab.name.lowercase()}")
                                                            ) {
                                                                Icon(
                                                                    imageVector = MikaIcons.ArrowUp,
                                                                    contentDescription = "Move ${tab.label} up",
                                                                    tint = if (enabledIdx > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                                                    modifier = Modifier.size(18.dp)
                                                                )
                                                            }
                                                            IconButton(
                                                                onClick = { onMoveTab(tab, 1) },
                                                                enabled = enabledIdx in 0 until settings.enabledTabs.lastIndex,
                                                                modifier = Modifier
                                                                    .size(38.dp)
                                                                    .testTag("move_tab_down_${tab.name.lowercase()}")
                                                            ) {
                                                                Icon(
                                                                    imageVector = MikaIcons.ArrowDown,
                                                                    contentDescription = "Move ${tab.label} down",
                                                                    tint = if (enabledIdx in 0 until settings.enabledTabs.lastIndex) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                                                    modifier = Modifier.size(18.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(Modifier.height(4.dp))
                                        Text("Default Startup Tab", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            items(settings.enabledTabs, key = { it.name }) { tab ->
                                                val isDefault = settings.defaultTab == tab
                                                FilterChip(
                                                    selected = isDefault,
                                                    onClick = { onSetDefaultTab(tab) },
                                                    label = { Text(tab.label, fontWeight = FontWeight.Bold) },
                                                    modifier = Modifier.testTag("setting_default_tab_${tab.name.lowercase()}")
                                                )
                                            }
                                        }
                                    }
                                }
                                item {
                                    SettingsSectionCard(title = "Tab Interaction Actions") {
                                        Text("Tab Tap Action", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            items(TabTapAction.entries, key = { it.name }) { action ->
                                                FilterChip(
                                                    selected = settings.tabTapAction == action,
                                                    onClick = { onSetTabActions(action, null) },
                                                    label = { Text(action.label, fontWeight = FontWeight.Bold) }
                                                )
                                            }
                                        }
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                                        Text("Tab Long-Press Action", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            items(TabLongPressAction.entries, key = { it.name }) { action ->
                                                FilterChip(
                                                    selected = settings.tabLongPressAction == action,
                                                    onClick = { onSetTabActions(null, action) },
                                                    label = { Text(action.label, fontWeight = FontWeight.Bold) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            SettingsCategory.PLAYBACK -> {
                                item {
                                    PlaybackAudioAndQueueExtras(
                                        settings = settings,
                                        onToggleSkipSilence = onSetSkipSilence,
                                        onToggleGapless = onSetGapless,
                                        onToggleCrossfadeEnabled = onSetCrossfadeEnabled,
                                        onCrossfadeDurationMs = onSetCrossfadeDurationMs,
                                        onCrossfadeTriggerSec = onSetCrossfadeTriggerLastSec,
                                        onCrossfadeStyle = onSetCrossfadeStyle,
                                        onTogglePlayPauseFade = onSetPlayPauseFadeEnabled,
                                        onPlayFadeDurationMs = onSetPlayFadeDurationMs,
                                        onPauseFadeDurationMs = onSetPauseFadeDurationMs,
                                        onToggleJumpToFirst = onSetJumpToFirstAfterQueue,
                                        onTogglePreviousReplays = onSetPreviousButtonReplays,
                                        onSkipIntervalSec = onSetSkipInterval,
                                        onRestoreMinDurationMin = onSetRestorePositionMinDurationMin
                                    )
                                }

                                item {
                                    SettingsSectionCard(title = "Session & Audio Focus") {
                                        SettingsSwitchRow(
                                            title = "Remember Last Position",
                                            checked = settings.rememberLastPosition,
                                            onCheckedChange = onSetRememberLastPosition
                                        )

                                        SettingsSwitchRowWithSubtitle(
                                            title = "Play Alongside Other Apps",
                                            subtitle = "Continue playing even when another player app starts audio",
                                            checked = settings.allowSimultaneousPlayback,
                                            onCheckedChange = onSetAllowSimultaneousPlayback,
                                            modifier = Modifier.testTag("simultaneous_playback_switch")
                                        )
                                    }
                                }
                            }

                            SettingsCategory.LIBRARY -> {
                                item {
                                    SettingsSectionCard(title = "Auto-Scan & Folders") {
                                        SettingsSwitchRowWithSubtitle(
                                            title = "Auto-Scan on Startup",
                                            subtitle = "Automatically refresh library when app launches",
                                            checked = settings.autoScanOnStartup,
                                            onCheckedChange = onSetAutoScanOnStartup,
                                            modifier = Modifier.testTag("auto_scan_startup_switch")
                                        )
                                        SettingsSwitchRowWithSubtitle(
                                            title = "Auto-Scan Device Audio",
                                            subtitle = "Automatically scan and update music from storage",
                                            checked = settings.autoScanAudio,
                                            onCheckedChange = { onSetAutoScan(it, null) },
                                            modifier = Modifier.testTag("auto_scan_audio_switch")
                                        )
                                        SettingsSwitchRowWithSubtitle(
                                            title = "Auto-Scan Device Videos",
                                            subtitle = "Automatically detect videos stored on device",
                                            checked = settings.autoScanVideo,
                                            onCheckedChange = { onSetAutoScan(null, it) },
                                            modifier = Modifier.testTag("auto_scan_video_switch")
                                        )
                                        SettingsSwitchRowWithSubtitle(
                                            title = "Obey .nomedia Files",
                                            subtitle = "Skip directories containing a .nomedia marker",
                                            checked = settings.obeyNoMedia,
                                            onCheckedChange = onSetObeyNoMedia,
                                            modifier = Modifier.testTag("obey_nomedia_switch")
                                        )
                                        SettingsSwitchRowWithSubtitle(
                                            title = "Group Artwork by Album",
                                            subtitle = "Reuse album cover when individual track art is missing",
                                            checked = settings.groupArtworkByAlbum,
                                            onCheckedChange = onSetGroupArtworkByAlbum,
                                            modifier = Modifier.testTag("group_artwork_album_switch")
                                        )
                                        SettingsActionRow(
                                            icon = MikaIcons.FolderAdd,
                                            title = "Add Local Folder",
                                            subtitle = "Pick a custom folder from device storage",
                                            onClick = onAddFolder
                                        )
                                    }
                                }
                                item {
                                    SettingsSectionCard(title = "Library Indexing & Filters") {
                                        SettingsActionRow(
                                            icon = MikaIcons.Refresh,
                                            title = "Rescan Local Music",
                                            subtitle = if (isScanning) "Scanning device audio..." else "$songCount audio files indexed",
                                            onClick = { if (!isScanning) onRescanLibrary() }
                                        )

                                        SettingsActionRow(
                                            icon = MikaIcons.FolderExclude,
                                            title = "Excluded Folders",
                                            subtitle = when (settings.excludedFolders.size) {
                                                0 -> "All audio folders included · Tap to filter"
                                                1 -> "1 folder excluded from indexing"
                                                else -> "${settings.excludedFolders.size} folders excluded from indexing"
                                            },
                                            trailing = {
                                                Icon(
                                                    imageVector = MikaIcons.ArrowRight,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            },
                                            onClick = {
                                                onRefreshFolders()
                                                showExcludedFoldersSheet = true
                                            }
                                        )

                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                        var blacklistInput by remember { mutableStateOf("") }
                                        Text("Extension Blacklist", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = blacklistInput,
                                                onValueChange = { blacklistInput = it },
                                                placeholder = { Text("e.g. amr, 3gp, mid") },
                                                singleLine = true,
                                                shape = RoundedCornerShape(14.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("extension_blacklist_input")
                                            )
                                            FilledTonalButton(
                                                onClick = {
                                                    val clean = blacklistInput.trim().lowercase().removePrefix(".")
                                                    if (clean.isNotEmpty()) {
                                                        onUpdateExtensionBlacklist(settings.extensionBlacklist + clean)
                                                        blacklistInput = ""
                                                    }
                                                },
                                                modifier = Modifier.height(48.dp)
                                            ) {
                                                Text("Add", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        val suggestedExts = listOf("amr", "3gp", "mid", "wma", "ogg")
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            (settings.extensionBlacklist + suggestedExts).distinct().forEach { ext ->
                                                val isBlocked = ext in settings.extensionBlacklist
                                                FilterChip(
                                                    selected = isBlocked,
                                                    onClick = {
                                                        val updated = if (isBlocked) {
                                                            settings.extensionBlacklist - ext
                                                        } else {
                                                            settings.extensionBlacklist + ext
                                                        }
                                                        onUpdateExtensionBlacklist(updated)
                                                    },
                                                    label = {
                                                        Text(
                                                            text = if (isBlocked) ".$ext ×" else "+ .$ext",
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                )
                                            }
                                        }

                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(MikaIcons.Clock, null, tint = accent, modifier = Modifier.size(22.dp))
                                                    Text("Minimum Track Duration", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                                }
                                                Text(
                                                    text = if (settings.minDurationSec == 0) "Off" else "${settings.minDurationSec}s",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = accent
                                                )
                                            }
                                            CompactSlimSlider(
                                                value = settings.minDurationSec.toFloat(),
                                                onValueChange = { onSetMinDurationSec(it.toInt()) },
                                                valueRange = 0f..180f,
                                                accentColor = accent,
                                                modifier = Modifier.testTag("min_duration_slider")
                                            )
                                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                items(listOf(0, 10, 30, 60, 120, 180)) { sec ->
                                                    FilterChip(
                                                        selected = settings.minDurationSec == sec,
                                                        onClick = { onSetMinDurationSec(sec) },
                                                        label = {
                                                            Text(
                                                                text = when (sec) {
                                                                    0 -> "Off"
                                                                    120 -> "2m"
                                                                    180 -> "3m"
                                                                    else -> "${sec}s"
                                                                },
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(MikaIcons.AudioFile, null, tint = accent, modifier = Modifier.size(22.dp))
                                                    Text("Minimum File Size", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                                }
                                                Text(
                                                    text = formatSizeFilterLabel(settings.minFileSizeKb),
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = accent
                                                )
                                            }
                                            CompactSlimSlider(
                                                value = settings.minFileSizeKb.toFloat(),
                                                onValueChange = { onSetMinFileSizeKb((it / 50f).toInt() * 50) },
                                                valueRange = 0f..5120f,
                                                accentColor = accent,
                                                modifier = Modifier.testTag("min_filesize_slider")
                                            )
                                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                items(listOf(0, 100, 500, 1024, 2048, 5120)) { kb ->
                                                    FilterChip(
                                                        selected = settings.minFileSizeKb == kb,
                                                        onClick = { onSetMinFileSizeKb(kb) },
                                                        label = { Text(formatSizeFilterLabel(kb), fontWeight = FontWeight.Bold) }
                                                    )
                                                }
                                            }
                                        }

                                        SettingsSwitchRow(
                                            title = "Show Lyrics Badges in Lists",
                                            checked = settings.showLyricsBadges,
                                            onCheckedChange = onSetShowLyricsBadges
                                        )
                                    }
                                }

                                item {
                                    SettingsSectionCard(title = "Duplicate File Detector") {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = if (duplicateGroups.isEmpty()) "Find identical files" else "${duplicateGroups.size} duplicate groups",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (duplicateGroups.isNotEmpty()) {
                                                    val reclaimTotal = duplicateGroups.sumOf { it.reclaimableBytes }
                                                    Text(
                                                        text = "${Formatters.formatFileSize(reclaimTotal)} reclaimable",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = accent
                                                    )
                                                }
                                            }
                                            FilledTonalButton(
                                                onClick = onScanDuplicates,
                                                enabled = !isScanningDuplicates,
                                                modifier = Modifier.testTag("scan_duplicates_button")
                                            ) {
                                                Text(if (isScanningDuplicates) "Scanning..." else "Scan", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        if (duplicateGroups.isNotEmpty()) {
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                duplicateGroups.take(6).forEach { group ->
                                                    Surface(
                                                        shape = RoundedCornerShape(14.dp),
                                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                                    ) {
                                                        Column(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(10.dp),
                                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Text(
                                                                text = group.displayName,
                                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            group.songs.forEachIndexed { idx, dupSong ->
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                                ) {
                                                                    Text(
                                                                        text = dupSong.filePath.ifBlank { dupSong.folderName },
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                        maxLines = 1,
                                                                        overflow = TextOverflow.Ellipsis,
                                                                        modifier = Modifier.weight(1f)
                                                                    )
                                                                    if (idx == 0) {
                                                                        Text(
                                                                            text = "Keep",
                                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                                            color = accent
                                                                        )
                                                                    } else {
                                                                        TextButton(
                                                                            onClick = { onDeleteDuplicateSong(dupSong, false) },
                                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                                        ) {
                                                                            Text("Trash", fontWeight = FontWeight.Bold)
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            SettingsCategory.STORAGE -> {
                                item {
                                    SettingsSectionCard(title = "Cache & History") {
                                        SettingsActionRow(
                                            icon = MikaIcons.BroomBold,
                                            title = "Clear All App Cache",
                                            subtitle = "Free ${Formatters.formatFileSize(cacheSizeBytes)} across artwork, video & waveform caches",
                                            onClick = { pendingClearAction = ConfirmClearAction.ALL_APP_CACHE }
                                        )
                                        SettingsActionRow(
                                            icon = MikaIcons.Cleaning,
                                            title = "Clear Artwork Cache",
                                            subtitle = "Remove cached track covers only",
                                            onClick = { pendingClearAction = ConfirmClearAction.ARTWORK_CACHE }
                                        )
                                        SettingsActionRow(
                                            icon = MikaIcons.Trash,
                                            title = "Clear Playback History",
                                            subtitle = "Reset recently played and play counts",
                                            onClick = { pendingClearAction = ConfirmClearAction.PLAYBACK_HISTORY }
                                        )
                                        SettingsActionRow(
                                            icon = MikaIcons.Trash,
                                            title = "Clear Search History",
                                            subtitle = "Remove saved recent search queries",
                                            onClick = { pendingClearAction = ConfirmClearAction.SEARCH_HISTORY }
                                        )
                                    }
                                }
                                item {
                                    SettingsSectionCard(title = "Reset & Data Management") {
                                        SettingsActionRow(
                                            icon = MikaIcons.Refresh,
                                            title = "Reset All Settings",
                                            subtitle = "Restore theme, player styles, and preferences to defaults",
                                            onClick = { pendingClearAction = ConfirmClearAction.RESET_SETTINGS }
                                        )
                                        SettingsActionRow(
                                            icon = MikaIcons.Trash,
                                            title = "Clear All App Data",
                                            subtitle = "Wipe playlists, favorites, history, overrides, and settings",
                                            onClick = { pendingClearAction = ConfirmClearAction.CLEAR_ALL_DATA }
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
}

@Composable
private fun SettingsHubRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(mikaScaledIcon(24.dp))
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(
            imageVector = MikaIcons.ArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(mikaScaledIcon(18.dp))
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExcludedFoldersSheet(
    availableFolders: List<MediaScanner.DeviceFolderSummary>,
    excludedFolders: Set<String>,
    onToggleFolder: (String) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val mergedFolders = remember(availableFolders, excludedFolders) {
        val knownPaths = availableFolders.map { it.path }.toSet()
        val extraExcluded = excludedFolders
            .filter { it !in knownPaths }
            .map { path ->
                MediaScanner.DeviceFolderSummary(
                    path = path,
                    name = path.substringAfterLast('/').ifBlank { path },
                    trackCount = 0
                )
            }
        (availableFolders + extraExcluded).sortedBy { it.name.lowercase() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 26.dp)
                .testTag("excluded_folders_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Excluded Folders",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Checked folders are skipped during library indexing",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (excludedFolders.isNotEmpty()) {
                    TextButton(onClick = onClearAll) {
                        Text("Clear All", fontWeight = FontWeight.Bold)
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            if (mergedFolders.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = MikaIcons.FolderExclude,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = "No device audio folders detected yet",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(mergedFolders, key = { it.path }) { folder ->
                        val isExcluded = folder.path in excludedFolders
                        Surface(
                            onClick = { onToggleFolder(folder.path) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isExcluded) {
                                accent.copy(alpha = 0.12f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isExcluded,
                                    onCheckedChange = { onToggleFolder(folder.path) }
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = folder.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${folder.path} · ${folder.trackCount} files",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    }
}

private fun formatSizeFilterLabel(kb: Int): String {
    return when {
        kb <= 0 -> "Off"
        kb >= 1024 -> {
            val mb = kb / 1024.0
            if (mb % 1.0 == 0.0) "${mb.toInt()} MB" else String.format(Locale.US, "%.1f MB", mb)
        }
        else -> "$kb KB"
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    MikaGlassSurface(
        shape = RoundedCornerShape(20.dp),
        tintStrength = 0.08f,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsSwitchRowWithSubtitle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(24.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        trailing?.invoke()
    }
}

@Composable
private fun AccentColorPickerCard(
    initialColorInt: Int,
    onApplyColor: (Long) -> Unit
) {
    val initialHsv = remember(initialColorInt) {
        FloatArray(3).also { AndroidColor.colorToHSV(initialColorInt, it) }
    }
    var hue by remember { mutableFloatStateOf(initialHsv[0].coerceIn(0f, 360f)) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1].coerceIn(0.08f, 1f)) }
    var brightness by remember { mutableFloatStateOf(initialHsv[2].coerceIn(0.28f, 1f)) }

    val pickedColor = Color.hsv(hue, saturation, brightness)
    val hexString = remember(pickedColor) {
        String.format(Locale.US, "#%06X", pickedColor.toArgb() and 0xFFFFFF)
    }
    var hexInput by remember(hexString) { mutableStateOf(hexString) }
    val quickSwatches = remember {
        listOf(
            Color(0xFF1E88FF),
            Color(0xFF06B6D4),
            Color(0xFF8B5CF6),
            Color(0xFFF43F5E),
            Color(0xFFF59E0B),
            Color(0xFF10B981),
            Color(0xFFEC4899)
        )
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
        border = BorderStroke(1.2.dp, pickedColor.copy(alpha = 0.55f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("accent_color_picker_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(pickedColor)
                        .border(2.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(14.dp))
                )
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { raw ->
                        hexInput = raw
                        val clean = raw.trim().removePrefix("#")
                        if (clean.length == 6) {
                            runCatching { AndroidColor.parseColor("#$clean") }.getOrNull()?.let { parsed ->
                                val hsv = FloatArray(3)
                                AndroidColor.colorToHSV(parsed, hsv)
                                hue = hsv[0]
                                saturation = hsv[1].coerceIn(0.05f, 1f)
                                brightness = hsv[2].coerceIn(0.25f, 1f)
                            }
                        }
                    },
                    label = { Text("Hex Color") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        val argb = pickedColor.toArgb().toLong() and 0xFFFFFFFFL
                        onApplyColor(argb)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = pickedColor,
                        contentColor = if (pickedColor.luminance() > 0.45f) Color.Black else Color.White
                    ),
                    modifier = Modifier.testTag("apply_custom_color_button")
                ) {
                    Text("Apply", fontWeight = FontWeight.Bold)
                }
            }

            // Interactive 2D Saturation / Value gradient square
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            saturation = (offset.x / size.width.coerceAtLeast(1)).coerceIn(0.02f, 1f)
                            brightness = (1f - (offset.y / size.height.coerceAtLeast(1))).coerceIn(0.25f, 1f)
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            saturation = (change.position.x / size.width.coerceAtLeast(1)).coerceIn(0.02f, 1f)
                            brightness = (1f - (change.position.y / size.height.coerceAtLeast(1))).coerceIn(0.25f, 1f)
                        }
                    }
            ) {
                val pureHue = Color.hsv(hue, 1f, 1f)
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(Color.White, pureHue)),
                    cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
                )
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)),
                    cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
                )
                val cx = saturation * size.width
                val cy = (1f - brightness) * size.height
                drawCircle(
                    color = Color.White,
                    radius = 9.dp.toPx(),
                    center = Offset(cx, cy),
                    style = Stroke(width = 2.5.dp.toPx())
                )
                drawCircle(
                    color = pickedColor,
                    radius = 6.dp.toPx(),
                    center = Offset(cx, cy)
                )
            }

            HueSpectrumBar(
                hue = hue,
                onHueChange = { hue = it }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                quickSwatches.forEach { swatch ->
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(swatch)
                            .border(1.5.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                            .clickable {
                                val hsv = FloatArray(3)
                                AndroidColor.colorToHSV(swatch.toArgb(), hsv)
                                hue = hsv[0]
                                saturation = hsv[1].coerceIn(0.08f, 1f)
                                brightness = hsv[2].coerceIn(0.28f, 1f)
                            }
                    )
                }
            }
        }
    }
}

@Composable
private fun HueSpectrumBar(
    hue: Float,
    onHueChange: (Float) -> Unit
) {
    val latestOnHueChange by rememberUpdatedState(onHueChange)
    val density = LocalDensity.current
    val rainbowColors = remember {
        listOf(
            Color(0xFFFF0000),
            Color(0xFFFFFF00),
            Color(0xFF00FF00),
            Color(0xFF00FFFF),
            Color(0xFF0000FF),
            Color(0xFFFF00FF),
            Color(0xFFFF0000)
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
    ) {
        val totalW = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val padPx = with(density) { 10.dp.toPx() }
        val usableW = (totalW - padPx * 2f).coerceAtLeast(1f)

        fun hueFromX(x: Float): Float {
            return (((x - padPx) / usableW).coerceIn(0f, 1f) * 360f)
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        latestOnHueChange(hueFromX(offset.x))
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { start -> latestOnHueChange(hueFromX(start.x)) },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            latestOnHueChange(hueFromX(change.position.x))
                        }
                    )
                }
        ) {
            val barH = 14.dp.toPx()
            val top = (size.height - barH) / 2f
            val left = padPx
            val width = (size.width - padPx * 2f).coerceAtLeast(1f)

            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = rainbowColors,
                    startX = left,
                    endX = left + width
                ),
                topLeft = Offset(left, top),
                size = Size(width, barH),
                cornerRadius = CornerRadius(barH / 2f, barH / 2f)
            )

            val thumbX = left + width * (hue / 360f).coerceIn(0f, 1f)
            val center = Offset(thumbX, size.height / 2f)
            drawCircle(
                color = Color.White,
                radius = 11.dp.toPx(),
                center = center
            )
            drawCircle(
                color = Color.hsv(hue.coerceIn(0f, 360f), 0.9f, 1f),
                radius = 7.5.dp.toPx(),
                center = center
            )
        }
    }
}
