package com.adkom666.shrednotes.ask.template

/**
 * Listener to a result of SKU details query.
 */
interface GoogleLikeSkuDetailsResponseListener {

    /**
     * Called to notify that a fetch SKU details operation has finished.
     *
     * @param isOk true if the query was successful.
     * @param skuDetailsList list of SKU details.
     */
    fun onSkuDetailsResponse(isOk: Boolean, skuDetailsList: List<GoogleLikeSkuDetails>?)
}
