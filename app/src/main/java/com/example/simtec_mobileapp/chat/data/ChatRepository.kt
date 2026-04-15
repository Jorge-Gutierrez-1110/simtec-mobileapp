package com.example.simtec_mobileapp.chat.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

// =====================================================
// REPOSITORY — Capa de datos del Chat
// =====================================================
//
// Combina Retrofit (HTTP) + Socket.IO (tiempo real).
// El ViewModel consume este repository.
// =====================================================

class ChatRepository {

    private val api = ChatRetrofitClient.chatApi

    // Flow para mensajes en tiempo real (el ViewModel colecta esto)
    private val _mensajesEnTiempoReal = MutableSharedFlow<Mensaje>(extraBufferCapacity = 64)
    val mensajesEnTiempoReal: SharedFlow<Mensaje> = _mensajesEnTiempoReal

    // Flow para senal de "refrescar conversaciones"
    private val _refrescarConversaciones = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    val refrescarConversaciones: SharedFlow<Unit> = _refrescarConversaciones

    // Flow para mensajes leidos
    private val _mensajesLeidos = MutableSharedFlow<Int>(extraBufferCapacity = 8)
    val mensajesLeidos: SharedFlow<Int> = _mensajesLeidos

    // --- Inicializar Socket.IO y sus listeners ---
    fun inicializarSocket(baseUrl: String) {
        ChatSocketManager.connect(baseUrl)

        ChatSocketManager.onNuevoMensaje { mensaje ->
            _mensajesEnTiempoReal.tryEmit(mensaje)
        }

        ChatSocketManager.onConversacionActualizada {
            _refrescarConversaciones.tryEmit(Unit)
        }

        ChatSocketManager.onMensajesLeidos { convId ->
            _mensajesLeidos.tryEmit(convId)
        }
    }

    // --- API HTTP ---

    suspend fun getConversaciones(): List<Conversacion> {
        return try {
            val response = api.getConversaciones()
            if (response.success) response.conversaciones else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getMensajes(conversacionId: Int, limit: Int = 50, beforeId: Int? = null): List<Mensaje> {
        return try {
            val response = api.getMensajes(conversacionId, limit, beforeId)
            if (response.success) response.mensajes else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun enviarMensaje(conversacionId: Int?, texto: String): Mensaje? {
        return try {
            val body = EnviarMensajeRequest(conversacionId, texto)
            val response = api.enviarMensaje(body)
            if (response.success) response.mensaje else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun marcarLeidos(conversacionId: Int) {
        try {
            api.marcarLeidos(LeerRequest(conversacionId))
        } catch (_: Exception) {}
    }

    suspend fun getNoLeidos(): Int {
        return try {
            val response = api.getNoLeidos()
            if (response.success) response.total else 0
        } catch (e: Exception) {
            0
        }
    }

    // --- Socket.IO rooms ---

    fun joinConversacion(conversacionId: Int) {
        ChatSocketManager.joinConversacion(conversacionId)
    }

    fun leaveConversacion(conversacionId: Int) {
        ChatSocketManager.leaveConversacion(conversacionId)
    }

    fun desconectar() {
        ChatSocketManager.disconnect()
    }
}
