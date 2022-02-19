package com.adkom666.shrednotes.ask

import android.content.Context
import com.adkom666.shrednotes.ask.template.GoogleLikeBillingClient
import com.adkom666.shrednotes.ask.template.GoogleLikeBillingFactory
import com.adkom666.shrednotes.ask.template.GoogleLikePurchasesUpdatedListener

/**
 * Factory for instantiating [GoogleLikeBillingClient] based on
 * [com.appcoins.sdk.billing.AppcoinsBillingClient].
 */
class CatappultBillingFactory : GoogleLikeBillingFactory {

    override fun createBillingClient(
        context: Context,
        purchasesUpdatedListener: GoogleLikePurchasesUpdatedListener
    ): GoogleLikeBillingClient = CatappultBillingClient(context, purchasesUpdatedListener)
}
