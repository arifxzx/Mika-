package com.example.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AlbumSortField
import com.example.model.AppIconPreset
import com.example.model.AppSettings
import com.example.model.ArtistSortField
import com.example.model.ArtworkGestureAction
import com.example.model.BackgroundEffect
import com.example.model.BottomNavDest
import com.example.model.CrossfadeStyle
import com.example.model.EqPreset
import com.example.model.EqualizerState
import com.example.model.FolderSortField
import com.example.model.FullPlayerConfig
import com.example.model.GradientStyle
import com.example.model.MainTab
import com.example.model.MiniPlayerConfig
import com.example.model.PlaybackIconStyle
import com.example.model.PlayerBackgroundMode
import com.example.model.PlaylistSortField
import com.example.model.RepeatMode
import com.example.model.SeekBarStyle
import com.example.model.SortDirection
import com.example.model.SortField
import com.example.model.SoundControlSettings
import com.example.model.TabLongPressAction
import com.example.model.TabTapAction
import com.example.model.ThemeMode
import com.example.model.TrackSwipeAction
import com.example.model.WidgetConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.mikaDataStore: DataStore<Preferences> by preferencesDataStore(name = "mika_preferences")

data class PersistedPlaybackSnapshot(
    val lastSongId: Long = -1L,
    val lastPositionMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF
)

class SettingsDataStore(private val context: Context) {

    private val widgetJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_COLOR_ID = stringPreferencesKey("accent_color_id")
        val CUSTOM_ACCENT_ARGB = longPreferencesKey("custom_accent_argb")
        val CUSTOM_DARK_ACCENT_ARGB = longPreferencesKey("custom_dark_accent_argb")
        val AUTO_COLORING = booleanPreferencesKey("auto_coloring")
        val PICK_COLORS_WALLPAPER = booleanPreferencesKey("pick_colors_wallpaper")
        val FORCE_MINI_TRACK_COLORS = booleanPreferencesKey("force_mini_track_colors")
        val USE_PITCH_BLACK = booleanPreferencesKey("use_pitch_black")
        val GRADIENT_STYLE = stringPreferencesKey("gradient_style")
        val GRADIENT_MINI_PLAYER = booleanPreferencesKey("gradient_mini_player")
        val GRADIENT_FULL_PLAYER = booleanPreferencesKey("gradient_full_player")
        val GRADIENT_SURFACES = booleanPreferencesKey("gradient_surfaces")

        val SORT_FIELD = stringPreferencesKey("sort_field")
        val SORT_DIRECTION = stringPreferencesKey("sort_direction")
        val GAPLESS_PLAYBACK = booleanPreferencesKey("gapless_playback")
        val SKIP_SILENCE = booleanPreferencesKey("skip_silence")
        val CROSSFADE_ENABLED = booleanPreferencesKey("crossfade_enabled")
        val CROSSFADE_SEC = intPreferencesKey("crossfade_sec")
        val CROSSFADE_DURATION_MS = intPreferencesKey("crossfade_duration_ms")
        val CROSSFADE_TRIGGER_SEC = intPreferencesKey("crossfade_trigger_sec")
        val CROSSFADE_STYLE = stringPreferencesKey("crossfade_style")
        val PLAY_PAUSE_FADE_ENABLED = booleanPreferencesKey("play_pause_fade_enabled")
        val PLAY_FADE_MS = intPreferencesKey("play_fade_ms")
        val PAUSE_FADE_MS = intPreferencesKey("pause_fade_ms")
        val JUMP_TO_FIRST_AFTER_QUEUE = booleanPreferencesKey("jump_to_first_after_queue")
        val PREVIOUS_BUTTON_REPLAYS = booleanPreferencesKey("previous_button_replays")
        val RESTORE_MIN_DURATION_MIN = intPreferencesKey("restore_min_duration_min")

        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        val PLAYBACK_PITCH = floatPreferencesKey("playback_pitch")
        val SKIP_INTERVAL_SEC = intPreferencesKey("skip_interval_sec")
        val REMEMBER_LAST_POSITION = booleanPreferencesKey("remember_last_position")
        val SHOW_LYRICS_BADGES = booleanPreferencesKey("show_lyrics_badges")

        val LAST_SONG_ID = longPreferencesKey("last_song_id")
        val LAST_POSITION_MS = longPreferencesKey("last_position_ms")
        val SHUFFLE_ENABLED = booleanPreferencesKey("shuffle_enabled")
        val REPEAT_MODE = stringPreferencesKey("repeat_mode")

        val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        val EQ_PRESET = stringPreferencesKey("eq_preset")
        val EQ_BANDS = stringPreferencesKey("eq_bands")
        val EQ_BASS = intPreferencesKey("eq_bass")
        val EQ_VIRTUALIZER = intPreferencesKey("eq_virtualizer")

        val FONT_SCALE = floatPreferencesKey("font_scale")
        val APP_ICON_SCALE = floatPreferencesKey("app_icon_scale")
        val CORNER_RADIUS_DP = intPreferencesKey("corner_radius_dp")
        val SEEK_BAR_STYLE = stringPreferencesKey("seek_bar_style")
        val SHOW_ACTUAL_WAVEFORM = booleanPreferencesKey("show_actual_waveform")
        val SHOW_LIVE_WAVEFORM = booleanPreferencesKey("show_live_waveform")
        val PLAYBACK_ICON_STYLE = stringPreferencesKey("playback_icon_style")
        val ENABLED_TABS = stringPreferencesKey("enabled_tabs")
        val DEFAULT_TAB = stringPreferencesKey("default_tab")
        val TAB_TAP_ACTION = stringPreferencesKey("tab_tap_action")
        val TAB_LONG_PRESS_ACTION = stringPreferencesKey("tab_long_press_action")
        val EXCLUDED_FOLDERS = stringSetPreferencesKey("excluded_folders")
        val INCLUDED_FOLDER_URIS = stringSetPreferencesKey("included_folder_uris")
        val VIDEO_FOLDER_URIS = stringSetPreferencesKey("video_folder_uris")
        val ENABLE_FOLDERS_HIERARCHY = booleanPreferencesKey("enable_folders_hierarchy")
        val SKIP_SINGLE_SUBFOLDER = booleanPreferencesKey("skip_single_subfolder")
        val MIN_DURATION_SEC = intPreferencesKey("min_duration_sec")
        val MIN_FILE_SIZE_KB = intPreferencesKey("min_file_size_kb")
        val OBEY_NO_MEDIA = booleanPreferencesKey("obey_nomedia")
        val AUTO_SCAN_STARTUP = booleanPreferencesKey("auto_scan_startup")
        val EXTENSION_BLACKLIST = stringSetPreferencesKey("extension_blacklist")
        val GROUP_ARTWORK_BY_ALBUM = booleanPreferencesKey("group_artwork_by_album")
        val SHOW_ACTUAL_ARTWORK_RATIO = booleanPreferencesKey("show_actual_artwork_ratio")
        val SHOW_LIKE_BUTTON_IN_LIST = booleanPreferencesKey("show_like_button_in_list")
        val SWIPE_LEFT_ACTION = stringPreferencesKey("swipe_left_action")
        val SWIPE_RIGHT_ACTION = stringPreferencesKey("swipe_right_action")
        val SWIPE_TO_OPEN_DRAWER = booleanPreferencesKey("swipe_to_open_drawer")
        val APPLY_COLOR_ON_APP = booleanPreferencesKey("apply_color_on_app")
        val APPLY_COLOR_ON_PLAYER = booleanPreferencesKey("apply_color_on_player")
        val APP_ICON_PRESET = stringPreferencesKey("app_icon_preset")
        val CUSTOM_APP_ICON_PATH = stringPreferencesKey("custom_app_icon_path")

