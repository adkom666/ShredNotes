package com.adkom666.shrednotes.statistics

import com.adkom666.shrednotes.data.model.Note
import com.adkom666.shrednotes.data.model.NoteFilter
import com.adkom666.shrednotes.data.repository.ExerciseRepository
import com.adkom666.shrednotes.data.repository.NoteRepository
import com.adkom666.shrednotes.util.DateRange
import com.adkom666.shrednotes.util.time.Days
import com.adkom666.shrednotes.util.time.timestampOrMax
import com.adkom666.shrednotes.util.time.timestampOrMin
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
    private val noteCountCache: MutableMap<DateRange, Int?> = mutableMapOf()
    private val relatedExerciseCountCache: MutableMap<DateRange, Int?> = mutableMapOf()
    private var allNotesCache: List<Note>? = null
    private var firstNoteDateCache: Array<Days?>? = null
    private var allNoteCountCache: Int? = null
    private var allExerciseCountCache: Int? = null

    /**
     * Aggregate common statistics.
     *
     * @param dateRange the date range over which statistics should be aggregated.
     * @return aggregated common statistics.
     */
    suspend fun aggregate(dateRange: DateRange): CommonStatistics {

        val filter = NoteFilter(
            dateFromInclusive = dateRange.fromInclusive,
            dateToExclusive = dateRange.toExclusive
        )

        val noteCount = noteCountCache[dateRange]
            ?: noteRepository.countSuspending(filter = filter)
                .also { noteCountCache[dateRange] = it }

        val relatedExerciseCount = relatedExerciseCountCache[dateRange]
            ?: exerciseRepository.countByRelatedNoteDateSuspending(dateRange)
                .also { relatedExerciseCountCache[dateRange] = it }

        val allNotes = allNotesCache
            ?: noteRepository.listAllUnorderedSuspending()
                .also { allNotesCache = it }

        val firstNoteDate = firstNoteDateCache?.first()
            ?: noteRepository.firstNoteDateSuspending()
                .also { firstNoteDateCache = arrayOf(it) }

        val allNoteCount = allNoteCountCache
            ?: noteRepository.countSuspending()
                .also { allNoteCountCache = it }

        val allExerciseCount = allExerciseCountCache
            ?: exerciseRepository.countSuspending()
                .also { allExerciseCountCache = it }

        val dayCount = firstNoteDate?.let {
            daysInRange(dateRange, it)
        } ?: 0

        val fromEpochMillisInclusive = dateRange.fromInclusive.timestampOrMin()
        val toEpochMillisExclusive = dateRange.toExclusive.timestampOrMax()
        val notes = allNotes.filter {
            it.dateTime.epochMillis in fromEpochMillisInclusive until toEpochMillisExclusive
        }
        val activeDaysSet = mutableSetOf<Days>()
        notes.forEach { note ->
            val days = Days(note.dateTime.date)
            activeDaysSet.add(days)
        }
        val activeDayCount = activeDaysSet.size

        val activeDaysShare = if (dayCount > 0) {
            activeDayCount.toFloat() / dayCount
        } else {
            null
        }

        return CommonStatistics(
            notes = noteCount,
            relatedExercises = relatedExerciseCount,
            days = dayCount,
            activeDays = activeDayCount,
            activeDaysShare = activeDaysShare,
            allNotes = allNoteCount,
            allExercises = allExerciseCount
        )
    }

    /**
     * Clear all cached values.
     */
    fun clearCache() {
        noteCountCache.clear()
        relatedExerciseCountCache.clear()
        allNotesCache = null
        firstNoteDateCache = null
        allNoteCountCache = null
        allExerciseCountCache = null
    }

    private fun daysInRange(dateRange: DateRange, firstDate: Days): Int {
        val startDate = if (
            dateRange.fromInclusive == null ||
            dateRange.fromInclusive.epochMillis < firstDate.epochMillis
        ) {
            firstDate
        } else {
            dateRange.fromInclusive
        }
        val nowDate = Days()
        val toDateInclusive = dateRange.toExclusive?.yesterday
        val endDate = if (
            toDateInclusive == null ||
            toDateInclusive.epochMillis > nowDate.epochMillis
        ) {
            nowDate
        } else {
            toDateInclusive
        }
        val start = startDate.epochMillis.toDuration(DurationUnit.MILLISECONDS)
        val end = endDate.epochMillis.toDuration(DurationUnit.MILLISECONDS)
        val totalDuration = end - start
        return totalDuration.inWholeDays.toInt() + 1
    }
}
