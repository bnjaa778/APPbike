package com.example.appbike

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ChatIdentityValidationTest {
    @Test
    fun acceptsStableChatAndMessageIds() {
        val chat = UserChat("chat-1", ChatType.SOCIAL, emptyList())
        val message = StoredMessage("message-1", "chat-1", "user-1", "Hola", "2026-08-05")

        assertEquals(listOf(chat), RemoteConnections.validateChatIds(listOf(chat)))
        assertEquals(listOf(message), RemoteConnections.validateMessageIds(listOf(message)))
    }

    @Test
    fun rejectsBlankIdsBeforeTheyReachComposeOrCache() {
        assertThrows(RemoteConnections.RemoteConnectionException::class.java) {
            RemoteConnections.validateChatIds(
                listOf(UserChat("", ChatType.MARKETPLACE, emptyList()))
            )
        }
        assertThrows(RemoteConnections.RemoteConnectionException::class.java) {
            RemoteConnections.validateMessageIds(
                listOf(StoredMessage("", "chat-1", "user-1", "Hola", "2026-08-05"))
            )
        }
    }

    @Test
    fun normalizesMissingChatIdAndRejectsCrossChatMessages() {
        val legacy = StoredMessage("message-1", "", "user-1", "Hola", "2026-08-06")
        val normalized = RemoteConnections.validateMessagesForChat(
            chatId = "chat-1",
            messages = listOf(legacy)
        )

        assertEquals("chat-1", normalized.single().chatId)
        assertThrows(RemoteConnections.RemoteConnectionException::class.java) {
            RemoteConnections.validateMessagesForChat(
                chatId = "chat-1",
                messages = listOf(legacy.copy(chatId = "chat-2"))
            )
        }
    }
}
