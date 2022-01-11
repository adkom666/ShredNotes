package com.adkom666.shrednotes.statistics

import com.adkom666.shrednotes.common.Id
import com.adkom666.shrednotes.data.repository.NoteRepository
import com.adkom666.shrednotes.statistics.util.endDateInclusive
import com.adkom666.shrednotes.statistics.util.startDateInclusive
import com.adkom666.shrednotes.util.DateRange
import com.adkom666.shrednotes.util.time.Days
import timber.log.Timber

/**
 * Get statistics tracking here.
 *
 * @property noteRepository note storage.
 */
class TrackingAggregator(
    private val noteRepository: NoteRepository
) {
    private var firstNoteDateCache: Array<Days?>? = null
    private var lastNoteDateCache: Array<Days?>? = null

    private var maxBpmTrackingPointsCache:
            MutableMap<TrackingPointsId, MaxBpmTrackingPoints> = mutableMapOf()

    private var noteCountTrackingPointsCache:
            MutableMap<TrackingPointsId, NoteCountTrackingPoints> = mutableMapOf()

    /**
     * Aggregate statistics of the maximum BPM per day.
     *
     * @param dateRange the date range over which statistics should be aggregated.
     * @param exerciseId the identifier of exercise for which statistics should be aggregated. If
     * value is null then notes for all exercises will be processed.
     * @return aggregated statistics of the maximum BPM per day.
     */
    suspend fun aggregateMaxBpmTracking(
        dateRange: DateRange,
        exerciseId: Id?
    ): MaxBpmTracking {
        Timber.d("aggregateMaxBpmTracking: dateRange=$dateRange, exerciseId=$exerciseId")

        val trackingPointsId = TrackingPointsId(dateRange, exerciseId)
        val points = maxBpmTrackingPointsCache[trackingPointsId]
            ?: aggregateMaxBpmTrackingPoints(dateRange, exerciseId)
                .also { maxBpmTrackingPointsCache[trackingPointsId] = it }

        return MaxBpmTracking(points = points)
    }

    /**
     * Aggregate statistics of the note count per day.
     *
     * @param dateRange the date range over which statistics should be aggregated.
     * @param exerciseId the identifier of exercise for which statistics should be aggregated. If
     * value is null then notes for all exercises will be processed.
     * @return aggregated statistics of the note count per day.
     */
    suspend fun aggregateNoteCountTracking(
        dateRange: DateRange,
        exerciseId: Id?
    ): NoteCountTracking {
        Timber.d("aggregateNoteCountTracking: dateRange=$dateRange, exerciseId=$exerciseId")

        val trackingPointsId = TrackingPointsId(dateRange, exerciseId)
        val points = noteCountTrackingPointsCache[trackingPointsId]
            ?: aggregateNoteCountTrackingPoints(dateRange, exerciseId)
                .also{ noteCountTrackingPointsCache[trackingPointsId] = it }

        return NoteCountTracking(points = points)
    }

    /**
     * Clear all cached values.
     */
    fun clearCache() {
        Timber.d("clearCache")
        firstNoteDateCache = null
        lastNoteDateCache = null
        maxBpmTrackingPointsCache.clear()
        noteCountTrackingPointsCache.clear()
    }

    private suspend fun aggregateMaxBpmTrackingPoints(
        dateRange: DateRange,
        exerciseId: Id?
    ): MaxBpmTrackingPoints {
        val notes = noteRepository.listUnorderedSuspending(dateRange, exerciseId)
        val bpmMap = mutableMapOf<Days, Int>()
        notes.forEach { note ->
            val days = Days(note.dateTime)
            val previousBpm = bpmMap[days]
            if (previousBpm == null || previousBpm < note.bpm) {
                bpmMap[days] = note.bpm
            }
        }
        val daysList = bpmMap.keys.toMutableList()
        daysList.sortBy { it.epochMillis }
        daysList.ensureEdges(dateRange)
        return daysList.map { days ->
            MaxBpmTracking.Point(
                days = days,
                maxBpm = bpmMap[days] ?: 0
            )
        }
    }

    private suspend fun aggregateNoteCountTrackingPoints(
        dateRange: DateRange,
        exerciseId: Id?
    ): NoteCountTrackingPoints {
        val notes = noteRepository.listUnorderedSuspending(dateRange, exerciseId)
        val noteCountMap = mutableMapOf<Days, Int>()
        notes.forEach { note ->
            val days = Days(note.dateTime)
            val previousNoteCount = noteCountMap[days] ?: 0
            noteCountMap[days] = previousNoteCount + 1
        }
        val daysList = noteCountMap.keys.toMutableList()
        daysList.sortBy { it.epochMillis }
        daysList.ensureEdges(dateRange)
        return daysList.map { days ->
            NoteCountTracking.Point(
                days = days,
                noteCount = noteCountMap[days] ?: 0
            )
        }
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

    private suspend fun MutableList<Days>.ensureEdges(dateRange: DateRange) {
        if (isNotEmpty()) {
            val startDate = firstDate()?.let {
                startDateInclusive(dateRange.fromInclusive, it)
            }
            startDate?.let { date ->
                if (date != firstOrNull()) {
                    add(0, date)
                }
            }
            val endDate = lastDate()?.let {
                endDateInclusive(dateRange.toExclusive, it)
            }
            endDate?.let { date ->
                if (date != lastOrNull()) {
                    add(date)
                }
            }
        }
    }

    private data class TrackingPointsId(val dateRange: DateRange, val exerciseId: Id?)
}
