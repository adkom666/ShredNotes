package com.adkom666.shrednotes.ask

import android.app.Activity
import android.content.Context
import android.content.Intent

/**
 * Here you can donate me money.
 */
interface Donor {

    /**
     * True if donation is possible.
     */
    val isPrepared: Boolean

    /**
     * True if donation is in progress.
     */
    val isActive: Boolean

    /**
     * Money to donate.
     */
    val price: String?

    /**
     * Make donation possible.
     *
     * @param context [Context].
     */
    suspend fun prepare(context: Context)

    /**
     * Set [OnDonationFinishListener] for single donation, or skip to null if it no longer needed.
     * It will be skipped automatically when the donation ends.
     *
     * @param listener callback to notify that the donation is complete.
     */
    fun setDisposableOnDonationFinishListener(listener: OnDonationFinishListener?)

    /**
     * Set [OnDonationFinishListener] for all donations.
     *
     * @param listener callback to notify that the donation is complete.
     */
    fun setReusableOnDonationFinishListener(listener: OnDonationFinishListener)

    /**
     * Initiate donation. Thank you!
     *
     * @param activity current [Activity].
     */
    suspend fun donate(activity: Activity)

    /**
     * Reset to initial state.
     */
    fun dispose()

    /**
     * Call this method from [Activity.onActivityResult] so the donor can select the information it
     * needs.
     *
     * @param requestCode the integer request code originally supplied to
     * [Activity.startActivityForResult], allowing you to identify who this result came from.
     * @param resultCode the integer result code returned by the child activity through its
     * [Activity.setResult].
     * @param data an [Intent], which can return result data to the caller (various data can be
     * attached to [Intent] "extras").
     * @return true if the result was handled.
     */
    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean
}
