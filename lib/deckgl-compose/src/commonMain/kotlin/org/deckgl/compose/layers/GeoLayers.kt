package org.deckgl.compose.layers

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import org.deckgl.compose.BoundingBox
import org.deckgl.compose.DeckComposable
import org.deckgl.compose.PickInfo
import org.deckgl.compose.Units
import org.deckgl.compose.internal.EmitLayer
import org.deckgl.compose.internal.LayerData

/** Where a [GeoJsonLayer] gets its features from. */
public sealed interface GeoJsonData {
  /** A GeoJSON file (or a JSON array of features) at a URL, loaded by deck.gl. */
  public data class Url(val url: String) : GeoJsonData

  /** A GeoJSON object (`FeatureCollection`, `Feature` or geometry). */
  public data class Inline(val geoJson: JsonElement) : GeoJsonData {
    public constructor(geoJson: String) : this(Json.parseToJsonElement(geoJson))
  }
}

/**
 * GeoJSON features, deck.gl's
 * [GeoJsonLayer](https://deck.gl/docs/api-reference/layers/geojson-layer).
 *
 * Styling is uniform across features; [onClick] and [onHover] receive the index of the picked
 * feature in [PickInfo.index].
 */
@Composable
@DeckComposable
public fun GeoJsonLayer(
  id: String,
  data: GeoJsonData,
  fillColor: Color = Color.Black,
  lineColor: Color = Color.Black,
  lineWidth: Double = 1.0,
  pointRadius: Double = 1.0,
  elevation: Double = 1000.0,
  filled: Boolean = true,
  stroked: Boolean = true,
  extruded: Boolean = false,
  wireframe: Boolean = false,
  elevationScale: Double = 1.0,
  lineWidthUnits: Units = Units.Meters,
  lineWidthScale: Double = 1.0,
  lineWidthMinPixels: Double = 0.0,
  lineWidthMaxPixels: Double? = null,
  lineJointRounded: Boolean = false,
  lineCapRounded: Boolean = false,
  pointRadiusUnits: Units = Units.Meters,
  pointRadiusScale: Double = 1.0,
  pointRadiusMinPixels: Double = 0.0,
  pointRadiusMaxPixels: Double? = null,
  visible: Boolean = true,
  opacity: Float = 1f,
  pickable: Boolean = false,
  autoHighlight: Boolean = false,
  highlightColor: Color? = null,
  onClick: ((PickInfo) -> Unit)? = null,
  onHover: ((PickInfo) -> Unit)? = null,
): Unit = EmitLayer<Nothing>(
  type = "GeoJsonLayer",
  id = id,
  data = LayerData.Raw(
    when (data) {
      is GeoJsonData.Url -> JsonPrimitive(data.url)
      is GeoJsonData.Inline -> data.geoJson
    },
  ),
  props = mapOf(
    "getFillColor" to fillColor,
    "getLineColor" to lineColor,
    "getLineWidth" to lineWidth,
    "getPointRadius" to pointRadius,
    "getElevation" to elevation,
    "filled" to filled,
    "stroked" to stroked,
    "extruded" to extruded,
    "wireframe" to wireframe,
    "elevationScale" to elevationScale,
    "lineWidthUnits" to lineWidthUnits,
    "lineWidthScale" to lineWidthScale,
    "lineWidthMinPixels" to lineWidthMinPixels,
    "lineWidthMaxPixels" to lineWidthMaxPixels,
    "lineJointRounded" to lineJointRounded,
    "lineCapRounded" to lineCapRounded,
    "pointRadiusUnits" to pointRadiusUnits,
    "pointRadiusScale" to pointRadiusScale,
    "pointRadiusMinPixels" to pointRadiusMinPixels,
    "pointRadiusMaxPixels" to pointRadiusMaxPixels,
  ),
  accessors = emptyMap(),
  visible, opacity, pickable, autoHighlight, highlightColor,
  onClick?.let { handler -> { _, info -> handler(info) } },
  onHover?.let { handler -> { _, info -> handler(info) } },
)

/**
 * One image stretched over [bounds], deck.gl's
 * [BitmapLayer](https://deck.gl/docs/api-reference/layers/bitmap-layer).
 *
 * @param image URL of a PNG or JPEG image
 */
@Composable
@DeckComposable
public fun BitmapLayer(
  id: String,
  image: String,
  bounds: BoundingBox,
  desaturate: Double = 0.0,
  tintColor: Color = Color.White,
  visible: Boolean = true,
  opacity: Float = 1f,
): Unit = EmitLayer<Nothing>(
  type = "BitmapLayer",
  id = id,
  data = LayerData.Raw(JsonPrimitive(null)),
  props = mapOf(
    "image" to image,
    "bounds" to bounds,
    "desaturate" to desaturate,
    "tintColor" to tintColor.copy(alpha = 1f),
  ),
  accessors = emptyMap(),
  visible, opacity,
  pickable = false, autoHighlight = false, highlightColor = null, onClick = null, onHover = null,
)

/**
 * Raster map tiles, such as a basemap: deck.gl's
 * [TileLayer](https://deck.gl/docs/api-reference/geo-layers/tile-layer) drawing each tile with
 * a [BitmapLayer].
 *
 * Mind the tile server's usage policy; OpenStreetMap's tile servers for instance require an
 * identifying user agent and do not allow heavy use.
 *
 * @param data URL template with `{z}`, `{x}` and `{y}` placeholders
 */
@Composable
@DeckComposable
public fun RasterTileLayer(
  id: String,
  data: String,
  tileSize: Int = 256,
  minZoom: Int = 0,
  maxZoom: Int = 19,
  maxRequests: Int = 6,
  visible: Boolean = true,
  opacity: Float = 1f,
): Unit = EmitLayer<Nothing>(
  type = "TileLayer",
  id = id,
  data = LayerData.Raw(JsonPrimitive(data)),
  props = mapOf(
    "tileSize" to tileSize,
    "minZoom" to minZoom,
    "maxZoom" to maxZoom,
    "maxRequests" to maxRequests,
  ),
  accessors = emptyMap(),
  visible, opacity,
  pickable = false, autoHighlight = false, highlightColor = null, onClick = null, onHover = null,
)
