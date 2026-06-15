package com.example.appbike

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.appbike.ui.theme.APPbikeTheme

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
                    platforms[index] = RemoteConnections.setSportsPlatformConnection(
                        platform = platform,
                        connected = !platform.connected
                    )
                }
            ) {
                Text(if (platform.connected) "Desconectar" else "Conectar")
            }
        }
    }
}
