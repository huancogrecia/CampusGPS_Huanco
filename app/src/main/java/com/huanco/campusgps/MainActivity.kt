package com.huanco.campusgps

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.LocationServices
import com.huanco.campusgps.data.LugaresRepositoryLocal
import com.huanco.campusgps.data.PreferenciasRepositoryImpl
import com.huanco.campusgps.data.UbicacionRepositoryGoogle
import com.huanco.campusgps.ui.PantallaMapa
import com.huanco.campusgps.ui.theme.CampusGPSTheme
import com.huanco.campusgps.viewmodel.CampusViewModelFactory
import com.huanco.campusgps.viewmodel.MapaViewModel

class MainActivity : ComponentActivity() {
    private val mapaViewModel: MapaViewModel by lazy {
        val clienteUbicacion = LocationServices.getFusedLocationProviderClient(this)
        val ubicacionRepository = UbicacionRepositoryGoogle(this, clienteUbicacion)
        val lugaresRepository = LugaresRepositoryLocal()
        val preferenciasRepository = PreferenciasRepositoryImpl(applicationContext)
        ViewModelProvider(
            this,
            CampusViewModelFactory(ubicacionRepository, lugaresRepository, preferenciasRepository)
        )[MapaViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CampusGPSTheme {
                PantallaMapa(mapaViewModel)
            }
        }
    }
}
