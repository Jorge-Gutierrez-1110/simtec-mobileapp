package com.example.simtec_mobileapp

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import java.util.Calendar
import java.util.concurrent.TimeUnit

import com.example.simtec_mobileapp.Periodo
import com.example.simtec_mobileapp.EmpleadoNomina
import com.example.simtec_mobileapp.NominaResumen
import com.example.simtec_mobileapp.NominaCierreRequest
import com.example.simtec_mobileapp.DetalleNomina

class ApiClient {

    companion object {
        private const val BASE_URL = "https://api.simtec-test.com"
        private const val TIMEOUT_SECONDS = 15L
    }

    private var authToken: String? = null

    fun setAuthToken(token: String?) {
        authToken = token
    }

    private val gson = Gson()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    /**
     * Realiza un login con email y password
     * Retorna una respuesta parseada o null si hay error
     */
    suspend fun login(email: String, password: String): LoginResponse? {
        return try {
            val payload = JsonObject().apply {
                addProperty("email", email)
                addProperty("password", password)
            }

            val requestBody = gson.toJson(payload)
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$BASE_URL/login")
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build()

            authToken = null

            Log.d("ApiClient", "═══════════════════════════════════")
            Log.d("ApiClient", "ENVIANDO LOGIN")
            Log.d("ApiClient", "URL: ${request.url}")
            Log.d("ApiClient", "Email: $email")
            Log.d("ApiClient", "Password: (oculto)")
            Log.d("ApiClient", "═══════════════════════════════════")

            val response = withContext(Dispatchers.IO) {
                httpClient.newCall(request).execute()
            }

            Log.d("ApiClient", "═══════════════════════════════════")
            Log.d("ApiClient", "RESPUESTA RECIBIDA")
            Log.d("ApiClient", "Status Code: ${response.code}")
            Log.d("ApiClient", "Is Successful: ${response.isSuccessful}")

            val responseBody = response.body?.string()
            Log.d("ApiClient", "Response Body: $responseBody")
            Log.d("ApiClient", "═══════════════════════════════════")

            if (responseBody != null) {
                try {
                    val loginResponse = gson.fromJson(responseBody, LoginResponse::class.java)

                    Log.d("ApiClient", "PARSEADO EXITOSAMENTE:")
                    Log.d("ApiClient", "  success: ${loginResponse.success}")
                    Log.d("ApiClient", "  token: ${if (loginResponse.token != null) "✓ Presente" else "✗ Null"}")
                    Log.d("ApiClient", "  user: ${if (loginResponse.user != null) "✓ ${loginResponse.user.email}" else "✗ Null"}")
                    Log.d("ApiClient", "  message: ${loginResponse.message}")

                    loginResponse
                } catch (e: Exception) {
                    Log.e("ApiClient", "❌ ERROR AL PARSEAR JSON: ${e.message}")
                    e.printStackTrace()
                    null
                }
            } else {
                Log.e("ApiClient", "❌ Response body es null")
                null
            }

        } catch (e: Exception) {
            Log.e("ApiClient", "❌ EXCEPCIÓN EN LOGIN: ${e.message}", e)
            e.printStackTrace()
            null
        }
    }

