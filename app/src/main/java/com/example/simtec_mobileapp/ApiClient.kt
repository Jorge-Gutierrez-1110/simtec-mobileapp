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

    // ============ HELPER: ejecuta request en IO y retorna (code, isSuccessful, body) ============

    private data class HttpResult(val code: Int, val isSuccessful: Boolean, val body: String?)

    private suspend fun executeRequest(request: Request): HttpResult = withContext(Dispatchers.IO) {
        val response = httpClient.newCall(request).execute()
        val body = response.body?.string()
        HttpResult(response.code, response.isSuccessful, body)
    }

    // ============ LOGIN ============

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
            Log.d("ApiClient", "═══════════════════════════════════")

            val result = executeRequest(request)

            Log.d("ApiClient", "═══════════════════════════════════")
            Log.d("ApiClient", "RESPUESTA RECIBIDA")
            Log.d("ApiClient", "Status Code: ${result.code}")
            Log.d("ApiClient", "Is Successful: ${result.isSuccessful}")
            Log.d("ApiClient", "Response Body: ${result.body}")
            Log.d("ApiClient", "═══════════════════════════════════")

            if (result.body != null) {
                try {
                    val loginResponse = gson.fromJson(result.body, LoginResponse::class.java)

                    Log.d("ApiClient", "PARSEADO EXITOSAMENTE:")
                    Log.d("ApiClient", "  success: ${loginResponse.success}")
                    Log.d("ApiClient", "  token: ${if (loginResponse.token != null) "Presente" else "Null"}")
                    Log.d("ApiClient", "  user: ${if (loginResponse.user != null) "${loginResponse.user.email}" else "Null"}")
                    Log.d("ApiClient", "  message: ${loginResponse.message}")

                    loginResponse
                } catch (e: Exception) {
                    Log.e("ApiClient", "ERROR AL PARSEAR JSON: ${e.message}")
                    e.printStackTrace()
                    null
                }
            } else {
                Log.e("ApiClient", "Response body es null")
                null
            }

        } catch (e: Exception) {
            Log.e("ApiClient", "EXCEPCION EN LOGIN: ${e.message}", e)
            e.printStackTrace()
            null
        }
    }

    // ============ ASISTENCIA ============

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

            Log.d("ApiClient", "GUARDANDO ASISTENCIA")
            Log.d("ApiClient", "  URL: ${request.url}")
            Log.d("ApiClient", "  Empleado: $empleadoId, Tipo: $tipo, Fecha: $fecha")

            val result = executeRequest(request)

            Log.d("ApiClient", "  Status: ${result.code}")
            Log.d("ApiClient", "  Response: ${result.body}")

            if (result.body != null) {
                try {
                    val attendanceResponse = gson.fromJson(result.body, AttendanceResponse::class.java)
                    Log.d("ApiClient", "  Success: ${attendanceResponse.success}")
                    attendanceResponse
                } catch (e: Exception) {
                    Log.e("ApiClient", "ERROR PARSE: ${e.message}")
                    null
                }
            } else {
                null
            }

        } catch (e: Exception) {
            Log.e("ApiClient", "EXCEPCION: ${e.message}", e)
            null
        }
    }

    // ============ DATA CLASSES ============

    data class LoginResponse(
        val success: Boolean = false,
        val token: String? = null,
        val user: UserData? = null,
        val message: String? = null
    )

    data class UserData(
        val id: Int = 0,
        val nombre: String = "",
        val email: String = "",
        val rol_id: Int = 0,
        val rol: String = "",
        val empleado_id: Int = 0,
        val permisos: List<String> = emptyList()
    )

    data class AttendanceResponse(
        val success: Boolean = false,
        val message: String? = null,
        val calculo: AttendanceCalculo? = null
    )

    data class AttendanceCalculo(
        val estatus: String = "",
        val extras: Int = 0
    )

    // ============ HISTORIAL ASISTENCIA ============

    suspend fun getAttendanceHistory(empleadoId: Int): List<AttendanceRecord>? {
        return try {
            val request = Request.Builder()
                .url("$BASE_URL/asistencia/historial/$empleadoId")
                .get()
                .addHeader("Content-Type", "application/json")
                .build()

            Log.d("ApiClient", "OBTENIENDO HISTORIAL")
            Log.d("ApiClient", "  URL: ${request.url}")

            val result = executeRequest(request)

            Log.d("ApiClient", "  Status: ${result.code}")
            Log.d("ApiClient", "  Response: ${result.body}")

            if (result.isSuccessful && result.body != null) {
                val type = object : TypeToken<List<AttendanceRecord>>() {}.type
                val records: List<AttendanceRecord> = gson.fromJson(result.body, type)
                Log.d("ApiClient", "  Registros obtenidos: ${records.size}")
                records
            } else {
                null
            }

        } catch (e: Exception) {
            Log.e("ApiClient", "EXCEPCION: ${e.message}", e)
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

    // ============ NOMINA API ============

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

            Log.d("ApiClient", "PRE-CALCULO NOMINA")
            Log.d("ApiClient", "  URL: ${request.url}")
            Log.d("ApiClient", "  Periodo ID: $periodoId")

            val result = executeRequest(request)

            Log.d("ApiClient", "  Status: ${result.code}")
            Log.d("ApiClient", "  Response: ${result.body}")

            if (result.body != null) {
                gson.fromJson(result.body, NominaResponse::class.java)
            } else {
                null
            }

        } catch (e: Exception) {
            Log.e("ApiClient", "EXCEPCION: ${e.message}", e)
            null
        }
    }

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

            Log.d("ApiClient", "CERRAR NOMINA")
            Log.d("ApiClient", "  URL: ${request.url}")

            val result = executeRequest(request)

            Log.d("ApiClient", "  Status: ${result.code}")
            Log.d("ApiClient", "  Response: ${result.body}")

            if (result.body != null) {
                gson.fromJson(result.body, NominaResponse::class.java)
            } else {
                null
            }

        } catch (e: Exception) {
            Log.e("ApiClient", "EXCEPCION: ${e.message}", e)
            null
        }
    }

    // ============ CATALOGOS ============

    suspend fun getCatalogoById(tabla: String, id: Int): Map<String, Any>? {
        return try {
            val request = Request.Builder()
                .url("$BASE_URL/config/catalogo/$tabla/$id")
                .get()
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer $authToken")
                .build()

            Log.d("ApiClient", "OBTENIENDO CATALOGO: $tabla id=$id")

            val result = executeRequest(request)

            Log.d("ApiClient", "  Response: ${result.body}")

            if (result.isSuccessful && result.body != null) {
                gson.fromJson(result.body, Map::class.java) as? Map<String, Any>
            } else {
                null
            }

        } catch (e: Exception) {
            Log.e("ApiClient", "EXCEPCION: ${e.message}", e)
            null
        }
    }

    // ============ EMPLEADO PERFIL ============

    suspend fun getEmpleadoCompleto(empleadoId: Int): EmpleadoPerfil? {
        return try {
            val request = Request.Builder()
                .url("$BASE_URL/empleado-completo/$empleadoId")
                .get()
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer $authToken")
                .build()

            Log.d("ApiClient", "OBTENIENDO PERFIL EMPLEADO")
            Log.d("ApiClient", "  URL: ${request.url}")

            val result = executeRequest(request)

            Log.d("ApiClient", "  Status: ${result.code}")
            Log.d("ApiClient", "  Response: ${result.body}")

            if (result.isSuccessful && result.body != null) {
                val responseObj = gson.fromJson(result.body, EmpleadoPerfilResponse::class.java)
                responseObj.empleado
            } else {
                null
            }

        } catch (e: Exception) {
            Log.e("ApiClient", "EXCEPCION: ${e.message}", e)
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
        val geocerca_activa: Int? = null,
        val geocerca_latitud: Double? = null,
        val geocerca_longitud: Double? = null,
        val geocerca_radio_metros: Int? = null
    )

    // ============ GASTOS ============

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
        val aprobador_id: Int? = null,
        val concepto: String = "",
        val subtotal: Double = 0.0,
        val iva: Double = 0.0,
        val retenciones: Double = 0.0,
        val total: Double = 0.0,
        val fecha_emision: String? = null,
        val fecha_vencimiento: String? = null,
        val factura_url: String? = null,
        val estatus: String = "",
        val motivo_rechazo: String? = null,
        val cliente_id: Int? = null,
        val created_at: String? = null,
        val updated_at: String? = null,
        val proveedor: String? = null,
        val categoria: String? = null,
        val categoria_color: String? = null,
        val solicitante: String? = null,
        val aprobador: String? = null,
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

            val result = executeRequest(request)

            if (result.isSuccessful && result.body != null) {
                gson.fromJson(result.body, GastosCatalogosResponse::class.java)
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

            val result = executeRequest(request)

            if (result.isSuccessful && result.body != null) {
                val type = object : TypeToken<List<GastoRecord>>() {}.type
                gson.fromJson(result.body, type)
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

            val result = executeRequest(request)

            if (result.isSuccessful && result.body != null) {
                gson.fromJson(result.body, object : TypeToken<Map<String, Any>>() {}.type)
            } else null

        } catch (e: Exception) {
            Log.e("ApiClient", "Error solicitar gasto: ${e.message}")
            null
        }
    }

    // ============ MIS RECIBOS (Empleado) ============

    suspend fun getMisRecibos(empleadoId: Int): List<ReciboNomina>? {
        return try {
            val request = Request.Builder()
                .url("$BASE_URL/nomina/mis-recibos/$empleadoId")
                .get()
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer $authToken")
                .build()

            Log.d("ApiClient", "OBTENIENDO MIS RECIBOS empleado=$empleadoId")

            val result = executeRequest(request)

            Log.d("ApiClient", "  Status: ${result.code}")

            if (result.isSuccessful && result.body != null) {
                val type = object : TypeToken<List<ReciboNomina>>() {}.type
                gson.fromJson(result.body, type)
            } else null

        } catch (e: Exception) {
            Log.e("ApiClient", "Error getMisRecibos: ${e.message}")
            null
        }
    }

    // ============ PERIODOS ============

    suspend fun getPeriodos(): List<Periodo>? {
        return try {
            val anio = Calendar.getInstance().get(Calendar.YEAR)
            val request = Request.Builder()
                .url("$BASE_URL/nomina/periodos?anio=$anio")
                .get()
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer $authToken")
                .build()

            val result = executeRequest(request)

            if (result.isSuccessful && result.body != null) {
                val type = object : TypeToken<List<Periodo>>() {}.type
                @Suppress("UNCHECKED_CAST")
                (gson.fromJson(result.body, type) as? List<Periodo>)
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

            val result = executeRequest(httpRequest)

            if (result.isSuccessful && result.body != null) {
                gson.fromJson(result.body, object : TypeToken<Map<String, Any>>() {}.type)
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

            val result = executeRequest(request)

            if (result.isSuccessful && result.body != null) {
                gson.fromJson(result.body, object : TypeToken<Map<String, Any>>() {}.type)
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

            val result = executeRequest(request)

            if (result.isSuccessful && result.body != null) {
                val type = object : TypeToken<List<EmpleadoNomina>>() {}.type
                gson.fromJson(result.body, type)
            } else null

        } catch (e: Exception) {
            Log.e("ApiClient", "Error getNominaDetalle: ${e.message}")
            null
        }
    }

    // ============ SOLICITUDES DE NOMINA ============

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

            val result = executeRequest(request)

            if (result.isSuccessful && result.body != null) {
                val type = object : TypeToken<List<EmpleadoNomina>>() {}.type
                @Suppress("UNCHECKED_CAST")
                gson.fromJson(result.body, type) as? List<EmpleadoNomina>
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

            val result = executeRequest(request)

            if (result.isSuccessful && result.body != null) {
                val type = object : TypeToken<List<Concepto>>() {}.type
                @Suppress("UNCHECKED_CAST")
                gson.fromJson(result.body, type) as? List<Concepto>
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

            val result = executeRequest(request)

            if (result.isSuccessful && result.body != null) {
                val type = object : TypeToken<List<NominaSolicitud>>() {}.type
                @Suppress("UNCHECKED_CAST")
                gson.fromJson(result.body, type) as? List<NominaSolicitud>
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

            val result = executeRequest(request)

            if (result.isSuccessful && result.body != null) {
                gson.fromJson(result.body, object : TypeToken<Map<String, Any>>() {}.type)
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

            val result = executeRequest(request)

            if (result.isSuccessful && result.body != null) {
                gson.fromJson(result.body, object : TypeToken<Map<String, Any>>() {}.type)
            } else null

        } catch (e: Exception) {
            Log.e("ApiClient", "Error crearNominaSolicitud: ${e.message}")
            null
        }
    }
}
