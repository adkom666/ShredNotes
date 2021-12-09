package com.adkom666.shrednotes.ui.notes

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
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
import com.adkom666.shrednotes.util.dialog.ConfirmationDialogFragment
import com.adkom666.shrednotes.util.forwardCursor
import com.adkom666.shrednotes.util.performIfConfirmationFoundByTag
import com.adkom666.shrednotes.util.performIfFoundByTag
import com.adkom666.shrednotes.util.setOnSafeClickListener
import com.adkom666.shrednotes.util.time.Days
import com.adkom666.shrednotes.util.time.Minutes
import com.adkom666.shrednotes.util.time.localTimestampOrNull
import com.adkom666.shrednotes.util.time.toLocalCalendar
import com.adkom666.shrednotes.util.toast
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import dagger.android.AndroidInjection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
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
            "${BuildConfig.APPLICATION_ID}.tags.note_date_picker"

        private const val TAG_TIME_PICKER =
            "${BuildConfig.APPLICATION_ID}.tags.note_time_picker"

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
        fun newIntent(context: Context, note: Note? = null): Intent {
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

    private val dateFormat: DateFormat = DateFormat.getDateTimeInstance(
        DateFormat.SHORT,
        DateFormat.SHORT,
        Locale.getDefault()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        _binding = ActivityNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        _model = viewModel(viewModelFactory)

        setupButtonListeners()
        restoreFragmentListeners()
        observeLiveData()
        listenChannels()

        val isFirstStart = savedInstanceState == null
        if (isFirstStart) {
            val note = intent?.extras?.getParcelable<Note>(EXTRA_NOTE)
            Timber.d("Initial note is $note")
            model.prepare(note)
        }
        lifecycleScope.launchWhenCreated {
            model.start()
        }
    }

    private fun setupButtonListeners() {
        binding.pickNoteDateTimeImageButton.setOnSafeClickListener {
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
        supportFragmentManager.performIfFoundByTag<MaterialDatePicker<*>>(TAG_DATE_PICKER) {
            it.addDateListeners()
        }
        supportFragmentManager.performIfFoundByTag<MaterialTimePicker>(TAG_TIME_PICKER) {
            it.addTimeListeners()
        }
        supportFragmentManager.performIfConfirmationFoundByTag(TAG_CONFIRM_SAVE_WITH_EXERCISE) {
            it.setSavingWithExerciseListeners()
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

    private fun afterPickDate(calendar: Calendar) = pickTime(calendar)

    private fun pickDateTime() = pickDate(::afterPickDate)

    private fun pickDate(afterPick: (Calendar) -> Unit) {
        val builder = MaterialDatePicker.Builder.datePicker()
        val selection = model.noteDateTime.localTimestampOrNull()
        builder.setSelection(selection)
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
        dialogFragment.setSavingWithExerciseListeners()
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

    private fun process(signal: NoteViewModel.Signal) {
        Timber.d("Signal is $signal")
        when (signal) {
            is NoteViewModel.Signal.ExerciseList ->
                setExerciseList(signal.value)
            is NoteViewModel.Signal.ActualNoteDateTime ->
                setNoteDateTime(signal.value)
            is NoteViewModel.Signal.NoteExerciseName ->
                setExerciseName(signal.value)
            is NoteViewModel.Signal.NoteBpmString ->
                setBpm(signal.value)
        }
    }

    private fun setExerciseList(exerciseList: List<Exercise>) {
        Timber.d("exerciseList=$exerciseList")
        val adapter = ArrayAdapter(
            this@NoteActivity,
            R.layout.item_exercise_name_dropdown,
            exerciseList.map { it.name }
        )
        binding.noteExerciseAutoCompleteTextView.setAdapter(adapter)
    }

    private fun setNoteDateTime(dateTime: Minutes) {
        binding.noteDateTimeTextView.text = dateFormat.format(dateTime.date)
    }

    private fun setExerciseName(exerciseName: String) {
        binding.noteExerciseAutoCompleteTextView.setText(exerciseName)
        binding.noteExerciseAutoCompleteTextView.forwardCursor()
        binding.noteExerciseAutoCompleteTextView.clearFocus()
    }

    private fun setBpm(bpmString: String) {
        binding.noteBpmEditText.setText(bpmString)
        binding.noteBpmEditText.forwardCursor()
        binding.noteBpmEditText.clearFocus()
    }

    private fun MaterialDatePicker<*>.addDateListeners() = addDateListeners(::afterPickDate)

    private fun MaterialDatePicker<*>.addDateListeners(afterPick: (Calendar) -> Unit) {

        fun setDate(epochMillis: Long): Calendar {
            val oldUtcEpochMillis = model.noteDateTime.epochMillis
            val dayUtcEpochMillis = oldUtcEpochMillis - Days(oldUtcEpochMillis).epochMillis
            val newUtcEpochMillis = Days(epochMillis).epochMillis + dayUtcEpochMillis
            model.noteDateTime = Minutes(newUtcEpochMillis)
            return newUtcEpochMillis.toLocalCalendar()
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
            val calendar = model.noteDateTime.epochMillis.toLocalCalendar()
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            model.noteDateTime = Minutes(calendar.timeInMillis)
        }

        addOnPositiveButtonClickListener {
            setTime(hour, minute)
        }
    }

    private fun ConfirmationDialogFragment.setSavingWithExerciseListeners() {
        setOnConfirmListener { model.acceptSaveWithExercise() }
        setOnNotConfirmListener { model.declineSaveWithExercise() }
        setOnCancelConfirmListener { model.declineSaveWithExercise() }
    }

    private inner class StateObserver : Observer<NoteViewModel.State> {

        override fun onChanged(state: NoteViewModel.State?) {
            Timber.d("State is $state")
            when (state) {
                NoteViewModel.State.Waiting ->
                    setWaiting()
                NoteViewModel.State.Working ->
                    setWorking()
                is NoteViewModel.State.Finishing -> {
                    beforeFinish(state)
                    finish()
                }
            }
        }

        private fun setWaiting() = setProgressActive(true)
        private fun setWorking() = setProgressActive(false)

        private fun beforeFinish(state: NoteViewModel.State.Finishing) = when (state) {
            NoteViewModel.State.Finishing.Declined ->
                setResult(RESULT_CANCELED)
            NoteViewModel.State.Finishing.Done ->
                setResult(RESULT_OK)
        }

        private fun setProgressActive(isActive: Boolean) {
            binding.progressBar.isVisible = isActive
            binding.noteCard.isVisible = isActive.not()
        }
    }
}
