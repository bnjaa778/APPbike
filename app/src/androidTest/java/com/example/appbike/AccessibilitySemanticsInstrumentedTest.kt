package com.example.appbike

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.appbike.ui.theme.APPbikeTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AccessibilitySemanticsInstrumentedTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun brandAndSportsCardExposeOneUsefulAnnouncementEach() {
        compose.setContent {
            APPbikeTheme(dynamicColor = false) {
                Column {
                    AppTopBar(
                        session = null,
                        accountSelected = false,
                        onAccountClick = {}
                    )
                    SportsPlatformCard(
                        SyncPlatform(
                            id = "strava",
                            name = "Strava",
                            description = "Actividades, rutas y entrenamientos.",
                            connected = false
                        )
                    )
                }
            }
        }

        compose.onNodeWithContentDescription("APPBIKE. RIDE, CONNECT, GROW")
            .assertIsDisplayed()

        compose.onNodeWithContentDescription(
            "Strava. Actividades, rutas y entrenamientos. " +
                "Vinculación en pausa. Próximamente."
        ).assertIsDisplayed()
    }

    @Test
    fun addBikeControlHasAButtonLabelAndWorks() {
        var clicked = false
        compose.setContent {
            APPbikeTheme(dynamicColor = false) {
                AddBikeButton(onClick = { clicked = true })
            }
        }

        compose.onNodeWithContentDescription("Agregar bicicleta")
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()

        compose.runOnIdle {
            assertTrue("Agregar bicicleta debe conservar su acción semántica.", clicked)
        }
    }

    @Test
    fun weatherStatusIsInformativeButNotClickable() {
        compose.setContent {
            APPbikeTheme(dynamicColor = false) {
                WeatherStatusChip(
                    state = WeatherHeaderState.Ready(
                        WeatherSnapshot(
                            temperatureCelsius = 18.0,
                            apparentTemperatureCelsius = 17.5,
                            weatherCode = 0,
                            condition = WeatherCondition.CLEAR,
                            isDay = true,
                            cloudCoverPercent = 0,
                            precipitationMillimeters = 0.0,
                            observedAt = "2026-08-16T16:00",
                            latitude = -33.45,
                            longitude = -70.67
                        )
                    ),
                    compact = false
                )
            }
        }

        compose.onNodeWithContentDescription(
            "Despejado, 18 grados Celsius. Datos de Open-Meteo."
        )
            .assertIsDisplayed()
            .assertHasNoClickAction()
    }

    @Test
    fun bikeSummaryIsClickableOnlyWhenAnActionExists() {
        val bike = Bike(
            name = "Ruta diaria",
            brand = "Trek",
            model = "FX",
            type = "Urbana",
            serialNumber = "TEST-1"
        )

        compose.setContent {
            APPbikeTheme(dynamicColor = false) {
                BikeSummaryCard(bike = bike)
            }
        }

        compose.onNodeWithTag(BIKE_SUMMARY_CARD_TEST_TAG)
            .assertIsDisplayed()
            .assertHasNoClickAction()
    }
}
