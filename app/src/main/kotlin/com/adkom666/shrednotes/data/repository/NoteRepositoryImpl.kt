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
        return noteDao.countSuspending(exerciseSubname)
    }

    override fun page(
        size: Int,
        requestedStartPosition: Int,
        exerciseSubname: String?
    ): Page<Note> {
        val noteWithExerciseEntityPage = noteDao.page(
            size,
            requestedStartPosition,
            exerciseSubname
        )
        return Page(
            noteWithExerciseEntityPage.items.map(NoteWithExerciseInfo::toNote),
            noteWithExerciseEntityPage.offset
        )
    }

    override fun list(
        size: Int,
        startPosition: Int,
        exerciseSubname: String?
    ): List<Note> {
        val noteWithExerciseEntityList = noteDao.list(
            size,
            startPosition,
            exerciseSubname
        )
        return noteWithExerciseEntityList.map(NoteWithExerciseInfo::toNote)
    }

    override suspend fun saveIfExerciseNamePresentSuspending(
        note: Note,
        exerciseRepository: ExerciseRepository
    ): Boolean = transactor.transaction {
        val noteEntity = note.toNoteEntity { exerciseName ->
            val exerciseList = exerciseRepository.exercisesByNameSuspending(exerciseName)
            if (exerciseList.isNotEmpty()) {
                exerciseList.first().id
            } else {
                NO_ID
            }
        }
        if (noteEntity.exerciseId != NO_ID) {
            noteDao.upsertSuspending(noteEntity)
            true
        } else {
            false
        }
    }

    override suspend fun saveWithExerciseSuspending(
        note: Note,
        exerciseRepository: ExerciseRepository
    ) = transactor.transaction {
        val noteEntity = note.toNoteEntity { exerciseName ->
            val exercise = Exercise(name = exerciseName)
            exerciseRepository.insert(exercise).toId()
        }
        noteDao.upsertSuspending(noteEntity)
    }

    override suspend fun deleteSuspending(ids: List<Id>, exerciseSubname: String?): Int {
        return 0
    }

    override suspend fun deleteOtherSuspending(ids: List<Id>, exerciseSubname: String?): Int {
        return 0
    }
}
