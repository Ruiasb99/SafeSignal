package com.example.emergencybutton

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class LocationUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val hasLocationPermission =
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

        if (!hasLocationPermission) return Result.failure()

        val location = getLocation() ?: return Result.retry()
        EmergencyStorage.saveLocation(applicationContext, location)
        return Result.success()
    }

    @SuppressLint("MissingPermission")
    private fun getLocation(): Location? {
        val manager =
            applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers =
            listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
                .filter(manager::isProviderEnabled)

        val fallback = providers
            .mapNotNull(manager::getLastKnownLocation)
            .maxByOrNull(Location::getTime)
        val provider = providers.firstOrNull() ?: return fallback

        val latch = CountDownLatch(1)
        val cancellationSignal = CancellationSignal()
        var result: Location? = null

        manager.getCurrentLocation(
            provider,
            cancellationSignal,
            applicationContext.mainExecutor
        ) { location ->
            result = location
            latch.countDown()
        }

        latch.await(LOCATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        cancellationSignal.cancel()
        return result ?: fallback
    }

    companion object {
        private const val LOCATION_TIMEOUT_SECONDS = 15L
    }
}
