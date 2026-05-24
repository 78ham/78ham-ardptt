package com.nrlptt.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.nrlptt.app.data.SettingsRepository
import com.nrlptt.app.service.PttService
import com.nrlptt.app.ui.screen.LoginScreen
import com.nrlptt.app.ui.screen.MainScreen
import com.nrlptt.app.ui.screen.SettingsScreen
import com.nrlptt.app.theme.NrlPttTheme

class MainActivity : ComponentActivity() {

    private var initialScreen = AppScreen.LOGIN

    enum class AppScreen { LOGIN, MAIN, SETTINGS }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 100)
        }

        val servers = SettingsRepository(this).loadServers()
        initialScreen = if (servers.any { it.autoConnect && it.username.isNotEmpty() }) AppScreen.MAIN else AppScreen.LOGIN

        startService(Intent(this, PttService::class.java))

        setContent {
            NrlPttTheme {
                var screen by remember { mutableStateOf(initialScreen) }

                when (screen) {
                    AppScreen.LOGIN -> LoginScreen(
                        onLoginSuccess = { screen = AppScreen.MAIN },
                        onSkip = { screen = AppScreen.MAIN }
                    )
                    AppScreen.MAIN -> {
                        val svc = PttService.instance
                        if (svc != null) {
                            MainScreen(
                                service = svc,
                                onSettings = { screen = AppScreen.SETTINGS },
                                onLogout = { svc.disconnectAll(); screen = AppScreen.LOGIN }
                            )
                        }
                    }
                    AppScreen.SETTINGS -> SettingsScreen(onBack = { screen = AppScreen.MAIN })
                }
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        val svc = PttService.instance
        if (svc != null && event != null && svc.handleKey(event)) return true
        return super.dispatchKeyEvent(event)
    }
}
