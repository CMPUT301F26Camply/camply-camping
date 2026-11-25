package com.example.camply.campsite.view

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.createBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp


// CampsiteMap.kt - Shared Campsite Map Component
//
// Contributor: Filip Kosmajac (fkosmajac)
// AI Assistance Reference:
// OpenAI ChatGPT 6
// https://chatgpt.com/
//
// Purpose of AI assistance:
// AI was used to research suitable open source mapping options and help implement a reusable
// campsite map component that integrates with the existing Camply interface and theme.
//
// Prompt:
// "I was initially considering Google Maps for our campsite location feature but im not
// sure if it requires a billing account even if basic map usage is free. I remember using
// OpenStreetMap in other projects and believe its licensing might allow free use in our
// uni project. Can you verify the licensing and suggest an alternative without payment
// details? I want a reusable map component for both campsite registration and the campsite
// details screen. Owners should be able to interact with the map and tap to select or
// reposition a pin, while the details screen should only display the saved pin. The component
// should support centering on coordinates, allow zooming and panning, and use our existing Camply
// theme presets."
//
// Additional follow-up prompts were used to resolve deprecated MapLibre APIs, correctly manage
// the MapView lifecycle, address/fix potential edge cases, and further refine the component to
// fit our project requirements and existing application structure.

// Mapping libraries and licenses:
// MapLibre Native: BSD 2-Clause
// https://github.com/maplibre/maplibre-native
//
// OpenFreeMap: MIT (software), hosted service terms apply
// https://openfreemap.org/
// https://openfreemap.org/tos/
//
// OpenStreetMap data: Open Database License (ODbL)
// https://www.openstreetmap.org/copyright




private const val PIN_SOURCE_ID = "camply-campsite-pin-source"
private const val PIN_LAYER_ID = "camply-campsite-pin-layer"
private const val PIN_ICON_ID = "camply-campsite-pin-icon"

/**
 * Shared campsite map used by the registration form and details screen.
 *
 * @param latitude Latitude of the selected campsite pin, if any.
 * @param longitude Longitude of the selected campsite pin, if any.
 * @param editable Whether tapping the map can change the pin.
 * @param modifier Modifier controlling the size and layout of the map.
 * @param onLocationSelected Called with coordinates when a pin is placed.
 * @param focusLatitude Optional latitude to move the camera toward.
 * @param focusLongitude Optional longitude to move the camera toward.
 */
