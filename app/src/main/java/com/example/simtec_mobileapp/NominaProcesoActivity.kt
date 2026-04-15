package com.example.simtec_mobileapp

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class NominaProcesoActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var stepperLayout: LinearLayout
    private lateinit var contentFrame: FrameLayout
    private lateinit var progressLayout: LinearLayout
    private lateinit var step1Circle: TextView
    private lateinit var step2Circle: TextView
    private lateinit var step3Circle: TextView
    private lateinit var step4Circle: TextView
    private lateinit var step1Line: View
    private lateinit var step2Line: View
    private lateinit var step3Line: View

    private var currentStep = 1
    private var periodoSeleccionado: Periodo? = null
    private var puedeAvanzar = false
    private var datosNomina: List<EmpleadoNomina> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nomina_proceso)

        initViews()
        setupToolbar()
        loadPeriodos()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        stepperLayout = findViewById(R.id.stepperLayout)
        contentFrame = findViewById(R.id.contentFrame)
        progressLayout = findViewById(R.id.progressLayout)
        step1Circle = findViewById(R.id.step1Circle)
        step2Circle = findViewById(R.id.step2Circle)
        step3Circle = findViewById(R.id.step3Circle)
        step4Circle = findViewById(R.id.step4Circle)
        step1Line = findViewById(R.id.step1Line)
        step2Line = findViewById(R.id.step2Line)
        step3Line = findViewById(R.id.step3Line)
    }

    private fun setupToolbar() {
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert)
        toolbar.setNavigationOnClickListener {
            when (currentStep) {
                1 -> finish()
                2 -> goToStep(1)
                3 -> goToStep(2)
                4 -> goToStep(3)
            }
        }
    }

    private fun loadPeriodos() {
        showLoading(true)
        lifecycleScope.launch {
            val apiClient = ApiClient()
            apiClient.setAuthToken(SessionManager(this@NominaProcesoActivity).getToken())

            val periodosResult = apiClient.getPeriodos()
            showLoading(false)

            if (periodosResult != null && periodosResult.isNotEmpty()) {
                renderStep1(periodosResult)
            } else {
                showError("No hay períodos disponibles")
            }
        }
    }

    private fun renderStep1(periodos: List<Periodo>) {
        contentFrame.removeAllViews()

        val scrollView = ScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setPadding(32, 32, 32, 32)
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val title = TextView(this).apply {
            text = "Paso 1: Seleccionar Período"
            textSize = 20f
            setTextColor(getColor(R.color.text_primary))
            setPadding(0, 0, 0, 32)
        }
        container.addView(title)

        periodos.forEach { periodo ->
            val card = layoutInflater.inflate(R.layout.item_periodo, container, true)
            val nombreView = card.findViewById<TextView>(R.id.periodoNombre)
            val fechasView = card.findViewById<TextView>(R.id.periodoFechas)
            val statusView = card.findViewById<TextView>(R.id.periodoStatus)

            nombreView.text = periodo.nombre
            fechasView.text = "${periodo.fecha_inicio} - ${periodo.fecha_fin}"
            statusView.text = periodo.status_cierre

            card.setOnClickListener {
                periodoSeleccionado = periodo
                if (periodo.status_cierre == "Cerrado") {
                    puedeAvanzar = true
                }
                goToStep(2)
            }

            container.addView(card)
        }

        scrollView.addView(container)
        contentFrame.addView(scrollView)
        updateStepper()
    }

    private fun goToStep(targetStep: Int) {
        if (targetStep < currentStep) {
            currentStep = targetStep
            renderCurrentStep()
            return
        }

        if (periodoSeleccionado?.status_cierre == "Cerrado") {
            currentStep = targetStep
            renderCurrentStep()
            return
        }

        if (targetStep > 2 && !puedeAvanzar) {
            showAlert("Auditoría Incompleta", "No puedes avanzar hasta que todos los empleados estén auditados.")
            return
        }

        currentStep = targetStep
        renderCurrentStep()
    }

    private fun renderCurrentStep() {
        when (currentStep) {
            1 -> loadPeriodos()
            2 -> renderStep2()
            3 -> renderStep3()
            4 -> renderStep4()
        }
        updateStepper()
    }

    private fun renderStep2() {
        contentFrame.removeAllViews()

        if (periodoSeleccionado == null) {
            showError("Selecciona un período primero")
            goToStep(1)
            return
        }

        showLoading(true)
        lifecycleScope.launch {
            val apiClient = ApiClient()
            apiClient.setAuthToken(SessionManager(this@NominaProcesoActivity).getToken())

            val result = apiClient.preCalculoNomina(periodoSeleccionado!!.id)
            showLoading(false)

            if (result?.success == true) {
                val nominaId = result.nomina_id
                if (nominaId != null) {
                    val detalles: List<EmpleadoNomina>? = apiClient.getNominaDetalle(nominaId)
                    if (detalles != null) {
                        datosNomina = detalles
                        puedeAvanzar = true
                        renderPrenominaPanel(detalles)
                    } else {
                        showError("Error al cargar detalles")
                    }
                }
            } else {
                val msg = result?.message ?: "Error"
                showError(msg)
                puedeAvanzar = false
                renderPrenominaPanel(emptyList())
            }
        }
    }

    private fun renderPrenominaPanel(detalles: List<EmpleadoNomina>) {
        contentFrame.removeAllViews()

        val scrollView = ScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setPadding(32, 32, 32, 32)
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val title = TextView(this).apply {
            text = "Paso 2: Prenómina - Validar Asistencias"
            textSize = 20f
            setTextColor(getColor(R.color.text_primary))
            setPadding(0, 0, 0, 32)
        }
        container.addView(title)

        var totalPercepciones = 0.0
        var totalDeducciones = 0.0
        var totalNeto = 0.0

        detalles.forEach { emp ->
            totalPercepciones += emp.totalPercepciones
            totalDeducciones += emp.totalDeducciones
            totalNeto += emp.neto

            val card = createEmpleadoCard(emp)
            container.addView(card)
        }

        val totalesCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(getColor(R.color.white))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 24
            }
        }

        totalesCard.addView(createRow("Total Percepciones", String.format("$%,.2f", totalPercepciones)))
        totalesCard.addView(createRow("Total Deducciones", String.format("$%,.2f", totalDeducciones)))
        totalesCard.addView(createRow("TOTAL NETO", String.format("$%,.2f", totalNeto)))

        container.addView(totalesCard)

        val btnNavigate = MaterialButton(this).apply {
            text = if (periodoSeleccionado?.status_cierre == "Cerrado") "Ver Resumen" else "Generar Nómina"
            setOnClickListener { goToStep(3) }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 32
            }
        }
        container.addView(btnNavigate)

        scrollView.addView(container)
        contentFrame.addView(scrollView)
        updateStepper()
    }

    private fun renderStep3() {
        contentFrame.removeAllViews()

        val scrollView = ScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setPadding(32, 32, 32, 32)
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val title = TextView(this).apply {
            text = "Paso 3: Resumen de Nómina"
            textSize = 20f
            setTextColor(getColor(R.color.text_primary))
            setPadding(0, 0, 0, 32)
        }
        container.addView(title)

        var total = 0.0
        datosNomina.forEach { total += it.neto }

        val summaryCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(getColor(R.color.white))
        }

        summaryCard.addView(createRow("Empleados", datosNomina.size.toString()))
        summaryCard.addView(createRow("Total Neto", String.format("$%,.2f", total)))

        container.addView(summaryCard)

        val btnCierre = MaterialButton(this).apply {
            text = if (periodoSeleccionado?.status_cierre == "Cerrado") "Ver Historial de Cierre" else "Ir al Cierre Definitivo"
            setOnClickListener { goToStep(4) }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 32
            }
        }
        container.addView(btnCierre)

        scrollView.addView(container)
        contentFrame.addView(scrollView)
        updateStepper()
    }

    private fun renderStep4() {
        contentFrame.removeAllViews()

        showLoading(true)
        lifecycleScope.launch {
            val apiClient = ApiClient()
            apiClient.setAuthToken(SessionManager(this@NominaProcesoActivity).getToken())

            val request = NominaCierreRequest(
                periodo_id = periodoSeleccionado?.id ?: 0,
                resumen = NominaResumen(
                    total_empleados = datosNomina.size,
                    total_percepciones = datosNomina.sumOf { it.totalPercepciones },
                    total_deducciones = datosNomina.sumOf { it.totalDeducciones },
                    total_neto = datosNomina.sumOf { it.neto }
                ),
                detalles = emptyList()
            )

            val result = apiClient.cerrarNominaAsMap(request)
            showLoading(false)

            val scrollView = ScrollView(this@NominaProcesoActivity).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                setPadding(32, 32, 32, 32)
            }

            val container = LinearLayout(this@NominaProcesoActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val title = TextView(this@NominaProcesoActivity).apply {
                text = "Paso 4: Cierre de Nómina"
                textSize = 20f
                setTextColor(getColor(R.color.text_primary))
                setPadding(0, 0, 0, 32)
            }
            container.addView(title)

            if (result?.get("success") == true) {
                val successMsg = TextView(this@NominaProcesoActivity).apply {
                    text = "Nómina cerrada correctamente"
                    textSize = 18f
                    setTextColor(getColor(android.R.color.holo_green_dark))
                    setPadding(0, 0, 0, 24)
                }
                container.addView(successMsg)
            } else {
                val msg = result?.get("message") as? String ?: "Error al cerrar nómina"
                showError(msg)
            }

            val btnVolver = MaterialButton(this@NominaProcesoActivity).apply {
                text = "Volver"
                setOnClickListener { goToStep(3) }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 24
                }
            }
            container.addView(btnVolver)

            scrollView.addView(container)
            contentFrame.addView(scrollView)
            updateStepper()
        }
    }

    private fun createEmpleadoCard(emp: EmpleadoNomina): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(getColor(R.color.white))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
        }

        card.addView(createRow("Nombre", "${emp.nombre} ${emp.apellido_paterno}"))
        card.addView(createRow("No. Empleado", emp.numero_empleado))
        card.addView(createRow("Percepciones", String.format("$%,.2f", emp.totalPercepciones)))
        card.addView(createRow("Deducciones", String.format("$%,.2f", emp.totalDeducciones)))
        card.addView(createRow("Neto", String.format("$%,.2f", emp.neto)))

        return card
    }

    private fun createRow(label: String, value: String): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 8, 0, 8)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val labelView = TextView(this).apply {
            text = label
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setTextColor(getColor(R.color.text_secondary))
        }

        val valueView = TextView(this).apply {
            text = value
            setTextColor(getColor(R.color.text_primary))
            textSize = 16f
        }

        row.addView(labelView)
        row.addView(valueView)
        return row
    }

    private fun updateStepper() {
        step1Circle.setBackgroundResource(if (currentStep >= 1) R.drawable.circle_primary else R.drawable.circle_gray)
        step2Circle.setBackgroundResource(if (currentStep >= 2) R.drawable.circle_primary else R.drawable.circle_gray)
        step3Circle.setBackgroundResource(if (currentStep >= 3) R.drawable.circle_primary else R.drawable.circle_gray)
        step4Circle.setBackgroundResource(if (currentStep >= 4) R.drawable.circle_primary else R.drawable.circle_gray)

        step1Line.setBackgroundColor(if (currentStep > 1) getColor(R.color.primary) else getColor(R.color.text_gray))
        step2Line.setBackgroundColor(if (currentStep > 2) getColor(R.color.primary) else getColor(R.color.text_gray))
        step3Line.setBackgroundColor(if (currentStep > 3) getColor(R.color.primary) else getColor(R.color.text_gray))
    }

    private fun showLoading(show: Boolean) {
        progressLayout.visibility = if (show) View.VISIBLE else View.GONE
        contentFrame.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showAlert(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Entendido", null)
            .show()
    }
}