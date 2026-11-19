package com.example.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

enum class ThemeMode(val label: String) {
    DARK("Dark Mode"),
    LIGHT("Light Mode"),
    SYSTEM("System Default")
}

@Immutable
data class AccentColorOption(
    val id: String,
    val name: String,
    val color: Color,
    val secondaryColor: Color
)

@Immutable
data class HarmonicAccentShades(
    val primary: Color,
    val secondary: Color,
    val container: Color,
    val gradientStart: Color,
    val gradientEnd: Color
)

object AccentPalette {
    val options: List<AccentColorOption> = listOf(
        AccentColorOption("electric_blue", "Electric Blue", Color(0xFF1E88FF), Color(0xFF38BDF8)),
        AccentColorOption("cyan_pulse", "Cyan Pulse", Color(0xFF00BCD4), Color(0xFF22D3EE)),
        AccentColorOption("indigo_glow", "Indigo Glow", Color(0xFF6366F1), Color(0xFF818CF8)),
        AccentColorOption("violet_bloom", "Violet Bloom", Color(0xFF8B5CF6), Color(0xFFA78BFA)),
        AccentColorOption("rose_coral", "Rose Coral", Color(0xFFF43F5E), Color(0xFFFB7185)),
        AccentColorOption("crimson_ember", "Crimson Ember", Color(0xFFEF4444), Color(0xFFF87171)),
        AccentColorOption("amber_gold", "Amber Gold", Color(0xFFF59E0B), Color(0xFFFBBF24)),
        AccentColorOption("emerald_mint", "Emerald Mint", Color(0xFF10B981), Color(0xFF34D399)),
        AccentColorOption("teal_wave", "Teal Wave", Color(0xFF14B8A6), Color(0xFF2DD4BF)),
        AccentColorOption("sunset_orange", "Sunset Orange", Color(0xFFF97316), Color(0xFFFB923C))
    )

    fun byId(id: String): AccentColorOption =
        options.firstOrNull { it.id == id } ?: options.first()

    fun deriveCustomOption(argb: Long): AccentColorOption {
        val base = if (argb != 0L) Color(argb.toInt()) else options.first().color
        val shades = deriveHarmonicThemeColors(base, isDark = true)
        return AccentColorOption(
            id = "custom",
            name = "Custom Color",
            color = base,
            secondaryColor = shades.secondary
        )
    }

    /**
     * Generates refined multi-shades of a SINGLE harmonic hue tuned separately
     * for Dark vs Light mode so colors remain balanced, velvety, and legible.
     */
    fun deriveHarmonicThemeColors(seedColor: Color, isDark: Boolean): HarmonicAccentShades {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(seedColor.toArgb(), hsv)
        val hue = hsv[0]
        val rawSat = hsv[1]
        val isNearNeutral = rawSat < 0.08f

        return if (isDark) {
            val primarySat = if (isNearNeutral) 0.16f else rawSat.coerceIn(0.42f, 0.78f)
            val primaryVal = if (isNearNeutral) 0.84f else 0.92f
            val secondarySat = if (isNearNeutral) 0.12f else (primarySat * 0.68f).coerceIn(0.24f, 0.58f)
            val containerSat = if (isNearNeutral) 0.14f else (primarySat * 0.56f).coerceIn(0.20f, 0.46f)
            val gradSat = if (isNearNeutral) 0.12f else (primarySat * 0.65f).coerceIn(0.22f, 0.56f)

            HarmonicAccentShades(
                primary = Color.hsv(hue, primarySat, primaryVal),
                secondary = Color.hsv(hue, secondarySat, 0.96f),
                container = Color.hsv(hue, containerSat, 0.29f),
                gradientStart = Color.hsv(hue, gradSat, 0.12f),
                gradientEnd = Color.hsv(hue, (gradSat * 1.12f).coerceIn(0.14f, 0.62f), 0.27f)
            )
        } else {
            val brightHuePenalty = if (hue in 40f..170f) 0.14f else 0f
            val primarySat = if (isNearNeutral) 0.20f else rawSat.coerceIn(0.48f, 0.82f)
            val primaryVal = (if (isNearNeutral) 0.40f else 0.60f - brightHuePenalty).coerceIn(0.36f, 0.62f)
            val secondarySat = if (isNearNeutral) 0.16f else (primarySat * 0.72f).coerceIn(0.30f, 0.60f)
            val secondaryVal = (primaryVal + 0.12f).coerceIn(0.46f, 0.74f)

            HarmonicAccentShades(
                primary = Color.hsv(hue, primarySat, primaryVal),
                secondary = Color.hsv(hue, secondarySat, secondaryVal),
                container = Color.hsv(hue, (primarySat * 0.24f).coerceIn(0.08f, 0.22f), 0.94f),
                gradientStart = Color.hsv(hue, (primarySat * 0.10f).coerceIn(0.03f, 0.10f), 0.98f),
                gradientEnd = Color.hsv(hue, (primarySat * 0.22f).coerceIn(0.08f, 0.20f), 0.91f)
            )
        }
    }

