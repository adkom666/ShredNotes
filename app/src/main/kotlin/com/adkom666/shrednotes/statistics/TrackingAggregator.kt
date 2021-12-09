package com.adkom666.shrednotes.statistics

import com.adkom666.shrednotes.common.Id
import com.adkom666.shrednotes.data.repository.NoteRepository
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
        Timber.d("Aggregate statistics of the maximum BPM per day")

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
        Timber.d("Aggregate statistics of the note count per day")

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
            val days = Days(note.dateTime.epochMillis)
            val previousBpm = bpmMap[days]
            if (previousBpm == null || previousBpm < note.bpm) {
                bpmMap[days] = note.bpm
            }
        }
        val daysList = bpmMap.keys.toMutableList()
        daysList.sortBy { it.epochMillis }
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
            val days = Days(note.dateTime.epochMillis)
            val previousNoteCount = noteCountMap[days] ?: 0
            noteCountMap[days] = previousNoteCount + 1
        }
        val daysList = noteCountMap.keys.toMutableList()
        daysList.sortBy { it.epochMillis }
        return daysList.map { days ->
            NoteCountTracking.Point(
                days = days,
                noteCount = noteCountMap[days] ?: 0
            )
        }
    }

    private data class TrackingPointsId(val dateRange: DateRange, val exerciseId: Id?)
}
