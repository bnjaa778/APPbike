package com.example.appbike

import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performTextInput
import com.example.appbike.ui.theme.APPbikeTheme
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class LocationSearchDialogInstrumentedTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun manualLocationFieldKeepsSpanishUnicodeInput() {
        compose.setContent {
            APPbikeTheme(dynamicColor = false) {
                LocationSearchDialog(
                    initial = "",
                    message = null,
                    onDismiss = {},
                    onLocation = {}
                )
            }
        }

        compose.onNodeWithText("Lugar o dirección")
            .performTextInput("Viña del Mar")
        compose.onNodeWithText("Viña del Mar").assertIsDisplayed()
    }

    @Test
    fun firstEditReplacesThePreviouslySelectedLocation() {
        compose.setContent {
            APPbikeTheme(dynamicColor = false) {
                LocationSearchDialog(
                    initial = "Mountain View",
                    message = null,
                    onDismiss = {},
                    onLocation = {}
                )
            }
        }

        compose.onNodeWithText("Lugar o dirección")
            .performClick()
            .performTextInput("Osorno")

        compose.onNodeWithText("Osorno").assertIsDisplayed()
    }

    @Test
    fun locationPanelConsumesTouchesWithoutAdvertisingAnEmptyAction() {
        var dismissed = false
        compose.setContent {
            APPbikeTheme(dynamicColor = false) {
                LocationSearchDialog(
                    initial = "",
                    message = null,
                    onDismiss = { dismissed = true },
                    onLocation = {}
                )
            }
        }

        compose.onNodeWithTag(LOCATION_SEARCH_PANEL_TEST_TAG, useUnmergedTree = true)
            .assertHasNoClickAction()
        compose.onNodeWithText("Busca una ciudad", substring = true)
            .performTouchInput { click() }

        compose.runOnIdle {
            assertFalse("Tocar dentro del panel no debe cerrar el selector.", dismissed)
        }
    }
}
