package org.deckgl.compose.internal

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import org.deckgl.compose.PickInfo

internal sealed class DeckNode

/** One deck.gl layer, as emitted by a layer composable. */
internal class LayerNode : DeckNode() {
  var spec: LayerSpec = LayerSpec.Empty
  var items: List<Any?> = emptyList()
  var onClick: ((Any?, PickInfo) -> Unit)? = null
  var onHover: ((Any?, PickInfo) -> Unit)? = null
}

/**
 * Root of the deck content: collects the layers in emission order and publishes them, once per
 * batch of changes, as [specs] (what deck.gl draws) and [layers] (for picking callbacks).
 */
internal class DeckRootNode : DeckNode() {
  val children = mutableListOf<LayerNode>()

  /** Layer descriptions in drawing order; only changes when a description does. */
  var specs: List<LayerSpec> by mutableStateOf(emptyList(), structuralEqualityPolicy())
    private set

  private var layers: List<LayerNode> = emptyList()

  fun commit() {
    layers = children.toList()
    specs = layers.map { it.spec }
  }

  /** The deck.gl JSON description of all layers, for `@deck.gl/json` and deck.gl-native. */
  fun description(specs: List<LayerSpec> = this.specs): JsonObject =
    JsonObject(mapOf("layers" to JsonArray(specs.map { it.json })))

  /** Calls the click handler of the picked layer. Returns whether there was one. */
  fun dispatchClick(info: PickInfo): Boolean = dispatch(info) { it.onClick }

  /** Calls the hover handler of the picked layer. Returns whether there was one. */
  fun dispatchHover(info: PickInfo): Boolean = dispatch(info) { it.onHover }

  val hasHoverHandlers: Boolean get() = layers.any { it.onHover != null }

  private inline fun dispatch(info: PickInfo, handler: (LayerNode) -> ((Any?, PickInfo) -> Unit)?): Boolean {
    val layerId = info.layerId ?: return false
    if (info.index < 0) return false
    // Composite layers may report the id of one of their sublayers ("<id>-points", ...).
    val node = layers.firstOrNull { it.spec.id == layerId }
      ?: layers.firstOrNull { layerId.startsWith(it.spec.id + "-") }
      ?: return false
    val callback = handler(node) ?: return false
    val item = node.items.getOrNull(info.index)
    callback(item, info)
    return true
  }
}

internal class DeckNodeApplier(private val rootNode: DeckRootNode) : AbstractApplier<DeckNode>(rootNode) {
  private val children get() = rootNode.children

  override fun insertTopDown(index: Int, instance: DeckNode) {
    check(current === rootNode) { "deck.gl layers cannot contain child layers" }
    children.add(index, instance as LayerNode)
  }

  override fun insertBottomUp(index: Int, instance: DeckNode) {}

  override fun remove(index: Int, count: Int) {
    children.subList(index, index + count).clear()
  }

  override fun move(from: Int, to: Int, count: Int) {
    val dest = if (from > to) to else to - count
    val moved = children.subList(from, from + count).toList()
    children.subList(from, from + count).clear()
    children.addAll(dest, moved)
  }

  override fun onClear() {
    children.clear()
  }

  override fun onEndChanges() {
    rootNode.commit()
  }
}
