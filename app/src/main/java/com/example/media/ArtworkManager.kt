package com.example.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.drawable.toDrawable
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.decode.DataSource
import coil.disk.DiskCache
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.key.Keyer
import coil.memory.MemoryCache
import coil.request.Options
import coil.size.Dimension
import com.example.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore

data class TrackArtworkRequest(
    val songId: Long,
    val contentUri: String,
    val filePath: String,
    val dateModifiedSec: Long,
    val title: String,
    val artist: String,
    val albumId: Long = -1L
)

fun Song.toArtworkRequest(): TrackArtworkRequest = TrackArtworkRequest(
    songId = id,
    contentUri = contentUri,
    filePath = filePath,
    dateModifiedSec = dateModifiedSec,
    title = title,
    artist = artist,
    albumId = albumId
)

data class ArtworkDetails(
    val width: Int,
    val height: Int,
    val format: String,
    val sizeBytes: Long,
    val sourceLabel: String,
    val dominantHex: String,
    val secondaryHex: String
)

object ArtworkManager {

    private const val CACHE_DIR_NAME = "track_artwork_cache"
    private const val CUSTOM_ART_DIR_NAME = "custom_artwork"
    private val extractionSemaphore = Semaphore(4)
    private val trackMutexes = ConcurrentHashMap<Long, Mutex>()
    private val paletteCache = LruCache<String, Pair<Color, Color>>(48)
    private val brightPaletteCache = LruCache<String, Pair<Color, Color>>(48)
    private val cacheWriteScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    var groupByAlbumEnabled: Boolean = false

