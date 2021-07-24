package com.adkom666.shrednotes.ask.template

/**
 * Represents an in-app product's listing details.
 */
interface GoogleLikeSkuDetails {

    /**
     * Formatted price of the item, including its currency sign.
     */
    val price: String

    /**
     * Does the [sku] correspond to this in-app product.
     *
     * @param sku target SKU.
     * @return true if the [sku] correspond to this in-app product.
     */
    fun isMatch(sku: String): Boolean
}
