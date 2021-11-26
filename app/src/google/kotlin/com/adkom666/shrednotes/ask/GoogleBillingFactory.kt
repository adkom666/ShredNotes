package com.adkom666.shrednotes.ask

import android.app.Activity
import android.content.Context
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
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams
import kotlinx.coroutines.ExperimentalCoroutinesApi
import timber.log.Timber
import kotlin.time.ExperimentalTime

/**
 * Factory for instantiating [GoogleLikeBillingClient] based on
 * [com.android.billingclient.api.BillingClient].
 */
class GoogleBillingFactory : GoogleLikeBillingFactory {

    @ExperimentalCoroutinesApi
    @ExperimentalTime
    override fun createBillingClient(
        context: Context,
        purchasesUpdatedListener: GoogleLikePurchasesUpdatedListener
    ): GoogleLikeBillingClient = object : GoogleLikeBillingClient {

        private val billingClient: BillingClient = BillingClient.newBuilder(context)
            .setListener(GooglePurchasesUpdatedListener())
            .enablePendingPurchases()
            .build()

        override val isReady: Boolean
            get() = billingClient.isReady

        override fun startConnection(listener: GoogleLikeBillingStateListener) {
            val googleListener = object : BillingClientStateListener {

                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    Timber.d("Billing setup finished: billingResult=$billingResult")
                    listener.onBillingSetupFinished(billingResult.isOk)
                }

                override fun onBillingServiceDisconnected() {
                    Timber.d("Billing service disconnected")
                    listener.onBillingServiceDisconnected()
                }
            }
            Timber.d("Start connection")
            billingClient.startConnection(googleListener)
        }

        override fun queryInAppPurchasesAsync(listener: GoogleLikePurchasesResponseListener) {
            val skuType = BillingClient.SkuType.INAPP
            Timber.d("Query purchases: skuType=$skuType")
            billingClient.queryPurchasesAsync(skuType) { billingResult, purchases ->
                Timber.d(
                    """Purchases acquired:
                        |billingResult=$billingResult,
                        |purchases=$purchases""".trimMargin()
                )
                listener.onPurchasesResponse(
                    billingResult.isOk,
                    purchases.map(Purchase::toGoogleLikePurchase)
                )
            }
        }

        override fun consumeAsync(
            purchase: GoogleLikePurchase,
            listener: GoogleLikeConsumeResponseListener
        ) {
            val params = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.token)
                .build()
            Timber.d("Consume async: params=$params")
            billingClient.consumeAsync(params) { billingResult, purchaseToken ->
                Timber.d(
                    """Consume response:
                        |billingResult=$billingResult,
                        |purchaseToken=$purchaseToken""".trimMargin()
                )
                listener.onConsumeResponse(billingResult.isOk, purchaseToken)
            }
        }

        override fun querySkuDetailsAsync(
            skus: List<String>,
            listener: GoogleLikeSkuDetailsResponseListener
        ) {
            val skuArrayList = ArrayList<String>()
            skuArrayList.addAll(skus)
            val params = SkuDetailsParams.newBuilder()
                .setSkusList(skuArrayList)
                .setType(BillingClient.SkuType.INAPP)
                .build()
            Timber.d("Query sku details async: params=$params")
            billingClient.querySkuDetailsAsync(params) { billingResult, skuDetailsList ->
                Timber.d(
                    """SKU details acquired:
                        |billingResult=$billingResult,
                        |skuDetailsList=$skuDetailsList""".trimMargin()
                )
                listener.onSkuDetailsResponse(
                    billingResult.isOk,
                    skuDetailsList?.map(SkuDetails::toGoogleLikeSkuDetails)
                )
            }
        }

        override fun launchBillingFlow(activity: Activity, skuDetails: GoogleLikeSkuDetails) {
            if (skuDetails is GoogleSkuDetails) {
                val params = BillingFlowParams.newBuilder()
                    .setSkuDetails(skuDetails.source)
                    .build()
                Timber.d("Launch billing flow: skuDetails.source=${skuDetails.source}")
                billingClient.launchBillingFlow(activity, params)
            } else if (BuildConfig.DEBUG) {
                throw ClassCastException("Failed to cast skuDetails to GoogleSkuDetails")
            }
        }

        override fun endConnection() {
            Timber.d("End connection")
            billingClient.endConnection()
        }

        private val BillingResult.isOk: Boolean
            get() = responseCode == BillingClient.BillingResponseCode.OK

        private inner class GooglePurchasesUpdatedListener : PurchasesUpdatedListener {

            override fun onPurchasesUpdated(
                billingResult: BillingResult,
                purchases: MutableList<Purchase>?
            ) {
                Timber.d(
                    """Purchases updated:
                        |billingResult=$billingResult,
                        |purchases=$purchases""".trimMargin()
                )
                purchasesUpdatedListener.onPurchasesUpdated(
                    billingResult.isOk,
                    purchases?.map(Purchase::toGoogleLikePurchase)
                )
            }
        }
    }
}
