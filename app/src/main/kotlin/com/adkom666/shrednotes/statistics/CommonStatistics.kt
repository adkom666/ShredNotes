package com.adkom666.shrednotes.statistics

/**
 * Common statistical quantities.
 *
 * @property totalNotes the number of all notes.
 * @property totalExercises the number of all exercises.
 * @property totalDays the number of all days from the first note to the last one.
 * @property activeDays the number of days on which notes were kept.
 * @property activeDaysShare the ratio of the number of active days to the total number of days or
 * null if there are no notes.
 */
data class CommonStatistics(
    val totalNotes: Int,
    val totalExercises: Int,
    val totalDays: Int,
    val activeDays: Int,
    val activeDaysShare: Float?
)
