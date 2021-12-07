package com.adkom666.shrednotes.statistics

import com.adkom666.shrednotes.util.time.Days

typealias MaxBpmTrackingPoints = List<MaxBpmTracking.Point>

/**
 * Maximum BPM per day tracking.
 *
 * @property points values of maximum BPM per day.
 */
data class MaxBpmTracking(
    val points: MaxBpmTrackingPoints
) {
    /**
     * Maximum BPM on a specific day.
     *
     * @property day target day.
     * @property maxBpm maximum BPM that [day].
     */
    data class Point(
        val day: Days,
        val maxBpm: Int
    )
}
