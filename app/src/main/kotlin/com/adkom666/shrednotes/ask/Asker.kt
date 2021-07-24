package com.adkom666.shrednotes.ask

import com.adkom666.shrednotes.sound.ShredSound
import timber.log.Timber

/**
 * Grateful recipient of money.
 *
 * @property shredSound way to thank you.
 */
class Asker(private val shredSound: ShredSound) : OnDonationFinishListener {

    override fun onDonationFinish(donationResult: DonationResult) {
        Timber.d("Donation finished: donationResult=$donationResult")
        when (donationResult) {
            DonationResult.FAIL ->
                shredSound.play(ShredSound.Track.SAD)
            DonationResult.DONATED, DonationResult.DONATED_NOT_CONSUMED ->
                shredSound.play(ShredSound.Track.JOY)
            DonationResult.DISPOSED -> Unit
        }
    }
}
