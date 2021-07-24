package com.adkom666.shrednotes.ui.exercises

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
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
import kotlinx.coroutines.launch
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
        fun newIntent(context: Context, exercise: Exercise? = null): Intent {
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
        observeLiveData()
        listenChannels()

        val isFirstStart = savedInstanceState == null
        if (isFirstStart) {
            val exercise = intent?.extras?.getParcelable<Exercise>(EXTRA_EXERCISE)
            Timber.d("Initial exercise is $exercise")
            model.prepare(exercise)
        }
        model.start()
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

    private fun observeLiveData() {
        model.stateAsLiveData.observe(this, StateObserver())
    }

    private fun listenChannels() {
        lifecycleScope.launchWhenStarted {
            model.messageChannel.consumeEach(::show)
        }
        lifecycleScope.launch {
            model.signalChannel.consumeEach(::process)
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

    private fun process(signal: ExerciseViewModel.Signal) {
        Timber.d("Signal is $signal")
        when (signal) {
            is ExerciseViewModel.Signal.ExerciseName ->
                setExerciseName(signal.value)
        }
    }

    private fun setExerciseName(exerciseName: String) {
        binding.exerciseNameEditText.setText(exerciseName)
        binding.exerciseNameEditText.forwardCursor()
        binding.exerciseNameEditText.clearFocus()
    }

    private inner class StateObserver : Observer<ExerciseViewModel.State> {

        override fun onChanged(state: ExerciseViewModel.State?) {
            Timber.d("State is $state")
            when (state) {
                ExerciseViewModel.State.Waiting ->
                    setWaiting()
                ExerciseViewModel.State.Working ->
                    setWorking()
                is ExerciseViewModel.State.Finishing -> {
                    beforeFinish(state)
                    finish()
                }
            }
        }

        private fun setWaiting() = setProgressActive(true)
        private fun setWorking() = setProgressActive(false)

        private fun beforeFinish(state: ExerciseViewModel.State.Finishing) = when (state) {
            ExerciseViewModel.State.Finishing.Declined ->
                setResult(RESULT_CANCELED)
            ExerciseViewModel.State.Finishing.Done ->
                setResult(RESULT_OK)
        }

        private fun setProgressActive(isActive: Boolean) {
            binding.progressBar.isVisible = isActive
            binding.exerciseCard.isVisible = isActive.not()
        }
    }
}
