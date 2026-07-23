package com.example.appbike

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import kotlin.math.roundToInt

private enum class MeetupCreationStep { CLOSED, SELECT_LOCATION, FORM }
private enum class LocationSetupStep { READY, REQUESTING_PERMISSION, LOCATING, CONFIRM, MANUAL }
private const val OPEN_FREE_MAP_STYLE = "https://tiles.openfreemap.org/styles/liberty"
private const val USER_SOURCE_ID = "appbike-user-source"
private const val USER_LAYER_ID = "appbike-user-layer"
private const val USER_ICON_ID = "appbike-user-icon"
private const val MEETUP_SOURCE_ID = "appbike-meetup-source"
private const val MEETUP_LAYER_ID = "appbike-meetup-layer"
private const val MEETUP_ICON_ID = "appbike-meetup-icon"
private const val SELECTED_SOURCE_ID = "appbike-selected-source"
private const val SELECTED_LAYER_ID = "appbike-selected-layer"
private const val SELECTED_ICON_ID = "appbike-selected-icon"
private val DEFAULT_MAP_CENTER = GeoPoint(-33.4489, -70.6693, "Santiago")

@Composable
fun RoutesScreen(account: AccountSession?, onOpenChat: (UserChat) -> Unit = {}) {
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
    var query by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showLoginRequired by remember { mutableStateOf(false) }
    var creationStep by remember { mutableStateOf(MeetupCreationStep.CLOSED) }
    var selectedPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var meetupDetail by remember { mutableStateOf<MeetupEvent?>(null) }
    var meetupDetailLoading by remember { mutableStateOf(false) }
    var meetupDetailError by remember { mutableStateOf<String?>(null) }
    val meetups = remember { mutableStateListOf<MeetupEvent>() }

    fun refresh(point: GeoPoint = center) {
        scope.launch {
            isLoading = true
            error = null
            runCatching {
                withContext(Dispatchers.IO) {
                    RemoteConnections.loadNearbyMeetups(
                        center = point,
                        query = query,
                        radiusKm = 40,
                        status = "activa"
                    )
                }
            }.onSuccess {
                meetups.clear()
                meetups.addAll(it)
            }.onFailure { error = RemoteConnections.userFriendlyError(it) }
            isLoading = false
        }
    }

    fun openMeetup(event: MeetupEvent) {
        scope.launch {
            meetupDetailLoading = true
            meetupDetailError = null
            runCatching {
                withContext(Dispatchers.IO) {
                    RemoteConnections.loadMeetupDetails(event.id)
                }
            }.onSuccess { meetupDetail = it }
                .onFailure { meetupDetailError = RemoteConnections.userFriendlyError(it) }
            meetupDetailLoading = false
        }
    }

    fun completeMeetup(event: MeetupEvent) {
        scope.launch {
            meetupDetailLoading = true
            meetupDetailError = null
            runCatching {
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
            runCatching {
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
            meetupDetailError = "Inicia sesión para contactar al organizador."
            return
        }
        scope.launch {
            meetupDetailLoading = true
            meetupDetailError = null
            runCatching {
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

    fun commitLocation(point: GeoPoint) {
        userLocation = point
        pendingLocation = null
        center = point
        locationSetupMessage = null
        locationSetupStep = LocationSetupStep.READY
        LocalDataStore.saveLocation(context, point)
        refresh(point)
        scope.launch {
            val resolved = withContext(Dispatchers.IO) {
                RemoteConnections.resolveCommunityLocation(point)
            }
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
            runCatching {
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

    LaunchedEffect(Unit) {
        if (storedLocation == null) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(storedLocation) {
        storedLocation?.let(::refresh)
    }

    val displayedLocation = pendingLocation ?: userLocation

    Box(Modifier.fillMaxSize()) {
        OpenStreetMap(
            modifier = Modifier.fillMaxSize(),
            center = center,
            userLocation = displayedLocation,
            meetups = meetups.toList(),
            selectedPoint = selectedPoint,
            creationStep = creationStep,
            onMapReady = {},
            onPointSelected = { selectedPoint = it },
            onMeetupSelected = ::openMeetup,
            onMapError = {
                error = "No fue posible cargar el mapa. Revisa tu conexión a internet."
            }
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 8.dp
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            if (creationStep == MeetupCreationStep.SELECT_LOCATION) {
                                "Toca el mapa para marcar el punto"
                            } else {
                                "Buscar junta"
                            }
                        )
                    },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { userLocation?.let(::refresh) }) {
                            Icon(Icons.Outlined.Search, contentDescription = "Buscar")
                        }
                    },
                    enabled = creationStep == MeetupCreationStep.CLOSED && userLocation != null,
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { userLocation?.let(::refresh) }
                    )
                )
            }

            CurrentLocationRow(
                location = displayedLocation,
                locating = locationSetupStep == LocationSetupStep.LOCATING ||
                    locationSetupStep == LocationSetupStep.REQUESTING_PERMISSION,
                enabled = creationStep == MeetupCreationStep.CLOSED &&
                    locationSetupStep != LocationSetupStep.LOCATING,
                onClick = {
                    locationSetupMessage = null
                    locationSetupStep = LocationSetupStep.MANUAL
                }
            )

        }

        if (isLoading) {
            Surface(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 132.dp),
                shape = CircleShape,
                shadowElevation = 4.dp
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(10.dp).size(24.dp),
                    strokeWidth = 2.dp
                )
            }
        }

        error?.let {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 136.dp, start = 24.dp, end = 24.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                shadowElevation = 4.dp
            ) {
                Text(
                    it,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        if (creationStep == MeetupCreationStep.SELECT_LOCATION) {
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                shape = RoundedCornerShape(22.dp),
                shadowElevation = 10.dp
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
                            containerColor = Color(0xFF246BCE)
                        ),
                        onClick = { creationStep = MeetupCreationStep.FORM }
                    ) { Text("Confirmar") }
                }
            }
        } else {
            FloatingActionButton(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp),
                shape = CircleShape,
                onClick = {
                    if (account == null) showLoginRequired = true
                    else creationStep = MeetupCreationStep.SELECT_LOCATION
                }
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Crear junta")
            }
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
    }

    if (showLoginRequired) {
        AlertDialog(
            onDismissRequest = { showLoginRequired = false },
            title = { Text("Inicio de sesión necesario") },
            text = { Text("Inicia sesión para crear una junta.") },
            confirmButton = {
                Button(onClick = { showLoginRequired = false }) { Text("Entendido") }
            }
        )
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
            onDismiss = { creationStep = MeetupCreationStep.SELECT_LOCATION },
            onCreate = { title, dateTime, description, imageUri ->
                scope.launch {
                    isLoading = true
                    val point = selectedPoint ?: return@launch
                    runCatching {
                        withContext(Dispatchers.IO) {
                            val region = RemoteConnections.communityRegionFor(
                                userLocation ?: point
                            )
                            RemoteConnections.createMeetupEvent(
                                context,
                                MeetupEvent(
                                    id = "",
                                    title = title,
                                    dateTime = dateTime,
                                    description = buildString {
                                        if (dateTime.isNotBlank()) {
                                            append("Fecha y hora: ")
                                            append(dateTime)
                                            append('\n')
                                        }
                                        append(description)
                                    },
                                    latitude = point.latitude,
                                    longitude = point.longitude,
                                    createdBy = account.userId,
                                    createdByUsername = account.username,
                                    region = region,
                                    location = userLocation?.label.orEmpty(),
                                    imageUri = imageUri,
                                    countryCode = userLocation?.countryCode.orEmpty(),
                                    administrativeArea = userLocation?.administrativeArea.orEmpty()
                                )
                            )
                        }
                    }.onSuccess {
                        creationStep = MeetupCreationStep.CLOSED
                        selectedPoint = null
                        userLocation?.let(::refresh)
                    }.onFailure { error = RemoteConnections.userFriendlyError(it) }
                    isLoading = false
                }
            }
        )
    }

    meetupDetail?.let { event ->
        MeetupDetailDialog(
            event = event,
            account = account,
            loading = meetupDetailLoading,
            error = meetupDetailError,
            onDismiss = {
                meetupDetail = null
                meetupDetailError = null
            },
            onComplete = { completeMeetup(event) },
            onAddPhoto = { imageUri -> addMeetupPhoto(event, imageUri) },
            onContact = { contactOrganizer(event) }
        )
    }
}

