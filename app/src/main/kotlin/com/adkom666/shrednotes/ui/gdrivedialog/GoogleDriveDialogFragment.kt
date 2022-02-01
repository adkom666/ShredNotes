package com.adkom666.shrednotes.ui.gdrivedialog

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.adkom666.shrednotes.BuildConfig
import com.adkom666.shrednotes.R
import com.adkom666.shrednotes.databinding.DialogGoogleDriveBinding
import com.adkom666.shrednotes.di.viewmodel.viewModel
import com.adkom666.shrednotes.util.FirstItemDecoration
import com.adkom666.shrednotes.util.dialog.ConfirmationDialogFragment
import com.adkom666.shrednotes.util.forwardCursor
import com.adkom666.shrednotes.util.performIfConfirmationFoundByTag
import com.adkom666.shrednotes.util.setEnabledWithChildren
import com.adkom666.shrednotes.util.startLinearSmoothScrollToPosition
import com.adkom666.shrednotes.util.toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.android.support.DaggerDialogFragment
import kotlinx.coroutines.flow.collect
import timber.log.Timber
import javax.inject.Inject

typealias FileNameListener = (String) -> Unit

/**
 * Google Drive dialog.
 */
class GoogleDriveDialogFragment : DaggerDialogFragment() {

    companion object {

        private const val ARG_MODE = "${BuildConfig.APPLICATION_ID}.args.google_drive.mode"

        private const val TAG_CONFIRM_OPERATION =
            "${BuildConfig.APPLICATION_ID}.tags.confirm_operation"

        private const val TAG_CONFIRM_DELETION =
            "${BuildConfig.APPLICATION_ID}.tags.confirm_deletion"

        private const val SCROLL_DELAY_MILLIS = 228L

        /**
         * Preferred way to create a fragment.
         *
         * @param mode Google Drive dialog mode. See [GoogleDriveDialogMode].
         * @return new instance as [GoogleDriveDialogMode].
         */
        fun newInstance(mode: GoogleDriveDialogMode): GoogleDriveDialogFragment {
            val fragment = GoogleDriveDialogFragment()
            val arguments = Bundle()
            arguments.putSerializable(ARG_MODE, mode)
            fragment.arguments = arguments
            return fragment
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val mode: GoogleDriveDialogMode
        get() = requireNotNull(_mode)

    private val binding: DialogGoogleDriveBinding
        get() = requireNotNull(_binding)

    private val model: GoogleDriveViewModel
        get() = requireNotNull(_model)

    private val adapter: GoogleDriveFileListAdapter
        get() = requireNotNull(_adapter)

    private val alertDialog: AlertDialog
        get() = requireNotNull(_alertDialog)

    private val authLauncher: ActivityResultLauncher<Intent>
        get() = requireNotNull(_authLauncher)

    private var _mode: GoogleDriveDialogMode? = null
    private var _binding: DialogGoogleDriveBinding? = null
    private var _model: GoogleDriveViewModel? = null
    private var _adapter: GoogleDriveFileListAdapter? = null
    private var _alertDialog: AlertDialog? = null
    private var _authLauncher: ActivityResultLauncher<Intent>? = null

    private val actionModeCallback: ActionModeCallback = ActionModeCallback(
        { actionMode != null },
        { actionMode = null }
    )

    private val fileNameWatcher: FileNameWatcher = FileNameWatcher()
    private var actionMode: ActionMode? = null
    private var fileNameListener: FileNameListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        acquireActivityLaunchers()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val arguments = requireArguments()
        _mode = arguments.getSerializable(ARG_MODE) as GoogleDriveDialogMode
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogGoogleDriveBinding.inflate(layoutInflater)
        _model = viewModel(viewModelFactory)
        _adapter = initFileRecycler()
        initFileCard()
        restoreFragmentListeners()
        observeLiveData()
        listenFlows()
        val isFirstStart = savedInstanceState == null
        if (isFirstStart) {
            model.prepare(mode)
        }
        model.start()

        isCancelable = false
        val positiveButtonTitleResId = when (mode) {
            GoogleDriveDialogMode.READ -> R.string.button_title_read
            GoogleDriveDialogMode.WRITE -> R.string.button_title_write
        }
        val builder = MaterialAlertDialogBuilder(
            requireContext(),
            R.style.AppTheme_MaterialAlertDialog_GoogleDrive
        )
        builder
            .setTitle(R.string.dialog_google_drive_title)
            .setView(binding.root)
            .setPositiveButton(positiveButtonTitleResId, null)
            .setNeutralButton(R.string.button_title_cancel, null)
        _alertDialog = builder.create()
        alertDialog.setOnShowListener {
            observeLiveDataOnShow()
            val positiveButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                model.onPositiveButtonClick()
            }
        }
        return alertDialog
    }

    override fun onDetach() {
        super.onDetach()
        skipActivityLaunchers()
    }

