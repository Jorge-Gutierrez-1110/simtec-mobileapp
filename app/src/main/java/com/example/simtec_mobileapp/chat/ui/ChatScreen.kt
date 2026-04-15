package com.example.simtec_mobileapp.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.simtec_mobileapp.chat.ChatViewModel
import com.example.simtec_mobileapp.chat.data.Conversacion
import com.example.simtec_mobileapp.chat.data.Mensaje
import java.text.SimpleDateFormat
import java.util.*

// =====================================================
// CHAT SCREEN — UI Principal del Chat (Jetpack Compose)
// =====================================================
//
// Pantalla completa con dos modos:
//   1. Lista de conversaciones (Admin/RH/Manager)
//   2. Hilo de mensajes (al seleccionar una conversacion)
//
// Para empleados (rolId >= 4):
//   - Salta directo a su conversacion o modo primer mensaje
// =====================================================

// Colores del chat
private val PrimaryBlue = Color(0xFF0052CC)
private val LightGray = Color(0xFFF1F5F9)
private val DarkText = Color(0xFF1E293B)
private val MutedText = Color(0xFF94A3B8)
private val BadgeRed = Color(0xFFEF4444)
private val SentBubble = Color(0xFF0052CC)
private val ReceivedBubble = Color(0xFFF1F5F9)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val conversaciones by viewModel.conversaciones.collectAsState()
    val mensajes by viewModel.mensajes.collectAsState()
    val activa by viewModel.conversacionActiva.collectAsState()
    val cargando by viewModel.cargando.collectAsState()
    val cargandoMensajes by viewModel.cargandoMensajes.collectAsState()
    val rolId = viewModel.rolId
    val isEmpleado = rolId >= 4

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (activa != null) {
                        Column {
                            Text(
                                text = "${activa!!.empNombre ?: ""} ${activa!!.empApellido ?: ""}".trim().ifEmpty { "Chat" },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (!activa!!.empPuesto.isNullOrEmpty()) {
                                Text(
                                    text = activa!!.empPuesto!!,
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Chat de Soporte",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (activa != null && !isEmpleado) {
                            viewModel.volverALista()
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryBlue
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                // Empleado sin conversacion — modo primer mensaje
                isEmpleado && activa == null && !cargando -> {
                    PrimerMensajeView(
                        onEnviar = { viewModel.enviarPrimerMensaje(it) }
                    )
                }
                // Conversacion activa — mostrar mensajes
                activa != null -> {
                    ConversacionView(
                        mensajes = mensajes,
                        cargando = cargandoMensajes,
                        usuarioId = viewModel.usuarioId,
                        onEnviar = { viewModel.enviarMensaje(it) }
                    )
                }
                // Lista de conversaciones (Admin/RH/Manager)
                else -> {
                    ListaConversaciones(
                        conversaciones = conversaciones,
                        cargando = cargando,
                        onSeleccionar = { viewModel.seleccionarConversacion(it) }
                    )
                }
            }
        }
    }
}

// =====================================================
// LISTA DE CONVERSACIONES
// =====================================================

@Composable
private fun ListaConversaciones(
    conversaciones: List<Conversacion>,
    cargando: Boolean,
    onSeleccionar: (Conversacion) -> Unit
) {
    var busqueda by remember { mutableStateOf("") }

    val filtradas = if (busqueda.isBlank()) {
        conversaciones
    } else {
        conversaciones.filter {
            val nombre = "${it.empNombre ?: ""} ${it.empApellido ?: ""}".lowercase()
            nombre.contains(busqueda.lowercase())
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Barra de busqueda
        OutlinedTextField(
            value = busqueda,
            onValueChange = { busqueda = it },
            placeholder = { Text("Buscar conversacion...", color = MutedText) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = MutedText)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        if (cargando) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else if (filtradas.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay conversaciones", color = MutedText, fontSize = 16.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filtradas, key = { it.id }) { conv ->
                    ConversacionItem(conv = conv, onClick = { onSeleccionar(conv) })
                    HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp)
                }
            }
        }
    }
}

// =====================================================
// ITEM DE CONVERSACION
// =====================================================

