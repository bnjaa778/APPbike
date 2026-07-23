package com.example.appbike

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.DirectionsBike
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val CHAT_POLL_INTERVAL_MS = 3_000L
private const val CHAT_PAGE_SIZE = 200

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    account: AccountSession?,
    initialChat: UserChat? = null,
    onInitialChatConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val chats = remember { mutableStateListOf<UserChat>() }
    val messages = remember { mutableStateListOf<StoredMessage>() }
    var selectedType by remember { mutableStateOf(ChatType.SOCIAL) }
    var selectedChat by remember { mutableStateOf<UserChat?>(null) }
    var loadingChats by remember { mutableStateOf(false) }
    var refreshingChats by remember { mutableStateOf(false) }
    var syncingMessages by remember { mutableStateOf(false) }
    var messageSyncInFlight by remember { mutableStateOf(false) }
    var sendingMessage by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun refreshChatList(session: AccountSession, initialLoad: Boolean) {
        if (initialLoad) loadingChats = true else refreshingChats = true
        runCatching {
            withContext(Dispatchers.IO) {
                RemoteConnections.loadUserChats(session.userId)
            }
        }.onSuccess { remote ->
            chats.clear()
            chats.addAll(remote)
            LocalDataStore.saveChats(context, session.userId, remote)
            selectedChat?.let { open ->
                remote.firstOrNull { it.id == open.id }?.let { updated ->
                    selectedChat = updated
                    selectedType = updated.type
                }
            }
            error = null
        }.onFailure {
            if (chats.isEmpty()) error = RemoteConnections.userFriendlyError(it)
        }
        loadingChats = false
        refreshingChats = false
    }

    suspend fun syncChatMessages(
        session: AccountSession,
        chat: UserChat,
        forceFullHistory: Boolean,
        showProgress: Boolean
    ) {
        if (messageSyncInFlight) return
        messageSyncInFlight = true
        if (showProgress) syncingMessages = true
        val local = if (selectedChat?.id == chat.id) {
            messages.toList()
        } else {
            LocalDataStore.loadMessages(context, session.userId, chat.id)
        }
        val firstCursor = if (forceFullHistory) "" else local.lastOrNull()?.id.orEmpty()
        runCatching {
            withContext(Dispatchers.IO) {
                val downloaded = mutableListOf<StoredMessage>()
                var cursor = firstCursor
                var highestCount = 0
                var highestVersion = 0L
                var pageNumber = 0
                do {
                    val page = RemoteConnections.loadChatMessagePage(
                        userId = session.userId,
                        chatId = chat.id,
                        afterMessageId = cursor
                    )
                    downloaded.addAll(page.messages)
                    highestCount = maxOf(highestCount, page.messageCount)
                    highestVersion = maxOf(highestVersion, page.version)
                    val nextCursor = page.messages.lastOrNull()?.id.orEmpty()
                    val canContinue = nextCursor.isNotBlank() && nextCursor != cursor &&
                        (page.hasMore || page.messages.size >= CHAT_PAGE_SIZE)
                    cursor = nextCursor
                    pageNumber += 1
                } while (canContinue && pageNumber < 20)
                ChatMessagePage(
                    messages = downloaded,
                    messageCount = highestCount,
                    version = highestVersion,
                    hasMore = false
                )
            }
        }.onSuccess { page ->
            val normalized = page.messages.map { message ->
                message.copy(chatId = message.chatId.ifBlank { chat.id })
            }
            val merged = mergeStoredMessages(local, normalized)
            if (selectedChat?.id == chat.id) {
                messages.clear()
                messages.addAll(merged)
            }
            LocalDataStore.saveMessages(context, session.userId, chat.id, merged)
            val resolvedCount = maxOf(chat.messageCount, page.messageCount, merged.size)
            val resolvedVersion = maxOf(chat.version, page.version)
            LocalDataStore.saveSync(
                context,
                session.userId,
                ChatSyncMetadata(
                    chatId = chat.id,
                    lastMessageId = merged.lastOrNull()?.id.orEmpty(),
                    messageCount = resolvedCount,
                    version = resolvedVersion,
                    lastSync = System.currentTimeMillis()
                )
            )
            val chatIndex = chats.indexOfFirst { it.id == chat.id }
            if (chatIndex >= 0) {
                chats[chatIndex] = chats[chatIndex].copy(
                    lastMessage = merged.lastOrNull()?.content.orEmpty(),
                    lastMessageId = merged.lastOrNull()?.id.orEmpty(),
                    lastMessageSenderId = merged.lastOrNull()?.senderId.orEmpty(),
                    lastMessageSenderUsername = merged.lastOrNull()?.senderUsername,
                    messageCount = resolvedCount,
                    version = resolvedVersion,
                    updatedAt = merged.lastOrNull()?.createdAt.orEmpty()
                )
                if (selectedChat?.id == chat.id) selectedChat = chats[chatIndex]
                LocalDataStore.saveChats(context, session.userId, chats)
            }
            error = null
        }.onFailure {
            error = RemoteConnections.userFriendlyError(it)
        }
        syncingMessages = false
        messageSyncInFlight = false
    }

    fun openChat(chat: UserChat) {
        val session = account ?: return
        selectedChat = chat
        selectedType = chat.type
        messages.clear()
        val local = LocalDataStore.loadMessages(context, session.userId, chat.id)
        messages.addAll(local)
        scope.launch {
            // A full sync on open repairs gaps left by an older incremental cache.
            syncChatMessages(
                session = session,
                chat = chat,
                forceFullHistory = true,
                showProgress = local.isEmpty()
            )
        }
    }

    LaunchedEffect(account?.userId) {
        chats.clear()
        messages.clear()
        selectedChat = null
        error = null
        val session = account ?: return@LaunchedEffect
        val local = LocalDataStore.loadChats(context, session.userId)
        chats.addAll(local)
        refreshChatList(session, initialLoad = true)
    }

    LaunchedEffect(account?.userId, initialChat?.id) {
        val session = account ?: return@LaunchedEffect
        val target = initialChat ?: return@LaunchedEffect
        if (chats.none { it.id == target.id }) {
            chats.add(0, target)
            LocalDataStore.saveChats(context, session.userId, chats)
        }
        openChat(target)
        onInitialChatConsumed()
    }

    LaunchedEffect(account?.userId, selectedChat?.id) {
        val session = account ?: return@LaunchedEffect
        val activeChat = selectedChat ?: return@LaunchedEffect
        while (true) {
            delay(CHAT_POLL_INTERVAL_MS)
            syncChatMessages(
                session = session,
                chat = activeChat,
                forceFullHistory = false,
                showProgress = false
            )
        }
    }

    if (account == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "Inicia sesión para ver tus conversaciones.",
                style = MaterialTheme.typography.titleLarge
            )
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        ChatTypeTabs(selectedType) {
            selectedType = it
            selectedChat = null
            messages.clear()
            error = null
        }

        if (selectedChat == null) {
            val visibleChats = chats.filter { it.type == selectedType }
            PullToRefreshBox(
                isRefreshing = refreshingChats,
                onRefresh = {
                    scope.launch { refreshChatList(account, initialLoad = false) }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    loadingChats && chats.isEmpty() -> Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }

                    visibleChats.isEmpty() -> Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            error ?: "No tienes chats en esta categoría.",
                            color = if (error == null) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                    }

                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(visibleChats, key = UserChat::id) { chat ->
                            ChatRow(chat, account.userId) { openChat(chat) }
                        }
                    }
                }
            }
        } else {
            val activeChat = selectedChat!!
            ConversationView(
                account = account,
                chat = activeChat,
                messages = messages,
                loading = syncingMessages,
                sending = sendingMessage,
                error = error,
                onBack = {
                    selectedChat = null
                    messages.clear()
                    error = null
                },
                onRefresh = {
                    scope.launch {
                        syncChatMessages(
                            session = account,
                            chat = activeChat,
                            forceFullHistory = true,
                            showProgress = true
                        )
                    }
                },
                onSend = { content ->
                    scope.launch {
                        sendingMessage = true
                        runCatching {
                            withContext(Dispatchers.IO) {
                                RemoteConnections.sendChatMessage(
                                    account.userId,
                                    activeChat.id,
                                    content
                                )
                            }
                        }.onSuccess { response ->
                            val sent = response.copy(
                                chatId = response.chatId.ifBlank { activeChat.id },
                                senderId = response.senderId.ifBlank { account.userId },
                                senderUsername = response.senderUsername ?: account.username
                            )
                            val merged = mergeStoredMessages(messages.toList(), listOf(sent))
                            messages.clear()
                            messages.addAll(merged)
                            val chatIndex = chats.indexOfFirst { it.id == activeChat.id }
                            if (chatIndex >= 0) {
                                chats[chatIndex] = chats[chatIndex].copy(
                                    lastMessage = sent.content,
                                    lastMessageId = sent.id,
                                    lastMessageSenderId = sent.senderId,
                                    lastMessageSenderUsername = sent.senderUsername,
                                    messageCount = maxOf(
                                        chats[chatIndex].messageCount + 1,
                                        merged.size
                                    ),
                                    updatedAt = sent.createdAt
                                )
                                selectedChat = chats[chatIndex]
                                LocalDataStore.saveChats(context, account.userId, chats)
                            }
                            LocalDataStore.saveMessages(
                                context,
                                account.userId,
                                activeChat.id,
                                merged
                            )
                            LocalDataStore.saveSync(
                                context,
                                account.userId,
                                ChatSyncMetadata(
                                    chatId = activeChat.id,
                                    lastMessageId = sent.id,
                                    messageCount = maxOf(activeChat.messageCount + 1, merged.size),
                                    version = activeChat.version,
                                    lastSync = System.currentTimeMillis()
                                )
                            )
                            error = null
                        }.onFailure { error = RemoteConnections.userFriendlyError(it) }
                        sendingMessage = false
                    }
                }
            )
        }
    }
}

