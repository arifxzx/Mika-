package com.example.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

enum class CrossfadeStyle(val label: String) {
    DEFAULT("Default"),
    EQUAL_POWER("Equal Power"),
    LINEAR("Linear"),
    SMOOTH("Smooth Curve")
}

enum class BackgroundEffect(val label: String) {
    NONE("None"),
    AUTO_SEASON("Auto (Season)"),
    SNOW("Snow"),
    SAKURA("Sakura"),
    RAIN("Rain"),
    FIREFLIES("Fireflies"),
    AUTUMN_LEAVES("Leaves"),
    STARS("Stars")
}

enum class PlayerBackgroundMode(val label: String) {
    NONE("None"),
    ARTWORK("Artwork"),
    CUSTOM("Custom"),
    MESH_GRADIENT("Mesh gradient"),
    AMBIENT_LIGHTS("Ambient lights"),
    BOKEH("Bokeh")
}

enum class ArtworkGestureAction(val label: String) {
    NONE("None"),
    PLAY_PAUSE("Play / Pause"),
    OPEN_LYRICS("Lyrics"),
    ARTWORK_OPEN("Artwork (Open)"),
    NEXT_TRACK("Next Track"),
    INFO("Info"),
    EQUALIZER("Equalizer"),
    FAVORITE("Favorite"),
    SHARE("Share")
}

enum class VisualizerStyle(val label: String) {
    BARS("Frequency Bars"),
    WAVEFORM("Smooth Wave"),
    CIRCULAR("Circular Halo"),
    MIRROR_PEAKS("Mirror Peaks"),
    DOT_MATRIX("Dot Matrix")
}

enum class BottomNavDest(val label: String) {
    HOME("Home"),
    FOLDERS("Folders"),
    LIBRARY("Library"),
    VIDEOS("Videos")
}

enum class SoundEffectType(val label: String) {
    BASS_ENHANCER("Bass enhancer"),
    REVERB("Reverb"),
    VIRTUAL_SURROUND("Virtual surround"),
    EIGHT_D("8D audio"),
    INSTRUMENTAL("Instrumental"),
    COMPRESSOR("Compressor"),
    CROSSFEED("Crossfeed"),
    ECHO("Echo"),
    TUBE_WARMTH("Tube warmth"),
    CHORUS("Chorus")
}

@Serializable
@Immutable
data class SoundControlSettings(
    val pitchSemitones: Float = 0f,
    val linkPitchToSpeed: Boolean = false,
    val playerVolumePct: Float = 100f,
    val loudnessEnhancerEnabled: Boolean = false,
    val preAmpDb: Float = 0f,
    val monoAudio: Boolean = false,
    val reverbEnabled: Boolean = false,
    val eightDEnabled: Boolean = false,
    val instrumentalEnabled: Boolean = false,
    val compressorEnabled: Boolean = false,
    val crossfeedEnabled: Boolean = false,
    val echoEnabled: Boolean = false,
    val tubeWarmthEnabled: Boolean = false,
    val chorusEnabled: Boolean = false
)

enum class WidgetPreviewSize(val label: String) {
    WIDE("Wide"),
    LARGE("Large"),
    SMALL("Small")
}

enum class WidgetThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

enum class WidgetBackgroundStyle(val label: String) {
    SOLID("Solid"),
    GRADIENT("Gradient"),
    BLURRED_ART("Blurred art")
}

enum class WidgetArtworkEffect(val label: String) {
    NONE("None"),
    GLOW("Glow"),
    SHADOW("Shadow")
}

enum class WidgetArrangement(val label: String) {
    AUTO("Auto"),
    ROW("Row"),
    CARD("Card"),
    COMPACT("Compact")
}

enum class WidgetTapAction(val label: String) {
    OPEN_APP("Open app"),
    PLAY_PAUSE("Play / Pause"),
    NOTHING("Nothing")
}

@Serializable
@Immutable
data class WidgetConfig(
    val previewSize: WidgetPreviewSize = WidgetPreviewSize.WIDE,
    val theme: WidgetThemeMode = WidgetThemeMode.DARK,
    val colorsFromArtwork: Boolean = true,
    val backgroundStyle: WidgetBackgroundStyle = WidgetBackgroundStyle.GRADIENT,
    val artworkEffect: WidgetArtworkEffect = WidgetArtworkEffect.NONE,
    val backgroundOpacity: Float = 1.0f,
    val cornerRadiusDp: Int = 18,
    val artworkRoundingPct: Int = 18,
    val arrangement: WidgetArrangement = WidgetArrangement.AUTO,
    val contentSizePct: Int = 100,
    val artworkSizePct: Int = 100,
    val showArtwork: Boolean = true,
    val showTitle: Boolean = true,
    val showSubtitle: Boolean = false,
    val showFavourite: Boolean = false,
    val showPrevious: Boolean = true,
    val showPlayPause: Boolean = true,
    val showNext: Boolean = true,
    val showRepeat: Boolean = false,
    val showShuffle: Boolean = false,
    val showStop: Boolean = true,
    val widgetTapAction: WidgetTapAction = WidgetTapAction.OPEN_APP,
    val artworkTapAction: WidgetTapAction = WidgetTapAction.PLAY_PAUSE
)

