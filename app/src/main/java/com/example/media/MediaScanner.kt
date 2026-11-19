package com.example.media

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.example.data.db.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object MediaScanner {

    data class AudioFilterConfig(
        val excludedFolders: Set<String> = emptySet(),
        val includedFolderUris: Set<String> = emptySet(),
        val minDurationMs: Long = 1000L,
        val minSizeBytes: Long = 0L,
        val obeyNoMedia: Boolean = true,
        val extensionBlacklist: Set<String> = emptySet()
    )

    data class DeviceFolderSummary(
        val path: String,
        val name: String,
        val trackCount: Int
    )

    val supportedExtensions = setOf(
        "mp3", "m4a", "aac", "flac", "wav", "ogg", "oga", "opus",
        "wma", "alac", "amr", "3gp", "3ga", "mka", "aiff", "aif",
        "ape", "dsf", "dff", "m4b", "m4p", "mp4", "mid", "midi",
        "webm", "ac3", "eac3", "dts", "caf", "wv", "tta"
    )

    fun requiredAudioPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    fun hasAudioPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            requiredAudioPermission()
        ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun scanDeviceAudio(
        context: Context,
        existingById: Map<Long, SongEntity>,
        filterConfig: AudioFilterConfig = AudioFilterConfig()
    ): List<SongEntity> = withContext(Dispatchers.IO) {
        if (!hasAudioPermission(context)) return@withContext emptyList()

        val results = mutableListOf<SongEntity>()
        val seenUris = HashSet<String>()

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.Audio.Media.TITLE)
            add(MediaStore.Audio.Media.ARTIST)
            add(MediaStore.Audio.Media.ALBUM)
            add(MediaStore.Audio.Media.ALBUM_ID)
            add(MediaStore.Audio.Media.ARTIST_ID)
            add(MediaStore.Audio.Media.DURATION)
            add(MediaStore.Audio.Media.DATE_ADDED)
            add(MediaStore.Audio.Media.DATE_MODIFIED)
            add(MediaStore.Audio.Media.SIZE)
            add(MediaStore.Audio.Media.MIME_TYPE)
            add(MediaStore.Audio.Media.TRACK)
            add(MediaStore.Audio.Media.YEAR)
            add(MediaStore.Audio.Media.DISPLAY_NAME)
            @Suppress("DEPRECATION")
            add(MediaStore.Audio.Media.DATA)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Audio.Media.RELATIVE_PATH)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                add(MediaStore.Audio.Media.GENRE)
            }
        }.toTypedArray()

        val genreFallbackMap = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            buildAudioGenreMap(context)
        } else emptyMap()

        val minDuration = maxOf(1000L, filterConfig.minDurationMs)
        val selection = "${MediaStore.Audio.Media.DURATION} >= $minDuration"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        val lrcCacheByFolder = HashMap<String, Set<String>>()
        val existingFolders = HashMap<String, Boolean>()
        val noMediaFolderCache = HashMap<String, Boolean>()
        val normalizedBlacklist = filterConfig.extensionBlacklist
            .map { it.trim().trimStart('.').lowercase() }
            .filter { it.isNotEmpty() }
            .toSet()

        fun isInsideNoMediaFolder(folderPath: String): Boolean {
            if (!filterConfig.obeyNoMedia || folderPath.isBlank()) return false
            return noMediaFolderCache.getOrPut(folderPath) {
                var dir: File? = File(folderPath)
                var depth = 0
                var blocked = false
                while (dir != null && depth < 4) {
                    if (File(dir, ".nomedia").exists()) {
                        blocked = true
                        break
                    }
                    dir = dir.parentFile
                    depth++
                }
                blocked
            }
        }

        context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val artistIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val dateModCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val displayNameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            @Suppress("DEPRECATION")
            val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
            val relPathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
            } else -1
            val genreCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                cursor.getColumnIndex(MediaStore.Audio.Media.GENRE)
            } else -1

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val displayName = cursor.getString(displayNameCol) ?: ""
                val filePath = if (dataCol >= 0) cursor.getString(dataCol) ?: "" else ""
                val ext = (filePath.substringAfterLast('.', "")
                    .ifBlank { displayName.substringAfterLast('.', "") })
                    .lowercase()

                if (ext.isNotEmpty() && ext in normalizedBlacklist) continue
                if (ext.isNotEmpty() && ext !in supportedExtensions) {
                    val mime = cursor.getString(mimeCol) ?: ""
                    if (!mime.startsWith("audio/") && mime != "application/ogg") continue
                }

                // Skip deleted files on filesystem if path is directly readable
                if (filePath.isNotBlank()) {
                    val f = File(filePath)
                    val parentPath = f.parent ?: ""
                    val parentExists = existingFolders.getOrPut(parentPath) { f.parentFile?.exists() == true }
                    if (parentExists && !f.exists()) continue
                }

                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                ).toString()
                if (!seenUris.add(contentUri)) continue

                val sizeBytes = cursor.getLong(sizeCol)
                if (filterConfig.minSizeBytes > 0L && sizeBytes in 1 until filterConfig.minSizeBytes) {
                    continue
                }

                val relPath = if (relPathCol >= 0) cursor.getString(relPathCol) ?: "" else ""
                val folderPath = when {
                    filePath.isNotBlank() -> File(filePath).parent ?: "/Music"
                    relPath.isNotBlank() -> "/" + relPath.trimEnd('/')
                    else -> "/Music"
                }
                if (folderPath in filterConfig.excludedFolders) continue
                if (isInsideNoMediaFolder(folderPath)) continue
                val folderName = folderPath.trimEnd('/').substringAfterLast('/').ifBlank { "Internal Storage" }

                val rawTitle = cursor.getString(titleCol)?.trim().orEmpty()
                val title = rawTitle.ifBlank {
                    displayName.substringBeforeLast('.').ifBlank { "Unknown Track" }
                }
                val artist = cursor.getString(artistCol)?.trim()
                    ?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "Unknown Artist"
                val album = cursor.getString(albumCol)?.trim()
                    ?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "Unknown Album"

                val existing = existingById[id]
                val hasSidecarLrc = if (filePath.isNotBlank()) {
                    val audioFile = File(filePath)
                    val parentDir = audioFile.parentFile
                    val lrcBases = if (parentDir != null) {
                        lrcCacheByFolder.getOrPut(parentDir.absolutePath) {
                            parentDir.list { _, name -> name.endsWith(".lrc", ignoreCase = true) }
                                ?.map { it.substringBeforeLast('.').lowercase() }
                                ?.toSet()
                                ?: emptySet()
                        }
                    } else emptySet()
                    audioFile.nameWithoutExtension.lowercase() in lrcBases
                } else false
                val hasLyrics = hasSidecarLrc || (existing?.hasLyrics == true)

                val genreRaw = if (genreCol >= 0) cursor.getString(genreCol) else genreFallbackMap[id]
                val genre = normalizeGenre(genreRaw)

                results.add(
                    SongEntity(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        albumId = cursor.getLong(albumIdCol),
                        artistId = cursor.getLong(artistIdCol),
                        durationMs = cursor.getLong(durationCol),
                        contentUri = contentUri,
                        filePath = filePath,
                        folderPath = folderPath,
                        folderName = folderName,
                        mimeType = cursor.getString(mimeCol) ?: "audio/*",
                        sizeBytes = sizeBytes,
                        dateAddedSec = cursor.getLong(dateAddedCol),
                        dateModifiedSec = cursor.getLong(dateModCol),
                        trackNumber = cursor.getInt(trackCol),
                        year = cursor.getInt(yearCol),
                        genre = genre,
                        playCount = existing?.playCount ?: 0,
                        lastPlayedAt = existing?.lastPlayedAt ?: 0L,
                        isFavorite = existing?.isFavorite ?: false,
                        hasLyrics = hasLyrics,
                        isTrashed = existing?.isTrashed ?: false,
                        trashedAt = existing?.trashedAt ?: 0L,
                        isHidden = existing?.isHidden ?: false,
                        mood = existing?.mood ?: "",
                        rating = existing?.rating ?: 0
                    )
                )
            }
        }

        results
    }

    private fun normalizeGenre(raw: String?): String {
        val cleaned = raw?.trim().orEmpty()
        if (cleaned.isBlank() || cleaned == "<unknown>" || cleaned == "null") return ""
        return cleaned
    }

    private fun buildAudioGenreMap(context: Context): Map<Long, String> {
        val map = HashMap<Long, String>()
        runCatching {
            val genresUri = MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI
            context.contentResolver.query(
                genresUri,
                arrayOf(MediaStore.Audio.Genres._ID, MediaStore.Audio.Genres.NAME),
                null,
                null,
                null
            )?.use { gc ->
                val idIdx = gc.getColumnIndexOrThrow(MediaStore.Audio.Genres._ID)
                val nameIdx = gc.getColumnIndexOrThrow(MediaStore.Audio.Genres.NAME)
                while (gc.moveToNext()) {
                    val genreId = gc.getLong(idIdx)
                    val genreName = normalizeGenre(gc.getString(nameIdx))
                    if (genreName.isEmpty()) continue
                    val membersUri = MediaStore.Audio.Genres.Members.getContentUri("external", genreId)
                    context.contentResolver.query(
                        membersUri,
                        arrayOf(MediaStore.Audio.Genres.Members.AUDIO_ID),
                        null,
                        null,
                        null
                    )?.use { mc ->
                        val audioCol = mc.getColumnIndexOrThrow(MediaStore.Audio.Genres.Members.AUDIO_ID)
                        while (mc.moveToNext()) {
                            val audioId = mc.getLong(audioCol)
                            map.putIfAbsent(audioId, genreName)
                        }
                    }
                }
            }
        }
        return map
    }

    suspend fun listDeviceAudioFolders(context: Context): List<DeviceFolderSummary> =
        withContext(Dispatchers.IO) {
            if (!hasAudioPermission(context)) return@withContext emptyList()
            val counts = HashMap<String, Int>()
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }
            val projection = buildList {
                @Suppress("DEPRECATION")
                add(MediaStore.Audio.Media.DATA)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    add(MediaStore.Audio.Media.RELATIVE_PATH)
                }
            }.toTypedArray()

            context.contentResolver.query(
                collection,
                projection,
                "${MediaStore.Audio.Media.DURATION} > 1000",
                null,
                null
            )?.use { cursor ->
                @Suppress("DEPRECATION")
                val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                val relCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
                } else -1
                while (cursor.moveToNext()) {
                    val filePath = if (dataCol >= 0) cursor.getString(dataCol) ?: "" else ""
                    val relPath = if (relCol >= 0) cursor.getString(relCol) ?: "" else ""
                    val folderPath = when {
                        filePath.isNotBlank() -> File(filePath).parent ?: "/Music"
                        relPath.isNotBlank() -> "/" + relPath.trimEnd('/')
                        else -> "/Music"
                    }
                    counts[folderPath] = (counts[folderPath] ?: 0) + 1
                }
            }
            counts.entries
                .map { (path, count) ->
                    DeviceFolderSummary(
                        path = path,
                        name = path.trimEnd('/').substringAfterLast('/').ifBlank { "Internal Storage" },
                        trackCount = count
                    )
                }
                .sortedBy { it.name.lowercase() }
        }

    suspend fun importAudioUri(context: Context, uri: Uri): SongEntity? = withContext(Dispatchers.IO) {
        val retriever = android.media.MediaMetadataRetriever()
        return@withContext runCatching {
            retriever.setDataSource(context, uri)
            val title = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() }
                ?: uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.')
                ?: "Imported Audio"
            val artist = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.takeIf { it.isNotBlank() } ?: "Unknown Artist"
            val album = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?.takeIf { it.isNotBlank() } ?: "Imported Files"
            val durationMs = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            val mime = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                ?: "audio/*"
            val genre = normalizeGenre(
                retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_GENRE)
            )
            val stableId = -kotlin.math.abs(uri.toString().hashCode().toLong()).coerceAtLeast(1L)
            SongEntity(
                id = stableId,
                title = title,
                artist = artist,
                album = album,
                albumId = stableId,
                artistId = stableId,
                durationMs = durationMs,
                contentUri = uri.toString(),
                filePath = "",
                folderPath = "/Imported",
                folderName = "Imported",
                mimeType = mime,
                sizeBytes = 0L,
                dateAddedSec = System.currentTimeMillis() / 1000L,
                dateModifiedSec = System.currentTimeMillis() / 1000L,
                genre = genre,
                hasLyrics = false
            )
        }.also {
            runCatching { retriever.release() }
        }.getOrNull()
    }

    suspend fun scanSafIncludedFolders(
        context: Context,
        includedFolderUris: Set<String>,
        existingById: Map<Long, SongEntity>,
        filterConfig: AudioFilterConfig
    ): List<SongEntity> = withContext(Dispatchers.IO) {
        if (includedFolderUris.isEmpty()) return@withContext emptyList()
        val results = mutableListOf<SongEntity>()
        val seenDocUris = HashSet<String>()

        val normalizedBlacklist = filterConfig.extensionBlacklist
            .map { it.trim().trimStart('.').lowercase() }
            .filter { it.isNotEmpty() }
            .toSet()

        fun walkTree(
            treeUri: Uri,
            documentId: String,
            folderDisplayPath: String,
            depth: Int
        ) {
            if (depth > 5) return
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
            )
            val dirs = mutableListOf<Pair<String, String>>()
            val files = mutableListOf<Triple<String, String, String>>()
            val sizes = mutableMapOf<String, Long>()
            val mods = mutableMapOf<String, Long>()
            var hasNoMedia = false

            runCatching {
                context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                    val idIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    val sizeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                    val modIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                    while (cursor.moveToNext()) {
                        val childDocId = cursor.getString(idIdx) ?: continue
                        val displayName = cursor.getString(nameIdx) ?: "Audio"
                        val mime = cursor.getString(mimeIdx) ?: ""

                        if (displayName.equals(".nomedia", ignoreCase = true)) {
                            hasNoMedia = true
                            if (filterConfig.obeyNoMedia) return@use
                            continue
                        }

                        if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                            dirs.add(childDocId to displayName)
                        } else {
                            files.add(Triple(childDocId, displayName, mime))
                            sizes[childDocId] = if (sizeIdx >= 0) cursor.getLong(sizeIdx) else 0L
                            mods[childDocId] = if (modIdx >= 0) cursor.getLong(modIdx) / 1000L else System.currentTimeMillis() / 1000L
                        }
                    }
                }
            }

            if (filterConfig.obeyNoMedia && hasNoMedia) return

            for ((childDocId, displayName) in dirs) {
                walkTree(treeUri, childDocId, "$folderDisplayPath/$displayName", depth + 1)
            }

            for ((childDocId, displayName, mime) in files) {
                val ext = displayName.substringAfterLast('.', "").lowercase()
                if (ext.isNotEmpty() && ext in normalizedBlacklist) continue
                val isAudio = mime.startsWith("audio/") || mime == "application/ogg" || ext in supportedExtensions
                if (!isAudio) continue

                val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childDocId)
                val docUriStr = docUri.toString()
                if (!seenDocUris.add(docUriStr)) continue

                val sizeBytes = sizes[childDocId] ?: 0L
                if (filterConfig.minSizeBytes > 0L && sizeBytes in 1 until filterConfig.minSizeBytes) continue
                if (folderDisplayPath in filterConfig.excludedFolders) continue

                val stableId = -kotlin.math.abs(docUriStr.hashCode().toLong()).coerceAtLeast(100L)
                val existing = existingById[stableId]
                val lastModSec = mods[childDocId] ?: (System.currentTimeMillis() / 1000L)

                if (existing != null && existing.dateModifiedSec == lastModSec && existing.durationMs >= filterConfig.minDurationMs) {
                    results.add(existing)
                    continue
                }

                val retriever = android.media.MediaMetadataRetriever()
                val parsed = runCatching {
                    retriever.setDataSource(context, docUri)
                    val durationMs = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                    if (durationMs < filterConfig.minDurationMs) return@runCatching null
                    val title = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE)
                        ?.takeIf { it.isNotBlank() } ?: displayName.substringBeforeLast('.').ifBlank { displayName }
                    val artist = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST)
                        ?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "Unknown Artist"
                    val album = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ALBUM)
                        ?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: folderDisplayPath.substringAfterLast('/')
                    val genre = normalizeGenre(retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_GENRE))
                    val folderName = folderDisplayPath.substringAfterLast('/').ifBlank { "Added Folder" }

                    SongEntity(
                        id = stableId,
                        title = title,
                        artist = artist,
                        album = album,
                        albumId = -kotlin.math.abs(album.lowercase().hashCode().toLong()).coerceAtLeast(100L),
                        artistId = -kotlin.math.abs(artist.lowercase().hashCode().toLong()).coerceAtLeast(100L),
                        durationMs = durationMs,
                        contentUri = docUriStr,
                        filePath = "",
                        folderPath = folderDisplayPath,
                        folderName = folderName,
                        mimeType = mime.ifBlank { "audio/*" },
                        sizeBytes = sizeBytes,
                        dateAddedSec = lastModSec,
                        dateModifiedSec = lastModSec,
                        genre = genre,
                        playCount = existing?.playCount ?: 0,
                        lastPlayedAt = existing?.lastPlayedAt ?: 0L,
                        isFavorite = existing?.isFavorite ?: false,
                        hasLyrics = existing?.hasLyrics ?: false,
                        isTrashed = existing?.isTrashed ?: false,
                        trashedAt = existing?.trashedAt ?: 0L,
                        isHidden = existing?.isHidden ?: false,
                        mood = existing?.mood ?: "",
                        rating = existing?.rating ?: 0
                    )
                }.also { runCatching { retriever.release() } }.getOrNull()

                if (parsed != null) results.add(parsed)
            }
        }

        includedFolderUris.forEach { uriStr ->
            runCatching {
                val treeUri = Uri.parse(uriStr)
                val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
                val rootName = treeDocId.substringAfter(':', treeDocId).substringAfterLast('/').ifBlank { "Custom" }
                walkTree(treeUri, treeDocId, "/Added Folders/$rootName", depth = 0)
            }
        }
        results
    }
}
