package com.example.appbike

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsBike
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.example.appbike.ui.theme.AppBackgroundElevated
import com.example.appbike.ui.theme.AppBorderSubtle
import com.example.appbike.ui.theme.AppPrimary
import com.example.appbike.ui.theme.AppPrimaryBright
import com.example.appbike.ui.theme.AppSurfaceElevated
import com.example.appbike.ui.theme.AppTextPrimary
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineOpacity
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import kotlin.math.roundToInt

internal enum class MeetupCreationStep { CLOSED, SELECT_LOCATION, FORM }
private enum class LocationSetupStep { READY, REQUESTING_PERMISSION, LOCATING, CONFIRM, MANUAL }
private const val OPEN_FREE_MAP_STYLE = "https://tiles.openfreemap.org/styles/liberty"
private const val STREET_MAX_CAMERA_ZOOM = 20.0
internal const val SATELLITE_MAX_CAMERA_ZOOM = 17.0
private const val SATELLITE_MAP_STYLE = """
    {
      "version": 8,
      "name": "APPbike Satelite",
      "sources": {
        "esri-world-imagery": {
          "type": "raster",
          "tiles": [
            "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
          ],
          "tileSize": 256,
          "minzoom": 0,
          "maxzoom": 19,
          "attribution": "Imagenes: Esri, Vantor, Earthstar Geographics y la comunidad GIS"
        },
        "esri-world-labels": {
          "type": "raster",
          "tiles": [
            "https://server.arcgisonline.com/ArcGIS/rest/services/Reference/World_Boundaries_and_Places/MapServer/tile/{z}/{y}/{x}"
          ],
          "tileSize": 256,
          "minzoom": 0,
          "maxzoom": 19,
          "attribution": "Etiquetas: Esri, HERE, Garmin, OpenStreetMap y la comunidad GIS"
        }
      },
      "layers": [
        {
          "id": "satellite-background",
          "type": "background",
          "paint": { "background-color": "#06100C" }
        },
        {
          "id": "satellite-imagery",
          "type": "raster",
          "source": "esri-world-imagery"
        },
        {
          "id": "satellite-labels",
          "type": "raster",
          "source": "esri-world-labels"
        }
      ]
    }
"""
internal enum class MapStyleMode(
    val visibleLabel: String,
    val styleDefinition: String,
    val maximumCameraZoom: Double
) {
    MAPA("Mapa", OPEN_FREE_MAP_STYLE, STREET_MAX_CAMERA_ZOOM),
    SATELITE("Satélite", SATELLITE_MAP_STYLE, SATELLITE_MAX_CAMERA_ZOOM)
}
internal const val LOCATION_SEARCH_PANEL_TEST_TAG = "location_search_panel"
private const val USER_SOURCE_ID = "appbike-user-source"
private const val USER_LAYER_ID = "appbike-user-layer"
private const val USER_ICON_ID = "appbike-user-icon"
private const val MEETUP_SOURCE_ID = "appbike-meetup-source"
private const val MEETUP_LAYER_ID = "appbike-meetup-layer"
private const val MEETUP_ICON_ID = "appbike-meetup-icon"
private const val SELECTED_SOURCE_ID = "appbike-selected-source"
private const val SELECTED_LAYER_ID = "appbike-selected-layer"
private const val SELECTED_ICON_ID = "appbike-selected-icon"
private const val ROUTE_SOURCE_ID = "appbike-route-source"
private const val ROUTE_LAYER_ID = "appbike-route-layer"
private val DEFAULT_MAP_CENTER = GeoPoint(-33.4489, -70.6693, "Santiago")

