package com.adkom666.shrednotes.ask

import com.adkom666.shrednotes.ask.template.GoogleLikePurchase
import com.adkom666.shrednotes.ask.template.GoogleLikeSkuDetails
import com.appcoins.sdk.billing.Purchase
import com.appcoins.sdk.billing.SkuDetails

/**
 * Getting a [GoogleLikePurchase] from a [CatappultSkuDetails].
 */
fun SkuDetails.toGoogleLikeSkuDetails(): GoogleLikeSkuDetails = CatappultSkuDetails(this)

/**
 * Getting a [GoogleLikePurchase] from a [CatappultPurchase].
 */
fun Purchase.toGoogleLikePurchase(): GoogleLikePurchase = CatappultPurchase(this)
