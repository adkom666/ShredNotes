package com.adkom666.shrednotes.util

import androidx.annotation.StringRes
import com.adkom666.shrednotes.R
import java.util.Calendar
import java.util.Date

val ALL_WEEKDAYS = Weekday.values()

/**
 * Getting the [Weekday] according to the [calendar].
 *
 * @param calendar [Calendar] to calculate weekday.
 * @return [Weekday] according to the [calendar].
 */
fun Date.weekday(calendar: Calendar): Weekday {
    calendar.time = this
    val weekdayNumber = calendar.get(Calendar.DAY_OF_WEEK)
    return ALL_WEEKDAYS[weekdayNumber - 1]
}

/**
 * Getting the string resource reference to the title of this [Weekday].
 *
 * @return string resource reference to the title of this [Weekday].
 */
@StringRes
fun Weekday.titleResId(): Int = when (this) {
    Weekday.SUNDAY -> R.string.title_sunday
    Weekday.MONDAY -> R.string.title_monday
    Weekday.TUESDAY -> R.string.title_tuesday
    Weekday.WEDNESDAY -> R.string.title_wednesday
    Weekday.THURSDAY -> R.string.title_thursday
    Weekday.FRIDAY -> R.string.title_friday
    Weekday.SATURDAY -> R.string.title_saturday
}

/**
 * Getting the string resource reference to the label of this [Weekday].
 *
 * @return string resource reference to the label of this [Weekday].
 */
@StringRes
fun Weekday.labelResId(): Int = when (this) {
    Weekday.SUNDAY -> R.string.label_sunday
    Weekday.MONDAY -> R.string.label_monday
    Weekday.TUESDAY -> R.string.label_tuesday
    Weekday.WEDNESDAY -> R.string.label_wednesday
    Weekday.THURSDAY -> R.string.label_thursday
    Weekday.FRIDAY -> R.string.label_friday
    Weekday.SATURDAY -> R.string.label_saturday
}
