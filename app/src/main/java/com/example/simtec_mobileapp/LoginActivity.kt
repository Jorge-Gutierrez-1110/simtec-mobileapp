package com.example.simtec_mobileapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.util.concurrent.Executor

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnFingerprint: ImageButton
    private lateinit var progressBar: ProgressBar

    private lateinit var sessionManager: SessionManager
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val apiClient = ApiClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        sessionManager = SessionManager(this)

        // Si ya hay sesión activa, ir a HomeActivity
        if (sessionManager.isLogged()) {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
            return
        }

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnFingerprint = findViewById(R.id.btnFingerprint)
        progressBar = findViewById(R.id.progressBar)

        checkBiometricSupport()
        setupBiometric()

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Validación básica
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(
                    this,
                    "Por favor completa email y contraseña",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Validar formato de email
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(
                    this,
                    "Por favor ingresa un email válido",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            performLogin(email, password)
        }

        btnFingerprint.setOnClickListener {
            biometricPrompt.authenticate(promptInfo)
        }
    }

    /**
     * Realiza el login con email y password contra la API
     */
    private fun performLogin(email: String, password: String) {
        // Deshabilitamos UI
        btnLogin.isEnabled = false
        progressBar.visibility = View.VISIBLE
        etEmail.isEnabled = false
        etPassword.isEnabled = false

        scope.launch {
            try {
                // Llamar a la API
                val response = apiClient.login(email, password)

                if (response?.success == true && response.token != null && response.user != null) {
                    // Login exitoso
                    val user = response.user!!

                    // Guardar sesión con token
                    sessionManager.saveLoginWithToken(
                        token = response.token,
                        userId = user.id,
                        userName = user.nombre,
                        email = user.email,
                        rolId = user.rol_id,
                        rol = user.rol,
                        empleadoId = user.empleado_id,
                        permisos = user.permisos
                    )

                    Toast.makeText(
                        this@LoginActivity,
                        "¡Bienvenido ${user.nombre}!",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Ir a HomeActivity
                    startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
                    finish()

                } else {
                    // Login fallido
                    Toast.makeText(
                        this@LoginActivity,
                        response?.message ?: "Email o contraseña incorrectos",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@LoginActivity,
                    "Error de conexión: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                e.printStackTrace()

            } finally {
                // Rehabilitar UI
                btnLogin.isEnabled = true
                progressBar.visibility = View.GONE
                etEmail.isEnabled = true
                etPassword.isEnabled = true
            }
        }
    }

    private fun checkBiometricSupport() {

        val biometricManager = BiometricManager.from(this)

        when (biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        )) {

            BiometricManager.BIOMETRIC_SUCCESS -> {

                if (sessionManager.isLogged()) {
                    btnFingerprint.visibility = View.VISIBLE
                }

            }

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Toast.makeText(this,"Este dispositivo no tiene sensor biométrico",Toast.LENGTH_LONG).show()
            }

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Toast.makeText(this,"No hay huellas registradas en el dispositivo",Toast.LENGTH_LONG).show()
            }

            else -> {
                btnFingerprint.visibility = View.GONE
            }
        }
    }

    private fun setupBiometric() {

        executor = ContextCompat.getMainExecutor(this)

        biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {

                    super.onAuthenticationSucceeded(result)

                    startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
                    finish()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(
                        applicationContext,
                        "Huella no reconocida",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Inicio de sesión biométrico")
            .setSubtitle("Usa tu huella para acceder")
            .setNegativeButtonText("Cancelar")
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
