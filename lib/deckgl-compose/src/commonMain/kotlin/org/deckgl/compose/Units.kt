package org.deckgl.compose

/** Units of a size prop, deck.gl's `radiusUnits`, `widthUnits`, `sizeUnits`, ... */
public enum class Units(internal val value: String) {
  /** Meters on the ground; scales with zoom. */
  Meters("meters"),

  /** World units, 512 per world width at zoom 0. */
  Common("common"),

  /** Screen pixels; constant across zoom levels. */
  Pixels("pixels"),
}

/** How values are combined by the aggregation layers. */
public enum class Aggregation(internal val value: String) {
  Sum("SUM"),
  Mean("MEAN"),
  Min("MIN"),
  Max("MAX"),
  Count("COUNT"),
}
