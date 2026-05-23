package com.nrlptt.app.audio

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import java.nio.ByteBuffer

class OpusCodec {

    companion object {
        private const val TAG = "OpusCodec"
        private const val SAMPLE_RATE = 16000
        private const val CHANNELS = 1
        private const val BITRATE = 6000  // 6kbps minimum bandwidth
        private const val FRAME_SIZE = 320  // 20ms @ 16kHz
    }

    private var encoder: MediaCodec? = null
    private var decoder: MediaCodec? = null

    fun initEncoder(): Boolean {
        return try {
            val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_OPUS, SAMPLE_RATE, CHANNELS)
            format.setInteger(MediaFormat.KEY_BIT_RATE, BITRATE)
            format.setInteger(MediaFormat.KEY_COMPLEXITY, 10)
            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_OPUS)
            encoder?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder?.start()
            Log.d(TAG, "Opus encoder started: ${SAMPLE_RATE}Hz, ${BITRATE}bps")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Opus encoder init failed", e)
            false
        }
    }

    fun initDecoder(): Boolean {
        return try {
            val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_OPUS, SAMPLE_RATE, CHANNELS)
            decoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_OPUS)
            decoder?.configure(format, null, null, 0)
            decoder?.start()
            Log.d(TAG, "Opus decoder started")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Opus decoder init failed", e)
            false
        }
    }

    fun encode(pcm16: ShortArray): ByteArray? {
        val enc = encoder ?: return null
        if (pcm16.size < FRAME_SIZE) return null

        val inputIndex = enc.dequeueInputBuffer(10000)
        if (inputIndex >= 0) {
            val buf = enc.getInputBuffer(inputIndex) ?: return null
            buf.clear()
            buf.asShortBuffer().put(pcm16, 0, FRAME_SIZE.coerceAtMost(pcm16.size))
            enc.queueInputBuffer(inputIndex, 0, FRAME_SIZE * 2, 0, 0)
        }

        val info = MediaCodec.BufferInfo()
        val outIndex = enc.dequeueOutputBuffer(info, 10000)
        if (outIndex >= 0) {
            val outBuf = enc.getOutputBuffer(outIndex) ?: return null
            val data = ByteArray(info.size)
            outBuf.position(info.offset)
            outBuf.get(data)
            enc.releaseOutputBuffer(outIndex, false)
            return data
        }
        return null
    }

    fun decode(opusData: ByteArray): ShortArray? {
        val dec = decoder ?: return null

        val inputIndex = dec.dequeueInputBuffer(10000)
        if (inputIndex >= 0) {
            val buf = dec.getInputBuffer(inputIndex) ?: return null
            buf.clear()
            buf.put(opusData)
            dec.queueInputBuffer(inputIndex, 0, opusData.size, 0, 0)
        }

        val info = MediaCodec.BufferInfo()
        val outIndex = dec.dequeueOutputBuffer(info, 10000)
        if (outIndex >= 0) {
            val outBuf = dec.getOutputBuffer(outIndex) ?: return null
            val samples = ShortArray(info.size / 2)
            outBuf.position(info.offset)
            outBuf.asShortBuffer().get(samples)
            dec.releaseOutputBuffer(outIndex, false)
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
