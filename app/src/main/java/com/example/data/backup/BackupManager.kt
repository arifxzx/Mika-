package com.example.data.backup

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import com.example.BuildConfig
import com.example.data.db.MikaDao
import com.example.data.db.PlayHistoryEntity
import com.example.data.db.PlaylistEntity
import com.example.data.db.PlaylistSongCrossRef
import com.example.data.db.SearchHistoryEntity
import com.example.data.prefs.SettingsDataStore
import com.example.media.LyricsParser
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
import com.example.model.SeekBarStyle
import com.example.model.SortDirection
import com.example.model.SortField
import com.example.model.SoundControlSettings
import com.example.model.TabLongPressAction
import com.example.model.TabTapAction
import com.example.model.ThemeMode
import com.example.model.TrackSwipeAction
import com.example.model.WidgetConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@Serializable
data class BackupSettingsDto(
    val themeMode: String = ThemeMode.DARK.name,
    val accentColorId: String = "electric_blue",
    val customAccentArgb: Long = 0L,
    val autoColoring: Boolean = false,
    val applyColorOnApp: Boolean = true,
    val applyColorOnPlayer: Boolean = true,
    val gradientStyle: String = GradientStyle.SUBTLE_LINEAR.name,
    val applyGradientToMiniPlayer: Boolean = true,
    val applyGradientToFullPlayer: Boolean = true,
    val applyGradientToSurfaces: Boolean = true,
    val sortField: String = SortField.TITLE.name,
    val sortDirection: String = SortDirection.ASCENDING.name,
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
    val seekBarStyle: String = SeekBarStyle.SLEEK_CAPSULE.name,
    val showActualWaveformSeekbar: Boolean = false,
    val showLiveWaveformSeekbar: Boolean = false,
    val playbackIconStyle: String = PlaybackIconStyle.NORMAL.name,
    val enabledTabs: List<String> = listOf(
        MainTab.SONGS.name,
        MainTab.PLAYLISTS.name,
        MainTab.ALBUMS.name,
        MainTab.ARTISTS.name
    ),
    val defaultTab: String = MainTab.SONGS.name,
    val tabTapAction: String = TabTapAction.OPEN_TAB.name,
    val tabLongPressAction: String = TabLongPressAction.CUSTOMIZE_TABS.name,
    val excludedFolders: List<String> = emptyList(),
    val minDurationSec: Int = 0,
    val minFileSizeKb: Int = 0,
    val obeyNoMedia: Boolean = true,
    val autoScanOnStartup: Boolean = true,
    val extensionBlacklist: List<String> = emptyList(),
    val groupArtworkByAlbum: Boolean = false,
    val showActualArtworkRatio: Boolean = false,
    val showLikeButtonInList: Boolean = false,
    val swipeLeftAction: String = TrackSwipeAction.ADD_TO_QUEUE.name,
    val swipeRightAction: String = TrackSwipeAction.FAVORITE.name,
    val swipeToOpenDrawer: Boolean = true,
    val playlistSortField: String = PlaylistSortField.CREATED_AT.name,
    val playlistSortDirection: String = SortDirection.DESCENDING.name,
    val folderListSortField: String = FolderSortField.NAME.name,
    val folderListSortDirection: String = SortDirection.ASCENDING.name,
    val albumSortField: String = AlbumSortField.NAME.name,
    val albumSortDirection: String = SortDirection.ASCENDING.name,
    val artistSortField: String = ArtistSortField.NAME.name,
    val artistSortDirection: String = SortDirection.ASCENDING.name,
    val showBottomNavBar: Boolean = true,
    val bottomNavOrder: List<String> = BottomNavDest.entries.map { it.name },
    val hiddenBottomNavItems: List<String> = emptyList(),
    val allowSimultaneousPlayback: Boolean = false,
    val autoScanAudio: Boolean = true,
    val autoScanVideo: Boolean = true,
    val videoGridView: Boolean = false,
    val autoBackupIntervalDays: Int = 0,
    val enableFoldersHierarchy: Boolean = true,
    val skipSingleSubfolder: Boolean = true,
    val skipSilence: Boolean = false,
    val crossfadeEnabled: Boolean = false,
    val crossfadeDurationMs: Int = 500,
    val crossfadeTriggerLastSec: Int = 5,
    val crossfadeStyle: String = "DEFAULT",
    val playPauseFadeEnabled: Boolean = false,
    val playFadeDurationMs: Int = 500,
    val pauseFadeDurationMs: Int = 500,
    val jumpToFirstAfterQueue: Boolean = true,
    val previousButtonReplays: Boolean = true,
    val restorePositionMinDurationMin: Int = 10,
    val enableBlurEffect: Boolean = false,
    val enableGlowEffect: Boolean = true,
    val enableParallaxEffect: Boolean = false,
    val backgroundEffect: String = "NONE",
    val playerBackgroundMode: String = "MESH_GRADIENT",
    val playerBackgroundVignette: Boolean = true,
    val playerBackgroundAnimated: Boolean = true,
    val playerBackgroundColorWhenExpanded: Boolean = true,
    val pickColorsFromWallpaper: Boolean = false,
    val forceMiniPlayerTrackColors: Boolean = false,
    val usePitchBlack: Boolean = false,
    val customDarkAccentArgb: Long = 0L,
    val doubleTapToToggleLyrics: Boolean = true,
    val artworkTapAction: String = "ARTWORK_OPEN",
    val artworkLongPressAction: String = "INFO",
    val appIconPreset: String = AppIconPreset.AURORA_GLASS.name,
    val widgetConfig: WidgetConfig = WidgetConfig(),
    val miniPlayerConfig: MiniPlayerConfig = MiniPlayerConfig(),
    val fullPlayerConfig: FullPlayerConfig = FullPlayerConfig(),
    val soundControl: SoundControlSettings = SoundControlSettings()
)

