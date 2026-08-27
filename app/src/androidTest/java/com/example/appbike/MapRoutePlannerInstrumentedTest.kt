package com.example.appbike

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.appbike.ui.theme.APPbikeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MapRoutePlannerInstrumentedTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun routeAndMeetupAreSeparatePrimaryActions() {
        var action = ""
        compose.setContent {
            APPbikeTheme(dynamicColor = false) {
                MapPrimaryActions(
                    onCreateRoute = { action = "route" },
                    onCreateMeetup = { action = "meetup" }
                )
            }
        }

        compose.onNodeWithText("Trayecto").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("route", action) }
        compose.onNodeWithText("Junta").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("meetup", action) }
    }

    @Test
    fun routeDestinationPickerUsesItsOwnCopy() {
        compose.setContent {
            APPbikeTheme(dynamicColor = false) {
                LocationSearchDialog(
                    initial = "",
                    message = null,
                    title = "Crear trayecto",
                    helpText = "Busca el lugar al que quieres llegar.",
                    fieldLabel = "Destino",
                    onDismiss = {},
                    onLocation = {}
                )
            }
        }

        compose.onNodeWithText("Crear trayecto").assertIsDisplayed()
        compose.onNodeWithText("Busca el lugar al que quieres llegar.").assertIsDisplayed()
        compose.onNodeWithText("Destino").assertIsDisplayed()
    }
}
