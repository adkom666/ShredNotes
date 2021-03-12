package com.adkom666.shrednotes.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.adkom666.shrednotes.common.Id
import com.adkom666.shrednotes.data.db.entity.NoteEntity
import com.adkom666.shrednotes.data.db.entity.NoteWithExerciseInfo
import com.adkom666.shrednotes.data.db.entity.TABLE_EXERCISES
import com.adkom666.shrednotes.data.db.entity.TABLE_EXERCISES_FIELD_ID
import com.adkom666.shrednotes.data.db.entity.TABLE_EXERCISES_FIELD_NAME
import com.adkom666.shrednotes.data.db.entity.TABLE_NOTES
import com.adkom666.shrednotes.data.db.entity.TABLE_NOTES_FIELD_ID
import com.adkom666.shrednotes.data.db.entity.TABLE_NOTES_FIELD_DATE_TIME
import com.adkom666.shrednotes.data.db.entity.TABLE_NOTES_FIELD_EXERCISE_ID
import com.adkom666.shrednotes.data.db.entity.TABLE_NOTES_FIELD_BPM
import com.adkom666.shrednotes.data.db.entity.TABLE_NOTES_WITH_EXERCISES_FIELD_NOTE_ID
import com.adkom666.shrednotes.data.db.entity.TABLE_NOTES_WITH_EXERCISES_FIELD_NOTE_DATE_TIME
import com.adkom666.shrednotes.data.db.entity.TABLE_NOTES_WITH_EXERCISES_FIELD_EXERCISE_NAME
import com.adkom666.shrednotes.data.db.entity.TABLE_NOTES_WITH_EXERCISES_FIELD_NOTE_BPM
import com.adkom666.shrednotes.util.paging.Page
import com.adkom666.shrednotes.util.paging.safeOffset
import kotlinx.coroutines.flow.Flow

private const val SELECT_COUNT_ALL = "SELECT COUNT(*) FROM $TABLE_NOTES"

private const val SELECT_COUNT_BY_ID =
    "SELECT COUNT(*) FROM $TABLE_NOTES " +
            "WHERE $TABLE_NOTES_FIELD_ID = :id"

private const val CONDITION_BY_EXERCISE_SUBNAME =
    "UPPER($TABLE_EXERCISES.$TABLE_EXERCISES_FIELD_NAME) " +
            "LIKE UPPER('%' || :exerciseSubname || '%')"

private const val TABLE_NOTES_WITH_EXERCISES =
    "$TABLE_NOTES LEFT JOIN $TABLE_EXERCISES " +
            "ON $TABLE_NOTES.$TABLE_NOTES_FIELD_EXERCISE_ID = " +
            "$TABLE_EXERCISES.$TABLE_EXERCISES_FIELD_ID"

private const val SELECT_COUNT_BY_EXERCISE_SUBNAME =
    "SELECT COUNT(*) FROM $TABLE_NOTES_WITH_EXERCISES " +
            "WHERE $CONDITION_BY_EXERCISE_SUBNAME"

private const val OPTIONS_FOR_SELECT_ENTITIES =
    "ORDER BY $TABLE_NOTES_FIELD_DATE_TIME DESC " +
            "LIMIT :size OFFSET :offset"

private const val NOTES_WITH_EXERCISES_FIELDS =
    "$TABLE_NOTES.$TABLE_NOTES_FIELD_ID " +
            "AS $TABLE_NOTES_WITH_EXERCISES_FIELD_NOTE_ID, " +
            "$TABLE_NOTES.$TABLE_NOTES_FIELD_DATE_TIME " +
            "AS $TABLE_NOTES_WITH_EXERCISES_FIELD_NOTE_DATE_TIME, " +
            "$TABLE_EXERCISES.$TABLE_EXERCISES_FIELD_NAME " +
            "AS $TABLE_NOTES_WITH_EXERCISES_FIELD_EXERCISE_NAME, " +
            "$TABLE_NOTES.$TABLE_NOTES_FIELD_BPM " +
            "AS $TABLE_NOTES_WITH_EXERCISES_FIELD_NOTE_BPM"

