package com.example.sentinel

sealed class SentinelScreen(val route: String) {
    object Splash : SentinelScreen("splash")
    object Auth : SentinelScreen("auth")
    object DevicePairing : SentinelScreen("device_pairing")
    object Home : SentinelScreen("home")
    object Emergency : SentinelScreen("emergency")
    object LiveTracking : SentinelScreen("live_tracking")
    object SafeRoute : SentinelScreen("safe_route")
    object CommunityRisk : SentinelScreen("community_risk")
    object ReportIncident : SentinelScreen("report_incident")
    object ActivityHistory : SentinelScreen("activity_history")
    object EmergencyContacts : SentinelScreen("emergency_contacts")
    object DeviceStatus : SentinelScreen("device_status")
    object ProfileSettings : SentinelScreen("profile_settings")
}
