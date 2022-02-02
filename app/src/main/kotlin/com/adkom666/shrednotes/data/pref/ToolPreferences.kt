package com.adkom666.shrednotes.data.pref

import android.content.SharedPreferences
import androidx.core.content.edit
import com.adkom666.shrednotes.util.containsDifferentTrimmedTextIgnoreCaseThan
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import timber.log.Timber
import kotlin.properties.Delegates.observable

/**
 * Managing tools such as filter and search.
 */
class ToolPreferences(
    private val preferences: SharedPreferences
) : NoteToolPreferences,
    ExerciseToolPreferences {

    private companion object {
        private const val KEY_IS_NOTE_SEARCH_ACTIVE = "notes.is_search_active"
        private const val KEY_NOTE_EXERCISE_SUBNAME = "notes.exercise_subname"
        private const val KEY_IS_NOTE_FILTER_ENABLED = "notes.is_filter_enabled"

        private const val KEY_IS_EXERCISE_SEARCH_ACTIVE = "exercises.is_search_active"
        private const val KEY_EXERCISE_SUBNAME = "exercises.subname"
    }

    override var isNoteSearchActive: Boolean by observable(
        initialValue = preferences.getBoolean(KEY_IS_NOTE_SEARCH_ACTIVE, false)
    ) { _, old, new ->
        Timber.d("Change isNoteSearchActive: old=$old, new=$new")
        preferences.edit {
            putBoolean(KEY_IS_NOTE_SEARCH_ACTIVE, new)
        }
        give(NoteToolPreferences.Signal.NoteSearchActivenessChanged)
    }

    override var noteExerciseSubname: String? by observable(
        initialValue = preferences.getString(KEY_NOTE_EXERCISE_SUBNAME, null)
    ) { _, old, new ->
        Timber.d("Change noteExerciseSubname: old=$old, new=$new")
        if (new containsDifferentTrimmedTextIgnoreCaseThan old) {
            val finalExerciseSubname = new?.trim()
            preferences.edit {
                putString(KEY_NOTE_EXERCISE_SUBNAME, finalExerciseSubname)
            }
            give(NoteToolPreferences.Signal.NoteExerciseSubnameChanged(old, new))
        }
    }

    override var isNoteFilterEnabled: Boolean by observable(
        initialValue = preferences.getBoolean(KEY_IS_NOTE_FILTER_ENABLED, false)
    ) { _, old, new ->
        Timber.d("Change _isNoteFilterEnabled: old=$old, new=$new")
        if (new != old) {
            preferences.edit {
                putBoolean(KEY_IS_NOTE_FILTER_ENABLED, new)
            }
            give(NoteToolPreferences.Signal.NoteFilterEnablementChanged(new))
        }
    }

    override val noteToolSignalFlow: Flow<NoteToolPreferences.Signal>
        get() = _noteToolSignalChannel.receiveAsFlow()

    override var isExcerciseSearchActive: Boolean by observable(
        initialValue = preferences.getBoolean(KEY_IS_EXERCISE_SEARCH_ACTIVE, false)
    ) { _, old, new ->
        Timber.d("Change isExcerciseSearchActive: old=$old, new=$new")
        preferences.edit {
            putBoolean(KEY_IS_EXERCISE_SEARCH_ACTIVE, new)
        }
        give(ExerciseToolPreferences.Signal.ExcerciseSearchActivenessChanged)
    }

    override var exerciseSubname: String? by observable(
        initialValue = preferences.getString(KEY_EXERCISE_SUBNAME, null)
    ) { _, old, new ->
        Timber.d("Change exerciseSubname: old=$old, new=$new")
        if (new containsDifferentTrimmedTextIgnoreCaseThan old) {
            val finalExerciseSubname = new?.trim()
            preferences.edit {
                putString(KEY_EXERCISE_SUBNAME, finalExerciseSubname)
            }
            give(ExerciseToolPreferences.Signal.ExerciseSubnameChanged(old, new))
        }
    }

    override val exerciseToolSignalFlow: Flow<ExerciseToolPreferences.Signal>
        get() = _exerciseToolSignalChannel.receiveAsFlow()

    private val _noteToolSignalChannel: Channel<NoteToolPreferences.Signal> =
        Channel(Channel.UNLIMITED)

    private val _exerciseToolSignalChannel: Channel<ExerciseToolPreferences.Signal> =
        Channel(Channel.UNLIMITED)

    /**
     * Reset all tools.
     */
    fun reset() {
        isNoteSearchActive = false
        noteExerciseSubname = null
        isExcerciseSearchActive = false
        exerciseSubname = null
        isNoteFilterEnabled = false
    }

    private fun give(signal: NoteToolPreferences.Signal) {
        Timber.d("Give: signal=$signal")
        _noteToolSignalChannel.offer(signal)
    }

    private fun give(signal: ExerciseToolPreferences.Signal) {
        Timber.d("Give: signal=$signal")
        _exerciseToolSignalChannel.offer(signal)
    }
}
