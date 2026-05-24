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

    // Header byte constants
    private const val H0 = 'N'.code.toByte()
    private const val H1 = 'R'.code.toByte()
    private const val H2 = 'L'.code.toByte()
    private const val H3 = '2'.code.toByte()

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

    // Reusable heartbeat template (48 bytes, only callsign/ssid/dmrid change)
    private val heartbeatTemplate = ByteArray(FIXED_SIZE).also { buf ->
        buf[0] = H0; buf[1] = H1; buf[2] = H2; buf[3] = H3
        // length = 48 (big endian)
        buf[4] = 0; buf[5] = FIXED_SIZE.toByte()
        // type = 2
        buf[20] = TYPE_HEARTBEAT.toByte()
        buf[21] = 1  // status
    }

    fun createPacket(
        type: Int, callSign: String, ssid: Int, devModel: Int,
        dmrId: Int, data: ByteArray? = null
    ): ByteArray {
        val dataSize = data?.size ?: 0
        val total = FIXED_SIZE + dataSize
        val buf = ByteArray(total)

        // Header "NRL2"
        buf[0] = H0; buf[1] = H1; buf[2] = H2; buf[3] = H3
        // Length (big endian short)
        buf[4] = ((total shr 8) and 0xFF).toByte()
        buf[5] = (total and 0xFF).toByte()
        // DMR ID (3 bytes big endian)
        buf[6] = ((dmrId shr 16) and 0xFF).toByte()
        buf[7] = ((dmrId shr 8) and 0xFF).toByte()
        buf[8] = (dmrId and 0xFF).toByte()
        // Password: zeros (9-19)
        // Type
        buf[20] = type.toByte()
        // Status
        buf[21] = 1
        // Count: 0
        buf[22] = 0; buf[23] = 0
        // CallSign (6 bytes)
        val csLen = callSign.length.coerceAtMost(6)
        for (i in 0 until csLen) buf[24 + i] = callSign[i].code.toByte()
        // SSID + DevModel
        buf[30] = ssid.toByte()
        buf[31] = devModel.toByte()
        // Data payload
        if (data != null) System.arraycopy(data, 0, buf, FIXED_SIZE, dataSize)
        return buf
    }

    fun createHeartbeat(callSign: String, ssid: Int, devModel: Int, dmrId: Int): ByteArray {
        val buf = heartbeatTemplate.copyOf()
        buf[6] = ((dmrId shr 16) and 0xFF).toByte()
        buf[7] = ((dmrId shr 8) and 0xFF).toByte()
        buf[8] = (dmrId and 0xFF).toByte()
        val csLen = callSign.length.coerceAtMost(6)
        for (i in 0 until csLen) buf[24 + i] = callSign[i].code.toByte()
        buf[30] = ssid.toByte()
        buf[31] = devModel.toByte()
        return buf
    }

    fun decode(data: ByteArray): Packet? {
        if (data.size < FIXED_SIZE) return null
        // Fast header check (no ByteBuffer needed)
        if (data[0] != H0 || data[1] != H1 || data[2] != H2 || data[3] != H3) return null

        val dmrId = ((data[6].toInt() and 0xFF) shl 16) or
                ((data[7].toInt() and 0xFF) shl 8) or
                (data[8].toInt() and 0xFF)

        // CallSign: read up to 6 bytes, stop at null
        val cs = StringBuilder(6)
        for (i in 24 until 30) {
            val c = data[i].toInt() and 0xFF
            if (c == 0) break
            cs.append(c.toChar())
        }

        return Packet(
            type = data[20].toInt() and 0xFF,
            callSign = cs.toString(),
            ssid = data[30].toInt() and 0xFF,
            devModel = data[31].toInt() and 0xFF,
            dmrId = dmrId,
            status = data[21].toInt() and 0xFF,
            count = ((data[22].toInt() and 0xFF) shl 8) or (data[23].toInt() and 0xFF),
            data = if (data.size > FIXED_SIZE) data.copyOfRange(FIXED_SIZE, data.size) else ByteArray(0)
        )
    }
}
