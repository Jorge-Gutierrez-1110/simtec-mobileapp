package com.example.simtec_mobileapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.example.simtec_mobileapp.ApiClient
import com.example.simtec_mobileapp.chat.ChatActivity
import com.example.simtec_mobileapp.chat.ChatNotificationHelper
import com.example.simtec_mobileapp.chat.data.ChatRetrofitClient
import com.example.simtec_mobileapp.chat.data.ChatSocketManager
import com.example.simtec_mobileapp.showLoading
import com.example.simtec_mobileapp.hideLoading
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow

class HomeActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var tvUserName: TextView
    private lateinit var tvUserRole: TextView
    private lateinit var tvWelcome: TextView
    private lateinit var tvLastRecord: TextView
    private lateinit var tvBtnRegister: TextView
    private lateinit var tvExitHint: TextView
    private lateinit var tvSuccessTitle: TextView
    private lateinit var tvSuccessTime: TextView
    private lateinit var tvEntryStatus: TextView
    private lateinit var tvExitStatus: TextView
    private lateinit var btnRegisterContainer: View
    private lateinit var btnMenu: ImageButton
    private lateinit var cardSuccessBanner: View
    private lateinit var layoutLastRecord: LinearLayout
    private lateinit var rvHistory: RecyclerView
    private lateinit var navHeaderName: TextView
    private lateinit var navHeaderRol: TextView

    private lateinit var sessionManager: SessionManager
    private lateinit var locationManagerCustom: LocationManager
    private lateinit var historyAdapter: HistoryAdapter
    private val historyItems = mutableListOf<HistoryAdapter.HistoryItem>()

    private lateinit var androidLocationManager: android.location.LocationManager

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
        private const val PERMISSION_LOCATION_CODE = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContentView(R.layout.activity_home)

        sessionManager = SessionManager(this)
        locationManagerCustom = LocationManager(this)
        androidLocationManager = getSystemService(LOCATION_SERVICE) as android.location.LocationManager

        setupDrawer()
        setupBackPressHandler()
        initViews()
        setupListeners()
        loadUserData()
        verificarEmpresaActual()
        loadHistory()
        updateUI()
        applyRolePermissions()
        requestRuntimePermissions()
        inicializarChatSocket()
    }

    private fun setupDrawer() {
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)

        ViewCompat.setOnApplyWindowInsetsListener(drawerLayout) { v, insets ->
            v.setPadding(0, 0, 0, 0)
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(navigationView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            R.string.app_name,
            R.string.app_name
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        val headerView = navigationView.getHeaderView(0)
        navHeaderName = headerView.findViewById(R.id.navHeaderName)
        navHeaderRol = headerView.findViewById(R.id.navHeaderRol)

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_register -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    authenticateUser()
                    true
                }
                R.id.nav_chat -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    startActivity(Intent(this, ChatActivity::class.java))
                    true
                }
                R.id.nav_mis_recibos -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    startActivity(Intent(this, MisRecibosActivity::class.java))
                    true
                }
                R.id.nav_nomina -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    startActivity(Intent(this, NominaProcesoActivity::class.java))
                    true
                }
                R.id.nav_solicitudes -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    startActivity(Intent(this, SolicitudesActivity::class.java))
                    true
                }
                R.id.nav_monitor -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    Toast.makeText(this, "Monitor en Vivo - Próximamente", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_expenses -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    startActivity(Intent(this, GastosActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    startActivity(Intent(this, PerfilActivity::class.java))
                    true
                }
                R.id.nav_logout -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    logout()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun applyRolePermissions() {
        val rol = sessionManager.getRol().lowercase()
        val isAdmin = rol == "admin" || rol == "administrador" || rol == "supervisor"

        val menu = navigationView.menu

        val adminHeader = menu.findItem(R.id.navAdminHeader)
        if (adminHeader != null) {
            adminHeader.isVisible = isAdmin
        }

        val itemsToShowOnlyAdmin = listOf(
            R.id.nav_nomina,
            R.id.nav_solicitudes,
            R.id.nav_monitor,
            R.id.nav_expenses
        )

        for (itemId in itemsToShowOnlyAdmin) {
            val item = menu.findItem(itemId)
            if (item != null) {
                item.isVisible = isAdmin
                item.isEnabled = isAdmin
            }
        }
    }

    // --- Chat: Conectar socket al iniciar la app ---
    private fun inicializarChatSocket() {
        Log.d("ChatSocket", "=== inicializarChatSocket() desde HomeActivity ===")

        // Inicializar cliente HTTP y notificaciones
        ChatRetrofitClient.init(this)
        ChatNotificationHelper.init(this)

        Log.d("ChatSocket", "Token disponible: ${ChatRetrofitClient.getToken() != null}")
        Log.d("ChatSocket", "UserId: ${ChatRetrofitClient.getUserId()}, RolId: ${ChatRetrofitClient.getRolId()}")

        // Conectar Socket.IO para recibir mensajes en tiempo real
        // (aunque el usuario no este en ChatActivity)
        if (!ChatSocketManager.isConnected) {
            ChatSocketManager.connect("https://api.simtec-test.com")

            // Feedback visual de conexion (solo para debug)
            ChatSocketManager.onConectado {
                runOnUiThread {
                    Log.d("ChatSocket", "✅ Socket conectado desde HomeActivity")
                    Toast.makeText(this, "Chat conectado ✅", Toast.LENGTH_SHORT).show()
                }
            }
            ChatSocketManager.onDesconectado {
                Log.w("ChatSocket", "❌ Socket desconectado desde HomeActivity")
            }
        } else {
            Log.d("ChatSocket", "Socket ya estaba conectado")
        }

    }

    private fun logout() {
        // Desconectar socket del chat al cerrar sesion
        ChatSocketManager.disconnect()

        val prefs = getSharedPreferences("attendance", MODE_PRIVATE)
        prefs.edit().clear().apply()

        sessionManager.logout()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun initViews() {
        tvUserName = findViewById(R.id.tvUserName)
        tvUserRole = findViewById(R.id.tvUserRole)
        tvWelcome = findViewById(R.id.tvWelcome)
        tvLastRecord = findViewById(R.id.tvLastRecord)
        tvBtnRegister = findViewById(R.id.tvBtnRegister)
        tvExitHint = findViewById(R.id.tvExitHint)
        tvSuccessTitle = findViewById(R.id.tvSuccessTitle)
        tvSuccessTime = findViewById(R.id.tvSuccessTime)
        tvEntryStatus = findViewById(R.id.tvEntryStatus)
        tvExitStatus = findViewById(R.id.tvExitStatus)
        btnRegisterContainer = findViewById(R.id.btnRegisterContainer)
        btnMenu = findViewById(R.id.btnMenu)
        cardSuccessBanner = findViewById(R.id.cardSuccessBanner)
        layoutLastRecord = findViewById(R.id.layoutLastRecord)
        rvHistory = findViewById(R.id.rvHistory)

        rvHistory.layoutManager = LinearLayoutManager(this)
        historyAdapter = HistoryAdapter(historyItems)
        rvHistory.adapter = historyAdapter
    }

    private fun setupListeners() {
        btnRegisterContainer.setOnClickListener {
            authenticateUser()
        }

        btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    private fun loadUserData() {
        val user = sessionManager.getUserName()
        val rol = sessionManager.getRol()

        tvUserName.text = user
        tvUserRole.text = rol
        navHeaderName.text = user
        navHeaderRol.text = rol
        tvWelcome.text = "Hola $user, registra tu asistencia con un toque"
    }

    private fun verificarEmpresaActual() {
        val savedClienteId = sessionManager.getClienteId()
        Log.d("HomeActivity", "📊 Cliente guardado: $savedClienteId")

        if (savedClienteId == 0) {
            Log.d("HomeActivity", "⚠️ Sin empresa guardada previamente")
            return
        }

        scope.launch {
            try {
                val apiClient = ApiClient()
                apiClient.setAuthToken(sessionManager.getToken())

                val empleado = apiClient.getEmpleadoCompleto(sessionManager.getEmpleadoId())
                val nuevoClienteId = empleado?.cliente_id ?: 0

                Log.d("HomeActivity", "📊 Cliente actual del empleado: $nuevoClienteId vs guardado: $savedClienteId")

                val hayCambioCliente = nuevoClienteId != savedClienteId && nuevoClienteId > 0

                if (hayCambioCliente) {
                    Log.d("HomeActivity", "🔄 Cliente diferente - actualizando geocerca")
                }

                val empresa = apiClient.getCatalogoById("clientes", if (nuevoClienteId > 0) nuevoClienteId else savedClienteId)
                if (empresa != null) {
                    val geocercaActiva = when (val v = empresa["geocerca_activa"]) {
                        is Number -> v.toInt()
                        is String -> v.toIntOrNull() ?: 0
                        else -> 0
                    }
                    val geocercaLat = when (val v = empresa["geocerca_latitud"]) {
                        is Number -> v.toDouble()
                        is String -> v.toDoubleOrNull()
                        else -> null
                    }
                    val geocercaLng = when (val v = empresa["geocerca_longitud"]) {
                        is Number -> v.toDouble()
                        is String -> v.toDoubleOrNull()
                        else -> null
                    }
                    val geocercaRadio = when (val v = empresa["geocerca_radio_metros"]) {
                        is Number -> v.toInt()
                        is String -> v.toIntOrNull() ?: 1000
                        else -> 1000
                    }

                    val hayCambioGeocerca = hayCambioGeocerca(
                        geocercaActiva,
                        geocercaLat,
                        geocercaLng,
                        geocercaRadio
                    )

                    if (hayCambioCliente || hayCambioGeocerca) {
                        Log.d("HomeActivity", "🔄 Actualizando geocerca - cambioCliente=$hayCambioCliente, cambioGeocerca=$hayCambioGeocerca")

                        if (nuevoClienteId > 0) {
                            sessionManager.saveClienteId(nuevoClienteId)
                        }

                        if (geocercaActiva == 1 && geocercaLat != null && geocercaLng != null) {
                            sessionManager.saveGeocerca(geocercaLat, geocercaLng, geocercaRadio, true)
                            Log.d("HomeActivity", "✅ Geocerca actualizada: lat=$geocercaLat, lng=$geocercaLng, radio=$geocercaRadio")
                        } else {
                            sessionManager.saveGeocerca(0.0, 0.0, 1000, false)
                            Log.d("HomeActivity", "⚠️ Empresa sin geocerca configurada")
                        }
                    } else {
                        Log.d("HomeActivity", "✅ Geocerca sin cambios")
                    }
                } else {
                    Log.w("HomeActivity", "⚠️ No se encontró empresa con ID $savedClienteId")
                }
            } catch (e: Exception) {
                Log.e("HomeActivity", "Error verificando empresa: ${e.message}")
            }
        }
    }

    private fun hayCambioGeocerca(
        geocercaActiva: Int,
        geocercaLat: Double?,
        geocercaLng: Double?,
        geocercaRadio: Int
    ): Boolean {
        val activaActual = sessionManager.isGeocercaActiva()
        val latActual = sessionManager.getGeocercaLatitud()
        val lngActual = sessionManager.getGeocercaLongitud()
        val radioActual = sessionManager.getGeocercaRadioMetros()

        val cambioActiva = geocercaActiva == 1 != activaActual
        val cambioLat = geocercaLat != null && geocercaLat != latActual
        val cambioLng = geocercaLng != null && geocercaLng != lngActual
        val cambioRadio = geocercaRadio != radioActual

        return cambioActiva || cambioLat || cambioLng || cambioRadio
    }

    private fun updateUI() {
        val isEntry = sessionManager.shouldBeEntry()
        val lastRecordType = sessionManager.getLastRecordType()
        val records = sessionManager.getAttendanceRecords()

        if (isEntry) {
            btnRegisterContainer.setBackgroundResource(R.drawable.gradient_green_3d)
            tvBtnRegister.text = "Registrar Entrada"
            tvExitHint.visibility = View.GONE
        } else {
            btnRegisterContainer.setBackgroundResource(R.drawable.gradient_red_3d)
            tvBtnRegister.text = "Registrar Salida"
            tvExitHint.visibility = View.VISIBLE
        }

        if (records.isNotEmpty()) {
            val last = records.first()
            tvLastRecord.text = "Último registro: ${last.type} - ${last.timestamp}"
            layoutLastRecord.visibility = View.VISIBLE

            tvEntryStatus.text = if (lastRecordType == "Entrada") last.timestamp else "8:00 AM"
            tvExitStatus.text = if (lastRecordType == "Salida") last.timestamp else "6:00 PM"
        } else {
            tvLastRecord.text = "Último registro: 07 abr 2026, 8:00 AM"
            tvEntryStatus.text = "8:00 AM"
            tvExitStatus.text = "6:00 PM"
        }
    }

    private fun loadHistory() {
        showLoading("Cargando historial...")
        val empleadoId = sessionManager.getEmpleadoId()
        val token = sessionManager.getToken()

        scope.launch {
            try {
                if (empleadoId > 0) {
                    ApiClient().setAuthToken(token)
                    val records = ApiClient().getAttendanceHistory(empleadoId)

                    historyItems.clear()

                    if (records != null && records.isNotEmpty()) {
                        for (record in records.take(3)) {
                            val entrada = record.hora_entrada ?: "8:00 AM"
                            val salida = record.hora_salida
                            val isEntry = salida == null

                            val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val sdfDisplay = SimpleDateFormat("dd MMM yyyy", Locale("es", "ES"))
                            val date = try {
                                sdfDisplay.format(sdfDate.parse(record.fecha)!!)
                            } catch (e: Exception) {
                                record.fecha
                            }

                            val type = if (isEntry) "Entrada" else "Salida"
                            val time = if (isEntry) entrada else salida ?: "6:00 PM"

                            historyItems.add(
                                HistoryAdapter.HistoryItem(
                                    date = date,
                                    type = type,
                                    time = time,
                                    isEntry = isEntry
                                )
                            )

                            if (!isEntry) {
                                historyItems.add(
                                    HistoryAdapter.HistoryItem(
                                        date = date,
                                        type = "Entrada",
                                        time = entrada,
                                        isEntry = true
                                    )
                                )
                            }
                        }
                    } else {
                        loadSimulatedHistory()
                    }
                } else {
                    loadSimulatedHistory()
                }

                historyAdapter.updateRecords(historyItems)

            } catch (e: Exception) {
                Log.e("HomeActivity", "Error cargando historial: ${e.message}")
                loadSimulatedHistory()
                historyAdapter.updateRecords(historyItems)
            } finally {
                hideLoading()
            }
        }
    }

    private fun loadSimulatedHistory() {
        historyItems.clear()
        
        historyItems.add(HistoryAdapter.HistoryItem("07 abr 2026", "Salida", "6:15 PM", false))
        historyItems.add(HistoryAdapter.HistoryItem("07 abr 2026", "Entrada", "8:00 AM", true))
        historyItems.add(HistoryAdapter.HistoryItem("05 abr 2026", "Salida", "6:30 PM", false))
        historyItems.add(HistoryAdapter.HistoryItem("05 abr 2026", "Entrada", "8:15 AM", true))
        historyItems.add(HistoryAdapter.HistoryItem("04 abr 2026", "Salida", "6:00 PM", false))
        historyItems.add(HistoryAdapter.HistoryItem("04 abr 2026", "Entrada", "8:00 AM", true))
    }

    private fun showSuccessBanner(type: String, time: String) {
        cardSuccessBanner.visibility = View.VISIBLE
        tvSuccessTitle.text = "$type registrada."
        tvSuccessTime.text = time

        handler.postDelayed({
            cardSuccessBanner.animate()
                .alpha(0f)
                .setDuration(500)
                .withEndAction {
                    cardSuccessBanner.visibility = View.GONE
                    cardSuccessBanner.alpha = 1f
                }
        }, 5000)
    }

    private fun requestRuntimePermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // Permiso de notificaciones (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    private fun authenticateUser() {
        // Primero verificar ubicación
        checkLocationAndProceed()
    }

    private fun checkLocationAndProceed() {
        if (!sessionManager.isGeocercaActiva()) {
            Log.d("HomeActivity", "Geocerca inactiva - registrando directa")
            openBiometric()
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                PERMISSION_LOCATION_CODE
            )
            return
        }

        scope.launch {
            try {
                val location = androidLocationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)

                if (location == null) {
                    Toast.makeText(this@HomeActivity, "Obteniendo ubicación...", Toast.LENGTH_SHORT).show()
                    checkAndOpenMap(null)
                    return@launch
                }

                val distance = calculateDistance(
                    location.latitude, location.longitude,
                    sessionManager.getGeocercaLatitud(), sessionManager.getGeocercaLongitud()
                )

                val maxDistance = sessionManager.getGeocercaRadioMetros().toDouble()

                if (distance <= maxDistance) {
                    runOnUiThread {
                        Toast.makeText(this@HomeActivity, "Ubicación verificada ✅", Toast.LENGTH_SHORT).show()
                        openBiometric()
                    }
                } else {
                    runOnUiThread {
                        checkAndOpenMap(location)
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeActivity", "Error verificando ubicación: ${e.message}")
                runOnUiThread {
                    openBiometric()
                }
            }
        }
    }

    private fun checkAndOpenMap(location: android.location.Location?) {
        val distance = if (location != null) {
            calculateDistance(
                location.latitude, location.longitude,
                sessionManager.getGeocercaLatitud(), sessionManager.getGeocercaLongitud()
            )
        } else {
            sessionManager.getGeocercaRadioMetros().toDouble() + 1.0
        }

        val intent = Intent(this, MapActivity::class.java)
        startActivityForResult(intent, 101)
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2).pow(2) + kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) * kotlin.math.sin(dLon / 2).pow(2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(1 - a), kotlin.math.sqrt(a))
        return r * c
    }

    private fun openBiometric() {
        val executor = ContextCompat.getMainExecutor(this)

        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(this@HomeActivity, "Huella verificada", Toast.LENGTH_SHORT).show()
                    openFaceDetection()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(this@HomeActivity, "Huella no reconocida", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(this@HomeActivity, "Error: $errString", Toast.LENGTH_SHORT).show()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Verificación Biométrica")
            .setSubtitle("Confirma tu huella dactilar")
            .setNegativeButtonText("Cancelar")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun openFaceDetection() {
        Toast.makeText(this, "Obteniendo ubicación...", Toast.LENGTH_SHORT).show()

        scope.launch {
            try {
                val location = locationManagerCustom.getReadableLocation()
                val intent = Intent(this@HomeActivity, CameraActivity::class.java)
                intent.putExtra(CameraActivity.EXTRA_LOCATION, location)
                startActivityForResult(intent, 100)
            } catch (e: Exception) {
                e.printStackTrace()
                val intent = Intent(this@HomeActivity, CameraActivity::class.java)
                intent.putExtra(CameraActivity.EXTRA_LOCATION, "Ubicación no disponible")
                startActivityForResult(intent, 100)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 101) {
            // Resultado del MapActivity (geofence)
            val withinRange = data?.getBooleanExtra(MapActivity.RESULT_WITHIN_RANGE, false) ?: false
            if (withinRange) {
                Toast.makeText(this, "Ubicación verificada ✅", Toast.LENGTH_SHORT).show()
                openBiometric()
            }
            return
        }

        if (requestCode == 100 && resultCode == RESULT_OK) {
            val location = data?.getStringExtra(CameraActivity.RESULT_LOCATION) ?: ""
            val photoBase64 = data?.getStringExtra(CameraActivity.RESULT_PHOTO)
            Toast.makeText(this, "Cara verificada", Toast.LENGTH_SHORT).show()

            if (location.isNotEmpty() && location != "Ubicación no disponible") {
                saveRegister(location, photoBase64)
            } else {
                checkLocationPermissionAndSave(photoBase64)
            }
        }
    }

    private fun checkLocationPermissionAndSave(photoBase64: String?) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                PERMISSION_LOCATION_CODE
            )
        } else {
            obtainLocationAndSaveRecord(photoBase64)
        }
    }

    private fun obtainLocationAndSaveRecord(photoBase64: String?) {
        Toast.makeText(this, "Obteniendo ubicación...", Toast.LENGTH_SHORT).show()

        scope.launch {
            try {
                val location = locationManagerCustom.getReadableLocation()
                saveRegister(location, photoBase64)
            } catch (e: Exception) {
                e.printStackTrace()
                saveRegister("Error obteniendo ubicación", photoBase64)
            }
        }
    }

    private fun saveRegister(location: String, photoBase64: String? = null) {
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
        val sdfDisplay = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("es", "ES"))

        val now = Date()
        val fecha = sdfDate.format(now)
        val hora = sdfTime.format(now)
        val timeDisplay = sdfDisplay.format(now)

        val isEntry = sessionManager.shouldBeEntry()
        val type = if (isEntry) "Entrada" else "Salida"

        Toast.makeText(this, "Guardando en servidor...", Toast.LENGTH_SHORT).show()

        scope.launch {
            try {
                val empleadoId = sessionManager.getEmpleadoId()
                val token = sessionManager.getToken()

                ApiClient().setAuthToken(token)

                val horaEntrada = if (isEntry) hora else sessionManager.getLastHoraEntrada()
                val horaSalida = if (!isEntry) hora else null

                val response = ApiClient().saveAttendance(
                    empleadoId = empleadoId,
                    fecha = fecha,
                    horaEntrada = horaEntrada,
                    horaSalida = horaSalida,
                    tipo = type,
                    ubicacion = location,
                    foto = photoBase64
                )

                if (response?.success == true) {
                    sessionManager.saveAttendanceRecord(
                        type = type,
                        timestamp = timeDisplay,
                        location = "☁️ $location",
                        faceConfidence = 0.9f
                    )
                    sessionManager.saveLastRecordType(type)
                    if (isEntry) {
                        sessionManager.saveLastHoraEntrada(hora)
                    }

                    runOnUiThread {
                        showSuccessBanner(type, timeDisplay)
                        updateUI()
                        loadHistory()
                        Toast.makeText(this@HomeActivity, "Asistencia registrada", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    saveLocal(location, type, timeDisplay, photoBase64)
                }

            } catch (e: Exception) {
                saveLocal(location, type, timeDisplay, photoBase64)
            }
        }
    }

    private fun saveLocal(location: String, type: String, timeDisplay: String, photoBase64: String? = null) {
        sessionManager.saveAttendanceRecord(
            type = type,
            timestamp = "$timeDisplay (local)",
            location = "⚠️ $location",
            faceConfidence = 0.9f
        )
        sessionManager.saveLastRecordType(type)
        if (type == "Entrada") {
            val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
            sessionManager.saveLastHoraEntrada(sdfTime.format(Date()))
        }

        runOnUiThread {
            showSuccessBanner(type, timeDisplay)
            updateUI()
            loadHistory()
            Toast.makeText(this, "Guardado local (sin conexión)", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        verificarEmpresaActual()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
