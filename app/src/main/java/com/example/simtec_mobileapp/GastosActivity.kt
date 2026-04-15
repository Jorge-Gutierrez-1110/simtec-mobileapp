package com.example.simtec_mobileapp

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class GastosActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "GastosActivity"
    }

    // Views
    private lateinit var toolbar: MaterialToolbar
    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressLayout: LinearLayout
    private lateinit var emptyLayout: LinearLayout
    private lateinit var tvEmptyTitle: TextView
    private lateinit var tvEmptySubtitle: TextView
    private lateinit var fabAgregar: FloatingActionButton

    // KPIs
    private lateinit var tvGastoEjercido: TextView
    private lateinit var tvPagado: TextView
    private lateinit var tvPorPagar: TextView
    private lateinit var tvPorAutorizar: TextView

    // Date filter
    private lateinit var tvFechaInicio: TextView
    private lateinit var tvFechaFin: TextView

    // Data
    private var allGastos: List<ApiClient.GastoRecord> = emptyList()
    private var filteredByDate: List<ApiClient.GastoRecord> = emptyList()
    private var currentTab = 0
    private lateinit var adapter: GastosAdapter

    // Date range (default: first day of previous month → today)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd MMM yyyy", Locale("es", "MX"))
    private lateinit var fechaInicio: Calendar
    private lateinit var fechaFin: Calendar

    // Tabs config
    private val tabNames = listOf("Todos", "Pendientes", "Aprobados", "Pagados", "Rechazados")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gastos)

        initDateRange()
        initViews()
        setupToolbar()
        setupTabs()
        setupDateFilter()
        setupSwipeRefresh()
        loadGastos()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        tabLayout = findViewById(R.id.tabLayout)
        recyclerView = findViewById(R.id.recyclerView)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        progressLayout = findViewById(R.id.progressLayout)
        emptyLayout = findViewById(R.id.emptyLayout)
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle)
        tvEmptySubtitle = findViewById(R.id.tvEmptySubtitle)
        fabAgregar = findViewById(R.id.fabAgregar)

        // KPIs
        tvGastoEjercido = findViewById(R.id.tvGastoEjercido)
        tvPagado = findViewById(R.id.tvPagado)
        tvPorPagar = findViewById(R.id.tvPorPagar)
        tvPorAutorizar = findViewById(R.id.tvPorAutorizar)

        // Date filter
        tvFechaInicio = findViewById(R.id.tvFechaInicio)
        tvFechaFin = findViewById(R.id.tvFechaFin)

        // RecyclerView
        adapter = GastosAdapter(emptyList()) { gasto -> showGastoDetail(gasto) }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        fabAgregar.setOnClickListener { showNuevoGastoDialog() }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupTabs() {
        tabNames.forEach { name ->
            tabLayout.addTab(tabLayout.newTab().setText(name))
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTab = tab?.position ?: 0
                filterAndDisplay()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun initDateRange() {
        // Default: first day of previous month → today (same as web frontend)
        fechaFin = Calendar.getInstance()

        fechaInicio = Calendar.getInstance()
        fechaInicio.add(Calendar.MONTH, -1)
        fechaInicio.set(Calendar.DAY_OF_MONTH, 1)
    }

    private fun setupDateFilter() {
        tvFechaInicio.text = displayDateFormat.format(fechaInicio.time)
        tvFechaFin.text = displayDateFormat.format(fechaFin.time)

        tvFechaInicio.setOnClickListener {
            DatePickerDialog(this, { _, year, month, day ->
                fechaInicio.set(year, month, day)
                tvFechaInicio.text = displayDateFormat.format(fechaInicio.time)
                applyDateFilterAndRefresh()
            }, fechaInicio.get(Calendar.YEAR), fechaInicio.get(Calendar.MONTH), fechaInicio.get(Calendar.DAY_OF_MONTH)).show()
        }

        tvFechaFin.setOnClickListener {
            DatePickerDialog(this, { _, year, month, day ->
                fechaFin.set(year, month, day)
                tvFechaFin.text = displayDateFormat.format(fechaFin.time)
                applyDateFilterAndRefresh()
            }, fechaFin.get(Calendar.YEAR), fechaFin.get(Calendar.MONTH), fechaFin.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    private fun applyDateFilterAndRefresh() {
        applyDateFilter()
        updateKPIs()
        filterAndDisplay()
    }

    private fun applyDateFilter() {
        val startStr = dateFormat.format(fechaInicio.time)
        val endStr = dateFormat.format(fechaFin.time)

        filteredByDate = allGastos.filter { gasto ->
            val fechaEmision = gasto.fecha_emision ?: return@filter false
            // Extract YYYY-MM-DD from the date string
            val fechaClean = if (fechaEmision.contains("T")) fechaEmision.substring(0, 10) else fechaEmision.take(10)
            fechaClean >= startStr && fechaClean <= endStr
        }

        Log.d(TAG, "Filtro fechas: $startStr → $endStr | ${allGastos.size} total → ${filteredByDate.size} en rango")
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setColorSchemeResources(R.color.primary)
        swipeRefresh.setOnRefreshListener { loadGastos() }
    }

    // ==================== DATA LOADING ====================

    private fun loadGastos() {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val apiClient = ApiClient()
                apiClient.setAuthToken(SessionManager(this@GastosActivity).getToken())

                val result = apiClient.getGastosTablero()

                runOnUiThread {
                    swipeRefresh.isRefreshing = false
                    showLoading(false)

                    if (result != null) {
                        allGastos = result
                        Log.d(TAG, "Gastos cargados: ${allGastos.size}")
                        applyDateFilter()
                        updateKPIs()
                        filterAndDisplay()
                    } else {
                        showError("Error al cargar gastos")
                        showEmptyState("Error de conexion", "No se pudieron cargar los gastos. Desliza hacia abajo para reintentar.")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading gastos: ${e.message}", e)
                runOnUiThread {
                    swipeRefresh.isRefreshing = false
                    showLoading(false)
                    showError("Error: ${e.message}")
                    showEmptyState("Error", "Ocurrio un error inesperado.")
                }
            }
        }
    }

    // ==================== KPI CALCULATIONS ====================

    private fun updateKPIs() {
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "MX"))

        // KPIs calculated on date-filtered data (same as web)
        // Gasto Ejercido = sum total of Aprobado + Pagado + Pagado en Nomina
        val gastoEjercido = filteredByDate
            .filter { it.estatus.lowercase() in listOf("aprobado", "pagado", "pagado en nomina", "pagado en nómina") }
            .sumOf { it.total }

        // Pagado = sum total of Pagado + Pagado en Nomina
        val pagado = filteredByDate
            .filter { it.estatus.lowercase() in listOf("pagado", "pagado en nomina", "pagado en nómina") }
            .sumOf { it.total }

        // Por Pagar = sum total of Aprobado
        val porPagar = filteredByDate
            .filter { it.estatus.equals("Aprobado", ignoreCase = true) }
            .sumOf { it.total }

        // Por Autorizar = count of Pendiente
        val porAutorizar = filteredByDate
            .count { it.estatus.equals("Pendiente", ignoreCase = true) }

        tvGastoEjercido.text = currencyFormat.format(gastoEjercido)
        tvPagado.text = currencyFormat.format(pagado)
        tvPorPagar.text = currencyFormat.format(porPagar)
        tvPorAutorizar.text = porAutorizar.toString()
    }

    // ==================== FILTERING ====================

    private fun filterAndDisplay() {
        // Filter by tab status on top of date-filtered data
        val filtered = when (currentTab) {
            0 -> filteredByDate // Todos
            1 -> filteredByDate.filter { it.estatus.equals("Pendiente", ignoreCase = true) }
            2 -> filteredByDate.filter { it.estatus.equals("Aprobado", ignoreCase = true) }
            3 -> filteredByDate.filter { it.estatus.lowercase() in listOf("pagado", "pagado en nomina", "pagado en nómina") }
            4 -> filteredByDate.filter { it.estatus.equals("Rechazado", ignoreCase = true) }
            else -> filteredByDate
        }

        adapter.updateData(filtered)

        if (filtered.isEmpty() && filteredByDate.isNotEmpty()) {
            val tabName = tabNames.getOrElse(currentTab) { "esta categoria" }
            showEmptyState("Sin gastos $tabName", "No hay gastos con este estatus en el periodo seleccionado")
        } else if (filtered.isEmpty()) {
            showEmptyState("Sin gastos", "No hay gastos en el periodo seleccionado. Ajusta las fechas o toca + para agregar uno.")
        } else {
            emptyLayout.visibility = View.GONE
        }
    }

    // ==================== BOTTOM SHEET DETAIL ====================

    private fun showGastoDetail(gasto: ApiClient.GastoRecord) {
        val dialog = BottomSheetDialog(this, com.google.android.material.R.style.Theme_Design_Light_BottomSheetDialog)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_gasto_detalle, null)
        dialog.setContentView(view)

        // Folio + Estatus
        val tvDetFolio = view.findViewById<TextView>(R.id.tvDetFolio)
        val tvDetEstatus = view.findViewById<TextView>(R.id.tvDetEstatus)
        tvDetFolio.text = gasto.folio ?: "Sin folio"
        tvDetEstatus.text = gasto.estatus
        tvDetEstatus.setBackgroundResource(getStatusBackground(gasto.estatus))

        // Concepto
        view.findViewById<TextView>(R.id.tvDetConcepto).text = gasto.concepto

        // Info rows
        view.findViewById<TextView>(R.id.tvDetProveedor).text = gasto.proveedor ?: "Sin proveedor"
        view.findViewById<TextView>(R.id.tvDetCategoria).text = gasto.categoria ?: "Sin categoria"
        view.findViewById<TextView>(R.id.tvDetSolicitante).text = gasto.solicitante ?: "N/A"

        // Cliente (show only if present)
        val rowCliente = view.findViewById<LinearLayout>(R.id.rowCliente)
        if (!gasto.cliente_nombre.isNullOrEmpty()) {
            rowCliente.visibility = View.VISIBLE
            view.findViewById<TextView>(R.id.tvDetCliente).text = gasto.cliente_nombre
        }

        // Fechas
        view.findViewById<TextView>(R.id.tvDetFechaEmision).text = formatDate(gasto.fecha_emision)

        val rowVencimiento = view.findViewById<LinearLayout>(R.id.rowVencimiento)
        if (!gasto.fecha_vencimiento.isNullOrEmpty()) {
            rowVencimiento.visibility = View.VISIBLE
            view.findViewById<TextView>(R.id.tvDetFechaVencimiento).text = formatDate(gasto.fecha_vencimiento)
        }

        // Aprobador (show if present)
        val rowAprobador = view.findViewById<LinearLayout>(R.id.rowAprobador)
        if (!gasto.aprobador.isNullOrEmpty()) {
            rowAprobador.visibility = View.VISIBLE
            view.findViewById<TextView>(R.id.tvDetAprobador).text = gasto.aprobador
        }

        // Motivo rechazo (show if rejected)
        val rowRechazo = view.findViewById<LinearLayout>(R.id.rowRechazo)
        if (gasto.estatus.equals("Rechazado", ignoreCase = true) && !gasto.motivo_rechazo.isNullOrEmpty()) {
            rowRechazo.visibility = View.VISIBLE
            view.findViewById<TextView>(R.id.tvDetMotivoRechazo).text = gasto.motivo_rechazo
        }

        // Montos
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
        view.findViewById<TextView>(R.id.tvDetSubtotal).text = currencyFormat.format(gasto.subtotal)
        view.findViewById<TextView>(R.id.tvDetIva).text = currencyFormat.format(gasto.iva)

        // Retenciones (show only if > 0)
        val rowRetenciones = view.findViewById<LinearLayout>(R.id.rowRetenciones)
        if (gasto.retenciones > 0) {
            rowRetenciones.visibility = View.VISIBLE
            view.findViewById<TextView>(R.id.tvDetRetenciones).text = "-${currencyFormat.format(gasto.retenciones)}"
        }

        view.findViewById<TextView>(R.id.tvDetTotal).text = currencyFormat.format(gasto.total)

        dialog.show()
    }

    // ==================== NUEVO GASTO DIALOG ====================

    private fun showNuevoGastoDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_nuevo_gasto, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val spinnerProveedor = dialogView.findViewById<Spinner>(R.id.spinnerProveedor)
        val spinnerCategoria = dialogView.findViewById<Spinner>(R.id.spinnerCategoria)
        val etConcepto = dialogView.findViewById<EditText>(R.id.etConcepto)
        val etSubtotal = dialogView.findViewById<EditText>(R.id.etSubtotal)
        val etIva = dialogView.findViewById<EditText>(R.id.etIva)
        val etTotal = dialogView.findViewById<EditText>(R.id.etTotal)
        val etFechaEmision = dialogView.findViewById<EditText>(R.id.etFechaEmision)
        val etFechaVencimiento = dialogView.findViewById<EditText>(R.id.etFechaVencimiento)
        val btnEnviar = dialogView.findViewById<MaterialButton>(R.id.btnEnviar)
        val btnCerrar = dialogView.findViewById<ImageButton>(R.id.btnCerrar)

        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        etFechaEmision.setText(dateFormat.format(calendar.time))

        // Auto-calc total
        val calcTotal = {
            val subtotal = etSubtotal.text.toString().toDoubleOrNull() ?: 0.0
            val iva = etIva.text.toString().toDoubleOrNull() ?: 0.0
            etTotal.setText(String.format("%.2f", subtotal + iva))
        }

        etSubtotal.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) { calcTotal() }
        })

        etIva.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) { calcTotal() }
        })

        etFechaEmision.setOnClickListener {
            DatePickerDialog(this, { _, year, month, day ->
                calendar.set(year, month, day)
                etFechaEmision.setText(dateFormat.format(calendar.time))
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        etFechaVencimiento.setOnClickListener {
            DatePickerDialog(this, { _, year, month, day ->
                calendar.set(year, month, day)
                etFechaVencimiento.setText(dateFormat.format(calendar.time))
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        var categorias: List<ApiClient.CatGasto> = emptyList()
        var proveedores: List<ApiClient.Proveedor> = emptyList()

        // Load catalogos
        lifecycleScope.launch {
            val session = SessionManager(this@GastosActivity)
            val apiClient = ApiClient()
            apiClient.setAuthToken(session.getToken())

            val catalogos = apiClient.getGastosCatalogos()
            categorias = catalogos?.categorias ?: emptyList()
            proveedores = catalogos?.proveedores ?: emptyList()

            runOnUiThread {
                spinnerCategoria.adapter = ArrayAdapter(
                    this@GastosActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    categorias.map { it.nombre }
                )
                spinnerProveedor.adapter = ArrayAdapter(
                    this@GastosActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    proveedores.map { it.nombre_comercial }
                )
            }
        }

        btnCerrar.setOnClickListener { dialog.dismiss() }

        btnEnviar.setOnClickListener {
            val concepto = etConcepto.text.toString()
            val subtotal = etSubtotal.text.toString().toDoubleOrNull() ?: 0.0
            val iva = etIva.text.toString().toDoubleOrNull() ?: 0.0
            val total = etTotal.text.toString().toDoubleOrNull() ?: 0.0
            val fechaEmision = etFechaEmision.text.toString()
            val fechaVencimiento = etFechaVencimiento.text.toString()

            if (concepto.isEmpty() || total <= 0) {
                showError("Completa los campos requeridos")
                return@setOnClickListener
            }

            btnEnviar.isEnabled = false
            btnEnviar.text = "Enviando..."

            val session = SessionManager(this)

            lifecycleScope.launch {
                val apiClient = ApiClient()
                apiClient.setAuthToken(session.getToken())

                val selectedCategoria = spinnerCategoria.selectedItemPosition
                val selectedProveedor = spinnerProveedor.selectedItemPosition

                val result = apiClient.solicitarGasto(
                    proveedor_id = if (selectedProveedor >= 0 && proveedores.isNotEmpty()) proveedores[selectedProveedor].id else 1,
                    categoria_id = if (selectedCategoria >= 0 && categorias.isNotEmpty()) categorias[selectedCategoria].id else 1,
                    solicitante_id = session.getEmpleadoId(),
                    beneficiario_id = null,
                    concepto = concepto,
                    subtotal = subtotal,
                    iva = iva,
                    retenciones = 0.0,
                    total = total,
                    fecha_emision = fechaEmision,
                    fecha_vencimiento = if (fechaVencimiento.isNotEmpty()) fechaVencimiento else null,
                    cliente_id = null
                )

                runOnUiThread {
                    btnEnviar.isEnabled = true
                    btnEnviar.text = "Enviar Solicitud"

                    val success = result?.get("success") as? Boolean ?: false
                    if (success) {
                        showSuccess("Gasto registrado correctamente")
                        dialog.dismiss()
                        loadGastos()
                    } else {
                        val msg = result?.get("message") as? String ?: "Error al registrar gasto"
                        showError(msg)
                    }
                }
            }
        }

        dialog.show()
    }

    // ==================== HELPERS ====================

    private fun getStatusBackground(estatus: String): Int {
        return when (estatus.lowercase()) {
            "pendiente" -> R.drawable.bg_status_pending
            "aprobado" -> R.drawable.bg_status_approved
            "pagado" -> R.drawable.bg_status_paid
            "pagado en nomina", "pagado en nómina" -> R.drawable.bg_status_payroll
            "rechazado" -> R.drawable.bg_status_error
            else -> R.drawable.bg_status_pending
        }
    }

    private fun formatDate(dateStr: String?): String {
        if (dateStr.isNullOrEmpty()) return "-"
        return try {
            // Try ISO format first (2026-04-15T00:00:00.000Z)
            val inputFormat = if (dateStr.contains("T")) {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            } else {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            }
            val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale("es", "MX"))
            val date = inputFormat.parse(dateStr)
            if (date != null) outputFormat.format(date) else dateStr
        } catch (e: Exception) {
            dateStr.take(10) // Fallback: show raw date trimmed
        }
    }

    private fun showLoading(show: Boolean) {
        progressLayout.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            recyclerView.visibility = View.GONE
            emptyLayout.visibility = View.GONE
        } else {
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun showEmptyState(title: String, subtitle: String) {
        emptyLayout.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        tvEmptyTitle.text = title
        tvEmptySubtitle.text = subtitle
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

// ==================== ADAPTER ====================

class GastosAdapter(
    private var gastos: List<ApiClient.GastoRecord>,
    private val onItemClick: (ApiClient.GastoRecord) -> Unit
) : RecyclerView.Adapter<GastosAdapter.ViewHolder>() {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
    private val dateInputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    private val dateInputFormatShort = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dateOutputFormat = SimpleDateFormat("dd MMM yyyy", Locale("es", "MX"))

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvFolio: TextView = view.findViewById(R.id.tvFolio)
        val tvEstatus: TextView = view.findViewById(R.id.tvEstatus)
        val tvConcepto: TextView = view.findViewById(R.id.tvConcepto)
        val tvProveedor: TextView = view.findViewById(R.id.tvProveedor)
        val tvCategoria: TextView = view.findViewById(R.id.tvCategoria)
        val tvSolicitante: TextView = view.findViewById(R.id.tvSolicitante)
        val tvFecha: TextView = view.findViewById(R.id.tvFecha)
        val tvSubtotal: TextView = view.findViewById(R.id.tvSubtotal)
        val tvIva: TextView = view.findViewById(R.id.tvIva)
        val tvTotal: TextView = view.findViewById(R.id.tvTotal)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gasto, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val gasto = gastos[position]

        holder.tvFolio.text = gasto.folio ?: "Sin folio"
        holder.tvConcepto.text = gasto.concepto
        holder.tvProveedor.text = gasto.proveedor ?: "Sin proveedor"
        holder.tvCategoria.text = gasto.categoria ?: "General"
        holder.tvSolicitante.text = "Por: ${gasto.solicitante ?: "N/A"}"
        holder.tvFecha.text = formatDate(gasto.fecha_emision)
        holder.tvSubtotal.text = currencyFormat.format(gasto.subtotal)
        holder.tvIva.text = currencyFormat.format(gasto.iva)
        holder.tvTotal.text = currencyFormat.format(gasto.total)

        // Status badge
        holder.tvEstatus.text = gasto.estatus
        holder.tvEstatus.setBackgroundResource(getStatusBg(gasto.estatus))

        // Click listener
        holder.itemView.setOnClickListener { onItemClick(gasto) }
    }

    override fun getItemCount() = gastos.size

    fun updateData(newGastos: List<ApiClient.GastoRecord>) {
        gastos = newGastos
        notifyDataSetChanged()
    }

    private fun getStatusBg(estatus: String): Int {
        return when (estatus.lowercase()) {
            "pendiente" -> R.drawable.bg_status_pending
            "aprobado" -> R.drawable.bg_status_approved
            "pagado" -> R.drawable.bg_status_paid
            "pagado en nomina", "pagado en nómina" -> R.drawable.bg_status_payroll
            "rechazado" -> R.drawable.bg_status_error
            else -> R.drawable.bg_status_pending
        }
    }

    private fun formatDate(dateStr: String?): String {
        if (dateStr.isNullOrEmpty()) return "-"
        return try {
            val inputFormat = if (dateStr.contains("T")) dateInputFormat else dateInputFormatShort
            val date = inputFormat.parse(dateStr)
            if (date != null) dateOutputFormat.format(date) else dateStr.take(10)
        } catch (e: Exception) {
            dateStr.take(10)
        }
    }
}