    /**
     * Guarda un registro de asistencia en el servidor
     * Retorna AttendanceResponse o null si hay error
     */
    suspend fun saveAttendance(
        empleadoId: Int,
        fecha: String,
        horaEntrada: String?,
        horaSalida: String?,
        tipo: String,
        ubicacion: String? = null,
        foto: String? = null
    ): AttendanceResponse? {
        return try {
            val payload = JsonObject().apply {
                addProperty("empleado_id", empleadoId)
                addProperty("fecha", fecha)
                if (horaEntrada != null) addProperty("hora_entrada", horaEntrada)
                if (horaSalida != null) addProperty("hora_salida", horaSalida)
                addProperty("dispositivo", "mobile")
                if (ubicacion != null) addProperty("ubicacion", ubicacion)
                if (foto != null) addProperty("foto", foto)
            }

            val requestBody = gson.toJson(payload)
                .toRequestBody("application/json".toMediaType())

            val requestBuilder = Request.Builder()
                .url("$BASE_URL/asistencia/registrar")
                .post(requestBody)
                .addHeader("Content-Type", "application/json")

            if (authToken != null) {
                requestBuilder.addHeader("Authorization", "Bearer $authToken")
            }

            val request = requestBuilder.build()

            Log.d("ApiClient", "📤 GUARDANDO ASISTENCIA")
            Log.d("ApiClient", "  URL: ${request.url}")
            Log.d("ApiClient", "  Empleado: $empleadoId, Tipo: $tipo, Fecha: $fecha")
            Log.d("ApiClient", "  Ubicación: ${ubicacion?.take(50)}...")
            Log.d("ApiClient", "  Foto: ${if (foto != null) "Presente (${foto.length} chars)" else "NULL"}")

            val response = withContext(Dispatchers.IO) {
                httpClient.newCall(request).execute()
            }

            Log.d("ApiClient", "  Status: ${response.code}")

            val responseBody = response.body?.string()
            Log.d("ApiClient", "  Response: $responseBody")

            if (responseBody != null) {
                try {
                    val attendanceResponse = gson.fromJson(responseBody, AttendanceResponse::class.java)
                    Log.d("ApiClient", "  Success: ${attendanceResponse.success}")
                    attendanceResponse
                } catch (e: Exception) {
                    Log.e("ApiClient", "❌ ERROR PARSE: ${e.message}")
                    null
                }
            } else {
                null
            }

        } catch (e: Exception) {
            Log.e("ApiClient", "❌ EXCEPCIÓN: ${e.message}", e)
            null
        }
    }

    /**
     * Data class para la respuesta del login
     */
    data class LoginResponse(
        val success: Boolean = false,
        val token: String? = null,
        val user: UserData? = null,
        val message: String? = null
    )

    /**
     * Data class para los datos del usuario devueltos por el API
     */
    data class UserData(
        val id: Int = 0,
        val nombre: String = "",
        val email: String = "",
        val rol_id: Int = 0,
        val rol: String = "",
        val empleado_id: Int = 0,
        val permisos: List<String> = emptyList()
    )

    /**
     * Data class para la respuesta de asistencia
     */
    data class AttendanceResponse(
        val success: Boolean = false,
        val message: String? = null,
        val calculo: AttendanceCalculo? = null
    )

    data class AttendanceCalculo(
        val estatus: String = "",
        val extras: Int = 0
    )

    /**
     * Obtiene el historial de asistencia del empleado desde el servidor
     */
    suspend fun getAttendanceHistory(empleadoId: Int): List<AttendanceRecord>? {
        return try {
            val request = Request.Builder()
                .url("$BASE_URL/asistencia/historial/$empleadoId")
                .get()
                .addHeader("Content-Type", "application/json")
                .build()

            Log.d("ApiClient", "📤 OBTENIENDO HISTORIAL")
            Log.d("ApiClient", "  URL: ${request.url}")

            val response = withContext(Dispatchers.IO) {
                httpClient.newCall(request).execute()
            }

            Log.d("ApiClient", "  Status: ${response.code}")

            val responseBody = response.body?.string()
            Log.d("ApiClient", "  Response: $responseBody")

            if (response.isSuccessful && responseBody != null) {
                val type = object : com.google.gson.reflect.TypeToken<List<AttendanceRecord>>() {}.type
                val records: List<AttendanceRecord> = gson.fromJson(responseBody, type)
                Log.d("ApiClient", "  Registros obtenidos: ${records.size}")
                records
            } else {
                null
            }

        } catch (e: Exception) {
            Log.e("ApiClient", "❌ EXCEPCIÓN: ${e.message}", e)
            null
        }
    }

    data class AttendanceRecord(
        val id: Int = 0,
        val empleado_id: Int = 0,
        val fecha: String = "",
        val hora_entrada: String? = null,
        val hora_salida: String? = null,
        val estatus: String = "",
        val comentarios: String? = null
    )

    // ============ NÓMINA API ============

