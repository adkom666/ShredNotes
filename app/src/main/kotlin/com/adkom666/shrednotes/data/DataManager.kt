package com.adkom666.shrednotes.data

import com.adkom666.shrednotes.BuildConfig
import com.adkom666.shrednotes.data.external.ExternalShredNotesEnvelope
import com.adkom666.shrednotes.data.external.ExternalShredNotesV1
import com.adkom666.shrednotes.data.google.Google
import com.adkom666.shrednotes.data.google.GoogleAuth
import com.adkom666.shrednotes.data.google.GoogleAuthException
import com.adkom666.shrednotes.data.google.GoogleRecoverableAuthException
import com.adkom666.shrednotes.data.repository.ShredNotesRepository
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Responsible for saving all application data.
 *
 * @property repository all application data storage.
 * @property google here we interacting with Google.
 * @property gson JSON converter.
 */
class DataManager(
    private val repository: ShredNotesRepository,
    private val google: Google,
    private val gson: Gson
) {
    private companion object {
        private const val GOOGLE_DRIVE_FILE_NAME = "shrednotes.json"
    }

    /**
     * Use it to authorization in Google before [read] or [write].
     */
    val googleAuth: GoogleAuth = google

    /**
     * Reading all content from Google Drive.
     *
     * @return true if the content was read, false if there is no information on Google Drive.
     * @throws GoogleAuthException when the user is signed out of the Google account.
     * @throws GoogleRecoverableAuthException when the user does not have enough rights to perform
     * an operation with Google Drive. This exception contains [android.content.Intent] to allow
     * user interaction to recover his rights.
     * @throws JsonSyntaxException when the syntax of the JSON from Google Drive is incorrect.
     * @throws UnsupportedDataException when the suggested shred notes version is not
     * supported.
     */
    @Throws(
        GoogleAuthException::class,
        GoogleRecoverableAuthException::class,
        JsonSyntaxException::class,
        UnsupportedDataException::class
    )
    suspend fun read(): Boolean = withContext(Dispatchers.IO) {
        Timber.d("Read")
        val json = google.readJson(fileName = GOOGLE_DRIVE_FILE_NAME)
        Timber.d("json=$json")
        json?.let {
            parseAsShredNotes(it)
            true
        } ?: false
    }

    /**
     * Writing all content to Google Drive.
     *
     * @param jsonKey key for storing information to be written on Google Drive as JSON in the map
     * [GoogleRecoverableAuthException.additionalData] if [GoogleRecoverableAuthException] is
     * thrown. This JSON should be used as [readyJson] after the rights are recovered.
     * @param readyJson information to be written on Google Drive as JSON.
     * @throws GoogleAuthException when user is signed out of the Google account.
     * @throws GoogleRecoverableAuthException when the user does not have enough rights to perform
     * an operation with Google Drive. This exception contains [android.content.Intent] to allow
     * user interaction to recover his rights. This exception also contains all information to be
     * written on Google Drive as JSON in its [GoogleRecoverableAuthException.additionalData] map
     * with the key [jsonKey]. This JSON should be used as [readyJson] after the rights are
     * recovered.
     */
    @Throws(
        GoogleAuthException::class,
        GoogleRecoverableAuthException::class
    )
    suspend fun write(jsonKey: String, readyJson: String? = null) = withContext(Dispatchers.IO) {
        Timber.d("Write: readyJson=$readyJson")
        val shredNotesJson = readyJson ?: run {
            val version = BuildConfig.SHRED_NOTES_VERSION
            prepareShredNotesJson(version)
        }
        Timber.d("shredNotesJson=$shredNotesJson")
        google.writeJson(
            fileName = GOOGLE_DRIVE_FILE_NAME,
            json = shredNotesJson,
            jsonKey = jsonKey
        )
    }

    @Throws(
        JsonSyntaxException::class,
        UnsupportedDataException::class
    )
    private suspend fun parseAsShredNotes(json: String) {
        val shredNotesEnvelope = gson.fromJson(
            json,
            ExternalShredNotesEnvelope::class.java
        )
        when (shredNotesEnvelope.version) {
            1 -> {
                val shredNotes = gson.fromJson(
                    shredNotesEnvelope.content,
                    ExternalShredNotesV1::class.java
                )
                Timber.d("shredNotes=$shredNotes")
                repository.replaceShredNotesByV1Suspending(shredNotes)
            }
            else -> throw UnsupportedDataException(shredNotesEnvelope.version)
        }
    }

    private suspend fun prepareShredNotesJson(version: Int): String {
        val shredNotes = when (version) {
            1 -> repository.shredNotesV1Suspending()
            else -> error("There is no implementation for the shred notes version $version")
        }
        Timber.d("shredNotes=$shredNotes")
        val shredNotesEnvelope = ExternalShredNotesEnvelope(
            version = version,
            content = gson.toJson(shredNotes)
        )
        Timber.d("shredNotesEnvelope=$shredNotesEnvelope")
        return gson.toJson(shredNotesEnvelope)
    }
}
