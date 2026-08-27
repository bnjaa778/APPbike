package com.example.appbike

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.widthIn
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.appbike.ui.theme.APPbikeTheme
import com.example.appbike.ui.theme.AppBackgroundElevated
import com.example.appbike.ui.theme.AppBorderActive
import com.example.appbike.ui.theme.AppBorderSubtle
import com.example.appbike.ui.theme.AppPrimaryBright
import com.example.appbike.ui.theme.AppPrimarySoft
import com.example.appbike.ui.theme.AppSurfaceElevated
import com.example.appbike.ui.theme.AppTextPrimary
import com.example.appbike.ui.theme.AppTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    private val sportsOauthCallback = mutableStateOf<SportsOAuthCallback?>(null)
    private val notificationChatTarget = mutableStateOf<NotificationChatTarget?>(null)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        captureSportsOauth(intent)
        captureNotificationTarget(intent)
        ChatNotificationCenter.createChannel(this)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        setContent {
            APPbikeTheme(dynamicColor = false) {
                var showLaunchBrand by remember {
                    mutableStateOf(savedInstanceState == null)
                }
                LaunchedEffect(showLaunchBrand) {
                    if (showLaunchBrand) {
                        delay(LAUNCH_BRAND_DURATION_MS)
                        showLaunchBrand = false
                    }
                }

                if (showLaunchBrand) {
                    LaunchBrandScreen()
                } else {
                    AppBikeApp(
                        oauthCallback = sportsOauthCallback.value,
                        onOauthCallbackConsumed = { sportsOauthCallback.value = null },
                        notificationTarget = notificationChatTarget.value,
                        onNotificationTargetConsumed = { notificationChatTarget.value = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        captureSportsOauth(intent)
        captureNotificationTarget(intent)
    }

    override fun onStart() {
        super.onStart()
        ChatNotificationCenter.appInForeground = true
    }

    override fun onStop() {
        ChatNotificationCenter.appInForeground = false
        super.onStop()
    }

    fun requestNotificationPermissionOnce() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) return
        val prefs = getSharedPreferences("appbike_permissions", MODE_PRIVATE)
        if (prefs.getBoolean("notifications_listener_v2_asked", false)) return
        prefs.edit { putBoolean("notifications_listener_v2_asked", true) }
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun captureSportsOauth(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme != "appbike" || uri.host != "oauth") return
        sportsOauthCallback.value = SportsOAuthCallback(
            provider = uri.getQueryParameter("provider").orEmpty(),
            code = uri.getQueryParameter("code").orEmpty(),
            state = uri.getQueryParameter("state").orEmpty(),
            error = uri.getQueryParameter("error").orEmpty()
        )
    }

    private fun captureNotificationTarget(intent: Intent?) {
        val chatId = intent?.getStringExtra(ChatNotificationCenter.EXTRA_CHAT_ID)
            ?.takeIf(String::isNotBlank)
            ?: return
        val recipientUserId = intent
            .getStringExtra(ChatNotificationCenter.EXTRA_RECIPIENT_USER_ID)
            ?.takeIf(String::isNotBlank)
            ?: run {
                intent.removeExtra(ChatNotificationCenter.EXTRA_CHAT_ID)
                return
            }
        val type = runCatching {
            ChatType.valueOf(
                intent.getStringExtra(ChatNotificationCenter.EXTRA_CHAT_TYPE).orEmpty()
            )
        }.getOrDefault(ChatType.SOCIAL)
        notificationChatTarget.value = NotificationChatTarget(
            recipientUserId = recipientUserId,
            chatId = chatId,
            type = type,
            title = intent.getStringExtra(ChatNotificationCenter.EXTRA_CHAT_TITLE).orEmpty()
        )
        intent.removeExtra(ChatNotificationCenter.EXTRA_CHAT_ID)
        intent.removeExtra(ChatNotificationCenter.EXTRA_RECIPIENT_USER_ID)
    }
}

private const val LAUNCH_BRAND_DURATION_MS = 5_000L
private const val LAUNCH_BRAND_ANIMATION_MS = 4_500
private val BRAND_LOGO_SIZE = 220.dp
private const val WEATHER_REFRESH_INTERVAL_MS = 15 * 60 * 1_000L
private const val WEATHER_RETRY_INTERVAL_MS = 60_000L
private const val WEATHER_PERMISSION_RECHECK_MS = 2_000L
private val MAIN_SWIPE_DESTINATIONS = listOf(
    AppScreen.HOME,
    AppScreen.MARKETPLACE,
    AppScreen.ROUTES,
    AppScreen.CHAT,
    AppScreen.ACCOUNT
)

internal fun initialDestinationFor(session: AccountSession?): AppScreen =
    if (session == null || session.username.isNullOrBlank()) {
        AppScreen.ACCOUNT
    } else {
        AppScreen.HOME
    }

internal fun mainDestinationFor(screen: AppScreen): AppScreen = when (screen) {
    AppScreen.BIKES, AppScreen.SYNC -> AppScreen.ACCOUNT
    AppScreen.CREATE_PUBLICATION -> AppScreen.MARKETPLACE
    else -> screen
}

internal fun mainDestinationIndex(screen: AppScreen): Int =
    MAIN_SWIPE_DESTINATIONS.indexOf(mainDestinationFor(screen))

internal fun mainDestinationAt(page: Int): AppScreen =
    MAIN_SWIPE_DESTINATIONS[page.coerceIn(MAIN_SWIPE_DESTINATIONS.indices)]

@Composable
private fun LaunchBrandScreen() {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = LAUNCH_BRAND_ANIMATION_MS,
                easing = FastOutSlowInEasing
            )
        )
    }
    val animationProgress = progress.value
    val revealProgress = FastOutSlowInEasing.transform(
        ((animationProgress - 0.48f) / 0.30f).coerceIn(0f, 1f)
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .clearAndSetSemantics { }
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val logoSizePx = BRAND_LOGO_SIZE.toPx()
                val logoRadius = logoSizePx * 0.24f
                val glowRadius = 5.dp.toPx()
                val ledRadius = 1.8.dp.toPx()
                val corners = arrayOf(
                    Offset.Zero,
                    Offset(size.width, 0f),
                    Offset(0f, size.height),
                    Offset(size.width, size.height)
                )

                repeat(40) { index ->
                    val stagger = (index % 10) * 0.018f
                    val travel = ((animationProgress - stagger) / 0.62f)
                        .coerceIn(0f, 1f)
                    val easedTravel = FastOutSlowInEasing.transform(travel)
                    val baseAngle = Math.toRadians(index * 137.508)
                    val direction = if (index % 2 == 0) 1f else -1f
                    val spin = (1f - easedTravel) * PI.toFloat() * 2.2f * direction
                    val angle = baseAngle + spin
                    val ring = 0.18f + (index % 7) / 8f
                    val target = Offset(
                        x = center.x + cos(angle).toFloat() * logoRadius * ring,
                        y = center.y + sin(angle).toFloat() * logoRadius * ring * 1.28f
                    )
                    val origin = corners[index % corners.size]
                    val bend = sin(PI * easedTravel).toFloat() *
                        logoRadius * (0.20f + (index % 3) * 0.06f)
                    val position = Offset(
                        x = origin.x + (target.x - origin.x) * easedTravel + bend * direction,
                        y = origin.y + (target.y - origin.y) * easedTravel - bend * direction
                    )
                    val settled = ((travel - 0.72f) / 0.28f).coerceIn(0f, 1f)
                    val particleAlpha = ((1f - revealProgress) *
                        (1f - settled * 0.55f)).coerceIn(0f, 1f)
                    if (particleAlpha > 0.01f) {
                        drawCircle(
                            color = Color.White.copy(alpha = particleAlpha * 0.18f),
                            radius = glowRadius,
                            center = position
                        )
                        drawLine(
                            color = Color.White.copy(alpha = particleAlpha * 0.55f),
                            start = Offset(position.x - ledRadius * 2.2f, position.y),
                            end = Offset(position.x + ledRadius * 2.2f, position.y),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawLine(
                            color = Color.White.copy(alpha = particleAlpha * 0.55f),
                            start = Offset(position.x, position.y - ledRadius * 2.2f),
                            end = Offset(position.x, position.y + ledRadius * 2.2f),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = particleAlpha),
                            radius = ledRadius + (index % 3) * 0.28.dp.toPx(),
                            center = position
                        )
                    }
                }
            }
            Image(
                painter = painterResource(R.drawable.appbike_brand_icon),
                contentDescription = "APPBIKE",
                modifier = Modifier
                    .size(BRAND_LOGO_SIZE)
                    .graphicsLayer {
                        alpha = revealProgress
                    },
                contentScale = ContentScale.Fit
            )
        }
    }
}

