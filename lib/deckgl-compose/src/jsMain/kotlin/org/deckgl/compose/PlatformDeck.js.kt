package org.deckgl.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.dom.domEventOrNull
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.viewinterop.HtmlElementView
import kotlinx.browser.document
import org.deckgl.compose.internal.DeckRootNode
import org.deckgl.compose.js.JsDeck
import org.w3c.dom.HTMLElement

/**
 * deck.gl in a DOM element that Compose positions over the deck's bounds.
 *
 * Compose for web places such elements above its canvas, which would hide any Compose UI drawn
 * over the deck. The element is moved below the canvas instead, the deck's bounds are cleared
 * to transparent in the canvas (so Compose content drawn before the deck is hidden by it, and
 * content drawn after it shows on top of it, as on other platforms), and the pointer events Compose receives over the deck are passed on
 * to deck.gl, so that its controller, picking and highlighting work as usual.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal actual fun PlatformDeck(
  modifier: Modifier,
  state: DeckState,
  controller: ControllerOptions,
  root: DeckRootNode,
  onClick: ((PickInfo) -> Unit)?,
  onHover: ((PickInfo) -> Unit)?,
) {
  val currentOnClick by rememberUpdatedState(onClick)
  val currentOnHover by rememberUpdatedState(onHover)
  var deck by remember { mutableStateOf<JsDeck?>(null) }
  var hoverPickable by remember { mutableStateOf(false) }

  Box(
    modifier
      // Punch a hole in the canvas through which the deck below shows.
      .drawBehind { drawRect(Color.Transparent, blendMode = BlendMode.Clear) }
      .pointerHoverIcon(if (hoverPickable) PointerIcon.Hand else PointerIcon.Default)
      .pointerInput(Unit) {
        awaitPointerEventScope {
          while (true) {
            val event = awaitPointerEvent()
            val domEvent = event.domEventOrNull ?: continue
            deck?.dispatchEvent(domEvent)
          }
        }
      },
  ) {
    HtmlElementView(
      factory = {
        (document.createElement("div") as HTMLElement).apply {
          style.width = "100%"
          style.height = "100%"
        }
      },
      modifier = Modifier.fillMaxSize(),
      update = { element ->
        // The element's group, absolutely positioned by Compose, goes below the canvas.
        (element.parentElement as? HTMLElement)?.style?.zIndex = "-1"
        if (deck == null) {
          deck = JsDeck(
            parent = element,
            initialViewState = state.viewState,
            controller = controller,
            onViewStateChange = { state.viewState = it },
            onClick = { info -> if (!root.dispatchClick(info)) currentOnClick?.invoke(info) },
            onHover = { info ->
              hoverPickable = info.picked
              root.dispatchHover(info)
              currentOnHover?.invoke(info)
            },
          )
        }
      },
    )
  }

  LaunchedEffect(deck) {
    val jsDeck = deck ?: return@LaunchedEffect
    snapshotFlow { root.specs }.collect { jsDeck.setLayers(it) }
  }
  LaunchedEffect(deck) {
    val jsDeck = deck ?: return@LaunchedEffect
    snapshotFlow { state.viewState }.collect { jsDeck.setViewState(it) }
  }
  LaunchedEffect(deck, controller) {
    deck?.setController(controller)
  }
  DisposableEffect(deck) {
    val created = deck
    onDispose { created?.finalize() }
  }
}