@Composable
fun RoutesScreen(
    account: AccountSession?,
    onOpenChat: (UserChat) -> Unit = {},
    onOpenAccount: () -> Unit = {},
    initialMeetupId: String? = null,
    onInitialMeetupConsumed: () -> Unit = {},
    isActive: Boolean = true
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val storedLocation = remember { LocalDataStore.loadLocation(context) }
    var center by remember { mutableStateOf(storedLocation ?: DEFAULT_MAP_CENTER) }
    var userLocation by remember { mutableStateOf(storedLocation) }
    var pendingLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var locationSetupStep by remember {
        mutableStateOf(
            if (storedLocation == null) {
                LocationSetupStep.REQUESTING_PERMISSION
            } else {
                LocationSetupStep.READY
            }
        )
    }
    var locationSetupMessage by remember { mutableStateOf<String?>(null) }
    var mapStyleMode by remember { mutableStateOf(MapStyleMode.MAPA) }
    var searchControlsExpanded by remember { mutableStateOf(false) }
    var mapStyleMenuExpanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var creationStep by remember { mutableStateOf(MeetupCreationStep.CLOSED) }
    var creationLoading by remember { mutableStateOf(false) }
    var creationError by remember { mutableStateOf<String?>(null) }
    var selectedPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var meetupDetail by remember { mutableStateOf<MeetupEvent?>(null) }
    var meetupDetailLoading by remember { mutableStateOf(false) }
    var meetupDetailError by remember { mutableStateOf<String?>(null) }
    var refreshRequestId by remember { mutableIntStateOf(0) }
    var meetupDetailRequestId by remember { mutableIntStateOf(0) }
    var locationCommitId by remember { mutableIntStateOf(0) }
    val meetups = remember { mutableStateListOf<MeetupEvent>() }
    var hasLoaded by remember { mutableStateOf(false) }
    var mapActivated by remember { mutableStateOf(false) }
    var locationRequestStarted by remember { mutableStateOf(false) }
    var routePreview by remember { mutableStateOf<CyclingRoutePreview?>(null) }
    var routeTitle by remember { mutableStateOf("") }
    var routeDestinationPicker by remember { mutableStateOf(false) }
    var routeLoading by remember { mutableStateOf(false) }
    var routeNotice by remember { mutableStateOf<String?>(null) }
    var routeRequestId by remember { mutableIntStateOf(0) }
    var routeJob by remember { mutableStateOf<Job?>(null) }

    fun refresh(point: GeoPoint = center) {
        val requestedQuery = query
        val requestId = refreshRequestId + 1
        refreshRequestId = requestId
        scope.launch {
            isLoading = true
            error = null
            val result = runSuspendCatching {
                withContext(Dispatchers.IO) {
                    RemoteConnections.loadNearbyMeetups(
                        center = point,
                        query = requestedQuery,
                        radiusKm = 40,
                        status = "activa"
                    )
                }
            }
            if (requestId != refreshRequestId) return@launch
            result.onSuccess {
                meetups.clear()
                meetups.addAll(it)
            }.onFailure { error = RemoteConnections.userFriendlyError(it) }
            isLoading = false
            hasLoaded = true
        }
    }

    fun openMeetupById(meetupId: String) {
        val requestId = meetupDetailRequestId + 1
        meetupDetailRequestId = requestId
        scope.launch {
            meetupDetail = null
            meetupDetailLoading = true
            meetupDetailError = null
            val result = runSuspendCatching {
                withContext(Dispatchers.IO) {
                    RemoteConnections.loadMeetupDetails(meetupId)
                }
            }
            if (requestId != meetupDetailRequestId) return@launch
            result.onSuccess { meetupDetail = it }
                .onFailure { meetupDetailError = RemoteConnections.userFriendlyError(it) }
            meetupDetailLoading = false
        }
    }

    fun completeMeetup(event: MeetupEvent) {
        scope.launch {
            meetupDetailLoading = true
            meetupDetailError = null
            runSuspendCatching {
                withContext(Dispatchers.IO) {
                    RemoteConnections.completeMeetup(
                        account?.userId ?: throw IllegalStateException("Inicia sesión."),
                        event.id
                    )
                }
            }.onSuccess {
                meetupDetail = it
                userLocation?.let(::refresh)
            }.onFailure { meetupDetailError = RemoteConnections.userFriendlyError(it) }
            meetupDetailLoading = false
        }
    }

    fun addMeetupPhoto(event: MeetupEvent, imageUri: String) {
        scope.launch {
            meetupDetailLoading = true
            meetupDetailError = null
            runSuspendCatching {
                withContext(Dispatchers.IO) {
                    RemoteConnections.uploadMeetupPhoto(
                        context,
                        account?.userId ?: throw IllegalStateException("Inicia sesión."),
                        event.id,
                        imageUri
                    )
                    RemoteConnections.loadMeetupDetails(event.id)
                }
            }.onSuccess {
                meetupDetail = it
                userLocation?.let(::refresh)
            }.onFailure { meetupDetailError = RemoteConnections.userFriendlyError(it) }
            meetupDetailLoading = false
        }
    }

    fun contactOrganizer(event: MeetupEvent) {
        val session = account ?: run {
            onOpenAccount()
            return
        }
        scope.launch {
            meetupDetailLoading = true
            meetupDetailError = null
            runSuspendCatching {
                withContext(Dispatchers.IO) {
                    RemoteConnections.getOrCreateChat(
                        userId = session.userId,
                        relatedUserId = event.createdBy,
                        type = ChatType.SOCIAL,
                        relatedEntityId = event.id,
                        title = event.title
                    )
                }
            }.onSuccess(onOpenChat)
                .onFailure { meetupDetailError = RemoteConnections.userFriendlyError(it) }
            meetupDetailLoading = false
        }
    }

    fun openMeetup(event: MeetupEvent) = openMeetupById(event.id)

    fun planCyclingRoute(destination: GeoPoint, title: String) {
        val origin = userLocation ?: run {
            meetupDetail = null
            locationSetupMessage = "Confirma tu ubicación para crear el trayecto."
            locationSetupStep = LocationSetupStep.MANUAL
            return
        }
        if (destination.latitude !in -90.0..90.0 ||
            destination.longitude !in -180.0..180.0
        ) {
            routeNotice = "Ese destino no tiene coordenadas válidas."
            return
        }
        val requestId = routeRequestId + 1
        routeRequestId = requestId
        routeJob?.cancel()
        routeTitle = title.ifBlank { destination.shortLabel() }
        routeDestinationPicker = false
        routePreview = null
        routeNotice = null
        routeLoading = true
        selectedPoint = destination
        LocalDataStore.rememberRecentLocation(context, destination)
        center = GeoPoint(
            latitude = (origin.latitude + destination.latitude) / 2.0,
            longitude = (origin.longitude + destination.longitude) / 2.0,
            label = routeTitle
        )
        meetupDetail = null
        meetupDetailError = null
        routeJob = scope.launch {
            val result = runSuspendCatching {
                withContext(Dispatchers.IO) {
                    RemoteConnections.loadCyclingRoute(origin, destination)
                }
            }
            if (requestId != routeRequestId) return@launch
            result.onSuccess { routePreview = it }
                .onFailure { failure ->
                    routePreview = buildDirectCyclingRoutePreview(origin, destination)
                    routeNotice =
                        "No pudimos seguir las calles ahora. Se muestra una línea directa de respaldo: " +
                            RemoteConnections.userFriendlyError(failure)
                }
            routeLoading = false
            routeJob = null
        }
    }

    fun previewRouteTo(event: MeetupEvent) {
        if (event.latitude !in -90.0..90.0 || event.longitude !in -180.0..180.0 ||
            (event.latitude == 0.0 && event.longitude == 0.0)
        ) {
            meetupDetailError = "Esta junta todavía no tiene coordenadas válidas."
            return
        }
        planCyclingRoute(
            destination = GeoPoint(
                latitude = event.latitude,
                longitude = event.longitude,
                label = event.location.substringAfter('|', event.location)
            ),
            title = event.title
        )
    }

    fun openExternalBikeNavigation(preview: CyclingRoutePreview) {
        val uri = "https://www.google.com/maps/dir/".toUri().buildUpon()
            .appendQueryParameter("api", "1")
            .appendQueryParameter(
                "origin",
                "${preview.origin.latitude},${preview.origin.longitude}"
            )
            .appendQueryParameter(
                "destination",
                "${preview.destination.latitude},${preview.destination.longitude}"
            )
            .appendQueryParameter("travelmode", "bicycling")
            .appendQueryParameter("dir_action", "navigate")
            .build()
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
            .onFailure {
                error = "No encontramos una aplicación compatible para abrir la ruta ciclista."
            }
    }

    fun commitLocation(point: GeoPoint) {
        val commitId = locationCommitId + 1
        locationCommitId = commitId
        userLocation = point
        pendingLocation = null
        center = point
        routePreview = null
        routeTitle = ""
        routeDestinationPicker = false
        routeLoading = false
        routeNotice = null
        routeRequestId += 1
        routeJob?.cancel()
        routeJob = null
        locationSetupMessage = null
        locationSetupStep = LocationSetupStep.READY
        LocalDataStore.saveLocation(context, point)
        refresh(point)
        scope.launch {
            val resolved = withContext(Dispatchers.IO) {
                RemoteConnections.resolveCommunityLocation(point)
            }
            if (commitId != locationCommitId) return@launch
            if (resolved != point) {
                userLocation = resolved
                center = resolved
                LocalDataStore.saveLocation(context, resolved)
                refresh(resolved)
            }
        }
    }

    fun locateDevice() {
        locationSetupStep = LocationSetupStep.LOCATING
        locationSetupMessage = null
        scope.launch {
            runSuspendCatching {
                withContext(Dispatchers.IO) {
                    val devicePoint = DeviceLocationProvider.currentLocation(context)
                    runCatching {
                        RemoteConnections.reverseGeocodeLocation(devicePoint)
                    }.getOrElse {
                        devicePoint.copy(label = devicePoint.coordinateLabel())
                    }
                }
            }.onSuccess { point ->
                pendingLocation = point
                center = point
                locationSetupStep = LocationSetupStep.CONFIRM
            }.onFailure { locationError ->
                locationSetupMessage = locationError.message
                    ?: "No fue posible obtener la ubicación actual."
                locationSetupStep = LocationSetupStep.MANUAL
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            locateDevice()
        } else {
            locationSetupMessage =
                "No se concedió acceso a la ubicación. Puedes elegirla manualmente."
            locationSetupStep = LocationSetupStep.MANUAL
        }
    }

    LaunchedEffect(isActive) {
        if (isActive) mapActivated = true
        if (isActive && storedLocation == null && !locationRequestStarted) {
            locationRequestStarted = true
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(storedLocation, isActive) {
        if (isActive && !hasLoaded) storedLocation?.let(::refresh)
    }

    LaunchedEffect(initialMeetupId, isActive) {
        val meetupId = initialMeetupId?.takeIf(String::isNotBlank) ?: return@LaunchedEffect
        if (!isActive) return@LaunchedEffect
        onInitialMeetupConsumed()
        openMeetupById(meetupId)
    }

    val displayedLocation = pendingLocation ?: userLocation

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .zIndex(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MapControlButton(
                    icon = if (searchControlsExpanded) Icons.Outlined.Close else Icons.Outlined.Search,
                    contentDescription = if (searchControlsExpanded) {
                        "Cerrar búsqueda de juntas"
                    } else {
                        "Buscar juntas"
                    },
                    selected = searchControlsExpanded,
                    enabled = creationStep == MeetupCreationStep.CLOSED,
                    onClick = { searchControlsExpanded = !searchControlsExpanded }
                )
                Box {
                    MapControlButton(
                        icon = Icons.Outlined.Layers,
                        contentDescription = "Capas del mapa: ${mapStyleMode.visibleLabel}",
                        selected = mapStyleMenuExpanded,
                        onClick = { mapStyleMenuExpanded = true }
                    )
                    DropdownMenu(
                        expanded = mapStyleMenuExpanded,
                        onDismissRequest = { mapStyleMenuExpanded = false },
                        containerColor = AppSurfaceElevated.copy(alpha = 0.94f)
                    ) {
                        MapStyleMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode.visibleLabel) },
                                leadingIcon = {
                                    if (mode == mapStyleMode) {
                                        Icon(Icons.Outlined.Check, contentDescription = null)
                                    } else {
                                        Icon(Icons.Outlined.Layers, contentDescription = null)
                                    }
                                },
                                onClick = {
                                    mapStyleMode = mode
                                    mapStyleMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (searchControlsExpanded) {
                SearchField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "Buscar junta",
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { userLocation?.let(::refresh) }) {
                            Icon(Icons.Outlined.Search, contentDescription = "Buscar")
                        }
                    },
                    enabled = userLocation != null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = AppSurfaceElevated.copy(alpha = 0.78f),
                        unfocusedContainerColor = AppSurfaceElevated.copy(alpha = 0.72f),
                        disabledContainerColor = AppSurfaceElevated.copy(alpha = 0.58f),
                        focusedTextColor = AppTextPrimary,
                        unfocusedTextColor = AppTextPrimary,
                        focusedBorderColor = AppPrimaryBright.copy(alpha = 0.78f),
                        unfocusedBorderColor = AppBorderSubtle,
                        cursorColor = AppPrimaryBright
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { userLocation?.let(::refresh) }
                    )
                )

                CurrentLocationRow(
                    location = displayedLocation,
                    locating = locationSetupStep == LocationSetupStep.LOCATING ||
                        locationSetupStep == LocationSetupStep.REQUESTING_PERMISSION,
                    enabled = locationSetupStep != LocationSetupStep.LOCATING,
                    onLocate = {
                        val hasFineLocation = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasFineLocation) {
                            locateDevice()
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    onClick = {
                        locationSetupMessage = null
                        locationSetupStep = LocationSetupStep.MANUAL
                    }
                )
            }
        }

        if (creationStep == MeetupCreationStep.SELECT_LOCATION) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .zIndex(1f),
                shape = RoundedCornerShape(22.dp),
                color = AppSurfaceElevated,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    AppBorderSubtle
                ),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedPoint = null
                            creationStep = MeetupCreationStep.CLOSED
                        }
                    ) { Text("Cancelar") }
                    Button(
                        modifier = Modifier.weight(1f),
                        enabled = selectedPoint != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        onClick = { creationStep = MeetupCreationStep.FORM }
                    ) { Text("Confirmar") }
                }
            }
        } else if (routePreview == null && !routeLoading) {
            MapPrimaryActions(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 18.dp)
                    .zIndex(1f),
                onCreateRoute = {
                    if (userLocation == null) {
                        locationSetupMessage =
                            "Confirma tu ubicación antes de elegir el destino del trayecto."
                        locationSetupStep = LocationSetupStep.MANUAL
                    } else {
                        searchControlsExpanded = false
                        routeNotice = null
                        routeDestinationPicker = true
                    }
                },
                onCreateMeetup = {
                    if (account == null) onOpenAccount()
                    else {
                        creationError = null
                        creationStep = MeetupCreationStep.SELECT_LOCATION
                    }
                }
            )
        }

        if (mapActivated) {
            OpenStreetMap(
                modifier = Modifier.fillMaxSize(),
                center = center,
                userLocation = displayedLocation,
                meetups = meetups.toList(),
                selectedPoint = selectedPoint,
                routePreview = routePreview,
                isActive = isActive,
                creationStep = creationStep,
                styleDefinition = mapStyleMode.styleDefinition,
                maximumZoom = mapStyleMode.maximumCameraZoom,
                onMapReady = {},
                onPointSelected = { selectedPoint = it },
                onMeetupSelected = ::openMeetup,
                onMapError = {
                    error = "No fue posible cargar el mapa. Revisa tu conexión a internet."
                }
            )
        }

        routePreview?.let { preview ->
            RoutePreviewPanel(
                preview = preview,
                destinationTitle = routeTitle,
                notice = routeNotice,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(AppDimens.Space4)
                    .zIndex(2f),
                onOpenNavigation = { openExternalBikeNavigation(preview) },
                onCancel = {
                    routeRequestId += 1
                    routeJob?.cancel()
                    routeJob = null
                    routePreview = null
                    routeTitle = ""
                    routeNotice = null
                    routeLoading = false
                    selectedPoint = null
                    userLocation?.let { center = it }
                }
            )
        }

        if (routeLoading) {
            RouteLoadingPanel(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(AppDimens.Space4)
                    .zIndex(2f),
                onCancel = {
                    routeRequestId += 1
                    routeJob?.cancel()
                    routeJob = null
                    routeLoading = false
                    routeTitle = ""
                    routeNotice = null
                    selectedPoint = null
                    userLocation?.let { center = it }
                }
            )
        }

        if (isLoading) {
            Surface(
                modifier = Modifier.align(Alignment.TopCenter).padding(
                    top = if (searchControlsExpanded) 160.dp else 68.dp
                ),
                shape = CircleShape,
                color = AppBackgroundElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, AppBorderSubtle),
                shadowElevation = 4.dp
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(10.dp).size(24.dp),
                    strokeWidth = 2.dp,
                    color = AppPrimaryBright
                )
            }
        }

        error?.let {
            ErrorBanner(
                message = it,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(
                        top = if (searchControlsExpanded) 164.dp else 72.dp,
                        start = 24.dp,
                        end = 24.dp
                    )
            )
        }

        if (locationSetupStep == LocationSetupStep.MANUAL) {
            LocationSearchDialog(
                initial = pendingLocation?.shortLabel().orEmpty(),
                message = locationSetupMessage,
                onDismiss = {
                    locationSetupStep = if (pendingLocation != null && userLocation == null) {
                        LocationSetupStep.CONFIRM
                    } else {
                        LocationSetupStep.READY
                    }
                },
                onLocation = ::commitLocation
            )
        }

        if (routeDestinationPicker) {
            LocationSearchDialog(
                initial = "",
                message = null,
                title = "Crear trayecto",
                helpText = "Busca el lugar al que quieres llegar.",
                fieldLabel = "Destino",
                onDismiss = { routeDestinationPicker = false },
                onLocation = { destination ->
                    planCyclingRoute(destination, destination.shortLabel())
                }
            )
        }
    }

    when (locationSetupStep) {
        LocationSetupStep.LOCATING -> LocationLoadingDialog()
        LocationSetupStep.CONFIRM -> pendingLocation?.let { point ->
            LocationConfirmationDialog(
                point = point,
                onConfirm = { commitLocation(point) },
                onManual = {
                    locationSetupMessage = null
                    locationSetupStep = LocationSetupStep.MANUAL
                }
            )
        }
        LocationSetupStep.MANUAL -> Unit
        else -> Unit
    }

    if (creationStep == MeetupCreationStep.FORM && selectedPoint != null && account != null) {
        MeetupFormDialog(
            isSaving = creationLoading,
            error = creationError,
            onDismiss = {
                if (!creationLoading) creationStep = MeetupCreationStep.SELECT_LOCATION
            },
            onCreate = { title, dateTime, description, imageUri ->
                if (creationLoading) return@MeetupFormDialog
                scope.launch {
                    creationLoading = true
                    creationError = null
                    val point = selectedPoint ?: run {
                        creationLoading = false
                        return@launch
                    }
                    runSuspendCatching {
                        withContext(Dispatchers.IO) {
                            val selectedLocation = runCatching {
                                RemoteConnections.reverseGeocodeLocation(point)
                            }.getOrElse {
                                point.copy(
                                    label = point.coordinateLabel(),
                                    countryCode = userLocation?.countryCode.orEmpty(),
                                    administrativeArea = userLocation?.administrativeArea.orEmpty(),
                                    regionCode = userLocation?.regionCode.orEmpty(),
                                    currencyCode = userLocation?.currencyCode.orEmpty()
                                )
                            }
                            val region = RemoteConnections.communityRegionFor(
                                selectedLocation
                            )
                            RemoteConnections.createMeetupEvent(
                                context,
                                MeetupEvent(
                                    id = "",
                                    title = title,
                                    dateTime = dateTime,
                                    description = description,
                                    latitude = point.latitude,
                                    longitude = point.longitude,
                                    createdBy = account.userId,
                                    createdByUsername = account.username,
                                    region = region,
                                    location = selectedLocation.label,
                                    imageUri = imageUri,
                                    countryCode = selectedLocation.countryCode,
                                    administrativeArea = selectedLocation.administrativeArea
                                )
                            )
                        }
                    }.onSuccess {
                        creationStep = MeetupCreationStep.CLOSED
                        selectedPoint = null
                        userLocation?.let(::refresh)
                    }.onFailure { failure ->
                        if (failure is RemoteConnections.RemotePartialSuccessException) {
                            creationStep = MeetupCreationStep.CLOSED
                            selectedPoint = null
                            error = RemoteConnections.userFriendlyError(failure)
                            userLocation?.let(::refresh)
                        } else {
                            creationError = RemoteConnections.userFriendlyError(failure)
                        }
                    }
                    creationLoading = false
                }
            }
        )
    }

    meetupDetail?.let { event ->
        MeetupDetailDialog(
            event = event,
            account = account,
            routePreview = userLocation?.let { origin ->
                if (event.latitude in -90.0..90.0 && event.longitude in -180.0..180.0 &&
                    !(event.latitude == 0.0 && event.longitude == 0.0)
                ) {
                    buildDirectCyclingRoutePreview(
                        origin,
                        GeoPoint(event.latitude, event.longitude)
                    )
                } else {
                    null
                }
            },
            loading = meetupDetailLoading,
            error = meetupDetailError,
            onDismiss = {
                meetupDetailRequestId += 1
                meetupDetail = null
                meetupDetailError = null
                meetupDetailLoading = false
            },
            onComplete = { completeMeetup(event) },
            onAddPhoto = { imageUri -> addMeetupPhoto(event, imageUri) },
            onContact = { contactOrganizer(event) },
            onPreviewRoute = { previewRouteTo(event) }
        )
    }
}

