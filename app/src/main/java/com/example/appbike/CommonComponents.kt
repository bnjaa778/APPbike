package com.example.appbike

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.appbike.ui.theme.AppBackground
import com.example.appbike.ui.theme.AppBackgroundElevated
import com.example.appbike.ui.theme.AppBorderActive
import com.example.appbike.ui.theme.AppBorderSubtle
import com.example.appbike.ui.theme.AppError
import com.example.appbike.ui.theme.AppErrorSoft
import com.example.appbike.ui.theme.AppPrimary
import com.example.appbike.ui.theme.AppPrimaryBright
import com.example.appbike.ui.theme.AppPrimarySoft
import com.example.appbike.ui.theme.AppSurface
import com.example.appbike.ui.theme.AppSurfaceElevated
import com.example.appbike.ui.theme.AppTextMuted
import com.example.appbike.ui.theme.AppTextPrimary
import com.example.appbike.ui.theme.AppTextSecondary

enum class PremiumGlowStyle {
    Account,
    Bikes,
    Marketplace,
    Chat,
    Map
}

@Composable
fun PremiumScreenBackground(
    style: PremiumGlowStyle,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val base = AppBackground
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(base)
            .drawBehind {
                val max = size.maxDimension
                val centerY = when (style) {
                    PremiumGlowStyle.Account -> 0.18f
                    PremiumGlowStyle.Bikes -> 0.26f
                    PremiumGlowStyle.Marketplace -> 0.16f
                    PremiumGlowStyle.Chat -> 0.22f
                    PremiumGlowStyle.Map -> 0.18f
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        0.0f to AppPrimaryBright.copy(alpha = 0.18f),
                        0.34f to AppPrimary.copy(alpha = 0.08f),
                        1.0f to Color.Transparent
                    ),
                    radius = max * 0.28f,
                    center = Offset(size.width * 0.04f, size.height * centerY)
                )
            }
    ) {
        content()
    }
}

object AppDimens {
    val RadiusSmall = 10.dp
    val RadiusMedium = 16.dp
    val RadiusLarge = 22.dp
    val RadiusXLarge = 28.dp
    val RadiusPill = 999.dp
    val Space1 = 4.dp
    val Space2 = 8.dp
    val Space3 = 12.dp
    val Space4 = 16.dp
    val Space5 = 20.dp
    val Space6 = 24.dp
    val Space8 = 32.dp
    val Space10 = 40.dp
}

fun Modifier.appSubtleGlow(enabled: Boolean = true): Modifier = drawBehind {
    if (!enabled) return@drawBehind
    drawCircle(
        brush = Brush.radialGradient(
            0.0f to AppPrimary.copy(alpha = 0.18f),
            0.42f to AppPrimary.copy(alpha = 0.07f),
            1.0f to Color.Transparent
        ),
        radius = size.maxDimension * 0.64f,
        center = Offset(size.width * 0.10f, size.height * 0.05f)
    )
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.appSubtleGlow(highlighted),
        shape = RoundedCornerShape(AppDimens.RadiusLarge),
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) AppSurfaceElevated else AppSurface
        ),
        border = BorderStroke(1.dp, if (highlighted) AppBorderActive.copy(alpha = 0.32f) else AppBorderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = if (highlighted) 6.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.Space5),
            content = content
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = AppTextPrimary
        )
        action?.invoke()
    }
}

@Composable
fun ErrorBanner(
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.RadiusMedium),
        color = AppErrorSoft,
        border = BorderStroke(1.dp, AppError.copy(alpha = 0.20f))
    ) {
        Row(
            modifier = Modifier.padding(AppDimens.Space4),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(AppDimens.Space3),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = AppError,
                modifier = Modifier.size(20.dp)
            )
            Text(
                message,
                modifier = Modifier.weight(1f),
                color = AppError,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    AppCard(
        modifier = modifier.fillMaxWidth(),
        highlighted = true
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(AppDimens.Space3)
        ) {
            Icon(
                Icons.Outlined.Inventory2,
                contentDescription = null,
                tint = AppPrimaryBright,
                modifier = Modifier.size(38.dp)
            )
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AppTextPrimary
            )
            Text(
                description,
                color = AppTextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            if (actionLabel != null && onAction != null) {
                TextButton(onClick = onAction) {
                    Text(actionLabel, color = AppPrimaryBright)
                }
            }
        }
    }
}