@Composable
private fun ChatTypeTabs(selected: ChatType, onSelect: (ChatType) -> Unit) {
    TabRow(selectedTabIndex = if (selected == ChatType.SOCIAL) 0 else 1) {
        Tab(
            selected = selected == ChatType.SOCIAL,
            onClick = { onSelect(ChatType.SOCIAL) },
            text = { Text("Juntas y amigos") },
            icon = {
                Icon(
                    Icons.AutoMirrored.Outlined.DirectionsBike,
                    contentDescription = null
                )
            }
        )
        Tab(
            selected = selected == ChatType.MARKETPLACE,
            onClick = { onSelect(ChatType.MARKETPLACE) },
            text = { Text("Marketplace") },
            icon = {
                Row {
                    Icon(Icons.Outlined.Storefront, contentDescription = null)
                    Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null)
                }
            }
        )
    }
}

@Composable
private fun ChatRow(chat: UserChat, currentUserId: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 2.dp
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                chatDisplayTitle(chat, currentUserId),
                fontWeight = FontWeight.Bold
            )
            Text(
                chat.lastMessage.ifBlank { "Sin mensajes" },
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ConversationView(
    account: AccountSession,
    chat: UserChat,
    messages: List<StoredMessage>,
    loading: Boolean,
    sending: Boolean,
    error: String?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSend: (String) -> Unit
) {
    var draft by remember(chat.id) { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
            }
            Text(
                chatDisplayTitle(chat, account.userId),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onRefresh, enabled = !loading) {
                Icon(Icons.Outlined.Refresh, contentDescription = "Actualizar conversación")
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = StoredMessage::id) { message ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (message.senderId == account.userId) {
                        Arrangement.End
                    } else {
                        Arrangement.Start
                    }
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (message.senderId == account.userId) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        }
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            if (message.senderId != account.userId) {
                                Text(
                                    message.senderUsername
                                        ?: chat.participantUsernames[message.senderId]
                                        ?: "Usuario de APPBIKE",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(message.content)
                        }
                    }
                }
            }
            if (loading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                }
            }
            error?.let { message ->
                item {
                    Text(
                        message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Mensaje") },
                shape = RoundedCornerShape(18.dp)
            )
            Button(
                enabled = draft.isNotBlank() && !sending,
                onClick = {
                    onSend(draft)
                    draft = ""
                }
            ) {
                if (sending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "Enviar")
                }
            }
        }
    }
}

private fun chatDisplayTitle(chat: UserChat, currentUserId: String): String =
    chat.title.takeIf(String::isNotBlank)
        ?: chat.participants
            .asSequence()
            .filter { it != currentUserId }
            .mapNotNull(chat.participantUsernames::get)
            .firstOrNull()
        ?: chat.participantUsernames.values.firstOrNull()
        ?: "Conversación"
