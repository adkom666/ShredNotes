package com.adkom666.shrednotes.data.pref

import android.content.SharedPreferences
import androidx.core.content.edit
import com.adkom666.shrednotes.util.containsDifferentTrimmedTextIgnoreCaseThan
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import timber.log.Timber
import kotlin.properties.Delegates

/**
 * Managing tools such as filter and search.
 */
@ExperimentalCoroutinesApi
class ToolPreferences(
    private val preferences: SharedPreferences
) : NoteToolPreferences,
    ExerciseToolPreferences {

    private companion object {
        private const val SIGNAL_CHANNEL_CAPACITY = Channel.BUFFERED

        private const val KEY_IS_NOTE_SEARCH_ACTIVE = "notes.is_search_active"
        private const val KEY_NOTE_EXERCISE_SUBNAME = "notes.exercise_subname"
        private const val KEY_IS_NOTE_FILTER_ENABLED = "notes.is_filter_enabled"

        private const val KEY_IS_EXERCISE_SEARCH_ACTIVE = "exercises.is_search_active"
        private const val KEY_EXERCISE_SUBNAME = "exercises.subname"
    }

    override var isNoteSearchActive: Boolean by Delegates.observable(
        preferences.getBoolean(KEY_IS_NOTE_SEARCH_ACTIVE, false)
    ) { _, old, new ->
        Timber.d("Change isNoteSearchActive: old=$old, new=$new")
        preferences.edit {
            putBoolean(KEY_IS_NOTE_SEARCH_ACTIVE, new)
        }
        give(NoteToolPreferences.Signal.NoteSearchActivenessChanged)
    }

    override var noteExerciseSubname: String? by Delegates.observable(
        preferences.getString(KEY_NOTE_EXERCISE_SUBNAME, null)
    ) { _, old, new ->
        Timber.d("Change noteExerciseSubname: old=$old, new=$new")
        if (new containsDifferentTrimmedTextIgnoreCaseThan old) {
            val finalExerciseSubname = new?.trim()
            preferences.edit {
                putString(KEY_NOTE_EXERCISE_SUBNAME, finalExerciseSubname)
            }
            give(NoteToolPreferences.Signal.NoteExerciseSubnameChanged)
        }
    }

    override var isNoteFilterEnabled: Boolean by Delegates.observable(
        preferences.getBoolean(KEY_IS_NOTE_FILTER_ENABLED, false)
    ) { _, old, new ->
        Timber.d("Change _isNoteFilterEnabled: old=$old, new=$new")
        if (new != old) {
            preferences.edit {
                putBoolean(KEY_IS_NOTE_FILTER_ENABLED, new)
            }
            give(NoteToolPreferences.Signal.NoteFilterEnablementChanged)
        }
    }

    override val noteToolSignalChannel: ReceiveChannel<NoteToolPreferences.Signal>
        get() = _noteToolSignalChannel.openSubscription()

    override var isExcerciseSearchActive: Boolean by Delegates.observable(
        preferences.getBoolean(KEY_IS_EXERCISE_SEARCH_ACTIVE, false)
    ) { _, old, new ->
        Timber.d("Change isExcerciseSearchActive: old=$old, new=$new")
        preferences.edit {
            putBoolean(KEY_IS_EXERCISE_SEARCH_ACTIVE, new)
        }
        give(ExerciseToolPreferences.Signal.ExcerciseSearchActivenessChanged)
    }

    override var exerciseSubname: String? by Delegates.observable(
        preferences.getString(KEY_EXERCISE_SUBNAME, null)
    ) { _, old, new ->
        Timber.d("Change exerciseSubname: old=$old, new=$new")
        if (new containsDifferentTrimmedTextIgnoreCaseThan old) {
            val finalExerciseSubname = new?.trim()
            preferences.edit {
                putString(KEY_EXERCISE_SUBNAME, finalExerciseSubname)
            }
            give(ExerciseToolPreferences.Signal.ExerciseSubnameChanged)
        }
    }

    override val exerciseToolSignalChannel: ReceiveChannel<ExerciseToolPreferences.Signal>
        get() = _exerciseToolSignalChannel.openSubscription()

    private val _noteToolSignalChannel: BroadcastChannel<NoteToolPreferences.Signal> =
        BroadcastChannel(SIGNAL_CHANNEL_CAPACITY)

    private val _exerciseToolSignalChannel: BroadcastChannel<ExerciseToolPreferences.Signal> =
        BroadcastChannel(SIGNAL_CHANNEL_CAPACITY)

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
