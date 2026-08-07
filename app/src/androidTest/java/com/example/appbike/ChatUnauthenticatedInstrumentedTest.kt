package com.example.appbike

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.appbike.ui.theme.APPbikeTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChatUnauthenticatedInstrumentedTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun loginCallToActionOpensAccountFlow() {
        var accountRequested = false
        compose.setContent {
            APPbikeTheme(dynamicColor = false) {
                ChatScreen(
                    account = null,
                    onOpenAccount = { accountRequested = true }
                )
            }
        }

        compose.onNodeWithText("Iniciar sesión")
            .assertIsDisplayed()
            .performClick()

        compose.runOnIdle {
            assertTrue("El CTA de Chat debe abrir Cuenta.", accountRequested)
        }
    }
}
