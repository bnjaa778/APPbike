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
