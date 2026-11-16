package com.huanco.campusgps.data

import com.huanco.campusgps.model.Lugar

interface LugaresRepository {
    fun obtenerLugares(): List<Lugar>
}
