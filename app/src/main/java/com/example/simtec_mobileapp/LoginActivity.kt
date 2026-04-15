package com.example.simtec_mobileapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.example.simtec_mobileapp.showLoading
import com.example.simtec_mobileapp.hideLoading
import kotlinx.coroutines.*
import java.util.concurrent.Executor

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnFingerprint: ImageButton
    private lateinit var progressBar: ProgressBar
    private lateinit var fingerprintCard: CardView
    private lateinit var tvFingerprintTitle: TextView
    private lateinit var tvFingerprintSubtitle: TextView

    private lateinit var sessionManager: SessionManager
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var cbRememberPassword: CheckBox
    private lateinit var tvForgotPassword: TextView

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val apiClient = ApiClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        sessionManager = SessionManager(this)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnFingerprint = findViewById(R.id.btnFingerprint)
        progressBar = findViewById(R.id.progressBar)
        fingerprintCard = findViewById(R.id.fingerprintCard)
        tvFingerprintTitle = findViewById(R.id.tvFingerprintTitle)
        tvFingerprintSubtitle = findViewById(R.id.tvFingerprintSubtitle)
        cbRememberPassword = findViewById(R.id.cbRememberPassword)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)

        checkBiometricSupport()
        loadSavedCredentials()

        tvForgotPassword.setOnClickListener {
            val url = getString(R.string.forgot_password_url)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor completa email y contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Por favor ingresa un email válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (cbRememberPassword.isChecked) {
                sessionManager.saveLastEmail(email)
            }

            performLogin(email, password)
        }

        btnFingerprint.setOnClickListener {
            biometricPrompt.authenticate(promptInfo)
        }
    }

    private fun performLogin(email: String, password: String) {
        btnLogin.isEnabled = false
        progressBar.visibility = View.VISIBLE
        etEmail.isEnabled = false
        etPassword.isEnabled = false

        Log.d("LoginActivity", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d("LoginActivity", "INICIANDO LOGIN")
        Log.d("LoginActivity", "Email: $email")
        Log.d("LoginActivity", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        scope.launch {
            try {
                val response = apiClient.login(email, password)

                Log.d("LoginActivity", "  Success: ${response?.success}")
                Log.d("LoginActivity", "  Token: ${response?.token?.take(20)}...")

                if (response?.success == true && response.token != null && response.user != null) {
                    val user = response.user!!

                    Log.d("LoginActivity", "✅ LOGIN EXITOSO")
                    Log.d("LoginActivity", "Usuario: ${user.nombre} (${user.email})")

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

                    sessionManager.saveLastEmail(user.email)

                    Toast.makeText(this@LoginActivity, "¡Bienvenido ${user.nombre}!", Toast.LENGTH_SHORT).show()

                    goToHome()

                } else {
                    val errorMessage = when {
                        response == null -> "No se pudo conectar con el servidor"
                        !response.success -> response.message ?: "Credenciales inválidas"
                        else -> "Error desconocido"
                    }
                    
                    Toast.makeText(this@LoginActivity, errorMessage, Toast.LENGTH_LONG).show()

                    btnLogin.isEnabled = true
                    progressBar.visibility = View.GONE
                    etEmail.isEnabled = true
                    etPassword.isEnabled = true
                }

            } catch (e: Exception) {
                Log.e("LoginActivity", "❌ EXCEPCIÓN: ${e.message}", e)
                Toast.makeText(this@LoginActivity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()

                btnLogin.isEnabled = true
                progressBar.visibility = View.GONE
                etEmail.isEnabled = true
                etPassword.isEnabled = true
            }
        }
    }

    private fun performFingerprintLogin(email: String, password: String) {
        btnFingerprint.isEnabled = false
        progressBar.visibility = View.VISIBLE
        etEmail.isEnabled = false

        Log.d("LoginActivity", "🔐 LOGIN CON HUELLA")
        Log.d("LoginActivity", "Email: $email")

        scope.launch {
            try {
                val response = apiClient.login(email, password)

                if (response?.success == true && response.token != null && response.user != null) {
                    val user = response.user!!

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

                    sessionManager.saveLastEmail(user.email)

                    Toast.makeText(this@LoginActivity, "¡Bienvenido ${user.nombre}!", Toast.LENGTH_SHORT).show()

                    goToHome()

                } else {
                    Toast.makeText(this@LoginActivity, "Error al iniciar sesión", Toast.LENGTH_SHORT).show()
                    btnFingerprint.isEnabled = true
                    progressBar.visibility = View.GONE
                }

            } catch (e: Exception) {
                Log.e("LoginActivity", "❌ EXCEPCIÓN: ${e.message}", e)
                Toast.makeText(this@LoginActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                btnFingerprint.isEnabled = true
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun checkBiometricSupport() {
        val biometricManager = BiometricManager.from(this)

        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                val savedEmail = sessionManager.getSavedEmail()
                
                if (sessionManager.isLogged() || savedEmail.isNotEmpty()) {
                    etEmail.setText(savedEmail)
                    etEmail.setEnabled(false)
                    fingerprintCard.visibility = View.VISIBLE
                    
                    if (sessionManager.isLogged()) {
                        tvFingerprintTitle.text = "Continuar sesión"
                        tvFingerprintSubtitle.text = "Toca para entrar directamente"
                    } else {
                        tvFingerprintTitle.text = getString(R.string.fingerprint_title)
                        tvFingerprintSubtitle.text = getString(R.string.fingerprint_subtitle)
                    }
                } else {
                    fingerprintCard.visibility = View.GONE
                }
            }

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                fingerprintCard.visibility = View.GONE
            }

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Toast.makeText(this, "No hay huellas registradas", Toast.LENGTH_LONG).show()
                fingerprintCard.visibility = View.GONE
            }

            else -> {
                fingerprintCard.visibility = View.GONE
            }
        }
    }

    private fun setupBiometric() {
        executor = ContextCompat.getMainExecutor(this)

        biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)

                    if (sessionManager.isLogged()) {
                        Toast.makeText(this@LoginActivity, "¡Bienvenido de vuelta!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, "Tu sesión expiró. Ingresa tu contraseña", Toast.LENGTH_LONG).show()
                        etPassword.requestFocus()
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Huella no reconocida", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode != 10) {
                        Toast.makeText(applicationContext, "Error: $errString", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.fingerprint_title))
            .setSubtitle("Confirma tu huella para acceder")
            .setNegativeButtonText("Usar contraseña")
            .build()
    }

    override fun onResume() {
        super.onResume()
        setupBiometric()
        checkBiometricSupport()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun goToHome() {
        sessionManager.clearAttendanceData()
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    private fun loadSavedCredentials() {
        val savedEmail = sessionManager.getSavedEmail()
        if (savedEmail.isNotEmpty()) {
            etEmail.setText(savedEmail)
            etEmail.setEnabled(false)
            cbRememberPassword.isChecked = true
            etPassword.requestFocus()
        }
    }
}