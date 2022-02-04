package com.adkom666.shrednotes.data.external

import com.adkom666.shrednotes.BuildConfig

/**
 * Envelope for external shred notes.
 *
 * @property version version of external shred notes.
 * @property content external shred notes content.
 * @property preferences data dependent preferences.
 */
data class ExternalShredNotesEnvelope(
    val version: Int = BuildConfig.SHRED_NOTES_VERSION,
    val content: String,
    val preferences: String?
)
