package com.adkom666.shrednotes.statistics

import com.adkom666.shrednotes.data.model.Note
import com.adkom666.shrednotes.data.repository.ExerciseRepository
import com.adkom666.shrednotes.data.repository.NoteRepository
import com.adkom666.shrednotes.util.time.Days
import com.adkom666.shrednotes.util.time.Minutes
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

/**
 * Get common statistics here.
 *
 * @property noteRepository note storage.
 * @property exerciseRepository exercise storage.
 */
@ExperimentalTime
class CommonStatisticsAggregator(
    private val noteRepository: NoteRepository,
    private val exerciseRepository: ExerciseRepository
) {
    private var totalNotesCache: Int? = null
    private var totalExercisesCache: Int? = null
    private var notesCache: List<Note>? = null

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

        val notes = notesCache
            ?: noteRepository.listAllUnorderedSuspending()
                .also { notesCache = it }

        val totalDays = notes.minByOrNull {
            it.dateTime.epochMillis
        }?.let { firstNote ->
            val start = firstNote.dateTime.epochMillis.toDuration(DurationUnit.MILLISECONDS)
            val now = Minutes().epochMillis.toDuration(DurationUnit.MILLISECONDS)
            val totalDuration = now - start
            totalDuration.inWholeDays.toInt() + 1
        } ?: 0

        val activeDaysSet = mutableSetOf<Days>()
        notes.forEach { note ->
            val days = Days(note.dateTime.date)
            activeDaysSet.add(days)
        }
        val activeDays = activeDaysSet.size

        val activeDaysShare = if (totalDays > 0) {
            activeDays.toFloat() / totalDays
        } else {
            0f
        }

        return CommonStatistics(
            totalNotes = totalNotes,
            totalExercises = totalExercises,
            totalDays = totalDays,
            activeDays = activeDays,
            activeDaysShare = activeDaysShare
        )
    }

    /**
     * Clear all cached values.
     */
    fun clearCache() {
        totalNotesCache = null
        totalExercisesCache = null
        notesCache = null
    }
}
