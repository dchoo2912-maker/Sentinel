package com.example.sentinel.viewmodel

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sentinel.data.FirestoreService
import com.example.sentinel.domain.ActivityType
import com.example.sentinel.domain.EmergencyContact
import com.example.sentinel.domain.IncidentReport
import com.example.sentinel.domain.SafetyActivity
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
    val location: String = "Detecting current location...",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
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

    fun updateLocation(lat: Double, lng: Double, address: String? = null) {
        val formattedLoc = if (!address.isNullOrBlank()) {
            address
        } else {
            String.format(java.util.Locale.getDefault(), "%.4f, %.4f", lat, lng)
        }
        _uiState.value = _uiState.value.copy(
            location = formattedLoc,
            latitude = lat,
            longitude = lng
        )
    }

    private fun fetchContacts(userId: String) {
        contactsJob?.cancel()
        contactsJob = viewModelScope.launch {
            firestoreService.getContacts(userId).collect { contacts ->
                _uiState.value = _uiState.value.copy(contacts = contacts)
            }
        }
    }

    fun startHolding(context: Context? = null) {
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
                    activateSos(context)
                }
                delay(16)
            }
        }
    }

    fun activateSos(context: Context? = null) {
        _uiState.value = _uiState.value.copy(isActivated = true, isHolding = false)
        sendEmergencyMessages(context)
        logSosEvent()
    }

    private fun logSosEvent() {
        viewModelScope.launch {
            try {
                // Log activity
                firestoreService.logActivity(
                    SafetyActivity(
                        userId = currentUserId,
                        title = "EMERGENCY SOS ACTIVATED!",
                        description = "SOS triggered at ${_uiState.value.location}",
                        timestamp = System.currentTimeMillis(),
                        type = ActivityType.SOS
                    )
                )

                // Save emergency report on community map
                if (_uiState.value.latitude != 0.0 && _uiState.value.longitude != 0.0) {
                    firestoreService.saveIncident(
                        IncidentReport(
                            userId = currentUserId,
                            category = "EMERGENCY SOS",
                            description = "User activated emergency panic SOS alert at ${_uiState.value.location}",
                            address = _uiState.value.location,
                            latitude = _uiState.value.latitude,
                            longitude = _uiState.value.longitude,
                            isAnonymous = false,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("EmergencyViewModel", "Failed to log SOS event", e)
            }
        }
    }

    private fun sendEmergencyMessages(context: Context? = null) {
        val contacts = _uiState.value.contacts
        val locationText = _uiState.value.location
        val message = "SENTINEL EMERGENCY ALERT: I need immediate help! My current location is: $locationText"

        viewModelScope.launch {
            if (context == null) {
                _uiState.value = _uiState.value.copy(statusMessage = "SOS Activated! Community alert broadcasted.")
                return@launch
            }

            val phones = contacts.mapNotNull { it.phone.trim().takeIf { p -> p.isNotBlank() } }

            if (phones.isEmpty()) {
                _uiState.value = _uiState.value.copy(statusMessage = "No emergency contacts found. Add contacts in Emergency Contacts screen.")
                return@launch
            }

            val hasSmsPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.SEND_SMS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasSmsPermission) {
                _uiState.value = _uiState.value.copy(statusMessage = "SMS permission required to send automatic text alerts.")
                return@launch
            }

            var sentCount = 0
            try {
                val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }

                phones.forEach { phone ->
                    try {
                        val parts = smsManager.divideMessage(message)
                        if (parts.size > 1) {
                            smsManager.sendMultipartTextMessage(phone, null, parts, null, null)
                        } else {
                            smsManager.sendTextMessage(phone, null, message, null, null)
                        }
                        sentCount++
                    } catch (e: Exception) {
                        android.util.Log.e("EmergencyViewModel", "Error sending automatic SMS to $phone", e)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("EmergencyViewModel", "SmsManager exception", e)
            }

            if (sentCount > 0) {
                _uiState.value = _uiState.value.copy(statusMessage = "Automatic Emergency SMS sent to $sentCount contact(s)!")
            } else {
                _uiState.value = _uiState.value.copy(statusMessage = "Failed to send automatic SMS. Please check SIM card & signal.")
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
