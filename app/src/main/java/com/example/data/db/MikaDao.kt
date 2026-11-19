package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

data class PlaylistSongOrderRow(
    val playlistId: Long,
    val songId: Long,
    val sortOrder: Int
)

@Dao
interface MikaDao {

    @Query("SELECT * FROM songs WHERE isTrashed = 0 AND isHidden = 0")
    fun observeAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE isTrashed = 0 AND isHidden = 1 ORDER BY title COLLATE NOCASE ASC")
    fun observeHiddenSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE isTrashed = 1 ORDER BY trashedAt DESC")
    fun observeTrashedSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs")
    suspend fun getAllSongsOnce(): List<SongEntity>

    @Query("SELECT * FROM songs WHERE id = :songId LIMIT 1")
    suspend fun getSongById(songId: Long): SongEntity?

    @Query("SELECT * FROM songs WHERE id IN (:ids)")
    suspend fun getSongsByIds(ids: List<Long>): List<SongEntity>

    @Query("SELECT * FROM songs WHERE isTrashed = 0 AND isHidden = 0 AND isFavorite = 1 ORDER BY title COLLATE NOCASE ASC")
    fun observeFavorites(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE isTrashed = 0 AND isHidden = 0 AND lastPlayedAt > 0 ORDER BY lastPlayedAt DESC LIMIT :limit")
    fun observeRecentlyPlayed(limit: Int = 25): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE isTrashed = 0 AND isHidden = 0 ORDER BY dateAddedSec DESC LIMIT :limit")
    fun observeRecentlyAdded(limit: Int = 25): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE isTrashed = 0 AND isHidden = 0 AND playCount > 0 ORDER BY playCount DESC, lastPlayedAt DESC LIMIT :limit")
    fun observeMostPlayed(limit: Int = 25): Flow<List<SongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSongs(songs: List<SongEntity>)

    @Query("DELETE FROM songs WHERE id IN (:ids)")
    suspend fun deleteSongsByIds(ids: List<Long>)

    @Query("UPDATE songs SET isTrashed = 1, trashedAt = :trashedAt WHERE id IN (:ids)")
    suspend fun moveSongsToTrash(ids: List<Long>, trashedAt: Long)

    @Query("UPDATE songs SET isTrashed = 0, trashedAt = 0 WHERE id IN (:ids)")
    suspend fun restoreSongsFromTrash(ids: List<Long>)

    @Query("DELETE FROM playlist_songs WHERE songId IN (:ids)")
    suspend fun removeSongsFromAllPlaylists(ids: List<Long>)

    @Query("DELETE FROM saved_queue WHERE songId IN (:ids)")
    suspend fun removeSongsFromSavedQueue(ids: List<Long>)

    @Query("DELETE FROM play_history WHERE songId IN (:ids)")
    suspend fun removeSongsFromPlayHistory(ids: List<Long>)

    @Transaction
    suspend fun deleteSongsPermanently(ids: List<Long>) {
        ids.chunked(500).forEach { chunk ->
            removeSongsFromAllPlaylists(chunk)
            removeSongsFromSavedQueue(chunk)
            removeSongsFromPlayHistory(chunk)
            deleteSongsByIds(chunk)
        }
    }

    @Transaction
    suspend fun syncLibrary(deletedIds: List<Long>, upsertedSongs: List<SongEntity>) {
        if (deletedIds.isNotEmpty()) {
            deletedIds.chunked(500).forEach { deleteSongsByIds(it) }
        }
        if (upsertedSongs.isNotEmpty()) {
            upsertedSongs.chunked(500).forEach { upsertSongs(it) }
        }
    }

    @Query("UPDATE songs SET isFavorite = :isFavorite WHERE id = :songId")
    suspend fun setFavorite(songId: Long, isFavorite: Boolean)

    @Query("UPDATE songs SET isFavorite = :isFavorite WHERE id IN (:songIds)")
    suspend fun setFavoritesBulk(songIds: List<Long>, isFavorite: Boolean)

    @Query("UPDATE songs SET playCount = playCount + 1, lastPlayedAt = :timestamp WHERE id = :songId")
    suspend fun incrementPlayStats(songId: Long, timestamp: Long)

    @Query("UPDATE songs SET hasLyrics = :hasLyrics WHERE id = :songId")
    suspend fun setHasLyrics(songId: Long, hasLyrics: Boolean)

    @Query("UPDATE songs SET playCount = 0, lastPlayedAt = 0")
    suspend fun clearAllSongPlayStats()

    // Play History
    @Insert
    suspend fun insertPlayHistory(entry: PlayHistoryEntity)

    @Query("SELECT COUNT(*) FROM play_history")
    fun observeTotalPlayHistoryCount(): Flow<Int>

    @Query("SELECT * FROM play_history ORDER BY playedAt DESC")
    fun observeAllPlayHistory(): Flow<List<PlayHistoryEntity>>

    @Query("SELECT * FROM play_history ORDER BY playedAt DESC")
    suspend fun getAllPlayHistoryOnce(): List<PlayHistoryEntity>

    @Insert
    suspend fun insertPlayHistoryList(entries: List<PlayHistoryEntity>)

    @Query("DELETE FROM play_history")
    suspend fun clearPlayHistory()

    @Query("UPDATE songs SET isHidden = :isHidden WHERE id = :songId")
    suspend fun setHidden(songId: Long, isHidden: Boolean)

    @Query("UPDATE songs SET isHidden = :isHidden WHERE id IN (:songIds)")
    suspend fun setHiddenBulk(songIds: List<Long>, isHidden: Boolean)

    @Query("UPDATE songs SET mood = :mood WHERE id = :songId")
    suspend fun setSongMood(songId: Long, mood: String)

    @Query("UPDATE songs SET rating = :rating WHERE id = :songId")
    suspend fun setSongRating(songId: Long, rating: Int)

    @Query("UPDATE songs SET mood = :mood WHERE id IN (:songIds)")
    suspend fun setSongMoodBulk(songIds: List<Long>, mood: String)

    @Query("UPDATE songs SET rating = :rating WHERE id IN (:songIds)")
    suspend fun setSongRatingBulk(songIds: List<Long>, rating: Int)

    @Query("UPDATE songs SET playCount = :playCount, lastPlayedAt = :lastPlayedAt, isFavorite = :isFavorite, hasLyrics = :hasLyrics, isHidden = :isHidden, mood = :mood, rating = :rating WHERE id = :songId")
    suspend fun restoreSongUserState(
        songId: Long,
        playCount: Int,
        lastPlayedAt: Long,
        isFavorite: Boolean,
        hasLyrics: Boolean,
        isHidden: Boolean = false,
        mood: String = "",
        rating: Int = 0
    )

    @Query("UPDATE songs SET playCount = 0, lastPlayedAt = 0, isFavorite = 0, isHidden = 0, mood = '', rating = 0")
    suspend fun resetAllSongStates()

    @Query("DELETE FROM playlists")
    suspend fun clearAllPlaylists()

    @Query("DELETE FROM playlist_songs")
    suspend fun clearAllPlaylistRefs()

    @Query("DELETE FROM track_tag_overrides")
    suspend fun clearAllTagOverrides()

    @Transaction
    suspend fun clearAllUserData() {
        clearPlayHistory()
        clearSearchHistory()
        clearSavedQueue()
        clearAllPlaylistRefs()
        clearAllPlaylists()
        clearAllTagOverrides()
        resetAllSongStates()
    }

    // Playlists
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists ORDER BY createdAt ASC")
    suspend fun getPlaylistsOnce(): List<PlaylistEntity>

    @Query("SELECT * FROM playlist_songs ORDER BY sortOrder ASC")
    suspend fun getAllPlaylistSongRefsOnce(): List<PlaylistSongCrossRef>

    @Query("SELECT * FROM playlists WHERE lastPlayedAt > 0 ORDER BY lastPlayedAt DESC LIMIT :limit")
    fun observeRecentlyPlayedPlaylists(limit: Int = 10): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlist_songs ORDER BY sortOrder ASC")
    fun observeAllPlaylistSongRefs(): Flow<List<PlaylistSongCrossRef>>

    @Insert
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name WHERE id = :playlistId")
    suspend fun renamePlaylist(playlistId: Long, name: String)

    @Query("UPDATE playlists SET lastPlayedAt = :timestamp WHERE id = :playlistId")
    suspend fun markPlaylistPlayed(playlistId: Long, timestamp: Long)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun clearPlaylistSongs(playlistId: Long)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun getMaxSortOrder(playlistId: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistSongs(refs: List<PlaylistSongCrossRef>)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY sortOrder ASC")
    suspend fun getPlaylistSongRefsOnce(playlistId: Long): List<PlaylistSongCrossRef>

    @Transaction
    suspend fun replacePlaylistOrder(playlistId: Long, orderedSongIds: List<Long>) {
        clearPlaylistSongs(playlistId)
        val refs = orderedSongIds.mapIndexed { index, songId ->
            PlaylistSongCrossRef(playlistId = playlistId, songId = songId, sortOrder = index)
        }
        if (refs.isNotEmpty()) {
            insertPlaylistSongs(refs)
        }
    }

    // Search History
    @Query("SELECT * FROM search_history ORDER BY searchedAt DESC LIMIT 15")
    fun observeSearchHistory(): Flow<List<SearchHistoryEntity>>

    @Query("SELECT * FROM search_history ORDER BY searchedAt DESC")
    suspend fun getSearchHistoryOnce(): List<SearchHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSearchHistory(entry: SearchHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSearchHistoryList(entries: List<SearchHistoryEntity>)

    @Query("DELETE FROM search_history WHERE query = :query")
    suspend fun deleteSearchHistoryItem(query: String)

    @Query("DELETE FROM search_history")
    suspend fun clearSearchHistory()

    // Saved Queue
    @Query("SELECT * FROM saved_queue ORDER BY positionIndex ASC")
    suspend fun getSavedQueue(): List<SavedQueueEntity>

    @Query("DELETE FROM saved_queue")
    suspend fun clearSavedQueue()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedQueue(entries: List<SavedQueueEntity>)

    @Transaction
    suspend fun saveQueue(songIds: List<Long>) {
        clearSavedQueue()
        val entries = songIds.mapIndexed { index, id ->
            SavedQueueEntity(positionIndex = index, songId = id)
        }
        if (entries.isNotEmpty()) {
            insertSavedQueue(entries)
        }
    }

    // Track Tag & Artwork Overrides
    @Query("SELECT * FROM track_tag_overrides")
    fun observeAllTagOverrides(): Flow<List<TrackTagOverrideEntity>>

    @Query("SELECT * FROM track_tag_overrides")
    suspend fun getAllTagOverridesOnce(): List<TrackTagOverrideEntity>

    @Query("SELECT * FROM track_tag_overrides WHERE songId IN (:songIds)")
    suspend fun getTagOverridesByIds(songIds: List<Long>): List<TrackTagOverrideEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTagOverrides(overrides: List<TrackTagOverrideEntity>)

    @Query("DELETE FROM track_tag_overrides WHERE songId IN (:songIds)")
    suspend fun deleteTagOverrides(songIds: List<Long>)
}

