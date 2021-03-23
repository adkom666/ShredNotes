package com.adkom666.shrednotes.di.module

import com.adkom666.shrednotes.data.repository.ExerciseRepository
import com.adkom666.shrednotes.data.repository.ExerciseRepositoryImpl
import com.adkom666.shrednotes.data.repository.NoteRepository
import com.adkom666.shrednotes.data.repository.NoteRepositoryImpl
import com.adkom666.shrednotes.data.repository.ShredNotesRepository
import com.adkom666.shrednotes.data.repository.ShredNotesRepositoryImpl
import dagger.Binds
import dagger.Module
import javax.inject.Singleton

@Suppress("unused", "UndocumentedPublicClass", "UndocumentedPublicFunction")
@Module(includes = [RepositoryImplModule::class])
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun exerciseRepository(impl: ExerciseRepositoryImpl): ExerciseRepository

    @Binds
    @Singleton
    abstract fun noteRepository(impl: NoteRepositoryImpl): NoteRepository

    @Binds
    @Singleton
    abstract fun shredNotesRepository(impl: ShredNotesRepositoryImpl): ShredNotesRepository
}