enum class MiniPlayerStyle(val label: String, val description: String) {
    CLASSIC("Classic Dock", "Frosted glass dock with progress ring"),
    FLOATING_PILL("Floating Pill", "Detached pill with bottom progress bar"),
    VINYL_SPIN("Vinyl Spin", "Rotating disc artwork with warm gradient"),
    ARTWORK_BANNER("Artwork Banner", "Full-bleed blurred album art backdrop"),
    MINIMAL("Minimalist Bar", "Progress-filled bar with favorite button")
}

@Serializable
@Immutable
data class MiniPlayerConfig(
    val style: MiniPlayerStyle = MiniPlayerStyle.CLASSIC,
    val cornerRadiusDp: Int = 22,
    val artworkSizeDp: Int = 58,
    val artworkCornerDp: Int = 15,
    val showPrevNext: Boolean = true,
    val showFavorite: Boolean = false,
    val showProgressIndicator: Boolean = true,
    val showArtist: Boolean = true,
    val compactHeight: Boolean = false,
    val swipeToChangeTrack: Boolean = true
)

enum class FullPlayerStyle(val label: String, val description: String) {
    CLASSIC("Mika Signature", "Balanced layout with glow, quick icons & visualizer"),
    VINYL("Vinyl Turntable", "Spinning vinyl disc with grooves & tonearm"),
    EDITORIAL("Card Editorial", "Left-aligned title, quick pills & crisp card"),
    IMMERSIVE("Immersive Poster", "Full-bleed faded art & glass control dock"),
    ZEN("Minimalist Zen", "Clean distraction-free focus on art & audio")
}

enum class SeekBarStyle(val label: String, val description: String) {
    SLEEK_CAPSULE("Sleek Capsule", "Gradient capsule track with glowing ring thumb"),
    WAVEFORM_BARS("Waveform Bars", "Audio spectrum bars lighting up along playback"),
    REAL_WAVEFORM("Track Waveform", "Actual audio peaks rendered across track"),
    LIVE_FLOW_WAVEFORM("Live Flow Bars", "Poweramp-style bars flowing right to left"),
    WAVY_PULSE("Wavy Pulse", "Flowing sinusoidal progress wave & capsule thumb"),
    MINIMAL_GLOW("Minimal Glow", "Razor-thin neon beam with crisp ring handle"),
    TICK_RULER("Tick Ruler", "Precision graduated ruler ticks & indicator pill")
}

enum class PlaybackIconStyle(val label: String) {
    NORMAL("Normal"),
    BROKEN("Broken"),
    OUTLINE("Outline"),
    CAPSULE("Capsule"),
    MINIMAL("Minimal")
}

enum class TrackSwipeAction(val label: String) {
    NONE("Off"),
    PLAY_NEXT("Play Next"),
    ADD_TO_QUEUE("Add to Queue"),
    FAVORITE("Like / Unlike"),
    ADD_TO_PLAYLIST("Add to Playlist"),
    RATE_MOOD("Mood & Rating"),
    HIDE("Hide Track"),
    TRASH("Move to Trash")
}

enum class TabTapAction(val label: String) {
    OPEN_TAB("Switch Tab"),
    SHUFFLE_PLAY("Shuffle Tab"),
    SCROLL_TOP("Scroll Top")
}

enum class TabLongPressAction(val label: String) {
    CUSTOMIZE_TABS("Customize Tabs"),
    SORT_MENU("Sort Menu"),
    SHUFFLE_PLAY("Shuffle Tab"),
    FILTER_INFO("Track Count")
}

enum class AppIconPreset(val label: String, val subtitle: String) {
    AURORA_GLASS("Aurora Glass", "Glowing cyan M"),
    MONO_OUTLINE("Mono Minimal", "Clean monochrome"),
    VIOLET_LINE("Violet Line", "Deep navy violet"),
    NEON_WAVE("Neon Wave", "3D pink & blue"),
    CLASSIC("Mika Classic", "Original icon"),
    CUSTOM("From Storage", "Pick your own")
}

@Serializable
@Immutable
data class FullPlayerConfig(
    val style: FullPlayerStyle = FullPlayerStyle.CLASSIC,
    val visualizerStyle: VisualizerStyle = VisualizerStyle.BARS,
    val seekBarStyle: SeekBarStyle = SeekBarStyle.SLEEK_CAPSULE,
    val showSimpleLyricsInVisualizer: Boolean = false,
    val artworkCornerDp: Int = 24,
    val artworkBorderDp: Int = 6,
    val showQueuePosition: Boolean = true,
    val showArtistInTopBar: Boolean = true,
    val showQuickActionsRow: Boolean = true,
    val showSkipChips: Boolean = true,
    val showBottomUtilityBar: Boolean = true,
    val showVisualizerPill: Boolean = true,
    val centerMetadata: Boolean = true,
    val controlsScale: Float = 1.0f,
    val showShuffleRepeat: Boolean = true
)
