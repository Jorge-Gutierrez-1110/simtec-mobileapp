package com.example.simtec_mobileapp

import android.content.ContentValues
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class MisRecibosActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MisRecibosActivity"
    }

    private lateinit var toolbar: MaterialToolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressLayout: LinearLayout
    private lateinit var emptyLayout: LinearLayout
    private lateinit var tvNombreEmpleado: TextView
    private lateinit var tvInfoEmpleado: TextView

    private lateinit var adapter: RecibosAdapter
    private var recibos: List<ReciboNomina> = emptyList()

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "MX"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mis_recibos)

        initViews()
        setupToolbar()
        loadRecibos()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        recyclerView = findViewById(R.id.recyclerView)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        progressLayout = findViewById(R.id.progressLayout)
        emptyLayout = findViewById(R.id.emptyLayout)
        tvNombreEmpleado = findViewById(R.id.tvNombreEmpleado)
        tvInfoEmpleado = findViewById(R.id.tvInfoEmpleado)

        val session = SessionManager(this)
        tvNombreEmpleado.text = session.getUserName()
        tvInfoEmpleado.text = "Recibos de nomina disponibles"

        adapter = RecibosAdapter(emptyList()) { recibo -> showReciboDetail(recibo) }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        swipeRefresh.setColorSchemeResources(R.color.primary)
        swipeRefresh.setOnRefreshListener { loadRecibos() }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun loadRecibos() {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val session = SessionManager(this@MisRecibosActivity)
                val apiClient = ApiClient()
                apiClient.setAuthToken(session.getToken())

                val empleadoId = session.getEmpleadoId()
                Log.d(TAG, "Cargando recibos para empleado_id=$empleadoId")

                val result = apiClient.getMisRecibos(empleadoId)

                runOnUiThread {
                    swipeRefresh.isRefreshing = false
                    showLoading(false)

                    if (result != null) {
                        recibos = result
                        tvInfoEmpleado.text = "${recibos.size} recibo${if (recibos.size != 1) "s" else ""} disponible${if (recibos.size != 1) "s" else ""}"
                        adapter.updateData(recibos)

                        if (recibos.isEmpty()) {
                            emptyLayout.visibility = View.VISIBLE
                            recyclerView.visibility = View.GONE
                        } else {
                            emptyLayout.visibility = View.GONE
                            recyclerView.visibility = View.VISIBLE
                        }
                    } else {
                        Toast.makeText(this@MisRecibosActivity, "Error al cargar recibos", Toast.LENGTH_LONG).show()
                        emptyLayout.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error: ${e.message}", e)
                runOnUiThread {
                    swipeRefresh.isRefreshing = false
                    showLoading(false)
                    Toast.makeText(this@MisRecibosActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // ==================== DETALLE BOTTOM SHEET ====================

    private fun showReciboDetail(recibo: ReciboNomina) {
        val dialog = BottomSheetDialog(this, com.google.android.material.R.style.Theme_Design_Light_BottomSheetDialog)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_recibo_detalle, null)
        dialog.setContentView(view)

        // Header
        view.findViewById<TextView>(R.id.tvDetPeriodo).text = recibo.periodo_nombre
        view.findViewById<TextView>(R.id.tvDetRangoFechas).text =
            "${formatDate(recibo.periodo_inicio)} - ${formatDate(recibo.periodo_fin)} | ${recibo.periodo_tipo}"

        // Info basica
        view.findViewById<TextView>(R.id.tvDetDiasPagados).text = "${recibo.dias_pagados} dias"
        view.findViewById<TextView>(R.id.tvDetSalarioDiario).text = currencyFormat.format(recibo.salario_diario)

        // Percepciones
        val llPercepciones = view.findViewById<LinearLayout>(R.id.llPercepciones)
        llPercepciones.removeAllViews()
        for (p in recibo.percepciones_list) {
            val nombre = p.concepto.ifEmpty { p.nombre }
            addConceptoRow(llPercepciones, nombre, p.monto, "#16a34a")
        }
        view.findViewById<TextView>(R.id.tvDetTotalPercepciones).text = currencyFormat.format(recibo.total_percepciones)

        // Deducciones
        val llDeducciones = view.findViewById<LinearLayout>(R.id.llDeducciones)
        llDeducciones.removeAllViews()
        for (d in recibo.deducciones_list) {
            val nombre = d.concepto.ifEmpty { d.nombre }
            addConceptoRow(llDeducciones, nombre, d.monto, "#DC2626")
        }
        view.findViewById<TextView>(R.id.tvDetTotalDeducciones).text = "-${currencyFormat.format(recibo.total_deducciones)}"

        // Neto
        view.findViewById<TextView>(R.id.tvDetNeto).text = currencyFormat.format(recibo.neto)

        // Boton descargar PDF
        view.findViewById<MaterialButton>(R.id.btnDescargarPdf).setOnClickListener {
            generateAndSavePdf(recibo)
        }

        dialog.show()
    }

    private fun addConceptoRow(parent: LinearLayout, nombre: String, monto: Double, colorStr: String) {
        val row = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 6 }
            orientation = LinearLayout.HORIZONTAL
        }

        val tvNombre = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            text = nombre
            textSize = 13f
            setTextColor(Color.parseColor("#334155"))
        }

        val tvMonto = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            text = currencyFormat.format(monto)
            textSize = 13f
            setTextColor(Color.parseColor(colorStr))
        }

        row.addView(tvNombre)
        row.addView(tvMonto)
        parent.addView(row)
    }

    // ==================== PDF GENERATION ====================

    private fun generateAndSavePdf(recibo: ReciboNomina) {
        try {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            val paintTitle = Paint().apply {
                color = Color.parseColor("#0052cc")
                textSize = 20f
                isFakeBoldText = true
            }
            val paintSubtitle = Paint().apply {
                color = Color.parseColor("#334155")
                textSize = 14f
                isFakeBoldText = true
            }
            val paintNormal = Paint().apply {
                color = Color.parseColor("#334155")
                textSize = 12f
            }
            val paintLabel = Paint().apply {
                color = Color.parseColor("#94A3B8")
                textSize = 11f
            }
            val paintGreen = Paint().apply {
                color = Color.parseColor("#16a34a")
                textSize = 12f
                isFakeBoldText = true
            }
            val paintRed = Paint().apply {
                color = Color.parseColor("#DC2626")
                textSize = 12f
                isFakeBoldText = true
            }
            val paintLine = Paint().apply {
                color = Color.parseColor("#E2E8F0")
                strokeWidth = 1f
            }
            val paintBigNeto = Paint().apply {
                color = Color.parseColor("#0052cc")
                textSize = 18f
                isFakeBoldText = true
            }

            val marginLeft = 40f
            val marginRight = 555f
            var y = 50f

            // === HEADER ===
            canvas.drawText("RECIBO DE NOMINA", marginLeft, y, paintTitle)
            y += 30f
            canvas.drawText(recibo.periodo_nombre, marginLeft, y, paintSubtitle)
            y += 20f
            canvas.drawText(
                "${formatDate(recibo.periodo_inicio)} - ${formatDate(recibo.periodo_fin)} | ${recibo.periodo_tipo}",
                marginLeft, y, paintNormal
            )
            y += 30f

            // Linea
            canvas.drawLine(marginLeft, y, marginRight, y, paintLine)
            y += 20f

            // === DATOS EMPLEADO ===
            canvas.drawText("Empleado:", marginLeft, y, paintLabel)
            canvas.drawText(recibo.nombre_completo, 150f, y, paintNormal)
            y += 18f
            canvas.drawText("RFC:", marginLeft, y, paintLabel)
            canvas.drawText(recibo.rfc, 150f, y, paintNormal)
            y += 18f
            canvas.drawText("Dias pagados:", marginLeft, y, paintLabel)
            canvas.drawText("${recibo.dias_pagados}", 150f, y, paintNormal)
            canvas.drawText("Salario diario:", 300f, y, paintLabel)
            canvas.drawText(currencyFormat.format(recibo.salario_diario), 420f, y, paintNormal)
            y += 25f

            // Linea
            canvas.drawLine(marginLeft, y, marginRight, y, paintLine)
            y += 20f

            // === PERCEPCIONES ===
            canvas.drawText("PERCEPCIONES", marginLeft, y, paintGreen)
            y += 18f

            for (p in recibo.percepciones_list) {
                val nombre = p.concepto.ifEmpty { p.nombre }
                canvas.drawText(nombre, marginLeft + 10f, y, paintNormal)
                canvas.drawText(currencyFormat.format(p.monto), 450f, y, paintNormal)
                y += 16f
                if (y > 780f) break // Evitar overflow
            }

            y += 5f
            canvas.drawLine(marginLeft, y, marginRight, y, paintLine)
            y += 15f
            canvas.drawText("Total Percepciones:", marginLeft, y, paintGreen)
            canvas.drawText(currencyFormat.format(recibo.total_percepciones), 450f, y, paintGreen)
            y += 25f

            // === DEDUCCIONES ===
            canvas.drawText("DEDUCCIONES", marginLeft, y, paintRed)
            y += 18f

            for (d in recibo.deducciones_list) {
                val nombre = d.concepto.ifEmpty { d.nombre }
                canvas.drawText(nombre, marginLeft + 10f, y, paintNormal)
                canvas.drawText("-${currencyFormat.format(d.monto)}", 450f, y, paintNormal)
                y += 16f
                if (y > 780f) break
            }

            y += 5f
            canvas.drawLine(marginLeft, y, marginRight, y, paintLine)
            y += 15f
            canvas.drawText("Total Deducciones:", marginLeft, y, paintRed)
            canvas.drawText("-${currencyFormat.format(recibo.total_deducciones)}", 450f, y, paintRed)
            y += 30f

            // === NETO ===
            canvas.drawLine(marginLeft, y, marginRight, y, Paint().apply {
                color = Color.parseColor("#0052cc")
                strokeWidth = 2f
            })
            y += 25f
            canvas.drawText("NETO A PAGAR:", marginLeft, y, paintSubtitle)
            canvas.drawText(currencyFormat.format(recibo.neto), 420f, y, paintBigNeto)

            document.finishPage(page)

            // Guardar en Downloads
            val fileName = "Recibo_${recibo.periodo_nombre.replace(" ", "_")}.pdf"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ usa MediaStore
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        document.writeTo(outputStream)
                    }
                    Toast.makeText(this, "PDF guardado en Descargas: $fileName", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Error al guardar PDF", Toast.LENGTH_LONG).show()
                }
            } else {
                // Android 9 y menor
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)
                FileOutputStream(file).use { outputStream ->
                    document.writeTo(outputStream)
                }
                Toast.makeText(this, "PDF guardado en: ${file.absolutePath}", Toast.LENGTH_LONG).show()
            }

            document.close()
            Log.d(TAG, "PDF generado: $fileName")

        } catch (e: Exception) {
            Log.e(TAG, "Error generando PDF: ${e.message}", e)
            Toast.makeText(this, "Error al generar PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // ==================== HELPERS ====================

    private fun formatDate(dateStr: String?): String {
        if (dateStr.isNullOrEmpty()) return "-"
        return try {
            val inputFormat = if (dateStr.contains("T")) {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            } else {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            }
            val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale("es", "MX"))
            val date = inputFormat.parse(dateStr)
            if (date != null) outputFormat.format(date) else dateStr.take(10)
        } catch (e: Exception) {
            dateStr.take(10)
        }
    }

    private fun showLoading(show: Boolean) {
        progressLayout.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            recyclerView.visibility = View.GONE
            emptyLayout.visibility = View.GONE
        }
    }
}

