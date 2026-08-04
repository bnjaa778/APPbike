package com.example.appbike

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.appbike.ui.theme.AppBackgroundElevated
import com.example.appbike.ui.theme.AppBorderActive
import com.example.appbike.ui.theme.AppBorderSubtle
import com.example.appbike.ui.theme.AppPrimary
import com.example.appbike.ui.theme.AppPrimaryBright
import com.example.appbike.ui.theme.AppPrimarySoft
import com.example.appbike.ui.theme.AppSurfaceElevated
import com.example.appbike.ui.theme.AppTextPrimary
import com.example.appbike.ui.theme.AppTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

private val MARKETPLACE_STATUSES = listOf(
    "activa" to "Activas",
    "pausada" to "Pausadas",
    "vendida" to "Vendidas",
    "en_revision" to "En revisiÃ³n"
)
private const val MARKETPLACE_LOADING_MIN_MS = 450L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen(account: AccountSession?, onOpenChat: (UserChat) -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val posts = remember { mutableStateListOf<ProductPublication>() }
    var location by remember {
        mutableStateOf(
            LocalDataStore.loadMarketplaceLocation(context)
                ?: LocalDataStore.loadLocation(context)
        )
    }
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var pullRefreshing by remember { mutableStateOf(false) }
    var refreshRequestId by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var showLocationPicker by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<ProductPublication?>(null) }
    var detailLoading by remember { mutableStateOf(false) }
    var detailError by remember { mutableStateOf<String?>(null) }

    fun refresh(fromPull: Boolean = false) {
        val center = location ?: run {
            error = "Elige una ubicaciÃ³n para Marketplace."
            return
        }
        val requestedQuery = query
        val requestId = refreshRequestId + 1
        refreshRequestId = requestId
        scope.launch {
            loading = !fromPull
            pullRefreshing = fromPull
            error = null
            val startedAt = System.nanoTime()
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    RemoteConnections.loadMarketplacePosts(
                        center = center,
                        query = requestedQuery,
                        radiusKm = 40,
                        status = "activa"
                    )
                }
            }
            val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000L
            if (elapsedMs < MARKETPLACE_LOADING_MIN_MS) {
                delay(MARKETPLACE_LOADING_MIN_MS - elapsedMs)
            }
            if (requestId != refreshRequestId) return@launch
            result.onSuccess {
                posts.clear()
                posts.addAll(it)
            }.onFailure { error = RemoteConnections.userFriendlyError(it) }
            loading = false
            pullRefreshing = false
        }
    }

    fun openDetail(publicationId: String) {
        scope.launch {
            detail = null
            detailLoading = true
            detailError = null
            runCatching {
                withContext(Dispatchers.IO) {
                    RemoteConnections.loadMarketplaceDetails(publicationId)
                }
            }.onSuccess { detail = it }
                .onFailure {
                    detailError = RemoteConnections.userFriendlyError(it)
                    error = detailError
                }
            detailLoading = false
        }
    }

    fun changeStatus(publicationId: String, status: String) {
        scope.launch {
            detailLoading = true
            detailError = null
            runCatching {
                withContext(Dispatchers.IO) {
                    RemoteConnections.updateMarketplaceStatus(
                        account?.userId ?: throw IllegalStateException("Inicia sesiÃ³n."),
                        publicationId,
                        status
                    )
                }
            }.onSuccess {
                detail = it
                refresh()
            }.onFailure { detailError = RemoteConnections.userFriendlyError(it) }
            detailLoading = false
        }
    }

    fun addPhoto(publicationId: String, imageUri: String) {
        scope.launch {
            detailLoading = true
            detailError = null
            runCatching {
                withContext(Dispatchers.IO) {
                    RemoteConnections.uploadMarketplacePhoto(
                        context,
                        account?.userId ?: throw IllegalStateException("Inicia sesiÃ³n."),
                        publicationId,
                        imageUri
                    )
                    RemoteConnections.loadMarketplaceDetails(publicationId)
                }
            }.onSuccess {
                detail = it
                refresh()
            }.onFailure { detailError = RemoteConnections.userFriendlyError(it) }
            detailLoading = false
        }
    }

    fun contactSeller(publication: ProductPublication) {
        val session = account ?: run {
            detailError = "Inicia sesiÃ³n para contactar al vendedor."
            return
        }
        scope.launch {
            detailLoading = true
            detailError = null
            runCatching {
                withContext(Dispatchers.IO) {
                    RemoteConnections.getOrCreateChat(
                        userId = session.userId,
                        relatedUserId = publication.createdBy,
                        type = ChatType.MARKETPLACE,
                        relatedEntityId = publication.id,
                        title = publication.title
                    )
                }
            }.onSuccess(onOpenChat)
                .onFailure { detailError = RemoteConnections.userFriendlyError(it) }
            detailLoading = false
        }
    }

    LaunchedEffect(Unit) {
        if (LocalDataStore.loadMarketplaceLocation(context) == null) {
            location?.let { LocalDataStore.saveMarketplaceLocation(context, it) }
        }
    }

    LaunchedEffect(location) {
        if (location != null) refresh()
    }

    val currency = marketplaceCurrencyFor(location)

    PremiumScreenBackground(PremiumGlowStyle.Marketplace) {
        Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = AppDimens.Space4)
                .padding(top = AppDimens.Space3, bottom = AppDimens.Space5),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Space3)
        ) {
            SearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Buscar en Marketplace",
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { refresh() }, enabled = !loading) {
                        Icon(Icons.Outlined.Search, contentDescription = "Buscar")
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { refresh() })
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showLocationPicker = true },
                shape = RoundedCornerShape(AppDimens.RadiusLarge),
                color = AppSurfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, AppBorderSubtle)
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = AppDimens.Space4,
                        vertical = AppDimens.Space3
                    ),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.Space3),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = AppPrimaryBright
                    )
                    Text(
                        text = location?.let { "Marketplace en ${marketplaceLocationLabel(it)}" }
                            ?: "Elegir ubicación de Marketplace",
                        modifier = Modifier.weight(1f),
                        color = AppTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        shape = RoundedCornerShape(AppDimens.RadiusPill),
                        color = AppPrimarySoft
                    ) {
                        Text(
                            currency.code,
                            modifier = Modifier.padding(
                                horizontal = AppDimens.Space3,
                                vertical = AppDimens.Space1
                            ),
                            color = AppPrimaryBright,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            error?.let { ErrorBanner(it) }

            PullToRefreshBox(
                isRefreshing = pullRefreshing,
                onRefresh = { refresh(fromPull = true) },
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                when {
                    posts.isEmpty() && !loading -> Box(
                        Modifier
                            .fillMaxSize()
                            .padding(top = AppDimens.Space4, bottom = AppDimens.Space10),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        MarketplaceEmptyState(
                            title = if (location == null) {
                                "Elige una ubicación"
                            } else {
                                "No hay publicaciones en esta ubicación"
                            },
                            description = if (location == null) {
                                "Define dónde buscar para ver productos cercanos."
                            } else {
                                "Prueba cambiando la ubicación o vuelve a intentarlo más tarde."
                            },
                            onChangeLocation = { showLocationPicker = true }
                        )
                    }

                    posts.isNotEmpty() -> LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = AppDimens.Space8),
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.Space3),
                        verticalArrangement = Arrangement.spacedBy(AppDimens.Space3)
                    ) {
                        items(posts, key = { it.id }) { post ->
                            MarketplaceCard(post) { openDetail(post.id) }
                        }
                    }
                }
            }
        }

        if (loading) {
            Surface(
                modifier = Modifier.align(Alignment.Center),
                shape = CircleShape,
                color = AppBackgroundElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, AppBorderSubtle),
                shadowElevation = 4.dp
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(16.dp).size(36.dp),
                    strokeWidth = 3.dp,
                    color = AppPrimaryBright
                )
            }
        }

        SmallFloatingActionButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = AppDimens.Space4, bottom = AppDimens.Space1),
            shape = CircleShape,
            containerColor = AppPrimary,
            contentColor = AppTextPrimary,
            onClick = {
                when {
                    account == null -> error = "Inicia sesión para publicar."
                    location == null -> error = "Elige una ubicación para Marketplace."
                    else -> showCreate = true
                }
            }
        ) {
            Icon(Icons.Outlined.Add, contentDescription = "Crear publicaciÃ³n")
        }

        if (detailLoading && detail == null) {
            Surface(
                modifier = Modifier.align(Alignment.Center),
                shape = CircleShape,
                shadowElevation = 8.dp
            ) {
                CircularProgressIndicator(Modifier.padding(16.dp))
            }
        }

        if (showLocationPicker) {
            LocationSearchDialog(
                initial = location?.label.orEmpty(),
                message = "Esta ubicaciÃ³n se usarÃ¡ solo en Marketplace y no cambiarÃ¡ la de Juntas.",
                onDismiss = { showLocationPicker = false },
                onLocation = { point ->
                    location = point
                    LocalDataStore.saveMarketplaceLocation(context, point)
                    showLocationPicker = false
                    error = null
                    scope.launch {
                        val resolved = withContext(Dispatchers.IO) {
                            RemoteConnections.resolveCommunityLocation(point)
                        }
                        if (resolved != point) {
                            location = resolved
                            LocalDataStore.saveMarketplaceLocation(context, resolved)
                        }
                    }
                }
            )
        }
        }
    }

    val publicationLocation = location
    if (showCreate && account != null && publicationLocation != null) {
        CreateMarketplaceDialog(
            currency = currency,
            onDismiss = { showCreate = false },
            onCreate = { post ->
                scope.launch {
                    loading = true
                    error = null
                    runCatching {
                        withContext(Dispatchers.IO) {
                            val region = RemoteConnections.communityRegionFor(publicationLocation)
                            RemoteConnections.createMarketplacePost(
                                context,
                                post.copy(
                                    latitude = publicationLocation.latitude,
                                    longitude = publicationLocation.longitude,
                                    createdBy = account.userId,
                                    seller = account.username ?: account.email,
                                    createdByUsername = account.username,
                                    region = region,
                                    location = publicationLocation.label,
                                    currencyCode = currency.code,
                                    countryCode = publicationLocation.countryCode,
                                    administrativeArea = publicationLocation.administrativeArea
                                )
                            )
                        }
                    }.onSuccess {
                        showCreate = false
                        refresh()
                        detail = it
                    }.onFailure { error = RemoteConnections.userFriendlyError(it) }
                    loading = false
                }
            }
        )
    }

    detail?.let { publication ->
        MarketplaceDetailScreen(
            publication = publication,
            currency = marketplaceCurrency(publication.currencyCode),
            account = account,
            loading = detailLoading,
            error = detailError,
            onDismiss = {
                detail = null
                detailError = null
            },
            onStatus = { changeStatus(publication.id, it) },
            onAddPhoto = { addPhoto(publication.id, it) },
            onContact = { contactSeller(publication) }
        )
    }

}

