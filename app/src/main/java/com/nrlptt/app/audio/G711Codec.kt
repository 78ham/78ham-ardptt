package com.nrlptt.app.audio

class G711Codec {
    companion object {
        val encTable: ByteArray = ByteArray(65536).also { enc ->
            for (i in -32768..32767) {
                var s = i; var sign = 0
                if (s < 0) { sign = 0x80; s = s.inv() }
                s = s shr 4; var ix = s
                if (ix > 15) {
                    var e = 1; while (ix > 31) { ix = ix shr 1; e++ }
                    ix -= 16; ix += e shl 4
                }
                if (sign == 0) ix = ix or 0x80
                enc[i + 32768] = (ix xor 0x55).toByte()
            }
        }
        val decTable: ShortArray = ShortArray(256).also { dec ->
            for (i in 0..255) {
                val c = i xor 0x55
                val seg = (c and 0x70) shr 4
                var s = ((c and 0x0f) shl 4) or 0x08
                if (seg > 0) s = (s + 0x100) shl (seg - 1)
                dec[i] = (if ((c and 0x80) != 0) s else -s).toShort()
            }
        }
    }

    fun encode(pcm: ShortArray): ByteArray {
        val out = ByteArray(pcm.size)
        for (i in pcm.indices) out[i] = encTable[(pcm[i] + 32768) and 0xffff]
        return out
    }

    fun decode(code: Int): Int = decTable[code and 0xff].toInt()
}
