package com.example.appbike

import android.content.Context
import android.net.Uri
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.net.HttpURLConnection
import java.net.UnknownHostException
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.UUID

object RemoteConnections {

    private const val API_URL = "https://api.zizzio.cl/APIS/AppBikeExternal.php"
    private const val PUBLIC_BASE_URL = "https://api.zizzio.cl/"

    fun login(email: String, password: String): AccountSession {
        val response = postJson(
            JSONObject()
                .put("action", "login")
                .put("usuario", email.trim())
                .put("password", password)
        )
        val user = response.optJSONObject("user")
        val userId = user?.firstString("id")
            ?: response.firstString("user_id", "id")
            ?: throw RemoteConnectionException("El servidor no devolvió el ID de usuario.")
        val userEmail = user?.optString("email")
            ?.takeIf { it.isNotBlank() }
            ?: response.optString("email", email.trim())

        return AccountSession(userId = userId, email = userEmail)
    }

    fun loadUserBikes(userId: String): List<Bike> {
        val response = postJson(
            JSONObject()
                .put("action", "user.bikes.list")
                .put("user_id", userId)
                .put("limit", 500)
                .put("offset", 0)
        )
        return response.optJSONArray("bikes").mapObjects(::bikeFromJson)
    }

    fun loadBikeDetails(account: AccountSession, bike: Bike): Bike {
        val bikeId = requireBikeId(bike)
        val response = postJson(
            JSONObject()
                .put("action", "bike.get")
                .put("bike_id", bikeId)
        )
        val remoteBike = response.optJSONObject("bike")
            ?: throw RemoteConnectionException(
                "El servidor no devolvió la información de la bicicleta."
            )
        val loadedBike = bikeFromJson(remoteBike)

        if (loadedBike.remoteId != bikeId) {
            throw RemoteConnectionException(
                "El servidor devolvió una bicicleta distinta a la solicitada."
            )
        }
        if (loadedBike.userId.isNotBlank() && loadedBike.userId != account.userId) {
            throw RemoteConnectionException(
                "Esta bicicleta no pertenece a la cuenta activa."
            )
        }

        return loadedBike.copy(
            imageUri = loadedBike.imageUri.ifBlank { bike.imageUri },
            isStolen = bike.isStolen,
            distanceKm = bike.distanceKm,
            lastMaintenance = loadedBike.lastMaintenance.ifBlank {
                bike.lastMaintenance
            },
            nextMaintenance = loadedBike.nextMaintenance.ifBlank {
                bike.nextMaintenance
            }
        )
    }

    fun registerBike(
        context: Context,
        account: AccountSession,
        bike: Bike
    ): Bike {
        val fields = linkedMapOf(
            "action" to "bike.create",
            "user_id" to account.userId,
            "bike_custom_name" to bike.name.trim(),
            "bike_brand" to bike.brand.trim(),
            "bike_model" to bike.model.trim(),
            "bike_type" to bike.type.trim(),
            "serial_number" to bike.serialNumber.trim()
        )

        val response = if (bike.imageUri.isBlank()) {
            postJson(
                JSONObject().apply {
                    fields.forEach { (key, value) -> put(key, value) }
                }
            )
        } else {
            postMultipart(context, fields, Uri.parse(bike.imageUri))
        }

        val created = response.optJSONObject("bike")
            ?: throw RemoteConnectionException("El servidor no devolvió la bicicleta creada.")
        val photo = response.optJSONObject("photo")

        return bikeFromJson(created).copy(
            imageUri = bike.imageUri,
            photoId = photo?.optString("photo_id").orEmpty()
        )
    }

