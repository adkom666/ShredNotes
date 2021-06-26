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
import com.adkom666.shrednotes.data.db.entity.TABLE_NOTES_FIELD_TIMESTAMP
import com.adkom666.shrednotes.data.db.entity.TABLE_NOTES_FIELD_EXERCISE_ID
import com.adkom666.shrednotes.data.db.entity.TABLE_NOTES_FIELD_BPM
import com.adkom666.shrednotes.data.db.entity.TABLE_NOTES_WITH_EXERCISES_FIELD_NOTE_ID
import com.adkom666.shrednotes.data.db.entity.TABLE_NOTES_WITH_EXERCISES_FIELD_NOTE_TIMESTAMP
import com.adkom666.shrednotes.data.db.entity.TABLE_NOTES_WITH_EXERCISES_FIELD_EXERCISE_NAME
import com.adkom666.shrednotes.data.db.entity.TABLE_NOTES_WITH_EXERCISES_FIELD_NOTE_BPM
import kotlinx.coroutines.flow.Flow

private const val SELECT_COUNT_ALL = "SELECT COUNT(*) FROM $TABLE_NOTES"

private const val SELECT_COUNT_BY_ID =
    "SELECT COUNT(*) " +
            "FROM $TABLE_NOTES " +
            "WHERE $TABLE_NOTES_FIELD_ID = :id"

private const val CONDITION_BY_EXERCISE_SUBNAME =
    "UPPER($TABLE_EXERCISES.$TABLE_EXERCISES_FIELD_NAME) " +
            "LIKE UPPER('%' || :exerciseSubname || '%')"

private const val CONDITION_NOTES_BY_TIMESTAMP_RANGE =
    ":timestampFromInclusive <= $TABLE_NOTES_FIELD_TIMESTAMP " +
            "AND $TABLE_NOTES_FIELD_TIMESTAMP < :timestampToExclusive"

private const val CONDITION_NOTES_WITH_EXERCISES_BY_TIMESTAMP_RANGE =
    ":timestampFromInclusive <= $TABLE_NOTES.$TABLE_NOTES_FIELD_TIMESTAMP " +
            "AND $TABLE_NOTES.$TABLE_NOTES_FIELD_TIMESTAMP < :timestampToExclusive"

private const val CONDITION_NOTES_BY_BPM_RANGE =
    ":bpmFromInclusive <= $TABLE_NOTES_FIELD_BPM " +
            "AND $TABLE_NOTES_FIELD_BPM <= :bpmToInclusive"

private const val CONDITION_NOTES_WITH_EXERCISES_BY_BPM_RANGE =
    ":bpmFromInclusive <= $TABLE_NOTES.$TABLE_NOTES_FIELD_BPM " +
            "AND $TABLE_NOTES.$TABLE_NOTES_FIELD_BPM <= :bpmToInclusive"

private const val TABLE_NOTES_WITH_EXERCISES =
    "$TABLE_NOTES LEFT JOIN $TABLE_EXERCISES " +
            "ON $TABLE_NOTES.$TABLE_NOTES_FIELD_EXERCISE_ID = " +
            "$TABLE_EXERCISES.$TABLE_EXERCISES_FIELD_ID"

private const val TABLE_NOTES_WITH_EXISTENT_EXERCISES =
    "$TABLE_NOTES INNER JOIN $TABLE_EXERCISES " +
            "ON $TABLE_NOTES.$TABLE_NOTES_FIELD_EXERCISE_ID = " +
            "$TABLE_EXERCISES.$TABLE_EXERCISES_FIELD_ID"

private const val SELECT_COUNT_BY_EXERCISE_SUBNAME =
    "SELECT COUNT(*) " +
            "FROM $TABLE_NOTES_WITH_EXISTENT_EXERCISES " +
            "WHERE $CONDITION_BY_EXERCISE_SUBNAME"

private const val SELECT_COUNT_BY_EXERCISE_SUBNAME_AND_TIMESTAMP_RANGE =
    "SELECT COUNT(*) " +
            "FROM $TABLE_NOTES_WITH_EXISTENT_EXERCISES " +
            "WHERE $CONDITION_BY_EXERCISE_SUBNAME " +
            "AND $CONDITION_NOTES_WITH_EXERCISES_BY_TIMESTAMP_RANGE"

private const val SELECT_COUNT_BY_EXERCISE_SUBNAME_AND_BPM_RANGE =
    "SELECT COUNT(*) " +
            "FROM $TABLE_NOTES_WITH_EXISTENT_EXERCISES " +
            "WHERE $CONDITION_BY_EXERCISE_SUBNAME " +
            "AND $CONDITION_NOTES_WITH_EXERCISES_BY_BPM_RANGE"

private const val SELECT_COUNT_BY_EXERCISE_SUBNAME_TIMESTAMP_RANGE_AND_BPM_RANGE =
    "SELECT COUNT(*) " +
            "FROM $TABLE_NOTES_WITH_EXISTENT_EXERCISES " +
            "WHERE $CONDITION_BY_EXERCISE_SUBNAME " +
            "AND $CONDITION_NOTES_WITH_EXERCISES_BY_TIMESTAMP_RANGE " +
            "AND $CONDITION_NOTES_WITH_EXERCISES_BY_BPM_RANGE"

private const val SELECT_COUNT_BY_TIMESTAMP_RANGE =
    "SELECT COUNT(*) " +
            "FROM $TABLE_NOTES " +
            "WHERE $CONDITION_NOTES_BY_TIMESTAMP_RANGE"

private const val SELECT_COUNT_BY_BPM_RANGE =
    "SELECT COUNT(*) " +
            "FROM $TABLE_NOTES " +
            "WHERE $CONDITION_NOTES_BY_BPM_RANGE"

private const val SELECT_COUNT_BY_TIMESTAMP_RANGE_AND_BPM_RANGE =
    "SELECT COUNT(*) " +
            "FROM $TABLE_NOTES " +
            "WHERE $CONDITION_NOTES_BY_TIMESTAMP_RANGE " +
            "AND $CONDITION_NOTES_BY_BPM_RANGE"

private const val SELECT_COUNT_BY_EXERCISE_IDS =
    "SELECT COUNT(*) " +
            "FROM $TABLE_NOTES " +
            "WHERE $TABLE_NOTES_FIELD_EXERCISE_ID IN (:exerciseIds)"

private const val SELECT_COUNT_OTHER_BY_EXERCISE_IDS =
    "SELECT COUNT(*) " +
            "FROM $TABLE_NOTES_WITH_EXISTENT_EXERCISES " +
            "WHERE $TABLE_EXERCISES.$TABLE_EXERCISES_FIELD_ID NOT IN (:exerciseIds)"

