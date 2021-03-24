package com.adkom666.shrednotes.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.distinctUntilChanged
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adkom666.shrednotes.data.DataManager
import com.adkom666.shrednotes.data.google.GoogleAuthException
import com.adkom666.shrednotes.data.google.GoogleRecoverableAuthException
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Main screen model.
 *
 * @property dataManager [DataManager] to read and write app info.
 */
@ExperimentalCoroutinesApi
class MainViewModel @Inject constructor(
    private val dataManager: DataManager
) : ViewModel() {

    private companion object {
        private val DEFAULT_SECTION = Section.NOTES

        private const val NAVIGATION_CHANNEL_CAPACITY = 3
        private const val MESSAGE_CHANNEL_CAPACITY = 3

        private const val KEY_JSON = "json"
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
             */
            object Continuing : Preparation()

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
         * Shred notes has not been updated (because they are not on Google drive).
         */
        object NoShredNotesUpdate : Message()

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
     * Consume navigation directions from this channel in the UI thread.
     */
    val navigationChannel: ReceiveChannel<NavDirection>
        get() = _navigationChannel.openSubscription()

    /**
     * Consume information messages from this channel in the UI thread.
     */
    val messageChannel: ReceiveChannel<Message>
        get() = _messageChannel.openSubscription()

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

    private val _navigationChannel: BroadcastChannel<NavDirection> =
        BroadcastChannel(NAVIGATION_CHANNEL_CAPACITY)

    private val _messageChannel: BroadcastChannel<Message> =
        BroadcastChannel(MESSAGE_CHANNEL_CAPACITY)

    private var readyJson: String? = null

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
     * Reading all content from Google Drive.
     */
    fun read() {
        Timber.d("Read")
        launchRead()
    }

    /**
     * Writing all content to Google Drive.
     */
    fun write() {
        Timber.d("Write")
        launchWrite()
    }

    /**
     * Call this method from [Activity.onActivityResult], making sure that the results of the Google
     * authorization call for intent from [NavDirection.ToSignIn.intent] are returned.
     *
     * @param context [Context].
     * @param resultCode there must be [Activity.RESULT_OK] if authorization goes well.
     * @param data authorization data.
     */
    fun handleSignInGoogleResult(context: Context, resultCode: Int, data: Intent?) {
        Timber.d("handleSignInGoogleResult: resultCode=$resultCode, data=$data")
        dataManager.googleAuth.handleSignInResult(context, resultCode, data) {
            setState(State.Preparation.GoogleDriveStateChanged)
        }
    }

    /**
     * Call this method from [Activity.onActivityResult], making sure that the results of the Google
     * authorization call for intent from [NavDirection.ToAuthOnRead.intent] are returned.
     *
     * @param resultCode there must be [Activity.RESULT_OK] if authorization goes well.
     */
    fun handleAuthOnReadResult(resultCode: Int) {
        Timber.d("handleAuthOnReadResult: resultCode=$resultCode")
        if (resultCode == Activity.RESULT_OK) {
            launchRead()
        }
    }

    /**
     * Call this method from [Activity.onActivityResult], making sure that the results of the Google
     * authorization call for intent from [NavDirection.ToAuthOnWrite.intent] are returned.
     *
     * @param resultCode there must be [Activity.RESULT_OK] if authorization goes well.
     */
    fun handleAuthOnWriteResult(resultCode: Int) {
        Timber.d("handleAuthOnWriteResult: resultCode=$resultCode")
        if (resultCode == Activity.RESULT_OK) {
            launchWrite()
        }
    }

    private fun launchRead() {
        setState(State.Waiting(State.Waiting.Operation.READING))
        viewModelScope.launch {
            @Suppress("TooGenericExceptionCaught")
            try {
                val message = if (dataManager.read()) {
                    Message.ShredNotesUpdate
                } else {
                    Message.NoShredNotesUpdate
                }
                setState(State.Preparation.Continuing)
                report(message)
            } catch (e: GoogleAuthException) {
                Timber.e(e)
                setState(State.Preparation.Continuing)
                report(Message.Error.UnauthorizedUser)
            } catch (e: GoogleRecoverableAuthException) {
                Timber.e(e)
                setState(State.Preparation.Continuing)
                e.intent?.let {
                    navigateTo(NavDirection.ToAuthOnRead(it))
                } ?: report(Message.Error.Unknown)
            } catch (e: JsonSyntaxException) {
                Timber.e(e)
                setState(State.Preparation.Continuing)
                report(Message.Error.WrongJsonSyntax)
            } catch (e: Exception) {
                Timber.e(e)
                setState(State.Preparation.Continuing)
                reportAbout(e)
            }
        }
    }

    private fun launchWrite() {
        setState(State.Waiting(State.Waiting.Operation.WRITING))
        viewModelScope.launch {
            @Suppress("TooGenericExceptionCaught")
            try {
                val json = readyJson
                readyJson = null
                dataManager.write(jsonKey = KEY_JSON, readyJson = json)
                setState(State.Preparation.Continuing)
                report(Message.GoogleDriveUpdate)
            } catch (e: GoogleAuthException) {
                Timber.e(e)
                setState(State.Preparation.Continuing)
                report(Message.Error.UnauthorizedUser)
            } catch (e: GoogleRecoverableAuthException) {
                Timber.e(e)
                setState(State.Preparation.Continuing)
                readyJson = e.additionalData[KEY_JSON] as? String
                e.intent?.let {
                    navigateTo(NavDirection.ToAuthOnWrite(it))
                } ?: report(Message.Error.Unknown)
            } catch (e: Exception) {
                Timber.e(e)
                setState(State.Preparation.Continuing)
                reportAbout(e)
            }
        }
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
        _navigationChannel.offer(direction)
    }

    private fun report(message: Message) {
        Timber.d("Report: message=$message")
        _messageChannel.offer(message)
    }
}
