package com.example.appbike

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal data class LocationQuality(
    val accuracyMeters: Float?,
    val timestampMillis: Long
)

internal fun compareLocationQuality(first: LocationQuality, second: LocationQuality): Int {
    val firstAccuracy = first.accuracyMeters
        ?.takeIf { it.isFinite() && it >= 0f }
        ?: Float.MAX_VALUE
    val secondAccuracy = second.accuracyMeters
        ?.takeIf { it.isFinite() && it >= 0f }
        ?: Float.MAX_VALUE
    val accuracyComparison = firstAccuracy.compareTo(secondAccuracy)
    return if (accuracyComparison != 0) {
        accuracyComparison
    } else {
        second.timestampMillis.compareTo(first.timestampMillis)
    }
}

internal object DeviceLocationProvider {

    private const val GPS_TIMEOUT_MS = 15_000L
    private const val NETWORK_TIMEOUT_MS = 8_000L
    private const val PASSIVE_TIMEOUT_MS = 3_000L
    private const val LAST_KNOWN_MAX_AGE_MS = 5 * 60 * 1_000L

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
            if (hasFineLocation && manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                add(LocationManager.GPS_PROVIDER)
            }
            if (hasCoarseLocation && manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                add(LocationManager.NETWORK_PROVIDER)
            }
            if (manager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
                add(LocationManager.PASSIVE_PROVIDER)
            }
        }.distinct()

        if (providers.isEmpty()) {
            throw IllegalStateException("Activa la ubicación del teléfono para continuar.")
        }

        val freshLocations = coroutineScope {
            providers.map { provider ->
                async {
                    try {
                        withTimeoutOrNull(timeoutFor(provider)) {
                            requestCurrentLocation(appContext, manager, provider)
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        null
                    }
                }
            }.awaitAll().filterNotNull()
        }

        bestLocation(freshLocations)?.let { return it.toGeoPoint() }

        val now = System.currentTimeMillis()
        val recentLastKnown = providers.mapNotNull { provider ->
            runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
        }.filter { location ->
            location.time > 0L && now - location.time in 0L..LAST_KNOWN_MAX_AGE_MS
        }

        return bestLocation(recentLastKnown)?.toGeoPoint()
            ?: throw IllegalStateException("No fue posible obtener una ubicación actual.")
    }

    private fun timeoutFor(provider: String): Long = when (provider) {
        LocationManager.GPS_PROVIDER -> GPS_TIMEOUT_MS
        LocationManager.NETWORK_PROVIDER -> NETWORK_TIMEOUT_MS
        else -> PASSIVE_TIMEOUT_MS
    }

    private fun bestLocation(locations: List<Location>): Location? = locations
        .asSequence()
        .filter { location ->
            location.latitude in -90.0..90.0 &&
                location.longitude in -180.0..180.0 &&
                location.latitude.isFinite() &&
                location.longitude.isFinite()
        }
        .minWithOrNull { first, second ->
            compareLocationQuality(first.quality(), second.quality())
        }

    private fun Location.quality() = LocationQuality(
        accuracyMeters = accuracy.takeIf { hasAccuracy() },
        timestampMillis = time
    )

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
        val cancellationSignal = CancellationSignal()
        LocationManagerCompat.getCurrentLocation(
            manager,
            provider,
            cancellationSignal,
            ContextCompat.getMainExecutor(context)
        ) { location ->
            if (continuation.isActive) continuation.resume(location)
        }
        continuation.invokeOnCancellation { cancellationSignal.cancel() }
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
