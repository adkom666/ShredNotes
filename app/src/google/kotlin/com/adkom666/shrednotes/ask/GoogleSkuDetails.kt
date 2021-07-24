package com.adkom666.shrednotes.ask

import com.adkom666.shrednotes.ask.template.GoogleLikeSkuDetails
import com.android.billingclient.api.SkuDetails

/**
 * Represents an in-app product's listing details based on
 * [com.android.billingclient.api.SkuDetails].
 *
 * @property source original in-app product's listing details.
 */
class GoogleSkuDetails(val source: SkuDetails) : GoogleLikeSkuDetails {

    override val price: String
        get() = source.price

    override fun isMatch(sku: String): Boolean {
        return source.sku == sku
    }
}
