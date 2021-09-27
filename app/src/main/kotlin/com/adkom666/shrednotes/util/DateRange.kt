package com.adkom666.shrednotes.util

import android.os.Parcelable
import com.adkom666.shrednotes.util.time.Days
import kotlinx.parcelize.Parcelize

val INFINITE_DATE_RANGE = DateRange(null, null)

/**
 * Date range in days.
 *
 * @property fromInclusive first day in the range or null if the beginning of the range is infinite.
 * @property toInclusive last day in the range or null if the end of the range is infinite.
 */
@Parcelize
data class DateRange(
    val fromInclusive: Days? = null,
    val toInclusive: Days? = null
) : Parcelable