data class NotificationChatTarget(
    val recipientUserId: String,
    val chatId: String,
    val type: ChatType,
    val title: String
) {
    fun asChat() = UserChat(
        id = chatId,
        type = type,
        participants = emptyList(),
        title = title
    )
}

internal data class MainDestination(
    val label: String,
    val screen: AppScreen,
    val icon: ImageVector,
    val emphasized: Boolean = false
)

@Composable
fun AppBikeApp(
    oauthCallback: SportsOAuthCallback? = null,
    onOauthCallbackConsumed: () -> Unit = {},
    notificationTarget: NotificationChatTarget? = null,
    onNotificationTargetConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val initialSession = remember(context) {
        AccountStore.loadSession(context).also(RemoteConnections::setSession)
    }
    var accountSession by remember {
        mutableStateOf(initialSession)
    }
    var profilePhotoUri by remember { mutableStateOf("") }
    var currentScreen by remember { mutableStateOf(initialDestinationFor(initialSession)) }
    var isValidatingInitialSession by remember { mutableStateOf(initialSession != null) }
    var initialLoginError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(accountSession?.userId) {
        profilePhotoUri = accountSession?.userId?.let { userId ->
            LocalDataStore.loadProfilePhotoUri(context, userId)
        }.orEmpty()
    }

    val bikes = remember { mutableStateListOf<Bike>() }
    val reminders = remember { mutableStateListOf<MaintenanceReminder>() }
    val bookings = remember { mutableStateListOf<ServiceBooking>() }
    var chatToOpen by remember { mutableStateOf<UserChat?>(null) }
    var meetupToOpenId by remember { mutableStateOf<String?>(null) }
    val pendingNotifications = remember { mutableStateListOf<MessageNotificationEvent>() }
    var activeNotification by remember { mutableStateOf<MessageNotificationEvent?>(null) }
    var unreadMessages by remember { mutableIntStateOf(0) }
    var weatherState by remember {
        mutableStateOf<WeatherHeaderState>(WeatherHeaderState.WaitingForLocation)
    }
    val platforms = remember {
        mutableStateListOf(
            SyncPlatform("strava", "Strava", "Actividades, rutas y entrenamientos.", false),
            SyncPlatform("garmin", "Garmin", "Relojes, ciclocomputadores y sensores.", false),
            SyncPlatform("wahoo", "Wahoo", "Entrenamientos y dispositivos deportivos.", false)
        )
    }

    val destinations = remember {
        listOf(
            MainDestination("Inicio", AppScreen.HOME, Icons.Outlined.Home),
            MainDestination(
                "Marketplace",
                AppScreen.MARKETPLACE,
                Icons.Outlined.Storefront
            ),
            MainDestination(
                "Mapa",
                AppScreen.ROUTES,
                Icons.Outlined.Map,
                emphasized = true
            ),
            MainDestination("Chat", AppScreen.CHAT, Icons.Outlined.ChatBubbleOutline),
            MainDestination("Perfil", AppScreen.ACCOUNT, Icons.Outlined.PersonOutline)
        )
    }

    LaunchedEffect(initialSession?.userId, initialSession?.accessToken) {
        val storedSession = initialSession ?: run {
            isValidatingInitialSession = false
            return@LaunchedEffect
        }
        val result = runSuspendCatching {
            withContext(Dispatchers.IO) {
                RemoteConnections.loadUserProfile(storedSession)
            }
        }
        if (accountSession?.userId != storedSession.userId) {
            isValidatingInitialSession = false
            return@LaunchedEffect
        }
        result.onSuccess { verifiedSession ->
            AccountStore.saveSession(context, verifiedSession)
            RemoteConnections.setSession(verifiedSession)
            accountSession = verifiedSession
            currentScreen = initialDestinationFor(verifiedSession)
        }.onFailure { error ->
            if (RemoteConnections.isAuthenticationFailure(error)) {
                ChatNotificationCenter.stopListener(context)
                ChatNotificationCenter.cancelBackgroundChecks(context)
                ChatNotificationCenter.clearNotifications(context)
                AccountStore.clearSession(context)
                RemoteConnections.setSession(null)
                accountSession = null
                currentScreen = AppScreen.ACCOUNT
                initialLoginError = "Tu sesión expiró. Inicia sesión nuevamente."
                bikes.clear()
                reminders.clear()
                bookings.clear()
            }
        }
        isValidatingInitialSession = false
    }

    LaunchedEffect(accountSession?.userId, accountSession?.username) {
        val session = accountSession ?: run {
            currentScreen = AppScreen.ACCOUNT
            return@LaunchedEffect
        }
        (context as? MainActivity)?.requestNotificationPermissionOnce()
        if (session.username.isNullOrBlank()) {
            currentScreen = AppScreen.ACCOUNT
        }
    }

    LaunchedEffect(context) {
        val lifecycle = (context as? MainActivity)?.lifecycle ?: return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                val hasLocationPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (!hasLocationPermission) {
                    weatherState = WeatherHeaderState.WaitingForLocation
                    delay(WEATHER_PERMISSION_RECHECK_MS)
                    continue
                }

                if (weatherState !is WeatherHeaderState.Ready) {
                    weatherState = WeatherHeaderState.Loading
                }
                val result = runSuspendCatching {
                    val point = DeviceLocationProvider.currentLocation(context)
                    withContext(Dispatchers.IO) {
                        RemoteConnections.loadCurrentWeather(
                            latitude = point.latitude,
                            longitude = point.longitude
                        )
                    }
                }
                weatherState = result.fold(
                    onSuccess = WeatherHeaderState::Ready,
                    onFailure = {
                        WeatherHeaderState.Unavailable(
                            RemoteConnections.userFriendlyError(it)
                        )
                    }
                )
                delay(
                    if (result.isSuccess) {
                        WEATHER_REFRESH_INTERVAL_MS
                    } else {
                        WEATHER_RETRY_INTERVAL_MS
                    }
                )
            }
        }
    }

    LaunchedEffect(accountSession?.userId, isValidatingInitialSession) {
        if (isValidatingInitialSession) return@LaunchedEffect
        val session = accountSession
        if (session == null) {
            ChatNotificationCenter.stopListener(context)
            ChatNotificationCenter.cancelBackgroundChecks(context)
            ChatNotificationCenter.clearNotifications(context)
            pendingNotifications.clear()
            activeNotification = null
            unreadMessages = 0
            return@LaunchedEffect
        }
        ChatNotificationCenter.startListener(context)
        ChatNotificationCenter.scheduleBackgroundChecks(context)
    }

    LaunchedEffect(accountSession?.userId) {
        val activeUserId = accountSession?.userId ?: return@LaunchedEffect
        ChatNotificationEventBus.events.collect { event ->
            if (isNotificationForActiveUser(event.recipientUserId, activeUserId)) {
                pendingNotifications.add(event)
                unreadMessages += 1
            }
        }
    }

    LaunchedEffect(pendingNotifications.size, activeNotification?.messageId) {
        if (activeNotification == null && pendingNotifications.isNotEmpty()) {
            activeNotification = pendingNotifications.removeAt(0)
        }
    }

    LaunchedEffect(activeNotification?.messageId) {
        if (activeNotification == null) return@LaunchedEffect
        delay(5_000L)
        activeNotification = null
    }

    LaunchedEffect(notificationTarget?.chatId, accountSession?.userId) {
        val target = notificationTarget ?: return@LaunchedEffect
        if (!isNotificationForActiveUser(target.recipientUserId, accountSession?.userId)) {
            onNotificationTargetConsumed()
            return@LaunchedEffect
        }
        chatToOpen = target.asChat()
        currentScreen = AppScreen.CHAT
        unreadMessages = 0
        onNotificationTargetConsumed()
    }

    if (isValidatingInitialSession) {
        SessionValidationScreen()
        return
    }

    val completeLogin: (AccountSession) -> Unit = { session ->
        initialLoginError = null
        AccountStore.saveSession(context, session)
        RemoteConnections.setSession(session)
        accountSession = session
        currentScreen = if (session.username.isNullOrBlank()) {
            AppScreen.ACCOUNT
        } else {
            AppScreen.HOME
        }
    }

    if (accountSession == null) {
        UnauthenticatedAccessScreen(
            initialErrorMessage = initialLoginError,
            onLogin = completeLogin
        )
        return
    }

    val pagerState = rememberPagerState(
        initialPage = mainDestinationIndex(currentScreen).coerceAtLeast(0),
        pageCount = { MAIN_SWIPE_DESTINATIONS.size }
    )

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                val destination = mainDestinationAt(page)
                if (currentScreen in MAIN_SWIPE_DESTINATIONS &&
                    currentScreen != destination
                ) {
                    if (destination == AppScreen.CHAT) unreadMessages = 0
                    currentScreen = destination
                }
            }
    }

    LaunchedEffect(currentScreen) {
        val canonicalScreen = mainDestinationFor(currentScreen)
        if (currentScreen != AppScreen.BIKES && canonicalScreen != currentScreen) {
            currentScreen = canonicalScreen
            return@LaunchedEffect
        }
        val targetPage = mainDestinationIndex(currentScreen)
        if (targetPage >= 0 && pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    val updateSession: (AccountSession) -> Unit = { session ->
        val wasCompletingIdentity = accountSession?.username.isNullOrBlank()
        AccountStore.saveSession(context, session)
        RemoteConnections.setSession(session)
        accountSession = session
        if (wasCompletingIdentity && !session.username.isNullOrBlank()) {
            currentScreen = AppScreen.HOME
        }
    }
    val logout: () -> Unit = {
        initialLoginError = null
        ChatNotificationCenter.stopListener(context)
        ChatNotificationCenter.cancelBackgroundChecks(context)
        ChatNotificationCenter.clearNotifications(context)
        AccountStore.clearSession(context)
        RemoteConnections.setSession(null)
        accountSession = null
        profilePhotoUri = ""
        currentScreen = AppScreen.ACCOUNT
        bikes.clear()
        reminders.clear()
        bookings.clear()
        chatToOpen = null
        meetupToOpenId = null
    }

    val mainDestinationContent: @Composable (AppScreen, Boolean) -> Unit =
        { destination, isActive ->
        when (destination) {
            AppScreen.HOME -> HomeScreen(
                session = accountSession,
                onOpenMap = { currentScreen = AppScreen.ROUTES },
                isActive = isActive
            )

            AppScreen.MARKETPLACE -> MarketplaceScreen(
                account = accountSession,
                onOpenChat = { chat ->
                    chatToOpen = chat
                    currentScreen = AppScreen.CHAT
                },
                onOpenAccount = { currentScreen = AppScreen.ACCOUNT },
                isActive = isActive
            )

            AppScreen.ROUTES -> RoutesScreen(
                account = accountSession,
                onOpenChat = { chat ->
                    chatToOpen = chat
                    currentScreen = AppScreen.CHAT
                },
                onOpenAccount = { currentScreen = AppScreen.ACCOUNT },
                initialMeetupId = meetupToOpenId,
                onInitialMeetupConsumed = { meetupToOpenId = null },
                isActive = isActive
            )

            AppScreen.CHAT -> ChatScreen(
                account = accountSession,
                initialChat = chatToOpen,
                onInitialChatConsumed = { chatToOpen = null },
                onOpenAccount = { currentScreen = AppScreen.ACCOUNT },
                onOpenMeetup = { meetupId ->
                    meetupToOpenId = meetupId
                    currentScreen = AppScreen.ROUTES
                },
                isActive = isActive
            )

            AppScreen.ACCOUNT -> AccountScreen(
                session = accountSession,
                platforms = platforms,
                isActive = isActive,
                bikeCount = bikes.size,
                onOpenBikes = { currentScreen = AppScreen.BIKES },
                initialErrorMessage = initialLoginError,
                oauthCallback = oauthCallback,
                onOauthCallbackConsumed = onOauthCallbackConsumed,
                onLogin = completeLogin,
                onSessionUpdated = updateSession,
                onProfilePhotoChanged = { profilePhotoUri = it },
                onLogout = logout
            )

            else -> Unit
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .zIndex(1f)
            ) {
                androidx.compose.runtime.key(currentScreen) {
                    AppTopBar(
                        session = accountSession,
                        accountSelected = mainDestinationFor(currentScreen) == AppScreen.ACCOUNT,
                        weatherState = weatherState,
                        onAccountClick = { currentScreen = AppScreen.ACCOUNT }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clipToBounds()
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = MAIN_SWIPE_DESTINATIONS.size,
                    userScrollEnabled = currentScreen in MAIN_SWIPE_DESTINATIONS
                ) { page ->
                    val destination = mainDestinationAt(page)
                    val isDestinationActive = currentScreen == destination &&
                        pagerState.settledPage == page
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val pageOffset = (
                                    (pagerState.currentPage - page) +
                                        pagerState.currentPageOffsetFraction
                                    ).absoluteValue.coerceIn(0f, 1f)
                                alpha = 1f - (pageOffset * 0.12f)
                                val pageScale = 1f - (pageOffset * 0.018f)
                                scaleX = pageScale
                                scaleY = pageScale
                            }
                    ) {
                        mainDestinationContent(destination, isDestinationActive)
                    }
                }

                if (currentScreen == AppScreen.BIKES) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(1f),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        BikesScreen(
                            account = accountSession,
                            bikes = bikes,
                            reminders = reminders,
                            bookings = bookings,
                            onOpenAccount = { currentScreen = AppScreen.ACCOUNT },
                            onBack = { currentScreen = AppScreen.ACCOUNT }
                        )
                    }
                }

                val notification = activeNotification
                InAppNotificationVisibility(
                    event = notification,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    onOpen = { event ->
                        chatToOpen = event.asChat()
                        currentScreen = AppScreen.CHAT
                        unreadMessages = 0
                        activeNotification = null
                    }
                ) { activeNotification = null }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(1f)
            ) {
                AppBottomBar(
                    destinations = destinations,
                    currentScreen = currentScreen,
                    unreadMessages = unreadMessages,
                    profilePhotoUri = profilePhotoUri,
                    profileDisplayName = accountSession?.username ?: accountSession?.email ?: "Perfil",
                    onNavigate = {
                        if (it == AppScreen.CHAT) unreadMessages = 0
                        currentScreen = it
                    }
                )
            }
        }
    }
}

