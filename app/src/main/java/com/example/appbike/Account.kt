package com.example.appbike

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PedalBike
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import com.example.appbike.ui.theme.AppBackground
import com.example.appbike.ui.theme.AppAccentBlue
import com.example.appbike.ui.theme.AppAccentOrange
import com.example.appbike.ui.theme.AppBorderSubtle
import com.example.appbike.ui.theme.AppErrorSoft
import com.example.appbike.ui.theme.AppPrimary
import com.example.appbike.ui.theme.AppPrimaryBright
import com.example.appbike.ui.theme.AppPrimarySoft
import com.example.appbike.ui.theme.AppSurface
import com.example.appbike.ui.theme.AppSurfaceElevated
import com.example.appbike.ui.theme.AppTextPrimary
import com.example.appbike.ui.theme.AppTextSecondary
import com.example.appbike.ui.theme.AppWarning
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.content.edit
import androidx.core.net.toUri

object AccountStore {
    private const val PREFS = "appbike_account"
    private const val USER_ID = "user_id"
    private const val EMAIL = "email"
    private const val USERNAME = "username"

    fun loadSession(context: Context): AccountSession? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val userId = prefs.all[USER_ID] as? String ?: ""
        val email = prefs.all[EMAIL] as? String ?: ""
        return if (isValidAccountUserId(userId) && email.isNotBlank()) {
            AccountSession(
                userId = userId,
                email = email,
                accessToken = SecureTokenStore.load(context),
                username = prefs.all[USERNAME] as? String
            )
        } else {
            if (prefs.contains(USER_ID)) clearSession(context)
            null
        }
    }

    fun saveSession(context: Context, session: AccountSession) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putString(USER_ID, session.userId)
            putString(EMAIL, session.email)
            putString(USERNAME, session.username)
        }
        SecureTokenStore.save(context, session.accessToken)
    }

    fun clearSession(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            remove(USER_ID)
            remove(EMAIL)
            remove(USERNAME)
        }
        SecureTokenStore.clear(context)
    }

}

internal fun isValidAccountUserId(value: String): Boolean =
    Regex(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-" +
            "[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$"
    ).matches(value)

internal const val AUTH_ACCESS_SCREEN_TEST_TAG = "auth_access_screen"

