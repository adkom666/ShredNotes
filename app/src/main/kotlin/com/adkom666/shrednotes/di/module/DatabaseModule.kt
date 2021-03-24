package com.adkom666.shrednotes.di.module

import android.content.Context
import androidx.room.Room
import com.adkom666.shrednotes.data.db.ShredNotesDatabase
import com.adkom666.shrednotes.data.db.Transactor
import com.adkom666.shrednotes.data.db.dao.ExerciseDao
import com.adkom666.shrednotes.data.db.dao.NoteDao
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Suppress("UndocumentedPublicClass", "UndocumentedPublicFunction")
@Module(includes = [ContextModule::class])
class DatabaseModule {

    private companion object {
        private const val DB_NAME = "shred_notes.db"
    }

    @Provides
    @Singleton
    fun exerciseDao(database: ShredNotesDatabase): ExerciseDao {
        return database.exerciseDao()
    }

    @Provides
    @Singleton
    fun noteDao(database: ShredNotesDatabase): NoteDao {
        return database.noteDao()
    }

    @Provides
    @Singleton
    fun transactor(database: ShredNotesDatabase): Transactor {
        return Transactor(database)
    }

    @Provides
    @Singleton
    fun shredNotesDatabase(context: Context): ShredNotesDatabase {
        return Room.databaseBuilder(
            context,
            ShredNotesDatabase::class.java,
            DB_NAME
        ).build()
    }
}
