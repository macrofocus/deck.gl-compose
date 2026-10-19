package org.deckgl.compose.layers

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.deckgl.compose.Accessor
import org.deckgl.compose.DeckComposable
import org.deckgl.compose.ItemHandler
import org.deckgl.compose.Position
import org.deckgl.compose.Units
import org.deckgl.compose.constant
import org.deckgl.compose.internal.EmitLayer
import org.deckgl.compose.internal.LayerData

/**
 * Circles at positions, deck.gl's
 * [ScatterplotLayer](https://deck.gl/docs/api-reference/layers/scatterplot-layer).
 */
@Composable
@DeckComposable
public fun <T> ScatterplotLayer(
  id: String,
  data: List<T>,
  getPosition: Accessor<T, Position>,
  getRadius: Accessor<T, Number> = constant(1),
  getFillColor: Accessor<T, Color> = constant(Color.Black),
  getLineColor: Accessor<T, Color> = constant(Color.Black),
  getLineWidth: Accessor<T, Number> = constant(1),
  radiusUnits: Units = Units.Meters,
  radiusScale: Double = 1.0,
  radiusMinPixels: Double = 0.0,
  radiusMaxPixels: Double? = null,
  lineWidthUnits: Units = Units.Meters,
  lineWidthScale: Double = 1.0,
  lineWidthMinPixels: Double = 0.0,
  lineWidthMaxPixels: Double? = null,
  stroked: Boolean = false,
  filled: Boolean = true,
  billboard: Boolean = false,
  antialiasing: Boolean = true,
  visible: Boolean = true,
  opacity: Float = 1f,
  pickable: Boolean = false,
  autoHighlight: Boolean = false,
  highlightColor: Color? = null,
  onClick: ItemHandler<T>? = null,
  onHover: ItemHandler<T>? = null,
): Unit = EmitLayer(
  type = "ScatterplotLayer",
  id = id,
  data = LayerData.Items(data),
  props = mapOf(
    "radiusUnits" to radiusUnits,
    "radiusScale" to radiusScale,
    "radiusMinPixels" to radiusMinPixels,
    "radiusMaxPixels" to radiusMaxPixels,
    "lineWidthUnits" to lineWidthUnits,
    "lineWidthScale" to lineWidthScale,
    "lineWidthMinPixels" to lineWidthMinPixels,
    "lineWidthMaxPixels" to lineWidthMaxPixels,
    "stroked" to stroked,
    "filled" to filled,
    "billboard" to billboard,
    "antialiasing" to antialiasing,
  ),
  accessors = mapOf(
    "getPosition" to getPosition,
    "getRadius" to getRadius,
    "getFillColor" to getFillColor,
    "getLineColor" to getLineColor,
    "getLineWidth" to getLineWidth,
  ),
  visible, opacity, pickable, autoHighlight, highlightColor, onClick, onHover,
)

/** Straight lines between pairs of positions, deck.gl's [LineLayer](https://deck.gl/docs/api-reference/layers/line-layer). */
@Composable
@DeckComposable
public fun <T> LineLayer(
  id: String,
  data: List<T>,
  getSourcePosition: Accessor<T, Position>,
  getTargetPosition: Accessor<T, Position>,
  getColor: Accessor<T, Color> = constant(Color.Black),
  getWidth: Accessor<T, Number> = constant(1),
  widthUnits: Units = Units.Pixels,
  widthScale: Double = 1.0,
  widthMinPixels: Double = 0.0,
  widthMaxPixels: Double? = null,
  visible: Boolean = true,
  opacity: Float = 1f,
  pickable: Boolean = false,
  autoHighlight: Boolean = false,
  highlightColor: Color? = null,
  onClick: ItemHandler<T>? = null,
  onHover: ItemHandler<T>? = null,
): Unit = EmitLayer(
  type = "LineLayer",
  id = id,
  data = LayerData.Items(data),
  props = mapOf(
    "widthUnits" to widthUnits,
    "widthScale" to widthScale,
    "widthMinPixels" to widthMinPixels,
    "widthMaxPixels" to widthMaxPixels,
  ),
  accessors = mapOf(
    "getSourcePosition" to getSourcePosition,
    "getTargetPosition" to getTargetPosition,
    "getColor" to getColor,
    "getWidth" to getWidth,
  ),
  visible, opacity, pickable, autoHighlight, highlightColor, onClick, onHover,
)

