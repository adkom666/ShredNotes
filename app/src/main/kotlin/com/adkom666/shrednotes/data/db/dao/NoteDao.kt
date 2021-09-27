package com.adkom666.shrednotes.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.adkom666.shrednotes.common.Id
import com.adkom666.shrednotes.data.db.entity.NoteCountPerExerciseInfo
import com.adkom666.shrednotes.data.db.entity.NoteEntity
import com.adkom666.shrednotes.data.db.entity.NoteWithExerciseInfo
import com.adkom666.shrednotes.data.db.entity.TABLE_EXERCISES
import com.adkom666.shrednotes.data.db.entity.TABLE_EXERCISES_FIELD_ID
import com.adkom666.shrednotes.data.db.entity.TABLE_EXERCISES_FIELD_NAME
import com.adkom666.shrednotes.data.db.entity.TABLE_NOTE_COUNT_PER_EXERCISE_FIELD_EXERCISE_ID
import com.adkom666.shrednotes.data.db.entity.TABLE_NOTE_COUNT_PER_EXERCISE_FIELD_EXERCISE_NAME
import com.adkom666.shrednotes.data.db.entity.TABLE_NOTE_COUNT_PER_EXERCISE_FIELD_NOTE_COUNT
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

private const val SELECT_FIRST_NOTE_TIMESTAMP =
    "SELECT MIN($TABLE_NOTES_FIELD_TIMESTAMP) " +
            "FROM $TABLE_NOTES"

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

private const val SELECT_ALL_WITH_EXERCISES_UNORDERED =
    "SELECT $NOTES_WITH_EXERCISES_FIELDS " +
            "FROM $TABLE_NOTES_WITH_EXERCISES"

private const val SELECT_BY_TIMESTAMP_RANGE_WITH_EXERCISES_UNORDERED =
    "SELECT $NOTES_WITH_EXERCISES_FIELDS " +
            "FROM $TABLE_NOTES_WITH_EXERCISES " +
            "WHERE $CONDITION_NOTES_WITH_EXERCISES_BY_TIMESTAMP_RANGE"

private const val ORDER_TOP_BPM_NOTES_WITH_EXERCISES =
    "ORDER BY $TABLE_NOTES_WITH_EXERCISES_FIELD_NOTE_BPM DESC, " +
            "$TABLE_NOTES_WITH_EXERCISES_FIELD_NOTE_TIMESTAMP DESC, " +
            "$TABLE_NOTES_WITH_EXERCISES_FIELD_EXERCISE_NAME ASC"

private const val TOP_PORTION = "LIMIT :size"

private const val MAX_BPM_PER_EXERCISE_ID =
    "$TABLE_NOTES_FIELD_EXERCISE_ID, " +
            "IFNULL(MAX($TABLE_NOTES_FIELD_BPM), 0) " +
            "AS $TABLE_NOTES_FIELD_BPM"

private const val GROUP_BY_NOTE_EXERCISE_ID =
    "GROUP BY $TABLE_NOTES_FIELD_EXERCISE_ID"

private const val SELECT_MAX_BPM_PER_EXERCISE_ID =
    "SELECT $MAX_BPM_PER_EXERCISE_ID " +
            "FROM $TABLE_NOTES " +
            GROUP_BY_NOTE_EXERCISE_ID

private const val SELECT_MAX_BPM_PER_EXERCISE_ID_BY_TIMESTAMP_RANGE =
    "SELECT $MAX_BPM_PER_EXERCISE_ID " +
            "FROM $TABLE_NOTES " +
            "WHERE $CONDITION_NOTES_BY_TIMESTAMP_RANGE " +
            GROUP_BY_NOTE_EXERCISE_ID

// Fields must be named the same as TABLE_NOTES fields so that we can substitute them.
private const val TOP_BPM_WITH_EXERCISE_IDS =
    "src_notes.$TABLE_NOTES_FIELD_ID, " +
            "src_notes.$TABLE_NOTES_FIELD_EXERCISE_ID, " +
            "src_notes.$TABLE_NOTES_FIELD_BPM, " +
            "IFNULL(MAX(src_notes.$TABLE_NOTES_FIELD_TIMESTAMP), 0) " +
            "AS $TABLE_NOTES_FIELD_TIMESTAMP"

