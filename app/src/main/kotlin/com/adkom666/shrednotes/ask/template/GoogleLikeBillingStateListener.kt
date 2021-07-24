package com.adkom666.shrednotes.ask.template

/**
 * Callback for setup process. This listener's [onBillingSetupFinished] method is called when the
 * setup process is complete.
 */
interface GoogleLikeBillingStateListener {

    /**
     * Called to notify that setup is complete.
     *
     * @param isOk true if setup is complete successfully.
     */
    fun onBillingSetupFinished(isOk: Boolean)

    /**
     * Called to notify that the connection to the billing service was lost.
     */
    fun onBillingServiceDisconnected()
}
