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
import com.adkom666.shrednotes.util.TruncatedToMinutesDate
import com.adkom666.shrednotes.util.forwardCursor
import com.adkom666.shrednotes.util.toast
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import dagger.android.AndroidInjection
import timber.log.Timber
import java.text.DateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

/**
 * Note screen.
 */
class NoteActivity : AppCompatActivity() {

    companion object {

        private const val NOTE_EXTRA = "${BuildConfig.APPLICATION_ID}.extras.note"

        private const val DATE_PICKER_TAG = "${BuildConfig.APPLICATION_ID}.tags.date_picker"
        private const val TIME_PICKER_TAG = "${BuildConfig.APPLICATION_ID}.tags.time_picker"

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
                intent.putExtra(NOTE_EXTRA, it)
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

        setupButtonListeners()
        restoreFragmentListeners()

        model.stateAsLiveData.observe(this, StateObserver())

        if (savedInstanceState == null) {
            val note = intent?.extras?.getParcelable<Note>(NOTE_EXTRA)
            Timber.d("Initial note is $note")
            model.start(note)
        }
    }

    private fun setupButtonListeners() {

        binding.pickNoteDateTimeImageButton.setOnClickListener {
            pickDateTime()
        }

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
    }

    private fun restoreFragmentListeners() {
        val datePickerFragment = supportFragmentManager.findFragmentByTag(DATE_PICKER_TAG)
        val datePicker = datePickerFragment as? MaterialDatePicker<*>
        datePicker?.addDateListeners(::pickTime)

        val timePickerFragment = supportFragmentManager.findFragmentByTag(TIME_PICKER_TAG)
        val timePicker = timePickerFragment as? MaterialTimePicker
        timePicker?.addTimeListeners()
    }

    private fun pickDateTime() = pickDate(::pickTime)

    private fun pickDate(afterPick: (Calendar) -> Unit) {
        val builder = MaterialDatePicker.Builder.datePicker()
        builder.setSelection(model.noteDateTime.epochMillis)
        val picker = builder.build()
        picker.addDateListeners(afterPick)
        picker.show(supportFragmentManager, DATE_PICKER_TAG)
    }

    private fun pickTime(calendar: Calendar) {
        val builder = MaterialTimePicker.Builder()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        builder.setHour(hour)
        builder.setMinute(minute)
        val picker = builder.build()
        picker.addTimeListeners()
        picker.show(supportFragmentManager, TIME_PICKER_TAG)
    }

    private fun MaterialDatePicker<*>.addDateListeners(afterPick: (Calendar) -> Unit) {

        fun setDate(epochMillis: Long): Calendar {
            val oldCalendar = model.noteDateTime.epochMillis.toCalendar()
            val hour = oldCalendar.get(Calendar.HOUR_OF_DAY)
            val minute = oldCalendar.get(Calendar.MINUTE)
            val newCalendar = epochMillis.toCalendar()
            newCalendar.set(Calendar.HOUR_OF_DAY, hour)
            newCalendar.set(Calendar.MINUTE, minute)
            model.noteDateTime = newCalendar.toTruncatedToMinutesDate()
            return newCalendar
        }

        addOnPositiveButtonClickListener { epochMillis ->
            if (epochMillis is Long) {
                val calendar = setDate(epochMillis)
                afterPick(calendar)
            }
        }
    }

    private fun MaterialTimePicker.addTimeListeners() {

        fun setTime(hour: Int, minute: Int) {
            val calendar = model.noteDateTime.epochMillis.toCalendar()
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            model.noteDateTime = calendar.toTruncatedToMinutesDate()
        }

        addOnPositiveButtonClickListener {
            setTime(hour, minute)
        }
    }

    private fun Long.toCalendar(): Calendar {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = this
        return calendar
    }

    private fun Calendar.toTruncatedToMinutesDate(): TruncatedToMinutesDate {
        return TruncatedToMinutesDate(time)
    }

    private inner class StateObserver : Observer<NoteViewModel.State> {

        private val dateFormat = DateFormat.getDateTimeInstance(
            DateFormat.SHORT,
            DateFormat.SHORT,
            Locale.getDefault()
        )

        override fun onChanged(state: NoteViewModel.State?) {
            Timber.d("State is $state")
            when (state) {
                NoteViewModel.State.Waiting ->
                    setWaiting(true)
                is NoteViewModel.State.Ready -> {
                    initExerciseList(state.exerciseList)
                    setWaiting(false)
                    initNote(state.noteDateTime, state.noteExerciseName, state.noteBpmString)
                }
                is NoteViewModel.State.NoteDateTimeChanged ->
                    setNoteDateTime(state.noteDateTime)
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

        private fun setWaiting(active: Boolean) {
            return if (active) {
                binding.noteCard.visibility = View.GONE
                binding.progressBar.visibility = View.VISIBLE
            } else {
                binding.noteCard.visibility = View.VISIBLE
                binding.progressBar.visibility = View.GONE
            }
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

        private fun initNote(
            dateTime: TruncatedToMinutesDate,
            exerciseName: String?,
            bpmString: String?
        ) {
            setNoteDateTime(dateTime)
            exerciseName?.let {
                binding.noteExerciseAutoCompleteTextView.setText(it)
                binding.noteExerciseAutoCompleteTextView.forwardCursor()
            }
            binding.noteExerciseAutoCompleteTextView.clearFocus()
            bpmString?.let {
                binding.noteBpmEditText.setText(it)
                binding.noteBpmEditText.forwardCursor()
            }
            binding.noteBpmEditText.clearFocus()
        }

        private fun setNoteDateTime(dateTime: TruncatedToMinutesDate) {
            binding.noteDateTimeTextView.text = dateFormat.format(dateTime.date)
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
                    model.saveWithExerciseIfValid(note)
                }
                .setNegativeButton(R.string.button_title_cancel, null)
                .create()
                .show()
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
    }
}
