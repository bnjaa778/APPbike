package com.example.appbike

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

@Composable
fun MarketplaceScreen(account: AccountSession?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val posts = remember { mutableStateListOf<ProductPublication>() }
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    val location = LocalDataStore.loadLocation(context)

    fun refresh() {
        val center = location ?: run {
            error = "Define primero tu ubicación desde Mapas."
            return
        }
        scope.launch {
            loading = true
            error = null
            runCatching {
                withContext(Dispatchers.IO) {
                    RemoteConnections.loadMarketplacePosts(center, query, 40)
                }
            }.onSuccess {
                posts.clear()
                posts.addAll(it)
            }.onFailure { error = RemoteConnections.userFriendlyError(it) }
            loading = false
        }
    }

    LaunchedEffect(location) { if (location != null) refresh() }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar en Marketplace") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = ::refresh) {
                        Icon(Icons.Outlined.Search, contentDescription = "Buscar")
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { refresh() })
            )
            Text(
                "Publicaciones a 40 km",
                modifier = Modifier.padding(vertical = 10.dp),
                style = MaterialTheme.typography.titleLarge
            )

            when {
                loading && posts.isEmpty() -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                posts.isEmpty() -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        error ?: "No hay publicaciones disponibles en esta zona.",
                        color = if (error == null) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }

                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(posts, key = { it.id.ifBlank { "${it.title}:${it.createdAt}" } }) {
                        MarketplaceCard(it)
                    }
                }
            }
        }

        FloatingActionButton(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp),
            shape = CircleShape,
            onClick = {
                when {
                    account == null -> error = "Inicia sesión para publicar."
                    location == null -> error = "Define primero tu ubicación desde Mapas."
                    else -> showCreate = true
                }
            }
        ) {
            Icon(Icons.Outlined.Add, contentDescription = "Crear publicación")
        }
    }

    if (showCreate && account != null && location != null) {
        CreateMarketplaceDialog(
            onDismiss = { showCreate = false },
            onCreate = { post ->
                scope.launch {
                    loading = true
                    runCatching {
                        withContext(Dispatchers.IO) {
                            RemoteConnections.createMarketplacePost(
                                context,
                                post.copy(
                                    latitude = location.latitude,
                                    longitude = location.longitude,
                                    createdBy = account.userId,
                                    seller = account.email
                                )
                            )
                        }
                    }.onSuccess {
                        showCreate = false
                        refresh()
                    }.onFailure { error = RemoteConnections.userFriendlyError(it) }
                    loading = false
                }
            }
        )
    }
}

@Composable
private fun MarketplaceCard(post: ProductPublication) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column {
            MarketplaceRemoteImage(post.images.firstOrNull())
            Column(
                Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(post.title, fontWeight = FontWeight.Bold, maxLines = 2)
                Text(
                    listOf(post.brand, post.model)
                        .filter(String::isNotBlank)
                        .joinToString(" "),
                    style = MaterialTheme.typography.bodySmall
                )
                if (post.price.isNotBlank()) {
                    Text(
                        post.price,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    post.description,
                    maxLines = 2,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MarketplaceRemoteImage(url: String?) {
    var bytes by remember(url) { mutableStateOf<ByteArray?>(null) }
    LaunchedEffect(url) {
        if (!url.isNullOrBlank()) {
            bytes = runCatching {
                withContext(Dispatchers.IO) {
                    if (url.startsWith("appbike-market-photo://")) {
                        RemoteConnections.loadMarketplacePhoto(
                            url.removePrefix("appbike-market-photo://")
                        )
                    } else {
                        URL(url).readBytes()
                    }
                }
            }.getOrNull()
        }
    }
    val bitmap = remember(bytes) {
        bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
    }
    ProductImage(bitmap)
}

@Composable
private fun CreateMarketplaceDialog(
    onDismiss: () -> Unit,
    onCreate: (ProductPublication) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var productType by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
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
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(max = 700.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Crear publicación", style = MaterialTheme.typography.headlineMedium)
                AppInput("Nombre del producto", title) { title = it }
                AppInput("Marca", brand) { brand = it }
                AppInput("Modelo", model) { model = it }
                AppInput("Tipo: bicicleta, repuesto, accesorio", productType) {
                    productType = it
                }
                AppInput("Precio", price) { price = it }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    shape = RoundedCornerShape(16.dp)
                )
                Text("Fotografía del producto", fontWeight = FontWeight.Bold)
                MarketplaceLocalImage(imageUri)
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        imagePicker.launch(arrayOf("image/jpeg", "image/png", "image/webp"))
                    }
                ) {
                    Text(
                        if (imageUri.isBlank()) "Seleccionar fotografía"
                        else "Cambiar fotografía"
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
                ) {
                    OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
                    Button(
                        enabled = title.isNotBlank() && brand.isNotBlank() &&
                            model.isNotBlank() && productType.isNotBlank() &&
                            price.isNotBlank() && description.isNotBlank() &&
                            imageUri.isNotBlank(),
                        onClick = {
                            onCreate(
                                ProductPublication(
                                    title = title,
                                    price = price,
                                    category = productType,
                                    condition = "",
                                    seller = "",
                                    description = description,
                                    mediaDescription = "",
                                    brand = brand,
                                    model = model,
                                    productType = productType,
                                    imageUri = imageUri
                                )
                            )
                        }
                    ) { Text("Publicar") }
                }
            }
        }
    }
}

@Composable
private fun MarketplaceLocalImage(imageUri: String) {
    val context = LocalContext.current
    var bitmap by remember(imageUri) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(imageUri) {
        bitmap = if (imageUri.isBlank()) null else {
            withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(Uri.parse(imageUri))?.use {
                        BitmapFactory.decodeStream(it)
                    }
                }.getOrNull()
            }
        }
    }
    ProductImage(bitmap)
}

@Composable
private fun ProductImage(bitmap: Bitmap?) {
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Fotografía del producto",
            modifier = Modifier.fillMaxWidth().aspectRatio(1.55f),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.55f)
                .background(
                    MaterialTheme.colorScheme.surfaceContainer,
                    RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Image,
                contentDescription = "Sin fotografía",
                modifier = Modifier.size(44.dp)
            )
        }
    }
}
