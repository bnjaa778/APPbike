package com.example.appbike

import android.content.Context
import android.net.Uri
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.UnknownHostException
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
import androidx.core.net.toUri
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object RemoteConnections {

    private const val API_URL = "https://api.zizzio.cl/APIS/AppBikeExternal.php"
    private const val PUBLIC_BASE_URL = "https://api.zizzio.cl/"
    private const val OPEN_STREET_MAP_GEOCODER = "https://nominatim.openstreetmap.org"
    private const val OPEN_STREET_MAP_BIKE_ROUTER =
        "https://routing.openstreetmap.de/routed-bike"
    private const val OPEN_METEO_FORECAST_URL = "https://api.open-meteo.com/v1/forecast"
    private const val GEOCODER_USER_AGENT = "APPbike-Android/1.0 (https://zizzio.cl)"
    private val PUBLIC_ACTIONS = setOf(
        "login",
        "junta.list",
        "junta.get",
        "junta.photo.get",
        "location.search",
        "location.reverse",
        "location.resolve",
        "marketplace.list",
        "marketplace.get",
        "marketplace.photo.get"
    )
    private const val GEOCODER_MIN_INTERVAL_MS = 1_000L
    private const val GEOCODER_CACHE_LIMIT = 30
    private const val DEFAULT_COMMUNITY_REGION = "LAS"
    private const val MAX_IMAGE_BYTES = 20L * 1024L * 1024L
    private val geocoderRequestLock = Any()
    private val geocoderCache = linkedMapOf<String, List<GeoPoint>>()
    private var lastGeocoderRequestAt = 0L
    @Volatile private var accessToken: String = ""

    fun setSession(session: AccountSession?) {
        accessToken = session?.accessToken.orEmpty()
    }

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
        if (!isValidAccountUserId(userId)) {
            throw RemoteConnectionException("El servidor devolvió una identidad de usuario inválida.")
        }
        val userEmail = user?.optString("email")
            ?.takeIf { it.isNotBlank() }
            ?: response.optString("email", email.trim())

        return AccountSession(
            userId = userId,
            email = userEmail,
            accessToken = response.firstString("access_token", "token")
                ?: user?.firstString("access_token", "token").orEmpty(),
            username = user?.firstString("nombre_de_usuario", "username")
                ?: response.firstString("nombre_de_usuario", "username")
        )
    }

    fun loadUserProfile(account: AccountSession): AccountSession {
        val response = postJson(JSONObject().put("action", "user.get"))
        return accountSessionFromProfileResponse(response, account)
    }

    fun updateUsername(account: AccountSession, username: String): AccountSession {
        val response = postJson(
            JSONObject()
                .put("action", "user.username.update")
                .put("nombre_de_usuario", username.trim())
        )
        return accountSessionFromProfileResponse(response, account)
    }

    private fun accountSessionFromProfileResponse(
        response: JSONObject,
        fallback: AccountSession
    ): AccountSession {
        val user = response.optJSONObject("user")
        val returnedId = user?.firstString("id", "user_id")
            ?: response.firstString("user_id", "id")
            ?: fallback.userId
        if (returnedId != fallback.userId) {
            throw RemoteConnectionException("El servidor devolvió un perfil distinto.")
        }
        return fallback.copy(
            email = user?.firstString("email")
                ?: response.firstString("email")
                ?: fallback.email,
            username = user?.firstString("nombre_de_usuario", "username")
                ?: response.firstString("nombre_de_usuario", "username")
                ?: fallback.username
        )
    }

    fun loadUserBikes(userId: String): List<Bike> {
        val response = postJson(
            JSONObject()
                .put("action", "user.bikes.list")
                .put("user_id", userId)
                .put("limit", 500)
                .put("offset", 0)
        )
        return validateBikeOwnership(
            userId = userId,
            bikes = response.optJSONArray("bikes").mapObjects(::bikeFromJson)
        )
    }

    fun loadBikeDetails(account: AccountSession, bike: Bike): Bike {
        val bikeId = requireOwnedBikeId(account, bike)
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
        val normalizedBike = normalizeBikeMutation(account, bike, requireRemoteId = false)
        val fields = linkedMapOf(
            "action" to "bike.create",
            "user_id" to account.userId,
            "bike_custom_name" to normalizedBike.name,
            "bike_brand" to normalizedBike.brand,
            "bike_model" to normalizedBike.model,
            "bike_type" to normalizedBike.type,
            "serial_number" to normalizedBike.serialNumber
        )

        val response = if (normalizedBike.imageUri.isBlank()) {
            postJson(
                JSONObject().apply {
                    fields.forEach { (key, value) -> put(key, value) }
                }
            )
        } else {
            postMultipart(context, fields, normalizedBike.imageUri.toUri())
        }

        val created = response.optJSONObject("bike")
            ?: throw RemoteConnectionException("El servidor no devolvió la bicicleta creada.")
        val photo = response.optJSONObject("photo")

        val parsedBike = bikeFromJson(created)
        val createdPhotoId = photo?.optString("photo_id")
            ?.takeIf { it.isNotBlank() && it != "null" }
            ?: parsedBike.photoId
        val savedBike = parsedBike.copy(
            userId = parsedBike.userId.ifBlank { account.userId },
            imageUri = if (createdPhotoId.isNotBlank()) {
                "appbike-photo://$createdPhotoId"
            } else {
                normalizedBike.imageUri
            },
            photoId = createdPhotoId
        )
        return validateBikeOwnership(account.userId, listOf(savedBike)).single()
    }

    fun updateBike(account: AccountSession, bike: Bike): Bike {
        val normalizedBike = normalizeBikeMutation(account, bike, requireRemoteId = true)
        val bikeId = requireOwnedBikeId(account, normalizedBike)
        val response = postJson(
            JSONObject()
                .put("action", "bike.update")
                .put("bike_id", bikeId)
                .put("user_id", account.userId)
                .put("bike_custom_name", normalizedBike.name)
                .put("bike_brand", normalizedBike.brand)
                .put("bike_model", normalizedBike.model)
                .put("bike_type", normalizedBike.type)
                .put("serial_number", normalizedBike.serialNumber)
        )
        val returnedBike = response.optJSONObject("bike")?.let(::bikeFromJson)
        return mergeUpdatedBike(account, normalizedBike, returnedBike)
    }

    fun deleteBike(account: AccountSession, bike: Bike) {
        val bikeId = requireOwnedBikeId(account, bike)
        postJson(
            JSONObject()
                .put("action", "bike.delete")
                .put("bike_id", bikeId)
        )
    }

    internal fun normalizeBikeMutation(
        account: AccountSession,
        bike: Bike,
        requireRemoteId: Boolean
    ): Bike {
        if (bike.userId.isNotBlank() && bike.userId != account.userId) {
            throw RemoteConnectionException("Esta bicicleta no pertenece a la cuenta activa.")
        }
        if (requireRemoteId) requireOwnedBikeId(account, bike)

        val normalized = bike.copy(
            name = bike.name.trim(),
            brand = bike.brand.trim(),
            model = bike.model.trim(),
            type = bike.type.trim(),
            serialNumber = bike.serialNumber.trim(),
            userId = account.userId
        )
        if (
            normalized.name.isBlank() ||
            normalized.brand.isBlank() ||
            normalized.model.isBlank() ||
            normalized.type.isBlank() ||
            normalized.serialNumber.isBlank()
        ) {
            throw RemoteConnectionException("Completa todos los datos obligatorios de la bicicleta.")
        }
        return normalized
    }

    internal fun mergeUpdatedBike(
        account: AccountSession,
        requested: Bike,
        returned: Bike?
    ): Bike {
        val requestedId = requireOwnedBikeId(account, requested)
        val candidate = returned ?: requested
        val merged = candidate.copy(
            name = candidate.name.ifBlank { requested.name },
            brand = candidate.brand.ifBlank { requested.brand },
            model = candidate.model.ifBlank { requested.model },
            type = candidate.type.ifBlank { requested.type },
            serialNumber = candidate.serialNumber.ifBlank { requested.serialNumber },
            remoteId = candidate.remoteId ?: requestedId,
            userId = candidate.userId.ifBlank { account.userId },
            photoId = candidate.photoId.ifBlank { requested.photoId },
            distanceKm = candidate.distanceKm.ifBlank { requested.distanceKm },
            lastMaintenance = candidate.lastMaintenance.ifBlank { requested.lastMaintenance },
            nextMaintenance = candidate.nextMaintenance.ifBlank { requested.nextMaintenance },
            imageUri = candidate.imageUri.ifBlank { requested.imageUri }
        )
        if (merged.remoteId != requestedId) {
            throw RemoteConnectionException(
                "El servidor devolvió una bicicleta distinta a la actualizada."
            )
        }
        return validateBikeOwnership(account.userId, listOf(merged)).single()
    }

    internal fun requireOwnedBikeId(account: AccountSession, bike: Bike): Long {
        if (bike.userId.isNotBlank() && bike.userId != account.userId) {
            throw RemoteConnectionException("Esta bicicleta no pertenece a la cuenta activa.")
        }
        return requireBikeId(bike)
    }

    internal fun validateBikeOwnership(userId: String, bikes: List<Bike>): List<Bike> {
        if (bikes.any { it.userId.isNotBlank() && it.userId != userId }) {
            throw RemoteConnectionException(
                "El servidor devolvió bicicletas que no pertenecen a la cuenta activa."
            )
        }
        return bikes
    }

    fun loadMaintenance(
        account: AccountSession,
        bike: Bike
    ): BikeMaintenanceData {
        val bikeId = requireOwnedBikeId(account, bike)

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
                pastMaintenanceFromJson(item, bike, bikeId)
            },
            future = futureResponse.optJSONArray("maintenance").mapObjects { item ->
                futureMaintenanceFromJson(item, bike, bikeId)
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

        return decodePhotoContent(encodedContent)
    }

    fun createMaintenanceReminder(
        account: AccountSession,
        bike: Bike,
        reminder: MaintenanceReminder
    ): MaintenanceReminder {
        val normalized = normalizeMaintenanceReminder(account, bike, reminder, false)
        val bikeId = normalized.bikeId!!
        val response = postJson(
            JSONObject()
                .put("action", "maintenance.past.create")
                .put("bike_id", bikeId)
                .put("maintenance_date", normalized.date)
                .put("maintenance_type", normalized.component)
                .put("description", normalized.notes)
        )
        val saved = response.optJSONObject("maintenance")
            ?: throw RemoteConnectionException("El servidor no devolvió la mantención creada.")

        return mergeMaintenanceReminder(normalized, saved, bike, bikeId)
    }

    fun updateMaintenanceReminder(
        account: AccountSession,
        bike: Bike,
        reminder: MaintenanceReminder
    ): MaintenanceReminder {
        val normalized = normalizeMaintenanceReminder(account, bike, reminder, true)
        val bikeId = normalized.bikeId!!
        val response = postJson(
            JSONObject()
                .put("action", "maintenance.past.update")
                .put("past_id", normalized.remoteId)
                .put("bike_id", bikeId)
                .put("maintenance_date", normalized.date)
                .put("maintenance_type", normalized.component)
                .put("description", normalized.notes)
        )
        val saved = response.firstObject("maintenance", "past_maintenance", "item")
            ?: return normalized
        return mergeMaintenanceReminder(normalized, saved, bike, bikeId)
    }

    fun deleteMaintenanceReminder(
        account: AccountSession,
        bike: Bike,
        reminder: MaintenanceReminder
    ) {
        val normalized = normalizeMaintenanceReminder(account, bike, reminder, true)
        postJson(
            JSONObject()
                .put("action", "maintenance.past.delete")
                .put("past_id", normalized.remoteId)
        )
    }

    fun bookService(
        account: AccountSession,
        bike: Bike,
        booking: ServiceBooking
    ): ServiceBooking {
        val normalized = normalizeServiceBooking(account, bike, booking, false)
        val bikeId = normalized.bikeId!!
        val response = postJson(
            JSONObject()
                .put("action", "maintenance.future.create")
                .put("bike_id", bikeId)
                .put("scheduled_date", normalized.date)
                .put("maintenance_type", normalized.service)
                .put("description", serviceDescription(normalized))
        )
        val saved = response.optJSONObject("maintenance")
            ?: throw RemoteConnectionException("El servidor no devolvió el servicio agendado.")

        return mergeServiceBooking(normalized, saved, bike, bikeId)
    }

    fun updateServiceBooking(
        account: AccountSession,
        bike: Bike,
        booking: ServiceBooking
    ): ServiceBooking {
        val normalized = normalizeServiceBooking(account, bike, booking, true)
        val bikeId = normalized.bikeId!!
        val response = postJson(
            JSONObject()
                .put("action", "maintenance.future.update")
                .put("future_id", normalized.remoteId)
                .put("bike_id", bikeId)
                .put("scheduled_date", normalized.date)
                .put("maintenance_type", normalized.service)
                .put("description", serviceDescription(normalized))
        )
        val saved = response.firstObject("maintenance", "future_maintenance", "item")
            ?: return normalized
        return mergeServiceBooking(normalized, saved, bike, bikeId)
    }

    fun deleteServiceBooking(
        account: AccountSession,
        bike: Bike,
        booking: ServiceBooking
    ) {
        val normalized = normalizeServiceBooking(account, bike, booking, true)
        postJson(
            JSONObject()
                .put("action", "maintenance.future.delete")
                .put("future_id", normalized.remoteId)
        )
    }

    fun completeServiceBooking(
        account: AccountSession,
        bike: Bike,
        booking: ServiceBooking,
        completedDate: String,
        notes: String
    ): MaintenanceReminder {
        val normalized = normalizeServiceBooking(account, bike, booking, true)
        val bikeId = normalized.bikeId!!
        val cleanDate = requireApiDate(completedDate)
        val cleanNotes = notes.trim().ifBlank {
            "Servicio completado${normalized.workshop.takeIf(String::isNotBlank)?.let { " en $it" }.orEmpty()}."
        }
        val response = postJson(
            JSONObject()
                .put("action", "maintenance.future.complete")
                .put("future_id", normalized.remoteId)
                .put("completed_date", cleanDate)
                .put("maintenance_type", normalized.service)
                .put("description", cleanNotes)
        )
        val saved = response.firstObject("past_maintenance", "maintenance", "item")
        val fallback = MaintenanceReminder(
            bike = bike.name,
            component = normalized.service,
            date = cleanDate,
            notes = cleanNotes,
            bikeId = bikeId
        )
        return if (saved == null) fallback else mergeMaintenanceReminder(
            fallback,
            saved,
            bike,
            bikeId
        )
    }

    internal fun normalizeMaintenanceReminder(
        account: AccountSession,
        bike: Bike,
        reminder: MaintenanceReminder,
        requireRemoteId: Boolean
    ): MaintenanceReminder {
        val bikeId = requireOwnedBikeId(account, bike)
        if (reminder.bikeId != null && reminder.bikeId != bikeId) {
            throw RemoteConnectionException("La mantención no corresponde a esta bicicleta.")
        }
        if (requireRemoteId && reminder.remoteId == null) {
            throw RemoteConnectionException("La mantención no tiene un identificador válido.")
        }
        val component = reminder.component.trim()
        if (component.isBlank()) {
            throw RemoteConnectionException("Indica el trabajo realizado.")
        }
        return reminder.copy(
            bike = bike.name,
            component = component,
            date = requireApiDate(reminder.date),
            notes = reminder.notes.trim(),
            bikeId = bikeId
        )
    }

    internal fun normalizeServiceBooking(
        account: AccountSession,
        bike: Bike,
        booking: ServiceBooking,
        requireRemoteId: Boolean
    ): ServiceBooking {
        val bikeId = requireOwnedBikeId(account, bike)
        if (booking.bikeId != null && booking.bikeId != bikeId) {
            throw RemoteConnectionException("El servicio no corresponde a esta bicicleta.")
        }
        if (requireRemoteId && booking.remoteId == null) {
            throw RemoteConnectionException("El servicio no tiene un identificador válido.")
        }
        val workshop = booking.workshop.trim()
        val service = booking.service.trim()
        if (workshop.isBlank() || service.isBlank()) {
            throw RemoteConnectionException("Indica el taller y el servicio requerido.")
        }
        return booking.copy(
            workshop = workshop,
            service = service,
            date = requireApiDate(booking.date),
            contact = booking.contact.trim(),
            bike = bike.name,
            bikeId = bikeId
        )
    }

    internal fun validateMaintenanceBikeId(expectedBikeId: Long, returnedBikeId: Long?) {
        if (returnedBikeId != null && returnedBikeId != expectedBikeId) {
            throw RemoteConnectionException(
                "El servidor devolvió una mantención de otra bicicleta."
            )
        }
    }

    private fun pastMaintenanceFromJson(
        item: JSONObject,
        bike: Bike,
        bikeId: Long
    ): MaintenanceReminder {
        validateMaintenanceBikeId(bikeId, item.firstLong("bike_id", "BikeID"))
        return MaintenanceReminder(
            bike = bike.name,
            component = item.optString("maintenance_type"),
            date = item.optString("maintenance_date"),
            notes = item.optString("description"),
            remoteId = item.firstLong("id", "past_id"),
            bikeId = bikeId
        )
    }

    private fun futureMaintenanceFromJson(
        item: JSONObject,
        bike: Bike,
        bikeId: Long
    ): ServiceBooking {
        validateMaintenanceBikeId(bikeId, item.firstLong("bike_id", "BikeID"))
        val description = item.optString("description")
        val workshop = description.lineValue("Taller:")
        val contact = description.lineValue("Contacto:")
        return ServiceBooking(
            workshop = workshop,
            service = item.optString("maintenance_type"),
            date = item.optString("scheduled_date"),
            contact = contact.ifBlank { description },
            bike = bike.name,
            remoteId = item.firstLong("id", "future_id"),
            bikeId = bikeId
        )
    }

    private fun mergeMaintenanceReminder(
        requested: MaintenanceReminder,
        saved: JSONObject,
        bike: Bike,
        bikeId: Long
    ): MaintenanceReminder {
        val parsed = pastMaintenanceFromJson(saved, bike, bikeId)
        if (
            requested.remoteId != null && parsed.remoteId != null &&
            requested.remoteId != parsed.remoteId
        ) {
            throw RemoteConnectionException("El servidor actualizó otra mantención.")
        }
        return requested.copy(
            component = parsed.component.ifBlank { requested.component },
            date = parsed.date.ifBlank { requested.date },
            notes = parsed.notes.ifBlank { requested.notes },
            remoteId = parsed.remoteId ?: requested.remoteId,
            bikeId = bikeId
        )
    }

    private fun mergeServiceBooking(
        requested: ServiceBooking,
        saved: JSONObject,
        bike: Bike,
        bikeId: Long
    ): ServiceBooking {
        val parsed = futureMaintenanceFromJson(saved, bike, bikeId)
        if (
            requested.remoteId != null && parsed.remoteId != null &&
            requested.remoteId != parsed.remoteId
        ) {
            throw RemoteConnectionException("El servidor actualizó otro servicio.")
        }
        return requested.copy(
            workshop = parsed.workshop.ifBlank { requested.workshop },
            service = parsed.service.ifBlank { requested.service },
            date = parsed.date.ifBlank { requested.date },
            contact = parsed.contact.ifBlank { requested.contact },
            remoteId = parsed.remoteId ?: requested.remoteId,
            bikeId = bikeId
        )
    }

    private fun serviceDescription(booking: ServiceBooking): String = buildString {
        if (booking.workshop.isNotBlank()) append("Taller: ${booking.workshop}")
        if (booking.contact.isNotBlank()) {
            if (isNotEmpty()) append('\n')
            append("Contacto: ${booking.contact}")
        }
    }

    fun loadNearbyMeetups(
        center: GeoPoint,
        query: String = "",
        radiusKm: Int = 40,
        status: String = "activa"
    ): List<MeetupEvent> {
        val basePayload = JSONObject()
                .put("action", "junta.list")
                .put("region", communityRegionFor(center))
                .put("junta_status", status)
                .put("lat", center.latitude)
                .put("lng", center.longitude)
                .put("radius_km", radiusKm)
                .put("q", query.trim())
                .put("limit", 100)
        val loaded = mutableListOf<MeetupEvent>()
        for (offset in 0 until 500 step 100) {
            val response = postJson(JSONObject(basePayload.toString()).put("offset", offset))
            val page = response.firstArray("juntas", "events", "items")
                .mapObjects(::meetupFromJson)
                .filter { it.id.isNotBlank() }
            loaded.addAll(page)
            if (page.size < 100) break
        }
        val cleanQuery = query.trim()
        return loaded.distinctBy(MeetupEvent::id)
            .filter { event ->
                cleanQuery.isBlank() || listOf(
                    event.title,
                    event.description,
                    event.location
                ).any { it.contains(cleanQuery, ignoreCase = true) }
            }
            .filter { event ->
                !event.hasCoordinates() || distanceKm(
                    center.latitude,
                    center.longitude,
                    event.latitude,
                    event.longitude
                ) <= radiusKm
            }
    }

    fun searchLocation(query: String): GeoPoint = searchLocations(query).firstOrNull()
        ?: throw RemoteConnectionException("No se encontró esa ubicación.")

    fun loadCyclingRoute(
        origin: GeoPoint,
        destination: GeoPoint,
        connectTimeoutMs: Int = 12_000,
        readTimeoutMs: Int = 20_000
    ): CyclingRoutePreview {
        requireValidRoutePoint(origin, "origen")
        requireValidRoutePoint(destination, "destino")
        val coordinates = "${origin.longitude},${origin.latitude};" +
            "${destination.longitude},${destination.latitude}"
        val url = "$OPEN_STREET_MAP_BIKE_ROUTER/route/v1/driving/$coordinates" +
            "?overview=full&geometries=geojson&steps=false"
        val response = executePublicJsonGet(
            url = url,
            failureMessage = "No fue posible calcular el trayecto ciclista.",
            connectTimeoutMs = connectTimeoutMs,
            readTimeoutMs = readTimeoutMs
        )
        return cyclingRouteFromJson(origin, destination, response)
    }

    internal fun cyclingRouteFromJson(
        origin: GeoPoint,
        destination: GeoPoint,
        response: JSONObject
    ): CyclingRoutePreview {
        if (!response.optString("code").equals("Ok", ignoreCase = true)) {
            throw RemoteConnectionException("No se encontró un camino ciclista hasta ese destino.")
        }
        val route = response.optJSONArray("routes")?.optJSONObject(0)
            ?: throw RemoteConnectionException("El servicio no devolvió un trayecto utilizable.")
        val coordinates = route.optJSONObject("geometry")?.optJSONArray("coordinates")
            ?: throw RemoteConnectionException("El trayecto no incluyó una geometría válida.")
        val geometry = buildList {
            for (index in 0 until coordinates.length()) {
                val coordinate = coordinates.optJSONArray(index) ?: continue
                val longitude = coordinate.optDouble(0, Double.NaN)
                val latitude = coordinate.optDouble(1, Double.NaN)
                if (latitude.isFinite() && longitude.isFinite() &&
                    latitude in -90.0..90.0 && longitude in -180.0..180.0
                ) {
                    add(GeoPoint(latitude, longitude))
                }
            }
        }
        if (geometry.size < 2) {
            throw RemoteConnectionException("El trayecto no incluyó suficientes puntos.")
        }
        val distanceKm = route.optDouble("distance", Double.NaN) / 1_000.0
        val durationSeconds = route.optDouble("duration", Double.NaN)
        if (!distanceKm.isFinite() || distanceKm <= 0.0 ||
            !durationSeconds.isFinite() || durationSeconds <= 0.0
        ) {
            throw RemoteConnectionException("El trayecto no incluyó distancia ni duración válidas.")
        }
        return CyclingRoutePreview(
            origin = origin,
            destination = destination,
            distanceKm = distanceKm,
            estimatedMinutes = ceil(durationSeconds / 60.0).toInt().coerceAtLeast(1),
            geometry = geometry,
            source = CyclingRouteSource.OPEN_STREET_MAP
        )
    }

    private fun requireValidRoutePoint(point: GeoPoint, name: String) {
        require(
            point.latitude.isFinite() && point.longitude.isFinite() &&
                point.latitude in -90.0..90.0 && point.longitude in -180.0..180.0
        ) { "El $name del trayecto no tiene coordenadas válidas." }
    }

    fun searchLocations(
        query: String,
        connectTimeoutMs: Int = 15_000,
        readTimeoutMs: Int = 20_000
    ): List<GeoPoint> {
        val cleanQuery = query.trim()
        val coordinateParts = cleanQuery.split(',').map(String::trim)
        if (coordinateParts.size == 2) {
            val latitude = coordinateParts[0].toDoubleOrNull()
            val longitude = coordinateParts[1].toDoubleOrNull()
            if (
                latitude != null && longitude != null &&
                latitude in -90.0..90.0 && longitude in -180.0..180.0
            ) {
                return listOf(GeoPoint(latitude, longitude, cleanQuery))
            }
        }

        if (cleanQuery.length < 3) {
            throw RemoteConnectionException("Escribe al menos 3 caracteres para buscar.")
        }

        val cacheKey = cleanQuery.lowercase(Locale.ROOT)
        synchronized(geocoderCache) {
            geocoderCache[cacheKey]?.let { return it }
        }

        val backendResults = runCatching {
            val response = postJson(
                JSONObject()
                    .put("action", "location.search")
                    .put("q", cleanQuery)
                    .put("limit", 5)
            )
            response.firstArray("locations", "items").mapObjects(::geoPointFromJson)
        }.getOrDefault(emptyList())
        if (backendResults.isNotEmpty()) {
            synchronized(geocoderCache) { geocoderCache[cacheKey] = backendResults }
            return backendResults
        }

        val url = "$OPEN_STREET_MAP_GEOCODER/search" +
            "?format=jsonv2&addressdetails=1&limit=5" +
            "&accept-language=es&q=${encode(cleanQuery)}"
        val response = JSONArray(
            executeOpenStreetMapRequest(
                url = url,
                connectTimeoutMs = connectTimeoutMs,
                readTimeoutMs = readTimeoutMs
            )
        )
        val results = buildList {
            for (index in 0 until response.length()) {
                val item = response.optJSONObject(index) ?: continue
                val latitude = item.optString("lat").toDoubleOrNull() ?: continue
                val longitude = item.optString("lon").toDoubleOrNull() ?: continue
                val address = item.optJSONObject("address")
                val countryCode = address?.optString("country_code")
                    .orEmpty().uppercase(Locale.ROOT)
                val administrativeArea = address?.firstString("state", "region", "county").orEmpty()
                add(GeoPoint(
                    latitude = latitude,
                    longitude = longitude,
                    label = item.optString("display_name").ifBlank { "$latitude, $longitude" },
                    countryCode = countryCode,
                    administrativeArea = administrativeArea,
                    regionCode = item.firstString("region_code")
                        ?: communityRegionCodeFor(countryCode, administrativeArea),
                    currencyCode = currencyCodeForCountry(countryCode)
                ))
            }
        }
        if (results.isEmpty()) {
            throw RemoteConnectionException("No se encontró esa ubicación.")
        }
        synchronized(geocoderCache) {
            geocoderCache[cacheKey] = results
            while (geocoderCache.size > GEOCODER_CACHE_LIMIT) {
                geocoderCache.remove(geocoderCache.keys.first())
            }
        }
        return results
    }

    fun reverseGeocodeLocation(point: GeoPoint): GeoPoint {
        val backend = runCatching {
            postJson(
                JSONObject()
                    .put("action", "location.reverse")
                    .put("lat", point.latitude)
                    .put("lng", point.longitude)
            ).firstObject("location", "item")?.let(::geoPointFromJson)
        }.getOrNull()
        if (backend != null) return backend

        val url = "$OPEN_STREET_MAP_GEOCODER/reverse" +
            "?format=jsonv2&addressdetails=1&zoom=14&accept-language=es" +
            "&lat=${point.latitude}&lon=${point.longitude}"
        val item = JSONObject(executeOpenStreetMapRequest(url))
        val address = item.optJSONObject("address")
        val countryCode = address?.optString("country_code")
            .orEmpty().uppercase(Locale.ROOT)
        val administrativeArea = address?.firstString("state", "region", "county").orEmpty()
        return point.copy(
            label = item.optString("display_name").ifBlank { formatCoordinates(point) },
            countryCode = countryCode,
            administrativeArea = administrativeArea,
            regionCode = communityRegionCodeFor(countryCode, administrativeArea),
            currencyCode = currencyCodeForCountry(countryCode)
        )
    }

    fun resolveCommunityLocation(point: GeoPoint): GeoPoint {
        val localFallback = point.copy(
            regionCode = point.regionCode.ifBlank {
                communityRegionCodeFor(point.countryCode, point.administrativeArea)
            },
            currencyCode = point.currencyCode.ifBlank {
                currencyCodeForCountry(point.countryCode)
            }
        )
        return runCatching {
            postJson(
                JSONObject()
                    .put("action", "location.resolve")
                    .put("lat", point.latitude)
                    .put("lng", point.longitude)
                    .put("label", point.label)
                    .put("country_code", point.countryCode)
                    .put("administrative_area", point.administrativeArea)
            ).firstObject("location", "item")?.let(::geoPointFromJson)
                ?: localFallback
        }.getOrDefault(localFallback)
    }

    fun createMeetupEvent(context: Context, event: MeetupEvent): MeetupEvent {
        val region = event.region.ifBlank {
            communityRegionFor(GeoPoint(event.latitude, event.longitude, event.location))
        }
        val location = if (parseCommunityLocation(event.location) != null) {
            event.location
        } else {
            communityLocation(
                region,
                GeoPoint(
                    event.latitude,
                    event.longitude,
                    event.location.ifBlank { event.title }
                )
            )
        }
        val response = postJson(
            JSONObject()
                .put("action", "junta.create")
                .put("region", region)
                .put("user_id", event.createdBy)
                .put("location", location)
                .put("latitude", event.latitude)
                .put("longitude", event.longitude)
                .put("country_code", event.countryCode)
                .put("administrative_area", event.administrativeArea)
                .put("title", event.title.trim())
                .put(
                    "description",
                    encodeMeetupDescription(event.dateTime, event.description)
                )
                .put("junta_status", event.status.ifBlank { "activa" })
        )
        val created = response.firstObject("junta", "event", "item")
            ?.let(::meetupFromJson)
            ?.let { parsed ->
                parsed.copy(
                    title = parsed.title.ifBlank { event.title.trim() },
                    dateTime = parsed.dateTime.ifBlank { event.dateTime.trim() },
                    description = parsed.description.ifBlank { event.description.trim() },
                    latitude = parsed.latitude.takeUnless { it == 0.0 } ?: event.latitude,
                    longitude = parsed.longitude.takeUnless { it == 0.0 } ?: event.longitude,
                    createdBy = parsed.createdBy.ifBlank { event.createdBy },
                    createdByUsername = parsed.createdByUsername ?: event.createdByUsername,
                    region = parsed.region.ifBlank { region },
                    location = parsed.location.ifBlank { location },
                    countryCode = parsed.countryCode.ifBlank { event.countryCode },
                    administrativeArea = parsed.administrativeArea.ifBlank {
                        event.administrativeArea
                    }
                )
            }
            ?: throw RemoteConnectionException("El servidor no devolvió la junta creada.")
        if (created.id.isBlank()) {
            throw RemoteConnectionException("La junta creada no tiene un ID regional.")
        }
        if (created.createdBy != event.createdBy) {
            throw RemotePartialSuccessException(
                message = "La junta se creó, pero el servidor devolvió un propietario inconsistente. Se actualizó la lista para evitar duplicarla.",
                entityId = created.id
            )
        }
        if (event.imageUri.isNotBlank()) {
            runCatching {
                uploadMeetupPhoto(context, event.createdBy, created.id, event.imageUri)
            }.getOrElse { cause ->
                throw RemotePartialSuccessException(
                    message = "La junta se creó, pero no fue posible subir su fotografía. Puedes agregarla desde tu perfil.",
                    entityId = created.id,
                    cause = cause
                )
            }
        }
        val detailed = runCatching { loadMeetupDetails(created.id) }
            .getOrDefault(created)
            .let { loaded ->
                loaded.copy(createdBy = loaded.createdBy.ifBlank { event.createdBy })
            }
        if (detailed.createdBy != event.createdBy) {
            throw RemotePartialSuccessException(
                message = "La junta se creó, pero su detalle devolvió un propietario inconsistente. Se actualizó la lista para evitar duplicarla.",
                entityId = created.id
            )
        }
        return detailed
    }

    fun loadMeetupDetails(juntaId: String): MeetupEvent {
        val response = postJson(
            JSONObject()
                .put("action", "junta.get")
                .put("junta_id", juntaId)
        )
        val meetup = response.firstObject("junta", "event", "item")
            ?.let(::meetupFromJson)
            ?: throw RemoteConnectionException("El servidor no devolvió la junta.")
        if (meetup.id != juntaId) {
            throw RemoteConnectionException("El servidor devolvió una junta distinta a la solicitada.")
        }
        return meetup
    }

    fun updateMeetupStatus(userId: String, juntaId: String, status: String): MeetupEvent {
        postJson(
            JSONObject()
                .put("action", "junta.status.update")
                .put("user_id", userId)
                .put("junta_id", juntaId)
                .put("junta_status", status)
        )
        return loadMeetupDetails(juntaId)
    }

    fun completeMeetup(userId: String, juntaId: String): MeetupEvent {
        postJson(
            JSONObject()
                .put("action", "junta.complete")
                .put("user_id", userId)
                .put("junta_id", juntaId)
        )
        return loadMeetupDetails(juntaId)
    }

    fun uploadMeetupPhoto(context: Context, userId: String, juntaId: String, imageUri: String) {
        postMultipart(
            context,
            linkedMapOf(
                "action" to "junta.photo.upload",
                "user_id" to userId,
                "junta_id" to juntaId
            ),
            imageUri.toUri()
        )
    }

    fun loadMeetupPhoto(juntaId: String, photoId: String): ByteArray = loadCommunityPhoto(
        action = "junta.photo.get",
        parentField = "junta_id",
        parentId = juntaId,
        photoId = photoId
    )

    fun communityRegionFor(point: GeoPoint): String {
        if (point.regionCode.matches(Regex("[A-Za-z]{3}"))) {
            return point.regionCode.uppercase(Locale.ROOT)
        }
        val explicit = Regex("""^\s*([A-Za-z]{3}):""")
            .find(point.label)
            ?.groupValues
            ?.getOrNull(1)
            ?.uppercase(Locale.ROOT)
        return explicit ?: DEFAULT_COMMUNITY_REGION
    }

    fun loadMarketplacePosts(
        center: GeoPoint,
        query: String = "",
        radiusKm: Int = 40,
        status: String = "activa"
    ): List<ProductPublication> {
        val basePayload = JSONObject()
                .put("action", "marketplace.list")
                .put("region", communityRegionFor(center))
                .put("publication_status", status)
                .put("lat", center.latitude)
                .put("lng", center.longitude)
                .put("radius_km", radiusKm)
                .put("q", query.trim())
                .put("limit", 100)
        val loaded = mutableListOf<ProductPublication>()
        for (offset in 0 until 500 step 100) {
            val response = postJson(JSONObject(basePayload.toString()).put("offset", offset))
            val page = response.firstArray("posts", "publications", "items")
                .mapObjects(::marketplaceFromJson)
                .filter { it.id.isNotBlank() }
            loaded.addAll(page)
            if (page.size < 100) break
        }
        val cleanQuery = query.trim()
        return loaded.distinctBy(ProductPublication::id)
            .filter { post ->
                cleanQuery.isBlank() || listOf(
                    post.title,
                    post.description,
                    post.productStatus,
                    post.location
                ).any { it.contains(cleanQuery, ignoreCase = true) }
            }
            .filter { post ->
                !post.hasCoordinates() || distanceKm(
                    center.latitude,
                    center.longitude,
                    post.latitude,
                    post.longitude
                ) <= radiusKm
            }
    }

    fun createMarketplacePost(
        context: Context,
        post: ProductPublication
    ): ProductPublication {
        val price = marketplaceWholeUnitPrice(post.price)
        val region = post.region.ifBlank {
            communityRegionFor(GeoPoint(post.latitude, post.longitude, post.location))
        }
        val location = if (parseCommunityLocation(post.location) != null) {
            post.location
        } else {
            communityLocation(
                region,
                GeoPoint(
                    post.latitude,
                    post.longitude,
                    post.location.ifBlank { post.title }
                )
            )
        }
        val response = postJson(
            JSONObject()
                .put("action", "marketplace.create")
                .put("region", region)
                .put("user_id", post.createdBy)
                .put("location", location)
                .put("latitude", post.latitude)
                .put("longitude", post.longitude)
                .put("country_code", post.countryCode)
                .put("administrative_area", post.administrativeArea)
                .put("title", post.title.trim())
                .put("description", post.description.trim())
                .put("price", price)
                .put("currency", post.currencyCode.ifBlank { "CLP" }.uppercase(Locale.ROOT))
                .put("product_status", post.productStatus.ifBlank { post.condition })
                .put(
                    "publication_status",
                    post.publicationStatus.ifBlank { "activa" }
                )
        )
        val createdObject = response.firstObject("post", "publication", "item")
        val created = createdObject
            ?.let(::marketplaceFromJson)
            ?.let { parsed ->
                parsed.copy(
                    title = parsed.title.ifBlank { post.title.trim() },
                    price = parsed.price.ifBlank { price.toString() },
                    condition = parsed.condition.ifBlank { post.productStatus },
                    seller = parsed.seller.ifBlank { post.seller },
                    description = parsed.description.ifBlank { post.description.trim() },
                    createdBy = parsed.createdBy.ifBlank { post.createdBy },
                    createdByUsername = parsed.createdByUsername ?: post.createdByUsername,
                    latitude = parsed.latitude.takeUnless { it == 0.0 } ?: post.latitude,
                    longitude = parsed.longitude.takeUnless { it == 0.0 } ?: post.longitude,
                    productStatus = parsed.productStatus.ifBlank { post.productStatus },
                    region = parsed.region.ifBlank { region },
                    location = parsed.location.ifBlank { location },
                    currencyCode = createdObject
                        .firstString("currency", "currency_code")
                        ?.uppercase(Locale.ROOT)
                        ?: post.currencyCode.ifBlank { "CLP" }.uppercase(Locale.ROOT),
                    countryCode = parsed.countryCode.ifBlank { post.countryCode },
                    administrativeArea = parsed.administrativeArea.ifBlank {
                        post.administrativeArea
                    }
                )
            }
            ?: throw RemoteConnectionException("El servidor no devolvió la publicación creada.")
        if (created.id.isBlank()) {
            throw RemoteConnectionException("La publicación creada no tiene un ID regional.")
        }
        if (created.createdBy != post.createdBy) {
            throw RemotePartialSuccessException(
                message = "La publicación se creó, pero el servidor devolvió un propietario inconsistente. Se actualizó la lista para evitar duplicarla.",
                entityId = created.id
            )
        }
        if (post.imageUri.isNotBlank()) {
            runCatching {
                uploadMarketplacePhoto(context, post.createdBy, created.id, post.imageUri)
            }.getOrElse { cause ->
                throw RemotePartialSuccessException(
                    message = "La publicación se creó, pero no fue posible subir su fotografía. Puedes agregarla desde tu perfil.",
                    entityId = created.id,
                    cause = cause
                )
            }
        }
        val detailed = runCatching { loadMarketplaceDetails(created.id) }
            .getOrDefault(created)
            .let { loaded ->
                loaded.copy(createdBy = loaded.createdBy.ifBlank { post.createdBy })
            }
        if (detailed.createdBy != post.createdBy) {
            throw RemotePartialSuccessException(
                message = "La publicación se creó, pero su detalle devolvió un propietario inconsistente. Se actualizó la lista para evitar duplicarla.",
                entityId = created.id
            )
        }
        return detailed
    }

    private fun marketplaceWholeUnitPrice(raw: String): Long {
        return parseMarketplaceWholeUnitPrice(raw) ?: throw RemoteConnectionException(
            "El precio debe ser mayor que cero e ingresarse sin decimales."
        )
    }

    fun loadMarketplaceDetails(publicationId: String): ProductPublication {
        val response = postJson(
            JSONObject()
                .put("action", "marketplace.get")
                .put("publication_id", publicationId)
        )
        val publication = response.firstObject("post", "publication", "item")
            ?.let(::marketplaceFromJson)
            ?: throw RemoteConnectionException("El servidor no devolvió la publicación.")
        if (publication.id != publicationId) {
            throw RemoteConnectionException(
                "El servidor devolvió una publicación distinta a la solicitada."
            )
        }
        return publication
    }

    fun updateMarketplaceStatus(
        userId: String,
        publicationId: String,
        status: String
    ): ProductPublication {
        postJson(
            JSONObject()
                .put("action", "marketplace.status.update")
                .put("user_id", userId)
                .put("publication_id", publicationId)
                .put("publication_status", status)
        )
        return loadMarketplaceDetails(publicationId)
    }

    fun uploadMarketplacePhoto(
        context: Context,
        userId: String,
        publicationId: String,
        imageUri: String
    ) {
        postMultipart(
            context,
            linkedMapOf(
                "action" to "marketplace.photo.upload",
                "user_id" to userId,
                "publication_id" to publicationId
            ),
            imageUri.toUri()
        )
    }

    fun loadMarketplacePhoto(publicationId: String, photoId: String): ByteArray =
        loadCommunityPhoto(
        action = "marketplace.photo.get",
        parentField = "publication_id",
        parentId = publicationId,
        photoId = photoId
    )

    fun loadOwnMarketplacePosts(
        userId: String,
        center: GeoPoint,
        statuses: List<String> = listOf("activa", "pausada", "vendida", "en_revision")
    ): List<ProductPublication> {
        val directResult = runCatching {
            postJson(
                JSONObject()
                    .put("action", "marketplace.mine.list")
                    .put("user_id", userId)
                    .put("limit", 500)
                    .put("offset", 0)
            ).firstArray("publications", "posts", "items")
                .mapObjects(::marketplaceFromJson)
                .let { validateOwnMarketplacePosts(userId, it) }
        }
        directResult.getOrNull()?.let { return it }
        directResult.exceptionOrNull()?.let { error ->
            if (!shouldUseRegionalProfileFallback(error)) throw error
        }

        val region = communityRegionFor(center)
        return statuses.flatMap { status ->
            postJson(
                JSONObject()
                    .put("action", "marketplace.list")
                    .put("region", region)
                    .put("user_id", userId)
                    .put("publication_status", status)
                    .put("limit", 500)
                    .put("offset", 0)
            ).firstArray("publications", "posts", "items")
                .mapObjects(::marketplaceFromJson)
                .filter { it.createdBy == userId }
        }.distinctBy(ProductPublication::id)
    }

    internal fun validateOwnMarketplacePosts(
        userId: String,
        posts: List<ProductPublication>
    ): List<ProductPublication> {
        if (posts.any { it.createdBy != userId }) {
            throw RemoteConnectionException(
                "El servidor devolvió publicaciones que no pertenecen a la cuenta activa."
            )
        }
        return posts
    }

    fun updateMarketplacePost(
        userId: String,
        publication: ProductPublication
    ): ProductPublication {
        postJson(
            JSONObject()
                .put("action", "marketplace.update")
                .put("user_id", userId)
                .put("publication_id", publication.id)
                .put("title", publication.title.trim())
                .put("description", publication.description.trim())
                .put("price", marketplaceWholeUnitPrice(publication.price))
                .put("currency", publication.currencyCode.uppercase(Locale.ROOT))
                .put("product_status", publication.productStatus)
        )
        return loadMarketplaceDetails(publication.id)
    }

    fun deleteMarketplacePost(userId: String, publicationId: String) {
        postJson(
            JSONObject()
                .put("action", "marketplace.delete")
                .put("user_id", userId)
                .put("publication_id", publicationId)
        )
    }

    fun loadOwnMeetups(userId: String, center: GeoPoint): List<MeetupEvent> {
        val directResult = runCatching {
            postJson(
                JSONObject()
                    .put("action", "junta.mine.list")
                    .put("user_id", userId)
                    .put("limit", 500)
                    .put("offset", 0)
            ).firstArray("juntas", "events", "items")
                .mapObjects(::meetupFromJson)
                .let { validateOwnMeetups(userId, it) }
        }
        directResult.getOrNull()?.let { return it }
        directResult.exceptionOrNull()?.let { error ->
            if (!shouldUseRegionalProfileFallback(error)) throw error
        }

        val region = communityRegionFor(center)
        return listOf("activa", "pasada").flatMap { status ->
            postJson(
                JSONObject()
                    .put("action", "junta.list")
                    .put("region", region)
                    .put("user_id", userId)
                    .put("junta_status", status)
                    .put("limit", 500)
                    .put("offset", 0)
            ).firstArray("juntas", "events", "items")
                .mapObjects(::meetupFromJson)
                .filter { it.createdBy == userId }
        }.distinctBy(MeetupEvent::id)
    }

    internal fun validateOwnMeetups(
        userId: String,
        meetups: List<MeetupEvent>
    ): List<MeetupEvent> {
        if (meetups.any { it.createdBy != userId }) {
            throw RemoteConnectionException(
                "El servidor devolvió juntas que no pertenecen a la cuenta activa."
            )
        }
        return meetups
    }

    fun updateMeetupEvent(userId: String, event: MeetupEvent): MeetupEvent {
        postJson(
            JSONObject()
                .put("action", "junta.update")
                .put("user_id", userId)
                .put("junta_id", event.id)
                .put("title", event.title.trim())
                .put(
                    "description",
                    encodeMeetupDescription(event.dateTime, event.description)
                )
                .put("location", event.location)
        )
        return loadMeetupDetails(event.id)
    }

    fun deleteMeetupEvent(userId: String, juntaId: String) {
        postJson(
            JSONObject()
                .put("action", "junta.delete")
                .put("user_id", userId)
                .put("junta_id", juntaId)
        )
    }

    fun loadUserChats(userId: String): List<UserChat> {
        val primary = runCatching {
            postJson(
                JSONObject()
                    .put("action", "chat.list")
                    .put("user_id", userId)
                    .put("limit", 200)
                    .put("offset", 0)
            )
        }
        val response = primary.getOrNull() ?: if (accessToken.isBlank()) {
            getResource("chats", mapOf("userId" to userId))
        } else {
            throw primary.exceptionOrNull()!!
        }
        return validateChatIds(
            response.firstArray("chats", "items").mapObjects(::chatFromJson)
        )
    }

    fun loadChatMessages(
        userId: String,
        chatId: String,
        afterMessageId: String = ""
    ): List<StoredMessage> = loadChatMessagePage(
        userId = userId,
        chatId = chatId,
        afterMessageId = afterMessageId
    ).messages

    fun loadChatMessagePage(
        userId: String,
        chatId: String,
        afterMessageId: String = ""
    ): ChatMessagePage {
        val primary = runCatching {
            postJson(
                JSONObject()
                    .put("action", "chat.messages.list")
                    .put("user_id", userId)
                    .put("chat_id", chatId)
                    .put("limit", 200)
                    .apply {
                        // The backend treats an empty cursor as invalid. Omitting it requests
                        // the complete first page and also repairs older local cache gaps.
                        if (afterMessageId.isNotBlank()) {
                            put("after_message_id", afterMessageId)
                        }
                    }
            )
        }
        val response = primary.getOrNull() ?: if (accessToken.isBlank()) {
            getResource(
                "chats/$chatId/messages",
                mapOf("userId" to userId, "after" to afterMessageId)
            )
        } else {
            throw primary.exceptionOrNull()!!
        }
        val messages = validateMessagesForChat(
            chatId = chatId,
            messages = validateMessageIds(
                response.firstArray("messages", "items").mapObjects(::messageFromJson)
            )
        )
        val metadata = response.firstObject("metadata", "sync", "pagination")
        val messageCount = response.firstInt("messageCount", "message_count", "total")
            ?: metadata?.firstInt("messageCount", "message_count", "total")
            ?: messages.size
        val version = response.firstLongValue("version")
            ?: metadata?.firstLongValue("version")
            ?: 0L
        val hasMore = when {
            response.has("hasMore") -> response.optBoolean("hasMore")
            response.has("has_more") -> response.optBoolean("has_more")
            metadata?.has("hasMore") == true -> metadata.optBoolean("hasMore")
            metadata?.has("has_more") == true -> metadata.optBoolean("has_more")
            afterMessageId.isBlank() -> messageCount > messages.size
            else -> false
        }
        return ChatMessagePage(
            messages = messages,
            messageCount = messageCount,
            version = version,
            hasMore = hasMore
        )
    }

    fun sendChatMessage(userId: String, chatId: String, content: String): StoredMessage {
        val clientMessageId = UUID.randomUUID().toString()
        val primary = runCatching {
            postJson(
                JSONObject()
                    .put("action", "chat.message.send")
                    .put("user_id", userId)
                    .put("chat_id", chatId)
                    .put("content", content.trim())
                    .put("client_message_id", clientMessageId)
            )
        }
        val response = primary.getOrNull() ?: if (accessToken.isBlank()) {
            postResource(
                "chats/$chatId/messages",
                JSONObject()
                    .put("senderId", userId)
                    .put("content", content.trim())
                    .put("clientMessageId", clientMessageId)
            )
        } else {
            throw primary.exceptionOrNull()!!
        }
        val message = response.firstObject("message", "item")
            ?.let(::messageFromJson)
            ?: throw RemoteConnectionException("El servidor no devolvió el mensaje enviado.")
        return validateMessagesForChat(
            chatId = chatId,
            messages = validateMessageIds(listOf(message))
        ).single()
    }

    fun getOrCreateChat(
        userId: String,
        relatedUserId: String,
        type: ChatType,
        relatedEntityId: String,
        title: String
    ): UserChat {
        if (relatedUserId.isBlank() || relatedUserId == userId) {
            throw RemoteConnectionException("No puedes iniciar esta conversación.")
        }
        val response = postJson(
            JSONObject()
                .put("action", "chat.get_or_create")
                .put("user_id", userId)
                .put("participant_user_id", relatedUserId)
                .put("chat_type", type.name.lowercase(Locale.ROOT))
                .put("related_entity_id", relatedEntityId)
                .put("title", title.trim())
        )
        val chat = response.firstObject("chat", "item")?.let(::chatFromJson)
            ?: throw RemoteConnectionException("El servidor no devolvió la conversación.")
        return validateChatIds(listOf(chat)).single()
    }

    internal fun validateChatIds(chats: List<UserChat>): List<UserChat> {
        if (chats.any { it.id.isBlank() }) {
            throw RemoteConnectionException("El servidor devolvió una conversación sin ID.")
        }
        return chats
    }

    internal fun validateMessageIds(messages: List<StoredMessage>): List<StoredMessage> {
        if (messages.any { it.id.isBlank() }) {
            throw RemoteConnectionException("El servidor devolvió un mensaje sin ID.")
        }
        return messages
    }

    internal fun validateMessagesForChat(
        chatId: String,
        messages: List<StoredMessage>
    ): List<StoredMessage> {
        if (chatId.isBlank()) {
            throw RemoteConnectionException("La conversación solicitada no tiene un ID válido.")
        }
        if (messages.any { it.chatId.isNotBlank() && it.chatId != chatId }) {
            throw RemoteConnectionException(
                "El servidor devolvió mensajes de una conversación distinta."
            )
        }
        return messages.map { message ->
            if (message.chatId.isBlank()) message.copy(chatId = chatId) else message
        }
    }

    fun loadSportsConnections(userId: String): List<SyncPlatform> {
        val response = postJson(
            JSONObject()
                .put("action", "sports.connections.list")
                .put("user_id", userId)
        )
        return response.firstArray("connections", "platforms", "items")
            .mapObjects(::sportsPlatformFromJson)
    }

    fun beginSportsConnection(
        userId: String,
        provider: String,
        redirectUri: String
    ): SportsOAuthStart {
        val response = postJson(
            JSONObject()
                .put("action", "sports.oauth.start")
                .put("user_id", userId)
                .put("provider", provider)
                .put("redirect_uri", redirectUri)
        )
        val oauth = response.firstObject("oauth", "authorization", "item") ?: response
        return SportsOAuthStart(
            provider = provider,
            authorizationUrl = oauth.firstString("authorization_url", "url")
                ?: throw RemoteConnectionException("El servidor no devolvió la URL de autorización."),
            state = oauth.optString("state")
        )
    }

    fun completeSportsConnection(
        userId: String,
        callback: SportsOAuthCallback,
        redirectUri: String
    ): SyncPlatform {
        val response = postJson(
            JSONObject()
                .put("action", "sports.oauth.complete")
                .put("user_id", userId)
                .put("provider", callback.provider)
                .put("code", callback.code)
                .put("state", callback.state)
                .put("redirect_uri", redirectUri)
        )
        return response.firstObject("connection", "platform", "item")
            ?.let(::sportsPlatformFromJson)
            ?: throw RemoteConnectionException("El servidor no confirmó la conexión deportiva.")
    }

    fun disconnectSportsConnection(userId: String, provider: String): SyncPlatform {
        val response = postJson(
            JSONObject()
                .put("action", "sports.connection.delete")
                .put("user_id", userId)
                .put("provider", provider)
        )
        return response.firstObject("connection", "platform", "item")
            ?.let(::sportsPlatformFromJson)
            ?: SyncPlatform(provider, provider.replaceFirstChar(Char::uppercase), "", false)
    }

    fun loadCurrentWeather(latitude: Double, longitude: Double): WeatherSnapshot {
        require(latitude.isFinite() && latitude in -90.0..90.0) {
            "Latitud meteorológica inválida."
        }
        require(longitude.isFinite() && longitude in -180.0..180.0) {
            "Longitud meteorológica inválida."
        }
        val latitudeValue = String.format(Locale.US, "%.5f", latitude)
        val longitudeValue = String.format(Locale.US, "%.5f", longitude)
        val url = "$OPEN_METEO_FORECAST_URL" +
            "?latitude=$latitudeValue" +
            "&longitude=$longitudeValue" +
            "&current=temperature_2m,apparent_temperature,is_day," +
            "precipitation,weather_code,cloud_cover" +
            "&temperature_unit=celsius&precipitation_unit=mm&timezone=auto"
        val response = executePublicJsonGet(
            url = url,
            failureMessage = "No fue posible obtener el tiempo actual."
        )
        return weatherFromJson(response, latitude, longitude)
    }

    fun loadWeatherForecast(latitude: Double, longitude: Double): WeatherForecast {
        require(latitude.isFinite() && latitude in -90.0..90.0) {
            "Latitud meteorológica inválida."
        }
        require(longitude.isFinite() && longitude in -180.0..180.0) {
            "Longitud meteorológica inválida."
        }
        val latitudeValue = String.format(Locale.US, "%.5f", latitude)
        val longitudeValue = String.format(Locale.US, "%.5f", longitude)
        val url = "$OPEN_METEO_FORECAST_URL" +
            "?latitude=$latitudeValue" +
            "&longitude=$longitudeValue" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min," +
            "precipitation_probability_max" +
            "&forecast_days=6" +
            "&temperature_unit=celsius&timezone=auto"
        val response = executePublicJsonGet(
            url = url,
            failureMessage = "No fue posible obtener el pronóstico del tiempo."
        )
        return weatherForecastFromJson(response, latitude, longitude)
    }

    internal fun weatherForecastFromJson(
        response: JSONObject,
        requestedLatitude: Double,
        requestedLongitude: Double
    ): WeatherForecast {
        val daily = response.optJSONObject("daily")
            ?: throw RemoteConnectionException("El servicio meteorológico no devolvió pronóstico.")
        val dates = daily.optJSONArray("time")
        val codes = daily.optJSONArray("weather_code")
        val maxTemperatures = daily.optJSONArray("temperature_2m_max")
        val minTemperatures = daily.optJSONArray("temperature_2m_min")
        val precipitationProbabilities = daily.optJSONArray("precipitation_probability_max")
        if (dates == null || codes == null || maxTemperatures == null || minTemperatures == null) {
            throw RemoteConnectionException("El servicio meteorológico devolvió un pronóstico incompleto.")
        }

        val itemCount = minOf(
            dates.length(),
            codes.length(),
            maxTemperatures.length(),
            minTemperatures.length(),
            6
        )
        val days = (0 until itemCount).mapNotNull { index ->
            val date = dates.optString(index).trim()
            val weatherCode = codes.optInt(index, -1)
            val maximum = maxTemperatures.optDouble(index, Double.NaN)
            val minimum = minTemperatures.optDouble(index, Double.NaN)
            if (date.isBlank() || weatherCode < 0 || !maximum.isFinite() || !minimum.isFinite()) {
                return@mapNotNull null
            }
            WeatherForecastDay(
                date = date,
                weatherCode = weatherCode,
                condition = weatherConditionForWmoCode(weatherCode),
                temperatureMaxCelsius = maximum,
                temperatureMinCelsius = minimum,
                precipitationProbabilityPercent = precipitationProbabilities
                    ?.optInt(index, 0)
                    ?.coerceIn(0, 100)
                    ?: 0
            )
        }
        if (days.isEmpty()) {
            throw RemoteConnectionException("El servicio meteorológico no devolvió días válidos.")
        }
        return WeatherForecast(
            days = days,
            latitude = response.optDouble("latitude", requestedLatitude)
                .takeIf(Double::isFinite)
                ?: requestedLatitude,
            longitude = response.optDouble("longitude", requestedLongitude)
                .takeIf(Double::isFinite)
                ?: requestedLongitude
        )
    }

    internal fun weatherFromJson(
        response: JSONObject,
        requestedLatitude: Double,
        requestedLongitude: Double
    ): WeatherSnapshot {
        val current = response.optJSONObject("current")
            ?: throw RemoteConnectionException("El servicio meteorológico no devolvió datos actuales.")
        val temperature = current.firstDouble("temperature_2m")
            ?.takeIf(Double::isFinite)
            ?: throw RemoteConnectionException("El servicio meteorológico no devolvió temperatura.")
        val weatherCode = current.firstInt("weather_code")
            ?: throw RemoteConnectionException("El servicio meteorológico no devolvió el estado del cielo.")
        return weatherSnapshotFromValues(
            temperatureCelsius = temperature,
            apparentTemperatureCelsius = current.firstDouble("apparent_temperature")
                ?.takeIf(Double::isFinite),
            weatherCode = weatherCode,
            isDay = current.optInt("is_day", 1) == 1,
            cloudCoverPercent = current.optInt("cloud_cover", 0),
            precipitationMillimeters = current.optDouble("precipitation", 0.0)
                .takeIf(Double::isFinite)
                ?: 0.0,
            observedAt = current.optString("time"),
            latitude = response.optDouble("latitude", requestedLatitude)
                .takeIf(Double::isFinite)
                ?: requestedLatitude,
            longitude = response.optDouble("longitude", requestedLongitude)
                .takeIf(Double::isFinite)
                ?: requestedLongitude
        )
    }

    internal fun weatherSnapshotFromValues(
        temperatureCelsius: Double,
        apparentTemperatureCelsius: Double?,
        weatherCode: Int,
        isDay: Boolean,
        cloudCoverPercent: Int,
        precipitationMillimeters: Double,
        observedAt: String,
        latitude: Double,
        longitude: Double
    ) = WeatherSnapshot(
        temperatureCelsius = temperatureCelsius,
        apparentTemperatureCelsius = apparentTemperatureCelsius,
        weatherCode = weatherCode,
        condition = weatherConditionForWmoCode(weatherCode),
        isDay = isDay,
        cloudCoverPercent = cloudCoverPercent.coerceIn(0, 100),
        precipitationMillimeters = precipitationMillimeters.coerceAtLeast(0.0),
        observedAt = observedAt,
        latitude = latitude,
        longitude = longitude
    )

    fun userFriendlyError(error: Throwable): String {
        val causes = generateSequence(error) { it.cause }.toList()
        val partialMessage = causes.filterIsInstance<RemotePartialSuccessException>()
            .firstOrNull()
            ?.message
        val remoteMessage = causes.filterIsInstance<RemoteConnectionException>()
            .firstOrNull()
            ?.message

        return when {
            !partialMessage.isNullOrBlank() -> partialMessage
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
        val action = payload.optString("action")
        return executeRequest(
            contentType = "application/json; charset=utf-8",
            includeAuthorization = shouldAuthenticateAction(action)
        ) { connection ->
            connection.outputStream.use { output ->
                output.write(payload.toString().toByteArray(StandardCharsets.UTF_8))
            }
        }
    }

    internal fun shouldAuthenticateAction(action: String): Boolean =
        action.isBlank() || action !in PUBLIC_ACTIONS

    internal fun shouldUseRegionalProfileFallback(
        error: Throwable,
        hasAccessToken: Boolean = accessToken.isNotBlank()
    ): Boolean {
        val remote = generateSequence(error) { it.cause }
            .filterIsInstance<RemoteConnectionException>()
            .firstOrNull()
            ?: return false
        return remote.statusCode in setOf(400, 404, 501) ||
            (remote.statusCode == 401 && !hasAccessToken)
    }

    internal fun isAuthenticationFailure(error: Throwable): Boolean {
        val remote = generateSequence(error) { it.cause }
            .filterIsInstance<RemoteConnectionException>()
            .firstOrNull()
            ?: return false
        if (remote.statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) return true

        val code = remote.remoteCode.orEmpty().lowercase(Locale.ROOT)
        if (code in setOf(
                "invalid_token",
                "token_invalid",
                "expired_token",
                "token_expired",
                "unauthorized"
            )
        ) return true

        val message = remote.message.orEmpty().lowercase(Locale.ROOT)
        val mentionsCredential = "token" in message || "sesión" in message || "sesion" in message
        val indicatesRejection = listOf(
            "inválid",
            "invalid",
            "expir",
            "vencid",
            "no autorizado",
            "unauthorized"
        ).any(message::contains)
        return mentionsCredential && indicatesRejection
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
            applyAuthorization(connection)
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
                    if (status in 200..299) {
                        "El endpoint ${pathFromUrl(url)} no devolvió una respuesta válida."
                    } else {
                        httpFailureMessage(status)
                    },
                    statusCode = status
                )
            }
            if (status !in 200..299 || !response.optBoolean("ok", true)) {
                throw RemoteConnectionException(
                    response.optString("message").ifBlank { httpFailureMessage(status) },
                    statusCode = status,
                    remoteCode = response.optString("code").takeIf(String::isNotBlank)
                )
            }
            return response
        } finally {
            connection.disconnect()
        }
    }

    private fun executeOpenStreetMapRequest(
        url: String,
        connectTimeoutMs: Int = 15_000,
        readTimeoutMs: Int = 20_000
    ): String {
        waitForGeocoderRateLimit()
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = connectTimeoutMs.coerceAtLeast(1_000)
            connection.readTimeout = readTimeoutMs.coerceAtLeast(1_000)
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Accept-Language", "es-CL,es;q=0.9")
            connection.setRequestProperty("User-Agent", GEOCODER_USER_AGENT)
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299 || text.isBlank()) {
                throw RemoteConnectionException(
                    "No fue posible buscar ubicaciones en este momento."
                )
            }
            return text
        } catch (error: RemoteConnectionException) {
            throw error
        } catch (error: Exception) {
            throw RemoteConnectionException(
                "No fue posible buscar ubicaciones: ${error.message ?: "error desconocido"}",
                error
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun waitForGeocoderRateLimit() {
        synchronized(geocoderRequestLock) {
            val now = System.currentTimeMillis()
            val remaining = GEOCODER_MIN_INTERVAL_MS - (now - lastGeocoderRequestAt)
            if (remaining > 0) Thread.sleep(remaining)
            lastGeocoderRequestAt = System.currentTimeMillis()
        }
    }

    private fun loadCommunityPhoto(
        action: String,
        parentField: String,
        parentId: String,
        photoId: String
    ): ByteArray {
        val response = postJson(
            JSONObject()
                .put("action", action)
                .put(parentField, parentId)
                .put("photo_id", photoId)
        )
        val photo = response.firstObject("photo", "item") ?: response
        val content = photo.optString("content_base64")
            .takeIf { it.isNotBlank() && it != "null" }
            ?: throw RemoteConnectionException(
                "El servidor no devolvió el contenido de la fotografía."
            )
        return decodePhotoContent(content)
    }

    private fun communityLocation(region: String, point: GeoPoint): String {
        val label = point.label
            .substringAfter('|', point.label)
            .replace('|', ' ')
            .replace('\n', ' ')
            .trim()
            .ifBlank { formatCoordinates(point) }
        return String.format(
            Locale.US,
            "%s:%.6f,%.6f|%s",
            region.uppercase(Locale.ROOT),
            point.latitude,
            point.longitude,
            label
        )
    }

    internal fun parseCommunityLocation(value: String): GeoPoint? {
        val match = Regex(
            """^\s*[A-Za-z]{3}:\s*(-?\d+(?:\.\d+)?),\s*(-?\d+(?:\.\d+)?)"""
        ).find(value) ?: return null
        val latitude = match.groupValues[1].toDoubleOrNull() ?: return null
        val longitude = match.groupValues[2].toDoubleOrNull() ?: return null
        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
        return GeoPoint(
            latitude = latitude,
            longitude = longitude,
            label = value.substringAfter('|', value)
        )
    }

    private fun MeetupEvent.hasCoordinates(): Boolean =
        latitude in -90.0..90.0 && longitude in -180.0..180.0 &&
            !(latitude == 0.0 && longitude == 0.0)

    private fun ProductPublication.hasCoordinates(): Boolean =
        latitude in -90.0..90.0 && longitude in -180.0..180.0 &&
            !(latitude == 0.0 && longitude == 0.0)

    private fun distanceKm(
        firstLatitude: Double,
        firstLongitude: Double,
        secondLatitude: Double,
        secondLongitude: Double
    ): Double {
        val earthRadiusKm = 6_371.0
        val latDistance = Math.toRadians(secondLatitude - firstLatitude)
        val lngDistance = Math.toRadians(secondLongitude - firstLongitude)
        val firstLat = Math.toRadians(firstLatitude)
        val secondLat = Math.toRadians(secondLatitude)
        val a = sin(latDistance / 2) * sin(latDistance / 2) +
            cos(firstLat) * cos(secondLat) *
            sin(lngDistance / 2) * sin(lngDistance / 2)
        return earthRadiusKm * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    private fun formatCoordinates(point: GeoPoint): String = String.format(
        Locale.US,
        "%.5f, %.5f",
        point.latitude,
        point.longitude
    )

    private fun pathFromUrl(url: String): String = url.substringAfter("$API_URL/")
        .substringBefore('?')

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private fun postMultipart(
        context: Context,
        fields: Map<String, String>,
        imageUri: Uri
    ): JSONObject {
        validateImageSize(context, imageUri)
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
                    copyImageWithLimit(input, output)
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
        validateImageSize(context, imageUri)
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
            applyAuthorization(connection)
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
                    copyImageWithLimit(input, output)
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
                    if (status in 200..299) {
                        "El endpoint ${pathFromUrl(connection.url.toString())} no devolvió una respuesta válida."
                    } else {
                        httpFailureMessage(status)
                    },
                    statusCode = status
                )
            }
            if (status !in 200..299 || !response.optBoolean("ok", true)) {
                throw RemoteConnectionException(
                    response.optString("message").ifBlank { httpFailureMessage(status) },
                    statusCode = status,
                    remoteCode = response.optString("code").takeIf(String::isNotBlank)
                )
            }
            return response
        } finally {
            connection.disconnect()
        }
    }

    private fun executePublicJsonGet(
        url: String,
        failureMessage: String,
        connectTimeoutMs: Int = 12_000,
        readTimeoutMs: Int = 15_000
    ): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = connectTimeoutMs
            connection.readTimeout = readTimeoutMs
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", GEOCODER_USER_AGENT)
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299 || text.isBlank()) {
                throw RemoteConnectionException(failureMessage, statusCode = status)
            }
            return runCatching { JSONObject(text) }.getOrElse { cause ->
                throw RemoteConnectionException("$failureMessage Respuesta inválida.", cause)
            }
        } catch (error: RemoteConnectionException) {
            throw error
        } catch (error: Exception) {
            throw RemoteConnectionException(failureMessage, error)
        } finally {
            connection.disconnect()
        }
    }

    private fun decodePhotoContent(encodedContent: String): ByteArray {
        val estimatedSize = encodedContent.length.toLong() * 3L / 4L
        if (estimatedSize > MAX_IMAGE_BYTES) {
            throw RemoteConnectionException("La fotografía recibida supera el máximo de 20 MB.")
        }
        return runCatching { Base64.decode(encodedContent, Base64.DEFAULT) }
            .getOrElse {
                throw RemoteConnectionException("La fotografía recibida no es válida.", it)
            }
            .also { bytes ->
                if (bytes.size.toLong() > MAX_IMAGE_BYTES) {
                    throw RemoteConnectionException(
                        "La fotografía recibida supera el máximo de 20 MB."
                    )
                }
            }
    }

    private fun validateImageSize(context: Context, imageUri: Uri) {
        val length = runCatching {
            context.contentResolver.openAssetFileDescriptor(imageUri, "r")?.use { it.length }
        }.getOrNull() ?: -1L
        if (length > MAX_IMAGE_BYTES) {
            throw RemoteConnectionException("La fotografía supera el máximo permitido de 20 MB.")
        }
    }

    private fun copyImageWithLimit(input: InputStream, output: OutputStream) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            if (total > MAX_IMAGE_BYTES) {
                throw RemoteConnectionException(
                    "La fotografía supera el máximo permitido de 20 MB."
                )
            }
            output.write(buffer, 0, count)
        }
    }

    private fun executeRequest(
        contentType: String,
        readTimeout: Int = 20_000,
        includeAuthorization: Boolean = true,
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
            if (includeAuthorization) applyAuthorization(connection)
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
                throw RemoteConnectionException(
                    if (statusCode in 200..299) {
                        "El servidor devolvió una respuesta no válida."
                    } else {
                        httpFailureMessage(statusCode)
                    },
                    statusCode = statusCode
                )
            }

            if (statusCode !in 200..299 || !response.optBoolean("ok", true)) {
                throw RemoteConnectionException(
                    response.optString("message").ifBlank {
                        httpFailureMessage(statusCode)
                    },
                    statusCode = statusCode,
                    remoteCode = response.optString("code").takeIf(String::isNotBlank)
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

    private fun applyAuthorization(connection: HttpURLConnection) {
        accessToken.takeIf(String::isNotBlank)?.let {
            connection.setRequestProperty("Authorization", "Bearer $it")
        }
    }

    private fun httpFailureMessage(statusCode: Int): String = when (statusCode) {
        400 -> "La solicitud no es válida. Revisa los datos e inténtalo nuevamente."
        401 -> "Tu sesión o tus credenciales no son válidas. Vuelve a iniciar sesión."
        403 -> "No tienes permiso para realizar esta acción."
        404 -> "El contenido solicitado ya no está disponible."
        409 -> "La información cambió en el servidor. Actualiza e inténtalo nuevamente."
        429 -> "Se hicieron demasiadas solicitudes. Espera un momento e inténtalo de nuevo."
        in 500..599 -> "El servidor no está disponible en este momento. Inténtalo más tarde."
        else -> "No fue posible completar la solicitud (HTTP $statusCode)."
    }

    internal fun bikeFromJson(item: JSONObject): Bike {
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

    private fun geoPointFromJson(item: JSONObject): GeoPoint {
        val latitude = item.firstDouble("latitude", "lat")
            ?: throw RemoteConnectionException("La ubicación no incluye latitud.")
        val longitude = item.firstDouble("longitude", "lng", "lon")
            ?: throw RemoteConnectionException("La ubicación no incluye longitud.")
        val countryCode = item.firstString("country_code", "countryCode")
            .orEmpty().uppercase(Locale.ROOT)
        val administrativeArea = item.firstString(
            "administrative_area", "administrativeArea", "state", "region_name"
        ).orEmpty()
        return GeoPoint(
            latitude = latitude,
            longitude = longitude,
            label = item.firstString("label", "display_name", "name")
                ?: formatCoordinates(GeoPoint(latitude, longitude)),
            countryCode = countryCode,
            administrativeArea = administrativeArea,
            regionCode = item.firstString("region_code", "regionCode")
                ?: communityRegionCodeFor(countryCode, administrativeArea),
            currencyCode = item.firstString("currency", "currency_code")
                ?: currencyCodeForCountry(countryCode)
        )
    }

    internal fun meetupFromJson(item: JSONObject): MeetupEvent {
        val location = item.optString("location")
        val descriptionParts = decodeMeetupDescription(
            rawDescription = item.optString("description"),
            explicitDateTime = item.firstString("dateTime", "date_time").orEmpty()
        )
        val point = parseCommunityLocation(location)
        val photos = item.optJSONArray("photos") ?: item.optJSONArray("images")
        val parentId = item.firstString("id", "event_id").orEmpty()
        return MeetupEvent(
            id = parentId,
            title = item.optString("title"),
            dateTime = descriptionParts.dateTime,
            description = descriptionParts.description,
            latitude = item.firstDouble("latitude", "lat") ?: point?.latitude ?: 0.0,
            longitude = item.firstDouble("longitude", "lng") ?: point?.longitude ?: 0.0,
            createdBy = item.firstString("createdBy", "created_by", "user_id").orEmpty(),
            createdByUsername = item.firstString("nombre_de_usuario", "username"),
            createdAt = item.firstString("createdAt", "created_at").orEmpty(),
            status = item.firstString("junta_status", "status").orEmpty(),
            region = item.firstString("region", "region_code")
                ?: item.firstString("id")?.take(3)
                ?: DEFAULT_COMMUNITY_REGION,
            location = location,
            photoFolderId = item.optString("photo_folder_id"),
            images = communityImages(item, photos, "junta", parentId),
            distanceKm = item.firstDouble("distance_km", "distanceKm"),
            countryCode = item.firstString("country_code", "countryCode").orEmpty(),
            administrativeArea = item.firstString(
                "administrative_area", "administrativeArea"
            ).orEmpty()
        )
    }

    internal fun marketplaceFromJson(item: JSONObject): ProductPublication {
        val location = item.optString("location")
        val point = parseCommunityLocation(location)
        val photos = item.optJSONArray("photos") ?: item.optJSONArray("images")
        val productStatus = item.firstString("product_status", "condition").orEmpty()
        val parentId = item.firstString("id", "post_id", "publication_id").orEmpty()
        val creatorUsername = item.firstString("nombre_de_usuario", "username")
        return ProductPublication(
            id = parentId,
            title = item.optString("title"),
            description = item.optString("description"),
            price = item.optString("price"),
            category = item.optString("category"),
            condition = productStatus,
            seller = creatorUsername
                ?: item.firstString("seller", "createdBy", "created_by", "user_id")
                .orEmpty(),
            mediaDescription = "",
            latitude = item.firstDouble("latitude", "lat") ?: point?.latitude ?: 0.0,
            longitude = item.firstDouble("longitude", "lng") ?: point?.longitude ?: 0.0,
            images = communityImages(item, photos, "market", parentId),
            createdBy = item.firstString("createdBy", "created_by", "user_id").orEmpty(),
            createdAt = item.firstString("createdAt", "created_at").orEmpty(),
            brand = item.optString("brand"),
            model = item.optString("model"),
            productType = item.firstString("productType", "product_type", "type").orEmpty(),
            publicationStatus = item.firstString("publication_status", "status").orEmpty(),
            productStatus = productStatus,
            region = item.firstString("region", "region_code")
                ?: item.firstString("id")?.take(3)
                ?: DEFAULT_COMMUNITY_REGION,
            location = location,
            photoFolderId = item.optString("photo_folder_id"),
            currencyCode = item.firstString("currency", "currency_code") ?: "CLP",
            distanceKm = item.firstDouble("distance_km", "distanceKm"),
            countryCode = item.firstString("country_code", "countryCode").orEmpty(),
            administrativeArea = item.firstString(
                "administrative_area", "administrativeArea"
            ).orEmpty(),
            createdByUsername = creatorUsername
        )
    }

    internal fun chatFromJson(item: JSONObject): UserChat {
        val rawType = item.firstString("type", "chat_type")
            .orEmpty()
            .uppercase(Locale.ROOT)
        val lastMessageObject = item.firstObject("lastMessage", "last_message")
        val participants = item.firstArray("participants", "participant_ids")
        return UserChat(
            id = item.firstString("id", "chat_id").orEmpty(),
            type = runCatching { ChatType.valueOf(rawType) }
                .getOrDefault(ChatType.SOCIAL),
            participants = participants.mapParticipantIds(),
            relatedEntityId = item.firstString("relatedEntityId", "related_entity_id"),
            lastMessage = lastMessageObject
                ?.firstString("content", "message", "body", "text")
                ?: item.firstString("lastMessage", "last_message").orEmpty(),
            messageCount = item.firstInt("messageCount", "message_count", "total_messages")
                ?: 0,
            version = item.firstLongValue("version") ?: 0L,
            updatedAt = item.firstString("updatedAt", "updated_at").orEmpty(),
            title = item.firstString("title", "display_name").orEmpty(),
            participantUsernames = participants.mapParticipantUsernames(),
            lastMessageId = lastMessageObject
                ?.firstString("id", "message_id")
                ?: item.firstString("last_message_id").orEmpty(),
            lastMessageSenderId = lastMessageObject?.firstString(
                "senderId",
                "sender_id",
                "sender_user_id",
                "user_id"
            ).orEmpty(),
            lastMessageSenderUsername = lastMessageObject?.firstString(
                "sender_nombre_de_usuario",
                "nombre_de_usuario",
                "sender_username"
            )
        )
    }

    internal fun messageFromJson(item: JSONObject) = StoredMessage(
        id = item.firstString("id", "message_id").orEmpty(),
        chatId = item.firstString("chatId", "chat_id").orEmpty(),
        senderId = item.firstString(
            "senderId",
            "sender_id",
            "sender_user_id",
            "user_id"
        ).orEmpty(),
        content = item.firstString("content", "message", "body", "text").orEmpty(),
        createdAt = item.firstString("createdAt", "created_at", "sent_at").orEmpty(),
        localStatus = item.firstString("localStatus", "local_status"),
        senderUsername = item.firstString(
            "sender_nombre_de_usuario",
            "nombre_de_usuario",
            "sender_username"
        )
    )

    private fun sportsPlatformFromJson(item: JSONObject): SyncPlatform {
        val id = item.firstString("provider", "id", "platform").orEmpty()
        return SyncPlatform(
            id = id,
            name = item.firstString("name", "display_name")
                ?: id.replaceFirstChar(Char::uppercase),
            description = item.optString("description"),
            connected = item.optBoolean("connected", item.optString("status") == "connected"),
            connectedAt = item.firstString("connected_at", "connectedAt").orEmpty()
        )
    }

    private fun photoSource(photoId: String, path: String): String {
        if (photoId.isNotBlank() && photoId != "null") {
            return "appbike-photo://$photoId"
        }
        return safePublicPhotoUrl(path)
    }

    internal fun safePublicPhotoUrl(path: String): String {
        val clean = path.trim()
        if (clean.isBlank() || clean.equals("null", ignoreCase = true)) return ""
        val lower = clean.lowercase(Locale.ROOT)
        val exposesInternalPath = lower.contains("bikesphotos/personalbikesphotos") ||
            lower.contains("appbikeinternal") ||
            lower.contains("internal-auth") ||
            lower.contains("/srv/") ||
            lower.contains("/var/") ||
            clean.contains('\\') ||
            clean.contains("..") ||
            Regex("^[A-Za-z]:[/\\\\]").containsMatchIn(clean)
        if (exposesInternalPath || clean.startsWith("//")) return ""

        if (lower.startsWith("https://")) {
            val host = runCatching { URL(clean).host.lowercase(Locale.ROOT) }.getOrNull()
                ?: return ""
            return clean.takeIf { host == "zizzio.cl" || host.endsWith(".zizzio.cl") }
                .orEmpty()
        }
        if (Regex("^[A-Za-z][A-Za-z0-9+.-]*:").containsMatchIn(clean)) return ""
        return PUBLIC_BASE_URL + clean.trimStart('/')
    }

    private fun requireBikeId(bike: Bike): Long =
        bike.remoteId ?: throw RemoteConnectionException(
            "La bicicleta no tiene un BikeID del servidor. Actualiza Mis bicicletas."
        )

    private fun requireApiDate(rawDate: String): String {
        val clean = rawDate.trim()
        val isRealDate = Regex("""^\d{4}-\d{2}-\d{2}$""").matches(clean) &&
            runCatching {
                SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
                    isLenient = false
                }.parse(clean) != null
            }.getOrDefault(false)
        if (!isRealDate) {
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

    private fun JSONArray?.mapParticipantIds(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                when (val value = opt(index)) {
                    is String -> value.takeIf {
                        it.isNotBlank() && it != "null"
                    }?.let(::add)
                    is JSONObject -> value.firstString(
                        "user_id",
                        "id",
                        "participant_user_id"
                    )?.let(::add)
                }
            }
        }.distinct()
    }

    private fun JSONArray?.mapParticipantUsernames(): Map<String, String> {
        if (this == null) return emptyMap()
        return buildMap {
            for (index in 0 until length()) {
                val value = optJSONObject(index) ?: continue
                val userId = value.firstString("user_id", "id", "participant_user_id")
                    ?: continue
                val username = value.firstString("nombre_de_usuario", "username")
                    ?: continue
                put(userId, username)
            }
        }
    }

    private fun JSONArray?.mapCommunityImages(type: String, parentId: String): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                val raw = opt(index)
                when (raw) {
                    is String -> safePublicPhotoUrl(raw).takeIf(String::isNotBlank)?.let(::add)
                    is JSONObject -> {
                        val photoId = raw.firstString("photo_id", "id")
                        val url = raw.firstString("url", "image_url", "path")
                        when {
                            !photoId.isNullOrBlank() && parentId.isNotBlank() ->
                                add("appbike-$type-photo://$parentId/$photoId")
                            !url.isNullOrBlank() -> safePublicPhotoUrl(url)
                                .takeIf(String::isNotBlank)
                                ?.let(::add)
                        }
                    }
                }
            }
        }
    }

    private fun communityImages(
        item: JSONObject,
        photos: JSONArray?,
        type: String,
        parentId: String
    ): List<String> {
        val listed = photos.mapCommunityImages(type, parentId)
        if (listed.isNotEmpty()) return listed
        val photoId = item.firstString("photo_id", "cover_photo_id", "first_photo_id")
        return if (!photoId.isNullOrBlank() && parentId.isNotBlank()) {
            listOf("appbike-$type-photo://$parentId/$photoId")
        } else {
            emptyList()
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
                null -> null
                is Number -> value.toLong()
                is String -> value.toLongOrNull()
                else -> null
            }
            if (parsed != null && parsed > 0) return parsed
        }
        return null
    }

    private fun JSONObject.firstLongValue(vararg keys: String): Long? {
        keys.forEach { key ->
            if (!has(key) || isNull(key)) return@forEach
            val parsed = when (val value = opt(key)) {
                null -> null
                is Number -> value.toLong()
                is String -> value.toLongOrNull()
                else -> null
            }
            if (parsed != null && parsed >= 0L) return parsed
        }
        return null
    }

    private fun JSONObject.firstInt(vararg keys: String): Int? {
        keys.forEach { key ->
            if (!has(key) || isNull(key)) return@forEach
            val parsed = when (val value = opt(key)) {
                null -> null
                is Number -> value.toInt()
                is String -> value.toIntOrNull()
                else -> null
            }
            if (parsed != null && parsed >= 0) return parsed
        }
        return null
    }

    private fun JSONObject.firstDouble(vararg keys: String): Double? {
        keys.forEach { key ->
            if (!has(key) || isNull(key)) return@forEach
            val parsed = when (val value = opt(key)) {
                null -> null
                is Number -> value.toDouble()
                is String -> value.toDoubleOrNull()
                else -> null
            }
            if (parsed != null) return parsed
        }
        return null
    }

    private fun JSONObject.firstString(vararg keys: String): String? {
        keys.forEach { key ->
            if (!has(key) || isNull(key)) return@forEach
            val raw = opt(key)
            if (raw is JSONObject || raw is JSONArray) return@forEach
            val value = raw?.toString()?.trim().orEmpty()
            if (value.isNotEmpty() && value != "null") return value
        }
        return null
    }

    class RemoteConnectionException(
        message: String,
        cause: Throwable? = null,
        val statusCode: Int? = null,
        val remoteCode: String? = null
    ) : RuntimeException(message, cause)

    class RemotePartialSuccessException(
        message: String,
        val entityId: String,
        cause: Throwable? = null
    ) : RuntimeException(message, cause)
}
