package com.adkom666.shrednotes.di.module

import com.adkom666.shrednotes.data.db.Transactor
import com.adkom666.shrednotes.data.db.dao.ExerciseDao
import com.adkom666.shrednotes.data.db.dao.NoteDao
import com.adkom666.shrednotes.data.repository.ExerciseRepositoryImpl
import com.adkom666.shrednotes.data.repository.NoteRepositoryImpl
import com.adkom666.shrednotes.data.repository.ShredNotesRepositoryImpl
import dagger.Module
import dagger.Provides

@Suppress("UndocumentedPublicClass", "UndocumentedPublicFunction")
@Module(includes = [DatabaseModule::class])
class RepositoryImplModule {

    @Provides
    fun exerciseRepositoryImpl(
        exerciseDao: ExerciseDao
    ): ExerciseRepositoryImpl {
        return ExerciseRepositoryImpl(exerciseDao)
    }

    @Provides
    fun noteRepositoryImpl(
        noteDao: NoteDao,
        transactor: Transactor
    ): NoteRepositoryImpl {
        return NoteRepositoryImpl(noteDao, transactor)
    }

    @Provides
    fun shredNotesRepositoryImpl(
        exerciseDao: ExerciseDao,
        noteDao: NoteDao,
        transactor: Transactor
    ): ShredNotesRepositoryImpl {
        return ShredNotesRepositoryImpl(exerciseDao, noteDao, transactor)
    }
}
