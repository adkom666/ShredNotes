package com.adkom666.shrednotes.data.model

import android.os.Parcelable
import com.adkom666.shrednotes.util.time.Days
import kotlinx.parcelize.Parcelize

/**
 * Model of the note filter.
 *
 * @property dateFromInclusive if this property is not null, then notes with a date less than this
 * will be filtered out.
 * @property dateToExclusive if this property is not null, then notes with a date greater than or
 * equal to this will be filtered out.
 * @property bpmFromInclusive if this property is not null, then notes with a BPM less than this
 * will be filtered out.
 * @property bpmToInclusive if this property is not null, then notes with a BPM greater than this
 * will be filtered out.
 */
@Parcelize
data class NoteFilter(
    val dateFromInclusive: Days? = null,
    val dateToExclusive: Days? = null,
    val bpmFromInclusive: Int? = null,
    val bpmToInclusive: Int? = null
) : Parcelable {

    /**
     * The undefined filter does not filter notes.
     */
    val isDefined: Boolean
        get() = isDateRangeDefined || isBpmRangeDefined

    /**
     * Filter with the undefined date range does not filter notes by date.
     */
    val isDateRangeDefined: Boolean
        get() = arrayOf(
            dateFromInclusive,
            dateToExclusive
        ).filterNotNull().isNotEmpty()

    /**
     * Filter with the undefined BPM range does not filter notes by BPM.
     */
    val isBpmRangeDefined: Boolean
        get() = arrayOf(
            bpmFromInclusive,
            bpmToInclusive
        ).filterNotNull().isNotEmpty()
}