@Composable
internal fun OpenStreetMap(
    modifier: Modifier,
    center: GeoPoint,
    userLocation: GeoPoint?,
    meetups: List<MeetupEvent>,
    selectedPoint: GeoPoint?,
    routePreview: CyclingRoutePreview? = null,
    isActive: Boolean = true,
    creationStep: MeetupCreationStep,
    styleDefinition: String = OPEN_FREE_MAP_STYLE,
    maximumZoom: Double = STREET_MAX_CAMERA_ZOOM,
    onMapReady: (MapLibreMap) -> Unit,
    onPointSelected: (GeoPoint) -> Unit,
    onMeetupSelected: (MeetupEvent) -> Unit,
    onMapError: (String) -> Unit
) {
    val context = LocalContext.current
    val routeCameraPadding = with(LocalDensity.current) { 96.dp.roundToPx() }
    val lifecycleOwner = context as? LifecycleOwner
    val currentCenter = rememberUpdatedState(center)
    val currentCreationStep = rememberUpdatedState(creationStep)
    val currentOnMapReady = rememberUpdatedState(onMapReady)
    val currentOnPointSelected = rememberUpdatedState(onPointSelected)
    val currentMeetups = rememberUpdatedState(meetups)
    val currentOnMeetupSelected = rememberUpdatedState(onMeetupSelected)
    val currentOnMapError = rememberUpdatedState(onMapError)
    val selectedIcon = remember(context) { createSelectedPointIcon(context) }
    val userLocationIcon = remember(context) { createUserLocationIcon(context) }
    val meetupIcon = remember(context) { createMeetupIcon(context) }
    val mapView = remember(context) {
        MapLibre.getInstance(context.applicationContext)
        val options = MapLibreMapOptions.createFromAttributes(context).textureMode(true)
        MapView(context, options).apply { onCreate(null) }
    }

    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var loadedStyleDefinition by remember { mutableStateOf<String?>(null) }
    var styleRequestId by remember { mutableIntStateOf(0) }

    LaunchedEffect(mapView, isActive) {
        mapView.setMaximumFps(if (isActive) 60 else 4)
    }

    DisposableEffect(mapView, lifecycleOwner) {
        var started = false
        var resumed = false
        var destroyed = false

        fun destroyMapView() {
            if (destroyed) return
            if (resumed) {
                mapView.onPause()
                resumed = false
            }
            if (started) {
                mapView.onStop()
                started = false
            }
            mapView.onDestroy()
            destroyed = true
        }

        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (!started) {
                    mapView.onStart()
                    started = true
                }
                Lifecycle.Event.ON_RESUME -> if (!resumed) {
                    mapView.onResume()
                    resumed = true
                }
                Lifecycle.Event.ON_PAUSE -> if (resumed) {
                    mapView.onPause()
                    resumed = false
                }
                Lifecycle.Event.ON_STOP -> if (started) {
                    mapView.onStop()
                    started = false
                }
                Lifecycle.Event.ON_DESTROY -> destroyMapView()
                else -> Unit
            }
        }

        if (lifecycleOwner != null) {
            lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
            if (!started && lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                mapView.onStart()
                started = true
            }
            if (!resumed && lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                mapView.onResume()
                resumed = true
            }
        } else {
            mapView.onStart()
            mapView.onResume()
            started = true
            resumed = true
        }

        var configuredMap: MapLibreMap? = null
        var clickListener: MapLibreMap.OnMapClickListener? = null

        mapView.getMapAsync { readyMap ->
            if (destroyed) return@getMapAsync

            val newClickListener = MapLibreMap.OnMapClickListener { point ->
                if (currentCreationStep.value == MeetupCreationStep.SELECT_LOCATION) {
                    currentOnPointSelected.value(
                        GeoPoint(point.latitude, point.longitude)
                    )
                    true
                } else {
                    val feature = readyMap.queryRenderedFeatures(
                        readyMap.projection.toScreenLocation(point),
                        MEETUP_LAYER_ID
                    ).firstOrNull()
                    val meetupId = feature?.getStringProperty("meetup_id")
                    val event = currentMeetups.value.firstOrNull { it.id == meetupId }
                    if (event != null) {
                        currentOnMeetupSelected.value(event)
                        true
                    } else {
                        false
                    }
                }
            }

            configuredMap = readyMap
            clickListener = newClickListener
            readyMap.addOnMapClickListener(newClickListener)
            readyMap.uiSettings.isCompassEnabled = false
            readyMap.uiSettings.isLogoEnabled = false
            readyMap.uiSettings.isAttributionEnabled = true
            readyMap.uiSettings.isZoomGesturesEnabled = true
            readyMap.uiSettings.isScrollGesturesEnabled = true
            readyMap.cameraPosition = CameraPosition.Builder()
                .target(currentCenter.value.toLatLng())
                .zoom(13.0)
                .build()
            map = readyMap
        }

        val failureListener = MapView.OnDidFailLoadingMapListener {
            currentOnMapError.value(it)
        }
        mapView.addOnDidFailLoadingMapListener(failureListener)

        onDispose {
            configuredMap?.let { configured ->
                clickListener?.let(configured::removeOnMapClickListener)
            }
            mapView.removeOnDidFailLoadingMapListener(failureListener)
            lifecycleOwner?.lifecycle?.removeObserver(lifecycleObserver)
            map = null
            loadedStyleDefinition = null
            destroyMapView()
        }
    }

    LaunchedEffect(map, styleDefinition) {
        val readyMap = map ?: return@LaunchedEffect
        val requestId = styleRequestId + 1
        styleRequestId = requestId
        loadedStyleDefinition = null
        val styleCallback = Style.OnStyleLoaded { style ->
            if (requestId != styleRequestId || map !== readyMap) return@OnStyleLoaded
            style.addImage(USER_ICON_ID, userLocationIcon)
            style.addImage(MEETUP_ICON_ID, meetupIcon)
            style.addImage(SELECTED_ICON_ID, selectedIcon)
            style.addSource(GeoJsonSource(USER_SOURCE_ID, emptyFeatureCollection()))
            style.addSource(GeoJsonSource(MEETUP_SOURCE_ID, emptyFeatureCollection()))
            style.addSource(GeoJsonSource(SELECTED_SOURCE_ID, emptyFeatureCollection()))
            style.addSource(GeoJsonSource(ROUTE_SOURCE_ID, emptyFeatureCollection()))
            style.addLayer(
                LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID).withProperties(
                    lineColor(0xFF21E58B.toInt()),
                    lineWidth(5f),
                    lineOpacity(0.92f)
                )
            )
            style.addLayer(symbolLayer(USER_LAYER_ID, USER_SOURCE_ID, USER_ICON_ID))
            style.addLayer(symbolLayer(MEETUP_LAYER_ID, MEETUP_SOURCE_ID, MEETUP_ICON_ID))
            style.addLayer(symbolLayer(SELECTED_LAYER_ID, SELECTED_SOURCE_ID, SELECTED_ICON_ID))
            loadedStyleDefinition = styleDefinition
            currentOnMapReady.value(readyMap)
        }
        if (styleDefinition.trimStart().startsWith("{")) {
            readyMap.setStyle(Style.Builder().fromJson(styleDefinition), styleCallback)
        } else {
            readyMap.setStyle(styleDefinition, styleCallback)
        }
    }

    LaunchedEffect(map, maximumZoom) {
        map?.setMaxZoomPreference(maximumZoom)
    }

    LaunchedEffect(map, center) {
        map?.animateCamera(
            CameraUpdateFactory.newLatLngZoom(center.toLatLng(), 13.0)
        )
    }

    LaunchedEffect(map, routePreview) {
        val geometry = routePreview?.geometry.orEmpty()
        if (geometry.size < 2) return@LaunchedEffect
        val bounds = LatLngBounds.Builder().apply {
            geometry.forEach { include(it.toLatLng()) }
        }.build()
        map?.animateCamera(
            CameraUpdateFactory.newLatLngBounds(bounds, routeCameraPadding)
        )
    }

    LaunchedEffect(
        map,
        loadedStyleDefinition,
        userLocation,
        meetups,
        selectedPoint,
        routePreview
    ) {
        val readyMap = map ?: return@LaunchedEffect
        if (loadedStyleDefinition == null) return@LaunchedEffect
        val style = readyMap.style ?: return@LaunchedEffect
        style.getSourceAs<GeoJsonSource>(USER_SOURCE_ID)?.setGeoJson(
            featureCollection(userLocation?.let { pointFeature(it) })
        )
        val meetupFeatures = meetups.filter { event ->
            event.latitude in -90.0..90.0 && event.longitude in -180.0..180.0 &&
                !(event.latitude == 0.0 && event.longitude == 0.0)
        }.map { event ->
            Feature.fromGeometry(Point.fromLngLat(event.longitude, event.latitude)).apply {
                addStringProperty("meetup_id", event.id)
                addStringProperty("title", event.title)
            }
        }
        style.getSourceAs<GeoJsonSource>(MEETUP_SOURCE_ID)?.setGeoJson(
            FeatureCollection.fromFeatures(meetupFeatures)
        )
        style.getSourceAs<GeoJsonSource>(SELECTED_SOURCE_ID)?.setGeoJson(
            featureCollection(selectedPoint?.let { pointFeature(it) })
        )
        style.getSourceAs<GeoJsonSource>(ROUTE_SOURCE_ID)?.setGeoJson(
            routeFeatureCollection(routePreview)
        )
    }

    AndroidView(
        factory = { mapView },
        update = { view ->
            val mapType = if (styleDefinition == SATELLITE_MAP_STYLE) {
                "Mapa satelital"
            } else {
                "Mapa de calles"
            }
            view.contentDescription = when (meetups.size) {
                0 -> "$mapType. No hay juntas cercanas visibles"
                1 -> "$mapType. 1 junta cercana visible"
                else -> "$mapType. ${meetups.size} juntas cercanas visibles"
            }
        },
        modifier = modifier
    )
}

