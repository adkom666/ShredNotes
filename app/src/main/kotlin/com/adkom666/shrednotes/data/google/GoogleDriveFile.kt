package com.adkom666.shrednotes.data.google

/**
 * Information about file on Google Drive.
 *
 * @property id file identifier, may be null if it not already assigned by Google.
 * @property name file name.
 */
data class GoogleDriveFile(
    val id: String?,
    val name: String
)
