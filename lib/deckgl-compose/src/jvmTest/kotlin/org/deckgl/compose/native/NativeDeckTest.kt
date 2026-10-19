package org.deckgl.compose.native

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.deckgl.compose.MapViewState

class NativeDeckTest {
  private val scene = """
    {"layers": [{"@@type": "ScatterplotLayer", "id": "points", "pickable": true,
      "data": [{"c0": [8.54, 47.37]}], "getPosition": "@@=c0",
      "getRadius": 40, "radiusUnits": "pixels", "getFillColor": [255, 0, 0, 255]}]}
  """.trimIndent()

  @Test
  fun rendersAndPicks() {
    NativeDeck.create().use { deck ->
      assertNull(deck.setLayers(scene))
      val camera = NativeCamera(MapViewState(8.54, 47.37, 10.0), 200, 100, 2f)
      val image = deck.render(camera).getOrThrow()
      assertEquals(200, image.width)
      val pixels = IntArray(200 * 100)
      image.readPixels(pixels)
      val center = pixels[50 * 200 + 100]
      assertEquals(0xFFFF0000.toInt(), center, "center pixel is ${center.toUInt().toString(16)}")
      assertEquals(0, pixels[0], "background is transparent")

      val picked = deck.pick(100.0, 50.0)
      assertEquals("points", picked?.layerId)
      assertEquals(0, picked?.index)
      assertTrue(deck.pick(5.0, 5.0)?.layerId == null)
    }
  }
}
