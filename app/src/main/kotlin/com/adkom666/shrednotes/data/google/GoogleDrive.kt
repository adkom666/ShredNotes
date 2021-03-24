package com.adkom666.shrednotes.data.google

/**
 * High-level operations with Google Drive.
 */
interface GoogleDrive {

    /**
     * Retrieving content of the JSON file named [fileName].
     *
     * @param fileName name of the target JSON file.
     * @return content of the JSON file named [fileName].
     * @throws GoogleAuthException when user is signed out of the Google account.
     * @throws GoogleRecoverableAuthException when the user does not have enough rights to perform
     * an operation with Google Drive. This exception contains [android.content.Intent] to allow
     * user interaction to recover his rights.
     */
    @Throws(GoogleAuthException::class, GoogleRecoverableAuthException::class)
    fun readJson(fileName: String): String?

    /**
     * Writing [json] to a file named [fileName].
     *
     * @param fileName name of the target JSON file.
     * @param json JSON to be written.
     * @param jsonKey key of the original [json] in the
     * [GoogleRecoverableAuthException.additionalData] map for the case if
     * [GoogleRecoverableAuthException] has been thrown.
     * @throws GoogleAuthException when user is signed out of the Google account.
     * @throws GoogleRecoverableAuthException when the user does not have enough rights to perform
     * an operation with Google Drive. This exception contains [android.content.Intent] to allow
     * user interaction to recover his rights. This exception also contains the original [json] in
     * its [GoogleRecoverableAuthException.additionalData] map with the key [jsonKey].
     */
    @Throws(GoogleAuthException::class, GoogleRecoverableAuthException::class)
    fun writeJson(fileName: String, json: String, jsonKey: String)
}
