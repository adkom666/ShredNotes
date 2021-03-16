package com.adkom666.shrednotes.ui.exercises

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.adkom666.shrednotes.BuildConfig
import com.adkom666.shrednotes.R
import com.adkom666.shrednotes.data.model.Exercise
import com.adkom666.shrednotes.databinding.ActivityExerciseBinding
import com.adkom666.shrednotes.di.viewmodel.viewModel
import com.adkom666.shrednotes.util.forwardCursor
import com.adkom666.shrednotes.util.toast
import dagger.android.AndroidInjection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.consumeEach
import timber.log.Timber
import javax.inject.Inject

/**
 * Exercise screen.
 */
@ExperimentalCoroutinesApi
class ExerciseActivity : AppCompatActivity() {

    companion object {

        private const val EXTRA_EXERCISE = "${BuildConfig.APPLICATION_ID}.extras.exercise"

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
                intent.putExtra(EXTRA_EXERCISE, it)
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

        lifecycleScope.launchWhenStarted {
            model.messageChannel.consumeEach(::show)
        }

        if (savedInstanceState == null) {
            val exercise = intent?.extras?.getParcelable<Exercise>(EXTRA_EXERCISE)
            Timber.d("Initial exercise is $exercise")
            model.prepare(exercise)
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

    private fun show(message: ExerciseViewModel.Message) = when (message) {
        is ExerciseViewModel.Message.Error -> showError(message)
    }

    private fun showError(message: ExerciseViewModel.Message.Error) = when (message) {
        ExerciseViewModel.Message.Error.MissingExerciseName ->
            toast(R.string.error_missing_excercise_name)
        is ExerciseViewModel.Message.Error.ExerciseAlreadyExists -> {
            val messageString = getString(
                R.string.error_excercise_already_exists,
                message.exerciseName
            )
            toast(messageString)
        }
        is ExerciseViewModel.Message.Error.Clarified -> {
            val messageString = getString(
                R.string.error_clarified,
                message.details
            )
            toast(messageString)
        }
        ExerciseViewModel.Message.Error.Unknown ->
            toast(R.string.error_unknown)
    }

    private inner class StateObserver : Observer<ExerciseViewModel.State> {

        override fun onChanged(state: ExerciseViewModel.State?) {
            Timber.d("State is $state")
            when (state) {
                ExerciseViewModel.State.Waiting ->
                    setWaiting(true)
                is ExerciseViewModel.State.Init -> {
                    initExercise(state.exerciseName)
                    model.ok()
                }
                ExerciseViewModel.State.Normal ->
                    setWaiting(false)
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
    }
}