@Composable
fun CampsiteMap(
    latitude: Double?,
    longitude: Double?,
    editable: Boolean,
    modifier: Modifier = Modifier,
    onLocationSelected: (Double, Double) -> Unit = { _, _ -> },
    focusLatitude: Double? = null,
    focusLongitude: Double? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Use the active Camply preset (including dark mode), never a fixed color.
    val pinFillColor = MaterialTheme.colorScheme.primary.toArgb()
    val pinAccentColor = MaterialTheme.colorScheme.onPrimary.toArgb()
    val pinBitmap = remember(pinFillColor, pinAccentColor) {
        createCampsitePinBitmap(pinFillColor, pinAccentColor)
    }
    val currentPinBitmap by rememberUpdatedState(pinBitmap)

    val currentEditable by rememberUpdatedState(editable)
    val currentOnLocationSelected by rememberUpdatedState(onLocationSelected)

    // MapLibre must be initialized before creating a MapView.
    val mapView = remember(context) {
        MapLibre.getInstance(context)
        MapView(context).apply { onCreate(null) }
    }

    var map by remember(mapView) {
        mutableStateOf<MapLibreMap?>(null)
    }
    var styleLoaded by remember(mapView) {
        mutableStateOf(false)
    }

    DisposableEffect(mapView, lifecycleOwner) {
        val lifecycle = lifecycleOwner.lifecycle
        var disposed = false

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }

        // The map may enter composition after the activity is already resumed.
        // Catch up with the current lifecycle before observing future events.
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            mapView.onStart()
        }
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            mapView.onResume()
        }
        lifecycle.addObserver(observer)

        val clickListener = MapLibreMap.OnMapClickListener { point ->
            if (currentEditable) {
                currentOnLocationSelected(point.latitude, point.longitude)
                true
            } else {
                false
            }
        }

        mapView.getMapAsync { readyMap ->
            if (!disposed) {
                map = readyMap
                readyMap.addOnMapClickListener(clickListener)

                readyMap.setStyle("https://tiles.openfreemap.org/styles/liberty") { style ->
                    if (!disposed) {
                        // Style layers replace the deprecated marker annotation API.
                        style.addImage(PIN_ICON_ID, currentPinBitmap)
                        style.addSource(GeoJsonSource(PIN_SOURCE_ID))
                        style.addLayer(
                            SymbolLayer(PIN_LAYER_ID, PIN_SOURCE_ID).withProperties(
                                PropertyFactory.iconImage(PIN_ICON_ID),
                                PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM),
                                PropertyFactory.iconAllowOverlap(true),
                                PropertyFactory.iconIgnorePlacement(true),
                                PropertyFactory.iconSize(0.65f)
                            )
                        )
                        styleLoaded = true
                    }
                }
            }
        }

        onDispose {
            disposed = true
            map?.removeOnMapClickListener(clickListener)
            lifecycle.removeObserver(observer)

            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                mapView.onPause()
            }
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                mapView.onStop()
            }
            mapView.onDestroy()
        }
    }

    // Update the existing map icon when Camply's theme/preset changes.
    LaunchedEffect(map, styleLoaded, pinBitmap) {
        if (styleLoaded) {
            map?.style?.addImage(PIN_ICON_ID, pinBitmap)
        }
    }

    // Center on the saved pin, or show Canada if the campsite has no pin yet.
    LaunchedEffect(
        map,
        styleLoaded,
        if (editable) null else latitude,
        if (editable) null else longitude
    ) {
        val readyMap = map ?: return@LaunchedEffect
        if (!styleLoaded) return@LaunchedEffect

        val selectedLocation = latitude?.let { lat ->
            longitude?.let { lng ->
                if (lat in -90.0..90.0 && lng in -180.0..180.0) {
                    LatLng(lat, lng)
                } else {
                    null
                }
            }
        }

        readyMap.cameraPosition = CameraPosition.Builder()
            .target(selectedLocation ?: LatLng(56.1304, -106.3468))
            .zoom(if (selectedLocation != null) 12.0 else 3.0)
            .build()
    }

    // Search results can move the camera independently of the selected pin.
    LaunchedEffect(map, styleLoaded, focusLatitude, focusLongitude) {
        val readyMap = map ?: return@LaunchedEffect
        if (styleLoaded &&
            focusLatitude != null && focusLongitude != null &&
            focusLatitude in -90.0..90.0 && focusLongitude in -180.0..180.0
        ) {
            readyMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(focusLatitude, focusLongitude),
                    12.0
                )
            )
        }
    }

    // Update the GeoJSON point when a map pin is placed, moved, or cleared.
    LaunchedEffect(map, styleLoaded, latitude, longitude) {
        val readyMap = map ?: return@LaunchedEffect
        if (!styleLoaded) return@LaunchedEffect

        val source = readyMap.style?.getSourceAs<GeoJsonSource>(PIN_SOURCE_ID)
            ?: return@LaunchedEffect

        if (latitude != null && longitude != null &&
            latitude in -90.0..90.0 && longitude in -180.0..180.0
        ) {
            // GeoJSON uses longitude before latitude.
            source.setGeoJson(Point.fromLngLat(longitude, latitude))
        } else {
            source.setGeoJson(FeatureCollection.fromFeatures(emptyArray<Feature>()))
        }
    }


    Box(modifier = modifier) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        // Zoom controls use the active Camply theme.
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp
        ) {
            Column {
                IconButton(
                    onClick = {
                        map?.animateCamera(
                            CameraUpdateFactory.zoomIn()
                        )
                    },
                    enabled = map != null
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom in",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = {
                        map?.animateCamera(
                            CameraUpdateFactory.zoomOut()
                        )
                    },
                    enabled = map != null
                ) {
                    Text(
                        text = "−",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

}

/** Creates the campsite pin icon using the active Camply theme colors. */
private fun createCampsitePinBitmap(fillColor: Int, accentColor: Int): Bitmap {
    val bitmap = createBitmap(width = 80, height = 104)
    val canvas = Canvas(bitmap)

    val shape = Path().apply {
        moveTo(40f, 100f)
        cubicTo(29f, 80f, 8f, 56f, 8f, 39f)
        cubicTo(8f, 19f, 22f, 7f, 40f, 7f)
        cubicTo(58f, 7f, 72f, 19f, 72f, 39f)
        cubicTo(72f, 56f, 51f, 80f, 40f, 100f)
        close()
    }

    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
    }
    val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    val center = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
    }

    canvas.drawPath(shape, fill)
    canvas.drawPath(shape, outline)
    canvas.drawCircle(40f, 38f, 12f, center)

    return bitmap
}
