package com.adkom666.shrednotes.data.repository

import com.adkom666.shrednotes.data.converter.toExercise
import com.adkom666.shrednotes.data.converter.toExerciseEntity
import com.adkom666.shrednotes.data.db.dao.ExerciseDao
import com.adkom666.shrednotes.data.db.entity.ExerciseEntity
import com.adkom666.shrednotes.data.model.Exercise
import com.adkom666.shrednotes.util.paging.Page
import kotlinx.coroutines.flow.Flow

/**
 * Implementation of [ExerciseRepository].
 *
 * @property exerciseDao object for performing operations with exercises in the database.
 */
class ExerciseRepositoryImpl(private val exerciseDao: ExerciseDao) : ExerciseRepository {

    override val countFlow: Flow<Int>
        get() = exerciseDao.countAsFlow()

    override suspend fun countSuspending(subname: String?): Int {
        return subname?.let {
            exerciseDao.countBySubnameSuspending(it)
        } ?: exerciseDao.countSuspending()
    }

    override fun page(
        size: Int,
        requestedStartPosition: Int,
        subname: String?
    ): Page<Exercise> {
        val exerciseEntityPage = exerciseDao.page(
            size,
            requestedStartPosition,
            subname
        )
        return Page(
            exerciseEntityPage.items.map(ExerciseEntity::toExercise),
            exerciseEntityPage.offset
        )
    }

    override fun list(
        size: Int,
        startPosition: Int,
        subname: String?
    ): List<Exercise> {
        val exerciseEntityList = exerciseDao.list(
            size,
            startPosition,
            subname
        )
        return exerciseEntityList.map(ExerciseEntity::toExercise)
    }

    override suspend fun saveIfNoSuchNameSuspending(exercise: Exercise): Boolean {
        val exerciseEntity = exercise.toExerciseEntity()
        return exerciseDao.upsertIfNoSuchNameSuspending(exerciseEntity)
    }

    override suspend fun deleteSuspending(ids: List<Long>, subname: String?): Int {
        return exerciseDao.deleteSuspending(ids, subname)
    }

    override suspend fun deleteOtherSuspending(ids: List<Long>, subname: String?): Int {
        return exerciseDao.deleteOtherSuspending(ids, subname)
    }
}