@Composable
fun LoadingState(
    label: String,
    modifier: Modifier = Modifier
) {
    AppCard(modifier = modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(AppDimens.Space4),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = AppPrimaryBright
            )
            Text(label, color = AppTextSecondary)
        }
    }
}

@Composable
fun ScreenContainer(
    title: String,
    onBack: (() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(8.dp))
        if (onBack != null) {
            TextButton(onClick = onBack) {
                Text("← Volver")
            }
        }

        Text(title, style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(14.dp))
        content()
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun CardItem(
    title: String,
    subtitle: String,
    body: String
) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = AppTextPrimary
        )
        if (subtitle.isNotBlank()) {
            Spacer(Modifier.height(AppDimens.Space1))
            Text(
                subtitle,
                color = AppPrimaryBright,
                style = MaterialTheme.typography.labelLarge
            )
        }
        if (body.isNotBlank()) {
            Spacer(Modifier.height(AppDimens.Space2))
            Text(body, color = AppTextSecondary)
        }
    }
}

@Composable
fun AppInput(
    label: String,
    value: String,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        visualTransformation = visualTransformation,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.RadiusMedium),
        colors = appBikeTextFieldColors()
    )
}

fun Modifier.appBikeTextFieldGlow(): Modifier = this

fun Modifier.focusGlow(focused: Boolean): Modifier = drawBehind {
    if (!focused) return@drawBehind
    val outerPadding = 6.dp.toPx()
    val corner = 18.dp.toPx()
    drawRoundRect(
        brush = Brush.radialGradient(
            0.0f to AppPrimary.copy(alpha = 0.16f),
            1.0f to Color.Transparent,
            center = Offset(size.width * 0.18f, size.height * 0.20f),
            radius = size.maxDimension * 0.76f
        ),
        topLeft = Offset(-outerPadding, -outerPadding),
        size = Size(size.width + outerPadding * 2, size.height + outerPadding * 2),
        cornerRadius = CornerRadius(corner + outerPadding)
    )
}

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
    keyboardActions: androidx.compose.foundation.text.KeyboardActions = androidx.compose.foundation.text.KeyboardActions.Default
) {
    var focused by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .focusGlow(focused)
            .onFocusChanged { focused = it.isFocused },
        placeholder = {
            Text(
                placeholder,
                color = AppTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        enabled = enabled,
        singleLine = singleLine,
        shape = RoundedCornerShape(AppDimens.RadiusLarge),
        colors = appBikeTextFieldColors(),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions
    )
}

@Composable
fun appBikeTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = AppTextPrimary,
    unfocusedTextColor = AppTextPrimary,
    disabledTextColor = AppTextMuted,
    focusedContainerColor = AppSurfaceElevated,
    unfocusedContainerColor = AppSurfaceElevated,
    disabledContainerColor = AppSurface,
    errorContainerColor = AppSurfaceElevated,
    focusedBorderColor = AppBorderActive,
    unfocusedBorderColor = Color.Transparent,
    disabledBorderColor = Color.Transparent,
    errorBorderColor = AppError,
    cursorColor = AppPrimaryBright,
    focusedLabelColor = AppPrimaryBright,
    unfocusedLabelColor = AppTextSecondary,
    focusedPlaceholderColor = AppTextMuted,
    unfocusedPlaceholderColor = AppTextMuted,
    focusedSupportingTextColor = AppTextSecondary,
    unfocusedSupportingTextColor = AppTextSecondary,
    focusedLeadingIconColor = AppTextSecondary,
    unfocusedLeadingIconColor = AppTextSecondary,
    focusedTrailingIconColor = AppTextSecondary,
    unfocusedTrailingIconColor = AppTextSecondary
)
