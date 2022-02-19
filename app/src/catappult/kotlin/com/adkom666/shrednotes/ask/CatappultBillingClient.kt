package com.adkom666.shrednotes.ask

import android.app.Activity
import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.adkom666.shrednotes.BuildConfig
import com.adkom666.shrednotes.ask.template.GoogleLikeBillingClient
import com.adkom666.shrednotes.ask.template.GoogleLikeBillingStateListener
import com.adkom666.shrednotes.ask.template.GoogleLikeConsumeResponseListener
import com.adkom666.shrednotes.ask.template.GoogleLikePurchase
import com.adkom666.shrednotes.ask.template.GoogleLikePurchasesResponseListener
import com.adkom666.shrednotes.ask.template.GoogleLikePurchasesUpdatedListener
import com.adkom666.shrednotes.ask.template.GoogleLikeSkuDetails
import com.adkom666.shrednotes.ask.template.GoogleLikeSkuDetailsResponseListener
import com.adkom666.shrednotes.ask.util.ActivityLifecycleOptionalCallbacks
import com.adkom666.shrednotes.ask.util.createBillingIntent
import com.adkom666.shrednotes.ask.util.createBillingUrl
import com.adkom666.shrednotes.ask.util.isAptoideOrWalletPackagePresent
import com.appcoins.sdk.billing.AppcoinsBillingClient
import com.appcoins.sdk.billing.Purchase
import com.appcoins.sdk.billing.PurchasesUpdatedListener
import com.appcoins.sdk.billing.ResponseCode
import com.appcoins.sdk.billing.SkuDetails
import com.appcoins.sdk.billing.SkuDetailsParams
import com.appcoins.sdk.billing.helpers.CatapultBillingAppCoinsFactory
import com.appcoins.sdk.billing.listeners.AppCoinsBillingStateListener
import com.appcoins.sdk.billing.types.SkuType
import timber.log.Timber
import java.lang.ref.WeakReference

/**
 * Main tools for communication between the 'Catappult' and user application code.
 *
 * @param context see [Context].
 * @property purchasesUpdatedListener listener for purchases updated events.
 */
class CatappultBillingClient(
    context: Context,
    private val purchasesUpdatedListener: GoogleLikePurchasesUpdatedListener
) : GoogleLikeBillingClient {

    private val billingClient: AppcoinsBillingClient =
        CatapultBillingAppCoinsFactory.BuildAppcoinsBilling(
            context,
            VENDING_BASE_64_ENCODED_PUBLIC_KEY,
            CatappultPurchasesUpdatedListener()
        )

    private var billingInfo: BillingInfo? = null
    private var billingActivityLauncher: ActivityResultLauncher<Intent>? = null

    override val isReady: Boolean
        get() = billingClient.isReady

    init {
        val activityLauncherRegistration = ActivityLauncherRegistration()
        val application = context.applicationContext as Application
        application.registerActivityLifecycleCallbacks(activityLauncherRegistration)
    }

    override fun startConnection(listener: GoogleLikeBillingStateListener) {
        Timber.d("Start connection")
        billingClient.startConnection(CatappultBillingClientStateListener(listener))
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
            billingActivityLauncher?.let { launcher ->
                val url = createBillingUrl(1, "APPC", skuDetails.source.sku)
                val intent = createBillingIntent(activity, url)
                val isAptoideOrWalletPresent = intent.isAptoideOrWalletPackagePresent()
                startBilling(
                    context = activity,
                    launcher = launcher,
                    intent = intent,
                    url = url,
                    isAptoideOrWalletPresent = isAptoideOrWalletPresent
                )
            } ?: if (BuildConfig.DEBUG) {
                error("No activity launcher to start billing")
            }
        } else if (BuildConfig.DEBUG) {
            throw ClassCastException("Failed to cast skuDetails to CatappultSkuDetails")
        }
    }

    override fun endConnection() {
        Timber.d("End connection")
        billingClient.endConnection()
    }

    private fun startBilling(
        context: Context,
        launcher: ActivityResultLauncher<Intent>,
        intent: Intent,
        url: String,
        isAptoideOrWalletPresent: Boolean
    ) {
        billingInfo = BillingInfo(
            url = url,
            isAptoideOrWalletPresent = isAptoideOrWalletPresent,
            contextRef = WeakReference(context)
        )
        try {
            launcher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Timber.e(e)
            finishBilling(false, null)
        }
    }

    private fun handleActivityResult(result: ActivityResult) = when (result.resultCode) {
        Activity.RESULT_OK -> {
            // There is no information about purchases,
            // 'Catappult' is not working correctly today(
            finishBilling(true, null)
        }
        Activity.RESULT_CANCELED -> if (restartBilling()) {
            // Waiting for the next result
        } else {
            finishBilling(false, null)
        }
        else -> finishBilling(false, null)
    }

    private fun restartBilling(): Boolean {
        billingInfo?.let { info ->
            if (info.isAptoideOrWalletPresent.not()) {
                info.contextRef.get()?.let { context ->
                    val intent = createBillingIntent(context, info.url)
                    val isAptoideOrWalletPresent = intent.isAptoideOrWalletPackagePresent()
                    if (isAptoideOrWalletPresent) {
                        billingActivityLauncher?.let { launcher ->
                            startBilling(
                                context = context,
                                launcher = launcher,
                                intent = intent,
                                url = info.url,
                                isAptoideOrWalletPresent = isAptoideOrWalletPresent
                            )
                            return true
                        }
                    }
                }
            }
        }
        return false
    }

    private fun finishBilling(
        isOk: Boolean,
        @Suppress("SameParameterValue")
        purchases: List<GoogleLikePurchase>?
    ) {
        billingInfo = null
        purchasesUpdatedListener.onPurchasesUpdated(isOk, purchases)
    }

    private val Int.isOk: Boolean
        get() = this == ResponseCode.OK.value

    private inner class CatappultBillingClientStateListener(
        private val listener: GoogleLikeBillingStateListener
    ) : AppCoinsBillingStateListener {

        override fun onBillingSetupFinished(responseCode: Int) {
            Timber.d("Billing setup finished: responseCode=$responseCode")
            listener.onBillingSetupFinished(responseCode.isOk)
        }

        override fun onBillingServiceDisconnected() {
            Timber.d("Billing service disconnected")
            listener.onBillingServiceDisconnected()
        }
    }

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

    private inner class ActivityLauncherRegistration : ActivityLifecycleOptionalCallbacks() {

        private val launchers: MutableMap<Int, ActivityResultLauncher<Intent>> = mutableMapOf()

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            Timber.d("onActivityCreated: $activity")
            if (activity is ComponentActivity) {
                activity.registerForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    Timber.d("On activity result: result=$result")
                    handleActivityResult(result)
                }.also { launcher ->
                    val activityHash = activity.hashCode()
                    launchers[activityHash] = launcher
                }
            }
        }

        override fun onActivityResumed(activity: Activity) {
            Timber.d("onActivityResumed: $activity")
            val activityHash = activity.hashCode()
            launchers[activityHash]?.let { launcher ->
                billingActivityLauncher = launcher
            }
        }

        override fun onActivityDestroyed(activity: Activity) {
            Timber.d("onActivityDestroyed: $activity")
            val activityHash = activity.hashCode()
            launchers.remove(activityHash)?.let { launcher ->
                if (billingActivityLauncher == launcher) {
                    billingActivityLauncher = null
                }
            }
        }
    }

    private data class BillingInfo(
        val contextRef: WeakReference<Context>,
        val url: String,
        val isAptoideOrWalletPresent: Boolean
    )
}
