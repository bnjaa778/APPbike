package com.example.appbike

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.PrimaryTabRow
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.appbike.ui.theme.AppBorderSubtle
import com.example.appbike.ui.theme.AppPrimaryBright
import com.example.appbike.ui.theme.AppPrimarySoft
import com.example.appbike.ui.theme.AppSurface
import com.example.appbike.ui.theme.AppSurfaceElevated
import com.example.appbike.ui.theme.AppTextPrimary
import com.example.appbike.ui.theme.AppTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val CHAT_POLL_INTERVAL_MS = 3_000L
private const val CHAT_PAGE_SIZE = 200

private data class ChatSyncResult(
    val mergedMessages: List<StoredMessage>,
    val resolvedCount: Int,
    val resolvedVersion: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    account: AccountSession?,
    initialChat: UserChat? = null,
    onInitialChatConsumed: () -> Unit = {},
    onOpenAccount: () -> Unit = {},
    onOpenMeetup: (String) -> Unit = {},
    isActive: Boolean = true
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accountKey = account?.userId.orEmpty()
    val chats = remember(accountKey) { mutableStateListOf<UserChat>() }
    val messages = remember(accountKey) { mutableStateListOf<StoredMessage>() }
    var selectedType by remember(accountKey) { mutableStateOf(ChatType.SOCIAL) }
    var selectedChat by remember(accountKey) { mutableStateOf<UserChat?>(null) }
    var loadingChats by remember(accountKey) { mutableStateOf(false) }
    var refreshingChats by remember(accountKey) { mutableStateOf(false) }
    var syncingMessageChatId by remember(accountKey) { mutableStateOf<String?>(null) }
    var sendingMessageChatId by remember(accountKey) { mutableStateOf<String?>(null) }
    var error by remember(accountKey) { mutableStateOf<String?>(null) }
    var hasLoaded by remember(accountKey) { mutableStateOf(false) }
    val chatListMutex = remember(accountKey) { Mutex() }
    val chatOperationMutexes = remember(accountKey) { mutableMapOf<String, Mutex>() }

    fun operationMutex(chatId: String): Mutex =
        chatOperationMutexes.getOrPut(chatId) { Mutex() }

    suspend fun refreshChatList(session: AccountSession, initialLoad: Boolean) {
        if (!chatListMutex.tryLock()) return
        try {
            if (initialLoad) loadingChats = true else refreshingChats = true
            runSuspendCatching {
                withContext(Dispatchers.IO) {
                    RemoteConnections.loadUserChats(session.userId).also { remote ->
                        LocalDataStore.saveChats(context, session.userId, remote)
                    }
                }
            }.onSuccess { remote ->
                chats.clear()
                chats.addAll(remote)
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
        } finally {
            loadingChats = false
            refreshingChats = false
            chatListMutex.unlock()
        }
    }

    suspend fun syncChatMessages(
        session: AccountSession,
        chat: UserChat,
        forceFullHistory: Boolean,
        showProgress: Boolean
    ) {
        operationMutex(chat.id).withLock {
            if (showProgress) syncingMessageChatId = chat.id
            try {
                val visibleLocal = messages.toList().takeIf { selectedChat?.id == chat.id }
                val result = runSuspendCatching {
                    withContext(Dispatchers.IO) {
                        val local = visibleLocal
                            ?: LocalDataStore.loadMessages(context, session.userId, chat.id)
                        val firstCursor = if (forceFullHistory) {
                            ""
                        } else {
                            local.lastOrNull()?.id.orEmpty()
                        }
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

                        val page = ChatMessagePage(
                            messages = downloaded,
                            messageCount = highestCount,
                            version = highestVersion,
                            hasMore = false
                        )
                        val merged = mergeStoredMessages(local, page.messages)
                        val resolvedCount = maxOf(chat.messageCount, page.messageCount, merged.size)
                        val resolvedVersion = maxOf(chat.version, page.version)
                        LocalDataStore.saveMessages(context, session.userId, chat.id, merged)
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
                        ChatSyncResult(merged, resolvedCount, resolvedVersion)
                    }
                }

                result.onSuccess { sync ->
                    if (selectedChat?.id == chat.id) {
                        messages.clear()
                        messages.addAll(sync.mergedMessages)
                    }
                    val latest = sync.mergedMessages.lastOrNull()
                    val chatIndex = chats.indexOfFirst { it.id == chat.id }
                    if (chatIndex >= 0) {
                        chats[chatIndex] = chats[chatIndex].copy(
                            lastMessage = latest?.content.orEmpty(),
                            lastMessageId = latest?.id.orEmpty(),
                            lastMessageSenderId = latest?.senderId.orEmpty(),
                            lastMessageSenderUsername = latest?.senderUsername,
                            messageCount = sync.resolvedCount,
                            version = sync.resolvedVersion,
                            updatedAt = latest?.createdAt.orEmpty()
                        )
                        if (selectedChat?.id == chat.id) selectedChat = chats[chatIndex]
                    }
                    error = null
                }.onFailure {
                    if (selectedChat?.id == chat.id) {
                        error = RemoteConnections.userFriendlyError(it)
                    }
                }

                if (result.isSuccess) {
                    val chatsSnapshot = chats.toList()
                    withContext(Dispatchers.IO) {
                        LocalDataStore.saveChats(context, session.userId, chatsSnapshot)
                    }
                }
            } finally {
                if (showProgress && syncingMessageChatId == chat.id) {
                    syncingMessageChatId = null
                }
            }
        }
    }

    fun openChat(chat: UserChat) {
        if (account == null) return
        selectedChat = chat
        selectedType = chat.type
        messages.clear()
    }

    LaunchedEffect(account?.userId, isActive) {
        if (!isActive || hasLoaded) return@LaunchedEffect
        chats.clear()
        messages.clear()
        selectedChat = null
        error = null
        val session = account ?: return@LaunchedEffect
        val local = withContext(Dispatchers.IO) {
            LocalDataStore.loadChats(context, session.userId)
        }
        chats.addAll(local)
        refreshChatList(session, initialLoad = true)
        hasLoaded = true
    }

    LaunchedEffect(account?.userId, initialChat?.id, isActive) {
        if (!isActive) return@LaunchedEffect
        val session = account ?: return@LaunchedEffect
        val target = initialChat ?: return@LaunchedEffect
        if (chats.none { it.id == target.id }) {
            chats.add(0, target)
            val chatsSnapshot = chats.toList()
            withContext(Dispatchers.IO) {
                LocalDataStore.saveChats(context, session.userId, chatsSnapshot)
            }
        }
        openChat(target)
        onInitialChatConsumed()
    }

    LaunchedEffect(account?.userId, selectedChat?.id, isActive) {
        if (!isActive) return@LaunchedEffect
        val session = account ?: return@LaunchedEffect
        val activeChat = selectedChat ?: return@LaunchedEffect
        val local = withContext(Dispatchers.IO) {
            LocalDataStore.loadMessages(context, session.userId, activeChat.id)
        }
        if (selectedChat?.id != activeChat.id) return@LaunchedEffect
        messages.clear()
        messages.addAll(local)
        // Opening a conversation always repairs historical cache gaps. This effect is
        // cancelled when the user changes chats, so an older screen cannot own the UI.
        syncChatMessages(
            session = session,
            chat = activeChat,
            forceFullHistory = true,
            showProgress = local.isEmpty()
        )
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
        PremiumScreenBackground(PremiumGlowStyle.Chat) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(AppDimens.Space4),
                verticalArrangement = Arrangement.Center
            ) {
                item {
                    EmptyState(
                        title = "Inicia sesión para ver tus conversaciones",
                        description = "Tus mensajes de juntas y Marketplace se sincronizan al entrar con tu cuenta.",
                        actionLabel = "Iniciar sesión",
                        onAction = onOpenAccount
                    )
                }
            }
        }
        return
    }

    PremiumScreenBackground(PremiumGlowStyle.Chat) {
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
                        Modifier.fillMaxSize().padding(AppDimens.Space4),
                        contentAlignment = Alignment.Center
                    ) { LoadingState("Cargando conversaciones...") }

                    visibleChats.isEmpty() -> {
                        if (error == null) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(AppDimens.Space4),
                                verticalArrangement = Arrangement.Center
                            ) {
                                item {
                                    EmptyState(
                                        title = "No tienes chats en esta categoría",
                                        description = "Cuando contactes publicaciones o juntas, aparecerán aquí."
                                    )
                                }
                            }
                        } else {
                            Box(
                                Modifier.fillMaxSize().padding(AppDimens.Space4),
                                contentAlignment = Alignment.Center
                            ) {
                                ErrorBanner(error ?: "")
                            }
                        }
                    }

                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(AppDimens.Space4),
                        verticalArrangement = Arrangement.spacedBy(AppDimens.Space3)
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
                loading = syncingMessageChatId == activeChat.id,
                sending = sendingMessageChatId == activeChat.id,
                error = error,
                onOpenMeetup = onOpenMeetup,
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
                onSend = { content, onSent ->
                    if (sendingMessageChatId != null) return@ConversationView
                    sendingMessageChatId = activeChat.id
                    scope.launch {
                        try {
                            operationMutex(activeChat.id).withLock {
                                val visibleLocal = messages.toList().takeIf {
                                    selectedChat?.id == activeChat.id
                                }
                                val local = visibleLocal ?: withContext(Dispatchers.IO) {
                                    LocalDataStore.loadMessages(
                                        context,
                                        account.userId,
                                        activeChat.id
                                    )
                                }
                                val result = runSuspendCatching {
                                    withContext(Dispatchers.IO) {
                                        RemoteConnections.sendChatMessage(
                                            account.userId,
                                            activeChat.id,
                                            content
                                        )
                                    }
                                }
                                val failure = result.exceptionOrNull()
                                if (failure != null) {
                                    if (selectedChat?.id == activeChat.id) {
                                        error = RemoteConnections.userFriendlyError(failure)
                                    }
                                } else {
                                    val response = result.getOrThrow()
                                    val sent = response.copy(
                                        chatId = response.chatId.ifBlank { activeChat.id },
                                        senderId = response.senderId.ifBlank { account.userId },
                                        senderUsername = response.senderUsername ?: account.username
                                    )
                                    val merged = mergeStoredMessages(local, listOf(sent))
                                    if (selectedChat?.id == activeChat.id) {
                                        messages.clear()
                                        messages.addAll(merged)
                                    }
                                    val chatIndex = chats.indexOfFirst { it.id == activeChat.id }
                                    val currentChat = chats.getOrNull(chatIndex) ?: activeChat
                                    val resolvedCount = maxOf(
                                        currentChat.messageCount + 1,
                                        merged.size
                                    )
                                    if (chatIndex >= 0) {
                                        chats[chatIndex] = currentChat.copy(
                                            lastMessage = sent.content,
                                            lastMessageId = sent.id,
                                            lastMessageSenderId = sent.senderId,
                                            lastMessageSenderUsername = sent.senderUsername,
                                            messageCount = resolvedCount,
                                            updatedAt = sent.createdAt
                                        )
                                        if (selectedChat?.id == activeChat.id) {
                                            selectedChat = chats[chatIndex]
                                        }
                                    }
                                    val chatsSnapshot = chats.toList()
                                    withContext(Dispatchers.IO) {
                                        LocalDataStore.saveChats(
                                            context,
                                            account.userId,
                                            chatsSnapshot
                                        )
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
                                                messageCount = resolvedCount,
                                                version = currentChat.version,
                                                lastSync = System.currentTimeMillis()
                                            )
                                        )
                                    }
                                    if (selectedChat?.id == activeChat.id) {
                                        error = null
                                        onSent()
                                    }
                                }
                            }
                        } finally {
                            if (sendingMessageChatId == activeChat.id) {
                                sendingMessageChatId = null
                            }
                        }
                    }
                }
            )
        }
        }
    }
}

