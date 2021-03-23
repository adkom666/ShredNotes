package com.adkom666.shrednotes.data.external

import com.adkom666.shrednotes.common.Id

/**
 * Exercise model to store separately from the application.
 *
 * @property id unique identifier of the exercise.
 * @property name name of this exercise.
 */
data class ExternalExercise(
    val id: Id,
    val name: String
)