@Composable
internal fun UnauthenticatedAccessScreen(
    initialErrorMessage: String? = null,
    onLogin: (AccountSession) -> Unit
) {
    var identity by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf(initialErrorMessage) }
    var showRegistrationNotice by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val windowHeight = LocalWindowInfo.current.containerSize.height
    val compactHeight = with(LocalDensity.current) { windowHeight.toDp() < 720.dp }

    LaunchedEffect(initialErrorMessage) {
        if (!initialErrorMessage.isNullOrBlank()) {
            errorMessage = initialErrorMessage
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag(AUTH_ACCESS_SCREEN_TEST_TAG)
    ) {
        Image(
            painter = painterResource(R.drawable.auth_mtb_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.18f),
                            AppBackground.copy(alpha = 0.48f),
                            AppBackground.copy(alpha = 0.97f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = Color.Black.copy(alpha = 0.72f),
                    border = BorderStroke(1.dp, AppPrimaryBright.copy(alpha = 0.60f))
                ) {
                    AppBrandLogo(
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        "APPBIKE",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = AppTextPrimary
                    )
                    Text(
                        "RIDE  •  CONNECT",
                        style = MaterialTheme.typography.labelMedium,
                        color = AppPrimaryBright
                    )
                }
            }

            Spacer(Modifier.height(if (compactHeight) 20.dp else 70.dp))

            Text(
                "ACCESO RIDER",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = AppPrimaryBright,
                letterSpacing = 1.2.sp
            )
            Text(
                "Vuelve a rodar",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Text(
                "Inicia sesión para recuperar tus bicicletas, rutas y comunidad.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.82f)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = AppSurfaceElevated.copy(alpha = 0.91f)
                ),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, AppPrimaryBright.copy(alpha = 0.38f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "Iniciar sesión",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = AppTextPrimary
                    )
                    AppInput(
                        label = "Correo o nombre de usuario",
                        value = identity,
                        onChange = {
                            identity = it
                            errorMessage = null
                        }
                    )
                    AppInput(
                        label = "Contraseña",
                        value = password,
                        visualTransformation = PasswordVisualTransformation(),
                        onChange = {
                            password = it
                            errorMessage = null
                        }
                    )

                    errorMessage?.let { ErrorBanner(it) }

                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        enabled = identity.isNotBlank() && password.isNotBlank() && !isLoading,
                        onClick = {
                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                runSuspendCatching {
                                    withContext(Dispatchers.IO) {
                                        RemoteConnections.login(identity, password)
                                    }
                                }.onSuccess { authenticatedSession ->
                                    identity = ""
                                    password = ""
                                    onLogin(authenticatedSession)
                                }.onFailure {
                                    errorMessage = RemoteConnections.userFriendlyError(it)
                                }
                                isLoading = false
                            }
                        }
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Iniciar sesión")
                        }
                    }

                    OutlinedButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = !isLoading,
                        onClick = { showRegistrationNotice = true }
                    ) {
                        Text("Crear cuenta")
                    }
                }
            }

        }
    }

    if (showRegistrationNotice) {
        AlertDialog(
            onDismissRequest = { showRegistrationNotice = false },
            title = { Text("Crear cuenta") },
            text = {
                Text(
                    "El registro automático estará disponible cuando el servidor APPBIKE " +
                        "habilite la creación segura de cuentas. No enviaremos tus datos " +
                        "a una acción que todavía no existe."
                )
            },
            confirmButton = {
                TextButton(onClick = { showRegistrationNotice = false }) {
                    Text("Entendido")
                }
            }
        )
    }
}

