package com.nrlptt.app.data

import android.content.Context
import android.view.KeyEvent
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val serverListType = object : TypeToken<List<ServerConfig>>() {}.type

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    private val _servers = MutableStateFlow(loadServers())
    val servers: StateFlow<List<ServerConfig>> = _servers.asStateFlow()

    fun load(): UserSettings = UserSettings(
        codec = if (prefs.getString("codec", "G711") == "OPUS") AudioCodec.OPUS else AudioCodec.G711,
        volume = prefs.getInt("volume", 100),
        screenOffPtt = prefs.getBoolean("screen_off_ptt", true),
        pttKeyCode = prefs.getInt("ptt_key", KeyEvent.KEYCODE_VOLUME_UP),
        defaultUsername = prefs.getString("default_username", "") ?: "",
        defaultPassword = prefs.getString("default_password", "") ?: ""
    )

    fun save(s: UserSettings) {
        prefs.edit().apply {
            putString("codec", s.codec.name)
            putInt("volume", s.volume)
            putBoolean("screen_off_ptt", s.screenOffPtt)
            putInt("ptt_key", s.pttKeyCode)
            putString("default_username", s.defaultUsername)
            putString("default_password", s.defaultPassword)
            apply()
        }
        _settings.value = s
    }

    fun loadServers(): List<ServerConfig> {
        val json = prefs.getString("servers", null)
        if (json != null) {
            return try { gson.fromJson(json, serverListType) } catch (_: Exception) { emptyList() }
        }
        // Migrate from old single-server config
        val oldHost = prefs.getString("server_address", null)
        val oldPort = prefs.getInt("server_port", 60050)
        val oldUser = prefs.getString("username", null)
        val oldPass = prefs.getString("password", null)
        if (oldHost != null && oldUser != null) {
            val migrated = listOf(ServerConfig(oldHost, oldPort, oldUser, oldPass ?: "", true))
            saveServers(migrated)
            return migrated
        }
        return listOf(ServerConfig())
    }

    fun saveServers(list: List<ServerConfig>) {
        prefs.edit().putString("servers", gson.toJson(list)).apply()
        _servers.value = list
    }

    fun addServer(config: ServerConfig) {
        val list = _servers.value.toMutableList()
        if (list.any { it.id == config.id }) return
        list.add(config)
        saveServers(list)
    }

    fun removeServer(id: String) {
        saveServers(_servers.value.filter { it.id != id })
    }

    fun updateServer(id: String, config: ServerConfig) {
        saveServers(_servers.value.map { if (it.id == id) config else it })
    }

    companion object {
        private const val PREFS_NAME = "nrl_ptt_settings"
    }
}
