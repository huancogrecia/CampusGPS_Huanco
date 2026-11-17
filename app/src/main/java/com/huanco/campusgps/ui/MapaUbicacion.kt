package com.huanco.campusgps.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
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
import com.huanco.campusgps.model.LugarConDistancia
import com.huanco.campusgps.model.UbicacionUi
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.Marker
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
    lugares: List<LugarConDistancia>,
    seleccionado: Int?,
    idMasCercano: Int?,
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
    val marcadoresLugares = remember(mapa) { mutableMapOf<Int, Marker>() }
    var marcadorUsuario by remember(mapa) { mutableStateOf<Marker?>(null) }
    val iconosLugares = remember(mapa) {
        mapa?.let {
            val fabrica = IconFactory.getInstance(context)
            val original = fabrica.defaultMarker().bitmap
            val azul = fabrica.fromBitmap(colorearMarcador(original, verde = false))
            val verde = fabrica.fromBitmap(colorearMarcador(original, verde = true))
            azul to verde
        }
    }

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

    LaunchedEffect(mapa, lugares, idMasCercano) {
        val actual = mapa ?: return@LaunchedEffect
        val (azul, verde) = iconosLugares ?: return@LaunchedEffect
        val lugaresPorId = lugares.associateBy { it.lugar.id }

        (marcadoresLugares.keys - lugaresPorId.keys).forEach { id ->
            marcadoresLugares.remove(id)?.let { actual.removeMarker(it) }
        }
        lugaresPorId.forEach { (id, dato) ->
            val lugar = dato.lugar
            val posicion = LatLng(lugar.latitud, lugar.longitud)
            val icono = if (id == idMasCercano) verde else azul
            val anterior = marcadoresLugares[id]
            if (anterior?.position != posicion || anterior.title != lugar.nombre ||
                anterior.snippet != lugar.categoria || anterior.icon != icono
            ) {
                anterior?.let { actual.removeMarker(it) }
                marcadoresLugares[id] = actual.addMarker(
                    MarkerOptions()
                        .position(posicion)
                        .title(lugar.nombre)
                        .snippet(lugar.categoria)
                        .icon(icono)
                )
            }
        }
    }

    LaunchedEffect(mapa, estado) {
        val actual = mapa ?: return@LaunchedEffect
        marcadorUsuario?.let { actual.removeMarker(it) }
        marcadorUsuario = null
        if (estado is UbicacionUi.Lista) {
            val posicion = LatLng(estado.posicion.latitud, estado.posicion.longitud)
            val marcador = actual.addMarker(
                MarkerOptions().position(posicion).title("Estás aquí")
            )
            marcadorUsuario = marcador
            if (seleccionado == null) {
                actual.animateCamera(CameraUpdateFactory.newLatLngZoom(posicion, 16.0))
                actual.selectMarker(marcador)
            }
        }
    }

    LaunchedEffect(mapa, seleccionado) {
        val actual = mapa ?: return@LaunchedEffect
        val marcador = marcadoresLugares[seleccionado] ?: return@LaunchedEffect
        actual.animateCamera(CameraUpdateFactory.newLatLngZoom(marcador.position, 17.0))
        actual.selectMarker(marcador)
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

private fun colorearMarcador(original: Bitmap, verde: Boolean): Bitmap {
    val resultado = Bitmap.createBitmap(original.width, original.height, Bitmap.Config.ARGB_8888)
    resultado.density = original.density
    val pintura = Paint().apply {
        colorFilter = ColorMatrixColorFilter(
            floatArrayOf(
                0f, 0f, 1f, 0f, 0f,
                if (verde) 1f else 0.45f, if (verde) 0f else 0.55f, 0f, 0f, 0f,
                if (verde) 0f else 1f, if (verde) 1f else 0f, 0f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }
    Canvas(resultado).drawBitmap(original, 0f, 0f, pintura)
    return resultado
}
