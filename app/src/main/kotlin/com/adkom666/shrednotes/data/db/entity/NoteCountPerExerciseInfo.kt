package com.adkom666.shrednotes.data.db.entity

import androidx.room.ColumnInfo
import com.adkom666.shrednotes.common.Id

const val TABLE_NOTE_COUNT_PER_EXERCISE_FIELD_EXERCISE_ID = "exercise_id"
const val TABLE_NOTE_COUNT_PER_EXERCISE_FIELD_EXERCISE_NAME = "exercise_name"
const val TABLE_NOTE_COUNT_PER_EXERCISE_FIELD_NOTE_COUNT = "note_count"

/**
 * Information about the count of exercise-related notes.
 *
 * @property exerciseId unique identifier of the training exercise.
 * @property exerciseName training exercise name.
 * @property noteCount the count of exercise-related notes.
 */
data class NoteCountPerExerciseInfo(
    @ColumnInfo(name = TABLE_NOTE_COUNT_PER_EXERCISE_FIELD_EXERCISE_ID)
    val exerciseId: Id,
    @ColumnInfo(name = TABLE_NOTE_COUNT_PER_EXERCISE_FIELD_EXERCISE_NAME)
    val exerciseName: String?,
    @ColumnInfo(name = TABLE_NOTE_COUNT_PER_EXERCISE_FIELD_NOTE_COUNT)
    val noteCount: Int
)
