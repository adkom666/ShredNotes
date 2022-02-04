package com.adkom666.shrednotes.data

import android.content.SharedPreferences
import androidx.core.content.edit
import com.adkom666.shrednotes.BuildConfig
import com.adkom666.shrednotes.data.external.ExternalShredNotesEnvelope
import com.adkom666.shrednotes.data.external.ExternalShredNotesV1
import com.adkom666.shrednotes.data.google.Google
import com.adkom666.shrednotes.data.google.GoogleAuth
import com.adkom666.shrednotes.data.google.GoogleAuthException
import com.adkom666.shrednotes.data.google.GoogleDriveFile
import com.adkom666.shrednotes.data.google.GoogleRecoverableAuthException
import com.adkom666.shrednotes.data.repository.ShredNotesRepository
import com.adkom666.shrednotes.util.put
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Responsible for saving all application data.
 *
 * @property repository all application data storage.
 * @property preferences project's [SharedPreferences] to keep in file with content.
 * @property google here we interacting with Google.
 * @property gson JSON converter.
 */
class DataManager(
    private val repository: ShredNotesRepository,
    private val preferences: SharedPreferences,
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
        Timber.d("parseAsShredNotes: json=$json")
        val shredNotesEnvelope = gson.fromJson(
            json,
            ExternalShredNotesEnvelope::class.java
        )
        Timber.d("shredNotesEnvelope=$shredNotesEnvelope")
        when (shredNotesEnvelope.version) {
            1 -> setupShredNotesV1(shredNotesEnvelope.content, shredNotesEnvelope.preferences)
            else -> throw UnsupportedDataException(shredNotesEnvelope.version)
        }
    }

    private suspend fun prepareShredNotesJson(version: Int): String {
        Timber.d("prepareShredNotesJson: version=$version")
        return when (version) {
            1 -> prepareShredNotesJsonV1()
            else -> error("There is no implementation for the shred notes version $version")
        }
    }

    private suspend fun setupShredNotesV1(contentJson: String, preferencesJson: String?) {
        Timber.d(
            """setupShredNotesV1:
                |contentJson=$contentJson,
                |preferencesJson=$preferencesJson""".trimMargin()
        )

        val shredNotes = gson.fromJson(
            contentJson,
            ExternalShredNotesV1::class.java
        )
        Timber.d("shredNotes=$shredNotes")
        repository.replaceShredNotesByV1Suspending(shredNotes)

        preferencesJson?.let { json ->
            preferences.edit {
                clear()
                putFromJson(json)
            }
        } ?: run {
            preferences.edit {
                clear()
            }
        }
    }

    private suspend fun prepareShredNotesJsonV1(): String {
        Timber.d("prepareShredNotesJsonV1")

        val shredNotes = repository.shredNotesV1Suspending()
        Timber.d("shredNotes=$shredNotes")
        val contentJson = gson.toJson(shredNotes)
        Timber.d("contentJson=$contentJson")

        val preferencesMap = preferences.all
        Timber.d("preferencesMap=$preferencesMap")
        val preferencesMapBackup = preferencesMap.backup()
        Timber.d("preferencesMapBackup=$preferencesMapBackup")
        val preferencesJson = gson.toJson(preferencesMapBackup)
        Timber.d("preferencesJson=$preferencesJson")

        val shredNotesEnvelope = ExternalShredNotesEnvelope(
            version = 1,
            content = contentJson,
            preferences = preferencesJson
        )
        Timber.d("shredNotesEnvelope=$shredNotesEnvelope")
        val shredNotesJson = gson.toJson(shredNotesEnvelope)
        Timber.d("shredNotesJson=$shredNotesJson")

        return shredNotesJson
    }

    private fun SharedPreferences.Editor.putFromJson(
        preferencesJson: String
    ): SharedPreferences.Editor {
        Timber.d("putFromJson: preferencesJson=$preferencesJson")
        val preferencesMapBackup = gson.fromJson(
            preferencesJson,
            MapBackup::class.java
        )
        Timber.d("preferencesMapBackup=$preferencesMapBackup")
        val preferencesMap = preferencesMapBackup.map()
        Timber.d("preferencesMap=$preferencesMap")
        put(preferencesMap)
        return this
    }

    private fun Map<String, *>.backup(): MapBackup {
        val backup = MapBackup()
        entries.forEach { entry ->
            when (val entryValue = entry.value) {
                is Boolean -> backup.booleans[entry.key] = entryValue
                is Int -> backup.integers[entry.key] = entryValue
                is Long -> backup.longs[entry.key] = entryValue
                is String -> backup.strings[entry.key] = entryValue
                is Float -> backup.floats[entry.key] = entryValue
            }
        }
        return backup
    }

    private fun MapBackup.map(): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        booleans.entries.forEach { map[it.key] = it.value }
        integers.entries.forEach { map[it.key] = it.value }
        longs.entries.forEach { map[it.key] = it.value }
        strings.entries.forEach { map[it.key] = it.value }
        floats.entries.forEach { map[it.key] = it.value }
        return map
    }

    private data class MapBackup(
        val booleans: MutableMap<String, Boolean> = mutableMapOf(),
        val integers: MutableMap<String, Int> = mutableMapOf(),
        val longs: MutableMap<String, Long> = mutableMapOf(),
        val strings: MutableMap<String, String> = mutableMapOf(),
        val floats: MutableMap<String, Float> = mutableMapOf()
    )
}
