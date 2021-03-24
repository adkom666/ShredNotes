package com.adkom666.shrednotes.data.converter

import com.adkom666.shrednotes.common.Id
import com.adkom666.shrednotes.data.db.entity.NoteEntity
import com.adkom666.shrednotes.data.db.entity.NoteWithExerciseInfo
import com.adkom666.shrednotes.data.external.ExternalNote
import com.adkom666.shrednotes.data.model.Note
import com.adkom666.shrednotes.util.TruncatedToMinutesDate

/**
 * Getting a note from a database entities.
 */
fun NoteWithExerciseInfo.toNote(): Note = Note(
    noteId,
    TruncatedToMinutesDate(noteTimestamp),
    exerciseName ?: "",
    noteBpm
)

/**
 * Getting a database entity from a note.
 */
suspend fun Note.toNoteEntity(exerciseIdByName: suspend (String) -> Id): NoteEntity = NoteEntity(
    id,
    dateTime.epochMillis,
    exerciseIdByName(exerciseName),
    bpm
)

/**
 * Getting an external note from a database entity.
 */
fun NoteEntity.toExternalNote(): ExternalNote = ExternalNote(
    id,
    timestamp,
    exerciseId,
    bpm
)

/**
 * Getting a database entity from an external note.
 */
fun ExternalNote.toNoteEntity(): NoteEntity = NoteEntity(
    id,
    timestamp,
    exerciseId,
    bpm
)