@Serializable
data class BackupEqualizerDto(
    val enabled: Boolean = false,
    val preset: String = EqPreset.NORMAL.name,
    val bandLevelsMb: List<Int> = listOf(0, 0, 0, 0, 0),
    val bassBoostStrength: Int = 0,
    val virtualizerStrength: Int = 0
)

@Serializable
data class BackupSongUserStateDto(
    val songId: Long,
    val filePath: String = "",
    val title: String = "",
    val artist: String = "",
    val playCount: Int = 0,
    val lastPlayedAt: Long = 0L,
    val isFavorite: Boolean = false,
    val hasLyrics: Boolean = false,
    val isHidden: Boolean = false,
    val mood: String = "",
    val rating: Int = 0
)

@Serializable
data class BackupPlaylistTrackRefDto(
    val songId: Long,
    val filePath: String = "",
    val title: String = ""
)

@Serializable
data class BackupPlaylistDto(
    val name: String,
    val createdAt: Long = 0L,
    val lastPlayedAt: Long = 0L,
    val tracks: List<BackupPlaylistTrackRefDto> = emptyList()
)

@Serializable
data class BackupPlayHistoryEntryDto(
    val songId: Long,
    val filePath: String = "",
    val title: String = "",
    val playedAt: Long
)

@Serializable
data class BackupSearchEntryDto(
    val query: String,
    val searchedAt: Long
)

@Serializable
data class BackupMetadataDto(
    val schemaVersion: Int = 2,
    val appName: String = "Mika Player",
    val appVersion: String = BuildConfig.VERSION_NAME,
    val createdAtMs: Long = System.currentTimeMillis()
)

@Serializable
data class MikaBackupPayload(
    val schemaVersion: Int = 2,
    val appName: String = "Mika Player",
    val appVersion: String = BuildConfig.VERSION_NAME,
    val createdAtMs: Long = System.currentTimeMillis(),
    val settings: BackupSettingsDto = BackupSettingsDto(),
    val equalizer: BackupEqualizerDto = BackupEqualizerDto(),
    val songUserStates: List<BackupSongUserStateDto> = emptyList(),
    val playlists: List<BackupPlaylistDto> = emptyList(),
    val playHistory: List<BackupPlayHistoryEntryDto> = emptyList(),
    val searchHistory: List<BackupSearchEntryDto> = emptyList(),
    val customLyrics: Map<Long, String> = emptyMap()
)

data class LocalBackupFileInfo(
    val file: File,
    val name: String,
    val createdAtMs: Long,
    val sizeBytes: Long,
    val trackStatsCount: Int,
    val playlistsCount: Int,
    val favoritesCount: Int
)

data class BackupRestoreSummary(
    val createdAtMs: Long,
    val restoredPlaylists: Int,
    val restoredSongStates: Int,
    val restoredHistoryEntries: Int,
    val restoredSearchEntries: Int
)

object BackupManager {

    private const val MIKA_ROOT_FOLDER = "mika"
    private const val BACKUP_SUBFOLDER = "backup"
    private const val LEGACY_MIKA_FOLDER = "Mika"
    private const val LEGACY_BACKUP_DIR = "backups"
    private const val MAX_LOCAL_BACKUPS = 8

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Primary backup directory at /mika/backup inside storage (or Documents/mika/backup / internal fallback).
     */
    fun getBackupDirectory(context: Context): File {
        val internalMikaBackup = File(File(context.filesDir, MIKA_ROOT_FOLDER), BACKUP_SUBFOLDER).apply {
            if (!exists()) mkdirs()
        }
        val externalRootMikaBackup = runCatching {
            @Suppress("DEPRECATION")
            val extRoot = Environment.getExternalStorageDirectory()
            File(File(extRoot, MIKA_ROOT_FOLDER), BACKUP_SUBFOLDER).also { dir ->
                if (!dir.exists()) dir.mkdirs()
            }
        }.getOrNull()
        if (externalRootMikaBackup != null && externalRootMikaBackup.exists() && externalRootMikaBackup.canWrite()) {
            return externalRootMikaBackup
        }
        val docsMikaBackup = runCatching {
            val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            File(File(docsDir, MIKA_ROOT_FOLDER), BACKUP_SUBFOLDER).also { dir ->
                if (!dir.exists()) dir.mkdirs()
            }
        }.getOrNull()

        return if (docsMikaBackup != null && docsMikaBackup.exists() && docsMikaBackup.canWrite()) {
            docsMikaBackup
        } else {
            internalMikaBackup
        }
    }

