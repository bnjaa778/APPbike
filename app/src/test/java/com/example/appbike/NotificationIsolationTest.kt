package com.example.appbike

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationIsolationTest {
    @Test
    fun onlyActiveRecipientCanReceiveEvent() {
        val firstUser = "11111111-1111-4111-8111-111111111111"
        val secondUser = "22222222-2222-4222-8222-222222222222"

        assertTrue(isNotificationForActiveUser(firstUser, firstUser))
        assertFalse(isNotificationForActiveUser(firstUser, secondUser))
        assertFalse(isNotificationForActiveUser(firstUser, null))
        assertFalse(isNotificationForActiveUser("", firstUser))
    }
}
