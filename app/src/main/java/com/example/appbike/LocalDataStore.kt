package com.example.appbike

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs

object LocalDataStore {
    private const val PREFS = "appbike_local_data"
    private const val LOCATION = "selected_location"
    private const val MARKETPLACE_LOCATION = "marketplace_location"
    private const val LOCATION_HISTORY = "location_history"
    private const val LOCATION_HISTORY_LIMIT = 8
    private const val PROFILE_BIO = "profile_bio"
    private const val PROFILE_PHOTO = "profile_photo"
    private const val SOCIAL_POSTS = "social_posts"
    private const val CHATS = "chats"
    private const val MESSAGES = "messages"
    private const val SYNC = "chat_sync"
    private const val NOTIFICATION_SYNC = "chat_notification_sync"

    fun loadLocation(context: Context): GeoPoint? = loadGeoPoint(context, LOCATION)

    fun saveLocation(context: Context, point: GeoPoint) {
        val previousLocations = loadLocationHistory(context)
        saveGeoPoint(context, LOCATION, point)
        rememberLocation(context, point, previousLocations)
    }

    fun loadMarketplaceLocation(context: Context): GeoPoint? =
        loadGeoPoint(context, MARKETPLACE_LOCATION)

    fun saveMarketplaceLocation(context: Context, point: GeoPoint) {
        val previousLocations = loadLocationHistory(context)
        saveGeoPoint(context, MARKETPLACE_LOCATION, point)
        rememberLocation(context, point, previousLocations)
    }

    fun rememberRecentLocation(context: Context, point: GeoPoint) {
        rememberLocation(context, point, loadLocationHistory(context))
    }

    fun loadProfileBio(context: Context, userId: String): String =
        prefs(context).getString("$PROFILE_BIO:$userId", "").orEmpty()

    fun saveProfileBio(context: Context, userId: String, bio: String) {
        prefs(context).edit { putString("$PROFILE_BIO:$userId", bio.trim()) }
    }

    fun loadProfilePhotoUri(context: Context, userId: String): String =
        prefs(context).getString("$PROFILE_PHOTO:$userId", "").orEmpty()

    fun saveProfilePhotoUri(context: Context, userId: String, uri: String) {
        prefs(context).edit { putString("$PROFILE_PHOTO:$userId", uri) }
    }

    fun loadSocialPosts(context: Context, userId: String): List<SocialPost> =
        readArray(context, "$SOCIAL_POSTS:$userId")
            .mapNotNull { item ->
                runCatching {
                    SocialPost(
                        id = item.getString("id"),
                        userId = item.getString("userId"),
                        username = item.optString("username"),
                        caption = item.optString("caption"),
                        mediaUri = item.getString("mediaUri"),
                        mediaType = item.optString("mediaType", "photo"),
                        createdAt = item.optLong("createdAt")
                    )
                }.getOrNull()
            }
            .filter { it.id.isNotBlank() && it.userId == userId && it.mediaUri.isNotBlank() }
            .sortedByDescending(SocialPost::createdAt)

    fun saveSocialPost(context: Context, post: SocialPost) {
        val updated = (loadSocialPosts(context, post.userId) + post)
            .distinctBy(SocialPost::id)
            .sortedByDescending(SocialPost::createdAt)
        writeArray(context, "$SOCIAL_POSTS:${post.userId}", updated.map { item ->
            JSONObject()
                .put("id", item.id)
                .put("userId", item.userId)
                .put("username", item.username)
                .put("caption", item.caption)
                .put("mediaUri", item.mediaUri)
                .put("mediaType", item.mediaType)
                .put("createdAt", item.createdAt)
        })
    }

    fun loadLocationHistory(context: Context): List<GeoPoint> {
        val persisted = readArray(context, LOCATION_HISTORY).mapNotNull(::geoPointFromJson)
        return distinctLocations(
            persisted + listOfNotNull(
                loadMarketplaceLocation(context),
                loadLocation(context)
            )
        ).take(LOCATION_HISTORY_LIMIT)
    }

    private fun loadGeoPoint(context: Context, key: String): GeoPoint? {
        val raw = prefs(context).getString(key, null) ?: return null
        return runCatching {
            geoPointFromJson(JSONObject(raw))
        }.getOrNull()
    }

    private fun saveGeoPoint(context: Context, key: String, point: GeoPoint) {
        prefs(context).edit {
            putString(key, geoPointToJson(point).toString())
        }
    }

    private fun rememberLocation(
        context: Context,
        point: GeoPoint,
        previousLocations: List<GeoPoint>
    ) {
        writeArray(
            context,
            LOCATION_HISTORY,
            mergeLocationHistory(point, previousLocations).map(::geoPointToJson)
        )
    }

    internal fun mergeLocationHistory(
        selected: GeoPoint,
        previousLocations: List<GeoPoint>
    ): List<GeoPoint> = distinctLocations(listOf(selected) + previousLocations)
        .take(LOCATION_HISTORY_LIMIT)