private const val NOTES_WITH_EXERCISES_FIELDS =
    "$TABLE_NOTES.$TABLE_NOTES_FIELD_ID " +
            "AS $TABLE_NOTES_WITH_EXERCISES_FIELD_NOTE_ID, " +
            "$TABLE_NOTES.$TABLE_NOTES_FIELD_TIMESTAMP " +
            "AS $TABLE_NOTES_WITH_EXERCISES_FIELD_NOTE_TIMESTAMP, " +
            "$TABLE_EXERCISES.$TABLE_EXERCISES_FIELD_NAME " +
            "AS $TABLE_NOTES_WITH_EXERCISES_FIELD_EXERCISE_NAME, " +
            "$TABLE_NOTES.$TABLE_NOTES_FIELD_BPM " +
            "AS $TABLE_NOTES_WITH_EXERCISES_FIELD_NOTE_BPM"

private const val SELECT_ALL_UNORDERED = "SELECT * FROM $TABLE_NOTES"

private const val ORDER_NOTES_WITH_EXERCISES =
    "ORDER BY $TABLE_NOTES_WITH_EXERCISES_FIELD_NOTE_TIMESTAMP DESC, " +
            "$TABLE_NOTES_WITH_EXERCISES_FIELD_EXERCISE_NAME ASC, " +
            "$TABLE_NOTES_WITH_EXERCISES_FIELD_NOTE_BPM ASC"

private const val PORTION = "LIMIT :size OFFSET :offset"
private const val OPTIONS_FOR_SELECT_PORTION = "$ORDER_NOTES_WITH_EXERCISES $PORTION"

private const val SELECT_PORTION =
    "SELECT $NOTES_WITH_EXERCISES_FIELDS " +
            "FROM $TABLE_NOTES_WITH_EXERCISES " +
            OPTIONS_FOR_SELECT_PORTION

private const val SELECT_PORTION_BY_EXERCISE_SUBNAME =
    "SELECT $NOTES_WITH_EXERCISES_FIELDS " +
            "FROM $TABLE_NOTES_WITH_EXISTENT_EXERCISES " +
            "WHERE $CONDITION_BY_EXERCISE_SUBNAME " +
            OPTIONS_FOR_SELECT_PORTION

private const val SELECT_PORTION_BY_EXERCISE_SUBNAME_AND_TIMESTAMP_RANGE =
    "SELECT $NOTES_WITH_EXERCISES_FIELDS " +
            "FROM $TABLE_NOTES_WITH_EXISTENT_EXERCISES " +
            "WHERE $CONDITION_BY_EXERCISE_SUBNAME " +
            "AND $CONDITION_NOTES_WITH_EXERCISES_BY_TIMESTAMP_RANGE " +
            OPTIONS_FOR_SELECT_PORTION

private const val SELECT_PORTION_BY_EXERCISE_SUBNAME_AND_BPM_RANGE =
    "SELECT $NOTES_WITH_EXERCISES_FIELDS " +
            "FROM $TABLE_NOTES_WITH_EXISTENT_EXERCISES " +
            "WHERE $CONDITION_BY_EXERCISE_SUBNAME " +
            "AND $CONDITION_NOTES_WITH_EXERCISES_BY_BPM_RANGE " +
            OPTIONS_FOR_SELECT_PORTION

private const val SELECT_PORTION_BY_EXERCISE_SUBNAME_TIMESTAMP_RANGE_AND_BPM_RANGE =
    "SELECT $NOTES_WITH_EXERCISES_FIELDS " +
            "FROM $TABLE_NOTES_WITH_EXISTENT_EXERCISES " +
            "WHERE $CONDITION_BY_EXERCISE_SUBNAME " +
            "AND $CONDITION_NOTES_WITH_EXERCISES_BY_TIMESTAMP_RANGE " +
            "AND $CONDITION_NOTES_WITH_EXERCISES_BY_BPM_RANGE " +
            OPTIONS_FOR_SELECT_PORTION

private const val SELECT_PORTION_BY_TIMESTAMP_RANGE =
    "SELECT $NOTES_WITH_EXERCISES_FIELDS " +
            "FROM $TABLE_NOTES_WITH_EXISTENT_EXERCISES " +
            "WHERE $CONDITION_NOTES_WITH_EXERCISES_BY_TIMESTAMP_RANGE " +
            OPTIONS_FOR_SELECT_PORTION

private const val SELECT_PORTION_BY_BPM_RANGE =
    "SELECT $NOTES_WITH_EXERCISES_FIELDS " +
            "FROM $TABLE_NOTES_WITH_EXISTENT_EXERCISES " +
            "WHERE $CONDITION_NOTES_WITH_EXERCISES_BY_BPM_RANGE " +
            OPTIONS_FOR_SELECT_PORTION

private const val SELECT_PORTION_BY_TIMESTAMP_RANGE_AND_BPM_RANGE =
    "SELECT $NOTES_WITH_EXERCISES_FIELDS " +
            "FROM $TABLE_NOTES_WITH_EXISTENT_EXERCISES " +
            "WHERE $CONDITION_NOTES_WITH_EXERCISES_BY_TIMESTAMP_RANGE " +
            "AND $CONDITION_NOTES_WITH_EXERCISES_BY_BPM_RANGE " +
            OPTIONS_FOR_SELECT_PORTION

private const val DELETE_ALL = "DELETE FROM $TABLE_NOTES"
private const val CONDITION_NOTES_BY_IDS = "$TABLE_NOTES_FIELD_ID IN (:ids)"
private const val DELETE_BY_IDS = "DELETE FROM $TABLE_NOTES WHERE $CONDITION_NOTES_BY_IDS"

private const val CONDITION_NOTES_WITH_EXERCISES_BY_IDS =
    "$TABLE_NOTES.$TABLE_NOTES_FIELD_ID IN (:ids)"

private const val DELETE_BY_IDS_AND_EXERCISE_SUBNAME =
    "DELETE FROM $TABLE_NOTES " +
            "WHERE $TABLE_NOTES_FIELD_ID IN " +
            "(SELECT $TABLE_NOTES.$TABLE_NOTES_FIELD_ID " +
            "FROM $TABLE_NOTES_WITH_EXISTENT_EXERCISES " +
            "WHERE $CONDITION_NOTES_WITH_EXERCISES_BY_IDS " +
            "AND $CONDITION_BY_EXERCISE_SUBNAME)"

