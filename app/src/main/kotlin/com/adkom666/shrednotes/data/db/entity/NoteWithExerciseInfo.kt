package com.adkom666.shrednotes.data.db.entity

import androidx.room.ColumnInfo
import com.adkom666.shrednotes.common.Id

const val TABLE_NOTES_WITH_EXERCISES_FIELD_NOTE_ID = "note_id"
const val TABLE_NOTES_WITH_EXERCISES_FIELD_NOTE_TIMESTAMP = "note_timestamp"
const val TABLE_NOTES_WITH_EXERCISES_FIELD_EXERCISE_NAME = "exercise_name"
const val TABLE_NOTES_WITH_EXERCISES_FIELD_NOTE_BPM = "note_bpm"

/**
 * Information about the note in connection with its exercise information.
 *
 * @property noteId unique identifier of the note.
 * @property noteTimestamp time of training in milliseconds since January 1, 1970, 00:00:00 GMT.
 * @property exerciseName training exercise name.
 * @property noteBpm training BPM.
 */
data class NoteWithExerciseInfo(
    @ColumnInfo(name = TABLE_NOTES_WITH_EXERCISES_FIELD_NOTE_ID)
    val noteId: Id,
    @ColumnInfo(name = TABLE_NOTES_WITH_EXERCISES_FIELD_NOTE_TIMESTAMP)
    val noteTimestamp: Long,
    @ColumnInfo(name = TABLE_NOTES_WITH_EXERCISES_FIELD_EXERCISE_NAME)
    val exerciseName: String?,
    @ColumnInfo(name = TABLE_NOTES_WITH_EXERCISES_FIELD_NOTE_BPM)
    val noteBpm: Int
)
