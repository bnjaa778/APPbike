package com.example.appbike

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.exifinterface.media.ExifInterface
import androidx.core.graphics.get
import androidx.core.net.toUri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.appbike.ui.theme.AppBorderSubtle
import com.example.appbike.ui.theme.AppPrimary
import com.example.appbike.ui.theme.AppPrimaryBright
import com.example.appbike.ui.theme.AppSurfaceElevated
import com.example.appbike.ui.theme.AppTextPrimary
import com.example.appbike.ui.theme.AppTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal const val BIKE_SUMMARY_CARD_TEST_TAG = "bike_summary_card"
internal const val BIKE_EDIT_ACTION_TEST_TAG = "bike_edit_action"
internal const val BIKE_DELETE_ACTION_TEST_TAG = "bike_delete_action"
internal const val BIKE_SAVE_CHANGES_TEST_TAG = "bike_save_changes"
internal const val BIKE_CONFIRM_DELETE_TEST_TAG = "bike_confirm_delete"
internal const val MAINTENANCE_EDIT_TEST_TAG = "maintenance_edit"
internal const val MAINTENANCE_DELETE_TEST_TAG = "maintenance_delete"
internal const val SERVICE_EDIT_TEST_TAG = "service_edit"
internal const val SERVICE_COMPLETE_TEST_TAG = "service_complete"
internal const val SERVICE_DELETE_TEST_TAG = "service_delete"
internal const val MAINTENANCE_SAVE_TEST_TAG = "maintenance_save"
internal const val SERVICE_SAVE_TEST_TAG = "service_save"
internal const val SERVICE_CONFIRM_COMPLETE_TEST_TAG = "service_confirm_complete"

internal fun isValidApiDateInput(value: String): Boolean {
    val clean = value.trim()
    if (!clean.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) return false
    return runCatching {
        SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
            isLenient = false
        }.parse(clean) != null
    }.getOrDefault(false)
}

