package org.deckgl.compose.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import org.deckgl.compose.DeckGL
import org.deckgl.compose.MapViewState
import org.deckgl.compose.Units
import org.deckgl.compose.accessor
import org.deckgl.compose.constant
import org.deckgl.compose.layers.ArcLayer
import org.deckgl.compose.layers.HeatmapLayer
import org.deckgl.compose.layers.HexagonLayer
import org.deckgl.compose.layers.RasterTileLayer
import org.deckgl.compose.layers.ScatterplotLayer
import org.deckgl.compose.layers.TextLayer
import org.deckgl.compose.rememberDeckState

private enum class Density { None, Heatmap, Hexagons }

private val Accent = Color(0xFFE8590C)
private val Selected = Color(0xFF1C7ED6)

private val Europe = MapViewState(longitude = 9.0, latitude = 48.5, zoom = 4.0, pitch = 30.0)

@Composable
fun DemoApp() {
  MaterialTheme {
    val deckState = rememberDeckState(Europe)
    var selected by remember { mutableStateOf(cities.first()) }
    var hovered by remember { mutableStateOf<Pair<City, Pair<Float, Float>>?>(null) }
    var showArcs by remember { mutableStateOf(true) }
    var showLabels by remember { mutableStateOf(true) }
    var density by remember { mutableStateOf(Density.None) }
    var radiusScale by remember { mutableFloatStateOf(1f) }

    Box(Modifier.fillMaxSize()) {
      DeckGL(
        modifier = Modifier.fillMaxSize(),
        state = deckState,
        onHover = { info -> if (!info.picked) hovered = null },
      ) {
        RasterTileLayer(
          id = "basemap",
          data = "https://tile.openstreetmap.org/{z}/{x}/{y}.png",
          opacity = 0.7f,
        )

        when (density) {
          Density.None -> {}
          Density.Heatmap -> HeatmapLayer(
            id = "heatmap",
            data = samples,
            getPosition = accessor { it },
            radiusPixels = 40.0,
          )
          Density.Hexagons -> HexagonLayer(
            id = "hexagons",
            data = samples,
            getPosition = accessor { it },
            radius = 20_000.0,
            extruded = true,
            elevationScale = 400.0,
            coverage = 0.9,
            opacity = 0.8f,
          )
        }

        if (showArcs) {
          ArcLayer(
            id = "arcs",
            data = cities.filter { it != selected },
            getSourcePosition = constant(selected.position),
            getTargetPosition = accessor { it.position },
            getSourceColor = constant(Selected),
            getTargetColor = constant(Accent),
            getWidth = constant(2),
          )
        }

        ScatterplotLayer(
          id = "cities",
          data = cities,
          getPosition = accessor { it.position },
          getRadius = accessor { kotlin.math.sqrt(it.population.toDouble()) * 40 },
          getFillColor = accessor { if (it == selected) Selected else Accent.copy(alpha = 0.8f) },
          getLineColor = constant(Color.White),
          getLineWidth = constant(2),
          stroked = true,
          lineWidthUnits = Units.Pixels,
          radiusScale = radiusScale.toDouble(),
          radiusMinPixels = 3.0,
          autoHighlight = true,
          highlightColor = Color(255, 255, 0, 200),
          onClick = { city, _ -> selected = city },
          onHover = { city, info -> hovered = city to (info.x to info.y) },
        )

        if (showLabels) {
          TextLayer(
            id = "labels",
            data = cities,
            getPosition = accessor { it.position },
            getText = accessor { it.name },
            getSize = constant(13),
            getColor = constant(Color(0xFF212529)),
            getPixelOffset = constant(listOf(0, -18)),
            outlineWidth = 2.0,
            outlineColor = Color.White,
            characterSet = "auto",
          )
        }
      }

      hovered?.let { (city, position) ->
        Card(Modifier.offset(position.first.dp + 12.dp, position.second.dp + 12.dp)) {
          Column(Modifier.padding(8.dp)) {
            Text(city.name, style = MaterialTheme.typography.titleSmall)
            Text("${formatThousands(city.population)} inhabitants", style = MaterialTheme.typography.bodySmall)
          }
        }
      }

      Text(
        "© OpenStreetMap contributors",
        Modifier.align(Alignment.BottomEnd).padding(4.dp),
        style = MaterialTheme.typography.labelSmall,
      )

      ControlPanel(
        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).width(300.dp),
        selected = selected,
        viewState = deckState.viewState,
        showArcs = showArcs,
        onShowArcs = { showArcs = it },
        showLabels = showLabels,
        onShowLabels = { showLabels = it },
        density = density,
        onDensity = { density = it },
        radiusScale = radiusScale,
        onRadiusScale = { radiusScale = it },
        onReset = { deckState.viewState = Europe },
      )
    }
  }
}

@Composable
private fun ControlPanel(
  modifier: Modifier,
  selected: City,
  viewState: MapViewState,
  showArcs: Boolean,
  onShowArcs: (Boolean) -> Unit,
  showLabels: Boolean,
  onShowLabels: (Boolean) -> Unit,
  density: Density,
  onDensity: (Density) -> Unit,
  radiusScale: Float,
  onRadiusScale: (Float) -> Unit,
  onReset: () -> Unit,
) {
  Card(modifier) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text("deck.gl × Compose", style = MaterialTheme.typography.titleMedium)
      Text("Click a city to connect it to the others.", style = MaterialTheme.typography.bodySmall)
      Text("Selected: ${selected.name}", style = MaterialTheme.typography.bodyMedium)

      LabeledSwitch("Arcs", showArcs, onShowArcs)
      LabeledSwitch("Labels", showLabels, onShowLabels)

      Text("Density", style = MaterialTheme.typography.labelLarge)
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Density.entries.forEach { option ->
          FilterChip(
            selected = density == option,
            onClick = { onDensity(option) },
            label = { Text(option.name) },
          )
        }
      }

      Text("Radius scale: ${(radiusScale * 10).roundToInt() / 10.0}", style = MaterialTheme.typography.labelLarge)
      Slider(value = radiusScale, onValueChange = onRadiusScale, valueRange = 0.2f..3f)

      Text(
        "lon ${viewState.longitude.format()}  lat ${viewState.latitude.format()}  " +
          "zoom ${viewState.zoom.format()}  pitch ${viewState.pitch.format()}  bearing ${viewState.bearing.format()}",
        style = MaterialTheme.typography.bodySmall,
      )
      TextButton(onClick = onReset) { Text("Reset view") }
    }
  }
}

@Composable
private fun LabeledSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Text(label, Modifier.weight(1f))
    Switch(checked = checked, onCheckedChange = onCheckedChange)
  }
}

private fun Double.format(): String = ((this * 100).roundToInt() / 100.0).toString()

private fun formatThousands(value: Int): String =
  value.toString().reversed().chunked(3).joinToString("'").reversed()
