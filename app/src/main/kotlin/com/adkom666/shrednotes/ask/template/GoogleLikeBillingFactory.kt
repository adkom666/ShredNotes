package com.adkom666.shrednotes.ask.template

import android.content.Context
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.time.ExperimentalTime

/**
 * Factory for instantiating [GoogleLikeBillingClient].
 */
interface GoogleLikeBillingFactory {

    /**
     * Instantiate [GoogleLikeBillingClient].
     *
     * @param context [Context].
     * @param purchasesUpdatedListener listener for purchases updated events.
     * @return new instance of [GoogleLikeBillingClient].
     */
    @ExperimentalCoroutinesApi
    @ExperimentalTime
    fun createBillingClient(
        context: Context,
        purchasesUpdatedListener: GoogleLikePurchasesUpdatedListener
    ): GoogleLikeBillingClient
}