private const val JOIN_MAX_BPM_PER_EXERCISE_ID_TO_NOTES =
    "INNER JOIN $TABLE_NOTES src_notes " +
            "ON src_notes.$TABLE_NOTES_FIELD_EXERCISE_ID = " +
            "    max_bpm_per_exercise_id.$TABLE_NOTES_FIELD_EXERCISE_ID " +
            "AND src_notes.$TABLE_NOTES_FIELD_BPM = " +
            "    max_bpm_per_exercise_id.$TABLE_NOTES_FIELD_BPM"

private const val MAX_BPM_PER_EXERCISE_ID_JOINED_TO_NOTES =
    "($SELECT_MAX_BPM_PER_EXERCISE_ID) max_bpm_per_exercise_id " +
            JOIN_MAX_BPM_PER_EXERCISE_ID_TO_NOTES

private const val MAX_BPM_PER_EXERCISE_ID_BY_TIMESTAMP_RANGE_JOINED_TO_NOTES =
    "($SELECT_MAX_BPM_PER_EXERCISE_ID_BY_TIMESTAMP_RANGE) max_bpm_per_exercise_id " +
            JOIN_MAX_BPM_PER_EXERCISE_ID_TO_NOTES

private const val GROUP_TOP_BPM_WITH_EXERCISE_IDS =
    "GROUP BY src_notes.$TABLE_NOTES_FIELD_EXERCISE_ID, src_notes.$TABLE_NOTES_FIELD_BPM"

// Results table fields must be named the same as TABLE_NOTES fields so that we can substitute them.
private const val SELECT_TOP_BPM_WITH_EXERCISE_IDS =
    "SELECT $TOP_BPM_WITH_EXERCISE_IDS " +
            "FROM $MAX_BPM_PER_EXERCISE_ID_JOINED_TO_NOTES " +
            GROUP_TOP_BPM_WITH_EXERCISE_IDS

// Results table fields must be named the same as TABLE_NOTES fields so that we can substitute them.
private const val SELECT_TOP_BPM_WITH_EXERCISE_IDS_BY_TIMESTAMP_RANGE =
    "SELECT $TOP_BPM_WITH_EXERCISE_IDS " +
            "FROM $MAX_BPM_PER_EXERCISE_ID_BY_TIMESTAMP_RANGE_JOINED_TO_NOTES " +
            GROUP_TOP_BPM_WITH_EXERCISE_IDS

private const val JOIN_TOP_BPM_NOTES_TO_EXERCISES =
    "LEFT JOIN $TABLE_EXERCISES " +
            "ON $TABLE_NOTES.$TABLE_NOTES_FIELD_EXERCISE_ID = " +
            "$TABLE_EXERCISES.$TABLE_EXERCISES_FIELD_ID"

private const val SELECT_TOP_BPM_WITH_EXERCISES =
    "SELECT $NOTES_WITH_EXERCISES_FIELDS " +
            "FROM ($SELECT_TOP_BPM_WITH_EXERCISE_IDS) $TABLE_NOTES " +
            "$JOIN_TOP_BPM_NOTES_TO_EXERCISES " +
            "$ORDER_TOP_BPM_NOTES_WITH_EXERCISES " +
            TOP_PORTION

private const val SELECT_TOP_BPM_WITH_EXERCISES_BY_TIMESTAMP_RANGE =
    "SELECT $NOTES_WITH_EXERCISES_FIELDS " +
            "FROM ($SELECT_TOP_BPM_WITH_EXERCISE_IDS_BY_TIMESTAMP_RANGE) $TABLE_NOTES " +
            "$JOIN_TOP_BPM_NOTES_TO_EXERCISES " +
            "WHERE $CONDITION_NOTES_WITH_EXERCISES_BY_TIMESTAMP_RANGE " +
            "$ORDER_TOP_BPM_NOTES_WITH_EXERCISES " +
            TOP_PORTION

