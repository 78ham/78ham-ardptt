package com.nrlptt.app.ui

import android.Manifest
import android.content.Context
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

    private var screen = AppScreen.LOGIN

    enum class AppScreen { LOGIN, MAIN, SETTINGS }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 100)
        }

        val settings = SettingsRepository(this).load()
        screen = if (settings.autoConnect && settings.username.isNotEmpty()) AppScreen.MAIN else AppScreen.LOGIN

        // Start PttService
        Intent(this, PttService::class.java).also { startService(it) }

        render()
    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        val svc = PttService.instance
        if (svc != null && event != null && svc.handleKey(event)) return true
        return super.dispatchKeyEvent(event)
    }

    private fun render() {
        setContent {
            NrlPttTheme {
                var currentScreen by remember { mutableStateOf(screen) }

                when (currentScreen) {
                    AppScreen.LOGIN -> LoginScreen(
                        onLoginSuccess = { currentScreen = AppScreen.MAIN },
                        onSkip = { currentScreen = AppScreen.MAIN }
                    )
                    AppScreen.MAIN -> {
                        val svc = PttService.instance
                        if (svc != null) {
                            MainScreen(
                                service = svc,
                                onSettings = { currentScreen = AppScreen.SETTINGS },
                                onLogout = {
                                    svc.disconnect()
                                    currentScreen = AppScreen.LOGIN
                                }
                            )
                        }
                    }
                    AppScreen.SETTINGS -> SettingsScreen(
                        onBack = { currentScreen = AppScreen.MAIN }
                    )
                }
            }
        }
    }
}
