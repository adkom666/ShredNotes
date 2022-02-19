package com.adkom666.shrednotes

import com.adkom666.shrednotes.ask.Donor
import com.adkom666.shrednotes.di.component.DaggerAppComponent
import dagger.android.AndroidInjector
import dagger.android.support.DaggerApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime
import timber.log.Timber
import javax.inject.Inject

/**
 * "Shred Notes" application.
 */
@ExperimentalCoroutinesApi
@ExperimentalTime
class App : DaggerApplication() {

    @Inject
    lateinit var donor: Donor

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
            donor.prepare(applicationContext)
        }
    }

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        return DaggerAppComponent.builder()
            .app(this)
            .build()
    }
}
