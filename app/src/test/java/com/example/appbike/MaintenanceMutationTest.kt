package com.example.appbike

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MaintenanceMutationTest {
    private val account = AccountSession(
        userId = "123e4567-e89b-42d3-a456-426614174000",
        email = "rider@appbike.cl"
    )
    private val bike = Bike(
        name = "Gravel diaria",
        brand = "Trek",
        model = "Checkpoint",
        type = "Gravel",
        serialNumber = "SERIE-42",
        remoteId = 42L,
        userId = account.userId
    )

    @Test
    fun maintenanceMutationTrimsFieldsAndBindsTheBike() {
        val normalized = RemoteConnections.normalizeMaintenanceReminder(
            account = account,
            bike = bike,
            reminder = MaintenanceReminder(
                bike = "nombre obsoleto",
                component = "  Cambio de cadena  ",
                date = " 2026-08-06 ",
                notes = "  Cadena nueva  ",
                remoteId = 7L
            ),
            requireRemoteId = true
        )

        assertEquals("Gravel diaria", normalized.bike)
        assertEquals("Cambio de cadena", normalized.component)
        assertEquals("2026-08-06", normalized.date)
        assertEquals("Cadena nueva", normalized.notes)
        assertEquals(42L, normalized.bikeId)
        assertEquals(7L, normalized.remoteId)
    }

    @Test
    fun serviceMutationTrimsFieldsAndPreservesRemoteIdentity() {
        val normalized = RemoteConnections.normalizeServiceBooking(
            account = account,
            bike = bike,
            booking = ServiceBooking(
                workshop = "  Taller Central ",
                service = " Ajuste de frenos  ",
                date = "2026-08-20",
                contact = " contacto@appbike.cl ",
                remoteId = 9L
            ),
            requireRemoteId = true
        )

        assertEquals("Taller Central", normalized.workshop)
        assertEquals("Ajuste de frenos", normalized.service)
        assertEquals("contacto@appbike.cl", normalized.contact)
        assertEquals("Gravel diaria", normalized.bike)
        assertEquals(42L, normalized.bikeId)
        assertEquals(9L, normalized.remoteId)
    }

    @Test
    fun rejectsForeignMissingOrInvalidMaintenanceIdentity() {
        assertThrows(RemoteConnections.RemoteConnectionException::class.java) {
            RemoteConnections.normalizeMaintenanceReminder(
                account,
                bike,
                MaintenanceReminder(
                    bike = bike.name,
                    component = "Cadena",
                    date = "2026-08-06",
                    notes = "",
                    bikeId = 99L,
                    remoteId = 1L
                ),
                requireRemoteId = true
            )
        }
        assertThrows(RemoteConnections.RemoteConnectionException::class.java) {
            RemoteConnections.normalizeMaintenanceReminder(
                account,
                bike,
                MaintenanceReminder(bike.name, "Cadena", "2026-02-30", ""),
                requireRemoteId = true
            )
        }
        assertThrows(RemoteConnections.RemoteConnectionException::class.java) {
            RemoteConnections.normalizeServiceBooking(
                account,
                bike,
                ServiceBooking("", "Frenos", "2026-08-20", ""),
                requireRemoteId = false
            )
        }
    }

    @Test
    fun rejectsMaintenanceReturnedForAnotherBike() {
        assertThrows(RemoteConnections.RemoteConnectionException::class.java) {
            RemoteConnections.validateMaintenanceBikeId(
                expectedBikeId = 42L,
                returnedBikeId = 43L
            )
        }

        RemoteConnections.validateMaintenanceBikeId(
            expectedBikeId = 42L,
            returnedBikeId = null
        )
        RemoteConnections.validateMaintenanceBikeId(
            expectedBikeId = 42L,
            returnedBikeId = 42L
        )
    }
}
