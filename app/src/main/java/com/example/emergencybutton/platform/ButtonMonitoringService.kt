package com.example.emergencybutton.platform

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.LocationManager
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.emergencybutton.EmergencyButtonApplication
import com.example.emergencybutton.MainActivity

/** Explicit user-started session. No boot auto-start and no silent claim to survive force-stop. */
class ButtonMonitoringService : Service() {
    private val container get() = (application as EmergencyButtonApplication).container
    private var observing: (() -> Unit)? = null
    private var foreground = false
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (foreground) return START_NOT_STICKY
        val issue = setupIssue(this)
        if (issue != null || container.ble.state.savedAddress == null) {
            container.ble.message(issue ?: "Select your button first")
            stopSelf()
            return START_NOT_STICKY
        }
        val notifications = getSystemService(NotificationManager::class.java)
        notifications.createNotificationChannel(NotificationChannel(CHANNEL, "Button monitoring",
            NotificationManager.IMPORTANCE_LOW).apply {
            description = "Shows whether the emergency button is connected"
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        })
        try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
            foreground = true
        } catch (_: RuntimeException) {
            container.ble.message("Could not start monitoring. Open the app and check Bluetooth/location permissions")
            stopSelf()
            return START_NOT_STICKY
        }
        container.ble.onStatus = { if (foreground) notifications.notify(NOTIFICATION_ID, notification()) }
        container.ble.onTrigger = {
            if (!container.emergencies.state.active) {
                if (!has(this, Manifest.permission.SEND_SMS)) {
                    container.ble.message("SOS could not send: SMS permission is missing. Open the app")
                } else if (container.emergencyRepository.loadContacts().isEmpty()) {
                    container.ble.message("SOS could not send: no SMS contacts saved")
                } else {
                    // Brief bounded lock for the initial SMS/current-location request, not continuous polling.
                    wakeLock?.let { if (it.isHeld) it.release() }
                    wakeLock = getSystemService(PowerManager::class.java)
                        .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SafeSignal:buttonSOS")
                        .apply { acquire(25_000L) }
                    container.emergencies.start(container.emergencyRepository.loadContacts())
                }
            }
        }
        var wasActive = container.emergencies.state.active
        observing = container.emergencies.observe { incident ->
            if (wasActive && !incident.active) container.ble.resetPattern()
            wasActive = incident.active
        }
        container.ble.start()
        return START_NOT_STICKY
    }

    private fun notification(): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle("SafeSignal · Button monitoring")
            .setContentText(container.ble.state.status)
            .setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true).setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).build()
    }

    override fun onDestroy() {
        val wasForeground = foreground
        foreground = false
        observing?.invoke(); observing = null
        container.ble.onStatus = null
        container.ble.onTrigger = null
        // Retain the useful startup error when service prerequisites were rejected.
        if (wasForeground) container.ble.stop()
        wakeLock?.let { if (it.isHeld) it.release() }; wakeLock = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "button_monitoring"
        private const val NOTIFICATION_ID = 101
        fun has(context: Context, permission: String) =
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        fun setupIssue(context: Context): String? = when {
            !has(context, Manifest.permission.BLUETOOTH_SCAN) || !has(context, Manifest.permission.BLUETOOTH_CONNECT) ->
                "Allow Nearby devices permission first"
            !has(context, Manifest.permission.SEND_SMS) -> "Allow SMS permission first"
            !has(context, Manifest.permission.ACCESS_COARSE_LOCATION) && !has(context, Manifest.permission.ACCESS_FINE_LOCATION) ->
                "Allow location permission first"
            !context.getSystemService(LocationManager::class.java).isLocationEnabled ->
                "Turn on your phone's Location setting, then start monitoring again"
            else -> null
        }
    }
}
