package com.adkom666.shrednotes.di.module

import com.adkom666.shrednotes.ask.CatappultBillingFactory
import com.adkom666.shrednotes.ask.Donor
import com.adkom666.shrednotes.ask.template.GoogleLikeDonor
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Singleton

@Suppress("UndocumentedPublicClass", "UndocumentedPublicFunction")
@Module
class AskModule {

    private companion object {
        private const val SKU = "donate"
    }

    @ExperimentalCoroutinesApi
    @Provides
    @Singleton
    fun donor(): Donor {
        val billingFactory = CatappultBillingFactory()
        return GoogleLikeDonor(billingFactory, SKU)
    }
}
