package com.adkom666.shrednotes.ui.gdrivedialog

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.distinctUntilChanged
import androidx.lifecycle.map
import com.adkom666.shrednotes.BuildConfig
import com.adkom666.shrednotes.common.Id
import com.adkom666.shrednotes.data.DataManager
import com.adkom666.shrednotes.data.google.GoogleAuthException
import com.adkom666.shrednotes.data.google.GoogleDriveFile
import com.adkom666.shrednotes.data.google.GoogleRecoverableAuthException
import com.adkom666.shrednotes.di.module.DATA_INDEPENDENT_PREFERENCES
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
import javax.inject.Named

/**
 * Google Drive dialog model.
 *
 * @property dataManager [DataManager] to manage files with app info.
 * @property preferences project's [SharedPreferences] independent on data.
 */
class GoogleDriveViewModel @Inject constructor(
    private val dataManager: DataManager,
    @Named(DATA_INDEPENDENT_PREFERENCES)
    private val preferences: SharedPreferences
) : ExecutiveViewModel() {

    private companion object {
        private const val FILE_SUFFIX = ".json"
        private const val KEY_FILE_ID_LIST = "file_id_list"
        private const val KEY_TARGET_FILE_NAME = "google_drive.target_file_name"
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
             * Finishing work with the dialog providing the target file.
             *
             * @property googleDriveFile target file provided by the user.
             */
            data class Dismissing(val googleDriveFile: GoogleDriveFile) : Finishing()
        }
    }

    /**
     * Navigation direction.
     */
    sealed class NavDirection {

        /**
         * To authorization on Google.
         *
         * @property operation see [Operation].
         * @property intent [Intent] to use when calling an activity.
         */
        data class ToAuth(val operation: Operation, val intent: Intent) : NavDirection() {

            /**
             * Interrupted operation.
             */
            enum class Operation {
                READING_FILE_NAMES,
                DELETION
            }
        }
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
         * Message about the count of deleted files.
         *
         * @property fileCount count of deleted files.
         */
        data class Deletion(val fileCount: Int) : Message()

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

    private val _stateAsLiveData: MutableLiveData<State>
    private val _fileNamesAsLiveData: MutableLiveData<List<String>?>
    private val _targetFileNameAsLiveData: MutableLiveData<String?>
    private val _navigationChannel: Channel<NavDirection>
    private val _messageChannel: Channel<Message>
    private val _manageableSelection: ManageableSelection

    private var _mode: GoogleDriveDialogMode? = null

    private var googleDriveFiles: List<GoogleDriveFile>? = null
    private var displayedFileNames: MutableList<String>? = null
    private var fileToConfirm: GoogleDriveFile? = null

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
        _stateAsLiveData = MutableLiveData(State.Waiting())
        _fileNamesAsLiveData = MutableLiveData(null)
        _targetFileNameAsLiveData = MutableLiveData(preferences.getTargetFileName())
        _navigationChannel = Channel(1)
        _messageChannel = Channel(Channel.UNLIMITED)
        _manageableSelection = ManageableSelection()
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
        acceptTargetFileName(fileName)
    }

    /**
     * Call this method to handle the target file name change.
     */
    fun onTargetFileNameChange(fileName: String?) {
        Timber.d("On target file name change")
        acceptTargetFileName(fileName)
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
            fileFromGoogleDrive(fileName)?.let { googleDriveFile ->
                fileToConfirm = googleDriveFile
                report(Message.ConfirmOperation)
            } ?: run {
                val googleDriveFile = GoogleDriveFile(id = null, name = fileName)
                setState(State.Finishing.Dismissing(googleDriveFile))
            }
        }
    }

    /**
     * Call this method to handle the confirmation of operation after [Message.ConfirmOperation]
     * message is shown.
     */
    fun onConfirmOperation() {
        Timber.d("On confirm operation")
        fileToConfirm?.let { file ->
            setState(State.Finishing.Dismissing(file))
        } ?: if (BuildConfig.DEBUG) {
            error("Missing confirmed file!")
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
            tryToDeleteSelectedFiles()
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
        _manageableSelection.reset()
    }

    /**
     * Call this method when activity result is acquired, making sure that the results of the Google
     * authorization call for intent from [NavDirection.ToAuth.intent] are returned.
     *
     * @param operation operation interrupted for authorization.
     * @param resultCode the integer result code returned by the child activity through its
     * [Activity.setResult].
     */
    fun handleAuthResult(operation: NavDirection.ToAuth.Operation, resultCode: Int) {
        Timber.d("Handle auth result: operation=$operation, resultCode=$resultCode")
        if (resultCode == Activity.RESULT_OK) {
            execute {
                when (operation) {
                    NavDirection.ToAuth.Operation.READING_FILE_NAMES -> tryToReadJsonFileNames()
                    NavDirection.ToAuth.Operation.DELETION -> tryToDeleteSelectedFiles()
                }
            }
        } else {
            setState(State.Finishing.Cancelling)
        }
    }

    private suspend fun tryToReadJsonFileNames(isUpdate: Boolean = false) {
        @Suppress("TooGenericExceptionCaught")
        try {
            if (isUpdate || googleDriveFiles == null) {
                googleDriveFiles = dataManager.listGoogleDriveJsonFiles()
                displayedFileNames = googleDriveFiles?.map {
                    it.name.ensureNoSuffix(FILE_SUFFIX)
                }?.toMutableList()
            }
            Timber.d("googleDriveFiles=$googleDriveFiles")
            Timber.d("displayedFileNames=$displayedFileNames")
            displayedFileNames?.let { setFileNames(it) }
            setState(State.Working(State.Working.Mode.ENTERING_FILE_NAME))
            _manageableSelection.reset()
            skipTargetFileNameIfFileMissing()
        } catch (e: GoogleAuthException) {
            Timber.e(e)
            report(Message.Error.UnauthorizedUser)
            setState(State.Finishing.Cancelling)
        } catch (e: GoogleRecoverableAuthException) {
            Timber.e(e)
            e.intent?.let { intent ->
                navigateTo(
                    NavDirection.ToAuth(
                        operation = NavDirection.ToAuth.Operation.READING_FILE_NAMES,
                        intent = intent
                    )
                )
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

    private suspend fun tryToDeleteSelectedFiles() {
        @Suppress("TooGenericExceptionCaught")
        return try {
            val deletionCount = deleteSelectedFiles()
            report(Message.Deletion(deletionCount))
            if (deletionCount > 0) {
                setState(State.Waiting(isCancelable = true))
                tryToReadJsonFileNames(isUpdate = true)
            } else {
                setState(State.Working(State.Working.Mode.ENTERING_FILE_NAME))
            }
        } catch (e: GoogleAuthException) {
            Timber.e(e)
            report(Message.Error.UnauthorizedUser)
            setState(State.Working(State.Working.Mode.DELETION))
        } catch (e: GoogleRecoverableAuthException) {
            Timber.e(e)
            e.intent?.let { intent ->
                navigateTo(
                    NavDirection.ToAuth(
                        operation = NavDirection.ToAuth.Operation.DELETION,
                        intent = intent
                    )
                )
            } ?: run {
                report(Message.Error.Unknown)
                setState(State.Working(State.Working.Mode.DELETION))
            }
        } catch (e: Exception) {
            Timber.e(e)
            reportAbout(e)
            setState(State.Working(State.Working.Mode.DELETION))
        }
    }

    private suspend fun deleteSelectedFiles(): Int = when (
        val selectionState = _manageableSelection.state
    ) {
        is ManageableSelection.State.Active.Inclusive ->
            googleDriveFiles?.let { files ->
                val selectedFileIndexList = selectionState
                    .selectedItemIdSet
                    .map(Id::toInt)
                    .toList()
                val fileIdList = selectedFileIndexList
                    .filter { files[it].id != null }
                    .map { files[it].id ?: if (BuildConfig.DEBUG) error("Id is null!") else "" }
                dataManager.deleteFilesFromGoogleDrive(fileIdList, KEY_FILE_ID_LIST)
                selectedFileIndexList.size
            } ?: 0
        is ManageableSelection.State.Active.Exclusive -> 0
        ManageableSelection.State.Inactive -> 0
    }

    private fun skipTargetFileNameIfFileMissing() {
        val fileName = targetFileNameForGoogleDrive()
        if (isOnGoogleDrive(fileName).not()) {
            acceptTargetFileName(null)
        }
    }

    private fun targetFileNameForGoogleDrive(): String? {
        return _targetFileNameAsLiveData.value?.googleDriveFileName()
    }

    private fun isOnGoogleDrive(fileName: String?): Boolean {
        return googleDriveFiles?.find { it.name.equals(fileName, ignoreCase = true) } != null
    }

    private fun fileFromGoogleDrive(fileName: String?): GoogleDriveFile? {
        return googleDriveFiles?.find { it.name.equals(fileName, ignoreCase = true) }
    }

    private fun acceptTargetFileName(fileName: String?) {
        preferences.edit {
            putTargetFileName(fileName)
        }
        setTargetFileName(fileName)
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
        _navigationChannel.trySend(direction)
    }

    private fun report(message: Message) {
        Timber.d("Report: message=$message")
        _messageChannel.trySend(message)
    }

    private fun SharedPreferences.getTargetFileName(): String? {
        return getString(KEY_TARGET_FILE_NAME, null)
    }

    private fun SharedPreferences.Editor.putTargetFileName(fileName: String?) {
        putString(KEY_TARGET_FILE_NAME, fileName)
    }

    private fun String.fileSelection(): String {
        return this.trim().ensureNoSuffix(FILE_SUFFIX)
    }

    private fun String.googleDriveFileName(): String {
        return this.trim().ensureSuffix(FILE_SUFFIX)
    }

    private fun String.ensureNoSuffix(
        suffix: String
    ): String = if (this.endsWith(suffix, ignoreCase = true) && this.length > suffix.length) {
        this.substring(0, this.length - suffix.length)
    } else {
        this
    }

    private fun String.ensureSuffix(
        suffix: String
    ): String = if (this.isEmpty() || this.endsWith(suffix, ignoreCase = true)) {
        this
    } else {
        this + suffix
    }
}
