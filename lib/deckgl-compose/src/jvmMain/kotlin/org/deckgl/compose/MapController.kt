package org.deckgl.compose

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tan

/**
 * Camera moves for the desktop, where deck.gl-native has no controller of its own. Offsets are
 * in dp relative to the deck's top left corner. Like deck.gl's MapController, positions are
 * moved in Web Mercator world coordinates (512 dp per world at zoom 0); pitch is not taken into
 * account when converting screen offsets.
 */
internal object MapController {
  private const val TILE_SIZE = 512.0

  fun pan(viewState: MapViewState, delta: Offset): MapViewState {
    val world = toWorld(viewState.longitude, viewState.latitude, viewState.zoom)
    val moved = rotate(delta, viewState.bearing)
    return fromWorld(world.first - moved.x, world.second - moved.y, viewState)
  }

  /** Zooms by [deltaZoom], keeping the point under [anchor] in place. */
  fun zoom(viewState: MapViewState, deltaZoom: Double, anchor: Offset, size: Offset, controller: ControllerOptions): MapViewState {
    val zoom = (viewState.zoom + deltaZoom).coerceIn(controller.minZoom, controller.maxZoom)
    val scale = 2.0.pow(zoom - viewState.zoom)
    val (cx, cy) = toWorld(viewState.longitude, viewState.latitude, viewState.zoom)
    val offset = rotate(anchor - size / 2f, viewState.bearing)
    val anchorX = (cx + offset.x) * scale
    val anchorY = (cy + offset.y) * scale
    return fromWorld(anchorX - offset.x, anchorY - offset.y, viewState.copy(zoom = zoom))
  }

  fun rotate(viewState: MapViewState, delta: Offset, controller: ControllerOptions): MapViewState =
    viewState.copy(
      bearing = viewState.bearing + delta.x * 0.4,
      pitch = (viewState.pitch - delta.y * 0.4).coerceIn(controller.minPitch, controller.maxPitch),
    )

  /** Rotates a screen offset into world axes for the map's bearing. */
  private fun rotate(offset: Offset, bearing: Double): Offset {
    val angle = bearing * PI / 180.0
    val c = cos(angle)
    val s = sin(angle)
    return Offset((offset.x * c - offset.y * s).toFloat(), (offset.x * s + offset.y * c).toFloat())
  }

  private fun toWorld(longitude: Double, latitude: Double, zoom: Double): Pair<Double, Double> {
    val size = TILE_SIZE * 2.0.pow(zoom)
    val x = (longitude + 180.0) / 360.0 * size
    val y = (PI - ln(tan(PI / 4 + latitude * PI / 360.0))) / (2 * PI) * size
    return x to y
  }

  private fun fromWorld(x: Double, y: Double, viewState: MapViewState): MapViewState {
    val size = TILE_SIZE * 2.0.pow(viewState.zoom)
    val longitude = x / size * 360.0 - 180.0
    val latitude = (2 * atan(exp(PI - y / size * 2 * PI)) - PI / 2) * 180.0 / PI
    return viewState.copy(longitude = longitude, latitude = latitude)
  }
}
