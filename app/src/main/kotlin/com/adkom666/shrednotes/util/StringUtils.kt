package com.adkom666.shrednotes.util

import timber.log.Timber
import java.lang.NumberFormatException

/**
 * Case-insensitive trimmed string comparison.
 *
 * @param other other string to compare.
 * @return true if one of the strings is not equal to the other after trim and case-insensitive.
 */
infix fun String?.containsDifferentTrimmedTextIgnoreCaseThan(other: String?): Boolean {
    return if (this != null && !isBlank()) {
        trim().equals(
            other?.trim(),
            ignoreCase = true
        ).not()
    } else {
        other.isNullOrBlank().not()
    }
}

/**
 * Check whether the first string not contain the second one ignore case.
 *
 * @param other other string to compare.
 * @return true if the first string is not contain the second one ignore case.
 */
infix fun String?.notContainsIgnoreCase(other: String?): Boolean {
    return containsIgnoreCase(other).not()
}

/**
 * Parsing of the string as a signed decimal integer.
 *
 * @return the integer value represented by the argument in decimal or null if if the string does
 * not contain a parsable integer.
 */
fun String.numberOrNull(): Int? = try {
    Integer.parseInt(this)
} catch (e: NumberFormatException) {
    Timber.d(e)
    null
}

private fun String?.containsIgnoreCase(other: String?): Boolean {
    return other?.let { this?.contains(it, ignoreCase = true) } ?: false
}
