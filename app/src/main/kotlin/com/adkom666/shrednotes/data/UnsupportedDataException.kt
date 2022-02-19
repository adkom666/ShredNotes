package com.adkom666.shrednotes.data

import java.lang.Exception

/**
 * Suggested data version is not supported.
 *
 * @property version suggested version.
 */
class UnsupportedDataException(val version: Int) : Exception()
