package com.example.appbike

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.appbike.ui.theme.APPbikeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            APPbikeTheme {
                AppBikeApp()
            }
        }
    }
}

@Composable
fun AppBikeApp() {
    var currentScreen by remember { mutableStateOf(AppScreen.HOME) }

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
            SyncPlatform("Strava", "Sincroniza actividades, rutas y entrenamientos.", false),
            SyncPlatform("Garmin", "Conecta relojes, ciclocomputadores y monitores deportivos.", false),
            SyncPlatform("Wahoo", "Importa datos de entrenamiento y sensores.", false)
        )
    }

    when (currentScreen) {
        AppScreen.HOME -> HomeScreen { currentScreen = it }

        AppScreen.BIKES -> BikesScreen(
            bikes = bikes,
            reminders = reminders,
            bookings = bookings,
            onBack = { currentScreen = AppScreen.HOME }
        )

        AppScreen.MARKETPLACE -> MarketplaceScreen(
            products = products,
            onCreatePublication = { currentScreen = AppScreen.CREATE_PUBLICATION },
            onBack = { currentScreen = AppScreen.HOME }
        )

        AppScreen.CREATE_PUBLICATION -> CreatePublicationScreen(
            onPublish = {
                products.add(0, RemoteConnections.publishProduct(it))
                currentScreen = AppScreen.MARKETPLACE
            },
            onBack = { currentScreen = AppScreen.MARKETPLACE }
        )

        AppScreen.ROUTES -> RoutesScreen(
            routes = routes,
            meetups = meetups,
            onBack = { currentScreen = AppScreen.HOME }
        )

        AppScreen.CHAT -> ChatScreen(
            messages = messages,
            onBack = { currentScreen = AppScreen.HOME }
        )

        AppScreen.SYNC -> SyncScreen(
            platforms = platforms,
            onBack = { currentScreen = AppScreen.HOME }
        )
    }
}

@Composable
fun HomeScreen(onNavigate: (AppScreen) -> Unit) {
    val features = listOf(
        AppFeature("Mis bicicletas", "Tus bicicletas, mantenciones y servicios en un solo lugar.", AppScreen.BIKES),
        AppFeature("Marketplace", "Compra y vende productos con contenido multimedia.", AppScreen.MARKETPLACE),
        AppFeature("Mapa y juntas rider", "Rutas seguras y organización de salidas grupales.", AppScreen.ROUTES),
        AppFeature("Chat", "Comunicación con técnicos, talleres o vendedores.", AppScreen.CHAT),
        AppFeature("Sincronización deportiva", "Conexión con Strava, Garmin y otros dispositivos.", AppScreen.SYNC)
    )

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("APPbike", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("App integral para ciclistas", style = MaterialTheme.typography.titleMedium)

            features.forEach { feature ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(feature.screen) },
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(feature.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(feature.description)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AppBikePreview() {
    APPbikeTheme {
        AppBikeApp()
    }
}
