package com.example.model

import androidx.compose.runtime.Immutable

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

enum class EqPreset(val label: String) {
    NORMAL("Normal"),
    CLASSICAL("Classical"),
    DANCE("Dance"),
    FLAT("Flat"),
    HIP_HOP("Hip-Hop"),
    JAZZ("Jazz"),
    POP("Pop"),
    ROCK("Rock"),
    CUSTOM("Custom")
}

@Immutable
data class EqualizerState(
    val enabled: Boolean = false,
    val preset: EqPreset = EqPreset.NORMAL,
    val bandFrequenciesHz: List<Int> = listOf(60, 230, 910, 3600, 14000),
    val bandLevelsMb: List<Int> = listOf(0, 0, 0, 0, 0),
    val minBandLevelMb: Int = -1500,
    val maxBandLevelMb: Int = 1500,
    val bassBoostStrength: Int = 0,
    val virtualizerStrength: Int = 0,
    val isSupported: Boolean = true
)

@Immutable
data class SleepTimerState(
    val isActive: Boolean = false,
    val remainingMs: Long = 0L,
    val stopAtEndOfTrack: Boolean = false,
    val totalConfiguredMs: Long = 0L,
    val fadeOutEnabled: Boolean = true
)

@Immutable
data class PlaybackUiState(
    val currentSong: Song? = null,
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val playbackSpeed: Float = 1.0f,
    val playbackPitch: Float = 1.0f,
    val errorMessage: String? = null
)

@Immutable
data class PlaybackProgressState(
    val positionMs: Long = 0L,
    val durationMs: Long = 0L
)
