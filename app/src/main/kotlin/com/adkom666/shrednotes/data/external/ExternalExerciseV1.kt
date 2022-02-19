package com.adkom666.shrednotes.data.external

import com.adkom666.shrednotes.common.Id

/**
 * Exercise model to store separately from the application. Version 1.
 *
 * @property id unique identifier of the exercise.
 * @property name name of this exercise.
 */
data class ExternalExerciseV1(
    val id: Id,
    val name: String
)
