package com.adkom666.shrednotes.statistics

import com.adkom666.shrednotes.data.model.Note
import com.adkom666.shrednotes.data.repository.NoteRepository

/**
 * Get records here.
 *
 * @property noteRepository note storage.
 */
class RecordsAggregator(
    private val noteRepository: NoteRepository
) {
    private var topNotesCache: List<Note>? = null
    private var topExerciseNamesCache: List<NoteCountRecords.Record>? = null

    /**
     * Aggregate BPM records.
     *
     * @param limit max count of records.
     * @return aggregated BPM records.
     */
    suspend fun aggregateBpmRecords(limit: Int): BpmRecords {
        val topNotes = topNotesCache
            ?: noteRepository.listTopBpmSuspending(limit)
                .also { topNotesCache = it }

        return BpmRecords(
            topNotes = topNotes
        )
    }

    /**
     * Aggregate records of the count of exercise-related notes.
     *
     * @param limit max count of records.
     * @return aggregated records of the count of exercise-related notes.
     */
    suspend fun aggregateNoteCountRecords(limit: Int): NoteCountRecords {
        val topExerciseNames = topExerciseNamesCache
            ?: noteRepository.listTopPopularExercisesSuspending(limit)
                .map { NoteCountRecords.Record(it.exerciseName, it.noteCount) }
                .also { topExerciseNamesCache = it }

        return NoteCountRecords(
            topExerciseNames = topExerciseNames
        )
    }

    /**
     * Clear all cached values.
     */
    fun clearCache() {
        topNotesCache = null
        topExerciseNamesCache = null
    }
}
