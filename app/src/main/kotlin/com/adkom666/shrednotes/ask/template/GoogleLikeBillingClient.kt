package com.adkom666.shrednotes.ask.template

import android.app.Activity

/**
 * Main interface for communication between the billing library and user application code. It
 * provides convenience methods for in-app billing. Similar to Google's billing client interface.
 * This is done so that its implementations can use Google-like billing clients. Should be created
 * before the activity initiating the donation starts.
 */
interface GoogleLikeBillingClient {

    /**
     * Checks if the billing client is currently connected to the service, so that requests to other
     * methods will succeed.
     */
    val isReady: Boolean

    /**
     * Starts up billing client setup process asynchronously.
     *
     * @param listener callback for setup process.
     */
    fun startConnection(listener: GoogleLikeBillingStateListener)

    /**
     * Requests in-app purchases details for currently owned items bought within your app. Only
     * non-consumed one-time purchases are returned.
     *
     * @param listener the listener for the result of the query returned asynchronously through the
     * callback with the list of purchases and the success flag.
     */
    fun queryInAppPurchasesAsync(listener: GoogleLikePurchasesResponseListener)

    /**
     * Consumes a given in-app product.
     *
     * @param purchase consuming purchase.
     * @param listener the listener for the result of the consume operation returned asynchronously
     * through the callback with the purchase token and the success flag.
     */
    fun consumeAsync(purchase: GoogleLikePurchase, listener: GoogleLikeConsumeResponseListener)

    /**
     * Performs a network query to get SKU details and return the result asynchronously.
     *
     * @param skus specifies the SKUs that are queried.
     * @param listener the listener for the result of the query operation returned asynchronously
     * through the callback with the list of SKU details and the success flag.
     */
    fun querySkuDetailsAsync(skus: List<String>, listener: GoogleLikeSkuDetailsResponseListener)

    /**
     * Initiates the billing flow for an in-app purchase.
     *
     * @param activity an activity reference from which the billing flow will be launched.
     * @param skuDetails specifies the SKU details of the item being purchased.
     */
    fun launchBillingFlow(activity: Activity, skuDetails: GoogleLikeSkuDetails)

    /**
     * Closes the connection and releases all held resources such as service connections.
     */
    fun endConnection()
}
