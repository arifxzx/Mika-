package com.example.media

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.LruCache
import com.example.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Extracts and caches normalized audio amplitude waveforms (48 bars) per track
 * so seek bars can render real track envelope peaks cleanly on the UI thread.
 */
object WaveformExtractor {
    const val BAR_COUNT = 48
    private const val CACHE_DIR = "waveform_cache"
    private val memoryCache = LruCache<String, FloatArray>(64)

    private fun cacheFile(context: Context, song: Song): File {
        val dir = File(context.cacheDir, CACHE_DIR).apply { if (!exists()) mkdirs() }
        return File(dir, "wf_${song.id}_${song.dateModifiedSec}.dat")
    }

    suspend fun getOrExtractWaveform(context: Context, song: Song): FloatArray = withContext(Dispatchers.IO) {
        val memKey = "${song.id}_${song.dateModifiedSec}"
        memoryCache.get(memKey)?.let { return@withContext it }

        val file = cacheFile(context, song)
        if (file.exists() && file.length() > 0L) {
            val loaded = runCatching {
                val parts = file.readText().split(",")
                if (parts.size == BAR_COUNT) {
                    FloatArray(BAR_COUNT) { idx -> parts[idx].toFloatOrNull()?.coerceIn(0.10f, 1.0f) ?: 0.25f }
                } else null
            }.getOrNull()
            if (loaded != null) {
                memoryCache.put(memKey, loaded)
                return@withContext loaded
            }
        }

        val extracted = extractPeaksFast(context, song) ?: deterministicFallbackWaveform(song)
        runCatching {
            file.writeText(extracted.joinToString(",") { "%.3f".format(it) })
        }
        memoryCache.put(memKey, extracted)
        extracted
    }

    private fun extractPeaksFast(context: Context, song: Song): FloatArray? {
        val extractor = MediaExtractor()
        return runCatching {
            var opened = false
            if (song.contentUri.isNotBlank()) {
                opened = runCatching {
                    extractor.setDataSource(context, Uri.parse(song.contentUri), null)
                }.isSuccess
            }
            if (!opened && song.filePath.isNotBlank() && File(song.filePath).exists()) {
                opened = runCatching { extractor.setDataSource(song.filePath) }.isSuccess
            }
            if (!opened) return null

            var audioTrackIndex = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val tf = extractor.getTrackFormat(i)
                val mime = tf.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    format = tf
                    break
                }
            }
            if (audioTrackIndex < 0 || format == null) return null

            extractor.selectTrack(audioTrackIndex)
            val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) {
                format.getLong(MediaFormat.KEY_DURATION)
            } else {
                song.durationMs.coerceAtLeast(1000L) * 1000L
            }
            if (durationUs <= 0L) return null

            val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
            val codec = MediaCodec.createDecoderByType(mime)
            val peaks = FloatArray(BAR_COUNT)
            try {
                codec.configure(format, null, null, 0)
                codec.start()

                val bufferInfo = MediaCodec.BufferInfo()
                val rawBuf = java.nio.ByteBuffer.allocate(64 * 1024).order(ByteOrder.LITTLE_ENDIAN)

                // Probe BAR_COUNT evenly spaced windows across the track for O(1)-bounded fast extraction
                for (barIdx in 0 until BAR_COUNT) {
                    val targetUs = ((barIdx + 0.5) / BAR_COUNT * durationUs).toLong()
                    extractor.seekTo(targetUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                    codec.flush()

                    var rmsSum = 0.0
                    var sampleCount = 0
                    var iterations = 0
                    while (iterations < 6 && sampleCount < 1024) {
                        iterations++
                        val inIdx = codec.dequeueInputBuffer(8_000L)
                        if (inIdx >= 0) {
                            val inBuf = codec.getInputBuffer(inIdx)
                            rawBuf.clear()
                            val readSize = extractor.readSampleData(rawBuf, 0)
                            if (readSize < 0) {
                                codec.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            } else {
                                rawBuf.limit(readSize)
                                inBuf?.clear()
                                inBuf?.put(rawBuf)
                                codec.queueInputBuffer(inIdx, 0, readSize, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }

                        val outIdx = codec.dequeueOutputBuffer(bufferInfo, 8_000L)
                        if (outIdx >= 0) {
                            val outBuf = codec.getOutputBuffer(outIdx)
                            if (outBuf != null && bufferInfo.size > 0) {
                                outBuf.position(bufferInfo.offset)
                                outBuf.limit(bufferInfo.offset + bufferInfo.size)
                                val dup = outBuf.slice().order(ByteOrder.LITTLE_ENDIAN)
                                while (dup.remaining() >= 2 && sampleCount < 1536) {
                                    val pcm = dup.short / 32768.0
                                    rmsSum += pcm * pcm
                                    sampleCount++
                                }
                            }
                            codec.releaseOutputBuffer(outIdx, false)
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) break
                        }
                    }

                    peaks[barIdx] = if (sampleCount > 0) {
                        sqrt(rmsSum / sampleCount).toFloat()
                    } else 0.12f
                }
            } finally {
                runCatching { codec.stop() }
                runCatching { codec.release() }
            }

            val maxPeak = peaks.maxOrNull()?.coerceAtLeast(0.05f) ?: 0.5f
            FloatArray(BAR_COUNT) { idx ->
                val norm = (peaks[idx] / maxPeak)
                max(0.12f, norm.coerceIn(0.12f, 1.0f))
            }
        }.also {
            runCatching { extractor.release() }
        }.getOrNull()
    }

    private fun deterministicFallbackWaveform(song: Song): FloatArray {
        var seed = song.id xor (song.durationMs * 31L)
        return FloatArray(BAR_COUNT) { idx ->
            seed = seed * 6364136223846793005L + 1442695040888963407L
            val rand = ((abs(seed shr 32) % 1000).toFloat() / 1000f)
            val env = 0.35f + 0.45f * kotlin.math.sin((idx.toFloat() / BAR_COUNT) * Math.PI).toFloat()
            (env * 0.65f + rand * 0.35f).coerceIn(0.14f, 0.98f)
        }
    }

    fun clearCache(context: Context): Long {
        memoryCache.evictAll()
        val dir = File(context.cacheDir, CACHE_DIR)
        var freed = 0L
        dir.listFiles()?.forEach {
            freed += it.length()
            it.delete()
        }
        return freed
    }
}
