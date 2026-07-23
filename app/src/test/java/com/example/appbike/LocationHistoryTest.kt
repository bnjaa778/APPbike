package com.example.appbike

import org.junit.Assert.assertEquals
import org.junit.Test

class LocationHistoryTest {
    private val osorno = GeoPoint(-40.5764006, -73.1148018, "Osorno, Chile")
    private val santiago = GeoPoint(-33.4488897, -70.6692655, "Santiago, Chile")

    @Test
    fun selectedLocationMovesToFrontWithoutLosingPreviousPlace() {
        val history = LocalDataStore.mergeLocationHistory(
            selected = santiago,
            previousLocations = listOf(osorno, santiago)
        )

        assertEquals(listOf(santiago, osorno), history)
    }

    @Test
    fun equivalentCoordinatesAreNotDuplicated() {
        val almostSameOsorno = osorno.copy(
            latitude = osorno.latitude + 0.000001,
            label = "Osorno actualizado"
        )

        val history = LocalDataStore.mergeLocationHistory(
            selected = almostSameOsorno,
            previousLocations = listOf(osorno)
        )

        assertEquals(1, history.size)
        assertEquals("Osorno actualizado", history.first().label)
    }
}
