package com.example.appbike

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class MessageNotificationEvent(
    val recipientUserId: String,
    val chatId: String,
    val messageId: String,
    val chatType: ChatType,
    val senderName: String,
    val message: String,
    val chatTitle: String
) {
    fun asChat(): UserChat = UserChat(
        id = chatId,
        type = chatType,
        participants = emptyList(),
        title = chatTitle
    )
}

internal fun isNotificationForActiveUser(
    recipientUserId: String,
    activeUserId: String?
): Boolean = recipientUserId.isNotBlank() && recipientUserId == activeUserId

object ChatNotificationEventBus {
    private val mutableEvents = MutableSharedFlow<MessageNotificationEvent>(
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events = mutableEvents.asSharedFlow()

    fun publish(event: MessageNotificationEvent) {
        mutableEvents.tryEmit(event)
    }
}

object ChatNotificationCenter {
    const val EXTRA_CHAT_ID = "notification_chat_id"
    const val EXTRA_CHAT_TYPE = "notification_chat_type"
    const val EXTRA_CHAT_TITLE = "notification_chat_title"
    const val EXTRA_RECIPIENT_USER_ID = "notification_recipient_user_id"

    private const val CHANNEL_ID = "appbike_new_messages"
    private const val LISTENER_CHANNEL_ID = "appbike_message_listener"
    internal const val LISTENER_NOTIFICATION_ID = 41_002
    private const val PERIODIC_WORK_NAME = "appbike_chat_notifications"

    @Volatile
    var appInForeground: Boolean = false

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val messageChannel = NotificationChannel(
            CHANNEL_ID,
            "Mensajes nuevos",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Mensajes recibidos en chats de APPBIKE"
            enableVibration(true)
        }
        val listenerChannel = NotificationChannel(
            LISTENER_CHANNEL_ID,
            "Conexión de mensajes",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Mantiene activa la recepción de mensajes fuera de APPBIKE"
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }
        manager.createNotificationChannels(listOf(messageChannel, listenerChannel))
    }

    fun startListener(context: Context) {
        createChannel(context)
        try {
            ContextCompat.startForegroundService(
                context.applicationContext,
                Intent(context.applicationContext, ChatNotificationListenerService::class.java)
            )
        } catch (error: IllegalStateException) {
            // Android can reject a foreground-service start after the activity
            // loses its launch exemption. Notifications are supplementary and
            // must never prevent APPbike from opening.
            if (
                Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                error::class.java.name !=
                    "android.app.ForegroundServiceStartNotAllowedException"
            ) {
                throw error
            }
        } catch (_: SecurityException) {
            // Missing/revoked foreground-service permission must not crash the app.
        }
    }

    fun stopListener(context: Context) {
        context.applicationContext.stopService(
            Intent(context.applicationContext, ChatNotificationListenerService::class.java)
        )
    }

    internal fun listenerNotification(context: Context) =
        NotificationCompat.Builder(context, LISTENER_CHANNEL_ID)
            .setSmallIcon(R.drawable.appbike_notification_icon)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.appbike_brand_icon))
            .setContentTitle("APPBIKE")
            .setContentText("Escuchando mensajes nuevos")
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    LISTENER_NOTIFICATION_ID,
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .build()

    internal fun dispatch(context: Context, event: MessageNotificationEvent) {
        val activeUserId = AccountStore.loadSession(context)?.userId
        if (!isNotificationForActiveUser(event.recipientUserId, activeUserId)) return
        if (appInForeground) {
            ChatNotificationEventBus.publish(event)
        } else {
            showSystemNotification(context, event)
        }
    }

    fun scheduleBackgroundChecks(context: Context) {
        val request = PeriodicWorkRequestBuilder<ChatNotificationWorker>(
            15,
            TimeUnit.MINUTES
        ).setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        ).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelBackgroundChecks(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
    }

    fun clearNotifications(context: Context) {
        NotificationManagerCompat.from(context).cancelAll()
    }

    fun canPostNotifications(context: Context): Boolean {
        val runtimePermissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        return runtimePermissionGranted &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun showSystemNotification(context: Context, event: MessageNotificationEvent) {
        if (!canPostNotifications(context)) return
        createChannel(context)
        val intent = messageNotificationIntent(context, event)
        val pendingIntent = PendingIntent.getActivity(
            context,
            "${event.recipientUserId}:${event.chatId}".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.appbike_notification_icon)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.appbike_brand_icon))
            .setContentTitle(event.senderName)
            .setContentText(event.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(event.message))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(
                buildString {
                    append(event.recipientUserId)
                    append(':')
                    append(event.chatId)
                    append(':')
                    append(event.messageId.ifBlank { event.message })
                }.hashCode(),
                notification
            )
        } catch (_: SecurityException) {
            // Android 13+ can revoke POST_NOTIFICATIONS after the pre-check.
        }
    }

    internal fun messageNotificationIntent(
        context: Context,
        event: MessageNotificationEvent
    ): Intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        putExtra(EXTRA_RECIPIENT_USER_ID, event.recipientUserId)
        putExtra(EXTRA_CHAT_ID, event.chatId)
        putExtra(EXTRA_CHAT_TYPE, event.chatType.name)
        putExtra(EXTRA_CHAT_TITLE, event.chatTitle)
    }
}

