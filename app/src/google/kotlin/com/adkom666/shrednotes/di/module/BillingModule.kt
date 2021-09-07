package com.adkom666.shrednotes.di.module

import com.adkom666.shrednotes.BuildConfig
import com.adkom666.shrednotes.ask.GoogleBillingFactory
import com.adkom666.shrednotes.ask.template.GoogleLikeBillingFactory
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Suppress("UndocumentedPublicClass", "UndocumentedPublicFunction")
@Module
class BillingModule {

    private companion object {
        private const val SKU = BuildConfig.DONATION_SKU
    }

    @Provides
    @Singleton
    fun billingFactory(): GoogleLikeBillingFactory {
        return GoogleBillingFactory()
    }

    @Provides
    @Named("sku")
    @Singleton
    fun sku(): String {
        return SKU
    }
}