    /**
     * Genera el pre-cálculo de nómina para un período
     */
    suspend fun preCalculoNomina(periodoId: Int): NominaResponse? {
        return try {
            val payload = JsonObject().apply {
                addProperty("periodo_id", periodoId)
            }

            val requestBody = gson.toJson(payload)
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$BASE_URL/nomina/pre-calculo")
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer $authToken")
                .build()

            Log.d("ApiClient", "📤 PRE-CÁLCULO NÓMINA")
            Log.d("ApiClient", "  URL: ${request.url}")
            Log.d("ApiClient", "  Periodo ID: $periodoId")

            val response = withContext(Dispatchers.IO) {
                httpClient.newCall(request).execute()
            }

            Log.d("ApiClient", "  Status: ${response.code}")

            val responseBody = response.body?.string()
            Log.d("ApiClient", "  Response: $responseBody")

            if (responseBody != null) {
                gson.fromJson(responseBody, NominaResponse::class.java)
            } else {
                null
            }

        } catch (e: Exception) {
            Log.e("ApiClient", "❌ EXCEPCIÓN: ${e.message}", e)
            null
        }
    }

    /**
     * Cierra la nómina de un período
     */
    suspend fun cerrarNomina(requestData: NominaCierreRequest): NominaResponse? {
        return try {
            val requestBody = gson.toJson(requestData)
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$BASE_URL/nomina/cerrar")
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer $authToken")
                .build()

            Log.d("ApiClient", "📤 CERRAR NÓMINA")
            Log.d("ApiClient", "  URL: ${request.url}")

            val response = withContext(Dispatchers.IO) {
                httpClient.newCall(request).execute()
            }

            Log.d("ApiClient", "  Status: ${response.code}")

            val responseBody = response.body?.string()
            Log.d("ApiClient", "  Response: $responseBody")

            if (responseBody != null) {
                gson.fromJson(responseBody, NominaResponse::class.java)
            } else {
                null
            }

        } catch (e: Exception) {
            Log.e("ApiClient", "❌ EXCEPCIÓN: ${e.message}", e)
            null
        }
    }

    /**
     * Obtiene un registro de cualquier catálogo por ID
     */
    suspend fun getCatalogoById(tabla: String, id: Int): Map<String, Any>? {
        return try {
            val request = Request.Builder()
                .url("$BASE_URL/config/catalogo/$tabla/$id")
                .get()
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer $authToken")
                .build()

            Log.d("ApiClient", "📤 OBTENIENDO CATALOGO: $tabla id=$id")

            val response = withContext(Dispatchers.IO) {
                httpClient.newCall(request).execute()
            }

            val responseBody = response.body?.string()
            Log.d("ApiClient", "  Response: $responseBody")

            if (response.isSuccessful && responseBody != null) {
                gson.fromJson(responseBody, Map::class.java) as? Map<String, Any>
            } else {
                null
            }

        } catch (e: Exception) {
            Log.e("ApiClient", "❌ EXCEPCIÓN: ${e.message}", e)
            null
        }
    }

    /**
     * Obtiene los datos completos del empleado (perfil)
     */
    suspend fun getEmpleadoCompleto(empleadoId: Int): EmpleadoPerfil? {
        return try {
            val request = Request.Builder()
                .url("$BASE_URL/empleado-completo/$empleadoId")
                .get()
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer $authToken")
                .build()

            Log.d("ApiClient", "📤 OBTENIENDO PERFIL EMPLEADO")
            Log.d("ApiClient", "  URL: ${request.url}")

            val response = withContext(Dispatchers.IO) {
                httpClient.newCall(request).execute()
            }

            Log.d("ApiClient", "  Status: ${response.code}")

            val responseBody = response.body?.string()
            Log.d("ApiClient", "  Response: $responseBody")

            if (response.isSuccessful && responseBody != null) {
                val responseObj = gson.fromJson(responseBody, EmpleadoPerfilResponse::class.java)
                responseObj.empleado
            } else {
                null
            }

        } catch (e: Exception) {
            Log.e("ApiClient", "❌ EXCEPCIÓN: ${e.message}", e)
            null
        }
    }

    data class EmpleadoPerfilResponse(
        val empleado: EmpleadoPerfil? = null,
        val documentos: List<Any> = emptyList()
    )

