package com.example.sentinel.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.sentinel.data.FirestoreService
import com.example.sentinel.data.LocationService
import com.google.firebase.auth.FirebaseAuth

class LocationUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.d("LocationUpdateWorker", "User not logged in, skipping background location update")
            return Result.success()
        }

        return try {
            val locationService = LocationService(applicationContext)
            val location = locationService.getCurrentLocation()

            if (location != null) {
                Log.d(
                    "LocationUpdateWorker",
                    "Periodic 15-min location update: ${location.latitude}, ${location.longitude}"
                )
                val firestoreService = FirestoreService()
                firestoreService.updateLiveLocation(
                    userId = currentUser.uid,
                    lat = location.latitude,
                    lng = location.longitude
                )
                Result.success()
            } else {
                Log.w("LocationUpdateWorker", "Could not fetch current location")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("LocationUpdateWorker", "Error updating location in background", e)
            Result.failure()
        }
    }
}