private fun createSelectedPointIcon(context: android.content.Context): Bitmap =
        createBitmap(
            (36 * context.resources.displayMetrics.density).roundToInt(),
            (36 * context.resources.displayMetrics.density).roundToInt(),
            Bitmap.Config.ARGB_8888
        ).also { bitmap ->
            val canvas = Canvas(bitmap)
            val radius = bitmap.width / 2f
            canvas.drawCircle(
                radius,
                radius,
                radius,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE }
            )
            canvas.drawCircle(
                radius,
                radius,
                radius * 0.72f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(36, 107, 206) }
            )
        }

private fun createUserLocationIcon(context: android.content.Context): Bitmap =
    createTintedMarkerBitmap(context, android.graphics.Color.rgb(23, 107, 82))

private fun createMeetupIcon(context: android.content.Context): Bitmap =
    createTintedMarkerBitmap(context, android.graphics.Color.rgb(36, 107, 206))

private fun createTintedMarkerBitmap(context: android.content.Context, tint: Int): Bitmap =
    ResourcesCompat.getDrawable(
        context.resources,
        org.maplibre.android.R.drawable.maplibre_marker_icon_default,
        context.theme
    )?.let { drawable ->
        val tinted = DrawableCompat.wrap(drawable.mutate())
        DrawableCompat.setTint(tinted, tint)
        val width = tinted.intrinsicWidth.coerceAtLeast(1)
        val height = tinted.intrinsicHeight.coerceAtLeast(1)
        val bitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888)
        tinted.setBounds(0, 0, width, height)
        tinted.draw(Canvas(bitmap))
        bitmap
    } ?: createSelectedPointIcon(context)

