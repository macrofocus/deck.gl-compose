package org.deckgl.compose

import androidx.compose.runtime.ComposableTargetMarker

/**
 * Marks composables that declare deck.gl content (layers) and may only be called inside the
 * `content` of [DeckGL], not inside regular UI.
 */
@Retention(AnnotationRetention.BINARY)
@ComposableTargetMarker(description = "deck.gl Composable")
@Target(
  AnnotationTarget.FILE,
  AnnotationTarget.FUNCTION,
  AnnotationTarget.PROPERTY_GETTER,
  AnnotationTarget.TYPE,
  AnnotationTarget.TYPE_PARAMETER,
)
public annotation class DeckComposable
