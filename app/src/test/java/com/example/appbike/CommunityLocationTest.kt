package com.example.appbike

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CommunityLocationTest {
    @Test
    fun parsesEncodedCommunityCoordinates() {
        val point = RemoteConnections.parseCommunityLocation(
            "LAS:-40.574401,-73.114802|Puerto Varas"
        )
        assertEquals(-40.574401, point!!.latitude, 0.000001)
        assertEquals(-73.114802, point.longitude, 0.000001)
        assertEquals("Puerto Varas", point.label)
    }

    @Test
    fun rejectsInvalidCoordinates() {
        assertNull(RemoteConnections.parseCommunityLocation("LAS:200,-73|Inválido"))
    }

    @Test
    fun mapsOnlyKnownDeployedRegionLocally() {
        assertEquals("LAS", communityRegionCodeFor("CL", "Región de Los Lagos"))
        assertEquals("", communityRegionCodeFor("AR", "Mendoza"))
    }
}
