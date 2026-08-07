package com.example.appbike

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BikeDateValidationTest {
    @Test
    fun acceptsRealIsoDates() {
        assertTrue(isValidApiDateInput("2026-08-05"))
        assertTrue(isValidApiDateInput(" 2024-02-29 "))
    }

    @Test
    fun rejectsInvalidOrAmbiguousDates() {
        assertFalse(isValidApiDateInput("2026-02-29"))
        assertFalse(isValidApiDateInput("2026-13-01"))
        assertFalse(isValidApiDateInput("05-08-2026"))
        assertFalse(isValidApiDateInput(""))
    }
}
