package com.example.playback

import android.content.Context
import android.net.Uri
import android.view.SurfaceView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import com.example.model.VideoItem
import com.example.model.VideoPlaybackUiState
import com.example.model.VideoResizeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class VideoPlaybackController(
    private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var exoPlayer: ExoPlayer? = null
    private var attachedSurfaceView: SurfaceView? = null
    private var positionTickerJob: Job? = null

    private val _state = MutableStateFlow(VideoPlaybackUiState())
    val state: StateFlow<VideoPlaybackUiState> = _state.asStateFlow()

    private val _videoAspectRatio = MutableStateFlow(16f / 9f)
    val videoAspectRatio: StateFlow<Float> = _videoAspectRatio.asStateFlow()

    var onBeforeVideoPlay: (() -> Unit)? = null

    private fun ensurePlayer(): ExoPlayer {
        exoPlayer?.let { return it }
        val attrs = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()
        return ExoPlayer.Builder(context)
            .setAudioAttributes(attrs, true)
            .setHandleAudioBecomingNoisy(true)
            .build().also { player ->
                player.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _state.value = _state.value.copy(isPlaying = isPlaying)
                        if (isPlaying) {
                            onBeforeVideoPlay?.invoke()
                            startTicker()
                        } else {
                            stopTicker()
                            syncProgress(player)
                        }
                    }

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        val idx = player.currentMediaItemIndex
                        val list = _state.value.queue
                        val current = list.getOrNull(idx)
                        if (current != null) {
                            val ratio = if (current.width > 0 && current.height > 0) {
                                current.width.toFloat() / current.height.toFloat()
                            } else _videoAspectRatio.value
                            _videoAspectRatio.value = ratio
                            _state.value = _state.value.copy(
                                currentVideo = current,
                                currentIndex = idx,
                                durationMs = current.durationMs.coerceAtLeast(player.duration.coerceAtLeast(0L))
                            )
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        syncProgress(player)
                    }

                    override fun onVideoSizeChanged(videoSize: VideoSize) {
                        if (videoSize.width > 0 && videoSize.height > 0) {
                            val ratio = (videoSize.width * videoSize.pixelWidthHeightRatio) / videoSize.height.toFloat()
                            if (ratio > 0.1f) {
                                _videoAspectRatio.value = ratio
                            }
                        }
                    }
                })
                attachedSurfaceView?.let { player.setVideoSurfaceView(it) }
                exoPlayer = player
            }
    }

    fun attachSurfaceView(surfaceView: SurfaceView) {
        attachedSurfaceView = surfaceView
        exoPlayer?.setVideoSurfaceView(surfaceView)
    }

    fun detachSurfaceView(surfaceView: SurfaceView) {
        if (attachedSurfaceView === surfaceView) {
            exoPlayer?.clearVideoSurfaceView(surfaceView)
            attachedSurfaceView = null
        }
    }

    fun playVideo(video: VideoItem, queue: List<VideoItem> = listOf(video), openFullScreen: Boolean = true) {
        onBeforeVideoPlay?.invoke()
        val list = if (queue.isEmpty()) listOf(video) else queue
        val startIndex = list.indexOfFirst { it.id == video.id }.coerceAtLeast(0)
        val player = ensurePlayer()
        val mediaItems = list.map { MediaItem.fromUri(Uri.parse(it.contentUri)) }

        if (video.width > 0 && video.height > 0) {
            _videoAspectRatio.value = video.width.toFloat() / video.height.toFloat()
        }

        _state.value = _state.value.copy(
            currentVideo = video,
            queue = list,
            currentIndex = startIndex,
            positionMs = 0L,
            durationMs = video.durationMs,
            isFullScreenOpen = openFullScreen,
            isControlsLocked = false
        )
        player.setMediaItems(mediaItems, startIndex, 0L)
        player.playbackParameters = PlaybackParameters(_state.value.playbackSpeed)
        player.prepare()
        player.play()
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            onBeforeVideoPlay?.invoke()
            if (player.playbackState == Player.STATE_ENDED) {
                player.seekTo(0L)
            }
            player.play()
        }
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun seekTo(positionMs: Long) {
        val player = exoPlayer ?: return
        val clamped = positionMs.coerceAtLeast(0L)
        player.seekTo(clamped)
        _state.value = _state.value.copy(positionMs = clamped)
    }

    fun seekRelative(deltaMs: Long) {
        val player = exoPlayer ?: return
        val duration = player.duration.coerceAtLeast(_state.value.durationMs).coerceAtLeast(0L)
        val target = (player.currentPosition + deltaMs).coerceIn(0L, duration.coerceAtLeast(Long.MAX_VALUE))
        seekTo(target)
    }

    fun skipNext() {
        val player = exoPlayer ?: return
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        }
    }

    fun skipPrevious() {
        val player = exoPlayer ?: return
        if (player.currentPosition > 3000L || !player.hasPreviousMediaItem()) {
            player.seekTo(0L)
        } else {
            player.seekToPreviousMediaItem()
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        val safe = speed.coerceIn(0.25f, 3.0f)
        exoPlayer?.playbackParameters = PlaybackParameters(safe)
        _state.value = _state.value.copy(playbackSpeed = safe)
    }

    fun toggleMute() {
        val next = !_state.value.isMuted
        exoPlayer?.volume = if (next) 0f else 1f
        _state.value = _state.value.copy(isMuted = next)
    }

    fun toggleLoop() {
        val next = !_state.value.isLooping
        exoPlayer?.repeatMode = if (next) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        _state.value = _state.value.copy(isLooping = next)
    }

    fun skipToIndex(index: Int) {
        val player = exoPlayer ?: return
        val list = _state.value.queue
        if (index in list.indices) {
            player.seekToDefaultPosition(index)
            player.play()
        }
    }

    fun cycleResizeMode() {
        val modes = VideoResizeMode.entries
        val next = modes[(_state.value.resizeMode.ordinal + 1) % modes.size]
        _state.value = _state.value.copy(resizeMode = next)
    }

    fun setFullScreen(open: Boolean) {
        _state.value = _state.value.copy(isFullScreenOpen = open)
    }

    fun toggleControlsLocked() {
        _state.value = _state.value.copy(isControlsLocked = !_state.value.isControlsLocked)
    }

    fun stopAndDismiss() {
        stopTicker()
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
        _state.value = VideoPlaybackUiState()
    }

    fun release() {
        stopTicker()
        exoPlayer?.release()
        exoPlayer = null
        attachedSurfaceView = null
    }

    private fun startTicker() {
        if (positionTickerJob?.isActive == true) return
        positionTickerJob = scope.launch {
            while (isActive) {
                exoPlayer?.let { syncProgress(it) }
                delay(200L)
            }
        }
    }

    private fun stopTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = null
    }

    private fun syncProgress(player: Player) {
        val rawDur = player.duration.coerceAtLeast(0L)
        val cur = _state.value
        val fallbackDur = cur.currentVideo?.durationMs ?: 0L
        val dur = if (rawDur > 0L) rawDur else fallbackDur
        val pos = player.currentPosition.coerceAtLeast(0L)
        if (cur.positionMs != pos || cur.durationMs != dur) {
            _state.value = cur.copy(positionMs = pos, durationMs = dur)
        }
    }
}
