package com.example.roadcracks.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.maplibre.android.location.engine.LocationEngineCallback
import org.maplibre.android.location.engine.LocationEngineDefault
import org.maplibre.android.location.engine.LocationEngineRequest
import org.maplibre.android.location.engine.LocationEngineResult

internal fun hasPreciseLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

internal fun hasLocationPermission(context: Context): Boolean =
    hasPreciseLocationPermission(context) ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

internal fun isDeviceLocationEnabled(context: Context): Boolean {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return LocationManagerCompat.isLocationEnabled(manager)
}

internal fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.takeIf { it !== this }?.findActivity()
    else -> null
}

internal fun isRecentLocation(location: Location): Boolean {
    val ageMillis = if (location.elapsedRealtimeNanos > 0L) {
        (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000L
    } else {
        System.currentTimeMillis() - location.time
    }
    return location.latitude.isFinite() && location.latitude in -90.0..90.0 &&
        location.longitude.isFinite() && location.longitude in -180.0..180.0 &&
        ageMillis in -5_000L..120_000L
}

/** Android's location providers; no Google Cloud account or API key is involved. */
@SuppressLint("MissingPermission") // Checked immediately before requesting updates.
internal fun userLocationUpdates(context: Context): Flow<Location> = callbackFlow {
    if (!hasLocationPermission(context)) {
        close(SecurityException("Location permission is required"))
        return@callbackFlow
    }

    val engine = LocationEngineDefault.getDefaultLocationEngine(context.applicationContext)
    val callback = object : LocationEngineCallback<LocationEngineResult> {
        override fun onSuccess(result: LocationEngineResult) {
            val location = result.lastLocation ?: return
            if (isRecentLocation(location)) trySend(Location(location))
        }

        override fun onFailure(exception: Exception) {
            close(exception)
        }
    }
    val request = LocationEngineRequest.Builder(5_000L)
        .setFastestInterval(2_000L)
        .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
        .build()

    try {
        engine.requestLocationUpdates(request, callback, Looper.getMainLooper())
    } catch (exception: Exception) {
        close(exception)
    }

    awaitClose { engine.removeLocationUpdates(callback) }
}
