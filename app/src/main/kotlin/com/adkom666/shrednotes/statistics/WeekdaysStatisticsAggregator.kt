package com.adkom666.shrednotes.statistics

import com.adkom666.shrednotes.data.model.Note
import com.adkom666.shrednotes.data.repository.NoteRepository
import com.adkom666.shrednotes.util.DateRange
import com.adkom666.shrednotes.util.time.Days
import timber.log.Timber
import java.util.Calendar
import java.util.Date

/**
 * Get statistics by days of week here.
 *
 * @property noteRepository note storage.
 */
class WeekdaysStatisticsAggregator(
    private val noteRepository: NoteRepository
) {
    private companion object {
        private val ALL_WEEKDAYS = Weekday.values()
    }

    private var averageAmongMaxBpmCache: MutableMap<DateRange, WeekdayValues> = mutableMapOf()
    private var averageAmongNoteCountCache: MutableMap<DateRange, WeekdayValues> = mutableMapOf()

    /**
     * Aggregate statistics of the average among max BPM by days of week.
     *
     * @return aggregated statistics of the average among max BPM by days of week.
     */
    suspend fun aggregateAverageAmongMaxBpm(dateRange: DateRange): WeekdaysStatistics {
        Timber.d("Aggregate statistics of the average among max BPM by days of week")
        val averageAmongMaxBpm = averageAmongMaxBpmCache[dateRange]
            ?: aggregateAverageQuantity(dateRange) { noteList ->
                noteList.map { it.bpm }.maxOrNull()
            }.also { averageAmongMaxBpmCache[dateRange] = it }
        return WeekdaysStatistics(valueMap = averageAmongMaxBpm)
    }

    /**
     * Aggregate statistics of the average note count by days of week.
     *
     * @return aggregated statistics of the average note count by days of week.
     */
    suspend fun aggregateAverageNoteCount(dateRange: DateRange): WeekdaysStatistics {
        Timber.d("Aggregate statistics of the average note count by days of week")
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
        averageAmongMaxBpmCache.clear()
        averageAmongNoteCountCache.clear()
    }

    private suspend fun aggregateAverageQuantity(
        dateRange: DateRange,
        calcQuantity: (List<Note>) -> Int?
    ): WeekdayValues {
        val noteToDaysMap = noteToDaysMap(dateRange)
        Timber.d("noteToDaysMap=$noteToDaysMap")

        val weekdayToQuantityListMap = weekdayToEmptyIntListMap()
        val calendar = Calendar.getInstance()
        noteToDaysMap.keys.forEach { days ->
            val weekday = days.date.toWeekday(calendar)
            noteToDaysMap[days]?.let { noteList ->
                calcQuantity(noteList)?.let { quantity ->
                    weekdayToQuantityListMap[weekday]?.add(quantity)
                }
            }
        }

        val averageQuantity = mutableMapOf<Weekday, Float>()
        ALL_WEEKDAYS.forEach { weekday ->
            val quantityList = weekdayToQuantityListMap[weekday]
            if (quantityList.isNullOrEmpty().not()) {
                quantityList?.average()?.let {
                    averageQuantity[weekday] = it.toFloat()
                }
            }
            Timber.d("weekday=$weekday, quantityList=$quantityList")
            Timber.d("averageQuantity[$weekday]=${averageQuantity[weekday]}")
        }
        return averageQuantity
    }

    private suspend fun noteToDaysMap(dateRange: DateRange): Map<Days, List<Note>> {
        val notes = noteRepository.listUnorderedSuspending(dateRange)
        val noteToDaysMap = mutableMapOf<Days, MutableList<Note>>()
        notes.forEach { note ->
            val days = Days(note.dateTime.epochMillis)
            val noteList = noteToDaysMap[days]
                ?: mutableListOf<Note>()
                    .also { noteToDaysMap[days] = it }
            noteList.add(note)
        }
        return noteToDaysMap
    }

    private fun weekdayToEmptyIntListMap(): Map<Weekday, MutableList<Int>> {
        val weekdayToIntListMap = mutableMapOf<Weekday, MutableList<Int>>()
        ALL_WEEKDAYS.forEach { weekday ->
            weekdayToIntListMap[weekday] = mutableListOf()
        }
        return weekdayToIntListMap
    }

    private fun Date.toWeekday(calendar: Calendar): Weekday {
        calendar.time = this
        val weekdayNumber = calendar.get(Calendar.DAY_OF_WEEK)
        return ALL_WEEKDAYS[weekdayNumber - 1]
    }
}