    data class EmpleadoPerfil(
        val id: Int = 0,
        val numero_empleado: String = "",
        val nombre: String = "",
        val apellido_paterno: String = "",
        val apellido_materno: String? = null,
        val puesto: String? = null,
        val departamento: String? = null,
        val email: String = "",
        val telefono: String? = null,
        val fecha_nacimiento: String? = null,
        val rfc: String? = null,
        val curp: String? = null,
        val nss: String? = null,
        val imagen_perfil: String? = null,
        val calle: String? = null,
        val numero_exterior: String? = null,
        val colonia: String? = null,
        val cp: String? = null,
        val ciudad: String? = null,
        val estado: String? = null,
        val turno_id: Int? = null,
        val nombre_turno: String? = null,
        val hora_entrada: String? = null,
        val hora_salida: String? = null,
        val cliente_id: Int? = null,
        val empresa_nombre: String? = null,
        val manager_id: Int? = null,
        val manager_nombre: String? = null,
        val tipo_nomina: String? = null,
        val salario_diario: Double = 0.0,
        val salario_diario_integrado: Double = 0.0,
        val periodo_pago: String? = null,
        val fecha_ingreso: String? = null,
        val status: String = "",
        val banco: String? = null,
        val cuenta_bancaria: String? = null,
        val clabe_interbancaria: String? = null,
        val usuario_email: String? = null,
        // Datos de geocerca de la empresa
        val geocerca_activa: Int? = null,
        val geocerca_latitud: Double? = null,
        val geocerca_longitud: Double? = null,
        val geocerca_radio_metros: Int? = null
    )

    data class GastosCatalogosResponse(
        val categorias: List<CatGasto>? = null,
        val proveedores: List<Proveedor>? = null,
        val empleados: List<EmpleadoBasic>? = null
    )

    data class CatGasto(
        val id: Int = 0,
        val nombre: String = "",
        val color_hex: String? = null
    )

    data class Proveedor(
        val id: Int = 0,
        val razon_social: String = "",
        val nombre_comercial: String = "",
        val rfc: String = "",
        val telefono: String? = null,
        val banco: String? = null,
        val cuenta_clabe: String? = null,
        val dias_credito: Int = 0
    )

    data class EmpleadoBasic(
        val id: Int = 0,
        val nombre: String = "",
        val apellido_paterno: String = "",
        val numero_empleado: String = ""
    )

    data class GastoRecord(
        val id: Int = 0,
        val folio: String? = null,
        val proveedor_id: Int? = null,
        val categoria_id: Int? = null,
        val solicitante_id: Int? = null,
        val beneficiario_id: Int? = null,
        val concepto: String = "",
        val subtotal: Double = 0.0,
        val iva: Double = 0.0,
        val retenciones: Double = 0.0,
        val total: Double = 0.0,
        val fecha_emision: String? = null,
        val fecha_vencimiento: String? = null,
        val factura_url: String? = null,
        val estatus: String = "",
        val cliente_id: Int? = null,
        val created_at: String? = null,
        val proveedor: String? = null,
        val categoria: String? = null,
        val categoria_color: String? = null,
        val solicitante: String? = null,
        val cliente_nombre: String? = null,
        val fecha_pago: String? = null,
        val pago_url: String? = null
    )

    suspend fun getGastosCatalogos(): GastosCatalogosResponse? {
        return try {
            val request = Request.Builder()
                .url("$BASE_URL/gastos/catalogos")
                .get()
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer $authToken")
                .build()

            val response = withContext(Dispatchers.IO) {
                httpClient.newCall(request).execute()
            }

            if (response.isSuccessful) {
                val body = response.body?.string()
                gson.fromJson(body, GastosCatalogosResponse::class.java)
            } else null

        } catch (e: Exception) {
            Log.e("ApiClient", "Error catalogo gastos: ${e.message}")
            null
        }
    }

    suspend fun getGastosTablero(): List<GastoRecord>? {
        return try {
            val request = Request.Builder()
                .url("$BASE_URL/gastos/tablero")
                .get()
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer $authToken")
                .build()

            val response = withContext(Dispatchers.IO) {
                httpClient.newCall(request).execute()
            }

            if (response.isSuccessful) {
                val body = response.body?.string()
                val type = object : com.google.gson.reflect.TypeToken<List<GastoRecord>>() {}.type
                gson.fromJson(body, type)
            } else null

        } catch (e: Exception) {
            Log.e("ApiClient", "Error tablero gastos: ${e.message}")
            null
        }
    }

