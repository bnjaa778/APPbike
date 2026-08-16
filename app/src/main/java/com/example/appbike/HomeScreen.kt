package com.example.appbike

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsBike
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.appbike.ui.theme.AppBorderActive
import com.example.appbike.ui.theme.AppBorderSubtle
import com.example.appbike.ui.theme.AppPrimary
import com.example.appbike.ui.theme.AppPrimaryBright
import com.example.appbike.ui.theme.AppPrimarySoft
import com.example.appbike.ui.theme.AppSurfaceElevated
import com.example.appbike.ui.theme.AppTextPrimary
import com.example.appbike.ui.theme.AppTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val HOME_FALLBACK_CENTER = GeoPoint(
    latitude = -33.4489,
    longitude = -70.6693,
    label = "Santiago"
)

private enum class HomeNewsKind { MEETUP, MARKETPLACE }

private data class HomeNewsEntry(
    val kind: HomeNewsKind,
    val id: String,
    val title: String,
    val author: String,
    val summary: String,
    val supporting: String,
    val imageSource: String,
    val createdAt: String
)

private data class HomeNewsPayload(
    val meetups: List<MeetupEvent>,
    val marketplace: List<ProductPublication>,
    val warning: String? = null
)

@Composable
fun HomeScreen(
    session: AccountSession?,
    onOpenMap: () -> Unit,
    onOpenMarketplace: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val center = remember(context) {
        LocalDataStore.loadLocation(context)
            ?: LocalDataStore.loadMarketplaceLocation(context)
            ?: HOME_FALLBACK_CENTER
    }
    var payload by remember { mutableStateOf(HomeNewsPayload(emptyList(), emptyList())) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        if (loading && payload.meetups.isNotEmpty()) return
        scope.launch {
            loading = true
            error = null
            val result = runSuspendCatching {
                coroutineScope {
                    val meetups = async(Dispatchers.IO) {
                        runSuspendCatching {
                            RemoteConnections.loadNearbyMeetups(center = center, status = "activa")
                        }
                    }
                    val marketplace = async(Dispatchers.IO) {
                        runSuspendCatching {
                            RemoteConnections.loadMarketplacePosts(center = center, status = "activa")
                        }
                    }
                    val meetupResult = meetups.await()
                    val marketplaceResult = marketplace.await()
                    if (meetupResult.isFailure && marketplaceResult.isFailure) {
                        throw meetupResult.exceptionOrNull()
                            ?: marketplaceResult.exceptionOrNull()
                            ?: IllegalStateException("No fue posible cargar las novedades.")
                    }
                    HomeNewsPayload(
                        meetups = meetupResult.getOrDefault(emptyList()),
                        marketplace = marketplaceResult.getOrDefault(emptyList()),
                        warning = if (meetupResult.isFailure || marketplaceResult.isFailure) {
                            "Se cargó solo una parte de las novedades. Puedes volver a intentar."
                        } else {
                            null
                        }
                    )
                }
            }
            result.onSuccess {
                payload = it
                error = it.warning
            }.onFailure {
                error = RemoteConnections.userFriendlyError(it)
            }
            loading = false
        }
    }

    LaunchedEffect(center) { refresh() }

    val entries = remember(payload) { homeNewsEntries(payload) }
    val storyEntries = remember(entries) { entries.take(10) }

    PremiumScreenBackground(PremiumGlowStyle.Map) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 12.dp, top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Novedades",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = AppTextPrimary
                        )
                        Text(
                            text = session?.username?.takeIf(String::isNotBlank)?.let { "Hola, @$it" }
                                ?: "Historias de la comunidad APPBIKE",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppTextSecondary
                        )
                    }
                    IconButton(onClick = ::refresh, enabled = !loading) {
                        Icon(
                            Icons.Outlined.Refresh,
                            contentDescription = "Actualizar novedades",
                            tint = AppPrimaryBright
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = AppPrimarySoft,
                        border = BorderStroke(1.dp, AppBorderActive)
                    ) {
                        IconButton(onClick = onOpenMap) {
                            Icon(
                                painterResource(R.drawable.ic_nav_routes),
                                contentDescription = "Abrir mapa de juntas",
                                tint = AppPrimaryBright
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    "Historias",
                    modifier = Modifier.padding(horizontal = 18.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppTextPrimary
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        HomeStory(
                            title = "Mapa",
                            imageSource = "",
                            kind = HomeNewsKind.MEETUP,
                            isMapShortcut = true,
                            onClick = onOpenMap
                        )
                    }
                    items(storyEntries, key = { "${it.kind}:${it.id}" }) { entry ->
                        HomeStory(
                            title = entry.author.ifBlank { entry.title },
                            imageSource = entry.imageSource,
                            kind = entry.kind,
                            onClick = {
                                if (entry.kind == HomeNewsKind.MEETUP) onOpenMap()
                                else onOpenMarketplace()
                            }
                        )
                    }
                }
            }

            if (error != null) {
                item {
                    ErrorBanner(
                        message = error.orEmpty(),
                        modifier = Modifier.padding(horizontal = 18.dp)
                    )
                }
            }

            if (loading && entries.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AppPrimaryBright)
                    }
                }
            } else if (entries.isEmpty()) {
                item {
                    AppCard(modifier = Modifier.padding(horizontal = 18.dp).fillMaxWidth()) {
                        Icon(
                            Icons.AutoMirrored.Outlined.DirectionsBike,
                            contentDescription = null,
                            tint = AppPrimaryBright,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            "Todavía no hay novedades cerca de ti",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Abre el mapa para descubrir o crear la próxima junta.",
                            color = AppTextSecondary
                        )
                    }
                }
            } else {
                items(entries, key = { "feed:${it.kind}:${it.id}" }) { entry ->
                    HomeFeedCard(
                        entry = entry,
                        onClick = {
                            if (entry.kind == HomeNewsKind.MEETUP) onOpenMap()
                            else onOpenMarketplace()
                        }
                    )
                }
            }
        }
    }
}