/** Raised arcs between pairs of positions, deck.gl's [ArcLayer](https://deck.gl/docs/api-reference/layers/arc-layer). */
@Composable
@DeckComposable
public fun <T> ArcLayer(
  id: String,
  data: List<T>,
  getSourcePosition: Accessor<T, Position>,
  getTargetPosition: Accessor<T, Position>,
  getSourceColor: Accessor<T, Color> = constant(Color.Black),
  getTargetColor: Accessor<T, Color> = constant(Color.Black),
  getWidth: Accessor<T, Number> = constant(1),
  getHeight: Accessor<T, Number> = constant(1),
  getTilt: Accessor<T, Number> = constant(0),
  greatCircle: Boolean = false,
  numSegments: Int = 50,
  widthUnits: Units = Units.Pixels,
  widthScale: Double = 1.0,
  widthMinPixels: Double = 0.0,
  widthMaxPixels: Double? = null,
  visible: Boolean = true,
  opacity: Float = 1f,
  pickable: Boolean = false,
  autoHighlight: Boolean = false,
  highlightColor: Color? = null,
  onClick: ItemHandler<T>? = null,
  onHover: ItemHandler<T>? = null,
): Unit = EmitLayer(
  type = "ArcLayer",
  id = id,
  data = LayerData.Items(data),
  props = mapOf(
    "greatCircle" to greatCircle,
    "numSegments" to numSegments,
    "widthUnits" to widthUnits,
    "widthScale" to widthScale,
    "widthMinPixels" to widthMinPixels,
    "widthMaxPixels" to widthMaxPixels,
  ),
  accessors = mapOf(
    "getSourcePosition" to getSourcePosition,
    "getTargetPosition" to getTargetPosition,
    "getSourceColor" to getSourceColor,
    "getTargetColor" to getTargetColor,
    "getWidth" to getWidth,
    "getHeight" to getHeight,
    "getTilt" to getTilt,
  ),
  visible, opacity, pickable, autoHighlight, highlightColor, onClick, onHover,
)

/** Polylines, deck.gl's [PathLayer](https://deck.gl/docs/api-reference/layers/path-layer). */
@Composable
@DeckComposable
public fun <T> PathLayer(
  id: String,
  data: List<T>,
  getPath: Accessor<T, List<Position>>,
  getColor: Accessor<T, Color> = constant(Color.Black),
  getWidth: Accessor<T, Number> = constant(1),
  widthUnits: Units = Units.Meters,
  widthScale: Double = 1.0,
  widthMinPixels: Double = 0.0,
  widthMaxPixels: Double? = null,
  jointRounded: Boolean = false,
  capRounded: Boolean = false,
  miterLimit: Double = 4.0,
  billboard: Boolean = false,
  visible: Boolean = true,
  opacity: Float = 1f,
  pickable: Boolean = false,
  autoHighlight: Boolean = false,
  highlightColor: Color? = null,
  onClick: ItemHandler<T>? = null,
  onHover: ItemHandler<T>? = null,
): Unit = EmitLayer(
  type = "PathLayer",
  id = id,
  data = LayerData.Items(data),
  props = mapOf(
    "widthUnits" to widthUnits,
    "widthScale" to widthScale,
    "widthMinPixels" to widthMinPixels,
    "widthMaxPixels" to widthMaxPixels,
    "jointRounded" to jointRounded,
    "capRounded" to capRounded,
    "miterLimit" to miterLimit,
    "billboard" to billboard,
  ),
  accessors = mapOf(
    "getPath" to getPath,
    "getColor" to getColor,
    "getWidth" to getWidth,
  ),
  visible, opacity, pickable, autoHighlight, highlightColor, onClick, onHover,
)

/**
 * Filled and/or stroked polygons, optionally extruded, deck.gl's
 * [PolygonLayer](https://deck.gl/docs/api-reference/layers/polygon-layer).
 *
 * A polygon is a list of rings: the outer ring, then its holes.
 */
@Composable
@DeckComposable
public fun <T> PolygonLayer(
  id: String,
  data: List<T>,
  getPolygon: Accessor<T, List<List<Position>>>,
  getFillColor: Accessor<T, Color> = constant(Color.Black),
  getLineColor: Accessor<T, Color> = constant(Color.Black),
  getLineWidth: Accessor<T, Number> = constant(1),
  getElevation: Accessor<T, Number> = constant(1000),
  stroked: Boolean = true,
  filled: Boolean = true,
  extruded: Boolean = false,
  wireframe: Boolean = false,
  elevationScale: Double = 1.0,
  lineWidthUnits: Units = Units.Meters,
  lineWidthScale: Double = 1.0,
  lineWidthMinPixels: Double = 0.0,
  lineWidthMaxPixels: Double? = null,
  lineJointRounded: Boolean = false,
  lineMiterLimit: Double = 4.0,
  visible: Boolean = true,
  opacity: Float = 1f,
  pickable: Boolean = false,
  autoHighlight: Boolean = false,
  highlightColor: Color? = null,
  onClick: ItemHandler<T>? = null,
  onHover: ItemHandler<T>? = null,
): Unit = EmitLayer(
  type = "PolygonLayer",
  id = id,
  data = LayerData.Items(data),
  props = mapOf(
    "stroked" to stroked,
    "filled" to filled,
    "extruded" to extruded,
    "wireframe" to wireframe,
    "elevationScale" to elevationScale,
    "lineWidthUnits" to lineWidthUnits,
    "lineWidthScale" to lineWidthScale,
    "lineWidthMinPixels" to lineWidthMinPixels,
    "lineWidthMaxPixels" to lineWidthMaxPixels,
    "lineJointRounded" to lineJointRounded,
    "lineMiterLimit" to lineMiterLimit,
  ),
  accessors = mapOf(
    "getPolygon" to getPolygon,
    "getFillColor" to getFillColor,
    "getLineColor" to getLineColor,
    "getLineWidth" to getLineWidth,
    "getElevation" to getElevation,
  ),
  visible, opacity, pickable, autoHighlight, highlightColor, onClick, onHover,
)