@Composable
private fun OpenStreetMap(
    modifier: Modifier,
    center: GeoPoint,
    userLocation: GeoPoint?,
    meetups: List<MeetupEvent>,
    selectedPoint: GeoPoint?,
    creationStep: MeetupCreationStep,
    onMapReady: (MapLibreMap) -> Unit,
    onPointSelected: (GeoPoint) -> Unit,
    onMeetupSelected: (MeetupEvent) -> Unit,
    onMapError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = context as? LifecycleOwner
    val currentCenter = rememberUpdatedState(center)
    val currentCreationStep = rememberUpdatedState(creationStep)
    val currentOnMapReady = rememberUpdatedState(onMapReady)
    val currentOnPointSelected = rememberUpdatedState(onPointSelected)
    val currentMeetups = rememberUpdatedState(meetups)
    val currentOnMeetupSelected = rememberUpdatedState(onMeetupSelected)
    val currentOnMapError = rememberUpdatedState(onMapError)
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    val selectedIcon = remember(context) { createSelectedPointIcon(context) }
    val userLocationIcon = remember(context) { createUserLocationIcon(context) }
    val meetupIcon = remember(context) { createMeetupIcon(context) }
    val mapView = remember(context) {
        MapLibre.getInstance(context.applicationContext)
        MapView(context).apply { onCreate(null) }
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
            readyMap.uiSettings.isZoomGesturesEnabled = true
            readyMap.uiSettings.isScrollGesturesEnabled = true
            readyMap.cameraPosition = CameraPosition.Builder()
                .target(currentCenter.value.toLatLng())
                .zoom(13.0)
                .build()
            readyMap.setStyle(OPEN_FREE_MAP_STYLE) { style ->
                if (!destroyed) {
                    style.addImage(USER_ICON_ID, userLocationIcon)
                    style.addImage(MEETUP_ICON_ID, meetupIcon)
                    style.addImage(SELECTED_ICON_ID, selectedIcon)
                    style.addSource(GeoJsonSource(USER_SOURCE_ID, emptyFeatureCollection()))
                    style.addSource(GeoJsonSource(MEETUP_SOURCE_ID, emptyFeatureCollection()))
                    style.addSource(GeoJsonSource(SELECTED_SOURCE_ID, emptyFeatureCollection()))
                    style.addLayer(symbolLayer(USER_LAYER_ID, USER_SOURCE_ID, USER_ICON_ID))
                    style.addLayer(symbolLayer(MEETUP_LAYER_ID, MEETUP_SOURCE_ID, MEETUP_ICON_ID))
                    style.addLayer(symbolLayer(SELECTED_LAYER_ID, SELECTED_SOURCE_ID, SELECTED_ICON_ID))
                    map = readyMap
                    currentOnMapReady.value(readyMap)
                }
            }
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
            destroyMapView()
        }
    }

    LaunchedEffect(map, center) {
        map?.animateCamera(
            CameraUpdateFactory.newLatLngZoom(center.toLatLng(), 13.0)
        )
    }

    LaunchedEffect(map, userLocation, meetups, selectedPoint) {
        val readyMap = map ?: return@LaunchedEffect
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
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier
    )
}

