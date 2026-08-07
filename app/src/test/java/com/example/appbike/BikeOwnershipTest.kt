package com.example.appbike

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BikeOwnershipTest {
    private val activeUser = "123e4567-e89b-42d3-a456-426614174000"

    @Test
    fun acceptsOwnedAndLegacyBikesWithoutOwnerField() {
        val bikes = listOf(
            Bike("Ruta", "Marca", "Modelo", "Ruta", "SERIE-1", userId = activeUser),
            Bike("Antigua", "Marca", "Modelo", "MTB", "SERIE-2")
        )

        assertEquals(bikes, RemoteConnections.validateBikeOwnership(activeUser, bikes))
    }

    @Test
    fun rejectsBikeFromAnotherAccount() {
        val foreignBike = Bike(
            "Ajena",
            "Marca",
            "Modelo",
            "Gravel",
            "SERIE-3",
            userId = "123e4567-e89b-42d3-a456-426614174001"
        )

        assertThrows(RemoteConnections.RemoteConnectionException::class.java) {
            RemoteConnections.validateBikeOwnership(activeUser, listOf(foreignBike))
        }
    }
}
