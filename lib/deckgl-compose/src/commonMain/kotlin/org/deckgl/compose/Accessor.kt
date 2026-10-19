package org.deckgl.compose

/**
 * A deck.gl accessor: either the same [constant] value for every item of a layer's data, or a
 * value computed per item with [accessor].
 *
 * ```kotlin
 * ScatterplotLayer(
 *   id = "cities",
 *   data = cities,
 *   getPosition = accessor { it.position },
 *   getRadius = accessor { it.population / 1000.0 },
 *   getFillColor = constant(Color.Red),
 * )
 * ```
 *
 * Per item accessors are evaluated in Kotlin whenever the data or the accessor changes, and
 * the results are handed to deck.gl as attributes. Accessors compare equal when they wrap the
 * same lambda instance, which the Compose compiler keeps stable across recompositions as long
 * as the lambda only captures stable values.
 */
public sealed interface Accessor<in T, out V>

internal data class ConstantAccessor<out V>(val value: V) : Accessor<Any?, V>

internal data class FunctionAccessor<in T, out V>(val function: (T) -> V) : Accessor<T, V>

/** An accessor returning [value] for every item. */
public fun <V> constant(value: V): Accessor<Any?, V> = ConstantAccessor(value)

/** An accessor computing a value for every item. */
public fun <T, V> accessor(function: (T) -> V): Accessor<T, V> = FunctionAccessor(function)
