package com.adkom666.shrednotes.statistics

import com.adkom666.shrednotes.data.model.Note

/**
 * BPM records.
 *
 * @property topNotes notes with max BPM.
 */
data class BpmRecords(
    val topNotes: List<Note>
)
