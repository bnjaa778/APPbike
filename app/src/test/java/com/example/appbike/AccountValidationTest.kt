package com.example.appbike

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountValidationTest {
    @Test
    fun acceptsUuidAndRejectsLegacyNumericIds() {
        assertTrue(isValidAccountUserId("123e4567-e89b-42d3-a456-426614174000"))
        assertFalse(isValidAccountUserId("123"))
        assertFalse(isValidAccountUserId("not-a-uuid"))
    }
}
