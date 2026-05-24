package com.nrlptt.app.data

import android.view.KeyEvent

data class UserSettings(
    val codec: AudioCodec = AudioCodec.OPUS,
    val volume: Int = 100,
    val screenOffPtt: Boolean = true,
    val pttKeyCode: Int = KeyEvent.KEYCODE_VOLUME_UP,
    val defaultUsername: String = "",
    val defaultPassword: String = ""
)

data class ServerConfig(
    val host: String = "m.nrlptt.com",
    val port: Int = 60050,
    val username: String = "",
    val password: String = "",
    val autoConnect: Boolean = true,
    val label: String = ""
) {
    val id: String get() = "$host:$port"
    val displayLabel: String get() = label.ifEmpty { host }
}

enum class AudioCodec { G711, OPUS }
