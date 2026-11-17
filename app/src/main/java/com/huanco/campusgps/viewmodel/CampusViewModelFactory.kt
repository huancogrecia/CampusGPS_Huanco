package com.huanco.campusgps.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.huanco.campusgps.data.LugaresRepository
import com.huanco.campusgps.data.PreferenciasRepository
import com.huanco.campusgps.data.UbicacionRepository

class CampusViewModelFactory(
    private val ubicacionRepository: UbicacionRepository,
    private val lugaresRepository: LugaresRepository,
    private val preferenciasRepository: PreferenciasRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapaViewModel::class.java)) {
            return MapaViewModel(ubicacionRepository, lugaresRepository, preferenciasRepository) as T
        }
        throw IllegalArgumentException("ViewModel desconocido")
    }
}