    fun cacheDir(context: Context): File {
        val dir = File(context.cacheDir, CACHE_DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun customArtworkDir(context: Context): File {
        val dir = File(context.filesDir, CUSTOM_ART_DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getCustomArtworkFile(context: Context, songId: Long): File {
        return File(customArtworkDir(context), "cover_${songId}.jpg")
    }

    fun getCachedFileForTrack(context: Context, songId: Long, dateModifiedSec: Long): File {
        return File(cacheDir(context), "track_${songId}_${dateModifiedSec}.jpg")
    }

    fun getCachedFileForAlbum(context: Context, albumId: Long): File {
        return File(cacheDir(context), "album_${albumId}.jpg")
    }

    fun buildImageLoader(context: Context): ImageLoader {
        val appContext = context.applicationContext
        return ImageLoader.Builder(appContext)
            .memoryCache {
                MemoryCache.Builder(appContext)
                    .maxSizePercent(0.22)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(appContext.cacheDir, "coil_image_cache"))
                    .maxSizeBytes(64L * 1024L * 1024L)
                    .build()
            }
            .components {
                add(TrackArtworkKeyer())
                add(TrackArtworkFetcher.Factory(appContext))
            }
            .allowRgb565(true)
            .respectCacheHeaders(false)
            .build()
    }

    suspend fun getOrExtractBitmap(
        context: Context,
        song: Song,
        maxDimension: Int = 600,
        groupByAlbum: Boolean = groupByAlbumEnabled
    ): Bitmap? = withContext(Dispatchers.IO) {
        getOrExtractBitmapLocked(context, song.toArtworkRequest(), maxDimension, groupByAlbum)
    }

    suspend fun getOrExtractBitmapLocked(
        context: Context,
        req: TrackArtworkRequest,
        maxDimension: Int = 600,
        groupByAlbum: Boolean = groupByAlbumEnabled
    ): Bitmap? {
        // Fast path: custom per-track cover always wins, then album cover if enabled, then per-track cache
        val customFile = getCustomArtworkFile(context, req.songId)
        if (!customFile.exists() && groupByAlbum && req.albumId > 0L) {
            val albumFile = getCachedFileForAlbum(context, req.albumId)
            if (albumFile.exists() && albumFile.length() > 0L) {
                decodeFileSampled(albumFile.absolutePath, maxDimension)?.let { return it }
            }
        }
        val fastCacheFile = getCachedFileForTrack(context, req.songId, req.dateModifiedSec)
        if (fastCacheFile.exists() && fastCacheFile.length() > 0L) {
            decodeFileSampled(fastCacheFile.absolutePath, maxDimension)?.let { return it }
        }

        val mutex = trackMutexes.getOrPut(req.songId) { Mutex() }
        return mutex.withLock {
            extractionSemaphore.withPermit {
                getOrExtractBitmapInternal(context, req, maxDimension, groupByAlbum)
            }
        }
    }

    private fun getOrExtractBitmapInternal(
        context: Context,
        req: TrackArtworkRequest,
        maxDimension: Int,
        groupByAlbum: Boolean
    ): Bitmap? {
        val customFile = getCustomArtworkFile(context, req.songId)
        if (!customFile.exists() && groupByAlbum && req.albumId > 0L) {
            val albumFile = getCachedFileForAlbum(context, req.albumId)
            if (albumFile.exists() && albumFile.length() > 0L) {
                decodeFileSampled(albumFile.absolutePath, maxDimension)?.let { return it }
            }
        }

        val cacheFile = getCachedFileForTrack(context, req.songId, req.dateModifiedSec)
        if (cacheFile.exists() && cacheFile.length() > 0L) {
            val cached = decodeFileSampled(cacheFile.absolutePath, maxDimension)
            if (cached != null) return cached
        }

        val dir = cacheDir(context)
        dir.listFiles { _, name ->
            name.startsWith("track_${req.songId}_") && name != cacheFile.name
        }?.forEach { it.delete() }

        var isEmbeddedOrCustom = false
        val fullBitmap: Bitmap = if (customFile.exists() && customFile.length() > 0L) {
            isEmbeddedOrCustom = true
            decodeFileSampled(customFile.absolutePath, 600)
                ?: SongFallbackArtworkGenerator.generate(req, 320)
        } else {
            val embeddedBytes = extractEmbeddedPictureBytes(context, req.contentUri, req.filePath)
            if (embeddedBytes != null && embeddedBytes.isNotEmpty()) {
                isEmbeddedOrCustom = true
                decodeSampledBitmap(embeddedBytes, 512)
                    ?: SongFallbackArtworkGenerator.generate(req, 320)
            } else {
                SongFallbackArtworkGenerator.generate(req, 320)
            }
        }

        val returnedBitmap = if (maxDimension < fullBitmap.width && maxDimension in 64..480) {
            runCatching {
                Bitmap.createScaledBitmap(fullBitmap, maxDimension, maxDimension, true)
            }.getOrDefault(fullBitmap)
        } else {
            fullBitmap
        }

        cacheWriteScope.launch {
            runCatching {
                val tmp = File(dir, "${cacheFile.name}.tmp")
                FileOutputStream(tmp).use { out ->
                    fullBitmap.compress(Bitmap.CompressFormat.JPEG, 84, out)
                }
                if (!tmp.renameTo(cacheFile)) {
                    tmp.copyTo(cacheFile, overwrite = true)
                    tmp.delete()
                }
                if (groupByAlbum && req.albumId > 0L && isEmbeddedOrCustom && !customFile.exists()) {
                    val albumFile = getCachedFileForAlbum(context, req.albumId)
                    if (!albumFile.exists()) {
                        cacheFile.copyTo(albumFile, overwrite = true)
                    }
                }
            }
        }

        return returnedBitmap
    }

    fun extractEmbeddedPictureBytes(
        context: Context,
        contentUri: String,
        filePath: String
    ): ByteArray? {
        val retriever = MediaMetadataRetriever()
        return try {
            var setOk = false
            if (contentUri.isNotBlank()) {
                setOk = runCatching {
                    retriever.setDataSource(context, Uri.parse(contentUri))
                }.isSuccess
            }
            if (!setOk && filePath.isNotBlank() && File(filePath).exists()) {
                setOk = runCatching {
                    retriever.setDataSource(filePath)
                }.isSuccess
            }
            if (setOk) retriever.embeddedPicture else null
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun decodeFileSampled(path: String, maxDim: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sampleSize = 1
        while (bounds.outWidth / sampleSize > maxDim * 2 ||
            bounds.outHeight / sampleSize > maxDim * 2
        ) {
            sampleSize *= 2
        }
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        return BitmapFactory.decodeFile(path, opts)
    }

    private fun decodeSampledBitmap(bytes: ByteArray, maxDim: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sampleSize = 1
        while (bounds.outWidth / sampleSize > maxDim * 2 ||
            bounds.outHeight / sampleSize > maxDim * 2
        ) {
            sampleSize *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }

    data class ArtworkColorBundle(
        val deepPair: Pair<Color, Color>,
        val brightPair: Pair<Color, Color>
    )

    suspend fun extractArtworkColorBundle(context: Context, song: Song): ArtworkColorBundle? =
        withContext(Dispatchers.Default) {
            val cacheKey = "${song.id}_${song.dateModifiedSec}"
            val cachedDeep = paletteCache.get(cacheKey)
            val cachedBright = brightPaletteCache.get(cacheKey)
            if (cachedDeep != null && cachedBright != null) {
                return@withContext ArtworkColorBundle(cachedDeep, cachedBright)
            }

            val bitmap = getOrExtractBitmap(context, song, 128) ?: return@withContext null
            val seedArgb = extractBestChromaticSeedArgb(bitmap) ?: return@withContext null

            val deepResult = cachedDeep ?: deriveDeepPairFromSeed(seedArgb).also {
                paletteCache.put(cacheKey, it)
            }
            val brightResult = cachedBright ?: deriveBrightPairFromSeed(seedArgb).also {
                brightPaletteCache.put(cacheKey, it)
            }
            ArtworkColorBundle(deepResult, brightResult)
        }

    suspend fun extractArtworkColors(context: Context, song: Song): Pair<Color, Color>? =
        extractArtworkColorBundle(context, song)?.deepPair

    suspend fun extractBrightArtworkColors(context: Context, song: Song): Pair<Color, Color>? =
        extractArtworkColorBundle(context, song)?.brightPair

    private fun deriveDeepPairFromSeed(seedArgb: Int): Pair<Color, Color> {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(seedArgb, hsv)
        val hue = hsv[0]
        val sat = hsv[1].coerceIn(0.20f, 0.64f)
        val deepShade = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, (sat * 0.80f).coerceIn(0.18f, 0.56f), 0.14f)))
        val richShade = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, (sat * 0.90f).coerceIn(0.22f, 0.64f), 0.27f)))
        return deepShade to richShade
    }

