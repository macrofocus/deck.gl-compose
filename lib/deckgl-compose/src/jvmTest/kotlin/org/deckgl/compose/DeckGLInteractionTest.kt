package org.deckgl.compose

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.deckgl.compose.layers.ScatterplotLayer

@OptIn(ExperimentalTestApi::class)
class DeckGLInteractionTest {
  private data class Point(val name: String, val position: Position)

  private val points = listOf(Point("zurich", Position(8.54, 47.37)))
  private val initial = MapViewState(longitude = 8.54, latitude = 47.37, zoom = 10.0)

  @Test
  fun clickPicksTheLayerItem() = runComposeUiTest {
    var clicked: Point? = null
    var missed = 0
    setContent {
      DeckGL(Modifier.size(300.dp, 200.dp).testTag("deck"), rememberDeckState(initial), onClick = { missed++ }) {
        ScatterplotLayer(
          id = "points",
          data = points,
          getPosition = accessor { it.position },
          getRadius = constant(30),
          radiusUnits = Units.Pixels,
          getFillColor = constant(Color.Red),
          onClick = { point, _ -> clicked = point },
        )
      }
    }
    // The deck is created and its first frame rendered on the native thread.
    Thread.sleep(1500)
    onNodeWithTag("deck").performMouseInput { click(center) }
    waitUntil(timeoutMillis = 5_000) { clicked != null }
    assertEquals("zurich", clicked?.name)

    onNodeWithTag("deck").performMouseInput { click(Offset(5f, 5f)) }
    waitUntil(timeoutMillis = 5_000) { missed == 1 }
  }

  @Test
  fun gesturesMoveTheCamera() = runComposeUiTest {
    lateinit var state: DeckState
    setContent {
      state = rememberDeckState(initial)
      DeckGL(Modifier.size(300.dp, 200.dp).testTag("deck"), state)
    }
    onNodeWithTag("deck").performMouseInput {
      moveTo(center)
      press()
      moveBy(Offset(60f, 0f))
      moveBy(Offset(60f, 0f))
      release()
    }
    waitForIdle()
    assertTrue(state.viewState.longitude < initial.longitude, "dragging right moves the center west: ${state.viewState}")
    assertEquals(initial.latitude, state.viewState.latitude, 1e-6)

    val before = state.viewState.zoom
    onNodeWithTag("deck").performMouseInput { scroll(-4f) }
    waitForIdle()
    assertTrue(state.viewState.zoom > before, "scrolling up zooms in: ${state.viewState}")
  }
}
