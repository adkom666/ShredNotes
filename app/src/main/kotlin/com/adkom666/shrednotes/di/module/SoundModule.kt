package com.adkom666.shrednotes.di.module

import android.content.Context
import com.adkom666.shrednotes.sound.ShredSound
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Suppress("UndocumentedPublicClass", "UndocumentedPublicFunction")
@Module(includes = [ContextModule::class])
class SoundModule {

    @Provides
    @Singleton
    fun shredSound(context: Context): ShredSound {
        return ShredSound(context)
    }
}
