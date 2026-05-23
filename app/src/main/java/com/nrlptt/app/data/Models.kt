package com.nrlptt.app.data

import android.view.KeyEvent

data class UserSettings(
    val serverAddress: String = "js.nrlptt.com",
    val serverPort: Int = 60050,
    val username: String = "",
    val password: String = "",
    val callsign: String = "",
    val dmrId: Int = 0,
    val ssid: Int = 78,
    val codec: AudioCodec = AudioCodec.G711,
    val volume: Int = 100,
    val screenOffPtt: Boolean = true,
    val pttKeyCode: Int = KeyEvent.KEYCODE_VOLUME_UP,
    val autoConnect: Boolean = false
)

enum class AudioCodec { G711, OPUS }