private const val SELECT_PORTION =
    "SELECT $NOTES_WITH_EXERCISES_FIELDS " +
            "FROM $TABLE_NOTES_WITH_EXERCISES " +
            OPTIONS_FOR_SELECT_ENTITIES

private const val SELECT_PORTION_BY_EXERCISE_SUBNAME =
    "SELECT $NOTES_WITH_EXERCISES_FIELDS " +
            "FROM $TABLE_NOTES_WITH_EXERCISES " +
            "WHERE $CONDITION_BY_EXERCISE_SUBNAME " +
            OPTIONS_FOR_SELECT_ENTITIES

private const val DELETE_BY_IDS =
    "DELETE FROM $TABLE_NOTES " +
            "WHERE $TABLE_NOTES_FIELD_ID IN (:ids)"

private const val DELETE_BY_IDS_AND_EXERCISE_SUBNAME =
    "DELETE FROM $TABLE_NOTES " +
            "WHERE $TABLE_NOTES_FIELD_ID IN " +
            "(SELECT $TABLE_NOTES.$TABLE_NOTES_FIELD_ID " +
            "FROM $TABLE_NOTES_WITH_EXERCISES " +
            "WHERE $TABLE_NOTES.$TABLE_NOTES_FIELD_ID IN (:ids) " +
            "AND $CONDITION_BY_EXERCISE_SUBNAME)"

private const val DELETE_OTHER_BY_IDS =
    "DELETE FROM $TABLE_NOTES " +
            "WHERE $TABLE_NOTES_FIELD_ID NOT IN (:ids)"

private const val DELETE_OTHER_BY_IDS_AND_EXERCISE_SUBNAME =
    "DELETE FROM $TABLE_NOTES " +
            "WHERE $TABLE_NOTES_FIELD_ID IN " +
            "(SELECT $TABLE_NOTES.$TABLE_NOTES_FIELD_ID " +
            "FROM $TABLE_NOTES_WITH_EXERCISES " +
            "WHERE $TABLE_NOTES.$TABLE_NOTES_FIELD_ID NOT IN (:ids) " +
            "AND $CONDITION_BY_EXERCISE_SUBNAME)"

/**
 * Operations with notes in the database.
 */
@Dao
interface NoteDao : BaseDao<NoteEntity> {

    /**
     * Getting the count of all notes stored in the database.
     *
     * @return count of all notes.
     */
    @Query(SELECT_COUNT_ALL)
    fun countAll(): Int

    /**
     * Getting the count of all notes stored in the database. Suspending version of [countAll].
     *
     * @return count of all notes.
     */
    @Query(SELECT_COUNT_ALL)
    suspend fun countAllSuspending(): Int

    /**
     * Getting the count of notes whose exercise names contain [exerciseSubname].
     *
     * @param exerciseSubname part of the names of the target notes' exercises.
     * @return count of notes whose exercise names contain [exerciseSubname].
     */
    @Query(SELECT_COUNT_BY_EXERCISE_SUBNAME)
    fun countByExerciseSubname(exerciseSubname: String): Int

    /**
     * Getting the count of notes whose exercise names contain [exerciseSubname]. Suspending version
     * of [countByExerciseSubname].
     *
     * @param exerciseSubname part of the names of the target notes' exercises.
     * @return count of notes whose exercise names contain [exerciseSubname].
     */
    @Query(SELECT_COUNT_BY_EXERCISE_SUBNAME)
    suspend fun countByExerciseSubnameSuspending(exerciseSubname: String): Int

    /**
     * Getting the count of all notes as [Flow].
     *
     * @return [Flow] of the count of all notes.
     */
    @Query(SELECT_COUNT_ALL)
    fun countAllAsFlow(): Flow<Int>

    /**
     * Getting the count of notes with the specified [id].
     *
     * @param id identifier of the target note.
     * @return count of notes with the specified [id]. Always 1 or 0, because [id] is unique.
     */
    @Query(SELECT_COUNT_BY_ID)
    fun countById(id: Id): Int