        val AUTO_SCAN_AUDIO = booleanPreferencesKey("auto_scan_audio")
        val AUTO_SCAN_VIDEO = booleanPreferencesKey("auto_scan_video")
        val VIDEO_GRID_VIEW = booleanPreferencesKey("video_grid_view")

        val ENABLE_BLUR_EFFECT = booleanPreferencesKey("enable_blur_effect")
        val ENABLE_GLOW_EFFECT = booleanPreferencesKey("enable_glow_effect")
        val ENABLE_PARALLAX_EFFECT = booleanPreferencesKey("enable_parallax_effect")
        val BACKGROUND_EFFECT = stringPreferencesKey("background_effect")
        val PLAYER_BG_MODE = stringPreferencesKey("player_bg_mode")
        val PLAYER_BG_CUSTOM_URI = stringPreferencesKey("player_bg_custom_uri")
        val PLAYER_BG_VIGNETTE = booleanPreferencesKey("player_bg_vignette")
        val PLAYER_BG_ANIMATED = booleanPreferencesKey("player_bg_animated")
        val PLAYER_BG_COLOR_EXPANDED = booleanPreferencesKey("player_bg_color_expanded")

        val DOUBLE_TAP_LYRICS = booleanPreferencesKey("double_tap_lyrics")
        val ARTWORK_TAP_ACTION = stringPreferencesKey("artwork_tap_action")
        val ARTWORK_LONG_PRESS_ACTION = stringPreferencesKey("artwork_long_press_action")

        val PLAYLIST_SORT_FIELD = stringPreferencesKey("playlist_sort_field")
        val PLAYLIST_SORT_DIRECTION = stringPreferencesKey("playlist_sort_direction")
        val FOLDER_LIST_SORT_FIELD = stringPreferencesKey("folder_list_sort_field")
        val FOLDER_LIST_SORT_DIRECTION = stringPreferencesKey("folder_list_sort_direction")
        val ALBUM_SORT_FIELD = stringPreferencesKey("album_sort_field")
        val ALBUM_SORT_DIRECTION = stringPreferencesKey("album_sort_direction")
        val ARTIST_SORT_FIELD = stringPreferencesKey("artist_sort_field")
        val ARTIST_SORT_DIRECTION = stringPreferencesKey("artist_sort_direction")

