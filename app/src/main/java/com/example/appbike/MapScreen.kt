package com.example.appbike

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.LocationOn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class MeetupCreationStep { CLOSED, SELECT_LOCATION, FORM }
private const val OPEN_FREE_MAP_STYLE = "https://tiles.openfreemap.org/styles/liberty"

@Composable
fun RoutesScreen(account: AccountSession?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var center by remember {
        mutableStateOf(
            LocalDataStore.loadLocation(context)
                ?: GeoPoint(-33.4489, -70.6693, "Santiago")
        )
    }
    var query by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showLocationPrompt by remember {
        mutableStateOf(LocalDataStore.loadLocation(context) == null)
    }
    var showLoginRequired by remember { mutableStateOf(false) }
    var creationStep by remember { mutableStateOf(MeetupCreationStep.CLOSED) }
    var selectedPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    val meetups = remember { mutableStateListOf<MeetupEvent>() }

    fun refresh(point: GeoPoint = center) {
        scope.launch {
            isLoading = true
            error = null
            runCatching {
                withContext(Dispatchers.IO) {
                    RemoteConnections.loadNearbyMeetups(point, query, 40)
                }
            }.onSuccess {
                meetups.clear()
                meetups.addAll(it)
            }.onFailure { error = RemoteConnections.userFriendlyError(it) }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        if (!showLocationPrompt) refresh()
    }

    Box(Modifier.fillMaxSize()) {
        OpenStreetMap(
            modifier = Modifier.fillMaxSize(),
            center = center,
            meetups = meetups.toList(),
            selectedPoint = selectedPoint,
            creationStep = creationStep,
            onMapReady = { mapLibreMap = it },
            onCenterChanged = { point ->
                center = point
                LocalDataStore.saveLocation(context, point)
                refresh(point)
            },
            onPointSelected = { selectedPoint = it },
            onMapError = {
                error = "No fue posible cargar el mapa. Revisa tu conexión a internet."
            }
        )

        if (creationStep == MeetupCreationStep.CLOSED) {
            Icon(
                Icons.Outlined.LocationOn,
                contentDescription = "Ubicación central",
                modifier = Modifier.align(Alignment.Center).size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 14.dp, vertical = 10.dp),
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
                    IconButton(onClick = { refresh() }) {
                        Icon(Icons.Outlined.Search, contentDescription = "Buscar")
                    }
                },
                enabled = creationStep == MeetupCreationStep.CLOSED,
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { refresh() })
            )
        }

        if (isLoading) {
            Surface(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 88.dp),
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
                    .padding(top = 92.dp, start = 24.dp, end = 24.dp),
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

    if (showLocationPrompt) {
        LocationDialog(
            initial = "${center.latitude}, ${center.longitude}",
            onDismiss = { showLocationPrompt = false },
            onLocation = {
                center = it
                LocalDataStore.saveLocation(context, it)
                showLocationPrompt = false
                mapLibreMap?.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(it.toLatLng(), 11.0)
                )
                refresh(it)
            }
        )
    }

    if (creationStep == MeetupCreationStep.FORM && selectedPoint != null && account != null) {
        MeetupFormDialog(
            onDismiss = { creationStep = MeetupCreationStep.SELECT_LOCATION },
            onCreate = { title, dateTime, description ->
                scope.launch {
                    isLoading = true
                    val point = selectedPoint ?: return@launch
                    runCatching {
                        withContext(Dispatchers.IO) {
                            RemoteConnections.createMeetupEvent(
                                MeetupEvent(
                                    id = "",
                                    title = title,
                                    dateTime = dateTime,
                                    description = description,
                                    latitude = point.latitude,
                                    longitude = point.longitude,
                                    createdBy = account.userId
                                )
                            )
                        }
                    }.onSuccess {
                        creationStep = MeetupCreationStep.CLOSED
                        selectedPoint = null
                        refresh()
                    }.onFailure { error = RemoteConnections.userFriendlyError(it) }
                    isLoading = false
                }
            }
        )
    }
}

