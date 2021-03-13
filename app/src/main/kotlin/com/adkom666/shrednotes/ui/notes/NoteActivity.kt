package com.adkom666.shrednotes.ui.notes

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.adkom666.shrednotes.BuildConfig
import com.adkom666.shrednotes.R
import com.adkom666.shrednotes.data.model.Exercise
import com.adkom666.shrednotes.data.model.NOTE_BPM_MAX
import com.adkom666.shrednotes.data.model.NOTE_BPM_MIN
import com.adkom666.shrednotes.data.model.Note
import com.adkom666.shrednotes.databinding.ActivityNoteBinding
import com.adkom666.shrednotes.di.viewmodel.viewModel
import com.adkom666.shrednotes.util.forwardCursor
import com.adkom666.shrednotes.util.toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.android.AndroidInjection
import timber.log.Timber
import javax.inject.Inject

/**
 * Note screen.
 */
class NoteActivity : AppCompatActivity() {

    companion object {

        private const val EXTRA_NOTE = "${BuildConfig.APPLICATION_ID}.extras.note"

        /**
         * Creating an intent to open the edit screen for a given [note], or to create a new note if
         * [note] is null.
         *
         * @param context [Context] to create an intent.
         * @param note [Note] for the edit.
         * @return intent to open the edit screen for a given [note], or to create a new note if
         * [note] is null.
         */
        fun newIntent(context: Context, note: Note?): Intent {
            val intent = Intent(context, NoteActivity::class.java)
            note?.let {
                intent.putExtra(EXTRA_NOTE, it)
            }
            return intent
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val binding: ActivityNoteBinding
        get() = requireNotNull(_binding)

    private val model: NoteViewModel
        get() = requireNotNull(_model)

    private var _binding: ActivityNoteBinding? = null
    private var _model: NoteViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        _binding = ActivityNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        _model = viewModel(viewModelFactory)

        binding.okButton.setOnClickListener {
            val noteExerciseName = binding.noteExerciseAutoCompleteTextView.text.toString().trim()
            val noteBpmString = binding.noteBpmEditText.text.toString().trim()
            Timber.d(
                """Save note:
                    |noteExerciseName=$noteExerciseName, 
                    |noteBpmString=$noteBpmString""".trimMargin()
            )
            model.save(noteExerciseName, noteBpmString)
        }

        binding.cancelButton.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }

        model.stateAsLiveData.observe(this, StateObserver())

        if (savedInstanceState == null) {
            val note = intent?.extras?.getParcelable<Note>(EXTRA_NOTE)
            Timber.d("Initial note is $note")
            model.start(note)
        }
    }

    private fun setWaiting(active: Boolean) {
        return if (active) {
            binding.noteCard.visibility = View.GONE
            binding.progressBar.visibility = View.VISIBLE
        } else {
            binding.noteCard.visibility = View.VISIBLE
            binding.progressBar.visibility = View.GONE
        }
    }

    private inner class StateObserver : Observer<NoteViewModel.State> {

        override fun onChanged(state: NoteViewModel.State?) {
            Timber.d("State is $state")
            when (state) {
                NoteViewModel.State.Waiting ->
                    setWaiting(true)
                is NoteViewModel.State.Ready -> {
                    initExerciseList(state.exerciseList)
                    setWaiting(false)
                    state.note?.let {
                        initNote(it)
                    }
                }
                is NoteViewModel.State.Error -> {
                    setWaiting(false)
                    handleError(state)
                }
                is NoteViewModel.State.ConsiderSaveWithExercise -> {
                    setWaiting(false)
                    saveWithExercise(this@NoteActivity, state.note)
                }
                NoteViewModel.State.Declined -> {
                    setResult(RESULT_CANCELED)
                    finish()
                }
                NoteViewModel.State.Done -> {
                    setResult(RESULT_OK)
                    finish()
                }
            }
        }

        private fun handleError(error: NoteViewModel.State.Error) = when (error) {
            NoteViewModel.State.Error.MissingNoteExerciseName ->
                toast(R.string.error_missing_note_excercise_name)
            NoteViewModel.State.Error.MissingNoteBpm ->
                toast(R.string.error_missing_note_bpm)
            NoteViewModel.State.Error.WrongNoteBpm -> {
                val message = getString(
                    R.string.error_wrong_note_bpm,
                    NOTE_BPM_MIN,
                    NOTE_BPM_MAX
                )
                toast(message)
            }
            is NoteViewModel.State.Error.Clarified -> {
                val message = getString(
                    R.string.error_clarified,
                    error.message
                )
                toast(message)
            }
            NoteViewModel.State.Error.Unknown ->
                toast(R.string.error_unknown)
        }

        private fun initExerciseList(exerciseList: List<Exercise>) {
            Timber.d("exerciseList=$exerciseList")
            val adapter = ArrayAdapter(
                this@NoteActivity,
                android.R.layout.simple_dropdown_item_1line,
                exerciseList.map { it.name }
            )
            binding.noteExerciseAutoCompleteTextView.setAdapter(adapter)
        }

        private fun initNote(note: Note) {
            binding.noteExerciseAutoCompleteTextView.setText(note.exerciseName)
            binding.noteExerciseAutoCompleteTextView.forwardCursor()
            binding.noteBpmEditText.setText(note.bpm.toString())
            binding.noteBpmEditText.forwardCursor()
        }

        private fun saveWithExercise(context: Context, note: Note) {
            val messageString = getString(
                R.string.dialog_confirm_note_exercise_creating_message,
                note.exerciseName
            )
            MaterialAlertDialogBuilder(context, R.style.AppTheme_MaterialAlertDialog_Confirmation)
                .setTitle(R.string.dialog_confirm_note_exercise_creating_title)
                .setMessage(messageString)
                .setPositiveButton(R.string.button_title_ok) { _, _ ->
                    model.saveWithExercise(note)
                }
                .setNegativeButton(R.string.button_title_cancel, null)
                .create()
                .show()
        }
    }
}
