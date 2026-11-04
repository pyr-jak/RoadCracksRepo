package com.example.roadcracks.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherModelsTest {
    private fun reading(latitude: Double = 50.06, longitude: Double = 19.94) =
        WeatherReading(latitude, longitude, "Kraków", 12.0, 0, true, 100_000L)

    @Test
    fun gpsJitterDoesNotCauseAnotherWeatherRequest() {
        assertTrue(WeatherRefreshPolicy.canReuse(reading(), 50.0601, 19.9401, 200_000L))
    }

    @Test
    fun weatherExpiresAfterFifteenMinutes() {
        val data = reading()
        assertTrue(WeatherRefreshPolicy.canReuse(data, data.latitude, data.longitude,
            data.loadedAtElapsedMillis + WeatherRefreshPolicy.REFRESH_INTERVAL_MILLIS - 1))
        assertFalse(WeatherRefreshPolicy.canReuse(data, data.latitude, data.longitude,
            data.loadedAtElapsedMillis + WeatherRefreshPolicy.REFRESH_INTERVAL_MILLIS))
    }

    @Test
    fun movingToAnotherAreaInvalidatesTheCachedLocality() {
        assertFalse(WeatherRefreshPolicy.canReuse(reading(), 50.075, 19.94, 200_000L))
    }

    @Test
    fun nearbyLocationsAcrossTheDateLineAreRecognized() {
        assertTrue(WeatherRefreshPolicy.canReuse(reading(0.0, 179.999), 0.0, -179.999, 200_000L))
    }

    @Test
    fun aRestartedMonotonicClockInvalidatesOldData() {
        assertFalse(WeatherRefreshPolicy.canReuse(reading(), 50.06, 19.94, 1L))
    }

    @Test
    fun failuresAndRepeatedTapsAreRateLimited() {
        assertTrue(WeatherRefreshPolicy.canAttempt(null, 1L, false))
        assertFalse(WeatherRefreshPolicy.canAttempt(0L, 59_999L, false))
        assertTrue(WeatherRefreshPolicy.canAttempt(0L, 60_000L, false))
        assertFalse(WeatherRefreshPolicy.canAttempt(0L, 9_999L, true))
        assertTrue(WeatherRefreshPolicy.canAttempt(0L, 10_000L, true))
    }

    @Test
    fun hazardousConditionsUseTheCorrectWeatherFamily() {
        assertEquals(WeatherKind.RAIN, weatherKind(67))
        assertEquals(WeatherKind.SNOW, weatherKind(86))
        assertEquals(WeatherKind.STORM, weatherKind(97))
        assertEquals(WeatherKind.STORM, weatherKind(99))
        assertEquals(WeatherKind.UNKNOWN, weatherKind(-1))
    }

    @Test
    fun temperaturesAroundZeroDoNotShowNegativeZero() {
        assertEquals("0°", temperatureLabel(-0.4))
        assertEquals("-2°", temperatureLabel(-2.2))
    }
}
