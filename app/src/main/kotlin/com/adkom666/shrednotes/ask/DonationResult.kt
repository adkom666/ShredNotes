package com.adkom666.shrednotes.ask

/**
 * Result of the donation process.
 */
enum class DonationResult {

    /**
     * Canceled or something went wrong.
     */
    FAIL,

    /**
     * The donation process was disposed outside.
     */
    DISPOSED,

    /**
     * Thank you!
     */
    DONATED,

    /**
     * Thank you! But technically, your in-app purchase was not consumed.
     */
    DONATED_NOT_CONSUMED
}
