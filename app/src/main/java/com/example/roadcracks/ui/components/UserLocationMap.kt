package com.example.roadcracks.ui.components

import android.Manifest
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.roadcracks.utils.findActivity
import com.example.roadcracks.utils.hasLocationPermission
import com.example.roadcracks.utils.hasPreciseLocationPermission
import com.example.roadcracks.utils.isDeviceLocationEnabled
import com.example.roadcracks.utils.isRecentLocation
import com.example.roadcracks.utils.userLocationUpdates
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect

/** OpenFreeMap + MapLibre. The map requires internet access but no account or API key. */
@Composable
internal fun UserLocationMap(modifier: Modifier = Modifier) {
    if (LocalInspectionMode.current) {
        MapPreviewPlaceholder(modifier)
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionGranted by remember { mutableStateOf(hasLocationPermission(context)) }
    var locationEnabled by remember { mutableStateOf(isDeviceLocationEnabled(context)) }
    var permanentlyDenied by remember { mutableStateOf(false) }
    var userLocation by remember { mutableStateOf<Location?>(null) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var mapError by remember { mutableStateOf<String?>(null) }
    var mapReady by remember { mutableStateOf(false) }
    var mapGeneration by remember { mutableIntStateOf(0) }
    var locationRetry by remember { mutableIntStateOf(0) }
    var recenterRequest by remember { mutableIntStateOf(0) }

    val permissions = remember { arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permissionGranted = hasLocationPermission(context)
        permanentlyDenied = !permissionGranted && context.findActivity()?.let { activity ->
            permissions.none { permission ->
                ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
            }
        } == true
        locationError = null
        if (!permissionGranted) userLocation = null
    }

    LaunchedEffect(lifecycleOwner, permissionGranted, locationRetry) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            // Re-check after returning from system settings or a one-time permission.
            val allowedNow = hasLocationPermission(context)
            permissionGranted = allowedNow
            locationEnabled = isDeviceLocationEnabled(context)
            locationError = null
            if (!allowedNow) {
                userLocation = null
                return@repeatOnLifecycle
            }
            permanentlyDenied = false
            if (userLocation?.let(::isRecentLocation) == false) userLocation = null
            if (!locationEnabled) return@repeatOnLifecycle

            try {
                userLocationUpdates(context).collect { userLocation = it }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: SecurityException) {
                permissionGranted = false
                userLocation = null
            } catch (_: Exception) {
                locationError = "Nie udało się pobrać pozycji. Spróbuj ponownie."
            }
        }
    }

    val status = when {
        mapError != null -> mapError.orEmpty()
        !mapReady -> "Wczytywanie mapy…"
        !permissionGranted && permanentlyDenied -> "Zezwól na lokalizację w ustawieniach aplikacji."
        !permissionGranted -> "Udostępnij lokalizację, aby zobaczyć swoją pinezkę."
        !locationEnabled -> "Włącz lokalizację w telefonie."
        locationError != null -> locationError.orEmpty()
        userLocation == null -> "Ustalanie pozycji… Pierwszy pomiar może chwilę potrwać."
        !hasPreciseLocationPermission(context) -> "Twoja przybliżona lokalizacja"
        else -> "Twoja lokalizacja"
    }
    val actionLabel = when {
        mapError != null -> "Ponów mapę"
        !permissionGranted && permanentlyDenied -> "Ustawienia"
        !permissionGranted -> "Udostępnij pozycję"
        !locationEnabled -> "Włącz lokalizację"
        locationError != null -> "Ponów pozycję"
        else -> "Moja pozycja"
    }

    Column(modifier) {
        key(mapGeneration) {
            MapLibreMapView(
                location = userLocation,
                recenterRequest = recenterRequest,
                onReady = { mapReady = true; mapError = null },
                onError = { mapError = it },
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        }
        LocationControls(status, actionLabel) {
            when {
                mapError != null -> {
                    mapReady = false
                    mapError = null
                    mapGeneration++
                }
                !hasLocationPermission(context) && permanentlyDenied -> {
                    runCatching {
                        context.startActivity(Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${context.packageName}")
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }.onFailure { locationError = "Otwórz uprawnienia aplikacji w ustawieniach telefonu." }
                }
                !hasLocationPermission(context) -> permissionLauncher.launch(permissions)
                !isDeviceLocationEnabled(context) -> {
                    runCatching {
                        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }.onFailure { locationError = "Włącz lokalizację w ustawieniach telefonu." }
                }
                else -> {
                    permissionGranted = true
                    recenterRequest++
                    if (userLocation == null || locationError != null) locationRetry++
                }
            }
        }
    }
}

@Composable
private fun LocationControls(status: String, actionLabel: String, onAction: () -> Unit) {
    // A separate panel keeps the map's attribution control unobstructed.
    Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(status, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            FilledTonalButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
private fun MapPreviewPlaceholder(modifier: Modifier) {
    Column(modifier) {
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().background(Color(0xFFE3E8E5)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Place, contentDescription = null,
                    modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Text("OpenFreeMap", style = MaterialTheme.typography.titleMedium)
                Text("Mapa działa na telefonie lub emulatorze",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
        LocationControls("Twoja lokalizacja", "Moja pozycja") {}
    }
}
