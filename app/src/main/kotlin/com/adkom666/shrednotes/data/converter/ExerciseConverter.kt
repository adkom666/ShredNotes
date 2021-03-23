package com.adkom666.shrednotes.data.converter

import com.adkom666.shrednotes.data.db.entity.ExerciseEntity
import com.adkom666.shrednotes.data.external.ExternalExercise
import com.adkom666.shrednotes.data.model.Exercise

/**
 * Getting an exercise from a database entity.
 */
fun ExerciseEntity.toExercise(): Exercise = Exercise(id, name)

/**
 * Getting a database entity from an exercise.
 */
fun Exercise.toExerciseEntity(): ExerciseEntity = ExerciseEntity(id, name)

/**
 * Getting an external exercise from a database entity.
 */
fun ExerciseEntity.toExternalExercise(): ExternalExercise = ExternalExercise(id, name)

/**
 * Getting a database entity from an external exercise.
 */
fun ExternalExercise.toExerciseEntity(): ExerciseEntity = ExerciseEntity(id, name)

