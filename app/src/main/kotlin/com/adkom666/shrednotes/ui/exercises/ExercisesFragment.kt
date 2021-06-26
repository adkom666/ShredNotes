package com.adkom666.shrednotes.ui.exercises

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
import com.adkom666.shrednotes.data.model.Exercise
import com.adkom666.shrednotes.databinding.FragmentExercisesBinding
import com.adkom666.shrednotes.di.viewmodel.viewModel
import com.adkom666.shrednotes.ui.Searchable
import com.adkom666.shrednotes.util.ConfirmationDialogFragment
import com.adkom666.shrednotes.util.FabDashboard
import com.adkom666.shrednotes.util.FirstItemDecoration
import com.adkom666.shrednotes.util.performIfConfirmationFoundByTag
import com.adkom666.shrednotes.util.selection.Selection
import com.adkom666.shrednotes.util.toast
import dagger.android.support.DaggerFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.consumeEach
import timber.log.Timber
import javax.inject.Inject

/**
 * Exercises section sub screen.
 */
@ExperimentalCoroutinesApi
class ExercisesFragment :
    DaggerFragment(),
    Searchable {

    companion object {

        private const val REQUEST_CODE_ADD_EXERCISE = 666
        private const val REQUEST_CODE_UPDATE_EXERCISE = 667

        private const val TAG_CONFIRM_EXERCISES_DELETION =
            "${BuildConfig.APPLICATION_ID}.tags.confirm_exercises_deletion"

        /**
         * Preferred way to create a fragment.
         *
         * @return new instance as [ExercisesFragment].
         */
        fun newInstance(): ExercisesFragment = ExercisesFragment()
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val binding: FragmentExercisesBinding
        get() = requireNotNull(_binding)

    private val model: ExercisesViewModel
        get() = requireNotNull(_model)

    private val adapter: ExercisePagedListAdapter
        get() = requireNotNull(_adapter)

    private val fabDashboard: FabDashboard
        get() = requireNotNull(_fabDashboard)

    private var _binding: FragmentExercisesBinding? = null
    private var _model: ExercisesViewModel? = null
    private var _adapter: ExercisePagedListAdapter? = null
    private var _fabDashboard: FabDashboard? = null

    override var isSearchActive: Boolean
        get() = model.isSearchActive
        set(value) {
            model.isSearchActive = value
        }

    override val currentQuery: String?
        get() = model.subname

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExercisesBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        _model = viewModel(viewModelFactory)
        _adapter = initExerciseRecycler()
        _fabDashboard = initFabDashboard(model.selection)

        setupFabListeners()
        restoreFragmentListeners()

        val stateObserver = StateObserver()
        model.stateAsLiveData.observe(viewLifecycleOwner, stateObserver)
        val exerciseListObserver = ExerciseListObserver(adapter, binding.exercisesRecycler)
        model.exercisePagedListAsLiveData.observe(viewLifecycleOwner, exerciseListObserver)

        lifecycleScope.launchWhenResumed {
            model.navigationChannel.consumeEach(::goToScreen)
        }

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
            REQUEST_CODE_ADD_EXERCISE ->
                model.onAddExerciseResult(resultCode == Activity.RESULT_OK)
            REQUEST_CODE_UPDATE_EXERCISE ->
                model.onUpdateExerciseResult(resultCode == Activity.RESULT_OK)
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun search(query: String?): Boolean {
        Timber.d("search: query=$query")
        return true
    }

    override fun preview(newText: String?): Boolean {
        Timber.d("preview: newText=$newText")
        model.subname = newText
        return true
    }

    private fun initExerciseRecycler(): ExercisePagedListAdapter {
        val llm = LinearLayoutManager(context)
        llm.orientation = LinearLayoutManager.VERTICAL
        binding.exercisesRecycler.layoutManager = llm
        val marginTop = resources.getDimension(R.dimen.card_vertical_margin)
        binding.exercisesRecycler.addItemDecoration(FirstItemDecoration(marginTop.toInt()))
        val adapter = ExercisePagedListAdapter(model.selectableExercises, model::onExerciseClick)
        binding.exercisesRecycler.adapter = adapter
        return adapter
    }

    private fun initFabDashboard(selection: Selection): FabDashboard = FabDashboard(
        selection = selection,
        recycler = binding.exercisesRecycler,
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
                model.requestAssociatedNoteCount()
            } else {
                model.addExercise()
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
        childFragmentManager.performIfConfirmationFoundByTag(TAG_CONFIRM_EXERCISES_DELETION) {
            it.setDeletingListener()
        }
    }

    private fun deleteSelectedExercisesIfConfirmed(associatedNoteCount: Int) {
        val dialogFragment = ConfirmationDialogFragment.newInstance(
            R.string.dialog_confirm_exercise_deletion_title,
            R.string.dialog_confirm_exercise_deletion_message,
            model.selectableExercises.selectedItemCount,
            associatedNoteCount
        )
        dialogFragment.setDeletingListener()
        dialogFragment.show(childFragmentManager, TAG_CONFIRM_EXERCISES_DELETION)
    }

    private fun goToScreen(direction: ExercisesViewModel.NavDirection) = when (direction) {
        ExercisesViewModel.NavDirection.ToAddExerciseScreen -> context?.let { safeContext ->
            val intent = ExerciseActivity.newIntent(safeContext)
            startActivityForResult(intent, REQUEST_CODE_ADD_EXERCISE)
        }
        is ExercisesViewModel.NavDirection.ToUpdateExerciseScreen -> context?.let { safeContext ->
            val intent = ExerciseActivity.newIntent(safeContext, direction.exercise)
            startActivityForResult(intent, REQUEST_CODE_UPDATE_EXERCISE)
        }
    }

    private fun show(message: ExercisesViewModel.Message) = when (message) {
        ExercisesViewModel.Message.Addition ->
            toast(R.string.message_exercise_has_been_added)
        ExercisesViewModel.Message.Update ->
            toast(R.string.message_exercise_has_been_updated)
        is ExercisesViewModel.Message.AssociatedNoteCount ->
            deleteSelectedExercisesIfConfirmed(message.noteCount)
        is ExercisesViewModel.Message.Deletion -> {
            Timber.d("Deleted exercises count is ${message.count}")
            val messageString = getString(R.string.message_deleted_exercises, message.count)
            toast(messageString)
        }
        is ExercisesViewModel.Message.Error ->
            showError(message)
    }

    private fun showError(message: ExercisesViewModel.Message.Error) = when (message) {
        is ExercisesViewModel.Message.Error.Clarified -> {
            val messageString = getString(
                R.string.error_clarified,
                message.details
            )
            toast(messageString)
        }
        ExercisesViewModel.Message.Error.Unknown ->
            toast(R.string.error_unknown)
    }

    private fun ConfirmationDialogFragment.setDeletingListener() {
        setOnConfirmListener {
            model.deleteSelectedExercises()
        }
    }

    private inner class StateObserver : Observer<ExercisesViewModel.State> {

        override fun onChanged(state: ExercisesViewModel.State?) {
            Timber.d("State is $state")
            when (state) {
                ExercisesViewModel.State.Waiting -> setWaiting()
                ExercisesViewModel.State.Working -> setWorking()
            }
        }

        private fun setWaiting() = setProgressActive(true)
        private fun setWorking() = setProgressActive(false)

        private fun setProgressActive(isActive: Boolean) {
            binding.progressBar.isVisible = isActive
            binding.content.isVisible = isActive.not()
        }
    }

    private class ExerciseListObserver(
        private val adapter: ExercisePagedListAdapter,
        private val recycler: RecyclerView
    ) : Observer<PagedList<Exercise>> {

        private val handler: Handler = Handler(Looper.getMainLooper())

        override fun onChanged(exercisePagedList: PagedList<Exercise>?) {
            adapter.submitList(exercisePagedList) {
                adapter.notifyDataSetChanged()
                handler.post {
                    recycler.smoothScrollToPosition(0)
                }
            }
        }
    }
}
