package com.example.sentinel.domain

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val isPro: Boolean = false
)

data class SentinelDevice(
    val id: String = "",
    val name: String = "",
    val serialNumber: String = "",
    val batteryLevel: Float = 0f,
    val isConnected: Boolean = false,
    val sensors: List<DeviceSensor> = emptyList()
)

data class DeviceSensor(
    val name: String = "",
    val status: String = "",
    val iconName: String = "",
    val isActive: Boolean = false
)

data class SafetyActivity(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val timestamp: Long = 0L,
    val type: ActivityType = ActivityType.DEVICE_EVENT
)

enum class ActivityType {
    SAFE_TRIP, CHECK_IN, LOCATION_SHARE, SOS, DEVICE_EVENT
}

data class EmergencyContact(
    val id: String = "",
    val userId: String = "", // Owner of this contact
    val contactUserId: String = "", // Link to the other person's account for tracking
    val name: String = "",
    val relation: String = "",
    val phone: String = "",
    val isOnline: Boolean = false,
    val avatarColorStart: Long = 0xFF8B5CF6L,
    val avatarColorEnd: Long = 0xFFD946EFL
)

data class IncidentReport(
    val id: String = "",
    val userId: String = "",
    val category: String = "",
    val description: String = "",
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val isAnonymous: Boolean = false,
    val timestamp: Long = 0L
)

data class RiskZone(
    val id: String = "",
    val severity: RiskLevel = RiskLevel.SAFE,
    val latitude: Float = 0f,
    val longitude: Float = 0f,
    val reportCount: Int = 0
)

enum class RiskLevel {
    HIGH, MODERATE, LOW, SAFE
}
