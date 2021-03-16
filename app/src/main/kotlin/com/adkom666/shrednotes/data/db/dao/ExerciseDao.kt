package com.adkom666.shrednotes.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.adkom666.shrednotes.common.Id
import com.adkom666.shrednotes.data.db.entity.ExerciseEntity
import com.adkom666.shrednotes.data.db.entity.TABLE_EXERCISES
import com.adkom666.shrednotes.data.db.entity.TABLE_EXERCISES_FIELD_ID
import com.adkom666.shrednotes.data.db.entity.TABLE_EXERCISES_FIELD_NAME
import com.adkom666.shrednotes.util.paging.Page
import com.adkom666.shrednotes.util.paging.safeOffset
import kotlinx.coroutines.flow.Flow

private const val SELECT_COUNT_ALL = "SELECT COUNT(*) FROM $TABLE_EXERCISES"

private const val SELECT_COUNT_BY_ID =
    "SELECT COUNT(*) FROM $TABLE_EXERCISES " +
            "WHERE $TABLE_EXERCISES_FIELD_ID = :id"

private const val SELECT_COUNT_BY_ANOTHER_ID_AND_NAME =
    "SELECT COUNT(*) FROM $TABLE_EXERCISES " +
            "WHERE $TABLE_EXERCISES_FIELD_ID <> :id " +
            "AND $TABLE_EXERCISES_FIELD_NAME = :name"

private const val CONDITION_BY_SUBNAME =
    "UPPER($TABLE_EXERCISES_FIELD_NAME) LIKE UPPER('%' || :subname || '%')"

private const val SELECT_COUNT_BY_SUBNAME =
    "SELECT COUNT(*) FROM $TABLE_EXERCISES " +
            "WHERE $CONDITION_BY_SUBNAME"

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

private const val CONDITION_BY_IDS = "$TABLE_EXERCISES_FIELD_ID IN (:ids)"

private const val DELETE_BY_IDS =
    "DELETE FROM $TABLE_EXERCISES " +
            "WHERE $CONDITION_BY_IDS"

private const val DELETE_BY_IDS_AND_SUBNAME =
    "DELETE FROM $TABLE_EXERCISES " +
            "WHERE $CONDITION_BY_IDS " +
            "AND $CONDITION_BY_SUBNAME"

private const val CONDITION_OTHER_BY_IDS = "$TABLE_EXERCISES_FIELD_ID NOT IN (:ids)"

