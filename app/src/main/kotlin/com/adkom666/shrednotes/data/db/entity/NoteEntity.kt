package com.adkom666.shrednotes.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import com.adkom666.shrednotes.common.Id

const val TABLE_NOTES = "notes"
const val TABLE_NOTES_FIELD_ID = "id"
const val TABLE_NOTES_FIELD_TIMESTAMP = "timestamp"
const val TABLE_NOTES_FIELD_EXERCISE_ID = "exercise_id"
const val TABLE_NOTES_FIELD_BPM = "bpm"

/**
 * Information about the table in the database for storing notes.
 *
 * @property id unique identifier of the note.
 * @property timestamp time of training in milliseconds since January 1, 1970, 00:00:00 GMT.
 * @property exerciseId training exercise identifier.
 * @property bpm training BPM.
 */
@Entity(
    tableName = TABLE_NOTES,
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = [TABLE_EXERCISES_FIELD_ID],
            childColumns = [TABLE_NOTES_FIELD_EXERCISE_ID],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = [TABLE_NOTES_FIELD_ID], unique = true),
        Index(value = [TABLE_NOTES_FIELD_TIMESTAMP], unique = false),
        Index(value = [TABLE_NOTES_FIELD_EXERCISE_ID], unique = false),
        Index(value = [TABLE_NOTES_FIELD_BPM], unique = false)
    ]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = TABLE_NOTES_FIELD_ID)
    val id: Id,
    @ColumnInfo(name = TABLE_NOTES_FIELD_TIMESTAMP)
    val timestamp: Long,
    @ColumnInfo(name = TABLE_NOTES_FIELD_EXERCISE_ID)
    val exerciseId: Long,
    @ColumnInfo(name = TABLE_NOTES_FIELD_BPM)
    val bpm: Int
)
