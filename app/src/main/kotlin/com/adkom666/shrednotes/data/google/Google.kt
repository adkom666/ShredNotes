package com.adkom666.shrednotes.data.google

import android.accounts.Account
import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import com.adkom666.shrednotes.common.Json
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import java.util.Collections
import timber.log.Timber

/**
 * Interact with Google.
 */
class Google(
    context: Context,
    @StringRes
    private val appNameResId: Int
) : GoogleAuth {

    private companion object {
        private const val SCOPE_STRING_DRIVE_APPDATA = DriveScopes.DRIVE_APPDATA
        private val SCOPE_DRIVE_APPDATA = Scope(SCOPE_STRING_DRIVE_APPDATA)
    }

    /**
     * Interact with Google Drive here.
     */
    val drive: GoogleDrive = DriveImpl()

    private val authHelper: GoogleAuthHelper = GoogleAuthHelper(context, SCOPE_DRIVE_APPDATA)
    private var driveHelper: GoogleDriveHelper? = null

    init {
        Timber.d("Init")
        Timber.d("Last Google signed in account: ${authHelper.signInAccount?.displayName}")
        invalidateDriveHelper(context)
    }

    override val isSignedIn: Boolean
        get() = authHelper.signInAccount != null

    override val accountDisplayName: String?
        get() = authHelper.signInAccount?.displayName

    override fun signInIntent(context: Context): Intent {
        Timber.d("Sign in intent requested")
        return authHelper.signInIntent(context)
    }

    override fun handleSignInResult(
        context: Context,
        resultCode: Int,
        data: Intent?,
        onSuccessListener: () -> Unit
    ) {
        Timber.d("handleSignInResult: resultCode=$resultCode, data=$data")
        authHelper.handleSignInResult(resultCode, data) {
            Timber.d("Sign in was successful")
            invalidateDriveHelper(context)
            onSuccessListener()
        }
    }

    override fun signOut(context: Context) {
        Timber.d("Sign out")
        authHelper.signOut(context)
        invalidateDriveHelper(context)
    }

    private fun invalidateDriveHelper(context: Context) {
        Timber.d("Invalidate driveHelper")
        Timber.d("authHelper.signInAccount=${authHelper.signInAccount}")
        Timber.d("authHelper.signInAccount?.account=${authHelper.signInAccount?.account}")
        driveHelper = authHelper.signInAccount?.account?.let { account ->
            val appName = context.getString(appNameResId)
            val driveV3 = createDriveV3(context, account, appName)
            GoogleDriveHelper(driveV3)
        }
        Timber.d("driveHelper=$driveHelper")
    }

    private fun createDriveV3(context: Context, account: Account, appName: String): Drive {
        val transport = AndroidHttp.newCompatibleTransport()
        val jacksonFactory = JacksonFactory.getDefaultInstance()
        val scopes = Collections.singleton(SCOPE_STRING_DRIVE_APPDATA)
        val credential = GoogleAccountCredential.usingOAuth2(context.applicationContext, scopes)
        credential.selectedAccount = account
        return Drive.Builder(transport, jacksonFactory, credential)
            .setApplicationName(appName)
            .build()
    }

    @Throws(GoogleRecoverableAuthException::class)
    private fun tryToListJsonDriveFiles(
        driveHelper: GoogleDriveHelper
    ): List<GoogleDriveFile> {
        try {
            return driveHelper.listJsonFile()
        } catch (e: UserRecoverableAuthIOException) {
            Timber.d(e)
            throw GoogleRecoverableAuthException(e)
        }
    }

    @Throws(GoogleRecoverableAuthException::class)
    private fun tryToReadFileFromDrive(
        driveHelper: GoogleDriveHelper,
        fileId: String,
        fileIdKey: String
    ): String {
        try {
            return readFileFromDrive(driveHelper = driveHelper, fileId = fileId)
        } catch (e: UserRecoverableAuthIOException) {
            Timber.d(e)
            val authIOException = GoogleRecoverableAuthException(e)
            authIOException.additionalData[fileIdKey] = fileId
            throw authIOException
        }
    }

    @Throws(GoogleRecoverableAuthException::class)
    private fun tryToWriteJsonToDrive(
        driveHelper: GoogleDriveHelper,
        file: GoogleDriveFile,
        fileKey: String,
        json: Json,
        jsonKey: String
    ) {
        try {
            writeJsonToDrive(driveHelper = driveHelper, file = file, json = json)
        } catch (e: UserRecoverableAuthIOException) {
            Timber.d(e)
            val authIOException = GoogleRecoverableAuthException(e)
            authIOException.additionalData[fileKey] = file
            authIOException.additionalData[jsonKey] = json
            throw authIOException
        }
    }

    @Throws(GoogleRecoverableAuthException::class)
    private fun tryToDeleteFilesFromDrive(
        driveHelper: GoogleDriveHelper,
        fileIdList: List<String>,
        fileIdListKey: String
    ) {
        try {
            deleteFilesFromDrive(driveHelper = driveHelper, driveFileIdList = fileIdList)
        } catch (e: UserRecoverableAuthIOException) {
            Timber.d(e)
            val authIOException = GoogleRecoverableAuthException(e)
            authIOException.additionalData[fileIdListKey] = fileIdList
            throw authIOException
        }
    }

    @Throws(UserRecoverableAuthIOException::class)
    private fun readFileFromDrive(driveHelper: GoogleDriveHelper, fileId: String): String {
        return driveHelper.readFile(fileId)
    }

    @Throws(UserRecoverableAuthIOException::class)
    private fun writeJsonToDrive(
        driveHelper: GoogleDriveHelper,
        file: GoogleDriveFile,
        json: Json
    ) {
        if (file.id.isNullOrBlank()) {
            driveHelper.createJsonFile(
                fileName = file.name,
                json = json
            )
        } else {
            driveHelper.updateJsonFile(
                fileId = file.id,
                fileName = file.name,
                json = json
            )
        }
    }

    @Throws(UserRecoverableAuthIOException::class)
    private fun deleteFilesFromDrive(
        driveHelper: GoogleDriveHelper,
        driveFileIdList: List<String>
    ) {
        driveFileIdList.forEach { driveFileId ->
            driveHelper.deleteFile(fileId = driveFileId)
        }
    }

    private inner class DriveImpl : GoogleDrive {

        override fun listJsonFiles(): List<GoogleDriveFile> {
            Timber.d("Read information about JSON files")
            return when (val helper = driveHelper) {
                null -> throw GoogleAuthException()
                else -> tryToListJsonDriveFiles(driveHelper = helper)
            }
        }

        override fun readFile(fileId: String, fileIdKey: String): String {
            Timber.d("Read file: fileId=$fileId")
            return when (val helper = driveHelper) {
                null -> throw GoogleAuthException()
                else -> tryToReadFileFromDrive(
                    driveHelper = helper,
                    fileId = fileId,
                    fileIdKey = fileIdKey
                )
            }
        }

        override fun writeJson(
            file: GoogleDriveFile,
            fileKey: String,
            json: Json,
            jsonKey: String
        ) {
            Timber.d(
                """Write JSON:
                    |file=$file,
                    |fileKey=$fileKey,
                    |json=$json,
                    |jsonKey=$jsonKey""".trimMargin()
            )
            when (val helper = driveHelper) {
                null -> throw GoogleAuthException()
                else -> tryToWriteJsonToDrive(
                    driveHelper = helper,
                    file = file,
                    fileKey = fileKey,
                    json = json,
                    jsonKey = jsonKey
                )
            }
        }

        override fun deleteFiles(fileIdList: List<String>, fileIdListKey: String) {
            Timber.d(
                """Delete files:
                    |fileIdList=$fileIdList,
                    |fileIdListKey=$fileIdListKey""".trimMargin()
            )
            when (val helper = driveHelper) {
                null -> throw GoogleAuthException()
                else -> tryToDeleteFilesFromDrive(
                    driveHelper = helper,
                    fileIdList = fileIdList,
                    fileIdListKey = fileIdListKey
                )
            }
        }
    }
}
