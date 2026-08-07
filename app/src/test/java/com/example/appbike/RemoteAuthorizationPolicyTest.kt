package com.example.appbike

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteAuthorizationPolicyTest {
    @Test
    fun publicDiscoveryAndLoginNeverAttachBearerToken() {
        listOf(
            "login",
            "junta.list",
            "junta.get",
            "junta.photo.get",
            "location.search",
            "location.reverse",
            "location.resolve",
            "marketplace.list",
            "marketplace.get",
            "marketplace.photo.get"
        ).forEach { action ->
            assertFalse(action, RemoteConnections.shouldAuthenticateAction(action))
        }
    }

    @Test
    fun privateReadsAndMutationsRequireBearerToken() {
        listOf(
            "user.get",
            "user.bikes.list",
            "bike.create",
            "bike.update",
            "bike.delete",
            "maintenance.past.list",
            "junta.create",
            "junta.mine.list",
            "marketplace.create",
            "marketplace.mine.list",
            "chat.list",
            "chat.message.send",
            "sports.connections.list"
        ).forEach { action ->
            assertTrue(action, RemoteConnections.shouldAuthenticateAction(action))
        }
    }

    @Test
    fun profileFallbackOnlyMasksUnavailableOptionalLists() {
        assertTrue(
            RemoteConnections.shouldUseRegionalProfileFallback(
                RemoteConnections.RemoteConnectionException("Acción no disponible", statusCode = 400),
                hasAccessToken = true
            )
        )
        assertTrue(
            RemoteConnections.shouldUseRegionalProfileFallback(
                RemoteConnections.RemoteConnectionException("No encontrado", statusCode = 404),
                hasAccessToken = true
            )
        )
        assertTrue(
            RemoteConnections.shouldUseRegionalProfileFallback(
                RemoteConnections.RemoteConnectionException("Token requerido", statusCode = 401),
                hasAccessToken = false
            )
        )
        assertFalse(
            RemoteConnections.shouldUseRegionalProfileFallback(
                RemoteConnections.RemoteConnectionException("Sesión vencida", statusCode = 401),
                hasAccessToken = true
            )
        )
        assertFalse(
            RemoteConnections.shouldUseRegionalProfileFallback(
                RemoteConnections.RemoteConnectionException("Servidor caído", statusCode = 500),
                hasAccessToken = true
            )
        )
    }
}