class ChatNotificationListenerService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var listenerJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        ChatNotificationCenter.createChannel(this)
        val notification = ChatNotificationCenter.listenerNotification(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                ChatNotificationCenter.LISTENER_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
            )
        } else {
            startForeground(ChatNotificationCenter.LISTENER_NOTIFICATION_ID, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (listenerJob?.isActive == true) return START_STICKY
        listenerJob = serviceScope.launch {
            while (isActive) {
                val session = AccountStore.loadSession(applicationContext)
                if (session == null) {
                    stopSelf()
                    break
                }
                RemoteConnections.setSession(session)
                runSuspendCatching {
                    ChatNotificationRepository.sync(applicationContext, session)
                }.onSuccess { events ->
                    events.forEach { event ->
                        ChatNotificationCenter.dispatch(applicationContext, event)
                    }
                }
                delay(6_000L)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        listenerJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

object ChatNotificationRepository {
    private val syncMutex = Mutex()

    suspend fun sync(
        context: Context,
        session: AccountSession
    ): List<MessageNotificationEvent> = syncMutex.withLock {
        withContext(Dispatchers.IO) {
            val chats = RemoteConnections.loadUserChats(session.userId)
            chats.flatMap { chat -> syncChat(context, session, chat) }
        }
    }

    private fun syncChat(
        context: Context,
        session: AccountSession,
        chat: UserChat
    ): List<MessageNotificationEvent> {
        val saved = LocalDataStore.loadNotificationSync(
            context,
            session.userId,
            chat.id
        )
        if (saved == null) {
            val latestId = chat.lastMessageId.ifBlank {
                if (chat.messageCount > 0 || chat.lastMessage.isNotBlank()) {
                    RemoteConnections.loadChatMessagePage(
                        session.userId,
                        chat.id
                    ).messages.lastOrNull()?.id.orEmpty()
                } else {
                    ""
                }
            }
            LocalDataStore.saveNotificationSync(
                context,
                session.userId,
                ChatNotificationSyncMetadata(
                    chatId = chat.id,
                    lastMessageId = latestId,
                    messageCount = chat.messageCount,
                    version = chat.version
                )
            )
            return emptyList()
        }

        val changed = when {
            chat.lastMessageId.isNotBlank() -> chat.lastMessageId != saved.lastMessageId
            chat.messageCount > saved.messageCount -> true
            chat.version > saved.version -> true
            else -> false
        }
        if (!changed) return emptyList()

        val page = RemoteConnections.loadChatMessagePage(
            userId = session.userId,
            chatId = chat.id,
            afterMessageId = saved.lastMessageId
        )
        val downloaded = if (
            page.messages.isEmpty() &&
            (chat.lastMessageId.isNotBlank() || chat.messageCount > saved.messageCount)
        ) {
            RemoteConnections.loadChatMessagePage(
                userId = session.userId,
                chatId = chat.id
            ).messages.messagesAfter(saved.lastMessageId)
        } else {
            page.messages.messagesAfter(saved.lastMessageId)
        }

        val latestId = downloaded.lastOrNull()?.id
            ?: chat.lastMessageId.takeIf(String::isNotBlank)
            ?: saved.lastMessageId
        LocalDataStore.saveNotificationSync(
            context,
            session.userId,
            ChatNotificationSyncMetadata(
                chatId = chat.id,
                lastMessageId = latestId,
                messageCount = maxOf(saved.messageCount, chat.messageCount, page.messageCount),
                version = maxOf(saved.version, chat.version, page.version)
            )
        )

        return downloaded
            .asSequence()
            .filter { it.senderId.isNotBlank() && it.senderId != session.userId }
            .filter { it.content.isNotBlank() }
            .map { message ->
                val senderName = message.senderUsername
                    ?: chat.participantUsernames[message.senderId]
                    ?: chat.lastMessageSenderUsername.takeIf {
                        chat.lastMessageSenderId == message.senderId
                    }
                    ?: "Usuario de APPBIKE"
                MessageNotificationEvent(
                    recipientUserId = session.userId,
                    chatId = chat.id,
                    messageId = message.id,
                    chatType = chat.type,
                    senderName = senderName,
                    message = message.content,
                    chatTitle = chat.title.ifBlank { senderName }
                )
            }
            .toList()
    }

    private fun List<StoredMessage>.messagesAfter(messageId: String): List<StoredMessage> {
        if (messageId.isBlank()) return this
        val cursorIndex = indexOfFirst { it.id == messageId }
        return if (cursorIndex >= 0) drop(cursorIndex + 1) else this
    }
}

class ChatNotificationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        if (ChatNotificationCenter.appInForeground) return Result.success()
        val session = AccountStore.loadSession(applicationContext) ?: return Result.success()
        RemoteConnections.setSession(session)
        return runSuspendCatching {
            ChatNotificationRepository.sync(applicationContext, session).forEach { event ->
                ChatNotificationCenter.dispatch(applicationContext, event)
            }
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { if (runAttemptCount < 3) Result.retry() else Result.success() }
        )
    }
}
