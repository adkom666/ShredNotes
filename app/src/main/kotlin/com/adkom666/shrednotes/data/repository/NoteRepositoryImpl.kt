package com.adkom666.shrednotes.data.repository

import com.adkom666.shrednotes.common.Id
import com.adkom666.shrednotes.common.NO_ID
import com.adkom666.shrednotes.common.toId
import com.adkom666.shrednotes.data.converter.toNote
import com.adkom666.shrednotes.data.converter.toNoteEntity
import com.adkom666.shrednotes.data.db.Transactor
import com.adkom666.shrednotes.data.db.dao.NoteDao
import com.adkom666.shrednotes.data.db.entity.NoteWithExerciseInfo
import com.adkom666.shrednotes.data.model.Exercise
import com.adkom666.shrednotes.data.model.Note
import com.adkom666.shrednotes.data.model.NoteFilter
import com.adkom666.shrednotes.util.paging.Page
import com.adkom666.shrednotes.util.paging.safeOffset
import com.adkom666.shrednotes.util.time.Days
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import timber.log.Timber

/**
 * Implementation of [NoteRepository].
 *
 * @property noteDao object for performing operations with notes in the database.
 * @property transactor see [Transactor].
 */
class NoteRepositoryImpl(
    private val noteDao: NoteDao,
    private val transactor: Transactor
) : NoteRepository {

    override val countFlow: Flow<Int>
        get() = noteDao.countAllAsFlow()

    override suspend fun countSuspending(exerciseSubname: String?, filter: NoteFilter?): Int {
        Timber.d("countSuspending: exerciseSubname=$exerciseSubname, filter=$filter")
        val count = entityCountSuspending(exerciseSubname, filter)
        Timber.d("count=$count")
        return count
    }

    override suspend fun countByExerciseIdsSuspending(exerciseIds: List<Id>): Int {
        Timber.d("countByExerciseIdsSuspending: exerciseIds=$exerciseIds")
        val count = noteDao.countByExerciseIdsSuspending(exerciseIds)
        Timber.d("count=$count")
        return count
    }

    override suspend fun countOtherByExerciseIdsSuspending(exerciseIds: List<Id>): Int {
        Timber.d("countOtherByExerciseIdsSuspending: exerciseIds=$exerciseIds")
        val count = noteDao.countOtherByExerciseIdsSuspending(exerciseIds)
        Timber.d("count=$count")
        return count
    }

    override suspend fun listAllUnorderedSuspending(): List<Note> {
        Timber.d("allSuspending")
        val noteWithExerciseEntityList = noteDao.listAllWithExercisesUnorderedSuspending()
        val noteList = noteWithExerciseEntityList.map(NoteWithExerciseInfo::toNote)
        Timber.d("noteList=$noteList")
        return noteList
    }

    override fun page(
        size: Int,
        requestedStartPosition: Int,
        exerciseSubname: String?,
        filter: NoteFilter?
    ): Page<Note> = runBlocking {
        transactor.transaction {
            Timber.d(
                """page:
                |size=$size,
                |requestedStartPosition=$requestedStartPosition,
                |exerciseSubname=$exerciseSubname,
                |filter=$filter""".trimMargin()
            )
            val count = entityCountSuspending(exerciseSubname, filter)
            val notePage = if (count > 0 && size > 0) {
                val offset = safeOffset(
                    requestedOffset = requestedStartPosition,
                    pageSize = size,
                    count = count
                )
                val noteWithExerciseEntityList = entityList(
                    size = size,
                    startPosition = offset,
                    exerciseSubname = exerciseSubname,
                    filter = filter
                )
                val noteList = noteWithExerciseEntityList.map(NoteWithExerciseInfo::toNote)
                Page(noteList, offset)
            } else {
                Page(emptyList(), 0)
            }
            Timber.d("notePage=$notePage")
            return@transaction notePage
        }
    }

    override fun list(
        size: Int,
        startPosition: Int,
        exerciseSubname: String?,
        filter: NoteFilter?
    ): List<Note> {
        Timber.d(
            """list:
                |size=$size,
                |startPosition=$startPosition,
                |exerciseSubname=$exerciseSubname,
                |filter=$filter""".trimMargin()
        )
        val noteWithExerciseEntityList = entityList(
            size = size,
            startPosition = startPosition,
            exerciseSubname = exerciseSubname,
            filter = filter
        )
        val noteList = noteWithExerciseEntityList.map(NoteWithExerciseInfo::toNote)
        Timber.d("noteList=$noteList")
        return noteList
    }

    override suspend fun saveIfExerciseNamePresentSuspending(
        note: Note,
        exerciseRepository: ExerciseRepository
    ): Boolean = transactor.transaction {
        Timber.d("saveIfExerciseNamePresentSuspending: note=$note")
        val noteEntity = note.toNoteEntity { exerciseName ->
            Timber.d("exerciseName=$exerciseName")
            val exerciseList = exerciseRepository.exercisesByNameSuspending(exerciseName)
            Timber.d("exerciseList=$exerciseList")
            val exerciseId = if (exerciseList.isNotEmpty()) {
                exerciseList.first().id
            } else {
                NO_ID
            }
            Timber.d("exerciseId=$exerciseId")
            return@toNoteEntity exerciseId
        }
        Timber.d("noteEntity=$noteEntity")
        val isSaved = if (noteEntity.exerciseId != NO_ID) {
            noteDao.upsertSuspending(noteEntity)
            true
        } else {
            false
        }
        Timber.d("isSaved=$isSaved")
        return@transaction isSaved
    }

    override suspend fun saveWithExerciseSuspending(
        note: Note,
        exerciseRepository: ExerciseRepository
    ) = transactor.transaction {
        Timber.d("saveWithExerciseSuspending: note=$note")
        val noteEntity = note.toNoteEntity { exerciseName ->
            Timber.d("exerciseName=$exerciseName")
            val exercise = Exercise(name = exerciseName)
            val insertedExerciseId = exerciseRepository.insert(exercise).toId()
            Timber.d("insertedExerciseId=$insertedExerciseId")
            return@toNoteEntity insertedExerciseId
        }
        noteDao.upsertSuspending(noteEntity)
    }

    override suspend fun deleteSuspending(
        ids: List<Id>,
        exerciseSubname: String?,
        filter: NoteFilter?
    ): Int {
        Timber.d(
            """deleteSuspending:
                |ids=$ids,
                |exerciseSubname=$exerciseSubname,
                |filter=$filter""".trimMargin()
        )
        val deletedNoteCount = deleteEntitiesSuspending(ids, exerciseSubname, filter)
        Timber.d("deletedNoteCount=$deletedNoteCount")
        return deletedNoteCount
    }

    override suspend fun deleteOtherSuspending(
        ids: List<Id>,
        exerciseSubname: String?,
        filter: NoteFilter?
    ): Int {
        Timber.d(
            """deleteOtherSuspending:
                |ids=$ids,
                |exerciseSubname=$exerciseSubname,
                |filter=$filter""".trimMargin()
        )
        val deletedNoteCount = deleteOtherEntitiesSuspending(ids, exerciseSubname, filter)
        Timber.d("deletedNoteCount=$deletedNoteCount")
        return deletedNoteCount
    }

    private suspend fun entityCountSuspending(
        exerciseSubname: String?,
        filter: NoteFilter?
    ): Int = when {
        // Using search query:
        !exerciseSubname.isNullOrBlank()
                && (filter == null || filter.isDefined.not()) ->
            noteDao.countByExerciseSubnameSuspending(
                exerciseSubname = exerciseSubname
            )
        // Using search query and filtering by date:
        !exerciseSubname.isNullOrBlank()
                && filter != null
                && filter.isDateRangeDefined
                && filter.isBpmRangeDefined.not() ->
            noteDao.countByExerciseSubnameAndTimestampRangeSuspending(
                exerciseSubname = exerciseSubname,
                timestampFromInclusive = filter.dateFromInclusive.timestampOrMin(),
                timestampToExclusive = filter.dateToExclusive.timestampOrMax()
            )
        // Using search query and filtering by BPM:
        !exerciseSubname.isNullOrBlank()
                && filter != null
                && filter.isDateRangeDefined.not()
                && filter.isBpmRangeDefined ->
            noteDao.countByExerciseSubnameAndBpmRangeSuspending(
                exerciseSubname = exerciseSubname,
                bpmFromInclusive = filter.bpmFromInclusive.valueOrMin(),
                bpmToInclusive = filter.bpmToInclusive.valueOrMax()
            )
        // Using search query and filtering by date and BPM:
        !exerciseSubname.isNullOrBlank()
                && filter != null
                && filter.isDateRangeDefined
                && filter.isBpmRangeDefined ->
            noteDao.countByExerciseSubnameTimestampRangeAndBpmRangeSuspending(
                exerciseSubname = exerciseSubname,
                timestampFromInclusive = filter.dateFromInclusive.timestampOrMin(),
                timestampToExclusive = filter.dateToExclusive.timestampOrMax(),
                bpmFromInclusive = filter.bpmFromInclusive.valueOrMin(),
                bpmToInclusive = filter.bpmToInclusive.valueOrMax()
            )
        // Filtering by date:
        exerciseSubname.isNullOrBlank()
                && filter != null
                && filter.isDateRangeDefined
                && filter.isBpmRangeDefined.not() ->
            noteDao.countByTimestampRangeSuspending(
                timestampFromInclusive = filter.dateFromInclusive.timestampOrMin(),
                timestampToExclusive = filter.dateToExclusive.timestampOrMax()
            )
        // Filtering by BPM:
        exerciseSubname.isNullOrBlank()
                && filter != null
                && filter.isDateRangeDefined.not()
                && filter.isBpmRangeDefined ->
            noteDao.countByBpmRangeSuspending(
                bpmFromInclusive = filter.bpmFromInclusive.valueOrMin(),
                bpmToInclusive = filter.bpmToInclusive.valueOrMax()
            )
        // Filtering by date and BPM:
        exerciseSubname.isNullOrBlank()
                && filter != null
                && filter.isDateRangeDefined
                && filter.isBpmRangeDefined ->
            noteDao.countByTimestampRangeAndBpmRangeSuspending(
                timestampFromInclusive = filter.dateFromInclusive.timestampOrMin(),
                timestampToExclusive = filter.dateToExclusive.timestampOrMax(),
                bpmFromInclusive = filter.bpmFromInclusive.valueOrMin(),
                bpmToInclusive = filter.bpmToInclusive.valueOrMax()
            )
        // All:
        else ->
            noteDao.countAllSuspending()
    }

    private fun entityList(
        size: Int,
        startPosition: Int,
        exerciseSubname: String?,
        filter: NoteFilter?
    ): List<NoteWithExerciseInfo> = when {
        // Using search query:
        !exerciseSubname.isNullOrBlank()
                && (filter == null || filter.isDefined.not()) ->
            noteDao.listByExerciseSubname(
                size = size,
                offset = startPosition,
                exerciseSubname = exerciseSubname
            )
        // Using search query and filtering by date:
        !exerciseSubname.isNullOrBlank()
                && filter != null
                && filter.isDateRangeDefined
                && filter.isBpmRangeDefined.not() ->
            noteDao.listByExerciseSubnameAndTimestampRange(
                size = size,
                offset = startPosition,
                exerciseSubname = exerciseSubname,
                timestampFromInclusive = filter.dateFromInclusive.timestampOrMin(),
                timestampToExclusive = filter.dateToExclusive.timestampOrMax()
            )
        // Using search query and filtering by BPM:
        !exerciseSubname.isNullOrBlank()
                && filter != null
                && filter.isDateRangeDefined.not()
                && filter.isBpmRangeDefined ->
            noteDao.listByExerciseSubnameAndBpmRange(
                size = size,
                offset = startPosition,
                exerciseSubname = exerciseSubname,
                bpmFromInclusive = filter.bpmFromInclusive.valueOrMin(),
                bpmToInclusive = filter.bpmToInclusive.valueOrMax()
            )
        // Using search query and filtering by date and BPM:
        !exerciseSubname.isNullOrBlank()
                && filter != null
                && filter.isDateRangeDefined
                && filter.isBpmRangeDefined ->
            noteDao.listByExerciseSubnameTimestampRangeAndBpmRange(
                size = size,
                offset = startPosition,
                exerciseSubname = exerciseSubname,
                timestampFromInclusive = filter.dateFromInclusive.timestampOrMin(),
                timestampToExclusive = filter.dateToExclusive.timestampOrMax(),
                bpmFromInclusive = filter.bpmFromInclusive.valueOrMin(),
                bpmToInclusive = filter.bpmToInclusive.valueOrMax()
            )
        // Filtering by date:
        exerciseSubname.isNullOrBlank()
                && filter != null
                && filter.isDateRangeDefined
                && filter.isBpmRangeDefined.not() ->
            noteDao.listByTimestampRange(
                size = size,
                offset = startPosition,
                timestampFromInclusive = filter.dateFromInclusive.timestampOrMin(),
                timestampToExclusive = filter.dateToExclusive.timestampOrMax()
            )
        // Filtering by BPM:
        exerciseSubname.isNullOrBlank()
                && filter != null
                && filter.isDateRangeDefined.not()
                && filter.isBpmRangeDefined ->
            noteDao.listByBpmRange(
                size = size,
                offset = startPosition,
                bpmFromInclusive = filter.bpmFromInclusive.valueOrMin(),
                bpmToInclusive = filter.bpmToInclusive.valueOrMax()
            )
        // Filtering by date and BPM:
        exerciseSubname.isNullOrBlank()
                && filter != null
                && filter.isDateRangeDefined
                && filter.isBpmRangeDefined ->
            noteDao.listByTimestampRangeAndBpmRange(
                size = size,
                offset = startPosition,
                timestampFromInclusive = filter.dateFromInclusive.timestampOrMin(),
                timestampToExclusive = filter.dateToExclusive.timestampOrMax(),
                bpmFromInclusive = filter.bpmFromInclusive.valueOrMin(),
                bpmToInclusive = filter.bpmToInclusive.valueOrMax()
            )
        // Default:
        else ->
            noteDao.list(
                size = size,
                offset = startPosition
            )
    }

    private suspend fun deleteEntitiesSuspending(
        ids: List<Id>,
        exerciseSubname: String?,
        filter: NoteFilter?
    ): Int = when {
        // Using search query:
        !exerciseSubname.isNullOrBlank()
                && (filter == null || filter.isDefined.not()) ->
            noteDao.deleteByIdsAndExerciseSubnameSuspending(
                ids = ids,
                exerciseSubname = exerciseSubname
            )
        // Using search query and filtering by date:
        !exerciseSubname.isNullOrBlank()
                && filter != null
                && filter.isDateRangeDefined
                && filter.isBpmRangeDefined.not() ->
            noteDao.deleteByIdsExerciseSubnameAndTimestampRangeSuspending(
                ids = ids,
                exerciseSubname = exerciseSubname,
                timestampFromInclusive = filter.dateFromInclusive.timestampOrMin(),
                timestampToExclusive = filter.dateToExclusive.timestampOrMax()
            )
        // Using search query and filtering by BPM:
        !exerciseSubname.isNullOrBlank()
                && filter != null
                && filter.isDateRangeDefined.not()
                && filter.isBpmRangeDefined ->
            noteDao.deleteByIdsExerciseSubnameAndBpmRangeSuspending(
                ids = ids,
                exerciseSubname = exerciseSubname,
                bpmFromInclusive = filter.bpmFromInclusive.valueOrMin(),
                bpmToInclusive = filter.bpmToInclusive.valueOrMax()
            )
        // Using search query and filtering by date and BPM:
        !exerciseSubname.isNullOrBlank()
                && filter != null
                && filter.isDateRangeDefined
                && filter.isBpmRangeDefined ->
            noteDao.deleteByIdsExerciseSubnameTimestampRangeAndBpmRangeSuspending(
                ids = ids,
                exerciseSubname = exerciseSubname,
                timestampFromInclusive = filter.dateFromInclusive.timestampOrMin(),
                timestampToExclusive = filter.dateToExclusive.timestampOrMax(),
                bpmFromInclusive = filter.bpmFromInclusive.valueOrMin(),
                bpmToInclusive = filter.bpmToInclusive.valueOrMax()
            )
        // Filtering by date:
        exerciseSubname.isNullOrBlank()
                && filter != null
                && filter.isDateRangeDefined
                && filter.isBpmRangeDefined.not() ->
            noteDao.deleteByIdsAndTimestampRangeSuspending(
                ids = ids,
                timestampFromInclusive = filter.dateFromInclusive.timestampOrMin(),
                timestampToExclusive = filter.dateToExclusive.timestampOrMax()
            )
        // Filtering by BPM:
        exerciseSubname.isNullOrBlank()
                && filter != null
                && filter.isDateRangeDefined.not()
                && filter.isBpmRangeDefined ->
            noteDao.deleteByIdsAndBpmRangeSuspending(
                ids = ids,
                bpmFromInclusive = filter.bpmFromInclusive.valueOrMin(),
                bpmToInclusive = filter.bpmToInclusive.valueOrMax()
            )
        // Filtering by date and BPM:
        exerciseSubname.isNullOrBlank()
                && filter != null
                && filter.isDateRangeDefined
                && filter.isBpmRangeDefined ->
            noteDao.deleteByIdsTimestampRangeAndBpmRangeSuspending(
                ids = ids,
                timestampFromInclusive = filter.dateFromInclusive.timestampOrMin(),
                timestampToExclusive = filter.dateToExclusive.timestampOrMax(),
                bpmFromInclusive = filter.bpmFromInclusive.valueOrMin(),
                bpmToInclusive = filter.bpmToInclusive.valueOrMax()
            )
        // Default:
        else ->
            noteDao.deleteByIdsSuspending(
                ids = ids
            )
    }

    private suspend fun deleteOtherEntitiesSuspending(
        ids: List<Id>,
        exerciseSubname: String?,
        filter: NoteFilter?
    ): Int = when {
        // Using search query:
        !exerciseSubname.isNullOrBlank()
                && (filter == null || filter.isDefined.not()) ->
            noteDao.deleteOtherByIdsAndExerciseSubnameSuspending(
                ids = ids,
                exerciseSubname = exerciseSubname
            )
        // Using search query and filtering by date:
        !exerciseSubname.isNullOrBlank()
                && filter != null
                && filter.isDateRangeDefined
                && filter.isBpmRangeDefined.not() ->
            noteDao.deleteOtherByIdsExerciseSubnameAndTimestampRangeSuspending(
                ids = ids,
                exerciseSubname = exerciseSubname,
                timestampFromInclusive = filter.dateFromInclusive.timestampOrMin(),
                timestampToExclusive = filter.dateToExclusive.timestampOrMax()
            )
        // Using search query and filtering by BPM:
        !exerciseSubname.isNullOrBlank()
                && filter != null
                && filter.isDateRangeDefined.not()
                && filter.isBpmRangeDefined ->
            noteDao.deleteOtherByIdsExerciseSubnameAndBpmRangeSuspending(
                ids = ids,
                exerciseSubname = exerciseSubname,
                bpmFromInclusive = filter.bpmFromInclusive.valueOrMin(),
                bpmToInclusive = filter.bpmToInclusive.valueOrMax()
            )
        // Using search query and filtering by date and BPM:
        !exerciseSubname.isNullOrBlank()
                && filter != null
                && filter.isDateRangeDefined
                && filter.isBpmRangeDefined ->
            noteDao.deleteOtherByIdsExerciseSubnameTimestampRangeAndBpmRangeSuspending(
                ids = ids,
                exerciseSubname = exerciseSubname,
                timestampFromInclusive = filter.dateFromInclusive.timestampOrMin(),
                timestampToExclusive = filter.dateToExclusive.timestampOrMax(),
                bpmFromInclusive = filter.bpmFromInclusive.valueOrMin(),
                bpmToInclusive = filter.bpmToInclusive.valueOrMax()
            )
        // Filtering by date:
        exerciseSubname.isNullOrBlank()
                && filter != null
                && filter.isDateRangeDefined
                && filter.isBpmRangeDefined.not() ->
            noteDao.deleteOtherByIdsAndTimestampRangeSuspending(
                ids = ids,
                timestampFromInclusive = filter.dateFromInclusive.timestampOrMin(),
                timestampToExclusive = filter.dateToExclusive.timestampOrMax()
            )
        // Filtering by BPM:
        exerciseSubname.isNullOrBlank()
                && filter != null
                && filter.isDateRangeDefined.not()
                && filter.isBpmRangeDefined ->
            noteDao.deleteOtherByIdsAndBpmRangeSuspending(
                ids = ids,
                bpmFromInclusive = filter.bpmFromInclusive.valueOrMin(),
                bpmToInclusive = filter.bpmToInclusive.valueOrMax()
            )
        // Filtering by date and BPM:
        exerciseSubname.isNullOrBlank()
                && filter != null
                && filter.isDateRangeDefined
                && filter.isBpmRangeDefined ->
            noteDao.deleteOtherByIdsTimestampRangeAndBpmRangeSuspending(
                ids = ids,
                timestampFromInclusive = filter.dateFromInclusive.timestampOrMin(),
                timestampToExclusive = filter.dateToExclusive.timestampOrMax(),
                bpmFromInclusive = filter.bpmFromInclusive.valueOrMin(),
                bpmToInclusive = filter.bpmToInclusive.valueOrMax()
            )
        // Default:
        else ->
            noteDao.deleteOtherByIdsSuspending(
                ids = ids
            )
    }

    private fun Days?.timestampOrMin(): Long {
        return this?.epochMillis ?: Long.MIN_VALUE
    }

    private fun Days?.timestampOrMax(): Long {
        return this?.epochMillis ?: Long.MAX_VALUE
    }

    private fun Int?.valueOrMin(): Int {
        return this ?: Int.MIN_VALUE
    }

    private fun Int?.valueOrMax(): Int {
        return this ?: Int.MAX_VALUE
    }
}
