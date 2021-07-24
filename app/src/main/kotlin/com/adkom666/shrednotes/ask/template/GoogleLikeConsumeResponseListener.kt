package com.adkom666.shrednotes.ask.template

/**
 * Callback that notifies when a consumption operation finishes.
 */
interface GoogleLikeConsumeResponseListener {

    /**
     * Called to notify that a consume operation has finished.
     *
     * @param isOk whether the consumption operation completed successfully.
     * @param purchaseToken the purchase token that was (or was to be) consumed.
     */
    fun onConsumeResponse(isOk: Boolean, purchaseToken: String?)
}
