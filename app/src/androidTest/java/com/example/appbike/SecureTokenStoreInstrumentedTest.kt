package com.example.appbike

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecureTokenStoreInstrumentedTest {
    @Test
    fun encryptedTokenRoundTripAndClear() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        SecureTokenStore.clear(context)
        SecureTokenStore.save(context, "test-token-not-plain-session-data")
        assertEquals("test-token-not-plain-session-data", SecureTokenStore.load(context))
        SecureTokenStore.clear(context)
        assertEquals("", SecureTokenStore.load(context))
    }
}
