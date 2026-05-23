package com.nrlptt.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.nrlptt.app.R
import com.nrlptt.app.audio.AudioManager
import com.nrlptt.app.data.SettingsRepository
import com.nrlptt.app.network.ApiClient
import com.nrlptt.app.network.ConnectionState
import com.nrlptt.app.network.Nrl21Protocol
import com.nrlptt.app.network.UdpClient
import com.nrlptt.app.ptt.PttController
import com.nrlptt.app.ui.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
    private lateinit var udp: UdpClient
    private lateinit var audio: AudioManager
    private lateinit var ptt: PttController

    private val _connState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connState: StateFlow<ConnectionState> = _connState.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentRoom = MutableStateFlow(0)
    val currentRoom: StateFlow<Int> = _currentRoom.asStateFlow()

    private val _roomName = MutableStateFlow("")
    val roomName: StateFlow<String> = _roomName.asStateFlow()

    private val _onlineCount = MutableStateFlow(0)
    val onlineCount: StateFlow<Int> = _onlineCount.asStateFlow()

    private val _lastSpeaker = MutableStateFlow("")
    val lastSpeaker: StateFlow<String> = _lastSpeaker.asStateFlow()

    private val _activityLog = MutableStateFlow<List<ActivityEntry>>(emptyList())
    val activityLog: StateFlow<List<ActivityEntry>> = _activityLog.asStateFlow()

    private val _isTransmitting = MutableStateFlow(false)
    val isTransmitting: StateFlow<Boolean> = _isTransmitting.asStateFlow()

    private val _isReceiving = MutableStateFlow(false)
    val isReceiving: StateFlow<Boolean> = _isReceiving.asStateFlow()

    private val _roomList = MutableStateFlow<List<ApiClient.RoomInfo>>(emptyList())
    val roomList: StateFlow<List<ApiClient.RoomInfo>> = _roomList.asStateFlow()

    private var userInfo: ApiClient.UserInfo? = null
    private var deviceData: ApiClient.DeviceData? = null
    private var refreshJob: Job? = null

    data class ActivityEntry(val callsign: String, val ssid: Int, val time: String, val type: ActivityType)
    enum class ActivityType { TX, RX, JOIN, LEAVE, SYSTEM }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        settings = SettingsRepository(this)
        udp = UdpClient()
        audio = AudioManager(this, udp)
        ptt = PttController(this)
        setupNetwork(); setupPtt(); createChannel()
        instance = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, notification("Standby"))
        isRunning = true
        val s = settings.load()
        if (s.autoConnect && s.username.isNotEmpty()) scope.launch { loginAndConnect() }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnect(); audio.release(); ptt.release(); udp.disconnect(); scope.cancel()
        isRunning = false
        instance = null
    }

    private fun setupNetwork() {
        udp.onPacket = { pkt ->
            scope.launch {
                _connState.value = ConnectionState.CONNECTED
                when (pkt.type) {
                    Nrl21Protocol.TYPE_VOICE, Nrl21Protocol.TYPE_OPUS -> {
                        audio.handleRx(pkt.data, pkt.type, pkt.callSign)
                        _lastSpeaker.value = "${pkt.callSign}-${pkt.ssid}"
                        _isReceiving.value = true
                        addActivity("${pkt.callSign}-${pkt.ssid}", pkt.ssid, ActivityType.RX)
                    }
                }
            }
        }
        // Sync receiving state from audio manager
        scope.launch {
            audio.isReceiving.collect { rx -> _isReceiving.value = rx }
        }
        scope.launch {
            udp.state.collect { s ->
                _connState.value = s
                updateNotif(when (s) {
                    ConnectionState.CONNECTED -> "Connected"
                    ConnectionState.CONNECTING -> "Connecting..."
                    ConnectionState.DISCONNECTED -> "Disconnected"
                })
            }
        }
    }

    private fun setupPtt() {
        val s = settings.load()
        ptt.init(s.pttKeyCode, s.screenOffPtt)
        ptt.listener = object : PttController.Listener {
            override fun onPress() { startTx() }
            override fun onRelease() { stopTx() }
            override fun onLongPress() { if (!_isLoggedIn.value) scope.launch { loginAndConnect() } }
        }
    }

    fun handleKey(event: android.view.KeyEvent): Boolean = ptt.onKey(event)

    suspend fun loginAndConnect(): Boolean = withContext(Dispatchers.IO) {
        val s = settings.load()
        if (s.username.isEmpty()) return@withContext false
        _connState.value = ConnectionState.CONNECTING
        val result = ApiClient.login(s.serverAddress, s.username, s.password)
        result.fold(
            onSuccess = { info ->
                userInfo = info; _isLoggedIn.value = true
                try { deviceData = ApiClient.getDevice(s.serverAddress, info.callsign, 100).getOrNull() } catch (_: Exception) {}
                connectUdp(info); startRefresh(); true
            },
            onFailure = { _connState.value = ConnectionState.DISCONNECTED; false }
        )
    }

    private fun connectUdp(info: ApiClient.UserInfo): Boolean {
        val s = settings.load()
        val host = info.server ?: s.serverAddress
        val port = info.serverPort ?: s.serverPort
        audio.setCodec(s.codec); audio.setVolume(s.volume)
        val ssid = if (s.ssid != 0) s.ssid else (deviceData?.ssid ?: 78)
        val dm = deviceData?.devModel ?: 101
        val did = deviceData?.dmrId ?: info.dmrId
        return udp.connect(host, port, did, info.callsign, ssid, dm)
    }

    fun disconnect() {
        refreshJob?.cancel(); udp.disconnect(); _isLoggedIn.value = false
        userInfo = null; deviceData = null; audio.stopTx()
    }

    fun joinRoom(roomId: Int) {
        val s = settings.load(); val u = userInfo ?: return
        _currentRoom.value = roomId
        scope.launch {
            val dev = ApiClient.getDevice(s.serverAddress, u.callsign, deviceData?.ssid ?: 78).getOrNull()
            if (dev != null) {
                deviceData = dev
                ApiClient.updateDevice(s.serverAddress, dev, roomId)
                refresh()
            }
        }
    }

    fun loadRoomList() = scope.launch {
        val s = settings.load()
        val rooms = ApiClient.getRoomList(s.serverAddress).getOrNull()
        if (rooms != null) _roomList.value = rooms
    }

    fun startTx(): Boolean {
        if (!_isLoggedIn.value || _connState.value != ConnectionState.CONNECTED) return false
        val ok = audio.startTx()
        if (ok) { _isTransmitting.value = true; addActivity("TX", settings.load().ssid, ActivityType.TX) }
        return ok
    }

    fun stopTx() { audio.stopTx(); _isTransmitting.value = false }

    private fun startRefresh() {
        refreshJob?.cancel()
        refreshJob = scope.launch { while (true) { refresh(); delay(5000) } }
    }

    private suspend fun refresh() {
        val s = settings.load(); val u = userInfo ?: return
        try {
            val dev = ApiClient.getDevice(s.serverAddress, u.callsign, deviceData?.ssid ?: 78).getOrNull() ?: return
            deviceData = dev
            if (dev.groupId > 0) {
                _currentRoom.value = dev.groupId
                val g = ApiClient.getGroup(s.serverAddress, dev.groupId).getOrNull()
                if (g != null) { _onlineCount.value = g.onlineCount; _roomName.value = g.name }
            }
        } catch (_: Exception) {}
    }

    private fun addActivity(cs: String, ssid: Int, type: ActivityType) {
        val t = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val list = listOf(ActivityEntry(cs, ssid, t, type)) + _activityLog.value
        _activityLog.value = list.take(20)
    }

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

    private fun updateNotif(text: String) {
        getSystemService(NotificationManager::class.java).notify(NOTIF_ID, notification(text))
    }
}
