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
import androidx.compose.ui.Alignment
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

enum class AppScreen {
    HOME,
    BIKES,
    MAINTENANCE,
    SERVICE,
    MARKETPLACE,
    CREATE_PUBLICATION,
    ROUTES,
    CHAT,
    SYNC
}

data class AppFeature(
    val title: String,
    val description: String,
    val screen: AppScreen
)

data class Bike(
    val name: String,
    val brand: String,
    val model: String,
    val type: String,
    val year: String,
    val serialNumber: String,
    val mediaDescription: String,
    val isStolen: Boolean = false
)

data class ProductPublication(
    val title: String,
    val price: String,
    val category: String,
    val condition: String,
    val seller: String,
    val description: String,
    val mediaDescription: String
)

data class MaintenanceReminder(
    val bike: String,
    val component: String,
    val date: String,
    val notes: String
)

data class ServiceBooking(
    val workshop: String,
    val service: String,
    val date: String,
    val contact: String
)

data class RoutePost(
    val name: String,
    val zone: String,
    val startPoint: String,
    val endPoint: String,
    val distanceKm: String,
    val estimatedTime: String,
    val difficulty: String,
    val safetyNote: String
)

data class RideMeetup(
    val title: String,
    val routeName: String,
    val meetingPoint: String,
    val dateTime: String,
    val organizer: String,
    val level: String,
    val maxRiders: String,
    val notes: String,
    val participants: List<String> = emptyList()
)

data class ChatMessage(
    val sender: String,
    val message: String
)

data class SyncPlatform(
    val name: String,
    val description: String,
    val connected: Boolean
)

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
            onBack = { currentScreen = AppScreen.HOME }
        )

        AppScreen.MAINTENANCE -> MaintenanceScreen(
            reminders = reminders,
            onBack = { currentScreen = AppScreen.HOME }
        )

        AppScreen.SERVICE -> ServiceScreen(
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
                products.add(0, it)
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
        AppFeature("Mis bicicletas", "Registro, número de serie, multimedia y reporte de robo.", AppScreen.BIKES),
        AppFeature("Alertas de mantención", "Recordatorios para cadena, frenos, neumáticos y revisiones.", AppScreen.MAINTENANCE),
        AppFeature("Agendar servicio", "Reserva mantenciones con talleres certificados.", AppScreen.SERVICE),
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

@Composable
fun ScreenContainer(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(onClick = onBack) {
                Text("← Volver")
            }

            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
fun BikesScreen(
    bikes: MutableList<Bike>,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var serialNumber by remember { mutableStateOf("") }
    var mediaDescription by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }

    val stolenBikes = bikes.filter {
        it.isStolen &&
                (
                        it.name.contains(search, true) ||
                                it.brand.contains(search, true) ||
                                it.model.contains(search, true) ||
                                it.serialNumber.contains(search, true)
                        )
    }

    ScreenContainer("Mis bicicletas", onBack) {
        Text("Registrar bicicleta", fontWeight = FontWeight.Bold)

        AppInput("Nombre de la bicicleta", name) { name = it }
        AppInput("Marca", brand) { brand = it }
        AppInput("Modelo", model) { model = it }
        AppInput("Tipo: MTB, Ruta, Gravel, Urbana", type) { type = it }
        AppInput("Año", year) { year = it }
        AppInput("Número de serie", serialNumber) { serialNumber = it }
        AppInput("Foto o video: describe el archivo seleccionado", mediaDescription) { mediaDescription = it }

        Button(
            onClick = {
                bikes.add(
                    Bike(
                        name = name,
                        brand = brand,
                        model = model,
                        type = type,
                        year = year,
                        serialNumber = serialNumber,
                        mediaDescription = mediaDescription
                    )
                )

                name = ""
                brand = ""
                model = ""
                type = ""
                year = ""
                serialNumber = ""
                mediaDescription = ""
            },
            enabled = name.isNotBlank() && brand.isNotBlank() && model.isNotBlank() && serialNumber.isNotBlank()
        ) {
            Text("Guardar bicicleta")
        }

        Divider()

        Text("Bicicletas registradas", fontWeight = FontWeight.Bold)

        if (bikes.isEmpty()) {
            Text("Todavía no tienes bicicletas registradas.")
        } else {
            bikes.forEachIndexed { index, bike ->
                CardItem(
                    title = bike.name,
                    subtitle = "${bike.brand} · ${bike.model} · ${bike.type}",
                    body = "Año: ${bike.year}\nSerie: ${bike.serialNumber}\nMultimedia: ${bike.mediaDescription}\nEstado: ${if (bike.isStolen) "Robada" else "Registrada"}"
                )

                Button(
                    onClick = {
                        bikes[index] = bike.copy(isStolen = !bike.isStolen)
                    }
                ) {
                    Text(if (bike.isStolen) "Marcar como recuperada" else "Reportar robo")
                }
            }
        }

        Divider()

        Text("Alerta comunitaria por robo", fontWeight = FontWeight.Bold)
        AppInput("Buscar por serie, marca, modelo o nombre", search) { search = it }

        if (stolenBikes.isEmpty()) {
            Text("No hay bicicletas reportadas como robadas.")
        } else {
            stolenBikes.forEach { bike ->
                CardItem(
                    title = "Bicicleta robada",
                    subtitle = "${bike.brand} · ${bike.model}",
                    body = "Nombre: ${bike.name}\nSerie: ${bike.serialNumber}\nMultimedia: ${bike.mediaDescription}"
                )
            }
        }
    }
}

@Composable
fun MarketplaceScreen(
    products: MutableList<ProductPublication>,
    onCreatePublication: () -> Unit,
    onBack: () -> Unit
) {
    var search by remember { mutableStateOf("") }

    val filteredProducts = products.filter {
        it.title.contains(search, true) ||
                it.category.contains(search, true) ||
                it.seller.contains(search, true) ||
                it.description.contains(search, true)
    }

    ScreenContainer("Marketplace", onBack) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text("Buscar productos") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            Button(onClick = onCreatePublication) {
                Text("Crear")
            }
        }

        Text("Publicaciones disponibles", fontWeight = FontWeight.Bold)

        filteredProducts.forEach { product ->
            CardItem(
                title = product.title,
                subtitle = "${product.price} · ${product.category} · ${product.condition}",
                body = "Vendedor: ${product.seller}\n${product.description}\nMultimedia: ${product.mediaDescription}"
            )

            Button(onClick = {}) {
                Text("Contactar vendedor")
            }
        }

        if (filteredProducts.isEmpty()) {
            Text("No se encontraron productos.")
        }
    }
}

