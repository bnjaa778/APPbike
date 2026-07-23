package com.example.appbike

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.annotation.RequiresApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal object DeviceLocationProvider {

    private const val LOCATION_TIMEOUT_MS = 15_000L

    @SuppressLint("MissingPermission")
    suspend fun currentLocation(context: Context): GeoPoint {
        val appContext = context.applicationContext
        val manager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val hasFineLocation = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFineLocation && !hasCoarseLocation) {
            throw IllegalStateException("No se concedió acceso a la ubicación.")
        }

        val providers = buildList {
            if (hasCoarseLocation && manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                add(LocationManager.NETWORK_PROVIDER)
            }
            if (hasFineLocation && manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                add(LocationManager.GPS_PROVIDER)
            }
            if (manager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
                add(LocationManager.PASSIVE_PROVIDER)
            }
        }.distinct()

        if (providers.isEmpty()) {
            throw IllegalStateException("Activa la ubicación del teléfono para continuar.")
        }

        val lastKnown = providers.mapNotNull { provider ->
            runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
        }.maxWithOrNull(
            compareBy<Location> { it.time }
                .thenByDescending { it.accuracy }
        )

        for (provider in providers) {
            val freshLocation = withTimeoutOrNull(LOCATION_TIMEOUT_MS / providers.size) {
                requestCurrentLocation(appContext, manager, provider)
            }
            if (freshLocation != null) {
                return freshLocation.toGeoPoint()
            }
        }

        return lastKnown?.toGeoPoint()
            ?: throw IllegalStateException("No fue posible obtener una ubicación actual.")
    }

    suspend fun searchLocations(
        context: Context,
        query: String,
        maxResults: Int = 5
    ): List<GeoPoint> {
        val cleanQuery = query.trim()
        if (cleanQuery.length < 3 || !Geocoder.isPresent()) return emptyList()

        parseCoordinates(cleanQuery)?.let { return listOf(it) }

        val geocoder = Geocoder(
            context.applicationContext,
            Locale.forLanguageTag("es")
        )
        val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getAddressesAsync(geocoder, cleanQuery, maxResults)
        } else {
            getAddressesBlocking(geocoder, cleanQuery, maxResults)
        }

        return addresses
            .asSequence()
            .map { address ->
                val countryCode = address.countryCode.orEmpty().uppercase(Locale.ROOT)
                GeoPoint(
                    latitude = address.latitude,
                    longitude = address.longitude,
                    label = address.displayLabel(),
                    countryCode = countryCode,
                    administrativeArea = address.adminArea.orEmpty(),
                    regionCode = communityRegionCodeFor(
                        countryCode,
                        address.adminArea.orEmpty()
                    ),
                    currencyCode = currencyCodeForCountry(countryCode)
                )
            }
            .distinctBy { point -> point.latitude to point.longitude }
            .take(maxResults)
            .toList()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun getAddressesAsync(
        geocoder: Geocoder,
        query: String,
        maxResults: Int
    ): List<Address> = suspendCancellableCoroutine { continuation ->
        geocoder.getFromLocationName(
            query,
            maxResults,
            object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<Address>) {
                    if (continuation.isActive) continuation.resume(addresses)
                }

                override fun onError(errorMessage: String?) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(
                            IllegalStateException(
                                errorMessage ?: "No fue posible buscar ubicaciones."
                            )
                        )
                    }
                }
            }
        )
    }

    @Suppress("DEPRECATION")
    private fun getAddressesBlocking(
        geocoder: Geocoder,
        query: String,
        maxResults: Int
    ): List<Address> = geocoder.getFromLocationName(query, maxResults).orEmpty()

    @SuppressLint("MissingPermission")
    private suspend fun requestCurrentLocation(
        context: Context,
        manager: LocationManager,
        provider: String
    ): Location? = suspendCancellableCoroutine { continuation ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val cancellationSignal = CancellationSignal()
            manager.getCurrentLocation(
                provider,
                cancellationSignal,
                context.mainExecutor
            ) { location ->
                if (continuation.isActive) continuation.resume(location)
            }
            continuation.invokeOnCancellation { cancellationSignal.cancel() }
        } else {
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    manager.removeUpdates(this)
                    if (continuation.isActive) continuation.resume(location)
                }

                @Deprecated("Deprecated in Android")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

                override fun onProviderEnabled(provider: String) = Unit

                override fun onProviderDisabled(provider: String) {
                    manager.removeUpdates(this)
                    if (continuation.isActive) continuation.resume(null)
                }
            }
            manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
            continuation.invokeOnCancellation { manager.removeUpdates(listener) }
        }
    }

    private fun Location.toGeoPoint() = GeoPoint(
        latitude = latitude,
        longitude = longitude
    )

    private fun parseCoordinates(value: String): GeoPoint? {
        val parts = value.split(',').map(String::trim)
        if (parts.size != 2) return null
        val latitude = parts[0].toDoubleOrNull() ?: return null
        val longitude = parts[1].toDoubleOrNull() ?: return null
        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
        return GeoPoint(latitude, longitude, value)
    }

    private fun Address.displayLabel(): String {
        if (maxAddressLineIndex >= 0) {
            getAddressLine(0)?.takeIf(String::isNotBlank)?.let { return it }
        }
        return listOfNotNull(
            featureName,
            thoroughfare,
            subThoroughfare,
            locality,
            subAdminArea,
            adminArea,
            countryName
        ).filter(String::isNotBlank)
            .distinct()
            .joinToString(", ")
            .ifBlank { "${latitude}, ${longitude}" }
    }
}