        val AUTO_BACKUP_INTERVAL_DAYS = intPreferencesKey("auto_backup_interval_days")
        val LAST_AUTO_BACKUP_AT_MS = longPreferencesKey("last_auto_backup_at_ms")
        val BACKUP_LOCATION_URI = stringPreferencesKey("backup_location_uri")
        val BACKUP_LOCATION_LABEL = stringPreferencesKey("backup_location_label")
        val ALLOW_SIMULTANEOUS_PLAYBACK = booleanPreferencesKey("allow_simultaneous_playback")
        val SHOW_BOTTOM_NAV_BAR = booleanPreferencesKey("show_bottom_nav_bar")
        val BOTTOM_NAV_ORDER = stringPreferencesKey("bottom_nav_order")
        val HIDDEN_BOTTOM_NAV_ITEMS = stringPreferencesKey("hidden_bottom_nav_items")
        val WIDGET_CONFIG_JSON = stringPreferencesKey("widget_config_json")
        val MINI_PLAYER_CONFIG_JSON = stringPreferencesKey("mini_player_config_json")
        val FULL_PLAYER_CONFIG_JSON = stringPreferencesKey("full_player_config_json")
        val SOUND_CONTROL_JSON = stringPreferencesKey("sound_control_json")
    }

    private val defaultLibraryTabs = listOf(
        MainTab.SONGS,
        MainTab.PLAYLISTS,
        MainTab.ALBUMS,
        MainTab.ARTISTS
    )

    val settingsFlow: Flow<AppSettings> = context.mikaDataStore.data.map { prefs ->
        val rawTabs = prefs[Keys.ENABLED_TABS]
            ?.split(",")
            ?.mapNotNull { name -> runCatching { MainTab.valueOf(name) }.getOrNull() }
            ?.distinct()
        val enabledTabs = if (rawTabs.isNullOrEmpty()) {
            defaultLibraryTabs
        } else {
            rawTabs.ifEmpty { defaultLibraryTabs }
        }
        val parsedDefaultTab = prefs[Keys.DEFAULT_TAB]
            ?.let { runCatching { MainTab.valueOf(it) }.getOrNull() }
            ?.takeIf { it in enabledTabs }
            ?: enabledTabs.first()

        val rawNavOrder = prefs[Keys.BOTTOM_NAV_ORDER]
            ?.split(",")
            ?.mapNotNull { name -> runCatching { BottomNavDest.valueOf(name) }.getOrNull() }
            ?.distinct()
        val bottomNavOrder = if (rawNavOrder.isNullOrEmpty()) {
            BottomNavDest.entries
        } else {
            (rawNavOrder + BottomNavDest.entries.filter { it !in rawNavOrder })
        }
        val hiddenBottomNavItems = prefs[Keys.HIDDEN_BOTTOM_NAV_ITEMS]
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.mapNotNull { name -> runCatching { BottomNavDest.valueOf(name) }.getOrNull() }
            ?.toSet()
            ?: emptySet()

        val legacyCrossfadeSec = prefs[Keys.CROSSFADE_SEC] ?: 0
        val crossfadeMs = (prefs[Keys.CROSSFADE_DURATION_MS]
            ?: if (legacyCrossfadeSec > 0) legacyCrossfadeSec * 1000 else 500).coerceIn(200, 12000)
        val crossfadeEnabled = prefs[Keys.CROSSFADE_ENABLED] ?: (legacyCrossfadeSec > 0)

        val parsedWidgetConfig = prefs[Keys.WIDGET_CONFIG_JSON]?.let { raw ->
            runCatching { widgetJson.decodeFromString<WidgetConfig>(raw) }.getOrNull()
        } ?: WidgetConfig()
        val parsedMiniPlayerConfig = prefs[Keys.MINI_PLAYER_CONFIG_JSON]?.let { raw ->
            runCatching { widgetJson.decodeFromString<MiniPlayerConfig>(raw) }.getOrNull()
        } ?: MiniPlayerConfig()
        val parsedFullPlayerConfig = prefs[Keys.FULL_PLAYER_CONFIG_JSON]?.let { raw ->
            runCatching { widgetJson.decodeFromString<FullPlayerConfig>(raw) }.getOrNull()
        } ?: FullPlayerConfig()
        val parsedSoundControl = prefs[Keys.SOUND_CONTROL_JSON]?.let { raw ->
            runCatching { widgetJson.decodeFromString<SoundControlSettings>(raw) }.getOrNull()
        } ?: SoundControlSettings()

        AppSettings(
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.DARK,
            accentColorId = prefs[Keys.ACCENT_COLOR_ID] ?: "electric_blue",
            customAccentArgb = prefs[Keys.CUSTOM_ACCENT_ARGB] ?: 0L,
            customDarkAccentArgb = prefs[Keys.CUSTOM_DARK_ACCENT_ARGB] ?: 0L,
            autoColoring = prefs[Keys.AUTO_COLORING] ?: false,
            applyColorOnApp = prefs[Keys.APPLY_COLOR_ON_APP] ?: true,
            applyColorOnPlayer = prefs[Keys.APPLY_COLOR_ON_PLAYER] ?: true,
            pickColorsFromWallpaper = prefs[Keys.PICK_COLORS_WALLPAPER] ?: false,
            forceMiniPlayerTrackColors = prefs[Keys.FORCE_MINI_TRACK_COLORS] ?: false,
            usePitchBlack = prefs[Keys.USE_PITCH_BLACK] ?: false,
            gradientStyle = prefs[Keys.GRADIENT_STYLE]?.let { runCatching { GradientStyle.valueOf(it) }.getOrNull() }
                ?: GradientStyle.SUBTLE_LINEAR,
            applyGradientToMiniPlayer = prefs[Keys.GRADIENT_MINI_PLAYER] ?: true,
            applyGradientToFullPlayer = prefs[Keys.GRADIENT_FULL_PLAYER] ?: true,
            applyGradientToSurfaces = prefs[Keys.GRADIENT_SURFACES] ?: true,
            sortField = prefs[Keys.SORT_FIELD]?.let { runCatching { SortField.valueOf(it) }.getOrNull() }
                ?: SortField.TITLE,
            sortDirection = prefs[Keys.SORT_DIRECTION]?.let { runCatching { SortDirection.valueOf(it) }.getOrNull() }
                ?: SortDirection.ASCENDING,
            gaplessPlayback = prefs[Keys.GAPLESS_PLAYBACK] ?: true,
            skipSilence = prefs[Keys.SKIP_SILENCE] ?: false,
            crossfadeEnabled = crossfadeEnabled,
            crossfadeDurationSec = if (crossfadeEnabled) (crossfadeMs / 1000).coerceAtLeast(1) else 0,
            crossfadeDurationMs = crossfadeMs,
            crossfadeTriggerLastSec = (prefs[Keys.CROSSFADE_TRIGGER_SEC] ?: 5).coerceIn(1, 15),
            crossfadeStyle = prefs[Keys.CROSSFADE_STYLE]?.let { runCatching { CrossfadeStyle.valueOf(it) }.getOrNull() }
                ?: CrossfadeStyle.DEFAULT,
            playPauseFadeEnabled = prefs[Keys.PLAY_PAUSE_FADE_ENABLED] ?: false,
            playFadeDurationMs = (prefs[Keys.PLAY_FADE_MS] ?: 500).coerceIn(0, 3000),
            pauseFadeDurationMs = (prefs[Keys.PAUSE_FADE_MS] ?: 500).coerceIn(0, 3000),
            jumpToFirstAfterQueue = prefs[Keys.JUMP_TO_FIRST_AFTER_QUEUE] ?: true,
            previousButtonReplays = prefs[Keys.PREVIOUS_BUTTON_REPLAYS] ?: true,
            restorePositionMinDurationMin = (prefs[Keys.RESTORE_MIN_DURATION_MIN] ?: 10).coerceIn(0, 60),
            playbackSpeed = prefs[Keys.PLAYBACK_SPEED] ?: 1.0f,
            playbackPitch = prefs[Keys.PLAYBACK_PITCH] ?: 1.0f,
            skipIntervalSec = (prefs[Keys.SKIP_INTERVAL_SEC] ?: 10).coerceIn(3, 60),
            rememberLastPosition = prefs[Keys.REMEMBER_LAST_POSITION] ?: true,
            showLyricsBadges = prefs[Keys.SHOW_LYRICS_BADGES] ?: true,
            fontScale = (prefs[Keys.FONT_SCALE] ?: 1.0f).coerceIn(0.85f, 1.30f),
            appIconScale = (prefs[Keys.APP_ICON_SCALE] ?: 1.0f).coerceIn(0.80f, 1.30f),
            cornerRadiusDp = (prefs[Keys.CORNER_RADIUS_DP] ?: 16).coerceIn(4, 28),
            seekBarStyle = prefs[Keys.SEEK_BAR_STYLE]
                ?.let { runCatching { SeekBarStyle.valueOf(it) }.getOrNull() }
                ?: parsedFullPlayerConfig.seekBarStyle,
            showActualWaveformSeekbar = prefs[Keys.SHOW_ACTUAL_WAVEFORM] ?: false,
            showLiveWaveformSeekbar = prefs[Keys.SHOW_LIVE_WAVEFORM] ?: false,
            playbackIconStyle = prefs[Keys.PLAYBACK_ICON_STYLE]
                ?.let { runCatching { PlaybackIconStyle.valueOf(it) }.getOrNull() }
                ?: PlaybackIconStyle.NORMAL,
            enabledTabs = enabledTabs,
            defaultTab = parsedDefaultTab,
            tabTapAction = prefs[Keys.TAB_TAP_ACTION]?.let { runCatching { TabTapAction.valueOf(it) }.getOrNull() }
                ?: TabTapAction.OPEN_TAB,
            tabLongPressAction = prefs[Keys.TAB_LONG_PRESS_ACTION]?.let { runCatching { TabLongPressAction.valueOf(it) }.getOrNull() }
                ?: TabLongPressAction.CUSTOMIZE_TABS,
            excludedFolders = prefs[Keys.EXCLUDED_FOLDERS] ?: emptySet(),
            includedFolderUris = prefs[Keys.INCLUDED_FOLDER_URIS] ?: emptySet(),
            videoFolderUris = prefs[Keys.VIDEO_FOLDER_URIS] ?: emptySet(),
            enableFoldersHierarchy = prefs[Keys.ENABLE_FOLDERS_HIERARCHY] ?: true,
            skipSingleSubfolder = prefs[Keys.SKIP_SINGLE_SUBFOLDER] ?: true,
            minDurationSec = (prefs[Keys.MIN_DURATION_SEC] ?: 0).coerceIn(0, 300),
            minFileSizeKb = (prefs[Keys.MIN_FILE_SIZE_KB] ?: 0).coerceIn(0, 10240),
            obeyNoMedia = prefs[Keys.OBEY_NO_MEDIA] ?: true,
            autoScanOnStartup = prefs[Keys.AUTO_SCAN_STARTUP] ?: true,
            extensionBlacklist = prefs[Keys.EXTENSION_BLACKLIST] ?: emptySet(),
            groupArtworkByAlbum = prefs[Keys.GROUP_ARTWORK_BY_ALBUM] ?: false,
            showActualArtworkRatio = prefs[Keys.SHOW_ACTUAL_ARTWORK_RATIO] ?: false,
            showLikeButtonInList = prefs[Keys.SHOW_LIKE_BUTTON_IN_LIST] ?: false,
            swipeLeftAction = prefs[Keys.SWIPE_LEFT_ACTION]?.let { runCatching { TrackSwipeAction.valueOf(it) }.getOrNull() }
                ?: TrackSwipeAction.ADD_TO_QUEUE,
            swipeRightAction = prefs[Keys.SWIPE_RIGHT_ACTION]?.let { runCatching { TrackSwipeAction.valueOf(it) }.getOrNull() }
                ?: TrackSwipeAction.FAVORITE,
            swipeToOpenDrawer = prefs[Keys.SWIPE_TO_OPEN_DRAWER] ?: true,
            autoScanAudio = prefs[Keys.AUTO_SCAN_AUDIO] ?: true,
            autoScanVideo = prefs[Keys.AUTO_SCAN_VIDEO] ?: true,
            videoGridView = prefs[Keys.VIDEO_GRID_VIEW] ?: false,
            enableBlurEffect = prefs[Keys.ENABLE_BLUR_EFFECT] ?: false,
            enableGlowEffect = prefs[Keys.ENABLE_GLOW_EFFECT] ?: true,
            enableParallaxEffect = prefs[Keys.ENABLE_PARALLAX_EFFECT] ?: false,
            backgroundEffect = prefs[Keys.BACKGROUND_EFFECT]?.let { runCatching { BackgroundEffect.valueOf(it) }.getOrNull() }
                ?: BackgroundEffect.NONE,
            playerBackgroundMode = prefs[Keys.PLAYER_BG_MODE]?.let { runCatching { PlayerBackgroundMode.valueOf(it) }.getOrNull() }
                ?: PlayerBackgroundMode.MESH_GRADIENT,
            playerBackgroundCustomUri = prefs[Keys.PLAYER_BG_CUSTOM_URI] ?: "",
            playerBackgroundVignette = prefs[Keys.PLAYER_BG_VIGNETTE] ?: true,
            playerBackgroundAnimated = prefs[Keys.PLAYER_BG_ANIMATED] ?: true,
            playerBackgroundColorWhenExpanded = prefs[Keys.PLAYER_BG_COLOR_EXPANDED] ?: true,
            doubleTapToToggleLyrics = prefs[Keys.DOUBLE_TAP_LYRICS] ?: true,
            artworkTapAction = prefs[Keys.ARTWORK_TAP_ACTION]?.let { runCatching { ArtworkGestureAction.valueOf(it) }.getOrNull() }
                ?: ArtworkGestureAction.ARTWORK_OPEN,
            artworkLongPressAction = prefs[Keys.ARTWORK_LONG_PRESS_ACTION]?.let { runCatching { ArtworkGestureAction.valueOf(it) }.getOrNull() }
                ?: ArtworkGestureAction.INFO,
            playlistSortField = prefs[Keys.PLAYLIST_SORT_FIELD]?.let { runCatching { PlaylistSortField.valueOf(it) }.getOrNull() }
                ?: PlaylistSortField.CREATED_AT,
            playlistSortDirection = prefs[Keys.PLAYLIST_SORT_DIRECTION]?.let { runCatching { SortDirection.valueOf(it) }.getOrNull() }
                ?: SortDirection.DESCENDING,
            folderListSortField = prefs[Keys.FOLDER_LIST_SORT_FIELD]?.let { runCatching { FolderSortField.valueOf(it) }.getOrNull() }
                ?: FolderSortField.NAME,
            folderListSortDirection = prefs[Keys.FOLDER_LIST_SORT_DIRECTION]?.let { runCatching { SortDirection.valueOf(it) }.getOrNull() }
                ?: SortDirection.ASCENDING,
            albumSortField = prefs[Keys.ALBUM_SORT_FIELD]?.let { runCatching { AlbumSortField.valueOf(it) }.getOrNull() }
                ?: AlbumSortField.NAME,
            albumSortDirection = prefs[Keys.ALBUM_SORT_DIRECTION]?.let { runCatching { SortDirection.valueOf(it) }.getOrNull() }
                ?: SortDirection.ASCENDING,
            artistSortField = prefs[Keys.ARTIST_SORT_FIELD]?.let { runCatching { ArtistSortField.valueOf(it) }.getOrNull() }
                ?: ArtistSortField.NAME,
            artistSortDirection = prefs[Keys.ARTIST_SORT_DIRECTION]?.let { runCatching { SortDirection.valueOf(it) }.getOrNull() }
                ?: SortDirection.ASCENDING,
            showBottomNavBar = prefs[Keys.SHOW_BOTTOM_NAV_BAR] ?: true,
            bottomNavOrder = bottomNavOrder,
            hiddenBottomNavItems = hiddenBottomNavItems,
            allowSimultaneousPlayback = prefs[Keys.ALLOW_SIMULTANEOUS_PLAYBACK] ?: false,
            autoBackupIntervalDays = (prefs[Keys.AUTO_BACKUP_INTERVAL_DAYS] ?: 0).coerceIn(0, 90),
            lastAutoBackupAtMs = prefs[Keys.LAST_AUTO_BACKUP_AT_MS] ?: 0L,
            backupLocationUri = prefs[Keys.BACKUP_LOCATION_URI] ?: "",
            backupLocationLabel = prefs[Keys.BACKUP_LOCATION_LABEL] ?: "/mika/backup",
            appIconPreset = prefs[Keys.APP_ICON_PRESET]?.let { runCatching { AppIconPreset.valueOf(it) }.getOrNull() }
                ?: AppIconPreset.AURORA_GLASS,
            customAppIconPath = prefs[Keys.CUSTOM_APP_ICON_PATH] ?: "",
            widgetConfig = parsedWidgetConfig,
            miniPlayerConfig = parsedMiniPlayerConfig,
            fullPlayerConfig = parsedFullPlayerConfig,
            soundControl = parsedSoundControl
        )
    }

    val equalizerFlow: Flow<EqualizerState> = context.mikaDataStore.data.map { prefs ->
        val bandStr = prefs[Keys.EQ_BANDS]
        val bands = bandStr?.split(",")?.mapNotNull { it.toIntOrNull() }
            ?.takeIf { it.isNotEmpty() } ?: listOf(0, 0, 0, 0, 0)
        EqualizerState(
            enabled = prefs[Keys.EQ_ENABLED] ?: false,
            preset = prefs[Keys.EQ_PRESET]?.let { runCatching { EqPreset.valueOf(it) }.getOrNull() }
                ?: EqPreset.NORMAL,
            bandLevelsMb = bands,
            bassBoostStrength = prefs[Keys.EQ_BASS] ?: 0,
            virtualizerStrength = prefs[Keys.EQ_VIRTUALIZER] ?: 0
        )
    }

    suspend fun getPlaybackSnapshot(): PersistedPlaybackSnapshot {
        val prefs = context.mikaDataStore.data.first()
        return PersistedPlaybackSnapshot(
            lastSongId = prefs[Keys.LAST_SONG_ID] ?: -1L,
            lastPositionMs = prefs[Keys.LAST_POSITION_MS] ?: 0L,
            shuffleEnabled = prefs[Keys.SHUFFLE_ENABLED] ?: false,
            repeatMode = prefs[Keys.REPEAT_MODE]?.let { runCatching { RepeatMode.valueOf(it) }.getOrNull() }
                ?: RepeatMode.OFF
        )
    }

    suspend fun updateThemeMode(mode: ThemeMode) {
        context.mikaDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun updateAccentColor(id: String) {
        context.mikaDataStore.edit { it[Keys.ACCENT_COLOR_ID] = id }
    }

    suspend fun updateCustomAccentColor(argb: Long) {
        context.mikaDataStore.edit {
            it[Keys.CUSTOM_ACCENT_ARGB] = argb
            it[Keys.ACCENT_COLOR_ID] = "custom"
        }
    }

    suspend fun updateCustomDarkAccentColor(argb: Long) {
        context.mikaDataStore.edit {
            it[Keys.CUSTOM_DARK_ACCENT_ARGB] = argb
            it[Keys.ACCENT_COLOR_ID] = "custom"
        }
    }

    suspend fun updateAutoColoring(enabled: Boolean) {
        context.mikaDataStore.edit { it[Keys.AUTO_COLORING] = enabled }
    }

    suspend fun updatePickColorsFromWallpaper(enabled: Boolean) {
        context.mikaDataStore.edit { it[Keys.PICK_COLORS_WALLPAPER] = enabled }
    }

    suspend fun updateForceMiniPlayerTrackColors(enabled: Boolean) {
        context.mikaDataStore.edit { it[Keys.FORCE_MINI_TRACK_COLORS] = enabled }
    }

    suspend fun updateUsePitchBlack(enabled: Boolean) {
        context.mikaDataStore.edit { it[Keys.USE_PITCH_BLACK] = enabled }
    }

    suspend fun updateGradientStyle(style: GradientStyle) {
        context.mikaDataStore.edit { it[Keys.GRADIENT_STYLE] = style.name }
    }

    suspend fun updateGradientTargets(miniPlayer: Boolean, fullPlayer: Boolean, surfaces: Boolean) {
        context.mikaDataStore.edit {
            it[Keys.GRADIENT_MINI_PLAYER] = miniPlayer
            it[Keys.GRADIENT_FULL_PLAYER] = fullPlayer
            it[Keys.GRADIENT_SURFACES] = surfaces
        }
    }

    suspend fun updateSort(field: SortField, direction: SortDirection) {
        context.mikaDataStore.edit {
            it[Keys.SORT_FIELD] = field.name
            it[Keys.SORT_DIRECTION] = direction.name
        }
    }

    suspend fun updateGapless(enabled: Boolean) {
        context.mikaDataStore.edit { it[Keys.GAPLESS_PLAYBACK] = enabled }
    }

    suspend fun updateSkipSilence(enabled: Boolean) {
        context.mikaDataStore.edit { it[Keys.SKIP_SILENCE] = enabled }
    }

    suspend fun updateCrossfadeEnabled(enabled: Boolean) {
        context.mikaDataStore.edit {
            it[Keys.CROSSFADE_ENABLED] = enabled
            it[Keys.CROSSFADE_SEC] = if (enabled) ((it[Keys.CROSSFADE_DURATION_MS] ?: 500) / 1000).coerceAtLeast(1) else 0
        }
    }

    suspend fun updateCrossfadeDurationMs(ms: Int) {
        val clamped = ms.coerceIn(200, 12000)
        context.mikaDataStore.edit {
            it[Keys.CROSSFADE_DURATION_MS] = clamped
            it[Keys.CROSSFADE_SEC] = (clamped / 1000).coerceAtLeast(1)
        }
    }

    suspend fun updateCrossfadeTriggerLastSec(seconds: Int) {
        context.mikaDataStore.edit { it[Keys.CROSSFADE_TRIGGER_SEC] = seconds.coerceIn(1, 15) }
    }

    suspend fun updateCrossfadeStyle(style: CrossfadeStyle) {
        context.mikaDataStore.edit { it[Keys.CROSSFADE_STYLE] = style.name }
    }

    suspend fun updatePlayPauseFadeEnabled(enabled: Boolean) {
        context.mikaDataStore.edit { it[Keys.PLAY_PAUSE_FADE_ENABLED] = enabled }
    }

    suspend fun updatePlayFadeDurationMs(ms: Int) {
        context.mikaDataStore.edit { it[Keys.PLAY_FADE_MS] = ms.coerceIn(0, 3000) }
    }

    suspend fun updatePauseFadeDurationMs(ms: Int) {
        context.mikaDataStore.edit { it[Keys.PAUSE_FADE_MS] = ms.coerceIn(0, 3000) }
    }

    suspend fun updateJumpToFirstAfterQueue(enabled: Boolean) {
        context.mikaDataStore.edit { it[Keys.JUMP_TO_FIRST_AFTER_QUEUE] = enabled }
    }

    suspend fun updatePreviousButtonReplays(enabled: Boolean) {
        context.mikaDataStore.edit { it[Keys.PREVIOUS_BUTTON_REPLAYS] = enabled }
    }

    suspend fun updateRestorePositionMinDurationMin(minutes: Int) {
        context.mikaDataStore.edit { it[Keys.RESTORE_MIN_DURATION_MIN] = minutes.coerceIn(0, 60) }
    }

    suspend fun updateCrossfade(seconds: Int) {
        val sec = seconds.coerceIn(0, 12)
        context.mikaDataStore.edit {
            it[Keys.CROSSFADE_SEC] = sec
            it[Keys.CROSSFADE_ENABLED] = sec > 0
            if (sec > 0) it[Keys.CROSSFADE_DURATION_MS] = sec * 1000
        }
    }

    suspend fun updatePlaybackSpeed(speed: Float) {
        context.mikaDataStore.edit { it[Keys.PLAYBACK_SPEED] = speed.coerceIn(0.25f, 3.0f) }
    }

    suspend fun updatePlaybackSpeedAndPitch(speed: Float, pitch: Float) {
        context.mikaDataStore.edit {
            it[Keys.PLAYBACK_SPEED] = speed.coerceIn(0.25f, 3.0f)
            it[Keys.PLAYBACK_PITCH] = pitch.coerceIn(0.5f, 2.0f)
        }
    }

    suspend fun updateSkipInterval(seconds: Int) {
        context.mikaDataStore.edit { it[Keys.SKIP_INTERVAL_SEC] = seconds.coerceIn(3, 60) }
    }

    suspend fun updateRememberLastPosition(remember: Boolean) {
        context.mikaDataStore.edit { it[Keys.REMEMBER_LAST_POSITION] = remember }
    }

    suspend fun updateShowLyricsBadges(show: Boolean) {
        context.mikaDataStore.edit { it[Keys.SHOW_LYRICS_BADGES] = show }
    }

    suspend fun updateVisualEffects(
        blur: Boolean? = null,
        glow: Boolean? = null,
        parallax: Boolean? = null,
        backgroundEffect: BackgroundEffect? = null
    ) {
        context.mikaDataStore.edit { prefs ->
            blur?.let { prefs[Keys.ENABLE_BLUR_EFFECT] = it }
            glow?.let { prefs[Keys.ENABLE_GLOW_EFFECT] = it }
            parallax?.let { prefs[Keys.ENABLE_PARALLAX_EFFECT] = it }
            backgroundEffect?.let { prefs[Keys.BACKGROUND_EFFECT] = it.name }
        }
    }

    suspend fun updatePlayerBackground(
        mode: PlayerBackgroundMode? = null,
        customUri: String? = null,
        vignette: Boolean? = null,
        animated: Boolean? = null,
        colorWhenExpanded: Boolean? = null
    ) {
        context.mikaDataStore.edit { prefs ->
            mode?.let { prefs[Keys.PLAYER_BG_MODE] = it.name }
            customUri?.let { prefs[Keys.PLAYER_BG_CUSTOM_URI] = it }
            vignette?.let { prefs[Keys.PLAYER_BG_VIGNETTE] = it }
            animated?.let { prefs[Keys.PLAYER_BG_ANIMATED] = it }
            colorWhenExpanded?.let { prefs[Keys.PLAYER_BG_COLOR_EXPANDED] = it }
        }
    }

    suspend fun updateArtworkGestures(
        doubleTapToToggleLyrics: Boolean? = null,
        tapAction: ArtworkGestureAction? = null,
        longPressAction: ArtworkGestureAction? = null
    ) {
        context.mikaDataStore.edit { prefs ->
            doubleTapToToggleLyrics?.let { prefs[Keys.DOUBLE_TAP_LYRICS] = it }
            tapAction?.let { prefs[Keys.ARTWORK_TAP_ACTION] = it.name }
            longPressAction?.let { prefs[Keys.ARTWORK_LONG_PRESS_ACTION] = it.name }
        }
    }

    suspend fun savePlaybackState(songId: Long, positionMs: Long, shuffle: Boolean, repeat: RepeatMode) {
        context.mikaDataStore.edit {
            it[Keys.LAST_SONG_ID] = songId
            it[Keys.LAST_POSITION_MS] = positionMs
            it[Keys.SHUFFLE_ENABLED] = shuffle
            it[Keys.REPEAT_MODE] = repeat.name
        }
    }

    suspend fun saveEqualizerState(state: EqualizerState) {
        context.mikaDataStore.edit {
            it[Keys.EQ_ENABLED] = state.enabled
            it[Keys.EQ_PRESET] = state.preset.name
            it[Keys.EQ_BANDS] = state.bandLevelsMb.joinToString(",")
            it[Keys.EQ_BASS] = state.bassBoostStrength
            it[Keys.EQ_VIRTUALIZER] = state.virtualizerStrength
        }
    }

    suspend fun updateFontScale(scale: Float) {
        context.mikaDataStore.edit {
            it[Keys.FONT_SCALE] = scale.coerceIn(0.85f, 1.30f)
        }
    }

    suspend fun updateAppIconScale(scale: Float) {
        context.mikaDataStore.edit {
            it[Keys.APP_ICON_SCALE] = scale.coerceIn(0.80f, 1.30f)
        }
    }

    suspend fun updateCornerRadiusDp(radiusDp: Int) {
        context.mikaDataStore.edit {
            it[Keys.CORNER_RADIUS_DP] = radiusDp.coerceIn(4, 28)
        }
    }

    suspend fun updateSeekBarStyle(style: SeekBarStyle) {
        context.mikaDataStore.edit { prefs ->
            prefs[Keys.SEEK_BAR_STYLE] = style.name
            prefs[Keys.SHOW_ACTUAL_WAVEFORM] = (style == SeekBarStyle.REAL_WAVEFORM)
            prefs[Keys.SHOW_LIVE_WAVEFORM] = (style == SeekBarStyle.LIVE_FLOW_WAVEFORM)
            val currentCfg = prefs[Keys.FULL_PLAYER_CONFIG_JSON]?.let { raw ->
                runCatching { widgetJson.decodeFromString<FullPlayerConfig>(raw) }.getOrNull()
            } ?: FullPlayerConfig()
            prefs[Keys.FULL_PLAYER_CONFIG_JSON] = widgetJson.encodeToString(
                currentCfg.copy(seekBarStyle = style)
            )
        }
    }

    suspend fun updateShowActualWaveformSeekbar(enabled: Boolean) {
        context.mikaDataStore.edit { prefs ->
            prefs[Keys.SHOW_ACTUAL_WAVEFORM] = enabled
            if (enabled) {
                prefs[Keys.SHOW_LIVE_WAVEFORM] = false
                prefs[Keys.SEEK_BAR_STYLE] = SeekBarStyle.REAL_WAVEFORM.name
            } else if (prefs[Keys.SEEK_BAR_STYLE] == SeekBarStyle.REAL_WAVEFORM.name) {
                prefs[Keys.SEEK_BAR_STYLE] = SeekBarStyle.SLEEK_CAPSULE.name
            }
        }
    }

    suspend fun updateShowLiveWaveformSeekbar(enabled: Boolean) {
        context.mikaDataStore.edit { prefs ->
            prefs[Keys.SHOW_LIVE_WAVEFORM] = enabled
            if (enabled) {
                prefs[Keys.SHOW_ACTUAL_WAVEFORM] = false
                prefs[Keys.SEEK_BAR_STYLE] = SeekBarStyle.LIVE_FLOW_WAVEFORM.name
            } else if (prefs[Keys.SEEK_BAR_STYLE] == SeekBarStyle.LIVE_FLOW_WAVEFORM.name) {
                prefs[Keys.SEEK_BAR_STYLE] = SeekBarStyle.SLEEK_CAPSULE.name
            }
        }
    }

    suspend fun updatePlaybackIconStyle(style: PlaybackIconStyle) {
        context.mikaDataStore.edit {
            it[Keys.PLAYBACK_ICON_STYLE] = style.name
        }
    }

    suspend fun updateEnabledTabs(tabs: List<MainTab>) {
        val ordered = tabs.distinct().ifEmpty { listOf(MainTab.SONGS) }
        context.mikaDataStore.edit { prefs ->
            prefs[Keys.ENABLED_TABS] = ordered.joinToString(",") { it.name }
            val currentDefault = prefs[Keys.DEFAULT_TAB]
                ?.let { runCatching { MainTab.valueOf(it) }.getOrNull() }
            if (currentDefault == null || currentDefault !in ordered) {
                prefs[Keys.DEFAULT_TAB] = ordered.first().name
            }
        }
    }

    suspend fun updateDefaultTab(tab: MainTab) {
        context.mikaDataStore.edit {
            it[Keys.DEFAULT_TAB] = tab.name
        }
    }

    suspend fun updateTabActions(tapAction: TabTapAction? = null, longPressAction: TabLongPressAction? = null) {
        context.mikaDataStore.edit { prefs ->
            tapAction?.let { prefs[Keys.TAB_TAP_ACTION] = it.name }
            longPressAction?.let { prefs[Keys.TAB_LONG_PRESS_ACTION] = it.name }
        }
    }

    suspend fun updateExcludedFolders(folders: Set<String>) {
        context.mikaDataStore.edit {
            it[Keys.EXCLUDED_FOLDERS] = folders
        }
    }

    suspend fun updateIncludedFolderUris(uris: Set<String>) {
        context.mikaDataStore.edit {
            it[Keys.INCLUDED_FOLDER_URIS] = uris
        }
    }

    suspend fun updateVideoFolderUris(uris: Set<String>) {
        context.mikaDataStore.edit {
            it[Keys.VIDEO_FOLDER_URIS] = uris
        }
    }

    suspend fun updateAutoScan(audio: Boolean? = null, video: Boolean? = null) {
        context.mikaDataStore.edit { prefs ->
            audio?.let { prefs[Keys.AUTO_SCAN_AUDIO] = it }
            video?.let { prefs[Keys.AUTO_SCAN_VIDEO] = it }
        }
    }

    suspend fun updateVideoGridView(grid: Boolean) {
        context.mikaDataStore.edit {
            it[Keys.VIDEO_GRID_VIEW] = grid
        }
    }

    suspend fun updateFoldersHierarchy(enableHierarchy: Boolean, skipSingleSubfolder: Boolean) {
        context.mikaDataStore.edit {
            it[Keys.ENABLE_FOLDERS_HIERARCHY] = enableHierarchy
            it[Keys.SKIP_SINGLE_SUBFOLDER] = skipSingleSubfolder
        }
    }

    suspend fun updateMinDurationSec(seconds: Int) {
        context.mikaDataStore.edit {
            it[Keys.MIN_DURATION_SEC] = seconds.coerceIn(0, 300)
        }
    }

    suspend fun updateMinFileSizeKb(kb: Int) {
        context.mikaDataStore.edit {
            it[Keys.MIN_FILE_SIZE_KB] = kb.coerceIn(0, 10240)
        }
    }

    suspend fun updateObeyNoMedia(enabled: Boolean) {
        context.mikaDataStore.edit { it[Keys.OBEY_NO_MEDIA] = enabled }
    }

    suspend fun updateAutoScanOnStartup(enabled: Boolean) {
        context.mikaDataStore.edit { it[Keys.AUTO_SCAN_STARTUP] = enabled }
    }

    suspend fun updateExtensionBlacklist(extensions: Set<String>) {
        context.mikaDataStore.edit { prefs ->
            prefs[Keys.EXTENSION_BLACKLIST] = extensions
                .map { it.trim().trimStart('.').lowercase() }
                .filter { it.isNotBlank() }
                .toSet()
        }
    }

    suspend fun updateGroupArtworkByAlbum(enabled: Boolean) {
        context.mikaDataStore.edit { it[Keys.GROUP_ARTWORK_BY_ALBUM] = enabled }
    }

    suspend fun updateShowActualArtworkRatio(enabled: Boolean) {
        context.mikaDataStore.edit { it[Keys.SHOW_ACTUAL_ARTWORK_RATIO] = enabled }
    }

    suspend fun updateShowLikeButtonInList(enabled: Boolean) {
        context.mikaDataStore.edit { it[Keys.SHOW_LIKE_BUTTON_IN_LIST] = enabled }
    }

    suspend fun updateTrackSwipeActions(left: TrackSwipeAction? = null, right: TrackSwipeAction? = null) {
        context.mikaDataStore.edit { prefs ->
            left?.let { prefs[Keys.SWIPE_LEFT_ACTION] = it.name }
            right?.let { prefs[Keys.SWIPE_RIGHT_ACTION] = it.name }
        }
    }

    suspend fun updateSwipeToOpenDrawer(enabled: Boolean) {
        context.mikaDataStore.edit { it[Keys.SWIPE_TO_OPEN_DRAWER] = enabled }
    }

    suspend fun updateColorTargets(onApp: Boolean? = null, onPlayer: Boolean? = null) {
        context.mikaDataStore.edit { prefs ->
            onApp?.let { prefs[Keys.APPLY_COLOR_ON_APP] = it }
            onPlayer?.let { prefs[Keys.APPLY_COLOR_ON_PLAYER] = it }
        }
    }

    suspend fun updateAppIconPreset(preset: AppIconPreset, customPath: String? = null) {
        context.mikaDataStore.edit { prefs ->
            prefs[Keys.APP_ICON_PRESET] = preset.name
            customPath?.let { prefs[Keys.CUSTOM_APP_ICON_PATH] = it }
        }
    }

    suspend fun updatePlaylistSort(field: PlaylistSortField, direction: SortDirection) {
        context.mikaDataStore.edit {
            it[Keys.PLAYLIST_SORT_FIELD] = field.name
            it[Keys.PLAYLIST_SORT_DIRECTION] = direction.name
        }
    }

    suspend fun updateFolderListSort(field: FolderSortField, direction: SortDirection) {
        context.mikaDataStore.edit {
            it[Keys.FOLDER_LIST_SORT_FIELD] = field.name
            it[Keys.FOLDER_LIST_SORT_DIRECTION] = direction.name
        }
    }

    suspend fun updateAlbumSort(field: AlbumSortField, direction: SortDirection) {
        context.mikaDataStore.edit {
            it[Keys.ALBUM_SORT_FIELD] = field.name
            it[Keys.ALBUM_SORT_DIRECTION] = direction.name
        }
    }

    suspend fun updateArtistSort(field: ArtistSortField, direction: SortDirection) {
        context.mikaDataStore.edit {
            it[Keys.ARTIST_SORT_FIELD] = field.name
            it[Keys.ARTIST_SORT_DIRECTION] = direction.name
        }
    }

    suspend fun updateShowBottomNavBar(show: Boolean) {
        context.mikaDataStore.edit {
            it[Keys.SHOW_BOTTOM_NAV_BAR] = show
        }
    }

    suspend fun updateBottomNavOrder(order: List<BottomNavDest>) {
        val clean = (order.distinct() + BottomNavDest.entries.filter { it !in order })
        context.mikaDataStore.edit {
            it[Keys.BOTTOM_NAV_ORDER] = clean.joinToString(",") { d -> d.name }
        }
    }

    suspend fun updateHiddenBottomNavItems(hidden: Set<BottomNavDest>) {
        // Always keep at least one destination visible (HOME stays visible if all else hidden)
        val safeHidden = if (hidden.size >= BottomNavDest.entries.size) {
            hidden - BottomNavDest.HOME
        } else hidden
        context.mikaDataStore.edit {
            it[Keys.HIDDEN_BOTTOM_NAV_ITEMS] = safeHidden.joinToString(",") { d -> d.name }
        }
    }

    suspend fun updateAllowSimultaneousPlayback(enabled: Boolean) {
        context.mikaDataStore.edit {
            it[Keys.ALLOW_SIMULTANEOUS_PLAYBACK] = enabled
        }
    }

    suspend fun updateAutoBackupIntervalDays(days: Int) {
        context.mikaDataStore.edit {
            it[Keys.AUTO_BACKUP_INTERVAL_DAYS] = days.coerceIn(0, 90)
        }
    }

    suspend fun updateLastAutoBackupAtMs(timestampMs: Long) {
        context.mikaDataStore.edit {
            it[Keys.LAST_AUTO_BACKUP_AT_MS] = timestampMs
        }
    }

    suspend fun updateBackupLocation(uri: String, label: String) {
        context.mikaDataStore.edit {
            it[Keys.BACKUP_LOCATION_URI] = uri
            it[Keys.BACKUP_LOCATION_LABEL] = label
        }
    }

    suspend fun updateWidgetConfig(config: WidgetConfig) {
        context.mikaDataStore.edit {
            it[Keys.WIDGET_CONFIG_JSON] = widgetJson.encodeToString(config)
        }
    }

    suspend fun updateMiniPlayerConfig(config: MiniPlayerConfig) {
        context.mikaDataStore.edit {
            it[Keys.MINI_PLAYER_CONFIG_JSON] = widgetJson.encodeToString(config)
        }
    }

    suspend fun updateFullPlayerConfig(config: FullPlayerConfig) {
        context.mikaDataStore.edit {
            it[Keys.FULL_PLAYER_CONFIG_JSON] = widgetJson.encodeToString(config)
        }
    }

    suspend fun updateSoundControl(soundControl: SoundControlSettings) {
        context.mikaDataStore.edit {
            it[Keys.SOUND_CONTROL_JSON] = widgetJson.encodeToString(soundControl)
        }
    }

    suspend fun resetAllSettings() {
        context.mikaDataStore.edit { it.clear() }
    }

    suspend fun restoreSettings(restored: AppSettings) {
        val orderedTabs = restored.enabledTabs.distinct().ifEmpty { defaultLibraryTabs }
        val defaultTab = if (restored.defaultTab in orderedTabs) restored.defaultTab else orderedTabs.first()
        val navOrder = (restored.bottomNavOrder.distinct() + BottomNavDest.entries.filter { it !in restored.bottomNavOrder })
        context.mikaDataStore.edit {
            it[Keys.THEME_MODE] = restored.themeMode.name
            it[Keys.ACCENT_COLOR_ID] = restored.accentColorId
            it[Keys.CUSTOM_ACCENT_ARGB] = restored.customAccentArgb
            it[Keys.CUSTOM_DARK_ACCENT_ARGB] = restored.customDarkAccentArgb
            it[Keys.AUTO_COLORING] = restored.autoColoring
            it[Keys.APPLY_COLOR_ON_APP] = restored.applyColorOnApp
            it[Keys.APPLY_COLOR_ON_PLAYER] = restored.applyColorOnPlayer
            it[Keys.PICK_COLORS_WALLPAPER] = restored.pickColorsFromWallpaper
            it[Keys.FORCE_MINI_TRACK_COLORS] = restored.forceMiniPlayerTrackColors
            it[Keys.USE_PITCH_BLACK] = restored.usePitchBlack
            it[Keys.GRADIENT_STYLE] = restored.gradientStyle.name
            it[Keys.GRADIENT_MINI_PLAYER] = restored.applyGradientToMiniPlayer
            it[Keys.GRADIENT_FULL_PLAYER] = restored.applyGradientToFullPlayer
            it[Keys.GRADIENT_SURFACES] = restored.applyGradientToSurfaces
            it[Keys.SORT_FIELD] = restored.sortField.name
            it[Keys.SORT_DIRECTION] = restored.sortDirection.name
            it[Keys.GAPLESS_PLAYBACK] = restored.gaplessPlayback
            it[Keys.SKIP_SILENCE] = restored.skipSilence
            it[Keys.CROSSFADE_ENABLED] = restored.crossfadeEnabled
            it[Keys.CROSSFADE_SEC] = restored.crossfadeDurationSec.coerceIn(0, 12)
            it[Keys.CROSSFADE_DURATION_MS] = restored.crossfadeDurationMs.coerceIn(200, 12000)
            it[Keys.CROSSFADE_TRIGGER_SEC] = restored.crossfadeTriggerLastSec.coerceIn(1, 15)
            it[Keys.CROSSFADE_STYLE] = restored.crossfadeStyle.name
            it[Keys.PLAY_PAUSE_FADE_ENABLED] = restored.playPauseFadeEnabled
            it[Keys.PLAY_FADE_MS] = restored.playFadeDurationMs.coerceIn(0, 3000)
            it[Keys.PAUSE_FADE_MS] = restored.pauseFadeDurationMs.coerceIn(0, 3000)
            it[Keys.JUMP_TO_FIRST_AFTER_QUEUE] = restored.jumpToFirstAfterQueue
            it[Keys.PREVIOUS_BUTTON_REPLAYS] = restored.previousButtonReplays
            it[Keys.RESTORE_MIN_DURATION_MIN] = restored.restorePositionMinDurationMin.coerceIn(0, 60)
            it[Keys.PLAYBACK_SPEED] = restored.playbackSpeed.coerceIn(0.25f, 3.0f)
            it[Keys.PLAYBACK_PITCH] = restored.playbackPitch.coerceIn(0.5f, 2.0f)
            it[Keys.SKIP_INTERVAL_SEC] = restored.skipIntervalSec.coerceIn(3, 60)
            it[Keys.REMEMBER_LAST_POSITION] = restored.rememberLastPosition
            it[Keys.SHOW_LYRICS_BADGES] = restored.showLyricsBadges
            it[Keys.FONT_SCALE] = restored.fontScale.coerceIn(0.85f, 1.30f)
            it[Keys.APP_ICON_SCALE] = restored.appIconScale.coerceIn(0.80f, 1.30f)
            it[Keys.CORNER_RADIUS_DP] = restored.cornerRadiusDp.coerceIn(4, 28)
            it[Keys.SEEK_BAR_STYLE] = restored.seekBarStyle.name
            it[Keys.SHOW_ACTUAL_WAVEFORM] = restored.showActualWaveformSeekbar
            it[Keys.SHOW_LIVE_WAVEFORM] = restored.showLiveWaveformSeekbar
            it[Keys.PLAYBACK_ICON_STYLE] = restored.playbackIconStyle.name
            it[Keys.ENABLED_TABS] = orderedTabs.joinToString(",") { tab -> tab.name }
            it[Keys.DEFAULT_TAB] = defaultTab.name
            it[Keys.TAB_TAP_ACTION] = restored.tabTapAction.name
            it[Keys.TAB_LONG_PRESS_ACTION] = restored.tabLongPressAction.name
            it[Keys.EXCLUDED_FOLDERS] = restored.excludedFolders
            it[Keys.ENABLE_FOLDERS_HIERARCHY] = restored.enableFoldersHierarchy
            it[Keys.SKIP_SINGLE_SUBFOLDER] = restored.skipSingleSubfolder
            it[Keys.MIN_DURATION_SEC] = restored.minDurationSec.coerceIn(0, 300)
            it[Keys.MIN_FILE_SIZE_KB] = restored.minFileSizeKb.coerceIn(0, 10240)
            it[Keys.OBEY_NO_MEDIA] = restored.obeyNoMedia
            it[Keys.AUTO_SCAN_STARTUP] = restored.autoScanOnStartup
            it[Keys.EXTENSION_BLACKLIST] = restored.extensionBlacklist
            it[Keys.GROUP_ARTWORK_BY_ALBUM] = restored.groupArtworkByAlbum
            it[Keys.SHOW_ACTUAL_ARTWORK_RATIO] = restored.showActualArtworkRatio
            it[Keys.SHOW_LIKE_BUTTON_IN_LIST] = restored.showLikeButtonInList
            it[Keys.SWIPE_LEFT_ACTION] = restored.swipeLeftAction.name
            it[Keys.SWIPE_RIGHT_ACTION] = restored.swipeRightAction.name
            it[Keys.SWIPE_TO_OPEN_DRAWER] = restored.swipeToOpenDrawer
            it[Keys.AUTO_SCAN_AUDIO] = restored.autoScanAudio
            it[Keys.AUTO_SCAN_VIDEO] = restored.autoScanVideo
            it[Keys.VIDEO_GRID_VIEW] = restored.videoGridView
            it[Keys.ENABLE_BLUR_EFFECT] = restored.enableBlurEffect
            it[Keys.ENABLE_GLOW_EFFECT] = restored.enableGlowEffect
            it[Keys.ENABLE_PARALLAX_EFFECT] = restored.enableParallaxEffect
            it[Keys.BACKGROUND_EFFECT] = restored.backgroundEffect.name
            it[Keys.PLAYER_BG_MODE] = restored.playerBackgroundMode.name
            it[Keys.PLAYER_BG_VIGNETTE] = restored.playerBackgroundVignette
            it[Keys.PLAYER_BG_ANIMATED] = restored.playerBackgroundAnimated
            it[Keys.PLAYER_BG_COLOR_EXPANDED] = restored.playerBackgroundColorWhenExpanded
            it[Keys.DOUBLE_TAP_LYRICS] = restored.doubleTapToToggleLyrics
            it[Keys.ARTWORK_TAP_ACTION] = restored.artworkTapAction.name
            it[Keys.ARTWORK_LONG_PRESS_ACTION] = restored.artworkLongPressAction.name
            it[Keys.PLAYLIST_SORT_FIELD] = restored.playlistSortField.name
            it[Keys.PLAYLIST_SORT_DIRECTION] = restored.playlistSortDirection.name
            it[Keys.FOLDER_LIST_SORT_FIELD] = restored.folderListSortField.name
            it[Keys.FOLDER_LIST_SORT_DIRECTION] = restored.folderListSortDirection.name
            it[Keys.ALBUM_SORT_FIELD] = restored.albumSortField.name
            it[Keys.ALBUM_SORT_DIRECTION] = restored.albumSortDirection.name
            it[Keys.ARTIST_SORT_FIELD] = restored.artistSortField.name
            it[Keys.ARTIST_SORT_DIRECTION] = restored.artistSortDirection.name
            it[Keys.SHOW_BOTTOM_NAV_BAR] = restored.showBottomNavBar
            it[Keys.BOTTOM_NAV_ORDER] = navOrder.joinToString(",") { d -> d.name }
            it[Keys.HIDDEN_BOTTOM_NAV_ITEMS] = restored.hiddenBottomNavItems.joinToString(",") { d -> d.name }
            it[Keys.ALLOW_SIMULTANEOUS_PLAYBACK] = restored.allowSimultaneousPlayback
            it[Keys.AUTO_BACKUP_INTERVAL_DAYS] = restored.autoBackupIntervalDays.coerceIn(0, 90)
            it[Keys.BACKUP_LOCATION_LABEL] = restored.backupLocationLabel.ifBlank { "/mika/backup" }
            it[Keys.APP_ICON_PRESET] = restored.appIconPreset.name
            it[Keys.CUSTOM_APP_ICON_PATH] = restored.customAppIconPath
            it[Keys.WIDGET_CONFIG_JSON] = widgetJson.encodeToString(restored.widgetConfig)
            it[Keys.MINI_PLAYER_CONFIG_JSON] = widgetJson.encodeToString(restored.miniPlayerConfig)
            it[Keys.FULL_PLAYER_CONFIG_JSON] = widgetJson.encodeToString(restored.fullPlayerConfig)
            it[Keys.SOUND_CONTROL_JSON] = widgetJson.encodeToString(restored.soundControl)
        }
    }
}
