package com.example.appbike

import org.junit.Assert.assertEquals
import org.junit.Test

class MarketplacePricingTest {
    @Test
    fun localizedThousandsAreSentAsWholeUnits() {
        assertEquals("10000", normalizeWholeUnitInput("10.000"))
        assertEquals(10_000L, parseMarketplaceWholeUnitPrice("10.000"))
        assertEquals(1_000_000L, parseMarketplaceWholeUnitPrice("1.000.000"))
    }

    @Test
    fun chileUsesClpAndDotGrouping() {
        val currency = marketplaceCurrencyFor(
            GeoPoint(-33.4489, -70.6693, "Santiago, Región Metropolitana, Chile")
        )

        assertEquals("CLP", currency.code)
        assertEquals("$25.000 CLP", formatMarketplacePrice("25000.00", currency))
    }

    @Test
    fun argentinaUsesArgentinePesosAndDotGrouping() {
        val currency = marketplaceCurrencyFor(
            GeoPoint(-34.6037, -58.3816, "Buenos Aires", countryCode = "AR")
        )

        assertEquals("ARS", currency.code)
        assertEquals("peso argentino", currency.name)
        assertEquals("$100.000 ARS", formatMarketplacePrice("100000", currency))
    }

    @Test
    fun persistedCurrencyWinsOverCurrentSearchLabel() {
        val currency = marketplaceCurrencyFor(
            GeoPoint(
                -33.4489,
                -70.6693,
                "Santiago, Chile",
                countryCode = "CL",
                currencyCode = "ARS"
            )
        )

        assertEquals("ARS", currency.code)
    }
}
