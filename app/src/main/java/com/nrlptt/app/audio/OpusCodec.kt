package com.nrlptt.app.audio

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder

class OpusCodec {

    companion object {
        private const val TAG = "OpusCodec"
        const val SAMPLE_RATE = 48000  // MediaCodec Opus requires 48kHz
        const val CHANNELS = 1
        const val BITRATE = 6000
        const val FRAME_MS = 20
        const val FRAME_SIZE = SAMPLE_RATE * FRAME_MS / 1000  // 960 samples
    }

    private var encoder: MediaCodec? = null
    private var decoder: MediaCodec? = null
    private val encInfo = MediaCodec.BufferInfo()
    private val decInfo = MediaCodec.BufferInfo()

    fun initEncoder(): Boolean {
        if (encoder != null) return true
        return try {
            val fmt = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_OPUS, SAMPLE_RATE, CHANNELS)
            fmt.setInteger(MediaFormat.KEY_BIT_RATE, BITRATE)
            fmt.setInteger(MediaFormat.KEY_COMPLEXITY, 10)
            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_OPUS)
            encoder?.configure(fmt, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder?.start()
            Log.d(TAG, "Encoder: ${SAMPLE_RATE}Hz ${BITRATE}bps")
            true
        } catch (e: Exception) { Log.e(TAG, "Encoder init failed", e); false }
    }

    fun initDecoder(): Boolean {
        if (decoder != null) return true
        return try {
            val fmt = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_OPUS, SAMPLE_RATE, CHANNELS)
            decoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_OPUS)
            decoder?.configure(fmt, null, null, 0)
            decoder?.start()
            Log.d(TAG, "Decoder started")
            true
        } catch (e: Exception) { Log.e(TAG, "Decoder init failed", e); false }
    }

    fun encode(pcm16: ShortArray): ByteArray? {
        val enc = encoder ?: return null
        if (pcm16.size < FRAME_SIZE) return null
        val inIdx = enc.dequeueInputBuffer(20000)
        if (inIdx >= 0) {
            val buf = enc.getInputBuffer(inIdx) ?: return null
            buf.clear()
            buf.asShortBuffer().put(pcm16, 0, FRAME_SIZE)
            enc.queueInputBuffer(inIdx, 0, FRAME_SIZE * 2, 0, 0)
        }
        val outIdx = enc.dequeueOutputBuffer(encInfo, 20000)
        if (outIdx >= 0) {
            val outBuf = enc.getOutputBuffer(outIdx) ?: return null
            val data = ByteArray(encInfo.size)
            outBuf.position(encInfo.offset)
            outBuf.get(data)
            enc.releaseOutputBuffer(outIdx, false)
            return data
        }
        return null
    }

    fun decode(opusData: ByteArray): ShortArray? {
        val dec = decoder ?: return null
        val inIdx = dec.dequeueInputBuffer(20000)
        if (inIdx >= 0) {
            val buf = dec.getInputBuffer(inIdx) ?: return null
            buf.clear()
            buf.put(opusData)
            dec.queueInputBuffer(inIdx, 0, opusData.size, 0, 0)
        }
        val outIdx = dec.dequeueOutputBuffer(decInfo, 20000)
        if (outIdx >= 0) {
            val outBuf = dec.getOutputBuffer(outIdx) ?: return null
            val samples = ShortArray(decInfo.size / 2)
            outBuf.position(decInfo.offset)
            outBuf.asShortBuffer().get(samples)
            dec.releaseOutputBuffer(outIdx, false)
            return samples
        }
        return null
    }

    fun release() {
        try { encoder?.stop(); encoder?.release() } catch (_: Exception) {}
        try { decoder?.stop(); decoder?.release() } catch (_: Exception) {}
        encoder = null; decoder = null
    }
}
