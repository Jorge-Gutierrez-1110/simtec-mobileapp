package com.example.simtec_mobileapp.chat.data

import com.google.gson.annotations.SerializedName

// =====================================================
// DATA CLASSES — Modelos del Chat
// =====================================================

data class Conversacion(
    val id: Int,
    @SerializedName("empleado_id") val empleadoId: Int,
    @SerializedName("emp_nombre") val empNombre: String?,
    @SerializedName("emp_apellido") val empApellido: String?,
    @SerializedName("emp_foto") val empFoto: String?,
    @SerializedName("emp_puesto") val empPuesto: String?,
    @SerializedName("emp_departamento") val empDepartamento: String?,
    val asunto: String?,
    val estado: String?,
    @SerializedName("no_leidos") val noLeidos: Int = 0,
    @SerializedName("ultimo_mensaje") val ultimoMensaje: String?,
    @SerializedName("ultimo_mensaje_fecha") val ultimoMensajeFecha: String?,
    @SerializedName("updated_at") val updatedAt: String?
)

data class Mensaje(
    val id: Int,
    @SerializedName("conversacion_id") val conversacionId: Int,
    @SerializedName("usuario_id") val usuarioId: Int,
    val mensaje: String,
    val leido: Int = 0,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("nombre_remitente") val nombreRemitente: String?,
    @SerializedName("imagen_perfil") val imagenPerfil: String?,
    @SerializedName("rol_id") val rolId: Int = 0
)

// --- Responses ---

data class ConversacionesResponse(
    val success: Boolean,
    val conversaciones: List<Conversacion> = emptyList()
)

data class MensajesResponse(
    val success: Boolean,
    val mensajes: List<Mensaje> = emptyList()
)

data class EnviarMensajeResponse(
    val success: Boolean,
    val mensaje: Mensaje? = null
)

data class NoLeidosResponse(
    val success: Boolean,
    val total: Int = 0
)

data class GenericResponse(
    val success: Boolean,
    val message: String? = null
)

// --- Requests ---

data class EnviarMensajeRequest(
    @SerializedName("conversacion_id") val conversacionId: Int?,
    val mensaje: String
)

data class LeerRequest(
    @SerializedName("conversacion_id") val conversacionId: Int
)
