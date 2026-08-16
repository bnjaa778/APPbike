package com.example.appbike

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
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

internal sealed interface WeatherHeaderState {
    data object WaitingForLocation : WeatherHeaderState
    data object Loading : WeatherHeaderState
    data class Ready(val snapshot: WeatherSnapshot) : WeatherHeaderState
    data class Unavailable(val reason: String) : WeatherHeaderState
}

@Composable
internal fun WeatherStatusChip(
    state: WeatherHeaderState,
    compact: Boolean
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

    Surface(
        modifier = Modifier
            .width(if (compact) 72.dp else 92.dp)
            .height(if (compact) 40.dp else 48.dp)
            .clearAndSetSemantics {
                contentDescription = spokenDescription
            },
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
