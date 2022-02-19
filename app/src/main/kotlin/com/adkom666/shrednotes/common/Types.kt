@file:Suppress("unused")

package com.adkom666.shrednotes.common

import kotlinx.coroutines.CoroutineScope

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

/**
 * The coroutine actions which will be invoked in the context of the provided scope.
 */
typealias CoroutineTask = suspend CoroutineScope.() -> Unit

/**
 * JSON as text.
 */
typealias Json = String