private fun symbolLayer(layerId: String, sourceId: String, iconId: String) =
    SymbolLayer(layerId, sourceId).withProperties(
        iconImage(iconId),
        iconAllowOverlap(true),
        iconIgnorePlacement(true)
    )

private fun pointFeature(point: GeoPoint): Feature =
    Feature.fromGeometry(Point.fromLngLat(point.longitude, point.latitude))

private fun featureCollection(feature: Feature?): FeatureCollection =
    FeatureCollection.fromFeatures(if (feature == null) emptyList() else listOf(feature))

private fun routeFeatureCollection(preview: CyclingRoutePreview?): FeatureCollection {
    val geometry = preview?.geometry.orEmpty()
    if (geometry.size < 2) return emptyFeatureCollection()
    val line = LineString.fromLngLats(
        geometry.map { point -> Point.fromLngLat(point.longitude, point.latitude) }
    )
    return FeatureCollection.fromFeature(Feature.fromGeometry(line))
}

private fun emptyFeatureCollection(): FeatureCollection =
    FeatureCollection.fromFeatures(emptyList<Feature>())

@Composable
internal fun MapPrimaryActions(
    modifier: Modifier = Modifier,
    onCreateRoute: () -> Unit,
    onCreateMeetup: () -> Unit
) {
    Surface(
        modifier = modifier
            .padding(horizontal = AppDimens.Space4),
        shape = RoundedCornerShape(AppDimens.RadiusPill),
        color = AppBackgroundElevated.copy(alpha = 0.78f),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppBorderSubtle),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            MapPrimaryAction(
                label = "Trayecto",
                icon = Icons.AutoMirrored.Outlined.DirectionsBike,
                selected = true,
                onClick = onCreateRoute
            )
            MapPrimaryAction(
                label = "Junta",
                icon = Icons.Outlined.Add,
                selected = false,
                onClick = onCreateMeetup
            )
        }
    }
}

