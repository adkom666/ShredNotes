package com.adkom666.shrednotes.ask.template

/**
 * Represents an in-app billing purchase.
 */
interface GoogleLikePurchase {

    /**
     * Uniquely identifies a purchase for a given item and user pair.
     */
    val token: String

    /**
     * Does the [sku] correspond to this purchase.
     *
     * @param sku target SKU.
     * @return true if the [sku] correspond to this purchase.
     */
    fun isMatch(sku: String): Boolean
}