@Composable
private fun OpenStreetMap(
    modifier: Modifier,
    center: GeoPoint,
    meetups: List<MeetupEvent>,
    selectedPoint: GeoPoint?,
    creationStep: MeetupCreationStep,
    onMapReady: (MapLibreMap) -> Unit,
    onCenterChanged: (GeoPoint) -> Unit,
    onPointSelected: (GeoPoint) -> Unit,
    onMapError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = context as? LifecycleOwner
    val currentCenter = rememberUpdatedState(center)
    val currentCreationStep = rememberUpdatedState(creationStep)
    val currentOnMapReady = rememberUpdatedState(onMapReady)
    val currentOnCenterChanged = rememberUpdatedState(onCenterChanged)
    val currentOnPointSelected = rememberUpdatedState(onPointSelected)
    val currentOnMapError = rememberUpdatedState(onMapError)
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    val selectedIcon = remember(context) { createSelectedPointIcon(context) }
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
        var idleListener: MapLibreMap.OnCameraIdleListener? = null

        mapView.getMapAsync { readyMap ->
            if (destroyed) return@getMapAsync

            val newClickListener = MapLibreMap.OnMapClickListener { point ->
                if (currentCreationStep.value == MeetupCreationStep.SELECT_LOCATION) {
                    currentOnPointSelected.value(
                        GeoPoint(point.latitude, point.longitude)
                    )
                    true
                } else {
                    false
                }
            }
            val newIdleListener = MapLibreMap.OnCameraIdleListener {
                if (currentCreationStep.value != MeetupCreationStep.CLOSED) {
                    return@OnCameraIdleListener
                }
                val target = readyMap.cameraPosition.target ?: return@OnCameraIdleListener
                val saved = currentCenter.value
                if (
                    abs(target.latitude - saved.latitude) > 0.000001 ||
                    abs(target.longitude - saved.longitude) > 0.000001
                ) {
                    currentOnCenterChanged.value(
                        GeoPoint(target.latitude, target.longitude, saved.label)
                    )
                }
            }

            configuredMap = readyMap
            clickListener = newClickListener
            idleListener = newIdleListener
            readyMap.addOnMapClickListener(newClickListener)
            readyMap.addOnCameraIdleListener(newIdleListener)
            readyMap.uiSettings.isCompassEnabled = false
            readyMap.uiSettings.isZoomGesturesEnabled = true
            readyMap.uiSettings.isScrollGesturesEnabled = true
            readyMap.cameraPosition = CameraPosition.Builder()
                .target(currentCenter.value.toLatLng())
                .zoom(11.0)
                .build()
            readyMap.setStyle(OPEN_FREE_MAP_STYLE) {
                if (!destroyed) {
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
                idleListener?.let(configured::removeOnCameraIdleListener)
            }
            mapView.removeOnDidFailLoadingMapListener(failureListener)
            lifecycleOwner?.lifecycle?.removeObserver(lifecycleObserver)
            map = null
            destroyMapView()
        }
    }

    LaunchedEffect(map, meetups, selectedPoint) {
        val readyMap = map ?: return@LaunchedEffect
        readyMap.clear()
        meetups.forEach { event ->
            readyMap.addMarker(
                MarkerOptions()
                    .position(LatLng(event.latitude, event.longitude))
                    .title(event.title)
                    .snippet("${event.dateTime}\n${event.description}")
            )
        }
        selectedPoint?.let { point ->
            readyMap.addMarker(
                MarkerOptions()
                    .position(point.toLatLng())
                    .title("Punto de encuentro")
                    .icon(selectedIcon)
            )
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier
    )
}

private fun createSelectedPointIcon(context: android.content.Context) =
    IconFactory.getInstance(context).fromBitmap(
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
    )

@Composable
private fun LocationDialog(
    initial: String,
    onDismiss: () -> Unit,
    onLocation: (GeoPoint) -> Unit
) {
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf(initial) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Dónde estás?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Escribe una dirección o coordenadas latitud, longitud.")
                AppInput("Dirección o coordenadas", input) { input = it }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Ahora no") } },
        confirmButton = {
            Button(
                enabled = input.isNotBlank() && !loading,
                onClick = {
                    scope.launch {
                        loading = true
                        runCatching {
                            withContext(Dispatchers.IO) {
                                RemoteConnections.searchLocation(input)
                            }
                        }.onSuccess(onLocation)
                            .onFailure { error = RemoteConnections.userFriendlyError(it) }
                        loading = false
                    }
                }
            ) { Text(if (loading) "Buscando..." else "Usar ubicación") }
        }
    )
}

@Composable
private fun MeetupFormDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var dateTime by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
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
            }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Volver") } },
        confirmButton = {
            Button(
                enabled = title.isNotBlank() && dateTime.isNotBlank() &&
                    description.isNotBlank(),
                onClick = { onCreate(title, dateTime, description) }
            ) { Text("Publicar") }
        }
    )
}

private fun GeoPoint.toLatLng() = LatLng(latitude, longitude)
