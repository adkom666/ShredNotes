package com.adkom666.shrednotes.di.module

import com.adkom666.shrednotes.ask.Asker
import com.adkom666.shrednotes.ask.Donor
import com.adkom666.shrednotes.ask.template.GoogleLikeBillingFactory
import com.adkom666.shrednotes.ask.template.GoogleLikeDonor
import com.adkom666.shrednotes.sound.ShredSound
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Named
import javax.inject.Singleton

@Suppress("UndocumentedPublicClass", "UndocumentedPublicFunction")
@Module(includes = [BillingModule::class, SoundModule::class])
class AskModule {

    @ExperimentalCoroutinesApi
    @Provides
    @Singleton
    fun donor(
        billingFactory: GoogleLikeBillingFactory,
        @Named("sku")
        sku: String,
        shredSound: ShredSound
    ): Donor {
        val donor = GoogleLikeDonor(billingFactory, sku)
        val asker = Asker(shredSound)
        donor.setReusableOnDonationFinishListener(asker)
        return donor
    }
}
