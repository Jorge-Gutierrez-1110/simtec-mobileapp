package com.example.simtec_mobileapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.biometric.BiometricPrompt
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.gson.Gson
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var btnRegister: Button
    private lateinit var listHistory: ListView
    private lateinit var tvWelcome: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var tvDateTime: TextView
    private lateinit var tvLastRecord: TextView
    private lateinit var navHeaderName: TextView
    private lateinit var navHeaderRol: TextView

    private lateinit var sessionManager: SessionManager
    private lateinit var locationManager: LocationManager

    private lateinit var adapter: ArrayAdapter<String>
    private val history = mutableListOf<String>()

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val handler = Handler(Looper.getMainLooper())
    private val dateTimeRunnable = object : Runnable {
        override fun run() {
            updateDateTime()
            handler.postDelayed(this, 1000)
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
        private const val PERMISSION_LOCATION_CODE = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        sessionManager = SessionManager(this)
        locationManager = LocationManager(this)

        setupToolbar()
        setupNavigationDrawer()
        setupBackPressHandler()
        setupViews()
        loadUserData()
        loadHistory()
        updateButtonText()
        updateLastRecordText()
        applyRolePermissions()
        requestRuntimePermissions()
    }

    override fun onResume() {
        super.onResume()
        handler.post(dateTimeRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(dateTimeRunnable)
    }

    private fun updateDateTime() {
        val sdf = SimpleDateFormat("EEEE, dd MMM yyyy - HH:mm:ss", Locale("es", "ES"))
        tvDateTime?.text = sdf.format(Date())
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

    private fun setupToolbar() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        supportActionBar?.title = "Simtec"
    }

    private fun setupNavigationDrawer() {
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)

        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            android.R.string.ok,
            android.R.string.cancel
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
                R.id.nav_nomina -> {
                    showModuleComingSoon("Nómina")
                    true
                }
                R.id.nav_reports -> {
                    showModuleComingSoon("Centro de Reportes")
                    true
                }
                R.id.nav_monitor -> {
                    showModuleComingSoon("Monitor en Vivo")
                    true
                }
                R.id.nav_expenses -> {
                    showModuleComingSoon("Control de Gastos")
                    true
                }
                R.id.nav_chat -> {
                    showModuleComingSoon("Mensajes")
                    true
                }
                R.id.nav_profile -> {
                    showModuleComingSoon("Mi Perfil")
                    true
                }
                R.id.nav_settings -> {
                    showModuleComingSoon("Configuración")
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
            R.id.nav_reports,
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

        if (isAdmin) {
            tvSubtitle.text = "Rol: $rol (Admin)"
        }
    }

    private fun showModuleComingSoon(moduleName: String) {
        drawerLayout.closeDrawer(GravityCompat.START)
        Toast.makeText(this, "$moduleName - Próximamente", Toast.LENGTH_SHORT).show()
    }

    private fun setupViews() {
        tvWelcome = findViewById(R.id.tvWelcome)
        tvSubtitle = findViewById(R.id.tvSubtitle)
        tvDateTime = findViewById(R.id.tvDateTime)
        tvLastRecord = findViewById(R.id.tvLastRecord)
        btnRegister = findViewById(R.id.btnRegister)
        listHistory = findViewById(R.id.listHistory)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, history)
        listHistory.adapter = adapter

        btnRegister.setOnClickListener {
            authenticateUser()
        }
    }

    private fun loadUserData() {
        val user = sessionManager.getUserName()
        val rol = sessionManager.getRol()

        tvWelcome.text = "Hola $user"
        tvSubtitle.text = "Rol: $rol"
        navHeaderName.text = user
        navHeaderRol.text = rol
    }

    private fun logout() {
        sessionManager.logout()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
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

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                for (i in permissions.indices) {
                    when (permissions[i]) {
                        Manifest.permission.CAMERA -> {
                            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                                Toast.makeText(this, "Permiso de cámara otorgado", Toast.LENGTH_SHORT).show()
                            }
                        }
                        Manifest.permission.ACCESS_FINE_LOCATION -> {
                            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                                Toast.makeText(this, "Permiso de ubicación otorgado", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun authenticateUser() {
        val executor = ContextCompat.getMainExecutor(this)

        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(this@HomeActivity, "Huella verificada ✓", Toast.LENGTH_SHORT).show()
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
        val intent = Intent(this, CameraActivity::class.java)
        startActivityForResult(intent, 100)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100 && resultCode == RESULT_OK) {
            Toast.makeText(this, "Cara verificada ✓", Toast.LENGTH_SHORT).show()
            checkLocationPermission()
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                PERMISSION_LOCATION_CODE
            )
        } else {
            obtainLocationAndSaveRecord()
        }
    }

    private fun obtainLocationAndSaveRecord() {
        Toast.makeText(this, "Obteniendo ubicación...", Toast.LENGTH_SHORT).show()

        scope.launch {
            try {
                val location = locationManager.getReadableLocation()
                saveRegister(location)
            } catch (e: Exception) {
                e.printStackTrace()
                saveRegister("Error obteniendo ubicación")
            }
        }
    }

    private fun saveRegister(location: String) {
        val sdf = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
        val time = sdf.format(Date())

        val isEntry = sessionManager.shouldBeEntry()
        val type = if (isEntry) "Entrada" else "Salida"

        sessionManager.saveAttendanceRecord(type = type, timestamp = time, location = location, faceConfidence = 0.9f)
        sessionManager.saveLastRecordType(type)

        val record = "$type - $time\n📍 $location"
        history.add(0, record)
        adapter.notifyDataSetChanged()
        saveHistory()
        updateButtonText()
        updateLastRecordText()

        Toast.makeText(this, "Asistencia registrada ✓", Toast.LENGTH_SHORT).show()
    }

    private fun updateButtonText() {
        val isEntry = sessionManager.shouldBeEntry()
        if (isEntry) {
            btnRegister.text = "Entrada"
            btnRegister.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.green_entry))
        } else {
            btnRegister.text = "Salida"
            btnRegister.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.red_exit))
        }
    }

    private fun updateLastRecordText() {
        val records = sessionManager.getAttendanceRecords()
        if (records.isNotEmpty()) {
            val lastRecord = records.first()
            val recordDate = lastRecord.timestamp.split(" ")[0]
            val today = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date())
            
            if (recordDate == today) {
                tvLastRecord.text = "Último registro: ${lastRecord.type} - ${lastRecord.timestamp}"
            } else {
                tvLastRecord.text = "Sin registros hoy"
            }
        } else {
            tvLastRecord.text = "Sin registros"
        }
    }

    private fun saveHistory() {
        val prefs = getSharedPreferences("attendance", MODE_PRIVATE)
        val gson = Gson()
        val json = gson.toJson(history)
        prefs.edit().putString("history_json", json).apply()
    }

    private fun loadHistory() {
        val prefs = getSharedPreferences("attendance", MODE_PRIVATE)
        val gson = Gson()
        val savedJson = prefs.getString("history_json", null)

        if (savedJson != null) {
            try {
                val loaded = gson.fromJson(savedJson, Array<String>::class.java).toList()
                history.addAll(loaded)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        adapter.notifyDataSetChanged()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        handler.removeCallbacks(dateTimeRunnable)
    }
}