@Composable
private fun ConversacionItem(conv: Conversacion, onClick: () -> Unit) {
    val nombre = "${conv.empNombre ?: ""} ${conv.empApellido ?: ""}".trim().ifEmpty { "Empleado" }
    val inicial = nombre.first().uppercaseChar()
    val colores = listOf(
        Color(0xFF1976D2), Color(0xFF388E3C), Color(0xFFE64A19),
        Color(0xFF7B1FA2), Color(0xFF00796B), Color(0xFFC2185B)
    )
    val color = colores[conv.id % colores.size]

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = inicial.toString(),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = nombre,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = DarkText,
                    modifier = Modifier.weight(1f)
                )
                if (conv.ultimoMensajeFecha != null) {
                    Text(
                        text = formatTimeAgo(conv.ultimoMensajeFecha),
                        fontSize = 11.sp,
                        color = MutedText
                    )
                }
            }

            if (!conv.empPuesto.isNullOrEmpty()) {
                Text(
                    text = conv.empPuesto,
                    fontSize = 12.sp,
                    color = MutedText
                )
            }

            if (!conv.ultimoMensaje.isNullOrEmpty()) {
                Text(
                    text = conv.ultimoMensaje,
                    fontSize = 13.sp,
                    color = MutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Badge no leidos
        if (conv.noLeidos > 0) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(if (conv.noLeidos > 9) 26.dp else 22.dp)
                    .background(BadgeRed, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (conv.noLeidos > 9) "9+" else conv.noLeidos.toString(),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// =====================================================
// VISTA DE CONVERSACION (MENSAJES)
// =====================================================

// Sealed class para items del chat (mensaje o separador de fecha)
private sealed class ChatItem {
    data class MensajeItem(val mensaje: Mensaje, val esMio: Boolean) : ChatItem()
    data class FechaSeparador(val fecha: String) : ChatItem()
}

// Pre-computa la lista plana con separadores de fecha intercalados
private fun buildChatItems(mensajes: List<Mensaje>, usuarioId: Int): List<ChatItem> {
    val items = mutableListOf<ChatItem>()
    var lastDate = ""
    for (msg in mensajes) {
        val msgDate = formatDate(msg.createdAt)
        if (msgDate.isNotEmpty() && msgDate != lastDate) {
            lastDate = msgDate
            items.add(ChatItem.FechaSeparador(msgDate))
        }
        items.add(ChatItem.MensajeItem(msg, msg.usuarioId == usuarioId))
    }
    return items
}

@Composable
private fun ConversacionView(
    mensajes: List<Mensaje>,
    cargando: Boolean,
    usuarioId: Int,
    onEnviar: (String) -> Unit
) {
    var texto by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Pre-computar items con separadores de fecha (fuera del LazyColumn)
    val chatItems = remember(mensajes) { buildChatItems(mensajes, usuarioId) }

    // Auto-scroll: scroll instantaneo en carga inicial, animado para mensajes nuevos
    val prevSize = remember { mutableIntStateOf(0) }
    LaunchedEffect(chatItems.size) {
        if (chatItems.isNotEmpty()) {
            val lastIndex = chatItems.size - 1
            if (prevSize.intValue == 0) {
                // Carga inicial — scroll instantaneo sin animacion
                listState.scrollToItem(lastIndex)
            } else {
                // Mensaje nuevo — scroll animado
                listState.animateScrollToItem(lastIndex)
            }
            prevSize.intValue = chatItems.size
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding() // Fix: ajustar cuando el teclado se abre
    ) {
        // Lista de mensajes
        if (cargando) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else if (mensajes.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay mensajes aun.\nEscribe el primero.",
                    color = MutedText,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                itemsIndexed(
                    items = chatItems,
                    key = { index, item ->
                        when (item) {
                            is ChatItem.MensajeItem -> "msg_${item.mensaje.id}"
                            is ChatItem.FechaSeparador -> "sep_${index}_${item.fecha}"
                        }
                    }
                ) { _, item ->
                    when (item) {
                        is ChatItem.FechaSeparador -> DateSeparator(date = item.fecha)
                        is ChatItem.MensajeItem -> ChatBubble(
                            mensaje = item.mensaje,
                            esMio = item.esMio
                        )
                    }
                }
            }
        }

        // Barra de input
        InputBar(
            texto = texto,
            onTextoChange = { texto = it },
            onEnviar = {
                onEnviar(texto)
                texto = ""
            }
        )
    }
}

// =====================================================
// VISTA PRIMER MENSAJE (EMPLEADO)
// =====================================================

@Composable
private fun PrimerMensajeView(onEnviar: (String) -> Unit) {
    var texto by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Chat de Soporte",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = DarkText
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Escribe tu primer mensaje para iniciar\nuna conversacion con el equipo de soporte.",
            fontSize = 14.sp,
            color = MutedText,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = texto,
            onValueChange = { texto = it },
            placeholder = { Text("Escribe tu mensaje...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            minLines = 3,
            maxLines = 6
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                onEnviar(texto)
                texto = ""
            },
            enabled = texto.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
        ) {
            Text("Enviar Mensaje", fontWeight = FontWeight.SemiBold)
        }
    }
}

// =====================================================
// BURBUJA DE MENSAJE
// =====================================================

@Composable
private fun ChatBubble(mensaje: Mensaje, esMio: Boolean) {
    val alignment = if (esMio) Arrangement.End else Arrangement.Start
    val bubbleColor = if (esMio) SentBubble else ReceivedBubble
    val textColor = if (esMio) Color.White else DarkText
    val bubbleShape = if (esMio) {
        RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp)
    } else {
        RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = alignment
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(bubbleColor, bubbleShape)
                .padding(10.dp)
        ) {
            // Nombre del remitente (solo si no es mio)
            if (!esMio) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = mensaje.nombreRemitente ?: "Usuario",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryBlue
                    )
                    val rolLabel = when (mensaje.rolId) {
                        1 -> "Admin"
                        2 -> "RH"
                        3 -> "Manager"
                        6 -> "Director"
                        else -> null
                    }
                    if (rolLabel != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = rolLabel,
                            fontSize = 10.sp,
                            color = Color.White,
                            modifier = Modifier
                                .background(PrimaryBlue, RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
            }

            // Texto del mensaje
            Text(
                text = mensaje.mensaje,
                fontSize = 14.sp,
                color = textColor
            )

            // Hora + estado de lectura
            Row(
                modifier = Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTime(mensaje.createdAt),
                    fontSize = 10.sp,
                    color = if (esMio) Color.White.copy(alpha = 0.7f) else MutedText
                )
                if (esMio) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (mensaje.leido == 1) "\u2713\u2713" else "\u2713",
                        fontSize = 10.sp,
                        color = if (mensaje.leido == 1) Color(0xFF90CAF9) else Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

// =====================================================
// BARRA DE INPUT
// =====================================================

@Composable
private fun InputBar(
    texto: String,
    onTextoChange: (String) -> Unit,
    onEnviar: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = texto,
            onValueChange = onTextoChange,
            placeholder = { Text("Escribe un mensaje...", color = MutedText) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp),
            singleLine = false,
            maxLines = 4
        )

        Spacer(modifier = Modifier.width(8.dp))

        FloatingActionButton(
            onClick = onEnviar,
            modifier = Modifier.size(44.dp),
            containerColor = if (texto.isNotBlank()) PrimaryBlue else Color(0xFFCBD5E1),
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Enviar",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// =====================================================
// SEPARADOR DE FECHA
// =====================================================

@Composable
private fun DateSeparator(date: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date,
            fontSize = 12.sp,
            color = MutedText,
            modifier = Modifier
                .background(LightGray, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

// =====================================================
// HELPERS DE FORMATO
// =====================================================

private fun formatTime(isoDate: String?): String {
    if (isoDate.isNullOrEmpty()) return ""
    return try {
        val formats = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        )
        formats.forEach { it.timeZone = TimeZone.getTimeZone("UTC") }

        var date: Date? = null
        for (fmt in formats) {
            try { date = fmt.parse(isoDate); break } catch (_: Exception) {}
        }
        if (date == null) return ""

        val localFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        localFormat.timeZone = TimeZone.getDefault()
        localFormat.format(date)
    } catch (e: Exception) {
        ""
    }
}

private fun formatDate(isoDate: String?): String {
    if (isoDate.isNullOrEmpty()) return ""
    return try {
        val formats = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        )
        formats.forEach { it.timeZone = TimeZone.getTimeZone("UTC") }

        var date: Date? = null
        for (fmt in formats) {
            try { date = fmt.parse(isoDate); break } catch (_: Exception) {}
        }
        if (date == null) return ""

        val cal = Calendar.getInstance().apply { time = date }
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        when {
            cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "Hoy"
            cal.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                    cal.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) -> "Ayer"
            else -> {
                val displayFormat = SimpleDateFormat("d MMM yyyy", Locale("es", "ES"))
                displayFormat.format(date)
            }
        }
    } catch (e: Exception) {
        ""
    }
}

private fun formatTimeAgo(isoDate: String?): String {
    if (isoDate.isNullOrEmpty()) return ""
    return try {
        val formats = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        )
        formats.forEach { it.timeZone = TimeZone.getTimeZone("UTC") }

        var date: Date? = null
        for (fmt in formats) {
            try { date = fmt.parse(isoDate); break } catch (_: Exception) {}
        }
        if (date == null) return ""

        val diff = System.currentTimeMillis() - date.time
        val mins = diff / 60000
        val hours = mins / 60
        val days = hours / 24

        when {
            mins < 1 -> "ahora"
            mins < 60 -> "${mins}m"
            hours < 24 -> "${hours}h"
            days < 7 -> "${days}d"
            else -> {
                val displayFormat = SimpleDateFormat("d MMM", Locale("es", "ES"))
                displayFormat.format(date)
            }
        }
    } catch (e: Exception) {
        ""
    }
}
