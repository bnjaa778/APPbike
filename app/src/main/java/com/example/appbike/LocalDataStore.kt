package com.example.appbike

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object LocalDataStore {
    private const val PREFS = "appbike_local_data"
    private const val LOCATION = "selected_location"
    private const val CHATS = "chats"
    private const val MESSAGES = "messages"
    private const val SYNC = "chat_sync"

    fun loadLocation(context: Context): GeoPoint? {
        val raw = prefs(context).getString(LOCATION, null) ?: return null
        return runCatching {
            JSONObject(raw).let {
                GeoPoint(
                    latitude = it.getDouble("latitude"),
                    longitude = it.getDouble("longitude"),
                    label = it.optString("label")
                )
            }
        }.getOrNull()
    }

    fun saveLocation(context: Context, point: GeoPoint) {
        prefs(context).edit().putString(
            LOCATION,
            JSONObject()
                .put("latitude", point.latitude)
                .put("longitude", point.longitude)
                .put("label", point.label)
                .toString()
        ).apply()
    }

    fun loadChats(context: Context, userId: String): List<UserChat> =
        readArray(context, "$CHATS:$userId").mapNotNull { item ->
            runCatching {
                UserChat(
                    id = item.getString("id"),
                    type = ChatType.valueOf(item.optString("type", "SOCIAL")),
                    participants = item.optJSONArray("participants").strings(),
                    relatedEntityId = item.optString("relatedEntityId").ifBlank { null },
                    lastMessage = item.optString("lastMessage"),
                    messageCount = item.optInt("messageCount"),
                    version = item.optLong("version"),
                    updatedAt = item.optString("updatedAt")
                )
            }.getOrNull()
        }

    fun saveChats(context: Context, userId: String, chats: List<UserChat>) {
        writeArray(context, "$CHATS:$userId", chats.map { chat ->
            JSONObject()
                .put("id", chat.id)
                .put("type", chat.type.name)
                .put("participants", JSONArray(chat.participants))
                .put("relatedEntityId", chat.relatedEntityId)
                .put("lastMessage", chat.lastMessage)
                .put("messageCount", chat.messageCount)
                .put("version", chat.version)
                .put("updatedAt", chat.updatedAt)
        })
    }

    fun loadMessages(context: Context, userId: String, chatId: String): List<StoredMessage> =
        readArray(context, "$MESSAGES:$userId:$chatId").mapNotNull { item ->
            runCatching {
                StoredMessage(
                    id = item.getString("id"),
                    chatId = item.getString("chatId"),
                    senderId = item.getString("senderId"),
                    content = item.getString("content"),
                    createdAt = item.getString("createdAt"),
                    localStatus = item.optString("localStatus").ifBlank { null }
                )
            }.getOrNull()
        }

    fun saveMessages(
        context: Context,
        userId: String,
        chatId: String,
        messages: List<StoredMessage>
    ) {
        writeArray(context, "$MESSAGES:$userId:$chatId", messages.map { message ->
            JSONObject()
                .put("id", message.id)
                .put("chatId", message.chatId)
                .put("senderId", message.senderId)
                .put("content", message.content)
                .put("createdAt", message.createdAt)
                .put("localStatus", message.localStatus)
        })
    }

    fun loadSync(context: Context, userId: String, chatId: String): ChatSyncMetadata? {
        val raw = prefs(context).getString("$SYNC:$userId:$chatId", null) ?: return null
        return runCatching {
            JSONObject(raw).let {
                ChatSyncMetadata(
                    chatId = chatId,
                    lastMessageId = it.optString("lastMessageId"),
                    messageCount = it.optInt("messageCount"),
                    version = it.optLong("version"),
                    lastSync = it.optLong("lastSync")
                )
            }
        }.getOrNull()
    }

    fun saveSync(context: Context, userId: String, metadata: ChatSyncMetadata) {
        prefs(context).edit().putString(
            "$SYNC:$userId:${metadata.chatId}",
            JSONObject()
                .put("lastMessageId", metadata.lastMessageId)
                .put("messageCount", metadata.messageCount)
                .put("version", metadata.version)
                .put("lastSync", metadata.lastSync)
                .toString()
        ).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun readArray(context: Context, key: String): List<JSONObject> {
        val raw = prefs(context).getString(key, "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    array.optJSONObject(index)?.let(::add)
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun writeArray(context: Context, key: String, values: List<JSONObject>) {
        prefs(context).edit().putString(key, JSONArray(values).toString()).apply()
    }

    private fun JSONArray?.strings(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                optString(index).takeIf(String::isNotBlank)?.let(::add)
            }
        }
    }
}
