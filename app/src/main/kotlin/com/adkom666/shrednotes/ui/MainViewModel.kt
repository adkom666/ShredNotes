package com.adkom666.shrednotes.ui

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.distinctUntilChanged
import androidx.lifecycle.ViewModel
import com.adkom666.shrednotes.data.google.Google
import timber.log.Timber
import javax.inject.Inject

/**
 * Main screen model.
 *
 * @property google [Google] to access "Google Drive" to store app data.
 */
class MainViewModel @Inject constructor(
    private val google: Google
) : ViewModel() {

    private companion object {
        private val DEFAULT_SECTION = Section.NOTES
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
        object Waiting : State()
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
     * Read visibility of the menu items that allow you to work with 'Google Drive'.
     */
    val googleDriveItemsVisibility: GoogleDriveItemsVisibility
        get() {
            val isSignedInGoogle = google.isSignedIn
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
        get() = google.accountDisplayName

    /**
     * Use this intent to sign in your Google account. When you get the result, give it to the
     * [handleSignInGoogleResult] method.
     */
    val signInGoogleIntent: Intent
        get() = google.signInIntent

    private val _stateAsLiveData: MutableLiveData<State> =
        MutableLiveData(State.Preparation.Initial)

    private val _sectionAsLiveData: MutableLiveData<Section> =
        MutableLiveData(DEFAULT_SECTION)

    /**
     * Tell the model that all the information received from it has been used.
     */
    fun ok() {
        Timber.d("OK!")
        setState(State.Working)
    }

    /**
     * Call this method from [Activity.onActivityResult], making sure that the results of the Google
     * authorization call for the previously received intent (from [signInGoogleIntent]) are
     * returned.
     *
     * @param resultCode there must be [Activity.RESULT_OK] if authorization goes well.
     * @param data authorization data.
     */
    fun handleSignInGoogleResult(resultCode: Int, data: Intent?) {
        Timber.d("handleSignInGoogleResult: resultCode=$resultCode, data=$data")
        google.handleSignInResult(resultCode, data) {
            setState(State.Preparation.GoogleDriveStateChanged)
        }
    }

    /**
     * Sign out from your Google account.
     */
    fun signOutFromGoogle() {
        Timber.d("Sign out from Google")
        google.signOut()
        setState(State.Preparation.GoogleDriveStateChanged)
    }

    private fun setState(state: State) {
        Timber.d("Set state: state=$state")
        _stateAsLiveData.postValue(state)
    }
}
