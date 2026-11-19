package com.example.model

import androidx.compose.runtime.Immutable

enum class MainTab(val label: String) {
    SONGS("Songs"),
    PLAYLISTS("Playlists"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    GENRES("Genres"),
    FAVORITES("Favorites"),
    MOST_PLAYED("Most Played"),
    RECENTLY_PLAYED("Recent"),
    RECENTLY_ADDED("New Added"),
    HISTORY("History")
}

enum class SortField(val label: String) {
    TITLE("Song name"),
    ARTIST("Artist"),
    ALBUM("Album"),
    DURATION("Duration"),
    DATE_ADDED("Date added"),
    RECENTLY_PLAYED("Recently played"),
    MOST_PLAYED("Most played")
}

enum class SortDirection(val label: String) {
    ASCENDING("Ascending"),
    DESCENDING("Descending")
}

enum class PlaylistSortField(val label: String) {
    NAME("Playlist name"),
    TRACK_COUNT("Track count"),
    CREATED_AT("Date created"),
    LAST_PLAYED("Recently played"),
    DURATION("Duration")
}

enum class FolderSortField(val label: String) {
    NAME("Folder name"),
    TRACK_COUNT("Track count"),
    SUBFOLDER_COUNT("Subfolders"),
    SIZE("Total size")
}

enum class AlbumSortField(val label: String) {
    NAME("Album name"),
    ARTIST("Artist"),
    TRACK_COUNT("Track count"),
    YEAR("Release year")
}

enum class ArtistSortField(val label: String) {
    NAME("Artist name"),
    TRACK_COUNT("Song count"),
    ALBUM_COUNT("Album count")
}

enum class GenreSortField(val label: String) {
    NAME("Genre"),
    TRACK_COUNT("Track count"),
    DURATION("Duration")
}

enum class CollectionType(val title: String, val subtitle: String) {
    FAVORITES("Favorite Songs", "Tracks you've liked"),
    LYRICS("Lyrics Tracks", "Songs with synced or embedded lyrics"),
    RECENTLY_PLAYED("Recently Played", "Your recent listening history"),
    MOST_PLAYED("Most Played", "Your top repeats"),
    RECENTLY_ADDED("Recently Added", "Newest tracks in your library")
}

@Immutable
data class Song(
    val id: Long,
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
)

object MoodPresets {
    val entries = listOf(
        "Chill",
        "Energetic",
        "Happy",
        "Focus",
        "Sad",
        "Late Night",
        "Workout",
        "Hype"
    )
}

@Immutable
data class Album(
    val id: Long,
    val name: String,
    val artist: String,
    val trackCount: Int,
    val year: Int,
    val representativeSong: Song?
)

@Immutable
data class Artist(
    val id: Long,
    val name: String,
    val songCount: Int,
    val albumCount: Int,
    val representativeSong: Song?
)

@Immutable
data class GenreItem(
    val name: String,
    val songCount: Int,
    val totalDurationMs: Long,
    val representativeSong: Song?
)

@Immutable
data class Playlist(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val lastPlayedAt: Long,
    val songs: List<Song> = emptyList()
) {
    val songCount: Int get() = songs.size
    val totalDurationMs: Long get() = songs.sumOf { it.durationMs }
}

@Immutable
data class FolderItem(
    val name: String,
    val path: String,
    val songCount: Int,
    val subfolderCount: Int,
    val representativeSong: Song?,
    val totalDurationMs: Long = 0L,
    val totalSizeBytes: Long = 0L
)

@Immutable
data class LyricLine(
    val timestampMs: Long,
    val text: String
)

@Immutable
data class LyricsData(
    val isSynced: Boolean,
    val lines: List<LyricLine>,
    val sourceLabel: String = ""
)

@Immutable
data class FormatStat(
    val format: String,
    val trackCount: Int,
    val sizeBytes: Long
)

@Immutable
data class ArtistStat(
    val artist: String,
    val trackCount: Int,
    val playCount: Int
)

@Immutable
data class LibraryStats(
    val totalSongs: Int = 0,
    val totalDurationMs: Long = 0L,
    val totalSizeBytes: Long = 0L,
    val favoritesCount: Int = 0,
    val playlistsCount: Int = 0,
    val albumsCount: Int = 0,
    val artistsCount: Int = 0,
    val foldersCount: Int = 0,
    val lyricsCount: Int = 0,
    val trashedCount: Int = 0,
    val hiddenCount: Int = 0,
    val totalPlayEvents: Int = 0,
    val songsPlayedCount: Int = 0,
    val averageDurationMs: Long = 0L,
    val genresCount: Int = 0,
    val totalPlayCountSum: Int = 0,
    val estimatedListeningMs: Long = 0L,
    val topPlayedSongs: List<Song> = emptyList(),
    val topArtists: List<ArtistStat> = emptyList(),
    val formatBreakdown: List<FormatStat> = emptyList(),
    val topGenres: List<GenreItem> = emptyList()
)
