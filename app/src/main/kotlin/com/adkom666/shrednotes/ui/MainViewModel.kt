package com.adkom666.shrednotes.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.distinctUntilChanged
import com.adkom666.shrednotes.BuildConfig
import com.adkom666.shrednotes.ask.Donor
import com.adkom666.shrednotes.data.DataManager
import com.adkom666.shrednotes.data.pref.ToolPreferences
import com.adkom666.shrednotes.data.UnsupportedDataException
import com.adkom666.shrednotes.data.google.GoogleAuthException
import com.adkom666.shrednotes.data.google.GoogleDriveFile
import com.adkom666.shrednotes.data.google.GoogleRecoverableAuthException
import com.adkom666.shrednotes.util.ExecutiveViewModel
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import timber.log.Timber
import javax.inject.Inject

/**
 * Main screen model.
 *
 * @property dataManager [DataManager] to read and write app info.
 * @property donor donor of money.
 * @property toolPreferences project's [ToolPreferences] to manage, for example, filter and search.
 */
class MainViewModel @Inject constructor(
    private val dataManager: DataManager,
    private val donor: Donor,
    private val toolPreferences: ToolPreferences
) : ExecutiveViewModel() {

    private companion object {
        private val DEFAULT_SECTION = Section.NOTES
        private const val KEY_FILE_ID = "file_id"
        private const val KEY_FILE = "file"
        private const val KEY_JSON = "json"
        private const val DEFAULT_FILE_NAME = "_shrednotes.json"
    }

    /**
     * Main screen state.
     */
    sealed class State {

        /**
         * Prepare for interacting with the user and tell me by [ok] call.
         */
        sealed class Preparation : State() {

            /**
             * At the very beginning, the options menu and account name are already displayed.
             */
            object Initial : Preparation()

            /**
             * Show options menu before continuing.
             *
             * @property isForceInvalidateTools whether to forcibly invalidate tools after
             * continuing, for example, invalidate search and filter.
             */
            data class Continuing(val isForceInvalidateTools: Boolean) : Preparation()

            /**
             * The user is logged in or logged out. Invalidate the options menu and the account
             * name.
             */
            object GoogleDriveStateChanged : Preparation()
        }

        /**
         * Interacting with the user. Show content, hide progress bar.
         */
        object Working : State()

        /**
         * Waiting for the end of some operation, e.g. read or write. Hide options menu and content,
         * show progress bar.
         */
        data class Waiting(val operation: Operation) : State() {

            /**
             * Operation that is expected to be completed.
             */
            enum class Operation {
                READING,
                WRITING
            }
        }
    }

    /**
     * Navigation direction.
     */
    sealed class NavDirection {

        /**
         * To signing in Google screen.
         *
         * @property intent [Intent] to use when calling an activity.
         */
        data class ToSignIn(val intent: Intent) : NavDirection()

        /**
         * To authorization when reading all content from Google Drive.
         *
         * @property intent [Intent] to use when calling an activity.
         */
        data class ToAuthOnRead(val intent: Intent) : NavDirection()

        /**
         * To authorization when writing all content to Google Drive.
         *
         * @property intent [Intent] to use when calling an activity.
         */
        data class ToAuthOnWrite(val intent: Intent) : NavDirection()
    }

    /**
     * Information message.
     */
    sealed class Message {

        /**
         * Shred notes has been updated.
         */
        object ShredNotesUpdate : Message()

        /**
         * Google Drive has been updated.
         */
        object GoogleDriveUpdate : Message()

        /**
         * Error message.
         */
        sealed class Error : Message() {

            /**
             * The user is signed out of the Google account.
             */
            object UnauthorizedUser : Error()

            /**
             * The syntax of the JSON from Google Drive is incorrect.
             */
            object WrongJsonSyntax : Error()

            /**
             * The [version] of shred notes data is not supported.
             *
             * @property version suggested version.
             */
            data class UnsupportedDataVersion(val version: Int) : Error()

            /**
             * The details of this error are described in the [details].
             *
             * @property details the details of the error.
             */
            data class Clarified(val details: String) : Error()

            /**
             * Unknown error.
             */
            object Unknown : Error()
        }
    }

    /**
     * Information signal.
     */
    sealed class Signal {

        /**
         * All app content has been updated.
         */
        object ContentUpdated : Signal()
    }

    /**
     * Visibility of the menu items that allow you to work with 'Google Drive'.
     *
     * @property isItemReadVisibile whether the 'Read' item must be visible.
     * @property isItemWriteVisibile whether the 'Write' item must be visible.
     * @property isItemSignOutVisibile whether the 'Sign Out' item must be visible.
     * @property isItemSignInVisibile whether the 'Sign In' item must be visible.
     */
    data class GoogleDriveItemsVisibility(
        val isItemReadVisibile: Boolean,
        val isItemWriteVisibile: Boolean,
        val isItemSignOutVisibile: Boolean,
        val isItemSignInVisibile: Boolean
    )

    /**
     * Read the current state in the UI thread.
     */
    val state: State?
        get() = _stateAsLiveData.value

    /**
     * Subscribe to the current state in the UI thread.
     */
    val stateAsLiveData: LiveData<State>
        get() = distinctUntilChanged(_stateAsLiveData)

    /**
     * Get and set the current section in the UI thread.
     */
    var section: Section
        get() = _sectionAsLiveData.value ?: DEFAULT_SECTION
        set(value) {
            _sectionAsLiveData.value = value
        }

    /**
     * Subscribe to the current section in the UI thread.
     */
    val sectionAsLiveData: LiveData<Section>
        get() = distinctUntilChanged(_sectionAsLiveData)

    /**
     * Collect navigation directions from this flow in the UI thread.
     */
    val navigationFlow: Flow<NavDirection>
        get() = _navigationChannel.receiveAsFlow()

    /**
     * Collect information messages from this flow in the UI thread.
     */
    val messageFlow: Flow<Message>
        get() = _messageChannel.receiveAsFlow()

    /**
     * Collect information signals from this flow in the UI thread.
     */
    val signalFlow: Flow<Signal>
        get() = _signalChannel.receiveAsFlow()

    /**
     * Read visibility of the menu items that allow you to work with 'Google Drive'.
     */
    val googleDriveItemsVisibility: GoogleDriveItemsVisibility
        get() {
            val isSignedInGoogle = dataManager.googleAuth.isSignedIn
            return GoogleDriveItemsVisibility(
                isItemReadVisibile = isSignedInGoogle,
                isItemWriteVisibile = isSignedInGoogle,
                isItemSignOutVisibile = isSignedInGoogle,
                isItemSignInVisibile = isSignedInGoogle.not()
            )
        }

    /**
     * Read current account name.
     */
    val googleAccountDisplayName: String?
        get() = dataManager.googleAuth.accountDisplayName

    private val _stateAsLiveData: MutableLiveData<State> =
        MutableLiveData(State.Preparation.Initial)

    private val _sectionAsLiveData: MutableLiveData<Section> =
        MutableLiveData(DEFAULT_SECTION)

    private val _navigationChannel: Channel<NavDirection> = Channel(1)
    private val _messageChannel: Channel<Message> = Channel(Channel.UNLIMITED)
    private val _signalChannel: Channel<Signal> = Channel(Channel.UNLIMITED)

    private var readyFileId: String? = null
    private var readyFile: GoogleDriveFile? = null
    private var readyJson: String? = null

    override fun onCleared() {
        super.onCleared()
        Timber.d("On cleared")
        donor.dispose()
    }

    /**
     * Tell the model that all the information received from it has been used.
     */
    fun ok() {
        Timber.d("OK!")
        setState(State.Working)
    }

    /**
     * Sign in your Google account.
     *
     * @param context [Context].
     */
    fun signInGoogle(context: Context) {
        Timber.d("Sign in Google")
        val intent = dataManager.googleAuth.signInIntent(context)
        navigateTo(NavDirection.ToSignIn(intent))
    }

    /**
     * Sign out from your Google account.
     *
     * @param context [Context].
     */
    fun signOutFromGoogle(context: Context) {
        Timber.d("Sign out from Google")
        dataManager.googleAuth.signOut(context)
        setState(State.Preparation.GoogleDriveStateChanged)
    }

    /**
     * Reading all content from Google Drive file with identifier [fileId].
     *
     * @param fileId name of the target file identifier.
     */
    fun read(fileId: String) {
        Timber.d("Read: fileId=$fileId")
        launchRead(fileId)
    }

    /**
     * Writing all content to Google Drive.
     *
     * @param googleDriveFile information about the target JSON file.
     */
    fun write(googleDriveFile: GoogleDriveFile) {
        Timber.d("Write: googleDriveFile=$googleDriveFile")
        launchWrite(googleDriveFile)
    }

    /**
     * Call this method when activity result is acquired, making sure that the results of the Google
     * authorization call for intent from [NavDirection.ToSignIn.intent] are returned.
     *
     * @param context [Context].
     * @param resultCode the integer result code returned by the child activity through its
     * [Activity.setResult].
     * @param data authorization data.
     */
    fun handleSignInGoogleResult(context: Context, resultCode: Int, data: Intent?) {
        Timber.d("Handle sign in Google result: resultCode=$resultCode, data=$data")
        dataManager.googleAuth.handleSignInResult(context, resultCode, data) {
            setState(State.Preparation.GoogleDriveStateChanged)
        }
    }

    /**
     * Call this method when activity result is acquired, making sure that the results of the Google
     * authorization call for intent from [NavDirection.ToAuthOnRead.intent] are returned.
     *
     * @param resultCode the integer result code returned by the child activity through its
     * [Activity.setResult].
     */
    fun handleAuthOnReadResult(resultCode: Int) {
        Timber.d("Handle auth on read result: resultCode=$resultCode")
        if (resultCode == Activity.RESULT_OK) {
            launchRead()
        }
    }

    /**
     * Call this method when activity result is acquired, making sure that the results of the Google
     * authorization call for intent from [NavDirection.ToAuthOnWrite.intent] are returned.
     *
     * @param resultCode the integer result code returned by the child activity through its
     * [Activity.setResult].
     */
    fun handleAuthOnWriteResult(resultCode: Int) {
        Timber.d("Handle auth on write result: resultCode=$resultCode")
        if (resultCode == Activity.RESULT_OK) {
            launchWrite()
        }
    }

    /**
     * Invalidate tools for all possible screens.
     */
    fun invalidateTools() {
        toolPreferences.invalidate()
    }

    private fun launchRead(fileId: String? = null) = execute {
        setState(State.Waiting(State.Waiting.Operation.READING))
        @Suppress("TooGenericExceptionCaught")
        val isForceInvalidateTools = try {
            val finalFileId = finalFileId(fileId, readyFileId)
            readyFileId = null
            dataManager.readShredNotesFromGoogleDriveJsonFile(
                fileId = finalFileId,
                fileIdKey = KEY_FILE_ID
            )
            give(Signal.ContentUpdated)
            report(Message.ShredNotesUpdate)
            true
        } catch (e: GoogleAuthException) {
            Timber.e(e)
            report(Message.Error.UnauthorizedUser)
            false
        } catch (e: GoogleRecoverableAuthException) {
            Timber.e(e)
            readyFileId = e.additionalData[KEY_FILE_ID] as? String
            e.intent?.let {
                navigateTo(NavDirection.ToAuthOnRead(it))
            } ?: report(Message.Error.Unknown)
            false
        } catch (e: JsonSyntaxException) {
            Timber.e(e)
            report(Message.Error.WrongJsonSyntax)
            false
        } catch (e: UnsupportedDataException) {
            Timber.e(e)
            report(Message.Error.UnsupportedDataVersion(e.version))
            false
        } catch (e: Exception) {
            Timber.e(e)
            reportAbout(e)
            false
        }
        setState(State.Preparation.Continuing(isForceInvalidateTools))
    }

    private fun launchWrite(googleDriveFile: GoogleDriveFile? = null) = execute {
        setState(State.Waiting(State.Waiting.Operation.WRITING))
        @Suppress("TooGenericExceptionCaught")
        try {
            val finalFile = finalFile(googleDriveFile, readyFile)
            readyFile = null
            val json = readyJson
            readyJson = null
            dataManager.writeShredNotesToGoogleDriveJsonFile(
                file = finalFile,
                fileKey = KEY_FILE,
                readyJson = json,
                jsonKey = KEY_JSON,
            )
            report(Message.GoogleDriveUpdate)
        } catch (e: GoogleAuthException) {
            Timber.e(e)
            report(Message.Error.UnauthorizedUser)
        } catch (e: GoogleRecoverableAuthException) {
            Timber.e(e)
            readyFile = e.additionalData[KEY_FILE] as? GoogleDriveFile
            readyJson = e.additionalData[KEY_JSON] as? String
            e.intent?.let {
                navigateTo(NavDirection.ToAuthOnWrite(it))
            } ?: report(Message.Error.Unknown)
        } catch (e: Exception) {
            Timber.e(e)
            reportAbout(e)
        }
        setState(State.Preparation.Continuing(isForceInvalidateTools = false))
    }

    private fun finalFileId(
        fileId: String?,
        readyFileId: String?
    ): String = fileId
        ?: readyFileId
        ?: if (BuildConfig.DEBUG) {
            error("Missing file identifier and ready file identifier!")
        } else {
            ""
        }

    private fun finalFile(
        file: GoogleDriveFile?,
        readyFile: GoogleDriveFile?
    ): GoogleDriveFile = file
        ?: readyFile
        ?: if (BuildConfig.DEBUG) {
            error("Missing file and ready file!")
        } else {
            GoogleDriveFile(id = null, name = DEFAULT_FILE_NAME)
        }

    private fun reportAbout(e: Exception) {
        e.localizedMessage?.let {
            report(Message.Error.Clarified(it))
        } ?: report(Message.Error.Unknown)
    }

    private fun setState(state: State) {
        Timber.d("Set state: state=$state")
        _stateAsLiveData.postValue(state)
    }

    private fun navigateTo(direction: NavDirection) {
        Timber.d("Navigate to: direction=$direction")
        _navigationChannel.trySend(direction)
    }

    private fun report(message: Message) {
        Timber.d("Report: message=$message")
        _messageChannel.trySend(message)
    }

    private fun give(signal: Signal) {
        Timber.d("Give: signal=$signal")
        _signalChannel.trySend(signal)
    }
}