@Composable
fun CreatePublicationScreen(
    onPublish: (ProductPublication) -> Unit,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var condition by remember { mutableStateOf("") }
    var seller by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var mediaDescription by remember { mutableStateOf("") }

    ScreenContainer("Crear publicación", onBack) {
        Text("Completa los datos del producto")

        AppInput("Foto o video: describe el archivo seleccionado", mediaDescription) { mediaDescription = it }
        AppInput("Título del producto", title) { title = it }
        AppInput("Precio", price) { price = it }
        AppInput("Categoría: bicicleta, repuesto, accesorio", category) { category = it }
        AppInput("Estado: nuevo o usado", condition) { condition = it }
        AppInput("Tu nombre o tienda", seller) { seller = it }
        AppInput("Descripción", description) { description = it }

        Button(
            onClick = {
                onPublish(
                    ProductPublication(
                        title = title,
                        price = price,
                        category = category,
                        condition = condition,
                        seller = seller,
                        description = description,
                        mediaDescription = mediaDescription
                    )
                )
            },
            enabled = title.isNotBlank() && price.isNotBlank() && seller.isNotBlank()
        ) {
            Text("Publicar producto")
        }
    }
}

@Composable
fun RoutesScreen(
    routes: MutableList<RoutePost>,
    meetups: MutableList<RideMeetup>,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    ScreenContainer("Mapa y juntas rider", onBack) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Mapa") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Juntas") })
        }

        if (selectedTab == 0) {
            MapSection(routes)
        } else {
            MeetupsSection(routes, meetups)
        }
    }
}

