package com.adkom666.shrednotes.statistics

/**
 * Note count records.
 *
 * @property topExerciseNames names of exercises with the count of related notes.
 */
data class NoteCountRecords(
    val topExerciseNames: List<Record>
) {
    /**
     * Single record.
     *
     * @property exerciseName names of exercise.
     * @property noteCount count of related notes.
     */
    data class Record(
        val exerciseName: String?,
        val noteCount: Int
    )
}
