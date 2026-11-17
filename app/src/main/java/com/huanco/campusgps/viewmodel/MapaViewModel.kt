package com.huanco.campusgps.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huanco.campusgps.data.LugaresRepository
import com.huanco.campusgps.data.UbicacionRepository
import com.huanco.campusgps.data.UbicacionNoDisponibleException
import com.huanco.campusgps.model.Lugar
import com.huanco.campusgps.model.LugarConDistancia
import com.huanco.campusgps.model.Posicion
import com.huanco.campusgps.model.TipoError
import com.huanco.campusgps.model.UbicacionUi
import com.huanco.campusgps.util.distanciaMetros
import com.huanco.campusgps.util.formatearDistancia
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MapaViewModel(
    private val ubicacionRepo: UbicacionRepository,
    lugaresRepo: LugaresRepository
) : ViewModel() {
    private val _estado = MutableStateFlow<UbicacionUi>(UbicacionUi.Inicial)
    val estado: StateFlow<UbicacionUi> = _estado.asStateFlow()
    private val lugaresCampus = lugaresRepo.obtenerLugares()
    private val _lugares = MutableStateFlow(
        lugaresCampus.map { LugarConDistancia(it, null) }
    )
    val lugares: StateFlow<List<LugarConDistancia>> = _lugares.asStateFlow()
    val masCercano: StateFlow<Lugar?> = lugares
        .map { lista -> lista.firstOrNull { it.metros != null }?.lugar }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    private val _seleccionado = MutableStateFlow<Int?>(null)
    val seleccionado: StateFlow<Int?> = _seleccionado.asStateFlow()

    fun buscar() {
        if (_estado.value is UbicacionUi.Buscando) return
        if (!ubicacionRepo.playServicesDisponible()) {
            _estado.value = UbicacionUi.Error(
                "Este dispositivo no tiene Google Play Services",
                TipoError.SIN_PLAY_SERVICES
            )
            return
        }
        _estado.value = UbicacionUi.Buscando
        viewModelScope.launch {
            ubicacionRepo.obtenerPosicion()
                .onSuccess { posicion ->
                    actualizarDistancias(posicion)
                    seleccionar(null)
                    _estado.value = UbicacionUi.Lista(posicion)
                }
                .onFailure { error ->
                    when (error) {
                        is SecurityException -> permisoDenegado()
                        is UbicacionNoDisponibleException -> {
                            _estado.value = UbicacionUi.Error(
                                "Activa el GPS e intenta de nuevo",
                                TipoError.GPS_APAGADO
                            )
                        }
                        else -> {
                            _estado.value = UbicacionUi.Error(
                                "No se pudo obtener la ubicación",
                                TipoError.OTRO
                            )
                        }
                    }
                }
        }
    }

    private fun actualizarDistancias(pos: Posicion) {
        val ordenados = lugaresCampus
            .map { lugar -> LugarConDistancia(lugar, distanciaMetros(pos, lugar)) }
            .sortedBy { it.metros }
        _lugares.value = ordenados
        Log.d("CampusGPS", ordenados.joinToString(" | ") { dato ->
            "${dato.lugar.nombre}: ${formatearDistancia(checkNotNull(dato.metros))}"
        })
    }

    fun seleccionar(id: Int?) {
        _seleccionado.value = id
    }

    fun permisoDenegado() {
        _estado.value = UbicacionUi.Error(
            "Sin permiso de ubicación no puedo mostrar dónde estás",
            TipoError.SIN_PERMISO
        )
    }
}
