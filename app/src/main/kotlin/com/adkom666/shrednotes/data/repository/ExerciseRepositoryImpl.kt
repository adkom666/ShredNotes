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
import timber.log.Timber

/**
 * Implementation of [ExerciseRepository].
 *
 * @property exerciseDao object for performing operations with exercises in the database.
 */
class ExerciseRepositoryImpl(private val exerciseDao: ExerciseDao) : ExerciseRepository {

    override val countFlow: Flow<Int>
        get() = exerciseDao.countAllAsFlow()

    override suspend fun countSuspending(subname: String?): Int {
        Timber.d("countSuspending: subname=$subname")
        val count = exerciseDao.countSuspending(subname)
        Timber.d("count=$count")
        return count
    }

    override fun page(
        size: Int,
        requestedStartPosition: Int,
        subname: String?
    ): Page<Exercise> {
        Timber.d(
            """page:
                |size=$size,
                |requestedStartPosition=$requestedStartPosition,
                |subname=$subname""".trimMargin()
        )
        val exerciseEntityPage = exerciseDao.page(
            size,
            requestedStartPosition,
            subname
        )
        val exercisePage = Page(
            exerciseEntityPage.items.map(ExerciseEntity::toExercise),
            exerciseEntityPage.offset
        )
        Timber.d("exercisePage=$exercisePage")
        return exercisePage
    }

    override fun list(
        size: Int,
        startPosition: Int,
        subname: String?
    ): List<Exercise> {
        Timber.d(
            """list:
                |size=$size,
                |startPosition=$startPosition,
                |subname=$subname""".trimMargin()
        )
        val exerciseEntityList = exerciseDao.list(
            size,
            startPosition,
            subname
        )
        val exerciseList = exerciseEntityList.map(ExerciseEntity::toExercise)
        Timber.d("exerciseList=$exerciseList")
        return exerciseList
    }

    override suspend fun allExercisesSuspending(): List<Exercise> {
        Timber.d("allExercisesSuspending")
        val exerciseEntityList = exerciseDao.listAllSuspending()
        val exerciseList = exerciseEntityList.map(ExerciseEntity::toExercise)
        Timber.d("exerciseList=$exerciseList")
        return exerciseList
    }

    override suspend fun exercisesByNameSuspending(name: String): List<Exercise> {
        Timber.d("exercisesByNameSuspending: name=$name")
        val exerciseEntityList = exerciseDao.listByNameSuspending(name)
        val exerciseList = exerciseEntityList.map(ExerciseEntity::toExercise)
        Timber.d("exerciseList=$exerciseList")
        return exerciseList
    }

    override suspend fun saveIfNoSuchNameSuspending(exercise: Exercise): Boolean {
        Timber.d("saveIfNoSuchNameSuspending: exercise=$exercise")
        val exerciseEntity = exercise.toExerciseEntity()
        val isSaved = exerciseDao.upsertIfNoSuchNameSuspending(exerciseEntity)
        Timber.d("isSaved=$isSaved")
        return isSaved
    }

    override suspend fun insert(exercise: Exercise): Id {
        Timber.d("insert: exercise=$exercise")
        val exerciseEntity = exercise.toExerciseEntity()
        val insertedExerciseId = exerciseDao.insert(exerciseEntity).toId()
        Timber.d("insertedExerciseId=$insertedExerciseId")
        return insertedExerciseId
    }

    override suspend fun deleteSuspending(ids: List<Id>, subname: String?): Int {
        Timber.d(
            """deleteSuspending:
                |ids=$ids,
                |subname=$subname""".trimMargin()
        )
        val deletedExerciseCount = exerciseDao.deleteSuspending(ids, subname)
        Timber.d("deletedExerciseCount=$deletedExerciseCount")
        return deletedExerciseCount
    }

    override suspend fun deleteOtherSuspending(ids: List<Id>, subname: String?): Int {
        Timber.d(
            """deleteOtherSuspending:
                |ids=$ids,
                |subname=$subname""".trimMargin()
        )
        val deletedExerciseCount = exerciseDao.deleteOtherSuspending(ids, subname)
        Timber.d("deletedExerciseCount=$deletedExerciseCount")
        return deletedExerciseCount
    }
}
