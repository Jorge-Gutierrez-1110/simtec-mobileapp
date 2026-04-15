package com.example.simtec_mobileapp

import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Funciones de extensión para mostrar/ocultar pantalla de carga
 */
fun AppCompatActivity.showLoading(message: String = "Cargando...") {
    val rootView = findViewById<android.view.ViewGroup>(android.R.id.content)
    
    // Verificar si ya existe el overlay
    var loadingOverlay = rootView.findViewWithTag<FrameLayout>("loading_overlay")
    
    if (loadingOverlay == null) {
        loadingOverlay = layoutInflater.inflate(R.layout.loading_overlay, null) as FrameLayout
        loadingOverlay.tag = "loading_overlay"
        rootView.addView(loadingOverlay)
    }
    
    loadingOverlay.findViewById<TextView>(R.id.tvLoadingMessage)?.text = message
    loadingOverlay.visibility = View.VISIBLE
}

fun AppCompatActivity.hideLoading() {
    val rootView = findViewById<android.view.ViewGroup>(android.R.id.content)
    val loadingOverlay = rootView.findViewWithTag<FrameLayout>("loading_overlay")
    loadingOverlay?.visibility = View.GONE
}