    private fun deriveBrightPairFromSeed(seedArgb: Int): Pair<Color, Color> {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(seedArgb, hsv)
        val hue = hsv[0]
        val primarySat = hsv[1].coerceIn(0.26f, 0.68f)
        val primaryVal = hsv[2].coerceIn(0.46f, 0.84f)
        val primaryColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, primarySat, primaryVal)))
        val secondaryColor = Color(
            android.graphics.Color.HSVToColor(
                floatArrayOf(
                    hue,
                    (primarySat * 0.62f).coerceIn(0.18f, 0.48f),
                    (primaryVal * 1.10f).coerceIn(0.56f, 0.92f)
                )
            )
        )
        return primaryColor to secondaryColor
    }

    private fun extractBestChromaticSeedArgb(bitmap: Bitmap): Int? {
        val palette = Palette.from(bitmap)
            .maximumColorCount(24)
            .clearFilters()
            .generate()

        val hsv = FloatArray(3)
        var bestSwatchArgb: Int? = null
        var bestScore = Float.NEGATIVE_INFINITY

        for (swatch in palette.swatches) {
            android.graphics.Color.colorToHSV(swatch.rgb, hsv)
            val s = hsv[1]
            val v = hsv[2]
            // Penalize near-white, near-black, and muddy neutral grays
            if (v < 0.12f || (v > 0.95f && s < 0.12f)) continue
            val chromaFactor = if (s < 0.14f) s * 0.35f else (0.35f + s * 1.25f)
            val luminanceSweetSpot = 1f - abs(v - 0.56f).coerceIn(0f, 0.56f)
            val popScore = kotlin.math.sqrt(swatch.population.toFloat().coerceAtLeast(1f))
            val score = popScore * chromaFactor * (0.55f + 0.65f * luminanceSweetSpot)
            if (score > bestScore) {
                bestScore = score
                bestSwatchArgb = swatch.rgb
            }
        }
        if (bestSwatchArgb != null) return bestSwatchArgb

        // Fallback: direct 16x16 grid sampling when Palette finds only tiny or muted regions
        val stepX = (bitmap.width / 12).coerceAtLeast(1)
        val stepY = (bitmap.height / 12).coerceAtLeast(1)
        var fallbackArgb: Int? = null
        var fallbackScore = -1f
        for (y in stepY / 2 until bitmap.height step stepY) {
            for (x in stepX / 2 until bitmap.width step stepX) {
                val px = bitmap.getPixel(x, y)
                if (android.graphics.Color.alpha(px) < 128) continue
                android.graphics.Color.colorToHSV(px, hsv)
                val s = hsv[1]
                val v = hsv[2]
                if (v < 0.15f) continue
                val score = s * (1f - abs(v - 0.58f))
                if (score > fallbackScore) {
                    fallbackScore = score
                    fallbackArgb = px
                }
            }
        }
        return fallbackArgb
            ?: palette.vibrantSwatch?.rgb
            ?: palette.dominantSwatch?.rgb
            ?: palette.mutedSwatch?.rgb
    }

    suspend fun saveCustomArtworkFromUri(
        context: Context,
        songIds: List<Long>,
        uri: Uri
    ): Map<Long, String> = withContext(Dispatchers.IO) {
        if (songIds.isEmpty()) return@withContext emptyMap()
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return@withContext emptyMap()
        val decoded = decodeSampledBitmap(bytes, 720) ?: return@withContext emptyMap()
        val outMap = HashMap<Long, String>(songIds.size)
        val cache = cacheDir(context)
        for (songId in songIds) {
            val dest = getCustomArtworkFile(context, songId)
            val tmp = File(customArtworkDir(context), "cover_${songId}.tmp")
            val saved = runCatching {
                FileOutputStream(tmp).use { out ->
                    decoded.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                if (!tmp.renameTo(dest)) {
                    tmp.copyTo(dest, overwrite = true)
                    tmp.delete()
                }
                true
            }.getOrDefault(false)
            if (saved) {
                outMap[songId] = dest.absolutePath
                cache.listFiles { _, name -> name.startsWith("track_${songId}_") }?.forEach { it.delete() }
            }
        }
        paletteCache.evictAll()
        brightPaletteCache.evictAll()
        outMap
    }

    suspend fun clearCustomArtworkForSongs(context: Context, songIds: List<Long>) = withContext(Dispatchers.IO) {
        val cache = cacheDir(context)
        for (songId in songIds) {
            val file = getCustomArtworkFile(context, songId)
            if (file.exists()) file.delete()
            cache.listFiles { _, name -> name.startsWith("track_${songId}_") }?.forEach { it.delete() }
        }
        paletteCache.evictAll()
        brightPaletteCache.evictAll()
    }

    fun clearCache(context: Context): Long {
        paletteCache.evictAll()
        brightPaletteCache.evictAll()
        val dir = cacheDir(context)
        var freed = 0L
        dir.listFiles()?.forEach {
            freed += it.length()
            it.delete()
        }
        return freed
    }

    fun getCacheSizeBytes(context: Context): Long {
        return cacheDir(context).listFiles()?.sumOf { it.length() } ?: 0L
    }

    suspend fun inspectArtworkDetails(context: Context, song: Song): ArtworkDetails =
        withContext(Dispatchers.IO) {
            val customFile = getCustomArtworkFile(context, song.id)
            var width = 0
            var height = 0
            var sizeBytes = 0L
            var format = "JPEG"
            val sourceLabel: String

            if (customFile.exists() && customFile.length() > 0L) {
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(customFile.absolutePath, opts)
                width = opts.outWidth.coerceAtLeast(0)
                height = opts.outHeight.coerceAtLeast(0)
                sizeBytes = customFile.length()
                sourceLabel = "Custom User Cover"
            } else {
                val embedded = extractEmbeddedPictureBytes(context, song.contentUri, song.filePath)
                if (embedded != null && embedded.isNotEmpty()) {
                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(embedded, 0, embedded.size, opts)
                    width = opts.outWidth.coerceAtLeast(0)
                    height = opts.outHeight.coerceAtLeast(0)
                    sizeBytes = embedded.size.toLong()
                    format = opts.outMimeType?.substringAfterLast('/')?.uppercase() ?: "JPEG"
                    sourceLabel = "Embedded Track Tag"
                } else {
                    width = 512
                    height = 512
                    sizeBytes = getCachedFileForTrack(context, song.id, song.dateModifiedSec)
                        .takeIf { it.exists() }?.length() ?: 0L
                    format = "Generated"
                    sourceLabel = "Mika Procedural Signature"
                }
            }

            val colors = extractBrightArtworkColors(context, song)
            val domHex = colors?.first?.let { colorToHex(it) } ?: "#3B82F6"
            val secHex = colors?.second?.let { colorToHex(it) } ?: "#06B6D4"
            ArtworkDetails(
                width = width,
                height = height,
                format = format,
                sizeBytes = sizeBytes,
                sourceLabel = sourceLabel,
                dominantHex = domHex,
                secondaryHex = secHex
            )
        }

    private fun colorToHex(color: Color): String {
        val r = (color.red * 255f).toInt().coerceIn(0, 255)
        val g = (color.green * 255f).toInt().coerceIn(0, 255)
        val b = (color.blue * 255f).toInt().coerceIn(0, 255)
        return String.format("#%02X%02X%02X", r, g, b)
    }

    suspend fun saveArtworkToMikaFolder(context: Context, song: Song): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val safeTitle = song.title.replace(Regex("[^a-zA-Z0-9._-]+"), "_")
                    .trim('_').ifBlank { "track_${song.id}" }
                val fileName = "${safeTitle}_${song.id}.jpg"
                val customFile = getCustomArtworkFile(context, song.id)
                val rawBytes: ByteArray = when {
                    customFile.exists() && customFile.length() > 0L -> customFile.readBytes()
                    else -> extractEmbeddedPictureBytes(context, song.contentUri, song.filePath)
                        ?: run {
                            val bmp = getOrExtractBitmap(context, song, 1024)
                                ?: SongFallbackArtworkGenerator.generate(song.toArtworkRequest(), 720)
                            val baos = java.io.ByteArrayOutputStream()
                            bmp.compress(Bitmap.CompressFormat.JPEG, 95, baos)
                            baos.toByteArray()
                        }
                }

                // Try primary external root /mika/artwork first
                val extRoot = runCatching {
                    @Suppress("DEPRECATION")
                    Environment.getExternalStorageDirectory()
                }.getOrNull()
                if (extRoot != null) {
                    val mikaArtDir = File(extRoot, "mika/artwork")
                    if (mikaArtDir.exists() || mikaArtDir.mkdirs()) {
                        if (mikaArtDir.canWrite()) {
                            val outFile = File(mikaArtDir, fileName)
                            FileOutputStream(outFile).use { it.write(rawBytes) }
                            return@runCatching "/mika/artwork/$fileName"
                        }
                    }
                }

                // Scoped storage MediaStore Pictures/mika/artwork on API 29+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        put(
                            MediaStore.Images.Media.RELATIVE_PATH,
                            "${Environment.DIRECTORY_PICTURES}/mika/artwork"
                        )
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                    val resolver = context.contentResolver
                    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { it.write(rawBytes) }
                        values.clear()
                        values.put(MediaStore.Images.Media.IS_PENDING, 0)
                        resolver.update(uri, values, null, null)
                        return@runCatching "Pictures/mika/artwork/$fileName"
                    }
                }

                // App-private fallback
                val fallbackDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "mika/artwork")
                if (!fallbackDir.exists()) fallbackDir.mkdirs()
                val fallbackFile = File(fallbackDir, fileName)
                FileOutputStream(fallbackFile).use { it.write(rawBytes) }
                fallbackFile.absolutePath
            }
        }
}

