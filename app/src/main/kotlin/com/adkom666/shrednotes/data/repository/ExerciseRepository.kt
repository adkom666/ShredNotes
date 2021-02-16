package com.adkom666.shrednotes.data.repository

import com.adkom666.shrednotes.data.model.Exercise
import com.adkom666.shrednotes.util.paging.Page
import kotlinx.coroutines.flow.Flow

/**
 * Managing exercise storage.
 */
interface ExerciseRepository {

    /**
     * Count of all exercises as [Flow].
     */
    val countFlow: Flow<Int>

    /**
     * Getting the count of exercises whose names contain [subname], or the count of all exercises
     * if [subname] is null or blank.
     *
     * @param subname part of the names of target exercises.
     * @return count of exercises whose names contain [subname], or the count of all exercises if
     * [subname] is null or blank.
     */
    suspend fun countSuspending(subname: String? = null): Int

    /**
     * Getting a [Page] of the [size] or fewer exercises in accordance with the
     * [requestedStartPosition] in the list of exercises whose names contain [subname], or in the
     * list of all exercises if [subname] is null or blank. If the [requestedStartPosition] exceeds
     * the count of required exercises, the exercises from the end of the target list are returned
     * as part of the [Page].
     *
     * @param size limit the count of exercises.
     * @param requestedStartPosition desired position of the first target exercise in the list of
     * exercises whose names contain [subname], or in the list of all exercises if [subname] is
     * null or blank.
     * @param subname part of the names of the target exercises.
     * @return [Page] of the [size] or fewer exercises in accordance with the
     * [requestedStartPosition] in the list of exercises whose names contain [subname], or in the
     * list of all exercises if [subname] is null or blank; or [Page] of exercises from the end of
     * the target list if the [requestedStartPosition] exceeds the count of required exercises.
     */
    fun page(
        size: Int,
        requestedStartPosition: Int,
        subname: String? = null
    ): Page<Exercise>

    /**
     * Getting a [List] of the [size] or fewer exercises in accordance with the [startPosition] in
     * the list of exercises whose names contain [subname], or in the list of all exercises if
     * [subname] is null or blank.
     *
     * @param size limit the count of exercises.
     * @param startPosition position of the first target exercise in the list of exercises whose
     * names contain [subname], or in the list of all exercises if [subname] is null or blank.
     * @param subname part of the names of the target exercises.
     * @return [List] of the [size] or fewer exercises in accordance with the [startPosition] in the
     * list of exercises whose names contain [subname], or in the list of all exercises if [subname]
     * is null or blank.
     */
    fun list(
        size: Int,
        startPosition: Int,
        subname: String? = null
    ): List<Exercise>

    /**
     * Saving an exercise if it doesn't duplicate the exercise names.
     *
     * @param exercise exercise to saving.
     * @return true if the exercise is saved, false if the exercise was not saved, because its name
     * duplicates the name of another saved exercise.
     */
    suspend fun saveIfNoSuchNameSuspending(exercise: Exercise): Boolean

    /**
     * Deleting exercises with the specified [ids] whose names contain [subname] if it is not null
     * or blank.
     *
     * @param ids identifiers of exercises to delete.
     * @param subname part of the names of the exercises to delete.
     * @return count of deleted exercises.
     */
    suspend fun deleteSuspending(ids: List<Long>, subname: String? = null): Int

    /**
     * Deleting exercises whose identifiers are not in the [ids] and whose names contain [subname]
     * if it is not null or blank.
     *
     * @param ids identifiers of exercises that should not be deleted.
     * @param subname part of the names of the exercises to delete.
     * @return count of deleted exercises.
     */
    suspend fun deleteOtherSuspending(ids: List<Long>, subname: String? = null): Int
}
