package com.example.mushaf.data.recite.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Conversion between the `ShortArray` the recorder produces and the byte layout the AI service
 * reads off the WebSocket.
 */
object PcmCodec {

    /**
     * Signed 16-bit samples as little-endian bytes.
     *
     * The byte order is set explicitly rather than inherited: [ByteBuffer]'s default is
     * big-endian, which would be accepted by the socket and decoded as noise — a failure that
     * looks like a bad model rather than a bad client. API.md §10 calls this out for the same
     * reason.
     */
    fun toLittleEndianBytes(samples: ShortArray, count: Int = samples.size): ByteArray {
        require(count in 0..samples.size) { "count=$count outside 0..${samples.size}" }
        val buffer = ByteBuffer.allocate(count * Short.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN)
        for (index in 0 until count) {
            buffer.putShort(samples[index])
        }
        return buffer.array()
    }
}
