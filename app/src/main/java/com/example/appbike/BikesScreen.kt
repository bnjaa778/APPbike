package com.example.appbike

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

private const val BIKE_PHOTO_LOG_TAG = "APPbikePhotos"

@Composable
fun BikesScreen(
    account: AccountSession?,
    bikes: MutableList<Bike>,
    reminders: MutableList<MaintenanceReminder>,
    bookings: MutableList<ServiceBooking>,
    onOpenAccount: () -> Unit,
    onBack: (() -> Unit)?
) {
    var showForm by remember { mutableStateOf(false) }
    var selectedBike by remember { mutableStateOf<Bike?>(null) }
    var isSavingBike by remember { mutableStateOf(false) }
    var isLoadingBikes by remember { mutableStateOf(false) }
    var loadingBikeId by remember { mutableStateOf<Long?>(null) }
    var connectionWarning by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(account?.userId) {
        bikes.clear()
        reminders.clear()
        bookings.clear()
        if (account == null) return@LaunchedEffect

        isLoadingBikes = true
        runCatching {
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

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 18.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
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
                textAlign = TextAlign.Center
            )

            if (account == null) {
                Text(
                    "Inicia sesión para ver tus bicicletas guardadas",
                    textAlign = TextAlign.Center
                )
                Button(onClick = onOpenAccount) {
                    Text("Iniciar sesión")
                }
            } else if (isLoadingBikes && bikes.isEmpty()) {
                CircularProgressIndicator()
                Text("Cargando bicicletas...")
            } else if (bikes.isEmpty()) {
                Spacer(modifier = Modifier.height(72.dp))
                AddBikeButton { showForm = true }
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
                                runCatching {
                                    withContext(Dispatchers.IO) {
                                        RemoteConnections.loadBikeDetails(
                                            activeAccount,
                                            bike
                                        )
                                    }
                                }.onSuccess { loadedBike ->
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

                AddBikeButton { showForm = true }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showForm) {
        BikeFormDialog(
            isSaving = isSavingBike,
            onDismiss = {
                if (!isSavingBike) {
                    showForm = false
                }
            },
            onSave = saveBike@ { bike ->
                val activeAccount = account ?: return@saveBike
                if (!isSavingBike) {
                    scope.launch {
                        isSavingBike = true

                        val result = runCatching {
                            withContext(Dispatchers.IO) {
                                RemoteConnections.registerBike(context, activeAccount, bike)
                            }
                        }

                        result
                            .onSuccess { savedBike ->
                                bikes.add(0, savedBike)
                                showForm = false
                            }
                            .onFailure { error ->
                                connectionWarning = RemoteConnections.userFriendlyError(error)
                            }

                        isSavingBike = false
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
            onDismiss = {
                selectedBike = null
            },
            onError = { message ->
                connectionWarning = message
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

    connectionWarning?.let { message ->
        AlertDialog(
            onDismissRequest = {
                connectionWarning = null
            },
            title = {
                Text("No se pudo sincronizar")
            },
            text = {
                Text(message)
            },
            confirmButton = {
                Button(
                    onClick = {
                        connectionWarning = null
                    }
                ) {
                    Text("Entendido")
                }
            }
        )
    }
}

@Composable
private fun AddBikeButton(onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            modifier = Modifier
                .size(82.dp)
                .clickable(onClick = onClick)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
            shape = CircleShape,
            color = Color.Transparent
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    "+",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Light
                )
            }
        }

        Text(
            "Agregar bicicleta",
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun BikeSummaryCard(
    bike: Bike,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
private fun BikeFormDialog(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (Bike) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    var serialNumber by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf("") }

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
                    "Agregar bicicleta",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

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
                    "Fotografía de la bicicleta",
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
                        onClick = {
                            onSave(
                                Bike(
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
                                imageUri.isNotBlank()
                    ) {
                        Text(
                            if (isSaving) {
                                "Guardando..."
                            } else {
                                "Guardar"
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
                    decodeBikeImage(context, Uri.parse(imageUri))
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
        return decodeBikeImageBytes(bytes, "api:$photoId")
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

    logBikeImageDiagnostics(
        source = uri.scheme.orEmpty().ifBlank { "local" },
        sourceBytes = null,
        sourceWidth = bounds.outWidth,
        sourceHeight = bounds.outHeight,
        sampleSize = sampleSize,
        decoded = oriented,
        displayed = displayBitmap,
        hasTransparency = hasTransparency
    )

    return ProcessedBikeImage(
        bitmap = displayBitmap,
        hasTransparency = hasTransparency
    )
}

private fun decodeBikeImageBytes(
    bytes: ByteArray,
    source: String = "bytes"
): ProcessedBikeImage? {
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

    logBikeImageDiagnostics(
        source = source,
        sourceBytes = bytes.size,
        sourceWidth = bounds.outWidth,
        sourceHeight = bounds.outHeight,
        sampleSize = sampleSize,
        decoded = decoded,
        displayed = displayBitmap,
        hasTransparency = hasTransparency
    )

    return ProcessedBikeImage(
        bitmap = displayBitmap,
        hasTransparency = hasTransparency
    )
}

private fun logBikeImageDiagnostics(
    source: String,
    sourceBytes: Int?,
    sourceWidth: Int,
    sourceHeight: Int,
    sampleSize: Int,
    decoded: Bitmap,
    displayed: Bitmap,
    hasTransparency: Boolean
) {
    Log.d(
        BIKE_PHOTO_LOG_TAG,
        buildString {
            append("source=").append(source)
            if (sourceBytes != null) append(" bytes=").append(sourceBytes)
            append(" source=").append(sourceWidth).append('x').append(sourceHeight)
            append(" sample=").append(sampleSize)
            append(" decoded=").append(decoded.width).append('x').append(decoded.height)
            append(" displayed=").append(displayed.width).append('x').append(displayed.height)
            append(" transparency=").append(hasTransparency)
        }
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
            if (android.graphics.Color.alpha(bitmap.getPixel(x, y)) < 250) {
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
private fun BikeDetailDialog(
    account: AccountSession,
    bike: Bike,
    reminders: MutableList<MaintenanceReminder>,
    bookings: MutableList<ServiceBooking>,
    onDismiss: () -> Unit,
    onError: (String) -> Unit
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

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val bikeReminders = reminders.filter {
        it.bikeId == bike.remoteId || (it.bikeId == null && it.bike == bike.name)
    }
    val bikeBookings = bookings.filter {
        it.bikeId == bike.remoteId || (it.bikeId == null && it.bike == bike.name)
    }

    suspend fun refreshMaintenance() {
        if (bike.remoteId == null || isLoadingMaintenance) return
        isLoadingMaintenance = true
        runCatching {
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
            onError(RemoteConnections.userFriendlyError(error))
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

                TextButton(onClick = onDismiss) {
                    Text("Cerrar")
                }
            }

            BikeSummaryCard(
                bike = bike,
                onClick = {}
            )

            CardItem(
                title = "Información de la bicicleta",
                subtitle = "${bike.brand} · ${bike.model} · ${bike.type}",
                body = "Serie: ${bike.serialNumber}\n" +
                        "Estado: ${if (bike.isStolen) "Reportada como robada" else "Registrada"}"
            )

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
                bikeReminders.forEach {
                    CardItem(
                        title = it.component,
                        subtitle = it.date,
                        body = it.notes.ifBlank { "Sin notas" }
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

            AppInput("Fecha (AAAA-MM-DD)", maintenanceDate) {
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

                        val result = runCatching {
                            withContext(Dispatchers.IO) {
                                RemoteConnections.createMaintenanceReminder(bike, reminder)
                            }
                        }

                        result
                            .onSuccess { savedReminder ->
                                reminders.add(savedReminder)
                                component = ""
                                maintenanceDate = ""
                                maintenanceNotes = ""
                            }
                            .onFailure { error ->
                                onError(
                                    RemoteConnections.userFriendlyError(error)
                                )
                            }

                        isSavingReminder = false
                    }
                },
                enabled = component.isNotBlank() &&
                        maintenanceDate.isNotBlank() &&
                        !isSavingReminder
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

            AppInput("Fecha (AAAA-MM-DD)", serviceDate) {
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

                        val result = runCatching {
                            withContext(Dispatchers.IO) {
                                RemoteConnections.bookService(bike, booking)
                            }
                        }

                        result
                            .onSuccess { savedBooking ->
                                bookings.add(savedBooking)
                                workshop = ""
                                service = ""
                                serviceDate = ""
                                contact = ""
                            }
                            .onFailure { error ->
                                onError(
                                    RemoteConnections.userFriendlyError(error)
                                )
                            }

                        isSavingBooking = false
                    }
                },
                enabled = workshop.isNotBlank() &&
                        service.isNotBlank() &&
                        serviceDate.isNotBlank() &&
                        !isSavingBooking
            ) {
                Text(
                    if (isSavingBooking) {
                        "Agendando..."
                    } else {
                        "Agendar"
                    }
                )
            }

            bikeBookings.forEach {
                CardItem(
                    title = it.service,
                    subtitle = it.workshop,
                    body = "Fecha: ${it.date}\nContacto: ${it.contact}"
                )
            }

            TextButton(
                enabled = !isLoadingMaintenance,
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

                    Text("La información aparecerá aquí al conectar la base de datos.")
                }
            }
        }
    }
}
