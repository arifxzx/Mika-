package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MikaApplication
import com.example.data.backup.LocalBackupFileInfo
import com.example.data.backup.MikaBackupPayload
import com.example.data.repository.MusicRepository
import com.example.media.ArtworkDetails
import com.example.media.ArtworkManager
import com.example.media.LyricsParser
import com.example.media.MediaScanner
import com.example.media.VideoScanner
import com.example.media.WaveformExtractor
import com.example.model.Album
import com.example.model.AlbumSortField
import com.example.model.AppIconPreset
import com.example.model.AppSettings
import com.example.model.Artist
import com.example.model.ArtistSortField
import com.example.model.ArtistStat
import com.example.model.ArtworkGestureAction
import com.example.model.BackgroundEffect
import com.example.model.BottomNavDest
import com.example.model.CrossfadeStyle
import com.example.model.SoundControlSettings
import com.example.model.EqPreset
import com.example.model.EqualizerState
import com.example.model.FolderItem
import com.example.model.FolderSortField
import com.example.model.FormatStat
import com.example.model.GenreItem
import com.example.model.GenreSortField
import com.example.model.GradientStyle
import com.example.model.LibraryStats
import com.example.model.LyricsData
import com.example.model.MainTab
import com.example.model.PlaybackIconStyle
import com.example.model.PlaybackProgressState
import com.example.model.PlaybackUiState
import com.example.model.PlayerBackgroundMode
import com.example.model.Playlist
import com.example.model.PlaylistSortField
import com.example.model.SleepTimerState
import com.example.model.Song
import com.example.model.SortDirection
import com.example.model.SortField
import com.example.model.TabLongPressAction
import com.example.model.TabTapAction
import com.example.model.ThemeMode
import com.example.model.TrackSwipeAction
import com.example.model.VideoItem
import com.example.model.VideoPlaybackUiState
import com.example.model.VideoSortField
import com.example.model.WidgetConfig
import com.example.playback.AudioSpectrumAnalyzer
import com.example.util.AppIconManager
import com.example.widget.MikaMusicWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class SearchResultsState(
    val query: String = "",
    val songs: List<Song> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val albums: List<Album> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val folders: List<FolderItem> = emptyList(),
    val videos: List<VideoItem> = emptyList(),
    val genres: List<GenreItem> = emptyList()
) {
    val isEmpty: Boolean
        get() = songs.isEmpty() && artists.isEmpty() && albums.isEmpty() &&
            playlists.isEmpty() && folders.isEmpty() && videos.isEmpty() && genres.isEmpty()
}

data class FolderBrowserState(
    val currentPath: String? = null,
    val breadcrumbs: List<Pair<String, String?>> = listOf("All Folders" to null),
    val subfolders: List<FolderItem> = emptyList(),
    val songs: List<Song> = emptyList()
)

