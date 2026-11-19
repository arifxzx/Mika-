package com.example.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.model.Song

@Entity(
    tableName = "songs",
    indices = [
        Index(value = ["contentUri"], unique = true),
        Index(value = ["albumId"]),
        Index(value = ["artistId"]),
        Index(value = ["folderPath"]),
        Index(value = ["isTrashed"]),
        Index(value = ["isHidden"]),
        Index(value = ["isFavorite"]),
        Index(value = ["lastPlayedAt"]),
        Index(value = ["playCount"]),
        Index(value = ["dateAddedSec"])
    ]
)
data class SongEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val artistId: Long,
    val durationMs: Long,
    val contentUri: String,
    val filePath: String,
    val folderPath: String,
    val folderName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val dateAddedSec: Long,
    val dateModifiedSec: Long,
    val trackNumber: Int = 0,
    val year: Int = 0,
    val genre: String = "",
    val playCount: Int = 0,
    val lastPlayedAt: Long = 0L,
    val isFavorite: Boolean = false,
    val hasLyrics: Boolean = false,
    val isTrashed: Boolean = false,
    val trashedAt: Long = 0L,
    val isHidden: Boolean = false,
    val mood: String = "",
    val rating: Int = 0
) {
    fun toDomain(): Song = Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        albumId = albumId,
        artistId = artistId,
        durationMs = durationMs,
        contentUri = contentUri,
        filePath = filePath,
        folderPath = folderPath,
        folderName = folderName,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        dateAddedSec = dateAddedSec,
        dateModifiedSec = dateModifiedSec,
        trackNumber = trackNumber,
        year = year,
        genre = genre,
        playCount = playCount,
        lastPlayedAt = lastPlayedAt,
        isFavorite = isFavorite,
        hasLyrics = hasLyrics,
        isTrashed = isTrashed,
        trashedAt = trashedAt,
        isHidden = isHidden,
        mood = mood,
        rating = rating
    )
}

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastPlayedAt: Long = 0L
)

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songId"],
    indices = [Index("playlistId"), Index("songId")]
)
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songId: Long,
    val sortOrder: Int
)

data class PlaylistWithOrderedSongs(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = PlaylistSongCrossRef::class,
            parentColumn = "playlistId",
            entityColumn = "songId"
        )
    )
    val songs: List<SongEntity>
)

@Entity(
    tableName = "play_history",
    indices = [Index(value = ["playedAt"])]
)
data class PlayHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val songId: Long,
    val playedAt: Long
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val query: String,
    val searchedAt: Long
)

@Entity(tableName = "saved_queue")
data class SavedQueueEntity(
    @PrimaryKey val positionIndex: Int,
    val songId: Long
)

@Entity(tableName = "track_tag_overrides")
data class TrackTagOverrideEntity(
    @PrimaryKey val songId: Long,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val genre: String? = null,
    val year: Int? = null,
    val trackNumber: Int? = null,
    val customArtworkPath: String? = null,
    val artworkCleared: Boolean = false,
    val updatedAtMs: Long = System.currentTimeMillis()
)

