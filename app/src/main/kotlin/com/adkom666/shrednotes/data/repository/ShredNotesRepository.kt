package com.adkom666.shrednotes.data.repository

import com.adkom666.shrednotes.data.external.ExternalShredNotes

/**
 * Managing all application data storage.
 */
interface ShredNotesRepository {

    /**
     * Getting all application data.
     *
     * @return all application data as [ExternalShredNotes].
     */
    suspend fun shredNotesSuspending(): ExternalShredNotes

    /**
     * Replacing all application data by [shredNotes].
     *
     * @param shredNotes all application data as [ExternalShredNotes].
     */
    suspend fun replaceShredNotesSuspendingBy(shredNotes: ExternalShredNotes)
}
