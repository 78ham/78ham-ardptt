package com.nrlptt.app.data

import android.content.Context
import android.view.KeyEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    fun load(): UserSettings = UserSettings(
        serverAddress = prefs.getString("server_address", "js.nrlptt.com") ?: "js.nrlptt.com",
        serverPort = prefs.getInt("server_port", 60050),
        username = prefs.getString("username", "") ?: "",
        password = prefs.getString("password", "") ?: "",
        callsign = prefs.getString("callsign", "") ?: "",
        dmrId = prefs.getInt("dmr_id", 0),
        ssid = prefs.getInt("ssid", 78),
        codec = if (prefs.getString("codec", "G711") == "OPUS") AudioCodec.OPUS else AudioCodec.G711,
        volume = prefs.getInt("volume", 100),
        screenOffPtt = prefs.getBoolean("screen_off_ptt", true),
        pttKeyCode = prefs.getInt("ptt_key", KeyEvent.KEYCODE_VOLUME_UP),
        autoConnect = prefs.getBoolean("auto_connect", false)
    )

    fun save(s: UserSettings) {
        prefs.edit().apply {
            putString("server_address", s.serverAddress)
            putInt("server_port", s.serverPort)
            putString("username", s.username)
            putString("password", s.password)
            putString("callsign", s.callsign)
            putInt("dmr_id", s.dmrId)
            putInt("ssid", s.ssid)
            putString("codec", s.codec.name)
            putInt("volume", s.volume)
            putBoolean("screen_off_ptt", s.screenOffPtt)
            putInt("ptt_key", s.pttKeyCode)
            putBoolean("auto_connect", s.autoConnect)
            apply()
        }
        _settings.value = s
    }

    companion object {
        private const val PREFS_NAME = "nrl_ptt_settings"
    }
}
