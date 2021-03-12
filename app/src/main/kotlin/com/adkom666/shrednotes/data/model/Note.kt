package com.adkom666.shrednotes.data.model

import android.os.Parcelable
import com.adkom666.shrednotes.common.Id
import com.adkom666.shrednotes.common.NO_ID
import kotlinx.parcelize.Parcelize
import java.util.Date

const val NOTE_BPM_MIN = 13
const val NOTE_BPM_MAX = 666

/**
 * Model of the note about shred training.
 *
 * @property id unique identifier of the note.
 * @property dateTime date and time of training.
 * @property exerciseName training exercise name.
 * @property bpm training BPM.
 */
@Parcelize
data class Note(
    val id: Id = NO_ID,
    val dateTime: Date,
    val exerciseName: String,
    val bpm: Int
) : Parcelable
