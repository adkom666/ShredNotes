package com.adkom666.shrednotes.data.model

import android.os.Parcelable
import com.adkom666.shrednotes.common.Id
import com.adkom666.shrednotes.common.NO_ID
import kotlinx.parcelize.Parcelize

/**
 * Model of the exercise.
 *
 * @property id unique identifier of the exercise.
 * @property name name of this exercise.
 */
@Parcelize
data class Exercise(
    val id: Id = NO_ID,
    val name: String
) : Parcelable
