@file:Suppress("unused")

package com.adkom666.shrednotes.common

/**
 * Identifier of the collection item.
 */
typealias Id = Long

const val NO_ID = 0L

/**
 * Ensure type compatibility with Long.
 */
fun Long.toId() = this

/**
 * Ensure type compatibility with Int.
 */
fun Int.toId() = this.toLong()
