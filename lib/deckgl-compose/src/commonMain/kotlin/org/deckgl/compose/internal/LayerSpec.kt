package org.deckgl.compose.internal

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.deckgl.compose.Accessor
import org.deckgl.compose.Aggregation
import org.deckgl.compose.BoundingBox
import org.deckgl.compose.ConstantAccessor
import org.deckgl.compose.FunctionAccessor
import org.deckgl.compose.Position
import org.deckgl.compose.Units

/**
 * A layer in the `@deck.gl/json` format: `{"@@type": "ScatterplotLayer", "id": ..., "data":
 * [...], "getPosition": "@@=c0", ...}`, which deck.gl JS (through `JSONConverter`) and
 * deck.gl-native both read.
 */
@Immutable
internal data class LayerSpec(val id: String, val type: String, val json: JsonObject) {
  companion object {
    val Empty = LayerSpec("", "", JsonObject(emptyMap()))
  }
}

/**
 * Everything a layer composable passes in. Equal inputs build equal specs, so the spec is only
 * rebuilt (and the per item accessors only evaluated) when an input changed.
 */
internal data class LayerInputs<T>(
  val type: String,
  val id: String,
  val data: LayerData<T>,
  val props: Map<String, Any?>,
  val accessors: Map<String, Accessor<T, *>?>,
)

internal sealed interface LayerData<out T> {
  val items: List<T>

  /** Items whose per item accessors are evaluated in Kotlin. */
  data class Items<T>(override val items: List<T>) : LayerData<T>

  /** Data handed to deck.gl as is: a URL, or inline JSON such as a GeoJSON object. */
  data class Raw(val value: JsonElement) : LayerData<Nothing> {
    override val items: List<Nothing> get() = emptyList()
  }
}

internal fun <T> LayerInputs<T>.toSpec(): LayerSpec {
  val json = LinkedHashMap<String, JsonElement>()
  json["@@type"] = JsonPrimitive(type)
  json["id"] = JsonPrimitive(id)
  for ((name, value) in props) {
    if (value != null) json[name] = encodeValue(value)
  }
  // Per item accessors become columns of the rows ("c0", "c1", ...) read by an expression.
  val columns = ArrayList<Pair<String, (T) -> Any?>>()
  for ((name, accessor) in accessors) {
    when (accessor) {
      null -> {}
      is ConstantAccessor<*> -> json[name] = encodeValue(accessor.value)
      is FunctionAccessor<T, *> -> {
        val column = "c${columns.size}"
        columns += column to accessor.function
        json[name] = JsonPrimitive("@@=$column")
      }
    }
  }
  json["data"] = when (data) {
    is LayerData.Raw -> data.value.takeIf { it != JsonNull } ?: return LayerSpec(id, type, JsonObject(json))
    is LayerData.Items -> JsonArray(
      data.items.map { item ->
        JsonObject(columns.associate { (column, function) -> column to encodeValue(function(item)) })
      },
    )
  }
  return LayerSpec(id, type, JsonObject(json))
}

/** Encodes a prop or accessor value the way deck.gl expects it in JSON. */
internal fun encodeValue(value: Any?): JsonElement = when (value) {
  null -> JsonNull
  is JsonElement -> value
  is String -> JsonPrimitive(value)
  is Boolean -> JsonPrimitive(value)
  is Float -> JsonPrimitive(value.toDouble())
  is Number -> JsonPrimitive(value)
  is Color -> JsonArray(
    listOf(value.red, value.green, value.blue, value.alpha).map { JsonPrimitive((it * 255f).roundToInt()) },
  )
  is Position -> JsonArray(
    if (value.altitude == 0.0) {
      listOf(JsonPrimitive(value.longitude), JsonPrimitive(value.latitude))
    } else {
      listOf(JsonPrimitive(value.longitude), JsonPrimitive(value.latitude), JsonPrimitive(value.altitude))
    },
  )
  is BoundingBox -> JsonArray(listOf(value.west, value.south, value.east, value.north).map(::JsonPrimitive))
  is Units -> JsonPrimitive(value.value)
  is Aggregation -> JsonPrimitive(value.value)
  is Enum<*> -> JsonPrimitive(value.toString())
  is Iterable<*> -> JsonArray(value.map(::encodeValue))
  is Array<*> -> JsonArray(value.map(::encodeValue))
  is DoubleArray -> JsonArray(value.map(::JsonPrimitive))
  is FloatArray -> JsonArray(value.map { JsonPrimitive(it.toDouble()) })
  is IntArray -> JsonArray(value.map(::JsonPrimitive))
  is Map<*, *> -> JsonObject(value.entries.associate { (k, v) -> k.toString() to encodeValue(v) })
  else -> error("Cannot encode ${value::class.simpleName} for deck.gl: $value")
}
