package com.adkom666.shrednotes.data.google

import com.adkom666.shrednotes.common.Json

/**
 * High-level operations with Google Drive.
 */
interface GoogleDrive {

    /**
     * Reading a list of information about JSON files stored on Google Drive.
     *
     * @return list of of information about JSON files stored on Google Drive.
     * @throws GoogleAuthException when user is signed out of the Google account.
     * @throws GoogleRecoverableAuthException when the user does not have enough rights to perform
     * an operation with Google Drive. This exception contains [android.content.Intent] to allow
     * user interaction to recover his rights.
     */
    @Throws(GoogleAuthException::class, GoogleRecoverableAuthException::class)
    fun listJsonFiles(): List<GoogleDriveFile>

    /**
     * Retrieving content of the file with identifier [fileId].
     *
     * @param fileId identifier of the target file.
     * @param fileIdKey key of the target file identifier to save it in the
     * [GoogleRecoverableAuthException.additionalData] map for the case if
     * [GoogleRecoverableAuthException] has been thrown.
     * @return content of the file with identifier [fileId].
     * @throws GoogleAuthException when user is signed out of the Google account.
     * @throws GoogleRecoverableAuthException when the user does not have enough rights to perform
     * an operation with Google Drive. This exception contains [android.content.Intent] to allow
     * user interaction to recover his rights. This exception also contains [fileId] in its
     * [GoogleRecoverableAuthException.additionalData] map with the key [fileIdKey].
     */
    @Throws(GoogleAuthException::class, GoogleRecoverableAuthException::class)
    fun readFile(fileId: String, fileIdKey: String): String

    /**
     * Writing [json] to a [file].
     *
     * @param file target JSON file information.
     * @param fileKey key of the target JSON file information to save it in the
     * [GoogleRecoverableAuthException.additionalData] map for the case if
     * [GoogleRecoverableAuthException] has been thrown.
     * @param json JSON to be written.
     * @param jsonKey key of the original [json] in the
     * [GoogleRecoverableAuthException.additionalData] map for the case if
     * [GoogleRecoverableAuthException] has been thrown.
     * @throws GoogleAuthException when user is signed out of the Google account.
     * @throws GoogleRecoverableAuthException when the user does not have enough rights to perform
     * an operation with Google Drive. This exception contains [android.content.Intent] to allow
     * user interaction to recover his rights. This exception also contains [file] in its
     * [GoogleRecoverableAuthException.additionalData] map with the key [fileKey] and original
     * [json] with the key [jsonKey].
     */
    @Throws(GoogleAuthException::class, GoogleRecoverableAuthException::class)
    fun writeJson(file: GoogleDriveFile, fileKey: String, json: Json, jsonKey: String)

    /**
     * Deleting files with identifiers listed in [fileIdList].
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
    @Throws(GoogleAuthException::class, GoogleRecoverableAuthException::class)
    fun deleteFiles(fileIdList: List<String>, fileIdListKey: String)
}
