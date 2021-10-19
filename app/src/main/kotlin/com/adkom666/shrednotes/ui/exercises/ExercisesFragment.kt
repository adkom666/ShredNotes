package com.adkom666.shrednotes.ui.exercises

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
import com.adkom666.shrednotes.data.model.Exercise
import com.adkom666.shrednotes.databinding.FragmentExercisesBinding
import com.adkom666.shrednotes.di.viewmodel.viewModel
import com.adkom666.shrednotes.ui.Scrollable
import com.adkom666.shrednotes.ui.Searchable
import com.adkom666.shrednotes.util.dialog.ConfirmationDialogFragment
import com.adkom666.shrednotes.util.FabDashboard
import com.adkom666.shrednotes.util.FirstItemDecoration
import com.adkom666.shrednotes.util.performIfConfirmationFoundByTag
import com.adkom666.shrednotes.util.ScrollToNewItem
import com.adkom666.shrednotes.util.selection.Selection
import com.adkom666.shrednotes.util.toast
import dagger.android.support.DaggerFragment
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.consumeEach
import timber.log.Timber

/**
 * Exercises section sub screen.
 */
@ExperimentalCoroutinesApi
class ExercisesFragment :
    DaggerFragment(),
    Searchable,
    Scrollable {

    companion object {

        private const val TAG_CONFIRM_EXERCISES_DELETION =
            "${BuildConfig.APPLICATION_ID}.tags.confirm_exercises_deletion"

        private const val SCROLL_DELAY_MILLIS = 666L

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

    private val adapter: ExercisePagingDataAdapter
        get() = requireNotNull(_adapter)

    private val fabDashboard: FabDashboard
        get() = requireNotNull(_fabDashboard)

    private val addExerciseLauncher: ActivityResultLauncher<Intent>
        get() = requireNotNull(_addExerciseLauncher)

    private val updateExerciseLauncher: ActivityResultLauncher<Intent>
        get() = requireNotNull(_updateExerciseLauncher)

    private val scroller: ScrollToNewItem
        get() = requireNotNull(_scroller)

    private var _binding: FragmentExercisesBinding? = null
    private var _model: ExercisesViewModel? = null
    private var _adapter: ExercisePagingDataAdapter? = null
    private var _fabDashboard: FabDashboard? = null
    private var _addExerciseLauncher: ActivityResultLauncher<Intent>? = null
    private var _updateExerciseLauncher: ActivityResultLauncher<Intent>? = null
    private var _scroller: ScrollToNewItem? = null

    override var isSearchActive: Boolean
        get() = model.isSearchActive
        set(value) {
            model.isSearchActive = value
        }

    override val currentQuery: String?
        get() = model.subname

    override fun onAttach(context: Context) {
        super.onAttach(context)
        acquireActivityLaunchers()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExercisesBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _model = viewModel(viewModelFactory)
        _adapter = initExerciseRecycler()
        _fabDashboard = initFabDashboard(model.selection)
        _scroller = ScrollToNewItem(binding.exercisesRecycler)

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
        model.subname = newText
        return true
    }

    override fun unsearch() {
        model.isSearchActive = false
        model.subname = null
    }

    override fun scrollToBegin() {
        view?.handler?.postDelayed({
            binding.exercisesRecycler.scrollToPosition(0)
        }, SCROLL_DELAY_MILLIS)
    }

    private fun acquireActivityLaunchers() {
        _addExerciseLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            Timber.d("On add exercise: result=$result")
            model.onAddExerciseResult(result.resultCode == Activity.RESULT_OK)
        }
        _updateExerciseLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            Timber.d("On update exercise: result=$result")
            model.onUpdateExerciseResult(result.resultCode == Activity.RESULT_OK)
        }
    }

    private fun skipActivityLaunchers() {
        _addExerciseLauncher = null
        _updateExerciseLauncher = null
    }

    private fun initExerciseRecycler(): ExercisePagingDataAdapter {
        val llm = LinearLayoutManager(context)
        llm.orientation = LinearLayoutManager.VERTICAL
        binding.exercisesRecycler.layoutManager = llm
        val marginTop = resources.getDimension(R.dimen.card_vertical_margin)
        val decoration = FirstItemDecoration(marginTop.toInt())
        binding.exercisesRecycler.addItemDecoration(decoration)
        val adapter = ExercisePagingDataAdapter(model.selectableExercises, model::onExerciseClick)
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

    @SuppressLint("NotifyDataSetChanged")
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
        }
    }

    private fun restoreFragmentListeners() {
        childFragmentManager.performIfConfirmationFoundByTag(TAG_CONFIRM_EXERCISES_DELETION) {
            it.setDeletingListener()
        }
    }

    private fun observeLiveData() {
        val stateObserver = StateObserver()
        model.stateAsLiveData.observe(viewLifecycleOwner, stateObserver)
        val exerciseExpectationObserver = ExerciseExpectationObserver()
        model.exerciseExpectationAsLiveData.observe(viewLifecycleOwner, exerciseExpectationObserver)
        val exercisePagingDataObserver = ExercisePagingDataObserver(lifecycle, adapter)
        model.exercisePagingAsLiveData.observe(viewLifecycleOwner, exercisePagingDataObserver)
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
            addExerciseLauncher.launch(intent)
        }
        is ExercisesViewModel.NavDirection.ToUpdateExerciseScreen -> context?.let { safeContext ->
            val intent = ExerciseActivity.newIntent(safeContext, direction.exercise)
            updateExerciseLauncher.launch(intent)
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

    @SuppressLint("NotifyDataSetChanged")
    private fun process(signal: ExercisesViewModel.Signal) = when (signal) {
        ExercisesViewModel.Signal.SelectionChanged ->
            adapter.notifyDataSetChanged()
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

    private inner class ExerciseExpectationObserver : Observer<Boolean> {

        override fun onChanged(isWait: Boolean?) {
            Timber.d("Exercise expectation: isWait=$isWait")
            when (isWait) {
                true -> scroller.waitForNewItem()
                false -> scroller.dontWaitForNewItem()
                null -> Unit
            }
        }
    }

    private class ExercisePagingDataObserver(
        private val lifecycle: Lifecycle,
        private val adapter: ExercisePagingDataAdapter
    ) : Observer<PagingData<Exercise>> {

        override fun onChanged(exercisePagingData: PagingData<Exercise>) {
            adapter.submitData(lifecycle, exercisePagingData)
        }
    }
}
