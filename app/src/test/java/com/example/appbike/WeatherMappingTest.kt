package com.example.appbike

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class WeatherMappingTest {
    @Test
    fun mapsWmoCodesToEveryVisibleWeatherFamily() {
        assertEquals(WeatherCondition.CLEAR, weatherConditionForWmoCode(0))
        assertEquals(WeatherCondition.PARTLY_CLOUDY, weatherConditionForWmoCode(2))
        assertEquals(WeatherCondition.CLOUDY, weatherConditionForWmoCode(3))
        assertEquals(WeatherCondition.FOG, weatherConditionForWmoCode(48))
        assertEquals(WeatherCondition.DRIZZLE, weatherConditionForWmoCode(53))
        assertEquals(WeatherCondition.FREEZING_RAIN, weatherConditionForWmoCode(67))
        assertEquals(WeatherCondition.RAIN, weatherConditionForWmoCode(82))
        assertEquals(WeatherCondition.SNOW, weatherConditionForWmoCode(86))
        assertEquals(WeatherCondition.THUNDERSTORM, weatherConditionForWmoCode(95))
        assertEquals(WeatherCondition.HAIL, weatherConditionForWmoCode(99))
        assertEquals(WeatherCondition.UNKNOWN, weatherConditionForWmoCode(500))
    }

    @Test
    fun distinguishesClearDayFromClearNight() {
        assertEquals("Despejado", weatherConditionLabel(WeatherCondition.CLEAR, isDay = true))
        assertEquals("Noche clara", weatherConditionLabel(WeatherCondition.CLEAR, isDay = false))
    }

    @Test
    fun buildsCurrentWeatherSnapshotFromNormalizedValues() {
        val weather = RemoteConnections.weatherSnapshotFromValues(
            temperatureCelsius = 8.4,
            apparentTemperatureCelsius = 6.1,
            weatherCode = 95,
            isDay = false,
            cloudCoverPercent = 96,
            precipitationMillimeters = 1.2,
            observedAt = "2026-08-12T21:30",
            latitude = -41.33,
            longitude = -72.97
        )

        assertEquals(8.4, weather.temperatureCelsius, 0.001)
        assertEquals(WeatherCondition.THUNDERSTORM, weather.condition)
        assertFalse(weather.isDay)
        assertEquals(96, weather.cloudCoverPercent)
        assertEquals(1.2, weather.precipitationMillimeters, 0.001)
        assertEquals(-41.33, weather.latitude, 0.001)
        assertEquals(-72.97, weather.longitude, 0.001)
    }
}
