package com.example.simtec_mobileapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

class MapActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    companion object {
        const val EXTRA_LATITUD_OFICINA = "extra_latitud_oficina"
        const val EXTRA_LONGITUD_OFICINA = "extra_longitud_oficina"
        const val EXTRA_RADIO_GEOFENCE = "extra_radio_geofence"
        const val RESULT_WITHIN_RANGE = "result_within_range"

        // Valores por defecto (fallback si no hay geocerca)
        const val DEFAULT_LATITUD = 20.61311309465089
        const val DEFAULT_LONGITUD = -103.20616810113239
        const val DEFAULT_RADIO = 20
    }

    private lateinit var mapView: MapView
    private lateinit var tvTitulo: TextView
    private lateinit var tvDistancia: TextView
    private lateinit var tvStatusGeofence: TextView
    private lateinit var btnCerrar: Button

    private var latitudeOficina = DEFAULT_LATITUD
    private var longitudOficina = DEFAULT_LONGITUD
    private var radioGeofence = DEFAULT_RADIO.toDouble()

    private var latitudeUsuario = 0.0
    private var longitudUsuario = 0.0
    private var hasLocation = false

    // Variables de la API Fused Location (Alta Precisión)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        // Importante para OSMDroid
        Configuration.getInstance().userAgentValue = packageName

        // Inicializar SessionManager
        sessionManager = SessionManager(this)

        initViews()
        setupMap()

        // Inicializar Fused Location e iniciar actualizaciones
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupLocationUpdates()

        btnCerrar.setOnClickListener {
            if (hasLocation && calculateDistance(latitudeUsuario, longitudUsuario, latitudeOficina, longitudOficina) <= radioGeofence) {
                val resultIntent = Intent()
                resultIntent.putExtra(RESULT_WITHIN_RANGE, true)
                setResult(RESULT_OK, resultIntent)
            }
            finish()
        }
    }

    private fun initViews() {
        mapView = findViewById(R.id.mapView)
        tvTitulo = findViewById(R.id.tvTitulo)
        tvDistancia = findViewById(R.id.tvDistancia)
        tvStatusGeofence = findViewById(R.id.tvStatusGeofence)
        btnCerrar = findViewById(R.id.btnCerrar)

        // Obtener de Intent o usar valores por defecto desde SessionManager
        val latIntent = intent.getDoubleExtra(EXTRA_LATITUD_OFICINA, -999.0)
        val lonIntent = intent.getDoubleExtra(EXTRA_LONGITUD_OFICINA, -999.0)
        val radioIntent = intent.getDoubleExtra(EXTRA_RADIO_GEOFENCE, -1.0)

        if (latIntent != -999.0 && lonIntent != -999.0) {
            // Usar datos del intent (enviados desde HomeActivity)
            latitudeOficina = latIntent
            longitudOficina = lonIntent
            radioGeofence = radioIntent
        } else if (sessionManager.isGeocercaActiva()) {
            // Usar datos guardados en SessionManager
            latitudeOficina = sessionManager.getGeocercaLatitud()
            longitudOficina = sessionManager.getGeocercaLongitud()
            radioGeofence = sessionManager.getGeocercaRadioMetros().toDouble()
        } else {
            // Usar valores por defecto
            latitudeOficina = DEFAULT_LATITUD
            longitudOficina = DEFAULT_LONGITUD
            radioGeofence = DEFAULT_RADIO.toDouble()
        }

        tvTitulo.text = "Acércate a la oficina"
    }

    private fun setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(16.0)

        val oficinaPoint = GeoPoint(latitudeOficina, longitudOficina)
        mapView.controller.setCenter(oficinaPoint)

        // Marker de la oficina (Jabil)
        val markerOficina = Marker(mapView)
        markerOficina.position = oficinaPoint
        markerOficina.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        markerOficina.title = "Jabil"
        mapView.overlays.add(markerOficina)

        // Círculo del geofence
        val circle = Polygon()
        circle.points = Polygon.pointsAsCircle(oficinaPoint, radioGeofence)
        circle.fillPaint.color = Color.argb(50, 0, 255, 0)
        circle.outlinePaint.color = Color.GREEN
        circle.outlinePaint.strokeWidth = 3f
        mapView.overlays.add(circle)
    }

    @SuppressLint("MissingPermission")
    private fun setupLocationUpdates() {
        // Verificar permisos
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
            return
        }

        // Petición de alta precisión cada 2 segundos
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
            .setMinUpdateIntervalMillis(1000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    updateLocation(location.latitude, location.longitude)
                }
            }
        }

        // Iniciar el monitoreo del GPS
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            Looper.getMainLooper()
        )
    }

    private fun updateLocation(lat: Double, lon: Double) {
        latitudeUsuario = lat
        longitudUsuario = lon
        hasLocation = true

        val userPoint = GeoPoint(lat, lon)

        // Buscar el marker del usuario y actualizarlo o crearlo
        val existingMarker = mapView.overlays.filterIsInstance<Marker>().find { it.title == "Tu ubicación" }
        if (existingMarker != null) {
            existingMarker.position = userPoint
        } else {
            val markerUser = Marker(mapView)
            markerUser.position = userPoint
            markerUser.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            markerUser.title = "Tu ubicación"
            mapView.overlays.add(markerUser)
        }

        // Calcular distancia usando la API nativa de Android
        val distanciaMetros = calculateDistance(lat, lon, latitudeOficina, longitudOficina)

        tvDistancia.text = if (distanciaMetros < 1000) {
            "📏 Distancia: ${distanciaMetros.toInt()} metros"
        } else {
            "📏 Distancia: %.2f km".format(distanciaMetros / 1000)
        }

        // Lógica de habilitación de botón
        if (distanciaMetros <= radioGeofence) {
            tvStatusGeofence.text = "✅ Estás dentro del área de registro"
            tvStatusGeofence.setBackgroundColor(Color.parseColor("#AA2E7E03"))
            btnCerrar.text = "Registrar asistencia"
        } else {
            tvStatusGeofence.text = "❌ Estás fuera del área de registro"
            tvStatusGeofence.setBackgroundColor(Color.parseColor("#AAC80E0E"))
            btnCerrar.text = "Cerrar"
        }

        // Refrescar el mapa para que se dibuje el nuevo punto
        mapView.invalidate()
    }

    // --- NUEVA FUNCIÓN DE CÁLCULO DE DISTANCIA (API NATIVA) ---
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0].toDouble()
    }

    // --- MANEJO DEL CICLO DE VIDA ---

    @SuppressLint("MissingPermission")
    override fun onResume() {
        super.onResume()
        mapView.onResume()

        // Si regresamos a la app y tenemos permisos, volvemos a encender el rastreo
        if (::fusedLocationClient.isInitialized && locationCallback != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.requestLocationUpdates(
                    LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000).build(),
                    locationCallback!!,
                    Looper.getMainLooper()
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()

        // Apagamos el GPS cuando minimizamos la app para no gastar batería
        if (::fusedLocationClient.isInitialized && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback!!)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Nos aseguramos de matar el proceso del GPS al cerrar
        if (::fusedLocationClient.isInitialized && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback!!)
        }
    }
}