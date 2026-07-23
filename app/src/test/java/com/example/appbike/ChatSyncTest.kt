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
}
