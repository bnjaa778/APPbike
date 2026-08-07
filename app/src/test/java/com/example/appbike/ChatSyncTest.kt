package com.example.appbike

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatSyncTest {
    @Test
    fun mergesIncrementalMessagesWithoutDuplicatesInOrder() {
        val first = StoredMessage("1", "chat", "a", "Uno", "2026-01-01T10:00:00Z")
        val second = StoredMessage("2", "chat", "b", "Dos", "2026-01-01T10:01:00Z")
        val merged = mergeStoredMessages(listOf(first), listOf(first, second))

        assertEquals(listOf("1", "2"), merged.map(StoredMessage::id))
    }

    @Test
    fun downloadedVersionRepairsStaleCachedMessage() {
        val cached = StoredMessage(
            "1",
            "chat",
            "a",
            "Texto pendiente",
            "2026-01-01T10:00:00Z",
            localStatus = "pending"
        )
        val remote = cached.copy(
            content = "Texto confirmado",
            localStatus = null,
            senderUsername = "ciclista.a"
        )

        val merged = mergeStoredMessages(listOf(cached), listOf(remote))

        assertEquals("Texto confirmado", merged.single().content)
        assertEquals(null, merged.single().localStatus)
        assertEquals("ciclista.a", merged.single().senderUsername)
    }

    @Test
    fun numericMessageIdsUseNumericOrderWhenTimestampMatches() {
        val tenth = StoredMessage("10", "chat", "a", "Décimo", "")
        val second = StoredMessage("2", "chat", "b", "Segundo", "")

        val merged = mergeStoredMessages(listOf(tenth), listOf(second))

        assertEquals(listOf("2", "10"), merged.map(StoredMessage::id))
        assertEquals("10", merged.last().id)
    }
}
