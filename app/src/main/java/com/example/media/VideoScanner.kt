package com.example.media

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.LruCache
import android.util.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/**
 * Play-Policy-compliant automatic video discovery, persistent metadata cache,
 * and asynchronous LRU-cached thumbnail engine (zero READ_MEDIA_VIDEO permission).
 */
object VideoScanner {

    private const val PREFS_NAME = "mika_video_sources"
    private const val KEY_FILE_URIS = "picked_video_uris"
    private const val KEY_TREE_URIS = "picked_video_tree_uris"
    private const val KEY_META_CACHE_JSON = "video_meta_index_v1"
    private const val THUMB_DIR = "video_thumbs"
    private const val MAX_SAF_DEPTH = 7
    private const val MAX_DIR_DEPTH = 5

    val videoExtensions = setOf(
        "mp4", "mkv", "webm", "mov", "avi", "flv", "3gp", "3g2",
        "ts", "m2ts", "mts", "m4v", "wmv", "mpg", "mpeg", "ogv", "vob"
    )

    private val thumbMemoryCache = object : LruCache<String, ImageBitmap>(56) {}
    private val thumbSemaphore = Semaphore(3)
    private val probeSemaphore = Semaphore(4)

    private data class CachedVideoMeta(
        val title: String,
        val durationMs: Long,
        val width: Int,
        val height: Int,
        val sizeBytes: Long,
        val mimeType: String,
        val folderName: String,
        val dateAddedSec: Long,
        val lastModifiedSec: Long
    )

    private fun thumbDir(context: Context): File {
        val dir = File(context.cacheDir, THUMB_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun expectedThumbFile(context: Context, uriStr: String): File {
        val hash = abs(uriStr.hashCode()).toString(16)
        return File(thumbDir(context), "vthumb_$hash.jpg")
    }

    fun suggestedVideosInitialTreeUri(): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching {
                DocumentsContract.buildDocumentUri(
                    "com.android.externalstorage.documents",
                    "primary:Movies"
                )
            }.getOrNull()
        } else {
            null
        }
    }