    suspend fun solicitarGasto(
        proveedor_id: Int,
        categoria_id: Int,
        solicitante_id: Int,
        beneficiario_id: Int?,
        concepto: String,
        subtotal: Double,
        iva: Double,
        retenciones: Double,
        total: Double,
        fecha_emision: String,
        fecha_vencimiento: String?,
        cliente_id: Int?
    ): Map<String, Any>? {
        return try {
            val json = gson.toJson(mapOf(
                "proveedor_id" to proveedor_id,
                "categoria_id" to categoria_id,
                "solicitante_id" to solicitante_id,
                "beneficiario_id" to (beneficiario_id ?: ""),
                "concepto" to concepto,
                "subtotal" to subtotal,
                "iva" to iva,
                "retenciones" to retenciones,
                "total" to total,
                "fecha_emision" to fecha_emision,
                "fecha_vencimiento" to (fecha_vencimiento ?: ""),
                "cliente_id" to (cliente_id ?: "")
            ))

            val requestBody = json.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$BASE_URL/gastos/solicitar")
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer $authToken")
                .build()

            val response = withContext(Dispatchers.IO) {
                httpClient.newCall(request).execute()
            }

            if (response.isSuccessful) {
                val body = response.body?.string()
                gson.fromJson(body, object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type)
            } else null

        } catch (e: Exception) {
            Log.e("ApiClient", "Error solicitar gasto: ${e.message}")
            null
        }
    }

    suspend fun getPeriodos(): List<Periodo>? {
        return try {
            val anio = Calendar.getInstance().get(Calendar.YEAR)
            val request = Request.Builder()
                .url("$BASE_URL/nomina/periodos?anio=$anio")
                .get()
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer $authToken")
                .build()

            val response = withContext(Dispatchers.IO) {
                httpClient.newCall(request).execute()
            }

            if (response.isSuccessful) {
                val body = response.body?.string()
                val type = object : TypeToken<List<Periodo>>() {}.type
                @Suppress("UNCHECKED_CAST")
                (gson.fromJson(body, type) as? List<Periodo>)
            } else null

        } catch (e: Exception) {
            Log.e("ApiClient", "Error getPeriodos: ${e.message}")
            null
        }
    }

    suspend fun cerrarNominaAsMap(request: NominaCierreRequest): Map<String, Any>? {
        return try {
            val json = gson.toJson(request)
            val requestBody = json.toRequestBody("application/json".toMediaType())

            val httpRequest = Request.Builder()
                .url("$BASE_URL/nomina/cerrar")
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer $authToken")
                .build()

            val response = withContext(Dispatchers.IO) {
                httpClient.newCall(httpRequest).execute()
            }

            if (response.isSuccessful) {
                val body = response.body?.string()
                gson.fromJson(body, object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type)
            } else null

        } catch (e: Exception) {
            Log.e("ApiClient", "Error cerrarNominaAsMap: ${e.message}")
            null
        }
    }

    suspend fun enviarEmailNomina(nominaId: Int, email: String): Map<String, Any>? {
        return try {
            val json = gson.toJson(mapOf("nomina_id" to nominaId, "email" to email))
            val requestBody = json.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$BASE_URL/nomina/enviar-email")
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer $authToken")
                .build()

            val response = withContext(Dispatchers.IO) {
                httpClient.newCall(request).execute()
            }

            if (response.isSuccessful) {
                val body = response.body?.string()
                gson.fromJson(body, object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type)
            } else null

        } catch (e: Exception) {
            Log.e("ApiClient", "Error enviarEmailNomina: ${e.message}")
            null
        }
    }

    suspend fun getNominaDetalle(nominaId: Int): List<EmpleadoNomina>? {
        return try {
            val request = Request.Builder()
                .url("$BASE_URL/nomina/detalle/$nominaId")
                .get()
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer $authToken")
                .build()

            val response = withContext(Dispatchers.IO) {
                httpClient.newCall(request).execute()
            }

            if (response.isSuccessful) {
                val body = response.body?.string()
                val type = object : TypeToken<List<EmpleadoNomina>>() {}.type
                gson.fromJson(body, type)
            } else null

        } catch (e: Exception) {
            Log.e("ApiClient", "Error getNominaDetalle: ${e.message}")
            null
        }
    }

    // ============ SOLICITUDES DE NÓMINA ============

    data class NominaSolicitud(
        val id: Int = 0,
        val emp_nombre: String = "",
        val emp_apellido: String = "",
        val tipo: String = "",
        val concepto_nombre: String = "",
        val periodo_nombre: String = "",
        val periodo_id: Int = 0,
        val monto: Double = 0.0,
        val estatus: String = "PENDIENTE",
        val comentarios: String? = null,
        val evidencia_url: String? = null,
        val solicitante_id: Int = 0,
        val aprobador_id: Int? = null,
        val concepto_id: Int = 0
    )

