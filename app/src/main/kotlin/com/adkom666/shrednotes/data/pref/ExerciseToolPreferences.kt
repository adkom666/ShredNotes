package com.adkom666.shrednotes.data.pref

import kotlinx.coroutines.flow.Flow

/**
 * Managing exercise search.
 */
interface ExerciseToolPreferences {

    /**
     * Information signal.
     */
    sealed class Signal {

        /**
         * Indicates that exercise search activeness has been changed.
         */
        object ExcerciseSearchActivenessChanged : Signal()

        /**
         * Indicates that part of the target exercise name has been changed.
         *
         * @property oldSubname previous subname value.
         * @property newSubname current subname value.
         */
        data class ExerciseSubnameChanged(
            val oldSubname: String?,
            val newSubname: String?
        ) : Signal()
    }

    /**
     * Property for storing a flag indicating whether the search is active.
     */
    var isExcerciseSearchActive: Boolean

    /**
     * The text that must be contained in the names of the displayed notes' exercises
     * (case-insensitive).
     */
    var exerciseSubname: String?

    /**
     * Collect information signals from this flow in the UI thread.
     */
    val exerciseToolSignalFlow: Flow<Signal>
}
