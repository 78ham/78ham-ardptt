package com.nrlptt.app.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

class WebSocketClient {

    companion object {
        private const val TAG = "WebSocket"
        private const val PING_INTERVAL = 20000L
    }

    private var connection: HttpURLConnection? = null
    private var reader: BufferedReader? = null
    private var writer: OutputStream? = null
    private val running = AtomicBoolean(false)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val gson = Gson()

    private val _roomStates = MutableStateFlow<Map<String, RoomState>>(emptyMap())
    val roomStates: StateFlow<Map<String, RoomState>> = _roomStates.asStateFlow()

    private val _recentCalls = MutableStateFlow<List<RecentCall>>(emptyList())
    val recentCalls: StateFlow<List<RecentCall>> = _recentCalls.asStateFlow()

    private val _onlineDevices = MutableStateFlow(0)
    val onlineDevices: StateFlow<Int> = _onlineDevices.asStateFlow()

    data class RoomState(val roomKey: String, val roomId: Int, val roomName: String, val active: Boolean, val speakers: List<String>)
    data class RecentCall(val callsign: String, val ssid: Int, val roomName: String, val duration: String, val active: Boolean)

    fun connect(host: String, token: String) {
        if (running.get()) return
        running.set(true)
        scope.launch {
            try {
                val url = URL("https://$host/ws/calls?token=$token")
                val conn = url.openConnection() as HttpURLConnection
                conn.apply {
                    requestMethod = "GET"
                    setRequestProperty("Upgrade", "websocket")
                    setRequestProperty("Connection", "Upgrade")
                    connectTimeout = 10000; readTimeout = 30000
                }
                connection = conn
                reader = BufferedReader(InputStreamReader(conn.inputStream))
                writer = conn.outputStream

                // Send initial ping
                sendJson(mapOf("action" to "ping"))

                // Read loop
                val mapType = object : TypeToken<Map<String, Any>>() {}.type
                while (running.get()) {
                    val line = reader?.readLine() ?: break
                    if (line.isBlank()) continue
                    try {
                        val msg = gson.fromJson<Map<String, Any>>(line, mapType)
                        handleMessage(msg)
                    } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                if (running.get()) Log.e(TAG, "WS error: ${e.message}")
            } finally {
                disconnect()
            }
        }

        // Ping loop
        scope.launch {
            while (running.get()) {
                delay(PING_INTERVAL)
                try { sendJson(mapOf("action" to "ping")) } catch (_: Exception) { break }
            }
        }
    }

    private fun handleMessage(msg: Map<String, Any>) {
        when (msg["type"] as? String) {
            "snapshot" -> {
                @Suppress("UNCHECKED_CAST")
                val rooms = msg["rooms"] as? List<Map<String, Any>> ?: emptyList()
                val states = rooms.associate { r ->
                    val key = r["room_key"] as? String ?: ""
                    key to RoomState(
                        roomKey = key,
                        roomId = (r["room_id"] as? Number)?.toInt() ?: 0,
                        roomName = r["room_name"] as? String ?: "",
                        active = r["active"] as? Boolean ?: false,
                        speakers = (r["speakers"] as? List<Map<String, Any>>)?.map { "${it["callsign"]}-${it["ssid"]}" } ?: emptyList()
                    )
                }
                _roomStates.value = states
                _onlineDevices.value = (msg["online_devices"] as? Number)?.toInt() ?: 0

                @Suppress("UNCHECKED_CAST")
                val calls = msg["recent_calls"] as? List<Map<String, Any>> ?: emptyList()
                _recentCalls.value = calls.map { c ->
                    RecentCall(
                        callsign = c["callsign"] as? String ?: "",
                        ssid = (c["ssid"] as? Number)?.toInt() ?: 0,
                        roomName = c["room_name"] as? String ?: "",
                        duration = c["duration_text"] as? String ?: "",
                        active = c["active"] as? Boolean ?: false
                    )
                }
            }
            "room_state" -> {
                @Suppress("UNCHECKED_CAST")
                val r = msg["room"] as? Map<String, Any> ?: return
                val key = r["room_key"] as? String ?: return
                val state = RoomState(
                    roomKey = key,
                    roomId = (r["room_id"] as? Number)?.toInt() ?: 0,
                    roomName = r["room_name"] as? String ?: "",
                    active = r["active"] as? Boolean ?: false,
                    speakers = (r["speakers"] as? List<Map<String, Any>>)?.map { "${it["callsign"]}-${it["ssid"]}" } ?: emptyList()
                )
                _roomStates.value = _roomStates.value + (key to state)
            }
            "stats" -> {
                _onlineDevices.value = (msg["online_devices"] as? Number)?.toInt() ?: 0
            }
        }
    }

    private fun sendJson(obj: Any) {
        val json = gson.toJson(obj) + "\n"
        writer?.write(json.toByteArray()); writer?.flush()
    }

    fun disconnect() {
        running.set(false)
        try { reader?.close(); writer?.close(); connection?.disconnect() } catch (_: Exception) {}
        connection = null; reader = null; writer = null
    }

    fun release() { disconnect(); scope.cancel() }
}
