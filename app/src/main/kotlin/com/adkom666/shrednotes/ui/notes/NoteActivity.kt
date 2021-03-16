package com.adkom666.shrednotes.ui.notes

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.adkom666.shrednotes.BuildConfig
import com.adkom666.shrednotes.R
import com.adkom666.shrednotes.data.model.Exercise
import com.adkom666.shrednotes.data.model.NOTE_BPM_MAX
import com.adkom666.shrednotes.data.model.NOTE_BPM_MIN
import com.adkom666.shrednotes.data.model.Note
import com.adkom666.shrednotes.databinding.ActivityNoteBinding
import com.adkom666.shrednotes.di.viewmodel.viewModel
import com.adkom666.shrednotes.util.ConfirmationDialogFragment
import com.adkom666.shrednotes.util.TruncatedToMinutesDate
import com.adkom666.shrednotes.util.forwardCursor
import com.adkom666.shrednotes.util.toast
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import dagger.android.AndroidInjection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.consumeEach
import timber.log.Timber
import java.text.DateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

/**
 * Note screen.
 */
@ExperimentalCoroutinesApi
class NoteActivity : AppCompatActivity() {

    companion object {

        private const val EXTRA_NOTE =
            "${BuildConfig.APPLICATION_ID}.extras.note"

        private const val TAG_DATE_PICKER =
            "${BuildConfig.APPLICATION_ID}.tags.date_picker"

        private const val TAG_TIME_PICKER =
            "${BuildConfig.APPLICATION_ID}.tags.time_picker"

        private const val TAG_CONFIRM_SAVE_WITH_EXERCISE =
            "${BuildConfig.APPLICATION_ID}.tags.confirm_save_with_exercise"

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

        setupButtonListeners()
        restoreFragmentListeners()

        model.stateAsLiveData.observe(this, StateObserver())

        lifecycleScope.launchWhenStarted {
            model.messageChannel.consumeEach(::show)
        }

        val isFirstStart = savedInstanceState == null
        if (isFirstStart) {
            val note = intent?.extras?.getParcelable<Note>(EXTRA_NOTE)
            Timber.d("Initial note is $note")
            model.prepare(note)
        }
        model.start(isFirstStart)
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
        val datePickerFragment = supportFragmentManager.findFragmentByTag(TAG_DATE_PICKER)
        val datePicker = datePickerFragment as? MaterialDatePicker<*>
        datePicker?.addDateListeners(::pickTime)

        val timePickerFragment = supportFragmentManager.findFragmentByTag(TAG_TIME_PICKER)
        val timePicker = timePickerFragment as? MaterialTimePicker
        timePicker?.addTimeListeners()

        val fragment = supportFragmentManager.findFragmentByTag(TAG_CONFIRM_SAVE_WITH_EXERCISE)
        val confirmation = fragment as? ConfirmationDialogFragment
        confirmation?.setListeners()
    }

    private fun pickDateTime() = pickDate(::pickTime)

    private fun pickDate(afterPick: (Calendar) -> Unit) {
        val builder = MaterialDatePicker.Builder.datePicker()
        builder.setSelection(model.noteDateTime.epochMillis)
        val picker = builder.build()
        picker.addDateListeners(afterPick)
        picker.show(supportFragmentManager, TAG_DATE_PICKER)
    }

    private fun pickTime(calendar: Calendar) {
        val builder = MaterialTimePicker.Builder()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        builder.setHour(hour)
        builder.setMinute(minute)
        val picker = builder.build()
        picker.addTimeListeners()
        picker.show(supportFragmentManager, TAG_TIME_PICKER)
    }

    private fun show(message: NoteViewModel.Message) = when (message) {
        is NoteViewModel.Message.SuggestSaveWithExercise ->
            suggestSaveWithExercise(message.noteExerciseName)
        is NoteViewModel.Message.Error ->
            showError(message)
    }

    private fun suggestSaveWithExercise(noteExerciseName: String) {
        val dialogFragment = ConfirmationDialogFragment.newInstance(
            R.string.dialog_confirm_note_exercise_creating_title,
            R.string.dialog_confirm_note_exercise_creating_message,
            noteExerciseName
        )
        dialogFragment.setListeners()
        dialogFragment.show(supportFragmentManager, TAG_CONFIRM_SAVE_WITH_EXERCISE)
    }

    private fun showError(message: NoteViewModel.Message.Error) = when (message) {
        NoteViewModel.Message.Error.MissingNoteExerciseName ->
            toast(R.string.error_missing_note_excercise_name)
        NoteViewModel.Message.Error.MissingNoteBpm ->
            toast(R.string.error_missing_note_bpm)
        NoteViewModel.Message.Error.WrongNoteBpm -> {
            val messageString = getString(
                R.string.error_wrong_note_bpm,
                NOTE_BPM_MIN,
                NOTE_BPM_MAX
            )
            toast(messageString)
        }
        is NoteViewModel.Message.Error.Clarified -> {
            val messageString = getString(
                R.string.error_clarified,
                message.details
            )
            toast(messageString)
        }
        NoteViewModel.Message.Error.Unknown ->
            toast(R.string.error_unknown)
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

    private fun ConfirmationDialogFragment.setListeners() {
        setOnConfirmListener { model.acceptSaveWithExercise() }
        setOnNotConfirmListener { model.declineSaveWithExercise() }
        setOnCancelConfirmListener { model.declineSaveWithExercise() }
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
                is NoteViewModel.State.Init -> {
                    initExerciseList(state.exerciseList)
                    initNote(state.noteDateTime, state.noteExerciseName, state.noteBpmString)
                    model.ok()
                }
                NoteViewModel.State.Normal ->
                    setWaiting(false)
                is NoteViewModel.State.NoteDateTimeChanged ->
                    setNoteDateTime(state.noteDateTime)
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
    }
}