    fun loadMaintenance(
        account: AccountSession,
        bike: Bike
    ): BikeMaintenanceData {
        val bikeId = requireBikeId(bike)

        val pastResponse = postJson(
            JSONObject()
                .put("action", "maintenance.past.list")
                .put("user_id", account.userId)
                .put("bike_id", bikeId)
                .put("limit", 500)
                .put("offset", 0)
        )
        val futureResponse = postJson(
            JSONObject()
                .put("action", "maintenance.future.list")
                .put("user_id", account.userId)
                .put("bike_id", bikeId)
                .put("limit", 500)
                .put("offset", 0)
        )

        return BikeMaintenanceData(
            past = pastResponse.optJSONArray("maintenance").mapObjects { item ->
                MaintenanceReminder(
                    bike = bike.name,
                    component = item.optString("maintenance_type"),
                    date = item.optString("maintenance_date"),
                    notes = item.optString("description"),
                    remoteId = item.firstLong("id"),
                    bikeId = bikeId
                )
            },
            future = futureResponse.optJSONArray("maintenance").mapObjects { item ->
                val description = item.optString("description")
                val workshop = description.lineValue("Taller:")
                val contact = description.lineValue("Contacto:")
                ServiceBooking(
                    workshop = workshop,
                    service = item.optString("maintenance_type"),
                    date = item.optString("scheduled_date"),
                    contact = contact.ifBlank { description },
                    bike = bike.name,
                    remoteId = item.firstLong("id"),
                    bikeId = bikeId
                )
            }
        )
    }

    fun loadBikePhoto(photoId: String): ByteArray {
        if (photoId.isBlank()) {
            throw RemoteConnectionException("La bicicleta no tiene una foto asociada.")
        }

        val response = postJson(
            JSONObject()
                .put("action", "bike.photo.get")
                .put("photo_id", photoId)
        )
        val photo = response.optJSONObject("photo")
            ?: throw RemoteConnectionException("El servidor no devolvió la fotografía.")
        val encodedContent = photo.optString("content_base64")
            .takeIf { it.isNotBlank() && it != "null" }
            ?: throw RemoteConnectionException(
                "El servidor no devolvió el contenido de la fotografía."
            )

        return runCatching {
            Base64.decode(encodedContent, Base64.DEFAULT)
        }.getOrElse {
            throw RemoteConnectionException(
                "El contenido de la fotografía no es válido.",
                it
            )
        }
    }

    fun createMaintenanceReminder(
        bike: Bike,
        reminder: MaintenanceReminder
    ): MaintenanceReminder {
        val bikeId = requireBikeId(bike)
        val response = postJson(
            JSONObject()
                .put("action", "maintenance.past.create")
                .put("bike_id", bikeId)
                .put("maintenance_date", requireApiDate(reminder.date))
                .put("maintenance_type", reminder.component.trim())
                .put("description", reminder.notes.trim())
        )
        val saved = response.optJSONObject("maintenance")
            ?: throw RemoteConnectionException("El servidor no devolvió la mantención creada.")

        return reminder.copy(
            date = saved.optString("maintenance_date", reminder.date),
            remoteId = saved.firstLong("id"),
            bikeId = bikeId
        )
    }

    fun bookService(bike: Bike, booking: ServiceBooking): ServiceBooking {
        val bikeId = requireBikeId(bike)
        val description = buildString {
            if (booking.workshop.isNotBlank()) append("Taller: ${booking.workshop.trim()}")
            if (booking.contact.isNotBlank()) {
                if (isNotEmpty()) append('\n')
                append("Contacto: ${booking.contact.trim()}")
            }
        }
        val response = postJson(
            JSONObject()
                .put("action", "maintenance.future.create")
                .put("bike_id", bikeId)
                .put("scheduled_date", requireApiDate(booking.date))
                .put("maintenance_type", booking.service.trim())
                .put("description", description)
        )
        val saved = response.optJSONObject("maintenance")
            ?: throw RemoteConnectionException("El servidor no devolvió el servicio agendado.")

        return booking.copy(
            date = saved.optString("scheduled_date", booking.date),
            remoteId = saved.firstLong("id"),
            bikeId = bikeId
        )
    }

    fun updateBikeTheftStatus(bike: Bike, isStolen: Boolean): Bike =
        bike.copy(isStolen = isStolen)

