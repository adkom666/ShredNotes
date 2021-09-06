package com.adkom666.shrednotes.data.external

/**
 * All data to store separately from the application. Version 1.
 *
 * @property exercises list of all exercises.
 * @property notes list of all notes.
 */
data class ExternalShredNotesV1(
    val exercises: List<ExternalExerciseV1>,
    val notes: List<ExternalNoteV1>
)