@Composable
fun AccountScreen(
    session: AccountSession?,
    platforms: MutableList<SyncPlatform>,
    isActive: Boolean = true,
    bikeCount: Int = 0,
    bikes: List<Bike> = emptyList(),
    onBikesLoaded: (String, List<Bike>) -> Unit = { _, _ -> },
    onOpenBikes: () -> Unit = {},
    onOpenRoutes: () -> Unit = {},
    initialErrorMessage: String? = null,
    oauthCallback: SportsOAuthCallback? = null,
    onOauthCallbackConsumed: () -> Unit = {},
    onLogin: (AccountSession) -> Unit,
    onSessionUpdated: (AccountSession) -> Unit,
    onProfilePhotoChanged: (String) -> Unit = {},
    onLogout: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember(session?.userId) { mutableStateOf(initialErrorMessage) }
    var profileCheckedUserId by remember { mutableStateOf<String?>(null) }
    var usernameCandidate by remember(session?.userId) { mutableStateOf("") }
    var usernameLoading by remember { mutableStateOf(false) }
    var usernameError by remember { mutableStateOf<String?>(null) }
    var profileBio by remember(session?.userId) { mutableStateOf("") }
    var profilePhotoUri by remember(session?.userId) { mutableStateOf("") }
    var profileBioDraft by remember(session?.userId) { mutableStateOf("") }
    var profileEditing by remember(session?.userId) { mutableStateOf(false) }
    var socialPostCount by remember(session?.userId) { mutableIntStateOf(0) }
    var routeCount by remember(session?.userId) { mutableIntStateOf(0) }
    var profileBikesLoading by remember(session?.userId) { mutableStateOf(false) }
    var profileBikesError by remember(session?.userId) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val profilePhotoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        val activeUserId = session?.userId ?: return@rememberLauncherForActivityResult
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        profilePhotoUri = uri.toString()
        LocalDataStore.saveProfilePhotoUri(context, activeUserId, profilePhotoUri)
        onProfilePhotoChanged(profilePhotoUri)
    }

    LaunchedEffect(initialErrorMessage, session?.userId) {
        if (session == null && !initialErrorMessage.isNullOrBlank()) {
            errorMessage = initialErrorMessage
        }
    }

    LaunchedEffect(session?.userId, isActive) {
        if (!isActive) return@LaunchedEffect
        val activeSession = session ?: run {
            profileCheckedUserId = null
            return@LaunchedEffect
        }
        if (activeSession.username.isNullOrBlank()) {
            runSuspendCatching {
                withContext(Dispatchers.IO) {
                    RemoteConnections.loadUserProfile(activeSession)
                }
            }.onSuccess(onSessionUpdated)
        }
        profileCheckedUserId = activeSession.userId
    }

    LaunchedEffect(session?.userId) {
        val activeUserId = session?.userId
        if (activeUserId == null) {
            profileBio = ""
            profileBioDraft = ""
            profilePhotoUri = ""
        } else {
            profileBio = LocalDataStore.loadProfileBio(context, activeUserId)
            profileBioDraft = profileBio
            profilePhotoUri = LocalDataStore.loadProfilePhotoUri(context, activeUserId)
        }
        profileEditing = false
        routeCount = 0
    }

    LaunchedEffect(session?.userId, isActive) {
        val activeSession = session ?: return@LaunchedEffect
        if (!isActive || bikes.isNotEmpty()) return@LaunchedEffect
        profileBikesLoading = true
        profileBikesError = null
        runSuspendCatching {
            withContext(Dispatchers.IO) {
                RemoteConnections.loadUserBikes(activeSession.userId)
            }
        }.onSuccess { loadedBikes ->
            if (session.userId == activeSession.userId) {
                onBikesLoaded(activeSession.userId, loadedBikes)
            }
        }.onFailure { error ->
            profileBikesError = RemoteConnections.userFriendlyError(error)
        }
        profileBikesLoading = false
    }

    LaunchedEffect(oauthCallback) {
        if (oauthCallback == null) return@LaunchedEffect
        onOauthCallbackConsumed()
    }

    PremiumScreenBackground(PremiumGlowStyle.Account) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 18.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (session == null) {
                AccountReferenceHeader(session)
            } else {
                RiderProfileHeader(
                    session = session,
                    bio = profileBio,
                    photoUri = profilePhotoUri,
                    bikeCount = bikeCount,
                    socialPostCount = socialPostCount,
                    routeCount = routeCount,
                    isEditing = profileEditing,
                    bioDraft = profileBioDraft,
                    onEditBio = {
                        profileBioDraft = profileBio
                        profileEditing = true
                    },
                    onBioDraftChange = { profileBioDraft = it.take(160) },
                    onCancelBio = {
                        profileBioDraft = profileBio
                        profileEditing = false
                    },
                    onSaveBio = {
                        LocalDataStore.saveProfileBio(context, session.userId, profileBioDraft)
                        profileBio = profileBioDraft.trim()
                        profileEditing = false
                    },
                    onPickPhoto = {
                        profilePhotoPicker.launch(arrayOf("image/*"))
                    },
                    onOpenBikes = onOpenBikes
                )
            }

        if (session == null) {
            LoginCard(
                email = email,
                password = password,
                isLoading = isLoading,
                errorMessage = errorMessage,
                onEmailChange = { email = it },
                onPasswordChange = { password = it },
                onSubmit = {
                    scope.launch {
                        isLoading = true
                        errorMessage = null
                        runSuspendCatching {
                            withContext(Dispatchers.IO) {
                                RemoteConnections.login(email, password)
                            }
                        }.onSuccess { authenticatedSession ->
                            email = ""
                            password = ""
                            onLogin(authenticatedSession)
                        }
                            .onFailure {
                                errorMessage = RemoteConnections.userFriendlyError(it)
                            }
                        isLoading = false
                    }
                }
            )
        } else {
            ProfileActionsCard(
                session = session,
                notificationsEnabled = ChatNotificationCenter.canPostNotifications(context),
                onOpenNotificationSettings = {
                    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    } else {
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            "package:${context.packageName}".toUri()
                        )
                    }
                    context.startActivity(intent)
                },
                onLogout = onLogout
            )
        }

        if (session != null) {
            SectionHeader(title = "Rendimiento")
            Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.Space3)) {
                SportMetricCard(
                    value = "—",
                    label = "Kilómetros",
                    supporting = "Sin actividad",
                    modifier = Modifier.weight(1f)
                )
                SportMetricCard(
                    value = "—",
                    label = "Tiempo total",
                    supporting = "Sin actividad",
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.Space3)) {
                SportMetricCard(
                    value = "—",
                    label = "Desnivel",
                    supporting = "Sin actividad",
                    modifier = Modifier.weight(1f)
                )
                SportMetricCard(
                    value = bikeCount.toString(),
                    label = "Bicicletas",
                    supporting = "En tu garaje",
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                text = "Las métricas deportivas aparecerán aquí cuando la sincronización de actividades esté disponible.",
                color = AppTextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (session == null) {
            AccountHeroPanel()
        }

        HorizontalDivider(color = AppBorderSubtle)

        AppCard(modifier = Modifier.fillMaxWidth(), highlighted = true) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Space3),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = AppAccentOrange.copy(alpha = 0.16f)
                ) {
                    Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Sync,
                            contentDescription = null,
                            tint = AppAccentOrange
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Conecta tu mundo rider",
                        style = MaterialTheme.typography.titleLarge,
                        color = AppTextPrimary
                    )
                    Text(
                        text = "Centraliza tus salidas, kilómetros y entrenamientos cuando la integración esté lista.",
                        color = AppTextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(Modifier.height(AppDimens.Space3))
            Surface(
                shape = RoundedCornerShape(AppDimens.RadiusPill),
                color = AppAccentOrange.copy(alpha = 0.16f)
            ) {
                Text(
                    "STRAVA · PRÓXIMAMENTE",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = AppAccentOrange,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        platforms.forEach { platform -> SportsPlatformCard(platform) }

        Text(
            "Strava será la primera integración visible. Garmin y Wahoo quedan reservados para una etapa posterior, sin solicitar permisos todavía.",
            color = AppTextSecondary,
            style = MaterialTheme.typography.bodySmall
        )

        if (session != null) {
            HorizontalDivider(color = AppBorderSubtle)
            if (profileBikesLoading) {
                AppSkeleton(Modifier.fillMaxWidth().height(82.dp))
            }
            profileBikesError?.let { ErrorBanner(it) }
            ProfileContentSection(
                account = session,
                bikes = bikes,
                isActive = isActive,
                onSocialPostCountChanged = { socialPostCount = it },
                onMeetupCountChanged = { routeCount = it },
                onOpenBikes = onOpenBikes,
                onOpenRoutes = onOpenRoutes
            )
            HorizontalDivider(color = AppBorderSubtle)
        }

            Spacer(Modifier.height(12.dp))
        }
    }

    if (
        session != null &&
        profileCheckedUserId == session.userId &&
        session.username.isNullOrBlank()
    ) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Elige tu nombre de usuario") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Este nombre aparecerá en mensajes, juntas y publicaciones. " +
                            "También podrás iniciar sesión con él."
                    )
                    OutlinedTextField(
                        value = usernameCandidate,
                        onValueChange = {
                            usernameCandidate = it.take(30)
                            usernameError = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .appBikeTextFieldGlow(),
                        singleLine = true,
                        label = { Text("Nombre de usuario") },
                        supportingText = {
                            Text("3 a 30 caracteres: letras, números, punto, _ o -")
                        },
                        isError = usernameError != null,
                        shape = RoundedCornerShape(14.dp),
                        colors = appBikeTextFieldColors()
                    )
                    usernameError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = isValidUsername(usernameCandidate) && !usernameLoading,
                    onClick = {
                        scope.launch {
                            usernameLoading = true
                            usernameError = null
                            runSuspendCatching {
                                withContext(Dispatchers.IO) {
                                    RemoteConnections.updateUsername(
                                        session,
                                        usernameCandidate
                                    )
                                }
                            }.onSuccess(onSessionUpdated)
                                .onFailure {
                                    usernameError = RemoteConnections.userFriendlyError(it)
                                }
                            usernameLoading = false
                        }
                    }
                ) {
                    if (usernameLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Guardar")
                    }
                }
            },
            dismissButton = {
                Text(
                    text = "Requerido",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    color = AppTextSecondary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        )
    }
}

