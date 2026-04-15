package com.example.simtec_mobileapp

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
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
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class GastosActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressLayout: LinearLayout
    private lateinit var tvTotalGastos: TextView
    private lateinit var tvPendientes: TextView
    private lateinit var tvAprobados: TextView
    private lateinit var fabAgregar: FloatingActionButton

    private var gastos: List<ApiClient.GastoRecord> = emptyList()
    private var currentTab = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gastos)

        initViews()
        setupToolbar()
        loadGastos()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        tabLayout = findViewById(R.id.tabLayout)
        recyclerView = findViewById(R.id.recyclerView)
        progressLayout = findViewById(R.id.progressLayout)
        tvTotalGastos = findViewById(R.id.tvTotalGastos)
        tvPendientes = findViewById(R.id.tvPendientes)
        tvAprobados = findViewById(R.id.tvAprobados)
        fabAgregar = findViewById(R.id.fabAgregar)

        tabLayout.addTab(tabLayout.newTab().setText("Todos"))
        tabLayout.addTab(tabLayout.newTab().setText("Pendientes"))
        tabLayout.addTab(tabLayout.newTab().setText("Aprobados"))

        recyclerView.layoutManager = LinearLayoutManager(this)

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTab = tab?.position ?: 0
                filterGastos()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        fabAgregar.setOnClickListener {
            showNuevoGastoDialog()
        }
    }

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
        
        etSubtotal.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val subtotal = s.toString().toDoubleOrNull() ?: 0.0
                val iva = etIva.text.toString().toDoubleOrNull() ?: 0.0
                etTotal.setText(String.format("%.2f", subtotal + iva))
            }
        })
        
        etIva.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val subtotal = etSubtotal.text.toString().toDoubleOrNull() ?: 0.0
                val iva = s.toString().toDoubleOrNull() ?: 0.0
                etTotal.setText(String.format("%.2f", subtotal + iva))
            }
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

        lifecycleScope.launch {
            val session = SessionManager(this@GastosActivity)
            val apiClient = ApiClient()
            apiClient.setAuthToken(session.getToken())

            val catalogos = apiClient.getGastosCatalogos()
            categorias = catalogos?.categorias ?: emptyList()
            proveedores = catalogos?.proveedores ?: emptyList()

            runOnUiThread {
                spinnerCategoria.adapter = ArrayAdapter(this@GastosActivity, android.R.layout.simple_spinner_dropdown_item, categorias.map { it.nombre })
                spinnerProveedor.adapter = ArrayAdapter(this@GastosActivity, android.R.layout.simple_spinner_dropdown_item, proveedores.map { it.nombre_comercial })
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

            val session = SessionManager(this)
            
            lifecycleScope.launch {
                val apiClient = ApiClient()
                apiClient.setAuthToken(session.getToken())

                val selectedCategoria = spinnerCategoria.selectedItemPosition
                val selectedProveedor = spinnerProveedor.selectedItemPosition

                val result = apiClient.solicitarGasto(
                    proveedor_id = if (selectedProveedor >= 0) proveedores[selectedProveedor].id else 1,
                    categoria_id = if (selectedCategoria >= 0) categorias[selectedCategoria].id else 1,
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
                    val success = result?.get("success") as? Boolean ?: false
                    if (success) {
                        showSuccess("Gasto solicitada")
                        dialog.dismiss()
                        loadGastos()
                    } else {
                        val msg = result?.get("message") as? String ?: "Error"
                        showError(msg)
                    }
                }
            }
        }

        dialog.show()
    }

    private fun setupToolbar() {
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun loadGastos() {
        showLoading(true)
        lifecycleScope.launch {
            val apiClient = ApiClient()
            apiClient.setAuthToken(SessionManager(this@GastosActivity).getToken())

            val result = apiClient.getGastosTablero()
            showLoading(false)

            if (result != null) {
                gastos = result
                updateKPIs()
                filterGastos()
            } else {
                showError("Error al cargar gastos")
            }
        }
    }

    private fun filterGastos() {
        val filtered = when (currentTab) {
            1 -> gastos.filter { it.estatus.equals("Pendiente", ignoreCase = true) }
            2 -> gastos.filter { it.estatus.equals("Aprobado", ignoreCase = true) }
            else -> gastos
        }

        recyclerView.adapter = GastosAdapter(filtered)
    }

    private fun updateKPIs() {
        val total = gastos.sumOf { it.total }
        val pendientes = gastos.count { it.estatus.equals("Pendiente", ignoreCase = true) }
        val aprobados = gastos.count { it.estatus.equals("Aprobado", ignoreCase = true) }

        tvTotalGastos.text = String.format("$%,.2f", total)
        tvPendientes.text = pendientes.toString()
        tvAprobados.text = aprobados.toString()
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

class GastosAdapter(private val gastos: List<ApiClient.GastoRecord>) : 
    RecyclerView.Adapter<GastosAdapter.ViewHolder>() {

    class ViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view) {
        val tvFolio: TextView = view.findViewById(R.id.tvFolio)
        val tvConcepto: TextView = view.findViewById(R.id.tvConcepto)
        val tvProveedor: TextView = view.findViewById(R.id.tvProveedor)
        val tvCategoria: TextView = view.findViewById(R.id.tvCategoria)
        val tvSolicitante: TextView = view.findViewById(R.id.tvSolicitante)
        val tvTotal: TextView = view.findViewById(R.id.tvTotal)
        val tvEstatus: TextView = view.findViewById(R.id.tvEstatus)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gasto, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val gasto = gastos[position]
        holder.tvFolio.text = gasto.folio
        holder.tvConcepto.text = gasto.concepto
        holder.tvProveedor.text = gasto.proveedor ?: "Sin proveedor"
        holder.tvCategoria.text = gasto.categoria ?: "Sin categoría"
        holder.tvSolicitante.text = "Por: ${gasto.solicitante ?: "N/A"}"
        holder.tvTotal.text = String.format("$%,.2f", gasto.total)
        holder.tvEstatus.text = gasto.estatus

        val bgRes = when (gasto.estatus.lowercase()) {
            "aprobado" -> R.drawable.bg_status_success
            "rechazado" -> R.drawable.bg_status_error
            else -> R.drawable.bg_status_pending
        }
        holder.tvEstatus.setBackgroundResource(bgRes)
    }

    override fun getItemCount() = gastos.size
}
