package com.example.simtec_mobileapp

import android.content.Context
import android.util.Log
import com.google.gson.Gson

class SessionManager(context: Context) {

    private val prefs =
        context.getSharedPreferences("simtec_session", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val TAG = "SessionManager"
    }

    /**
     * Guarda los datos de login (legacy, para compatibilidad)
     */
    fun saveLogin(user: String) {

        prefs.edit()
            .putBoolean("logged", true)
            .putString("user", user)
            .apply()
    }

    /**
     * Guarda el login con token JWT (nuevo método)
     */
    fun saveLoginWithToken(
        token: String,
        userId: Int,
        userName: String,
        email: String,
        rolId: Int,
        rol: String,
        empleadoId: Int,
        permisos: List<String>
    ) {
        val expirationTime = System.currentTimeMillis() + (8 * 60 * 60 * 1000) // 8 horas

        prefs.edit()
            .putBoolean("logged", true)
            .putString("token", token)
            .putInt("user_id", userId)
            .putString("user_name", userName)
            .putString("email", email)
            .putInt("rol_id", rolId)
            .putString("rol", rol)
            .putInt("empleado_id", empleadoId)
            .putLong("token_expiration", expirationTime)
            .putString("permisos", gson.toJson(permisos))
            .apply()

        Log.d(TAG, "Login guardado para: $email (Token válido hasta ${expirationTime})")
    }

    /**
     * Obtiene el token JWT guardado
     */
    fun getToken(): String? {
        return if (isTokenValid()) {
            prefs.getString("token", null)
        } else {
            Log.w(TAG, "Token expirado")
            null
        }
    }

    /**
     * Verifica si el token sigue siendo válido
     */
    fun isTokenValid(): Boolean {
        val expirationTime = prefs.getLong("token_expiration", 0)
        val isValid = expirationTime > System.currentTimeMillis()
        
        if (!isValid && expirationTime > 0) {
            Log.w(TAG, "Token expiró hace ${(System.currentTimeMillis() - expirationTime) / 1000} segundos")
        }
        
        return isValid
    }

    /**
     * Obtiene el ID del usuario
     */
    fun getUserId(): Int {
        return prefs.getInt("user_id", -1)
    }

    /**
     * Obtiene el nombre del usuario
     */
    fun getUserName(): String {
        return prefs.getString("user_name", "Usuario") ?: "Usuario"
    }

    /**
     * Obtiene el email del usuario
     */
    fun getEmail(): String {
        return prefs.getString("email", "") ?: ""
    }

    /**
     * Obtiene el rol del usuario
     */
    fun getRol(): String {
        return prefs.getString("rol", "Usuario") ?: "Usuario"
    }

    /**
     * Obtiene los permisos del usuario
     */
    fun getPermisos(): List<String> {
        return try {
            val json = prefs.getString("permisos", "[]")
            gson.fromJson(json, Array<String>::class.java).toList()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing permisos: ${e.message}")
            emptyList()
        }
    }

    /**
     * Verifica si el usuario tiene un permiso específico
     */
    fun tienePermiso(permiso: String): Boolean {
        return getPermisos().contains(permiso)
    }

    /**
     * Verifica si hay sesión activa
     */
    fun isLogged(): Boolean {
        val hasToken = prefs.getString("token", null) != null
        val isTokenValid = isTokenValid()
        return hasToken && isTokenValid
    }

    /**
     * Limpia toda la sesión
     */
    fun logout() {
        Log.d(TAG, "Logout ejecutado para: ${getEmail()}")
        prefs.edit().clear().apply()
    }

    /**
     * Guarda el template de cara del usuario (se captura en primer registro)
     */
    fun saveFaceTemplate(template: FaceDetectionManager.FaceTemplate) {
        val json = gson.toJson(template)
        prefs.edit()
            .putString("face_template", json)
            .apply()
    }

    /**
     * Obtiene el template de cara guardado del usuario
     */
    fun getFaceTemplate(): FaceDetectionManager.FaceTemplate? {
        return try {
            val json = prefs.getString("face_template", null)
            if (json != null) {
                gson.fromJson(json, FaceDetectionManager.FaceTemplate::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Guarda un registro de asistencia con ubicación
     * Formato: "Entrada - 08 Apr 2026 14:30 - Calle Principal 123, Montevideo"
     */
    fun saveAttendanceRecord(
        type: String,
        timestamp: String,
        location: String,
        faceConfidence: Float = 0f
    ) {
        val record = AttendanceRecord(
            type = type,
            timestamp = timestamp,
            location = location,
            faceMatchConfidence = faceConfidence
        )

        // Obtemos registros anteriores
        val existingJson = prefs.getString("attendance_records_json", "[]")
        val existingList = try {
            gson.fromJson(existingJson, Array<AttendanceRecord>::class.java).toMutableList()
        } catch (e: Exception) {
            mutableListOf()
        }

        // Agregamos el nuevo registro al inicio
        existingList.add(0, record)

        // Guardamos
        val json = gson.toJson(existingList)
        prefs.edit()
            .putString("attendance_records_json", json)
            .apply()
    }

    /**
     * Obtiene todos los registros de asistencia
     */
    fun getAttendanceRecords(): List<AttendanceRecord> {
        return try {
            val json = prefs.getString("attendance_records_json", "[]")
            gson.fromJson(json, Array<AttendanceRecord>::class.java).toList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Guarda el último tipo de registro (Entrada/Salida)
     * Esto permite saber si el próximo debe ser entrada o salida
     */
    fun saveLastRecordType(type: String) {
        prefs.edit()
            .putString("last_record_type", type)
            .apply()
    }

    /**
     * Obtiene el último tipo de registro guardado
     * Si es "Entrada", el próximo debe ser "Salida" y viceversa
     */
    fun getLastRecordType(): String? {
        return prefs.getString("last_record_type", null)
    }

    /**
     * Determina si el siguiente registro debe ser Entrada o Salida
     * basándose en el último registro guardado
     */
    fun shouldBeEntry(): Boolean {
        val lastType = getLastRecordType()
        // Si no hay último registro, es entrada
        // Si el último fue "Salida", el próximo es "Entrada"
        // Si el último fue "Entrada", el próximo es "Salida"
        return lastType == null || lastType == "Salida"
    }

    /**
     * Data class para un registro de asistencia
     */
    data class AttendanceRecord(
        val type: String,
        val timestamp: String,
        val location: String,
        val faceMatchConfidence: Float
    )
}
