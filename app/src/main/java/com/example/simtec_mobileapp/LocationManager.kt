package com.example.simtec_mobileapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager as AndroidLocationManager
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class LocationManager(private val context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as AndroidLocationManager
    private val geocoder = Geocoder(context, Locale("es", "UY"))

    /**
     * Obtiene la ubicación legible actual del dispositivo
     * Retorna una dirección formateada como "Calle Principal 123, Montevideo"
     * Si falla, retorna "Ubicación desconocida"
     */
    suspend fun getReadableLocation(): String {
        return try {
            // Verificamos permisos
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w("LocationManager", "Permisos de ubicación no otorgados")
                return "Permisos denegados"
            }

            // Obtenemos el proveedor de ubicación más preciso disponible
            val providers = locationManager.allProviders.filter { provider ->
                locationManager.isProviderEnabled(provider)
            }

            if (providers.isEmpty()) {
                Log.w("LocationManager", "No hay proveedores de ubicación disponibles")
                return "GPS desactivado"
            }

            // Intentamos con GPS primero, luego con red
            val provider = providers.firstOrNull { it == AndroidLocationManager.GPS_PROVIDER }
                ?: providers.firstOrNull { it == AndroidLocationManager.NETWORK_PROVIDER }
                ?: providers.first()

            Log.d("LocationManager", "Usando proveedor: $provider")

            // Obtenemos la última ubicación conocida (para no esperar mucho)
            val location = withContext(Dispatchers.Default) {
                locationManager.getLastKnownLocation(provider)
            }

            if (location == null) {
                Log.w("LocationManager", "No hay ubicación disponible")
                return "Ubicación no disponible"
            }

            Log.d("LocationManager", "Ubicación obtenida: ${location.latitude}, ${location.longitude}")

            // Convertimos coordenadas a dirección legible (Reverse Geocoding)
            val addresses = withContext(Dispatchers.Default) {
                try {
                    geocoder.getFromLocation(location.latitude, location.longitude, 1)
                } catch (e: Exception) {
                    Log.e("LocationManager", "Error en reverse geocoding: ${e.message}")
                    emptyList()
                }
            }

            if (addresses.isNullOrEmpty()) {
                Log.w("LocationManager", "No se encontró dirección para las coordenadas")
                return formatCoordinates(location.latitude, location.longitude)
            }

            val address = addresses.first()
            formatAddress(address)

        } catch (e: Exception) {
            Log.e("LocationManager", "Error obteniendo ubicación: ${e.message}")
            "Error obteniendo ubicación"
        }
    }

    /**
     * Formatea una dirección Android Address a string legible
     */
    private fun formatAddress(address: Address): String {
        val parts = mutableListOf<String>()

        // Agregamos calle y número
        if (!address.thoroughfare.isNullOrEmpty()) {
            parts.add(address.thoroughfare)
            if (address.featureName != null && address.featureName != address.thoroughfare) {
                parts.add(address.featureName)
            }
        }

        // Agregamos ciudad/localidad
        if (!address.locality.isNullOrEmpty()) {
            parts.add(address.locality)
        } else if (!address.adminArea.isNullOrEmpty()) {
            parts.add(address.adminArea)
        }

        // Agregamos país si está disponible
        if (!address.countryName.isNullOrEmpty()) {
            parts.add(address.countryName)
        }

        return if (parts.isNotEmpty()) {
            parts.joinToString(", ")
        } else {
            formatCoordinates(address.latitude, address.longitude)
        }
    }

    /**
     * Formatea coordenadas como fallback
     */
    private fun formatCoordinates(latitude: Double, longitude: Double): String {
        return "%.4f, %.4f".format(latitude, longitude)
    }

    /**
     * Verifica si el dispositivo tiene permisos de ubicación
     */
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Verifica si la ubicación está habilitada en el dispositivo
     */
    fun isLocationEnabled(): Boolean {
        return locationManager.isProviderEnabled(AndroidLocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(AndroidLocationManager.NETWORK_PROVIDER)
    }
}
