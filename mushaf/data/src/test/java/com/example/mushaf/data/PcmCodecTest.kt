package com.example.mushaf.data

import com.example.mushaf.data.recite.audio.PcmCodec
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class PcmCodecTest {

    @Test
    fun `samples are encoded little-endian`() {
        
        
        val bytes = PcmCodec.toLittleEndianBytes(shortArrayOf(0x0102))

        assertArrayEquals(byteArrayOf(0x02, 0x01), bytes)
    }

    @Test
    fun `negative samples keep their two's complement bytes`() {
        val bytes = PcmCodec.toLittleEndianBytes(shortArrayOf(-1, Short.MIN_VALUE))

        assertArrayEquals(
            byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0x00, 0x80.toByte()),
            bytes,
        )
    }

    @Test
    fun `output is two bytes per sample`() {
        val bytes = PcmCodec.toLittleEndianBytes(ShortArray(1600))

        assertEquals(3200, bytes.size)
    }

    @Test
    fun `a partial read encodes only the samples that were filled`() {
        // AudioRecord may return fewer samples than the buffer holds; the tail is stale data
        // from the previous read and must not reach the wire.
        val buffer = shortArrayOf(1, 2, 3, 4, 5, 6)

        val bytes = PcmCodec.toLittleEndianBytes(buffer, count = 2)

        assertArrayEquals(byteArrayOf(0x01, 0x00, 0x02, 0x00), bytes)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a count past the end of the buffer is rejected`() {
        PcmCodec.toLittleEndianBytes(ShortArray(4), count = 5)
    }
}
