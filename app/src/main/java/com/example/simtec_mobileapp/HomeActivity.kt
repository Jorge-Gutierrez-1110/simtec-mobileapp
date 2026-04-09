package com.example.simtec_mobileapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : AppCompatActivity() {

    private lateinit var btnLogout: Button
    private lateinit var btnRegister: Button
    private lateinit var listHistory: ListView
    private lateinit var tvWelcome: TextView

    private lateinit var sessionManager: SessionManager
    private lateinit var locationManager: LocationManager

    private lateinit var adapter: ArrayAdapter<String>
    private val history = mutableListOf<String>()

    private var isEntry = true
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_home)

        sessionManager = SessionManager(this)
        locationManager = LocationManager(this)

        tvWelcome = findViewById(R.id.tvWelcome)
        btnLogout = findViewById(R.id.btnLogout)
        btnRegister = findViewById(R.id.btnRegister)
        listHistory = findViewById(R.id.listHistory)

        val user = getSharedPreferences("simtec_session", MODE_PRIVATE)
            .getString("user", "Usuario")

        tvWelcome.text = "Hola $user, registra tu asistencia"

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, history)
        listHistory.adapter = adapter

        loadHistory()

        btnRegister.setOnClickListener {
            authenticateUser()
        }

        btnLogout.setOnClickListener {

            sessionManager.logout()

            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        updateButtonText()
    }

    /**
     * PASO 1: Autenticación biométrica (huella dactilar)
     */
    private fun authenticateUser() {

        val executor = ContextCompat.getMainExecutor(this)

        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)

                    // Huella verificada, ahora vamos a reconocimiento facial
                    Toast.makeText(
                        this@HomeActivity,
                        "Huella verificada ✓",
                        Toast.LENGTH_SHORT
                    ).show()

                    openFaceDetection()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(
                        this@HomeActivity,
                        "Huella no reconocida",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(
                        this@HomeActivity,
                        "Error: $errString",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Paso 1: Verificación Biométrica")
            .setSubtitle("Confirma tu huella dactilar")
            .setNegativeButtonText("Cancelar")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    /**
     * PASO 2: Reconocimiento facial
     */
    private fun openFaceDetection() {

        val intent = Intent(this, CameraActivity::class.java)
        startActivityForResult(intent, 100)
    }

    /**
     * Resultado del reconocimiento facial
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100 && resultCode == RESULT_OK) {

            // Cara verificada, ahora obtener ubicación y guardar registro
            Toast.makeText(this, "Cara verificada ✓", Toast.LENGTH_SHORT).show()

            checkLocationPermission()
        }
    }

    /**
     * Verifica y solicita permisos de ubicación
     */
    private fun checkLocationPermission() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                2
            )

        } else {

            obtainLocationAndSaveRecord()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 2) {

            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {

                obtainLocationAndSaveRecord()

            } else {

                Toast.makeText(
                    this,
                    "Permisos de ubicación denegados",
                    Toast.LENGTH_SHORT
                ).show()

                // Guardamos igual sin ubicación
                saveRegister("Ubicación no disponible")
            }
        }
    }

    /**
     * PASO 3: Obtener ubicación y guardar registro
     */
    private fun obtainLocationAndSaveRecord() {

        // Mostramos loading
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

    /**
     * Guarda el registro de asistencia con ubicación
     */
    private fun saveRegister(location: String) {

        val sdf = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
        val time = sdf.format(Date())

        val type = if (isEntry) "Entrada" else "Salida"

        // Guardamos en SessionManager con ubicación
        sessionManager.saveAttendanceRecord(
            type = type,
            timestamp = time,
            location = location,
            faceConfidence = 0.9f // Placeholder, en una versión mejorada pasamos el valor real
        )

        // Mostramos en la lista
        val record = "$type - $time\n📍 $location"

        history.add(0, record)

        adapter.notifyDataSetChanged()

        saveHistory()

        isEntry = !isEntry

        updateButtonText()

        Toast.makeText(
            this,
            "Asistencia registrada ✓",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun updateButtonText() {
        if (isEntry) {
            btnRegister.text = "Entrada"
            btnRegister.setBackgroundTintList(
                ContextCompat.getColorStateList(this, R.color.green_entry)
            )
        } else {
            btnRegister.text = "Salida"
            btnRegister.setBackgroundTintList(
                ContextCompat.getColorStateList(this, R.color.red_exit)
            )
        }
    }

    private fun saveHistory() {

        val prefs = getSharedPreferences("attendance", MODE_PRIVATE)

        prefs.edit()
            .putStringSet("history", history.toSet())
            .apply()
    }

    private fun loadHistory() {

        val prefs = getSharedPreferences("attendance", MODE_PRIVATE)

        val saved = prefs.getStringSet("history", setOf())

        history.addAll(saved!!)

        adapter.notifyDataSetChanged()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