@Composable
private fun SessionValidationScreen() {
    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.appbike_brand_icon),
                contentDescription = "APPBIKE",
                modifier = Modifier.size(BRAND_LOGO_SIZE),
                contentScale = ContentScale.Fit
            )
            CircularProgressIndicator(
                modifier = Modifier.size(30.dp),
                color = AppPrimaryBright,
                strokeWidth = 2.dp
            )
            Text(
                "Verificando sesión…",
                modifier = Modifier.padding(top = 14.dp),
                color = AppTextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun InAppNotificationVisibility(
    event: MessageNotificationEvent?,
    modifier: Modifier = Modifier,
    onOpen: (MessageNotificationEvent) -> Unit,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = event != null,
        modifier = modifier,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        event?.let { visibleEvent ->
            InAppMessageBanner(
                event = visibleEvent,
                onOpen = { onOpen(visibleEvent) },
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
private fun InAppMessageBanner(
    event: MessageNotificationEvent,
    onOpen: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 10.dp,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.ChatBubbleOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    event.senderName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    event.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Outlined.Close, contentDescription = "Cerrar aviso")
            }
        }
    }
}

@Composable
internal fun AppTopBar(
    session: AccountSession?,
    accountSelected: Boolean,
    weatherState: WeatherHeaderState = WeatherHeaderState.WaitingForLocation,
    @Suppress("UNUSED_PARAMETER") onAccountClick: () -> Unit = {}
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val compactCopy = LocalDensity.current.fontScale >= 1.6f || isLandscape
    var weatherForecastOpen by remember { mutableStateOf(false) }
    var weatherForecastState by remember {
        mutableStateOf<WeatherForecastState>(WeatherForecastState.Idle)
    }
    val currentWeather = (weatherState as? WeatherHeaderState.Ready)?.snapshot

    LaunchedEffect(
        weatherForecastOpen,
        weatherState::class,
        currentWeather?.latitude,
        currentWeather?.longitude
    ) {
        if (!weatherForecastOpen) {
            weatherForecastState = WeatherForecastState.Idle
            return@LaunchedEffect
        }
        val weather = currentWeather
        if (weather == null) {
            weatherForecastState = when (weatherState) {
                WeatherHeaderState.Loading -> WeatherForecastState.Loading
                WeatherHeaderState.WaitingForLocation -> WeatherForecastState.Unavailable(
                    "Concede permiso de ubicación para consultar el pronóstico."
                )
                is WeatherHeaderState.Unavailable -> WeatherForecastState.Unavailable(
                    weatherState.reason
                )
                is WeatherHeaderState.Ready -> WeatherForecastState.Unavailable(
                    "No se pudo obtener la ubicación para consultar el pronóstico."
                )
            }
            return@LaunchedEffect
        }
        weatherForecastState = WeatherForecastState.Loading
        val result = runSuspendCatching {
            withContext(Dispatchers.IO) {
                RemoteConnections.loadWeatherForecast(
                    latitude = weather.latitude,
                    longitude = weather.longitude
                )
            }
        }
        weatherForecastState = result.fold(
            onSuccess = WeatherForecastState::Ready,
            onFailure = {
                WeatherForecastState.Unavailable(RemoteConnections.userFriendlyError(it))
            }
        )
    }

    Surface(
        modifier = Modifier
            .statusBarsPadding(),
        color = AppBackgroundElevated,
        border = BorderStroke(1.dp, AppBorderSubtle),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 18.dp,
                    vertical = if (isLandscape) 4.dp else 10.dp
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clearAndSetSemantics {
                        contentDescription = if (compactCopy) {
                            "APPBIKE. RIDE, CONNECT"
                        } else {
                            "APPBIKE. RIDE, CONNECT, GROW"
                        }
                        heading()
                    }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(if (isLandscape) 32.dp else 36.dp),
                        shape = MaterialTheme.shapes.small,
                        color = AppPrimarySoft,
                        border = BorderStroke(1.dp, AppBorderActive.copy(alpha = 0.60f))
                    ) {
                        Image(
                            painter = painterResource(R.drawable.appbike_brand_icon),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = 1.18f
                                    scaleY = 1.18f
                                },
                            contentScale = ContentScale.Fit
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    if (isLandscape) {
                        Text(
                            text = "APP",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = AppTextPrimary
                        )
                        Text(
                            text = "BIKE",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = AppPrimaryBright
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = "RIDE  •  CONNECT",
                            style = MaterialTheme.typography.labelMedium,
                            color = AppPrimaryBright,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "APP",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = AppTextPrimary
                                )
                                Text(
                                    text = "BIKE",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = AppPrimaryBright
                                )
                            }
                            Spacer(Modifier.height(1.dp))
                            Text(
                                text = "RIDE  •  CONNECT",
                                style = MaterialTheme.typography.labelMedium,
                                color = AppPrimaryBright,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
            ) {
                WeatherStatusChip(
                    state = weatherState,
                    compact = compactCopy,
                    onClick = { weatherForecastOpen = !weatherForecastOpen }
                )
                DropdownMenu(
                    expanded = weatherForecastOpen,
                    onDismissRequest = { weatherForecastOpen = false },
                    modifier = Modifier.widthIn(min = 280.dp, max = 340.dp)
                ) {
                    WeatherForecastPanel(
                        state = weatherForecastState,
                        onClose = { weatherForecastOpen = false }
                    )
                }
            }
        }
    }
}

