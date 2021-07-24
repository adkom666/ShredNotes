package com.adkom666.shrednotes.ask

/**
 * Listener for donation finished events.
 */
interface OnDonationFinishListener {

    /**
     * Implement this method to receive end-of-donation notifications.
     *
     * @param donationResult see [DonationResult].
     */
    fun onDonationFinish(donationResult: DonationResult)
}
