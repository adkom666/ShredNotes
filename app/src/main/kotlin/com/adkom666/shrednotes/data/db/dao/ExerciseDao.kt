package com.adkom666.shrednotes.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.adkom666.shrednotes.common.Id
import com.adkom666.shrednotes.data.db.entity.ExerciseEntity
import com.adkom666.shrednotes.data.db.entity.TABLE_EXERCISES
import com.adkom666.shrednotes.data.db.entity.TABLE_EXERCISES_FIELD_ID
import com.adkom666.shrednotes.data.db.entity.TABLE_EXERCISES_FIELD_NAME
import com.adkom666.shrednotes.data.db.entity.TABLE_NOTES
import com.adkom666.shrednotes.data.db.entity.TABLE_NOTES_FIELD_EXERCISE_ID
import com.adkom666.shrednotes.data.db.entity.TABLE_NOTES_FIELD_TIMESTAMP
import kotlinx.coroutines.flow.Flow

private const val SELECT_COUNT_ALL = "SELECT COUNT(*) FROM $TABLE_EXERCISES"

private const val SELECT_COUNT_BY_ID =
    "SELECT COUNT(*) " +
            "FROM $TABLE_EXERCISES " +
            "WHERE $TABLE_EXERCISES_FIELD_ID = :id"

private const val SELECT_COUNT_BY_ANOTHER_ID_AND_NAME =
    "SELECT COUNT(*) " +
            "FROM $TABLE_EXERCISES " +
            "WHERE $TABLE_EXERCISES_FIELD_ID <> :id " +
            "AND $TABLE_EXERCISES_FIELD_NAME = :name"

private const val CONDITION_BY_SUBNAME =
    "UPPER($TABLE_EXERCISES_FIELD_NAME) LIKE UPPER('%' || :subname || '%')"

private const val SELECT_COUNT_BY_SUBNAME =
    "SELECT COUNT(*) " +
            "FROM $TABLE_EXERCISES " +
            "WHERE $CONDITION_BY_SUBNAME"

private const val SELECT_EXERCISE_IDS_BY_RELATED_NOTE_TIMESTAMP =
    "SELECT $TABLE_NOTES_FIELD_EXERCISE_ID " +
            "FROM $TABLE_NOTES " +
            "WHERE :timestampFromInclusive <= $TABLE_NOTES_FIELD_TIMESTAMP " +
            "AND $TABLE_NOTES_FIELD_TIMESTAMP < :timestampToExclusive"

private const val SELECT_COUNT_BY_RELATED_NOTE_TIMESTAMP =
    "SELECT COUNT(*) " +
            "FROM $TABLE_EXERCISES " +
            "WHERE $TABLE_EXERCISES_FIELD_ID " +
            "IN ($SELECT_EXERCISE_IDS_BY_RELATED_NOTE_TIMESTAMP)"

private const val SELECT_ALL_UNORDERED = "SELECT * FROM $TABLE_EXERCISES"
private const val ORDER = "ORDER BY $TABLE_EXERCISES_FIELD_NAME ASC"
private const val SELECT_ALL = "SELECT * FROM $TABLE_EXERCISES $ORDER"

private const val SELECT_BY_NAME =
    "SELECT * FROM $TABLE_EXERCISES " +
            "WHERE $TABLE_EXERCISES_FIELD_NAME = :name " +
            ORDER

private const val PORTION = "LIMIT :size OFFSET :offset"
private const val OPTIONS_FOR_SELECT_PORTION = "$ORDER $PORTION"

private const val SELECT_PORTION =
    "SELECT * FROM $TABLE_EXERCISES " +
            OPTIONS_FOR_SELECT_PORTION

private const val SELECT_PORTION_BY_SUBNAME =
    "SELECT * FROM $TABLE_EXERCISES " +
            "WHERE $CONDITION_BY_SUBNAME " +
            OPTIONS_FOR_SELECT_PORTION

private const val DELETE_ALL = "DELETE FROM $TABLE_EXERCISES"
private const val CONDITION_BY_IDS = "$TABLE_EXERCISES_FIELD_ID IN (:ids)"
private const val DELETE_BY_IDS = "DELETE FROM $TABLE_EXERCISES WHERE $CONDITION_BY_IDS"

