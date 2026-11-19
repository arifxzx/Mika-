package com.example.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.example.MainActivity
import com.example.MikaApplication
import com.example.R
import com.example.model.CrossfadeStyle
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

class MikaPlaybackService : MediaSessionService() {

    companion object {
        const val ACTION_DISMISS_PLAYER = "dev.sagor.mika.ACTION_DISMISS_PLAYER"
        const val ACTION_TOGGLE_FAVORITE = "dev.sagor.mika.ACTION_TOGGLE_FAVORITE"
        private const val IDLE_DISMISS_MS = 30L * 60L * 1000L
    }

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var crossfadeJob: Job? = null
    private var tailWatcherJob: Job? = null
    private var idleDismissJob: Job? = null
    @Volatile
    private var crossfadeEnabled: Boolean = false
    @Volatile
    private var crossfadeDurationMs: Int = 500
    @Volatile
    private var crossfadeTriggerLastSec: Int = 5
    @Volatile
    private var crossfadeStyle: CrossfadeStyle = CrossfadeStyle.DEFAULT
    @Volatile
    private var jumpToFirstAfterQueue: Boolean = true
    @Volatile
    private var basePlayerVolume: Float = 1f
    private var lastHandleAudioFocus: Boolean? = null
    private var currentFavoriteIds: Set<Long> = emptySet()

