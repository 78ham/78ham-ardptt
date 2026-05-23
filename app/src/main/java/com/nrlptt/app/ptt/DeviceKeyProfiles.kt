package com.nrlptt.app.ptt

import android.os.Build

object DeviceKeyProfiles {

    data class Profile(
        val name: String,
        val match: () -> Boolean,
        val pttKeyCode: Int,
        val extraKeyCodes: List<Int> = emptyList(),
        val useBroadcastPtt: Boolean = false,
        val broadcastActions: List<String> = emptyList()
    )

    val profiles = listOf(
        Profile(
            name = "D12",
            match = { Build.MODEL.contains("D12", ignoreCase = true) },
            pttKeyCode = 113,
            extraKeyCodes = listOf(113, 0x107, 0x108, 0x109, 0x10A, 0x10B)
        ),
        Profile(
            name = "MTK PTT",
            match = { Build.HARDWARE.contains("mt", ignoreCase = true) && !Build.MODEL.contains("D12", ignoreCase = true) },
            pttKeyCode = 0x106,
            useBroadcastPtt = true,
            broadcastActions = listOf("android.intent.action.PTT.down", "android.intent.action.PTT.up")
        ),
        Profile(
            name = "Generic PTT",
            match = { true },
            pttKeyCode = 0x106
        )
    )

    fun detect(): Profile {
        val p = profiles.first { it.match() }
        android.util.Log.i("DeviceKeyProfiles", "model=${Build.MODEL} hw=${Build.HARDWARE} -> ${p.name}")
        return p
    }
}