    fun getAllBackupDirectories(context: Context): List<File> {
        val dirs = mutableListOf<File>()
        runCatching {
            @Suppress("DEPRECATION")
            val extRoot = Environment.getExternalStorageDirectory()
            File(File(extRoot, MIKA_ROOT_FOLDER), BACKUP_SUBFOLDER)
                .takeIf { it.exists() && it.isDirectory }?.let { dirs.add(it) }
        }
        runCatching {
            val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            File(File(docsDir, MIKA_ROOT_FOLDER), BACKUP_SUBFOLDER)
                .takeIf { it.exists() && it.isDirectory }?.let { dirs.add(it) }
            File(docsDir, LEGACY_MIKA_FOLDER)
                .takeIf { it.exists() && it.isDirectory }?.let { dirs.add(it) }
        }
        File(File(context.filesDir, MIKA_ROOT_FOLDER), BACKUP_SUBFOLDER)
            .takeIf { it.exists() && it.isDirectory }?.let { dirs.add(it) }
        File(context.filesDir, LEGACY_MIKA_FOLDER).takeIf { it.exists() && it.isDirectory }?.let { dirs.add(it) }
        File(context.filesDir, LEGACY_BACKUP_DIR).takeIf { it.exists() && it.isDirectory }?.let { dirs.add(it) }
        return dirs.distinctBy { it.absolutePath }
    }

    suspend fun buildPayload(
        context: Context? = null,
        dao: MikaDao,
        settingsDataStore: SettingsDataStore
    ): MikaBackupPayload = withContext(Dispatchers.IO) {
        val settings = settingsDataStore.settingsFlow.first()
        val equalizer = settingsDataStore.equalizerFlow.first()
        val songs = dao.getAllSongsOnce()
        val songsById = songs.associateBy { it.id }

        val userStates = songs
            .filter {
                it.isFavorite || it.playCount > 0 || it.lastPlayedAt > 0L ||
                    it.hasLyrics || it.isHidden || it.mood.isNotBlank() || it.rating > 0
            }
            .map { s ->
                BackupSongUserStateDto(
                    songId = s.id,
                    filePath = s.filePath,
                    title = s.title,
                    artist = s.artist,
                    playCount = s.playCount,
                    lastPlayedAt = s.lastPlayedAt,
                    isFavorite = s.isFavorite,
                    hasLyrics = s.hasLyrics,
                    isHidden = s.isHidden,
                    mood = s.mood,
                    rating = s.rating
                )
            }

        val playlists = dao.getPlaylistsOnce()
        val refsByPlaylist = dao.getAllPlaylistSongRefsOnce().groupBy { it.playlistId }
        val playlistDtos = playlists.map { pl ->
            val trackDtos = refsByPlaylist[pl.id]
                .orEmpty()
                .sortedBy { it.sortOrder }
                .mapNotNull { ref ->
                    val s = songsById[ref.songId]
                    BackupPlaylistTrackRefDto(
                        songId = ref.songId,
                        filePath = s?.filePath ?: "",
                        title = s?.title ?: ""
                    )
                }
            BackupPlaylistDto(
                name = pl.name,
                createdAt = pl.createdAt,
                lastPlayedAt = pl.lastPlayedAt,
                tracks = trackDtos
            )
        }

        val historyDtos = dao.getAllPlayHistoryOnce().take(500).map { h ->
            val s = songsById[h.songId]
            BackupPlayHistoryEntryDto(
                songId = h.songId,
                filePath = s?.filePath ?: "",
                title = s?.title ?: "",
                playedAt = h.playedAt
            )
        }

        val searchDtos = dao.getSearchHistoryOnce().map { sh ->
            BackupSearchEntryDto(query = sh.query, searchedAt = sh.searchedAt)
        }

        val customLyricsMap = if (context != null) {
            val dir = File(context.filesDir, "custom_lyrics")
            dir.listFiles { f -> f.isFile && f.extension.equals("lrc", ignoreCase = true) }
                ?.mapNotNull { file ->
                    val id = file.nameWithoutExtension.toLongOrNull() ?: return@mapNotNull null
                    id to file.readText(Charsets.UTF_8)
                }?.toMap().orEmpty()
        } else emptyMap()

        MikaBackupPayload(
            createdAtMs = System.currentTimeMillis(),
            settings = settings.toDto(),
            equalizer = BackupEqualizerDto(
                enabled = equalizer.enabled,
                preset = equalizer.preset.name,
                bandLevelsMb = equalizer.bandLevelsMb,
                bassBoostStrength = equalizer.bassBoostStrength,
                virtualizerStrength = equalizer.virtualizerStrength
            ),
            songUserStates = userStates,
            playlists = playlistDtos,
            playHistory = historyDtos,
            searchHistory = searchDtos,
            customLyrics = customLyricsMap
        )
    }

    private fun encodePayloadToZipBytes(payload: MikaBackupPayload): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            fun putEntry(name: String, content: String) {
                zos.putNextEntry(ZipEntry(name))
                zos.write(content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
            val meta = BackupMetadataDto(
                schemaVersion = payload.schemaVersion,
                appName = payload.appName,
                appVersion = payload.appVersion,
                createdAtMs = payload.createdAtMs
            )
            putEntry("metadata.json", json.encodeToString(meta))
            putEntry("settings.json", json.encodeToString(payload.settings))
            putEntry("equalizer.json", json.encodeToString(payload.equalizer))
            putEntry("playlists.json", json.encodeToString(payload.playlists))
            putEntry("song_states.json", json.encodeToString(payload.songUserStates))
            putEntry("play_history.json", json.encodeToString(payload.playHistory))
            putEntry("search_history.json", json.encodeToString(payload.searchHistory))
            putEntry("custom_lyrics.json", json.encodeToString(payload.customLyrics))
        }
        return baos.toByteArray()
    }

