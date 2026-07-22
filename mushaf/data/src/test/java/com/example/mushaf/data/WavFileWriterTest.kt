package com.example.mushaf.data

import com.example.mushaf.data.recite.audio.WavFileWriter
import com.example.mushaf.domain.model.recite.AudioFrame
import com.example.mushaf.domain.model.recite.RecitationAudioFormat
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * The debug WAV only earns its keep if a desktop player opens it. A header that misreports the
 * sample rate would make correctly-captured audio play back at the wrong pitch — sending the
 * next debugging session after a bug that does not exist.
 */
class WavFileWriterTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `header describes 16 kHz mono PCM16 and the real payload size`() {
        val frameCount = 3
        val file = temporaryFolder.newFile("capture.wav")

        WavFileWriter(file).use { writer ->
            repeat(frameCount) { index ->
                writer.write(
                    AudioFrame(
                        samples = ShortArray(RecitationAudioFormat.FRAME_SAMPLES) { 1234 },
                        startSample = index.toLong() * RecitationAudioFormat.FRAME_SAMPLES,
                    ),
                )
            }
        }

        val bytes = file.readBytes()
        val expectedPayload = frameCount * RecitationAudioFormat.FRAME_BYTES
        assertEquals(HEADER_BYTES + expectedPayload, bytes.size)

        val header = ByteBuffer.wrap(bytes, 0, HEADER_BYTES).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals("RIFF", header.ascii(4))
        assertEquals("chunk size", 36 + expectedPayload, header.int)
        assertEquals("WAVE", header.ascii(4))
        assertEquals("fmt ", header.ascii(4))
        assertEquals("fmt chunk size", 16, header.int)
        assertEquals("PCM format tag", 1, header.short.toInt())
        assertEquals("channels", RecitationAudioFormat.CHANNEL_COUNT, header.short.toInt())
        assertEquals("sample rate", RecitationAudioFormat.SAMPLE_RATE_HZ, header.int)
        assertEquals("byte rate", RecitationAudioFormat.SAMPLE_RATE_HZ * 2, header.int)
        assertEquals("block align", 2, header.short.toInt())
        assertEquals("bits per sample", RecitationAudioFormat.BITS_PER_SAMPLE, header.short.toInt())
        assertEquals("data", header.ascii(4))
        assertEquals("data size", expectedPayload, header.int)
    }

    @Test
    fun `samples land in the payload little-endian`() {
        val file = temporaryFolder.newFile("one-sample.wav")

        WavFileWriter(file).use { writer ->
            writer.write(AudioFrame(shortArrayOf(0x0102), startSample = 0))
        }

        val payload = file.readBytes().drop(HEADER_BYTES)
        assertEquals(listOf<Byte>(0x02, 0x01), payload)
    }

    @Test
    fun `an empty capture still produces a valid zero-length file`() {
        val file = temporaryFolder.newFile("empty.wav")

        WavFileWriter(file).close()

        val bytes = file.readBytes()
        assertEquals(HEADER_BYTES, bytes.size)
        val header = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        header.position(DATA_SIZE_OFFSET)
        assertEquals(0, header.int)
    }

    @Test
    fun `duration is reported from the sample rate`() {
        val file = temporaryFolder.newFile("duration.wav")

        WavFileWriter(file).use { writer ->
            // Exactly one second of audio: 10 frames of 100 ms.
            repeat(10) {
                writer.write(AudioFrame(ShortArray(RecitationAudioFormat.FRAME_SAMPLES), 0))
            }
            assertEquals(1_000L, writer.durationMs)
        }
    }

    private fun ByteBuffer.ascii(length: Int): String {
        val chars = ByteArray(length)
        get(chars)
        return String(chars, Charsets.US_ASCII)
    }

    private companion object {
        const val HEADER_BYTES = 44
        const val DATA_SIZE_OFFSET = 40
    }
}
