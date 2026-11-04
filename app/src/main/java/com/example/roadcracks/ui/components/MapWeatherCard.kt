package com.example.roadcracks.ui.components

import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Dehaze
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.roadcracks.data.WeatherKind
import com.example.roadcracks.data.WeatherReading
import com.example.roadcracks.data.WeatherRefreshPolicy
import com.example.roadcracks.data.WeatherRepository
import com.example.roadcracks.data.temperatureLabel
import com.example.roadcracks.data.weatherDescription
import com.example.roadcracks.data.weatherKind
import com.example.roadcracks.ui.theme.RoadCracksTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
internal fun MapWeatherCard(
    location: Location?,
    hasLocationPermission: Boolean,
    modifier: Modifier = Modifier
) {
    if (LocalInspectionMode.current) {
        WeatherTile(previewWeather(), loading = false, failed = false, hasLocation = true, modifier = modifier) {}
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestLocation = rememberUpdatedState(location)
    var reading by remember { mutableStateOf<WeatherReading?>(null) }
    var loading by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var retryRequest by remember { mutableIntStateOf(0) }

    // A new GPS sample updates latestLocation without cancelling an in-flight request.
    LaunchedEffect(lifecycleOwner, hasLocationPermission, location != null, retryRequest) {
        if (!hasLocationPermission || latestLocation.value == null) {
            reading = null
            loading = false
            failed = false
            return@LaunchedEffect
        }
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var forceRefresh = retryRequest > 0
            while (isActive) {
                val position = latestLocation.value
                if (position != null) {
                    val oldReading = reading
                    if (oldReading != null && !WeatherRefreshPolicy.isSameArea(
                        oldReading, position.latitude, position.longitude
                    )) reading = null
                    loading = true
                    try {
                        reading = WeatherRepository.load(
                            context.applicationContext, position.latitude, position.longitude, forceRefresh
                        )
                        failed = false
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        failed = true
                    } finally {
                        loading = false
                        forceRefresh = false
                    }
                }
                delay(30_000L)
            }
        }
    }

    val position = location
    val visibleReading = reading?.takeIf { data ->
        position != null && WeatherRefreshPolicy.isSameArea(data, position.latitude, position.longitude)
    }
    WeatherTile(
        reading = visibleReading,
        loading = loading,
        failed = failed,
        hasLocation = hasLocationPermission && location != null,
        modifier = modifier,
        stale = visibleReading != null && SystemClock.elapsedRealtime() - visibleReading.loadedAtElapsedMillis >=
            WeatherRefreshPolicy.REFRESH_INTERVAL_MILLIS,
        onRefresh = { retryRequest++ }
    )
}

@Composable
private fun WeatherTile(
    reading: WeatherReading?,
    loading: Boolean,
    failed: Boolean,
    hasLocation: Boolean,
    modifier: Modifier = Modifier,
    stale: Boolean = false,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    Surface(
        modifier = modifier.clickable(enabled = hasLocation && !loading, role = Role.Button,
            onClickLabel = "Odśwież pogodę", onClick = onRefresh),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
    ) {
        BoxWithConstraints(Modifier.padding(horizontal = 10.dp, vertical = 12.dp)) {
            val compact = maxWidth < 140.dp
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(min = 116.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
            ) {
                Text(
                    text = reading?.placeName ?: "Pogoda",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                if (reading != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 8.dp)
                    ) {
                        Icon(weatherIcon(reading), contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(if (compact) 26.dp else 34.dp))
                        Text(temperatureLabel(reading.temperatureCelsius),
                            fontSize = if (compact) 28.sp else 36.sp,
                            lineHeight = if (compact) 32.sp else 40.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1)
                    }
                    Text(weatherDescription(reading.weatherCode),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                } else {
                    if (loading && hasLocation) {
                        Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    } else {
                        Icon(if (hasLocation && failed) Icons.Default.CloudOff else Icons.Default.LocationOn,
                            contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp))
                    }
                    Text(when {
                        !hasLocation -> "Czekam na lokalizację"
                        failed -> "Brak pogody. Dotknij, aby ponowić."
                        else -> "Wczytuję pogodę…"
                    }, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                }
                if (reading != null && (failed || stale)) {
                    Text("Ostatnie dane", fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    text = "Open-Meteo ↗",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable(role = Role.Button, onClickLabel = "Źródło danych pogodowych") {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://open-meteo.com/")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    }.padding(top = 2.dp)
                )
            }
        }
    }
}

private fun weatherIcon(reading: WeatherReading): ImageVector = when (weatherKind(reading.weatherCode)) {
    WeatherKind.CLEAR -> if (reading.isDay) Icons.Default.WbSunny else Icons.Default.NightsStay
    WeatherKind.PARTLY_CLOUDY -> Icons.Default.CloudQueue
    WeatherKind.CLOUDY, WeatherKind.UNKNOWN -> Icons.Default.Cloud
    WeatherKind.FOG -> Icons.Default.Dehaze
    WeatherKind.DRIZZLE -> Icons.Default.Grain
    WeatherKind.RAIN -> Icons.Default.WaterDrop
    WeatherKind.SNOW -> Icons.Default.AcUnit
    WeatherKind.STORM -> Icons.Default.Thunderstorm
}

private fun previewWeather() = WeatherReading(50.06, 19.94, "Kraków", 18.0, 2, true, 0L)

@Preview(showBackground = true, name = "Pogoda — dzień")
@Composable
private fun WeatherCardPreview() {
    RoadCracksTheme(dynamicColor = false) {
        WeatherTile(previewWeather(), false, false, true, Modifier.padding(16.dp).width(184.dp)) {}
    }
}

@Preview(showBackground = true, name = "Pogoda — noc i wąski ekran")
@Composable
private fun WeatherCardNightPreview() {
    RoadCracksTheme(darkTheme = true, dynamicColor = false) {
        WeatherTile(previewWeather().copy(temperatureCelsius = -2.0, weatherCode = 0, isDay = false),
            false, false, true, Modifier.padding(16.dp).width(122.dp)) {}
    }
}

@Preview(showBackground = true, name = "Pogoda — brak sieci")
@Composable
private fun WeatherCardErrorPreview() {
    RoadCracksTheme(dynamicColor = false) {
        WeatherTile(null, false, true, true, Modifier.padding(16.dp).width(184.dp)) {}
    }
}
