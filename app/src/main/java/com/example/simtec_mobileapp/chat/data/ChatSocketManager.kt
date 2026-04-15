package com.example.simtec_mobileapp.chat.data

import android.util.Log
import com.example.simtec_mobileapp.chat.ChatNotificationHelper
import com.google.gson.Gson
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import java.net.URI

// =====================================================
// SOCKET.IO MANAGER — Conexion en tiempo real
// =====================================================
//
// Wrapper sobre la libreria Socket.IO Java para Android.
// Maneja conexion, rooms y eventos del chat.
//
// USO:
//   ChatSocketManager.connect("https://api.simtec-test.com")
//   ChatSocketManager.joinConversacion(convId)
//   ChatSocketManager.onNuevoMensaje { mensaje -> ... }
//   ChatSocketManager.disconnect()
// =====================================================

object ChatSocketManager {

    private const val TAG = "ChatSocket"
    private var socket: Socket? = null
    private val gson = Gson()

    // Callbacks
    private var onNuevoMensaje: ((Mensaje) -> Unit)? = null
    private var onMensajesLeidos: ((Int) -> Unit)? = null
    private var onConversacionActualizada: (() -> Unit)? = null
    private var onConectado: (() -> Unit)? = null
    private var onDesconectado: (() -> Unit)? = null

    val isConnected: Boolean get() = socket?.connected() == true

    // --- Helper para parsear args de Socket.IO a JSONObject ---
    private fun argsToJson(args: Array<Any>): JSONObject? {
        if (args.isEmpty()) {
            Log.w(TAG, "args vacio, no hay datos")
            return null
        }
        val raw = args[0]
        Log.d(TAG, "Tipo de arg[0]: ${raw?.javaClass?.name} | valor: $raw")
        return when (raw) {
            is JSONObject -> raw
            is String -> try { JSONObject(raw) } catch (e: Exception) {
                Log.e(TAG, "No se pudo parsear String a JSON: $raw"); null
            }
            else -> try { JSONObject(raw.toString()) } catch (e: Exception) {
                Log.e(TAG, "No se pudo convertir ${raw.javaClass.name} a JSON"); null
            }
        }
    }

    // --- Conectar al servidor ---
    fun connect(baseUrl: String) {
        try {
            if (socket?.connected() == true) {
                Log.d(TAG, "✅ Ya conectado (id=${socket?.id()}), ignorando connect()")
                return
            }

            // Si hay socket previo desconectado, limpiarlo
            socket?.let {
                Log.d(TAG, "Limpiando socket previo...")
                it.off()
                it.disconnect()
                socket = null
            }

            val opts = IO.Options().apply {
                transports = arrayOf("polling", "websocket")
                reconnection = true
                reconnectionAttempts = Int.MAX_VALUE  // Reconectar indefinidamente
                reconnectionDelay = 2000
                reconnectionDelayMax = 10000
                timeout = 20000
            }

            socket = IO.socket(URI.create(baseUrl), opts)

            // Eventos de conexion
            socket?.on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "✅✅✅ SOCKET CONECTADO — id: ${socket?.id()}")
                onConectado?.invoke()
            }

            socket?.on(Socket.EVENT_DISCONNECT) { args ->
                Log.w(TAG, "❌ SOCKET DESCONECTADO — razon: ${args.firstOrNull()}")
                onDesconectado?.invoke()
            }

            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                val error = args.firstOrNull()
                Log.e(TAG, "❌❌ ERROR DE CONEXION: $error")
                if (error is Exception) {
                    Log.e(TAG, "Detalle error: ${error.message}", error)
                }
            }

            // --- Eventos del Chat ---

            // Nuevo mensaje recibido (solo llega si estamos en la sala)
            socket?.on("chat:nuevo_mensaje") { args ->
                Log.d(TAG, "📩 Evento chat:nuevo_mensaje recibido (${args.size} args)")
                try {
                    val json = argsToJson(args) ?: return@on
                    val mensaje = gson.fromJson(json.toString(), Mensaje::class.java)
                    Log.d(TAG, "📩 Mensaje room: id=${mensaje.id} de ${mensaje.nombreRemitente}")
                    onNuevoMensaje?.invoke(mensaje)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing mensaje: ${e.message}", e)
                }
            }

            // Notificacion global (llega SIEMPRE, sin importar la sala)
            // Esto es lo que dispara las notificaciones push
            socket?.on("chat:notificacion") { args ->
                Log.d(TAG, "🔔🔔🔔 Evento chat:notificacion recibido (${args.size} args)")
                try {
                    val json = argsToJson(args) ?: return@on
                    Log.d(TAG, "🔔 JSON notificacion: $json")
                    val mensaje = gson.fromJson(json.toString(), Mensaje::class.java)
                    Log.d(TAG, "🔔 Notificacion parseada: id=${mensaje.id}, de=${mensaje.nombreRemitente}, usuarioId=${mensaje.usuarioId}")
                    Log.d(TAG, "🔔 Mi userId=${ChatRetrofitClient.getUserId()}, isChatVisible=${ChatNotificationHelper.isChatVisible}")
                    ChatNotificationHelper.mostrarNotificacion(mensaje)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing notificacion: ${e.message}", e)
                }
            }

            // Mensajes marcados como leidos
            socket?.on("chat:mensajes_leidos") { args ->
                try {
                    val json = argsToJson(args) ?: return@on
                    val convId = json.getInt("conversacion_id")
                    Log.d(TAG, "✓ Mensajes leidos en conv: $convId")
                    onMensajesLeidos?.invoke(convId)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing leidos: ${e.message}", e)
                }
            }

            // Conversacion actualizada (refrescar lista)
            socket?.on("chat:conversacion_actualizada") {
                Log.d(TAG, "🔄 Conversacion actualizada")
                onConversacionActualizada?.invoke()
            }

            socket?.connect()
            Log.d(TAG, "🔌 Conectando a $baseUrl ...")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fatal al conectar: ${e.message}", e)
        }
    }

    // --- Unirse a sala de conversacion ---
    fun joinConversacion(conversacionId: Int) {
        socket?.emit("chat:join", conversacionId)
        Log.d(TAG, "Join sala chat_$conversacionId")
    }

    // --- Salir de sala ---
    fun leaveConversacion(conversacionId: Int) {
        socket?.emit("chat:leave", conversacionId)
        Log.d(TAG, "Leave sala chat_$conversacionId")
    }

    // --- Registrar callbacks ---
    fun onNuevoMensaje(callback: (Mensaje) -> Unit) {
        onNuevoMensaje = callback
    }

    fun onMensajesLeidos(callback: (Int) -> Unit) {
        onMensajesLeidos = callback
    }

    fun onConversacionActualizada(callback: () -> Unit) {
        onConversacionActualizada = callback
    }

    fun onConectado(callback: () -> Unit) {
        onConectado = callback
    }

    fun onDesconectado(callback: () -> Unit) {
        onDesconectado = callback
    }

    // --- Desconectar ---
    fun disconnect() {
        socket?.off()
        socket?.disconnect()
        socket = null
        onNuevoMensaje = null
        onMensajesLeidos = null
        onConversacionActualizada = null
        onConectado = null
        onDesconectado = null
        Log.d(TAG, "Desconectado y limpiado")
    }
}
