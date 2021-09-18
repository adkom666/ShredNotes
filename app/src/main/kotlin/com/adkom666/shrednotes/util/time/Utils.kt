package com.adkom666.shrednotes.util.time

/**
 * This function is guaranteed to return a nonnull value: a timestamp or the smallest possible
 * value.
 *
 * @return a timestamp or the smallest possible value.
 */
fun Days?.timestampOrMin(): Long {
    return this?.epochMillis ?: Long.MIN_VALUE
}

/**
 * This function is guaranteed to return a nonnull value: timestamp or maximum possible value.
 *
 * @return timestamp or maximum possible value.
 */
fun Days?.timestampOrMax(): Long {
    return this?.epochMillis ?: Long.MAX_VALUE
}
