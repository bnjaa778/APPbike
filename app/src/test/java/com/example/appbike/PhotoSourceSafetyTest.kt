package com.example.appbike

import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoSourceSafetyTest {
    @Test
    fun acceptsHttpsZizzioAndSafeRelativePaths() {
        assertEquals(
            "https://api.zizzio.cl/media/photo.webp",
            RemoteConnections.safePublicPhotoUrl("https://api.zizzio.cl/media/photo.webp")
        )
        assertEquals(
            "https://api.zizzio.cl/uploads/photo.jpg",
            RemoteConnections.safePublicPhotoUrl("uploads/photo.jpg")
        )
    }

    @Test
    fun rejectsInternalPathsForeignHostsAndCleartext() {
        val unsafe = listOf(
            "BikesPhotos/PersonalBikesPhotos/123",
            "/srv/internal-auth/public/photo.jpg",
            "Z:\\srv\\internal-auth\\photo.jpg",
            "../private/photo.jpg",
            "https://cdn.example.com/tracker.jpg",
            "http://api.zizzio.cl/uploads/photo.jpg",
            "file:///srv/photo.jpg"
        )

        unsafe.forEach { source ->
            assertEquals("", RemoteConnections.safePublicPhotoUrl(source))
        }
    }
}
