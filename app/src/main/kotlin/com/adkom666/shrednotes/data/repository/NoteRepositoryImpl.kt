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
import com.adkom666.shrednotes.util.paging.Page
import kotlinx.coroutines.flow.Flow
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

    override suspend fun countSuspending(exerciseSubname: String?): Int {
        Timber.d("countSuspending: exerciseSubname=$exerciseSubname")
        val count = noteDao.countSuspending(exerciseSubname)
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

    override fun page(
        size: Int,
        requestedStartPosition: Int,
        exerciseSubname: String?
    ): Page<Note> {
        Timber.d(
            """page:
                |size=$size,
                |requestedStartPosition=$requestedStartPosition,
                |exerciseSubname=$exerciseSubname""".trimMargin()
        )
        val noteWithExerciseEntityPage = noteDao.page(
            size,
            requestedStartPosition,
            exerciseSubname
        )
        val notePage = Page(
            noteWithExerciseEntityPage.items.map(NoteWithExerciseInfo::toNote),
            noteWithExerciseEntityPage.offset
        )
        Timber.d("notePage=$notePage")
        return notePage
    }

    override fun list(
        size: Int,
        startPosition: Int,
        exerciseSubname: String?
    ): List<Note> {
        Timber.d(
            """list:
                |size=$size,
                |startPosition=$startPosition,
                |exerciseSubname=$exerciseSubname""".trimMargin()
        )
        val noteWithExerciseEntityList = noteDao.list(
            size,
            startPosition,
            exerciseSubname
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

    override suspend fun deleteSuspending(ids: List<Id>, exerciseSubname: String?): Int {
        Timber.d(
            """deleteSuspending:
                |ids=$ids,
                |exerciseSubname=$exerciseSubname""".trimMargin()
        )
        val deletedNoteCount = noteDao.deleteSuspending(ids, exerciseSubname)
        Timber.d("deletedNoteCount=$deletedNoteCount")
        return deletedNoteCount
    }

    override suspend fun deleteOtherSuspending(ids: List<Id>, exerciseSubname: String?): Int {
        Timber.d(
            """deleteOtherSuspending:
                |ids=$ids,
                |exerciseSubname=$exerciseSubname""".trimMargin()
        )
        val deletedNoteCount = noteDao.deleteOtherSuspending(ids, exerciseSubname)
        Timber.d("deletedNoteCount=$deletedNoteCount")
        return deletedNoteCount
    }
}
