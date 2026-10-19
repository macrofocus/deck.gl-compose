package org.deckgl.compose.js

import kotlinx.serialization.json.Json
import org.deckgl.compose.ControllerOptions
import org.deckgl.compose.MapViewState
import org.deckgl.compose.PickInfo
import org.deckgl.compose.Position
import org.deckgl.compose.internal.LayerSpec
import org.w3c.dom.HTMLElement

/** A deck.gl `Deck` in a DOM element, with a controlled view state. */
internal class JsDeck(
  parent: HTMLElement,
  initialViewState: MapViewState,
  controller: ControllerOptions,
  private val onViewStateChange: (MapViewState) -> Unit,
  private val onClick: (PickInfo) -> Unit,
  private val onHover: (PickInfo) -> Unit,
) {
  private var viewState = initialViewState
  private var controller = controller
  private var layerCache = mapOf<LayerSpec, dynamic>()

  private val deck: dynamic = run {
    val props = jsObject()
    props.parent = parent
    props.width = "100%"
    props.height = "100%"
    props.viewState = viewState.toJs(controller)
    props.controller = controller.toJs()
    props.layers = arrayOf<dynamic>()
    props.onViewStateChange = { params: dynamic ->
      val v = params.viewState
      val next = MapViewState(
        longitude = v.longitude as Double,
        latitude = v.latitude as Double,
        zoom = v.zoom as Double,
        pitch = (v.pitch as Double?) ?: 0.0,
        bearing = (v.bearing as Double?) ?: 0.0,
      )
      setViewState(next)
      onViewStateChange(next)
      null
    }
    props.onClick = { info: dynamic, _: dynamic -> onClick(toPickInfo(info)) }
    props.onHover = { info: dynamic, _: dynamic -> onHover(toPickInfo(info)) }
    jsNew(DeckCoreModule.asDynamic().Deck, props)
  }

  fun setViewState(viewState: MapViewState) {
    if (viewState == this.viewState) return
    this.viewState = viewState
    deck.setProps(jsObject { it.viewState = viewState.toJs(controller) })
  }

  fun setController(controller: ControllerOptions) {
    if (controller == this.controller) return
    this.controller = controller
    deck.setProps(
      jsObject {
        it.controller = controller.toJs()
        it.viewState = viewState.toJs(controller)
      },
    )
  }

  /** Converts the layers that changed and hands all of them to deck.gl, in order. */
  fun setLayers(specs: List<LayerSpec>) {
    val cache = specs.associateWith { spec -> layerCache[spec] ?: convertLayer(spec) }
    layerCache = cache
    deck.setProps(jsObject { it.layers = specs.map { spec -> cache[spec] }.toTypedArray() })
  }

  /** Hands a DOM event that Compose received over the deck to deck.gl's own event handling. */
  fun dispatchEvent(event: dynamic) {
    val canvas = deck.getCanvas() ?: return
    canvas.dispatchEvent(cloneEvent(event))
    // Browsers follow pointer events with compatibility mouse events, which Compose does not
    // pass on; deck.gl's hover handling listens to those.
    val type = event.type as String
    if (type.startsWith("pointer") && event.pointerType == "mouse") {
      canvas.dispatchEvent(cloneMouseEvent("mouse" + type.removePrefix("pointer"), event))
    }
  }

  fun finalize() {
    deck.finalize()
    layerCache = emptyMap()
  }

  private fun convertLayer(spec: LayerSpec): dynamic {
    val json = JSON.parse<dynamic>(Json.encodeToString(spec.json))
    val wrapper = jsObject()
    wrapper.layers = arrayOf(json)
    return converter.convert(wrapper).layers[0]
  }

  companion object {
    private val converter: dynamic by lazy {
      val classes = jsAssign(jsObject(), DeckLayersModule, DeckGeoLayersModule, DeckAggregationLayersModule)
      val enumerations = jsObject()
      enumerations.COORDINATE_SYSTEM = DeckCoreModule.asDynamic().COORDINATE_SYSTEM
      val configuration = jsObject()
      configuration.classes = classes
      configuration.enumerations = enumerations
      configuration.preProcessClassProps = { layerClass: dynamic, props: dynamic ->
        // Like deck.gl-native, image tiles are drawn as bitmaps when no renderSubLayers is given.
        if (layerClass == DeckGeoLayersModule.asDynamic().TileLayer && props.renderSubLayers == null) {
          props.renderSubLayers = ::renderBitmapTile
        }
        props
      }
      val converterProps = jsObject()
      converterProps.configuration = configuration
      jsNew(DeckJsonModule.asDynamic().JSONConverter, converterProps)
    }

    private fun renderBitmapTile(props: dynamic): dynamic {
      val box = props.tile.boundingBox
      val bitmapProps = jsAssign(jsObject(), props)
      bitmapProps.data = null
      bitmapProps.image = props.data
      bitmapProps.bounds = arrayOf(box[0][0], box[0][1], box[1][0], box[1][1])
      return jsNew(DeckLayersModule.asDynamic().BitmapLayer, bitmapProps)
    }
  }
}

private val cloneEvent: (dynamic) -> dynamic = js("(function (e) { return new e.constructor(e.type, e); })")
private val cloneMouseEvent: (String, dynamic) -> dynamic = js("(function (type, e) { return new MouseEvent(type, e); })")

private fun MapViewState.toJs(controller: ControllerOptions): dynamic = jsObject {
  it.longitude = longitude
  it.latitude = latitude
  it.zoom = zoom
  it.pitch = pitch
  it.bearing = bearing
  it.minZoom = controller.minZoom
  it.maxZoom = controller.maxZoom
  it.minPitch = controller.minPitch
  it.maxPitch = controller.maxPitch
}

private fun ControllerOptions.toJs(): dynamic = jsObject {
  it.dragPan = dragPan
  it.dragRotate = dragRotate
  it.scrollZoom = scrollZoom
  it.doubleClickZoom = doubleClickZoom
  it.touchZoom = scrollZoom
  it.touchRotate = dragRotate
  it.keyboard = keyboard
}

private fun toPickInfo(info: dynamic): PickInfo {
  val coordinate = info.coordinate
  return PickInfo(
    layerId = info.layer?.id as String?,
    index = info.index as Int? ?: -1,
    coordinate = if (coordinate != null) Position(coordinate[0] as Double, coordinate[1] as Double) else null,
    x = (info.x as Number).toFloat(),
    y = (info.y as Number).toFloat(),
  )
}