    private fun buildCustomLayout(mediaItem: MediaItem?): List<CommandButton> {
        val songId = mediaItem?.mediaId?.toLongOrNull()
        val isFav = songId != null && songId in currentFavoriteIds
        val favoriteCommand = SessionCommand(ACTION_TOGGLE_FAVORITE, Bundle.EMPTY)
        val favoriteButton = CommandButton.Builder(CommandButton.ICON_UNDEFINED)
            .setDisplayName(if (isFav) "Unlike" else "Like")
            .setIconResId(if (isFav) R.drawable.ic_notif_heart_filled else R.drawable.ic_notif_heart_outline)
            .setSessionCommand(favoriteCommand)
            .build()

        val dismissCommand = SessionCommand(ACTION_DISMISS_PLAYER, Bundle.EMPTY)
        val dismissButton = CommandButton.Builder(CommandButton.ICON_UNDEFINED)
            .setDisplayName("Dismiss")
            .setIconResId(R.drawable.ic_notif_close)
            .setSessionCommand(dismissCommand)
            .build()

        return listOf(favoriteButton, dismissButton)
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        val app = application as MikaApplication

        val dspProcessor = MikaDspAudioProcessor()
        val spectrumProcessor = AudioSpectrumProcessor()
        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .setAudioProcessors(arrayOf(dspProcessor, spectrumProcessor))
                    .build()
            }
        }

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        player = ExoPlayer.Builder(this, renderersFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                app.audioEffectsController.attachToAudioSession(audioSessionId)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED &&
                    jumpToFirstAfterQueue &&
                    player.repeatMode == Player.REPEAT_MODE_OFF &&
                    player.mediaItemCount > 0
                ) {
                    player.seekTo(0, 0L)
                    player.pause()
                    app.playbackController.persistCurrentSnapshot()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val controller = app.playbackController
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO &&
                    controller.sleepTimerState.value.stopAtEndOfTrack
                ) {
                    player.pause()
                    controller.cancelSleepTimer()
                    return
                }

                mediaSession?.setCustomLayout(buildCustomLayout(mediaItem))

                val songId = mediaItem?.mediaId?.toLongOrNull()
                if (songId != null && songId != -1L) {
                    serviceScope.launch(Dispatchers.IO) {
                        app.repository.recordSongPlayed(songId)
                    }
                }

                if (crossfadeEnabled && crossfadeDurationMs > 0 && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                    performFadeIn(crossfadeDurationMs, crossfadeStyle)
                } else {
                    crossfadeJob?.cancel()
                    player.volume = basePlayerVolume
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    idleDismissJob?.cancel()
                    idleDismissJob = null
                    ensureTailWatcherRunning()
                } else {
                    tailWatcherJob?.cancel()
                    tailWatcherJob = null
                    AudioSpectrumAnalyzer.reset()
                    app.playbackController.persistCurrentSnapshot()
                    scheduleIdleDismiss(app)
                }
            }
        })

        // Observe settings reactively for gapless, skipSilence, crossfade, queue end, audio focus & Sound Control DSP
        serviceScope.launch {
            app.repository.settingsFlow.collect { settings ->
                crossfadeEnabled = settings.crossfadeEnabled
                crossfadeDurationMs = settings.crossfadeDurationMs
                crossfadeTriggerLastSec = settings.crossfadeTriggerLastSec
                crossfadeStyle = settings.crossfadeStyle
                jumpToFirstAfterQueue = settings.jumpToFirstAfterQueue
                basePlayerVolume = (settings.soundControl.playerVolumePct / 100f).coerceIn(0f, 1f)
                dspProcessor.updateSettings(settings.soundControl)
                app.audioEffectsController.updateSoundControl(settings.soundControl)
                if (crossfadeJob?.isActive != true) {
                    player.volume = basePlayerVolume
                }
                player.skipSilenceEnabled = settings.skipSilence
                player.pauseAtEndOfMediaItems = !settings.gaplessPlayback
                val handleAudioFocus = !settings.allowSimultaneousPlayback
                if (lastHandleAudioFocus != handleAudioFocus) {
                    lastHandleAudioFocus = handleAudioFocus
                    player.setAudioAttributes(audioAttributes, handleAudioFocus)
                }
                if (player.isPlaying) {
                    ensureTailWatcherRunning()
                }
            }
        }

        // Observe favorite song IDs so media notification heart stays in sync
        serviceScope.launch {
            app.repository.favoritesFlow
                .map { songs -> songs.map { it.id }.toSet() }
                .collect { favIds ->
                    currentFavoriteIds = favIds
                    mediaSession?.setCustomLayout(buildCustomLayout(player.currentMediaItem))
                }
        }

        // Observe equalizer persistence into hardware controller
        serviceScope.launch {
            app.repository.equalizerFlow.collect { eqState ->
                app.audioEffectsController.updateState(eqState)
            }
        }

        val sessionActivityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            sessionActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val favoriteCommand = SessionCommand(ACTION_TOGGLE_FAVORITE, Bundle.EMPTY)
        val dismissCommand = SessionCommand(ACTION_DISMISS_PLAYER, Bundle.EMPTY)

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent)
            .setCustomLayout(buildCustomLayout(player.currentMediaItem))
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): MediaSession.ConnectionResult {
                    val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                        .buildUpon()
                        .add(favoriteCommand)
                        .add(dismissCommand)
                        .build()
                    return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                        .setAvailableSessionCommands(sessionCommands)
                        .build()
                }

                override fun onCustomCommand(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    customCommand: SessionCommand,
                    args: Bundle
                ): ListenableFuture<SessionResult> {
                    if (customCommand.customAction == ACTION_TOGGLE_FAVORITE) {
                        val songId = player.currentMediaItem?.mediaId?.toLongOrNull()
                        if (songId != null && songId != -1L) {
                            val nextFav = songId !in currentFavoriteIds
                            currentFavoriteIds = if (nextFav) {
                                currentFavoriteIds + songId
                            } else {
                                currentFavoriteIds - songId
                            }
                            mediaSession?.setCustomLayout(buildCustomLayout(player.currentMediaItem))
                            app.playbackController.updateSongFavoriteInQueue(songId, nextFav)
                            serviceScope.launch(Dispatchers.IO) {
                                app.repository.setFavorite(songId, nextFav)
                            }
                        }
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    if (customCommand.customAction == ACTION_DISMISS_PLAYER) {
                        AudioSpectrumAnalyzer.reset()
                        app.playbackController.persistCurrentSnapshot()
                        player.pause()
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    return super.onCustomCommand(session, controller, customCommand, args)
                }
            })
            .build()
        scheduleIdleDismiss(app)
    }

    private fun scheduleIdleDismiss(app: MikaApplication) {
        idleDismissJob?.cancel()
        idleDismissJob = serviceScope.launch {
            delay(IDLE_DISMISS_MS)
            if (!player.isPlaying) {
                AudioSpectrumAnalyzer.reset()
                app.playbackController.persistCurrentSnapshot()
                player.pause()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun ensureTailWatcherRunning() {
        if (!crossfadeEnabled) {
            tailWatcherJob?.cancel()
            tailWatcherJob = null
            if (crossfadeJob?.isActive != true) player.volume = basePlayerVolume
            return
        }
        if (tailWatcherJob?.isActive == true) return
        tailWatcherJob = serviceScope.launch {
            while (isActive && crossfadeEnabled) {
                val dur = player.duration
                val pos = player.currentPosition
                val triggerWindowMs = crossfadeTriggerLastSec * 1000L
                val remaining = dur - pos
                val hasNext = player.hasNextMediaItem()
                if (dur > triggerWindowMs + 1000L && remaining in 1L..triggerWindowMs && hasNext && crossfadeJob?.isActive != true) {
                    val fractionOut = (remaining.toFloat() / triggerWindowMs.toFloat()).coerceIn(0f, 1f)
                    player.volume = (curveValue(fractionOut, crossfadeStyle).coerceIn(0.08f, 1f) * basePlayerVolume)
                }
                delay(120L)
            }
        }
    }

    private fun curveValue(t: Float, style: CrossfadeStyle): Float {
        val clamped = t.coerceIn(0f, 1f)
        return when (style) {
            CrossfadeStyle.LINEAR -> clamped
            CrossfadeStyle.EQUAL_POWER -> sin(clamped * (PI.toFloat() / 2f))
            CrossfadeStyle.SMOOTH -> clamped * clamped * (3f - 2f * clamped)
            CrossfadeStyle.DEFAULT -> clamped.pow(0.75f)
        }
    }

    private fun performFadeIn(durationMs: Int, style: CrossfadeStyle) {
        crossfadeJob?.cancel()
        crossfadeJob = serviceScope.launch {
            val steps = 20
            val stepDelay = (durationMs.toLong() / steps).coerceAtLeast(15L)
            for (i in 1..steps) {
                val t = i.toFloat() / steps
                player.volume = (curveValue(t, style).coerceIn(0.05f, 1f) * basePlayerVolume)
                delay(stepDelay)
            }
            player.volume = basePlayerVolume
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = mediaSession?.player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        val app = application as? MikaApplication
        AudioSpectrumAnalyzer.reset()
        app?.playbackController?.persistCurrentSnapshot()
        app?.audioEffectsController?.releaseEffects()
        idleDismissJob?.cancel()
        tailWatcherJob?.cancel()
        crossfadeJob?.cancel()
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
