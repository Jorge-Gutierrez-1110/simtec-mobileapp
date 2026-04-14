package com.example.simtec_mobileapp

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch

class NominaActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var step1: LinearLayout
    private lateinit var step2: LinearLayout
    private lateinit var step3: LinearLayout
    private lateinit var step4: LinearLayout
    private lateinit var step1Circle: TextView
    private lateinit var step2Circle: TextView
    private lateinit var step3Circle: TextView
    private lateinit var step4Circle: TextView
    private lateinit var contentFrame: FrameLayout
    private lateinit var progressLayout: LinearLayout

    private var currentStep = 1
    private var selectedPeriodoId: Int = 0
    private var nominaId: Int = 0
    private var periodos: List<Periodo> = emptyList()
    private var empleadoDetalles: List<EmpleadoNomina> = emptyList()
    private var resumenNomina: NominaResumen? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nomina)

        initViews()
        setupToolbar()
        loadPeriodos()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        step1 = findViewById(R.id.step1)
        step2 = findViewById(R.id.step2)
        step3 = findViewById(R.id.step3)
        step4 = findViewById(R.id.step4)
        step1Circle = findViewById(R.id.step1Circle)
        step2Circle = findViewById(R.id.step2Circle)
        step3Circle = findViewById(R.id.step3Circle)
        step4Circle = findViewById(R.id.step4Circle)
        contentFrame = findViewById(R.id.contentFrame)
        progressLayout = findViewById(R.id.progressLayout)
    }

    private fun setupToolbar() {
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert)
        toolbar.setNavigationOnClickListener {
            when (currentStep) {
                1 -> finish()
                2 -> {
                    currentStep = 1
                    renderStep1Periodos()
                }
                3 -> {
                    currentStep = 2
                    renderStep2Prenomina()
                }
                4 -> {
                    currentStep = 3
                    renderStep3Nomina()
                }
            }
        }
    }

    private fun loadPeriodos() {
        showLoading(true)
        lifecycleScope.launch {
            val apiClient = ApiClient()
            apiClient.setAuthToken(SessionManager(this@NominaActivity).getToken())

            val periodosResult = apiClient.getPeriodos()
            showLoading(false)

            if (periodosResult != null && periodosResult.isNotEmpty()) {
                periodos = periodosResult
                renderStep1Periodos()
            } else {
                showError(getString(R.string.no_periodos))
            }
        }
    }

    private fun renderStep1Periodos() {
        contentFrame.removeAllViews()

        val scrollView = ScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setPadding(32, 32, 32, 32)
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val title = TextView(this).apply {
            text = getString(R.string.seleccionar_periodo)
            setTextColor(resources.getColor(R.color.text_primary, theme))
            textSize = 20f
            setPadding(0, 0, 0, 32)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        container.addView(title)

        periodos.forEach { periodo ->
            val periodoCard = layoutInflater.inflate(R.layout.item_periodo, container, false)

            val nombreView = periodoCard.findViewById<TextView>(R.id.periodoNombre)
            val fechasView = periodoCard.findViewById<TextView>(R.id.periodoFechas)
            val statusView = periodoCard.findViewById<TextView>(R.id.periodoStatus)

            nombreView.text = periodo.nombre
            fechasView.text = "${periodo.fecha_inicio} - ${periodo.fecha_fin}"
            statusView.text = periodo.status_cierre

            periodoCard.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 24
            }

            periodoCard.setOnClickListener {
                selectedPeriodoId = periodo.id
                generatePrenomina()
            }

            container.addView(periodoCard)
        }

        scrollView.addView(container)
        contentFrame.addView(scrollView)
        updateStepper()
    }

    private fun generatePrenomina() {
        showLoading(true)
        lifecycleScope.launch {
            val apiClient = ApiClient()
            apiClient.setAuthToken(SessionManager(this@NominaActivity).getToken())

            val result = apiClient.preCalculoNomina(selectedPeriodoId)
            showLoading(false)

            if (result?.success == true) {
                nominaId = result.nomina_id ?: 0
                currentStep = 2
                renderStep2Prenomina()
            } else {
                showError(result?.message ?: getString(R.string.error_generar_nomina))
            }
        }
    }

    private fun renderStep2Prenomina() {
        showLoading(true)
        lifecycleScope.launch {
            val apiClient = ApiClient()
            apiClient.setAuthToken(SessionManager(this@NominaActivity).getToken())

            val detalles = apiClient.getNominaDetalle(nominaId)
            showLoading(false)

            if (detalles != null && detalles.isNotEmpty()) {
                empleadoDetalles = detalles

                var totalEmpleados = 0
                var totalPercepciones = 0.0
                var totalDeducciones = 0.0
                var totalNeto = 0.0

                detalles.forEach { emp ->
                    totalEmpleados++
                    totalPercepciones += emp.totalPercepciones
                    totalDeducciones += emp.totalDeducciones
                    totalNeto += emp.neto
                }

                resumenNomina = NominaResumen(
                    total_empleados = totalEmpleados,
                    total_percepciones = totalPercepciones,
                    total_deducciones = totalDeducciones,
                    total_neto = totalNeto
                )

                contentFrame.removeAllViews()

                val scrollView = ScrollView(this@NominaActivity).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setPadding(32, 32, 32, 32)
                }

                val container = LinearLayout(this@NominaActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }

                val title = TextView(this@NominaActivity).apply {
                    text = getString(R.string.generar_prenomina)
                    setTextColor(resources.getColor(R.color.text_primary, theme))
                    textSize = 20f
                    setPadding(0, 0, 0, 32)
                }
                container.addView(title)

                addResumenCard(container,
                    getString(R.string.total_empleados) to totalEmpleados.toString(),
                    getString(R.string.total_percepciones) to String.format("$%,.2f", totalPercepciones),
                    getString(R.string.total_deducciones) to String.format("$%,.2f", totalDeducciones),
                    getString(R.string.total_neto) to String.format("$%,.2f", totalNeto)
                )

                val btnVerDetalle = com.google.android.material.button.MaterialButton(this@NominaActivity).apply {
                    text = getString(R.string.ver_detalle)
                    setOnClickListener {
                        currentStep = 3
                        renderStep3Nomina()
                    }
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = 32
                    }
                }
                container.addView(btnVerDetalle)

                val btnCerrar = com.google.android.material.button.MaterialButton(this@NominaActivity).apply {
                    text = getString(R.string.cerrar_nomina)
                    setOnClickListener { cerrarNomina() }
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = 16
                    }
                }
                container.addView(btnCerrar)

                scrollView.addView(container)
                contentFrame.addView(scrollView)
                updateStepper()
            } else {
                showError(getString(R.string.error_generar_nomina))
            }
        }
    }

    private fun renderStep3Nomina() {
        contentFrame.removeAllViews()

        val scrollView = ScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setPadding(32, 32, 32, 32)
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val title = TextView(this).apply {
            text = getString(R.string.step_nomina)
            setTextColor(resources.getColor(R.color.text_primary, theme))
            textSize = 20f
            setPadding(0, 0, 0, 24)
        }
        container.addView(title)

        empleadoDetalles.forEach { emp ->
            val empCard = createEmpleadoCard(emp)
            container.addView(empCard)
        }

        val btnEnviarEmail = com.google.android.material.button.MaterialButton(this).apply {
            text = getString(R.string.enviar_email)
            setOnClickListener { showEmailDialog() }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 32
            }
        }
        container.addView(btnEnviarEmail)

        scrollView.addView(container)
        contentFrame.addView(scrollView)
        updateStepper()
    }

    private fun createEmpleadoCard(emp: EmpleadoNomina): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(resources.getColor(R.color.white, theme))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
        }

        val nombreText = TextView(this).apply {
            text = "${emp.nombre} ${emp.apellido_paterno}"
            setTextColor(resources.getColor(R.color.text_primary, theme))
            textSize = 16f
            setPadding(0, 0, 0, 8)
        }
        card.addView(nombreText)

        val numeroText = TextView(this).apply {
            text = "No. Empleado: ${emp.numero_empleado}"
            setTextColor(resources.getColor(R.color.text_secondary, theme))
            textSize = 14f
            setPadding(0, 0, 0, 16)
        }
        card.addView(numeroText)

        val percepcionesRow = createRow("Percepciones", String.format("$%,.2f", emp.totalPercepciones))
        card.addView(percepcionesRow)

        val deduccionesRow = createRow("Deducciones", String.format("$%,.2f", emp.totalDeducciones))
        card.addView(deduccionesRow)

        val netoRow = createRow("Neto", String.format("$%,.2f", emp.neto))
        card.addView(netoRow)

        return card
    }

    private fun createRow(label: String, value: String): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 8, 0, 8)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val labelView = TextView(this).apply {
            text = label
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setTextColor(resources.getColor(R.color.text_secondary, theme))
        }

        val valueView = TextView(this).apply {
            text = value
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            setTextColor(resources.getColor(R.color.text_primary, theme))
            textSize = 16f
        }

        row.addView(labelView)
        row.addView(valueView)
        return row
    }

    private fun renderStep4Cierre() {
        contentFrame.removeAllViews()

        val successCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
            setBackgroundColor(resources.getColor(R.color.green_light, theme))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 100
            }
        }

        val checkMark = TextView(this).apply {
            text = "✓"
            textSize = 64f
            setTextColor(resources.getColor(R.color.green_entry, theme))
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val mensaje = TextView(this).apply {
            text = getString(R.string.nomina_cerrada_success)
            setTextColor(resources.getColor(R.color.green_entry, theme))
            textSize = 20f
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setPadding(0, 24, 0, 0)
        }

        val btnRegresar = com.google.android.material.button.MaterialButton(this).apply {
            text = "Regresar al Inicio"
            setOnClickListener { finish() }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 48
            }
        }

        successCard.addView(checkMark)
        successCard.addView(mensaje)
        successCard.addView(btnRegresar)
        contentFrame.addView(successCard)

        updateStepper()
    }

    private fun cerrarNomina() {
        showLoading(true)
        lifecycleScope.launch {
            val apiClient = ApiClient()
            apiClient.setAuthToken(SessionManager(this@NominaActivity).getToken())

            val detalles = empleadoDetalles.map { emp ->
                DetalleNomina(
                    empleado_id = emp.id,
                    dias_pagados = emp.diasTrab,
                    sueldo_diario = emp.salario_diario,
                    total_percepciones = emp.totalPercepciones,
                    total_deducciones = emp.totalDeducciones,
                    neto = emp.neto,
                    desglose = Desglose(
                        percepciones = emp.listaPercepciones,
                        deducciones = emp.listaDeducciones
                    )
                )
            }

            val request = NominaCierreRequest(
                periodo_id = selectedPeriodoId,
                resumen = resumenNomina ?: NominaResumen(0, 0.0, 0.0, 0.0),
                detalles = detalles
            )

            val result = apiClient.cerrarNomina(request)
            showLoading(false)

            if (result?.success == true) {
                currentStep = 4
                renderStep4Cierre()
            } else {
                showError(result?.message ?: getString(R.string.error_generar_nomina))
            }
        }
    }

    private fun showEmailDialog() {
        val email = SessionManager(this).getEmail()
        if (email.isNullOrEmpty()) {
            showError("No se encontró email del usuario")
            return
        }

        showLoading(true)
        lifecycleScope.launch {
            val apiClient = ApiClient()
            apiClient.setAuthToken(SessionManager(this@NominaActivity).getToken())

            val result = apiClient.enviarEmailNomina(nominaId, email)
            showLoading(false)

            if (result?.success == true) {
                Toast.makeText(this@NominaActivity, "Email enviado exitosamente", Toast.LENGTH_LONG).show()
            } else {
                showError(result?.message ?: "Error al enviar email")
            }
        }
    }

    private fun addResumenCard(container: LinearLayout, vararg items: Pair<String, String>) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(resources.getColor(R.color.white, theme))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        items.forEach { (label, value) ->
            val row = LinearLayout(this@NominaActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 8, 0, 8)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }

            val labelView = TextView(this@NominaActivity).apply {
                text = label
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setTextColor(resources.getColor(R.color.text_secondary, theme))
            }

            val valueView = TextView(this@NominaActivity).apply {
                text = value
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                setTextColor(resources.getColor(R.color.text_primary, theme))
                textSize = 16f
            }

            row.addView(labelView)
            row.addView(valueView)
            card.addView(row)
        }

        container.addView(card)
    }

    private fun updateStepper() {
        step1Circle.setBackgroundResource(if (currentStep >= 1) R.drawable.circle_primary else R.drawable.circle_gray)
        step2Circle.setBackgroundResource(if (currentStep >= 2) R.drawable.circle_primary else R.drawable.circle_gray)
        step3Circle.setBackgroundResource(if (currentStep >= 3) R.drawable.circle_primary else R.drawable.circle_gray)
        step4Circle.setBackgroundResource(if (currentStep >= 4) R.drawable.circle_primary else R.drawable.circle_gray)

        step1Circle.setTextColor(resources.getColor(if (currentStep >= 1) R.color.white else R.color.text_gray, theme))
        step2Circle.setTextColor(resources.getColor(if (currentStep >= 2) R.color.white else R.color.text_gray, theme))
        step3Circle.setTextColor(resources.getColor(if (currentStep >= 3) R.color.white else R.color.text_gray, theme))
        step4Circle.setTextColor(resources.getColor(if (currentStep >= 4) R.color.white else R.color.text_gray, theme))
    }

    private fun showLoading(show: Boolean) {
        progressLayout.visibility = if (show) View.VISIBLE else View.GONE
        contentFrame.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
