package com.example.mushaf.data.recite.audio

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.example.mushaf.data.MushafLog
import com.example.mushaf.domain.model.recite.AudioFrame
import com.example.mushaf.domain.model.recite.RecitationAudioFormat
import java.io.BufferedOutputStream
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder


class WavDebugSink(
    private val context: Context,
    private val enabled: Boolean = context.isDebuggable(),
    private val maxFiles: Int = DEFAULT_MAX_FILES,
) {

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

     
    private fun pruneOldest(directory: File) {
        val files = directory.listFiles { file -> file.extension == "wav" } ?: return
        if (files.size < maxFiles) return
        files.sortedBy { it.lastModified() }
            .take(files.size - maxFiles + 1)
            .forEach { it.delete() }
    }

    private companion object {
        const val DIRECTORY_NAME = "recitation-debug"

         
        const val DEFAULT_MAX_FILES = 6
    }
}






 
class WavFileWriter internal constructor(val file: File) : Closeable {

    /**
     * Buffered, not a [RandomAccessFile]: [write] is called from the live capture path, twice per
     * 100ms frame (raw and gated), and an unbuffered write is a syscall each time - twenty a
     * second sitting directly in front of the audio the server is waiting for. The header is the
     * only thing that needs random access, and it is patched once at [close].
     */
    private val output = BufferedOutputStream(FileOutputStream(file), BUFFER_BYTES)
    private var dataBytes = 0

    init {
        output.write(ByteArray(HEADER_BYTES))
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
        output.close()
        // Only now that every sample is on disk is the byte count final; reopening to patch the
        // 44-byte header is cheaper than keeping a seekable handle open for the whole session.
        RandomAccessFile(file, "rw").use { handle ->
            handle.seek(0)
            handle.write(riffHeader(dataBytes))
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
            putInt(CHUNK_SIZE_PREFIX_BYTES + dataByteCount) 
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

        /** 64KB - about two seconds of 16kHz mono PCM16, so the capture path touches the disk
         * roughly every other second instead of ten times a second. */
        const val BUFFER_BYTES = 64 * 1024

        const val CHUNK_SIZE_PREFIX_BYTES = 36
        const val PCM_SUBCHUNK_SIZE = 16
        const val PCM_FORMAT_TAG: Short = 1
    }
}




 
internal fun Context.isDebuggable(): Boolean =
    (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
