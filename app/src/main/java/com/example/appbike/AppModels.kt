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
    val email: String
)

data class AppFeature(
    val title: String,
    val description: String,
    val screen: AppScreen
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
    val imageUri: String = ""
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

data class RoutePost(
    val name: String,
    val zone: String,
    val startPoint: String,
    val endPoint: String,
    val distanceKm: String,
    val estimatedTime: String,
    val difficulty: String,
    val safetyNote: String
)

data class RideMeetup(
    val title: String,
    val routeName: String,
    val meetingPoint: String,
    val dateTime: String,
    val organizer: String,
    val level: String,
    val maxRiders: String,
    val notes: String,
    val participants: List<String> = emptyList()
)

data class ChatMessage(
    val sender: String,
    val message: String
)

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
    val label: String = ""
)

data class MeetupEvent(
    val id: String,
    val title: String,
    val dateTime: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val createdBy: String,
    val createdAt: String = ""
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
    val updatedAt: String = ""
)

data class StoredMessage(
    val id: String,
    val chatId: String,
    val senderId: String,
    val content: String,
    val createdAt: String,
    val localStatus: String? = null
)

data class ChatSyncMetadata(
    val chatId: String,
    val lastMessageId: String = "",
    val messageCount: Int = 0,
    val version: Long = 0,
    val lastSync: Long = 0
)

data class SyncPlatform(
    val name: String,
    val description: String,
    val connected: Boolean
)