    suspend fun getEmpleadosNomina(): List<EmpleadoNomina>? {
        return try {
            val request = Request.Builder()
                .url("$BASE_URL/empleados-nomina")
                .get()
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer $authToken")
                .build()

            val response = withContext(Dispatchers.IO) {
                httpClient.newCall(request).execute()
            }

            if (response.isSuccessful) {
                val body = response.body?.string()
                val type = object : TypeToken<List<EmpleadoNomina>>() {}.type
                @Suppress("UNCHECKED_CAST")
                gson.fromJson(body, type) as? List<EmpleadoNomina>
            } else null

        } catch (e: Exception) {
            Log.e("ApiClient", "Error getEmpleadosNomina: ${e.message}")
            null
        }
    }

    suspend fun getConceptosNomina(): List<Concepto>? {
        return try {
            val request = Request.Builder()
                .url("$BASE_URL/config/catalogo/cat_conceptos_nomina")
                .get()
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer $authToken")
                .build()

            val response = withContext(Dispatchers.IO) {
                httpClient.newCall(request).execute()
            }

            if (response.isSuccessful) {
                val body = response.body?.string()
                val type = object : TypeToken<List<Concepto>>() {}.type
                @Suppress("UNCHECKED_CAST")
                gson.fromJson(body, type) as? List<Concepto>
            } else null

        } catch (e: Exception) {
            Log.e("ApiClient", "Error getConceptosNomina: ${e.message}")
            null
        }
    }

    suspend fun getNominaSolicitudes(usuarioId: Int, rol: String, tipo: String): List<NominaSolicitud>? {
        return try {
            val request = Request.Builder()
                .url("$BASE_URL/nomina/solicitudes?usuario_id=$usuarioId&rol=$rol&tipo=$tipo")
                .get()
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer $authToken")
                .build()

            val response = withContext(Dispatchers.IO) {
                httpClient.newCall(request).execute()
            }

            if (response.isSuccessful) {
                val body = response.body?.string()
                val type = object : TypeToken<List<NominaSolicitud>>() {}.type
                @Suppress("UNCHECKED_CAST")
                gson.fromJson(body, type) as? List<NominaSolicitud>
            } else null

        } catch (e: Exception) {
            Log.e("ApiClient", "Error getNominaSolicitudes: ${e.message}")
            null
        }
    }

    suspend fun responderNominaSolicitud(id: Int, estatus: String, comentarios: String?): Map<String, Any>? {
        return try {
            val json = gson.toJson(mapOf(
                "estatus" to estatus,
                "comentarios" to (comentarios ?: "")
            ))
            val requestBody = json.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$BASE_URL/nomina/solicitudes/$id")
                .put(requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer $authToken")
                .build()

            val response = withContext(Dispatchers.IO) {
                httpClient.newCall(request).execute()
            }

            if (response.isSuccessful) {
                val body = response.body?.string()
                gson.fromJson(body, object : TypeToken<Map<String, Any>>() {}.type)
            } else null

        } catch (e: Exception) {
            Log.e("ApiClient", "Error responderNominaSolicitud: ${e.message}")
            null
        }
    }

    suspend fun crearNominaSolicitud(
        empleado_id: Int,
        solicitante_id: Int,
        periodo_id: Int,
        concepto_id: Int,
        monto: Double,
        comentarios: String?,
        tipo: String
    ): Map<String, Any>? {
        return try {
            val json = gson.toJson(mapOf(
                "empleado_id" to empleado_id,
                "solicitante_id" to solicitante_id,
                "periodo_id" to periodo_id,
                "concepto_id" to concepto_id,
                "monto" to monto,
                "comentarios" to (comentarios ?: ""),
                "tipo" to tipo
            ))
            val requestBody = json.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$BASE_URL/nomina/solicitudes")
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer $authToken")
                .build()

            val response = withContext(Dispatchers.IO) {
                httpClient.newCall(request).execute()
            }

            if (response.isSuccessful) {
                val body = response.body?.string()
                gson.fromJson(body, object : TypeToken<Map<String, Any>>() {}.type)
            } else null

        } catch (e: Exception) {
            Log.e("ApiClient", "Error crearNominaSolicitud: ${e.message}")
            null
        }
    }
}