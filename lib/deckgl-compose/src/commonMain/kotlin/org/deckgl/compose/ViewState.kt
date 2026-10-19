package org.deckgl.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * Camera of a deck.gl `MapView`, in deck.gl's conventions.
 *
 * @property zoom zoom level, where 0 shows the whole world in 512 dp
 * @property pitch tilt in degrees, 0 looks straight down
 * @property bearing rotation in degrees, clockwise from north
 */
@Immutable
public data class MapViewState(
  val longitude: Double = 0.0,
  val latitude: Double = 0.0,
  val zoom: Double = 1.0,
  val pitch: Double = 0.0,
  val bearing: Double = 0.0,
) {
  val center: Position get() = Position(longitude, latitude)
}

/**
 * State of a [DeckGL]: its current [viewState], which the user's gestures update and which can
 * be set to move the camera.
 */
@Stable
public class DeckState(initialViewState: MapViewState) {
  public var viewState: MapViewState by mutableStateOf(initialViewState)
}

/** Remembers a [DeckState], saved across configuration changes. */
@Composable
public fun rememberDeckState(initialViewState: MapViewState = MapViewState()): DeckState =
  rememberSaveable(saver = DeckStateSaver) { DeckState(initialViewState) }

private val DeckStateSaver = listSaver<DeckState, Double>(
  save = {
    with(it.viewState) { listOf(longitude, latitude, zoom, pitch, bearing) }
  },
  restore = { DeckState(MapViewState(it[0], it[1], it[2], it[3], it[4])) },
)

/**
 * Which gestures move the camera, deck.gl's `controller` prop. [None] makes the deck static,
 * with the camera only moved by setting [DeckState.viewState].
 */
@Immutable
public data class ControllerOptions(
  val dragPan: Boolean = true,
  val dragRotate: Boolean = true,
  val scrollZoom: Boolean = true,
  val doubleClickZoom: Boolean = true,
  val keyboard: Boolean = true,
  val minZoom: Double = 0.0,
  val maxZoom: Double = 20.0,
  val minPitch: Double = 0.0,
  val maxPitch: Double = 60.0,
) {
  internal val enabled: Boolean get() = dragPan || dragRotate || scrollZoom || doubleClickZoom || keyboard

  internal fun constrain(viewState: MapViewState): MapViewState = viewState.copy(
    longitude = ((viewState.longitude + 540.0) % 360.0) - 180.0,
    latitude = viewState.latitude.coerceIn(-MAX_LATITUDE, MAX_LATITUDE),
    zoom = viewState.zoom.coerceIn(minZoom, maxZoom),
    pitch = viewState.pitch.coerceIn(minPitch, maxPitch),
  )

  public companion object {
    public val Default: ControllerOptions = ControllerOptions()
    public val None: ControllerOptions = ControllerOptions(
      dragPan = false,
      dragRotate = false,
      scrollZoom = false,
      doubleClickZoom = false,
      keyboard = false,
    )

    internal const val MAX_LATITUDE = 85.051129
  }
}