@Composable
fun BikesScreen(
    account: AccountSession?,
    bikes: MutableList<Bike>,
    reminders: MutableList<MaintenanceReminder>,
    bookings: MutableList<ServiceBooking>,
    onOpenAccount: () -> Unit,
    onBack: (() -> Unit)?
) {
    var showForm by remember(account?.userId) { mutableStateOf(false) }
    var selectedBike by remember(account?.userId) { mutableStateOf<Bike?>(null) }
    var bikeToEdit by remember(account?.userId) { mutableStateOf<Bike?>(null) }
    var bikePendingDeletion by remember(account?.userId) { mutableStateOf<Bike?>(null) }
    var isSavingBike by remember(account?.userId) { mutableStateOf(false) }
    var isUpdatingBike by remember(account?.userId) { mutableStateOf(false) }
    var isDeletingBike by remember(account?.userId) { mutableStateOf(false) }
    var bikeFormError by remember(account?.userId) { mutableStateOf<String?>(null) }
    var deleteBikeError by remember(account?.userId) { mutableStateOf<String?>(null) }
    var isLoadingBikes by remember { mutableStateOf(false) }
    var loadingBikeId by remember { mutableStateOf<Long?>(null) }
    var connectionWarning by remember { mutableStateOf<String?>(null) }
    var reloadRequest by remember { mutableIntStateOf(0) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(account?.userId, reloadRequest) {
        bikes.clear()
        reminders.clear()
        bookings.clear()
        connectionWarning = null
        if (account == null) return@LaunchedEffect

        isLoadingBikes = true
        runSuspendCatching {
            withContext(Dispatchers.IO) {
                RemoteConnections.loadUserBikes(account.userId)
            }
        }.onSuccess { remoteBikes ->
            bikes.clear()
            bikes.addAll(remoteBikes)
        }.onFailure { error ->
            connectionWarning = RemoteConnections.userFriendlyError(error)
        }
        isLoadingBikes = false
    }

    PremiumScreenBackground(PremiumGlowStyle.Bikes) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AppDimens.Space4)
                .padding(top = AppDimens.Space4)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppDimens.Space4)
        ) {
            if (onBack != null) Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onBack) {
                    Text("← Volver")
                }
            }

            Text(
                text = "Mis bicicletas",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                color = AppTextPrimary
            )

            if (connectionWarning != null && bikes.isNotEmpty()) {
                ErrorBanner(connectionWarning.orEmpty())
            }

            if (account == null) {
                EmptyState(
                    title = "Inicia sesión para ver tus bicicletas guardadas",
                    description = "Accede a tu cuenta para cargar tus bicicletas, fotos y mantenciones.",
                    actionLabel = "Iniciar sesión",
                    onAction = onOpenAccount
                )
            } else if (isLoadingBikes && bikes.isEmpty()) {
                LoadingState("Cargando bicicletas...")
            } else if (connectionWarning != null && bikes.isEmpty()) {
                EmptyState(
                    title = "No pudimos cargar tus bicicletas",
                    description = connectionWarning.orEmpty(),
                    actionLabel = "Reintentar",
                    onAction = { reloadRequest += 1 }
                )
            } else if (bikes.isEmpty()) {
                EmptyState(
                    title = "Aún no tienes bicicletas",
                    description = "Agrega tu primera bicicleta para guardar fotos, mantenciones y servicios.",
                    actionLabel = "Agregar bicicleta",
                    onAction = {
                        bikeFormError = null
                        showForm = true
                    }
                )
            } else {
                bikes.forEach { bike ->
                    BikeSummaryCard(bike) {
                        val activeAccount = account
                        val bikeId = bike.remoteId
                        if (bikeId == null) {
                            connectionWarning =
                                "La bicicleta no tiene un ID válido del servidor."
                        } else if (loadingBikeId == null) {
                            scope.launch {
                                loadingBikeId = bikeId
                                runSuspendCatching {
                                    withContext(Dispatchers.IO) {
                                        RemoteConnections.loadBikeDetails(
                                            activeAccount,
                                            bike
                                        )
                                    }
                                }.onSuccess { loadedBike ->
                                    connectionWarning = null
                                    val index = bikes.indexOfFirst {
                                        it.remoteId == loadedBike.remoteId
                                    }
                                    if (index >= 0) {
                                        bikes[index] = loadedBike
                                    }
                                    selectedBike = loadedBike
                                }.onFailure { error ->
                                    connectionWarning =
                                        RemoteConnections.userFriendlyError(error)
                                }
                                loadingBikeId = null
                            }
                        }
                    }
                }

                AddBikeButton {
                    bikeFormError = null
                    showForm = true
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showForm) {
        BikeFormDialog(
            isSaving = isSavingBike,
            errorMessage = bikeFormError,
            onDismiss = {
                if (!isSavingBike) {
                    showForm = false
                    bikeFormError = null
                }
            },
            onSave = saveBike@ { bike ->
                val activeAccount = account ?: return@saveBike
                if (!isSavingBike) {
                    scope.launch {
                        isSavingBike = true

                        val result = runSuspendCatching {
                            withContext(Dispatchers.IO) {
                                RemoteConnections.registerBike(context, activeAccount, bike)
                            }
                        }

                        result
                            .onSuccess { savedBike ->
                                connectionWarning = null
                                bikeFormError = null
                                bikes.add(0, savedBike)
                                showForm = false
                            }
                            .onFailure { error ->
                                bikeFormError = RemoteConnections.userFriendlyError(error)
                            }

                        isSavingBike = false
                    }
                }
            }
        )
    }

    bikeToEdit?.let { editingBike ->
        BikeFormDialog(
            initialBike = editingBike,
            isSaving = isUpdatingBike,
            errorMessage = bikeFormError,
            onDismiss = {
                if (!isUpdatingBike) {
                    bikeToEdit = null
                    bikeFormError = null
                    selectedBike = editingBike
                }
            },
            onSave = updateBike@ { candidate ->
                val activeAccount = account ?: return@updateBike
                if (!isUpdatingBike) {
                    scope.launch {
                        isUpdatingBike = true
                        bikeFormError = null
                        runSuspendCatching {
                            withContext(Dispatchers.IO) {
                                RemoteConnections.updateBike(activeAccount, candidate)
                            }
                        }.onSuccess { updatedBike ->
                            val index = bikes.indexOfFirst {
                                it.remoteId == updatedBike.remoteId
                            }
                            if (index >= 0) bikes[index] = updatedBike
                            connectionWarning = null
                            bikeFormError = null
                            bikeToEdit = null
                            selectedBike = updatedBike
                        }.onFailure { error ->
                            bikeFormError = RemoteConnections.userFriendlyError(error)
                        }
                        isUpdatingBike = false
                    }
                }
            }
        )
    }

    selectedBike?.let { bike ->
        BikeDetailDialog(
            account = account ?: return@let,
            bike = bike,
            reminders = reminders,
            bookings = bookings,
            isMutatingBike = isDeletingBike,
            onEdit = {
                bikeFormError = null
                selectedBike = null
                bikeToEdit = bike
            },
            onDelete = {
                deleteBikeError = null
                bikePendingDeletion = bike
            },
            onDismiss = {
                if (!isDeletingBike) selectedBike = null
            }
        )
    }

    bikePendingDeletion?.let { targetBike ->
        BikeDeleteConfirmationDialog(
            bike = targetBike,
            isDeleting = isDeletingBike,
            errorMessage = deleteBikeError,
            onDismiss = {
                bikePendingDeletion = null
                deleteBikeError = null
            },
            onConfirm = {
                val activeAccount = account
                if (activeAccount != null) scope.launch {
                    isDeletingBike = true
                    deleteBikeError = null
                    runSuspendCatching {
                        withContext(Dispatchers.IO) {
                            RemoteConnections.deleteBike(activeAccount, targetBike)
                        }
                    }.onSuccess {
                        val bikeId = targetBike.remoteId
                        bikes.removeAll { it.remoteId == bikeId }
                        reminders.removeAll { it.bikeId == bikeId }
                        bookings.removeAll { it.bikeId == bikeId }
                        if (selectedBike?.remoteId == bikeId) selectedBike = null
                        if (bikeToEdit?.remoteId == bikeId) bikeToEdit = null
                        bikePendingDeletion = null
                        connectionWarning = null
                    }.onFailure { error ->
                        deleteBikeError = RemoteConnections.userFriendlyError(error)
                    }
                    isDeletingBike = false
                }
            }
        )
    }

    if (loadingBikeId != null) {
        Dialog(onDismissRequest = {}) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator()
                    Text("Cargando información de la bicicleta...")
                }
            }
        }
    }

}

@Composable
internal fun BikeDeleteConfirmationDialog(
    bike: Bike,
    isDeleting: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (!isDeleting) onDismiss()
        },
        title = { Text("¿Eliminar ${bike.name}?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "La bicicleta y sus datos asociados dejarán de estar disponibles. " +
                            "Esta acción no se puede deshacer."
                )
                errorMessage?.let { ErrorBanner(it) }
            }
        },
        dismissButton = {
            TextButton(
                enabled = !isDeleting,
                onClick = onDismiss
            ) {
                Text("Cancelar")
            }
        },
        confirmButton = {
            TextButton(
                modifier = Modifier.testTag(BIKE_CONFIRM_DELETE_TEST_TAG),
                enabled = !isDeleting,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                onClick = onConfirm
            ) {
                Text(if (isDeleting) "Eliminando..." else "Eliminar")
            }
        }
    )
}

