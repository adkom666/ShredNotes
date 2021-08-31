package com.adkom666.shrednotes.statistics

import com.adkom666.shrednotes.data.repository.NoteRepository

class TestRecordsAggregatorKeeper {

    val recordsAggregator: RecordsAggregator
        get() = requireNotNull(_recordsAggregator)

    private var _recordsAggregator: RecordsAggregator? = null

    fun createRecordsAggregator(noteRepository: NoteRepository) {
        _recordsAggregator = RecordsAggregator(noteRepository)
    }

    fun destroyRecordsAggregator() {
        _recordsAggregator = null
    }
}