@Composable
internal fun AppBottomBar(
    destinations: List<MainDestination>,
    currentScreen: AppScreen,
    unreadMessages: Int,
    profilePhotoUri: String = "",
    profileDisplayName: String = "Perfil",
    onNavigate: (AppScreen) -> Unit
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    Surface(
        modifier = Modifier.navigationBarsPadding(),
        color = AppBackgroundElevated,
        border = BorderStroke(1.dp, AppBorderSubtle),
        shadowElevation = 14.dp,
        tonalElevation = 0.dp
    ) {
        NavigationBar(
            modifier = Modifier.height(
                if (isLandscape) AppSizes.NavigationBarLandscape else AppSizes.NavigationBarPortrait
            ),
            containerColor = AppBackgroundElevated,
            tonalElevation = 0.dp
        ) {
            destinations.forEach { destination ->
                val selected = mainDestinationFor(currentScreen) == destination.screen
                val iconScale by animateFloatAsState(
                    targetValue = when {
                        selected && destination.emphasized -> 1.12f
                        selected -> 1.07f
                        else -> 1f
                    },
                    animationSpec = tween(durationMillis = AppMotion.Standard),
                    label = "${destination.label} icon scale"
                )

                NavigationBarItem(
                    modifier = Modifier.semantics {
                        contentDescription = destination.label
                    },
                    selected = selected,
                    onClick = { onNavigate(destination.screen) },
                    icon = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                modifier = Modifier
                                    .width(if (selected) 26.dp else 10.dp)
                                    .height(3.dp),
                                shape = MaterialTheme.shapes.extraLarge,
                                color = if (selected) AppPrimaryBright else Color.Transparent
                            ) {}
                            Spacer(Modifier.height(5.dp))
                            Surface(
                                modifier = Modifier
                                    .size(
                                        width = when {
                                            isLandscape -> 48.dp
                                            destination.emphasized -> 58.dp
                                            else -> 52.dp
                                        },
                                        height = if (isLandscape) 36.dp else 40.dp
                                    )
                                    .graphicsLayer {
                                        scaleX = iconScale
                                        scaleY = iconScale
                                    },
                                shape = MaterialTheme.shapes.large,
                                color = if (selected) AppPrimarySoft else Color.Transparent,
                                border = if (selected) {
                                    BorderStroke(1.dp, AppBorderActive.copy(alpha = 0.48f))
                                } else {
                                    null
                                }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    BadgedBox(
                                        badge = {
                                            if (destination.screen == AppScreen.CHAT && unreadMessages > 0) {
                                                Badge {
                                                    Text(unreadMessages.coerceAtMost(9).toString())
                                                }
                                            }
                                        }
                                    ) {
                                        if (destination.screen == AppScreen.ACCOUNT) {
                                            ProfileAvatar(
                                                photoUri = profilePhotoUri,
                                                displayName = profileDisplayName,
                                                modifier = Modifier.size(30.dp)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = destination.icon,
                                                contentDescription = null,
                                                modifier = Modifier.size(
                                                    if (destination.emphasized) {
                                                        AppSizes.NavigationIconEmphasized
                                                    } else {
                                                        AppSizes.NavigationIcon
                                                    }
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    label = if (!isLandscape && selected) {
                        {
                            Text(
                                text = destination.label,
                                maxLines = 1,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        null
                    },
                    alwaysShowLabel = false,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AppPrimaryBright,
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = AppTextSecondary,
                        disabledIconColor = AppTextSecondary.copy(alpha = 0.38f)
                    )
                )
            }
        }
    }
}
