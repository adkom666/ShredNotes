package com.adkom666.shrednotes.ui.notes

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagingData
import androidx.recyclerview.widget.LinearLayoutManager
import com.adkom666.shrednotes.BuildConfig
import com.adkom666.shrednotes.R
import com.adkom666.shrednotes.data.model.Note
import com.adkom666.shrednotes.data.model.NoteFilter
import com.adkom666.shrednotes.databinding.FragmentNotesBinding
import com.adkom666.shrednotes.di.viewmodel.viewModel
import com.adkom666.shrednotes.ui.Filterable
import com.adkom666.shrednotes.ui.OnFilterEnablingChangedListener
import com.adkom666.shrednotes.ui.Searchable
import com.adkom666.shrednotes.util.dialog.ConfirmationDialogFragment
import com.adkom666.shrednotes.util.FabDashboard
import com.adkom666.shrednotes.util.FirstItemDecoration
import com.adkom666.shrednotes.util.performIfConfirmationFoundByTag
import com.adkom666.shrednotes.util.performIfFoundByTag
import com.adkom666.shrednotes.util.ScrollToNewItem
import com.adkom666.shrednotes.util.selection.Selection
import com.adkom666.shrednotes.util.toast
import dagger.android.support.DaggerFragment
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.consumeEach
import timber.log.Timber

/**
 * Notes section sub screen.
 */
