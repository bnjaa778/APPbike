package com.example.appbike

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BikeMutationTest {
    private val account = AccountSession(
        userId = "123e4567-e89b-42d3-a456-426614174000",
        email = "rider@appbike.cl"
    )

    @Test
    fun normalizesEditableFieldsAndBindsTheActiveOwner() {
        val normalized = RemoteConnections.normalizeBikeMutation(
            account = account,
            bike = Bike(
                name = "  Gravel diaria  ",
                brand = "  Trek ",
                model = " Checkpoint  ",
                type = " Gravel ",
                serialNumber = " SERIE-42 ",
                remoteId = 42L
            ),
            requireRemoteId = true
        )

        assertEquals("Gravel diaria", normalized.name)
        assertEquals("Trek", normalized.brand)
        assertEquals("Checkpoint", normalized.model)
        assertEquals("Gravel", normalized.type)
        assertEquals("SERIE-42", normalized.serialNumber)
        assertEquals(account.userId, normalized.userId)
    }

    @Test
    fun rejectsIncompleteOrForeignBikeMutations() {
        assertThrows(RemoteConnections.RemoteConnectionException::class.java) {
            RemoteConnections.normalizeBikeMutation(
                account,
                Bike("", "Marca", "Modelo", "MTB", "SERIE", remoteId = 1L),
                requireRemoteId = true
            )
        }
        assertThrows(RemoteConnections.RemoteConnectionException::class.java) {
            RemoteConnections.requireOwnedBikeId(
                account,
                Bike(
                    "Ajena",
                    "Marca",
                    "Modelo",
                    "MTB",
                    "SERIE",
                    remoteId = 2L,
                    userId = "123e4567-e89b-42d3-a456-426614174001"
                )
            )
        }
    }

    @Test
    fun mergesPartialUpdateWithoutLosingPhotoOrServerIdentity() {
        val requested = Bike(
            name = "Ruta renovada",
            brand = "Specialized",
            model = "Allez",
            type = "Ruta",
            serialNumber = "SERIE-7",
            remoteId = 7L,
            userId = account.userId,
            photoId = "foto-7.webp",
            imageUri = "appbike-photo://foto-7.webp"
        )
        val returned = Bike(
            name = "Ruta renovada",
            brand = "",
            model = "",
            type = "",
            serialNumber = "",
            remoteId = 7L
        )

        val merged = RemoteConnections.mergeUpdatedBike(account, requested, returned)

        assertEquals("Specialized", merged.brand)
        assertEquals("Allez", merged.model)
        assertEquals(account.userId, merged.userId)
        assertEquals("foto-7.webp", merged.photoId)
        assertEquals("appbike-photo://foto-7.webp", merged.imageUri)
    }

    @Test
    fun rejectsAResponseForAnotherBike() {
        val requested = Bike(
            "Propia",
            "Marca",
            "Modelo",
            "MTB",
            "SERIE",
            remoteId = 9L,
            userId = account.userId
        )
        val wrongResponse = requested.copy(remoteId = 10L)

        assertThrows(RemoteConnections.RemoteConnectionException::class.java) {
            RemoteConnections.mergeUpdatedBike(account, requested, wrongResponse)
        }
    }
}
