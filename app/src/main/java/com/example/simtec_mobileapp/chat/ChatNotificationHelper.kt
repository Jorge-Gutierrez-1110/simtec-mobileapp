package com.example.simtec_mobileapp.chat

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.simtec_mobileapp.R
import com.example.simtec_mobileapp.chat.data.ChatRetrofitClient
import com.example.simtec_mobileapp.chat.data.Mensaje

// =====================================================
// CHAT NOTIFICATION HELPER — Notificaciones locales
// =====================================================
//
// Muestra notificaciones cuando llega un mensaje de chat
// y el usuario NO esta viendo la pantalla de chat.
//
// - Crea el NotificationChannel (Android 8+)
// - Muestra notificaciones con nombre del remitente
// - Al tocar, abre ChatActivity
// - Se suprime cuando ChatActivity esta activa
//
// USO:
//   ChatNotificationHelper.init(context)
//   ChatNotificationHelper.mostrarNotificacion(mensaje)
//   ChatNotificationHelper.isChatVisible = true/false
// =====================================================

object ChatNotificationHelper {

    private const val TAG = "ChatNotif"
    private const val CHANNEL_ID = "chat_mensajes"
    private const val CHANNEL_NAME = "Mensajes de Chat"
    private const val CHANNEL_DESC = "Notificaciones de nuevos mensajes en el chat de soporte"
    private const val NOTIFICATION_GROUP = "com.example.simtec_mobileapp.CHAT"

    private var appContext: Context? = null
    private var notifId = 1000

    /**
     * Cuando ChatActivity esta visible, NO mostrar notificaciones
     * (el mensaje ya se ve en pantalla).
     * ChatActivity pone esto en true/false en onResume/onPause.
     */
    @Volatile
    var isChatVisible: Boolean = false

    // --- Inicializar (llamar una vez en HomeActivity.onCreate) ---
    fun init(context: Context) {
        appContext = context.applicationContext
        crearCanal()
    }

    // --- Crear canal de notificacion (Android 8+) ---
    private fun crearCanal() {
        val ctx = appContext ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                setShowBadge(true)
            }

            val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
            Log.d(TAG, "Canal de notificacion creado: $CHANNEL_ID")
        }
    }

    // --- Mostrar notificacion por un mensaje nuevo ---
    fun mostrarNotificacion(mensaje: Mensaje) {
        Log.d(TAG, "=== mostrarNotificacion() INICIO ===")
        Log.d(TAG, "  mensaje.id=${mensaje.id}, usuario=${mensaje.usuarioId}, texto='${mensaje.mensaje}'")

        val ctx = appContext
        if (ctx == null) {
            Log.e(TAG, "❌ appContext es NULL — init() no fue llamado?")
            return
        }
        Log.d(TAG, "  ✓ appContext OK")

        // No notificar si el chat esta visible en pantalla
        if (isChatVisible) {
            Log.d(TAG, "  ⏭ Chat visible en pantalla, suprimida")
            return
        }
        Log.d(TAG, "  ✓ Chat NO visible")

        // No notificar mensajes propios
        val myUserId = ChatRetrofitClient.getUserId()
        Log.d(TAG, "  Mi userId=$myUserId vs mensaje.usuarioId=${mensaje.usuarioId}")
        if (mensaje.usuarioId == myUserId) {
            Log.d(TAG, "  ⏭ Es mi propio mensaje, suprimida")
            return
        }
        Log.d(TAG, "  ✓ No es mi mensaje")

        // Verificar permiso POST_NOTIFICATIONS (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permStatus = ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
            Log.d(TAG, "  Permiso POST_NOTIFICATIONS: ${if (permStatus == PackageManager.PERMISSION_GRANTED) "GRANTED" else "DENIED"}")
            if (permStatus != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "  ❌ Sin permiso POST_NOTIFICATIONS — no se puede mostrar")
                return
            }
        } else {
            Log.d(TAG, "  ✓ Android < 13, no requiere POST_NOTIFICATIONS")
        }

        // Verificar que el canal existe
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = manager.getNotificationChannel(CHANNEL_ID)
            Log.d(TAG, "  Canal '$CHANNEL_ID': ${if (channel != null) "existe (importance=${channel.importance})" else "NO EXISTE"}")
            if (channel == null) {
                Log.w(TAG, "  Recreando canal...")
                crearCanal()
            }
        }

        // Intent para abrir ChatActivity al tocar la notificacion
        val intent = Intent(ctx, ChatActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            ctx, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Construir notificacion
        val remitente = mensaje.nombreRemitente ?: "Nuevo mensaje"
        val rolLabel = when (mensaje.rolId) {
            1 -> " (Admin)"
            2 -> " (RH)"
            3 -> " (Manager)"
            else -> ""
        }
        val titulo = "$remitente$rolLabel"
        val texto = mensaje.mensaje

        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setGroup(NOTIFICATION_GROUP)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        try {
            val currentId = notifId++
            NotificationManagerCompat.from(ctx).notify(currentId, notification)
            Log.d(TAG, "  ✅✅✅ NOTIFICACION MOSTRADA (id=$currentId): $titulo — $texto")
        } catch (e: SecurityException) {
            Log.e(TAG, "  ❌ SecurityException: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "  ❌ Error inesperado: ${e.message}", e)
        }
        Log.d(TAG, "=== mostrarNotificacion() FIN ===")
    }

    // --- Test: disparar notificacion de prueba (para verificar pipeline) ---
    fun testNotificacion() {
        Log.d(TAG, "=== TEST NOTIFICACION ===")
        val fakeMensaje = Mensaje(
            id = -1,
            conversacionId = 0,
            usuarioId = -999, // ID que nunca sera el del usuario actual
            mensaje = "Esta es una notificacion de prueba. Si la ves, el sistema funciona correctamente.",
            leido = 0,
            createdAt = null,
            nombreRemitente = "Sistema de Prueba",
            imagenPerfil = null,
            rolId = 1
        )
        // Forzar isChatVisible = false temporalmente para que no se suprima
        val wasVisible = isChatVisible
        isChatVisible = false
        mostrarNotificacion(fakeMensaje)
        isChatVisible = wasVisible
    }

    // --- Limpiar notificaciones de chat ---
    fun limpiarNotificaciones() {
        val ctx = appContext ?: return
        val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancelAll()
    }
}