@Composable
private fun MarketplaceEmptyState(
    title: String,
    description: String,
    onChangeLocation: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppDimens.Space4),
        shape = RoundedCornerShape(AppDimens.RadiusLarge),
        color = AppSurfaceElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, AppBorderActive.copy(alpha = 0.32f))
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.Space4),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppDimens.Space2)
        ) {
            Icon(
                Icons.Outlined.Image,
                contentDescription = null,
                tint = AppPrimaryBright,
                modifier = Modifier.size(28.dp)
            )
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = AppTextPrimary,
                maxLines = 2
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = AppTextSecondary,
                maxLines = 2
            )
            TextButton(onClick = onChangeLocation) {
                Text("Cambiar ubicación", color = AppPrimaryBright)
            }
        }
    }
}

@Composable
private fun MarketplaceCard(
    post: ProductPublication,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(AppDimens.RadiusLarge),
        color = AppSurfaceElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, AppBorderSubtle),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column {
            MarketplaceRemoteImage(
                url = post.images.firstOrNull(),
                publicationId = post.id
            )
            Column(
                Modifier.padding(AppDimens.Space3),
                verticalArrangement = Arrangement.spacedBy(AppDimens.Space1)
            ) {
                Text(
                    post.title,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    color = AppTextPrimary
                )
                Text(
                    publicationSellerName(post),
                    style = MaterialTheme.typography.labelMedium,
                    color = AppTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (post.productStatus.isNotBlank()) {
                    Text(
                        post.productStatus.replaceFirstChar(Char::uppercase),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTextSecondary
                    )
                }
                if (post.price.isNotBlank()) {
                    Text(
                        formatMarketplacePrice(
                            post.price,
                            marketplaceCurrency(post.currencyCode)
                        ),
                        color = AppPrimaryBright,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    post.description,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTextSecondary
                )
            }
        }
    }
}