class TrackArtworkKeyer : Keyer<TrackArtworkRequest> {
    override fun key(data: TrackArtworkRequest, options: Options): String {
        val targetBucket = when (val w = options.size.width) {
            is Dimension.Pixels -> if (w.px <= 220) "sm" else "lg"
            else -> "lg"
        }
        return "mika_track_${data.songId}_${data.dateModifiedSec}_$targetBucket"
    }
}

class TrackArtworkFetcher(
    private val context: Context,
    private val request: TrackArtworkRequest,
    private val targetMaxDim: Int
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val bitmap = withContext(Dispatchers.IO) {
            ArtworkManager.getOrExtractBitmapLocked(context, request, maxDimension = targetMaxDim)
        } ?: SongFallbackArtworkGenerator.generate(request, targetMaxDim.coerceAtMost(512))

        return DrawableResult(
            drawable = bitmap.toDrawable(context.resources),
            isSampled = true,
            dataSource = DataSource.DISK
        )
    }

    class Factory(private val context: Context) : Fetcher.Factory<TrackArtworkRequest> {
        override fun create(
            data: TrackArtworkRequest,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher {
            val px = (options.size.width as? Dimension.Pixels)?.px ?: 600
            val targetDim = if (px <= 220) 240 else 600
            return TrackArtworkFetcher(context, data, targetDim)
        }
    }
}