// ==================== ADAPTER ====================

class RecibosAdapter(
    private var recibos: List<ReciboNomina>,
    private val onItemClick: (ReciboNomina) -> Unit
) : RecyclerView.Adapter<RecibosAdapter.ViewHolder>() {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
    private val dateInputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    private val dateInputFormatShort = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dateOutputFormat = SimpleDateFormat("dd MMM yyyy", Locale("es", "MX"))

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPeriodoNombre: TextView = view.findViewById(R.id.tvPeriodoNombre)
        val tvTipoPeriodo: TextView = view.findViewById(R.id.tvTipoPeriodo)
        val tvRangoFechas: TextView = view.findViewById(R.id.tvRangoFechas)
        val tvPercepciones: TextView = view.findViewById(R.id.tvPercepciones)
        val tvDeducciones: TextView = view.findViewById(R.id.tvDeducciones)
        val tvNeto: TextView = view.findViewById(R.id.tvNeto)
        val tvFechaCierre: TextView = view.findViewById(R.id.tvFechaCierre)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recibo, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val recibo = recibos[position]

        holder.tvPeriodoNombre.text = recibo.periodo_nombre
        holder.tvTipoPeriodo.text = recibo.periodo_tipo
        holder.tvRangoFechas.text = "${formatDate(recibo.periodo_inicio)} - ${formatDate(recibo.periodo_fin)}"
        holder.tvPercepciones.text = currencyFormat.format(recibo.total_percepciones)
        holder.tvDeducciones.text = "-${currencyFormat.format(recibo.total_deducciones)}"
        holder.tvNeto.text = currencyFormat.format(recibo.neto)
        holder.tvFechaCierre.text = "Cerrado: ${formatDate(recibo.fecha_cierre)}"

        holder.itemView.setOnClickListener { onItemClick(recibo) }
    }

    override fun getItemCount() = recibos.size

    fun updateData(newRecibos: List<ReciboNomina>) {
        recibos = newRecibos
        notifyDataSetChanged()
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
