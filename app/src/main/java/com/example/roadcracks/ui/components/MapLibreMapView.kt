package com.example.roadcracks.ui.components

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.location.Location
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.roadcracks.R
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconAnchor
import org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource

@Composable
internal fun MapLibreMapView(
    location: Location?,
    recenterRequest: Int,
    onReady: () -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnReady = rememberUpdatedState(onReady)
    val currentOnError = rememberUpdatedState(onError)
    val host = remember(context, lifecycleOwner) {
        NativeMapHost(
            context = context,
            onReady = { currentOnReady.value() },
            onError = { currentOnError.value(it) }
        )
    }

    DisposableEffect(host, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> host.onLifecycleEvent(event) }
        val callbacks = object : ComponentCallbacks2 {
            override fun onConfigurationChanged(newConfig: Configuration) = Unit
            override fun onLowMemory() = host.onLowMemory()
            override fun onTrimMemory(level: Int) = host.onLowMemory()
        }
        context.applicationContext.registerComponentCallbacks(callbacks)
        lifecycleOwner.lifecycle.addObserver(observer)
        host.loadMap()

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            context.applicationContext.unregisterComponentCallbacks(callbacks)
            host.destroy()
        }
    }

    AndroidView(
        factory = { host.view },
        modifier = modifier,
        update = { host.updateLocation(location, recenterRequest) }
    )
}

private class NativeMapHost(
    context: Context,
    private val onReady: () -> Unit,
    private val onError: (String) -> Unit
) {
    val view: MapView
    private var map: MapLibreMap? = null
    private var source: GeoJsonSource? = null
    private var lastLocation: Location? = null
    private var recenterRequest = 0
    private var handledRecenterRequest = -1
    private var started = false
    private var resumed = false
    private var destroyed = false
    private val pinBitmap = requireNotNull(
        ContextCompat.getDrawable(context, R.drawable.ic_user_location_pin)
    ).toBitmap()
    private val loadingErrorListener = object : MapView.OnDidFailLoadingMapListener {
        override fun onDidFailLoadingMap(errorMessage: String) {
            if (!destroyed) onError("Nie udało się wczytać mapy. Sprawdź połączenie z internetem.")
        }
    }

    init {
        MapLibre.getInstance(context.applicationContext)
        view = MapView(context).apply {
            onCreate(Bundle())
            addOnDidFailLoadingMapListener(loadingErrorListener)
        }
    }

    fun loadMap() {
        if (destroyed) return
        view.getMapAsync { readyMap ->
            if (destroyed) return@getMapAsync
            map = readyMap
            readyMap.cameraPosition = CameraPosition.Builder()
                .target(LatLng(52.0, 19.0))
                .zoom(5.0)
                .build()
            // Keep MapLibre's attribution control and logo visible.
            readyMap.setStyle(Style.Builder().fromUri(OPEN_FREE_MAP_STYLE)) { style ->
                if (destroyed) return@setStyle
                style.addImage(PIN_IMAGE_ID, pinBitmap)
                source = GeoJsonSource(PIN_SOURCE_ID).also { style.addSource(it) }
                style.addLayer(SymbolLayer(PIN_LAYER_ID, PIN_SOURCE_ID).withProperties(
                    iconImage(PIN_IMAGE_ID),
                    iconAnchor(Property.ICON_ANCHOR_BOTTOM),
                    iconAllowOverlap(true),
                    iconIgnorePlacement(true)
                ))
                renderLocation()
                onReady()
            }
        }
    }

    fun updateLocation(location: Location?, request: Int) {
        if (destroyed) return
        lastLocation = location
        recenterRequest = request
        renderLocation()
    }

    private fun renderLocation() {
        if (destroyed) return
        val readySource = source ?: return
        val location = lastLocation
        if (location == null) {
            readySource.setGeoJson("""{"type":"FeatureCollection","features":[]}""")
            handledRecenterRequest = -1
            return
        }
        readySource.setGeoJson(
            """{"type":"Feature","geometry":{"type":"Point","coordinates":[${location.longitude},${location.latitude}]},"properties":{}}"""
        )
        if (handledRecenterRequest != recenterRequest) {
            map?.animateCamera(CameraUpdateFactory.newLatLngZoom(
                LatLng(location.latitude, location.longitude), 17.0
            ))
            handledRecenterRequest = recenterRequest
        }
    }

    fun onLifecycleEvent(event: Lifecycle.Event) {
        if (destroyed) return
        when (event) {
            Lifecycle.Event.ON_START -> start()
            Lifecycle.Event.ON_RESUME -> {
                start()
                if (!resumed) {
                    view.onResume()
                    resumed = true
                }
            }
            Lifecycle.Event.ON_PAUSE -> pause()
            Lifecycle.Event.ON_STOP -> stop()
            Lifecycle.Event.ON_DESTROY -> destroy()
            else -> Unit
        }
    }

    private fun start() {
        if (!started) {
            view.onStart()
            started = true
        }
    }

    private fun pause() {
        if (resumed) {
            view.onPause()
            resumed = false
        }
    }

    private fun stop() {
        pause()
        if (started) {
            view.onStop()
            started = false
        }
    }

    fun onLowMemory() {
        if (!destroyed) view.onLowMemory()
    }

    fun destroy() {
        if (destroyed) return
        stop()
        destroyed = true
        source = null
        map = null
        view.removeOnDidFailLoadingMapListener(loadingErrorListener)
        view.onDestroy()
    }

    private companion object {
        const val OPEN_FREE_MAP_STYLE = "https://tiles.openfreemap.org/styles/liberty"
        const val PIN_IMAGE_ID = "roadcracks-user-pin-image"
        const val PIN_SOURCE_ID = "roadcracks-user-location"
        const val PIN_LAYER_ID = "roadcracks-user-pin"
    }
}
