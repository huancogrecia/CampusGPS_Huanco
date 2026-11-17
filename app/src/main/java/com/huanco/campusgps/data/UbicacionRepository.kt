package com.huanco.campusgps.data

import com.huanco.campusgps.model.Posicion

interface UbicacionRepository {
    fun playServicesDisponible(): Boolean
    suspend fun obtenerPosicion(): Result<Posicion>
}

class UbicacionNoDisponibleException : Exception()
