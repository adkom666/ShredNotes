package com.adkom666.shrednotes.di.module

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Suppress("UndocumentedPublicClass", "UndocumentedPublicFunction")
@Module
class GsonModule {

    @Provides
    @Singleton
    fun gson(): Gson {
        return GsonBuilder().create()
    }
}
