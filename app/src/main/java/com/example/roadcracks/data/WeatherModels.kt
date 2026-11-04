package com.example.roadcracks.data

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

internal data class WeatherReading(
    val latitude: Double,
    val longitude: Double,
    val placeName: String,
    val temperatureCelsius: Double,
    val weatherCode: Int,
    val isDay: Boolean,
    val loadedAtElapsedMillis: Long
)

internal enum class WeatherKind { CLEAR, PARTLY_CLOUDY, CLOUDY, FOG, DRIZZLE, RAIN, SNOW, STORM, UNKNOWN }

internal fun weatherKind(code: Int): WeatherKind = when (code) {
    0, 1 -> WeatherKind.CLEAR
    2 -> WeatherKind.PARTLY_CLOUDY
    3 -> WeatherKind.CLOUDY
    45, 48 -> WeatherKind.FOG
    51, 53, 55, 56, 57 -> WeatherKind.DRIZZLE
    61, 63, 65, 66, 67, 80, 81, 82 -> WeatherKind.RAIN
    71, 73, 75, 77, 85, 86 -> WeatherKind.SNOW
    95, 96, 97, 99 -> WeatherKind.STORM
    else -> WeatherKind.UNKNOWN
}

internal fun weatherDescription(code: Int): String = when (code) {
    0 -> "Bezchmurnie"
    1 -> "Pogodnie"
    2 -> "Częściowe zachmurzenie"
    3 -> "Pochmurno"
    45, 48 -> "Mgła"
    51, 53, 55 -> "Mżawka"
    56, 57 -> "Marznąca mżawka"
    61 -> "Lekki deszcz"
    63 -> "Deszcz"
    65 -> "Silny deszcz"
    66, 67 -> "Marznący deszcz"
    71, 73, 75 -> "Śnieg"
    77 -> "Śnieg ziarnisty"
    80, 81, 82 -> "Przelotny deszcz"
    85, 86 -> "Przelotny śnieg"
    95, 97 -> "Burza"
    96, 99 -> "Burza z gradem"
    else -> "Warunki pogodowe"
}

internal fun temperatureLabel(temperatureCelsius: Double): String =
    "${temperatureCelsius.roundToInt()}°"

internal object WeatherRefreshPolicy {
    const val REFRESH_INTERVAL_MILLIS = 15 * 60_000L
    const val AUTOMATIC_RETRY_MILLIS = 60_000L
    const val MANUAL_RETRY_MILLIS = 10_000L
    const val LOCATION_DISTANCE_METERS = 1_000.0

    fun canReuse(reading: WeatherReading, latitude: Double, longitude: Double, now: Long): Boolean =
        now - reading.loadedAtElapsedMillis in 0L until REFRESH_INTERVAL_MILLIS &&
            isSameArea(reading, latitude, longitude)

    fun isSameArea(reading: WeatherReading, latitude: Double, longitude: Double): Boolean =
        distanceMeters(reading.latitude, reading.longitude, latitude, longitude) < LOCATION_DISTANCE_METERS

    fun canAttempt(lastAttempt: Long?, now: Long, manual: Boolean): Boolean =
        lastAttempt == null || now - lastAttempt >=
            if (manual) MANUAL_RETRY_MILLIS else AUTOMATIC_RETRY_MILLIS

    private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val deltaLat = Math.toRadians(lat2 - lat1)
        val deltaLon = Math.toRadians(lon2 - lon1)
        val a = (sin(deltaLat / 2) * sin(deltaLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(deltaLon / 2) * sin(deltaLon / 2)).coerceIn(0.0, 1.0)
        return 6_371_000.0 * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}
