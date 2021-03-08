package com.adkom666.shrednotes.di.module

import com.adkom666.shrednotes.data.repository.ExerciseRepositoryImpl
import com.adkom666.shrednotes.data.db.dao.ExerciseDao
import com.adkom666.shrednotes.data.repository.NoteRepositoryImpl
import dagger.Module
import dagger.Provides

@Suppress("UndocumentedPublicClass", "UndocumentedPublicFunction")
@Module(includes = [DatabaseModule::class])
class RepositoryImplModule {

    @Provides
    fun noteRepositoryImpl(): NoteRepositoryImpl {
        return NoteRepositoryImpl()
    }

    @Provides
    fun exerciseRepositoryImpl(exerciseDao: ExerciseDao): ExerciseRepositoryImpl {
        return ExerciseRepositoryImpl(exerciseDao)
    }
}
