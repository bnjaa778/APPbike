package com.example.appbike

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RemoteParsersInstrumentedTest {
    @Test
    fun bikeParserDoesNotExposeInternalPhotoPath() {
        val bike = RemoteConnections.bikeFromJson(
            JSONObject()
                .put("id", 42)
                .put("bike_custom_name", "Ruta")
                .put("photo_path", "BikesPhotos/PersonalBikesPhotos/42.jpg")
        )

        assertEquals("", bike.imageUri)
    }

    @Test
    fun marketplaceParserPreservesCurrencyDistanceAndCoverPhoto() {
        val item = JSONObject()
            .put("id", "LAS-abc")
            .put("title", "Casco")
            .put("price", "10000")
            .put("currency", "CLP")
            .put("user_id", "123e4567-e89b-42d3-a456-426614174000")
            .put("nombre_de_usuario", "ciclista.osorno")
            .put("publication_status", "activa")
            .put("product_status", "usado")
            .put("location", "LAS:-40.5,-73.1|Osorno")
            .put("distance_km", 2.5)
            .put("photo_id", "cover.webp")

        val post = RemoteConnections.marketplaceFromJson(item)

        assertEquals("CLP", post.currencyCode)
        assertEquals("ciclista.osorno", post.createdByUsername)
        assertEquals(2.5, post.distanceKm!!, 0.001)
        assertEquals("appbike-market-photo://LAS-abc/cover.webp", post.images.single())
    }

    @Test
    fun marketplaceParserIgnoresInternalPhotoObjectPath() {
        val post = RemoteConnections.marketplaceFromJson(
            JSONObject()
                .put("id", "LAS-private-photo")
                .put("title", "Casco")
                .put(
                    "photos",
                    JSONArray(
                        listOf(
                            JSONObject().put(
                                "path",
                                "/srv/internal-auth/public/AppBikeInternal/photo.jpg"
                            )
                        )
                    )
                )
        )

        assertTrue(post.images.isEmpty())
    }

    @Test
    fun weatherForecastParserKeepsSixDaysAndRainProbability() {
        val forecast = RemoteConnections.weatherForecastFromJson(
            JSONObject()
                .put("latitude", -33.45)
                .put("longitude", -70.67)
                .put(
                    "daily",
                    JSONObject()
                        .put(
                            "time",
                            JSONArray(
                                listOf(
                                    "2026-08-26",
                                    "2026-08-27",
                                    "2026-08-28",
                                    "2026-08-29",
                                    "2026-08-30",
                                    "2026-08-31",
                                    "2026-09-01"
                                )
                            )
                        )
                        .put("weather_code", JSONArray(listOf(0, 2, 3, 61, 80, 95, 99)))
                        .put(
                            "temperature_2m_max",
                            JSONArray(listOf(19.0, 18.0, 16.0, 14.0, 13.0, 15.0, 17.0))
                        )
                        .put(
                            "temperature_2m_min",
                            JSONArray(listOf(7.0, 8.0, 9.0, 10.0, 8.0, 7.0, 6.0))
                        )
                        .put(
                            "precipitation_probability_max",
                            JSONArray(listOf(0, 10, 20, 70, 85, 45, 30))
                        )
                ),
            requestedLatitude = -33.45,
            requestedLongitude = -70.67
        )

        assertEquals(6, forecast.days.size)
        assertEquals("2026-08-26", forecast.days.first().date)
        assertEquals(70, forecast.days[3].precipitationProbabilityPercent)
        assertEquals(WeatherCondition.RAIN, forecast.days[3].condition)
    }

    @Test
    fun meetupParserSeparatesEncodedDateFromVisibleDescription() {
        val meetup = RemoteConnections.meetupFromJson(
            JSONObject()
                .put("id", "LAS-meetup")
                .put("title", "Salida costera")
                .put("description", "Fecha y hora: 10 de agosto, 18:00\nRuta costera.")
                .put("location", "LAS:-41.47,-72.94|Puerto Montt")
        )

        assertEquals("10 de agosto, 18:00", meetup.dateTime)
        assertEquals("Ruta costera.", meetup.description)
    }

    @Test
    fun cyclingRouteParserKeepsStreetGeometryDistanceAndDuration() {
        val origin = GeoPoint(-33.4489, -70.6693, "Santiago")
        val destination = GeoPoint(-33.44, -70.64, "Destino")
        val response = JSONObject()
            .put("code", "Ok")
            .put(
                "routes",
                JSONArray().put(
                    JSONObject()
                        .put("distance", 2_981.3)
                        .put("duration", 765.8)
                        .put(
                            "geometry",
                            JSONObject().put(
                                "coordinates",
                                JSONArray()
                                    .put(JSONArray().put(-70.6693).put(-33.4489))
                                    .put(JSONArray().put(-70.655).put(-33.445))
                                    .put(JSONArray().put(-70.64).put(-33.44))
                            )
                        )
                )
            )

        val route = RemoteConnections.cyclingRouteFromJson(origin, destination, response)

        assertEquals(CyclingRouteSource.OPEN_STREET_MAP, route.source)
        assertEquals(2.9813, route.distanceKm, 0.0001)
        assertEquals(13, route.estimatedMinutes)
        assertEquals(3, route.geometry.size)
        assertEquals(destination.longitude, route.geometry.last().longitude, 0.0001)
        assertTrue(!route.isDirectEstimate)
    }

    @Test
    fun chatParserReadsTitleParticipantsAndVersions() {
        val chat = RemoteConnections.chatFromJson(
            JSONObject()
                .put("chat_id", "chat-1")
                .put("chat_type", "marketplace")
                .put("title", "Casco")
                .put(
                    "participants",
                    JSONArray(
                        listOf(
                            JSONObject()
                                .put("user_id", "uno")
                                .put("nombre_de_usuario", "pedalea.uno"),
                            JSONObject()
                                .put("user_id", "dos")
                                .put("nombre_de_usuario", "pedalea.dos")
                        )
                    )
                )
                .put(
                    "last_message",
                    JSONObject()
                        .put("id", "mensaje-4")
                        .put("content", "Hola")
                        .put("sender_id", "dos")
                        .put("sender_nombre_de_usuario", "pedalea.dos")
                )
                .put("message_count", 4)
                .put("version", 7)
        )

        assertEquals(ChatType.MARKETPLACE, chat.type)
        assertEquals("Casco", chat.title)
        assertEquals("Hola", chat.lastMessage)
        assertEquals(4, chat.messageCount)
        assertTrue(chat.participants.contains("dos"))
        assertEquals("pedalea.dos", chat.participantUsernames["dos"])
        assertEquals("mensaje-4", chat.lastMessageId)
        assertEquals("pedalea.dos", chat.lastMessageSenderUsername)
    }

    @Test
    fun messageParserReadsSenderUsername() {
        val message = RemoteConnections.messageFromJson(
            JSONObject()
                .put("message_id", "mensaje-5")
                .put("chat_id", "chat-1")
                .put("sender_id", "dos")
                .put("sender_nombre_de_usuario", "pedalea.dos")
                .put("content", "Nos vemos")
        )

        assertEquals("pedalea.dos", message.senderUsername)
    }
}
