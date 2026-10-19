package org.deckgl.compose.layers

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.deckgl.compose.Accessor
import org.deckgl.compose.DeckComposable
import org.deckgl.compose.ItemHandler
import org.deckgl.compose.PickInfo
import org.deckgl.compose.internal.EmitLayer
import org.deckgl.compose.internal.LayerData

/**
 * Any deck.gl layer type, for those without a dedicated composable.
 *
 * [props] are encoded like the typed layers' parameters ([Color], [org.deckgl.compose.Position],
 * lists, numbers, strings, booleans, maps, or raw [kotlinx.serialization.json.JsonElement]s), as
 * are [accessors]. The layer type must exist in both deck.gl and deck.gl-native to work on all
 * platforms.
 *
 * ```kotlin
 * Layer(
 *   type = "GridCellLayer",
 *   id = "cells",
 *   data = cells,
 *   props = mapOf("cellSize" to 200, "extruded" to true),
 *   accessors = mapOf("getPosition" to accessor { it.position }, "getElevation" to accessor { it.value }),
 * )
 * ```
 */
@Composable
@DeckComposable
public fun <T> Layer(
  type: String,
  id: String,
  data: List<T>,
  props: Map<String, Any?> = emptyMap(),
  accessors: Map<String, Accessor<T, *>> = emptyMap(),
  visible: Boolean = true,
  opacity: Float = 1f,
  pickable: Boolean = false,
  autoHighlight: Boolean = false,
  highlightColor: Color? = null,
  onClick: ItemHandler<T>? = null,
  onHover: ItemHandler<T>? = null,
): Unit = EmitLayer(
  type, id, LayerData.Items(data), props, accessors,
  visible, opacity, pickable, autoHighlight, highlightColor, onClick, onHover,
)

/**
 * A layer written in deck.gl's JSON format (`@deck.gl/json`, pydeck), with its `@@type`, `id`,
 * `data` and props, accessors as `"@@=expression"`:
 *
 * ```kotlin
 * JsonLayer("""
 *   {"@@type": "ScatterplotLayer", "id": "stops", "data": "https://example.com/stops.json",
 *    "getPosition": "@@=coordinates", "getRadius": "@@=passengers / 10", "getFillColor": [255, 140, 0]}
 * """)
 * ```
 */
@Composable
@DeckComposable
public fun JsonLayer(
  json: String,
  onClick: ((PickInfo) -> Unit)? = null,
  onHover: ((PickInfo) -> Unit)? = null,
): Unit = JsonLayer(Json.parseToJsonElement(json).jsonObject, onClick, onHover)

/** A layer in deck.gl's JSON format, see the overload taking a string. */
@Composable
@DeckComposable
public fun JsonLayer(
  json: JsonObject,
  onClick: ((PickInfo) -> Unit)? = null,
  onHover: ((PickInfo) -> Unit)? = null,
) {
  val type = requireNotNull(json["@@type"]?.jsonPrimitive?.content) { "A JSON layer needs a \"@@type\"" }
  val id = requireNotNull(json["id"]?.jsonPrimitive?.content) { "A JSON layer needs an \"id\"" }
  val pickable = json["pickable"]?.jsonPrimitive?.content == "true"
  EmitLayer<Nothing>(
    type = type,
    id = id,
    data = LayerData.Raw(json["data"] ?: JsonPrimitive(null)),
    props = json - setOf("@@type", "id", "data", "pickable"),
    accessors = emptyMap(),
    visible = json["visible"]?.jsonPrimitive?.content != "false",
    opacity = json["opacity"]?.jsonPrimitive?.content?.toFloatOrNull() ?: 1f,
    pickable = pickable,
    autoHighlight = json["autoHighlight"]?.jsonPrimitive?.content == "true",
    highlightColor = null,
    onClick = onClick?.let { handler -> { _, info -> handler(info) } },
    onHover = onHover?.let { handler -> { _, info -> handler(info) } },
  )
}
