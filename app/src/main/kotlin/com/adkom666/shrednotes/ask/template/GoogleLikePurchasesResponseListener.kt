package com.adkom666.shrednotes.ask.template

/**
 * Listener to a result of purchases query.
 */
interface GoogleLikePurchasesResponseListener {

    /**
     * Called to notify that the query purchases operation has finished.
     *
     * @param isOk whether the query completed successfully.
     * @param purchases list of purchases.
     */
    fun onPurchasesResponse(isOk: Boolean, purchases: List<GoogleLikePurchase>?)
}
