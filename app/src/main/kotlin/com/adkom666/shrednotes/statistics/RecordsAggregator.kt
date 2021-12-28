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
    private var topNoteCountCache: MutableMap<DateRange, Int> = mutableMapOf()
    private var topExerciseNameCountCache: MutableMap<DateRange, Int> = mutableMapOf()

    /**
     * Aggregate BPM records.
     *
     * @param dateRange the date range over which records should be aggregated.
     * @param startPosition position of the first record in the list of target records.
     * @param limit maximum count of records.
     * @return aggregated BPM records.
     */
    suspend fun aggregateBpmRecords(
        dateRange: DateRange,
        startPosition: Int,
        limit: Int
    ): BpmRecords {
        Timber.d("aggregateBpmRecords: dateRange=$dateRange, limit=$limit")

        val recordsId = RecordsId(
            dateRange = dateRange,
            startPosition = startPosition,
            limit = limit
        )
        val topNotes = topNotesCache[recordsId]
            ?: noteRepository.listTopBpmSuspending(
                size = limit,
                startPosition = startPosition,
                dateRange = dateRange
            ).also { topNotesCache[recordsId] = it }

        return BpmRecords(topNotes = topNotes)
    }

    /**
     * Aggregate records of the count of exercise-related notes.
     *
     * @param dateRange the date range over which records should be aggregated.
     * @param startPosition position of the first record in the list of target records.
     * @param limit maximum count of records.
     * @return aggregated records of the count of exercise-related notes.
     */
    suspend fun aggregateNoteCountRecords(
        dateRange: DateRange,
        startPosition: Int,
        limit: Int
    ): NoteCountRecords {
        Timber.d("aggregateNoteCountRecords: dateRange=$dateRange, limit=$limit")

        val recordsId = RecordsId(
            dateRange = dateRange,
            startPosition = startPosition,
            limit = limit
        )
        val topExerciseNames = topExerciseNamesCache[recordsId]
            ?: noteRepository.listTopPopularExercisesSuspending(
                size = limit,
                startPosition = startPosition,
                dateRange = dateRange
            ).map { NoteCountRecords.Record(it.exerciseName, it.noteCount) }
                .also { topExerciseNamesCache[recordsId] = it }

        return NoteCountRecords(topExerciseNames = topExerciseNames)
    }

    /**
     * Count of BPM records over [dateRange].
     *
     * @param dateRange the date range of the target records.
     * @return count of BPM records over [dateRange].
     */
    suspend fun bpmRecordCount(dateRange: DateRange): Int {
        Timber.d("bpmRecordCount: dateRange=$dateRange")
        return topNoteCountCache[dateRange]
            ?: noteRepository.countTopBpmSuspending(dateRange)
                .also { topNoteCountCache[dateRange] = it }
    }

    /**
     * Count of note count records over [dateRange].
     *
     * @param dateRange the date range of the target records.
     * @return count of note count records over [dateRange].
     */
    suspend fun noteCountRecordCount(dateRange: DateRange): Int {
        Timber.d("noteCountRecordCount: dateRange=$dateRange")
        return topExerciseNameCountCache[dateRange]
            ?: noteRepository.countTopPopularExercisesSuspending(dateRange)
                .also { topExerciseNameCountCache[dateRange] = it }
    }

    /**
     * Clear all cached values.
     */
    fun clearCache() {
        Timber.d("clearCache")
        topNotesCache.clear()
        topExerciseNamesCache.clear()
        topNoteCountCache.clear()
        topExerciseNameCountCache.clear()
    }

    private data class RecordsId(
        val dateRange: DateRange,
        val startPosition: Int,
        val limit: Int
    )
}
