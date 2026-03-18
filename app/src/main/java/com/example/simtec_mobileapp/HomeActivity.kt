package com.example.simtec_mobileapp

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : AppCompatActivity() {

    private lateinit var btnLogout: Button
    private lateinit var btnRegister: Button
    private lateinit var listHistory: ListView
    private lateinit var tvWelcome: TextView

    private lateinit var sessionManager: SessionManager

    private lateinit var adapter: ArrayAdapter<String>
    private val history = mutableListOf<String>()

    private var isEntry = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_home)

        sessionManager = SessionManager(this)

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
    }

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

                    checkCameraPermission()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Verificación requerida")
            .setSubtitle("Confirma tu identidad")
            .setNegativeButtonText("Cancelar")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun checkCameraPermission() {

        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.CAMERA),
                1
            )

        } else {

            openCamera()

        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1) {

            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {

                openCamera()

            } else {

                Toast.makeText(
                    this,
                    "Se necesita permiso de cámara",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun openCamera() {

        val intent = Intent(this, CameraActivity::class.java)
        startActivityForResult(intent, 100)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100 && resultCode == RESULT_OK) {

            saveRegister()
        }
    }

    private fun saveRegister() {

        val sdf = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
        val time = sdf.format(Date())

        val type = if (isEntry) "Entrada" else "Salida"

        val record = "$type - $time"

        history.add(0, record)

        adapter.notifyDataSetChanged()

        saveHistory()

        isEntry = !isEntry

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
}