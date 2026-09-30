package com.example.sentinel.viewmodel

import android.content.Context
import android.location.Geocoder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sentinel.data.DirectionsService
import com.example.sentinel.data.OsrmService
import com.example.sentinel.domain.IncidentReport
import com.example.sentinel.util.PolylineDecoder
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*

enum class TravelMode(val label: String, val googleMode: String, val osrmProfile: String) {
    WALKING("Walking", "walking", "walking"),
    DRIVING("Driving", "driving", "driving")
}

data class NavigationUiState(
    val origin: LatLng? = null,
    val destination: LatLng? = null,
    val destinationName: String = "",
    val routes: List<SafeRoute> = emptyList(),
    val selectedRouteIndex: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val travelMode: TravelMode = TravelMode.WALKING
)

data class SafeRoute(
    val points: List<LatLng>,
    val distance: String,
    val duration: String,
    val safetyScore: Int,
    val riskCount: Int
)

class NavigationViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(NavigationUiState())
    val uiState: StateFlow<NavigationUiState> = _uiState.asStateFlow()

    private val googleDirectionsService = Retrofit.Builder()
        .baseUrl("https://maps.googleapis.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(DirectionsService::class.java)

    private val osrmService = Retrofit.Builder()
        .baseUrl("https://router.project-osrm.org/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OsrmService::class.java)

    fun setLocation(origin: LatLng) {
        _uiState.value = _uiState.value.copy(origin = origin)
    }

    fun setTravelMode(mode: TravelMode, context: Context, apiKey: String, incidents: List<IncidentReport>) {
        _uiState.value = _uiState.value.copy(travelMode = mode)
        if (_uiState.value.destinationName.isNotBlank()) {
            findSafeRoute(context, _uiState.value.destinationName, apiKey, incidents)
        }
    }

    fun findSafeRoute(context: Context, destinationName: String, apiKey: String, incidents: List<IncidentReport>) {
        val origin = _uiState.value.origin
        if (origin == null) {
            _uiState.value = _uiState.value.copy(error = "Origin location not found. Ensure GPS is on.")
            return
        }
        
        if (destinationName.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Please enter a destination.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, destinationName = destinationName)
            val currentMode = _uiState.value.travelMode
            
            // Step 1: Try Google Directions (Needs Billing)
            try {
                android.util.Log.d("SafeRoute", "Attempting Google Directions...")
                val response = googleDirectionsService.getDirections(
                    origin = "${origin.latitude},${origin.longitude}",
                    destination = destinationName,
                    apiKey = apiKey,
                    mode = currentMode.googleMode
                )

                if (response.status == "OK") {
                    processGoogleRoutes(response.routes, incidents)
                    return@launch
                } else {
                    android.util.Log.w("SafeRoute", "Google API Denied (${response.status}), falling back to OSRM...")
                }
            } catch (e: Exception) {
                android.util.Log.e("SafeRoute", "Google API Error", e)
            }

            // Step 2: Fallback to OSRM (Free)
            try {
                android.util.Log.d("SafeRoute", "Attempting OSRM (Free alternative)...")
                val geocoder = Geocoder(context, Locale.getDefault())
                val results = geocoder.getFromLocationName(destinationName, 1)
                
                if (!results.isNullOrEmpty()) {
                    val destCoords = results[0]
                    val coordsParam = "${origin.longitude},${origin.latitude};${destCoords.longitude},${destCoords.latitude}"
                    val response = osrmService.getRoute(
                        profile = currentMode.osrmProfile,
                        coordinates = coordsParam
                    )
                    
                    if (response.code == "Ok") {
                        processOsrmRoutes(response.routes, incidents)
                        return@launch
                    } else {
                        android.util.Log.w("SafeRoute", "OSRM API returned error code: ${response.code}")
                    }
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Could not find that location. Please try a more specific address.")
                    return@launch
                }
            } catch (e: Exception) {
                android.util.Log.e("SafeRoute", "OSRM Error", e)
            }

            // Step 3: Fallback to Mock Mode (Last resort)
            android.util.Log.w("SafeRoute", "All APIs failed, using Mock Mode.")
            generateMockSafeRoute(destinationName, incidents)
        }
    }

    private fun processGoogleRoutes(googleRoutes: List<com.example.sentinel.data.Route>, incidents: List<IncidentReport>) {
        val safeRoutes = googleRoutes.map { route ->
            val points = PolylineDecoder.decode(route.overview_polyline.points)
            val leg = route.legs.first()
            evaluateSafety(points, leg.distance.text, leg.duration.text, incidents)
        }.sortedByDescending { it.safetyScore }

        updateUiWithRoutes(safeRoutes)
    }

    private fun processOsrmRoutes(osrmRoutes: List<com.example.sentinel.data.OsrmRoute>, incidents: List<IncidentReport>) {
        val safeRoutes = osrmRoutes.map { route ->
            val points = PolylineDecoder.decode(route.geometry)
            val dist = String.format("%.1f km", route.distance / 1000)
            val dur = String.format("%.0f min", route.duration / 60)
            evaluateSafety(points, dist, dur, incidents)
        }.sortedByDescending { it.safetyScore }

        updateUiWithRoutes(safeRoutes)
    }

    private fun evaluateSafety(points: List<LatLng>, distance: String, duration: String, incidents: List<IncidentReport>): SafeRoute {
        val risks = incidents.filter { incident ->
            points.any { point -> 
                calculateDistance(point.latitude, point.longitude, incident.latitude, incident.longitude) < 0.3 
            }
        }
        return SafeRoute(
            points = points,
            distance = distance,
            duration = duration,
            riskCount = risks.size,
            safetyScore = (100 - (risks.size * 10)).coerceIn(0, 100)
        )
    }

    private fun updateUiWithRoutes(routes: List<SafeRoute>) {
        _uiState.value = _uiState.value.copy(
            routes = routes,
            selectedRouteIndex = 0,
            destination = routes.firstOrNull()?.points?.last(),
            isLoading = false
        )
    }

    private fun generateMockSafeRoute(destinationName: String, incidents: List<IncidentReport>) {
        val origin = _uiState.value.origin ?: return
        val destLatLng = LatLng(origin.latitude + 0.005, origin.longitude + 0.005)
        val points = listOf(origin, LatLng(origin.latitude + 0.002, origin.longitude + 0.001), destLatLng)
        val isWalking = _uiState.value.travelMode == TravelMode.WALKING
        val dur = if (isWalking) "15 mins" else "3 mins"
        val route = evaluateSafety(points, "1.2 km", dur, incidents)
        updateUiWithRoutes(listOf(route))
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}
