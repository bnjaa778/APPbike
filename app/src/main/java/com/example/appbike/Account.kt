package com.example.appbike

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.Lock
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
                    Image(
                        painter = painterResource(R.drawable.appbike_brand_icon),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
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

            Text(
                "Tu sesión solo se conserva en este dispositivo cuando el acceso es válido.",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.68f)
            )
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
    initialErrorMessage: String? = null,
    oauthCallback: SportsOAuthCallback? = null,
    onOauthCallbackConsumed: () -> Unit = {},
    onLogin: (AccountSession) -> Unit,
    onSessionUpdated: (AccountSession) -> Unit,
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
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(initialErrorMessage, session?.userId) {
        if (session == null && !initialErrorMessage.isNullOrBlank()) {
            errorMessage = initialErrorMessage
        }
    }

    LaunchedEffect(session?.userId) {
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
            AccountReferenceHeader(session)

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
            ProfileCard(
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

        AccountHeroPanel()

        HorizontalDivider(color = AppBorderSubtle)

        AppCard(modifier = Modifier.fillMaxWidth(), highlighted = true) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Space3),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = CircleShape, color = AppPrimarySoft) {
                    Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Sync,
                            contentDescription = null,
                            tint = AppPrimaryBright
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sincronización deportiva",
                        style = MaterialTheme.typography.titleLarge,
                        color = AppTextPrimary
                    )
                    Text(
                        text = "Tus relojes y plataformas se unirán a APPBIKE en una futura actualización.",
                        color = AppTextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(Modifier.height(AppDimens.Space3))
            Surface(shape = RoundedCornerShape(AppDimens.RadiusPill), color = AppPrimarySoft) {
                Text(
                    "EN DESARROLLO",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = AppPrimaryBright,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        platforms.forEach { platform -> SportsPlatformCard(platform) }

        Text(
            "La vinculación está detenida intencionalmente. No se solicitará acceso a Strava, Garmin ni Wahoo hasta que esta función sea habilitada.",
            color = AppTextSecondary,
            style = MaterialTheme.typography.bodySmall
        )

        if (session != null) {
            HorizontalDivider(color = AppBorderSubtle)
            ProfileContentSection(session)
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = buildString {
                    append(platform.name)
                    append(". ")
                    append(platform.description)
                    append(" Vinculación en pausa. Próximamente.")
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = AppSurfaceElevated
        ),
        shape = RoundedCornerShape(AppDimens.RadiusLarge),
        border = BorderStroke(1.dp, AppPrimary.copy(alpha = 0.24f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(Modifier.fillMaxWidth()) {
            if (largeText) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                        .padding(top = 30.dp),
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
                        "Vinculación en pausa",
                        style = MaterialTheme.typography.labelMedium,
                        color = AppWarning
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

            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 32.dp, y = 15.dp)
                    .rotate(34f),
                color = AppPrimary,
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
