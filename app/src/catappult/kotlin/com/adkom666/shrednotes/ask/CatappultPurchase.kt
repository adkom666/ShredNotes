package com.adkom666.shrednotes.ask

import com.adkom666.shrednotes.ask.template.GoogleLikePurchase
import com.appcoins.sdk.billing.Purchase

/**
 * Represents an in-app billing purchase based on [com.appcoins.sdk.billing.Purchase].
 *
 * @property source original in-app billing purchase.
 */
class CatappultPurchase(private val source: Purchase) : GoogleLikePurchase {

    override val token: String
        get() = source.token

    override fun isMatch(sku: String): Boolean {
        return source.sku == sku
    }
}
