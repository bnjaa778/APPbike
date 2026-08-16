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
    fun horizontalSwipeMovesInRequestedMainOrder() {
        assertEquals(
            AppScreen.MARKETPLACE,
            mainDestinationAfterSwipe(AppScreen.BIKES, horizontalDrag = -120f, threshold = 72f)
        )
        assertEquals(
            AppScreen.BIKES,
            mainDestinationAfterSwipe(AppScreen.MARKETPLACE, horizontalDrag = 120f, threshold = 72f)
        )
        assertEquals(
            AppScreen.CHAT,
            mainDestinationAfterSwipe(AppScreen.CHAT, horizontalDrag = -120f, threshold = 72f)
        )
        assertEquals(
            AppScreen.BIKES,
            mainDestinationAfterSwipe(AppScreen.BIKES, horizontalDrag = 40f, threshold = 72f)
        )
    }
}