data class PendingBackupPreview(
    val payload: MikaBackupPayload,
    val sourceLabel: String
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class MikaViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MikaApplication
    private val repository = app.repository
    val playbackController = app.playbackController
    val videoPlaybackController = app.videoPlaybackController

    private val _selectedTab = MutableStateFlow(MainTab.SONGS)
    val selectedTab: StateFlow<MainTab> = _selectedTab.asStateFlow()

    private val _hasAudioPermission = MutableStateFlow(MediaScanner.hasAudioPermission(application))
    val hasAudioPermission: StateFlow<Boolean> = _hasAudioPermission.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _rawVideos = MutableStateFlow<List<VideoItem>>(emptyList())
    private val _isLoadingVideos = MutableStateFlow(false)
    val isLoadingVideos: StateFlow<Boolean> = _isLoadingVideos.asStateFlow()

    private val _videoSortField = MutableStateFlow(VideoSortField.DATE_ADDED)
    private val _videoSortDirection = MutableStateFlow(SortDirection.DESCENDING)
    val videoSortField: StateFlow<VideoSortField> = _videoSortField.asStateFlow()
    val videoSortDirection: StateFlow<SortDirection> = _videoSortDirection.asStateFlow()

    private val _selectedSongIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedSongIds: StateFlow<Set<Long>> = _selectedSongIds.asStateFlow()

    private val _currentFolderPath = MutableStateFlow<String?>(null)
    private val _folderSortField = MutableStateFlow(SortField.TITLE)
    private val _folderSortDirection = MutableStateFlow(SortDirection.ASCENDING)
    val folderSortField: StateFlow<SortField> = _folderSortField.asStateFlow()
    val folderSortDirection: StateFlow<SortDirection> = _folderSortDirection.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _currentArtworkColors = MutableStateFlow<Pair<Color, Color>?>(null)
    val currentArtworkColors: StateFlow<Pair<Color, Color>?> = _currentArtworkColors.asStateFlow()

    private val _currentBrightArtworkColors = MutableStateFlow<Pair<Color, Color>?>(null)
    val currentBrightArtworkColors: StateFlow<Pair<Color, Color>?> = _currentBrightArtworkColors.asStateFlow()

    private val _currentLyrics = MutableStateFlow<LyricsData?>(null)
    val currentLyrics: StateFlow<LyricsData?> = _currentLyrics.asStateFlow()

    private val _cacheSizeBytes = MutableStateFlow(0L)
    val cacheSizeBytes: StateFlow<Long> = _cacheSizeBytes.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _availableDeviceFolders = MutableStateFlow<List<MediaScanner.DeviceFolderSummary>>(emptyList())
    val availableDeviceFolders: StateFlow<List<MediaScanner.DeviceFolderSummary>> = _availableDeviceFolders.asStateFlow()

    private val _genreSortField = MutableStateFlow(GenreSortField.TRACK_COUNT)
    private val _genreSortDirection = MutableStateFlow(SortDirection.DESCENDING)
    val genreSortField: StateFlow<GenreSortField> = _genreSortField.asStateFlow()
    val genreSortDirection: StateFlow<SortDirection> = _genreSortDirection.asStateFlow()

    private val _localBackups = MutableStateFlow<List<LocalBackupFileInfo>>(emptyList())
    val localBackups: StateFlow<List<LocalBackupFileInfo>> = _localBackups.asStateFlow()

    private val _pendingBackupPreview = MutableStateFlow<PendingBackupPreview?>(null)
    val pendingBackupPreview: StateFlow<PendingBackupPreview?> = _pendingBackupPreview.asStateFlow()

    private val _isBackupBusy = MutableStateFlow(false)
    val isBackupBusy: StateFlow<Boolean> = _isBackupBusy.asStateFlow()

    private val _duplicateGroups = MutableStateFlow<List<MusicRepository.DuplicateTrackGroup>>(emptyList())
    val duplicateGroups: StateFlow<List<MusicRepository.DuplicateTrackGroup>> = _duplicateGroups.asStateFlow()

    private val _isScanningDuplicates = MutableStateFlow(false)
    val isScanningDuplicates: StateFlow<Boolean> = _isScanningDuplicates.asStateFlow()

    private var debouncedScanJob: Job? = null
    private var refreshVideosJob: Job? = null
    private var hasAppliedInitialDefaultTab = false

    val settings: StateFlow<AppSettings> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val playbackState: StateFlow<PlaybackUiState> = playbackController.playbackState
    val playbackProgress: StateFlow<PlaybackProgressState> = playbackController.playbackProgress
    val equalizerState: StateFlow<EqualizerState> = playbackController.equalizerState
    val sleepTimerState: StateFlow<SleepTimerState> = playbackController.sleepTimerState
    val videoState: StateFlow<VideoPlaybackUiState> = videoPlaybackController.state
    val videoAspectRatio: StateFlow<Float> = videoPlaybackController.videoAspectRatio
    val liveWaveformHistory: StateFlow<FloatArray> = AudioSpectrumAnalyzer.waveformHistoryFlow

    val trackWaveform: StateFlow<FloatArray> = playbackState
        .map { it.currentSong }
        .distinctUntilChanged { a, b -> a?.id == b?.id && a?.dateModifiedSec == b?.dateModifiedSec }
        .flatMapLatest { song ->
            flow {
                emit(if (song == null) FloatArray(0) else WaveformExtractor.getOrExtractWaveform(app, song))
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FloatArray(0))

    val videos: StateFlow<List<VideoItem>> = combine(
        _rawVideos,
        _videoSortField,
        _videoSortDirection
    ) { list, field, dir ->
        val cmp: Comparator<VideoItem> = when (field) {
            VideoSortField.TITLE -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            VideoSortField.DURATION -> compareBy<VideoItem> { it.durationMs }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            VideoSortField.DATE_ADDED -> compareBy<VideoItem> { it.dateAddedSec }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            VideoSortField.SIZE -> compareBy<VideoItem> { it.sizeBytes }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
        }
        val sorted = list.sortedWith(cmp)
        if (dir == SortDirection.DESCENDING) sorted.reversed() else sorted
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rawSongs: StateFlow<List<Song>> = repository.allSongsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hiddenSongs: StateFlow<List<Song>> = repository.hiddenSongsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashedSongs: StateFlow<List<Song>> = repository.trashedSongsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sortedSongs: StateFlow<List<Song>> = combine(rawSongs, settings) { songs, st ->
        sortSongs(songs, st.sortField, st.sortDirection)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<Song>> = repository.favoritesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lyricsSongs: StateFlow<List<Song>> = sortedSongs
        .map { list -> list.filter { it.hasLyrics } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteSongIds: StateFlow<Set<Long>> = favorites
        .map { list -> list.mapTo(HashSet(list.size)) { it.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val recentlyPlayed: StateFlow<List<Song>> = repository.recentlyPlayedFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyAdded: StateFlow<List<Song>> = repository.recentlyAddedFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mostPlayed: StateFlow<List<Song>> = repository.mostPlayedFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<Playlist>> = repository.playlistsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sortedPlaylists: StateFlow<List<Playlist>> = combine(playlists, settings) { list, st ->
        sortPlaylists(list, st.playlistSortField, st.playlistSortDirection)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val libraryStats: StateFlow<LibraryStats> = combine(
        rawSongs,
        playlists,
        trashedSongs,
        hiddenSongs,
        repository.totalPlayEventsFlow
    ) { songs, playlistList, trashed, hidden, totalPlayEvents ->
        computeLibraryStats(songs, playlistList, trashed.size, hidden.size, totalPlayEvents)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryStats())

    val playHistory: StateFlow<List<com.example.data.db.PlayHistoryEntity>> = repository.playHistoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyPlayedPlaylists: StateFlow<List<Playlist>> = repository.recentlyPlayedPlaylistsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchHistory: StateFlow<List<String>> = repository.searchHistoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albums: StateFlow<List<Album>> = combine(rawSongs, settings) { songs, st ->
        val rawAlbums = songs.groupBy { it.albumId to it.album }
            .map { (key, list) ->
                val sorted = list.sortedBy { it.trackNumber }
                Album(
                    id = key.first,
                    name = key.second,
                    artist = sorted.firstOrNull()?.artist ?: "Unknown Artist",
                    trackCount = sorted.size,
                    year = sorted.firstOrNull { it.year > 0 }?.year ?: 0,
                    representativeSong = sorted.firstOrNull()
                )
            }
        sortAlbums(rawAlbums, st.albumSortField, st.albumSortDirection)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val artists: StateFlow<List<Artist>> = combine(rawSongs, settings) { songs, st ->
        val rawArtists = songs.groupBy { it.artistId to it.artist }
            .map { (key, list) ->
                Artist(
                    id = key.first,
                    name = key.second,
                    songCount = list.size,
                    albumCount = list.map { it.album }.distinct().size,
                    representativeSong = list.firstOrNull()
                )
            }
        sortArtists(rawArtists, st.artistSortField, st.artistSortDirection)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val genres: StateFlow<List<GenreItem>> = combine(
        rawSongs,
        _genreSortField,
        _genreSortDirection
    ) { songs, sortField, sortDir ->
        val grouped = songs.groupBy { s ->
            s.genre.trim().ifBlank { "Other / Untagged" }
        }.map { (name, list) ->
            GenreItem(
                name = name,
                songCount = list.size,
                totalDurationMs = list.sumOf { it.durationMs },
                representativeSong = list.firstOrNull()
            )
        }
        val cmp: Comparator<GenreItem> = when (sortField) {
            GenreSortField.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            GenreSortField.TRACK_COUNT -> compareBy<GenreItem> { it.songCount }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            GenreSortField.DURATION -> compareBy<GenreItem> { it.totalDurationMs }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
        }
        val sorted = grouped.sortedWith(cmp)
        if (sortDir == SortDirection.DESCENDING) sorted.reversed() else sorted
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFolders: StateFlow<List<FolderItem>> = combine(rawSongs, settings) { songs, st ->
        val rawFolders = songs.groupBy { it.folderPath }
            .map { (path, list) ->
                FolderItem(
                    name = list.firstOrNull()?.folderName ?: path.substringAfterLast('/'),
                    path = path,
                    songCount = list.size,
                    subfolderCount = 0,
                    representativeSong = list.firstOrNull(),
                    totalDurationMs = list.sumOf { it.durationMs },
                    totalSizeBytes = list.sumOf { it.sizeBytes }
                )
            }
        sortFolders(rawFolders, st.folderListSortField, st.folderListSortDirection)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folderBrowserState: StateFlow<FolderBrowserState> = combine(
        rawSongs,
        _currentFolderPath,
        _folderSortField,
        _folderSortDirection,
        settings
    ) { songs, currentPath, sortField, sortDir, st ->
        buildFolderBrowserState(
            songs = songs,
            currentPath = currentPath,
            sortField = sortField,
            sortDir = sortDir,
            folderSortField = st.folderListSortField,
            folderSortDir = st.folderListSortDirection,
            enableHierarchy = st.enableFoldersHierarchy,
            skipSingleSubfolder = st.skipSingleSubfolder
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FolderBrowserState())

    val searchResults: StateFlow<SearchResultsState> = combine(
        _searchQuery.debounce(120).distinctUntilChanged(),
        rawSongs,
        artists,
        albums,
        combine(playlists, allFolders, genres, _rawVideos) { pl, fo, ge, vi ->
            listOf(pl, fo, ge, vi)
        }
    ) { query, songs, artistList, albumList, packed ->
        @Suppress("UNCHECKED_CAST")
        val playlistList = packed[0] as List<Playlist>
        @Suppress("UNCHECKED_CAST")
        val folderList = packed[1] as List<FolderItem>
        @Suppress("UNCHECKED_CAST")
        val genreList = packed[2] as List<GenreItem>
        @Suppress("UNCHECKED_CAST")
        val videoList = packed[3] as List<VideoItem>
        val q = query.trim()
        if (q.isEmpty()) {
            SearchResultsState()
        } else {
            val matchedSongs = songs.filter {
                it.title.contains(q, ignoreCase = true) ||
                    it.artist.contains(q, ignoreCase = true) ||
                    it.album.contains(q, ignoreCase = true) ||
                    it.genre.contains(q, ignoreCase = true) ||
                    it.mood.contains(q, ignoreCase = true)
            }
            val matchedArtists = artistList.filter { it.name.contains(q, ignoreCase = true) }
            val matchedAlbums = albumList.filter {
                it.name.contains(q, ignoreCase = true) || it.artist.contains(q, ignoreCase = true)
            }
            val matchedPlaylists = playlistList.filter { it.name.contains(q, ignoreCase = true) }
            val matchedFolders = folderList.filter {
                it.path.contains(q, ignoreCase = true) || it.name.contains(q, ignoreCase = true)
            }
            val matchedVideos = videoList.filter {
                it.title.contains(q, ignoreCase = true) || it.folderName.contains(q, ignoreCase = true)
            }
            val matchedGenres = genreList.filter { it.name.contains(q, ignoreCase = true) }
            SearchResultsState(
                query = query,
                songs = matchedSongs,
                artists = matchedArtists,
                albums = matchedAlbums,
                playlists = matchedPlaylists,
                folders = matchedFolders,
                videos = matchedVideos,
                genres = matchedGenres
            )
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchResultsState())

    init {
        playbackController.connect()
        viewModelScope.launch {
            val initial = repository.settingsFlow.first()
            ArtworkManager.groupByAlbumEnabled = initial.groupArtworkByAlbum
            if (!hasAppliedInitialDefaultTab) {
                hasAppliedInitialDefaultTab = true
                val startTab = if (initial.defaultTab in initial.enabledTabs) {
                    initial.defaultTab
                } else {
                    initial.enabledTabs.firstOrNull() ?: MainTab.SONGS
                }
                _selectedTab.value = startTab
            }
            if ((initial.autoScanAudio || initial.autoScanOnStartup) &&
                (_hasAudioPermission.value || initial.includedFolderUris.isNotEmpty())
            ) {
                rescanLibrary()
                refreshDeviceFolders()
            }
            if (initial.autoScanVideo || initial.autoScanOnStartup) {
                refreshVideos()
            }
        }
        viewModelScope.launch {
            settings.map { it.enabledTabs }.distinctUntilChanged().collect { enabled ->
                if (enabled.isNotEmpty() && _selectedTab.value !in enabled) {
                    _selectedTab.value = enabled.first()
                }
            }
        }
        viewModelScope.launch {
            settings.map { it.groupArtworkByAlbum }.distinctUntilChanged().collect { enabled ->
                ArtworkManager.groupByAlbumEnabled = enabled
            }
        }
        observeCurrentTrackArtAndLyrics()
        refreshLocalBackups()
        viewModelScope.launch(Dispatchers.IO) {
            _cacheSizeBytes.value = repository.getTotalCacheSizeBytes()
        }
        viewModelScope.launch {
            delay(1500L)
            val autoCreated = repository.runScheduledAutoBackupIfDue()
            if (autoCreated != null) {
                refreshLocalBackups()
            }
        }
    }

    private fun observeCurrentTrackArtAndLyrics() {
        viewModelScope.launch {
            playbackState
                .map { it.currentSong }
                .distinctUntilChanged { old, new -> old?.id == new?.id && old?.dateModifiedSec == new?.dateModifiedSec }
                .collect { song ->
                    if (song == null) {
                        _currentArtworkColors.value = null
                        _currentBrightArtworkColors.value = null
                        _currentLyrics.value = null
                    } else {
                        launch(Dispatchers.Default) {
                            val bundle = ArtworkManager.extractArtworkColorBundle(app, song)
                            _currentArtworkColors.value = bundle?.deepPair
                            _currentBrightArtworkColors.value = bundle?.brightPair
                        }
                        launch(Dispatchers.IO) {
                            _currentLyrics.value = repository.loadLyrics(song)
                        }
                    }
                }
        }
    }

    fun selectTab(tab: MainTab) {
        _selectedTab.value = tab
    }

    fun onPermissionChanged(granted: Boolean) {
        _hasAudioPermission.value = granted
        if (granted) {
            rescanLibrary()
            refreshDeviceFolders()
            refreshVideos()
        }
    }

    // Video library actions (Play-policy compliant automatic discovery)
    fun refreshVideos() {
        refreshVideosJob?.cancel()
        refreshVideosJob = viewModelScope.launch {
            _isLoadingVideos.value = true
            val curSettings = settings.value
            val extraTrees = curSettings.includedFolderUris + curSettings.videoFolderUris
            val discovered = runCatching {
                VideoScanner.loadAllVideos(
                    context = app,
                    extraTreeUris = extraTrees,
                    obeyNoMedia = curSettings.obeyNoMedia,
                    excludedFolders = curSettings.excludedFolders,
                    extensionBlacklist = curSettings.extensionBlacklist
                )
            }.getOrDefault(_rawVideos.value)
            _rawVideos.value = discovered
            _isLoadingVideos.value = false
        }
    }

    fun onVideosPicked(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            VideoScanner.persistPickedVideoUris(app, uris)
            refreshVideos()
            val label = if (uris.size == 1) "Added 1 video" else "Added ${uris.size} videos"
            _statusMessage.value = label
        }
    }

    fun onVideoFolderPicked(treeUri: Uri) {
        viewModelScope.launch {
            VideoScanner.persistVideoFolderTreeUri(app, treeUri)
            val updated = settings.value.videoFolderUris + treeUri.toString()
            repository.settingsDataStore.updateVideoFolderUris(updated)
            refreshVideos()
        }
    }

    fun playExternalVideoUri(uri: Uri) {
        viewModelScope.launch {
            VideoScanner.persistPickedVideoUris(app, listOf(uri))
            val item = VideoScanner.inspectVideoUri(app, uri)
            if (item != null) {
                val merged = (_rawVideos.value.filterNot { it.contentUri == item.contentUri } + item)
                _rawVideos.value = merged
                videoPlaybackController.playVideo(item, listOf(item), openFullScreen = true)
            } else {
                _statusMessage.value = "Unable to open video file"
            }
        }
    }

    fun playVideo(video: VideoItem) {
        val currentList = videos.value.ifEmpty { listOf(video) }
        videoPlaybackController.playVideo(video, currentList, openFullScreen = true)
    }

    fun removeVideoFromList(video: VideoItem) {
        VideoScanner.removePersistedVideoUri(app, video.contentUri)
        _rawVideos.value = _rawVideos.value.filterNot { it.contentUri == video.contentUri }
    }

    fun setVideoSort(field: VideoSortField, direction: SortDirection) {
        _videoSortField.value = field
        _videoSortDirection.value = direction
    }

    fun refreshDeviceFolders() {
        viewModelScope.launch {
            _availableDeviceFolders.value = repository.getDeviceAudioFolders()
        }
    }

    private fun scheduleDebouncedRescan() {
        debouncedScanJob?.cancel()
        debouncedScanJob = viewModelScope.launch {
            delay(320L)
            rescanLibrary()
        }
    }

    fun rescanLibrary() {
        if (_isScanning.value) return
        viewModelScope.launch {
            _isScanning.value = true
            runCatching { repository.scanLibrary() }
            _isScanning.value = false
            _cacheSizeBytes.value = withContext(Dispatchers.IO) { repository.getTotalCacheSizeBytes() }
            if (settings.value.autoScanVideo) {
                refreshVideos()
            }
        }
    }

    fun importAudioUri(uri: Uri) {
        viewModelScope.launch {
            val song = repository.importAudioUri(uri)
            if (song != null) {
                playbackController.playSong(song, listOf(song))
                _statusMessage.value = "Playing ${song.title}"
            } else {
                _statusMessage.value = "Could not open selected audio file"
            }
        }
    }

    fun importLyricsUri(uri: Uri) {
        viewModelScope.launch {
            val loaded = LyricsParser.loadFromUri(app, uri)
            if (loaded != null) {
                val (rawText, parsed) = loaded
                val activeSong = playbackState.value.currentSong
                if (activeSong != null) {
                    val saved = repository.saveCustomLyrics(activeSong.id, rawText)
                    _currentLyrics.value = saved ?: parsed
                } else {
                    _currentLyrics.value = parsed
                }
                _statusMessage.value = "Lyrics imported & saved"
            } else {
                _statusMessage.value = "Could not read LRC file"
            }
        }
    }

    fun saveCustomLyricsForCurrentSong(rawText: String) {
        val activeSong = playbackState.value.currentSong ?: return
        viewModelScope.launch {
            val saved = repository.saveCustomLyrics(activeSong.id, rawText)
            _currentLyrics.value = saved
            _statusMessage.value = if (saved != null) "Lyrics saved for track" else "Custom lyrics cleared"
        }
    }

    fun clearCustomLyricsForCurrentSong() {
        val activeSong = playbackState.value.currentSong ?: return
        viewModelScope.launch {
            val fallback = repository.clearCustomLyrics(activeSong)
            _currentLyrics.value = fallback
            _statusMessage.value = "Reverted to embedded/sidecar lyrics"
        }
    }

    // Playback actions
    fun playSong(song: Song, queueContext: List<Song> = sortedSongs.value, playlistId: Long? = null) {
        playbackController.playSong(song, queueContext, playlistId)
    }

    fun playAll(songs: List<Song>, shuffle: Boolean = false, playlistId: Long? = null) {
        playbackController.playQueue(songs, startIndex = 0, shuffle = shuffle, playlistId = playlistId)
    }

    fun playNext(song: Song) {
        playbackController.playNext(song)
    }

    fun addToQueue(songs: List<Song>) {
        playbackController.addToQueue(songs)
    }

    fun toggleFavorite(songId: Long) {
        val currentlyFav = songId in favoriteSongIds.value ||
            playbackState.value.currentSong?.takeIf { it.id == songId }?.isFavorite == true ||
            rawSongs.value.firstOrNull { it.id == songId }?.isFavorite == true
        val targetFav = !currentlyFav
        playbackController.updateSongFavoriteInQueue(songId, targetFav)
        viewModelScope.launch {
            repository.setFavorite(songId, targetFav)
            MikaMusicWidgetProvider.requestWidgetUpdate(app)
        }
    }

    fun setSongMood(songId: Long, mood: String) {
        playbackController.updateSongsMoodRatingInQueue(setOf(songId), mood = mood.trim())
        viewModelScope.launch {
            repository.setSongMood(songId, mood)
        }
    }

    fun setSongRating(songId: Long, rating: Int) {
        val clamped = rating.coerceIn(0, 5)
        playbackController.updateSongsMoodRatingInQueue(setOf(songId), rating = clamped)
        viewModelScope.launch {
            repository.setSongRating(songId, clamped)
        }
    }

    fun bulkSetMood(mood: String) {
        val ids = _selectedSongIds.value.toList()
        if (ids.isEmpty()) return
        playbackController.updateSongsMoodRatingInQueue(ids.toSet(), mood = mood.trim())
        viewModelScope.launch {
            repository.setSongMoodBulk(ids, mood)
            clearSelection()
        }
    }

    fun bulkSetRating(rating: Int) {
        val ids = _selectedSongIds.value.toList()
        if (ids.isEmpty()) return
        val clamped = rating.coerceIn(0, 5)
        playbackController.updateSongsMoodRatingInQueue(ids.toSet(), rating = clamped)
        viewModelScope.launch {
            repository.setSongRatingBulk(ids, clamped)
            clearSelection()
        }
    }

    fun setPlaybackSpeedAndPitch(speed: Float, pitch: Float) {
        playbackController.setPlaybackSpeedAndPitch(speed, pitch)
    }

    // Multi-selection actions
    fun toggleSongSelection(songId: Long) {
        val current = _selectedSongIds.value.toMutableSet()
        if (!current.add(songId)) {
            current.remove(songId)
        }
        _selectedSongIds.value = current
    }

    fun selectAllVisible(songs: List<Song>) {
        _selectedSongIds.value = songs.map { it.id }.toSet()
    }

    fun clearSelection() {
        _selectedSongIds.value = emptySet()
    }

    fun bulkAddToQueue() {
        val ids = _selectedSongIds.value
        val songs = sortedSongs.value.filter { it.id in ids }
        if (songs.isNotEmpty()) {
            addToQueue(songs)
        }
        clearSelection()
    }

    fun bulkFavorite(favorite: Boolean) {
        val ids = _selectedSongIds.value.toList()
        playbackController.updateSongsFavoriteInQueue(ids.toSet(), favorite)
        viewModelScope.launch {
            repository.setFavoritesBulk(ids, favorite)
            clearSelection()
        }
    }

    fun bulkMoveToTrash() {
        val ids = _selectedSongIds.value.toList()
        if (ids.isEmpty()) return
        playbackController.removeSongsFromQueue(ids.toSet())
        viewModelScope.launch {
            repository.moveSongsToTrash(ids)
            val label = if (ids.size == 1) "Moved 1 track to Trash" else "Moved ${ids.size} tracks to Trash"
            _statusMessage.value = label
            clearSelection()
        }
    }

    fun moveSongToTrash(song: Song) {
        playbackController.removeSongsFromQueue(setOf(song.id))
        viewModelScope.launch {
            repository.moveSongsToTrash(listOf(song.id))
            _statusMessage.value = "Moved \"${song.title}\" to Trash"
        }
    }

    fun hideSongs(songIds: List<Long>) {
        if (songIds.isEmpty()) return
        playbackController.removeSongsFromQueue(songIds.toSet())
        viewModelScope.launch {
            repository.setHiddenBulk(songIds, true)
            clearSelection()
        }
    }

    fun unhideSongs(songIds: List<Long>) {
        if (songIds.isEmpty()) return
        viewModelScope.launch {
            repository.setHiddenBulk(songIds, false)
        }
    }

    fun restoreSongsFromTrash(songIds: List<Long>) {
        if (songIds.isEmpty()) return
        viewModelScope.launch {
            repository.restoreSongsFromTrash(songIds)
            val label = if (songIds.size == 1) "Restored 1 track to Library" else "Restored ${songIds.size} tracks to Library"
            _statusMessage.value = label
        }
    }

    fun restoreAllFromTrash() {
        val ids = trashedSongs.value.map { it.id }
        if (ids.isEmpty()) return
        restoreSongsFromTrash(ids)
    }

    fun finalizePermanentDelete(songs: List<Song>, deletedBySystemPrompt: Boolean = false) {
        if (songs.isEmpty()) return
        val ids = songs.map { it.id }.toSet()
        playbackController.removeSongsFromQueue(ids)
        viewModelScope.launch {
            repository.permanentlyDeleteSongs(songs, attemptDeviceFileDelete = !deletedBySystemPrompt)
            _statusMessage.value = if (songs.size == 1) {
                "Deleted \"${songs.first().title}\""
            } else {
                "Deleted ${songs.size} tracks"
            }
            if (_duplicateGroups.value.isNotEmpty()) {
                scanDuplicates()
            }
        }
    }

    // Playlist actions
    fun createPlaylist(name: String, initialSongIds: List<Long> = emptyList()) {
        viewModelScope.launch {
            repository.createPlaylist(name, initialSongIds)
        }
    }

    fun renamePlaylist(playlistId: Long, newName: String) {
        viewModelScope.launch {
            repository.renamePlaylist(playlistId, newName)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
        }
    }

    fun addSongsToPlaylist(playlistId: Long, songIds: List<Long>) {
        viewModelScope.launch {
            repository.addSongsToPlaylist(playlistId, songIds)
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(playlistId, songId)
        }
    }

    fun reorderPlaylistSongs(playlistId: Long, orderedSongIds: List<Long>) {
        viewModelScope.launch {
            repository.reorderPlaylistSongs(playlistId, orderedSongIds)
        }
    }

    // Folder browsing
    fun navigateToFolder(path: String?) {
        _currentFolderPath.value = path
    }

    fun navigateFolderUp(): Boolean {
        val cur = _currentFolderPath.value ?: return false
        val parent = File(cur).parent
        val hasParentAudio = parent != null && rawSongs.value.any {
            it.folderPath == parent || it.folderPath.startsWith("$parent/")
        }
        _currentFolderPath.value = if (hasParentAudio && parent != "/") parent else null
        return true
    }

    fun updateFolderSort(field: SortField, direction: SortDirection) {
        _folderSortField.value = field
        _folderSortDirection.value = direction
    }

    // Search
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun commitSearchQuery(query: String) {
        viewModelScope.launch {
            repository.saveSearchQuery(query)
        }
    }

    fun removeSearchHistoryItem(query: String) {
        viewModelScope.launch {
            repository.removeSearchQuery(query)
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            repository.clearSearchHistory()
        }
    }

    // Appearance & Settings
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.settingsDataStore.updateThemeMode(mode) }
    }

    fun setAccentColor(id: String) {
        viewModelScope.launch { repository.settingsDataStore.updateAccentColor(id) }
    }

    fun setCustomAccentColor(argb: Long) {
        viewModelScope.launch { repository.settingsDataStore.updateCustomAccentColor(argb) }
    }

    fun setCustomDarkAccentColor(argb: Long) {
        viewModelScope.launch { repository.settingsDataStore.updateCustomDarkAccentColor(argb) }
    }

    fun setAutoColoring(enabled: Boolean) {
        viewModelScope.launch { repository.settingsDataStore.updateAutoColoring(enabled) }
    }

    fun setPickColorsFromWallpaper(enabled: Boolean) {
        viewModelScope.launch { repository.settingsDataStore.updatePickColorsFromWallpaper(enabled) }
    }

    fun setForceMiniPlayerTrackColors(enabled: Boolean) {
        viewModelScope.launch { repository.settingsDataStore.updateForceMiniPlayerTrackColors(enabled) }
    }

    fun setUsePitchBlack(enabled: Boolean) {
        viewModelScope.launch { repository.settingsDataStore.updateUsePitchBlack(enabled) }
    }

    fun setGradientStyle(style: GradientStyle) {
        viewModelScope.launch { repository.settingsDataStore.updateGradientStyle(style) }
    }

    fun setGradientTargets(miniPlayer: Boolean, fullPlayer: Boolean, surfaces: Boolean) {
        viewModelScope.launch {
            repository.settingsDataStore.updateGradientTargets(miniPlayer, fullPlayer, surfaces)
        }
    }

    fun setSort(field: SortField, direction: SortDirection) {
        viewModelScope.launch { repository.settingsDataStore.updateSort(field, direction) }
    }

    fun setPlaylistSort(field: PlaylistSortField, direction: SortDirection) {
        viewModelScope.launch { repository.settingsDataStore.updatePlaylistSort(field, direction) }
    }

    fun setFolderListSort(field: FolderSortField, direction: SortDirection) {
        viewModelScope.launch { repository.settingsDataStore.updateFolderListSort(field, direction) }
    }

    fun setAlbumSort(field: AlbumSortField, direction: SortDirection) {
        viewModelScope.launch { repository.settingsDataStore.updateAlbumSort(field, direction) }
    }

    fun setArtistSort(field: ArtistSortField, direction: SortDirection) {
        viewModelScope.launch { repository.settingsDataStore.updateArtistSort(field, direction) }
    }

    fun setGenreSort(field: GenreSortField, direction: SortDirection) {
        _genreSortField.value = field
        _genreSortDirection.value = direction
    }

    fun setGaplessPlayback(enabled: Boolean) {
        viewModelScope.launch { repository.settingsDataStore.updateGapless(enabled) }
    }

    fun setCrossfade(seconds: Int) {
        viewModelScope.launch { repository.settingsDataStore.updateCrossfade(seconds) }
    }

    fun setSkipInterval(seconds: Int) {
        viewModelScope.launch { repository.settingsDataStore.updateSkipInterval(seconds) }
    }

    fun setRememberLastPosition(remember: Boolean) {
        viewModelScope.launch { repository.settingsDataStore.updateRememberLastPosition(remember) }
    }

    fun setShowBottomNavBar(show: Boolean) {
        viewModelScope.launch { repository.settingsDataStore.updateShowBottomNavBar(show) }
    }

    fun moveBottomNavDest(dest: BottomNavDest, delta: Int) {
        val current = settings.value.bottomNavOrder.toMutableList()
        val idx = current.indexOf(dest)
        if (idx < 0) return
        val target = (idx + delta).coerceIn(0, current.lastIndex)
        if (target == idx) return
        val item = current.removeAt(idx)
        current.add(target, item)
        viewModelScope.launch { repository.settingsDataStore.updateBottomNavOrder(current) }
    }

    fun reorderBottomNavDest(ordered: List<BottomNavDest>) {
        viewModelScope.launch { repository.settingsDataStore.updateBottomNavOrder(ordered) }
    }

    fun toggleBottomNavDestHidden(dest: BottomNavDest) {
        val current = settings.value.hiddenBottomNavItems
        val next = if (dest in current) {
            current - dest
        } else {
            val visibleAfter = BottomNavDest.entries.count { it !in current && it != dest }
            if (visibleAfter < 1) {
                _statusMessage.value = "Keep at least one navigation tab"
                return
            }
            current + dest
        }
        viewModelScope.launch { repository.settingsDataStore.updateHiddenBottomNavItems(next) }
    }

    fun setAutoScan(audio: Boolean? = null, video: Boolean? = null) {
        viewModelScope.launch {
            repository.settingsDataStore.updateAutoScan(audio, video)
            if (audio == true) {
                rescanLibrary()
                refreshDeviceFolders()
            }
            if (video == true) {
                refreshVideos()
            }
        }
    }

    fun setVideoGridView(grid: Boolean) {
        viewModelScope.launch { repository.settingsDataStore.updateVideoGridView(grid) }
    }

    fun setSoundControl(soundControl: SoundControlSettings, speedOverride: Float? = null) {
        playbackController.setSoundControl(soundControl, speedOverride)
    }

    fun setAllowSimultaneousPlayback(enabled: Boolean) {
        viewModelScope.launch { repository.settingsDataStore.updateAllowSimultaneousPlayback(enabled) }
    }

    fun setShowLyricsBadges(show: Boolean) {
        viewModelScope.launch { repository.settingsDataStore.updateShowLyricsBadges(show) }
    }

    fun setFontScale(scale: Float) {
        viewModelScope.launch { repository.settingsDataStore.updateFontScale(scale) }
    }

    fun setAppIconScale(scale: Float) {
        viewModelScope.launch { repository.settingsDataStore.updateAppIconScale(scale) }
    }

    fun setCornerRadiusDp(radiusDp: Int) {
        viewModelScope.launch { repository.settingsDataStore.updateCornerRadiusDp(radiusDp) }
    }

    fun setSeekBarStyle(style: com.example.model.SeekBarStyle) {
        viewModelScope.launch { repository.settingsDataStore.updateSeekBarStyle(style) }
    }

    fun setShowActualWaveformSeekbar(enabled: Boolean) {
        viewModelScope.launch { repository.settingsDataStore.updateShowActualWaveformSeekbar(enabled) }
    }

    fun setShowLiveWaveformSeekbar(enabled: Boolean) {
        viewModelScope.launch { repository.settingsDataStore.updateShowLiveWaveformSeekbar(enabled) }
    }

    fun setPlaybackIconStyle(style: PlaybackIconStyle) {
        viewModelScope.launch { repository.settingsDataStore.updatePlaybackIconStyle(style) }
    }

    fun setColorTargets(onApp: Boolean? = null, onPlayer: Boolean? = null) {
        viewModelScope.launch { repository.settingsDataStore.updateColorTargets(onApp, onPlayer) }
    }

    fun setAppIconPreset(preset: AppIconPreset) {
        viewModelScope.launch {
            AppIconManager.applyLauncherPreset(app, preset)
            repository.settingsDataStore.updateAppIconPreset(preset)
        }
    }

    fun importCustomAppIcon(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            AppIconManager.saveAndPinCustomIcon(app, uri)
                .onSuccess { path ->
                    repository.settingsDataStore.updateAppIconPreset(AppIconPreset.CUSTOM, path)
                    _statusMessage.value = "Custom app icon applied"
                }
                .onFailure {
                    _statusMessage.value = "Could not apply custom icon"
                }
        }
    }

    fun toggleTabEnabled(tab: MainTab) {
        val current = settings.value.enabledTabs
        val next = if (tab in current) {
            if (current.size <= 1) return
            current - tab
        } else {
            current + tab
        }
        viewModelScope.launch { repository.settingsDataStore.updateEnabledTabs(next) }
    }

    fun moveEnabledTab(tab: MainTab, delta: Int) {
        val current = settings.value.enabledTabs.toMutableList()
        val idx = current.indexOf(tab)
        if (idx < 0) return
        val target = (idx + delta).coerceIn(0, current.lastIndex)
        if (target == idx) return
        val item = current.removeAt(idx)
        current.add(target, item)
        viewModelScope.launch { repository.settingsDataStore.updateEnabledTabs(current) }
    }

    fun reorderEnabledTabs(ordered: List<MainTab>) {
        viewModelScope.launch { repository.settingsDataStore.updateEnabledTabs(ordered) }
    }

    fun setDefaultTab(tab: MainTab) {
        if (tab !in settings.value.enabledTabs) return
        viewModelScope.launch { repository.settingsDataStore.updateDefaultTab(tab) }
    }

    fun setTabActions(tapAction: TabTapAction? = null, longPressAction: TabLongPressAction? = null) {
        viewModelScope.launch { repository.settingsDataStore.updateTabActions(tapAction, longPressAction) }
    }

    fun updateSongTagsAndArtwork(
        songIds: List<Long>,
        title: String? = null,
        artist: String? = null,
        album: String? = null,
        genre: String? = null,
        year: Int? = null,
        trackNumber: Int? = null,
        artworkUri: Uri? = null,
        clearArtwork: Boolean = false
    ) {
        if (songIds.isEmpty()) return
        viewModelScope.launch {
            val updated = repository.updateSongTagsAndArtwork(
                songIds = songIds,
                title = title,
                artist = artist,
                album = album,
                genre = genre,
                year = year,
                trackNumber = trackNumber,
                artworkUri = artworkUri,
                clearArtwork = clearArtwork
            )
            playbackController.refreshUpdatedSongs(updated)
            val currentId = playbackState.value.currentSong?.id
            if (currentId != null) {
                val activeUpdated = updated.firstOrNull { it.id == currentId }
                if (activeUpdated != null) {
                    launch(Dispatchers.Default) {
                        val bundle = ArtworkManager.extractArtworkColorBundle(app, activeUpdated)
                        _currentArtworkColors.value = bundle?.deepPair
                        _currentBrightArtworkColors.value = bundle?.brightPair
                    }
                }
            }
            _statusMessage.value = if (songIds.size == 1) {
                "Updated tags & cover for \"${updated.firstOrNull()?.title ?: "track"}\""
            } else {
                "Updated ${songIds.size} tracks"
            }
            clearSelection()
        }
    }

    fun toggleExcludedFolder(folderPath: String) {
        val current = settings.value.excludedFolders.toMutableSet()
        if (!current.add(folderPath)) {
            current.remove(folderPath)
        }
        viewModelScope.launch {
            repository.settingsDataStore.updateExcludedFolders(current)
            scheduleDebouncedRescan()
        }
    }

    fun clearExcludedFolders() {
        viewModelScope.launch {
            repository.settingsDataStore.updateExcludedFolders(emptySet())
            scheduleDebouncedRescan()
        }
    }

    fun setMinDurationSec(seconds: Int) {
        viewModelScope.launch {
            repository.settingsDataStore.updateMinDurationSec(seconds)
            scheduleDebouncedRescan()
        }
    }

    fun setMinFileSizeKb(kb: Int) {
        viewModelScope.launch {
            repository.settingsDataStore.updateMinFileSizeKb(kb)
            scheduleDebouncedRescan()
        }
    }

    fun setObeyNoMedia(enabled: Boolean) {
        viewModelScope.launch {
            repository.settingsDataStore.updateObeyNoMedia(enabled)
            scheduleDebouncedRescan()
            refreshVideos()
        }
    }

    fun setAutoScanOnStartup(enabled: Boolean) {
        viewModelScope.launch { repository.settingsDataStore.updateAutoScanOnStartup(enabled) }
    }

    fun updateExtensionBlacklist(extensions: Set<String>) {
        viewModelScope.launch {
            repository.settingsDataStore.updateExtensionBlacklist(extensions)
            scheduleDebouncedRescan()
            refreshVideos()
        }
    }

    fun setGroupArtworkByAlbum(enabled: Boolean) {
        ArtworkManager.groupByAlbumEnabled = enabled
        viewModelScope.launch {
            repository.settingsDataStore.updateGroupArtworkByAlbum(enabled)
        }
    }

    fun setShowActualArtworkRatio(enabled: Boolean) {
        viewModelScope.launch { repository.settingsDataStore.updateShowActualArtworkRatio(enabled) }
    }

    fun setShowLikeButtonInList(enabled: Boolean) {
        viewModelScope.launch { repository.settingsDataStore.updateShowLikeButtonInList(enabled) }
    }

    fun setTrackSwipeActions(left: TrackSwipeAction? = null, right: TrackSwipeAction? = null) {
        viewModelScope.launch { repository.settingsDataStore.updateTrackSwipeActions(left, right) }
    }

    fun setSwipeToOpenDrawer(enabled: Boolean) {
        viewModelScope.launch { repository.settingsDataStore.updateSwipeToOpenDrawer(enabled) }
    }

    fun scanDuplicates() {
        if (_isScanningDuplicates.value) return
        viewModelScope.launch {
            _isScanningDuplicates.value = true
            _duplicateGroups.value = withContext(Dispatchers.Default) {
                repository.findDuplicateGroups()
            }
            _isScanningDuplicates.value = false
        }
    }

    fun deleteDuplicateSong(song: Song, permanent: Boolean = false) {
        if (permanent) {
            finalizePermanentDelete(listOf(song))
        } else {
            playbackController.removeSongsFromQueue(setOf(song.id))
            viewModelScope.launch {
                repository.moveSongsToTrash(listOf(song.id))
                scanDuplicates()
            }
        }
    }

    fun saveArtworkToMikaFolder(song: Song) {
        viewModelScope.launch {
            ArtworkManager.saveArtworkToMikaFolder(app, song)
                .onSuccess { savedPath ->
                    _statusMessage.value = "Artwork saved to $savedPath"
                }
                .onFailure {
                    _statusMessage.value = "Unable to save artwork"
                }
        }
    }

    suspend fun inspectArtworkDetails(song: Song): ArtworkDetails =
        ArtworkManager.inspectArtworkDetails(app, song)

    fun setFoldersHierarchy(enableHierarchy: Boolean, skipSingleSubfolder: Boolean) {
        viewModelScope.launch {
            repository.settingsDataStore.updateFoldersHierarchy(enableHierarchy, skipSingleSubfolder)
            if (!enableHierarchy) {
                _currentFolderPath.value = null
            }
        }
    }

    fun addIncludedFolderTreeUri(treeUri: Uri) {
        runCatching {
            app.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        val current = settings.value.includedFolderUris + treeUri.toString()
        viewModelScope.launch {
            repository.settingsDataStore.updateIncludedFolderUris(current)
            runCatching { repository.scanLibrary() }
            if (settings.value.autoScanVideo) {
                refreshVideos()
            }
        }
    }

    fun setDefaultBackupLocation(treeUri: Uri?) {
        viewModelScope.launch {
            if (treeUri == null) {
                repository.settingsDataStore.updateBackupLocation(
                    uri = "",
                    label = "/mika/backup"
                )
            } else {
                runCatching {
                    app.contentResolver.takePersistableUriPermission(
                        treeUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }
                val docId = runCatching { android.provider.DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull() ?: ""
                val nicePath = if (docId.startsWith("primary:")) {
                    "/storage/emulated/0/" + docId.removePrefix("primary:").trim('/') + "/"
                } else {
                    treeUri.lastPathSegment?.replace(':', '/') ?: "Selected Backup Folder"
                }
                repository.settingsDataStore.updateBackupLocation(treeUri.toString(), nicePath)
            }
        }
    }

    fun setSkipSilence(enabled: Boolean) {
        viewModelScope.launch { repository.settingsDataStore.updateSkipSilence(enabled) }
    }

    fun setCrossfadeEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.settingsDataStore.updateCrossfadeEnabled(enabled) }
    }

    fun setCrossfadeDurationMs(ms: Int) {
        viewModelScope.launch { repository.settingsDataStore.updateCrossfadeDurationMs(ms) }
    }

    fun setCrossfadeTriggerLastSec(sec: Int) {
        viewModelScope.launch { repository.settingsDataStore.updateCrossfadeTriggerLastSec(sec) }
    }

    fun setCrossfadeStyle(style: CrossfadeStyle) {
        viewModelScope.launch { repository.settingsDataStore.updateCrossfadeStyle(style) }
    }

    fun setPlayPauseFadeEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.settingsDataStore.updatePlayPauseFadeEnabled(enabled) }
    }

    fun setPlayFadeDurationMs(ms: Int) {
        viewModelScope.launch { repository.settingsDataStore.updatePlayFadeDurationMs(ms) }
    }

    fun setPauseFadeDurationMs(ms: Int) {
        viewModelScope.launch { repository.settingsDataStore.updatePauseFadeDurationMs(ms) }
    }

    fun setJumpToFirstAfterQueue(enabled: Boolean) {
        viewModelScope.launch { repository.settingsDataStore.updateJumpToFirstAfterQueue(enabled) }
    }

    fun setPreviousButtonReplays(enabled: Boolean) {
        viewModelScope.launch { repository.settingsDataStore.updatePreviousButtonReplays(enabled) }
    }

    fun setRestorePositionMinDurationMin(minutes: Int) {
        viewModelScope.launch { repository.settingsDataStore.updateRestorePositionMinDurationMin(minutes) }
    }

    fun setVisualEffects(
        blur: Boolean? = null,
        glow: Boolean? = null,
        parallax: Boolean? = null,
        backgroundEffect: BackgroundEffect? = null
    ) {
        viewModelScope.launch {
            repository.settingsDataStore.updateVisualEffects(blur, glow, parallax, backgroundEffect)
        }
    }

    fun setPlayerBackground(
        mode: PlayerBackgroundMode? = null,
        customUri: String? = null,
        vignette: Boolean? = null,
        animated: Boolean? = null,
        colorWhenExpanded: Boolean? = null
    ) {
        viewModelScope.launch {
            repository.settingsDataStore.updatePlayerBackground(mode, customUri, vignette, animated, colorWhenExpanded)
        }
    }

    fun setArtworkGestures(
        doubleTapToToggleLyrics: Boolean? = null,
        tapAction: ArtworkGestureAction? = null,
        longPressAction: ArtworkGestureAction? = null
    ) {
        viewModelScope.launch {
            repository.settingsDataStore.updateArtworkGestures(doubleTapToToggleLyrics, tapAction, longPressAction)
        }
    }

    fun resetArtworkGestures() {
        setArtworkGestures(
            doubleTapToToggleLyrics = true,
            tapAction = ArtworkGestureAction.ARTWORK_OPEN,
            longPressAction = ArtworkGestureAction.INFO
        )
    }

    fun saveWidgetConfig(config: WidgetConfig) {
        viewModelScope.launch {
            repository.settingsDataStore.updateWidgetConfig(config)
            MikaMusicWidgetProvider.requestWidgetUpdate(app)
        }
    }

    fun resetWidgetConfig() {
        saveWidgetConfig(WidgetConfig())
    }

    fun saveMiniPlayerConfig(config: com.example.model.MiniPlayerConfig) {
        viewModelScope.launch {
            repository.settingsDataStore.updateMiniPlayerConfig(config)
        }
    }

    fun resetMiniPlayerConfig() {
        saveMiniPlayerConfig(com.example.model.MiniPlayerConfig())
    }

    fun saveFullPlayerConfig(config: com.example.model.FullPlayerConfig) {
        viewModelScope.launch {
            repository.settingsDataStore.updateFullPlayerConfig(config)
        }
    }

    fun resetFullPlayerConfig() {
        saveFullPlayerConfig(com.example.model.FullPlayerConfig())
    }

    fun clearArtworkCache() {
        viewModelScope.launch(Dispatchers.IO) {
            val freed = repository.clearArtworkCache()
            _cacheSizeBytes.value = repository.getTotalCacheSizeBytes()
            _statusMessage.value = "Cleared artwork cache (${com.example.util.Formatters.formatFileSize(freed)})"
        }
    }

    fun clearAllAppCaches() {
        viewModelScope.launch(Dispatchers.IO) {
            val freed = repository.clearAllAppCaches()
            _cacheSizeBytes.value = repository.getTotalCacheSizeBytes()
            _statusMessage.value = "Cleared app cache (${com.example.util.Formatters.formatFileSize(freed)})"
        }
    }

    fun clearPlaybackHistory() {
        viewModelScope.launch {
            repository.clearPlaybackHistory()
            _statusMessage.value = "Playback history cleared"
        }
    }

    fun resetAllSettings() {
        viewModelScope.launch {
            repository.resetAllSettings()
            _statusMessage.value = "Settings reset to defaults"
        }
    }

    fun clearAllAppData() {
        viewModelScope.launch {
            videoPlaybackController.stopAndDismiss()
            playbackController.clearQueue()
            repository.clearAllAppData()
            VideoScanner.clearAllPersistedVideoSources(app)
            _rawVideos.value = emptyList()
            _cacheSizeBytes.value = withContext(Dispatchers.IO) { repository.getTotalCacheSizeBytes() }
            _statusMessage.value = "All Mika Player data cleared"
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    // Backup & Restore
    fun refreshLocalBackups() {
        viewModelScope.launch {
            _localBackups.value = repository.listLocalBackups()
        }
    }

    fun createLocalBackup() {
        if (_isBackupBusy.value) return
        viewModelScope.launch {
            _isBackupBusy.value = true
            runCatching {
                repository.createLocalBackup()
            }.onSuccess { info ->
                refreshLocalBackups()
                _statusMessage.value = "Backup saved: ${info.name}"
            }.onFailure {
                _statusMessage.value = "Failed to create backup"
            }
            _isBackupBusy.value = false
        }
    }

    fun exportBackupToUri(uri: Uri) {
        viewModelScope.launch {
            _isBackupBusy.value = true
            runCatching {
                repository.exportBackupToUri(uri)
            }.onSuccess { payload ->
                _statusMessage.value = "Exported ${payload.playlists.size} playlists & ${payload.songUserStates.size} track records"
            }.onFailure {
                _statusMessage.value = "Failed to export backup file"
            }
            _isBackupBusy.value = false
        }
    }

    fun inspectBackupUri(uri: Uri) {
        viewModelScope.launch {
            _isBackupBusy.value = true
            runCatching {
                repository.parseBackupUri(uri)
            }.onSuccess { payload ->
                _pendingBackupPreview.value = PendingBackupPreview(
                    payload = payload,
                    sourceLabel = "Selected Backup Archive"
                )
            }.onFailure {
                _statusMessage.value = "Invalid or unreadable Mika backup file"
            }
            _isBackupBusy.value = false
        }
    }

    fun inspectLocalBackup(info: LocalBackupFileInfo) {
        viewModelScope.launch {
            _isBackupBusy.value = true
            runCatching {
                repository.parseLocalBackupFile(info.file)
            }.onSuccess { payload ->
                _pendingBackupPreview.value = PendingBackupPreview(
                    payload = payload,
                    sourceLabel = info.name
                )
            }.onFailure {
                _statusMessage.value = "Unable to read backup snapshot"
            }
            _isBackupBusy.value = false
        }
    }

    fun confirmPendingRestore() {
        val pending = _pendingBackupPreview.value ?: return
        _pendingBackupPreview.value = null
        viewModelScope.launch {
            _isBackupBusy.value = true
            runCatching {
                repository.restoreBackupPayload(pending.payload)
            }.onSuccess { summary ->
                _statusMessage.value = "Restored ${summary.restoredPlaylists} playlists & ${summary.restoredSongStates} track states"
            }.onFailure {
                _statusMessage.value = "Restore failed"
            }
            _isBackupBusy.value = false
        }
    }

    fun dismissPendingRestore() {
        _pendingBackupPreview.value = null
    }

    fun deleteLocalBackup(info: LocalBackupFileInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { info.file.delete() }
            refreshLocalBackups()
            _statusMessage.value = "Deleted ${info.name}"
        }
    }

    fun setAutoBackupIntervalDays(days: Int) {
        viewModelScope.launch {
            repository.settingsDataStore.updateAutoBackupIntervalDays(days)
            if (days > 0) {
                val created = repository.runScheduledAutoBackupIfDue()
                if (created != null) {
                    refreshLocalBackups()
                }
            }
        }
    }

    // Equalizer & Sleep Timer delegates
    fun setEqEnabled(enabled: Boolean) = playbackController.setEqEnabled(enabled)
    fun applyEqPreset(preset: EqPreset) = playbackController.applyEqPreset(preset)
    fun updateEqBand(index: Int, levelMb: Int) = playbackController.updateEqBand(index, levelMb)
    fun updateBassBoost(strength: Int) = playbackController.updateBassBoost(strength)
    fun updateVirtualizer(strength: Int) = playbackController.updateVirtualizer(strength)
    fun resetEqualizer() = playbackController.resetEqualizer()

    fun shareSong(context: Context, song: Song) {
        runCatching {
            val shareUri: Uri = if (song.filePath.isNotBlank() && File(song.filePath).exists()) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    File(song.filePath)
                )
            } else {
                Uri.parse(song.contentUri)
            }
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = song.mimeType.ifBlank { "audio/*" }
                putExtra(Intent.EXTRA_STREAM, shareUri)
                putExtra(Intent.EXTRA_SUBJECT, song.title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share ${song.title}"))
        }.onFailure {
            _statusMessage.value = "Unable to share audio file"
        }
    }

    private fun sortSongs(
        songs: List<Song>,
        field: SortField,
        direction: SortDirection
    ): List<Song> {
        val comparator: Comparator<Song> = when (field) {
            SortField.TITLE -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            SortField.ARTIST -> compareBy<Song, String>(String.CASE_INSENSITIVE_ORDER) { it.artist }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            SortField.ALBUM -> compareBy<Song, String>(String.CASE_INSENSITIVE_ORDER) { it.album }
                .thenBy { it.trackNumber }
            SortField.DURATION -> compareBy { it.durationMs }
            SortField.DATE_ADDED -> compareBy { it.dateAddedSec }
            SortField.RECENTLY_PLAYED -> compareBy { it.lastPlayedAt }
            SortField.MOST_PLAYED -> compareBy { it.playCount }
        }
        val sorted = songs.sortedWith(comparator)
        return if (direction == SortDirection.DESCENDING) sorted.reversed() else sorted
    }

    private fun sortPlaylists(
        list: List<Playlist>,
        field: PlaylistSortField,
        direction: SortDirection
    ): List<Playlist> {
        val cmp: Comparator<Playlist> = when (field) {
            PlaylistSortField.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            PlaylistSortField.TRACK_COUNT -> compareBy<Playlist> { it.songCount }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            PlaylistSortField.CREATED_AT -> compareBy { it.createdAt }
            PlaylistSortField.LAST_PLAYED -> compareBy { it.lastPlayedAt }
            PlaylistSortField.DURATION -> compareBy { it.totalDurationMs }
        }
        val sorted = list.sortedWith(cmp)
        return if (direction == SortDirection.DESCENDING) sorted.reversed() else sorted
    }

    private fun sortFolders(
        list: List<FolderItem>,
        field: FolderSortField,
        direction: SortDirection
    ): List<FolderItem> {
        val cmp: Comparator<FolderItem> = when (field) {
            FolderSortField.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            FolderSortField.TRACK_COUNT -> compareBy<FolderItem> { it.songCount }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            FolderSortField.SUBFOLDER_COUNT -> compareBy<FolderItem> { it.subfolderCount }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            FolderSortField.SIZE -> compareBy<FolderItem> { it.totalSizeBytes }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
        }
        val sorted = list.sortedWith(cmp)
        return if (direction == SortDirection.DESCENDING) sorted.reversed() else sorted
    }

    private fun sortAlbums(
        list: List<Album>,
        field: AlbumSortField,
        direction: SortDirection
    ): List<Album> {
        val cmp: Comparator<Album> = when (field) {
            AlbumSortField.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            AlbumSortField.ARTIST -> compareBy<Album, String>(String.CASE_INSENSITIVE_ORDER) { it.artist }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            AlbumSortField.TRACK_COUNT -> compareBy<Album> { it.trackCount }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            AlbumSortField.YEAR -> compareBy<Album> { it.year }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
        }
        val sorted = list.sortedWith(cmp)
        return if (direction == SortDirection.DESCENDING) sorted.reversed() else sorted
    }

    private fun sortArtists(
        list: List<Artist>,
        field: ArtistSortField,
        direction: SortDirection
    ): List<Artist> {
        val cmp: Comparator<Artist> = when (field) {
            ArtistSortField.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            ArtistSortField.TRACK_COUNT -> compareBy<Artist> { it.songCount }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            ArtistSortField.ALBUM_COUNT -> compareBy<Artist> { it.albumCount }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
        }
        val sorted = list.sortedWith(cmp)
        return if (direction == SortDirection.DESCENDING) sorted.reversed() else sorted
    }

    private fun buildFolderBrowserState(
        songs: List<Song>,
        currentPath: String?,
        sortField: SortField,
        sortDir: SortDirection,
        folderSortField: FolderSortField,
        folderSortDir: SortDirection,
        enableHierarchy: Boolean = true,
        skipSingleSubfolder: Boolean = true
    ): FolderBrowserState {
        if (songs.isEmpty()) return FolderBrowserState()

        val grouped = songs.groupBy { it.folderPath }

        if (!enableHierarchy) {
            if (currentPath == null || !grouped.containsKey(currentPath)) {
                val flatFolders = grouped.map { (path, list) ->
                    FolderItem(
                        name = list.firstOrNull()?.folderName ?: path.substringAfterLast('/'),
                        path = path,
                        songCount = list.size,
                        subfolderCount = 0,
                        representativeSong = list.firstOrNull(),
                        totalDurationMs = list.sumOf { it.durationMs },
                        totalSizeBytes = list.sumOf { it.sizeBytes }
                    )
                }
                return FolderBrowserState(
                    currentPath = null,
                    breadcrumbs = listOf("All Folders" to null),
                    subfolders = sortFolders(flatFolders, folderSortField, folderSortDir),
                    songs = emptyList()
                )
            }
            val directSongs = sortSongs(grouped[currentPath].orEmpty(), sortField, sortDir)
            val folderName = grouped[currentPath]?.firstOrNull()?.folderName ?: currentPath.substringAfterLast('/')
            return FolderBrowserState(
                currentPath = currentPath,
                breadcrumbs = listOf("All Folders" to null, folderName to currentPath),
                subfolders = emptyList(),
                songs = directSongs
            )
        }

        fun immediateChildPathsFor(basePath: String?): List<String> {
            val prefix = if (basePath == null) "/" else "$basePath/"
            return grouped.keys
                .filter { path ->
                    if (basePath == null) true else path.startsWith(prefix) && path != basePath
                }
                .map { fullPath ->
                    val relative = if (basePath == null) fullPath.trimStart('/') else fullPath.removePrefix(prefix)
                    val childSegment = relative.substringBefore('/')
                    if (basePath == null) "/$childSegment" else "$basePath/$childSegment"
                }
                .distinct()
        }

        fun collapseSingleSubfolder(candidatePath: String): String {
            if (!skipSingleSubfolder) return candidatePath
            var cursor = candidatePath
            while (true) {
                val directCount = grouped[cursor]?.size ?: 0
                if (directCount > 0) break
                val children = immediateChildPathsFor(cursor)
                if (children.size != 1) break
                cursor = children.first()
            }
            return cursor
        }

        val effectiveCurrentPath = if (currentPath == null && skipSingleSubfolder) {
            // If everything sits inside one top root (e.g. /storage/emulated/0), skip straight past trivial roots
            val roots = immediateChildPathsFor(null)
            if (roots.size == 1 && grouped[roots.first()].isNullOrEmpty()) {
                collapseSingleSubfolder(roots.first())
            } else {
                null
            }
        } else {
            currentPath
        }

        val directSongs = if (effectiveCurrentPath != null) {
            sortSongs(grouped[effectiveCurrentPath].orEmpty(), sortField, sortDir)
        } else {
            emptyList()
        }

        val immediateSubfolders = immediateChildPathsFor(effectiveCurrentPath)
            .map { rawChild ->
                val resolvedChild = collapseSingleSubfolder(rawChild)
                val matchingSongs = grouped.entries
                    .filter { it.key == resolvedChild || it.key.startsWith("$resolvedChild/") }
                    .flatMap { it.value }
                val label = if (effectiveCurrentPath != null) {
                    resolvedChild.removePrefix("$effectiveCurrentPath/").ifBlank { resolvedChild.substringAfterLast('/') }
                } else {
                    resolvedChild.trimStart('/').ifBlank { resolvedChild }
                }
                FolderItem(
                    name = label,
                    path = resolvedChild,
                    songCount = matchingSongs.size,
                    subfolderCount = grouped.keys.count { it != resolvedChild && it.startsWith("$resolvedChild/") },
                    representativeSong = matchingSongs.firstOrNull(),
                    totalDurationMs = matchingSongs.sumOf { it.durationMs },
                    totalSizeBytes = matchingSongs.sumOf { it.sizeBytes }
                )
            }

        val breadcrumbs = buildList {
            add("All Folders" to null)
            if (effectiveCurrentPath != null) {
                val segments = effectiveCurrentPath.trim('/').split('/').filter { it.isNotBlank() }
                var acc = ""
                for (seg in segments) {
                    acc += "/$seg"
                    val hasDirectOrBranch = !grouped[acc].isNullOrEmpty() || !skipSingleSubfolder || immediateChildPathsFor(acc).size > 1
                    if (hasDirectOrBranch || acc == effectiveCurrentPath) {
                        add(seg to acc)
                    }
                }
            }
        }

        return FolderBrowserState(
            currentPath = effectiveCurrentPath,
            breadcrumbs = breadcrumbs,
            subfolders = sortFolders(immediateSubfolders, folderSortField, folderSortDir),
            songs = directSongs
        )
    }

    private fun computeLibraryStats(
        songs: List<Song>,
        playlists: List<Playlist>,
        trashedCount: Int,
        hiddenCount: Int,
        totalPlayEvents: Int
    ): LibraryStats {
        if (songs.isEmpty()) {
            return LibraryStats(
                playlistsCount = playlists.size,
                trashedCount = trashedCount,
                hiddenCount = hiddenCount,
                totalPlayEvents = totalPlayEvents
            )
        }
        val totalDuration = songs.sumOf { it.durationMs }
        val totalBytes = songs.sumOf { it.sizeBytes }
        val favoritesCount = songs.count { it.isFavorite }
        val albumsCount = songs.distinctBy { it.albumId to it.album }.size
        val artistsCount = songs.distinctBy { it.artistId to it.artist }.size
        val foldersCount = songs.distinctBy { it.folderPath }.size
        val lyricsCount = songs.count { it.hasLyrics }
        val songsPlayedCount = songs.count { it.playCount > 0 }
        val totalPlayCountSum = songs.sumOf { it.playCount }
        val estimatedListeningMs = songs.sumOf { it.playCount.toLong() * it.durationMs }
        val avgDuration = totalDuration / songs.size.coerceAtLeast(1)

        val genreGroups = songs.groupBy { it.genre.trim().ifBlank { "Untagged" } }
        val topGenres = genreGroups
            .filterKeys { it != "Untagged" }
            .ifEmpty { genreGroups }
            .map { (name, list) ->
                GenreItem(
                    name = name,
                    songCount = list.size,
                    totalDurationMs = list.sumOf { it.durationMs },
                    representativeSong = list.firstOrNull()
                )
            }
            .sortedByDescending { it.songCount }
            .take(6)

        val topPlayedSongs = songs
            .filter { it.playCount > 0 }
            .sortedWith(compareByDescending<Song> { it.playCount }.thenByDescending { it.lastPlayedAt })
            .take(8)

        val topArtists = songs
            .groupBy { it.artist }
            .map { (artist, list) ->
                ArtistStat(
                    artist = artist,
                    trackCount = list.size,
                    playCount = list.sumOf { it.playCount }
                )
            }
            .sortedWith(compareByDescending<ArtistStat> { it.trackCount }.thenByDescending { it.playCount })
            .take(6)

        val formatBreakdown = songs
            .groupBy { s ->
                val ext = s.filePath.substringAfterLast('.', "").uppercase()
                if (ext.isNotBlank() && ext.length <= 5) ext
                else s.mimeType.substringAfterLast('/', "AUDIO").uppercase().replace("MPEG", "MP3")
            }
            .map { (fmt, list) ->
                FormatStat(
                    format = fmt,
                    trackCount = list.size,
                    sizeBytes = list.sumOf { it.sizeBytes }
                )
            }
            .sortedByDescending { it.trackCount }

        return LibraryStats(
            totalSongs = songs.size,
            totalDurationMs = totalDuration,
            totalSizeBytes = totalBytes,
            favoritesCount = favoritesCount,
            playlistsCount = playlists.size,
            albumsCount = albumsCount,
            artistsCount = artistsCount,
            foldersCount = foldersCount,
            lyricsCount = lyricsCount,
            trashedCount = trashedCount,
            hiddenCount = hiddenCount,
            totalPlayEvents = maxOf(totalPlayEvents, totalPlayCountSum),
            songsPlayedCount = songsPlayedCount,
            averageDurationMs = avgDuration,
            genresCount = genreGroups.keys.count { it != "Untagged" }.coerceAtLeast(if (genreGroups.isNotEmpty()) 1 else 0),
            totalPlayCountSum = totalPlayCountSum,
            estimatedListeningMs = estimatedListeningMs,
            topPlayedSongs = topPlayedSongs,
            topArtists = topArtists,
            formatBreakdown = formatBreakdown,
            topGenres = topGenres
        )
    }
}