    fun publishProduct(publication: ProductPublication): ProductPublication = publication

    fun contactSeller(publication: ProductPublication) = Unit

    fun publishRoute(route: RoutePost): RoutePost = route

    fun createMeetup(meetup: RideMeetup): RideMeetup = meetup

    fun updateMeetupMembership(meetup: RideMeetup, join: Boolean): RideMeetup =
        meetup.copy(
            participants = if (join) {
                meetup.participants + "Yo"
            } else {
                meetup.participants - "Yo"
            }
        )

    fun sendChatMessage(message: ChatMessage): ChatMessage = message

    fun setSportsPlatformConnection(
        platform: SyncPlatform,
        connected: Boolean
    ): SyncPlatform = platform.copy(connected = connected)

    fun loadNearbyMeetups(
        center: GeoPoint,
        query: String = "",
        radiusKm: Int = 40
    ): List<MeetupEvent> {
        val response = getResource(
            "juntas",
            mapOf(
                "lat" to center.latitude.toString(),
                "lng" to center.longitude.toString(),
                "radiusKm" to radiusKm.toString(),
                "q" to query
            )
        )
        return response.firstArray("juntas", "events", "items")
            .mapObjects(::meetupFromJson)
    }

    fun searchLocation(query: String): GeoPoint {
        val coordinateParts = query.split(',').map(String::trim)
        if (coordinateParts.size == 2) {
            val latitude = coordinateParts[0].toDoubleOrNull()
            val longitude = coordinateParts[1].toDoubleOrNull()
            if (latitude != null && longitude != null) {
                return GeoPoint(latitude, longitude, query)
            }
        }
        val response = getResource("locations/search", mapOf("q" to query))
        val item = response.firstObject("location", "item")
            ?: response.firstArray("locations", "items")?.optJSONObject(0)
            ?: throw RemoteConnectionException("No se encontró esa ubicación.")
        return GeoPoint(
            latitude = item.optDouble("latitude", item.optDouble("lat")),
            longitude = item.optDouble("longitude", item.optDouble("lng")),
            label = item.firstString("label", "name", "address").orEmpty()
        )
    }

    fun createMeetupEvent(event: MeetupEvent): MeetupEvent {
        val response = postResource(
            "juntas/events",
            JSONObject()
                .put("title", event.title)
                .put("dateTime", event.dateTime)
                .put("description", event.description)
                .put("latitude", event.latitude)
                .put("longitude", event.longitude)
                .put("createdBy", event.createdBy)
        )
        return response.firstObject("junta", "event", "item")
            ?.let(::meetupFromJson)
            ?: event
    }

    fun loadMarketplacePosts(
        center: GeoPoint,
        query: String = "",
        radiusKm: Int = 40
    ): List<ProductPublication> {
        val response = getResource(
            "marketplace",
            mapOf(
                "lat" to center.latitude.toString(),
                "lng" to center.longitude.toString(),
                "radiusKm" to radiusKm.toString(),
                "q" to query
            )
        )
        return response.firstArray("posts", "publications", "items")
            .mapObjects(::marketplaceFromJson)
    }

    fun createMarketplacePost(
        context: Context,
        post: ProductPublication
    ): ProductPublication {
        val fields = linkedMapOf(
            "title" to post.title.trim(),
            "brand" to post.brand.trim(),
            "model" to post.model.trim(),
            "product_type" to post.productType.trim(),
            "price" to post.price.trim(),
            "description" to post.description.trim(),
            "latitude" to post.latitude.toString(),
            "longitude" to post.longitude.toString(),
            "createdBy" to post.createdBy
        )
        val response = if (post.imageUri.isBlank()) {
            postResource(
                "marketplace",
                JSONObject().apply { fields.forEach { (key, value) -> put(key, value) } }
            )
        } else {
            postEndpointMultipart(
                context = context,
                path = "marketplace",
                fields = fields,
                imageUri = Uri.parse(post.imageUri),
                fileField = "foto"
            )
        }
        return response.firstObject("post", "publication", "item")
            ?.let(::marketplaceFromJson)
            ?: post
    }

