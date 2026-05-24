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
    }

    private var track: AudioTrack? = null
    private val playing = AtomicBoolean(false)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val queue = LinkedBlockingQueue<ByteArray>(MAX_QUEUE)
    private var playJob: Job? = null

    fun start(): Boolean {
        if (playing.get()) return true
        return try {
            val bufSize = AudioTrack.getMinBufferSize(8000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            track = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                .setAudioFormat(AudioFormat.Builder()
                    .setSampleRate(8000).setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(bufSize)
                .setTransferMode(AudioTrack.MODE_STREAM).build()
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
        track?.play(); playing.set(true)
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

    fun feed(pcm: ByteArray) {
        if (!playing.get()) start()
        if (!queue.offer(pcm)) queue.poll(); queue.offer(pcm)
    }

    fun setVolume(v: Float) { track?.setVolume(v.coerceIn(0f, 1f)) }

    fun stop() {
        playing.set(false)
        playJob?.cancel(); playJob = null
        track?.stop(); track?.release(); track = null; queue.clear()
    }

    fun release() { stop(); scope.cancel() }
}