    /**
     * Set [listener] to use on positive button click.
     *
     * @param listener the [FileNameListener] to use on positive button click.
     */
    fun setFileNameListener(listener: FileNameListener) {
        fileNameListener = listener
    }

    private fun acquireActivityLaunchers() {
        _authLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            Timber.d("On auth: result=$result")
            model.handleAuthResult(result.resultCode)
        }
    }

    private fun skipActivityLaunchers() {
        _authLauncher = null
    }

    private fun initFileRecycler(): GoogleDriveFileListAdapter {
        val llm = LinearLayoutManager(context)
        llm.orientation = LinearLayoutManager.VERTICAL
        binding.fileRecycler.layoutManager = llm
        val marginTop = resources.getDimension(R.dimen.card_vertical_margin)
        val decoration = FirstItemDecoration(marginTop.toInt())
        binding.fileRecycler.addItemDecoration(decoration)
        val adapter = GoogleDriveFileListAdapter(
            model.selectableFiles,
            model::onFileClick
        )
        binding.fileRecycler.adapter = adapter
        return adapter
    }

    private fun initFileCard() {
        binding.fileCard.isVisible = mode == GoogleDriveDialogMode.WRITE
        binding.fileNameEditText.addTextChangedListener(fileNameWatcher)
    }

    private fun restoreFragmentListeners() {
        childFragmentManager.performIfConfirmationFoundByTag(TAG_CONFIRM_OPERATION) {
            it.setPositiveListener()
        }
        childFragmentManager.performIfConfirmationFoundByTag(TAG_CONFIRM_DELETION) {
            it.setDeletingListener()
        }
    }

    private fun observeLiveData() {
        model.fileNamesAsLiveData.observe(this, FileNamesObserver())
        model.targetFileSelectionAsLiveData.observe(this, TargetFileSelectionObserver())
        model.targetFileNameAsLiveData.observe(this, TargetFileNameObserver())
    }

    private fun observeLiveDataOnShow() {
        model.stateAsLiveData.observe(this, StateObserver())
    }

    private fun listenFlows() {
        lifecycleScope.launchWhenResumed {
            model.navigationFlow.collect(::goToScreen)
        }
        lifecycleScope.launchWhenStarted {
            model.messageFlow.collect(::show)
        }
    }

    private fun goToScreen(direction: GoogleDriveViewModel.NavDirection) = when (direction) {
        is GoogleDriveViewModel.NavDirection.ToAuth ->
            authLauncher.launch(direction.intent)
    }

    private fun show(message: GoogleDriveViewModel.Message) = when (message) {
        GoogleDriveViewModel.Message.ConfirmOperation ->
            showConfirmOperationDialog()
        GoogleDriveViewModel.Message.ConfirmDeletion ->
            showConfirmDeletionDialog()
        is GoogleDriveViewModel.Message.Error ->
            showError(message)
    }

    private fun showConfirmOperationDialog() {
        val titleResId = when (mode) {
            GoogleDriveDialogMode.READ -> R.string.dialog_confirm_read_title
            GoogleDriveDialogMode.WRITE -> R.string.dialog_confirm_write_title
        }
        val messageResId = when (mode) {
            GoogleDriveDialogMode.READ -> R.string.dialog_confirm_read_message
            GoogleDriveDialogMode.WRITE -> R.string.dialog_confirm_write_message
        }
        val dialogFragment = ConfirmationDialogFragment.newInstance(
            titleResId,
            messageResId
        )
        dialogFragment.setPositiveListener()
        dialogFragment.show(childFragmentManager, TAG_CONFIRM_OPERATION)
    }

    private fun showConfirmDeletionDialog() {
        val dialogFragment = ConfirmationDialogFragment.newInstance(
            R.string.dialog_confirm_file_deletion_title,
            R.string.dialog_confirm_file_deletion_message,
            model.selectableFiles.selectedItemCount
        )
        dialogFragment.setDeletingListener()
        dialogFragment.show(childFragmentManager, TAG_CONFIRM_DELETION)
    }

    private fun showError(message: GoogleDriveViewModel.Message.Error) = when (message) {
        GoogleDriveViewModel.Message.Error.UnauthorizedUser ->
            toast(R.string.error_unauthorized_user)
        GoogleDriveViewModel.Message.Error.UnselectedFile ->
            toast(R.string.error_unselected_file)
        GoogleDriveViewModel.Message.Error.MissingFileName ->
            toast(R.string.error_missing_file_name)
        is GoogleDriveViewModel.Message.Error.Clarified -> {
            val messageString = getString(
                R.string.error_clarified,
                message.details
            )
            toast(messageString)
        }
        GoogleDriveViewModel.Message.Error.Unknown ->
            toast(R.string.error_unknown)
    }

    private fun ConfirmationDialogFragment.setPositiveListener() {
        setOnConfirmListener {
            model.onConfirmOperation()
        }
    }

    private fun ConfirmationDialogFragment.setDeletingListener() {
        setOnConfirmListener {
            model.onConfirmDeletion()
        }
    }

    private inner class ActionModeCallback(
        private val isDestroyEnabled: () -> Boolean,
        private val beforeDestroy: () -> Unit
    ) : ActionMode.Callback {

        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            mode?.menuInflater?.inflate(R.menu.google_drive_file_context, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            return if (item?.itemId == R.id.action_delete) {
                model.onDeleteClick()
                true
            } else {
                false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            Timber.d("onDestroyActionMode: lifecycle.currentState=${lifecycle.currentState}")
            if (isDestroyEnabled() && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                beforeDestroy()
                model.onSkipDeletionByUser()
            }
        }
    }

    private inner class FileNameWatcher : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        override fun afterTextChanged(s: Editable?) = model.onTargetFileNameChange(s?.toString())
    }

    private inner class StateObserver : Observer<GoogleDriveViewModel.State> {

        override fun onChanged(state: GoogleDriveViewModel.State) {
            Timber.d("State is $state")
            when (state) {
                is GoogleDriveViewModel.State.Waiting -> {
                    binding.progressBar.isVisible = true
                    binding.filesContainer.isVisible = false
                    setNonSelectionUi(isEnabled = false, isCancelable = state.isCancelable)
                }
                is GoogleDriveViewModel.State.Working -> {
                    binding.progressBar.isVisible = false
                    binding.filesContainer.isVisible = true
                    setWorkingMode(state.workingMode)
                }
                is GoogleDriveViewModel.State.Finishing.Dismissing -> {
                    fileNameListener?.invoke(state.fileName)
                    dialog?.dismiss()
                }
                GoogleDriveViewModel.State.Finishing.Cancelling ->
                    dialog?.cancel()
            }
        }

        private fun setWorkingMode(
            workingMode: GoogleDriveViewModel.State.Working.Mode
        ) = when (workingMode) {
            GoogleDriveViewModel.State.Working.Mode.ENTERING_FILE_NAME ->
                setEnteringFileNameMode()
            GoogleDriveViewModel.State.Working.Mode.DELETION ->
                setDeletionMode()
        }

        private fun setEnteringFileNameMode() {
            actionMode?.let { safeActionMode ->
                actionMode = null
                safeActionMode.finish()
            }
            adapter.selectionSource = GoogleDriveFileListAdapter.SelectionSource.PROVIDED_FILE_NAME
            setNonSelectionUi(isEnabled = true, isCancelable = true)
        }

        private fun setDeletionMode() {
            setNonSelectionUi(isEnabled = false, isCancelable = true)
            adapter.selectionSource = GoogleDriveFileListAdapter.SelectionSource.SELECTABLE_FILES
            if (actionMode == null) {
                actionMode = dialog?.window?.decorView?.startActionMode(actionModeCallback)
            }
        }

        private fun setNonSelectionUi(isEnabled: Boolean, isCancelable: Boolean) {
            binding.fileCard.setEnabledWithChildren(isEnabled)
            val neutralButton = alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL)
            val positiveButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE)
            neutralButton?.isEnabled = isCancelable
            positiveButton?.isEnabled = isEnabled
        }
    }

    private inner class FileNamesObserver : Observer<List<String>?> {

        override fun onChanged(fileNames: List<String>?) {
            Timber.d("File names are $fileNames")
            fileNames?.let { setFileNames(it) }
        }

        private fun setFileNames(fileNames: List<String>) {
            adapter.submitList(fileNames)
            if (fileNames.isNotEmpty()) {
                binding.fileRecycler.isVisible = true
                binding.noFilesTextView.isVisible = false
            } else {
                binding.fileRecycler.isVisible = false
                binding.noFilesTextView.isVisible = true
            }
        }
    }

    private inner class TargetFileSelectionObserver : Observer<String?> {

        private val handler: Handler = Handler(Looper.getMainLooper())

        override fun onChanged(fileName: String?) {
            Timber.d("Target file selection is $fileName")
            setTargetFileSelection(fileName ?: "")
        }

        private fun setTargetFileSelection(fileName: String) {
            val selectedItemPosition = adapter.select(fileName)
            selectedItemPosition?.let { position ->
                handler.postDelayed({
                    binding.fileRecycler.startLinearSmoothScrollToPosition(position)
                }, SCROLL_DELAY_MILLIS)
            }
        }
    }

    private inner class TargetFileNameObserver : Observer<String?> {

        override fun onChanged(fileName: String?) {
            Timber.d("Target file name is $fileName")
            setTargetFileName(fileName ?: "")
        }

        private fun setTargetFileName(fileName: String) {
            if (binding.fileNameEditText.text.toString() != fileName) {
                binding.fileNameEditText.removeTextChangedListener(fileNameWatcher)
                binding.fileNameEditText.setText(fileName)
                binding.fileNameEditText.forwardCursor()
                binding.fileNameEditText.addTextChangedListener(fileNameWatcher)
            }
        }
    }
}