object SongFallbackArtworkGenerator {

    private val palettePairs = listOf(
        0xFF0F294A.toInt() to 0xFF1E88FF.toInt(),
        0xFF0A2E36.toInt() to 0xFF06B6D4.toInt(),
        0xFF1E1B4B.toInt() to 0xFF6366F1.toInt(),
        0xFF2E1065.toInt() to 0xFFA855F7.toInt(),
        0xFF4C0519.toInt() to 0xFFF43F5E.toInt(),
        0xFF064E3B.toInt() to 0xFF10B981.toInt(),
        0xFF451A03.toInt() to 0xFFF59E0B.toInt(),
        0xFF134E4A.toInt() to 0xFF2DD4BF.toInt(),
        0xFF3B0764.toInt() to 0xFFEC4899.toInt(),
        0xFF172554.toInt() to 0xFF38BDF8.toInt(),
        0xFF31102F.toInt() to 0xFFE11D48.toInt(),
        0xFF111827.toInt() to 0xFF3B82F6.toInt()
    )

    data class VisualSignature(
        val paletteIndex: Int,
        val darkColor: Int,
        val brightColor: Int,
        val angleMode: Int,
        val initials: String
    )

    fun deriveVisualSignature(req: TrackArtworkRequest): VisualSignature {
        val seed = (req.songId * 31L) xor req.title.hashCode().toLong() xor req.filePath.hashCode().toLong()
        val idx = abs(seed % palettePairs.size).toInt()
        val (darkColor, brightColor) = palettePairs[idx]
        val angleMode = abs((seed shr 4) % 4).toInt()
        val initials = req.title
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .ifEmpty { "M" }
        return VisualSignature(idx, darkColor, brightColor, angleMode, initials)
    }

