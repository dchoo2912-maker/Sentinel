package com.example.sentinel.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sentinel.data.FirestoreService
import com.example.sentinel.domain.IncidentReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.launch

data class IncidentUiState(
    val category: String = "Theft",
    val description: String = "",
    val address: String = "Detecting location...",
    val isAnonymous: Boolean = false,
    val isSubmitting: Boolean = false,
    val isSubmitted: Boolean = false,
    val error: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val isLocationManual: Boolean = false
)

class IncidentViewModel : ViewModel() {
    private val firestoreService = FirestoreService()
    private val _uiState = MutableStateFlow(IncidentUiState())
    val uiState: StateFlow<IncidentUiState> = _uiState.asStateFlow()
    private var currentUserId: String = ""

    fun setUser(userId: String) {
        currentUserId = userId
    }

    fun updateLocation(lat: Double, lng: Double, addressName: String? = null) {
        if (_uiState.value.isLocationManual && addressName == null) return
        
        _uiState.value = _uiState.value.copy(
            latitude = lat,
            longitude = lng,
            address = addressName ?: _uiState.value.address,
            isLocationManual = addressName != null
        )
    }

    fun setAddress(address: String) {
        _uiState.value = _uiState.value.copy(address = address)
    }

    fun updateCategory(category: String) {
        _uiState.value = _uiState.value.copy(category = category)
    }

    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    fun updateAnonymous(isAnonymous: Boolean) {
        _uiState.value = _uiState.value.copy(isAnonymous = isAnonymous)
    }

    fun submitReport() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, error = null)
            try {
                android.util.Log.d("IncidentViewModel", "Starting incident submission with timeout...")
                withTimeout(10000) { // 10 second timeout
                    firestoreService.saveIncident(
                        IncidentReport(
                            userId = currentUserId,
                            category = _uiState.value.category,
                            description = _uiState.value.description,
                            address = "Current Location",
                            latitude = _uiState.value.latitude,
                            longitude = _uiState.value.longitude,
                            isAnonymous = _uiState.value.isAnonymous,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
                android.util.Log.d("IncidentViewModel", "Incident submitted successfully")
                _uiState.value = _uiState.value.copy(isSubmitting = false, isSubmitted = true)
            } catch (e: Exception) {
                android.util.Log.e("IncidentViewModel", "Submission failed", e)
                val errorMessage = if (e is kotlinx.coroutines.TimeoutCancellationException) {
                    "Request timed out. Please check your internet connection."
                } else {
                    e.message ?: "Submission failed"
                }
                _uiState.value = _uiState.value.copy(isSubmitting = false, error = errorMessage)
            }
        }
    }

    fun resetSubmission() {
        _uiState.value = IncidentUiState()
    }
}