    fun loadMarketplacePhoto(photoId: String): ByteArray {
        val response = postJson(
            JSONObject()
                .put("action", "marketplace.photo.get")
                .put("photo_id", photoId)
        )
        val photo = response.firstObject("photo", "item")
            ?: throw RemoteConnectionException("El servidor no devolvió la fotografía.")
        val content = photo.optString("content_base64")
            .takeIf { it.isNotBlank() && it != "null" }
            ?: throw RemoteConnectionException(
                "El servidor no devolvió el contenido de la fotografía."
            )
        return runCatching { Base64.decode(content, Base64.DEFAULT) }.getOrElse {
            throw RemoteConnectionException("La fotografía recibida no es válida.", it)
        }
    }

    fun loadUserChats(userId: String): List<UserChat> {
        val response = getResource("chats", mapOf("userId" to userId))
        return response.firstArray("chats", "items").mapObjects(::chatFromJson)
    }

    fun loadChatMessages(
        userId: String,
        chatId: String,
        afterMessageId: String = ""
    ): List<StoredMessage> {
        val response = getResource(
            "chats/$chatId/messages",
            mapOf("userId" to userId, "after" to afterMessageId)
        )
        return response.firstArray("messages", "items").mapObjects(::messageFromJson)
    }

    fun sendChatMessage(userId: String, chatId: String, content: String): StoredMessage {
        val response = postResource(
            "chats/$chatId/messages",
            JSONObject()
                .put("senderId", userId)
                .put("content", content)
        )
        return response.firstObject("message", "item")
            ?.let(::messageFromJson)
            ?: throw RemoteConnectionException("El servidor no devolvió el mensaje enviado.")
    }

    fun userFriendlyError(error: Throwable): String {
        val causes = generateSequence(error) { it.cause }.toList()
        val remoteMessage = causes.filterIsInstance<RemoteConnectionException>()
            .firstOrNull()
            ?.message

        return when {
            causes.any { it is UnknownHostException } ->
                "No se pudo encontrar el servidor. Revisa tu conexión a internet."
            causes.any { it is java.net.SocketTimeoutException } ->
                "El servidor tardó demasiado en responder. Inténtalo nuevamente."
            causes.any { it is java.net.ConnectException } ->
                "El servidor no está disponible en este momento."
            !remoteMessage.isNullOrBlank() -> remoteMessage
            else -> "Ocurrió un problema al comunicarse con el servidor."
        }
    }

    private fun postJson(payload: JSONObject): JSONObject {
        return executeRequest("application/json; charset=utf-8") { connection ->
            connection.outputStream.use { output ->
                output.write(payload.toString().toByteArray(StandardCharsets.UTF_8))
            }
        }
    }

    private fun getResource(path: String, parameters: Map<String, String>): JSONObject {
        val query = parameters
            .filterValues { it.isNotBlank() }
            .entries
            .joinToString("&") { (key, value) ->
                "${encode(key)}=${encode(value)}"
            }
        val url = "$API_URL/${path.trimStart('/')}" +
            if (query.isBlank()) "" else "?$query"
        return executeEndpointRequest(url, "GET")
    }

    private fun postResource(path: String, payload: JSONObject): JSONObject =
        executeEndpointRequest(
            "$API_URL/${path.trimStart('/')}",
            "POST",
            payload
        )