    fun generate(req: TrackArtworkRequest, size: Int = 512): Bitmap {
        val seed = (req.songId * 31L) xor req.title.hashCode().toLong() xor req.filePath.hashCode().toLong()
        val sig = deriveVisualSignature(req)
        val darkColor = sig.darkColor
        val brightColor = sig.brightColor

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val w = size.toFloat()
        val h = size.toFloat()

        val (x0, y0, x1, y1) = when (sig.angleMode) {
            0 -> listOf(0f, 0f, w, h)
            1 -> listOf(w, 0f, 0f, h)
            2 -> listOf(0f, h * 0.5f, w, h * 0.5f)
            else -> listOf(w * 0.5f, 0f, w * 0.5f, h)
        }

        // Smooth ambient deep gradient backdrop
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(x0, y0, x1, y1, darkColor, brightColor, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // Subtle atmospheric corner vignette glow
        val glowCx = w * (0.30f + (abs(seed % 40) / 100f))
        val glowCy = h * (0.28f + (abs((seed shr 6) % 40) / 100f))
        val orbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                glowCx, glowCy, w * 0.72f,
                intArrayOf(0x42FFFFFF, 0x00FFFFFF),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(glowCx, glowCy, w * 0.72f, orbPaint)

        // Center minimalist vinyl disc + soft frosted translucent core
        val center = w / 2f
        val discOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x26000000
        }
        canvas.drawCircle(center, center, w * 0.30f, discOuterPaint)

        val discStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = 0x44FFFFFF
            strokeWidth = w * 0.010f
        }
        canvas.drawCircle(center, center, w * 0.30f, discStrokePaint)

        // Inner thin vinyl groove
        val groovePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = 0x24FFFFFF
            strokeWidth = w * 0.006f
        }
        canvas.drawCircle(center, center, w * 0.22f, groovePaint)

        // Frosted centerpiece emblem with harmonic 5-bar pulse
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x2EFFFFFF
        }
        canvas.drawCircle(center, center, w * 0.145f, labelPaint)

        val eqPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xEBFFFFFF.toInt()
            strokeCap = Paint.Cap.ROUND
            strokeWidth = w * 0.022f
        }
        val heights = floatArrayOf(0.036f, 0.068f, 0.095f, 0.068f, 0.036f)
        val spacing = w * 0.036f
        val baseStartX = center - spacing * 2f
        for (i in heights.indices) {
            val bx = baseStartX + i * spacing
            val halfBar = w * heights[i]
            canvas.drawLine(bx, center - halfBar, bx, center + halfBar, eqPaint)
        }

        return bitmap
    }
}
