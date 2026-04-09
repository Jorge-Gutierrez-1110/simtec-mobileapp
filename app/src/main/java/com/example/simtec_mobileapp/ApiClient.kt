package com.example.simtec_mobileapp

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

            Log.d("ApiClient", "═══════════════════════════════════")
            Log.d("ApiClient", "ENVIANDO LOGIN")
            Log.d("ApiClient", "URL: ${request.url}")
            Log.d("ApiClient", "Email: $email")
            Log.d("ApiClient", "Password: (oculto)")
            Log.d("ApiClient", "═══════════════════════════════════")

            val response = withContext(Dispatchers.IO) {
                httpClient.newCall(request).execute()
            }

            Log.d("ApiClient", "═══════════════════════════════════")
            Log.d("ApiClient", "RESPUESTA RECIBIDA")
            Log.d("ApiClient", "Status Code: ${response.code}")
            Log.d("ApiClient", "Is Successful: ${response.isSuccessful}")

            val responseBody = response.body?.string()
            Log.d("ApiClient", "Response Body: $responseBody")
            Log.d("ApiClient", "═══════════════════════════════════")

            if (responseBody != null) {
                try {
                    val loginResponse = gson.fromJson(responseBody, LoginResponse::class.java)
                    
                    Log.d("ApiClient", "PARSEADO EXITOSAMENTE:")
                    Log.d("ApiClient", "  success: ${loginResponse.success}")
                    Log.d("ApiClient", "  token: ${if (loginResponse.token != null) "✓ Presente" else "✗ Null"}")
                    Log.d("ApiClient", "  user: ${if (loginResponse.user != null) "✓ ${loginResponse.user.email}" else "✗ Null"}")
                    Log.d("ApiClient", "  message: ${loginResponse.message}")
                    
                    loginResponse
                } catch (e: Exception) {
                    Log.e("ApiClient", "❌ ERROR AL PARSEAR JSON: ${e.message}")
                    e.printStackTrace()
                    null
                }
            } else {
                Log.e("ApiClient", "❌ Response body es null")
                null
            }

        } catch (e: Exception) {
            Log.e("ApiClient", "❌ EXCEPCIÓN EN LOGIN: ${e.message}")
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
