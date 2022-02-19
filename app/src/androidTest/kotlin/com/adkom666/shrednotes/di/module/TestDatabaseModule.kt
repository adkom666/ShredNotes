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
@Module(includes = [TestContextModule::class])
class TestDatabaseModule {

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
        return Room.inMemoryDatabaseBuilder(
            context,
            ShredNotesDatabase::class.java
        ).build()
    }
}
