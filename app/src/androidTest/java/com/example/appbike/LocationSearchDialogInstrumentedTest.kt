package com.example.appbike

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import com.example.appbike.ui.theme.APPbikeTheme
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
}
