package com.adkom666.shrednotes.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.adkom666.shrednotes.data.converter.DateConverter
import com.adkom666.shrednotes.data.db.dao.ExerciseDao
import com.adkom666.shrednotes.data.db.dao.NoteDao
import com.adkom666.shrednotes.data.db.entity.ExerciseEntity
import com.adkom666.shrednotes.data.db.entity.NoteEntity

private const val DB_VERSION = 1

/**
 * All notes and exercises are stored here.
 */
@Database(
    entities = [
        ExerciseEntity::class,
        NoteEntity::class
    ],
    version = DB_VERSION
)
@TypeConverters(DateConverter::class)
abstract class ShredNotesDatabase : RoomDatabase() {

    /**
     * Get an object to interact with the exercises in the database.
     */
    abstract fun exerciseDao(): ExerciseDao


    /**
     * Get an object to interact with the notes in the database.
     */
    abstract fun noteDao(): NoteDao
}
