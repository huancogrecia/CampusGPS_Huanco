package com.huanco.campusgps.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.huanco.campusgps.model.LugarConDistancia
import com.huanco.campusgps.util.formatearDistancia

@Composable
fun ListaLugares(
    lugares: List<LugarConDistancia>,
    seleccionado: Int?,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(lugares, key = { it.lugar.id }) { (lugar, metros) ->
            val estaSeleccionado = lugar.id == seleccionado
            val colores = MaterialTheme.colorScheme
            Card(
                onClick = { onClick(lugar.id) },
                modifier = Modifier.fillMaxWidth().semantics { selected = estaSeleccionado },
                colors = CardDefaults.cardColors(
                    containerColor = if (estaSeleccionado) colores.primaryContainer else colores.surfaceVariant,
                    contentColor = if (estaSeleccionado) colores.onPrimaryContainer else colores.onSurfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(lugar.nombre, style = MaterialTheme.typography.titleSmall)
                        Text(lugar.categoria, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        text = metros?.let(::formatearDistancia) ?: "Sin calcular",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}
