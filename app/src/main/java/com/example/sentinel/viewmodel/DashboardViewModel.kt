package com.example.sentinel.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sentinel.data.FirestoreService
import com.example.sentinel.domain.ActivityType
import com.example.sentinel.domain.SafetyActivity
import com.example.sentinel.domain.User
import com.example.sentinel.domain.IncidentReport
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class DashboardUiState(
    val user: User? = null,
    val recentActivities: List<SafetyActivity> = emptyList(),
    val incidents: List<IncidentReport> = emptyList(),
    val safetyScore: Int = 100,
    val isProtected: Boolean = true,
    val currentLocation: LatLng = LatLng(34.0522, -118.2437), // Default to LA
    val isSharingLocation: Boolean = false,
    val activeLocations: Map<String, LatLng> = emptyMap(),
    val allowedContactIds: List<String> = emptyList(),
    val immediateSafetyScore: Int = 100 // 1km radius score
)

class DashboardViewModel : ViewModel() {
    private val firestoreService = FirestoreService()
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    private var dataJob: Job? = null

    fun setUser(user: User) {
        _uiState.value = _uiState.value.copy(user = user)
        startFetchingData(user.id)
    }

    fun clearUser() {
        dataJob?.cancel()
        _uiState.value = DashboardUiState()
    }

    fun updateLocation(lat: Double, lng: Double) {
        _uiState.value = _uiState.value.copy(currentLocation = LatLng(lat, lng))
        
        if (_uiState.value.isSharingLocation) {
            val userId = _uiState.value.user?.id
            if (userId != null) {
                viewModelScope.launch {
                    try {
                        firestoreService.updateLiveLocation(userId, lat, lng)
                    } catch (e: Exception) {
                        android.util.Log.e("DashboardViewModel", "Failed to update live location", e)
                    }
                }
            }
        }
        recalculateScore()
    }

    fun toggleLocationSharing(enabled: Boolean) {
        val userId = _uiState.value.user?.id ?: return
        _uiState.value = _uiState.value.copy(isSharingLocation = enabled)
        
        viewModelScope.launch {
            try {
                if (enabled) {
                    val loc = _uiState.value.currentLocation
                    firestoreService.updateLiveLocation(userId, loc.latitude, loc.longitude)
                } else {
                    firestoreService.stopLocationSharing(userId)
                }
            } catch (e: Exception) {
                android.util.Log.e("DashboardViewModel", "Toggle sharing failed", e)
            }
        }
    }

    private fun startFetchingData(userId: String) {
        dataJob?.cancel()
        dataJob = viewModelScope.launch {
            launch {
                firestoreService.getRecentActivity(userId).collect { activities ->
                    _uiState.value = _uiState.value.copy(recentActivities = activities)
                    recalculateScore()
                }
            }
            
            launch {
                firestoreService.getAllIncidents().collect { incidents ->
                    _uiState.value = _uiState.value.copy(incidents = incidents)
                    recalculateScore()
                }
            }

            launch {
                firestoreService.getActiveLocations().collect { locations ->
                    // Filter to only show locations of people in our contact list
                    val filteredLocations = locations.filter { (userId, _) ->
                        uiState.value.allowedContactIds.contains(userId)
                    }
                    _uiState.value = _uiState.value.copy(activeLocations = filteredLocations)
                }
            }

            launch {
                firestoreService.getContacts(userId).collect { contacts ->
                    val allowedIds = contacts.mapNotNull { it.contactUserId.takeIf { id -> id.isNotEmpty() } }
                    _uiState.value = _uiState.value.copy(allowedContactIds = allowedIds)
                }
            }
        }
    }

    fun getSafetyScoreForLocation(lat: Double, lng: Double): Int {
        val incidents = _uiState.value.incidents
        val nearbyIncidents = incidents.filter { incident ->
            val dist = calculateDistance(lat, lng, incident.latitude, incident.longitude)
            dist < 5.0 // 5km radius
        }
        return (100 - (nearbyIncidents.size * 5)).coerceIn(30, 100)
    }

    private fun recalculateScore() {
        val userLoc = _uiState.value.currentLocation
        val incidents = _uiState.value.incidents
        
        // 5km Radius Score (General)
        val nearbyIncidents5km = incidents.filter { incident ->
            calculateDistance(userLoc.latitude, userLoc.longitude, incident.latitude, incident.longitude) < 5.0
        }

        // 1km Radius Score (Immediate)
        val nearbyIncidents1km = incidents.filter { incident ->
            calculateDistance(userLoc.latitude, userLoc.longitude, incident.latitude, incident.longitude) < 1.0
        }

        val generalScore = (100 - (nearbyIncidents5km.size * 5)).coerceIn(30, 100)
        val immediateScore = (100 - (nearbyIncidents1km.size * 20)).coerceIn(0, 100) // Much steeper penalty for 1km

        _uiState.value = _uiState.value.copy(
            safetyScore = generalScore,
            immediateSafetyScore = immediateScore,
            isProtected = generalScore > 70
        )
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}
