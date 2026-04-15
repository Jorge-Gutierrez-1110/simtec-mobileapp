package com.example.simtec_mobileapp

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class PerfilActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var ivFoto: ImageView
    private lateinit var tvNombre: TextView
    private lateinit var tvPuesto: TextView
    private lateinit var tvNumeroEmpleado: TextView
    private lateinit var progressLayout: LinearLayout

    private lateinit var rowEmail: View
    private lateinit var rowTelefono: View
    private lateinit var rowRFC: View
    private lateinit var rowCURP: View
    private lateinit var rowNSS: View
    private lateinit var rowEmpresa: View
    private lateinit var rowTurno: View
    private lateinit var rowHorario: View
    private lateinit var rowFechaIngreso: View
    private lateinit var rowStatus: View
    private lateinit var rowBanco: View
    private lateinit var rowCuenta: View
    private lateinit var rowCLABE: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        initViews()
        setupToolbar()
        loadPerfil()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        ivFoto = findViewById(R.id.ivFoto)
        tvNombre = findViewById(R.id.tvNombre)
        tvPuesto = findViewById(R.id.tvPuesto)
        tvNumeroEmpleado = findViewById(R.id.tvNumeroEmpleado)
        progressLayout = findViewById(R.id.progressLayout)

        rowEmail = findViewById(R.id.rowEmail)
        rowTelefono = findViewById(R.id.rowTelefono)
        rowRFC = findViewById(R.id.rowRFC)
        rowCURP = findViewById(R.id.rowCURP)
        rowNSS = findViewById(R.id.rowNSS)
        rowEmpresa = findViewById(R.id.rowEmpresa)
        rowTurno = findViewById(R.id.rowTurno)
        rowHorario = findViewById(R.id.rowHorario)
        rowFechaIngreso = findViewById(R.id.rowFechaIngreso)
        rowStatus = findViewById(R.id.rowStatus)
        rowBanco = findViewById(R.id.rowBanco)
        rowCuenta = findViewById(R.id.rowCuenta)
        rowCLABE = findViewById(R.id.rowCLABE)
    }

    private fun setupToolbar() {
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun loadPerfil() {
        showLoading(true)

        val sessionManager = SessionManager(this)
        val empleadoId = sessionManager.getEmpleadoId()

        if (empleadoId <= 0) {
            showError("No se encontró el empleado")
            return
        }

        lifecycleScope.launch {
            try {
                val apiClient = ApiClient()
                apiClient.setAuthToken(sessionManager.getToken())

                val perfil = apiClient.getEmpleadoCompleto(empleadoId)

                var empresaNombre = perfil?.empresa_nombre ?: "No asignada"
                val clienteId = perfil?.cliente_id

                if (clienteId != null && clienteId > 0) {
                    val empresa = apiClient.getCatalogoById("clientes", clienteId)
                    if (empresa != null) {
                        empresaNombre = empresa["nombre_comercial"]?.toString() ?: empresaNombre
                    }
                }

                showLoading(false)

                if (perfil != null) {
                    displayPerfil(perfil, empresaNombre)
                } else {
                    showError("Error al cargar perfil")
                }

            } catch (e: Exception) {
                showLoading(false)
                showError("Error: ${e.message}")
            }
        }
    }

    private fun displayPerfil(p: ApiClient.EmpleadoPerfil, empresaNombre: String) {
        tvNombre.text = "${p.nombre} ${p.apellido_paterno} ${p.apellido_materno ?: ""}"
        tvPuesto.text = p.puesto ?: "Sin puesto"
        tvNumeroEmpleado.text = "EMP-${p.numero_empleado}"

        // Datos Personales
        setRowValue(rowEmail, "Email", p.email)
        setRowValue(rowTelefono, "Teléfono", p.telefono ?: "No registrado")
        setRowValue(rowRFC, "RFC", p.rfc ?: "No registrado")
        setRowValue(rowCURP, "CURP", p.curp ?: "No registrado")
        setRowValue(rowNSS, "NSS", p.nss ?: "No registrado")

        // Datos Laborales
        setRowValue(rowEmpresa, "Empresa", empresaNombre)
        setRowValue(rowTurno, "Turno", p.nombre_turno ?: "No asignado")
        setRowValue(rowHorario, "Horario", "${p.hora_entrada ?: "--:--"} - ${p.hora_salida ?: "--:--"}")
        setRowValue(rowFechaIngreso, "Fecha de Ingreso", formatFecha(p.fecha_ingreso))
        setRowValue(rowStatus, "Estatus", p.status)

        // Datos Bancarios
        setRowValue(rowBanco, "Banco", p.banco ?: "No registrado")
        setRowValue(rowCuenta, "Cuenta", p.cuenta_bancaria ?: "No registrada")
        setRowValue(rowCLABE, "CLABE", p.clabe_interbancaria ?: "No registrada")
    }

    private fun setRowValue(view: View, label: String, value: String) {
        val tvLabel = view.findViewById<TextView>(R.id.tvLabel)
        val tvValor = view.findViewById<TextView>(R.id.tvValor)
        tvLabel.text = label
        tvValor.text = value
    }

    private fun formatFecha(fecha: String?): String {
        if (fecha.isNullOrEmpty()) return "No registrada"
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale("es", "MX"))
            val date = inputFormat.parse(fecha)
            outputFormat.format(date!!)
        } catch (e: Exception) {
            fecha
        }
    }

    private fun showLoading(show: Boolean) {
        progressLayout.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
