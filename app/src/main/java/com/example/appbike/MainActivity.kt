package com.example.appbike

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.appbike.ui.theme.APPbikeTheme
import com.example.appbike.ui.theme.AppBackgroundElevated
import com.example.appbike.ui.theme.AppPrimaryBright
import com.example.appbike.ui.theme.AppPrimarySoft
import com.example.appbike.ui.theme.AppSurfaceElevated
import com.example.appbike.ui.theme.AppTextPrimary
import com.example.appbike.ui.theme.AppTextSecondary
import kotlinx.coroutines.delay

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
        enableEdgeToEdge()

        setContent {
            APPbikeTheme(dynamicColor = false) {
                AppBikeApp(
                    oauthCallback = sportsOauthCallback.value,
                    onOauthCallbackConsumed = { sportsOauthCallback.value = null },
                    notificationTarget = notificationChatTarget.value,
                    onNotificationTargetConsumed = { notificationChatTarget.value = null }
                )
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
        prefs.edit().putBoolean("notifications_listener_v2_asked", true).apply()
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
        val type = runCatching {
            ChatType.valueOf(
                intent.getStringExtra(ChatNotificationCenter.EXTRA_CHAT_TYPE).orEmpty()
            )
        }.getOrDefault(ChatType.SOCIAL)
        notificationChatTarget.value = NotificationChatTarget(
            chatId = chatId,
            type = type,
            title = intent.getStringExtra(ChatNotificationCenter.EXTRA_CHAT_TITLE).orEmpty()
        )
        intent.removeExtra(ChatNotificationCenter.EXTRA_CHAT_ID)
    }
}

