package com.adkom666.shrednotes.data.db

import androidx.room.withTransaction

/**
 * A wrapper over a database that only allows you to organize the execution of data operations
 * within a transaction.
 *
 * @property database wrapped database.
 */
class Transactor(private val database: ShredNotesDatabase) {

    /**
     * Calls the specified suspending [block] in a database transaction.
     *
     * @param block a set of operations to be performed within a transaction.
     * @return value from the [block].
     */
    suspend fun <R> transaction(block: suspend () -> R): R {
        return database.withTransaction(block)
    }
}
