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
        private const val HEARTBEAT_MS = 1000L
        private const val TIMEOUT_MS = 6000L
        private const val MAX_RECONNECT = 20
        private const val BASE_DELAY = 2000L
        private const val MAX_DELAY = 60000L
    }

    private var socket: DatagramSocket? = null
    private var addr: InetAddress? = null
    private var port: Int = 0
    private var host: String = ""

    private val _state = MutableStateFlow(ConnectionState.DISCONNECTED)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val running = AtomicBoolean(false)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    @Volatile private var lastPacketTime = 0L
    @Volatile private var reconnects = 0
    private var receiveJob: Job? = null
    private var heartbeatJob: Job? = null
    private var monitorJob: Job? = null

    var callsign: String = ""
    var ssid: Int = 178
    var dmrId: Int = 178
    var devModel: Int = 101

    var onPacket: ((Nrl21Protocol.Packet) -> Unit)? = null

    fun connect(host: String, port: Int, dmrId: Int, call: String, ssid: Int, devModel: Int): Boolean {
        if (running.get()) disconnect()
        this.host = host; this.port = port; this.dmrId = dmrId
        this.callsign = call; this.ssid = ssid; this.devModel = devModel
        _state.value = ConnectionState.CONNECTING
        if (!openSocket()) { _state.value = ConnectionState.DISCONNECTED; return false }
        running.set(true); lastPacketTime = System.currentTimeMillis(); reconnects = 0
        // Loops live for the whole connection; a reconnect only swaps the socket (see startMonitor),
        // so they must be started exactly once here — never on the reconnect path.
        startReceive(); startHeartbeat(); startMonitor()
        _state.value = ConnectionState.CONNECTED
        Log.d(TAG, "Connected $host:$port")
        return true
    }

    /**
     * (Re)open the UDP socket. Closing the previous socket unblocks any in-flight
     * receive() so the existing receive loop picks up the new socket next iteration.
     */
    private fun openSocket(): Boolean {
        return try {
            addr = InetAddress.getByName(host)
            socket?.close()
            socket = DatagramSocket().apply { soTimeout = 5000 }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Socket open failed", e); false
        }
    }

    fun disconnect() {
        running.set(false)
        receiveJob?.cancel(); receiveJob = null
        heartbeatJob?.cancel(); heartbeatJob = null
        monitorJob?.cancel(); monitorJob = null
        socket?.close(); socket = null
        _state.value = ConnectionState.DISCONNECTED
    }

    fun send(data: ByteArray): Boolean {
        val targetSocket = socket ?: return false
        val targetAddr = addr ?: return false
        val targetPort = port
        return try {
            targetSocket.send(DatagramPacket(data, data.size, targetAddr, targetPort))
            true
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
        receiveJob?.cancel()
        receiveJob = scope.launch {
            val buf = ByteArray(BUFFER)
            val pkt = DatagramPacket(buf, buf.size)
            while (running.get()) {
                try {
                    pkt.length = buf.size
                    val activeSocket = socket ?: continue
                    activeSocket.receive(pkt)
                    lastPacketTime = System.currentTimeMillis()
                    if (reconnects > 0) { reconnects = 0; Log.d(TAG, "Connection recovered") }
                    val data = buf.copyOf(pkt.length)
                    Nrl21Protocol.decode(data)?.let { onPacket?.invoke(it) }
                } catch (_: CancellationException) { break }
                catch (e: Exception) { if (running.get()) Log.e(TAG, "Recv err", e) }
            }
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (running.get()) {
                try {
                    send(Nrl21Protocol.createHeartbeat(callsign, ssid, devModel, dmrId))
                    delay(HEARTBEAT_MS)
                } catch (_: CancellationException) { break }
            }
        }
    }

    private fun startMonitor() {
        monitorJob?.cancel()
        monitorJob = scope.launch {
            while (running.get()) {
                delay(3000)
                if (!running.get()) break
                if (System.currentTimeMillis() - lastPacketTime > TIMEOUT_MS) {
                    _state.value = ConnectionState.DISCONNECTED
                    if (reconnects < MAX_RECONNECT) {
                        reconnects++
                        val delayMs = (BASE_DELAY * (1L shl (reconnects - 1).coerceAtMost(4))).coerceAtMost(MAX_DELAY)
                        Log.w(TAG, "Timeout, reconnect #$reconnects in ${delayMs}ms")
                        delay(delayMs)
                        if (!running.get()) break
                        // Swap the socket only — keep looping. reconnects resets to 0 in the
                        // receive loop once a packet actually arrives (true recovery).
                        val ok = openSocket()
                        if (ok) { lastPacketTime = System.currentTimeMillis(); _state.value = ConnectionState.CONNECTED }
                    } else {
                        Log.e(TAG, "Max reconnects reached")
                        disconnect()
                        break
                    }
                }
            }
        }
    }
}