@Composable
internal fun AddBikeButton(onClick: () -> Unit) {
    val addBike = onClick
    Column(
        modifier = Modifier
            .clickable(onClick = addBike)
            .clearAndSetSemantics {
                contentDescription = "Agregar bicicleta"
                role = Role.Button
                onClick(label = "Agregar bicicleta") {
                    addBike()
                    true
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            color = AppPrimary,
            shadowElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    "+",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Light,
                    color = AppTextPrimary
                )
            }
        }

        Text(
            "Agregar bicicleta",
            fontWeight = FontWeight.SemiBold,
            color = AppTextSecondary
        )
    }
}

@Composable
internal fun BikeSummaryCard(
    bike: Bike,
    onClick: (() -> Unit)? = null
) {
    val cardModifier = Modifier
        .fillMaxWidth()
        .testTag(BIKE_SUMMARY_CARD_TEST_TAG)
        .then(
            if (onClick != null) Modifier.clickable(onClick = onClick)
            else Modifier
        )
    Card(
        modifier = cardModifier,
        shape = RoundedCornerShape(AppDimens.RadiusXLarge),
        colors = CardDefaults.cardColors(
            containerColor = AppSurfaceElevated
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            AppBorderSubtle
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.Space5),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppDimens.Space3)
        ) {
            Text(
                bike.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.55f)
            ) {
                BikeImageFrame(
                    imageUri = bike.imageUri,
                    contentDescription = "Fotografía de ${bike.name}"
                )
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                BikeMetric(
                    label = "Últ. mantención",
                    value = bike.lastMaintenance.ifBlank { "Sin datos" },
                    modifier = Modifier.weight(1f)
                )

                BikeMetric(
                    label = "Uso",
                    value = bike.distanceKm.ifBlank { "Sin datos" },
                    modifier = Modifier.weight(1f)
                )

                BikeMetric(
                    label = "Próx. mantención",
                    value = bike.nextMaintenance.ifBlank { "Sin datos" },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

}

@Composable
private fun BikeMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            value,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun BikeFormDialog(
    initialBike: Bike? = null,
    isSaving: Boolean,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onSave: (Bike) -> Unit
) {
    val isEditing = initialBike != null
    var name by remember(initialBike?.remoteId) { mutableStateOf(initialBike?.name.orEmpty()) }
    var brand by remember(initialBike?.remoteId) { mutableStateOf(initialBike?.brand.orEmpty()) }
    var model by remember(initialBike?.remoteId) { mutableStateOf(initialBike?.model.orEmpty()) }
    var type by remember(initialBike?.remoteId) { mutableStateOf(initialBike?.type.orEmpty()) }
    var serialNumber by remember(initialBike?.remoteId) {
        mutableStateOf(initialBike?.serialNumber.orEmpty())
    }
    var imageUri by remember(initialBike?.remoteId) {
        mutableStateOf(initialBike?.imageUri.orEmpty())
    }

    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            imageUri = uri.toString()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 700.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    if (isEditing) "Editar bicicleta" else "Agregar bicicleta",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                errorMessage?.let { ErrorBanner(it) }

                AppInput("Nombre de la bicicleta", name) {
                    name = it
                }

                AppInput("Marca", brand) {
                    brand = it
                }

                AppInput("Modelo", model) {
                    model = it
                }

                AppInput("Tipo: MTB, Ruta, Gravel, Urbana", type) {
                    type = it
                }

                AppInput("Número de serie", serialNumber) {
                    serialNumber = it
                }

                Text(
                    if (isEditing) "Fotografía actual" else "Fotografía de la bicicleta",
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.55f)
                ) {
                    BikeImageFrame(
                        imageUri = imageUri,
                        contentDescription = "Vista previa de la bicicleta"
                    )
                }

                if (isEditing) {
                    Text(
                        "La fotografía se conserva. La edición actualiza nombre, marca, " +
                                "modelo, tipo y número de serie.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTextSecondary
                    )
                } else {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSaving,
                        onClick = {
                            imagePicker.launch(
                                arrayOf("image/jpeg", "image/png", "image/webp")
                            )
                        }
                    ) {
                        Text(
                            if (imageUri.isBlank()) {
                                "Seleccionar fotografía"
                            } else {
                                "Cambiar fotografía"
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
                ) {
                    TextButton(
                        enabled = !isSaving,
                        onClick = onDismiss
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        modifier = Modifier.testTag(BIKE_SAVE_CHANGES_TEST_TAG),
                        onClick = {
                            val baseBike = initialBike ?: Bike(
                                name = name,
                                brand = brand,
                                model = model,
                                type = type,
                                serialNumber = serialNumber,
                                imageUri = imageUri
                            )
                            onSave(
                                baseBike.copy(
                                    name = name,
                                    brand = brand,
                                    model = model,
                                    type = type,
                                    serialNumber = serialNumber,
                                    imageUri = imageUri
                                )
                            )
                        },
                        enabled = !isSaving &&
                                name.isNotBlank() &&
                                brand.isNotBlank() &&
                                model.isNotBlank() &&
                                type.isNotBlank() &&
                                serialNumber.isNotBlank() &&
                                (isEditing || imageUri.isNotBlank())
                    ) {
                        Text(
                            if (isSaving) {
                                "Guardando..."
                            } else {
                                if (isEditing) "Guardar cambios" else "Guardar"
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BikeImageFrame(
    imageUri: String,
    contentDescription: String
) {
    val context = LocalContext.current

    val image by produceState<ProcessedBikeImage?>(
        initialValue = null,
        key1 = imageUri
    ) {
        if (imageUri.isBlank()) {
            value = null
        } else {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    decodeBikeImage(context, imageUri.toUri())
                }.getOrNull()
            }
        }
    }

    val loadedImage = image

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(
                when {
                    loadedImage == null -> MaterialTheme.colorScheme.surfaceVariant
                    loadedImage.hasTransparency -> Color.Transparent
                    else -> Color.White
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (loadedImage != null) {
            Image(
                bitmap = loadedImage.bitmap.asImageBitmap(),
                contentDescription = contentDescription,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (loadedImage.hasTransparency) 4.dp else 10.dp),
                contentScale = ContentScale.Fit,
                filterQuality = FilterQuality.High
            )
        } else {
            Text(
                text = if (imageUri.isBlank()) {
                    "Selecciona una imagen"
                } else {
                    "No fue posible cargar la imagen"
                },
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

private data class ProcessedBikeImage(
    val bitmap: Bitmap,
    val hasTransparency: Boolean
)

private fun decodeBikeImage(
    context: android.content.Context,
    uri: Uri
): ProcessedBikeImage? {
    if (uri.scheme == "appbike-photo") {
        val photoId = uri.host.orEmpty()
        if (photoId.isBlank()) return null
        val bytes = RemoteConnections.loadBikePhoto(photoId)
        return decodeBikeImageBytes(bytes)
    }

    val bounds = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }

    openBikeImageStream(context, uri)?.use {
        BitmapFactory.decodeStream(it, null, bounds)
    }

    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sampleSize = 1

    while (
        bounds.outWidth / sampleSize > 1600 ||
        bounds.outHeight / sampleSize > 1600
    ) {
        sampleSize *= 2
    }

    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }

    val decoded = openBikeImageStream(context, uri)?.use {
        BitmapFactory.decodeStream(it, null, options)
    } ?: return null

    val oriented = applyExifOrientation(context, uri, decoded)
    val hasTransparency = containsTransparentPixels(oriented)
    val displayBitmap = if (hasTransparency) {
        cropTransparentMargins(oriented)
    } else {
        oriented
    }

    return ProcessedBikeImage(
        bitmap = displayBitmap,
        hasTransparency = hasTransparency
    )
}

private fun decodeBikeImageBytes(bytes: ByteArray): ProcessedBikeImage? {
    val bounds = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sampleSize = 1
    while (
        bounds.outWidth / sampleSize > 1600 ||
        bounds.outHeight / sampleSize > 1600
    ) {
        sampleSize *= 2
    }

    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        ?: return null
    val hasTransparency = containsTransparentPixels(decoded)
    val displayBitmap = if (hasTransparency) {
        cropTransparentMargins(decoded)
    } else {
        decoded
    }

    return ProcessedBikeImage(
        bitmap = displayBitmap,
        hasTransparency = hasTransparency
    )
}

private fun applyExifOrientation(
    context: android.content.Context,
    uri: Uri,
    bitmap: Bitmap
): Bitmap {
    if (uri.scheme == "http" || uri.scheme == "https") return bitmap

    val orientation = runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        }
    }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

    val matrix = Matrix().apply {
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                setRotate(90f)
                postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                setRotate(-90f)
                postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(-90f)
        }
    }

    if (matrix.isIdentity) return bitmap

    return Bitmap.createBitmap(
        bitmap,
        0,
        0,
        bitmap.width,
        bitmap.height,
        matrix,
        true
    )
}

private fun openBikeImageStream(
    context: android.content.Context,
    uri: Uri
) = if (uri.scheme == "http" || uri.scheme == "https") {
    URL(uri.toString()).openStream()
} else {
    context.contentResolver.openInputStream(uri)
}

private fun containsTransparentPixels(bitmap: Bitmap): Boolean {
    if (!bitmap.hasAlpha()) return false

    val stepX = (bitmap.width / 80).coerceAtLeast(1)
    val stepY = (bitmap.height / 80).coerceAtLeast(1)

    for (y in 0 until bitmap.height step stepY) {
        for (x in 0 until bitmap.width step stepX) {
            if (android.graphics.Color.alpha(bitmap[x, y]) < 250) {
                return true
            }
        }
    }

    return false
}

private fun cropTransparentMargins(bitmap: Bitmap): Bitmap {
    var left = bitmap.width
    var top = bitmap.height
    var right = -1
    var bottom = -1

    val row = IntArray(bitmap.width)

    for (y in 0 until bitmap.height) {
        bitmap.getPixels(row, 0, bitmap.width, 0, y, bitmap.width, 1)

        for (x in row.indices) {
            if (android.graphics.Color.alpha(row[x]) > 12) {
                if (x < left) left = x
                if (x > right) right = x
                if (y < top) top = y
                if (y > bottom) bottom = y
            }
        }
    }

    if (right < left || bottom < top) return bitmap

    val horizontalPadding = ((right - left + 1) * 0.04f).toInt()
    val verticalPadding = ((bottom - top + 1) * 0.04f).toInt()
    val cropLeft = (left - horizontalPadding).coerceAtLeast(0)
    val cropTop = (top - verticalPadding).coerceAtLeast(0)
    val cropRight = (right + horizontalPadding).coerceAtMost(bitmap.width - 1)
    val cropBottom = (bottom + verticalPadding).coerceAtMost(bitmap.height - 1)

    if (
        cropLeft == 0 &&
        cropTop == 0 &&
        cropRight == bitmap.width - 1 &&
        cropBottom == bitmap.height - 1
    ) {
        return bitmap
    }

    return Bitmap.createBitmap(
        bitmap,
        cropLeft,
        cropTop,
        cropRight - cropLeft + 1,
        cropBottom - cropTop + 1
    )
}

@Composable
internal fun BikeDetailDialog(
    account: AccountSession,
    bike: Bike,
    reminders: MutableList<MaintenanceReminder>,
    bookings: MutableList<ServiceBooking>,
    isMutatingBike: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var component by remember { mutableStateOf("") }
    var maintenanceDate by remember { mutableStateOf("") }
    var maintenanceNotes by remember { mutableStateOf("") }
    var workshop by remember { mutableStateOf("") }
    var service by remember { mutableStateOf("") }
    var serviceDate by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var isSavingReminder by remember { mutableStateOf(false) }
    var isSavingBooking by remember { mutableStateOf(false) }
    var isLoadingMaintenance by remember { mutableStateOf(false) }
    var maintenanceLoaded by remember(bike.remoteId) { mutableStateOf(false) }
    var dialogError by remember(bike.remoteId) { mutableStateOf<String?>(null) }
    var reminderToEdit by remember(bike.remoteId) {
        mutableStateOf<MaintenanceReminder?>(null)
    }
    var reminderPendingDeletion by remember(bike.remoteId) {
        mutableStateOf<MaintenanceReminder?>(null)
    }
    var bookingToEdit by remember(bike.remoteId) {
        mutableStateOf<ServiceBooking?>(null)
    }
    var bookingPendingDeletion by remember(bike.remoteId) {
        mutableStateOf<ServiceBooking?>(null)
    }
    var bookingPendingCompletion by remember(bike.remoteId) {
        mutableStateOf<ServiceBooking?>(null)
    }
    var isMutatingMaintenance by remember(bike.remoteId) { mutableStateOf(false) }
    var maintenanceActionError by remember(bike.remoteId) { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val bikeReminders = reminders.filter {
        it.bikeId == bike.remoteId || (it.bikeId == null && it.bike == bike.name)
    }
    val bikeBookings = bookings.filter {
        it.bikeId == bike.remoteId || (it.bikeId == null && it.bike == bike.name)
    }
    val maintenanceDateValid = isValidApiDateInput(maintenanceDate)
    val serviceDateValid = isValidApiDateInput(serviceDate)

    fun <T> runMaintenanceMutation(
        remoteCall: () -> T,
        onSuccess: (T) -> Unit
    ) {
        if (isMutatingMaintenance) return
        isMutatingMaintenance = true
        maintenanceActionError = null
        scope.launch {
            runSuspendCatching {
                withContext(Dispatchers.IO) { remoteCall() }
            }.onSuccess(onSuccess)
                .onFailure { error ->
                    maintenanceActionError = RemoteConnections.userFriendlyError(error)
                }
            isMutatingMaintenance = false
        }
    }

    suspend fun refreshMaintenance() {
        if (bike.remoteId == null || isLoadingMaintenance) return
        isLoadingMaintenance = true
        dialogError = null
        runSuspendCatching {
            withContext(Dispatchers.IO) {
                RemoteConnections.loadMaintenance(account, bike)
            }
        }.onSuccess { data ->
            reminders.removeAll { it.bikeId == bike.remoteId }
            bookings.removeAll { it.bikeId == bike.remoteId }
            reminders.addAll(data.past)
            bookings.addAll(data.future)
            maintenanceLoaded = true
        }.onFailure { error ->
            dialogError = RemoteConnections.userFriendlyError(error)
        }
        isLoadingMaintenance = false
    }

    LaunchedEffect(scrollState, bike.remoteId) {
        snapshotFlow { scrollState.value to scrollState.maxValue }
            .filter { (value, maximum) ->
                maximum > 0 && value >= maximum * 0.12f
            }
            .first()
        refreshMaintenance()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(scrollState)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    bike.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black
                )

                TextButton(
                    enabled = !isMutatingBike && !isMutatingMaintenance,
                    onClick = onDismiss
                ) {
                    Text("Cerrar")
                }
            }

            dialogError?.let { message ->
                ErrorBanner(message)
            }

            BikeSummaryCard(bike = bike)

            CardItem(
                title = "Información de la bicicleta",
                subtitle = "${bike.brand} · ${bike.model} · ${bike.type}",
                body = "Serie: ${bike.serialNumber}\n" +
                        "Estado: ${if (bike.isStolen) "Reportada como robada" else "Registrada"}"
            )

            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(BIKE_EDIT_ACTION_TEST_TAG),
                enabled = !isMutatingBike && !isMutatingMaintenance,
                onClick = onEdit
            ) {
                Text("Editar datos de la bicicleta")
            }

            TextButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(BIKE_DELETE_ACTION_TEST_TAG),
                enabled = !isMutatingBike && !isMutatingMaintenance,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                onClick = onDelete
            ) {
                Text("Eliminar bicicleta")
            }

            Text(
                "Mantenciones pasadas",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            if (isLoadingMaintenance) {
                CircularProgressIndicator()
                Text("Actualizando solo las mantenciones...")
            } else if (!maintenanceLoaded) {
                MaintenanceSkeleton()
            } else if (bikeReminders.isEmpty()) {
                Text("No hay mantenciones pasadas registradas.")
            } else {
                bikeReminders.forEach { reminder ->
                    PastMaintenanceCard(
                        reminder = reminder,
                        enabled = !isMutatingMaintenance && reminder.remoteId != null,
                        onEdit = {
                            maintenanceActionError = null
                            reminderToEdit = reminder
                        },
                        onDelete = {
                            maintenanceActionError = null
                            reminderPendingDeletion = reminder
                        }
                    )
                }
            }

            Text(
                "Agregar mantención o alerta",
                fontWeight = FontWeight.Bold
            )

            AppInput("Componente o trabajo realizado", component) {
                component = it
            }

            AppInput(
                label = "Fecha (AAAA-MM-DD)",
                value = maintenanceDate,
                isError = maintenanceDate.isNotBlank() && !maintenanceDateValid,
                supportingText = if (maintenanceDate.isNotBlank() && !maintenanceDateValid) {
                    "Ingresa una fecha real con formato AAAA-MM-DD."
                } else {
                    null
                }
            ) {
                maintenanceDate = it
            }

            AppInput("Notas", maintenanceNotes) {
                maintenanceNotes = it
            }

            Button(
                onClick = {
                    val reminder = MaintenanceReminder(
                        bike = bike.name,
                        component = component,
                        date = maintenanceDate,
                        notes = maintenanceNotes
                    )

                    scope.launch {
                        isSavingReminder = true
                        dialogError = null

                        val result = runSuspendCatching {
                            withContext(Dispatchers.IO) {
                                RemoteConnections.createMaintenanceReminder(
                                    account,
                                    bike,
                                    reminder
                                )
                            }
                        }

                        result
                            .onSuccess { savedReminder ->
                                reminders.removeAll {
                                    savedReminder.remoteId != null &&
                                        it.remoteId == savedReminder.remoteId
                                }
                                reminders.add(savedReminder)
                                component = ""
                                maintenanceDate = ""
                                maintenanceNotes = ""
                            }
                            .onFailure { error ->
                                dialogError = RemoteConnections.userFriendlyError(error)
                            }

                        isSavingReminder = false
                    }
                },
                enabled = component.isNotBlank() &&
                        maintenanceDateValid &&
                        !isSavingReminder &&
                        !isMutatingMaintenance
            ) {
                Text(
                    if (isSavingReminder) {
                        "Guardando..."
                    } else {
                        "Guardar mantención"
                    }
                )
            }

            HorizontalDivider()

            Text(
                "Agendar servicio",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            AppInput("Taller", workshop) {
                workshop = it
            }

            AppInput("Servicio requerido", service) {
                service = it
            }

            AppInput(
                label = "Fecha (AAAA-MM-DD)",
                value = serviceDate,
                isError = serviceDate.isNotBlank() && !serviceDateValid,
                supportingText = if (serviceDate.isNotBlank() && !serviceDateValid) {
                    "Ingresa una fecha real con formato AAAA-MM-DD."
                } else {
                    null
                }
            ) {
                serviceDate = it
            }

            AppInput("Teléfono o correo", contact) {
                contact = it
            }

            Button(
                onClick = {
                    val booking = ServiceBooking(
                        workshop = workshop,
                        service = service,
                        date = serviceDate,
                        contact = contact,
                        bike = bike.name
                    )

                    scope.launch {
                        isSavingBooking = true
                        dialogError = null

                        val result = runSuspendCatching {
                            withContext(Dispatchers.IO) {
                                RemoteConnections.bookService(account, bike, booking)
                            }
                        }

                        result
                            .onSuccess { savedBooking ->
                                bookings.removeAll {
                                    savedBooking.remoteId != null &&
                                        it.remoteId == savedBooking.remoteId
                                }
                                bookings.add(savedBooking)
                                workshop = ""
                                service = ""
                                serviceDate = ""
                                contact = ""
                            }
                            .onFailure { error ->
                                dialogError = RemoteConnections.userFriendlyError(error)
                            }

                        isSavingBooking = false
                    }
                },
                enabled = workshop.isNotBlank() &&
                        service.isNotBlank() &&
                        serviceDateValid &&
                        !isSavingBooking &&
                        !isMutatingMaintenance
            ) {
                Text(
                    if (isSavingBooking) {
                        "Agendando..."
                    } else {
                        "Agendar"
                    }
                )
            }

            Text(
                "Servicios agendados",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            if (maintenanceLoaded && bikeBookings.isEmpty()) {
                Text("No hay servicios próximos agendados.")
            }

            bikeBookings.forEach { booking ->
                FutureServiceCard(
                    booking = booking,
                    enabled = !isMutatingMaintenance && booking.remoteId != null,
                    onEdit = {
                        maintenanceActionError = null
                        bookingToEdit = booking
                    },
                    onComplete = {
                        maintenanceActionError = null
                        bookingPendingCompletion = booking
                    },
                    onDelete = {
                        maintenanceActionError = null
                        bookingPendingDeletion = booking
                    }
                )
            }

            TextButton(
                enabled = !isLoadingMaintenance && !isMutatingMaintenance,
                onClick = {
                    scope.launch {
                        refreshMaintenance()
                    }
                }
            ) {
                Text("Actualizar mantenciones")
            }
        }
    }

    reminderToEdit?.let { target ->
        PastMaintenanceEditorDialog(
            reminder = target,
            isSaving = isMutatingMaintenance,
            errorMessage = maintenanceActionError,
            onDismiss = {
                if (!isMutatingMaintenance) {
                    reminderToEdit = null
                    maintenanceActionError = null
                }
            },
            onSave = { candidate ->
                runMaintenanceMutation(
                    remoteCall = {
                        RemoteConnections.updateMaintenanceReminder(account, bike, candidate)
                    },
                    onSuccess = { saved ->
                        reminders.removeAll { it.remoteId == target.remoteId }
                        reminders.add(saved)
                        reminderToEdit = null
                        maintenanceActionError = null
                    }
                )
            }
        )
    }

    reminderPendingDeletion?.let { target ->
        MaintenanceMutationConfirmationDialog(
            title = "Eliminar mantención",
            message = "Se eliminará definitivamente ${target.component} del historial.",
            confirmLabel = "Eliminar",
            confirmTag = MAINTENANCE_DELETE_TEST_TAG,
            isLoading = isMutatingMaintenance,
            errorMessage = maintenanceActionError,
            onDismiss = {
                if (!isMutatingMaintenance) {
                    reminderPendingDeletion = null
                    maintenanceActionError = null
                }
            },
            onConfirm = {
                runMaintenanceMutation(
                    remoteCall = {
                        RemoteConnections.deleteMaintenanceReminder(account, bike, target)
                    },
                    onSuccess = {
                        reminders.removeAll { it.remoteId == target.remoteId }
                        reminderPendingDeletion = null
                        maintenanceActionError = null
                    }
                )
            }
        )
    }

    bookingToEdit?.let { target ->
        FutureServiceEditorDialog(
            booking = target,
            isSaving = isMutatingMaintenance,
            errorMessage = maintenanceActionError,
            onDismiss = {
                if (!isMutatingMaintenance) {
                    bookingToEdit = null
                    maintenanceActionError = null
                }
            },
            onSave = { candidate ->
                runMaintenanceMutation(
                    remoteCall = {
                        RemoteConnections.updateServiceBooking(account, bike, candidate)
                    },
                    onSuccess = { saved ->
                        bookings.removeAll { it.remoteId == target.remoteId }
                        bookings.add(saved)
                        bookingToEdit = null
                        maintenanceActionError = null
                    }
                )
            }
        )
    }

    bookingPendingCompletion?.let { target ->
        ServiceCompletionDialog(
            booking = target,
            isSaving = isMutatingMaintenance,
            errorMessage = maintenanceActionError,
            onDismiss = {
                if (!isMutatingMaintenance) {
                    bookingPendingCompletion = null
                    maintenanceActionError = null
                }
            },
            onConfirm = { completedDate, notes ->
                runMaintenanceMutation(
                    remoteCall = {
                        RemoteConnections.completeServiceBooking(
                            account,
                            bike,
                            target,
                            completedDate,
                            notes
                        )
                    },
                    onSuccess = { completed ->
                        bookings.removeAll { it.remoteId == target.remoteId }
                        completed.remoteId?.let { completedId ->
                            reminders.removeAll { it.remoteId == completedId }
                        }
                        reminders.add(completed)
                        bookingPendingCompletion = null
                        maintenanceActionError = null
                    }
                )
            }
        )
    }

    bookingPendingDeletion?.let { target ->
        MaintenanceMutationConfirmationDialog(
            title = "Cancelar servicio",
            message = "Se eliminará el servicio ${target.service} agendado para ${target.date}.",
            confirmLabel = "Cancelar servicio",
            confirmTag = SERVICE_DELETE_TEST_TAG,
            isLoading = isMutatingMaintenance,
            errorMessage = maintenanceActionError,
            onDismiss = {
                if (!isMutatingMaintenance) {
                    bookingPendingDeletion = null
                    maintenanceActionError = null
                }
            },
            onConfirm = {
                runMaintenanceMutation(
                    remoteCall = {
                        RemoteConnections.deleteServiceBooking(account, bike, target)
                    },
                    onSuccess = {
                        bookings.removeAll { it.remoteId == target.remoteId }
                        bookingPendingDeletion = null
                        maintenanceActionError = null
                    }
                )
            }
        )
    }
}

@Composable
internal fun PastMaintenanceCard(
    reminder: MaintenanceReminder,
    enabled: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            reminder.component,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(reminder.date, color = AppPrimaryBright)
        Text(
            reminder.notes.ifBlank { "Sin notas" },
            color = AppTextSecondary
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                modifier = Modifier
                    .weight(1f)
                    .testTag(MAINTENANCE_EDIT_TEST_TAG),
                enabled = enabled,
                onClick = onEdit
            ) {
                Text("Editar")
            }
            TextButton(
                modifier = Modifier
                    .weight(1f)
                    .testTag(MAINTENANCE_DELETE_TEST_TAG),
                enabled = enabled,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                onClick = onDelete
            ) {
                Text("Eliminar")
            }
        }
    }
}

@Composable
internal fun FutureServiceCard(
    booking: ServiceBooking,
    enabled: Boolean,
    onEdit: () -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit
) {
    AppCard(modifier = Modifier.fillMaxWidth(), highlighted = true) {
        Text(
            booking.service,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text("${booking.workshop} · ${booking.date}", color = AppPrimaryBright)
        if (booking.contact.isNotBlank()) {
            Text("Contacto: ${booking.contact}", color = AppTextSecondary)
        }
        Spacer(Modifier.height(10.dp))
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(SERVICE_COMPLETE_TEST_TAG),
            enabled = enabled,
            onClick = onComplete
        ) {
            Text("Marcar como realizado")
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                modifier = Modifier
                    .weight(1f)
                    .testTag(SERVICE_EDIT_TEST_TAG),
                enabled = enabled,
                onClick = onEdit
            ) {
                Text("Editar")
            }
            TextButton(
                modifier = Modifier
                    .weight(1f)
                    .testTag(SERVICE_DELETE_TEST_TAG),
                enabled = enabled,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                onClick = onDelete
            ) {
                Text("Cancelar")
            }
        }
    }
}

