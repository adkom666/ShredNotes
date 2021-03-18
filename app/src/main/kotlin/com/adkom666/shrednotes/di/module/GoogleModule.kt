package com.adkom666.shrednotes.di.module

import com.adkom666.shrednotes.data.google.Google
import dagger.Module
import dagger.Provides

@Suppress("UndocumentedPublicClass", "UndocumentedPublicFunction")
@Module
class GoogleModule {

    @Provides
    fun google(): Google {
        return Google()
    }
}
