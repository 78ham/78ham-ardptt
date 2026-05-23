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
    private val codec = G711Codec()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var audioCodec = AudioCodec.G711

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
        recorder.onData = { pcm ->
            if (!_tx.value || pcm.size < AudioRecorder.BYTES) return@onData
            val frame = pcm.copyOf(AudioRecorder.BYTES)
            val samples = ShortArray(frame.size / 2)
            ByteBuffer.wrap(frame).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(samples)
            val encoded = when (audioCodec) {
                AudioCodec.G711 -> codec.encode(samples)
                AudioCodec.OPUS -> frame
            }
            udp.sendAudio(encoded, audioCodec == AudioCodec.OPUS)
        }
    }

    fun setCodec(c: AudioCodec) { audioCodec = c }
    fun setVolume(v: Int) { player.setVolume(v / 100f) }
    fun preparePlayer() { player.start() }

    fun startTx(): Boolean {
        if (_tx.value) return true
        player.pause()
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

        // Calculate simple audio level
        var sum = 0L
        for (b in data) sum += (b.toInt() and 0xFF)
        _level.value = (sum.toFloat() / data.size / 255f).coerceIn(0f, 1f)

        rxJob?.cancel()
        rxJob = scope.launch {
            delay(RX_TIMEOUT)
            if (System.currentTimeMillis() - lastAudioTime >= RX_TIMEOUT) {
                _rx.value = false; _level.value = 0f
            }
        }

        val pcm = when (type) {
            Nrl21Protocol.TYPE_VOICE -> {
                val samples = ShortArray(data.size)
                for (i in data.indices) samples[i] = codec.decode(data[i].toInt() and 0xFF).toShort()
                ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN).apply { asShortBuffer().put(samples) }.array()
            }
            Nrl21Protocol.TYPE_OPUS -> data
            else -> return
        }
        player.feed(pcm)
    }

    fun release() {
        rxJob?.cancel(); recorder.release(); player.release(); scope.cancel()
    }
}