private fun isValidUsername(value: String): Boolean =
    Regex("^[A-Za-z0-9][A-Za-z0-9._-]{2,29}$").matches(value.trim())

@Composable
private fun RiderProfileHeader(
    session: AccountSession,
    bio: String,
    photoUri: String,
    bikeCount: Int,
    socialPostCount: Int,
    routeCount: Int,
    isEditing: Boolean,
    bioDraft: String,
    onEditBio: () -> Unit,
    onBioDraftChange: (String) -> Unit,
    onCancelBio: () -> Unit,
    onSaveBio: () -> Unit,
    onPickPhoto: () -> Unit,
    onOpenBikes: () -> Unit
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        highlighted = true
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Space4),
            verticalAlignment = Alignment.Top
        ) {
            ProfileAvatar(
                photoUri = photoUri,
                displayName = session.username ?: session.email,
                modifier = Modifier.size(92.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppDimens.Space1)
            ) {
                Text(
                    text = session.username ?: "Rider APPBIKE",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = AppTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "@${session.username ?: session.email.substringBefore('@')}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppPrimaryBright,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = bio.ifBlank { "Añade una bio para contarle a la comunidad quién eres." },
                    modifier = Modifier.padding(top = 7.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (bio.isBlank()) AppTextSecondary else AppTextPrimary
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppDimens.Space4),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Space2)
        ) {
            ProfileStat(value = socialPostCount.toString(), label = "Posts", Modifier.weight(1f))
            ProfileStat(value = bikeCount.toString(), label = "Bicis", Modifier.weight(1f))
            ProfileStat(value = routeCount.toString(), label = "Rutas", Modifier.weight(1f))
        }

        if (isEditing) {
            OutlinedTextField(
                value = bioDraft,
                onValueChange = onBioDraftChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppDimens.Space4)
                    .appBikeTextFieldGlow(),
                label = { Text("Tu bio") },
                supportingText = { Text("Hasta 160 caracteres") },
                minLines = 3,
                maxLines = 5,
                colors = appBikeTextFieldColors(),
                shape = RoundedCornerShape(AppDimens.RadiusMedium)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppDimens.Space2),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCancelBio) { Text("Cancelar") }
                Button(onClick = onSaveBio) { Text("Guardar bio") }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppDimens.Space3),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Space2),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onPickPhoto
                ) {
                    Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text("Cambiar foto")
                }
                TextButton(onClick = onEditBio) {
                    Text(if (bio.isBlank()) "Añadir bio" else "Editar bio")
                }
            }
        }

        TextButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onOpenBikes
        ) {
            Icon(Icons.Outlined.PedalBike, contentDescription = null)
            Spacer(Modifier.size(6.dp))
            Text("Abrir Mi garaje")
        }
    }
}

