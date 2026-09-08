package com.example.emergencybutton.platform

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.example.emergencybutton.LocationUpdateWorker
import com.example.emergencybutton.ProtectionMode
import com.example.emergencybutton.domain.LocationScheduler
import java.util.concurrent.TimeUnit

class WorkManagerLocationScheduler(context: Context) : LocationScheduler {
    private val workManager = WorkManager.getInstance(context.applicationContext)

    override fun schedule(mode: ProtectionMode) {
        val interval = mode.intervalMinutes
        if (interval == null) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }
        val request = PeriodicWorkRequest.Builder(
            LocationUpdateWorker::class.java, interval, TimeUnit.MINUTES
        ).build()
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, request
        )
    }

    companion object {
        // Preserve this name and the worker class name for already scheduled work.
        private const val WORK_NAME = "periodic_location_update"
    }
}
