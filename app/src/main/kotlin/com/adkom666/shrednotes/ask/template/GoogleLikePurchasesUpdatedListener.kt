package com.adkom666.shrednotes.ask.template

/**
 * Listener for purchases updated events.
 */
interface GoogleLikePurchasesUpdatedListener {

    /**
     * Implement this method to get notifications for purchases updates. Both purchases initiated by
     * your app and the ones initiated outside of your app will be reported here.
     *
     * @param isOk true if new purchases are made.
     * @param purchases list of updated purchases if present.
     */
    fun onPurchasesUpdated(isOk: Boolean, purchases: List<GoogleLikePurchase>?)
}
