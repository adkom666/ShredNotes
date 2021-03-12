package com.adkom666.shrednotes.data.db.entity

import androidx.room.ColumnInfo
import com.adkom666.shrednotes.common.Id
import java.util.Date

const val TABLE_NOTES_WITH_EXERCISES_FIELD_NOTE_ID = "note_id"
const val TABLE_NOTES_WITH_EXERCISES_FIELD_NOTE_DATE_TIME = "note_date_time"
const val TABLE_NOTES_WITH_EXERCISES_FIELD_EXERCISE_NAME = "exercise_name"
const val TABLE_NOTES_WITH_EXERCISES_FIELD_NOTE_BPM = "note_bpm"

/**
 * Information about the note in connection with its exercise information.
 *
 * @property noteId unique identifier of the note.
 * @property noteDateTime date and time of training.
 * @property exerciseName training exercise name.
 * @property noteBpm training BPM.
 */
data class NoteWithExerciseInfo(
    @ColumnInfo(name = TABLE_NOTES_WITH_EXERCISES_FIELD_NOTE_ID)
    val noteId: Id,
    @ColumnInfo(name = TABLE_NOTES_WITH_EXERCISES_FIELD_NOTE_DATE_TIME)
    val noteDateTime: Date,
    @ColumnInfo(name = TABLE_NOTES_WITH_EXERCISES_FIELD_EXERCISE_NAME)
    val exerciseName: String?,
    @ColumnInfo(name = TABLE_NOTES_WITH_EXERCISES_FIELD_NOTE_BPM)
    val noteBpm: Int
)
