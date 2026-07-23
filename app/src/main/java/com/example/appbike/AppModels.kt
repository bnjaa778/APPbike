package com.example.appbike

enum class AppScreen {
    HOME,
    ACCOUNT,
    BIKES,
    MARKETPLACE,
    CREATE_PUBLICATION,
    ROUTES,
    CHAT,
    SYNC
}

data class AccountSession(
    val userId: String,
    val email: String,
    val accessToken: String = "",
    val username: String? = null
)

data class Bike(
    val name: String,
    val brand: String,
    val model: String,
    val type: String,
    val serialNumber: String,
    val remoteId: Long? = null,
    val userId: String = "",
    val photoId: String = "",
    val isStolen: Boolean = false,
    val distanceKm: String = "",
    val lastMaintenance: String = "",
    val nextMaintenance: String = "",
    val imageUri: String = ""
)

data class ProductPublication(
    val title: String,
    val price: String,
    val category: String,
    val condition: String,
    val seller: String,
    val description: String,
    val mediaDescription: String,
    val id: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val images: List<String> = emptyList(),
    val createdBy: String = "",
    val createdAt: String = "",
    val brand: String = "",
    val model: String = "",
    val productType: String = "",
    val imageUri: String = "",
    val publicationStatus: String = "activa",
    val productStatus: String = "",
    val region: String = "LAS",
    val location: String = "",
    val photoFolderId: String = "",
    val currencyCode: String = "CLP",
    val distanceKm: Double? = null,
    val countryCode: String = "",
    val administrativeArea: String = "",
    val createdByUsername: String? = null
)

data class MaintenanceReminder(
    val bike: String,
    val component: String,
    val date: String,
    val notes: String,
    val remoteId: Long? = null,
    val bikeId: Long? = null
)

data class ServiceBooking(
    val workshop: String,
    val service: String,
    val date: String,
    val contact: String,
    val bike: String = "",
    val remoteId: Long? = null,
    val bikeId: Long? = null
)

data class BikeMaintenanceData(
    val past: List<MaintenanceReminder>,
    val future: List<ServiceBooking>
)

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
    val label: String = "",
    val countryCode: String = "",
    val administrativeArea: String = "",
    val regionCode: String = "",
    val currencyCode: String = ""
)

data class MeetupEvent(
    val id: String,
    val title: String,
    val dateTime: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val createdBy: String,
    val createdAt: String = "",
    val status: String = "activa",
    val region: String = "LAS",
    val location: String = "",
    val photoFolderId: String = "",
    val images: List<String> = emptyList(),
    val imageUri: String = "",
    val distanceKm: Double? = null,
    val countryCode: String = "",
    val administrativeArea: String = "",
    val createdByUsername: String? = null
)

enum class ChatType { SOCIAL, MARKETPLACE }

data class UserChat(
    val id: String,
    val type: ChatType,
    val participants: List<String>,
    val relatedEntityId: String? = null,
    val lastMessage: String = "",
    val messageCount: Int = 0,
    val version: Long = 0,
    val updatedAt: String = "",
    val title: String = "",
    val participantUsernames: Map<String, String> = emptyMap(),
    val lastMessageId: String = "",
    val lastMessageSenderId: String = "",
    val lastMessageSenderUsername: String? = null
)

data class StoredMessage(
    val id: String,
    val chatId: String,
    val senderId: String,
    val content: String,
    val createdAt: String,
    val localStatus: String? = null,
    val senderUsername: String? = null
)

data class ChatMessagePage(
    val messages: List<StoredMessage>,
    val messageCount: Int,
    val version: Long,
    val hasMore: Boolean
)

data class ChatSyncMetadata(
    val chatId: String,
    val lastMessageId: String = "",
    val messageCount: Int = 0,
    val version: Long = 0,
    val lastSync: Long = 0
)

data class ChatNotificationSyncMetadata(
    val chatId: String,
    val lastMessageId: String = "",
    val messageCount: Int = 0,
    val version: Long = 0
)

data class SyncPlatform(
    val id: String,
    val name: String,
    val description: String,
    val connected: Boolean,
    val connectedAt: String = ""
)

data class SportsOAuthStart(
    val provider: String,
    val authorizationUrl: String,
    val state: String
)

data class SportsOAuthCallback(
    val provider: String,
    val code: String,
    val state: String,
    val error: String = ""
)