@Composable
private fun MapPrimaryAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.heightIn(min = AppSizes.TouchTarget),
        shape = RoundedCornerShape(AppDimens.RadiusPill),
        color = if (selected) AppPrimary.copy(alpha = 0.86f) else Color.Transparent,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else AppTextPrimary,
        border = if (selected) {
            null
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, AppBorderSubtle)
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RouteLoadingPanel(
    modifier: Modifier = Modifier,
    onCancel: () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.RadiusXLarge),
        color = AppBackgroundElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, AppBorderSubtle),
        shadowElevation = AppElevation.Floating
    ) {
        Row(
            modifier = Modifier.padding(AppDimens.Space4),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Space3),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                strokeWidth = 2.dp,
                color = AppPrimaryBright
            )
            Column(Modifier.weight(1f)) {
                Text("Calculando trayecto", fontWeight = FontWeight.Bold)
                Text(
                    "Buscando un camino adecuado para bicicleta…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onCancel) {
                Icon(Icons.Outlined.Close, contentDescription = "Cancelar cálculo del trayecto")
            }
        }
    }
}

@Composable
private fun RoutePreviewPanel(
    preview: CyclingRoutePreview,
    destinationTitle: String,
    notice: String?,
    modifier: Modifier = Modifier,
    onOpenNavigation: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.RadiusXLarge),
        color = AppBackgroundElevated,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            AppPrimaryBright.copy(alpha = 0.42f)
        ),
        shadowElevation = AppElevation.Floating
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.Space4),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Space3)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Space3),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = CircleShape, color = AppPrimaryBright.copy(alpha = 0.14f)) {
                    Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.AutoMirrored.Outlined.DirectionsBike,
                            contentDescription = null,
                            tint = AppPrimaryBright
                        )
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = destinationTitle.ifBlank { "Trayecto" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (preview.isDirectEstimate) {
                            "Línea directa de respaldo · " +
                                "${preview.averageSpeedKmh?.roundToInt() ?: 15} km/h"
                        } else {
                            "Ruta ciclista por calles · OpenStreetMap"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onCancel) {
                    Icon(Icons.Outlined.Close, contentDescription = "Cancelar trayecto")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.Space3)) {
                SportMetricCard(
                    value = formatRouteDistance(preview.distanceKm),
                    label = if (preview.isDirectEstimate) {
                        "Distancia directa"
                    } else {
                        "Distancia de ruta"
                    },
                    modifier = Modifier.weight(1f)
                )
                SportMetricCard(
                    value = "${preview.estimatedMinutes} min",
                    label = "Tiempo estimado",
                    modifier = Modifier.weight(1f)
                )
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenNavigation
            ) {
                Icon(Icons.AutoMirrored.Outlined.DirectionsBike, contentDescription = null)
                Text("Abrir navegación ciclista", modifier = Modifier.padding(start = 8.dp))
            }
            notice?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Text(
                text = if (preview.isDirectEstimate) {
                    "La línea de respaldo no sigue calles. Usa la navegación externa para obtener indicaciones reales."
                } else {
                    "Camino calculado para bicicleta con datos © OpenStreetMap contributors. Revisa las condiciones del terreno antes de salir."
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CurrentLocationRow(
    location: GeoPoint?,
    locating: Boolean,
    enabled: Boolean,
    onLocate: () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.74f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.MyLocation,
                contentDescription = null,
                modifier = Modifier.size(17.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                "Ubicación actual:",
                modifier = Modifier.padding(start = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                when {
                    locating -> "Buscando…"
                    location != null -> location.shortLabel()
                    else -> "Sin configurar · toca para elegir"
                },
                modifier = Modifier.padding(start = 4.dp).weight(1f),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            TextButton(
                enabled = enabled && !locating,
                onClick = onLocate
            ) {
                Icon(
                    Icons.Outlined.MyLocation,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Text("Precisar", modifier = Modifier.padding(start = 4.dp))
            }
        }
    }
}

@Composable
private fun MapControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.size(48.dp),
        shape = CircleShape,
        color = if (selected) {
            AppPrimary.copy(alpha = 0.72f)
        } else {
            AppSurfaceElevated.copy(alpha = 0.72f)
        },
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) AppPrimaryBright.copy(alpha = 0.78f) else AppBorderSubtle
        ),
        shadowElevation = 3.dp
    ) {
        IconButton(onClick = onClick, enabled = enabled) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (enabled) AppTextPrimary else AppTextPrimary.copy(alpha = 0.42f)
            )
        }
    }
}

