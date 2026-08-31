package com.example.sentinel.data

import com.example.sentinel.domain.IncidentReport
import com.example.sentinel.domain.SafetyActivity
import com.example.sentinel.domain.ActivityType
import com.example.sentinel.domain.EmergencyContact
import com.example.sentinel.domain.User
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.channels.awaitClose

class FirestoreService {
    private val db = FirebaseFirestore.getInstance()

    suspend fun saveIncident(report: IncidentReport) {
        val incidentRef = db.collection("incidents").document()
        val reportWithId = report.copy(id = incidentRef.id)
        incidentRef.set(reportWithId).await()
        
        // Also log this as an activity
        logActivity(
            SafetyActivity(
                userId = report.userId,
                title = "Incident Reported",
                description = "Reported ${report.category} at ${report.address}",
                timestamp = System.currentTimeMillis(),
                type = ActivityType.DEVICE_EVENT
            )
        )
    }

    suspend fun logActivity(activity: SafetyActivity) {
        val activityRef = db.collection("activity").document()
        val activityWithServerTime = activity.copy(
            id = activityRef.id,
            timestamp = System.currentTimeMillis()
        )
        activityRef.set(activityWithServerTime).await()
    }

    fun getRecentActivity(userId: String): Flow<List<SafetyActivity>> = callbackFlow {
        val subscription = db.collection("activity")
            .whereEqualTo("userId", userId)
            // Temporarily removed orderBy to fix sync issue without index
            .limit(20)
            .addSnapshotListener { snapshot: QuerySnapshot?, error: FirebaseFirestoreException? ->
                if (error != null) {
                    android.util.Log.e("FirestoreService", "Snapshot error", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val activities = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(SafetyActivity::class.java)
                        } catch (e: Exception) {
                            // If timestamp is a String, this will fail. Let's handle it.
                            android.util.Log.w("FirestoreService", "Legacy activity data found, skipping: ${doc.id}")
                            null
                        }
                    }
                    trySend(activities)
                }
            }
        awaitClose { subscription.remove() }
    }.onStart { emit(emptyList()) }

    suspend fun saveContact(contact: EmergencyContact) {
        val contactRef = if (contact.id.isEmpty()) {
            db.collection("contacts").document()
        } else {
            db.collection("contacts").document(contact.id)
        }
        val contactToSave = contact.copy(id = contactRef.id)
        contactRef.set(contactToSave).await()
    }

    suspend fun deleteContact(contactId: String) {
        db.collection("contacts").document(contactId).delete().await()
    }

    fun getContacts(userId: String): Flow<List<EmergencyContact>> = callbackFlow {
        val subscription = db.collection("contacts")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirestoreService", "Contacts error", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val contacts = snapshot.toObjects(EmergencyContact::class.java)
                    trySend(contacts)
                }
            }
        awaitClose { subscription.remove() }
    }

    fun getAllIncidents(): Flow<List<IncidentReport>> = callbackFlow {
        val subscription = db.collection("incidents")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirestoreService", "Incidents error", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val incidents = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(IncidentReport::class.java)
                        } catch (e: Exception) {
                            android.util.Log.w("FirestoreService", "Invalid incident data: ${doc.id}")
                            null
                        }
                    }
                    trySend(incidents)
                }
            }
        awaitClose { subscription.remove() }
    }.onStart { emit(emptyList()) }

    suspend fun updateLiveLocation(userId: String, lat: Double, lng: Double) {
        val data = mapOf(
            "latitude" to lat,
            "longitude" to lng,
            "timestamp" to System.currentTimeMillis(),
            "isActive" to true
        )
        db.collection("live_locations").document(userId).set(data).await()
    }

    suspend fun stopLocationSharing(userId: String) {
        db.collection("live_locations").document(userId).update("isActive", false).await()
    }

    suspend fun saveUserProfile(user: User) {
        db.collection("users").document(user.id).set(user).await()
    }

    suspend fun getUserById(userId: String): User? {
        return try {
            val doc = db.collection("users").document(userId).get().await()
            doc.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun getActiveLocations(): Flow<Map<String, LatLng>> = callbackFlow {
        val subscription = db.collection("live_locations")
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyMap())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val locations = snapshot.documents.associate { doc ->
                        val lat = doc.getDouble("latitude") ?: 0.0
                        val lng = doc.getDouble("longitude") ?: 0.0
                        doc.id to LatLng(lat, lng)
                    }
                    trySend(locations)
                }
            }
        awaitClose { subscription.remove() }
    }
}