    fun resolve(id: String, customArgb: Long): AccentColorOption {
        return if (id == "custom" && customArgb != 0L) {
            deriveCustomOption(customArgb)
        } else {
            byId(id)
        }
    }
}

enum class GradientStyle(val label: String) {
    NONE("Solid Surface"),
    SUBTLE_LINEAR("Subtle Linear"),
    RADIAL_GLOW("Radial Glow"),
    AURORA_DUAL("Aurora Blend")
}

@Immutable
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val accentColorId: String = "electric_blue",
    val customAccentArgb: Long = 0L,
    val autoColoring: Boolean = false,
    val applyColorOnApp: Boolean = true,
    val applyColorOnPlayer: Boolean = true,
    val gradientStyle: GradientStyle = GradientStyle.SUBTLE_LINEAR,
    val applyGradientToMiniPlayer: Boolean = true,
    val applyGradientToFullPlayer: Boolean = true,
    val applyGradientToSurfaces: Boolean = true,
    val sortField: SortField = SortField.TITLE,
    val sortDirection: SortDirection = SortDirection.ASCENDING,
    val gaplessPlayback: Boolean = true,
    val crossfadeDurationSec: Int = 0,
    val playbackSpeed: Float = 1.0f,
    val playbackPitch: Float = 1.0f,
    val skipIntervalSec: Int = 5,
    val rememberLastPosition: Boolean = true,
    val showLyricsBadges: Boolean = true,
    val fontScale: Float = 1.0f,
    val appIconScale: Float = 1.0f,
    val cornerRadiusDp: Int = 16,
    val seekBarStyle: SeekBarStyle = SeekBarStyle.SLEEK_CAPSULE,
    val showActualWaveformSeekbar: Boolean = false,
    val showLiveWaveformSeekbar: Boolean = false,
    val playbackIconStyle: PlaybackIconStyle = PlaybackIconStyle.NORMAL,
    val enabledTabs: List<MainTab> = listOf(
        MainTab.SONGS,
        MainTab.PLAYLISTS,
        MainTab.ALBUMS,
        MainTab.ARTISTS
    ),
    val defaultTab: MainTab = MainTab.SONGS,
    val tabTapAction: TabTapAction = TabTapAction.OPEN_TAB,
    val tabLongPressAction: TabLongPressAction = TabLongPressAction.CUSTOMIZE_TABS,
    val excludedFolders: Set<String> = emptySet(),
    val minDurationSec: Int = 0,
    val minFileSizeKb: Int = 0,
    val obeyNoMedia: Boolean = true,
    val autoScanOnStartup: Boolean = true,
    val extensionBlacklist: Set<String> = emptySet(),
    val groupArtworkByAlbum: Boolean = false,
    val showActualArtworkRatio: Boolean = false,
    val showLikeButtonInList: Boolean = false,
    val swipeLeftAction: TrackSwipeAction = TrackSwipeAction.ADD_TO_QUEUE,
    val swipeRightAction: TrackSwipeAction = TrackSwipeAction.FAVORITE,
    val swipeToOpenDrawer: Boolean = true,
    // Independent per-tab sorting
    val playlistSortField: PlaylistSortField = PlaylistSortField.CREATED_AT,
    val playlistSortDirection: SortDirection = SortDirection.DESCENDING,
    val folderListSortField: FolderSortField = FolderSortField.NAME,
    val folderListSortDirection: SortDirection = SortDirection.ASCENDING,
    val albumSortField: AlbumSortField = AlbumSortField.NAME,
    val albumSortDirection: SortDirection = SortDirection.ASCENDING,
    val artistSortField: ArtistSortField = ArtistSortField.NAME,
    val artistSortDirection: SortDirection = SortDirection.ASCENDING,
    // Customizable bottom navigation bar
    val showBottomNavBar: Boolean = true,
    val bottomNavOrder: List<BottomNavDest> = BottomNavDest.entries,
    val hiddenBottomNavItems: Set<BottomNavDest> = emptySet(),
    // Simultaneous / mixed playback with other player apps
    val allowSimultaneousPlayback: Boolean = false,
    // Auto Scan & Manual Folder additions
    val autoScanAudio: Boolean = true,
    val autoScanVideo: Boolean = true,
    val videoGridView: Boolean = false,
    // Backup & Restore schedule & default location
    val autoBackupIntervalDays: Int = 0,
    val lastAutoBackupAtMs: Long = 0L,
    val backupLocationUri: String = "",
    val backupLocationLabel: String = "/mika/backup",
    // App Icon Customization
    val appIconPreset: AppIconPreset = AppIconPreset.AURORA_GLASS,
    val customAppIconPath: String = "",
    // Included SAF audio & video folder URIs
    val includedFolderUris: Set<String> = emptySet(),
    val videoFolderUris: Set<String> = emptySet(),
    // Folders Hierarchy options
    val enableFoldersHierarchy: Boolean = true,
    val skipSingleSubfolder: Boolean = true,
    // Playback & Audio Effects
    val skipSilence: Boolean = false,
    val crossfadeEnabled: Boolean = false,
    val crossfadeDurationMs: Int = 500,
    val crossfadeTriggerLastSec: Int = 5,
    val crossfadeStyle: CrossfadeStyle = CrossfadeStyle.DEFAULT,
    val playPauseFadeEnabled: Boolean = false,
    val playFadeDurationMs: Int = 500,
    val pauseFadeDurationMs: Int = 500,
    val soundControl: SoundControlSettings = SoundControlSettings(),
    // Queue & Seek Behavior
    val jumpToFirstAfterQueue: Boolean = true,
    val previousButtonReplays: Boolean = true,
    val restorePositionMinDurationMin: Int = 10,
    // Visual Effects & Player Background
    val enableBlurEffect: Boolean = false,
    val enableGlowEffect: Boolean = true,
    val enableParallaxEffect: Boolean = false,
    val backgroundEffect: BackgroundEffect = BackgroundEffect.NONE,
    val playerBackgroundMode: PlayerBackgroundMode = PlayerBackgroundMode.MESH_GRADIENT,
    val playerBackgroundCustomUri: String = "",
    val playerBackgroundVignette: Boolean = true,
    val playerBackgroundAnimated: Boolean = true,
    val playerBackgroundColorWhenExpanded: Boolean = true,
    // Color & Theme Extras
    val pickColorsFromWallpaper: Boolean = false,
    val forceMiniPlayerTrackColors: Boolean = false,
    val usePitchBlack: Boolean = false,
    val customDarkAccentArgb: Long = 0L,
    // Artwork Gestures
    val doubleTapToToggleLyrics: Boolean = true,
    val artworkTapAction: ArtworkGestureAction = ArtworkGestureAction.ARTWORK_OPEN,
    val artworkLongPressAction: ArtworkGestureAction = ArtworkGestureAction.INFO,
    // Home Screen Widget Configuration
    val widgetConfig: WidgetConfig = WidgetConfig(),
    // Mini Player & Full Screen Player Customization
    val miniPlayerConfig: MiniPlayerConfig = MiniPlayerConfig(),
    val fullPlayerConfig: FullPlayerConfig = FullPlayerConfig()
)
