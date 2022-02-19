package com.adkom666.shrednotes.di.component

import com.adkom666.shrednotes.App
import com.adkom666.shrednotes.di.module.AppModule
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjectionModule
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import javax.inject.Singleton
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.ExperimentalCoroutinesApi

@Suppress("UndocumentedPublicClass", "UndocumentedPublicFunction")
@ExperimentalCoroutinesApi
@ExperimentalTime
@Singleton
@Component(
    modules = [
        AndroidInjectionModule::class,
        AndroidSupportInjectionModule::class,
        AppModule::class
    ]
)
interface AppComponent : AndroidInjector<App> {

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun app(app: App): Builder

        fun build(): AppComponent
    }
}
