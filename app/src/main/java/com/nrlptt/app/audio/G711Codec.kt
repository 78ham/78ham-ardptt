package com.nrlptt.app.audio

class G711Codec {
    companion object {
        private lateinit var encTable: ByteArray
        private lateinit var decTable: ShortArray
        private var inited = false
    }

    init {
        if (!inited) {
            encTable = ByteArray(65536)
            decTable = ShortArray(256)
            for (i in -32768..32767) encTable[i + 32768] = linear2alaw(i).toByte()
            for (i in 0..255) decTable[i] = alaw2linear(i).toShort()
            inited = true
        }
    }

    private fun linear2alaw(sample: Int): Int {
        var s = sample; var sign = 0
        if (s < 0) { sign = 0x80; s = s.inv() }
        s = s shr 4
        var ix = s
        if (ix > 15) {
            var e = 1; while (ix > 31) { ix = ix shr 1; e++ }
            ix -= 16; ix += e shl 4
        }
        if (sign == 0) ix = ix or 0x80
        return ix xor 0x55
    }

    private fun alaw2linear(code: Int): Int {
        val c = code xor 0x55
        val seg = (c and 0x70) shr 4
        var s = ((c and 0x0f) shl 4) or 0x08
        if (seg > 0) s = (s + 0x100) shl (seg - 1)
        return if ((c and 0x80) != 0) s else -s
    }

    fun encode(pcm: ShortArray): ByteArray {
        val out = ByteArray(pcm.size)
        for (i in pcm.indices) out[i] = encTable[(pcm[i] + 32768) and 0xffff]
        return out
    }

    fun decode(code: Int): Int = decTable[code and 0xff].toInt()
}
