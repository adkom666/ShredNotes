package com.adkom666.shrednotes.data.external

/**
 * All data to store separately from the application.
 *
 * @property exercises list of all exercises.
 * @property notes list of all notes.
 */
data class ExternalShredNotes(
    val exercises: List<ExternalExercise>,
    val notes: List<ExternalNote>
)
