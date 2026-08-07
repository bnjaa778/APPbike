package com.example.appbike

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CommunityOwnershipTest {
    private val activeUser = "123e4567-e89b-42d3-a456-426614174000"

    @Test
    fun acceptsCommunityContentOwnedByActiveAccount() {
        val posts = listOf(publication(activeUser))
        val meetups = listOf(meetup(activeUser))

        assertEquals(posts, RemoteConnections.validateOwnMarketplacePosts(activeUser, posts))
        assertEquals(meetups, RemoteConnections.validateOwnMeetups(activeUser, meetups))
    }

    @Test
    fun rejectsCommunityContentWithMissingOwner() {
        assertThrows(RemoteConnections.RemoteConnectionException::class.java) {
            RemoteConnections.validateOwnMarketplacePosts(activeUser, listOf(publication("")))
        }
        assertThrows(RemoteConnections.RemoteConnectionException::class.java) {
            RemoteConnections.validateOwnMeetups(activeUser, listOf(meetup("")))
        }
    }

    @Test
    fun rejectsCommunityContentOwnedByAnotherAccount() {
        val foreignUser = "123e4567-e89b-42d3-a456-426614174001"

        assertThrows(RemoteConnections.RemoteConnectionException::class.java) {
            RemoteConnections.validateOwnMarketplacePosts(
                activeUser,
                listOf(publication(foreignUser))
            )
        }
        assertThrows(RemoteConnections.RemoteConnectionException::class.java) {
            RemoteConnections.validateOwnMeetups(activeUser, listOf(meetup(foreignUser)))
        }
    }

    private fun publication(owner: String) = ProductPublication(
        title = "Casco",
        price = "25000",
        category = "Accesorios",
        condition = "usado",
        seller = "Ciclista",
        description = "Casco de prueba",
        mediaDescription = "",
        id = "publication-1",
        createdBy = owner
    )

    private fun meetup(owner: String) = MeetupEvent(
        id = "meetup-1",
        title = "Salida",
        dateTime = "2026-08-06 10:00",
        description = "Junta de prueba",
        latitude = -33.45,
        longitude = -70.66,
        createdBy = owner
    )
}