private const val DELETE_BY_IDS_EXERCISE_SUBNAME_AND_TIMESTAMP_RANGE =
    "DELETE FROM $TABLE_NOTES " +
            "WHERE $TABLE_NOTES_FIELD_ID IN " +
            "(SELECT $TABLE_NOTES.$TABLE_NOTES_FIELD_ID " +
            "FROM $TABLE_NOTES_WITH_EXISTENT_EXERCISES " +
            "WHERE $CONDITION_NOTES_WITH_EXERCISES_BY_IDS " +
            "AND $CONDITION_BY_EXERCISE_SUBNAME " +
            "AND $CONDITION_NOTES_WITH_EXERCISES_BY_TIMESTAMP_RANGE)"

private const val DELETE_BY_IDS_EXERCISE_SUBNAME_AND_BPM_RANGE =
    "DELETE FROM $TABLE_NOTES " +
            "WHERE $TABLE_NOTES_FIELD_ID IN " +
            "(SELECT $TABLE_NOTES.$TABLE_NOTES_FIELD_ID " +
            "FROM $TABLE_NOTES_WITH_EXISTENT_EXERCISES " +
            "WHERE $CONDITION_NOTES_WITH_EXERCISES_BY_IDS " +
            "AND $CONDITION_BY_EXERCISE_SUBNAME " +
            "AND $CONDITION_NOTES_WITH_EXERCISES_BY_BPM_RANGE)"

private const val DELETE_BY_IDS_EXERCISE_SUBNAME_TIMESTAMP_RANGE_AND_BPM_RANGE =
    "DELETE FROM $TABLE_NOTES " +
            "WHERE $TABLE_NOTES_FIELD_ID IN " +
            "(SELECT $TABLE_NOTES.$TABLE_NOTES_FIELD_ID " +
            "FROM $TABLE_NOTES_WITH_EXISTENT_EXERCISES " +
            "WHERE $CONDITION_NOTES_WITH_EXERCISES_BY_IDS " +
            "AND $CONDITION_BY_EXERCISE_SUBNAME " +
            "AND $CONDITION_NOTES_WITH_EXERCISES_BY_TIMESTAMP_RANGE " +
            "AND $CONDITION_NOTES_WITH_EXERCISES_BY_BPM_RANGE)"

private const val DELETE_BY_IDS_AND_TIMESTAMP_RANGE =
    "DELETE FROM $TABLE_NOTES " +
            "WHERE $TABLE_NOTES_FIELD_ID IN " +
            "(SELECT $TABLE_NOTES.$TABLE_NOTES_FIELD_ID " +
            "FROM $TABLE_NOTES_WITH_EXISTENT_EXERCISES " +
            "WHERE $CONDITION_NOTES_WITH_EXERCISES_BY_IDS " +
            "AND $CONDITION_NOTES_WITH_EXERCISES_BY_TIMESTAMP_RANGE)"

private const val DELETE_BY_IDS_AND_BPM_RANGE =
    "DELETE FROM $TABLE_NOTES " +
            "WHERE $TABLE_NOTES_FIELD_ID IN " +
            "(SELECT $TABLE_NOTES.$TABLE_NOTES_FIELD_ID " +
            "FROM $TABLE_NOTES_WITH_EXISTENT_EXERCISES " +
            "WHERE $CONDITION_NOTES_WITH_EXERCISES_BY_IDS " +
            "AND $CONDITION_NOTES_WITH_EXERCISES_BY_BPM_RANGE)"

private const val DELETE_BY_IDS_TIMESTAMP_RANGE_AND_BPM_RANGE =
    "DELETE FROM $TABLE_NOTES " +
            "WHERE $TABLE_NOTES_FIELD_ID IN " +
            "(SELECT $TABLE_NOTES.$TABLE_NOTES_FIELD_ID " +
            "FROM $TABLE_NOTES_WITH_EXISTENT_EXERCISES " +
            "WHERE $CONDITION_NOTES_WITH_EXERCISES_BY_IDS " +
            "AND $CONDITION_NOTES_WITH_EXERCISES_BY_TIMESTAMP_RANGE " +
            "AND $CONDITION_NOTES_WITH_EXERCISES_BY_BPM_RANGE)"

private const val CONDITION_OTHER_NOTES_BY_IDS = "$TABLE_NOTES_FIELD_ID NOT IN (:ids)"

private const val DELETE_OTHER_BY_IDS =
    "DELETE FROM $TABLE_NOTES " +
            "WHERE $CONDITION_OTHER_NOTES_BY_IDS"

private const val CONDITION_OTHER_NOTES_WITH_EXERCISES_BY_IDS =
    "$TABLE_NOTES.$TABLE_NOTES_FIELD_ID NOT IN (:ids)"

private const val DELETE_OTHER_BY_IDS_AND_EXERCISE_SUBNAME =
    "DELETE FROM $TABLE_NOTES " +
            "WHERE $TABLE_NOTES_FIELD_ID IN " +
            "(SELECT $TABLE_NOTES.$TABLE_NOTES_FIELD_ID " +
            "FROM $TABLE_NOTES_WITH_EXISTENT_EXERCISES " +
            "WHERE $CONDITION_OTHER_NOTES_WITH_EXERCISES_BY_IDS " +
            "AND $CONDITION_BY_EXERCISE_SUBNAME)"

private const val DELETE_OTHER_BY_IDS_EXERCISE_SUBNAME_AND_TIMESTAMP_RANGE =
    "DELETE FROM $TABLE_NOTES " +
            "WHERE $TABLE_NOTES_FIELD_ID IN " +
            "(SELECT $TABLE_NOTES.$TABLE_NOTES_FIELD_ID " +
            "FROM $TABLE_NOTES_WITH_EXISTENT_EXERCISES " +
            "WHERE $CONDITION_OTHER_NOTES_WITH_EXERCISES_BY_IDS " +
            "AND $CONDITION_BY_EXERCISE_SUBNAME " +
            "AND $CONDITION_NOTES_WITH_EXERCISES_BY_TIMESTAMP_RANGE)"

private const val DELETE_OTHER_BY_IDS_EXERCISE_SUBNAME_AND_BPM_RANGE =
    "DELETE FROM $TABLE_NOTES " +
            "WHERE $TABLE_NOTES_FIELD_ID IN " +
            "(SELECT $TABLE_NOTES.$TABLE_NOTES_FIELD_ID " +
            "FROM $TABLE_NOTES_WITH_EXISTENT_EXERCISES " +
            "WHERE $CONDITION_OTHER_NOTES_WITH_EXERCISES_BY_IDS " +
            "AND $CONDITION_BY_EXERCISE_SUBNAME " +
            "AND $CONDITION_NOTES_WITH_EXERCISES_BY_BPM_RANGE)"

