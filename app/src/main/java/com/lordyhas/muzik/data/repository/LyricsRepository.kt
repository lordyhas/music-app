package com.lordyhas.muzik.data.repository

import android.content.Context
import android.util.LruCache
import com.lordyhas.muzik.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.nio.charset.Charset

data class LyricLine(val timeMs: Long?, val text: String)

sealed interface LyricsState {
    data object Hidden : LyricsState
    data object Loading : LyricsState
    data object NotFound : LyricsState
    data class Loaded(val lyrics: Lyrics) : LyricsState
}

data class Lyrics(
    val lines: List<LyricLine>,
    /** True when every line has a timestamp (LRC / SYLT), enabling karaoke-style highlighting. */
    val synced: Boolean,
    val source: String
)

/**
 * Finds lyrics for a song, in order:
 * 1. a `.lrc` file with the same name next to the audio file (when storage access allows it),
 * 2. lyrics embedded in the file's tags: ID3v2 USLT/SYLT (MP3), Vorbis comments (FLAC/OGG/Opus),
 *    or the `©lyr` atom (M4A/AAC).
 * Tag text that itself contains LRC timestamps is treated as synced.
 */
class LyricsRepository(private val context: Context) {

    private val cache = LruCache<Long, Lyrics>(32)
    private val misses = HashSet<Long>()

