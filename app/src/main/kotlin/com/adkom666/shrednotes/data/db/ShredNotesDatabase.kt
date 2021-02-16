package com.adkom666.shrednotes.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.adkom666.shrednotes.data.db.dao.ExerciseDao
import com.adkom666.shrednotes.data.db.entity.ExerciseEntity

private const val DB_VERSION = 1

/**
 * All notes and exercises are stored here.
 */
@Database(
    entities = [
        ExerciseEntity::class
    ],
    version = DB_VERSION
)
abstract class ShredNotesDatabase : RoomDatabase() {

    /**
     * Get an object to interact with the exercises in the database.
     */
    abstract fun exerciseDao(): ExerciseDao
}
