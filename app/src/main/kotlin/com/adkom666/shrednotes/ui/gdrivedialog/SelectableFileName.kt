package com.adkom666.shrednotes.ui.gdrivedialog

/**
 * File name with information about its selection.
 *
 * @property fileName file name.
 * @property isSelected information about its selection.
 */
@Suppress("DataClassShouldBeImmutable")
data class SelectableFileName(
    val fileName: String,
    var isSelected: Boolean = false
)
