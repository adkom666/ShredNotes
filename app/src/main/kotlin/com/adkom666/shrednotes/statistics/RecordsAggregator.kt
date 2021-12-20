package com.adkom666.shrednotes.statistics

import com.adkom666.shrednotes.data.repository.NoteRepository
import com.adkom666.shrednotes.util.DateRange
import timber.log.Timber

/**
 * Get records here.
 *
 * @property noteRepository note storage.
 */
class RecordsAggregator(
    private val noteRepository: NoteRepository
) {
    private var topNotesCache: MutableMap<RecordsId, TopNotes> = mutableMapOf()
    private var topExerciseNamesCache: MutableMap<RecordsId, TopExerciseNames> = mutableMapOf()

    /**
     * Aggregate BPM records.
     *
     * @param dateRange the date range over which records should be aggregated.
     * @param limit maximum count of records.
     * @return aggregated BPM records.
     */
    suspend fun aggregateBpmRecords(dateRange: DateRange, limit: Int): BpmRecords {
        Timber.d("aggregateBpmRecords: dateRange=$dateRange, limit=$limit")

        val recordsId = RecordsId(dateRange, limit)
        val topNotes = topNotesCache[recordsId]
            ?: noteRepository.listTopBpmSuspending(limit, dateRange)
                .also { topNotesCache[recordsId] = it }

        return BpmRecords(topNotes = topNotes)
    }

    /**
     * Aggregate records of the count of exercise-related notes.
     *
     * @param dateRange the date range over which records should be aggregated.
     * @param limit maximum count of records.
     * @return aggregated records of the count of exercise-related notes.
     */
    suspend fun aggregateNoteCountRecords(dateRange: DateRange, limit: Int): NoteCountRecords {
        Timber.d("aggregateNoteCountRecords: dateRange=$dateRange, limit=$limit")

        val recordsId = RecordsId(dateRange, limit)
        val topExerciseNames = topExerciseNamesCache[recordsId]
            ?: noteRepository.listTopPopularExercisesSuspending(limit, dateRange)
                .map { NoteCountRecords.Record(it.exerciseName, it.noteCount) }
                .also { topExerciseNamesCache[recordsId] = it }

        return NoteCountRecords(topExerciseNames = topExerciseNames)
    }

    /**
     * Clear all cached values.
     */
    fun clearCache() {
        Timber.d("clearCache")
        topNotesCache.clear()
        topExerciseNamesCache.clear()
    }

    private data class RecordsId(val dateRange: DateRange, val limit: Int)
}
