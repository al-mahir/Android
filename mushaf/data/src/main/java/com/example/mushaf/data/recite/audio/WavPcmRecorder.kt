package com.example.mushaf.data.recite.audio

import com.example.mushaf.domain.model.recite.AudioFrame
import com.example.mushaf.domain.model.recite.RecitationAudioFormat
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * A [PcmRecorder] that replays a 16 kHz mono PCM16 WAV instead of opening the microphone.
 *
 * Makes a recitation reproducible: the same audio, byte for byte, on every run. That turns "the
 * feedback looks wrong" from an argument about how someone recited into a repeatable test — and
 * it is the only way to debug the session protocol on an emulator with no working microphone.
 *
 * Pairs with [WavDebugSink]: pull a `raw-*.wav` off a device, drop it in, and replay it.
 *
 * @param open supplies a fresh stream per collection, so the flow can be collected more than once.
 * @param realTime when true, paces frames as if spoken. Leave false in tests; set true on a
 *   device, where sending a whole recitation instantly would defeat the server's VAD chunking.
 */
class WavPcmRecorder(
    private val open: () -> InputStream,
    private val realTime: Boolean = false,
) : PcmRecorder {

    override fun record(): Flow<AudioFrame> = flow {
        open().buffered().use { stream ->
            val format = stream.readWavHeader()
            check(format.sampleRate == RecitationAudioFormat.SAMPLE_RATE_HZ) {
                "WAV is ${format.sampleRate} Hz; the service requires " +
                    "${RecitationAudioFormat.SAMPLE_RATE_HZ} Hz — resample it rather than sending it"
            }
            check(format.channels == RecitationAudioFormat.CHANNEL_COUNT) {
                "WAV has ${format.channels} channels; the service requires mono"
            }
            check(format.bitsPerSample == RecitationAudioFormat.BITS_PER_SAMPLE) {
                "WAV is ${format.bitsPerSample}-bit; the service requires 16-bit PCM"
            }

            val bytes = ByteArray(RecitationAudioFormat.FRAME_BYTES)
            var startSample = 0L
            while (true) {
                val read = stream.readAtMost(bytes)
                if (read <= 0) break
                val samples = bytes.toShortsLittleEndian(read)
                emit(AudioFrame(samples, startSample))
                startSample += samples.size
                if (realTime) delay(RecitationAudioFormat.FRAME_DURATION_MS.toLong())
            }
        }
    }

    private data class WavFormat(val sampleRate: Int, val channels: Int, val bitsPerSample: Int)

    /**
     * Walks the RIFF chunk list to the `data` payload.
     *
     * Chunks are not at fixed offsets — writers interleave `LIST`/`fact` chunks freely — so
     * assuming the canonical 44-byte header would read metadata as audio on files this app did
     * not write.
     */
    private fun InputStream.readWavHeader(): WavFormat {
        val riff = ByteArray(12)
        check(readAtMost(riff) == 12) { "Not a WAV file: truncated RIFF header" }
        check(riff.ascii(0, 4) == "RIFF" && riff.ascii(8, 4) == "WAVE") {
            "Not a WAV file: missing RIFF/WAVE marker"
        }

        var format: WavFormat? = null
        val chunkHeader = ByteArray(8)
        while (readAtMost(chunkHeader) == 8) {
            val id = chunkHeader.ascii(0, 4)
            val size = chunkHeader.intLittleEndian(4)
            when (id) {
                "fmt " -> {
                    val body = ByteArray(size)
                    check(readAtMost(body) == size) { "Truncated fmt chunk" }
                    format = WavFormat(
                        channels = body.shortLittleEndian(2).toInt(),
                        sampleRate = body.intLittleEndian(4),
                        bitsPerSample = body.shortLittleEndian(14).toInt(),
                    )
                }
                // Payload starts here; leave the stream positioned on it.
                "data" -> return checkNotNull(format) { "WAV data chunk preceded its fmt chunk" }
                else -> skipFully(size.toLong())
            }
        }
        error("WAV has no data chunk")
    }

    private fun InputStream.skipFully(count: Long) {
        var remaining = count
        while (remaining > 0) {
            val skipped = skip(remaining)
            if (skipped <= 0) return
            remaining -= skipped
        }
    }

    /** [InputStream.read] may return a short read; loop until the buffer is full or the file ends. */
    private fun InputStream.readAtMost(target: ByteArray): Int {
        var filled = 0
        while (filled < target.size) {
            val read = read(target, filled, target.size - filled)
            if (read < 0) break
            filled += read
        }
        return filled
    }

    private fun ByteArray.toShortsLittleEndian(byteCount: Int): ShortArray {
        val buffer = ByteBuffer.wrap(this, 0, byteCount).order(ByteOrder.LITTLE_ENDIAN)
        return ShortArray(byteCount / Short.SIZE_BYTES) { buffer.short }
    }

    private fun ByteArray.ascii(offset: Int, length: Int): String =
        String(this, offset, length, Charsets.US_ASCII)

    private fun ByteArray.intLittleEndian(offset: Int): Int =
        ByteBuffer.wrap(this, offset, Int.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN).int

    private fun ByteArray.shortLittleEndian(offset: Int): Short =
        ByteBuffer.wrap(this, offset, Short.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN).short
}
