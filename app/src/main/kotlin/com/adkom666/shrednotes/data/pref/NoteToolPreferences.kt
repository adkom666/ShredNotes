package com.adkom666.shrednotes.data.pref

import kotlinx.coroutines.flow.Flow

/**
 * Managing tools such as note filter and search.
 */
interface NoteToolPreferences {

    /**
     * Information signal.
     */
    sealed class Signal {

        /**
         * Indicates that note search activeness has been changed.
         */
        object NoteSearchActivenessChanged : Signal()

        /**
         * Indicates that part of the exercise name of the target note has been changed.
         *
         * @property oldSubname previous subname value.
         * @property newSubname current subname value.
         */
        data class NoteExerciseSubnameChanged(
            val oldSubname: String?,
            val newSubname: String?
        ) : Signal()

        /**
         * Indicates that note filter enablement has been changed.
         *
         * @property isEnabled is filter enabled now.
         */
        data class NoteFilterEnablementChanged(val isEnabled: Boolean) : Signal()
    }

    /**
     * Property for storing a flag indicating whether the search is active.
     */
    var isNoteSearchActive: Boolean

    /**
     * The text that must be contained in the names of the displayed notes' exercises
     * (case-insensitive).
     */
    var noteExerciseSubname: String?

    /**
     * Indicates whether the filter is enabled.
     */
    var isNoteFilterEnabled: Boolean

    /**
     * Collect information signals from this flow in the UI thread.
     */
    val noteToolSignalFlow: Flow<Signal>
}
