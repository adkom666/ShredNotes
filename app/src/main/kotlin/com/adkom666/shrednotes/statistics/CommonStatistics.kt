package com.adkom666.shrednotes.statistics

/**
 * Common statistical quantities.
 *
 * @property notes the number of notes for the defined period.
 * @property relatedExercises the number of exercises referenced by at least one note during the
 * defined period.
 * @property daysIdeally maximum number of days for training: the number of days from the first note
 * to the last one or to the end of the period or to today inclusive (if it greater than the last
 * note date).
 * @property activeDays the number of days on which notes were kept.
 * @property activeDaysShare the ratio of the number of active days to the total number of days or
 * null if there are no notes.
 * @property allNotes the number of all notes.
 * @property allExercises the number of all exercises.
 */
data class CommonStatistics(
    val notes: Int,
    val relatedExercises: Int,
    val daysIdeally: Int,
    val activeDays: Int,
    val activeDaysShare: Float?,
    val allNotes: Int,
    val allExercises: Int
)
