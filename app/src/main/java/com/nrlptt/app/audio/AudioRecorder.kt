package com.nrlptt.app.audio

import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Process
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

class AudioRecorder(private val context: Context) {

    companion object {
        private const val TAG = "AudioRecorder"
        const val FRAME_MS = 20
    }

    // Capture rate / frame size depend on the active codec (G711 = 8k/160, Opus = 16k/320).
    @Volatile var sampleRate = 8000
        private set
    @Volatile var frameSamples = sampleRate * FRAME_MS / 1000
        private set
    val frameBytes: Int get() = frameSamples * 2

    private var recorder: AudioRecord? = null
    private val running = AtomicBoolean(false)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    var onData: ((ByteArray, Int) -> Unit)? = null  // (buffer, validBytes) — no copy

    /** Set capture format. Must be called while not recording. */
    fun configure(sampleRate: Int) {
        this.sampleRate = sampleRate
        this.frameSamples = sampleRate * FRAME_MS / 1000
    }

    fun start(): Boolean {
        if (running.get()) return true
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) return false
        return try {
            val frameBytes = this.frameBytes
            val bufSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
                .coerceAtLeast(frameBytes * 5)
            recorder = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufSize)
            if (recorder?.state != AudioRecord.STATE_INITIALIZED) { recorder?.release(); recorder = null; return false }
            running.set(true)
            scope.launch {
                Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
                val buf = ByteArray(frameBytes)
                recorder?.startRecording()
                while (running.get()) {
                    try {
                        val n = recorder?.read(buf, 0, buf.size) ?: 0
                        if (n > 0) onData?.invoke(buf, n)
                    } catch (_: CancellationException) { break }
                    catch (e: Exception) { Log.e(TAG, "Record err", e) }
                }
            }
            true
        } catch (e: Exception) { Log.e(TAG, "Start failed", e); false }
    }

    fun stop() {
        running.set(false)
        recorder?.stop(); recorder?.release(); recorder = null
    }

    fun release() { stop(); scope.cancel() }
}