private const val DELETE_OTHER_BY_IDS_EXERCISE_SUBNAME_TIMESTAMP_RANGE_AND_BPM_RANGE =
    "DELETE FROM $TABLE_NOTES " +
            "WHERE $TABLE_NOTES_FIELD_ID IN " +
            "(SELECT $TABLE_NOTES.$TABLE_NOTES_FIELD_ID " +
            "FROM $TABLE_NOTES_WITH_EXISTENT_EXERCISES " +
            "WHERE $CONDITION_OTHER_NOTES_WITH_EXERCISES_BY_IDS " +
            "AND $CONDITION_BY_EXERCISE_SUBNAME " +
            "AND $CONDITION_NOTES_WITH_EXERCISES_BY_TIMESTAMP_RANGE " +
            "AND $CONDITION_NOTES_WITH_EXERCISES_BY_BPM_RANGE)"

private const val DELETE_OTHER_BY_IDS_AND_TIMESTAMP_RANGE =
    "DELETE FROM $TABLE_NOTES " +
            "WHERE $TABLE_NOTES_FIELD_ID IN " +
            "(SELECT $TABLE_NOTES.$TABLE_NOTES_FIELD_ID " +
            "FROM $TABLE_NOTES_WITH_EXISTENT_EXERCISES " +
            "WHERE $CONDITION_OTHER_NOTES_WITH_EXERCISES_BY_IDS " +
            "AND $CONDITION_NOTES_WITH_EXERCISES_BY_TIMESTAMP_RANGE)"

private const val DELETE_OTHER_BY_IDS_AND_BPM_RANGE =
    "DELETE FROM $TABLE_NOTES " +
            "WHERE $TABLE_NOTES_FIELD_ID IN " +
            "(SELECT $TABLE_NOTES.$TABLE_NOTES_FIELD_ID " +
            "FROM $TABLE_NOTES_WITH_EXISTENT_EXERCISES " +
            "WHERE $CONDITION_OTHER_NOTES_WITH_EXERCISES_BY_IDS " +
            "AND $CONDITION_NOTES_WITH_EXERCISES_BY_BPM_RANGE)"

private const val DELETE_OTHER_BY_IDS_TIMESTAMP_RANGE_AND_BPM_RANGE =
    "DELETE FROM $TABLE_NOTES " +
            "WHERE $TABLE_NOTES_FIELD_ID IN " +
            "(SELECT $TABLE_NOTES.$TABLE_NOTES_FIELD_ID " +
            "FROM $TABLE_NOTES_WITH_EXISTENT_EXERCISES " +
            "WHERE $CONDITION_OTHER_NOTES_WITH_EXERCISES_BY_IDS " +
            "AND $CONDITION_NOTES_WITH_EXERCISES_BY_TIMESTAMP_RANGE " +
            "AND $CONDITION_NOTES_WITH_EXERCISES_BY_BPM_RANGE)"

/**
 * Operations with notes in the database.
 */
