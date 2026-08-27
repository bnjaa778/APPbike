package com.example.appbike

import org.junit.Assert.assertEquals
import org.junit.Test

class AppNavigationTest {
    private val session = AccountSession(
        userId = "550e8400-e29b-41d4-a716-446655440000",
        email = "rider@appbike.cl",
        username = "rider"
    )

    @Test
    fun launchSendsMissingOrIncompleteSessionToAccount() {
        assertEquals(AppScreen.ACCOUNT, initialDestinationFor(null))
        assertEquals(AppScreen.ACCOUNT, initialDestinationFor(session.copy(username = null)))
    }

    @Test
    fun launchSendsRegisteredRiderToNewsHome() {
        assertEquals(AppScreen.HOME, initialDestinationFor(session))
    }

    @Test
    fun horizontalPagerKeepsRequestedMainOrderAndBounds() {
        assertEquals(0, mainDestinationIndex(AppScreen.HOME))
        assertEquals(1, mainDestinationIndex(AppScreen.MARKETPLACE))
        assertEquals(2, mainDestinationIndex(AppScreen.ROUTES))
        assertEquals(3, mainDestinationIndex(AppScreen.CHAT))
        assertEquals(4, mainDestinationIndex(AppScreen.ACCOUNT))
        assertEquals(4, mainDestinationIndex(AppScreen.BIKES))
        assertEquals(4, mainDestinationIndex(AppScreen.SYNC))
        assertEquals(1, mainDestinationIndex(AppScreen.CREATE_PUBLICATION))
        assertEquals(AppScreen.HOME, mainDestinationAt(-1))
        assertEquals(AppScreen.ROUTES, mainDestinationAt(2))
        assertEquals(AppScreen.ACCOUNT, mainDestinationAt(99))
    }
}
