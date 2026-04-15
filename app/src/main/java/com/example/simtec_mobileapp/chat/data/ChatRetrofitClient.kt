package com.example.simtec_mobileapp.chat.data

import android.content.Context
import android.content.SharedPreferences
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// =====================================================
// CHAT RETROFIT CLIENT — Singleton con JWT del SessionManager
// =====================================================
//
// Lee el token JWT directamente de SharedPreferences
// "simtec_session" (las mismas que usa SessionManager).
// NO crea SharedPreferences propias ni duplica el token.
//
// USO:
//   ChatRetrofitClient.init(context)   // Una vez, en ChatActivity.onCreate()
//   val api = ChatRetrofitClient.chatApi
//   val userId = ChatRetrofitClient.getUserId()
// =====================================================

object ChatRetrofitClient {

    private const val BASE_URL = "https://api.simtec-test.com/"
    private const val PREFS_NAME = "simtec_session" // Mismas que SessionManager

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        if (!::prefs.isInitialized) {
            prefs = context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    // --- Leer datos de sesion (mismas keys que SessionManager) ---

    fun getToken(): String? {
        val expiration = prefs.getLong("token_expiration", 0)
        if (expiration > 0 && expiration <= System.currentTimeMillis()) {
            return null // Token expirado
        }
        return prefs.getString("token", null)
    }

    fun getUserId(): Int = prefs.getInt("user_id", 0)
    fun getRolId(): Int = prefs.getInt("rol_id", 0)
    fun getEmpleadoId(): Int = prefs.getInt("empleado_id", 0)
    fun getUserName(): String = prefs.getString("user_name", "Usuario") ?: "Usuario"

    // --- Interceptor que agrega JWT a cada request ---

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val token = getToken()

        val request = if (token != null) {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            original
        }

        chain.proceed(request)
    }

    // Logging para debug
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val chatApi: ChatApi by lazy {
        retrofit.create(ChatApi::class.java)
    }
}
