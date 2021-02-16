package com.adkom666.shrednotes.util

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
