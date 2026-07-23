package com.example.mushaf.data.recite.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder




 
object PcmCodec {

    






 
    fun toLittleEndianBytes(samples: ShortArray, count: Int = samples.size): ByteArray {
        require(count in 0..samples.size) { "count=$count outside 0..${samples.size}" }
        val buffer = ByteBuffer.allocate(count * Short.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN)
        for (index in 0 until count) {
            buffer.putShort(samples[index])
        }
        return buffer.array()
    }
}
