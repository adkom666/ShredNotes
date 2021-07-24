package com.adkom666.shrednotes.ask

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.adkom666.shrednotes.BuildConfig
import com.adkom666.shrednotes.ask.template.GoogleLikeBillingClient
import com.adkom666.shrednotes.ask.template.GoogleLikeBillingFactory
import com.adkom666.shrednotes.ask.template.GoogleLikeBillingStateListener
import com.adkom666.shrednotes.ask.template.GoogleLikeConsumeResponseListener
import com.adkom666.shrednotes.ask.template.GoogleLikePurchase
import com.adkom666.shrednotes.ask.template.GoogleLikePurchasesResponseListener
import com.adkom666.shrednotes.ask.template.GoogleLikePurchasesUpdatedListener
import com.adkom666.shrednotes.ask.template.GoogleLikeSkuDetails
import com.adkom666.shrednotes.ask.template.GoogleLikeSkuDetailsResponseListener
import com.appcoins.sdk.billing.AppcoinsBillingClient
import com.appcoins.sdk.billing.BillingFlowParams
import com.appcoins.sdk.billing.Purchase
import com.appcoins.sdk.billing.PurchasesUpdatedListener
import com.appcoins.sdk.billing.ResponseCode
import com.appcoins.sdk.billing.SkuDetails
import com.appcoins.sdk.billing.SkuDetailsParams
import com.appcoins.sdk.billing.helpers.CatapultBillingAppCoinsFactory
import com.appcoins.sdk.billing.listeners.AppCoinsBillingStateListener
import com.appcoins.sdk.billing.types.SkuType
import timber.log.Timber

/**
 * Factory for instantiating [GoogleLikeBillingClient] based on
 * [com.appcoins.sdk.billing.AppcoinsBillingClient].
 */
class CatappultBillingFactory : GoogleLikeBillingFactory {

    override fun createBillingClient(
        context: Context,
        purchasesUpdatedListener: GoogleLikePurchasesUpdatedListener
    ): GoogleLikeBillingClient = object : GoogleLikeBillingClient {

        private val billingClient: AppcoinsBillingClient =
            CatapultBillingAppCoinsFactory.BuildAppcoinsBilling(
                context,
                VENDING_BASE_64_ENCODED_PUBLIC_KEY,
                CatappultPurchasesUpdatedListener()
            )

        override val isReady: Boolean
            get() = billingClient.isReady

        override fun startConnection(listener: GoogleLikeBillingStateListener) {
            val appCoinsListener = object : AppCoinsBillingStateListener {

                override fun onBillingSetupFinished(responseCode: Int) {
                    Timber.d("Billing setup finished: responseCode=$responseCode")
                    listener.onBillingSetupFinished(responseCode.isOk)
                }

                override fun onBillingServiceDisconnected() {
                    Timber.d("Billing service disconnected")
                    listener.onBillingServiceDisconnected()
                }
            }
            Timber.d("Start connection")
            billingClient.startConnection(appCoinsListener)
        }

        override fun queryInAppPurchasesAsync(listener: GoogleLikePurchasesResponseListener) {
            val skuType = SkuType.inapp.toString()
            Timber.d("Query purchases: skuType=$skuType")
            val purchasesResult = billingClient.queryPurchases(skuType)
            Timber.d("purchasesResult=$purchasesResult")
            listener.onPurchasesResponse(
                purchasesResult.responseCode.isOk,
                purchasesResult.purchases?.map(Purchase::toGoogleLikePurchase)
            )
        }

        override fun consumeAsync(
            purchase: GoogleLikePurchase,
            listener: GoogleLikeConsumeResponseListener
        ) {
            Timber.d("Consume async: purchase.token=${purchase.token}")
            billingClient.consumeAsync(purchase.token) { responseCode, purchaseToken ->
                Timber.d(
                    """Consume response:
                        |responseCode=$responseCode,
                        |purchaseToken=$purchaseToken""".trimMargin()
                )
                listener.onConsumeResponse(responseCode.isOk, purchaseToken)
            }
        }

        override fun querySkuDetailsAsync(
            skus: List<String>,
            listener: GoogleLikeSkuDetailsResponseListener
        ) {
            val skuArrayList = ArrayList<String>()
            skuArrayList.addAll(skus)
            val params = SkuDetailsParams()
            params.itemType = SkuType.inapp.toString()
            params.moreItemSkus = skuArrayList
            Timber.d("Query sku details async: params=$params")
            billingClient.querySkuDetailsAsync(params) { responseCode, skuDetailsList ->
                Timber.d(
                    """SKU details acquired:
                        |responseCode=$responseCode,
                        |skuDetailsList=$skuDetailsList""".trimMargin()
                )
                listener.onSkuDetailsResponse(
                    responseCode.isOk,
                    skuDetailsList?.map(SkuDetails::toGoogleLikeSkuDetails)
                )
            }
        }

        override fun launchBillingFlow(activity: Activity, skuDetails: GoogleLikeSkuDetails) {
            if (skuDetails is CatappultSkuDetails) {
                val params = BillingFlowParams(
                    skuDetails.source.sku,
                    skuDetails.source.itemType,
                    "orderId=" + System.currentTimeMillis(),
                    DEVELOPER_PAYLOAD,
                    "origin"
                )
                Timber.d("Launch billing flow: skuDetails.source=${skuDetails.source}")
                billingClient.launchBillingFlow(activity, params)
            } else if (BuildConfig.DEBUG) {
                throw ClassCastException("Failed to cast skuDetails to CatappultSkuDetails")
            }
        }

        override fun endConnection() {
            Timber.d("End connection")
            billingClient.endConnection()
        }

        override fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            Timber.d(
                """Handle activity result:
                    |requestCode=$requestCode,
                    |resultCode=$resultCode,
                    |data=$data""".trimMargin()
            )
            billingClient.onActivityResult(requestCode, resultCode, data)
        }

        private val Int.isOk: Boolean
            get() = this == ResponseCode.OK.value

        private inner class CatappultPurchasesUpdatedListener : PurchasesUpdatedListener {

            override fun onPurchasesUpdated(
                responseCode: Int,
                purchases: MutableList<Purchase>?
            ) {
                Timber.d(
                    """Purchases updated:
                        |responseCode=$responseCode,
                        |purchases=$purchases""".trimMargin()
                )
                purchasesUpdatedListener.onPurchasesUpdated(
                    responseCode.isOk,
                    purchases?.map(Purchase::toGoogleLikePurchase)
                )
            }
        }
    }
}
