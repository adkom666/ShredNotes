package com.adkom666.shrednotes.statistics

import com.adkom666.shrednotes.data.model.Note
import com.adkom666.shrednotes.data.repository.NoteRepository
import com.adkom666.shrednotes.statistics.util.endDateInclusive
import com.adkom666.shrednotes.statistics.util.startDateInclusive
import com.adkom666.shrednotes.util.ALL_WEEKDAYS
import com.adkom666.shrednotes.util.DateRange
import com.adkom666.shrednotes.util.Weekday
import com.adkom666.shrednotes.util.time.Days
import com.adkom666.shrednotes.util.weekday
import timber.log.Timber
import java.util.Calendar

/**
 * Get statistics by days of week here.
 *
 * @property noteRepository note storage.
 */
class WeekdaysStatisticsAggregator(
    private val noteRepository: NoteRepository
) {
    private var firstNoteDateCache: Array<Days?>? = null
    private var lastNoteDateCache: Array<Days?>? = null
    private var averageAmongMaxBpmCache: MutableMap<DateRange, WeekdayValues> = mutableMapOf()
    private var averageAmongNoteCountCache: MutableMap<DateRange, WeekdayValues> = mutableMapOf()

    /**
     * Aggregate statistics of the average among maximum BPM by days of week.
     *
     * @param dateRange the date range over which statistics should be aggregated.
     * @return aggregated statistics of the average among maximum BPM by days of week.
     */
    suspend fun aggregateAverageAmongMaxBpm(dateRange: DateRange): WeekdaysStatistics {
        Timber.d("aggregateAverageAmongMaxBpm: dateRange=$dateRange")
        val averageAmongMaxBpm = averageAmongMaxBpmCache[dateRange]
            ?: aggregateAverageQuantity(dateRange) { noteList ->
                noteList.maxOfOrNull { it.bpm }
            }.also { averageAmongMaxBpmCache[dateRange] = it }
        return WeekdaysStatistics(valueMap = averageAmongMaxBpm)
    }

    /**
     * Aggregate statistics of the average note count by days of week.
     *
     * @param dateRange the date range over which statistics should be aggregated.
     * @return aggregated statistics of the average note count by days of week.
     */
    suspend fun aggregateAverageNoteCount(dateRange: DateRange): WeekdaysStatistics {
        Timber.d("aggregateAverageNoteCount: dateRange=$dateRange")
        val averageAmongNoteCount = averageAmongNoteCountCache[dateRange]
            ?: aggregateAverageQuantity(dateRange) { noteList ->
                noteList.count()
            }.also { averageAmongNoteCountCache[dateRange] = it }
        return WeekdaysStatistics(valueMap = averageAmongNoteCount)
    }

    /**
     * Clear all cached values.
     */
    fun clearCache() {
        Timber.d("clearCache")
        firstNoteDateCache = null
        lastNoteDateCache = null
        averageAmongMaxBpmCache.clear()
        averageAmongNoteCountCache.clear()
    }

    private suspend fun aggregateAverageQuantity(
        dateRange: DateRange,
        calcQuantity: (List<Note>) -> Int?
    ): WeekdayValues {
        val daysToNotesMap = daysToNotesMap(dateRange)
        Timber.d("daysToNotesMap=$daysToNotesMap")
        val startDate = firstDate()?.let {
            startDateInclusive(dateRange.fromInclusive, it)
        }
        val endDate = lastDate()?.let {
            endDateInclusive(dateRange.toExclusive, it)
        }
        val weekdayToQuantityListMap = if (startDate != null && endDate != null) {
            weekdayToQuantityListMap(daysToNotesMap, startDate, endDate, calcQuantity)
        } else {
            emptyMap()
        }

        val averageQuantity = mutableMapOf<Weekday, Float>()
        ALL_WEEKDAYS.forEach { weekday ->
            val quantityList = weekdayToQuantityListMap[weekday]
            Timber.d("weekday=$weekday, quantityList=$quantityList")
            if (quantityList.isNullOrEmpty().not()) {
                quantityList?.average()?.let {
                    averageQuantity[weekday] = it.toFloat()
                }
            }
            Timber.d("averageQuantity[$weekday]=${averageQuantity[weekday]}")
        }
        return averageQuantity
    }

    private suspend fun daysToNotesMap(dateRange: DateRange): Map<Days, List<Note>> {
        val notes = noteRepository.listUnorderedSuspending(dateRange)
        val daysToNotesMap = mutableMapOf<Days, MutableList<Note>>()
        notes.forEach { note ->
            val days = Days(note.dateTime)
            val noteList = daysToNotesMap[days]
                ?: mutableListOf<Note>()
                    .also { daysToNotesMap[days] = it }
            noteList.add(note)
        }
        return daysToNotesMap
    }

    private fun weekdayToQuantityListMap(
        daysToNotesMap: Map<Days, List<Note>>,
        startDate: Days,
        endDate: Days,
        calcQuantity: (List<Note>) -> Int?
    ): Map<Weekday, List<Int>> {
        val weekdayToQuantityListMap = weekdayToEmptyIntListMap()
        val noNoteList = emptyList<Note>()
        val calendar = Calendar.getInstance()
        var days = startDate
        while (days < endDate) {
            val noteList = daysToNotesMap[days] ?: noNoteList
            calcQuantity(noteList)?.addToListFrom(weekdayToQuantityListMap, days, calendar)
            days = days.tomorrow
        }
        daysToNotesMap[endDate]?.let { noteList ->
            calcQuantity(noteList)?.addToListFrom(weekdayToQuantityListMap, days, calendar)
        }
        return weekdayToQuantityListMap
    }

    private fun weekdayToEmptyIntListMap(): Map<Weekday, MutableList<Int>> {
        val weekdayToIntListMap = mutableMapOf<Weekday, MutableList<Int>>()
        ALL_WEEKDAYS.forEach { weekday ->
            weekdayToIntListMap[weekday] = mutableListOf()
        }
        return weekdayToIntListMap
    }

    private suspend fun firstDate(): Days? {
        return firstNoteDateCache?.first()
            ?: noteRepository.firstNoteDateSuspending()
                .also { firstNoteDateCache = arrayOf(it) }
    }

    private suspend fun lastDate(): Days? {
        return lastNoteDateCache?.first()
            ?: noteRepository.lastNoteDateSuspending()
                .also { lastNoteDateCache = arrayOf(it) }
    }

    private fun Int.addToListFrom(
        weekdayToQuantityListMap: Map<Weekday, MutableList<Int>>,
        days: Days,
        calendar: Calendar
    ) {
        val weekday = days.date.weekday(calendar)
        weekdayToQuantityListMap[weekday]?.add(this)
    }
}