@Composable
fun MapSection(routes: MutableList<RoutePost>) {
    var name by remember { mutableStateOf("") }
    var zone by remember { mutableStateOf("") }
    var startPoint by remember { mutableStateOf("") }
    var endPoint by remember { mutableStateOf("") }
    var distanceKm by remember { mutableStateOf("") }
    var estimatedTime by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf("") }
    var safetyNote by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var selectedRoute by remember { mutableStateOf<RoutePost?>(routes.firstOrNull()) }

    val filteredRoutes = routes.filter {
        it.name.contains(search, true) ||
                it.zone.contains(search, true) ||
                it.startPoint.contains(search, true) ||
                it.endPoint.contains(search, true) ||
                it.difficulty.contains(search, true)
    }

    Text("Mapa colaborativo", fontWeight = FontWeight.Bold)
    AppInput("Buscar ruta", search) { search = it }

    CardItem(
        title = "Vista de mapa",
        subtitle = selectedRoute?.name ?: "Sin ruta seleccionada",
        body = selectedRoute?.let {
            "Zona: ${it.zone}\nSalida: ${it.startPoint}\nLlegada: ${it.endPoint}\nDistancia: ${it.distanceKm} km\nTiempo: ${it.estimatedTime}\nDificultad: ${it.difficulty}\nSeguridad: ${it.safetyNote}"
        } ?: "Selecciona o publica una ruta para verla aquí."
    )

    Divider()

    Text("Agregar ruta segura", fontWeight = FontWeight.Bold)

    AppInput("Nombre de la ruta", name) { name = it }
    AppInput("Zona o ciudad", zone) { zone = it }
    AppInput("Punto de salida", startPoint) { startPoint = it }
    AppInput("Punto de llegada", endPoint) { endPoint = it }
    AppInput("Distancia aproximada en km", distanceKm) { distanceKm = it }
    AppInput("Tiempo estimado", estimatedTime) { estimatedTime = it }
    AppInput("Dificultad", difficulty) { difficulty = it }
    AppInput("Comentario de seguridad", safetyNote) { safetyNote = it }

    Button(
        onClick = {
            val route = RoutePost(
                name = name,
                zone = zone,
                startPoint = startPoint,
                endPoint = endPoint,
                distanceKm = distanceKm,
                estimatedTime = estimatedTime,
                difficulty = difficulty,
                safetyNote = safetyNote
            )

            routes.add(0, route)
            selectedRoute = route

            name = ""
            zone = ""
            startPoint = ""
            endPoint = ""
            distanceKm = ""
            estimatedTime = ""
            difficulty = ""
            safetyNote = ""
        },
        enabled = name.isNotBlank() && zone.isNotBlank() && startPoint.isNotBlank() && endPoint.isNotBlank()
    ) {
        Text("Publicar ruta")
    }

    Divider()

    Text("Rutas disponibles", fontWeight = FontWeight.Bold)

    filteredRoutes.forEach { route ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { selectedRoute = route },
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(route.name, fontWeight = FontWeight.Bold)
                Text("${route.zone} · ${route.difficulty}")
                Text("Salida: ${route.startPoint}")
                Text("Llegada: ${route.endPoint}")
            }
        }
    }
}

@Composable
fun MeetupsSection(
    routes: List<RoutePost>,
    meetups: MutableList<RideMeetup>
) {
    var title by remember { mutableStateOf("") }
    var routeName by remember { mutableStateOf("") }
    var meetingPoint by remember { mutableStateOf("") }
    var dateTime by remember { mutableStateOf("") }
    var organizer by remember { mutableStateOf("") }
    var level by remember { mutableStateOf("") }
    var maxRiders by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    Text("Organización de juntas rider", fontWeight = FontWeight.Bold)
    Text("Crea salidas grupales, cupos, nivel y punto de encuentro.")

    AppInput("Título de la junta", title) { title = it }
    AppInput("Ruta asociada", routeName) { routeName = it }
    AppInput("Punto de encuentro", meetingPoint) { meetingPoint = it }
    AppInput("Fecha y hora", dateTime) { dateTime = it }
    AppInput("Organizador", organizer) { organizer = it }
    AppInput("Nivel", level) { level = it }
    AppInput("Máximo de riders", maxRiders) { maxRiders = it }
    AppInput("Notas", notes) { notes = it }

    if (routes.isNotEmpty()) {
        Text("Rutas disponibles: ${routes.joinToString { it.name }}")
    }

    Button(
        onClick = {
            val organizerName = organizer.ifBlank { "Organizador" }

            meetups.add(
                0,
                RideMeetup(
                    title = title,
                    routeName = routeName,
                    meetingPoint = meetingPoint,
                    dateTime = dateTime,
                    organizer = organizerName,
                    level = level,
                    maxRiders = maxRiders,
                    notes = notes,
                    participants = listOf(organizerName)
                )
            )

            title = ""
            routeName = ""
            meetingPoint = ""
            dateTime = ""
            organizer = ""
            level = ""
            maxRiders = ""
            notes = ""
        },
        enabled = title.isNotBlank() && routeName.isNotBlank() && meetingPoint.isNotBlank()
    ) {
        Text("Crear junta")
    }

    Divider()

    Text("Juntas disponibles", fontWeight = FontWeight.Bold)

    meetups.forEachIndexed { index, meetup ->
        val joined = meetup.participants.contains("Yo")
        val max = meetup.maxRiders.toIntOrNull()
        val available = max == null || meetup.participants.size < max

        CardItem(
            title = meetup.title,
            subtitle = "${meetup.routeName} · ${meetup.level}",
            body = "Punto: ${meetup.meetingPoint}\nFecha: ${meetup.dateTime}\nOrganizador: ${meetup.organizer}\nCupos: ${meetup.participants.size}/${meetup.maxRiders}\nNotas: ${meetup.notes}\nRiders: ${meetup.participants.joinToString()}"
        )

        Button(
            onClick = {
                meetups[index] = if (joined) {
                    meetup.copy(participants = meetup.participants - "Yo")
                } else {
                    meetup.copy(participants = meetup.participants + "Yo")
                }
            },
            enabled = joined || available
        ) {
            Text(if (joined) "Salir de la junta" else if (available) "Unirme a la junta" else "Cupos llenos")
        }
    }
}

