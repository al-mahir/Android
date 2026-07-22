package com.example.mushaf.data.recite.audio

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.example.mushaf.data.MushafLog
import com.example.mushaf.domain.model.recite.AudioFrame
import com.example.mushaf.domain.model.recite.RecitationAudioFormat
import java.io.Closeable
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Writes captured audio to a playable `.wav` under `cacheDir/recitation-debug/`, on debuggable
 * builds only.
 *
 * This exists because every failure mode of a capture pipeline sounds different and looks
 * identical: a wrong sample rate, a wrong byte order, a stereo buffer read as mono and a wrong
 * audio source all produce "the model returns nonsense". Listening to one file settles it in
 * seconds — a chipmunk pitch is the sample rate, static is the byte order.
 *
 * Retrieve the newest capture with:
 * ```
 * adb exec-out run-as com.iti.al_mahir sh -c 'cat cache/recitation-debug/capture-*' > capture.wav
 * ```
 *
 * The service itself expects *headerless* PCM — the 44-byte header here is for the desktop
 * player only, and is never sent over the socket (sending it is the first mistake API.md §5.3
 * lists).
 */
class WavDebugSink(
    private val context: Context,
    private val enabled: Boolean = context.isDebuggable(),
    private val maxFiles: Int = DEFAULT_MAX_FILES,
) {

    /**
     * Opens a new capture file, or returns `null` on release builds and when the file cannot be
     * created. A null return is never fatal: debug capture must not be able to break recording.
     *
     * [label] distinguishes streams captured in the same session — `raw` against `gated` is how
     * the speech gate is verified, by listening to both.
     */
    fun open(label: String = "capture"): WavFileWriter? {
        if (!enabled) return null
        return runCatching {
            val directory = File(context.cacheDir, DIRECTORY_NAME).apply { mkdirs() }
            pruneOldest(directory)
            val file = File(directory, "$label-${System.currentTimeMillis()}.wav")
            WavFileWriter(file).also {
                Log.i(MushafLog.TAG, "Debug capture → ${file.absolutePath}")
            }
        }.onFailure {
            Log.w(MushafLog.TAG, "Debug capture unavailable; recording continues", it)
        }.getOrNull()
    }

    /** Keeps the debug directory bounded — these are throwaway files in the cache. */
    private fun pruneOldest(directory: File) {
        val files = directory.listFiles { file -> file.extension == "wav" } ?: return
        if (files.size < maxFiles) return
        files.sortedBy { it.lastModified() }
            .take(files.size - maxFiles + 1)
            .forEach { it.delete() }
    }

    private companion object {
        const val DIRECTORY_NAME = "recitation-debug"

        /** A session writes a raw and a gated file, so this holds a few sessions' worth. */
        const val DEFAULT_MAX_FILES = 6
    }
}

/**
 * A single WAV file being written. The RIFF header carries byte counts that are only known once
 * writing finishes, so a placeholder goes down first and [close] seeks back and patches it. A
 * file whose [close] never ran is therefore unplayable — hence the `use`/`finally` at the call
 * site.
 */
class WavFileWriter internal constructor(val file: File) : Closeable {

    private val output = RandomAccessFile(file, "rw")
    private var dataBytes = 0

    init {
        output.setLength(0)
        output.write(ByteArray(HEADER_BYTES)) // Placeholder, patched in close().
    }

    fun write(frame: AudioFrame) {
        val bytes = PcmCodec.toLittleEndianBytes(frame.samples)
        output.write(bytes)
        dataBytes += bytes.size
    }

    val durationMs: Long
        get() = RecitationAudioFormat.durationMsOf(
            (dataBytes / RecitationAudioFormat.BYTES_PER_SAMPLE).toLong(),
        )

    override fun close() {
        try {
            output.seek(0)
            output.write(riffHeader(dataBytes))
        } finally {
            output.close()
        }
        Log.i(
            MushafLog.TAG,
            "Debug capture closed: ${file.name}, $dataBytes bytes, ${durationMs}ms",
        )
    }

    private fun riffHeader(dataByteCount: Int): ByteArray {
        val sampleRate = RecitationAudioFormat.SAMPLE_RATE_HZ
        val channels = RecitationAudioFormat.CHANNEL_COUNT
        val bitsPerSample = RecitationAudioFormat.BITS_PER_SAMPLE
        val byteRate = sampleRate * channels * RecitationAudioFormat.BYTES_PER_SAMPLE
        val blockAlign = channels * RecitationAudioFormat.BYTES_PER_SAMPLE

        return ByteBuffer.allocate(HEADER_BYTES).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(CHUNK_SIZE_PREFIX_BYTES + dataByteCount) // Everything after this field.
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(PCM_SUBCHUNK_SIZE)
            putShort(PCM_FORMAT_TAG)
            putShort(channels.toShort())
            putInt(sampleRate)
            putInt(byteRate)
            putShort(blockAlign.toShort())
            putShort(bitsPerSample.toShort())
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(dataByteCount)
        }.array()
    }

    private companion object {
        const val HEADER_BYTES = 44
        const val CHUNK_SIZE_PREFIX_BYTES = 36
        const val PCM_SUBCHUNK_SIZE = 16
        const val PCM_FORMAT_TAG: Short = 1
    }
}

/**
 * True on a debuggable build. Read from the manifest flag rather than a `BuildConfig` constant
 * so `:mushaf:data` does not need `buildConfig = true` for one boolean.
 */
internal fun Context.isDebuggable(): Boolean =
    (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
