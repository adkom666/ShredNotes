package com.adkom666.shrednotes.data

import com.adkom666.shrednotes.BuildConfig
import com.adkom666.shrednotes.data.external.ExternalShredNotesEnvelope
import com.adkom666.shrednotes.data.external.ExternalShredNotesV1
import com.adkom666.shrednotes.data.google.Google
import com.adkom666.shrednotes.data.google.GoogleAuth
import com.adkom666.shrednotes.data.google.GoogleAuthException
import com.adkom666.shrednotes.data.google.GoogleDriveFile
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
    /**
     * Use it to authorization in Google before [readFileFromGoogleDrive] or
     * [writeJsonToGoogleDrive].
     */
    val googleAuth: GoogleAuth = google

    /**
     * Reading a list of information about JSON files stored on Google Drive.
     *
     * @return list of of information about JSON files stored on Google Drive.
     * @throws GoogleAuthException when user is signed out of the Google account.
     * @throws GoogleRecoverableAuthException when the user does not have enough rights to perform
     * an operation with Google Drive. This exception contains [android.content.Intent] to allow
     * user interaction to recover his rights.
     */
    @Throws(
        GoogleAuthException::class,
        GoogleRecoverableAuthException::class
    )
    suspend fun listGoogleDriveJsonFiles(): List<GoogleDriveFile> = withContext(Dispatchers.IO) {
        Timber.d("Read information about JSON files stored on GoogleDrive")
        val jsonFiles = google.drive.listJsonFiles()
        Timber.d("jsonFiles=$jsonFiles")
        jsonFiles
    }

    /**
     * Reading all content from Google Drive file with identifier [fileId].
     *
     * @param fileId identifier of the target Google Drive file.
     * @param fileIdKey key for storing information to be read from Google Drive as text in the
     * map [GoogleRecoverableAuthException.additionalData] if [GoogleRecoverableAuthException] is
     * thrown. This text should be used as [fileId] after the rights are recovered.
     * @throws GoogleAuthException when the user is signed out of the Google account.
     * @throws GoogleRecoverableAuthException when the user does not have enough rights to perform
     * an operation with Google Drive. This exception contains [android.content.Intent] to allow
     * user interaction to recover his rights. This exception also contains [fileId] in its
     * [GoogleRecoverableAuthException.additionalData] map with the key [fileIdKey].
     * @throws JsonSyntaxException when the syntax of the JSON from Google Drive is incorrect.
     * @throws UnsupportedDataException when the suggested shred notes version is not supported.
     */
    @Throws(
        GoogleAuthException::class,
        GoogleRecoverableAuthException::class,
        JsonSyntaxException::class,
        UnsupportedDataException::class
    )
    suspend fun readFileFromGoogleDrive(
        fileId: String,
        fileIdKey: String
    ) = withContext(Dispatchers.IO) {
        Timber.d("Read: fileId=$fileId")
        val json = google.drive.readFile(fileId = fileId, fileIdKey = fileIdKey)
        Timber.d("json=$json")
        parseAsShredNotes(json)
    }

    /**
     * Writing all content to Google Drive as JSON.
     *
     * @param file information about target JSON file on Google Drive.
     * @param fileKey key for storing information to be written on Google Drive as [GoogleDriveFile]
     * in the map [GoogleRecoverableAuthException.additionalData] if
     * [GoogleRecoverableAuthException] is thrown. This text should be used as [file] after the
     * rights are recovered.
     * @param readyJson information to be written on Google Drive as JSON.
     * @param jsonKey key for storing information to be written on Google Drive as JSON in the map
     * [GoogleRecoverableAuthException.additionalData] if [GoogleRecoverableAuthException] is
     * thrown. This JSON should be used as [readyJson] after the rights are recovered.
     * @throws GoogleAuthException when user is signed out of the Google account.
     * @throws GoogleRecoverableAuthException when the user does not have enough rights to perform
     * an operation with Google Drive. This exception contains [android.content.Intent] to allow
     * user interaction to recover his rights. This exception also contains [file] in its
     * [GoogleRecoverableAuthException.additionalData] map with the key [fileKey] and all
     * information to be written on Google Drive as JSON with the key [jsonKey].
     */
    @Throws(
        GoogleAuthException::class,
        GoogleRecoverableAuthException::class
    )
    suspend fun writeJsonToGoogleDrive(
        file: GoogleDriveFile,
        fileKey: String,
        readyJson: String? = null,
        jsonKey: String
    ) = withContext(Dispatchers.IO) {
        Timber.d("Write: file=$file, readyJson=$readyJson")
        val shredNotesJson = readyJson ?: run {
            val version = BuildConfig.SHRED_NOTES_VERSION
            prepareShredNotesJson(version)
        }
        Timber.d("shredNotesJson=$shredNotesJson")
        google.drive.writeJson(
            file = file,
            fileKey = fileKey,
            json = shredNotesJson,
            jsonKey = jsonKey
        )
    }

    /**
     * Deleting files from Google Drive with identifiers listed in [fileIdList].
     *
     * @param fileIdList list of target file identifiers.
     * @param fileIdListKey key of the list of target file identifiers to save it in the
     * [GoogleRecoverableAuthException.additionalData] map for the case if
     * [GoogleRecoverableAuthException] has been thrown.
     * @throws GoogleAuthException when user is signed out of the Google account.
     * @throws GoogleRecoverableAuthException when the user does not have enough rights to perform
     * an operation with Google Drive. This exception contains [android.content.Intent] to allow
     * user interaction to recover his rights. This exception also contains the original
     * [fileIdList] in its [GoogleRecoverableAuthException.additionalData] map with the key
     * [fileIdListKey].
     */
    @Throws(
        GoogleAuthException::class,
        GoogleRecoverableAuthException::class
    )
    suspend fun deleteFilesFromGoogleDrive(
        fileIdList: List<String>,
        fileIdListKey: String
    ) = withContext(Dispatchers.IO) {
        Timber.d("Delete: fileIdList=$fileIdList")
        google.drive.deleteFiles(fileIdList = fileIdList, fileIdListKey = fileIdListKey)
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
