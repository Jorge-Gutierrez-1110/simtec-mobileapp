package com.example.simtec_mobileapp.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.simtec_mobileapp.chat.data.ChatRetrofitClient
import com.example.simtec_mobileapp.chat.data.ChatRepository
import com.example.simtec_mobileapp.chat.data.Conversacion
import com.example.simtec_mobileapp.chat.data.Mensaje
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// =====================================================
// VIEWMODEL — Logica del Chat
// =====================================================
//
// Maneja el estado del chat: lista de conversaciones,
// mensajes activos, no leidos, y eventos en tiempo real.
//
// USO en Compose:
//   val viewModel: ChatViewModel = viewModel()
//   val conversaciones by viewModel.conversaciones.collectAsState()
//   val mensajes by viewModel.mensajes.collectAsState()
// =====================================================

class ChatViewModel : ViewModel() {

    // URL del backend (sin trailing slash para Socket.IO)
    private val BASE_URL = "https://api.simtec-test.com"

    private val repository = ChatRepository()

    // --- Estados observables ---
    private val _conversaciones = MutableStateFlow<List<Conversacion>>(emptyList())
    val conversaciones: StateFlow<List<Conversacion>> = _conversaciones.asStateFlow()

    private val _mensajes = MutableStateFlow<List<Mensaje>>(emptyList())
    val mensajes: StateFlow<List<Mensaje>> = _mensajes.asStateFlow()

    private val _conversacionActiva = MutableStateFlow<Conversacion?>(null)
    val conversacionActiva: StateFlow<Conversacion?> = _conversacionActiva.asStateFlow()

    private val _noLeidos = MutableStateFlow(0)
    val noLeidos: StateFlow<Int> = _noLeidos.asStateFlow()

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private val _cargandoMensajes = MutableStateFlow(false)
    val cargandoMensajes: StateFlow<Boolean> = _cargandoMensajes.asStateFlow()

    // Lee datos de sesion desde las mismas SharedPreferences que SessionManager
    val usuarioId: Int get() = ChatRetrofitClient.getUserId()
    val rolId: Int get() = ChatRetrofitClient.getRolId()

    init {
        inicializarSocket()
        cargarConversaciones()
        cargarNoLeidos()

        // Si es empleado, cargar su conversacion directamente
        if (rolId >= 4) {
            cargarConversacionEmpleado()
        }
    }

    // --- Inicializar Socket.IO y escuchar eventos ---
    private fun inicializarSocket() {
        repository.inicializarSocket(BASE_URL)

        // Nuevo mensaje en tiempo real
        viewModelScope.launch {
            repository.mensajesEnTiempoReal.collect { mensaje ->
                val activa = _conversacionActiva.value
                if (activa != null && mensaje.conversacionId == activa.id) {
                    _mensajes.value = _mensajes.value.toMutableList().apply {
                        if (none { it.id == mensaje.id }) add(mensaje)
                    }
                    if (mensaje.usuarioId != usuarioId) {
                        marcarLeidos(activa.id)
                    }
                } else {
                    if (mensaje.usuarioId != usuarioId) {
                        _noLeidos.value = _noLeidos.value + 1
                    }
                }
                cargarConversaciones()
            }
        }

        // Senal de refrescar conversaciones
        viewModelScope.launch {
            repository.refrescarConversaciones.collect {
                cargarConversaciones()
                cargarNoLeidos()
            }
        }

        // Mensajes marcados como leidos
        viewModelScope.launch {
            repository.mensajesLeidos.collect { convId ->
                val activa = _conversacionActiva.value
                if (activa != null && convId == activa.id) {
                    _mensajes.value = _mensajes.value.map { it.copy(leido = 1) }
                }
            }
        }
    }

    // --- Cargar conversacion del empleado automaticamente ---
    private fun cargarConversacionEmpleado() {
        viewModelScope.launch {
            _cargando.value = true
            val convs = repository.getConversaciones()
            _conversaciones.value = convs
            // Si el empleado ya tiene conversacion, seleccionarla
            if (convs.isNotEmpty()) {
                seleccionarConversacion(convs.first())
            }
            _cargando.value = false
        }
    }

    // --- Cargar lista de conversaciones ---
    fun cargarConversaciones() {
        viewModelScope.launch {
            _cargando.value = true
            _conversaciones.value = repository.getConversaciones()
            _cargando.value = false
        }
    }

    // --- Cargar no leidos ---
    fun cargarNoLeidos() {
        viewModelScope.launch {
            _noLeidos.value = repository.getNoLeidos()
        }
    }

    // --- Seleccionar conversacion ---
    fun seleccionarConversacion(conv: Conversacion) {
        _conversacionActiva.value?.let { repository.leaveConversacion(it.id) }

        _conversacionActiva.value = conv
        repository.joinConversacion(conv.id)

        viewModelScope.launch {
            _cargandoMensajes.value = true
            _mensajes.value = repository.getMensajes(conv.id)
            _cargandoMensajes.value = false
            marcarLeidos(conv.id)
        }
    }

    // --- Volver a la lista (deseleccionar) ---
    fun volverALista() {
        _conversacionActiva.value?.let { repository.leaveConversacion(it.id) }
        _conversacionActiva.value = null
        _mensajes.value = emptyList()
        cargarConversaciones()
    }

    // --- Enviar mensaje ---
    fun enviarMensaje(texto: String) {
        if (texto.isBlank()) return
        val convId = _conversacionActiva.value?.id

        viewModelScope.launch {
            repository.enviarMensaje(convId, texto)
        }
    }

    // --- Enviar primer mensaje (empleado sin conversacion) ---
    fun enviarPrimerMensaje(texto: String) {
        if (texto.isBlank()) return

        viewModelScope.launch {
            val resultado = repository.enviarMensaje(null, texto)
            if (resultado != null) {
                cargarConversaciones()
                val convId = resultado.conversacionId
                repository.joinConversacion(convId)

                val convs = repository.getConversaciones()
                _conversaciones.value = convs
                val conv = convs.find { it.id == convId }
                if (conv != null) {
                    _conversacionActiva.value = conv
                    _mensajes.value = repository.getMensajes(convId)
                }
            }
        }
    }

    // --- Marcar como leidos ---
    private fun marcarLeidos(conversacionId: Int) {
        viewModelScope.launch {
            repository.marcarLeidos(conversacionId)
            _noLeidos.value = repository.getNoLeidos()
        }
    }

    // --- Limpieza ---
    override fun onCleared() {
        super.onCleared()
        // Solo salir de la sala activa, NO desconectar el socket global
        // (el socket se mantiene vivo en HomeActivity para notificaciones)
        _conversacionActiva.value?.let { repository.leaveConversacion(it.id) }
    }
}
