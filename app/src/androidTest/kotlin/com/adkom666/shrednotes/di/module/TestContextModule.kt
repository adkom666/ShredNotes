package com.adkom666.shrednotes.di.module

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import dagger.Module
import dagger.Provides

@Suppress("UndocumentedPublicClass", "UndocumentedPublicFunction")
@Module
class TestContextModule {

    @Provides
    fun context(): Context {
        return InstrumentationRegistry.getInstrumentation().targetContext
    }
}
