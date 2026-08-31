package com.example.sentinel.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sentinel.domain.DeviceSensor
import com.example.sentinel.domain.SentinelDevice
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DeviceUiState(
    val devices: List<SentinelDevice> = emptyList(),
    val isScanning: Boolean = false,
    val connectedDevice: SentinelDevice? = null
)

class DeviceViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(DeviceUiState())
    val uiState: StateFlow<DeviceUiState> = _uiState.asStateFlow()

    fun startScanning() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true)
            delay(3000) // Simulate scanning
            _uiState.value = _uiState.value.copy(
                isScanning = false,
                devices = listOf(
                    SentinelDevice("1", "Sentinel Watch X1", "SN: 882-392", 0.87f, false, emptyList()),
                    SentinelDevice("2", "Sentinel Guard", "SN: 112-445", 0.42f, false, emptyList())
                )
            )
        }
    }

    fun connectDevice(device: SentinelDevice) {
        viewModelScope.launch {
            delay(1000)
            val connected = device.copy(
                isConnected = true,
                sensors = listOf(
                    DeviceSensor("Heart Rate", "72 BPM", "favorite", true),
                    DeviceSensor("GPS Accuracy", "6m", "gps_fixed", true),
                    DeviceSensor("Fall Detection", "Active", "warning", true)
                )
            )
            _uiState.value = _uiState.value.copy(connectedDevice = connected)
        }
    }
}
