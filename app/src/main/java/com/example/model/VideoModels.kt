package com.example.model

import androidx.compose.runtime.Immutable

enum class BottomNavDestination(val label: String) {
    HOME("Home"),
    MUSIC("Music"),
    VIDEO("Video"),
    FOLDERS("Folders")
}

enum class VideoSortField(val label: String) {
    TITLE("Name"),
    DURATION("Duration"),
    DATE_ADDED("Recent"),
    SIZE("Size")
}

enum class VideoResizeMode(val label: String) {
    FIT("Fit"),
    FILL("Fill"),
    ZOOM("Zoom"),
    STRETCH("Stretch")
}

@Immutable
data class VideoItem(
    val id: String,
    val contentUri: String,
    val title: String,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val mimeType: String,
    val folderName: String,
    val dateAddedSec: Long,
    val thumbnailPath: String? = null
)

@Immutable
data class VideoPlaybackUiState(
    val currentVideo: VideoItem? = null,
    val queue: List<VideoItem> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val resizeMode: VideoResizeMode = VideoResizeMode.FIT,
    val isFullScreenOpen: Boolean = false,
    val isControlsLocked: Boolean = false,
    val isMuted: Boolean = false,
    val isLooping: Boolean = false
)
