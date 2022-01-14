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

    override fun readJson(fileName: String): String? {
        Timber.d("Read JSON: fileName=$fileName")
        return when (val helper = driveHelper) {
            null -> throw GoogleAuthException()
            else -> tryToReadJson(
                driveHelper = helper,
                fileName = fileName
            )
        }
    }

    override fun writeJson(fileName: String, json: String, jsonKey: String) {
        Timber.d("Write JSON: fileName=$fileName, json=$json")
        when (val helper = driveHelper) {
            null -> throw GoogleAuthException()
            else -> tryToWriteJson(
                driveHelper = helper,
                fileName = fileName,
                json = json,
                jsonKey = jsonKey
            )
        }
    }

    @Throws(GoogleRecoverableAuthException::class)
    private fun tryToReadJson(driveHelper: GoogleDriveHelper, fileName: String): String? {
        try {
            return readJson(driveHelper = driveHelper, fileName = fileName)
        } catch (e: UserRecoverableAuthIOException) {
            Timber.d(e)
            throw GoogleRecoverableAuthException(e)
        }
    }

    @Throws(GoogleRecoverableAuthException::class)
    private fun tryToWriteJson(
        driveHelper: GoogleDriveHelper,
        fileName: String,
        json: String,
        jsonKey: String
    ) {
        try {
            writeJson(driveHelper = driveHelper, fileName = fileName, json = json)
        } catch (e: UserRecoverableAuthIOException) {
            Timber.d(e)
            val authIOException = GoogleRecoverableAuthException(e)
            authIOException.additionalData[jsonKey] = json
            throw authIOException
        }
    }

    @Throws(UserRecoverableAuthIOException::class)
    private fun readJson(driveHelper: GoogleDriveHelper, fileName: String): String? {
        val fileIdList = driveHelper.listJsonFileId(fileName)
        return fileIdList.firstOrNull()?.let {
            driveHelper.readFile(it)
        }
    }

    @Throws(UserRecoverableAuthIOException::class)
    private fun writeJson(driveHelper: GoogleDriveHelper, fileName: String, json: String) {
        val fileIdList = driveHelper.listJsonFileId(fileName)
        if (fileIdList.isNullOrEmpty()) {
            driveHelper.createJsonFile(fileName = fileName, json = json)
        } else {
            fileIdList.forEach { fileId ->
                driveHelper.updateJsonFile(fileId = fileId, fileName = fileName, json = json)
            }
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
}
