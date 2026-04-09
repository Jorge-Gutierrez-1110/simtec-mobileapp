package com.example.simtec_mobileapp

import android.content.Context
import com.google.gson.Gson

class SessionManager(context: Context) {

    private val prefs =
        context.getSharedPreferences("simtec_session", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveLogin(user: String) {

        prefs.edit()
            .putBoolean("logged", true)
            .putString("user", user)
            .apply()
    }

    fun isLogged(): Boolean {

        return prefs.getBoolean("logged", false)
    }

    fun logout() {

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
     * Data class para un registro de asistencia
     */
    data class AttendanceRecord(
        val type: String,
        val timestamp: String,
        val location: String,
        val faceMatchConfidence: Float
    )
}