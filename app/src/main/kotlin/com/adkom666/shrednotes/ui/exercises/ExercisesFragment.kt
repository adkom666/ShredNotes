package com.adkom666.shrednotes.ui.exercises

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
import com.adkom666.shrednotes.data.model.Exercise
import com.adkom666.shrednotes.databinding.FragmentExercisesBinding
import com.adkom666.shrednotes.di.viewmodel.viewModel
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
 * Exercises section sub screen.
 */
@ExperimentalCoroutinesApi
class ExercisesFragment :
    DaggerFragment(),
    Searchable {

    companion object {

        private const val REQUEST_CODE_ADD_EXERCISE = 666
        private const val REQUEST_CODE_UPDATE_EXERCISE = 667

        /**
         * Preferred way to create a fragment.
         *
         * @return new instance as a [Fragment].
         */
        fun newInstance(): Fragment = ExercisesFragment()
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

        val stateObserver = StateObserver()
        model.stateAsLiveData.observe(viewLifecycleOwner, stateObserver)
        val exerciseListObserver = ExerciseListObserver(adapter, binding.exercisesRecycler)
        model.exercisePagedListAsLiveData.observe(viewLifecycleOwner, exerciseListObserver)

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
                if (resultCode == Activity.RESULT_OK) {
                    Timber.d("Exercise has been added")
                    toast(R.string.message_exercise_has_been_added)
                }
            REQUEST_CODE_UPDATE_EXERCISE ->
                if (resultCode == Activity.RESULT_OK) {
                    Timber.d("Exercise has been updated")
                    toast(R.string.message_exercise_has_been_updated)
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
        model.subname = newText
        return true
    }

    private fun initExerciseRecycler(): ExercisePagedListAdapter {
        val llm = LinearLayoutManager(context)
        llm.orientation = LinearLayoutManager.VERTICAL
        binding.exercisesRecycler.layoutManager = llm

        val marginTop = resources.getDimension(R.dimen.card_vertical_margin)
        binding.exercisesRecycler.addItemDecoration(FirstItemDecoration(marginTop.toInt()))

        val adapter = ExercisePagedListAdapter(model.selectableExercises) { exercise ->
            Timber.d("Edit exercise: exercise=$exercise")
            goToExerciseScreen(exercise)
        }

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
                context?.let { deleteSelectedExercisesIfConfirmed(it) }
            } else {
                goToExerciseScreen()
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

    private fun deleteSelectedExercisesIfConfirmed(context: Context) {
        val messageString = getString(
            R.string.dialog_confirm_exercise_deletion_message,
            model.selectableExercises.selectedItemCount
        )
        MaterialAlertDialogBuilder(context, R.style.AppTheme_MaterialAlertDialog_Confirmation)
            .setTitle(R.string.dialog_confirm_exercise_deletion_title)
            .setMessage(messageString)
            .setPositiveButton(R.string.button_title_yes) { _, _ ->
                deleteSelectedExercises()
            }
            .setNegativeButton(R.string.button_title_no, null)
            .create()
            .show()
    }

    private fun deleteSelectedExercises() {
        Timber.d("Start deleting the selected exercises.")
        model.deleteSelectedExercises()
    }

    private fun goToExerciseScreen(exercise: Exercise? = null) {
        context?.let {
            val intent = ExerciseActivity.newIntent(it, exercise)
            val requestCode = exercise?.let {
                REQUEST_CODE_UPDATE_EXERCISE
            } ?: REQUEST_CODE_ADD_EXERCISE
            startActivityForResult(intent, requestCode)
        }
    }

    private fun show(message: ExercisesViewModel.Message) = when (message) {
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

    private inner class StateObserver : Observer<ExercisesViewModel.State> {

        override fun onChanged(state: ExercisesViewModel.State?) {
            Timber.d("State is $state")
            when (state) {
                ExercisesViewModel.State.Waiting -> setWaiting(true)
                ExercisesViewModel.State.Normal -> setWaiting(false)
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