@Composable
private fun LocationLoadingDialog() {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Buscando tu ubicación") },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(26.dp), strokeWidth = 2.dp)
                Text(
                    "Usaremos esta posición solo para proponerte una ubicación inicial.",
                    modifier = Modifier.padding(start = 14.dp)
                )
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun LocationConfirmationDialog(
    point: GeoPoint,
    onConfirm: () -> Unit,
    onManual: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("¿Esta ubicación está bien?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(point.label.ifBlank { point.coordinateLabel() })
                Text(
                    point.coordinateLabel(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "La usaremos para encontrar juntas y publicaciones cercanas.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onManual) {
                Text("No, déjame corregirla")
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text("Está bien") }
        }
    )
}

@Composable
internal fun LocationSearchDialog(
    initial: String,
    message: String?,
    title: String = "Introduce tu ubicación",
    helpText: String = "Busca una ciudad, dirección o lugar.",
    fieldLabel: String = "Lugar o dirección",
    onDismiss: () -> Unit,
    onLocation: (GeoPoint) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrimInteractionSource = remember { MutableInteractionSource() }
    val recentLocations = remember { LocalDataStore.loadLocationHistory(context) }
    var input by remember(initial) { mutableStateOf(initial) }
    var clearInitialTextOnFocus by remember(initial) { mutableStateOf(initial.isNotEmpty()) }
    var hasEditedInput by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }

    fun close(action: () -> Unit) {
        if (closing) return
        closing = true
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        action()
    }

    fun dismiss() = close(onDismiss)

    fun selectLocation(point: GeoPoint) = close { onLocation(point) }

    BackHandler(enabled = !closing, onBack = ::dismiss)

    LaunchedEffect(input, hasEditedInput, closing) {
        if (!hasEditedInput || closing) return@LaunchedEffect
        val liveQuery = input.trim()
        if (liveQuery.length < 3) {
            loading = false
            results = emptyList()
            return@LaunchedEffect
        }

        loading = true
        try {
            delay(450)
            val deviceResults = withTimeoutOrNull(3_000L) {
                withContext(Dispatchers.IO) {
                    DeviceLocationProvider.searchLocations(context, liveQuery)
                }
            }.orEmpty()
            val liveResults = deviceResults.ifEmpty {
                withTimeoutOrNull(8_000L) {
                    withContext(Dispatchers.IO) {
                        RemoteConnections.searchLocations(
                            query = liveQuery,
                            connectTimeoutMs = 2_500,
                            readTimeoutMs = 4_500
                        )
                    }
                }.orEmpty()
            }
            results = liveResults
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            // Las sugerencias locales son opcionales. La lupa mantiene
            // disponible la busqueda remota de respaldo.
        } finally {
            loading = false
        }
    }

    fun search() {
        if (input.isBlank() || loading || closing) return
        hasEditedInput = true
        scope.launch {
            loading = true
            error = null
            try {
                val remoteResults = withContext(Dispatchers.IO) {
                    RemoteConnections.searchLocations(input)
                }
                results = remoteResults
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Throwable) {
                error = RemoteConnections.userFriendlyError(failure)
            } finally {
                loading = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.52f))
            .clickable(
                interactionSource = scrimInteractionSource,
                indication = null,
                enabled = !closing,
                onClick = ::dismiss
            )
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .padding(16.dp)
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .testTag(LOCATION_SEARCH_PANEL_TEST_TAG)
                .pointerInput(Unit) { detectTapGestures(onTap = {}) },
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 14.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(helpText)
                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        input = it
                        hasEditedInput = true
                        error = null
                        results = emptyList()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused && clearInitialTextOnFocus) {
                                input = ""
                                clearInitialTextOnFocus = false
                                hasEditedInput = false
                                error = null
                                results = emptyList()
                            }
                        }
                        .appBikeTextFieldGlow(),
                    colors = appBikeTextFieldColors(),
                    label = { Text(fieldLabel) },
                    leadingIcon = { Icon(Icons.Outlined.LocationOn, contentDescription = null) },
                    trailingIcon = {
                        IconButton(enabled = !loading, onClick = ::search) {
                            if (loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Outlined.Search, contentDescription = "Buscar lugar")
                            }
                        }
                    },
                    enabled = !closing,
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        autoCorrectEnabled = true,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Search,
                        hintLocales = LocaleList("es-CL,es")
                    ),
                    keyboardActions = KeyboardActions(onSearch = { search() })
                )

                (error ?: message)?.let { status ->
                    Text(
                        status,
                        color = if (error != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                val showSearchResults = hasEditedInput && input.trim().length >= 3
                val displayedLocations = if (showSearchResults) results else recentLocations

                if (displayedLocations.isNotEmpty()) {
                    Text(
                        if (showSearchResults) "Resultados" else "Ubicaciones recientes",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 230.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(
                            items = displayedLocations,
                            key = { "${it.latitude}:${it.longitude}" }
                        ) { point ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        enabled = !closing,
                                        onClick = { selectLocation(point) }
                                    ),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Column(Modifier.padding(10.dp)) {
                                    Text(
                                        point.shortLabel(),
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        point.label,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                } else if (showSearchResults && !loading) {
                    Text(
                        "No se encontraron lugares.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    "Sugerencias del dispositivo · Búsqueda manual: © OpenStreetMap contributors",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(enabled = !closing, onClick = ::dismiss) {
                        Text("Cancelar")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MeetupDetailDialog(
    event: MeetupEvent,
    account: AccountSession?,
    loading: Boolean,
    error: String?,
    routePreview: CyclingRoutePreview? = null,
    onDismiss: () -> Unit,
    onComplete: () -> Unit,
    onAddPhoto: (String) -> Unit,
    onContact: () -> Unit,
    onPreviewRoute: () -> Unit = {}
) {
    val context = LocalContext.current
    val isOwner = account?.userId == event.createdBy
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppBackgroundElevated,
        contentColor = AppTextPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 760.dp)
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
                Text(event.title, style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Organiza ${event.createdByUsername?.takeIf(String::isNotBlank) ?: "Usuario de APPBIKE"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${if (event.status == "pasada") "Junta pasada" else "Junta activa"} · ${event.region}",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                if (event.images.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(event.images) { photo ->
                            MeetupRemoteImage(
                                photo,
                                modifier = Modifier.size(220.dp, 145.dp)
                            )
                        }
                    }
                } else {
                    MeetupRemoteImage(
                        source = null,
                        modifier = Modifier.fillMaxWidth().height(132.dp)
                    )
                }
                if (event.dateTime.isNotBlank()) {
                    Text(event.dateTime, fontWeight = FontWeight.SemiBold)
                }
                Text(event.description)
                Text(
                    event.location.substringAfter('|', event.location),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                routePreview?.let { preview ->
                    Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.Space3)) {
                        SportMetricCard(
                            value = formatRouteDistance(preview.distanceKm),
                            label = "Distancia directa",
                            modifier = Modifier.weight(1f)
                        )
                        SportMetricCard(
                            value = "${preview.estimatedMinutes} min",
                            label = "Tiempo estimado",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Text(
                    text = "Participantes, dificultad y tipo de ciclismo aparecerán cuando estén disponibles.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (loading) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))

                if (isOwner) {
                    Text("Administrar junta", fontWeight = FontWeight.Bold)
                    if (event.status != "pasada") {
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !loading,
                            onClick = onComplete
                        ) { Text("Marcar como realizada") }
                    }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading,
                        onClick = {
                            imagePicker.launch(
                                arrayOf("image/jpeg", "image/png", "image/webp")
                            )
                        }
                    ) { Text("Agregar fotografía") }
                } else {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading,
                        onClick = onContact
                    ) {
                        Text(if (account == null) "Inicia sesión para participar" else "Quiero participar")
                    }
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !loading,
                    onClick = onPreviewRoute
                ) {
                    Icon(Icons.AutoMirrored.Outlined.DirectionsBike, contentDescription = null)
                    Text("Cómo llegar", modifier = Modifier.padding(start = 8.dp))
                }
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDismiss
                ) { Text("Cerrar") }
        }
    }
}

@Composable
private fun MeetupRemoteImage(
    source: String?,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(source) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(source) {
        bitmap = if (source.isNullOrBlank()) {
            null
        } else {
            runSuspendCatching {
                withContext(Dispatchers.IO) {
                    RemoteImageLoader.loadBitmap(source)
                }
            }.getOrNull()
        }
    }
    val displayedBitmap = bitmap
    if (displayedBitmap != null) {
        Image(
            bitmap = displayedBitmap.asImageBitmap(),
            contentDescription = "Fotografía de la junta",
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
                Icons.Outlined.LocationOn,
                contentDescription = "Sin fotografía",
                modifier = Modifier.size(44.dp)
            )
        }
    }
}

@Composable
private fun MeetupFormDialog(
    isSaving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onCreate: (String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var dateTime by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf("") }
    val context = LocalContext.current
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
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
    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("Crear junta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                error?.let { ErrorBanner(it) }
                AppInput("Título", title, enabled = !isSaving) { title = it }
                AppInput("Fecha y hora", dateTime, enabled = !isSaving) { dateTime = it }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Información de la junta") },
                    enabled = !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .appBikeTextFieldGlow(),
                    shape = RoundedCornerShape(14.dp),
                    colors = appBikeTextFieldColors()
                )
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    onClick = {
                        imagePicker.launch(arrayOf("image/jpeg", "image/png", "image/webp"))
                    }
                ) {
                    Text(
                        if (imageUri.isBlank()) "Agregar fotografía (opcional)"
                        else "Cambiar fotografía"
                    )
                }
            }
        },
        dismissButton = {
            OutlinedButton(enabled = !isSaving, onClick = onDismiss) { Text("Volver") }
        },
        confirmButton = {
            Button(
                enabled = !isSaving && title.isNotBlank() && dateTime.isNotBlank() &&
                    description.isNotBlank(),
                onClick = { onCreate(title, dateTime, description, imageUri) }
            ) {
                if (isSaving) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Publicar")
                }
            }
        }
    )
}

private fun GeoPoint.toLatLng() = LatLng(latitude, longitude)

private fun GeoPoint.shortLabel(): String = label
    .substringBefore(',')
    .trim()
    .ifBlank { coordinateLabel() }

private fun GeoPoint.coordinateLabel(): String = String.format(
    java.util.Locale.US,
    "%.5f, %.5f",
    latitude,
    longitude
)