    private fun decodePayloadFromBytes(bytes: ByteArray): MikaBackupPayload {
        // Detect ZIP archive header ('P','K')
        if (bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()) {
            val entries = HashMap<String, String>()
            ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        entries[entry.name] = zis.readBytes().toString(Charsets.UTF_8)
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            val meta = entries["metadata.json"]?.let {
                runCatching { json.decodeFromString<BackupMetadataDto>(it) }.getOrNull()
            } ?: BackupMetadataDto()
            val settings = entries["settings.json"]?.let {
                runCatching { json.decodeFromString<BackupSettingsDto>(it) }.getOrNull()
            } ?: BackupSettingsDto()
            val eq = entries["equalizer.json"]?.let {
                runCatching { json.decodeFromString<BackupEqualizerDto>(it) }.getOrNull()
            } ?: BackupEqualizerDto()
            val states = entries["song_states.json"]?.let {
                runCatching { json.decodeFromString<List<BackupSongUserStateDto>>(it) }.getOrNull()
            } ?: emptyList()
            val playlists = entries["playlists.json"]?.let {
                runCatching { json.decodeFromString<List<BackupPlaylistDto>>(it) }.getOrNull()
            } ?: emptyList()
            val history = entries["play_history.json"]?.let {
                runCatching { json.decodeFromString<List<BackupPlayHistoryEntryDto>>(it) }.getOrNull()
            } ?: emptyList()
            val search = entries["search_history.json"]?.let {
                runCatching { json.decodeFromString<List<BackupSearchEntryDto>>(it) }.getOrNull()
            } ?: emptyList()
            val lyrics = entries["custom_lyrics.json"]?.let {
                runCatching { json.decodeFromString<Map<Long, String>>(it) }.getOrNull()
            } ?: emptyMap()

            return MikaBackupPayload(
                schemaVersion = meta.schemaVersion,
                appName = meta.appName,
                appVersion = meta.appVersion,
                createdAtMs = meta.createdAtMs,
                settings = settings,
                equalizer = eq,
                songUserStates = states,
                playlists = playlists,
                playHistory = history,
                searchHistory = search,
                customLyrics = lyrics
            )
        }
        // Fallback: legacy single JSON backup file
        return json.decodeFromString<MikaBackupPayload>(bytes.toString(Charsets.UTF_8))
    }

    suspend fun createInternalBackup(
        context: Context,
        dao: MikaDao,
        settingsDataStore: SettingsDataStore,
        prefix: String = "mika_backup"
    ): LocalBackupFileInfo = withContext(Dispatchers.IO) {
        val payload = buildPayload(context, dao, settingsDataStore)
        val zipBytes = encodePayloadToZipBytes(payload)
        val dir = getBackupDirectory(context)
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(payload.createdAtMs))
        val fileName = "${prefix}_$stamp.zip"
        val target = File(dir, fileName)
        FileOutputStream(target).use { out ->
            out.write(zipBytes)
            out.flush()
        }

        // Mirror copy into internal /mika/backup
        val internalMika = File(File(context.filesDir, MIKA_ROOT_FOLDER), BACKUP_SUBFOLDER).apply { if (!exists()) mkdirs() }
        if (internalMika.absolutePath != dir.absolutePath) {
            runCatching {
                File(internalMika, fileName).writeBytes(zipBytes)
                pruneOldBackups(internalMika)
            }
        }

        val settings = settingsDataStore.settingsFlow.first()
        if (settings.backupLocationUri.isNotBlank()) {
            mirrorBytesToTreeUri(context, settings.backupLocationUri, fileName, "application/zip", zipBytes)
        }

        settingsDataStore.updateLastAutoBackupAtMs(payload.createdAtMs)
        pruneOldBackups(dir)

