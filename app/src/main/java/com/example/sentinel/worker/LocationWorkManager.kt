package com.example.sentinel.worker

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object LocationWorkManager {
    private const val WORK_NAME = "PeriodicLocationUpdateWork"

    fun schedule15MinLocationUpdates(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<LocationUpdateWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )
    }

    fun stopLocationUpdates(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
