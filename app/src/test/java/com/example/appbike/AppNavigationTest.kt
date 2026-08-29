package com.example.appbike

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun systemBackLeavesSecondaryGarageWithoutChangingMainDestinations() {
        assertEquals(
            AppScreen.ACCOUNT,
            secondaryDestinationAfterBack(AppScreen.BIKES)
        )
        assertNull(secondaryDestinationAfterBack(AppScreen.HOME))
        assertNull(secondaryDestinationAfterBack(AppScreen.ACCOUNT))
    }

    @Test
    fun mapKeepsHorizontalPanWhileOtherDestinationsKeepPagerSwipe() {
        assertFalse(mainPagerSwipeEnabled(AppScreen.ROUTES, false))
        assertTrue(mainPagerSwipeEnabled(AppScreen.ROUTES, true))
        assertTrue(mainPagerSwipeEnabled(AppScreen.HOME, false))
    }
}
