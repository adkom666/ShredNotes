package com.adkom666.shrednotes.statistics

import com.adkom666.shrednotes.data.repository.NoteRepository
import com.adkom666.shrednotes.util.DateRange

/**
 * Get records here.
 *
 * @property noteRepository note storage.
 */
class RecordsAggregator(
    private val noteRepository: NoteRepository
) {
    private var topNotesCache: MutableMap<DateRange, TopNotes> = mutableMapOf()
    private var topExerciseNamesCache: MutableMap<DateRange, TopExerciseNames> = mutableMapOf()

    /**
     * Aggregate BPM records.
     *
     * @param dateRange the date range over which records should be aggregated.
     * @param limit max count of records.
     * @return aggregated BPM records.
     */
    suspend fun aggregateBpmRecords(dateRange: DateRange, limit: Int): BpmRecords {
        val topNotes = topNotesCache[dateRange]
            ?: noteRepository.listTopBpmSuspending(limit, dateRange)
                .also { topNotesCache[dateRange] = it }

        return BpmRecords(
            topNotes = topNotes
        )
    }

    /**
     * Aggregate records of the count of exercise-related notes.
     *
     * @param dateRange the date range over which records should be aggregated.
     * @param limit max count of records.
     * @return aggregated records of the count of exercise-related notes.
     */
    suspend fun aggregateNoteCountRecords(dateRange: DateRange, limit: Int): NoteCountRecords {
        val topExerciseNames = topExerciseNamesCache[dateRange]
            ?: noteRepository.listTopPopularExercisesSuspending(limit, dateRange)
                .map { NoteCountRecords.Record(it.exerciseName, it.noteCount) }
                .also { topExerciseNamesCache[dateRange] = it }

        return NoteCountRecords(
            topExerciseNames = topExerciseNames
        )
    }

    /**
     * Clear all cached values.
     */
    fun clearCache() {
        topNotesCache.clear()
        topExerciseNamesCache.clear()
    }
}