/** Extruded cylinders, deck.gl's [ColumnLayer](https://deck.gl/docs/api-reference/layers/column-layer). */
@Composable
@DeckComposable
public fun <T> ColumnLayer(
  id: String,
  data: List<T>,
  getPosition: Accessor<T, Position>,
  getElevation: Accessor<T, Number> = constant(1000),
  getFillColor: Accessor<T, Color> = constant(Color.Black),
  getLineColor: Accessor<T, Color> = constant(Color.Black),
  getLineWidth: Accessor<T, Number> = constant(1),
  radius: Double = 1000.0,
  diskResolution: Int = 20,
  angle: Double = 0.0,
  coverage: Double = 1.0,
  elevationScale: Double = 1.0,
  extruded: Boolean = true,
  filled: Boolean = true,
  stroked: Boolean = false,
  wireframe: Boolean = false,
  radiusUnits: Units = Units.Meters,
  visible: Boolean = true,
  opacity: Float = 1f,
  pickable: Boolean = false,
  autoHighlight: Boolean = false,
  highlightColor: Color? = null,
  onClick: ItemHandler<T>? = null,
  onHover: ItemHandler<T>? = null,
): Unit = EmitLayer(
  type = "ColumnLayer",
  id = id,
  data = LayerData.Items(data),
  props = mapOf(
    "radius" to radius,
    "diskResolution" to diskResolution,
    "angle" to angle,
    "coverage" to coverage,
    "elevationScale" to elevationScale,
    "extruded" to extruded,
    "filled" to filled,
    "stroked" to stroked,
    "wireframe" to wireframe,
    "radiusUnits" to radiusUnits,
  ),
  accessors = mapOf(
    "getPosition" to getPosition,
    "getElevation" to getElevation,
    "getFillColor" to getFillColor,
    "getLineColor" to getLineColor,
    "getLineWidth" to getLineWidth,
  ),
  visible, opacity, pickable, autoHighlight, highlightColor, onClick, onHover,
)

/** Text labels, deck.gl's [TextLayer](https://deck.gl/docs/api-reference/layers/text-layer). */
@Composable
@DeckComposable
public fun <T> TextLayer(
  id: String,
  data: List<T>,
  getPosition: Accessor<T, Position>,
  getText: Accessor<T, String>,
  getColor: Accessor<T, Color> = constant(Color.Black),
  getSize: Accessor<T, Number> = constant(32),
  getAngle: Accessor<T, Number> = constant(0),
  getTextAnchor: Accessor<T, TextAnchor> = constant(TextAnchor.Middle),
  getAlignmentBaseline: Accessor<T, AlignmentBaseline> = constant(AlignmentBaseline.Center),
  getPixelOffset: Accessor<T, List<Number>> = constant(listOf(0, 0)),
  sizeUnits: Units = Units.Pixels,
  sizeScale: Double = 1.0,
  sizeMinPixels: Double = 0.0,
  sizeMaxPixels: Double? = null,
  billboard: Boolean = true,
  background: Boolean = false,
  getBackgroundColor: Accessor<T, Color> = constant(Color.White),
  outlineWidth: Double = 0.0,
  outlineColor: Color = Color.Black,
  characterSet: String? = null,
  visible: Boolean = true,
  opacity: Float = 1f,
  pickable: Boolean = false,
  autoHighlight: Boolean = false,
  highlightColor: Color? = null,
  onClick: ItemHandler<T>? = null,
  onHover: ItemHandler<T>? = null,
): Unit = EmitLayer(
  type = "TextLayer",
  id = id,
  data = LayerData.Items(data),
  props = mapOf(
    "sizeUnits" to sizeUnits,
    "sizeScale" to sizeScale,
    "sizeMinPixels" to sizeMinPixels,
    "sizeMaxPixels" to sizeMaxPixels,
    "billboard" to billboard,
    "background" to background,
    "outlineWidth" to outlineWidth,
    "outlineColor" to outlineColor,
    "characterSet" to characterSet,
    // Outlines are drawn from signed distance fields
    "fontSettings" to if (outlineWidth > 0.0) mapOf("sdf" to true) else null,
  ),
  accessors = mapOf(
    "getPosition" to getPosition,
    "getText" to getText,
    "getColor" to getColor,
    "getSize" to getSize,
    "getAngle" to getAngle,
    "getTextAnchor" to getTextAnchor,
    "getAlignmentBaseline" to getAlignmentBaseline,
    "getPixelOffset" to getPixelOffset,
    "getBackgroundColor" to getBackgroundColor,
  ),
  visible, opacity, pickable, autoHighlight, highlightColor, onClick, onHover,
)

public enum class TextAnchor(private val value: String) {
  Start("start"),
  Middle("middle"),
  End("end"),
  ;

  override fun toString(): String = value
}

public enum class AlignmentBaseline(private val value: String) {
  Top("top"),
  Center("center"),
  Bottom("bottom"),
  ;

  override fun toString(): String = value
}
