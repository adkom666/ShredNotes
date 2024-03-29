package com.adkom666.shrednotes.data.converter

import com.adkom666.shrednotes.common.Id
import com.adkom666.shrednotes.data.db.entity.NoteEntity
import com.adkom666.shrednotes.data.db.entity.NoteWithExerciseInfo
import com.adkom666.shrednotes.data.external.ExternalNoteV1
import com.adkom666.shrednotes.data.model.Note
import com.adkom666.shrednotes.util.time.Minutes

/**
 * Getting a note from a database entity.
 */
fun NoteWithExerciseInfo.toNote(): Note = Note(
    id = noteId,
    dateTime = Minutes(noteTimestamp),
    exerciseName = exerciseName ?: "",
    bpm = noteBpm
)

/**
 * Getting a database entity from a note.
 *
 * @param exerciseIdByName used to provide an exercise identifier by the exercise name.
 */
suspend fun Note.toNoteEntity(
    exerciseIdByName: suspend (String) -> Id
): NoteEntity = NoteEntity(
    id = id,
    timestamp = dateTime.epochMillis,
    exerciseId = exerciseIdByName(exerciseName),
    bpm = bpm
)

/**
 * Getting an external note (version 1) from a database entity.
 */
fun NoteEntity.toExternalNoteV1(): ExternalNoteV1 = ExternalNoteV1(
    id = id,
    timestamp = timestamp,
    exerciseId = exerciseId,
    bpm = bpm
)

/**
 * Getting a database entity from an external note (version 1).
 */
fun ExternalNoteV1.toNoteEntity(): NoteEntity = NoteEntity(
    id = id,
    timestamp = timestamp,
    exerciseId = exerciseId,
    bpm = bpm
)