private const val TOP_POPULAR_EXERCISES =
    "$TABLE_EXERCISES.$TABLE_EXERCISES_FIELD_ID " +
            "AS $TABLE_NOTE_COUNT_PER_EXERCISE_FIELD_EXERCISE_ID, " +
            "$TABLE_EXERCISES.$TABLE_EXERCISES_FIELD_NAME " +
            "AS $TABLE_NOTE_COUNT_PER_EXERCISE_FIELD_EXERCISE_NAME, " +
            "COUNT(*) " +
            "AS $TABLE_NOTE_COUNT_PER_EXERCISE_FIELD_NOTE_COUNT"

private const val ORDER_TOP_POPULAR_EXERCISES =
    "ORDER BY $TABLE_NOTE_COUNT_PER_EXERCISE_FIELD_NOTE_COUNT DESC, " +
            "$TABLE_NOTE_COUNT_PER_EXERCISE_FIELD_EXERCISE_NAME ASC"

private const val GROUP_TOP_POPULAR_EXERCISES =
    "GROUP BY $TABLE_NOTE_COUNT_PER_EXERCISE_FIELD_EXERCISE_ID"

private const val SELECT_TOP_POPULAR_EXERCISES =
    "SELECT $TOP_POPULAR_EXERCISES " +
            "FROM $TABLE_NOTES_WITH_EXERCISES " +
            "$GROUP_TOP_POPULAR_EXERCISES " +
            "$ORDER_TOP_POPULAR_EXERCISES " +
            TOP_PORTION

private const val SELECT_TOP_POPULAR_EXERCISES_BY_TIMESTAMP_RANGE =
    "SELECT $TOP_POPULAR_EXERCISES " +
            "FROM $TABLE_NOTES_WITH_EXERCISES " +
            "WHERE $CONDITION_NOTES_WITH_EXERCISES_BY_TIMESTAMP_RANGE " +
            "$GROUP_TOP_POPULAR_EXERCISES " +
            "$ORDER_TOP_POPULAR_EXERCISES " +
            TOP_PORTION

