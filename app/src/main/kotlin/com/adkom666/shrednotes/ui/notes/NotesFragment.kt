package com.adkom666.shrednotes.ui.notes

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.adkom666.shrednotes.R
import com.adkom666.shrednotes.data.model.Note
import com.adkom666.shrednotes.databinding.FragmentNotesBinding
import com.adkom666.shrednotes.di.viewmodel.viewModel
import com.adkom666.shrednotes.ui.Filterable
import com.adkom666.shrednotes.ui.Searchable
import com.adkom666.shrednotes.util.FabDashboard
import com.adkom666.shrednotes.util.FirstItemDecoration
import com.adkom666.shrednotes.util.selection.Selection
import com.adkom666.shrednotes.util.toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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

        private const val REQUEST_CODE_ADD_NOTE = 666
        private const val REQUEST_CODE_UPDATE_NOTE = 667

        /**
         * Preferred way to create a fragment.
         *
         * @return new instance as a [Fragment].
         */
        fun newInstance(): Fragment = NotesFragment()
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
        get() = false

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

        binding.control.fabAddDel.setOnClickListener {
            if (model.selection.isActive) {
                context?.let { deleteSelectedNotesIfConfirmed(it) }
            } else {
                goToNoteScreen()
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

        val stateObserver = StateObserver()
        model.stateAsLiveData.observe(viewLifecycleOwner, stateObserver)
        val noteListObserver = NoteListObserver(adapter, binding.noteRecycler)
        model.notePagedListAsLiveData.observe(viewLifecycleOwner, noteListObserver)

        lifecycleScope.launchWhenStarted {
            model.messageChannel.consumeEach(::show)
        }
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
                if (resultCode == Activity.RESULT_OK) {
                    Timber.d("Note has been added")
                    toast(R.string.message_note_has_been_added)
                }
            REQUEST_CODE_UPDATE_NOTE ->
                if (resultCode == Activity.RESULT_OK) {
                    Timber.d("Note has been updated")
                    toast(R.string.message_note_has_been_updated)
                }
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

    override fun filter(onFilter: (filterEnabled: Boolean) -> Unit) {
        // Dummy
        onFilter(true)
    }

    private fun initNoteRecycler(): NotePagedListAdapter {
        val llm = LinearLayoutManager(context)
        llm.orientation = LinearLayoutManager.VERTICAL
        binding.noteRecycler.layoutManager = llm

        val marginTop = resources.getDimension(R.dimen.card_vertical_margin)
        binding.noteRecycler.addItemDecoration(FirstItemDecoration(marginTop.toInt()))

        val adapter = NotePagedListAdapter(model.selectableItems) { note ->
            Timber.d("Edit note: note=$note")
            goToNoteScreen(note)
        }

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

    private fun deleteSelectedNotesIfConfirmed(context: Context) {
        val messageString = getString(
            R.string.dialog_confirm_note_deletion_message,
            model.selectableItems.selectedItemCount
        )
        MaterialAlertDialogBuilder(context, R.style.AppTheme_MaterialAlertDialog_Confirmation)
            .setTitle(R.string.dialog_confirm_note_deletion_title)
            .setMessage(messageString)
            .setPositiveButton(R.string.button_title_ok) { _, _ ->
                deleteSelectedNotes()
            }
            .setNegativeButton(R.string.button_title_cancel, null)
            .create()
            .show()
    }

    private fun deleteSelectedNotes() {
        Timber.d("Start deleting the selected notes.")
        model.deleteSelectedNotes()
    }

    private fun goToNoteScreen(note: Note? = null) {
        context?.let {
            val intent = NoteActivity.newIntent(it, note)
            val requestCode = note?.let {
                REQUEST_CODE_UPDATE_NOTE
            } ?: REQUEST_CODE_ADD_NOTE
            startActivityForResult(intent, requestCode)
        }
    }

    private fun show(message: NotesViewModel.Message) = when (message) {
        is NotesViewModel.Message.Deleted -> {
            Timber.d("Deleted notes count is ${message.count}")
            val messageString = getString(R.string.message_deleted_notes, message.count)
            toast(messageString)
        }
    }

    private inner class StateObserver : Observer<NotesViewModel.State> {

        override fun onChanged(state: NotesViewModel.State?) {
            Timber.d("State is $state")
            when (state) {
                NotesViewModel.State.Waiting -> setWaiting(true)
                NotesViewModel.State.Ready -> setWaiting(false)
                is NotesViewModel.State.Error -> {
                    setWaiting(false)
                    handleError(state)
                }
            }
        }

        private fun setWaiting(active: Boolean) {
            return if (active) {
                binding.content.visibility = View.GONE
                binding.progressBar.visibility = View.VISIBLE
            } else {
                binding.content.visibility = View.VISIBLE
                binding.progressBar.visibility = View.GONE
            }
        }

        private fun handleError(error: NotesViewModel.State.Error) = when (error) {
            is NotesViewModel.State.Error.Clarified -> {
                val message = getString(
                    R.string.error_clarified,
                    error.message
                )
                toast(message)
            }
            NotesViewModel.State.Error.Unknown ->
                toast(R.string.error_unknown)
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
