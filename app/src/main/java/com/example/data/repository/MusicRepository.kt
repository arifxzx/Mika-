package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.db.MikaDao
import com.example.data.db.PlayHistoryEntity
import com.example.data.db.PlaylistEntity
import com.example.data.db.PlaylistSongCrossRef
import com.example.data.db.SearchHistoryEntity
import com.example.data.db.SongEntity
import com.example.data.db.TrackTagOverrideEntity
import com.example.data.prefs.PersistedPlaybackSnapshot
import com.example.data.prefs.SettingsDataStore
import com.example.media.ArtworkManager
import com.example.media.LyricsParser
import com.example.media.MediaScanner
import com.example.media.VideoScanner
import com.example.media.WaveformExtractor
import com.example.model.AppSettings
import com.example.model.EqualizerState
import com.example.model.LyricsData
import com.example.model.Playlist
import com.example.model.RepeatMode
import com.example.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File

class MusicRepository(
    private val context: Context,
    private val dao: MikaDao,
    val settingsDataStore: SettingsDataStore
) {

    private val overridesByIdFlow: Flow<Map<Long, TrackTagOverrideEntity>> =
        dao.observeAllTagOverrides().map { list -> list.associateBy { it.songId } }

    private fun applyOverride(entity: SongEntity, override: TrackTagOverrideEntity?): Song {
        val base = entity.toDomain()
        if (override == null) return base
        val bumpedModified = maxOf(base.dateModifiedSec, override.updatedAtMs / 1000L)
        return base.copy(
            title = override.title?.takeIf { it.isNotBlank() } ?: base.title,
            artist = override.artist?.takeIf { it.isNotBlank() } ?: base.artist,
            album = override.album?.takeIf { it.isNotBlank() } ?: base.album,
            genre = override.genre ?: base.genre,
            year = override.year ?: base.year,
            trackNumber = override.trackNumber ?: base.trackNumber,
            dateModifiedSec = bumpedModified
        )
    }

    val allSongsFlow: Flow<List<Song>> = combine(
        dao.observeAllSongs(),
        overridesByIdFlow
    ) { list, overrides ->
        list.map { applyOverride(it, overrides[it.id]) }
    }

    val hiddenSongsFlow: Flow<List<Song>> = combine(
        dao.observeHiddenSongs(),
        overridesByIdFlow
    ) { list, overrides ->
        list.map { applyOverride(it, overrides[it.id]) }
    }

    val trashedSongsFlow: Flow<List<Song>> = combine(
        dao.observeTrashedSongs(),
        overridesByIdFlow
    ) { list, overrides ->
        list.map { applyOverride(it, overrides[it.id]) }
    }

    val totalPlayEventsFlow: Flow<Int> = dao.observeTotalPlayHistoryCount()

    val playHistoryFlow: Flow<List<PlayHistoryEntity>> = dao.observeAllPlayHistory()

    val favoritesFlow: Flow<List<Song>> = combine(
        dao.observeFavorites(),
        overridesByIdFlow
    ) { list, overrides ->
        list.map { applyOverride(it, overrides[it.id]) }
    }

    val recentlyPlayedFlow: Flow<List<Song>> = combine(
        dao.observeRecentlyPlayed(25),
        overridesByIdFlow
    ) { list, overrides ->
        list.map { applyOverride(it, overrides[it.id]) }
    }

    val recentlyAddedFlow: Flow<List<Song>> = combine(
        dao.observeRecentlyAdded(25),
        overridesByIdFlow
    ) { list, overrides ->
        list.map { applyOverride(it, overrides[it.id]) }
    }

    val mostPlayedFlow: Flow<List<Song>> = combine(
        dao.observeMostPlayed(25),
        overridesByIdFlow
    ) { list, overrides ->
        list.map { applyOverride(it, overrides[it.id]) }
    }

    val playlistsFlow: Flow<List<Playlist>> = combine(
        dao.observePlaylists(),
        dao.observeAllPlaylistSongRefs(),
        dao.observeAllSongs(),
        overridesByIdFlow
    ) { playlists, refs, songs, overrides ->
        val songMap = songs.associateBy({ it.id }, { applyOverride(it, overrides[it.id]) })
        val refsByPlaylist = refs.groupBy { it.playlistId }
        playlists.map { pl ->
            val orderedSongs = refsByPlaylist[pl.id]
                .orEmpty()
                .sortedBy { it.sortOrder }
                .mapNotNull { songMap[it.songId] }
            Playlist(
                id = pl.id,
                name = pl.name,
                createdAt = pl.createdAt,
                lastPlayedAt = pl.lastPlayedAt,
                songs = orderedSongs
            )
        }
    }

    val recentlyPlayedPlaylistsFlow: Flow<List<Playlist>> = combine(
        dao.observeRecentlyPlayedPlaylists(10),
        playlistsFlow
    ) { recent, all ->
        val byId = all.associateBy { it.id }
        recent.mapNotNull { byId[it.id] }
    }

    val searchHistoryFlow: Flow<List<String>> = dao.observeSearchHistory().map { entries ->
        entries.map { it.query }
    }

    val settingsFlow: Flow<AppSettings> = settingsDataStore.settingsFlow
    val equalizerFlow: Flow<EqualizerState> = settingsDataStore.equalizerFlow

    suspend fun scanLibrary(): Int {
        val settings = settingsFlow.first()
        ArtworkManager.groupByAlbumEnabled = settings.groupArtworkByAlbum
        val hasPerm = MediaScanner.hasAudioPermission(context)
        if (!hasPerm && settings.includedFolderUris.isEmpty()) return 0

        val filterConfig = MediaScanner.AudioFilterConfig(
            excludedFolders = settings.excludedFolders,
            includedFolderUris = settings.includedFolderUris,
            minDurationMs = maxOf(1000L, settings.minDurationSec * 1000L),
            minSizeBytes = settings.minFileSizeKb * 1024L,
            obeyNoMedia = settings.obeyNoMedia,
            extensionBlacklist = settings.extensionBlacklist
        )
        val existing = dao.getAllSongsOnce()
        val existingById = existing.associateBy { it.id }

        val mediaStoreScanned = if (hasPerm) {
            MediaScanner.scanDeviceAudio(context, existingById, filterConfig)
        } else {
            emptyList()
        }
        val mediaStoreKeys = mediaStoreScanned.mapTo(HashSet()) {
            "${it.title.lowercase()}|${it.sizeBytes}|${it.durationMs / 1000L}"
        }
        val safScanned = if (settings.includedFolderUris.isNotEmpty()) {
            MediaScanner.scanSafIncludedFolders(
                context = context,
                includedFolderUris = settings.includedFolderUris,
                existingById = existingById,
                filterConfig = filterConfig
            ).filter { s ->
                val key = "${s.title.lowercase()}|${s.sizeBytes}|${s.durationMs / 1000L}"
                key !in mediaStoreKeys
            }
        } else {
            emptyList()
        }

        val scanned = mediaStoreScanned + safScanned
        val scannedIds = scanned.map { it.id }.toSet()

        // Preserve manually imported single SAF files (/Imported) while pruning removed MediaStore or SAF-tree entries
        val deletedIds = existing
            .filter {
                (it.id > 0L && hasPerm && it.id !in scannedIds) ||
                    (it.id < 0L && it.folderPath.startsWith("/Added Folders") && it.id !in scannedIds)
            }
            .map { it.id }

        val changedOrNew = scanned.filter { entity ->
            val old = existingById[entity.id]
            old == null || old != entity
        }

        if (deletedIds.isNotEmpty() || changedOrNew.isNotEmpty()) {
            dao.syncLibrary(deletedIds = deletedIds, upsertedSongs = changedOrNew)
        }
        return scanned.size
    }

    suspend fun getDeviceAudioFolders(): List<MediaScanner.DeviceFolderSummary> {
        return MediaScanner.listDeviceAudioFolders(context)
    }

    suspend fun importAudioUri(uri: Uri): Song? {
        val entity = MediaScanner.importAudioUri(context, uri) ?: return null
        dao.upsertSongs(listOf(entity))
        val override = dao.getTagOverridesByIds(listOf(entity.id)).firstOrNull()
        return applyOverride(entity, override)
    }

    suspend fun getSongById(songId: Long): Song? {
        val entity = dao.getSongById(songId) ?: return null
        val override = dao.getTagOverridesByIds(listOf(songId)).firstOrNull()
        return applyOverride(entity, override)
    }

    suspend fun updateSongTagsAndArtwork(
        songIds: List<Long>,
        title: String? = null,
        artist: String? = null,
        album: String? = null,
        genre: String? = null,
        year: Int? = null,
        trackNumber: Int? = null,
        artworkUri: Uri? = null,
        clearArtwork: Boolean = false
    ): List<Song> {
        if (songIds.isEmpty()) return emptyList()
        val nowMs = System.currentTimeMillis()
        val existingOverrides = dao.getTagOverridesByIds(songIds).associateBy { it.songId }

        val savedArtworkPaths = when {
            clearArtwork -> {
                ArtworkManager.clearCustomArtworkForSongs(context, songIds)
                emptyMap()
            }
            artworkUri != null -> ArtworkManager.saveCustomArtworkFromUri(context, songIds, artworkUri)
            else -> emptyMap()
        }

        val updatedOverrides = songIds.map { id ->
            val prev = existingOverrides[id]
            val newArtPath = when {
                clearArtwork -> null
                savedArtworkPaths.containsKey(id) -> savedArtworkPaths[id]
                else -> prev?.customArtworkPath
            }
            TrackTagOverrideEntity(
                songId = id,
                title = if (songIds.size == 1 && !title.isNullOrBlank()) title.trim() else prev?.title,
                artist = artist?.trim()?.takeIf { it.isNotEmpty() } ?: prev?.artist,
                album = album?.trim()?.takeIf { it.isNotEmpty() } ?: prev?.album,
                genre = genre?.trim() ?: prev?.genre,
                year = year ?: prev?.year,
                trackNumber = if (songIds.size == 1) trackNumber ?: prev?.trackNumber else prev?.trackNumber,
                customArtworkPath = newArtPath,
                artworkCleared = clearArtwork || (prev?.artworkCleared == true && artworkUri == null && newArtPath == null),
                updatedAtMs = nowMs
            )
        }
        dao.upsertTagOverrides(updatedOverrides)
        val overrideById = updatedOverrides.associateBy { it.songId }
        return dao.getSongsByIds(songIds).map { applyOverride(it, overrideById[it.id]) }
    }

    suspend fun setFavorite(songId: Long, isFavorite: Boolean) {
        dao.setFavorite(songId, isFavorite)
    }

    suspend fun toggleFavorite(songId: Long) {
        val song = dao.getSongById(songId) ?: return
        dao.setFavorite(songId, !song.isFavorite)
    }

    suspend fun setFavoritesBulk(songIds: List<Long>, isFavorite: Boolean) {
        if (songIds.isNotEmpty()) {
            dao.setFavoritesBulk(songIds, isFavorite)
        }
    }

    suspend fun setHidden(songId: Long, isHidden: Boolean) {
        dao.setHidden(songId, isHidden)
    }

    suspend fun setHiddenBulk(songIds: List<Long>, isHidden: Boolean) {
        if (songIds.isNotEmpty()) {
            dao.setHiddenBulk(songIds, isHidden)
        }
    }

    suspend fun setSongMood(songId: Long, mood: String) {
        dao.setSongMood(songId, mood.trim())
    }

    suspend fun setSongRating(songId: Long, rating: Int) {
        dao.setSongRating(songId, rating.coerceIn(0, 5))
    }

    suspend fun setSongMoodBulk(songIds: List<Long>, mood: String) {
        if (songIds.isNotEmpty()) {
            dao.setSongMoodBulk(songIds, mood.trim())
        }
    }

    suspend fun setSongRatingBulk(songIds: List<Long>, rating: Int) {
        if (songIds.isNotEmpty()) {
            dao.setSongRatingBulk(songIds, rating.coerceIn(0, 5))
        }
    }

    data class DuplicateTrackGroup(
        val signature: String,
        val displayName: String,
        val songs: List<Song>,
        val reclaimableBytes: Long
    )

    suspend fun findDuplicateGroups(): List<DuplicateTrackGroup> {
        val overrides = dao.getAllTagOverridesOnce().associateBy { it.songId }
        val activeSongs = dao.getAllSongsOnce()
            .filter { !it.isTrashed }
            .map { applyOverride(it, overrides[it.id]) }

        return activeSongs
            .groupBy { song ->
                val durBucket = song.durationMs / 1000L
                if (song.sizeBytes > 0L) {
                    "sz_${song.sizeBytes}_$durBucket"
                } else {
                    "nm_${song.title.trim().lowercase()}_${song.artist.trim().lowercase()}_$durBucket"
                }
            }
            .filter { (_, group) -> group.size >= 2 }
            .map { (sig, group) ->
                val sorted = group.sortedWith(
                    compareByDescending<Song> { it.playCount }
                        .thenByDescending { it.isFavorite }
                        .thenBy { it.dateAddedSec }
                )
                val reclaimable = sorted.drop(1).sumOf { it.sizeBytes.coerceAtLeast(0L) }
                DuplicateTrackGroup(
                    signature = sig,
                    displayName = "${sorted.first().title} • ${sorted.first().artist}",
                    songs = sorted,
                    reclaimableBytes = reclaimable
                )
            }
            .sortedByDescending { it.reclaimableBytes }
    }

    suspend fun recordSongPlayed(songId: Long) {
        val now = System.currentTimeMillis()
        dao.incrementPlayStats(songId, now)
        dao.insertPlayHistory(PlayHistoryEntity(songId = songId, playedAt = now))
    }

    suspend fun createPlaylist(name: String, initialSongIds: List<Long> = emptyList()): Long {
        val trimmed = name.trim().ifBlank { "Untitled Playlist" }
        val playlistId = dao.insertPlaylist(PlaylistEntity(name = trimmed))
        if (initialSongIds.isNotEmpty()) {
            val refs = initialSongIds.mapIndexed { idx, songId ->
                PlaylistSongCrossRef(playlistId = playlistId, songId = songId, sortOrder = idx)
            }
            dao.insertPlaylistSongs(refs)
        }
        return playlistId
    }

    suspend fun renamePlaylist(playlistId: Long, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isNotEmpty()) {
            dao.renamePlaylist(playlistId, trimmed)
        }
    }

    suspend fun deletePlaylist(playlistId: Long) {
        dao.deletePlaylist(playlistId)
    }

    suspend fun markPlaylistPlayed(playlistId: Long) {
        dao.markPlaylistPlayed(playlistId, System.currentTimeMillis())
    }

    suspend fun addSongsToPlaylist(playlistId: Long, songIds: List<Long>) {
        if (songIds.isEmpty()) return
        val existingRefs = dao.getPlaylistSongRefsOnce(playlistId)
        val existingIds = existingRefs.map { it.songId }.toSet()
        var nextOrder = (existingRefs.maxOfOrNull { it.sortOrder } ?: -1) + 1
        val toAdd = songIds.filter { it !in existingIds }.map { songId ->
            PlaylistSongCrossRef(playlistId = playlistId, songId = songId, sortOrder = nextOrder++)
        }
        if (toAdd.isNotEmpty()) {
            dao.insertPlaylistSongs(toAdd)
        }
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        dao.removeSongFromPlaylist(playlistId, songId)
    }

    suspend fun reorderPlaylistSongs(playlistId: Long, orderedSongIds: List<Long>) {
        dao.replacePlaylistOrder(playlistId, orderedSongIds)
    }

    suspend fun saveSearchQuery(query: String) {
        val clean = query.trim()
        if (clean.isNotEmpty()) {
            dao.upsertSearchHistory(SearchHistoryEntity(clean, System.currentTimeMillis()))
        }
    }

    suspend fun removeSearchQuery(query: String) {
        dao.deleteSearchHistoryItem(query)
    }

    suspend fun clearSearchHistory() {
        dao.clearSearchHistory()
    }

    suspend fun loadLyrics(song: Song): LyricsData? {
        val lyrics = LyricsParser.loadLyrics(context, song)
        if (lyrics != null && !song.hasLyrics) {
            dao.setHasLyrics(song.id, true)
        }
        return lyrics
    }

    suspend fun saveCustomLyrics(songId: Long, rawText: String): LyricsData? {
        val saved = LyricsParser.saveCustomLyrics(context, songId, rawText)
        dao.setHasLyrics(songId, saved != null)
        return saved
    }

    suspend fun clearCustomLyrics(song: Song): LyricsData? {
        LyricsParser.deleteCustomLyrics(context, song.id)
        val fallback = LyricsParser.loadLyrics(context, song)
        dao.setHasLyrics(song.id, fallback != null)
        return fallback
    }

    suspend fun createLocalBackup() =
        com.example.data.backup.BackupManager.createInternalBackup(context, dao, settingsDataStore)

    suspend fun exportBackupToUri(uri: Uri) =
        com.example.data.backup.BackupManager.exportToUri(context, uri, dao, settingsDataStore)

    suspend fun parseLocalBackupFile(file: File) =
        com.example.data.backup.BackupManager.parseFromFile(file)

    suspend fun parseBackupUri(uri: Uri) =
        com.example.data.backup.BackupManager.parseFromUri(context, uri)

    suspend fun listLocalBackups() =
        com.example.data.backup.BackupManager.listLocalBackups(context)

    suspend fun restoreBackupPayload(payload: com.example.data.backup.MikaBackupPayload) =
        com.example.data.backup.BackupManager.restorePayload(context, payload, dao, settingsDataStore)

    suspend fun runScheduledAutoBackupIfDue() =
        com.example.data.backup.BackupManager.runScheduledAutoBackupIfDue(context, dao, settingsDataStore)

    suspend fun savePlaybackQueue(songIds: List<Long>) {
        dao.saveQueue(songIds)
    }

    suspend fun loadSavedQueue(): List<Song> {
        val saved = dao.getSavedQueue()
        if (saved.isEmpty()) return emptyList()
        val ids = saved.map { it.songId }
        val songsById = dao.getSongsByIds(ids).associateBy { it.id }
        val overrides = dao.getTagOverridesByIds(ids).associateBy { it.songId }
        return ids.mapNotNull { id ->
            songsById[id]?.let { applyOverride(it, overrides[id]) }
        }
    }

    suspend fun getPlaybackSnapshot(): PersistedPlaybackSnapshot =
        settingsDataStore.getPlaybackSnapshot()

    suspend fun savePlaybackSnapshot(
        songId: Long,
        positionMs: Long,
        shuffle: Boolean,
        repeatMode: RepeatMode
    ) {
        settingsDataStore.savePlaybackState(songId, positionMs, shuffle, repeatMode)
    }

    fun clearArtworkCache(): Long = ArtworkManager.clearCache(context)

    fun getArtworkCacheSizeBytes(): Long = ArtworkManager.getCacheSizeBytes(context)

    fun clearAllAppCaches(): Long {
        var freed = ArtworkManager.clearCache(context)
        freed += WaveformExtractor.clearCache(context)
        freed += VideoScanner.clearCache(context)
        val coilDir = File(context.cacheDir, "coil_image_cache")
        if (coilDir.exists()) {
            coilDir.walkTopDown().filter { it.isFile }.forEach { file ->
                freed += file.length()
                file.delete()
            }
        }
        return freed
    }

    fun getTotalCacheSizeBytes(): Long {
        var total = ArtworkManager.getCacheSizeBytes(context)
        total += VideoScanner.getCacheSizeBytes(context)
        val waveformDir = File(context.cacheDir, "waveform_cache")
        if (waveformDir.exists()) {
            total += waveformDir.listFiles()?.sumOf { it.length() } ?: 0L
        }
        val coilDir = File(context.cacheDir, "coil_image_cache")
        if (coilDir.exists()) {
            total += coilDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        }
        return total
    }

    suspend fun clearPlaybackHistory() {
        dao.clearPlayHistory()
        dao.clearAllSongPlayStats()
    }

    suspend fun resetAllSettings() {
        settingsDataStore.resetAllSettings()
    }

    suspend fun clearAllAppData() {
        dao.clearAllUserData()
        settingsDataStore.resetAllSettings()
        ArtworkManager.clearCache(context)
        File(context.filesDir, "custom_lyrics").deleteRecursively()
    }

    suspend fun moveSongsToTrash(songIds: List<Long>) {
        if (songIds.isEmpty()) return
        dao.moveSongsToTrash(songIds, System.currentTimeMillis())
    }

    suspend fun restoreSongsFromTrash(songIds: List<Long>) {
        if (songIds.isEmpty()) return
        dao.restoreSongsFromTrash(songIds)
    }

    suspend fun permanentlyDeleteSongs(songs: List<Song>, attemptDeviceFileDelete: Boolean = true) {
        if (songs.isEmpty()) return
        if (attemptDeviceFileDelete) {
            songs.forEach { song ->
                runCatching {
                    val uri = Uri.parse(song.contentUri)
                    val deletedViaResolver = context.contentResolver.delete(uri, null, null)
                    if (deletedViaResolver <= 0 && song.filePath.isNotBlank()) {
                        val file = File(song.filePath)
                        if (file.exists()) {
                            file.delete()
                        }
                    }
                }.onFailure {
                    if (song.filePath.isNotBlank()) {
                        runCatching {
                            val file = File(song.filePath)
                            if (file.exists()) file.delete()
                        }
                    }
                }
            }
        }
        dao.deleteSongsPermanently(songs.map { it.id })
        dao.deleteTagOverrides(songs.map { it.id })
        ArtworkManager.clearCustomArtworkForSongs(context, songs.map { it.id })
    }
}