private const val ORDER_NOTES_WITH_EXERCISES =
    "ORDER BY $TABLE_NOTES_WITH_EXERCISES_FIELD_NOTE_TIMESTAMP DESC, " +
            "$TABLE_NOTES_WITH_EXERCISES_FIELD_EXERCISE_NAME ASC, " +
            "$TABLE_NOTES_WITH_EXERCISES_FIELD_NOTE_BPM DESC"

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
     * @param timestampFromInclusive minimum timestamp value of the target notes.
     * @param timestampToExclusive value that should not be reached by the timestamp of the target
     * notes.
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
     * @param bpmFromInclusive minimum BPM value of the target notes.
     * @param bpmToInclusive maximum BPM value of the target notes.
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
     * @param timestampFromInclusive minimum timestamp value of the target notes.
     * @param timestampToExclusive value that should not be reached by the timestamp of the target
     * notes.
     * @param bpmFromInclusive minimum BPM value of the target notes.
     * @param bpmToInclusive maximum BPM value of the target notes.
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
     * @param timestampFromInclusive minimum timestamp value of the target notes.
     * @param timestampToExclusive value that should not be reached by the timestamp of the target
     * notes.
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
     * @param bpmFromInclusive minimum BPM value of the target notes.
     * @param bpmToInclusive maximum BPM value of the target notes.
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
     * @param timestampFromInclusive minimum timestamp value of the target notes.
     * @param timestampToExclusive value that should not be reached by the timestamp of the target
     * notes.
     * @param bpmFromInclusive minimum BPM value of the target notes.
     * @param bpmToInclusive maximum BPM value of the target notes.
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
     * Getting the timestamp of the first note or null if it does not exist.
     *
     * @return timestamp of the first note or null if it does not exist.
     */
    @Query(SELECT_FIRST_NOTE_TIMESTAMP)
    suspend fun firstNoteTimestampSuspending(): Long?

    /**
     * Getting a [List] of all note entities.
     *
     * @return [List] of all note entities.
     */
    @Query(SELECT_ALL_UNORDERED)
    suspend fun listAllUnorderedSuspending(): List<NoteEntity>

    /**
     * Getting a [List] of all notes with their exercises' info.
     *
     * @return [List] of all notes with their exercises' info.
     */
    @Query(SELECT_ALL_WITH_EXERCISES_UNORDERED)
    suspend fun listAllWithExercisesUnorderedSuspending(): List<NoteWithExerciseInfo>

    /**
     * Getting a [List] of notes with their exercises' info whose timestamp is greater than or equal
     * to [timestampFromInclusive] and less than [timestampToExclusive].
     *
     * @param timestampFromInclusive minimum timestamp value of the target notes.
     * @param timestampToExclusive value that should not be reached by the timestamp of the target
     * notes.
     * @return [List] of notes with their exercises' info whose timestamp is greater than or equal
     * to [timestampFromInclusive] and less than [timestampToExclusive].
     */
    @Query(SELECT_BY_TIMESTAMP_RANGE_WITH_EXERCISES_UNORDERED)
    suspend fun listByTimestampRangeWithExercisesUnorderedSuspending(
        timestampFromInclusive: Long,
        timestampToExclusive: Long
    ): List<NoteWithExerciseInfo>

    /**
     * Getting a [List] of the [size] or fewer notes with their exercises' info. Notes are grouped
     * by exercise name, and each group consists of one note with maximum BPM and maximum timestamp.
     *
     * @param size limit the count of notes.
     * @return [List] of the [size] or fewer notes with their exercises' info. Notes are grouped by
     * exercise name, and each group consists of one note with maximum BPM and maximum timestamp.
     */
    @Query(SELECT_TOP_BPM_WITH_EXERCISES)
    suspend fun listTopBpmWithExercisesSuspending(size: Int): List<NoteWithExerciseInfo>

    /**
     * Getting a [List] of the [size] or fewer notes with their exercises' info whose timestamp is
     * greater than or equal to [timestampFromInclusive] and less than [timestampToExclusive]. Notes
     * are grouped by exercise name, and each group consists of one note with maximum BPM and
     * maximum timestamp.
     *
     * @param size limit the count of notes.
     * @param timestampFromInclusive minimum timestamp value of the target notes.
     * @param timestampToExclusive value that should not be reached by the timestamp of the target
     * notes.
     * @return [List] of the [size] or fewer notes with their exercises' info whose timestamp is
     * greater than or equal to [timestampFromInclusive] and less than [timestampToExclusive]. Notes
     * are grouped by exercise name, and each group consists of one note with maximum BPM and
     * maximum timestamp.
     */
    @Query(SELECT_TOP_BPM_WITH_EXERCISES_BY_TIMESTAMP_RANGE)
    suspend fun listTopBpmWithExercisesByTimestampRangeSuspending(
        size: Int,
        timestampFromInclusive: Long,
        timestampToExclusive: Long
    ): List<NoteWithExerciseInfo>

    /**
     * Getting a [List] of the [size] or fewer [NoteCountPerExerciseInfo] objects. They are sorted
     * in descending order by note count and then ascending by exercise name.
     *
     * @param size limit the count of [NoteCountPerExerciseInfo] objects.
     * @return [List] of the [size] or fewer [NoteCountPerExerciseInfo] objects. They are sorted in
     * descending order by note count and then ascending by exercise name.
     */
    @Query(SELECT_TOP_POPULAR_EXERCISES)
    suspend fun listTopPopularExercisesSuspending(size: Int): List<NoteCountPerExerciseInfo>

    /**
     * Getting a [List] of the [size] or fewer [NoteCountPerExerciseInfo] objects whose related
     * notes' timestamp is greater than or equal to [timestampFromInclusive] and less than
     * [timestampToExclusive]. They are sorted in descending order by note count and then ascending
     * by exercise name.
     *
     * @param size limit the count of [NoteCountPerExerciseInfo] objects.
     * @param timestampFromInclusive minimum timestamp value of the related notes.
     * @param timestampToExclusive value that should not be reached by the timestamp of the related
     * notes.
     * @return [List] of the [size] or fewer [NoteCountPerExerciseInfo] objects whose related notes'
     * timestamp is greater than or equal to [timestampFromInclusive] and less than
     * [timestampToExclusive]. They are sorted in descending order by note count and then ascending
     * by exercise name.
     */
    @Query(SELECT_TOP_POPULAR_EXERCISES_BY_TIMESTAMP_RANGE)
    suspend fun listTopPopularExercisesByTimestampRangeSuspending(
        size: Int,
        timestampFromInclusive: Long,
        timestampToExclusive: Long
    ): List<NoteCountPerExerciseInfo>

    /**
     * Getting a [List] of the [size] or fewer notes with their exercises' info in accordance with
     * the [offset] in the list of all notes. The notes are sorted in descending order by timestamp,
     * then ascending by exercise name, and then descending by BPM.
     *
     * @param size limit the count of notes.
     * @param offset position of the first target note in the list of all notes.
     * @return [List] of the [size] or fewer notes with their exercises' info in accordance with the
     * [offset] in the list of all notes. The notes are sorted in descending order by timestamp,
     * then ascending by exercise name, and then descending by BPM.
     */
    @Query(SELECT_PORTION)
    fun list(size: Int, offset: Int): List<NoteWithExerciseInfo>

    /**
     * Getting a [List] of the [size] or fewer notes with their exercises' info in accordance with
     * the [offset] in the list of notes whose exercise names contain [exerciseSubname]. The notes
     * are sorted in descending order by timestamp, then ascending by exercise name, and then
     * descending by BPM.
     *
     * @param size limit the count of notes.
     * @param offset position of the first target note in the list of notes whose exercise names
     * contain [exerciseSubname].
     * @param exerciseSubname part of the names of the target notes' exercises.
     * @return [List] of the [size] or fewer notes with their exercises' info in accordance with the
     * [offset] in the list of notes whose exercise names contain [exerciseSubname]. The notes are
     * sorted in descending order by timestamp, then ascending by exercise name, and then descending
     * by BPM.
     */
    @Query(SELECT_PORTION_BY_EXERCISE_SUBNAME)
    fun listByExerciseSubname(
        size: Int,
        offset: Int,
        exerciseSubname: String
    ): List<NoteWithExerciseInfo>

    /**
     * Getting a [List] of the [size] or fewer notes with their exercises' info in accordance with
     * the [offset] in the list of notes whose exercise names contain [exerciseSubname] and the
     * timestamp is greater than or equal to [timestampFromInclusive] and less than
     * [timestampToExclusive]. The notes are sorted in descending order by timestamp, then ascending
     * by exercise name, and then descending by BPM.
     *
     * @param size limit the count of notes.
     * @param offset position of the first target note in the list of notes whose exercise names
     * contain [exerciseSubname] and the timestamp is greater than or equal to
     * [timestampFromInclusive] and less than [timestampToExclusive].
     * @param exerciseSubname part of the names of the target notes.
     * @param timestampFromInclusive minimum timestamp value of the target notes.
     * @param timestampToExclusive value that should not be reached by the timestamp of the target
     * notes.
     * @return [List] of the [size] or fewer notes with their exercises' info in accordance with the
     * [offset] in the list of notes whose exercise names contain [exerciseSubname] and the
     * timestamp is greater than or equal to [timestampFromInclusive] and less than
     * [timestampToExclusive]. The notes are sorted in descending order by timestamp, then ascending
     * by exercise name, and then descending by BPM.
     */
    @Query(SELECT_PORTION_BY_EXERCISE_SUBNAME_AND_TIMESTAMP_RANGE)
    fun listByExerciseSubnameAndTimestampRange(
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
     * then descending by BPM.
     *
     * @param size limit the count of notes.
     * @param offset position of the first target note in the list of notes whose exercise names
     * contain [exerciseSubname] and the BPM is greater than or equal to [bpmFromInclusive] and less
     * than or equal to [bpmToInclusive].
     * @param exerciseSubname part of the names of the target notes' exercises.
     * @param bpmFromInclusive minimum BPM value of the target notes.
     * @param bpmToInclusive maximum BPM value of the target notes.
     * @return [List] of the [size] or fewer notes with their exercises' info in accordance with the
     * [offset] in the list of notes whose exercise names contain [exerciseSubname] and the BPM is
     * greater than or equal to [bpmFromInclusive] and less than or equal to [bpmToInclusive]. The
     * notes are sorted in descending order by timestamp, then ascending by exercise name, and then
     * descending by BPM.
     */
    @Query(SELECT_PORTION_BY_EXERCISE_SUBNAME_AND_BPM_RANGE)
    fun listByExerciseSubnameAndBpmRange(
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
     * then ascending by exercise name, and then descending by BPM.
     *
     * @param size limit the count of notes.
     * @param offset position of the first target note in the list of notes whose exercise names
     * contain [exerciseSubname], the timestamp is greater than or equal to [timestampFromInclusive]
     * and less than [timestampToExclusive] and the BPM is greater than or equal to
     * [bpmFromInclusive] and less than or equal to [bpmToInclusive].
     * @param exerciseSubname part of the names of the target notes' exercises.
     * @param timestampFromInclusive minimum timestamp value of the target notes.
     * @param timestampToExclusive value that should not be reached by the timestamp of the target
     * notes.
     * @param bpmFromInclusive minimum BPM value of the target notes.
     * @param bpmToInclusive maximum BPM value of the target notes.
     * @return [List] of the [size] or fewer notes with their exercises' info in accordance with the
     * [offset] in the list of notes whose exercise names contain [exerciseSubname], the timestamp
     * is greater than or equal to [timestampFromInclusive] and less than [timestampToExclusive] and
     * the BPM is greater than or equal to [bpmFromInclusive] and less than or equal to
     * [bpmToInclusive]. The notes are sorted in descending order by timestamp, then ascending by
     * exercise name, and then descending by BPM.
     */
    @Query(SELECT_PORTION_BY_EXERCISE_SUBNAME_TIMESTAMP_RANGE_AND_BPM_RANGE)
    fun listByExerciseSubnameTimestampRangeAndBpmRange(
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
     * descending order by timestamp, then ascending by exercise name, and then descending by BPM.
     *
     * @param size limit the count of notes.
     * @param offset position of the first target note in the list of notes whose timestamp is
     * greater than or equal to [timestampFromInclusive] and less than [timestampToExclusive].
     * @param timestampFromInclusive minimum timestamp value of the target notes.
     * @param timestampToExclusive value that should not be reached by the timestamp of the target
     * notes.
     * @return [List] of the [size] or fewer notes with their exercises' info in accordance with the
     * [offset] in the list of notes whose timestamp is greater than or equal to
     * [timestampFromInclusive] and less than [timestampToExclusive]. The notes are sorted in
     * descending order by timestamp, then ascending by exercise name, and then descending by BPM.
     */
    @Query(SELECT_PORTION_BY_TIMESTAMP_RANGE)
    fun listByTimestampRange(
        size: Int,
        offset: Int,
        timestampFromInclusive: Long,
        timestampToExclusive: Long
    ): List<NoteWithExerciseInfo>

    /**
     * Getting a [List] of the [size] or fewer notes with their exercises' info in accordance with
     * the [offset] in the list of notes whose BPM is greater than or equal to [bpmFromInclusive]
     * and less than or equal to [bpmToInclusive]. The notes are sorted in descending order by
     * timestamp, then ascending by exercise name, and then descending by BPM.
     *
     * @param size limit the count of notes.
     * @param offset position of the first target note in the list of notes whose BPM is greater
     * than or equal to [bpmFromInclusive] and less than or equal to [bpmToInclusive].
     * @param bpmFromInclusive minimum BPM value of the target notes.
     * @param bpmToInclusive maximum BPM value of the target notes.
     * @return [List] of the [size] or fewer notes with their exercises' info in accordance with the
     * [offset] in the list of notes whose BPM is greater than or equal to [bpmFromInclusive] and
     * less than or equal to [bpmToInclusive]. The notes are sorted in descending order by
     * timestamp, then ascending by exercise name, and then descending by BPM.
     */
    @Query(SELECT_PORTION_BY_BPM_RANGE)
    fun listByBpmRange(
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
     * in descending order by timestamp, then ascending by exercise name, and then descending by
     * BPM.
     *
     * @param size limit the count of notes.
     * @param offset position of the first target note in the list of notes whose timestamp is
     * greater than or equal to [timestampFromInclusive] and less than [timestampToExclusive] and
     * the BPM is greater than or equal to [bpmFromInclusive] and less than or equal to
     * [bpmToInclusive].
     * @param timestampFromInclusive minimum timestamp value of the target notes.
     * @param timestampToExclusive value that should not be reached by the timestamp of the target
     * notes.
     * @param bpmFromInclusive minimum BPM value of the target notes.
     * @param bpmToInclusive maximum BPM value of the target notes.
     * @return [List] of the [size] or fewer notes with their exercises' info in accordance with the
     * [offset] in the list of notes whose timestamp is greater than or equal to
     * [timestampFromInclusive] and less than [timestampToExclusive] and the BPM is greater than or
     * equal to [bpmFromInclusive] and less than or equal to [bpmToInclusive]. The notes are sorted
     * in descending order by timestamp, then ascending by exercise name, and then descending by
     * BPM.
     */
    @Query(SELECT_PORTION_BY_TIMESTAMP_RANGE_AND_BPM_RANGE)
    fun listByTimestampRangeAndBpmRange(
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
     * @param timestampFromInclusive minimum timestamp value of the target notes.
     * @param timestampToExclusive value that should not be reached by the timestamp of the target
     * notes.
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
     * @param bpmFromInclusive minimum BPM value of the target notes.
     * @param bpmToInclusive maximum BPM value of the target notes.
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
     * @param timestampFromInclusive minimum timestamp value of the target notes.
     * @param timestampToExclusive value that should not be reached by the timestamp of the target
     * notes.
     * @param bpmFromInclusive minimum BPM value of the target notes.
     * @param bpmToInclusive maximum BPM value of the target notes.
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
     * @param timestampFromInclusive minimum timestamp value of the target notes.
     * @param timestampToExclusive value that should not be reached by the timestamp of the target
     * notes.
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
     * @param bpmFromInclusive minimum BPM value of the target notes.
     * @param bpmToInclusive maximum BPM value of the target notes.
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
     * @param timestampFromInclusive minimum timestamp value of the target notes.
     * @param timestampToExclusive value that should not be reached by the timestamp of the target
     * notes.
     * @param bpmFromInclusive minimum BPM value of the target notes.
     * @param bpmToInclusive maximum BPM value of the target notes.
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
     * @param timestampFromInclusive minimum timestamp value of the target notes.
     * @param timestampToExclusive value that should not be reached by the timestamp of the target
     * notes.
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
     * @param bpmFromInclusive minimum BPM value of the target notes.
     * @param bpmToInclusive maximum BPM value of the target notes.
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
     * @param timestampFromInclusive minimum timestamp value of the target notes.
     * @param timestampToExclusive value that should not be reached by the timestamp of the target
     * notes.
     * @param bpmFromInclusive minimum BPM value of the target notes.
     * @param bpmToInclusive maximum BPM value of the target notes.
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
     * @param timestampFromInclusive minimum timestamp value of the target notes.
     * @param timestampToExclusive value that should not be reached by the timestamp of the target
     * notes.
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
     * @param bpmFromInclusive minimum BPM value of the target notes.
     * @param bpmToInclusive maximum BPM value of the target notes.
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
     * @param timestampFromInclusive minimum timestamp value of the target notes.
     * @param timestampToExclusive value that should not be reached by the timestamp of the target
     * notes.
     * @param bpmFromInclusive minimum BPM value of the target notes.
     * @param bpmToInclusive maximum BPM value of the target notes.
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
