package com.adkom666.shrednotes.di.module

import android.content.Context
import com.adkom666.shrednotes.data.google.Google
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Suppress("UndocumentedPublicClass", "UndocumentedPublicFunction")
@Module(includes = [ContextModule::class])
class GoogleModule {

    @Provides
    @Singleton
    fun google(context: Context): Google {
        return Google(context)
    }
}