    private fun executeEndpointRequest(
        url: String,
        method: String,
        payload: JSONObject? = null
    ): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.setRequestProperty("Accept", "application/json")
            if (payload != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use {
                    it.write(payload.toString().toByteArray(StandardCharsets.UTF_8))
                }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
            val response = runCatching { JSONObject(text) }.getOrElse {
                throw RemoteConnectionException(
                    "El endpoint ${pathFromUrl(url)} no está disponible todavía en el servidor."
                )
            }
            if (status !in 200..299 || !response.optBoolean("ok", true)) {
                throw RemoteConnectionException(
                    response.optString("message", "Error HTTP $status")
                )
            }
            return response
        } finally {
            connection.disconnect()
        }
    }

    private fun pathFromUrl(url: String): String = url.substringAfter("$API_URL/")
        .substringBefore('?')

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private fun postMultipart(
        context: Context,
        fields: Map<String, String>,
        imageUri: Uri
    ): JSONObject {
        val boundary = "AppBike-${UUID.randomUUID()}"
        val mimeType = context.contentResolver.getType(imageUri)
            ?.takeIf { it in setOf("image/jpeg", "image/png", "image/webp") }
            ?: "image/jpeg"
        val extension = when (mimeType) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }

        return executeRequest("multipart/form-data; boundary=$boundary", 60_000) { connection ->
            BufferedOutputStream(connection.outputStream).use { output ->
                fields.forEach { (name, value) ->
                    output.write("--$boundary\r\n".toByteArray())
                    output.write(
                        "Content-Disposition: form-data; name=\"$name\"\r\n\r\n"
                            .toByteArray()
                    )
                    output.write(value.toByteArray(StandardCharsets.UTF_8))
                    output.write("\r\n".toByteArray())
                }

                output.write("--$boundary\r\n".toByteArray())
                output.write(
                    "Content-Disposition: form-data; name=\"foto\"; filename=\"bicicleta.$extension\"\r\n"
                        .toByteArray()
                )
                output.write("Content-Type: $mimeType\r\n\r\n".toByteArray())

                context.contentResolver.openInputStream(imageUri)?.use { input ->
                    input.copyTo(output)
                } ?: throw RemoteConnectionException("No fue posible leer la fotografía seleccionada.")

                output.write("\r\n--$boundary--\r\n".toByteArray())
            }
        }
    }

    private fun postEndpointMultipart(
        context: Context,
        path: String,
        fields: Map<String, String>,
        imageUri: Uri,
        fileField: String
    ): JSONObject {
        val boundary = "AppBike-${UUID.randomUUID()}"
        val mimeType = context.contentResolver.getType(imageUri)
            ?.takeIf { it in setOf("image/jpeg", "image/png", "image/webp") }
            ?: "image/jpeg"
        val extension = when (mimeType) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        val connection = URL("$API_URL/${path.trimStart('/')}").openConnection()
            as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 60_000
            connection.doOutput = true
            connection.setRequestProperty(
                "Content-Type",
                "multipart/form-data; boundary=$boundary"
            )
            connection.setRequestProperty("Accept", "application/json")
            BufferedOutputStream(connection.outputStream).use { output ->
                fields.forEach { (name, value) ->
                    output.write("--$boundary\r\n".toByteArray())
                    output.write(
                        "Content-Disposition: form-data; name=\"$name\"\r\n\r\n"
                            .toByteArray()
                    )
                    output.write(value.toByteArray(StandardCharsets.UTF_8))
                    output.write("\r\n".toByteArray())
                }
                output.write("--$boundary\r\n".toByteArray())
                output.write(
                    (
                        "Content-Disposition: form-data; name=\"$fileField\"; " +
                            "filename=\"marketplace.$extension\"\r\n"
                        ).toByteArray()
                )
                output.write("Content-Type: $mimeType\r\n\r\n".toByteArray())
                context.contentResolver.openInputStream(imageUri)?.use { input ->
                    input.copyTo(output)
                } ?: throw RemoteConnectionException(
                    "No fue posible leer la fotografía seleccionada."
                )
                output.write("\r\n--$boundary--\r\n".toByteArray())
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
            val response = runCatching { JSONObject(text) }.getOrElse {
                throw RemoteConnectionException(
                    "El endpoint ${pathFromUrl(connection.url.toString())} no está disponible."
                )
            }
            if (status !in 200..299 || !response.optBoolean("ok", true)) {
                throw RemoteConnectionException(
                    response.optString("message", "Error HTTP $status")
                )
            }
            return response
        } finally {
            connection.disconnect()
        }
    }

    private fun executeRequest(
        contentType: String,
        readTimeout: Int = 20_000,
        writeBody: (HttpURLConnection) -> Unit
    ): JSONObject {
        val connection = URL(API_URL).openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = readTimeout
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", contentType)
            connection.setRequestProperty("Accept", "application/json")
            writeBody(connection)

            val statusCode = connection.responseCode
            val stream = if (statusCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val responseText = stream
                ?.bufferedReader(StandardCharsets.UTF_8)
                ?.use { it.readText() }
                .orEmpty()
            val response = runCatching { JSONObject(responseText) }.getOrElse {
                throw RemoteConnectionException("El servidor devolvió una respuesta no válida.")
            }

            if (statusCode !in 200..299 || !response.optBoolean("ok", true)) {
                throw RemoteConnectionException(
                    response.optString("message", "Error HTTP $statusCode")
                )
            }

            return response
        } catch (error: RemoteConnectionException) {
            throw error
        } catch (error: Exception) {
            throw RemoteConnectionException(
                "No se pudo conectar con la API: ${error.message ?: "error desconocido"}",
                error
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun bikeFromJson(item: JSONObject): Bike {
        val photoPath = item.optString("photo_path")
        val photoId = item.optString("photo_id")
        return Bike(
            name = item.optString("bike_custom_name"),
            brand = item.optString("bike_brand"),
            model = item.optString("bike_model"),
            type = item.optString("bike_type"),
            serialNumber = item.optString("serial_number"),
            remoteId = item.firstLong("id"),
            userId = item.optString("user_id"),
            photoId = photoId,
            lastMaintenance = item.optString("last_maintenance_date"),
            nextMaintenance = item.optString("next_maintenance_date"),
            imageUri = photoSource(photoId, photoPath)
        )
    }

    private fun meetupFromJson(item: JSONObject) = MeetupEvent(
        id = item.firstString("id", "event_id").orEmpty(),
        title = item.optString("title"),
        dateTime = item.firstString("dateTime", "date_time").orEmpty(),
        description = item.optString("description"),
        latitude = item.optDouble("latitude", item.optDouble("lat")),
        longitude = item.optDouble("longitude", item.optDouble("lng")),
        createdBy = item.firstString("createdBy", "created_by", "user_id").orEmpty(),
        createdAt = item.firstString("createdAt", "created_at").orEmpty()
    )

    private fun marketplaceFromJson(item: JSONObject) = ProductPublication(
        id = item.firstString("id", "post_id").orEmpty(),
        title = item.optString("title"),
        description = item.optString("description"),
        price = item.optString("price"),
        category = item.optString("category"),
        condition = item.optString("condition"),
        seller = item.firstString("seller", "createdBy", "created_by").orEmpty(),
        mediaDescription = "",
        latitude = item.optDouble("latitude", item.optDouble("lat")),
        longitude = item.optDouble("longitude", item.optDouble("lng")),
        images = item.optJSONArray("images").mapMarketplaceImages(),
        createdBy = item.firstString("createdBy", "created_by", "user_id").orEmpty(),
        createdAt = item.firstString("createdAt", "created_at").orEmpty(),
        brand = item.optString("brand"),
        model = item.optString("model"),
        productType = item.firstString("productType", "product_type", "type").orEmpty()
    )

    private fun chatFromJson(item: JSONObject) = UserChat(
        id = item.firstString("id", "chat_id").orEmpty(),
        type = runCatching {
            ChatType.valueOf(item.optString("type", "SOCIAL").uppercase(Locale.ROOT))
        }.getOrDefault(ChatType.SOCIAL),
        participants = item.optJSONArray("participants").mapStrings(),
        relatedEntityId = item.firstString("relatedEntityId", "related_entity_id"),
        lastMessage = item.firstString("lastMessage", "last_message").orEmpty(),
        messageCount = item.optInt("messageCount", item.optInt("message_count")),
        version = item.optLong("version"),
        updatedAt = item.firstString("updatedAt", "updated_at").orEmpty()
    )

    private fun messageFromJson(item: JSONObject) = StoredMessage(
        id = item.firstString("id", "message_id").orEmpty(),
        chatId = item.firstString("chatId", "chat_id").orEmpty(),
        senderId = item.firstString("senderId", "sender_id").orEmpty(),
        content = item.optString("content"),
        createdAt = item.firstString("createdAt", "created_at").orEmpty(),
        localStatus = item.firstString("localStatus", "local_status")
    )

    private fun photoSource(photoId: String, path: String): String {
        if (photoId.isNotBlank() && photoId != "null") {
            return "appbike-photo://$photoId"
        }
        return publicPhotoUrl(path)
    }

    private fun publicPhotoUrl(path: String): String {
        if (path.isBlank() || path == "null") return ""
        if (path.startsWith("http://") || path.startsWith("https://")) return path
        return PUBLIC_BASE_URL + path.trimStart('/')
    }

    private fun requireBikeId(bike: Bike): Long =
        bike.remoteId ?: throw RemoteConnectionException(
            "La bicicleta no tiene un BikeID del servidor. Actualiza Mis bicicletas."
        )

    private fun requireApiDate(rawDate: String): String {
        val clean = rawDate.trim()
        if (!Regex("""^\d{4}-\d{2}-\d{2}$""").matches(clean)) {
            throw RemoteConnectionException("La fecha debe tener formato AAAA-MM-DD.")
        }
        return clean
    }

    private fun String.lineValue(prefix: String): String =
        lineSequence()
            .firstOrNull { it.trimStart().startsWith(prefix, ignoreCase = true) }
            ?.substringAfter(prefix)
            ?.trim()
            .orEmpty()

    private fun <T> JSONArray?.mapObjects(transform: (JSONObject) -> T): List<T> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                optJSONObject(index)?.let { add(transform(it)) }
            }
        }
    }

    private fun JSONArray?.mapStrings(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                optString(index).takeIf { it.isNotBlank() && it != "null" }?.let(::add)
            }
        }
    }

    private fun JSONArray?.mapMarketplaceImages(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                val raw = opt(index)
                when (raw) {
                    is String -> raw.takeIf { it.isNotBlank() && it != "null" }?.let(::add)
                    is JSONObject -> {
                        val photoId = raw.firstString("photo_id", "id")
                        val url = raw.firstString("url", "image_url", "path")
                        when {
                            !photoId.isNullOrBlank() -> add("appbike-market-photo://$photoId")
                            !url.isNullOrBlank() -> add(publicPhotoUrl(url))
                        }
                    }
                }
            }
        }
    }

    private fun JSONObject.firstArray(vararg keys: String): JSONArray? {
        keys.forEach { key -> optJSONArray(key)?.let { return it } }
        return null
    }

    private fun JSONObject.firstObject(vararg keys: String): JSONObject? {
        keys.forEach { key -> optJSONObject(key)?.let { return it } }
        return null
    }

    private fun JSONObject.firstLong(vararg keys: String): Long? {
        keys.forEach { key ->
            if (!has(key) || isNull(key)) return@forEach
            val parsed = when (val value = opt(key)) {
                is Number -> value.toLong()
                is String -> value.toLongOrNull()
                else -> null
            }
            if (parsed != null && parsed > 0) return parsed
        }
        return null
    }

    private fun JSONObject.firstString(vararg keys: String): String? {
        keys.forEach { key ->
            if (!has(key) || isNull(key)) return@forEach
            val value = optString(key).trim()
            if (value.isNotEmpty() && value != "null") return value
        }
        return null
    }

    class RemoteConnectionException(
        message: String,
        cause: Throwable? = null
    ) : RuntimeException(message, cause)
}
