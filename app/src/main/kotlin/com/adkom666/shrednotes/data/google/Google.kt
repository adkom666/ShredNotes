package com.adkom666.shrednotes.data.google

import android.accounts.Account
import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
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
) : GoogleAuth,
    GoogleDrive {

    private companion object {
        private const val SCOPE_STRING_DRIVE_APPDATA = DriveScopes.DRIVE_APPDATA
        private val SCOPE_DRIVE_APPDATA = Scope(SCOPE_STRING_DRIVE_APPDATA)
    }

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

    override fun listJsonFiles(): List<GoogleDriveFile> {
        Timber.d("Read information about JSON files")
        return when (val helper = driveHelper) {
            null -> throw GoogleAuthException()
            else -> tryToListJsonFiles(driveHelper = helper)
        }
    }

    override fun readFile(fileId: String, fileIdKey: String): String {
        Timber.d("Read file: fileId=$fileId")
        return when (val helper = driveHelper) {
            null -> throw GoogleAuthException()
            else -> tryToReadFile(
                driveHelper = helper,
                fileId = fileId,
                fileIdKey = fileIdKey
            )
        }
    }

    override fun writeJson(
        driveFile: GoogleDriveFile,
        driveFileKey: String,
        json: String,
        jsonKey: String
    ) {
        Timber.d(
            """Write JSON:
                |driveFile=$driveFile,
                |driveFileKey=$driveFileKey,
                |json=$json,
                |jsonKey=$jsonKey""".trimMargin()
        )
        when (val helper = driveHelper) {
            null -> throw GoogleAuthException()
            else -> tryToWriteJson(
                driveHelper = helper,
                driveFile = driveFile,
                driveFileKey = driveFileKey,
                json = json,
                jsonKey = jsonKey
            )
        }
    }

    override fun deleteFiles(fileIdList: List<String>, fileIdListKey: String) {
        Timber.d("Delete files: fileIdList=$fileIdList, fileIdListKey=$fileIdListKey")
        when (val helper = driveHelper) {
            null -> throw GoogleAuthException()
            else -> tryToDeleteFiles(
                driveHelper = helper,
                fileIdList = fileIdList,
                fileIdListKey = fileIdListKey
            )
        }
    }

    private fun invalidateDriveHelper(context: Context) {
        Timber.d("Invalidate driveHelper")
        Timber.d("authHelper.signInAccount=${authHelper.signInAccount}")
        Timber.d("authHelper.signInAccount?.account=${authHelper.signInAccount?.account}")
        driveHelper = authHelper.signInAccount?.account?.let { account ->
            val appName = context.getString(appNameResId)
            val drive = createDrive(context, account, appName)
            GoogleDriveHelper(drive)
        }
        Timber.d("driveHelper=$driveHelper")
    }

    private fun createDrive(context: Context, account: Account, appName: String): Drive {
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
    private fun tryToListJsonFiles(
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
    private fun tryToReadFile(
        driveHelper: GoogleDriveHelper,
        fileId: String,
        fileIdKey: String
    ): String {
        try {
            return readFile(driveHelper = driveHelper, fileId = fileId)
        } catch (e: UserRecoverableAuthIOException) {
            Timber.d(e)
            val authIOException = GoogleRecoverableAuthException(e)
            authIOException.additionalData[fileIdKey] = fileId
            throw authIOException
        }
    }

    @Throws(GoogleRecoverableAuthException::class)
    private fun tryToWriteJson(
        driveHelper: GoogleDriveHelper,
        driveFile: GoogleDriveFile,
        driveFileKey: String,
        json: String,
        jsonKey: String
    ) {
        try {
            writeJson(driveHelper = driveHelper, driveFile = driveFile, json = json)
        } catch (e: UserRecoverableAuthIOException) {
            Timber.d(e)
            val authIOException = GoogleRecoverableAuthException(e)
            authIOException.additionalData[driveFileKey] = driveFile
            authIOException.additionalData[jsonKey] = json
            throw authIOException
        }
    }

    @Throws(GoogleRecoverableAuthException::class)
    private fun tryToDeleteFiles(
        driveHelper: GoogleDriveHelper,
        fileIdList: List<String>,
        fileIdListKey: String
    ) {
        try {
            deleteFiles(driveHelper = driveHelper, fileIdList = fileIdList)
        } catch (e: UserRecoverableAuthIOException) {
            Timber.d(e)
            val authIOException = GoogleRecoverableAuthException(e)
            authIOException.additionalData[fileIdListKey] = fileIdList
            throw authIOException
        }
    }

    @Throws(UserRecoverableAuthIOException::class)
    private fun readFile(driveHelper: GoogleDriveHelper, fileId: String): String {
        return driveHelper.readFile(fileId)
    }

    @Throws(UserRecoverableAuthIOException::class)
    private fun writeJson(
        driveHelper: GoogleDriveHelper,
        driveFile: GoogleDriveFile,
        json: String
    ) {
        if (driveFile.id.isNullOrBlank()) {
            driveHelper.createJsonFile(
                fileName = driveFile.name,
                json = json
            )
        } else {
            driveHelper.updateJsonFile(
                fileId = driveFile.id,
                fileName = driveFile.name,
                json = json
            )
        }
    }

    @Throws(UserRecoverableAuthIOException::class)
    private fun deleteFiles(driveHelper: GoogleDriveHelper, fileIdList: List<String>) {
        fileIdList.forEach { fileId ->
            driveHelper.deleteFile(fileId = fileId)
        }
    }
}
