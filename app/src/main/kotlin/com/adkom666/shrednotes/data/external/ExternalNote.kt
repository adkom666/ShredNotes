package com.adkom666.shrednotes.data.external

import com.adkom666.shrednotes.common.Id

/**
 * Note model to store separately from the application.
 *
 * @property id unique identifier of the note.
 * @property timestamp time of training in milliseconds since January 1, 1970, 00:00:00 GMT.
 * @property exerciseId training exercise identifier.
 * @property bpm training BPM.
 */
data class ExternalNote(
    val id: Id,
    val timestamp: Long,
    val exerciseId: Long,
    val bpm: Int
)
