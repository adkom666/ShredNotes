package com.adkom666.shrednotes.di.module

import android.content.Context
import com.adkom666.shrednotes.App
import dagger.Module
import dagger.Provides

@Suppress("UndocumentedPublicClass", "UndocumentedPublicFunction")
@Module
class ContextModule {

    @Provides
    fun context(app: App): Context {
        return app.applicationContext
    }
}
