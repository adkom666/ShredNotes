package com.adkom666.shrednotes.data.repository

import com.adkom666.shrednotes.data.converter.toExerciseEntity
import com.adkom666.shrednotes.data.converter.toExternalExercise
import com.adkom666.shrednotes.data.converter.toExternalNote
import com.adkom666.shrednotes.data.converter.toNoteEntity
import com.adkom666.shrednotes.data.db.Transactor
import com.adkom666.shrednotes.data.db.dao.ExerciseDao
import com.adkom666.shrednotes.data.db.dao.NoteDao
import com.adkom666.shrednotes.data.db.entity.ExerciseEntity
import com.adkom666.shrednotes.data.db.entity.NoteEntity
import com.adkom666.shrednotes.data.external.ExternalExercise
import com.adkom666.shrednotes.data.external.ExternalNote
import com.adkom666.shrednotes.data.external.ExternalShredNotes

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

    override suspend fun shredNotesSuspending(): ExternalShredNotes = transactor.transaction {
        val exersiseEntityList = exerciseDao.listAllUnorderedSuspending()
        val noteEntityList = noteDao.listAllUnorderedSuspending()
        val exercises = exersiseEntityList.map(ExerciseEntity::toExternalExercise)
        val notes = noteEntityList.map(NoteEntity::toExternalNote)
        return@transaction ExternalShredNotes(exercises, notes)
    }

    override suspend fun replaceShredNotesSuspendingBy(
        shredNotes: ExternalShredNotes
    ) = transactor.transaction {
        val exersiseEntityList = shredNotes.exercises.map(ExternalExercise::toExerciseEntity)
        exerciseDao.deleteAll()
        exerciseDao.insertAll(exersiseEntityList)
        val noteEntityList = shredNotes.notes.map(ExternalNote::toNoteEntity)
        noteDao.deleteAll()
        noteDao.insertAll(noteEntityList)
    }
}
