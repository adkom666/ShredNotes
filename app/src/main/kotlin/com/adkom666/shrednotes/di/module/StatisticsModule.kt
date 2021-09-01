package com.adkom666.shrednotes.di.module

import com.adkom666.shrednotes.data.repository.ExerciseRepository
import com.adkom666.shrednotes.data.repository.NoteRepository
import com.adkom666.shrednotes.statistics.CommonStatisticsAggregator
import com.adkom666.shrednotes.statistics.RecordsAggregator
import com.adkom666.shrednotes.statistics.WeekdaysStatisticsAggregator
import dagger.Module
import dagger.Provides
import javax.inject.Singleton
import kotlin.time.ExperimentalTime

@Suppress("UndocumentedPublicClass", "UndocumentedPublicFunction")
@ExperimentalTime
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

    @Provides
    @Singleton
    fun weekdaysStatisticsAggregator(
        noteRepository: NoteRepository
    ): WeekdaysStatisticsAggregator {
        return WeekdaysStatisticsAggregator(noteRepository)
    }

    @Provides
    @Singleton
    fun recordsAggregator(
        noteRepository: NoteRepository
    ): RecordsAggregator {
        return RecordsAggregator(noteRepository)
    }
}