    suspend fun load(song: Song): Lyrics? = withContext(Dispatchers.IO) {
        cache.get(song.id)?.let { return@withContext it }
        if (song.id in misses) return@withContext null

        val lyrics = try {
            readSidecarLrc(song) ?: readEmbedded(song)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
        if (lyrics != null) cache.put(song.id, lyrics) else misses.add(song.id)
        lyrics
    }

    // ─── .lrc next to the file ──────────────────────────────────────────────

    private fun readSidecarLrc(song: Song): Lyrics? {
        if (song.folderPath.isBlank() || song.fileName.isBlank()) return null
        val base = song.fileName.substringBeforeLast('.')
        val candidates = listOf("$base.lrc", "$base.LRC", "$base.txt")
        for (name in candidates) {
            val file = File(song.folderPath, name)
            val text = try {
                if (file.canRead()) file.readText() else null
            } catch (e: SecurityException) {
                null
            } ?: continue
            parse(text, source = name)?.let { return it }
        }
        return null
    }

    // ─── Embedded tags ──────────────────────────────────────────────────────

    private fun readEmbedded(song: Song): Lyrics? {
        // Formats with tags at a known position at the start of the file
        var hasHeaderTags = false
        val fromHeader = context.contentResolver.openInputStream(song.contentUri)?.use { input ->
            val head = ByteArray(10)
            if (input.readFully(head) < 4) return null
            when {
                head.startsWith("ID3") -> { hasHeaderTags = true; readId3(input, head) }
                head.startsWith("fLaC") -> { hasHeaderTags = true; readFlac(input, head) }
                else -> null
            }
        }
        if (hasHeaderTags) return fromHeader
        // Formats whose tags can live anywhere in the file: scan the bytes
        return context.contentResolver.openInputStream(song.contentUri)?.use { input ->
            val bytes = input.readUpTo(MAX_SCAN_BYTES)
            readVorbisCommentScan(bytes) ?: readMp4Lyr(bytes)
        }
    }

    /** ID3v2.3 / v2.4: USLT (plain) and SYLT (synced) frames. */
    private fun readId3(input: InputStream, header: ByteArray): Lyrics? {
        val major = header[3].toInt()
        val tagSize = syncSafe(header, 6)
        val tag = ByteArray(tagSize)
        input.readFully(tag)

        var pos = 0
        var plain: String? = null
        while (pos + 10 <= tag.size) {
            val id = String(tag, pos, 4, Charsets.ISO_8859_1)
            if (id[0] == '\u0000') break
            val size = if (major >= 4) syncSafe(tag, pos + 4) else int32(tag, pos + 4)
            val start = pos + 10
            if (size <= 0 || start + size > tag.size) break
            when (id) {
                "SYLT" -> parseSylt(tag, start, size)?.let { return it }
                "USLT" -> if (plain == null) plain = parseUslt(tag, start, size)
            }
            pos = start + size
        }
        return plain?.let { parse(it, source = "ID3 tag") }
    }

    private fun parseUslt(tag: ByteArray, start: Int, size: Int): String? {
        val encoding = tag[start].toInt()
        var p = start + 4 // encoding + 3-byte language
        val end = start + size
        p = skipTerminated(tag, p, end, encoding) // content descriptor
        if (p >= end) return null
        return decode(tag, p, end - p, encoding).trim().ifBlank { null }
    }

    private fun parseSylt(tag: ByteArray, start: Int, size: Int): Lyrics? {
        val encoding = tag[start].toInt()
        val timestampFormat = tag[start + 4].toInt() // 2 = milliseconds
        if (timestampFormat != 2) return null
        val end = start + size
        var p = skipTerminated(tag, start + 6, end, encoding)
        val lines = mutableListOf<LyricLine>()
        while (p < end) {
            val textEnd = findTerminator(tag, p, end, encoding)
            val text = decode(tag, p, textEnd - p, encoding)
            p = textEnd + terminatorLength(encoding)
            if (p + 4 > end) break
            lines += LyricLine(int32(tag, p).toLong(), text.trim())
            p += 4
        }
        return lines.takeIf { it.isNotEmpty() }?.let { Lyrics(it.sortedBy { l -> l.timeMs }, synced = true, source = "ID3 synced") }
    }

    /** FLAC metadata blocks → VORBIS_COMMENT (type 4). */
    private fun readFlac(input: InputStream, head: ByteArray): Lyrics? {
        // head holds "fLaC" + first 6 bytes of the first block header
        var blockHeader = byteArrayOf(head[4], head[5], head[6], head[7])
        var leftover = byteArrayOf(head[8], head[9])
        while (true) {
            val isLast = blockHeader[0].toInt() and 0x80 != 0
            val type = blockHeader[0].toInt() and 0x7F
            val length = ((blockHeader[1].toInt() and 0xFF) shl 16) or
                ((blockHeader[2].toInt() and 0xFF) shl 8) or (blockHeader[3].toInt() and 0xFF)
            val body = ByteArray(length)
            val pre = leftover.size.coerceAtMost(length)
            System.arraycopy(leftover, 0, body, 0, pre)
            input.readFully(body, pre)
            leftover = if (leftover.size > length) leftover.copyOfRange(length, leftover.size) else ByteArray(0)

            if (type == 4) return parseVorbisComments(body, 0)
            if (isLast) return null

            val next = ByteArray(4)
            val fromLeft = leftover.size.coerceAtMost(4)
            System.arraycopy(leftover, 0, next, 0, fromLeft)
            if (input.readFully(next, fromLeft) < 4) return null
            leftover = ByteArray(0)
            blockHeader = next
        }
    }

    private fun parseVorbisComments(data: ByteArray, offset: Int): Lyrics? {
        var p = offset
        val vendorLength = int32LE(data, p)
        p += 4 + vendorLength
        val count = int32LE(data, p)
        p += 4
        repeat(count) {
            if (p + 4 > data.size) return null
            val len = int32LE(data, p)
            p += 4
            if (len < 0 || p + len > data.size) return null
            val comment = String(data, p, len, Charsets.UTF_8)
            p += len
            val key = comment.substringBefore('=').uppercase()
            if (key == "LYRICS" || key == "UNSYNCEDLYRICS" || key == "SYNCEDLYRICS") {
                return parse(comment.substringAfter('='), source = "Vorbis comment")
            }
        }
        return null
    }

    /** OGG/Opus: find a "LYRICS=" comment and use its little-endian length prefix. */
    private fun readVorbisCommentScan(bytes: ByteArray): Lyrics? {
        for (key in listOf("LYRICS=", "lyrics=", "UNSYNCEDLYRICS=", "Lyrics=")) {
            val idx = bytes.indexOf(key.toByteArray())
            if (idx < 4) continue
            val len = int32LE(bytes, idx - 4)
            if (len <= key.length || idx + len > bytes.size) continue
            val text = String(bytes, idx + key.length, len - key.length, Charsets.UTF_8)
            return parse(text, source = "Vorbis comment")
        }
        return null
    }

    /** MP4: "©lyr" atom containing a "data" atom with UTF-8 text. */
    private fun readMp4Lyr(bytes: ByteArray): Lyrics? {
        val marker = byteArrayOf(0xA9.toByte(), 'l'.code.toByte(), 'y'.code.toByte(), 'r'.code.toByte())
        val idx = bytes.indexOf(marker)
        if (idx < 0 || idx + 16 > bytes.size) return null
        val dataSize = int32(bytes, idx + 4)
        if (String(bytes, idx + 8, 4, Charsets.ISO_8859_1) != "data") return null
        val textStart = idx + 16
        val textLength = dataSize - 16
        if (textLength <= 0 || textStart + textLength > bytes.size) return null
        return parse(String(bytes, textStart, textLength, Charsets.UTF_8), source = "MP4 tag")
    }

    private fun parse(raw: String, source: String): Lyrics? = parseLyrics(raw, source)

    // ─── Byte helpers ───────────────────────────────────────────────────────

    private fun ByteArray.startsWith(prefix: String) =
        size >= prefix.length && prefix.indices.all { this[it] == prefix[it].code.toByte() }

    private fun ByteArray.indexOf(needle: ByteArray): Int {
        outer@ for (i in 0..size - needle.size) {
            for (j in needle.indices) if (this[i + j] != needle[j]) continue@outer
            return i
        }
        return -1
    }

    private fun InputStream.readFully(buffer: ByteArray, offset: Int = 0): Int {
        var read = offset
        while (read < buffer.size) {
            val n = read(buffer, read, buffer.size - read)
            if (n < 0) break
            read += n
        }
        return read
    }

    private fun InputStream.readUpTo(max: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buf = ByteArray(64 * 1024)
        while (out.size() < max) {
            val n = read(buf)
            if (n < 0) break
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }

    private fun syncSafe(b: ByteArray, o: Int) =
        ((b[o].toInt() and 0x7F) shl 21) or ((b[o + 1].toInt() and 0x7F) shl 14) or
            ((b[o + 2].toInt() and 0x7F) shl 7) or (b[o + 3].toInt() and 0x7F)

    private fun int32(b: ByteArray, o: Int) =
        ((b[o].toInt() and 0xFF) shl 24) or ((b[o + 1].toInt() and 0xFF) shl 16) or
            ((b[o + 2].toInt() and 0xFF) shl 8) or (b[o + 3].toInt() and 0xFF)

    private fun int32LE(b: ByteArray, o: Int) =
        (b[o].toInt() and 0xFF) or ((b[o + 1].toInt() and 0xFF) shl 8) or
            ((b[o + 2].toInt() and 0xFF) shl 16) or ((b[o + 3].toInt() and 0xFF) shl 24)

    private fun charsetFor(encoding: Int): Charset = when (encoding) {
        1 -> Charsets.UTF_16
        2 -> Charsets.UTF_16BE
        3 -> Charsets.UTF_8
        else -> Charsets.ISO_8859_1
    }

    private fun terminatorLength(encoding: Int) = if (encoding == 1 || encoding == 2) 2 else 1

    private fun findTerminator(b: ByteArray, from: Int, end: Int, encoding: Int): Int {
        if (terminatorLength(encoding) == 1) {
            var i = from
            while (i < end && b[i] != 0.toByte()) i++
            return i
        }
        var i = from
        while (i + 1 < end && !(b[i] == 0.toByte() && b[i + 1] == 0.toByte())) i += 2
        return i.coerceAtMost(end)
    }

    private fun skipTerminated(b: ByteArray, from: Int, end: Int, encoding: Int) =
        findTerminator(b, from, end, encoding) + terminatorLength(encoding)

    private fun decode(b: ByteArray, offset: Int, length: Int, encoding: Int): String {
        if (length <= 0) return ""
        return String(b, offset, length, charsetFor(encoding)).trimEnd('\u0000')
    }

    private companion object {
        const val MAX_SCAN_BYTES = 16 * 1024 * 1024
    }
}

// ─── LRC parsing (top-level so it can be unit tested) ──────────────────

private val timeTag = Regex("""\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?]""")
private val offsetTag = Regex("""\[offset:\s*([+-]?\d+)]""", RegexOption.IGNORE_CASE)

/** Parses LRC text into timed lines, or falls back to plain lines. */
internal fun parseLyrics(raw: String, source: String): Lyrics? {
    val text = raw.replace("\r\n", "\n").replace('\r', '\n').trim()
    if (text.isBlank()) return null

    val offset = offsetTag.find(text)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
    val timed = mutableListOf<LyricLine>()
    text.lines().forEach { line ->
        val tags = timeTag.findAll(line).toList()
        if (tags.isEmpty()) return@forEach
        val lyric = line.substring(tags.last().range.last + 1).trim()
        tags.forEach { m ->
            val min = m.groupValues[1].toLong()
            val sec = m.groupValues[2].toLong()
            val frac = m.groupValues[3]
            val ms = when (frac.length) {
                0 -> 0L
                1 -> frac.toLong() * 100
                2 -> frac.toLong() * 10
                else -> frac.take(3).toLong()
            }
            timed += LyricLine((min * 60_000 + sec * 1000 + ms - offset).coerceAtLeast(0), lyric)
        }
    }

    return if (timed.size >= 2) {
        Lyrics(timed.sortedBy { it.timeMs }, synced = true, source = source)
    } else {
        // Plain lyrics: drop LRC metadata tags like [ar:...] if any
        val lines = text.lines()
            .filterNot { it.trim().matches(Regex("""\[[a-zA-Z]+:.*]""")) }
            .map { LyricLine(null, it.trim()) }
        Lyrics(lines, synced = false, source = source)
    }
}