    private fun distinctLocations(points: List<GeoPoint>): List<GeoPoint> = buildList {
        points.forEach { point ->
            if (none { it.sameCoordinates(point) }) add(point)
        }
    }

    private fun GeoPoint.sameCoordinates(other: GeoPoint): Boolean =
        abs(latitude - other.latitude) < 0.00001 &&
            abs(longitude - other.longitude) < 0.00001

    private fun geoPointFromJson(item: JSONObject): GeoPoint? = runCatching {
        GeoPoint(
            latitude = item.getDouble("latitude"),
            longitude = item.getDouble("longitude"),
            label = item.optString("label"),
            countryCode = item.optString("countryCode"),
            administrativeArea = item.optString("administrativeArea"),
            regionCode = item.optString("regionCode"),
            currencyCode = item.optString("currencyCode")
        )
    }.getOrNull()

    private fun geoPointToJson(point: GeoPoint) = JSONObject()
        .put("latitude", point.latitude)
        .put("longitude", point.longitude)
        .put("label", point.label)
        .put("countryCode", point.countryCode)
        .put("administrativeArea", point.administrativeArea)
        .put("regionCode", point.regionCode)
        .put("currencyCode", point.currencyCode)

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
                    updatedAt = item.optString("updatedAt"),
                    title = item.optString("title"),
                    participantUsernames = item.optJSONObject("participantUsernames")
                        .stringMap(),
                    lastMessageId = item.optString("lastMessageId"),
                    lastMessageSenderId = item.optString("lastMessageSenderId"),
                    lastMessageSenderUsername = item.optString(
                        "lastMessageSenderUsername"
                    ).ifBlank { null }
                )
            }.getOrNull()
        }.filter { it.id.isNotBlank() }

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
                .put("title", chat.title)
                .put("participantUsernames", JSONObject(chat.participantUsernames))
                .put("lastMessageId", chat.lastMessageId)
                .put("lastMessageSenderId", chat.lastMessageSenderId)
                .put("lastMessageSenderUsername", chat.lastMessageSenderUsername)
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
                    localStatus = item.optString("localStatus").ifBlank { null },
                    senderUsername = item.optString("senderUsername").ifBlank { null }
                )
            }.getOrNull()
        }.filter { message ->
            message.id.isNotBlank() &&
                (message.chatId.isBlank() || message.chatId == chatId)
        }.map { message ->
            if (message.chatId.isBlank()) message.copy(chatId = chatId) else message
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
                .put("senderUsername", message.senderUsername)
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
        prefs(context).edit {
            putString(
                "$SYNC:$userId:${metadata.chatId}",
                JSONObject()
                .put("lastMessageId", metadata.lastMessageId)
                .put("messageCount", metadata.messageCount)
                .put("version", metadata.version)
                .put("lastSync", metadata.lastSync)
                .toString()
            )
        }
    }

    fun loadNotificationSync(
        context: Context,
        userId: String,
        chatId: String
    ): ChatNotificationSyncMetadata? {
        val raw = prefs(context).getString(
            "$NOTIFICATION_SYNC:$userId:$chatId",
            null
        ) ?: return null
        return runCatching {
            JSONObject(raw).let {
                ChatNotificationSyncMetadata(
                    chatId = chatId,
                    lastMessageId = it.optString("lastMessageId"),
                    messageCount = it.optInt("messageCount"),
                    version = it.optLong("version")
                )
            }
        }.getOrNull()
    }

    fun saveNotificationSync(
        context: Context,
        userId: String,
        metadata: ChatNotificationSyncMetadata
    ) {
        prefs(context).edit {
            putString(
                "$NOTIFICATION_SYNC:$userId:${metadata.chatId}",
                JSONObject()
                .put("lastMessageId", metadata.lastMessageId)
                .put("messageCount", metadata.messageCount)
                .put("version", metadata.version)
                .toString()
            )
        }
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
        prefs(context).edit { putString(key, JSONArray(values).toString()) }
    }

    private fun JSONArray?.strings(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                optString(index).takeIf(String::isNotBlank)?.let(::add)
            }
        }
    }

    private fun JSONObject?.stringMap(): Map<String, String> {
        if (this == null) return emptyMap()
        return buildMap {
            keys().forEach { key ->
                optString(key).takeIf { it.isNotBlank() && it != "null" }?.let {
                    put(key, it)
                }
            }
        }
    }
}

internal fun mergeStoredMessages(
    local: List<StoredMessage>,
    downloaded: List<StoredMessage>
): List<StoredMessage> = (local + downloaded)
    .associateBy(StoredMessage::id)
    .values
    .sortedWith { first, second ->
        val createdAtOrder = first.createdAt.compareTo(second.createdAt)
        if (createdAtOrder != 0) createdAtOrder else compareRemoteIds(first.id, second.id)
    }

internal fun compareRemoteIds(first: String, second: String): Int {
    val firstNumber = first.toBigIntegerOrNull()
    val secondNumber = second.toBigIntegerOrNull()
    return if (firstNumber != null && secondNumber != null) {
        firstNumber.compareTo(secondNumber)
    } else {
        first.compareTo(second)
    }
}