@Composable
private fun ChatTypeTabs(selected: ChatType, onSelect: (ChatType) -> Unit) {
    PrimaryTabRow(
        selectedTabIndex = if (selected == ChatType.SOCIAL) 0 else 1,
        containerColor = AppSurface,
        contentColor = AppTextPrimary
    ) {
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
        shape = RoundedCornerShape(AppDimens.RadiusLarge),
        color = AppSurfaceElevated,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            AppBorderSubtle
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(AppDimens.Space4),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Space3),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = AppPrimarySoft
            ) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (chat.type == ChatType.MARKETPLACE) {
                            Icons.Outlined.Storefront
                        } else {
                            Icons.AutoMirrored.Outlined.DirectionsBike
                        },
                        contentDescription = null,
                        tint = AppPrimaryBright
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppDimens.Space1)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = chatDisplayTitle(chat, currentUserId),
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Bold,
                        color = AppTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (chat.type == ChatType.MARKETPLACE) "COMPRA" else "JUNTA",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AppPrimaryBright
                    )
                }
                Text(
                    text = chat.lastMessage.ifBlank { "Sin mensajes" },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = AppTextSecondary
                )
            }
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
    onOpenMeetup: (String) -> Unit,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSend: (String, () -> Unit) -> Unit
) {
    var draft by remember(chat.id) { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = AppDimens.Space3, vertical = AppDimens.Space2),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Space2),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, enabled = !sending) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
            }
            Text(
                chatDisplayTitle(chat, account.userId),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppTextPrimary
            )
            IconButton(onClick = onRefresh, enabled = !loading) {
                Icon(Icons.Outlined.Refresh, contentDescription = "Actualizar conversación")
            }
        }
        if (chat.type == ChatType.SOCIAL && !chat.relatedEntityId.isNullOrBlank()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppDimens.Space3),
                shape = RoundedCornerShape(AppDimens.RadiusMedium),
                color = AppPrimarySoft,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    AppPrimaryBright.copy(alpha = 0.28f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = AppDimens.Space3),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.DirectionsBike,
                        contentDescription = null,
                        tint = AppPrimaryBright
                    )
                    Text(
                        text = "Chat de junta",
                        modifier = Modifier.weight(1f).padding(start = AppDimens.Space2),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppTextPrimary
                    )
                    TextButton(onClick = { onOpenMeetup(chat.relatedEntityId) }) {
                        Text("Ver en mapa")
                    }
                }
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = AppDimens.Space3),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Space2)
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
                            AppPrimarySoft
                        } else {
                            AppSurfaceElevated
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (message.senderId == account.userId) {
                                AppPrimaryBright.copy(alpha = 0.24f)
                            } else {
                                AppBorderSubtle
                            }
                        )
                    ) {
                        Column(Modifier.padding(AppDimens.Space3)) {
                            if (message.senderId != account.userId) {
                                Text(
                                    message.senderUsername
                                        ?: chat.participantUsernames[message.senderId]
                                        ?: "Usuario de APPBIKE",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AppPrimaryBright,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(message.content, color = AppTextPrimary)
                        }
                    }
                }
            }
            if (loading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = AppPrimaryBright,
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
            error?.let { message ->
                item {
                    ErrorBanner(message, modifier = Modifier.padding(AppDimens.Space2))
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(AppDimens.Space3),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Space2),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it.take(2_000) },
                modifier = Modifier
                    .weight(1f)
                    .appBikeTextFieldGlow(),
                placeholder = { Text("Mensaje") },
                enabled = !sending,
                maxLines = 4,
                shape = RoundedCornerShape(AppDimens.RadiusMedium),
                colors = appBikeTextFieldColors()
            )
            Button(
                enabled = draft.isNotBlank() && !sending,
                onClick = {
                    val submittedDraft = draft.trim()
                    onSend(submittedDraft) {
                        if (draft.trim() == submittedDraft) draft = ""
                    }
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
