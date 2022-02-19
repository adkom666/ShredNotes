package com.adkom666.shrednotes.ask.template

import android.app.Activity
import android.content.Context
import com.adkom666.shrednotes.ask.DonationResult
import com.adkom666.shrednotes.ask.Donor
import com.adkom666.shrednotes.ask.OnDonationFinishListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.time.ExperimentalTime

/**
 * Donation way based on [GoogleLikeBillingClient].
 *
 * @property billingFactory factory for instantiating [GoogleLikeBillingClient].
 * @property sku donation purchase identifier.
 */
@ExperimentalCoroutinesApi
@ExperimentalTime
class GoogleLikeDonor(
    private val billingFactory: GoogleLikeBillingFactory,
    private val sku: String
) : Donor {

    private var billingClient: GoogleLikeBillingClient? = null
    private var skuDetails: GoogleLikeSkuDetails? = null
    private var isSkuDetailsQueried: Boolean = false
    private var isDonationInProgress: Boolean = false
    private var disposableOnDonationFinishListener: OnDonationFinishListener? = null
    private var reusableOnDonationFinishListener: OnDonationFinishListener? = null

    init {
        Timber.d("Init")
        setInitialState()
    }

    override val isPrepared: Boolean
        get() = isSkuDetailsQueried

    override val isActive: Boolean
        get() = isDonationInProgress

    override val price: String?
        get() = skuDetails?.price

    override suspend fun prepare(context: Context) = withContext(Dispatchers.IO) {
        Timber.d("Prepare")
        val billingClient = requestReadyBillingClient(context)
        Timber.d("billingClient=$billingClient")
        if (billingClient.isReady && billingClient.ensureNoPurchases()) {
            skuDetails = billingClient.querySkuDetails()
            isSkuDetailsQueried = true
        }
    }

    override fun setDisposableOnDonationFinishListener(listener: OnDonationFinishListener?) {
        Timber.d("Set disposable on donation finish listener: listener=$listener")
        disposableOnDonationFinishListener = listener
    }

    override fun setReusableOnDonationFinishListener(listener: OnDonationFinishListener) {
        Timber.d("Set reusable on donation finish listener: listener=$listener")
        reusableOnDonationFinishListener = listener
    }

    override suspend fun donate(activity: Activity) {
        Timber.d("Donate")
        Timber.d("skuDetails=$skuDetails")
        isDonationInProgress = true
        skuDetails?.let { details ->
            val billingClient = requestReadyBillingClient(activity)
            Timber.d("billingClient=$billingClient")
            if (billingClient.isReady) {
                Timber.d("Launch billing flow")
                billingClient.launchBillingFlow(activity, details)
            } else {
                finishDonation(DonationResult.FAIL)
            }
        } ?: finishDonation(DonationResult.FAIL)
    }

    override fun dispose() {
        Timber.d("Dispose")
        Timber.d("billingClient=$billingClient")
        billingClient?.endConnection()
        finishDonation(DonationResult.DISPOSED)
        setInitialState()
    }

    private fun setInitialState() {
        Timber.d("Set initial state")
        billingClient = null
        isSkuDetailsQueried = false
        skuDetails = null
        disposableOnDonationFinishListener = null
    }

    private suspend fun requestReadyBillingClient(
        context: Context
    ): GoogleLikeBillingClient = withContext(Dispatchers.IO) {
        billingClient?.let { client ->
            if (!client.isReady) {
                client.connect()
            }
            client
        } ?: run {
            val client = createBillingClient(context)
            billingClient = client
            client.connect()
            client
        }
    }

    private fun createBillingClient(context: Context): GoogleLikeBillingClient {
        Timber.d("Create billing client")
        return billingFactory.createBillingClient(context, DonationListener())
    }

    private fun finishDonation(donationResult: DonationResult) {
        Timber.d("Finish donation: donationResult=$donationResult")
        isDonationInProgress = false
        val listener = disposableOnDonationFinishListener
        disposableOnDonationFinishListener = null
        listener?.onDonationFinish(donationResult)
        reusableOnDonationFinishListener?.onDonationFinish(donationResult)
    }

    private suspend fun GoogleLikeBillingClient.connect(): Boolean {
        Timber.d("Connect")
        return suspendCancellableCoroutine { continuation ->
            val listener = object : GoogleLikeBillingStateListener {

                override fun onBillingSetupFinished(isOk: Boolean) {
                    Timber.d("Billing setup finished: isOk=$isOk")
                    if (continuation.isActive) {
                        continuation.resume(isOk, null)
                    }
                }

                override fun onBillingServiceDisconnected() {
                    Timber.d("Billing service disconnected")
                }
            }
            Timber.d("Start connection")
            startConnection(listener)
        }
    }

    private suspend fun GoogleLikeBillingClient.ensureNoPurchases(): Boolean {
        Timber.d("Ensure no purchases")
        return queryInAppPurchases()?.let { purchases ->
            Timber.d("purchases=$purchases")
            val notConsumedPurchase = purchases.find { purchase ->
                consume(purchase).not()
            }
            notConsumedPurchase?.let {
                Timber.d("Not consumed purchase: $it")
                false
            } ?: true
        } ?: true
    }

    private suspend fun GoogleLikeBillingClient.queryInAppPurchases(): List<GoogleLikePurchase>? {
        Timber.d("Query in-app purchases")
        return suspendCancellableCoroutine { continuation ->
            val listener = object : GoogleLikePurchasesResponseListener {

                override fun onPurchasesResponse(
                    isOk: Boolean,
                    purchases: List<GoogleLikePurchase>?
                ) {
                    Timber.d(
                        """Purchase response:
                            |isOk=$isOk,
                            |purchases=$purchases""".trimMargin()
                    )
                    val result = if (isOk) {
                        purchases
                    } else {
                        null
                    }
                    continuation.resume(result, null)
                }
            }
            Timber.d("Query in-app purchases async")
            queryInAppPurchasesAsync(listener)
        }
    }

    private suspend fun GoogleLikeBillingClient.consume(purchase: GoogleLikePurchase): Boolean {
        Timber.d("Consume: purchase=$purchase")
        return suspendCancellableCoroutine { continuation ->
            val listener = object : GoogleLikeConsumeResponseListener {

                override fun onConsumeResponse(isOk: Boolean, purchaseToken: String?) {
                    Timber.d(
                        """Purchase consumed:
                            |isOk=$isOk,
                            |purchaseToken=$purchaseToken""".trimMargin()
                    )
                    Timber.d("result=$isOk")
                    continuation.resume(isOk, null)
                }
            }
            Timber.d("Consume async: purchase=$purchase")
            consumeAsync(purchase, listener)
        }
    }

    private suspend fun GoogleLikeBillingClient.querySkuDetails(): GoogleLikeSkuDetails? {
        Timber.d("Query SKU details: sku=$sku")
        return suspendCancellableCoroutine { continuation ->
            val skus = listOf(sku)
            val listener = object : GoogleLikeSkuDetailsResponseListener {

                override fun onSkuDetailsResponse(
                    isOk: Boolean,
                    skuDetailsList: List<GoogleLikeSkuDetails>?
                ) {
                    Timber.d(
                        """SKU details acquired:
                            |isOk=$isOk,
                            |skuDetailsList=$skuDetailsList""".trimMargin()
                    )
                    val result = if (isOk) {
                        skuDetailsList?.find { it.isMatch(sku) }
                    } else {
                        null
                    }
                    Timber.d("result=$result")
                    continuation.resume(result, null)
                }
            }
            Timber.d("Query SKU details async: skus=$skus")
            querySkuDetailsAsync(skus, listener)
        }
    }

    private inner class DonationListener :
        GoogleLikePurchasesUpdatedListener,
        GoogleLikeConsumeResponseListener {

        override fun onPurchasesUpdated(isOk: Boolean, purchases: List<GoogleLikePurchase>?) {
            Timber.d(
                """Purchases updated:
                    |isOk=$isOk,
                    |purchases=$purchases""".trimMargin()
            )
            if (isOk) {
                val donation = purchases?.find { it.isMatch(sku) }
                Timber.d("donation=$donation")
                donation?.let {
                    beginConsume(it)
                } ?: finishDonation(DonationResult.DONATED_NOT_CONSUMED)
            } else {
                finishDonation(DonationResult.FAIL)
            }
        }

        override fun onConsumeResponse(isOk: Boolean, purchaseToken: String?) {
            Timber.d(
                """Purchase consumed:
                    |isOk=$isOk,
                    |purchaseToken=$purchaseToken""".trimMargin()
            )
            val donationResult = if (isOk) {
                DonationResult.DONATED
            } else {
                DonationResult.DONATED_NOT_CONSUMED
            }
            finishDonation(donationResult)
        }

        private fun beginConsume(purchase: GoogleLikePurchase) {
            Timber.d("Begin consume: purchase=$purchase")
            billingClient?.consumeAsync(purchase, this)
        }
    }
}
