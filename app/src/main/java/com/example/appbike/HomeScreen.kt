package com.example.appbike

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.VideoView
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.viewinterop.AndroidView
import com.example.appbike.ui.theme.AppBorderActive
import com.example.appbike.ui.theme.AppPrimaryBright
import com.example.appbike.ui.theme.AppSurfaceElevated
import com.example.appbike.ui.theme.AppTextPrimary
import com.example.appbike.ui.theme.AppTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val HOME_HERO_ROTATION_MS = 4_000L

private val HOME_FALLBACK_CENTER = GeoPoint(
    latitude = -33.4489,
    longitude = -70.6693,
    label = "Santiago"
)

private val HOME_HERO_IMAGES = intArrayOf(
    R.drawable.home_hero_forest,
    R.drawable.home_hero_ridge,
    R.drawable.home_hero_gravel,
    R.drawable.home_hero_group
)

private data class HomeSocialEntry(
    val kind: HomeSocialKind,
    val id: String,
    val title: String,
    val author: String,
    val summary: String,
    val supporting: String,
    val imageSource: String,
    val mediaType: String = "photo",
    val createdAt: String
)

private enum class HomeSocialKind { MEETUP, PERSONAL }

@Composable
fun HomeScreen(
    session: AccountSession?,
    onOpenMap: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onOpenMarketplace: () -> Unit = {},
    isActive: Boolean = true
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val center = remember(context) {
        LocalDataStore.loadLocation(context)
            ?: HOME_FALLBACK_CENTER
    }
    var meetups by remember { mutableStateOf(emptyList<MeetupEvent>()) }
    var personalPosts by remember(session?.userId) {
        mutableStateOf(emptyList<SocialPost>())
    }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var hasLoaded by remember { mutableStateOf(false) }

    fun refresh() {
        if (loading && meetups.isNotEmpty()) return
        scope.launch {
            loading = true
            error = null
            runSuspendCatching {
                withContext(Dispatchers.IO) {
                    RemoteConnections.loadNearbyMeetups(center = center, status = "activa")
                }
            }.onSuccess { result ->
                meetups = result
            }.onFailure {
                error = RemoteConnections.userFriendlyError(it)
            }
            loading = false
            hasLoaded = true
        }
    }

    LaunchedEffect(center, isActive) {
        if (isActive && !hasLoaded) refresh()
    }

    LaunchedEffect(session?.userId, isActive) {
        val userId = session?.userId
        personalPosts = if (isActive && userId != null) {
            withContext(Dispatchers.IO) {
                LocalDataStore.loadSocialPosts(context, userId)
            }
        } else if (userId == null) {
            emptyList()
        } else {
            personalPosts
        }
    }

    val entries = remember(meetups, personalPosts) {
        homeSocialEntries(meetups, personalPosts)
    }
    val storyEntries = remember(entries) { entries.take(10) }

    PremiumScreenBackground(PremiumGlowStyle.Home) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                HomeAdventureHero(
                    username = session?.username.orEmpty(),
                    meetupCount = meetups.size,
                    loading = loading,
                    isActive = isActive,
                    onOpenMap = onOpenMap
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 12.dp, top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Descubre la comunidad",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = AppTextPrimary
                        )
                        Text(
                            text = "Rutas, aventuras y riders cerca de ti",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppTextSecondary
                        )
                    }
                    IconButton(onClick = ::refresh, enabled = !loading) {
                        Icon(
                            Icons.Outlined.Refresh,
                            contentDescription = "Actualizar historias de la comunidad",
                            tint = AppPrimaryBright
                        )
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
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        HomeStory(
                            title = "Mapa",
                            imageSource = "",
                            isMapShortcut = true,
                            onClick = onOpenMap
                        )
                    }
                    items(storyEntries, key = { it.id }) { entry ->
                        HomeStory(
                            title = entry.author.ifBlank { entry.title },
                            imageSource = entry.imageSource,
                            onClick = onOpenMap
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

            item {
                Text(
                    "Aventuras de riders",
                    modifier = Modifier.padding(horizontal = 18.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = AppTextPrimary
                )
            }

            if (loading && entries.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(AppDimens.Space3)
                    ) {
                        AppSkeleton(Modifier.fillMaxWidth().height(260.dp))
                        AppSkeleton(Modifier.fillMaxWidth().height(180.dp))
                    }
                }
            } else if (entries.isEmpty()) {
                item {
                    EmptyState(
                        title = "Todavía no hay aventuras cerca de ti",
                        description = "Abre el mapa para descubrir o crear la próxima junta.",
                        modifier = Modifier.padding(horizontal = 18.dp),
                        actionLabel = "Explorar mapa",
                        onAction = onOpenMap
                    )
                }
            } else {
                items(entries, key = { "feed:${it.id}" }) { entry ->
                    HomeSocialCard(
                        entry = entry,
                        isActive = isActive,
                        onClick = if (entry.kind == HomeSocialKind.MEETUP) onOpenMap else null
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeAdventureHero(
    username: String,
    meetupCount: Int,
    loading: Boolean,
    isActive: Boolean,
    onOpenMap: () -> Unit
) {
    var heroIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(isActive) {
        if (!isActive) return@LaunchedEffect
        while (true) {
            delay(HOME_HERO_ROTATION_MS)
            heroIndex = (heroIndex + 1) % HOME_HERO_IMAGES.size
        }
    }

    Surface(
        modifier = Modifier
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.RadiusXLarge),
        color = Color.Black,
        border = BorderStroke(1.dp, AppBorderActive.copy(alpha = 0.48f)),
        shadowElevation = AppElevation.Floating
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 360.dp, max = 400.dp)
        ) {
            Crossfade(
                modifier = Modifier.matchParentSize(),
                targetState = HOME_HERO_IMAGES[heroIndex],
                animationSpec = tween(AppMotion.Emphasis),
                label = "Cambio de aventura"
            ) { imageRes ->
                Box(Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(imageRes),
                        contentDescription = "Aventura de ciclismo en la montaña",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.16f),
                            0.42f to Color.Black.copy(alpha = 0.25f),
                            1f to Color.Black.copy(alpha = 0.94f)
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(AppDimens.RadiusPill),
                        color = Color.Black.copy(alpha = 0.58f),
                        border = BorderStroke(1.dp, AppPrimaryBright.copy(alpha = 0.58f))
                    ) {
                        Text(
                            text = "RIDE  •  CONNECT",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = AppPrimaryBright,
                            fontWeight = FontWeight.Black
                        )
                    }
                    if (username.isNotBlank()) {
                        Text(
                            text = "@$username",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.height(22.dp))
                Text(
                    text = "TU PRÓXIMA AVENTURA",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    maxLines = 2
                )
                Text(
                    text = "Historias, rutas y juntas para tu siguiente salida.",
                    modifier = Modifier.padding(top = 6.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.84f)
                )
                HomeHeroMetric(
                    value = if (loading) "…" else meetupCount.toString(),
                    label = "juntas activas",
                    modifier = Modifier.padding(top = 12.dp)
                )
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .heightIn(min = AppSizes.TouchTarget),
                    onClick = onOpenMap
                ) {
                    Icon(Icons.Outlined.Map, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Explorar mapa", fontWeight = FontWeight.Black)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HOME_HERO_IMAGES.indices.forEach { index ->
                        Surface(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(if (index == heroIndex) 8.dp else 6.dp),
                            shape = CircleShape,
                            color = if (index == heroIndex) AppPrimaryBright else Color.White.copy(alpha = 0.52f)
                        ) {}
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeroMetric(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(AppDimens.RadiusPill),
        color = Color.Black.copy(alpha = 0.58f),
        border = BorderStroke(1.dp, AppPrimaryBright.copy(alpha = 0.52f))
    ) {
        Text(
            text = "$value $label",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelSmall,
            color = AppPrimaryBright,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

private fun homeSocialEntries(
    meetups: List<MeetupEvent>,
    personalPosts: List<SocialPost>
): List<HomeSocialEntry> = buildList {
    personalPosts.forEach { post ->
        add(
            HomeSocialEntry(
                kind = HomeSocialKind.PERSONAL,
                id = post.id,
                title = "Experiencia de ${post.username}",
                author = post.username,
                summary = post.caption,
                supporting = "Publicación personal",
                imageSource = post.mediaUri,
                mediaType = post.mediaType,
                createdAt = post.createdAt.toString()
            )
        )
    }
    meetups.forEach { event ->
        add(
            HomeSocialEntry(
                kind = HomeSocialKind.MEETUP,
                id = event.id,
                title = event.title,
                author = event.createdByUsername?.takeIf(String::isNotBlank) ?: "Rider APPBIKE",
                summary = event.description,
                supporting = listOf(
                    event.dateTime,
                    event.location.substringAfter('|').ifBlank { event.region }
                ).filter(String::isNotBlank).joinToString(" · "),
                imageSource = event.images.firstOrNull().orEmpty(),
                createdAt = event.createdAt
            )
        )
    }
}

@Composable
private fun HomeStory(
    title: String,
    imageSource: String,
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
            border = BorderStroke(2.dp, AppPrimaryBright.copy(alpha = 0.82f))
        ) {
            Box(Modifier.padding(3.dp).clip(CircleShape), contentAlignment = Alignment.Center) {
                HomeRemoteImage(
                    source = imageSource,
                    modifier = Modifier.fillMaxSize(),
                    fallback = {
                        Icon(
                            imageVector = if (isMapShortcut) Icons.Outlined.Map else Icons.Outlined.Groups,
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
private fun HomeSocialCard(
    entry: HomeSocialEntry,
    isActive: Boolean,
    onClick: (() -> Unit)?
) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 14.dp)
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(AppDimens.RadiusLarge),
        color = AppSurfaceElevated,
        border = BorderStroke(1.dp, AppPrimaryBright.copy(alpha = 0.24f)),
        shadowElevation = AppElevation.Raised
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = CircleShape, color = AppPrimaryBright.copy(alpha = 0.16f)) {
                    Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.Groups,
                            contentDescription = null,
                            tint = AppPrimaryBright,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp)
                ) {
                    Text(
                        entry.author,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = AppTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        if (entry.kind == HomeSocialKind.MEETUP) {
                            "Junta de la comunidad"
                        } else {
                            "Publicación personal"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTextSecondary
                    )
                }
                HomeEntryTypeBadge(
                    if (entry.kind == HomeSocialKind.MEETUP) "AVENTURA" else "RIDER",
                    AppPrimaryBright
                )
            }

            Box(Modifier.fillMaxWidth().heightIn(min = 250.dp, max = 320.dp)) {
                if (entry.kind == HomeSocialKind.PERSONAL) {
                    HomeLocalMedia(
                        uri = entry.imageSource,
                        mediaType = entry.mediaType,
                        isActive = isActive,
                        modifier = Modifier.matchParentSize()
                    )
                } else {
                    HomeRemoteImage(
                        source = entry.imageSource,
                        modifier = Modifier.matchParentSize(),
                        fallback = {
                            Image(
                                painter = painterResource(R.drawable.home_hero_forest),
                                contentDescription = null,
                                modifier = Modifier.matchParentSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    )
                }
            }

            Column(
                modifier = Modifier.padding(AppDimens.Space4),
                verticalArrangement = Arrangement.spacedBy(7.dp)
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
                if (entry.kind == HomeSocialKind.MEETUP) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Ver en el mapa",
                            style = MaterialTheme.typography.labelLarge,
                            color = AppPrimaryBright,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Outlined.Map,
                            contentDescription = null,
                            tint = AppPrimaryBright,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeEntryTypeBadge(label: String, accent: Color) {
    Surface(
        shape = RoundedCornerShape(AppDimens.RadiusPill),
        color = Color.Black.copy(alpha = 0.60f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.68f))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = accent
        )
    }
}

@Composable
private fun HomeLocalMedia(
    uri: String,
    mediaType: String,
    isActive: Boolean,
    modifier: Modifier
) {
    val context = LocalContext.current
    if (mediaType == "video") {
        val videoView = remember(uri) {
            VideoView(context).apply {
                setVideoURI(Uri.parse(uri))
                setOnPreparedListener { player ->
                    player.isLooping = true
                    if (isActive) start()
                }
            }
        }
        LaunchedEffect(isActive, videoView) {
            if (isActive) videoView.start() else videoView.pause()
        }
        DisposableEffect(videoView) {
            onDispose { videoView.stopPlayback() }
        }
        AndroidView(
            factory = { videoView },
            modifier = modifier,
            update = { view ->
                if (isActive && !view.isPlaying) view.start()
                if (!isActive && view.isPlaying) view.pause()
            }
        )
    } else {
        var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }
        LaunchedEffect(uri) {
            bitmap = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(Uri.parse(uri))?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }.getOrNull()
            }
        }
        val loaded = bitmap
        if (loaded == null) {
            Image(
                painter = painterResource(R.drawable.home_hero_forest),
                contentDescription = "Fotografía de la publicación personal",
                modifier = modifier,
                contentScale = ContentScale.Crop
            )
        } else {
            Image(
                bitmap = loaded.asImageBitmap(),
                contentDescription = "Fotografía de la publicación personal",
                modifier = modifier,
                contentScale = ContentScale.Crop
            )
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
                contentDescription = "Fotografía de la aventura de ${source.take(20)}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}
