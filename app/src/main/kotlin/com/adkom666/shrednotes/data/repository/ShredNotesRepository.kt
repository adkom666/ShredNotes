package com.adkom666.shrednotes.data.repository

import com.adkom666.shrednotes.data.external.ExternalShredNotesV1

/**
 * Managing all application data storage.
 */
interface ShredNotesRepository {

    /**
     * Getting all application data.
     *
     * @return all application data as [ExternalShredNotesV1].
     */
    suspend fun shredNotesV1Suspending(): ExternalShredNotesV1

    /**
     * Replacing all application data by [shredNotes].
     *
     * @param shredNotes all application data as [ExternalShredNotesV1].
     */
    suspend fun replaceShredNotesByV1Suspending(shredNotes: ExternalShredNotesV1)
}