data class NotificationChatTarget(
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

private data class MainDestination(
    val label: String,
    val screen: AppScreen,
    val iconAsset: String
)

@Composable
fun AppBikeApp(
    oauthCallback: SportsOAuthCallback? = null,
    onOauthCallbackConsumed: () -> Unit = {},
    notificationTarget: NotificationChatTarget? = null,
    onNotificationTargetConsumed: () -> Unit = {}
) {
    var currentScreen by remember { mutableStateOf(AppScreen.ROUTES) }
    val context = LocalContext.current
    var accountSession by remember {
        mutableStateOf(AccountStore.loadSession(context).also(RemoteConnections::setSession))
    }

    val bikes = remember { mutableStateListOf<Bike>() }
    val reminders = remember { mutableStateListOf<MaintenanceReminder>() }
    val bookings = remember { mutableStateListOf<ServiceBooking>() }
    var chatToOpen by remember { mutableStateOf<UserChat?>(null) }
    val pendingNotifications = remember { mutableStateListOf<MessageNotificationEvent>() }
    var activeNotification by remember { mutableStateOf<MessageNotificationEvent?>(null) }
    var unreadMessages by remember { mutableStateOf(0) }
    val platforms = remember {
        mutableStateListOf(
            SyncPlatform("strava", "Strava", "Actividades, rutas y entrenamientos.", false),
            SyncPlatform("garmin", "Garmin", "Relojes, ciclocomputadores y sensores.", false),
            SyncPlatform("wahoo", "Wahoo", "Entrenamientos y dispositivos deportivos.", false)
        )
    }

    val destinations = remember {
        listOf(
            MainDestination("Mapas", AppScreen.ROUTES, "navigation/mapas-logo.png"),
            MainDestination(
                "Bicicletas",
                AppScreen.BIKES,
                "navigation/bicicletas-logo.png"
            ),
            MainDestination("Marketplace", AppScreen.MARKETPLACE, "navigation/marketplace-logo.png"),
            MainDestination("Chat", AppScreen.CHAT, "navigation/chat-logo.png")
        )
    }

    LaunchedEffect(accountSession?.userId, accountSession?.username) {
        val session = accountSession ?: return@LaunchedEffect
        (context as? MainActivity)?.requestNotificationPermissionOnce()
        if (session.username.isNullOrBlank()) {
            currentScreen = AppScreen.ACCOUNT
        }
    }

    LaunchedEffect(accountSession?.userId) {
        val session = accountSession
        if (session == null) {
            ChatNotificationCenter.stopListener(context)
            ChatNotificationCenter.cancelBackgroundChecks(context)
            pendingNotifications.clear()
            activeNotification = null
            unreadMessages = 0
            return@LaunchedEffect
        }
        ChatNotificationCenter.startListener(context)
        ChatNotificationCenter.scheduleBackgroundChecks(context)
    }

    LaunchedEffect(Unit) {
        ChatNotificationEventBus.events.collect { event ->
            pendingNotifications.add(event)
            unreadMessages += 1
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

    LaunchedEffect(notificationTarget?.chatId) {
        val target = notificationTarget ?: return@LaunchedEffect
        chatToOpen = target.asChat()
        currentScreen = AppScreen.CHAT
        unreadMessages = 0
        onNotificationTargetConsumed()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                session = accountSession,
                accountSelected = currentScreen == AppScreen.ACCOUNT,
                onAccountClick = { currentScreen = AppScreen.ACCOUNT }
            )
        },
        bottomBar = {
            AppBottomBar(
                destinations = destinations,
                currentScreen = currentScreen,
                unreadMessages = unreadMessages,
                onNavigate = {
                    if (it == AppScreen.CHAT) unreadMessages = 0
                    currentScreen = it
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                AppScreen.ACCOUNT, AppScreen.SYNC -> AccountScreen(
                    session = accountSession,
                    platforms = platforms,
                    oauthCallback = oauthCallback,
                    onOauthCallbackConsumed = onOauthCallbackConsumed,
                    onLogin = { session ->
                        AccountStore.saveSession(context, session)
                        RemoteConnections.setSession(session)
                        accountSession = session
                    },
                    onSessionUpdated = { session ->
                        AccountStore.saveSession(context, session)
                        RemoteConnections.setSession(session)
                        accountSession = session
                    },
                    onLogout = {
                        ChatNotificationCenter.stopListener(context)
                        ChatNotificationCenter.cancelBackgroundChecks(context)
                        AccountStore.clearSession(context)
                        RemoteConnections.setSession(null)
                        accountSession = null
                        bikes.clear()
                        reminders.clear()
                        bookings.clear()
                    }
                )

                AppScreen.BIKES -> BikesScreen(
                    account = accountSession,
                    bikes = bikes,
                    reminders = reminders,
                    bookings = bookings,
                    onOpenAccount = { currentScreen = AppScreen.ACCOUNT },
                    onBack = null
                )

                AppScreen.MARKETPLACE, AppScreen.CREATE_PUBLICATION ->
                    MarketplaceScreen(accountSession) { chat ->
                        chatToOpen = chat
                        currentScreen = AppScreen.CHAT
                    }

                AppScreen.ROUTES, AppScreen.HOME -> RoutesScreen(accountSession) { chat ->
                    chatToOpen = chat
                    currentScreen = AppScreen.CHAT
                }

                AppScreen.CHAT -> ChatScreen(
                    accountSession,
                    initialChat = chatToOpen,
                    onInitialChatConsumed = { chatToOpen = null }
                )
            }

            val notification = activeNotification
            AnimatedVisibility(
                visible = notification != null,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
            ) {
                notification?.let { event ->
                    InAppMessageBanner(
                        event = event,
                        onOpen = {
                            chatToOpen = event.asChat()
                            currentScreen = AppScreen.CHAT
                            unreadMessages = 0
                            activeNotification = null
                        },
                        onDismiss = { activeNotification = null }
                    )
                }
            }
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
private fun AppTopBar(
    session: AccountSession?,
    accountSelected: Boolean,
    onAccountClick: () -> Unit
) {
    Surface(
        modifier = Modifier.statusBarsPadding(),
        color = AppBackgroundElevated,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "APPBIKE",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = AppTextPrimary
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    text = "Rendimiento. Ruta. Comunidad.",
                    style = MaterialTheme.typography.labelMedium,
                    color = AppPrimaryBright
                )
            }

            Surface(
                shape = MaterialTheme.shapes.large,
                color = if (accountSelected) {
                    AppPrimarySoft
                } else {
                    AppSurfaceElevated
                }
            ) {
                IconButton(onClick = onAccountClick) {
                    BadgedBox(
                        badge = {
                            if (session != null) {
                                Badge(containerColor = MaterialTheme.colorScheme.primary)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PersonOutline,
                            contentDescription = "Cuenta y sincronizacion",
                            tint = if (accountSelected) {
                                AppPrimaryBright
                            } else {
                                AppTextPrimary
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppBottomBar(
    destinations: List<MainDestination>,
    currentScreen: AppScreen,
    unreadMessages: Int,
    onNavigate: (AppScreen) -> Unit
) {
    Surface(
        modifier = Modifier.navigationBarsPadding(),
        color = AppBackgroundElevated,
        shadowElevation = 8.dp,
        tonalElevation = 0.dp
    ) {
        NavigationBar(
            containerColor = AppBackgroundElevated,
            tonalElevation = 0.dp
        ) {
            destinations.forEach { destination ->
                val selected = currentScreen == destination.screen ||
                    (destination.screen == AppScreen.MARKETPLACE &&
                        currentScreen == AppScreen.CREATE_PUBLICATION)

                NavigationBarItem(
                    selected = selected,
                    onClick = { onNavigate(destination.screen) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (destination.screen == AppScreen.CHAT && unreadMessages > 0) {
                                    Badge { Text(unreadMessages.coerceAtMost(9).toString()) }
                                }
                            }
                        ) {
                            NavigationLogoIcon(
                                assetPath = destination.iconAsset
                            )
                        }
                    },
                    label = { Text(destination.label, maxLines = 1) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AppTextPrimary,
                        selectedTextColor = AppPrimaryBright,
                        indicatorColor = AppPrimarySoft,
                        unselectedIconColor = AppTextSecondary,
                        unselectedTextColor = AppTextSecondary
                    )
                )
            }
        }
    }
}

@Composable
private fun NavigationLogoIcon(assetPath: String) {
    val context = LocalContext.current
    val image = remember(assetPath) {
        context.assets.open(assetPath).use(BitmapFactory::decodeStream).asImageBitmap()
    }

    Image(
        bitmap = image,
        contentDescription = null,
        modifier = Modifier.size(28.dp),
        contentScale = ContentScale.Fit
    )
}
