package com.example.simtec_mobileapp.chat.data

import retrofit2.http.*

// =====================================================
// CHAT API — Interface Retrofit
// =====================================================
//
// Mapea los 5 endpoints del backend:
//   GET  /chat/conversaciones
//   GET  /chat/conversaciones/:id/mensajes
//   POST /chat/mensajes
//   PUT  /chat/mensajes/leer
//   GET  /chat/no-leidos
// =====================================================

interface ChatApi {

    @GET("chat/conversaciones")
    suspend fun getConversaciones(): ConversacionesResponse

    @GET("chat/conversaciones/{id}/mensajes")
    suspend fun getMensajes(
        @Path("id") conversacionId: Int,
        @Query("limit") limit: Int = 50,
        @Query("before_id") beforeId: Int? = null
    ): MensajesResponse

    @POST("chat/mensajes")
    suspend fun enviarMensaje(
        @Body body: EnviarMensajeRequest
    ): EnviarMensajeResponse

    @PUT("chat/mensajes/leer")
    suspend fun marcarLeidos(
        @Body body: LeerRequest
    ): GenericResponse

    @GET("chat/no-leidos")
    suspend fun getNoLeidos(): NoLeidosResponse
}
