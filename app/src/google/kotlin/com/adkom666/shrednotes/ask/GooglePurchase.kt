package com.adkom666.shrednotes.ask

import com.adkom666.shrednotes.ask.template.GoogleLikePurchase
import com.android.billingclient.api.Purchase

/**
 * Represents an in-app billing purchase based on [com.android.billingclient.api.Purchase].
 *
 * @property source original in-app billing purchase.
 */
class GooglePurchase(private val source: Purchase) : GoogleLikePurchase {

    override val token: String
        get() = source.purchaseToken

    override fun isMatch(sku: String): Boolean {
        return source.skus.contains(sku)
    }
}