private fun createSelectedPointIcon(context: android.content.Context): Bitmap =
        Bitmap.createBitmap(
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
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
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

private fun emptyFeatureCollection(): FeatureCollection =
    FeatureCollection.fromFeatures(emptyList<Feature>())

@Composable
private fun CurrentLocationRow(
    location: GeoPoint?,
    locating: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
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
    onDismiss: () -> Unit,
    onLocation: (GeoPoint) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrimInteractionSource = remember { MutableInteractionSource() }
    val panelInteractionSource = remember { MutableInteractionSource() }
    val recentLocations = remember { LocalDataStore.loadLocationHistory(context) }
    var input by remember { mutableStateOf(initial) }
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
                .clickable(
                    interactionSource = panelInteractionSource,
                    indication = null,
                    onClick = {}
                ),
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
                    "Introduce tu ubicación",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text("Busca una ciudad, dirección o lugar.")
                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        input = it
                        hasEditedInput = true
                        error = null
                        results = emptyList()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Lugar o dirección") },
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

@Composable
private fun MeetupDetailDialog(
    event: MeetupEvent,
    account: AccountSession?,
    loading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onComplete: () -> Unit,
    onAddPhoto: (String) -> Unit,
    onContact: () -> Unit
) {
    val context = LocalContext.current
    val isOwner = account?.userId == event.createdBy
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

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(max = 760.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
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
                    MeetupRemoteImage(null)
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
                        enabled = !loading && account != null,
                        onClick = onContact
                    ) {
                        Text(if (account == null) "Inicia sesión para contactar" else "Contactar al organizador")
                    }
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDismiss
                ) { Text("Cerrar") }
            }
        }
    }
}

@Composable
private fun MeetupRemoteImage(
    source: String?,
    modifier: Modifier = Modifier.fillMaxWidth().height(190.dp)
) {
    var bitmap by remember(source) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(source) {
        bitmap = if (source.isNullOrBlank()) {
            null
        } else {
            runCatching {
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
        onDismissRequest = onDismiss,
        title = { Text("Crear junta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AppInput("Título", title) { title = it }
                AppInput("Fecha y hora", dateTime) { dateTime = it }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Información de la junta") },
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    shape = RoundedCornerShape(16.dp)
                )
                Button(
                    modifier = Modifier.fillMaxWidth(),
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
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Volver") } },
        confirmButton = {
            Button(
                enabled = title.isNotBlank() && dateTime.isNotBlank() &&
                    description.isNotBlank(),
                onClick = { onCreate(title, dateTime, description, imageUri) }
            ) { Text("Publicar") }
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
