package com.example.appbike

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapStyleConfigurationTest {
    @Test
    fun mapOffersStreetAndSatelliteModes() {
        assertEquals(listOf("Mapa", "Satélite"), MapStyleMode.entries.map { it.visibleLabel })
    }

    @Test
    fun satelliteModeUsesRasterImageryLabelsAndAttribution() {
        val style = MapStyleMode.SATELITE.styleDefinition

        assertTrue(style.contains("World_Imagery/MapServer/tile/{z}/{y}/{x}"))
        assertTrue(style.contains("World_Boundaries_and_Places/MapServer/tile/{z}/{y}/{x}"))
        assertTrue(style.contains("\"type\": \"raster\""))
        assertTrue(style.contains("\"attribution\""))
        assertEquals(SATELLITE_MAX_CAMERA_ZOOM, MapStyleMode.SATELITE.maximumCameraZoom, 0.0)
        assertEquals(17.0, MapStyleMode.SATELITE.maximumCameraZoom, 0.0)
        assertTrue(style.contains("\"maxzoom\": 19"))
    }
}
