package com.adkom666.shrednotes.data.converter

import com.adkom666.shrednotes.data.db.entity.ExerciseEntity
import com.adkom666.shrednotes.data.external.ExternalExerciseV1
import com.adkom666.shrednotes.data.model.Exercise

/**
 * Getting an exercise from a database entity.
 */
fun ExerciseEntity.toExercise(): Exercise = Exercise(
    id = id,
    name = name
)

/**
 * Getting a database entity from an exercise.
 */
fun Exercise.toExerciseEntity(): ExerciseEntity = ExerciseEntity(
    id = id,
    name = name
)

/**
 * Getting an external exercise (version 1) from a database entity.
 */
fun ExerciseEntity.toExternalExerciseV1(): ExternalExerciseV1 = ExternalExerciseV1(
    id = id,
    name = name
)

/**
 * Getting a database entity from an external exercise (version 1).
 */
fun ExternalExerciseV1.toExerciseEntity(): ExerciseEntity = ExerciseEntity(
    id = id,
    name = name
)

