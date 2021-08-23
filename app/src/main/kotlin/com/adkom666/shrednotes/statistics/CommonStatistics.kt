package com.adkom666.shrednotes.statistics

/**
 * Common statistical quantities.
 *
 * @property totalNotes the number of all notes.
 * @property totalExercises the number of all exercises.
 */
data class CommonStatistics(
    val totalNotes: Int,
    val totalExercises: Int
)
