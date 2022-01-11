package com.adkom666.shrednotes.statistics

import com.adkom666.shrednotes.data.model.Note
import com.adkom666.shrednotes.data.model.NoteFilter
import com.adkom666.shrednotes.data.repository.ExerciseRepository
import com.adkom666.shrednotes.data.repository.NoteRepository
import com.adkom666.shrednotes.statistics.util.endDateInclusive
import com.adkom666.shrednotes.statistics.util.startDateInclusive
import com.adkom666.shrednotes.util.DateRange
import com.adkom666.shrednotes.util.time.Days
import com.adkom666.shrednotes.util.time.timestampOrMax
import com.adkom666.shrednotes.util.time.timestampOrMin
import timber.log.Timber
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
    private var lastNoteDateCache: Array<Days?>? = null
    private var allNoteCountCache: Int? = null
    private var allExerciseCountCache: Int? = null

    /**
     * Aggregate common statistics.
     *
     * @param dateRange the date range over which statistics should be aggregated.
     * @return aggregated common statistics.
     */
    suspend fun aggregate(dateRange: DateRange): CommonStatistics {

        Timber.d("aggregate: dateRange=$dateRange")

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

        val lastNoteDate = lastNoteDateCache?.first()
            ?: noteRepository.lastNoteDateSuspending()
                .also { lastNoteDateCache = arrayOf(it) }

        val allNoteCount = allNoteCountCache
            ?: noteRepository.countSuspending()
                .also { allNoteCountCache = it }

        val allExerciseCount = allExerciseCountCache
            ?: exerciseRepository.countSuspending()
                .also { allExerciseCountCache = it }

        val dayCount = if (firstNoteDate != null && lastNoteDate != null) {
            daysInRange(dateRange, firstNoteDate, lastNoteDate)
        } else {
            0
        }

        val fromEpochMillisInclusive = dateRange.fromInclusive.timestampOrMin()
        val toEpochMillisExclusive = dateRange.toExclusive.timestampOrMax()
        val notes = allNotes.filter {
            it.dateTime.epochMillis in fromEpochMillisInclusive until toEpochMillisExclusive
        }
        val activeDaysSet = mutableSetOf<Days>()
        notes.forEach { note ->
            val days = Days(note.dateTime)
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
        Timber.d("clearCache")
        noteCountCache.clear()
        relatedExerciseCountCache.clear()
        allNotesCache = null
        firstNoteDateCache = null
        lastNoteDateCache = null
        allNoteCountCache = null
        allExerciseCountCache = null
    }

    private fun daysInRange(dateRange: DateRange, firstDate: Days, lastDate: Days): Int {
        val startDate = startDateInclusive(dateRange.fromInclusive, firstDate)
        val endDate = endDateInclusive(dateRange.toExclusive, lastDate)
        val start = startDate.epochMillis.toDuration(DurationUnit.MILLISECONDS)
        val end = endDate.epochMillis.toDuration(DurationUnit.MILLISECONDS)
        return if (end >= start) {
            val totalDuration = end - start
            totalDuration.inWholeDays.toInt() + 1
        } else {
            0
        }
    }
}
