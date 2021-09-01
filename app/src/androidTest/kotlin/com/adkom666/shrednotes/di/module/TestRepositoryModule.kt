package com.adkom666.shrednotes.di.module

import com.adkom666.shrednotes.data.repository.ExerciseRepository
import com.adkom666.shrednotes.data.repository.ExerciseRepositoryImpl
import com.adkom666.shrednotes.data.repository.NoteRepository
import com.adkom666.shrednotes.data.repository.NoteRepositoryImpl
import dagger.Binds
import dagger.Module
import javax.inject.Singleton

@Suppress("unused", "UndocumentedPublicClass", "UndocumentedPublicFunction")
@Module(includes = [TestRepositoryImplModule::class])
abstract class TestRepositoryModule {

    @Binds
    @Singleton
    abstract fun exerciseRepository(impl: ExerciseRepositoryImpl): ExerciseRepository

    @Binds
    @Singleton
    abstract fun noteRepository(impl: NoteRepositoryImpl): NoteRepository
}