private const val DELETE_OTHER_BY_IDS =
    "DELETE FROM $TABLE_EXERCISES " +
            "WHERE $CONDITION_OTHER_BY_IDS"

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
     * Getting a [List] of all exercise entities.
     *
     * @return [List] of all exercise entities.
     */
    @Query(SELECT_ALL)
    suspend fun listAllSuspending(): List<ExerciseEntity>

    /**
     * Getting a [List] of exercise entities whose name is equal to [name].
     *
     * @param name name of target exercise entities.
     * @return [List] of exercise entities whose name is equal to [name].
     */
    @Query(SELECT_BY_NAME)
    suspend fun listByNameSuspending(name: String): List<ExerciseEntity>

    /**
     * Getting a [List] of the [size] or fewer exercise entities in accordance with the [offset] in
     * the list of all exercise entities.
     *
     * @param size limit the count of exercise entities.
     * @param offset position of the first target exercise entity in the list of all exercise
     * entities.
     * @return [List] of the [size] or fewer exercise entities in accordance with the [offset] in
     * the list of all exercise entities.
     */
    @Query(SELECT_PORTION)
    fun listPortion(size: Int, offset: Int): List<ExerciseEntity>

    /**
     * Getting a [List] of the [size] or fewer exercise entities in accordance with the [offset] in
     * the list of exercise entities whose names contain [subname].
     *
     * @param size limit the count of exercise entities.
     * @param offset position of the first target exercise entity in the list of exercise entities
     * whose names contain [subname].
     * @param subname part of the names of the target exercises.
     * @return [List] of the [size] or fewer exercise entities in accordance with the [offset] in
     * the list of exercise entities whose names contain [subname].
     */
    @Query(SELECT_PORTION_BY_SUBNAME)
    fun listPortionBySubname(size: Int, offset: Int, subname: String): List<ExerciseEntity>

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
     * Getting a [Page] of the [size] or fewer exercise entities in accordance with the
     * [requestedOffset] in the list of exercise entities whose names contain [subname], or in the
     * list of all exercise entities if [subname] is null or blank. If the [requestedOffset] exceeds
     * the count of required exercise entities, the entities from the end of the target list are
     * returned as part of the [Page].
     *
     * @param size limit the count of exercise entities.
     * @param requestedOffset desired position of the first target exercise entity in the list of
     * exercise entities whose names contain [subname], or in the list of all exercise entities if
     * [subname] is null or blank.
     * @param subname part of the names of the target exercises.
     * @return [Page] of the [size] or fewer exercise entities in accordance with the
     * [requestedOffset] in the list of exercise entities whose names contain [subname], or in the
     * list of all exercise entities if [subname] is null or blank; or [Page] of exercise entities
     * from the end of the target list if the [requestedOffset] exceeds the count of required
     * exercise entities.
     */
    @Transaction
    fun page(
        size: Int,
        requestedOffset: Int,
        subname: String?
    ): Page<ExerciseEntity> {
        val count = if (subname != null && subname.isNotBlank()) {
            countBySubname(subname)
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
                subname = subname
            )
            Page(entityList, offset)
        } else {
            Page(emptyList(), 0)
        }
    }

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

    /**
     * Getting the count of exercises whose names contain [subname] or count of all exercises if
     * [subname] is null or blank.
     *
     * @param subname part of the names of the target exercises.
     * @return count of exercises whose names contain [subname] or count of all exercises if
     * [subname] is null or blank.
     */
    suspend fun countSuspending(subname: String?): Int {
        return if (subname != null && subname.isNotBlank()) {
            countBySubnameSuspending(subname)
        } else {
            countAllSuspending()
        }
    }

    /**
     * Getting a [List] of the [size] or fewer exercise entities in accordance with the [offset] in
     * the list of exercise entities whose names contain [subname], or in the list of all exercise
     * entities if [subname] is null or blank.
     *
     * @param size limit the count of exercise entities.
     * @param offset position of the first target exercise entity in the list of exercise entities
     * whose names contain [subname], or in the list of all exercise entities if [subname] is null
     * or blank.
     * @param subname part of the names of the target exercises.
     * @return [List] of the [size] or fewer exercise entities in accordance with the [offset] in
     * the list of exercise entities whose names contain [subname], or in the list of all exercise
     * entities if [subname] is null or blank.
     */
    fun list(
        size: Int,
        offset: Int,
        subname: String?
    ): List<ExerciseEntity> {
        return if (subname != null && subname.isNotBlank()) {
            listPortionBySubname(size = size, offset = offset, subname = subname)
        } else {
            listPortion(size = size, offset = offset)
        }
    }

    /**
     * Deleting information about exercises with the specified [ids] whose names contain [subname]
     * if it is not null or blank.
     *
     * @param ids identifiers of exercises to delete.
     * @param subname part of the names of the exercises to delete.
     * @return count of deleted rows from the database table.
     */
    suspend fun deleteSuspending(ids: List<Id>, subname: String?): Int {
        return if (subname != null && subname.isNotBlank()) {
            deleteByIdsAndSubnameSuspending(ids, subname)
        } else {
            deleteByIdsSuspending(ids)
        }
    }

    /**
     * Deleting information about exercises whose identifiers are not in the [ids] and whose names
     * contain [subname] if it is not null or blank.
     *
     * @param ids identifiers of exercises that should not be deleted.
     * @param subname part of the names of the exercises to delete.
     * @return count of deleted rows from the database table.
     */
    suspend fun deleteOtherSuspending(ids: List<Id>, subname: String?): Int {
        return if (subname != null && subname.isNotBlank()) {
            deleteOtherByIdsAndSubnameSuspending(ids, subname)
        } else {
            deleteOtherByIdsSuspending(ids)
        }
    }
}
