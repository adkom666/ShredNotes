package com.adkom666.shrednotes.data.repository

import com.adkom666.shrednotes.common.Id
import com.adkom666.shrednotes.common.toId
import com.adkom666.shrednotes.data.converter.toExercise
import com.adkom666.shrednotes.data.converter.toExerciseEntity
import com.adkom666.shrednotes.data.db.Transactor
import com.adkom666.shrednotes.data.db.dao.ExerciseDao
import com.adkom666.shrednotes.data.db.entity.ExerciseEntity
import com.adkom666.shrednotes.data.model.Exercise
import com.adkom666.shrednotes.util.DateRange
import com.adkom666.shrednotes.util.paging.Page
import com.adkom666.shrednotes.util.paging.safeOffset
import com.adkom666.shrednotes.util.time.timestampOrMax
import com.adkom666.shrednotes.util.time.timestampOrMin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.util.Calendar

/**
 * Implementation of [ExerciseRepository].
 *
 * @property exerciseDao object for performing operations with exercises in the database.
 * @property transactor see [Transactor].
 */
class ExerciseRepositoryImpl(
    private val exerciseDao: ExerciseDao,
    private val transactor: Transactor
) : ExerciseRepository {

    override val countFlow: Flow<Int>
        get() = exerciseDao.countAllAsFlow()

    override suspend fun countSuspending(subname: String?): Int {
        Timber.d("countSuspending: subname=$subname")
        val count = entityCountSuspending(subname)
        Timber.d("count=$count")
        return count
    }

    override suspend fun countByRelatedNoteDateSuspending(dateRange: DateRange): Int {
        Timber.d("countByRelatedNoteTimestampSuspending: dateRange=$dateRange")
        val calendar = Calendar.getInstance()
        val count = entityCountByRelatedNoteTimestampSuspending(dateRange, calendar)
        Timber.d("count=$count")
        return count
    }

    override suspend fun listAllSuspending(): List<Exercise> {
        Timber.d("listAllSuspending")
        val exerciseEntityList = exerciseDao.listAllSuspending()
        val exerciseList = exerciseEntityList.map(ExerciseEntity::toExercise)
        Timber.d("exerciseList=$exerciseList")
        return exerciseList
    }

    override suspend fun listByNameSuspending(name: String): List<Exercise> {
        Timber.d("listByNameSuspending: name=$name")
        val exerciseEntityList = exerciseDao.listByNameSuspending(name)
        val exerciseList = exerciseEntityList.map(ExerciseEntity::toExercise)
        Timber.d("exerciseList=$exerciseList")
        return exerciseList
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
        val exerciseEntityList = entityList(
            size = size,
            startPosition = startPosition,
            subname = subname
        )
        val exerciseList = exerciseEntityList.map(ExerciseEntity::toExercise)
        Timber.d("exerciseList=$exerciseList")
        return exerciseList
    }

    override fun page(
        size: Int,
        requestedStartPosition: Int,
        subname: String?
    ): Page<Exercise> = runBlocking {
        transactor.transaction {
            Timber.d(
                """page:
                    |size=$size,
                    |requestedStartPosition=$requestedStartPosition,
                    |subname=$subname""".trimMargin()
            )
            val count = entityCountSuspending(subname)
            val exerciseEntityPage = if (count > 0 && size > 0) {
                val offset = safeOffset(
                    requestedOffset = requestedStartPosition,
                    pageSize = size,
                    count = count
                )
                val exerciseEntityList = entityList(
                    size = size,
                    startPosition = offset,
                    subname = subname
                )
                Page(exerciseEntityList, offset)
            } else {
                Page(emptyList(), 0)
            }
            val exercisePage = Page(
                exerciseEntityPage.items.map(ExerciseEntity::toExercise),
                exerciseEntityPage.offset
            )
            Timber.d("exercisePage=$exercisePage")
            return@transaction exercisePage
        }
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
        val deletedExerciseCount = deleteEntitiesSuspending(ids, subname)
        Timber.d("deletedExerciseCount=$deletedExerciseCount")
        return deletedExerciseCount
    }

    override suspend fun deleteOtherSuspending(ids: List<Id>, subname: String?): Int {
        Timber.d(
            """deleteOtherSuspending:
                |ids=$ids,
                |subname=$subname""".trimMargin()
        )
        val deletedExerciseCount = deleteOtherEntitiesSuspending(ids, subname)
        Timber.d("deletedExerciseCount=$deletedExerciseCount")
        return deletedExerciseCount
    }

    private suspend fun entityCountSuspending(
        subname: String?
    ): Int = if (subname.isNullOrBlank()) {
        exerciseDao.countAllSuspending()
    } else {
        exerciseDao.countBySubnameSuspending(subname)
    }

    private suspend fun entityCountByRelatedNoteTimestampSuspending(
        dateRange: DateRange,
        calendar: Calendar
    ): Int = exerciseDao.countByRelatedNoteTimestampSuspending(
        dateRange.fromInclusive.timestampOrMin(calendar),
        dateRange.toExclusive.timestampOrMax(calendar)
    )

    private fun entityList(
        size: Int,
        startPosition: Int,
        subname: String?
    ): List<ExerciseEntity> = if (subname.isNullOrBlank()) {
        exerciseDao.list(
            size = size,
            offset = startPosition
        )
    } else {
        exerciseDao.listBySubname(
            size = size,
            offset = startPosition,
            subname = subname
        )
    }

    private suspend fun deleteEntitiesSuspending(
        ids: List<Id>,
        subname: String?
    ): Int = if (subname.isNullOrBlank()) {
        exerciseDao.deleteByIdsSuspending(ids)
    } else {
        exerciseDao.deleteByIdsAndSubnameSuspending(ids, subname)
    }

    private suspend fun deleteOtherEntitiesSuspending(
        ids: List<Id>,
        subname: String?
    ): Int = if (subname.isNullOrBlank()) {
        exerciseDao.deleteOtherByIdsSuspending(ids)
    } else {
        exerciseDao.deleteOtherByIdsAndSubnameSuspending(ids, subname)
    }
}