@Composable
private fun ProfileStat(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(AppDimens.RadiusMedium),
        color = AppSurface,
        border = BorderStroke(1.dp, AppBorderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(label, style = MaterialTheme.typography.labelSmall, color = AppTextSecondary)
        }
    }
}

@Composable
internal fun ProfileAvatar(
    photoUri: String,
    displayName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember(photoUri) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(photoUri) {
        bitmap = if (photoUri.isBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(photoUri.toUri())?.use(
                        BitmapFactory::decodeStream
                    )
                }.getOrNull()
            }
        }
    }
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = AppPrimary,
        border = BorderStroke(2.dp, AppPrimaryBright.copy(alpha = 0.80f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            val loaded = bitmap
            if (loaded != null) {
                Image(
                    bitmap = loaded.asImageBitmap(),
                    contentDescription = "Foto de perfil de $displayName",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = displayName.firstOrNull()?.uppercase() ?: "R",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
private fun ProfileActionsCard(
    session: AccountSession,
    notificationsEnabled: Boolean,
    onOpenNotificationSettings: () -> Unit,
    onLogout: () -> Unit
) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Text("Cuenta y privacidad", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "Sesión activa para ${session.email}",
            modifier = Modifier.padding(top = 4.dp),
            color = AppTextSecondary,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!notificationsEnabled) {
            Surface(
                modifier = Modifier.padding(top = AppDimens.Space3),
                shape = RoundedCornerShape(AppDimens.RadiusMedium),
                color = AppErrorSoft
            ) {
                Column(
                    modifier = Modifier.padding(AppDimens.Space3),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.Space1)
                ) {
                    Text(
                        "Notificaciones desactivadas",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        "Actívalas para recibir mensajes cuando APPBIKE esté en segundo plano.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    TextButton(onClick = onOpenNotificationSettings) {
                        Text("Abrir ajustes")
                    }
                }
            }
        }
        OutlinedButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppDimens.Space3),
            onClick = onLogout
        ) {
            Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Cerrar sesión")
        }
    }
}

