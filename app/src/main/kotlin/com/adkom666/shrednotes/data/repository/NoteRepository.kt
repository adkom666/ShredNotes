package com.adkom666.shrednotes.data.repository

import com.adkom666.shrednotes.common.Id
import com.adkom666.shrednotes.data.model.Note
import com.adkom666.shrednotes.util.paging.Page
import kotlinx.coroutines.flow.Flow

/**
 * Managing note storage.
 */
interface NoteRepository {

    /**
     * Count of all notes as [Flow].
     */
    val countFlow: Flow<Int>

    /**
     * Getting the count of notes whose exercise names contain [exerciseSubname], or the count of
     * all notes if [exerciseSubname] is null or blank.
     *
     * @param exerciseSubname part of the names of the target notes' exercises.
     * @return count of notes whose exercise names contain [exerciseSubname], or the count of all
     * notes if [exerciseSubname] is null or blank.
     */
    suspend fun countSuspending(exerciseSubname: String? = null): Int

    /**
     * Getting the count of notes with the specified [exerciseIds].
     *
     * @param exerciseIds identifiers of the target notes' exercises.
     * @return count of notes whose identifiers are in the [exerciseIds].
     */
    suspend fun countByExerciseIdsSuspending(exerciseIds: List<Id>): Int

    /**
     * Getting the count of notes with exercises whose identifiers are not in the [exerciseIds].
     *
     * @param exerciseIds identifiers of notes that should not be counted.
     * @return count of notes with exercises whose identifiers are not in the [exerciseIds].
     */
    suspend fun countOtherByExerciseIdsSuspending(exerciseIds: List<Id>): Int

    /**
     * Getting a [Page] of the [size] or fewer notes in accordance with the [requestedStartPosition]
     * in the list of notes whose exercise names contain [exerciseSubname], or in the list of all
     * notes if [exerciseSubname] is null or blank. If the [requestedStartPosition] exceeds the
     * count of required notes, the notes from the end of the target list are returned as part of
     * the [Page].
     *
     * @param size limit the count of notes.
     * @param requestedStartPosition desired position of the first target note in the list of notes
     * whose exercise names contain [exerciseSubname], or in the list of all notes if
     * [exerciseSubname] is null or blank.
     * @param exerciseSubname part of the names of the target notes' exercises.
     * @return [Page] of the [size] or fewer notes in accordance with the [requestedStartPosition]
     * in the list of notes whose exercise names contain [exerciseSubname], or in the list of all
     * notes if [exerciseSubname] is null or blank; or [Page] of notes from the end of the target
     * list if the [requestedStartPosition] exceeds the count of required notes.
     */
    fun page(
        size: Int,
        requestedStartPosition: Int,
        exerciseSubname: String? = null
    ): Page<Note>

    /**
     * Getting a [List] of the [size] or fewer notes in accordance with the [startPosition] in the
     * list of notes whose exercise names contain [exerciseSubname], or in the list of all notes if
     * [exerciseSubname] is null or blank.
     *
     * @param size limit the count of notes.
     * @param startPosition position of the first target note in the list of notes whose exercise
     * names contain [exerciseSubname], or in the list of all notes if [exerciseSubname] is null or
     * blank.
     * @param exerciseSubname part of the names of the target notes' exercises.
     * @return [List] of the [size] or fewer notes in accordance with the [startPosition] in the
     * list of notes whose exercise names contain [exerciseSubname], or in the list of all notes if
     * [exerciseSubname] is null or blank.
     */
    fun list(
        size: Int,
        startPosition: Int,
        exerciseSubname: String? = null
    ): List<Note>

    /**
     * Saving a note if there is an exercise with the name specified in the note.
     *
     * @param note note to saving.
     * @param exerciseRepository exercise storage. See [ExerciseRepository].
     * @return true if the note is saved, false if the note was not saved, because there is no
     * exercise with the name specified in the note.
     */
    suspend fun saveIfExerciseNamePresentSuspending(
        note: Note,
        exerciseRepository: ExerciseRepository
    ): Boolean

    /**
     * Saving a note with the creation of an exercise with the name specified in the note.
     *
     * @param note note to saving.
     * @param exerciseRepository exercise storage. See [ExerciseRepository].
     */
    suspend fun saveWithExerciseSuspending(
        note: Note,
        exerciseRepository: ExerciseRepository
    )

    /**
     * Deleting notes with the specified [ids] whose exercise names contain [exerciseSubname] if it
     * is not null or blank.
     *
     * @param ids identifiers of notes to delete.
     * @param exerciseSubname part of the exercise names from the notes to delete.
     * @return count of deleted notes.
     */
    suspend fun deleteSuspending(ids: List<Id>, exerciseSubname: String? = null): Int

    /**
     * Deleting notes whose identifiers are not in the [ids] and whose exercise names contain
     * [exerciseSubname] if it is not null or blank.
     *
     * @param ids identifiers of notes that should not be deleted.
     * @param exerciseSubname part of the exercise names from the notes to delete.
     * @return count of deleted notes.
     */
    suspend fun deleteOtherSuspending(ids: List<Id>, exerciseSubname: String? = null): Int
}