@Composable
private fun MarketplaceDetailScreen(
    publication: ProductPublication,
    currency: MarketplaceCurrency,
    account: AccountSession?,
    loading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onStatus: (String) -> Unit,
    onAddPhoto: (String) -> Unit,
    onContact: () -> Unit
) {
    val context = LocalContext.current
    val isOwner = account?.userId == publication.createdBy
    var selectedPhoto by remember(publication.id, publication.images) { mutableStateOf(0) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            onAddPhoto(uri.toString())
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Volver a Marketplace"
                        )
                    }
                    Text(
                        "Marketplace",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
                HorizontalDivider()

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    MarketplaceRemoteImage(
                        url = publication.images.getOrNull(selectedPhoto),
                        publicationId = publication.id,
                        modifier = Modifier.fillMaxWidth().height(340.dp)
                    )

                    if (publication.images.size > 1) {
                        LazyRow(
                            modifier = Modifier.padding(top = 12.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 18.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(publication.images.size) { index ->
                                MarketplaceRemoteImage(
                                    url = publication.images[index],
                                    publicationId = publication.id,
                                    modifier = Modifier
                                        .size(86.dp, 64.dp)
                                        .border(
                                            width = if (selectedPhoto == index) 3.dp else 1.dp,
                                            color = if (selectedPhoto == index) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.outlineVariant
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { selectedPhoto = index }
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            formatMarketplacePrice(publication.price, currency),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            publication.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Vende ${publicationSellerName(publication)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    publication.productStatus
                                        .ifBlank { publication.condition }
                                        .replaceFirstChar(Char::uppercase),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest
                            ) {
                                Text(
                                    marketplaceStatusLabel(publication.publicationStatus),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        HorizontalDivider()
                        Text(
                            "DescripciÃ³n",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            publication.description,
                            style = MaterialTheme.typography.bodyLarge
                        )

                        if (publication.location.isNotBlank()) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    Icons.Outlined.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text("UbicaciÃ³n", fontWeight = FontWeight.SemiBold)
                                    Text(
                                        publication.location.substringAfter(
                                            '|',
                                            publication.location
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (publication.createdAt.isNotBlank()) {
                            Text(
                                "Publicada el ${marketplacePublishedLabel(publication.createdAt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        error?.let {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(
                                    it,
                                    modifier = Modifier.padding(12.dp),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }

                        if (isOwner) {
                            HorizontalDivider()
                            Text(
                                "Administrar publicaciÃ³n",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MARKETPLACE_STATUSES.forEach { (status, label) ->
                                    FilterChip(
                                        selected = publication.publicationStatus == status,
                                        enabled = !loading,
                                        onClick = { onStatus(status) },
                                        label = { Text(label) }
                                    )
                                }
                            }
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 12.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        if (isOwner) {
                            OutlinedButton(
                                modifier = Modifier.weight(1f).height(50.dp),
                                enabled = !loading,
                                onClick = {
                                    picker.launch(
                                        arrayOf("image/jpeg", "image/png", "image/webp")
                                    )
                                }
                            ) { Text("Agregar fotografÃ­a") }
                        } else {
                            Button(
                                modifier = Modifier.weight(1f).height(50.dp),
                                enabled = !loading && account != null,
                                onClick = onContact
                            ) {
                                Text(
                                    if (account == null) {
                                        "Inicia sesiÃ³n para contactar"
                                    } else {
                                        "Contactar al vendedor"
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MarketplaceRemoteImage(
    url: String?,
    publicationId: String = "",
    modifier: Modifier = Modifier.fillMaxWidth().aspectRatio(1.55f)
) {
    var bitmap by remember(url, publicationId) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(url, publicationId) {
        bitmap = runCatching {
            withContext(Dispatchers.IO) {
                url?.takeIf(String::isNotBlank)?.let(RemoteImageLoader::loadBitmap)
            }
        }.getOrNull()
    }
    ProductImage(bitmap, modifier)
}

@Composable
private fun CreateMarketplaceDialog(
    currency: MarketplaceCurrency,
    onDismiss: () -> Unit,
    onCreate: (ProductPublication) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var productStatus by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priceDigits by remember { mutableStateOf("") }
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
                Text("Crear publicaciÃ³n", style = MaterialTheme.typography.headlineMedium)
                AppInput("Nombre del producto", title) { title = it }
                ProductStatusSelector(
                    selectedStatus = productStatus,
                    onStatusSelected = { productStatus = it }
                )
                OutlinedTextField(
                    value = priceDigits,
                    onValueChange = { priceDigits = normalizeWholeUnitInput(it) },
                    label = { Text("Precio (${currency.code})") },
                    supportingText = { Text(currency.name.replaceFirstChar(Char::uppercase)) },
                    prefix = {
                        Text(
                            currency.symbol,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    visualTransformation = WholeUnitPriceVisualTransformation(
                        currency.thousandsSeparator
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .appBikeTextFieldGlow(),
                    shape = RoundedCornerShape(14.dp),
                    colors = appBikeTextFieldColors()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("DescripciÃ³n") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .appBikeTextFieldGlow(),
                    shape = RoundedCornerShape(14.dp),
                    colors = appBikeTextFieldColors()
                )
                Text("FotografÃ­a del producto", fontWeight = FontWeight.Bold)
                MarketplaceLocalImage(imageUri)
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        imagePicker.launch(arrayOf("image/jpeg", "image/png", "image/webp"))
                    }
                ) {
                    Text(
                        if (imageUri.isBlank()) "Seleccionar fotografÃ­a"
                        else "Cambiar fotografÃ­a"
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
                ) {
                    OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
                    Button(
                        enabled = title.isNotBlank() && productStatus.isNotBlank() &&
                            priceDigits.toLongOrNull()?.let { it > 0L } == true &&
                            description.isNotBlank(),
                        onClick = {
                            onCreate(
                                ProductPublication(
                                    title = title,
                                    price = priceDigits,
                                    category = "",
                                    condition = productStatus,
                                    seller = "",
                                    description = description,
                                    mediaDescription = "",
                                    productStatus = productStatus,
                                    publicationStatus = "activa",
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
internal fun ProductStatusSelector(
    selectedStatus: String,
    onStatusSelected: (String) -> Unit
) {
    val options = listOf(
        "nuevo" to "Nuevo",
        "usado" to "Usado",
        "reacondicionado" to "Reacondicionado"
    )
    var expanded by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = options.firstOrNull { it.first == selectedStatus }?.second
                    ?: "Selecciona el estado del producto",
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.82f)
        ) {
            options.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onStatusSelected(value)
                        expanded = false
                    }
                )
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
private fun ProductImage(
    bitmap: Bitmap?,
    modifier: Modifier = Modifier.fillMaxWidth().aspectRatio(1.55f)
) {
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "FotografÃ­a del producto",
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier.background(
                MaterialTheme.colorScheme.surfaceContainer,
                RoundedCornerShape(20.dp)
            ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Image,
                contentDescription = "Sin fotografÃ­a",
                modifier = Modifier.size(44.dp)
            )
        }
    }
}

private fun marketplaceStatusLabel(status: String): String =
    MARKETPLACE_STATUSES.firstOrNull { it.first == status }?.second
        ?: status.replace('_', ' ').replaceFirstChar(Char::uppercase)

private fun marketplacePublishedLabel(raw: String): String {
    val match = Regex(
        """^(\d{4})-(\d{2})-(\d{2})[ T](\d{2}):(\d{2})"""
    ).find(raw.trim()) ?: return raw
    val (year, month, day, hour, minute) = match.destructured
    return "$day/$month/$year Â· $hour:$minute"
}

private fun marketplaceLocationLabel(point: GeoPoint): String = point.label
    .substringBefore(',')
    .trim()
    .ifBlank {
        String.format(
            java.util.Locale.US,
            "%.5f, %.5f",
            point.latitude,
            point.longitude
        )
    }

private fun publicationSellerName(publication: ProductPublication): String =
    publication.createdByUsername
        ?.takeIf(String::isNotBlank)
        ?: publication.seller.takeIf {
            it.isNotBlank() && !isValidAccountUserId(it)
        }
        ?: "Usuario de APPBIKE"
