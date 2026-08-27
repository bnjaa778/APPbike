package com.example.appbike

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appbike.ui.theme.AppAccentAmber
import com.example.appbike.ui.theme.AppAccentBlue
import com.example.appbike.ui.theme.AppBorderSubtle
import com.example.appbike.ui.theme.AppPrimaryBright
import com.example.appbike.ui.theme.AppSurfaceElevated
import com.example.appbike.ui.theme.AppTextPrimary
import com.example.appbike.ui.theme.AppTextSecondary
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import java.text.SimpleDateFormat
import java.util.Locale

internal sealed interface WeatherHeaderState {
    data object WaitingForLocation : WeatherHeaderState
    data object Loading : WeatherHeaderState
    data class Ready(val snapshot: WeatherSnapshot) : WeatherHeaderState
    data class Unavailable(val reason: String) : WeatherHeaderState
}

internal sealed interface WeatherForecastState {
    data object Idle : WeatherForecastState
    data object Loading : WeatherForecastState
    data class Ready(val forecast: WeatherForecast) : WeatherForecastState
    data class Unavailable(val reason: String) : WeatherForecastState
}

@Composable
internal fun WeatherStatusChip(
    state: WeatherHeaderState,
    compact: Boolean,
    onClick: (() -> Unit)? = null
) {
    val ready = state as? WeatherHeaderState.Ready
    val temperature = ready?.snapshot?.temperatureCelsius?.roundToInt()
    val label = ready?.snapshot?.let {
        weatherConditionLabel(it.condition, it.isDay)
    }
    val spokenDescription = when (state) {
        WeatherHeaderState.Loading -> "Actualizando el tiempo mediante la ubicación del teléfono."
        WeatherHeaderState.WaitingForLocation ->
            "Tiempo pendiente. Concede ubicación para actualizarlo mediante GPS."
        is WeatherHeaderState.Unavailable ->
            "Tiempo no disponible. ${state.reason}"
        is WeatherHeaderState.Ready ->
            "$label, $temperature grados Celsius. Datos de Open-Meteo."
    }

    val chipModifier = Modifier
        .width(if (compact) 72.dp else 92.dp)
        .height(if (compact) 40.dp else 48.dp)
        .then(
            onClick?.let { action ->
                Modifier
                    .clickable(onClick = action)
                    .semantics { contentDescription = spokenDescription }
            } ?: Modifier.clearAndSetSemantics {
                contentDescription = spokenDescription
            }
        )

    Surface(
        modifier = chipModifier,
        shape = MaterialTheme.shapes.medium,
        color = AppSurfaceElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, AppBorderSubtle)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (compact) 7.dp else 8.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (state) {
                WeatherHeaderState.Loading -> CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = AppPrimaryBright,
                    strokeWidth = 2.dp
                )
                else -> WeatherGlyph(
                    condition = ready?.snapshot?.condition ?: WeatherCondition.UNKNOWN,
                    isDay = ready?.snapshot?.isDay ?: true,
                    modifier = Modifier.size(if (compact) 20.dp else 24.dp)
                )
            }
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = when (state) {
                        WeatherHeaderState.Loading -> "Clima"
                        WeatherHeaderState.WaitingForLocation -> "GPS"
                        is WeatherHeaderState.Unavailable -> "Sin datos"
                        is WeatherHeaderState.Ready -> "$temperature°"
                    },
                    color = AppTextPrimary,
                    fontSize = if (compact) 13.sp else 15.sp,
                    lineHeight = if (compact) 14.sp else 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                if (!compact && label != null) {
                    Text(
                        text = label,
                        color = AppTextSecondary,
                        fontSize = 7.sp,
                        lineHeight = 8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "Open-Meteo",
                    color = AppPrimaryBright,
                    fontSize = 6.sp,
                    lineHeight = 7.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
internal fun WeatherForecastPanel(
    state: WeatherForecastState,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .heightIn(max = 440.dp)
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Pronóstico de 6 días",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = AppTextPrimary
                )
                Text(
                    "Tiempo en tu ubicación",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppTextSecondary
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Outlined.Close, contentDescription = "Cerrar pronóstico")
            }
        }

        when (state) {
            WeatherForecastState.Idle -> Text(
                "Abriendo el pronóstico…",
                color = AppTextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
            WeatherForecastState.Loading -> Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = AppPrimaryBright,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
                Text("Actualizando…", color = AppTextSecondary)
            }
            is WeatherForecastState.Unavailable -> Text(
                state.reason,
                color = AppTextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
            is WeatherForecastState.Ready -> state.forecast.days.forEach { day ->
                WeatherForecastDayRow(day)
            }
        }

        Text(
            "Open-Meteo",
            color = AppPrimaryBright,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun WeatherForecastDayRow(day: WeatherForecastDay) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AppSurfaceElevated.copy(alpha = 0.72f),
        shape = MaterialTheme.shapes.small,
        border = androidx.compose.foundation.BorderStroke(1.dp, AppBorderSubtle.copy(alpha = 0.72f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    weatherForecastDayLabel(day.date),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppTextPrimary
                )
                Text(
                    buildString {
                        append(weatherConditionLabel(day.condition, true))
                        if (day.precipitationProbabilityPercent > 0) {
                            append(" · ${day.precipitationProbabilityPercent}% lluvia")
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = AppTextSecondary,
                    maxLines = 1
                )
            }
            WeatherGlyph(
                condition = day.condition,
                isDay = true,
                modifier = Modifier.size(25.dp)
            )
            Text(
                "${day.temperatureMaxCelsius.roundToInt()}° / ${day.temperatureMinCelsius.roundToInt()}°",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = AppTextPrimary
            )
        }
    }
}

private fun weatherForecastDayLabel(date: String): String {
    val locale = Locale.forLanguageTag("es-CL")
    val parsed = runCatching {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date)
    }.getOrNull()
    return parsed?.let {
        SimpleDateFormat("EEE d", locale).format(it).replaceFirstChar { first ->
            first.titlecase(locale)
        }
    } ?: date
}