        LocalBackupFileInfo(
            file = target,
            name = target.name,
            createdAtMs = payload.createdAtMs,
            sizeBytes = target.length(),
            trackStatsCount = payload.songUserStates.size,
            playlistsCount = payload.playlists.size,
            favoritesCount = payload.songUserStates.count { it.isFavorite }
        )
    }

    private fun mirrorBytesToTreeUri(
        context: Context,
        treeUriString: String,
        fileName: String,
        mimeType: String,
        content: ByteArray
    ): Boolean {
        return runCatching {
            val treeUri = Uri.parse(treeUriString)
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
            val createdUri = DocumentsContract.createDocument(
                context.contentResolver,
                parentDocUri,
                mimeType,
                fileName
            ) ?: return false
            context.contentResolver.openOutputStream(createdUri)?.use { out ->
                out.write(content)
                out.flush()
            }
            true
        }.getOrDefault(false)
    }

    suspend fun exportToUri(
        context: Context,
        uri: Uri,
        dao: MikaDao,
        settingsDataStore: SettingsDataStore
    ): MikaBackupPayload = withContext(Dispatchers.IO) {
        val payload = buildPayload(context, dao, settingsDataStore)
        val zipBytes = encodePayloadToZipBytes(payload)
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(zipBytes)
            stream.flush()
        } ?: error("Cannot write to destination file")
        settingsDataStore.updateLastAutoBackupAtMs(payload.createdAtMs)
        payload
    }

    suspend fun parseFromUri(context: Context, uri: Uri): MikaBackupPayload = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.readBytes()
        } ?: error("Cannot read selected backup file")
        decodePayloadFromBytes(bytes)
    }

    suspend fun parseFromFile(file: File): MikaBackupPayload = withContext(Dispatchers.IO) {
        val bytes = FileInputStream(file).use { it.readBytes() }
        decodePayloadFromBytes(bytes)
    }

    suspend fun listLocalBackups(context: Context): List<LocalBackupFileInfo> = withContext(Dispatchers.IO) {
        val seenNames = HashSet<String>()
        val files = getAllBackupDirectories(context)
            .flatMap { dir ->
                dir.listFiles { f ->
                    f.isFile && (f.extension.equals("zip", ignoreCase = true) || f.extension.equals("json", ignoreCase = true))
                }?.toList().orEmpty()
            }
            .sortedByDescending { it.lastModified() }
            .filter { seenNames.add(it.name) }

        files.mapNotNull { file ->
            runCatching {
                val payload = parseFromFile(file)
                LocalBackupFileInfo(
                    file = file,
                    name = file.name,
                    createdAtMs = payload.createdAtMs,
                    sizeBytes = file.length(),
                    trackStatsCount = payload.songUserStates.size,
                    playlistsCount = payload.playlists.size,
                    favoritesCount = payload.songUserStates.count { it.isFavorite }
                )
            }.getOrNull()
        }
    }

    suspend fun restorePayload(
        context: Context? = null,
        payload: MikaBackupPayload,
        dao: MikaDao,
        settingsDataStore: SettingsDataStore
    ): BackupRestoreSummary = withContext(Dispatchers.IO) {
        settingsDataStore.restoreSettings(payload.settings.toDomain())
        settingsDataStore.saveEqualizerState(
            EqualizerState(
                enabled = payload.equalizer.enabled,
                preset = runCatching { EqPreset.valueOf(payload.equalizer.preset) }.getOrDefault(EqPreset.NORMAL),
                bandLevelsMb = payload.equalizer.bandLevelsMb.ifEmpty { listOf(0, 0, 0, 0, 0) },
                bassBoostStrength = payload.equalizer.bassBoostStrength,
                virtualizerStrength = payload.equalizer.virtualizerStrength
            )
        )

        val existingSongs = dao.getAllSongsOnce()
        val byId = existingSongs.associateBy { it.id }
        val byPath = existingSongs.filter { it.filePath.isNotBlank() }.associateBy { it.filePath }
        val byTitleArtist = existingSongs.associateBy {
            "${it.title.trim().lowercase()}|${it.artist.trim().lowercase()}"
        }
        val byTitle = existingSongs.associateBy { it.title.trim().lowercase() }

        fun resolveSongId(songId: Long, filePath: String, title: String, artist: String = ""): Long? {
            if (byId.containsKey(songId)) return songId
            if (filePath.isNotBlank()) byPath[filePath]?.let { return it.id }
            if (title.isNotBlank() && artist.isNotBlank()) {
                byTitleArtist["${title.trim().lowercase()}|${artist.trim().lowercase()}"]?.let { return it.id }
            }
            if (title.isNotBlank()) byTitle[title.trim().lowercase()]?.let { return it.id }
            return null
        }

        var restoredStates = 0
        for (state in payload.songUserStates) {
            val resolvedId = resolveSongId(state.songId, state.filePath, state.title, state.artist) ?: continue
            dao.restoreSongUserState(
                songId = resolvedId,
                playCount = state.playCount,
                lastPlayedAt = state.lastPlayedAt,
                isFavorite = state.isFavorite,
                hasLyrics = state.hasLyrics,
                isHidden = state.isHidden,
                mood = state.mood,
                rating = state.rating
            )
            restoredStates++
        }

        if (context != null && payload.customLyrics.isNotEmpty()) {
            payload.customLyrics.forEach { (id, rawLyrics) ->
                if (byId.containsKey(id)) {
                    LyricsParser.saveCustomLyrics(context, id, rawLyrics)
                }
            }
        }

        val existingPlaylistsByName = dao.getPlaylistsOnce().associateBy { it.name.trim().lowercase() }
        var restoredPlaylists = 0
        for (plDto in payload.playlists) {
            val resolvedIds = plDto.tracks.mapNotNull { t ->
                resolveSongId(t.songId, t.filePath, t.title)
            }.distinct()

            val existingPl = existingPlaylistsByName[plDto.name.trim().lowercase()]
            val playlistId = existingPl?.id ?: dao.insertPlaylist(
                PlaylistEntity(
                    name = plDto.name,
                    createdAt = if (plDto.createdAt > 0L) plDto.createdAt else System.currentTimeMillis(),
                    lastPlayedAt = plDto.lastPlayedAt
                )
            )
            if (resolvedIds.isNotEmpty()) {
                val existingRefs = dao.getPlaylistSongRefsOnce(playlistId).map { it.songId }.toSet()
                var order = (dao.getMaxSortOrder(playlistId)) + 1
                val newRefs = resolvedIds.filter { it !in existingRefs }.map { songId ->
                    PlaylistSongCrossRef(playlistId = playlistId, songId = songId, sortOrder = order++)
                }
                if (newRefs.isNotEmpty()) {
                    dao.insertPlaylistSongs(newRefs)
                }
            }
            restoredPlaylists++
        }

        val resolvedHistory = payload.playHistory.mapNotNull { entry ->
            val resolvedId = resolveSongId(entry.songId, entry.filePath, entry.title) ?: return@mapNotNull null
            PlayHistoryEntity(songId = resolvedId, playedAt = entry.playedAt)
        }
        if (resolvedHistory.isNotEmpty()) {
            dao.clearPlayHistory()
            dao.insertPlayHistoryList(resolvedHistory)
        }

        val searchEntries = payload.searchHistory.map { SearchHistoryEntity(it.query, it.searchedAt) }
        if (searchEntries.isNotEmpty()) {
            dao.upsertSearchHistoryList(searchEntries)
        }

        BackupRestoreSummary(
            createdAtMs = payload.createdAtMs,
            restoredPlaylists = restoredPlaylists,
            restoredSongStates = restoredStates,
            restoredHistoryEntries = resolvedHistory.size,
            restoredSearchEntries = searchEntries.size
        )
    }

    // Overload kept for tests calling restorePayload(payload, dao, settingsDataStore) directly
    suspend fun restorePayload(
        payload: MikaBackupPayload,
        dao: MikaDao,
        settingsDataStore: SettingsDataStore
    ): BackupRestoreSummary = restorePayload(null, payload, dao, settingsDataStore)

    suspend fun runScheduledAutoBackupIfDue(
        context: Context,
        dao: MikaDao,
        settingsDataStore: SettingsDataStore
    ): LocalBackupFileInfo? {
        val settings = settingsDataStore.settingsFlow.first()
        val intervalDays = settings.autoBackupIntervalDays
        if (intervalDays <= 0) return null
        val intervalMs = intervalDays * 86_400_000L
        val now = System.currentTimeMillis()
        if (settings.lastAutoBackupAtMs > 0L && (now - settings.lastAutoBackupAtMs) < intervalMs) {
            return null
        }
        return runCatching {
            createInternalBackup(context, dao, settingsDataStore, prefix = "mika_auto")
        }.getOrNull()
    }

    private fun pruneOldBackups(dir: File) {
        val backups = dir.listFiles { f ->
            f.isFile && (f.extension.equals("zip", ignoreCase = true) || f.extension.equals("json", ignoreCase = true))
        }?.sortedByDescending { it.lastModified() } ?: return
        if (backups.size > MAX_LOCAL_BACKUPS) {
            backups.drop(MAX_LOCAL_BACKUPS).forEach { runCatching { it.delete() } }
        }
    }

    private fun AppSettings.toDto(): BackupSettingsDto = BackupSettingsDto(
        themeMode = themeMode.name,
        accentColorId = accentColorId,
        customAccentArgb = customAccentArgb,
        autoColoring = autoColoring,
        applyColorOnApp = applyColorOnApp,
        applyColorOnPlayer = applyColorOnPlayer,
        gradientStyle = gradientStyle.name,
        applyGradientToMiniPlayer = applyGradientToMiniPlayer,
        applyGradientToFullPlayer = applyGradientToFullPlayer,
        applyGradientToSurfaces = applyGradientToSurfaces,
        sortField = sortField.name,
        sortDirection = sortDirection.name,
        gaplessPlayback = gaplessPlayback,
        crossfadeDurationSec = crossfadeDurationSec,
        playbackSpeed = playbackSpeed,
        playbackPitch = playbackPitch,
        skipIntervalSec = skipIntervalSec,
        rememberLastPosition = rememberLastPosition,
        showLyricsBadges = showLyricsBadges,
        fontScale = fontScale,
        appIconScale = appIconScale,
        cornerRadiusDp = cornerRadiusDp,
        seekBarStyle = seekBarStyle.name,
        showActualWaveformSeekbar = showActualWaveformSeekbar,
        showLiveWaveformSeekbar = showLiveWaveformSeekbar,
        playbackIconStyle = playbackIconStyle.name,
        enabledTabs = enabledTabs.map { it.name },
        defaultTab = defaultTab.name,
        tabTapAction = tabTapAction.name,
        tabLongPressAction = tabLongPressAction.name,
        excludedFolders = excludedFolders.toList(),
        minDurationSec = minDurationSec,
        minFileSizeKb = minFileSizeKb,
        obeyNoMedia = obeyNoMedia,
        autoScanOnStartup = autoScanOnStartup,
        extensionBlacklist = extensionBlacklist.toList(),
        groupArtworkByAlbum = groupArtworkByAlbum,
        showActualArtworkRatio = showActualArtworkRatio,
        showLikeButtonInList = showLikeButtonInList,
        swipeLeftAction = swipeLeftAction.name,
        swipeRightAction = swipeRightAction.name,
        swipeToOpenDrawer = swipeToOpenDrawer,
        playlistSortField = playlistSortField.name,
        playlistSortDirection = playlistSortDirection.name,
        folderListSortField = folderListSortField.name,
        folderListSortDirection = folderListSortDirection.name,
        albumSortField = albumSortField.name,
        albumSortDirection = albumSortDirection.name,
        artistSortField = artistSortField.name,
        artistSortDirection = artistSortDirection.name,
        showBottomNavBar = showBottomNavBar,
        bottomNavOrder = bottomNavOrder.map { it.name },
        hiddenBottomNavItems = hiddenBottomNavItems.map { it.name },
        allowSimultaneousPlayback = allowSimultaneousPlayback,
        autoScanAudio = autoScanAudio,
        autoScanVideo = autoScanVideo,
        videoGridView = videoGridView,
        autoBackupIntervalDays = autoBackupIntervalDays,
        enableFoldersHierarchy = enableFoldersHierarchy,
        skipSingleSubfolder = skipSingleSubfolder,
        skipSilence = skipSilence,
        crossfadeEnabled = crossfadeEnabled,
        crossfadeDurationMs = crossfadeDurationMs,
        crossfadeTriggerLastSec = crossfadeTriggerLastSec,
        crossfadeStyle = crossfadeStyle.name,
        playPauseFadeEnabled = playPauseFadeEnabled,
        playFadeDurationMs = playFadeDurationMs,
        pauseFadeDurationMs = pauseFadeDurationMs,
        jumpToFirstAfterQueue = jumpToFirstAfterQueue,
        previousButtonReplays = previousButtonReplays,
        restorePositionMinDurationMin = restorePositionMinDurationMin,
        enableBlurEffect = enableBlurEffect,
        enableGlowEffect = enableGlowEffect,
        enableParallaxEffect = enableParallaxEffect,
        backgroundEffect = backgroundEffect.name,
        playerBackgroundMode = playerBackgroundMode.name,
        playerBackgroundVignette = playerBackgroundVignette,
        playerBackgroundAnimated = playerBackgroundAnimated,
        playerBackgroundColorWhenExpanded = playerBackgroundColorWhenExpanded,
        pickColorsFromWallpaper = pickColorsFromWallpaper,
        forceMiniPlayerTrackColors = forceMiniPlayerTrackColors,
        usePitchBlack = usePitchBlack,
        customDarkAccentArgb = customDarkAccentArgb,
        doubleTapToToggleLyrics = doubleTapToToggleLyrics,
        artworkTapAction = artworkTapAction.name,
        artworkLongPressAction = artworkLongPressAction.name,
        appIconPreset = appIconPreset.name,
        widgetConfig = widgetConfig,
        miniPlayerConfig = miniPlayerConfig,
        fullPlayerConfig = fullPlayerConfig,
        soundControl = soundControl
    )

    private fun BackupSettingsDto.toDomain(): AppSettings {
        val defaultLibraryTabs = listOf(
            MainTab.SONGS,
            MainTab.PLAYLISTS,
            MainTab.ALBUMS,
            MainTab.ARTISTS
        )
        val tabs = enabledTabs.mapNotNull { runCatching { MainTab.valueOf(it) }.getOrNull() }
            .distinct()
            .ifEmpty { defaultLibraryTabs }
        val defTab = runCatching { MainTab.valueOf(defaultTab) }.getOrNull()?.takeIf { it in tabs }
            ?: tabs.first()
        val parsedNavOrder = bottomNavOrder.mapNotNull { runCatching { BottomNavDest.valueOf(it) }.getOrNull() }
            .distinct()
            .let { list -> (list + BottomNavDest.entries.filter { it !in list }) }
        val parsedHiddenNav = hiddenBottomNavItems.mapNotNull { runCatching { BottomNavDest.valueOf(it) }.getOrNull() }.toSet()

        return AppSettings(
            themeMode = runCatching { ThemeMode.valueOf(themeMode) }.getOrDefault(ThemeMode.DARK),
            accentColorId = accentColorId,
            customAccentArgb = customAccentArgb,
            autoColoring = autoColoring,
            applyColorOnApp = applyColorOnApp,
            applyColorOnPlayer = applyColorOnPlayer,
            gradientStyle = runCatching { GradientStyle.valueOf(gradientStyle) }.getOrDefault(GradientStyle.SUBTLE_LINEAR),
            applyGradientToMiniPlayer = applyGradientToMiniPlayer,
            applyGradientToFullPlayer = applyGradientToFullPlayer,
            applyGradientToSurfaces = applyGradientToSurfaces,
            sortField = runCatching { SortField.valueOf(sortField) }.getOrDefault(SortField.TITLE),
            sortDirection = runCatching { SortDirection.valueOf(sortDirection) }.getOrDefault(SortDirection.ASCENDING),
            gaplessPlayback = gaplessPlayback,
            crossfadeDurationSec = crossfadeDurationSec,
            playbackSpeed = playbackSpeed,
            playbackPitch = playbackPitch,
            skipIntervalSec = skipIntervalSec,
            rememberLastPosition = rememberLastPosition,
            showLyricsBadges = showLyricsBadges,
            fontScale = fontScale,
            appIconScale = appIconScale.coerceIn(0.80f, 1.30f),
            cornerRadiusDp = cornerRadiusDp.coerceIn(4, 28),
            seekBarStyle = runCatching { SeekBarStyle.valueOf(seekBarStyle) }.getOrDefault(SeekBarStyle.SLEEK_CAPSULE),
            showActualWaveformSeekbar = showActualWaveformSeekbar,
            showLiveWaveformSeekbar = showLiveWaveformSeekbar,
            playbackIconStyle = runCatching { PlaybackIconStyle.valueOf(playbackIconStyle) }.getOrDefault(PlaybackIconStyle.NORMAL),
            enabledTabs = tabs,
            defaultTab = defTab,
            tabTapAction = runCatching { TabTapAction.valueOf(tabTapAction) }.getOrDefault(TabTapAction.OPEN_TAB),
            tabLongPressAction = runCatching { TabLongPressAction.valueOf(tabLongPressAction) }.getOrDefault(TabLongPressAction.CUSTOMIZE_TABS),
            excludedFolders = excludedFolders.toSet(),
            minDurationSec = minDurationSec,
            minFileSizeKb = minFileSizeKb,
            obeyNoMedia = obeyNoMedia,
            autoScanOnStartup = autoScanOnStartup,
            extensionBlacklist = extensionBlacklist.toSet(),
            groupArtworkByAlbum = groupArtworkByAlbum,
            showActualArtworkRatio = showActualArtworkRatio,
            showLikeButtonInList = showLikeButtonInList,
            swipeLeftAction = runCatching { TrackSwipeAction.valueOf(swipeLeftAction) }.getOrDefault(TrackSwipeAction.ADD_TO_QUEUE),
            swipeRightAction = runCatching { TrackSwipeAction.valueOf(swipeRightAction) }.getOrDefault(TrackSwipeAction.FAVORITE),
            swipeToOpenDrawer = swipeToOpenDrawer,
            playlistSortField = runCatching { PlaylistSortField.valueOf(playlistSortField) }.getOrDefault(PlaylistSortField.CREATED_AT),
            playlistSortDirection = runCatching { SortDirection.valueOf(playlistSortDirection) }.getOrDefault(SortDirection.DESCENDING),
            folderListSortField = runCatching { FolderSortField.valueOf(folderListSortField) }.getOrDefault(FolderSortField.NAME),
            folderListSortDirection = runCatching { SortDirection.valueOf(folderListSortDirection) }.getOrDefault(SortDirection.ASCENDING),
            albumSortField = runCatching { AlbumSortField.valueOf(albumSortField) }.getOrDefault(AlbumSortField.NAME),
            albumSortDirection = runCatching { SortDirection.valueOf(albumSortDirection) }.getOrDefault(SortDirection.ASCENDING),
            artistSortField = runCatching { ArtistSortField.valueOf(artistSortField) }.getOrDefault(ArtistSortField.NAME),
            artistSortDirection = runCatching { SortDirection.valueOf(artistSortDirection) }.getOrDefault(SortDirection.ASCENDING),
            showBottomNavBar = showBottomNavBar,
            bottomNavOrder = parsedNavOrder,
            hiddenBottomNavItems = parsedHiddenNav,
            allowSimultaneousPlayback = allowSimultaneousPlayback,
            autoScanAudio = autoScanAudio,
            autoScanVideo = autoScanVideo,
            videoGridView = videoGridView,
            autoBackupIntervalDays = autoBackupIntervalDays,
            enableFoldersHierarchy = enableFoldersHierarchy,
            skipSingleSubfolder = skipSingleSubfolder,
            skipSilence = skipSilence,
            crossfadeEnabled = crossfadeEnabled,
            crossfadeDurationMs = crossfadeDurationMs,
            crossfadeTriggerLastSec = crossfadeTriggerLastSec,
            crossfadeStyle = runCatching { CrossfadeStyle.valueOf(crossfadeStyle) }.getOrDefault(CrossfadeStyle.DEFAULT),
            playPauseFadeEnabled = playPauseFadeEnabled,
            playFadeDurationMs = playFadeDurationMs,
            pauseFadeDurationMs = pauseFadeDurationMs,
            jumpToFirstAfterQueue = jumpToFirstAfterQueue,
            previousButtonReplays = previousButtonReplays,
            restorePositionMinDurationMin = restorePositionMinDurationMin,
            enableBlurEffect = enableBlurEffect,
            enableGlowEffect = enableGlowEffect,
            enableParallaxEffect = enableParallaxEffect,
            backgroundEffect = runCatching { BackgroundEffect.valueOf(backgroundEffect) }.getOrDefault(BackgroundEffect.NONE),
            playerBackgroundMode = runCatching { PlayerBackgroundMode.valueOf(playerBackgroundMode) }.getOrDefault(PlayerBackgroundMode.MESH_GRADIENT),
            playerBackgroundVignette = playerBackgroundVignette,
            playerBackgroundAnimated = playerBackgroundAnimated,
            playerBackgroundColorWhenExpanded = playerBackgroundColorWhenExpanded,
            pickColorsFromWallpaper = pickColorsFromWallpaper,
            forceMiniPlayerTrackColors = forceMiniPlayerTrackColors,
            usePitchBlack = usePitchBlack,
            customDarkAccentArgb = customDarkAccentArgb,
            doubleTapToToggleLyrics = doubleTapToToggleLyrics,
            artworkTapAction = runCatching { ArtworkGestureAction.valueOf(artworkTapAction) }.getOrDefault(ArtworkGestureAction.ARTWORK_OPEN),
            artworkLongPressAction = runCatching { ArtworkGestureAction.valueOf(artworkLongPressAction) }.getOrDefault(ArtworkGestureAction.INFO),
            appIconPreset = runCatching { AppIconPreset.valueOf(appIconPreset) }.getOrDefault(AppIconPreset.AURORA_GLASS),
            widgetConfig = widgetConfig,
            miniPlayerConfig = miniPlayerConfig,
            fullPlayerConfig = fullPlayerConfig,
            soundControl = soundControl
        )
    }
}
