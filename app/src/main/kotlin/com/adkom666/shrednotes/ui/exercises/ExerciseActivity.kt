package com.adkom666.shrednotes.ui.exercises

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.adkom666.shrednotes.BuildConfig
import com.adkom666.shrednotes.R
import com.adkom666.shrednotes.data.model.Exercise
import com.adkom666.shrednotes.databinding.ActivityExerciseBinding
import com.adkom666.shrednotes.di.viewmodel.viewModel
import com.adkom666.shrednotes.util.forwardCursor
import com.adkom666.shrednotes.util.toast
import dagger.android.AndroidInjection
import timber.log.Timber
import javax.inject.Inject

/**
 * Exercise screen.
 */
class ExerciseActivity : AppCompatActivity() {

    companion object {

        private const val EXERCISE_EXTRA = "${BuildConfig.APPLICATION_ID}.extras.exercise"

        /**
         * Creating an intent to open the edit screen for a given [exercise], or to create a new
         * exercise if [exercise] is null.
         *
         * @param context [Context] to create an intent.
         * @param exercise [Exercise] for the edit.
         * @return intent to open the edit screen for a given [exercise], or to create a new
         * exercise if [exercise] is null.
         */
        fun newIntent(context: Context, exercise: Exercise?): Intent {
            val intent = Intent(context, ExerciseActivity::class.java)
            exercise?.let {
                intent.putExtra(EXERCISE_EXTRA, it)
            }
            return intent
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val binding: ActivityExerciseBinding
        get() = requireNotNull(_binding)

    private val model: ExerciseViewModel
        get() = requireNotNull(_model)

    private var _binding: ActivityExerciseBinding? = null
    private var _model: ExerciseViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        _binding = ActivityExerciseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        _model = viewModel(viewModelFactory)

        setupButtonListeners()

        model.stateAsLiveData.observe(this, StateObserver())

        if (savedInstanceState == null) {
            val exercise = intent?.extras?.getParcelable<Exercise>(EXERCISE_EXTRA)
            Timber.d("Initial exercise is $exercise")
            model.start(exercise)
        }
    }

    private fun setupButtonListeners() {

        binding.okButton.setOnClickListener {
            val exerciseName = binding.exerciseNameEditText.text.toString().trim()
            Timber.d("Save exercise under the exerciseName=$exerciseName")
            model.save(exerciseName)
        }

        binding.cancelButton.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private inner class StateObserver : Observer<ExerciseViewModel.State> {

        override fun onChanged(state: ExerciseViewModel.State?) {
            Timber.d("State is $state")
            when (state) {
                ExerciseViewModel.State.Waiting ->
                    setWaiting(true)
                is ExerciseViewModel.State.Ready -> {
                    setWaiting(false)
                    initExercise(state.exerciseName)
                }
                is ExerciseViewModel.State.Error -> {
                    setWaiting(false)
                    handleError(state)
                }
                ExerciseViewModel.State.Declined -> {
                    setResult(RESULT_CANCELED)
                    finish()
                }
                ExerciseViewModel.State.Done -> {
                    setResult(RESULT_OK)
                    finish()
                }
            }
        }

        private fun setWaiting(active: Boolean) {
            return if (active) {
                binding.exerciseCard.visibility = View.GONE
                binding.progressBar.visibility = View.VISIBLE
            } else {
                binding.exerciseCard.visibility = View.VISIBLE
                binding.progressBar.visibility = View.GONE
            }
        }

        private fun initExercise(name: String?) {
            name?.let {
                binding.exerciseNameEditText.setText(it)
                binding.exerciseNameEditText.forwardCursor()
            }
            binding.exerciseNameEditText.clearFocus()
        }

        private fun handleError(error: ExerciseViewModel.State.Error) = when (error) {
            ExerciseViewModel.State.Error.MissingExerciseName ->
                toast(R.string.error_missing_excercise_name)
            is ExerciseViewModel.State.Error.ExerciseAlreadyExists -> {
                val message = getString(
                    R.string.error_excercise_already_exists,
                    error.exerciseName
                )
                toast(message)
            }
            is ExerciseViewModel.State.Error.Clarified -> {
                val message = getString(
                    R.string.error_clarified,
                    error.message
                )
                toast(message)
            }
            ExerciseViewModel.State.Error.Unknown ->
                toast(R.string.error_unknown)
        }
    }
}