    /**
     * Getting a [List] of the [size] or fewer notes with their exercises' info in accordance with
     * the [offset] in the list of all notes.
     *
     * @param size limit the count of notes.
     * @param offset position of the first target note in the list of all notes.
     * @return [List] of the [size] or fewer notes with their exercises' info in accordance with the
     * [offset] in the list of all notes.
     */
    @Transaction
    @Query(SELECT_PORTION)
    fun listPortion(size: Int, offset: Int): List<NoteWithExerciseInfo>

    /**
     * Getting a [List] of the [size] or fewer notes with their exercises' info in accordance with
     * the [offset] in the list of notes whose exercise names contain [exerciseSubname].
     *
     * @param size limit the count of notes.
     * @param offset position of the first target note in the list of notes whose exercise names
     * contain [exerciseSubname].
     * @param exerciseSubname part of the names of the target notes' exercises.
     * @return [List] of the [size] or fewer notes with their exercises' info in accordance with the
     * [offset] in the list of notes whose exercise names contain [exerciseSubname].
     */
    @Transaction
    @Query(SELECT_PORTION_BY_EXERCISE_SUBNAME)
    fun listPortionByExerciseSubname(
        size: Int,
        offset: Int,
        exerciseSubname: String
    ): List<NoteWithExerciseInfo>

    /**
     * Deleting information about notes with the specified [ids].
     *
     * @param ids identifiers of notes to delete.
     * @return count of deleted rows from the database table.
     */
    @Query(DELETE_BY_IDS)
    suspend fun deleteByIdsSuspending(ids: List<Id>): Int

    /**
     * Deleting information about notes with the specified [ids] whose exercise names contain
     * [exerciseSubname].
     *
     * @param ids identifiers of notes to delete.
     * @param exerciseSubname part of the names of the target notes' exercises.
     * @return count of deleted rows from the database table.
     */
    @Query(DELETE_BY_IDS_AND_EXERCISE_SUBNAME)
    suspend fun deleteByIdsAndExerciseSubnameSuspending(
        ids: List<Id>,
        exerciseSubname: String
    ): Int

    /**
     * Deleting information about notes whose identifiers are not in the [ids].
     *
     * @param ids identifiers of notes that should not be deleted.
     * @return count of deleted rows from the database table.
     */
    @Query(DELETE_OTHER_BY_IDS)
    suspend fun deleteOtherByIdsSuspending(ids: List<Id>): Int

    /**
     * Deleting information about notes whose identifiers are not in the [ids] and whose exercise
     * names contain [exerciseSubname].
     *
     * @param ids identifiers of notes that should not be deleted.
     * @param exerciseSubname part of the names of the target notes' exercises.
     * @return count of deleted rows from the database table.
     */
    @Query(DELETE_OTHER_BY_IDS_AND_EXERCISE_SUBNAME)
    suspend fun deleteOtherByIdsAndExerciseSubnameSuspending(
        ids: List<Id>,
        exerciseSubname: String
    ): Int

    /**
     * Getting a [Page] of the [size] or fewer notes with their exercises' info in accordance with
     * the [requestedOffset] in the list of notes whose exercise names contain [exerciseSubname], or
     * in the list of all notes if [exerciseSubname] is null or blank. If the [requestedOffset]
     * exceeds the count of required notes, the notes from the end of the target list are returned
     * as part of the [Page].
     *
     * @param size limit the count of notes.
     * @param requestedOffset desired position of the first target note in the list of notes whose
     * exercise names contain [exerciseSubname], or in the list of all notes if [exerciseSubname] is
     * null or blank.
     * @param exerciseSubname part of the names of the target notes' exercises.
     * @return [Page] of the [size] or fewer notes with their exercises' info in accordance with the
     * [requestedOffset] in the list of notes whose names contain [exerciseSubname], or in the list
     * of all notes if [exerciseSubname] is null or blank; or [Page] of notes from the end of the
     * target list if the [requestedOffset] exceeds the count of required notes.
     */
    @Transaction
    fun page(
        size: Int,
        requestedOffset: Int,
        exerciseSubname: String?
    ): Page<NoteWithExerciseInfo> {
        val count = if (exerciseSubname != null && exerciseSubname.isNotBlank()) {
            countByExerciseSubname(exerciseSubname)
        } else {
            countAll()
        }
        return if (count > 0 && size > 0) {
            val offset = safeOffset(
                requestedOffset,
                size,
                count
            )
            val entityList = list(
                size = size,
                offset = offset,
                exerciseSubname = exerciseSubname
            )
            Page(entityList, offset)
        } else {
            Page(emptyList(), 0)
        }
    }

