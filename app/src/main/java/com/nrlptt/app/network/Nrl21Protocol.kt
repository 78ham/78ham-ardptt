package com.nrlptt.app.network

import java.nio.ByteBuffer
import java.nio.ByteOrder

object Nrl21Protocol {
    const val HEADER = "NRL2"
    const val FIXED_SIZE = 48

    const val TYPE_VOICE      = 1
    const val TYPE_HEARTBEAT  = 2
    const val TYPE_TEXT       = 5
    const val TYPE_JOIN_GROUP = 7
    const val TYPE_OPUS       = 8

    data class Packet(
        val type: Int,
        val callSign: String,
        val ssid: Int,
        val devModel: Int,
        val dmrId: Int,
        val status: Int = 1,
        val count: Int = 0,
        val data: ByteArray = ByteArray(0)
    )

    fun createPacket(
        type: Int, callSign: String, ssid: Int, devModel: Int,
        dmrId: Int, data: ByteArray? = null
    ): ByteArray {
        val dataSize = data?.size ?: 0
        val buf = ByteBuffer.allocate(FIXED_SIZE + dataSize)
        buf.order(ByteOrder.BIG_ENDIAN)

        writeStr(buf, 0, HEADER, 4)
        buf.putShort(4, (FIXED_SIZE + dataSize).toShort())
        writeUint24(buf, 6, dmrId)
        buf.put(20, type.toByte())
        buf.put(21, 1)
        buf.putShort(22, 0)
        writeStr(buf, 24, callSign, 6)
        buf.put(30, ssid.toByte())
        buf.put(31, devModel.toByte())

        if (data != null) System.arraycopy(data, 0, buf.array(), FIXED_SIZE, dataSize)
        return buf.array()
    }

    fun decode(data: ByteArray): Packet? {
        if (data.size < FIXED_SIZE) return null
        val buf = ByteBuffer.wrap(data)
        buf.order(ByteOrder.BIG_ENDIAN)
        if (readStr(buf, 0, 4) != HEADER) return null

        return Packet(
            type = data[20].toInt() and 0xFF,
            callSign = readStr(buf, 24, 6).trim(),
            ssid = data[30].toInt() and 0xFF,
            devModel = data[31].toInt() and 0xFF,
            dmrId = readUint24(buf, 6),
            status = data[21].toInt() and 0xFF,
            count = ((data[22].toInt() and 0xFF) shl 8) or (data[23].toInt() and 0xFF),
            data = if (data.size > FIXED_SIZE) data.sliceArray(FIXED_SIZE until data.size) else ByteArray(0)
        )
    }

    private fun writeStr(buf: ByteBuffer, off: Int, s: String, len: Int) {
        for (i in 0 until len) buf.put(off + i, (if (i < s.length) s[i].code else 0).toByte())
    }

    private fun readStr(buf: ByteBuffer, off: Int, len: Int): String {
        val sb = StringBuilder()
        for (i in 0 until len) {
            val c = buf.get(off + i).toInt() and 0xFF
            if (c != 0) sb.append(c.toChar())
        }
        return sb.toString()
    }

    private fun writeUint24(buf: ByteBuffer, off: Int, v: Int) {
        buf.put(off, ((v shr 16) and 0xFF).toByte())
        buf.put(off + 1, ((v shr 8) and 0xFF).toByte())
        buf.put(off + 2, (v and 0xFF).toByte())
    }

    private fun readUint24(buf: ByteBuffer, off: Int): Int {
        return ((buf.get(off).toInt() and 0xFF) shl 16) or
                ((buf.get(off + 1).toInt() and 0xFF) shl 8) or
                (buf.get(off + 2).toInt() and 0xFF)
    }
}
