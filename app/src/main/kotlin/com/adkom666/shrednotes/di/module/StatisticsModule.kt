package com.adkom666.shrednotes.di.module

import com.adkom666.shrednotes.data.repository.ExerciseRepository
import com.adkom666.shrednotes.data.repository.NoteRepository
import com.adkom666.shrednotes.statistics.CommonStatisticsAggregator
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Suppress("UndocumentedPublicClass", "UndocumentedPublicFunction")
@Module(includes = [RepositoryModule::class])
class StatisticsModule {

    @Provides
    @Singleton
    fun commonStatisticsAggregator(
        noteRepository: NoteRepository,
        exerciseRepository: ExerciseRepository
    ): CommonStatisticsAggregator {
        return CommonStatisticsAggregator(noteRepository, exerciseRepository)
    }
}
