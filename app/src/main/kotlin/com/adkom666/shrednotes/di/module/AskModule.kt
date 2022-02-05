package com.adkom666.shrednotes.di.module

import com.adkom666.shrednotes.ask.Asker
import com.adkom666.shrednotes.ask.Donor
import com.adkom666.shrednotes.ask.template.GoogleLikeBillingFactory
import com.adkom666.shrednotes.ask.template.GoogleLikeDonor
import com.adkom666.shrednotes.sound.ShredSound
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.ExperimentalCoroutinesApi

@Suppress("UndocumentedPublicClass", "UndocumentedPublicFunction")
@ExperimentalCoroutinesApi
@ExperimentalTime
@Module(includes = [BillingModule::class, SoundModule::class])
class AskModule {

    @Provides
    @Singleton
    fun donor(
        billingFactory: GoogleLikeBillingFactory,
        @Named(SKU_STRING)
        sku: String,
        shredSound: ShredSound
    ): Donor {
        val donor = GoogleLikeDonor(billingFactory, sku)
        val asker = Asker(shredSound)
        donor.setReusableOnDonationFinishListener(asker)
        return donor
    }
}
