package com.adkom666.shrednotes.data.model

import android.os.Parcelable
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
    val id: Long = 0L,
    val dateTime: Date,
    val exercise: Exercise,
    val bpm: Int
) : Parcelable
