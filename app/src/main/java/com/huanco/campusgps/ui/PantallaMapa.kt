package com.huanco.campusgps.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.huanco.campusgps.model.UbicacionUi
import com.huanco.campusgps.util.formatearDistancia
import com.huanco.campusgps.viewmodel.MapaViewModel

@Composable
fun PantallaMapa(viewModel: MapaViewModel) {
    val context = LocalContext.current
    val estado by viewModel.estado.collectAsState()
    val lugares by viewModel.lugares.collectAsState()
    val seleccionado by viewModel.seleccionado.collectAsState()
    val masCercano by viewModel.masCercano.collectAsState()
    val solicitudPermisos = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permisos ->
        if (permisos[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permisos[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            viewModel.buscar()
        } else {
            viewModel.permisoDenegado()
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Text(
                    text = "CampusGPS",
                    modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(16.dp),
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (estado !is UbicacionUi.Error) {
                MapaUbicacion(
                    estado = estado,
                    lugares = lugares,
                    seleccionado = seleccionado,
                    idMasCercano = masCercano?.id,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(
                enabled = estado !is UbicacionUi.Buscando,
                onClick = {
                    val tienePermisoPreciso = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    val tienePermisoAproximado = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                    if (tienePermisoPreciso || tienePermisoAproximado) {
                        viewModel.buscar()
                    } else {
                        solicitudPermisos.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                }
            ) {
                Text("Obtener mi ubicación")
            }
            if (estado !is UbicacionUi.Error) {
                OutlinedButton(
                    enabled = masCercano != null,
                    onClick = { masCercano?.let { viewModel.seleccionar(it.id) } }
                ) {
                    Text("Ir al más cercano")
                }
                masCercano?.let { lugar ->
                    lugares.firstOrNull { it.lugar.id == lugar.id }?.metros?.let { metros ->
                        Text(
                            text = "Más cercano: ${lugar.nombre} (${formatearDistancia(metros)})",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            when (val actual = estado) {
                UbicacionUi.Inicial -> Text("Presiona el botón para consultar tu ubicación")
                UbicacionUi.Buscando -> CircularProgressIndicator()
                is UbicacionUi.Lista -> {
                    Text("Latitud: ${actual.posicion.latitud}")
                    Text("Longitud: ${actual.posicion.longitud}")
                }
                is UbicacionUi.Error -> MensajeError(actual, onReintentar = viewModel::buscar)
            }
            Spacer(Modifier.height(12.dp))
            if (estado !is UbicacionUi.Error) {
                ListaLugares(
                    lugares = lugares,
                    seleccionado = seleccionado,
                    onClick = viewModel::seleccionar,
                    modifier = Modifier.fillMaxWidth().height(264.dp)
                )
            }
        }
    }
}
