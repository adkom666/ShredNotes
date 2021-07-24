package com.adkom666.shrednotes.ask

import com.adkom666.shrednotes.ask.template.GoogleLikeSkuDetails
import com.appcoins.sdk.billing.SkuDetails

/**
 * Represents an in-app product's listing details based on [com.appcoins.sdk.billing.SkuDetails].
 *
 * @property source original in-app product's listing details.
 */
class CatappultSkuDetails(val source: SkuDetails) : GoogleLikeSkuDetails {

    override val price: String
        get() = source.price

    override fun isMatch(sku: String): Boolean {
        return source.sku == sku
    }
}
