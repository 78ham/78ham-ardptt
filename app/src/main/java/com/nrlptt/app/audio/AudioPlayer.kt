package com.nrlptt.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class AudioPlayer(private val context: Context) {

    companion object {
        private const val TAG = "AudioPlayer"
        private const val MAX_QUEUE = 50
        private const val DEFAULT_RATE = 16000
    }

    private var track: AudioTrack? = null
    private val playing = AtomicBoolean(false)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val queue = LinkedBlockingQueue<ByteArray>(MAX_QUEUE)
    private var playJob: Job? = null

    @Volatile private var sampleRate = DEFAULT_RATE
    private var volume = 1f

    fun start(rate: Int = sampleRate): Boolean {
        if (playing.get() && rate == sampleRate && track != null) return true
        // Rate change while playing → rebuild the track at the new rate.
        if (track != null) stop()
        sampleRate = rate
        return try {
            val bufSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            track = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                .setAudioFormat(AudioFormat.Builder()
                    .setSampleRate(sampleRate).setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(bufSize)
                .setTransferMode(AudioTrack.MODE_STREAM).build()
            track?.setVolume(volume)
            track?.play(); playing.set(true)
            startPlayLoop()
            true
        } catch (e: Exception) { Log.e(TAG, "Start failed", e); false }
    }

    fun pause() {
        playing.set(false)
        playJob?.cancel(); playJob = null
        track?.pause()
    }

    fun resume() {
        if (playing.get()) return
        queue.clear()
        val activeTrack = track ?: return
        activeTrack.play(); playing.set(true)
        startPlayLoop()
    }

    private fun startPlayLoop() {
        playJob?.cancel()
        playJob = scope.launch {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
            while (playing.get()) {
                val d = queue.poll(2, TimeUnit.MILLISECONDS)
                if (d != null) track?.write(d, 0, d.size)
            }
        }
    }

    /** Feed PCM produced at [rate] Hz; (re)opens the track if the rate changed. */
    fun feed(pcm: ByteArray, rate: Int) {
        if ((!playing.get() || rate != sampleRate || track == null) && !start(rate)) return
        if (!queue.offer(pcm)) { queue.poll(); queue.offer(pcm) }
    }

    fun setVolume(v: Float) {
        volume = v.coerceIn(0f, 1f)
        track?.setVolume(volume)
    }

    fun stop() {
        playing.set(false)
        playJob?.cancel(); playJob = null
        track?.stop(); track?.release(); track = null; queue.clear()
    }

    fun release() { stop(); scope.cancel() }
}
