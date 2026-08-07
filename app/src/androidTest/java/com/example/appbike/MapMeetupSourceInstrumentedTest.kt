package com.example.appbike

import android.os.Looper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.appbike.ui.theme.APPbikeTheme
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap

class MapMeetupSourceInstrumentedTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun loadedMeetupIsWrittenToGeoJsonSource() {
        val mapReference = AtomicReference<MapLibreMap?>()
        val meetups = mutableStateOf(emptyList<MeetupEvent>())
        val meetup = MeetupEvent(
            id = "meetup-osorno",
            title = "Salida de prueba",
            dateTime = "2026-08-06 10:00",
            description = "Ruta pública",
            latitude = -40.574401,
            longitude = -73.114802,
            createdBy = "owner-1"
        )

        compose.setContent {
            APPbikeTheme(dynamicColor = false) {
                Box(Modifier.fillMaxSize()) {
                    OpenStreetMap(
                        modifier = Modifier.fillMaxSize(),
                        center = GeoPoint(-40.5736955, -73.1358091, "Osorno"),
                        userLocation = GeoPoint(-40.5736955, -73.1358091, "Osorno"),
                        meetups = meetups.value,
                        selectedPoint = null,
                        creationStep = MeetupCreationStep.CLOSED,
                        styleDefinition = OFFLINE_STYLE,
                        onMapReady = { mapReference.set(it) },
                        onPointSelected = {},
                        onMeetupSelected = {},
                        onMapError = {}
                    )
                }
            }
        }

        compose.waitUntil(timeoutMillis = 15_000) { mapReference.get() != null }
        compose.runOnIdle { meetups.value = listOf(meetup) }
        compose.waitUntil(timeoutMillis = 15_000) {
            renderedMeetupFeatures(mapReference.get()).size == 1
        }

        val feature = renderedMeetupFeatures(mapReference.get()).single()
        assertEquals("meetup-osorno", feature.getStringProperty("meetup_id"))
    }

    private fun renderedMeetupFeatures(map: MapLibreMap?) = onMainThread {
        map?.let { readyMap ->
            readyMap.queryRenderedFeatures(
                readyMap.projection.toScreenLocation(
                    LatLng(-40.574401, -73.114802)
                ),
                "appbike-meetup-layer"
            )
        }.orEmpty()
    }

    private fun <T> onMainThread(block: () -> T): T {
        if (Looper.myLooper() == Looper.getMainLooper()) return block()
        val result = AtomicReference<Result<T>>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            result.set(runCatching(block))
        }
        return result.get().getOrThrow()
    }

    private companion object {
        const val OFFLINE_STYLE = """
            {
              "version": 8,
              "sources": {},
              "layers": [
                {
                  "id": "background",
                  "type": "background",
                  "paint": { "background-color": "#07140F" }
                }
              ]
            }
        """
    }
}
