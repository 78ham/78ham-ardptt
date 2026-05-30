package com.nrlptt.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.nrlptt.app.R
import com.nrlptt.app.audio.AudioManager
import com.nrlptt.app.data.ServerConfig
import com.nrlptt.app.data.SettingsRepository
import com.nrlptt.app.network.*
import com.nrlptt.app.ptt.PttController
import com.nrlptt.app.ui.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

class PttService : Service() {

    companion object {
        private const val TAG = "PttService"
        private const val CH_ID = "nrl_ptt_channel"
        private const val NOTIF_ID = 1
        @Volatile var isRunning = false
        @Volatile var instance: PttService? = null
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var settings: SettingsRepository
    private lateinit var audio: AudioManager
    private lateinit var ptt: PttController
    private lateinit var locationReporter: LocationReporter

    private val _connections = MutableStateFlow<Map<String, ServerConnection>>(emptyMap())
    val connections: StateFlow<Map<String, ServerConnection>> = _connections.asStateFlow()

    private val _activeServerId = MutableStateFlow("")
    val activeServerId: StateFlow<String> = _activeServerId.asStateFlow()

    private val _isTransmitting = MutableStateFlow(false)
    val isTransmitting: StateFlow<Boolean> = _isTransmitting.asStateFlow()

    private val _isReceiving = MutableStateFlow(false)
    val isReceiving: StateFlow<Boolean> = _isReceiving.asStateFlow()

    // Reactive views across all servers — the UI collects these so it recomposes when any
    // connection's messages / activity / connection-state changes.
    @OptIn(ExperimentalCoroutinesApi::class)
    val allMessages: StateFlow<List<ServerConnection.MessageEntry>> =
        _connections.flatMapLatest { conns ->
            if (conns.isEmpty()) flowOf(emptyList<ServerConnection.MessageEntry>())
            else combine(conns.values.map { it.messages }) { arr ->
                arr.flatMap { it }.sortedByDescending { it.time }.take(5)
            }
        }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val allActivity: StateFlow<List<ServerConnection.ActivityEntry>> =
        _connections.flatMapLatest { conns ->
            if (conns.isEmpty()) flowOf(emptyList<ServerConnection.ActivityEntry>())
            else combine(conns.values.map { it.activityLog }) { arr ->
                arr.flatMap { it }.sortedByDescending { it.time }.take(5)
            }
        }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val anyConnected: StateFlow<Boolean> =
        _connections.flatMapLatest { conns ->
            if (conns.isEmpty()) flowOf(false)
            else combine(conns.values.map { it.connState }) { states ->
                states.any { it == ConnectionState.CONNECTED }
            }
        }.stateIn(scope, SharingStarted.Eagerly, false)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        settings = SettingsRepository(this)
        audio = AudioManager(this)
        ptt = PttController(this)
        locationReporter = LocationReporter(this).apply {
            getTargets = { _connections.value.values.filter { it.connState.value == ConnectionState.CONNECTED }.map { it.udp } }
            getCallsign = { _connections.value.values.firstOrNull()?.userInfo?.callsign ?: "" }
            getSsid = { _connections.value.values.firstOrNull()?.deviceData?.ssid ?: 178 }
            getDevModel = { _connections.value.values.firstOrNull()?.deviceData?.devModel ?: 101 }
            getDmrId = { _connections.value.values.firstOrNull()?.deviceData?.dmrId ?: 178 }
        }

        // Wire audio TX to all connected UdpClients
        audio.getTxTargets = { _connections.value.values.filter { it.connState.value == ConnectionState.CONNECTED }.map { it.udp } }

        // Sync RX state
        scope.launch { audio.isReceiving.collect { _isReceiving.value = it } }

        setupPtt()
        createChannel()
        locationReporter.start()
        instance = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, notification("待机"))
        isRunning = true
        val servers = settings.loadServers()
        for (cfg in servers) {
            if (cfg.autoConnect && cfg.username.isNotEmpty()) {
                scope.launch { loginAndConnect(cfg) }
            }
        }
        if (_activeServerId.value.isEmpty() && servers.isNotEmpty()) {
            _activeServerId.value = servers.first().id
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnectAll(); audio.release(); ptt.destroy(); locationReporter.release(); scope.cancel()
        isRunning = false; instance = null
    }

    private fun setupPtt() {
        val s = settings.load()
        ptt.init(s.pttKeyCode, s.screenOffPtt)
        ptt.listener = object : PttController.Listener {
            override fun onPress() { startTx() }
            override fun onRelease() { stopTx() }
            override fun onLongPress() {
                // Connect all disconnected auto-connect servers
                val servers = settings.loadServers()
                for (cfg in servers) {
                    val conn = _connections.value[cfg.id]
                    if (conn == null || conn.connState.value == ConnectionState.DISCONNECTED) {
                        scope.launch { loginAndConnect(cfg) }
                    }
                }
            }
        }
    }

    suspend fun loginAndConnect(config: ServerConfig): Boolean = withContext(Dispatchers.IO) {
        val existing = _connections.value[config.id]
        if (existing != null && existing.isLoggedIn.value) return@withContext true

        val conn = ServerConnection(config, scope)
        conn.addPacketListener { pkt ->
            scope.launch {
                when (pkt.type) {
                    Nrl21Protocol.TYPE_VOICE, Nrl21Protocol.TYPE_OPUS -> {
                        audio.handleRx(pkt.data, pkt.type, pkt.callSign)
                        conn.addActivity("${pkt.callSign}-${pkt.ssid}", pkt.ssid, "RX")
                    }
                    Nrl21Protocol.TYPE_TEXT -> Unit
                }
            }
        }
        _connections.value = _connections.value + (config.id to conn)
        if (_activeServerId.value.isEmpty()) _activeServerId.value = config.id

        val s = settings.load()
        audio.setCodec(s.codec); audio.setVolume(s.volume)

        val ok = conn.login(config.username, config.password)
        if (ok) {
            updateNotification("已连接 ${_connections.value.size} 个服务器")
        }
        ok
    }

    fun disconnectServer(id: String) {
        _connections.value[id]?.disconnect()
        _connections.value = _connections.value - id
        if (_activeServerId.value == id) {
            _activeServerId.value = _connections.value.keys.firstOrNull() ?: ""
        }
    }

    fun disconnectAll() {
        _connections.value.values.forEach { it.disconnect() }
        _connections.value = emptyMap()
        _activeServerId.value = ""
    }

    fun setActiveServer(id: String) { _activeServerId.value = id }

    fun getActiveConnection(): ServerConnection? = _connections.value[_activeServerId.value]

    fun startTx(): Boolean {
        val connected = _connections.value.values.any { it.connState.value == ConnectionState.CONNECTED }
        if (!connected) return false
        val ok = audio.startTx()
        if (ok) _isTransmitting.value = true
        return ok
    }

    fun stopTx() { audio.stopTx(); _isTransmitting.value = false }

    fun handleKey(event: android.view.KeyEvent): Boolean = ptt.onKey(event)

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CH_ID, "NRL PTT", NotificationManager.IMPORTANCE_LOW).apply {
                description = "PTT Service"; setSound(null, null)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    private fun notification(text: String): Notification {
        val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CH_ID)
            .setContentTitle("NRL PTT").setContentText(text)
            .setSmallIcon(R.drawable.ic_mic).setContentIntent(pi).setOngoing(true).build()
    }

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java).notify(NOTIF_ID, notification(text))
    }
}
