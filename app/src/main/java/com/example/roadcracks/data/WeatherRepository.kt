package com.example.roadcracks.data

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.SystemClock
import java.io.IOException
import java.net.URL
import java.util.Locale
import java.util.concurrent.ScheduledThreadPoolExecutor
import javax.net.ssl.HttpsURLConnection
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

/** Current conditions from Open-Meteo; its free hosted API is for non-commercial use. */
internal object WeatherRepository {
    private val mutex = Mutex()
    private var cached: WeatherReading? = null
    private var lastAttempt: Long? = null
    // Older Android versions expose a blocking geocoder. Keep it off the UI thread
    // and let the weather request finish even if the device's geocoder gets stuck.
    private val legacyGeocoderExecutor = ScheduledThreadPoolExecutor(1) { task ->
        Thread(task, "RoadCracksGeocoder").apply { isDaemon = true }
    }.apply { removeOnCancelPolicy = true }

    suspend fun load(
        context: Context,
        latitude: Double,
        longitude: Double,
        force: Boolean = false
    ): WeatherReading = mutex.withLock {
        val now = SystemClock.elapsedRealtime()
        val previous = cached
        if (!force && previous != null && WeatherRefreshPolicy.canReuse(previous, latitude, longitude, now)) {
            return@withLock previous
        }
        if (!WeatherRefreshPolicy.canAttempt(lastAttempt, now, force)) {
            if (previous != null && WeatherRefreshPolicy.isSameArea(previous, latitude, longitude)) {
                return@withLock previous
            }
            throw IOException("Please retry after the weather request cooldown")
        }
        lastAttempt = now

        coroutineScope {
            val place = async { findPlaceName(context.applicationContext, latitude, longitude) }
            val current = async(Dispatchers.IO) { requestCurrentWeather(latitude, longitude) }
            val conditions = current.await()
            WeatherReading(
                latitude = latitude,
                longitude = longitude,
                placeName = place.await(),
                temperatureCelsius = conditions.temperature,
                weatherCode = conditions.code,
                isDay = conditions.isDay,
                loadedAtElapsedMillis = SystemClock.elapsedRealtime()
            ).also { cached = it }
        }
    }

    private data class CurrentConditions(val temperature: Double, val code: Int, val isDay: Boolean)

    private fun requestCurrentWeather(latitude: Double, longitude: Double): CurrentConditions {
        val coordinates = String.format(Locale.US, "latitude=%.3f&longitude=%.3f", latitude, longitude)
        val connection = URL("https://api.open-meteo.com/v1/forecast?$coordinates" +
            "&current=temperature_2m,weather_code,is_day&temperature_unit=celsius&timezone=auto")
            .openConnection() as HttpsURLConnection
        connection.connectTimeout = 6_000
        connection.readTimeout = 6_000
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "RoadCracks-Android/1.0")
        try {
            if (connection.responseCode !in 200..299) throw IOException("Weather HTTP ${connection.responseCode}")
            val json = connection.inputStream.bufferedReader(Charsets.UTF_8).use { JSONObject(it.readText()) }
            val current = json.getJSONObject("current")
            if (current.isNull("temperature_2m") || current.isNull("weather_code") || current.isNull("is_day")) {
                throw IOException("Incomplete current conditions")
            }
            val temperature = current.getDouble("temperature_2m")
            val code = current.getInt("weather_code")
            val day = current.getInt("is_day")
            if (!temperature.isFinite() || day !in 0..1) throw IOException("Invalid weather response")
            return CurrentConditions(temperature, code, day == 1)
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun findPlaceName(context: Context, latitude: Double, longitude: Double): String {
        if (!Geocoder.isPresent()) return "Twoja okolica"
        return try {
            withTimeoutOrNull(3_000L) {
                val geocoder = Geocoder(context, Locale("pl", "PL"))
                val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine<Address?> { continuation ->
                        geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) {
                                if (continuation.isActive) continuation.resume(addresses.firstOrNull())
                            }
                            override fun onError(errorMessage: String?) {
                                if (continuation.isActive) continuation.resume(null)
                            }
                        })
                    }
                } else {
                    legacyPlaceName(geocoder, latitude, longitude)
                }
                listOf(address?.locality, address?.subLocality, address?.subAdminArea)
                    .firstOrNull { !it.isNullOrBlank() }
            } ?: "Twoja okolica"
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            "Twoja okolica"
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun legacyPlaceName(geocoder: Geocoder, latitude: Double, longitude: Double): Address? =
        suspendCancellableCoroutine { continuation ->
            val task = legacyGeocoderExecutor.submit {
                if (continuation.isActive) {
                    val address = try {
                        geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
                    } catch (_: Exception) {
                        null
                    }
                    if (continuation.isActive) continuation.resume(address)
                }
            }
            continuation.invokeOnCancellation { task.cancel(true) }
        }
}
