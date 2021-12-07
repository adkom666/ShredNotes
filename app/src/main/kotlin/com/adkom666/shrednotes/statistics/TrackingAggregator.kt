package com.adkom666.shrednotes.statistics

import com.adkom666.shrednotes.data.repository.NoteRepository
import com.adkom666.shrednotes.util.DateRange
import com.adkom666.shrednotes.util.time.Days
import kotlin.random.Random

/**
 * Get statistics tracking here.
 *
 * @property noteRepository note storage.
 */
class TrackingAggregator(
    private val noteRepository: NoteRepository
) {
    /**
     * Aggregate statistics of the maximum BPM per day.
     *
     * @param dateRange the date range over which statistics should be aggregated.
     * @return aggregated statistics of the maximum BPM per day.
     */
    suspend fun aggregateMaxBpmTracking(dateRange: DateRange): MaxBpmTracking {
        // DUMMY
        val random = Random(System.currentTimeMillis())
        val points = mutableListOf<MaxBpmTracking.Point>()
        var day = Days().tomorrow
        repeat(6) {
            points.add(
                0,
                MaxBpmTracking.Point(
                    day = day,
                    maxBpm = random.nextInt(45, 166)
                )
            )
            day = day.yesterday
        }
        return MaxBpmTracking(points)
    }

    /**
     * Aggregate statistics of the note count per day.
     *
     * @param dateRange the date range over which statistics should be aggregated.
     * @return aggregated statistics of the note count per day.
     */
    suspend fun aggregateNoteCountTracking(dateRange: DateRange): NoteCountTracking {
        // DUMMY
        val random = Random(System.currentTimeMillis())
        val points = mutableListOf<NoteCountTracking.Point>()
        var day = Days().tomorrow
        repeat(666) {
            points.add(
                0,
                NoteCountTracking.Point(
                    day = day,
                    noteCount = random.nextInt(0, 30)
                )
            )
            day = day.yesterday
        }
        return NoteCountTracking(points)
    }

    /**
     * Clear all cached values.
     */
    fun clearCache() {
    }
}
