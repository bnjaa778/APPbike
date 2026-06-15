package com.example.appbike

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsBike
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Storefront
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.appbike.ui.theme.APPbikeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            APPbikeTheme(dynamicColor = false) {
                AppBikeApp()
            }
        }
    }
}

private data class MainDestination(
    val label: String,
    val screen: AppScreen,
    val icon: ImageVector
)

@Composable
fun AppBikeApp() {
    var currentScreen by remember { mutableStateOf(AppScreen.ROUTES) }
    val context = LocalContext.current
    var accountSession by remember { mutableStateOf(AccountStore.loadSession(context)) }

    val bikes = remember { mutableStateListOf<Bike>() }
    val reminders = remember { mutableStateListOf<MaintenanceReminder>() }
    val bookings = remember { mutableStateListOf<ServiceBooking>() }
    val products = remember {
        mutableStateListOf(
            ProductPublication(
                title = "Casco MTB Specialized",
                price = "$35.000",
                category = "Seguridad",
                condition = "Usado",
                seller = "Carlos",
                description = "Casco en buen estado, talla M.",
                mediaDescription = "Foto del casco"
            ),
            ProductPublication(
                title = "Bicicleta Trek Marlin 5",
                price = "$480.000",
                category = "Bicicletas",
                condition = "Usado",
                seller = "Daniela",
                description = "Aro 29, frenos hidráulicos.",
                mediaDescription = "Video demostrativo"
            )
        )
    }
    val routes = remember {
        mutableStateListOf(
            RoutePost(
                name = "Ruta Costanera Segura",
                zone = "Santiago Centro",
                startPoint = "Metro Baquedano",
                endPoint = "Parque Bicentenario",
                distanceKm = "12",
                estimatedTime = "45 min",
                difficulty = "Media",
                safetyNote = "Buena iluminación y ciclovía en gran parte del trayecto."
            )
        )
    }
    val meetups = remember {
        mutableStateListOf(
            RideMeetup(
                title = "Junta MTB sábado",
                routeName = "Ruta Costanera Segura",
                meetingPoint = "Metro Baquedano",
                dateTime = "Sábado 09:00",
                organizer = "Matías",
                level = "Intermedio",
                maxRiders = "8",
                notes = "Llevar casco, agua y luces.",
                participants = listOf("Matías")
            )
        )
    }
    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                sender = "Técnico APPbike",
                message = "Hola, puedes escribir tu duda técnica o consultar por una mantención."
            )
        )
    }
    val platforms = remember {
        mutableStateListOf(
            SyncPlatform("Strava", "Actividades, rutas y entrenamientos.", false),
            SyncPlatform("Garmin", "Relojes, ciclocomputadores y sensores.", false),
            SyncPlatform("Wahoo", "Entrenamientos y dispositivos deportivos.", false)
        )
    }

    val destinations = remember {
        listOf(
            MainDestination("Mapas", AppScreen.ROUTES, Icons.Outlined.Map),
            MainDestination(
                "Bicicletas",
                AppScreen.BIKES,
                Icons.AutoMirrored.Outlined.DirectionsBike
            ),
            MainDestination("Marketplace", AppScreen.MARKETPLACE, Icons.Outlined.Storefront),
            MainDestination("Chat", AppScreen.CHAT, Icons.Outlined.ChatBubbleOutline)
        )
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
                unreadMessages = 0,
                onNavigate = { currentScreen = it }
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
                    onLogin = { session ->
                        AccountStore.saveSession(context, session)
                        accountSession = session
                    },
                    onLogout = {
                        AccountStore.clearSession(context)
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
                    MarketplaceScreen(accountSession)

                AppScreen.ROUTES, AppScreen.HOME -> RoutesScreen(accountSession)

                AppScreen.CHAT -> ChatScreen(accountSession)
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
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "APPBIKE",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    text = "Muévete. Conecta. Disfruta.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = MaterialTheme.shapes.large,
                color = if (accountSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
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
                            contentDescription = "Cuenta y sincronización",
                            tint = MaterialTheme.colorScheme.onSurface
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
    Surface(shadowElevation = 12.dp) {
        NavigationBar(
            modifier = Modifier.height(76.dp),
            containerColor = MaterialTheme.colorScheme.surface
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
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    label = { Text(destination.label, maxLines = 1) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}