@Composable
private fun AccountReferenceHeader(session: AccountSession?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "Tu perfil",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black
            )
            Text(
                session?.username ?: session?.email ?: "Perfil APPBIKE",
                style = MaterialTheme.typography.bodyMedium,
                color = AppTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Surface(
            shape = CircleShape,
            color = AppSurfaceElevated,
            border = BorderStroke(1.dp, AppBorderSubtle)
        ) {
            Box(
                modifier = Modifier.size(52.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.PersonOutline,
                    contentDescription = null,
                    tint = AppPrimaryBright
                )
            }
        }
    }
}

@Composable
private fun AccountHeroPanel() {
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = AppDimens.Space2),
        highlighted = true
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.Space4)) {
        Surface(
            shape = CircleShape,
                color = AppSurfaceElevated,
                border = BorderStroke(1.dp, AppBorderSubtle)
        ) {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                        imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                        tint = AppTextPrimary
                )
            }
        }
            Text(
                text = "Descubre lo lejos que puedes llegar",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = AppTextPrimary
            )
            Text(
                text = "Controla tu progreso, tus rutas, tus bicicletas y tu comunidad desde APPBIKE.",
                style = MaterialTheme.typography.bodyLarge,
                color = AppTextSecondary
            )
        }
    }
}

@Composable
private fun LoginCard(
    email: String,
    password: String,
    isLoading: Boolean,
    errorMessage: String?,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = AppSurfaceElevated
        ),
        shape = RoundedCornerShape(AppDimens.RadiusXLarge),
        border = BorderStroke(1.dp, AppBorderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.Space5),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Space4)
        ) {
            Surface(
                shape = CircleShape,
                color = AppPrimarySoft
            ) {
                Box(
                    modifier = Modifier.size(52.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PersonOutline,
                        contentDescription = null,
                        tint = AppPrimaryBright
                    )
                }
            }
            Text(
                "Bienvenido",
                style = MaterialTheme.typography.headlineMedium,
                color = AppTextPrimary
            )
            Text(
                "Inicia sesión para cargar tus bicicletas y mantenciones personales.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AppInput("Correo o nombre de usuario", email, onChange = onEmailChange)
            AppInput(
                label = "Contraseña",
                value = password,
                visualTransformation = PasswordVisualTransformation(),
                onChange = onPasswordChange
            )

            errorMessage?.let { ErrorBanner(it) }

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = email.isNotBlank() && password.isNotBlank() && !isLoading,
                onClick = onSubmit
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Iniciar sesión")
                }
            }
        }
    }
}