@Suppress("ComplexInterface")
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
     * Getting the count of notes whose exercise names contain [exerciseSubname] and the timestamp
     * is greater than or equal to [timestampFromInclusive] and less than [timestampToExclusive].
     *
     * @param exerciseSubname part of the names of the target notes' exercises.
     * @param timestampFromInclusive minimum timestamp value of the target notes' exercises.
     * @param timestampToExclusive value that should not be reached by the timestamp of the target
     * notes' exercises.
     * @return count of notes whose exercise names contain [exerciseSubname] and the timestamp is
     * greater than or equal to [timestampFromInclusive] and less than [timestampToExclusive].
     */
    @Query(SELECT_COUNT_BY_EXERCISE_SUBNAME_AND_TIMESTAMP_RANGE)
    suspend fun countByExerciseSubnameAndTimestampRangeSuspending(
        exerciseSubname: String,
        timestampFromInclusive: Long,
        timestampToExclusive: Long
    ): Int

    /**
     * Getting the count of notes whose exercise names contain [exerciseSubname] and the BPM is
     * greater than or equal to [bpmFromInclusive] and less than or equal to [bpmToInclusive].
     *
     * @param exerciseSubname part of the names of the target notes' exercises.
     * @param bpmFromInclusive minimum BPM value of the target notes' exercises.
     * @param bpmToInclusive maximum BPM value of the target notes' exercises.
     * @return count of notes whose exercise names contain [exerciseSubname] and the BPM is greater
     * than or equal to [bpmFromInclusive] and less than or equal to [bpmToInclusive].
     */
    @Query(SELECT_COUNT_BY_EXERCISE_SUBNAME_AND_BPM_RANGE)
    suspend fun countByExerciseSubnameAndBpmRangeSuspending(
        exerciseSubname: String,
        bpmFromInclusive: Int,
        bpmToInclusive: Int
    ): Int

    /**
     * Getting the count of notes whose exercise names contain [exerciseSubname], the timestamp is
     * greater than or equal to [timestampFromInclusive] and less than [timestampToExclusive] and
     * the BPM is greater than or equal to [bpmFromInclusive] and less than or equal to
     * [bpmToInclusive].
     *
     * @param exerciseSubname part of the names of the target notes' exercises.
     * @param timestampFromInclusive minimum timestamp value of the target notes' exercises.
     * @param timestampToExclusive value that should not be reached by the timestamp of the target
     * notes' exercises.
     * @param bpmFromInclusive minimum BPM value of the target notes' exercises.
     * @param bpmToInclusive maximum BPM value of the target notes' exercises.
     * @return count of notes whose exercise names contain [exerciseSubname], the timestamp is
     * greater than or equal to [timestampFromInclusive] and less than [timestampToExclusive] and
     * the BPM is greater than or equal to [bpmFromInclusive] and less than or equal to
     * [bpmToInclusive].
     */
    @Query(SELECT_COUNT_BY_EXERCISE_SUBNAME_TIMESTAMP_RANGE_AND_BPM_RANGE)
    suspend fun countByExerciseSubnameTimestampRangeAndBpmRangeSuspending(
        exerciseSubname: String,
        timestampFromInclusive: Long,
        timestampToExclusive: Long,
        bpmFromInclusive: Int,
        bpmToInclusive: Int
    ): Int

    /**
     * Getting the count of notes whose timestamp is greater than or equal to
     * [timestampFromInclusive] and less than [timestampToExclusive].
     *
     * @param timestampFromInclusive minimum timestamp value of the target notes' exercises.
     * @param timestampToExclusive value that should not be reached by the timestamp of the target
     * notes' exercises.
     * @return count of notes whose timestamp is greater than or equal to [timestampFromInclusive]
     * and less than [timestampToExclusive].
     */
    @Query(SELECT_COUNT_BY_TIMESTAMP_RANGE)
    suspend fun countByTimestampRangeSuspending(
        timestampFromInclusive: Long,
        timestampToExclusive: Long
    ): Int

    /**
     * Getting the count of notes whose BPM is greater than or equal to [bpmFromInclusive] and less
     * than or equal to [bpmToInclusive].
     *
     * @param bpmFromInclusive minimum BPM value of the target notes' exercises.
     * @param bpmToInclusive maximum BPM value of the target notes' exercises.
     * @return count of notes whose BPM is greater than or equal to [bpmFromInclusive] and less than
     * or equal to [bpmToInclusive].
     */
    @Query(SELECT_COUNT_BY_BPM_RANGE)
    suspend fun countByBpmRangeSuspending(
        bpmFromInclusive: Int,
        bpmToInclusive: Int
    ): Int

    /**
     * Getting the count of notes whose timestamp is greater than or equal to
     * [timestampFromInclusive] and less than [timestampToExclusive] and the BPM is greater than or
     * equal to [bpmFromInclusive] and less than or equal to [bpmToInclusive].
     *
     * @param timestampFromInclusive minimum timestamp value of the target notes' exercises.
     * @param timestampToExclusive value that should not be reached by the timestamp of the target
     * notes' exercises.
     * @param bpmFromInclusive minimum BPM value of the target notes' exercises.
     * @param bpmToInclusive maximum BPM value of the target notes' exercises.
     * @return count of notes whose timestamp is greater than or equal to [timestampFromInclusive]
     * and less than [timestampToExclusive] and the BPM is greater than or equal to
     * [bpmFromInclusive] and less than or equal to [bpmToInclusive].
     */
    @Query(SELECT_COUNT_BY_TIMESTAMP_RANGE_AND_BPM_RANGE)
    suspend fun countByTimestampRangeAndBpmRangeSuspending(
        timestampFromInclusive: Long,
        timestampToExclusive: Long,
        bpmFromInclusive: Int,
        bpmToInclusive: Int
    ): Int

    /**
     * Getting the count of notes with the specified [exerciseIds].
     *
     * @param exerciseIds identifiers of the target notes' exercises.
     * @return count of notes whose identifiers are in the [exerciseIds].
     */
    @Query(SELECT_COUNT_BY_EXERCISE_IDS)
    suspend fun countByExerciseIdsSuspending(exerciseIds: List<Id>): Int

    /**
     * Getting the count of notes with exercises whose identifiers are not in the [exerciseIds].
     *
     * @param exerciseIds identifiers of notes that should not be counted.
     * @return count of notes with exercises whose identifiers are not in the [exerciseIds].
     */
    @Query(SELECT_COUNT_OTHER_BY_EXERCISE_IDS)
    suspend fun countOtherByExerciseIdsSuspending(exerciseIds: List<Id>): Int

    /**
     * Getting a [List] of all note entities.
     *
     * @return [List] of all note entities.
     */
    @Query(SELECT_ALL_UNORDERED)
    suspend fun listAllUnorderedSuspending(): List<NoteEntity>

    /**
     * Getting a [List] of the [size] or fewer notes with their exercises' info in accordance with
     * the [offset] in the list of all notes. The notes are sorted in descending order by timestamp,
     * then ascending by exercise name, and then ascending by BPM.
     *
     * @param size limit the count of notes.
     * @param offset position of the first target note in the list of all notes.
     * @return [List] of the [size] or fewer notes with their exercises' info in accordance with the
     * [offset] in the list of all notes. The notes are sorted in descending order by timestamp,
     * then ascending by exercise name, and then ascending by BPM.
     */
    @Query(SELECT_PORTION)
    fun listPortion(size: Int, offset: Int): List<NoteWithExerciseInfo>

    /**
     * Getting a [List] of the [size] or fewer notes with their exercises' info in accordance with
     * the [offset] in the list of notes whose exercise names contain [exerciseSubname]. The notes
     * are sorted in descending order by timestamp, then ascending by exercise name, and then
     * ascending by BPM.
     *
     * @param size limit the count of notes.
     * @param offset position of the first target note in the list of notes whose exercise names
     * contain [exerciseSubname].
     * @param exerciseSubname part of the names of the target notes' exercises.
     * @return [List] of the [size] or fewer notes with their exercises' info in accordance with the
     * [offset] in the list of notes whose exercise names contain [exerciseSubname]. The notes are
     * sorted in descending order by timestamp, then ascending by exercise name, and then ascending
     * by BPM.
     */
    @Query(SELECT_PORTION_BY_EXERCISE_SUBNAME)
    fun listPortionByExerciseSubname(
        size: Int,
        offset: Int,
        exerciseSubname: String
    ): List<NoteWithExerciseInfo>

    /**
     * Getting a [List] of the [size] or fewer notes with their exercises' info in accordance with
     * the [offset] in the list of notes whose exercise names contain [exerciseSubname] and the
     * timestamp is greater than or equal to [timestampFromInclusive] and less than
     * [timestampToExclusive]. The notes are sorted in descending order by timestamp, then ascending
     * by exercise name, and then ascending by BPM.
     *
     * @param size limit the count of notes.
     * @param offset position of the first target note in the list of notes whose exercise names
     * contain [exerciseSubname] and the timestamp is greater than or equal to
     * [timestampFromInclusive] and less than [timestampToExclusive].
     * @param exerciseSubname part of the names of the target notes' exercises.
     * @param timestampFromInclusive minimum timestamp value of the target notes' exercises.
     * @param timestampToExclusive value that should not be reached by the timestamp of the target
     * notes' exercises.
     * @return [List] of the [size] or fewer notes with their exercises' info in accordance with the
     * [offset] in the list of notes whose exercise names contain [exerciseSubname] and the
     * timestamp is greater than or equal to [timestampFromInclusive] and less than
     * [timestampToExclusive]. The notes are sorted in descending order by timestamp, then ascending
     * by exercise name, and then ascending by BPM.
     */
    @Query(SELECT_PORTION_BY_EXERCISE_SUBNAME_AND_TIMESTAMP_RANGE)
    fun listPortionByExerciseSubnameAndTimestampRange(
        size: Int,
        offset: Int,
        exerciseSubname: String,
        timestampFromInclusive: Long,
        timestampToExclusive: Long
    ): List<NoteWithExerciseInfo>

    /**
     * Getting a [List] of the [size] or fewer notes with their exercises' info in accordance with
     * the [offset] in the list of notes whose exercise names contain [exerciseSubname] and the BPM
     * is greater than or equal to [bpmFromInclusive] and less than or equal to [bpmToInclusive].
     * The notes are sorted in descending order by timestamp, then ascending by exercise name, and
     * then ascending by BPM.
     *
     * @param size limit the count of notes.
     * @param offset position of the first target note in the list of notes whose exercise names
     * contain [exerciseSubname] and the BPM is greater than or equal to [bpmFromInclusive] and less
     * than or equal to [bpmToInclusive].
     * @param exerciseSubname part of the names of the target notes' exercises.
     * @param bpmFromInclusive minimum BPM value of the target notes' exercises.
     * @param bpmToInclusive maximum BPM value of the target notes' exercises.
     * @return [List] of the [size] or fewer notes with their exercises' info in accordance with the
     * [offset] in the list of notes whose exercise names contain [exerciseSubname] and the BPM is
     * greater than or equal to [bpmFromInclusive] and less than or equal to [bpmToInclusive]. The
     * notes are sorted in descending order by timestamp, then ascending by exercise name, and then
     * ascending by BPM.
     */
    @Query(SELECT_PORTION_BY_EXERCISE_SUBNAME_AND_BPM_RANGE)
    fun listPortionByExerciseSubnameAndBpmRange(
        size: Int,
        offset: Int,
        exerciseSubname: String,
        bpmFromInclusive: Int,
        bpmToInclusive: Int
    ): List<NoteWithExerciseInfo>

    /**
     * Getting a [List] of the [size] or fewer notes with their exercises' info in accordance with
     * the [offset] in the list of notes whose exercise names contain [exerciseSubname], the
     * timestamp is greater than or equal to [timestampFromInclusive] and less than
     * [timestampToExclusive] and the BPM is greater than or equal to [bpmFromInclusive] and less
     * than or equal to [bpmToInclusive]. The notes are sorted in descending order by timestamp,
     * then ascending by exercise name, and then ascending by BPM.
     *
     * @param size limit the count of notes.
     * @param offset position of the first target note in the list of notes whose exercise names
     * contain [exerciseSubname], the timestamp is greater than or equal to [timestampFromInclusive]
     * and less than [timestampToExclusive] and the BPM is greater than or equal to
     * [bpmFromInclusive] and less than or equal to [bpmToInclusive].
     * @param exerciseSubname part of the names of the target notes' exercises.
     * @param timestampFromInclusive minimum timestamp value of the target notes' exercises.
     * @param timestampToExclusive value that should not be reached by the timestamp of the target
     * notes' exercises.
     * @param bpmFromInclusive minimum BPM value of the target notes' exercises.
     * @param bpmToInclusive maximum BPM value of the target notes' exercises.
     * @return [List] of the [size] or fewer notes with their exercises' info in accordance with the
     * [offset] in the list of notes whose exercise names contain [exerciseSubname], the timestamp
     * is greater than or equal to [timestampFromInclusive] and less than [timestampToExclusive] and
     * the BPM is greater than or equal to [bpmFromInclusive] and less than or equal to
     * [bpmToInclusive]. The notes are sorted in descending order by timestamp, then ascending by
     * exercise name, and then ascending by BPM.
     */
    @Query(SELECT_PORTION_BY_EXERCISE_SUBNAME_TIMESTAMP_RANGE_AND_BPM_RANGE)
    fun listPortionByExerciseSubnameTimestampRangeAndBpmRange(
        size: Int,
        offset: Int,
        exerciseSubname: String,
        timestampFromInclusive: Long,
        timestampToExclusive: Long,
        bpmFromInclusive: Int,
        bpmToInclusive: Int
    ): List<NoteWithExerciseInfo>

    /**
     * Getting a [List] of the [size] or fewer notes with their exercises' info in accordance with
     * the [offset] in the list of notes whose timestamp is greater than or equal to
     * [timestampFromInclusive] and less than [timestampToExclusive]. The notes are sorted in
     * descending order by timestamp, then ascending by exercise name, and then ascending by BPM.
     *
     * @param size limit the count of notes.
     * @param offset position of the first target note in the list of notes whose timestamp is
     * greater than or equal to [timestampFromInclusive] and less than [timestampToExclusive].
     * @param timestampFromInclusive minimum timestamp value of the target notes' exercises.
     * @param timestampToExclusive value that should not be reached by the timestamp of the target
     * notes' exercises.
     * @return [List] of the [size] or fewer notes with their exercises' info in accordance with the
     * [offset] in the list of notes whose timestamp is greater than or equal to
     * [timestampFromInclusive] and less than [timestampToExclusive]. The notes are sorted in
     * descending order by timestamp, then ascending by exercise name, and then ascending by BPM.
     */
    @Query(SELECT_PORTION_BY_TIMESTAMP_RANGE)
    fun listPortionByTimestampRange(
        size: Int,
        offset: Int,
        timestampFromInclusive: Long,
        timestampToExclusive: Long
    ): List<NoteWithExerciseInfo>

    /**
     * Getting a [List] of the [size] or fewer notes with their exercises' info in accordance with
     * the [offset] in the list of notes whose BPM is greater than or equal to [bpmFromInclusive]
     * and less than or equal to [bpmToInclusive]. The notes are sorted in descending order by
     * timestamp, then ascending by exercise name, and then ascending by BPM.
     *
     * @param size limit the count of notes.
     * @param offset position of the first target note in the list of notes whose BPM is greater
     * than or equal to [bpmFromInclusive] and less than or equal to [bpmToInclusive].
     * @param bpmFromInclusive minimum BPM value of the target notes' exercises.
     * @param bpmToInclusive maximum BPM value of the target notes' exercises.
     * @return [List] of the [size] or fewer notes with their exercises' info in accordance with the
     * [offset] in the list of notes whose BPM is greater than or equal to [bpmFromInclusive] and
     * less than or equal to [bpmToInclusive]. The notes are sorted in descending order by
     * timestamp, then ascending by exercise name, and then ascending by BPM.
     */
    @Query(SELECT_PORTION_BY_BPM_RANGE)
    fun listPortionByBpmRange(
        size: Int,
        offset: Int,
        bpmFromInclusive: Int,
        bpmToInclusive: Int
    ): List<NoteWithExerciseInfo>

    /**
     * Getting a [List] of the [size] or fewer notes with their exercises' info in accordance with
     * the [offset] in the list of notes whose timestamp is greater than or equal to
     * [timestampFromInclusive] and less than [timestampToExclusive] and the BPM is greater than or
     * equal to [bpmFromInclusive] and less than or equal to [bpmToInclusive]. The notes are sorted
     * in descending order by timestamp, then ascending by exercise name, and then ascending by BPM.
     *
     * @param size limit the count of notes.
     * @param offset position of the first target note in the list of notes whose timestamp is
     * greater than or equal to [timestampFromInclusive] and less than [timestampToExclusive] and
     * the BPM is greater than or equal to [bpmFromInclusive] and less than or equal to
     * [bpmToInclusive].
     * @param timestampFromInclusive minimum timestamp value of the target notes' exercises.
     * @param timestampToExclusive value that should not be reached by the timestamp of the target
     * notes' exercises.
     * @param bpmFromInclusive minimum BPM value of the target notes' exercises.
     * @param bpmToInclusive maximum BPM value of the target notes' exercises.
     * @return [List] of the [size] or fewer notes with their exercises' info in accordance with the
     * [offset] in the list of notes whose timestamp is greater than or equal to
     * [timestampFromInclusive] and less than [timestampToExclusive] and the BPM is greater than or
     * equal to [bpmFromInclusive] and less than or equal to [bpmToInclusive]. The notes are sorted
     * in descending order by timestamp, then ascending by exercise name, and then ascending by BPM.
     */
    @Query(SELECT_PORTION_BY_TIMESTAMP_RANGE_AND_BPM_RANGE)
    fun listPortionByTimestampRangeAndBpmRange(
        size: Int,
        offset: Int,
        timestampFromInclusive: Long,
        timestampToExclusive: Long,
        bpmFromInclusive: Int,
        bpmToInclusive: Int
    ): List<NoteWithExerciseInfo>

    /**
     * Deleting information about all notes.
     *
     * @return count of deleted rows from the database table.
     */
    @Query(DELETE_ALL)
    fun deleteAll(): Int

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
     * Deleting information about notes with the specified [ids] whose exercise names contain
     * [exerciseSubname] and the timestamp is greater than or equal to [timestampFromInclusive]
     * and less than [timestampToExclusive].
     *
     * @param ids identifiers of notes to delete.
     * @param exerciseSubname part of the names of the target notes' exercises.
     * @param timestampFromInclusive minimum timestamp value of the target notes' exercises.
     * @param timestampToExclusive value that should not be reached by the timestamp of the target
     * notes' exercises.
     * @return count of deleted rows from the database table.
     */
    @Query(DELETE_BY_IDS_EXERCISE_SUBNAME_AND_TIMESTAMP_RANGE)
    suspend fun deleteByIdsExerciseSubnameAndTimestampRangeSuspending(
        ids: List<Id>,
        exerciseSubname: String,
        timestampFromInclusive: Long,
        timestampToExclusive: Long
    ): Int

    /**
     * Deleting information about notes with the specified [ids] whose exercise names contain
     * [exerciseSubname] and the BPM is greater than or equal to [bpmFromInclusive] and less than or
     * equal to [bpmToInclusive].
     *
     * @param ids identifiers of notes to delete.
     * @param exerciseSubname part of the names of the target notes' exercises.
     * @param bpmFromInclusive minimum BPM value of the target notes' exercises.
     * @param bpmToInclusive maximum BPM value of the target notes' exercises.
     * @return count of deleted rows from the database table.
     */
    @Query(DELETE_BY_IDS_EXERCISE_SUBNAME_AND_BPM_RANGE)
    suspend fun deleteByIdsExerciseSubnameAndBpmRangeSuspending(
        ids: List<Id>,
        exerciseSubname: String,
        bpmFromInclusive: Int,
        bpmToInclusive: Int
    ): Int

    /**
     * Deleting information about notes with the specified [ids] whose exercise names contain
     * [exerciseSubname], the timestamp is greater than or equal to [timestampFromInclusive] and
     * less than [timestampToExclusive] and the BPM is greater than or equal to [bpmFromInclusive]
     * and less than or equal to [bpmToInclusive].
     *
     * @param ids identifiers of notes to delete.
     * @param exerciseSubname part of the names of the target notes' exercises.
     * @param timestampFromInclusive minimum timestamp value of the target notes' exercises.
     * @param timestampToExclusive value that should not be reached by the timestamp of the target
     * notes' exercises.
     * @param bpmFromInclusive minimum BPM value of the target notes' exercises.
     * @param bpmToInclusive maximum BPM value of the target notes' exercises.
     * @return count of deleted rows from the database table.
     */
    @Query(DELETE_BY_IDS_EXERCISE_SUBNAME_TIMESTAMP_RANGE_AND_BPM_RANGE)
    suspend fun deleteByIdsExerciseSubnameTimestampRangeAndBpmRangeSuspending(
        ids: List<Id>,
        exerciseSubname: String,
        timestampFromInclusive: Long,
        timestampToExclusive: Long,
        bpmFromInclusive: Int,
        bpmToInclusive: Int
    ): Int

    /**
     * Deleting information about notes with the specified [ids] whose timestamp is greater than or
     * equal to [timestampFromInclusive] and less than [timestampToExclusive].
     *
     * @param ids identifiers of notes to delete.
     * @param timestampFromInclusive minimum timestamp value of the target notes' exercises.
     * @param timestampToExclusive value that should not be reached by the timestamp of the target
     * notes' exercises.
     * @return count of deleted rows from the database table.
     */
    @Query(DELETE_BY_IDS_AND_TIMESTAMP_RANGE)
    suspend fun deleteByIdsAndTimestampRangeSuspending(
        ids: List<Id>,
        timestampFromInclusive: Long,
        timestampToExclusive: Long
    ): Int

    /**
     * Deleting information about notes with the specified [ids] whose BPM is greater than or equal
     * to [bpmFromInclusive] and less than or equal to [bpmToInclusive].
     *
     * @param ids identifiers of notes to delete.
     * @param bpmFromInclusive minimum BPM value of the target notes' exercises.
     * @param bpmToInclusive maximum BPM value of the target notes' exercises.
     * @return count of deleted rows from the database table.
     */
    @Query(DELETE_BY_IDS_AND_BPM_RANGE)
    suspend fun deleteByIdsAndBpmRangeSuspending(
        ids: List<Id>,
        bpmFromInclusive: Int,
        bpmToInclusive: Int
    ): Int

    /**
     * Deleting information about notes with the specified [ids] whose timestamp is greater than or
     * equal to [timestampFromInclusive] and less than [timestampToExclusive] and the BPM is greater
     * than or equal to [bpmFromInclusive] and less than or equal to [bpmToInclusive].
     *
     * @param ids identifiers of notes to delete.
     * @param timestampFromInclusive minimum timestamp value of the target notes' exercises.
     * @param timestampToExclusive value that should not be reached by the timestamp of the target
     * notes' exercises.
     * @param bpmFromInclusive minimum BPM value of the target notes' exercises.
     * @param bpmToInclusive maximum BPM value of the target notes' exercises.
     * @return count of deleted rows from the database table.
     */
    @Query(DELETE_BY_IDS_TIMESTAMP_RANGE_AND_BPM_RANGE)
    suspend fun deleteByIdsTimestampRangeAndBpmRangeSuspending(
        ids: List<Id>,
        timestampFromInclusive: Long,
        timestampToExclusive: Long,
        bpmFromInclusive: Int,
        bpmToInclusive: Int
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
     * Deleting information about notes whose identifiers are not in the [ids] whose exercise names
     * contain [exerciseSubname] and the timestamp is greater than or equal to
     * [timestampFromInclusive] and less than [timestampToExclusive].
     *
     * @param ids identifiers of notes that should not be deleted.
     * @param exerciseSubname part of the names of the target notes' exercises.
     * @param timestampFromInclusive minimum timestamp value of the target notes' exercises.
     * @param timestampToExclusive value that should not be reached by the timestamp of the target
     * notes' exercises.
     * @return count of deleted rows from the database table.
     */
    @Query(DELETE_OTHER_BY_IDS_EXERCISE_SUBNAME_AND_TIMESTAMP_RANGE)
    suspend fun deleteOtherByIdsExerciseSubnameAndTimestampRangeSuspending(
        ids: List<Id>,
        exerciseSubname: String,
        timestampFromInclusive: Long,
        timestampToExclusive: Long
    ): Int

    /**
     * Deleting information about notes whose identifiers are not in the [ids] and whose exercise
     * names contain [exerciseSubname] and the BPM is greater than or equal to [bpmFromInclusive]
     * and less than or equal to [bpmToInclusive].
     *
     * @param ids identifiers of notes that should not be deleted.
     * @param exerciseSubname part of the names of the target notes' exercises.
     * @param bpmFromInclusive minimum BPM value of the target notes' exercises.
     * @param bpmToInclusive maximum BPM value of the target notes' exercises.
     * @return count of deleted rows from the database table.
     */
    @Query(DELETE_OTHER_BY_IDS_EXERCISE_SUBNAME_AND_BPM_RANGE)
    suspend fun deleteOtherByIdsExerciseSubnameAndBpmRangeSuspending(
        ids: List<Id>,
        exerciseSubname: String,
        bpmFromInclusive: Int,
        bpmToInclusive: Int
    ): Int

    /**
     * Deleting information about notes whose exercise names contain [exerciseSubname], the
     * timestamp is greater than or equal to [timestampFromInclusive] and less than
     * [timestampToExclusive] and the BPM is greater than or equal to [bpmFromInclusive] and less
     * than or equal to [bpmToInclusive].
     *
     * @param ids identifiers of notes that should not be deleted.
     * @param exerciseSubname part of the names of the target notes' exercises.
     * @param timestampFromInclusive minimum timestamp value of the target notes' exercises.
     * @param timestampToExclusive value that should not be reached by the timestamp of the target
     * notes' exercises.
     * @param bpmFromInclusive minimum BPM value of the target notes' exercises.
     * @param bpmToInclusive maximum BPM value of the target notes' exercises.
     * @return count of deleted rows from the database table.
     */
    @Query(DELETE_OTHER_BY_IDS_EXERCISE_SUBNAME_TIMESTAMP_RANGE_AND_BPM_RANGE)
    suspend fun deleteOtherByIdsExerciseSubnameTimestampRangeAndBpmRangeSuspending(
        ids: List<Id>,
        exerciseSubname: String,
        timestampFromInclusive: Long,
        timestampToExclusive: Long,
        bpmFromInclusive: Int,
        bpmToInclusive: Int
    ): Int

    /**
     * Deleting information about notes whose identifiers are not in the [ids] and whose timestamp
     * is greater than or equal to [timestampFromInclusive] and less than [timestampToExclusive].
     *
     * @param ids identifiers of notes that should not be deleted.
     * @param timestampFromInclusive minimum timestamp value of the target notes' exercises.
     * @param timestampToExclusive value that should not be reached by the timestamp of the target
     * notes' exercises.
     * @return count of deleted rows from the database table.
     */
    @Query(DELETE_OTHER_BY_IDS_AND_TIMESTAMP_RANGE)
    suspend fun deleteOtherByIdsAndTimestampRangeSuspending(
        ids: List<Id>,
        timestampFromInclusive: Long,
        timestampToExclusive: Long
    ): Int

    /**
     * Deleting information about notes whose identifiers are not in the [ids] and whose BPM is
     * greater than or equal to [bpmFromInclusive] and less than or equal to [bpmToInclusive].
     *
     * @param ids identifiers of notes that should not be deleted.
     * @param bpmFromInclusive minimum BPM value of the target notes' exercises.
     * @param bpmToInclusive maximum BPM value of the target notes' exercises.
     * @return count of deleted rows from the database table.
     */
    @Query(DELETE_OTHER_BY_IDS_AND_BPM_RANGE)
    suspend fun deleteOtherByIdsAndBpmRangeSuspending(
        ids: List<Id>,
        bpmFromInclusive: Int,
        bpmToInclusive: Int
    ): Int

    /**
     * Deleting information about notes whose identifiers are not in the [ids] and whose timestamp
     * is greater than or equal to [timestampFromInclusive] and less than [timestampToExclusive] and
     * the BPM is greater than or equal to [bpmFromInclusive] and less than or equal to
     * [bpmToInclusive].
     *
     * @param ids identifiers of notes that should not be deleted.
     * @param timestampFromInclusive minimum timestamp value of the target notes' exercises.
     * @param timestampToExclusive value that should not be reached by the timestamp of the target
     * notes' exercises.
     * @param bpmFromInclusive minimum BPM value of the target notes' exercises.
     * @param bpmToInclusive maximum BPM value of the target notes' exercises.
     * @return count of deleted rows from the database table.
     */
    @Query(DELETE_OTHER_BY_IDS_TIMESTAMP_RANGE_AND_BPM_RANGE)
    suspend fun deleteOtherByIdsTimestampRangeAndBpmRangeSuspending(
        ids: List<Id>,
        timestampFromInclusive: Long,
        timestampToExclusive: Long,
        bpmFromInclusive: Int,
        bpmToInclusive: Int
    ): Int

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
}
