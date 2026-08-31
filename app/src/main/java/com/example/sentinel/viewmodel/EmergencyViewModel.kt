package com.example.sentinel.viewmodel

import android.telephony.SmsManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sentinel.data.FirestoreService
import com.example.sentinel.domain.EmergencyContact
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EmergencyUiState(
    val isHolding: Boolean = false,
    val progress: Float = 0f,
    val isActivated: Boolean = false,
    val countdown: Float = 3f,
    val contacts: List<EmergencyContact> = emptyList(),
    val location: String = "34.0522° N, 118.2437° W",
    val statusMessage: String? = null
)

class EmergencyViewModel : ViewModel() {
    private val firestoreService = FirestoreService()
    private val _uiState = MutableStateFlow(EmergencyUiState())
    val uiState: StateFlow<EmergencyUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null
    private var contactsJob: Job? = null
    private var currentUserId: String = ""

    fun setUser(userId: String) {
        currentUserId = userId
        fetchContacts(userId)
    }

    private fun fetchContacts(userId: String) {
        contactsJob?.cancel()
        contactsJob = viewModelScope.launch {
            firestoreService.getContacts(userId).collect { contacts ->
                _uiState.value = _uiState.value.copy(contacts = contacts)
            }
        }
    }

    fun startHolding() {
        if (_uiState.value.isActivated) return
        
        _uiState.value = _uiState.value.copy(isHolding = true, statusMessage = null)
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            while (_uiState.value.isHolding && _uiState.value.progress < 1f) {
                val elapsedTime = System.currentTimeMillis() - startTime
                val newProgress = (elapsedTime / 3000f).coerceIn(0f, 1f)
                _uiState.value = _uiState.value.copy(
                    progress = newProgress,
                    countdown = 3f - (newProgress * 3f)
                )
                if (newProgress >= 1f) {
                    activateSos()
                }
                delay(16)
            }
        }
    }

    private fun activateSos() {
        _uiState.value = _uiState.value.copy(isActivated = true, isHolding = false)
        sendEmergencyMessages()
    }

    private fun sendEmergencyMessages() {
        val contacts = _uiState.value.contacts
        if (contacts.isEmpty()) {
            _uiState.value = _uiState.value.copy(statusMessage = "No emergency contacts found to alert.")
            return
        }

        viewModelScope.launch {
            try {
                val smsManager = SmsManager.getDefault()
                val message = "SENTINEL EMERGENCY ALERT: I need help! My current location is: ${_uiState.value.location}"
                
                contacts.forEach { contact ->
                    if (contact.phone.isNotBlank()) {
                        smsManager.sendTextMessage(contact.phone, null, message, null, null)
                    }
                }
                _uiState.value = _uiState.value.copy(statusMessage = "Alert sent to ${contacts.size} contacts.")
            } catch (e: Exception) {
                android.util.Log.e("EmergencyViewModel", "Failed to send SMS", e)
                _uiState.value = _uiState.value.copy(statusMessage = "Error sending SMS alerts.")
            }
        }
    }

    fun stopHolding() {
        if (_uiState.value.isActivated) return
        
        _uiState.value = _uiState.value.copy(isHolding = false, progress = 0f, countdown = 3f)
        countdownJob?.cancel()
    }

    fun deactivateSos() {
        _uiState.value = _uiState.value.copy(isActivated = false, progress = 0f, countdown = 3f, statusMessage = null)
    }
}
