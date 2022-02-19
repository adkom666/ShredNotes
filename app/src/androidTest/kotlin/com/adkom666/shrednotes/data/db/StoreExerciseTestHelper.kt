package com.adkom666.shrednotes.data.db

import com.adkom666.shrednotes.data.converter.toExerciseEntity
import com.adkom666.shrednotes.data.db.dao.ExerciseDao
import com.adkom666.shrednotes.data.db.entity.ExerciseEntity
import com.adkom666.shrednotes.data.model.Exercise

object StoreExerciseTestHelper {

    const val EXERCISE_NAME_PREFIX = "Test Exercise "
    const val EXERCISE_NAME_NOT_CONSISTS_IT = "xxx"
    const val PROBABLE_EXERCISE_SUBNAME = "1"
    const val EXERCISE_COUNT = 666

    fun insertExercises(exerciseDao: ExerciseDao) {
        (1..EXERCISE_COUNT).forEach { number ->
            val exerciseEntity = newExerciseEntity(number)
            exerciseDao.insert(exerciseEntity)
        }
    }

    private fun newExerciseEntity(number: Int): ExerciseEntity {
        val exerciseName = "$EXERCISE_NAME_PREFIX$number"
        val exercise = Exercise(name = exerciseName)
        return exercise.toExerciseEntity()
    }
}
