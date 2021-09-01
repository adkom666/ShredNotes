package com.adkom666.shrednotes.di.component

import com.adkom666.shrednotes.data.db.StoreExerciseTest
import com.adkom666.shrednotes.data.db.StoreNoteTest
import com.adkom666.shrednotes.di.module.TestDatabaseModule
import dagger.Component
import javax.inject.Singleton

@Suppress("UndocumentedPublicClass", "UndocumentedPublicFunction")
@Singleton
@Component(modules = [TestDatabaseModule::class])
interface TestDatabaseComponent {
    fun inject(test: StoreExerciseTest)
    fun inject(test: StoreNoteTest)
}
