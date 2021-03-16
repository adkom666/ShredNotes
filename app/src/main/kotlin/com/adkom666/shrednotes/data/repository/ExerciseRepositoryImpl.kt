package com.adkom666.shrednotes.data.repository

import com.adkom666.shrednotes.common.Id
import com.adkom666.shrednotes.common.toId
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
        get() = exerciseDao.countAllAsFlow()

    override suspend fun countSuspending(subname: String?): Int {
        return exerciseDao.countSuspending(subname)
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

    override suspend fun allExercisesSuspending(): List<Exercise> {
        val exerciseEntityList = exerciseDao.listAllSuspending()
        return exerciseEntityList.map(ExerciseEntity::toExercise)
    }

    override suspend fun exercisesByNameSuspending(name: String): List<Exercise> {
        val exerciseEntityList = exerciseDao.listByNameSuspending(name)
        return exerciseEntityList.map(ExerciseEntity::toExercise)
    }

    override suspend fun saveIfNoSuchNameSuspending(exercise: Exercise): Boolean {
        val exerciseEntity = exercise.toExerciseEntity()
        return exerciseDao.upsertIfNoSuchNameSuspending(exerciseEntity)
    }

    override suspend fun insert(exercise: Exercise): Id {
        val exerciseEntity = exercise.toExerciseEntity()
        return exerciseDao.insert(exerciseEntity).toId()
    }

    override suspend fun deleteSuspending(ids: List<Id>, subname: String?): Int {
        return exerciseDao.deleteSuspending(ids, subname)
    }

    override suspend fun deleteOtherSuspending(ids: List<Id>, subname: String?): Int {
        return exerciseDao.deleteOtherSuspending(ids, subname)
    }
}
