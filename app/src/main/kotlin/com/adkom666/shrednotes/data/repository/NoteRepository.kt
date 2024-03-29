package com.adkom666.shrednotes.data.repository

import com.adkom666.shrednotes.common.Id
import com.adkom666.shrednotes.data.db.entity.NoteCountPerExerciseInfo
import com.adkom666.shrednotes.data.model.Note
import com.adkom666.shrednotes.data.model.NoteFilter
import com.adkom666.shrednotes.util.DateRange
import com.adkom666.shrednotes.util.paging.Page
import com.adkom666.shrednotes.util.time.Days
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
     * @param filter see [NoteFilter].
     * @return count of notes whose exercise names contain [exerciseSubname], or the count of all
     * notes if [exerciseSubname] is null or blank.
     */
    suspend fun countSuspending(exerciseSubname: String? = null, filter: NoteFilter? = null): Int

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
     * Getting the count notes' groups whose dates are in the [dateRange]. Notes are grouped by
     * exercise name, and each group consists of one note with maximum BPM and maximum timestamp.
     *
     * @param dateRange range of dates of the target notes.
     * @return count notes' groups whose dates are in the [dateRange]. Notes are grouped by exercise
     * name, and each group consists of one note with maximum BPM and maximum timestamp.
     */
    suspend fun countTopBpmSuspending(dateRange: DateRange): Int

    /**
     * Getting the count of [NoteCountPerExerciseInfo] objects whose related notes' dates are in the
     * [dateRange].
     *
     * @param dateRange range of dates of the target [NoteCountPerExerciseInfo] objects' related
     * notes.
     * @return count of [NoteCountPerExerciseInfo] objects whose related notes' dates are in the
     * [dateRange].
     */
    suspend fun countTopPopularExercisesSuspending(dateRange: DateRange): Int

    /**
     * Getting the date of the first note or null if it does not exist.
     *
     * @return date of the first note or null if it does not exist.
     */
    suspend fun firstNoteDateSuspending(): Days?

    /**
     * Getting the date of the last note or null if it does not exist.
     *
     * @return date of the last note or null if it does not exist.
     */
    suspend fun lastNoteDateSuspending(): Days?

    /**
     * Getting a [List] of all notes.
     *
     * @return [List] of all notes.
     */
    suspend fun listAllUnorderedSuspending(): List<Note>

    /**
     * Getting a [List] of notes with a date in [dateRange] and for exercise with identifier
     * [exerciseId] if it is not null.
     *
     * @param dateRange range of dates of the target notes.
     * @param exerciseId identifier of the target notes' exercise. If value is null then notes for
     * all exercises will be considered.
     * @return [List] of notes with a date in [dateRange] and for exercise with identifier
     * [exerciseId] if it is not null.
     */
    suspend fun listUnorderedSuspending(dateRange: DateRange, exerciseId: Id? = null): List<Note>

    /**
     * Getting a [List] of the [size] or fewer notes with their exercises' info whose dates are in
     * the [dateRange]. Notes are grouped by exercise name, and each group consists of one note with
     * maximum BPM and maximum timestamp.
     *
     * @param size limit the count of notes.
     * @param startPosition position of the first note in the list of target notes.
     * @param dateRange range of dates of the target notes.
     * @return [List] of the [size] or fewer notes with their exercises' info whose dates are in the
     * [dateRange]. Notes are grouped by exercise name, and each group consists of one note with
     * maximum BPM and maximum timestamp.
     */
    suspend fun listTopBpmSuspending(
        size: Int,
        startPosition: Int,
        dateRange: DateRange
    ): List<Note>

    /**
     * Getting a [List] of the [size] or fewer [NoteCountPerExerciseInfo] objects whose related
     * notes' dates are in the [dateRange]. The objects are sorted in descending order by note count
     * and then ascending by exercise name.
     *
     * @param size limit the count of [NoteCountPerExerciseInfo] objects.
     * @param startPosition position of the first [NoteCountPerExerciseInfo] object in the list of
     * target [NoteCountPerExerciseInfo] objects.
     * @param dateRange range of dates of the target [NoteCountPerExerciseInfo] objects' related
     * notes.
     * @return [List] of the [size] or fewer [NoteCountPerExerciseInfo] objects whose related notes'
     * dates are in the [dateRange]. They are sorted in descending order by note count and then
     * ascending by exercise name.
     */
    suspend fun listTopPopularExercisesSuspending(
        size: Int,
        startPosition: Int,
        dateRange: DateRange,
    ): List<NoteCountPerExerciseInfo>

    /**
     * Getting a [List] of the [size] or fewer notes in accordance with the [startPosition] in the
     * list of notes whose exercise names contain [exerciseSubname] and match filter, or in the list
     * of all notes if [exerciseSubname] is null or blank and [filter] is not defined. The notes are
     * sorted in descending order by timestamp, then ascending by exercise name, and then ascending
     * by BPM.
     *
     * @param size limit the count of notes.
     * @param startPosition position of the first target note in the list of notes whose exercise
     * names contain [exerciseSubname], or in the list of all notes if [exerciseSubname] is null or
     * blank.
     * @param exerciseSubname part of the names of the target notes' exercises.
     * @param filter see [NoteFilter].
     * @return [List] of the [size] or fewer notes in accordance with the [startPosition] in the
     * list of notes whose exercise names contain [exerciseSubname] and match filter, or in the list
     * of all notes if [exerciseSubname] is null or blank and [filter] is not defined. The notes are
     * sorted in descending order by timestamp, then ascending by exercise name, and then ascending
     * by BPM.
     */
    fun list(
        size: Int,
        startPosition: Int,
        exerciseSubname: String? = null,
        filter: NoteFilter? = null
    ): List<Note>

    /**
     * Getting a [Page] of the [size] or fewer notes in accordance with the [requestedStartPosition]
     * in the list of notes whose exercise names contain [exerciseSubname] and match filter, or in
     * the list of all notes if [exerciseSubname] is null or blank and [filter] is not defined. If
     * the [requestedStartPosition] exceeds the count of required notes, the notes from the end of
     * the target list are returned as part of the [Page]. The notes are sorted in descending order
     * by timestamp, then ascending by exercise name, and then descending by BPM.
     *
     * @param size limit the count of notes.
     * @param requestedStartPosition desired position of the first target note in the list of notes
     * whose exercise names contain [exerciseSubname], or in the list of all notes if
     * [exerciseSubname] is null or blank.
     * @param exerciseSubname part of the names of the target notes' exercises.
     * @param filter see [NoteFilter].
     * @return [Page] of the [size] or fewer notes in accordance with the [requestedStartPosition]
     * in the list of notes whose exercise names contain [exerciseSubname] and match filter, or in
     * the list of all notes if [exerciseSubname] is null or blank and [filter] is not defined; or
     * [Page] of notes from the end of the target list if the [requestedStartPosition] exceeds the
     * count of required notes. The notes are sorted in descending order by timestamp, then
     * ascending by exercise name, and then descending by BPM.
     */
    fun page(
        size: Int,
        requestedStartPosition: Int,
        exerciseSubname: String? = null,
        filter: NoteFilter? = null
    ): Page<Note>

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
     * is not null or blank and match filter if it is defined.
     *
     * @param ids identifiers of notes to delete.
     * @param exerciseSubname part of the exercise names from the notes to delete.
     * @param filter filter notes that should be deleted. See [NoteFilter].
     * @return count of deleted notes.
     */
    suspend fun deleteSuspending(
        ids: List<Id>,
        exerciseSubname: String? = null,
        filter: NoteFilter? = null
    ): Int

    /**
     * Deleting notes whose identifiers are not in the [ids] and whose exercise names contain
     * [exerciseSubname] if it is not null or blank and match filter if it is defined.
     *
     * @param ids identifiers of notes that should not be deleted.
     * @param exerciseSubname part of the exercise names from the notes to delete.
     * @param filter filter notes that should be deleted. See [NoteFilter].
     * @return count of deleted notes.
     */
    suspend fun deleteOtherSuspending(
        ids: List<Id>,
        exerciseSubname: String? = null,
        filter: NoteFilter? = null
    ): Int
}
