package com.example.appbike

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.example.appbike.ui.theme.AppPrimaryBright
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val PROFILE_MARKETPLACE_STATUSES = listOf(
    "activa" to "Activas",
    "pausada" to "Pausadas",
    "vendida" to "Vendidas",
    "en_revision" to "En revisión"
)

@Composable
internal fun ProfileContentSection(
    account: AccountSession,
    isActive: Boolean = true,
    onSocialPostCountChanged: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val publications = remember(account.userId) { mutableStateListOf<ProductPublication>() }
    val meetups = remember(account.userId) { mutableStateListOf<MeetupEvent>() }
    val socialPosts = remember(account.userId) {
        mutableStateListOf<SocialPost>().apply {
            addAll(LocalDataStore.loadSocialPosts(context, account.userId))
        }
    }
    var publicationStatus by remember(account.userId) { mutableStateOf("activa") }
    var meetupStatus by remember(account.userId) { mutableStateOf("activa") }
    var selectedPublication by remember(account.userId) {
        mutableStateOf<ProductPublication?>(null)
    }
    var selectedMeetup by remember(account.userId) { mutableStateOf<MeetupEvent?>(null) }
    var loading by remember(account.userId) { mutableStateOf(false) }
    var error by remember(account.userId) { mutableStateOf<String?>(null) }
    var hasLoaded by remember(account.userId) { mutableStateOf(false) }
    var showSocialComposer by remember(account.userId) { mutableStateOf(false) }

    LaunchedEffect(socialPosts.size) {
        onSocialPostCountChanged(socialPosts.size)
    }

    suspend fun reload() {
        try {
            loading = true
            error = null
            val center = LocalDataStore.loadMarketplaceLocation(context)
                ?: LocalDataStore.loadLocation(context)
                ?: GeoPoint(-33.4489, -70.6693, "Santiago, Chile", countryCode = "CL")
            val (publicationsResult, meetupsResult) = withContext(Dispatchers.IO) {
                val publicationsDeferred = async {
                    runSuspendCatching {
                        RemoteConnections.loadOwnMarketplacePosts(account.userId, center)
                    }
                }
                val meetupsDeferred = async {
                    runSuspendCatching {
                        RemoteConnections.loadOwnMeetups(account.userId, center)
                    }
                }
                publicationsDeferred.await() to meetupsDeferred.await()
            }
            publicationsResult.onSuccess { ownPublications ->
                publications.clear()
                publications.addAll(ownPublications)
            }
            meetupsResult.onSuccess { ownMeetups ->
                meetups.clear()
                meetups.addAll(ownMeetups)
            }
            error = listOfNotNull(
                publicationsResult.exceptionOrNull(),
                meetupsResult.exceptionOrNull()
            ).map(RemoteConnections::userFriendlyError)
                .distinct()
                .joinToString("\n")
                .takeIf(String::isNotBlank)
        } finally {
            loading = false
            hasLoaded = true
        }
    }

    LaunchedEffect(account.userId, isActive) {
        if (isActive && !hasLoaded) reload()
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Tu contenido", style = MaterialTheme.typography.titleLarge)
        Text(
            "Comparte experiencias con riders y administra tus espacios de Marketplace.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text("Publicaciones sociales", fontWeight = FontWeight.Bold)
        Text(
            "Fotos, videos y aventuras personales. Se guardan en este dispositivo hasta que el backend social esté disponible.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { showSocialComposer = true }
        ) {
            Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Publicar una experiencia")
        }
        if (socialPosts.isEmpty()) {
            Text(
                "Aún no has compartido una experiencia.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            socialPosts.forEach { post ->
                SocialPostItemCard(
                    title = post.caption.ifBlank { "Experiencia de ${post.username}" },
                    detail = if (post.mediaType == "video") "Video personal" else "Foto personal",
                    status = "Publicado en tu perfil"
                )
            }
        }

        Text("Ventas en Marketplace", fontWeight = FontWeight.Bold)
        if (loading) {
            AppSkeleton(Modifier.fillMaxWidth().height(96.dp))
        }
        error?.let {
            ErrorBanner(it)
            TextButton(onClick = { scope.launch { reload() } }, enabled = !loading) {
                Text("Reintentar carga de actividad")
            }
        }

        Text("Publicaciones", fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PROFILE_MARKETPLACE_STATUSES.forEach { (status, label) ->
                FilterChip(
                    selected = publicationStatus == status,
                    onClick = { publicationStatus = status },
                    label = { Text(label) }
                )
            }
        }
        val visiblePublications = publications.filter {
            it.publicationStatus.ifBlank { "activa" } == publicationStatus
        }
        if (!loading && visiblePublications.isEmpty()) {
            Text(
                "No tienes publicaciones en este estado.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        visiblePublications.forEach { publication ->
            ProfileItemCard(
                title = publication.title,
                detail = formatMarketplacePrice(
                    publication.price,
                    marketplaceCurrency(publication.currencyCode)
                ),
                status = PROFILE_MARKETPLACE_STATUSES.firstOrNull {
                    it.first == publication.publicationStatus
                }?.second ?: publication.publicationStatus,
                onClick = { selectedPublication = publication }
            )
        }

        Text("Juntas creadas", fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = meetupStatus == "activa",
                onClick = { meetupStatus = "activa" },
                label = { Text("Actuales") }
            )
            FilterChip(
                selected = meetupStatus == "pasada",
                onClick = { meetupStatus = "pasada" },
                label = { Text("Anteriores") }
            )
        }
        val visibleMeetups = meetups.filter { it.status.ifBlank { "activa" } == meetupStatus }
        if (!loading && visibleMeetups.isEmpty()) {
            Text(
                if (meetupStatus == "pasada") {
                    "Aún no tienes juntas anteriores."
                } else {
                    "No tienes juntas actuales."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        visibleMeetups.forEach { meetup ->
            ProfileItemCard(
                title = meetup.title,
                detail = meetup.dateTime.ifBlank {
                    meetup.location.substringAfter('|', meetup.location)
                },
                status = if (meetup.status == "pasada") "Anterior" else "Actual",
                onClick = { selectedMeetup = meetup }
            )
        }
    }

    selectedPublication?.let { publication ->
        OwnPublicationDialog(
            account = account,
            publication = publication,
            onDismiss = { selectedPublication = null },
            onChanged = {
                selectedPublication = null
                scope.launch { reload() }
            }
        )
    }
    selectedMeetup?.let { meetup ->
        OwnMeetupDialog(
            account = account,
            meetup = meetup,
            onDismiss = { selectedMeetup = null },
            onChanged = {
                selectedMeetup = null
                scope.launch { reload() }
            }
        )
    }
    if (showSocialComposer) {
        SocialPostComposerDialog(
            account = account,
            onDismiss = { showSocialComposer = false },
            onSaved = { post ->
                LocalDataStore.saveSocialPost(context, post)
                socialPosts.removeAll { it.id == post.id }
                socialPosts.add(0, post)
                showSocialComposer = false
            }
        )
    }
}

@Composable
private fun SocialPostComposerDialog(
    account: AccountSession,
    onDismiss: () -> Unit,
    onSaved: (SocialPost) -> Unit
) {
    val context = LocalContext.current
    var mediaUri by remember { mutableStateOf("") }
    var mediaType by remember { mutableStateOf("photo") }
    var caption by remember { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        mediaUri = uri.toString()
        mediaType = if (context.contentResolver.getType(uri)?.startsWith("video/") == true) {
            "video"
        } else {
            "photo"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva experiencia") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Comparte una foto o video de tu última salida.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it.take(280) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .appBikeTextFieldGlow(),
                    label = { Text("¿Qué quieres contar?") },
                    minLines = 3,
                    maxLines = 5,
                    colors = appBikeTextFieldColors(),
                    shape = RoundedCornerShape(14.dp)
                )
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        picker.launch(arrayOf("image/*", "video/*"))
                    }
                ) {
                    Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(if (mediaUri.isBlank()) "Añadir foto o video" else "Cambiar multimedia")
                }
                if (mediaUri.isNotBlank()) {
                    Text(
                        if (mediaType == "video") "Video listo para publicar" else "Foto lista para publicar",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppPrimaryBright
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = mediaUri.isNotBlank(),
                onClick = {
                    onSaved(
                        SocialPost(
                            id = "${account.userId}-${System.currentTimeMillis()}",
                            userId = account.userId,
                            username = account.username ?: account.email.substringBefore('@'),
                            caption = caption.trim(),
                            mediaUri = mediaUri,
                            mediaType = mediaType,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }
            ) { Text("Publicar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun ProfileItemCard(
    title: String,
    detail: String,
    status: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title.ifBlank { "Sin título" }, fontWeight = FontWeight.Bold)
            if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodySmall)
            Text(status, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SocialPostItemCard(
    title: String,
    detail: String,
    status: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title.ifBlank { "Experiencia personal" }, fontWeight = FontWeight.Bold)
            Text(detail, style = MaterialTheme.typography.bodySmall)
            Text(status, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun OwnPublicationDialog(
    account: AccountSession,
    publication: ProductPublication,
    onDismiss: () -> Unit,
    onChanged: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember(publication.id) { mutableStateOf(publication.title) }
    var description by remember(publication.id) { mutableStateOf(publication.description) }
    var price by remember(publication.id) {
        mutableStateOf(normalizeWholeUnitInput(publication.price))
    }
    var productStatus by remember(publication.id) {
        mutableStateOf(publication.productStatus.ifBlank { "usado" })
    }
    var editing by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val publicationCurrency = marketplaceCurrency(publication.currencyCode)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            scope.launch {
                loading = true
                runSuspendCatching {
                    withContext(Dispatchers.IO) {
                        RemoteConnections.uploadMarketplacePhoto(
                            context, account.userId, publication.id, uri.toString()
                        )
                    }
                }.onSuccess { onChanged() }
                    .onFailure { error = RemoteConnections.userFriendlyError(it) }
                loading = false
            }
        }
    }

    fun runAction(action: () -> Unit) {
        scope.launch {
            loading = true
            error = null
            runSuspendCatching { withContext(Dispatchers.IO) { action() } }
                .onSuccess { onChanged() }
                .onFailure { error = RemoteConnections.userFriendlyError(it) }
            loading = false
        }
    }

    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        title = { Text(if (editing) "Editar publicación" else publication.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (editing) {
                    OutlinedTextField(
                        title,
                        { title = it },
                        enabled = !loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .appBikeTextFieldGlow(),
                        label = { Text("Título") },
                        shape = RoundedCornerShape(14.dp),
                        colors = appBikeTextFieldColors()
                    )
                    OutlinedTextField(
                        description,
                        { description = it },
                        enabled = !loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .appBikeTextFieldGlow(),
                        label = { Text("Descripción") },
                        minLines = 3,
                        shape = RoundedCornerShape(14.dp),
                        colors = appBikeTextFieldColors()
                    )
                    OutlinedTextField(
                        price,
                        { price = normalizeWholeUnitInput(it) },
                        enabled = !loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .appBikeTextFieldGlow(),
                        label = { Text("Precio (${publication.currencyCode})") },
                        prefix = { Text(publicationCurrency.symbol) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        colors = appBikeTextFieldColors()
                    )
                    ProductStatusSelector(
                        selectedStatus = productStatus,
                        enabled = !loading,
                        onStatusSelected = { productStatus = it }
                    )
                } else {
                    Text(
                        formatMarketplacePrice(
                            publication.price,
                            marketplaceCurrency(publication.currencyCode)
                        ),
                        fontWeight = FontWeight.Bold
                    )
                    Text(publication.description)
                    Text("Estado: ${publication.publicationStatus}")
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (loading) CircularProgressIndicator()
                if (!editing) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PROFILE_MARKETPLACE_STATUSES.forEach { (status, label) ->
                            FilterChip(
                                selected = publication.publicationStatus == status,
                                enabled = !loading,
                                onClick = {
                                    runAction {
                                        RemoteConnections.updateMarketplaceStatus(
                                            account.userId, publication.id, status
                                        )
                                    }
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (editing) {
                Button(
                    enabled = !loading && title.isNotBlank() && description.isNotBlank() &&
                        parseMarketplaceWholeUnitPrice(price) != null,
                    onClick = {
                        runAction {
                            RemoteConnections.updateMarketplacePost(
                                account.userId,
                                publication.copy(
                                    title = title,
                                    description = description,
                                    price = price,
                                    productStatus = productStatus
                                )
                            )
                        }
                    }
                ) { Text("Guardar") }
            } else {
                TextButton(enabled = !loading, onClick = { editing = true }) {
                    Icon(Icons.Outlined.Edit, contentDescription = null)
                    Text("Editar")
                }
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    enabled = !loading,
                    onClick = {
                        picker.launch(arrayOf("image/jpeg", "image/png", "image/webp"))
                    }
                ) { Icon(Icons.Outlined.PhotoCamera, contentDescription = "Agregar foto") }
                TextButton(enabled = !loading, onClick = { confirmDelete = true }) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Eliminar")
                }
                TextButton(
                    enabled = !loading,
                    onClick = if (editing) ({ editing = false }) else onDismiss
                ) {
                    Text(if (editing) "Cancelar" else "Cerrar")
                }
            }
        }
    )

    if (confirmDelete) {
        ConfirmDeleteDialog(
            label = "esta publicación",
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                runAction { RemoteConnections.deleteMarketplacePost(account.userId, publication.id) }
            }
        )
    }
}

@Composable
private fun OwnMeetupDialog(
    account: AccountSession,
    meetup: MeetupEvent,
    onDismiss: () -> Unit,
    onChanged: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember(meetup.id) { mutableStateOf(meetup.title) }
    var description by remember(meetup.id) { mutableStateOf(meetup.description) }
    var editing by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    fun runAction(action: () -> Unit) {
        scope.launch {
            loading = true
            error = null
            runSuspendCatching { withContext(Dispatchers.IO) { action() } }
                .onSuccess { onChanged() }
                .onFailure { error = RemoteConnections.userFriendlyError(it) }
            loading = false
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            runAction {
                RemoteConnections.uploadMeetupPhoto(
                    context, account.userId, meetup.id, uri.toString()
                )
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        title = { Text(if (editing) "Editar junta" else meetup.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (editing) {
                    OutlinedTextField(
                        title,
                        { title = it },
                        enabled = !loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .appBikeTextFieldGlow(),
                        label = { Text("Título") },
                        shape = RoundedCornerShape(14.dp),
                        colors = appBikeTextFieldColors()
                    )
                    OutlinedTextField(
                        description,
                        { description = it },
                        enabled = !loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .appBikeTextFieldGlow(),
                        label = { Text("Descripción") },
                        minLines = 3,
                        shape = RoundedCornerShape(14.dp),
                        colors = appBikeTextFieldColors()
                    )
                } else {
                    Text(if (meetup.status == "pasada") "Junta anterior" else "Junta actual")
                    if (meetup.dateTime.isNotBlank()) Text(meetup.dateTime)
                    Text(meetup.description)
                    Text(meetup.location.substringAfter('|', meetup.location))
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (loading) CircularProgressIndicator()
                if (!editing && meetup.status != "pasada") {
                    Button(
                        enabled = !loading,
                        onClick = {
                            runAction {
                                RemoteConnections.completeMeetup(account.userId, meetup.id)
                            }
                        }
                    ) { Text("Marcar como realizada") }
                }
            }
        },
        confirmButton = {
            if (editing) {
                Button(
                    enabled = !loading && title.isNotBlank() && description.isNotBlank(),
                    onClick = {
                        runAction {
                            RemoteConnections.updateMeetupEvent(
                                account.userId,
                                meetup.copy(title = title, description = description)
                            )
                        }
                    }
                ) { Text("Guardar") }
            } else {
                TextButton(enabled = !loading, onClick = { editing = true }) {
                    Icon(Icons.Outlined.Edit, contentDescription = null)
                    Text("Editar")
                }
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    enabled = !loading,
                    onClick = {
                        picker.launch(arrayOf("image/jpeg", "image/png", "image/webp"))
                    }
                ) { Icon(Icons.Outlined.PhotoCamera, contentDescription = "Agregar foto") }
                TextButton(enabled = !loading, onClick = { confirmDelete = true }) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Eliminar")
                }
                TextButton(
                    enabled = !loading,
                    onClick = if (editing) ({ editing = false }) else onDismiss
                ) {
                    Text(if (editing) "Cancelar" else "Cerrar")
                }
            }
        }
    )

    if (confirmDelete) {
        ConfirmDeleteDialog(
            label = "esta junta",
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                runAction { RemoteConnections.deleteMeetupEvent(account.userId, meetup.id) }
            }
        )
    }
}

@Composable
private fun ConfirmDeleteDialog(label: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirmar eliminación") },
        text = { Text("¿Seguro que quieres eliminar $label? Esta acción no se puede deshacer.") },
        confirmButton = { Button(onClick = onConfirm) { Text("Eliminar") } },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
