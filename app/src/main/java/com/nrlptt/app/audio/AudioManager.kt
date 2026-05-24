package com.nrlptt.app.audio

import android.content.Context
import android.util.Log
import com.nrlptt.app.data.AudioCodec
import com.nrlptt.app.network.Nrl21Protocol
import com.nrlptt.app.network.UdpClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioManager(private val context: Context, private val udp: UdpClient) {

    companion object {
        private const val TAG = "AudioManager"
        private const val RX_TIMEOUT = 3000L
    }

    private val recorder = AudioRecorder(context)
    private val player = AudioPlayer(context)
    private val g711 = G711Codec()
    private val opus = OpusCodec()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var audioCodec = AudioCodec.OPUS

    // Reusable buffers to avoid GC pressure on hot path
    private val txSamples = ShortArray(AudioRecorder.SAMPLES)
    private val rxPcmBuf = ByteBuffer.allocate(4096).order(ByteOrder.LITTLE_ENDIAN)

    private val _tx = MutableStateFlow(false)
    val isTransmitting: StateFlow<Boolean> = _tx.asStateFlow()

    private val _rx = MutableStateFlow(false)
    val isReceiving: StateFlow<Boolean> = _rx.asStateFlow()

    private val _speaker = MutableStateFlow("")
    val currentSpeaker: StateFlow<String> = _speaker.asStateFlow()

    private val _level = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _level.asStateFlow()

    private var lastAudioTime = 0L
    private var rxJob: Job? = null

    init {
        recorder.onData = { buf, len ->
            if (!_tx.value || len < AudioRecorder.BYTES) return@onData
            // Convert PCM bytes to shorts in-place (zero-copy on buffer)
            ByteBuffer.wrap(buf, 0, len).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(txSamples, 0, len / 2)
            val encoded = when (audioCodec) {
                AudioCodec.G711 -> g711.encode(txSamples)
                AudioCodec.OPUS -> opus.encode(txSamples) ?: buf.copyOf(len)
            }
            udp.sendAudio(encoded, audioCodec == AudioCodec.OPUS)
        }
    }

    fun setCodec(c: AudioCodec) {
        audioCodec = c
        if (c == AudioCodec.OPUS) opus.initEncoder()
    }

    fun setVolume(v: Int) { player.setVolume(v / 100f) }
    fun preparePlayer() { player.start(); opus.initDecoder() }

    fun startTx(): Boolean {
        if (_tx.value) return true
        player.pause()
        if (audioCodec == AudioCodec.OPUS) opus.initEncoder()
        val ok = recorder.start()
        if (ok) _tx.value = true else player.resume()
        return ok
    }

    fun stopTx() {
        recorder.stop(); _tx.value = false; player.resume()
    }

    fun handleRx(data: ByteArray, type: Int, callsign: String) {
        if (_tx.value) return
        _speaker.value = callsign
        _rx.value = true
        lastAudioTime = System.currentTimeMillis()

        // Audio level: simple RMS approximation
        var sum = 0L
        for (i in data.indices step 2) sum += (data[i].toInt() and 0xFF)
        _level.value = (sum.toFloat() / (data.size / 2) / 255f).coerceIn(0f, 1f)

        rxJob?.cancel()
        rxJob = scope.launch {
            delay(RX_TIMEOUT)
            if (System.currentTimeMillis() - lastAudioTime >= RX_TIMEOUT) {
                _rx.value = false; _level.value = 0f
            }
        }

        val pcm = when (type) {
            Nrl21Protocol.TYPE_VOICE -> {
                // G711 decode directly into reusable buffer
                rxPcmBuf.clear()
                for (i in data.indices) {
                    rxPcmBuf.putShort(g711.decode(data[i].toInt() and 0xFF).toShort())
                }
                val out = ByteArray(rxPcmBuf.position())
                rxPcmBuf.flip()
                rxPcmBuf.get(out)
                out
            }
            Nrl21Protocol.TYPE_OPUS -> {
                val decoded = opus.decode(data) ?: return
                rxPcmBuf.clear()
                for (s in decoded) rxPcmBuf.putShort(s)
                val out = ByteArray(rxPcmBuf.position())
                rxPcmBuf.flip()
                rxPcmBuf.get(out)
                out
            }
            else -> return
        }
        player.feed(pcm)
    }

    fun release() {
        rxJob?.cancel(); recorder.release(); player.release(); opus.release(); scope.cancel()
    }
}
