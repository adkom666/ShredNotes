package com.adkom666.shrednotes.statistics

import com.adkom666.shrednotes.util.time.Days

typealias NoteCountTrackingPoints = List<NoteCountTracking.Point>

/**
 * Note count per day tracking.
 *
 * @property points values of note count per day.
 */
data class NoteCountTracking(
    val points: NoteCountTrackingPoints
) {
    /**
     * Note count on a specific day.
     *
     * @property days information about target day.
     * @property noteCount note count that day.
     */
    data class Point(
        val days: Days,
        val noteCount: Int
    )
}