@ExperimentalCoroutinesApi
class NotesFragment :
    DaggerFragment(),
    Searchable,
    Filterable {

    companion object {

        private const val TAG_CONFIRM_NOTES_DELETION =
            "${BuildConfig.APPLICATION_ID}.tags.confirm_notes_deletion"

        private const val TAG_FILTER_NOTES =
            "${BuildConfig.APPLICATION_ID}.tags.filter_notes"

        /**
         * Preferred way to create a fragment.
         *
         * @return new instance as [NotesFragment].
         */
        fun newInstance(): NotesFragment = NotesFragment()
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val binding: FragmentNotesBinding
        get() = requireNotNull(_binding)

    private val model: NotesViewModel
        get() = requireNotNull(_model)

    private val adapter: NotePagingDataAdapter
        get() = requireNotNull(_adapter)

    private val fabDashboard: FabDashboard
        get() = requireNotNull(_fabDashboard)

    private val addNoteLauncher: ActivityResultLauncher<Intent>
        get() = requireNotNull(_addNoteLauncher)

    private val updateNoteLauncher: ActivityResultLauncher<Intent>
        get() = requireNotNull(_updateNoteLauncher)

    private val scroller: ScrollToNewItem
        get() = requireNotNull(_scroller)

    private var _binding: FragmentNotesBinding? = null
    private var _model: NotesViewModel? = null
    private var _adapter: NotePagingDataAdapter? = null
    private var _fabDashboard: FabDashboard? = null
    private var _addNoteLauncher: ActivityResultLauncher<Intent>? = null
    private var _updateNoteLauncher: ActivityResultLauncher<Intent>? = null
    private var _scroller: ScrollToNewItem? = null

    override var isSearchActive: Boolean
        get() = model.isSearchActive
        set(value) {
            model.isSearchActive = value
        }

    override val currentQuery: String?
        get() = model.exerciseSubname

    override val isFilterEnabled: Boolean
        get() = model.isFilterEnabled

    override var onFilterEnablingChangedListener: OnFilterEnablingChangedListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        acquireActivityLaunchers()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotesBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _model = viewModel(viewModelFactory)
        _adapter = initNoteRecycler()
        _fabDashboard = initFabDashboard(model.selection)
        _scroller = ScrollToNewItem(binding.noteRecycler)

        setupFabListeners()
        restoreFragmentListeners()
        observeLiveData()
        listenChannels()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fabDashboard.dispose()
        _binding = null
    }

    override fun onDetach() {
        super.onDetach()
        skipActivityLaunchers()
    }

    override fun search(query: String?): Boolean {
        Timber.d("search: query=$query")
        return true
    }

    override fun preview(newText: String?): Boolean {
        Timber.d("preview: newText=$newText")
        model.exerciseSubname = newText
        return true
    }

    override fun filter() {
        model.requestFilter()
    }

    private fun acquireActivityLaunchers() {
        _addNoteLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            Timber.d("On add note: result=$result")
            model.onAddNoteResult(result.resultCode == Activity.RESULT_OK)
        }
        _updateNoteLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            Timber.d("On update note: result=$result")
            model.onUpdateNoteResult(result.resultCode == Activity.RESULT_OK)
        }
    }

    private fun skipActivityLaunchers() {
        _addNoteLauncher = null
        _updateNoteLauncher = null
    }

    private fun initNoteRecycler(): NotePagingDataAdapter {
        val llm = LinearLayoutManager(context)
        llm.orientation = LinearLayoutManager.VERTICAL
        binding.noteRecycler.layoutManager = llm
        val marginTop = resources.getDimension(R.dimen.card_vertical_margin)
        val decoration = FirstItemDecoration(marginTop.toInt())
        binding.noteRecycler.addItemDecoration(decoration)
        val adapter = NotePagingDataAdapter(model.selectableNotes, model::onNoteClick)
        binding.noteRecycler.adapter = adapter
        return adapter
    }

    private fun initFabDashboard(selection: Selection): FabDashboard = FabDashboard(
        selection = selection,
        recycler = binding.noteRecycler,
        parentFab = binding.control.fabAddDel,
        bottomChildFab = binding.control.fabSelectAll,
        topChildFab = binding.control.fabCancel,
        parentFabConstructiveImageRes = R.drawable.ic_plus,
        parentFabDestructiveImageRes = R.drawable.ic_minus,
        childFabMargin = resources.getDimension(R.dimen.child_fab_margin),
        bottomChildFabMargin = resources.getDimension(R.dimen.bottom_child_fab_margin)
    )

    @SuppressLint("NotifyDataSetChanged")
    private fun setupFabListeners() {
        binding.control.fabAddDel.setOnClickListener {
            if (model.selection.isActive) {
                deleteSelectedNotesIfConfirmed()
            } else {
                model.addNote()
            }
        }
        binding.control.fabSelectAll.setOnClickListener {
            model.selectionDashboard.selectAll()
            adapter.notifyDataSetChanged()
        }
        binding.control.fabCancel.setOnClickListener {
            model.selectionDashboard.deselectAll()
        }
    }

    private fun restoreFragmentListeners() {
        childFragmentManager.performIfConfirmationFoundByTag(TAG_CONFIRM_NOTES_DELETION) {
            it.setDeletingListener()
        }
        childFragmentManager.performIfFoundByTag<NoteFilterDialogFragment>(TAG_FILTER_NOTES) {
            it.setListeners()
        }
    }

    private fun observeLiveData() {
        val stateObserver = StateObserver()
        model.stateAsLiveData.observe(viewLifecycleOwner, stateObserver)
        val noteExpectationObserver = NoteExpectationObserver()
        model.noteExpectationAsLiveData.observe(viewLifecycleOwner, noteExpectationObserver)
        val notePagingDataObserver = NotePagingDataObserver(lifecycle, adapter)
        model.notePagingAsLiveData.observe(viewLifecycleOwner, notePagingDataObserver)
    }

    private fun listenChannels() {
        lifecycleScope.launchWhenResumed {
            model.navigationChannel.consumeEach(::goToScreen)
        }
        lifecycleScope.launchWhenStarted {
            model.messageChannel.consumeEach(::show)
        }
        lifecycleScope.launchWhenStarted {
            model.signalChannel.consumeEach(::process)
        }
    }

    private fun deleteSelectedNotesIfConfirmed() {
        val dialogFragment = ConfirmationDialogFragment.newInstance(
            R.string.dialog_confirm_note_deletion_title,
            R.string.dialog_confirm_note_deletion_message,
            model.selectableNotes.selectedItemCount
        )
        dialogFragment.setDeletingListener()
        dialogFragment.show(childFragmentManager, TAG_CONFIRM_NOTES_DELETION)
    }

    private fun goToScreen(direction: NotesViewModel.NavDirection) = when (direction) {
        NotesViewModel.NavDirection.ToAddNoteScreen -> context?.let { safeContext ->
            val intent = NoteActivity.newIntent(safeContext)
            addNoteLauncher.launch(intent)
        }
        is NotesViewModel.NavDirection.ToUpdateNoteScreen -> context?.let { safeContext ->
            val intent = NoteActivity.newIntent(safeContext, direction.note)
            updateNoteLauncher.launch(intent)
        }
        is NotesViewModel.NavDirection.ToConfigFilterScreen ->
            showFilterDialog(direction.filter, direction.isFilterEnabled)
    }

    private fun show(message: NotesViewModel.Message) = when (message) {
        NotesViewModel.Message.Addition ->
            toast(R.string.message_note_has_been_added)
        NotesViewModel.Message.Update ->
            toast(R.string.message_note_has_been_updated)
        is NotesViewModel.Message.Deletion -> {
            Timber.d("Deleted notes count is ${message.count}")
            val messageString = getString(R.string.message_deleted_notes, message.count)
            toast(messageString)
        }
        NotesViewModel.Message.FilterUndefined ->
            toast(R.string.message_filter_undefined)
        is NotesViewModel.Message.Error ->
            showError(message)
    }

    private fun showError(message: NotesViewModel.Message.Error) = when (message) {
        is NotesViewModel.Message.Error.Clarified -> {
            val messageString = getString(
                R.string.error_clarified,
                message.details
            )
            toast(messageString)
        }
        NotesViewModel.Message.Error.Unknown ->
            toast(R.string.error_unknown)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun process(signal: NotesViewModel.Signal) = when (signal) {
        NotesViewModel.Signal.FilterEnablingChanged ->
            onFilterEnablingChangedListener?.invoke()
        NotesViewModel.Signal.SelectionChanged ->
            adapter.notifyDataSetChanged()
    }

    private fun ConfirmationDialogFragment.setDeletingListener() {
        setOnConfirmListener {
            model.deleteSelectedNotes()
        }
    }

    private fun showFilterDialog(filter: NoteFilter, isFilterEnabled: Boolean) {
        val dialogFragment = NoteFilterDialogFragment.newInstance(
            filter,
            isFilterEnabled
        )
        dialogFragment.setListeners()
        dialogFragment.show(childFragmentManager, TAG_FILTER_NOTES)
    }

    private fun NoteFilterDialogFragment.setListeners() {
        setOnApplyFilterListener { filter ->
            Timber.d("onApplyFilterListener: filter=$filter")
            model.onConfigFilterResult(NotesViewModel.ConfigFilterStatus.APPLY, filter)
        }
        setOnDisableFilterListener { filter ->
            Timber.d("onDisableFilterListener: filter=$filter")
            model.onConfigFilterResult(NotesViewModel.ConfigFilterStatus.DISABLE, filter)
        }
        setOnCancelFilterListener { filter ->
            Timber.d("onCancelFilterListener: filter=$filter")
            model.onConfigFilterResult(NotesViewModel.ConfigFilterStatus.CANCEL, filter)
        }
    }

    private inner class StateObserver : Observer<NotesViewModel.State> {

        override fun onChanged(state: NotesViewModel.State?) {
            Timber.d("State is $state")
            when (state) {
                NotesViewModel.State.Waiting -> setWaiting()
                NotesViewModel.State.Working -> setWorking()
            }
        }

        private fun setWaiting() = setProgressActive(true)
        private fun setWorking() = setProgressActive(false)

        private fun setProgressActive(isActive: Boolean) {
            binding.progressBar.isVisible = isActive
            binding.content.isVisible = isActive.not()
        }
    }

    private inner class NoteExpectationObserver : Observer<Boolean> {

        override fun onChanged(isWait: Boolean?) {
            Timber.d("Note expectation: isWait=$isWait")
            when (isWait) {
                true -> scroller.waitForNewItem()
                false -> scroller.dontWaitForNewItem()
                null -> Unit
            }
        }
    }

    private class NotePagingDataObserver(
        private val lifecycle: Lifecycle,
        private val adapter: NotePagingDataAdapter
    ) : Observer<PagingData<Note>> {

        override fun onChanged(notePagingData: PagingData<Note>) {
            adapter.submitData(lifecycle, notePagingData)
        }
    }
}
