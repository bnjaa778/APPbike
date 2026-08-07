package com.example.appbike

import android.app.Notification
import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatNotificationsInstrumentedTest {
    @Test
    fun systemNotificationShowsSenderAndMessage() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        assumeTrue(ChatNotificationCenter.canPostNotifications(context))
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.cancelAll()
        val event = MessageNotificationEvent(
            recipientUserId = "notification-test-user",
            chatId = "notification-test-chat",
            messageId = "notification-test-message",
            chatType = ChatType.SOCIAL,
            senderName = "ciclista.prueba",
            message = "Mensaje de prueba",
            chatTitle = "Chat de prueba"
        )
        val targetIntent = ChatNotificationCenter.messageNotificationIntent(context, event)
        assertEquals(
            event.recipientUserId,
            targetIntent.getStringExtra(ChatNotificationCenter.EXTRA_RECIPIENT_USER_ID)
        )
        assertEquals(event.chatId, targetIntent.getStringExtra(ChatNotificationCenter.EXTRA_CHAT_ID))

        ChatNotificationCenter.showSystemNotification(context, event)

        val posted = manager.activeNotifications.firstOrNull()
        assertNotNull(posted)
        assertEquals(
            "ciclista.prueba",
            posted!!.notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        )
        assertEquals(
            "Mensaje de prueba",
            posted.notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        )
        manager.cancelAll()
    }
}
