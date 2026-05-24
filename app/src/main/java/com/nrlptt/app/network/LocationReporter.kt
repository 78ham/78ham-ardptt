package com.nrlptt.app.network

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*

class LocationReporter(private val context: Context) {

    companion object {
        private const val TAG = "LocationReporter"
        private const val INTERVAL_MS = 60000L  // 1 minute
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var job: Job? = null
    private var locationManager: LocationManager? = null

    var getTargets: (() -> List<UdpClient>)? = null
    var getCallsign: (() -> String)? = null
    var getSsid: (() -> Int)? = null
    var getDevModel: (() -> Int)? = null
    var getDmrId: (() -> Int)? = null

    fun start() {
        if (job != null) return
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        job = scope.launch {
            while (isActive) {
                reportLocation()
                delay(INTERVAL_MS)
            }
        }
        Log.d(TAG, "Location reporting started (interval: ${INTERVAL_MS}ms)")
    }

    fun stop() {
        job?.cancel(); job = null
        Log.d(TAG, "Location reporting stopped")
    }

    fun reportOnce() {
        scope.launch { reportLocation() }
    }

    private fun reportLocation() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) return

        val lm = locationManager ?: return
        val location = getLastKnownLocation(lm) ?: return
        val callsign = getCallsign?.invoke() ?: return
        val ssid = getSsid?.invoke() ?: 178
        val devModel = getDevModel?.invoke() ?: 101
        val dmrId = getDmrId?.invoke() ?: 178

        // Create [loc]lat,lng text packet
        val locText = "${location.latitude},${location.longitude}"
        val content = "[loc]$locText"
        val data = ByteArray(6 + content.length)
        val prefix = "[loc]".toByteArray(Charsets.UTF_8)
        val body = locText.toByteArray(Charsets.UTF_8)
        System.arraycopy(prefix, 0, data, 0, prefix.size)
        System.arraycopy(body, 0, data, prefix.size, body.size)

        val packet = Nrl21Protocol.createPacket(
            Nrl21Protocol.TYPE_TEXT, callsign, ssid, devModel, dmrId,
            "[loc]$locText".toByteArray(Charsets.UTF_8)
        )

        val targets = getTargets?.invoke() ?: emptyList()
        for (udp in targets) udp.send(packet)

        Log.d(TAG, "Location reported: $locText to ${targets.size} servers")
    }

    private fun getLastKnownLocation(lm: LocationManager): Location? {
        return try {
            val gps = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val net = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            when {
                gps != null && net != null -> if (gps.time > net.time) gps else net
                gps != null -> gps
                net != null -> net
                else -> null
            }
        } catch (_: SecurityException) { null }
    }

    fun release() { stop(); scope.cancel() }
}
