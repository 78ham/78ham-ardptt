package com.nrlptt.app.network

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicBoolean

class UdpClient {

    companion object {
        private const val TAG = "UdpClient"
        private const val BUFFER = 2048
        private const val HEARTBEAT_MS = 2000L
        private const val TIMEOUT_MS = 30000L
        private const val MAX_RECONNECT = 5
    }

    private var socket: DatagramSocket? = null
    private var addr: InetAddress? = null
    private var port: Int = 0
    private var host: String = ""

    private val _state = MutableStateFlow(ConnectionState.DISCONNECTED)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val running = AtomicBoolean(false)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var lastPacketTime = 0L
    private var reconnects = 0

    var callsign: String = ""
    var ssid: Int = 78
    var dmrId: Int = 0
    var devModel: Int = 101

    var onPacket: ((Nrl21Protocol.Packet) -> Unit)? = null

    fun connect(host: String, port: Int, dmrId: Int, call: String, ssid: Int, devModel: Int): Boolean {
        return try {
            this.host = host; this.port = port; this.dmrId = dmrId
            this.callsign = call; this.ssid = ssid; this.devModel = devModel
            _state.value = ConnectionState.CONNECTING
            addr = InetAddress.getByName(host)
            socket = DatagramSocket().apply { soTimeout = 5000 }
            running.set(true); lastPacketTime = System.currentTimeMillis(); reconnects = 0
            startReceive(); startHeartbeat(); startMonitor()
            _state.value = ConnectionState.CONNECTED
            Log.d(TAG, "Connected $host:$port")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Connect failed", e); _state.value = ConnectionState.DISCONNECTED; false
        }
    }

    fun disconnect() {
        running.set(false)
        socket?.close(); socket = null
        _state.value = ConnectionState.DISCONNECTED
    }

    fun send(data: ByteArray): Boolean {
        return try {
            val a = addr ?: return false
            socket?.send(DatagramPacket(data, data.size, a, port)); true
        } catch (e: Exception) { false }
    }

    fun sendAudio(audioData: ByteArray, isOpus: Boolean = false) {
        if (_state.value != ConnectionState.CONNECTED) return
        val type = if (isOpus) Nrl21Protocol.TYPE_OPUS else Nrl21Protocol.TYPE_VOICE
        send(Nrl21Protocol.createPacket(type, callsign, ssid, devModel, dmrId, audioData))
    }

    fun sendJoinGroup(groupId: Int) {
        if (_state.value != ConnectionState.CONNECTED) return
        val data = ByteArray(5)
        data[0] = 0x01
        data[1] = ((groupId shr 24) and 0xFF).toByte()
        data[2] = ((groupId shr 16) and 0xFF).toByte()
        data[3] = ((groupId shr 8) and 0xFF).toByte()
        data[4] = (groupId and 0xFF).toByte()
        send(Nrl21Protocol.createPacket(Nrl21Protocol.TYPE_JOIN_GROUP, callsign, ssid, devModel, dmrId, data))
    }

    private fun startReceive() {
        scope.launch {
            val buf = ByteArray(BUFFER)
            while (running.get()) {
                try {
                    val pkt = DatagramPacket(buf, buf.size)
                    socket?.receive(pkt)
                    lastPacketTime = System.currentTimeMillis()
                    Nrl21Protocol.decode(pkt.data.copyOf(pkt.length))?.let { onPacket?.invoke(it) }
                } catch (_: CancellationException) { break }
                catch (e: Exception) { if (running.get()) Log.e(TAG, "Recv err", e) }
            }
        }
    }

    private fun startHeartbeat() {
        scope.launch {
            while (running.get()) {
                try {
                    send(Nrl21Protocol.createPacket(Nrl21Protocol.TYPE_HEARTBEAT, callsign, ssid, devModel, dmrId))
                    delay(HEARTBEAT_MS)
                } catch (_: CancellationException) { break }
            }
        }
    }

    private fun startMonitor() {
        scope.launch {
            while (running.get()) {
                delay(5000)
                if (System.currentTimeMillis() - lastPacketTime > TIMEOUT_MS) {
                    _state.value = ConnectionState.DISCONNECTED
                    if (reconnects < MAX_RECONNECT) {
                        reconnects++; delay(2000)
                        connect(host, port, dmrId, callsign, ssid, devModel)
                    } else disconnect()
                    break
                }
            }
        }
    }
}
