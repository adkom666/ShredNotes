package com.adkom666.shrednotes.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.adkom666.shrednotes.common.Id

const val TABLE_EXERCISES = "exercises"
const val TABLE_EXERCISES_FIELD_ID = "id"
const val TABLE_EXERCISES_FIELD_NAME = "name"

/**
 * Information about the table in the database for storing exercises.
 *
 * @property id unique identifier of the exercise.
 * @property name name of this exercise.
 */
@Entity(
    tableName = TABLE_EXERCISES,
    indices = [
        Index(value = [TABLE_EXERCISES_FIELD_ID], unique = true),
        Index(value = [TABLE_EXERCISES_FIELD_NAME], unique = true)
    ]
)
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = TABLE_EXERCISES_FIELD_ID)
    val id: Id,
    @ColumnInfo(name = TABLE_EXERCISES_FIELD_NAME)
    val name: String
)