    fun persistPickedVideoUris(context: Context, uris: List<Uri>) {
        if (uris.isEmpty()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getStringSet(KEY_FILE_URIS, emptySet())?.toMutableSet() ?: mutableSetOf()
        uris.forEach { uri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            existing.add(uri.toString())
        }
        prefs.edit().putStringSet(KEY_FILE_URIS, existing).apply()
    }

    fun persistVideoFolderTreeUri(context: Context, treeUri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getStringSet(KEY_TREE_URIS, emptySet())?.toMutableSet() ?: mutableSetOf()
        existing.add(treeUri.toString())
        prefs.edit().putStringSet(KEY_TREE_URIS, existing).apply()
    }

    fun removePersistedVideoUri(context: Context, uriString: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val files = prefs.getStringSet(KEY_FILE_URIS, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (files.remove(uriString)) {
            prefs.edit().putStringSet(KEY_FILE_URIS, files).apply()
        }
        thumbMemoryCache.remove(uriString)
    }

    fun clearAllPersistedVideoSources(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_FILE_URIS)
            .remove(KEY_TREE_URIS)
            .remove(KEY_META_CACHE_JSON)
            .apply()
        thumbMemoryCache.evictAll()
    }

    fun clearCache(context: Context): Long {
        thumbMemoryCache.evictAll()
        var freed = 0L
        thumbDir(context).listFiles()?.forEach {
            freed += it.length()
            it.delete()
        }
        return freed
    }

    fun getCacheSizeBytes(context: Context): Long {
        return thumbDir(context).listFiles()?.sumOf { it.length() } ?: 0L
    }

    /**
     * Automatically discovers videos from:
     * 1) MediaStore.Video.Media + MediaStore.Files (across API versions where accessible, zero READ_MEDIA_VIDEO)
     * 2) All persisted SAF trees (video trees + audio included trees + ContentResolver persistedUriPermissions)
     * 3) Readable public & app-external video directories (Movies, DCIM, Download, Pictures, Video, etc.)
     * 4) Persisted individual picked document URIs
     *
     * Metadata is cached by URI+size+mtime so subsequent refreshes complete instantaneously,
     * and thumbnail decoding/extraction is deferred until cards actually scroll into view.
     */
    suspend fun loadAllVideos(
        context: Context,
        extraTreeUris: Set<String> = emptySet(),
        obeyNoMedia: Boolean = true,
        excludedFolders: Set<String> = emptySet(),
        extensionBlacklist: Set<String> = emptySet()
    ): List<VideoItem> = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val metaCache = loadMetaCache(prefs)
        val collected = ConcurrentHashMap<String, VideoItem>()
        val dedupSignatures = ConcurrentHashMap.newKeySet<String>()
        val normalizedBlacklist = extensionBlacklist
            .map { it.trim().trimStart('.').lowercase() }
            .filter { it.isNotEmpty() }
            .toSet()
        val excludedFolderNames = excludedFolders.map {
            it.trimEnd('/').substringAfterLast('/').lowercase()
        }.filter { it.isNotBlank() }.toSet()

        fun registerVideo(item: VideoItem) {
            val titleExt = item.title.substringAfterLast('.', "").lowercase()
            val uriExt = item.contentUri.substringAfterLast('.', "").substringBefore('?').lowercase()
            if ((titleExt.isNotEmpty() && titleExt in normalizedBlacklist) ||
                (uriExt.isNotEmpty() && uriExt in normalizedBlacklist)
            ) {
                return
            }
            if (item.folderName.lowercase() in excludedFolderNames) {
                return
            }
            val sig = buildDedupSignature(item.title, item.sizeBytes, item.durationMs)
            if (sig != null && !dedupSignatures.add(sig)) {
                val existing = collected.values.firstOrNull {
                    buildDedupSignature(it.title, it.sizeBytes, it.durationMs) == sig
                }
                if (existing != null && (existing.durationMs >= item.durationMs && existing.height >= item.height)) {
                    return
                }
            }
            collected[item.contentUri] = item
        }

        // Source A: MediaStore (instant cursor read)
        queryMediaStoreVideos(context).forEach(::registerVideo)
        queryMediaStoreFilesForVideos(context, collected.keys).forEach(::registerVideo)

        // Source B: All readable public / app storage directories
        val fileCandidates = scanReadablePublicVideoFiles(
            context = context,
            obeyNoMedia = obeyNoMedia,
            excludedFolders = excludedFolders,
            blacklist = normalizedBlacklist
        )
        if (fileCandidates.isNotEmpty()) {
            coroutineScope {
                fileCandidates.map { candidate ->
                    async(Dispatchers.IO) {
                        probeSemaphore.withPermit {
                            resolveFileCandidate(context, candidate, metaCache)
                        }
                    }
                }.awaitAll().filterNotNull().forEach(::registerVideo)
            }
        }

        // Source C: All granted SAF trees (saved video trees + library included trees + OS persisted permissions)
        val allTreeUriStrings = LinkedHashSet<String>()
        allTreeUriStrings.addAll(prefs.getStringSet(KEY_TREE_URIS, emptySet()).orEmpty())
        allTreeUriStrings.addAll(extraTreeUris)
        runCatching {
            context.contentResolver.persistedUriPermissions
                .filter { it.isReadPermission && DocumentsContract.isTreeUri(it.uri) }
                .forEach { allTreeUriStrings.add(it.uri.toString()) }
        }

        val safCandidates = CollectionsSafCollector()
        for (treeStr in allTreeUriStrings) {
            val treeUri = runCatching { Uri.parse(treeStr) }.getOrNull() ?: continue
            val rootDocId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull() ?: continue
            val folderTitle = rootDocId.substringAfterLast(':').substringAfterLast('/')
                .ifBlank { "Videos" }
            walkTreeCandidates(
                context = context,
                treeUri = treeUri,
                parentDocId = rootDocId,
                folderName = folderTitle,
                visitedDocIds = HashSet(),
                out = safCandidates,
                depth = 0,
                obeyNoMedia = obeyNoMedia,
                excludedFolderNames = excludedFolderNames,
                blacklist = normalizedBlacklist
            )
        }

        if (safCandidates.items.isNotEmpty()) {
            coroutineScope {
                safCandidates.items.map { candidate ->
                    async(Dispatchers.IO) {
                        if (collected.containsKey(candidate.uri.toString())) return@async null
                        probeSemaphore.withPermit {
                            inspectCandidateFast(
                                context = context,
                                uri = candidate.uri,
                                folderName = candidate.folderName,
                                displayName = candidate.displayName,
                                sizeBytes = candidate.sizeBytes,
                                lastModSec = candidate.lastModSec,
                                mimeType = candidate.mimeType,
                                metaCache = metaCache
                            )
                        }
                    }
                }.awaitAll().filterNotNull().forEach(::registerVideo)
            }
        }

        // Source D: Individually picked video URIs (fallback / Photo Picker)
        val savedFiles = prefs.getStringSet(KEY_FILE_URIS, emptySet()).orEmpty()
        if (savedFiles.isNotEmpty()) {
            coroutineScope {
                savedFiles.mapNotNull { runCatching { Uri.parse(it) }.getOrNull() }
                    .filter { !collected.containsKey(it.toString()) }
                    .map { uri ->
                        async(Dispatchers.IO) {
                            probeSemaphore.withPermit {
                                inspectVideoUriWithCache(context, uri, folderName = "Selected Videos", metaCache = metaCache)
                            }
                        }
                    }
                    .awaitAll()
                    .filterNotNull()
                    .forEach(::registerVideo)
            }
        }

        saveMetaCache(prefs, metaCache)
        collected.values.toList()
    }

