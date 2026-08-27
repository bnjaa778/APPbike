package com.example.appbike

import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal const val DEFAULT_CYCLING_SPEED_KMH = 15.0

internal fun buildDirectCyclingRoutePreview(
    origin: GeoPoint,
    destination: GeoPoint,
    averageSpeedKmh: Double = DEFAULT_CYCLING_SPEED_KMH
): CyclingRoutePreview {
    require(averageSpeedKmh > 0.0) { "La velocidad promedio debe ser mayor que cero." }
    val distanceKm = haversineDistanceKm(origin, destination)
    val estimatedMinutes = if (distanceKm == 0.0) {
        0
    } else {
        ceil((distanceKm / averageSpeedKmh) * 60.0).toInt().coerceAtLeast(1)
    }
    return CyclingRoutePreview(
        origin = origin,
        destination = destination,
        distanceKm = distanceKm,
        estimatedMinutes = estimatedMinutes,
        geometry = listOf(origin, destination),
        source = CyclingRouteSource.DIRECT_FALLBACK,
        averageSpeedKmh = averageSpeedKmh
    )
}

internal fun haversineDistanceKm(start: GeoPoint, end: GeoPoint): Double {
    val earthRadiusKm = 6_371.0088
    val startLatitude = Math.toRadians(start.latitude)
    val endLatitude = Math.toRadians(end.latitude)
    val latitudeDelta = endLatitude - startLatitude
    val longitudeDelta = Math.toRadians(end.longitude - start.longitude)
    val sinLatitude = sin(latitudeDelta / 2.0)
    val sinLongitude = sin(longitudeDelta / 2.0)
    val a = sinLatitude * sinLatitude +
        cos(startLatitude) * cos(endLatitude) * sinLongitude * sinLongitude
    return earthRadiusKm * 2.0 * atan2(sqrt(a), sqrt((1.0 - a).coerceAtLeast(0.0)))
}

internal fun formatRouteDistance(distanceKm: Double): String =
    if (distanceKm < 10.0) {
        String.format(java.util.Locale.forLanguageTag("es-CL"), "%.1f km", distanceKm)
    } else {
        String.format(java.util.Locale.forLanguageTag("es-CL"), "%.0f km", distanceKm)
    }

