package com.adkom666.shrednotes.di.module

import android.content.SharedPreferences
import com.adkom666.shrednotes.data.DataManager
import com.adkom666.shrednotes.data.google.Google
import com.adkom666.shrednotes.data.repository.ShredNotesRepository
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Named

@Suppress("UndocumentedPublicClass", "UndocumentedPublicFunction")
@ExperimentalCoroutinesApi
@ExperimentalTime
@Module(
    includes = [
        RepositoryModule::class,
        PreferencesModule::class,
        GoogleModule::class,
        GsonModule::class
    ]
)
class DataManagerModule {

    @Provides
    fun dataManager(
        repository: ShredNotesRepository,
        @Named(DATA_DEPENDENT_PREFERENCES)
        preferences: SharedPreferences,
        google: Google,
        gson: Gson
    ): DataManager {
        return DataManager(repository, preferences, google, gson)
    }
}
