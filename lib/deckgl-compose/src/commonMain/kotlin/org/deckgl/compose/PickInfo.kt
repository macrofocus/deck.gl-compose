package org.deckgl.compose

import androidx.compose.runtime.Immutable

/**
 * What lies under the pointer, as reported by deck.gl's picking.
 *
 * @property layerId id of the picked layer, null when nothing was picked
 * @property index index of the picked item in the layer's data, -1 when nothing was picked
 * @property coordinate geographic position under the pointer, when known
 * @property x pointer position in dp from the left of the deck
 * @property y pointer position in dp from the top of the deck
 */
@Immutable
public data class PickInfo(
  val layerId: String?,
  val index: Int,
  val coordinate: Position?,
  val x: Float,
  val y: Float,
) {
  val picked: Boolean get() = layerId != null && index >= 0
}

/** Click and hover handler of a layer: the picked [item] of the layer's data and the [info]. */
public typealias ItemHandler<T> = (item: T, info: PickInfo) -> Unit
