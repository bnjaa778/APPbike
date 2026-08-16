package com.example.appbike

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.appbike.ui.theme.APPbikeTheme
import org.junit.Rule
import org.junit.Test

class AuthenticationGatewayInstrumentedTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun unauthenticatedGatewayReplacesProfileAndOffersBothActions() {
        compose.setContent {
            APPbikeTheme(dynamicColor = false) {
                UnauthenticatedAccessScreen(
                    initialErrorMessage = "Tu sesión expiró. Inicia sesión nuevamente.",
                    onLogin = {}
                )
            }
        }

        compose.onNodeWithTag(AUTH_ACCESS_SCREEN_TEST_TAG)
            .assertIsDisplayed()
        compose.onNodeWithText("Tu sesión expiró. Inicia sesión nuevamente.")
            .assertIsDisplayed()
        compose.onNodeWithText("Crear cuenta")
            .performScrollTo()
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()
        compose.onNodeWithText(
            "El registro automático estará disponible cuando el servidor APPBIKE " +
                "habilite la creación segura de cuentas. No enviaremos tus datos " +
                "a una acción que todavía no existe."
        ).assertIsDisplayed()
    }
}
