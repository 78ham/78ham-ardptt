package com.nrlptt.app

import android.app.Application
import android.util.Log

class NrlPttApp : Application() {
    companion object {
        private const val TAG = "NrlPttApp"
        lateinit var instance: NrlPttApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(TAG, "NRL PTT started")
    }
}
