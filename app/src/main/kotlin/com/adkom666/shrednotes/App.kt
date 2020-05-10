package com.adkom666.shrednotes

import android.app.Application
import timber.log.Timber

/**
 * "Shred Notes" application.
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}