    /**
     * Update or insert information about a note.
     *
     * @param noteEntity information about a note to update or insert.
     */
    @Transaction
    suspend fun upsertSuspending(noteEntity: NoteEntity) {
        if (countById(noteEntity.id) == 0) {
            insert(noteEntity)
        } else {
            update(noteEntity)
        }
    }

    /**
     * Getting the count of notes whose exercise names contain [exerciseSubname] or count of all
     * notes if [exerciseSubname] is null or blank.
     *
     * @param exerciseSubname part of the names of the target notes' exercises.
     * @return count of notes whose exercise names contain [exerciseSubname] or count of all notes
     * if [exerciseSubname] is null or blank.
     */
    suspend fun countSuspending(exerciseSubname: String?): Int {
        return if (exerciseSubname != null && exerciseSubname.isNotBlank()) {
            countByExerciseSubnameSuspending(exerciseSubname)
        } else {
            countAllSuspending()
        }
    }

    /**
     * Getting a [List] of the [size] or fewer notes with their exercises' info in accordance with
     * the [offset] in the list of notes whose exercise names contain [exerciseSubname], or in the
     * list of all notes if [exerciseSubname] is null or blank.
     *
     * @param size limit the count of notes.
     * @param offset position of the first target note in the list of notes whose exercise names
     * contain [exerciseSubname], or in the list of all notes if [exerciseSubname] is null or blank.
     * @param exerciseSubname part of the names of the target notes' exercises.
     * @return [List] of the [size] or fewer notes with their exercises' info in accordance with the
     * [offset] in the list of notes whose exercise names contain [exerciseSubname], or in the list
     * of all notes if [exerciseSubname] is null or blank.
     */
    fun list(
        size: Int,
        offset: Int,
        exerciseSubname: String?
    ): List<NoteWithExerciseInfo> {
        return if (exerciseSubname != null && exerciseSubname.isNotBlank()) {
            listPortionByExerciseSubname(
                size = size,
                offset = offset,
                exerciseSubname = exerciseSubname
            )
        } else {
            listPortion(size = size, offset = offset)
        }
    }

    /**
     * Deleting information about notes with the specified [ids] whose exercise names contain
     * [exerciseSubname] if it is not null or blank.
     *
     * @param ids identifiers of notes to delete.
     * @param exerciseSubname part of the names of the target notes' exercises.
     * @return count of deleted rows from the database table.
     */
    suspend fun deleteSuspending(ids: List<Id>, exerciseSubname: String?): Int {
        return if (exerciseSubname != null && exerciseSubname.isNotBlank()) {
            deleteByIdsAndExerciseSubnameSuspending(ids, exerciseSubname)
        } else {
            deleteByIdsSuspending(ids)
        }
    }

    /**
     * Deleting information about notes whose identifiers are not in the [ids] and whose exercise
     * names contain [exerciseSubname] if it is not null or blank.
     *
     * @param ids identifiers of exercises that should not be deleted.
     * @param exerciseSubname part of the names of the target notes' exercises.
     * @return count of deleted rows from the database table.
     */
    suspend fun deleteOtherSuspending(ids: List<Id>, exerciseSubname: String?): Int {
        return if (exerciseSubname != null && exerciseSubname.isNotBlank()) {
            deleteOtherByIdsAndExerciseSubnameSuspending(ids, exerciseSubname)
        } else {
            deleteOtherByIdsSuspending(ids)
        }
    }
}
