package com.adkom666.shrednotes.data.pref

import kotlinx.coroutines.channels.ReceiveChannel

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
         */
        object NoteExerciseSubnameChanged : Signal()

        /**
         * Indicates that note filter enablement has been changed.
         */
        object NoteFilterEnablementChanged : Signal()
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
     * Consume information signals from this channel in the UI thread.
     */
    val noteToolSignalChannel: ReceiveChannel<Signal>
}