@Composable
private fun WeatherGlyph(
    condition: WeatherCondition,
    isDay: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.clearAndSetSemantics { }) {
        val strokeWidth = 1.7.dp.toPx()
        when (condition) {
            WeatherCondition.CLEAR -> drawSunOrMoon(isDay, strokeWidth)
            WeatherCondition.PARTLY_CLOUDY -> {
                drawSunOrMoon(isDay, strokeWidth, scale = 0.68f, shift = Offset(-3.dp.toPx(), -3.dp.toPx()))
                drawWeatherCloud(strokeWidth)
            }
            WeatherCondition.CLOUDY,
            WeatherCondition.UNKNOWN -> drawWeatherCloud(strokeWidth)
            WeatherCondition.FOG -> {
                drawWeatherCloud(strokeWidth, verticalShift = -2.dp.toPx())
                repeat(2) { index ->
                    val y = size.height * (0.76f + index * 0.12f)
                    drawLine(
                        color = AppTextSecondary,
                        start = Offset(size.width * 0.22f, y),
                        end = Offset(size.width * 0.82f, y),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }
            WeatherCondition.DRIZZLE,
            WeatherCondition.RAIN,
            WeatherCondition.FREEZING_RAIN -> {
                drawWeatherCloud(strokeWidth, verticalShift = -2.dp.toPx())
                val drops = if (condition == WeatherCondition.DRIZZLE) 2 else 3
                repeat(drops) { index ->
                    val x = size.width * (0.33f + index * 0.18f)
                    val top = size.height * 0.73f
                    val length = if (condition == WeatherCondition.DRIZZLE) 0.08f else 0.16f
                    drawLine(
                        color = AppAccentBlue,
                        start = Offset(x, top),
                        end = Offset(x - 1.dp.toPx(), top + size.height * length),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
                if (condition == WeatherCondition.FREEZING_RAIN) {
                    drawCircle(AppTextPrimary, 1.2.dp.toPx(), Offset(size.width * 0.78f, size.height * 0.84f))
                }
            }
            WeatherCondition.SNOW -> {
                drawWeatherCloud(strokeWidth, verticalShift = -2.dp.toPx())
                repeat(3) { index ->
                    val center = Offset(size.width * (0.32f + index * 0.2f), size.height * 0.84f)
                    drawLine(AppAccentBlue, center - Offset(2.dp.toPx(), 0f), center + Offset(2.dp.toPx(), 0f), strokeWidth)
                    drawLine(AppAccentBlue, center - Offset(0f, 2.dp.toPx()), center + Offset(0f, 2.dp.toPx()), strokeWidth)
                }
            }
            WeatherCondition.THUNDERSTORM,
            WeatherCondition.HAIL -> {
                drawWeatherCloud(strokeWidth, verticalShift = -3.dp.toPx())
                val bolt = Path().apply {
                    moveTo(size.width * 0.53f, size.height * 0.68f)
                    lineTo(size.width * 0.42f, size.height * 0.86f)
                    lineTo(size.width * 0.53f, size.height * 0.84f)
                    lineTo(size.width * 0.47f, size.height)
                    lineTo(size.width * 0.68f, size.height * 0.77f)
                    lineTo(size.width * 0.57f, size.height * 0.79f)
                    close()
                }
                drawPath(bolt, AppAccentAmber)
                if (condition == WeatherCondition.HAIL) {
                    drawCircle(AppTextPrimary, 1.4.dp.toPx(), Offset(size.width * 0.28f, size.height * 0.86f))
                    drawCircle(AppTextPrimary, 1.4.dp.toPx(), Offset(size.width * 0.78f, size.height * 0.86f))
                }
            }
        }
    }
}

private fun DrawScope.drawSunOrMoon(
    isDay: Boolean,
    strokeWidth: Float,
    scale: Float = 1f,
    shift: Offset = Offset.Zero
) {
    val center = Offset(size.width * 0.5f, size.height * 0.5f) + shift
    val radius = size.minDimension * 0.2f * scale
    if (isDay) {
        drawCircle(AppAccentAmber, radius, center, style = Stroke(strokeWidth))
        repeat(8) { index ->
            val angle = Math.toRadians(index * 45.0)
            val inner = radius * 1.45f
            val outer = radius * 1.9f
            drawLine(
                color = AppAccentAmber,
                start = center + Offset(cos(angle).toFloat() * inner, sin(angle).toFloat() * inner),
                end = center + Offset(cos(angle).toFloat() * outer, sin(angle).toFloat() * outer),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    } else {
        drawCircle(AppAccentBlue, radius * 1.15f, center)
        drawCircle(AppSurfaceElevated, radius, center + Offset(radius * 0.55f, -radius * 0.3f))
    }
}

private fun DrawScope.drawWeatherCloud(
    strokeWidth: Float,
    verticalShift: Float = 0f
) {
    val path = Path().apply {
        moveTo(size.width * 0.2f, size.height * 0.64f + verticalShift)
        cubicTo(
            size.width * 0.07f,
            size.height * 0.62f + verticalShift,
            size.width * 0.08f,
            size.height * 0.43f + verticalShift,
            size.width * 0.25f,
            size.height * 0.42f + verticalShift
        )
        cubicTo(
            size.width * 0.29f,
            size.height * 0.22f + verticalShift,
            size.width * 0.57f,
            size.height * 0.2f + verticalShift,
            size.width * 0.65f,
            size.height * 0.4f + verticalShift
        )
        cubicTo(
            size.width * 0.86f,
            size.height * 0.36f + verticalShift,
            size.width * 0.94f,
            size.height * 0.62f + verticalShift,
            size.width * 0.78f,
            size.height * 0.67f + verticalShift
        )
        lineTo(size.width * 0.22f, size.height * 0.67f + verticalShift)
    }
    drawPath(path, AppTextPrimary, style = Stroke(strokeWidth, cap = StrokeCap.Round))
}
