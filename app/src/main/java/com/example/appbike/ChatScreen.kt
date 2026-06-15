package com.example.appbike

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsBike
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ChatScreen(account: AccountSession?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val chats = remember { mutableStateListOf<UserChat>() }
    val messages = remember { mutableStateListOf<StoredMessage>() }
    var selectedType by remember { mutableStateOf(ChatType.SOCIAL) }
    var selectedChat by remember { mutableStateOf<UserChat?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(account?.userId) {
        chats.clear()
        messages.clear()
        selectedChat = null
        val session = account ?: return@LaunchedEffect
        val local = LocalDataStore.loadChats(context, session.userId)
        chats.addAll(local)
        loading = true
        runCatching {
            withContext(Dispatchers.IO) {
                RemoteConnections.loadUserChats(session.userId)
            }
        }.onSuccess { remote ->
            chats.clear()
            chats.addAll(remote)
            LocalDataStore.saveChats(context, session.userId, remote)
        }.onFailure {
            if (local.isEmpty()) error = RemoteConnections.userFriendlyError(it)
        }
        loading = false
    }

    fun openChat(chat: UserChat) {
        val session = account ?: return
        selectedChat = chat
        messages.clear()
        val local = LocalDataStore.loadMessages(context, session.userId, chat.id)
        messages.addAll(local)
        val metadata = LocalDataStore.loadSync(context, session.userId, chat.id)
        val isOutdated = local.isEmpty() ||
            local.size < chat.messageCount ||
            (metadata?.version ?: 0) < chat.version
        if (!isOutdated) return

        scope.launch {
            loading = true
            runCatching {
                withContext(Dispatchers.IO) {
                    RemoteConnections.loadChatMessages(
                        userId = session.userId,
                        chatId = chat.id,
                        afterMessageId = local.lastOrNull()?.id.orEmpty()
                    )
                }
            }.onSuccess { downloaded ->
                val merged = (local + downloaded).distinctBy { it.id }
                messages.clear()
                messages.addAll(merged)
                LocalDataStore.saveMessages(context, session.userId, chat.id, merged)
                LocalDataStore.saveSync(
                    context,
                    session.userId,
                    ChatSyncMetadata(
                        chatId = chat.id,
                        lastMessageId = merged.lastOrNull()?.id.orEmpty(),
                        messageCount = maxOf(chat.messageCount, merged.size),
                        version = chat.version,
                        lastSync = System.currentTimeMillis()
                    )
                )
            }.onFailure { error = RemoteConnections.userFriendlyError(it) }
            loading = false
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
        }

        if (selectedChat == null) {
            val visibleChats = chats.filter { it.type == selectedType }
            when {
                loading && chats.isEmpty() -> Box(
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
                        ChatRow(chat) { openChat(chat) }
                    }
                }
            }
        } else {
            ConversationView(
                account = account,
                chat = selectedChat!!,
                messages = messages,
                loading = loading,
                onBack = {
                    selectedChat = null
                    messages.clear()
                },
                onSend = { content ->
                    scope.launch {
                        runCatching {
                            withContext(Dispatchers.IO) {
                                RemoteConnections.sendChatMessage(
                                    account.userId,
                                    selectedChat!!.id,
                                    content
                                )
                            }
                        }.onSuccess { sent ->
                            messages.add(sent)
                            LocalDataStore.saveMessages(
                                context,
                                account.userId,
                                selectedChat!!.id,
                                messages
                            )
                            LocalDataStore.saveSync(
                                context,
                                account.userId,
                                ChatSyncMetadata(
                                    chatId = selectedChat!!.id,
                                    lastMessageId = sent.id,
                                    messageCount = messages.size,
                                    version = selectedChat!!.version,
                                    lastSync = System.currentTimeMillis()
                                )
                            )
                        }.onFailure { error = RemoteConnections.userFriendlyError(it) }
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
private fun ChatRow(chat: UserChat, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 2.dp
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                chat.participants.joinToString().ifBlank { "Conversación" },
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
    onBack: () -> Unit,
    onSend: (String) -> Unit
) {
    var draft by remember(chat.id) { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        Button(onClick = onBack, modifier = Modifier.padding(10.dp)) {
            Text("Volver a chats")
        }
        LazyColumn(
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
                        Text(message.content, modifier = Modifier.padding(12.dp))
                    }
                }
            }
            if (loading) item { CircularProgressIndicator() }
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
                enabled = draft.isNotBlank(),
                onClick = {
                    onSend(draft)
                    draft = ""
                }
            ) {
                Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "Enviar")
            }
        }
    }
}
