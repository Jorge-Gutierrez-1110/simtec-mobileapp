package com.example.simtec_mobileapp

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

import com.example.simtec_mobileapp.Periodo
import com.example.simtec_mobileapp.EmpleadoNomina
import com.example.simtec_mobileapp.Concepto
import com.example.simtec_mobileapp.ApiClient.NominaSolicitud

class SolicitudesActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressLayout: LinearLayout
    private lateinit var tvTotal: TextView
    private lateinit var tvMonto: TextView
    private lateinit var tvPendientes: TextView
    private lateinit var fabAgregar: FloatingActionButton

    private var solicitudes: List<NominaSolicitud> = emptyList()
    private var currentTab = 0
    private var puedeAprobar = false
    
    private var empleados: List<EmpleadoNomina> = emptyList()
    private var periodos: List<Periodo> = emptyList()
    private var conceptos: List<Concepto> = emptyList()
    private var filteredConceptos: List<Concepto> = emptyList()
    private var selectedTipo = "Percepcion"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_solicitudes)

        val sessionManager = SessionManager(this)
        val rol = sessionManager.getRol()
        puedeAprobar = rol == "Admin" || rol == "RH" || rol == "Director"

        initViews()
        setupToolbar()
        loadSolicitudes()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        tabLayout = findViewById(R.id.tabLayout)
        recyclerView = findViewById(R.id.recyclerView)
        progressLayout = findViewById(R.id.progressLayout)
        tvTotal = findViewById(R.id.tvTotal)
        tvMonto = findViewById(R.id.tvMonto)
        tvPendientes = findViewById(R.id.tvPendientes)
        fabAgregar = findViewById(R.id.fabAgregar)

        tabLayout.addTab(tabLayout.newTab().setText("Mis Solicitudes"))
        tabLayout.addTab(tabLayout.newTab().setText("Por Aprobar"))
        tabLayout.addTab(tabLayout.newTab().setText("Historial"))

        recyclerView.layoutManager = LinearLayoutManager(this)

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTab = tab?.position ?: 0
                loadSolicitudes()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        fabAgregar.setOnClickListener {
            showNuevaSolicitudDialog()
        }
    }

    private fun setupToolbar() {
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun loadSolicitudes() {
        showLoading(true)
        lifecycleScope.launch {
            val sessionManager = SessionManager(this@SolicitudesActivity)
            val apiClient = ApiClient()
            apiClient.setAuthToken(sessionManager.getToken())

            val usuarioId = sessionManager.getEmpleadoId()
            val rol = sessionManager.getRol() ?: "Empleado"

            val tipo = when (currentTab) {
                0 -> "mis_solicitudes"
                1 -> "por_aprobar"
                2 -> "historial_aprobador"
                else -> "mis_solicitudes"
            }

            val result = apiClient.getNominaSolicitudes(usuarioId, rol, tipo)
            showLoading(false)

            if (result != null) {
                solicitudes = result
                updateKPIs()
                recyclerView.adapter = SolicitudesAdapter(solicitudes, puedeAprobar) { solicitud, accion ->
                    responderSolicitud(solicitud.id, accion)
                }
            } else {
                showError("Error al cargar solicitudes")
            }
        }
    }

    private fun updateKPIs() {
        val total = solicitudes.size
        val monto = solicitudes.sumOf { it.monto }
        val pendientes = solicitudes.count { it.estatus == "PENDIENTE" }

        tvTotal.text = total.toString()
        tvMonto.text = String.format("$%,.2f", monto)
        tvPendientes.text = pendientes.toString()
    }

    private fun responderSolicitud(solicitudId: Int, accion: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(if (accion == "APROBADO") "Aprobar" else "Rechazar")

        val input = android.widget.EditText(this)
        input.hint = "Comentarios"
        builder.setView(input)

        builder.setPositiveButton("Confirmar") { _, _ ->
            val comentario = input.text.toString()
            if (comentario.isEmpty()) {
                showError("Agrega un comentario")
                return@setPositiveButton
            }
            enviarRespuesta(solicitudId, accion, comentario)
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun enviarRespuesta(solicitudId: Int, accion: String, comentario: String) {
        showLoading(true)
        lifecycleScope.launch {
            val session = SessionManager(this@SolicitudesActivity)
            val apiClient = ApiClient()
            apiClient.setAuthToken(session.getToken())

            val result = apiClient.responderNominaSolicitud(
                solicitudId,
                accion,
                comentario
            )

            showLoading(false)

            val success = result?.get("success") as? Boolean ?: false
            if (success) {
                showSuccess(if (accion == "APROBADO") "Aprobado" else "Rechazado")
                loadSolicitudes()
            } else {
                val msg = result?.get("message") as? String ?: "Error"
                showError(msg)
            }
        }
    }

    private fun showNuevaSolicitudDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_nueva_solicitud, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val spinnerEmpleado = dialogView.findViewById<Spinner>(R.id.spinnerEmpleado)
        val spinnerPeriodo = dialogView.findViewById<Spinner>(R.id.spinnerPeriodo)
        val spinnerConcepto = dialogView.findViewById<Spinner>(R.id.spinnerConcepto)
        val rgTipo = dialogView.findViewById<RadioGroup>(R.id.rgTipo)
        val etMonto = dialogView.findViewById<EditText>(R.id.etMonto)
        val etComentarios = dialogView.findViewById<EditText>(R.id.etComentarios)
        val btnEnviar = dialogView.findViewById<MaterialButton>(R.id.btnEnviar)
        val btnCerrar = dialogView.findViewById<ImageButton>(R.id.btnCerrar)
        val progressLayout = dialogView.findViewById<LinearLayout>(R.id.progressLayout)

        loadCatalogosForSolicitud(spinnerEmpleado, spinnerPeriodo, spinnerConcepto, rgTipo)

        var currentTipo = "Percepcion"
        rgTipo.setOnCheckedChangeListener { _, checkedId ->
            currentTipo = if (checkedId == R.id.rbPercepcion) "Percepcion" else "Deduccion"
            filterConceptos(currentTipo, spinnerConcepto)
        }

        btnCerrar.setOnClickListener { dialog.dismiss() }

        btnEnviar.setOnClickListener {
            val selectedEmpleado = spinnerEmpleado.selectedItemPosition
            val selectedPeriodo = spinnerPeriodo.selectedItemPosition
            val selectedConcepto = spinnerConcepto.selectedItemPosition

            if (selectedEmpleado < 0 || selectedPeriodo < 0 || selectedConcepto < 0) {
                showError("Cargando datos... intenta en unos segundos")
                return@setOnClickListener
            }

            if (empleados.isEmpty() || periodos.isEmpty() || filteredConceptos.isEmpty()) {
                showError("Cargando catálogos... intenta en unos segundos")
                return@setOnClickListener
            }

            val monto = etMonto.text.toString().toDoubleOrNull()
            val comentarios = etComentarios.text.toString()

            if (monto == null || monto <= 0) {
                showError("Ingresa un monto válido")
                return@setOnClickListener
            }

            if (comentarios.isEmpty()) {
                showError("Ingresa una justificación")
                return@setOnClickListener
            }

            val empleado = empleados[selectedEmpleado]
            val periodo = periodos[selectedPeriodo]
            val concepto = filteredConceptos[selectedConcepto]

            progressLayout.visibility = View.VISIBLE
            btnEnviar.isEnabled = false

            lifecycleScope.launch {
                try {
                    val session = SessionManager(this@SolicitudesActivity)
                    val apiClient = ApiClient()
                    apiClient.setAuthToken(session.getToken())

                    val result = apiClient.crearNominaSolicitud(
                        empleado_id = empleado.id,
                        solicitante_id = session.getEmpleadoId(),
                        periodo_id = periodo.id,
                        concepto_id = concepto.id,
                        monto = monto,
                        comentarios = comentarios,
                        tipo = currentTipo
                    )

                    progressLayout.visibility = View.GONE

                    val success = result?.get("success") as? Boolean ?: false
                    if (success) {
                        showSuccess("Solicitud enviada")
                        dialog.dismiss()
                        loadSolicitudes()
                    } else {
                        val msg = result?.get("message") as? String ?: "Error al enviar"
                        showError(msg)
                        btnEnviar.isEnabled = true
                    }
                } catch (e: Exception) {
                    progressLayout.visibility = View.GONE
                    showError("Error: ${e.message}")
                    btnEnviar.isEnabled = true
                }
            }
        }

        dialog.show()
    }

    private fun loadCatalogosForSolicitud(
        spinnerEmpleado: Spinner,
        spinnerPeriodo: Spinner,
        spinnerConcepto: Spinner,
        rgTipo: RadioGroup
    ) {
        lifecycleScope.launch {
            val session = SessionManager(this@SolicitudesActivity)
            val apiClient = ApiClient()
            apiClient.setAuthToken(session.getToken())

            val (empRes, perRes, conRes) = Triple(
                apiClient.getEmpleadosNomina(),
                apiClient.getPeriodos(),
                apiClient.getConceptosNomina()
            )

            empleados = empRes ?: emptyList()
            periodos = perRes ?: emptyList()
            conceptos = conRes ?: emptyList()

            val nombresEmpleados = empleados.map { "${it.nombre} ${it.apellido_paterno}" }
            val nombresPeriodos = periodos.map { it.nombre }
            val nombresConceptos = conceptos.filter { it.tipo == "Percepcion" }.map { it.nombre }

            filteredConceptos = conceptos.filter { it.tipo == "Percepcion" }

            runOnUiThread {
                spinnerEmpleado.adapter = ArrayAdapter(this@SolicitudesActivity, android.R.layout.simple_spinner_dropdown_item, nombresEmpleados)
                spinnerPeriodo.adapter = ArrayAdapter(this@SolicitudesActivity, android.R.layout.simple_spinner_dropdown_item, nombresPeriodos)
                spinnerConcepto.adapter = ArrayAdapter(this@SolicitudesActivity, android.R.layout.simple_spinner_dropdown_item, nombresConceptos)
            }
        }
    }

    private fun filterConceptos(tipo: String, spinnerConcepto: Spinner) {
        filteredConceptos = conceptos.filter { it.tipo == tipo }
        val nombres = filteredConceptos.map { it.nombre }
        spinnerConcepto.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, nombres)
    }

    private fun showLoading(show: Boolean) {
        progressLayout.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

class SolicitudesAdapter(
    private val solicitudes: List<NominaSolicitud>,
    private val puedeAprobar: Boolean,
    private val onAction: (NominaSolicitud, String) -> Unit
) : RecyclerView.Adapter<SolicitudesAdapter.ViewHolder>() {

    class ViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view) {
        val tvFolio: TextView = view.findViewById(R.id.tvFolio)
        val tvEmpleado: TextView = view.findViewById(R.id.tvEmpleado)
        val tvTipo: TextView = view.findViewById(R.id.tvTipo)
        val tvConcepto: TextView = view.findViewById(R.id.tvConcepto)
        val tvPeriodo: TextView = view.findViewById(R.id.tvPeriodo)
        val tvMonto: TextView = view.findViewById(R.id.tvMonto)
        val tvEstatus: TextView = view.findViewById(R.id.tvEstatus)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_solicitud, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val sol = solicitudes[position]
        holder.tvFolio.text = "#NOM-${sol.id}"
        holder.tvEmpleado.text = "${sol.emp_nombre} ${sol.emp_apellido}"
        holder.tvTipo.text = sol.tipo
        holder.tvConcepto.text = sol.concepto_nombre
        holder.tvPeriodo.text = sol.periodo_nombre ?: "Periodo #${sol.periodo_id}"
        holder.tvMonto.text = String.format("$%,.2f", sol.monto)
        holder.tvEstatus.text = sol.estatus

        val bgRes = when (sol.estatus) {
            "APROBADO", "AUTO_APROBADO" -> R.drawable.bg_status_success
            "RECHAZADO" -> R.drawable.bg_status_error
            else -> R.drawable.bg_status_pending
        }
        holder.tvEstatus.setBackgroundResource(bgRes)
    }

    override fun getItemCount() = solicitudes.size
}
