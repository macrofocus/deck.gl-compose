package org.deckgl.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import java.awt.Cursor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.deckgl.compose.internal.DeckRootNode
import org.deckgl.compose.internal.LayerSpec
import org.deckgl.compose.native.NativeCamera
import org.deckgl.compose.native.NativeDeck
import org.deckgl.compose.native.NativePick

@Composable
internal actual fun PlatformDeck(
  modifier: Modifier,
  state: DeckState,
  controller: ControllerOptions,
  root: DeckRootNode,
  onClick: ((PickInfo) -> Unit)?,
  onHover: ((PickInfo) -> Unit)?,
) {
  val density = LocalDensity.current.density
  val currentController by rememberUpdatedState(controller)
  val currentOnClick by rememberUpdatedState(onClick)
  val currentOnHover by rememberUpdatedState(onHover)

  var deck by remember { mutableStateOf<NativeDeck?>(null) }
  var error by remember { mutableStateOf<String?>(null) }
  var image by remember { mutableStateOf<ImageBitmap?>(null) }
  var size by remember { mutableStateOf(IntSize.Zero) }
  var hoverPickable by remember { mutableStateOf(false) }

  // The deck lives on the native thread, created once and destroyed with the composable.
  LaunchedEffect(Unit) {
    try {
      deck = withContext(NativeDeck.dispatcher) { NativeDeck.create() }
    } catch (e: Throwable) {
      if (e is CancellationException) throw e
      val cause = generateSequence(e) { it.cause }.last()
      System.err.println("deck.gl-native could not start")
      e.printStackTrace()
      error = "deck.gl-native could not start: $cause"
    }
  }
  DisposableEffect(deck) {
    val created = deck
    onDispose {
      if (created != null) CoroutineScope(NativeDeck.dispatcher).launch { created.close() }
    }
  }

  val frames = remember { Channel<Unit>(Channel.CONFLATED) }

  // Render whenever the layers, camera or size change, and while data is loading.
  LaunchedEffect(deck, density) {
    val nativeDeck = deck ?: return@LaunchedEffect
    launch {
      snapshotFlow { Triple(root.specs, state.viewState, size) }.collect { frames.trySend(Unit) }
    }
    var appliedSpecs: List<LayerSpec>? = null
    for (request in frames) {
      val specs = root.specs
      val camera = NativeCamera(state.viewState, size.width, size.height, density)
      if (camera.width <= 0 || camera.height <= 0) continue
      val (frame, loading) = withContext(NativeDeck.dispatcher) {
        if (specs != appliedSpecs) {
          nativeDeck.setLayers(root.description(specs).toString())?.let { System.err.println("deck.gl-native: $it") }
          appliedSpecs = specs
        }
        nativeDeck.render(camera) to nativeDeck.isLoading()
      }
      frame.onSuccess { image = it; error = null }.onFailure { error = it.message }
      if (loading) launch {
        delay(LOADING_FRAME_INTERVAL_MS)
        frames.trySend(Unit)
      }
    }
  }

  // Hover: picking waits for the GPU, so only the latest pointer position is processed.
  val hovers = remember { Channel<Offset?>(Channel.CONFLATED) }
  LaunchedEffect(deck, density) {
    val nativeDeck = deck ?: return@LaunchedEffect
    for (position in hovers) {
      val picked = withContext(NativeDeck.dispatcher) {
        if (position == null) {
          nativeDeck.pointerLeave()
          null
        } else {
          nativeDeck.pointerMove(position.x.toDouble(), position.y.toDouble())
          nativeDeck.pick(position.x.toDouble(), position.y.toDouble())
        }
      }
      frames.trySend(Unit) // autoHighlight
      val info = picked?.toPickInfo(position!!, density)
      hoverPickable = info?.picked == true
      if (info != null) {
        root.dispatchHover(info)
        currentOnHover?.invoke(info)
      }
    }
  }

  suspend fun click(position: Offset) {
    val nativeDeck = deck ?: return
    val picked = withContext(NativeDeck.dispatcher) {
      nativeDeck.pick(position.x.toDouble(), position.y.toDouble())
    } ?: return
    val info = picked.toPickInfo(position, density)
    if (!root.dispatchClick(info)) currentOnClick?.invoke(info)
  }

  val touchSlop = LocalViewConfiguration.current.touchSlop
  val gestureScope = rememberCoroutineScope()
  Box(
    modifier
      .onSizeChanged { size = it }
      .pointerHoverIcon(if (hoverPickable) PointerIcon(Cursor(Cursor.HAND_CURSOR)) else PointerIcon.Default)
      .pointerInput(deck, density) {
        var lastClickTime = 0L
        var lastClickPosition = Offset.Zero
        awaitPointerEventScope {
          var pressPosition: Offset? = null
          var lastPosition = Offset.Zero
          var rotating = false
          var dragging = false
          while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull() ?: continue
            val position = change.position // physical pixels
            val dp = position / density
            val controls = currentController
            val sizeDp = Offset(size.width / density, size.height / density)
            when (event.type) {
              PointerEventType.Press -> {
                pressPosition = position
                lastPosition = position
                dragging = false
                rotating = event.buttons.isSecondaryPressed ||
                  event.keyboardModifiers.isCtrlPressed ||
                  event.keyboardModifiers.isMetaPressed ||
                  event.keyboardModifiers.isShiftPressed
              }
              PointerEventType.Move -> {
                val press = pressPosition
                if (press != null && (event.buttons.isPrimaryPressed || event.buttons.isSecondaryPressed)) {
                  if (!dragging && (position - press).getDistance() > touchSlop) dragging = true
                  if (dragging) {
                    val delta = (position - lastPosition) / density
                    state.viewState = when {
                      rotating && controls.dragRotate -> MapController.rotate(state.viewState, delta, controls)
                      !rotating && controls.dragPan -> controls.constrain(MapController.pan(state.viewState, delta))
                      else -> state.viewState
                    }
                    change.consume()
                  }
                  lastPosition = position
                } else {
                  hovers.trySend(position)
                }
              }
              PointerEventType.Release -> {
                if (pressPosition != null && !dragging) {
                  val now = change.uptimeMillis
                  val doubleClick = now - lastClickTime < DOUBLE_CLICK_MS &&
                    (position - lastClickPosition).getDistance() < touchSlop
                  if (doubleClick && controls.doubleClickZoom) {
                    val delta = if (event.keyboardModifiers.isShiftPressed) -1.0 else 1.0
                    state.viewState = controls.constrain(MapController.zoom(state.viewState, delta, dp, sizeDp, controls))
                    lastClickTime = 0L
                  } else {
                    lastClickTime = now
                    lastClickPosition = position
                    gestureScope.launch { click(position) }
                  }
                }
                pressPosition = null
                dragging = false
              }
              PointerEventType.Scroll -> {
                if (controls.scrollZoom) {
                  val delta = -change.scrollDelta.y.toDouble() * SCROLL_ZOOM_SPEED
                  state.viewState = controls.constrain(MapController.zoom(state.viewState, delta, dp, sizeDp, controls))
                  change.consume()
                }
              }
              PointerEventType.Exit -> hovers.trySend(null)
            }
          }
        }
      },
  ) {
    Canvas(Modifier.fillMaxSize()) {
      image?.let { drawImage(it) }
    }
    error?.let {
      BasicText(it, Modifier.padding(16.dp), style = androidx.compose.ui.text.TextStyle(color = Color.Red))
    }
  }
}

private fun NativePick.toPickInfo(position: Offset, density: Float) = PickInfo(
  layerId = layerId,
  index = index,
  coordinate = Position(longitude, latitude),
  x = position.x / density,
  y = position.y / density,
)

private const val LOADING_FRAME_INTERVAL_MS = 50L
private const val DOUBLE_CLICK_MS = 300L
private const val SCROLL_ZOOM_SPEED = 0.25
