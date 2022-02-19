package com.adkom666.shrednotes.data.repository

import com.adkom666.shrednotes.data.converter.toExerciseEntity
import com.adkom666.shrednotes.data.converter.toExternalExerciseV1
import com.adkom666.shrednotes.data.converter.toExternalNoteV1
import com.adkom666.shrednotes.data.converter.toNoteEntity
import com.adkom666.shrednotes.data.db.Transactor
import com.adkom666.shrednotes.data.db.dao.ExerciseDao
import com.adkom666.shrednotes.data.db.dao.NoteDao
import com.adkom666.shrednotes.data.db.entity.ExerciseEntity
import com.adkom666.shrednotes.data.db.entity.NoteEntity
import com.adkom666.shrednotes.data.external.ExternalExerciseV1
import com.adkom666.shrednotes.data.external.ExternalNoteV1
import com.adkom666.shrednotes.data.external.ExternalShredNotesV1
import timber.log.Timber

/**
 * Implementation of [ShredNotesRepository].
 *
 * @property exerciseDao object for performing operations with exercises in the database.
 * @property noteDao object for performing operations with notes in the database.
 * @property transactor see [Transactor].
 */
class ShredNotesRepositoryImpl(
    private val exerciseDao: ExerciseDao,
    private val noteDao: NoteDao,
    private val transactor: Transactor
) : ShredNotesRepository {

    override suspend fun shredNotesV1Suspending(): ExternalShredNotesV1 = transactor.transaction {
        Timber.d("shredNotesV1Suspending")
        val exersiseEntityList = exerciseDao.listAllUnorderedSuspending()
        val noteEntityList = noteDao.listAllRawUnorderedSuspending()
        val exercises = exersiseEntityList.map(ExerciseEntity::toExternalExerciseV1)
        val notes = noteEntityList.map(NoteEntity::toExternalNoteV1)
        val shredNotes = ExternalShredNotesV1(exercises, notes)
        Timber.d("shredNotes=$shredNotes")
        return@transaction shredNotes
    }

    override suspend fun replaceShredNotesByV1Suspending(
        shredNotes: ExternalShredNotesV1
    ) = transactor.transaction {
        Timber.d("replaceShredNotesByV1Suspending: shredNotes=$shredNotes")
        val exersiseEntityList = shredNotes.exercises.map(ExternalExerciseV1::toExerciseEntity)
        exerciseDao.deleteAll()
        exerciseDao.insertAll(exersiseEntityList)
        val noteEntityList = shredNotes.notes.map(ExternalNoteV1::toNoteEntity)
        noteDao.deleteAll()
        noteDao.insertAll(noteEntityList)
    }
}
