package com.example.para_mobile.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.para_mobile.api.LoginRequest
import com.example.para_mobile.api.LoginResponse
import com.example.para_mobile.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TokenManager(private val context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    private val TAG = "TokenManager"

    companion object {
        private const val KEY_JWT_TOKEN = "jwt_token"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password" // Note: In a production app, consider more secure storage
    }

    // Save token and credentials
    fun saveToken(token: String) {
        sharedPreferences.edit().putString(KEY_JWT_TOKEN, token).apply()
    }

    // Save user credentials for potential token refresh
    fun saveCredentials(username: String, password: String) {
        sharedPreferences.edit()
            .putString(KEY_USERNAME, username)
            .putString(KEY_PASSWORD, password)
            .apply()
    }

    // Get the stored token
    fun getToken(): String? {
        return sharedPreferences.getString(KEY_JWT_TOKEN, null)
    }

    // Clear token and credentials on logout
    fun clearToken() {
        sharedPreferences.edit()
            .remove(KEY_JWT_TOKEN)
            .remove(KEY_USERNAME)
            .remove(KEY_PASSWORD)
            .apply()
    }

    // Check if we have a token
    fun hasToken(): Boolean {
        return !getToken().isNullOrEmpty()
    }

    // Refresh token using stored credentials
    fun refreshToken(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val username = sharedPreferences.getString(KEY_USERNAME, null)
        val password = sharedPreferences.getString(KEY_PASSWORD, null)

        if (username.isNullOrEmpty() || password.isNullOrEmpty()) {
            onError("No stored credentials for token refresh")
            return
        }

        val loginRequest = LoginRequest(username, password)

        RetrofitClient.instance.loginUser(loginRequest).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val newToken = response.body()!!.token
                    saveToken(newToken)
                    Log.d(TAG, "Token refreshed successfully")
                    onSuccess(newToken)
                } else {
                    val errorMsg = "Failed to refresh token: ${response.code()}"
                    Log.e(TAG, errorMsg)
                    onError(errorMsg)
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                val errorMsg = "Network error during token refresh: ${t.message}"
                Log.e(TAG, errorMsg)
                onError(errorMsg)
            }
        })
    }
}
