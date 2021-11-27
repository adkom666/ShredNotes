package com.adkom666.shrednotes.ask

import android.content.Context
import com.adkom666.shrednotes.ask.template.GoogleLikeBillingClient
import com.adkom666.shrednotes.ask.template.GoogleLikeBillingFactory
import com.adkom666.shrednotes.ask.template.GoogleLikePurchasesUpdatedListener

/**
 * Factory for instantiating [GoogleLikeBillingClient] based on
 * [com.android.billingclient.api.BillingClient].
 */
class GoogleBillingFactory : GoogleLikeBillingFactory {

    override fun createBillingClient(
        context: Context,
        purchasesUpdatedListener: GoogleLikePurchasesUpdatedListener
    ): GoogleLikeBillingClient = GoogleBillingClient(context, purchasesUpdatedListener)
}
