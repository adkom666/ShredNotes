package com.adkom666.shrednotes.di.module

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import javax.inject.Singleton
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.ExperimentalCoroutinesApi

@Suppress("UndocumentedPublicClass", "UndocumentedPublicFunction")
@ExperimentalCoroutinesApi
@ExperimentalTime
@Module(includes = [ContextModule::class])
class PreferencesModule {

    private companion object {
        private const val PREFERENCES_FILE_NAME = "shred_notes.pref"
    }

    @Provides
    @Singleton
    fun preferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)
    }
}
