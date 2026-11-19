package com.example.playback

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.data.repository.MusicRepository
import com.example.media.ArtworkManager
import com.example.model.AppSettings
import com.example.model.EqPreset
import com.example.model.EqualizerState
import com.example.model.PlaybackProgressState
import com.example.model.PlaybackUiState
import com.example.model.RepeatMode
import com.example.model.SleepTimerState
import com.example.model.Song
import com.example.model.SoundControlSettings
import com.example.widget.MikaMusicWidgetProvider
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.pow

class PlaybackController(
    private val context: Context,
    private val repository: MusicRepository,
    private val audioEffectsController: AudioEffectsController
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private val _playbackState = MutableStateFlow(PlaybackUiState())
    val playbackState: StateFlow<PlaybackUiState> = _playbackState.asStateFlow()

    private val _playbackProgress = MutableStateFlow(PlaybackProgressState())
    val playbackProgress: StateFlow<PlaybackProgressState> = _playbackProgress.asStateFlow()

    private val _sleepTimerState = MutableStateFlow(SleepTimerState())
    val sleepTimerState: StateFlow<SleepTimerState> = _sleepTimerState.asStateFlow()

    val equalizerState: StateFlow<EqualizerState> = audioEffectsController.state

    private var queueSongs: MutableList<Song> = mutableListOf()
    private var positionTickerJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var playPauseFadeJob: Job? = null
    private var hasRestoredSession = false
    private var pendingQueueRequest: PendingQueueRequest? = null
    private var seekHoldUntilElapsedMs: Long = 0L
    @Volatile
    private var cachedSettings: AppSettings = AppSettings()

    init {
        scope.launch {
            repository.settingsFlow.collect { cachedSettings = it }
        }
    }

    var onBeforeAudioPlay: (() -> Unit)? = null

    fun pausePlayback() {
        playPauseFadeJob?.cancel()
        mediaController?.volume = 1f
        mediaController?.pause()
    }

    fun stopPlaybackAndClear() {
        val player = mediaController ?: return
        playPauseFadeJob?.cancel()
        player.volume = 1f
        AudioSpectrumAnalyzer.reset()
        stopPositionTicker()
        player.pause()
        player.seekTo(0L)
        _playbackState.value = _playbackState.value.copy(isPlaying = false)
        MikaMusicWidgetProvider.requestWidgetUpdate(context)
    }

    private data class PendingQueueRequest(
        val songs: List<Song>,
        val startIndex: Int,
        val shuffle: Boolean,
        val playlistId: Long?
    )

    fun connect() {
        if (controllerFuture != null) return
        val token = SessionToken(context, ComponentName(context, MikaPlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        controllerFuture = future
        future.addListener({
            val controller = runCatching { future.get() }.getOrNull() ?: return@addListener
            mediaController = controller
            attachPlayerListener(controller)
            syncStateFromPlayer()

            val pending = pendingQueueRequest
            if (pending != null) {
                pendingQueueRequest = null
                hasRestoredSession = true
                playQueue(pending.songs, pending.startIndex, pending.shuffle, pending.playlistId)
            } else {
                restoreSavedPlaybackIfEmpty()
            }
        }, MoreExecutors.directExecutor())
    }

    private fun attachPlayerListener(player: Player) {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playbackState.value = _playbackState.value.copy(isPlaying = isPlaying)
                if (isPlaying) {
                    startPositionTicker()
                } else {
                    AudioSpectrumAnalyzer.reset()
                    stopPositionTicker()
                    syncProgressOnce(player)
                }
                MikaMusicWidgetProvider.requestWidgetUpdate(context)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                syncCurrentItemFromPlayer()
                ensureNotificationArtworkForCurrentSong()
                MikaMusicWidgetProvider.requestWidgetUpdate(context)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                syncStateFromPlayer()
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _playbackState.value = _playbackState.value.copy(shuffleEnabled = shuffleModeEnabled)
                MikaMusicWidgetProvider.requestWidgetUpdate(context)
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _playbackState.value = _playbackState.value.copy(repeatMode = repeatMode.toDomainRepeat())
                MikaMusicWidgetProvider.requestWidgetUpdate(context)
            }

            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                _playbackState.value = _playbackState.value.copy(
                    playbackSpeed = playbackParameters.speed,
                    playbackPitch = playbackParameters.pitch
                )
            }

            override fun onPlayerError(error: PlaybackException) {
                _playbackState.value = _playbackState.value.copy(
                    errorMessage = error.localizedMessage ?: "Unable to play this audio file"
                )
            }
        })
        if (player.isPlaying) {
            startPositionTicker()
        }
    }

    private fun restoreSavedPlaybackIfEmpty() {
        if (hasRestoredSession) return
        hasRestoredSession = true
        scope.launch {
            val player = mediaController ?: return@launch
            if (player.mediaItemCount > 0) {
                syncStateFromPlayer()
                return@launch
            }
            val settings = repository.settingsFlow.first()
            val sc = settings.soundControl
            val effectivePitch = if (sc.linkPitchToSpeed) {
                settings.playbackSpeed.coerceIn(0.5f, 2.0f)
            } else {
                2f.pow(sc.pitchSemitones / 12f).coerceIn(0.5f, 2.0f)
            }
            player.playbackParameters = PlaybackParameters(
                settings.playbackSpeed.coerceIn(0.25f, 3.0f),
                effectivePitch
            )
            audioEffectsController.updateSoundControl(sc)
            val snapshot = repository.getPlaybackSnapshot()
            player.shuffleModeEnabled = snapshot.shuffleEnabled
            player.repeatMode = snapshot.repeatMode.toMedia3Repeat()

            val savedQueue = repository.loadSavedQueue()
            if (savedQueue.isNotEmpty()) {
                val startIndex = savedQueue.indexOfFirst { it.id == snapshot.lastSongId }
                    .let { if (it >= 0) it else 0 }
                val targetSong = savedQueue.getOrNull(startIndex)
                val minRestoreMs = settings.restorePositionMinDurationMin * 60_000L
                val meetsMinDur = targetSong != null && (minRestoreMs <= 0L || targetSong.durationMs >= minRestoreMs)
                val startPos = if (settings.rememberLastPosition && meetsMinDur) snapshot.lastPositionMs else 0L
                setQueueInternal(savedQueue, startIndex, startPos, playWhenReady = false)
            }
        }
    }

    private fun startPositionTicker() {
        if (positionTickerJob?.isActive == true) return
        positionTickerJob = scope.launch {
            while (isActive) {
                val p = mediaController
                if (p != null && p.mediaItemCount > 0) {
                    syncProgressOnce(p)
                }
                delay(250L)
            }
        }
    }

    private fun stopPositionTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = null
    }

    private fun syncProgressOnce(player: Player) {
        if (android.os.SystemClock.elapsedRealtime() < seekHoldUntilElapsedMs) {
            return
        }
        val rawDuration = player.duration.coerceAtLeast(0L)
        val fallbackDuration = _playbackState.value.currentSong?.durationMs ?: 0L
        val duration = if (rawDuration > 0L) rawDuration else fallbackDuration
        val pos = player.currentPosition.coerceAtLeast(0L)
        val cur = _playbackProgress.value
        if (cur.positionMs != pos || cur.durationMs != duration) {
            _playbackProgress.value = PlaybackProgressState(positionMs = pos, durationMs = duration)
        }
    }

    fun playSong(song: Song, contextList: List<Song> = listOf(song), playlistId: Long? = null) {
        val list = if (contextList.isEmpty()) listOf(song) else contextList
        val startIndex = list.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        if (mediaController == null) {
            pendingQueueRequest = PendingQueueRequest(list, startIndex, shuffle = false, playlistId = playlistId)
            connect()
            return
        }
        scope.launch {
            if (playlistId != null) {
                repository.markPlaylistPlayed(playlistId)
            }
            setQueueInternal(list, startIndex, 0L, playWhenReady = true)
        }
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0, shuffle: Boolean = false, playlistId: Long? = null) {
        if (songs.isEmpty()) return
        if (mediaController == null) {
            pendingQueueRequest = PendingQueueRequest(songs, startIndex, shuffle, playlistId)
            connect()
            return
        }
        scope.launch {
            if (playlistId != null) {
                repository.markPlaylistPlayed(playlistId)
            }
            mediaController?.shuffleModeEnabled = shuffle
            val safeIndex = if (shuffle) songs.indices.random() else startIndex.coerceIn(0, songs.lastIndex)
            setQueueInternal(songs, safeIndex, 0L, playWhenReady = true)
        }
    }

    private suspend fun setQueueInternal(
        songs: List<Song>,
        startIndex: Int,
        startPositionMs: Long,
        playWhenReady: Boolean
    ) {
        val player = mediaController ?: run {
            connect()
            return
        }
        queueSongs = songs.toMutableList()
        val startSong = queueSongs.getOrNull(startIndex)

        // Pre-cache artwork for the starting track and batch-list cached files once on IO
        val (startArtworkBytes, cachedFileNames) = withContext(Dispatchers.IO) {
            val bytes = startSong?.let { extractJpegBytes(it) }
            val names = ArtworkManager.cacheDir(context).list()?.toHashSet().orEmpty()
            bytes to names
        }

        val mediaItems = withContext(Dispatchers.Default) {
            songs.mapIndexed { idx, s ->
                s.toMediaItem(
                    artworkBytes = if (idx == startIndex) startArtworkBytes else null,
                    cachedFileNames = cachedFileNames
                )
            }
        }

        player.setMediaItems(mediaItems, startIndex, startPositionMs)
        player.volume = 1f
        player.prepare()
        if (playWhenReady) {
            onBeforeAudioPlay?.invoke()
            player.play()
        }
        _playbackState.value = _playbackState.value.copy(
            queue = queueSongs.toList(),
            currentIndex = startIndex,
            currentSong = startSong,
            errorMessage = null
        )
        _playbackProgress.value = PlaybackProgressState(
            positionMs = startPositionMs,
            durationMs = startSong?.durationMs ?: 0L
        )
        withContext(Dispatchers.IO) {
            repository.savePlaybackQueue(songs.map { it.id })
        }
    }

    private suspend fun extractJpegBytes(song: Song): ByteArray? {
        val cachedFile = ArtworkManager.getCachedFileForTrack(context, song.id, song.dateModifiedSec)
        if (cachedFile.exists() && cachedFile.length() in 1L..524_288L) {
            val direct = runCatching { cachedFile.readBytes() }.getOrNull()
            if (direct != null && direct.isNotEmpty()) return direct
        }
        val bitmap = ArtworkManager.getOrExtractBitmap(context, song, maxDimension = 384) ?: return null
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 86, stream)
        return stream.toByteArray()
    }

    private fun ensureNotificationArtworkForCurrentSong() {
        val player = mediaController ?: return
        val idx = player.currentMediaItemIndex
        val song = queueSongs.getOrNull(idx) ?: return
        if (idx in 0 until player.mediaItemCount &&
            player.getMediaItemAt(idx).mediaMetadata.artworkData != null
        ) {
            return
        }
        scope.launch(Dispatchers.IO) {
            val bytes = extractJpegBytes(song) ?: return@launch
            withContext(Dispatchers.Main) {
                if (idx in 0 until player.mediaItemCount &&
                    player.getMediaItemAt(idx).mediaId == song.id.toString() &&
                    player.getMediaItemAt(idx).mediaMetadata.artworkData == null
                ) {
                    val currentItem = player.getMediaItemAt(idx)
                    val updatedMeta = currentItem.mediaMetadata.buildUpon()
                        .setArtworkData(bytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                        .build()
                    val updatedItem = currentItem.buildUpon()
                        .setMediaMetadata(updatedMeta)
                        .build()
                    player.replaceMediaItem(idx, updatedItem)
                }
            }
        }
    }

    fun playNext(song: Song) {
        val player = mediaController ?: return
        if (queueSongs.isEmpty() || player.mediaItemCount == 0) {
            playSong(song)
            return
        }
        scope.launch {
            val mediaItem = withContext(Dispatchers.IO) { song.toMediaItem(artworkBytes = null) }
            val activePlayer = mediaController ?: return@launch
            val insertIdx = (activePlayer.currentMediaItemIndex + 1).coerceIn(0, queueSongs.size)
            queueSongs.add(insertIdx, song)
            activePlayer.addMediaItem(insertIdx, mediaItem)
            syncQueueSnapshot()
        }
    }

    fun addToQueue(songs: List<Song>) {
        if (songs.isEmpty()) return
        val player = mediaController ?: return
        if (queueSongs.isEmpty() || player.mediaItemCount == 0) {
            playQueue(songs)
            return
        }
        scope.launch {
            val items = withContext(Dispatchers.IO) {
                val names = ArtworkManager.cacheDir(context).list()?.toHashSet().orEmpty()
                songs.map { it.toMediaItem(artworkBytes = null, cachedFileNames = names) }
            }
            val activePlayer = mediaController ?: return@launch
            queueSongs.addAll(songs)
            activePlayer.addMediaItems(items)
            syncQueueSnapshot()
        }
    }

    fun removeFromQueue(index: Int) {
        val player = mediaController ?: return
        if (index !in queueSongs.indices) return
        queueSongs.removeAt(index)
        player.removeMediaItem(index)
        syncQueueSnapshot()
    }

    fun removeSongsFromQueue(songIds: Set<Long>) {
        if (songIds.isEmpty()) return
        val player = mediaController
        val indicesToRemove = queueSongs.indices
            .filter { queueSongs[it].id in songIds }
            .sortedDescending()
        if (indicesToRemove.isEmpty()) return

        for (idx in indicesToRemove) {
            queueSongs.removeAt(idx)
            if (player != null && idx in 0 until player.mediaItemCount) {
                player.removeMediaItem(idx)
            }
        }
        if (queueSongs.isEmpty()) {
            AudioSpectrumAnalyzer.reset()
            stopPositionTicker()
            player?.stop()
            _playbackState.value = _playbackState.value.copy(
                queue = emptyList(),
                currentSong = null,
                currentIndex = -1,
                isPlaying = false
            )
            _playbackProgress.value = PlaybackProgressState()
            scope.launch(Dispatchers.IO) {
                repository.savePlaybackQueue(emptyList())
            }
        } else {
            syncQueueSnapshot()
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val player = mediaController ?: return
        if (fromIndex !in queueSongs.indices || toIndex !in queueSongs.indices || fromIndex == toIndex) return
        val item = queueSongs.removeAt(fromIndex)
        queueSongs.add(toIndex, item)
        player.moveMediaItem(fromIndex, toIndex)
        syncQueueSnapshot()
    }

    fun clearQueue() {
        val player = mediaController ?: return
        AudioSpectrumAnalyzer.reset()
        stopPositionTicker()
        player.stop()
        player.clearMediaItems()
        queueSongs.clear()
        _playbackState.value = _playbackState.value.copy(
            queue = emptyList(),
            currentSong = null,
            currentIndex = -1,
            isPlaying = false
        )
        _playbackProgress.value = PlaybackProgressState()
        scope.launch(Dispatchers.IO) {
            repository.savePlaybackQueue(emptyList())
        }
    }

    fun skipToQueueIndex(index: Int) {
        val player = mediaController ?: return
        if (index !in queueSongs.indices) return
        player.seekToDefaultPosition(index)
        if (!player.isPlaying) {
            onBeforeAudioPlay?.invoke()
            player.play()
        }
        syncCurrentItemFromPlayer()
    }

    fun togglePlayPause() {
        val player = mediaController ?: return
        if (player.mediaItemCount == 0 && queueSongs.isNotEmpty()) {
            playQueue(queueSongs)
            return
        }
        val st = cachedSettings
        if (player.isPlaying) {
            playPauseFadeJob?.cancel()
            if (st.playPauseFadeEnabled && st.pauseFadeDurationMs > 50) {
                val durationMs = st.pauseFadeDurationMs.toLong()
                playPauseFadeJob = scope.launch {
                    val steps = 12
                    val stepDelay = (durationMs / steps).coerceAtLeast(10L)
                    for (i in steps downTo 1) {
                        player.volume = (i.toFloat() / steps).coerceIn(0.05f, 1f)
                        delay(stepDelay)
                    }
                    player.pause()
                    player.volume = 1f
                }
            } else {
                player.volume = 1f
                player.pause()
            }
        } else {
            onBeforeAudioPlay?.invoke()
            if (player.playbackState == Player.STATE_ENDED) {
                player.seekTo(0, 0L)
            }
            playPauseFadeJob?.cancel()
            if (st.playPauseFadeEnabled && st.playFadeDurationMs > 50) {
                val durationMs = st.playFadeDurationMs.toLong()
                player.volume = 0.05f
                player.play()
                playPauseFadeJob = scope.launch {
                    val steps = 12
                    val stepDelay = (durationMs / steps).coerceAtLeast(10L)
                    for (i in 1..steps) {
                        player.volume = (i.toFloat() / steps).coerceIn(0.05f, 1f)
                        delay(stepDelay)
                    }
                    player.volume = 1f
                }
            } else {
                player.volume = 1f
                player.play()
            }
        }
    }

    fun skipNext() {
        val player = mediaController ?: return
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        } else if (cachedSettings.jumpToFirstAfterQueue && queueSongs.isNotEmpty()) {
            player.seekTo(0, 0L)
        }
    }

    fun skipPrevious() {
        val player = mediaController ?: return
        val replayThresholdMs = if (cachedSettings.previousButtonReplays) {
            cachedSettings.skipIntervalSec * 1000L
        } else {
            Long.MAX_VALUE
        }
        if (cachedSettings.previousButtonReplays && player.currentPosition > replayThresholdMs) {
            player.seekTo(0L)
        } else if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        } else {
            player.seekTo(0L)
        }
    }

    fun seekTo(positionMs: Long) {
        val player = mediaController ?: return
        val safePos = positionMs.coerceAtLeast(0L)
        seekHoldUntilElapsedMs = android.os.SystemClock.elapsedRealtime() + 450L
        player.seekTo(safePos)
        _playbackProgress.value = _playbackProgress.value.copy(positionMs = safePos)
    }

    fun seekRelative(deltaMs: Long) {
        val player = mediaController ?: return
        val maxDur = player.duration.takeIf { it > 0L }
            ?: (_playbackState.value.currentSong?.durationMs ?: 0L)
        val target = (player.currentPosition + deltaMs).coerceIn(0L, maxDur.coerceAtLeast(0L))
        seekTo(target)
    }

    fun toggleShuffle() {
        val player = mediaController ?: return
        player.shuffleModeEnabled = !player.shuffleModeEnabled
        persistCurrentSnapshot()
    }

    fun cycleRepeatMode() {
        val player = mediaController ?: return
        val next = when (_playbackState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        player.repeatMode = next.toMedia3Repeat()
        persistCurrentSnapshot()
    }

    fun setPlaybackSpeedAndPitch(speed: Float, pitch: Float) {
        val safeSpeed = speed.coerceIn(0.25f, 3.0f)
        val safePitch = pitch.coerceIn(0.5f, 2.0f)
        mediaController?.playbackParameters = PlaybackParameters(safeSpeed, safePitch)
        _playbackState.value = _playbackState.value.copy(
            playbackSpeed = safeSpeed,
            playbackPitch = safePitch
        )
        scope.launch {
            repository.settingsDataStore.updatePlaybackSpeedAndPitch(safeSpeed, safePitch)
        }
    }

    fun setSoundControl(soundControl: SoundControlSettings, speedOverride: Float? = null) {
        val currentSpeed = (speedOverride ?: _playbackState.value.playbackSpeed).coerceIn(0.25f, 3.0f)
        val pitchFactor = if (soundControl.linkPitchToSpeed) {
            currentSpeed.coerceIn(0.5f, 2.0f)
        } else {
            2f.pow(soundControl.pitchSemitones / 12f).coerceIn(0.5f, 2.0f)
        }
        mediaController?.playbackParameters = PlaybackParameters(currentSpeed, pitchFactor)
        mediaController?.volume = (soundControl.playerVolumePct / 100f).coerceIn(0f, 1f)
        audioEffectsController.updateSoundControl(soundControl)
        _playbackState.value = _playbackState.value.copy(
            playbackSpeed = currentSpeed,
            playbackPitch = pitchFactor
        )
        scope.launch {
            repository.settingsDataStore.updateSoundControl(soundControl)
            if (speedOverride != null) {
                repository.settingsDataStore.updatePlaybackSpeedAndPitch(currentSpeed, pitchFactor)
            }
        }
    }

    fun updateSongFavoriteInQueue(songId: Long, isFavorite: Boolean) {
        var changed = false
        for (i in queueSongs.indices) {
            if (queueSongs[i].id == songId && queueSongs[i].isFavorite != isFavorite) {
                queueSongs[i] = queueSongs[i].copy(isFavorite = isFavorite)
                changed = true
            }
        }
        val cur = _playbackState.value.currentSong
        val updatedCur = if (cur?.id == songId) cur.copy(isFavorite = isFavorite) else cur
        if (changed || updatedCur != cur) {
            _playbackState.value = _playbackState.value.copy(
                queue = if (changed) queueSongs.toList() else _playbackState.value.queue,
                currentSong = updatedCur
            )
        }
    }

    fun updateSongsFavoriteInQueue(songIds: Set<Long>, isFavorite: Boolean) {
        if (songIds.isEmpty()) return
        var changed = false
        for (i in queueSongs.indices) {
            if (queueSongs[i].id in songIds && queueSongs[i].isFavorite != isFavorite) {
                queueSongs[i] = queueSongs[i].copy(isFavorite = isFavorite)
                changed = true
            }
        }
        val cur = _playbackState.value.currentSong
        val updatedCur = if (cur != null && cur.id in songIds) cur.copy(isFavorite = isFavorite) else cur
        if (changed || updatedCur != cur) {
            _playbackState.value = _playbackState.value.copy(
                queue = if (changed) queueSongs.toList() else _playbackState.value.queue,
                currentSong = updatedCur
            )
        }
    }

    fun updateSongsMoodRatingInQueue(songIds: Set<Long>, mood: String? = null, rating: Int? = null) {
        if (songIds.isEmpty()) return
        var changed = false
        for (i in queueSongs.indices) {
            if (queueSongs[i].id in songIds) {
                queueSongs[i] = queueSongs[i].copy(
                    mood = mood ?: queueSongs[i].mood,
                    rating = rating ?: queueSongs[i].rating
                )
                changed = true
            }
        }
        val cur = _playbackState.value.currentSong
        val updatedCur = if (cur != null && cur.id in songIds) {
            cur.copy(
                mood = mood ?: cur.mood,
                rating = rating ?: cur.rating
            )
        } else cur
        if (changed || updatedCur != cur) {
            _playbackState.value = _playbackState.value.copy(
                queue = if (changed) queueSongs.toList() else _playbackState.value.queue,
                currentSong = updatedCur
            )
        }
    }

    fun refreshUpdatedSongs(updatedSongs: List<Song>) {
        if (updatedSongs.isEmpty()) return
        val byId = updatedSongs.associateBy { it.id }
        var changed = false
        for (i in queueSongs.indices) {
            val updated = byId[queueSongs[i].id]
            if (updated != null) {
                queueSongs[i] = updated
                changed = true
            }
        }
        val cur = _playbackState.value.currentSong
        val updatedCur = cur?.let { byId[it.id] ?: it }
        if (changed || updatedCur != cur) {
            _playbackState.value = _playbackState.value.copy(
                queue = if (changed) queueSongs.toList() else _playbackState.value.queue,
                currentSong = updatedCur
            )
        }
        val player = mediaController ?: return
        scope.launch {
            for (idx in 0 until player.mediaItemCount) {
                val songId = player.getMediaItemAt(idx).mediaId.toLongOrNull() ?: continue
                val updated = byId[songId] ?: continue
                val artworkBytes = withContext(Dispatchers.IO) {
                    extractJpegBytes(updated)
                }
                if (idx < player.mediaItemCount && player.getMediaItemAt(idx).mediaId == songId.toString()) {
                    player.replaceMediaItem(idx, updated.toMediaItem(artworkBytes))
                }
            }
        }
    }

    fun clearError() {
        _playbackState.value = _playbackState.value.copy(errorMessage = null)
    }

    fun setEqEnabled(enabled: Boolean) {
        val updated = audioEffectsController.state.value.copy(enabled = enabled)
        audioEffectsController.updateState(updated)
        scope.launch { repository.settingsDataStore.saveEqualizerState(updated) }
    }

    fun applyEqPreset(preset: EqPreset) {
        val cur = audioEffectsController.state.value
        val levels = audioEffectsController.presetLevelsFor(
            preset,
            cur.bandFrequenciesHz.size,
            cur.minBandLevelMb,
            cur.maxBandLevelMb
        )
        val updated = cur.copy(enabled = true, preset = preset, bandLevelsMb = levels)
        audioEffectsController.updateState(updated)
        scope.launch { repository.settingsDataStore.saveEqualizerState(updated) }
    }

    fun updateEqBand(index: Int, levelMb: Int) {
        val cur = audioEffectsController.state.value
        val mutable = cur.bandLevelsMb.toMutableList()
        if (index in mutable.indices) {
            mutable[index] = levelMb.coerceIn(cur.minBandLevelMb, cur.maxBandLevelMb)
        }
        val updated = cur.copy(enabled = true, preset = EqPreset.CUSTOM, bandLevelsMb = mutable)
        audioEffectsController.updateState(updated)
        scope.launch { repository.settingsDataStore.saveEqualizerState(updated) }
    }

    fun updateBassBoost(strength: Int) {
        val cur = audioEffectsController.state.value
        val updated = cur.copy(enabled = true, bassBoostStrength = strength.coerceIn(0, 1000))
        audioEffectsController.updateState(updated)
        scope.launch { repository.settingsDataStore.saveEqualizerState(updated) }
    }

    fun updateVirtualizer(strength: Int) {
        val cur = audioEffectsController.state.value
        val updated = cur.copy(enabled = true, virtualizerStrength = strength.coerceIn(0, 1000))
        audioEffectsController.updateState(updated)
        scope.launch { repository.settingsDataStore.saveEqualizerState(updated) }
    }

    fun resetEqualizer() {
        val cur = audioEffectsController.state.value
        val zeroed = List(cur.bandLevelsMb.size) { 0 }
        val updated = cur.copy(
            preset = EqPreset.FLAT,
            bandLevelsMb = zeroed,
            bassBoostStrength = 0,
            virtualizerStrength = 0
        )
        audioEffectsController.updateState(updated)
        scope.launch { repository.settingsDataStore.saveEqualizerState(updated) }
    }

    fun startSleepTimer(minutes: Int, fadeOutEnabled: Boolean = _sleepTimerState.value.fadeOutEnabled) {
        val totalMs = minutes.coerceAtLeast(1) * 60_000L
        startSleepTimerMs(totalMs, totalMs, fadeOutEnabled)
    }

    fun extendSleepTimer(extraMinutes: Int = 5) {
        val cur = _sleepTimerState.value
        if (!cur.isActive || cur.stopAtEndOfTrack) return
        val newRemaining = (cur.remainingMs + extraMinutes * 60_000L).coerceAtLeast(60_000L)
        val newTotal = maxOf(cur.totalConfiguredMs + extraMinutes * 60_000L, newRemaining)
        mediaController?.volume = 1f
        startSleepTimerMs(newRemaining, newTotal, cur.fadeOutEnabled)
    }

    fun setSleepTimerFadeOut(enabled: Boolean) {
        _sleepTimerState.value = _sleepTimerState.value.copy(fadeOutEnabled = enabled)
        if (!enabled) {
            mediaController?.volume = 1f
        }
    }

    private fun startSleepTimerMs(remainingStartMs: Long, totalMs: Long, fadeOutEnabled: Boolean) {
        sleepTimerJob?.cancel()
        mediaController?.volume = 1f
        _sleepTimerState.value = SleepTimerState(
            isActive = true,
            remainingMs = remainingStartMs,
            stopAtEndOfTrack = false,
            totalConfiguredMs = totalMs,
            fadeOutEnabled = fadeOutEnabled
        )
        sleepTimerJob = scope.launch {
            var remaining = remainingStartMs
            while (remaining > 0 && isActive) {
                delay(1000L)
                remaining -= 1000L
                val safeRemaining = remaining.coerceAtLeast(0L)
                _sleepTimerState.value = _sleepTimerState.value.copy(remainingMs = safeRemaining)
                if (_sleepTimerState.value.fadeOutEnabled && safeRemaining in 1L..10_000L) {
                    mediaController?.volume = (safeRemaining.toFloat() / 10_000f).coerceIn(0.05f, 1f)
                }
            }
            if (isActive && remaining <= 0L) {
                mediaController?.pause()
                mediaController?.volume = 1f
                cancelSleepTimer()
            }
        }
    }

    fun setSleepAtEndOfTrack() {
        sleepTimerJob?.cancel()
        mediaController?.volume = 1f
        _sleepTimerState.value = SleepTimerState(
            isActive = true,
            remainingMs = 0L,
            stopAtEndOfTrack = true,
            totalConfiguredMs = 0L,
            fadeOutEnabled = _sleepTimerState.value.fadeOutEnabled
        )
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        mediaController?.volume = 1f
        _sleepTimerState.value = SleepTimerState(fadeOutEnabled = _sleepTimerState.value.fadeOutEnabled)
    }

    fun persistCurrentSnapshot() {
        val p = mediaController ?: return
        val song = _playbackState.value.currentSong ?: return
        val pos = p.currentPosition.coerceAtLeast(0L)
        val shuffle = p.shuffleModeEnabled
        val repeat = p.repeatMode.toDomainRepeat()
        scope.launch(Dispatchers.IO) {
            repository.savePlaybackSnapshot(song.id, pos, shuffle, repeat)
        }
    }

    private fun syncQueueSnapshot() {
        val player = mediaController ?: return
        val idx = player.currentMediaItemIndex
        _playbackState.value = _playbackState.value.copy(
            queue = queueSongs.toList(),
            currentIndex = idx,
            currentSong = queueSongs.getOrNull(idx)
        )
        val ids = queueSongs.map { it.id }
        scope.launch(Dispatchers.IO) {
            repository.savePlaybackQueue(ids)
        }
    }

    private fun syncCurrentItemFromPlayer() {
        val player = mediaController ?: return
        val idx = player.currentMediaItemIndex
        val song = queueSongs.getOrNull(idx)
        _playbackState.value = _playbackState.value.copy(
            currentIndex = idx,
            currentSong = song
        )
        syncProgressOnce(player)
    }

    private fun syncStateFromPlayer() {
        val player = mediaController ?: return
        val idx = player.currentMediaItemIndex
        val song = queueSongs.getOrNull(idx)
        _playbackState.value = _playbackState.value.copy(
            currentIndex = idx,
            currentSong = song ?: _playbackState.value.currentSong,
            isPlaying = player.isPlaying,
            shuffleEnabled = player.shuffleModeEnabled,
            repeatMode = player.repeatMode.toDomainRepeat(),
            playbackSpeed = player.playbackParameters.speed,
            playbackPitch = player.playbackParameters.pitch
        )
        syncProgressOnce(player)
    }

    private fun Song.toMediaItem(
        artworkBytes: ByteArray?,
        cachedFileNames: Set<String>? = null
    ): MediaItem {
        val cachedFile = ArtworkManager.getCachedFileForTrack(context, id, dateModifiedSec)
        val hasCachedFile = if (cachedFileNames != null) {
            cachedFile.name in cachedFileNames
        } else {
            cachedFile.exists()
        }
        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
        if (artworkBytes != null && artworkBytes.isNotEmpty()) {
            metadataBuilder.setArtworkData(artworkBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
        } else if (hasCachedFile) {
            metadataBuilder.setArtworkUri(Uri.fromFile(cachedFile))
        }
        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(Uri.parse(contentUri))
            .setMediaMetadata(metadataBuilder.build())
            .build()
    }

    private fun Int.toDomainRepeat(): RepeatMode = when (this) {
        Player.REPEAT_MODE_ALL -> RepeatMode.ALL
        Player.REPEAT_MODE_ONE -> RepeatMode.ONE
        else -> RepeatMode.OFF
    }

    private fun RepeatMode.toMedia3Repeat(): Int = when (this) {
        RepeatMode.OFF -> Player.REPEAT_MODE_OFF
        RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        RepeatMode.ONE -> Player.REPEAT_MODE_ONE
    }
}