@Composable
fun MaintenanceScreen(
    reminders: MutableList<MaintenanceReminder>,
    onBack: () -> Unit
) {
    var bike by remember { mutableStateOf("") }
    var component by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    ScreenContainer("Alertas de mantención", onBack) {
        AppInput("Bicicleta", bike) { bike = it }
        AppInput("Componente", component) { component = it }
        AppInput("Fecha o kilometraje", date) { date = it }
        AppInput("Notas", notes) { notes = it }

        Button(
            onClick = {
                reminders.add(MaintenanceReminder(bike, component, date, notes))
                bike = ""
                component = ""
                date = ""
                notes = ""
            },
            enabled = bike.isNotBlank() && component.isNotBlank()
        ) {
            Text("Crear alerta")
        }

        reminders.forEach {
            CardItem(it.component, it.bike, "Cuándo: ${it.date}\nNotas: ${it.notes}")
        }
    }
}

@Composable
fun ServiceScreen(
    bookings: MutableList<ServiceBooking>,
    onBack: () -> Unit
) {
    var workshop by remember { mutableStateOf("") }
    var service by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }

    ScreenContainer("Agendar servicio", onBack) {
        AppInput("Taller", workshop) { workshop = it }
        AppInput("Servicio requerido", service) { service = it }
        AppInput("Fecha y hora", date) { date = it }
        AppInput("Teléfono o correo", contact) { contact = it }

        Button(
            onClick = {
                bookings.add(ServiceBooking(workshop, service, date, contact))
                workshop = ""
                service = ""
                date = ""
                contact = ""
            },
            enabled = workshop.isNotBlank() && service.isNotBlank()
        ) {
            Text("Agendar")
        }

        bookings.forEach {
            CardItem(it.service, it.workshop, "Fecha: ${it.date}\nContacto: ${it.contact}")
        }
    }
}

@Composable
fun ChatScreen(
    messages: MutableList<ChatMessage>,
    onBack: () -> Unit
) {
    var message by remember { mutableStateOf("") }

    ScreenContainer("Chat", onBack) {
        messages.forEach {
            CardItem(it.sender, "", it.message)
        }

        AppInput("Escribe tu mensaje", message) { message = it }

        Button(
            onClick = {
                messages.add(ChatMessage("Yo", message))
                message = ""
            },
            enabled = message.isNotBlank()
        ) {
            Text("Enviar")
        }
    }
}

@Composable
fun SyncScreen(
    platforms: MutableList<SyncPlatform>,
    onBack: () -> Unit
) {
    ScreenContainer("Sincronización deportiva", onBack) {
        platforms.forEachIndexed { index, platform ->
            CardItem(
                title = platform.name,
                subtitle = if (platform.connected) "Estado: conectado" else "Estado: desconectado",
                body = platform.description
            )

            Button(
                onClick = {
                    platforms[index] = platform.copy(connected = !platform.connected)
                }
            ) {
                Text(if (platform.connected) "Desconectar" else "Conectar")
            }
        }
    }
}

@Composable
fun CardItem(
    title: String,
    subtitle: String,
    body: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            if (subtitle.isNotBlank()) {
                Text(subtitle)
            }

            if (body.isNotBlank()) {
                Text(body)
            }
        }
    }
}

@Composable
fun AppInput(
    label: String,
    value: String,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth()
    )
}

@Preview(showBackground = true)
@Composable
fun AppBikePreview() {
    APPbikeTheme {
        AppBikeApp()
    }
}