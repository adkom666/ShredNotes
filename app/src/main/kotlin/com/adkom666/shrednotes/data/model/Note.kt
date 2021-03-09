package com.adkom666.shrednotes.data.model

import android.os.Parcelable
import com.adkom666.shrednotes.common.Id
import kotlinx.parcelize.Parcelize
import java.util.Date

/**
 * Model of the note about shred training.
 *
 * @property id unique identifier of the note.
 * @property dateTime date and time of training.
 * @property exercise training exercise.
 * @property bpm training BPM.
 */
@Parcelize
data class Note(
    val id: Id = 0,
    val dateTime: Date,
    val exercise: Exercise,
    val bpm: Int
) : Parcelable
