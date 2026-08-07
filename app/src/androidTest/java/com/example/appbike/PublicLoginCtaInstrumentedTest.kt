package com.example.appbike

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.example.appbike.ui.theme.APPbikeTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PublicLoginCtaInstrumentedTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun marketplaceContactWithoutSessionRequestsAccount() {
        var accountRequested = false
        var showDetail by mutableStateOf(true)
        compose.setContent {
            APPbikeTheme(dynamicColor = false) {
                if (showDetail) MarketplaceDetailScreen(
                    publication = ProductPublication(
                        id = "market-1",
                        title = "Casco",
                        price = "25000",
                        category = "Accesorios",
                        condition = "usado",
                        seller = "Ciclista",
                        description = "Casco de prueba",
                        mediaDescription = "",
                        createdBy = "owner-1",
                        productStatus = "usado",
                        currencyCode = "CLP"
                    ),
                    currency = marketplaceCurrency("CLP"),
                    account = null,
                    loading = false,
                    error = null,
                    onDismiss = { showDetail = false },
                    onStatus = {},
                    onAddPhoto = {},
                    onContact = {
                        accountRequested = true
                        showDetail = false
                    }
                )
            }
        }

        compose.onNodeWithText("Inicia sesión para contactar")
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()

        compose.runOnIdle {
            assertTrue("El CTA de Marketplace debe abrir Cuenta.", accountRequested)
        }
    }

    @Test
    fun meetupContactWithoutSessionRequestsAccount() {
        var accountRequested = false
        var showDetail by mutableStateOf(true)
        compose.setContent {
            APPbikeTheme(dynamicColor = false) {
                if (showDetail) MeetupDetailDialog(
                    event = MeetupEvent(
                        id = "meetup-1",
                        title = "Salida grupal",
                        dateTime = "2026-08-06 10:00",
                        description = "Ruta de prueba",
                        latitude = -41.47,
                        longitude = -72.94,
                        createdBy = "owner-1",
                        location = "LAS:-41.47,-72.94|Puerto Montt"
                    ),
                    account = null,
                    loading = false,
                    error = null,
                    onDismiss = { showDetail = false },
                    onComplete = {},
                    onAddPhoto = {},
                    onContact = {
                        accountRequested = true
                        showDetail = false
                    }
                )
            }
        }

        compose.onNodeWithText("Inicia sesión para contactar")
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()

        compose.runOnIdle {
            assertTrue("El CTA de Juntas debe abrir Cuenta.", accountRequested)
        }
    }

    @Test
    fun marketplaceDetailWithoutPhotoUsesCompactPlaceholder() {
        var showDetail by mutableStateOf(true)
        compose.setContent {
            APPbikeTheme(dynamicColor = false) {
                if (showDetail) MarketplaceDetailScreen(
                    publication = ProductPublication(
                        id = "market-no-photo",
                        title = "Producto sin portada",
                        price = "15000",
                        category = "Accesorios",
                        condition = "usado",
                        seller = "Ciclista",
                        description = "Detalle visible sin un espacio vacio excesivo.",
                        mediaDescription = "",
                        createdBy = "owner-1",
                        productStatus = "usado",
                        currencyCode = "CLP"
                    ),
                    currency = marketplaceCurrency("CLP"),
                    account = null,
                    loading = false,
                    error = null,
                    onDismiss = { showDetail = false },
                    onStatus = {},
                    onAddPhoto = {},
                    onContact = {}
                )
            }
        }

        compose.onNodeWithTag(MARKETPLACE_DETAIL_HERO_TEST_TAG)
            .assertHeightIsEqualTo(180.dp)
        compose.onNodeWithContentDescription("Volver a Marketplace")
            .performClick()
        compose.runOnIdle {
            assertTrue("El detalle debe cerrarse al volver.", !showDetail)
        }
    }
}
