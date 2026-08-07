package com.example.appbike

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatExchangeInstrumentedTest {
    @Test
    fun exchangesMessagesWithActiveAccount() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val session = AccountStore.loadSession(context)
        assumeTrue(
            "Chat remoto requiere una sesión de prueba activa en el dispositivo.",
            session != null
        )
        session ?: return@runBlocking
        RemoteConnections.setSession(session)

        val arguments = InstrumentationRegistry.getArguments()
        val requestedChatId = arguments.getString("chatId").orEmpty()
        val sendText = arguments.getString("sendText").orEmpty()
        val expectedText = arguments.getString("expectText").orEmpty()

        val chats = withContext(Dispatchers.IO) {
            RemoteConnections.loadUserChats(session.userId)
        }
        val chat = chats.firstOrNull { it.id == requestedChatId }
            ?: chats.firstOrNull { it.type == ChatType.MARKETPLACE }
        assertNotNull("La cuenta debe compartir un chat de Marketplace.", chat)
        chat ?: return@runBlocking

        val sent = if (sendText.isNotBlank()) {
            withContext(Dispatchers.IO) {
                RemoteConnections.sendChatMessage(session.userId, chat.id, sendText)
            }.also { delay(500) }
        } else {
            null
        }

        val page = withContext(Dispatchers.IO) {
            RemoteConnections.loadChatMessagePage(
                userId = session.userId,
                chatId = chat.id,
                afterMessageId = ""
            )
        }

        assertTrue("Los mensajes remotos deben tener ID.", page.messages.all { it.id.isNotBlank() })
        assertTrue(
            "Los mensajes remotos deben conservar el chat solicitado.",
            page.messages.all { it.chatId.isBlank() || it.chatId == chat.id }
        )
        if (sendText.isNotBlank()) {
            assertTrue(
                "El mensaje enviado debe aparecer en la lectura completa.",
                page.messages.any { it.content == sendText }
            )
        }
        if (expectedText.isNotBlank()) {
            assertTrue(
                "El mensaje del otro dispositivo debe llegar a esta cuenta.",
                page.messages.any { it.content == expectedText }
            )
        }

        Log.i(
            "APPbikeChatTest",
            "userSuffix=${session.userId.takeLast(6)} " +
                "chat=${chat.id} chats=${chats.size} messages=${page.messages.size} " +
                "remoteCount=${page.messageCount} version=${page.version} " +
                "sentId=${sent?.id.orEmpty()} expectedFound=" +
                (expectedText.isBlank() || page.messages.any { it.content == expectedText })
        )
    }
}
