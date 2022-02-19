package com.adkom666.shrednotes.data.pref

import android.content.SharedPreferences
import androidx.core.content.edit
import timber.log.Timber
import kotlin.properties.Delegates.observable

/**
 * Manage preferences for reading and writing shred notes.
 *
 * @property preferences project's [SharedPreferences] to store values.
 */
class InputOutputPreferences(
    private val preferences: SharedPreferences
) {
    private companion object {
        private const val KEY_GOOGLE_DRIVE_FILE_NAME = "google_drive.file_name"
    }

    /**
     * Property for storing a name of the last read or written file on Google Drive.
     */
    var googleDriveFileName: String? by observable(
        initialValue = preferences.getString(KEY_GOOGLE_DRIVE_FILE_NAME, null)
    ) { _, old, new ->
        Timber.d("Change googleDrivaFileName: old=$old, new=$new")
        if (new != old) {
            preferences.edit {
                putString(KEY_GOOGLE_DRIVE_FILE_NAME, new)
            }
        }
    }
}
