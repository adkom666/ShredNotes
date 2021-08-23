package com.adkom666.shrednotes.statistics

import com.adkom666.shrednotes.data.repository.ExerciseRepository
import com.adkom666.shrednotes.data.repository.NoteRepository

/**
 * Get common statistics here.
 *
 * @property noteRepository note storage.
 * @property exerciseRepository exercise storage.
 */
class CommonStatisticsAggregator(
    private val noteRepository: NoteRepository,
    private val exerciseRepository: ExerciseRepository
) {
    private var totalNotesCache: Int? = null
    private var totalExercisesCache: Int? = null

    /**
     * Aggregate common statistics.
     *
     * @return aggregated common statistics.
     */
    suspend fun aggregate(): CommonStatistics {
        val totalNotes = totalNotesCache
            ?: noteRepository.countSuspending()
                .also { totalNotesCache = it }

        val totalExercises = totalExercisesCache
            ?: exerciseRepository.countSuspending()
                .also { totalExercisesCache = it }

        return CommonStatistics(
            totalNotes = totalNotes,
            totalExercises = totalExercises
        )
    }

    /**
     * Clear all cached values.
     */
    fun clearCache() {
        totalNotesCache = null
        totalExercisesCache = null
    }
}
