package com.adkom666.shrednotes.statistics

import com.adkom666.shrednotes.data.repository.NoteRepository

class TestWeekdaysStatisticsAggregatorKeeper {

    val statisticsAggregator: WeekdaysStatisticsAggregator
        get() = requireNotNull(_statisticsAggregator)

    private var _statisticsAggregator: WeekdaysStatisticsAggregator? = null

    fun createStatisticsAggregator(noteRepository: NoteRepository) {
        _statisticsAggregator = WeekdaysStatisticsAggregator(noteRepository)
    }

    fun destroyStatisticsAggregator() {
        _statisticsAggregator = null
    }
}
