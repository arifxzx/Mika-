package com.example.media

import android.content.Context
import android.net.Uri
import com.example.model.LyricLine
import com.example.model.LyricsData
import com.example.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.Locale

object LyricsParser {

    private val lrcLineRegex = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?](.*)""")
    private val lyricsTagKeys = setOf(
        "LYRICS",
        "UNSYNCEDLYRICS",
        "UNSYNCED LYRICS",
        "SYNCEDLYRICS",
        "SYNCED LYRICS",
        "LYRIC"
    )

    private const val CUSTOM_LYRICS_DIR = "lyrics"

    fun resolveCustomLyricsFile(context: Context, songId: Long): File {
        val dir = File(context.filesDir, CUSTOM_LYRICS_DIR)
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "$songId.lrc")
    }

    fun hasSidecarLrc(filePath: String): Boolean {
        if (filePath.isBlank()) return false
        return resolveLrcFile(filePath)?.exists() == true
    }

    fun resolveLrcFile(audioFilePath: String): File? {
        if (audioFilePath.isBlank()) return null
        val audioFile = File(audioFilePath)
        val baseName = audioFile.nameWithoutExtension
        val parent = audioFile.parentFile ?: return null
        val candidateDirs = listOfNotNull(
            parent,
            File(parent, "Lyrics").takeIf { it.isDirectory },
            File(parent, "lyrics").takeIf { it.isDirectory }
        )
        for (dir in candidateDirs) {
            for (ext in listOf("lrc", "LRC", "txt", "TXT")) {
                val exact = File(dir, "$baseName.$ext")
                if (exact.exists() && exact.canRead()) return exact
            }
            val matched = dir.listFiles()?.firstOrNull { file ->
                file.isFile &&
                    file.canRead() &&
                    (file.extension.equals("lrc", ignoreCase = true) ||
                        file.extension.equals("txt", ignoreCase = true)) &&
                    file.nameWithoutExtension.equals(baseName, ignoreCase = true)
            }
            if (matched != null) return matched
        }
        return null
    }

    private fun readSafSiblingLrc(context: Context, song: Song): String? {
        val songUri = runCatching { Uri.parse(song.contentUri) }.getOrNull() ?: return null
        val docId = runCatching {
            if (android.provider.DocumentsContract.isDocumentUri(context, songUri)) {
                android.provider.DocumentsContract.getDocumentId(songUri)
            } else null
        }.getOrNull() ?: return null
        if (!song.contentUri.contains("/tree/")) return null
        val treePart = song.contentUri.substringBefore("/document/")
        val treeUri = runCatching { Uri.parse(treePart) }.getOrNull() ?: return null
        val parentDocId = docId.substringBeforeLast('/', "")
        val baseName = song.title.trim()
        val fileBase = song.filePath.substringAfterLast('/').substringBeforeLast('.').ifBlank { baseName }
        val childrenUri = runCatching {
            android.provider.DocumentsContract.buildChildDocumentsUriUsingTree(
                treeUri,
                parentDocId.ifBlank { android.provider.DocumentsContract.getTreeDocumentId(treeUri) }
            )
        }.getOrNull() ?: return null

        return runCatching {
            context.contentResolver.query(
                childrenUri,
                arrayOf(
                    android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME
                ),
                null,
                null,
                null
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndex(android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    if (idCol < 0 || nameCol < 0) continue
                    val name = cursor.getString(nameCol) ?: continue
                    val ext = name.substringAfterLast('.', "").lowercase(Locale.US)
                    val stem = name.substringBeforeLast('.')
                    if (ext in setOf("lrc", "txt") &&
                        (stem.equals(fileBase, ignoreCase = true) || stem.equals(baseName, ignoreCase = true))
                    ) {
                        val lrcUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(
                            treeUri,
                            cursor.getString(idCol)
                        )
                        return@runCatching context.contentResolver.openInputStream(lrcUri)
                            ?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }
                    }
                }
                null
            }
        }.getOrNull()
    }

    suspend fun loadLyrics(context: Context, song: Song): LyricsData? = withContext(Dispatchers.IO) {
        // 0. Check user-edited or imported custom lyrics cached per song
        val customFile = File(File(context.filesDir, CUSTOM_LYRICS_DIR), "${song.id}.lrc")
        if (customFile.exists() && customFile.canRead()) {
            val customContent = runCatching { customFile.readText(StandardCharsets.UTF_8) }.getOrNull()
            if (!customContent.isNullOrBlank()) {
                return@withContext parseRawText(customContent, "Saved Lyrics")
            }
        }

        // 1. Check local .lrc / .txt sidecar file (same folder or Lyrics subfolder)
        val lrcFile = resolveLrcFile(song.filePath)
        if (lrcFile != null) {
            val content = runCatching { lrcFile.readText(StandardCharsets.UTF_8) }.getOrNull()
            if (!content.isNullOrBlank()) {
                return@withContext parseRawText(content, "Local LRC")
            }
        }

        // 2. Check SAF document sibling sidecar if track lives inside anSAF tree
        val safLrc = readSafSiblingLrc(context, song)
        if (!safLrc.isNullOrBlank()) {
            return@withContext parseRawText(safLrc, "Local LRC")
        }

        // 3. Check embedded tags via File (fast) or ContentResolver URI fallback (Scoped Storage)
        val embedded = extractEmbeddedLyrics(context, song)
        if (!embedded.isNullOrBlank()) {
            return@withContext parseRawText(embedded, "Embedded Tags")
        }

        null
    }

    suspend fun saveCustomLyrics(context: Context, songId: Long, rawText: String): LyricsData? = withContext(Dispatchers.IO) {
        val clean = rawText.trim()
        val target = resolveCustomLyricsFile(context, songId)
        if (clean.isEmpty()) {
            if (target.exists()) target.delete()
            return@withContext null
        }
        target.writeText(clean, StandardCharsets.UTF_8)
        parseRawText(clean, "Saved Lyrics")
    }

    suspend fun deleteCustomLyrics(context: Context, songId: Long): Boolean = withContext(Dispatchers.IO) {
        val target = File(File(context.filesDir, CUSTOM_LYRICS_DIR), "${songId}.lrc")
        if (target.exists()) target.delete() else false
    }

    suspend fun loadFromUri(context: Context, uri: Uri): Pair<String, LyricsData>? = withContext(Dispatchers.IO) {
        val text = runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull()
        if (text.isNullOrBlank()) null else text to parseRawText(text, "Imported LRC")
    }

    fun parseRawText(raw: String, sourceLabel: String): LyricsData {
        val synced = mutableListOf<LyricLine>()
        val plainLines = raw.replace("\r\n", "\n").replace('\r', '\n').lines()

        for (line in plainLines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            val matches = lrcLineRegex.findAll(trimmed).toList()
            if (matches.isNotEmpty()) {
                val textPart = trimmed.replace(Regex("""\[\d{1,3}:\d{2}(?:[.:]\d{1,3})?]"""), "").trim()
                if (textPart.isNotEmpty()) {
                    for (m in matches) {
                        val min = m.groupValues[1].toLongOrNull() ?: 0L
                        val sec = m.groupValues[2].toLongOrNull() ?: 0L
                        val fracStr = m.groupValues[3]
                        val ms = when (fracStr.length) {
                            1 -> (fracStr.toLongOrNull() ?: 0L) * 100L
                            2 -> (fracStr.toLongOrNull() ?: 0L) * 10L
                            3 -> fracStr.toLongOrNull() ?: 0L
                            else -> 0L
                        }
                        val totalMs = min * 60_000L + sec * 1000L + ms
                        synced.add(LyricLine(totalMs, textPart))
                    }
                }
            }
        }

        if (synced.isNotEmpty()) {
            return LyricsData(
                isSynced = true,
                lines = synced.sortedBy { it.timestampMs },
                sourceLabel = sourceLabel
            )
        }

        val cleanedPlain = plainLines
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapIndexed { index, text -> LyricLine(index * 1000L, text) }

        return LyricsData(
            isSynced = false,
            lines = cleanedPlain,
            sourceLabel = sourceLabel
        )
    }

    fun formatLrcTimestamp(ms: Long): String {
        val clamped = ms.coerceAtLeast(0L)
        val mins = clamped / 60_000L
        val secs = (clamped % 60_000L) / 1000L
        val centis = (clamped % 1000L) / 10L
        return String.format(Locale.US, "[%02d:%02d.%02d]", mins, secs, centis)
    }

    fun shiftLrcTimestamps(rawText: String, deltaMs: Long): String {
        if (deltaMs == 0L) return rawText
        val tagRegex = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")
        return rawText.replace("\r\n", "\n").replace('\r', '\n').lines().joinToString("\n") { line ->
            tagRegex.replace(line) { match ->
                val min = match.groupValues[1].toLongOrNull() ?: 0L
                val sec = match.groupValues[2].toLongOrNull() ?: 0L
                val frac = match.groupValues[3]
                val ms = when (frac.length) {
                    1 -> (frac.toLongOrNull() ?: 0L) * 100L
                    2 -> (frac.toLongOrNull() ?: 0L) * 10L
                    3 -> frac.toLongOrNull() ?: 0L
                    else -> 0L
                }
                val shifted = (min * 60_000L + sec * 1000L + ms + deltaMs).coerceAtLeast(0L)
                formatLrcTimestamp(shifted)
            }
        }
    }

    fun stripLrcTimestamps(rawText: String): String {
        val tagRegex = Regex("""\[\d{1,3}:\d{2}(?:[.:]\d{1,3})?]\s*""")
        return rawText.replace("\r\n", "\n").replace('\r', '\n')
            .lines()
            .joinToString("\n") { line -> line.replace(tagRegex, "").trimEnd() }
    }

    fun stampLineTimestamp(rawText: String, lineIndex: Int, timestampMs: Long): String {
        val lines = rawText.replace("\r\n", "\n").replace('\r', '\n').lines().toMutableList()
        if (lineIndex !in lines.indices) return rawText
        val tagRegex = Regex("""^\s*(\[\d{1,3}:\d{2}(?:[.:]\d{1,3})?]\s*)+""")
        val stripped = lines[lineIndex].replace(tagRegex, "").trimStart()
        lines[lineIndex] = "${formatLrcTimestamp(timestampMs)}$stripped"
        return lines.joinToString("\n")
    }

    internal fun extractEmbeddedLyricsFromFile(file: File): String? {
        if (!file.exists() || !file.canRead() || file.length() < 16) return null
        return runCatching {
            FileInputStream(file).use { fis ->
                extractEmbeddedLyricsFromStream(BufferedInputStream(fis, 32 * 1024), file.extension.lowercase())
            }
        }.getOrNull()
    }

    private fun extractEmbeddedLyrics(context: Context, song: Song): String? {
        if (song.filePath.isNotBlank()) {
            val file = File(song.filePath)
            if (file.exists() && file.canRead() && file.length() >= 16) {
                extractEmbeddedLyricsFromFile(file)?.let { return it }
            }
        }
        val uri = runCatching { Uri.parse(song.contentUri) }.getOrNull() ?: return null
        val ext = song.filePath.substringAfterLast('.', "").ifBlank {
            mimeToExtension(song.mimeType)
        }.lowercase()
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                extractEmbeddedLyricsFromStream(BufferedInputStream(stream, 32 * 1024), ext)
            }
        }.getOrNull()
    }

    private fun mimeToExtension(mime: String): String = when {
        mime.contains("mpeg") || mime.contains("mp3") -> "mp3"
        mime.contains("flac") -> "flac"
        mime.contains("mp4") || mime.contains("m4a") || mime.contains("aac") -> "m4a"
        mime.contains("ogg") -> "ogg"
        mime.contains("opus") -> "opus"
        else -> ""
    }

    internal fun extractEmbeddedLyricsFromStream(input: BufferedInputStream, hintExt: String): String? {
        input.mark(16)
        val probe = ByteArray(12)
        val n = readUpTo(input, probe)
        input.reset()
        if (n < 4) return null

        val magic3 = String(probe, 0, minOf(3, n), StandardCharsets.US_ASCII)
        val magic4 = String(probe, 0, minOf(4, n), StandardCharsets.US_ASCII)
        val boxType = if (n >= 8) String(probe, 4, 4, StandardCharsets.US_ASCII) else ""

        return when {
            magic3 == "ID3" -> readId3v2Lyrics(input)
            magic4 == "fLaC" -> readFlacLyrics(input)
            magic4 == "OggS" -> readOggVorbisOrOpusLyrics(input)
            boxType == "ftyp" || hintExt in setOf("m4a", "mp4", "m4b", "aac", "alac") -> readMp4Lyrics(input)
            hintExt == "flac" -> readFlacLyrics(input)
            hintExt == "ogg" || hintExt == "opus" -> readOggVorbisOrOpusLyrics(input)
            else -> readId3v2Lyrics(input)
        }
    }

    // -------------------------------------------------------------------------
    // ID3v2 (v2.2 / v2.3 / v2.4) — streams frames and skips large APIC art cleanly
    // -------------------------------------------------------------------------
    private fun readId3v2Lyrics(input: InputStream): String? = runCatching {
        val header = readExact(input, 10) ?: return@runCatching null
        if (header[0] != 'I'.code.toByte() ||
            header[1] != 'D'.code.toByte() ||
            header[2] != '3'.code.toByte()
        ) return@runCatching null

        val majorVersion = header[3].toInt() and 0xFF
        val headerFlags = header[5].toInt() and 0xFF
        val globalUnsync = (headerFlags and 0x80) != 0
        val hasExtendedHeader = (headerFlags and 0x40) != 0

        val tagSize = syncSafeInt(header[6], header[7], header[8], header[9])
        if (tagSize <= 0 || tagSize > 64 * 1024 * 1024) return@runCatching null

        var remaining = tagSize
        if (hasExtendedHeader && remaining >= 4) {
            val extLenBytes = readExact(input, 4) ?: return@runCatching null
            remaining -= 4
            val extSize = if (majorVersion >= 4) {
                (syncSafeInt(extLenBytes[0], extLenBytes[1], extLenBytes[2], extLenBytes[3]) - 4).coerceAtLeast(0)
            } else {
                bigEndianInt(extLenBytes[0], extLenBytes[1], extLenBytes[2], extLenBytes[3]).coerceAtLeast(0)
            }
            if (extSize > remaining || !skipFully(input, extSize.toLong())) return@runCatching null
            remaining -= extSize
        }

        val frameHeaderSize = if (majorVersion == 2) 6 else 10
        var fallbackTxxx: String? = null
        var fallbackSylt: String? = null

        while (remaining >= frameHeaderSize) {
            val fh = readExact(input, frameHeaderSize) ?: break
            remaining -= frameHeaderSize
            if (fh[0] == 0.toByte()) break

            val frameId: String
            val frameSize: Int
            var frameFlags = 0
            if (majorVersion == 2) {
                frameId = String(fh, 0, 3, StandardCharsets.US_ASCII)
                frameSize = ((fh[3].toInt() and 0xFF) shl 16) or
                    ((fh[4].toInt() and 0xFF) shl 8) or
                    (fh[5].toInt() and 0xFF)
            } else {
                frameId = String(fh, 0, 4, StandardCharsets.US_ASCII)
                frameSize = if (majorVersion == 4) {
                    syncSafeInt(fh[4], fh[5], fh[6], fh[7])
                } else {
                    bigEndianInt(fh[4], fh[5], fh[6], fh[7])
                }
                frameFlags = ((fh[8].toInt() and 0xFF) shl 8) or (fh[9].toInt() and 0xFF)
            }

            if (frameSize <= 0 || frameSize > remaining) break

            val isLyricsFrame = frameId == "USLT" || frameId == "ULT" ||
                frameId == "SYLT" || frameId == "SLT" ||
                frameId == "TXXX" || frameId == "TXX"

            if (!isLyricsFrame || frameSize > 2 * 1024 * 1024) {
                // Skip APIC/PRIV/GEOB/etc. without allocating RAM!
                if (!skipFully(input, frameSize.toLong())) break
                remaining -= frameSize
                continue
            }

            val rawPayload = readExact(input, frameSize) ?: break
            remaining -= frameSize

            val frameUnsync = globalUnsync || (majorVersion >= 4 && (frameFlags and 0x0002) != 0)
            var payload = if (frameUnsync) removeId3Unsync(rawPayload) else rawPayload

            // Strip v2.4 Data Length Indicator (4 bytes) if flag bit 0x0001 is set
            if (majorVersion >= 4 && (frameFlags and 0x0001) != 0 && payload.size > 4) {
                payload = payload.copyOfRange(4, payload.size)
            }

            when (frameId) {
                "USLT", "ULT" -> {
                    val decoded = decodeUsltPayload(payload)
                    if (!decoded.isNullOrBlank()) return@runCatching decoded
                }
                "SYLT", "SLT" -> {
                    if (fallbackSylt == null) {
                        fallbackSylt = decodeSyltPayload(payload)
                    }
                }
                "TXXX", "TXX" -> {
                    if (fallbackTxxx == null) {
                        fallbackTxxx = decodeTxxxLyricsPayload(payload)
                    }
                }
            }
        }

        fallbackSylt ?: fallbackTxxx
    }.getOrNull()

    private fun decodeUsltPayload(payload: ByteArray): String? {
        if (payload.size <= 4) return null
        val encoding = payload[0].toInt() and 0xFF
        val charset = id3Charset(encoding)
        // 1 byte encoding + 3 bytes language
        var pos = skipTerminatedString(payload, 4, encoding)
        if (pos >= payload.size) return null
        return decodeId3Text(payload, pos, payload.size, encoding, charset)
            .trim('\u0000', ' ', '\n', '\r')
            .takeIf { it.isNotBlank() }
    }

    private fun decodeTxxxLyricsPayload(payload: ByteArray): String? {
        if (payload.size <= 2) return null
        val encoding = payload[0].toInt() and 0xFF
        val charset = id3Charset(encoding)
        val descEnd = findStringTerminator(payload, 1, encoding)
        val desc = decodeId3Text(payload, 1, descEnd, encoding, charset)
            .trim('\u0000', ' ')
            .uppercase(Locale.US)
        if (desc !in lyricsTagKeys && !desc.startsWith("LYRICS")) return null
        val valStart = skipTerminatedString(payload, 1, encoding)
        if (valStart >= payload.size) return null
        return decodeId3Text(payload, valStart, payload.size, encoding, charset)
            .trim('\u0000', ' ', '\n', '\r')
            .takeIf { it.isNotBlank() }
    }

    private fun decodeSyltPayload(payload: ByteArray): String? {
        if (payload.size <= 6) return null
        val encoding = payload[0].toInt() and 0xFF
        val timestampFormat = payload[4].toInt() and 0xFF
        if (timestampFormat != 1 && timestampFormat != 2) return null // 1 = MPEG frames, 2 = ms
        val charset = id3Charset(encoding)
        var pos = skipTerminatedString(payload, 6, encoding)
        val sb = StringBuilder()
        while (pos < payload.size) {
            val textEnd = findStringTerminator(payload, pos, encoding)
            val lineText = decodeId3Text(payload, pos, textEnd, encoding, charset)
                .replace('\r', ' ')
                .replace('\n', ' ')
                .trim('\u0000', ' ')
            pos = skipTerminatedString(payload, pos, encoding)
            if (pos + 4 > payload.size) break
            val rawStamp = bigEndianInt(payload[pos], payload[pos + 1], payload[pos + 2], payload[pos + 3]).toLong()
                .coerceAtLeast(0L)
            val ms = if (timestampFormat == 1) rawStamp * 26L else rawStamp
            pos += 4
            if (lineText.isNotEmpty()) {
                val mins = ms / 60_000L
                val secs = (ms % 60_000L) / 1000L
                val centis = (ms % 1000L) / 10L
                sb.append(String.format(Locale.US, "[%02d:%02d.%02d]%s\n", mins, secs, centis, lineText))
            }
        }
        return sb.toString().trim().takeIf { it.isNotBlank() }
    }

    // -------------------------------------------------------------------------
    // MP4 / M4A / AAC (`moov -> udta -> meta -> ilst -> ©lyr`)
    // -------------------------------------------------------------------------
    private fun readMp4Lyrics(input: InputStream): String? = runCatching {
        while (true) {
            val atomHeader = readExact(input, 8) ?: break
            var size = bigEndianUInt32(atomHeader, 0)
            val type = String(atomHeader, 4, 4, StandardCharsets.US_ASCII)
            var headerLen = 8L
            if (size == 1L) {
                val ext = readExact(input, 8) ?: break
                size = bigEndianInt64(ext, 0)
                headerLen = 16L
            } else if (size == 0L) {
                break
            }
            val payloadSize = size - headerLen
            if (payloadSize < 0L) break

            if (type == "moov") {
                if (payloadSize > 16 * 1024 * 1024L) return@runCatching null
                val moovBytes = readExact(input, payloadSize.toInt()) ?: return@runCatching null
                return@runCatching findMp4LyricsInContainer(moovBytes, 0, moovBytes.size)
            } else {
                if (!skipFully(input, payloadSize)) break
            }
        }
        null
    }.getOrNull()

    private fun findMp4LyricsInContainer(data: ByteArray, start: Int, end: Int): String? {
        var pos = start
        while (pos + 8 <= end) {
            val rawSize = bigEndianUInt32(data, pos)
            val type = String(data, pos + 4, 4, StandardCharsets.ISO_8859_1)
            var headerLen = 8
            var boxSize = rawSize
            if (boxSize == 1L && pos + 16 <= end) {
                boxSize = bigEndianInt64(data, pos + 8)
                headerLen = 16
            }
            if (boxSize < headerLen || pos + boxSize > end) break
            val childStart = pos + headerLen
            val childEnd = (pos + boxSize).toInt()

            when (type) {
                "udta", "ilst", "trak", "mdia" -> {
                    findMp4LyricsInContainer(data, childStart, childEnd)?.let { return it }
                }
                "meta" -> {
                    // MP4 'meta' FullBox has 4 bytes version/flags before sub-atoms
                    val subStart = childStart + 4
                    if (subStart < childEnd) {
                        findMp4LyricsInContainer(data, subStart, childEnd)?.let { return it }
                    }
                }
                "\u00A9lyr" -> {
                    extractMp4DataAtomText(data, childStart, childEnd)?.let { return it }
                }
                "----" -> {
                    extractMp4FreeformLyrics(data, childStart, childEnd)?.let { return it }
                }
            }
            pos = childEnd
        }
        return null
    }

    private fun extractMp4DataAtomText(data: ByteArray, start: Int, end: Int): String? {
        var pos = start
        while (pos + 8 <= end) {
            val size = bigEndianUInt32(data, pos).toInt()
            val type = String(data, pos + 4, 4, StandardCharsets.US_ASCII)
            if (size < 8 || pos + size > end) break
            if (type == "data" && size > 16) {
                // 8 byte atom header + 4 byte type/flags + 4 byte locale = 16 bytes
                val text = String(data, pos + 16, size - 16, StandardCharsets.UTF_8).trim('\u0000', ' ')
                if (text.isNotBlank()) return text
            }
            pos += size
        }
        return null
    }

    private fun extractMp4FreeformLyrics(data: ByteArray, start: Int, end: Int): String? {
        var pos = start
        var fieldName = ""
        var valueText: String? = null
        while (pos + 8 <= end) {
            val size = bigEndianUInt32(data, pos).toInt()
            val type = String(data, pos + 4, 4, StandardCharsets.US_ASCII)
            if (size < 8 || pos + size > end) break
            if (type == "name" && size > 12) {
                fieldName = String(data, pos + 12, size - 12, StandardCharsets.UTF_8).trim('\u0000', ' ')
                    .uppercase(Locale.US)
            } else if (type == "data" && size > 16) {
                valueText = String(data, pos + 16, size - 16, StandardCharsets.UTF_8).trim('\u0000', ' ')
            }
            pos += size
        }
        return if (fieldName in lyricsTagKeys && !valueText.isNullOrBlank()) valueText else null
    }

    // -------------------------------------------------------------------------
    // FLAC VorbisComment (`fLaC`)
    // -------------------------------------------------------------------------
    private fun readFlacLyrics(input: InputStream): String? = runCatching {
        val magic = readExact(input, 4) ?: return@runCatching null
        if (String(magic, StandardCharsets.US_ASCII) != "fLaC") return@runCatching null
        var isLast = false
        while (!isLast) {
            val blockHeader = readExact(input, 4) ?: break
            isLast = (blockHeader[0].toInt() and 0x80) != 0
            val blockType = blockHeader[0].toInt() and 0x7F
            val blockLen = ((blockHeader[1].toInt() and 0xFF) shl 16) or
                ((blockHeader[2].toInt() and 0xFF) shl 8) or
                (blockHeader[3].toInt() and 0xFF)
            if (blockType == 4 && blockLen in 8..(2 * 1024 * 1024)) {
                val block = readExact(input, blockLen) ?: break
                parseVorbisComments(block, 0)?.let { return@runCatching it }
            } else {
                if (!skipFully(input, blockLen.toLong())) break
            }
        }
        null
    }.getOrNull()

    // -------------------------------------------------------------------------
    // OGG Vorbis & Opus (`OggS`)
    // -------------------------------------------------------------------------
    private fun readOggVorbisOrOpusLyrics(input: InputStream): String? = runCatching {
        val packetBuffer = java.io.ByteArrayOutputStream()
        var pagesRead = 0
        while (pagesRead < 32 && packetBuffer.size() < 512 * 1024) {
            val header = readExact(input, 27) ?: break
            if (header[0] != 'O'.code.toByte() ||
                header[1] != 'g'.code.toByte() ||
                header[2] != 'g'.code.toByte() ||
                header[3] != 'S'.code.toByte()
            ) break
            val segments = header[26].toInt() and 0xFF
            val segTable = readExact(input, segments) ?: break
            var pageBodySize = 0
            for (b in segTable) {
                pageBodySize += (b.toInt() and 0xFF)
            }
            if (pageBodySize > 0) {
                val body = readExact(input, pageBodySize) ?: break
                packetBuffer.write(body)
            }
            pagesRead++
        }
        val bytes = packetBuffer.toByteArray()
        if (bytes.isEmpty()) return@runCatching null

        // Look for Vorbis comment header `\u0003vorbis` or `OpusTags`
        val vorbisOffset = indexOfAscii(bytes, "\u0003vorbis")
        if (vorbisOffset >= 0) {
            return@runCatching parseVorbisComments(bytes, vorbisOffset + 7)
        }
        val opusOffset = indexOfAscii(bytes, "OpusTags")
        if (opusOffset >= 0) {
            return@runCatching parseVorbisComments(bytes, opusOffset + 8)
        }
        null
    }.getOrNull()

    private fun parseVorbisComments(block: ByteArray, startOffset: Int): String? {
        var offset = startOffset
        fun readLEInt(): Int {
            if (offset + 4 > block.size) throw EOFException()
            val v = (block[offset].toInt() and 0xFF) or
                ((block[offset + 1].toInt() and 0xFF) shl 8) or
                ((block[offset + 2].toInt() and 0xFF) shl 16) or
                ((block[offset + 3].toInt() and 0xFF) shl 24)
            offset += 4
            return v
        }
        if (offset + 4 > block.size) return null
        val vendorLen = readLEInt()
        if (vendorLen < 0 || offset + vendorLen + 4 > block.size) return null
        offset += vendorLen
        val commentCount = readLEInt().coerceAtMost(512)
        repeat(commentCount) {
            if (offset + 4 > block.size) return null
            val len = readLEInt()
            if (len <= 0 || offset + len > block.size) return null
            val comment = String(block, offset, len, StandardCharsets.UTF_8)
            offset += len
            val eqIdx = comment.indexOf('=')
            if (eqIdx > 0) {
                val key = comment.substring(0, eqIdx).trim().uppercase(Locale.US)
                if (key in lyricsTagKeys) {
                    val value = comment.substring(eqIdx + 1).trim()
                    if (value.isNotBlank()) return value
                }
            }
        }
        return null
    }

    // -------------------------------------------------------------------------
    // Binary Helpers
    // -------------------------------------------------------------------------
    private fun id3Charset(encoding: Int): Charset = when (encoding) {
        1 -> StandardCharsets.UTF_16
        2 -> StandardCharsets.UTF_16BE
        3 -> StandardCharsets.UTF_8
        else -> StandardCharsets.ISO_8859_1
    }

    private fun decodeId3Text(
        data: ByteArray,
        start: Int,
        end: Int,
        encoding: Int,
        defaultCharset: Charset
    ): String {
        if (start >= end) return ""
        return if (encoding == 1 && end - start >= 2) {
            val b0 = data[start].toInt() and 0xFF
            val b1 = data[start + 1].toInt() and 0xFF
            when {
                b0 == 0xFF && b1 == 0xFE -> String(data, start + 2, end - start - 2, StandardCharsets.UTF_16LE)
                b0 == 0xFE && b1 == 0xFF -> String(data, start + 2, end - start - 2, StandardCharsets.UTF_16BE)
                else -> String(data, start, end - start, StandardCharsets.UTF_16LE)
            }
        } else {
            String(data, start, end - start, defaultCharset)
        }
    }

    private fun findStringTerminator(data: ByteArray, start: Int, encoding: Int): Int {
        var pos = start
        if (encoding == 1 || encoding == 2) {
            while (pos + 1 < data.size) {
                if (data[pos] == 0.toByte() && data[pos + 1] == 0.toByte()) return pos
                pos += 2
            }
        } else {
            while (pos < data.size) {
                if (data[pos] == 0.toByte()) return pos
                pos += 1
            }
        }
        return data.size
    }

    private fun skipTerminatedString(data: ByteArray, start: Int, encoding: Int): Int {
        val term = findStringTerminator(data, start, encoding)
        return if (term >= data.size) data.size
        else if (encoding == 1 || encoding == 2) (term + 2).coerceAtMost(data.size)
        else (term + 1).coerceAtMost(data.size)
    }

    private fun removeId3Unsync(bytes: ByteArray): ByteArray {
        val out = ByteArray(bytes.size)
        var outLen = 0
        var i = 0
        while (i < bytes.size) {
            val b = bytes[i]
            out[outLen++] = b
            if (b == 0xFF.toByte() && i + 1 < bytes.size && bytes[i + 1] == 0x00.toByte()) {
                i += 2
            } else {
                i += 1
            }
        }
        return if (outLen == bytes.size) out else out.copyOf(outLen)
    }

    private fun indexOfAscii(haystack: ByteArray, needle: String): Int {
        val target = needle.toByteArray(StandardCharsets.ISO_8859_1)
        if (target.isEmpty() || haystack.size < target.size) return -1
        outer@ for (i in 0..(haystack.size - target.size)) {
            for (j in target.indices) {
                if (haystack[i + j] != target[j]) continue@outer
            }
            return i
        }
        return -1
    }

    private fun syncSafeInt(b0: Byte, b1: Byte, b2: Byte, b3: Byte): Int =
        ((b0.toInt() and 0x7F) shl 21) or
            ((b1.toInt() and 0x7F) shl 14) or
            ((b2.toInt() and 0x7F) shl 7) or
            (b3.toInt() and 0x7F)

    private fun bigEndianInt(b0: Byte, b1: Byte, b2: Byte, b3: Byte): Int =
        ((b0.toInt() and 0xFF) shl 24) or
            ((b1.toInt() and 0xFF) shl 16) or
            ((b2.toInt() and 0xFF) shl 8) or
            (b3.toInt() and 0xFF)

    private fun bigEndianUInt32(data: ByteArray, offset: Int): Long =
        ((data[offset].toLong() and 0xFFL) shl 24) or
            ((data[offset + 1].toLong() and 0xFFL) shl 16) or
            ((data[offset + 2].toLong() and 0xFFL) shl 8) or
            (data[offset + 3].toLong() and 0xFFL)

    private fun bigEndianInt64(data: ByteArray, offset: Int): Long {
        var res = 0L
        for (i in 0 until 8) {
            res = (res shl 8) or (data[offset + i].toLong() and 0xFFL)
        }
        return res
    }

    private fun readUpTo(input: InputStream, dest: ByteArray): Int {
        var total = 0
        while (total < dest.size) {
            val r = input.read(dest, total, dest.size - total)
            if (r <= 0) break
            total += r
        }
        return total
    }

    private fun readExact(input: InputStream, count: Int): ByteArray? {
        if (count < 0) return null
        val buf = ByteArray(count)
        var total = 0
        while (total < count) {
            val r = input.read(buf, total, count - total)
            if (r < 0) return null
            total += r
        }
        return buf
    }

    private fun skipFully(input: InputStream, count: Long): Boolean {
        var rem = count
        while (rem > 0L) {
            val skipped = input.skip(rem)
            if (skipped > 0L) {
                rem -= skipped
            } else {
                val b = input.read()
                if (b < 0) return false
                rem -= 1L
            }
        }
        return true
    }
}