@Composable
private fun ProfileCard(
    session: AccountSession,
    notificationsEnabled: Boolean,
    onOpenNotificationSettings: () -> Unit,
    onLogout: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = AppSurfaceElevated
        ),
        shape = RoundedCornerShape(AppDimens.RadiusXLarge),
        border = BorderStroke(1.dp, AppBorderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.Space5),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Space4)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = AppPrimary
                ) {
                    Box(
                        modifier = Modifier.size(54.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PersonOutline,
                            contentDescription = null,
                            tint = AppTextPrimary
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Sesión activa",
                        style = MaterialTheme.typography.labelLarge,
                        color = AppPrimaryBright
                    )
                    Text(
                        session.username ?: session.email,
                        style = MaterialTheme.typography.titleLarge,
                        color = AppTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!session.username.isNullOrBlank()) {
                        Text(
                            session.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = "Cuenta conectada",
                    tint = AppPrimaryBright
                )
            }
            Text(
                "Tus bicicletas y mantenciones se cargan de forma privada desde esta cuenta.",
                color = AppTextSecondary
            )
            if (!notificationsEnabled) {
                Surface(
                    shape = RoundedCornerShape(AppDimens.RadiusMedium),
                    color = AppErrorSoft
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "Notificaciones desactivadas",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            "Actívalas para recibir mensajes cuando APPBIKE esté en segundo plano.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        TextButton(onClick = onOpenNotificationSettings) {
                            Text("Abrir ajustes")
                        }
                    }
                }
            }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onLogout
            ) {
                Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Cerrar sesión")
            }
        }
    }
}

@Composable
internal fun SportsPlatformCard(
    platform: SyncPlatform
) {
    val fontScale = LocalDensity.current.fontScale
    val largeText = fontScale >= 1.6f
    val isComingSoon = platform.id.equals("strava", ignoreCase = true)
    val platformAccent = when (platform.id.lowercase()) {
        "strava" -> AppAccentOrange
        "garmin" -> AppAccentBlue
        else -> AppPrimaryBright
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = buildString {
                    append(platform.name)
                    append(". ")
                    append(platform.description)
                    append(" Vinculación en pausa.")
                    if (isComingSoon) append(" Próximamente.")
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = AppSurfaceElevated
        ),
        shape = RoundedCornerShape(AppDimens.RadiusLarge),
            border = BorderStroke(1.dp, platformAccent.copy(alpha = 0.34f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(Modifier.fillMaxWidth()) {
            if (largeText) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                        .padding(top = if (isComingSoon) 30.dp else 12.dp),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.Space3)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SportsPlatformIcon()
                        Text(
                            platform.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = AppTextPrimary
                        )
                    }
                    Text(
                        platform.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTextSecondary
                    )
                    Text(
                        if (isComingSoon) "Vinculación en pausa · próximamente" else "Vinculación en pausa",
                        style = MaterialTheme.typography.labelMedium,
                        color = platformAccent
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                        .padding(end = 64.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SportsPlatformIcon()
                    SportsPlatformText(
                        platform = platform,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (isComingSoon) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 32.dp, y = 15.dp)
                        .rotate(34f),
                    color = platformAccent,
                    shadowElevation = 6.dp
                ) {
                    Text(
                        "PRÓXIMAMENTE",
                        modifier = Modifier
                            .clearAndSetSemantics { }
                            .padding(horizontal = 30.dp, vertical = 6.dp),
                        color = AppBackground,
                        fontSize = (9f / fontScale.coerceAtLeast(1f)).sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (1f / fontScale.coerceAtLeast(1f)).sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun SportsPlatformIcon() {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = AppSurface,
        border = BorderStroke(1.dp, AppBorderSubtle)
    ) {
        Box(
            modifier = Modifier.size(50.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Watch,
                contentDescription = null,
                tint = AppPrimaryBright
            )
        }
    }
}

@Composable
private fun SportsPlatformText(
    platform: SyncPlatform,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            platform.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = AppTextPrimary
        )
        Text(
            platform.description,
            style = MaterialTheme.typography.bodySmall,
            color = AppTextSecondary
        )
        Text(
            "Vinculación en pausa",
            style = MaterialTheme.typography.labelMedium,
            color = AppWarning
        )
    }
}
