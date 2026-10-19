package org.deckgl.compose.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import org.deckgl.compose.Accessor
import org.deckgl.compose.DeckComposable
import org.deckgl.compose.ItemHandler

/** Emits one layer: the shared implementation of all layer composables. */
@Composable
@DeckComposable
internal fun <T> EmitLayer(
  type: String,
  id: String,
  data: LayerData<T>,
  props: Map<String, Any?>,
  accessors: Map<String, Accessor<T, *>?>,
  visible: Boolean,
  opacity: Float,
  pickable: Boolean,
  autoHighlight: Boolean,
  highlightColor: Color?,
  onClick: ItemHandler<T>?,
  onHover: ItemHandler<T>?,
) {
  val allProps = props + mapOf(
    "visible" to visible,
    "opacity" to opacity,
    "pickable" to (pickable || autoHighlight || onClick != null || onHover != null),
    "autoHighlight" to autoHighlight,
  ) + (if (highlightColor != null) mapOf("highlightColor" to highlightColor) else emptyMap())
  val inputs = LayerInputs(type, id, data, allProps, accessors)
  val spec = remember(inputs) { inputs.toSpec() }
  @Suppress("UNCHECKED_CAST")
  key(id) {
    ComposeNode<LayerNode, DeckNodeApplier>(
      factory = ::LayerNode,
      update = {
        set(spec) { this.spec = it }
        set(data.items) { this.items = it }
        set(onClick) { this.onClick = it as ((Any?, org.deckgl.compose.PickInfo) -> Unit)? }
        set(onHover) { this.onHover = it as ((Any?, org.deckgl.compose.PickInfo) -> Unit)? }
      },
    )
  }
}