private fun homeNewsEntries(payload: HomeNewsPayload): List<HomeNewsEntry> =
    buildList {
        payload.meetups.forEach { event ->
            add(
                HomeNewsEntry(
                    kind = HomeNewsKind.MEETUP,
                    id = event.id,
                    title = event.title,
                    author = event.createdByUsername?.takeIf(String::isNotBlank) ?: "Junta APPBIKE",
                    summary = event.description,
                    supporting = listOf(event.dateTime, event.location.substringAfter('|'))
                        .filter(String::isNotBlank)
                        .joinToString(" · "),
                    imageSource = event.images.firstOrNull().orEmpty(),
                    createdAt = event.createdAt
                )
            )
        }
        payload.marketplace.forEach { post ->
            add(
                HomeNewsEntry(
                    kind = HomeNewsKind.MARKETPLACE,
                    id = post.id,
                    title = post.title,
                    author = post.createdByUsername?.takeIf(String::isNotBlank)
                        ?: post.seller.takeIf { it.isNotBlank() && !isValidAccountUserId(it) }
                        ?: "Marketplace",
                    summary = post.description,
                    supporting = formatMarketplacePrice(
                        rawPrice = post.price,
                        currency = marketplaceCurrency(post.currencyCode)
                    ),
                    imageSource = post.images.firstOrNull().orEmpty(),
                    createdAt = post.createdAt
                )
            )
        }
    }.sortedByDescending(HomeNewsEntry::createdAt)

@Composable
private fun HomeStory(
    title: String,
    imageSource: String,
    kind: HomeNewsKind,
    isMapShortcut: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .size(width = 76.dp, height = 100.dp)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Historia: $title" },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(68.dp),
            shape = CircleShape,
            color = Color.Transparent,
            border = BorderStroke(2.dp, if (isMapShortcut) AppPrimaryBright else AppBorderActive)
        ) {
            Box(Modifier.padding(3.dp).clip(CircleShape), contentAlignment = Alignment.Center) {
                HomeRemoteImage(
                    source = imageSource,
                    modifier = Modifier.fillMaxSize(),
                    fallback = {
                        Icon(
                            imageVector = when {
                                isMapShortcut -> Icons.Outlined.Map
                                kind == HomeNewsKind.MEETUP -> Icons.Outlined.Groups
                                else -> Icons.Outlined.Storefront
                            },
                            contentDescription = null,
                            modifier = Modifier.size(30.dp),
                            tint = AppPrimaryBright
                        )
                    }
                )
            }
        }
        Text(
            text = title,
            modifier = Modifier.padding(top = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            color = AppTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HomeFeedCard(entry: HomeNewsEntry, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 14.dp)
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = AppSurfaceElevated,
        border = BorderStroke(1.dp, AppBorderSubtle)
    ) {
        Column {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = CircleShape, color = AppPrimarySoft) {
                    Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            if (entry.kind == HomeNewsKind.MEETUP) {
                                Icons.Outlined.Groups
                            } else {
                                Icons.Outlined.Storefront
                            },
                            contentDescription = null,
                            tint = AppPrimaryBright,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
                Column(Modifier.padding(start = 10.dp).weight(1f)) {
                    Text(entry.author, fontWeight = FontWeight.Bold, color = AppTextPrimary)
                    Text(
                        if (entry.kind == HomeNewsKind.MEETUP) "Junta" else "Marketplace",
                        style = MaterialTheme.typography.labelMedium,
                        color = AppTextSecondary
                    )
                }
            }
            HomeRemoteImage(
                source = entry.imageSource,
                modifier = Modifier.fillMaxWidth().aspectRatio(1.22f),
                fallback = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.AutoMirrored.Outlined.DirectionsBike,
                            contentDescription = null,
                            modifier = Modifier.size(52.dp),
                            tint = AppPrimaryBright
                        )
                        Text("APPBIKE", color = AppTextSecondary, fontWeight = FontWeight.Bold)
                    }
                }
            )
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    entry.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppTextPrimary
                )
                if (entry.summary.isNotBlank()) {
                    Text(
                        entry.summary,
                        color = AppTextSecondary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (entry.supporting.isNotBlank()) {
                    Text(
                        entry.supporting,
                        style = MaterialTheme.typography.labelLarge,
                        color = AppPrimaryBright,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeRemoteImage(
    source: String,
    modifier: Modifier,
    fallback: @Composable () -> Unit
) {
    var bitmap by remember(source) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(source) {
        bitmap = if (source.isBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching { RemoteImageLoader.loadBitmap(source) }.getOrNull()
            }
        }
    }
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainer),
        contentAlignment = Alignment.Center
    ) {
        val loaded = bitmap
        if (loaded == null) {
            fallback()
        } else {
            Image(
                bitmap = loaded.asImageBitmap(),
                contentDescription = "Fotografía de ${if (source.contains("junta")) "junta" else "novedad"}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}
