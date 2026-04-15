package com.example.simtec_mobileapp.chat

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.example.simtec_mobileapp.chat.data.ChatRetrofitClient
import com.example.simtec_mobileapp.chat.ui.ChatScreen
import com.example.simtec_mobileapp.ui.theme.SimtecmobileappTheme

// =====================================================
// CHAT ACTIVITY — Puente entre Activities XML y Compose
// =====================================================
//
// Esta Activity aloja el ChatScreen (Jetpack Compose).
// Se abre desde HomeActivity via:
//   startActivity(Intent(this, ChatActivity::class.java))
//
// Controla el flag isChatVisible para suprimir
// notificaciones cuando el chat esta en pantalla.
// =====================================================

class ChatActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ChatRetrofitClient.init(this)

        setContent {
            SimtecmobileappTheme {
                ChatScreen(
                    onBack = { finish() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Chat visible — suprimir notificaciones
        ChatNotificationHelper.isChatVisible = true
        // Limpiar notificaciones pendientes al abrir el chat
        ChatNotificationHelper.limpiarNotificaciones()
    }

    override fun onPause() {
        super.onPause()
        // Chat ya no visible — permitir notificaciones
        ChatNotificationHelper.isChatVisible = false
    }
}
