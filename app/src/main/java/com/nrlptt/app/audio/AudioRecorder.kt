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
        const val SAMPLE_RATE = 8000
        const val FRAME_MS = 20
        const val SAMPLES = SAMPLE_RATE * FRAME_MS / 1000
        const val BYTES = SAMPLES * 2
    }

    private var recorder: AudioRecord? = null
    private val running = AtomicBoolean(false)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    var onData: ((ByteArray) -> Unit)? = null

    fun start(): Boolean {
        if (running.get()) return true
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) return false
        return try {
            val bufSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
                .coerceAtLeast(BYTES * 5)
            recorder = AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufSize)
            if (recorder?.state != AudioRecord.STATE_INITIALIZED) { recorder?.release(); recorder = null; return false }
            running.set(true)
            scope.launch {
                Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
                val buf = ByteArray(BYTES)
                recorder?.startRecording()
                while (running.get()) {
                    try {
                        val n = recorder?.read(buf, 0, buf.size) ?: 0
                        if (n > 0) onData?.invoke(buf.copyOf(n))
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
