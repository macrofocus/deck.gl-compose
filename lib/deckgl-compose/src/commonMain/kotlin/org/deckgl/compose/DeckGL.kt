package org.deckgl.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import org.deckgl.compose.internal.DeckNodeApplier
import org.deckgl.compose.internal.DeckRootNode

/**
 * Displays deck.gl layers in a `MapView`.
 *
 * Layers are declared as composables in [content], drawn in declaration order (the first one at
 * the bottom), and updated whenever their parameters change:
 *
 * ```kotlin
 * val state = rememberDeckState(MapViewState(longitude = 8.54, latitude = 47.37, zoom = 11.0))
 * DeckGL(Modifier.fillMaxSize(), state) {
 *   RasterTileLayer(id = "basemap", data = "https://tile.openstreetmap.org/{z}/{x}/{y}.png")
 *   ScatterplotLayer(
 *     id = "stations",
 *     data = stations,
 *     getPosition = accessor { it.position },
 *     getFillColor = constant(Color.Red),
 *     radiusMinPixels = 4.0,
 *     onClick = { station, _ -> selected = station },
 *   )
 * }
 * ```
 *
 * In the browser this is deck.gl itself; on the desktop it is deck.gl-native, rendered
 * offscreen with WebGPU (Metal, Vulkan or DirectX 12) and drawn by Compose.
 *
 * The deck has a transparent background: place it over a basemap of your own, or start the
 * content with a [RasterTileLayer].
 *
 * @param state the camera, updated by the user's gestures
 * @param controller which gestures move the camera
 * @param onClick called on a click that no layer's own `onClick` handled
 * @param onHover called as the pointer moves over the deck, with what lies under it
 */
@Composable
public fun DeckGL(
  modifier: Modifier = Modifier,
  state: DeckState = rememberDeckState(),
  controller: ControllerOptions = ControllerOptions.Default,
  onClick: ((PickInfo) -> Unit)? = null,
  onHover: ((PickInfo) -> Unit)? = null,
  content: @Composable @DeckComposable () -> Unit = {},
) {
  val root = remember { DeckRootNode() }
  val compositionContext = rememberCompositionContext()
  val currentContent by rememberUpdatedState(content)
  DisposableEffect(root, compositionContext) {
    val composition = Composition(DeckNodeApplier(root), compositionContext)
    composition.setContent { currentContent() }
    onDispose { composition.dispose() }
  }
  PlatformDeck(modifier, state, controller, root, onClick, onHover)
}

/** The platform's deck.gl, drawing the layers of [root] with the camera of [state]. */
@Composable
internal expect fun PlatformDeck(
  modifier: Modifier,
  state: DeckState,
  controller: ControllerOptions,
  root: DeckRootNode,
  onClick: ((PickInfo) -> Unit)?,
  onHover: ((PickInfo) -> Unit)?,
)
