package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.data.db.MikaDatabase
import com.example.data.prefs.SettingsDataStore
import com.example.data.repository.MusicRepository
import com.example.media.ArtworkManager
import com.example.playback.AudioEffectsController
import com.example.playback.PlaybackController
import com.example.playback.VideoPlaybackController

class MikaApplication : Application(), ImageLoaderFactory {

    lateinit var database: MikaDatabase
        private set

    lateinit var settingsDataStore: SettingsDataStore
        private set

    lateinit var repository: MusicRepository
        private set

    lateinit var audioEffectsController: AudioEffectsController
        private set

    lateinit var playbackController: PlaybackController
        private set

    lateinit var videoPlaybackController: VideoPlaybackController
        private set

    override fun onCreate() {
        super.onCreate()
        database = MikaDatabase.getInstance(this)
        settingsDataStore = SettingsDataStore(this)
        repository = MusicRepository(this, database.mikaDao(), settingsDataStore)
        audioEffectsController = AudioEffectsController()
        playbackController = PlaybackController(this, repository, audioEffectsController)
        videoPlaybackController = VideoPlaybackController(this)

        playbackController.onBeforeAudioPlay = {
            videoPlaybackController.pause()
        }
        videoPlaybackController.onBeforeVideoPlay = {
            playbackController.pausePlayback()
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ArtworkManager.buildImageLoader(this)
    }
}
