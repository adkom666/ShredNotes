package com.adkom666.shrednotes.di.module

import android.content.Context
import android.content.SharedPreferences
import com.adkom666.shrednotes.data.pref.ExerciseToolPreferences
import com.adkom666.shrednotes.data.pref.NoteToolPreferences
import com.adkom666.shrednotes.data.pref.ToolPreferences
import dagger.Module
import dagger.Provides
import javax.inject.Singleton
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Named

@Suppress("UndocumentedPublicClass", "UndocumentedPublicFunction")
@ExperimentalCoroutinesApi
@ExperimentalTime
@Module(includes = [ContextModule::class])
class PreferencesModule {

    private companion object {
        private const val DATA_DEPENDENT_PREFS_FILE_NAME = "shred_notes_data_dependent.pref"
        private const val DATA_INDEPENDENT_PREFS_FILE_NAME = "shred_notes_data_independent.pref"
    }

    @Provides
    @Named(DATA_DEPENDENT_PREFERENCES)
    @Singleton
    fun dataDependentPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(
            DATA_DEPENDENT_PREFS_FILE_NAME,
            Context.MODE_PRIVATE
        )
    }

    @Provides
    @Named(DATA_INDEPENDENT_PREFERENCES)
    @Singleton
    fun dataIndependentPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(
            DATA_INDEPENDENT_PREFS_FILE_NAME,
            Context.MODE_PRIVATE
        )
    }

    @Provides
    @Singleton
    fun toolPreferences(
        @Named(DATA_DEPENDENT_PREFERENCES)
        preferences: SharedPreferences
    ): ToolPreferences {
        return ToolPreferences(preferences)
    }

    @Provides
    @Singleton
    fun noteToolPreferences(toolPreferences: ToolPreferences): NoteToolPreferences {
        return toolPreferences
    }

    @Provides
    @Singleton
    fun exerciseToolPreferences(toolPreferences: ToolPreferences): ExerciseToolPreferences {
        return toolPreferences
    }
}
