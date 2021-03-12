package com.adkom666.shrednotes.data.converter

import androidx.room.TypeConverter
import java.util.Date

/**
 * Converting a date to a timestamp and back.
 */
class DateConverter {

    /**
     * Getting a date from a timestamp.
     *
     * @param timestamp timestamp for conversion.
     */
    @TypeConverter
    fun dateFromTimestamp(timestamp: Long?): Date? = timestamp?.let { Date(it) }

    /**
     * Getting a timestamp from a date.
     *
     * @param date [Date] for conversion.
     */
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time
}
