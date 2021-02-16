package com.adkom666.shrednotes.data.db.dao

import androidx.room.Insert
import androidx.room.Update
import androidx.room.Delete
import androidx.room.OnConflictStrategy

/**
 * Common data set operations.
 */
interface BaseDao<T> {

    /**
     * Insert the information from [entity] into the corresponding database table.
     *
     * @param entity information to insert into the database.
     * @return identifier of the database row that contains the inserted information.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insert(entity: T): Long

    /**
     * Insert the information from all entities of the [entityList] into the corresponding database
     * table.
     *
     * @param entityList list of entities with information to insert.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertAll(entityList: List<T>)

    /**
     * Update the information in the corresponding database table according to [entity].
     *
     * @param entity information to update in the database.
     * @return count of updated rows in the database table.
     */
    @Update
    fun update(entity: T): Int

    /**
     * Delete the information contained in [entity] from the corresponding database table.
     *
     * @param entity information to delete from the database.
     * @return count of deleted rows from the database table.
     */
    @Delete
    fun delete(entity: T): Int

    /**
     * Delete the information contained in the [entityList] from the corresponding database table.
     *
     * @param entityList list of entities with information to delete.
     * @return count of deleted rows from the database table.
     */
    @Delete
    fun deleteAll(entityList: List<T>): Int
}
