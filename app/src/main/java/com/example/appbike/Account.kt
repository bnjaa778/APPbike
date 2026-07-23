package com.example.appbike

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object AccountStore {
    private const val PREFS = "appbike_account"
    private const val USER_ID = "user_id"
    private const val EMAIL = "email"
    private const val USERNAME = "username"

    fun loadSession(context: Context): AccountSession? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val userId = prefs.all[USER_ID] as? String ?: ""
        val email = prefs.getString(EMAIL, "").orEmpty()
        return if (isValidAccountUserId(userId) && email.isNotBlank()) {
            AccountSession(
                userId = userId,
                email = email,
                accessToken = SecureTokenStore.load(context),
                username = prefs.getString(USERNAME, null)
            )
        } else {
            if (prefs.contains(USER_ID)) clearSession(context)
            null
        }
    }

    fun saveSession(context: Context, session: AccountSession) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(USER_ID, session.userId)
            .putString(EMAIL, session.email)
            .putString(USERNAME, session.username)
            .apply()
        SecureTokenStore.save(context, session.accessToken)
    }

    fun clearSession(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(USER_ID)
            .remove(EMAIL)
            .remove(USERNAME)
            .apply()
        SecureTokenStore.clear(context)
    }

}

internal fun isValidAccountUserId(value: String): Boolean =
    Regex(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-" +
            "[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$"
    ).matches(value)

@Composable
fun AccountScreen(
    session: AccountSession?,
    platforms: MutableList<SyncPlatform>,
    oauthCallback: SportsOAuthCallback? = null,
    onOauthCallbackConsumed: () -> Unit = {},
    onLogin: (AccountSession) -> Unit,
    onSessionUpdated: (AccountSession) -> Unit,
    onLogout: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var sportsLoading by remember { mutableStateOf(false) }
    var sportsError by remember { mutableStateOf<String?>(null) }
    var profileCheckedUserId by remember { mutableStateOf<String?>(null) }
    var usernameCandidate by remember(session?.userId) { mutableStateOf("") }
    var usernameLoading by remember { mutableStateOf(false) }
    var usernameError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val redirectUri = "appbike://oauth/callback"

    fun replacePlatform(updated: SyncPlatform) {
        val index = platforms.indexOfFirst { it.id.equals(updated.id, ignoreCase = true) }
        if (index >= 0) platforms[index] = updated else platforms.add(updated)
    }

    LaunchedEffect(session?.userId) {
        val activeSession = session ?: run {
            profileCheckedUserId = null
            return@LaunchedEffect
        }
        if (activeSession.username.isNullOrBlank()) {
            runCatching {
                withContext(Dispatchers.IO) {
                    RemoteConnections.loadUserProfile(activeSession)
                }
            }.onSuccess(onSessionUpdated)
        }
        profileCheckedUserId = activeSession.userId
    }

    LaunchedEffect(session?.userId) {
        val activeSession = session ?: return@LaunchedEffect
        sportsLoading = true
        runCatching {
            withContext(Dispatchers.IO) {
                RemoteConnections.loadSportsConnections(activeSession.userId)
            }
        }.onSuccess { remote ->
            remote.forEach(::replacePlatform)
            sportsError = null
        }.onFailure {
            sportsError = "La sincronización real quedará disponible cuando el backend habilite OAuth."
        }
        sportsLoading = false
    }

    LaunchedEffect(session?.userId, oauthCallback) {
        val activeSession = session ?: return@LaunchedEffect
        val callback = oauthCallback ?: return@LaunchedEffect
        onOauthCallbackConsumed()
        if (callback.error.isNotBlank()) {
            sportsError = "La autorización fue cancelada: ${callback.error}"
            return@LaunchedEffect
        }
        sportsLoading = true
        runCatching {
            withContext(Dispatchers.IO) {
                RemoteConnections.completeSportsConnection(
                    activeSession.userId,
                    callback,
                    redirectUri
                )
            }
        }.onSuccess {
            replacePlatform(it)
            sportsError = null
        }.onFailure { sportsError = RemoteConnections.userFriendlyError(it) }
        sportsLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Cuenta y deporte",
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                text = "Tu perfil y conexiones deportivas en un solo lugar.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        runCatching {
                            withContext(Dispatchers.IO) {
                                RemoteConnections.login(email, password)
                            }
                        }.onSuccess(onLogin)
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
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    )
                },
                onLogout = onLogout
            )
            ProfileContentSection(session)
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Sync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Sincronización deportiva",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Text(
                text = "Conecta tus plataformas para reunir rutas y entrenamientos.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        platforms.forEachIndexed { index, platform ->
            SportsPlatformCard(
                platform = platform,
                enabled = session != null && !sportsLoading,
                onToggle = {
                    val activeSession = session ?: return@SportsPlatformCard
                    scope.launch {
                        sportsLoading = true
                        sportsError = null
                        runCatching {
                            withContext(Dispatchers.IO) {
                                if (platform.connected) {
                                    RemoteConnections.disconnectSportsConnection(
                                        activeSession.userId,
                                        platform.id
                                    )
                                } else {
                                    RemoteConnections.beginSportsConnection(
                                        activeSession.userId,
                                        platform.id,
                                        redirectUri
                                    )
                                }
                            }
                        }.onSuccess { result ->
                            when (result) {
                                is SyncPlatform -> platforms[index] = result
                                is SportsOAuthStart -> context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(result.authorizationUrl))
                                )
                            }
                        }.onFailure {
                            sportsError = RemoteConnections.userFriendlyError(it)
                        }
                        sportsLoading = false
                    }
                }
            )
        }

        if (sportsLoading) CircularProgressIndicator()
        sportsError?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }

        Spacer(Modifier.height(12.dp))
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
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Nombre de usuario") },
                        supportingText = {
                            Text("3 a 30 caracteres: letras, números, punto, _ o -")
                        },
                        isError = usernameError != null
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
                            runCatching {
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
                TextButton(enabled = false, onClick = {}) { Text("Requerido") }
            }
        )
    }
}

private fun isValidUsername(value: String): Boolean =
    Regex("^[A-Za-z0-9][A-Za-z0-9._-]{2,29}$").matches(value.trim())

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
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(
                    modifier = Modifier.size(52.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PersonOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Text("Bienvenido", style = MaterialTheme.typography.headlineMedium)
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

            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

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
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(
                        modifier = Modifier.size(54.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PersonOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Sesión activa",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        session.username ?: session.email,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!session.username.isNullOrBlank()) {
                        Text(
                            session.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = "Cuenta conectada",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                "Tus bicicletas y mantenciones se cargan de forma privada desde esta cuenta.",
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (!notificationsEnabled) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "Notificaciones desactivadas",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            "Actívalas para recibir mensajes cuando APPBIKE esté en segundo plano.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
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
                Icon(Icons.Outlined.Logout, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Cerrar sesión")
            }
        }
    }
}

@Composable
private fun SportsPlatformCard(
    platform: SyncPlatform,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (platform.connected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                }
            ) {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (platform.connected) {
                            Icons.Outlined.CheckCircle
                        } else {
                            Icons.Outlined.Link
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(platform.name, fontWeight = FontWeight.Bold)
                Text(
                    platform.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    if (platform.connected) "Conectado" else "Sin conectar",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (platform.connected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Button(
                enabled = enabled,
                onClick = onToggle,
                colors = if (platform.connected) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Text(if (platform.connected) "Quitar" else "Conectar")
            }
        }
    }
}
