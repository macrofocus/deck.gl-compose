package org.deckgl.compose.layers

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.deckgl.compose.Accessor
import org.deckgl.compose.Aggregation
import org.deckgl.compose.DeckComposable
import org.deckgl.compose.ItemHandler
import org.deckgl.compose.Position
import org.deckgl.compose.constant
import org.deckgl.compose.internal.EmitLayer
import org.deckgl.compose.internal.LayerData

/** deck.gl's default `colorRange` of the aggregation layers (ColorBrewer YlOrRd). */
public val DefaultColorRange: List<Color> = listOf(
  Color(255, 255, 178),
  Color(254, 217, 118),
  Color(254, 178, 76),
  Color(253, 141, 60),
  Color(240, 59, 32),
  Color(189, 0, 38),
)

/**
 * Aggregates points into hexagonal bins, colored and optionally extruded by their weight,
 * deck.gl's [HexagonLayer](https://deck.gl/docs/api-reference/aggregation-layers/hexagon-layer).
 *
 * Picking reports bins, not items: the handlers receive `null` items.
 */
@Composable
@DeckComposable
public fun <T> HexagonLayer(
  id: String,
  data: List<T>,
  getPosition: Accessor<T, Position>,
  getColorWeight: Accessor<T, Number> = constant(1),
  getElevationWeight: Accessor<T, Number> = constant(1),
  radius: Double = 1000.0,
  colorAggregation: Aggregation = Aggregation.Sum,
  elevationAggregation: Aggregation = Aggregation.Sum,
  colorRange: List<Color> = DefaultColorRange,
  elevationRange: Pair<Double, Double> = 0.0 to 1000.0,
  elevationScale: Double = 1.0,
  extruded: Boolean = false,
  coverage: Double = 1.0,
  upperPercentile: Double = 100.0,
  lowerPercentile: Double = 0.0,
  visible: Boolean = true,
  opacity: Float = 1f,
  pickable: Boolean = false,
  autoHighlight: Boolean = false,
  highlightColor: Color? = null,
  onClick: ItemHandler<Nothing?>? = null,
  onHover: ItemHandler<Nothing?>? = null,
): Unit = EmitLayer(
  type = "HexagonLayer",
  id = id,
  data = LayerData.Items(data),
  props = mapOf(
    "radius" to radius,
    "colorAggregation" to colorAggregation,
    "elevationAggregation" to elevationAggregation,
    "colorRange" to colorRange.map { it.copy(alpha = 1f) },
    "elevationRange" to listOf(elevationRange.first, elevationRange.second),
    "elevationScale" to elevationScale,
    "extruded" to extruded,
    "coverage" to coverage,
    "upperPercentile" to upperPercentile,
    "lowerPercentile" to lowerPercentile,
  ),
  accessors = mapOf(
    "getPosition" to getPosition,
    "getColorWeight" to getColorWeight,
    "getElevationWeight" to getElevationWeight,
  ),
  visible, opacity, pickable, autoHighlight, highlightColor,
  onClick?.let { handler -> { _: T, info -> handler(null, info) } },
  onHover?.let { handler -> { _: T, info -> handler(null, info) } },
)

/**
 * A density map of weighted points, deck.gl's
 * [HeatmapLayer](https://deck.gl/docs/api-reference/aggregation-layers/heatmap-layer).
 */
@Composable
@DeckComposable
public fun <T> HeatmapLayer(
  id: String,
  data: List<T>,
  getPosition: Accessor<T, Position>,
  getWeight: Accessor<T, Number> = constant(1),
  radiusPixels: Double = 30.0,
  intensity: Double = 1.0,
  threshold: Double = 0.05,
  colorRange: List<Color> = DefaultColorRange,
  aggregation: Aggregation = Aggregation.Sum,
  visible: Boolean = true,
  opacity: Float = 1f,
): Unit = EmitLayer(
  type = "HeatmapLayer",
  id = id,
  data = LayerData.Items(data),
  props = mapOf(
    "radiusPixels" to radiusPixels,
    "intensity" to intensity,
    "threshold" to threshold,
    "colorRange" to colorRange.map { it.copy(alpha = 1f) },
    "aggregation" to aggregation,
  ),
  accessors = mapOf(
    "getPosition" to getPosition,
    "getWeight" to getWeight,
  ),
  visible, opacity,
  pickable = false, autoHighlight = false, highlightColor = null, onClick = null, onHover = null,
)