private const val DELETE_BY_IDS_AND_SUBNAME =
    "DELETE FROM $TABLE_EXERCISES " +
            "WHERE $CONDITION_BY_IDS " +
            "AND $CONDITION_BY_SUBNAME"

private const val CONDITION_OTHER_BY_IDS = "$TABLE_EXERCISES_FIELD_ID NOT IN (:ids)"
private const val DELETE_OTHER_BY_IDS = "DELETE FROM $TABLE_EXERCISES WHERE $CONDITION_OTHER_BY_IDS"

private const val DELETE_OTHER_BY_IDS_AND_SUBNAME =
    "DELETE FROM $TABLE_EXERCISES " +
            "WHERE $CONDITION_OTHER_BY_IDS " +
            "AND $CONDITION_BY_SUBNAME"

/**
 * Operations with exercises in the database.
 */
@Dao
interface ExerciseDao : BaseDao<ExerciseEntity> {

    /**
     * Getting the count of all exercises stored in the database.
     *
     * @return count of all exercises.
     */
    @Query(SELECT_COUNT_ALL)
    fun countAll(): Int

    /**
     * Getting the count of all exercises stored in the database. Suspending version of [count].
     *
     * @return count of all exercises.
     */
    @Query(SELECT_COUNT_ALL)
    suspend fun countAllSuspending(): Int

    /**
     * Getting the count of all exercises as [Flow].
     *
     * @return [Flow] of the count of all exercises.
     */
    @Query(SELECT_COUNT_ALL)
    fun countAllAsFlow(): Flow<Int>

    /**
     * Getting the count of exercises with the specified [id].
     *
     * @param id identifier of the target exercise.
     * @return count of exercises with the specified [id]. Always 1 or 0, because [id] is unique.
     */
    @Query(SELECT_COUNT_BY_ID)
    fun countById(id: Id): Int

    /**
     * Getting the count of exercises with the specified [name], whose identifier is not equal to
     * specified [id].
     *
     * @param id identifier that should not be equal to the identifiers of the target exercises.
     * @param name name that should be equal to the names of the target exercises.
     * @return count of exercises with the specified [name], whose identifier is not equal to [id].
     */
    @Query(SELECT_COUNT_BY_ANOTHER_ID_AND_NAME)
    fun countByAnotherIdAndName(id: Id, name: String): Int

    /**
     * Getting the count of exercises whose names contain [subname].
     *
     * @param subname part of the names of the target exercises.
     * @return count of exercises whose names contain [subname].
     */
    @Query(SELECT_COUNT_BY_SUBNAME)
    fun countBySubname(subname: String): Int

    /**
     * Getting the count of exercises whose names contain [subname]. Suspending version of
     * [countBySubname].
     *
     * @param subname part of the names of the target exercises.
     * @return count of exercises whose names contain [subname].
     */
    @Query(SELECT_COUNT_BY_SUBNAME)
    suspend fun countBySubnameSuspending(subname: String): Int

    /**
     * Getting the count of exercises referenced by at least one note with a timestamp greater than
     * or equal to [timestampFromInclusive] and less than [timestampToExclusive].
     *
     * @param timestampFromInclusive minimum timestamp value of the target exercises' notes.
     * @param timestampToExclusive value that should not be reached by the timestamp of the target
     * exercises' notes.
     * @return count of exercises referenced by at least one note with a timestamp greater than or
     * equal to [timestampFromInclusive] and less than [timestampToExclusive].
     */
    @Query(SELECT_COUNT_BY_RELATED_NOTE_TIMESTAMP)
    suspend fun countByRelatedNoteTimestampSuspending(
        timestampFromInclusive: Long,
        timestampToExclusive: Long
    ): Int

    /**
     * Getting a [List] of all exercise entities.
     *
     * @return [List] of all exercise entities.
     */
    @Query(SELECT_ALL_UNORDERED)
    suspend fun listAllUnorderedSuspending(): List<ExerciseEntity>

    /**
     * Getting a [List] of all exercise entities sorted in ascending order by name.
     *
     * @return [List] of all exercise entities sorted in ascending order by name.
     */
    @Query(SELECT_ALL)
    suspend fun listAllSuspending(): List<ExerciseEntity>