    private fun buildDedupSignature(title: String, sizeBytes: Long, durationMs: Long): String? {
        if (sizeBytes <= 0L) return null
        val normTitle = title.trim().lowercase()
        val durBucket = if (durationMs > 0L) (durationMs / 1000L) else -1L
        return "$normTitle|$sizeBytes|$durBucket"
    }

    private data class SafVideoCandidate(
        val uri: Uri,
        val folderName: String,
        val displayName: String,
        val sizeBytes: Long,
        val lastModSec: Long,
        val mimeType: String
    )

    private class CollectionsSafCollector {
        val items = ArrayList<SafVideoCandidate>()
        val seen = HashSet<String>()
    }

    private fun walkTreeCandidates(
        context: Context,
        treeUri: Uri,
        parentDocId: String,
        folderName: String,
        visitedDocIds: MutableSet<String>,
        out: CollectionsSafCollector,
        depth: Int,
        obeyNoMedia: Boolean = true,
        excludedFolderNames: Set<String> = emptySet(),
        blacklist: Set<String> = emptySet()
    ) {
        if (depth > MAX_SAF_DEPTH || !visitedDocIds.add(parentDocId)) return
        if (folderName.lowercase() in excludedFolderNames) return
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )
        val childDirs = ArrayList<Pair<String, String>>()
        val childVideos = ArrayList<SafVideoCandidate>()
        var hasNoMedia = false