@Composable
internal fun PastMaintenanceEditorDialog(
    reminder: MaintenanceReminder,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (MaintenanceReminder) -> Unit
) {
    var component by remember(reminder.remoteId) { mutableStateOf(reminder.component) }
    var date by remember(reminder.remoteId) { mutableStateOf(reminder.date) }
    var notes by remember(reminder.remoteId) { mutableStateOf(reminder.notes) }
    val dateValid = isValidApiDateInput(date)

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("Editar mantención") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                errorMessage?.let { ErrorBanner(it) }
                AppInput("Trabajo realizado", component) { component = it }
                AppInput(
                    label = "Fecha (AAAA-MM-DD)",
                    value = date,
                    isError = date.isNotBlank() && !dateValid,
                    supportingText = if (date.isNotBlank() && !dateValid) {
                        "Ingresa una fecha real."
                    } else {
                        null
                    }
                ) { date = it }
                AppInput("Notas", notes) { notes = it }
            }
        },
        confirmButton = {
            Button(
                modifier = Modifier.testTag(MAINTENANCE_SAVE_TEST_TAG),
                enabled = component.isNotBlank() && dateValid && !isSaving,
                onClick = {
                    onSave(
                        reminder.copy(
                            component = component,
                            date = date,
                            notes = notes
                        )
                    )
                }
            ) {
                Text(if (isSaving) "Guardando..." else "Guardar cambios")
            }
        },
        dismissButton = {
            TextButton(enabled = !isSaving, onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
internal fun FutureServiceEditorDialog(
    booking: ServiceBooking,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (ServiceBooking) -> Unit
) {
    var workshop by remember(booking.remoteId) { mutableStateOf(booking.workshop) }
    var service by remember(booking.remoteId) { mutableStateOf(booking.service) }
    var date by remember(booking.remoteId) { mutableStateOf(booking.date) }
    var contact by remember(booking.remoteId) { mutableStateOf(booking.contact) }
    val dateValid = isValidApiDateInput(date)

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("Editar servicio") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                errorMessage?.let { ErrorBanner(it) }
                AppInput("Taller", workshop) { workshop = it }
                AppInput("Servicio requerido", service) { service = it }
                AppInput(
                    label = "Fecha (AAAA-MM-DD)",
                    value = date,
                    isError = date.isNotBlank() && !dateValid,
                    supportingText = if (date.isNotBlank() && !dateValid) {
                        "Ingresa una fecha real."
                    } else {
                        null
                    }
                ) { date = it }
                AppInput("Teléfono o correo", contact) { contact = it }
            }
        },
        confirmButton = {
            Button(
                modifier = Modifier.testTag(SERVICE_SAVE_TEST_TAG),
                enabled = workshop.isNotBlank() && service.isNotBlank() &&
                    dateValid && !isSaving,
                onClick = {
                    onSave(
                        booking.copy(
                            workshop = workshop,
                            service = service,
                            date = date,
                            contact = contact
                        )
                    )
                }
            ) {
                Text(if (isSaving) "Guardando..." else "Guardar cambios")
            }
        },
        dismissButton = {
            TextButton(enabled = !isSaving, onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
internal fun ServiceCompletionDialog(
    booking: ServiceBooking,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (completedDate: String, notes: String) -> Unit
) {
    var completedDate by remember(booking.remoteId) {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date()))
    }
    var notes by remember(booking.remoteId) { mutableStateOf("") }
    val dateValid = isValidApiDateInput(completedDate)

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("Completar servicio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${booking.service} · ${booking.workshop}")
                errorMessage?.let { ErrorBanner(it) }
                AppInput(
                    label = "Fecha realizada (AAAA-MM-DD)",
                    value = completedDate,
                    isError = completedDate.isNotBlank() && !dateValid,
                    supportingText = if (completedDate.isNotBlank() && !dateValid) {
                        "Ingresa una fecha real."
                    } else {
                        null
                    }
                ) { completedDate = it }
                AppInput("Notas del trabajo realizado", notes) { notes = it }
            }
        },
        confirmButton = {
            Button(
                modifier = Modifier.testTag(SERVICE_CONFIRM_COMPLETE_TEST_TAG),
                enabled = dateValid && !isSaving,
                onClick = { onConfirm(completedDate, notes) }
            ) {
                Text(if (isSaving) "Completando..." else "Confirmar")
            }
        },
        dismissButton = {
            TextButton(enabled = !isSaving, onClick = onDismiss) { Text("Volver") }
        }
    )
}

@Composable
internal fun MaintenanceMutationConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    confirmTag: String,
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(message)
                Text("Esta acción no se puede deshacer.", color = AppTextSecondary)
                errorMessage?.let { ErrorBanner(it) }
            }
        },
        confirmButton = {
            TextButton(
                modifier = Modifier.testTag(confirmTag),
                enabled = !isLoading,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                onClick = onConfirm
            ) {
                Text(if (isLoading) "Procesando..." else confirmLabel)
            }
        },
        dismissButton = {
            OutlinedButton(enabled = !isLoading, onClick = onDismiss) { Text("Volver") }
        }
    )
}

@Composable
private fun MaintenanceSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(2) {
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(0.55f)
                            .height(18.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )

                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )

                    Text("Cargando información de mantenciones...")
                }
            }
        }
    }
}
