package org.deckgl.compose

import androidx.compose.runtime.Immutable

/** A geographic position in degrees, with an optional altitude in meters. */
@Immutable
public data class Position(
  val longitude: Double,
  val latitude: Double,
  val altitude: Double = 0.0,
)

/** A bounding box in degrees, as deck.gl's `[west, south, east, north]`. */
@Immutable
public data class BoundingBox(
  val west: Double,
  val south: Double,
  val east: Double,
  val north: Double,
)
