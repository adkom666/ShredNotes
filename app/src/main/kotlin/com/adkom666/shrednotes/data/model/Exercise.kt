package com.adkom666.shrednotes.data.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * Model of the exercise.
 *
 * @property id unique identifier of the exercise.
 * @property name name of this exercise.
 */
@Parcelize
data class Exercise(
    val id: Long = 0L,
    val name: String = ""
) : Parcelable
