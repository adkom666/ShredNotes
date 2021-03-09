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
     * Getting the count of notes whose names contain [subname], or the count of all notes if
     * [subname] is null or blank.
     *
     * @param subname part of the names of target notes.
     * @return count of notes whose names contain [subname], or the count of all notes if [subname]
     * is null or blank.
     */
    suspend fun countSuspending(subname: String? = null): Int

    /**
     * Getting a [Page] of the [size] or fewer notes in accordance with the [requestedStartPosition]
     * in the list of notes whose names contain [subname], or in the list of all notes if [subname]
     * is null or blank. If the [requestedStartPosition] exceeds the count of required notes, the
     * notes from the end of the target list are returned as part of the [Page].
     *
     * @param size limit the count of notes.
     * @param requestedStartPosition desired position of the first target note in the list of notes
     * whose names contain [subname], or in the list of all notes if [subname] is null or blank.
     * @param subname part of the names of the target notes.
     * @return [Page] of the [size] or fewer notes in accordance with the [requestedStartPosition]
     * in the list of notes whose names contain [subname], or in the list of all notes if [subname]
     * is null or blank; or [Page] of notes from the end of the target list if the
     * [requestedStartPosition] exceeds the count of required notes.
     */
    fun page(
        size: Int,
        requestedStartPosition: Int,
        subname: String? = null
    ): Page<Note>

    /**
     * Getting a [List] of the [size] or fewer notes in accordance with the [startPosition] in the
     * list of notes whose names contain [subname], or in the list of all notes if [subname] is null
     * or blank.
     *
     * @param size limit the count of notes.
     * @param startPosition position of the first target note in the list of notes whose names
     * contain [subname], or in the list of all notes if [subname] is null or blank.
     * @param subname part of the names of the target notes.
     * @return [List] of the [size] or fewer notes in accordance with the [startPosition] in the
     * list of notes whose names contain [subname], or in the list of all notes if [subname] is null
     * or blank.
     */
    fun list(
        size: Int,
        startPosition: Int,
        subname: String? = null
    ): List<Note>

    /**
     * Saving a note.
     *
     * @param note note to saving.
     */
    suspend fun saveSuspending(note: Note)

    /**
     * Deleting notes with the specified [ids] whose names contain [subname] if it is not null or
     * blank.
     *
     * @param ids identifiers of notes to delete.
     * @param subname part of the names of the notes to delete.
     * @return count of deleted notes.
     */
    suspend fun deleteSuspending(ids: List<Id>, subname: String? = null): Int

    /**
     * Deleting notes whose identifiers are not in the [ids] and whose names contain [subname] if it
     * is not null or blank.
     *
     * @param ids identifiers of notes that should not be deleted.
     * @param subname part of the names of the notes to delete.
     * @return count of deleted notes.
     */
    suspend fun deleteOtherSuspending(ids: List<Id>, subname: String? = null): Int
}
