package com.example.emergencybutton.platform

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import com.example.emergencybutton.SavedLocation
import com.example.emergencybutton.domain.CancelLocationRequest
import com.example.emergencybutton.domain.LocationProvider

class AndroidLocationProvider(context: Context) : LocationProvider {
    private val context = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())

    @SuppressLint("MissingPermission")
    override fun findLocation(onResult: (SavedLocation?) -> Unit): CancelLocationRequest {
        val manager = context.getSystemService(LocationManager::class.java)
        val cancellation = CancellationSignal()
        var completed = false
        var fallback: Location? = null
        lateinit var timeout: Runnable

        fun complete(location: Location?) {
            if (completed) return
            completed = true
            handler.removeCallbacks(timeout)
            cancellation.cancel()
            onResult(location?.let {
                SavedLocation(it.latitude, it.longitude, it.accuracy, it.time)
            })
        }

        timeout = Runnable { complete(fallback) }
        try {
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
                .filter(manager::isProviderEnabled)
            fallback = providers.mapNotNull(manager::getLastKnownLocation).maxByOrNull(Location::getTime)
            val provider = providers.firstOrNull()
            if (provider == null) {
                complete(fallback)
            } else {
                handler.postDelayed(timeout, TIMEOUT_MS)
                manager.getCurrentLocation(provider, cancellation, context.mainExecutor) {
                    complete(it ?: fallback)
                }
            }
        } catch (_: SecurityException) {
            complete(null)
        } catch (_: IllegalArgumentException) {
            complete(fallback)
        }
        return CancelLocationRequest {
            completed = true
            handler.removeCallbacks(timeout)
            cancellation.cancel()
        }
    }

    companion object {
        private const val TIMEOUT_MS = 15_000L
    }
}
