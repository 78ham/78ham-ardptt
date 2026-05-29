package com.nrlptt.app.ptt

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.KeyEvent

class PttController(private val context: Context) {

    companion object {
        private const val TAG = "PttController"
        private const val WAKE_TAG = "NrlPtt:PttWake"
        const val KEYCODE_PTT = 0x106
    }

    interface Listener {
        fun onPress()
        fun onRelease()
        fun onLongPress()
    }

    var listener: Listener? = null
    var pttKeyCode: Int = KEYCODE_PTT
    var screenOffPtt: Boolean = true
    var isPressed: Boolean = false
        private set

    private val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    private var wakeLock: PowerManager.WakeLock? = null
    private var pressTime = 0L
    private var deviceProfile: DeviceKeyProfiles.Profile? = null
    private val registeredReceivers = mutableListOf<BroadcastReceiver>()

    fun init(key: Int, screenOff: Boolean) {
        pttKeyCode = key; screenOffPtt = screenOff
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP, WAKE_TAG)

        deviceProfile = DeviceKeyProfiles.detect()
        val profile = deviceProfile!!
        if (key == KeyEvent.KEYCODE_VOLUME_UP) pttKeyCode = profile.pttKeyCode
        Log.i(TAG, "Profile: ${profile.name}, PTT: 0x${pttKeyCode.toString(16)}")

        if (profile.useBroadcastPtt) {
            profile.broadcastActions.forEach { action ->
                try {
                    val receiver = object : BroadcastReceiver() {
                        override fun onReceive(ctx: Context, intent: Intent?) {
                            when (intent?.action) {
                                "android.intent.action.PTT.down" -> onKey(KeyEvent(KeyEvent.ACTION_DOWN, pttKeyCode))
                                "android.intent.action.PTT.up" -> onKey(KeyEvent(KeyEvent.ACTION_UP, pttKeyCode))
                            }
                        }
                    }
                    context.registerReceiver(receiver, IntentFilter(action))
                    registeredReceivers.add(receiver)
                } catch (e: Exception) { Log.e(TAG, "Broadcast failed: $action", e) }
            }
        }
    }

    fun onKey(event: KeyEvent): Boolean {
        if (!isPttKey(event.keyCode)) return false
        when (event.action) {
            KeyEvent.ACTION_DOWN -> if (!isPressed) {
                isPressed = true; pressTime = System.currentTimeMillis()
                acquireWake(); vibrate(); listener?.onPress()
            }
            KeyEvent.ACTION_UP -> if (isPressed) {
                if (System.currentTimeMillis() - pressTime >= 1000) listener?.onLongPress()
                isPressed = false; listener?.onRelease(); releaseWake()
            }
        }
        return true
    }

    fun press() { if (!isPressed) { isPressed = true; acquireWake(); listener?.onPress() } }
    fun release() { if (isPressed) { isPressed = false; listener?.onRelease(); releaseWake() } }

    private fun isPttKey(k: Int): Boolean {
        if (k == pttKeyCode || k == KEYCODE_PTT) return true
        deviceProfile?.extraKeyCodes?.let { if (k in it) return true }
        return k == 113 || k == 368 || k == 270 || k == 531 || k == 532 ||
                k == KeyEvent.KEYCODE_HEADSETHOOK ||
                k in KeyEvent.KEYCODE_BUTTON_1..KeyEvent.KEYCODE_BUTTON_12
    }

    private fun vibrate() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                vibrator?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
            else @Suppress("DEPRECATION") vibrator?.vibrate(40)
        } catch (_: Exception) {}
    }

    private fun acquireWake() { if (screenOffPtt && wakeLock?.isHeld == false) wakeLock?.acquire(30000) }
    private fun releaseWake() { if (wakeLock?.isHeld == true) wakeLock?.release() }

    fun destroy() {
        releaseWake()
        registeredReceivers.forEach { try { context.unregisterReceiver(it) } catch (_: Exception) {} }
        registeredReceivers.clear()
        listener = null
    }
}
