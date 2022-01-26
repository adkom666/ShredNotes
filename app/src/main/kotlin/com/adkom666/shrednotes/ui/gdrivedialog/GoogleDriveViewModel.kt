package com.adkom666.shrednotes.ui.gdrivedialog

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.distinctUntilChanged
import androidx.lifecycle.map
import com.adkom666.shrednotes.BuildConfig
import com.adkom666.shrednotes.data.DataManager
import com.adkom666.shrednotes.data.google.GoogleAuthException
import com.adkom666.shrednotes.data.google.GoogleRecoverableAuthException
import com.adkom666.shrednotes.util.ExecutiveViewModel
import com.adkom666.shrednotes.util.selection.ManageableSelection
import com.adkom666.shrednotes.util.selection.OnActivenessChangeListener
import com.adkom666.shrednotes.util.selection.SelectableItems
import com.adkom666.shrednotes.util.selection.Selection
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import timber.log.Timber
import javax.inject.Inject

/**
 * Google Drive dialog model.
 *
 * @property dataManager [DataManager] to manage files with app info.
 */
class GoogleDriveViewModel @Inject constructor(
    private val dataManager: DataManager
) : ExecutiveViewModel() {

    private companion object {
        private const val FILE_SUFFIX = ".json"
    }

    /**
     * Google Drive dialog state.
     */
    sealed class State {

        /**
         * Waiting for the end of some operation.
         *
         * @property isCancelable whether user is able to cancel the dialog.
         */
        data class Waiting(val isCancelable: Boolean = true) : State()

        /**
         * Interacting with the user.
         *
         * @property workingMode current [Mode].
         */
        data class Working(val workingMode: Mode) : State() {

            /**
             * In addition to entering a file name, the user has the ability to delete files from
             * Google Drive.
             */
            enum class Mode {
                ENTERING_FILE_NAME,
                DELETION
            }
        }

        /**
         * Finishing work with the dialog.
         */
        sealed class Finishing : State() {

            /**
             * Cancelling work with the dialog.
             */
            object Cancelling : Finishing()

            /**
             * Finishing work with the dialog providing the name of the target file.
             *
             * @property fileName the name of the target file provided by the user.
             */
            data class Dismissing(val fileName: String) : Finishing()
        }
    }

    /**
     * Navigation direction.
     */
    sealed class NavDirection {

        /**
         * To authorization on Google.
         *
         * @property intent [Intent] to use when calling an activity.
         */
        data class ToAuth(val intent: Intent) : NavDirection()
    }

    /**
     * Information message.
     */
    sealed class Message {

        /**
         * Message about the consequences of reading or writing, allowing you to confirm or reject
         * this operation.
         */
        object ConfirmOperation : Message()

        /**
         * Message about the consequences of deletion, allowing you to confirm or reject it.
         */
        object ConfirmDeletion : Message()

        /**
         * Error message.
         */
        sealed class Error : Message() {

            /**
             * The user is signed out of the Google account.
             */
            object UnauthorizedUser : Error()

            /**
             * No file is selected.
             */
            object UnselectedFile : Error()

            /**
             * File name is empty or blank.
             */
            object MissingFileName : Error()

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
     * Subscribe to the current state in the UI thread.
     */
    val stateAsLiveData: LiveData<State>
        get() = distinctUntilChanged(_stateAsLiveData)

    /**
     * Subscribe to the current file name list in the UI thread.
     */
    val fileNamesAsLiveData: LiveData<List<String>?>
        get() = distinctUntilChanged(_fileNamesAsLiveData)

    /**
     * Subscribe to the target file selection in the UI thread.
     */
    val targetFileSelectionAsLiveData: LiveData<String?>
        get() = distinctUntilChanged(_targetFileNameAsLiveData.map { it?.fileSelection() })

    /**
     * Subscribe to the target file name in the UI thread.
     */
    val targetFileNameAsLiveData: LiveData<String?>
        get() = distinctUntilChanged(_targetFileNameAsLiveData)

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
     * Files to select.
     */
    val selectableFiles: SelectableItems
        get() = _manageableSelection

    /**
     * Information about the presence of selected notes.
     */
    val selection: Selection
        get() = _manageableSelection

    private val mode: GoogleDriveDialogMode
        get() = requireNotNull(_mode)

    private val _stateAsLiveData: MutableLiveData<State> = MutableLiveData(State.Waiting())
    private val _fileNamesAsLiveData: MutableLiveData<List<String>?> = MutableLiveData(null)
    private val _targetFileNameAsLiveData: MutableLiveData<String?> = MutableLiveData(null)
    private val _navigationChannel: Channel<NavDirection> = Channel(1)
    private val _messageChannel: Channel<Message> = Channel(Channel.UNLIMITED)
    private val _manageableSelection: ManageableSelection = ManageableSelection()

    private var _mode: GoogleDriveDialogMode? = null

    private var googleDriveFileNames: List<String>? = null
    private var displayedFileNames: MutableList<String>? = null
    private var fileNameToConfirm: String? = null

    private val onSelectionActivenessChangeListener: OnActivenessChangeListener =
        object : OnActivenessChangeListener {
            override fun onActivenessChange(isActive: Boolean) {
                val workingMode = if (isActive) {
                    State.Working.Mode.DELETION
                } else {
                    State.Working.Mode.ENTERING_FILE_NAME
                }
                setState(State.Working(workingMode))
            }
        }

    init {
        Timber.d("Init")
        _manageableSelection.addOnActivenessChangeListener(onSelectionActivenessChangeListener)
    }

    /**
     * Prepare for working with the Google Drive.
     *
     * @param mode Google Drive dialog mode (reading or writing).
     */
    fun prepare(mode: GoogleDriveDialogMode) {
        Timber.d("Prepare")
        _mode = mode
    }

    /**
     * Start working.
     */
    fun start() {
        Timber.d("Start")
        execute {
            setState(State.Waiting())
            tryToReadJsonFileNames()
        }
    }

    /**
     * Call this method to handle the file name click.
     *
     * @param fileName clicked file name.
     */
    fun onFileClick(fileName: String?) {
        Timber.d("On file name click")
        setTargetFileName(fileName)
    }

    /**
     * Call this method to handle the target file name change.
     */
    fun onTargetFileNameChange(fileName: String?) {
        Timber.d("On target file name change")
        setTargetFileName(fileName)
    }

    /**
     * Call this method to handle the positive button click.
     */
    fun onPositiveButtonClick() {
        Timber.d("On positive button click")
        val fileName = targetFileNameForGoogleDrive()
        if (fileName.isNullOrEmpty()) {
            val message = when (mode) {
                GoogleDriveDialogMode.READ -> Message.Error.UnselectedFile
                GoogleDriveDialogMode.WRITE -> Message.Error.MissingFileName
            }
            report(message)
        } else {
            if (isOnGoogleDrive(fileName)) {
                fileNameToConfirm = fileName
                report(Message.ConfirmOperation)
            } else {
                setState(State.Finishing.Dismissing(fileName))
            }
        }
    }

    /**
     * Call this method to handle the confirmation of operation after [Message.ConfirmOperation]
     * message is shown.
     */
    fun onConfirmOperation() {
        Timber.d("On confirm operation")
        fileNameToConfirm?.let { fileName ->
            setState(State.Finishing.Dismissing(fileName))
        } ?: if (BuildConfig.DEBUG) {
            error("Missing confirmed file name!")
        }
    }

    /**
     * Call this method to handle the confirmation of deletion after [Message.ConfirmDeletion]
     * message is shown.
     */
    fun onConfirmDeletion() {
        Timber.d("On confirm deletion")
        execute {
            setState(State.Waiting(isCancelable = false))
            // Deletion
            setState(State.Waiting(isCancelable = true))
            tryToReadJsonFileNames(isUpdate = true)
            val fileName = targetFileNameForGoogleDrive()
            if (isOnGoogleDrive(fileName).not()) {
                setTargetFileName(null)
            }
        }
    }

    /**
     * Call this method to handle the 'Delete' click.
     */
    fun onDeleteClick() {
        Timber.d("On action click")
        report(Message.ConfirmDeletion)
    }

    /**
     * Call this method when user skips [State.Working.Mode.DELETION].
     */
    fun onSkipDeletionByUser() {
        Timber.d("On skip deletion by user")
        _manageableSelection.reset(0)
    }

    /**
     * Call this method when activity result is acquired, making sure that the results of the Google
     * authorization call for intent from [NavDirection.ToAuth.intent] are returned.
     *
     * @param resultCode the integer result code returned by the child activity through its
     * [Activity.setResult].
     */
    fun handleAuthResult(resultCode: Int) {
        Timber.d("Handle auth result: resultCode=$resultCode")
        if (resultCode == Activity.RESULT_OK) {
            execute {
                tryToReadJsonFileNames()
            }
        } else {
            setState(State.Finishing.Cancelling)
        }
    }

    private suspend fun tryToReadJsonFileNames(isUpdate: Boolean = false) {
        @Suppress("TooGenericExceptionCaught")
        try {
            if (isUpdate || googleDriveFileNames == null) {
                googleDriveFileNames = dataManager.readJsonFileNames()
                displayedFileNames = googleDriveFileNames?.map {
                    it.ensureNoSuffix(FILE_SUFFIX)
                }?.toMutableList()
            }
            Timber.d("googleDriveFileNames=$googleDriveFileNames")
            Timber.d("displayedFileNames=$displayedFileNames")
            displayedFileNames?.let { setFileNames(it) }
            setState(State.Working(State.Working.Mode.ENTERING_FILE_NAME))
        } catch (e: GoogleAuthException) {
            Timber.e(e)
            report(Message.Error.UnauthorizedUser)
            setState(State.Finishing.Cancelling)
        } catch (e: GoogleRecoverableAuthException) {
            Timber.e(e)
            e.intent?.let {
                navigateTo(NavDirection.ToAuth(it))
            } ?: run {
                report(Message.Error.Unknown)
                setState(State.Finishing.Cancelling)
            }
        } catch (e: Exception) {
            Timber.e(e)
            reportAbout(e)
            setState(State.Finishing.Cancelling)
        }
    }

    private fun targetFileNameForGoogleDrive(): String? {
        return _targetFileNameAsLiveData.value?.googleDriveFileName()
    }

    private fun isOnGoogleDrive(fileName: String?): Boolean {
        return googleDriveFileNames?.contains(fileName) == true
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

    private fun setFileNames(fileNames: List<String>) {
        Timber.d("Set file names: fileNames=$fileNames")
        _fileNamesAsLiveData.postValue(fileNames)
    }

    private fun setTargetFileName(fileName: String?) {
        Timber.d("Set target file name: fileName=$fileName")
        _targetFileNameAsLiveData.postValue(fileName)
    }

    private fun navigateTo(direction: NavDirection) {
        Timber.d("Navigate to: direction=$direction")
        _navigationChannel.offer(direction)
    }

    private fun report(message: Message) {
        Timber.d("Report: message=$message")
        _messageChannel.offer(message)
    }

    private fun String.fileSelection(): String {
        return this.trim().ensureNoSuffix(FILE_SUFFIX)
    }

    private fun String.googleDriveFileName(): String {
        return this.trim().ensureSuffix(FILE_SUFFIX)
    }

    private fun String.ensureNoSuffix(
        suffix: String
    ): String = if (this.endsWith(suffix) && this.length > suffix.length) {
        this.removeSuffix(suffix)
    } else {
        this
    }

    private fun String.ensureSuffix(
        suffix: String
    ): String = if (this.isEmpty() || this.endsWith(suffix)) {
        this
    } else {
        this + suffix
    }
}
