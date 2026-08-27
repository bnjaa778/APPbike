package com.example.appbike

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CyclingNavigationTest {
    @Test
    fun samePointProducesZeroDistanceAndTime() {
        val point = GeoPoint(-33.4489, -70.6693)

        val preview = buildDirectCyclingRoutePreview(point, point)

        assertEquals(0.0, preview.distanceKm, 0.0001)
        assertEquals(0, preview.estimatedMinutes)
        assertTrue(preview.isDirectEstimate)
        assertEquals(listOf(point, point), preview.geometry)
    }

    @Test
    fun previewUsesCyclingSpeedAndDirectGeometry() {
        val origin = GeoPoint(-33.4372, -70.6506)
        val destination = GeoPoint(-33.4254, -70.6338)

        val preview = buildDirectCyclingRoutePreview(
            origin,
            destination,
            averageSpeedKmh = 15.0
        )

        assertTrue(preview.distanceKm in 1.9..2.2)
        assertEquals(9, preview.estimatedMinutes)
        assertEquals(listOf(origin, destination), preview.geometry)
        assertEquals("2,0 km", formatRouteDistance(preview.distanceKm))
    }

    @Test(expected = IllegalArgumentException::class)
    fun previewRejectsInvalidCyclingSpeed() {
        buildDirectCyclingRoutePreview(GeoPoint(0.0, 0.0), GeoPoint(1.0, 1.0), 0.0)
    }
}

