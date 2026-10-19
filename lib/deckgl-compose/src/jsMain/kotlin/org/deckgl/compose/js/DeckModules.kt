package org.deckgl.compose.js

// deck.gl's npm modules, used dynamically: only a handful of members are needed, and layers
// are instantiated by @deck.gl/json from their JSON descriptions.

@JsModule("@deck.gl/core")
@JsNonModule
internal external object DeckCoreModule

@JsModule("@deck.gl/layers")
@JsNonModule
internal external object DeckLayersModule

@JsModule("@deck.gl/geo-layers")
@JsNonModule
internal external object DeckGeoLayersModule

@JsModule("@deck.gl/aggregation-layers")
@JsNonModule
internal external object DeckAggregationLayersModule

@JsModule("@deck.gl/json")
@JsNonModule
internal external object DeckJsonModule

internal val jsNew: (dynamic, dynamic) -> dynamic = js("(function (C, props) { return new C(props); })")
internal val jsAssign: dynamic = js("Object.assign")

internal fun jsObject(): dynamic = js("({})")

internal inline fun jsObject(init: (dynamic) -> Unit): dynamic {
  val o = jsObject()
  init(o)
  return o
}
