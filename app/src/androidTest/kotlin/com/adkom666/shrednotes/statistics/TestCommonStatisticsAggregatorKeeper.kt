package com.adkom666.shrednotes.statistics

import com.adkom666.shrednotes.data.repository.ExerciseRepository
import com.adkom666.shrednotes.data.repository.NoteRepository
import kotlin.time.ExperimentalTime

@ExperimentalTime
class TestCommonStatisticsAggregatorKeeper {

    val statisticsAggregator: CommonStatisticsAggregator
        get() = requireNotNull(_statisticsAggregator)

    private var _statisticsAggregator: CommonStatisticsAggregator? = null

    fun createStatisticsAggregator(
        noteRepository: NoteRepository,
        exerciseRepository: ExerciseRepository
    ) {
        _statisticsAggregator = CommonStatisticsAggregator(noteRepository, exerciseRepository)
    }

    fun destroyStatisticsAggregator() {
        _statisticsAggregator = null
    }
}
