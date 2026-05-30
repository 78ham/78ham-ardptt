package com.nrlptt.app.audio

import android.util.Log
import io.github.jaredmdobson.concentus.OpusApplication
import io.github.jaredmdobson.concentus.OpusDecoder
import io.github.jaredmdobson.concentus.OpusEncoder

/**
 * Opus codec backed by Concentus (pure-Java libopus port).
 *
 * Wire format per NRL21 Type=8 (see server decode.go): raw Opus frames,
 * 16 kHz / mono / 20 ms (320 samples) / VOIP / complexity 10, VBR.
 * No Ogg container — each packet payload is exactly one raw Opus frame.
 */
class OpusCodec {

    companion object {
        private const val TAG = "OpusCodec"
        const val SAMPLE_RATE = 16000
        const val CHANNELS = 1
        const val FRAME_MS = 20
        const val FRAME_SIZE = SAMPLE_RATE * FRAME_MS / 1000  // 320 samples / 20ms
        const val BITRATE = 24000                              // ~24 kbps VBR, in the 32-40k spec range but lighter for 公网
        private const val MAX_PACKET = 1275                    // max bytes in one Opus frame
        private const val MAX_DECODE_SAMPLES = SAMPLE_RATE * 120 / 1000  // 120ms worst case
    }

    private var encoder: OpusEncoder? = null
    private var decoder: OpusDecoder? = null
    private val encBuf = ByteArray(MAX_PACKET)
    private val decBuf = ShortArray(MAX_DECODE_SAMPLES)

    fun initEncoder(): Boolean {
        if (encoder != null) return true
        return try {
            encoder = OpusEncoder(SAMPLE_RATE, CHANNELS, OpusApplication.OPUS_APPLICATION_VOIP).apply {
                setBitrate(BITRATE)
                // Concentus is pure-Java; complexity 5 keeps encode within the 20ms realtime
                // budget on low-end MTK/D12 CPUs. Complexity is encoder-local, not a wire param.
                setComplexity(5)
            }
            Log.d(TAG, "Encoder: ${SAMPLE_RATE}Hz ${BITRATE}bps")
            true
        } catch (e: Exception) { Log.e(TAG, "Encoder init failed", e); false }
    }

    fun initDecoder(): Boolean {
        if (decoder != null) return true
        return try {
            decoder = OpusDecoder(SAMPLE_RATE, CHANNELS)
            Log.d(TAG, "Decoder started")
            true
        } catch (e: Exception) { Log.e(TAG, "Decoder init failed", e); false }
    }

    /** Encode exactly one 20 ms frame (320 samples @16k). Returns the raw Opus packet. */
    fun encode(pcm16: ShortArray): ByteArray? {
        val enc = encoder ?: return null
        if (pcm16.size < FRAME_SIZE) return null
        return try {
            val len = enc.encode(pcm16, 0, FRAME_SIZE, encBuf, 0, encBuf.size)
            if (len > 0) encBuf.copyOf(len) else null
        } catch (e: Exception) { Log.e(TAG, "encode failed", e); null }
    }

    /** Decode one raw Opus packet to 16k PCM samples. */
    fun decode(opusData: ByteArray): ShortArray? {
        val dec = decoder ?: return null
        if (opusData.isEmpty()) return null
        return try {
            val n = dec.decode(opusData, 0, opusData.size, decBuf, 0, decBuf.size, false)
            if (n > 0) decBuf.copyOf(n) else null
        } catch (e: Exception) { Log.e(TAG, "decode failed", e); null }
    }

    fun release() {
        encoder = null
        decoder = null
    }
}
