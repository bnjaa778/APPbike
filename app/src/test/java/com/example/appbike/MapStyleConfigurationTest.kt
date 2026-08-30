package com.example.appbike

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapStyleConfigurationTest {
    @Test
    fun mapOffersStreetSatelliteAndHybridModes() {
        assertEquals(
            listOf("Mapa", "Satélite", "Híbrido"),
            MapStyleMode.entries.map { it.visibleLabel }
        )
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

    @Test
    fun hybridModeCombinesSatelliteImageryWithStreetVectorData() {
        val style = MapStyleMode.HIBRIDO.styleDefinition

        assertTrue(style.contains("World_Imagery/MapServer/tile/{z}/{y}/{x}"))
        assertTrue(style.contains("\"type\": \"vector\""))
        assertTrue(style.contains("tiles.openfreemap.org/planet"))
        assertTrue(style.contains("\"source-layer\": \"transportation\""))
        assertTrue(style.contains("hybrid-street-names"))
        assertEquals(SATELLITE_MAX_CAMERA_ZOOM, MapStyleMode.HIBRIDO.maximumCameraZoom, 0.0)
    }

    @Test
    fun cyclingModesExposeTheThreeRiderProfiles() {
        assertEquals(
            listOf("Bicicleta de ruta", "Gravel", "Mountain Bike"),
            CyclingMode.entries.map { it.visibleLabel }
        )
        assertEquals(
            listOf("Ruta", "Gravel", "MTB"),
            CyclingMode.entries.map { it.compactLabel }
        )
        assertEquals(CyclingMode.RUTA, CyclingMode.fromStored("unknown"))
    }

    @Test
    fun compassReportsCardinalDirectionsAndHandlesWrapAround() {
        assertEquals("N", compassCardinalDirection(0.0))
        assertEquals("E", compassCardinalDirection(90.0))
        assertEquals("S", compassCardinalDirection(180.0))
        assertEquals("O", compassCardinalDirection(270.0))
        assertEquals("NE", compassCardinalDirection(45.0))
        assertEquals(359.0, normalizeBearing(-1.0), 0.0)
    }

    @Test
    fun compassSmoothingUsesTheShortestPathAcrossNorth() {
        assertEquals(0.0, smoothBearing(359.0, 1.0, 0.5), 0.001)
        assertEquals(135.0, smoothBearing(90.0, 180.0, 0.5), 0.001)
    }
}
