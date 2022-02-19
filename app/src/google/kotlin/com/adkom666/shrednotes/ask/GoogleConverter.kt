package com.adkom666.shrednotes.ask

import com.adkom666.shrednotes.ask.template.GoogleLikePurchase
import com.adkom666.shrednotes.ask.template.GoogleLikeSkuDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails

/**
 * Getting a [GoogleLikeSkuDetails] from a [GoogleSkuDetails].
 */
fun SkuDetails.toGoogleLikeSkuDetails(): GoogleLikeSkuDetails = GoogleSkuDetails(this)

/**
 * Getting a [GoogleLikePurchase] from a [GooglePurchase].
 */
fun Purchase.toGoogleLikePurchase(): GoogleLikePurchase = GooglePurchase(this)
