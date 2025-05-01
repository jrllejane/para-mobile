package com.example.para_mobile.api

import com.example.para_mobile.model.JeepneyRoute
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val role: String
)

data class LoginRequest(val username: String, val password: String)

data class LoginResponse(val token: String) // JWT Token response

data class UserProfile(val username: String, val email: String, val role: String)

interface ApiService {

    // Register User
    @POST("api/users/register")
    fun registerUser(@Body request: RegisterRequest): Call<UserProfile>

    // Login User
    @POST("api/users/login")
    fun loginUser(@Body request: LoginRequest): Call<LoginResponse>

    // Get All Routes - Make sure this matches your backend endpoint exactly
    @GET("api/routes/all")
    fun getAllRoutes(@Header("Authorization") token: String): Call<List<JeepneyRoute>>

    // Lookup Route by route number
    @GET("api/routes/lookup")
    fun lookupRoute(
        @Header("Authorization") token: String,
        @Query("routeNumber") routeNumber: String
    ): Call<JeepneyRoute>
}

