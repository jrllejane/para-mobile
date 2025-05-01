package com.example.para_mobile.activity

import android.util.Log
import com.example.para_mobile.api.NominatimAPI
import com.example.para_mobile.api.OSRMApi
import com.example.para_mobile.api.OSRMResponse
import com.example.para_mobile.helper.PolylineDecoder
import com.example.para_mobile.model.LocationResult
import com.example.para_mobile.util.RouteOverlay
import okhttp3.OkHttpClient
import org.osmdroid.util.GeoPoint
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class SearchService {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    private val nominatimRetrofit = Retrofit.Builder()
        .baseUrl("https://nominatim.openstreetmap.org/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val osrmRetrofit = Retrofit.Builder()
        .baseUrl("https://router.project-osrm.org/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val nominatimApi = nominatimRetrofit.create(NominatimAPI::class.java)
    private val routingApi = osrmRetrofit.create(OSRMApi::class.java)

    fun searchLocation(
        query: String,
        onSuccess: (LocationResult) -> Unit,
        onEmpty: () -> Unit,
        onError: (String) -> Unit
    ) {
        nominatimApi.searchLocation(query).enqueue(object : Callback<List<LocationResult>> {
            override fun onResponse(call: Call<List<LocationResult>>, response: Response<List<LocationResult>>) {
                response.body()?.let { results ->
                    if (results.isNotEmpty()) {
                        onSuccess(results[0])
                    } else {
                        onEmpty()
                    }
                } ?: onError("Empty response")
            }

            override fun onFailure(call: Call<List<LocationResult>>, t: Throwable) {
                onError(t.message ?: "Unknown error")
            }
        })
    }

    fun getRoute(
        startPoint: GeoPoint,
        endPoint: GeoPoint,
        routeOverlay: RouteOverlay,
        onRouteCalculated: (distance: Double, duration: Double) -> Unit,
        onError: (String) -> Unit
    ) {
        // Clear any existing route before drawing a new one
        routeOverlay.clearRoute()

        val coordinates = "${startPoint.longitude},${startPoint.latitude};${endPoint.longitude},${endPoint.latitude}"

        routingApi.getRoute(
            coordinates = coordinates,
            overview = "simplified",
            geometries = "polyline"
        ).enqueue(object : Callback<OSRMResponse> {
            override fun onResponse(call: Call<OSRMResponse>, response: Response<OSRMResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let { osrmResponse ->
                        if (osrmResponse.routes.isNotEmpty()) {
                            val route = osrmResponse.routes[0]
                            val routePoints = PolylineDecoder.decode(route.geometry)

                            // Draw the route on the map
                            routeOverlay.drawRoute(routePoints)

                            // Return route info
                            val distance = route.distance / 1000 // km
                            val duration = route.duration / 60 // minutes
                            onRouteCalculated(distance, duration)
                        } else {
                            onError("No routes found")
                        }
                    } ?: onError("Empty response")
                } else {
                    onError("Failed to get route: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<OSRMResponse>, t: Throwable) {
                onError(t.message ?: "Unknown error")
            }
        })
    }


    fun search(query: String, mainActivity: MainActivity, any: Any) {

    }

    // Add this method to your SearchService class
    fun checkApiStatus(callback: (String) -> Unit) {
        // Make a simple API call to check status
        routingApi.getRoute(
            coordinates = "13.388860,52.517037;13.397634,52.529407", // Berlin coordinates
            overview = "false"
        ).enqueue(object : Callback<OSRMResponse> {
            override fun onResponse(call: Call<OSRMResponse>, response: Response<OSRMResponse>) {
                val headers = response.headers()
                val rateLimitInfo = StringBuilder("OSRM API Status:\n")

                // Check for rate limit headers
                val remaining = headers.get("X-Rate-Limit-Remaining")
                val limit = headers.get("X-Rate-Limit-Limit")
                val reset = headers.get("X-Rate-Limit-Reset")

                rateLimitInfo.append("Response code: ${response.code()}\n")

                if (remaining != null) {
                    rateLimitInfo.append("Remaining calls: $remaining\n")
                    rateLimitInfo.append("Limit: $limit\n")
                    rateLimitInfo.append("Reset time: $reset\n")
                } else {
                    rateLimitInfo.append("No explicit rate limit headers found.\n")
                }

                // Check response code
                if (response.code() == 429) {
                    rateLimitInfo.append("WARNING: You are being rate limited!\n")
                } else if (!response.isSuccessful) {
                    rateLimitInfo.append("Error: ${response.code()} ${response.message()}\n")
                } else {
                    rateLimitInfo.append("API is responding normally.\n")
                }

                // Send the status back to the callback
                callback(rateLimitInfo.toString())
            }

            override fun onFailure(call: Call<OSRMResponse>, t: Throwable) {
                callback("Failed to check API status: ${t.message}")
            }
        })
    }
}