        runCatching {
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                val modCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                while (cursor.moveToNext()) {
                    val docId = cursor.getString(idCol) ?: continue
                    val name = if (nameCol >= 0) cursor.getString(nameCol) ?: "" else ""
                    if (name.equals(".nomedia", ignoreCase = true)) {
                        hasNoMedia = true
                        if (obeyNoMedia) return@use
                        continue
                    }
                    if (name.startsWith(".")) continue
                    val mime = if (mimeCol >= 0) cursor.getString(mimeCol) ?: "" else ""
                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        if (name.equals("Android", ignoreCase = true) || name.equals("cache", ignoreCase = true)) {
                            continue
                        }
                        val subFolder = name.ifBlank { folderName }
                        childDirs.add(docId to subFolder)
                    } else {
                        val ext = name.substringAfterLast('.', "").lowercase()
                        if (ext.isNotEmpty() && ext in blacklist) continue
                        if (mime.startsWith("video/") || ext in videoExtensions) {
                            val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                            val uriStr = docUri.toString()
                            if (out.seen.add(uriStr)) {
                                val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                                val modSec = if (modCol >= 0) cursor.getLong(modCol) / 1000L else (System.currentTimeMillis() / 1000L)
                                childVideos.add(
                                    SafVideoCandidate(
                                        uri = docUri,
                                        folderName = folderName,
                                        displayName = name,
                                        sizeBytes = size,
                                        lastModSec = modSec,
                                        mimeType = mime.ifBlank { "video/*" }
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
        if (obeyNoMedia && hasNoMedia) return
        out.items.addAll(childVideos)
        for ((subDocId, subFolder) in childDirs) {
            walkTreeCandidates(
                context = context,
                treeUri = treeUri,
                parentDocId = subDocId,
                folderName = subFolder,
                visitedDocIds = visitedDocIds,
                out = out,
                depth = depth + 1,
                obeyNoMedia = obeyNoMedia,
                excludedFolderNames = excludedFolderNames,
                blacklist = blacklist
            )
        }
    }

    private fun queryMediaStoreVideos(context: Context): List<VideoItem> {
        val result = ArrayList<VideoItem>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        val projection = buildList {
            add(MediaStore.Video.Media._ID)
            add(MediaStore.Video.Media.TITLE)
            add(MediaStore.Video.Media.DISPLAY_NAME)
            add(MediaStore.Video.Media.DURATION)
            add(MediaStore.Video.Media.WIDTH)
            add(MediaStore.Video.Media.HEIGHT)
            add(MediaStore.Video.Media.SIZE)
            add(MediaStore.Video.Media.MIME_TYPE)
            add(MediaStore.Video.Media.DATE_ADDED)
            add(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
        }.toTypedArray()

        runCatching {
            context.contentResolver.query(
                collection,
                projection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val titleCol = cursor.getColumnIndex(MediaStore.Video.Media.TITLE)
                val nameCol = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                val durCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                val widthCol = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)
                val sizeCol = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
                val mimeCol = cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)
                val addedCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)
                val bucketCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    val uriStr = uri.toString()
                    val displayName = if (nameCol >= 0) cursor.getString(nameCol) ?: "" else ""
                    val rawTitle = if (titleCol >= 0) cursor.getString(titleCol) ?: "" else ""
                    val title = rawTitle.ifBlank { displayName.substringBeforeLast('.') }.ifBlank { "Video" }
                    val dur = if (durCol >= 0) cursor.getLong(durCol) else 0L
                    val sz = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                    if (dur <= 0L && sz <= 0L) continue
                    val w = if (widthCol >= 0) cursor.getInt(widthCol) else 0
                    val h = if (heightCol >= 0) cursor.getInt(heightCol) else 0
                    val mime = if (mimeCol >= 0) cursor.getString(mimeCol) ?: "video/*" else "video/*"
                    val added = if (addedCol >= 0) cursor.getLong(addedCol) else 0L
                    val bucket = if (bucketCol >= 0) cursor.getString(bucketCol)?.ifBlank { "Videos" } ?: "Videos" else "Videos"
                    val cachedThumb = expectedThumbFile(context, uriStr).takeIf { it.exists() && it.length() > 0L }

                    result.add(
                        VideoItem(
                            id = uriStr,
                            contentUri = uriStr,
                            title = title,
                            durationMs = dur,
                            width = w,
                            height = h,
                            sizeBytes = sz,
                            mimeType = mime,
                            folderName = bucket,
                            dateAddedSec = added,
                            thumbnailPath = cachedThumb?.absolutePath
                        )
                    )
                }
            }
        }
        return result
    }

    private fun queryMediaStoreFilesForVideos(
        context: Context,
        alreadyCollectedUris: Set<String>
    ): List<VideoItem> {
        val result = ArrayList<VideoItem>()
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.TITLE,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.MediaColumns.DURATION,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME
        )
        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO}"

        runCatching {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val titleCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.TITLE)
                val nameCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
                val mimeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)
                val addedCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_ADDED)
                val durCol = cursor.getColumnIndex(MediaStore.MediaColumns.DURATION)
                val widthCol = cursor.getColumnIndex(MediaStore.MediaColumns.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.MediaColumns.HEIGHT)
                val bucketCol = cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    val uriStr = uri.toString()
                    if (uriStr in alreadyCollectedUris) continue

                    val displayName = if (nameCol >= 0) cursor.getString(nameCol) ?: "" else ""
                    val rawTitle = if (titleCol >= 0) cursor.getString(titleCol) ?: "" else ""
                    val title = rawTitle.ifBlank { displayName.substringBeforeLast('.') }.ifBlank { "Video" }
                    val sz = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                    val dur = if (durCol >= 0) cursor.getLong(durCol) else 0L
                    if (sz <= 0L && dur <= 0L) continue
                    val w = if (widthCol >= 0) cursor.getInt(widthCol) else 0
                    val h = if (heightCol >= 0) cursor.getInt(heightCol) else 0
                    val mime = if (mimeCol >= 0) cursor.getString(mimeCol) ?: "video/*" else "video/*"
                    val added = if (addedCol >= 0) cursor.getLong(addedCol) else 0L
                    val bucket = if (bucketCol >= 0) cursor.getString(bucketCol)?.ifBlank { "Videos" } ?: "Videos" else "Videos"
                    val cachedThumb = expectedThumbFile(context, uriStr).takeIf { it.exists() && it.length() > 0L }

                    result.add(
                        VideoItem(
                            id = uriStr,
                            contentUri = uriStr,
                            title = title,
                            durationMs = dur,
                            width = w,
                            height = h,
                            sizeBytes = sz,
                            mimeType = mime,
                            folderName = bucket,
                            dateAddedSec = added,
                            thumbnailPath = cachedThumb?.absolutePath
                        )
                    )
                }
            }
        }
        return result
    }

    private fun scanReadablePublicVideoFiles(
        context: Context,
        obeyNoMedia: Boolean = true,
        excludedFolders: Set<String> = emptySet(),
        blacklist: Set<String> = emptySet()
    ): List<File> {
        val candidateRoots = LinkedHashSet<File>()
        runCatching {
            @Suppress("DEPRECATION")
            val extRoot = Environment.getExternalStorageDirectory()
            if (extRoot != null) {
                listOf("Movies", "DCIM", "Download", "Downloads", "Pictures", "Video", "Videos", "Music")
                    .forEach { candidateRoots.add(File(extRoot, it)) }
            }
        }
        runCatching {
            @Suppress("DEPRECATION")
            candidateRoots.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES))
            @Suppress("DEPRECATION")
            candidateRoots.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM))
            @Suppress("DEPRECATION")
            candidateRoots.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
        }
        runCatching {
            context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)?.let { candidateRoots.add(it) }
            @Suppress("DEPRECATION")
            context.externalMediaDirs.filterNotNull().forEach { candidateRoots.add(it) }
        }

        val found = ArrayList<File>()
        val seenCanonical = HashSet<String>()

        fun walkFileDir(dir: File, depth: Int) {
            if (depth > MAX_DIR_DEPTH || !dir.exists() || !dir.isDirectory || !dir.canRead()) return
            if (dir.absolutePath in excludedFolders) return
            if (obeyNoMedia && File(dir, ".nomedia").exists()) return
            val canonical = runCatching { dir.canonicalPath }.getOrDefault(dir.absolutePath)
            if (!seenCanonical.add(canonical)) return
            val children = dir.listFiles() ?: return
            for (child in children) {
                val name = child.name
                if (name.startsWith(".")) continue
                if (child.isDirectory) {
                    if (name.equals("Android", ignoreCase = true) || name.equals("cache", ignoreCase = true)) {
                        continue
                    }
                    walkFileDir(child, depth + 1)
                } else if (child.isFile && child.canRead() && child.length() > 0L) {
                    val ext = name.substringAfterLast('.', "").lowercase()
                    if (ext in videoExtensions && ext !in blacklist) {
                        found.add(child)
                    }
                }
            }
        }

        candidateRoots.forEach { walkFileDir(it, depth = 0) }
        return found
    }

    private fun resolveFileCandidate(
        context: Context,
        file: File,
        metaCache: MutableMap<String, CachedVideoMeta>
    ): VideoItem? {
        val fileUri = Uri.fromFile(file)
        val uriStr = fileUri.toString()
        val sizeBytes = file.length()
        val lastModSec = (file.lastModified() / 1000L).coerceAtLeast(0L)
        val folderName = file.parentFile?.name?.ifBlank { "Videos" } ?: "Videos"
        val fallbackTitle = file.nameWithoutExtension.ifBlank { file.name }
        val cachedThumb = expectedThumbFile(context, uriStr).takeIf { it.exists() && it.length() > 0L }

        val cached = metaCache[uriStr]
        if (cached != null && cached.sizeBytes == sizeBytes && cached.lastModifiedSec == lastModSec) {
            return VideoItem(
                id = uriStr,
                contentUri = uriStr,
                title = cached.title.ifBlank { fallbackTitle },
                durationMs = cached.durationMs,
                width = cached.width,
                height = cached.height,
                sizeBytes = sizeBytes,
                mimeType = cached.mimeType,
                folderName = cached.folderName.ifBlank { folderName },
                dateAddedSec = cached.dateAddedSec,
                thumbnailPath = cachedThumb?.absolutePath
            )
        }

        val retriever = MediaMetadataRetriever()
        var durationMs = 0L
        var width = 0
        var height = 0
        var mime = "video/*"
        var metaTitle = fallbackTitle
        runCatching {
            retriever.setDataSource(file.absolutePath)
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                ?.takeIf { it.isNotBlank() }?.let { mime = it }
        }
        runCatching { retriever.release() }

        metaCache[uriStr] = CachedVideoMeta(
            title = metaTitle,
            durationMs = durationMs,
            width = width,
            height = height,
            sizeBytes = sizeBytes,
            mimeType = mime,
            folderName = folderName,
            dateAddedSec = lastModSec,
            lastModifiedSec = lastModSec
        )

        return VideoItem(
            id = uriStr,
            contentUri = uriStr,
            title = metaTitle,
            durationMs = durationMs,
            width = width,
            height = height,
            sizeBytes = sizeBytes,
            mimeType = mime,
            folderName = folderName,
            dateAddedSec = lastModSec,
            thumbnailPath = cachedThumb?.absolutePath
        )
    }

    private fun inspectCandidateFast(
        context: Context,
        uri: Uri,
        folderName: String,
        displayName: String,
        sizeBytes: Long,
        lastModSec: Long,
        mimeType: String,
        metaCache: MutableMap<String, CachedVideoMeta>
    ): VideoItem {
        val uriStr = uri.toString()
        val fallbackTitle = displayName.substringBeforeLast('.').ifBlank { "Video" }
        val cachedThumb = expectedThumbFile(context, uriStr).takeIf { it.exists() && it.length() > 0L }

        val cached = metaCache[uriStr]
        if (cached != null && (sizeBytes <= 0L || cached.sizeBytes == sizeBytes) && cached.lastModifiedSec == lastModSec) {
            return VideoItem(
                id = uriStr,
                contentUri = uriStr,
                title = cached.title.ifBlank { fallbackTitle },
                durationMs = cached.durationMs,
                width = cached.width,
                height = cached.height,
                sizeBytes = if (sizeBytes > 0L) sizeBytes else cached.sizeBytes,
                mimeType = cached.mimeType,
                folderName = folderName,
                dateAddedSec = lastModSec,
                thumbnailPath = cachedThumb?.absolutePath
            )
        }

        val retriever = MediaMetadataRetriever()
        var durationMs = 0L
        var width = 0
        var height = 0
        var mime = mimeType
        var metaTitle = fallbackTitle
        runCatching {
            retriever.setDataSource(context, uri)
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                ?.takeIf { it.isNotBlank() }?.let { mime = it }
        }
        runCatching { retriever.release() }

        metaCache[uriStr] = CachedVideoMeta(
            title = metaTitle,
            durationMs = durationMs,
            width = width,
            height = height,
            sizeBytes = sizeBytes,
            mimeType = mime,
            folderName = folderName,
            dateAddedSec = lastModSec,
            lastModifiedSec = lastModSec
        )

        return VideoItem(
            id = uriStr,
            contentUri = uriStr,
            title = metaTitle,
            durationMs = durationMs,
            width = width,
            height = height,
            sizeBytes = sizeBytes,
            mimeType = mime,
            folderName = folderName,
            dateAddedSec = lastModSec,
            thumbnailPath = cachedThumb?.absolutePath
        )
    }

    suspend fun inspectVideoUri(
        context: Context,
        uri: Uri,
        folderName: String = "Selected Videos",
        fallbackTitle: String? = null,
        fallbackSize: Long = 0L,
        fallbackDateSec: Long = System.currentTimeMillis() / 1000L,
        fallbackMime: String = "video/*"
    ): VideoItem? = inspectVideoUriWithCache(
        context = context,
        uri = uri,
        folderName = folderName,
        fallbackTitle = fallbackTitle,
        fallbackSize = fallbackSize,
        fallbackDateSec = fallbackDateSec,
        fallbackMime = fallbackMime,
        metaCache = null
    )

    private suspend fun inspectVideoUriWithCache(
        context: Context,
        uri: Uri,
        folderName: String = "Selected Videos",
        fallbackTitle: String? = null,
        fallbackSize: Long = 0L,
        fallbackDateSec: Long = System.currentTimeMillis() / 1000L,
        fallbackMime: String = "video/*",
        metaCache: MutableMap<String, CachedVideoMeta>? = null
    ): VideoItem? = withContext(Dispatchers.IO) {
        val uriStr = uri.toString()
        var resolvedName = fallbackTitle ?: ""
        var resolvedSize = fallbackSize

        runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null
            )?.use { c ->
                if (c.moveToFirst()) {
                    val nIdx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sIdx = c.getColumnIndex(OpenableColumns.SIZE)
                    if (nIdx >= 0) {
                        val disp = c.getString(nIdx)
                        if (!disp.isNullOrBlank()) resolvedName = disp.substringBeforeLast('.')
                    }
                    if (sIdx >= 0 && resolvedSize <= 0L) {
                        resolvedSize = c.getLong(sIdx)
                    }
                }
            }
        }

        val cached = metaCache?.get(uriStr)
        val cachedThumb = expectedThumbFile(context, uriStr).takeIf { it.exists() && it.length() > 0L }
        if (cached != null && (resolvedSize <= 0L || cached.sizeBytes == resolvedSize)) {
            return@withContext VideoItem(
                id = uriStr,
                contentUri = uriStr,
                title = resolvedName.ifBlank { cached.title }.ifBlank { "Video" },
                durationMs = cached.durationMs,
                width = cached.width,
                height = cached.height,
                sizeBytes = if (resolvedSize > 0L) resolvedSize else cached.sizeBytes,
                mimeType = cached.mimeType,
                folderName = folderName,
                dateAddedSec = fallbackDateSec,
                thumbnailPath = cachedThumb?.absolutePath
            )
        }

        val retriever = MediaMetadataRetriever()
        var durationMs = 0L
        var width = 0
        var height = 0
        var mime = fallbackMime
        var metaTitle = resolvedName

        runCatching {
            retriever.setDataSource(context, uri)
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull() ?: 0
            height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull() ?: 0
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                ?.takeIf { it.isNotBlank() }?.let { mime = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() && metaTitle.isBlank() }?.let { metaTitle = it }
        }
        runCatching { retriever.release() }

        val finalTitle = metaTitle.ifBlank {
            uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.') ?: "Video"
        }

        metaCache?.put(
            uriStr,
            CachedVideoMeta(
                title = finalTitle,
                durationMs = durationMs,
                width = width,
                height = height,
                sizeBytes = resolvedSize,
                mimeType = mime,
                folderName = folderName,
                dateAddedSec = fallbackDateSec,
                lastModifiedSec = fallbackDateSec
            )
        )

        VideoItem(
            id = uriStr,
            contentUri = uriStr,
            title = finalTitle,
            durationMs = durationMs,
            width = width,
            height = height,
            sizeBytes = resolvedSize,
            mimeType = mime,
            folderName = folderName,
            dateAddedSec = fallbackDateSec,
            thumbnailPath = cachedThumb?.absolutePath
        )
    }

    /**
     * Fast in-memory LRU check for smooth scroll without flickering.
     */
    fun getCachedMemoryThumbnail(uriStr: String): ImageBitmap? = thumbMemoryCache.get(uriStr)

    /**
     * Lazy, bounded, LRU + disk cached video thumbnail loader.
     * Uses Android Q+ hardware ContentResolver.loadThumbnail first, then MediaMetadataRetriever fallback.
     */
    suspend fun getOrLoadThumbnail(context: Context, video: VideoItem): ImageBitmap? {
        thumbMemoryCache.get(video.contentUri)?.let { return it }

        return withContext(Dispatchers.IO) {
            thumbMemoryCache.get(video.contentUri)?.let { return@withContext it }

            val diskFile = video.thumbnailPath?.let(::File)
                ?.takeIf { it.exists() && it.length() > 0L }
                ?: expectedThumbFile(context, video.contentUri).takeIf { it.exists() && it.length() > 0L }

            if (diskFile != null) {
                val decoded = runCatching {
                    BitmapFactory.decodeFile(diskFile.absolutePath)?.asImageBitmap()
                }.getOrNull()
                if (decoded != null) {
                    thumbMemoryCache.put(video.contentUri, decoded)
                    return@withContext decoded
                }
            }

            thumbSemaphore.withPermit {
                thumbMemoryCache.get(video.contentUri)?.let { return@withPermit it }
                val uri = runCatching { Uri.parse(video.contentUri) }.getOrNull() ?: return@withPermit null

                var bitmap: Bitmap? = null
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && uri.scheme == "content") {
                    bitmap = runCatching {
                        context.contentResolver.loadThumbnail(uri, Size(480, 270), null)
                    }.getOrNull()
                }

                if (bitmap == null) {
                    val retriever = MediaMetadataRetriever()
                    bitmap = try {
                        if (uri.scheme == "file" && uri.path != null) {
                            retriever.setDataSource(uri.path)
                        } else {
                            retriever.setDataSource(context, uri)
                        }
                        retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                            ?: retriever.frameAtTime
                    } catch (_: Throwable) {
                        null
                    } finally {
                        runCatching { retriever.release() }
                    }
                }

                val rawBmp = bitmap ?: return@withPermit null
                val targetW = 480
                val scale = targetW.toFloat() / rawBmp.width.coerceAtLeast(1)
                val scaled = if (scale < 1f) {
                    Bitmap.createScaledBitmap(
                        rawBmp,
                        targetW,
                        (rawBmp.height * scale).toInt().coerceAtLeast(160),
                        true
                    )
                } else rawBmp

                val outFile = expectedThumbFile(context, video.contentUri)
                runCatching {
                    FileOutputStream(outFile).use { fos ->
                        scaled.compress(Bitmap.CompressFormat.JPEG, 82, fos)
                    }
                }

                val imageBitmap = scaled.asImageBitmap()
                thumbMemoryCache.put(video.contentUri, imageBitmap)
                if (scaled !== rawBmp) rawBmp.recycle()
                imageBitmap
            }
        }
    }

    private fun loadMetaCache(prefs: android.content.SharedPreferences): ConcurrentHashMap<String, CachedVideoMeta> {
        val map = ConcurrentHashMap<String, CachedVideoMeta>()
        val raw = prefs.getString(KEY_META_CACHE_JSON, null) ?: return map
        runCatching {
            val root = JSONObject(raw)
            val keys = root.keys()
            while (keys.hasNext()) {
                val uriStr = keys.next()
                val obj = root.optJSONObject(uriStr) ?: continue
                map[uriStr] = CachedVideoMeta(
                    title = obj.optString("t", ""),
                    durationMs = obj.optLong("d", 0L),
                    width = obj.optInt("w", 0),
                    height = obj.optInt("h", 0),
                    sizeBytes = obj.optLong("s", 0L),
                    mimeType = obj.optString("m", "video/*"),
                    folderName = obj.optString("f", "Videos"),
                    dateAddedSec = obj.optLong("a", 0L),
                    lastModifiedSec = obj.optLong("lm", 0L)
                )
            }
        }
        return map
    }

    private fun saveMetaCache(
        prefs: android.content.SharedPreferences,
        cache: Map<String, CachedVideoMeta>
    ) {
        runCatching {
            val root = JSONObject()
            cache.entries.take(1500).forEach { (uriStr, meta) ->
                val obj = JSONObject().apply {
                    put("t", meta.title)
                    put("d", meta.durationMs)
                    put("w", meta.width)
                    put("h", meta.height)
                    put("s", meta.sizeBytes)
                    put("m", meta.mimeType)
                    put("f", meta.folderName)
                    put("a", meta.dateAddedSec)
                    put("lm", meta.lastModifiedSec)
                }
                root.put(uriStr, obj)
            }
            prefs.edit().putString(KEY_META_CACHE_JSON, root.toString()).apply()
        }
    }
}
