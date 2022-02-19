package com.adkom666.shrednotes.ask.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.adkom666.shrednotes.BuildConfig

private const val DOMAIN = BuildConfig.APPLICATION_ID

private const val PACKAGE_APTOIDE = "cm.aptoide.pt"
private const val PACKAGE_APPCOINS_WALLET = "com.appcoins.wallet"

/**
 * Creation of an URL, from the call of which billing starts.
 *
 * @param value the value of the chosen product (if no currency is set it is considered APPC).
 * @param currency the currency in which the value is sent, if no currency is sent it is considered
 * APPC (AppCoins).
 * @param product the id of the item being bought.
 * @return URL, from the call of which billing starts.
 */
fun createBillingUrl(value: Int, currency: String, product: String): String {
    return "https://apichain.catappult.io/transaction/inapp" +
            "?value=$value" +
            "&currency=$currency" +
            "&domain=$DOMAIN" +
            "&product=$product"
}

/**
 * Creating [Intent] based on a given [url] in such a way that it opens through the applications
 * necessary for billing, if available.
 *
 * @param context see [Context].
 * @param url URL, from the call of which billing starts.
 * @return [Intent] based on a given [url] in such a way that it opens through the applications
 * necessary for billing, if available.
 */
fun createBillingIntent(context: Context, url: String): Intent {
    val intent = Intent(Intent.ACTION_VIEW)
    intent.data = Uri.parse(url)
    // Check if there is an application that can process the AppCoins billing flow
    val packageManager = context.applicationContext.packageManager
    val resolveInfoList = packageManager.queryIntentActivities(
        intent,
        PackageManager.MATCH_DEFAULT_ONLY
    )
    resolveInfoList.forEach { info ->
        val packageName = info.activityInfo.packageName
        if (packageName == PACKAGE_APTOIDE) {
            // If there's 'Aptoide' installed always choose 'Aptoide' as default to open url
            intent.`package` = packageName
            return@forEach
        } else if (packageName == PACKAGE_APPCOINS_WALLET) {
            // If 'Aptoide' is not installed and 'Wallet' is installed
            // then choose 'Wallet' as default to open url
            intent.`package` = packageName
        }
    }
    return intent
}

/**
 * Check whether the applications necessary for billing are available.
 *
 * @return true, if the applications necessary for billing, if available.
 */
fun Intent.isAptoideOrWalletPackagePresent(): Boolean = `package`?.let { packageName ->
    packageName == PACKAGE_APTOIDE || packageName == PACKAGE_APPCOINS_WALLET
} ?: false
