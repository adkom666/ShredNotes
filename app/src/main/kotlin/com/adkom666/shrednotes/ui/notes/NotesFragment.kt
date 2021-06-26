package com.adkom666.shrednotes.ui.notes

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.adkom666.shrednotes.BuildConfig
import com.adkom666.shrednotes.R
import com.adkom666.shrednotes.data.model.Note
import com.adkom666.shrednotes.data.model.NoteFilter
import com.adkom666.shrednotes.databinding.FragmentNotesBinding
import com.adkom666.shrednotes.di.viewmodel.viewModel
import com.adkom666.shrednotes.ui.Filterable
import com.adkom666.shrednotes.ui.OnFilterEnablingChangedListener
import com.adkom666.shrednotes.ui.Searchable
import com.adkom666.shrednotes.util.ConfirmationDialogFragment
import com.adkom666.shrednotes.util.FabDashboard
import com.adkom666.shrednotes.util.FirstItemDecoration
import com.adkom666.shrednotes.util.performIfConfirmationFoundByTag
import com.adkom666.shrednotes.util.performIfFoundByTag
import com.adkom666.shrednotes.util.selection.Selection
import com.adkom666.shrednotes.util.toast
import dagger.android.support.DaggerFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.consumeEach
import timber.log.Timber
import javax.inject.Inject

/**
 * Notes section sub screen.
 */
@ExperimentalCoroutinesApi
class NotesFragment :
    DaggerFragment(),
    Searchable,
    Filterable {

    companion object {

        private const val REQUEST_CODE_ADD_NOTE = 6_666
        private const val REQUEST_CODE_UPDATE_NOTE = 6_667

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

    private val adapter: NotePagedListAdapter
        get() = requireNotNull(_adapter)

    private val fabDashboard: FabDashboard
        get() = requireNotNull(_fabDashboard)

    private var _binding: FragmentNotesBinding? = null
    private var _model: NotesViewModel? = null
    private var _adapter: NotePagedListAdapter? = null
    private var _fabDashboard: FabDashboard? = null

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotesBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        _model = viewModel(viewModelFactory)
        _adapter = initNoteRecycler()
        _fabDashboard = initFabDashboard(model.selection)

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Timber.d("onActivityResult: requestCode=$requestCode, resultCode=$resultCode")
        when (requestCode) {
            REQUEST_CODE_ADD_NOTE ->
                model.onAddNoteResult(resultCode == Activity.RESULT_OK)
            REQUEST_CODE_UPDATE_NOTE ->
                model.onUpdateNoteResult(resultCode == Activity.RESULT_OK)
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
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

    private fun initNoteRecycler(): NotePagedListAdapter {
        val llm = LinearLayoutManager(context)
        llm.orientation = LinearLayoutManager.VERTICAL
        binding.noteRecycler.layoutManager = llm
        val marginTop = resources.getDimension(R.dimen.card_vertical_margin)
        binding.noteRecycler.addItemDecoration(FirstItemDecoration(marginTop.toInt()))
        val adapter = NotePagedListAdapter(model.selectableNotes, model::onNoteClick)
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
            adapter.notifyDataSetChanged()
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
        val noteListObserver = NoteListObserver(adapter, binding.noteRecycler)
        model.notePagedListAsLiveData.observe(viewLifecycleOwner, noteListObserver)
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
            startActivityForResult(intent, REQUEST_CODE_ADD_NOTE)
        }
        is NotesViewModel.NavDirection.ToUpdateNoteScreen -> context?.let { safeContext ->
            val intent = NoteActivity.newIntent(safeContext, direction.note)
            startActivityForResult(intent, REQUEST_CODE_UPDATE_NOTE)
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

    private fun process(signal: NotesViewModel.Signal) = when (signal) {
        NotesViewModel.Signal.FilterEnablingChanged ->
            onFilterEnablingChangedListener?.invoke()
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

    private class NoteListObserver(
        private val adapter: NotePagedListAdapter,
        private val recycler: RecyclerView
    ) : Observer<PagedList<Note>> {

        private val handler: Handler = Handler(Looper.getMainLooper())

        override fun onChanged(notePagedList: PagedList<Note>?) {
            adapter.submitList(notePagedList) {
                adapter.notifyDataSetChanged()
                handler.post {
                    recycler.smoothScrollToPosition(0)
                }
            }
        }
    }
}
