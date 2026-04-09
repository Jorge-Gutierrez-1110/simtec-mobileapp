package com.example.simtec_mobileapp

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit

class ApiClient {

    companion object {
        private const val BASE_URL = "https://api.simtec-test.com"
        private const val TIMEOUT_SECONDS = 15L
    }

    private val gson = Gson()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    /**
     * Realiza un login con email y password
     * Retorna una respuesta parseada o null si hay error
     */
    suspend fun login(email: String, password: String): LoginResponse? {
        return try {
            val payload = JsonObject().apply {
                addProperty("email", email)
                addProperty("password", password)
            }

            val requestBody = gson.toJson(payload)
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$BASE_URL/login")
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build()

            Log.d("ApiClient", "Enviando login a: ${request.url}")

            val response = httpClient.newCall(request).execute()

            Log.d("ApiClient", "Respuesta status: ${response.code}")

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                Log.d("ApiClient", "Response body: $responseBody")

                if (responseBody != null) {
                    val loginResponse = gson.fromJson(responseBody, LoginResponse::class.java)
                    Log.d("ApiClient", "Login exitoso para: ${loginResponse.user?.email}")
                    loginResponse
                } else {
                    Log.e("ApiClient", "Response body es null")
                    null
                }
            } else {
                val errorBody = response.body?.string()
                Log.e("ApiClient", "Error en login: ${response.code} - $errorBody")
                null
            }

        } catch (e: Exception) {
            Log.e("ApiClient", "Excepción en login: ${e.message}", e)
            e.printStackTrace()
            null
        }
    }

    /**
     * Data class para la respuesta del login
     */
    data class LoginResponse(
        val success: Boolean = false,
        val token: String? = null,
        val user: UserData? = null,
        val message: String? = null
    )

    /**
     * Data class para los datos del usuario devueltos por el API
     */
    data class UserData(
        val id: Int = 0,
        val nombre: String = "",
        val email: String = "",
        val rol_id: Int = 0,
        val rol: String = "",
        val empleado_id: Int = 0,
        val permisos: List<String> = emptyList()
    )
}
