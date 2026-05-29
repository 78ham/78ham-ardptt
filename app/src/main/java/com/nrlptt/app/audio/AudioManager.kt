package com.nrlptt.app.audio

import android.content.Context
import android.util.Log
import com.nrlptt.app.data.AudioCodec
import com.nrlptt.app.network.Nrl21Protocol
import com.nrlptt.app.network.UdpClient
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioManager(private val context: Context) {

    companion object {
        private const val TAG = "AudioManager"
        private const val RX_TIMEOUT = 3000L
        private const val G711_RATE = 8000
    }

    private val recorder = AudioRecorder(context)
    private val player = AudioPlayer(context)
    private val g711 = G711Codec()
    private val opus = OpusCodec()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var audioCodec = AudioCodec.OPUS

    // Reallocated by setCodec to match the codec frame (G711 = 160 @8k, Opus = 320 @16k).
    private var txSamples = ShortArray(OpusCodec.FRAME_SIZE)
    private val rxPcmBuf = ByteBuffer.allocate(4096).order(ByteOrder.LITTLE_ENDIAN)

    // TX targets — set by PttService
    var getTxTargets: (() -> List<UdpClient>)? = null

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

    // RX packets arrive concurrently from every connected ServerConnection. Funnel them
    // through one consumer so the single OpusCodec decoder and rxPcmBuf (neither thread-safe)
    // are only ever touched by one coroutine. Drop oldest on overflow to favour fresh audio.
    private class RxItem(val data: ByteArray, val type: Int, val callsign: String)
    private val rxChannel = Channel<RxItem>(capacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    init {
        // RX can carry Opus from any peer regardless of our TX codec, so the decoder must
        // always be ready (it was previously only inited in the never-called preparePlayer).
        opus.initDecoder()

        scope.launch {
            for (item in rxChannel) processRx(item.data, item.type, item.callsign)
        }

        recorder.onData = { buf, len ->
            val frame = txSamples
            if (_tx.value && len >= frame.size * 2) {
                ByteBuffer.wrap(buf, 0, len).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(frame, 0, frame.size)
                val encoded = when (audioCodec) {
                    AudioCodec.G711 -> g711.encode(frame)
                    AudioCodec.OPUS -> opus.encode(frame)   // raw 16k Opus frame, or null on failure
                }
                if (encoded != null) {
                    val targets = getTxTargets?.invoke() ?: emptyList()
                    for (udp in targets) udp.sendAudio(encoded, audioCodec == AudioCodec.OPUS)
                }
            }
        }

        // Align recorder rate / tx frame with the default codec (PttService re-applies on connect).
        setCodec(audioCodec)
    }

    fun setCodec(c: AudioCodec) {
        audioCodec = c
        when (c) {
            AudioCodec.G711 -> {
                recorder.configure(G711_RATE)
                txSamples = ShortArray(G711_RATE * AudioRecorder.FRAME_MS / 1000)  // 160
            }
            AudioCodec.OPUS -> {
                recorder.configure(OpusCodec.SAMPLE_RATE)
                txSamples = ShortArray(OpusCodec.FRAME_SIZE)                        // 320
                opus.initEncoder()
            }
        }
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

    /** Called from arbitrary network coroutines — must stay cheap and thread-safe. */
    fun handleRx(data: ByteArray, type: Int, callsign: String) {
        if (_tx.value) return
        rxChannel.trySend(RxItem(data, type, callsign))
    }

    /** Runs only on the single rxChannel consumer coroutine — owns opus/g711/rxPcmBuf. */
    private fun processRx(data: ByteArray, type: Int, callsign: String) {
        if (_tx.value) return
        _speaker.value = callsign
        _rx.value = true
        lastAudioTime = System.currentTimeMillis()

        rxJob?.cancel()
        rxJob = scope.launch {
            delay(RX_TIMEOUT)
            if (System.currentTimeMillis() - lastAudioTime >= RX_TIMEOUT) {
                _rx.value = false; _level.value = 0f
            }
        }

        // Decode to PCM (G711 → 8k, Opus → 16k) and compute the level meter from the actual
        // samples (the encoded Opus/G711 payload bytes carry no usable amplitude info).
        val (samples, rate) = when (type) {
            Nrl21Protocol.TYPE_VOICE ->
                ShortArray(data.size) { g711.decode(data[it].toInt() and 0xFF).toShort() } to G711_RATE
            Nrl21Protocol.TYPE_OPUS ->
                (opus.decode(data) ?: return) to OpusCodec.SAMPLE_RATE
            else -> return
        }
        if (samples.isEmpty()) return

        var sum = 0L
        for (s in samples) sum += kotlin.math.abs(s.toInt())
        _level.value = (sum.toFloat() / samples.size / 32768f).coerceIn(0f, 1f)

        rxPcmBuf.clear()
        for (s in samples) rxPcmBuf.putShort(s)
        val out = ByteArray(rxPcmBuf.position()); rxPcmBuf.flip(); rxPcmBuf.get(out)
        player.feed(out, rate)
    }

    fun release() {
        rxJob?.cancel(); recorder.release(); player.release(); opus.release(); scope.cancel()
    }
}
