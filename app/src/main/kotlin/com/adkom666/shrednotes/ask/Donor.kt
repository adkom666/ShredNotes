package com.adkom666.shrednotes.ask

import android.app.Activity
import android.content.Context

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
}