    /**
     * Getting a [List] of exercise entities whose name is equal to [name] sorted in ascending order
     * by name.
     *
     * @param name name of target exercise entities.
     * @return [List] of exercise entities whose name is equal to [name] sorted in ascending order
     * by name.
     */
    @Query(SELECT_BY_NAME)
    suspend fun listByNameSuspending(name: String): List<ExerciseEntity>

    /**
     * Getting a [List] of the [size] or fewer exercise entities in accordance with the [offset] in
     * the list of all exercise entities sorted in ascending order by name.
     *
     * @param size limit the count of exercise entities.
     * @param offset position of the first target exercise entity in the list of all exercise
     * entities.
     * @return [List] of the [size] or fewer exercise entities in accordance with the [offset] in
     * the list of all exercise entities sorted in ascending order by name.
     */
    @Query(SELECT_PORTION)
    fun list(size: Int, offset: Int): List<ExerciseEntity>

    /**
     * Getting a [List] of the [size] or fewer exercise entities in accordance with the [offset] in
     * the list of exercise entities whose names contain [subname]. The exercise entities are sorted
     * in ascending order by name.
     *
     * @param size limit the count of exercise entities.
     * @param offset position of the first target exercise entity in the list of exercise entities
     * whose names contain [subname].
     * @param subname part of the names of the target exercises.
     * @return [List] of the [size] or fewer exercise entities in accordance with the [offset] in
     * the list of exercise entities whose names contain [subname]. The exercise entities are sorted
     * in ascending order by name.
     */
    @Query(SELECT_PORTION_BY_SUBNAME)
    fun listBySubname(size: Int, offset: Int, subname: String): List<ExerciseEntity>

    /**
     * Deleting information about all exercises.
     *
     * @return count of deleted rows from the database table.
     */
    @Query(DELETE_ALL)
    fun deleteAll(): Int

    /**
     * Deleting information about exercises with the specified [ids].
     *
     * @param ids identifiers of exercises to delete.
     * @return count of deleted rows from the database table.
     */
    @Query(DELETE_BY_IDS)
    suspend fun deleteByIdsSuspending(ids: List<Id>): Int

    /**
     * Deleting information about exercises with the specified [ids] whose names contain [subname].
     *
     * @param ids identifiers of exercises to delete.
     * @param subname part of the names of the exercises to delete.
     * @return count of deleted rows from the database table.
     */
    @Query(DELETE_BY_IDS_AND_SUBNAME)
    suspend fun deleteByIdsAndSubnameSuspending(ids: List<Id>, subname: String): Int

    /**
     * Deleting information about exercises whose identifiers are not in the [ids].
     *
     * @param ids identifiers of exercises that should not be deleted.
     * @return count of deleted rows from the database table.
     */
    @Query(DELETE_OTHER_BY_IDS)
    suspend fun deleteOtherByIdsSuspending(ids: List<Id>): Int

    /**
     * Deleting information about exercises whose identifiers are not in the [ids] and whose names
     * contain [subname].
     *
     * @param ids identifiers of exercises that should not be deleted.
     * @param subname part of the names of the exercises to delete.
     * @return count of deleted rows from the database table.
     */
    @Query(DELETE_OTHER_BY_IDS_AND_SUBNAME)
    suspend fun deleteOtherByIdsAndSubnameSuspending(ids: List<Id>, subname: String): Int

    /**
     * Update or insert information about an exercise, if it does not duplicate the exercise names.
     *
     * @param exerciseEntity information about an exercise to update or insert.
     * @return true if a database table row has been updated or inserted, false if a database table
     * row has not been updated or inserted to prevent duplication of information in the 'name'
     * field.
     */
    @Transaction
    suspend fun upsertIfNoSuchNameSuspending(exerciseEntity: ExerciseEntity): Boolean {
        return if (countByAnotherIdAndName(exerciseEntity.id, exerciseEntity.name) == 0) {
            if (countById(exerciseEntity.id) == 0) {
                insert(exerciseEntity)
            } else {
                update(exerciseEntity)
            }
            true
        } else {
            false
        }
    }
}
