package com.adkom666.shrednotes.data.google

import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.http.ByteArrayContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Collections

/**
 * Operations with Google Drive.
 *
 * @property drive service definition for Google Drive (v3). See [Drive].
 */
class GoogleDriveHelper(private val drive: Drive) {

    private companion object {
        private const val FOLDER_APPDATA = "appDataFolder"
        private const val SPACE_APPDATA = "appDataFolder"
        private const val MIME_TYPE_JSON = "application/json"
    }

    /**
     * Getting a list of JSON file names with the parent named [parentName].
     *
     * @param parentName name of one of the file's parents.
     * @return list of JSON file names with the parent named [parentName].
     * @throws UserRecoverableAuthIOException when the user does not have enough rights to perform
     * an operation with Google Drive. This exception contains [android.content.Intent] to allow
     * user interaction to recover his rights.
     */
    @Throws(UserRecoverableAuthIOException::class)
    fun listJsonFileName(parentName: String = FOLDER_APPDATA): List<String> {
        Timber.d("Get root JSON file name list")
        val fileNameList = mutableListOf<String>()
        var nextPageToken: String? = null
        do {
            val result = drive.files().list().apply {
                q = "mimeType='$MIME_TYPE_JSON' and '$parentName' in parents"
                fields = "nextPageToken, files(id, name, parents)"
                spaces = SPACE_APPDATA
                pageToken = nextPageToken
            }.execute()
            Timber.d("result=$result")
            val pageFileNameList = result.files?.map { it.name }
            pageFileNameList?.let { fileNameList.addAll(it) }
            nextPageToken = result.nextPageToken
            Timber.d("nextPageToken=$nextPageToken")
        } while (nextPageToken != null)
        Timber.d("fileNameList=$fileNameList")
        return fileNameList
    }

    /**
     * Getting a list of identifiers for files named [fileName] with the parent named [parentName].
     *
     * @param fileName name of the target files.
     * @param parentName name of one of the file's parents.
     * @return list of identifiers for files named [fileName] with the parent named [parentName].
     * @throws UserRecoverableAuthIOException when the user does not have enough rights to perform
     * an operation with Google Drive. This exception contains [android.content.Intent] to allow
     * user interaction to recover his rights.
     */
    @Throws(UserRecoverableAuthIOException::class)
    fun listJsonFileId(fileName: String, parentName: String = FOLDER_APPDATA): List<String> {
        Timber.d("Get root JSON file id list: fileName=$fileName")
        val fileIdList = mutableListOf<String>()
        var nextPageToken: String? = null
        do {
            val result = drive.files().list().apply {
                q = "name='$fileName' and mimeType='$MIME_TYPE_JSON' and '$parentName' in parents"
                fields = "nextPageToken, files(id, name, parents)"
                spaces = SPACE_APPDATA
                pageToken = nextPageToken
            }.execute()
            Timber.d("result=$result")
            val pageFileIdList = result.files?.map { it.id }
            pageFileIdList?.let { fileIdList.addAll(it) }
            nextPageToken = result.nextPageToken
            Timber.d("nextPageToken=$nextPageToken")
        } while (nextPageToken != null)
        Timber.d("fileIdList=$fileIdList")
        return fileIdList
    }

    /**
     * Retrieves the content of the file with the identifier [fileId].
     *
     * @param fileId identifier of the target file.
     * @return content of the file with the identifier [fileId].
     * @throws UserRecoverableAuthIOException when the user does not have enough rights to perform
     * an operation with Google Drive. This exception contains [android.content.Intent] to allow
     * user interaction to recover his rights.
     */
    @Throws(UserRecoverableAuthIOException::class)
    fun readFile(fileId: String): String {
        Timber.d("Read from file: fileId=$fileId")
        val inputStream = drive.files().get(fileId).executeMediaAsInputStream()
        val reader = BufferedReader(InputStreamReader(inputStream))
        val stringBuilder = StringBuilder()
        var line = reader.readLine()
        while (line != null) {
            stringBuilder.append(line)
            line = reader.readLine()
        }
        val content = stringBuilder.toString()
        Timber.d("content=$content")
        return content
    }

    /**
     * Creating a JSON file named [fileName] with the parent named [parentName] and writing [json]
     * to this file.
     *
     * @param fileName name of the target files.
     * @param json JSON to be written.
     * @param parentName name of the file's parent.
     * @throws UserRecoverableAuthIOException when the user does not have enough rights to perform
     * an operation with Google Drive. This exception contains [android.content.Intent] to allow
     * user interaction to recover his rights.
     */
    @Throws(UserRecoverableAuthIOException::class)
    fun createJsonFile(fileName: String, json: String, parentName: String = FOLDER_APPDATA) {
        Timber.d("Create JSON file: fileName=$fileName, json=$json")
        val parent = Collections.singletonList(parentName)
        val metadata = File()
            .setParents(parent)
            .setMimeType(MIME_TYPE_JSON)
            .setName(fileName)
        val contentStream = ByteArrayContent.fromString(MIME_TYPE_JSON, json)
        val file = drive.files().create(metadata, contentStream).execute()
        Timber.d("File created: file=$file")
    }

    /**
     * Updating a file with identifier [fileId]: setting JSON as the content type, [fileName] as the
     * name and writing [json] to this file.
     *
     * @param fileId identifier of the target file.
     * @param fileName name of the updated file .
     * @param json JSON to be written.
     * @throws UserRecoverableAuthIOException when the user does not have enough rights to perform
     * an operation with Google Drive. This exception contains [android.content.Intent] to allow
     * user interaction to recover his rights.
     */
    @Throws(UserRecoverableAuthIOException::class)
    fun updateJsonFile(fileId: String, fileName: String, json: String) {
        Timber.d("Update JSON file: fileId=$fileId, fileName=$fileName, json=$json")
        val metadata = File()
            .setMimeType(MIME_TYPE_JSON)
            .setName(fileName)
        val contentStream = ByteArrayContent.fromString(MIME_TYPE_JSON, json)
        val file = drive.files().update(fileId, metadata, contentStream).execute()
        Timber.d("File updated: file=$file")
    }
}
