package com.example.sentinel.data

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface DirectionsService {
    @GET("maps/api/directions/json")
    suspend fun getDirections(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("key") apiKey: String,
        @Query("alternatives") alternatives: Boolean = true,
        @Query("mode") mode: String = "walking"
    ): DirectionsResponse
}

interface OsrmService {
    @GET("route/v1/{profile}/{coordinates}")
    suspend fun getRoute(
        @Path("profile") profile: String = "walking",
        @Path("coordinates") coordinates: String,
        @Query("overview") overview: String = "full",
        @Query("geometries") geometries: String = "polyline",
        @Query("alternatives") alternatives: Boolean = true
    ): OsrmResponse
}

data class DirectionsResponse(
    val routes: List<Route>,
    val status: String
)

data class Route(
    val overview_polyline: Polyline,
    val legs: List<Leg>
)

data class Polyline(
    val points: String
)

data class Leg(
    val distance: TextValue,
    val duration: TextValue,
    val start_address: String,
    val end_address: String
)

data class TextValue(
    val text: String,
    val value: Int
)

data class OsrmResponse(
    val code: String,
    val routes: List<OsrmRoute>
)

data class OsrmRoute(
    val geometry: String,
    val distance: Float,
    val duration: Float
)
