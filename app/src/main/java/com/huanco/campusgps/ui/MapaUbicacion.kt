package com.huanco.campusgps.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.huanco.campusgps.model.UbicacionUi
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

private const val ESTILO_MAPA = "https://tiles.openfreemap.org/styles/liberty"

@Suppress("DEPRECATION")
@Composable
fun MapaUbicacion(
    estado: UbicacionUi,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember(context, lifecycle) {
        MapLibre.getInstance(context.applicationContext)
        MapView(context).apply { onCreate(null) }
    }
    var mapa by remember(mapView) { mutableStateOf<MapLibreMap?>(null) }
    var error by remember(mapView) { mutableStateOf(false) }

    DisposableEffect(mapView, lifecycle) {
        val fallo = MapView.OnDidFailLoadingMapListener { error = true }
        mapView.addOnDidFailLoadingMapListener(fallo)
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        mapView.getMapAsync { disponible ->
            disponible.uiSettings.isLogoEnabled = false
            disponible.uiSettings.isAttributionEnabled = false
            disponible.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(-12.0464, -77.0428), 12.0))
            disponible.setStyle(ESTILO_MAPA) {
                error = false
                mapa = disponible
            }
        }
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.removeOnDidFailLoadingMapListener(fallo)
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onPause()
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStop()
            mapView.onDestroy()
        }
    }

    LaunchedEffect(mapa, estado) {
        val actual = mapa ?: return@LaunchedEffect
        actual.markers.toList().forEach { actual.removeMarker(it) }
        if (estado is UbicacionUi.Lista) {
            val posicion = LatLng(estado.posicion.latitud, estado.posicion.longitud)
            val marcador = actual.addMarker(
                MarkerOptions().position(posicion).title("Estás aquí")
            )
            actual.animateCamera(CameraUpdateFactory.newLatLngZoom(posicion, 16.0))
            actual.selectMarker(marcador)
        }
    }

    Box(modifier) {
        AndroidView(factory = { mapView }, modifier = Modifier.matchParentSize())
        if (error) {
            Surface(modifier = Modifier.align(Alignment.Center)) {
                Text("No se pudo cargar el mapa", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
